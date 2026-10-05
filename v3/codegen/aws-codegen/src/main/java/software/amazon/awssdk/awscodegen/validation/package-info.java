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
 * Canonical-model validation of {@code aws-codegen}: the first generation phase, which runs before Smithy's
 * {@code CodegenDirector} and before anything is staged.
 *
 * <ol>
 *   <li>{@link software.amazon.awssdk.awscodegen.validation.ModelSourceValidator} classifies every model-directory file
 *       by content and rejects C2J models and V2 paginator, waiter, endpoint, example, customization and smoke-test
 *       sidecars (Requirements 2.3-2.8), as well as unrecognized or unreadable JSON inputs.</li>
 *   <li>{@link software.amazon.awssdk.awscodegen.validation.ModelValidationDiagnostics} turns Smithy assembly events of
 *       severity {@code DANGER} or {@code ERROR} into diagnostics. Unknown traits are allowed during assembly and are
 *       retained as exact {@code DynamicTrait}s (Requirement 2.11).</li>
 *   <li>{@link software.amazon.awssdk.awscodegen.validation.SemanticValidator} runs the required-AWS-behavior
 *       {@link software.amazon.awssdk.awscodegen.validation.SemanticCheck}s (Requirement 2.10).</li>
 * </ol>
 * Each phase reports every problem it finds as a sorted list of structured
 * {@link software.amazon.awssdk.awscodegen.validation.Diagnostic}s; a non-empty list stops generation with nothing
 * published for the service (Requirement 2.9). This package only reads model inputs: it never writes, never builds a
 * whole-service representation and never repairs or infers missing semantics.
 */
package software.amazon.awssdk.awscodegen.validation;
