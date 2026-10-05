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

package software.amazon.awssdk.http.apache.internal;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import software.amazon.awssdk.core.exception.SdkClientConfigurationException;
import software.amazon.awssdk.http.CancellationReason;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SyncHttpExchange;

/**
 * The shared state of one Apache client: the connection pool, the optional idle reaper, and the exchanges in flight.
 *
 * <p>Ownership: {@link #create} registers the pool and then the reaper, as each is acquired, with the supplied
 * registrar (the provider's {@code HttpClientConstruction}, or a no-op for a directly built client), so a failed
 * construction releases them in reverse order. {@link #close()} is idempotent: it aborts every exchange in flight
 * with {@link CancellationReason#CLIENT_CLOSED}, stops the reaper and shuts the pool down, closing every pooled and
 * leased connection; the construction's own release of the same resources afterwards is a no-op.
 *
 * <p>Buffering (Requirement 12.6): each connection has an 8 KiB session input buffer and an 8 KiB session output
 * buffer, chunked output adds a 2 KiB chunk cache, and the request body is copied through one 8 KiB buffer, so at
 * most {@link #MAX_BUFFERED_BYTES_PER_EXCHANGE} body bytes are buffered per exchange in each direction beyond the
 * caller's reads.
 */
public final class ApacheEngine implements AutoCloseable {
    /** Session buffer size of every connection, in bytes. */
    public static final int SESSION_BUFFER_SIZE = 8192;
    /** The documented bound on body bytes buffered per exchange in each direction. */
    public static final long MAX_BUFFERED_BYTES_PER_EXCHANGE = 32 * 1024;

    private final String clientName;
    private final EngineSettings settings;
    private final PoolingHttpClientConnectionManager manager;
    private final IdleConnectionReaper reaper;
    private final Set<ApacheExchange> live = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean closed = new AtomicBoolean();

    private ApacheEngine(String clientName, EngineSettings settings, PoolingHttpClientConnectionManager manager,
                         IdleConnectionReaper reaper) {
        this.clientName = clientName;
        this.settings = settings;
        this.manager = manager;
        this.reaper = reaper;
    }

    /**
     * Builds the TLS context (acquires nothing), then the pool and the reaper, registering each with
     * {@code registrar} as soon as it exists.
     *
     * @throws SdkClientConfigurationException if the TLS context cannot be initialized
     */
    public static ApacheEngine create(String clientName, EngineSettings settings,
                                      Consumer<AutoCloseable> registrar) {
        SSLConnectionSocketFactory tls = tlsSocketFactory(settings);
        PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager(
            SocketFactories.registry(tls), null, DefaultSchemePortResolver.INSTANCE, SystemDefaultDnsResolver.INSTANCE,
            settings.connectionTimeToLive().isZero() ? -1 : settings.connectionTimeToLive().toMillis(),
            TimeUnit.MILLISECONDS);
        registrar.accept(manager);
        manager.setMaxTotal(settings.maxConnections());
        manager.setDefaultMaxPerRoute(settings.maxConnections());
        manager.setDefaultSocketConfig(SocketConfig.custom()
                                                   .setSoKeepAlive(settings.tcpKeepAlive())
                                                   .setSoTimeout(millis(settings.socketTimeout().toMillis()))
                                                   .setTcpNoDelay(true)
                                                   .build());
        manager.setDefaultConnectionConfig(ConnectionConfig.custom()
                                                           .setBufferSize(SESSION_BUFFER_SIZE)
                                                           .setFragmentSizeHint(SESSION_BUFFER_SIZE)
                                                           .build());
        IdleConnectionReaper reaper = null;
        if (settings.useIdleConnectionReaper()) {
            reaper = new IdleConnectionReaper(manager, settings.connectionMaxIdleTime());
            registrar.accept(reaper);
        }
        return new ApacheEngine(clientName, settings, manager, reaper);
    }

    /**
     * A new exchange; no I/O.
     *
     * @throws IllegalStateException if the engine is closed
     * @throws IllegalArgumentException if a {@code Content-Length} header disagrees with the body's known length
     */
    public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
        if (closed.get()) {
            throw new IllegalStateException(clientName + " is closed");
        }
        HttpBody body = request.body();
        List<String> lengths = request.headers().allValues("Content-Length");
        if (!lengths.isEmpty() && body.contentLength().isPresent()) {
            for (String value : lengths) {
                if (!value.trim().equals(Long.toString(body.contentLength().getAsLong()))) {
                    throw new IllegalArgumentException("The Content-Length header disagrees with the body length "
                                                       + body.contentLength().getAsLong());
                }
            }
        }
        return new ApacheExchange(this, request, options == null ? HttpExecutionOptions.defaults() : options);
    }

    String clientName() {
        return clientName;
    }

    EngineSettings settings() {
        return settings;
    }

    PoolingHttpClientConnectionManager manager() {
        return manager;
    }

    /** Tracks {@code exchange} while it holds pool resources; false if the engine is closed. */
    boolean track(ApacheExchange exchange) {
        live.add(exchange);
        if (closed.get()) {
            live.remove(exchange);
            return false;
        }
        return true;
    }

    void untrack(ApacheExchange exchange) {
        live.remove(exchange);
    }

    public boolean isClosed() {
        return closed.get();
    }

    /** Leased plus available pooled connections; for tests and diagnostics. */
    public int pooledConnections() {
        var stats = manager.getTotalStats();
        return stats.getLeased() + stats.getAvailable();
    }

    /** Pooled connections not leased; for tests and diagnostics. */
    public int availableConnections() {
        return manager.getTotalStats().getAvailable();
    }

    /** Whether the reaper thread was started and has been stopped; false if there is no reaper. */
    public boolean reaperStopped() {
        return reaper != null && reaper.isClosed();
    }

    /** Whether an idle reaper runs for this engine. */
    public boolean hasReaper() {
        return reaper != null;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        for (ApacheExchange exchange : live) {
            exchange.abort(CancellationReason.CLIENT_CLOSED);
        }
        if (reaper != null) {
            reaper.close();
        }
        manager.shutdown();
    }

    @Override
    public String toString() {
        return "ApacheEngine[" + clientName + ", closed=" + closed.get() + ", " + settings + "]";
    }

    static int millis(long millis) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, millis));
    }

    private static SSLConnectionSocketFactory tlsSocketFactory(EngineSettings settings) {
        if (settings.trustAllCertificates() && settings.trustManagers() != null) {
            throw SdkClientConfigurationException.create("tlsTrustManagersProvider and trustAllCertificates cannot "
                                                         + "both be configured");
        }
        TrustManager[] trust = settings.trustAllCertificates() ? new TrustManager[] {new TrustAll()}
                                                                : settings.trustManagers();
        HostnameVerifier verifier = settings.trustAllCertificates() ? NoopHostnameVerifier.INSTANCE
                                                                    : SSLConnectionSocketFactory.getDefaultHostnameVerifier();
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(settings.keyManagers(), trust, null);
            return new SSLConnectionSocketFactory(context, verifier);
        } catch (GeneralSecurityException e) {
            throw (SdkClientConfigurationException) SdkClientConfigurationException.builder()
                                                                                   .message("The TLS context could "
                                                                                            + "not be initialized")
                                                                                   .cause(e)
                                                                                   .build();
        }
    }

    /** Accepts every certificate: {@code trustAllCertificates}, for testing only. */
    private static final class TrustAll implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
