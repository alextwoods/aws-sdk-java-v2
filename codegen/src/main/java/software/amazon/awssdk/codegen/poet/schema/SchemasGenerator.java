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
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.Modifier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.schema.SchemaFieldOrder.SchemaField;
import software.amazon.awssdk.codegen.schema.SchemaCodegenException;
import software.amazon.awssdk.codegen.schema.SchemaRuntimeType;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.traits.UnitTypeTrait;

/**
 * Generates the {@code <Svc>Schemas}, {@code <Svc>Schemas1}, … partition classes: one package-private
 * {@code static final Schema} field per shape in the service closure, in the order and partitions of
 * {@link SchemaFieldOrder} (design A.17, Requirements 10.1, 10.4, 10.5, 10.6, 10.7, 10.9). Port of smithy-java
 * {@code SchemasGenerator}, rewritten on JavaPoet.
 *
 * <p>For example:
 * <pre>{@code
 * static final Schema GET_QUEUE_URL_REQUEST = Schema.structureBuilder(ShapeId.fromParts(NS, "GetQueueUrlRequest"))
 *     .putMember("QueueName", PreludeSchemas.STRING, RequiredTrait.INSTANCE)
 *     .putMember("QueueOwnerAWSAccountId", PreludeSchemas.STRING)
 *     .build();
 * }</pre>
 *
 * <p>Member traits are emitted on {@code putMember}, apart from the target's traits, which are on the target's schema
 * and merged at runtime. A structure or union whose generated class has schema serde members ({@link SchemaSerdeModel})
 * gets {@code .builderSupplier(X::newShapeBuilder).shapeClass(X.class)}: a method reference to a package-private static
 * method, which initializes neither the class nor a builder until a builder is requested. An enum with a generated enum
 * class passes the class to {@code createEnum}. A member that targets {@code smithy.api#Unit} (a union member) targets
 * {@code Unit.SCHEMA} of the generated {@code Unit} class when that class has schema serde members, else
 * {@code PreludeSchemas.UNIT}.
 */
@SdkInternalApi
public final class SchemasGenerator {
    private static final Map<String, String> PRELUDE_FIELDS = preludeFields();
    private static final ShapeId UNIT = ShapeId.from("smithy.api#Unit");

    private final ServiceShape service;
    private final SchemaTraits traits;
    private final String modelPackage;
    private final String classPrefix;
    private final SchemaFieldOrder order;
    private final SchemaSerdeModel serde;

    SchemasGenerator(Model model, ServiceShape service, SchemaTraits traits, String modelPackage, String serviceName,
                     Set<String> reservedNames, int partitionBudget, SchemaSerdeModel serde) {
        this.serde = serde;
        this.service = service;
        this.traits = traits;
        this.modelPackage = modelPackage;
        this.classPrefix = serviceName;
        this.order = SchemaFieldOrder.create(model, service, reservedNames, this::estimate, partitionBudget);
    }

    /**
     * @return the field order and partitions
     */
    public SchemaFieldOrder order() {
        return order;
    }

    /**
     * @param partition a partition index
     * @return the partition's class name: {@code <Svc>Schemas} for the first, then {@code <Svc>Schemas1}, …
     */
    public ClassName partitionClass(int partition) {
        return ClassName.get(modelPackage, classPrefix + "Schemas" + (partition == 0 ? "" : String.valueOf(partition)));
    }

    /**
     * @param field a schema field
     * @return an expression that reads the field from another class
     */
    public CodeBlock fieldReference(SchemaField field) {
        return CodeBlock.of("$T.$L", partitionClass(field.partition()), field.fieldName());
    }

    /**
     * @return the partition classes, in order
     */
    public List<TypeSpec> partitionSpecs() {
        List<TypeSpec> result = new ArrayList<>();
        List<List<SchemaField>> partitions = order.partitions();
        for (int i = 0; i < partitions.size(); i++) {
            result.add(partitionSpec(i, partitions.get(i), partitions.size()));
        }
        return result;
    }

    private TypeSpec partitionSpec(int index, List<SchemaField> fields, int count) {
        ClassName name = partitionClass(index);
        TypeSpec.Builder type = TypeSpec.classBuilder(name)
                                        .addAnnotation(PoetUtils.generatedAnnotation())
                                        .addAnnotation(SdkInternalApi.class)
                                        .addModifiers(Modifier.FINAL)
                                        .addJavadoc("Schemas of the shapes of the $L service$L.\n", service.getId(),
                                                    count == 1 ? "" : " (part " + (index + 1) + " of " + count + ")");
        if (usesServiceNamespace(fields)) {
            type.addField(FieldSpec.builder(String.class, SchemaFieldOrder.NAMESPACE_CONSTANT, Modifier.PRIVATE,
                                            Modifier.STATIC, Modifier.FINAL)
                                   .initializer("$S", service.getId().getNamespace())
                                   .build());
        }
        int unit = -1;
        for (int i = 0; i < fields.size(); i++) {
            SchemaField field = fields.get(i);
            if (field.unit() != unit) {
                unit = field.unit();
                for (int j = i; j < fields.size() && fields.get(j).unit() == unit; j++) {
                    if (fields.get(j).hasBuilder()) {
                        type.addField(builderField(fields.get(j)));
                    }
                }
            }
            type.addField(FieldSpec.builder(SchemaRuntimeType.SCHEMA, field.fieldName(), Modifier.STATIC,
                                            Modifier.FINAL)
                                   .initializer(initializer(field, name))
                                   .build());
        }
        type.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());
        return type.build();
    }

    private boolean usesServiceNamespace(List<SchemaField> fields) {
        for (SchemaField field : fields) {
            if (field.emittedId().getNamespace().equals(service.getId().getNamespace())) {
                return true;
            }
        }
        return false;
    }

    private FieldSpec builderField(SchemaField field) {
        return FieldSpec.builder(SchemaRuntimeType.SCHEMA_BUILDER, field.builderName(), Modifier.PRIVATE,
                                 Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.$L($L$L)", SchemaRuntimeType.SCHEMA, builderMethod(field.shape()),
                                     shapeId(field.emittedId()), SchemaTraits.arguments(rootTraits(field)))
                        .build();
    }

    private CodeBlock initializer(SchemaField field, ClassName self) {
        Shape shape = field.shape();
        CodeBlock id = shapeId(field.emittedId());
        CodeBlock traitArgs = SchemaTraits.arguments(rootTraits(field));
        switch (shape.getType()) {
            case STRUCTURE:
            case UNION:
            case LIST:
            case SET:
            case MAP:
                return aggregate(field, self, id, traitArgs);
            case ENUM:
                return CodeBlock.of("$T.createEnum($L, $L$L$L)", SchemaRuntimeType.SCHEMA, id,
                                    stringSet(enumValues(shape)), enumClassArgument(shape), traitArgs);
            case INT_ENUM:
                return CodeBlock.of("$T.createIntEnum($L, $L$L)", SchemaRuntimeType.SCHEMA, id,
                                    intSet(intEnumValues(shape)), traitArgs);
            case STRING:
                Optional<EnumTrait> legacyEnum = shape.getTrait(EnumTrait.class);
                if (legacyEnum.isPresent()) {
                    return CodeBlock.of("$T.createEnum($L, $L$L$L)", SchemaRuntimeType.SCHEMA, id,
                                        stringSet(legacyEnum.get().getEnumDefinitionValues()), enumClassArgument(shape),
                                        traitArgs);
                }
                return CodeBlock.of("$T.createString($L$L)", SchemaRuntimeType.SCHEMA, id, traitArgs);
            default:
                return CodeBlock.of("$T.$L($L$L)", SchemaRuntimeType.SCHEMA, simpleFactory(shape), id, traitArgs);
        }
    }

    private CodeBlock aggregate(SchemaField field, ClassName self, CodeBlock id, CodeBlock traitArgs) {
        CodeBlock.Builder code = CodeBlock.builder();
        if (field.hasBuilder()) {
            code.add("$L", field.builderName());
        } else {
            code.add("$T.$L($L$L)", SchemaRuntimeType.SCHEMA, builderMethod(field.shape()), id, traitArgs);
        }
        code.add("$>");
        for (MemberShape member : SchemaFieldOrder.edges(field.shape())) {
            code.add("\n.putMember($S, $L$L)", member.getMemberName(), target(field, member, self),
                     SchemaTraits.arguments(traits.forShape(member)));
        }
        ClassName shapeClass = serde.builderClass(field.shape().getId());
        if (shapeClass != null) {
            code.add("\n.builderSupplier($T::newShapeBuilder)", shapeClass)
                .add("\n.shapeClass($T.class)", shapeClass);
        }
        code.add("\n.build()");
        if (field.hasDeferredMember()) {
            code.add("\n.resolve()");
        }
        code.add("$<");
        return code.build();
    }

    /**
     * Renders a copy of a structure's schema for a class outside the partitions: the same ShapeId, traits and members
     * as the structure's partition field, with the class's builder supplier and class. Member targets are read from
     * the partition fields, which are complete once their partition class is initialized.
     *
     * @param shapeId a structure or union of the closure. A member may target it only if a model class owns its
     *     partition field ({@link SchemaSerdeModel#builderClass}); the copy's member then references that field
     * @param shapeClass the class that holds the copy
     * @return the initializer expression
     */
    CodeBlock classSchemaInitializer(ShapeId shapeId, ClassName shapeClass) {
        SchemaField field = order.field(shapeId);
        if (field == null) {
            throw new IllegalStateException("Shape " + shapeId + " has no schema");
        }
        Shape shape = field.shape();
        CodeBlock.Builder code = CodeBlock.builder()
                                          .add("$T.$L($L$L)", SchemaRuntimeType.SCHEMA, builderMethod(shape),
                                               qualifiedShapeId(field.emittedId()),
                                               SchemaTraits.arguments(rootTraits(field)))
                                          .add("$>");
        for (MemberShape member : SchemaFieldOrder.edges(shape)) {
            if (member.getTarget().equals(shapeId) && serde.builderClass(shapeId) == null) {
                throw new IllegalStateException("Shape " + shapeId + " targets itself through " + member.getId());
            }
            code.add("\n.putMember($S, $L$L)", member.getMemberName(), qualifiedTarget(member),
                     SchemaTraits.arguments(traits.forShape(member)));
        }
        return code.add("\n.builderSupplier($T::newShapeBuilder)", shapeClass)
                   .add("\n.shapeClass($T.class)", shapeClass)
                   .add("\n.build()$<")
                   .build();
    }

    private CodeBlock qualifiedTarget(MemberShape member) {
        ShapeId targetId = member.getTarget();
        if (UNIT.equals(targetId) && serde.unitSchemaReference() != null) {
            return serde.unitSchemaReference();
        }
        if (Prelude.isPreludeShape(targetId)) {
            return CodeBlock.of("$T.$L", SchemaRuntimeType.PRELUDE_SCHEMAS, preludeField(targetId));
        }
        return fieldReference(order.field(targetId));
    }

    /**
     * {@code , X.class} for an enum with a generated enum class, else nothing.
     */
    private CodeBlock enumClassArgument(Shape shape) {
        ClassName enumClass = serde.enumClass(shape.getId());
        return enumClass == null ? CodeBlock.of("") : CodeBlock.of(", $T.class", enumClass);
    }

    private CodeBlock target(SchemaField from, MemberShape member, ClassName self) {
        ShapeId targetId = member.getTarget();
        if (UNIT.equals(targetId) && serde.unitSchemaReference() != null) {
            // The generated Unit class's schema, which has the builder that PreludeSchemas.UNIT lacks.
            return serde.unitSchemaReference();
        }
        if (Prelude.isPreludeShape(targetId)) {
            return CodeBlock.of("$T.$L", SchemaRuntimeType.PRELUDE_SCHEMAS, preludeField(targetId));
        }
        SchemaField target = order.field(targetId);
        if (from.isDeferred(member)) {
            return CodeBlock.of("$L", target.builderName());
        }
        if (target.partition() == from.partition()) {
            return CodeBlock.of("$L", target.fieldName());
        }
        return CodeBlock.of("$T.$L", partitionClass(target.partition()), target.fieldName());
    }

    /**
     * The shape's own included traits, plus {@code unitType} for a shape synthesized from {@code smithy.api#Unit}.
     */
    private List<CodeBlock> rootTraits(SchemaField field) {
        List<CodeBlock> rendered = traits.forShape(field.shape());
        if (field.isSyntheticUnit() && !field.shape().hasTrait(UnitTypeTrait.class)) {
            List<CodeBlock> withUnit = new ArrayList<>(rendered);
            withUnit.add(CodeBlock.of("$T.INSTANCE", SchemaRuntimeType.UNIT_TYPE_TRAIT));
            return withUnit;
        }
        return rendered;
    }

    private CodeBlock shapeId(ShapeId id) {
        if (id.getNamespace().equals(service.getId().getNamespace())) {
            return CodeBlock.of("$T.fromParts($L, $S)", SchemaRuntimeType.SHAPE_ID, SchemaFieldOrder.NAMESPACE_CONSTANT,
                                id.getName());
        }
        return qualifiedShapeId(id);
    }

    private static CodeBlock qualifiedShapeId(ShapeId id) {
        return CodeBlock.of("$T.fromParts($S, $S)", SchemaRuntimeType.SHAPE_ID, id.getNamespace(), id.getName());
    }

    /**
     * Estimates a field's static-initializer bytecode, including its builder field.
     */
    int estimate(SchemaField field) {
        Shape shape = field.shape();
        int rootTraits = BytecodeEstimate.traitArray(rootTraits(field));
        int head = BytecodeEstimate.SHAPE_ID + rootTraits + BytecodeEstimate.INSTRUCTION;
        int size;
        switch (shape.getType()) {
            case STRUCTURE:
            case UNION:
            case LIST:
            case SET:
            case MAP:
                size = field.hasBuilder() ? BytecodeEstimate.INSTRUCTION : head;
                for (MemberShape member : SchemaFieldOrder.edges(shape)) {
                    size += 3 * BytecodeEstimate.INSTRUCTION + BytecodeEstimate.traitArray(traits.forShape(member));
                }
                size += BytecodeEstimate.INSTRUCTION;
                if (serde.builderClass(shape.getId()) != null) {
                    size += 4 * BytecodeEstimate.INSTRUCTION;
                }
                if (field.hasDeferredMember()) {
                    size += BytecodeEstimate.INSTRUCTION;
                }
                if (field.hasBuilder()) {
                    size += head + BytecodeEstimate.INSTRUCTION;
                }
                break;
            case ENUM:
            case INT_ENUM:
            case STRING:
                size = head + setSize(shape);
                if (serde.enumClass(shape.getId()) != null) {
                    size += BytecodeEstimate.INSTRUCTION;
                }
                break;
            default:
                size = head;
                break;
        }
        return size + BytecodeEstimate.INSTRUCTION;
    }

    private int setSize(Shape shape) {
        int count;
        int perElement;
        switch (shape.getType()) {
            case ENUM:
                count = enumValues(shape).size();
                perElement = BytecodeEstimate.ARRAY_ELEMENT + BytecodeEstimate.INSTRUCTION;
                break;
            case INT_ENUM:
                count = intEnumValues(shape).size();
                perElement = BytecodeEstimate.ARRAY_ELEMENT + 2 * BytecodeEstimate.INSTRUCTION;
                break;
            default:
                Optional<EnumTrait> legacyEnum = shape.getTrait(EnumTrait.class);
                if (!legacyEnum.isPresent()) {
                    return 0;
                }
                count = legacyEnum.get().getEnumDefinitionValues().size();
                perElement = BytecodeEstimate.ARRAY_ELEMENT + BytecodeEstimate.INSTRUCTION;
                break;
        }
        return BytecodeEstimate.NEW + BytecodeEstimate.ARRAY + count * perElement + 2 * BytecodeEstimate.INSTRUCTION;
    }

    private static List<String> enumValues(Shape shape) {
        List<String> values = new ArrayList<>();
        for (MemberShape member : shape.members()) {
            values.add(member.expectTrait(EnumValueTrait.class).expectStringValue());
        }
        return values;
    }

    private static List<Integer> intEnumValues(Shape shape) {
        List<Integer> values = new ArrayList<>();
        for (MemberShape member : shape.members()) {
            values.add(member.expectTrait(EnumValueTrait.class).expectIntValue());
        }
        return values;
    }

    /**
     * Renders a {@code Set<String>} that keeps declaration order: {@code Collections.singleton(v)} or
     * {@code new LinkedHashSet<>(Arrays.asList(...))}.
     */
    private static CodeBlock stringSet(List<String> values) {
        List<CodeBlock> elements = new ArrayList<>(values.size());
        for (String value : new LinkedHashSet<>(values)) {
            elements.add(CodeBlock.of("$S", value));
        }
        return set(elements);
    }

    private static CodeBlock intSet(List<Integer> values) {
        List<CodeBlock> elements = new ArrayList<>(values.size());
        for (Integer value : new LinkedHashSet<>(values)) {
            elements.add(CodeBlock.of("$L", value));
        }
        return set(elements);
    }

    private static CodeBlock set(List<CodeBlock> elements) {
        if (elements.size() == 1) {
            return CodeBlock.of("$T.singleton($L)", Collections.class, elements.get(0));
        }
        return CodeBlock.of("new $T<>($T.asList($L))", LinkedHashSet.class, Arrays.class,
                            CodeBlock.join(elements, ", "));
    }

    private static String builderMethod(Shape shape) {
        switch (shape.getType()) {
            case STRUCTURE:
                return "structureBuilder";
            case UNION:
                return "unionBuilder";
            case LIST:
            case SET:
                return "listBuilder";
            case MAP:
                return "mapBuilder";
            default:
                throw new IllegalStateException("Shape " + shape.getId() + " of type " + shape.getType()
                                                + " has no members");
        }
    }

    private static String simpleFactory(Shape shape) {
        switch (shape.getType()) {
            case BLOB:
                return "createBlob";
            case BOOLEAN:
                return "createBoolean";
            case TIMESTAMP:
                return "createTimestamp";
            case BYTE:
                return "createByte";
            case SHORT:
                return "createShort";
            case INTEGER:
                return "createInteger";
            case LONG:
                return "createLong";
            case FLOAT:
                return "createFloat";
            case DOUBLE:
                return "createDouble";
            case BIG_INTEGER:
                return "createBigInteger";
            case BIG_DECIMAL:
                return "createBigDecimal";
            case DOCUMENT:
                return "createDocument";
            default:
                throw new SchemaCodegenException("Shape " + shape.getId() + " of type " + shape.getType()
                                                 + " has no schema");
        }
    }

    private static String preludeField(ShapeId id) {
        String field = PRELUDE_FIELDS.get(id.getName());
        if (field == null) {
            throw new SchemaCodegenException("Prelude shape " + id + " has no PreludeSchemas constant");
        }
        return field;
    }

    private static Map<String, String> preludeFields() {
        Map<String, String> fields = new HashMap<>();
        for (String name : Arrays.asList("Blob", "Boolean", "String", "Timestamp", "Byte", "Short", "Integer", "Long",
                                         "Float", "Double", "BigInteger", "BigDecimal", "Document", "PrimitiveBoolean",
                                         "PrimitiveByte", "PrimitiveShort", "PrimitiveInteger", "PrimitiveLong",
                                         "PrimitiveFloat", "PrimitiveDouble", "Unit")) {
            fields.put(name, SchemaFieldOrder.toSchemaName(name));
        }
        return Collections.unmodifiableMap(fields);
    }
}
