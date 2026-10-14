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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.core.client.protocol.AsyncResponseHandoff;
import software.amazon.awssdk.core.client.protocol.HttpMessageExchange;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;

class CapturingClientTransportTest {

    static SdkHttpFullRequest request(String body) {
        SdkHttpFullRequest.Builder builder = SdkHttpFullRequest.builder()
                                                               .method(SdkHttpMethod.PUT)
                                                               .protocol("https")
                                                               .host("example.com")
                                                               .encodedPath("/things/a%20b")
                                                               .putRawQueryParameter("q", "1 2")
                                                               .putHeader("X-Foo", "Foo")
                                                               .appendHeader("X-Foo", "Bar");
        if (body != null) {
            builder.contentStreamProvider(ContentStreamProvider.fromUtf8String(body));
        }
        return builder.build();
    }

    @Test
    void messageExchange_isHttp() {
        assertThat(CapturingClientTransport.create().messageExchange()).isSameAs(HttpMessageExchange.INSTANCE);
    }

    @Test
    void syncSend_recordsMethodUriHeadersAndBody_andThrowsRequestCaptured() {
        CapturingClientTransport transport = CapturingClientTransport.create();

        assertThatThrownBy(() -> transport.send(new ExecutionAttributes(), request("payload")))
            .isInstanceOfSatisfying(RequestCaptured.class, e -> {
                assertThat(e.request()).isSameAs(transport.firstRequest().get());
                assertThat(e.getStackTrace()).isEmpty();
            });

        CapturedRequest captured = transport.firstRequest().get();
        assertThat(captured.method()).isEqualTo("PUT");
        assertThat(captured.host()).isEqualTo("example.com");
        assertThat(captured.path()).isEqualTo("/things/a%20b");
        assertThat(captured.queryParameters()).containsExactly(QueryParameter.of("q", "1 2"));
        assertThat(captured.header("x-foo")).hasValue("Foo, Bar");
        assertThat(new String(captured.body(), StandardCharsets.UTF_8)).isEqualTo("payload");
        assertThat(transport.requests()).containsExactly(captured);
    }

    @Test
    void syncSend_withoutBody_capturesAnEmptyBody() {
        CapturingClientTransport transport = CapturingClientTransport.create();

        assertThatThrownBy(() -> transport.send(new ExecutionAttributes(), request(null)))
            .isInstanceOf(RequestCaptured.class);

        assertThat(transport.firstRequest().get().body()).isEmpty();
    }

    @Test
    void everySend_isRecorded_andTheFirstIsKept() {
        CapturingClientTransport transport = CapturingClientTransport.create();

        assertThatThrownBy(() -> transport.send(new ExecutionAttributes(), request("one"))).isInstanceOf(RequestCaptured.class);
        assertThatThrownBy(() -> transport.send(new ExecutionAttributes(), request("two"))).isInstanceOf(RequestCaptured.class);

        assertThat(transport.requests()).extracting(r -> new String(r.body(), StandardCharsets.UTF_8))
                                        .containsExactly("one", "two");
        assertThat(transport.firstRequest().get().body()).isEqualTo("one".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void asyncSend_recordsTheRequest_andFailsTheFutureWithoutCallingTheHandoff() throws Exception {
        CapturingClientTransport transport = CapturingClientTransport.create();
        AtomicBoolean handoffCalled = new AtomicBoolean();
        AsyncResponseHandoff<SdkHttpFullResponse> handoff = new AsyncResponseHandoff<SdkHttpFullResponse>() {
            @Override
            public boolean onHeaders(SdkHttpFullResponse response) {
                handoffCalled.set(true);
                return true;
            }

            @Override
            public void onStream(Publisher<ByteBuffer> body) {
                handoffCalled.set(true);
            }
        };

        CompletableFuture<SdkHttpFullResponse> future = transport.send(new ExecutionAttributes(), request("async"), handoff);

        assertThat(future).isCompletedExceptionally();
        assertThatThrownBy(future::get).isInstanceOf(ExecutionException.class)
                                       .hasCauseInstanceOf(RequestCaptured.class);
        assertThat(handoffCalled).isFalse();
        assertThat(transport.awaitFirstRequest(Duration.ofSeconds(1)).body())
            .isEqualTo("async".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void awaitFirstRequest_withoutAnySend_failsAfterTheTimeout() {
        assertThatThrownBy(() -> CapturingClientTransport.create().awaitFirstRequest(Duration.ofMillis(10)))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("sent no request");
        assertThat(CapturingClientTransport.create().firstRequest()).isEmpty();
    }
}
