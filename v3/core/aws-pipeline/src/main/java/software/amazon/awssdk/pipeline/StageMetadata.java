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

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * The stages active in one {@link PipelinePlan}, computed once when the plan is built: every {@link
 * PipelineStage.Kind#CORE} stage plus the feature and hook stages the operation, its plugins and the client's
 * interceptors contribute. {@link #isActive(PipelineStage)} is one bit test over an immutable mask.
 */
public final class StageMetadata {
    private static final long CORE_MASK;

    static {
        if (PipelineStage.values().length > Long.SIZE) {
            throw new ExceptionInInitializerError("The stage mask holds at most " + Long.SIZE + " stages");
        }
        long mask = 0;
        for (PipelineStage stage : PipelineStage.values()) {
            if (stage.kind() == PipelineStage.Kind.CORE) {
                mask |= bit(stage);
            }
        }
        CORE_MASK = mask;
    }

    private final long mask;

    private StageMetadata(long mask) {
        this.mask = mask;
    }

    /** The metadata of a plan with the core stages and {@code additional} active. */
    static StageMetadata of(Collection<PipelineStage> additional) {
        long mask = CORE_MASK;
        for (PipelineStage stage : additional) {
            mask |= bit(stage);
        }
        return new StageMetadata(mask);
    }

    /** Whether {@code stage} runs in calls of the plan. */
    public boolean isActive(PipelineStage stage) {
        return (mask & bit(stage)) != 0;
    }

    /** The active stages, in lifecycle order. Immutable. */
    public Set<PipelineStage> activeStages() {
        EnumSet<PipelineStage> active = EnumSet.noneOf(PipelineStage.class);
        for (PipelineStage stage : PipelineStage.values()) {
            if (isActive(stage)) {
                active.add(stage);
            }
        }
        return Collections.unmodifiableSet(active);
    }

    private static long bit(PipelineStage stage) {
        return 1L << stage.ordinal();
    }

    @Override
    public String toString() {
        return "StageMetadata" + activeStages();
    }
}
