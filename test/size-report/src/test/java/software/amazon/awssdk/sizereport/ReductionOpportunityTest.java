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

import static org.assertj.core.api.Assertions.assertThat;
import static org.objectweb.asm.Opcodes.ACC_BRIDGE;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_8;
import static software.amazon.awssdk.sizereport.SyntheticClasses.CLIENT;
import static software.amazon.awssdk.sizereport.SyntheticClasses.SERVICE;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

class ReductionOpportunityTest {
    private static final String SCHEMA_GETTER = "()Lsoftware/amazon/awssdk/schema/Schema;";
    private static final String DESERIALIZE = "(Lsoftware/amazon/awssdk/schema/serde/ShapeDeserializer;)";
    private static final String BAR = SERVICE + "/model/Bar";
    private static final String BAR_BUILDER = BAR + "$BuilderImpl";
    private static final String INNER_DESERIALIZER = BAR_BUILDER + "$InnerDeserializer";
    private static final String SHARED_SERDE = SERVICE + "/model/FooSharedSerde";
    private static final String LIST_SERDE = SHARED_SERDE + "$BarListSerde";
    private static final String MARSHALLER = SERVICE + "/transform/GetBarRequestMarshaller";

    @Test
    void measureAll_sharedSerdeNestedClass_removesTheWholeClassButNotTheOuterClass() {
        Map<ReductionOpportunity, ReductionOpportunity.Measurement> measured = measure(classes());

        ReductionOpportunity.Measurement serde = measured.get(ReductionOpportunity.GENERIC_COLLECTION_SERDE);
        assertThat(serde.classes()).isEqualTo(1);
        assertThat(serde.classBytes()).isEqualTo(classes().get(LIST_SERDE).length);
        // A JAR built without entry sizes stands in the class-file size for the compressed size.
        assertThat(serde.jarBytes()).isEqualTo(classes().get(LIST_SERDE).length);
    }

    @Test
    void measureAll_modelClassMembers_carvesOnlyTheSelectedMethods() {
        Map<ReductionOpportunity, ReductionOpportunity.Measurement> measured = measure(classes());

        ReductionOpportunity.Measurement accessors = measured.get(ReductionOpportunity.PACKAGE_PRIVATE_MEMBER_SCHEMAS);
        ReductionOpportunity.Measurement bridges = measured.get(ReductionOpportunity.NO_SHAPE_BUILDER_BRIDGES);
        ReductionOpportunity.Measurement equals = measured.get(ReductionOpportunity.GENERIC_EQUALS_HASHCODE);
        assertThat(accessors.classes()).isEqualTo(1);
        assertThat(bridges.classes()).isEqualTo(1);
        assertThat(equals.classes()).isEqualTo(1);
        assertThat(accessors.classBytes()).isPositive();
        assertThat(bridges.classBytes()).isPositive();
        assertThat(accessors.classBytes() + equals.classBytes()).isLessThan(classes().get(BAR).length);
        assertThat(bridges.classBytes()).isLessThan(classes().get(BAR_BUILDER).length);
    }

    @Test
    void measureAll_innerDeserializer_keepsTheAcceptMethodThatMovesIntoTheBuilder() {
        Map<ReductionOpportunity, ReductionOpportunity.Measurement> measured = measure(classes());

        ReductionOpportunity.Measurement inner = measured.get(ReductionOpportunity.INNER_DESERIALIZER_IN_BUILDER);
        assertThat(inner.classes()).isEqualTo(1);
        assertThat(inner.classBytes()).isPositive().isLessThan(classes().get(INNER_DESERIALIZER).length);
        // The class is selected whole, so its bridge deserialize method is not measured again by row 4.
        assertThat(measured.get(ReductionOpportunity.NO_SHAPE_BUILDER_BRIDGES).classes()).isEqualTo(1);
    }

    @Test
    void measureAll_marshallerAndExceptionMapping_countAsLegacyPathRemainder() {
        Map<ReductionOpportunity, ReductionOpportunity.Measurement> measured = measure(classes());

        ReductionOpportunity.Measurement legacy = measured.get(ReductionOpportunity.LEGACY_PATH_REMAINDER);
        long mapping = SyntheticClasses.client(true).length - SyntheticClasses.client(false).length;
        assertThat(legacy.classes()).isEqualTo(2);
        assertThat(legacy.classBytes()).isEqualTo(classes().get(MARSHALLER).length + mapping);
    }

    @Test
    void measureAll_noSuchCode_measuresNothing() {
        Map<String, byte[]> classes = new TreeMap<>();
        classes.put(CLIENT, SyntheticClasses.client(false));

        Map<ReductionOpportunity, ReductionOpportunity.Measurement> measured = measure(classes);

        assertThat(measured).containsOnlyKeys(ReductionOpportunity.values());
        assertThat(measured.values()).allSatisfy(m -> assertThat(m.classes()).isZero());
    }

    @Test
    void estimate_paginatorRange_takesTwentyToFortyPercent() {
        ReductionOpportunity.Measurement measurement = new ReductionOpportunity.Measurement();
        measurement.addClass(2_000, 1_000);

        assertThat(ReductionOpportunity.Estimate.PAGINATOR_RANGE.low(measurement)).isEqualTo(200);
        assertThat(ReductionOpportunity.Estimate.PAGINATOR_RANGE.high(measurement)).isEqualTo(400);
        assertThat(ReductionOpportunity.Estimate.FULL.low(measurement)).isEqualTo(1_000);
        assertThat(ReductionOpportunity.Estimate.FULL.high(measurement)).isEqualTo(1_000);
    }

    private static Map<ReductionOpportunity, ReductionOpportunity.Measurement> measure(Map<String, byte[]> classes) {
        JarContents jar = new JarContents(1, classes, Collections.emptyMap(), "21");
        return ReductionOpportunity.measureAll(jar, ServiceSizes.measure(jar));
    }

    private static Map<String, byte[]> classes() {
        Map<String, byte[]> classes = new TreeMap<>();
        classes.put(CLIENT, SyntheticClasses.client(true));
        classes.put(MARSHALLER, SyntheticClasses.plain(MARSHALLER, 3));
        classes.put(SHARED_SERDE, SyntheticClasses.plain(SHARED_SERDE, 1));
        classes.put(LIST_SERDE, SyntheticClasses.plain(LIST_SERDE, 4));
        classes.put(BAR, modelClass(BAR, "java/lang/Object", ReductionOpportunity.SERIALIZABLE_STRUCT,
                                    new Method(ACC_STATIC | ACC_SYNTHETIC, "access$100", SCHEMA_GETTER),
                                    new Method(ACC_PUBLIC | ACC_FINAL, "equals", "(Ljava/lang/Object;)Z"),
                                    new Method(ACC_PUBLIC, "toString", "()Ljava/lang/String;")));
        classes.put(BAR_BUILDER, modelClass(BAR_BUILDER, "java/lang/Object", ReductionOpportunity.SHAPE_BUILDER,
                                            new Method(ACC_PUBLIC, "deserialize", DESERIALIZE + "L" + BAR_BUILDER + ";"),
                                            new Method(ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC, "deserialize",
                                                       DESERIALIZE + "L" + ReductionOpportunity.SHAPE_BUILDER + ";")));
        String accept = "(L" + BAR_BUILDER + ";Lsoftware/amazon/awssdk/schema/Schema;"
                        + "Lsoftware/amazon/awssdk/schema/serde/ShapeDeserializer;)V";
        String bridgeAccept = "(Ljava/lang/Object;Lsoftware/amazon/awssdk/schema/Schema;"
                              + "Lsoftware/amazon/awssdk/schema/serde/ShapeDeserializer;)V";
        classes.put(INNER_DESERIALIZER, modelClass(INNER_DESERIALIZER, "java/lang/Object", null,
                                                   new Method(ACC_PUBLIC, "accept", accept),
                                                   new Method(ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC, "deserialize",
                                                              bridgeAccept)));
        return classes;
    }

    private static byte[] modelClass(String name, String superName, String iface, Method... methods) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V1_8, ACC_PUBLIC | ACC_FINAL, name, null, superName, iface == null ? null : new String[] {iface});
        for (Method method : methods) {
            MethodVisitor mv = cw.visitMethod(method.access, method.name, method.desc, null, null);
            mv.visitCode();
            mv.visitLdcInsn(method.name + "-body");
            mv.visitInsn(POP);
            Type returnType = Type.getReturnType(method.desc);
            if (returnType.getSort() == Type.VOID) {
                mv.visitInsn(RETURN);
            } else if (returnType.getSort() == Type.BOOLEAN) {
                mv.visitInsn(ICONST_0);
                mv.visitInsn(IRETURN);
            } else {
                mv.visitInsn(ACONST_NULL);
                mv.visitInsn(ARETURN);
            }
            mv.visitMaxs(1, Type.getArgumentsAndReturnSizes(method.desc) >> 2);
            mv.visitEnd();
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static final class Method {
        private final int access;
        private final String name;
        private final String desc;

        Method(int access, String name, String desc) {
            this.access = access;
            this.name = name;
            this.desc = desc;
        }
    }
}
