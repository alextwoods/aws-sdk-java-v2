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

import java.util.Objects;

/** A JSON number, kept as its exact text. */
public record NumberJsonNode(String value) implements JsonNode {
    public NumberJsonNode {
        Objects.requireNonNull(value, "value");
    }

    @Override
    public boolean isNumber() {
        return true;
    }

    @Override
    public String asNumber() {
        return value;
    }

    @Override
    public String text() {
        return value;
    }

    @Override
    public <T> T visit(JsonNodeVisitor<T> visitor) {
        return visitor.visitNumber(value);
    }

    @Override
    public String kind() {
        return "number";
    }

    @Override
    public String toString() {
        return value;
    }
}
