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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.Certificate;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/** A self-signed {@code localhost} certificate generated at test time with the JDK's {@code keytool}. */
final class TestTls {
    private static final char[] PASSWORD = "changeit".toCharArray();
    private static TestTls instance;

    final SSLContext serverContext;
    final TrustManager[] trustManagers;

    private TestTls() throws Exception {
        Path dir = Files.createTempDirectory("apache-tls");
        Path store = dir.resolve("server.p12");
        String keytool = Path.of(System.getProperty("java.home"), "bin", "keytool").toString();
        Process p = new ProcessBuilder(keytool, "-genkeypair", "-alias", "server", "-keyalg", "RSA", "-keysize", "2048",
                                       "-dname", "CN=localhost", "-ext", "SAN=dns:localhost,ip:127.0.0.1",
                                       "-validity", "2", "-storetype", "PKCS12", "-keystore", store.toString(),
                                       "-storepass", "changeit", "-keypass", "changeit")
            .redirectErrorStream(true).start();
        byte[] output = p.getInputStream().readAllBytes();
        if (p.waitFor() != 0) {
            throw new IllegalStateException("keytool failed: " + new String(output));
        }
        KeyStore keys = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(store)) {
            keys.load(in, PASSWORD);
        }
        Files.delete(store);
        Files.delete(dir);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keys, PASSWORD);
        serverContext = SSLContext.getInstance("TLS");
        serverContext.init(kmf.getKeyManagers(), null, null);
        Certificate cert = keys.getCertificate("server");
        KeyStore trust = KeyStore.getInstance("PKCS12");
        trust.load(null, null);
        trust.setCertificateEntry("server", cert);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trust);
        trustManagers = tmf.getTrustManagers();
    }

    static synchronized TestTls get() throws Exception {
        if (instance == null) {
            instance = new TestTls();
        }
        return instance;
    }
}
