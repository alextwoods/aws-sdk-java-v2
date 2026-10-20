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

package software.amazon.awssdk.benchmark.e2e;

/**
 * Which request pipeline a V2 client arm runs, as detected by {@link PipelineCheck} on the SDK actually on the
 * classpath. Every arm uses the same class names and Maven coordinates, so the jar alone does not say which one ran.
 */
enum Pipeline {
    /** The legacy {@code RequestPipelineBuilder} pipeline (Baseline_Build and the racecar build). */
    STOCK,
    /** The schema-based {@code SyncApiCallPipeline}/{@code AsyncApiCallPipeline} path (this branch, task 9.2). */
    SCHEMA
}
