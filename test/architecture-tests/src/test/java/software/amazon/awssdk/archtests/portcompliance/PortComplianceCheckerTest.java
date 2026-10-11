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

package software.amazon.awssdk.archtests.portcompliance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import software.amazon.awssdk.archtests.portcompliance.fixtures.UnannotatedPublicPortFixture;

/**
 * Negative and positive fixtures for {@link PortComplianceChecker}. Fixtures that javac cannot produce under
 * {@code --release 8} (records, sealed types, Java 9+ API calls, Java 9+ class files) are generated with ASM.
 */
class PortComplianceCheckerTest {

    private static final String PKG = "fixture/port/";
    private static final String INTERNAL_API = "Lsoftware/amazon/awssdk/annotations/SdkInternalApi;";
    private static final String PUBLIC_API = "Lsoftware/amazon/awssdk/annotations/SdkPublicApi;";

    private static PortComplianceChecker checker;

    @TempDir
    Path classes;

    @BeforeAll
    static void loadSignature() throws IOException {
        try (InputStream in = Files.newInputStream(PortComplianceTest.java18Signature())) {
            checker = PortComplianceChecker.fromSignature(in);
        }
    }

    @Test
    void compliantClass_hasNoViolations() throws IOException {
        write("Good", Opcodes.V1_8, Opcodes.ACC_PUBLIC, "java/lang/Object", cw -> {
            cw.visitAnnotation(INTERNAL_API, false).visitEnd();
            // A Java 8 API call, a call into a third-party type and a call into the module's own package.
            method(cw, mv -> {
                mv.visitTypeInsn(Opcodes.NEW, "java/util/ArrayList");
                mv.visitInsn(Opcodes.DUP);
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
                mv.visitInsn(Opcodes.POP);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Optional", "empty", "()Ljava/util/Optional;",
                                   false);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/Optional", "isPresent", "()Z", false);
                mv.visitInsn(Opcodes.POP);
                mv.visitInsn(Opcodes.ACONST_NULL);
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/reactivestreams/Publisher", "subscribe",
                                   "(Lorg/reactivestreams/Subscriber;)V", true);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, PKG + "Helper", "help", "()V", false);
            });
        });
        write("Helper", Opcodes.V1_8, 0, "java/lang/Object", cw -> { });
        write("package-info", Opcodes.V1_8, Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT | Opcodes.ACC_SYNTHETIC,
              "java/lang/Object", cw -> { });

        assertThat(checker.check(classes)).isEmpty();
    }

    @Test
    void java9ApiCall_isReported() throws IOException {
        write("UsesListOf", Opcodes.V1_8, Opcodes.ACC_PUBLIC, "java/lang/Object", cw -> {
            cw.visitAnnotation(INTERNAL_API, false).visitEnd();
            method(cw, mv -> {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/List", "of", "()Ljava/util/List;", true);
                mv.visitInsn(Opcodes.POP);
                mv.visitLdcInsn("x");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "isBlank", "()Z", false);
                mv.visitInsn(Opcodes.POP);
            });
        });

        List<String> violations = checker.check(classes);
        assertThat(violations).hasSize(2);
        assertThat(violations).anySatisfy(v -> assertThat(v).contains("Java 9+ API").contains("java.util.List.of"));
        assertThat(violations).anySatisfy(v -> assertThat(v).contains("Java 9+ API").contains("isBlank"));
    }

    @Test
    void flowReference_isReported() throws IOException {
        write("UsesFlow", Opcodes.V1_8, Opcodes.ACC_PUBLIC, "java/lang/Object", cw -> {
            cw.visitAnnotation(INTERNAL_API, false).visitEnd();
            cw.visitField(Opcodes.ACC_PRIVATE, "publisher", "Ljava/util/concurrent/Flow$Publisher;", null, null)
              .visitEnd();
        });
        write("UsesFlowInMethodBody", Opcodes.V1_8, Opcodes.ACC_PUBLIC, "java/lang/Object", cw -> {
            cw.visitAnnotation(INTERNAL_API, false).visitEnd();
            method(cw, mv -> {
                mv.visitInsn(Opcodes.ACONST_NULL);
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/util/concurrent/Flow$Subscriber");
                mv.visitInsn(Opcodes.POP);
            });
        });

        assertThat(checker.check(classes))
            .anySatisfy(v -> assertThat(v).contains("fixture.port.UsesFlow:")
                                          .contains("java.util.concurrent.Flow$Publisher")
                                          .contains("org.reactivestreams"))
            .anySatisfy(v -> assertThat(v).contains("fixture.port.UsesFlowInMethodBody:")
                                          .contains("java.util.concurrent.Flow$Subscriber"));
    }

    @Test
    void record_isReported() throws IOException {
        write("ARecord", Opcodes.V16, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_RECORD, "java/lang/Record",
              cw -> {
                  cw.visitAnnotation(INTERNAL_API, false).visitEnd();
                  cw.visitRecordComponent("value", "I", null).visitEnd();
              });

        assertThat(checker.check(classes))
            .anySatisfy(v -> assertThat(v).contains("fixture.port.ARecord: is a record"))
            .anySatisfy(v -> assertThat(v).contains("fixture.port.ARecord: class file major version 60"));
    }

    @Test
    void sealedType_isReported() throws IOException {
        write("Sealed", Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "java/lang/Object", cw -> {
            cw.visitAnnotation(INTERNAL_API, false).visitEnd();
            cw.visitPermittedSubclass(PKG + "SealedImpl");
        });

        assertThat(checker.check(classes))
            .anySatisfy(v -> assertThat(v).contains("fixture.port.Sealed: is a sealed type"));
    }

    @Test
    void java9ClassFile_isReported() throws IOException {
        write("Java9", Opcodes.V9, Opcodes.ACC_PUBLIC, "java/lang/Object",
              cw -> cw.visitAnnotation(INTERNAL_API, false).visitEnd());

        assertThat(checker.check(classes))
            .containsExactly("fixture.port.Java9: class file major version 53 is above Java 8 (52)");
    }

    @Test
    void publicTopLevelTypeWithoutVisibilityAnnotation_isReported() throws IOException {
        write("Unannotated", Opcodes.V1_8, Opcodes.ACC_PUBLIC, "java/lang/Object", cw -> { });
        write("PackagePrivateUnannotated", Opcodes.V1_8, 0, "java/lang/Object", cw -> { });

        assertThat(checker.check(classes))
            .containsExactly("fixture.port.Unannotated: public top-level type has no SDK visibility annotation "
                             + "(@SdkPublicApi, @SdkProtectedApi or @SdkInternalApi)");
    }

    @Test
    void typeWithTwoVisibilityAnnotations_isReported() throws IOException {
        write("TwoAnnotations", Opcodes.V1_8, 0, "java/lang/Object", cw -> {
            cw.visitAnnotation(INTERNAL_API, false).visitEnd();
            cw.visitAnnotation(PUBLIC_API, false).visitEnd();
        });

        assertThat(checker.check(classes))
            .containsExactly("fixture.port.TwoAnnotations: has more than one SDK visibility annotation "
                             + "[SdkInternalApi, SdkPublicApi]");
    }

    @Test
    void javacCompiledUnannotatedPublicFixture_isReported_butItsNestedTypeIsNot() throws IOException {
        copyCompiled(UnannotatedPublicPortFixture.class);
        copyCompiled(UnannotatedPublicPortFixture.Nested.class);

        assertThat(checker.check(classes))
            .containsExactly(UnannotatedPublicPortFixture.class.getName()
                             + ": public top-level type has no SDK visibility annotation "
                             + "(@SdkPublicApi, @SdkProtectedApi or @SdkInternalApi)");
    }

    private void copyCompiled(Class<?> type) throws IOException {
        String resource = type.getName().replace('.', '/') + ".class";
        Path target = classes.resolve(resource);
        Files.createDirectories(target.getParent());
        try (InputStream in = type.getClassLoader().getResourceAsStream(resource)) {
            Files.copy(in, target);
        }
    }

    private void write(String simpleName, int version, int access, String superName, Consumer<ClassWriter> body)
        throws IOException {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(version, access, PKG + simpleName, null, superName, null);
        body.accept(cw);
        cw.visitEnd();
        Path target = classes.resolve(PKG + simpleName + ".class");
        Files.createDirectories(target.getParent());
        Files.write(target, cw.toByteArray());
    }

    private static void method(ClassWriter cw, Consumer<MethodVisitor> code) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "m", "()V", null, null);
        mv.visitCode();
        code.accept(mv);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
}
