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
package software.amazon.awssdk.enhanced.dynamodb.testing;

import software.amazon.awssdk.enhanced.dynamodb.internal.json.JsonNode;
import software.amazon.awssdk.enhanced.dynamodb.internal.json.JsonNodeParser;

/**
 * Reads JSON text into comparable trees for the ported V2 document suites, which compared Jackson trees: object
 * member order is ignored and scalars compare by type and exact text.
 */
public final class JsonTreeReader {
    public JsonNode readTree(String json) {
        return JsonNodeParser.create().parse(json);
    }
}
