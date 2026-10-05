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

package software.amazon.awssdk.http.nio.netty;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.SdkHttpResponse;

/** Recording handler/subscriber and test-time TLS material. */
final class TestSupport {
    static final char[] PASSWORD = "changeit".toCharArray();
    private static KeyStore keyStore;

    private TestSupport() {
    }

    /** A PKCS12 key store with a fresh self-signed localhost/127.0.0.1 certificate, made by the JDK keytool. */
    static synchronized KeyStore keyStore() throws Exception {
        if (keyStore == null) {
            Path dir = Files.createTempDirectory("netty-tls");
            Path file = dir.resolve("server.p12");
            Process p = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "keytool").toString(),
                                           "-genkeypair", "-alias", "server", "-keyalg", "EC", "-groupname",
                                           "secp256r1", "-dname", "CN=localhost", "-ext",
                                           "SAN=dns:localhost,ip:127.0.0.1", "-validity", "2", "-storetype",
                                           "PKCS12", "-keystore", file.toString(), "-storepass",
                                           new String(PASSWORD), "-keypass", new String(PASSWORD))
                .redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes());
            if (!p.waitFor(60, TimeUnit.SECONDS) || p.exitValue() != 0) {
                throw new IllegalStateException("keytool failed: " + out);
            }
            KeyStore ks = KeyStore.getInstance("PKCS12");
            try (InputStream in = Files.newInputStream(file)) {
                ks.load(in, PASSWORD);
            }
            Files.delete(file);
            Files.delete(dir);
            keyStore = ks;
        }
        return keyStore;
    }

    static KeyManagerFactory keyManagers() throws Exception {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore(), PASSWORD);
        return kmf;
    }

    static TrustManagerFactory trustManagers() throws Exception {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore());
        return tmf;
    }

    static SSLContext serverSslContext() throws Exception {
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(keyManagers().getKeyManagers(), null, null);
        return ctx;
    }

    /** Records every signal, its thread, and the body; requests {@code batch} chunks at a time. */
    static final class Recorder implements AsyncHttpResponseHandler {
        final CompletableFuture<SdkHttpResponse> headers = new CompletableFuture<>();
        final CompletableFuture<Publisher<ByteBuffer>> stream = new CompletableFuture<>();
        final CompletableFuture<byte[]> body = new CompletableFuture<>();
        final CompletableFuture<Throwable> error = new CompletableFuture<>();
        final List<String> threads = new CopyOnWriteArrayList<>();
        final List<String> signals = new CopyOnWriteArrayList<>();
        final AtomicReference<Subscription> subscription = new AtomicReference<>();
        private final long batch;
        private final boolean subscribe;
        private final ByteArrayOutputStream data = new ByteArrayOutputStream();

        Recorder(long batch, boolean subscribe) {
            this.batch = batch;
            this.subscribe = subscribe;
        }

        Recorder() {
            this(1, true);
        }

        private void record(String signal) {
            signals.add(signal);
            threads.add(Thread.currentThread().getName());
        }

        @Override
        public void onHeaders(SdkHttpResponse response) {
            record("onHeaders");
            headers.complete(response);
        }

        @Override
        public void onStream(Publisher<ByteBuffer> publisher) {
            record("onStream");
            stream.complete(publisher);
            if (!subscribe) {
                return;
            }
            publisher.subscribe(new Subscriber<>() {
                @Override
                public void onSubscribe(Subscription s) {
                    record("onSubscribe");
                    subscription.set(s);
                    s.request(batch);
                }

                @Override
                public void onNext(ByteBuffer chunk) {
                    record("onNext");
                    byte[] bytes = new byte[chunk.remaining()];
                    chunk.get(bytes);
                    synchronized (data) {
                        data.writeBytes(bytes);
                    }
                    subscription.get().request(1);
                }

                @Override
                public void onError(Throwable t) {
                    record("bodyError");
                    error.complete(t);
                    body.completeExceptionally(t);
                }

                @Override
                public void onComplete() {
                    record("onComplete");
                    synchronized (data) {
                        body.complete(data.toByteArray());
                    }
                }
            });
        }

        @Override
        public void onError(Throwable e) {
            record("onError");
            error.complete(e);
            headers.completeExceptionally(e);
            body.completeExceptionally(e);
        }
    }
}
