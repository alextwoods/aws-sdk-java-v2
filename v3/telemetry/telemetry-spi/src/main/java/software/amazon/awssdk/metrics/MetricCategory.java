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

/**
 * The categories of SDK metrics (V2-compatible); publishers may filter metrics by category.
 */
public enum MetricCategory {
    /** Metrics collected by the core SDK. */
    CORE("Core"),
    /** Metrics collected at the HTTP client level. */
    HTTP_CLIENT("HttpClient"),
    /** Metrics specified by the customer. */
    CUSTOM("Custom"),
    /** Every category; verbose, especially with {@link MetricLevel#TRACE}. */
    ALL("All");

    private final String value;

    MetricCategory(String value) {
        this.value = value;
    }

    /** The category's string value. */
    public String getValue() {
        return value;
    }

    /**
     * The category whose {@link #getValue()} equals {@code value}, ignoring case.
     *
     * @throws IllegalArgumentException if no category has that value
     */
    public static MetricCategory fromString(String value) {
        for (MetricCategory category : values()) {
            if (category.value.equalsIgnoreCase(value)) {
                return category;
            }
        }
        throw new IllegalArgumentException("MetricCategory cannot be created from value: " + value);
    }
}
