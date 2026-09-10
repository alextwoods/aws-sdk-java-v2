package software.amazon.awssdk.benchmark.e2e;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * SDK-neutral DynamoDB item definitions shared by the benchmark runner and the mock server.
 *
 * <p>Items are described once as an {@link Attr} tree and converted to the V1, V2 and smithy-java
 * model types, and to DynamoDB wire-format JSON (for the server's canned responses). All item
 * content is deterministic (seeded {@link Random}), so the client and server processes always
 * agree on shapes and sizes without any coordination beyond this class.
 *
 * <p>Sizes:
 * <ul>
 *   <li>SMALL: 12 mixed-type attributes, ~0.4 KB of wire JSON. Used by small-get / small-put.</li>
 *   <li>MEDIUM: fat "user profile" row, ~2 KB of wire JSON. {@link #BATCH_SIZE} of these make up
 *       a batch (~50 KB per request/response). Used by batch-get / batch-put.</li>
 * </ul>
 */
public final class BenchmarkItems {

    public static final String TABLE_NAME = "benchmark-table";
    public static final String SMALL_KEY = "benchmark-key";
    /** 25 is the BatchWriteItem maximum; BatchGetItem uses the same count for symmetry. */
    public static final int BATCH_SIZE = 25;

    private BenchmarkItems() {
    }

    // ==================== Neutral attribute tree ====================

    public static final class Attr {
        enum Kind { S, N, B, BOOL, NUL, SS, NS, M, L }

        final Kind kind;
        final String scalar;
        final byte[] bytes;
        final boolean bool;
        final List<String> strings;
        final Map<String, Attr> map;
        final List<Attr> list;

        private Attr(Kind kind, String scalar, byte[] bytes, boolean bool,
                     List<String> strings, Map<String, Attr> map, List<Attr> list) {
            this.kind = kind;
            this.scalar = scalar;
            this.bytes = bytes;
            this.bool = bool;
            this.strings = strings;
            this.map = map;
            this.list = list;
        }

        static Attr s(String v) {
            return new Attr(Kind.S, v, null, false, null, null, null);
        }

        static Attr n(String v) {
            return new Attr(Kind.N, v, null, false, null, null, null);
        }

        static Attr b(byte[] v) {
            return new Attr(Kind.B, null, v, false, null, null, null);
        }

        static Attr bool(boolean v) {
            return new Attr(Kind.BOOL, null, null, v, null, null, null);
        }

        static Attr nul() {
            return new Attr(Kind.NUL, null, null, false, null, null, null);
        }

        static Attr ss(List<String> v) {
            return new Attr(Kind.SS, null, null, false, v, null, null);
        }

        static Attr ns(List<String> v) {
            return new Attr(Kind.NS, null, null, false, v, null, null);
        }

        static Attr m(Map<String, Attr> v) {
            return new Attr(Kind.M, null, null, false, null, v, null);
        }

        static Attr l(List<Attr> v) {
            return new Attr(Kind.L, null, null, false, null, null, v);
        }
    }

    // ==================== Item definitions ====================

    /** SMALL: 12 mixed-type attributes (~0.4 KB wire JSON). */
    public static Map<String, Attr> smallItem() {
        Map<String, Attr> item = new LinkedHashMap<>();
        item.put("pk", Attr.s(SMALL_KEY));
        item.put("sk", Attr.n("100"));
        item.put("stringField", Attr.s("test-value"));
        item.put("numberField", Attr.n("123.456"));
        item.put("binaryField", Attr.b("hello world".getBytes(StandardCharsets.UTF_8)));
        item.put("stringSetField", Attr.ss(List.of("value1", "value2", "value3")));
        item.put("numberSetField", Attr.ns(List.of("1.1", "2.2", "3.3")));
        item.put("boolField", Attr.bool(false));
        item.put("nullField", Attr.nul());
        Map<String, Attr> nested = new LinkedHashMap<>();
        nested.put("nested", Attr.s("nested-value"));
        Map<String, Attr> deep = new LinkedHashMap<>();
        deep.put("level2", Attr.n("999"));
        nested.put("deepNested", Attr.m(deep));
        item.put("mapField", Attr.m(nested));
        item.put("listField", Attr.l(List.of(
            Attr.s("item1"), Attr.n("42"), Attr.bool(true), Attr.nul())));
        return item;
    }

    /**
     * MEDIUM: fat "user profile" row (~2 KB wire JSON): 28 scalar attributes + 10-entry map +
     * 10-element mixed list. {@code index} makes each batch member unique and drives the seed,
     * so content is deterministic per index.
     */
    public static Map<String, Attr> mediumItem(int index) {
        Random r = new Random(42 + index);
        Map<String, Attr> item = new LinkedHashMap<>();
        item.put("pk", Attr.s(batchKey(index)));
        item.put("sk", Attr.s("profile#v2"));
        for (int i = 0; i < 20; i++) {
            item.put("strAttr" + i, Attr.s(randomString(r, 16)));
        }
        for (int i = 0; i < 8; i++) {
            item.put("numAttr" + i, Attr.n(Long.toString(r.nextLong() % 1_000_000)));
        }
        Map<String, Attr> prefs = new LinkedHashMap<>();
        for (int i = 0; i < 10; i++) {
            prefs.put("pref" + i, Attr.s(randomString(r, 10)));
        }
        item.put("preferences", Attr.m(prefs));
        List<Attr> events = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            events.add(i % 3 == 0 ? Attr.n(Integer.toString(r.nextInt(100000)))
                                  : Attr.s(randomString(r, 12)));
        }
        item.put("recentEvents", Attr.l(events));
        item.put("active", Attr.bool(true));
        return item;
    }

    public static String batchKey(int index) {
        return String.format("user#%05d", index);
    }

    public static List<Map<String, Attr>> batchItems() {
        List<Map<String, Attr>> items = new ArrayList<>(BATCH_SIZE);
        for (int i = 0; i < BATCH_SIZE; i++) {
            items.add(mediumItem(i));
        }
        return items;
    }

    private static String randomString(Random r, int len) {
        char[] chars = new char[len];
        for (int i = 0; i < len; i++) {
            chars[i] = (char) ('a' + r.nextInt(26));
        }
        return new String(chars);
    }

    // ==================== Converter: V2 model ====================

    public static Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> toV2(
            Map<String, Attr> item) {
        Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> out = new LinkedHashMap<>();
        for (Map.Entry<String, Attr> e : item.entrySet()) {
            out.put(e.getKey(), toV2(e.getValue()));
        }
        return out;
    }

    private static software.amazon.awssdk.services.dynamodb.model.AttributeValue toV2(Attr attr) {
        software.amazon.awssdk.services.dynamodb.model.AttributeValue.Builder b =
            software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder();
        switch (attr.kind) {
            case S:
                return b.s(attr.scalar).build();
            case N:
                return b.n(attr.scalar).build();
            case B:
                return b.b(software.amazon.awssdk.core.SdkBytes.fromByteArray(attr.bytes)).build();
            case BOOL:
                return b.bool(attr.bool).build();
            case NUL:
                return b.nul(true).build();
            case SS:
                return b.ss(attr.strings).build();
            case NS:
                return b.ns(attr.strings).build();
            case M:
                Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue> m = new LinkedHashMap<>();
                for (Map.Entry<String, Attr> e : attr.map.entrySet()) {
                    m.put(e.getKey(), toV2(e.getValue()));
                }
                return b.m(m).build();
            case L:
                List<software.amazon.awssdk.services.dynamodb.model.AttributeValue> l = new ArrayList<>();
                for (Attr a : attr.list) {
                    l.add(toV2(a));
                }
                return b.l(l).build();
            default:
                throw new IllegalStateException();
        }
    }

    // ==================== Converter: V1 model ====================

    public static Map<String, com.amazonaws.services.dynamodbv2.model.AttributeValue> toV1(
            Map<String, Attr> item) {
        Map<String, com.amazonaws.services.dynamodbv2.model.AttributeValue> out = new LinkedHashMap<>();
        for (Map.Entry<String, Attr> e : item.entrySet()) {
            out.put(e.getKey(), toV1(e.getValue()));
        }
        return out;
    }

    private static com.amazonaws.services.dynamodbv2.model.AttributeValue toV1(Attr attr) {
        com.amazonaws.services.dynamodbv2.model.AttributeValue v =
            new com.amazonaws.services.dynamodbv2.model.AttributeValue();
        switch (attr.kind) {
            case S:
                return v.withS(attr.scalar);
            case N:
                return v.withN(attr.scalar);
            case B:
                return v.withB(ByteBuffer.wrap(attr.bytes));
            case BOOL:
                return v.withBOOL(attr.bool);
            case NUL:
                return v.withNULL(true);
            case SS:
                return v.withSS(attr.strings);
            case NS:
                return v.withNS(attr.strings);
            case M:
                Map<String, com.amazonaws.services.dynamodbv2.model.AttributeValue> m = new LinkedHashMap<>();
                for (Map.Entry<String, Attr> e : attr.map.entrySet()) {
                    m.put(e.getKey(), toV1(e.getValue()));
                }
                return v.withM(m);
            case L:
                List<com.amazonaws.services.dynamodbv2.model.AttributeValue> l = new ArrayList<>();
                for (Attr a : attr.list) {
                    l.add(toV1(a));
                }
                return v.withL(l);
            default:
                throw new IllegalStateException();
        }
    }

    // ==================== Converter: smithy-java model ====================

    public static Map<String, software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue> toSj(
            Map<String, Attr> item) {
        Map<String, software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue> out =
            new LinkedHashMap<>();
        for (Map.Entry<String, Attr> e : item.entrySet()) {
            out.put(e.getKey(), toSj(e.getValue()));
        }
        return out;
    }

    private static software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue toSj(Attr attr) {
        software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue.Builder b =
            software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue.builder();
        switch (attr.kind) {
            case S:
                return b.s(attr.scalar).build();
            case N:
                return b.n(attr.scalar).build();
            case B:
                return b.b(ByteBuffer.wrap(attr.bytes)).build();
            case BOOL:
                return b.bool(attr.bool).build();
            case NUL:
                return b.nullMember(true).build();
            case SS:
                return b.ss(attr.strings).build();
            case NS:
                return b.ns(attr.strings).build();
            case M:
                Map<String, software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue> m =
                    new LinkedHashMap<>();
                for (Map.Entry<String, Attr> e : attr.map.entrySet()) {
                    m.put(e.getKey(), toSj(e.getValue()));
                }
                return b.m(m).build();
            case L:
                List<software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue> l = new ArrayList<>();
                for (Attr a : attr.list) {
                    l.add(toSj(a));
                }
                return b.l(l).build();
            default:
                throw new IllegalStateException();
        }
    }

    // ==================== DynamoDB wire-format JSON (server responses) ====================

    /** {@code {"Item": {...}}} — GetItem response. */
    public static String getItemResponseJson() {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("{\"Item\":");
        appendMap(sb, smallItem());
        sb.append('}');
        return sb.toString();
    }

    /** {@code {}} — PutItem response when ReturnValues is not requested. */
    public static String putItemResponseJson() {
        return "{}";
    }

    /** {@code {"Responses":{"benchmark-table":[...]},"UnprocessedKeys":{}}} — BatchGetItem response. */
    public static String batchGetItemResponseJson() {
        StringBuilder sb = new StringBuilder(64 * 1024);
        sb.append("{\"Responses\":{\"").append(TABLE_NAME).append("\":[");
        List<Map<String, Attr>> items = batchItems();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            appendMap(sb, items.get(i));
        }
        sb.append("]},\"UnprocessedKeys\":{}}");
        return sb.toString();
    }

    /** {@code {"UnprocessedItems":{}}} — fully successful BatchWriteItem response. */
    public static String batchWriteItemResponseJson() {
        return "{\"UnprocessedItems\":{}}";
    }

    /**
     * {@code {"Table": {...}}} — DescribeTable response.
     *
     * <p>Exists because the other four scenarios cannot see structure deserialization. A DynamoDB item is a map of
     * {@code AttributeValue}, which is a <i>union</i>, so get/put/batch responses materialize hundreds of unions and
     * essentially one structure (the response shape itself). This response is the opposite shape: <b>46 nested
     * structures and no unions</b> — one {@code TableDescription}, 6 {@code AttributeDefinition}, 2 top-level
     * {@code KeySchemaElement}, a {@code ProvisionedThroughputDescription}, 5 global secondary indexes and 2 local
     * secondary indexes each carrying their own key schema, projection and throughput, plus stream and SSE
     * descriptions. That makes it the scenario in which per-structure construction cost is visible at all.
     *
     * <p>Values are realistic but fixed; the response is a canned constant like every other, so no client is charged
     * for server-side variability.
     */
    public static String describeTableResponseJson() {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("{\"Table\":{");
        sb.append("\"TableName\":\"").append(TABLE_NAME).append("\",");
        sb.append("\"TableStatus\":\"ACTIVE\",");
        sb.append("\"TableArn\":\"arn:aws:dynamodb:us-east-1:123456789012:table/").append(TABLE_NAME).append("\",");
        sb.append("\"TableId\":\"12345678-1234-1234-1234-123456789012\",");
        sb.append("\"CreationDateTime\":1.6e9,");
        sb.append("\"ItemCount\":483920,");
        sb.append("\"TableSizeBytes\":98431029,");
        sb.append("\"TableClassSummary\":{\"TableClass\":\"STANDARD\"},");

        sb.append("\"AttributeDefinitions\":[");
        String[][] attrs = {{"pk", "S"}, {"sk", "S"}, {"gsi1pk", "S"}, {"gsi1sk", "N"}, {"lsi1sk", "N"}, {"status", "S"}};
        for (int i = 0; i < attrs.length; i++) {
            sb.append(i > 0 ? "," : "")
              .append("{\"AttributeName\":\"").append(attrs[i][0]).append("\",\"AttributeType\":\"")
              .append(attrs[i][1]).append("\"}");
        }
        sb.append("],");

        sb.append("\"KeySchema\":[")
          .append("{\"AttributeName\":\"pk\",\"KeyType\":\"HASH\"},")
          .append("{\"AttributeName\":\"sk\",\"KeyType\":\"RANGE\"}],");

        sb.append("\"ProvisionedThroughput\":{")
          .append("\"ReadCapacityUnits\":250,\"WriteCapacityUnits\":125,")
          .append("\"NumberOfDecreasesToday\":2,")
          .append("\"LastIncreaseDateTime\":1.7e9,\"LastDecreaseDateTime\":1.68e9},");

        sb.append("\"GlobalSecondaryIndexes\":[");
        for (int i = 0; i < 5; i++) {
            sb.append(i > 0 ? "," : "").append('{')
              .append("\"IndexName\":\"gsi-").append(i).append("\",")
              .append("\"IndexStatus\":\"ACTIVE\",")
              .append("\"Backfilling\":false,")
              .append("\"IndexArn\":\"arn:aws:dynamodb:us-east-1:123456789012:table/")
              .append(TABLE_NAME).append("/index/gsi-").append(i).append("\",")
              .append("\"ItemCount\":").append(120000 + i).append(',')
              .append("\"IndexSizeBytes\":").append(4500000 + i).append(',')
              .append("\"KeySchema\":[{\"AttributeName\":\"gsi1pk\",\"KeyType\":\"HASH\"},")
              .append("{\"AttributeName\":\"gsi1sk\",\"KeyType\":\"RANGE\"}],")
              .append("\"Projection\":{\"ProjectionType\":\"INCLUDE\",")
              .append("\"NonKeyAttributes\":[\"status\",\"updatedAt\",\"owner\"]},")
              .append("\"ProvisionedThroughput\":{\"ReadCapacityUnits\":100,\"WriteCapacityUnits\":50,")
              .append("\"NumberOfDecreasesToday\":0}")
              .append('}');
        }
        sb.append("],");

        sb.append("\"LocalSecondaryIndexes\":[");
        for (int i = 0; i < 2; i++) {
            sb.append(i > 0 ? "," : "").append('{')
              .append("\"IndexName\":\"lsi-").append(i).append("\",")
              .append("\"IndexArn\":\"arn:aws:dynamodb:us-east-1:123456789012:table/")
              .append(TABLE_NAME).append("/index/lsi-").append(i).append("\",")
              .append("\"ItemCount\":").append(80000 + i).append(',')
              .append("\"IndexSizeBytes\":").append(2200000 + i).append(',')
              .append("\"KeySchema\":[{\"AttributeName\":\"pk\",\"KeyType\":\"HASH\"},")
              .append("{\"AttributeName\":\"lsi1sk\",\"KeyType\":\"RANGE\"}],")
              .append("\"Projection\":{\"ProjectionType\":\"ALL\"}")
              .append('}');
        }
        sb.append("],");

        sb.append("\"StreamSpecification\":{\"StreamEnabled\":true,\"StreamViewType\":\"NEW_AND_OLD_IMAGES\"},");
        sb.append("\"LatestStreamLabel\":\"2026-01-01T00:00:00.000\",");
        sb.append("\"LatestStreamArn\":\"arn:aws:dynamodb:us-east-1:123456789012:table/")
          .append(TABLE_NAME).append("/stream/2026-01-01T00:00:00.000\",");
        sb.append("\"SSEDescription\":{\"Status\":\"ENABLED\",\"SSEType\":\"KMS\",")
          .append("\"KMSMasterKeyArn\":\"arn:aws:kms:us-east-1:123456789012:key/abcd1234\"},");
        sb.append("\"DeletionProtectionEnabled\":true");
        sb.append("}}");
        return sb.toString();
    }

    private static void appendMap(StringBuilder sb, Map<String, Attr> map) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, Attr> e : map.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            appendString(sb, e.getKey());
            sb.append(':');
            appendAttr(sb, e.getValue());
        }
        sb.append('}');
    }

    private static void appendAttr(StringBuilder sb, Attr attr) {
        sb.append('{');
        switch (attr.kind) {
            case S:
                sb.append("\"S\":");
                appendString(sb, attr.scalar);
                break;
            case N:
                sb.append("\"N\":");
                appendString(sb, attr.scalar);
                break;
            case B:
                sb.append("\"B\":");
                appendString(sb, Base64.getEncoder().encodeToString(attr.bytes));
                break;
            case BOOL:
                sb.append("\"BOOL\":").append(attr.bool);
                break;
            case NUL:
                sb.append("\"NULL\":true");
                break;
            case SS:
            case NS:
                sb.append('"').append(attr.kind.name()).append("\":[");
                for (int i = 0; i < attr.strings.size(); i++) {
                    if (i > 0) {
                        sb.append(',');
                    }
                    appendString(sb, attr.strings.get(i));
                }
                sb.append(']');
                break;
            case M:
                sb.append("\"M\":");
                appendMap(sb, attr.map);
                break;
            case L:
                sb.append("\"L\":[");
                for (int i = 0; i < attr.list.size(); i++) {
                    if (i > 0) {
                        sb.append(',');
                    }
                    appendAttr(sb, attr.list.get(i));
                }
                sb.append(']');
                break;
            default:
                throw new IllegalStateException();
        }
        sb.append('}');
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
}
