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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.poet.schema.SchemaFieldOrder.SchemaField;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Which generated model classes of a schema-enabled service implement {@code SerializableStruct} (or
 * {@code SmithyEnum}), and how their members map to the members of their Root_Schema (design A.6, task 3.7).
 *
 * <p>Built from the IntermediateModel and its Smithy model before the schema partitions are rendered, because the
 * partitions reference the classes ({@code X::newShapeBuilder}, {@code X.class}). Once the partitions are laid out,
 * {@link #attach(SchemasGenerator)} gives the model the schema fields, from which the model classes,
 * {@code <Svc>SharedSerde} and {@code <Svc>Operations} read their Schema references.
 *
 * <p>A class gets schema serde members when the class, every member's type and every structure it reaches support it.
 * Others keep the generated code of the previous generator, and their schema has no builder:
 * <ul>
 *     <li>event streams, events and shapes with streaming or {@code BigInteger} members (later tasks);</li>
 *     <li>a Smithy shape that several classes represent, unless at most one of them is not a request or response
 *     class, or a class whose members do not match its shape's members;</li>
 *     <li>members with the {@code ignoreDataTypeConversionFailures}, {@code attachPayloadTraitToMember} or
 *     {@code modelMarshallerDefaultValueSupplier} customizations, whose {@code SdkField} traits the schema cannot
 *     carry.</li>
 * </ul>
 * Request and response classes the IntermediateModel synthesizes for a {@code smithy.api#Unit} input or output get a
 * schema of their own ({@link Entry#isSynthetic()}). So does the {@code Unit} class it synthesizes for union members
 * that target {@code smithy.api#Unit} ({@link Entry#isUnit()}): the prelude shape has no partition field, and
 * {@code PreludeSchemas.UNIT} has no builder, so the class's schema is {@code smithy.api#Unit} with its builder, and
 * members that target {@code smithy.api#Unit} reference it ({@link #unitSchemaReference()}), as smithy-java's do
 * {@code Unit.SCHEMA}.
 *
 * <p>When the IntermediateModel generates several request and response classes from one Smithy structure (a structure
 * that is both an operation's input and its output, or the input of two operations), each class holds its own copy of
 * the structure's schema with its own builder ({@link Entry#isSharedShape()}): one Root_Schema names one
 * {@code shapeClass}. The partition field of the structure keeps no builder, unless a model class also represents the
 * structure (for example a structure that is an operation's input and also a member target, task 7.21): that class
 * owns the partition field, with its builder, and members that target the structure reference it.
 */
@SdkInternalApi
public final class SchemaSerdeModel {
    private static final SchemaSerdeModel NONE = new SchemaSerdeModel();
    private static final ShapeId UNIT = ShapeId.from("smithy.api#Unit");

    private final Map<String, Entry> entries;
    private final Map<ShapeId, Entry> byShapeId;
    private final Map<ShapeId, ClassName> enumClasses;
    private final Map<ShapeId, OperationClasses> operations;
    private final Map<ShapeId, CollectionSerde> collections;
    private final ClassName sharedSerdeClass;
    private final Protocol protocol;
    private final boolean requiredTraits;
    private final String serviceNamespace;
    private SchemasGenerator schemas;

    private SchemaSerdeModel() {
        this.entries = Collections.emptyMap();
        this.byShapeId = Collections.emptyMap();
        this.enumClasses = Collections.emptyMap();
        this.operations = Collections.emptyMap();
        this.collections = Collections.emptyMap();
        this.sharedSerdeClass = null;
        this.protocol = null;
        this.requiredTraits = false;
        this.serviceNamespace = null;
    }

    private SchemaSerdeModel(IntermediateModel model, Model smithy, String modelPackage, String serviceName) {
        this.sharedSerdeClass = ClassName.get(modelPackage, serviceName + "SharedSerde");
        this.protocol = model.getMetadata().getProtocol();
        CustomizationConfig customization = model.getCustomizationConfig();
        this.requiredTraits = customization.isRequiredTraitValidationEnabled();
        this.serviceNamespace = model.serviceId().getNamespace();

        Map<ShapeId, List<ShapeModel>> classesByShape = new HashMap<>();
        Map<ShapeId, ClassName> enums = new TreeMap<>();
        for (ShapeModel shape : new TreeMap<>(model.getShapes()).values()) {
            if (shape.getCustomization().isSkipGeneratingModelClass() || shape.shapeId() == null) {
                continue;
            }
            if (shape.getShapeType() == ShapeType.Enum) {
                // An enum constant named SCHEMA would clash with the SCHEMA field. An int enum class would need
                // SmithyIntEnum, which no generated enum class implements yet.
                boolean clashes = shape.getEnums() != null
                                  && shape.getEnums().stream().anyMatch(e -> "SCHEMA".equals(e.getName()));
                boolean intEnum = smithy.getShape(shape.shapeId()).map(Shape::isIntEnumShape).orElse(false);
                if (!clashes && !intEnum) {
                    enums.put(shape.shapeId(), ClassName.get(modelPackage, shape.getShapeName()));
                }
            } else {
                classesByShape.computeIfAbsent(shape.shapeId(), id -> new ArrayList<>()).add(shape);
            }
        }
        this.enumClasses = Collections.unmodifiableMap(enums);

        Map<String, Entry> candidates = new TreeMap<>();
        for (Map.Entry<ShapeId, List<ShapeModel>> classes : classesByShape.entrySet()) {
            boolean severalClasses = classes.getValue().size() > 1;
            long otherClasses = classes.getValue().stream().filter(s -> !isOperationShape(s)).count();
            if (severalClasses && otherClasses > 1) {
                continue;
            }
            Shape smithyShape = smithy.getShape(classes.getKey()).orElse(null);
            if (smithyShape == null) {
                continue;
            }
            for (ShapeModel shape : classes.getValue()) {
                // The one class that is not a request or response class, if any, owns the partition field.
                boolean shared = severalClasses && isOperationShape(shape);
                Entry entry = candidate(shape, smithyShape, modelPackage, customization, shared);
                if (entry != null) {
                    candidates.put(shape.getShapeName(), entry);
                }
            }
        }
        for (ShapeModel shape : new TreeMap<>(model.getShapes()).values()) {
            if (shape.shapeId() == null && isOperationShape(shape) && shape.getMembers().isEmpty()
                && !shape.getCustomization().isSkipGeneratingModelClass()) {
                candidates.put(shape.getShapeName(),
                               new Entry(shape, null, ClassName.get(modelPackage, shape.getShapeName()),
                                         Collections.emptyList(), false));
            }
        }
        removeUnsupportedReferences(candidates, smithy);
        this.entries = Collections.unmodifiableMap(candidates);

        Map<ShapeId, Entry> ids = new HashMap<>();
        for (Entry entry : candidates.values()) {
            if (entry.shapeId() != null && !entry.isSharedShape()) {
                ids.put(entry.shapeId(), entry);
            }
        }
        this.byShapeId = Collections.unmodifiableMap(ids);

        Map<ShapeId, OperationClasses> ops = new TreeMap<>();
        for (OperationModel operation : model.getOperations().values()) {
            if (operation.operationId() == null) {
                continue;
            }
            // The class names, rather than the linked shapes: a synthesized Unit output has no linked output shape.
            Entry input = operation.getInput() == null ? null : candidates.get(operation.getInput().getSimpleType());
            Entry output = operation.getReturnType() == null ? null
                                                             : candidates.get(operation.getReturnType().getReturnType());
            if (input != null && output != null) {
                ops.put(operation.operationId(), new OperationClasses(input, output));
            }
        }
        this.operations = Collections.unmodifiableMap(ops);

        Map<ShapeId, CollectionSerde> containers = new TreeMap<>();
        for (Entry entry : candidates.values()) {
            for (MemberBinding member : entry.members()) {
                if (member.member() != null) {
                    collectContainers(smithy, member.member(), containers);
                }
            }
        }
        this.collections = Collections.unmodifiableMap(containers);
    }

    /**
     * @return a model with no schema serde classes, for schema generation without an IntermediateModel
     */
    public static SchemaSerdeModel none() {
        return NONE;
    }

    /**
     * @param model an IntermediateModel built from a Smithy model
     * @param modelPackage the model package
     * @param serviceName the class name prefix of the service
     * @return the classes of the model that get schema serde members
     */
    public static SchemaSerdeModel create(IntermediateModel model, String modelPackage, String serviceName) {
        return new SchemaSerdeModel(model, model.smithyModel(), modelPackage, serviceName);
    }

    private static boolean isOperationShape(ShapeModel shape) {
        return shape.getShapeType() == ShapeType.Request || shape.getShapeType() == ShapeType.Response;
    }

    private static Entry candidate(ShapeModel shape, Shape smithyShape, String modelPackage,
                                   CustomizationConfig customization, boolean shared) {
        switch (shape.getShapeType()) {
            case Model:
            case Request:
            case Response:
            case Exception:
                break;
            default:
                return null;
        }
        if (!smithyShape.isStructureShape() && !smithyShape.isUnionShape()) {
            return null;
        }
        if (shape.isEvent() || shape.isEventStream() || shape.isDocument()
            || customization.getAttachPayloadTraitToMember().containsKey(shape.getC2jName())) {
            return null;
        }
        boolean isException = shape.getShapeType() == ShapeType.Exception;
        Map<String, MemberModel> imMembers = new LinkedHashMap<>();
        for (MemberModel member : shape.getMembers()) {
            imMembers.put(member.getC2jName(), member);
        }
        List<MemberBinding> members = new ArrayList<>();
        Set<String> constants = new HashSet<>();
        int index = 0;
        for (MemberShape smithyMember : smithyShape.members()) {
            String name = smithyMember.getMemberName();
            MemberModel member = imMembers.remove(name);
            if (member == null) {
                if (!isException || !"message".equalsIgnoreCase(name)) {
                    return null;
                }
            } else if (member.getHttp().getIsStreaming()) {
                // A streaming blob: in the schema, but not held by the class; the client method takes or returns it.
                members.add(new MemberBinding(name, index++, member, unique(constantName(name), constants), true));
                continue;
            } else if (!isSupportedMember(member, customization)) {
                return null;
            }
            members.add(new MemberBinding(name, index++, member, unique(constantName(name), constants), false));
        }
        if (!imMembers.isEmpty()) {
            return null;
        }
        return new Entry(shape, smithyShape.getId(), ClassName.get(modelPackage, shape.getShapeName()), members,
                         shared);
    }

    private static boolean isSupportedMember(MemberModel member, CustomizationConfig customization) {
        if (member.getHttp().getIsStreaming() || member.ignoreDataTypeConversionFailures()
            || member.isSynthetic() || customization.getModelMarshallerDefaultValueSupplier()
                                                    .containsKey(member.getC2jName())) {
            return false;
        }
        return ValueKind.of(member) != null;
    }

    /**
     * Removes candidates that reference, directly or through lists and maps, a structure without schema serde members,
     * until none does: their generated code writes those values with {@code writeStruct}.
     */
    private static void removeUnsupportedReferences(Map<String, Entry> candidates, Model smithy) {
        boolean changed = true;
        while (changed) {
            changed = false;
            Set<ShapeId> supported = new HashSet<>();
            for (Entry entry : candidates.values()) {
                if (entry.shapeId() != null && !entry.isSharedShape()) {
                    supported.add(entry.shapeId());
                }
            }
            for (String name : new ArrayList<>(candidates.keySet())) {
                for (MemberBinding member : candidates.get(name).members()) {
                    if (member.member() != null && !structuresSupported(member.member(), supported, smithy)) {
                        candidates.remove(name);
                        changed = true;
                        break;
                    }
                }
            }
        }
    }

    private static boolean structuresSupported(MemberModel member, Set<ShapeId> supported, Model smithy) {
        switch (ValueKind.of(member)) {
            case STRUCTURE:
                return member.memberId() != null
                       && supported.contains(smithy.expectShape(member.memberId(), MemberShape.class).getTarget());
            case LIST:
                return structuresSupported(member.getListModel().getListMemberModel(), supported, smithy);
            case MAP:
                return structuresSupported(member.getMapModel().getValueModel(), supported, smithy);
            default:
                return true;
        }
    }

    private static void collectContainers(Model smithy, MemberModel member, Map<ShapeId, CollectionSerde> containers) {
        ValueKind kind = ValueKind.of(member);
        if (kind != ValueKind.LIST && kind != ValueKind.MAP) {
            return;
        }
        ShapeId target = smithy.expectShape(member.memberId(), MemberShape.class).getTarget();
        if (!containers.containsKey(target)) {
            containers.put(target, new CollectionSerde(target, member));
        }
        if (kind == ValueKind.LIST) {
            collectContainers(smithy, member.getListModel().getListMemberModel(), containers);
        } else {
            collectContainers(smithy, member.getMapModel().getValueModel(), containers);
        }
    }

    static String constantName(String memberName) {
        return "M_" + SchemaFieldOrder.toSchemaName(memberName);
    }

    private static String unique(String name, Set<String> used) {
        String candidate = name;
        for (int i = 2; used.contains(candidate); i++) {
            candidate = name + "_" + i;
        }
        used.add(candidate);
        return candidate;
    }

    /**
     * Gives the model the laid-out schema fields.
     */
    void attach(SchemasGenerator schemas) {
        this.schemas = schemas;
        Set<String> used = new HashSet<>();
        for (CollectionSerde collection : collections.values()) {
            SchemaField field = schemas.order().field(collection.shapeId);
            if (field == null) {
                throw new IllegalStateException("CollectionSerde " + collection.shapeId + " has no schema");
            }
            collection.schemaReference = schemas.fieldReference(field);
            collection.className = sharedSerdeClass.nestedClass(unique(pascalCase(field.fieldName()) + "Serde", used));
        }
    }

    private static String pascalCase(String constantName) {
        StringBuilder result = new StringBuilder();
        for (String part : constantName.split("_")) {
            if (!part.isEmpty()) {
                result.append(part.charAt(0)).append(part.substring(1).toLowerCase(Locale.ENGLISH));
            }
        }
        return result.toString();
    }

    /**
     * @return whether any class gets schema serde members
     */
    public boolean isEnabled() {
        return sharedSerdeClass != null;
    }

    /**
     * @param shapeName the IntermediateModel shape name of a model class
     * @return its schema serde description, or null if the class keeps the previous generated code
     */
    public Entry entry(String shapeName) {
        return entries.get(shapeName);
    }

    /**
     * @return every class with schema serde members, by IntermediateModel shape name
     */
    public Map<String, Entry> entries() {
        return entries;
    }

    /**
     * @param shapeId a structure or union ShapeId of the Smithy model
     * @return the class whose {@code newShapeBuilder} its schema's builder supplier calls, or null
     */
    public ClassName builderClass(ShapeId shapeId) {
        Entry entry = byShapeId.get(shapeId);
        return entry == null ? null : entry.className();
    }

    /**
     * @param shapeId an enum ShapeId of the Smithy model
     * @return the generated enum class, or null
     */
    public ClassName enumClass(ShapeId shapeId) {
        return enumClasses.get(shapeId);
    }

    /**
     * @param shape an enum shape of the IntermediateModel
     * @return the expression of its schema if the enum implements {@code SmithyEnum}, else null
     */
    public CodeBlock enumSchemaReference(ShapeModel shape) {
        if (!isEnabled() || shape.shapeId() == null || !enumClasses.containsKey(shape.shapeId())) {
            return null;
        }
        SchemaField field = schemas.order().field(shape.shapeId());
        return field == null ? null : schemas.fieldReference(field);
    }

    /**
     * @param operationId an operation ShapeId
     * @return the operation's input and output classes, or null if either keeps the previous generated code
     */
    public OperationClasses operation(ShapeId operationId) {
        return operations.get(operationId);
    }

    /**
     * @return the list and map shapes that schema serde members hold, by ShapeId
     */
    public Map<ShapeId, CollectionSerde> collections() {
        return collections;
    }

    /**
     * @param member a list or map member of a schema serde class (or a list element or map value)
     * @param smithy the Smithy model
     * @return the {@code <Svc>SharedSerde} entry of its target
     */
    public CollectionSerde collection(MemberModel member, Model smithy) {
        ShapeId target = smithy.expectShape(member.memberId(), MemberShape.class).getTarget();
        CollectionSerde collection = collections.get(target);
        if (collection == null) {
            throw new IllegalStateException("No shared serde for " + target);
        }
        return collection;
    }

    /**
     * @return {@code <Svc>SharedSerde}
     */
    public ClassName sharedSerdeClass() {
        return sharedSerdeClass;
    }

    /**
     * @return the IntermediateModel protocol, which decides the derived {@code SdkField} location names
     */
    public Protocol protocol() {
        return protocol;
    }

    /**
     * @return whether derived {@code SdkField}s carry {@code RequiredTrait} ({@code requiredTraitValidationEnabled})
     */
    public boolean requiredTraits() {
        return requiredTraits;
    }

    /**
     * @return whether the protocol binds operation input and output members to HTTP locations
     */
    public boolean honorsHttpBindings() {
        return protocol == Protocol.REST_JSON || protocol == Protocol.REST_XML;
    }

    /**
     * @param entry a class
     * @return the expression of its Root_Schema in its own class: a partition field, or null for a class that holds its
     *     own schema ({@link Entry#hasOwnSchema()})
     */
    public CodeBlock schemaReference(Entry entry) {
        if (entry.hasOwnSchema()) {
            return null;
        }
        SchemaField field = schemas.order().field(entry.shapeId());
        if (field == null) {
            throw new IllegalStateException("Shape " + entry.shapeId() + " has no schema");
        }
        return schemas.fieldReference(field);
    }

    /**
     * @param entry a class of a shared shape ({@link Entry#isSharedShape()})
     * @return the initializer of the class's own copy of its structure's schema: the same ShapeId, traits and members
     *     as the partition field, with the class's builder supplier and class
     */
    public CodeBlock sharedShapeSchemaInitializer(Entry entry) {
        if (!entry.isSharedShape()) {
            throw new IllegalArgumentException(entry.className() + " is not a class of a shared shape");
        }
        return schemas.classSchemaInitializer(entry.shapeId(), entry.className());
    }

    /**
     * @return the target schema of members that target {@code smithy.api#Unit}: {@code Unit.SCHEMA} of the generated
     *     {@code Unit} class if it has schema serde members, else null ({@code PreludeSchemas.UNIT})
     */
    public CodeBlock unitSchemaReference() {
        ClassName unitClass = builderClass(UNIT);
        return unitClass == null ? null : CodeBlock.of("$T.$L", unitClass, "SCHEMA");
    }

    /**
     * @return the namespace of the ShapeIds of synthetic classes' schemas
     */
    public String serviceNamespace() {
        return serviceNamespace;
    }

    /**
     * A class with schema serde members.
     */
    public static final class Entry {
        private final ShapeModel shape;
        private final ShapeId shapeId;
        private final ClassName className;
        private final List<MemberBinding> members;
        private final boolean sharedShape;

        private Entry(ShapeModel shape, ShapeId shapeId, ClassName className, List<MemberBinding> members,
                      boolean sharedShape) {
            this.shape = shape;
            this.shapeId = shapeId;
            this.className = className;
            this.members = Collections.unmodifiableList(members);
            this.sharedShape = sharedShape;
        }

        public ShapeModel shape() {
            return shape;
        }

        /**
         * @return the Smithy shape, or null for a synthetic class
         */
        public ShapeId shapeId() {
            return shapeId;
        }

        public ClassName className() {
            return className;
        }

        /**
         * @return the members of the Root_Schema, in member index order
         */
        public List<MemberBinding> members() {
            return members;
        }

        /**
         * @return whether the IntermediateModel synthesized the class for a {@code smithy.api#Unit} input or output, so
         *     the class holds its own schema
         */
        public boolean isSynthetic() {
            return shapeId == null;
        }

        /**
         * @return whether the class is the {@code Unit} class the IntermediateModel synthesizes from the prelude
         *     {@code smithy.api#Unit} for union members that target it, so the class holds its own schema
         */
        public boolean isUnit() {
            return UNIT.equals(shapeId);
        }

        /**
         * @return whether the class is a request or response class of a Smithy structure that the IntermediateModel
         *     generates several classes from (for example a structure that is both an operation's input and its
         *     output, or also a model class), so the class holds its own copy of the structure's schema, with its own
         *     builder
         */
        public boolean isSharedShape() {
            return sharedShape;
        }

        /**
         * @return whether the class holds its own schema rather than referencing a partition field: a synthetic
         *     class, the {@code Unit} class or a class of a shared shape
         */
        public boolean hasOwnSchema() {
            return isSynthetic() || isUnit() || isSharedShape();
        }
    }

    /**
     * One member of a Root_Schema and the class member it reads and writes.
     */
    public static final class MemberBinding {
        private final String memberName;
        private final int index;
        private final MemberModel member;
        private final String constantName;
        private final boolean streaming;

        private MemberBinding(String memberName, int index, MemberModel member, String constantName, boolean streaming) {
            this.memberName = memberName;
            this.index = index;
            this.member = member;
            this.constantName = constantName;
            this.streaming = streaming;
        }

        public String memberName() {
            return memberName;
        }

        public int index() {
            return index;
        }

        /**
         * @return the class member, or null for the {@code message} member of an exception, which is the exception's
         *     message, and for a {@linkplain #isStreaming() streaming} member
         */
        public MemberModel member() {
            return streaming ? null : member;
        }

        /**
         * @return whether the member is a streaming blob (task 7.19): it is in the schema, but the class does not hold
         *     it, because the client method takes it as a {@code RequestBody}/{@code AsyncRequestBody} or hands it to a
         *     response transformer. The class neither writes nor reads it, and {@code getMemberValue} returns null
         */
        public boolean isStreaming() {
            return streaming;
        }

        /**
         * @return the name of the member schema constant
         */
        public String constantName() {
            return constantName;
        }
    }

    /**
     * The input and output classes of an operation.
     */
    public static final class OperationClasses {
        private final Entry input;
        private final Entry output;

        private OperationClasses(Entry input, Entry output) {
            this.input = input;
            this.output = output;
        }

        public Entry input() {
            return input;
        }

        public Entry output() {
            return output;
        }
    }

    /**
     * A list or map shape and the nested class of {@code <Svc>SharedSerde} that writes and reads it.
     */
    public static final class CollectionSerde {
        private final ShapeId shapeId;
        private final MemberModel member;
        private CodeBlock schemaReference;
        private ClassName className;

        private CollectionSerde(ShapeId shapeId, MemberModel member) {
            this.shapeId = shapeId;
            this.member = member;
        }

        public ShapeId shapeId() {
            return shapeId;
        }

        /**
         * @return a member that targets the collection, for its Java types
         */
        public MemberModel member() {
            return member;
        }

        /**
         * @return the expression of the collection's schema
         */
        public CodeBlock schemaReference() {
            return schemaReference;
        }

        public ClassName className() {
            return className;
        }
    }
}
