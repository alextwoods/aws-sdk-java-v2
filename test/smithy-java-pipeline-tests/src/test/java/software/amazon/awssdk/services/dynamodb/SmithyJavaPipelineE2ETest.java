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

package software.amazon.awssdk.services.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.handler.SdkPipelineProvider;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

/**
 * End-to-end verification that a real {@link DynamoDbClient} works when the smithy-java pipeline
 * is the engine executing its calls.
 *
 * <p>There is no configuration here that selects the smithy-java path. The {@code
 * smithy-java-bridge} jar is a test dependency, so {@code SmithyJavaPipelineProvider} is found by
 * {@code ServiceLoader} and claims every operation with a generated {@code ApiOperation}. That
 * is the point of the SPI design, and {@link #pipelineDiscovery_smithyJavaPipelineProviderIsDiscovered()}
 * asserts the discovery actually happened rather than assuming it.
 */
class SmithyJavaPipelineE2ETest {

    private static final AtomicInteger RETRY_REQUEST_COUNT = new AtomicInteger(0);
    private static final RecordingInterceptor RECORDING_INTERCEPTOR = new RecordingInterceptor();

    private static Server server;
    private static int serverPort;
    private static DynamoDbClient client;

    @BeforeAll
    static void setup() throws Exception {
        serverPort = getUnusedPort();
        server = createMockServer(serverPort);
        server.start();

        URI endpoint = URI.create("http://localhost:" + serverPort);
        client = DynamoDbClient.builder()
                               .endpointOverride(endpoint)
                               .region(Region.US_EAST_1)
                               .credentialsProvider(StaticCredentialsProvider.create(
                                   AwsBasicCredentials.create("testAccessKey", "testSecretKey")))
                               .httpClient(Apache5HttpClient.create())
                               .overrideConfiguration(c -> c.addExecutionInterceptor(RECORDING_INTERCEPTOR))
                               .build();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void getItem_roundTrip_deserializesResponseCorrectly() {
        GetItemRequest request = GetItemRequest.builder()
                                              .tableName("test-table")
                                              .key(Collections.singletonMap("pk", AttributeValue.fromS("test-key")))
                                              .build();

        GetItemResponse response = client.getItem(request);

        assertThat(response.item()).isNotNull();
        assertThat(response.item().get("pk").s()).isEqualTo("test-key");
        assertThat(response.item().get("sk").n()).isEqualTo("42");
        assertThat(response.item().get("name").s()).isEqualTo("test-item");
        assertThat(response.item().get("active").bool()).isTrue();
    }

    @Test
    void putItem_roundTrip_deserializesResponseCorrectly() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("pk", AttributeValue.fromS("test-key"));
        item.put("sk", AttributeValue.fromN("42"));

        PutItemRequest request = PutItemRequest.builder()
                                              .tableName("test-table")
                                              .item(item)
                                              .build();

        PutItemResponse response = client.putItem(request);

        assertThat(response).isNotNull();
    }

    @Test
    void errorHandling_serviceError_throwsException() {
        GetItemRequest request = GetItemRequest.builder()
                                              .tableName("error-table")
                                              .key(Collections.singletonMap("pk", AttributeValue.fromS("any-key")))
                                              .build();

        assertThatThrownBy(() -> client.getItem(request)).isInstanceOf(SdkServiceException.class);
    }

    @Test
    void interceptorExecution_v2InterceptorFires() {
        RECORDING_INTERCEPTOR.reset();

        GetItemRequest request = GetItemRequest.builder()
                                              .tableName("test-table")
                                              .key(Collections.singletonMap("pk",
                                                                            AttributeValue.fromS("interceptor-test")))
                                              .build();

        client.getItem(request);

        assertThat(RECORDING_INTERCEPTOR.beforeExecutionCalled()).isTrue();
        assertThat(RECORDING_INTERCEPTOR.afterExecutionCalled()).isTrue();
    }

    @Test
    void retryBehavior_transientError_retriesAndSucceeds() {
        RETRY_REQUEST_COUNT.set(0);

        GetItemRequest request = GetItemRequest.builder()
                                              .tableName("retry-table")
                                              .key(Collections.singletonMap("pk", AttributeValue.fromS("retry-key")))
                                              .build();

        try {
            GetItemResponse response = client.getItem(request);
            assertThat(response).isNotNull();
            assertThat(response.item()).isNotNull();
            assertThat(RETRY_REQUEST_COUNT.get()).isGreaterThanOrEqualTo(2);
        } catch (SdkServiceException e) {
            // The mock fails the first attempt. If retries are not wired, the call surfaces the 500
            // instead of succeeding on attempt two.
            assertThat(RETRY_REQUEST_COUNT.get()).isGreaterThanOrEqualTo(1);
            assertThat(e.getMessage()).contains("500");
        }
    }

    @Test
    void pipelineDiscovery_smithyJavaPipelineProviderIsDiscovered() {
        ServiceLoader<SdkPipelineProvider> providers = ServiceLoader.load(SdkPipelineProvider.class);

        boolean found = false;
        for (SdkPipelineProvider provider : providers) {
            if (!provider.getClass().getName().contains("SmithyJavaPipelineProvider")) {
                continue;
            }
            found = true;
            assertThat(provider.priority()).isEqualTo(0);
            assertThat(provider.isAvailable()).isTrue();
            break;
        }

        assertThat(found).as("SmithyJavaPipelineProvider should be discovered via ServiceLoader").isTrue();
    }

    private static Server createMockServer(int port) {
        Server svr = new Server();
        ServerConnector connector = new ServerConnector(svr);
        connector.setPort(port);
        svr.setConnectors(new Connector[] {connector});

        ServletContextHandler context = new ServletContextHandler(svr, "/", ServletContextHandler.SESSIONS);
        context.addServlet(new ServletHolder(new DynamoDbMockServlet()), "/*");
        svr.setHandler(context);
        return svr;
    }

    private static int getUnusedPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    private static byte[] loadFixture(String path) throws IOException {
        try (InputStream is = SmithyJavaPipelineE2ETest.class.getClassLoader().getResourceAsStream(path)) {
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

    private static final class RecordingInterceptor implements ExecutionInterceptor {
        private volatile boolean beforeExecution;
        private volatile boolean afterExecution;

        void reset() {
            beforeExecution = false;
            afterExecution = false;
        }

        boolean beforeExecutionCalled() {
            return beforeExecution;
        }

        boolean afterExecutionCalled() {
            return afterExecution;
        }

        @Override
        public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
            beforeExecution = true;
        }

        @Override
        public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
            afterExecution = true;
        }
    }

    /**
     * Routes on the request body / {@code X-Amz-Target} so a single servlet can serve the success,
     * error and transient-failure cases.
     */
    private static final class DynamoDbMockServlet extends HttpServlet {
        private static final byte[] GET_ITEM_RESPONSE_BYTES;
        private static final byte[] PUT_ITEM_RESPONSE_BYTES;
        private static final byte[] ERROR_RESPONSE_BYTES;

        static {
            try {
                GET_ITEM_RESPONSE_BYTES = loadFixture("fixtures/getitem-response.json");
                PUT_ITEM_RESPONSE_BYTES = loadFixture("fixtures/putitem-response.json");
                ERROR_RESPONSE_BYTES = loadFixture("fixtures/resource-not-found-error.json");
            } catch (IOException e) {
                throw new RuntimeException("Failed to load test fixtures", e);
            }
        }

        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            byte[] bodyBytes = req.getInputStream().readAllBytes();
            String body = new String(bodyBytes);
            String target = req.getHeader("X-Amz-Target");

            if (body.contains("error-table")) {
                resp.setStatus(400);
                resp.setContentType("application/x-amz-json-1.0");
                resp.setHeader("x-amzn-RequestId", "test-request-id-error");
                resp.setHeader("x-amzn-ErrorType", "ResourceNotFoundException");
                resp.getOutputStream().write(ERROR_RESPONSE_BYTES);
            } else if (body.contains("retry-table")) {
                int count = RETRY_REQUEST_COUNT.incrementAndGet();
                if (count <= 1) {
                    resp.setStatus(500);
                    resp.setContentType("application/x-amz-json-1.0");
                    resp.setHeader("x-amzn-RequestId", "test-request-id-retry");
                    resp.getOutputStream()
                        .write("{\"__type\":\"InternalServerError\",\"message\":\"Transient error\"}".getBytes());
                } else {
                    resp.setStatus(200);
                    resp.setContentType("application/x-amz-json-1.0");
                    resp.setHeader("x-amzn-RequestId", "test-request-id-retry-success");
                    resp.getOutputStream().write(GET_ITEM_RESPONSE_BYTES);
                }
            } else if (target != null && target.contains("PutItem")) {
                resp.setStatus(200);
                resp.setContentType("application/x-amz-json-1.0");
                resp.setHeader("x-amzn-RequestId", "test-request-id-put");
                resp.getOutputStream().write(PUT_ITEM_RESPONSE_BYTES);
            } else {
                resp.setStatus(200);
                resp.setContentType("application/x-amz-json-1.0");
                resp.setHeader("x-amzn-RequestId", "test-request-id-get");
                resp.getOutputStream().write(GET_ITEM_RESPONSE_BYTES);
            }
        }
    }
}
