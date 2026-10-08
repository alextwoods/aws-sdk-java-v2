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

package software.amazon.awssdk.test.fault;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;

/**
 * A credentials provider that runs one {@link IdentityStep} per resolution. Hanging resolutions return futures the
 * provider owns and never completes; the pipeline must stop waiting for them without cancelling them, since caching
 * providers share one future between callers. Thread-safe.
 */
public final class ScriptedIdentityProvider implements IdentityProvider<AwsCredentialsIdentity> {
    /** The identity every {@link IdentityStep#RESOLVE} returns. */
    public static final AwsCredentialsIdentity IDENTITY = AwsCredentialsIdentity.create("AKIDFAULTINJECTION",
                                                                                         "fault-injection-secret");

    private final Deque<IdentityStep> script;
    private final Violations violations;
    private final AtomicInteger resolutions = new AtomicInteger();
    private final List<HeldIdentity> held = new CopyOnWriteArrayList<>();
    private final BlockingQueue<HeldIdentity> parked = new LinkedBlockingQueue<>();

    public ScriptedIdentityProvider(List<IdentityStep> script, Violations violations) {
        this.script = new ArrayDeque<>(script);
        this.violations = Objects.requireNonNull(violations, "violations");
    }

    @Override
    public Class<AwsCredentialsIdentity> identityType() {
        return AwsCredentialsIdentity.class;
    }

    @Override
    public CompletableFuture<? extends AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
        int n = resolutions.incrementAndGet();
        IdentityStep step;
        synchronized (this) {
            step = script.poll();
        }
        if (step == null) {
            violations.record("identity: unscripted resolution " + n);
            return CompletableFuture.failedFuture(new IllegalStateException("The trace scripts no further identity "
                                                                            + "resolution"));
        }
        return switch (step) {
            case RESOLVE -> CompletableFuture.completedFuture(IDENTITY);
            case FAIL -> CompletableFuture.failedFuture(
                new FaultInjectedException("Injected identity failure (resolution " + n + ")"));
            case HANG -> {
                HeldIdentity future = new HeldIdentity();
                held.add(future);
                parked.add(future);
                yield future;
            }
        };
    }

    /** How many resolutions the pipeline started. */
    public int resolutions() {
        return resolutions.get();
    }

    /** The futures of the hanging resolutions, in order. */
    public List<HeldIdentity> held() {
        return List.copyOf(held);
    }

    /** True if the pipeline cancelled a provider-owned future, which it must never do. */
    public boolean anyHeldCancelled() {
        return held.stream().anyMatch(h -> h.cancels() > 0);
    }

    /** Waits for the next hanging resolution. */
    public HeldIdentity awaitHeld(Duration timeout) {
        try {
            HeldIdentity future = parked.poll(timeout.toNanos(), TimeUnit.NANOSECONDS);
            if (future == null) {
                throw new IllegalStateException("No identity resolution hung within " + timeout);
            }
            return future;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for an identity resolution", e);
        }
    }

    /** The scripted steps no resolution has used yet. */
    public synchronized List<IdentityStep> unused() {
        return List.copyOf(script);
    }

    /** A provider-owned identity future that counts attempts to cancel it. */
    public static final class HeldIdentity extends CompletableFuture<AwsCredentialsIdentity> {
        private final AtomicInteger cancels = new AtomicInteger();

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancels.incrementAndGet();
            return super.cancel(mayInterruptIfRunning);
        }

        public int cancels() {
            return cancels.get();
        }
    }
}
