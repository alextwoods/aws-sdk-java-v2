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

package software.amazon.awssdk.awscodegen.validation;

/**
 * Stable identifier of the kind of problem a {@link Diagnostic} reports. Codes are part of the generator's diagnostic
 * contract: tests and tooling match on them, so existing codes are never renamed or reused for another meaning.
 */
public enum DiagnosticCode {
    /** A C2J service or documentation model ({@code service-2.json}, {@code api-2.json}, {@code docs-2.json}). */
    LEGACY_C2J_MODEL,
    /** A V2 paginator sidecar ({@code paginators-1.json}). */
    LEGACY_PAGINATOR_SIDECAR,
    /** A V2 waiter sidecar ({@code waiters-2.json}). */
    LEGACY_WAITER_SIDECAR,
    /** A V2 endpoint rule-set, endpoint BDD or endpoint test sidecar. */
    LEGACY_ENDPOINT_SIDECAR,
    /** A V2 example sidecar ({@code examples-1.json}). */
    LEGACY_EXAMPLE_SIDECAR,
    /** A V2 customization sidecar ({@code customization.config}). */
    LEGACY_CUSTOMIZATION_SIDECAR,
    /** A V2 smoke-test sidecar ({@code smoke.json}). */
    LEGACY_SMOKE_TEST_SIDECAR,
    /** A JSON model-directory file that is neither a Smithy JSON AST model nor a recognized legacy format. */
    UNRECOGNIZED_MODEL_SOURCE,
    /** A model-directory file that cannot be read or parsed. */
    UNREADABLE_MODEL_SOURCE,
    /** A Smithy validation event of severity {@code ERROR} or {@code DANGER} raised while assembling the model. */
    INVALID_MODEL,
    /** A behavior every generated AWS client requires has no representation in the canonical model. */
    MISSING_SEMANTIC_ELEMENT,
    /** The canonical model represents a required behavior only with elements V3 does not support. */
    UNSUPPORTED_SEMANTIC_ELEMENT,
    /**
     * Two contributors (integrations, or an integration and the core generator {@code aws-codegen}) produced the same
     * generated file, through the file manifest or the writer delegator (Requirement 3.7).
     */
    INTEGRATION_OUTPUT_CONFLICT,
    /**
     * Two integrations decorated the symbol provider so that both change the Java symbol (namespace, name or
     * definition file) of the same shape, or the Java member name of the same member (Requirement 3.7).
     */
    INTEGRATION_SYMBOL_CONFLICT,
    /**
     * Two integrations preprocessed the model so that both change the same trait of the same shape, or one changes
     * the definition of a shape (adds, removes, retypes or restructures it) that another also changes
     * (Requirement 3.7).
     */
    INTEGRATION_MODEL_CONFLICT,
    /** One contributor wrote the same generated file more than once through the file manifest. */
    DUPLICATE_GENERATED_FILE,
    /**
     * Two or more shapes of the service closure (or a shape and a type the generator emits for the service, such as
     * the service's base exception) would be generated as the same Java type or into the same source file, compared
     * case-insensitively since file systems may be case-insensitive: for example the errors {@code ClusterFault} and
     * {@code ClusterException}, which both map to the V2 name {@code ClusterException}. Checked on the final symbol
     * provider, so renames by integrations are included.
     */
    GENERATED_NAME_COLLISION,
    /**
     * A generated file contains environment-dependent content, such as the absolute path of the output or model
     * directory, so the tree would differ between checkouts (Requirement 3.6).
     */
    ENVIRONMENT_DEPENDENT_OUTPUT
}
