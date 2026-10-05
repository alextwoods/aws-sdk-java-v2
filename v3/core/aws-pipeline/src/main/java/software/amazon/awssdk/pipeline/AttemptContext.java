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

import java.util.Objects;
import software.amazon.awssdk.core.context.ContextKey;

/**
 * The attempt-scoped typed context of one attempt (Requirements 10.8, 10.9, 11.7): a fresh, empty set of
 * {@link ContextKey.Scope#ATTEMPT} values created when the attempt starts, plus read-through access to the call's
 * {@link ExecutionContext}.
 *
 * <p>Values are forward only: a value is visible once the stage or hook that produced it has stored it, and only to
 * later stages and hooks of this attempt. When the attempt completes the context closes, and every further access
 * throws, so no attempt value survives into the next attempt or another call. The {@link PipelineKeys} are produced
 * by the pipeline alone: {@link #put} rejects them.
 *
 * <p>Execution-scoped keys are read through to {@link #execution()}; writing one through this context is rejected, so
 * an attempt changes execution state only through the explicit {@link #execution()} API. Storage is one array indexed
 * by the plan's {@link ContextLayout}. Not thread safe; confined to the call's driver.
 */
public final class AttemptContext extends ContextValues {
    private final ExecutionContext execution;
    private volatile boolean open = true;

    AttemptContext(ExecutionContext execution, int attemptNumber) {
        super(execution.layout(), ContextKey.Scope.ATTEMPT);
        this.execution = execution;
        writeIndex(PipelineKeys.ATTEMPT_NUMBER_SLOT, attemptNumber);
    }

    /** The one-based number of the attempt; also readable as {@link PipelineKeys#ATTEMPT_NUMBER}. */
    public int attemptNumber() {
        requireOpen();
        return (Integer) readIndex(PipelineKeys.ATTEMPT_NUMBER_SLOT);
    }

    /** The call's execution context; it outlives this attempt. */
    public ExecutionContext execution() {
        return execution;
    }

    /** Whether the attempt is still running; a closed context rejects every access. */
    public boolean isOpen() {
        return open;
    }

    /**
     * The value stored under {@code key}, or {@code null}: an attempt key reads this attempt's value, an execution key
     * reads the call's value.
     *
     * @throws IllegalStateException if the attempt has completed
     */
    public <T> T get(ContextKey<T> key) {
        Objects.requireNonNull(key, "key");
        requireOpen();
        return key.scope() == ContextKey.Scope.EXECUTION ? execution.get(key) : read(key);
    }

    /**
     * Whether a value is stored under {@code key}, in this attempt or, for an execution key, in the call.
     *
     * @throws IllegalStateException if the attempt has completed
     */
    public boolean contains(ContextKey<?> key) {
        Objects.requireNonNull(key, "key");
        requireOpen();
        return key.scope() == ContextKey.Scope.EXECUTION ? execution.contains(key) : has(key);
    }

    /**
     * Stores {@code value} under the attempt key {@code key} for later stages of this attempt; {@code null} removes it.
     *
     * @throws IllegalArgumentException if {@code key} is execution scoped (use {@link #execution()}) or is one of the
     *     pipeline-produced {@link PipelineKeys}
     * @throws ClassCastException if {@code value} is not of the key's value type
     * @throws IllegalStateException if the attempt has completed
     */
    public <T> void put(ContextKey<T> key, T value) {
        Objects.requireNonNull(key, "key");
        requireOpen();
        if (key.scope() == ContextKey.Scope.EXECUTION) {
            throw new IllegalArgumentException(key + " is execution-scoped; store it through execution()");
        }
        requireNotBuiltIn(key);
        write(key, value);
    }

    /**
     * The value in the attempt slot {@code slot}, or {@code null}.
     *
     * @throws IllegalArgumentException if the slot belongs to another plan or is execution scoped
     * @throws IllegalStateException if the attempt has completed
     */
    public <T> T get(ContextSlot<T> slot) {
        Objects.requireNonNull(slot, "slot");
        requireOpen();
        return read(slot);
    }

    /**
     * Stores {@code value} in the attempt slot {@code slot}; {@code null} clears it.
     *
     * @throws IllegalArgumentException if the slot belongs to another plan, is execution scoped, or holds a
     *     pipeline-produced key
     * @throws IllegalStateException if the attempt has completed
     */
    public <T> void put(ContextSlot<T> slot, T value) {
        Objects.requireNonNull(slot, "slot");
        requireOpen();
        requireNotBuiltIn(slot.key());
        write(slot, value);
    }

    /** Stores a pipeline-produced value in its fixed slot; used only by {@link AttemptState}. */
    void putBuiltIn(int index, Object value) {
        requireOpen();
        writeIndex(index, value);
    }

    Object getBuiltIn(int index) {
        requireOpen();
        return readIndex(index);
    }

    /** Ends the attempt; idempotent. */
    void close() {
        open = false;
    }

    private void requireOpen() {
        if (!open) {
            throw new IllegalStateException("The attempt has completed; its attempt-scoped context is no longer "
                                            + "accessible");
        }
    }

    private static void requireNotBuiltIn(ContextKey<?> key) {
        if (PipelineKeys.isBuiltIn(key)) {
            throw new IllegalArgumentException(key + " is produced by the pipeline and cannot be replaced");
        }
    }

    @Override
    public String toString() {
        return "AttemptContext(open=" + open + ")";
    }
}
