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

import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import software.amazon.awssdk.v2migration.ItemKind;
import software.amazon.awssdk.v2migration.MigrationItem;
import software.amazon.awssdk.v2migration.SourceLocation;
import software.amazon.awssdk.v2migration.TextEdit;
import software.amazon.awssdk.v2migration.manifest.ManifestEntry;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;
import software.amazon.awssdk.v2migration.recipe.JavaNode;
import software.amazon.awssdk.v2migration.recipe.MigrationRecipe;
import software.amazon.awssdk.v2migration.recipe.RecipeContext;
import software.amazon.awssdk.v2migration.recipe.RecipeOutcome;

/**
 * A recipe that converts the Java uses of one {@link Family} to the V3 replacements the Parity Manifest names for
 * recipe-replaced entries ({@link Replacements}):
 * <ul>
 *   <li>a single-type import of a replaced type imports the replacement, and a type reference names the replacement
 *       ({@link TypeNames}: unqualified when the unit's imports resolve it once rewritten);</li>
 *   <li>a method call, field access or constructor call whose replacement has the same (or replaced) owner and
 *       compatible parameters is renamed, or preserved when only replaced types around it change; a call whose
 *       result changes type is converted only if the result is discarded;</li>
 *   <li>a method declaration that overrides a recipe-replaced V2 method is converted by {@link OverrideConversion},
 *       together with the types of its signature and the accessor calls on its rewritten parameters, which that
 *       conversion owns.</li>
 * </ul>
 * Every other use of a recipe-replaced entry is unsafe and left unchanged with the reason; entries that are not
 * recipe-replaced keep the manifest's disposition. The engine's client and configuration recipes own the imports of
 * their own surfaces; these recipes edit only the imports of their families' types.
 */
abstract class ReplacementRecipe implements MigrationRecipe {

    private final String id;
    private final String description;
    private final Family family;

    ReplacementRecipe(String id, String description, Family family) {
        this.id = id;
        this.description = description;
        this.family = family;
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
        Optional<JavaNode> java = context.java();
        if (java.isEmpty()) {
            return RecipeOutcome.notApplicable();
        }
        JavaNode node = java.get();
        if (item.kind() == ItemKind.METHOD_OVERRIDE) {
            return override(context, node);
        }
        if (item.kind() == ItemKind.TYPE_REFERENCE || item.kind() == ItemKind.METHOD_INVOCATION) {
            Optional<RecipeOutcome> claimed = claimed(context, node);
            if (claimed.isPresent()) {
                return claimed.get();
            }
        }
        if (item.id() == null || !owns(item.id())) {
            return RecipeOutcome.notApplicable();
        }
        if (item.kind() == ItemKind.IMPORT) {
            return typeImport(context, node);
        }
        Optional<ManifestEntry> entry = context.entry().flatMap(Replacements::recipe);
        if (entry.isEmpty()) {
            return RecipeOutcome.notApplicable();
        }
        switch (item.kind()) {
            case TYPE_REFERENCE:
                return typeReference(context, node, entry.get());
            case METHOD_INVOCATION:
            case FIELD_ACCESS:
            case CONSTRUCTOR_CALL:
                return memberUse(context, node, entry.get());
            default:
                return RecipeOutcome.notApplicable();
        }
    }

    private boolean owns(String entryId) {
        return ApiIds.owner(entryId).map(family::owns).orElse(false);
    }

    private RecipeOutcome override(RecipeContext context, JavaNode node) {
        MigrationItem item = context.item();
        if (item.id() == null || !owns(item.id())) {
            return RecipeOutcome.notApplicable();
        }
        String path = item.location().path();
        OverrideConversion.Plan plan = family.conversion().plan(node, node.path(), path, context.text(path),
                                                                context.manifest());
        if (plan instanceof OverrideConversion.Unsafe unsafe) {
            return RecipeOutcome.unsafe(unsafe.reason());
        }
        if (plan instanceof OverrideConversion.Convert convert) {
            return convert.edits().isEmpty()
                   ? RecipeOutcome.preserve(convert.summary() + " The declaration already matches.")
                   : RecipeOutcome.transform(convert.summary(), convert.edits());
        }
        return RecipeOutcome.notApplicable();
    }

    /**
     * The answer for an item that a declaration conversion rewrites, if it is one: this family answers for the
     * declarations it converts, and every other family stays out of the way, whatever the item's own surface.
     */
    private Optional<RecipeOutcome> claimed(RecipeContext context, JavaNode node) {
        Optional<TreePath> declaration = OverrideConversion.enclosingDeclaration(node, node.path());
        Optional<String> overridden = declaration.flatMap(d -> OverrideConversion.overriddenId(node, d));
        if (overridden.isEmpty()) {
            return Optional.empty();
        }
        MigrationItem item = context.item();
        Optional<Family> owner = ApiIds.owner(overridden.get()).flatMap(Family::of);
        if (owner.isEmpty()) {
            return Optional.empty();
        }
        boolean ours = owner.get() == family;
        String path = item.location().path();
        OverrideConversion.Plan plan = owner.get().conversion().plan(node, declaration.get(), path, context.text(path),
                                                                     context.manifest());
        if (!(plan instanceof OverrideConversion.Convert convert)) {
            return Optional.empty();
        }
        SourceLocation at = item.location();
        long line = node.unit().getLineMap().getLineNumber(node.start(declaration.get().getLeaf()));
        if (item.kind() == ItemKind.TYPE_REFERENCE && convert.claims(at.startOffset(), at.endOffset())) {
            return Optional.of(ours ? RecipeOutcome.preserve("Rewritten as part of the declaration at line " + line
                                                             + " by " + id + ": " + convert.summary())
                                    : RecipeOutcome.notApplicable());
        }
        if (item.kind() == ItemKind.METHOD_INVOCATION && convert.accessors().containsKey(at.startOffset())) {
            if (!ours) {
                return Optional.of(RecipeOutcome.notApplicable());
            }
            String newName = convert.accessors().get(at.startOffset());
            String oldName = ((MemberSelectTree) ((MethodInvocationTree) node.tree()).getMethodSelect())
                .getIdentifier().toString();
            if (newName.equals(oldName)) {
                return Optional.of(RecipeOutcome.preserve("The V3 context of the declaration converted at line "
                                                          + line + " has the same accessor " + oldName + "."));
            }
            return Optional.of(RecipeOutcome.transform(
                "Renamed the context accessor " + oldName + " to " + newName + " for the declaration converted at "
                + "line " + line + ".",
                List.of(new TextEdit(path, at.startOffset(), at.startOffset() + oldName.length(), newName))));
        }
        return Optional.empty();
    }

    /**
     * A single-type import of one of this family's types is rewritten to the type's recipe replacement, unless
     * declaration conversions rewrite away every use of the type, when {@link ConvertedImportRecipe} removes it.
     */
    private RecipeOutcome typeImport(RecipeContext context, JavaNode node) {
        Optional<String> old = ApiIds.type(context.item().id());
        if (old.isEmpty() || !(node.tree() instanceof ImportTree imported) || imported.isStatic()) {
            return RecipeOutcome.notApplicable();
        }
        Tree qualid = imported.getQualifiedIdentifier();
        String path = context.item().location().path();
        String text = context.text(path);
        String oldName = ApiIds.sourceName(old.get());
        if (ConvertedImportRecipe.removes(node, context.manifest())) {
            return RecipeOutcome.notApplicable();
        }
        Optional<ManifestEntry> entry = context.entry().flatMap(Replacements::recipe);
        Optional<String> replacement = entry.flatMap(e -> ApiIds.type(e.replacement()));
        if (replacement.isEmpty()) {
            return RecipeOutcome.notApplicable();
        }
        int start = node.start(qualid);
        int end = node.end(qualid);
        String newName = ApiIds.sourceName(replacement.get());
        if (!text.substring(start, end).equals(oldName)) {
            return RecipeOutcome.unsafe("The import of " + oldName + " is not written as a plain qualified name.");
        }
        String clash = TypeNames.importClash(node, context.manifest(), oldName, newName);
        if (clash != null) {
            return RecipeOutcome.unsafe("Importing " + newName + " instead of " + oldName + " would clash: " + clash
                                        + ".");
        }
        return RecipeOutcome.transform("Imported the V3 replacement " + newName + " instead of V2 " + oldName + ".",
                                       List.of(new TextEdit(path, start, end, newName)));
    }

    private RecipeOutcome typeReference(RecipeContext context, JavaNode node, ManifestEntry entry) {
        Optional<String> replacement = ApiIds.type(entry.replacement());
        if (replacement.isEmpty()) {
            return RecipeOutcome.unsafe("The manifest replaces this type by " + entry.replacement()
                                        + ", which is not a type, so the reference cannot be renamed.");
        }
        Symbols symbols = new Symbols(node);
        TreePath parent = node.path().getParentPath();
        if (parent != null && parent.getLeaf() instanceof MemberSelectTree select
            && select.getExpression() == node.tree()
            && symbols.element(parent) instanceof TypeElement nested && symbols.isV2(nested)) {
            // The qualifier of a nested V2 type: the nested type's own reference decides.
            String nestedBinary = symbols.binary(nested);
            if (Replacements.type(context.manifest(), nestedBinary).isPresent()) {
                return RecipeOutcome.preserve("Rewritten with the qualified type name "
                                              + ApiIds.sourceName(nestedBinary) + ", whose manifest replacement "
                                              + "names its V3 type.");
            }
            return RecipeOutcome.unsafe("Renaming the qualifier would change the nested type "
                                        + ApiIds.sourceName(nestedBinary) + ", which the manifest does not replace.");
        }
        Tree tree = node.tree();
        if (!(tree instanceof IdentifierTree) && !(tree instanceof MemberSelectTree)) {
            return RecipeOutcome.unsafe("The type reference has an unexpected form.");
        }
        // A package-qualified reference stays qualified; a simple or type-qualified one uses the shortest name the
        // unit resolves once its imports are rewritten.
        boolean packageQualified = tree instanceof MemberSelectTree select
                                   && symbols.element(new TreePath(node.path(), select.getExpression()))
                                       instanceof PackageElement;
        String text = packageQualified ? ApiIds.sourceName(replacement.get())
                                       : TypeNames.reference(node, context.manifest(), replacement.get());
        if (context.text(context.item().location()).equals(text)) {
            return RecipeOutcome.preserve("The V3 replacement " + ApiIds.sourceName(replacement.get())
                                          + " is referenced by the same name once the imports are rewritten.");
        }
        return RecipeOutcome.transform("Renamed V2 " + ApiIds.sourceName(ApiIds.type(entry.id()).orElseThrow())
                                       + " to its V3 replacement " + ApiIds.sourceName(replacement.get()) + ".",
                                       List.of(TextEdit.replace(context.item().location(), text)));
    }

    private RecipeOutcome memberUse(RecipeContext context, JavaNode node, ManifestEntry entry) {
        MigrationManifest manifest = context.manifest();
        MigrationItem item = context.item();
        ApiIds.Member v2 = ApiIds.member(entry.id()).orElse(null);
        ApiIds.Member v3 = ApiIds.member(entry.replacement()).orElse(null);
        if (v2 == null || v3 == null || v2.field() != v3.field()
            || v2.name().equals("<init>") != v3.name().equals("<init>")) {
            return RecipeOutcome.unsafe("The manifest replaces " + entry.id() + " by " + entry.replacement()
                                        + ", which is not the same kind of member.");
        }
        if (!Replacements.sameOwner(manifest, v2.owner(), v3.owner())) {
            return RecipeOutcome.unsafe("V3 " + v3.describe() + " is declared by " + ApiIds.sourceName(v3.owner())
                                        + " instead of " + ApiIds.sourceName(v2.owner())
                                        + ", which the manifest does not replace by it; move the use manually.");
        }
        if (v2.parameters().size() != v3.parameters().size()) {
            return RecipeOutcome.unsafe("V3 " + v3.describe() + " takes different parameters than V2 "
                                        + v2.describe() + "; adapt the arguments manually.");
        }
        for (int i = 0; i < v2.parameters().size(); i++) {
            if (!Replacements.compatible(manifest, v2.parameters().get(i), v3.parameters().get(i))) {
                return RecipeOutcome.unsafe("Parameter " + (i + 1) + " of V3 " + v3.describe() + " is "
                                            + ApiIds.describeDescriptor(v3.parameters().get(i)) + " instead of "
                                            + ApiIds.describeDescriptor(v2.parameters().get(i))
                                            + "; adapt the argument manually.");
            }
        }
        boolean discarded = item.kind() == ItemKind.METHOD_INVOCATION && node.path().getParentPath() != null
                            && node.path().getParentPath().getLeaf() instanceof ExpressionStatementTree;
        if (!Replacements.compatible(manifest, v2.result(), v3.result()) && !discarded) {
            return RecipeOutcome.unsafe("V3 " + v3.describe() + (v3.field() ? " has type " : " returns ")
                                        + ApiIds.describeDescriptor(v3.result()) + " instead of "
                                        + ApiIds.describeDescriptor(v2.result()) + "; adapt the use of the result "
                                        + "manually.");
        }
        if (v2.name().equals(v3.name())) {
            return RecipeOutcome.preserve("V3 " + v3.describe() + " keeps the shape of V2 " + v2.describe()
                                          + "; only replaced types around it change.");
        }
        SourceLocation at = item.location();
        String text = context.text(at);
        int start = item.kind() == ItemKind.FIELD_ACCESS ? at.endOffset() - v2.name().length() : at.startOffset();
        int offset = start - at.startOffset();
        if (offset < 0 || !text.startsWith(v2.name(), offset)) {
            return RecipeOutcome.unsafe("The name " + v2.name() + " could not be located in the construct.");
        }
        return RecipeOutcome.transform("Renamed V2 " + v2.describe() + " to V3 " + v3.describe() + ".",
                                       List.of(new TextEdit(at.path(), start, start + v2.name().length(),
                                                            v3.name())));
    }
}
