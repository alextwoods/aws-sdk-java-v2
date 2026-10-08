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

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * The canonical text of a modeled value, so an expected and an actual value compare with {@code equals} under the
 * Smithy protocol test semantics: structure members and map entries are unordered (a map compares by its key-value
 * pairs, as the Smithy protocol test runners compare deserialized maps; absent members are not rendered), list
 * entries keep their order, numbers compare by numeric value regardless of their Java type (non-finite floats by
 * name), blobs by bytes and timestamps by instant. Documents compare by kind and value.
 */
public final class Canonical {
    private Canonical() {
    }

    /** The canonical text of a modeled structure (output, error or input). */
    public static String of(SerializableStruct value) {
        return render(Document.of(value));
    }

    public static String render(Document document) {
        if (document == null) {
            return "null";
        }
        ShapeType type = document.type();
        return switch (type) {
            case STRUCTURE, UNION -> {
                Map<String, String> members = new TreeMap<>();
                document.asStringMap().forEach((k, v) -> members.put(k, render(v)));
                yield "{" + String.join(", ", members.entrySet().stream()
                                                        .map(e -> e.getKey() + "=" + e.getValue()).toList()) + "}";
            }
            case MAP -> {
                Map<String, String> entries = new TreeMap<>();
                document.asStringMap().forEach((k, v) -> entries.put(k, render(v)));
                yield "map" + entries.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toList();
            }
            case LIST, SET -> {
                List<String> items = new ArrayList<>();
                for (Document item : document.asList()) {
                    items.add(render(item));
                }
                yield items.toString();
            }
            case BLOB -> "blob:" + Base64.getEncoder().encodeToString(bytes(document.asBlob()));
            case TIMESTAMP -> "timestamp:" + document.asTimestamp();
            case STRING, ENUM -> "\"" + document.asString() + "\"";
            case BOOLEAN -> String.valueOf(document.asBoolean());
            case FLOAT, DOUBLE -> {
                double value = document.asNumber().doubleValue();
                yield Double.isFinite(value) ? number(new BigDecimal(document.asNumber().toString()))
                                             : String.valueOf(value);
            }
            case BYTE, SHORT, INTEGER, INT_ENUM, LONG, BIG_INTEGER, BIG_DECIMAL ->
                number(new BigDecimal(document.asNumber().toString()));
            default -> type + ":" + document;
        };
    }

    private static String number(BigDecimal value) {
        return value.signum() == 0 ? "0" : value.stripTrailingZeros().toPlainString();
    }

    static byte[] bytes(ByteBuffer buffer) {
        ByteBuffer view = buffer.duplicate();
        byte[] out = new byte[view.remaining()];
        view.get(out);
        return out;
    }
}
