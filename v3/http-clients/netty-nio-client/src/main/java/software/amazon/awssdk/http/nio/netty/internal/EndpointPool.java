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

package software.amazon.awssdk.http.nio.netty.internal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.Protocol;

/**
 * The connections of one {@link Endpoint}, without pool decorators: a set of open connections, the ones with a free
 * slot (LIFO, so warm connections are reused first), a bounded FIFO of waiters and a count of connections being
 * opened.
 *
 * <p>Bounds: at most {@code maxConcurrency} leased slots and at most {@code maxConcurrency} connections (HTTP/1.1:
 * one slot each; HTTP/2: {@code http2MaxStreams} slots each, so fewer connections are opened), at most
 * {@code maxPendingConnectionAcquires} waiters; beyond that acquisition fails at once
 * ({@link NettyFailures#poolExhausted}).
 *
 * <p>Threading: decisions are made under the pool lock, which is held only for constant-time bookkeeping and never
 * across I/O or a callback, so taking it on an event loop does not block the loop. Connections are opened, handed to
 * waiters and closed outside the lock. A connection whose channel closes leaves the pool through its close listener,
 * the only place that forgets it, so capacity is returned exactly once.
 *
 * <p>Lifetimes: a connection older than {@code connectionTimeToLive} is not leased again and is closed on release;
 * a connection idle for {@code connectionMaxIdleTime} is closed by a timer on its event loop.
 */
public final class EndpointPool {
    /** An acquisition in progress. Callbacks run outside the pool lock, on arbitrary threads. */
    public interface Waiter {
        /** The phase of the connection opened for this waiter. */
        void phase(HttpPhase phase);

        /** A slot of {@code connection} is leased to this waiter; false if it no longer wants it (then released). */
        boolean acquired(Connection connection);

        /** Acquisition failed. */
        void failed(HttpTransportException failure);

        /** The waiter's current phase, for mapping connect failures. */
        HttpPhase currentPhase();
    }

    private final Endpoint endpoint;
    private final ClientConfig config;
    private final ChannelSetup setup;
    private final Set<Connection> open = new LinkedHashSet<>();
    private final ArrayDeque<Connection> available = new ArrayDeque<>();
    private final ArrayDeque<Waiter> waiters = new ArrayDeque<>();
    private final boolean http2Preferred;
    private int connecting;
    private int leased;
    private boolean closed;

    public EndpointPool(Endpoint endpoint, ClientConfig config, ChannelSetup setup) {
        this.endpoint = endpoint;
        this.config = config;
        this.setup = setup;
        // With HTTP/2 over TLS one connection is opened at a time: it usually serves many streams.
        this.http2Preferred = config.protocol() == Protocol.HTTP2 && endpoint.secure();
    }

    /** Leases a slot to {@code waiter}, opening a connection if allowed, else queues it or fails it. */
    public void acquire(Waiter waiter) {
        Connection lease = null;
        boolean connect = false;
        HttpTransportException failure = null;
        List<Connection> expired = new ArrayList<>();
        synchronized (this) {
            if (closed) {
                failure = NettyFailures.closed(HttpPhase.CONNECTION_ACQUISITION, config.clientName());
            } else if (leased < config.maxConcurrency()) {
                lease = pollAvailable(expired);
                if (lease == null && open.size() + connecting < config.maxConcurrency()
                    && !(http2Preferred && connecting > 0)) {
                    connecting++;
                    connect = true;
                }
            }
            if (lease != null) {
                leased++;
            } else if (!connect && failure == null) {
                if (waiters.size() < config.maxPendingConnectionAcquires()) {
                    waiters.add(waiter);
                } else {
                    failure = NettyFailures.poolExhausted(config.maxPendingConnectionAcquires(), config.clientName());
                }
            }
        }
        expired.forEach(c -> c.channel().close());
        if (failure != null) {
            waiter.failed(failure);
        } else if (lease != null) {
            deliver(waiter, lease);
        } else if (connect) {
            open(waiter);
        }
    }

    /** Removes a waiter that gave up (cancelled or timed out); true if it was queued. */
    public synchronized boolean abandon(Waiter waiter) {
        return waiters.remove(waiter);
    }

    /**
     * Returns a slot of {@code connection}. A non-reusable slot closes the connection (HTTP/1.1) or just frees the
     * stream slot (HTTP/2, where the stream channel was closed by the exchange).
     */
    public void release(Connection connection, boolean reusable) {
        boolean close = false;
        Waiter next = null;
        synchronized (this) {
            connection.leased--;
            leased--;
            boolean dead = closed || !connection.channel().isActive() || !open.contains(connection);
            boolean retire = dead || expired(connection) || !reusable && !connection.isHttp2();
            if (retire) {
                available.remove(connection);
                // An expiring HTTP/2 connection is closed once its last stream ends.
                close = dead || connection.leased == 0;
            } else {
                next = waiters.poll();
                if (next != null) {
                    connection.leased++;
                    leased++;
                } else if (connection.leased < connection.capacity() && !available.contains(connection)) {
                    available.push(connection);
                }
                if (connection.leased == 0) {
                    connection.idleSinceNanos = System.nanoTime();
                    scheduleIdleClose(connection);
                }
            }
        }
        if (close) {
            // The close listener forgets the connection and serves the next waiter with a new connection.
            connection.channel().close();
        } else if (next != null) {
            deliver(next, connection);
        } else if (!connection.isHttp2() && connection.leased == 0 && connection.channel().isActive()) {
            // Keep one read pending on an idle HTTP/1.1 connection so a server close is noticed.
            connection.channel().read();
        }
    }

    /** Closes every connection and fails every waiter; idempotent. */
    public void close() {
        List<Waiter> failed;
        List<Connection> connections;
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            failed = new ArrayList<>(waiters);
            waiters.clear();
            connections = new ArrayList<>(open);
            available.clear();
        }
        for (Waiter waiter : failed) {
            waiter.failed(NettyFailures.closed(HttpPhase.CONNECTION_ACQUISITION, config.clientName()));
        }
        connections.forEach(c -> c.channel().close());
    }

    /** Open connections, for tests. */
    public synchronized int openConnections() {
        return open.size();
    }

    private Connection pollAvailable(List<Connection> expired) {
        for (Iterator<Connection> it = available.iterator(); it.hasNext(); ) {
            Connection c = it.next();
            if (!c.channel().isActive() || expired(c) || idleTooLong(c)) {
                it.remove();
                if (c.leased == 0) {
                    expired.add(c);
                }
                continue;
            }
            c.leased++;
            if (c.idleTimer != null) {
                c.idleTimer.cancel(false);
                c.idleTimer = null;
            }
            if (c.leased >= c.capacity()) {
                it.remove();
            }
            return c;
        }
        return null;
    }

    private boolean expired(Connection c) {
        return config.connectionTimeToLive() != null
               && System.nanoTime() - c.createdNanos() >= config.connectionTimeToLive().toNanos();
    }

    private boolean idleTooLong(Connection c) {
        return c.leased == 0 && config.connectionMaxIdleTime() != null
               && System.nanoTime() - c.idleSinceNanos >= config.connectionMaxIdleTime().toNanos();
    }

    private void scheduleIdleClose(Connection c) {
        if (config.connectionMaxIdleTime() == null) {
            return;
        }
        if (c.idleTimer != null) {
            c.idleTimer.cancel(false);
        }
        long idleNanos = config.connectionMaxIdleTime().toNanos();
        c.idleTimer = c.channel().eventLoop().schedule(() -> {
            boolean close;
            synchronized (this) {
                close = c.leased == 0 && System.nanoTime() - c.idleSinceNanos >= idleNanos;
                if (close) {
                    available.remove(c);
                }
            }
            if (close) {
                c.channel().close();
            }
        }, idleNanos, TimeUnit.NANOSECONDS);
    }

    private void open(Waiter waiter) {
        setup.connect(endpoint, waiter::phase).addListener(f -> {
            if (!f.isSuccess()) {
                synchronized (this) {
                    connecting--;
                }
                waiter.failed(NettyFailures.map(f.cause(), waiter.currentPhase(), config.clientName()));
                serveWaiters();
                return;
            }
            Connection connection = (Connection) f.getNow();
            boolean accepted;
            synchronized (this) {
                connecting--;
                accepted = !closed;
                if (accepted) {
                    open.add(connection);
                    connection.leased++;
                    leased++;
                }
            }
            if (!accepted) {
                connection.channel().close();
                waiter.failed(NettyFailures.closed(HttpPhase.CONNECTION_ACQUISITION, config.clientName()));
                return;
            }
            connection.channel().closeFuture().addListener(c -> forget(connection));
            deliver(waiter, connection);
            if (connection.isHttp2()) {
                offerSlots(connection);
            } else {
                serveWaiters();
            }
        });
    }

    /** Leases the free stream slots of a new HTTP/2 connection to queued waiters. */
    private void offerSlots(Connection connection) {
        List<Waiter> served = new ArrayList<>();
        synchronized (this) {
            while (connection.leased < connection.capacity() && leased < config.maxConcurrency()
                   && !waiters.isEmpty()) {
                served.add(waiters.poll());
                connection.leased++;
                leased++;
            }
            if (connection.leased < connection.capacity() && !available.contains(connection)) {
                available.push(connection);
            }
        }
        served.forEach(w -> deliver(w, connection));
    }

    private void deliver(Waiter waiter, Connection connection) {
        if (!waiter.acquired(connection)) {
            release(connection, true);
        }
    }

    private void forget(Connection connection) {
        synchronized (this) {
            open.remove(connection);
            available.remove(connection);
            if (connection.idleTimer != null) {
                connection.idleTimer.cancel(false);
            }
        }
        serveWaiters();
    }

    /** Opens a connection for the first waiter if capacity allows (after a close or a failed connect). */
    private void serveWaiters() {
        Waiter next;
        synchronized (this) {
            if (closed || waiters.isEmpty() || open.size() + connecting >= config.maxConcurrency()
                || leased >= config.maxConcurrency() || http2Preferred && connecting > 0) {
                return;
            }
            next = waiters.poll();
            connecting++;
        }
        open(next);
    }
}
