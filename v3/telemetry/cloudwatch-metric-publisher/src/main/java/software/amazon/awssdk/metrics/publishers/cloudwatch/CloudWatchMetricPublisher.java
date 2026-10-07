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

package software.amazon.awssdk.metrics.publishers.cloudwatch;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricLevel;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.metrics.publishers.cloudwatch.internal.CloudWatchLog;
import software.amazon.awssdk.metrics.publishers.cloudwatch.internal.MetricCollectionAggregator;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;

/**
 * A {@link MetricPublisher} that aggregates SDK metrics and uploads them to Amazon CloudWatch (V2-compatible; the
 * separate opt-in {@code software.amazon.awssdk:cloudwatch-metric-publisher} exporter artifact, Requirement 15.7).
 *
 * <p>Configure it on a client with {@code addMetricPublisher} (or
 * {@code TelemetryConfiguration.Builder#addMetricPublisher}). It aggregates the collections it receives into one-minute
 * buckets per metric and dimension set ({@link Builder#dimensions}: by default the service ID and operation name),
 * summarizing each metric as a CloudWatch statistic set (or, for {@link Builder#detailedMetrics}, as value/count
 * pairs), and uploads them with the configured {@link CloudWatchAsyncClient} every {@link Builder#uploadFrequency}.
 * Only metrics whose category ({@link Builder#metricCategories}) and level ({@link Builder#metricLevel}) are enabled,
 * and whose values are durations (milliseconds), numbers or booleans (one or zero), are uploaded.
 *
 * <p>{@link #publish} never blocks the calling thread: aggregation runs on one internal thread, behind a bounded
 * {@link Builder#taskQueue}; a full queue drops the collection and logs one warning. {@link #close()} uploads the
 * pending metrics, waits for the upload (up to a minute per step) and closes the CloudWatch client if the publisher
 * created it. Build the publisher's CloudWatch client without this publisher, so its own uploads are not measured.
 *
 * <p>Differences from V2: requests carry no {@code hll/cw-mp} user-agent API name, and background failures are logged
 * through the SDK logging facade ({@code software.amazon.awssdk.utils.Logger}, over JDK platform logging) under V2's
 * logger name {@code software.amazon.awssdk.metrics.publishers.cloudwatch}, at {@code WARN} once per category and
 * publisher, then at {@code DEBUG}, where V2 logs every occurrence (shutdown failures at {@code ERROR}).
 */
public final class CloudWatchMetricPublisher implements MetricPublisher {
    private static final int DEFAULT_TASK_QUEUE_SIZE = 128;
    private static final String DEFAULT_NAMESPACE = "AwsSdk/JavaSdk2";
    private static final int DEFAULT_MAXIMUM_CALLS_PER_UPLOAD = 10;
    private static final Duration DEFAULT_UPLOAD_FREQUENCY = Duration.ofMinutes(1);
    private static final Duration CLOSE_STEP_TIMEOUT = Duration.ofSeconds(60);

    private final boolean closeClientWithPublisher;
    private final CloudWatchAsyncClient client;
    private final MetricCollectionAggregator aggregator;
    private final ExecutorService executor;
    private final ScheduledExecutorService scheduledExecutor;
    private final int maximumCallsPerUpload;
    /** This publisher's warning suppression scope: each background failure category warns once. */
    private final CloudWatchLog log = new CloudWatchLog();

    private CloudWatchMetricPublisher(Builder builder) {
        this.closeClientWithPublisher = builder.client == null;
        this.aggregator = new MetricCollectionAggregator(
            builder.namespace == null ? DEFAULT_NAMESPACE : builder.namespace,
            builder.dimensions == null ? Set.of(CoreMetric.SERVICE_ID, CoreMetric.OPERATION_NAME)
                                       : new LinkedHashSet<>(builder.dimensions),
            builder.metricCategories == null ? Set.of(MetricCategory.ALL) : new LinkedHashSet<>(builder.metricCategories),
            builder.metricLevel == null ? MetricLevel.INFO : builder.metricLevel,
            builder.detailedMetrics == null ? Set.of() : new LinkedHashSet<>(builder.detailedMetrics));
        this.maximumCallsPerUpload = builder.maximumCallsPerUpload == null ? DEFAULT_MAXIMUM_CALLS_PER_UPLOAD
                                                                           : builder.maximumCallsPerUpload;
        if (maximumCallsPerUpload < 1) {
            throw new IllegalArgumentException("maximumCallsPerUpload must be positive, was " + maximumCallsPerUpload);
        }
        Duration uploadFrequency = builder.uploadFrequency == null ? DEFAULT_UPLOAD_FREQUENCY : builder.uploadFrequency;
        if (uploadFrequency.isNegative() || uploadFrequency.isZero()) {
            throw new IllegalArgumentException("uploadFrequency must be positive, was " + uploadFrequency);
        }
        this.client = builder.client == null ? CloudWatchAsyncClient.create() : builder.client;

        ThreadFactory threads = daemonThreads("cloud-watch-metric-publisher");
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threads);
        // One thread only: the aggregator is not thread safe.
        this.executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                                               builder.taskQueue == null
                                               ? new ArrayBlockingQueue<>(DEFAULT_TASK_QUEUE_SIZE) : builder.taskQueue,
                                               threads);
        long millis = uploadFrequency.toMillis();
        scheduledExecutor.scheduleAtFixedRate(this::flushQuietly, millis, millis, TimeUnit.MILLISECONDS);
    }

    /** A builder of a publisher. */
    public static Builder builder() {
        return new Builder();
    }

    /** A publisher with every default, uploading through a new {@link CloudWatchAsyncClient} it owns. */
    public static CloudWatchMetricPublisher create() {
        return builder().build();
    }

    @Override
    public void publish(MetricCollection metricCollection) {
        if (metricCollection == null) {
            throw new IllegalArgumentException("metricCollection must not be null");
        }
        try {
            executor.execute(() -> aggregator.addCollection(metricCollection));
        } catch (RejectedExecutionException e) {
            log.warnOnce("dropped", () -> "Some AWS SDK client-side metrics have been dropped because an "
                                          + "internal executor did not accept them. This usually occurs "
                                          + "because the publisher has been closed or the application "
                                          + "produces more calls than the publisher can aggregate in time.",
                         e);
        }
    }

    /** Submits one upload of everything aggregated so far; its outer future completes when the upload starts. */
    private Future<CompletableFuture<Void>> flush() throws InterruptedException {
        while (!executor.isShutdown()) {
            try {
                return executor.submit(this::upload);
            } catch (RejectedExecutionException e) {
                Thread.sleep(100);
            }
        }
        return CompletableFuture.completedFuture(CompletableFuture.completedFuture(null));
    }

    private CompletableFuture<Void> upload() {
        try {
            List<PutMetricDataRequest> requests = aggregator.getRequests();
            if (requests.size() > maximumCallsPerUpload) {
                int all = requests.size();
                log.warnOnce("max-calls", () -> "Maximum AWS SDK client-side metric call count exceeded: "
                                                + all + " > " + maximumCallsPerUpload + ". Some metric "
                                                + "requests will be dropped. Configure fewer metrics or "
                                                + "dimensions, or raise maximumCallsPerUpload.", null);
                requests = requests.subList(0, maximumCallsPerUpload);
            }
            CompletableFuture<?>[] calls = new CompletableFuture<?>[requests.size()];
            for (int i = 0; i < calls.length; i++) {
                calls[i] = client.putMetricData(requests.get(i));
            }
            int count = calls.length;
            return CompletableFuture.allOf(calls).whenComplete((ignored, failure) -> {
                if (failure != null) {
                    log.warnOnce("upload", () -> "Failed while publishing some or all AWS SDK client-side "
                                                 + "metrics to CloudWatch.", failure);
                } else {
                    log.debug(() -> "Published " + count + " AWS SDK client-side metric requests to "
                                    + "CloudWatch.");
                }
            });
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private void flushQuietly() {
        try {
            flush();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Uploads the pending metrics, waits for the upload, shuts the publisher's threads down and closes the CloudWatch
     * client if the publisher created it. Failures are logged, never thrown.
     */
    @Override
    public void close() {
        try {
            scheduledExecutor.shutdownNow();
            Future<CompletableFuture<Void>> flushed = flush();
            executor.shutdown();
            long timeout = CLOSE_STEP_TIMEOUT.toMillis();
            flushed.get(timeout, TimeUnit.MILLISECONDS).get(timeout, TimeUnit.MILLISECONDS);
            if (!executor.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
                throw new TimeoutException("The internal executor did not shut down in " + CLOSE_STEP_TIMEOUT);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warnOnce("close", () -> "Interrupted during graceful metric publisher shutdown.", e);
        } catch (ExecutionException | TimeoutException e) {
            log.warnOnce("close", () -> "Failed during graceful metric publisher shutdown.", e);
        } finally {
            scheduledExecutor.shutdownNow();
            executor.shutdownNow();
            if (closeClientWithPublisher) {
                try {
                    client.close();
                } catch (Exception e) {
                    log.warnOnce("close-client", () -> "Failed to close the CloudWatch client.", e);
                }
            }
        }
    }

    /** Whether both internal executors are shut down; for tests. */
    boolean isShutdown() {
        return scheduledExecutor.isShutdown() && executor.isShutdown();
    }

    private static ThreadFactory daemonThreads(String prefix) {
        AtomicInteger count = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + "-" + count.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    /** Builder of a {@link CloudWatchMetricPublisher}. Not thread safe. */
    public static final class Builder {
        private CloudWatchAsyncClient client;
        private Duration uploadFrequency;
        private String namespace;
        private Integer maximumCallsPerUpload;
        private Collection<SdkMetric<String>> dimensions;
        private Collection<MetricCategory> metricCategories;
        private MetricLevel metricLevel;
        private Collection<SdkMetric<?>> detailedMetrics;
        private BlockingQueue<Runnable> taskQueue;

        private Builder() {
        }

        /** The CloudWatch namespace of the uploaded metrics; {@code AwsSdk/JavaSdk2} by default, as in V2. */
        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        /**
         * The caller-owned client the metrics are uploaded with, which the publisher never closes. By default the
         * publisher creates (and closes) one with {@link CloudWatchAsyncClient#create()}.
         */
        public Builder cloudWatchClient(CloudWatchAsyncClient client) {
            this.client = client;
            return this;
        }

        /** How often the aggregated metrics are uploaded; one minute by default. */
        public Builder uploadFrequency(Duration uploadFrequency) {
            this.uploadFrequency = uploadFrequency;
            return this;
        }

        /** The maximum number of {@code PutMetricData} calls per upload; 10 by default. */
        public Builder maximumCallsPerUpload(Integer maximumCallsPerUpload) {
            this.maximumCallsPerUpload = maximumCallsPerUpload;
            return this;
        }

        /** The queue of pending aggregation tasks; a bounded queue of 128 by default. */
        public Builder taskQueue(BlockingQueue<Runnable> taskQueue) {
            this.taskQueue = taskQueue;
            return this;
        }

        /** The string metrics that become CloudWatch dimensions; the service ID and operation name by default. */
        public Builder dimensions(Collection<SdkMetric<String>> dimensions) {
            this.dimensions = new ArrayList<>(Objects.requireNonNull(dimensions, "dimensions"));
            return this;
        }

        /** {@link #dimensions(Collection)}. */
        @SafeVarargs
        public final Builder dimensions(SdkMetric<String>... dimensions) {
            return dimensions(Arrays.asList(dimensions));
        }

        /** The metric categories that are uploaded; {@link MetricCategory#ALL} by default. */
        public Builder metricCategories(Collection<MetricCategory> metricCategories) {
            this.metricCategories = new ArrayList<>(Objects.requireNonNull(metricCategories, "metricCategories"));
            return this;
        }

        /** {@link #metricCategories(Collection)}. */
        public Builder metricCategories(MetricCategory... metricCategories) {
            return metricCategories(Arrays.asList(metricCategories));
        }

        /** The most verbose metric level that is uploaded; {@link MetricLevel#INFO} by default. */
        public Builder metricLevel(MetricLevel metricLevel) {
            this.metricLevel = metricLevel;
            return this;
        }

        /** The metrics uploaded as value/count pairs instead of statistic sets; none by default. */
        public Builder detailedMetrics(Collection<SdkMetric<?>> detailedMetrics) {
            this.detailedMetrics = new ArrayList<>(Objects.requireNonNull(detailedMetrics, "detailedMetrics"));
            return this;
        }

        /** {@link #detailedMetrics(Collection)}. */
        public Builder detailedMetrics(SdkMetric<?>... detailedMetrics) {
            return detailedMetrics(Arrays.asList(detailedMetrics));
        }

        /**
         * Builds the publisher and starts its upload schedule.
         *
         * @throws IllegalArgumentException if the upload frequency or maximum calls per upload is not positive
         */
        public CloudWatchMetricPublisher build() {
            return new CloudWatchMetricPublisher(this);
        }
    }
}
