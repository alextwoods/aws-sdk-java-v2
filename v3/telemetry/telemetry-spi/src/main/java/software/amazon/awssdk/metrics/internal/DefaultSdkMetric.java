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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricLevel;
import software.amazon.awssdk.metrics.SdkMetric;

/**
 * The {@link SdkMetric} implementation: equal by name, and each name created at most once per JVM. Not a public API.
 *
 * @param <T> the value type
 */
public final class DefaultSdkMetric<T> implements SdkMetric<T> {
    private static final Set<String> NAMES = ConcurrentHashMap.newKeySet();

    private final String name;
    private final Class<T> valueClass;
    private final MetricLevel level;
    private final Set<MetricCategory> categories;

    private DefaultSdkMetric(String name, Class<T> valueClass, MetricLevel level, Set<MetricCategory> categories) {
        this.name = name;
        this.valueClass = valueClass;
        this.level = level;
        this.categories = Collections.unmodifiableSet(EnumSet.copyOf(categories));
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Set<MetricCategory> categories() {
        return categories;
    }

    @Override
    public MetricLevel level() {
        return level;
    }

    @Override
    public Class<T> valueClass() {
        return valueClass;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof DefaultSdkMetric<?> that && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "DefaultMetric(name=" + name + ", categories=" + categories + ")";
    }

    /** {@link SdkMetric#create(String, Class, MetricLevel, MetricCategory, MetricCategory...)}. */
    public static <T> SdkMetric<T> create(String name, Class<T> clzz, MetricLevel level, MetricCategory c1,
                                          MetricCategory... cn) {
        Set<MetricCategory> categories = EnumSet.of(Objects.requireNonNull(c1, "categories must not contain null"));
        if (cn != null) {
            for (MetricCategory category : cn) {
                categories.add(Objects.requireNonNull(category, "categories must not contain null"));
            }
        }
        return create(name, clzz, level, categories);
    }

    /** {@link SdkMetric#create(String, Class, MetricLevel, Set)}. */
    public static <T> SdkMetric<T> create(String name, Class<T> clzz, MetricLevel level,
                                          Set<MetricCategory> categories) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(clzz, "clzz must not be null");
        Objects.requireNonNull(level, "level must not be null");
        if (categories == null || categories.isEmpty()) {
            throw new IllegalArgumentException("categories must not be empty");
        }
        for (MetricCategory category : categories) {
            Objects.requireNonNull(category, "categories must not contain null elements");
        }
        if (!NAMES.add(name)) {
            throw new IllegalArgumentException("Metric with name " + name + " has already been created");
        }
        return new DefaultSdkMetric<>(name, clzz, level, categories);
    }
}
