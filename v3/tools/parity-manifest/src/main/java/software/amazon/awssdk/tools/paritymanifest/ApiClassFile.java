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

package software.amazon.awssdk.tools.paritymanifest;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * The API-relevant facts of one class file (JVMS chapter 4): binary name, access flags (taking the
 * {@code InnerClasses} entry of a nested class into account), enclosing class, direct superclass and
 * superinterfaces, annotations and the {@code Deprecated} attribute, and the fields and methods with their
 * descriptors, access flags and annotations.
 *
 * <p>Like the architecture tests' {@code ClassFile}, this is a deliberately minimal reader rather than a bytecode
 * library: JDK 21 has no supported class-file API and ASM rejects class-file versions newer than it knows. Only the
 * constant pool, member tables and the {@code InnerClasses}, {@code Deprecated}, {@code RuntimeVisibleAnnotations} and
 * {@code RuntimeInvisibleAnnotations} attributes are read; every other attribute is skipped by length.
 *
 * @param name        binary name ({@code a.b.Outer$Inner})
 * @param access      effective access flags; for a nested class, the {@code InnerClasses} flags
 * @param outerName   binary name of the declaring class of a member class, or {@code null}
 * @param local       true for a local or anonymous class
 * @param annotations descriptors of the class's annotations, e.g. {@code Ljava/lang/Deprecated;}
 * @param deprecated  true if the class carries {@code @Deprecated} or the {@code Deprecated} attribute
 * @param superName   binary name of the direct superclass, or {@code null} for {@code java.lang.Object} (and
 *                    {@code module-info})
 * @param interfaces  binary names of the direct superinterfaces, in declaration order
 */
public record ApiClassFile(String name, int access, String outerName, boolean local, Set<String> annotations,
                           boolean deprecated, List<Member> fields, List<Member> methods, String superName,
                           List<String> interfaces) {

    public static final int ACC_PUBLIC = 0x0001;
    public static final int ACC_PROTECTED = 0x0004;
    public static final int ACC_STATIC = 0x0008;
    public static final int ACC_FINAL = 0x0010;
    public static final int ACC_BRIDGE = 0x0040;
    public static final int ACC_INTERFACE = 0x0200;
    public static final int ACC_ABSTRACT = 0x0400;
    public static final int ACC_SYNTHETIC = 0x1000;
    public static final int ACC_ANNOTATION = 0x2000;
    public static final int ACC_ENUM = 0x4000;

    private static final String DEPRECATED = "Ljava/lang/Deprecated;";
    private static final int MAGIC = 0xCAFEBABE;

    /**
     * A field or method; {@code deprecated} is the member's own deprecation only, and {@code signature} its generic
     * {@code Signature} attribute (JVMS 4.7.9.1), or {@code null} when it has none.
     */
    public record Member(String name, String descriptor, int access, Set<String> annotations, boolean deprecated,
                         String signature) {

        /** True for a method that declares type parameters of its own ({@code <T> T get(Class<T>)}). */
        public boolean isGenericMethod() {
            return signature != null && signature.startsWith("<");
        }

        /** Public or protected, and neither synthetic nor a bridge method. */
        public boolean isApi() {
            return (access & (ACC_PUBLIC | ACC_PROTECTED)) != 0 && (access & (ACC_SYNTHETIC | ACC_BRIDGE)) == 0;
        }

        public boolean isProtected() {
            return (access & ACC_PROTECTED) != 0;
        }

        public boolean isStatic() {
            return (access & ACC_STATIC) != 0;
        }

        public boolean isFinal() {
            return (access & ACC_FINAL) != 0;
        }
    }

    /** Public or protected (by effective access), not synthetic, and not local or anonymous. */
    public boolean isApi() {
        return !local && (access & (ACC_PUBLIC | ACC_PROTECTED)) != 0 && (access & ACC_SYNTHETIC) == 0;
    }

    public boolean isFinal() {
        return (access & ACC_FINAL) != 0;
    }

    public boolean isAbstract() {
        return (access & ACC_ABSTRACT) != 0;
    }

    public boolean isProtected() {
        return (access & ACC_PROTECTED) != 0;
    }

    /** True for a member class without {@code static}: an inner class, whose instances need an enclosing instance. */
    public boolean isInner() {
        return outerName != null && (access & ACC_STATIC) == 0 && (access & ACC_INTERFACE) == 0;
    }

    /** {@code annotation}, {@code interface}, {@code enum} or {@code class}. */
    public String typeKind() {
        if ((access & ACC_ANNOTATION) != 0) {
            return "annotation";
        }
        if ((access & ACC_INTERFACE) != 0) {
            return "interface";
        }
        return (access & ACC_ENUM) != 0 ? "enum" : "class";
    }

    /** The package name, empty for the unnamed package. */
    public String packageName() {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(0, dot);
    }

    /** Parses a class file. */
    public static ApiClassFile parse(byte[] bytes) {
        try {
            return new Reader(new DataInputStream(new ByteArrayInputStream(bytes))).read();
        } catch (IOException e) {
            throw new UncheckedIOException("Truncated or malformed class file", e);
        }
    }

    private static final class Reader {
        private final DataInputStream in;
        private Object[] pool;
        private int[] tags;

        Reader(DataInputStream in) {
            this.in = in;
        }

        ApiClassFile read() throws IOException {
            if (in.readInt() != MAGIC) {
                throw new IllegalArgumentException("Not a class file (bad magic number)");
            }
            in.readUnsignedShort(); // minor_version
            in.readUnsignedShort(); // major_version
            readConstantPool();
            int access = in.readUnsignedShort();
            int thisIndex = in.readUnsignedShort();
            String name = className(thisIndex);
            int superIndex = in.readUnsignedShort();
            String superName = superIndex == 0 ? null : className(superIndex);
            if ("java.lang.Object".equals(superName)) {
                superName = null;
            }
            List<String> interfaces = new ArrayList<>();
            for (int i = in.readUnsignedShort(); i > 0; i--) {
                interfaces.add(className(in.readUnsignedShort()));
            }
            List<Member> fields = readMembers();
            List<Member> methods = readMembers();
            Set<String> annotations = new TreeSet<>();
            boolean deprecatedAttribute = false;
            String outerName = null;
            boolean local = false;
            for (int i = in.readUnsignedShort(); i > 0; i--) {
                String attribute = utf8(in.readUnsignedShort());
                int length = in.readInt();
                switch (attribute) {
                    case "InnerClasses" -> {
                        for (int c = in.readUnsignedShort(); c > 0; c--) {
                            int inner = in.readUnsignedShort();
                            int outer = in.readUnsignedShort();
                            int innerName = in.readUnsignedShort();
                            int innerAccess = in.readUnsignedShort();
                            if (inner != 0 && className(inner).equals(name)) {
                                access = innerAccess | (access & ACC_SYNTHETIC);
                                local = outer == 0 || innerName == 0;
                                outerName = outer == 0 ? null : className(outer);
                            }
                        }
                    }
                    case "EnclosingMethod" -> {
                        local = true;
                        in.skipNBytes(length);
                    }
                    case "Deprecated" -> deprecatedAttribute = true;
                    case "RuntimeVisibleAnnotations", "RuntimeInvisibleAnnotations" -> readAnnotations(annotations);
                    default -> in.skipNBytes(length);
                }
            }
            return new ApiClassFile(name, access, outerName, local, Set.copyOf(annotations),
                                    deprecatedAttribute || annotations.contains(DEPRECATED), fields, methods,
                                    superName, List.copyOf(interfaces));
        }

        private List<Member> readMembers() throws IOException {
            List<Member> result = new ArrayList<>();
            for (int count = in.readUnsignedShort(); count > 0; count--) {
                int access = in.readUnsignedShort();
                String name = utf8(in.readUnsignedShort());
                String descriptor = utf8(in.readUnsignedShort());
                Set<String> annotations = new TreeSet<>();
                boolean deprecatedAttribute = false;
                String signature = null;
                for (int i = in.readUnsignedShort(); i > 0; i--) {
                    String attribute = utf8(in.readUnsignedShort());
                    int length = in.readInt();
                    switch (attribute) {
                        case "Deprecated" -> deprecatedAttribute = true;
                        case "Signature" -> signature = utf8(in.readUnsignedShort());
                        case "RuntimeVisibleAnnotations", "RuntimeInvisibleAnnotations" -> readAnnotations(annotations);
                        default -> in.skipNBytes(length);
                    }
                }
                result.add(new Member(name, descriptor, access, Set.copyOf(annotations),
                                      deprecatedAttribute || annotations.contains(DEPRECATED), signature));
            }
            return List.copyOf(result);
        }

        /** Reads {@code num_annotations} annotations (JVMS 4.7.16), adding their type descriptors. */
        private void readAnnotations(Set<String> out) throws IOException {
            for (int i = in.readUnsignedShort(); i > 0; i--) {
                out.add(annotation());
            }
        }

        private String annotation() throws IOException {
            String type = utf8(in.readUnsignedShort());
            for (int pairs = in.readUnsignedShort(); pairs > 0; pairs--) {
                in.readUnsignedShort(); // element_name_index
                elementValue();
            }
            return type;
        }

        private void elementValue() throws IOException {
            int tag = in.readUnsignedByte();
            switch (tag) {
                case 'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z', 's', 'c' -> in.readUnsignedShort();
                case 'e' -> in.skipNBytes(4);
                case '@' -> annotation();
                case '[' -> {
                    for (int n = in.readUnsignedShort(); n > 0; n--) {
                        elementValue();
                    }
                }
                default -> throw new IllegalArgumentException("Unknown element_value tag " + (char) tag);
            }
        }

        private void readConstantPool() throws IOException {
            int count = in.readUnsignedShort();
            pool = new Object[count];
            tags = new int[count];
            for (int i = 1; i < count; i++) {
                int tag = in.readUnsignedByte();
                tags[i] = tag;
                switch (tag) {
                    case 1 -> pool[i] = in.readUTF();                           // Utf8
                    case 3, 4 -> in.skipNBytes(4);                               // Integer, Float
                    case 5, 6 -> {                                               // Long, Double: two slots
                        in.skipNBytes(8);
                        i++;
                    }
                    case 7, 8, 16, 19, 20 -> pool[i] = in.readUnsignedShort();   // Class, String, MethodType,
                                                                                 // Module, Package
                    case 9, 10, 11, 12, 17, 18 -> in.skipNBytes(4);              // refs, NameAndType, (Invoke)Dynamic
                    case 15 -> in.skipNBytes(3);                                 // MethodHandle
                    default -> throw new IllegalArgumentException("Unknown constant-pool tag " + tag + " at " + i);
                }
            }
        }

        private String className(int index) {
            if (tags[index] != 7) {
                throw new IllegalArgumentException("Constant-pool entry " + index + " is not a class");
            }
            return utf8((Integer) pool[index]).replace('/', '.');
        }

        private String utf8(int index) {
            if (tags[index] != 1) {
                throw new IllegalArgumentException("Constant-pool entry " + index + " is not a Utf8 entry");
            }
            return (String) pool[index];
        }
    }
}
