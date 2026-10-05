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
 * Root package of the {@code software.amazon.awssdk:endpoints} V3 artifact.
 *
 * <p>Owns: Endpoint rules runtime and endpoint resolution. Every endpoint dimension is resolved from the service's
 * canonical Smithy model rules and traits:
 * <ul>
 *   <li>regional, partition, FIPS, dual-stack, account-based and endpoint-auth dimensions by evaluating the canonical
 *       {@code smithy.rules#endpointRuleSet} ({@link software.amazon.awssdk.endpoints.rules.EndpointRuleSet}) over the
 *       canonical partition table ({@link software.amazon.awssdk.endpoints.partition.Partitions}), with the resolved
 *       {@link software.amazon.awssdk.endpoints.EndpointAuthScheme}s;</li>
 *   <li>parameter binding from static, context, operation-context and client-context parameters and built-ins
 *       ({@link software.amazon.awssdk.endpoints.binding.EndpointBinding});</li>
 *   <li>host prefixes ({@link software.amazon.awssdk.endpoints.binding.HostPrefix});</li>
 *   <li>endpoint discovery ({@link software.amazon.awssdk.endpoints.discovery.EndpointDiscoveryCache}).</li>
 * </ul>
 * {@link software.amazon.awssdk.endpoints.rules.EndpointTestSuite} runs the canonical
 * {@code smithy.rules#endpointTests} vectors.
 *
 * <p>Allowed V3 module dependencies: {@code sdk-core}, {@code regions}, {@code smithy-runtime}. Forbidden
 * dependencies: generated service artifacts and the Smithy model toolchain.
 */
package software.amazon.awssdk.endpoints;
