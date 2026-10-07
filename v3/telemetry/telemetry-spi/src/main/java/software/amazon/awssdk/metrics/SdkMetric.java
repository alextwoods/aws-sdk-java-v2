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

package software.amazon.awssdk.metrics;

import java.util.Set;
import software.amazon.awssdk.metrics.internal.DefaultSdkMetric;

/**
 * A named SDK metric (V2-compatible). Metric names are unique in the JVM: two metrics with the same name cannot be
 * created, and metrics are equal exactly when their names are.
 *
 * @param <T> the type of the metric's values
 */
public interface SdkMetric<T> {
    /** The name of this metric. */
    String name();

    /** The categories of this metric. */
    Set<MetricCategory> categories();

    /** The level of this metric. */
    MetricLevel level();

    /** The class of this metric's values. */
    Class<T> valueClass();

    /**
     * Creates a metric.
     *
     * @param name the metric name
     * @param clzz the class of the metric's values
     * @param level the metric level
     * @param c1 a category of the metric
     * @param cn further categories of the metric
     * @param <T> the value type
     * @return the metric
     * @throws IllegalArgumentException if a metric with the same name already exists
     */
    static <T> SdkMetric<T> create(String name, Class<T> clzz, MetricLevel level, MetricCategory c1,
                                   MetricCategory... cn) {
        return DefaultSdkMetric.create(name, clzz, level, c1, cn);
    }

    /**
     * Creates a metric.
     *
     * @param name the metric name
     * @param clzz the class of the metric's values
     * @param level the metric level
     * @param categories the categories of the metric; not empty
     * @param <T> the value type
     * @return the metric
     * @throws IllegalArgumentException if a metric with the same name already exists
     */
    static <T> SdkMetric<T> create(String name, Class<T> clzz, MetricLevel level, Set<MetricCategory> categories) {
        return DefaultSdkMetric.create(name, clzz, level, categories);
    }
}
