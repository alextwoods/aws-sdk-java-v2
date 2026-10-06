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
import software.amazon.awssdk.v2migration.recipe.JavaNode;
import software.amazon.awssdk.v2migration.recipe.MigrationRecipe;
import software.amazon.awssdk.v2migration.recipe.RecipeContext;
import software.amazon.awssdk.v2migration.recipe.RecipeOutcome;

/**
 * Rewrites imports of, and references to, a V2 type that the Parity Manifest classifies {@code ADAPTED} with a
 * recipe and replaces with another type (a moved or renamed builder, client, model or configuration type; see
 * {@link Scope}). All uses in a file are planned together by {@link TypeRenames}: either every use is rewritten
 * consistently, or none is and each is reported.
 */
public final class RenamedTypeRecipe implements MigrationRecipe {

    @Override
    public String id() {
        return "imports.renamed-types";
    }

    @Override
    public String description() {
        return "Rewrites imports and references of builder, client, model and configuration types that the Parity "
               + "Manifest replaces with a renamed or moved V3 type.";
    }

    @Override
    public RecipeOutcome visit(RecipeContext context) {
        MigrationItem item = context.item();
        if (item.kind() != ItemKind.IMPORT && item.kind() != ItemKind.TYPE_REFERENCE) {
            return RecipeOutcome.notApplicable();
        }
        ManifestEntry entry = context.entry().orElse(null);
        if (!ManifestIds.hasRecipeReplacement(entry)) {
            return RecipeOutcome.notApplicable();
        }
        Optional<String> oldType = ManifestIds.type(entry.id());
        Optional<String> newType = ManifestIds.renamedType(context.manifest(), oldType.orElse(""));
        Optional<JavaNode> node = context.java();
        if (oldType.isEmpty() || newType.isEmpty() || Scope.of(oldType.get()).isEmpty() || node.isEmpty()) {
            return RecipeOutcome.notApplicable();
        }
        String path = item.location().path();
        TypeRenames.Plan plan = TypeRenames.plan(node.get(), path, context.text(path), oldType.get(), newType.get());
        if (plan.unsafe() != null) {
            return RecipeOutcome.unsafe(plan.unsafe());
        }
        Optional<Optional<TextEdit>> own = plan.at(item.location().startOffset(), item.location().endOffset());
        String from = ManifestIds.canonical(oldType.get());
        String to = ManifestIds.canonical(newType.get());
        if (own.isEmpty()) {
            return RecipeOutcome.unsafe("This use of " + from + " was not found among the uses planned for renaming "
                                        + "it to " + to);
        }
        if (own.get().isEmpty()) {
            return RecipeOutcome.preserve("The simple name resolves through the single-type import, which is "
                                          + "rewritten from " + from + " to " + to + ".");
        }
        return RecipeOutcome.transform("Rename " + from + " to " + to + " (Parity Manifest replacement "
                                       + entry.replacement() + ").", List.of(own.get().get()));
    }
}
