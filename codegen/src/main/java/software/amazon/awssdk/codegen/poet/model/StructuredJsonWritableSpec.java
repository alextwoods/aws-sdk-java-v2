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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;
import software.amazon.awssdk.protocols.json.JsonFieldNameToken;
import software.amazon.awssdk.protocols.json.StructuredJsonGenerator;
import software.amazon.awssdk.protocols.json.StructuredJsonWritable;
import software.amazon.awssdk.utils.DateUtils;

/**
 * Generates the {@link StructuredJsonWritable#marshallJsonFields} implementation for model shapes of
 * JSON-family protocols whose members all bind to the payload. The generated method writes each present
 * member with straight-line code (null check, pre-encoded field-name token, direct typed write), producing
 * bytes identical to the generic {@code SdkField} marshalling loop it bypasses.
 */
public final class StructuredJsonWritableSpec {

    /**
     * Simple marshalling types the generated code can write directly. DOCUMENT and STREAM are absent:
     * shapes containing them fall back to the generic loop.
     */
    private static final Set<String> SUPPORTED_SIMPLE_TYPES = new HashSet<>(Arrays.asList(
        "STRING", "INTEGER", "LONG", "SHORT", "BYTE", "FLOAT", "DOUBLE", "BIG_DECIMAL", "BOOLEAN",
        "INSTANT", "SDK_BYTES"));

    /**
     * Per-model shape qualification, computed once per {@link IntermediateModel} because qualification
     * is transitive over nested shapes (with cycles, e.g. DynamoDB's AttributeValue).
     */
    private static final Map<IntermediateModel, Map<String, Boolean>> QUALIFICATION_CACHE =
        Collections.synchronizedMap(new WeakHashMap<>());

    private final IntermediateModel intermediateModel;
    private final ShapeModel shapeModel;
    private final TypeProvider typeProvider;
    private final NamingStrategy namingStrategy;

    public StructuredJsonWritableSpec(IntermediateModel intermediateModel, ShapeModel shapeModel,
                                      TypeProvider typeProvider) {
        this.intermediateModel = intermediateModel;
        this.shapeModel = shapeModel;
        this.typeProvider = typeProvider;
        this.namingStrategy = intermediateModel.getNamingStrategy();
    }

    /**
     * Whether the service's protocol marshals requests through the shared JSON marshaller
     * (JsonProtocolMarshaller), which is where the generated method is dispatched from.
     */
    public static boolean isSupportedProtocol(IntermediateModel model) {
        Protocol protocol = model.getMetadata().getProtocol();
        return protocol == Protocol.AWS_JSON
               || protocol == Protocol.REST_JSON
               || protocol == Protocol.CBOR
               || protocol == Protocol.SMITHY_RPC_V2_CBOR;
    }

    /**
     * Whether this shape gets a generated {@code marshallJsonFields} implementation.
     */
    public boolean qualifies() {
        if (!isSupportedProtocol(intermediateModel)) {
            return false;
        }
        ShapeType type = shapeModel.getShapeType();
        if (type != ShapeType.Request && type != ShapeType.Model) {
            return false;
        }
        return qualificationMap().getOrDefault(shapeModel.getShapeName(), false);
    }

    private Map<String, Boolean> qualificationMap() {
        return QUALIFICATION_CACHE.computeIfAbsent(intermediateModel, StructuredJsonWritableSpec::computeQualification);
    }

    /**
     * Computes shape qualification as a fixpoint: start from each shape's local qualification, then
     * repeatedly disqualify shapes that (transitively through container members) reference a
     * disqualified shape, until stable. Handles recursive shapes correctly.
     */
    private static Map<String, Boolean> computeQualification(IntermediateModel model) {
        Map<String, Boolean> qualified = new HashMap<>();
        Map<String, Set<String>> pojoRefs = new HashMap<>();

        for (Map.Entry<String, ShapeModel> e : model.getShapes().entrySet()) {
            ShapeModel shape = e.getValue();
            Set<String> refs = new HashSet<>();
            qualified.put(shape.getShapeName(), locallyQualifies(model, shape, refs));
            pojoRefs.put(shape.getShapeName(), refs);
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (Map.Entry<String, Boolean> e : qualified.entrySet()) {
                if (!e.getValue()) {
                    continue;
                }
                for (String ref : pojoRefs.get(e.getKey())) {
                    if (!qualified.getOrDefault(ref, false)) {
                        e.setValue(false);
                        changed = true;
                        break;
                    }
                }
            }
        }
        return qualified;
    }

    private static boolean locallyQualifies(IntermediateModel model, ShapeModel shape, Set<String> pojoRefs) {
        ShapeType type = shape.getShapeType();
        if (type != ShapeType.Request && type != ShapeType.Model) {
            return false;
        }
        if (shape.isEventStream() || shape.isEvent() || shape.isHasStreamingMember() || shape.isDocument()) {
            return false;
        }
        for (MemberModel m : marshallableMembers(shape)) {
            if (!memberQualifies(model, shape, m, pojoRefs)) {
                return false;
            }
        }
        return true;
    }

    /**
     * The members the generic loop would marshall: exactly the members that make up the generated
     * SDK_FIELDS list (non-streaming, non-exception-target, non-synthetic).
     */
    private static List<MemberModel> marshallableMembers(ShapeModel shape) {
        List<MemberModel> result = new ArrayList<>();
        for (MemberModel m : shape.getNonStreamingMembers()) {
            if (m.getShape() != null && m.getShape().getShapeType() == ShapeType.Exception) {
                continue;
            }
            if (m.isSynthetic()) {
                continue;
            }
            result.add(m);
        }
        return result;
    }

    private static boolean memberQualifies(IntermediateModel model, ShapeModel shape, MemberModel m,
                                           Set<String> pojoRefs) {
        CustomizationConfig customization = model.getCustomizationConfig();
        if (m.getHttp() == null
            || m.getHttp().getMarshallLocation() != MarshallLocation.PAYLOAD
            || m.isEventHeader()
            || m.isEventPayload()
            || m.getHttp().getIsPayload()) {
            return false;
        }
        if (customization.getAttachPayloadTraitToMember()
                         .getOrDefault(shape.getC2jName(), "")
                         .equals(m.getC2jName())) {
            return false;
        }
        if (m.isJsonValue() || m.isIdempotencyToken()) {
            return false;
        }
        if (customization.getModelMarshallerDefaultValueSupplier().get(m.getC2jName()) != null) {
            return false;
        }
        return valueQualifies(model, m, pojoRefs);
    }

    /**
     * Whether the member's value (recursing through container element types) is directly writable.
     * Collects referenced POJO shape names for the transitive qualification fixpoint.
     */
    private static boolean valueQualifies(IntermediateModel model, MemberModel m, Set<String> pojoRefs) {
        if (m.isList()) {
            return valueQualifies(model, m.getListModel().getListMemberModel(), pojoRefs);
        }
        if (m.isMap()) {
            MemberModel keyModel = m.getMapModel().getKeyModel();
            // JSON map keys are always strings on the wire; anything else falls back.
            if (keyModel != null && keyModel.isSimple() && !"String".equals(keyModel.getVariable().getSimpleType())) {
                return false;
            }
            return valueQualifies(model, m.getMapModel().getValueModel(), pojoRefs);
        }
        if (!m.isSimple()) {
            // Container element MemberModels are not shape-linked; resolve by C2J name.
            ShapeModel target = m.getShape();
            if (target == null && m.getC2jShape() != null) {
                try {
                    target = Utils.findMemberShapeModelByC2jNameIfExists(model, m.getC2jShape());
                } catch (IllegalStateException e) {
                    // Conflicting candidates: cannot establish the target, fall back to the generic loop.
                    return false;
                }
            }
            if (target == null) {
                return false;
            }
            pojoRefs.add(target.getShapeName());
            return true;
        }
        String marshallingType = m.getMarshallingType();
        if (!SUPPORTED_SIMPLE_TYPES.contains(marshallingType)) {
            return false;
        }
        if ("INSTANT".equals(marshallingType) && m.getTimestampFormat() != null) {
            try {
                TimestampFormatTrait.Format format = TimestampFormatTrait.Format.fromString(m.getTimestampFormat());
                return format == TimestampFormatTrait.Format.UNIX_TIMESTAMP
                       || format == TimestampFormatTrait.Format.RFC_822
                       || format == TimestampFormatTrait.Format.ISO_8601;
            } catch (RuntimeException e) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Code generation
    // ------------------------------------------------------------------

    /**
     * The pre-encoded {@code "name":} token constants, one per member.
     */
    public List<FieldSpec> nameTokenFields() {
        List<FieldSpec> fields = new ArrayList<>();
        for (MemberModel m : marshallableMembers(shapeModel)) {
            fields.add(FieldSpec.builder(byte[].class, nameTokenFieldName(m),
                                         Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                                .initializer("$T.of($S)", ClassName.get(JsonFieldNameToken.class),
                                             m.getHttp().getMarshallLocationName())
                                .build());
        }
        return fields;
    }

    public MethodSpec marshallJsonFieldsMethod() {
        MethodSpec.Builder method = MethodSpec.methodBuilder("marshallJsonFields")
                                              .addAnnotation(Override.class)
                                              .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                              .addParameter(ClassName.get(StructuredJsonGenerator.class), "generator");
        for (MemberModel m : marshallableMembers(shapeModel)) {
            method.addCode(memberMarshalling(m));
        }
        return method.build();
    }

    private String nameTokenFieldName(MemberModel m) {
        return namingStrategy.getSdkFieldFieldName(m) + "_NAME_TOKEN";
    }

    private CodeBlock memberMarshalling(MemberModel m) {
        String fieldVar = m.getVariable().getVariableName();
        String locationName = m.getHttp().getMarshallLocationName();
        boolean required = intermediateModel.getCustomizationConfig().isRequiredTraitValidationEnabled()
                           && m.isRequired();

        CodeBlock.Builder code = CodeBlock.builder();
        if (required) {
            // Mirrors the generic loop: a null required payload member fails the request.
            code.beginControlFlow("if ($N == null)", fieldVar)
                .addStatement("throw new $T($S)", IllegalArgumentException.class,
                              String.format("Parameter '%s' must not be null", locationName))
                .endControlFlow();
            code.beginControlFlow("if ($L)", emitCondition(m, fieldVar, false));
        } else {
            code.beginControlFlow("if ($L)", emitCondition(m, fieldVar, true));
        }
        code.addStatement("generator.writeFieldName($S, $N)", locationName, nameTokenFieldName(m));
        code.add(valueWrite(m, fieldVar, 0, true));
        code.endControlFlow();
        return code.build();
    }

    /**
     * The presence check matching the generic loop: non-null for scalars and POJOs; for lists and maps
     * additionally not an empty auto-construct instance (which represents an unset member).
     */
    private CodeBlock emitCondition(MemberModel m, String fieldVar, boolean includeNullCheck) {
        CodeBlock.Builder cond = CodeBlock.builder();
        if (includeNullCheck) {
            cond.add("$N != null", fieldVar);
        }
        if (m.isList()) {
            if (includeNullCheck) {
                cond.add(" && ");
            }
            cond.add("(!$N.isEmpty() || !($N instanceof $T))", fieldVar, fieldVar,
                     ClassName.get(SdkAutoConstructList.class));
        } else if (m.isMap()) {
            if (includeNullCheck) {
                cond.add(" && ");
            }
            cond.add("(!$N.isEmpty() || !($N instanceof $T))", fieldVar, fieldVar,
                     ClassName.get(SdkAutoConstructMap.class));
        } else if (!includeNullCheck) {
            cond.add("true");
        }
        return cond.build();
    }

    /**
     * Writes the member's value. {@code topLevel} controls whether an INSTANT honors the member's
     * TimestampFormatTrait: container elements deliberately use the wire default, matching the generic
     * path, which never sees the member SdkField for elements.
     */
    private CodeBlock valueWrite(MemberModel m, String valExpr, int depth, boolean topLevel) {
        if (m.isList()) {
            return listWrite(m, valExpr, depth);
        }
        if (m.isMap()) {
            return mapWrite(m, valExpr, depth);
        }
        if (!m.isSimple()) {
            return CodeBlock.builder()
                            .addStatement("generator.writeStartObject()")
                            .addStatement("$L.marshallJsonFields(generator)", valExpr)
                            .addStatement("generator.writeEndObject()")
                            .build();
        }
        String marshallingType = m.getMarshallingType();
        switch (marshallingType) {
            case "SDK_BYTES":
                return CodeBlock.builder()
                                .addStatement("generator.writeBinaryValue($L.asByteArrayUnsafe())", valExpr)
                                .build();
            case "INSTANT":
                return instantWrite(m, valExpr, topLevel);
            default:
                return CodeBlock.builder().addStatement("generator.writeValue($L)", valExpr).build();
        }
    }

    private CodeBlock instantWrite(MemberModel m, String valExpr, boolean topLevel) {
        String timestampFormat = topLevel ? m.getTimestampFormat() : null;
        if (timestampFormat == null) {
            // The generator supplies the wire-format default (unix seconds for JSON, tagged epoch
            // millis for CBOR), matching SimpleTypeJsonMarshaller.INSTANT's fallback.
            return CodeBlock.builder().addStatement("generator.writeValue($L)", valExpr).build();
        }
        TimestampFormatTrait.Format format = TimestampFormatTrait.Format.fromString(timestampFormat);
        switch (format) {
            case UNIX_TIMESTAMP:
                return CodeBlock.builder()
                                .addStatement("generator.writeNumber($T.formatUnixTimestampInstant($L))",
                                              ClassName.get(DateUtils.class), valExpr)
                                .build();
            case RFC_822:
                return CodeBlock.builder()
                                .addStatement("generator.writeValue($T.formatRfc822Date($L))",
                                              ClassName.get(DateUtils.class), valExpr)
                                .build();
            case ISO_8601:
                return CodeBlock.builder()
                                .addStatement("generator.writeValue($T.formatIso8601Date($L))",
                                              ClassName.get(DateUtils.class), valExpr)
                                .build();
            default:
                throw new IllegalStateException("Unsupported timestamp format for generated marshalling: " + format);
        }
    }

    private CodeBlock listWrite(MemberModel m, String valExpr, int depth) {
        MemberModel element = m.getListModel().getListMemberModel();
        String elementVar = "item" + depth;
        CodeBlock.Builder code = CodeBlock.builder()
                                          .addStatement("generator.writeStartArray($L.size())", valExpr)
                                          .beginControlFlow("for ($T $N : $L)",
                                                            typeProvider.fieldType(element), elementVar, valExpr);
        code.beginControlFlow("if ($N == null)", elementVar)
            .addStatement("generator.writeNull()");
        if (element.isList()) {
            code.nextControlFlow("else if (!$N.isEmpty() || !($N instanceof $T))",
                                 elementVar, elementVar, ClassName.get(SdkAutoConstructList.class));
        } else if (element.isMap()) {
            code.nextControlFlow("else if (!$N.isEmpty() || !($N instanceof $T))",
                                 elementVar, elementVar, ClassName.get(SdkAutoConstructMap.class));
        } else {
            code.nextControlFlow("else");
        }
        code.add(valueWrite(element, elementVar, depth + 1, false));
        code.endControlFlow();
        code.endControlFlow();
        code.addStatement("generator.writeEndArray()");
        return code.build();
    }

    private CodeBlock mapWrite(MemberModel m, String valExpr, int depth) {
        MemberModel value = m.getMapModel().getValueModel();
        String entryVar = "entry" + depth;
        CodeBlock.Builder code = CodeBlock.builder()
                                          .addStatement("generator.writeStartObject()")
                                          .beginControlFlow("for ($T<String, $T> $N : $L.entrySet())",
                                                            ClassName.get(Map.Entry.class),
                                                            typeProvider.fieldType(value), entryVar, valExpr);
        CodeBlock.Builder guard = CodeBlock.builder();
        guard.add("$N.getValue() != null", entryVar);
        if (value.isList()) {
            guard.add(" && (!$N.getValue().isEmpty() || !($N.getValue() instanceof $T))",
                      entryVar, entryVar, ClassName.get(SdkAutoConstructList.class));
        } else if (value.isMap()) {
            guard.add(" && (!$N.getValue().isEmpty() || !($N.getValue() instanceof $T))",
                      entryVar, entryVar, ClassName.get(SdkAutoConstructMap.class));
        }
        code.beginControlFlow("if ($L)", guard.build());
        code.addStatement("generator.writeFieldName($N.getKey())", entryVar);
        code.add(valueWrite(value, entryVar + ".getValue()", depth + 1, false));
        code.endControlFlow();
        code.endControlFlow();
        code.addStatement("generator.writeEndObject()");
        return code.build();
    }
}
