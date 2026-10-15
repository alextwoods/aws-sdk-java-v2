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

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.H_INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 * Class files generated with ASM for the attribution tests. They only need to be well-formed enough for ASM to read
 * and write them; they are never loaded.
 */
final class SyntheticClasses {
    static final String SERVICE = "test/svc";
    static final String CLIENT = SERVICE + "/DefaultFooClient";
    static final String OPERATIONS = SERVICE + "/model/FooOperations";

    private static final String OPTIONAL_MAPPER = "(Ljava/lang/String;)Ljava/util/Optional;";
    private static final String EXCEPTION_METADATA = "Lsoftware/amazon/awssdk/protocols/core/ExceptionMetadata;";
    private static final String SUPPLIER = "()Ljava/util/function/Supplier;";
    private static final Handle METAFACTORY = new Handle(
        H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory",
        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
        + "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)"
        + "Ljava/lang/invoke/CallSite;", false);

    private SyntheticClasses() {
    }

    /**
     * A {@code DefaultFooClient}. With exception mapping it also has: a {@code lambda$op$0(String)Optional} error-code
     * mapper, a {@code createErrorResponseHandler} whose descriptor mentions {@code ExceptionMetadata}, and an
     * {@code init} that calls {@code ExceptionMetadata.builder()} and creates {@code lambda$init$1}.
     */
    static byte[] client(boolean withExceptionMapping) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_8, ACC_PUBLIC | ACC_FINAL, CLIENT, null, "java/lang/Object", null);
        cw.visitSource("DefaultFooClient.java", null);

        MethodVisitor op = cw.visitMethod(ACC_PUBLIC, "op", "(Ljava/lang/String;)Ljava/lang/Object;", null, null);
        op.visitCode();
        Label opStart = new Label();
        op.visitLabel(opStart);
        op.visitLineNumber(5, opStart);
        op.visitLdcInsn("operation-name");
        op.visitInsn(ARETURN);
        op.visitMaxs(1, 2);
        op.visitEnd();

        if (withExceptionMapping) {
            MethodVisitor mapper = cw.visitMethod(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC, "lambda$op$0",
                                                  OPTIONAL_MAPPER, null, null);
            mapper.visitCode();
            mapper.visitLdcInsn("ThrottlingException");
            mapper.visitInsn(POP);
            mapper.visitMethodInsn(INVOKESTATIC, "java/util/Optional", "empty", "()Ljava/util/Optional;", false);
            mapper.visitInsn(ARETURN);
            mapper.visitMaxs(1, 1);
            mapper.visitEnd();

            MethodVisitor handler = cw.visitMethod(ACC_PRIVATE, "createErrorResponseHandler",
                                                   "(" + EXCEPTION_METADATA + ")Ljava/lang/Object;", null, null);
            handler.visitCode();
            handler.visitInsn(ACONST_NULL);
            handler.visitInsn(ARETURN);
            handler.visitMaxs(1, 2);
            handler.visitEnd();

            MethodVisitor init = cw.visitMethod(ACC_PRIVATE, "init", "()Ljava/lang/Object;", null, null);
            init.visitCode();
            init.visitMethodInsn(INVOKESTATIC, "software/amazon/awssdk/protocols/core/ExceptionMetadata", "builder",
                                 "()Lsoftware/amazon/awssdk/protocols/core/ExceptionMetadata$Builder;", false);
            init.visitInsn(POP);
            init.visitInvokeDynamicInsn("get", SUPPLIER, METAFACTORY, Type.getType("()Ljava/lang/Object;"),
                                        new Handle(H_INVOKESTATIC, CLIENT, "lambda$init$1", "()Ljava/lang/Object;",
                                                   false),
                                        Type.getType("()Ljava/lang/Object;"));
            init.visitInsn(ARETURN);
            init.visitMaxs(1, 1);
            init.visitEnd();

            MethodVisitor initLambda = cw.visitMethod(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC, "lambda$init$1",
                                                      "()Ljava/lang/Object;", null, null);
            initLambda.visitCode();
            initLambda.visitLdcInsn("service-exception");
            initLambda.visitInsn(ARETURN);
            initLambda.visitMaxs(1, 0);
            initLambda.visitEnd();
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    /**
     * A {@code model/FooOperations} whose {@code <clinit>} initializes {@code NS}, then (with the registry)
     * {@code ERROR_REGISTRY} through {@code lambda$static$0}, then {@code OP}, which reads {@code ERROR_REGISTRY}.
     * Without the registry, the class is what carving the registry should leave: no {@code ERROR_REGISTRY} field,
     * segment or lambda, but {@code OP}'s initializer still reads the field.
     */
    static byte[] operations(boolean withRegistry) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_8, ACC_PUBLIC | ACC_FINAL, OPERATIONS, null, "java/lang/Object", null);
        cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "NS", "Ljava/lang/String;", null, null).visitEnd();
        if (withRegistry) {
            cw.visitField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, "ERROR_REGISTRY", ClassAttributor.ERROR_REGISTRY, null,
                          null).visitEnd();
        }
        cw.visitField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, "OP", "Ljava/lang/Object;", null, null).visitEnd();

        MethodVisitor clinit = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        clinit.visitCode();
        Label first = new Label();
        clinit.visitLabel(first);
        clinit.visitLineNumber(10, first);
        clinit.visitLdcInsn("test.ns");
        clinit.visitFieldInsn(PUTSTATIC, OPERATIONS, "NS", "Ljava/lang/String;");
        if (withRegistry) {
            Label registry = new Label();
            clinit.visitLabel(registry);
            clinit.visitLineNumber(11, registry);
            clinit.visitInvokeDynamicInsn("get", SUPPLIER, METAFACTORY, Type.getType("()Ljava/lang/Object;"),
                                          new Handle(H_INVOKESTATIC, OPERATIONS, "lambda$static$0",
                                                     "()Ljava/lang/Object;", false),
                                          Type.getType("()Ljava/lang/Object;"));
            clinit.visitMethodInsn(INVOKESTATIC, "software/amazon/awssdk/schema/serde/ErrorRegistry", "lazy",
                                   "(Ljava/util/function/Supplier;)" + ClassAttributor.ERROR_REGISTRY, false);
            clinit.visitFieldInsn(PUTSTATIC, OPERATIONS, "ERROR_REGISTRY", ClassAttributor.ERROR_REGISTRY);
        }
        Label op = new Label();
        clinit.visitLabel(op);
        clinit.visitLineNumber(12, op);
        clinit.visitFieldInsn(GETSTATIC, OPERATIONS, "ERROR_REGISTRY", ClassAttributor.ERROR_REGISTRY);
        clinit.visitFieldInsn(PUTSTATIC, OPERATIONS, "OP", "Ljava/lang/Object;");
        clinit.visitInsn(RETURN);
        clinit.visitMaxs(1, 0);
        clinit.visitEnd();

        if (withRegistry) {
            MethodVisitor lambda = cw.visitMethod(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC, "lambda$static$0",
                                                  "()Ljava/lang/Object;", null, null);
            lambda.visitCode();
            lambda.visitFieldInsn(GETSTATIC, SERVICE + "/model/FooSchemas", "THROTTLING", "Ljava/lang/Object;");
            lambda.visitInsn(ARETURN);
            lambda.visitMaxs(1, 0);
            lambda.visitEnd();
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    /**
     * A class with {@code count} static {@code String} fields, each with a constant value; its size grows with
     * {@code count}.
     */
    static byte[] plain(String internalName, int count) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_8, ACC_PUBLIC | ACC_FINAL, internalName, null, "java/lang/Object", null);
        for (int i = 0; i < count; i++) {
            cw.visitField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, "F" + i, "Ljava/lang/String;", null, "value" + i)
              .visitEnd();
        }
        cw.visitEnd();
        return cw.toByteArray();
    }
}
