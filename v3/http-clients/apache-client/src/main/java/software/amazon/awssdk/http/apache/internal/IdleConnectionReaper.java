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

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

/**
 * The idle-connection reaper of one client: a daemon thread that periodically closes expired connections and
 * connections idle longer than {@code connectionMaxIdleTime}. Unlike V2's process-wide singleton, it belongs to one
 * client and is registered with its construction as an SDK-owned resource; {@link #close()} is idempotent.
 */
final class IdleConnectionReaper implements AutoCloseable {
    static final String THREAD_NAME = "aws-sdk-apache-idle-connection-reaper";
    private static final long MAX_PERIOD_MILLIS = 60_000;
    private static final long MIN_PERIOD_MILLIS = 100;

    private final ScheduledExecutorService executor;

    IdleConnectionReaper(PoolingHttpClientConnectionManager manager, Duration maxIdle) {
        long maxIdleMillis = maxIdle.toMillis();
        long period = Math.max(MIN_PERIOD_MILLIS, Math.min(MAX_PERIOD_MILLIS, maxIdleMillis));
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, THREAD_NAME);
            t.setDaemon(true);
            return t;
        });
        executor.scheduleWithFixedDelay(() -> {
            try {
                manager.closeExpiredConnections();
                manager.closeIdleConnections(maxIdleMillis, TimeUnit.MILLISECONDS);
            } catch (RuntimeException ignored) {
                // A failed sweep is retried on the next period.
            }
        }, period, period, TimeUnit.MILLISECONDS);
    }

    boolean isClosed() {
        return executor.isShutdown();
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    @Override
    public String toString() {
        return "IdleConnectionReaper[closed=" + isClosed() + "]";
    }
}
