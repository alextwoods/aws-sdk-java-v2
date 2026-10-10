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

package software.amazon.awssdk.benchmark.dynamodb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.apache5.Apache5AsyncHttpClient;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.identity.providers.StaticCredentialsProvider;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.pipeline.ClientRuntime;
import software.amazon.awssdk.pipeline.ExecutionInterceptor;
import software.amazon.awssdk.pipeline.HookContext;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

/**
 * Profiled hot-path assertions (Req 20.21, 20.22) on the generated V3 DynamoDB clients (awsJson1.0), sync and async,
 * over a real transport (Apache 5) against the in-process DynamoDB mock service, after warmup. Deterministic: counts
 * and per-thread allocated bytes, each with a positive control, never timing.
 *
 * <ul>
 *     <li>Req 20.21: a registered interceptor that implements no hook is an inactive extension. The sync caller thread
 *     (which runs the whole sync call over Apache 5) allocates, per call, less than one minimal object (16 bytes)
 *     more with it than without any interceptor; the positive control, an interceptor implementing one hook, is
 *     measured to allocate at least one object more per call, so the measurement can see a single object.</li>
 *     <li>Req 20.22: after warmup, calls construct no execution plan (the client runtime's plan cache size is
 *     constant and larger than one: every plan is built at client construction). Reflective member access, runtime
 *     operation lookup and model loading on the call path are not detected dynamically here; they are excluded
 *     statically by {@code PipelineArchitectureTest} and {@code ForbiddenGenerationPathsTest} (bytecode
 *     reachability). A JVM-wide loaded-class count is not asserted: it also counts the in-process mock service and
 *     JDK background threads, so it cannot be attributed to the call path.</li>
 * </ul>
 */
class GeneratedClientHotPathTest {

    private static final int WARMUP = 3_000;
    private static final int CALLS = 2_000;
    private static final int ROUNDS = 5;
    /** The smallest Java object on a 64-bit HotSpot heap. */
    private static final double ONE_OBJECT = 16;

    private static final com.sun.management.ThreadMXBean THREADS =
        (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    private static final PutItemRequest REQUEST = PutItemRequest.builder().tableName(DynamoDbItems.TABLE_NAME)
                                                                .item(DynamoDbItems.smallItemAllKinds()).build();

    private static MockDdbServer server;
    private static URI endpoint;

    @BeforeAll
    static void start() throws Exception {
        server = MockDdbServer.start(0, 4, MockDdbServer.cannedResponses());
        endpoint = URI.create("http://127.0.0.1:" + server.port());
    }

    @AfterAll
    static void stop() {
        server.stop();
    }

    static DynamoDbClient sync(ExecutionInterceptor interceptor) {
        var b = DynamoDbClient.builder().region(Region.US_EAST_1).credentialsProvider(credentials())
                              .endpointOverride(endpoint).httpClient(Apache5HttpClient.builder().build());
        if (interceptor != null) {
            b.addExecutionInterceptor(interceptor);
        }
        return b.build();
    }

    static DynamoDbAsyncClient async() {
        return DynamoDbAsyncClient.builder().region(Region.US_EAST_1).credentialsProvider(credentials())
                                  .endpointOverride(endpoint).httpClient(Apache5AsyncHttpClient.builder().build())
                                  .build();
    }

    static StaticCredentialsProvider credentials() {
        return StaticCredentialsProvider.create(AwsCredentialsIdentity.create("hot-path-akid", "hot-path-secret"));
    }

    /** Allocated bytes per call on this thread, the minimum over interleaved rounds (noise only adds bytes). */
    static double[] bytesPerCall(Runnable... calls) {
        double[] min = new double[calls.length];
        java.util.Arrays.fill(min, Double.MAX_VALUE);
        long thread = Thread.currentThread().getId();
        for (Runnable c : calls) {
            for (int i = 0; i < WARMUP; i++) {
                c.run();
            }
        }
        for (int r = 0; r < ROUNDS; r++) {
            for (int k = 0; k < calls.length; k++) {
                long before = THREADS.getThreadAllocatedBytes(thread);
                for (int i = 0; i < CALLS; i++) {
                    calls[k].run();
                }
                long after = THREADS.getThreadAllocatedBytes(thread);
                min[k] = Math.min(min[k], (after - before) / (double) CALLS);
            }
        }
        return min;
    }

    @Test
    void aRegisteredInterceptorWithoutHooksAllocatesNothingPerSyncCall() {
        ExecutionInterceptor inactive = new ExecutionInterceptor() {
        };
        ExecutionInterceptor active = new ExecutionInterceptor() {
            @Override
            public void afterExecution(HookContext.AfterExecution context) {
            }
        };
        try (DynamoDbClient none = sync(null); DynamoDbClient withInactive = sync(inactive);
             DynamoDbClient withActive = sync(active)) {
            double[] b = bytesPerCall(() -> none.putItem(REQUEST), () -> withInactive.putItem(REQUEST),
                                      () -> withActive.putItem(REQUEST));
            String detail = String.format(java.util.Locale.ROOT, "bytes/call: none %.1f, inactive %.1f, active %.1f",
                                          b[0], b[1], b[2]);
            assertTrue(b[2] - b[0] >= ONE_OBJECT, "positive control: an active hook allocates; " + detail);
            assertTrue(b[1] - b[0] < ONE_OBJECT, "an inactive interceptor allocates per call; " + detail);
        }
    }

    @Test
    void steadyStateSyncCallsBuildNoPlan() throws Exception {
        try (DynamoDbClient client = sync(null)) {
            assertSteadyState(() -> runtime(client), () -> client.putItem(REQUEST));
        }
    }

    @Test
    void steadyStateAsyncCallsBuildNoPlan() throws Exception {
        try (DynamoDbAsyncClient client = async()) {
            assertSteadyState(() -> runtime(client), () -> {
                try {
                    client.putItem(REQUEST).get(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            });
        }
    }

    private static void assertSteadyState(Supplier<ClientRuntime> runtime, Runnable call) throws Exception {
        for (int i = 0; i < WARMUP; i++) {
            call.run();
        }
        int plans = builtPlans(runtime.get());
        assertTrue(plans > 1, "the generated client builds its operations' plans at construction: " + plans);
        for (int i = 0; i < CALLS; i++) {
            call.run();
        }
        assertEquals(plans, builtPlans(runtime.get()), "plans built during steady-state calls");
    }

    /** The client's runtime, through its call engine (test-side reflection; the call path has none). */
    private static ClientRuntime runtime(Object client) {
        try {
            Field engine = client.getClass().getDeclaredField("engine");
            engine.setAccessible(true);
            Object e = engine.get(client);
            return (ClientRuntime) e.getClass().getMethod("runtime").invoke(e);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static int builtPlans(ClientRuntime runtime) throws Exception {
        Method m = ClientRuntime.class.getDeclaredMethod("builtPlans");
        m.setAccessible(true);
        return (int) m.invoke(runtime);
    }
}
