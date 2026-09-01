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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.protocols.json.JsonMemberTable;
import software.amazon.awssdk.protocols.json.StructuredJsonReadable;
import software.amazon.awssdk.protocols.json.StructuredJsonReader;

/**
 * Generates the {@link StructuredJsonReadable#readJsonFields} implementation on model builders of
 * JSON-family protocols whose members all bind to the payload. The generated code deserializes with
 * straight-line reads: member matching via a static {@link JsonMemberTable}, a switch on member
 * ordinal, direct builder-field writes (no setter dispatch, no copier re-copy), and collections
 * built exactly once. Produces objects identical to the generic unmarshalling loop it bypasses.
 */
public final class StructuredJsonReadableSpec {

    private final IntermediateModel intermediateModel;
    private final ShapeModel shapeModel;
    private final TypeProvider typeProvider;
    private final ClassName builderImplName;
    private final ClassName shapeName;

    public StructuredJsonReadableSpec(IntermediateModel intermediateModel, ShapeModel shapeModel,
                                      TypeProvider typeProvider, ClassName shapeName, ClassName builderImplName) {
        this.intermediateModel = intermediateModel;
        this.shapeModel = shapeModel;
        this.typeProvider = typeProvider;
        this.shapeName = shapeName;
        this.builderImplName = builderImplName;
    }

    /**
     * Whether this shape's builder gets a generated {@code readJsonFields} implementation.
     */
    public boolean qualifies() {
        if (!StructuredJsonWritableSpec.isSupportedProtocol(intermediateModel)) {
            return false;
        }
        ShapeType type = shapeModel.getShapeType();
        if (type != ShapeType.Response && type != ShapeType.Model) {
            return false;
        }
        return StructuredJsonWritableSpec.qualificationFor(intermediateModel)
                                         .getOrDefault(shapeModel.getShapeName(), false);
    }

    /**
     * The static member-name table, in SDK_FIELDS order (ordinals match the generated switch).
     */
    public FieldSpec memberTableField() {
        CodeBlock.Builder names = CodeBlock.builder().add("$T.of(", ClassName.get(JsonMemberTable.class));
        List<MemberModel> members = StructuredJsonWritableSpec.marshallableMembers(shapeModel);
        for (int i = 0; i < members.size(); i++) {
            names.add(i == 0 ? "$S" : ", $S", members.get(i).getHttp().getMarshallLocationName());
        }
        names.add(")");
        return FieldSpec.builder(ClassName.get(JsonMemberTable.class), "$JSON_MEMBER_TABLE",
                                 Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer(names.build())
                        .build();
    }

    public MethodSpec readJsonFieldsMethod() {
        return MethodSpec.methodBuilder("readJsonFields")
                         .addAnnotation(Override.class)
                         .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                         .addParameter(ClassName.get(StructuredJsonReader.class), "reader")
                         .addStatement("reader.readStruct(this, $$JSON_MEMBER_TABLE, $T::$$readJsonMember)",
                                       builderImplName)
                         .build();
    }

    /**
     * The per-member switch invoked by {@link StructuredJsonReader#readStruct}.
     */
    public MethodSpec readJsonMemberMethod() {
        MethodSpec.Builder method = MethodSpec.methodBuilder("$readJsonMember")
                                              .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                                              .addParameter(builderImplName, "b")
                                              .addParameter(int.class, "memberIndex")
                                              .addParameter(ClassName.get(StructuredJsonReader.class), "reader");
        method.beginControlFlow("switch (memberIndex)");
        List<MemberModel> members = StructuredJsonWritableSpec.marshallableMembers(shapeModel);
        for (int i = 0; i < members.size(); i++) {
            MemberModel m = members.get(i);
            // Each case body is braced: member reads declare locals, and switch cases share scope.
            method.addCode("case $L: {\n$>", i);
            method.addCode(memberRead(m));
            method.addStatement("break");
            method.addCode("$<}\n");
        }
        method.addCode("default:\n$>");
        method.addStatement("throw new $T($S + memberIndex)", IllegalStateException.class, "Unexpected member index: ");
        method.addCode("$<");
        method.endControlFlow();
        return method.build();
    }

    /**
     * The package-private static factory other generated shapes use for nested members:
     * builds, fills and returns a shape from the reader.
     */
    public MethodSpec readJsonStaticMethod() {
        return MethodSpec.methodBuilder("$readJson")
                         .addModifiers(Modifier.STATIC)
                         .returns(shapeName)
                         .addParameter(ClassName.get(StructuredJsonReader.class), "reader")
                         .addStatement("$T builder = new $T()", builderImplName, builderImplName)
                         .addStatement("builder.readJsonFields(reader)")
                         .addStatement("return builder.build()")
                         .build();
    }

    private CodeBlock memberRead(MemberModel m) {
        String fieldVar = m.getVariable().getVariableName();
        boolean union = shapeModel.isUnion();
        if (m.isList() || m.isMap()) {
            CodeBlock.Builder code = CodeBlock.builder();
            String collectVar = fieldVar + "Value";
            code.add(containerRead(m, collectVar, 0, "reader"));
            if (union) {
                code.addStatement("$T oldValue = b.$N", Object.class, fieldVar)
                    .addStatement("b.$N = $L", fieldVar, wrapUnmodifiable(m, collectVar))
                    .addStatement("b.handleUnionValueChange(Type.$N, oldValue, b.$N)",
                                  m.getUnionEnumTypeName(), fieldVar);
            } else {
                code.addStatement("b.$N = $L", fieldVar, wrapUnmodifiable(m, collectVar));
            }
            return code.build();
        }
        CodeBlock valueExpr = scalarOrPojoReadExpr(m, "reader", true);
        if (union) {
            // Scalar setters on unions only assign the field and maintain the union type; no copy.
            return CodeBlock.builder().addStatement("b.$N($L)", m.getFluentSetterMethodName(), valueExpr).build();
        }
        return CodeBlock.builder().addStatement("b.$N = $L", fieldVar, valueExpr).build();
    }

    /**
     * Emits code declaring {@code collectVar} and filling it from {@code readerVar}. Nested
     * containers recurse with block lambdas; collections are wrapped unmodifiable exactly once by
     * the caller.
     */
    private CodeBlock containerRead(MemberModel m, String collectVar, int depth, String readerVar) {
        CodeBlock.Builder code = CodeBlock.builder();
        if (m.isList()) {
            MemberModel element = m.getListModel().getListMemberModel();
            TypeName elementType = typeProvider.fieldType(element);
            String lambdaState = "l" + depth;
            String lambdaReader = "r" + depth;
            code.addStatement("$T<$T> $N = new $T<>()", List.class, elementType, collectVar, ArrayList.class);
            if (element.isList() || element.isMap()) {
                String innerVar = collectVar + depth;
                code.add("$N.readList($N, ($N, $N) -> {\n$>", readerVar, collectVar, lambdaState, lambdaReader)
                    .beginControlFlow("if ($N.readNullIfPresent())", lambdaReader)
                    .addStatement("$N.add(null)", lambdaState)
                    .nextControlFlow("else")
                    .add(containerRead(element, innerVar, depth + 1, lambdaReader))
                    .addStatement("$N.add($L)", lambdaState, wrapUnmodifiable(element, innerVar))
                    .endControlFlow()
                    .add("$<});\n");
            } else {
                code.addStatement("$N.readList($N, ($N, $N) -> $N.add($L))",
                                  readerVar, collectVar, lambdaState, lambdaReader, lambdaState,
                                  nullableReadExpr(element, lambdaReader));
            }
        } else {
            MemberModel value = m.getMapModel().getValueModel();
            TypeName valueType = typeProvider.fieldType(value);
            String lambdaState = "m" + depth;
            String lambdaKey = "k" + depth;
            String lambdaReader = "r" + depth;
            code.addStatement("$T<String, $T> $N = new $T<>()", Map.class, valueType, collectVar, LinkedHashMap.class);
            if (value.isList() || value.isMap()) {
                String innerVar = collectVar + depth;
                code.add("$N.readStringMap($N, ($N, $N, $N) -> {\n$>",
                         readerVar, collectVar, lambdaState, lambdaKey, lambdaReader)
                    .beginControlFlow("if ($N.readNullIfPresent())", lambdaReader)
                    .addStatement("$N.put($N, null)", lambdaState, lambdaKey)
                    .nextControlFlow("else")
                    .add(containerRead(value, innerVar, depth + 1, lambdaReader))
                    .addStatement("$N.put($N, $L)", lambdaState, lambdaKey, wrapUnmodifiable(value, innerVar))
                    .endControlFlow()
                    .add("$<});\n");
            } else {
                code.addStatement("$N.readStringMap($N, ($N, $N, $N) -> $N.put($N, $L))",
                                  readerVar, collectVar, lambdaState, lambdaKey, lambdaReader, lambdaState, lambdaKey,
                                  nullableReadExpr(value, lambdaReader));
            }
        }
        return code.build();
    }

    private CodeBlock wrapUnmodifiable(MemberModel m, String var) {
        if (m.isList()) {
            return CodeBlock.of("$T.unmodifiableList($N)", Collections.class, var);
        }
        return CodeBlock.of("$T.unmodifiableMap($N)", Collections.class, var);
    }

    /**
     * Read expression for a possibly-null container element or map value.
     */
    private CodeBlock nullableReadExpr(MemberModel m, String readerVar) {
        return CodeBlock.of("$N.readNullIfPresent() ? null : $L", readerVar, scalarOrPojoReadExpr(m, readerVar, false));
    }

    /**
     * Read expression for a scalar or nested-shape value. {@code topLevel} controls whether an
     * INSTANT honors the member's timestamp format trait; both the generic loop and this path honor
     * element-level traits on the read side, so the trait is applied at every level.
     */
    private CodeBlock scalarOrPojoReadExpr(MemberModel m, String readerVar, boolean topLevel) {
        if (!m.isSimple()) {
            return CodeBlock.of("$T.$$readJson($N)", typeProvider.fieldType(m), readerVar);
        }
        switch (m.getMarshallingType()) {
            case "STRING":
                return CodeBlock.of("$N.readString()", readerVar);
            case "INTEGER":
                return CodeBlock.of("$N.readInt()", readerVar);
            case "LONG":
                return CodeBlock.of("$N.readLong()", readerVar);
            case "SHORT":
                return CodeBlock.of("$N.readShort()", readerVar);
            case "BYTE":
                return CodeBlock.of("$N.readByte()", readerVar);
            case "FLOAT":
                return CodeBlock.of("$N.readFloat()", readerVar);
            case "DOUBLE":
                return CodeBlock.of("$N.readDouble()", readerVar);
            case "BIG_DECIMAL":
                return CodeBlock.of("$N.readBigDecimal()", readerVar);
            case "BOOLEAN":
                return CodeBlock.of("$N.readBoolean()", readerVar);
            case "SDK_BYTES":
                return CodeBlock.of("$N.readSdkBytes()", readerVar);
            case "INSTANT":
                if (m.getTimestampFormat() != null) {
                    TimestampFormatTrait.Format format = TimestampFormatTrait.Format.fromString(m.getTimestampFormat());
                    return CodeBlock.of("$N.readInstant($T.$L)", readerVar,
                                        ClassName.get(TimestampFormatTrait.Format.class), format.name());
                }
                return CodeBlock.of("$N.readInstant(null)", readerVar);
            default:
                throw new IllegalStateException("Unsupported marshalling type for generated read: "
                                                + m.getMarshallingType());
        }
    }

}
