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

package software.amazon.awssdk.v2migration.recipes.runtime;

import com.sun.source.tree.ImportTree;
import com.sun.source.util.TreePath;
import java.util.List;
import javax.lang.model.element.TypeElement;
import software.amazon.awssdk.v2migration.ItemKind;
import software.amazon.awssdk.v2migration.TextEdit;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;
import software.amazon.awssdk.v2migration.recipe.JavaNode;
import software.amazon.awssdk.v2migration.recipe.MigrationRecipe;
import software.amazon.awssdk.v2migration.recipe.RecipeContext;
import software.amazon.awssdk.v2migration.recipe.RecipeOutcome;

/**
 * {@code conversions.unused-imports}: removes the single-type import of a V2 type whose every use in the file lies in
 * signature text that a declaration conversion of these recipes replaces ({@link ImportUses}), such as the
 * {@code ExecutionAttributes} and {@code SdkRequest} imports of an interceptor whose hooks are all converted. Left in
 * place, such an import would name a type V3 does not have. The declaration text stays a blank line; the engine
 * applies edits only within an item's own range.
 */
public final class ConvertedImportRecipe implements MigrationRecipe {

    @Override
    public String id() {
        return "conversions.unused-imports";
    }

    @Override
    public String description() {
        return "Removes imports of V2 types whose every use is rewritten away by interceptor and other declaration "
               + "conversions.";
    }

    @Override
    public RecipeOutcome visit(RecipeContext context) {
        if (context.item().kind() != ItemKind.IMPORT || context.java().isEmpty()
            || !removes(context.java().get(), context.manifest())) {
            return RecipeOutcome.notApplicable();
        }
        String imported = context.item().construct().replaceFirst("^import ", "");
        return RecipeOutcome.transform("Removed the import of " + imported
                                       + " because every use of it in this file is rewritten by a declaration "
                                       + "conversion.",
                                       List.of(TextEdit.replace(context.item().location(), "")));
    }

    /** True if the import at {@code node} names a V2 type whose every use the conversions rewrite away. */
    static boolean removes(JavaNode node, MigrationManifest manifest) {
        if (!(node.tree() instanceof ImportTree imported) || imported.isStatic()) {
            return false;
        }
        Symbols symbols = new Symbols(node);
        return symbols.element(new TreePath(node.path(), imported.getQualifiedIdentifier())) instanceof TypeElement type
               && symbols.isV2(type) && ImportUses.allRewritten(node, type, manifest);
    }
}
