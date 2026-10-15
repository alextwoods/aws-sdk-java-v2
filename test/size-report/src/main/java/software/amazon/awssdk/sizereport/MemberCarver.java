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

package software.amazon.awssdk.sizereport;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Measures the class-file bytes of a set of class members by removing them.
 *
 * <p>The class is written twice with an ASM {@link ClassWriter} that builds a fresh constant pool: once whole and
 * once without the members. The difference is the bytes of the members' {@code method_info}/{@code field_info}
 * structures (code, exception tables, stack map frames, debug attributes) plus the constant-pool entries and
 * bootstrap methods that only those members use. Constant-pool entries shared with the rest of the class stay with
 * the rest of the class.
 *
 * <p>The members are:
 * <ul>
 *     <li>the given methods;</li>
 *     <li>the given static fields, together with the {@code <clinit>} segment that computes each one: the
 *     instructions after the previous {@code putstatic} to a field of the class (or the start of {@code <clinit>})
 *     up to and including the field's own {@code putstatic};</li>
 *     <li>transitively, the {@code lambda$} methods of the class that the removed code creates through
 *     {@code invokedynamic}.</li>
 * </ul>
 */
@SdkInternalApi
final class MemberCarver {
    private static final String CLINIT = "<clinit>";
    private static final String LAMBDA_PREFIX = "lambda$";

    private MemberCarver() {
    }

    /**
     * Carve the members out of a class.
     *
     * @param classBytes the class file
     * @param category the category the carved bytes go to
     * @param methods the methods to carve, as {@code name + descriptor}
     * @param staticFields the static fields to carve with their {@code <clinit>} segments
     * @param keep methods ({@code name + descriptor}) that are never carved, for example because another carve of the
     * same class already holds them
     * @return the carve, or {@code null} if no member was selected
     */
    static ClassAttribution.Carve carve(byte[] classBytes, SizeCategory category, Set<String> methods,
                                        Set<String> staticFields, Set<String> keep) {
        ClassNode reduced = read(classBytes);
        Set<String> dropMethods = new LinkedHashSet<>();
        for (String method : methods) {
            if (!keep.contains(method)) {
                dropMethods.add(method);
            }
        }

        MethodNode clinit = find(reduced, CLINIT + "()V");
        List<AbstractInsnNode> segmentInsns = new ArrayList<>();
        int segments = 0;
        if (clinit != null && !staticFields.isEmpty()) {
            AbstractInsnNode start = clinit.instructions.getFirst();
            for (AbstractInsnNode insn = start; insn != null; insn = insn.getNext()) {
                if (insn.getOpcode() == Opcodes.PUTSTATIC && ((FieldInsnNode) insn).owner.equals(reduced.name)) {
                    if (staticFields.contains(((FieldInsnNode) insn).name)) {
                        for (AbstractInsnNode s = start; s != insn.getNext(); s = s.getNext()) {
                            segmentInsns.add(s);
                        }
                        segments++;
                    }
                    start = insn.getNext();
                }
            }
        }

        Deque<String> work = new ArrayDeque<>(dropMethods);
        addCreatedLambdas(reduced, segmentInsns, dropMethods, keep, work);
        while (!work.isEmpty()) {
            MethodNode method = find(reduced, work.pop());
            if (method != null) {
                List<AbstractInsnNode> insns = new ArrayList<>();
                Collections.addAll(insns, method.instructions.toArray());
                addCreatedLambdas(reduced, insns, dropMethods, keep, work);
            }
        }

        List<String> droppedMethods = new ArrayList<>();
        reduced.methods.removeIf(m -> {
            boolean drop = dropMethods.contains(m.name + m.desc);
            if (drop) {
                droppedMethods.add(m.name + m.desc);
            }
            return drop;
        });
        List<String> droppedFields = new ArrayList<>();
        reduced.fields.removeIf(f -> {
            boolean drop = isStatic(f) && staticFields.contains(f.name);
            if (drop) {
                droppedFields.add(f.name);
            }
            return drop;
        });
        for (AbstractInsnNode insn : segmentInsns) {
            // Labels stay: kept code (line numbers, jumps, try-catch ranges) may refer to them, and they take no bytes.
            if (!(insn instanceof LabelNode)) {
                clinit.instructions.remove(insn);
            }
        }
        if (droppedMethods.isEmpty() && droppedFields.isEmpty() && segments == 0) {
            return null;
        }
        long carved = write(read(classBytes)) - write(reduced);
        Collections.sort(droppedMethods);
        Collections.sort(droppedFields);
        return new ClassAttribution.Carve(category, carved, droppedMethods, droppedFields, segments);
    }

    static ClassNode read(byte[] classBytes) {
        ClassNode node = new ClassNode();
        // Flags 0: keep debug information and frames, so both writes encode what the compiler emitted.
        new ClassReader(classBytes).accept(node, 0);
        return node;
    }

    private static long write(ClassNode node) {
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray().length;
    }

    private static MethodNode find(ClassNode node, String nameAndDesc) {
        for (MethodNode method : node.methods) {
            if (nameAndDesc.equals(method.name + method.desc)) {
                return method;
            }
        }
        return null;
    }

    private static void addCreatedLambdas(ClassNode node, List<AbstractInsnNode> insns, Set<String> dropMethods,
                                          Set<String> keep, Deque<String> work) {
        for (AbstractInsnNode insn : insns) {
            if (!(insn instanceof InvokeDynamicInsnNode)) {
                continue;
            }
            for (Object arg : ((InvokeDynamicInsnNode) insn).bsmArgs) {
                if (arg instanceof Handle) {
                    Handle handle = (Handle) arg;
                    String key = handle.getName() + handle.getDesc();
                    if (handle.getOwner().equals(node.name) && handle.getName().startsWith(LAMBDA_PREFIX)
                        && !keep.contains(key) && dropMethods.add(key)) {
                        work.add(key);
                    }
                }
            }
        }
    }

    static boolean isStatic(FieldNode field) {
        return (field.access & Opcodes.ACC_STATIC) != 0;
    }
}
