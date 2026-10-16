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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codecs.commons.internal.ByteArrays;
import software.amazon.awssdk.schema.MemberLookup;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.traits.JsonNameTrait;

/**
 * Field name lookup that matches the UTF-8 bytes of a JSON field name directly in the input buffer.
 *
 * <p>Names of 1 to 7 bytes are packed, with their closing quote, into a {@code long} that identifies them exactly;
 * longer names are pre-hashed with FNV-1a. Also implements {@link MemberLookup} for the field mapper API.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.json.smithy.SmithyMemberLookup};
 * {@code Arrays.equals(byte[], int, int, byte[], int, int)} becomes {@link ByteArrays#rangeEquals}.
 */
@SdkInternalApi
final class SmithyMemberLookup implements MemberLookup {

    // The closing quote makes packed names length-sensitive and directly probeable.
    static final int PACK_MAX_LEN = 7;

    // FNV-1a constants
    private static final long FNV_OFFSET = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;

    // Members in definition order (for speculative matching)
    final long[] orderedHashes;
    final Schema[] orderedSchemas;
    final byte[][] orderedNameBytes;
    final long[] orderedPackedNames;

    SmithyMemberLookup(List<Schema> members, boolean useJsonName) {
        int size = members.size();
        this.orderedHashes = new long[size];
        this.orderedSchemas = new Schema[size];
        this.orderedNameBytes = new byte[size][];
        this.orderedPackedNames = new long[size];
        for (int i = 0; i < size; i++) {
            Schema m = members.get(i);
            String fieldName;
            if (useJsonName) {
                JsonNameTrait jsonNameTrait = m.getTrait(TraitKey.JSON_NAME_TRAIT);
                fieldName = jsonNameTrait != null ? jsonNameTrait.getValue() : m.memberName();
            } else {
                fieldName = m.memberName();
            }
            byte[] nameBytes = fieldName.getBytes(StandardCharsets.UTF_8);
            orderedNameBytes[i] = nameBytes;
            orderedHashes[i] = fnvHash(nameBytes, 0, nameBytes.length);
            int len = nameBytes.length;
            orderedPackedNames[i] = (len >= 1 && len <= PACK_MAX_LEN) ? packName(nameBytes, 0, len) : 0L;
            orderedSchemas[i] = m;
        }
    }

    /**
     * Looks up a member by matching the field name bytes directly from the input buffer, without allocating a String.
     *
     * <ol>
     *   <li>Names of 1 to 7 bytes: one scan of packed {@code long}s, checking the speculative position first. This is
     *   the union discriminator's hot path, where the speculative guess misses for every member after the first.</li>
     *   <li>Longer names: linear scan with an FNV-1a hash pre-filter, computed only on this path, then a byte
     *   comparison.</li>
     * </ol>
     *
     * @param buf input buffer containing the field name bytes
     * @param start start offset (after the opening quote)
     * @param end end offset (before the closing quote)
     * @param expectedNext speculative next member index (-1 to disable)
     * @return the matched schema, or null if not found
     */
    Schema lookup(byte[] buf, int start, int end, int expectedNext) {
        int nameLen = end - start;
        if (nameLen >= 1 && nameLen <= PACK_MAX_LEN) {
            long key = packName(buf, start, nameLen);
            long[] packed = orderedPackedNames;
            // Check the speculative position first to preserve in-order locality.
            if (expectedNext >= 0 && expectedNext < packed.length && packed[expectedNext] == key) {
                return orderedSchemas[expectedNext];
            }
            for (int i = 0; i < packed.length; i++) {
                if (packed[i] == key) {
                    return orderedSchemas[i];
                }
            }
            return null;
        }

        long hash = fnvHash(buf, start, end);
        for (int i = 0; i < orderedHashes.length; i++) {
            if (orderedHashes[i] == hash
                && orderedNameBytes[i].length == nameLen
                && ByteArrays.rangeEquals(buf, start, end, orderedNameBytes[i], 0, nameLen)) {
                return orderedSchemas[i];
            }
        }
        return null;
    }

    /**
     * {@link MemberLookup} implementation; encodes the name to UTF-8 first.
     */
    @Override
    public Schema member(String memberName) {
        byte[] nameBytes = memberName.getBytes(StandardCharsets.UTF_8);
        long hash = fnvHash(nameBytes, 0, nameBytes.length);
        for (int i = 0; i < orderedHashes.length; i++) {
            if (orderedHashes[i] == hash
                && nameBytes.length == orderedNameBytes[i].length
                && Arrays.equals(nameBytes, orderedNameBytes[i])) {
                return orderedSchemas[i];
            }
        }
        return null;
    }

    private static long packName(byte[] buf, int start, int len) {
        long key = (long) '"' << (len << 3);
        for (int i = 0; i < len; i++) {
            key |= (buf[start + i] & 0xFFL) << (i << 3);
        }
        return key;
    }

    private static long fnvHash(byte[] buf, int start, int end) {
        long hash = FNV_OFFSET;
        for (int i = start; i < end; i++) {
            hash ^= buf[i] & 0xFF;
            hash *= FNV_PRIME;
        }
        return hash;
    }
}
