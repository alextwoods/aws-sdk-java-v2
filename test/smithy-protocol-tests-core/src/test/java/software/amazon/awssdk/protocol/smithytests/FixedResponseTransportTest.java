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
import static software.amazon.awssdk.protocol.smithytests.CapturingClientTransportTest.request;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.client.protocol.AsyncResponseHandoff;
import software.amazon.awssdk.core.client.protocol.HttpMessageExchange;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.IoUtils;

class FixedResponseTransportTest {
    private static final Map<String, String> HEADERS = ProtocolTestValues.mapOf("Content-Type", "application/cbor",
                                                                               "X-Multi", "a, b");
    /** {@code v/8=} is the CBOR empty indefinite-length map {@code 0xBF 0xFF}. */
    private static final byte[] CBOR_EMPTY_MAP = {(byte) 0xBF, (byte) 0xFF};

    private static StubbedResponse cborCase() {
        return StubbedResponse.fromTestCase(202, HEADERS, "v/8=", "application/cbor");
    }

    @Test
    void messageExchange_isHttp() {
        assertThat(FixedResponseTransport.create(cborCase()).messageExchange()).isSameAs(HttpMessageExchange.INSTANCE);
    }

    @Test
    void syncSend_returnsTheCaseStatusHeadersAndBase64DecodedCborBody() throws IOException {
        FixedResponseTransport transport = FixedResponseTransport.create(cborCase());

        SdkHttpFullResponse response = transport.send(new ExecutionAttributes(), request("in"));

        assertThat(response.statusCode()).isEqualTo(202);
        assertThat(response.headers()).containsEntry("Content-Type", Collections.singletonList("application/cbor"))
                                      .containsEntry("X-Multi", Collections.singletonList("a, b"));
        assertThat(read(response)).isEqualTo(CBOR_EMPTY_MAP);
    }

    @Test
    void syncSend_returnsAFreshBodyStreamForEveryAttempt_andRecordsEachRequest() throws IOException {
        StubbedResponse text = StubbedResponse.fromTestCase(500, Collections.emptyMap(), "{\"a\":1}", "application/json");
        FixedResponseTransport transport = FixedResponseTransport.create(text);

        SdkHttpFullResponse first = transport.send(new ExecutionAttributes(), request("one"));
        SdkHttpFullResponse second = transport.send(new ExecutionAttributes(), request("two"));

        assertThat(new String(read(first), StandardCharsets.UTF_8)).isEqualTo("{\"a\":1}");
        assertThat(new String(read(second), StandardCharsets.UTF_8)).isEqualTo("{\"a\":1}");
        assertThat(transport.requests()).extracting(r -> new String(r.body(), StandardCharsets.UTF_8))
                                        .containsExactly("one", "two");
    }

    @Test
    void syncSend_emptyCaseBody_isAnEmptyStream() throws IOException {
        FixedResponseTransport transport =
            FixedResponseTransport.create(StubbedResponse.fromTestCase(200, Collections.emptyMap(), "", "application/cbor"));

        assertThat(read(transport.send(new ExecutionAttributes(), request(null)))).isEmpty();
    }

    @Test
    void asyncSend_withoutHandoff_completesWithTheBufferedResponse() throws Exception {
        FixedResponseTransport transport = FixedResponseTransport.create(cborCase());

        SdkHttpFullResponse response = transport.send(new ExecutionAttributes(), request("in"), null).get();

        assertThat(response.statusCode()).isEqualTo(202);
        assertThat(read(response)).isEqualTo(CBOR_EMPTY_MAP);
        assertThat(transport.requests()).hasSize(1);
    }

    @Test
    void asyncSend_handoffTakingTheStream_getsTheBody_andTheFutureHasOnlyTheHeaders() throws Exception {
        FixedResponseTransport transport = FixedResponseTransport.create(cborCase());
        AtomicReference<SdkHttpFullResponse> headers = new AtomicReference<>();
        CompletableFuture<byte[]> streamed = new CompletableFuture<>();

        SdkHttpFullResponse response = transport.send(new ExecutionAttributes(), request("in"),
                                                      handoff(true, headers, streamed)).get();

        assertThat(headers.get().statusCode()).isEqualTo(202);
        assertThat(headers.get().content()).isEmpty();
        assertThat(streamed.get()).isEqualTo(CBOR_EMPTY_MAP);
        assertThat(response).isSameAs(headers.get());
    }

    @Test
    void asyncSend_handoffDecliningTheStream_completesWithTheBufferedResponse() throws Exception {
        FixedResponseTransport transport = FixedResponseTransport.create(cborCase());
        AtomicReference<SdkHttpFullResponse> headers = new AtomicReference<>();
        CompletableFuture<byte[]> streamed = new CompletableFuture<>();

        SdkHttpFullResponse response = transport.send(new ExecutionAttributes(), request("in"),
                                                      handoff(false, headers, streamed)).get();

        assertThat(headers.get()).isNotNull();
        assertThat(streamed).isNotDone();
        assertThat(read(response)).isEqualTo(CBOR_EMPTY_MAP);
    }

    private static AsyncResponseHandoff<SdkHttpFullResponse> handoff(boolean takeStream,
                                                                     AtomicReference<SdkHttpFullResponse> headers,
                                                                     CompletableFuture<byte[]> streamed) {
        return new AsyncResponseHandoff<SdkHttpFullResponse>() {
            @Override
            public boolean onHeaders(SdkHttpFullResponse response) {
                headers.set(response);
                return takeStream;
            }

            @Override
            public void onStream(Publisher<ByteBuffer> body) {
                collect(body, streamed);
            }
        };
    }

    static void collect(Publisher<ByteBuffer> body, CompletableFuture<byte[]> result) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        body.subscribe(new Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer buffer) {
                byte[] chunk = new byte[buffer.remaining()];
                buffer.get(chunk);
                out.write(chunk, 0, chunk.length);
            }

            @Override
            public void onError(Throwable t) {
                result.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                result.complete(out.toByteArray());
            }
        });
    }

    private static byte[] read(SdkHttpFullResponse response) throws IOException {
        try (InputStream in = response.content().get()) {
            return IoUtils.toByteArray(in);
        }
    }
}
