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

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * An immutable collection of metrics (V2-compatible).
 */
public interface MetricCollection extends Iterable<MetricRecord<?>> {
    /** The name of this metric collection. */
    String name();

    /** The records of this collection. */
    default Stream<MetricRecord<?>> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * Every value of {@code metric} in this collection, in reporting order.
     *
     * @param metric the metric
     * @param <T> the value type
     * @return the values; empty if the metric was not reported
     */
    <T> List<T> metricValues(SdkMetric<T> metric);

    /** The child metric collections. */
    List<MetricCollection> children();

    /**
     * The {@link #children()} named {@code name}.
     *
     * @param name the child name to select
     * @return the children with that name
     */
    default Stream<MetricCollection> childrenWithName(String name) {
        return children().stream().filter(c -> c.name().equals(name));
    }

    /** The time this collection was created. */
    Instant creationTime();
}
