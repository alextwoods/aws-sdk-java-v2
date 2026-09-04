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

package software.amazon.awssdk.benchmark.smithybridge;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
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
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.client.handler.SdkPipeline;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

/**
 * End-to-end comparison of the smithy-java {@link SdkPipeline} against the default v2 pipeline.
 *
 * <p>Both clients are configured identically - same endpoint, same credentials, same
 * {@link Apache5HttpClient} transport - and both run in the same JVM against the same
 * canned-response Jetty server. The only difference is which pipeline executes the call:
 *
 * <ul>
 *   <li>{@code smithyJavaClient} leaves {@link SdkClientOption#SDK_PIPELINE} unset, so
 *       {@code SdkPipelineLoader} discovers {@code SmithyJavaPipelineProvider} from the
 *       {@code smithy-java-bridge} jar on the classpath.</li>
 *   <li>{@code standardClient} has {@link NoOpPipeline} injected into that option. Because
 *       {@code NoOpPipeline.supportsOperation()} always returns {@code false}, the generated
 *       client falls through to the existing v2 {@code clientHandler.execute(...)} path.</li>
 * </ul>
 *
 * <p>This keeps the classpath identical for both contenders, so the delta is attributable to
 * the pipeline itself rather than to differing dependency sets.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class SmithyBridgeBenchmark {

    private Server server;
    private DynamoDbClient smithyJavaClient;
    private DynamoDbClient standardClient;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        byte[] getItemResponse = loadFixture("fixtures/getitem-response.json");

        int port = getUnusedPort();
        server = createServer(port, getItemResponse);
        server.start();

        URI endpoint = URI.create("http://localhost:" + port);

        smithyJavaClient = DynamoDbClient.builder()
                                         .endpointOverride(endpoint)
                                         .region(Region.US_EAST_1)
                                         .credentialsProvider(benchmarkCredentials())
                                         .httpClient(Apache5HttpClient.create())
                                         .build();

        DynamoDbClientBuilder standardBuilder = DynamoDbClient.builder()
                                                              .endpointOverride(endpoint)
                                                              .region(Region.US_EAST_1)
                                                              .credentialsProvider(benchmarkCredentials())
                                                              .httpClient(Apache5HttpClient.create());
        injectSdkPipelineOption(standardBuilder, NoOpPipeline.INSTANCE);
        standardClient = standardBuilder.build();
    }

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        if (smithyJavaClient != null) {
            smithyJavaClient.close();
        }
        if (standardClient != null) {
            standardClient.close();
        }
        if (server != null) {
            server.stop();
        }
    }

    @Benchmark
    public void smithyJavaPipelineGetItem(Blackhole bh) {
        GetItemRequest request = GetItemRequest.builder()
                                              .tableName("benchmark-table")
                                              .key(keyMap())
                                              .build();
        bh.consume(smithyJavaClient.getItem(request));
    }

    @Benchmark
    public void smithyJavaPipelinePutItem(Blackhole bh) {
        PutItemRequest request = PutItemRequest.builder()
                                              .tableName("benchmark-table")
                                              .item(itemMap())
                                              .build();
        bh.consume(smithyJavaClient.putItem(request));
    }

    @Benchmark
    public void standardPipelineGetItem(Blackhole bh) {
        GetItemRequest request = GetItemRequest.builder()
                                              .tableName("benchmark-table")
                                              .key(keyMap())
                                              .build();
        bh.consume(standardClient.getItem(request));
    }

    @Benchmark
    public void standardPipelinePutItem(Blackhole bh) {
        PutItemRequest request = PutItemRequest.builder()
                                              .tableName("benchmark-table")
                                              .item(itemMap())
                                              .build();
        bh.consume(standardClient.putItem(request));
    }

    private static StaticCredentialsProvider benchmarkCredentials() {
        return StaticCredentialsProvider.create(
            AwsBasicCredentials.create("benchmarkAccessKey", "benchmarkSecretKey"));
    }

    /**
     * Forces a client onto a specific {@link SdkPipeline} by reaching into the builder's
     * {@code clientConfiguration} and setting {@link SdkClientOption#SDK_PIPELINE}. There is no
     * public API for this, and deliberately so - the option exists for testing and benchmarking.
     */
    private static void injectSdkPipelineOption(DynamoDbClientBuilder builder, SdkPipeline pipeline) {
        try {
            Field configField = findField(builder.getClass(), "clientConfiguration");
            configField.setAccessible(true);
            SdkClientConfiguration.Builder configBuilder = (SdkClientConfiguration.Builder) configField.get(builder);
            configBuilder.option(SdkClientOption.SDK_PIPELINE, pipeline);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to inject SDK_PIPELINE option into builder", e);
        }
    }

    private static Field findField(Class<?> clazz, String fieldName) {
        for (Class<?> current = clazz; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                // Keep walking up the hierarchy.
            }
        }
        throw new RuntimeException("Field '" + fieldName + "' not found in hierarchy of " + clazz.getName());
    }

    private static Map<String, AttributeValue> keyMap() {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("pk", AttributeValue.fromS("benchmark-key"));
        key.put("sk", AttributeValue.fromN("100"));
        return key;
    }

    private static Map<String, AttributeValue> itemMap() {
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

        Map<String, AttributeValue> deep = new HashMap<>();
        deep.put("level2", AttributeValue.fromN("999"));

        Map<String, AttributeValue> nested = new HashMap<>();
        nested.put("nested", AttributeValue.fromS("nested-value"));
        nested.put("deepNested", AttributeValue.fromM(deep));
        item.put("mapField", AttributeValue.fromM(nested));

        item.put("listField", AttributeValue.builder()
                                            .l(AttributeValue.fromS("item1"),
                                               AttributeValue.fromN("42"),
                                               AttributeValue.fromBool(true),
                                               AttributeValue.builder().nul(true).build())
                                            .build());
        return item;
    }

    private static Server createServer(int port, byte[] responseBody) {
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.setConnectors(new Connector[] {connector});

        ServletContextHandler context = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
        context.addServlet(new ServletHolder(new CannedResponseServlet(responseBody)), "/*");
        server.setHandler(context);
        return server;
    }

    private static int getUnusedPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    private static byte[] loadFixture(String path) throws IOException {
        try (InputStream is = SmithyBridgeBenchmark.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Fixture not found: " + path);
            }
            byte[] buffer = new byte[8192];
            int totalRead = 0;
            int bytesRead;
            while ((bytesRead = is.read(buffer, totalRead, buffer.length - totalRead)) != -1) {
                totalRead += bytesRead;
                if (totalRead == buffer.length) {
                    byte[] newBuffer = new byte[buffer.length * 2];
                    System.arraycopy(buffer, 0, newBuffer, 0, totalRead);
                    buffer = newBuffer;
                }
            }
            byte[] result = new byte[totalRead];
            System.arraycopy(buffer, 0, result, 0, totalRead);
            return result;
        }
    }

    /**
     * An {@link SdkPipeline} that never claims an operation, so the generated client always falls
     * back to the default v2 execution path. Injecting this is how the "standard" contender is
     * pinned to v2 even though the smithy-java bridge is on the classpath.
     */
    static final class NoOpPipeline implements SdkPipeline {
        static final NoOpPipeline INSTANCE = new NoOpPipeline();

        private NoOpPipeline() {
        }

        @Override
        public <InputT extends SdkRequest, OutputT extends SdkResponse> OutputT execute(
                ClientExecutionParams<InputT, OutputT> executionParams,
                SdkClientConfiguration clientConfiguration) {
            throw new UnsupportedOperationException("NoOpPipeline does not execute operations. "
                                                    + "supportsOperation() should prevent this call.");
        }

        @Override
        public <InputT extends SdkRequest, OutputT extends SdkResponse> boolean supportsOperation(
                ClientExecutionParams<InputT, OutputT> executionParams) {
            return false;
        }

        @Override
        public void close() {
        }
    }

    static class CannedResponseServlet extends HttpServlet {
        private final byte[] body;

        CannedResponseServlet(byte[] body) {
            this.body = body;
        }

        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setStatus(200);
            resp.setContentLength(body.length);
            resp.setContentType("application/x-amz-json-1.0");
            resp.getOutputStream().write(body);
        }
    }
}
