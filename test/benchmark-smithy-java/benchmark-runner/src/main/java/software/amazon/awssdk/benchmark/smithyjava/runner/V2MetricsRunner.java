package software.amazon.awssdk.benchmark.smithyjava.runner;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.benchmark.smithyjava.utils.MockHttpServer;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.MetricRecord;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

/**
 * Runs V2 DynamoDB PutItem/GetItem against the local mock server with a metric publisher
 * attached, and prints aggregate per-component durations reported by the SDK's own
 * CoreMetric instrumentation (endpoint resolution, marshalling, signing, service call,
 * unmarshalling, credential fetch, ...).
 *
 * <p>Designed to be run twice: once plain (baseline timing) and once under async-profiler
 * (wall/cpu) so the SDK-reported component durations can be cross-checked against
 * profiler-attributed time.
 *
 * Usage: java -cp pipeline-benchmarks.jar ...V2MetricsRunner [iterations] [put|get|both]
 */
public final class V2MetricsRunner {

    private static final int DEFAULT_ITERATIONS = 100_000;
    private static final int WARMUP = 5_000;

    /** Aggregates every Duration-valued metric in the collection tree, keyed by metric name. */
    static final class AggregatingPublisher implements MetricPublisher {
        final Map<String, AtomicLong> totalNanos = new HashMap<>();
        final Map<String, AtomicLong> counts = new HashMap<>();

        @Override
        public void publish(MetricCollection metricCollection) {
            walk(metricCollection);
        }

        private void walk(MetricCollection collection) {
            for (MetricRecord<?> record : collection) {
                if (record.value() instanceof Duration) {
                    Duration d = (Duration) record.value();
                    String name = record.metric().name();
                    totalNanos.computeIfAbsent(name, k -> new AtomicLong()).addAndGet(d.toNanos());
                    counts.computeIfAbsent(name, k -> new AtomicLong()).incrementAndGet();
                }
            }
            for (MetricCollection child : collection.children()) {
                walk(child);
            }
        }

        void reset() {
            totalNanos.clear();
            counts.clear();
        }

        @Override
        public void close() {
        }

        void print(String label, int iterations, long wallNanos) {
            System.out.printf("%n=== SDK-reported metrics: %s (%,d ops, wall %,.0f ms, avg %,.1f us/op) ===%n",
                              label, iterations, wallNanos / 1e6, wallNanos / 1e3 / iterations);
            System.out.printf("  %-28s %12s %14s %12s %9s%n",
                              "Metric", "count", "total(ms)", "avg(us/op)", "% of wall");
            Long apiCallTotal = null;
            for (Map.Entry<String, AtomicLong> e : new TreeMap<>(totalNanos).entrySet()) {
                long nanos = e.getValue().get();
                long count = counts.get(e.getKey()).get();
                if ("ApiCallDuration".equals(e.getKey())) {
                    apiCallTotal = nanos;
                }
                System.out.printf("  %-28s %,12d %,14.1f %,12.2f %8.1f%%%n",
                                  e.getKey(), count, nanos / 1e6, (double) nanos / count / 1e3,
                                  100.0 * nanos / wallNanos);
            }
            if (apiCallTotal != null) {
                // Sum of the component metrics the SDK claims add up to ApiCallDuration.
                long components = get("CredentialsFetchDuration") + get("MarshallingDuration")
                                  + get("SigningDuration") + get("ServiceCallDuration")
                                  + get("UnmarshallingDuration") + get("BackoffDelayDuration");
                System.out.printf("  %-28s %27s %,12.2f %8.1f%% (of ApiCallDuration)%n",
                                  "[sum of documented parts]", "",
                                  (double) components / counts.get("ApiCallDuration").get() / 1e3,
                                  100.0 * components / apiCallTotal);
                System.out.printf("  %-28s %27s %,12.2f %8.1f%% (of wall)%n",
                                  "[ApiCallDuration]", "",
                                  (double) apiCallTotal / counts.get("ApiCallDuration").get() / 1e3,
                                  100.0 * apiCallTotal / wallNanos);
            }
        }

        private long get(String name) {
            AtomicLong v = totalNanos.get(name);
            return v == null ? 0 : v.get();
        }
    }

    public static void main(String[] args) throws Exception {
        int iterations = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_ITERATIONS;
        String mode = args.length > 1 ? args[1] : "both";

        System.out.println("=== V2 DynamoDB Metrics Runner ===");
        System.out.println("Iterations: " + iterations + ", mode: " + mode);

        byte[] putResponse = MockHttpServer.loadFixture("dynamodb/putitem-response.json");
        byte[] getResponse = MockHttpServer.loadFixture("dynamodb/getitem-response.json");

        AggregatingPublisher publisher = new AggregatingPublisher();

        if ("both".equals(mode) || "put".equals(mode)) {
            MockHttpServer server = new MockHttpServer(putResponse, "application/x-amz-json-1.0");
            server.start();
            DynamoDbClient client = client(server, publisher);
            for (int i = 0; i < WARMUP; i++) {
                client.putItem(putRequest());
            }
            pause("WARMUP DONE");
            publisher.reset();
            long start = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                client.putItem(putRequest());
            }
            long wall = System.nanoTime() - start;
            pause("MEASURE DONE");
            publisher.print("PutItem", iterations, wall);
            client.close();
            server.stop();
        }

        if ("both".equals(mode) || "get".equals(mode)) {
            MockHttpServer server = new MockHttpServer(getResponse, "application/x-amz-json-1.0");
            server.start();
            DynamoDbClient client = client(server, publisher);
            for (int i = 0; i < WARMUP; i++) {
                client.getItem(getRequest());
            }
            publisher.reset();
            long start = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                client.getItem(getRequest());
            }
            long wall = System.nanoTime() - start;
            publisher.print("GetItem", iterations, wall);
            client.close();
            server.stop();
        }
    }

    /**
     * If -DpauseSeconds=N is set, prints a marker with the PID and sleeps, giving an external
     * profiler (asprof) a window to attach/detach so the profile covers only the measurement loop.
     */
    private static void pause(String marker) throws InterruptedException {
        int seconds = Integer.getInteger("pauseSeconds", 0);
        System.out.printf("%s pid=%d%n", marker, ProcessHandle.current().pid());
        System.out.flush();
        if (seconds > 0) {
            Thread.sleep(seconds * 1000L);
        }
    }

    private static DynamoDbClient client(MockHttpServer server, MetricPublisher publisher) {
        return DynamoDbClient.builder()
                             .endpointOverride(server.getHttpUri())
                             .region(Region.US_EAST_1)
                             .credentialsProvider(StaticCredentialsProvider.create(
                                 AwsBasicCredentials.create("benchmarkAccessKey", "benchmarkSecretKey")))
                             .overrideConfiguration(o -> o.addMetricPublisher(publisher))
                             .build();
    }

    // Same request shapes as DynamoDbRunner so results are comparable with earlier runs.

    private static PutItemRequest putRequest() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("pk", AttributeValue.fromS("benchmark-key"));
        item.put("sk", AttributeValue.fromN("100"));
        item.put("stringField", AttributeValue.fromS("test-value"));
        item.put("numberField", AttributeValue.fromN("123.456"));
        item.put("binaryField", AttributeValue.fromB(SdkBytes.fromUtf8String("hello world")));
        item.put("stringSetField", AttributeValue.builder().ss("value1", "value2", "value3").build());
        item.put("numberSetField", AttributeValue.builder().ns("1.1", "2.2", "3.3").build());
        item.put("boolField", AttributeValue.fromBool(false));
        item.put("nullField", AttributeValue.builder().nul(true).build());
        Map<String, AttributeValue> nested = new HashMap<>();
        nested.put("nested", AttributeValue.fromS("nested-value"));
        Map<String, AttributeValue> deep = new HashMap<>();
        deep.put("level2", AttributeValue.fromN("999"));
        nested.put("deepNested", AttributeValue.fromM(deep));
        item.put("mapField", AttributeValue.fromM(nested));
        item.put("listField", AttributeValue.builder().l(
            AttributeValue.fromS("item1"),
            AttributeValue.fromN("42"),
            AttributeValue.fromBool(true),
            AttributeValue.builder().nul(true).build()).build());
        return PutItemRequest.builder().tableName("benchmark-table").item(item).build();
    }

    private static GetItemRequest getRequest() {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("pk", AttributeValue.fromS("benchmark-key"));
        return GetItemRequest.builder().tableName("benchmark-table").key(key).build();
    }
}
