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

package software.amazon.awssdk.protocols.json.internal.unmarshall;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.protocols.json.JsonMemberTable;
import software.amazon.awssdk.protocols.json.StructuredJsonReadable;
import software.amazon.awssdk.protocols.json.StructuredJsonReader;
import software.amazon.awssdk.protocols.json.internal.AwsStructuredPlainJsonFactory;
import software.amazon.awssdk.thirdparty.jackson.core.JsonParser;

/**
 * Differential test: the byte-level {@link FastJsonStructuredReader} must produce objects equal to
 * the Jackson-backed {@link JacksonStructuredJsonReader} for the same document, across the full
 * value matrix generated code can emit, plus randomized documents. Malformed documents must be
 * rejected.
 */
public class FastJsonStructuredReaderDifferentialTest {

    /**
     * A hand-written analog of a generated readable builder covering every reader operation,
     * including a recursive union-ish shape.
     */
    static final class TestShape implements StructuredJsonReadable {
        private static final JsonMemberTable TABLE = JsonMemberTable.of(
            "S", "I", "L", "D", "F", "BOOL", "BYTES", "TS_DEFAULT", "TS_ISO", "BIG",
            "LIST_S", "MAP_S", "NESTED", "LIST_NESTED", "MAP_NESTED", "LIST_LIST", "LongerFieldName");

        String s;
        Integer i;
        Long l;
        Double d;
        Float f;
        Boolean bool;
        SdkBytes bytes;
        Instant tsDefault;
        Instant tsIso;
        BigDecimal big;
        List<String> listS;
        Map<String, String> mapS;
        TestShape nested;
        List<TestShape> listNested;
        Map<String, TestShape> mapNested;
        List<List<String>> listList;
        String longerFieldName;

        @Override
        public void readJsonFields(StructuredJsonReader reader) {
            reader.readStruct(this, TABLE, TestShape::readMember);
        }

        private static TestShape readNested(StructuredJsonReader reader) {
            TestShape shape = new TestShape();
            shape.readJsonFields(reader);
            return shape;
        }

        private static void readMember(TestShape b, int memberIndex, StructuredJsonReader reader) {
            switch (memberIndex) {
                case 0:
                    b.s = reader.readString();
                    break;
                case 1:
                    b.i = reader.readInt();
                    break;
                case 2:
                    b.l = reader.readLong();
                    break;
                case 3:
                    b.d = reader.readDouble();
                    break;
                case 4:
                    b.f = reader.readFloat();
                    break;
                case 5:
                    b.bool = reader.readBoolean();
                    break;
                case 6:
                    b.bytes = reader.readSdkBytes();
                    break;
                case 7:
                    b.tsDefault = reader.readInstant(null);
                    break;
                case 8:
                    b.tsIso = reader.readInstant(TimestampFormatTrait.Format.ISO_8601);
                    break;
                case 9:
                    b.big = reader.readBigDecimal();
                    break;
                case 10: {
                    List<String> value = new ArrayList<>();
                    reader.readList(value, (list, r) -> list.add(r.readNullIfPresent() ? null : r.readString()));
                    b.listS = Collections.unmodifiableList(value);
                    break;
                }
                case 11: {
                    Map<String, String> value = new LinkedHashMap<>();
                    reader.readStringMap(value, (map, k, r) -> map.put(k, r.readNullIfPresent() ? null : r.readString()));
                    b.mapS = Collections.unmodifiableMap(value);
                    break;
                }
                case 12:
                    b.nested = readNested(reader);
                    break;
                case 13: {
                    List<TestShape> value = new ArrayList<>();
                    reader.readList(value, (list, r) -> list.add(r.readNullIfPresent() ? null : readNested(r)));
                    b.listNested = Collections.unmodifiableList(value);
                    break;
                }
                case 14: {
                    Map<String, TestShape> value = new LinkedHashMap<>();
                    reader.readStringMap(value,
                                         (map, k, r) -> map.put(k, r.readNullIfPresent() ? null : readNested(r)));
                    b.mapNested = Collections.unmodifiableMap(value);
                    break;
                }
                case 15: {
                    List<List<String>> value = new ArrayList<>();
                    reader.readList(value, (l0, r0) -> {
                        if (r0.readNullIfPresent()) {
                            l0.add(null);
                        } else {
                            List<String> inner = new ArrayList<>();
                            r0.readList(inner, (l1, r1) -> l1.add(r1.readNullIfPresent() ? null : r1.readString()));
                            l0.add(Collections.unmodifiableList(inner));
                        }
                    });
                    b.listList = Collections.unmodifiableList(value);
                    break;
                }
                case 16:
                    b.longerFieldName = reader.readString();
                    break;
                default:
                    throw new IllegalStateException("Unexpected member index: " + memberIndex);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof TestShape)) {
                return false;
            }
            TestShape t = (TestShape) o;
            return Objects.equals(s, t.s) && Objects.equals(i, t.i) && Objects.equals(l, t.l)
                   && Objects.equals(d, t.d) && Objects.equals(f, t.f) && Objects.equals(bool, t.bool)
                   && Objects.equals(bytes, t.bytes) && Objects.equals(tsDefault, t.tsDefault)
                   && Objects.equals(tsIso, t.tsIso) && Objects.equals(big, t.big)
                   && Objects.equals(listS, t.listS) && Objects.equals(mapS, t.mapS)
                   && Objects.equals(nested, t.nested) && Objects.equals(listNested, t.listNested)
                   && Objects.equals(mapNested, t.mapNested) && Objects.equals(listList, t.listList)
                   && Objects.equals(longerFieldName, t.longerFieldName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(s, i, l, d, f, bool, bytes, tsDefault, tsIso, big, listS, mapS,
                                nested, listNested, mapNested, listList, longerFieldName);
        }
    }

    private static TestShape parseFast(String json) {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        TestShape shape = new TestShape();
        boolean nonNull = FastJsonStructuredReader.parseDocument(
            body, 0, body.length, TimestampFormatTrait.Format.UNIX_TIMESTAMP, shape);
        return nonNull ? shape : null;
    }

    private static TestShape parseJackson(String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        try (JsonParser parser = AwsStructuredPlainJsonFactory.SDK_JSON_FACTORY.getJsonFactory().createParser(body)) {
            software.amazon.awssdk.thirdparty.jackson.core.JsonToken first = parser.nextToken();
            if (first == null) {
                return new TestShape();
            }
            if (first == software.amazon.awssdk.thirdparty.jackson.core.JsonToken.VALUE_NULL) {
                return null;
            }
            JacksonStructuredJsonReader reader = new JacksonStructuredJsonReader(
                parser, TimestampFormatTrait.Format.UNIX_TIMESTAMP, first);
            TestShape shape = new TestShape();
            shape.readJsonFields(reader);
            return shape;
        }
    }

    private static void assertSame(String json) throws IOException {
        TestShape jackson = parseJackson(json);
        TestShape fast = parseFast(json);
        assertThat(fast).as("document: %s", json).isEqualTo(jackson);
    }

    @Test
    public void valueMatrix() throws IOException {
        assertSame("{}");
        assertSame("{\"S\":\"hello\"}");
        assertSame("{\"S\":\"\"}");
        assertSame("{\"S\":\"with \\\"escapes\\\" and \\n tab \\t and \\u0041 unicode\"}");
        assertSame("{\"S\":\"caf\u00e9 \u4e2d\u6587 \ud83d\ude00\"}");
        assertSame("{\"I\":42,\"L\":9223372036854775807,\"D\":1.5,\"F\":2.5,\"BOOL\":true}");
        assertSame("{\"I\":-42,\"L\":-9223372036854775808,\"D\":-1.5e10,\"F\":-0.5,\"BOOL\":false}");
        assertSame("{\"I\":\"77\",\"L\":\"123\",\"D\":\"NaN\",\"F\":\"Infinity\"}");
        assertSame("{\"D\":\"-Infinity\",\"BOOL\":\"true\"}");
        assertSame("{\"BIG\":123456789.123456789}");
        assertSame("{\"BIG\":\"42.5\"}");
        assertSame("{\"BYTES\":\"" + Base64.getEncoder().encodeToString("hello world".getBytes(StandardCharsets.UTF_8))
                   + "\"}");
        assertSame("{\"BYTES\":\"\"}");
        assertSame("{\"TS_DEFAULT\":1548118964.201}");
        assertSame("{\"TS_DEFAULT\":1548118964}");
        assertSame("{\"TS_DEFAULT\":\"1548118964.201\"}");
        assertSame("{\"TS_ISO\":\"2019-01-22T01:02:44.201Z\"}");
        assertSame("{\"LIST_S\":[]}");
        assertSame("{\"LIST_S\":[\"a\",\"b\",null,\"d\"]}");
        assertSame("{\"MAP_S\":{}}");
        assertSame("{\"MAP_S\":{\"k1\":\"v1\",\"k2\":null,\"caf\u00e9\":\"v3\"}}");
        assertSame("{\"NESTED\":{\"S\":\"inner\",\"I\":1}}");
        assertSame("{\"NESTED\":{\"NESTED\":{\"NESTED\":{\"S\":\"deep\"}}}}");
        assertSame("{\"LIST_NESTED\":[{\"S\":\"a\"},null,{\"I\":2}]}");
        assertSame("{\"MAP_NESTED\":{\"x\":{\"S\":\"a\"},\"y\":null}}");
        assertSame("{\"LIST_LIST\":[[\"a\"],[],null,[\"b\",null]]}");
        assertSame("{\"LongerFieldName\":\"beyond the packed-name limit\"}");
        // Null members are skipped identically.
        assertSame("{\"S\":null,\"I\":null,\"LIST_S\":null,\"MAP_S\":null,\"NESTED\":null}");
        // Unknown keys of every value type are skipped.
        assertSame("{\"unknown1\":\"x\",\"unknown2\":123,\"unknown3\":{\"a\":[1,2,{\"b\":null}]},"
                   + "\"unknown4\":[\"y\",{\"z\":true}],\"S\":\"kept\"}");
        // Whitespace tolerance.
        assertSame("  {\n\t\"S\" : \"spaced\" ,\r\n \"I\"\t:\t7 }  ");
        // Whitespace at every position the reader skips it, including inside lists, maps and nested
        // structures. Skipping has a one-comparison fast path for the whitespace-free case that AWS
        // responses always take, so the padded form has to be exercised through every container too.
        assertSame("{ \"LIST_S\" : [ \"a\" , \"b\" ] , \"MAP_S\" : { \"k\" : \"v\" , \"k2\" : \"v2\" } ,"
                   + " \"NESTED\" : { \"S\" : \"n\" , \"I\" : 1 } ,"
                   + " \"LIST_NESTED\" : [ { \"S\" : \"a\" } , { \"I\" : 2 } ] ,"
                   + " \"LIST_LIST\" : [ [ \"x\" ] , [ ] ] , \"BOOL\" : true , \"S\" : null }");
        // Every structural position padded with each whitespace byte the reader accepts.
        assertSame("\n{\r\t \"LIST_S\"\n:\r[\t \"a\"\n,\r\t\"b\" ]\n,\r\t\"I\"\n:\r\t42\n}\r\t ");
        // Top-level null and empty documents.
        assertSame("null");
        assertSame("");
    }

    @Test
    public void randomizedDocuments() throws IOException {
        Random random = new Random(42);
        for (int iteration = 0; iteration < 500; iteration++) {
            String json = randomDocument(random, 0);
            assertSame(json);
        }
    }

    @Test
    public void malformedDocumentsAreRejected() {
        String[] malformed = {
            "{", "{\"S\"", "{\"S\":", "{\"S\":\"unterminated", "{\"S\":\"x\",}", "{\"S\" \"x\"}",
            "{\"I\":12.5}", "{\"I\":true}", "{\"BOOL\":12}", "{\"LIST_S\":[\"a\",]}", "{\"LIST_S\":\"notalist\"}",
            "{\"S\":\"bad escape \\q\"}", "{\"S\":\"bad unicode \\u00ZZ\"}", "{\"MAP_S\":{\"k\" \"v\"}}",
            "{\"S\":\"ctrl \u0001 char\"}", "nul", "{\"NESTED\":[}",
        };
        for (String json : malformed) {
            assertThatThrownBy(() -> parseFast(json))
                .as("document: %s", json)
                .isInstanceOf(RuntimeException.class);
        }
    }

    // ------------------------------------------------------------------
    // Random document generation over the TestShape schema
    // ------------------------------------------------------------------

    private String randomDocument(Random r, int depth) {
        StringBuilder sb = new StringBuilder("{");
        int fields = r.nextInt(6);
        for (int i = 0; i < fields; i++) {
            if (i > 0) {
                sb.append(',');
            }
            switch (r.nextInt(depth < 2 ? 10 : 7)) {
                case 0:
                    sb.append("\"S\":").append(randomJsonString(r));
                    break;
                case 1:
                    sb.append("\"I\":").append(r.nextInt());
                    break;
                case 2:
                    sb.append("\"L\":").append(r.nextLong());
                    break;
                case 3:
                    sb.append("\"D\":").append(Double.toString(r.nextDouble() * 1e6));
                    break;
                case 4:
                    sb.append("\"BOOL\":").append(r.nextBoolean());
                    break;
                case 5: {
                    sb.append("\"LIST_S\":[");
                    int n = r.nextInt(4);
                    for (int j = 0; j < n; j++) {
                        sb.append(j > 0 ? "," : "").append(r.nextInt(4) == 0 ? "null" : randomJsonString(r));
                    }
                    sb.append(']');
                    break;
                }
                case 6: {
                    sb.append("\"MAP_S\":{");
                    int n = r.nextInt(4);
                    for (int j = 0; j < n; j++) {
                        sb.append(j > 0 ? "," : "").append("\"key").append(j).append("\":")
                          .append(r.nextInt(4) == 0 ? "null" : randomJsonString(r));
                    }
                    sb.append('}');
                    break;
                }
                case 7:
                    sb.append("\"NESTED\":").append(randomDocument(r, depth + 1));
                    break;
                case 8: {
                    sb.append("\"LIST_NESTED\":[");
                    int n = r.nextInt(3);
                    for (int j = 0; j < n; j++) {
                        sb.append(j > 0 ? "," : "")
                          .append(r.nextInt(4) == 0 ? "null" : randomDocument(r, depth + 1));
                    }
                    sb.append(']');
                    break;
                }
                default: {
                    sb.append("\"MAP_NESTED\":{");
                    int n = r.nextInt(3);
                    for (int j = 0; j < n; j++) {
                        sb.append(j > 0 ? "," : "").append("\"m").append(j).append("\":")
                          .append(r.nextInt(4) == 0 ? "null" : randomDocument(r, depth + 1));
                    }
                    sb.append('}');
                    break;
                }
            }
        }
        return sb.append('}').toString();
    }

    private String randomJsonString(Random r) {
        int len = r.nextInt(16);
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < len; i++) {
            int kind = r.nextInt(12);
            if (kind < 8) {
                char c = (char) (0x20 + r.nextInt(0x5f));
                if (c == '"' || c == '\\') {
                    sb.append('\\');
                }
                sb.append(c);
            } else if (kind == 8) {
                sb.append("\\n");
            } else if (kind == 9) {
                sb.append(String.format("\\u%04x", 0x20 + r.nextInt(0x2000)));
            } else if (kind == 10) {
                sb.append('\u00e9');
            } else {
                sb.append('\u4e2d');
            }
        }
        return sb.append('"').toString();
    }
}
