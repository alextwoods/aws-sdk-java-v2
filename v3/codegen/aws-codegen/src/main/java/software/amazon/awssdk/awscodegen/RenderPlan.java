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

package software.amazon.awssdk.awscodegen;

/**
 * A small immutable plan for rendering exactly one generated file.
 *
 * <p>Emitters do not build an intermediate representation of the service (Requirements 3.2-3.4). Each emitter:
 * <ol>
 *   <li>computes a render plan for the one file it writes, directly from the directed context: the Smithy
 *       {@code Model}, Smithy knowledge indexes over it ({@code TopDownIndex}, {@code OperationIndex}, ...), traits
 *       and the {@code SymbolProvider};</li>
 *   <li>renders that file from the plan alone; and</li>
 *   <li>discards the plan. Plans are never cached across files, shared between emitters, handed to another generator
 *       or serialized.</li>
 * </ol>
 *
 * <p>A plan therefore holds only what the file's text needs: Java {@code Symbol}s, Smithy {@code ShapeId}s, names,
 * primitives, and lists or nested records of those. It never holds a {@code Model}, {@code Shape}, trait,
 * {@code Node} or knowledge index, so it cannot become an alternate semantic authority that restates the model.
 * {@code RenderPlanRulesTest} enforces this for every permitted plan type.
 */
sealed interface RenderPlan permits OperationDescriptorPlan, OperationTablePlan, ServiceMetadataPlan, TypeRegistryPlan,
                                    EndpointParamsPlan, EndpointProviderPlan, EndpointRulesPlan {
}
