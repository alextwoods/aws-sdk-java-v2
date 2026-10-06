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

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import software.amazon.awssdk.v2migration.TextEdit;
import software.amazon.awssdk.v2migration.manifest.Classification;
import software.amazon.awssdk.v2migration.manifest.EntryKind;
import software.amazon.awssdk.v2migration.manifest.ManifestEntry;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;
import software.amazon.awssdk.v2migration.recipe.JavaNode;

/**
 * The conversion of one method declaration that overrides a recipe-replaced V2 method (such as a V2
 * {@code ExecutionInterceptor} hook) to the V3 method its manifest entry names, computed purely from the declaration,
 * its attribution and the manifest, so every item of the declaration (the override, the types of its signature and the
 * accessor calls on its parameters) reaches the same decision.
 *
 * <p>The V3 replacement's parameters correspond positionally to the first V2 parameters; V2 parameters beyond them
 * (such as V2 {@code ExecutionAttributes}) are dropped, which is safe only when the body never reads them. A kept
 * parameter whose type changes (for example {@code Context.ModifyHttpRequest} to
 * {@code HookContext.ModifyBeforeSigning}) has its declared type rewritten here unless its V2 type is recipe-replaced
 * by exactly the V3 type, in which case its own reference is renamed. Every use of such a parameter must be an accessor call whose own manifest entry is
 * recipe-replaced by a member of the new type (or of the type that encloses it, as a V3 context family declares its
 * shared accessors) with compatible parameters, and whose result is either compatible or only returned, cast or
 * tested. V2 accessors are declared on shared context supertypes, so the receiver's new type, not the accessor's
 * declaring type, decides. A rewritten return type is accepted only for values the V3
 * return type is known to accept. When the replacement renames the method and the family requires it (lifecycle hooks
 * whose renamed V3 counterpart runs at another point or frequency), the body must also be free of side effects. Any
 * other construct makes the whole declaration unsafe, so it is left unchanged and reported.
 */
final class OverrideConversion {

    /** The outcome for one declaration. */
    sealed interface Plan {
    }

    /** The declaration does not override a recipe-replaced V2 method; the manifest decides. */
    enum None implements Plan {
        INSTANCE
    }

    /** The declaration cannot be converted safely. */
    record Unsafe(String reason) implements Plan {
    }

    /**
     * The conversion.
     *
     * @param summary   what the conversion does
     * @param edits     the edits within the declaration (empty when the declaration already matches)
     * @param claimed   the replaced ranges: items wholly inside them are rewritten by these edits
     * @param accessors the accessor calls on rewritten parameters, by the offset of the method name; the value is
     *                  the V3 name
     */
    record Convert(String summary, List<TextEdit> edits, List<int[]> claimed, Map<Integer, String> accessors)
        implements Plan {

        boolean claims(int start, int end) {
            return claimed.stream().anyMatch(r -> r[0] <= start && end <= r[1]);
        }
    }

    /** The V2 owners a lifecycle-hook body may call without side effects, beyond the JDK's value types. */
    private final Predicate<String> sideEffectFreeV2Owner;
    private final boolean pureBodyOnRename;

    /**
     * @param pureBodyOnRename      true if a renamed replacement requires a side-effect-free body
     * @param sideEffectFreeV2Owner the V2 owner binary names whose methods such a body may call
     */
    OverrideConversion(boolean pureBodyOnRename, Predicate<String> sideEffectFreeV2Owner) {
        this.pureBodyOnRename = pureBodyOnRename;
        this.sideEffectFreeV2Owner = sideEffectFreeV2Owner;
    }

    /** The manifest ID of the V2 method that the declaration at {@code methodPath} overrides, if any. */
    static Optional<String> overriddenId(JavaNode node, TreePath methodPath) {
        Symbols symbols = new Symbols(node);
        if (!(methodPath.getLeaf() instanceof MethodTree)
            || !(symbols.element(methodPath) instanceof ExecutableElement method)) {
            return Optional.empty();
        }
        return symbols.overriddenV2(method).flatMap(symbols::methodId);
    }

    /**
     * The declaration whose conversion would rewrite the item at {@code itemPath}: a method whose return type or
     * parameter contains the item, or the method whose parameter receives the accessor call at {@code itemPath}.
     */
    static Optional<TreePath> enclosingDeclaration(JavaNode node, TreePath itemPath) {
        Tree leaf = itemPath.getLeaf();
        if (leaf instanceof MethodInvocationTree invocation) {
            if (invocation.getMethodSelect() instanceof MemberSelectTree select
                && select.getExpression() instanceof IdentifierTree receiver) {
                TreePath receiverPath = new TreePath(new TreePath(itemPath, select), receiver);
                Element e = new Symbols(node).element(receiverPath);
                if (e != null && e.getKind() == ElementKind.PARAMETER
                    && e.getEnclosingElement() instanceof ExecutableElement method) {
                    TreePath declaration = node.trees().getPath(method);
                    if (declaration != null && declaration.getCompilationUnit() == node.unit()) {
                        return Optional.of(declaration);
                    }
                }
            }
            return Optional.empty();
        }
        Tree child = leaf;
        for (TreePath p = itemPath.getParentPath(); p != null; p = p.getParentPath()) {
            Tree t = p.getLeaf();
            if (t instanceof MethodTree method) {
                boolean signature = child == method.getReturnType() || method.getParameters().contains(child);
                return signature ? Optional.of(p) : Optional.empty();
            }
            if (t instanceof ClassTree || t instanceof LambdaExpressionTree || t instanceof NewClassTree
                || t.getKind() == Tree.Kind.BLOCK) {
                return Optional.empty();
            }
            child = t;
        }
        return Optional.empty();
    }

    /** A declaration that converts: its signature and the accessor calls of its body (see {@link Convert}). */
    record Checked(Signature signature, Map<Integer, String> accessors) implements Plan {
    }

    /**
     * Whether the declaration at {@code methodPath} converts under the signature and body rules, without generating
     * any text: {@link Checked}, {@link Unsafe} or {@link None}.
     */
    Plan check(JavaNode node, TreePath methodPath, MigrationManifest manifest) {
        Plan structure = signature(node, methodPath, manifest);
        if (!(structure instanceof Signature signature)) {
            return structure;
        }
        MethodTree tree = (MethodTree) methodPath.getLeaf();
        boolean renamed = !signature.v2().name().equals(signature.v3().name());
        Body body = new Body(node, new Symbols(node), manifest, signature.retyped(), signature.dropped(),
                             signature.v2(), signature.v3(), signature.overridden(), signature.returnRange() != null,
                             renamed && pureBodyOnRename);
        if (tree.getBody() != null) {
            body.scan(new TreePath(methodPath, tree.getBody()), null);
        }
        return body.problem != null ? new Unsafe(body.problem) : new Checked(signature, body.accessors);
    }

    /** The conversion of the declaration at {@code methodPath} in the file {@code path} with text {@code text}. */
    Plan plan(JavaNode node, TreePath methodPath, String path, String text, MigrationManifest manifest) {
        Plan verdict = check(node, methodPath, manifest);
        if (!(verdict instanceof Checked checked)) {
            return verdict;
        }
        Signature signature = checked.signature();
        MethodTree tree = (MethodTree) methodPath.getLeaf();
        ApiIds.Member v2 = signature.v2();
        ApiIds.Member v3 = signature.v3();
        List<TextEdit> edits = new ArrayList<>();
        List<String> changes = new ArrayList<>();
        for (Rewrite r : signature.rewrites()) {
            edits.add(new TextEdit(path, r.start(), r.end(), TypeNames.reference(node, manifest, r.type())));
            changes.add("parameter " + r.parameter().getSimpleName() + " becomes " + ApiIds.sourceName(r.type()));
        }
        for (Element dropped : signature.dropped()) {
            changes.add("parameter " + dropped.getSimpleName() + " is removed");
        }
        if (signature.droppedRange() != null) {
            edits.add(new TextEdit(path, signature.droppedRange()[0], signature.droppedRange()[1], ""));
        }
        if (signature.returnRange() != null) {
            edits.add(new TextEdit(path, signature.returnRange()[0], signature.returnRange()[1],
                                   TypeNames.descriptorReference(node, manifest, v3.result())));
            changes.add("the return type becomes " + ApiIds.describeDescriptor(v3.result()));
        }
        boolean renamed = !v2.name().equals(v3.name());
        if (renamed) {
            int from = tree.getReturnType() == null ? node.start(tree) : node.end(tree.getReturnType());
            int name = nameOffset(text, from, v2.name());
            if (name < 0) {
                return new Unsafe("The name of the declaration of " + v2.name() + " could not be located.");
            }
            edits.add(new TextEdit(path, name, name + v2.name().length(), v3.name()));
            changes.add("it is renamed to " + v3.name());
        }
        String summary = "Converted the override of V2 " + v2.describe() + " to V3 " + v3.describe()
                         + (changes.isEmpty() ? "" : ": " + String.join(", ", changes)) + ".";
        return new Convert(summary, edits, signature.claimed(), checked.accessors());
    }

    /** A kept parameter whose declared type {@code [start, end)} is rewritten to the V3 type {@code type}. */
    record Rewrite(Element parameter, int start, int end, String type) {
    }

    /**
     * The signature part of a conversion, which depends only on the declaration's attribution and the manifest (no
     * text is generated).
     *
     * @param retyped      every kept parameter whose V3 type differs from its V2 type, with the V3 type's binary
     *                     name: its uses must be convertible accessor calls
     * @param rewrites     the retyped parameters whose declared type this conversion rewrites (the others' V2 types
     *                     are recipe-replaced by exactly the V3 type, so their own references are renamed)
     * @param dropped      the removed parameters
     * @param droppedRange the removed parameter text, or null
     * @param returnRange  the rewritten return type, or null
     */
    record Signature(ExecutableElement overridden, ApiIds.Member v2, ApiIds.Member v3, Map<Element, String> retyped,
                     List<Rewrite> rewrites, Set<Element> dropped, int[] droppedRange, int[] returnRange)
        implements Plan {

        /** The source ranges the conversion replaces. */
        List<int[]> claimed() {
            List<int[]> claimed = new ArrayList<>();
            rewrites.forEach(r -> claimed.add(new int[] {r.start(), r.end()}));
            if (droppedRange != null) {
                claimed.add(droppedRange);
            }
            if (returnRange != null) {
                claimed.add(returnRange);
            }
            return claimed;
        }
    }

    /** The {@link Signature} of the declaration at {@code methodPath}, or {@link None} or {@link Unsafe}. */
    static Plan signature(JavaNode node, TreePath methodPath, MigrationManifest manifest) {
        Symbols symbols = new Symbols(node);
        if (!(methodPath.getLeaf() instanceof MethodTree tree)
            || !(symbols.element(methodPath) instanceof ExecutableElement method)) {
            return None.INSTANCE;
        }
        Optional<ExecutableElement> overridden = symbols.overriddenV2(method);
        Optional<String> id = overridden.flatMap(symbols::methodId);
        Optional<ManifestEntry> entry = id.flatMap(i -> Replacements.recipe(manifest, i));
        if (entry.isEmpty()) {
            return None.INSTANCE;
        }
        ApiIds.Member v2 = ApiIds.member(id.get()).orElse(null);
        ApiIds.Member v3 = ApiIds.member(entry.get().replacement()).filter(m -> !m.field()).orElse(null);
        if (v2 == null || v3 == null || v3.name().equals("<init>")) {
            return new Unsafe("The manifest replaces " + id.get() + " by " + entry.get().replacement()
                              + ", which is not a method, so the override cannot be converted.");
        }
        if (!Replacements.sameOwner(manifest, v2.owner(), v3.owner())) {
            return new Unsafe("The V3 replacement " + v3.describe() + " is declared by "
                              + ApiIds.sourceName(v3.owner()) + ", but the manifest does not replace the V2 type "
                              + ApiIds.sourceName(v2.owner()) + " by it, so the class would not override it.");
        }
        List<? extends VariableTree> parameters = tree.getParameters();
        int n = parameters.size();
        int k = v3.parameters().size();
        if (k > n) {
            return new Unsafe("The V3 replacement " + v3.describe() + " takes more parameters than V2 "
                              + v2.describe() + "; the additional arguments cannot be derived automatically.");
        }
        Map<Element, String> retyped = new HashMap<>();
        List<Rewrite> rewrites = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            String d2 = v2.parameters().get(i);
            String d3 = v3.parameters().get(i);
            if (d2.equals(d3)) {
                continue;
            }
            Optional<String> type = ApiIds.className(d3);
            Element parameter = symbols.element(new TreePath(methodPath, parameters.get(i)));
            if (type.isEmpty() || ApiIds.className(d2).isEmpty() || parameter == null) {
                return new Unsafe("Parameter " + (i + 1) + " changes from " + ApiIds.describeDescriptor(d2) + " to "
                                  + ApiIds.describeDescriptor(d3) + ", which is not a class-type conversion.");
            }
            retyped.put(parameter, type.get());
            if (!Replacements.compatible(manifest, d2, d3)) {
                Tree typeTree = parameters.get(i).getType();
                rewrites.add(new Rewrite(parameter, node.start(typeTree), node.end(typeTree), type.get()));
            }
        }
        Set<Element> dropped = new LinkedHashSet<>();
        for (int i = k; i < n; i++) {
            Element parameter = symbols.element(new TreePath(methodPath, parameters.get(i)));
            if (parameter == null) {
                return new Unsafe("Parameter " + parameters.get(i).getName() + " cannot be attributed.");
            }
            dropped.add(parameter);
        }
        int[] droppedRange = null;
        if (n > k) {
            droppedRange = new int[] {k > 0 ? node.end(parameters.get(k - 1)) : node.start(parameters.get(0)),
                                      node.end(parameters.get(n - 1))};
        }
        int[] returnRange = null;
        if (!Replacements.compatible(manifest, v2.result(), v3.result())) {
            if (v2.result().equals("V") || v3.result().equals("V") || tree.getReturnType() == null) {
                return new Unsafe("V2 " + v2.describe() + " returns " + ApiIds.describeDescriptor(v2.result())
                                  + " but V3 " + v3.describe() + " returns "
                                  + ApiIds.describeDescriptor(v3.result())
                                  + "; the return statements cannot be converted automatically.");
            }
            returnRange = new int[] {node.start(tree.getReturnType()), node.end(tree.getReturnType())};
        }
        return new Signature(overridden.get(), v2, v3, retyped, rewrites, dropped, droppedRange, returnRange);
    }

    /** The offset of {@code name} after {@code from}, skipping whitespace and comments, or -1. */
    static int nameOffset(String text, int from, String name) {
        int i = from;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (text.startsWith("//", i)) {
                int eol = text.indexOf('\n', i);
                i = eol < 0 ? text.length() : eol + 1;
            } else if (text.startsWith("/*", i)) {
                int close = text.indexOf("*/", i + 2);
                i = close < 0 ? text.length() : close + 2;
            } else {
                break;
            }
        }
        int end = i + name.length();
        boolean matches = text.startsWith(name, i)
                          && (end == text.length() || !Character.isJavaIdentifierPart(text.charAt(end)));
        return matches ? i : -1;
    }

    /** Checks a body against the conversion rules and collects its accessor calls. */
    private final class Body extends TreePathScanner<Void, Void> {
        private final JavaNode node;
        private final Symbols symbols;
        private final MigrationManifest manifest;
        private final Map<Element, String> rewritten;
        private final Set<Element> dropped;
        private final ApiIds.Member v2;
        private final ApiIds.Member v3;
        private final ExecutableElement overridden;
        private final boolean returnRewritten;
        private final boolean requirePure;
        private final Map<Integer, String> accessors = new HashMap<>();
        private int nested;
        private String problem;

        Body(JavaNode node, Symbols symbols, MigrationManifest manifest, Map<Element, String> rewritten,
             Set<Element> dropped, ApiIds.Member v2, ApiIds.Member v3, ExecutableElement overridden,
             boolean returnRewritten, boolean requirePure) {
            this.node = node;
            this.symbols = symbols;
            this.manifest = manifest;
            this.rewritten = rewritten;
            this.dropped = dropped;
            this.v2 = v2;
            this.v3 = v3;
            this.overridden = overridden;
            this.returnRewritten = returnRewritten;
            this.requirePure = requirePure;
        }

        @Override
        public Void scan(Tree tree, Void unused) {
            return problem == null ? super.scan(tree, unused) : null;
        }

        private void fail(Tree at, String message) {
            if (problem == null) {
                problem = message + " (line " + node.unit().getLineMap().getLineNumber(node.start(at)) + ")";
            }
        }

        @Override
        public Void visitIdentifier(IdentifierTree identifier, Void unused) {
            Element e = symbols.element(getCurrentPath());
            if (e != null && dropped.contains(e)) {
                fail(identifier, "The body reads the V2 parameter " + identifier.getName() + ", which V3 "
                                 + v3.describe() + " does not receive; move the state it carries (for example "
                                 + "execution attributes) to the V3 typed context manually");
            } else if (e != null && rewritten.containsKey(e)) {
                accessor(identifier, rewritten.get(e));
            }
            return super.visitIdentifier(identifier, unused);
        }

        /** A use of a parameter whose type is rewritten to {@code newType}. */
        private void accessor(IdentifierTree identifier, String newType) {
            TreePath selectPath = getCurrentPath().getParentPath();
            TreePath invocationPath = selectPath == null ? null : selectPath.getParentPath();
            if (selectPath == null || invocationPath == null
                || !(selectPath.getLeaf() instanceof MemberSelectTree select)
                || !(invocationPath.getLeaf() instanceof MethodInvocationTree invocation)
                || invocation.getMethodSelect() != select) {
                fail(identifier, "The body passes or stores the V2 context " + identifier.getName()
                                 + "; only accessor calls on it are converted automatically");
                return;
            }
            if (!(symbols.element(selectPath) instanceof ExecutableElement method)
                || !(method.getEnclosingElement() instanceof TypeElement owner)) {
                fail(invocation, "The accessor call " + select.getIdentifier() + " cannot be attributed");
                return;
            }
            if (!symbols.isV2(owner)) {
                // An Object method, which every V3 context type also has.
                return;
            }
            Optional<String> id = symbols.methodId(method);
            Optional<ManifestEntry> entry = id.flatMap(i -> Replacements.recipe(manifest, i));
            ApiIds.Member old = id.flatMap(ApiIds::member).orElse(null);
            ApiIds.Member replacement = entry.flatMap(e -> ApiIds.member(e.replacement()))
                                             .filter(m -> !m.field()).orElse(null);
            if (old == null || replacement == null) {
                fail(invocation, "The body calls " + (old == null ? select.getIdentifier() : old.describe())
                                 + " on the V2 context, which the Parity Manifest does not replace by a V3 accessor"
                                 + classification(id));
                return;
            }
            boolean ownedByType = replacement.owner().equals(newType)
                                  || newType.startsWith(replacement.owner() + "$");
            if (!ownedByType) {
                fail(invocation, "V2 " + old.describe() + " is replaced by " + replacement.describe()
                                 + ", which is not an accessor of the V3 context " + ApiIds.sourceName(newType));
                return;
            }
            if (old.parameters().size() != replacement.parameters().size()) {
                fail(invocation, "V3 " + replacement.describe() + " takes different parameters than V2 "
                                 + old.describe());
                return;
            }
            for (int i = 0; i < old.parameters().size(); i++) {
                if (!Replacements.compatible(manifest, old.parameters().get(i), replacement.parameters().get(i))) {
                    fail(invocation, "V3 " + replacement.describe() + " takes different parameters than V2 "
                                     + old.describe());
                    return;
                }
            }
            if (!Replacements.compatible(manifest, old.result(), replacement.result())
                && !resultTolerated(invocationPath, method.getReturnType(), replacement.result())) {
                fail(invocation, "The body uses the result of V2 " + old.describe() + ", which V3 "
                                 + replacement.describe() + " returns as "
                                 + ApiIds.describeDescriptor(replacement.result()) + " instead of "
                                 + ApiIds.describeDescriptor(old.result()));
                return;
            }
            int name = node.end(select) - select.getIdentifier().length();
            accessors.put(name, replacement.name());
        }

        /** True if an accessor result whose type changes is only returned as the new return type, cast or tested. */
        private boolean resultTolerated(TreePath invocationPath, TypeMirror oldResult, String newResult) {
            TreePath parent = invocationPath.getParentPath();
            while (parent != null && parent.getLeaf() instanceof ParenthesizedTree) {
                parent = parent.getParentPath();
            }
            if (parent == null) {
                return false;
            }
            Tree p = parent.getLeaf();
            if (p instanceof ReturnTree) {
                return nested == 0 && newResult.equals(v3.result());
            }
            if (p instanceof TypeCastTree cast) {
                return preservedStrictSubtype(new TreePath(parent, cast.getType()), oldResult);
            }
            if (p instanceof InstanceOfTree test && test.getType() != null) {
                return preservedStrictSubtype(new TreePath(parent, test.getType()), oldResult);
            }
            return false;
        }

        /** True if the type at {@code path} is a V2 type that the manifest preserves and that strictly extends
         * {@code supertype}, so V3 keeps it as a subtype of the modeled-value type that replaces {@code supertype}. */
        private boolean preservedStrictSubtype(TreePath path, TypeMirror supertype) {
            TypeMirror type = node.trees().getTypeMirror(path);
            if (type == null || type.getKind() != TypeKind.DECLARED
                || !(((DeclaredType) type).asElement() instanceof TypeElement element) || !symbols.isV2(element)) {
                return false;
            }
            TypeMirror erased = node.types().erasure(type);
            TypeMirror erasedSuper = node.types().erasure(supertype);
            return !node.types().isSameType(erased, erasedSuper) && node.types().isSubtype(erased, erasedSuper)
                   && manifest.entry(EntryKind.typeId(symbols.binary(element)))
                              .map(e -> e.classification() == Classification.PRESERVED).orElse(false);
        }

        private String classification(Optional<String> id) {
            return id.flatMap(manifest::entry)
                     .map(e -> " (it is " + e.classification()
                               + (e.migrationKind() == null ? "" : " with a " + e.migrationKind() + " migration")
                               + ")")
                     .orElse(" (it has no manifest entry)");
        }

        @Override
        public Void visitReturn(ReturnTree ret, Void unused) {
            if (returnRewritten && nested == 0 && ret.getExpression() != null) {
                ExpressionTree e = ret.getExpression();
                TreePath p = new TreePath(getCurrentPath(), e);
                while (e instanceof ParenthesizedTree parenthesized) {
                    e = parenthesized.getExpression();
                    p = new TreePath(p, e);
                }
                boolean accessorCall = e instanceof MethodInvocationTree mi
                                       && mi.getMethodSelect() instanceof MemberSelectTree ms
                                       && ms.getExpression() instanceof IdentifierTree id
                                       && rewritten.containsKey(symbols.element(
                                           new TreePath(new TreePath(p, ms), id)));
                boolean nullLiteral = e.getKind() == Tree.Kind.NULL_LITERAL;
                if (!accessorCall && !nullLiteral && !preservedStrictSubtype(p, overridden.getReturnType())) {
                    fail(e, "The body returns a value that V3 " + v3.describe() + " is not known to accept as "
                            + ApiIds.describeDescriptor(v3.result()) + "; only a context accessor result, null or a "
                            + "preserved subtype of V2 " + ApiIds.describeDescriptor(v2.result())
                            + " is converted automatically");
                }
            }
            return super.visitReturn(ret, unused);
        }

        @Override
        public Void visitLambdaExpression(LambdaExpressionTree lambda, Void unused) {
            nested++;
            try {
                return super.visitLambdaExpression(lambda, unused);
            } finally {
                nested--;
            }
        }

        @Override
        public Void visitClass(ClassTree type, Void unused) {
            if (requirePure) {
                fail(type, impure("declares a class"));
                return null;
            }
            nested++;
            try {
                return super.visitClass(type, unused);
            } finally {
                nested--;
            }
        }

        // Side effects, checked only when a renamed hook runs at another point or frequency in V3.

        private String impure(String what) {
            return "V2 " + v2.name() + " becomes V3 " + v3.name() + ", which runs at a different lifecycle point or "
                   + "frequency (see the manifest differences), and the body " + what + ", which may have side "
                   + "effects; only a side-effect-free body is converted automatically";
        }

        @Override
        public Void visitAssignment(AssignmentTree assignment, Void unused) {
            if (requirePure) {
                local(assignment, assignment.getVariable());
            }
            return super.visitAssignment(assignment, unused);
        }

        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree assignment, Void unused) {
            if (requirePure) {
                local(assignment, assignment.getVariable());
            }
            return super.visitCompoundAssignment(assignment, unused);
        }

        @Override
        public Void visitUnary(UnaryTree unary, Void unused) {
            if (requirePure) {
                switch (unary.getKind()) {
                    case PREFIX_INCREMENT:
                    case PREFIX_DECREMENT:
                    case POSTFIX_INCREMENT:
                    case POSTFIX_DECREMENT:
                        local(unary, unary.getExpression());
                        break;
                    default:
                        break;
                }
            }
            return super.visitUnary(unary, unused);
        }

        private void local(Tree at, ExpressionTree variable) {
            Element e = symbols.element(new TreePath(getCurrentPath(), variable));
            boolean local = e != null && (e.getKind() == ElementKind.LOCAL_VARIABLE
                                          || e.getKind() == ElementKind.PARAMETER
                                          || e.getKind() == ElementKind.EXCEPTION_PARAMETER
                                          || e.getKind() == ElementKind.RESOURCE_VARIABLE
                                          || e.getKind() == ElementKind.BINDING_VARIABLE);
            if (!local) {
                fail(at, impure("writes " + variable));
            }
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree invocation, Void unused) {
            if (requirePure) {
                Element e = symbols.element(new TreePath(getCurrentPath(), invocation.getMethodSelect()));
                if (!sideEffectFree(e)) {
                    fail(invocation, impure("calls " + invocation.getMethodSelect()));
                }
            }
            return super.visitMethodInvocation(invocation, unused);
        }

        @Override
        public Void visitNewClass(NewClassTree creation, Void unused) {
            if (requirePure
                && (creation.getClassBody() != null || !sideEffectFree(symbols.element(getCurrentPath())))) {
                fail(creation, impure("creates " + creation.getIdentifier()));
            }
            return super.visitNewClass(creation, unused);
        }

        @Override
        public Void visitMemberReference(MemberReferenceTree reference, Void unused) {
            if (requirePure && !sideEffectFree(symbols.element(getCurrentPath()))) {
                fail(reference, impure("references " + reference));
            }
            return super.visitMemberReference(reference, unused);
        }

        @Override
        public Void visitSynchronized(SynchronizedTree block, Void unused) {
            if (requirePure) {
                fail(block, impure("synchronizes"));
            }
            return super.visitSynchronized(block, unused);
        }

        private boolean sideEffectFree(Element e) {
            if (!(e instanceof ExecutableElement method) || !(method.getEnclosingElement() instanceof TypeElement o)) {
                return false;
            }
            String owner = symbols.binary(o);
            if (symbols.isV2(o)) {
                return sideEffectFreeV2Owner.test(owner);
            }
            return JdkValues.sideEffectFree(owner, method.getSimpleName().toString());
        }
    }

    /** The JDK methods a side-effect-free body may call: value types and read-only collection queries. */
    private static final class JdkValues {
        private static final Set<String> VALUE_TYPES = Set.of(
            "java.lang.Object", "java.lang.String", "java.lang.StringBuilder", "java.lang.Math", "java.lang.Integer",
            "java.lang.Long", "java.lang.Boolean", "java.lang.Character", "java.util.Objects", "java.util.Optional");
        private static final Set<String> COLLECTIONS = Set.of(
            "java.util.Map", "java.util.List", "java.util.Collection", "java.util.Set");
        private static final Set<String> QUERIES = Set.of(
            "get", "getOrDefault", "containsKey", "containsValue", "contains", "isEmpty", "size");

        static boolean sideEffectFree(String owner, String method) {
            return VALUE_TYPES.contains(owner) || COLLECTIONS.contains(owner) && QUERIES.contains(method);
        }
    }
}
