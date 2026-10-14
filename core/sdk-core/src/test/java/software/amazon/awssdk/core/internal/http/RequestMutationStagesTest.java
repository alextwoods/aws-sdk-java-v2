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

package software.amazon.awssdk.core.internal.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.CLIENT_ENDPOINT;
import static software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.OPERATION_PATH;
import static software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.assertSameRequest;
import static software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.attributesWithProtocol;
import static software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.baseConfig;
import static software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.context;
import static software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.dependencies;
import static software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.legacyMutation;
import static software.amazon.awssdk.core.internal.http.ApiCallStagesTestSupport.marshalledRequest;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.CredentialType;
import software.amazon.awssdk.core.SdkProtocolMetadata;
import software.amazon.awssdk.core.SdkRequestOverrideConfiguration;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.TestRequest;
import software.amazon.awssdk.core.internal.http.pipeline.stages.utils.RetryableStageHelper;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.useragent.AdditionalMetadata;
import software.amazon.awssdk.core.useragent.BusinessMetricCollection;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;

/**
 * Compares each flattened mutation of {@link RequestMutationStages#prepare} with the legacy stage it replaces, by running
 * the legacy stage chain and the single pass on the same input, and tests {@link RequestMutationStages#marshall}.
 */
class RequestMutationStagesTest {
    private static final URI RESOLVED = URI.create("https://resolved.us-west-2.amazonaws.com:8443");
    private static final URI RESOLVED_WITH_PATH = URI.create("https://resolved.us-west-2.amazonaws.com/prefix");

    static Stream<Scenario> scenarios() {
        return Stream.of(
            new Scenario("no mutation input"),
            new Scenario("client and request custom headers")
                .config(c -> c.option(SdkClientOption.ADDITIONAL_HTTP_HEADERS,
                                      headers("x-client", "a", "Content-Type", "text/plain")))
                .override(o -> o.putHeader("x-request", Arrays.asList("b", "c")).putHeader("x-client", "d"))
                .expect(r -> assertThat(r.matchingHeaders("x-request")).containsExactly("b", "c")),
            new Scenario("request custom query parameters")
                .request(r -> r.putRawQueryParameter("q", "0").putRawQueryParameter("z", "9"))
                .override(o -> o.putRawQueryParameter("q", "1").putRawQueryParameter("n", Arrays.asList("x", "y")))
                .expect(r -> assertThat(r.rawQueryParameters().get("q")).containsExactly("0", "1")),
            new Scenario("query protocol parameters moved to the body")
                .body(null)
                .request(r -> r.putRawQueryParameter("Action", "GetThing").putRawQueryParameter("Version", "2020"))
                .attributes(a -> a.putAttribute(SdkInternalExecutionAttribute.PROTOCOL_METADATA, protocol("query")))
                .expect(r -> assertThat(ApiCallStagesTestSupport.body(r)).isEqualTo("Action=GetThing&Version=2020")),
            new Scenario("query protocol with a body keeps the query")
                .request(r -> r.putRawQueryParameter("Action", "GetThing"))
                .attributes(a -> a.putAttribute(SdkInternalExecutionAttribute.PROTOCOL_METADATA, protocol("ec2"))),
            new Scenario("resolved endpoint with a path and headers")
                .attributes(a -> resolveTo(a, Endpoint.builder().url(RESOLVED_WITH_PATH)
                                                      .putHeader("x-endpoint", "e1").putHeader("x-endpoint", "e2")
                                                      .build()))
                .expect(r -> assertThat(r.getUri()).isEqualTo(URI.create(RESOLVED_WITH_PATH + OPERATION_PATH))),
            new Scenario("resolved endpoint with the client endpoint path")
                .attributes(a -> resolveTo(a, Endpoint.builder().url(RESOLVED).build()))
                .expect(r -> assertThat(r.getUri()).isEqualTo(URI.create(RESOLVED + OPERATION_PATH))),
            new Scenario("client endpoint with a path")
                .clientEndpoint(URI.create("https://svc.us-east-1.amazonaws.com/v1"))
                .attributes(a -> resolveTo(a, Endpoint.builder().url(RESOLVED_WITH_PATH).build())),
            new Scenario("interceptor moved the request to another host")
                .request(r -> r.host("intercepted.example.com"))
                .attributes(a -> resolveTo(a, Endpoint.builder().url(RESOLVED_WITH_PATH).build())),
            new Scenario("user agent with request and client values")
                .config(c -> c.option(SdkAdvancedClientOption.USER_AGENT_PREFIX, " my-prefix ")
                              .option(SdkAdvancedClientOption.USER_AGENT_SUFFIX, "my-suffix"))
                .override(o -> o.addApiName(ApiName.builder().name("myLib").version("1.0").build())
                                .addApiName(ApiName.builder().name("sdk-metrics").version("P").build()))
                .attributes(a -> {
                    a.putAttribute(SdkInternalExecutionAttribute.USER_AGENT_METADATA,
                                   new ArrayList<>(Collections.singletonList(
                                       AdditionalMetadata.builder().name("rb").value("b").build())));
                    BusinessMetricCollection metrics = new BusinessMetricCollection();
                    metrics.addMetric("E");
                    a.putAttribute(SdkInternalExecutionAttribute.BUSINESS_METRICS, metrics);
                    a.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME,
                                   new SelectedAuthScheme<AwsCredentialsIdentity>(
                                       CompletableFuture.completedFuture(
                                           AwsSessionCredentialsIdentity.builder().accessKeyId("a").secretAccessKey("s")
                                                                        .sessionToken("t").providerName("w").build()),
                                       new NoOpSigner<>(),
                                       AuthSchemeOption.builder().schemeId("unset").build()));
                })
                .expect(r -> assertThat(r.firstMatchingHeader("User-Agent").get())
                    .startsWith("my-prefix " + ApiCallStagesTestSupport.CLIENT_USER_AGENT + " md/rb#b m/E,P,w myLib/1.0")
                    .endsWith(" my-suffix")),
            new Scenario("user agent set in the client's additional headers")
                .config(c -> c.option(SdkClientOption.ADDITIONAL_HTTP_HEADERS, headers("User-Agent", "custom"))),
            new Scenario("user agent set in the request's headers")
                .override(o -> o.putHeader("User-Agent", "per-request")),
            new Scenario("no client user agent")
                .config(c -> c.option(SdkClientOption.CLIENT_USER_AGENT, null)),
            new Scenario("required MD5 checksum")
                .attributes(a -> a.putAttribute(SdkInternalExecutionAttribute.HTTP_CHECKSUM_REQUIRED,
                                                software.amazon.awssdk.core.interceptor.trait.HttpChecksumRequired.create()))
                .expect(r -> assertThat(r.firstMatchingHeader("Content-MD5")).isPresent()),
            new Scenario("retry policy max attempts")
                .config(c -> c.option(SdkClientOption.RETRY_POLICY, RetryPolicy.builder().numRetries(5).build()))
                .expect(r -> assertThat(r.firstMatchingHeader("amz-sdk-request")).hasValue("attempt=1; max=6")),
            new Scenario("auth scheme resolution")
                .authScheme()
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void prepare_producesTheLegacyStagesRequest(Scenario scenario) throws Exception {
        for (ClientType clientType : Arrays.asList(ClientType.SYNC, ClientType.ASYNC)) {
            SdkClientConfiguration config = scenario.config();
            HttpClientDependencies dependencies = dependencies(config);

            RequestExecutionContext legacyContext = scenario.context(config);
            SdkHttpFullRequest expected = legacyMutation(scenario.input(), legacyContext, dependencies, clientType);

            RequestExecutionContext newContext = scenario.context(config);
            RequestMutationStages stages = RequestMutationStages.create(dependencies, clientType);
            SdkHttpFullRequest actual = stages.prepare(scenario.input(), newContext);

            assertSameRequest(actual, expected);
            scenario.expect.accept(actual);
            assertSameAttributes(newContext.executionAttributes(), legacyContext.executionAttributes());
        }
    }

    @Test
    void prepare_stampsTheFirstAttemptRetryInfoAsTheLegacyHelper() throws Exception {
        Scenario scenario = new Scenario("retry info");
        SdkClientConfiguration config = scenario.config();
        RequestExecutionContext context = scenario.context(config);
        SdkHttpFullRequest prepared = RequestMutationStages.create(dependencies(config), ClientType.SYNC)
                                                           .prepare(scenario.input(), context);

        assertThat(prepared.firstMatchingHeader(RetryableStageHelper.SDK_RETRY_INFO_HEADER))
            .hasValue("attempt=1; max=3");
    }

    @Test
    void prepare_appliesTheResolvedEndpointThroughTheProtocol() throws Exception {
        Scenario scenario = new Scenario("endpoint").attributes(a -> resolveTo(a, Endpoint.builder().url(RESOLVED_WITH_PATH)
                                                                                         .build()));
        SdkClientConfiguration config = scenario.config();
        RequestExecutionContext context = scenario.context(config);

        SdkHttpFullRequest prepared = RequestMutationStages.create(dependencies(config), ClientType.SYNC)
                                                           .prepare(scenario.input(), context);

        assertThat(scenario.protocol.updatedEndpoints).extracting(Endpoint::url).containsExactly(RESOLVED_WITH_PATH);
        assertThat(prepared.getUri()).isEqualTo(URI.create(RESOLVED_WITH_PATH + OPERATION_PATH));
    }

    @Test
    void prepare_interceptorHostChange_keepsTheHostAndDoesNotCallTheProtocol() throws Exception {
        Scenario scenario = new Scenario("moved").request(r -> r.host("intercepted.example.com"))
                                                 .attributes(a -> resolveTo(a, Endpoint.builder().url(RESOLVED_WITH_PATH)
                                                                                       .build()));
        SdkClientConfiguration config = scenario.config();
        SdkHttpFullRequest prepared = RequestMutationStages.create(dependencies(config), ClientType.SYNC)
                                                           .prepare(scenario.input(), scenario.context(config));

        assertThat(scenario.protocol.updatedEndpoints).isEmpty();
        assertThat(prepared.host()).isEqualTo("intercepted.example.com");
        assertThat(prepared.encodedPath()).isEqualTo("/prefix" + OPERATION_PATH);
    }

    @Test
    void prepare_invocationIdIsNewForEachCall() throws Exception {
        Scenario scenario = new Scenario("ids");
        SdkClientConfiguration config = scenario.config();
        RequestMutationStages stages = RequestMutationStages.create(dependencies(config), ClientType.SYNC);

        String first = stages.prepare(scenario.input(), scenario.context(config))
                             .firstMatchingHeader("amz-sdk-invocation-id").get();
        String second = stages.prepare(scenario.input(), scenario.context(config))
                              .firstMatchingHeader("amz-sdk-invocation-id").get();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void prepare_interruptedThread_throwsInterruptedException() {
        Scenario scenario = new Scenario("interrupted");
        SdkClientConfiguration config = scenario.config();
        RequestMutationStages stages = RequestMutationStages.create(dependencies(config), ClientType.SYNC);
        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> stages.prepare(scenario.input(), scenario.context(config)))
                .isInstanceOf(InterruptedException.class);
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void marshall_addressesTheRequestToTheClientEndpointBeforeAfterMarshalling() {
        HostRecorder recorder = new HostRecorder();
        MarshallFixture fixture = new MarshallFixture(baseConfig().build(), ClientType.SYNC, recorder);

        SdkHttpFullRequest marshalled = fixture.stages.marshall(fixture.executionContext, null, null);

        assertThat(recorder.afterMarshallingUri).isEqualTo(URI.create(CLIENT_ENDPOINT + OPERATION_PATH));
        assertThat(recorder.modifyHttpRequestUri).isEqualTo(recorder.afterMarshallingUri);
        assertThat(marshalled.getUri()).isEqualTo(recorder.afterMarshallingUri);
        assertThat(fixture.attributes.getAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_URI_BEFORE_MODIFY))
            .isEqualTo(recorder.afterMarshallingUri);
        assertThat(fixture.executionContext.interceptorContext().httpRequest()).isSameAs(marshalled);
    }

    @Test
    void marshall_hostPrefix_isAppliedBeforeAfterMarshalling() {
        HostRecorder recorder = new HostRecorder();
        MarshallFixture fixture = new MarshallFixture(baseConfig().build(), ClientType.SYNC, recorder);

        fixture.stages.marshall(fixture.executionContext, "data-", null);

        assertThat(recorder.afterMarshallingUri.getHost()).isEqualTo("data-" + CLIENT_ENDPOINT.getHost());
    }

    @Test
    void marshall_hostPrefixInjectionDisabled_keepsTheClientHost() {
        HostRecorder recorder = new HostRecorder();
        SdkClientConfiguration config = baseConfig().option(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION, true)
                                                    .build();
        MarshallFixture fixture = new MarshallFixture(config, ClientType.SYNC, recorder);

        fixture.stages.marshall(fixture.executionContext, "data-", null);

        assertThat(recorder.afterMarshallingUri.getHost()).isEqualTo(CLIENT_ENDPOINT.getHost());
    }

    @Test
    void marshall_discoveredEndpoint_usesItsHostAndPortAndSkipsEndpointResolution() {
        HostRecorder recorder = new HostRecorder();
        MarshallFixture fixture = new MarshallFixture(baseConfig().build(), ClientType.SYNC, recorder);

        fixture.stages.marshall(fixture.executionContext, "data-", URI.create("https://discovered.example.com:9443"));

        assertThat(recorder.afterMarshallingUri).isEqualTo(URI.create("https://discovered.example.com:9443" + OPERATION_PATH));
        assertThat(fixture.attributes.getAttribute(SdkInternalExecutionAttribute.SKIP_ENDPOINT_RESOLUTION)).isTrue();
        assertThat(fixture.attributes.getAttribute(SdkInternalExecutionAttribute.IS_DISCOVERED_ENDPOINT)).isTrue();
    }

    @Test
    void marshall_noClientEndpoint_keepsTheUnresolvedUri() {
        HostRecorder recorder = new HostRecorder();
        SdkClientConfiguration config = baseConfig().option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER, null).build();
        MarshallFixture fixture = new MarshallFixture(config, ClientType.SYNC, recorder);

        fixture.stages.marshall(fixture.executionContext, null, null);

        assertThat(recorder.afterMarshallingUri.getHost()).isEqualTo(ProtocolMarshalling.UNRESOLVED_URI.getHost());
    }

    @Test
    void marshall_noClientEndpoint_stillAppliesTheDiscoveredEndpoint() {
        HostRecorder recorder = new HostRecorder();
        SdkClientConfiguration config = baseConfig().option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER, null).build();
        MarshallFixture fixture = new MarshallFixture(config, ClientType.SYNC, recorder);

        fixture.stages.marshall(fixture.executionContext, null, URI.create("https://discovered.example.com:9443"));

        assertThat(recorder.afterMarshallingUri.getHost()).isEqualTo("discovered.example.com");
        assertThat(recorder.afterMarshallingUri.getPort()).isEqualTo(9443);
        assertThat(fixture.attributes.getAttribute(SdkInternalExecutionAttribute.SKIP_ENDPOINT_RESOLUTION)).isTrue();
    }

    @Test
    void marshall_modifyHttpRequestAndContent_areApplied() {
        ExecutionInterceptor modifier = new ExecutionInterceptor() {
            @Override
            public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes attributes) {
                return context.httpRequest().toBuilder().putHeader("x-modified", "1").build();
            }

            @Override
            public java.util.Optional<RequestBody> modifyHttpContent(Context.ModifyHttpRequest context,
                                                                     ExecutionAttributes attributes) {
                return java.util.Optional.of(RequestBody.fromString("replaced"));
            }
        };
        MarshallFixture fixture = new MarshallFixture(baseConfig().build(), ClientType.SYNC, modifier);

        SdkHttpFullRequest marshalled = fixture.stages.marshall(fixture.executionContext, null, null);

        assertThat(marshalled.firstMatchingHeader("x-modified")).hasValue("1");
        assertThat(ApiCallStagesTestSupport.body(marshalled)).isEqualTo("replaced");
        assertThat(fixture.executionContext.interceptorContext().httpRequest()).isSameAs(marshalled);
    }

    @Test
    void marshall_asyncWithAsyncBody_doesNotReattachTheRequestBody() {
        MarshallFixture fixture = new MarshallFixture(baseConfig().build(), ClientType.ASYNC);
        fixture.executionContext.interceptorContext(
            fixture.executionContext.interceptorContext()
                                    .copy(b -> b.asyncRequestBody(software.amazon.awssdk.core.async.AsyncRequestBody
                                                                      .fromString("async"))));
        SdkHttpFullRequest marshalled = fixture.stages.marshall(fixture.executionContext, null, null);

        assertThat(marshalled.contentStreamProvider().get())
            .isSameAs(fixture.protocol.lastSerialized.contentStreamProvider().get());
    }

    @Test
    void marshall_bearerTokenSignerWithPlainHttp_isRejected() {
        SdkClientConfiguration config =
            baseConfig().option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER,
                                ClientEndpointProvider.create(URI.create("http://svc.example.com"), true))
                        .build();
        MarshallFixture fixture = new MarshallFixture(config, ClientType.SYNC);
        Signer tokenSigner = new Signer() {
            @Override
            public SdkHttpFullRequest sign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
                return request;
            }

            @Override
            public CredentialType credentialType() {
                return CredentialType.TOKEN;
            }
        };
        ExecutionContext withSigner = fixture.executionContext.toBuilder().signer(tokenSigner).build();

        assertThatThrownBy(() -> fixture.stages.marshall(withSigner, null, null))
            .isInstanceOf(SdkClientException.class)
            .hasMessageStartingWith("Cannot use bearer token signer with a plaintext HTTP endpoint");
    }

    private static void assertSameAttributes(ExecutionAttributes actual, ExecutionAttributes expected) {
        assertThat(actual.getAttribute(SdkInternalExecutionAttribute.RESOLVED_ENDPOINT))
            .isEqualTo(expected.getAttribute(SdkInternalExecutionAttribute.RESOLVED_ENDPOINT));
        SelectedAuthScheme<?> actualScheme = actual.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        SelectedAuthScheme<?> expectedScheme = expected.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        if (expectedScheme == null) {
            assertThat(actualScheme).isNull();
        } else {
            // AuthSchemeOption has no equals; its toString lists the scheme ID and every property
            assertThat(actualScheme.authSchemeOption()).hasToString(expectedScheme.authSchemeOption().toString());
            assertThat(actualScheme.signer()).hasSameClassAs(expectedScheme.signer());
            assertThat(actualScheme.identity().join()).isEqualTo(expectedScheme.identity().join());
        }
        BusinessMetricCollection actualMetrics = actual.getAttribute(SdkInternalExecutionAttribute.BUSINESS_METRICS);
        BusinessMetricCollection expectedMetrics = expected.getAttribute(SdkInternalExecutionAttribute.BUSINESS_METRICS);
        if (expectedMetrics != null) {
            assertThat(actualMetrics.recordedMetrics()).isEqualTo(expectedMetrics.recordedMetrics());
        }
        assertThat(actual.getAttribute(SdkExecutionAttribute.RESOLVED_CHECKSUM_SPECS))
            .isEqualTo(expected.getAttribute(SdkExecutionAttribute.RESOLVED_CHECKSUM_SPECS));
    }

    private static Map<String, List<String>> headers(String... namesAndValues) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            headers.computeIfAbsent(namesAndValues[i], k -> new ArrayList<>()).add(namesAndValues[i + 1]);
        }
        return headers;
    }

    private static SdkProtocolMetadata protocol(String name) {
        return () -> name;
    }

    private static void resolveTo(ExecutionAttributes attributes, Endpoint endpoint) {
        attributes.putAttribute(SdkInternalExecutionAttribute.ENDPOINT_RESOLVER, (request, attrs) -> endpoint);
    }

    /**
     * One input to compare: client configuration, request override configuration, attributes and the marshalled
     * request. Each call of {@link #context} creates fresh attributes, so the legacy and new runs share no state.
     */
    static final class Scenario {
        private final String name;
        private final ApiCallStagesTestSupport.HttpStubProtocol protocol = new ApiCallStagesTestSupport.HttpStubProtocol();
        private Consumer<SdkClientConfiguration.Builder> config = c -> { };
        private Consumer<SdkRequestOverrideConfiguration.Builder> override;
        private Consumer<ExecutionAttributes> attributes = a -> { };
        private Consumer<SdkHttpFullRequest.Builder> request = r -> { };
        private Consumer<SdkHttpFullRequest> expect = r -> { };
        private String body = "{\"a\":1}";
        private URI clientEndpoint = CLIENT_ENDPOINT;
        private Map<String, AuthScheme<?>> authSchemes;
        private IdentityProviders identityProviders;

        Scenario(String name) {
            this.name = name;
        }

        Scenario config(Consumer<SdkClientConfiguration.Builder> config) {
            this.config = config;
            return this;
        }

        Scenario override(Consumer<SdkRequestOverrideConfiguration.Builder> override) {
            this.override = override;
            return this;
        }

        Scenario attributes(Consumer<ExecutionAttributes> attributes) {
            this.attributes = attributes;
            return this;
        }

        Scenario request(Consumer<SdkHttpFullRequest.Builder> request) {
            this.request = request;
            return this;
        }

        Scenario expect(Consumer<SdkHttpFullRequest> expect) {
            this.expect = expect;
            return this;
        }

        Scenario body(String body) {
            this.body = body;
            return this;
        }

        Scenario clientEndpoint(URI clientEndpoint) {
            this.clientEndpoint = clientEndpoint;
            return this;
        }

        Scenario authScheme() {
            IdentityProvider<AwsCredentialsIdentity> provider = new StaticCredentials();
            this.identityProviders = IdentityProviders.builder().putIdentityProvider(provider).build();
            Map<String, AuthScheme<?>> schemes = new HashMap<>();
            schemes.put("aws.auth#sigv4", new TestAuthScheme("aws.auth#sigv4"));
            this.authSchemes = schemes;
            return this;
        }

        SdkClientConfiguration config() {
            SdkClientConfiguration.Builder builder =
                baseConfig().option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER, ClientEndpointProvider.create(clientEndpoint,
                                                                                                            false));
            if (authSchemes != null) {
                builder.option(SdkClientOption.AUTH_SCHEMES, authSchemes)
                       .option(SdkClientOption.IDENTITY_PROVIDERS, identityProviders);
            }
            config.accept(builder);
            return builder.build();
        }

        SdkHttpFullRequest input() {
            SdkHttpFullRequest.Builder builder = marshalledRequest(clientEndpoint, body).toBuilder();
            request.accept(builder);
            return builder.build();
        }

        RequestExecutionContext context(SdkClientConfiguration config) {
            ExecutionAttributes attrs = attributesWithProtocol(protocol);
            attrs.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER,
                               config.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER));
            attrs.putAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_URI_BEFORE_MODIFY,
                               marshalledRequest(clientEndpoint, body).getUri());
            if (authSchemes != null) {
                attrs.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES, authSchemes);
                attrs.putAttribute(SdkInternalExecutionAttribute.IDENTITY_PROVIDERS, identityProviders);
                attrs.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_OPTIONS_RESOLVER,
                                   (r, a) -> Collections.singletonList(AuthSchemeOption.builder()
                                                                                       .schemeId("aws.auth#sigv4")
                                                                                       .build()));
            }
            attributes.accept(attrs);
            NoopTestRequest.Builder sdkRequest = NoopTestRequest.builder();
            if (override != null) {
                SdkRequestOverrideConfiguration.Builder overrideBuilder = SdkRequestOverrideConfiguration.builder();
                override.accept(overrideBuilder);
                sdkRequest.overrideConfiguration(overrideBuilder.build());
            }
            return ApiCallStagesTestSupport.context(sdkRequest.build(), attrs, input());
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * The new path's marshalling fixture: a schema request, the HTTP stub protocol and the given interceptors.
     */
    private static final class MarshallFixture {
        final RecordingProtocol protocol = new RecordingProtocol();
        final ExecutionAttributes attributes;
        final ExecutionContext executionContext;
        final RequestMutationStages stages;

        MarshallFixture(SdkClientConfiguration config, ClientType clientType, ExecutionInterceptor... interceptors) {
            attributes = attributesWithProtocol(protocol);
            attributes.putAttribute(SdkExecutionAttribute.API_OPERATION, ProtocolStepsTestSupport.OPERATION);
            executionContext = ExecutionContext.builder()
                                               .executionAttributes(attributes)
                                               .interceptorChain(new ExecutionInterceptorChain(
                                                   new ArrayList<>(Arrays.asList(interceptors))))
                                               .interceptorContext(InterceptorContext.builder()
                                                                                     .request(new TestRequest("a"))
                                                                                     .build())
                                               .build();
            stages = RequestMutationStages.create(dependencies(config), clientType);
        }
    }

    private static final class RecordingProtocol extends ApiCallStagesTestSupport.HttpStubProtocol {
        SdkHttpFullRequest lastSerialized;

        RecordingProtocol() {
            serialize = endpoint -> {
                byte[] bytes = "{}".getBytes(StandardCharsets.UTF_8);
                lastSerialized = SdkHttpFullRequest.builder()
                                                   .method(SdkHttpMethod.POST)
                                                   .uri(endpoint)
                                                   .encodedPath(OPERATION_PATH)
                                                   .putHeader("Content-Length", "2")
                                                   .contentStreamProvider(() -> new ByteArrayInputStream(bytes))
                                                   .build();
                return lastSerialized;
            };
        }
    }

    private static final class HostRecorder implements ExecutionInterceptor {
        URI afterMarshallingUri;
        URI modifyHttpRequestUri;

        @Override
        public void afterMarshalling(Context.AfterMarshalling context, ExecutionAttributes executionAttributes) {
            afterMarshallingUri = context.httpRequest().getUri();
        }

        @Override
        public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
            modifyHttpRequestUri = context.httpRequest().getUri();
            return context.httpRequest();
        }
    }

    static final class StaticCredentials implements IdentityProvider<AwsCredentialsIdentity> {
        private static final AwsCredentialsIdentity IDENTITY = AwsCredentialsIdentity.create("akid", "secret");

        @Override
        public Class<AwsCredentialsIdentity> identityType() {
            return AwsCredentialsIdentity.class;
        }

        @Override
        public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
            return CompletableFuture.completedFuture(IDENTITY);
        }
    }

    static final class TestAuthScheme implements AuthScheme<AwsCredentialsIdentity> {
        private final String schemeId;
        private final HttpSigner<AwsCredentialsIdentity> signer = new NoOpSigner<>();

        TestAuthScheme(String schemeId) {
            this.schemeId = schemeId;
        }

        @Override
        public String schemeId() {
            return schemeId;
        }

        @Override
        public IdentityProvider<AwsCredentialsIdentity> identityProvider(IdentityProviders providers) {
            return providers.identityProvider(AwsCredentialsIdentity.class);
        }

        @Override
        public HttpSigner<AwsCredentialsIdentity> signer() {
            return signer;
        }
    }

    static final class NoOpSigner<T extends software.amazon.awssdk.identity.spi.Identity> implements HttpSigner<T> {
        @Override
        public SignedRequest sign(SignRequest<? extends T> request) {
            return SignedRequest.builder().request(request.request()).payload(request.payload().orElse(null)).build();
        }

        @Override
        public CompletableFuture<software.amazon.awssdk.http.auth.spi.signer.AsyncSignedRequest> signAsync(
            software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest<? extends T> request) {
            throw new UnsupportedOperationException();
        }
    }
}
