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

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Builds strings from byte ranges that the caller has already checked are ASCII.
 *
 * <p>Port of the {@code asciiString} half of smithy-java
 * {@code software.amazon.smithy.java.codecs.commons.CompactStringAccess}. smithy-java falls back to the ISO-8859-1
 * charset constructor when compact strings are disabled, which it detects through {@code String.coder}; that field is
 * not reachable with Java 8 API, so this class always uses the hibyte constructor.
 */
@SdkInternalApi
public final class AsciiStrings {

    private AsciiStrings() {
    }

    /**
     * Returns the string of the {@code len} bytes starting at {@code off}. Every byte in the range must be below
     * {@code 0x80}; the result for other bytes is the ISO-8859-1 decoding, not UTF-8.
     *
     * <p>This uses the deprecated hibyte constructor {@code String(byte[], int, int, int)}, which skips charset
     * dispatch. On JDK 9+ with compact strings (the default) it is a Latin-1 array copy; on JDK 8, and under
     * {@code -XX:-CompactStrings}, it fills the {@code char} array in a plain loop.
     *
     * @throws IndexOutOfBoundsException if {@code off} or {@code len} is negative or the range is outside
     *     {@code bytes}
     */
    @SuppressWarnings("deprecation")
    public static String fromAscii(byte[] bytes, int off, int len) {
        return new String(bytes, 0, off, len);
    }
}
