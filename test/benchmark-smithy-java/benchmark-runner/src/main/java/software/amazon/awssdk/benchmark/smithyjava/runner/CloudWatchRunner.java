package software.amazon.awssdk.benchmark.smithyjava.runner;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.benchmark.smithyjava.utils.MockHttpServer;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.Metric;
import software.amazon.awssdk.services.cloudwatch.model.MetricDataQuery;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.MetricStat;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.smithy.java.aws.client.core.settings.RegionSetting;
import software.amazon.smithy.java.client.http.smithy.SmithyHttpClientTransport;
import software.amazon.smithy.java.endpoints.Endpoint;
import software.amazon.smithy.java.http.client.HttpClient;
import software.amazon.smithy.java.http.client.connection.HttpVersionPolicy;
import software.amazon.smithy.java.retries.api.RetryStrategy;

/**
 * Standalone fixed-iteration CloudWatch benchmark runner.
 * Runs PutMetricData and GetMetricData N times for each client variant.
 *
 * Usage: java -cp pipeline-benchmarks.jar ...CloudWatchRunner [iterations] [operation]
 *   iterations: number of operations (default 100000)
 *   operation:  v2-sync | v2-async | smithy-java | all (default: all)
 */
public class CloudWatchRunner {

    private static final int DEFAULT_ITERATIONS = 100_000;

    public static void main(String[] args) throws Exception {
        int iterations = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_ITERATIONS;
        String mode = args.length > 1 ? args[1] : "all";

        System.out.println("=== CloudWatch Pipeline Benchmark ===");
        System.out.println("Iterations: " + iterations);
        System.out.println("Mode: " + mode);
        System.out.println();

        byte[] putResponse = MockHttpServer.loadFixture("cloudwatch/putmetricdata-response.xml");
        byte[] getResponse = MockHttpServer.loadFixture("cloudwatch/getmetricdata-response.xml");

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

        MockHttpServer putServer = new MockHttpServer(putResponse, "text/xml");
        putServer.start();
        MockHttpServer getServer = new MockHttpServer(getResponse, "text/xml");
        getServer.start();

        CloudWatchClient putClient = CloudWatchClient.builder()
                .endpointOverride(putServer.getHttpUri())
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("benchmarkAccessKey", "benchmarkSecretKey")))
                .build();

        CloudWatchClient getClient = CloudWatchClient.builder()
                .endpointOverride(getServer.getHttpUri())
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("benchmarkAccessKey", "benchmarkSecretKey")))
                .build();

        // Warmup
        for (int i = 0; i < 1000; i++) {
            putClient.putMetricData(v2PutMetricDataRequest());
            getClient.getMetricData(v2GetMetricDataRequest());
        }

        // PutMetricData
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            putClient.putMetricData(v2PutMetricDataRequest());
        }
        long putDuration = System.nanoTime() - start;

        // GetMetricData
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            getClient.getMetricData(v2GetMetricDataRequest());
        }
        long getDuration = System.nanoTime() - start;

        printResults("V2-Sync PutMetricData", iterations, putDuration);
        printResults("V2-Sync GetMetricData", iterations, getDuration);

        putClient.close();
        getClient.close();
        putServer.stop();
        getServer.stop();
        System.out.println();
    }

    // ==================== V2 Async + CRT ====================

    private static void runV2Async(int iterations, byte[] putResponse, byte[] getResponse) throws Exception {
        System.out.println("--- V2 Async Client (CRT HTTP) ---");

        MockHttpServer putServer = new MockHttpServer(putResponse, "text/xml");
        putServer.start();
        MockHttpServer getServer = new MockHttpServer(getResponse, "text/xml");
        getServer.start();

        CloudWatchAsyncClient putClient = CloudWatchAsyncClient.builder()
                .endpointOverride(putServer.getHttpUri())
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("benchmarkAccessKey", "benchmarkSecretKey")))
                .httpClient(AwsCrtAsyncHttpClient.builder().maxConcurrency(50).build())
                .build();

        CloudWatchAsyncClient getClient = CloudWatchAsyncClient.builder()
                .endpointOverride(getServer.getHttpUri())
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("benchmarkAccessKey", "benchmarkSecretKey")))
                .httpClient(AwsCrtAsyncHttpClient.builder().maxConcurrency(50).build())
                .build();

        // Warmup
        for (int i = 0; i < 1000; i++) {
            putClient.putMetricData(v2PutMetricDataRequest()).join();
            getClient.getMetricData(v2GetMetricDataRequest()).join();
        }

        // PutMetricData
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            putClient.putMetricData(v2PutMetricDataRequest()).join();
        }
        long putDuration = System.nanoTime() - start;

        // GetMetricData
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            getClient.getMetricData(v2GetMetricDataRequest()).join();
        }
        long getDuration = System.nanoTime() - start;

        printResults("V2-Async PutMetricData", iterations, putDuration);
        printResults("V2-Async GetMetricData", iterations, getDuration);

        putClient.close();
        getClient.close();
        putServer.stop();
        getServer.stop();
        System.out.println();
    }

    // ==================== Smithy-Java ====================

    private static void runSmithyJava(int iterations, byte[] putResponse, byte[] getResponse) throws Exception {
        System.out.println("--- Smithy-Java Client ---");
        System.out.println("  [SKIP] Smithy-Java CloudWatch (awsQuery protocol) has a connection pool leak issue.");
        System.out.println("         Connections are not returned to pool after awsQuery responses.");
        System.out.println("         This is a smithy-java 1.5.1 bug with the SmithyHttpClient + awsQuery.");
        System.out.println();
    }

    // ==================== V2 Request Builders ====================

    private static PutMetricDataRequest v2PutMetricDataRequest() {
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        List<MetricDatum> data = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            data.add(MetricDatum.builder()
                    .metricName("BenchmarkMetric" + i)
                    .timestamp(now.plusSeconds(i * 60))
                    .value((double) (40 + i))
                    .unit(StandardUnit.PERCENT)
                    .dimensions(
                            Dimension.builder().name("InstanceId").value("i-12345678").build(),
                            Dimension.builder().name("Environment").value("benchmark").build())
                    .build());
        }
        return PutMetricDataRequest.builder().namespace("Benchmark/Test").metricData(data).build();
    }

    private static GetMetricDataRequest v2GetMetricDataRequest() {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-01-01T01:00:00Z");
        List<MetricDataQuery> queries = new ArrayList<>();
        queries.add(MetricDataQuery.builder()
                .id("cpu_utilization")
                .metricStat(MetricStat.builder()
                        .metric(Metric.builder().namespace("AWS/EC2").metricName("CPUUtilization")
                                .dimensions(Dimension.builder().name("InstanceId").value("i-12345678").build()).build())
                        .period(300).stat("Average").build())
                .build());
        queries.add(MetricDataQuery.builder()
                .id("memory_utilization")
                .metricStat(MetricStat.builder()
                        .metric(Metric.builder().namespace("CWAgent").metricName("MemoryUtilization")
                                .dimensions(Dimension.builder().name("InstanceId").value("i-12345678").build()).build())
                        .period(300).stat("Average").build())
                .build());
        return GetMetricDataRequest.builder().startTime(start).endTime(end).metricDataQueries(queries).build();
    }

    // ==================== Smithy-Java Request Builders ====================

    private static software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutMetricDataInput sjPutMetricDataInput() {
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        List<software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.MetricDatum> data = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            data.add(software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.MetricDatum.builder()
                    .metricName("BenchmarkMetric" + i)
                    .timestamp(now.plusSeconds(i * 60))
                    .value((double) (40 + i))
                    .unit(software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.StandardUnit.PERCENT)
                    .dimensions(List.of(
                            software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.Dimension.builder()
                                    .name("InstanceId").value("i-12345678").build(),
                            software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.Dimension.builder()
                                    .name("Environment").value("benchmark").build()))
                    .build());
        }
        return software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutMetricDataInput.builder()
                .namespace("Benchmark/Test").metricData(data).build();
    }

    private static software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetMetricDataInput sjGetMetricDataInput() {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-01-01T01:00:00Z");
        List<software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.MetricDataQuery> queries = new ArrayList<>();
        queries.add(software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.MetricDataQuery.builder()
                .id("cpu_utilization")
                .metricStat(software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.MetricStat.builder()
                        .metric(software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.Metric.builder()
                                .namespace("AWS/EC2").metricName("CPUUtilization")
                                .dimensions(List.of(software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.Dimension.builder()
                                        .name("InstanceId").value("i-12345678").build())).build())
                        .period(300).stat("Average").build())
                .build());
        queries.add(software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.MetricDataQuery.builder()
                .id("memory_utilization")
                .metricStat(software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.MetricStat.builder()
                        .metric(software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.Metric.builder()
                                .namespace("CWAgent").metricName("MemoryUtilization")
                                .dimensions(List.of(software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.Dimension.builder()
                                        .name("InstanceId").value("i-12345678").build())).build())
                        .period(300).stat("Average").build())
                .build());
        return software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetMetricDataInput.builder()
                .startTime(start).endTime(end).metricDataQueries(queries).build();
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
