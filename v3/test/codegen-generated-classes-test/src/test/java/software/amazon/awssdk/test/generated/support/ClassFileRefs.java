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

package software.amazon.awssdk.test.generated.support;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * The symbolic references in a class file's constant pool (JVMS 4.4): class names, field and method references, and
 * the members that {@code CONSTANT_MethodHandle} entries reference. A linkage fixture uses it to check that the V2
 * caller javac compiled references exactly the manifest entry's owner, name and JVM descriptor, and that a method or
 * constructor is referenced through a method handle (so running the caller links it), not through a synthetic lambda.
 *
 * @param classes      binary names of every {@code CONSTANT_Class}
 * @param fields       every {@code CONSTANT_Fieldref}
 * @param methods      every {@code CONSTANT_Methodref} and {@code CONSTANT_InterfaceMethodref}
 * @param handleTargets the field and method references of every {@code CONSTANT_MethodHandle}
 */
public record ClassFileRefs(Set<String> classes, Set<Ref> fields, Set<Ref> methods, Set<Ref> handleTargets) {
    private static final Comparator<Ref> ORDER =
        Comparator.comparing(Ref::owner).thenComparing(Ref::name).thenComparing(Ref::descriptor);

    /**
     * One member reference.
     *
     * @param owner      binary name of the referenced class
     * @param name       member name
     * @param descriptor JVM descriptor
     */
    public record Ref(String owner, String name, String descriptor) {
    }

    /** Reads the references of the class file at {@code path}. */
    public static ClassFileRefs read(Path path) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(Files.readAllBytes(path)));
        if (in.readInt() != 0xCAFEBABE) {
            throw new IOException(path + " is not a class file");
        }
        in.readUnsignedShort(); // minor_version
        in.readUnsignedShort(); // major_version
        int count = in.readUnsignedShort();
        int[] tags = new int[count];
        int[] first = new int[count];
        int[] second = new int[count];
        String[] utf8 = new String[count];
        for (int i = 1; i < count; i++) {
            int tag = in.readUnsignedByte();
            tags[i] = tag;
            switch (tag) {
                case 1 -> utf8[i] = in.readUTF();
                case 3, 4 -> in.skipNBytes(4);
                case 5, 6 -> {
                    in.skipNBytes(8);
                    i++;
                }
                case 7, 8, 16, 19, 20 -> first[i] = in.readUnsignedShort();
                case 9, 10, 11, 12, 17, 18 -> {
                    first[i] = in.readUnsignedShort();
                    second[i] = in.readUnsignedShort();
                }
                case 15 -> {
                    first[i] = in.readUnsignedByte();
                    second[i] = in.readUnsignedShort();
                }
                default -> throw new IOException("Unknown constant-pool tag " + tag + " at " + i + " in " + path);
            }
        }
        Set<String> classes = new TreeSet<>();
        Set<Ref> fields = new TreeSet<>(ORDER);
        Set<Ref> methods = new TreeSet<>(ORDER);
        Set<Ref> handles = new TreeSet<>(ORDER);
        for (int i = 1; i < count; i++) {
            switch (tags[i]) {
                case 7 -> classes.add(className(utf8, first[i]));
                case 9 -> fields.add(ref(i, utf8, first, second));
                case 10, 11 -> methods.add(ref(i, utf8, first, second));
                case 15 -> handles.add(ref(second[i], utf8, first, second));
                default -> {
                }
            }
        }
        return new ClassFileRefs(Collections.unmodifiableSet(classes), Collections.unmodifiableSet(fields),
                                 Collections.unmodifiableSet(methods), Collections.unmodifiableSet(handles));
    }

    private static Ref ref(int index, String[] utf8, int[] first, int[] second) {
        int classIndex = first[index];
        int nameAndType = second[index];
        return new Ref(className(utf8, first[classIndex]), utf8[first[nameAndType]], utf8[second[nameAndType]]);
    }

    private static String className(String[] utf8, int nameIndex) {
        return utf8[nameIndex].replace('/', '.');
    }
}
