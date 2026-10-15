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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * The traces that compiler options leave in class files: the class-file major versions ({@code --release}) and which
 * optional attributes are present ({@code -g} debug information, {@code -parameters}). Two JARs compiled with the
 * same options have the same fingerprint (Requirement 36.7).
 */
@SdkInternalApi
final class ClassFileFingerprint {
    private final SortedSet<Integer> majorVersions;
    private final boolean sourceFile;
    private final boolean lineNumbers;
    private final boolean localVariables;
    private final boolean methodParameters;

    private ClassFileFingerprint(SortedSet<Integer> majorVersions, boolean sourceFile, boolean lineNumbers,
                                 boolean localVariables, boolean methodParameters) {
        this.majorVersions = Collections.unmodifiableSortedSet(majorVersions);
        this.sourceFile = sourceFile;
        this.lineNumbers = lineNumbers;
        this.localVariables = localVariables;
        this.methodParameters = methodParameters;
    }

    /**
     * Fingerprint a set of class files.
     */
    static ClassFileFingerprint of(Collection<byte[]> classFiles) {
        SortedSet<Integer> majors = new TreeSet<>();
        Scanner scanner = new Scanner();
        for (byte[] classFile : classFiles) {
            majors.add(((classFile[6] & 0xFF) << 8) | (classFile[7] & 0xFF));
            new ClassReader(classFile).accept(scanner, ClassReader.SKIP_FRAMES);
        }
        return new ClassFileFingerprint(majors, scanner.sourceFile, scanner.lineNumbers, scanner.localVariables,
                                        scanner.methodParameters);
    }

    /**
     * A one-line description for the report.
     */
    String describe() {
        List<String> versions = new ArrayList<>();
        for (int major : majorVersions) {
            versions.add(major + " (Java " + (major - 44) + ")");
        }
        List<String> debug = new ArrayList<>();
        if (sourceFile) {
            debug.add("source");
        }
        if (lineNumbers) {
            debug.add("lines");
        }
        if (localVariables) {
            debug.add("vars");
        }
        return "class-file version " + String.join(", ", versions)
               + "; debug attributes: " + (debug.isEmpty() ? "none" : String.join(", ", debug))
               + "; MethodParameters: " + (methodParameters ? "yes" : "no");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClassFileFingerprint that = (ClassFileFingerprint) o;
        return sourceFile == that.sourceFile
               && lineNumbers == that.lineNumbers
               && localVariables == that.localVariables
               && methodParameters == that.methodParameters
               && majorVersions.equals(that.majorVersions);
    }

    @Override
    public int hashCode() {
        int result = majorVersions.hashCode();
        result = 31 * result + Boolean.hashCode(sourceFile);
        result = 31 * result + Boolean.hashCode(lineNumbers);
        result = 31 * result + Boolean.hashCode(localVariables);
        result = 31 * result + Boolean.hashCode(methodParameters);
        return result;
    }

    @Override
    public String toString() {
        return describe();
    }

    private static final class Scanner extends ClassVisitor {
        private boolean sourceFile;
        private boolean lineNumbers;
        private boolean localVariables;
        private boolean methodParameters;
        private final MethodVisitor methodScanner = new MethodVisitor(Opcodes.ASM9) {
            @Override
            public void visitParameter(String name, int access) {
                methodParameters = true;
            }

            @Override
            public void visitLineNumber(int line, Label start) {
                lineNumbers = true;
            }

            @Override
            public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end,
                                           int index) {
                localVariables = true;
            }
        };

        Scanner() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visitSource(String source, String debug) {
            if (source != null) {
                sourceFile = true;
            }
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                         String[] exceptions) {
            return methodScanner;
        }
    }
}
