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

package software.amazon.awssdk.v2migration.recipes.modern;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import software.amazon.awssdk.v2migration.ItemKind;
import software.amazon.awssdk.v2migration.MigrationItem;
import software.amazon.awssdk.v2migration.TextEdit;
import software.amazon.awssdk.v2migration.manifest.Classification;
import software.amazon.awssdk.v2migration.manifest.EntryKind;
import software.amazon.awssdk.v2migration.manifest.ManifestEntry;
import software.amazon.awssdk.v2migration.manifest.MigrationKind;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;
import software.amazon.awssdk.v2migration.recipe.JavaNode;
import software.amazon.awssdk.v2migration.recipe.MigrationRecipe;
import software.amazon.awssdk.v2migration.recipe.RecipeContext;
import software.amazon.awssdk.v2migration.recipe.RecipeOutcome;

/**
 * Moves imports of, and references to, a V2 Modern Auth or Modern Retry type to the V3 package the Parity Manifest
 * names (design "Migration tooling flow", step 4: Modern Auth/Retry replacements). It acts only on an entry that is
 * {@code ADAPTED} with an automated recipe and whose replacement is a type with the same name in another package (for
 * example {@code software.amazon.awssdk.retries.api.RetryStrategy} to {@code software.amazon.awssdk.retries.RetryStrategy}),
 * so the rename is purely a package move. The members of a moved type are separate items with their own entries.
 *
 * <ul>
 *   <li>A single-type import written as the plain qualified name is rewritten to the V3 name.</li>
 *   <li>A fully qualified reference is rewritten to the V3 name.</li>
 *   <li>A simple name is preserved when the unit has the single-type import of the type, which is rewritten; a
 *       nested type qualified by its enclosing type (for example {@code RetryStrategy.Builder}) is preserved when the
 *       enclosing type is itself moved to the same V3 type's enclosing type.</li>
 *   <li>Anything else (a simple name resolved through an on-demand import, the same package or inheritance, or a name
 *       interrupted by comments) is unsafe and left unchanged.</li>
 * </ul>
 */
public abstract class ModernTypeRecipe implements MigrationRecipe {
    private final ModernFacility facility;
    private final String id;

    ModernTypeRecipe(ModernFacility facility, String id) {
        this.facility = facility;
        this.id = id;
    }

    /** Modern Auth types: auth schemes, scheme options, signers and signer properties. */
    public static final class Auth extends ModernTypeRecipe {
        public Auth() {
            super(ModernFacility.AUTH, "auth.modern-type-package");
        }
    }

    /** Modern Retry types: retry strategies, their builders, backoff strategies and tokens. */
    public static final class Retry extends ModernTypeRecipe {
        public Retry() {
            super(ModernFacility.RETRY, "retry.modern-type-package");
        }
    }

    @Override
    public final String id() {
        return id;
    }

    @Override
    public final String description() {
        return "Moves imports and qualified references of V2 " + facility.label() + " types to the V3 package the "
               + "Parity Manifest replaces them with.";
    }

    @Override
    public final RecipeOutcome visit(RecipeContext context) {
        MigrationItem item = context.item();
        if (item.kind() != ItemKind.IMPORT && item.kind() != ItemKind.TYPE_REFERENCE) {
            return RecipeOutcome.notApplicable();
        }
        Optional<Move> move = context.entry().flatMap(this::move);
        Optional<JavaNode> node = context.java();
        if (move.isEmpty() || node.isEmpty()) {
            return RecipeOutcome.notApplicable();
        }
        String path = item.location().path();
        String text = context.text(path);
        Tree tree = node.get().tree();
        if (item.kind() == ItemKind.IMPORT) {
            return tree instanceof ImportTree imported ? importOutcome(node.get(), imported, path, text, move.get())
                                                       : RecipeOutcome.notApplicable();
        }
        if (tree instanceof IdentifierTree) {
            return importedByUnit(node.get(), move.get().from())
                   ? RecipeOutcome.preserve("The simple name resolves through the single-type import of "
                                            + move.get().fromName() + ", which is moved to " + move.get().toName()
                                            + ".")
                   : RecipeOutcome.unsafe(item.construct() + " refers to " + move.get().fromName() + " by its simple "
                                          + "name without a single-type import (through an on-demand import, the "
                                          + "same package or inheritance), so its V3 name cannot be resolved; import "
                                          + move.get().toName() + ".");
        }
        if (tree instanceof MemberSelectTree select) {
            return qualifiedOutcome(node.get(), select, path, text, move.get(), context.manifest());
        }
        return RecipeOutcome.notApplicable();
    }

    /** A package move from V2 type {@code from} to V3 type {@code to}, both binary names. */
    private record Move(String from, String to, ManifestEntry entry) {
        String fromName() {
            return Names.canonical(from);
        }

        String toName() {
            return Names.canonical(to);
        }

        String summary() {
            return "Move " + fromName() + " to " + toName() + " (" + entry.classification() + " Parity Manifest "
                   + "replacement " + entry.replacement() + ").";
        }
    }

    /** The package move of {@code entry}, if it is one of this facility's recipe-backed package moves. */
    private Optional<Move> move(ManifestEntry entry) {
        if (entry.classification() != Classification.ADAPTED || entry.migrationKind() != MigrationKind.RECIPE
            || !facility.owns(entry) || !facility.isModern(entry.replacement())) {
            return Optional.empty();
        }
        Optional<String> from = Names.type(entry.id());
        Optional<String> to = Names.type(entry.replacement());
        if (from.isEmpty() || to.isEmpty() || !Names.nestedName(from.get()).equals(Names.nestedName(to.get()))
            || Names.packageName(from.get()).equals(Names.packageName(to.get()))) {
            return Optional.empty();
        }
        return Optional.of(new Move(from.get(), to.get(), entry));
    }

    private static RecipeOutcome importOutcome(JavaNode node, ImportTree imported, String path, String text,
                                               Move move) {
        if (imported.isStatic()) {
            return RecipeOutcome.notApplicable();
        }
        Tree qualid = imported.getQualifiedIdentifier();
        int start = node.start(qualid);
        int end = node.end(qualid);
        if (start < 0 || end > text.length() || !text.substring(start, end).equals(move.fromName())) {
            return RecipeOutcome.unsafe("The import of " + move.fromName() + " is not written as a plain qualified "
                                        + "name, so it is left unchanged; import " + move.toName() + ".");
        }
        return RecipeOutcome.transform(move.summary(), List.of(new TextEdit(path, start, end, move.toName())));
    }

    private RecipeOutcome qualifiedOutcome(JavaNode node, MemberSelectTree select, String path, String text,
                                           Move move, MigrationManifest manifest) {
        int start = node.start(select);
        int end = node.end(select);
        String source = start < 0 || end > text.length() ? "" : text.substring(start, end);
        Element qualifier = node.trees().getElement(new TreePath(node.path(), select.getExpression()));
        if (qualifier instanceof PackageElement && source.equals(move.fromName())) {
            return RecipeOutcome.transform(move.summary(), List.of(new TextEdit(path, start, end, move.toName())));
        }
        if (qualifier instanceof TypeElement enclosing) {
            String enclosingName = node.elements().getBinaryName(enclosing).toString();
            Optional<Move> outer = manifest.entry(EntryKind.typeId(enclosingName)).flatMap(this::move);
            if (outer.isPresent() && Names.enclosing(move.to()).equals(Optional.of(outer.get().to()))) {
                return RecipeOutcome.preserve("The nested name is qualified by " + outer.get().fromName()
                                              + ", which is moved to " + outer.get().toName() + ".");
            }
        }
        return RecipeOutcome.unsafe("The qualified name " + source.strip().replaceAll("\\s+", " ") + " of "
                                    + move.fromName() + " cannot be rewritten to " + move.toName() + " safely.");
    }

    /** True if the unit of {@code node} has the single-type import of {@code binaryName}. */
    private static boolean importedByUnit(JavaNode node, String binaryName) {
        String canonical = Names.canonical(binaryName);
        for (ImportTree i : node.unit().getImports()) {
            if (!i.isStatic() && i.getQualifiedIdentifier().toString().equals(canonical)) {
                return true;
            }
        }
        return false;
    }
}
