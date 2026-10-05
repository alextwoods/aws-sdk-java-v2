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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpClientOption;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.SdkHttpRequest;

/**
 * Capability gating and shared helpers of the sync and async suites.
 *
 * <p>Gating model: a check that depends on a capability calls {@link #require(TransportUnderTest,
 * HttpClientCapabilities, HttpClientOption)} (or {@link #requireNo(TransportUnderTest, Deviation)}) first. An
 * undeclared capability aborts the check through a JUnit assumption, so the check is reported as skipped with the
 * reason (never as a silent pass). Universal checks (terminal uniqueness, demand, ownership, cancellation within the
 * declared cancellation timeout) call neither and always run.
 *
 * <p>The declared buffer bound ({@link HttpClientCapabilities#maxBufferedBytesPerExchange()}) is not checked here: from
 * a loopback server it can only be observed together with kernel socket buffering, which the OS autotunes and which
 * varies with load. Each transport checks its bound directly against its own internal buffers in its module tests.
 */
final class TckSupport {

    /** Upper bound for anything a check waits on that is not a declared timeout. */
    static final Duration WAIT = Duration.ofSeconds(10);
    /** The short read timeout used by timeout checks. */
    static final Duration SHORT_TIMEOUT = Duration.ofMillis(400);
    /** Allowance for scheduling on top of a declared timeout. */
    static final Duration SLACK = Duration.ofSeconds(2);

    private TckSupport() {
    }

    static void require(TransportUnderTest<?> transport, HttpClientCapabilities capabilities,
                        HttpClientOption option) {
        assumeTrue(capabilities.supports(option),
                   () -> transport.name() + " does not declare " + option + "; check not applicable");
    }

    static void requireNo(TransportUnderTest<?> transport, Deviation deviation) {
        assumeFalse(transport.documents(deviation),
                    () -> transport.name() + " documents deviation " + deviation + ": " + deviation.reason());
    }

    static void requireTestTrust(TransportUnderTest<?> transport, HttpClientCapabilities capabilities) {
        assumeTrue(capabilities.supports(HttpClientOption.TLS_TRUST_MANAGERS)
                   || transport.documents(Deviation.TEST_TRUST_BY_PEM),
                   () -> transport.name() + " declares no way to trust a test certificate; check not applicable");
    }

    /** Skips unless {@code localhost} resolves to more than one address (the TCK servers bind only 127.0.0.1). */
    static void requireMultiAddressLocalhost() throws IOException {
        int addresses = InetAddress.getAllByName("localhost").length;
        assumeTrue(addresses > 1, "localhost resolves to a single address on this host; check not applicable");
    }

    static SdkHttpRequest.Builder get(String scheme, int port, String path) {
        return SdkHttpRequest.builder().method("GET").scheme(scheme).host("localhost").port(port).encodedPath(path);
    }

    static SdkHttpRequest.Builder get(TckServer server, String path) {
        return get("http", server.port(), path);
    }

    static HttpTransportException transportFailure(Throwable failure, HttpFailureCategory expected) {
        Throwable t = failure;
        while (!(t instanceof HttpTransportException) && t != null && t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        HttpTransportException e = assertInstanceOf(HttpTransportException.class, t,
                                                    () -> "not a transport failure: " + failure);
        assertEquals(expected, e.category(), () -> "category of " + e + " (phase " + e.phase() + "), causes "
                                                   + causes(e));
        return e;
    }

    private static String causes(Throwable t) {
        StringBuilder out = new StringBuilder();
        for (Throwable c = t.getCause(); c != null && out.length() < 2000; c = c.getCause()) {
            out.append(" <- ").append(c);
            for (Throwable s : c.getSuppressed()) {
                out.append(" [suppressed ").append(s).append(']');
            }
        }
        return out.toString();
    }

    static byte[] bytes(int size) {
        byte[] b = new byte[size];
        for (int i = 0; i < size; i++) {
            b[i] = (byte) ('a' + i % 26);
        }
        return b;
    }

    static String utf8(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /** A port with nothing listening on it. */
    static int refusedPort() throws IOException {
        try (ServerSocket s = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return s.getLocalPort();
        }
    }

    static void await(CountDownLatch latch) throws InterruptedException {
        if (!latch.await(WAIT.toMillis(), TimeUnit.MILLISECONDS)) {
            throw new AssertionError("timed out waiting for the server");
        }
    }

    static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(WAIT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** A handler that answers every request with {@code body} and keeps the connection. */
    static TckServer.Handler serving(String body) {
        return ex -> {
            ex.body();
            ex.out().write(TckServer.response(200, body));
            return true;
        };
    }
}
