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

/**
 * Observes the {@link PipelineStage}s a call runs, in the order it runs them (Property 29, lifecycle partial order).
 * A test-only seam: an engine has an observer only when its builder was given one, and without one every stage point
 * costs one null check, with no allocation, dispatch or timing.
 *
 * <p>The engine reports a stage when the stage starts, and only if it is active in the call's plan, so inactive
 * feature and hook stages are never reported. {@link PipelineStage#VALIDATE} is reported on the call it produced.
 * Called on the call's driver thread.
 */
@FunctionalInterface
interface StageObserver {
    /**
     * {@code call} started {@code stage}.
     *
     * @param attempt the current attempt for an attempt-scoped stage, else {@code null}
     */
    void entered(CallState<?, ?> call, AttemptState attempt, PipelineStage stage);
}
