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
 * The core V2-to-V3 recipes (Requirements 9.10, 17.3–17.7, 17.9): Maven dependencies, BOM imports, version
 * properties and the Java language level; renamed types and their imports; renamed builder, client, model,
 * configuration and request override members; renamed configuration settings; and diagnostics for configuration
 * options without V3 behavior and for preserved members whose signature uses a renamed or excluded type.
 *
 * <p>Every transformation is read from the Parity Manifest: an artifact's V3 coordinates from its {@code artifact:}
 * entry and the version from the manifest's release candidate, and a source rename from an {@code ADAPTED} entry with
 * a {@code RECIPE} migration whose {@code replacement} has the same shape (type for type, member of the same
 * signature for member, setting of the same source for setting). Recipes answer only for the constructs they own
 * ({@link software.amazon.awssdk.v2migration.recipe.core.Scope}); a construct they own but cannot rewrite safely is
 * answered {@code unsafe} with the reason, so it is left byte-for-byte unchanged and reported. The one constant that
 * is not read from the manifest is the V3 minimum Java release, a property of the V3 platform rather than of any V2
 * API.
 */
package software.amazon.awssdk.v2migration.recipe.core;
