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
import software.amazon.awssdk.v2migration.TextEdit;
import software.amazon.awssdk.v2migration.manifest.ManifestEntry;
import software.amazon.awssdk.v2migration.recipe.MigrationRecipe;
import software.amazon.awssdk.v2migration.recipe.RecipeContext;
import software.amazon.awssdk.v2migration.recipe.RecipeOutcome;
import software.amazon.awssdk.v2migration.recipe.core.ManifestIds.Configuration;

/**
 * Rewrites the string literal of a V2 environment variable or system property name, passed to
 * {@code System.getenv} or the system-property methods, that the Parity Manifest classifies {@code ADAPTED} with a
 * recipe and replaces with a setting of the same source under another name. A replacement read from a different
 * source (an environment variable for a system property, or the reverse) needs a different API call and is reported.
 */
public final class RenamedConfigurationKeyRecipe implements MigrationRecipe {

    @Override
    public String id() {
        return "configuration.renamed-keys";
    }

    @Override
    public String description() {
        return "Rewrites environment variable and system property names that the Parity Manifest renames.";
    }

    @Override
    public RecipeOutcome visit(RecipeContext context) {
        MigrationItem item = context.item();
        ManifestEntry entry = context.entry().orElse(null);
        if (item.kind() != ItemKind.CONFIGURATION_KEY || !ManifestIds.hasRecipeReplacement(entry)) {
            return RecipeOutcome.notApplicable();
        }
        Optional<Configuration> from = ManifestIds.configuration(entry.id());
        Optional<Configuration> to = ManifestIds.configuration(entry.replacement());
        if (from.isEmpty() || to.isEmpty() || from.get().equals(to.get())) {
            return RecipeOutcome.notApplicable();
        }
        if (!from.get().source().equals(to.get().source())) {
            return RecipeOutcome.unsafe("V3 reads this setting as " + entry.replacement() + ", from a different "
                                        + "source than " + entry.id() + "; read it through the matching API");
        }
        String written = context.text(item.location());
        if (!written.equals(literal(from.get().key()))) {
            return RecipeOutcome.unsafe("The setting name is not written as one plain string literal");
        }
        return RecipeOutcome.transform("Rename the setting " + from.get().key() + " to " + to.get().key()
                                       + " (Parity Manifest replacement " + entry.replacement() + ").",
                                       List.of(TextEdit.replace(item.location(), literal(to.get().key()))));
    }

    /** The Java string literal of {@code value}. */
    static String literal(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20 || c == 0x7f) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }
}
