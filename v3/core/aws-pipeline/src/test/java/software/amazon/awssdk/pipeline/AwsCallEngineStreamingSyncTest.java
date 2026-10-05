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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_INPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_THING;
import static software.amazon.awssdk.pipeline.PipelineFixtures.STREAM_OUTPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.STREAM_THING;
import static software.amazon.awssdk.pipeline.PipelineFixtures.retries;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.pipeline.PipelineFixtures.Struct;

/** The sync driver's streaming-output path: the transformer consumes the live payload of a successful response. */
class AwsCallEngineStreamingSyncTest {
    private static final byte[] PAYLOAD = "streamed payload bytes".getBytes(StandardCharsets.UTF_8);

    private final FakeHttp http = new FakeHttp();
    private final AwsCallEngine engine = AwsCallEngine.builder()
                                                      .runtime(PipelineFixtures.engineRuntime(
                                                          retries(3), HttpTransportMode.SYNC).build())
                                                      .syncHttpClient(http)
                                                      .build();

    @Test
    void transformer_receivesTheOutputAndTheExactPayload_thenTheExchangeIsClosed() {
        http.stream(200, "v1", PAYLOAD);
        List<Integer> closedWhileTransforming = new ArrayList<>();

        ResponseBytes<Struct> result = engine.executeSync(
            plan(), PipelineFixtures.request(GET_INPUT, "a"), null, null, (output, stream) -> {
                closedWhileTransforming.add(http.closedExchanges);
                return ResponseBytes.fromByteArrayUnsafe(output, stream.readAllBytes());
            });

        assertThat((String) result.response().getMemberValue(STREAM_OUTPUT.member("Value"))).isEqualTo("v1");
        assertThat(result.asByteArrayUnsafe()).isEqualTo(PAYLOAD);
        assertThat(closedWhileTransforming).containsExactly(0);
        assertThat(http.closedExchanges).isEqualTo(1);
        assertThat(http.streamsOpened).isEqualTo(1);
    }

    @Test
    void errorResponse_isReadInFullAndRetried_beforeTheTransformerRuns() {
        http.stream(503, null, "error".getBytes(StandardCharsets.UTF_8)).stream(200, "v2", PAYLOAD);
        List<String> transformed = new ArrayList<>();

        byte[] result = engine.executeSync(plan(), PipelineFixtures.request(GET_INPUT, "a"), null, null,
                                           (output, stream) -> {
                                               transformed.add(output.getMemberValue(STREAM_OUTPUT.member("Value")));
                                               return stream.readAllBytes();
                                           });

        assertThat(result).isEqualTo(PAYLOAD);
        assertThat(transformed).containsExactly("v2");
        assertThat(http.transmitted).hasSize(2);
        assertThat(http.closedExchanges).isEqualTo(2);
    }

    @Test
    void retriesExhausted_surfacesTheServiceError_andNeverCallsTheTransformer() {
        http.stream(503, null, PAYLOAD).stream(503, null, PAYLOAD).stream(503, null, PAYLOAD);
        List<Object> transformed = new ArrayList<>();

        assertThatThrownBy(() -> engine.executeSync(plan(), PipelineFixtures.request(GET_INPUT, "a"), null, null,
                                                    (output, stream) -> transformed.add(output)))
            .isInstanceOfSatisfying(AwsServiceException.class, e -> assertThat(e.statusCode()).isEqualTo(503));
        assertThat(transformed).isEmpty();
        assertThat(http.closedExchanges).isEqualTo(3);
    }

    @Test
    void transformerFailure_isMappedOnce_andNotRetried() {
        http.stream(200, "v1", PAYLOAD).stream(200, "unused", PAYLOAD);
        IOException boom = new IOException("disk full");

        assertThatThrownBy(() -> engine.executeSync(plan(), PipelineFixtures.request(GET_INPUT, "a"), null, null,
                                                    (output, stream) -> {
                                                        stream.read();
                                                        throw boom;
                                                    }))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("com.example#StreamThing")
            .hasCause(boom);
        assertThat(http.transmitted).hasSize(1);
        assertThat(http.closedExchanges).isEqualTo(1);
    }

    @Test
    void transformerThatKeepsTheStreamOpen_transfersCloseToTheReturnedStream() throws IOException {
        http.stream(200, "v1", PAYLOAD);
        ResponseTransformer<Struct, InputStream> keepOpen = new ResponseTransformer<>() {
            @Override
            public InputStream transform(Struct response, InputStream inputStream) {
                return inputStream;
            }

            @Override
            public boolean needsConnectionLeftOpen() {
                return true;
            }
        };

        InputStream stream = engine.executeSync(plan(), PipelineFixtures.request(GET_INPUT, "a"), null, null,
                                                keepOpen);

        assertThat(http.closedExchanges).isZero();
        assertThat(stream.readAllBytes()).isEqualTo(PAYLOAD);
        stream.close();
        assertThat(http.closedExchanges).isEqualTo(1);
    }

    @Test
    void nonStreamingPlan_isRejectedBeforeTheCallStarts() {
        assertThatThrownBy(() -> engine.executeSync(engine.runtime().plan(GET_THING),
                                                    PipelineFixtures.request(GET_INPUT, "a"), null, null,
                                                    (output, stream) -> output))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no streaming blob output member");
        assertThat(http.transmitted).isEmpty();
    }

    private PipelinePlan<Struct, Struct> plan() {
        return engine.runtime().plan(STREAM_THING);
    }

    /** A scripted sync transport whose responses stream their bodies. */
    private static final class FakeHttp implements SdkSyncHttpClient {
        private final Deque<SdkHttpResponse> outcomes = new ArrayDeque<>();
        private final List<SdkHttpRequest> transmitted = new ArrayList<>();
        private int closedExchanges;
        private int streamsOpened;

        FakeHttp stream(int status, String value, byte[] body) {
            SdkHttpResponse.Builder response = SdkHttpResponse.builder().statusCode(status);
            if (value != null) {
                response.addHeader("X-Value", value);
            }
            outcomes.add(response.body(HttpBody.streaming(StreamingBodies.fromInputStreamSupplier(
                () -> {
                    streamsOpened++;
                    return new ByteArrayInputStream(body);
                }, body.length, "application/octet-stream"))).build());
            return this;
        }

        @Override
        public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
            return new SyncHttpExchange() {
                private boolean closed;

                @Override
                public SdkHttpResponse execute() {
                    transmitted.add(request);
                    return outcomes.remove();
                }

                @Override
                public void abort() {
                }

                @Override
                public void close() {
                    if (!closed) {
                        closed = true;
                        closedExchanges++;
                    }
                }
            };
        }

        @Override
        public HttpClientCapabilities capabilities() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String clientName() {
            return "FakeHttp";
        }

        @Override
        public void close() {
        }
    }
}
