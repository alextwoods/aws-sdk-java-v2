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

package software.amazon.awssdk.codecs.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.codecs.xml.TestCollections.listOf;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codecs.xml.internal.XmlSchemaExtensions;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;
import software.amazon.awssdk.schema.traits.XmlAttributeTrait;
import software.amazon.awssdk.schema.traits.XmlFlattenedTrait;
import software.amazon.awssdk.schema.traits.XmlNameTrait;
import software.amazon.awssdk.schema.traits.XmlNamespaceTrait;

/**
 * The XML codec's output and input for the cases recorded in the porting-deviations wire-format table (task 7.4), the
 * legacy-disposition rows kept as SDK-owned protocol tests, the codec settings, and the error, null-argument and
 * concurrency contracts of Requirement 8. SDK addition.
 */
public class XmlCodecWireFormatTest extends ProviderTestBase {
    private static final String NS = "software.amazon.awssdk.protocoltests.restxml";

    private static final Schema SIMPLE = Schema.structureBuilder(ShapeId.fromParts(NS, "SimpleStruct"))
                                               .putMember("stringMember", PreludeSchemas.STRING)
                                               .build();
    private static final Schema XML_BODY_REQUEST = Schema.structureBuilder(ShapeId.fromParts(NS, "SdkXmlBodyRequest"))
                                                         .putMember("stringMember", PreludeSchemas.STRING)
                                                         .putMember("simpleStruct", SIMPLE)
                                                         .build();
    private static final Schema TIMESTAMP_HOLDER = Schema.structureBuilder(ShapeId.fromParts(NS, "TimestampHolder"))
                                                         .putMember("timestampMember", PreludeSchemas.TIMESTAMP)
                                                         .build();
    private static final Schema TIMESTAMPS_REQUEST =
        Schema.structureBuilder(ShapeId.fromParts(NS, "SdkTimestampsRequest"))
              .putMember("timestampMember", PreludeSchemas.TIMESTAMP)
              .putMember("nested", TIMESTAMP_HOLDER)
              .build();
    private static final Schema STRING_LIST = Schema.listBuilder(ShapeId.fromParts(NS, "StringList"))
                                                    .putMember("member", PreludeSchemas.STRING)
                                                    .build();
    private static final Schema STRUCT_LIST = Schema.listBuilder(ShapeId.fromParts(NS, "StructList"))
                                                    .putMember("member", SIMPLE)
                                                    .build();
    private static final Schema XML_BODY_RESPONSE = Schema.structureBuilder(ShapeId.fromParts(NS, "SdkXmlBodyResponse"))
                                                          .putMember("stringMember", PreludeSchemas.STRING)
                                                          .putMember("flattenedStringList", STRING_LIST,
                                                                     XmlFlattenedTrait.INSTANCE)
                                                          .build();
    private static final Schema XML_LISTS_OUTPUT = Schema.structureBuilder(ShapeId.fromParts(NS, "SdkXmlListsOutput"))
                                                         .putMember("flattenedStructList", STRUCT_LIST,
                                                                    XmlFlattenedTrait.INSTANCE)
                                                         .putMember("flattenedStringList", STRING_LIST,
                                                                    XmlFlattenedTrait.INSTANCE)
                                                         .build();
    private static final Schema NUMBERS =
        Schema.structureBuilder(ShapeId.fromParts(NS, "Numbers"))
              .putMember("d", PreludeSchemas.DOUBLE)
              .putMember("f", PreludeSchemas.FLOAT)
              .putMember("i", PreludeSchemas.INTEGER)
              .putMember("big", PreludeSchemas.BIG_DECIMAL)
              .putMember("blob", PreludeSchemas.BLOB)
              .putMember("epoch", PreludeSchemas.TIMESTAMP, new TimestampFormatTrait(TimestampFormatTrait.EPOCH_SECONDS))
              .putMember("http", PreludeSchemas.TIMESTAMP, new TimestampFormatTrait(TimestampFormatTrait.HTTP_DATE))
              .putMember("plain", PreludeSchemas.TIMESTAMP)
              .build();
    private static final Schema RENAMED =
        Schema.structureBuilder(ShapeId.fromParts(NS, "Renamed"), new XmlNameTrait("Root"))
              .putMember("attr", PreludeSchemas.STRING, XmlAttributeTrait.INSTANCE, new XmlNameTrait("a"))
              .putMember("value", PreludeSchemas.STRING, new XmlNameTrait("Value"))
              .putMember("list", Schema.listBuilder(ShapeId.fromParts(NS, "RenamedList"))
                                       .putMember("member", PreludeSchemas.STRING, new XmlNameTrait("item"))
                                       .build())
              .build();
    private static final Schema NAMESPACED =
        Schema.structureBuilder(ShapeId.fromParts(NS, "Namespaced"))
              .putMember("plain", PreludeSchemas.STRING)
              .putMember("prefixed", PreludeSchemas.STRING, new XmlNamespaceTrait("urn:member", "m"))
              .build();
    private static final Schema UNION = Schema.unionBuilder(ShapeId.fromParts(NS, "Choice"))
                                              .putMember("a", PreludeSchemas.STRING)
                                              .putMember("b", PreludeSchemas.INTEGER)
                                              .build();

    private static String write(boolean useNative, SchemaStruct value) {
        try (XmlCodec codec = codec(useNative)) {
            return codec.serializeToString(value);
        }
    }

    private static SchemaStruct read(XmlCodec codec, String xml, Schema schema) {
        return codec.deserializeShape(xml, SchemaStruct.builder(schema));
    }

    private static SchemaStruct read(boolean useNative, String xml, Schema schema) {
        try (XmlCodec codec = codec(useNative)) {
            return read(codec, xml, schema);
        }
    }

    private static SchemaStruct roundtrip(boolean useNative, SchemaStruct value) {
        return read(useNative, write(useNative, value), value.sdkSchema());
    }

    // --- legacy-disposition rows kept as SDK-owned protocol tests ---

    // rest-xml-input.json#3, SdkRestXmlEscapedCharacters. The legacy body escapes quotes, apostrophes, CR and LF as
    // entities; the codec writes quotes, apostrophes and LF as themselves (equal under the XMLUnit comparison) and CR as
    // a character reference, which a parser would otherwise normalize to LF.
    @PerProvider
    void escapedCharacters_writtenAsEquivalentXmlAndReadBack(boolean useNative) {
        String value = "\"'<\r\nNormalCharacters\"'>\r\n";
        SchemaStruct request = SchemaStruct.builder(XML_BODY_REQUEST).set("stringMember", value).build();
        String legacyBody = "<SdkXmlBodyRequest><stringMember>&quot;&apos;&lt;&#x0D;&#x0A;NormalCharacters&quot;&apos;"
                            + "&gt;&#x0D;&#x0A;</stringMember></SdkXmlBodyRequest>";

        assertThat(roundtrip(useNative, request)).isEqualTo(request);
        assertThat(read(useNative, legacyBody, XML_BODY_REQUEST)).isEqualTo(request);
    }

    @Test
    void escapedCharacters_nativeOutput() {
        SchemaStruct request = SchemaStruct.builder(XML_BODY_REQUEST).set("stringMember", "\"'<&>\r\n\t").build();

        assertThat(write(NATIVE, request))
            .isEqualTo("<SdkXmlBodyRequest><stringMember>\"'&lt;&amp;&gt;&#xD;\n\t</stringMember></SdkXmlBodyRequest>");
    }

    // rest-xml-input.json#7 and #18, SdkRestXmlEmptyStructure and SdkRestXmlNoParamsBody.
    @PerProvider
    void emptyStructures_writtenAsStartAndEndTags(boolean useNative) {
        SchemaStruct emptyMember = SchemaStruct.builder(XML_BODY_REQUEST)
                                               .set("simpleStruct", SchemaStruct.builder(SIMPLE).build())
                                               .build();

        assertThat(write(useNative, emptyMember))
            .isEqualTo("<SdkXmlBodyRequest><simpleStruct></simpleStruct></SdkXmlBodyRequest>");
        assertThat(write(useNative, SchemaStruct.builder(XML_BODY_REQUEST).build()))
            .isEqualTo("<SdkXmlBodyRequest></SdkXmlBodyRequest>");
        assertThat(roundtrip(useNative, emptyMember)).isEqualTo(emptyMember);
    }

    // rest-xml-input.json#16 and #17, SdkRestXmlTimestampMillisecondPrecision.
    @PerProvider
    void dateTimeTimestamps_keepMillisecondPrecision(boolean useNative) {
        Instant time = Instant.parse("2015-01-25T08:00:00.123Z");
        SchemaStruct request = SchemaStruct.builder(TIMESTAMPS_REQUEST)
                                           .set("timestampMember", time)
                                           .set("nested", SchemaStruct.builder(TIMESTAMP_HOLDER)
                                                                      .set("timestampMember", time).build())
                                           .build();

        assertThat(write(useNative, request))
            .isEqualTo("<SdkTimestampsRequest><timestampMember>2015-01-25T08:00:00.123Z</timestampMember><nested>"
                       + "<timestampMember>2015-01-25T08:00:00.123Z</timestampMember></nested></SdkTimestampsRequest>");
        assertThat(roundtrip(useNative, request)).isEqualTo(request);
    }

    // rest-xml-output.json#8 (SdkRestXmlSingleElementFlattenedList) and ec2-output.json#7
    // (SdkEc2SingleElementFlattenedList).
    @PerProvider
    void singleElementFlattenedList_readsAsOneElementList(boolean useNative) {
        String restXml = "<SdkXmlBodyResponse>\n    <flattenedStringList>val1</flattenedStringList>\n</SdkXmlBodyResponse>\n";
        String ec2 = "<SdkXmlListsResponse xmlns=\"https://example.com/\">\n"
                     + "    <flattenedStringList>val1</flattenedStringList>\n"
                     + "    <requestId>requestid</requestId>\n"
                     + "</SdkXmlListsResponse>\n";

        assertThat(read(useNative, restXml, XML_BODY_RESPONSE).get("flattenedStringList")).isEqualTo(listOf("val1"));
        try (XmlCodec lenient = XmlCodec.builder().useNative(useNative).strictRootElement(false).build()) {
            assertThat(read(lenient, ec2, XML_LISTS_OUTPUT).get("flattenedStringList")).isEqualTo(listOf("val1"));
        }
    }

    // ec2-output.json#8, SdkEc2FlattenedListOfStructures, with the items interleaved with another member.
    @PerProvider
    void flattenedListOfStructures_interleavedWithOtherMembers(boolean useNative) {
        String xml = "<SdkXmlListsOutput>"
                     + "<flattenedStructList><stringMember>foo</stringMember></flattenedStructList>"
                     + "<flattenedStringList>s1</flattenedStringList>"
                     + "<flattenedStructList><stringMember>bar</stringMember></flattenedStructList>"
                     + "</SdkXmlListsOutput>";

        SchemaStruct result = read(useNative, xml, XML_LISTS_OUTPUT);

        assertThat(result.get("flattenedStructList"))
            .isEqualTo(listOf(SchemaStruct.builder(SIMPLE).set("stringMember", "foo").build(),
                              SchemaStruct.builder(SIMPLE).set("stringMember", "bar").build()));
        assertThat(result.get("flattenedStringList")).isEqualTo(listOf("s1"));
    }

    // --- wire format compared with the legacy SDK ---

    @Test
    void numbersAndTimestamps_nativeSpellings() {
        SchemaStruct value = SchemaStruct.builder(NUMBERS)
                                         .set("d", 1.0)
                                         .set("f", 1.0e10f)
                                         .set("i", -7)
                                         .set("big", new BigDecimal("1E+3"))
                                         .set("blob", ByteBuffer.wrap(new byte[] {1}))
                                         .set("epoch", Instant.parse("2015-01-25T08:00:00.100Z"))
                                         .set("http", Instant.parse("2014-04-29T18:30:38Z"))
                                         .set("plain", Instant.parse("2015-01-25T08:00:00Z"))
                                         .build();

        assertThat(write(NATIVE, value))
            .isEqualTo("<Numbers><d>1</d><f>1.0E10</f><i>-7</i><big>1E+3</big><blob>AQ==</blob>"
                       + "<epoch>1422172800.100</epoch><http>Tue, 29 Apr 2014 18:30:38 GMT</http>"
                       + "<plain>2015-01-25T08:00:00Z</plain></Numbers>");
        assertThat(write(STAX, value)).contains("<d>1.0</d>");
        assertThat(roundtrip(NATIVE, value)).isEqualTo(value);
        assertThat(roundtrip(STAX, value)).isEqualTo(value);
    }

    @Test
    void doubles_nativeWritesShortestFormAndIntegersWithoutFraction() {
        double[] values = {1e10, 1.5e-7, -0.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        String[] expected = {"10000000000", "1.5E-7", "0", "NaN", "Infinity", "-Infinity"};
        for (int i = 0; i < values.length; i++) {
            assertThat(write(NATIVE, SchemaStruct.builder(NUMBERS).set("d", values[i]).build()))
                .isEqualTo("<Numbers><d>" + expected[i] + "</d></Numbers>");
        }
    }

    @Test
    void epochSecondsTimestamps_wholeSecondsHaveNoFraction() {
        SchemaStruct value = SchemaStruct.builder(NUMBERS).set("epoch", Instant.parse("2015-01-25T08:00:00Z")).build();
        assertThat(write(NATIVE, value)).isEqualTo("<Numbers><epoch>1422172800</epoch></Numbers>");
    }

    @PerProvider
    void serialize_writesNoXmlDeclaration(boolean useNative) {
        assertThat(write(useNative, SchemaStruct.builder(SIMPLE).set("stringMember", "x").build()))
            .startsWith("<SimpleStruct>");
    }

    @PerProvider
    void defaultNamespace_declaredOnRootOnly(boolean useNative) {
        SchemaStruct value = SchemaStruct.builder(XML_BODY_REQUEST)
                                         .set("simpleStruct", SchemaStruct.builder(SIMPLE).set("stringMember", "x")
                                                                          .build())
                                         .build();
        try (XmlCodec codec = XmlCodec.builder().useNative(useNative)
                                      .defaultNamespace(new XmlNamespaceTrait("https://example.com/")).build()) {
            String xml = codec.serializeToString(value);

            assertThat(xml).isEqualTo("<SdkXmlBodyRequest xmlns=\"https://example.com/\"><simpleStruct><stringMember>x"
                                      + "</stringMember></simpleStruct></SdkXmlBodyRequest>");
            assertThat(read(codec, xml, XML_BODY_REQUEST)).isEqualTo(value);
        }
    }

    @PerProvider
    void namespaces_prefixedDefaultAndMemberNamespaces(boolean useNative) {
        SchemaStruct value = SchemaStruct.builder(NAMESPACED).set("plain", "p").set("prefixed", "q").build();
        try (XmlCodec codec = XmlCodec.builder().useNative(useNative)
                                      .defaultNamespace(new XmlNamespaceTrait("urn:root", "r")).build()) {
            String xml = codec.serializeToString(value);

            assertThat(xml).isEqualTo("<Namespaced xmlns:r=\"urn:root\"><plain>p</plain>"
                                      + "<prefixed xmlns:m=\"urn:member\">q</prefixed></Namespaced>");
            assertThat(read(codec, xml, NAMESPACED)).isEqualTo(value);
        }
    }

    @PerProvider
    void attributes_escapedAndRenamed(boolean useNative) {
        SchemaStruct value = SchemaStruct.builder(RENAMED)
                                         .set("attr", "a\"b'c<d&e")
                                         .set("value", "v")
                                         .set("list", listOf("x", "y"))
                                         .build();

        String xml = write(useNative, value);

        assertThat(xml).startsWith("<Root a=\"a&quot;b").contains("<Value>v</Value><list><item>x</item><item>y</item></list>");
        assertThat(read(useNative, xml, RENAMED)).isEqualTo(value);
    }

    @PerProvider
    void useXmlNameFalse_usesMemberAndShapeNames(boolean useNative) {
        SchemaStruct value = SchemaStruct.builder(RENAMED).set("attr", "1").set("value", "v").set("list", listOf("x"))
                                         .build();
        try (XmlCodec codec = XmlCodec.builder().useNative(useNative).useXmlName(false).build()) {
            String xml = codec.serializeToString(value);

            assertThat(xml).isEqualTo("<Renamed attr=\"1\"><value>v</value><list><member>x</member></list></Renamed>");
            assertThat(read(codec, xml, RENAMED)).isEqualTo(value);
        }
    }

    @PerProvider
    void useTimestampFormatFalse_usesTheDefaultFormat(boolean useNative) {
        Instant time = Instant.parse("2015-01-25T08:00:00Z");
        SchemaStruct value = SchemaStruct.builder(NUMBERS).set("http", time).build();
        try (XmlCodec codec = XmlCodec.builder().useNative(useNative).useTimestampFormat(false)
                                      .defaultTimestampFormat(TimestampFormatTrait.Format.EPOCH_SECONDS).build()) {
            String xml = codec.serializeToString(value);

            assertThat(xml).isEqualTo("<Numbers><http>1422172800</http></Numbers>");
            assertThat(read(codec, xml, NUMBERS)).isEqualTo(value);
        }
    }

    // --- read behavior ---

    @PerProvider
    void unknownMembers_skippedAndReported(boolean useNative) {
        String xml = "<SimpleStruct><other><deep>1</deep></other><stringMember>x</stringMember></SimpleStruct>";
        try (XmlCodec codec = codec(useNative)) {
            SchemaStruct.Builder builder = SchemaStruct.builder(SIMPLE);
            codec.deserializeShape(xml, builder);

            assertThat(builder.build().get("stringMember")).isEqualTo("x");
            assertThat(builder.unknownMembers()).containsExactly("other");
        }
    }

    @PerProvider
    void unknownUnionMember_rejectedOnlyWhenForbidden(boolean useNative) {
        String xml = "<Choice><c>1</c></Choice>";
        try (XmlCodec lenient = codec(useNative);
             XmlCodec strict = XmlCodec.builder().useNative(useNative).forbidUnknownUnionMembers(true).build()) {
            SchemaStruct.Builder builder = SchemaStruct.builder(UNION);
            lenient.deserializeShape(xml, builder);
            assertThat(builder.unknownMembers()).containsExactly("c");

            assertThatThrownBy(() -> read(strict, xml, UNION))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Unknown member c encountered for union " + NS + "#Choice");
            assertThat(read(strict, "<Choice><b>2</b></Choice>", UNION).get("b")).isEqualTo(2);
        }
    }

    @PerProvider
    void malformedValue_messageNamesTheShapeId(boolean useNative) {
        assertThatThrownBy(() -> read(useNative, "<Numbers><i>abc</i></Numbers>", NUMBERS))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining(NS + "#Numbers$i");
        assertThatThrownBy(() -> read(useNative, "<Numbers><blob>!!!</blob></Numbers>", NUMBERS))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining(NS + "#Numbers$blob");
        assertThatThrownBy(() -> read(useNative, "<Numbers><plain>yesterday</plain></Numbers>", NUMBERS))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining(NS + "#Numbers$plain");
    }

    @PerProvider
    void wrapperElements_skipQueryResponseAndResult(boolean useNative) {
        String xml = "<OpResponse xmlns=\"https://example.com/\"><OpResult><stringMember>x</stringMember></OpResult>"
                     + "<ResponseMetadata><RequestId>r</RequestId></ResponseMetadata></OpResponse>";
        try (XmlCodec codec = XmlCodec.builder().useNative(useNative).wrapperElements(listOf("OpResponse", "OpResult"))
                                      .strictRootElement(false).build()) {
            assertThat(read(codec, xml, SIMPLE).get("stringMember")).isEqualTo("x");
            assertThatThrownBy(() -> read(codec, "<Other><OpResult/></Other>", SIMPLE))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Expected wrapper element 'OpResponse', found 'Other'");
        }
    }

    @PerProvider
    void parseErrorCodeName_emptyBodyOrNonError_throws(boolean useNative) {
        try (XmlCodec codec = codec(useNative)) {
            assertThatThrownBy(() -> XmlUtil.parseErrorCodeName(codec.createDeserializer(new byte[0])))
                .isInstanceOf(SerializationException.class);
            ShapeDeserializer notAnError = codec.createDeserializer("<Foo/>".getBytes(StandardCharsets.UTF_8));
            assertThatThrownBy(() -> XmlUtil.parseErrorCodeName(notAnError)).isInstanceOf(SerializationException.class);
        }
    }

    @PerProvider
    void createDeserializer_doesNotMoveTheSourcePosition(boolean useNative) {
        byte[] bytes = "xx<SimpleStruct><stringMember>s</stringMember></SimpleStruct>".getBytes(StandardCharsets.UTF_8);
        ByteBuffer source = ByteBuffer.wrap(bytes);
        source.position(2);
        try (XmlCodec codec = codec(useNative)) {
            assertThat(codec.deserializeShape(source, SchemaStruct.builder(SIMPLE)).get("stringMember")).isEqualTo("s");
            assertThat(source.position()).isEqualTo(2);
        }
    }

    // --- codec contracts ---

    @PerProvider
    void serialize_isByteIdenticalToTheSerializerPathAndDoesNotCloseTheSink(boolean useNative) {
        SchemaStruct value = SchemaStruct.builder(NUMBERS).set("d", 2.5).set("i", 3).build();
        try (XmlCodec codec = codec(useNative)) {
            ByteBuffer viaSerialize = codec.serialize(value);
            CloseTrackingStream sink = new CloseTrackingStream();
            try (ShapeSerializer serializer = codec.createSerializer(sink)) {
                value.serialize(serializer);
                serializer.flush();
                assertThat(sink.size()).isPositive();
            }

            assertThat(ByteBuffer.wrap(sink.toByteArray())).isEqualTo(viaSerialize);
            assertThat(sink.closed).isFalse();
        }
    }

    @Test
    void createSerializer_unusedSerializerCanBeClosedTwice() {
        try (XmlCodec codec = codec(NATIVE)) {
            ShapeSerializer serializer = codec.createSerializer(new ByteArrayOutputStream());
            serializer.close();
            serializer.close();
        }
    }

    @Test
    void nullArguments_throwNamedNullPointerException() {
        try (XmlCodec codec = codec(NATIVE)) {
            assertThatThrownBy(() -> codec.serialize(null)).hasMessage("shape must not be null");
            assertThatThrownBy(() -> codec.deserializeShape((byte[]) null, SchemaStruct.builder(SIMPLE)))
                .hasMessage("source must not be null");
            assertThatThrownBy(() -> codec.deserializeShape(ByteBuffer.allocate(0), null))
                .hasMessage("builder must not be null");
        }
    }

    @Test
    void schemaExtension_isRegistered() {
        assertThat(XML_BODY_REQUEST.getExtension(XmlSchemaExtensions.KEY))
            .isInstanceOf(XmlSchemaExtensions.StructExtension.class);
        assertThat(STRING_LIST.getExtension(XmlSchemaExtensions.KEY)).isInstanceOf(XmlSchemaExtensions.ListExtension.class);
        assertThat(PreludeSchemas.STRING.getExtension(XmlSchemaExtensions.KEY)).isNull();
    }

    @PerProvider
    void oneCodec_usedByEightThreads_givesTheSingleThreadedResults(boolean useNative) throws Exception {
        try (XmlCodec codec = codec(useNative)) {
            List<SchemaStruct> values = new ArrayList<>();
            List<String> expected = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                SchemaStruct value = SchemaStruct.builder(XML_BODY_RESPONSE)
                                                 .set("stringMember", "thread-" + i)
                                                 .set("flattenedStringList", listOf("a" + i, "b" + i))
                                                 .build();
                values.add(value);
                expected.add(codec.serializeToString(value));
            }

            ExecutorService executor = Executors.newFixedThreadPool(8);
            try {
                List<Future<Boolean>> results = new ArrayList<>();
                for (int t = 0; t < 8; t++) {
                    int index = t;
                    results.add(executor.submit(() -> {
                        for (int n = 0; n < 200; n++) {
                            String xml = codec.serializeToString(values.get(index));
                            if (!xml.equals(expected.get(index))
                                || !read(codec, xml, XML_BODY_RESPONSE).equals(values.get(index))) {
                                return false;
                            }
                        }
                        return true;
                    }));
                }
                for (Future<Boolean> result : results) {
                    assertThat(result.get(60, TimeUnit.SECONDS)).isTrue();
                }
            } finally {
                executor.shutdownNow();
            }
        }
    }

    private static final class CloseTrackingStream extends ByteArrayOutputStream {
        private boolean closed;

        @Override
        public void close() {
            closed = true;
        }
    }
}
