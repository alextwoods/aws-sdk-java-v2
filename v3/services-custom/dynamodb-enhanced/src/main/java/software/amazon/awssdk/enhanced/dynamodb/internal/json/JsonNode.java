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
package software.amazon.awssdk.enhanced.dynamodb.internal.json;

import java.util.List;
import java.util.Map;

/**
 * An immutable JSON value: the replacement for V2's {@code software.amazon.awssdk.enhanced.dynamodb.internal.json.JsonNode}, which
 * has no V3 counterpart. Numbers keep their exact source text. {@link #toString()} is the compact JSON form.
 */
public sealed interface JsonNode
    permits NullJsonNode, BooleanJsonNode, NumberJsonNode, StringJsonNode, ArrayJsonNode, ObjectJsonNode {

    default boolean isNull() {
        return false;
    }

    default boolean isBoolean() {
        return false;
    }

    default boolean isNumber() {
        return false;
    }

    default boolean isString() {
        return false;
    }

    default boolean isArray() {
        return false;
    }

    default boolean isObject() {
        return false;
    }

    default String asNumber() {
        throw new UnsupportedOperationException("A JSON " + kind() + " cannot be converted to a number.");
    }

    default String asString() {
        throw new UnsupportedOperationException("A JSON " + kind() + " cannot be converted to a string.");
    }

    default boolean asBoolean() {
        throw new UnsupportedOperationException("A JSON " + kind() + " cannot be converted to a boolean.");
    }

    default List<JsonNode> asArray() {
        throw new UnsupportedOperationException("A JSON " + kind() + " cannot be converted to an array.");
    }

    default Map<String, JsonNode> asObject() {
        throw new UnsupportedOperationException("A JSON " + kind() + " cannot be converted to an object.");
    }

    /** The scalar text of this node (string contents, number text, true/false, null), or null for containers. */
    String text();

    <T> T visit(JsonNodeVisitor<T> visitor);

    /** A short name of this node's kind, for diagnostics. */
    String kind();
}
