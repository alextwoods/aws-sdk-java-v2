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
 * JMH benchmark comparing DynamoDB operations through the standard v2 pipeline vs
 * the smithy-java bridge pipeline (SmithyJavaPipeline).
 *
 * <h2>How pipeline selection works</h2>
 * <ul>
 *   <li><b>smithy-java pipeline</b>: The {@code smithy-java-bridge} module is on the classpath
 *       and discovered via ServiceLoader (SPI). No explicit SDK_PIPELINE option is set, so the
 *       normal SPI discovery finds and uses the SmithyJavaPipeline for supported operations.</li>
 *   <li><b>standard pipeline</b>: A {@link NoOpPipeline} is set via {@code SdkClientOption.SDK_PIPELINE}.
 *       This pipeline always returns {@code supportsOperation() = false}, which causes the generated
 *       client to fall back to the standard v2 handler path regardless of what's on the classpath.</li>
 * </ul>
 *
 * <h2>Running</h2>
 * <pre>
 * # Build the uber-JAR:
 * mvn clean install -pl :smithy-bridge-benchmark -P quick --am
 *
 * # Run all benchmarks:
 * java -jar test/smithy-bridge-benchmark/target/smithy-bridge-benchmarks.jar
 *
 * # Run only GetItem:
 * java -jar test/smithy-bridge-benchmark/target/smithy-bridge-benchmarks.jar ".*GetItem.*"
 * </pre>
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
        // Single Jetty server that returns a canned response for all requests.
        // The response payload doesn't affect pipeline selection — both pipelines
        // use the same server.
        byte[] getItemResponse = loadFixture("fixtures/getitem-response.json");

        int port = getUnusedPort();
        server = createServer(port, getItemResponse);
        server.start();

        URI endpoint = URI.create("http://localhost:" + port);

        // smithy-java pipeline client: no SDK_PIPELINE option set, so ServiceLoader
        // discovers SmithyJavaPipeline from the smithy-java-bridge module on the classpath.
        smithyJavaClient = DynamoDbClient.builder()
            .endpointOverride(endpoint)
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create("benchmarkAccessKey", "benchmarkSecretKey")))
            .httpClient(Apache5HttpClient.create())
            .build();

        // standard pipeline client: set SDK_PIPELINE to NoOpPipeline which always returns
        // supportsOperation() = false, forcing fallback to the standard v2 handler.
        DynamoDbClientBuilder standardBuilder = DynamoDbClient.builder()
            .endpointOverride(endpoint)
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create("benchmarkAccessKey", "benchmarkSecretKey")))
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

    /**
     * Benchmark DynamoDB GetItem through the smithy-java bridge pipeline.
     */
    @Benchmark
    public void smithyJavaPipelineGetItem(Blackhole bh) {
        GetItemRequest request = GetItemRequest.builder()
            .tableName("benchmark-table")
            .key(keyMap())
            .build();
        bh.consume(smithyJavaClient.getItem(request));
    }

    /**
     * Benchmark DynamoDB PutItem through the smithy-java bridge pipeline.
     */
    @Benchmark
    public void smithyJavaPipelinePutItem(Blackhole bh) {
        PutItemRequest request = PutItemRequest.builder()
            .tableName("benchmark-table")
            .item(itemMap())
            .build();
        bh.consume(smithyJavaClient.putItem(request));
    }

    /**
     * Benchmark DynamoDB GetItem through the standard v2 pipeline (forced via NoOpPipeline).
     */
    @Benchmark
    public void standardPipelineGetItem(Blackhole bh) {
        GetItemRequest request = GetItemRequest.builder()
            .tableName("benchmark-table")
            .key(keyMap())
            .build();
        bh.consume(standardClient.getItem(request));
    }

    /**
     * Benchmark DynamoDB PutItem through the standard v2 pipeline (forced via NoOpPipeline).
     */
    @Benchmark
    public void standardPipelinePutItem(Blackhole bh) {
        PutItemRequest request = PutItemRequest.builder()
            .tableName("benchmark-table")
            .item(itemMap())
            .build();
        bh.consume(standardClient.putItem(request));
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    /**
     * Injects the {@code SdkClientOption.SDK_PIPELINE} option into the client builder's
     * internal configuration via reflection. This is necessary because the public builder
     * API does not expose a setter for this internal option.
     */
    private static void injectSdkPipelineOption(DynamoDbClientBuilder builder, SdkPipeline pipeline) {
        try {
            // SdkDefaultClientBuilder has a protected field: clientConfiguration (SdkClientConfiguration.Builder)
            Field configField = findField(builder.getClass(), "clientConfiguration");
            configField.setAccessible(true);
            SdkClientConfiguration.Builder configBuilder =
                (SdkClientConfiguration.Builder) configField.get(builder);
            configBuilder.option(SdkClientOption.SDK_PIPELINE, pipeline);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to inject SDK_PIPELINE option into builder", e);
        }
    }

    private static Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new RuntimeException("Field '" + fieldName + "' not found in hierarchy of " + clazz.getName());
    }

    private static Map<String, AttributeValue> keyMap() {
        Map<String, AttributeValue> key = new HashMap<String, AttributeValue>();
        key.put("pk", AttributeValue.fromS("benchmark-key"));
        key.put("sk", AttributeValue.fromN("100"));
        return key;
    }

    private static Map<String, AttributeValue> itemMap() {
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put("pk", AttributeValue.fromS("benchmark-key"));
        item.put("sk", AttributeValue.fromN("100"));
        item.put("stringField", AttributeValue.fromS("test-value"));
        item.put("numberField", AttributeValue.fromN("123.456"));
        item.put("binaryField", AttributeValue.fromB(SdkBytes.fromUtf8String("hello world")));
        item.put("stringSetField", AttributeValue.builder().ss("value1", "value2", "value3").build());
        item.put("numberSetField", AttributeValue.builder().ns("1.1", "2.2", "3.3").build());
        item.put("boolField", AttributeValue.fromBool(false));
        item.put("nullField", AttributeValue.builder().nul(true).build());
        Map<String, AttributeValue> deep = new HashMap<String, AttributeValue>();
        deep.put("level2", AttributeValue.fromN("999"));
        Map<String, AttributeValue> nested = new HashMap<String, AttributeValue>();
        nested.put("nested", AttributeValue.fromS("nested-value"));
        nested.put("deepNested", AttributeValue.fromM(deep));
        item.put("mapField", AttributeValue.fromM(nested));
        item.put("listField", AttributeValue.builder().l(
            AttributeValue.fromS("item1"),
            AttributeValue.fromN("42"),
            AttributeValue.fromBool(true),
            AttributeValue.builder().nul(true).build()).build());
        return item;
    }

    private static Server createServer(int port, byte[] responseBody) {
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.setConnectors(new Connector[] {connector});

        ServletContextHandler context = new ServletContextHandler(server, "/",
            ServletContextHandler.SESSIONS);
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

    // ========================================================================
    // Inner classes
    // ========================================================================

    /**
     * A no-op pipeline that always returns {@code supportsOperation() = false}. When set as the
     * explicit {@code SDK_PIPELINE} option, it forces the generated client to use the standard v2
     * handler path for all operations, bypassing any SPI-discovered pipeline on the classpath.
     */
    static final class NoOpPipeline implements SdkPipeline {
        static final NoOpPipeline INSTANCE = new NoOpPipeline();

        private NoOpPipeline() {
        }

        @Override
        public <InputT extends SdkRequest, OutputT extends SdkResponse> OutputT execute(
            ClientExecutionParams<InputT, OutputT> executionParams,
            SdkClientConfiguration clientConfiguration) {
            throw new UnsupportedOperationException(
                "NoOpPipeline does not execute operations. supportsOperation() should prevent this call.");
        }

        @Override
        public <InputT extends SdkRequest, OutputT extends SdkResponse> boolean supportsOperation(
            ClientExecutionParams<InputT, OutputT> executionParams) {
            return false;
        }

        @Override
        public void close() {
            // no-op
        }
    }

    /**
     * Minimal servlet that returns a canned DynamoDB JSON response for every request.
     * Mimics the DynamoDB service for benchmark purposes.
     */
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
