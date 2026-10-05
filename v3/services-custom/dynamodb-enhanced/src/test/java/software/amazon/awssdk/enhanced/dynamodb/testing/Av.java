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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.internal.json.JsonNode;
import software.amazon.awssdk.enhanced.dynamodb.internal.json.JsonWriter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * DynamoDB JSON wire form of attribute values ({@code {"S":"x"}}, {@code {"N":"1"}}, ...) for the in-process service,
 * plus the DynamoDB comparison rules it needs (numbers compare as decimals, binaries as unsigned bytes, sets ignore
 * order).
 */
public final class Av {
    private Av() {
    }

    public static AttributeValue fromJson(JsonNode node) {
        Map<String, JsonNode> object = node.asObject();
        if (object.size() != 1) {
            throw new IllegalArgumentException("An attribute value must have exactly one member: " + node);
        }
        Map.Entry<String, JsonNode> entry = object.entrySet().iterator().next();
        JsonNode v = entry.getValue();
        return switch (entry.getKey()) {
            case "S" -> AttributeValue.fromS(v.asString());
            case "N" -> AttributeValue.fromN(v.asString());
            case "B" -> AttributeValue.fromB(SdkBytes.fromByteArray(Base64.getDecoder().decode(v.asString())));
            case "SS" -> AttributeValue.fromSs(v.asArray().stream().map(JsonNode::asString).toList());
            case "NS" -> AttributeValue.fromNs(v.asArray().stream().map(JsonNode::asString).toList());
            case "BS" -> AttributeValue.fromBs(v.asArray().stream()
                                               .map(b -> SdkBytes.fromByteArray(Base64.getDecoder()
                                                                                      .decode(b.asString())))
                                               .toList());
            case "M" -> AttributeValue.fromM(mapFromJson(v));
            case "L" -> AttributeValue.fromL(v.asArray().stream().map(Av::fromJson).toList());
            case "NULL" -> AttributeValue.fromNullValue(v.asBoolean());
            case "BOOL" -> AttributeValue.fromBool(v.asBoolean());
            default -> throw new IllegalArgumentException("Unknown attribute value member " + entry.getKey());
        };
    }

    public static Map<String, AttributeValue> mapFromJson(JsonNode node) {
        Map<String, AttributeValue> result = new LinkedHashMap<>();
        node.asObject().forEach((k, v) -> result.put(k, fromJson(v)));
        return result;
    }

    public static void write(JsonWriter w, AttributeValue av) {
        w.writeStartObject();
        switch (av.type()) {
            case S -> w.writeFieldName("S").writeValue(av.s());
            case N -> w.writeFieldName("N").writeValue(av.n());
            case B -> w.writeFieldName("B").writeValue(av.b().asByteBuffer());
            case SS -> w.writeFieldName("SS").writeStringArray(av.ss());
            case NS -> w.writeFieldName("NS").writeStringArray(av.ns());
            case BS -> {
                w.writeFieldName("BS").writeStartArray();
                av.bs().forEach(b -> w.writeValue(b.asByteBuffer()));
                w.writeEndArray();
            }
            case M -> {
                w.writeFieldName("M");
                writeMap(w, av.m());
            }
            case L -> {
                w.writeFieldName("L").writeStartArray();
                av.l().forEach(e -> write(w, e));
                w.writeEndArray();
            }
            case NULL -> w.writeFieldName("NULL").writeValue(true);
            case BOOL -> w.writeFieldName("BOOL").writeValue(av.bool());
            default -> throw new IllegalArgumentException("Cannot write attribute value " + av);
        }
        w.writeEndObject();
    }

    public static void writeMap(JsonWriter w, Map<String, AttributeValue> map) {
        w.writeStartObject();
        map.forEach((k, v) -> {
            w.writeFieldName(k);
            write(w, v);
        });
        w.writeEndObject();
    }

    /** DynamoDB value equality: numbers by value, sets ignoring order, maps and lists recursively. */
    public static boolean same(AttributeValue a, AttributeValue b) {
        if (a == null || b == null || a.type() != b.type()) {
            return false;
        }
        return switch (a.type()) {
            case N -> new BigDecimal(a.n()).compareTo(new BigDecimal(b.n())) == 0;
            case SS -> new HashSet<>(a.ss()).equals(new HashSet<>(b.ss()));
            case NS -> a.ns().stream().map(Av::canonicalNumber).collect(java.util.stream.Collectors.toSet())
                         .equals(b.ns().stream().map(Av::canonicalNumber)
                                  .collect(java.util.stream.Collectors.toSet()));
            case BS -> new HashSet<>(a.bs()).equals(new HashSet<>(b.bs()));
            case M -> a.m().size() == b.m().size()
                      && a.m().entrySet().stream().allMatch(e -> same(e.getValue(), b.m().get(e.getKey())));
            case L -> a.l().size() == b.l().size() && indexesSame(a.l(), b.l());
            default -> a.equals(b);
        };
    }

    /** Ordering of two scalar values of the same type, or {@code null} when they are not comparable. */
    public static Integer compare(AttributeValue a, AttributeValue b) {
        if (a == null || b == null || a.type() != b.type()) {
            return null;
        }
        return switch (a.type()) {
            case N -> new BigDecimal(a.n()).compareTo(new BigDecimal(b.n()));
            case S -> a.s().compareTo(b.s());
            case B -> Arrays.compareUnsigned(a.b().asByteArray(), b.b().asByteArray());
            default -> null;
        };
    }

    private static boolean indexesSame(List<AttributeValue> a, List<AttributeValue> b) {
        for (int i = 0; i < a.size(); i++) {
            if (!same(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static String canonicalNumber(String n) {
        BigDecimal d = new BigDecimal(n);
        return d.signum() == 0 ? "0" : d.stripTrailingZeros().toPlainString();
    }

    /** A deep, order-preserving copy of an item. */
    public static Map<String, AttributeValue> copy(Map<String, AttributeValue> item) {
        return item == null ? null : new LinkedHashMap<>(Objects.requireNonNull(item));
    }

    static List<AttributeValue> list(AttributeValue... values) {
        return new ArrayList<>(Arrays.asList(values));
    }
}
