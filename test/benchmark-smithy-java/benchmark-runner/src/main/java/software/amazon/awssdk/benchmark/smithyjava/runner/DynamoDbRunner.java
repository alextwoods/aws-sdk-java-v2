package software.amazon.awssdk.benchmark.smithyjava.runner;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.benchmark.smithyjava.utils.MockHttpServer;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.smithy.java.aws.client.core.settings.RegionSetting;
import software.amazon.smithy.java.client.http.smithy.SmithyHttpClientTransport;
import software.amazon.smithy.java.endpoints.Endpoint;
import software.amazon.smithy.java.http.client.HttpClient;
import software.amazon.smithy.java.http.client.connection.HttpVersionPolicy;
import software.amazon.smithy.java.retries.api.RetryStrategy;

/**
 * Standalone fixed-iteration DynamoDB benchmark runner.
 * Runs PutItem and GetItem N times for each client variant, printing timing results.
 *
 * Usage: java -cp pipeline-benchmarks.jar ...DynamoDbRunner [iterations] [operation]
 *   iterations: number of operations (default 100000)
 *   operation:  v2-sync | v2-async | smithy-java | all (default: all)
 */
public class DynamoDbRunner {

    private static final int DEFAULT_ITERATIONS = 100_000;

    public static void main(String[] args) throws Exception {
        int iterations = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_ITERATIONS;
        String mode = args.length > 1 ? args[1] : "all";

        System.out.println("=== DynamoDB Pipeline Benchmark ===");
        System.out.println("Iterations: " + iterations);
        System.out.println("Mode: " + mode);
        System.out.println();

        byte[] putResponse = MockHttpServer.loadFixture("dynamodb/putitem-response.json");
        byte[] getResponse = MockHttpServer.loadFixture("dynamodb/getitem-response.json");

        if ("all".equals(mode) || "v2-sync".equals(mode)) {
            runV2Sync(iterations, putResponse, getResponse);
        }
        if ("all".equals(mode) || "v2-async".equals(mode)) {
            runV2Async(iterations, putResponse, getResponse);
        }
        if ("all".equals(mode) || "smithy-java".equals(mode)) {
            runSmithyJava(iterations, putResponse, getResponse);
        }
    }

    // ==================== V2 Sync ====================

    private static void runV2Sync(int iterations, byte[] putResponse, byte[] getResponse) throws Exception {
        System.out.println("--- V2 Sync Client ---");

        MockHttpServer putServer = new MockHttpServer(putResponse, "application/x-amz-json-1.0");
        putServer.start();
        MockHttpServer getServer = new MockHttpServer(getResponse, "application/x-amz-json-1.0");
        getServer.start();

        DynamoDbClient putClient = DynamoDbClient.builder()
                .endpointOverride(putServer.getHttpUri())
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("benchmarkAccessKey", "benchmarkSecretKey")))
                .build();

        DynamoDbClient getClient = DynamoDbClient.builder()
                .endpointOverride(getServer.getHttpUri())
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("benchmarkAccessKey", "benchmarkSecretKey")))
                .build();

        // Warmup
        for (int i = 0; i < 1000; i++) {
            putClient.putItem(v2PutRequest());
            getClient.getItem(v2GetRequest());
        }

        // PutItem
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            putClient.putItem(v2PutRequest());
        }
        long putDuration = System.nanoTime() - start;

        // GetItem
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            getClient.getItem(v2GetRequest());
        }
        long getDuration = System.nanoTime() - start;

        printResults("V2-Sync PutItem", iterations, putDuration);
        printResults("V2-Sync GetItem", iterations, getDuration);

        putClient.close();
        getClient.close();
        putServer.stop();
        getServer.stop();
        System.out.println();
    }

    // ==================== V2 Async + CRT ====================

    private static void runV2Async(int iterations, byte[] putResponse, byte[] getResponse) throws Exception {
        System.out.println("--- V2 Async Client (CRT HTTP) ---");

        MockHttpServer putServer = new MockHttpServer(putResponse, "application/x-amz-json-1.0");
        putServer.start();
        MockHttpServer getServer = new MockHttpServer(getResponse, "application/x-amz-json-1.0");
        getServer.start();

        DynamoDbAsyncClient putClient = DynamoDbAsyncClient.builder()
                .endpointOverride(putServer.getHttpUri())
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("benchmarkAccessKey", "benchmarkSecretKey")))
                .httpClient(AwsCrtAsyncHttpClient.builder()
                        .maxConcurrency(50)
                        .build())
                .build();

        DynamoDbAsyncClient getClient = DynamoDbAsyncClient.builder()
                .endpointOverride(getServer.getHttpUri())
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("benchmarkAccessKey", "benchmarkSecretKey")))
                .httpClient(AwsCrtAsyncHttpClient.builder()
                        .maxConcurrency(50)
                        .build())
                .build();

        // Warmup
        for (int i = 0; i < 1000; i++) {
            putClient.putItem(v2PutRequest()).join();
            getClient.getItem(v2GetRequest()).join();
        }

        // PutItem (sequential await to measure per-call latency)
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            putClient.putItem(v2PutRequest()).join();
        }
        long putDuration = System.nanoTime() - start;

        // GetItem
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            getClient.getItem(v2GetRequest()).join();
        }
        long getDuration = System.nanoTime() - start;

        printResults("V2-Async PutItem", iterations, putDuration);
        printResults("V2-Async GetItem", iterations, getDuration);

        putClient.close();
        getClient.close();
        putServer.stop();
        getServer.stop();
        System.out.println();
    }

    // ==================== Smithy-Java ====================

    private static void runSmithyJava(int iterations, byte[] putResponse, byte[] getResponse) throws Exception {
        System.out.println("--- Smithy-Java Client ---");

        MockHttpServer putServer = new MockHttpServer(putResponse, "application/x-amz-json-1.0");
        putServer.start();
        MockHttpServer getServer = new MockHttpServer(getResponse, "application/x-amz-json-1.0");
        getServer.start();

        var staticCreds = software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsIdentity
                .create("benchmarkAccessKey", "benchmarkSecretKey");
        var staticResolver = (software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsResolver)
                (ctx) -> software.amazon.smithy.java.auth.api.identity.IdentityResult.of(staticCreds);

        HttpClient putHttpClient = HttpClient.builder()
                .httpVersionPolicy(HttpVersionPolicy.ENFORCE_HTTP_1_1)
                .maxConnectionsPerRoute(50)
                .maxTotalConnections(50)
                .build();

        var sjPutClient = software.amazon.awssdk.benchmark.smithyjava.dynamodb.client.DynamoDBClient.builder()
                .putConfig(RegionSetting.REGION, "us-east-1")
                .endpoint(Endpoint.builder().uri(putServer.getHttpUri().toString()).build())
                .addIdentityResolver(staticResolver)
                .retryStrategy(RetryStrategy.noRetries())
                .transport(new SmithyHttpClientTransport(putHttpClient))
                .build();

        HttpClient getHttpClient = HttpClient.builder()
                .httpVersionPolicy(HttpVersionPolicy.ENFORCE_HTTP_1_1)
                .maxConnectionsPerRoute(50)
                .maxTotalConnections(50)
                .build();

        var sjGetClient = software.amazon.awssdk.benchmark.smithyjava.dynamodb.client.DynamoDBClient.builder()
                .putConfig(RegionSetting.REGION, "us-east-1")
                .endpoint(Endpoint.builder().uri(getServer.getHttpUri().toString()).build())
                .addIdentityResolver(staticResolver)
                .retryStrategy(RetryStrategy.noRetries())
                .transport(new SmithyHttpClientTransport(getHttpClient))
                .build();

        // Warmup
        for (int i = 0; i < 1000; i++) {
            sjPutClient.putItem(sjPutRequest());
            sjGetClient.getItem(sjGetRequest());
        }

        // PutItem
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            sjPutClient.putItem(sjPutRequest());
        }
        long putDuration = System.nanoTime() - start;

        // GetItem
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            sjGetClient.getItem(sjGetRequest());
        }
        long getDuration = System.nanoTime() - start;

        printResults("SmithyJava PutItem", iterations, putDuration);
        printResults("SmithyJava GetItem", iterations, getDuration);

        ((software.amazon.smithy.java.client.core.Client) sjPutClient).close();
        ((software.amazon.smithy.java.client.core.Client) sjGetClient).close();
        putHttpClient.close();
        getHttpClient.close();
        putServer.stop();
        getServer.stop();
        System.out.println();
    }

    // ==================== Request Builders ====================

    private static PutItemRequest v2PutRequest() {
        return PutItemRequest.builder()
                .tableName("benchmark-table")
                .item(v2ItemMap())
                .build();
    }

    private static GetItemRequest v2GetRequest() {
        return GetItemRequest.builder()
                .tableName("benchmark-table")
                .key(Map.of("pk", AttributeValue.fromS("benchmark-key")))
                .build();
    }

    private static Map<String, AttributeValue> v2ItemMap() {
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
        return item;
    }

    private static software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.PutItemInput sjPutRequest() {
        return software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.PutItemInput.builder()
                .tableName("benchmark-table")
                .item(sjItemMap())
                .build();
    }

    private static software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.GetItemInput sjGetRequest() {
        return software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.GetItemInput.builder()
                .tableName("benchmark-table")
                .key(Map.of("pk",
                        software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue
                                .builder().s("benchmark-key").build()))
                .build();
    }

    private static Map<String, software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue> sjItemMap() {
        Map<String, software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue> item = new HashMap<>();
        item.put("pk", software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue.builder().s("benchmark-key").build());
        item.put("sk", software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue.builder().n("100").build());
        item.put("stringField", software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue.builder().s("test-value").build());
        item.put("numberField", software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue.builder().n("123.456").build());
        item.put("binaryField", software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue.builder().b(ByteBuffer.wrap("hello world".getBytes())).build());
        item.put("stringSetField", software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue.builder().ss(List.of("value1", "value2", "value3")).build());
        item.put("numberSetField", software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue.builder().ns(List.of("1.1", "2.2", "3.3")).build());
        item.put("boolField", software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue.builder().bool(false).build());
        item.put("nullField", software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue.builder().nullMember(true).build());
        Map<String, software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue> nested = new HashMap<>();
        nested.put("nested", software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue.builder().s("nested-value").build());
        Map<String, software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue> deep = new HashMap<>();
        deep.put("level2", software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue.builder().n("999").build());
        nested.put("deepNested", software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue.builder().m(deep).build());
        item.put("mapField", software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue.builder().m(nested).build());
        item.put("listField", software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue.builder().l(List.of(
                software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue.builder().s("item1").build(),
                software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue.builder().n("42").build(),
                software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue.builder().bool(true).build(),
                software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue.builder().nullMember(true).build()
        )).build());
        return item;
    }

    // ==================== Utilities ====================

    private static void printResults(String label, int iterations, long durationNanos) {
        double durationMs = durationNanos / 1_000_000.0;
        double opsPerSec = iterations / (durationNanos / 1_000_000_000.0);
        double avgLatencyUs = (durationNanos / 1000.0) / iterations;
        System.out.printf("  %-25s %,d ops in %,.0f ms | %,.0f ops/s | avg %,.1f us/op%n",
                label, iterations, durationMs, opsPerSec, avgLatencyUs);
    }
}
