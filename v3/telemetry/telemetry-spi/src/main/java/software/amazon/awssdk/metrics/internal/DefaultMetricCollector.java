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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricRecord;
import software.amazon.awssdk.metrics.SdkMetric;

/**
 * The thread-safe {@link MetricCollector} implementation. Not a public API.
 */
public final class DefaultMetricCollector implements MetricCollector {
    private final String name;
    private final Map<SdkMetric<?>, List<MetricRecord<?>>> metrics = new LinkedHashMap<>();
    private final List<MetricCollector> children = new ArrayList<>();

    public DefaultMetricCollector(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public synchronized <T> void reportMetric(SdkMetric<T> metric, T data) {
        metrics.computeIfAbsent(metric, m -> new ArrayList<>()).add(new DefaultMetricRecord<>(metric, data));
    }

    @Override
    public synchronized MetricCollector createChild(String name) {
        MetricCollector child = new DefaultMetricCollector(name);
        children.add(child);
        return child;
    }

    @Override
    public synchronized MetricCollection collect() {
        List<MetricCollection> collected = new ArrayList<>(children.size());
        for (MetricCollector child : children) {
            collected.add(child.collect());
        }
        return new DefaultMetricCollection(name, metrics, collected);
    }

    /**
     * A new collector named {@code name}.
     *
     * @throws IllegalArgumentException if {@code name} is null or empty
     */
    public static MetricCollector create(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name must not be empty");
        }
        return new DefaultMetricCollector(name);
    }

    @Override
    public synchronized String toString() {
        return "DefaultMetricCollector(metrics=" + metrics + ")";
    }
}
