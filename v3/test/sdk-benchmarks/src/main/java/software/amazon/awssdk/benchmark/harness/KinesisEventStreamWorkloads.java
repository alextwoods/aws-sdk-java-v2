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
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.benchmark.harness.LambdaEventStreamWorkloads.EventCheck;
import software.amazon.awssdk.benchmark.harness.LambdaEventStreamWorkloads.EventStreamWorkload;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.dynamic.client.DynamicAsyncClient;
import software.amazon.awssdk.dynamic.client.DynamicClient;
import software.amazon.awssdk.eventstream.MessageEncoder;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.Record;
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardEvent;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardEventStream;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardRequest;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardResponse;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardResponseHandler;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The awsJson1.1 Event Stream fixture (Req 20.3): Kinesis {@code SubscribeToShard}. The mock service answers with an
 * {@code application/vnd.amazon.eventstream} body of the RPC protocol's {@code initial-response} event (the empty
 * output) then {@link LambdaEventStreamWorkloads#CHUNKS} {@code SubscribeToShardEvent} events, each with one record
 * whose {@code Data} (base64 in the event's JSON payload) is the next {@link LambdaEventStreamWorkloads#CHUNK_BYTES}
 * bytes of {@link StreamingWorkloads#content}, every message framed by V3's {@code core/eventstream}
 * {@link MessageEncoder}. The stream then ends, as a Kinesis subscription does when the service closes it.
 *
 * <p>The client consumes the events one at a time (demand of one on the async paths, one pull per event on
 * dynamic-sync). Every operation fails unless it received exactly {@code CHUNKS} records totalling
 * {@code CHUNKS * CHUNK_BYTES} bytes; {@link CellWorkload#verify()} additionally compares every record byte, in order,
 * with the content, and each record's sequence number, partition key and the event's continuation sequence number and
 * {@code MillisBehindLatest} with the fixture's.
 *
 * <p>HTTP version: the Kinesis model lists {@code eventStreamHttp: ["h2"]} (the real service serves
 * {@code SubscribeToShard} only over HTTP/2), but neither the generated nor the Dynamic Client forces HTTP/2 for it:
 * V3's construction-time protocol/transport check is built with no event-stream operations
 * ({@code AwsClientBuildState} passes {@code false}), and every cell's transport is built with its default HTTP/1.1
 * protocol, so against the cleartext loopback endpoint every path and transport sends {@code SubscribeToShard} as an
 * HTTP/1.1 request and reads the event stream from the HTTP/1.1 response body. That is the exchange this fixture
 * serves and these cells measure; they do not measure HTTP/2 framing.
 *
 * <p>Paths: generated-async, dynamic-async and dynamic-sync (the Dynamic Client's sync output document holds the
 * event stream reader). Generated-sync Event Stream cells are inapplicable ({@link CellMatrix#inapplicability}).
 */
final class KinesisEventStreamWorkloads implements WorkloadProvider {
    static final String CONSUMER_ARN =
        "arn:aws:kinesis:us-east-1:123456789012:stream/benchmark-stream/consumer/benchmark-consumer:1";
    static final String SHARD = "shardId-000000000000";
    static final String KEY = KinesisWorkloads.KEY;
    static final long ARRIVAL_SECONDS = 1_700_000_000L;
    static final byte[] REQUEST = utf8("{\"ConsumerARN\":\"" + CONSUMER_ARN + "\",\"ShardId\":\"" + SHARD
                                       + "\",\"StartingPosition\":{\"Type\":\"LATEST\"}}");
    private static final String TARGET = "Kinesis_20131202.SubscribeToShard";
    private static final ShapeId SERVICE = ShapeId.from("com.amazonaws.kinesis#Kinesis_20131202");
    private static final String OPERATION = "SubscribeToShard";
    private static final String EVENT_STREAM_MEMBER = "EventStream";
    private static final String JSON = KinesisWorkloads.CONTENT_TYPE;

    @Override
    public String name() {
        return "kinesis-subscribetoshard";
    }

    @Override
    public boolean supports(BenchmarkCell cell) {
        // generated-sync Event Stream cells are inapplicable and never reach a provider (CellMatrix.inapplicability)
        return cell.protocol() == BenchProtocol.AWS_JSON_1_1 && cell.kind() == WorkloadKind.EVENT_STREAM
               && cell.path() != ClientPath.GENERATED_SYNC;
    }

    /** The sequence number of the {@code i}th record (0-based). */
    static String sequence(int i) {
        return Integer.toString(i + 1);
    }

    /** The framed response body: the initial response, then the record events. */
    static byte[] eventStreamBody() {
        byte[] content = StreamingWorkloads.sharedContent();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        write(body, event("initial-response", JSON, utf8("{}")));
        Base64.Encoder base64 = Base64.getEncoder();
        for (int i = 0; i < CHUNKS; i++) {
            String data = base64.encodeToString(Arrays.copyOfRange(content, i * CHUNK_BYTES, (i + 1) * CHUNK_BYTES));
            write(body, event("SubscribeToShardEvent", JSON, utf8(
                "{\"Records\":[{\"SequenceNumber\":\"" + sequence(i) + "\",\"ApproximateArrivalTimestamp\":"
                + ARRIVAL_SECONDS + ",\"Data\":\"" + data + "\",\"PartitionKey\":\"" + KEY + "\"}],"
                + "\"ContinuationSequenceNumber\":\"" + sequence(i) + "\",\"MillisBehindLatest\":0}")));
        }
        return body.toByteArray();
    }

    @Override
    public ServiceFixture fixture(BenchmarkCell cell) {
        ServiceFixture.MockResponse success = response(200, "application/vnd.amazon.eventstream", eventStreamBody(),
                                                       Map.of());
        ServiceFixture.MockResponse error = response(
            400, JSON, "{\"__type\":\"ResourceNotFoundException\",\"message\":\"Consumer not found\"}");
        ServiceFixture.MockResponse retry = response(
            500, JSON, "{\"__type\":\"InternalFailureException\",\"message\":\"injected retry\"}");
        return new ServiceFixture() {
            @Override
            public boolean matches(String method, String path, String routingHeader) {
                return "POST".equals(method) && "/".equals(path) && TARGET.equals(routingHeader);
            }

            @Override
            public boolean matchesBody(byte[] body) {
                return Arrays.equals(REQUEST, body);
            }

            @Override
            public String routingHeaderName() {
                return "X-Amz-Target";
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
        input.put("ConsumerARN", Document.of(CONSUMER_ARN));
        input.put("ShardId", Document.of(SHARD));
        input.put("StartingPosition", Document.of(Map.of("Type", Document.of("LATEST"))));
        return Document.of(input);
    }

    @Override
    public CellWorkload create(BenchmarkCell cell, URI endpoint, Options options) {
        AutoCloseable transport = cell.transport().create(cell.concurrency());
        try {
            Path model = options.models().getOrDefault(
                "kinesis", Path.of("../../services/kinesis/src/main/smithy/model.json"));
            if (cell.path() == ClientPath.DYNAMIC_SYNC) {
                DynamicClient dynamic = DynamicClient.builder().model(model).serviceId(SERVICE)
                    .region(Region.US_EAST_1).credentialsProvider(DynamoDbWorkloads.credentials())
                    .endpointOverride(endpoint).retryStrategy(DynamoDbWorkloads.retryStrategy())
                    .httpClient((SdkSyncHttpClient) transport).build();
                Document input = dynamicInput();
                return EventStreamWorkload.sync(cell, transport, dynamic, exact -> LambdaEventStreamWorkloads.readSync(
                    dynamic.call(OPERATION, input), EVENT_STREAM_MEMBER, check(exact),
                    KinesisEventStreamWorkloads::onDynamicEvent));
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
                        check(exact), KinesisEventStreamWorkloads::onDynamicEvent, done)).whenComplete((r, t) -> {
                            if (t != null) {
                                done.completeExceptionally(t);
                            }
                        });
                    return done;
                });
            }
            KinesisAsyncClient generated = KinesisAsyncClient.builder().region(Region.US_EAST_1)
                .credentialsProvider(DynamoDbWorkloads.credentials()).endpointOverride(endpoint)
                .retryStrategy(DynamoDbWorkloads.retryStrategy()).httpClient((SdkAsyncHttpClient) transport).build();
            SubscribeToShardRequest request = SubscribeToShardRequest.builder().consumerARN(CONSUMER_ARN)
                .shardId(SHARD).startingPosition(p -> p.type(ShardIteratorType.LATEST)).build();
            return EventStreamWorkload.async(cell, transport, generated, exact -> {
                CompletableFuture<Void> done = new CompletableFuture<>();
                var handler = LambdaEventStreamWorkloads.<SubscribeToShardResponse, SubscribeToShardEventStream>
                    handler(check(exact), KinesisEventStreamWorkloads::onGeneratedEvent, done);
                generated.subscribeToShard(request, new SubscribeToShardResponseHandler() {
                    @Override
                    public void responseReceived(SubscribeToShardResponse response) {
                        handler.responseReceived(response);
                    }

                    @Override
                    public void onEventStream(SdkPublisher<SubscribeToShardEventStream> publisher) {
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
        return new EventCheck(exact, null, false);
    }

    /** Checks one decoded record event's members (with {@code exact}) and feeds its data to {@code check}. */
    private static void record(EventCheck check, int records, String sequence, String partitionKey,
                               Long arrivalSeconds, String continuation, Long millisBehind, ByteBuffer data) {
        String expected = sequence(check.chunks());
        if (records != 1) {
            throw new IllegalStateException("event with " + records + " records, expected 1");
        }
        if (check.exact() && (!expected.equals(sequence) || !KEY.equals(partitionKey)
                              || arrivalSeconds == null || arrivalSeconds != ARRIVAL_SECONDS
                              || !expected.equals(continuation) || millisBehind == null || millisBehind != 0L)) {
            throw new IllegalStateException("record event " + expected + " differs from the fixture: sequence="
                                            + sequence + " key=" + partitionKey + " arrival=" + arrivalSeconds
                                            + " continuation=" + continuation + " millisBehind=" + millisBehind);
        }
        check.chunk(data);
    }

    private static void onGeneratedEvent(EventCheck check, SubscribeToShardEventStream event) {
        if (event.type() != SubscribeToShardEventStream.Type.SUBSCRIBE_TO_SHARD_EVENT) {
            throw new IllegalStateException("unexpected event " + event);
        }
        SubscribeToShardEvent e = event.subscribeToShardEvent();
        List<Record> records = e.records();
        Record r = records.isEmpty() ? null : records.get(0);
        record(check, records.size(), r == null ? null : r.sequenceNumber(), r == null ? null : r.partitionKey(),
               r == null || r.approximateArrivalTimestamp() == null ? null
                                                                    : r.approximateArrivalTimestamp().getEpochSecond(),
               e.continuationSequenceNumber(), e.millisBehindLatest(),
               r == null ? ByteBuffer.allocate(0) : r.data().asByteBuffer());
    }

    private static void onDynamicEvent(EventCheck check, Document event) {
        Document e = event.getMember("SubscribeToShardEvent");
        if (e == null) {
            throw new IllegalStateException("unexpected event " + event);
        }
        List<Document> records = e.getMember("Records").asList();
        Document r = records.isEmpty() ? null : records.get(0);
        Document millis = e.getMember("MillisBehindLatest");
        record(check, records.size(), r == null ? null : r.getMember("SequenceNumber").asString(),
               r == null ? null : r.getMember("PartitionKey").asString(),
               r == null || r.getMember("ApproximateArrivalTimestamp") == null ? null
               : r.getMember("ApproximateArrivalTimestamp").asTimestamp().getEpochSecond(),
               e.getMember("ContinuationSequenceNumber").asString(), millis == null ? null : millis.asLong(),
               r == null ? ByteBuffer.allocate(0) : r.getMember("Data").asBlob());
    }
}
