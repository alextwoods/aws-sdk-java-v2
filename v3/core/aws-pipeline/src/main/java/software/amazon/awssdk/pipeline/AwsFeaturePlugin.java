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

import software.amazon.smithy.model.shapes.ShapeId;

/**
 * A schema-driven feature plugin (Requirement 11.11; design "Interceptors and plugins"): it contributes predeclared
 * stages, context slots, derived metadata and {@linkplain PipelinePlan.Builder#addInterceptor interceptors} to the
 * plans of the operations it applies to, once per client and operation, when {@link ClientRuntime#plan(AwsOperation)}
 * builds the plan; calls never consult plugins. It depends only on schemas and runtime
 * abstractions shared by generated services and the dynamic client; it cannot introduce an alternate pipeline or
 * refer to generated service classes.
 */
public interface AwsFeaturePlugin {
    /** The plugin's identity, for diagnostics and deduplication. */
    ShapeId id();

    /** Whether the plugin contributes to {@code operation}'s plan; decided from its schemas and metadata alone. */
    boolean appliesTo(AwsOperation<?, ?> operation);

    /** Contributes to the plan being built; {@link PipelinePlan.Builder#operation()} is the operation. */
    void configure(PipelinePlan.Builder<?, ?> plan);
}
