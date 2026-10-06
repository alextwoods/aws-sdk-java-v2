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

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import javax.lang.model.element.TypeElement;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;
import software.amazon.awssdk.v2migration.recipe.JavaNode;

/**
 * How a converted construct names a V3 type, given the unit's single-type imports as they will read after migration.
 * An import of a V2 type is removed when conversions rewrite away every use of it ({@link ConvertedImportRecipe});
 * otherwise an import of a type that a {@link Family} owns is rewritten to import its recipe replacement, if it has
 * one; every other import is unchanged. A type is written unqualified (or qualified by an imported enclosing type)
 * when such an import, or the unit's own package, resolves it; otherwise its fully qualified source name is written,
 * which compiles whatever happens to the imports.
 */
final class TypeNames {

    private TypeNames() {
    }

    /** The text naming the type {@code binaryName} in {@code node}'s unit. */
    static String reference(JavaNode node, MigrationManifest manifest, String binaryName) {
        CompilationUnitTree unit = node.unit();
        String qualified = ApiIds.sourceName(binaryName);
        String topLevel = binaryName.contains("$") ? binaryName.substring(0, binaryName.indexOf('$')) : binaryName;
        String simpleTop = simpleName(topLevel);
        if (declares(unit, simpleTop)) {
            return qualified;
        }
        for (ImportTree i : unit.getImports()) {
            String imported = migratedImport(node, i, manifest);
            if (imported != null && (qualified.equals(imported) || qualified.startsWith(imported + "."))) {
                return simpleName(imported) + qualified.substring(imported.length());
            }
        }
        String pkg = unit.getPackageName() == null ? "" : unit.getPackageName().toString();
        int dot = topLevel.lastIndexOf('.');
        String typePackage = dot < 0 ? "" : topLevel.substring(0, dot);
        if (typePackage.equals(pkg) && importingSimpleName(node, manifest, simpleTop, null) == null) {
            return qualified.substring(typePackage.isEmpty() ? 0 : typePackage.length() + 1);
        }
        return qualified;
    }

    /** The Java source text of a field descriptor's type ({@code void} for {@code V}). */
    static String descriptorReference(JavaNode node, MigrationManifest manifest, String descriptor) {
        if (descriptor.startsWith("[")) {
            return descriptorReference(node, manifest, descriptor.substring(1)) + "[]";
        }
        return ApiIds.className(descriptor).map(c -> reference(node, manifest, c))
                     .orElseGet(() -> ApiIds.primitive(descriptor.charAt(0)));
    }

    /**
     * Why rewriting the import of {@code oldName} to {@code newName} (source names) would make the new simple name
     * ambiguous or shadowed in {@code node}'s unit, or {@code null} if it would not.
     */
    static String importClash(JavaNode node, MigrationManifest manifest, String oldName, String newName) {
        String simple = simpleName(newName);
        if (simple.equals(simpleName(oldName))) {
            return null;
        }
        if (declares(node.unit(), simple)) {
            return "the unit declares a type named " + simple;
        }
        String other = importingSimpleName(node, manifest, simple, newName);
        return other == null ? null : "the unit also imports " + other;
    }

    /** The import (after migration) of another type with {@code simpleName}, other than {@code except}, or null. */
    private static String importingSimpleName(JavaNode node, MigrationManifest manifest, String simpleName,
                                              String except) {
        for (ImportTree i : node.unit().getImports()) {
            String imported = migratedImport(node, i, manifest);
            if (imported != null && simpleName(imported).equals(simpleName) && !imported.equals(except)) {
                return imported;
            }
        }
        return null;
    }

    /**
     * The source name a non-static single-type import will import after migration, or null for other imports and
     * imports that migration removes.
     */
    private static String migratedImport(JavaNode node, ImportTree i, MigrationManifest manifest) {
        Tree qualid = i.getQualifiedIdentifier();
        String imported = qualid.toString();
        if (i.isStatic() || imported.endsWith(".*")) {
            return null;
        }
        TreePath path = new TreePath(new TreePath(new TreePath(node.unit()), i), qualid);
        if (!(new Symbols(node).element(path) instanceof TypeElement type)) {
            return imported;
        }
        Symbols symbols = new Symbols(node);
        if (!symbols.isV2(type)) {
            return imported;
        }
        if (ImportUses.allRewritten(node, type, manifest)) {
            // Removed by ConvertedImportRecipe.
            return null;
        }
        String binary = symbols.binary(type);
        return Family.anyOwns(binary) ? Replacements.type(manifest, binary).map(ApiIds::sourceName).orElse(imported)
                                      : imported;
    }

    /** True if the unit declares a top-level type named {@code simpleName}, which would shadow an imported one. */
    private static boolean declares(CompilationUnitTree unit, String simpleName) {
        for (Tree t : unit.getTypeDecls()) {
            if (t instanceof ClassTree c && c.getSimpleName().contentEquals(simpleName)) {
                return true;
            }
        }
        return false;
    }

    private static String simpleName(String name) {
        return name.substring(name.lastIndexOf('.') + 1);
    }
}
