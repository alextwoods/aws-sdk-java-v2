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

import java.util.Objects;

/**
 * Validation of raw (already encoded) request-target components and header field names, so a value that would
 * change the meaning of the request line or header block (a line break, a space, {@code ?}/{@code #} in a path, a
 * {@code &} in a query entry, a {@code :} in a field name) is rejected when the message is built rather than
 * transmitted.
 */
final class RawTargets {
    private RawTargets() {
    }

    /** A raw path: printable ASCII other than space, {@code ?}, {@code #}, with well-formed percent escapes. */
    static void requirePath(String path) {
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '?' || c == '#' || !isVisibleAscii(c)) {
                throw new IllegalArgumentException("The encoded path has an unencoded character at index " + i);
            }
            if (c == '%') {
                requireEscape(path, i, "encoded path");
            }
        }
    }

    /** A raw query name or value: printable ASCII other than space, {@code &}, {@code #} (and {@code =} in names). */
    static void requireQueryComponent(String text, boolean name) {
        Objects.requireNonNull(text, name ? "rawName" : "rawValue");
        if (name && text.isEmpty()) {
            throw new IllegalArgumentException("A query parameter name is empty");
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&' || c == '#' || name && c == '=' || !isVisibleAscii(c)) {
                throw new IllegalArgumentException("The encoded query " + (name ? "name" : "value")
                                                   + " has an unencoded character at index " + i);
            }
            if (c == '%') {
                requireEscape(text, i, "encoded query component");
            }
        }
    }

    /**
     * A header field name: non-empty visible ASCII without {@code :}. HTTP/2 pseudo-header fields are derived from
     * the request's method and target (see {@link SdkHttpRequest#pseudoHeaders()}), never added as headers.
     */
    static void requireFieldName(String name) {
        if (name.isEmpty()) {
            throw new IllegalArgumentException("A header field name is empty");
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == ':' || !isVisibleAscii(c)) {
                throw new IllegalArgumentException("The header field name has an invalid character at index " + i);
            }
        }
    }

    /** A request method: an RFC 9110 token (visible ASCII other than delimiters), so it cannot split the request line. */
    static void requireMethod(String method) {
        if (method.isEmpty()) {
            throw new IllegalArgumentException("The HTTP method is empty");
        }
        for (int i = 0; i < method.length(); i++) {
            char c = method.charAt(i);
            if (!isVisibleAscii(c) || "\"(),/:;<=>?@[\\]{}".indexOf(c) >= 0) {
                throw new IllegalArgumentException("The HTTP method has an invalid character at index " + i);
            }
        }
    }

    /**
     * A host as it appears in a URI authority: a registered name or IPv4 address (visible ASCII without {@code :} or
     * the authority/target delimiters), or an IP literal in brackets such as {@code [::1]}. The host is transmitted in
     * {@code Host}/{@code :authority}, so anything that would change that field is rejected.
     */
    static void requireHost(String host) {
        if (host.isEmpty()) {
            throw new IllegalArgumentException("The host is empty");
        }
        boolean literal = host.charAt(0) == '[';
        if (literal && (host.length() < 3 || host.charAt(host.length() - 1) != ']')) {
            throw new IllegalArgumentException("The host IP literal is not enclosed in brackets");
        }
        int end = literal ? host.length() - 1 : host.length();
        for (int i = literal ? 1 : 0; i < end; i++) {
            char c = host.charAt(i);
            if (!isVisibleAscii(c) || "/?#@[]\\".indexOf(c) >= 0 || c == ':' && !literal) {
                throw new IllegalArgumentException("The host has an invalid character at index " + i
                                                   + (c == ':' ? " (an IPv6 address is written in brackets)" : ""));
            }
        }
    }

    /** A header field value: no CR, LF or NUL. */
    static void requireFieldValue(String name, String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\r' || c == '\n' || c == '\0') {
                throw new IllegalArgumentException("The value of header " + name + " contains a line break or NUL");
            }
        }
    }

    private static boolean isVisibleAscii(char c) {
        return c > 0x20 && c < 0x7F;
    }

    private static void requireEscape(String text, int index, String what) {
        if (index + 2 >= text.length() || !isHex(text.charAt(index + 1)) || !isHex(text.charAt(index + 2))) {
            throw new IllegalArgumentException("The " + what + " has a malformed percent escape at index " + index);
        }
    }

    private static boolean isHex(char c) {
        return c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f';
    }
}
