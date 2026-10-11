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

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import software.amazon.awssdk.utils.Validate;

/**
 * Selects the {@link BodyComparator} for a Smithy {@code bodyMediaType}:
 *
 * <ul>
 *     <li>JSON ({@code application/json}, {@code application/x-amz-json-*}, any {@code +json}): parsed with json-utils and
 *     compared structurally. Member order is ignored and numbers compare by value.</li>
 *     <li>XML ({@code application/xml}, {@code text/xml}, any {@code +xml}): XMLUnit {@code similar()}, ignoring
 *     whitespace between elements.</li>
 *     <li>CBOR ({@code application/cbor}): the expected body is base64; both sides are decoded with the shaded Jackson CBOR
 *     parser to data-model trees. Map key order is ignored and floats compare by value whatever their encoded width.</li>
 *     <li>{@code application/x-www-form-urlencoded}: the multiset of percent-decoded {@code name=value} pairs.</li>
 *     <li>Anything else, or no media type: the UTF-8 bytes of the expected string, byte for byte.</li>
 * </ul>
 */
public final class BodyComparators {
    private static final BodyComparator JSON = new JsonBodyComparator();
    private static final BodyComparator XML = new XmlBodyComparator();
    private static final BodyComparator CBOR = new CborBodyComparator();
    private static final BodyComparator FORM = new FormUrlEncodedBodyComparator();
    private static final BodyComparator BYTES = new BytesBodyComparator();

    private BodyComparators() {
    }

    /**
     * Assert that {@code actual} matches the test case body {@code expected} for {@code mediaType}. An empty expected
     * body matches only an empty actual body, whatever the media type.
     *
     * @param expected  the test case {@code body}; must not be null (omit the assertion when the case has no body)
     * @param mediaType the test case {@code bodyMediaType}, or null
     * @param actual    the actual body bytes; null is treated as empty
     */
    public static void assertBodyEquals(String expected, String mediaType, byte[] actual) {
        Validate.paramNotNull(expected, "expected");
        byte[] actualBytes = actual == null ? new byte[0] : actual;
        if (expected.isEmpty() || actualBytes.length == 0) {
            if (expected.isEmpty() != (actualBytes.length == 0)) {
                throw new AssertionError("Expected " + (expected.isEmpty() ? "an empty body" : "body:\n" + expected)
                                         + "\nbut the actual body was "
                                         + (actualBytes.length == 0 ? "empty"
                                                                    : ":\n" + new String(actualBytes,
                                                                                         StandardCharsets.UTF_8)));
            }
            return;
        }
        forMediaType(mediaType).assertMatches(expected, actualBytes);
    }

    /**
     * The comparator for {@code mediaType}. Media type parameters (for example {@code ; charset=utf-8}) are ignored, as is
     * case. A null media type selects the byte comparator.
     */
    public static BodyComparator forMediaType(String mediaType) {
        String type = baseType(mediaType);
        if (type == null) {
            return BYTES;
        }
        if (isCborType(type)) {
            return CBOR;
        }
        if (type.equals("application/json") || type.startsWith("application/x-amz-json") || type.endsWith("+json")) {
            return JSON;
        }
        if (type.equals("application/xml") || type.equals("text/xml") || type.endsWith("+xml")) {
            return XML;
        }
        if (type.equals("application/x-www-form-urlencoded")) {
            return FORM;
        }
        return BYTES;
    }

    static boolean isCbor(String mediaType) {
        String type = baseType(mediaType);
        return type != null && isCborType(type);
    }

    private static boolean isCborType(String type) {
        return type.equals("application/cbor") || type.endsWith("+cbor");
    }

    private static String baseType(String mediaType) {
        if (mediaType == null) {
            return null;
        }
        int semicolon = mediaType.indexOf(';');
        String type = (semicolon < 0 ? mediaType : mediaType.substring(0, semicolon)).trim().toLowerCase(Locale.ROOT);
        return type.isEmpty() ? null : type;
    }
}
