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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.http.urlconnection.TestRequests.readAll;
import static software.amazon.awssdk.http.urlconnection.TestRequests.request;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SyncHttpExchange;

/** TLS against a JDK {@link HttpsServer} with a certificate generated at test time, and HTTP proxies. */
class UrlConnectionTlsProxyTest {
    private static final char[] PASSWORD = "changeit".toCharArray();
    @TempDir
    static Path temp;
    private static KeyStore keyStore;
    private static HttpsServer https;

    @BeforeAll
    static void startHttps() throws Exception {
        Path store = temp.resolve("server.p12");
        String keytool = Path.of(System.getProperty("java.home"), "bin", "keytool").toString();
        Process p = new ProcessBuilder(keytool, "-genkeypair", "-alias", "server", "-keyalg", "EC", "-groupname",
                                       "secp256r1", "-dname", "CN=localhost", "-ext", "SAN=ip:127.0.0.1,dns:localhost",
                                       "-validity", "2", "-storetype", "PKCS12", "-keystore", store.toString(),
                                       "-storepass", "changeit", "-keypass", "changeit")
            .redirectErrorStream(true).start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(p.waitFor(60, TimeUnit.SECONDS) && p.exitValue() == 0, output);
        keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(store)) {
            keyStore.load(in, PASSWORD);
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, PASSWORD);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(kmf.getKeyManagers(), null, null);
        https = HttpsServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        https.setHttpsConfigurator(new HttpsConfigurator(context));
        https.createContext("/", exchange -> {
            byte[] body = ("secure " + exchange.getRequestURI().getRawPath()).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        https.start();
    }

    @AfterAll
    static void stopHttps() {
        https.stop(0);
    }

    private static SdkHttpRequest httpsGet(String path) {
        return request("GET", "https", https.getAddress().getPort(), path).build();
    }

    private static String send(UrlConnectionHttpClient client, SdkHttpRequest request) {
        try (client; SyncHttpExchange exchange = client.prepare(request, HttpExecutionOptions.defaults())) {
            return readAll(exchange.execute());
        }
    }

    @Test
    void customTrustManagersTrustTheGeneratedCertificate() throws Exception {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);
        UrlConnectionHttpClient client = UrlConnectionHttpClient.builder()
                                                                .tlsTrustManagersProvider(tmf::getTrustManagers)
                                                                .build();
        assertEquals("secure /a%2Fb", send(client, httpsGet("/a%2Fb")));
    }

    @Test
    void trustAllCertificatesSkipsValidation() {
        UrlConnectionHttpClient client = UrlConnectionHttpClient.builder().trustAllCertificates(true).build();
        assertEquals("secure /", send(client, httpsGet("/")));
    }

    @Test
    void untrustedCertificateIsATlsFailure() {
        UrlConnectionHttpClient client = UrlConnectionHttpClient.create();
        HttpTransportException e = assertThrows(HttpTransportException.class, () -> send(client, httpsGet("/")));
        assertEquals(HttpFailureCategory.TLS, e.category());
        assertEquals(HttpPhase.TLS_HANDSHAKE, e.phase());
        assertTrue(!e.retryable());
    }

    @Test
    void plainHttpThroughAProxyUsesTheAbsoluteRawTargetAndPreemptiveBasicCredentials() throws Exception {
        try (RawHttpServer proxy = new RawHttpServer((request, out) -> {
            out.write(RawHttpServer.response(200, "via proxy"));
            return true;
        })) {
            UrlConnectionHttpClient client = UrlConnectionHttpClient
                .builder()
                .proxyConfiguration(ProxyConfiguration.builder()
                                                      .endpoint(URI.create("http://127.0.0.1:" + proxy.port()))
                                                      .username("user").password("pa:ss").build())
                .build();
            SdkHttpRequest target = SdkHttpRequest.builder().method("GET").scheme("http").host("target.invalid")
                                                  .port(8080).encodedPath("/p%2Fq")
                                                  .addRawQueryParameter("a", "%20").build();
            assertEquals("via proxy", send(client, target));
            RawHttpServer.Request received = proxy.take();
            assertEquals("GET http://target.invalid:8080/p%2Fq?a=%20 HTTP/1.1", received.requestLine());
            assertEquals("Basic " + Base64.getEncoder().encodeToString("user:pa:ss".getBytes(StandardCharsets.UTF_8)),
                         received.header("Proxy-Authorization"));
            assertEquals("target.invalid:8080", received.header("Host"));
        }
    }

    @Test
    void nonProxyHostsConnectDirectly() throws Exception {
        try (RawHttpServer direct = new RawHttpServer((request, out) -> {
            out.write(RawHttpServer.response(200, "direct"));
            return true;
        })) {
            int unusedProxyPort;
            try (ServerSocket s = new ServerSocket(0)) {
                unusedProxyPort = s.getLocalPort();
            }
            UrlConnectionHttpClient client = UrlConnectionHttpClient
                .builder()
                .proxyConfiguration(ProxyConfiguration.builder()
                                                      .endpoint(URI.create("http://127.0.0.1:" + unusedProxyPort))
                                                      .addNonProxyHost("127\\.0\\.0\\.1").build())
                .build();
            assertEquals("direct", send(client, TestRequests.get(direct.port(), "/x").build()));
            assertEquals("GET /x HTTP/1.1", direct.take().requestLine());
            assertEquals(1, direct.connections());
        }
    }

    @Test
    void unreachableProxyIsAProxyFailure() throws Exception {
        int unusedProxyPort;
        try (ServerSocket s = new ServerSocket(0)) {
            unusedProxyPort = s.getLocalPort();
        }
        UrlConnectionHttpClient client = UrlConnectionHttpClient
            .builder()
            .proxyConfiguration(ProxyConfiguration.builder()
                                                  .endpoint(URI.create("http://127.0.0.1:" + unusedProxyPort)).build())
            .build();
        HttpTransportException e = assertThrows(
            HttpTransportException.class,
            () -> send(client, SdkHttpRequest.builder().method("GET").scheme("http").host("target.invalid")
                                             .encodedPath("/").build()));
        assertEquals(HttpFailureCategory.PROXY, e.category());
        assertEquals(HttpPhase.PROXY, e.phase());
    }

    @Test
    void tunnelRejectedByTheProxyIsAProxyFailure() throws Exception {
        try (RawHttpServer proxy = new RawHttpServer((request, out) -> {
            out.write("HTTP/1.1 403 Forbidden\r\nContent-Length: 0\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
            return false;
        })) {
            UrlConnectionHttpClient client = UrlConnectionHttpClient
                .builder()
                .proxyConfiguration(ProxyConfiguration.builder()
                                                      .endpoint(URI.create("http://127.0.0.1:" + proxy.port()))
                                                      .build())
                .build();
            HttpTransportException e = assertThrows(
                HttpTransportException.class,
                () -> send(client, SdkHttpRequest.builder().method("GET").scheme("https").host("target.invalid")
                                                 .encodedPath("/").build()));
            assertEquals(HttpFailureCategory.PROXY, e.category(), e::getMessage);
            assertEquals("CONNECT target.invalid:443 HTTP/1.1", proxy.take().requestLine());
        }
    }

    @Test
    void httpsThroughAProxyWithCredentialsFailsInsteadOfDroppingThem() throws Exception {
        try (RawHttpServer proxy = new RawHttpServer((request, out) -> false)) {
            UrlConnectionHttpClient client = UrlConnectionHttpClient
                .builder()
                .proxyConfiguration(ProxyConfiguration.builder()
                                                      .endpoint(URI.create("http://127.0.0.1:" + proxy.port()))
                                                      .username("u").password("p").build())
                .build();
            HttpTransportException e = assertThrows(
                HttpTransportException.class,
                () -> send(client, SdkHttpRequest.builder().method("GET").scheme("https").host("target.invalid")
                                                 .encodedPath("/").build()));
            assertEquals(HttpFailureCategory.PROXY, e.category());
            assertTrue(e.getMessage().contains("CONNECT"), e.getMessage());
            assertEquals(0, proxy.connections());
        }
    }
}
