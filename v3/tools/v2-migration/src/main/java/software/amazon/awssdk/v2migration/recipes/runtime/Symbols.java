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

import com.sun.source.util.TreePath;
import java.util.ArrayDeque;
import java.util.HashSet;
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
import software.amazon.awssdk.v2migration.manifest.EntryKind;
import software.amazon.awssdk.v2migration.recipe.JavaNode;

/**
 * Manifest IDs of attributed symbols, computed exactly as the engine's inventory joins items to the manifest: binary
 * names, and JVM descriptors of the erased declaration (an inner class constructor takes its enclosing instance first).
 */
final class Symbols {

    /** The V2 API package prefix. */
    static final String V2_PACKAGE = "software.amazon.awssdk.";

    private final JavaNode node;

    Symbols(JavaNode node) {
        this.node = node;
    }

    /** The element of {@code path}, or {@code null} if it cannot be attributed. */
    Element element(TreePath path) {
        try {
            return path == null ? null : node.trees().getElement(path);
        } catch (RuntimeException e) {
            return null;
        }
    }

    String binary(TypeElement type) {
        return node.elements().getBinaryName(type).toString();
    }

    /** True for a type of the V2 API namespace that the project's own sources do not declare. */
    boolean isV2(TypeElement type) {
        return binary(type).startsWith(V2_PACKAGE) && node.trees().getPath(type) == null;
    }

    /** The manifest ID of a method or constructor, or empty if its signature cannot be attributed. */
    Optional<String> methodId(ExecutableElement method) {
        if (!(method.getEnclosingElement() instanceof TypeElement owner)) {
            return Optional.empty();
        }
        StringBuilder out = new StringBuilder("(");
        boolean constructor = method.getKind() == ElementKind.CONSTRUCTOR;
        if (constructor && owner.getNestingKind() == NestingKind.MEMBER
            && !owner.getModifiers().contains(Modifier.STATIC) && owner.getKind() == ElementKind.CLASS
            && owner.getEnclosingElement() instanceof TypeElement outer) {
            out.append(descriptor(outer.asType()));
        }
        for (VariableElement parameter : method.getParameters()) {
            String d = descriptor(parameter.asType());
            if (d == null) {
                return Optional.empty();
            }
            out.append(d);
        }
        out.append(')');
        String result = constructor ? "V" : descriptor(method.getReturnType());
        if (result == null) {
            return Optional.empty();
        }
        String name = constructor ? "<init>" : method.getSimpleName().toString();
        return Optional.of(EntryKind.methodId(binary(owner), name, out.append(result).toString()));
    }

    /** The JVM field descriptor of {@code type}'s erasure, or {@code null} if unattributable. */
    String descriptor(TypeMirror type) {
        TypeMirror erased = node.types().erasure(type);
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

    /** The nearest V2 method that {@code method} overrides, searching supertypes breadth-first like the inventory. */
    Optional<ExecutableElement> overriddenV2(ExecutableElement method) {
        if (method.getKind() != ElementKind.METHOD || method.getModifiers().contains(Modifier.STATIC)
            || !(method.getEnclosingElement() instanceof TypeElement type)) {
            return Optional.empty();
        }
        ArrayDeque<TypeMirror> queue = new ArrayDeque<>(node.types().directSupertypes(type.asType()));
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
                        && node.elements().overrides(method, (ExecutableElement) member, type)) {
                        return Optional.of((ExecutableElement) member);
                    }
                }
            }
            queue.addAll(node.types().directSupertypes(next));
        }
        return Optional.empty();
    }
}
