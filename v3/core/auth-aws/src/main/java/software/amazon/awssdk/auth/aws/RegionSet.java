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

package software.amazon.awssdk.auth.aws;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * The regions a SigV4a signature is valid in, with the V2 name: region names or wildcards such as {@code *} and
 * {@code us-*}, rendered comma-separated in {@code X-Amz-Region-Set}. Immutable; equal when the rendered text is equal.
 */
public final class RegionSet {
    /** Every region. */
    public static final RegionSet GLOBAL = new RegionSet(List.of("*"));

    private final List<String> regions;
    private final String text;

    private RegionSet(List<String> regions) {
        if (regions.isEmpty()) {
            throw new IllegalArgumentException("A region set needs at least one region.");
        }
        for (String region : regions) {
            if (region == null || region.isBlank() || region.contains(",")) {
                throw new IllegalArgumentException("Invalid region set entry: " + region);
            }
        }
        this.regions = List.copyOf(regions);
        this.text = String.join(",", this.regions);
    }

    /** The set of {@code regions}. */
    public static RegionSet create(Collection<String> regions) {
        return new RegionSet(List.copyOf(Objects.requireNonNull(regions, "regions")));
    }

    /** The set of the comma-separated {@code value} (whitespace ignored), as in the V2 setting. */
    public static RegionSet create(String value) {
        Objects.requireNonNull(value, "value");
        return new RegionSet(Arrays.stream(value.split(",")).map(String::strip).filter(s -> !s.isEmpty()).toList());
    }

    /** The regions, in order. */
    public List<String> regions() {
        return regions;
    }

    /** The comma-separated header value. */
    public String asString() {
        return text;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RegionSet that && text.equals(that.text);
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

    @Override
    public String toString() {
        return text;
    }
}
