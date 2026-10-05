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

package software.amazon.awssdk.eventstream;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/** Strict UTF-8 helpers of the framing code. */
final class Utf8 {
    private Utf8() {
    }

    /** The UTF-8 length of {@code value}. */
    static int length(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    /** Decodes {@code length} bytes at {@code offset}, or returns {@code null} if they are not valid UTF-8. */
    static String decode(byte[] bytes, int offset, int length) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                                         .onMalformedInput(CodingErrorAction.REPORT)
                                         .onUnmappableCharacter(CodingErrorAction.REPORT)
                                         .decode(ByteBuffer.wrap(bytes, offset, length))
                                         .toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }
}
