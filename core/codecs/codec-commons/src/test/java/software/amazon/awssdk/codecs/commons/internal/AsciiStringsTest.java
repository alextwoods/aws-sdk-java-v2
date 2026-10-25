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

package software.amazon.awssdk.codecs.commons.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * SDK tests for {@link AsciiStrings}: the hibyte constructor must give the same string as the ISO-8859-1 and US-ASCII
 * charset constructors that the JSON and XML readers used before.
 */
class AsciiStringsTest {

    private static final int MAX_OFFSET = 3;
    private static final int MAX_LENGTH = 64;

    @Test
    void fromAscii_everyByteValueAtEveryOffsetAndLength_matchesCharsetConstructors() {
        byte[] bytes = new byte[MAX_OFFSET + MAX_LENGTH];
        for (int first = 0; first < 0x80; first++) {
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) ((first + i) & 0x7F);
            }
            for (int off = 0; off <= MAX_OFFSET; off++) {
                for (int len = 0; len <= MAX_LENGTH; len++) {
                    String expected = new String(bytes, off, len, StandardCharsets.ISO_8859_1);
                    String actual = AsciiStrings.fromAscii(bytes, off, len);
                    assertThat(actual).isEqualTo(expected)
                                      .isEqualTo(new String(bytes, off, len, StandardCharsets.US_ASCII));
                }
            }
        }
    }

    @Test
    void fromAscii_emptyRange_returnsEmptyString() {
        assertThat(AsciiStrings.fromAscii(new byte[0], 0, 0)).isEmpty();
        assertThat(AsciiStrings.fromAscii(new byte[] {'a', 'b'}, 2, 0)).isEmpty();
    }

    @Test
    void fromAscii_doesNotAliasTheInputArray() {
        byte[] bytes = {'a', 'b', 'c'};
        String s = AsciiStrings.fromAscii(bytes, 0, 3);
        bytes[0] = 'z';
        assertThat(s).isEqualTo("abc");
    }

    @Test
    void fromAscii_rangeOutsideArray_throwsIndexOutOfBounds() {
        byte[] bytes = new byte[4];
        assertThatThrownBy(() -> AsciiStrings.fromAscii(bytes, -1, 1)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> AsciiStrings.fromAscii(bytes, 0, -1)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> AsciiStrings.fromAscii(bytes, 2, 3)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> AsciiStrings.fromAscii(bytes, 5, 0)).isInstanceOf(IndexOutOfBoundsException.class);
    }
}
