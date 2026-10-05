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
import javax.net.ssl.SSLServerSocket;

/**
 * A byte-exact HTTP/1.1 test server over a JDK {@link ServerSocket} (or {@link SSLServerSocket}). It records each
 * request head in wire order, counts TCP connections (so a reused connection can be told from a discarded one), and
 * hands every request to a scripted {@link Handler} that may read the body, stream a response, hold, or close.
 * Each connection is served on its own daemon thread.
 */
final class TckServer implements AutoCloseable {

    /** A received request head; {@code connection} is the 1-based index of the TCP connection it arrived on. */
    record Head(String requestLine, List<String> headerLines, int connection) {
        String header(String name) {
            for (String line : headerLines) {
                int colon = line.indexOf(':');
                if (colon > 0 && line.substring(0, colon).equalsIgnoreCase(name)) {
                    return line.substring(colon + 1).trim();
                }
            }
            return null;
        }

        String method() {
            return requestLine.substring(0, requestLine.indexOf(' '));
        }

        String target() {
            return requestLine.substring(requestLine.indexOf(' ') + 1, requestLine.lastIndexOf(' '));
        }
    }

    /** One request being served. */
    static final class Exchange {
        private final Head head;
        private final InputStream in;
        private final OutputStream out;
        private final Socket socket;
        private byte[] body;

        Exchange(Head head, InputStream in, OutputStream out, Socket socket) {
            this.head = head;
            this.in = in;
            this.out = out;
            this.socket = socket;
        }

        Head head() {
            return head;
        }

        OutputStream out() {
            return out;
        }

        InputStream in() {
            return in;
        }

        Socket socket() {
            return socket;
        }

        /** Reads the request body according to its framing (once; cached). */
        byte[] body() throws IOException {
            if (body == null) {
                body = readBody(head, in);
            }
            return body;
        }

        boolean bodyRead() {
            return body != null;
        }
    }

    /** Serves one request; returns false to close the connection afterwards. */
    @FunctionalInterface
    interface Handler {
        boolean handle(Exchange exchange) throws Exception;
    }

    private final ServerSocket socket;
    private final Handler handler;
    private final AtomicInteger connections = new AtomicInteger();
    private final BlockingQueue<Head> heads = new LinkedBlockingQueue<>();
    private final BlockingQueue<byte[]> bodies = new LinkedBlockingQueue<>();
    private final List<Socket> open = new CopyOnWriteArrayList<>();

    private TckServer(ServerSocket socket, Handler handler) {
        this.socket = socket;
        this.handler = handler;
        Thread acceptor = new Thread(this::acceptLoop, "tck-accept-" + socket.getLocalPort());
        acceptor.setDaemon(true);
        acceptor.start();
    }

    static TckServer plain(Handler handler) throws IOException {
        return new TckServer(new ServerSocket(0, 50, InetAddress.getLoopbackAddress()), handler);
    }

    static TckServer tls(TckTls tls, Handler handler) throws IOException {
        SSLServerSocket s = (SSLServerSocket) tls.serverContext().getServerSocketFactory()
                                                 .createServerSocket(0, 50, InetAddress.getLoopbackAddress());
        return new TckServer(s, handler);
    }

    /** A complete response with a fixed-length body. */
    static byte[] response(int status, byte[] body, String... headers) {
        StringBuilder out = new StringBuilder("HTTP/1.1 " + status + " TCK\r\n");
        for (String header : headers) {
            out.append(header).append("\r\n");
        }
        out.append("Content-Length: ").append(body.length).append("\r\n\r\n");
        byte[] head = out.toString().getBytes(StandardCharsets.ISO_8859_1);
        byte[] all = new byte[head.length + body.length];
        System.arraycopy(head, 0, all, 0, head.length);
        System.arraycopy(body, 0, all, head.length, body.length);
        return all;
    }

    static byte[] response(int status, String body, String... headers) {
        return response(status, body.getBytes(StandardCharsets.UTF_8), headers);
    }

    static byte[] ascii(String text) {
        return text.getBytes(StandardCharsets.ISO_8859_1);
    }

    int port() {
        return socket.getLocalPort();
    }

    /** TCP connections accepted so far. */
    int connections() {
        return connections.get();
    }

    /** The next request head, waiting up to 10 seconds. */
    Head take() throws InterruptedException {
        Head head = heads.poll(10, TimeUnit.SECONDS);
        if (head == null) {
            throw new AssertionError("no request received");
        }
        return head;
    }

    /** The body of the next request whose body the server read to completion (by a handler or between requests). */
    byte[] takeBody() throws InterruptedException {
        byte[] body = bodies.poll(10, TimeUnit.SECONDS);
        if (body == null) {
            throw new AssertionError("no request body received");
        }
        return body;
    }

    private void acceptLoop() {
        while (!socket.isClosed()) {
            try {
                Socket s = socket.accept();
                int id = connections.incrementAndGet();
                open.add(s);
                Thread t = new Thread(() -> serve(s, id), "tck-conn-" + port() + "-" + id);
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
                Head head = readHead(in, id);
                if (head == null) {
                    return;
                }
                heads.add(head);
                Exchange exchange = new Exchange(head, in, out, s);
                boolean keep = handler.handle(exchange);
                out.flush();
                if (!keep) {
                    return;
                }
                if (!exchange.bodyRead()) {
                    exchange.body();
                }
                bodies.add(exchange.body());
            }
        } catch (Exception e) {
            // The client closed or reset the connection, or a handler gave up; the connection ends here.
        }
    }

    private static Head readHead(InputStream in, int id) throws IOException {
        String requestLine = readLine(in);
        if (requestLine == null) {
            return null;
        }
        List<String> headers = new ArrayList<>();
        String line;
        while ((line = readLine(in)) != null && !line.isEmpty()) {
            headers.add(line);
        }
        return new Head(requestLine, List.copyOf(headers), id);
    }

    private static byte[] readBody(Head head, InputStream in) throws IOException {
        if (head.requestLine().startsWith("CONNECT ")) {
            return new byte[0];
        }
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        String length = head.header("Content-Length");
        String te = head.header("Transfer-Encoding");
        if (te != null && te.toLowerCase(Locale.ROOT).contains("chunked")) {
            while (true) {
                String sizeLine = readLine(in);
                if (sizeLine == null) {
                    throw new IOException("EOF in chunked body");
                }
                int semicolon = sizeLine.indexOf(';');
                int size = Integer.parseInt((semicolon < 0 ? sizeLine : sizeLine.substring(0, semicolon)).trim(), 16);
                if (size == 0) {
                    String trailer;
                    while ((trailer = readLine(in)) != null && !trailer.isEmpty()) {
                        // trailers ignored
                    }
                    break;
                }
                body.write(in.readNBytes(size));
                readLine(in);
            }
        } else if (length != null) {
            body.write(in.readNBytes(Integer.parseInt(length)));
        }
        return body.toByteArray();
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
            try {
                s.close();
            } catch (IOException e) {
                // already closed
            }
        }
    }
}
