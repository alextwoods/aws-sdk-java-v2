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

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;
import software.amazon.awssdk.v2migration.recipe.JavaNode;

/**
 * Whether an imported V2 type is still used once declaration conversions are applied: an import whose type is used
 * only inside the signature ranges that {@linkplain OverrideConversion#check converting declarations} of these
 * recipes' families replace (such as the {@code ExecutionAttributes} parameter of a converted hook) becomes unused, and
 * is removed rather than left naming a type V3 does not have. A declaration that is not converted keeps its imports.
 */
final class ImportUses {

    private ImportUses() {
    }

    /** True if {@code type} has uses outside imports in {@code node}'s unit and every one is rewritten away. */
    static boolean allRewritten(JavaNode node, TypeElement type, MigrationManifest manifest) {
        List<TreePath> uses = new ArrayList<>();
        new TreePathScanner<Void, Void>() {
            @Override
            public Void visitImport(ImportTree tree, Void unused) {
                return null;
            }

            @Override
            public Void visitIdentifier(IdentifierTree tree, Void unused) {
                use(tree);
                return super.visitIdentifier(tree, unused);
            }

            @Override
            public Void visitMemberSelect(MemberSelectTree tree, Void unused) {
                use(tree);
                return super.visitMemberSelect(tree, unused);
            }

            private void use(Tree tree) {
                Element e = new Symbols(node).element(getCurrentPath());
                if (type.equals(e)) {
                    uses.add(getCurrentPath());
                }
            }
        }.scan(node.unit(), null);
        if (uses.isEmpty()) {
            return false;
        }
        for (TreePath use : uses) {
            if (!rewritten(node, use, manifest)) {
                return false;
            }
        }
        return true;
    }

    private static boolean rewritten(JavaNode node, TreePath use, MigrationManifest manifest) {
        JavaNode at = new JavaNode(node.unit(), use, node.trees(), node.elements(), node.types());
        Optional<TreePath> declaration = OverrideConversion.enclosingDeclaration(at, use);
        Optional<Family> owner = declaration.flatMap(d -> OverrideConversion.overriddenId(at, d))
                                            .flatMap(ApiIds::owner).flatMap(Family::of);
        if (owner.isEmpty() || !(owner.get().conversion().check(at, declaration.get(), manifest)
                                     instanceof OverrideConversion.Checked checked)) {
            return false;
        }
        int start = node.start(use.getLeaf());
        int end = node.end(use.getLeaf());
        return checked.signature().claimed().stream().anyMatch(r -> r[0] <= start && end <= r[1]);
    }
}
