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

package software.amazon.awssdk.metrics.publishers.cloudwatch.internal;

import static java.time.temporal.ChronoUnit.MINUTES;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricLevel;
import software.amazon.awssdk.metrics.MetricRecord;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.awssdk.services.cloudwatch.model.StatisticSet;

/**
 * Aggregates metric collections into one-minute buckets of one aggregate per metric and dimension set, and turns
 * them into {@code PutMetricData} requests within CloudWatch's limits (20 data per request, 300 detailed values per
 * request), as V2's publisher does. Not thread safe: the publisher confines it to one thread. Not a public API.
 */
public final class MetricCollectionAggregator {
    /** CloudWatch's maximum number of metric data per {@code PutMetricData} request. */
    public static final int MAX_METRIC_DATA_PER_REQUEST = 20;
    /** CloudWatch's maximum number of values per {@code PutMetricData} request. */
    public static final int MAX_VALUES_PER_REQUEST = 300;
    private static final double ZERO_THRESHOLD = 0.0001;

    private final String namespace;
    private final Set<SdkMetric<String>> dimensions;
    private final Set<MetricCategory> metricCategories;
    private final boolean allCategories;
    private final MetricLevel metricLevel;
    private final Set<SdkMetric<?>> detailedMetrics;
    private final Map<Instant, Map<Key, Aggregate>> buckets = new LinkedHashMap<>();

    public MetricCollectionAggregator(String namespace, Set<SdkMetric<String>> dimensions,
                                      Set<MetricCategory> metricCategories, MetricLevel metricLevel,
                                      Set<SdkMetric<?>> detailedMetrics) {
        this.namespace = namespace;
        this.dimensions = Set.copyOf(dimensions);
        this.metricCategories = Set.copyOf(metricCategories);
        this.allCategories = metricCategories.contains(MetricCategory.ALL);
        this.metricLevel = metricLevel;
        this.detailedMetrics = Set.copyOf(detailedMetrics);
    }

    /** Adds every reportable metric of {@code collection} and its children to the collection's minute bucket. */
    public void addCollection(MetricCollection collection) {
        Map<Key, Aggregate> bucket = buckets.computeIfAbsent(collection.creationTime().truncatedTo(MINUTES),
                                                             i -> new LinkedHashMap<>());
        add(collection, dimensionsOf(collection), bucket);
    }

    private void add(MetricCollection collection, List<Dimension> callDimensions, Map<Key, Aggregate> bucket) {
        for (MetricRecord<?> record : collection) {
            Double value = valueOf(record);
            if (value != null) {
                Key key = new Key(record.metric(), callDimensions);
                bucket.computeIfAbsent(key, k -> new Aggregate(k, detailedMetrics.contains(k.metric())))
                      .add(normalize(value));
            }
        }
        for (MetricCollection child : collection.children()) {
            add(child, callDimensions, bucket);
        }
    }

    /**
     * The dimensions of a call: the configured dimension metrics of its top-level collection, sorted by name in
     * descending order ({@code ServiceId} before {@code OperationName}), so their reporting order does not matter.
     */
    private List<Dimension> dimensionsOf(MetricCollection collection) {
        List<Dimension> result = new ArrayList<>();
        for (MetricRecord<?> record : collection) {
            if (dimensions.contains(record.metric()) && record.value() != null) {
                result.add(Dimension.builder().name(record.metric().name()).value((String) record.value()).build());
            }
        }
        result.sort(Comparator.comparing(Dimension::name).reversed());
        return List.copyOf(result);
    }

    private Double valueOf(MetricRecord<?> record) {
        SdkMetric<?> metric = record.metric();
        Object value = record.value();
        boolean category = allCategories || metric.categories().stream().anyMatch(metricCategories::contains);
        if (value == null || !category || !metricLevel.includesLevel(metric.level())) {
            return null;
        }
        if (value instanceof Duration duration) {
            return (double) duration.toMillis();
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof Boolean flag) {
            return flag ? 1.0 : 0.0;
        }
        return null;
    }

    private static double normalize(double value) {
        return value > ZERO_THRESHOLD || value < -ZERO_THRESHOLD ? value : 0;
    }

    /** The requests of everything aggregated since the last call; the aggregates are then cleared. */
    public List<PutMetricDataRequest> getRequests() {
        List<PutMetricDataRequest> requests = new ArrayList<>();
        List<MetricDatum> data = new ArrayList<>();
        int values = 0;
        for (Map.Entry<Instant, Map<Key, Aggregate>> bucket : buckets.entrySet()) {
            Instant time = bucket.getKey();
            for (Aggregate aggregate : bucket.getValue().values()) {
                if (!aggregate.detailed) {
                    if (data.size() >= MAX_METRIC_DATA_PER_REQUEST) {
                        requests.add(request(data));
                        data = new ArrayList<>();
                        values = 0;
                    }
                    data.add(aggregate.summary(time));
                    continue;
                }
                List<Map.Entry<Double, Integer>> details = new ArrayList<>(aggregate.counts.entrySet());
                int start = 0;
                while (start < details.size()) {
                    if (data.size() >= MAX_METRIC_DATA_PER_REQUEST || values >= MAX_VALUES_PER_REQUEST) {
                        requests.add(request(data));
                        data = new ArrayList<>();
                        values = 0;
                    }
                    int end = Math.min(details.size(), start + MAX_VALUES_PER_REQUEST - values);
                    data.add(aggregate.detailed(time, details.subList(start, end)));
                    values += end - start;
                    start = end;
                }
            }
        }
        if (!data.isEmpty()) {
            requests.add(request(data));
        }
        buckets.clear();
        return requests;
    }

    private PutMetricDataRequest request(List<MetricDatum> data) {
        return PutMetricDataRequest.builder().namespace(namespace).metricData(List.copyOf(data)).build();
    }

    private record Key(SdkMetric<?> metric, List<Dimension> dimensions) {
    }

    /** The aggregate of one metric and dimension set in one bucket: a statistic set, or per-value counts. */
    private static final class Aggregate {
        private final Key key;
        private final boolean detailed;
        private final StandardUnit unit;
        private final Map<Double, Integer> counts = new LinkedHashMap<>();
        private double min = Double.MAX_VALUE;
        private double max = -Double.MAX_VALUE;
        private double sum;
        private int count;

        private Aggregate(Key key, boolean detailed) {
            this.key = key;
            this.detailed = detailed;
            this.unit = Duration.class.isAssignableFrom(key.metric().valueClass()) ? StandardUnit.MILLISECONDS
                                                                                     : StandardUnit.NONE;
        }

        private void add(double value) {
            if (detailed) {
                counts.merge(value, 1, Integer::sum);
                return;
            }
            min = Math.min(min, value);
            max = Math.max(max, value);
            sum += value;
            count++;
        }

        private MetricDatum summary(Instant time) {
            StatisticSet statistics = StatisticSet.builder()
                                                  .minimum(normalize(min))
                                                  .maximum(normalize(max))
                                                  .sum(normalize(sum))
                                                  .sampleCount((double) count)
                                                  .build();
            return datum(time).statisticValues(statistics).build();
        }

        private MetricDatum detailed(Instant time, List<Map.Entry<Double, Integer>> details) {
            List<Double> values = new ArrayList<>(details.size());
            List<Double> valueCounts = new ArrayList<>(details.size());
            for (Map.Entry<Double, Integer> detail : details) {
                values.add(detail.getKey());
                valueCounts.add((double) detail.getValue());
            }
            return datum(time).values(values).counts(valueCounts).build();
        }

        private MetricDatum.Builder datum(Instant time) {
            return MetricDatum.builder()
                              .timestamp(time)
                              .metricName(key.metric().name())
                              .dimensions(key.dimensions())
                              .unit(unit);
        }
    }
}
