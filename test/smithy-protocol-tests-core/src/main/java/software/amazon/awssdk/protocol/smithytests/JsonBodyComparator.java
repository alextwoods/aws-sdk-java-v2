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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;

/**
 * Compares JSON bodies structurally. Both sides are parsed with json-utils' {@link JsonNodeParser}, which is independent
 * of the SDK codecs under test. Object member order is ignored and numbers compare by value ({@code 1} equals
 * {@code 1.0}).
 */
final class JsonBodyComparator implements BodyComparator {
    private static final JsonNodeParser PARSER = JsonNodeParser.create();

    @Override
    public void assertMatches(String expected, byte[] actual) {
        String actualText = new String(actual, StandardCharsets.UTF_8);
        Object expectedTree = toTree(parse("expected", expected));
        Object actualTree = toTree(parse("actual", actualText));
        TreeComparison.assertTreesEqual("JSON", expectedTree, actualTree, expected, actualText);
    }

    private static JsonNode parse(String side, String json) {
        try {
            return PARSER.parse(json);
        } catch (RuntimeException e) {
            throw new AssertionError("The " + side + " body is not valid JSON: " + e.getMessage() + "\n" + json, e);
        }
    }

    private static Object toTree(JsonNode node) {
        if (node.isNull()) {
            return null;
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isString()) {
            return node.asString();
        }
        if (node.isNumber()) {
            return new BigDecimal(node.asNumber());
        }
        if (node.isArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonNode element : node.asArray()) {
                list.add(toTree(element));
            }
            return list;
        }
        if (node.isObject()) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Map.Entry<String, JsonNode> entry : node.asObject().entrySet()) {
                map.put(entry.getKey(), toTree(entry.getValue()));
            }
            return map;
        }
        throw new AssertionError("Unsupported JSON node: " + node);
    }
}
