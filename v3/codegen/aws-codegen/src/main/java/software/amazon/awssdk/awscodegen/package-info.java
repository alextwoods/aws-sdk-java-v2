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
 * Root package of the {@code software.amazon.awssdk:aws-codegen} V3 artifact.
 *
 * <p>Owns: The single AWS Smithy directed code generator.
 *
 * <p>{@link software.amazon.awssdk.awscodegen.AwsCodegen} is the entry point; it drives Smithy's
 * {@code CodegenDirector} with the one AWS {@code DirectedCodegen}. Customizations implement
 * {@link software.amazon.awssdk.awscodegen.AwsCodegenIntegration} and run in {@code (order, identifier)} order over an
 * {@link software.amazon.awssdk.awscodegen.AwsCodegenContext}.
 *
 * <p>Allowed V3 module dependencies: none yet. Third-party dependencies: the Smithy model and
 * {@code smithy-codegen-core} toolchain. Forbidden dependencies: runtime HTTP implementations.
 */
package software.amazon.awssdk.awscodegen;
