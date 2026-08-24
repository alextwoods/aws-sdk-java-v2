package software.amazon.awssdk.benchmark.smithyjava.dynamodb;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
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
 * Roundtrip benchmark for DynamoDB PutItem and GetItem comparing
 * AWS SDK Java V2 vs Smithy-Java generated client.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
@Fork(1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class DynamoDbBenchmark {

    // ========== V2 SDK ==========
    private MockHttpServer v2PutServer;
    private MockHttpServer v2GetServer;
    private DynamoDbClient v2Client;
    private DynamoDbClient v2GetClient;

    // ========== Smithy-Java ==========
    private MockHttpServer sjPutServer;
    private MockHttpServer sjGetServer;
    private software.amazon.awssdk.benchmark.smithyjava.dynamodb.client.DynamoDBClient sjClient;
    private software.amazon.awssdk.benchmark.smithyjava.dynamodb.client.DynamoDBClient sjGetClient;
    private HttpClient sjHttpClient;
    private HttpClient sjGetHttpClient;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        byte[] putResponse = MockHttpServer.loadFixture("dynamodb/putitem-response.json");
        byte[] getResponse = MockHttpServer.loadFixture("dynamodb/getitem-response.json");

        // V2 SDK clients
        v2PutServer = new MockHttpServer(putResponse, "application/x-amz-json-1.0");
        v2PutServer.start();
        v2Client = DynamoDbClient.builder()
                .endpointOverride(v2PutServer.getHttpUri())
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("benchmarkAccessKey", "benchmarkSecretKey")))
                .build();

        v2GetServer = new MockHttpServer(getResponse, "application/x-amz-json-1.0");
        v2GetServer.start();
        v2GetClient = DynamoDbClient.builder()
                .endpointOverride(v2GetServer.getHttpUri())
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("benchmarkAccessKey", "benchmarkSecretKey")))
                .build();

        // Smithy-Java clients
        sjPutServer = new MockHttpServer(putResponse, "application/x-amz-json-1.0");
        sjPutServer.start();
        sjHttpClient = HttpClient.builder()
                .httpVersionPolicy(HttpVersionPolicy.ENFORCE_HTTP_1_1)
                .maxConnectionsPerRoute(50)
                .maxTotalConnections(50)
                .build();

        var staticCreds = software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsIdentity
                .create("benchmarkAccessKey", "benchmarkSecretKey");
        var staticResolver = (software.amazon.smithy.java.aws.auth.api.identity.AwsCredentialsResolver)
                (ctx) -> software.amazon.smithy.java.auth.api.identity.IdentityResult.of(staticCreds);

        sjClient = software.amazon.awssdk.benchmark.smithyjava.dynamodb.client.DynamoDBClient.builder()
                .putConfig(RegionSetting.REGION, "us-east-1")
                .endpoint(Endpoint.builder().uri(sjPutServer.getHttpUri().toString()).build())
                .addIdentityResolver(staticResolver)
                .retryStrategy(RetryStrategy.noRetries())
                .transport(new SmithyHttpClientTransport(sjHttpClient))
                .build();

        sjGetServer = new MockHttpServer(getResponse, "application/x-amz-json-1.0");
        sjGetServer.start();
        sjGetHttpClient = HttpClient.builder()
                .httpVersionPolicy(HttpVersionPolicy.ENFORCE_HTTP_1_1)
                .maxConnectionsPerRoute(50)
                .maxTotalConnections(50)
                .build();

        sjGetClient = software.amazon.awssdk.benchmark.smithyjava.dynamodb.client.DynamoDBClient.builder()
                .putConfig(RegionSetting.REGION, "us-east-1")
                .endpoint(Endpoint.builder().uri(sjGetServer.getHttpUri().toString()).build())
                .addIdentityResolver(staticResolver)
                .retryStrategy(RetryStrategy.noRetries())
                .transport(new SmithyHttpClientTransport(sjGetHttpClient))
                .build();
    }

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        v2Client.close();
        v2GetClient.close();
        ((software.amazon.smithy.java.client.core.Client) sjClient).close();
        ((software.amazon.smithy.java.client.core.Client) sjGetClient).close();
        sjHttpClient.close();
        sjGetHttpClient.close();
        v2PutServer.stop();
        v2GetServer.stop();
        sjPutServer.stop();
        sjGetServer.stop();
    }

    // ========== V2 SDK Benchmarks ==========

    @Benchmark
    public void v2_dynamodb_putItem(Blackhole bh) {
        bh.consume(v2Client.putItem(PutItemRequest.builder()
                .tableName("benchmark-table")
                .item(v2ItemMap())
                .build()));
    }

    @Benchmark
    public void v2_dynamodb_getItem(Blackhole bh) {
        bh.consume(v2GetClient.getItem(GetItemRequest.builder()
                .tableName("benchmark-table")
                .key(Map.of("pk", AttributeValue.fromS("benchmark-key")))
                .build()));
    }

    // ========== Smithy-Java Benchmarks ==========

    @Benchmark
    public void sj_dynamodb_putItem(Blackhole bh) {
        bh.consume(sjClient.putItem(
                software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.PutItemInput.builder()
                        .tableName("benchmark-table")
                        .item(sjItemMap())
                        .build()));
    }

    @Benchmark
    public void sj_dynamodb_getItem(Blackhole bh) {
        bh.consume(sjGetClient.getItem(
                software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.GetItemInput.builder()
                        .tableName("benchmark-table")
                        .key(Map.of("pk",
                                software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue
                                        .builder().s("benchmark-key").build()))
                        .build()));
    }

    // ========== Helper Methods ==========

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

    private static Map<String, software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue> sjItemMap() {
        var sjAv = software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue.class;
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
}
