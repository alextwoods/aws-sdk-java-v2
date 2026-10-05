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
 * Root package of the {@code software.amazon.awssdk:sdk-core} V3 artifact.
 *
 * <p>Owns: Base exceptions, request/response metadata, builders, typed configuration keys, request overrides, close
 * aggregation, and sync/async body transformers.
 *
 * <p>Allowed V3 module dependencies: {@code smithy-runtime} (for shape identity). Forbidden dependencies: Smithy model
 * parser, code-generation, and HTTP implementation artifacts.
 *
 * <p>This package holds {@link software.amazon.awssdk.core.SdkBytes}, the V2-compatible immutable value of Smithy
 * {@code blob} members of generated types.
 */
package software.amazon.awssdk.core;
