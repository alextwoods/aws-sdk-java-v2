package software.amazon.awssdk.benchmark.smithyjava.s3;

import java.util.Random;
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
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.smithy.java.aws.client.core.settings.RegionSetting;
import software.amazon.smithy.java.client.http.smithy.SmithyHttpClientTransport;
import software.amazon.smithy.java.endpoints.Endpoint;
import software.amazon.smithy.java.http.client.HttpClient;
import software.amazon.smithy.java.http.client.connection.HttpVersionPolicy;
import software.amazon.smithy.java.io.datastream.DataStream;
import software.amazon.smithy.java.retries.api.RetryStrategy;

/**
 * Roundtrip benchmark for S3 GetObject and PutObject (1MB payload) comparing
 * AWS SDK Java V2 vs Smithy-Java generated client.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
@Fork(1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class S3Benchmark {

    private static final int OBJECT_SIZE = 1024 * 1024; // 1MB

    // Shared payload
    private byte[] payload;

    // ========== V2 SDK ==========
    private MockHttpServer v2PutServer;
    private MockHttpServer v2GetServer;
    private S3Client v2PutClient;
    private S3Client v2GetClient;

    // ========== Smithy-Java ==========
    private MockHttpServer sjPutServer;
    private MockHttpServer sjGetServer;
    private software.amazon.awssdk.benchmark.smithyjava.s3.client.S3Client sjPutClient;
    private software.amazon.awssdk.benchmark.smithyjava.s3.client.S3Client sjGetClient;
    private HttpClient sjPutHttpClient;
    private HttpClient sjGetHttpClient;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        // Create 1MB payload
        payload = new byte[OBJECT_SIZE];
        new Random(0xBEEF).nextBytes(payload);

        // For PutObject, server just returns 200 with ETag
        byte[] putResponse = new byte[0];
        String[][] putHeaders = {{"ETag", "\"d41d8cd98f00b204e9800998ecf8427e\""}};

        // For GetObject, server returns the 1MB payload
        byte[] getResponse = payload;
        String[][] getHeaders = {
                {"ETag", "\"d41d8cd98f00b204e9800998ecf8427e\""},
                {"Content-Length", String.valueOf(OBJECT_SIZE)}
        };

        // V2 SDK clients (path-style for simplicity with localhost)
        v2PutServer = new MockHttpServer(putResponse, "application/xml", putHeaders);
        v2PutServer.start();
        v2PutClient = S3Client.builder()
                .endpointOverride(v2PutServer.getHttpUri())
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("benchmarkAccessKey", "benchmarkSecretKey")))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();

        v2GetServer = new MockHttpServer(getResponse, "application/octet-stream", getHeaders);
        v2GetServer.start();
        v2GetClient = S3Client.builder()
                .endpointOverride(v2GetServer.getHttpUri())
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("benchmarkAccessKey", "benchmarkSecretKey")))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();

        // Smithy-Java clients
        var staticCreds = software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsIdentity
                .create("benchmarkAccessKey", "benchmarkSecretKey");
        var staticResolver = (software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsResolver)
                (ctx) -> software.amazon.smithy.java.auth.api.identity.IdentityResult.of(staticCreds);

        sjPutServer = new MockHttpServer(putResponse, "application/xml", putHeaders);
        sjPutServer.start();
        sjPutHttpClient = HttpClient.builder()
                .httpVersionPolicy(HttpVersionPolicy.ENFORCE_HTTP_1_1)
                .maxConnectionsPerRoute(50)
                .maxTotalConnections(50)
                .build();
        sjPutClient = software.amazon.awssdk.benchmark.smithyjava.s3.client.S3Client.builder()
                .putConfig(RegionSetting.REGION, "us-east-1")
                .endpoint(Endpoint.builder().uri(sjPutServer.getHttpUri().toString()).build())
                .addIdentityResolver(staticResolver)
                .retryStrategy(RetryStrategy.noRetries())
                .transport(new SmithyHttpClientTransport(sjPutHttpClient))
                .build();

        sjGetServer = new MockHttpServer(getResponse, "application/octet-stream", getHeaders);
        sjGetServer.start();
        sjGetHttpClient = HttpClient.builder()
                .httpVersionPolicy(HttpVersionPolicy.ENFORCE_HTTP_1_1)
                .maxConnectionsPerRoute(50)
                .maxTotalConnections(50)
                .build();
        sjGetClient = software.amazon.awssdk.benchmark.smithyjava.s3.client.S3Client.builder()
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
    public void v2_s3_putObject(Blackhole bh) {
        bh.consume(v2PutClient.putObject(
                PutObjectRequest.builder()
                        .bucket("benchmark-bucket")
                        .key("benchmark-key")
                        .build(),
                RequestBody.fromBytes(payload)));
    }

    @Benchmark
    public void v2_s3_getObject(Blackhole bh) {
        var response = v2GetClient.getObject(
                GetObjectRequest.builder()
                        .bucket("benchmark-bucket")
                        .key("benchmark-key")
                        .build());
        try {
            bh.consume(response.readAllBytes());
            response.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ========== Smithy-Java Benchmarks ==========

    @Benchmark
    public void sj_s3_putObject(Blackhole bh) {
        bh.consume(sjPutClient.putObject(
                software.amazon.awssdk.benchmark.smithyjava.s3.model.PutObjectInput.builder()
                        .bucket("benchmark-bucket")
                        .key("benchmark-key")
                        .contentLength((long) OBJECT_SIZE)
                        .body(DataStream.ofBytes(payload))
                        .build()));
    }

    @Benchmark
    public void sj_s3_getObject(Blackhole bh) {
        var output = sjGetClient.getObject(
                software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObjectInput.builder()
                        .bucket("benchmark-bucket")
                        .key("benchmark-key")
                        .build());
        try {
            bh.consume(output.getBody().asInputStream().readAllBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
