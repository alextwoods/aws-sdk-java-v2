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
 * Root package of the {@code software.amazon.awssdk:dynamic-model} V3 artifact.
 *
 * <p>Owns: The Smithy JSON AST parser/model assembler and model-to-schema conversion used by the dynamic client.
 * {@link software.amazon.awssdk.dynamic.model.DynamicModel} loads a runtime model and selects a service;
 * {@link software.amazon.awssdk.dynamic.model.DynamicService} exposes the service's runtime schemas through the same
 * {@code ServiceSchemaIndex} generated services use (unknown traits by exact trait ID, enum member schemas included)
 * and one {@link software.amazon.awssdk.dynamic.model.DynamicAwsOperation} per operation, whose
 * {@code AwsOperationDescriptor} has the contents a generated descriptor would have.
 *
 * <p>Allowed V3 module dependencies: {@code annotations}, {@code sdk-core}, {@code smithy-runtime}, {@code aws-core}.
 * Forbidden dependencies: generated service dependency trees (no generated service or runtime module may depend on
 * this artifact).
 */
package software.amazon.awssdk.dynamic.model;
