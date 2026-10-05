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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** A JSON object; member order is preserved. */
public record ObjectJsonNode(Map<String, JsonNode> value) implements JsonNode {
    public ObjectJsonNode {
        value = Collections.unmodifiableMap(new LinkedHashMap<>(value));
    }

    @Override
    public boolean isObject() {
        return true;
    }

    @Override
    public Map<String, JsonNode> asObject() {
        return value;
    }

    @Override
    public String text() {
        return null;
    }

    @Override
    public <T> T visit(JsonNodeVisitor<T> visitor) {
        return visitor.visitObject(value);
    }

    @Override
    public String kind() {
        return "object";
    }

    @Override
    public String toString() {
        JsonWriter writer = new JsonWriter();
        writer.writeNode(this);
        return writer.toString();
    }
}
