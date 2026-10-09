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

package software.amazon.awssdk.benchmark.harness;

import java.util.Optional;

/**
 * Finite payload size classes (Req 20.2), defined by the encoded bytes of one logical operation: the final request
 * body plus the final response body as they cross the wire, excluding HTTP framing. The ranges are half-open,
 * contiguous and non-overlapping, so every finite encoded size up to {@link #LARGE}'s bound has exactly one class:
 *
 * <ul>
 *     <li>{@code small}: [0, 1 KiB)</li>
 *     <li>{@code medium}: [1 KiB, 64 KiB)</li>
 *     <li>{@code large}: [64 KiB, 8 MiB)</li>
 * </ul>
 *
 * <p>Each class also names the target size fixtures are built to, well inside its range so codec differences between
 * protocols cannot push a fixture across a boundary. Fixtures assert their own class when they are built.
 */
public enum PayloadClass {
    SMALL("small", 0, 1024, 512),
    MEDIUM("medium", 1024, 64 * 1024, 16 * 1024),
    LARGE("large", 64 * 1024, 8 * 1024 * 1024, 512 * 1024);

    private final String id;
    private final long minInclusive;
    private final long maxExclusive;
    private final int targetBytes;

    PayloadClass(String id, long minInclusive, long maxExclusive, int targetBytes) {
        this.id = id;
        this.minInclusive = minInclusive;
        this.maxExclusive = maxExclusive;
        this.targetBytes = targetBytes;
    }

    public String id() {
        return id;
    }

    public long minInclusive() {
        return minInclusive;
    }

    public long maxExclusive() {
        return maxExclusive;
    }

    /** The encoded operation size fixtures for this class aim at. */
    public int targetBytes() {
        return targetBytes;
    }

    public boolean contains(long encodedBytes) {
        return encodedBytes >= minInclusive && encodedBytes < maxExclusive;
    }

    /** The unique class of {@code encodedBytes}, or empty if it is negative or not finite-class sized. */
    public static Optional<PayloadClass> classify(long encodedBytes) {
        for (PayloadClass c : values()) {
            if (c.contains(encodedBytes)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    public static PayloadClass fromId(String id) {
        for (PayloadClass c : values()) {
            if (c.id.equals(id)) {
                return c;
            }
        }
        throw new IllegalArgumentException("unknown payload class: " + id);
    }
}
