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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.statistics.Statistics;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.pipeline.PipelineFixtures.Driver;
import software.amazon.awssdk.pipeline.PipelineFixtures.ScriptedHttp;
import software.amazon.awssdk.pipeline.PipelineFixtures.Struct;
import software.amazon.awssdk.retries.AdaptiveRetryStrategy;
import software.amazon.awssdk.retries.BackoffStrategy;
import software.amazon.awssdk.retries.RetryStrategy;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Property 68: User-agent contribution is exact (Requirements 21.6, 21.7).
 *
 * <p>For any set of components that contribute to a call, the user agent contains every required base dimension and
 * exactly the contributing optional feature identifiers, with no noncontributor identifier. Each try builds one client
 * (an engine over a {@link ClientRuntime}) from generated client dimensions (service, SDK version, HTTP
 * Implementation, and a standard, adaptive or custom retry strategy) and generated feature plugins, each applying to a
 * generated subset of two operations and contributing a generated list of feature identifiers (possibly none, possibly
 * duplicated). It then runs a generated sequence of calls through the sync or async driver, each of one operation,
 * with no override, an override carrying a generated list of {@link ApiName}s ({@value ApiName#SDK_METRICS} and
 * library names, duplicates included), an override contributing nothing to the user agent, or an override setting its
 * own {@code User-Agent} header; some calls are retried once. Identifiers, names, versions and client dimensions are
 * drawn from a small overlapping pool with characters a user-agent token cannot carry, so distinct inputs collapse to
 * one token after sanitization.
 *
 * <p>The header sent on every attempt is parsed by this test's own parser and compared with an oracle written from
 * the requirements and the documented header form ({@link UserAgent}): the eight base dimensions exactly once each and
 * in order, then an {@code m/} list (absent when empty) of the sanitized feature identifiers of the plugins applying
 * to the call's operation followed by the sanitized {@value ApiName#SDK_METRICS} versions of the call, deduplicated
 * in contribution order, then the call's other API names as {@code name/version}, deduplicated in contribution order,
 * and no other token. An override {@code User-Agent} header is sent unchanged instead. The identifiers contributed
 * by other plugins, operations and calls of the same client are absent unless this call contributes them too.
 */
class UserAgentContributionPropertyTest {
    private static final List<AwsOperation<Struct, Struct>> OPERATIONS =
        List.of(PipelineFixtures.GET_THING, PipelineFixtures.PUT_THING);
    private static final List<Schema> INPUTS = List.of(PipelineFixtures.GET_INPUT, PipelineFixtures.PUT_INPUT);
    private static final List<String> OPERATION_NAMES = List.of("GetThing", "PutThing");
    private static final String OVERRIDE_USER_AGENT = "my-agent/1.0 (custom)";
    /** The characters a user-agent token may carry besides letters and digits (RFC 9110 {@code tchar}). */
    private static final String TOKEN_SYMBOLS = "!#$%&'*+-.^_`|~";
    /** The base-dimension token prefixes, in the documented header order. */
    private static final List<String> BASE_PREFIXES = List.of(
        "aws-sdk-java/", "ua/", "os/", "lang/", "api/", "md/op#", "md/http#", "cfg/retry-mode#");
    private static final String METRICS_PREFIX = "m/";

    // Feature: aws-sdk-java-v3, Property 68: User-agent contribution is exact
    // Embedded edge cases are disabled: they are mostly the first value of each generator. The coverage checks below
    // fail the property if contributing and noncontributing plugins, API names of either kind, collapsing duplicates,
    // sanitized input, override headers, retries or either driver stop being generated.
    @Property(tries = 500, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 68: the user agent carries the base dimensions and exactly the contributing feature identifiers")
    void userAgentContributionIsExact(@ForAll("scenarios") Scenario scenario) {
        ScriptedHttp http = new ScriptedHttp();
        AwsCallEngine engine = engine(scenario, http);
        Set<String> scenarioIds = scenarioContributions(scenario);

        for (int i = 0; i < scenario.calls().size(); i++) {
            Call call = scenario.calls().get(i);
            String description = "call " + i + " of " + scenario;
            int before = http.requests.size();
            run(engine, scenario.driver(), http, call);

            List<SdkHttpRequest> attempts = http.requests.subList(before, http.requests.size());
            assertThat(attempts).as("attempts: " + description).hasSize(call.retried() ? 2 : 1);
            for (SdkHttpRequest attempt : attempts) {
                List<String> values = attempt.headers().allValues(UserAgent.HEADER);
                assertThat(values).as("user-agent headers: " + description).hasSize(1);
                String header = values.get(0);
                if (call.override() == OverrideKind.USER_AGENT_HEADER) {
                    assertThat(header).as("override user agent: " + description).isEqualTo(OVERRIDE_USER_AGENT);
                    continue;
                }
                Expected expected = expected(scenario, call);
                ParsedUserAgent parsed = ParsedUserAgent.parse(header);
                String context = description + " sent " + header;
                assertThat(parsed.base()).as("base dimensions: " + context).isEqualTo(expected.base());
                assertThat(parsed.metricsTokens()).as("m/ tokens: " + context)
                                                  .isEqualTo(expected.metrics().isEmpty() ? 0 : 1);
                assertThat(parsed.metrics()).as("m/ feature identifiers: " + context)
                                            .isEqualTo(expected.metrics());
                assertThat(parsed.others()).as("API name tokens: " + context).isEqualTo(expected.others());
                assertThat(parsed.inDocumentedOrder()).as("token order: " + context).isTrue();

                // Explicitly: no identifier another plugin, operation or call contributed leaks into this call.
                Set<String> noncontributors = new LinkedHashSet<>(scenarioIds);
                noncontributors.removeAll(expected.metrics());
                noncontributors.removeAll(expected.others());
                if (!noncontributors.isEmpty()) {
                    assertThat(parsed.metrics()).as("noncontributor feature ids: " + context)
                                                .doesNotContainAnyElementsOf(noncontributors);
                    assertThat(parsed.others()).as("noncontributor API names: " + context)
                                               .doesNotContainAnyElementsOf(noncontributors);
                }
            }
            collectStatistics(scenario, call, scenarioIds);
        }
    }

    private static void collectStatistics(Scenario scenario, Call call, Set<String> scenarioIds) {
        Statistics.label("driver").collect(scenario.driver());
        Statistics.label("driver").coverage(c -> {
            c.check(Driver.SYNC).percentage(p -> p >= 30);
            c.check(Driver.ASYNC).percentage(p -> p >= 30);
        });
        Statistics.label("retry mode").collect(scenario.client().retry());
        Statistics.label("retry mode").coverage(c -> {
            for (RetryKind kind : RetryKind.values()) {
                c.check(kind).percentage(p -> p >= 15);
            }
        });
        Statistics.label("override").collect(call.override());
        Statistics.label("override").coverage(c -> {
            for (OverrideKind kind : OverrideKind.values()) {
                c.check(kind).percentage(p -> p >= 8);
            }
        });
        if (call.override() == OverrideKind.USER_AGENT_HEADER) {
            return;
        }
        Expected expected = expected(scenario, call);
        List<String> planIds = planFeatures(scenario, call.operation()).stream()
                                                                       .map(UserAgentContributionPropertyTest::part)
                                                                       .toList();
        boolean metricsApiNames = call.contributingApiNames().stream()
                                      .anyMatch(a -> ApiName.SDK_METRICS.equals(a.name()));
        boolean otherApiNames = call.contributingApiNames().stream()
                                    .anyMatch(a -> !ApiName.SDK_METRICS.equals(a.name()));
        Statistics.label("plugin feature ids in call").collect(!planIds.isEmpty());
        Statistics.label("plugin feature ids in call").coverage(c -> {
            c.check(true).percentage(p -> p >= 30);
            c.check(false).percentage(p -> p >= 15);
        });
        Statistics.label("sdk-metrics API names").collect(metricsApiNames);
        Statistics.label("sdk-metrics API names").coverage(c -> c.check(true).percentage(p -> p >= 15));
        Statistics.label("other API names").collect(otherApiNames);
        Statistics.label("other API names").coverage(c -> c.check(true).percentage(p -> p >= 15));
        Statistics.label("no optional contribution").collect(expected.metrics().isEmpty()
                                                             && expected.others().isEmpty());
        Statistics.label("no optional contribution").coverage(c -> c.check(true).percentage(p -> p >= 10));
        int raw = planIds.size() + call.contributingApiNames().size();
        int unique = expected.metrics().size() + expected.others().size();
        Statistics.label("duplicates collapsed").collect(raw > unique);
        Statistics.label("duplicates collapsed").coverage(c -> c.check(true).percentage(p -> p >= 15));
        boolean sanitized = planFeatures(scenario, call.operation()).stream().anyMatch(id -> !id.equals(part(id)))
                            || call.contributingApiNames().stream()
                                   .anyMatch(a -> !a.name().equals(part(a.name()))
                                                  || !a.version().equals(part(a.version())));
        Statistics.label("sanitized contribution").collect(sanitized);
        Statistics.label("sanitized contribution").coverage(c -> c.check(true).percentage(p -> p >= 15));
        Set<String> noncontributors = new LinkedHashSet<>(scenarioIds);
        noncontributors.removeAll(expected.metrics());
        noncontributors.removeAll(expected.others());
        Statistics.label("noncontributor ids in client").collect(!noncontributors.isEmpty());
        Statistics.label("noncontributor ids in client").coverage(c -> c.check(true).percentage(p -> p >= 30));
        Statistics.label("retried").collect(call.retried());
        Statistics.label("retried").coverage(c -> c.check(true).percentage(p -> p >= 15));
    }

    // ---- The oracle, written from Requirements 21.6/21.7 and the documented header form ----

    /** The tokens a call's header must carry: the base dimensions in order, the m/ list, the other API names. */
    record Expected(List<String> base, List<String> metrics, List<String> others) {
    }

    private static Expected expected(Scenario scenario, Call call) {
        ClientDimensions client = scenario.client();
        String sdk = sanitize(client.sdkVersion() == null ? UserAgent.sdkVersion() : client.sdkVersion());
        List<String> base = List.of(
            "aws-sdk-java/" + sdk,
            "ua/2.1",
            "os/" + sanitize(System.getProperty("os.name", "unknown")) + "#"
            + sanitize(System.getProperty("os.version", "unknown")),
            "lang/java#" + sanitize(System.getProperty("java.version", "unknown")),
            "api/" + sanitize(client.service()) + "#" + sdk,
            "md/op#" + OPERATION_NAMES.get(call.operation()),
            "md/http#" + sanitize(client.httpImplementation()),
            "cfg/retry-mode#" + client.retry().mode);
        Set<String> metrics = new LinkedHashSet<>();
        planFeatures(scenario, call.operation()).forEach(id -> metrics.add(part(id)));
        Set<String> others = new LinkedHashSet<>();
        for (ApiName apiName : call.contributingApiNames()) {
            if (ApiName.SDK_METRICS.equals(apiName.name())) {
                metrics.add(part(apiName.version()));
            } else {
                others.add(part(apiName.name()) + "/" + part(apiName.version()));
            }
        }
        return new Expected(base, List.copyOf(metrics), List.copyOf(others));
    }

    /** The feature identifiers of the plugins applying to {@code operation}, in plugin then contribution order. */
    private static List<String> planFeatures(Scenario scenario, int operation) {
        List<String> ids = new ArrayList<>();
        for (PluginSpec plugin : scenario.plugins()) {
            if (plugin.appliesTo().contains(operation)) {
                ids.addAll(plugin.features());
            }
        }
        return ids;
    }

    /** Every optional token any component of the client contributes to any call. */
    private static Set<String> scenarioContributions(Scenario scenario) {
        Set<String> ids = new LinkedHashSet<>();
        scenario.plugins().forEach(p -> p.features().forEach(id -> ids.add(part(id))));
        for (Call call : scenario.calls()) {
            for (ApiName apiName : call.apiNames()) {
                ids.add(ApiName.SDK_METRICS.equals(apiName.name())
                        ? part(apiName.version())
                        : part(apiName.name()) + "/" + part(apiName.version()));
            }
        }
        return ids;
    }

    /** Every character outside RFC 9110 {@code tchar} replaced by {@code _}. */
    private static String sanitize(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            boolean tchar = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                            || TOKEN_SYMBOLS.indexOf(c) >= 0;
            out.append(tchar ? c : '_');
        }
        return out.toString();
    }

    /** A feature identifier or API-name part: sanitized, and without the {@code #} separator. */
    private static String part(String value) {
        return sanitize(value).replace('#', '_');
    }

    // ---- The test's own header parser ----

    /**
     * A header split into its space-separated tokens and classified by prefix.
     *
     * @param tokens every token, in order
     * @param base the base-dimension tokens, in header order
     * @param metricsTokens the number of {@code m/} tokens
     * @param metrics the comma-separated entries of the {@code m/} tokens
     * @param others every other token
     */
    record ParsedUserAgent(List<String> tokens, List<String> base, int metricsTokens, List<String> metrics,
                           List<String> others) {
        static ParsedUserAgent parse(String header) {
            List<String> tokens = List.of(header.split(" ", -1));
            List<String> base = new ArrayList<>();
            List<String> metrics = new ArrayList<>();
            List<String> others = new ArrayList<>();
            int metricsTokens = 0;
            for (String token : tokens) {
                if (BASE_PREFIXES.stream().anyMatch(token::startsWith)) {
                    base.add(token);
                } else if (token.startsWith(METRICS_PREFIX)) {
                    metricsTokens++;
                    metrics.addAll(List.of(token.substring(METRICS_PREFIX.length()).split(",", -1)));
                } else {
                    others.add(token);
                }
            }
            return new ParsedUserAgent(tokens, base, metricsTokens, metrics, others);
        }

        /** The base dimensions first, then the {@code m/} list, then the other API names. */
        boolean inDocumentedOrder() {
            int expectedSize = base.size() + metricsTokens + others.size();
            if (tokens.size() != expectedSize || !tokens.subList(0, base.size()).equals(base)) {
                return false;
            }
            int next = base.size();
            if (metricsTokens == 1 && !tokens.get(next++).startsWith(METRICS_PREFIX)) {
                return false;
            }
            return tokens.subList(next, tokens.size()).equals(others);
        }
    }

    // ---- The client and its calls ----

    private static AwsCallEngine engine(Scenario scenario, ScriptedHttp http) {
        ClientDimensions client = scenario.client();
        RetryStrategy retryStrategy = client.retry().strategy();
        UserAgent.Builder userAgent = UserAgent.builder()
                                               .serviceName(client.service())
                                               .httpImplementation(client.httpImplementation())
                                               .retryMode(UserAgent.retryMode(retryStrategy));
        if (client.sdkVersion() != null) {
            userAgent.sdkVersion(client.sdkVersion());
        }
        ClientRuntime.Builder runtime = PipelineFixtures.engineRuntime(retryStrategy, scenario.driver().mode)
                                                        .userAgent(userAgent.build());
        for (int i = 0; i < scenario.plugins().size(); i++) {
            runtime.addPlugin(new FeaturePlugin(i, scenario.plugins().get(i)));
        }
        AwsCallEngine.Builder engine = AwsCallEngine.builder();
        if (scenario.driver() == Driver.ASYNC) {
            runtime.completionExecutor(Runnable::run).retryScheduler(delay -> CompletableFuture.completedFuture(null));
            engine.asyncHttpClient(http);
        } else {
            engine.syncHttpClient(http);
        }
        return engine.runtime(runtime.build()).build();
    }

    private static void run(AwsCallEngine engine, Driver driver, ScriptedHttp http, Call call) {
        if (call.retried()) {
            http.respond(503, null);
        }
        http.respond(200, "v");
        AwsOperation<Struct, Struct> operation = OPERATIONS.get(call.operation());
        PipelinePlan<Struct, Struct> plan = engine.runtime().plan(operation);
        Struct input = PipelineFixtures.request(INPUTS.get(call.operation()), "a");
        AwsRequestOverrideConfiguration override = call.overrideConfiguration();
        if (driver == Driver.ASYNC) {
            engine.executeAsync(plan, input, override, null).join();
        } else {
            engine.executeSync(plan, input, override, null);
        }
    }

    /** A plugin applying to the operations {@code spec} names and contributing its feature identifiers. */
    private static final class FeaturePlugin implements AwsFeaturePlugin {
        private final ShapeId id;
        private final PluginSpec spec;

        FeaturePlugin(int index, PluginSpec spec) {
            this.id = ShapeId.from("com.example#UserAgentFeature" + index);
            this.spec = spec;
        }

        @Override
        public ShapeId id() {
            return id;
        }

        @Override
        public boolean appliesTo(AwsOperation<?, ?> operation) {
            return spec.appliesTo().contains(OPERATIONS.indexOf(operation));
        }

        @Override
        public void configure(PipelinePlan.Builder<?, ?> plan) {
            spec.features().forEach(plan::addUserAgentFeature);
        }
    }

    enum RetryKind {
        STANDARD("standard"),
        ADAPTIVE("adaptive"),
        CUSTOM("custom");

        final String mode;

        RetryKind(String mode) {
            this.mode = mode;
        }

        /** A strategy of this kind allowing one retry without backoff. */
        RetryStrategy strategy() {
            return switch (this) {
                case STANDARD -> PipelineFixtures.retries(2);
                case ADAPTIVE -> AdaptiveRetryStrategy.builder()
                                                      .maxAttempts(2)
                                                      .backoffStrategy(BackoffStrategy.none())
                                                      .throttlingBackoffStrategy(BackoffStrategy.none())
                                                      .build();
                case CUSTOM -> new PipelineFixtures.RecordingRetryStrategy(PipelineFixtures.retries(2));
            };
        }
    }

    /** How a call's override configuration contributes to its user agent. */
    enum OverrideKind {
        /** No override configuration at all. */
        NONE,
        /** An override adding the call's API names. */
        API_NAMES,
        /** An override contributing nothing to the user agent: an unrelated header only. */
        UNRELATED,
        /** An override adding the call's API names and setting its own user-agent header, which wins. */
        USER_AGENT_HEADER
    }

    /** @param sdkVersion the explicit SDK version dimension, or {@code null} for the runtime's own */
    record ClientDimensions(String service, String sdkVersion, String httpImplementation, RetryKind retry) {
    }

    /** @param appliesTo the indexes in {@link #OPERATIONS} of the operations the plugin applies to */
    record PluginSpec(Set<Integer> appliesTo, List<String> features) {
    }

    /**
     * @param operation the index in {@link #OPERATIONS} of the call's operation
     * @param apiNames the API names the override adds when it adds any ({@link OverrideKind#API_NAMES} and
     *                 {@link OverrideKind#USER_AGENT_HEADER})
     */
    record Call(int operation, OverrideKind override, List<ApiName> apiNames, boolean retried) {
        /** The API names that contribute to the SDK user agent of the call. */
        List<ApiName> contributingApiNames() {
            return override == OverrideKind.API_NAMES ? apiNames : List.of();
        }

        AwsRequestOverrideConfiguration overrideConfiguration() {
            if (override == OverrideKind.NONE) {
                return null;
            }
            AwsRequestOverrideConfiguration.Builder builder = AwsRequestOverrideConfiguration.builder();
            switch (override) {
                case UNRELATED -> builder.putHeader("x-unrelated", "value");
                case USER_AGENT_HEADER -> builder.putHeader("user-agent", OVERRIDE_USER_AGENT);
                default -> {
                }
            }
            if (override != OverrideKind.UNRELATED) {
                apiNames.forEach(builder::addApiName);
            }
            return builder.build();
        }

        @Override
        public String toString() {
            return OPERATION_NAMES.get(operation) + "(" + override + ", " + apiNames + (retried ? ", retried)" : ")");
        }
    }

    record Scenario(Driver driver, ClientDimensions client, List<PluginSpec> plugins, List<Call> calls) {
    }

    // ---- Generators ----

    @Provide
    Arbitrary<Scenario> scenarios() {
        Arbitrary<ClientDimensions> client = Combinators.combine(
            dimension(),
            Arbitraries.frequencyOf(Tuple.of(1, Arbitraries.just((String) null)),
                                    Tuple.of(3, Arbitraries.of("3.0.0", "3.1.0-preview", "3.0 beta", "3/0,1"))),
            dimension(),
            Arbitraries.of(RetryKind.class))
            .as(ClientDimensions::new);
        Arbitrary<PluginSpec> plugin = Combinators.combine(
            Arbitraries.subsetOf(0, 1),
            Arbitraries.frequencyOf(Tuple.of(1, Arbitraries.just(List.<String>of())),
                                    Tuple.of(4, featureId().list().ofMinSize(1).ofMaxSize(3))))
            .as(PluginSpec::new);
        Arbitrary<Call> call = Combinators.combine(
            Arbitraries.integers().between(0, OPERATIONS.size() - 1),
            Arbitraries.frequency(Tuple.of(2, OverrideKind.NONE), Tuple.of(5, OverrideKind.API_NAMES),
                                  Tuple.of(2, OverrideKind.UNRELATED), Tuple.of(1, OverrideKind.USER_AGENT_HEADER)),
            apiName().list().ofMinSize(0).ofMaxSize(5),
            Arbitraries.frequency(Tuple.of(3, false), Tuple.of(1, true)))
            .as(Call::new);
        return Combinators.combine(
            PipelineArbitraries.drivers(),
            client,
            plugin.list().ofMinSize(0).ofMaxSize(4),
            call.list().ofMinSize(1).ofMaxSize(4))
            .as(Scenario::new);
    }

    /** A service or HTTP Implementation dimension, sometimes with characters a token cannot carry. */
    private static Arbitrary<String> dimension() {
        return Arbitraries.frequencyOf(
            Tuple.of(2, Arbitraries.of("Things", "DynamoDB", "Apache5", "UrlConnection")),
            Tuple.of(1, Arbitraries.of("S3 Control", "Things(v2)", "Netty/Nio", "Crt,Http", "Sérvice", "a#b")));
    }

    /**
     * A feature identifier or API-name version from a small pool whose members overlap after sanitization (for
     * example {@code a b}, {@code a,b}, {@code a#b} and {@code a_b}), or a random one with separators.
     */
    private static Arbitrary<String> featureId() {
        return Arbitraries.frequencyOf(
            Tuple.of(4, Arbitraries.of("P", "Q", "d", "a_b", "a b", "a,b", "a#b", "x/y", "é", "1.2")),
            Tuple.of(1, Arbitraries.strings().withChars("abAB12 ,/#()é").ofMinLength(1).ofMaxLength(5)));
    }

    /** An {@value ApiName#SDK_METRICS} API name or a library name; never a name of a base or m/ token. */
    private static Arbitrary<ApiName> apiName() {
        Arbitrary<String> name = Arbitraries.frequencyOf(
            Tuple.of(4, Arbitraries.just(ApiName.SDK_METRICS)),
            Tuple.of(3, Arbitraries.of("hll", "lib a", "lib_a", "lib,a", "Sdk-Metrics", "sdk metrics", "ext(1)")),
            Tuple.of(1, Arbitraries.strings().withChars("ab ,/#").ofMaxLength(3).map(s -> "lib" + s)));
        return Combinators.combine(name, featureId())
                          .as((n, v) -> ApiName.builder().name(n).version(v).build());
    }
}
