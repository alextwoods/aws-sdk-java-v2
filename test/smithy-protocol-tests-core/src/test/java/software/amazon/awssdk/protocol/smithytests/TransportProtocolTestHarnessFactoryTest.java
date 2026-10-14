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

package software.amazon.awssdk.protocol.smithytests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.client.builder.SdkClientBuilder;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.util.IdempotentUtils;
import software.amazon.awssdk.protocol.smithytests.EchoClients.EchoAsyncClient;
import software.amazon.awssdk.protocol.smithytests.EchoClients.EchoAsyncClientBuilder;
import software.amazon.awssdk.protocol.smithytests.EchoClients.EchoClient;
import software.amazon.awssdk.protocol.smithytests.EchoClients.EchoClientBuilder;
import software.amazon.awssdk.protocol.smithytests.EchoClients.EchoResponse;

/**
 * Runs the harness against {@link EchoClients}, whose calls go through the SDK's legacy request pipeline, so requests
 * reach the transports through the harness's HTTP client view.
 */
class TransportProtocolTestHarnessFactoryTest {
    private static final String PROTOCOL = "smithy.protocols#rpcv2Cbor";
    private static final TransportProtocolTestHarnessFactory FACTORY = new TransportProtocolTestHarnessFactory();
    private static final Map<String, String> CBOR_HEADERS = ProtocolTestValues.mapOf("Content-Type", "application/cbor",
                                                                                     "X-Answer", "42");

    /** Settings like the generator's: a custom header stands in for region and credentials, and retries are on. */
    private static final Consumer<SdkClientBuilder<?, ?>> CONFIGURATION =
        b -> b.overrideConfiguration(o -> o.putHeader("X-Configured", "yes").retryStrategy(RetryMode.STANDARD));

    private static ProtocolTestHarness<EchoClient> syncHarness() {
        return FACTORY.<EchoClient, EchoClientBuilder>create(PROTOCOL, EchoClient::builder, CONFIGURATION);
    }

    private static ProtocolTestHarness<EchoAsyncClient> asyncHarness() {
        return FACTORY.<EchoAsyncClient, EchoAsyncClientBuilder>create(PROTOCOL, EchoAsyncClient::builder, CONFIGURATION);
    }

    @Test
    void captureRequest_sync_returnsTheRequestTheClientSent() {
        CapturedRequest request = syncHarness().captureRequest(c -> c.echo("hello"), "https://foo.example.com:8443");

        assertCaptured(request);
    }

    @Test
    void captureRequest_async_returnsTheRequestTheClientSent() {
        CapturedRequest joined = asyncHarness().captureRequest(c -> c.echo("hello").join(), "https://foo.example.com:8443");
        CapturedRequest notJoined = asyncHarness().captureRequest(c -> c.echo("hello"), "https://foo.example.com:8443");

        assertCaptured(joined);
        assertCaptured(notJoined);
    }

    private static void assertCaptured(CapturedRequest request) {
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.host()).isEqualTo("foo.example.com");
        assertThat(request.httpRequest().port()).isEqualTo(8443);
        assertThat(request.path()).isEqualTo("/echo");
        assertThat(request.queryParameters()).containsExactly(QueryParameter.of("mode", "loud"));
        assertThat(request.header("content-type")).hasValue("text/plain");
        assertThat(request.header("X-Configured")).hasValue("yes");
        assertThat(new String(request.body(), StandardCharsets.UTF_8)).isEqualTo("hello");
    }

    @Test
    void captureRequest_fixesTheIdempotencyTokenDuringTheCall_andRestoresItAfterwards() {
        CapturedRequest request = syncHarness().captureRequest(c -> c.echo("x"), "https://example.com");

        assertThat(request.header(EchoClients.TOKEN_HEADER)).hasValue(FixedIdempotencyToken.TOKEN);
        assertThat(IdempotentUtils.getGenerator().get()).isNotEqualTo(FixedIdempotencyToken.TOKEN);
    }

    @Test
    void captureRequest_installsTheTransportInTheClientConfiguration_andTheCallIsNotRetried() {
        AtomicReference<SdkClientConfiguration> sync = new AtomicReference<>();
        AtomicReference<SdkClientConfiguration> async = new AtomicReference<>();

        syncHarness().captureRequest(c -> {
            sync.set(c.configuration());
            c.echo("x");
        }, "https://example.com");
        asyncHarness().captureRequest(c -> {
            async.set(c.configuration());
            c.echo("x").join();
        }, "https://example.com");

        for (SdkClientConfiguration configuration : new SdkClientConfiguration[] {sync.get(), async.get()}) {
            CapturingClientTransport transport = (CapturingClientTransport) configuration.option(SdkClientOption.CLIENT_TRANSPORT);
            assertThat(configuration.option(SdkClientOption.ASYNC_CLIENT_TRANSPORT)).isSameAs(transport);
            // The client has a retrying strategy; RequestCaptured is not retryable, so exactly one attempt is sent.
            assertThat(transport.requests()).hasSize(1);
        }
    }

    @Test
    void captureRequest_failureBeforeTheSend_propagates() {
        assertThatThrownBy(() -> syncHarness().captureRequest(c -> c.echo(null), "https://example.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("body must not be null");
        assertThatThrownBy(() -> asyncHarness().captureRequest(c -> c.echo(null).join(), "https://example.com"))
            .isInstanceOf(CompletionException.class)
            .hasRootCauseMessage("body must not be null");
    }

    @Test
    void captureRequest_callThatSendsNothing_failsTheTest() {
        assertThatThrownBy(() -> syncHarness().captureRequest(c -> { }, "https://example.com"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining(PROTOCOL)
            .hasMessageContaining("sent no request");
    }

    @Test
    void respondWith_sync_deliversTheCaseStatusHeadersAndBase64DecodedCborBody() {
        EchoResponse response = syncHarness().respondWith(StubbedResponse.fromTestCase(200, CBOR_HEADERS, "v/8=",
                                                                                      "application/cbor"),
                                                          c -> c.echo("ignored"));

        assertThat(response.body()).containsExactly(0xBF, 0xFF);
        assertThat(response.sdkHttpResponse().statusCode()).isEqualTo(200);
        assertThat(response.sdkHttpResponse().firstMatchingHeader("x-answer")).hasValue("42");
    }

    @Test
    void respondWith_async_deliversTheCaseStatusHeadersAndBase64DecodedCborBody() {
        EchoResponse response = asyncHarness().respondWith(StubbedResponse.fromTestCase(200, CBOR_HEADERS, "v/8=",
                                                                                       "application/cbor"),
                                                           c -> c.echo("ignored").join());

        assertThat(response.body()).containsExactly(0xBF, 0xFF);
        assertThat(response.sdkHttpResponse().firstMatchingHeader("x-answer")).hasValue("42");
    }

    @Test
    void respondWith_errorStatus_propagatesTheClientException() {
        StubbedResponse error = StubbedResponse.create(400, Collections.emptyMap(), new byte[0]);

        assertThatThrownBy(() -> syncHarness().respondWith(error, c -> c.echo("x")))
            .isInstanceOfSatisfying(SdkServiceException.class, e -> assertThat(e.statusCode()).isEqualTo(400));
        assertThatThrownBy(() -> asyncHarness().respondWith(error, c -> c.echo("x").join()))
            .isInstanceOf(CompletionException.class)
            .hasCauseInstanceOf(SdkServiceException.class);
    }

    @Test
    void create_withABuilderThatIsNotAnSdkDefaultClientBuilder_failsOnUse() {
        // Every builder method of the proxy returns the proxy itself.
        Supplier<OtherBuilder> builder = () -> (OtherBuilder) Proxy.newProxyInstance(OtherBuilder.class.getClassLoader(),
                                                                                     new Class<?>[] {OtherBuilder.class},
                                                                                     (proxy, method, args) -> proxy);
        ProtocolTestHarness<Object> harness = FACTORY.<Object, OtherBuilder>create(PROTOCOL, builder, b -> { });

        assertThatThrownBy(() -> harness.captureRequest(c -> { }, "https://example.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("SdkDefaultClientBuilder");
    }

    private interface OtherBuilder extends SdkClientBuilder<OtherBuilder, Object> {
    }
}
