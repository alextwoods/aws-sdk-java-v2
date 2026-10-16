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

package software.amazon.awssdk.codecs.json.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codecs.commons.TimestampFormatter;
import software.amazon.awssdk.schema.MemberLookup;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaExtensionKey;
import software.amazon.awssdk.schema.SchemaExtensionProvider;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.traits.JsonNameTrait;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;

// CHECKSTYLE:OFF - Keeps the smithy-java (and design Porting_Map) name.
/**
 * Computes, once per schema, the JSON data shared by every JSON serde provider: the {@code jsonName} member lookup of
 * each structure or union (used by {@code JsonFieldMapper.UseJsonNameTrait}) and the timestamp formatter of each
 * schema with a {@code timestampFormat} trait (used by {@code TimestampResolver.UseTimestampFormatTrait}).
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.json.JsonSchemaExtensions}, without the Jackson
 * {@code SerializableString} field names that only the Jackson provider used. Registered as a
 * {@link SchemaExtensionProvider} through {@code META-INF/services}.
 */
@SdkInternalApi
public final class JsonSchemaExtensions implements SchemaExtensionProvider<JsonSchemaExtensions.JsonSchemaExtension> {
    // CHECKSTYLE:ON

    /**
     * Extension key for JSON-specific schema data.
     */
    public static final SchemaExtensionKey<JsonSchemaExtension> KEY = new SchemaExtensionKey<>();

    private static final MemberLookup NO_MEMBERS = name -> null;

    @Override
    public SchemaExtensionKey<JsonSchemaExtension> key() {
        return KEY;
    }

    @Override
    public JsonSchemaExtension provide(Schema schema) {
        if (schema.isMember()) {
            return forMember(schema);
        }
        ShapeType type = schema.type();
        if (type == ShapeType.STRUCTURE || type == ShapeType.UNION) {
            return new JsonSchemaExtension(buildMemberLookup(schema.members()), null);
        }
        TimestampFormatter formatter = formatter(schema);
        return formatter == null ? null : new JsonSchemaExtension(null, formatter);
    }

    private static JsonSchemaExtension forMember(Schema schema) {
        return new JsonSchemaExtension(null, formatter(schema));
    }

    private static TimestampFormatter formatter(Schema schema) {
        TimestampFormatTrait trait = schema.getTrait(TraitKey.TIMESTAMP_FORMAT_TRAIT);
        return trait == null ? null : TimestampFormatter.match(trait.getFormat());
    }

    private static MemberLookup buildMemberLookup(List<Schema> members) {
        if (members.isEmpty()) {
            return NO_MEMBERS;
        }
        Map<String, Schema> jsonNameMap = new HashMap<>((int) (members.size() / 0.75f) + 1);
        for (Schema m : members) {
            JsonNameTrait jsonNameTrait = m.getTrait(TraitKey.JSON_NAME_TRAIT);
            jsonNameMap.put(jsonNameTrait != null ? jsonNameTrait.getValue() : m.memberName(), m);
        }
        return jsonNameMap::get;
    }

    /**
     * Pre-computed JSON data stored on a schema. Immutable.
     */
    public static final class JsonSchemaExtension {
        private final MemberLookup jsonMemberLookup;
        private final TimestampFormatter timestampFormatter;

        JsonSchemaExtension(MemberLookup jsonMemberLookup, TimestampFormatter timestampFormatter) {
            this.jsonMemberLookup = jsonMemberLookup;
            this.timestampFormatter = timestampFormatter;
        }

        /**
         * Returns the lookup of a structure's or union's members by {@code jsonName} (falling back to the member
         * name); null for other schemas.
         *
         * @return the member lookup, or null
         */
        public MemberLookup jsonMemberLookup() {
            return jsonMemberLookup;
        }

        /**
         * Returns the formatter of the schema's {@code timestampFormat} trait; null without the trait.
         *
         * @return the timestamp formatter, or null
         */
        public TimestampFormatter timestampFormatter() {
            return timestampFormatter;
        }
    }
}
