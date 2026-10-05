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

/** The JSON null. */
public final class NullJsonNode implements JsonNode {
    private static final NullJsonNode INSTANCE = new NullJsonNode();

    private NullJsonNode() {
    }

    public static NullJsonNode instance() {
        return INSTANCE;
    }

    @Override
    public boolean isNull() {
        return true;
    }

    @Override
    public String text() {
        return null;
    }

    @Override
    public <T> T visit(JsonNodeVisitor<T> visitor) {
        return visitor.visitNull();
    }

    @Override
    public String kind() {
        return "null";
    }

    @Override
    public String toString() {
        return "null";
    }
}
