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

package software.amazon.awssdk.test.protocoltests.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.opentest4j.AssertionFailedError;
import software.amazon.smithy.model.node.Node;

/**
 * Body comparison by {@code bodyMediaType}, as the Smithy protocol test specification requires: JSON and XML bodies
 * are compared as documents (not bytes), form-encoded bodies as their unordered parameter pairs, CBOR bodies (base64 in
 * the case) as their decoded data items, and every other body byte for byte.
 */
public final class Bodies {
    private Bodies() {
    }

    /** Whether {@code mediaType} is a binary type whose case bodies are base64-encoded. */
    public static boolean binary(String mediaType) {
        return mediaType.contains("cbor");
    }

    /** The bytes a case body denotes. */
    public static byte[] bytes(String body, String mediaType) {
        return binary(mediaType) ? Base64.getDecoder().decode(body) : body.getBytes(StandardCharsets.UTF_8);
    }

    /** Asserts that {@code actual} is the body {@code expected} of media type {@code mediaType} denotes. */
    public static void assertBody(String expected, String mediaType, byte[] actual) {
        String text = new String(actual, StandardCharsets.UTF_8);
        if (expected.isEmpty() || mediaType.isEmpty()) {
            if (binary(mediaType)) {
                assertEquals(expected, Base64.getEncoder().encodeToString(actual), "body (base64)");
            } else {
                assertEquals(expected, text, "body");
            }
        } else if (mediaType.contains("json")) {
            Node actualJson;
            try {
                actualJson = Node.parse(text);
            } catch (RuntimeException e) {
                throw new AssertionFailedError("The body is not JSON", expected, text);
            }
            assertEquals(normalize(Node.parse(expected)), normalize(actualJson), () -> "JSON body " + text);
        } else if (mediaType.contains("xml")) {
            XmlBodies.assertEquivalent(expected, text);
        } else if (mediaType.contains("x-www-form-urlencoded")) {
            assertEquals(formPairs(expected), formPairs(text), () -> "form body " + text);
        } else if (binary(mediaType)) {
            Object expectedItem = CborValues.decode(Base64.getDecoder().decode(expected));
            Object actualItem;
            try {
                actualItem = CborValues.decode(actual);
            } catch (IllegalArgumentException e) {
                throw new AssertionFailedError("The body is not CBOR: " + e.getMessage(), expected,
                                               Base64.getEncoder().encodeToString(actual));
            }
            assertEquals(expectedItem, actualItem, "CBOR body");
        } else {
            assertEquals(expected, text, "body");
        }
    }

    /** The raw {@code key=value} pairs of a form body, sorted (parameter order is not significant). */
    static List<String> formPairs(String body) {
        if (body.isEmpty()) {
            return List.of();
        }
        List<String> pairs = new ArrayList<>(Arrays.asList(body.split("&", -1)));
        pairs.sort(null);
        return pairs;
    }

    /** A comparable form of a JSON value in which numbers compare by numeric value ({@code 2} equals {@code 2.0}). */
    static Object normalize(Node node) {
        if (node.isObjectNode()) {
            Map<String, Object> out = new TreeMap<>();
            node.expectObjectNode().getStringMap().forEach((k, v) -> out.put(k, normalize(v)));
            return out;
        }
        if (node.isArrayNode()) {
            List<Object> out = new ArrayList<>();
            node.expectArrayNode().getElements().forEach(e -> out.add(normalize(e)));
            return out;
        }
        if (node.isNumberNode()) {
            BigDecimal value = new BigDecimal(node.expectNumberNode().getValue().toString());
            return value.signum() == 0 ? BigDecimal.ZERO : value.stripTrailingZeros();
        }
        return node;
    }
}
