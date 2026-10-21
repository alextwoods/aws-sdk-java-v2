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

package software.amazon.awssdk.dynamicclient.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.dynamicclient.schema.internal.ModelConversions;
import software.amazon.awssdk.dynamicclient.schema.internal.RuntimeTraitInclusion;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaBuilder;
import software.amazon.awssdk.schema.SchemaIndex;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.traits.Trait;
import software.amazon.awssdk.schema.traits.UnitTypeTrait;
import software.amazon.awssdk.utils.Validate;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.traits.synthetic.OriginalShapeIdTrait;

/**
 * Converts the shapes of a Smithy {@link Model} to runtime {@link Schema}s (Requirement 25), so that a client can work
 * with a model it was not generated from.
 *
 * <p>A converted schema has the shape's ShapeId, ShapeType and members in model declaration order. It carries exactly
 * the traits the SDK's default Trait_Inclusion_Policy includes, the same set that generated schemas carry: the
 * default traits, plus the traits of the protocol and auth definitions on the service, minus constraint traits and
 * documentation and C2J marker traits. Each included trait is created through the trait factory registry, so it is a
 * typed runtime trait where the SDK has one and a {@code GenericTrait} otherwise.
 *
 * <p>Each converter caches the schemas it creates, by ShapeId, so asking for the same shape again returns the same
 * schema instance, from any thread. A conversion that fails, for example because a member targets a shape that is not
 * in the model, caches nothing. Recursive shapes are supported: a member that targets an enclosing shape resolves to
 * the same schema instance the converter returns for that shape. Do not call {@link Schema#resolve()} on a converted
 * schema: that returns a copy, and member targets would then no longer be the instances the converter returns.
 *
 * <p>When a shape carries the {@code OriginalShapeIdTrait} that model transforms leave on renamed shapes, its schema
 * uses the original ShapeId, so that wire names derived from the ShapeId (such as the restXml root element) are the
 * modeled ones. A shape synthesized from {@code smithy.api#Unit} keeps its own ShapeId and carries the
 * {@code unitType} trait, as in generated schemas.
 *
 * <p>The prelude simple shapes ({@code smithy.api#String}, {@code smithy.api#PrimitiveInteger} and the others that
 * have a {@link PreludeSchemas} constant) convert to those constants, as generated schemas target them. Structure and
 * union schemas, {@code smithy.api#Unit} included, have a shape builder that creates a {@link StructDocument}.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.dynamicschemas.SchemaConverter}.
 */
@SdkPublicApi
@ThreadSafe
public final class SchemaConverter {
    private static final ShapeId UNIT = ShapeId.from("smithy.api#Unit");

    /**
     * The prelude simple shapes, which convert to the {@link PreludeSchemas} constants that generated schemas target.
     * {@code smithy.api#Unit} is converted like any structure, so that it has a document builder.
     */
    private static final Map<ShapeId, Schema> PRELUDE_SCHEMAS = preludeSchemas();

    private final Model model;
    private final software.amazon.awssdk.schema.ShapeId serviceId;
    private final RuntimeTraitInclusion traitInclusion;

    /**
     * Converted schemas, published only once the whole conversion that created them has succeeded.
     */
    private final ConcurrentMap<ShapeId, Schema> schemas = new ConcurrentHashMap<>();

    /**
     * Serializes conversions. The three collections below are guarded by it and are empty between conversions.
     */
    private final Object lock = new Object();

    /**
     * The builders of the aggregate shapes created by the conversion in progress. A member that targets a shape whose
     * builder is still {@link #building} targets the builder, which closes the cycle.
     */
    private final Map<ShapeId, SchemaBuilder> recursiveBuilders = new HashMap<>();

    /**
     * The aggregate shapes whose members the conversion in progress is adding.
     */
    private final Set<ShapeId> building = new HashSet<>();

    /**
     * The schemas created by the conversion in progress, added to {@link #schemas} when it succeeds.
     */
    private final Map<ShapeId, Schema> pending = new LinkedHashMap<>();

    /**
     * Creates a converter whose trait policy includes the protocol and auth traits of every service in the model.
     *
     * @param model the model whose shapes to convert
     * @throws NullPointerException if {@code model} is null
     */
    public SchemaConverter(Model model) {
        this.model = Validate.paramNotNull(model, "model");
        this.serviceId = null;
        this.traitInclusion = RuntimeTraitInclusion.forServices(model, model.getServiceShapes());
    }

    /**
     * Creates a converter whose trait policy is the one codegen uses for the given service: the default traits plus
     * the service's protocol and auth traits.
     *
     * <p>Structure and union shape builders are created with the service's ShapeId (see
     * {@link #createDocumentBuilder(Schema, software.amazon.awssdk.schema.ShapeId)}).
     *
     * @param model the model whose shapes to convert
     * @param service a service shape of the model
     * @throws NullPointerException if either argument is null
     * @throws IllegalArgumentException if the model does not contain the service
     */
    public SchemaConverter(Model model, ServiceShape service) {
        this.model = Validate.paramNotNull(model, "model");
        Validate.paramNotNull(service, "service");
        if (!service.equals(model.getShape(service.getId()).orElse(null))) {
            throw new IllegalArgumentException("Service " + service.getId() + " is not in the model");
        }
        this.serviceId = ModelConversions.shapeId(service.getId());
        this.traitInclusion = RuntimeTraitInclusion.forService(model, service);
    }

    /**
     * Creates a shape builder that reads a structure or union into a {@link StructDocument}.
     *
     * @param schema the structure or union schema that guides deserialization
     * @param serviceId the ShapeId of the service the documents belong to. It is reserved for resolving relative
     *     shape IDs in nested document discriminators, which the builder does not read yet (as in smithy-java)
     * @return the builder
     * @throws IllegalArgumentException if the schema is not a structure or union
     */
    public static ShapeBuilder<StructDocument> createDocumentBuilder(Schema schema,
                                                                     software.amazon.awssdk.schema.ShapeId serviceId) {
        Validate.paramNotNull(schema, "schema");
        Validate.paramNotNull(serviceId, "serviceId");
        return new SchemaGuidedDocumentBuilder(schema, serviceId);
    }

    /**
     * Creates a shape builder that reads a structure or union into a {@link StructDocument}, with the schema's own
     * ShapeId as the service ShapeId.
     *
     * @param schema the structure or union schema that guides deserialization
     * @return the builder
     * @throws IllegalArgumentException if the schema is not a structure or union
     */
    public static ShapeBuilder<StructDocument> createDocumentBuilder(Schema schema) {
        Validate.paramNotNull(schema, "schema");
        return createDocumentBuilder(schema, schema.id());
    }

    /**
     * Returns the schema of a shape in the model, converting it and every shape it targets on first use.
     *
     * <p>A member ShapeId ({@code ns#Shape$member}) returns that member's schema from its container's schema.
     *
     * @param id the ShapeId of a shape or member in the model
     * @return the schema; the same instance on every call
     * @throws IllegalArgumentException if the model has no shape with this ID, or a shape the shape targets
     *     (directly or through other shapes) is not in the model. The message names the missing ShapeId.
     */
    public Schema getSchema(ShapeId id) {
        Validate.paramNotNull(id, "id");
        Schema result = findSchema(id);
        if (result == null) {
            throw new IllegalArgumentException((id.hasMember() ? "Member " : "Shape ") + id + " is not in the model");
        }
        return result;
    }

    /**
     * Returns the schema of a shape of the model.
     *
     * @param shape a shape or member of the model
     * @return the schema; the same instance on every call
     * @throws IllegalArgumentException if the shape is not in the model, or a shape it targets is not in the model
     * @see #getSchema(ShapeId)
     */
    public Schema getSchema(Shape shape) {
        Validate.paramNotNull(shape, "shape");
        Shape modelShape = model.getShape(shape.getId()).orElse(null);
        if (modelShape != shape && !shape.equals(modelShape)) {
            throw new IllegalArgumentException("Shape " + shape.getId() + " is not in the converter's model");
        }
        return getSchema(shape.getId());
    }

    /**
     * Returns a schema index over this converter. {@link SchemaIndex#getSchema} converts on demand and returns null
     * for a ShapeId that is not in the model; {@link SchemaIndex#visit} visits the schemas converted so far.
     *
     * @return the schema index
     */
    public SchemaIndex getSchemaIndex() {
        return new SchemaIndex() {
            @Override
            public Schema getSchema(software.amazon.awssdk.schema.ShapeId id) {
                Validate.paramNotNull(id, "id");
                return findSchema(ModelConversions.smithyShapeId(id));
            }

            @Override
            public void visit(Consumer<Schema> visitor) {
                Validate.paramNotNull(visitor, "visitor");
                schemas.values().forEach(visitor);
            }
        };
    }

    /**
     * Returns the schema of a shape or member, or null if the model has no shape or member with this ID.
     */
    private Schema findSchema(ShapeId id) {
        Schema result = schemas.get(id);
        if (result != null) {
            return result;
        }
        if (id.hasMember()) {
            Schema container = findSchema(id.withoutMember());
            return container == null ? null : container.member(id.getMember().get());
        }
        Optional<Shape> shape = model.getShape(id);
        return shape.isPresent() ? convertRoot(shape.get()) : null;
    }

    /**
     * Converts a shape and its closure under the lock, and publishes the new schemas only if every one was created.
     */
    private Schema convertRoot(Shape shape) {
        if (Thread.holdsLock(lock)) {
            // Only code that runs during a conversion, such as a trait factory, can get here. Converting now would
            // clear the state of the conversion in progress.
            throw new IllegalStateException("Cannot convert " + shape.getId() + " while this SchemaConverter is "
                                            + "converting another shape");
        }
        synchronized (lock) {
            Schema cached = schemas.get(shape.getId());
            if (cached != null) {
                return cached;
            }
            try {
                Schema result = convert(shape);
                schemas.putAll(pending);
                return result;
            } finally {
                pending.clear();
                recursiveBuilders.clear();
                building.clear();
            }
        }
    }

    private Schema convert(Shape shape) {
        Schema result = schemas.get(shape.getId());
        if (result == null) {
            result = pending.get(shape.getId());
        }
        if (result == null) {
            result = createSchema(shape);
            Schema previous = pending.putIfAbsent(shape.getId(), result);
            if (previous != null) {
                result = previous;
            }
        }
        return result;
    }

    private Schema createSchema(Shape shape) {
        Schema prelude = PRELUDE_SCHEMAS.get(shape.getId());
        if (prelude != null) {
            return prelude;
        }
        switch (shape.getType()) {
            case LIST:
            case SET:
            case MAP:
            case STRUCTURE:
            case UNION:
                return getOrCreateRecursiveSchemaBuilder(shape).build();
            default:
                return createNonRecursiveSchema(shape);
        }
    }

    private Schema createNonRecursiveSchema(Shape shape) {
        software.amazon.awssdk.schema.ShapeId id = schemaId(shape);
        Trait[] traits = convertTraits(shape);
        switch (shape.getType()) {
            case BLOB:
                return Schema.createBlob(id, traits);
            case BOOLEAN:
                return Schema.createBoolean(id, traits);
            case STRING:
                Optional<EnumTrait> legacyEnum = shape.getTrait(EnumTrait.class);
                if (legacyEnum.isPresent()) {
                    return Schema.createEnum(id, new LinkedHashSet<>(legacyEnum.get().getEnumDefinitionValues()), traits);
                }
                return Schema.createString(id, traits);
            case TIMESTAMP:
                return Schema.createTimestamp(id, traits);
            case BYTE:
                return Schema.createByte(id, traits);
            case SHORT:
                return Schema.createShort(id, traits);
            case INTEGER:
                return Schema.createInteger(id, traits);
            case LONG:
                return Schema.createLong(id, traits);
            case FLOAT:
                return Schema.createFloat(id, traits);
            case DOUBLE:
                return Schema.createDouble(id, traits);
            case BIG_DECIMAL:
                return Schema.createBigDecimal(id, traits);
            case BIG_INTEGER:
                return Schema.createBigInteger(id, traits);
            case DOCUMENT:
                return Schema.createDocument(id, traits);
            case ENUM:
                return Schema.createEnum(id, enumValues(shape), traits);
            case INT_ENUM:
                return Schema.createIntEnum(id, intEnumValues(shape), traits);
            case OPERATION:
                return Schema.createOperation(id, traits);
            case SERVICE:
                return Schema.createService(id, traits);
            case RESOURCE:
                return Schema.createResource(id, traits);
            default:
                throw new IllegalArgumentException("Unexpected shape type " + shape.getType() + ": " + shape.getId());
        }
    }

    private SchemaBuilder getOrCreateRecursiveSchemaBuilder(Shape shape) {
        ShapeId shapeId = shape.getId();
        SchemaBuilder builder = recursiveBuilders.get(shapeId);
        if (builder == null) {
            builder = newBuilder(shape);
            if (shape.isStructureShape() || shape.isUnionShape()) {
                // Runtime consumers that call Schema.shapeBuilder() on a structure or union (such as the event
                // stream decoder) get a schema-guided document builder, as generated schemas give a model builder.
                builder.builderSupplier(() -> createDocumentBuilder(getSchema(shapeId), documentNamespace(shapeId)));
            }
            recursiveBuilders.put(shapeId, builder);
            building.add(shapeId);
            addMembers(shape, builder);
            building.remove(shapeId);
        }
        return builder;
    }

    private software.amazon.awssdk.schema.ShapeId documentNamespace(ShapeId shapeId) {
        return serviceId != null ? serviceId : getSchema(shapeId).id();
    }

    private SchemaBuilder newBuilder(Shape shape) {
        software.amazon.awssdk.schema.ShapeId id = schemaId(shape);
        Trait[] traits = convertTraits(shape);
        switch (shape.getType()) {
            case LIST:
            case SET:
                return Schema.listBuilder(id, traits);
            case MAP:
                return Schema.mapBuilder(id, traits);
            case STRUCTURE:
                return Schema.structureBuilder(id, traits);
            case UNION:
                return Schema.unionBuilder(id, traits);
            default:
                throw new IllegalArgumentException("Expected an aggregate shape: " + shape.getId());
        }
    }

    private void addMembers(Shape shape, SchemaBuilder builder) {
        for (MemberShape member : shape.members()) {
            Optional<Shape> target = model.getShape(member.getTarget());
            if (!target.isPresent()) {
                throw new IllegalArgumentException("Shape " + member.getTarget() + " targeted by member "
                                                   + member.getId() + " is not in the model");
            }
            Trait[] memberTraits = convertTraits(member);
            if (building.contains(target.get().getId())) {
                // The target is being built: a cycle. Target its builder, which is built when its shape completes.
                builder.putMember(member.getMemberName(), getOrCreateRecursiveSchemaBuilder(target.get()), memberTraits);
            } else {
                builder.putMember(member.getMemberName(), convert(target.get()), memberTraits);
            }
        }
    }

    private Trait[] convertTraits(Shape shape) {
        List<software.amazon.smithy.model.traits.Trait> included = traitInclusion.includedTraits(shape);
        List<Trait> result = new ArrayList<>(included.size() + 1);
        for (software.amazon.smithy.model.traits.Trait trait : included) {
            result.add(ModelConversions.trait(trait));
        }
        if (isSyntheticUnit(shape) && !shape.hasTrait(software.amazon.smithy.model.traits.UnitTypeTrait.class)) {
            result.add(UnitTypeTrait.INSTANCE);
        }
        return result.toArray(new Trait[0]);
    }

    /**
     * Returns the ShapeId a shape's schema carries: the original ID that a model transform recorded with
     * {@code OriginalShapeIdTrait} (Requirement 25.6), except for shapes synthesized from {@code smithy.api#Unit},
     * which keep their own ID, as in the Schema_Generator.
     */
    private static software.amazon.awssdk.schema.ShapeId schemaId(Shape shape) {
        ShapeId original = originalId(shape);
        return ModelConversions.shapeId(original == null || UNIT.equals(original) ? shape.getId() : original);
    }

    private static boolean isSyntheticUnit(Shape shape) {
        return UNIT.equals(originalId(shape));
    }

    private static ShapeId originalId(Shape shape) {
        return shape.getTrait(OriginalShapeIdTrait.class).map(OriginalShapeIdTrait::getOriginalId).orElse(null);
    }

    private static Map<ShapeId, Schema> preludeSchemas() {
        Schema[] schemas = {
            PreludeSchemas.BLOB, PreludeSchemas.BOOLEAN, PreludeSchemas.STRING, PreludeSchemas.TIMESTAMP,
            PreludeSchemas.BYTE, PreludeSchemas.SHORT, PreludeSchemas.INTEGER, PreludeSchemas.LONG, PreludeSchemas.FLOAT,
            PreludeSchemas.DOUBLE, PreludeSchemas.BIG_INTEGER, PreludeSchemas.BIG_DECIMAL, PreludeSchemas.DOCUMENT,
            PreludeSchemas.PRIMITIVE_BOOLEAN, PreludeSchemas.PRIMITIVE_BYTE, PreludeSchemas.PRIMITIVE_SHORT,
            PreludeSchemas.PRIMITIVE_INTEGER, PreludeSchemas.PRIMITIVE_LONG, PreludeSchemas.PRIMITIVE_FLOAT,
            PreludeSchemas.PRIMITIVE_DOUBLE
        };
        Map<ShapeId, Schema> result = new HashMap<>();
        for (Schema schema : schemas) {
            result.put(ModelConversions.smithyShapeId(schema.id()), schema);
        }
        return Collections.unmodifiableMap(result);
    }

    private static Set<String> enumValues(Shape shape) {
        Set<String> values = new LinkedHashSet<>();
        for (MemberShape member : shape.members()) {
            values.add(member.expectTrait(EnumValueTrait.class).expectStringValue());
        }
        return values;
    }

    private static Set<Integer> intEnumValues(Shape shape) {
        Set<Integer> values = new LinkedHashSet<>();
        for (MemberShape member : shape.members()) {
            values.add(member.expectTrait(EnumValueTrait.class).expectIntValue());
        }
        return values;
    }
}
