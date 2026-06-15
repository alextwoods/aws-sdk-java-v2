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

package software.amazon.awssdk.bridge.smithyjava.serde;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.ListTrait;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.core.traits.MapTrait;
import software.amazon.awssdk.core.traits.PayloadTrait;
import software.amazon.awssdk.core.traits.RequiredTrait;
import software.amazon.awssdk.core.traits.Trait;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SchemaExtensionKey;
import software.amazon.smithy.java.core.schema.SchemaExtensionProvider;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;

/**
 * The reusable core of v2 backward compatibility: lazily derives a v2-style {@code List<SdkField<?>>}
 * from a <b>native smithy-java</b> {@code $SCHEMA}, caching it on the schema via a
 * {@link SchemaExtensionProvider}. This is the {@code smithy -> c2j} direction — the inverse of
 * {@link SdkSchemaFactory} — and it runs <b>only if</b> a legacy code path actually calls
 * {@code SdkPojo.sdkFields()} (lazy: computed once per schema on first access, then read as a free
 * extension-array slot).
 *
 * <p>Each derived {@link SdkField} is fully generic — no per-shape code:
 * <ul>
 *   <li>getter: {@code pojo -> ((SerializableStruct) pojo).getMemberValue(memberSchema)}</li>
 *   <li>setter: {@code (builder, v) -> ((ShapeBuilder) builder).setMemberValue(memberSchema, v)}</li>
 * </ul>
 * so a generated v2-API wrapper can implement {@code sdkFields()} by returning
 * {@code SdkFieldsFromSchema.of(NativeType.$SCHEMA)} and delegate state to the native struct.
 */
@SdkProtectedApi
public final class SdkFieldsFromSchema implements SchemaExtensionProvider<List<SdkField<?>>> {

    /** Extension key holding the lazily-derived SdkField list for a (structure) schema. */
    public static final SchemaExtensionKey<List<SdkField<?>>> KEY = new SchemaExtensionKey<>();

    /** Public no-arg constructor required by ServiceLoader. */
    public SdkFieldsFromSchema() {
    }

    /** Lazily get (and cache on the schema) the v2 SdkField list for a native structure schema. */
    public static List<SdkField<?>> of(Schema schema) {
        List<SdkField<?>> fields = schema.getExtension(KEY);
        return fields != null ? fields : build(schema);
    }

    @Override
    public SchemaExtensionKey<List<SdkField<?>>> key() {
        return KEY;
    }

    @Override
    public List<SdkField<?>> provide(Schema schema) {
        ShapeType t = schema.type();
        if (t != ShapeType.STRUCTURE && t != ShapeType.UNION) {
            return null;
        }
        return build(schema);
    }

    private static List<SdkField<?>> build(Schema schema) {
        List<Schema> members = schema.members();
        List<SdkField<?>> fields = new ArrayList<>(members.size());
        for (Schema member : members) {
            fields.add(field(member));
        }
        return fields;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static SdkField<?> field(Schema member) {
        MarshallingType type = marshallingType(member);
        // Generic getter/setter: the v2 POJO IS (wraps) a native SerializableStruct/ShapeBuilder.
        java.util.function.Function<Object, Object> getter = pojo -> getMember(pojo, member);
        java.util.function.BiConsumer<Object, Object> setter = (obj, val) -> setMember(obj, member, val);
        SdkField.Builder builder = SdkField.builder(type)
                .memberName(member.memberName())
                .getter(getter)
                .setter(setter);

        List<Trait> traits = new ArrayList<>(2);
        // v2 SdkField REQUIRES a LocationTrait (SdkField ctor dereferences it). HTTP-bound members
        // map to PATH/HEADER/QUERY; everything else is a body member, which v2 models as
        // MarshallLocation.PAYLOAD with the wire name (jsonName/xmlName if present, else member name).
        LocationTrait loc = locationTrait(member);
        if (loc == null) {
            loc = LocationTrait.builder().location(MarshallLocation.PAYLOAD)
                    .locationName(bodyWireName(member)).build();
        }
        traits.add(loc);
        if (member.getTrait(TraitKey.get(HttpPayloadTrait.class)) != null) {
            traits.add(PayloadTrait.create());
        }
        if (member.getTrait(TraitKey.REQUIRED_TRAIT) != null) {
            traits.add(RequiredTrait.create());
        }
        // List/Map need element SdkField so nested serde/copiers work.
        if (type == MarshallingType.LIST) {
            traits.add(ListTrait.builder()
                    .memberFieldInfo(field(member.listMember()))
                    .build());
        } else if (type == MarshallingType.MAP) {
            traits.add(MapTrait.builder()
                    .valueFieldInfo(field(member.mapValueMember()))
                    .build());
        }
        if (!traits.isEmpty()) {
            builder.traits(traits.toArray(new Trait[0]));
        }
        return builder.build();
    }

    private static Object getMember(Object pojo, Schema member) {
        return pojo instanceof SerializableStruct ? ((SerializableStruct) pojo).getMemberValue(member) : null;
    }

    private static void setMember(Object builder, Schema member, Object value) {
        if (builder instanceof ShapeBuilder) {
            ((ShapeBuilder<?>) builder).setMemberValue(member, value);
        }
    }

    private static String bodyWireName(Schema member) {
        software.amazon.smithy.model.traits.JsonNameTrait json =
                member.getTrait(TraitKey.JSON_NAME_TRAIT);
        if (json != null) {
            return json.getValue();
        }
        software.amazon.smithy.model.traits.XmlNameTrait xml =
                member.getTrait(TraitKey.get(software.amazon.smithy.model.traits.XmlNameTrait.class));
        if (xml != null) {
            return xml.getValue();
        }
        return member.memberName();
    }

    private static LocationTrait locationTrait(Schema member) {
        HttpLabelTrait label = member.getTrait(TraitKey.get(HttpLabelTrait.class));
        if (label != null) {
            return LocationTrait.builder().location(MarshallLocation.PATH)
                    .locationName(member.memberName()).build();
        }
        HttpHeaderTrait header = member.getTrait(TraitKey.get(HttpHeaderTrait.class));
        if (header != null) {
            return LocationTrait.builder().location(MarshallLocation.HEADER)
                    .locationName(header.getValue()).build();
        }
        HttpQueryTrait query = member.getTrait(TraitKey.get(HttpQueryTrait.class));
        if (query != null) {
            return LocationTrait.builder().location(MarshallLocation.QUERY_PARAM)
                    .locationName(query.getValue()).build();
        }
        HttpPrefixHeadersTrait prefix = member.getTrait(TraitKey.get(HttpPrefixHeadersTrait.class));
        if (prefix != null) {
            return LocationTrait.builder().location(MarshallLocation.HEADER)
                    .locationName(prefix.getValue()).build();
        }
        return null;
    }

    private static MarshallingType<?> marshallingType(Schema member) {
        switch (member.type()) {
            case STRING:
            case ENUM:
                return MarshallingType.STRING;
            case BOOLEAN:
                return MarshallingType.BOOLEAN;
            case BYTE:
                return MarshallingType.BYTE;
            case SHORT:
                return MarshallingType.SHORT;
            case INTEGER:
            case INT_ENUM:
                return MarshallingType.INTEGER;
            case LONG:
                return MarshallingType.LONG;
            case FLOAT:
                return MarshallingType.FLOAT;
            case DOUBLE:
                return MarshallingType.DOUBLE;
            case BIG_DECIMAL:
                return MarshallingType.BIG_DECIMAL;
            case TIMESTAMP:
                return MarshallingType.INSTANT;
            case BLOB:
                return MarshallingType.SDK_BYTES;
            case LIST:
            case SET:
                return MarshallingType.LIST;
            case MAP:
                return MarshallingType.MAP;
            case STRUCTURE:
            case UNION:
                return MarshallingType.SDK_POJO;
            default:
                return MarshallingType.STRING;
        }
    }
}
