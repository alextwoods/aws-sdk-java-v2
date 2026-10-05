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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Base64;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * A self-signed {@code localhost} certificate generated once per JVM with the JDK's {@code keytool}. It carries
 * serverAuth EKU, key usage and basic constraints so that native trust stores (the CRT on macOS) accept it as an
 * anchor, and is exposed both as JSSE trust managers and as PEM.
 */
final class TckTls {
    private static final char[] PASSWORD = "changeit".toCharArray();
    private static TckTls instance;

    private final SSLContext serverContext;
    private final TrustManager[] trustManagers;
    private final String certificatePem;

    private TckTls() throws Exception {
        Path dir = Files.createTempDirectory("http-tck-tls");
        Path store = dir.resolve("server.p12");
        String keytool = Path.of(System.getProperty("java.home"), "bin", "keytool").toString();
        Process p = new ProcessBuilder(keytool, "-genkeypair", "-alias", "server", "-keyalg", "RSA", "-keysize", "2048",
                                       "-dname", "CN=localhost", "-ext", "SAN=dns:localhost,ip:127.0.0.1",
                                       "-ext", "EKU=serverAuth", "-ext",
                                       "KU=digitalSignature,keyEncipherment,keyCertSign", "-ext", "BC=ca:true",
                                       "-validity", "2", "-storetype", "PKCS12", "-keystore", store.toString(),
                                       "-storepass", "changeit", "-keypass", "changeit")
            .redirectErrorStream(true).start();
        byte[] output = p.getInputStream().readAllBytes();
        if (p.waitFor() != 0) {
            throw new IllegalStateException("keytool failed: " + new String(output, StandardCharsets.UTF_8));
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
        certificatePem = "-----BEGIN CERTIFICATE-----\n"
                         + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                                 .encodeToString(cert.getEncoded())
                         + "\n-----END CERTIFICATE-----\n";
    }

    static synchronized TckTls get() {
        if (instance == null) {
            try {
                instance = new TckTls();
            } catch (Exception e) {
                throw new IllegalStateException("Could not generate the TCK certificate", e);
            }
        }
        return instance;
    }

    SSLContext serverContext() {
        return serverContext;
    }

    TrustManager[] trustManagers() {
        return trustManagers.clone();
    }

    String certificatePem() {
        return certificatePem;
    }
}
