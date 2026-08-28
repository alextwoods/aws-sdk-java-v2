package software.amazon.awssdk.benchmark.e2e;

import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.regions.Region;

import software.amazon.smithy.java.aws.client.core.settings.RegionSetting;
import software.amazon.smithy.java.client.http.smithy.SmithyHttpClientTransport;
import software.amazon.smithy.java.client.metrics.otel.OperationMetricsPlugin;
import software.amazon.smithy.java.endpoints.Endpoint;
import software.amazon.smithy.java.http.client.HttpClient;
import software.amazon.smithy.java.http.client.connection.HttpVersionPolicy;
import software.amazon.smithy.java.retries.StandardRetryStrategy;

/**
 * One workload per SDK variant: a single client pointed at the mock server, prebuilt request
 * objects, and one method per scenario.
 *
 * <p>Retry parity (fairness fix #1): every client is explicitly configured for standard,
 * token-bucket retries with a maximum of 3 attempts, so all SDKs pay comparable per-call retry
 * accounting (token acquisition/release) while none actually retries (the server always
 * returns 200):
 * <ul>
 *   <li>V2: {@code RetryMode.STANDARD} (3 attempts)</li>
 *   <li>V1: default DynamoDB retry policy capped at {@code maxErrorRetry=2} (3 attempts)</li>
 *   <li>smithy-java: {@code StandardRetryStrategy} with {@code maxAttempts(3)}</li>
 * </ul>
 */
interface Workloads {

    String ACCESS_KEY = "benchmarkAccessKey";
    String SECRET_KEY = "benchmarkSecretKey";

    interface Workload extends AutoCloseable {
        void smallGet() throws Exception;

        void smallPut() throws Exception;

        void batchGet() throws Exception;

        void batchPut() throws Exception;

        /**
         * Human-readable transport identity for the run header and the {@code transport} results
         * column. Reported because it has been a silent variable: with several {@code SdkHttpService}
         * implementations on the classpath, V2's default resolution picks by an internal priority
         * table, and this benchmark's classpath resolved to Apache5 while its README claimed Apache
         * 4.x. Every client pins its transport explicitly and records which one it used, so a results
         * file can never leave the question open.
         */
        String transport();

        /**
         * Whether {@code *Async} methods are usable. Async clients can hold many operations in
         * flight from a single thread, which is the workload shape they exist for; a blocking
         * {@code join()} per call measures something else.
         */
        default boolean supportsAsync() {
            return false;
        }

        default CompletableFuture<?> smallGetAsync() {
            throw new UnsupportedOperationException();
        }

        default CompletableFuture<?> smallPutAsync() {
            throw new UnsupportedOperationException();
        }

        default CompletableFuture<?> batchGetAsync() {
            throw new UnsupportedOperationException();
        }

        default CompletableFuture<?> batchPutAsync() {
            throw new UnsupportedOperationException();
        }

        default void resetMetrics() {
        }

        default void printMetrics(PrintStream out) {
        }
    }

    /**
     * @param concurrency operations the driver will keep in flight. Every client's connection pool is
     *                    sized to exactly this, so no client is measured waiting on its own pool and
     *                    none is given a larger pool than another.
     */
    static Workload create(String client, URI endpoint, boolean metrics, int concurrency) {
        switch (client) {
            case "v1":
                return v1(endpoint, metrics, concurrency);
            case "v2-sync":
                return v2Sync(endpoint, metrics, concurrency);
            case "v2-async":
                return v2Async(endpoint, metrics, concurrency);
            case "smithy":
                return smithy(endpoint, metrics, concurrency);
            default:
                throw new IllegalArgumentException("unknown client: " + client);
        }
    }

    // ==================== V1 ====================

    private static Workload v1(URI endpoint, boolean metrics, int concurrency) {
        MetricsSupport.V1Collector collector = new MetricsSupport.V1Collector();
        var builder = com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder.standard()
            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint.toString(), "us-east-1"))
            .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY)))
            // Default DynamoDB policy honors this cap: 2 retries = 3 attempts, matching V2 STANDARD.
            .withClientConfiguration(new ClientConfiguration().withMaxErrorRetry(2)
                                                              .withMaxConnections(concurrency));
        if (metrics) {
            builder.withMetricsCollector(collector);
        }
        var ddb = builder.build();

        var smallItem = BenchmarkItems.toV1(BenchmarkItems.smallItem());
        var getReq = new com.amazonaws.services.dynamodbv2.model.GetItemRequest()
            .withTableName(BenchmarkItems.TABLE_NAME)
            .withKey(Map.of("pk", new com.amazonaws.services.dynamodbv2.model.AttributeValue()
                .withS(BenchmarkItems.SMALL_KEY)));
        var putReq = new com.amazonaws.services.dynamodbv2.model.PutItemRequest()
            .withTableName(BenchmarkItems.TABLE_NAME).withItem(smallItem);

        List<Map<String, com.amazonaws.services.dynamodbv2.model.AttributeValue>> keys = new ArrayList<>();
        List<com.amazonaws.services.dynamodbv2.model.WriteRequest> writes = new ArrayList<>();
        for (int i = 0; i < BenchmarkItems.BATCH_SIZE; i++) {
            keys.add(Map.of("pk", new com.amazonaws.services.dynamodbv2.model.AttributeValue()
                .withS(BenchmarkItems.batchKey(i))));
            writes.add(new com.amazonaws.services.dynamodbv2.model.WriteRequest()
                           .withPutRequest(new com.amazonaws.services.dynamodbv2.model.PutRequest()
                                               .withItem(BenchmarkItems.toV1(BenchmarkItems.mediumItem(i)))));
        }
        var batchGetReq = new com.amazonaws.services.dynamodbv2.model.BatchGetItemRequest()
            .withRequestItems(Map.of(BenchmarkItems.TABLE_NAME,
                                     new com.amazonaws.services.dynamodbv2.model.KeysAndAttributes().withKeys(keys)));
        var batchPutReq = new com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest()
            .withRequestItems(Map.of(BenchmarkItems.TABLE_NAME, writes));

        return new Workload() {
            public void smallGet() {
                ddb.getItem(getReq);
            }

            public void smallPut() {
                ddb.putItem(putReq);
            }

            public void batchGet() {
                ddb.batchGetItem(batchGetReq);
            }

            public void batchPut() {
                ddb.batchWriteItem(batchPutReq);
            }

            public String transport() {
                return "apache4-v1";
            }

            public void resetMetrics() {
                collector.reset();
            }

            public void printMetrics(PrintStream out) {
                collector.print(out);
            }

            public void close() {
                ddb.shutdown();
            }
        };
    }

    // ==================== V2 shared ====================

    private static StaticCredentialsProvider v2Creds() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY));
    }

    private static ClientOverrideConfiguration v2Override(boolean metrics, MetricsSupport.V2Publisher publisher) {
        ClientOverrideConfiguration.Builder b = ClientOverrideConfiguration.builder()
                                                                           .retryStrategy(RetryMode.STANDARD);
        if (metrics) {
            b.addMetricPublisher(publisher);
        }
        return b.build();
    }

    private static software.amazon.awssdk.services.dynamodb.model.GetItemRequest v2GetReq() {
        return software.amazon.awssdk.services.dynamodb.model.GetItemRequest.builder()
            .tableName(BenchmarkItems.TABLE_NAME)
            .key(Map.of("pk", software.amazon.awssdk.services.dynamodb.model.AttributeValue
                .fromS(BenchmarkItems.SMALL_KEY)))
            .build();
    }

    private static software.amazon.awssdk.services.dynamodb.model.PutItemRequest v2PutReq() {
        return software.amazon.awssdk.services.dynamodb.model.PutItemRequest.builder()
            .tableName(BenchmarkItems.TABLE_NAME)
            .item(BenchmarkItems.toV2(BenchmarkItems.smallItem()))
            .build();
    }

    private static software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest v2BatchGetReq() {
        List<Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue>> keys = new ArrayList<>();
        for (int i = 0; i < BenchmarkItems.BATCH_SIZE; i++) {
            keys.add(Map.of("pk", software.amazon.awssdk.services.dynamodb.model.AttributeValue
                .fromS(BenchmarkItems.batchKey(i))));
        }
        return software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest.builder()
            .requestItems(Map.of(BenchmarkItems.TABLE_NAME,
                                 software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes.builder()
                                                                                                 .keys(keys).build()))
            .build();
    }

    private static software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest v2BatchPutReq() {
        List<software.amazon.awssdk.services.dynamodb.model.WriteRequest> writes = new ArrayList<>();
        for (int i = 0; i < BenchmarkItems.BATCH_SIZE; i++) {
            writes.add(software.amazon.awssdk.services.dynamodb.model.WriteRequest.builder()
                .putRequest(software.amazon.awssdk.services.dynamodb.model.PutRequest.builder()
                                .item(BenchmarkItems.toV2(BenchmarkItems.mediumItem(i))).build())
                .build());
        }
        return software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest.builder()
            .requestItems(Map.of(BenchmarkItems.TABLE_NAME, writes))
            .build();
    }

    // ==================== V2 sync (Apache5) ====================
    //
    // Apache5 is the transport V2 sync is standardizing on, so it is the one worth measuring. It is
    // pinned rather than resolved from the classpath: `dynamodb` pulls in apache5-client and
    // netty-nio-client transitively, so several SdkHttpService implementations can be present at
    // once and V2 picks by an internal priority table (ClasspathSdkHttpServiceProvider) instead of
    // failing. That made the transport an invisible variable — this module's README claimed Apache
    // 4.x for `v2-sync` while Apache5 was in fact what ran. Naming it here means the priority table,
    // and anything new that lands on the classpath, cannot change what is under test.

    private static Workload v2Sync(URI endpoint, boolean metrics, int concurrency) {
        MetricsSupport.V2Publisher publisher = new MetricsSupport.V2Publisher();
        var ddb = software.amazon.awssdk.services.dynamodb.DynamoDbClient.builder()
            .endpointOverride(endpoint).region(Region.US_EAST_1).credentialsProvider(v2Creds())
            .httpClientBuilder(software.amazon.awssdk.http.apache5.Apache5HttpClient.builder()
                                                                                   .maxConnections(concurrency))
            .overrideConfiguration(v2Override(metrics, publisher))
            .build();

        var getReq = v2GetReq();
        var putReq = v2PutReq();
        var batchGetReq = v2BatchGetReq();
        var batchPutReq = v2BatchPutReq();

        return new Workload() {
            public void smallGet() {
                ddb.getItem(getReq);
            }

            public void smallPut() {
                ddb.putItem(putReq);
            }

            public void batchGet() {
                ddb.batchGetItem(batchGetReq);
            }

            public void batchPut() {
                ddb.batchWriteItem(batchPutReq);
            }

            public String transport() {
                return "apache5";
            }

            public void resetMetrics() {
                publisher.reset();
            }

            public void printMetrics(PrintStream out) {
                publisher.print(out);
            }

            public void close() {
                ddb.close();
            }
        };
    }

    // ==================== V2 async (CRT) ====================
    //
    // CRT is the transport V2 async is standardizing on, so it is the one worth measuring, and it is
    // pinned for the same reason as the sync side. Note that V2's *current* default resolution would
    // pick Netty (priority 1 in ClasspathSdkHttpServiceProvider's async table), so this benchmark is
    // deliberately measuring the intended long-term default rather than today's fallback.

    private static Workload v2Async(URI endpoint, boolean metrics, int concurrency) {
        MetricsSupport.V2Publisher publisher = new MetricsSupport.V2Publisher();
        var ddb = software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient.builder()
            .endpointOverride(endpoint).region(Region.US_EAST_1).credentialsProvider(v2Creds())
            .httpClient(AwsCrtAsyncHttpClient.builder().maxConcurrency(concurrency).build())
            .overrideConfiguration(v2Override(metrics, publisher))
            .build();

        var getReq = v2GetReq();
        var putReq = v2PutReq();
        var batchGetReq = v2BatchGetReq();
        var batchPutReq = v2BatchPutReq();

        return new Workload() {
            public void smallGet() {
                ddb.getItem(getReq).join();
            }

            public void smallPut() {
                ddb.putItem(putReq).join();
            }

            public void batchGet() {
                ddb.batchGetItem(batchGetReq).join();
            }

            public void batchPut() {
                ddb.batchWriteItem(batchPutReq).join();
            }

            public boolean supportsAsync() {
                return true;
            }

            public CompletableFuture<?> smallGetAsync() {
                return ddb.getItem(getReq);
            }

            public CompletableFuture<?> smallPutAsync() {
                return ddb.putItem(putReq);
            }

            public CompletableFuture<?> batchGetAsync() {
                return ddb.batchGetItem(batchGetReq);
            }

            public CompletableFuture<?> batchPutAsync() {
                return ddb.batchWriteItem(batchPutReq);
            }

            public String transport() {
                return "crt";
            }

            public void resetMetrics() {
                publisher.reset();
            }

            public void printMetrics(PrintStream out) {
                publisher.print(out);
            }

            public void close() {
                ddb.close();
            }
        };
    }

    // ==================== smithy-java ====================

    private static Workload smithy(URI endpoint, boolean metrics, int concurrency) {
        var staticCreds = software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsIdentity
            .create(ACCESS_KEY, SECRET_KEY);
        var resolver = (software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsResolver)
            ctx -> software.amazon.smithy.java.auth.api.identity.IdentityResult.of(staticCreds);

        MetricsSupport.OtelHolder otel = new MetricsSupport.OtelHolder();
        HttpClient http = HttpClient.builder().httpVersionPolicy(HttpVersionPolicy.ENFORCE_HTTP_1_1)
                                    .maxConnectionsPerRoute(concurrency)
                                    .maxTotalConnections(concurrency).build();

        var builder = software.amazon.awssdk.benchmark.smithyjava.dynamodb.client.DynamoDBClient.builder()
            .putConfig(RegionSetting.REGION, "us-east-1")
            .endpoint(Endpoint.builder().uri(endpoint.toString()).build())
            .addIdentityResolver(resolver)
            // Standard token-bucket retries, 3 attempts: equivalent to V2 RetryMode.STANDARD.
            .retryStrategy(StandardRetryStrategy.builder().maxAttempts(3).build())
            .transport(new SmithyHttpClientTransport(http));
        if (metrics) {
            builder.addPlugin(new OperationMetricsPlugin(otel.otel));
        }
        var ddb = builder.build();

        var getReq = software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.GetItemInput.builder()
            .tableName(BenchmarkItems.TABLE_NAME)
            .key(Map.of("pk", software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue
                .builder().s(BenchmarkItems.SMALL_KEY).build()))
            .build();
        var putReq = software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.PutItemInput.builder()
            .tableName(BenchmarkItems.TABLE_NAME)
            .item(BenchmarkItems.toSj(BenchmarkItems.smallItem()))
            .build();

        List<Map<String, software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue>> keys =
            new ArrayList<>();
        List<software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.WriteRequest> writes = new ArrayList<>();
        for (int i = 0; i < BenchmarkItems.BATCH_SIZE; i++) {
            keys.add(Map.of("pk", software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue
                .builder().s(BenchmarkItems.batchKey(i)).build()));
            writes.add(software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.WriteRequest.builder()
                .putRequest(software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.PutRequest.builder()
                                .item(BenchmarkItems.toSj(BenchmarkItems.mediumItem(i))).build())
                .build());
        }
        var batchGetReq = software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.BatchGetItemInput.builder()
            .requestItems(Map.of(BenchmarkItems.TABLE_NAME,
                                 software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.KeysAndAttributes
                                     .builder().keys(keys).build()))
            .build();
        Map<String, List<software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.WriteRequest>> writeMap =
            new LinkedHashMap<>();
        writeMap.put(BenchmarkItems.TABLE_NAME, writes);
        var batchPutReq = software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.BatchWriteItemInput.builder()
            .requestItems(writeMap)
            .build();

        return new Workload() {
            public void smallGet() {
                ddb.getItem(getReq);
            }

            public void smallPut() {
                ddb.putItem(putReq);
            }

            public void batchGet() {
                ddb.batchGetItem(batchGetReq);
            }

            public void batchPut() {
                ddb.batchWriteItem(batchPutReq);
            }

            public String transport() {
                return "smithy-http1";
            }

            public void resetMetrics() {
                otel.reset();
            }

            public void printMetrics(PrintStream out) {
                otel.print(out);
            }

            public void close() throws Exception {
                ((software.amazon.smithy.java.client.core.Client) ddb).close();
                http.close();
            }
        };
    }
}
