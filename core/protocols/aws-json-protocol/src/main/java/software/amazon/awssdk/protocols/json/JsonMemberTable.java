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

package software.amazon.awssdk.protocols.json;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Immutable table of a shape's payload member names, created once per generated shape as a
 * {@code static final} constant. Maps incoming JSON field names to member ordinals for
 * {@link StructuredJsonReader#readStruct}.
 *
 * <p>Precomputes both a string-keyed index (for token-stream readers whose field names arrive as
 * canonicalized Strings) and the members' UTF-8 name bytes with packed-long identities for short
 * names (for byte-level readers that match names without allocating a String).
 */
@SdkProtectedApi
public final class JsonMemberTable {

    /**
     * Names of at most this many bytes pack into a single long that is an exact identity
     * (length is encoded by the terminating quote byte), enabling one-comparison matching.
     */
    public static final int PACK_MAX_LEN = 7;

    private final String[] names;
    private final byte[][] nameBytes;
    private final long[] packedNames;
    private final Map<String, Integer> indexByName;

    private JsonMemberTable(String[] names) {
        this.names = names;
        this.nameBytes = new byte[names.length][];
        this.packedNames = new long[names.length];
        this.indexByName = new HashMap<>(names.length * 2);
        for (int i = 0; i < names.length; i++) {
            byte[] bytes = names[i].getBytes(StandardCharsets.UTF_8);
            this.nameBytes[i] = bytes;
            this.packedNames[i] = bytes.length >= 1 && bytes.length <= PACK_MAX_LEN
                                  ? packName(bytes, 0, bytes.length)
                                  : 0L;
            this.indexByName.put(names[i], i);
        }
    }

    /**
     * @param memberNames the shape's payload member location names, in SDK_FIELDS order.
     */
    public static JsonMemberTable of(String... memberNames) {
        return new JsonMemberTable(memberNames.clone());
    }

    /**
     * @return the member ordinal for the given field name, or -1 if unknown.
     */
    public int indexOf(String fieldName) {
        Integer index = indexByName.get(fieldName);
        return index == null ? -1 : index;
    }

    /**
     * @return the number of members in the table.
     */
    public int size() {
        return names.length;
    }

    /**
     * @return the member ordinal whose name matches the given UTF-8 bytes, or -1 if unknown.
     * No String is allocated on the short-name path.
     */
    public int indexOf(byte[] buf, int start, int end) {
        int nameLen = end - start;
        if (nameLen >= 1 && nameLen <= PACK_MAX_LEN) {
            long key = packName(buf, start, nameLen);
            long[] packed = packedNames;
            for (int i = 0; i < packed.length; i++) {
                if (packed[i] == key) {
                    return i;
                }
            }
            return -1;
        }
        byte[][] candidates = nameBytes;
        for (int i = 0; i < candidates.length; i++) {
            byte[] candidate = candidates[i];
            if (candidate.length == nameLen && regionMatches(buf, start, candidate)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @return the UTF-8 bytes of the member name at the given ordinal.
     */
    public byte[] nameBytes(int index) {
        return nameBytes[index];
    }

    /**
     * @return the packed-long identity of the member name at the given ordinal, or 0 if the name
     * is too long to pack.
     */
    public long packedName(int index) {
        return packedNames[index];
    }

    /**
     * Packs a short name plus a terminating quote byte into a long. Distinct (bytes, length) pairs
     * never collide because the quote byte encodes the length.
     */
    public static long packName(byte[] buf, int start, int len) {
        long key = (long) '"' << (len << 3);
        for (int i = 0; i < len; i++) {
            key |= (buf[start + i] & 0xFFL) << (i << 3);
        }
        return key;
    }

    private static boolean regionMatches(byte[] buf, int start, byte[] candidate) {
        for (int i = 0; i < candidate.length; i++) {
            if (buf[start + i] != candidate[i]) {
                return false;
            }
        }
        return true;
    }
}
