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

package software.amazon.awssdk.protocol.smithytests;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * RFC 3986 percent-decoding to UTF-8. Unlike {@link java.net.URLDecoder}, {@code +} is not treated as a space.
 */
final class PercentDecoding {
    private PercentDecoding() {
    }

    static String decode(String value) {
        if (value.indexOf('%') < 0) {
            return value;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(value.length());
        int i = 0;
        while (i < value.length()) {
            char c = value.charAt(i);
            if (c == '%') {
                if (i + 2 >= value.length()) {
                    throw new IllegalArgumentException("Truncated percent-encoding at index " + i + " in: " + value);
                }
                int hi = Character.digit(value.charAt(i + 1), 16);
                int lo = Character.digit(value.charAt(i + 2), 16);
                if (hi < 0 || lo < 0) {
                    throw new IllegalArgumentException("Invalid percent-encoding at index " + i + " in: " + value);
                }
                out.write((hi << 4) + lo);
                i += 3;
            } else {
                byte[] bytes = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
                if (Character.isHighSurrogate(c) && i + 1 < value.length()) {
                    bytes = value.substring(i, i + 2).getBytes(StandardCharsets.UTF_8);
                    i++;
                }
                out.write(bytes, 0, bytes.length);
                i++;
            }
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
