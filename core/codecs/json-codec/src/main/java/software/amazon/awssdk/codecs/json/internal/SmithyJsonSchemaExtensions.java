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

import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaExtensionKey;
import software.amazon.awssdk.schema.SchemaExtensionProvider;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.traits.JsonNameTrait;

// CHECKSTYLE:OFF - Keeps the smithy-java (and design Porting_Map) name.
/**
 * Computes, once per schema, the data the hand-written JSON serializer and deserializer need: for each member the
 * pre-encoded {@code "name":} bytes with and without {@code jsonName}, and for each structure or union the
 * {@link SmithyMemberLookup} tables (packed-long and pre-hashed names) and the encoded field names indexed by
 * {@link Schema#memberIndex()}.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.json.smithy.SmithyJsonSchemaExtensions}. Registered as a
 * {@link SchemaExtensionProvider} through {@code META-INF/services}.
 */
@SdkInternalApi
public final class SmithyJsonSchemaExtensions
    implements SchemaExtensionProvider<SmithyJsonSchemaExtensions.NativeJsonExtension> {
    // CHECKSTYLE:ON

    /**
     * Extension key for the native JSON codec data.
     */
    public static final SchemaExtensionKey<NativeJsonExtension> KEY = new SchemaExtensionKey<>();

    @Override
    public SchemaExtensionKey<NativeJsonExtension> key() {
        return KEY;
    }

    @Override
    public NativeJsonExtension provide(Schema schema) {
        if (schema.isMember()) {
            return forMember(schema);
        }
        ShapeType type = schema.type();
        if (type == ShapeType.STRUCTURE || type == ShapeType.UNION) {
            return forStruct(schema);
        }
        return null;
    }

    private static NativeJsonExtension forMember(Schema schema) {
        byte[] jsonNameBytes = JsonWriteUtils.encodeFieldNameToken(jsonName(schema));
        byte[] memberNameBytes = JsonWriteUtils.encodeFieldNameToken(schema.memberName());
        return new NativeJsonExtension(jsonNameBytes, memberNameBytes, null, null, null, null);
    }

    private static NativeJsonExtension forStruct(Schema schema) {
        List<Schema> members = schema.members();
        if (members.isEmpty()) {
            return new NativeJsonExtension(null, null, null, null, null, null);
        }

        SmithyMemberLookup jsonNameLookup = new SmithyMemberLookup(members, true);
        SmithyMemberLookup memberNameLookup = new SmithyMemberLookup(members, false);

        // Field name tables indexed by memberIndex, so member writes need no per-member extension read.
        int maxIndex = 0;
        for (Schema m : members) {
            maxIndex = Math.max(maxIndex, m.memberIndex());
        }
        byte[][] jsonFieldNameTable = new byte[maxIndex + 1][];
        byte[][] memberFieldNameTable = new byte[maxIndex + 1][];
        for (Schema m : members) {
            int idx = m.memberIndex();
            jsonFieldNameTable[idx] = JsonWriteUtils.encodeFieldNameToken(jsonName(m));
            memberFieldNameTable[idx] = JsonWriteUtils.encodeFieldNameToken(m.memberName());
        }

        return new NativeJsonExtension(null, null, jsonNameLookup, memberNameLookup, jsonFieldNameTable,
                                       memberFieldNameTable);
    }

    private static String jsonName(Schema member) {
        JsonNameTrait jsonNameTrait = member.getTrait(TraitKey.JSON_NAME_TRAIT);
        return jsonNameTrait != null ? jsonNameTrait.getValue() : member.memberName();
    }

    /**
     * Pre-computed native JSON data stored on a schema. Immutable; the arrays are never modified after construction
     * and are only read by this package.
     */
    public static final class NativeJsonExtension {
        private final byte[] jsonNameBytes;
        private final byte[] memberNameBytes;
        private final SmithyMemberLookup jsonNameLookup;
        private final SmithyMemberLookup memberNameLookup;
        private final byte[][] jsonFieldNameTable;
        private final byte[][] memberFieldNameTable;

        NativeJsonExtension(byte[] jsonNameBytes, byte[] memberNameBytes, SmithyMemberLookup jsonNameLookup,
                            SmithyMemberLookup memberNameLookup, byte[][] jsonFieldNameTable,
                            byte[][] memberFieldNameTable) {
            this.jsonNameBytes = jsonNameBytes;
            this.memberNameBytes = memberNameBytes;
            this.jsonNameLookup = jsonNameLookup;
            this.memberNameLookup = memberNameLookup;
            this.jsonFieldNameTable = jsonFieldNameTable;
            this.memberFieldNameTable = memberFieldNameTable;
        }

        /**
         * The field name as {@code "jsonName":} bytes; null for non-members.
         */
        byte[] jsonNameBytes() {
            return jsonNameBytes;
        }

        /**
         * The field name as {@code "memberName":} bytes; null for non-members.
         */
        byte[] memberNameBytes() {
            return memberNameBytes;
        }

        /**
         * The lookup by {@code jsonName}; null for non-structures and structures without members.
         */
        SmithyMemberLookup jsonNameLookup() {
            return jsonNameLookup;
        }

        /**
         * The lookup by member name; null for non-structures and structures without members.
         */
        SmithyMemberLookup memberNameLookup() {
            return memberNameLookup;
        }

        /**
         * The {@code "jsonName":} bytes indexed by member index; null for non-structures.
         */
        byte[][] jsonFieldNameTable() {
            return jsonFieldNameTable;
        }

        /**
         * The {@code "memberName":} bytes indexed by member index; null for non-structures.
         */
        byte[][] memberFieldNameTable() {
            return memberFieldNameTable;
        }
    }
}
