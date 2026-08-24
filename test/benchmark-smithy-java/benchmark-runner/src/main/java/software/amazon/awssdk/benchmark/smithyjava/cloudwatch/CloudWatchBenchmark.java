package software.amazon.awssdk.benchmark.smithyjava.cloudwatch;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.benchmark.smithyjava.utils.MockHttpServer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.MetricDataQuery;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.MetricStat;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.awssdk.services.cloudwatch.model.Metric;
import software.amazon.smithy.java.aws.client.core.settings.RegionSetting;
import software.amazon.smithy.java.client.http.smithy.SmithyHttpClientTransport;
import software.amazon.smithy.java.endpoints.Endpoint;
import software.amazon.smithy.java.http.client.HttpClient;
import software.amazon.smithy.java.http.client.connection.HttpVersionPolicy;
import software.amazon.smithy.java.retries.api.RetryStrategy;

/**
 * Roundtrip benchmark for CloudWatch GetMetricData and PutMetricData comparing
 * AWS SDK Java V2 vs Smithy-Java generated client.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
@Fork(1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class CloudWatchBenchmark {

    // ========== V2 SDK ==========
    private MockHttpServer v2PutServer;
    private MockHttpServer v2GetServer;
    private CloudWatchClient v2PutClient;
    private CloudWatchClient v2GetClient;

    // ========== Smithy-Java ==========
    private MockHttpServer sjPutServer;
    private MockHttpServer sjGetServer;
    private software.amazon.awssdk.benchmark.smithyjava.cloudwatch.client.CloudWatchClient sjPutClient;
    private software.amazon.awssdk.benchmark.smithyjava.cloudwatch.client.CloudWatchClient sjGetClient;
    private HttpClient sjPutHttpClient;
    private HttpClient sjGetHttpClient;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        byte[] putResponse = MockHttpServer.loadFixture("cloudwatch/putmetricdata-response.xml");
        byte[] getResponse = MockHttpServer.loadFixture("cloudwatch/getmetricdata-response.xml");

        // V2 SDK clients
        v2PutServer = new MockHttpServer(putResponse, "text/xml");
        v2PutServer.start();
        v2PutClient = CloudWatchClient.builder()
                .endpointOverride(v2PutServer.getHttpUri())
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("benchmarkAccessKey", "benchmarkSecretKey")))
                .build();

        v2GetServer = new MockHttpServer(getResponse, "text/xml");
        v2GetServer.start();
        v2GetClient = CloudWatchClient.builder()
                .endpointOverride(v2GetServer.getHttpUri())
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("benchmarkAccessKey", "benchmarkSecretKey")))
                .build();

        // Smithy-Java clients
        var staticCreds = software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsIdentity
                .create("benchmarkAccessKey", "benchmarkSecretKey");
        var staticResolver = (software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsResolver)
                (ctx) -> software.amazon.smithy.java.auth.api.identity.IdentityResult.of(staticCreds);

        sjPutServer = new MockHttpServer(putResponse, "text/xml");
        sjPutServer.start();
        sjPutHttpClient = HttpClient.builder()
                .httpVersionPolicy(HttpVersionPolicy.ENFORCE_HTTP_1_1)
                .maxConnectionsPerRoute(50)
                .maxTotalConnections(50)
                .build();
        sjPutClient = software.amazon.awssdk.benchmark.smithyjava.cloudwatch.client.CloudWatchClient.builder()
                .putConfig(RegionSetting.REGION, "us-east-1")
                .endpoint(Endpoint.builder().uri(sjPutServer.getHttpUri().toString()).build())
                .addIdentityResolver(staticResolver)
                .retryStrategy(RetryStrategy.noRetries())
                .transport(new SmithyHttpClientTransport(sjPutHttpClient))
                .build();

        sjGetServer = new MockHttpServer(getResponse, "text/xml");
        sjGetServer.start();
        sjGetHttpClient = HttpClient.builder()
                .httpVersionPolicy(HttpVersionPolicy.ENFORCE_HTTP_1_1)
                .maxConnectionsPerRoute(50)
                .maxTotalConnections(50)
                .build();
        sjGetClient = software.amazon.awssdk.benchmark.smithyjava.cloudwatch.client.CloudWatchClient.builder()
                .putConfig(RegionSetting.REGION, "us-east-1")
                .endpoint(Endpoint.builder().uri(sjGetServer.getHttpUri().toString()).build())
                .addIdentityResolver(staticResolver)
                .retryStrategy(RetryStrategy.noRetries())
                .transport(new SmithyHttpClientTransport(sjGetHttpClient))
                .build();
    }

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        v2PutClient.close();
        v2GetClient.close();
        ((software.amazon.smithy.java.client.core.Client) sjPutClient).close();
        ((software.amazon.smithy.java.client.core.Client) sjGetClient).close();
        sjPutHttpClient.close();
        sjGetHttpClient.close();
        v2PutServer.stop();
        v2GetServer.stop();
        sjPutServer.stop();
        sjGetServer.stop();
    }

    // ========== V2 SDK Benchmarks ==========

    @Benchmark
    public void v2_cloudwatch_putMetricData(Blackhole bh) {
        bh.consume(v2PutClient.putMetricData(buildV2PutMetricDataRequest()));
    }

    @Benchmark
    public void v2_cloudwatch_getMetricData(Blackhole bh) {
        bh.consume(v2GetClient.getMetricData(buildV2GetMetricDataRequest()));
    }

    // ========== Smithy-Java Benchmarks ==========

    @Benchmark
    public void sj_cloudwatch_putMetricData(Blackhole bh) {
        bh.consume(sjPutClient.putMetricData(buildSjPutMetricDataInput()));
    }

    @Benchmark
    public void sj_cloudwatch_getMetricData(Blackhole bh) {
        bh.consume(sjGetClient.getMetricData(buildSjGetMetricDataInput()));
    }

    // ========== V2 Request Builders ==========

    private static PutMetricDataRequest buildV2PutMetricDataRequest() {
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
        return PutMetricDataRequest.builder()
                .namespace("Benchmark/Test")
                .metricData(data)
                .build();
    }

    private static GetMetricDataRequest buildV2GetMetricDataRequest() {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-01-01T01:00:00Z");
        List<MetricDataQuery> queries = new ArrayList<>();
        queries.add(MetricDataQuery.builder()
                .id("cpu_utilization")
                .metricStat(MetricStat.builder()
                        .metric(Metric.builder()
                                .namespace("AWS/EC2")
                                .metricName("CPUUtilization")
                                .dimensions(
                                        Dimension.builder().name("InstanceId").value("i-12345678").build())
                                .build())
                        .period(300)
                        .stat("Average")
                        .build())
                .build());
        queries.add(MetricDataQuery.builder()
                .id("memory_utilization")
                .metricStat(MetricStat.builder()
                        .metric(Metric.builder()
                                .namespace("CWAgent")
                                .metricName("MemoryUtilization")
                                .dimensions(
                                        Dimension.builder().name("InstanceId").value("i-12345678").build())
                                .build())
                        .period(300)
                        .stat("Average")
                        .build())
                .build());
        return GetMetricDataRequest.builder()
                .startTime(start)
                .endTime(end)
                .metricDataQueries(queries)
                .build();
    }

    // ========== Smithy-Java Request Builders ==========

    private static software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.PutMetricDataInput buildSjPutMetricDataInput() {
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
                .namespace("Benchmark/Test")
                .metricData(data)
                .build();
    }

    private static software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetMetricDataInput buildSjGetMetricDataInput() {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-01-01T01:00:00Z");
        List<software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.MetricDataQuery> queries = new ArrayList<>();
        queries.add(software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.MetricDataQuery.builder()
                .id("cpu_utilization")
                .metricStat(software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.MetricStat.builder()
                        .metric(software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.Metric.builder()
                                .namespace("AWS/EC2")
                                .metricName("CPUUtilization")
                                .dimensions(List.of(
                                        software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.Dimension.builder()
                                                .name("InstanceId").value("i-12345678").build()))
                                .build())
                        .period(300)
                        .stat("Average")
                        .build())
                .build());
        queries.add(software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.MetricDataQuery.builder()
                .id("memory_utilization")
                .metricStat(software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.MetricStat.builder()
                        .metric(software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.Metric.builder()
                                .namespace("CWAgent")
                                .metricName("MemoryUtilization")
                                .dimensions(List.of(
                                        software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.Dimension.builder()
                                                .name("InstanceId").value("i-12345678").build()))
                                .build())
                        .period(300)
                        .stat("Average")
                        .build())
                .build());
        return software.amazon.awssdk.benchmark.smithyjava.cloudwatch.model.GetMetricDataInput.builder()
                .startTime(start)
                .endTime(end)
                .metricDataQueries(queries)
                .build();
    }
}
