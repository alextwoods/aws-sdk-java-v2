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

package software.amazon.awssdk.test.architecture;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * The class-level facts that the architecture tests need from a class file (JVMS chapter 4): its name, access flags,
 * supertypes, generic signature, fields and methods (with descriptors, signatures and declared exceptions), and every
 * class it references through its constant pool.
 *
 * <p>This is a deliberately minimal reader instead of a bytecode library. JDK 21 has no supported class-file API
 * ({@code java.lang.classfile} is a preview API in JDK 22-23 and final only in JDK 24), and a library such as ASM
 * rejects class files newer than the versions it knows, so every JDK upgrade of a scanned dependency would require an
 * ASM upgrade. The structures read here (constant-pool tags 1-20, member tables, the {@code Signature},
 * {@code Exceptions}, {@code Code} and {@code BootstrapMethods} attributes, and the instruction set) have been stable
 * since Java 11; all other attributes are skipped by length, so the reader is independent of the class-file version.
 *
 * <p>{@link #referencedClasses()} covers {@code CONSTANT_Class} entries (every class used by instructions,
 * supertypes, exception tables and declared exceptions), descriptors of member references, method types and
 * {@code invokedynamic} call sites, and the descriptors and generic signatures of the class's own fields and methods.
 * String constants are not references. Annotations are not read: the JVM ignores annotations whose types are absent,
 * so they cannot link a class to a forbidden artifact.
 *
 * <p>{@link #memberReferences()} are the field and method references of the constant pool, and
 * {@link #stringConstants()} its {@code CONSTANT_String} values. Each method's {@link Member#memberReferences()} are
 * the members its {@code Code} attribute uses: the operands of field-access and invoke instructions, method-handle
 * constants loaded with {@code ldc}, and the bootstrap method and method-handle arguments of its
 * {@code invokedynamic} call sites (so a method reference such as {@code Class::forName} is attributed to the method
 * that creates it).
 */
public record ClassFile(String name, int access, String superName, List<String> interfaces, String signature,
                        List<Member> fields, List<Member> methods, Set<String> referencedClasses,
                        Set<MemberRef> memberReferences, Set<String> stringConstants) {

    public static final int ACC_PUBLIC = 0x0001;
    public static final int ACC_PRIVATE = 0x0002;
    public static final int ACC_PROTECTED = 0x0004;
    public static final int ACC_STATIC = 0x0008;
    public static final int ACC_SYNTHETIC = 0x1000;

    private static final int MAGIC = 0xCAFEBABE;

    /**
     * A field or method. {@code signature} is empty when absent; {@code exceptions} is empty for fields;
     * {@code memberReferences} are the members used by a method's code and are empty for fields and for methods
     * without code.
     */
    public record Member(String name, String descriptor, int access, String signature, List<String> exceptions,
                         Set<MemberRef> memberReferences) {

        /** Public or protected, and not compiler-generated. */
        public boolean isApi() {
            return (access & (ACC_PUBLIC | ACC_PROTECTED)) != 0 && (access & ACC_SYNTHETIC) == 0;
        }

        public boolean isStatic() {
            return (access & ACC_STATIC) != 0;
        }

        public boolean isPrivate() {
            return (access & ACC_PRIVATE) != 0;
        }
    }

    /**
     * A field or method reference: the binary name of the class or interface named by the reference (an array
     * descriptor such as {@code [Ljava.lang.Object;} for array methods), the member name and its descriptor.
     */
    public record MemberRef(String owner, String name, String descriptor) implements Comparable<MemberRef> {

        @Override
        public int compareTo(MemberRef other) {
            int result = owner.compareTo(other.owner);
            if (result == 0) {
                result = name.compareTo(other.name);
            }
            return result != 0 ? result : descriptor.compareTo(other.descriptor);
        }

        @Override
        public String toString() {
            return owner + "." + name + descriptor;
        }
    }

    public boolean isPublic() {
        return (access & ACC_PUBLIC) != 0;
    }

    /** The class's simple binary name: the part after the last {@code .}, including any {@code $} nesting. */
    public String simpleName() {
        return name.substring(name.lastIndexOf('.') + 1);
    }

    /** The package name, empty for the unnamed package. */
    public String packageName() {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(0, dot);
    }

    /** Parses a class file. Names are binary names ({@code a.b.Outer$Inner}). */
    public static ClassFile parse(byte[] bytes) {
        try {
            return new Reader(new DataInputStream(new ByteArrayInputStream(bytes))).read();
        } catch (IOException e) {
            throw new UncheckedIOException("Truncated or malformed class file", e);
        }
    }

    /**
     * Binary names of every class type in a field/method descriptor or a generic signature (JVMS 4.3 and 4.7.9.1).
     * Type variables, primitives and the simple names of inner-class suffixes ({@code .Inner}) are not class names
     * and are skipped.
     */
    public static Set<String> classNamesIn(String descriptorOrSignature) {
        Set<String> result = new TreeSet<>();
        try {
            new SignatureScanner(descriptorOrSignature, result).scan();
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Malformed descriptor or signature: " + descriptorOrSignature, e);
        }
        return result;
    }

    /**
     * Recursive-descent scanner for the descriptor and signature grammars of JVMS 4.3 and 4.7.9.1. Descriptors are
     * the subset without type parameters, type variables and throws clauses.
     */
    private static final class SignatureScanner {
        private final String s;
        private final Set<String> out;
        private int pos;

        SignatureScanner(String s, Set<String> out) {
            this.s = s;
            this.out = out;
        }

        void scan() {
            if (pos < s.length() && s.charAt(pos) == '<') {
                formalTypeParameters();
            }
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == '(' || c == ')' || c == '^') {
                    pos++;
                } else {
                    type();
                }
            }
        }

        private void formalTypeParameters() {
            pos++;
            while (s.charAt(pos) != '>') {
                while (s.charAt(pos) != ':') {
                    pos++; // identifier
                }
                // ClassBound is ':' followed by an optional type; each InterfaceBound is ':' followed by a type.
                while (s.charAt(pos) == ':') {
                    pos++;
                    if ("LT[".indexOf(s.charAt(pos)) >= 0) {
                        type();
                    }
                }
            }
            pos++;
        }

        private void type() {
            char c = s.charAt(pos);
            switch (c) {
                case 'L' -> classType();
                case 'T' -> {
                    int end = s.indexOf(';', pos);
                    if (end < 0) {
                        throw new IllegalArgumentException("Unterminated type variable at " + pos + " in " + s);
                    }
                    pos = end + 1;
                }
                case '[' -> {
                    pos++;
                    type();
                }
                case 'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z', 'V' -> pos++;
                default -> throw new IllegalArgumentException("Unexpected '" + c + "' at " + pos + " in " + s);
            }
        }

        private void classType() {
            pos++;
            String name = identifier();
            out.add(name);
            while (true) {
                char c = s.charAt(pos);
                if (c == '<') {
                    typeArguments();
                } else if (c == '.') {
                    pos++;
                    name = name + "$" + identifier();
                    out.add(name);
                } else if (c == ';') {
                    pos++;
                    return;
                } else {
                    throw new IllegalArgumentException("Unexpected '" + c + "' at " + pos + " in " + s);
                }
            }
        }

        private void typeArguments() {
            pos++;
            while (s.charAt(pos) != '>') {
                char c = s.charAt(pos);
                if (c == '*') {
                    pos++;
                } else {
                    if (c == '+' || c == '-') {
                        pos++;
                    }
                    type();
                }
            }
            pos++;
        }

        private String identifier() {
            int start = pos;
            while (";<.".indexOf(s.charAt(pos)) < 0) {
                pos++;
            }
            return s.substring(start, pos).replace('/', '.');
        }
    }

    private static final class Reader {
        private final DataInputStream in;
        private Object[] pool;
        private int[] tags;

        Reader(DataInputStream in) {
            this.in = in;
        }

        ClassFile read() throws IOException {
            if (in.readInt() != MAGIC) {
                throw new IllegalArgumentException("Not a class file (bad magic number)");
            }
            in.readUnsignedShort(); // minor_version
            in.readUnsignedShort(); // major_version
            readConstantPool();
            int access = in.readUnsignedShort();
            String name = className(in.readUnsignedShort());
            int superIndex = in.readUnsignedShort();
            String superName = superIndex == 0 ? "" : className(superIndex);
            List<String> interfaces = new ArrayList<>();
            for (int i = in.readUnsignedShort(); i > 0; i--) {
                interfaces.add(className(in.readUnsignedShort()));
            }
            Set<String> references = new TreeSet<>(poolReferences());
            List<RawMember> fields = readMembers(references);
            List<RawMember> methods = readMembers(references);
            String signature = "";
            int[][] bootstrapMethods = new int[0][];
            for (int i = in.readUnsignedShort(); i > 0; i--) {
                String attribute = utf8(in.readUnsignedShort());
                int length = in.readInt();
                if (attribute.equals("Signature")) {
                    signature = utf8(in.readUnsignedShort());
                } else if (attribute.equals("BootstrapMethods")) {
                    bootstrapMethods = readBootstrapMethods();
                } else {
                    in.skipNBytes(length);
                }
            }
            references.addAll(classNamesIn(signature));
            references.remove(name);
            return new ClassFile(name, access, superName, List.copyOf(interfaces), signature,
                                 finish(fields, bootstrapMethods), finish(methods, bootstrapMethods),
                                 Collections.unmodifiableSet(references), poolMemberReferences(), poolStrings());
        }

        /** Each bootstrap method as {@code [bootstrap_method_ref, argument...]}. */
        private int[][] readBootstrapMethods() throws IOException {
            int[][] result = new int[in.readUnsignedShort()][];
            for (int i = 0; i < result.length; i++) {
                int method = in.readUnsignedShort();
                int[] entry = new int[1 + in.readUnsignedShort()];
                entry[0] = method;
                for (int a = 1; a < entry.length; a++) {
                    entry[a] = in.readUnsignedShort();
                }
                result[i] = entry;
            }
            return result;
        }

        private List<Member> finish(List<RawMember> raw, int[][] bootstrapMethods) {
            List<Member> result = new ArrayList<>();
            for (RawMember m : raw) {
                Set<MemberRef> used = new TreeSet<>(m.memberReferences);
                for (int indy : m.invokeDynamicIndices) {
                    int[] bootstrap = bootstrapMethods[((int[]) pool[indy])[0]];
                    for (int index : bootstrap) {
                        if (tags[index] == 15) {
                            used.add(memberRef((Integer) pool[index]));
                        }
                    }
                }
                result.add(new Member(m.name, m.descriptor, m.access, m.signature, m.exceptions,
                                      Collections.unmodifiableSet(used)));
            }
            return List.copyOf(result);
        }

        private Set<MemberRef> poolMemberReferences() {
            Set<MemberRef> result = new TreeSet<>();
            for (int i = 1; i < pool.length; i++) {
                if (tags[i] == 9 || tags[i] == 10 || tags[i] == 11) {
                    result.add(memberRef(i));
                }
            }
            return Collections.unmodifiableSet(result);
        }

        private Set<String> poolStrings() {
            Set<String> result = new TreeSet<>();
            for (int i = 1; i < pool.length; i++) {
                if (tags[i] == 8) {
                    result.add(utf8((Integer) pool[i]));
                }
            }
            return Collections.unmodifiableSet(result);
        }

        /** The Fieldref, Methodref or InterfaceMethodref at {@code index}. */
        private MemberRef memberRef(int index) {
            if (tags[index] != 9 && tags[index] != 10 && tags[index] != 11) {
                throw new IllegalArgumentException("Constant-pool entry " + index + " is not a member reference");
            }
            int[] ref = (int[]) pool[index];
            int[] nameAndType = (int[]) pool[ref[1]];
            return new MemberRef(className(ref[0]), utf8(nameAndType[0]), utf8(nameAndType[1]));
        }

        /**
         * Members used by the instructions of one {@code Code} array (JVMS 6.5), plus the {@code CONSTANT_InvokeDynamic}
         * indices of its {@code invokedynamic} instructions, which are resolved once the class's
         * {@code BootstrapMethods} attribute has been read.
         */
        private void scanCode(byte[] code, Set<MemberRef> used, List<Integer> invokeDynamic) {
            int pc = 0;
            while (pc < code.length) {
                int opcode = code[pc] & 0xFF;
                switch (opcode) {
                    case 0x12 -> ldc(code[pc + 1] & 0xFF, used);                                  // ldc
                    case 0x13 -> ldc(u2(code, pc + 1), used);                                     // ldc_w
                    case 0xb2, 0xb3, 0xb4, 0xb5, 0xb6, 0xb7, 0xb8, 0xb9 ->                         // field, invoke*
                        used.add(memberRef(u2(code, pc + 1)));
                    case 0xba -> invokeDynamic.add(u2(code, pc + 1));                             // invokedynamic
                    default -> {
                    }
                }
                pc += instructionLength(code, pc, opcode);
            }
        }

        private void ldc(int index, Set<MemberRef> used) {
            if (tags[index] == 15) {
                used.add(memberRef((Integer) pool[index]));
            }
        }

        private static int instructionLength(byte[] code, int pc, int opcode) {
            if (opcode == 0xaa || opcode == 0xab) {                                               // *switch
                int operands = (pc + 4) & ~3;                                                     // 4-byte aligned
                if (opcode == 0xaa) {
                    int low = s4(code, operands + 4);
                    int high = s4(code, operands + 8);
                    return operands + 12 + (high - low + 1) * 4 - pc;
                }
                return operands + 8 + s4(code, operands + 4) * 8 - pc;
            }
            if (opcode == 0xc4) {                                                                 // wide
                return (code[pc + 1] & 0xFF) == 0x84 ? 6 : 4;
            }
            if (opcode < INSTRUCTION_LENGTHS.length && INSTRUCTION_LENGTHS[opcode] > 0) {
                return INSTRUCTION_LENGTHS[opcode];
            }
            throw new IllegalArgumentException("Unknown opcode 0x" + Integer.toHexString(opcode) + " at " + pc);
        }

        private static int u2(byte[] code, int at) {
            return ((code[at] & 0xFF) << 8) | (code[at + 1] & 0xFF);
        }

        private static int s4(byte[] code, int at) {
            return ((code[at] & 0xFF) << 24) | ((code[at + 1] & 0xFF) << 16) | ((code[at + 2] & 0xFF) << 8)
                   | (code[at + 3] & 0xFF);
        }

        private void readConstantPool() throws IOException {
            int count = in.readUnsignedShort();
            pool = new Object[count];
            tags = new int[count];
            for (int i = 1; i < count; i++) {
                int tag = in.readUnsignedByte();
                tags[i] = tag;
                switch (tag) {
                    case 1 -> pool[i] = in.readUTF();                                      // Utf8
                    case 3, 4 -> in.skipNBytes(4);                                          // Integer, Float
                    case 5, 6 -> {                                                          // Long, Double: two slots
                        in.skipNBytes(8);
                        i++;
                    }
                    case 7, 8, 16, 19, 20 -> pool[i] = in.readUnsignedShort();              // Class, String,
                                                                                            // MethodType, Module, Package
                    case 9, 10, 11, 12, 17, 18 -> pool[i] = new int[] {in.readUnsignedShort(),
                                                                       in.readUnsignedShort()}; // refs, NameAndType,
                                                                                            // (Invoke)Dynamic
                    case 15 -> {                                                            // MethodHandle
                        in.readUnsignedByte();
                        pool[i] = in.readUnsignedShort();
                    }
                    default -> throw new IllegalArgumentException("Unknown constant-pool tag " + tag + " at index " + i);
                }
            }
        }

        private Set<String> poolReferences() {
            Set<String> result = new TreeSet<>();
            for (int i = 1; i < pool.length; i++) {
                switch (tags[i]) {
                    case 7 -> {
                        String internal = utf8((Integer) pool[i]);
                        result.addAll(internal.startsWith("[") ? classNamesIn(internal)
                                                               : Set.of(internal.replace('/', '.')));
                    }
                    case 12 -> result.addAll(classNamesIn(utf8(((int[]) pool[i])[1])));   // NameAndType descriptor
                    case 16 -> result.addAll(classNamesIn(utf8((Integer) pool[i])));       // MethodType descriptor
                    default -> {
                    }
                }
            }
            return result;
        }

        private List<RawMember> readMembers(Set<String> references) throws IOException {
            List<RawMember> result = new ArrayList<>();
            for (int count = in.readUnsignedShort(); count > 0; count--) {
                int access = in.readUnsignedShort();
                String name = utf8(in.readUnsignedShort());
                String descriptor = utf8(in.readUnsignedShort());
                String signature = "";
                List<String> exceptions = new ArrayList<>();
                Set<MemberRef> used = new TreeSet<>();
                List<Integer> invokeDynamic = new ArrayList<>();
                for (int i = in.readUnsignedShort(); i > 0; i--) {
                    String attribute = utf8(in.readUnsignedShort());
                    int length = in.readInt();
                    if (attribute.equals("Signature")) {
                        signature = utf8(in.readUnsignedShort());
                    } else if (attribute.equals("Exceptions")) {
                        for (int e = in.readUnsignedShort(); e > 0; e--) {
                            exceptions.add(className(in.readUnsignedShort()));
                        }
                    } else if (attribute.equals("Code")) {
                        // max_stack, max_locals, code_length, code; the exception table and nested attributes
                        // (line numbers, stack maps) reference no members and are skipped with the remainder.
                        in.skipNBytes(4);
                        int codeLength = in.readInt();
                        scanCode(in.readNBytes(codeLength), used, invokeDynamic);
                        in.skipNBytes(length - 8L - codeLength);
                    } else {
                        in.skipNBytes(length);
                    }
                }
                references.addAll(classNamesIn(descriptor));
                references.addAll(classNamesIn(signature));
                result.add(new RawMember(name, descriptor, access, signature, List.copyOf(exceptions), used,
                                         invokeDynamic));
            }
            return result;
        }

        private record RawMember(String name, String descriptor, int access, String signature,
                                 List<String> exceptions, Set<MemberRef> memberReferences,
                                 List<Integer> invokeDynamicIndices) {
        }

        /** Fixed instruction lengths by opcode (JVMS 6.5); 0 marks the variable-length switch and wide forms. */
        private static final int[] INSTRUCTION_LENGTHS = instructionLengths();

        private static int[] instructionLengths() {
            int[] lengths = new int[0xca];                  // 0xca (breakpoint) and above never occur in class files
            Arrays.fill(lengths, 1);
            lengths[0x10] = 2;                              // bipush
            lengths[0x11] = 3;                              // sipush
            lengths[0x12] = 2;                              // ldc
            lengths[0x13] = 3;                              // ldc_w
            lengths[0x14] = 3;                              // ldc2_w
            Arrays.fill(lengths, 0x15, 0x1a, 2);            // iload..aload
            Arrays.fill(lengths, 0x36, 0x3b, 2);            // istore..astore
            lengths[0x84] = 3;                              // iinc
            Arrays.fill(lengths, 0x99, 0xa9, 3);            // if*, goto, jsr
            lengths[0xa9] = 2;                              // ret
            lengths[0xaa] = 0;                              // tableswitch
            lengths[0xab] = 0;                              // lookupswitch
            Arrays.fill(lengths, 0xb2, 0xb9, 3);            // get/put field/static, invokevirtual/special/static
            lengths[0xb9] = 5;                              // invokeinterface
            lengths[0xba] = 5;                              // invokedynamic
            lengths[0xbb] = 3;                              // new
            lengths[0xbc] = 2;                              // newarray
            lengths[0xbd] = 3;                              // anewarray
            lengths[0xc0] = 3;                              // checkcast
            lengths[0xc1] = 3;                              // instanceof
            lengths[0xc4] = 0;                              // wide
            lengths[0xc5] = 4;                              // multianewarray
            lengths[0xc6] = 3;                              // ifnull
            lengths[0xc7] = 3;                              // ifnonnull
            lengths[0xc8] = 5;                              // goto_w
            lengths[0xc9] = 5;                              // jsr_w
            return lengths;
        }

        private String className(int classIndex) {
            if (tags[classIndex] != 7) {
                throw new IllegalArgumentException("Constant-pool entry " + classIndex + " is not a class");
            }
            return utf8((Integer) pool[classIndex]).replace('/', '.');
        }

        private String utf8(int index) {
            if (tags[index] != 1) {
                throw new IllegalArgumentException("Constant-pool entry " + index + " is not a Utf8 entry");
            }
            return (String) pool[index];
        }
    }
}
