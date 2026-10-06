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

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import software.amazon.awssdk.v2migration.ItemKind;
import software.amazon.awssdk.v2migration.MigrationItem;
import software.amazon.awssdk.v2migration.manifest.Classification;
import software.amazon.awssdk.v2migration.manifest.EntryKind;
import software.amazon.awssdk.v2migration.manifest.ManifestEntry;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;
import software.amazon.awssdk.v2migration.recipe.JavaNode;
import software.amazon.awssdk.v2migration.recipe.MigrationRecipe;
import software.amazon.awssdk.v2migration.recipe.RecipeContext;
import software.amazon.awssdk.v2migration.recipe.RecipeOutcome;
import software.amazon.awssdk.v2migration.recipe.core.ManifestIds.Member;

/**
 * Reports uses of builder, client, configuration and request override members (see {@link Scope}) that the Parity
 * Manifest classifies {@code PRESERVED} although their signature uses a V2 type that the manifest renames or
 * excludes. A preserved member keeps its V2 parameter and return types (Requirement 17.1), which cannot hold when
 * such a type changes its name or does not exist in V3, and the manifest records no V3 member with the replaced types,
 * so the V3 counterpart of the use is uncertain: the use is left unchanged and reported (Requirement 17.9) with the
 * types and their manifest replacements. The whole generic signature is checked, so a type argument counts too (for
 * example {@code ClientOverrideConfiguration.Builder#retryStrategy(Consumer<RetryStrategy.Builder>)} uses
 * {@code RetryStrategy.Builder}).
 *
 * <p>A manifest that classifies such a member {@code ADAPTED} with a recipe and the replaced signature is handled by
 * {@link RenamedMemberRecipe}; a member whose V3 counterpart moved to another receiver (for example from the override
 * configuration to the client builder) cannot be expressed as a rename and keeps the manifest's diagnostic.
 */
public final class ReplacedSignatureTypeRecipe implements MigrationRecipe {
    private static final String V2 = "software.amazon.awssdk.";

    @Override
    public String id() {
        return "members.replaced-signature-types";
    }

    @Override
    public String description() {
        return "Reports uses of preserved builder, client, configuration and request override members whose "
               + "signature uses a type that the Parity Manifest renames or excludes.";
    }

    @Override
    public RecipeOutcome visit(RecipeContext context) {
        MigrationItem item = context.item();
        ItemKind kind = item.kind();
        if (kind != ItemKind.METHOD_INVOCATION && kind != ItemKind.CONSTRUCTOR_CALL && kind != ItemKind.FIELD_ACCESS) {
            return RecipeOutcome.notApplicable();
        }
        ManifestEntry entry = context.entry().orElse(null);
        Optional<Member> member = entry == null ? Optional.empty() : ManifestIds.member(entry.id());
        Optional<JavaNode> node = context.java();
        if (entry == null || entry.classification() != Classification.PRESERVED || member.isEmpty()
            || Scope.of(member.get().owner()).isEmpty() || node.isEmpty()) {
            return RecipeOutcome.notApplicable();
        }
        Element element = element(node.get());
        if (element == null) {
            return RecipeOutcome.notApplicable();
        }
        List<String> problems = problems(node.get(), element, context.manifest());
        if (problems.isEmpty()) {
            return RecipeOutcome.notApplicable();
        }
        return RecipeOutcome.unsafe("The Parity Manifest classifies " + ManifestIds.canonical(member.get().owner()) + "#"
                                    + member.get().name() + " as PRESERVED, but its signature uses "
                                    + String.join("; ", problems) + ". The manifest records no V3 member with the "
                                    + "replaced types, so the V3 counterpart of this use is uncertain and it is left "
                                    + "unchanged; adapt it to the V3 API manually");
    }

    /** The member the item's construct resolves to. */
    private static Element element(JavaNode node) {
        try {
            TreePath path = node.tree() instanceof MethodInvocationTree invocation
                            ? new TreePath(node.path(), invocation.getMethodSelect())
                            : node.path();
            Element element = node.trees().getElement(path);
            return element instanceof ExecutableElement || element instanceof VariableElement ? element : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** One description per V2 type of the signature that the manifest renames or excludes, by binary name. */
    private static List<String> problems(JavaNode node, Element member, MigrationManifest manifest) {
        TreeMap<String, TypeElement> types = new TreeMap<>();
        if (member instanceof ExecutableElement executable) {
            collect(node, executable.getReturnType(), types);
            executable.getParameters().forEach(p -> collect(node, p.asType(), types));
            executable.getThrownTypes().forEach(t -> collect(node, t, types));
            for (TypeParameterElement parameter : executable.getTypeParameters()) {
                parameter.getBounds().forEach(b -> collect(node, b, types));
            }
        } else {
            collect(node, member.asType(), types);
        }
        List<String> problems = new ArrayList<>();
        for (String binary : types.keySet()) {
            Optional<ManifestEntry> typeEntry = manifest.entry(EntryKind.typeId(binary));
            Optional<String> renamed = ManifestIds.renamedType(manifest, binary);
            if (renamed.isPresent()) {
                problems.add(ManifestIds.canonical(binary) + ", which the manifest replaces with "
                             + ManifestIds.canonical(renamed.get()) + " (" + ManifestIds.describe(typeEntry.get())
                             + ")");
            } else if (typeEntry.isPresent() && typeEntry.get().classification() == Classification.EXCLUDED) {
                problems.add(ManifestIds.canonical(binary) + ", which V3 excludes ("
                             + ManifestIds.describe(typeEntry.get())
                             + (typeEntry.get().replacement() == null ? ""
                                                                      : "; replacement " + typeEntry.get().replacement())
                             + ")");
            }
        }
        return problems;
    }

    /** Adds every V2 class or interface {@code type} mentions, type arguments and wildcard bounds included. */
    private static void collect(JavaNode node, TypeMirror type, TreeMap<String, TypeElement> out) {
        switch (type.getKind()) {
            case DECLARED -> {
                DeclaredType declared = (DeclaredType) type;
                if (declared.asElement() instanceof TypeElement element) {
                    String binary = node.elements().getBinaryName(element).toString();
                    boolean v2 = binary.startsWith(V2) && node.trees().getPath(element) == null;
                    if (v2 && out.putIfAbsent(binary, element) != null) {
                        // Already expanded.
                        return;
                    }
                    declared.getTypeArguments().forEach(a -> collect(node, a, out));
                }
            }
            case ARRAY -> collect(node, ((ArrayType) type).getComponentType(), out);
            case WILDCARD -> {
                WildcardType wildcard = (WildcardType) type;
                if (wildcard.getExtendsBound() != null) {
                    collect(node, wildcard.getExtendsBound(), out);
                }
                if (wildcard.getSuperBound() != null) {
                    collect(node, wildcard.getSuperBound(), out);
                }
            }
            default -> {
                // Primitives, type variables (their bounds are checked on the declaring member) and others.
            }
        }
    }
}
