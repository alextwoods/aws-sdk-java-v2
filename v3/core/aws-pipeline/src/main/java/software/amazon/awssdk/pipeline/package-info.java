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

/**
 * Root package of the {@code software.amazon.awssdk:aws-pipeline} V3 artifact.
 *
 * <p>Owns: AwsCallEngine, PipelinePlan, stages, execution context, interceptor/plugin dispatch, cancellation,
 * timeouts, and exception mapping.
 *
 * <p>Runtime objects (design "Core runtime objects"): one immutable
 * {@link software.amazon.awssdk.pipeline.ClientRuntime} per client holds one immutable {@link software.amazon.awssdk.pipeline.PipelinePlan} per
 * {@link software.amazon.awssdk.pipeline.AwsOperation}, built once and holding no customer data. Each call creates one
 * {@link software.amazon.awssdk.pipeline.CallState} (request, call options, execution context, retry token, attempt
 * counter, cancellation scope, terminal state), and each attempt one sequential
 * {@link software.amazon.awssdk.pipeline.AttemptState} with a fresh attempt context. Typed contexts are compact arrays
 * indexed by the plan's {@link software.amazon.awssdk.pipeline.ContextLayout}.
 *
 * <p>Allowed V3 module dependencies: {@code aws-core}, {@code protocol-core}, {@code auth-api}, {@code endpoints},
 * {@code retries}, {@code checksums}, {@code http-spi}. Forbidden dependencies: service artifacts, the Smithy model
 * parser, and V2 artifacts.
 */
package software.amazon.awssdk.pipeline;
