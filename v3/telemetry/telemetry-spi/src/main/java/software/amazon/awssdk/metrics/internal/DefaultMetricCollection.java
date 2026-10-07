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

package software.amazon.awssdk.metrics.internal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricRecord;
import software.amazon.awssdk.metrics.SdkMetric;

/**
 * The immutable {@link MetricCollection} implementation: records grouped by metric in first-reporting order.
 * Not a public API.
 */
public final class DefaultMetricCollection implements MetricCollection {
    private final String name;
    private final Map<SdkMetric<?>, List<MetricRecord<?>>> metrics;
    private final List<MetricCollection> children;
    private final Instant creationTime;

    public DefaultMetricCollection(String name, Map<SdkMetric<?>, List<MetricRecord<?>>> metrics,
                                   List<MetricCollection> children) {
        this(name, metrics, children, Instant.now());
    }

    public DefaultMetricCollection(String name, Map<SdkMetric<?>, List<MetricRecord<?>>> metrics,
                                   List<MetricCollection> children, Instant creationTime) {
        this.name = name;
        Map<SdkMetric<?>, List<MetricRecord<?>>> copy = new LinkedHashMap<>();
        metrics.forEach((metric, records) -> copy.put(metric, List.copyOf(records)));
        this.metrics = Collections.unmodifiableMap(copy);
        this.children = children == null ? List.of() : List.copyOf(children);
        this.creationTime = creationTime;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> metricValues(SdkMetric<T> metric) {
        List<MetricRecord<?>> records = metrics.get(metric);
        if (records == null) {
            return List.of();
        }
        List<T> values = new ArrayList<>(records.size());
        for (MetricRecord<?> record : records) {
            values.add((T) record.value());
        }
        return Collections.unmodifiableList(values);
    }

    @Override
    public List<MetricCollection> children() {
        return children;
    }

    @Override
    public Instant creationTime() {
        return creationTime;
    }

    @Override
    public Iterator<MetricRecord<?>> iterator() {
        return metrics.values().stream().<MetricRecord<?>>flatMap(List::stream).iterator();
    }

    @Override
    public String toString() {
        List<MetricRecord<?>> records = new ArrayList<>();
        metrics.values().forEach(records::addAll);
        return "MetricCollection(name=" + name + ", metrics=" + records + ", children=" + children + ")";
    }
}
