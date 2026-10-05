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

package software.amazon.awssdk.http;

import java.nio.charset.StandardCharsets;

/**
 * RFC 3986 percent-encoding as AWS protocols and SigV4 apply it: every byte of the UTF-8 encoding except the
 * unreserved characters {@code A-Z a-z 0-9 - . _ ~} is encoded as {@code %XX} with upper-case hex digits (so a space
 * is {@code %20}, never {@code +}).
 */
public final class UriEncoding {
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private UriEncoding() {
    }

    /** Encodes {@code text} for a query parameter name or value, or a non-greedy path label. */
    public static String encode(String text) {
        return encode(text, false);
    }

    /**
     * Encodes {@code text}, leaving {@code /} unencoded if {@code keepSlash} (a greedy path label).
     */
    public static String encode(String text, boolean keepSlash) {
        int i = 0;
        int length = text.length();
        while (i < length && isUnreserved(text.charAt(i), keepSlash)) {
            i++;
        }
        if (i == length) {
            return text;
        }
        StringBuilder out = new StringBuilder(length + 16);
        out.append(text, 0, i);
        byte[] bytes = text.substring(i).getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            char c = (char) (b & 0xFF);
            if (c < 0x80 && isUnreserved(c, keepSlash)) {
                out.append(c);
            } else {
                out.append('%').append(HEX[(b >> 4) & 0xF]).append(HEX[b & 0xF]);
            }
        }
        return out.toString();
    }

    private static boolean isUnreserved(char c, boolean keepSlash) {
        return c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '-' || c == '.'
               || c == '_' || c == '~' || keepSlash && c == '/';
    }
}
