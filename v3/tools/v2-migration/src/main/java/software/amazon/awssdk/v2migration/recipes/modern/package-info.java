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
 * Modern Auth and Modern Retry migration recipes (Requirements 18.1-18.9; design "Migration tooling flow"): moving
 * V2 Modern Auth and Modern Retry types to their V3 packages ({@code auth.modern-type-package},
 * {@code retry.modern-type-package}), and halting the migration of excluded legacy signer, presigner and retry APIs
 * with a diagnostic that names the Modern Auth or Modern Retry replacement, or states that no safe replacement exists
 * ({@code excluded.legacy-signer-retry}).
 *
 * <p>Every decision follows the Parity Manifest: a type is moved only when its entry is {@code ADAPTED} with an
 * automated recipe and its replacement is the same type name in a Modern Auth or Modern Retry package, and every
 * replacement named in a diagnostic is the entry's own. These recipes answer only for signer and retry constructs
 * ({@code ModernFacility}).
 */
package software.amazon.awssdk.v2migration.recipes.modern;
