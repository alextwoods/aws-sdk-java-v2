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

package software.amazon.awssdk.enhanced.dynamodb.internal.document;

import java.util.Map;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.internal.json.JsonWriter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * JSON serializer for DynamoDB Enhanced Client document operations.
 */
final class DocumentJsonSerializer {
    private DocumentJsonSerializer() {
    }

    public static String serializeAttributeValueMap(Map<String, AttributeValue> map) {
        JsonWriter jsonGen = new JsonWriter();

        jsonGen.writeStartObject();
        map.forEach((key, value) -> {
            jsonGen.writeFieldName(key);
            serializeAttributeValue(jsonGen, value);
        });
        jsonGen.writeEndObject();

        return jsonGen.toString();
    }

    public static String serializeSingleAttributeValue(AttributeValue av) {
        JsonWriter jsonGen = new JsonWriter();
        serializeAttributeValue(jsonGen, av);
        return jsonGen.toString();
    }

    public static void serializeAttributeValue(JsonWriter generator, AttributeValue av) {
        switch (av.type()) {
            case NULL:
                generator.writeNull();
                break;
            case S:
                generator.writeValue(av.s());
                break;
            case N:
                generator.writeNumber(av.n());
                break;
            case BOOL:
                generator.writeValue(av.bool());
                break;
            case B:
                generator.writeValue(av.b().asByteBuffer());
                break;
            case L:
                generator.writeStartArray();
                for (AttributeValue item : av.l()) {
                    serializeAttributeValue(generator, item);
                }
                generator.writeEndArray();
                break;
            case M:
                generator.writeStartObject();
                for (Map.Entry<String, AttributeValue> entry : av.m().entrySet()) {
                    generator.writeFieldName(entry.getKey());
                    serializeAttributeValue(generator, entry.getValue());
                }
                generator.writeEndObject();
                break;
            case SS:
                generator.writeStartArray();
                for (String s : av.ss()) {
                    generator.writeValue(s);
                }
                generator.writeEndArray();
                break;
            case NS:
                generator.writeStartArray();
                for (String n : av.ns()) {
                    generator.writeNumber(n);
                }
                generator.writeEndArray();
                break;
            case BS:
                generator.writeStartArray();
                for (SdkBytes b : av.bs()) {
                    generator.writeValue(b.asByteBuffer());
                }
                generator.writeEndArray();
                break;
            default:
                throw new IllegalArgumentException("Unsupported AttributeValue type: " + av.type());
        }
    }

}