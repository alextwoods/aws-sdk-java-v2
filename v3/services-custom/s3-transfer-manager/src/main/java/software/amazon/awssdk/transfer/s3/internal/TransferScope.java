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

package software.amazon.awssdk.transfer.s3.internal;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The cancellation scope of one transfer (design, "Resource and cancellation state"): it holds the handles of the
 * transfer's work in flight (S3 calls, child transfers, listing subscriptions) and stops all of them at once.
 *
 * <p>A scope ends at most once, with a {@link Reason}: {@link Reason#FAILED} (a part or listing failed),
 * {@link Reason#CANCELLED} (the caller cancelled the transfer or closed the transfer manager) or
 * {@link Reason#PAUSED}. Ending the scope cancels every tracked handle; handles tracked afterwards are cancelled at
 * once, so no new work starts after the transfer ended. The transfer reads {@link #reason()} to choose its cleanup:
 * failed and cancelled multipart uploads are aborted, paused ones are kept for resumption. Thread safe.
 */
public final class TransferScope {
    private final Set<Runnable> cancellers = ConcurrentHashMap.newKeySet();
    private final AtomicReference<Ended> ended = new AtomicReference<>();
    private final TransferScope parent;
    private volatile Runnable detach = () -> { };

    /** Why a scope ended. */
    public enum Reason {
        FAILED,
        CANCELLED,
        PAUSED
    }

    private record Ended(Reason reason, Throwable cause) {
    }

    public TransferScope() {
        this(null);
    }

    private TransferScope(TransferScope parent) {
        this.parent = parent;
    }

    /** A scope that ends (with the same reason and cause) when this one ends, and that can also end on its own. */
    public TransferScope child() {
        TransferScope child = new TransferScope(this);
        Runnable canceller = () -> {
            Ended e = ended.get();
            child.end(e.reason(), e.cause());
        };
        if (register(canceller)) {
            child.detach = () -> cancellers.remove(canceller);
        }
        return child;
    }

    /**
     * Releases this child scope's registration with its parent without ending it: called once the child's transfer
     * finished, so a long directory transfer does not accumulate the handles of finished file transfers.
     */
    public void detach() {
        detach.run();
    }

    /**
     * Tracks {@code future}: it is cancelled when the scope ends (immediately if it already ended) and untracked once
     * it completes. Returns {@code future}.
     */
    public <T> CompletableFuture<T> track(CompletableFuture<T> future) {
        Runnable canceller = () -> future.cancel(true);
        if (register(canceller)) {
            future.whenComplete((r, t) -> cancellers.remove(canceller));
        }
        return future;
    }

    /**
     * Runs {@code canceller} when the scope ends (immediately if it already ended). The returned handle untracks it.
     */
    public Runnable onEnd(Runnable canceller) {
        register(canceller);
        return () -> cancellers.remove(canceller);
    }

    /**
     * Ends the scope and cancels everything it tracks; only the first call has an effect.
     *
     * @return true if this call ended the scope
     */
    public boolean end(Reason reason, Throwable cause) {
        if (!ended.compareAndSet(null, new Ended(reason, cause))) {
            return false;
        }
        detach();
        for (Runnable canceller : cancellers) {
            if (cancellers.remove(canceller)) {
                canceller.run();
            }
        }
        return true;
    }

    public boolean isEnded() {
        return ended.get() != null;
    }

    /** Why the scope ended, or {@code null} while it is open. */
    public Reason reason() {
        Ended e = ended.get();
        return e == null ? null : e.reason();
    }

    /** The cause it ended with, or {@code null}. */
    public Throwable cause() {
        Ended e = ended.get();
        return e == null ? null : e.cause();
    }

    /** True if this scope or an ancestor ended. */
    public boolean isEndedOrAncestorEnded() {
        return isEnded() || (parent != null && parent.isEndedOrAncestorEnded());
    }

    /** Registers {@code canceller}; runs it now (and returns false) if the scope already ended. */
    private boolean register(Runnable canceller) {
        cancellers.add(canceller);
        if (ended.get() != null && cancellers.remove(canceller)) {
            canceller.run();
            return false;
        }
        return true;
    }
}
