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
package software.amazon.awssdk.test.boundedmemory;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/**
 * A MediaStore Data object server on loopback HTTP/1.1 that never holds a payload: {@code PUT} streams the request
 * body through a {@link Payload.Verifier} (exact bytes, order and SHA-256), and {@code GET} streams the generated
 * payload of {@link #objectLength(long)} bytes. Each exchange's outcome (bytes moved, digest or I/O failure) is
 * published through {@link #nextUpload()} and {@link #nextDownload()}, and its bytes are counted into the gauges the
 * test installs.
 */
final class LoopbackObjectServer implements AutoCloseable {
    private final HttpServer server;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private volatile long objectLength;
    private volatile InFlightGauge uploadGauge = new InFlightGauge();
    private volatile InFlightGauge downloadGauge = new InFlightGauge();
    private volatile CompletableFuture<Outcome> upload = new CompletableFuture<>();
    private volatile CompletableFuture<Outcome> download = new CompletableFuture<>();
    /** What one exchange moved: the body bytes read or written, the SHA-256 of an upload, or the I/O failure. */
    record Outcome(long bytes, String sha256, Throwable failure) {
    }
    LoopbackObjectServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.setExecutor(executor);
        server.createContext("/", this::handle);
        server.start();
    }
    URI endpoint() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }
    void objectLength(long length) {
        this.objectLength = length;
    }
    /** Resets and returns the future outcome of the next upload; its received bytes are consumed from {@code gauge}. */
    CompletableFuture<Outcome> nextUpload(InFlightGauge gauge) {
        uploadGauge = gauge;
        upload = new CompletableFuture<>();
        return upload;
    }
    /** Resets and returns the future outcome of the next download; its sent bytes are produced into {@code gauge}. */
    CompletableFuture<Outcome> nextDownload(InFlightGauge gauge) {
        downloadGauge = gauge;
        download = new CompletableFuture<>();
        return download;
    }
    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if ("PUT".equals(exchange.getRequestMethod())) {
                receive(exchange);
            } else if ("GET".equals(exchange.getRequestMethod())) {
                send(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }
    private void receive(HttpExchange exchange) throws IOException {
        CompletableFuture<Outcome> outcome = upload;
        InFlightGauge gauge = uploadGauge;
        Payload.Verifier verifier = new Payload.Verifier();
        byte[] buffer = new byte[64 * 1024];
        try (InputStream body = exchange.getRequestBody()) {
            int n;
            while ((n = body.read(buffer)) > 0) {
                gauge.consumed(n);
                verifier.update(buffer, 0, n);
            }
        } catch (IOException | AssertionError e) {
            outcome.complete(new Outcome(verifier.position(), null, e));
            throw e instanceof IOException io ? io : new IOException(e);
        }
        String sha256 = verifier.sha256();
        byte[] json = ("{\"ETag\":\"etag-" + verifier.position() + "\",\"ContentSHA256\":\"" + sha256 + "\"}")
            .getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, json.length);
        exchange.getResponseBody().write(json);
        outcome.complete(new Outcome(verifier.position(), sha256, null));
    }
    private void send(HttpExchange exchange) throws IOException {
        CompletableFuture<Outcome> outcome = download;
        InFlightGauge gauge = downloadGauge;
        long length = objectLength;
        exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
        exchange.getResponseHeaders().set("ETag", "\"generated\"");
        exchange.sendResponseHeaders(200, length);
        byte[] buffer = new byte[64 * 1024];
        long position = 0;
        try {
            OutputStream body = exchange.getResponseBody();
            while (position < length) {
                int n = (int) Math.min(buffer.length, length - position);
                Payload.fill(position, buffer, 0, n);
                gauge.produced(n);
                body.write(buffer, 0, n);
                position += n;
            }
            body.flush();
        } catch (IOException e) {
            outcome.complete(new Outcome(position, null, e));
            throw e;
        }
        outcome.complete(new Outcome(position, null, null));
    }
    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }
}
