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

package software.amazon.awssdk.v2migration.recipe.core;

import java.util.Optional;
import java.util.Set;
import software.amazon.awssdk.v2migration.ItemKind;
import software.amazon.awssdk.v2migration.MigrationCategory;
import software.amazon.awssdk.v2migration.manifest.EntryKind;

/**
 * Which V2 types the core source recipes own. The partition follows {@link MigrationCategory}, derived from the V2
 * package of the type that declares a construct, so the core recipes never answer for interceptor, HTTP, retry,
 * signer, streaming or high-level-library constructs, which other recipe families own:
 * <ul>
 *   <li>{@link #REQUEST_OVERRIDES}: the request override configuration types and their builders;</li>
 *   <li>{@link #BUILDERS}: other {@link MigrationCategory#BUILDER} types (client and model builders);</li>
 *   <li>{@link #CLIENTS}: {@link MigrationCategory#CLIENT} types and the other generated types of service packages
 *       (models, exceptions, utilities);</li>
 *   <li>{@link #CONFIGURATION}: {@link MigrationCategory#CONFIGURATION} types (client configuration, regions,
 *       credentials and identity, profiles, defaults modes).</li>
 * </ul>
 * Presigner packages are signing facilities and belong to no core scope.
 */
enum Scope {
    REQUEST_OVERRIDES,
    BUILDERS,
    CLIENTS,
    CONFIGURATION;

    private static final String V2 = "software.amazon.awssdk.";

    /** The request override configuration types (and their builders, {@code $Builder}). */
    private static final Set<String> REQUEST_OVERRIDE_TYPES = Set.of(
        V2 + "core.RequestOverrideConfiguration", V2 + "awscore.AwsRequestOverrideConfiguration");

    /** The scope of constructs declared by the V2 type {@code ownerBinaryName}, if the core recipes own it. */
    static Optional<Scope> of(String ownerBinaryName) {
        if (ownerBinaryName.contains(".presigner.")) {
            // Presigners are signing facilities, owned by the Modern Auth recipes.
            return Optional.empty();
        }
        String outer = ownerBinaryName.endsWith("$Builder")
                       ? ownerBinaryName.substring(0, ownerBinaryName.length() - "$Builder".length())
                       : ownerBinaryName;
        if (REQUEST_OVERRIDE_TYPES.contains(outer)) {
            return Optional.of(REQUEST_OVERRIDES);
        }
        MigrationCategory category = MigrationCategory.of(ItemKind.TYPE_REFERENCE, EntryKind.typeId(ownerBinaryName));
        switch (category) {
            case BUILDER:
                return Optional.of(BUILDERS);
            case CLIENT:
                return Optional.of(CLIENTS);
            case CONFIGURATION:
                return Optional.of(CONFIGURATION);
            case API:
                return ownerBinaryName.startsWith(V2 + "services.") ? Optional.of(CLIENTS) : Optional.empty();
            default:
                return Optional.empty();
        }
    }
}
