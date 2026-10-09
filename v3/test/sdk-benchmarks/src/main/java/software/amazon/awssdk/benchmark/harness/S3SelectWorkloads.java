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

package software.amazon.awssdk.benchmark.harness;

import static software.amazon.awssdk.benchmark.harness.FiniteWorkloads.response;
import static software.amazon.awssdk.benchmark.harness.FiniteWorkloads.utf8;
import static software.amazon.awssdk.benchmark.harness.LambdaEventStreamWorkloads.CHUNKS;
import static software.amazon.awssdk.benchmark.harness.LambdaEventStreamWorkloads.CHUNK_BYTES;
import static software.amazon.awssdk.benchmark.harness.LambdaEventStreamWorkloads.event;
import static software.amazon.awssdk.benchmark.harness.LambdaEventStreamWorkloads.write;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.benchmark.harness.LambdaEventStreamWorkloads.EventCheck;
import software.amazon.awssdk.benchmark.harness.LambdaEventStreamWorkloads.EventStreamWorkload;
import software.amazon.awssdk.dynamic.client.DynamicAsyncClient;
import software.amazon.awssdk.dynamic.client.DynamicClient;
import software.amazon.awssdk.eventstream.MessageEncoder;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ExpressionType;
import software.amazon.awssdk.services.s3.model.FileHeaderInfo;
import software.amazon.awssdk.services.s3.model.SelectObjectContentEventStream;
import software.amazon.awssdk.services.s3.model.SelectObjectContentRequest;
import software.amazon.awssdk.services.s3.model.SelectObjectContentResponse;
import software.amazon.awssdk.services.s3.model.SelectObjectContentResponseHandler;
import software.amazon.awssdk.services.s3.model.Stats;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The restXml Event Stream fixture (Req 20.3): S3 {@code SelectObjectContent}. The mock service answers with an
 * {@code application/vnd.amazon.eventstream} body of {@link LambdaEventStreamWorkloads#CHUNKS} {@code Records}
 * events of {@link LambdaEventStreamWorkloads#CHUNK_BYTES} bytes each (consecutive slices of
 * {@link StreamingWorkloads#content}), one {@code Stats} event (an XML event payload whose three byte counts are the
 * records' total) and one {@code End} event, every message framed by V3's {@code core/eventstream}
 * {@link MessageEncoder}.
 *
 * <p>The client consumes the events one at a time (demand of one on the async paths, one pull per event on
 * dynamic-sync). Every operation fails unless it received exactly the records totalling the content's first
 * {@code CHUNKS * CHUNK_BYTES} bytes, then the stats event, then the end event; {@link CellWorkload#verify()}
 * additionally compares every record byte, in order, with the content and the decoded stats with the fixture's.
 *
 * <p>Paths: generated-async, dynamic-async and dynamic-sync (the Dynamic Client's sync output document holds the
 * event stream reader). Generated-sync Event Stream cells are inapplicable ({@link CellMatrix#inapplicability}).
 *
 * <p>The mock routes on the path the V3 S3 client actually sends to an IP-address endpoint override: as for
 * {@link S3Workloads}, {@code /bucket/bucket/key}, the known V3 S3 defect of the bucket bound both by the endpoint
 * rules' path-style URL and by the modeled {@code {Bucket}} label; it is not corrected here. The request body must be
 * exactly {@link #REQUEST}, so a client sending a different path or request is {@code unmatched} and invalidates the
 * run.
 */
final class S3SelectWorkloads implements WorkloadProvider {
    static final String BUCKET = S3Workloads.BUCKET;
    static final String KEY = "benchmark-select-object";
    static final String EXPRESSION = "SELECT * FROM S3Object";
    /** The path the V3 S3 client sends (see the class comment); the query string is {@code ?select&select-type=2}. */
    static final String PATH = "/" + BUCKET + "/" + BUCKET + "/" + KEY;
    static final long STATS_BYTES = (long) CHUNKS * CHUNK_BYTES;
    /** The completion value {@link EventCheck} expects of the decoded stats event. */
    static final String STATS = STATS_BYTES + "/" + STATS_BYTES + "/" + STATS_BYTES;
    /** The exact request body every client path sends for the request below. */
    static final byte[] REQUEST = utf8(
        "<SelectObjectContentRequest xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">"
        + "<Expression>" + EXPRESSION + "</Expression><ExpressionType>SQL</ExpressionType>"
        + "<InputSerialization><CSV><FileHeaderInfo>NONE</FileHeaderInfo></CSV></InputSerialization>"
        + "<OutputSerialization><CSV></CSV></OutputSerialization></SelectObjectContentRequest>");
    private static final String XML = "application/xml";
    private static final ShapeId SERVICE = ShapeId.from("com.amazonaws.s3#AmazonS3");
    private static final String OPERATION = "SelectObjectContent";
    private static final String EVENT_STREAM_MEMBER = "Payload";

    @Override
    public String name() {
        return "s3-selectobjectcontent";
    }

    @Override
    public boolean supports(BenchmarkCell cell) {
        // generated-sync Event Stream cells are inapplicable and never reach a provider (CellMatrix.inapplicability)
        return cell.protocol() == BenchProtocol.REST_XML && cell.kind() == WorkloadKind.EVENT_STREAM
               && cell.path() != ClientPath.GENERATED_SYNC;
    }

    /** The framed response body: the records events, the stats event, then the end event. */
    static byte[] eventStreamBody() {
        byte[] content = StreamingWorkloads.sharedContent();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        for (int i = 0; i < CHUNKS; i++) {
            write(body, event("Records", "application/octet-stream",
                              Arrays.copyOfRange(content, i * CHUNK_BYTES, (i + 1) * CHUNK_BYTES)));
        }
        write(body, event("Stats", "text/xml", utf8(
            "<Stats><BytesScanned>" + STATS_BYTES + "</BytesScanned><BytesProcessed>" + STATS_BYTES
            + "</BytesProcessed><BytesReturned>" + STATS_BYTES + "</BytesReturned></Stats>")));
        write(body, event("End", null, new byte[0]));
        return body.toByteArray();
    }

    @Override
    public ServiceFixture fixture(BenchmarkCell cell) {
        ServiceFixture.MockResponse success = response(200, "application/vnd.amazon.eventstream", eventStreamBody(),
                                                       Map.of());
        ServiceFixture.MockResponse error = response(404, XML, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Error>"
                                                               + "<Code>NoSuchKey</Code><Message>The specified key "
                                                               + "does not exist.</Message></Error>");
        ServiceFixture.MockResponse retry = response(500, XML, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<Error>"
                                                               + "<Code>InternalError</Code><Message>injected retry"
                                                               + "</Message></Error>");
        return new ServiceFixture() {
            @Override
            public boolean matches(String method, String path, String routingHeader) {
                return "POST".equals(method) && PATH.equals(path);
            }

            @Override
            public boolean matchesBody(byte[] body) {
                return Arrays.equals(REQUEST, body);
            }

            @Override
            public String routingHeaderName() {
                return null;
            }

            @Override
            public MockResponse success() {
                return success;
            }

            @Override
            public MockResponse modeledError() {
                return error;
            }

            @Override
            public MockResponse retryableError() {
                return retry;
            }

            @Override
            public long encodedBytes() {
                return REQUEST.length + (long) success.body().length;
            }
        };
    }

    private static Document dynamicInput() {
        Map<String, Document> input = new LinkedHashMap<>();
        input.put("Bucket", Document.of(BUCKET));
        input.put("Key", Document.of(KEY));
        input.put("Expression", Document.of(EXPRESSION));
        input.put("ExpressionType", Document.of("SQL"));
        input.put("InputSerialization", Document.of(Map.of(
            "CSV", Document.of(Map.of("FileHeaderInfo", Document.of("NONE"))))));
        input.put("OutputSerialization", Document.of(Map.of("CSV", Document.of(Map.of()))));
        return Document.of(input);
    }

    @Override
    public CellWorkload create(BenchmarkCell cell, URI endpoint, Options options) {
        AutoCloseable transport = cell.transport().create(cell.concurrency());
        try {
            Path model = options.models().getOrDefault("s3", Path.of("../../services/s3/src/main/smithy/model.json"));
            if (cell.path() == ClientPath.DYNAMIC_SYNC) {
                DynamicClient dynamic = DynamicClient.builder().model(model).serviceId(SERVICE)
                    .region(Region.US_EAST_1).credentialsProvider(DynamoDbWorkloads.credentials())
                    .endpointOverride(endpoint).retryStrategy(DynamoDbWorkloads.retryStrategy())
                    .httpClient((SdkSyncHttpClient) transport).build();
                Document input = dynamicInput();
                return EventStreamWorkload.sync(cell, transport, dynamic, exact -> LambdaEventStreamWorkloads.readSync(
                    dynamic.call(OPERATION, input), EVENT_STREAM_MEMBER, check(exact),
                    S3SelectWorkloads::onDynamicEvent));
            }
            if (cell.path() == ClientPath.DYNAMIC_ASYNC) {
                DynamicAsyncClient dynamic = DynamicAsyncClient.builder().model(model).serviceId(SERVICE)
                    .region(Region.US_EAST_1).credentialsProvider(DynamoDbWorkloads.credentials())
                    .endpointOverride(endpoint).retryStrategy(DynamoDbWorkloads.retryStrategy())
                    .httpClient((SdkAsyncHttpClient) transport).build();
                Document input = dynamicInput();
                return EventStreamWorkload.async(cell, transport, dynamic, exact -> {
                    CompletableFuture<Void> done = new CompletableFuture<>();
                    dynamic.callAsync(OPERATION, input, LambdaEventStreamWorkloads.<Document, Document>handler(
                        check(exact), S3SelectWorkloads::onDynamicEvent, done)).whenComplete((r, t) -> {
                            if (t != null) {
                                done.completeExceptionally(t);
                            }
                        });
                    return done;
                });
            }
            S3AsyncClient generated = S3AsyncClient.builder().region(Region.US_EAST_1)
                .credentialsProvider(DynamoDbWorkloads.credentials()).endpointOverride(endpoint)
                .retryStrategy(DynamoDbWorkloads.retryStrategy()).httpClient((SdkAsyncHttpClient) transport).build();
            SelectObjectContentRequest request = SelectObjectContentRequest.builder().bucket(BUCKET).key(KEY)
                .expression(EXPRESSION).expressionType(ExpressionType.SQL)
                .inputSerialization(i -> i.csv(c -> c.fileHeaderInfo(FileHeaderInfo.NONE)))
                .outputSerialization(o -> o.csv(c -> { }))
                .build();
            return EventStreamWorkload.async(cell, transport, generated, exact -> {
                CompletableFuture<Void> done = new CompletableFuture<>();
                var handler = LambdaEventStreamWorkloads.<SelectObjectContentResponse, SelectObjectContentEventStream>
                    handler(check(exact), S3SelectWorkloads::onGeneratedEvent, done);
                generated.selectObjectContent(request, new SelectObjectContentResponseHandler() {
                    @Override
                    public void responseReceived(SelectObjectContentResponse response) {
                        handler.responseReceived(response);
                    }

                    @Override
                    public void onEventStream(
                        software.amazon.awssdk.core.async.SdkPublisher<SelectObjectContentEventStream> publisher) {
                        handler.onEventStream(publisher);
                    }

                    @Override
                    public void exceptionOccurred(Throwable throwable) {
                        handler.exceptionOccurred(throwable);
                    }

                    @Override
                    public void complete() {
                        handler.complete();
                    }
                }).whenComplete((r, t) -> {
                    if (t != null) {
                        done.completeExceptionally(t);
                    }
                });
                return done;
            });
        } catch (RuntimeException e) {
            try {
                transport.close();
            } catch (Exception ignored) {
                // best effort while failing construction
            }
            throw e;
        }
    }

    private static EventCheck check(boolean exact) {
        return new EventCheck(exact, STATS, true);
    }

    private static String stats(Long scanned, Long processed, Long returned) {
        return scanned + "/" + processed + "/" + returned;
    }

    private static void onGeneratedEvent(EventCheck check, SelectObjectContentEventStream event) {
        switch (event.type()) {
            case RECORDS -> check.chunk(event.records().payload().asByteBuffer());
            case STATS -> {
                Stats details = event.stats().details();
                check.complete(details == null ? null : stats(details.bytesScanned(), details.bytesProcessed(),
                                                              details.bytesReturned()));
            }
            case END -> check.end();
            default -> throw new IllegalStateException("unexpected event " + event);
        }
    }

    private static void onDynamicEvent(EventCheck check, Document event) {
        Document records = event.getMember("Records");
        Document stats = event.getMember("Stats");
        if (records != null) {
            check.chunk(records.getMember("Payload").asBlob());
        } else if (stats != null) {
            Document details = stats.getMember("Details");
            check.complete(details == null ? null : stats(number(details, "BytesScanned"),
                                                          number(details, "BytesProcessed"),
                                                          number(details, "BytesReturned")));
        } else if (event.getMember("End") != null) {
            check.end();
        } else {
            throw new IllegalStateException("unexpected event " + event);
        }
    }

    private static Long number(Document struct, String member) {
        Document value = struct.getMember(member);
        return value == null ? null : value.asLong();
    }
}
