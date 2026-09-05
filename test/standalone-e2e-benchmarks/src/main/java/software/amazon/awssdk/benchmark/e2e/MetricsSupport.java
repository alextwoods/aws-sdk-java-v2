package software.amazon.awssdk.benchmark.e2e;

import java.io.PrintStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.amazonaws.metrics.RequestMetricCollector;
import com.amazonaws.util.AWSRequestMetrics;
import com.amazonaws.util.TimingInfo;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;

import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.MetricRecord;

/**
 * Per-SDK metric aggregation, enabled with {@code --metrics}. Each SDK's native metric facility is
 * used (V2 {@link MetricPublisher}, V1 {@link RequestMetricCollector}, smithy-java OTel plugin) and
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

    /** Aggregates V1 AWSRequestMetrics timing sub-measurements. Concurrent, as above. */
    static final class V1Collector extends RequestMetricCollector {
        private static final String[] FIELDS = {
            "ClientExecuteTime", "CredentialsRequestTime", "RequestMarshallTime", "RequestSigningTime",
            "HttpRequestTime", "HttpClientSendRequestTime", "HttpClientReceiveResponseTime",
            "ResponseProcessingTime", "RetryPauseTime", "HttpClientPoolAcquireTime",
        };

        private final Map<String, AtomicLong> totalNanos = new ConcurrentHashMap<>();
        private final Map<String, AtomicLong> counts = new ConcurrentHashMap<>();

        @Override
        public void collectMetrics(com.amazonaws.Request<?> request, com.amazonaws.Response<?> response) {
            AWSRequestMetrics metrics = request.getAWSRequestMetrics();
            if (metrics == null) {
                return;
            }
            TimingInfo root = metrics.getTimingInfo();
            for (String field : FIELDS) {
                TimingInfo sub = root.getSubMeasurement(field);
                if (sub != null) {
                    Double ms = sub.getTimeTakenMillisIfKnown();
                    if (ms != null) {
                        totalNanos.computeIfAbsent(field, k -> new AtomicLong())
                                  .addAndGet((long) (ms * 1_000_000));
                        counts.computeIfAbsent(field, k -> new AtomicLong()).incrementAndGet();
                    }
                }
            }
        }

        void reset() {
            totalNanos.clear();
            counts.clear();
        }

        void print(PrintStream out) {
            for (Map.Entry<String, AtomicLong> e : new TreeMap<>(totalNanos).entrySet()) {
                long count = counts.get(e.getKey()).get();
                out.printf("METRIC %-42s count=%,d totalMs=%,.1f avgUs=%,.2f%n",
                           e.getKey(), count, e.getValue().get() / 1e6,
                           (double) e.getValue().get() / count / 1e3);
            }
        }
    }

    /** In-memory OTel pipeline for the smithy-java OperationMetricsPlugin. */
    static final class OtelHolder {
        final InMemoryMetricReader reader = InMemoryMetricReader.createDelta();
        final OpenTelemetry otel = OpenTelemetrySdk.builder()
            .setMeterProvider(SdkMeterProvider.builder().registerMetricReader(reader).build())
            .build();

        void reset() {
            reader.collectAllMetrics(); // delta reader: discard everything so far
        }

        void print(PrintStream out) {
            Map<String, MetricData> sorted = new TreeMap<>();
            for (MetricData m : reader.collectAllMetrics()) {
                sorted.put(m.getName(), m);
            }
            for (MetricData md : sorted.values()) {
                if (md.getType() != MetricDataType.HISTOGRAM) {
                    continue;
                }
                double sum = 0;
                long count = 0;
                for (HistogramPointData p : md.getHistogramData().getPoints()) {
                    sum += p.getSum();
                    count += p.getCount();
                }
                if (count == 0) {
                    continue;
                }
                if ("s".equals(md.getUnit())) {
                    out.printf("METRIC %-42s count=%,d totalMs=%,.1f avgUs=%,.2f%n",
                               md.getName(), count, sum * 1e3, sum / count * 1e6);
                } else {
                    out.printf("METRIC %-42s count=%,d total=%,.0f avg=%,.0f (%s)%n",
                               md.getName(), count, sum, sum / count, md.getUnit());
                }
            }
        }
    }
}
