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

package software.amazon.awssdk.protocols.core.xml;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.protocols.core.binding.HttpOperationBindings;
import software.amazon.awssdk.protocols.core.serde.HttpBindingWriter;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.SerializationException;
import software.amazon.smithy.java.dynamicschemas.StructDocument;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;

/** The shared XML writer: names, namespaces, attributes, collections, scalars, escaping, and round trips. */
class XmlWriterTest {
    private static final Model MODEL = XmlTestValues.model("""
        $version: "2"
        namespace example.xml

        @xmlName("Root")
        @xmlNamespace(uri: "urn:xsi", prefix: "xsi")
        structure Doc {
            @xmlAttribute
            @xmlName("xsi:type")
            kind: String
            @xmlAttribute
            plain: String
            @xmlName("Renamed")
            text: String
            @xmlNamespace(uri: "urn:m", prefix: "m")
            spaced: String
            nested: Nested
            items: Items
            @xmlFlattened
            @xmlName("Flat")
            flat: Items
            pairs: Pairs
            @xmlFlattened
            @xmlName("Entry")
            flatPairs: Pairs
            sparse: SparseMap
            grid: Grid
            absent: Integer
        }

        @xmlNamespace(uri: "urn:nested")
        structure Nested {
            @xmlAttribute
            id: Integer
            @xmlName("Item")
            value: String
        }

        list Items {
            @xmlName("Item")
            @xmlNamespace(uri: "urn:item")
            member: String
        }

        map Pairs {
            @xmlName("K")
            key: String
            @xmlName("V")
            value: Nested
        }

        @sparse
        map SparseMap {
            key: String
            value: String
        }

        list Grid {
            member: Strings
        }

        list Strings {
            member: String
        }

        structure Scalars {
            @xmlAttribute
            attr: String
            dateTime: Timestamp
            @timestampFormat("epoch-seconds")
            epoch: Timestamp
            @timestampFormat("http-date")
            httpDate: Timestamp
            blob: Blob
            nan: Float
            inf: Float
            negInf: Double
            ratio: Float
            tenth: Double
            flag: Boolean
            big: BigDecimal
            count: Long
            text: String
        }

        structure PutInput {
            @httpHeader("X-Token")
            token: String
            @xmlName("Cfg")
            config: Config
            plain: Plain
            name: String
        }

        @xmlName("Configuration")
        @xmlNamespace(uri: "urn:config")
        structure Config {
            a: String
        }

        structure Plain {
            a: String
        }
        """);
    private static final XmlNamespaceTrait SERVICE = XmlNamespaceTrait.builder().uri("urn:svc").build();
    private static final Instant INSTANT = Instant.parse("2019-12-16T23:48:18Z");

    private static Schema schema(String name) {
        return XmlTestValues.schema(MODEL, "example.xml#" + name);
    }

    private static String xml(SerializableStruct value, XmlNamespaceTrait namespace) {
        return new String(XmlShapeSerializer.serialize(value, namespace), StandardCharsets.UTF_8);
    }

    private static Map<String, Object> members(Object... pairs) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            out.put((String) pairs[i], pairs[i + 1]);
        }
        return out;
    }

    private static StructDocument doc() {
        Map<String, Object> sparse = new HashMap<>();
        sparse.put("z", null);
        return XmlTestValues.value(schema("Doc"), members(
            "kind", "T", "plain", "P", "text", "t", "spaced", "s",
            "nested", members("id", 7, "value", "n"),
            "items", List.of("a", "b"), "flat", List.of("x", "y"),
            "pairs", Map.of("k1", members("value", "v")),
            "flatPairs", Map.of("k2", members("id", 2, "value", "w")),
            "sparse", sparse,
            "grid", List.of(List.of("1", "2"), List.of("3"))));
    }

    /** Parses {@code xml} back into a value of {@code structure}, by the shared reader. */
    private static StructDocument read(Schema structure, byte[] xml) {
        ShapeBuilder<StructDocument> builder = software.amazon.smithy.java.dynamicschemas.SchemaConverter
            .createDocumentBuilder(structure);
        builder.deserialize(XmlShapeDeserializer.of(XmlTreeReader.parse(xml)));
        return builder.build();
    }

    @Test
    void writesNamesNamespacesAttributesAndCollections() {
        assertEquals("<Root xmlns:xsi=\"urn:xsi\" xsi:type=\"T\" plain=\"P\">"
                     + "<Renamed>t</Renamed>"
                     + "<spaced xmlns:m=\"urn:m\">s</spaced>"
                     + "<nested xmlns=\"urn:nested\" id=\"7\"><Item>n</Item></nested>"
                     + "<items><Item xmlns=\"urn:item\">a</Item><Item xmlns=\"urn:item\">b</Item></items>"
                     + "<Flat>x</Flat><Flat>y</Flat>"
                     + "<pairs><entry><K>k1</K><V xmlns=\"urn:nested\"><Item>v</Item></V></entry></pairs>"
                     + "<Entry><K>k2</K><V xmlns=\"urn:nested\" id=\"2\"><Item>w</Item></V></Entry>"
                     + "<sparse><entry><key>z</key></entry></sparse>"
                     + "<grid><member><member>1</member><member>2</member></member>"
                     + "<member><member>3</member></member></grid>"
                     + "</Root>",
                     xml(doc(), SERVICE));
    }

    @Test
    void writesScalarsAndEscapesTextAndAttributes() {
        StructDocument value = XmlTestValues.value(schema("Scalars"), members(
            "attr", "q\"<&>\t\n\r", "dateTime", INSTANT, "epoch", INSTANT, "httpDate", INSTANT,
            "blob", "hello".getBytes(StandardCharsets.UTF_8), "nan", Float.NaN, "inf", Float.POSITIVE_INFINITY,
            "negInf", Double.NEGATIVE_INFINITY, "ratio", 1.1f, "tenth", 0.1, "flag", true,
            "big", new BigDecimal("1E+3"), "count", 5L, "text", "a\r\nb<&>\"'\u00e9\ud834\udd1e]]>"));
        assertEquals("<Scalars attr=\"q&quot;&lt;&amp;&gt;&#x9;&#xA;&#xD;\">"
                     + "<dateTime>2019-12-16T23:48:18Z</dateTime>"
                     + "<epoch>1576540098</epoch>"
                     + "<httpDate>Mon, 16 Dec 2019 23:48:18 GMT</httpDate>"
                     + "<blob>aGVsbG8=</blob>"
                     + "<nan>NaN</nan><inf>Infinity</inf><negInf>-Infinity</negInf>"
                     + "<ratio>1.1</ratio><tenth>0.1</tenth><flag>true</flag><big>1000</big><count>5</count>"
                     + "<text>a&#xD;\nb&lt;&amp;&gt;\"'\u00e9\ud834\udd1e]]&gt;</text>"
                     + "</Scalars>",
                     xml(value, null));
        // The escapes survive parsing exactly, including the carriage return and the attribute's whitespace.
        StructDocument read = read(schema("Scalars"), XmlShapeSerializer.serialize(value, null));
        assertEquals("q\"<&>\t\n\r", read.getMember("attr").asString());
        assertEquals("a\r\nb<&>\"'\u00e9\ud834\udd1e]]>", read.getMember("text").asString());
        assertTrue(Float.isNaN(read.getMember("nan").asFloat()));
        assertEquals(Float.POSITIVE_INFINITY, read.getMember("inf").asFloat());
        assertEquals(Double.NEGATIVE_INFINITY, read.getMember("negInf").asDouble());
        // 1E+3 is written in plain notation, so it reads back equal in value but not in scale.
        assertEquals(0, new BigDecimal("1E+3").compareTo(read.getMember("big").asBigDecimal()));
        // NaN is not equal to itself; the remaining members must read back exactly.
        assertEquals(Node.printJson(rest(XmlTestValues.canonical(value))),
                     Node.printJson(rest(XmlTestValues.canonical(read))));
    }

    private static Node rest(Node value) {
        return value.expectObjectNode().withoutMember("nan").withoutMember("inf").withoutMember("negInf")
                    .withoutMember("big");
    }

    @Test
    void namesTheRootByPayloadMemberThenShapeAndAppliesTheDefaultNamespace() {
        Schema input = schema("PutInput");
        StructDocument config = XmlTestValues.value(schema("Config"), members("a", "1"));
        StructDocument plain = XmlTestValues.value(schema("Plain"), members("a", "2"));
        assertEquals("<Cfg xmlns=\"urn:config\"><a>1</a></Cfg>", new String(
            XmlShapeSerializer.serialize(input.member("config"), config, SERVICE), StandardCharsets.UTF_8));
        assertEquals("<Configuration xmlns=\"urn:config\"><a>1</a></Configuration>", xml(config, SERVICE));
        assertEquals("<Plain xmlns=\"urn:svc\"><a>2</a></Plain>", new String(
            XmlShapeSerializer.serialize(input.member("plain"), plain, SERVICE), StandardCharsets.UTF_8));
        assertEquals("<Plain><a>2</a></Plain>", xml(plain, null));
        assertEquals("Cfg", XmlShapeSerializer.rootName(input.member("config")));
        assertEquals("Plain", XmlShapeSerializer.rootName(input.member("plain")));
        assertEquals("PutInput", XmlShapeSerializer.rootName(input));
    }

    @Test
    void writesBodyMembersUnderTheInputRootAndPayloadsThroughTheFactory() {
        Schema input = schema("PutInput");
        Schema operation = Schema.createOperation(
            ShapeId.from("example.xml#Put"),
            HttpTrait.builder().method("PUT").uri(UriPattern.parse("/")).code(200).build());
        HttpBindingWriter writer = HttpBindingWriter.of(HttpOperationBindings.of(
            OperationSchemas.builder(operation).input(input).output(schema("Plain")).build()));
        StructDocument value = XmlTestValues.value(input, members(
            "token", "secret", "config", members("a", "1"), "plain", members("a", "2"), "name", "n"));
        assertEquals("<PutInput xmlns=\"urn:svc\"><Cfg xmlns=\"urn:config\"><a>1</a></Cfg>"
                     + "<plain><a>2</a></plain><name>n</name></PutInput>",
                     xml(writer.bodyMembers(value), SERVICE));

        // A payload member view writes writeStruct(member, value) into a factory-created serializer.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XmlShapeSerializer serializer = XmlShapeSerializer.create(out, SERVICE)) {
            serializer.writeStruct(input.member("config"),
                                   XmlTestValues.value(schema("Config"), members("a", "1")));
        }
        assertEquals("<Cfg xmlns=\"urn:config\"><a>1</a></Cfg>", out.toString(StandardCharsets.UTF_8));
    }

    @Test
    void omitsAbsentMembersAndRejectsUnrepresentableValues() {
        assertEquals("<Root xmlns:xsi=\"urn:xsi\"></Root>",
                     xml(XmlTestValues.value(schema("Doc"), Map.of()), null));
        assertThrows(SerializationException.class, () -> xml(
            XmlTestValues.value(schema("Plain"), members("a", "bell\u0007")), null));
        assertThrows(SerializationException.class, () -> xml(
            XmlTestValues.value(schema("Plain"), members("a", "half\ud834")), null));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XmlShapeSerializer serializer = XmlShapeSerializer.create(out, null);
        assertThrows(SerializationException.class, () -> serializer.writeString(schema("Plain").member("a"), "x"));
        serializer.writeNull(schema("Plain"));
        serializer.close();
        assertEquals(0, out.size());
    }

    @Test
    void roundTripsThroughTheSharedReader() {
        StructDocument value = doc();
        byte[] xml = XmlShapeSerializer.serialize(value, SERVICE);
        assertEquals(XmlTestValues.canonical(value), XmlTestValues.canonical(read(schema("Doc"), xml)));

        // Arbitrary text, including markup, whitespace and non-ASCII characters, in elements and attributes.
        Random random = new Random(42);
        String alphabet = "ab <>&\"'\r\n\t]]\u00e9\u4e2d\ud834\udd1e";
        for (int i = 0; i < 200; i++) {
            StringBuilder text = new StringBuilder();
            int length = random.nextInt(12);
            for (int j = 0; j < length; j++) {
                int k = random.nextInt(alphabet.length() - 1);
                if (Character.isHighSurrogate(alphabet.charAt(k))) {
                    text.append(alphabet, k, k + 2);
                } else if (!Character.isLowSurrogate(alphabet.charAt(k))) {
                    text.append(alphabet.charAt(k));
                }
            }
            StructDocument sample = XmlTestValues.value(schema("Doc"), members(
                "plain", text.toString(), "text", text.toString(), "items", List.of(text.toString())));
            StructDocument read = read(schema("Doc"), XmlShapeSerializer.serialize(sample, null));
            assertEquals(XmlTestValues.canonical(sample), XmlTestValues.canonical(read), "sample " + i);
        }
        assertArrayEquals(xml, XmlShapeSerializer.serialize(value, SERVICE));
    }
}
