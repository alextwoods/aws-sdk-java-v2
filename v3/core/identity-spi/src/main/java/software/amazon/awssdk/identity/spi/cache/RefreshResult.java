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

package software.amazon.awssdk.identity.spi.cache;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.utils.ToString;

/**
 * One identity an {@link IdentityCache} loaded, with when it must and may be refreshed (the V2 {@code RefreshResult}
 * semantics).
 *
 * <ul>
 *   <li>{@code staleTime}: from this instant on the value is never returned without a successful refresh (unless the
 *       cache allows stale values). Absent means the value never goes stale.</li>
 *   <li>{@code prefetchTime}: from this instant on, a call still returns the value but starts a background refresh
 *       whose failure is ignored. Absent means no prefetch.</li>
 * </ul>
 *
 * @param <T> the value type
 */
public final class RefreshResult<T> {
    private final T value;
    private final Instant staleTime;
    private final Instant prefetchTime;

    private RefreshResult(T value, Instant staleTime, Instant prefetchTime) {
        this.value = Objects.requireNonNull(value, "value");
        this.staleTime = staleTime;
        this.prefetchTime = prefetchTime;
    }

    /** {@code value}, stale at {@code staleTime} and prefetched from {@code prefetchTime}; either may be null. */
    public static <T> RefreshResult<T> of(T value, Instant staleTime, Instant prefetchTime) {
        return new RefreshResult<>(value, staleTime, prefetchTime);
    }

    /** {@code value}, which never goes stale and is never prefetched. */
    public static <T> RefreshResult<T> of(T value) {
        return new RefreshResult<>(value, null, null);
    }

    /** The loaded value. */
    public T value() {
        return value;
    }

    /** When the value goes stale, if it does. */
    public Optional<Instant> staleTime() {
        return Optional.ofNullable(staleTime);
    }

    /** When a background refresh should start, if one should. */
    public Optional<Instant> prefetchTime() {
        return Optional.ofNullable(prefetchTime);
    }

    /** This result with {@code staleTime}. */
    public RefreshResult<T> withStaleTime(Instant staleTime) {
        return new RefreshResult<>(value, staleTime, prefetchTime);
    }

    @Override
    public String toString() {
        return ToString.builder("RefreshResult").add("staleTime", staleTime).add("prefetchTime", prefetchTime).build();
    }
}
