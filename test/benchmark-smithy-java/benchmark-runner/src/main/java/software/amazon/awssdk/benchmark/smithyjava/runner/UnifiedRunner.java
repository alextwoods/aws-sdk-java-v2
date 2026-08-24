package software.amazon.awssdk.benchmark.smithyjava.runner;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.metrics.RequestMetricCollector;
import com.amazonaws.util.AWSRequestMetrics;
import com.amazonaws.util.TimingInfo;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.MetricRecord;
import software.amazon.awssdk.regions.Region;

import software.amazon.smithy.java.aws.client.core.settings.RegionSetting;
import software.amazon.smithy.java.client.http.smithy.SmithyHttpClientTransport;
import software.amazon.smithy.java.client.metrics.otel.OperationMetricsPlugin;
import software.amazon.smithy.java.endpoints.Endpoint;
import software.amazon.smithy.java.http.client.HttpClient;
import software.amazon.smithy.java.http.client.connection.HttpVersionPolicy;
import software.amazon.smithy.java.retries.api.RetryStrategy;

/**
 * Unified fixed-iteration pipeline benchmark runner for all four SDKs, talking to the
 * out-of-process {@link StandaloneMockServer}.
 *
 * <p>Phases: warmup(put+get) -> [pause] -> put loop -> [pause] -> get loop -> [pause] -> report.
 * The pauses print a marker with the PID and sleep {@code -DpauseSeconds=N} seconds so an
 * external profiler (asprof) can attach/detach per phase, producing clean per-phase profiles.
 *
 * <p>Metric modes:
 * <ul>
 *   <li>v2-sync / v2-async: CoreMetric durations via a {@link MetricPublisher}</li>
 *   <li>v1: {@link AWSRequestMetrics} sub-measurements via a {@link RequestMetricCollector}</li>
 *   <li>smithy: smithy.client.call.* histograms via {@link OperationMetricsPlugin} + in-memory OTel reader</li>
 * </ul>
 *
 * Usage: UnifiedRunner &lt;v1|v2-sync|v2-async|smithy&gt; &lt;dynamodb|s3|cloudwatch&gt;
 *        &lt;iterations&gt; &lt;metrics:on|off&gt; [basePort]
 */
public final class UnifiedRunner {

    private static final String ACCESS_KEY = "benchmarkAccessKey";
    private static final String SECRET_KEY = "benchmarkSecretKey";
    private static final byte[] S3_PAYLOAD = StandaloneMockServer.payload();

    /** One put-op and one get-op against the external mock server, plus per-phase metric reporting. */
    interface Workload extends AutoCloseable {
        void put() throws Exception;

        void get() throws Exception;

        default void resetMetrics() {
        }

        default void printPutMetrics() {
        }

        default void printGetMetrics() {
        }
    }

    /** Item size for DynamoDB workloads: SMALL (12 attrs), MEDIUM (~2 KB), LARGE (~19 KB). */
    private static String ddbSize = "SMALL";
    private static Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> v2Item;
    private static Map<String, com.amazonaws.services.dynamodbv2.model.AttributeValue> v1Item;
    private static Map<String, software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue> sjItem;

    public static void main(String[] args) throws Exception {
        String sdk = args[0];
        String service = args[1];
        int iterations = Integer.parseInt(args[2]);
        boolean metrics = "on".equals(args[3]);
        int basePort = args.length > 4 ? Integer.parseInt(args[4]) : StandaloneMockServer.DEFAULT_BASE_PORT;

        // "dynamodb-medium" / "dynamodb-large" select bigger items (nested structures, mixed
        // types, from the shared DdbItems spec) and the matching size-specific GetItem endpoint.
        if (service.startsWith("dynamodb-")) {
            ddbSize = service.substring("dynamodb-".length()).toUpperCase();
            service = "dynamodb";
        }
        Map<String, software.amazon.awssdk.benchmark.smithyjava.serde.DdbItems.Attr> attrs =
            software.amazon.awssdk.benchmark.smithyjava.serde.DdbItems.forSize(ddbSize);
        v2Item = software.amazon.awssdk.benchmark.smithyjava.serde.DdbItems.toV2(attrs);
        v1Item = software.amazon.awssdk.benchmark.smithyjava.serde.DdbItems.toV1(attrs);
        sjItem = software.amazon.awssdk.benchmark.smithyjava.serde.DdbItems.toSj(attrs);

        System.out.printf("=== UnifiedRunner sdk=%s service=%s ddbSize=%s iterations=%d metrics=%s basePort=%d ===%n",
                          sdk, service, ddbSize, iterations, metrics ? "on" : "off", basePort);

        if ("smithy".equals(sdk) && "cloudwatch".equals(service)) {
            System.out.println("SKIP: smithy-java awsQuery leaks pooled connections for Unit-output operations "
                               + "(PutMetricData); see verification-and-synthesis.md section 5.");
            return;
        }

        int warmup = "s3".equals(service) ? 200 : 2000;

        try (Workload w = createWorkload(sdk, service, metrics, basePort)) {
            for (int i = 0; i < warmup; i++) {
                w.put();
                w.get();
            }
            w.resetMetrics();
            pause("WARMUP DONE");

            long start = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                w.put();
            }
            long putNanos = System.nanoTime() - start;
            pause("PUT DONE");

            start = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                w.get();
            }
            long getNanos = System.nanoTime() - start;
            pause("GET DONE");

            printResults(sdk + " " + service + " put", iterations, putNanos);
            printResults(sdk + " " + service + " get", iterations, getNanos);
            if (metrics) {
                System.out.println("--- put-phase metrics ---");
                w.printPutMetrics();
                System.out.println("--- get-phase metrics ---");
                w.printGetMetrics();
            }
        }
    }

    private static Workload createWorkload(String sdk, String service, boolean metrics, int basePort)
            throws Exception {
        URI putUri;
        URI getUri;
        switch (service) {
            case "dynamodb":
                putUri = uri(basePort);
                getUri = uri(basePort + ("MEDIUM".equals(ddbSize) ? 8 : "LARGE".equals(ddbSize) ? 9 : 1));
                break;
            case "s3":
                putUri = uri(basePort + 2);
                getUri = uri(basePort + 3);
                break;
            case "cloudwatch":
                // V1 1.12.797 and V2 >= 2.54 speak RPCv2-CBOR to CloudWatch (the protocol was
                // migrated from awsQuery in Dec 2025); smithy-java 1.5.1's model is still awsQuery.
                if ("v1".equals(sdk) || sdk.startsWith("v2")) {
                    putUri = uri(basePort + 6);
                    getUri = uri(basePort + 7);
                } else {
                    putUri = uri(basePort + 4);
                    getUri = uri(basePort + 5);
                }
                break;
            default:
                throw new IllegalArgumentException("unknown service " + service);
        }
        switch (sdk) {
            case "v2-sync":
                return v2Sync(service, metrics, putUri, getUri);
            case "v2-async":
                return v2Async(service, metrics, putUri, getUri);
            case "v1":
                return v1(service, metrics, putUri, getUri);
            case "smithy":
                return smithy(service, metrics, putUri, getUri);
            default:
                throw new IllegalArgumentException("unknown sdk " + sdk);
        }
    }

    private static URI uri(int port) {
        return URI.create("http://localhost:" + port);
    }

    private static void pause(String marker) throws InterruptedException {
        int seconds = Integer.getInteger("pauseSeconds", 0);
        System.out.printf("%s pid=%d%n", marker, ProcessHandle.current().pid());
        System.out.flush();
        if (seconds > 0) {
            Thread.sleep(seconds * 1000L);
        }
    }

    private static void printResults(String label, int iterations, long nanos) {
        System.out.printf("RESULT %-28s %,d ops in %,.0f ms | %,.0f ops/s | avg %,.1f us/op%n",
                          label, iterations, nanos / 1e6, iterations / (nanos / 1e9), nanos / 1e3 / iterations);
    }

    // ==================== V2 sync ====================

    private static Workload v2Sync(String service, boolean metrics, URI putUri, URI getUri) {
        V2Publisher putPub = new V2Publisher();
        V2Publisher getPub = new V2Publisher();
        switch (service) {
            case "dynamodb": {
                var put = software.amazon.awssdk.services.dynamodb.DynamoDbClient.builder()
                    .endpointOverride(putUri).region(Region.US_EAST_1).credentialsProvider(v2Creds())
                    .overrideConfiguration(v2Override(metrics, putPub)).build();
                var get = software.amazon.awssdk.services.dynamodb.DynamoDbClient.builder()
                    .endpointOverride(getUri).region(Region.US_EAST_1).credentialsProvider(v2Creds())
                    .overrideConfiguration(v2Override(metrics, getPub)).build();
                return new Workload() {
                    public void put() {
                        put.putItem(v2DdbPut());
                    }

                    public void get() {
                        get.getItem(v2DdbGet());
                    }

                    public void resetMetrics() {
                        putPub.reset();
                        getPub.reset();
                    }

                    public void printPutMetrics() {
                        putPub.print();
                    }

                    public void printGetMetrics() {
                        getPub.print();
                    }

                    public void close() {
                        put.close();
                        get.close();
                    }
                };
            }
            case "s3": {
                var put = software.amazon.awssdk.services.s3.S3Client.builder()
                    .endpointOverride(putUri).region(Region.US_EAST_1).credentialsProvider(v2Creds())
                    .serviceConfiguration(c -> c.pathStyleAccessEnabled(true))
                    .overrideConfiguration(v2Override(metrics, putPub)).build();
                var get = software.amazon.awssdk.services.s3.S3Client.builder()
                    .endpointOverride(getUri).region(Region.US_EAST_1).credentialsProvider(v2Creds())
                    .serviceConfiguration(c -> c.pathStyleAccessEnabled(true))
                    .overrideConfiguration(v2Override(metrics, getPub)).build();
                return new Workload() {
                    public void put() {
                        put.putObject(software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                                          .bucket("b").key("k").build(), RequestBody.fromBytes(S3_PAYLOAD));
                    }

                    public void get() throws Exception {
                        try (var resp = get.getObject(
                            software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                                .bucket("b").key("k").build())) {
                            resp.readAllBytes();
                        }
                    }

                    public void resetMetrics() {
                        putPub.reset();
                        getPub.reset();
                    }

                    public void printPutMetrics() {
                        putPub.print();
                    }

                    public void printGetMetrics() {
                        getPub.print();
                    }

                    public void close() {
                        put.close();
                        get.close();
                    }
                };
            }
            case "cloudwatch": {
                var put = software.amazon.awssdk.services.cloudwatch.CloudWatchClient.builder()
                    .endpointOverride(putUri).region(Region.US_EAST_1).credentialsProvider(v2Creds())
                    .overrideConfiguration(v2Override(metrics, putPub)).build();
                var get = software.amazon.awssdk.services.cloudwatch.CloudWatchClient.builder()
                    .endpointOverride(getUri).region(Region.US_EAST_1).credentialsProvider(v2Creds())
                    .overrideConfiguration(v2Override(metrics, getPub)).build();
                return new Workload() {
                    public void put() {
                        put.putMetricData(v2CwPut());
                    }

                    public void get() {
                        get.getMetricData(v2CwGet());
                    }

                    public void resetMetrics() {
                        putPub.reset();
                        getPub.reset();
                    }

                    public void printPutMetrics() {
                        putPub.print();
                    }

                    public void printGetMetrics() {
                        getPub.print();
                    }

                    public void close() {
                        put.close();
                        get.close();
                    }
                };
            }
            default:
                throw new IllegalArgumentException(service);
        }
    }

    // ==================== V2 async (CRT) ====================

    private static Workload v2Async(String service, boolean metrics, URI putUri, URI getUri) {
        V2Publisher putPub = new V2Publisher();
        V2Publisher getPub = new V2Publisher();
        switch (service) {
            case "dynamodb": {
                var put = software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient.builder()
                    .endpointOverride(putUri).region(Region.US_EAST_1).credentialsProvider(v2Creds())
                    .httpClient(AwsCrtAsyncHttpClient.builder().maxConcurrency(50).build())
                    .overrideConfiguration(v2Override(metrics, putPub)).build();
                var get = software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient.builder()
                    .endpointOverride(getUri).region(Region.US_EAST_1).credentialsProvider(v2Creds())
                    .httpClient(AwsCrtAsyncHttpClient.builder().maxConcurrency(50).build())
                    .overrideConfiguration(v2Override(metrics, getPub)).build();
                return new Workload() {
                    public void put() {
                        put.putItem(v2DdbPut()).join();
                    }

                    public void get() {
                        get.getItem(v2DdbGet()).join();
                    }

                    public void resetMetrics() {
                        putPub.reset();
                        getPub.reset();
                    }

                    public void printPutMetrics() {
                        putPub.print();
                    }

                    public void printGetMetrics() {
                        getPub.print();
                    }

                    public void close() {
                        put.close();
                        get.close();
                    }
                };
            }
            case "s3": {
                var put = software.amazon.awssdk.services.s3.S3AsyncClient.builder()
                    .endpointOverride(putUri).region(Region.US_EAST_1).credentialsProvider(v2Creds())
                    .serviceConfiguration(c -> c.pathStyleAccessEnabled(true))
                    .httpClient(AwsCrtAsyncHttpClient.builder().maxConcurrency(50).build())
                    .overrideConfiguration(v2Override(metrics, putPub)).build();
                var get = software.amazon.awssdk.services.s3.S3AsyncClient.builder()
                    .endpointOverride(getUri).region(Region.US_EAST_1).credentialsProvider(v2Creds())
                    .serviceConfiguration(c -> c.pathStyleAccessEnabled(true))
                    .httpClient(AwsCrtAsyncHttpClient.builder().maxConcurrency(50).build())
                    .overrideConfiguration(v2Override(metrics, getPub)).build();
                return new Workload() {
                    public void put() {
                        put.putObject(software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                                          .bucket("b").key("k").build(),
                                      AsyncRequestBody.fromBytes(S3_PAYLOAD)).join();
                    }

                    public void get() {
                        get.getObject(software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                                          .bucket("b").key("k").build(),
                                      AsyncResponseTransformer.toBytes()).join();
                    }

                    public void resetMetrics() {
                        putPub.reset();
                        getPub.reset();
                    }

                    public void printPutMetrics() {
                        putPub.print();
                    }

                    public void printGetMetrics() {
                        getPub.print();
                    }

                    public void close() {
                        put.close();
                        get.close();
                    }
                };
            }
            case "cloudwatch": {
                var put = software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient.builder()
                    .endpointOverride(putUri).region(Region.US_EAST_1).credentialsProvider(v2Creds())
                    .httpClient(AwsCrtAsyncHttpClient.builder().maxConcurrency(50).build())
                    .overrideConfiguration(v2Override(metrics, putPub)).build();
                var get = software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient.builder()
                    .endpointOverride(getUri).region(Region.US_EAST_1).credentialsProvider(v2Creds())
                    .httpClient(AwsCrtAsyncHttpClient.builder().maxConcurrency(50).build())
                    .overrideConfiguration(v2Override(metrics, getPub)).build();
                return new Workload() {
                    public void put() {
                        put.putMetricData(v2CwPut()).join();
                    }

                    public void get() {
                        get.getMetricData(v2CwGet()).join();
                    }

                    public void resetMetrics() {
                        putPub.reset();
                        getPub.reset();
                    }

                    public void printPutMetrics() {
                        putPub.print();
                    }

                    public void printGetMetrics() {
                        getPub.print();
                    }

                    public void close() {
                        put.close();
                        get.close();
                    }
                };
            }
            default:
                throw new IllegalArgumentException(service);
        }
    }

    private static StaticCredentialsProvider v2Creds() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY));
    }

    private static ClientOverrideConfiguration v2Override(boolean metrics, V2Publisher publisher) {
        ClientOverrideConfiguration.Builder b = ClientOverrideConfiguration.builder();
        if (metrics) {
            b.addMetricPublisher(publisher);
        }
        return b.build();
    }

    // ==================== V1 ====================

    private static Workload v1(String service, boolean metrics, URI putUri, URI getUri) {
        V1Collector putCol = new V1Collector();
        V1Collector getCol = new V1Collector();
        var creds = new AWSStaticCredentialsProvider(new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY));
        switch (service) {
            case "dynamodb": {
                var putB = com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(putUri.toString(), "us-east-1"))
                    .withCredentials(creds);
                var getB = com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(getUri.toString(), "us-east-1"))
                    .withCredentials(creds);
                if (metrics) {
                    putB.withMetricsCollector(putCol);
                    getB.withMetricsCollector(getCol);
                }
                var put = putB.build();
                var get = getB.build();
                return new Workload() {
                    public void put() {
                        put.putItem(v1DdbPut());
                    }

                    public void get() {
                        get.getItem(v1DdbGet());
                    }

                    public void resetMetrics() {
                        putCol.reset();
                        getCol.reset();
                    }

                    public void printPutMetrics() {
                        putCol.print();
                    }

                    public void printGetMetrics() {
                        getCol.print();
                    }

                    public void close() {
                        put.shutdown();
                        get.shutdown();
                    }
                };
            }
            case "s3": {
                var putB = com.amazonaws.services.s3.AmazonS3ClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(putUri.toString(), "us-east-1"))
                    .withCredentials(creds).enablePathStyleAccess();
                var getB = com.amazonaws.services.s3.AmazonS3ClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(getUri.toString(), "us-east-1"))
                    .withCredentials(creds).enablePathStyleAccess();
                if (metrics) {
                    putB.withMetricsCollector(putCol);
                    getB.withMetricsCollector(getCol);
                }
                var put = putB.build();
                var get = getB.build();
                return new Workload() {
                    public void put() {
                        var meta = new com.amazonaws.services.s3.model.ObjectMetadata();
                        meta.setContentLength(S3_PAYLOAD.length);
                        put.putObject("b", "k", new ByteArrayInputStream(S3_PAYLOAD), meta);
                    }

                    public void get() throws Exception {
                        try (var obj = get.getObject("b", "k")) {
                            obj.getObjectContent().readAllBytes();
                        }
                    }

                    public void resetMetrics() {
                        putCol.reset();
                        getCol.reset();
                    }

                    public void printPutMetrics() {
                        putCol.print();
                    }

                    public void printGetMetrics() {
                        getCol.print();
                    }

                    public void close() {
                        put.shutdown();
                        get.shutdown();
                    }
                };
            }
            case "cloudwatch": {
                var putB = com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(putUri.toString(), "us-east-1"))
                    .withCredentials(creds);
                var getB = com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(getUri.toString(), "us-east-1"))
                    .withCredentials(creds);
                if (metrics) {
                    putB.withMetricsCollector(putCol);
                    getB.withMetricsCollector(getCol);
                }
                var put = putB.build();
                var get = getB.build();
                return new Workload() {
                    public void put() {
                        put.putMetricData(v1CwPut());
                    }

                    public void get() {
                        get.getMetricData(v1CwGet());
                    }

                    public void resetMetrics() {
                        putCol.reset();
                        getCol.reset();
                    }

                    public void printPutMetrics() {
                        putCol.print();
                    }

                    public void printGetMetrics() {
                        getCol.print();
                    }

                    public void close() {
                        put.shutdown();
                        get.shutdown();
                    }
                };
            }
            default:
                throw new IllegalArgumentException(service);
        }
    }

    // ==================== Smithy-Java ====================

    private static Workload smithy(String service, boolean metrics, URI putUri, URI getUri) {
        var staticCreds = software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsIdentity
            .create(ACCESS_KEY, SECRET_KEY);
        var resolver = (software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsResolver)
            ctx -> software.amazon.smithy.java.auth.api.identity.IdentityResult.of(staticCreds);

        OtelHolder putOtel = new OtelHolder();
        OtelHolder getOtel = new OtelHolder();
        HttpClient putHttp = HttpClient.builder().httpVersionPolicy(HttpVersionPolicy.ENFORCE_HTTP_1_1)
                                       .maxConnectionsPerRoute(50).maxTotalConnections(50).build();
        HttpClient getHttp = HttpClient.builder().httpVersionPolicy(HttpVersionPolicy.ENFORCE_HTTP_1_1)
                                       .maxConnectionsPerRoute(50).maxTotalConnections(50).build();

        switch (service) {
            case "dynamodb": {
                var putB = software.amazon.awssdk.benchmark.smithyjava.dynamodb.client.DynamoDBClient.builder()
                    .putConfig(RegionSetting.REGION, "us-east-1")
                    .endpoint(Endpoint.builder().uri(putUri.toString()).build())
                    .addIdentityResolver(resolver)
                    .retryStrategy(RetryStrategy.noRetries())
                    .transport(new SmithyHttpClientTransport(putHttp));
                var getB = software.amazon.awssdk.benchmark.smithyjava.dynamodb.client.DynamoDBClient.builder()
                    .putConfig(RegionSetting.REGION, "us-east-1")
                    .endpoint(Endpoint.builder().uri(getUri.toString()).build())
                    .addIdentityResolver(resolver)
                    .retryStrategy(RetryStrategy.noRetries())
                    .transport(new SmithyHttpClientTransport(getHttp));
                if (metrics) {
                    putB.addPlugin(new OperationMetricsPlugin(putOtel.otel));
                    getB.addPlugin(new OperationMetricsPlugin(getOtel.otel));
                }
                var put = putB.build();
                var get = getB.build();
                return new Workload() {
                    public void put() {
                        put.putItem(sjDdbPut());
                    }

                    public void get() {
                        get.getItem(sjDdbGet());
                    }

                    public void resetMetrics() {
                        putOtel.reset();
                        getOtel.reset();
                    }

                    public void printPutMetrics() {
                        putOtel.print();
                    }

                    public void printGetMetrics() {
                        getOtel.print();
                    }

                    public void close() throws Exception {
                        ((software.amazon.smithy.java.client.core.Client) put).close();
                        ((software.amazon.smithy.java.client.core.Client) get).close();
                        putHttp.close();
                        getHttp.close();
                    }
                };
            }
            case "s3": {
                var putB = software.amazon.awssdk.benchmark.smithyjava.s3.client.S3Client.builder()
                    .putConfig(RegionSetting.REGION, "us-east-1")
                    .endpoint(Endpoint.builder().uri(putUri.toString()).build())
                    .addIdentityResolver(resolver)
                    .retryStrategy(RetryStrategy.noRetries())
                    .transport(new SmithyHttpClientTransport(putHttp));
                var getB = software.amazon.awssdk.benchmark.smithyjava.s3.client.S3Client.builder()
                    .putConfig(RegionSetting.REGION, "us-east-1")
                    .endpoint(Endpoint.builder().uri(getUri.toString()).build())
                    .addIdentityResolver(resolver)
                    .retryStrategy(RetryStrategy.noRetries())
                    .transport(new SmithyHttpClientTransport(getHttp));
                if (metrics) {
                    putB.addPlugin(new OperationMetricsPlugin(putOtel.otel));
                    getB.addPlugin(new OperationMetricsPlugin(getOtel.otel));
                }
                var put = putB.build();
                var get = getB.build();
                return new Workload() {
                    public void put() {
                        put.putObject(software.amazon.awssdk.benchmark.smithyjava.s3.model.PutObjectInput.builder()
                                          .bucket("b").key("k")
                                          .contentLength((long) S3_PAYLOAD.length)
                                          .body(software.amazon.smithy.java.io.datastream.DataStream.ofBytes(S3_PAYLOAD))
                                          .build());
                    }

                    public void get() throws Exception {
                        var out = get.getObject(
                            software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObjectInput.builder()
                                .bucket("b").key("k").build());
                        out.getBody().asInputStream().readAllBytes();
                    }

                    public void resetMetrics() {
                        putOtel.reset();
                        getOtel.reset();
                    }

                    public void printPutMetrics() {
                        putOtel.print();
                    }

                    public void printGetMetrics() {
                        getOtel.print();
                    }

                    public void close() throws Exception {
                        ((software.amazon.smithy.java.client.core.Client) put).close();
                        ((software.amazon.smithy.java.client.core.Client) get).close();
                        putHttp.close();
                        getHttp.close();
                    }
                };
            }
            default:
                throw new IllegalArgumentException(service);
        }
    }

    // ==================== Metric aggregators ====================

    /** Aggregates every Duration-valued V2 metric in the collection tree, keyed by metric name. */
    static final class V2Publisher implements MetricPublisher {
        final Map<String, AtomicLong> totalNanos = new HashMap<>();
        final Map<String, AtomicLong> counts = new HashMap<>();

        @Override
        public void publish(MetricCollection collection) {
            walk(collection);
        }

        private void walk(MetricCollection collection) {
            for (MetricRecord<?> record : collection) {
                if (record.value() instanceof Duration) {
                    String name = record.metric().name();
                    totalNanos.computeIfAbsent(name, k -> new AtomicLong())
                              .addAndGet(((Duration) record.value()).toNanos());
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

        void print() {
            for (Map.Entry<String, AtomicLong> e : new TreeMap<>(totalNanos).entrySet()) {
                long count = counts.get(e.getKey()).get();
                System.out.printf("METRIC %-38s count=%,d totalMs=%,.1f avgUs=%,.2f%n",
                                  e.getKey(), count, e.getValue().get() / 1e6,
                                  (double) e.getValue().get() / count / 1e3);
            }
        }

        @Override
        public void close() {
        }
    }

    /** Aggregates V1 AWSRequestMetrics timing sub-measurements. */
    static final class V1Collector extends RequestMetricCollector {
        private static final String[] FIELDS = {
            "ClientExecuteTime", "CredentialsRequestTime", "RequestMarshallTime", "RequestSigningTime",
            "HttpRequestTime", "HttpClientSendRequestTime", "HttpClientReceiveResponseTime",
            "ResponseProcessingTime", "RetryPauseTime", "HttpClientPoolAcquireTime",
        };
        final Map<String, AtomicLong> totalNanos = new HashMap<>();
        final Map<String, AtomicLong> counts = new HashMap<>();

        @Override
        public void collectMetrics(com.amazonaws.Request<?> request, com.amazonaws.Response<?> response) {
            AWSRequestMetrics metrics = request.getAWSRequestMetrics();
            if (metrics == null) {
                return;
            }
            TimingInfo root = metrics.getTimingInfo();
            for (String field : FIELDS) {
                TimingInfo sub = root.getSubMeasurement(field);
                if (sub != null) {
                    Double ms = sub.getTimeTakenMillisIfKnown();
                    if (ms != null) {
                        totalNanos.computeIfAbsent(field, k -> new AtomicLong())
                                  .addAndGet((long) (ms * 1_000_000));
                        counts.computeIfAbsent(field, k -> new AtomicLong()).incrementAndGet();
                    }
                }
            }
        }

        void reset() {
            totalNanos.clear();
            counts.clear();
        }

        void print() {
            for (Map.Entry<String, AtomicLong> e : new TreeMap<>(totalNanos).entrySet()) {
                long count = counts.get(e.getKey()).get();
                System.out.printf("METRIC %-38s count=%,d totalMs=%,.1f avgUs=%,.2f%n",
                                  e.getKey(), count, e.getValue().get() / 1e6,
                                  (double) e.getValue().get() / count / 1e3);
            }
        }
    }

    /** In-memory OTel pipeline for the smithy-java OperationMetricsPlugin. */
    static final class OtelHolder {
        final InMemoryMetricReader reader = InMemoryMetricReader.createDelta();
        final OpenTelemetry otel = OpenTelemetrySdk.builder()
            .setMeterProvider(SdkMeterProvider.builder().registerMetricReader(reader).build())
            .build();

        void reset() {
            reader.collectAllMetrics(); // delta reader: discard everything so far
        }

        void print() {
            for (MetricData md : new TreeMap<String, MetricData>() {
                {
                    for (MetricData m : reader.collectAllMetrics()) {
                        put(m.getName(), m);
                    }
                }
            }.values()) {
                if (md.getType() == MetricDataType.HISTOGRAM) {
                    double sum = 0;
                    long count = 0;
                    for (HistogramPointData p : md.getHistogramData().getPoints()) {
                        sum += p.getSum();
                        count += p.getCount();
                    }
                    if (count == 0) {
                        continue;
                    }
                    if ("s".equals(md.getUnit())) {
                        System.out.printf("METRIC %-52s count=%,d totalMs=%,.1f avgUs=%,.2f%n",
                                          md.getName(), count, sum * 1e3, sum / count * 1e6);
                    } else {
                        System.out.printf("METRIC %-52s count=%,d total=%,.0f avg=%,.0f (%s)%n",
                                          md.getName(), count, sum, sum / count, md.getUnit());
                    }
                }
            }
        }
    }

    // ==================== Request builders (identical shapes across SDKs) ====================

    private static software.amazon.awssdk.services.dynamodb.model.PutItemRequest v2DdbPut() {
        return software.amazon.awssdk.services.dynamodb.model.PutItemRequest.builder()
                                                                            .tableName("benchmark-table").item(v2Item).build();
    }

    private static software.amazon.awssdk.services.dynamodb.model.GetItemRequest v2DdbGet() {
        return software.amazon.awssdk.services.dynamodb.model.GetItemRequest.builder()
            .tableName("benchmark-table")
            .key(Map.of("pk", software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS("benchmark-key")))
            .build();
    }

    private static com.amazonaws.services.dynamodbv2.model.PutItemRequest v1DdbPut() {
        return new com.amazonaws.services.dynamodbv2.model.PutItemRequest()
            .withTableName("benchmark-table").withItem(v1Item);
    }

    private static com.amazonaws.services.dynamodbv2.model.GetItemRequest v1DdbGet() {
        return new com.amazonaws.services.dynamodbv2.model.GetItemRequest()
            .withTableName("benchmark-table")
            .withKey(Map.of("pk", new com.amazonaws.services.dynamodbv2.model.AttributeValue().withS("benchmark-key")));
    }

    private static software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue.Builder sjAv() {
        return software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue.builder();
    }

    private static software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.PutItemInput sjDdbPut() {
        return software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.PutItemInput.builder()
            .tableName("benchmark-table").item(sjItem).build();
    }

    private static software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.GetItemInput sjDdbGet() {
        return software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.GetItemInput.builder()
            .tableName("benchmark-table")
            .key(Map.of("pk", sjAv().s("benchmark-key").build()))
            .build();
    }

    private static software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest v2CwPut() {
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        List<software.amazon.awssdk.services.cloudwatch.model.MetricDatum> data = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            data.add(software.amazon.awssdk.services.cloudwatch.model.MetricDatum.builder()
                         .metricName("BenchmarkMetric" + i)
                         .timestamp(now.plusSeconds(i * 60))
                         .value((double) (40 + i))
                         .unit(software.amazon.awssdk.services.cloudwatch.model.StandardUnit.PERCENT)
                         .dimensions(
                             software.amazon.awssdk.services.cloudwatch.model.Dimension.builder()
                                 .name("InstanceId").value("i-12345678").build(),
                             software.amazon.awssdk.services.cloudwatch.model.Dimension.builder()
                                 .name("Environment").value("benchmark").build())
                         .build());
        }
        return software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest.builder()
            .namespace("Benchmark/Test").metricData(data).build();
    }

    private static software.amazon.awssdk.services.cloudwatch.model.GetMetricDataRequest v2CwGet() {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-01-01T01:00:00Z");
        List<software.amazon.awssdk.services.cloudwatch.model.MetricDataQuery> queries = new ArrayList<>();
        queries.add(software.amazon.awssdk.services.cloudwatch.model.MetricDataQuery.builder()
                        .id("cpu_utilization")
                        .metricStat(software.amazon.awssdk.services.cloudwatch.model.MetricStat.builder()
                                        .metric(software.amazon.awssdk.services.cloudwatch.model.Metric.builder()
                                                    .namespace("AWS/EC2").metricName("CPUUtilization")
                                                    .dimensions(software.amazon.awssdk.services.cloudwatch.model.Dimension
                                                                    .builder().name("InstanceId").value("i-12345678").build())
                                                    .build())
                                        .period(300).stat("Average").build())
                        .build());
        queries.add(software.amazon.awssdk.services.cloudwatch.model.MetricDataQuery.builder()
                        .id("memory_utilization")
                        .metricStat(software.amazon.awssdk.services.cloudwatch.model.MetricStat.builder()
                                        .metric(software.amazon.awssdk.services.cloudwatch.model.Metric.builder()
                                                    .namespace("CWAgent").metricName("MemoryUtilization")
                                                    .dimensions(software.amazon.awssdk.services.cloudwatch.model.Dimension
                                                                    .builder().name("InstanceId").value("i-12345678").build())
                                                    .build())
                                        .period(300).stat("Average").build())
                        .build());
        return software.amazon.awssdk.services.cloudwatch.model.GetMetricDataRequest.builder()
            .startTime(start).endTime(end).metricDataQueries(queries).build();
    }

    private static com.amazonaws.services.cloudwatch.model.PutMetricDataRequest v1CwPut() {
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        List<com.amazonaws.services.cloudwatch.model.MetricDatum> data = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            data.add(new com.amazonaws.services.cloudwatch.model.MetricDatum()
                         .withMetricName("BenchmarkMetric" + i)
                         .withTimestamp(Date.from(now.plusSeconds(i * 60)))
                         .withValue((double) (40 + i))
                         .withUnit(com.amazonaws.services.cloudwatch.model.StandardUnit.Percent)
                         .withDimensions(
                             new com.amazonaws.services.cloudwatch.model.Dimension()
                                 .withName("InstanceId").withValue("i-12345678"),
                             new com.amazonaws.services.cloudwatch.model.Dimension()
                                 .withName("Environment").withValue("benchmark")));
        }
        return new com.amazonaws.services.cloudwatch.model.PutMetricDataRequest()
            .withNamespace("Benchmark/Test").withMetricData(data);
    }

    private static com.amazonaws.services.cloudwatch.model.GetMetricDataRequest v1CwGet() {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-01-01T01:00:00Z");
        List<com.amazonaws.services.cloudwatch.model.MetricDataQuery> queries = new ArrayList<>();
        queries.add(new com.amazonaws.services.cloudwatch.model.MetricDataQuery()
                        .withId("cpu_utilization")
                        .withMetricStat(new com.amazonaws.services.cloudwatch.model.MetricStat()
                                            .withMetric(new com.amazonaws.services.cloudwatch.model.Metric()
                                                            .withNamespace("AWS/EC2").withMetricName("CPUUtilization")
                                                            .withDimensions(new com.amazonaws.services.cloudwatch.model.Dimension()
                                                                                .withName("InstanceId").withValue("i-12345678")))
                                            .withPeriod(300).withStat("Average")));
        queries.add(new com.amazonaws.services.cloudwatch.model.MetricDataQuery()
                        .withId("memory_utilization")
                        .withMetricStat(new com.amazonaws.services.cloudwatch.model.MetricStat()
                                            .withMetric(new com.amazonaws.services.cloudwatch.model.Metric()
                                                            .withNamespace("CWAgent").withMetricName("MemoryUtilization")
                                                            .withDimensions(new com.amazonaws.services.cloudwatch.model.Dimension()
                                                                                .withName("InstanceId").withValue("i-12345678")))
                                            .withPeriod(300).withStat("Average")));
        return new com.amazonaws.services.cloudwatch.model.GetMetricDataRequest()
            .withStartTime(Date.from(start)).withEndTime(Date.from(end)).withMetricDataQueries(queries);
    }
}
