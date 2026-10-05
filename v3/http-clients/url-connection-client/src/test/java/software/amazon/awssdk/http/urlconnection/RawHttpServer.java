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

package software.amazon.awssdk.http.urlconnection;

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
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A minimal HTTP/1.1 server over a JDK {@link ServerSocket} that records the exact request bytes (request line and
 * header lines in wire order) and counts TCP connections, so tests can tell a reused connection from a discarded one.
 * Each connection is served on its own daemon thread by a scripted {@link Handler}.
 */
final class RawHttpServer implements AutoCloseable {
    /** A received request. {@code connection} is the 1-based index of the TCP connection it arrived on. */
    record Request(String requestLine, List<String> headerLines, byte[] body, int connection) {
        String header(String name) {
            for (String line : headerLines) {
                int colon = line.indexOf(':');
                if (line.substring(0, colon).equalsIgnoreCase(name)) {
                    return line.substring(colon + 1).trim();
                }
            }
            return null;
        }
    }

    /** Writes the raw response; returns false to close the connection afterwards. */
    @FunctionalInterface
    interface Handler {
        boolean handle(Request request, OutputStream out) throws IOException;
    }

    private final ServerSocket socket;
    private final Handler handler;
    private final AtomicInteger connections = new AtomicInteger();
    private final BlockingQueue<Request> requests = new LinkedBlockingQueue<>();
    private final List<Socket> open = new CopyOnWriteArrayList<>();
    private final AtomicInteger closedByClient = new AtomicInteger();

    RawHttpServer(Handler handler) throws IOException {
        this.handler = handler;
        this.socket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        Thread acceptor = new Thread(this::acceptLoop, "raw-http-accept");
        acceptor.setDaemon(true);
        acceptor.start();
    }

    /** A response with a fixed-length body. */
    static byte[] response(int status, String body, String... headers) {
        StringBuilder out = new StringBuilder("HTTP/1.1 " + status + " X\r\n");
        for (String header : headers) {
            out.append(header).append("\r\n");
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        out.append("Content-Length: ").append(bytes.length).append("\r\n\r\n");
        byte[] head = out.toString().getBytes(StandardCharsets.ISO_8859_1);
        byte[] all = new byte[head.length + bytes.length];
        System.arraycopy(head, 0, all, 0, head.length);
        System.arraycopy(bytes, 0, all, head.length, bytes.length);
        return all;
    }

    int port() {
        return socket.getLocalPort();
    }

    /** TCP connections accepted so far. */
    int connections() {
        return connections.get();
    }

    /** Connections the client closed (EOF or reset seen by the server). */
    int closedByClient() {
        return closedByClient.get();
    }

    Request take() throws InterruptedException {
        Request r = requests.poll(10, TimeUnit.SECONDS);
        if (r == null) {
            throw new AssertionError("no request received");
        }
        return r;
    }

    private void acceptLoop() {
        while (!socket.isClosed()) {
            try {
                Socket s = socket.accept();
                int id = connections.incrementAndGet();
                open.add(s);
                Thread t = new Thread(() -> serve(s, id), "raw-http-conn-" + id);
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                return;
            }
        }
    }

    private void serve(Socket s, int id) {
        try (s; InputStream in = s.getInputStream(); OutputStream out = s.getOutputStream()) {
            while (true) {
                Request request = readRequest(in, id);
                if (request == null) {
                    closedByClient.incrementAndGet();
                    return;
                }
                requests.add(request);
                boolean keep = handler.handle(request, out);
                out.flush();
                if (!keep) {
                    return;
                }
            }
        } catch (IOException | RuntimeException e) {
            closedByClient.incrementAndGet();
        }
    }

    private static Request readRequest(InputStream in, int id) throws IOException {
        String requestLine = readLine(in);
        if (requestLine == null) {
            return null;
        }
        List<String> headers = new ArrayList<>();
        String line;
        while ((line = readLine(in)) != null && !line.isEmpty()) {
            headers.add(line);
        }
        Request head = new Request(requestLine, headers, new byte[0], id);
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        String length = head.header("Content-Length");
        String te = head.header("Transfer-Encoding");
        if (requestLine.startsWith("CONNECT ")) {
            return head;
        }
        if (length != null) {
            body.write(in.readNBytes(Integer.parseInt(length)));
        } else if (te != null && te.toLowerCase(Locale.ROOT).contains("chunked")) {
            while (true) {
                int size = Integer.parseInt(readLine(in).trim(), 16);
                if (size == 0) {
                    readLine(in);
                    break;
                }
                body.write(in.readNBytes(size));
                readLine(in);
            }
        }
        return new Request(requestLine, headers, body.toByteArray(), id);
    }

    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') {
                byte[] bytes = line.toByteArray();
                int len = bytes.length > 0 && bytes[bytes.length - 1] == '\r' ? bytes.length - 1 : bytes.length;
                return new String(bytes, 0, len, StandardCharsets.ISO_8859_1);
            }
            line.write(b);
        }
        return line.size() == 0 ? null : line.toString(StandardCharsets.ISO_8859_1);
    }

    @Override
    public void close() throws IOException {
        socket.close();
        for (Socket s : open) {
            s.close();
        }
    }
}
