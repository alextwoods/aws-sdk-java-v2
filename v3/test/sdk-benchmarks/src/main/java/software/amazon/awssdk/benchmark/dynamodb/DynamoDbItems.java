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

package software.amazon.awssdk.benchmark.dynamodb;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * DynamoDB benchmark items and their wire JSON, ported from the racecar benchmark
 * ({@code feature/poc/racecar:test/standalone-e2e-benchmarks/.../BenchmarkItems.java}) so that V3 is measured against
 * the same payloads and the mock server returns the same bytes.
 *
 * <p>{@link #smallItem()} is racecar's small item exactly (11 attributes, no {@code BS}), because it is the body of
 * the canned GetItem response and the PutItem request, and both must match racecar byte for byte.
 * {@link #smallItemAllKinds()} adds a {@code BS} attribute so the protocol smoke test covers every
 * {@code AttributeValue} kind; it is never used by the benchmark.
 *
 * <p>Items are built directly as generated V3 {@link AttributeValue}s; the wire JSON is written independently of the
 * V3 serializer so it can serve as the expected value of serialization and as canned response bodies.
 */
public final class DynamoDbItems {
    public static final String TABLE_NAME = "benchmark-table";
    public static final String SMALL_KEY = "benchmark-key";
    /** The BatchWriteItem maximum; BatchGetItem uses the same count. */
    public static final int BATCH_SIZE = 25;

    private DynamoDbItems() {
    }

    /** SMALL: racecar's 11 attributes covering S, N, B, SS, NS, BOOL, NULL, M (nested two deep) and L. */
    public static Map<String, AttributeValue> smallItem() {
        Map<String, AttributeValue> item = new LinkedHashMap<>();
        item.put("pk", AttributeValue.fromS(SMALL_KEY));
        item.put("sk", AttributeValue.fromN("100"));
        item.put("stringField", AttributeValue.fromS("test-value"));
        item.put("numberField", AttributeValue.fromN("123.456"));
        item.put("binaryField", AttributeValue.fromB(SdkBytes.fromUtf8String("hello world")));
        item.put("stringSetField", AttributeValue.fromSs(List.of("value1", "value2", "value3")));
        item.put("numberSetField", AttributeValue.fromNs(List.of("1.1", "2.2", "3.3")));
        item.put("boolField", AttributeValue.fromBool(false));
        item.put("nullField", AttributeValue.fromNullValue(true));
        Map<String, AttributeValue> deep = new LinkedHashMap<>();
        deep.put("level2", AttributeValue.fromN("999"));
        Map<String, AttributeValue> nested = new LinkedHashMap<>();
        nested.put("nested", AttributeValue.fromS("nested-value"));
        nested.put("deepNested", AttributeValue.fromM(deep));
        item.put("mapField", AttributeValue.fromM(nested));
        item.put("listField", AttributeValue.fromL(List.of(AttributeValue.fromS("item1"), AttributeValue.fromN("42"),
                                                           AttributeValue.fromBool(true),
                                                           AttributeValue.fromNullValue(true))));
        return item;
    }

    /** {@link #smallItem()} plus a {@code BS} attribute, so every {@code AttributeValue} kind is present. */
    public static Map<String, AttributeValue> smallItemAllKinds() {
        Map<String, AttributeValue> item = new LinkedHashMap<>();
        for (Map.Entry<String, AttributeValue> e : smallItem().entrySet()) {
            item.put(e.getKey(), e.getValue());
            if ("numberSetField".equals(e.getKey())) {
                item.put("binarySetField", AttributeValue.fromBs(List.of(SdkBytes.fromUtf8String("a"),
                                                                         SdkBytes.fromByteArray(new byte[] {0, 1, -1}))));
            }
        }
        return item;
    }

    /** MEDIUM: a ~1.5 KB "user profile" row; {@code index} seeds deterministic, unique content. */
    public static Map<String, AttributeValue> mediumItem(int index) {
        Random r = new Random(42 + index);
        Map<String, AttributeValue> item = new LinkedHashMap<>();
        item.put("pk", AttributeValue.fromS(batchKey(index)));
        item.put("sk", AttributeValue.fromS("profile#v2"));
        for (int i = 0; i < 20; i++) {
            item.put("strAttr" + i, AttributeValue.fromS(randomString(r, 16)));
        }
        for (int i = 0; i < 8; i++) {
            item.put("numAttr" + i, AttributeValue.fromN(Long.toString(r.nextLong() % 1_000_000)));
        }
        Map<String, AttributeValue> prefs = new LinkedHashMap<>();
        for (int i = 0; i < 10; i++) {
            prefs.put("pref" + i, AttributeValue.fromS(randomString(r, 10)));
        }
        item.put("preferences", AttributeValue.fromM(prefs));
        List<AttributeValue> events = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            events.add(i % 3 == 0 ? AttributeValue.fromN(Integer.toString(r.nextInt(100000)))
                                  : AttributeValue.fromS(randomString(r, 12)));
        }
        item.put("recentEvents", AttributeValue.fromL(events));
        item.put("active", AttributeValue.fromBool(true));
        return item;
    }

    public static String batchKey(int index) {
        return String.format("user#%05d", index);
    }

    public static List<Map<String, AttributeValue>> batchItems() {
        List<Map<String, AttributeValue>> items = new ArrayList<>(BATCH_SIZE);
        for (int i = 0; i < BATCH_SIZE; i++) {
            items.add(mediumItem(i));
        }
        return items;
    }

    /** {@code {"Item": <small item>}}: the canned GetItem response. */
    public static String getItemResponseJson() {
        return "{\"Item\":" + json(smallItem()) + "}";
    }

    /** {@code {}}: the canned PutItem response (no ReturnValues requested). */
    public static String putItemResponseJson() {
        return "{}";
    }

    /** {@code {"Responses":{"benchmark-table":[25 medium items]},"UnprocessedKeys":{}}}. */
    public static String batchGetItemResponseJson() {
        StringBuilder sb = new StringBuilder("{\"Responses\":{\"").append(TABLE_NAME).append("\":[");
        List<Map<String, AttributeValue>> items = batchItems();
        for (int i = 0; i < items.size(); i++) {
            sb.append(i > 0 ? "," : "").append(json(items.get(i)));
        }
        return sb.append("]},\"UnprocessedKeys\":{}}").toString();
    }

    /** {@code {"UnprocessedItems":{}}}: the canned, fully successful BatchWriteItem response. */
    public static String batchWriteItemResponseJson() {
        return "{\"UnprocessedItems\":{}}";
    }

    public static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    /** DynamoDB wire JSON of an item map, written without the V3 serializer. */
    public static String json(Map<String, AttributeValue> item) {
        StringBuilder sb = new StringBuilder();
        appendMap(sb, item);
        return sb.toString();
    }

    private static void appendMap(StringBuilder sb, Map<String, AttributeValue> map) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, AttributeValue> e : map.entrySet()) {
            sb.append(first ? "" : ",");
            first = false;
            appendString(sb, e.getKey());
            sb.append(':');
            appendAttr(sb, e.getValue());
        }
        sb.append('}');
    }

    private static void appendAttr(StringBuilder sb, AttributeValue value) {
        sb.append('{');
        switch (value.type()) {
            case S -> appendString(sb.append("\"S\":"), value.s());
            case N -> appendString(sb.append("\"N\":"), value.n());
            case B -> appendString(sb.append("\"B\":"), base64(value.b()));
            case BOOL -> sb.append("\"BOOL\":").append(value.bool());
            case NULL -> sb.append("\"NULL\":").append(value.nullValue());
            case SS -> appendStrings(sb.append("\"SS\":"), value.ss());
            case NS -> appendStrings(sb.append("\"NS\":"), value.ns());
            case BS -> appendStrings(sb.append("\"BS\":"), value.bs().stream().map(DynamoDbItems::base64).toList());
            case M -> appendMap(sb.append("\"M\":"), value.m());
            case L -> {
                sb.append("\"L\":[");
                for (int i = 0; i < value.l().size(); i++) {
                    sb.append(i > 0 ? "," : "");
                    appendAttr(sb, value.l().get(i));
                }
                sb.append(']');
            }
            default -> throw new IllegalStateException("Unexpected attribute type " + value.type());
        }
        sb.append('}');
    }

    private static void appendStrings(StringBuilder sb, List<String> values) {
        sb.append('[');
        for (int i = 0; i < values.size(); i++) {
            sb.append(i > 0 ? "," : "");
            appendString(sb, values.get(i));
        }
        sb.append(']');
    }

    private static void appendString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\');
            }
            sb.append(c);
        }
        sb.append('"');
    }

    private static String base64(SdkBytes bytes) {
        return Base64.getEncoder().encodeToString(bytes.asByteArray());
    }

    private static String randomString(Random r, int len) {
        char[] chars = new char[len];
        for (int i = 0; i < len; i++) {
            chars[i] = (char) ('a' + r.nextInt(26));
        }
        return new String(chars);
    }
}
