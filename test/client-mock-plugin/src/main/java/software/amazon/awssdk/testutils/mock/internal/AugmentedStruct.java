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

package software.amazon.awssdk.testutils.mock.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaBuilder;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.traits.Trait;
import software.amazon.awssdk.schema.traits.XmlNameTrait;

/**
 * A structure value whose schema is another structure's schema with extra string members appended, such as an error's
 * {@code __type} and {@code message}, which SDK exceptions do not model as members.
 *
 * <p>The augmented schema copies the base schema's traits and members, in order, so every base member keeps its member
 * index; codecs and HTTP binding plans that look members up by index then see the base members unchanged. An extra
 * member whose name, ignoring case, is already a base member is not added, and its value is not written.
 */
@SdkInternalApi
final class AugmentedStruct implements SerializableStruct {
    private final Schema schema;
    private final SerializableStruct delegate;
    private final Schema[] extraMembers;
    private final String[] extraValues;

    private AugmentedStruct(Schema schema, SerializableStruct delegate, Schema[] extraMembers, String[] extraValues) {
        this.schema = schema;
        this.delegate = delegate;
        this.extraMembers = extraMembers;
        this.extraValues = extraValues;
    }

    @Override
    public Schema sdkSchema() {
        return schema;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        for (int i = 0; i < extraMembers.length; i++) {
            if (extraMembers[i] != null && extraValues[i] != null) {
                serializer.writeString(extraMembers[i], extraValues[i]);
            }
        }
        delegate.serializeMembers(serializer);
    }

    @Override
    public <T> T getMemberValue(Schema member) {
        for (int i = 0; i < extraMembers.length; i++) {
            if (extraMembers[i] == member) {
                @SuppressWarnings("unchecked")
                T value = (T) extraValues[i];
                return value;
            }
        }
        Schema baseMember = delegate.sdkSchema().member(member.memberName());
        return baseMember == null ? null : delegate.getMemberValue(baseMember);
    }

    /**
     * Builds and caches augmented schemas for one set of extra members.
     */
    static final class Augmenter {
        private final String rootXmlName;
        private final List<String> extraNames;
        private final Map<Schema, Schema> cache = Collections.synchronizedMap(new IdentityHashMap<>());

        /**
         * @param rootXmlName the {@code xmlName} of the augmented structure, replacing the base one, or null to keep it
         * @param extraNames the names of the extra string members, written first and in this order
         */
        Augmenter(String rootXmlName, String... extraNames) {
            this.rootXmlName = rootXmlName;
            List<String> names = new ArrayList<>(extraNames.length);
            Collections.addAll(names, extraNames);
            this.extraNames = Collections.unmodifiableList(names);
        }

        /**
         * Returns the augmented schema of a base structure schema.
         */
        Schema augment(Schema base) {
            return cache.computeIfAbsent(base, this::build);
        }

        /**
         * Wraps a value so it serializes with the augmented schema and the given extra member values.
         *
         * @param value the base value
         * @param extraValues one value per extra member name, null to omit it
         * @return the augmented value
         */
        AugmentedStruct wrap(SerializableStruct value, String... extraValues) {
            Schema base = value.sdkSchema();
            Schema augmented = augment(base);
            Schema[] members = new Schema[extraNames.size()];
            for (int i = 0; i < members.length; i++) {
                String name = extraNames.get(i);
                members[i] = hasMember(base, name) ? null : augmented.member(name);
            }
            return new AugmentedStruct(augmented, value, members, extraValues.clone());
        }

        private Schema build(Schema base) {
            List<Trait> traits = new ArrayList<>();
            for (Trait trait : base.traits()) {
                if (rootXmlName == null || !(trait instanceof XmlNameTrait)) {
                    traits.add(trait);
                }
            }
            if (rootXmlName != null) {
                traits.add(new XmlNameTrait(rootXmlName));
            }
            SchemaBuilder builder = Schema.structureBuilder(base.id(), traits.toArray(new Trait[0]));
            for (Schema member : base.members()) {
                builder.putMember(member.memberName(), member.memberTarget(), member.traits().toArray(new Trait[0]));
            }
            for (String name : extraNames) {
                if (!hasMember(base, name)) {
                    builder.putMember(name, PreludeSchemas.STRING);
                }
            }
            Schema augmented = builder.build();
            for (Schema member : base.members()) {
                Schema copy = augmented.member(member.memberName());
                if (copy.memberIndex() != member.memberIndex()) {
                    throw new IllegalStateException("Unable to augment " + base.id() + ": member " + member.memberName()
                                                    + " moved from index " + member.memberIndex() + " to "
                                                    + copy.memberIndex());
                }
            }
            return augmented;
        }

        // Case-insensitive: a modeled "Message" member already carries the message that an extra "message" would write.
        private static boolean hasMember(Schema base, String name) {
            for (Schema member : base.members()) {
                if (member.memberName().equalsIgnoreCase(name)) {
                    return true;
                }
            }
            return false;
        }
    }
}
