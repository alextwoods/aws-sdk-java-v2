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

package software.amazon.awssdk.codegen.poet.schema;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.model.TypeProvider;
import software.amazon.awssdk.codegen.poet.schema.SchemaSerdeModel.CollectionSerde;
import software.amazon.awssdk.codegen.schema.SchemaRuntimeType;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.traits.SparseTrait;

/**
 * Generates {@code <Svc>SharedSerde}, the package-private class with the schema serde code that the model classes of a
 * service share (design A.6, task 3.7):
 * <ul>
 *     <li>the {@code SdkFieldsFromSchema} settings their {@code sdkFields()} use: {@code SDK_FIELDS}, plus
 *     {@code REQUEST_SDK_FIELDS} and {@code RESPONSE_SDK_FIELDS} for the HTTP bindings of a REST protocol;</li>
 *     <li>one nested class per list or map shape, with a static {@code INSTANCE} that is both the
 *     {@code ShapeSerializer.writeList}/{@code writeMap} writer and the element consumer of
 *     {@code ShapeDeserializer.readList}/{@code readStringMap}, and a static {@code read} method that returns the
 *     unmodifiable collection. Element schemas are static fields of the nested class, so no writer or reader captures
 *     state and each schema partition is initialized only when a collection that needs it is used.</li>
 * </ul>
 * Port of the idea of smithy-java's generated {@code SharedSerde}, rewritten on JavaPoet for the SDK's collection
 * types.
 */
@SdkInternalApi
public final class SharedSerdeSpec {
    public static final String SDK_FIELDS = "SDK_FIELDS";
    public static final String REQUEST_SDK_FIELDS = "REQUEST_SDK_FIELDS";
    public static final String RESPONSE_SDK_FIELDS = "RESPONSE_SDK_FIELDS";

    private static final ClassName WIRE_NAMING = SchemaRuntimeType.SDK_FIELDS_FROM_SCHEMA.nestedClass("WireNaming");
    private static final ClassName HTTP_BINDING_MODE = SchemaRuntimeType.SDK_FIELDS_FROM_SCHEMA.nestedClass(
        "HttpBindingMode");
    private static final ClassName LIST_MEMBER_CONSUMER =
        SchemaRuntimeType.SHAPE_DESERIALIZER.nestedClass("ListMemberConsumer");
    private static final ClassName MAP_MEMBER_CONSUMER =
        SchemaRuntimeType.SHAPE_DESERIALIZER.nestedClass("MapMemberConsumer");
    private static final String SERIALIZER = "serializer";
    private static final String DESERIALIZER = "deserializer";

    private final SchemaSerdeModel serde;
    private final TypeProvider typeProvider;
    private final Model smithy;

    public SharedSerdeSpec(SchemaSerdeModel serde, IntermediateModel model) {
        this.serde = serde;
        this.typeProvider = new TypeProvider(model);
        this.smithy = model.smithyModel();
    }

    public ClassName className() {
        return serde.sharedSerdeClass();
    }

    public TypeSpec spec() {
        TypeSpec.Builder type = TypeSpec.classBuilder(className())
                                        .addAnnotation(PoetUtils.generatedAnnotation())
                                        .addAnnotation(SdkInternalApi.class)
                                        .addModifiers(Modifier.FINAL)
                                        .addJavadoc("Schema serde code shared by the model classes of the service.\n");
        type.addField(settings(SDK_FIELDS, "NONE"));
        if (serde.honorsHttpBindings()) {
            type.addField(settings(REQUEST_SDK_FIELDS, "REQUEST"));
            type.addField(settings(RESPONSE_SDK_FIELDS, "RESPONSE"));
        }
        type.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());
        for (CollectionSerde collection : serde.collections().values()) {
            type.addType(collection.member().isList() ? listSerde(collection) : mapSerde(collection));
        }
        return type.build();
    }

    private FieldSpec settings(String name, String httpBindings) {
        CodeBlock.Builder init = CodeBlock.builder()
                                          .add("$T.builder()", SchemaRuntimeType.SDK_FIELDS_FROM_SCHEMA)
                                          .add(".wireNames($T.$L)", WIRE_NAMING, wireNaming(serde.protocol()));
        if (!"NONE".equals(httpBindings)) {
            init.add(".httpBindings($T.$L)", HTTP_BINDING_MODE, httpBindings);
        }
        if (serde.requiredTraits()) {
            init.add(".requiredTraits(true)");
        }
        init.add(".build()");
        return FieldSpec.builder(SchemaRuntimeType.SDK_FIELDS_FROM_SCHEMA, name, Modifier.STATIC, Modifier.FINAL)
                        .initializer(init.build())
                        .build();
    }

    private static String wireNaming(Protocol protocol) {
        switch (protocol) {
            case AWS_JSON:
            case REST_JSON:
                return "JSON";
            case REST_XML:
            case QUERY:
                return "XML";
            case EC2:
                return "EC2";
            default:
                return "MEMBER";
        }
    }

    private TypeSpec listSerde(CollectionSerde collection) {
        MemberModel element = collection.member().getListModel().getListMemberModel();
        TypeName elementType = typeProvider.fieldType(element);
        TypeName listType = typeProvider.fieldType(collection.member());
        ClassName self = collection.className();
        CodeBlock memberSchema = CodeBlock.of("MEMBER");
        ValueKind kind = ValueKind.of(element);

        boolean sparse = isSparse(collection);
        MethodSpec write = MethodSpec.methodBuilder("accept")
                                     .addAnnotation(Override.class)
                                     .addModifiers(Modifier.PUBLIC)
                                     .addParameter(listType, "values")
                                     .addParameter(SchemaRuntimeType.SHAPE_SERIALIZER, SERIALIZER)
                                     .beginControlFlow("for ($T value : values)", elementType)
                                     .addCode(writeOrNull(kind, element, memberSchema, sparse))
                                     .endControlFlow()
                                     .build();
        MethodSpec readElement = MethodSpec.methodBuilder("accept")
                                           .addAnnotation(Override.class)
                                           .addModifiers(Modifier.PUBLIC)
                                           .addParameter(listType, "values")
                                           .addParameter(SchemaRuntimeType.SHAPE_DESERIALIZER, DESERIALIZER)
                                           .beginControlFlow("if ($N.isNull())", DESERIALIZER)
                                           .addCode(readNull("values.add($L)", kind, sparse))
                                           .nextControlFlow("else")
                                           .addStatement("values.add($L)", read(kind, element, memberSchema))
                                           .endControlFlow()
                                           .build();
        MethodSpec read = MethodSpec.methodBuilder("read")
                                    .addModifiers(Modifier.STATIC)
                                    .returns(listType)
                                    .addParameter(SchemaRuntimeType.SCHEMA, "schema")
                                    .addParameter(SchemaRuntimeType.SHAPE_DESERIALIZER, DESERIALIZER)
                                    .addStatement("$T values = $T.newList($N)", listType,
                                                  SchemaRuntimeType.SCHEMA_SERDE_SUPPORT, DESERIALIZER)
                                    .addStatement("$N.readList(schema, values, INSTANCE)", DESERIALIZER)
                                    .addStatement("return $T.unmodifiableList(values)", Collections.class)
                                    .build();
        return TypeSpec.classBuilder(self)
                       .addModifiers(Modifier.STATIC, Modifier.FINAL)
                       .addJavadoc("Writes and reads {@code $L}.\n", collection.shapeId())
                       .addSuperinterface(ParameterizedTypeName.get(ClassName.get(BiConsumer.class), listType,
                                                                    SchemaRuntimeType.SHAPE_SERIALIZER))
                       .addSuperinterface(ParameterizedTypeName.get(LIST_MEMBER_CONSUMER, listType))
                       .addField(FieldSpec.builder(self, "INSTANCE", Modifier.STATIC, Modifier.FINAL)
                                          .initializer("new $T()", self)
                                          .build())
                       .addField(FieldSpec.builder(SchemaRuntimeType.SCHEMA, "MEMBER", Modifier.STATIC, Modifier.FINAL)
                                          .initializer("$L.listMember()", collection.schemaReference())
                                          .build())
                       .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build())
                       .addMethod(write)
                       .addMethod(readElement)
                       .addMethod(read)
                       .build();
    }

    private TypeSpec mapSerde(CollectionSerde collection) {
        MemberModel value = collection.member().getMapModel().getValueModel();
        TypeName valueType = typeProvider.fieldType(value);
        TypeName mapType = typeProvider.fieldType(collection.member());
        TypeName entryType = ParameterizedTypeName.get(ClassName.get(Map.Entry.class), ClassName.get(String.class),
                                                       valueType);
        ClassName self = collection.className();
        ClassName valueWriter = self.nestedClass("ValueWriter");
        CodeBlock valueSchema = CodeBlock.of("VALUE");
        ValueKind kind = ValueKind.of(value);
        boolean sparse = isSparse(collection);

        MethodSpec write = MethodSpec.methodBuilder("accept")
                                     .addAnnotation(Override.class)
                                     .addModifiers(Modifier.PUBLIC)
                                     .addParameter(mapType, "values")
                                     .addParameter(SchemaRuntimeType.MAP_SERIALIZER, SERIALIZER)
                                     .beginControlFlow("for ($T entry : values.entrySet())", entryType)
                                     .addStatement("$N.writeEntry(KEY, entry.getKey(), entry.getValue(), $T.INSTANCE)",
                                                   SERIALIZER, valueWriter)
                                     .endControlFlow()
                                     .build();
        MethodSpec readEntry = MethodSpec.methodBuilder("accept")
                                         .addAnnotation(Override.class)
                                         .addModifiers(Modifier.PUBLIC)
                                         .addParameter(mapType, "values")
                                         .addParameter(String.class, "key")
                                         .addParameter(SchemaRuntimeType.SHAPE_DESERIALIZER, DESERIALIZER)
                                         .beginControlFlow("if ($N.isNull())", DESERIALIZER)
                                         .addCode(readNull("values.put(key, $L)", kind, sparse))
                                         .nextControlFlow("else")
                                         .addStatement("values.put(key, $L)", read(kind, value, valueSchema))
                                         .endControlFlow()
                                         .build();
        MethodSpec read = MethodSpec.methodBuilder("read")
                                    .addModifiers(Modifier.STATIC)
                                    .returns(mapType)
                                    .addParameter(SchemaRuntimeType.SCHEMA, "schema")
                                    .addParameter(SchemaRuntimeType.SHAPE_DESERIALIZER, DESERIALIZER)
                                    .addStatement("$T values = $T.newMap($N)", mapType,
                                                  SchemaRuntimeType.SCHEMA_SERDE_SUPPORT, DESERIALIZER)
                                    .addStatement("$N.readStringMap(schema, values, INSTANCE)", DESERIALIZER)
                                    .addStatement("return $T.unmodifiableMap(values)", Collections.class)
                                    .build();
        TypeSpec writer = TypeSpec.classBuilder(valueWriter)
                                  .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                                  .addSuperinterface(ParameterizedTypeName.get(ClassName.get(BiConsumer.class), valueType,
                                                                               SchemaRuntimeType.SHAPE_SERIALIZER))
                                  .addField(FieldSpec.builder(valueWriter, "INSTANCE", Modifier.STATIC, Modifier.FINAL)
                                                     .initializer("new $T()", valueWriter)
                                                     .build())
                                  .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build())
                                  .addMethod(MethodSpec.methodBuilder("accept")
                                                       .addAnnotation(Override.class)
                                                       .addModifiers(Modifier.PUBLIC)
                                                       .addParameter(valueType, "value")
                                                       .addParameter(SchemaRuntimeType.SHAPE_SERIALIZER, SERIALIZER)
                                                       .addCode(writeOrNull(kind, value, valueSchema, sparse))
                                                       .build())
                                  .build();
        return TypeSpec.classBuilder(self)
                       .addModifiers(Modifier.STATIC, Modifier.FINAL)
                       .addJavadoc("Writes and reads {@code $L}.\n", collection.shapeId())
                       .addSuperinterface(ParameterizedTypeName.get(ClassName.get(BiConsumer.class), mapType,
                                                                    SchemaRuntimeType.MAP_SERIALIZER))
                       .addSuperinterface(ParameterizedTypeName.get(MAP_MEMBER_CONSUMER, ClassName.get(String.class),
                                                                    mapType))
                       .addField(FieldSpec.builder(self, "INSTANCE", Modifier.STATIC, Modifier.FINAL)
                                          .initializer("new $T()", self)
                                          .build())
                       .addField(FieldSpec.builder(SchemaRuntimeType.SCHEMA, "KEY", Modifier.STATIC, Modifier.FINAL)
                                          .initializer("$L.mapKeyMember()", collection.schemaReference())
                                          .build())
                       .addField(FieldSpec.builder(SchemaRuntimeType.SCHEMA, "VALUE", Modifier.STATIC, Modifier.FINAL)
                                          .initializer("$L.mapValueMember()", collection.schemaReference())
                                          .build())
                       .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build())
                       .addMethod(write)
                       .addMethod(readEntry)
                       .addMethod(read)
                       .addType(writer)
                       .build();
    }

    private boolean isSparse(CollectionSerde collection) {
        return smithy.expectShape(collection.shapeId()).hasTrait(SparseTrait.class);
    }

    /**
     * Writes a null element as null. In a sparse collection, a list or map element that is
     * {@code SdkAutoConstructList}/{@code SdkAutoConstructMap} is written as null too: the builder's copier stores a
     * null nested collection as the auto-construct instance.
     */
    private CodeBlock writeOrNull(ValueKind kind, MemberModel element, CodeBlock schema, boolean sparse) {
        CodeBlock isNull = CodeBlock.of("value == null");
        Class<?> autoConstruct = autoConstructType(kind);
        if (sparse && autoConstruct != null) {
            isNull = CodeBlock.of("value == null || value instanceof $T", autoConstruct);
        }
        return CodeBlock.builder()
                        .beginControlFlow("if ($L)", isNull)
                        .addStatement("$N.writeNull($L)", SERIALIZER, schema)
                        .nextControlFlow("else")
                        .addStatement("$L", kind.write(SERIALIZER, schema, CodeBlock.of("value"), writerOf(kind, element)))
                        .endControlFlow()
                        .build();
    }

    /**
     * Reads a null element. In a sparse collection, a null list or map element becomes the
     * {@code DefaultSdkAutoConstructList}/{@code DefaultSdkAutoConstructMap} instance, as the builder's copier and the
     * legacy unmarshallers (which call the builder setter) store it; other null elements stay null.
     */
    private CodeBlock readNull(String addFormat, ValueKind kind, boolean sparse) {
        if (sparse && autoConstructType(kind) != null) {
            ClassName instance = ClassName.get(kind == ValueKind.LIST ? DefaultSdkAutoConstructList.class
                                                                     : DefaultSdkAutoConstructMap.class);
            return CodeBlock.builder()
                            .addStatement("$N.readNull()", DESERIALIZER)
                            .addStatement(addFormat, CodeBlock.of("$T.getInstance()", instance))
                            .build();
        }
        return CodeBlock.builder().addStatement(addFormat, CodeBlock.of("$N.readNull()", DESERIALIZER)).build();
    }

    private static Class<?> autoConstructType(ValueKind kind) {
        switch (kind) {
            case LIST:
                return SdkAutoConstructList.class;
            case MAP:
                return SdkAutoConstructMap.class;
            default:
                return null;
        }
    }

    private CodeBlock read(ValueKind kind, MemberModel element, CodeBlock schema) {
        return kind.read(DESERIALIZER, schema, kind == ValueKind.STRUCTURE ? typeProvider.fieldType(element) : null,
                         readerOf(kind, element));
    }

    /**
     * The writer of a nested list or map: its serde class's {@code INSTANCE}.
     */
    public CodeBlock writerOf(ValueKind kind, MemberModel member) {
        if (kind != ValueKind.LIST && kind != ValueKind.MAP) {
            return null;
        }
        return CodeBlock.of("$T.INSTANCE", serde.collection(member, smithy).className());
    }

    /**
     * The reader of a nested list or map: its serde class.
     */
    public CodeBlock readerOf(ValueKind kind, MemberModel member) {
        if (kind != ValueKind.LIST && kind != ValueKind.MAP) {
            return null;
        }
        return CodeBlock.of("$T", serde.collection(member, smithy).className());
    }
}
