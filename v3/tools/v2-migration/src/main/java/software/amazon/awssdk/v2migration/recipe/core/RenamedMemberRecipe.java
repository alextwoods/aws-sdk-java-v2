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

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.v2migration.ItemKind;
import software.amazon.awssdk.v2migration.MigrationItem;
import software.amazon.awssdk.v2migration.TextEdit;
import software.amazon.awssdk.v2migration.manifest.ManifestEntry;
import software.amazon.awssdk.v2migration.recipe.JavaNode;
import software.amazon.awssdk.v2migration.recipe.MigrationRecipe;
import software.amazon.awssdk.v2migration.recipe.RecipeContext;
import software.amazon.awssdk.v2migration.recipe.RecipeOutcome;
import software.amazon.awssdk.v2migration.recipe.core.ManifestIds.Member;

/**
 * Renames uses of V2 methods and fields that the Parity Manifest classifies {@code ADAPTED} with a recipe and
 * replaces with a member of the same signature up to renamed types: the same owner under a new name, or the same name
 * on the owner's renamed type, where every type of the signature that differs is one the manifest renames (whose
 * references {@link RenamedTypeRecipe} or the recipe family of the type rewrites). A use whose name stays is preserved;
 * one whose name changes is renamed. One recipe per {@link Scope}, so the report shows builder, client, configuration
 * and request override changes separately.
 *
 * <p>A qualified use ({@code builder.oldName(...)}, {@code Type.OLD}) is renamed in place. An unqualified use is
 * renamed unless it may resolve through a static import, and static imports of such members are reported, because a
 * static import cannot be rewritten without rewriting all its uses together. A replacement with a different signature
 * is not a rename and is left to the manifest's diagnostic; overriding declarations are left to the recipe family of
 * their hook.
 */
public abstract class RenamedMemberRecipe implements MigrationRecipe {

    private final Scope scope;
    private final String id;
    private final String description;

    RenamedMemberRecipe(Scope scope, String id, String description) {
        this.scope = scope;
        this.id = id;
        this.description = description;
    }

    /** Model and client builder members. */
    public static final class Builders extends RenamedMemberRecipe {
        public Builders() {
            super(Scope.BUILDERS, "builders.renamed-members",
                  "Renames uses of client and model builder methods that the Parity Manifest renames.");
        }
    }

    /** Service client and generated model members. */
    public static final class Clients extends RenamedMemberRecipe {
        public Clients() {
            super(Scope.CLIENTS, "clients.renamed-members",
                  "Renames uses of service client and generated model members that the Parity Manifest renames.");
        }
    }

    /** Client configuration, region, identity and profile members. */
    public static final class Configuration extends RenamedMemberRecipe {
        public Configuration() {
            super(Scope.CONFIGURATION, "configuration.renamed-members",
                  "Renames uses of client configuration, region, identity and profile members that the Parity "
                  + "Manifest renames.");
        }
    }

    /** Request override configuration members. */
    public static final class RequestOverrides extends RenamedMemberRecipe {
        public RequestOverrides() {
            super(Scope.REQUEST_OVERRIDES, "configuration.request-overrides",
                  "Renames uses of request override configuration members that the Parity Manifest renames.");
        }
    }

    @Override
    public final String id() {
        return id;
    }

    @Override
    public final String description() {
        return description;
    }

    @Override
    public final RecipeOutcome visit(RecipeContext context) {
        MigrationItem item = context.item();
        ItemKind kind = item.kind();
        if (kind != ItemKind.METHOD_INVOCATION && kind != ItemKind.FIELD_ACCESS && kind != ItemKind.CONSTRUCTOR_CALL
            && kind != ItemKind.STATIC_IMPORT) {
            return RecipeOutcome.notApplicable();
        }
        ManifestEntry entry = context.entry().orElse(null);
        if (!ManifestIds.hasRecipeReplacement(entry)) {
            return RecipeOutcome.notApplicable();
        }
        Optional<Member> old = ManifestIds.member(entry.id());
        Optional<Member> replacement = ManifestIds.member(entry.replacement());
        if (old.isEmpty() || replacement.isEmpty() || Scope.of(old.get().owner()).orElse(null) != scope) {
            return RecipeOutcome.notApplicable();
        }
        Member from = old.get();
        Member to = replacement.get();
        boolean ownerMoves = !to.owner().equals(from.owner());
        if (ownerMoves && !ManifestIds.renamedType(context.manifest(), from.owner()).filter(to.owner()::equals)
                                      .isPresent()) {
            // Moved to an unrelated type: the receiver changes, which is not a rename.
            return RecipeOutcome.notApplicable();
        }
        // The signature may change only by the types the manifest renames (the owner included), whose references
        // their own recipes rewrite.
        String expected = ManifestIds.substituteRenamed(context.manifest(), from.descriptor());
        boolean retypes = !expected.equals(from.descriptor());
        boolean renames = !to.name().equals(from.name());
        if (!to.descriptor().equals(expected) || (!renames && !ownerMoves && !retypes)
            || from.isConstructor() != to.isConstructor() || from.isField() != to.isField()) {
            return RecipeOutcome.notApplicable();
        }
        String target = ManifestIds.canonical(to.owner()) + "#" + to.name();
        if (renames && !ManifestIds.isIdentifier(to.name())) {
            return RecipeOutcome.unsafe("The replacement name " + to.name() + " is not a Java identifier");
        }
        if (kind == ItemKind.STATIC_IMPORT && (renames || ownerMoves)) {
            return RecipeOutcome.unsafe("The static import names a member that the Parity Manifest replaces with "
                                        + target + "; rewrite the static import and its unqualified uses together");
        }
        if (!renames) {
            return RecipeOutcome.preserve("The member keeps its name as " + target + "; only types that the Parity "
                                          + "Manifest renames change around it ("
                                          + (ownerMoves ? "its receiver type" : "")
                                          + (ownerMoves && retypes ? " and " : "")
                                          + (retypes ? "its signature" : "")
                                          + "), and their references are rewritten by their own recipes.");
        }
        return rename(context, from, to, target);
    }

    private static RecipeOutcome rename(RecipeContext context, Member from, Member to, String target) {
        Optional<JavaNode> java = context.java();
        if (java.isEmpty()) {
            return RecipeOutcome.unsafe("The construct has no attributed syntax");
        }
        JavaNode node = java.get();
        Tree tree = node.tree();
        Tree name = tree instanceof MethodInvocationTree invocation ? invocation.getMethodSelect() : tree;
        int start;
        int end;
        if (name instanceof MemberSelectTree select) {
            end = node.end(select);
            start = end - from.name().length();
        } else if (name instanceof IdentifierTree identifier) {
            TreePath identifierPath = identifier == tree ? node.path() : new TreePath(node.path(), identifier);
            Element element = node.trees().getElement(identifierPath);
            if (element == null || (element.getModifiers().contains(Modifier.STATIC)
                                    && staticallyImported(node, from.name()))) {
                return RecipeOutcome.unsafe("The unqualified use of " + from.name() + " may resolve through a static "
                                            + "import; rewrite the static import and its uses to " + target
                                            + " together");
            }
            start = node.start(identifier);
            end = node.end(identifier);
        } else {
            return RecipeOutcome.unsafe("The member name of this use could not be located");
        }
        String path = context.item().location().path();
        String text = context.text(path);
        if (start < 0 || end > text.length() || start > end || !text.substring(start, end).equals(from.name())) {
            return RecipeOutcome.unsafe("The member name " + from.name() + " is not written contiguously at this use");
        }
        return RecipeOutcome.transform("Rename " + ManifestIds.canonical(from.owner()) + "#" + from.name() + " to "
                                       + target + " (Parity Manifest replacement).",
                                       List.of(new TextEdit(path, start, end, to.name())));
    }

    /** True if a static import of the unit could import a member named {@code name}. */
    private static boolean staticallyImported(JavaNode node, String name) {
        for (ImportTree i : node.unit().getImports()) {
            if (i.isStatic() && i.getQualifiedIdentifier() instanceof MemberSelectTree select) {
                String imported = select.getIdentifier().toString();
                if (imported.equals(name) || imported.equals("*")) {
                    return true;
                }
            }
        }
        return false;
    }
}
