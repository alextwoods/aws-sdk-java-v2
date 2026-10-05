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

package software.amazon.awssdk.http.crt;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;

/** JDK-only test servers and helpers. */
final class TestServers {
    private TestServers() {
    }

    /** A JDK HttpServer on an ephemeral port of every interface. */
    static final class Server implements AutoCloseable {
        final HttpServer server;
        final ExecutorService executor = Executors.newCachedThreadPool();

        Server(HttpServer server) {
            this.server = server;
            server.setExecutor(executor);
            server.start();
        }

        static Server http(HttpHandler handler) throws IOException {
            HttpServer s = HttpServer.create(new InetSocketAddress(0), 0);
            s.createContext("/", handler);
            return new Server(s);
        }

        static Server https(HttpHandler handler, Tls tls) throws Exception {
            HttpsServer s = HttpsServer.create(new InetSocketAddress(0), 0);
            s.setHttpsConfigurator(new HttpsConfigurator(tls.serverContext()));
            s.createContext("/", handler);
            return new Server(s);
        }

        int port() {
            return server.getAddress().getPort();
        }

        SdkHttpRequest.Builder get(String path) {
            return request("http", "127.0.0.1", "GET", path);
        }

        SdkHttpRequest.Builder request(String scheme, String host, String method, String path) {
            return SdkHttpRequest.builder().method(method).scheme(scheme).host(host).port(port()).encodedPath(path);
        }

        @Override
        public void close() {
            server.stop(0);
            executor.shutdownNow();
        }
    }

    /** A self-signed localhost certificate generated with keytool at test time. */
    static final class Tls {
        final Path keyStore;
        final String pem;

        private Tls(Path keyStore, String pem) {
            this.keyStore = keyStore;
            this.pem = pem;
        }

        static Tls generate() throws Exception {
            Path dir = Files.createTempDirectory("crt-tls");
            Path ks = dir.resolve("server.p12");
            Path cert = dir.resolve("server.pem");
            String keytool = Path.of(System.getProperty("java.home"), "bin", "keytool").toString();
            run(keytool, "-genkeypair", "-alias", "server", "-keyalg", "RSA", "-keysize", "2048", "-dname",
                "CN=localhost", "-ext", "SAN=dns:localhost,ip:127.0.0.1", "-ext", "EKU=serverAuth", "-ext",
                "KU=digitalSignature,keyEncipherment,keyCertSign", "-ext", "BC=ca:true", "-validity", "2", "-storetype", "PKCS12",
                "-keystore", ks.toString(), "-storepass", "changeit", "-keypass", "changeit");
            run(keytool, "-exportcert", "-rfc", "-alias", "server", "-keystore", ks.toString(), "-storepass",
                "changeit", "-file", cert.toString());
            return new Tls(ks, Files.readString(cert));
        }

        SSLContext serverContext() throws Exception {
            KeyStore store = KeyStore.getInstance("PKCS12");
            try (InputStream in = Files.newInputStream(keyStore)) {
                store.load(in, "changeit".toCharArray());
            }
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(store, "changeit".toCharArray());
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(kmf.getKeyManagers(), null, null);
            return context;
        }

        private static void run(String... command) throws Exception {
            Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!p.waitFor(60, TimeUnit.SECONDS) || p.exitValue() != 0) {
                throw new IllegalStateException("keytool failed: " + out);
            }
        }
    }

    /** A raw one-request-per-connection HTTP/1.1 server that records the exact request head bytes. */
    static final class RawServer implements AutoCloseable {
        final ServerSocket socket;
        final AtomicReference<String> lastHead = new AtomicReference<>();
        final Thread thread;

        RawServer() throws IOException {
            socket = new ServerSocket(0);
            thread = new Thread(this::serve, "raw-http");
            thread.setDaemon(true);
            thread.start();
        }

        private void serve() {
            while (!socket.isClosed()) {
                try (Socket s = socket.accept()) {
                    InputStream in = s.getInputStream();
                    ByteArrayOutputStream head = new ByteArrayOutputStream();
                    int state = 0;
                    while (state < 4) {
                        int b = in.read();
                        if (b < 0) {
                            break;
                        }
                        head.write(b);
                        state = (b == '\r' && (state == 0 || state == 2)) || (b == '\n' && (state == 1 || state == 3))
                                ? state + 1 : 0;
                    }
                    lastHead.set(head.toString(StandardCharsets.ISO_8859_1));
                    OutputStream out = s.getOutputStream();
                    out.write(("HTTP/1.1 200 OK\r\nContent-Length: 2\r\nX-Rep: a\r\nx-rep: b\r\nX-Rep: c\r\n"
                               + "Connection: close\r\n\r\nok").getBytes(StandardCharsets.ISO_8859_1));
                    out.flush();
                } catch (IOException e) {
                    return;
                }
            }
        }

        int port() {
            return socket.getLocalPort();
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }

    static byte[] readAll(HttpBody body) throws IOException {
        if (body.kind() == HttpBody.Kind.EMPTY) {
            return new byte[0];
        }
        try (InputStream in = body.streamingBody().openStream()) {
            return in.readAllBytes();
        }
    }

    /** The CRT native objects with a live native handle now (requires aws.crt.debugnative). */
    static Set<CrtResource> liveNativeResources() {
        Set<CrtResource> live = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        List<CrtResource> all = new ArrayList<>();
        // A wrapper whose native constructor threw has no native handle (nothing to release) but stays tracked.
        CrtResource.collectNativeResource(r -> {
            if (r.getWrapper() != null && !r.getWrapper().isNull()) {
                all.add(r.getWrapper());
            }
        });
        live.addAll(all);
        return live;
    }

    /** Waits until every native object alive now was alive in {@code before}; returns the leftovers. */
    static Set<CrtResource> awaitNoNewNativeResources(Set<CrtResource> before, Duration timeout)
        throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        Set<CrtResource> extra;
        do {
            extra = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            extra.addAll(liveNativeResources());
            extra.removeAll(before);
            if (extra.isEmpty()) {
                return extra;
            }
            Thread.sleep(50);
        } while (System.nanoTime() < deadline);
        return new HashSet<>(extra);
    }
}
