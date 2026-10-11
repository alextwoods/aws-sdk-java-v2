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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.traits.Trait;
import software.amazon.awssdk.utils.Validate;

/**
 * Builds a schema for a shape that has members: a structure, union, list or map.
 *
 * <p>Members are kept in the order they are added, which should be the model declaration order; that order defines
 * each member's {@link Schema#memberIndex()}. A list takes exactly one member named {@code member}, and a map exactly
 * two, named {@code key} and {@code value}.
 *
 * <p>For recursive shapes, a builder can be passed as the target of another shape's member before it is built. A
 * schema with such a member is completed lazily, and the builder's own schema is obtained with
 * {@code build().resolve()} once every builder in the recursive group has been built. Accessing the target of a member
 * whose builder has not been built yet fails with an {@link IllegalStateException}.
 *
 * <p>Once {@link #build()} is called the builder can no longer be changed. A builder is not thread-safe; build it on
 * one thread and publish the resulting schema safely.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.schema.SchemaBuilder}.
 */
@SdkPublicApi
public final class SchemaBuilder {
    final ShapeId id;
    final ShapeType type;
    final TraitMap traits;

    private final List<MemberSchemaBuilder> members;
    private Supplier<? extends ShapeBuilder<?>> builderSupplier;
    private Class<?> shapeClass;

    /**
     * The built schema: null before {@link #build()}, then the root schema, replaced by the resolved schema when a
     * deferred root schema is resolved. Volatile because deferred member schemas read it to find their target.
     */
    private volatile Schema builtShape;

    SchemaBuilder(ShapeId id, ShapeType type, Trait... traits) {
        Validate.paramNotNull(id, "id");
        Validate.paramNotNull(type, "type");
        if (type != ShapeType.STRUCTURE && type != ShapeType.UNION && type != ShapeType.LIST && type != ShapeType.MAP) {
            throw new IllegalArgumentException("Shape " + id + " of type " + type + " does not take members");
        }
        if (id.hasMember()) {
            throw new IllegalArgumentException("Shape ID of a " + type + " schema must not have a member: " + id);
        }
        this.id = id;
        this.type = type;
        this.traits = TraitMap.create(traits);
        this.members = new ArrayList<>(type == ShapeType.LIST ? 1 : type == ShapeType.MAP ? 2 : 8);
    }

    /**
     * Adds a member that targets a built schema.
     *
     * @param name the Smithy member name
     * @param target the target schema; must not be a member schema
     * @param traits the traits declared on the member
     * @return this builder
     * @throws IllegalArgumentException if the name is already used, is not valid for a list or map, or the target is a
     *                                  member schema
     * @throws IllegalStateException if the builder has already been built
     */
    public SchemaBuilder putMember(String name, Schema target, Trait... traits) {
        Validate.paramNotNull(name, "name");
        Validate.paramNotNull(target, "target");
        if (target.isMember()) {
            throw new IllegalArgumentException("Member " + id + "$" + name + " cannot target a member schema: "
                                               + target.id());
        }
        checkMember(name);
        members.add(new MemberSchemaBuilder(this, name, target, null, traits));
        return this;
    }

    /**
     * Adds a member that targets a shape whose builder may not be built yet. Used for recursive shapes.
     *
     * @param name the Smithy member name
     * @param target the builder of the target shape
     * @param traits the traits declared on the member
     * @return this builder
     * @throws IllegalArgumentException if the name is already used or is not valid for a list or map
     * @throws IllegalStateException if the builder has already been built
     */
    public SchemaBuilder putMember(String name, SchemaBuilder target, Trait... traits) {
        Validate.paramNotNull(name, "name");
        Validate.paramNotNull(target, "target");
        checkMember(name);
        members.add(new MemberSchemaBuilder(this, name, null, target, traits));
        return this;
    }

    /**
     * Sets the supplier of new builders for the generated class of this shape. {@link Schema#shapeBuilder()} invokes
     * it on every call.
     *
     * <p>Generated code passes a method reference such as {@code GetQueueUrlRequest::newShapeBuilder}.
     *
     * @param builderSupplier the supplier, or null for none
     * @return this builder
     * @throws IllegalStateException if the builder has already been built
     */
    public SchemaBuilder builderSupplier(Supplier<? extends ShapeBuilder<?>> builderSupplier) {
        checkNotBuilt();
        this.builderSupplier = builderSupplier;
        return this;
    }

    /**
     * Sets the generated class associated with this shape.
     *
     * @param shapeClass the class, or null for none
     * @return this builder
     * @throws IllegalStateException if the builder has already been built
     */
    public SchemaBuilder shapeClass(Class<?> shapeClass) {
        checkNotBuilt();
        this.shapeClass = shapeClass;
        return this;
    }

    /**
     * Builds the schema. Later calls return the same schema, or the resolved schema once it has been resolved.
     *
     * <p>If any member targets a {@link SchemaBuilder}, the returned schema resolves its members when they are first
     * accessed, and {@link Schema#resolve()} must be called on it once every builder in the recursive group is built.
     *
     * @return the schema
     * @throws IllegalArgumentException if a list does not have its {@code member} member, a map does not have both its
     *                                  {@code key} and {@code value} members, or a union has no member
     */
    public Schema build() {
        Schema result = builtShape;
        if (result != null) {
            return result;
        }

        switch (type) {
            case LIST:
                requireMember("member");
                break;
            case MAP:
                requireMember("key");
                requireMember("value");
                break;
            case UNION:
                if (members.isEmpty()) {
                    throw new IllegalArgumentException("Union shape " + id + " requires at least one member");
                }
                break;
            default:
                break;
        }

        boolean hasDeferredMember = false;
        for (MemberSchemaBuilder member : members) {
            if (member.targetBuilder != null) {
                hasDeferredMember = true;
                break;
            }
        }

        List<MemberSchemaBuilder> memberBuilders = Collections.unmodifiableList(new ArrayList<>(members));
        if (hasDeferredMember) {
            result = new DeferredRootSchema(type, id, traits, memberBuilders, builderSupplier, shapeClass, this);
        } else {
            result = new RootSchema(type, id, traits, buildMembers(type, memberBuilders), builderSupplier, shapeClass);
        }
        builtShape = result;
        return result;
    }

    /**
     * Returns the built schema, or null if {@link #build()} has not been called.
     */
    Schema builtShape() {
        return builtShape;
    }

    /**
     * Replaces this builder's deferred root schema with its resolved form, once, and returns the resolved form.
     */
    synchronized Schema resolve(DeferredRootSchema deferred, Members resolvedMembers) {
        Schema current = builtShape;
        if (current == deferred) {
            current = new ResolvedRootSchema(deferred, resolvedMembers);
            builtShape = current;
        }
        return current;
    }

    private void checkNotBuilt() {
        if (builtShape != null) {
            throw new IllegalStateException("Cannot change the builder of " + id + " after it has been built");
        }
    }

    private void checkMember(String name) {
        checkNotBuilt();
        if (type == ShapeType.LIST && !"member".equals(name)) {
            throw new IllegalArgumentException("List shape " + id + " takes only a member named 'member', not '" + name
                                               + "'");
        }
        if (type == ShapeType.MAP && !"key".equals(name) && !"value".equals(name)) {
            throw new IllegalArgumentException("Map shape " + id + " takes only members named 'key' and 'value', not '"
                                               + name + "'");
        }
        if (findMember(name) != null) {
            throw new IllegalArgumentException("Duplicate member '" + name + "' on shape " + id);
        }
    }

    private void requireMember(String name) {
        if (findMember(name) == null) {
            throw new IllegalArgumentException(type + " shape " + id + " is missing its '" + name + "' member ("
                                               + id.withMember(name) + ")");
        }
    }

    private MemberSchemaBuilder findMember(String name) {
        for (MemberSchemaBuilder member : members) {
            if (member.memberName.equals(name)) {
                return member;
            }
        }
        return null;
    }

    int memberCount() {
        return members.size();
    }

    /**
     * Builds the member schemas of a container, in the order given.
     */
    static Members buildMembers(ShapeType type, List<MemberSchemaBuilder> memberBuilders) {
        if (memberBuilders.isEmpty()) {
            return Members.NONE;
        }
        Schema[] array = new Schema[memberBuilders.size()];
        int requiredCount = 0;
        long requiredMask = 0L;
        for (int i = 0; i < array.length; i++) {
            Schema member = memberBuilders.get(i).build();
            array[i] = member;
            if (type == ShapeType.STRUCTURE && member.isRequiredByValidation) {
                requiredCount++;
                if (i < Long.SIZE) {
                    requiredMask |= 1L << i;
                }
            }
        }
        return new Members(array, requiredCount, requiredMask);
    }

    /**
     * The built member schemas of a root schema, with name and index lookups and the required-member summary.
     */
    static final class Members {
        static final Members NONE = new Members(new Schema[0], 0, 0L);

        final Schema[] array;
        final List<Schema> list;
        final Map<String, Schema> byName;
        final int requiredCount;
        final long requiredMask;

        private Members(Schema[] array, int requiredCount, long requiredMask) {
            this.array = array;
            this.list = array.length == 0 ? Collections.<Schema>emptyList()
                                          : Collections.unmodifiableList(Arrays.asList(array));
            this.byName = createByName(array);
            this.requiredCount = requiredCount;
            this.requiredMask = requiredMask;
        }

        private static Map<String, Schema> createByName(Schema[] array) {
            switch (array.length) {
                case 0:
                    return Collections.emptyMap();
                case 1:
                    return Collections.singletonMap(array[0].memberName(), array[0]);
                case 2:
                    return new Map2<>(array[0].memberName(), array[0], array[1].memberName(), array[1]);
                default:
                    Map<String, Schema> map = new HashMap<>((int) (array.length / 0.75f) + 1);
                    for (Schema member : array) {
                        map.put(member.memberName(), member);
                    }
                    return Collections.unmodifiableMap(map);
            }
        }
    }

    /**
     * An immutable two-entry map, smaller and faster than a hash map for map key/value members and two-member
     * structures.
     */
    private static final class Map2<K, V> extends AbstractMap<K, V> {
        private final K firstKey;
        private final V firstValue;
        private final K secondKey;
        private final V secondValue;
        private final Set<Entry<K, V>> entries;

        Map2(K firstKey, V firstValue, K secondKey, V secondValue) {
            this.firstKey = firstKey;
            this.firstValue = firstValue;
            this.secondKey = secondKey;
            this.secondValue = secondValue;
            Set<Entry<K, V>> entrySet = new LinkedHashSet<>(2);
            entrySet.add(new SimpleImmutableEntry<>(firstKey, firstValue));
            entrySet.add(new SimpleImmutableEntry<>(secondKey, secondValue));
            this.entries = Collections.unmodifiableSet(entrySet);
        }

        @Override
        public boolean containsKey(Object key) {
            return firstKey.equals(key) || secondKey.equals(key);
        }

        @Override
        public V get(Object key) {
            if (firstKey.equals(key)) {
                return firstValue;
            }
            if (secondKey.equals(key)) {
                return secondValue;
            }
            return null;
        }

        @Override
        public int size() {
            return 2;
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return entries;
        }
    }
}
