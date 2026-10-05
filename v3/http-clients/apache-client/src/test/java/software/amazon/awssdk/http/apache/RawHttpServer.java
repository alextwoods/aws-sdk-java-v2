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

package software.amazon.awssdk.http.apache;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A byte-exact HTTP/1.1 test server on a plain {@link ServerSocket}: it records every request head as sent (request
 * line and header fields in wire order) with its decoded body and the id of the connection it arrived on, and lets
 * each test write the raw response, including stalls released by {@link #release()} or {@link #close()}.
 */
final class RawHttpServer implements AutoCloseable {
    /** A request as received. */
    record Request(int connection, String requestLine, List<String[]> headers, byte[] body) {
        String method() {
            return requestLine.substring(0, requestLine.indexOf(' '));
        }

        String target() {
            return requestLine.substring(requestLine.indexOf(' ') + 1, requestLine.lastIndexOf(' '));
        }

        List<String> headerNames() {
            return headers.stream().map(h -> h[0]).toList();
        }

        List<String> values(String name) {
            return headers.stream().filter(h -> h[0].equalsIgnoreCase(name)).map(h -> h[1]).toList();
        }
    }

    /** Writes the complete raw response to {@code request}. */
    @FunctionalInterface
    interface Handler {
        void respond(Request request, OutputStream out, RawHttpServer server) throws IOException, InterruptedException;
    }

    final BlockingQueue<Request> requests = new LinkedBlockingQueue<>();
    private final ServerSocket serverSocket;
    private final Handler handler;
    private final CountDownLatch released = new CountDownLatch(1);
    private final AtomicInteger connections = new AtomicInteger();
    private final Set<Socket> sockets = ConcurrentHashMap.newKeySet();

    RawHttpServer(Handler handler) throws IOException {
        this.handler = handler;
        this.serverSocket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        Thread acceptor = new Thread(this::accept, "raw-http-server-accept");
        acceptor.setDaemon(true);
        acceptor.start();
    }

    int port() {
        return serverSocket.getLocalPort();
    }

    int connectionCount() {
        return connections.get();
    }

    Request take() throws InterruptedException {
        Request r = requests.poll(10, TimeUnit.SECONDS);
        if (r == null) {
            throw new AssertionError("No request received");
        }
        return r;
    }

    /** Blocks a stalling handler until the test releases it or the server closes. */
    void awaitRelease() throws InterruptedException {
        released.await(30, TimeUnit.SECONDS);
    }

    void release() {
        released.countDown();
    }

    private void accept() {
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                sockets.add(socket);
                int id = connections.incrementAndGet();
                Thread t = new Thread(() -> serve(socket, id), "raw-http-server-" + id);
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                return;
            }
        }
    }

    private void serve(Socket socket, int id) {
        try (socket) {
            InputStream in = new BufferedInputStream(socket.getInputStream());
            OutputStream out = socket.getOutputStream();
            while (true) {
                String requestLine = line(in);
                if (requestLine == null || requestLine.isEmpty()) {
                    return;
                }
                List<String[]> headers = new ArrayList<>();
                String line;
                while (!(line = line(in)).isEmpty()) {
                    int colon = line.indexOf(':');
                    headers.add(new String[] {line.substring(0, colon), line.substring(colon + 1).trim()});
                }
                Request request = new Request(id, requestLine, headers, body(in, headers));
                requests.add(request);
                handler.respond(request, out, this);
                out.flush();
            }
        } catch (IOException | InterruptedException | RuntimeException e) {
            // Connection closed by the client or the test.
        } finally {
            sockets.remove(socket);
        }
    }

    private static byte[] body(InputStream in, List<String[]> headers) throws IOException {
        String length = null;
        boolean chunked = false;
        for (String[] h : headers) {
            if (h[0].equalsIgnoreCase("Content-Length")) {
                length = h[1];
            } else if (h[0].equalsIgnoreCase("Transfer-Encoding") && h[1].equalsIgnoreCase("chunked")) {
                chunked = true;
            } else if (h[0].equalsIgnoreCase("Expect") && h[1].equalsIgnoreCase("100-continue")) {
                continue;
            }
        }
        if (chunked) {
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            while (true) {
                int size = Integer.parseInt(line(in).split(";")[0].trim(), 16);
                if (size == 0) {
                    while (!line(in).isEmpty()) {
                        // trailers
                    }
                    return body.toByteArray();
                }
                body.write(in.readNBytes(size));
                line(in);
            }
        }
        return length == null ? new byte[0] : in.readNBytes(Integer.parseInt(length));
    }

    private static String line(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) >= 0) {
            if (b == '\n') {
                byte[] bytes = buf.toByteArray();
                int n = bytes.length > 0 && bytes[bytes.length - 1] == '\r' ? bytes.length - 1 : bytes.length;
                return new String(bytes, 0, n, StandardCharsets.ISO_8859_1);
            }
            buf.write(b);
        }
        if (buf.size() == 0) {
            return null;
        }
        throw new IOException("Truncated line");
    }

    /** Writes a complete response with {@code body} and the given header lines ({@code "Name: value"}). */
    static void respond(OutputStream out, int status, byte[] body, String... headerLines) throws IOException {
        StringBuilder head = new StringBuilder("HTTP/1.1 ").append(status).append(" Status\r\n");
        for (String h : headerLines) {
            head.append(h).append("\r\n");
        }
        head.append("Content-Length: ").append(body.length).append("\r\n\r\n");
        out.write(head.toString().getBytes(StandardCharsets.ISO_8859_1));
        out.write(body);
        out.flush();
    }

    static void ok(OutputStream out, String body) throws IOException {
        respond(out, 200, body.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void close() throws IOException {
        released.countDown();
        serverSocket.close();
        for (Socket s : sockets) {
            s.close();
        }
    }
}
