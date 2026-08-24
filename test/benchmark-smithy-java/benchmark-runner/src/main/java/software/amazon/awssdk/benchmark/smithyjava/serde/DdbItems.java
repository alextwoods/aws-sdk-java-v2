package software.amazon.awssdk.benchmark.smithyjava.serde;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import software.amazon.awssdk.core.SdkBytes;

/**
 * Generates DynamoDB items of various sizes/shapes in three parallel representations that are
 * guaranteed structurally identical because they derive from one neutral tree:
 * <ul>
 *   <li>V2 {@code Map<String, software.amazon.awssdk.services.dynamodb.model.AttributeValue>}</li>
 *   <li>Smithy-Java {@code Map<String, ...smithyjava.dynamodb.model.AttributeValue>}</li>
 *   <li>DynamoDB wire-format JSON (for unmarshalling benchmarks)</li>
 * </ul>
 *
 * LinkedHashMap is used everywhere so both SDKs marshal entries in identical order.
 */
public final class DdbItems {

    private DdbItems() {
    }

    // ==================== Neutral attribute tree ====================

    public static final class Attr {
        enum Kind { S, N, B, BOOL, NUL, M, L, SS, NS }

        final Kind kind;
        final String scalar;          // S/N value
        final byte[] bytes;           // B value
        final boolean bool;           // BOOL value
        final Map<String, Attr> map;  // M entries
        final List<Attr> list;        // L entries
        final List<String> strings;   // SS/NS entries

        private Attr(Kind kind, String scalar, byte[] bytes, boolean bool,
                     Map<String, Attr> map, List<Attr> list, List<String> strings) {
            this.kind = kind;
            this.scalar = scalar;
            this.bytes = bytes;
            this.bool = bool;
            this.map = map;
            this.list = list;
            this.strings = strings;
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

        static Attr m(Map<String, Attr> v) {
            return new Attr(Kind.M, null, null, false, v, null, null);
        }

        static Attr l(List<Attr> v) {
            return new Attr(Kind.L, null, null, false, null, v, null);
        }

        static Attr ss(List<String> v) {
            return new Attr(Kind.SS, null, null, false, null, null, v);
        }

        static Attr ns(List<String> v) {
            return new Attr(Kind.NS, null, null, false, null, null, v);
        }
    }

    // ==================== Size ladder ====================

    /**
     * SMALL: the 12-attribute mixed item used by the original pipeline benchmarks (~0.4 KB JSON).
     */
    public static Map<String, Attr> small() {
        Map<String, Attr> item = new LinkedHashMap<>();
        item.put("pk", Attr.s("benchmark-key"));
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
     * MEDIUM: 30 scalar attributes + 10-entry map + 10-element mixed list (~2 KB JSON).
     * Shaped like a typical fat "user profile" row.
     */
    public static Map<String, Attr> medium() {
        Random r = new Random(42);
        Map<String, Attr> item = new LinkedHashMap<>();
        item.put("pk", Attr.s("user#" + randomString(r, 12)));
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

    /**
     * LARGE: document-style row: 10 scalars + list of 50 nested order maps + depth-4 nested map
     * + 2 KB binary blob (~20 KB JSON).
     */
    public static Map<String, Attr> large() {
        Random r = new Random(4242);
        Map<String, Attr> item = new LinkedHashMap<>();
        item.put("pk", Attr.s("account#" + randomString(r, 16)));
        item.put("sk", Attr.s("orders#2026-08"));
        for (int i = 0; i < 8; i++) {
            item.put("attr" + i, i % 2 == 0 ? Attr.s(randomString(r, 24))
                                            : Attr.n(Long.toString(Math.abs(r.nextLong()) % 10_000_000)));
        }
        List<Attr> orders = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            Map<String, Attr> order = new LinkedHashMap<>();
            order.put("orderId", Attr.s("ord-" + randomString(r, 12)));
            order.put("customerName", Attr.s(randomString(r, 24)));
            order.put("total", Attr.n(r.nextInt(100000) + "." + (10 + r.nextInt(89))));
            order.put("shipped", Attr.bool(r.nextBoolean()));
            order.put("quantity", Attr.n(Integer.toString(1 + r.nextInt(20))));
            order.put("tags", Attr.l(List.of(
                Attr.s(randomString(r, 8)), Attr.s(randomString(r, 8)), Attr.s(randomString(r, 8)))));
            Map<String, Attr> meta = new LinkedHashMap<>();
            meta.put("warehouse", Attr.s(randomString(r, 6)));
            meta.put("carrier", Attr.s(randomString(r, 10)));
            meta.put("zone", Attr.n(Integer.toString(r.nextInt(100))));
            order.put("meta", Attr.m(meta));
            orders.add(Attr.m(order));
        }
        item.put("orders", Attr.l(orders));

        // Depth-4 nested map, 4 keys per level.
        item.put("config", nestedMap(r, 4, 4));

        byte[] blob = new byte[2048];
        r.nextBytes(blob);
        item.put("payload", Attr.b(blob));
        return item;
    }

    /**
     * XL: list of 400 flat 10-field maps (~120 KB JSON) — batch/scan-result shaped, stresses
     * repetitive struct deserialization and string decoding throughput.
     */
    public static Map<String, Attr> xl() {
        Random r = new Random(424242);
        Map<String, Attr> item = new LinkedHashMap<>();
        item.put("pk", Attr.s("scan-result"));
        List<Attr> rows = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            Map<String, Attr> row = new LinkedHashMap<>();
            for (int j = 0; j < 8; j++) {
                row.put("col" + j, Attr.s(randomString(r, 20)));
            }
            row.put("id", Attr.n(Integer.toString(i)));
            row.put("version", Attr.n(Integer.toString(r.nextInt(1000))));
            rows.add(Attr.m(row));
        }
        item.put("rows", Attr.l(rows));
        return item;
    }

    public static Map<String, Attr> forSize(String size) {
        switch (size) {
            case "SMALL":
                return small();
            case "MEDIUM":
                return medium();
            case "LARGE":
                return large();
            case "XL":
                return xl();
            default:
                throw new IllegalArgumentException("Unknown size: " + size);
        }
    }

    private static Attr nestedMap(Random r, int depth, int keysPerLevel) {
        Map<String, Attr> map = new LinkedHashMap<>();
        for (int i = 0; i < keysPerLevel; i++) {
            if (depth > 1 && i == 0) {
                map.put("child" + i, nestedMap(r, depth - 1, keysPerLevel));
            } else {
                map.put("key" + i, Attr.s(randomString(r, 12)));
            }
        }
        return Attr.m(map);
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
                return b.b(SdkBytes.fromByteArray(attr.bytes)).build();
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
        com.amazonaws.services.dynamodbv2.model.AttributeValue av =
            new com.amazonaws.services.dynamodbv2.model.AttributeValue();
        switch (attr.kind) {
            case S:
                return av.withS(attr.scalar);
            case N:
                return av.withN(attr.scalar);
            case B:
                return av.withB(ByteBuffer.wrap(attr.bytes));
            case BOOL:
                return av.withBOOL(attr.bool);
            case NUL:
                return av.withNULL(true);
            case SS:
                return av.withSS(attr.strings);
            case NS:
                return av.withNS(attr.strings);
            case M:
                Map<String, com.amazonaws.services.dynamodbv2.model.AttributeValue> m = new LinkedHashMap<>();
                for (Map.Entry<String, Attr> e : attr.map.entrySet()) {
                    m.put(e.getKey(), toV1(e.getValue()));
                }
                return av.withM(m);
            case L:
                List<com.amazonaws.services.dynamodbv2.model.AttributeValue> l = new ArrayList<>();
                for (Attr a : attr.list) {
                    l.add(toV1(a));
                }
                return av.withL(l);
            default:
                throw new IllegalStateException();
        }
    }

    // ==================== Converter: Smithy-Java model ====================

    public static Map<String, software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue> toSj(
        Map<String, Attr> item) {
        Map<String, software.amazon.awssdk.benchmark.smithyjava.dynamodb.model.AttributeValue> out = new LinkedHashMap<>();
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

    // ==================== Converter: DynamoDB wire JSON ====================

    /** Renders {@code {"Item": {...}}} — a GetItem response body in DynamoDB wire format. */
    public static String toGetItemResponseJson(Map<String, Attr> item) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("{\"Item\":");
        appendMap(sb, item);
        sb.append('}');
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
