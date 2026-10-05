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

/** A visitor over {@link JsonNode} kinds. */
public interface JsonNodeVisitor<T> {
    T visitNull();

    T visitBoolean(boolean bool);

    T visitNumber(String number);

    T visitString(String string);

    T visitArray(List<JsonNode> array);

    T visitObject(Map<String, JsonNode> object);
}
