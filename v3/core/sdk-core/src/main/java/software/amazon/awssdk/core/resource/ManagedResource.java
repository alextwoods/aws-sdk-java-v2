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

package software.amazon.awssdk.core.resource;

import java.util.Objects;
import java.util.Optional;

/**
 * One configured resource and who owns it (Requirement 9.6).
 *
 * <p>Immutable. Two managed resources are equal when they hold the <em>same</em> resource instance (identity, not
 * {@code equals}) with the same ownership, which is how close aggregation deduplicates aliases of one resource.
 *
 * @param <T> the resource type
 */
public final class ManagedResource<T> {
    private final T resource;
    private final ResourceOwnership ownership;

    private ManagedResource(T resource, ResourceOwnership ownership) {
        this.resource = Objects.requireNonNull(resource, "resource");
        this.ownership = Objects.requireNonNull(ownership, "ownership");
    }

    /** {@code resource}, created by the SDK and closed by it. */
    public static <T> ManagedResource<T> sdkOwned(T resource) {
        return new ManagedResource<>(resource, ResourceOwnership.SDK_OWNED);
    }

    /** {@code resource}, supplied by the caller and never closed by the SDK. */
    public static <T> ManagedResource<T> callerOwned(T resource) {
        return new ManagedResource<>(resource, ResourceOwnership.CALLER_OWNED);
    }

    /** {@code resource} with {@code ownership}. */
    public static <T> ManagedResource<T> of(T resource, ResourceOwnership ownership) {
        return new ManagedResource<>(resource, ownership);
    }

    /** The resource. */
    public T resource() {
        return resource;
    }

    /** Who closes the resource. */
    public ResourceOwnership ownership() {
        return ownership;
    }

    /** The resource as an {@link AutoCloseable}, if it is SDK-owned and closeable; otherwise empty. */
    public Optional<AutoCloseable> closeableBySdk() {
        return ownership.closedBySdk() && resource instanceof AutoCloseable closeable
               ? Optional.of(closeable)
               : Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ManagedResource<?> that && resource == that.resource && ownership == that.ownership;
    }

    @Override
    public int hashCode() {
        return 31 * System.identityHashCode(resource) + ownership.hashCode();
    }

    @Override
    public String toString() {
        return "ManagedResource(" + resource.getClass().getSimpleName() + ", " + ownership + ")";
    }
}
