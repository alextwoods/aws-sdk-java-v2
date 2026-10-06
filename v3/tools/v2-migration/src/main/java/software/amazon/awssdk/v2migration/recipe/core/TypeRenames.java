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

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.util.TreePathScanner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import software.amazon.awssdk.v2migration.TextEdit;
import software.amazon.awssdk.v2migration.recipe.JavaNode;

/**
 * The rewrite of one renamed type throughout one compilation unit. Every use of the type in the unit is planned
 * together, so a unit is either rewritten consistently or not at all: if any use cannot be rewritten safely, every use
 * is reported. A use is rewritable when it is
 * <ul>
 *   <li>the single-type import of the type (the qualified name is replaced);</li>
 *   <li>a simple name resolved through that import (renamed when the simple name changes, otherwise unchanged);</li>
 *   <li>the fully qualified name (replaced), or a name qualified by an unchanged enclosing type (last segment
 *       renamed).</li>
 * </ul>
 * A use through an on-demand import or inheritance, an import of a member or nested type of the type, a new simple
 * name that another type, type parameter or import of the unit already uses, or a name interrupted by comments is not
 * rewritable.
 */
final class TypeRenames {

    private static final int MAX_REPORTED_PROBLEMS = 3;

    /**
     * The plan: either why the unit cannot be rewritten, or the edit (empty when the use stays unchanged) of each use,
     * keyed by the use's source range.
     */
    record Plan(String unsafe, Map<Long, Optional<TextEdit>> uses) {

        /** The planned edit of the use spanning {@code [start, end)}; empty if the range is not a planned use. */
        Optional<Optional<TextEdit>> at(int start, int end) {
            Long key = key(start, end);
            return uses.containsKey(key) ? Optional.of(uses.get(key)) : Optional.empty();
        }
    }

    private TypeRenames() {
    }

    /** Plans renaming {@code oldBinary} to {@code newBinary} in {@code node}'s unit, whose text is {@code text}. */
    static Plan plan(JavaNode node, String path, String text, String oldBinary, String newBinary) {
        TypeElement old = node.elements().getTypeElement(ManifestIds.canonical(oldBinary));
        if (old == null || !node.elements().getBinaryName(old).contentEquals(oldBinary)) {
            return new Plan("The type " + oldBinary + " cannot be resolved on the project's class path", Map.of());
        }
        return new Planner(node, path, text, old, oldBinary, newBinary).plan();
    }

    private static Long key(int start, int end) {
        return ((long) start << 32) | (end & 0xFFFFFFFFL);
    }

    private static final class Planner {
        private final JavaNode node;
        private final CompilationUnitTree unit;
        private final String path;
        private final String text;
        private final TypeElement old;
        private final String oldCanonical;
        private final String newCanonical;
        private final String oldSimple;
        private final String newSimple;
        private final boolean renamesSimpleName;
        private final Map<Long, Optional<TextEdit>> uses = new HashMap<>();
        private final List<String> problems = new ArrayList<>();
        private boolean imported;

        Planner(JavaNode node, String path, String text, TypeElement old, String oldBinary, String newBinary) {
            this.node = node;
            this.unit = node.unit();
            this.path = path;
            this.text = text;
            this.old = old;
            this.oldCanonical = ManifestIds.canonical(oldBinary);
            this.newCanonical = ManifestIds.canonical(newBinary);
            this.oldSimple = ManifestIds.simpleName(oldBinary);
            this.newSimple = ManifestIds.simpleName(newBinary);
            this.renamesSimpleName = !oldSimple.equals(newSimple);
        }

        Plan plan() {
            imports();
            new Scanner().scan(unit, null);
            if (!problems.isEmpty()) {
                List<String> shown = problems.subList(0, Math.min(MAX_REPORTED_PROBLEMS, problems.size()));
                return new Plan("Renaming " + oldCanonical + " to " + newCanonical + " requires rewriting every use in "
                                + "this file, and " + problems.size() + " cannot be rewritten safely: "
                                + String.join("; ", shown) + (problems.size() > shown.size() ? "; ..." : ""),
                                Map.of());
            }
            return new Plan(null, uses);
        }

        private void imports() {
            for (ImportTree i : unit.getImports()) {
                Tree qualid = i.getQualifiedIdentifier();
                String name = qualid.toString();
                if (!i.isStatic() && name.equals(oldCanonical)) {
                    imported = true;
                    int start = node.start(qualid);
                    int end = node.end(qualid);
                    if (!plainName(start, end, oldCanonical)) {
                        problem(start, "the import of " + oldCanonical + " is not written as a plain qualified name");
                    } else {
                        uses.put(key(node.start(i), node.end(i)), Optional.of(new TextEdit(path, start, end,
                                                                                           newCanonical)));
                    }
                } else if (name.startsWith(oldCanonical + ".")) {
                    problem(node.start(i), "the import of " + name + " names a member or nested type of "
                                           + oldCanonical);
                } else if (!i.isStatic() && renamesSimpleName && name.endsWith("." + newSimple)
                           && !name.equals(newCanonical)) {
                    problem(node.start(i), "the import of " + name + " already uses the simple name " + newSimple);
                }
            }
        }

        /** Plans one use of the old type. */
        private void use(Tree tree) {
            int start = node.start(tree);
            int end = node.end(tree);
            if (start < 0 || end < start) {
                problems.add("a use of " + oldCanonical + " has no source position");
                return;
            }
            String source = text.substring(start, end);
            if (tree instanceof IdentifierTree) {
                if (!imported) {
                    problem(start, source + " refers to " + oldCanonical + " by its simple name without a "
                                   + "single-type import (for example through an on-demand import or inheritance)");
                } else {
                    uses.put(key(start, end), renamesSimpleName ? Optional.of(new TextEdit(path, start, end, newSimple))
                                                                : Optional.empty());
                }
            } else if (plainName(start, end, oldCanonical)) {
                uses.put(key(start, end), Optional.of(new TextEdit(path, start, end, newCanonical)));
            } else if (qualifier(oldCanonical).equals(qualifier(newCanonical)) && source.endsWith(oldSimple)
                       && source.indexOf('/') < 0) {
                // Qualified by an enclosing type that keeps its name: only the last segment changes.
                uses.put(key(start, end), Optional.of(new TextEdit(path, end - oldSimple.length(), end, newSimple)));
            } else {
                problem(start, "the qualified name " + source.strip().replaceAll("\\s+", " ") + " cannot be rewritten");
            }
        }

        private boolean plainName(int start, int end, String expected) {
            String source = text.substring(start, end);
            return source.indexOf('/') < 0 && source.replaceAll("\\s+", "").equals(expected);
        }

        private void problem(int offset, String message) {
            problems.add("line " + unit.getLineMap().getLineNumber(offset) + ": " + message);
        }

        private void clash(Tree tree, String what) {
            problem(node.start(tree), what + " already uses the new simple name " + newSimple);
        }

        /** Finds every use of the old type outside imports, and every other meaning of the new simple name. */
        private final class Scanner extends TreePathScanner<Void, Void> {

            @Override
            public Void visitImport(ImportTree tree, Void unused) {
                return null;
            }

            @Override
            public Void visitClass(ClassTree tree, Void unused) {
                if (renamesSimpleName && tree.getSimpleName().contentEquals(newSimple)) {
                    clash(tree, "the type declared here");
                }
                return super.visitClass(tree, unused);
            }

            @Override
            public Void visitTypeParameter(TypeParameterTree tree, Void unused) {
                if (renamesSimpleName && tree.getName().contentEquals(newSimple)) {
                    clash(tree, "the type parameter declared here");
                }
                return super.visitTypeParameter(tree, unused);
            }

            @Override
            public Void visitIdentifier(IdentifierTree tree, Void unused) {
                Element e = element();
                if (old.equals(e)) {
                    use(tree);
                } else if (renamesSimpleName && tree.getName().contentEquals(newSimple) && e != null
                           && isTypeLike(e)) {
                    clash(tree, "the reference to " + e);
                }
                return super.visitIdentifier(tree, unused);
            }

            @Override
            public Void visitMemberSelect(MemberSelectTree tree, Void unused) {
                if (old.equals(element())) {
                    use(tree);
                    // The qualifier is a package or an enclosing type, planned on its own.
                }
                return super.visitMemberSelect(tree, unused);
            }

            private Element element() {
                try {
                    return node.trees().getElement(getCurrentPath());
                } catch (RuntimeException e) {
                    return null;
                }
            }
        }

        private static boolean isTypeLike(Element e) {
            ElementKind kind = e.getKind();
            return kind.isClass() || kind.isInterface() || kind == ElementKind.TYPE_PARAMETER;
        }

        private static String qualifier(String canonical) {
            return canonical.substring(0, canonical.lastIndexOf('.') + 1);
        }
    }
}
