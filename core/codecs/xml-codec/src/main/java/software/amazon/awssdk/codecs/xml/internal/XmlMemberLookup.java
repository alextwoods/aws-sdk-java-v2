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

package software.amazon.awssdk.codecs.xml.internal;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codecs.commons.internal.ByteArrays;
import software.amazon.awssdk.schema.Schema;

/**
 * Element name lookup that matches the UTF-8 bytes of an element name directly in the input buffer. A hint array
 * remembers the position after the last match, so members read in schema order match on the first comparison.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.xml.XmlMemberLookup};
 * {@code Arrays.equals(byte[], int, int, byte[], int, int)} becomes {@link ByteArrays#rangeEquals}.
 */
@SdkInternalApi
final class XmlMemberLookup {

    private final byte[][] memberNames;
    private final int[] memberLens;
    private final Schema[] memberSchemas;

    XmlMemberLookup(Map<String, Schema> elements) {
        int size = elements.size();
        memberNames = new byte[size][];
        memberLens = new int[size];
        memberSchemas = new Schema[size];
        int i = 0;
        for (Map.Entry<String, Schema> entry : elements.entrySet()) {
            byte[] nameBytes = entry.getKey().getBytes(StandardCharsets.UTF_8);
            memberNames[i] = nameBytes;
            memberLens[i] = nameBytes.length;
            memberSchemas[i] = entry.getValue();
            i++;
        }
    }

    Schema findMember(byte[] buf, int nameStart, int nameLen, int[] hint) {
        int len = memberNames.length;
        if (len == 0) {
            return null;
        }

        int idx = hint[0];
        if (idx < len && memberLens[idx] == nameLen
            && ByteArrays.rangeEquals(buf, nameStart, nameStart + nameLen, memberNames[idx], 0, nameLen)) {
            hint[0] = idx + 1;
            return memberSchemas[idx];
        }

        for (int i = 0; i < len; i++) {
            if (i != idx && memberLens[i] == nameLen
                && ByteArrays.rangeEquals(buf, nameStart, nameStart + nameLen, memberNames[i], 0, nameLen)) {
                hint[0] = i + 1;
                return memberSchemas[i];
            }
        }

        return null;
    }
}
