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

package software.amazon.awssdk.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.http.CancellationReason;

/**
 * The hierarchical cancellation scope of one call or attempt (design "Resource and cancellation state").
 *
 * <p>In-flight work registers a {@link Handle}: the active transport exchange, a body subscription, a scheduled retry
 * delay, endpoint or identity child work, or a queued completion task. A scope is {@link State#OPEN} until one
 * transition, exactly once and irreversibly, either {@linkplain #cancel cancels} it, running every registered handle
 * exactly once (most recent first) and cancelling every child scope, or {@linkplain #close closes} it, releasing every
 * handle without running it because the work ended normally. Work registered after that transition is cancelled at
 * once, with the scope's cancellation reason or, for a closed scope, {@link CancellationReason#RESPONSE_ABANDONED}, so
 * a late callback can never start work that outlives its call.
 *
 * <p>A {@linkplain #child() child} scope (one per attempt) is cancelled with its parent, and can itself be cancelled
 * (for example on an attempt timeout) or closed without affecting the parent; closing or cancelling a child removes
 * it from the parent, so a call's scope never accumulates finished attempts. Thread safe: cancellation typically
 * arrives on a caller's thread while the driver runs elsewhere.
 */
public final class CancellationScope {
    /** The lifecycle of a scope. */
    public enum State {
        /** Accepting work. */
        OPEN,
        /** Cancelled; every handle was run once. */
        CANCELLED,
        /** Ended normally; handles were released without running. */
        CLOSED
    }

    /** Cancels one piece of in-flight work. Must be idempotent-safe and should not block or throw. */
    @FunctionalInterface
    public interface Handle {
        /** Cancels the work for {@code reason}. */
        void cancel(CancellationReason reason);
    }

    /** The registration of one handle in one scope. */
    public interface Registration {
        /**
         * Removes the handle because its work finished; it will not be run.
         *
         * @return true if the handle was still registered in an open scope
         */
        boolean release();
    }

    private static final Registration RELEASED = () -> false;

    private final Object lock = new Object();
    private volatile State state = State.OPEN;
    private CancellationReason reason;
    private List<Node> handles;
    private Registration parentRegistration;

    private CancellationScope() {
    }

    /** A new open scope with no parent: the scope of one call. */
    public static CancellationScope root() {
        return new CancellationScope();
    }

    /**
     * A new scope cancelled with this one: the scope of one attempt. If this scope is no longer open, the child is
     * returned already cancelled.
     */
    public CancellationScope child() {
        CancellationScope child = new CancellationScope();
        Registration registration = register(child::cancel);
        boolean ended;
        synchronized (child.lock) {
            ended = child.state != State.OPEN;
            if (!ended) {
                child.parentRegistration = registration;
            }
        }
        if (ended) {
            // The parent ended between registering the child and now; the child is already cancelled.
            registration.release();
        }
        return child;
    }

    /**
     * Registers {@code handle} to be run once if this scope is cancelled. If the scope is no longer open, the handle
     * runs at once on the calling thread and the returned registration is already released.
     */
    public Registration register(Handle handle) {
        Objects.requireNonNull(handle, "handle");
        CancellationReason late;
        synchronized (lock) {
            if (state == State.OPEN) {
                Node node = new Node(handle);
                if (handles == null) {
                    handles = new ArrayList<>(2);
                }
                handles.add(node);
                return node;
            }
            late = state == State.CANCELLED ? reason : CancellationReason.RESPONSE_ABANDONED;
        }
        handle.cancel(late);
        return RELEASED;
    }

    /**
     * Cancels this scope for {@code reason}: runs every registered handle and child exactly once, most recently
     * registered first, continuing past failures.
     *
     * @return true if this call made the one transition; false if the scope was already cancelled or closed
     * @throws RuntimeException the first failure of a handle, with any others suppressed, after every handle ran; the
     *     scope is cancelled regardless
     */
    public boolean cancel(CancellationReason reason) {
        Objects.requireNonNull(reason, "reason");
        List<Node> toCancel;
        Registration fromParent;
        synchronized (lock) {
            if (state != State.OPEN) {
                return false;
            }
            this.reason = reason;
            this.state = State.CANCELLED;
            toCancel = handles;
            handles = null;
            fromParent = parentRegistration;
            parentRegistration = null;
        }
        if (fromParent != null) {
            fromParent.release();
        }
        RuntimeException failure = null;
        if (toCancel != null) {
            for (int i = toCancel.size() - 1; i >= 0; i--) {
                try {
                    toCancel.get(i).handle.cancel(reason);
                } catch (RuntimeException e) {
                    if (failure == null) {
                        failure = e;
                    } else {
                        failure.addSuppressed(e);
                    }
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
        return true;
    }

    /**
     * Ends this scope normally: releases every handle without running it and detaches from the parent.
     *
     * @return true if this call made the one transition; false if the scope was already cancelled or closed
     */
    public boolean close() {
        Registration fromParent;
        synchronized (lock) {
            if (state != State.OPEN) {
                return false;
            }
            state = State.CLOSED;
            handles = null;
            fromParent = parentRegistration;
            parentRegistration = null;
        }
        if (fromParent != null) {
            fromParent.release();
        }
        return true;
    }

    /** The current state. */
    public State state() {
        return state;
    }

    /** Whether the scope accepts work. */
    public boolean isOpen() {
        return state == State.OPEN;
    }

    /** Whether the scope was cancelled. */
    public boolean isCancelled() {
        return state == State.CANCELLED;
    }

    /** The reason the scope was cancelled, if it was. */
    public Optional<CancellationReason> reason() {
        synchronized (lock) {
            return Optional.ofNullable(reason);
        }
    }

    /** The number of registered handles (including children); for diagnostics and tests. */
    int registeredHandles() {
        synchronized (lock) {
            return handles == null ? 0 : handles.size();
        }
    }

    private boolean remove(Node node) {
        synchronized (lock) {
            return handles != null && handles.remove(node);
        }
    }

    @Override
    public String toString() {
        return "CancellationScope(" + state + ")";
    }

    private final class Node implements Registration {
        private final Handle handle;

        private Node(Handle handle) {
            this.handle = handle;
        }

        @Override
        public boolean release() {
            return remove(this);
        }
    }
}
