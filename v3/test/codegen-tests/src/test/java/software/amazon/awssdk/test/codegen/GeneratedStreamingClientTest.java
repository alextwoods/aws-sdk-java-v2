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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.http.AsyncExchangeCompletion;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.test.codegen.support.CompiledSources;

/**
 * Requirements 7.7 and 8.5: the generated streaming-output overloads of a synthetic restJson1 service, whose output
 * binds a header and a streaming blob {@code @httpPayload}, run through the engine on both clients. A probe written
 * against the generated public API calls the typed and consumer response-transformer overloads with V2 transformer
 * factories and a lambda, over a fake transport whose response bodies stream.
 */
class GeneratedStreamingClientTest {
    private static final String PACKAGE = "software.amazon.awssdk.test.codegen.blobs";
    private static final String PAYLOAD = "streamed payload";

    private static final String MODEL = """
        $version: "2"
        namespace example.blobs

        @aws.protocols#restJson1
        @aws.auth#sigv4(name: "blobs")
        @aws.api#service(sdkId: "Blobs", endpointPrefix: "blobs")
        service Blobs {
            version: "2024-01-01"
            operations: [GetBlob]
        }

        @auth([])
        @readonly
        @http(method: "GET", uri: "/blobs/{name}")
        operation GetBlob {
            input := {
                @required
                @httpLabel
                name: String
            }
            output := {
                @httpHeader("X-Kind")
                kind: String

                @httpPayload
                @required
                body: StreamingBlob
            }
        }

        @streaming
        blob StreamingBlob
        """;

    private static final String PROBE = """
        package example.probe;

        import java.net.URI;
        import java.nio.charset.StandardCharsets;
        import software.amazon.awssdk.core.ResponseBytes;
        import software.amazon.awssdk.core.async.AsyncResponseTransformer;
        import software.amazon.awssdk.core.sync.ResponseTransformer;
        import software.amazon.awssdk.http.SdkAsyncHttpClient;
        import software.amazon.awssdk.http.SdkSyncHttpClient;
        import software.amazon.awssdk.regions.Region;
        import PKG.BlobsAsyncClient;
        import PKG.BlobsClient;
        import PKG.model.GetBlobRequest;
        import PKG.model.GetBlobResponse;

        public final class StreamingProbe {
            private static final URI ENDPOINT = URI.create("https://blobs.test");

            private StreamingProbe() {
            }

            public static String sync(SdkSyncHttpClient http) throws Exception {
                try (BlobsClient client = BlobsClient.builder().region(Region.of("us-east-1"))
                                                     .endpointOverride(ENDPOINT).httpClient(http).build()) {
                    ResponseBytes<GetBlobResponse> typed =
                        client.getBlob(GetBlobRequest.builder().name("a").build(), ResponseTransformer.toBytes());
                    String consumer = client.getBlob(r -> r.name("b"), (response, stream) ->
                        response.kind() + ":" + new String(stream.readAllBytes(), StandardCharsets.UTF_8));
                    return typed.response().kind() + ":" + typed.asUtf8String() + "," + consumer;
                }
            }

            public static String async(SdkAsyncHttpClient http) throws Exception {
                try (BlobsAsyncClient client = BlobsAsyncClient.builder().region(Region.of("us-east-1"))
                                                               .endpointOverride(ENDPOINT).httpClient(http)
                                                               .build()) {
                    ResponseBytes<GetBlobResponse> typed =
                        client.getBlob(GetBlobRequest.builder().name("a").build(),
                                       AsyncResponseTransformer.toBytes()).join();
                    ResponseBytes<GetBlobResponse> consumer =
                        client.getBlob(r -> r.name("b"), AsyncResponseTransformer.<GetBlobResponse>toBytes()).join();
                    return typed.response().kind() + ":" + typed.asUtf8String() + ","
                           + consumer.response().kind() + ":" + consumer.asUtf8String();
                }
            }
        }
        """.replace("PKG", PACKAGE);

    private static final String EXPECTED = "k:" + PAYLOAD + ",k:" + PAYLOAD;

    @TempDir
    static Path temp;

    private static URLClassLoader loader;

    @BeforeAll
    static void generateAndCompile() throws Exception {
        Path models = Files.createDirectories(temp.resolve("model"));
        Files.writeString(models.resolve("blobs.smithy"), MODEL, StandardCharsets.UTF_8);
        AwsCodegenResult result = AwsCodegen.generate(new AwsCodegenRequest("example.blobs#Blobs", PACKAGE, models,
                                                                            temp.resolve("out")), List.of());
        Path sources = temp.resolve("sources");
        copyTree(result.sourceRoot(), sources);
        Path probe = Files.createDirectories(sources.resolve("example/probe")).resolve("StreamingProbe.java");
        Files.writeString(probe, PROBE, StandardCharsets.UTF_8);
        loader = CompiledSources.compile(sources, temp.resolve("classes")).loader();
    }

    @AfterAll
    static void close() throws Exception {
        if (loader != null) {
            loader.close();
        }
    }

    @Test
    void theSyncStreamingOverloadsHandTheLivePayloadToTheTransformer() throws Exception {
        StreamingHttp http = new StreamingHttp(HttpTransportMode.SYNC);
        assertEquals(EXPECTED, probe("sync", SdkSyncHttpClient.class, http));
        assertEquals(2, http.requests.size());
        assertTrue(http.requests.stream().allMatch(r -> "GET".equals(r.method().toString())));
    }

    @Test
    void theAsyncStreamingOverloadsHandTheLivePayloadToTheTransformer() throws Exception {
        StreamingHttp http = new StreamingHttp(HttpTransportMode.ASYNC);
        assertEquals(EXPECTED, probe("async", SdkAsyncHttpClient.class, http));
        assertEquals(2, http.requests.size());
    }

    private static String probe(String method, Class<?> parameter, Object http) throws Exception {
        Method probe = Class.forName("example.probe.StreamingProbe", true, loader).getMethod(method, parameter);
        try {
            return (String) probe.invoke(null, http);
        } catch (InvocationTargetException e) {
            throw new AssertionError("StreamingProbe." + method + " failed", e.getCause());
        }
    }

    private static void copyTree(Path from, Path to) throws Exception {
        try (var walk = Files.walk(from)) {
            for (Path source : walk.toList()) {
                Path target = to.resolve(from.relativize(source).toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(source, target);
                }
            }
        }
    }

    /** A sync and async transport answering every request with the {@code X-Kind} header and a streaming body. */
    static final class StreamingHttp implements SdkSyncHttpClient, SdkAsyncHttpClient {
        final List<SdkHttpRequest> requests = new CopyOnWriteArrayList<>();
        private final HttpTransportMode mode;

        StreamingHttp(HttpTransportMode mode) {
            this.mode = mode;
        }

        private static SdkHttpResponse.Builder headers() {
            return SdkHttpResponse.builder().statusCode(200).addHeader("X-Kind", "k")
                                  .addHeader("Content-Type", "application/octet-stream");
        }

        @Override
        public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
            requests.add(request);
            return new SyncHttpExchange() {
                @Override
                public SdkHttpResponse execute() {
                    byte[] bytes = PAYLOAD.getBytes(StandardCharsets.UTF_8);
                    return headers().body(HttpBody.streaming(StreamingBodies.fromInputStream(
                        new ByteArrayInputStream(bytes), bytes.length, "application/octet-stream"))).build();
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
            requests.add(request);
            AsyncExchangeCompletion exchange =
                new AsyncExchangeCompletion(clientName(), (reason, failure) -> handler.onError(failure));
            handler.onHeaders(headers().body(HttpBody.empty()).build());
            handler.onStream(publisherOf(ByteBuffer.wrap(PAYLOAD.getBytes(StandardCharsets.UTF_8))));
            exchange.complete();
            return exchange;
        }

        @Override
        public HttpClientCapabilities capabilities() {
            return HttpClientCapabilities.builder().mode(mode).addProtocol(Protocol.HTTP1_1)
                                         .maxBufferedBytesPerExchange(1 << 20)
                                         .cancellationTimeout(Duration.ofSeconds(1)).build();
        }

        @Override
        public String clientName() {
            return "StreamingHttp";
        }

        @Override
        public void close() {
        }
    }

    private static Publisher<ByteBuffer> publisherOf(ByteBuffer bytes) {
        return subscriber -> subscriber.onSubscribe(new Subscription() {
            private boolean done;

            @Override
            public void request(long n) {
                if (done) {
                    return;
                }
                done = true;
                subscriber.onNext(bytes.asReadOnlyBuffer());
                subscriber.onComplete();
            }

            @Override
            public void cancel() {
                done = true;
            }
        });
    }
}
