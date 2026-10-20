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

package software.amazon.awssdk.benchmark.e2e;

import java.io.PrintStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.MetricRecord;

/**
 * Per-SDK metric aggregation, enabled with {@code --metrics}. Each SDK's native metric facility is
 * used (V2 {@link MetricPublisher}) and
 * summaries are printed as {@code METRIC name count total avg} lines to the configured sink.
 */
final class MetricsSupport {

    private MetricsSupport() {
    }

    /**
     * Aggregates every Duration-valued V2 metric in the collection tree, keyed by metric name.
     *
     * <p>Concurrent by necessity: at concurrency above 1 the SDK publishes from every caller thread
     * (and, for async clients, from completion threads). The maps were plain {@link HashMap}s while
     * the harness was single-threaded, which surfaced as a {@code ConcurrentModificationException}
     * inside {@code computeIfAbsent} as soon as more than one operation was in flight.
     */
    static final class V2Publisher implements MetricPublisher {
        private final Map<String, AtomicLong> totalNanos = new ConcurrentHashMap<>();
        private final Map<String, AtomicLong> counts = new ConcurrentHashMap<>();

        @Override
        public void publish(MetricCollection collection) {
            walk(collection);
        }

        private void walk(MetricCollection collection) {
            for (MetricRecord<?> record : collection) {
                if (record.value() instanceof Duration duration) {
                    String name = record.metric().name();
                    totalNanos.computeIfAbsent(name, k -> new AtomicLong()).addAndGet(duration.toNanos());
                    counts.computeIfAbsent(name, k -> new AtomicLong()).incrementAndGet();
                }
            }
            for (MetricCollection child : collection.children()) {
                walk(child);
            }
        }

        void reset() {
            totalNanos.clear();
            counts.clear();
        }

        /**
         * Mean of one Duration metric in µs since the last {@link #reset()}, or -1 when it was not reported. Used for
         * the {@code marshalling_us} and {@code signing_us} results columns: {@code MARSHALLING_DURATION} is request
         * marshalling only, and SigV4's payload SHA-256 is inside {@code SIGNING_DURATION}, so the two columns
         * separate serde cost from payload hashing (Requirement 34.2).
         */
        double meanMicros(String metricName) {
            AtomicLong total = totalNanos.get(metricName);
            AtomicLong count = counts.get(metricName);
            if (total == null || count == null || count.get() == 0) {
                return -1;
            }
            return (double) total.get() / count.get() / 1e3;
        }

        void print(PrintStream out) {
            for (Map.Entry<String, AtomicLong> e : new TreeMap<>(totalNanos).entrySet()) {
                long count = counts.get(e.getKey()).get();
                out.printf("METRIC %-42s count=%,d totalMs=%,.1f avgUs=%,.2f%n",
                           e.getKey(), count, e.getValue().get() / 1e6,
                           (double) e.getValue().get() / count / 1e3);
            }
        }

        @Override
        public void close() {
        }
    }

    /**
     * Records the largest request {@code Content-Length} sent, for the {@code request_body_bytes} results column, so
     * a results row states whether its request body was at least 100 KB (Requirement 34.2). Registered only with
     * {@code --metrics}, as the publisher is, so timing runs carry neither.
     */
    static final class RequestSizeInterceptor implements ExecutionInterceptor {
        private final AtomicLong maxBytes = new AtomicLong(-1);

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            // The header when a stage has set it; otherwise the body's own length (sync, then async).
            long bytes = context.httpRequest().firstMatchingHeader("Content-Length")
                                .map(value -> Long.parseLong(value.trim()))
                                .orElseGet(() -> context.requestBody()
                                                        .flatMap(RequestBody::optionalContentLength)
                                                        .orElseGet(() -> context.asyncRequestBody()
                                                                                .flatMap(AsyncRequestBody::contentLength)
                                                                                .orElse(-1L)));
            maxBytes.accumulateAndGet(bytes, Math::max);
        }

        long maxBytes() {
            return maxBytes.get();
        }

        void reset() {
            maxBytes.set(-1);
        }
    }

}
