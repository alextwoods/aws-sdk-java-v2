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

import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.v2migration.ItemKind;
import software.amazon.awssdk.v2migration.MigrationItem;
import software.amazon.awssdk.v2migration.manifest.Classification;
import software.amazon.awssdk.v2migration.manifest.ManifestEntry;
import software.amazon.awssdk.v2migration.recipe.MigrationRecipe;
import software.amazon.awssdk.v2migration.recipe.RecipeContext;
import software.amazon.awssdk.v2migration.recipe.RecipeOutcome;

/**
 * Reports every use of a V2 configuration option without V3 behavior (Requirement 9.10): a configuration constant,
 * client or request override builder setting, or environment variable or system property name whose Parity Manifest
 * entry is {@code EXCLUDED} with no replacement. The use is left unchanged; the diagnostic names the option and its
 * manifest disposition in the form the V3 client configuration uses when it rejects the option at client
 * construction ({@code <option> (disposition <classification>)}, Requirement 9.9), and the engine adds the source
 * location. Excluded options with a replacement are left to the recipe family of the replacement.
 */
public final class UnsupportedOptionRecipe implements MigrationRecipe {

    private static final List<String> CONFIGURATION_PACKAGES = List.of(
        "software.amazon.awssdk.core.client.config.", "software.amazon.awssdk.awscore.client.config.");

    @Override
    public String id() {
        return "configuration.unsupported-options";
    }

    @Override
    public String description() {
        return "Reports V2 configuration options that have no V3 behavior, with the option name and Parity Manifest "
               + "disposition.";
    }

    @Override
    public RecipeOutcome visit(RecipeContext context) {
        MigrationItem item = context.item();
        ManifestEntry entry = context.entry().orElse(null);
        if (entry == null || entry.classification() != Classification.EXCLUDED || entry.replacement() != null) {
            return RecipeOutcome.notApplicable();
        }
        Optional<String> option = option(item.kind(), entry.id());
        if (option.isEmpty()) {
            return RecipeOutcome.notApplicable();
        }
        boolean external = item.kind() == ItemKind.CONFIGURATION_KEY;
        return RecipeOutcome.unsafe("The V2 configuration option " + option.get() + " (disposition "
                                    + entry.classification() + ") has no V3 behavior: "
                                    + (external ? "V3 does not read this setting"
                                                : "V3 rejects client construction when it is supplied")
                                    + ". Remove it"
                                    + (entry.migration() == null ? "." : "; " + entry.migration()));
    }

    /** The option name of an item that configures a client, or empty if the item is not a configuration option. */
    private static Optional<String> option(ItemKind kind, String id) {
        switch (kind) {
            case CONFIGURATION_KEY:
                return ManifestIds.configuration(id).map(ManifestIds.Configuration::key);
            case FIELD_ACCESS:
            case METHOD_INVOCATION:
                return ManifestIds.member(id)
                                  .filter(m -> !m.isConstructor() && m.isField() == (kind == ItemKind.FIELD_ACCESS))
                                  .filter(m -> isConfigurationScope(m.owner(), kind))
                                  .map(ManifestIds.Member::name);
            default:
                return Optional.empty();
        }
    }

    /**
     * Option constants and setters are declared by the client configuration packages ({@code core.client.config},
     * {@code awscore.client.config}); option setters also by request override builders and client (not model)
     * builders.
     */
    private static boolean isConfigurationScope(String owner, ItemKind kind) {
        if (CONFIGURATION_PACKAGES.stream().anyMatch(owner::startsWith)) {
            return true;
        }
        Optional<Scope> scope = Scope.of(owner);
        return kind == ItemKind.METHOD_INVOCATION && scope.isPresent()
               && (scope.get() == Scope.REQUEST_OVERRIDES
                   || (scope.get() == Scope.BUILDERS && !owner.contains(".model.")));
    }
}
