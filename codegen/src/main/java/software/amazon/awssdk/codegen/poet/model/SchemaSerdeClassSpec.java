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

package software.amazon.awssdk.codegen.poet.model;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.poet.schema.SchemaSerdeModel;
import software.amazon.awssdk.codegen.poet.schema.SchemaSerdeModel.Entry;
import software.amazon.awssdk.codegen.poet.schema.SchemaSerdeModel.MemberBinding;
import software.amazon.awssdk.codegen.poet.schema.SharedSerdeSpec;
import software.amazon.awssdk.codegen.poet.schema.ValueKind;
import software.amazon.awssdk.codegen.schema.SchemaRuntimeType;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;

/**
 * The schema serde members of a generated model class and its builder (design A.6, task 3.7): the {@code SCHEMA} and
 * member schema constants, {@code sdkSchema()}, {@code serializeMembers}, {@code getMemberValue},
 * {@code newShapeBuilder()}, schema-derived {@code sdkFields()} and {@code toString()}, and on the builder
 * {@code sdkSchema()}, {@code deserialize}, {@code deserializeMember}, {@code setMemberValue} and the
 * {@code InnerDeserializer}.
 *
 * <p>Members are handled in member index order with {@code switch} statements on {@code Schema.memberIndex()}. Builder
 * fields are package-private so that the {@code InnerDeserializer}, a nested class of the builder, and the class
 * constructor read and write them without synthetic accessors; the {@code InnerDeserializer} assigns deserialized
 * values, including the unmodifiable collections of {@code <Svc>SharedSerde}, directly to the fields, except for unions,
 * whose fluent setters track the set member, and for the message of an exception.
 */
final class SchemaSerdeClassSpec {
    static final String SCHEMA_FIELD = "SCHEMA";

    private static final ClassName STRUCT_MEMBER_CONSUMER =
        SchemaRuntimeType.SHAPE_DESERIALIZER.nestedClass("StructMemberConsumer");
    private static final String SERIALIZER = "serializer";
    private static final String DESERIALIZER = "deserializer";
    private static final String MEMBER = "member";
    private static final String BUILDER = "builder";

    private final Entry entry;
    private final SchemaSerdeModel serde;
    private final SharedSerdeSpec sharedSerde;
    private final TypeProvider typeProvider;
    private final ShapeModel shape;
    private final ClassName className;
    private final ClassName builderImpl;

    SchemaSerdeClassSpec(IntermediateModel model, SchemaSerdeModel serde, Entry entry) {
        this.entry = entry;
        this.serde = serde;
        this.sharedSerde = new SharedSerdeSpec(serde, model);
        this.typeProvider = new TypeProvider(model);
        this.shape = entry.shape();
        this.className = entry.className();
        this.builderImpl = className.nestedClass("BuilderImpl");
    }

    /**
     * @return the generated class's schema serde description if it has one, else null
     */
    static SchemaSerdeClassSpec forShape(IntermediateModel model, SchemaSerdeModel serde, ShapeModel shape) {
        if (serde == null || !serde.isEnabled()) {
            return null;
        }
        Entry entry = serde.entry(shape.getShapeName());
        return entry == null ? null : new SchemaSerdeClassSpec(model, serde, entry);
    }

    TypeName serializableStruct() {
        return SchemaRuntimeType.SERIALIZABLE_STRUCT;
    }

    TypeName shapeBuilder() {
        return ParameterizedTypeName.get(SchemaRuntimeType.SHAPE_BUILDER, className);
    }

    /**
     * The {@code SCHEMA} and member schema constants.
     */
    List<FieldSpec> staticFields() {
        List<FieldSpec> fields = new ArrayList<>();
        fields.add(FieldSpec.builder(SchemaRuntimeType.SCHEMA, SCHEMA_FIELD, PUBLIC, STATIC, FINAL)
                            .addJavadoc("The schema of this shape.\n")
                            .initializer(schemaInitializer())
                            .build());
        for (MemberBinding member : entry.members()) {
            fields.add(FieldSpec.builder(SchemaRuntimeType.SCHEMA, member.constantName(), PRIVATE, STATIC, FINAL)
                                .initializer("$N.member($S)", SCHEMA_FIELD, member.memberName())
                                .build());
        }
        return fields;
    }

    private CodeBlock schemaInitializer() {
        if (!entry.isSynthetic()) {
            return serde.schemaReference(entry);
        }
        return CodeBlock.builder()
                        .add("$T.structureBuilder($T.fromParts($S, $S), $T.INSTANCE)", SchemaRuntimeType.SCHEMA,
                             SchemaRuntimeType.SHAPE_ID, serde.serviceNamespace(), className.simpleName(),
                             SchemaRuntimeType.UNIT_TYPE_TRAIT)
                        .add(".builderSupplier($T::newShapeBuilder).shapeClass($T.class).build()", className, className)
                        .build();
    }

    /**
     * The methods of the class: {@code sdkSchema}, {@code serializeMembers}, {@code getMemberValue} and
     * {@code newShapeBuilder}.
     */
    List<MethodSpec> classMethods() {
        List<MethodSpec> methods = new ArrayList<>();
        methods.add(sdkSchemaMethod());
        methods.add(serializeMembersMethod());
        methods.add(getMemberValueMethod());
        methods.add(MethodSpec.methodBuilder("newShapeBuilder")
                              .addModifiers(STATIC)
                              .returns(shapeBuilder())
                              .addStatement("return new $T()", builderImpl)
                              .build());
        return methods;
    }

    private MethodSpec sdkSchemaMethod() {
        return MethodSpec.methodBuilder("sdkSchema")
                         .addAnnotation(Override.class)
                         .addModifiers(PUBLIC)
                         .returns(SchemaRuntimeType.SCHEMA)
                         .addStatement("return $N", SCHEMA_FIELD)
                         .build();
    }

    private MethodSpec serializeMembersMethod() {
        MethodSpec.Builder method = MethodSpec.methodBuilder("serializeMembers")
                                              .addAnnotation(Override.class)
                                              .addModifiers(PUBLIC)
                                              .addParameter(SchemaRuntimeType.SHAPE_SERIALIZER, SERIALIZER);
        for (MemberBinding binding : entry.members()) {
            CodeBlock schema = CodeBlock.of("$N", binding.constantName());
            MemberModel member = binding.member();
            if (member == null) {
                method.beginControlFlow("if (rawMessage() != null)")
                      .addStatement("$N.writeString($L, rawMessage())", SERIALIZER, schema)
                      .endControlFlow();
                continue;
            }
            ValueKind kind = ValueKind.of(member);
            CodeBlock value = CodeBlock.of("this.$N", member.getVariable().getVariableName());
            if (kind == ValueKind.LIST || kind == ValueKind.MAP) {
                method.beginControlFlow("if ($L != null && !($L instanceof $T))", value, value,
                                        kind == ValueKind.LIST ? SdkAutoConstructList.class : SdkAutoConstructMap.class);
            } else {
                method.beginControlFlow("if ($L != null)", value);
            }
            method.addStatement("$L", kind.write(SERIALIZER, schema, value, sharedSerde.writerOf(kind, member)))
                  .endControlFlow();
        }
        return method.build();
    }

    private MethodSpec getMemberValueMethod() {
        TypeVariableName t = TypeVariableName.get("T");
        MethodSpec.Builder method = MethodSpec.methodBuilder("getMemberValue")
                                              .addAnnotation(Override.class)
                                              .addAnnotation(unchecked())
                                              .addModifiers(PUBLIC)
                                              .addTypeVariable(t)
                                              .returns(t)
                                              .addParameter(SchemaRuntimeType.SCHEMA, MEMBER);
        if (entry.members().isEmpty()) {
            return method.addStatement("throw $T.notAMember($N, $N)", SchemaRuntimeType.SCHEMA_UTILS, SCHEMA_FIELD, MEMBER)
                         .build();
        }
        method.beginControlFlow("switch ($N.memberIndex())", MEMBER);
        for (MemberBinding binding : entry.members()) {
            CodeBlock value = binding.member() == null
                              ? CodeBlock.of("rawMessage()")
                              : CodeBlock.of("this.$N", binding.member().getVariable().getVariableName());
            method.addCode("case $L:\n", binding.index())
                  .addStatement("$>return ($T) $T.validateSameMember($N, $N, $L)$<", t, SchemaRuntimeType.SCHEMA_UTILS,
                                binding.constantName(), MEMBER, value);
        }
        method.addCode("default:\n")
              .addStatement("$>throw $T.notAMember($N, $N)$<", SchemaRuntimeType.SCHEMA_UTILS, SCHEMA_FIELD, MEMBER)
              .endControlFlow();
        return method.build();
    }

    /**
     * {@code sdkFields()} of the class or its builder, derived from the schema.
     */
    MethodSpec sdkFieldsMethod() {
        ParameterizedTypeName sdkField = ParameterizedTypeName.get(ClassName.get(SdkField.class),
                                                                   WildcardTypeName.subtypeOf(Object.class));
        return MethodSpec.methodBuilder("sdkFields")
                         .addModifiers(PUBLIC)
                         .addAnnotation(Override.class)
                         .returns(ParameterizedTypeName.get(ClassName.get(List.class), sdkField))
                         .addStatement("return $T.$N.fields($N)", serde.sharedSerdeClass(), sdkFieldsSettings(),
                                       SCHEMA_FIELD)
                         .build();
    }

    /**
     * {@code sdkFieldNameToField()} of the class or its builder, derived from the schema.
     */
    MethodSpec sdkFieldNameToFieldMethod() {
        ParameterizedTypeName sdkField = ParameterizedTypeName.get(ClassName.get(SdkField.class),
                                                                   WildcardTypeName.subtypeOf(Object.class));
        return MethodSpec.methodBuilder("sdkFieldNameToField")
                         .addModifiers(PUBLIC)
                         .addAnnotation(Override.class)
                         .returns(ParameterizedTypeName.get(ClassName.get(Map.class), ClassName.get(String.class),
                                                            sdkField))
                         .addStatement("return $T.$N.fieldsByName($N)", serde.sharedSerdeClass(), sdkFieldsSettings(),
                                       SCHEMA_FIELD)
                         .build();
    }

    private String sdkFieldsSettings() {
        if (!serde.honorsHttpBindings()) {
            return SharedSerdeSpec.SDK_FIELDS;
        }
        switch (shape.getShapeType()) {
            case Request:
                return SharedSerdeSpec.REQUEST_SDK_FIELDS;
            case Response:
            case Exception:
                return SharedSerdeSpec.RESPONSE_SDK_FIELDS;
            default:
                return SharedSerdeSpec.SDK_FIELDS;
        }
    }

    /**
     * {@code toString()} through {@code ToStringSerializer}, which writes the format of the previous generated
     * {@code toString} methods.
     */
    CodeBlock toStringBody() {
        return CodeBlock.of("return $T.serialize(this);\n", SchemaRuntimeType.TO_STRING_SERIALIZER);
    }

    /**
     * Whether {@code toString()} delegates to {@code ToStringSerializer}. Not for a class with a sensitive list or map
     * member: the previous generated {@code toString} wrote the redaction placeholder for such a member even when it
     * was never set, while {@code serializeMembers} does not write an unset collection, so the class keeps the
     * previous {@code toString} to keep its output identical.
     */
    boolean usesToStringSerializer() {
        for (MemberModel member : shape.getNonStreamingMembers()) {
            if (member.isSensitive() && member.getAutoConstructClassIfExists().isPresent()) {
                return false;
            }
        }
        return true;
    }

    /**
     * The builder methods: {@code sdkSchema}, {@code deserialize}, {@code deserializeMember} and
     * {@code setMemberValue}.
     */
    List<MethodSpec> builderMethods() {
        List<MethodSpec> methods = new ArrayList<>();
        methods.add(sdkSchemaMethod());
        methods.add(MethodSpec.methodBuilder("deserialize")
                              .addAnnotation(Override.class)
                              .addModifiers(PUBLIC)
                              .returns(builderImpl)
                              .addParameter(SchemaRuntimeType.SHAPE_DESERIALIZER, DESERIALIZER)
                              .addStatement("$N.readStruct($N, this, InnerDeserializer.INSTANCE)", DESERIALIZER,
                                            SCHEMA_FIELD)
                              .addStatement("return this")
                              .build());
        methods.add(MethodSpec.methodBuilder("deserializeMember")
                              .addAnnotation(Override.class)
                              .addModifiers(PUBLIC)
                              .returns(builderImpl)
                              .addParameter(SchemaRuntimeType.SHAPE_DESERIALIZER, DESERIALIZER)
                              .addParameter(SchemaRuntimeType.SCHEMA, "schema")
                              .addStatement("$N.readStruct(schema.assertMemberTargetIs($N), this, "
                                            + "InnerDeserializer.INSTANCE)", DESERIALIZER, SCHEMA_FIELD)
                              .addStatement("return this")
                              .build());
        methods.add(setMemberValueMethod());
        return methods;
    }

    private MethodSpec setMemberValueMethod() {
        MethodSpec.Builder method = MethodSpec.methodBuilder("setMemberValue")
                                              .addAnnotation(Override.class)
                                              .addModifiers(PUBLIC)
                                              .addParameter(SchemaRuntimeType.SCHEMA, MEMBER)
                                              .addParameter(Object.class, "value");
        if (entry.members().isEmpty()) {
            return method.addStatement("throw $T.notAMember($N, $N)", SchemaRuntimeType.SCHEMA_UTILS, SCHEMA_FIELD, MEMBER)
                         .build();
        }
        boolean unchecked = false;
        method.beginControlFlow("switch ($N.memberIndex())", MEMBER);
        for (MemberBinding binding : entry.members()) {
            MemberModel member = binding.member();
            String setter = member == null ? "message" : member.getFluentSetterMethodName();
            TypeName type = member == null ? ClassName.get(String.class) : typeProvider.parameterType(member);
            unchecked |= type instanceof ParameterizedTypeName;
            method.addCode("case $L:\n", binding.index())
                  .addStatement("$>$N(($T) $T.validateSameMember($N, $N, value))", setter, type,
                                SchemaRuntimeType.SCHEMA_UTILS, binding.constantName(), MEMBER)
                  .addStatement("break$<");
        }
        method.addCode("default:\n")
              .addStatement("$>throw $T.notAMember($N, $N)$<", SchemaRuntimeType.SCHEMA_UTILS, SCHEMA_FIELD, MEMBER)
              .endControlFlow();
        if (unchecked) {
            method.addAnnotation(unchecked());
        }
        return method.build();
    }

    /**
     * The builder's {@code InnerDeserializer}: a stateless {@code StructMemberConsumer} that sets the member a
     * deserializer reads, selected by member index.
     */
    TypeSpec innerDeserializer() {
        ClassName self = builderImpl.nestedClass("InnerDeserializer");
        MethodSpec.Builder accept = MethodSpec.methodBuilder("accept")
                                              .addAnnotation(Override.class)
                                              .addModifiers(PUBLIC)
                                              .addParameter(builderImpl, BUILDER)
                                              .addParameter(SchemaRuntimeType.SCHEMA, MEMBER)
                                              .addParameter(SchemaRuntimeType.SHAPE_DESERIALIZER, DESERIALIZER);
        if (!entry.members().isEmpty()) {
            accept.beginControlFlow("switch ($N.memberIndex())", MEMBER);
            for (MemberBinding binding : entry.members()) {
                accept.addCode("case $L:\n", binding.index())
                      .addStatement("$>$L", assignment(binding))
                      .addStatement("break$<");
            }
            accept.addCode("default:\n")
                  .addStatement("$>break$<")
                  .endControlFlow();
        }
        return TypeSpec.classBuilder(self)
                       .addModifiers(PRIVATE, STATIC, FINAL)
                       .addSuperinterface(ParameterizedTypeName.get(STRUCT_MEMBER_CONSUMER, builderImpl))
                       .addField(FieldSpec.builder(self, "INSTANCE", STATIC, FINAL)
                                          .initializer("new $T()", self)
                                          .build())
                       .addMethod(MethodSpec.constructorBuilder().addModifiers(PRIVATE).build())
                       .addMethod(accept.build())
                       .build();
    }

    private CodeBlock assignment(MemberBinding binding) {
        MemberModel member = binding.member();
        CodeBlock schema = CodeBlock.of("$N", MEMBER);
        if (member == null) {
            return CodeBlock.of("$N.message($N.readString($L))", BUILDER, DESERIALIZER, schema);
        }
        ValueKind kind = ValueKind.of(member);
        CodeBlock value = kind.read(DESERIALIZER, schema,
                                    kind == ValueKind.STRUCTURE ? typeProvider.fieldType(member) : null,
                                    sharedSerde.readerOf(kind, member));
        if (shape.isUnion()) {
            return CodeBlock.of("$N.$N($L)", BUILDER, member.getFluentSetterMethodName(), value);
        }
        return CodeBlock.of("$N.$N = $L", BUILDER, member.getVariable().getVariableName(), value);
    }

    private static AnnotationSpec unchecked() {
        return AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "$S", "unchecked").build();
    }
}
