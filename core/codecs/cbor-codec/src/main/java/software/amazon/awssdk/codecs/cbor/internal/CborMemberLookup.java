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

import java.nio.charset.StandardCharsets;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.Schema;

/**
 * Finds a structure or union member by the raw UTF-8 bytes of its name in the CBOR payload, without allocating a
 * {@code String} on the common path. Members are tried first at a speculative index (the member after the last one
 * read, since serializers usually write members in schema order), then by a 64-bit FNV-1a hash.
 *
 * <p>Port of smithy-java {@code CborMemberLookup}.
 */
@SdkInternalApi
final class CborMemberLookup {
    private static final long FNV_OFFSET = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;

    final long[] orderedHashes;
    final Schema[] orderedSchemas;
    final byte[][] orderedNameBytes;

    CborMemberLookup(List<Schema> members) {
        int size = members.size();
        this.orderedHashes = new long[size];
        this.orderedSchemas = new Schema[size];
        this.orderedNameBytes = new byte[size][];
        for (int i = 0; i < size; i++) {
            Schema m = members.get(i);
            byte[] nameBytes = m.memberName().getBytes(StandardCharsets.UTF_8);
            orderedNameBytes[i] = nameBytes;
            orderedHashes[i] = fnvHash(nameBytes, 0, nameBytes.length);
            orderedSchemas[i] = m;
        }
    }

    /**
     * Looks up a member by matching the field name bytes directly from the CBOR payload buffer.
     *
     * @param buf input buffer containing the field name bytes (raw UTF-8, no CBOR header)
     * @param start start offset in buf
     * @param end end offset in buf (exclusive)
     * @param expectedNext speculative next member position (-1 to disable)
     * @return the matched member schema, or null if not found
     */
    Schema lookup(byte[] buf, int start, int end, int expectedNext) {
        int nameLen = end - start;
        if (expectedNext >= 0 && expectedNext < orderedNameBytes.length
            && orderedNameBytes[expectedNext].length == nameLen
            && CborReadUtil.regionEquals(buf, start, end, orderedNameBytes[expectedNext], 0)) {
            return orderedSchemas[expectedNext];
        }
        long hash = fnvHash(buf, start, end);
        for (int i = 0; i < orderedHashes.length; i++) {
            if (orderedHashes[i] == hash
                && orderedNameBytes[i].length == nameLen
                && CborReadUtil.regionEquals(buf, start, end, orderedNameBytes[i], 0)) {
                return orderedSchemas[i];
            }
        }
        return null;
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
