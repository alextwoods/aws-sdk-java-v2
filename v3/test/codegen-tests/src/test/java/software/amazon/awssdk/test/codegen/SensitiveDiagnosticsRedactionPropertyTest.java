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

package software.amazon.awssdk.test.codegen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Builders;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.BeforeContainer;
import net.jqwik.api.statistics.Statistics;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.client.config.ConfigurationSources;
import software.amazon.awssdk.core.client.config.SettingSource;
import software.amazon.awssdk.dynamic.client.DynamicClient;
import software.amazon.awssdk.dynamic.client.DynamicServiceException;
import software.amazon.awssdk.dynamic.model.DynamicModel;
import software.amazon.awssdk.http.AsyncExchangeCompletion;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpHeaders;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.identity.providers.AwsCredentialsProviderChain;
import software.amazon.awssdk.identity.providers.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.identity.providers.ProcessCredentialsProvider;
import software.amazon.awssdk.identity.providers.ProfileCredentialsProvider;
import software.amazon.awssdk.identity.providers.SsoTokenProvider;
import software.amazon.awssdk.identity.providers.StaticCredentialsProvider;
import software.amazon.awssdk.identity.providers.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.metrics.LoggingMetricPublisher;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.SystemSettings;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.BackoffStrategy;
import software.amazon.awssdk.retries.RetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.telemetry.spi.AttemptRecord;
import software.amazon.awssdk.telemetry.spi.CallRecord;
import software.amazon.awssdk.telemetry.spi.TelemetryConfiguration;
import software.amazon.awssdk.telemetry.spi.TelemetryPublisher;
import software.amazon.awssdk.test.codegen.support.CompiledSources;
import software.amazon.awssdk.utils.Redaction;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Property 69 (Requirement 21.10): sensitive data is absent from diagnostics.
 *
 * <p>Fixture, generated and compiled once: a restJson1 SigV4 {@code Vault} service whose {@code PutSecret} operation
 * carries {@code @sensitive} shapes everywhere a value can sit (a top-level string and blob, a sensitive structure,
 * nested structures, lists, maps with sensitive keys or values, a union, an {@code @httpLabel}, an {@code @httpHeader}
 * and an {@code @httpQuery} whose wire names are not sensitive names, a response header, and the members of two
 * modeled errors, one with a sensitive {@code message}); its JSON AST is the Dynamic Client's runtime model; and an
 * S3-shaped restXml service whose generated presigner presigns {@code GetObject} with a sensitive
 * {@code x-amz-server-side-encryption-customer-key}.
 *
 * <p>Each try allocates fresh unique canaries ({@code zq} + 8 random letters + a 3-digit index, so no canary contains
 * another) and places one in every generated sensitive position: the modeled input members chosen, the
 * {@code Authorization}, {@code Proxy-Authorization}, {@code Cookie}, {@code X-Amz-Security-Token}, SSE-C key and API
 * key override headers, the {@code X-Amz-Signature}, {@code X-Amz-Credential} and {@code X-Amz-Security-Token} raw
 * query parameters of a presigned-looking URL, the secret access key and session token of the credentials (static,
 * JVM-property, environment, profile file, credential process and provider-chain sources, each also in a failing
 * variant that holds the secret but cannot produce credentials), an SSO bearer token in a cache file (valid, expired,
 * incomplete or unparseable), and the server's echoed sensitive header, {@code Set-Cookie} and
 * {@code x-amz-security-token} response headers and modeled error members. The call runs through the generated sync
 * client, the generated async client or the Dynamic Client against a scripted transport (successes, modeled errors,
 * unmodeled retryable 5xx errors and retryable transport failures, retried up to the script length), with a telemetry
 * publisher and the logging metric publisher, while every {@code software.amazon.awssdk} log record at the chosen
 * threshold (TRACE to ERROR) is captured.
 *
 * <p>Rendered output: {@code toString} of every modeled request, response, nested generated value, builder, dynamic
 * output document, credential identity, provider, token, override configuration, transmitted
 * {@link SdkHttpRequest}, received {@link SdkHttpResponse}, response metadata, error details, presigned request and
 * telemetry record; {@code getMessage}, {@code toString} and the full stack trace (causes and suppressed included) of
 * every exception a caller or provider surfaces; every captured log record formatted with its throwable; and the
 * {@link Redaction} renderings of sensitive header maps, queries and URIs. Oracle: no canary (nor, for blobs, its
 * Base64 form) occurs in any rendering. The one scoped exception: the {@code toString} of an HTTP-level request
 * ({@link SdkHttpRequest}, a presigned request) renders the encoded path, and the HTTP SPI has no model to know that a
 * path segment is a {@code @sensitive} label, so label canaries are not checked against those two renderings (they
 * are checked everywhere else, including the request log). To keep the property honest, a try that reaches the
 * transport also checks
 * that every modeled input canary was actually transmitted and, with DEBUG enabled, that the request log was written.
 */
class SensitiveDiagnosticsRedactionPropertyTest {
    private static final String NAMESPACE = "example.redaction";
    private static final String VAULT_PACKAGE = "software.amazon.awssdk.test.codegen.redaction.vault";
    private static final String S3_PACKAGE = "software.amazon.awssdk.test.codegen.redaction.s3like";
    private static final ShapeId VAULT_ID = ShapeId.from(NAMESPACE + "#Vault");
    private static final URI ENDPOINT = URI.create("https://vault.example.test");
    private static final String TRANSPORT_NAME = "RedactionHttp";
    private static final String REQUEST_LOGGER = "software.amazon.awssdk.request";
    /** A fixed clock for the SSO token cache: valid tokens expire after it, expired ones before it. */
    private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");

    private static final String VAULT_MODEL = """
        $version: "2"
        namespace example.redaction

        @aws.protocols#restJson1
        @aws.auth#sigv4(name: "vault")
        @aws.api#service(sdkId: "Vault", endpointPrefix: "vault")
        service Vault {
            version: "2024-01-01"
            operations: [PutSecret]
        }

        @idempotent
        @http(method: "PUT", uri: "/vaults/{vault}/secrets/{secretName}")
        operation PutSecret {
            input := with [SecretFields] {
                @required
                @httpLabel
                vault: String

                @required
                @httpLabel
                secretName: Secret

                @httpHeader("x-vault-pin")
                pin: Secret

                @httpQuery("code")
                code: Secret

                @httpQuery("page")
                page: String
            }
            output := with [SecretFields] {
                @httpHeader("x-vault-echo")
                echo: Secret
            }
            errors: [VaultDenied, VaultSealed]
        }

        @mixin
        structure SecretFields {
            label: String
            secret: Secret
            blob: SecretBlob
            envelope: Envelope
            secrets: SecretList
            byName: SecretMap
            keyed: SecretKeyMap
            envelopes: EnvelopeList
            envelopeMap: EnvelopeMap
            choice: Choice
            sealed: Sealed
            sealedList: SealedList
        }

        @sensitive
        string Secret

        @sensitive
        blob SecretBlob

        structure Envelope {
            label: String
            secret: Secret
            inner: Inner
            choice: Choice
        }

        structure Inner {
            secrets: SecretList
            byName: SecretMap
        }

        list SecretList {
            member: Secret
        }

        map SecretMap {
            key: String
            value: Secret
        }

        map SecretKeyMap {
            key: Secret
            value: String
        }

        list EnvelopeList {
            member: Envelope
        }

        map EnvelopeMap {
            key: String
            value: Envelope
        }

        union Choice {
            plain: String
            secret: Secret
            sealed: Sealed
            inner: Inner
        }

        @sensitive
        structure Sealed {
            note: String
            code: String
        }

        list SealedList {
            member: Sealed
        }

        @error("client")
        @httpError(403)
        structure VaultDenied {
            message: String
            secret: Secret
            envelope: Envelope
        }

        @error("client")
        @httpError(409)
        structure VaultSealed {
            message: Secret
        }
        """;

    private static final String S3_MODEL = """
        $version: "2"
        namespace com.amazonaws.s3

        @aws.protocols#restXml(noErrorWrapping: true)
        @aws.auth#sigv4(name: "s3")
        @aws.api#service(sdkId: "S3", endpointPrefix: "s3")
        service AmazonS3 {
            version: "2006-03-01"
            operations: [GetObject]
        }

        @readonly
        @http(method: "GET", uri: "/{Bucket}/{Key+}")
        operation GetObject {
            input := {
                @required
                @httpLabel
                Bucket: String

                @required
                @httpLabel
                Key: String

                @httpQuery("versionId")
                VersionId: String

                @httpHeader("x-amz-server-side-encryption-customer-key")
                SSECustomerKey: SSECustomerKey
            }
            output := {
                @httpHeader("ETag")
                ETag: String
            }
        }

        @sensitive
        string SSECustomerKey
        """;

    /** Compiled with the generated sources against their public API (and the builder state, to isolate it). */
    private static final String PROBE = """
        package example.probe;

        import java.lang.reflect.Field;
        import java.net.URI;
        import java.time.Duration;
        import java.util.concurrent.CompletableFuture;
        import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
        import software.amazon.awssdk.core.client.config.ConfigurationSources;
        import software.amazon.awssdk.http.SdkAsyncHttpClient;
        import software.amazon.awssdk.http.SdkSyncHttpClient;
        import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
        import software.amazon.awssdk.identity.spi.IdentityProvider;
        import software.amazon.awssdk.pipeline.client.AwsClientBuildState;
        import software.amazon.awssdk.regions.Region;
        import software.amazon.awssdk.retries.RetryStrategy;
        import software.amazon.awssdk.telemetry.spi.TelemetryConfiguration;
        import software.amazon.smithy.java.core.serde.document.Document;
        import VPKG.VaultAsyncClient;
        import VPKG.VaultAsyncClientBuilder;
        import VPKG.VaultClient;
        import VPKG.VaultClientBuilder;
        import VPKG.model.PutSecretRequest;
        import SPKG.presigner.S3Presigner;
        import SPKG.presigner.model.PresignedGetObjectRequest;

        public final class RedactionProbe {
            private static final URI ENDPOINT = URI.create("https://vault.example.test");

            private RedactionProbe() {
            }

            public static AutoCloseable syncClient(SdkSyncHttpClient http,
                                                   IdentityProvider<? extends AwsCredentialsIdentity> credentials,
                                                   RetryStrategy retryStrategy, TelemetryConfiguration telemetry)
                throws Exception {
                VaultClientBuilder builder = VaultClient.builder().region(Region.of("us-west-2"))
                    .endpointOverride(ENDPOINT).credentialsProvider(credentials).retryStrategy(retryStrategy)
                    .httpClient(http).telemetry(telemetry);
                isolate(builder);
                return builder.build();
            }

            public static AutoCloseable asyncClient(SdkAsyncHttpClient http,
                                                    IdentityProvider<? extends AwsCredentialsIdentity> credentials,
                                                    RetryStrategy retryStrategy, TelemetryConfiguration telemetry)
                throws Exception {
                VaultAsyncClientBuilder builder = VaultAsyncClient.builder().region(Region.of("us-west-2"))
                    .endpointOverride(ENDPOINT).credentialsProvider(credentials).retryStrategy(retryStrategy)
                    .httpClient(http).telemetry(telemetry);
                isolate(builder);
                return builder.build();
            }

            public static Object request(Document input) {
                return input.asShape(PutSecretRequest.builder());
            }

            public static Object call(Object client, Object request, AwsRequestOverrideConfiguration override) {
                return ((VaultClient) client).putSecret((PutSecretRequest) request, override);
            }

            public static CompletableFuture<?> callAsync(Object client, Object request,
                                                         AwsRequestOverrideConfiguration override) {
                return ((VaultAsyncClient) client).putSecret((PutSecretRequest) request, override);
            }

            public static Object[] presign(IdentityProvider<? extends AwsCredentialsIdentity> credentials,
                                           String customerKey) {
                try (S3Presigner presigner = S3Presigner.builder().region(Region.of("us-east-1"))
                         .credentialsProvider(credentials).endpointOverride(URI.create("https://s3.example.test"))
                         .build()) {
                    PresignedGetObjectRequest get = presigner.presignGetObject(
                        r -> r.signatureDuration(Duration.ofMinutes(10))
                              .getObjectRequest(g -> g.bucket("bucket").key("photos/a.jpg").versionId("v1")
                                                      .sseCustomerKey(customerKey)));
                    return new Object[] {get, get.httpRequest(), get.url(), get.signedHeaders()};
                }
            }

            /** Disables the external configuration sources, as the dynamic builder's configurationSources(none()). */
            private static void isolate(Object builder) throws Exception {
                Field state = builder.getClass().getDeclaredField("state");
                state.setAccessible(true);
                ((AwsClientBuildState) state.get(builder)).configurationSources(ConfigurationSources.none());
            }
        }
        """.replace("VPKG", VAULT_PACKAGE).replace("SPKG", S3_PACKAGE);

    /** The modeled body members of the input (and output) that may hold a canary, besides {@code label}. */
    private static final List<String> BODY_MEMBERS = List.of(
        "secret", "blob", "envelope", "secrets", "byName", "keyed", "envelopes", "envelopeMap", "choice", "sealed",
        "sealedList");
    /** Override headers whose names mark them sensitive. */
    private static final List<String> SENSITIVE_HEADERS = List.of(
        "Authorization", "Proxy-Authorization", "Cookie", "X-Amz-Security-Token",
        "x-amz-server-side-encryption-customer-key", "x-api-key");
    /** Raw query parameters of a presigned URL whose values are credentials. */
    private static final List<String> SENSITIVE_QUERY = List.of(
        "X-Amz-Signature", "X-Amz-Credential", "X-Amz-Security-Token");

    private static URLClassLoader loader;
    private static Method syncClient;
    private static Method asyncClient;
    private static Method request;
    private static Method call;
    private static Method callAsync;
    private static Method presign;
    private static DynamicModel runtimeModel;
    private static Path work;

    @BeforeContainer
    static void generateAndCompile() throws Exception {
        work = Files.createTempDirectory(Files.createDirectories(Path.of("target", "diagnostic-redaction")),
                                         "fixture-");
        Path vaultModels = Files.createDirectories(work.resolve("vault-model"));
        Path vaultModel = vaultModels.resolve("vault.smithy");
        Files.writeString(vaultModel, VAULT_MODEL, StandardCharsets.UTF_8);
        Path s3Models = Files.createDirectories(work.resolve("s3-model"));
        Files.writeString(s3Models.resolve("s3like.smithy"), S3_MODEL, StandardCharsets.UTF_8);
        AwsCodegenResult vault = AwsCodegen.generate(
            new AwsCodegenRequest(VAULT_ID.toString(), VAULT_PACKAGE, vaultModels, work.resolve("vault")), List.of());
        AwsCodegenResult s3 = AwsCodegen.generate(
            new AwsCodegenRequest("com.amazonaws.s3#AmazonS3", S3_PACKAGE, s3Models, work.resolve("s3")), List.of());
        Path sources = work.resolve("sources");
        ClientProjectionLifecyclePropertyTest.copyTree(vault.sourceRoot(), sources);
        ClientProjectionLifecyclePropertyTest.copyTree(s3.sourceRoot(), sources);
        Path probe = Files.createDirectories(sources.resolve("example/probe")).resolve("RedactionProbe.java");
        Files.writeString(probe, PROBE, StandardCharsets.UTF_8);
        loader = CompiledSources.compile(sources, work.resolve("classes")).loader();
        Class<?> probeClass = Class.forName("example.probe.RedactionProbe", true, loader);
        syncClient = probeClass.getMethod("syncClient", SdkSyncHttpClient.class, IdentityProvider.class,
                                          RetryStrategy.class, TelemetryConfiguration.class);
        asyncClient = probeClass.getMethod("asyncClient", SdkAsyncHttpClient.class, IdentityProvider.class,
                                           RetryStrategy.class, TelemetryConfiguration.class);
        request = probeClass.getMethod("request", Document.class);
        call = probeClass.getMethod("call", Object.class, Object.class, AwsRequestOverrideConfiguration.class);
        callAsync = probeClass.getMethod("callAsync", Object.class, Object.class,
                                         AwsRequestOverrideConfiguration.class);
        presign = probeClass.getMethod("presign", IdentityProvider.class, String.class);
        runtimeModel = DynamicModel.fromJsonAst(ClientProjectionLifecyclePropertyTest.jsonAst(vaultModel),
                                                "vault.json");
    }

    @AfterContainer
    static void close() throws Exception {
        if (loader != null) {
            loader.close();
        }
    }

    // Feature: aws-sdk-java-v3, Property 69: Sensitive data is absent from diagnostics
    @Property(tries = 200, edgeCases = EdgeCasesMode.NONE)
    void noCanaryOccursInAnyRenderedDiagnostic(@ForAll("scenarios") Scenario scenario) throws Exception {
        Outcome outcome = run(scenario);

        List<String> leaks = new ArrayList<>();
        for (Map.Entry<String, String> canary : scenario.canaries().placements().entrySet()) {
            List<String> forms = new ArrayList<>(List.of(canary.getKey()));
            forms.add(Base64.getEncoder().encodeToString(canary.getKey().getBytes(StandardCharsets.UTF_8)));
            for (String form : forms) {
                for (Rendering rendering : outcome.renderings()) {
                    if (rendering.wireValue() && scenario.canaries().labels().contains(canary.getKey())) {
                        // Label values are part of the encoded path an HTTP-level request renders (see Rendering).
                        continue;
                    }
                    int at = rendering.text().indexOf(form);
                    if (at >= 0) {
                        leaks.add("the canary of " + canary.getValue() + " (" + form + ") occurs in "
                                  + rendering.source() + ": ..." + excerpt(rendering.text(), at, form.length())
                                  + "...");
                    }
                }
            }
        }
        if (!leaks.isEmpty()) {
            fail(leaks.size() + " sensitive value(s) rendered for " + scenario + ":\n" + String.join("\n", leaks));
        }

        // Non-vacuity: the canaries reached the code that renders them.
        if (!outcome.wire().isEmpty()) {
            for (String canary : scenario.inputCanaries()) {
                String encoded = Base64.getEncoder().encodeToString(canary.getBytes(StandardCharsets.UTF_8));
                assertTrue(outcome.wire().contains(canary) || outcome.wire().contains(encoded),
                           "the input canary of " + scenario.canaries().placements().get(canary)
                           + " was never transmitted:\n" + outcome.wire());
            }
            if (scenario.logLevel().intValue() <= Level.FINE.intValue()) {
                assertTrue(outcome.requestLogged(), "DEBUG is enabled but nothing was logged under " + REQUEST_LOGGER);
            }
        }
        assertFalse(outcome.renderings().isEmpty());

        Statistics.label("projection").collect(scenario.projection());
        Statistics.label("projection").coverage(c -> {
            for (Projection projection : Projection.values()) {
                c.check(projection).percentage(p -> p >= 15);
            }
        });
        Statistics.label("credentials").collect(scenario.credentials().kind());
        Statistics.label("credentials").coverage(c -> {
            for (CredentialKind kind : CredentialKind.values()) {
                c.check(kind).count(n -> n >= 3);
            }
        });
        Statistics.label("terminal").collect(outcome.terminal());
        Statistics.label("terminal").coverage(c -> {
            c.check("success").percentage(p -> p >= 10);
            c.check("VaultDenied").percentage(p -> p >= 5);
            c.check("VaultSealed").percentage(p -> p >= 5);
            c.check("credentials failure").percentage(p -> p >= 10);
        });
        Statistics.label("reached transport").collect(!outcome.wire().isEmpty());
        Statistics.label("reached transport").coverage(c -> c.check(true).percentage(p -> p >= 40));
        Statistics.label("log level").collect(scenario.logLevel().getName());
        Statistics.label("sso token").collect(scenario.token().kind());
        Statistics.label("presigned").collect(scenario.presign());
        Statistics.label("canaries").collect(bucket(scenario.canaries().placements().size()));
    }

    // -------------------------------------------------------------------------------------------------------------
    // One try

    private static Outcome run(Scenario scenario) throws Exception {
        Path tryDirectory = Files.createTempDirectory(work, "try-");
        Renderings out = new Renderings();
        List<Object> records = new CopyOnWriteArrayList<>();
        LogCapture logs = LogCapture.start(scenario.logLevel());
        String terminal;
        RedactionHttp http = new RedactionHttp(scenario);
        try {
            IdentityProvider<? extends AwsCredentialsIdentity> credentials =
                scenario.credentials().provider(tryDirectory);
            out.add("credentials provider", credentials);
            renderResolution(out, "credentials", credentials);
            renderToken(out, scenario.token(), tryDirectory);

            AwsRequestOverrideConfiguration override = scenario.override(scenario.credentialsOnOverride()
                                                                         ? credentials : null);
            out.add("override configuration", override);
            IdentityProvider<? extends AwsCredentialsIdentity> clientCredentials =
                scenario.credentialsOnOverride()
                ? StaticCredentialsProvider.create(AwsCredentialsIdentity.create("AKIDCLIENT", "client-key"))
                : credentials;
            TelemetryPublisher publisher = new TelemetryPublisher() {
                @Override
                public void attemptCompleted(AttemptRecord attempt) {
                    records.add(attempt);
                }

                @Override
                public void callCompleted(CallRecord record) {
                    records.add(record);
                }
            };
            TelemetryConfiguration telemetry =
                TelemetryConfiguration.builder().addPublisher(publisher)
                                      .addMetricPublisher(LoggingMetricPublisher.create(
                                          LoggingMetricPublisher.LogLevel.DEBUG,
                                          LoggingMetricPublisher.Format.PRETTY))
                                      .build();
            RetryStrategy retryStrategy = StandardRetryStrategy.builder().maxAttempts(scenario.replies().size())
                                                               .backoffStrategy(BackoffStrategy.none())
                                                               .throttlingBackoffStrategy(BackoffStrategy.none())
                                                               .build();
            Document input = ClientProjectionLifecyclePropertyTest.toDocument(scenario.input());
            terminal = callVault(scenario, out, http, clientCredentials, retryStrategy, telemetry, input, override);

            if (scenario.presign()) {
                renderPresign(out, credentials, scenario.customerKey());
            }
            renderRedactionUtilities(out, scenario);
            if (credentials instanceof AutoCloseable closeable) {
                closeable.close();
            }
        } finally {
            logs.stop();
        }
        out.addAll("log record", logs.formatted());
        records.forEach(r -> out.add("telemetry record", r));
        for (SdkHttpRequest sent : http.requests) {
            out.addWireValue("transmitted request", sent);
            out.add("transmitted request headers", sent.headers());
        }
        http.responses.forEach(r -> out.add("received response", r));
        boolean requestLogged = logs.records.stream().anyMatch(r -> REQUEST_LOGGER.equals(r.getLoggerName()));
        return new Outcome(out.list, http.wire(), terminal, requestLogged);
    }

    /** Calls PutSecret through the scenario's projection; renders its result or failure; returns the terminal kind. */
    private static String callVault(Scenario scenario, Renderings out, RedactionHttp http,
                                    IdentityProvider<? extends AwsCredentialsIdentity> credentials,
                                    RetryStrategy retryStrategy, TelemetryConfiguration telemetry, Document input,
                                    AwsRequestOverrideConfiguration override) throws Exception {
        if (scenario.projection() == Projection.DYNAMIC_SYNC) {
            try (DynamicClient client = DynamicClient.builder().model(runtimeModel).serviceId(VAULT_ID)
                                                     .region(Region.of("us-west-2")).endpointOverride(ENDPOINT)
                                                     .credentialsProvider(credentials).retryStrategy(retryStrategy)
                                                     .configurationSources(ConfigurationSources.none())
                                                     .httpClient(http).build()) {
                out.add("dynamic client", client);
                try {
                    Document output = client.call("PutSecret", input, override);
                    out.add("dynamic output document", output);
                    return "success";
                } catch (RuntimeException e) {
                    return renderFailure(out, e);
                }
            }
        }
        Object modeledRequest = invoke(request, input);
        renderGenerated(out, "modeled request", modeledRequest);
        boolean async = scenario.projection() == Projection.GENERATED_ASYNC;
        try (AutoCloseable client = (AutoCloseable) invoke(async ? asyncClient : syncClient, http, credentials,
                                                           retryStrategy, telemetry)) {
            out.add("generated client", client);
            try {
                Object response = async
                                  ? ((CompletableFuture<?>) invoke(callAsync, client, modeledRequest, override))
                                      .get(30, TimeUnit.SECONDS)
                                  : invoke(call, client, modeledRequest, override);
                renderGenerated(out, "modeled response", response);
                return "success";
            } catch (InvocationTargetException e) {
                return renderFailure(out, e.getCause());
            } catch (ExecutionException e) {
                return renderFailure(out, e.getCause());
            }
        }
    }

    private static String renderFailure(Renderings out, Throwable failure) {
        out.addThrowable("call failure", failure);
        if (failure instanceof AwsServiceException service) {
            out.add("error details", service.awsErrorDetails());
            if (service.awsErrorDetails() != null) {
                out.add("error details response", service.awsErrorDetails().sdkHttpResponse());
            }
            renderGenerated(out, "modeled error", failure);
            if (failure instanceof DynamicServiceException dynamic) {
                out.add("dynamic error document", dynamic.error());
            }
            String code = service.awsErrorDetails() == null ? null : service.awsErrorDetails().errorCode();
            return "VaultDenied".equals(code) || "VaultSealed".equals(code) ? code : "service error " + code;
        }
        String message = String.valueOf(failure.getMessage());
        if (message.contains("credentials") || message.contains("Credential") || message.contains("credential")) {
            return "credentials failure";
        }
        return failure.getClass().getSimpleName();
    }

    /**
     * Renders {@code value} (a generated model value or exception) and, recursively, every generated structure, union,
     * list or map of them its accessors return, plus its response metadata and HTTP response. Raw scalar values
     * (strings, bytes) are what a caller reads explicitly, so they are not renderings.
     */
    private static void renderGenerated(Renderings out, String source, Object value) {
        renderGenerated(out, source, value, 0);
    }

    private static void renderGenerated(Renderings out, String source, Object value, int depth) {
        if (value == null || depth > 6) {
            return;
        }
        if (value instanceof Collection<?> collection) {
            if (collection.stream().allMatch(SensitiveDiagnosticsRedactionPropertyTest::isGenerated)) {
                out.add(source, value);
                collection.forEach(element -> renderGenerated(out, source + "[]", element, depth + 1));
            }
            return;
        }
        if (value instanceof Map<?, ?> map) {
            if (map.values().stream().allMatch(SensitiveDiagnosticsRedactionPropertyTest::isGenerated)) {
                // The keys of a map are rendered only where they are not sensitive; the map's own toString would
                // render raw keys, so only the values are rendered.
                map.values().forEach(element -> renderGenerated(out, source + "{}", element, depth + 1));
            }
            return;
        }
        if (!isGenerated(value)) {
            return;
        }
        out.add(source, value);
        if (!(value instanceof Throwable)) {
            Object builder = accessor(value, "toBuilder");
            if (builder != null) {
                out.add(source + " builder", builder);
            }
        }
        for (Method method : value.getClass().getMethods()) {
            if (method.getParameterCount() != 0 || Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            String name = method.getName();
            if (Set.of("sdkHttpResponse", "responseMetadata").contains(name)) {
                out.add(source + "." + name, accessor(value, name));
                continue;
            }
            if (Set.of("toBuilder", "hashCode", "toString", "getClass", "copy", "getCause", "getStackTrace",
                       "getSuppressed", "fillInStackTrace", "getLocalizedMessage", "getMessage").contains(name)) {
                continue;
            }
            Class<?> type = method.getReturnType();
            if (isGeneratedType(type) || Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)
                || Optional.class.isAssignableFrom(type)) {
                Object result = accessor(value, name);
                if (result instanceof Optional<?> optional) {
                    result = optional.orElse(null);
                }
                renderGenerated(out, source + "." + name, result, depth + 1);
            }
        }
    }

    private static boolean isGenerated(Object value) {
        return value != null && isGeneratedType(value.getClass());
    }

    private static boolean isGeneratedType(Class<?> type) {
        return type.getName().startsWith(VAULT_PACKAGE) || type.getName().startsWith(S3_PACKAGE);
    }

    private static Object accessor(Object value, String name) {
        try {
            Method method = value.getClass().getMethod(name);
            method.setAccessible(true);
            return method.invoke(value);
        } catch (NoSuchMethodException e) {
            return null;
        } catch (InvocationTargetException e) {
            return null;
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    /** Resolves an identity directly and renders the identity or the provider's failure. */
    private static void renderResolution(Renderings out, String source, IdentityProvider<?> provider) {
        try {
            out.add(source + " identity", provider.resolveIdentity().join());
        } catch (CompletionException e) {
            out.addThrowable(source + " failure", e.getCause() == null ? e : e.getCause());
        } catch (RuntimeException e) {
            out.addThrowable(source + " failure", e);
        }
    }

    private static void renderToken(Renderings out, TokenPlan token, Path directory) throws IOException {
        if (token.kind() == TokenKind.NONE) {
            return;
        }
        Path cache = Files.createDirectories(directory.resolve("sso-cache"));
        try (SsoTokenProvider provider = SsoTokenProvider.builder().startUrl("https://start.example.test/start")
                                                         .cacheDirectory(cache)
                                                         .clock(Clock.fixed(NOW, ZoneOffset.UTC)).build()) {
            Files.writeString(provider.cacheFile(), token.cacheContent(), StandardCharsets.UTF_8);
            out.add("token provider", provider);
            renderResolution(out, "token", provider);
        }
    }

    private static void renderPresign(Renderings out, IdentityProvider<? extends AwsCredentialsIdentity> credentials,
                                      String customerKey) {
        try {
            Object[] presigned = (Object[]) invoke(presign, credentials, customerKey);
            out.addWireValue("presigned request", presigned[0]);
            out.addWireValue("presigned http request", presigned[1]);
            out.add("presigned http request headers", ((SdkHttpRequest) presigned[1]).headers());
            // The generated presigned request's url() is a java.net.URL, as in V2.
            out.add("redacted presigned url", Redaction.uri(URI.create(String.valueOf(presigned[2]))));
            @SuppressWarnings("unchecked")
            Map<String, List<String>> signedHeaders = (Map<String, List<String>>) presigned[3];
            out.add("redacted presigned headers", Redaction.headers(signedHeaders));
        } catch (InvocationTargetException e) {
            out.addThrowable("presign failure", e.getCause());
        }
    }

    /** The {@link Redaction} renderings, and an SDK HTTP request built with the sensitive values, rendered. */
    private static void renderRedactionUtilities(Renderings out, Scenario scenario) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        scenario.headers().forEach((name, value) -> headers.put(name, List.of(value)));
        headers.put("x-visible", List.of("visible"));
        out.add("redacted headers", Redaction.headers(headers));
        StringBuilder query = new StringBuilder("X-Amz-Algorithm=AWS4-HMAC-SHA256");
        scenario.query().forEach((name, value) -> query.append('&').append(name).append('=').append(value));
        out.add("redacted query", Redaction.query(query.toString()));
        Map<String, List<String>> parameters = new LinkedHashMap<>();
        scenario.query().forEach((name, value) -> parameters.put(name, List.of(value)));
        out.add("redacted query parameters", Redaction.queryParameters(parameters));
        String userInfo = scenario.canaries().userInfo();
        out.add("redacted uri", Redaction.uri(URI.create("https://user:" + userInfo + "@vault.example.test/v?"
                                                         + query)));
        scenario.headers().forEach((name, value) -> out.add("redacted value " + name, Redaction.value(name, value)));
        out.add("redacted secret", Redaction.secret(userInfo));
        SdkHttpRequest.Builder built = SdkHttpRequest.builder().method("GET").host("vault.example.test")
                                                     .encodedPath("/v");
        scenario.headers().forEach(built::putHeader);
        scenario.query().forEach(built::addRawQueryParameter);
        SdkHttpRequest sdkRequest = built.build();
        out.add("built SdkHttpRequest", sdkRequest);
        HttpHeaders.Builder builtHeaders = HttpHeaders.builder();
        scenario.headers().forEach(builtHeaders::add);
        out.add("built HttpHeaders", builtHeaders.build());
    }

    private static Object invoke(Method method, Object... arguments) throws InvocationTargetException {
        try {
            return method.invoke(null, arguments);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private static String excerpt(String text, int at, int length) {
        int from = Math.max(0, at - 80);
        int to = Math.min(text.length(), at + length + 80);
        return text.substring(from, to).replace("\n", "\\n");
    }

    private static String bucket(int count) {
        return count < 10 ? "<10" : count < 25 ? "10-24" : count < 50 ? "25-49" : ">=50";
    }

    // -------------------------------------------------------------------------------------------------------------
    // Scenarios

    enum Projection {
        GENERATED_SYNC, GENERATED_ASYNC, DYNAMIC_SYNC
    }

    /** A scripted reply of one attempt. */
    enum Reply {
        SUCCESS, DENIED, SEALED, SERVER_ERROR, TRANSPORT_FAILURE;

        boolean retryable() {
            return this == SERVER_ERROR || this == TRANSPORT_FAILURE;
        }
    }

    /** Where the credentials come from, and whether the source holds a secret but cannot produce credentials. */
    enum CredentialKind {
        STATIC_BASIC, STATIC_SESSION, SYSTEM_PROPERTIES, ENVIRONMENT, PROFILE, PROCESS, CHAIN_FALLBACK,
        FAILING_SYSTEM_PROPERTIES, FAILING_PROFILE, FAILING_PROCESS_MISSING_KEY_ID, FAILING_PROCESS_MALFORMED,
        FAILING_PROCESS_EXIT, FAILING_PROCESS_VERSION, FAILING_CHAIN
    }

    enum TokenKind {
        NONE, VALID, EXPIRED, INCOMPLETE, MALFORMED
    }

    /** A credentials source holding the canaries {@code secret} and {@code token}. */
    record CredentialPlan(CredentialKind kind, String secret, String token) {
        IdentityProvider<? extends AwsCredentialsIdentity> provider(Path directory) {
            return switch (kind) {
                case STATIC_BASIC -> StaticCredentialsProvider.create(
                    AwsCredentialsIdentity.create("AKIDSTATIC", secret));
                case STATIC_SESSION -> StaticCredentialsProvider.create(
                    AwsSessionCredentialsIdentity.create("AKIDSTATIC", secret, token));
                case SYSTEM_PROPERTIES -> SystemPropertyCredentialsProvider.create(SettingSource.fromMap(Map.of(
                    "aws.accessKeyId", "AKIDPROPERTY", "aws.secretAccessKey", secret, "aws.sessionToken", token)));
                case ENVIRONMENT -> EnvironmentVariableCredentialsProvider.create(SettingSource.fromMap(Map.of(
                    "AWS_ACCESS_KEY_ID", "AKIDENVIRONMENT", "AWS_SECRET_ACCESS_KEY", secret,
                    "AWS_SESSION_TOKEN", token)));
                case PROFILE -> profile("aws_access_key_id = AKIDPROFILE\naws_secret_access_key = " + secret
                                        + "\naws_session_token = " + token + "\n");
                case PROCESS -> process("{\"Version\":1,\"AccessKeyId\":\"AKIDPROCESS\",\"SecretAccessKey\":\""
                                        + secret + "\",\"SessionToken\":\"" + token + "\"}", 0);
                case CHAIN_FALLBACK -> AwsCredentialsProviderChain.of(
                    SystemPropertyCredentialsProvider.create(SettingSource.fromMap(Map.of(
                        "aws.secretAccessKey", token))),
                    StaticCredentialsProvider.create(AwsSessionCredentialsIdentity.create("AKIDCHAIN", secret,
                                                                                          token)));
                case FAILING_SYSTEM_PROPERTIES -> SystemPropertyCredentialsProvider.create(SettingSource.fromMap(
                    Map.of("aws.secretAccessKey", secret, "aws.sessionToken", token)));
                case FAILING_PROFILE -> profile("aws_secret_access_key = " + secret + "\naws_session_token = "
                                                + token + "\n");
                case FAILING_PROCESS_MISSING_KEY_ID -> process("{\"Version\":1,\"SecretAccessKey\":\"" + secret
                                                               + "\",\"SessionToken\":\"" + token + "\"}", 0);
                case FAILING_PROCESS_MALFORMED -> process("{\"Version\":1,\"AccessKeyId\":\"AKIDPROCESS\","
                                                          + "\"SecretAccessKey\":\"" + secret + "\" \"" + token, 0);
                case FAILING_PROCESS_EXIT -> process("{\"Version\":1,\"AccessKeyId\":\"AKIDPROCESS\","
                                                     + "\"SecretAccessKey\":\"" + secret + "\",\"SessionToken\":\""
                                                     + token + "\"}", 3);
                case FAILING_PROCESS_VERSION -> process("{\"Version\":2,\"AccessKeyId\":\"AKIDPROCESS\","
                                                        + "\"SecretAccessKey\":\"" + secret + "\",\"SessionToken\":\""
                                                        + token + "\"}", 0);
                case FAILING_CHAIN -> AwsCredentialsProviderChain.of(
                    SystemPropertyCredentialsProvider.create(SettingSource.fromMap(Map.of(
                        "aws.secretAccessKey", secret))),
                    EnvironmentVariableCredentialsProvider.create(SettingSource.fromMap(Map.of(
                        "AWS_SECRET_ACCESS_KEY", token))),
                    profile("aws_secret_access_key = " + secret + "\n"));
            };
        }

        boolean fails() {
            return kind.name().startsWith("FAILING");
        }

        private static ProfileCredentialsProvider profile(String properties) {
            return ProfileCredentialsProvider.builder()
                                             .profileFile(ProfileFile.parse("[default]\n" + properties,
                                                                            ProfileFile.Type.CREDENTIALS))
                                             .profileName("default")
                                             .settings(new SystemSettings(SettingSource.empty(), SettingSource.empty()))
                                             .build();
        }

        private static ProcessCredentialsProvider process(String output, int exit) {
            // Canaries and the fixed JSON contain no single quote, so the output is one shell word.
            return ProcessCredentialsProvider.builder().command("printf '%s' '" + output + "'; exit " + exit)
                                             .build();
        }
    }

    /** An SSO token cache file holding the canary {@code token}. */
    record TokenPlan(TokenKind kind, String token, String refresh) {
        String cacheContent() {
            return switch (kind) {
                case NONE -> "";
                case VALID -> "{\"accessToken\":\"" + token + "\",\"expiresAt\":\"" + NOW.plus(Duration.ofHours(1))
                              + "\",\"refreshToken\":\"" + refresh + "\"}";
                case EXPIRED -> "{\"accessToken\":\"" + token + "\",\"expiresAt\":\""
                                + NOW.minus(Duration.ofHours(1)) + "\",\"refreshToken\":\"" + refresh + "\"}";
                case INCOMPLETE -> "{\"accessToken\":\"" + token + "\",\"refreshToken\":\"" + refresh + "\"}";
                case MALFORMED -> "{\"accessToken\":\"" + token + "\" \"refreshToken\":\"" + refresh + "\"";
            };
        }
    }

    /** Allocates unique canaries and records where each was placed. */
    static final class Canaries {
        private final String base;
        private final Map<String, String> placements = new LinkedHashMap<>();
        private final Set<String> labels = new HashSet<>();
        private final String userInfo;

        Canaries(String base) {
            this.base = base;
            this.userInfo = next("URI user information");
        }

        String next(String placement) {
            String canary = String.format("zq%s%03d", base, placements.size());
            placements.put(canary, placement);
            return canary;
        }

        Map<String, String> placements() {
            return placements;
        }

        /** The canaries placed in {@code @httpLabel} members. */
        Set<String> labels() {
            return labels;
        }

        String userInfo() {
            return userInfo;
        }
    }

    /** The choices of one try; the canaries are allocated from them by {@link #of}. */
    record Choices(String base, Projection projection, Set<String> members, String choiceVariant,
                   int collectionSize, boolean sensitiveLabel, boolean pin, boolean code, Set<String> headers,
                   Set<String> query, CredentialKind credentials, boolean credentialsOnOverride, TokenKind token,
                   List<Reply> replies, Level logLevel, boolean presign) {
    }

    record Scenario(Choices choices, Canaries canaries, Map<String, Object> input, List<String> inputCanaries,
                    Map<String, String> headers, Map<String, String> query, CredentialPlan credentials,
                    TokenPlan token, String customerKey, Map<Reply, Map<String, String>> replyCanaries) {

        static Scenario of(Choices choices) {
            Canaries canaries = new Canaries(choices.base());
            List<String> inputCanaries = new ArrayList<>();
            InputBuilder in = new InputBuilder(canaries, inputCanaries, choices.collectionSize());
            Map<String, Object> input = new LinkedHashMap<>();
            input.put("vault", "main");
            if (choices.sensitiveLabel()) {
                String label = in.secret("secretName (@sensitive @httpLabel)");
                canaries.labels.add(label);
                input.put("secretName", label);
            } else {
                input.put("secretName", "plain-name");
            }
            if (choices.pin()) {
                input.put("pin", in.secret("pin (@httpHeader x-vault-pin)"));
            }
            if (choices.code()) {
                input.put("code", in.secret("code (@httpQuery code)"));
            }
            input.put("page", "1");
            input.put("label", "visible-label");
            for (String member : BODY_MEMBERS) {
                if (choices.members().contains(member)) {
                    input.put(member, in.member(member, member, choices.choiceVariant()));
                }
            }
            Map<String, String> headers = new LinkedHashMap<>();
            for (String header : choices.headers()) {
                String value = canaries.next("override header " + header);
                headers.put(header, header.startsWith("Authorization") ? "Bearer " + value : value);
            }
            Map<String, String> query = new LinkedHashMap<>();
            for (String parameter : choices.query()) {
                query.put(parameter, canaries.next("override raw query parameter " + parameter));
            }
            CredentialPlan credentials = new CredentialPlan(choices.credentials(),
                                                            canaries.next("secret access key"),
                                                            canaries.next("session token"));
            TokenPlan token = new TokenPlan(choices.token(), canaries.next("SSO access token"),
                                            canaries.next("SSO refresh token"));
            String customerKey = canaries.next("presigned SSE-C customer key (@sensitive @httpHeader)");
            Map<Reply, Map<String, String>> replyCanaries = new LinkedHashMap<>();
            for (Reply reply : Set.copyOf(choices.replies())) {
                Map<String, String> values = new LinkedHashMap<>();
                values.put("Set-Cookie", canaries.next(reply + " response Set-Cookie header"));
                values.put("x-amz-security-token", canaries.next(reply + " response x-amz-security-token header"));
                if (reply == Reply.DENIED) {
                    values.put("secret", canaries.next("VaultDenied.secret"));
                    values.put("envelope", canaries.next("VaultDenied.envelope.secret"));
                } else if (reply == Reply.SEALED) {
                    values.put("message", canaries.next("VaultSealed.message (@sensitive message)"));
                }
                replyCanaries.put(reply, values);
            }
            return new Scenario(choices, canaries, input, inputCanaries, headers, query, credentials, token,
                                customerKey, replyCanaries);
        }

        Projection projection() {
            return choices.projection();
        }

        List<Reply> replies() {
            return choices.replies();
        }

        Level logLevel() {
            return choices.logLevel();
        }

        boolean presign() {
            return choices.presign();
        }

        boolean credentialsOnOverride() {
            return choices.credentialsOnOverride();
        }

        AwsRequestOverrideConfiguration override(IdentityProvider<? extends AwsCredentialsIdentity> credentials) {
            AwsRequestOverrideConfiguration.Builder builder = AwsRequestOverrideConfiguration.builder();
            headers.forEach(builder::putHeader);
            query.forEach(builder::putRawQueryParameter);
            if (credentials != null) {
                builder.credentialsProvider(credentials);
            }
            return builder.build();
        }

        @Override
        public String toString() {
            return "Scenario" + choices;
        }
    }

    /** Builds the modeled input value of a member, allocating a canary for every sensitive position. */
    private static final class InputBuilder {
        private final Canaries canaries;
        private final List<String> inputCanaries;
        private final int size;

        InputBuilder(Canaries canaries, List<String> inputCanaries, int size) {
            this.canaries = canaries;
            this.inputCanaries = inputCanaries;
            this.size = size;
        }

        String secret(String placement) {
            String canary = canaries.next(placement);
            inputCanaries.add(canary);
            return canary;
        }

        Object member(String member, String path, String choiceVariant) {
            return switch (member) {
                case "secret" -> secret(path + " (@sensitive string)");
                case "blob" -> secret(path + " (@sensitive blob)").getBytes(StandardCharsets.UTF_8);
                case "envelope" -> envelope(path, choiceVariant);
                case "secrets" -> list(i -> secret(path + "[" + i + "] (list of @sensitive)"));
                case "byName" -> map(i -> "k" + i, i -> secret(path + "{k" + i + "} (map value @sensitive)"));
                case "keyed" -> map(i -> secret(path + " key " + i + " (map key @sensitive)"), i -> "v" + i);
                case "envelopes" -> list(i -> envelope(path + "[" + i + "]", choiceVariant));
                case "envelopeMap" -> map(i -> "e" + i, i -> envelope(path + "{e" + i + "}", choiceVariant));
                case "choice" -> choice(path, choiceVariant);
                case "sealed" -> sealed(path);
                case "sealedList" -> list(i -> sealed(path + "[" + i + "]"));
                default -> throw new IllegalArgumentException(member);
            };
        }

        private Map<String, Object> envelope(String path, String choiceVariant) {
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("label", "envelope-label");
            envelope.put("secret", secret(path + ".secret (nested @sensitive)"));
            envelope.put("inner", inner(path + ".inner"));
            envelope.put("choice", choice(path + ".choice", choiceVariant));
            return envelope;
        }

        private Map<String, Object> inner(String path) {
            Map<String, Object> inner = new LinkedHashMap<>();
            inner.put("secrets", list(i -> secret(path + ".secrets[" + i + "]")));
            inner.put("byName", map(i -> "n" + i, i -> secret(path + ".byName{n" + i + "}")));
            return inner;
        }

        private Map<String, Object> choice(String path, String variant) {
            return switch (variant) {
                case "secret" -> Map.of("secret", secret(path + ".secret (union @sensitive member)"));
                case "sealed" -> Map.of("sealed", sealed(path + ".sealed (union @sensitive structure)"));
                case "inner" -> Map.of("inner", inner(path + ".inner (union structure)"));
                default -> Map.of("plain", "plain-choice");
            };
        }

        private Map<String, Object> sealed(String path) {
            Map<String, Object> sealed = new LinkedHashMap<>();
            sealed.put("note", secret(path + ".note (member of @sensitive structure)"));
            sealed.put("code", secret(path + ".code (member of @sensitive structure)"));
            return sealed;
        }

        private List<Object> list(IntFunction<Object> element) {
            List<Object> list = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                list.add(element.apply(i));
            }
            return list;
        }

        private Map<String, Object> map(IntFunction<String> key,
                                        IntFunction<Object> value) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (int i = 0; i < size; i++) {
                map.put(key.apply(i), value.apply(i));
            }
            return map;
        }
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        Arbitrary<List<Reply>> replies = Arbitraries.frequencyOf(
            Tuple.of(4, Arbitraries.just(Reply.SUCCESS)),
            Tuple.of(2, Arbitraries.just(Reply.DENIED)),
            Tuple.of(2, Arbitraries.just(Reply.SEALED)),
            Tuple.of(1, Arbitraries.just(Reply.SERVER_ERROR)),
            Tuple.of(1, Arbitraries.just(Reply.TRANSPORT_FAILURE)))
            .list().ofMinSize(1).ofMaxSize(3)
            .map(SensitiveDiagnosticsRedactionPropertyTest::untilTerminal);
        Arbitrary<Level> logLevel = Arbitraries.frequencyOf(
            Tuple.of(5, Arbitraries.just(Level.ALL)),
            Tuple.of(2, Arbitraries.just(Level.FINE)),
            Tuple.of(1, Arbitraries.just(Level.WARNING)),
            Tuple.of(1, Arbitraries.just(Level.SEVERE)));
        return Builders.withBuilder(() -> new ChoicesBuilder())
                       .use(Arbitraries.strings().withCharRange('a', 'z').ofLength(8)).inSetter((b, v) -> b.base = v)
                       .use(Arbitraries.of(Projection.class)).inSetter((b, v) -> b.projection = v)
                       .use(Arbitraries.subsetOf(BODY_MEMBERS).ofMinSize(1)).inSetter((b, v) -> b.members = v)
                       .use(Arbitraries.of("plain", "secret", "sealed", "inner")).inSetter((b, v) -> b.choiceVariant = v)
                       .use(Arbitraries.integers().between(1, 2)).inSetter((b, v) -> b.collectionSize = v)
                       .use(Arbitraries.of(true, false)).inSetter((b, v) -> b.sensitiveLabel = v)
                       .use(Arbitraries.of(true, false)).inSetter((b, v) -> b.pin = v)
                       .use(Arbitraries.of(true, false)).inSetter((b, v) -> b.code = v)
                       .use(Arbitraries.subsetOf(SENSITIVE_HEADERS)).inSetter((b, v) -> b.headers = v)
                       .use(Arbitraries.subsetOf(SENSITIVE_QUERY)).inSetter((b, v) -> b.query = v)
                       .use(Arbitraries.of(CredentialKind.class)).inSetter((b, v) -> b.credentials = v)
                       .use(Arbitraries.of(true, false)).inSetter((b, v) -> b.credentialsOnOverride = v)
                       .use(Arbitraries.of(TokenKind.class)).inSetter((b, v) -> b.token = v)
                       .use(replies).inSetter((b, v) -> b.replies = v)
                       .use(logLevel).inSetter((b, v) -> b.logLevel = v)
                       .use(Arbitraries.of(true, false)).inSetter((b, v) -> b.presign = v)
                       .build(b -> Scenario.of(b.build()));
    }

    /** A script ends at its first non-retryable reply: later attempts never happen. */
    private static List<Reply> untilTerminal(List<Reply> replies) {
        List<Reply> script = new ArrayList<>();
        for (Reply reply : replies) {
            script.add(reply);
            if (!reply.retryable()) {
                break;
            }
        }
        return List.copyOf(script);
    }

    private static final class ChoicesBuilder {
        String base;
        Projection projection;
        Set<String> members;
        String choiceVariant;
        int collectionSize;
        boolean sensitiveLabel;
        boolean pin;
        boolean code;
        Set<String> headers;
        Set<String> query;
        CredentialKind credentials;
        boolean credentialsOnOverride;
        TokenKind token;
        List<Reply> replies;
        Level logLevel;
        boolean presign;

        Choices build() {
            return new Choices(base, projection, members, choiceVariant, collectionSize, sensitiveLabel, pin, code,
                               headers, query, credentials, credentialsOnOverride, token, replies, logLevel, presign);
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Outcome, renderings and capture

    /**
     * One rendering. A wire value is the {@code toString} of an HTTP-level request ({@link SdkHttpRequest},
     * presigned request): the HTTP SPI has no model, so it renders the encoded path, which holds label values.
     */
    record Rendering(String source, String text, boolean wireValue) {
    }

    record Outcome(List<Rendering> renderings, String wire, String terminal, boolean requestLogged) {
    }

    /** Every rendering of a try, labeled by its source. */
    private static final class Renderings {
        final List<Rendering> list = new ArrayList<>();

        void add(String source, Object value) {
            if (value != null) {
                list.add(new Rendering(source, String.valueOf(value), false));
            }
        }

        void addWireValue(String source, Object value) {
            if (value != null) {
                list.add(new Rendering(source, String.valueOf(value), true));
            }
        }

        void addAll(String source, List<String> values) {
            values.forEach(v -> add(source, v));
        }

        /** The message, {@code toString} and full stack trace (causes and suppressed included) of a failure. */
        void addThrowable(String source, Throwable failure) {
            add(source + " message", failure.getMessage());
            add(source + " toString", failure);
            StringWriter trace = new StringWriter();
            failure.printStackTrace(new PrintWriter(trace));
            add(source + " stack trace", trace);
        }
    }

    /** Captures every record of the {@code software.amazon.awssdk} loggers at or above a level. */
    private static final class LogCapture {
        private final Logger logger = Logger.getLogger("software.amazon.awssdk");
        private final Level previousLevel;
        private final boolean previousUseParent;
        final List<LogRecord> records = new CopyOnWriteArrayList<>();
        private final Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };

        private LogCapture(Level level) {
            previousLevel = logger.getLevel();
            previousUseParent = logger.getUseParentHandlers();
            handler.setLevel(Level.ALL);
            logger.setLevel(level);
            logger.setUseParentHandlers(false);
            logger.addHandler(handler);
        }

        static LogCapture start(Level level) {
            return new LogCapture(level);
        }

        void stop() {
            logger.removeHandler(handler);
            logger.setLevel(previousLevel);
            logger.setUseParentHandlers(previousUseParent);
        }

        /** Each record as the default formatter writes it: logger, level, message and throwable stack trace. */
        List<String> formatted() {
            SimpleFormatter formatter = new SimpleFormatter();
            List<String> out = new ArrayList<>();
            for (LogRecord record : records) {
                out.add(record.getLoggerName() + " " + formatter.format(record));
            }
            return out;
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Transport

    /**
     * A sync and async transport that answers attempt {@code n} with the scenario's {@code n}-th reply: a success
     * echoing the request body and its sensitive {@code x-vault-pin} header (as {@code x-vault-echo}), a modeled
     * error with sensitive members, an unmodeled 500, or a retryable transport failure. Every reply carries sensitive
     * {@code Set-Cookie} and {@code x-amz-security-token} headers.
     */
    static final class RedactionHttp implements SdkSyncHttpClient, SdkAsyncHttpClient {
        final List<SdkHttpRequest> requests = new CopyOnWriteArrayList<>();
        final List<SdkHttpResponse> responses = new CopyOnWriteArrayList<>();
        private final List<String> bodies = new CopyOnWriteArrayList<>();
        private final Scenario scenario;
        private final AtomicInteger attempts = new AtomicInteger();

        RedactionHttp(Scenario scenario) {
            this.scenario = scenario;
        }

        /** Every transmitted request: method, raw target, header fields and body. */
        String wire() {
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < requests.size(); i++) {
                SdkHttpRequest request = requests.get(i);
                out.append(request.method()).append(' ').append(request.rawTarget()).append('\n');
                HttpHeaders headers = request.headers();
                for (int h = 0; h < headers.size(); h++) {
                    out.append(headers.name(h)).append(": ").append(headers.value(h)).append('\n');
                }
                out.append(bodies.get(i)).append('\n');
            }
            return out.toString();
        }

        private Object answer(SdkHttpRequest request) {
            ByteBuffer buffer = request.body().buffered().asByteBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            String body = new String(bytes, StandardCharsets.UTF_8);
            synchronized (this) {
                requests.add(request);
                bodies.add(body);
            }
            List<Reply> replies = scenario.replies();
            Reply reply = replies.get(Math.min(attempts.getAndIncrement(), replies.size() - 1));
            Map<String, String> values = scenario.replyCanaries().get(reply);
            if (reply == Reply.TRANSPORT_FAILURE) {
                return HttpTransportException.builder()
                                             .phase(HttpPhase.REQUEST_TRANSMISSION)
                                             .category(HttpFailureCategory.of(HttpPhase.REQUEST_TRANSMISSION,
                                                                              HttpFailureCategory.Kind.IO))
                                             .clientName(TRANSPORT_NAME)
                                             .message("scripted transmission failure")
                                             .cause(new IOException("connection reset"))
                                             .build();
            }
            SdkHttpResponse.Builder response = SdkHttpResponse.builder()
                                                              .addHeader("Content-Type", "application/json")
                                                              .addHeader("x-amzn-RequestId", "req-" + attempts.get())
                                                              .addHeader("Set-Cookie",
                                                                         "session=" + values.get("Set-Cookie"))
                                                              .addHeader("x-amz-security-token",
                                                                         values.get("x-amz-security-token"));
            SdkHttpResponse built = switch (reply) {
                case SUCCESS -> {
                    request.headers().firstValue("x-vault-pin").ifPresent(pin -> response.addHeader("x-vault-echo",
                                                                                                     pin));
                    yield response.statusCode(200).body(HttpBody.fromUtf8(body.isEmpty() ? "{}" : body)).build();
                }
                case DENIED -> response.statusCode(403).addHeader("X-Amzn-ErrorType", "VaultDenied")
                                       .body(HttpBody.fromUtf8("{\"message\":\"access denied\",\"secret\":\""
                                                               + values.get("secret") + "\",\"envelope\":{\"secret\":\""
                                                               + values.get("envelope") + "\"}}"))
                                       .build();
                case SEALED -> response.statusCode(409).addHeader("X-Amzn-ErrorType", "VaultSealed")
                                       .body(HttpBody.fromUtf8("{\"message\":\"" + values.get("message") + "\"}"))
                                       .build();
                case SERVER_ERROR -> response.statusCode(500).addHeader("X-Amzn-ErrorType", "InternalFailure")
                                             .body(HttpBody.fromUtf8("{\"message\":\"internal failure\"}")).build();
                case TRANSPORT_FAILURE -> throw new IllegalStateException();
            };
            responses.add(built);
            return built;
        }

        @Override
        public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
            return new SyncHttpExchange() {
                @Override
                public SdkHttpResponse execute() {
                    Object answer = answer(request);
                    if (answer instanceof HttpTransportException failure) {
                        throw failure;
                    }
                    return (SdkHttpResponse) answer;
                }

                @Override
                public void abort() {
                }

                @Override
                public void close() {
                }
            };
        }

        @Override
        public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                         HttpExecutionOptions options) {
            AsyncExchangeCompletion completion =
                new AsyncExchangeCompletion(TRANSPORT_NAME, (reason, failure) -> handler.onError(failure));
            Object answer = answer(request);
            if (answer instanceof HttpTransportException failure) {
                handler.onError(failure);
                completion.fail(failure);
                return completion;
            }
            SdkHttpResponse response = (SdkHttpResponse) answer;
            handler.onHeaders(response.toBuilder().body(HttpBody.empty()).build());
            handler.onStream(publisherOf(response.body().asByteArray()));
            completion.complete();
            return completion;
        }

        @Override
        public HttpClientCapabilities capabilities() {
            HttpTransportMode mode = scenario.projection() == Projection.GENERATED_ASYNC ? HttpTransportMode.ASYNC
                                                                                         : HttpTransportMode.SYNC;
            return HttpClientCapabilities.builder().mode(mode).addProtocol(Protocol.HTTP1_1)
                                         .maxBufferedBytesPerExchange(1 << 20)
                                         .cancellationTimeout(Duration.ofSeconds(1)).build();
        }

        @Override
        public String clientName() {
            return TRANSPORT_NAME;
        }

        @Override
        public void close() {
        }
    }

    private static Publisher<ByteBuffer> publisherOf(byte[] bytes) {
        return subscriber -> subscriber.onSubscribe(new Subscription() {
            private boolean done;

            @Override
            public void request(long n) {
                if (done) {
                    return;
                }
                done = true;
                if (bytes.length > 0) {
                    subscriber.onNext(ByteBuffer.wrap(bytes).asReadOnlyBuffer());
                }
                subscriber.onComplete();
            }

            @Override
            public void cancel() {
                done = true;
            }
        });
    }
}
