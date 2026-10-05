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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SyncHttpExchange;

/** HTTPS through a plain HTTP proxy: {@code CONNECT} with preemptive basic credentials, then TLS in the tunnel. */
class ApacheProxyTunnelTest {
    @Test
    void httpsIsTunnelledThroughThePlainProxyWithCredentials() throws Exception {
        TestTls tls = TestTls.get();
        HttpsServer https = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        https.setHttpsConfigurator(new HttpsConfigurator(tls.serverContext));
        https.createContext("/", e -> {
            byte[] body = "tunnelled".getBytes(StandardCharsets.UTF_8);
            e.sendResponseHeaders(200, body.length);
            try (OutputStream out = e.getResponseBody()) {
                out.write(body);
            }
        });
        https.start();
        List<String> connectHead = new CopyOnWriteArrayList<>();
        try (ServerSocket proxy = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            Thread proxyThread = new Thread(() -> tunnelOnce(proxy, https.getAddress().getPort(), connectHead));
            proxyThread.setDaemon(true);
            proxyThread.start();
            ProxyConfiguration config = ProxyConfiguration.builder()
                                                          .endpoint(URI.create("http://127.0.0.1:"
                                                                               + proxy.getLocalPort()))
                                                          .username("user").password("pw").build();
            try (ApacheHttpClient client = ApacheHttpClient.builder().proxyConfiguration(config)
                                                           .tlsTrustManagersProvider(() -> tls.trustManagers)
                                                           .build()) {
                SdkHttpRequest request = SdkHttpRequest.builder().method("GET").scheme("https").host("localhost")
                                                       .port(https.getAddress().getPort()).encodedPath("/t").build();
                try (SyncHttpExchange exchange = client.prepare(request, HttpExecutionOptions.defaults());
                     InputStream in = exchange.execute().body().streamingBody().openStream()) {
                    assertEquals("tunnelled", new String(in.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
        } finally {
            https.stop(0);
        }
        assertEquals("CONNECT localhost:" + https.getAddress().getPort() + " HTTP/1.1", connectHead.get(0));
        assertTrue(connectHead.contains("Proxy-Authorization: Basic dXNlcjpwdw=="), connectHead.toString());
    }

    /** Accepts one CONNECT, answers 200 and relays bytes both ways to the HTTPS server. */
    private static void tunnelOnce(ServerSocket proxy, int targetPort, List<String> head) {
        try (Socket client = proxy.accept();
             Socket target = new Socket(InetAddress.getLoopbackAddress(), targetPort)) {
            InputStream in = client.getInputStream();
            String line;
            while (!(line = line(in)).isEmpty()) {
                head.add(line);
            }
            client.getOutputStream().write("HTTP/1.1 200 Connection established\r\n\r\n"
                                               .getBytes(StandardCharsets.US_ASCII));
            client.getOutputStream().flush();
            Thread up = new Thread(() -> relay(in, target));
            up.setDaemon(true);
            up.start();
            relay(target.getInputStream(), client);
        } catch (IOException ignored) {
            // The test fails on the client side.
        }
    }

    private static void relay(InputStream from, Socket to) {
        try {
            from.transferTo(to.getOutputStream());
        } catch (IOException ignored) {
            // Connection closed.
        }
    }

    private static String line(InputStream in) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) >= 0 && b != '\n') {
            if (b != '\r') {
                bytes.write(b);
            }
        }
        return bytes.toString(StandardCharsets.US_ASCII);
    }
}
