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
 * Root package of the {@code software.amazon.awssdk:dynamic-client} V3 artifact.
 *
 * <p>Owns: The V3 dynamic client facade and dynamic operation descriptors.
 *
 * <p>{@link software.amazon.awssdk.dynamic.client.DynamicClient} and
 * {@link software.amazon.awssdk.dynamic.client.DynamicAsyncClient} call any service of a runtime Smithy model with
 * finite {@code Document} inputs and outputs, on the same {@code AwsClientBuildState}, {@code ClientRuntime},
 * {@code PipelinePlan}s and {@code AwsCallEngine} as generated service clients (Requirement 14.14). Streaming blobs use
 * the sdk-core request bodies and response transformers, event streams the Smithy Java {@code EventStream} values
 * generated types hold, and {@code DynamicPages}/{@code DynamicPagePublisher}/{@code DynamicWaiter}/
 * {@code DynamicAsyncWaiter} the sdk-core paginator and waiter runtime of generated paginators and waiters
 * (Requirements 14.12, 14.13, 14.15).
 *
 * <p>Allowed V3 module dependencies: {@code aws-pipeline}, {@code dynamic-model}, the protocol runtimes and the AWS
 * runtime modules generated services use. Forbidden dependencies: service artifacts.
 */
package software.amazon.awssdk.dynamic.client;
