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

import software.amazon.awssdk.metrics.internal.DefaultMetricCollector;

/**
 * Collects metrics into a {@link MetricCollection} (V2-compatible). Implementations created by {@link #create} are
 * thread safe.
 */
public interface MetricCollector {
    /** The name of this collector, which becomes the collection's name. */
    String name();

    /** Reports one value of {@code metric}. */
    <T> void reportMetric(SdkMetric<T> metric, T data);

    /**
     * Creates a child of this collector, whose collection becomes a child of this collector's collection.
     *
     * @param name the name of the child collector
     * @return the child collector
     */
    MetricCollector createChild(String name);

    /**
     * The collected metrics, including every child's.
     *
     * @return the collected metrics
     */
    MetricCollection collect();

    /**
     * A new collector named {@code name}.
     *
     * @throws IllegalArgumentException if {@code name} is empty
     */
    static MetricCollector create(String name) {
        return DefaultMetricCollector.create(name);
    }
}
