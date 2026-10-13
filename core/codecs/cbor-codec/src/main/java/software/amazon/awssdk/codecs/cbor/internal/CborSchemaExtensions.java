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

package software.amazon.awssdk.codecs.cbor.internal;

import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaExtensionKey;
import software.amazon.awssdk.schema.SchemaExtensionProvider;
import software.amazon.awssdk.schema.ShapeType;

// CHECKSTYLE:OFF - Keeps the smithy-java (and design Porting_Map) name.
/**
 * Computes, once per schema, the CBOR data the codec needs: the encoded text-string key (header and UTF-8 name) of
 * each member, and for each structure or union a member lookup table and a table of encoded keys indexed by
 * {@link Schema#memberIndex()}.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.cbor.CborSchemaExtensions}. Registered as a
 * {@link SchemaExtensionProvider} through {@code META-INF/services}.
 */
@SdkInternalApi
public final class CborSchemaExtensions implements SchemaExtensionProvider<CborSchemaExtensions.CborExtension> {
    // CHECKSTYLE:ON

    /**
     * Extension key for CBOR codec data.
     */
    public static final SchemaExtensionKey<CborExtension> KEY = new SchemaExtensionKey<>();

    @Override
    public SchemaExtensionKey<CborExtension> key() {
        return KEY;
    }

    @Override
    public CborExtension provide(Schema schema) {
        if (schema.isMember()) {
            return forMember(schema);
        }
        ShapeType type = schema.type();
        if (type == ShapeType.STRUCTURE || type == ShapeType.UNION) {
            return forStruct(schema);
        }
        return null;
    }

    private static CborExtension forMember(Schema schema) {
        return new CborExtension(CborSerializer.encodeMemberName(schema.memberName()), null, null);
    }

    private static CborExtension forStruct(Schema schema) {
        List<Schema> members = schema.members();
        if (members.isEmpty()) {
            return new CborExtension(null, null, null);
        }
        CborMemberLookup memberLookup = new CborMemberLookup(members);
        int maxIndex = 0;
        for (Schema m : members) {
            maxIndex = Math.max(maxIndex, m.memberIndex());
        }
        byte[][] fieldNameTable = new byte[maxIndex + 1][];
        for (Schema m : members) {
            fieldNameTable[m.memberIndex()] = CborSerializer.encodeMemberName(m.memberName());
        }
        return new CborExtension(null, memberLookup, fieldNameTable);
    }

    /**
     * Pre-computed CBOR data stored on a schema. Immutable; the arrays are never modified after construction and are
     * only read by this package.
     */
    public static final class CborExtension {
        private final byte[] memberNameBytes;
        private final CborMemberLookup memberLookup;
        private final byte[][] fieldNameTable;

        CborExtension(byte[] memberNameBytes, CborMemberLookup memberLookup, byte[][] fieldNameTable) {
            this.memberNameBytes = memberNameBytes;
            this.memberLookup = memberLookup;
            this.fieldNameTable = fieldNameTable;
        }

        /**
         * The CBOR text-string header and name bytes of a member schema; null for other schemas.
         */
        byte[] memberNameBytes() {
            return memberNameBytes;
        }

        /**
         * The member lookup of a structure or union with members; null otherwise.
         */
        CborMemberLookup memberLookup() {
            return memberLookup;
        }

        /**
         * The encoded member keys of a structure or union with members, indexed by member index; null otherwise.
         */
        byte[][] fieldNameTable() {
            return fieldNameTable;
        }
    }
}
