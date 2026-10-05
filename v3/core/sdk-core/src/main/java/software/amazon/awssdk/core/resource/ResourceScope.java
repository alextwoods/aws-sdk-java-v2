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

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import software.amazon.awssdk.core.exception.SdkCloseException;
import software.amazon.awssdk.utils.ToString;

/**
 * The resources one client closes, with who owns each (Requirements 9.6, 9.7, 9.8, 9.13, 9.14).
 *
 * <p>Entries are deduplicated by resource <em>identity</em>, not {@code equals}: aliases of one instance form one entry.
 * When one instance is registered both caller-owned and SDK-owned, it is caller-owned, so the SDK never closes a
 * resource the caller supplied.
 *
 * <p>{@link #close()} is idempotent and thread-safe. The first call gives every SDK-owned {@link AutoCloseable} entry
 * exactly one close attempt, in reverse registration order (dependents before what they depend on), continuing past
 * failures, and then throws one {@link SdkCloseException} carrying every failure, in attempt order, as suppressed
 * exceptions. Caller-owned entries are never closed. Every later call, and every concurrent call, attempts nothing and
 * throws nothing; a concurrent call returns once the first call's attempts are complete (a call from a resource's own
 * {@code close} on the closing thread returns at once).
 */
public final class ResourceScope implements AutoCloseable {
    private final List<Entry> entries;
    private final AtomicReference<Thread> closingThread = new AtomicReference<>();
    private final CountDownLatch closed = new CountDownLatch(1);

    private ResourceScope(List<Entry> entries) {
        this.entries = entries;
    }

    /**
     * The scope of {@code resources}, deduplicated by identity; a caller-owned registration of an instance wins over an
     * SDK-owned one.
     */
    public static ResourceScope of(Collection<? extends ManagedResource<?>> resources) {
        Map<Object, Integer> positions = new IdentityHashMap<>();
        List<ManagedResource<?>> deduplicated = new ArrayList<>();
        for (ManagedResource<?> resource : Objects.requireNonNull(resources, "resources")) {
            Objects.requireNonNull(resource, "resource");
            Integer position = positions.get(resource.resource());
            if (position == null) {
                positions.put(resource.resource(), deduplicated.size());
                deduplicated.add(resource);
            } else if (resource.ownership() == ResourceOwnership.CALLER_OWNED) {
                deduplicated.set(position, resource);
            }
        }
        List<Entry> entries = new ArrayList<>(deduplicated.size());
        deduplicated.forEach(resource -> entries.add(new Entry(resource)));
        return new ResourceScope(List.copyOf(entries));
    }

    /** Every distinct resource and its effective ownership, in first-registration order. Immutable. */
    public List<ManagedResource<?>> resources() {
        List<ManagedResource<?>> resources = new ArrayList<>(entries.size());
        entries.forEach(entry -> resources.add(entry.resource));
        return List.copyOf(resources);
    }

    /** The effective ownership of {@code resource} (compared by identity), or empty if it is not in this scope. */
    public Optional<ResourceOwnership> ownership(Object resource) {
        Objects.requireNonNull(resource, "resource");
        for (Entry entry : entries) {
            if (entry.resource.resource() == resource) {
                return Optional.of(entry.resource.ownership());
            }
        }
        return Optional.empty();
    }

    /** Whether {@link #close()} has been called. */
    public boolean isClosed() {
        return closingThread.get() != null;
    }

    /**
     * Closes every SDK-owned resource once; see the class description.
     *
     * @throws SdkCloseException from the first call only, if any close attempt failed, after all attempts
     */
    @Override
    public void close() {
        Thread current = Thread.currentThread();
        if (!closingThread.compareAndSet(null, current)) {
            awaitFirstClose(current);
            return;
        }
        List<Throwable> failures = new ArrayList<>();
        boolean interrupted = false;
        try {
            for (int i = entries.size() - 1; i >= 0; i--) {
                Entry entry = entries.get(i);
                Optional<AutoCloseable> closeable = entry.resource.closeableBySdk();
                if (closeable.isEmpty() || !entry.attempted.compareAndSet(false, true)) {
                    continue;
                }
                try {
                    closeable.get().close();
                } catch (InterruptedException e) {
                    interrupted = true;
                    failures.add(e);
                } catch (Throwable t) {
                    failures.add(t);
                }
            }
        } finally {
            closed.countDown();
            if (interrupted) {
                current.interrupt();
            }
        }
        if (!failures.isEmpty()) {
            throw SdkCloseException.create(failures);
        }
    }

    private void awaitFirstClose(Thread current) {
        if (closingThread.get() == current) {
            return;
        }
        try {
            closed.await();
        } catch (InterruptedException e) {
            current.interrupt();
        }
    }

    @Override
    public String toString() {
        return ToString.builder("ResourceScope")
                       .add("resources", resources())
                       .add("closed", isClosed())
                       .build();
    }

    /** One distinct resource and whether its one close attempt has been made. */
    private static final class Entry {
        private final ManagedResource<?> resource;
        private final AtomicBoolean attempted = new AtomicBoolean();

        private Entry(ManagedResource<?> resource) {
            this.resource = resource;
        }
    }
}
