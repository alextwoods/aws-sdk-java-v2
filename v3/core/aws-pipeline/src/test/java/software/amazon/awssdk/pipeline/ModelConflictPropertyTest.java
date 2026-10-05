/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_INPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_OUTPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.STRING;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.ForAll;
import net.jqwik.api.GenerationMode;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.statistics.Statistics;
import software.amazon.awssdk.auth.api.AuthSchemeIds;
import software.amazon.awssdk.auth.api.AuthSchemeSelector;
import software.amazon.awssdk.auth.api.ModeledAuth;
import software.amazon.awssdk.auth.api.NoAuthAuthScheme;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.checksums.HttpChecksumMetadata;
import software.amazon.awssdk.checksums.compression.CompressionOptions;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.ModelConflictException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.endpoints.EndpointBuiltIn;
import software.amazon.awssdk.endpoints.EndpointResolutionException;
import software.amazon.awssdk.endpoints.binding.EndpointBinding;
import software.amazon.awssdk.endpoints.binding.EndpointClientValues;
import software.amazon.awssdk.endpoints.rules.EndpointRuleSet;
import software.amazon.awssdk.pipeline.PipelineFixtures.Driver;
import software.amazon.awssdk.pipeline.PipelineFixtures.HeaderProtocol;
import software.amazon.awssdk.pipeline.PipelineFixtures.RecordingRetryStrategy;
import software.amazon.awssdk.pipeline.PipelineFixtures.ScriptedHttp;
import software.amazon.awssdk.pipeline.PipelineFixtures.Struct;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.RequestCompressionTrait;
import software.amazon.smithy.model.traits.StreamingTrait;

/**
 * Property 51: Model conflicts block transmission (Requirement 13.18).
 *
 * <p>For any endpoint, auth, checksum, compression or content-encoding setting that conflicts with the operation's
 * model, a call through {@link AwsCallEngine} (either driver) fails before its transport is invoked, makes no retry,
 * and its error names the operation and the conflicting setting; for any non-conflicting control of the same shape,
 * exactly one transmission succeeds. {@link Oracle} decides each generated configuration independently of the
 * production checks, from the Smithy and SDK rules, and gives the exact diagnostic it must carry.
 *
 * <p>The conflicts are those production detects: a selected request checksum algorithm the model or runtime does not
 * support (unless an explicit checksum header makes the selection moot); a caller-supplied integrity header that
 * request compression would invalidate; a caller-declared {@code aws-chunked} coding that the trailer checksum needs
 * to apply; modeled auth schemes the client cannot satisfy; no endpoint, a binding whose value does not have the rule
 * parameter's type, and endpoint rule errors; and plan-time {@code httpChecksum} metadata that names missing members
 * or unknown algorithms (which fails {@link ClientRuntime#plan} before any call exists).
 *
 * <p>Every endpoint failure names the operation (Requirement 13.18): for a rule error the message is prefixed with the
 * operation while {@link EndpointResolutionException#ruleError()} stays the rule's exact message.
 */
class ModelConflictPropertyTest {
    private static final String SUPPORTED_ALGORITHMS = "CRC32 CRC32C CRC64NVME SHA1 SHA256 SHA512";
    private static final Set<String> INTEGRITY_HEADERS =
        Set.of("content-md5", "x-amz-trailer", "x-amz-checksum-crc32", "x-amz-checksum-sha256");
    private static final URI CUSTOM_ENDPOINT = URI.create("https://custom.example.com");
    private static final EndpointRuleSet RULES = EndpointRuleSet.fromJson("""
        {"version": "1.0",
         "parameters": {
           "Region": {"type": "String", "builtIn": "AWS::Region"},
           "UseFIPS": {"type": "Boolean", "builtIn": "AWS::UseFIPS", "required": true, "default": false},
           "Endpoint": {"type": "String", "builtIn": "SDK::Endpoint"}
         },
         "rules": [
           {"conditions": [{"fn": "isSet", "argv": [{"ref": "Endpoint"}]},
                           {"fn": "booleanEquals", "argv": [{"ref": "UseFIPS"}, true]}],
            "type": "error", "error": "Invalid Configuration: FIPS and custom endpoint are not supported"},
           {"conditions": [{"fn": "isSet", "argv": [{"ref": "Endpoint"}]}], "type": "endpoint",
            "endpoint": {"url": "{Endpoint}"}},
           {"conditions": [{"fn": "isSet", "argv": [{"ref": "Region"}]}], "type": "endpoint",
            "endpoint": {"url": "https://things.{Region}.example.com"}},
           {"conditions": [], "type": "error", "error": "Invalid Configuration: Missing Region"}
         ]}
        """);

    private static final Schema CHECKSUM_INPUT = Schema.structureBuilder(ShapeId.from("com.example#ChecksumInput"))
                                                       .putMember("Name", STRING)
                                                       .putMember("ChecksumAlgorithm", STRING)
                                                       .putMember("ValidationMode", STRING)
                                                       .build();
    private static final Schema STREAM_INPUT = Schema.structureBuilder(ShapeId.from("com.example#StreamInput"))
                                                     .putMember("Name", STRING)
                                                     .putMember("ChecksumAlgorithm", STRING)
                                                     .putMember("Body", Schema.createBlob(
                                                         ShapeId.from("com.example#Stream"), new StreamingTrait()))
                                                     .build();

    /** One generated setting of one category; each category has its own operation. */
    sealed interface Config permits Checksum, Compression, Encoding, Auth, Endpoint, Plan {
        default String category() {
            return getClass().getSimpleName();
        }

        default ShapeId operation() {
            return ShapeId.from("com.example#" + category() + "Thing");
        }
    }

    /** The caller's algorithm member value (or none) and whether it also sends an explicit checksum header. */
    record Checksum(String algorithm, boolean explicitHeader) implements Config {
    }

    /** A caller-supplied header (or none) on an operation with gzip request compression, possibly disabled. */
    record Compression(String header, boolean disabled) implements Config {
    }

    /** The caller's {@code Content-Encoding} (or none) on a streaming upload with a defaulted trailer checksum. */
    record Encoding(String contentEncoding) implements Config {
    }

    /** The modeled signed schemes in priority order, {@code @optionalAuth}, and whether the client has a selector. */
    record Auth(List<String> schemes, boolean optionalAuth, boolean noAuthSelector) implements Config {
        /** The Smithy effective schemes: the modeled ones, then noAuth if there are none or auth is optional. */
        List<String> effective() {
            List<String> effective = new ArrayList<>(schemes);
            if (schemes.isEmpty() || optionalAuth) {
                effective.add(AuthSchemeIds.NO_AUTH);
            }
            return effective;
        }
    }

    /** Either no rule set (with or without a client endpoint) or the rule set with the client's endpoint values. */
    record Endpoint(boolean rules, boolean clientEndpoint, boolean region, boolean fips, boolean custom,
                    boolean mistypedBinding) implements Config {
    }

    /** The operation's {@code httpChecksum} metadata: valid, or with one defect. */
    record Plan(Defect defect) implements Config {
        enum Defect { NONE, MISSING_ALGORITHM_MEMBER, MISSING_VALIDATION_MEMBER, UNKNOWN_RESPONSE_ALGORITHM }
    }

    record Scenario(Driver driver, Config config) {
    }

    /**
     * The diagnostic a conflict must carry.
     *
     * @param type the failure's type
     * @param operation the text naming the operation
     * @param setting the exact {@link ModelConflictException#setting()} (compared ignoring case), or {@code null}
     * @param fragments the texts naming the conflicting settings, each in the message ignoring case
     * @param planTime whether the conflict fails planning, before the call exists
     */
    record Diagnostic(Class<? extends Throwable> type, String operation, String setting, List<String> fragments,
                      boolean planTime) {
    }

    // Feature: aws-sdk-java-v3, Property 51: Model conflicts block transmission
    // Randomized: the space is small enough that jqwik would otherwise enumerate it exhaustively, skewing coverage.
    @Property(tries = 400, edgeCases = EdgeCasesMode.NONE, generation = GenerationMode.RANDOMIZED)
    @Label("Property 51: Model conflicts block transmission")
    void conflictsFailBeforeTransmissionNamingOperationAndSetting(@ForAll("scenarios") Scenario scenario) {
        Diagnostic expected = Oracle.diagnostic(scenario.config());
        Call call = new Call(scenario);
        call.run();
        String description = scenario + "\n  failure: " + call.failure + "\n  ledger: " + call.retries.ledger;

        if (expected == null) {
            assertThat(call.failure).as("control " + description).isNull();
            assertThat(call.http.requests).as("one transmission: " + description).hasSize(1);
            assertThat(call.http.transmissions).as("one transmission: " + description).isEqualTo(1);
            assertThat(call.attempts.get()).as("attempts of " + description).isEqualTo(1);
            assertThat(call.output.<String>getMemberValue(GET_OUTPUT.member("Value"))).isEqualTo("ok");
        } else {
            assertThat(call.http.requests).as("no transport invocation: " + description).isEmpty();
            assertThat(call.http.transmissions).as("no transmission: " + description).isZero();
            assertThat(call.attempts.get()).as("no retry: " + description).isEqualTo(expected.planTime() ? 0 : 1);
            assertThat(call.retries.ledger).as("no retry: " + description).noneMatch(e -> e.startsWith("retry#"));
            assertDiagnostic(call.failure, expected, scenario.config(), description);
        }

        String category = scenario.config().category();
        Statistics.label("driver").collect(scenario.driver());
        Statistics.label("case").collect(category, expected != null);
        Statistics.label("case").coverage(c -> {
            for (String name : List.of("Checksum", "Compression", "Encoding", "Auth", "Endpoint", "Plan")) {
                c.check(name, true).percentage(p -> p >= 3);
                c.check(name, false).percentage(p -> p >= 3);
            }
        });
        Statistics.label("conflict").collect(expected != null);
        Statistics.label("conflict").coverage(c -> {
            c.check(true).percentage(p -> p >= 30);
            c.check(false).percentage(p -> p >= 25);
        });
    }

    private static void assertDiagnostic(Throwable failure, Diagnostic expected, Config config, String description) {
        assertThat(failure).as(description).isInstanceOf(expected.type());
        String message = failure.getMessage().toLowerCase(Locale.ROOT);
        if (failure instanceof ModelConflictException conflict) {
            assertThat(conflict.operation()).as(description).isEqualTo(config.operation());
            assertThat(conflict.setting()).as(description).isEqualToIgnoringCase(expected.setting());
            assertThat(conflict.retryable()).as(description).isFalse();
        }
        if (expected.operation() != null) {
            assertThat(message).as("operation in " + description)
                               .contains(expected.operation().toLowerCase(Locale.ROOT));
        }
        for (String fragment : expected.fragments()) {
            assertThat(message).as("setting in " + description).contains(fragment.toLowerCase(Locale.ROOT));
        }
        if (failure instanceof EndpointResolutionException endpoint) {
            boolean ruleError = expected.fragments().contains("FIPS and custom endpoint")
                                || expected.fragments().contains("Missing Region");
            assertThat(endpoint.ruleError().isPresent()).as("rule error of " + description).isEqualTo(ruleError);
            endpoint.ruleError().ifPresent(rule -> assertThat(rule).as(description)
                                                                   .doesNotContain(config.operation().toString()));
        }
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        Arbitrary<Config> checksum = Combinators.combine(
            Arbitraries.frequencyOf(
                Tuple.of(3, Arbitraries.of("CRC32", "CRC32C", "CRC64NVME", "SHA1", "SHA256", "SHA512", "crc32c")),
                Tuple.of(4, Arbitraries.of("MD5", "md5", "XXHASH64", "XXHASH3", "XXHASH128", "CRC16", "BLAKE3")))
                       .injectNull(0.1),
            Arbitraries.frequency(Tuple.of(1, true), Tuple.of(4, false))).as(Checksum::new);
        Arbitrary<Config> compression = Combinators.combine(
            Arbitraries.of("X-Unrelated", "x-amz-checksum-mode", "Content-MD5", "x-amz-checksum-crc32",
                           "X-Amz-Checksum-Sha256", "x-amz-trailer").injectNull(0.1),
            Arbitraries.frequency(Tuple.of(1, true), Tuple.of(4, false))).as(Compression::new);
        Arbitrary<Config> encoding = Arbitraries.of("gzip", "identity", "aws-chunked", "gzip, aws-chunked",
                                                    "AWS-Chunked").injectNull(0.2).map(Encoding::new);
        Arbitrary<Config> auth = Combinators.combine(
            Arbitraries.of(List.<String>of(), List.of(AuthSchemeIds.SIGV4), List.of(AuthSchemeIds.HTTP_BEARER),
                           List.of(AuthSchemeIds.SIGV4, AuthSchemeIds.HTTP_BEARER),
                           List.of(AuthSchemeIds.HTTP_BEARER, AuthSchemeIds.SIGV4)),
            Arbitraries.of(true, false), Arbitraries.of(true, false)).as(Auth::new);
        Arbitrary<Boolean> flag = Arbitraries.of(true, false);
        Arbitrary<Config> endpoint = Combinators.combine(
            Arbitraries.frequency(Tuple.of(3, true), Tuple.of(1, false)), flag, flag, flag, flag,
            Arbitraries.frequency(Tuple.of(1, true), Tuple.of(5, false))).as(Endpoint::new);
        Arbitrary<Config> plan = Arbitraries.frequency(
            Tuple.of(3, Plan.Defect.NONE), Tuple.of(1, Plan.Defect.MISSING_ALGORITHM_MEMBER),
            Tuple.of(1, Plan.Defect.MISSING_VALIDATION_MEMBER), Tuple.of(1, Plan.Defect.UNKNOWN_RESPONSE_ALGORITHM))
                                            .map(Plan::new);
        return Combinators.combine(PipelineArbitraries.drivers(),
                                   Arbitraries.oneOf(checksum, compression, encoding, auth, endpoint, plan))
                          .as(Scenario::new);
    }

    /** The conflict rules of Requirement 13.18 per category, written from the Smithy and SDK rules. */
    static final class Oracle {
        private Oracle() {
        }

        /** The diagnostic of {@code config}'s conflict, or {@code null} if it does not conflict. */
        static Diagnostic diagnostic(Config config) {
            String operation = config.operation().toString();
            return switch (config) {
                case Checksum c -> c.algorithm() == null || c.explicitHeader() || supported(c.algorithm()) ? null
                    : conflict(operation, "ChecksumAlgorithm", c.algorithm().toUpperCase(Locale.ROOT));
                case Compression c -> c.disabled() || c.header() == null
                                      || !INTEGRITY_HEADERS.contains(c.header().toLowerCase(Locale.ROOT)) ? null
                    : conflict(operation, c.header(), "request compression");
                case Encoding e -> !codings(e.contentEncoding()).contains("aws-chunked") ? null
                    : conflict(operation, "Content-Encoding", "aws-chunked");
                case Auth a -> auth(a, operation);
                case Endpoint e -> endpoint(e, operation);
                case Plan p -> switch (p.defect()) {
                    case NONE -> null;
                    case MISSING_ALGORITHM_MEMBER -> planConflict(operation, "httpChecksum.requestAlgorithmMember",
                                                                  "'Algorithm'");
                    case MISSING_VALIDATION_MEMBER ->
                        planConflict(operation, "httpChecksum.requestValidationModeMember", "'Mode'");
                    case UNKNOWN_RESPONSE_ALGORITHM -> planConflict(operation, "httpChecksum.responseAlgorithms",
                                                                    "'CRC16'");
                };
            };
        }

        private static boolean supported(String algorithm) {
            return List.of(SUPPORTED_ALGORITHMS.split(" ")).contains(algorithm.toUpperCase(Locale.ROOT));
        }

        private static List<String> codings(String contentEncoding) {
            List<String> codings = new ArrayList<>();
            if (contentEncoding != null) {
                for (String coding : contentEncoding.split(",")) {
                    codings.add(coding.trim().toLowerCase(Locale.ROOT));
                }
            }
            return codings;
        }

        /** Without a selector only a sole noAuth is usable; the noAuth selector satisfies exactly noAuth. */
        private static Diagnostic auth(Auth auth, String operation) {
            List<String> effective = auth.effective();
            List<String> fragments = new ArrayList<>();
            if (!auth.noAuthSelector()) {
                if (effective.equals(List.of(AuthSchemeIds.NO_AUTH))) {
                    return null;
                }
                fragments.add("no auth scheme selector");
                fragments.addAll(effective);
            } else {
                if (effective.contains(AuthSchemeIds.NO_AUTH)) {
                    return null;
                }
                fragments.add("None of the modeled auth schemes can be used");
                effective.forEach(id -> fragments.add(id + " is not supported by this client"));
            }
            return new Diagnostic(SdkClientException.class, operation, null, fragments, false);
        }

        /**
         * Without rules the client endpoint is required. With rules, a mistyped binding fails binding first; then the
         * rules reject FIPS with a custom endpoint, and neither endpoint nor region.
         */
        private static Diagnostic endpoint(Endpoint e, String operation) {
            if (!e.rules()) {
                return e.clientEndpoint() ? null
                    : new Diagnostic(SdkClientException.class, operation, null,
                                     List.of("No endpoint is configured"), false);
            }
            if (e.mistypedBinding()) {
                return new Diagnostic(EndpointResolutionException.class, operation, null,
                                      List.of("Endpoint parameter UseFIPS", "must be a BOOLEAN"), false);
            }
            if (e.custom() && e.fips()) {
                return new Diagnostic(EndpointResolutionException.class, operation, null,
                                      List.of("FIPS and custom endpoint"), false);
            }
            if (!e.custom() && !e.region()) {
                return new Diagnostic(EndpointResolutionException.class, operation, null, List.of("Missing Region"),
                                      false);
            }
            return null;
        }

        private static Diagnostic conflict(String operation, String setting, String fragment) {
            return new Diagnostic(ModelConflictException.class, operation, setting, List.of(setting, fragment), false);
        }

        private static Diagnostic planConflict(String operation, String setting, String fragment) {
            return new Diagnostic(ModelConflictException.class, operation, setting, List.of(setting, fragment), true);
        }
    }

    /** One call of a scenario through a real engine over a scripted transport. */
    private static final class Call {
        final ScriptedHttp http = new ScriptedHttp().respond(200, "ok");
        final RecordingRetryStrategy retries = new RecordingRetryStrategy(PipelineFixtures.retries(3));
        final AtomicInteger attempts = new AtomicInteger();
        private final Scenario scenario;
        Struct output;
        Throwable failure;

        Call(Scenario scenario) {
            this.scenario = scenario;
        }

        void run() {
            Config config = scenario.config();
            Driver driver = scenario.driver();
            SdkClientConfiguration.Builder configuration =
                SdkClientConfiguration.builder().option(CompressionOptions.REQUEST_MIN_COMPRESSION_SIZE_BYTES, 0);
            if (!(config instanceof Endpoint e) || e.clientEndpoint()) {
                configuration.option(SdkClientOption.CLIENT_ENDPOINT, PipelineFixtures.ENDPOINT);
            }
            if (config instanceof Compression c) {
                configuration.option(CompressionOptions.DISABLE_REQUEST_COMPRESSION, c.disabled());
            }
            ClientRuntime.Builder runtime = ClientRuntime.builder().configuration(configuration.build())
                                                         .protocol(new HeaderProtocol())
                                                         .transportMode(driver.mode)
                                                         .retryStrategy(retries, "things");
            if (driver == Driver.ASYNC) {
                runtime.completionExecutor(Runnable::run)
                       .retryScheduler(delay -> CompletableFuture.completedFuture(null));
            }
            if (config instanceof Auth a && a.noAuthSelector()) {
                runtime.authSchemeSelector(AuthSchemeSelector.create(List.of(NoAuthAuthScheme.create())));
            }
            if (config instanceof Endpoint e && e.rules()) {
                EndpointClientValues.Builder values = EndpointClientValues.builder()
                                                                          .builtIn(EndpointBuiltIn.USE_FIPS, e.fips());
                if (e.region()) {
                    values.builtIn(EndpointBuiltIn.REGION, "us-west-2");
                }
                if (e.custom()) {
                    values.builtIn(EndpointBuiltIn.ENDPOINT, CUSTOM_ENDPOINT.toString());
                }
                runtime.endpointRules(RULES).endpointClientValues(values.build());
            }
            AwsCallEngine.Builder builder = AwsCallEngine.builder().runtime(runtime.build()).stageObserver(
                (call, attempt, stage) -> attempts.addAndGet(stage == PipelineStage.RESOLVE_ENDPOINT ? 1 : 0));
            AwsCallEngine engine = (driver == Driver.SYNC ? builder.syncHttpClient(http)
                                                          : builder.asyncHttpClient(http)).build();
            PipelinePlan<Struct, Struct> plan;
            try {
                plan = engine.runtime().plan(operation(config));
            } catch (RuntimeException planFailure) {
                failure = planFailure;
                return;
            }
            execute(engine, plan, config);
        }

        private void execute(AwsCallEngine engine, PipelinePlan<Struct, Struct> plan, Config config) {
            Map<String, Object> members = new HashMap<>();
            members.put("Name", "thing");
            AwsRequestOverrideConfiguration.Builder override = AwsRequestOverrideConfiguration.builder();
            switch (config) {
                case Checksum c -> {
                    if (c.algorithm() != null) {
                        members.put("ChecksumAlgorithm", c.algorithm());
                    }
                    if (c.explicitHeader()) {
                        override.putHeader("x-amz-checksum-crc32", "AAAAAA==");
                    }
                }
                case Compression c -> {
                    if (c.header() != null) {
                        override.putHeader(c.header(), "AAAAAA==");
                    }
                }
                case Encoding e -> {
                    if (e.contentEncoding() != null) {
                        override.putHeader("Content-Encoding", e.contentEncoding());
                    }
                }
                default -> {
                }
            }
            Struct input = new Struct(plan.operation().schemas().input(), members);
            boolean streaming = config instanceof Encoding;
            if (scenario.driver() == Driver.SYNC) {
                try {
                    output = engine.executeSync(plan, input, override.build(),
                                                streaming ? RequestBody.fromString("payload") : null);
                } catch (RuntimeException callFailure) {
                    failure = callFailure;
                }
                return;
            }
            CompletableFuture<Struct> future = engine.executeAsync(
                plan, input, override.build(), streaming ? AsyncRequestBody.fromString("payload") : null);
            assertThat(future).as("inline async call of " + scenario).isDone();
            try {
                output = future.get(5, TimeUnit.SECONDS);
            } catch (ExecutionException callFailure) {
                failure = callFailure.getCause();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        /** The operation of {@code config}'s category, modeled for its conflict. */
        private static AwsOperation<Struct, Struct> operation(Config config) {
            ShapeId id = config.operation();
            return switch (config) {
                case Checksum c -> PipelineFixtures.operationBuilder(Schema.createOperation(id), CHECKSUM_INPUT,
                                                                     GET_OUTPUT)
                                                   .httpChecksum(HttpChecksumMetadata.builder()
                                                                                     .requestAlgorithmMember(
                                                                                         "ChecksumAlgorithm")
                                                                                     .build())
                                                   .build();
                case Compression c -> PipelineFixtures.operationBuilder(
                    Schema.createOperation(id, RequestCompressionTrait.builder().encodings(List.of("gzip")).build()),
                    GET_INPUT, GET_OUTPUT).build();
                case Encoding e -> PipelineFixtures.operationBuilder(Schema.createOperation(id), STREAM_INPUT,
                                                                     GET_OUTPUT)
                                                   .httpChecksum(HttpChecksumMetadata.builder()
                                                                                     .requestAlgorithmMember(
                                                                                         "ChecksumAlgorithm")
                                                                                     .build())
                                                   .build();
                case Auth a -> PipelineFixtures.operation(id.toString(), GET_INPUT, GET_OUTPUT, modeledAuth(id, a));
                case Endpoint e -> PipelineFixtures.operationBuilder(Schema.createOperation(id), GET_INPUT,
                                                                     GET_OUTPUT)
                                                   .endpointBinding(e.mistypedBinding()
                                                                    ? EndpointBinding.builder(id.getName())
                                                                                     .staticContextParam("UseFIPS",
                                                                                                         "yes")
                                                                                     .build()
                                                                    : EndpointBinding.none())
                                                   .build();
                case Plan p -> PipelineFixtures.operationBuilder(Schema.createOperation(id), CHECKSUM_INPUT,
                                                                 GET_OUTPUT)
                                               .httpChecksum(planMetadata(p.defect()))
                                               .build();
            };
        }

        private static ModeledAuth modeledAuth(ShapeId id, Auth auth) {
            ModeledAuth.Builder builder = ModeledAuth.builder().operationId(id.toString())
                                                     .optionalAuth(auth.optionalAuth());
            auth.schemes().forEach(builder::addServiceScheme);
            if (!auth.schemes().isEmpty()) {
                builder.serviceAuthTrait(auth.schemes());
            }
            return builder.build();
        }

        private static HttpChecksumMetadata planMetadata(Plan.Defect defect) {
            HttpChecksumMetadata.Builder metadata = HttpChecksumMetadata.builder();
            return switch (defect) {
                case NONE -> metadata.requestAlgorithmMember("ChecksumAlgorithm").build();
                case MISSING_ALGORITHM_MEMBER -> metadata.requestAlgorithmMember("Algorithm").build();
                case MISSING_VALIDATION_MEMBER -> metadata.requestValidationModeMember("Mode")
                                                          .addResponseAlgorithm("CRC32").build();
                case UNKNOWN_RESPONSE_ALGORITHM -> metadata.requestValidationModeMember("ValidationMode")
                                                           .addResponseAlgorithm("CRC32")
                                                           .addResponseAlgorithm("CRC16").build();
            };
        }
    }
}
