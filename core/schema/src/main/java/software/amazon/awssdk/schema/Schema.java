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

package software.amazon.awssdk.schema;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.traits.GenericTrait;
import software.amazon.awssdk.schema.traits.Trait;
import software.amazon.awssdk.utils.Validate;

/**
 * An immutable runtime description of a Smithy shape: its shape ID, shape type, serde-relevant traits and members.
 *
 * <p>A schema is either a root schema, which describes a non-member shape, or a member schema, which describes a
 * structure, union, list or map member. A member schema acts as its target: its {@link #type()}, {@link #members()},
 * list and map members and enum values are those of the target shape, and its traits are the target's traits
 * overridden by the member's own traits. {@link #type()} therefore never returns {@link ShapeType#MEMBER}.
 *
 * <p>Members are kept in model declaration order, and {@link #memberIndex()} is the position of a member in that
 * order. Shapes that refer to themselves, directly or through other shapes, are built with a {@link SchemaBuilder}
 * passed as a member target, then completed with {@code build().resolve()}.
 *
 * <p>Schemas are compared by identity. Each shape has one schema instance per generated client or per runtime model
 * conversion, and generated code compares member schemas with {@code ==}.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.schema.Schema}.
 */
@SdkPublicApi
public abstract class Schema implements MemberLookup {
    /**
     * Stands for a cached {@code null} extension value, so that a null array element means "not computed".
     */
    private static final Object NULL_EXTENSION = new Object();

    /**
     * The schema's traits. For a member schema, the target's traits merged with the member's own traits.
     */
    final TraitMap traits;

    /**
     * The traits declared on the shape itself. For a root schema, the same as {@link #traits}.
     */
    final TraitMap directTraits;

    /**
     * True for a member with the {@code required} trait and no default value (or a {@code null} default).
     */
    final boolean isRequiredByValidation;

    final Supplier<? extends ShapeBuilder<?>> builderSupplier;
    final Class<?> shapeClass;

    private final ShapeType type;
    private final ShapeId id;
    private final String memberName;
    private final int memberIndex;

    // Cached list and map member lookups. A race computes the same member instance, which is safely published
    // through its final fields, so the fields need no synchronization.
    private Schema listMember;
    private Schema mapKeyMember;
    private Schema mapValueMember;

    // Cached extension values indexed by SchemaExtensionKey.id, allocated under this schema's monitor when the first
    // value is published, so a schema whose extensions are never read pays only this field. A null element means "not
    // computed" and NULL_EXTENSION is a cached null. Each element is set once with compareAndSet, so every caller sees
    // the first published value, and the volatile element semantics publish that value safely.
    private volatile AtomicReferenceArray<Object> extensions;

    /**
     * Creates a root schema.
     */
    Schema(ShapeType type, ShapeId id, TraitMap traits, Supplier<? extends ShapeBuilder<?>> builderSupplier,
           Class<?> shapeClass) {
        this.type = type;
        this.id = id;
        this.traits = traits;
        this.directTraits = traits;
        this.memberName = null;
        this.memberIndex = -1;
        this.isRequiredByValidation = false;
        this.builderSupplier = builderSupplier;
        this.shapeClass = shapeClass;
    }

    /**
     * Creates a member schema.
     */
    Schema(MemberSchemaBuilder builder) {
        this.type = builder.type;
        this.id = builder.id;
        this.traits = builder.traits;
        this.directTraits = builder.directTraits;
        this.memberName = builder.memberName;
        this.memberIndex = builder.memberIndex;
        this.isRequiredByValidation = builder.isRequiredByValidation;
        this.builderSupplier = null;
        this.shapeClass = null;
    }

    /**
     * Creates a boolean schema.
     *
     * @param id the shape ID
     * @param traits the shape's traits
     * @return the schema
     */
    public static Schema createBoolean(ShapeId id, Trait... traits) {
        return createSimple(ShapeType.BOOLEAN, id, traits);
    }

    /**
     * Creates a byte schema.
     *
     * @param id the shape ID
     * @param traits the shape's traits
     * @return the schema
     */
    public static Schema createByte(ShapeId id, Trait... traits) {
        return createSimple(ShapeType.BYTE, id, traits);
    }

    /**
     * Creates a short schema.
     *
     * @param id the shape ID
     * @param traits the shape's traits
     * @return the schema
     */
    public static Schema createShort(ShapeId id, Trait... traits) {
        return createSimple(ShapeType.SHORT, id, traits);
    }

    /**
     * Creates an integer schema.
     *
     * @param id the shape ID
     * @param traits the shape's traits
     * @return the schema
     */
    public static Schema createInteger(ShapeId id, Trait... traits) {
        return createSimple(ShapeType.INTEGER, id, traits);
    }

    /**
     * Creates an int enum schema.
     *
     * @param id the shape ID
     * @param values the modeled enum values; copied, keeping iteration order
     * @param traits the shape's traits
     * @return the schema
     */
    public static Schema createIntEnum(ShapeId id, Set<Integer> values, Trait... traits) {
        return createIntEnum(id, values, null, traits);
    }

    /**
     * Creates an int enum schema associated with a generated enum class.
     *
     * @param id the shape ID
     * @param values the modeled enum values; copied, keeping iteration order
     * @param shapeClass the generated enum class, or null
     * @param traits the shape's traits
     * @return the schema
     */
    public static Schema createIntEnum(ShapeId id, Set<Integer> values, Class<?> shapeClass, Trait... traits) {
        Validate.paramNotNull(id, "id");
        return new RootSchema(ShapeType.INT_ENUM, id, TraitMap.create(traits), Collections.<String>emptySet(),
                              copyOf(values, "values"), shapeClass);
    }

    /**
     * Creates a long schema.
     *
     * @param id the shape ID
     * @param traits the shape's traits
     * @return the schema
     */
    public static Schema createLong(ShapeId id, Trait... traits) {
        return createSimple(ShapeType.LONG, id, traits);
    }

    /**
     * Creates a float schema.
     *
     * @param id the shape ID
     * @param traits the shape's traits
     * @return the schema
     */
    public static Schema createFloat(ShapeId id, Trait... traits) {
        return createSimple(ShapeType.FLOAT, id, traits);
    }

    /**
     * Creates a double schema.
     *
     * @param id the shape ID
     * @param traits the shape's traits
     * @return the schema
     */
    public static Schema createDouble(ShapeId id, Trait... traits) {
        return createSimple(ShapeType.DOUBLE, id, traits);
    }

    /**
     * Creates a bigInteger schema.
     *
     * @param id the shape ID
     * @param traits the shape's traits
     * @return the schema
     */
    public static Schema createBigInteger(ShapeId id, Trait... traits) {
        return createSimple(ShapeType.BIG_INTEGER, id, traits);
    }

    /**
     * Creates a bigDecimal schema.
     *
     * @param id the shape ID
     * @param traits the shape's traits
     * @return the schema
     */
    public static Schema createBigDecimal(ShapeId id, Trait... traits) {
        return createSimple(ShapeType.BIG_DECIMAL, id, traits);
    }

    /**
     * Creates a string schema.
     *
     * @param id the shape ID
     * @param traits the shape's traits
     * @return the schema
     */
    public static Schema createString(ShapeId id, Trait... traits) {
        return createSimple(ShapeType.STRING, id, traits);
    }

    /**
     * Creates a string enum schema.
     *
     * @param id the shape ID
     * @param values the modeled enum values; copied, keeping iteration order
     * @param traits the shape's traits
     * @return the schema
     */
    public static Schema createEnum(ShapeId id, Set<String> values, Trait... traits) {
        return createEnum(id, values, null, traits);
    }

    /**
     * Creates a string enum schema associated with a generated enum class.
     *
     * @param id the shape ID
     * @param values the modeled enum values; copied, keeping iteration order
     * @param shapeClass the generated enum class, or null
     * @param traits the shape's traits
     * @return the schema
     */
    public static Schema createEnum(ShapeId id, Set<String> values, Class<?> shapeClass, Trait... traits) {
        Validate.paramNotNull(id, "id");
        return new RootSchema(ShapeType.ENUM, id, TraitMap.create(traits), copyOf(values, "values"),
                              Collections.<Integer>emptySet(), shapeClass);
    }

    /**
     * Creates a blob schema.
     *
     * @param id the shape ID
     * @param traits the shape's traits
     * @return the schema
     */
    public static Schema createBlob(ShapeId id, Trait... traits) {
        return createSimple(ShapeType.BLOB, id, traits);
    }

    /**
     * Creates a document schema.
     *
     * @param id the shape ID
     * @param traits the shape's traits
     * @return the schema
     */
    public static Schema createDocument(ShapeId id, Trait... traits) {
        return createSimple(ShapeType.DOCUMENT, id, traits);
    }

    /**
     * Creates a timestamp schema.
     *
     * @param id the shape ID
     * @param traits the shape's traits
     * @return the schema
     */
    public static Schema createTimestamp(ShapeId id, Trait... traits) {
        return createSimple(ShapeType.TIMESTAMP, id, traits);
    }

    /**
     * Creates an operation schema.
     *
     * @param id the operation shape ID
     * @param traits the operation's traits
     * @return the schema
     */
    public static Schema createOperation(ShapeId id, Trait... traits) {
        return createSimple(ShapeType.OPERATION, id, traits);
    }

    /**
     * Creates a resource schema.
     *
     * @param id the resource shape ID
     * @param traits the resource's traits
     * @return the schema
     */
    public static Schema createResource(ShapeId id, Trait... traits) {
        return createSimple(ShapeType.RESOURCE, id, traits);
    }

    /**
     * Creates a service schema.
     *
     * @param id the service shape ID
     * @param traits the service's traits
     * @return the schema
     */
    public static Schema createService(ShapeId id, Trait... traits) {
        return createSimple(ShapeType.SERVICE, id, traits);
    }

    /**
     * Returns a builder for a structure schema.
     *
     * @param id the shape ID
     * @param traits the shape's traits
     * @return the builder
     */
    public static SchemaBuilder structureBuilder(ShapeId id, Trait... traits) {
        return new SchemaBuilder(id, ShapeType.STRUCTURE, traits);
    }

    /**
     * Returns a builder for a union schema.
     *
     * @param id the shape ID
     * @param traits the shape's traits
     * @return the builder
     */
    public static SchemaBuilder unionBuilder(ShapeId id, Trait... traits) {
        return new SchemaBuilder(id, ShapeType.UNION, traits);
    }

    /**
     * Returns a builder for a list schema. The list takes exactly one member, named {@code member}.
     *
     * @param id the shape ID
     * @param traits the shape's traits
     * @return the builder
     */
    public static SchemaBuilder listBuilder(ShapeId id, Trait... traits) {
        return new SchemaBuilder(id, ShapeType.LIST, traits);
    }

    /**
     * Returns a builder for a map schema. The map takes exactly two members, named {@code key} and {@code value}.
     *
     * @param id the shape ID
     * @param traits the shape's traits
     * @return the builder
     */
    public static SchemaBuilder mapBuilder(ShapeId id, Trait... traits) {
        return new SchemaBuilder(id, ShapeType.MAP, traits);
    }

    private static Schema createSimple(ShapeType type, ShapeId id, Trait... traits) {
        Validate.paramNotNull(id, "id");
        return new RootSchema(type, id, TraitMap.create(traits));
    }

    private static <T> Set<T> copyOf(Set<T> values, String paramName) {
        Validate.paramNotNull(values, paramName);
        if (values.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }

    /**
     * Returns the shape ID. For a member schema, the member ID ({@code namespace#Shape$member}).
     *
     * @return the shape ID
     */
    public final ShapeId id() {
        return id;
    }

    /**
     * Returns the shape type. A member schema returns its target's type, so this never returns
     * {@link ShapeType#MEMBER}.
     *
     * @return the shape type
     */
    public final ShapeType type() {
        return type;
    }

    /**
     * Returns whether this is a member schema.
     *
     * @return true for a member schema
     */
    public final boolean isMember() {
        return memberName != null;
    }

    /**
     * Returns the member name of a member schema.
     *
     * @return the Smithy member name, or null for a root schema
     */
    public final String memberName() {
        return memberName;
    }

    /**
     * Returns the position of a member in its container's members, in model declaration order, from 0. A root schema
     * returns -1.
     *
     * <p>The index is stable within one build of a generated client, but it can change between model versions. Do
     * not persist it.
     *
     * @return the member index, or -1 for a root schema
     */
    @SdkInternalApi
    public final int memberIndex() {
        return memberIndex;
    }

    /**
     * Returns the target schema of a member schema.
     *
     * @return the target schema, or null for a root schema
     * @throws IllegalStateException if the member targets a {@link SchemaBuilder} that has not been built yet
     */
    public Schema memberTarget() {
        return null;
    }

    /**
     * Ensures that this schema is a member that targets the given schema.
     *
     * @param target the schema this member must target; must not be null
     * @return this schema
     * @throws IllegalStateException if this schema is not a member that targets {@code target}
     */
    public final Schema assertMemberTargetIs(Schema target) {
        if (memberTarget() == target) {
            return this;
        }
        throw memberTargetMismatch(target, this);
    }

    // Port of smithy-java 2fd0f976f: building the exception here keeps assertMemberTargetIs below C2's default
    // 35-bytecode inlining limit (MaxInlineSize), so the generated deserializers that call it for every nested
    // member can inline it.
    private static IllegalStateException memberTargetMismatch(Schema target, Schema found) {
        Validate.paramNotNull(target, "target");
        return new IllegalStateException("Expected a member schema that targets " + target.id() + ", found " + found);
    }

    /**
     * Returns a typed trait. For a member schema, a trait declared on the member wins over the same trait declared on
     * its target.
     *
     * @param key the trait key
     * @param <T> the trait class
     * @return the trait, or null if absent
     */
    public final <T extends Trait> T getTrait(TraitKey<T> key) {
        return traits.get(key);
    }

    /**
     * Returns whether a typed trait is present, on the member or its target.
     *
     * @param key the trait key
     * @return true if present
     */
    public final boolean hasTrait(TraitKey<? extends Trait> key) {
        return traits.contains(key);
    }

    /**
     * Returns a typed trait that must be present.
     *
     * @param key the trait key
     * @param <T> the trait class
     * @return the trait
     * @throws NoSuchElementException if the trait is absent
     */
    public final <T extends Trait> T expectTrait(TraitKey<T> key) {
        T trait = traits.get(key);
        if (trait == null) {
            throw new NoSuchElementException("Expected trait not found on " + id + ": " + key.traitClass().getName());
        }
        return trait;
    }

    /**
     * Returns a typed trait only if it is declared on the shape itself. For a member schema, a trait declared only on
     * the target is not returned. For a root schema, this is the same as {@link #getTrait(TraitKey)}.
     *
     * @param key the trait key
     * @param <T> the trait class
     * @return the trait, or null if not declared directly
     */
    public final <T extends Trait> T getDirectTrait(TraitKey<T> key) {
        return directTraits.get(key);
    }

    /**
     * Returns a generic trait by its trait shape ID. Generic traits have no {@link TraitKey}. For a member schema, a
     * generic trait declared on the member wins over one with the same ID declared on its target. Typed traits are
     * not returned.
     *
     * @param traitId the trait shape ID
     * @return the generic trait, or null if absent
     */
    public final GenericTrait getGenericTrait(ShapeId traitId) {
        return traits.getGeneric(traitId);
    }

    /**
     * Returns a generic trait by its trait shape ID only if it is declared on the shape itself, like
     * {@link #getDirectTrait(TraitKey)}.
     *
     * @param traitId the trait shape ID
     * @return the generic trait, or null if not declared directly
     */
    public final GenericTrait getDirectGenericTrait(ShapeId traitId) {
        return directTraits.getGeneric(traitId);
    }

    /**
     * Returns every trait of the schema, typed and generic, each once. For a member schema, these are the merged
     * member and target traits.
     *
     * @return an unmodifiable collection of the traits
     */
    public final Collection<Trait> traits() {
        return traits.traits();
    }

    /**
     * Returns the extension data for a key, computing it on first access with the {@link SchemaExtensionProvider}
     * registered for the key.
     *
     * <p>The first call for this schema instance and key invokes the provider with this schema and caches the result,
     * including a {@code null} result. Later calls return the cached instance without invoking the provider. When
     * several threads make the first call at the same time, each may invoke the provider, but all of them, and every
     * later caller, receive the one value that was cached first. If the provider throws, the exception propagates
     * unchanged, nothing is cached, and the next call invokes the provider again.
     *
     * <p>Values are cached per schema instance. A member schema and its target, and a schema built from a recursive
     * {@link SchemaBuilder} and its {@linkplain #resolve() resolved} form, are different instances and each get their
     * own value.
     *
     * <p>The first call on any schema discovers the providers through {@link java.util.ServiceLoader}; see
     * {@link SchemaExtensionProvider}.
     *
     * @param key the extension key
     * @param <T> the extension data type
     * @return the extension data, or null if the provider returned null or no provider is registered for the key
     * @throws IllegalStateException if provider discovery finds two providers for one key
     */
    @SuppressWarnings("unchecked")
    public final <T> T getExtension(SchemaExtensionKey<T> key) {
        Validate.paramNotNull(key, "key");
        AtomicReferenceArray<Object> ext = extensions;
        int id = key.id;
        if (ext != null && id < ext.length()) {
            Object value = ext.get(id);
            if (value != null) {
                return value == NULL_EXTENSION ? null : (T) value;
            }
        }
        return computeExtension(key);
    }

    @SuppressWarnings("unchecked")
    private <T> T computeExtension(SchemaExtensionKey<T> key) {
        SchemaExtensionRegistry registry = SchemaExtensionRegistry.instance();
        SchemaExtensionProvider<T> provider = registry.providerFor(key);
        if (provider == null) {
            return null;
        }

        // Computed outside the lock: a provider may read extensions of other schemas (for example of member targets in
        // a recursive graph), and holding this schema's monitor meanwhile could deadlock with a thread doing the same
        // in the other direction. A throw leaves nothing cached.
        T value = provider.provide(this);
        Object computed = value == null ? NULL_EXTENSION : value;

        AtomicReferenceArray<Object> ext = extensions;
        if (ext == null) {
            synchronized (this) {
                ext = extensions;
                if (ext == null) {
                    // The registry is fixed once discovered, and a key with a provider has an ID below its size.
                    ext = new AtomicReferenceArray<>(registry.size());
                    extensions = ext;
                }
            }
        }
        if (ext.compareAndSet(key.id, null, computed)) {
            return value;
        }
        // Another thread published first; return its value so every caller sees one instance.
        Object existing = ext.get(key.id);
        return existing == NULL_EXTENSION ? null : (T) existing;
    }

    /**
     * Returns the members, in model declaration order. A member schema returns its target's members.
     *
     * @return an unmodifiable list of member schemas; empty for shapes without members
     */
    public List<Schema> members() {
        return memberData().list;
    }

    @Override
    public Schema member(String memberName) {
        return memberData().byName.get(memberName);
    }

    /**
     * Returns the member at a member index, as returned by {@link #memberIndex()}. A member schema looks the index up
     * in its target's members.
     *
     * @param memberIndex the member index
     * @return the member schema, or null if there is no member with that index
     */
    @SdkInternalApi
    public Schema member(int memberIndex) {
        Schema[] array = memberData().array;
        return memberIndex >= 0 && memberIndex < array.length ? array[memberIndex] : null;
    }

    /**
     * Returns the {@code member} member of a list schema.
     *
     * @return the list member
     * @throws IllegalStateException if this is not a list schema
     */
    public final Schema listMember() {
        Schema result = listMember;
        if (result == null) {
            result = expectMember(ShapeType.LIST, "member");
            listMember = result;
        }
        return result;
    }

    /**
     * Returns the {@code key} member of a map schema.
     *
     * @return the map key member
     * @throws IllegalStateException if this is not a map schema
     */
    public final Schema mapKeyMember() {
        Schema result = mapKeyMember;
        if (result == null) {
            result = expectMember(ShapeType.MAP, "key");
            mapKeyMember = result;
        }
        return result;
    }

    /**
     * Returns the {@code value} member of a map schema.
     *
     * @return the map value member
     * @throws IllegalStateException if this is not a map schema
     */
    public final Schema mapValueMember() {
        Schema result = mapValueMember;
        if (result == null) {
            result = expectMember(ShapeType.MAP, "value");
            mapValueMember = result;
        }
        return result;
    }

    private Schema expectMember(ShapeType expectedType, String name) {
        Schema result = type == expectedType ? member(name) : null;
        if (result == null) {
            throw new IllegalStateException("Expected a " + expectedType + " schema with a '" + name + "' member, found "
                                            + this);
        }
        return result;
    }

    /**
     * Returns the modeled values of a string enum. A member schema returns its target's values.
     *
     * @return an unmodifiable set of values; empty for shapes other than enums
     */
    public Set<String> stringEnumValues() {
        return Collections.emptySet();
    }

    /**
     * Returns the modeled values of an int enum. A member schema returns its target's values.
     *
     * @return an unmodifiable set of values; empty for shapes other than int enums
     */
    public Set<Integer> intEnumValues() {
        return Collections.emptySet();
    }

    /**
     * Returns a new builder for the generated class of this schema. Every call invokes the supplier given to
     * {@link SchemaBuilder#builderSupplier(Supplier)}, so it returns a new builder each time.
     *
     * @return a new builder, or null if the schema has no builder supplier (including every member schema)
     */
    public final ShapeBuilder<? extends SerializableShape> shapeBuilder() {
        return builderSupplier == null ? null : builderSupplier.get();
    }

    /**
     * Returns the generated class associated with this schema.
     *
     * @return the class, or null if there is none (including every member schema)
     */
    public final Class<?> shapeClass() {
        return shapeClass;
    }

    /**
     * Returns the required-member bitmask of a structure: bit {@code i} is set when the member with index {@code i} has
     * the {@code required} trait and no default value (or a {@code null} default). Only members with an index below
     * 64 have a bit. Required members at index 64 or more are not in the mask; presence checks find them from the
     * member schemas instead. A member schema returns its target's mask. Shapes other than structures return 0.
     *
     * @return the required-member bitmask
     */
    @SdkInternalApi
    public long requiredMemberMask() {
        return memberData().requiredMask;
    }

    /**
     * Returns the number of members with the {@code required} trait and no default value, including those with an
     * index of 64 or more. Shapes other than structures return 0.
     */
    int requiredMemberCount() {
        return memberData().requiredCount;
    }

    /**
     * Returns the member data of this schema, or of the target for a member schema.
     */
    abstract SchemaBuilder.Members memberData();

    /**
     * Returns the resolved form of this schema. A schema built from a {@link SchemaBuilder} whose members target other
     * builders must be resolved once all builders in the recursive group are built; other schemas return themselves.
     *
     * @return the resolved schema
     */
    public Schema resolve() {
        return this;
    }

    @Override
    public final String toString() {
        return "Schema{id='" + id + "', type=" + type + '}';
    }
}
