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

package software.amazon.awssdk.http.tck;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.http.tck.TckSupport.get;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.test.concurrency.ConcurrencyPlan;
import software.amazon.awssdk.test.concurrency.ConcurrentRun;
import software.amazon.awssdk.test.concurrency.OverlapBarrier;

/**
 * Release concurrency suite of every HTTP implementation and mode (task 18.7; Requirements 12.x via 19.9, 19.10),
 * over the TCK's transports ({@code *TckTest.TRANSPORT}) and server: one shared client per transport,
 * {@value #SIMULTANEOUS} simultaneous exchanges, {@value #TOTAL} in total, a bounded {@value #TIMEOUT_SECONDS}-second
 * run per transport. An {@link OverlapBarrier} of {@value #SIMULTANEOUS} parties in the server holds every request
 * (after its body was read) until all simultaneous exchanges of the round are open at once, so the transport has that
 * many connections, request bodies and responses in flight. Every exchange sends a distinct body of a distinct length
 * to a distinct target; the server echoes both, and each caller must receive exactly its own (no cross-talk between
 * pooled connections or buffers). The async exchanges must also keep the TCK's signal rules (one terminal signal, no
 * demand violations, serial handler signals).
 *
 * <p>The TCK's own {@code concurrency_overlappingExchanges_allComplete} checks (two by three) stay in the per-transport
 * suites; this suite is the larger documented release run.
 */
class TransportConcurrencyTest {
    static final int SIMULTANEOUS = 8;
    static final int TOTAL = 64;
    static final int TIMEOUT_SECONDS = 60;

    static Stream<Arguments> syncTransports() {
        return Stream.of(Apache4SyncTckTest.TRANSPORT, Apache5SyncTckTest.TRANSPORT, UrlConnectionSyncTckTest.TRANSPORT,
                         CrtSyncTckTest.TRANSPORT)
                     .map(t -> Arguments.of(t.name(), t));
    }

    static Stream<Arguments> asyncTransports() {
        return Stream.of(Apache5AsyncTckTest.TRANSPORT, NettyAsyncTckTest.TRANSPORT, CrtAsyncTckTest.TRANSPORT)
                     .map(t -> Arguments.of(t.name(), t));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("syncTransports")
    @Timeout(value = TIMEOUT_SECONDS + 10, unit = TimeUnit.SECONDS)
    void sharedSyncClient_overlappingExchanges_eachReceivesItsOwnResponse(
        String name, TransportUnderTest<SdkSyncHttpClient> transport) throws Exception {
        ConcurrencyPlan plan = plan(name);
        OverlapBarrier overlap = plan.overlapBarrier();
        Queue<Throwable> serverFailures = new ConcurrentLinkedQueue<>();
        AtomicInteger requests = new AtomicInteger();
        try (TckServer server = TckServer.plain(echo(overlap, serverFailures, requests));
             SdkSyncHttpClient client = transport.create(ClientSettings.defaults())) {
            List<byte[]> responses = ConcurrentRun.run(plan, call -> {
                try (SyncHttpExchange exchange = client.prepare(request(server, call), HttpExecutionOptions.defaults())) {
                    SdkHttpResponse response = exchange.execute();
                    assertEquals(200, response.statusCode());
                    assertEquals(target(call), response.headers().firstValue("X-Tck-Call").orElse(null));
                    try (InputStream in = response.body().streamingBody().openStream()) {
                        return in.readAllBytes();
                    }
                }
            }, overlap::breakBarrier);
            assertResponses(plan, responses, server, overlap, serverFailures, requests);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("asyncTransports")
    @Timeout(value = TIMEOUT_SECONDS + 10, unit = TimeUnit.SECONDS)
    void sharedAsyncClient_overlappingExchanges_eachReceivesItsOwnResponse(
        String name, TransportUnderTest<SdkAsyncHttpClient> transport) throws Exception {
        ConcurrencyPlan plan = plan(name);
        OverlapBarrier overlap = plan.overlapBarrier();
        Queue<Throwable> serverFailures = new ConcurrentLinkedQueue<>();
        AtomicInteger requests = new AtomicInteger();
        try (TckServer server = TckServer.plain(echo(overlap, serverFailures, requests));
             SdkAsyncHttpClient client = transport.create(ClientSettings.defaults())) {
            // Nothing blocks in the completion callbacks, which may run on the transport's event loop.
            List<AsyncProbes.Recorder> recorders = ConcurrentRun.runAsync(plan, call -> {
                AsyncProbes.Recorder recorder = new AsyncProbes.Recorder(true);
                return client.execute(request(server, call), recorder, HttpExecutionOptions.defaults()).completion()
                             .thenApply(done -> recorder);
            }, overlap::breakBarrier);

            List<String> violations = new ArrayList<>();
            List<byte[]> responses = new ArrayList<>();
            for (int i = 0; i < TOTAL; i++) {
                ConcurrentRun.Call call = callOf(i);
                AsyncProbes.Recorder recorder = recorders.get(i);
                SdkHttpResponse head = recorder.headers.join();
                if (head.statusCode() != 200
                    || !target(call).equals(head.headers().firstValue("X-Tck-Call").orElse(null))) {
                    violations.add(call + ": response head " + head);
                }
                responses.add(settled(call, recorder, violations));
            }
            assertResponses(plan, responses, server, overlap, serverFailures, requests);
            assertEquals(List.of(), violations, "async signal rules");
        }
    }

    private static ConcurrencyPlan plan(String transport) {
        return ConcurrencyPlan.of("transport-" + transport, SIMULTANEOUS, TOTAL, Duration.ofSeconds(TIMEOUT_SECONDS));
    }

    /** Reads the body, waits at the overlap barrier, then echoes the target (header and body) and the request body. */
    private static TckServer.Handler echo(OverlapBarrier overlap, Queue<Throwable> failures, AtomicInteger requests) {
        return ex -> {
            byte[] body = ex.body();
            requests.incrementAndGet();
            try {
                overlap.arrive();
            } catch (AssertionError e) {
                failures.add(e);
                return false;
            }
            byte[] target = (ex.head().target() + "\n").getBytes(StandardCharsets.UTF_8);
            byte[] echo = new byte[target.length + body.length];
            System.arraycopy(target, 0, echo, 0, target.length);
            System.arraycopy(body, 0, echo, target.length, body.length);
            ex.out().write(TckServer.response(200, echo, "X-Tck-Call: " + ex.head().target()));
            return true;
        };
    }

    private static SdkHttpRequest request(TckServer server, ConcurrentRun.Call call) {
        return get(server, target(call)).method("PUT").body(HttpBody.fromBytes(body(call))).build();
    }

    private static String target(ConcurrentRun.Call call) {
        return "/lane-" + call.lane() + "/call-" + call.index();
    }

    /** A distinct body of a distinct length (up to about 16 KiB) per call. */
    private static byte[] body(ConcurrentRun.Call call) {
        String unit = "call-" + call.index() + ";";
        return unit.repeat(1 + call.index() * 37 % 1500).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] expected(ConcurrentRun.Call call) {
        byte[] target = (target(call) + "\n").getBytes(StandardCharsets.UTF_8);
        byte[] body = body(call);
        byte[] all = new byte[target.length + body.length];
        System.arraycopy(target, 0, all, 0, target.length);
        System.arraycopy(body, 0, all, target.length, body.length);
        return all;
    }

    private static ConcurrentRun.Call callOf(int index) {
        return new ConcurrentRun.Call(index % SIMULTANEOUS, index / SIMULTANEOUS, index);
    }

    /** The response body after the exchange's single terminal signal; records protocol violations. */
    private static byte[] settled(ConcurrentRun.Call call, AsyncProbes.Recorder recorder, List<String> violations)
        throws InterruptedException {
        long deadline = System.nanoTime() + TckSupport.WAIT.toNanos();
        while (recorder.terminals.get() == 0 && System.nanoTime() < deadline) {
            Thread.sleep(1);
        }
        if (recorder.terminals.get() != 1) {
            violations.add(call + ": " + recorder.terminals.get() + " terminal signals");
        }
        recorder.violations.forEach(v -> violations.add(call + ": " + v));
        return recorder.body.bytes();
    }

    private static void assertResponses(ConcurrencyPlan plan, List<byte[]> responses, TckServer server,
                                        OverlapBarrier overlap, Queue<Throwable> serverFailures,
                                        AtomicInteger requests) throws IOException {
        assertEquals(List.of(), List.copyOf(serverFailures), "server-side overlap failures");
        for (int i = 0; i < TOTAL; i++) {
            ConcurrentRun.Call call = callOf(i);
            assertArrayEquals(expected(call), responses.get(i), "the response of " + call);
        }
        assertEquals(TOTAL, requests.get(), "each exchange reached the server exactly once");
        assertEquals(plan.rounds(), overlap.trips(), "every round had all exchanges open together");
        assertEquals(SIMULTANEOUS, overlap.maxInside());
        assertTrue(server.connections() >= SIMULTANEOUS,
                   "simultaneous HTTP/1.1 exchanges need their own connections: " + server.connections());
    }
}
