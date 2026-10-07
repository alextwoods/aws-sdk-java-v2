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

package software.amazon.awssdk.protocols.smithyjava.internal;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Turns smithy-java's XML request document into the bytes v2's S3 XML writer would have produced for the same
 * content.
 *
 * <p>Two differences are not cosmetic, and are why this exists:
 * <ul>
 *   <li>v2 escapes carriage return and line feed in text as {@code &#x0D;} and {@code &#x0A;}; smithy-java writes
 *       them raw. An XML parser normalizes a raw CR (and CRLF) to LF, so an object key containing CR would reach
 *       S3 as a different key, which is why S3's documentation tells clients to escape it.</li>
 *   <li>A raw CR or LF inside an attribute value is normalized to a space.</li>
 * </ul>
 * The rest make the bytes match: v2 also escapes {@code "} and {@code '} in text, and starts every non-empty
 * document with an XML declaration.
 *
 * <p>The input is a document smithy-java just wrote, so it is well formed, has no comments, CDATA or processing
 * instructions, and quotes attribute values with {@code "}. That makes a single pass enough: outside markup,
 * escape the four characters; inside markup, escape CR and LF, which can only come from attribute values, and
 * track {@code "} so a {@code >} inside an attribute value does not end the markup.
 */
@SdkInternalApi
public final class V2XmlBodyRules {

    private static final byte[] PROLOG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes(StandardCharsets.UTF_8);
    private static final byte[] CR = "&#x0D;".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] LF = "&#x0A;".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] QUOT = "&quot;".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] APOS = "&apos;".getBytes(StandardCharsets.US_ASCII);

    private V2XmlBodyRules() {
    }

    public static byte[] apply(byte[] document) {
        if (document.length == 0) {
            return document;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(document.length + PROLOG.length + 32);
        out.write(PROLOG, 0, PROLOG.length);
        boolean inMarkup = false;
        boolean inAttributeValue = false;
        int copyFrom = 0;
        for (int i = 0; i < document.length; i++) {
            byte b = document[i];
            byte[] replacement = null;
            if (inMarkup && b == '"') {
                inAttributeValue = !inAttributeValue;
            } else if (!inMarkup && b == '<') {
                inMarkup = true;
            } else if (inMarkup && !inAttributeValue && b == '>') {
                inMarkup = false;
            } else if (b == '\r') {
                replacement = CR;
            } else if (b == '\n') {
                replacement = LF;
            } else if (!inMarkup && b == '"') {
                replacement = QUOT;
            } else if (!inMarkup && b == '\'') {
                replacement = APOS;
            }
            if (replacement != null) {
                out.write(document, copyFrom, i - copyFrom);
                out.write(replacement, 0, replacement.length);
                copyFrom = i + 1;
            }
        }
        out.write(document, copyFrom, document.length - copyFrom);
        return out.toByteArray();
    }

    /**
     * Whether {@code body} is an XML document whose root element is {@code <Error>}, which is how S3 reports a
     * failure inside a 200 response. Looks at the first element only.
     */
    public static boolean isErrorDocument(byte[] body) {
        int i = 0;
        while (i < body.length) {
            i = skipWhitespace(body, i);
            if (i + 1 < body.length && body[i] == '<' && (body[i + 1] == '?' || body[i + 1] == '!')) {
                // XML declaration, comment or doctype: skip to its end.
                while (i < body.length && body[i] != '>') {
                    i++;
                }
                i++;
                continue;
            }
            break;
        }
        if (i >= body.length || body[i] != '<') {
            return false;
        }
        int start = i + 1;
        int end = start;
        while (end < body.length && body[end] != '>' && body[end] != '/' && !isWhitespace(body[end])) {
            end++;
        }
        return end - start == 5
               && body[start] == 'E' && body[start + 1] == 'r' && body[start + 2] == 'r'
               && body[start + 3] == 'o' && body[start + 4] == 'r';
    }

    private static int skipWhitespace(byte[] body, int i) {
        while (i < body.length && isWhitespace(body[i])) {
            i++;
        }
        return i;
    }

    private static boolean isWhitespace(byte b) {
        return b == ' ' || b == '\t' || b == '\r' || b == '\n';
    }
}
