package software.amazon.awssdk.benchmark.smithyjava.runner;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.benchmark.smithyjava.utils.MockHttpServer;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
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
 * Standalone fixed-iteration S3 benchmark runner.
 * Runs PutObject and GetObject (1MB) N times for each client variant.
 *
 * Usage: java -cp pipeline-benchmarks.jar ...S3Runner [iterations] [operation]
 *   iterations: number of operations (default 100000)
 *   operation:  v2-sync | v2-async | smithy-java | all (default: all)
 */
public class S3Runner {

    private static final int DEFAULT_ITERATIONS = 100_000;
    private static final int OBJECT_SIZE = 1024 * 1024; // 1MB

    public static void main(String[] args) throws Exception {
        int iterations = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_ITERATIONS;
        String mode = args.length > 1 ? args[1] : "all";

        System.out.println("=== S3 Pipeline Benchmark ===");
        System.out.println("Iterations: " + iterations);
        System.out.println("Object size: " + OBJECT_SIZE + " bytes (1MB)");
        System.out.println("Mode: " + mode);
        System.out.println();

        byte[] payload = new byte[OBJECT_SIZE];
        new Random(0xBEEF).nextBytes(payload);

        byte[] putResponse = new byte[0];
        String[][] putHeaders = {{"ETag", "\"d41d8cd98f00b204e9800998ecf8427e\""}};
        String[][] getHeaders = {
                {"ETag", "\"d41d8cd98f00b204e9800998ecf8427e\""},
                {"Content-Length", String.valueOf(OBJECT_SIZE)}
        };

        if ("all".equals(mode) || "v2-sync".equals(mode)) {
            runV2Sync(iterations, payload, putResponse, putHeaders, getHeaders);
        }
        if ("all".equals(mode) || "v2-async".equals(mode)) {
            runV2Async(iterations, payload, putResponse, putHeaders, getHeaders);
        }
        if ("all".equals(mode) || "smithy-java".equals(mode)) {
            runSmithyJava(iterations, payload, putResponse, putHeaders, getHeaders);
        }
    }

    // ==================== V2 Sync ====================

    private static void runV2Sync(int iterations, byte[] payload, byte[] putResponse,
                                  String[][] putHeaders, String[][] getHeaders) throws Exception {
        System.out.println("--- V2 Sync Client ---");

        MockHttpServer putServer = new MockHttpServer(putResponse, "application/xml", putHeaders);
        putServer.start();
        MockHttpServer getServer = new MockHttpServer(payload, "application/octet-stream", getHeaders);
        getServer.start();

        S3Client putClient = S3Client.builder()
                .endpointOverride(putServer.getHttpUri())
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("benchmarkAccessKey", "benchmarkSecretKey")))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();

        S3Client getClient = S3Client.builder()
                .endpointOverride(getServer.getHttpUri())
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("benchmarkAccessKey", "benchmarkSecretKey")))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();

        // Warmup
        for (int i = 0; i < 100; i++) {
            putClient.putObject(PutObjectRequest.builder().bucket("b").key("k").build(), RequestBody.fromBytes(payload));
            getClient.getObject(GetObjectRequest.builder().bucket("b").key("k").build()).readAllBytes();
        }

        // PutObject
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            putClient.putObject(PutObjectRequest.builder().bucket("b").key("k").build(), RequestBody.fromBytes(payload));
        }
        long putDuration = System.nanoTime() - start;

        // GetObject
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            try (var resp = getClient.getObject(GetObjectRequest.builder().bucket("b").key("k").build())) {
                resp.readAllBytes();
            }
        }
        long getDuration = System.nanoTime() - start;

        printResults("V2-Sync PutObject", iterations, putDuration);
        printResults("V2-Sync GetObject", iterations, getDuration);

        putClient.close();
        getClient.close();
        putServer.stop();
        getServer.stop();
        System.out.println();
    }

    // ==================== V2 Async + CRT ====================

    private static void runV2Async(int iterations, byte[] payload, byte[] putResponse,
                                   String[][] putHeaders, String[][] getHeaders) throws Exception {
        System.out.println("--- V2 Async Client (CRT HTTP) ---");

        MockHttpServer putServer = new MockHttpServer(putResponse, "application/xml", putHeaders);
        putServer.start();
        MockHttpServer getServer = new MockHttpServer(payload, "application/octet-stream", getHeaders);
        getServer.start();

        S3AsyncClient putClient = S3AsyncClient.builder()
                .endpointOverride(putServer.getHttpUri())
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("benchmarkAccessKey", "benchmarkSecretKey")))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .httpClient(AwsCrtAsyncHttpClient.builder().maxConcurrency(50).build())
                .build();

        S3AsyncClient getClient = S3AsyncClient.builder()
                .endpointOverride(getServer.getHttpUri())
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("benchmarkAccessKey", "benchmarkSecretKey")))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .httpClient(AwsCrtAsyncHttpClient.builder().maxConcurrency(50).build())
                .build();

        // Warmup
        for (int i = 0; i < 100; i++) {
            putClient.putObject(PutObjectRequest.builder().bucket("b").key("k").build(),
                    AsyncRequestBody.fromBytes(payload)).join();
            getClient.getObject(GetObjectRequest.builder().bucket("b").key("k").build(),
                    AsyncResponseTransformer.toBytes()).join();
        }

        // PutObject
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            putClient.putObject(PutObjectRequest.builder().bucket("b").key("k").build(),
                    AsyncRequestBody.fromBytes(payload)).join();
        }
        long putDuration = System.nanoTime() - start;

        // GetObject
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            getClient.getObject(GetObjectRequest.builder().bucket("b").key("k").build(),
                    AsyncResponseTransformer.toBytes()).join();
        }
        long getDuration = System.nanoTime() - start;

        printResults("V2-Async PutObject", iterations, putDuration);
        printResults("V2-Async GetObject", iterations, getDuration);

        putClient.close();
        getClient.close();
        putServer.stop();
        getServer.stop();
        System.out.println();
    }

    // ==================== Smithy-Java ====================

    private static void runSmithyJava(int iterations, byte[] payload, byte[] putResponse,
                                      String[][] putHeaders, String[][] getHeaders) throws Exception {
        System.out.println("--- Smithy-Java Client ---");

        MockHttpServer putServer = new MockHttpServer(putResponse, "application/xml", putHeaders);
        putServer.start();
        MockHttpServer getServer = new MockHttpServer(payload, "application/octet-stream", getHeaders);
        getServer.start();

        var staticCreds = software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsIdentity
                .create("benchmarkAccessKey", "benchmarkSecretKey");
        var staticResolver = (software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsResolver)
                (ctx) -> software.amazon.smithy.java.auth.api.identity.IdentityResult.of(staticCreds);

        HttpClient putHttpClient = HttpClient.builder()
                .httpVersionPolicy(HttpVersionPolicy.ENFORCE_HTTP_1_1)
                .maxConnectionsPerRoute(50).maxTotalConnections(50).build();
        var sjPutClient = software.amazon.awssdk.benchmark.smithyjava.s3.client.S3Client.builder()
                .putConfig(RegionSetting.REGION, "us-east-1")
                .endpoint(Endpoint.builder().uri(putServer.getHttpUri().toString()).build())
                .addIdentityResolver(staticResolver)
                .retryStrategy(RetryStrategy.noRetries())
                .transport(new SmithyHttpClientTransport(putHttpClient))
                .build();

        HttpClient getHttpClient = HttpClient.builder()
                .httpVersionPolicy(HttpVersionPolicy.ENFORCE_HTTP_1_1)
                .maxConnectionsPerRoute(50).maxTotalConnections(50).build();
        var sjGetClient = software.amazon.awssdk.benchmark.smithyjava.s3.client.S3Client.builder()
                .putConfig(RegionSetting.REGION, "us-east-1")
                .endpoint(Endpoint.builder().uri(getServer.getHttpUri().toString()).build())
                .addIdentityResolver(staticResolver)
                .retryStrategy(RetryStrategy.noRetries())
                .transport(new SmithyHttpClientTransport(getHttpClient))
                .build();

        // Warmup
        for (int i = 0; i < 100; i++) {
            sjPutClient.putObject(software.amazon.awssdk.benchmark.smithyjava.s3.model.PutObjectInput.builder()
                    .bucket("b").key("k").contentLength((long) OBJECT_SIZE).body(DataStream.ofBytes(payload)).build());
            sjGetClient.getObject(software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObjectInput.builder()
                    .bucket("b").key("k").build()).getBody().asInputStream().readAllBytes();
        }

        // PutObject
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            sjPutClient.putObject(software.amazon.awssdk.benchmark.smithyjava.s3.model.PutObjectInput.builder()
                    .bucket("b").key("k").contentLength((long) OBJECT_SIZE).body(DataStream.ofBytes(payload)).build());
        }
        long putDuration = System.nanoTime() - start;

        // GetObject
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            sjGetClient.getObject(software.amazon.awssdk.benchmark.smithyjava.s3.model.GetObjectInput.builder()
                    .bucket("b").key("k").build()).getBody().asInputStream().readAllBytes();
        }
        long getDuration = System.nanoTime() - start;

        printResults("SmithyJava PutObject", iterations, putDuration);
        printResults("SmithyJava GetObject", iterations, getDuration);

        ((software.amazon.smithy.java.client.core.Client) sjPutClient).close();
        ((software.amazon.smithy.java.client.core.Client) sjGetClient).close();
        putHttpClient.close();
        getHttpClient.close();
        putServer.stop();
        getServer.stop();
        System.out.println();
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
