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

package software.amazon.awssdk.v2migration.internal;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import software.amazon.awssdk.v2migration.ItemKind;
import software.amazon.awssdk.v2migration.MigrationItem;
import software.amazon.awssdk.v2migration.manifest.Classification;
import software.amazon.awssdk.v2migration.manifest.EntryKind;
import software.amazon.awssdk.v2migration.manifest.ManifestEntry;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;
import software.amazon.awssdk.v2migration.recipe.JavaNode;

/**
 * The Java side of the migration inventory. Parses and attributes the project's sources with the JDK compiler's tree
 * API against the project's own class path (no class files are written and no annotation processors run), then
 * inventories every use of a V2 public type or member in the inventoried files: imports, type references, method and
 * constructor invocations, field accesses, overrides of V2 methods (such as interceptor hooks) and V2 configuration
 * keys. Members join the Parity Manifest by their exact JVM descriptor, computed from the erased declaration the
 * compiler resolved, which is how the manifest's {@code api:} member IDs are formed.
 *
 * <p>Every compiler error in an inventoried file becomes an {@link ItemKind#UNATTRIBUTED_SOURCE} item, because a
 * construct the compiler cannot attribute may hide a V2 use. Attribution is kept alive until {@link #close()}, so
 * recipes can inspect the {@link JavaNode} of each item.
 */
public final class JavaInventory implements AutoCloseable {

    /** The V2 API package prefix. */
    static final String V2_PACKAGE = "software.amazon.awssdk.";

    private static final Set<String> CONFIGURATION_METHODS = Set.of("getenv", "getProperty", "setProperty",
                                                                    "clearProperty");

    /** One inventoried item and, for attributed items, its syntax. */
    public record Found(MigrationItem item, JavaNode node) {
    }

    private final StandardJavaFileManager fileManager;
    private final List<Found> found;

    private JavaInventory(StandardJavaFileManager fileManager, List<Found> found) {
        this.fileManager = fileManager;
        this.found = found;
    }

    /**
     * Attributes {@code inventoried} and {@code context} sources together against {@code classpath} and inventories
     * {@code inventoried}. Context sources (for example generated sources) resolve symbols but are not inventoried.
     */
    public static JavaInventory analyze(List<SourceText> inventoried, List<SourceText> context, List<Path> classpath,
                                        MigrationManifest manifest) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("The V2 migration tooling must run on a JDK: the Java compiler "
                                            + "(module jdk.compiler) is not available in " + System.getProperty("java.home"));
        }
        // The compiler wraps client file objects, so sources are matched by URI.
        Map<URI, SourceText> sources = new HashMap<>();
        List<JavaFileObject> units = new ArrayList<>();
        for (SourceText text : inventoried) {
            InMemorySource source = new InMemorySource(text);
            sources.put(source.toUri(), text);
            units.add(source);
        }
        for (SourceText text : context) {
            units.add(new InMemorySource(text));
        }
        List<Diagnostic<? extends JavaFileObject>> errors = new ArrayList<>();
        DiagnosticListener<JavaFileObject> listener = d -> {
            if (d.getKind() == Diagnostic.Kind.ERROR) {
                errors.add(d);
            }
        };
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(listener, Locale.ROOT, null);
        boolean success = false;
        try {
            fileManager.setLocationFromPaths(StandardLocation.CLASS_PATH, classpath);
            fileManager.setLocationFromPaths(StandardLocation.SOURCE_PATH, List.of());
            fileManager.setLocationFromPaths(StandardLocation.ANNOTATION_PROCESSOR_PATH, List.of());
            List<String> options = List.of("-proc:none", "-implicit:none", "-Xlint:none", "-nowarn",
                                           "-Xmaxerrs", String.valueOf(Integer.MAX_VALUE),
                                           // Keep attributing every class after the first error.
                                           "-XDshould-stop.ifError=FLOW");
            JavacTask task = (JavacTask) compiler.getTask(null, fileManager, listener, options, null, units);
            Iterable<? extends CompilationUnitTree> trees = task.parse();
            task.analyze();
            Trees treeUtils = Trees.instance(task);
            List<Found> found = new ArrayList<>();
            for (CompilationUnitTree unit : trees) {
                SourceText text = sources.get(unit.getSourceFile().toUri());
                if (text != null) {
                    new Scanner(unit, text, treeUtils, task.getElements(), task.getTypes(), manifest, found)
                        .scan(unit, null);
                }
            }
            for (Diagnostic<? extends JavaFileObject> error : errors) {
                SourceText text = error.getSource() == null ? null : sources.get(error.getSource().toUri());
                if (text != null) {
                    found.add(new Found(unattributed(text, error), null));
                }
            }
            success = true;
            return new JavaInventory(fileManager, found);
        } finally {
            if (!success) {
                fileManager.close();
            }
        }
    }

    /** The inventoried items, in discovery order. */
    public List<Found> found() {
        return found;
    }

    @Override
    public void close() throws IOException {
        fileManager.close();
    }

    private static MigrationItem unattributed(SourceText text, Diagnostic<? extends JavaFileObject> error) {
        long position = error.getStartPosition() != Diagnostic.NOPOS ? error.getStartPosition() : error.getPosition();
        int start = position == Diagnostic.NOPOS ? 0 : (int) position;
        int end = error.getEndPosition() == Diagnostic.NOPOS ? start : Math.max(start, (int) error.getEndPosition());
        String message = SourceText.abbreviate(error.getMessage(Locale.ROOT));
        String construct = end > start ? text.construct(start, end) : message;
        return MigrationItem.of(ItemKind.UNATTRIBUTED_SOURCE, null, construct, text.location(start, end),
                                "The compiler could not attribute this source against the project class path: "
                                + message);
    }

    /** A source file served from memory, so compiler offsets index exactly the text the engine edits. */
    private static final class InMemorySource extends SimpleJavaFileObject {
        private final String content;

        InMemorySource(SourceText text) {
            super(uri(text.path()), Kind.SOURCE);
            String content = text.content();
            // The compiler rejects a byte-order mark; a space keeps every offset unchanged.
            this.content = content.startsWith("\uFEFF") ? " " + content.substring(1) : content;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return content;
        }

        private static URI uri(String path) {
            try {
                return new URI("source", null, "/" + path, null);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Invalid source path " + path, e);
            }
        }
    }

    /** Inventories one compilation unit. */
    private static final class Scanner extends TreePathScanner<Void, Void> {
        private final CompilationUnitTree unit;
        private final SourceText text;
        private final Trees trees;
        private final SourcePositions positions;
        private final Elements elements;
        private final Types types;
        private final MigrationManifest manifest;
        private final List<Found> found;

        Scanner(CompilationUnitTree unit, SourceText text, Trees trees, Elements elements, Types types,
                MigrationManifest manifest, List<Found> found) {
            this.unit = unit;
            this.text = text;
            this.trees = trees;
            this.positions = trees.getSourcePositions();
            this.elements = elements;
            this.types = types;
            this.manifest = manifest;
            this.found = found;
        }

        @Override
        public Void visitImport(ImportTree node, Void unused) {
            Tree qualid = node.getQualifiedIdentifier();
            String name = qualid.toString();
            if (!name.startsWith(V2_PACKAGE) || name.endsWith(".*") || !(qualid instanceof MemberSelectTree select)) {
                // On-demand imports name packages, which have no manifest entries; their types are inventoried at
                // each use.
                return null;
            }
            TreePath qualidPath = new TreePath(getCurrentPath(), qualid);
            if (node.isStatic()) {
                staticImport(node, select, qualidPath);
            } else {
                // An unresolvable name with no manifest entry is also a compiler error, inventoried as
                // UNATTRIBUTED_SOURCE; a project's own type is not a V2 item.
                String binary = binaryName(element(qualidPath), name);
                if (binary != null) {
                    add(ItemKind.IMPORT, EntryKind.typeId(binary), node, start(node), end(node), null);
                }
            }
            // The qualified name is part of the import item, not a separate type reference.
            return null;
        }

        private void staticImport(ImportTree node, MemberSelectTree select, TreePath qualidPath) {
            String member = select.getIdentifier().toString();
            String ownerName = select.getExpression().toString();
            String owner = binaryName(element(new TreePath(qualidPath, select.getExpression())), ownerName);
            if (owner == null) {
                return;
            }
            List<ManifestEntry> candidates = manifest.members(owner, member);
            String id = null;
            String detail = null;
            if (candidates.isEmpty()) {
                detail = "No Parity Manifest member entry named " + member + " in " + owner;
            } else {
                id = candidates.stream().filter(e -> e.classification() != Classification.PRESERVED).findFirst()
                               .orElse(candidates.get(0)).id();
                if (candidates.size() > 1) {
                    detail = "Static import of " + candidates.size() + " overloads; joined by the first non-preserved "
                             + "overload, or the first overload when all are preserved";
                }
            }
            add(ItemKind.STATIC_IMPORT, id, node, start(node), end(node), detail);
        }

        @Override
        public Void visitIdentifier(IdentifierTree node, Void unused) {
            reference(node, node.getName().toString());
            return super.visitIdentifier(node, unused);
        }

        @Override
        public Void visitMemberSelect(MemberSelectTree node, Void unused) {
            reference(node, node.getIdentifier().toString());
            return super.visitMemberSelect(node, unused);
        }

        /** A type reference or a field access at an identifier or member select. */
        private void reference(Tree node, String simpleName) {
            if (simpleName.equals("class") || simpleName.equals("this") || simpleName.equals("super")) {
                return;
            }
            Element e = element(getCurrentPath());
            if (e instanceof TypeElement type && isV2(type)) {
                add(ItemKind.TYPE_REFERENCE, EntryKind.typeId(binary(type)), node, start(node), end(node), null);
            } else if (e instanceof VariableElement field
                       && (e.getKind() == ElementKind.FIELD || e.getKind() == ElementKind.ENUM_CONSTANT)
                       && field.getEnclosingElement() instanceof TypeElement owner && isV2(owner)) {
                String descriptor = descriptor(field.asType());
                add(ItemKind.FIELD_ACCESS,
                    descriptor == null ? null : EntryKind.fieldId(binary(owner), simpleName, descriptor),
                    node, start(node), end(node), descriptor == null ? unattributable(owner, simpleName) : null);
            }
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
            ExpressionTree select = node.getMethodSelect();
            Element e = element(new TreePath(getCurrentPath(), select));
            if (e instanceof ExecutableElement method && method.getEnclosingElement() instanceof TypeElement owner) {
                if (isV2(owner)) {
                    String name = method.getSimpleName().toString();
                    boolean constructor = method.getKind() == ElementKind.CONSTRUCTOR;
                    int start = start(node);
                    if (!constructor && select instanceof MemberSelectTree) {
                        // From the method name, so a receiver chain's invocations do not overlap.
                        start = end(select) - name.length();
                    }
                    member(constructor ? ItemKind.CONSTRUCTOR_CALL : ItemKind.METHOD_INVOCATION, owner, method,
                           node, start, end(node));
                } else {
                    configurationKey(owner, method, node);
                }
            }
            return super.visitMethodInvocation(node, unused);
        }

        @Override
        public Void visitNewClass(NewClassTree node, Void unused) {
            Element e = element(getCurrentPath());
            // An anonymous class body is inventoried through its own members; its type is a type reference.
            if (node.getClassBody() == null && e instanceof ExecutableElement constructor
                && constructor.getEnclosingElement() instanceof TypeElement owner && isV2(owner)) {
                member(ItemKind.CONSTRUCTOR_CALL, owner, constructor, node, start(node), end(node));
            }
            return super.visitNewClass(node, unused);
        }

        @Override
        public Void visitMethod(MethodTree node, Void unused) {
            Element e = element(getCurrentPath());
            if (e instanceof ExecutableElement method && method.getKind() == ElementKind.METHOD
                && !method.getModifiers().contains(Modifier.STATIC)
                && method.getEnclosingElement() instanceof TypeElement type) {
                overridden(method, type).ifPresent(o -> member(ItemKind.METHOD_OVERRIDE,
                                                               (TypeElement) o.getEnclosingElement(), o, node,
                                                               start(node), end(node)));
            }
            return super.visitMethod(node, unused);
        }

        /** The nearest V2 method that {@code method} overrides, searching supertypes breadth-first. */
        private Optional<ExecutableElement> overridden(ExecutableElement method, TypeElement type) {
            ArrayDeque<TypeMirror> queue = new ArrayDeque<>(types.directSupertypes(type.asType()));
            Set<String> visited = new HashSet<>();
            while (!queue.isEmpty()) {
                TypeMirror next = queue.poll();
                if (next.getKind() != TypeKind.DECLARED
                    || !(((DeclaredType) next).asElement() instanceof TypeElement supertype)
                    || !visited.add(binary(supertype))) {
                    continue;
                }
                if (isV2(supertype)) {
                    for (Element member : supertype.getEnclosedElements()) {
                        if (member.getKind() == ElementKind.METHOD
                            && member.getSimpleName().contentEquals(method.getSimpleName())
                            && elements.overrides(method, (ExecutableElement) member, type)) {
                            return Optional.of((ExecutableElement) member);
                        }
                    }
                }
                queue.addAll(types.directSupertypes(next));
            }
            return Optional.empty();
        }

        private void member(ItemKind kind, TypeElement owner, ExecutableElement method, Tree node, int start,
                            int end) {
            String descriptor = methodDescriptor(owner, method);
            String name = method.getKind() == ElementKind.CONSTRUCTOR ? "<init>" : method.getSimpleName().toString();
            add(kind, descriptor == null ? null : EntryKind.methodId(binary(owner), name, descriptor), node, start,
                end, descriptor == null ? unattributable(owner, name) : null);
        }

        /** A V2 configuration key literal passed to {@code System.getenv} or the system-property methods. */
        private void configurationKey(TypeElement owner, ExecutableElement method, MethodInvocationTree node) {
            if (!binary(owner).equals("java.lang.System")
                || !CONFIGURATION_METHODS.contains(method.getSimpleName().toString())
                || node.getArguments().isEmpty()
                || !(node.getArguments().get(0) instanceof LiteralTree literal)
                || !(literal.getValue() instanceof String key)) {
                return;
            }
            String source = method.getSimpleName().contentEquals("getenv") ? EntryKind.ENVIRONMENT_SOURCE
                                                                             : EntryKind.SYSTEM_PROPERTY_SOURCE;
            String id = EntryKind.configurationId(source, key);
            if (manifest.entry(id).isPresent()) {
                add(ItemKind.CONFIGURATION_KEY, id, literal, start(literal), end(literal), null);
            }
        }

        private void add(ItemKind kind, String id, Tree node, int start, int end, String detail) {
            if (start < 0 || end < start) {
                return;
            }
            MigrationItem item = MigrationItem.of(kind, id, text.construct(start, end), text.location(start, end),
                                                  detail);
            TreePath path = getCurrentPath().getLeaf() == node ? getCurrentPath()
                                                               : TreePath.getPath(getCurrentPath(), node);
            found.add(new Found(item, new JavaNode(unit, path == null ? getCurrentPath() : path, trees, elements,
                                                   types)));
        }

        private Element element(TreePath path) {
            try {
                return trees.getElement(path);
            } catch (RuntimeException e) {
                // Defensive: an unattributable tree has no element.
                return null;
            }
        }

        /** The binary name of a resolved type, or the manifest's binary name for an unresolved source name. */
        private String binaryName(Element element, String sourceName) {
            if (element instanceof TypeElement type) {
                return isV2(type) ? binary(type) : null;
            }
            return manifest.typeEntry(sourceName).map(e -> e.id().substring("api:".length())).orElse(null);
        }

        /** True for a type of the V2 API namespace that the project's own sources do not declare. */
        private boolean isV2(TypeElement type) {
            return binary(type).startsWith(V2_PACKAGE) && trees.getPath(type) == null;
        }

        private String binary(TypeElement type) {
            return elements.getBinaryName(type).toString();
        }

        private String unattributable(TypeElement owner, String member) {
            return "The signature of " + binary(owner) + "#" + member + " could not be attributed";
        }

        /** The JVM method descriptor of {@code method}'s erased declaration, or {@code null} if unattributable. */
        private String methodDescriptor(TypeElement owner, ExecutableElement method) {
            StringBuilder out = new StringBuilder("(");
            if (method.getKind() == ElementKind.CONSTRUCTOR && owner.getNestingKind() == NestingKind.MEMBER
                && !owner.getModifiers().contains(Modifier.STATIC) && owner.getKind() == ElementKind.CLASS
                && owner.getEnclosingElement() instanceof TypeElement outer) {
                // An inner class constructor takes its enclosing instance first in the class file.
                out.append(descriptor(outer.asType()));
            }
            for (VariableElement parameter : method.getParameters()) {
                String d = descriptor(parameter.asType());
                if (d == null) {
                    return null;
                }
                out.append(d);
            }
            out.append(')');
            String result = method.getKind() == ElementKind.CONSTRUCTOR ? "V" : descriptor(method.getReturnType());
            return result == null ? null : out.append(result).toString();
        }

        /** The JVM field descriptor of {@code type}'s erasure, or {@code null} if unattributable. */
        private String descriptor(TypeMirror type) {
            TypeMirror erased = types.erasure(type);
            switch (erased.getKind()) {
                case BOOLEAN: return "Z";
                case BYTE: return "B";
                case CHAR: return "C";
                case SHORT: return "S";
                case INT: return "I";
                case LONG: return "J";
                case FLOAT: return "F";
                case DOUBLE: return "D";
                case VOID: return "V";
                case ARRAY:
                    String component = descriptor(((ArrayType) erased).getComponentType());
                    return component == null ? null : "[" + component;
                case DECLARED:
                    Element element = ((DeclaredType) erased).asElement();
                    return element instanceof TypeElement t ? "L" + binary(t).replace('.', '/') + ";" : null;
                default:
                    return null;
            }
        }

        private int start(Tree tree) {
            return (int) positions.getStartPosition(unit, tree);
        }

        private int end(Tree tree) {
            return (int) positions.getEndPosition(unit, tree);
        }
    }
}
