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

package software.amazon.awssdk.http.crt.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.crt.http.HttpClientConnection;
import software.amazon.awssdk.crt.http.HttpClientConnectionManager;
import software.amazon.awssdk.crt.http.HttpClientConnectionManagerOptions;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpProxyOptions;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.crt.http.HttpRequestBodyStream;
import software.amazon.awssdk.crt.io.ClientBootstrap;
import software.amazon.awssdk.crt.io.EventLoopGroup;
import software.amazon.awssdk.crt.io.HostResolver;
import software.amazon.awssdk.crt.io.SocketOptions;
import software.amazon.awssdk.crt.io.TlsCipherPreference;
import software.amazon.awssdk.crt.io.TlsConnectionOptions;
import software.amazon.awssdk.crt.io.TlsContext;
import software.amazon.awssdk.crt.io.TlsContextOptions;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpHeaders;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.crt.ProxyConfiguration;
import software.amazon.awssdk.http.crt.TcpKeepAliveConfiguration;

/**
 * The native resources and connection pools of one CRT client, shared by the sync and async clients.
 *
 * <p>JNI ownership. Every native object is wrapped in an {@link Owned} that closes it at most once. Client-wide
 * objects are acquired at construction in dependency order: (1) {@link EventLoopGroup} and (2) {@link HostResolver},
 * only when {@code numEventLoopThreads} is set (otherwise the CRT's process-wide static defaults are used and never
 * closed here), (3) {@link ClientBootstrap}, (4) {@link SocketOptions}, (5) {@link TlsContextOptions},
 * (6) {@link TlsContext}. Each is handed to the {@code registrar} (a provider's {@code HttpClientConstruction.sdkOwned})
 * as soon as it exists, so a failed construction releases them in reverse order. Per-endpoint objects
 * ({@link TlsConnectionOptions} and {@link HttpClientConnectionManager}) are created on the first request to an
 * endpoint, after construction, and are owned by this core alone. {@link #close()} releases the pools, newest first
 * (each manager before its TLS connection options), then the client-wide objects in reverse acquisition order; a
 * later release by the resolver of the same registrations is a no-op.
 */
public final class CrtClientCore implements AutoCloseable {
    private final String clientName;
    private final CrtSettings settings;
    private final List<Owned> owned = new ArrayList<>();
    private final Map<String, Pool> pools = new LinkedHashMap<>();
    private final ClientBootstrap bootstrap;
    private final SocketOptions socketOptions;
    private final TlsContext tlsContext;
    private final HttpProxyOptions proxyOptions;
    private final long readBufferSize;
    private final java.util.Set<Runnable> inFlight = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private boolean closed;

    /** A native object closed at most once. */
    public static final class Owned implements AutoCloseable {
        private final CrtResource resource;
        private final String name;
        private final AtomicInteger closes = new AtomicInteger();

        Owned(CrtResource resource, String name) {
            this.resource = resource;
            this.name = name;
        }

        public CrtResource resource() {
            return resource;
        }

        public String name() {
            return name;
        }

        /** How many times the native object was closed: 0 or 1. */
        public int closeCount() {
            return closes.get();
        }

        @Override
        public void close() {
            if (closes.compareAndSet(0, 1)) {
                resource.close();
            }
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private record Pool(Owned manager, Owned tlsConnectionOptions) {
    }

    public CrtClientCore(String clientName, CrtSettings settings, Consumer<AutoCloseable> registrar) {
        this.clientName = clientName;
        this.settings = settings;
        this.readBufferSize = settings.readBufferSize();
        try {
            EventLoopGroup group = null;
            HostResolver resolver = null;
            if (settings.numEventLoopThreads != null) {
                group = own(new EventLoopGroup(settings.numEventLoopThreads), "EventLoopGroup", registrar);
                resolver = own(new HostResolver(group), "HostResolver", registrar);
            }
            this.bootstrap = own(new ClientBootstrap(group, resolver), "ClientBootstrap", registrar);
            this.socketOptions = own(socketOptions(settings), "SocketOptions", registrar);
            TlsContextOptions tlsOptions = own(tlsContextOptions(settings), "TlsContextOptions", registrar);
            this.tlsContext = own(new TlsContext(tlsOptions), "TlsContext", registrar);
            this.proxyOptions = proxyOptions(settings.proxyConfiguration, tlsContext);
        } catch (RuntimeException | Error e) {
            // Direct builder use has no construction to roll back; release what exists (no-op if a resolver does).
            closeOwned();
            throw e;
        }
    }

    private <T extends CrtResource> T own(T resource, String name, Consumer<AutoCloseable> registrar) {
        Owned o = new Owned(resource, name);
        synchronized (this) {
            owned.add(o);
        }
        registrar.accept(o);
        return resource;
    }

    private static SocketOptions socketOptions(CrtSettings settings) {
        SocketOptions options = new SocketOptions();
        options.connectTimeoutMs = saturatedMillis(settings.connectionTimeoutOrDefault());
        TcpKeepAliveConfiguration keepAlive = settings.tcpKeepAliveConfiguration;
        if (keepAlive != null) {
            options.keepAlive = true;
            options.keepAliveIntervalSecs = (int) Math.min(Integer.MAX_VALUE, keepAlive.keepAliveInterval().getSeconds());
            options.keepAliveTimeoutSecs = (int) Math.min(Integer.MAX_VALUE, keepAlive.keepAliveTimeout().getSeconds());
            keepAlive.keepAliveProbes().ifPresent(p -> options.keepAliveMaxFailedProbes = p);
        }
        return options;
    }

    private static TlsContextOptions tlsContextOptions(CrtSettings settings) {
        TlsContextOptions options = TlsContextOptions.createDefaultClient();
        options.withCipherPreference(cipherPreference(settings.postQuantumTlsEnabled))
               .withVerifyPeer(!Boolean.TRUE.equals(settings.trustAllCertificates));
        if (settings.trustedCertificatesPem != null) {
            options.overrideDefaultTrustStore(settings.trustedCertificatesPem);
        }
        return options;
    }

    /** V2 {@code resolveCipherPreference}: the CRT prefers post-quantum by default; only an explicit false opts out. */
    static TlsCipherPreference cipherPreference(Boolean postQuantumTlsEnabled) {
        if (Boolean.FALSE.equals(postQuantumTlsEnabled) && TlsCipherPreference.TLS_CIPHER_NON_PQ_DEFAULT.isSupported()) {
            return TlsCipherPreference.TLS_CIPHER_NON_PQ_DEFAULT;
        }
        return TlsCipherPreference.TLS_CIPHER_SYSTEM_DEFAULT;
    }

    private static HttpProxyOptions proxyOptions(ProxyConfiguration proxy, TlsContext tlsContext) {
        if (proxy == null) {
            return null;
        }
        HttpProxyOptions options = new HttpProxyOptions();
        options.setHost(proxy.host());
        options.setPort(proxy.port());
        if ("https".equals(proxy.scheme())) {
            options.setTlsContext(tlsContext);
        }
        if (proxy.hasCredentials()) {
            options.setAuthorizationType(HttpProxyOptions.HttpProxyAuthorizationType.Basic);
            options.setAuthorizationUsername(proxy.username().orElseThrow());
            options.setAuthorizationPassword(proxy.password().orElseThrow());
        }
        return options;
    }

    /** The per-stream read window, which bounds response bytes buffered ahead of the consumer. */
    public long readBufferSize() {
        return readBufferSize;
    }

    public String clientName() {
        return clientName;
    }

    /** A leased connection and the pool it returns to. */
    public record Lease(HttpClientConnectionManager manager, HttpClientConnection connection) {
        /** Returns the connection for reuse. */
        public void release() {
            manager.releaseConnection(connection);
        }

        /** Discards the connection: shuts it down, then releases it so the pool forgets it. */
        public void discard() {
            try {
                connection.shutdown();
            } finally {
                manager.releaseConnection(connection);
            }
        }
    }

    /**
     * Acquires a connection to the request's endpoint. The returned future fails with an
     * {@link HttpTransportException} of phase {@link HttpPhase#CONNECTION_ACQUISITION} (or the connect/TLS/proxy
     * phase the CRT reports), or times out after {@code acquisitionTimeout} if present. A connection delivered after
     * the returned future already terminated is released to the pool, so it never leaks.
     *
     * @throws IllegalStateException if the client is closed
     */
    public CompletableFuture<Lease> acquire(SdkHttpRequest request, Optional<Duration> acquisitionTimeout) {
        HttpClientConnectionManager manager = pool(request);
        CompletableFuture<Lease> result = new CompletableFuture<>();
        CompletableFuture<HttpClientConnection> acquired = manager.acquireConnection();
        acquired.whenComplete((connection, failure) -> {
            if (failure != null) {
                result.completeExceptionally(
                    CrtFailureMapping.toTransportException(failure, HttpPhase.CONNECTION_ACQUISITION, clientName));
                return;
            }
            Lease lease = new Lease(manager, connection);
            if (!result.complete(lease)) {
                lease.release();
            }
        });
        acquisitionTimeout.ifPresent(timeout -> {
            CompletableFuture<Lease> timer = new CompletableFuture<Lease>().completeOnTimeout(null, timeout.toNanos(),
                                                                                              TimeUnit.NANOSECONDS);
            timer.whenComplete((ignored, t) -> result.completeExceptionally(
                CrtFailureMapping.toTransportException(new java.util.concurrent.TimeoutException(
                    "No connection was acquired within " + timeout), HttpPhase.CONNECTION_ACQUISITION, clientName)));
        });
        return result;
    }

    private synchronized HttpClientConnectionManager pool(SdkHttpRequest request) {
        if (closed) {
            throw new IllegalStateException(clientName + " is closed");
        }
        String key = request.scheme() + "://" + request.authority();
        Pool pool = pools.get(key);
        if (pool == null) {
            pool = createPool(request);
            pools.put(key, pool);
        }
        return (HttpClientConnectionManager) pool.manager().resource();
    }

    private Pool createPool(SdkHttpRequest request) {
        boolean https = "https".equalsIgnoreCase(request.scheme());
        Owned tlsConnection = null;
        try {
            URI uri = new URI(request.scheme(), null, request.host(), request.port(), null, null, null);
            HttpClientConnectionManagerOptions options =
                new HttpClientConnectionManagerOptions()
                    .withClientBootstrap(bootstrap)
                    .withSocketOptions(socketOptions)
                    .withUri(uri)
                    .withWindowSize(readBufferSize)
                    .withManualWindowManagement(true)
                    .withMaxConnections(settings.maxConcurrencyOrDefault())
                    .withMaxConnectionIdleInMilliseconds(settings.maxIdleTimeOrDefault().toMillis())
                    .withConnectionAcquisitionTimeoutInMilliseconds(settings.acquisitionTimeoutOrDefault().toMillis())
                    .withProxyOptions(proxyOptions);
            if (https) {
                TlsConnectionOptions tls = new TlsConnectionOptions(tlsContext)
                    .withServerName(request.host())
                    .withTimeoutMs(saturatedMillis(settings.tlsNegotiationTimeoutOrDefault()));
                tlsConnection = new Owned(tls, "TlsConnectionOptions[" + request.authority() + "]");
                options.withTlsConnectionOptions(tls);
            }
            HttpClientConnectionManager manager = HttpClientConnectionManager.create(options);
            return new Pool(new Owned(manager, "HttpClientConnectionManager[" + request.scheme() + "://"
                                               + request.authority() + "]"), tlsConnection);
        } catch (URISyntaxException e) {
            if (tlsConnection != null) {
                tlsConnection.close();
            }
            throw new IllegalArgumentException("Invalid endpoint " + request.authority(), e);
        } catch (RuntimeException e) {
            if (tlsConnection != null) {
                tlsConnection.close();
            }
            throw e;
        }
    }

    /**
     * The CRT request of {@code request}: the method, the raw target ({@link SdkHttpRequest#rawTarget()}, never
     * re-encoded) and the header fields in order with repeated fields kept, plus the derived {@code Host} (from the
     * authority) and {@code Content-Length} (for a body of known non-zero length) fields when the caller did not set
     * them.
     */
    public static HttpRequest toCrtRequest(SdkHttpRequest request, HttpRequestBodyStream body) {
        HttpHeaders headers = request.headers();
        List<HttpHeader> fields = new ArrayList<>(headers.size() + 2);
        if (!headers.contains("Host")) {
            fields.add(new HttpHeader("Host", request.authority()));
        }
        for (int i = 0; i < headers.size(); i++) {
            fields.add(new HttpHeader(headers.name(i), headers.value(i)));
        }
        HttpBody requestBody = request.body();
        long length = requestBody.contentLength().orElse(-1);
        if (requestBody.kind() != HttpBody.Kind.EMPTY && !headers.contains("Content-Length")
            && !headers.contains("Transfer-Encoding")) {
            if (length < 0) {
                throw HttpTransportException.builder().phase(HttpPhase.REQUEST_TRANSMISSION)
                                            .category(software.amazon.awssdk.http.HttpFailureCategory
                                                          .REQUEST_TRANSMISSION)
                                            .message("The CRT HTTP/1.1 client needs a known request body length")
                                            .build();
            }
            if (length > 0) {
                fields.add(new HttpHeader("Content-Length", Long.toString(length)));
            }
        }
        return new HttpRequest(request.method(), request.rawTarget(), fields.toArray(new HttpHeader[0]),
                               requestBody.kind() == HttpBody.Kind.EMPTY ? null : body);
    }

    /** The owned client-wide native objects, in acquisition order; for tests and diagnostics. */
    public synchronized List<Owned> ownedResources() {
        return List.copyOf(owned);
    }

    /** The owned per-endpoint native objects created so far; for tests and diagnostics. */
    public synchronized List<Owned> poolResources() {
        List<Owned> out = new ArrayList<>();
        for (Pool pool : pools.values()) {
            out.add(pool.manager());
            if (pool.tlsConnectionOptions() != null) {
                out.add(pool.tlsConnectionOptions());
            }
        }
        return out;
    }

    /**
     * Tracks an in-flight exchange: {@code cancelOnClose} runs if the client closes before {@link #untrack}.
     *
     * @throws IllegalStateException if the client is closed
     */
    public void track(Runnable cancelOnClose) {
        synchronized (this) {
            if (closed) {
                throw new IllegalStateException(clientName + " is closed");
            }
            inFlight.add(cancelOnClose);
        }
    }

    public void untrack(Runnable cancelOnClose) {
        inFlight.remove(cancelOnClose);
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    /** Releases pools and native objects; idempotent. */
    @Override
    public void close() {
        List<Pool> toClose;
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            toClose = new ArrayList<>(pools.values());
            pools.clear();
        }
        for (Runnable cancel : List.copyOf(inFlight)) {
            try {
                cancel.run();
            } catch (RuntimeException e) {
                // Cancelling one exchange must not stop the release of the client.
            }
        }
        inFlight.clear();
        Collections.reverse(toClose);
        for (Pool pool : toClose) {
            closeQuietly(pool.manager());
            if (pool.tlsConnectionOptions() != null) {
                closeQuietly(pool.tlsConnectionOptions());
            }
        }
        closeOwned();
    }

    private void closeOwned() {
        List<Owned> reversed;
        synchronized (this) {
            reversed = new ArrayList<>(owned);
        }
        Collections.reverse(reversed);
        reversed.forEach(CrtClientCore::closeQuietly);
    }

    private static void closeQuietly(Owned o) {
        try {
            o.close();
        } catch (RuntimeException e) {
            // A native release failure must not stop the release of the remaining objects.
        }
    }

    private static int saturatedMillis(Duration d) {
        return (int) Math.min(Integer.MAX_VALUE, d.toMillis());
    }
}
