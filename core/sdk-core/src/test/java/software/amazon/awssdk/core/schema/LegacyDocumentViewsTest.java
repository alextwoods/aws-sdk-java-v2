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

package software.amazon.awssdk.core.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.core.document.DocumentVisitor;
import software.amazon.awssdk.core.document.VoidDocumentVisitor;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.SerializationException;

/**
 * {@link SchemaSerdeSupport#wrapLegacy} and {@link SchemaSerdeSupport#unwrapLegacy}: lazy views between the
 * Legacy_Document and the schema document.
 */
class LegacyDocumentViewsTest {

    @Test
    void wrapLegacy_nullOrNullDocument_returnsNull() {
        assertThat(SchemaSerdeSupport.wrapLegacy(null)).isNull();
        assertThat(SchemaSerdeSupport.wrapLegacy(Document.fromNull())).isNull();
    }

    @Test
    void unwrapLegacy_null_returnsNullDocument() {
        assertThat(SchemaSerdeSupport.unwrapLegacy(null)).isEqualTo(Document.fromNull());
    }

    @Test
    void wrapLegacy_scalars_haveTheirSchemaTypesAndValues() {
        assertThat(SchemaSerdeSupport.wrapLegacy(Document.fromString("s")))
            .isEqualTo(schemaOf("s"));
        assertThat(SchemaSerdeSupport.wrapLegacy(Document.fromBoolean(true)))
            .isEqualTo(software.amazon.awssdk.schema.serde.document.Document.of(true));
    }

    static Stream<Arguments> numbers() {
        return Stream.of(
            Arguments.of(Document.fromNumber(0), ShapeType.INTEGER, 0),
            Arguments.of(Document.fromNumber(2147483648L), ShapeType.LONG, 2147483648L),
            Arguments.of(Document.fromNumber(new BigInteger("9223372036854775808")), ShapeType.BIG_INTEGER,
                         new BigInteger("9223372036854775808")),
            Arguments.of(Document.fromNumber(1.5), ShapeType.DOUBLE, 1.5),
            Arguments.of(Document.fromNumber(0.1f), ShapeType.DOUBLE, 0.1),
            Arguments.of(Document.fromNumber("1.00000000000000000001"), ShapeType.BIG_DECIMAL,
                         new BigDecimal("1.00000000000000000001")),
            Arguments.of(Document.fromNumber(1.0), ShapeType.INTEGER, 1),
            Arguments.of(Document.fromNumber("1E+3"), ShapeType.INTEGER, 1000),
            Arguments.of(Document.fromNumber("1e400"), ShapeType.BIG_INTEGER, BigInteger.TEN.pow(400)),
            Arguments.of(Document.fromNumber("1e-400"), ShapeType.BIG_DECIMAL, new BigDecimal("1e-400")),
            Arguments.of(Document.fromNumber("1E+999999999"), ShapeType.BIG_DECIMAL, new BigDecimal("1E+999999999")),
            Arguments.of(Document.fromNumber(Double.NaN), ShapeType.DOUBLE, Double.NaN),
            Arguments.of(Document.fromNumber(Float.NEGATIVE_INFINITY), ShapeType.DOUBLE, Double.NEGATIVE_INFINITY));
    }

    @ParameterizedTest
    @MethodSource("numbers")
    void wrapLegacy_number_reportsTheNarrowestExactTypeAndKeepsTheSdkNumber(Document legacy, ShapeType type,
                                                                            Number value) {
        software.amazon.awssdk.schema.serde.document.Document view = SchemaSerdeSupport.wrapLegacy(legacy);

        assertThat(view.type()).isEqualTo(type);
        assertThat(view.asNumber()).isEqualTo(value);
        assertThat(SchemaSerdeSupport.unwrapLegacy(view)).isSameAs(legacy);
        assertThat(view.toString()).contains(legacy.asNumber().stringValue());
    }

    @Test
    void wrapLegacy_number_coercesWithJavaConversions() {
        software.amazon.awssdk.schema.serde.document.Document view =
            SchemaSerdeSupport.wrapLegacy(Document.fromNumber("4294967297"));

        assertThat(view.asInteger()).isEqualTo(1);
        assertThat(view.asBigDecimal()).isEqualByComparingTo("4294967297");
        assertThat(SchemaSerdeSupport.wrapLegacy(Document.fromNumber(1.9)).asInteger()).isEqualTo(1);
    }

    @Test
    void wrapLegacy_invalidNumber_throwsWithoutTheValue() {
        assertThatThrownBy(() -> SchemaSerdeSupport.wrapLegacy(Document.fromNumber(SdkNumber.fromString("secret"))))
            .isInstanceOf(SerializationException.class)
            .hasMessage("A document number is not a valid number");
    }

    @Test
    void wrapLegacy_nestedDocument_equalsTheSchemaDocumentOfTheSameValues() {
        software.amazon.awssdk.schema.serde.document.Document view = SchemaSerdeSupport.wrapLegacy(legacyTree());

        software.amazon.awssdk.schema.serde.document.Document expected = schemaTree();
        assertThat(view).isEqualTo(expected);
        assertThat(expected).isEqualTo(view);
        assertThat(view.hashCode()).isEqualTo(expected.hashCode());
        assertThat(view.size()).isEqualTo(3);
        assertThat(view.getMemberNames()).containsExactly("name", "items", "none");
        assertThat(view.getMember("name")).isEqualTo(schemaOf("n"));
        assertThat(view.getMember("none")).isNull();
        assertThat(view.getMember("missing")).isNull();
        assertThat(view.asMap()).containsKey("none");
        assertThat(view.getMember("items").asList()).hasSize(3);
        assertThat(view.getMember("items").asList().get(2)).isNull();
    }

    @Test
    void unwrapLegacy_nestedDocument_equalsTheLegacyDocumentOfTheSameValues() {
        Document view = SchemaSerdeSupport.unwrapLegacy(schemaTree());

        Document expected = legacyTree();
        assertThat(view).isEqualTo(expected);
        assertThat(expected).isEqualTo(view);
        assertThat(view.hashCode()).isEqualTo(expected.hashCode());
        assertThat(view.toString()).isEqualTo(expected.toString());
        assertThat(view.asMap().get("none")).isEqualTo(Document.fromNull());
        assertThat(view.asMap().get("items").asList().get(2)).isEqualTo(Document.fromNull());
        assertThat(view.asMap().get("missing")).isNull();
        assertThat(view.unwrap()).isEqualTo(expected.unwrap());
    }

    @Test
    void unwrapLegacy_emptyContainers_equalTheLegacyEmptyContainers() {
        Document emptyMap = SchemaSerdeSupport.unwrapLegacy(
            software.amazon.awssdk.schema.serde.document.Document.EMPTY_MAP);
        Document emptyList = SchemaSerdeSupport.unwrapLegacy(
            software.amazon.awssdk.schema.serde.document.Document.of(new ArrayList<>()));

        assertThat(emptyMap).isEqualTo(Document.fromMap(new LinkedHashMap<>()));
        assertThat(emptyMap.toString()).isEqualTo("{}");
        assertThat(emptyList).isEqualTo(Document.fromList(new ArrayList<>()));
        assertThat(emptyList.toString()).isEqualTo("[]");
        assertThat(SchemaSerdeSupport.wrapLegacy(Document.fromMap(new LinkedHashMap<>())))
            .isEqualTo(software.amazon.awssdk.schema.serde.document.Document.EMPTY_MAP);
    }

    @Test
    void unwrapLegacy_numbers_useTheFactoryOfTheValueType() {
        assertThat(unwrapNumber(software.amazon.awssdk.schema.serde.document.Document.of(7))).isEqualTo("7");
        assertThat(unwrapNumber(software.amazon.awssdk.schema.serde.document.Document.of((byte) 7))).isEqualTo("7");
        assertThat(unwrapNumber(software.amazon.awssdk.schema.serde.document.Document.of(1L << 40)))
            .isEqualTo("1099511627776");
        assertThat(unwrapNumber(software.amazon.awssdk.schema.serde.document.Document.of(2.0))).isEqualTo("2.0");
        assertThat(unwrapNumber(software.amazon.awssdk.schema.serde.document.Document.of(1.5f))).isEqualTo("1.5");
        assertThat(unwrapNumber(software.amazon.awssdk.schema.serde.document.Document.of(
            new BigInteger("123456789012345678901234567890")))).isEqualTo("123456789012345678901234567890");
        assertThat(unwrapNumber(software.amazon.awssdk.schema.serde.document.Document.of(
            new BigDecimal("1.00000000000000000001")))).isEqualTo("1.00000000000000000001");
    }

    @Test
    void unwrapLegacy_blobAndTimestamp_becomeBase64StringAndEpochSeconds() {
        assertThat(SchemaSerdeSupport.unwrapLegacy(
            software.amazon.awssdk.schema.serde.document.Document.of("hi".getBytes(StandardCharsets.UTF_8))))
            .isEqualTo(Document.fromString("aGk="));
        assertThat(unwrapNumber(software.amazon.awssdk.schema.serde.document.Document.of(
            Instant.ofEpochSecond(1398796238)))).isEqualTo("1398796238");
        assertThat(unwrapNumber(software.amazon.awssdk.schema.serde.document.Document.of(
            Instant.ofEpochMilli(1500).plusNanos(999)))).isEqualTo("1.5");
    }

    @Test
    void wrapLegacy_doesNotCopyOrConvertUntilRead() {
        AtomicInteger reads = new AtomicInteger();
        Map<String, Document> members = new LinkedHashMap<>();
        members.put("counted", new CountingNumberDocument(reads));
        software.amazon.awssdk.schema.serde.document.Document view =
            SchemaSerdeSupport.wrapLegacy(Document.fromMap(members));

        assertThat(reads).hasValue(0);
        members.put("late", Document.fromString("x"));
        assertThat(view.getMemberNames()).containsExactly("counted", "late");
        assertThat(reads).hasValue(0);
        assertThat(view.getMember("counted").asInteger()).isEqualTo(42);
        assertThat(reads).hasValue(1);
    }

    @Test
    void unwrapLegacy_doesNotCopy() {
        Map<String, software.amazon.awssdk.schema.serde.document.Document> members = new LinkedHashMap<>();
        List<software.amazon.awssdk.schema.serde.document.Document> elements = new ArrayList<>();
        members.put("list", software.amazon.awssdk.schema.serde.document.Document.of(elements));
        Document view = SchemaSerdeSupport.unwrapLegacy(software.amazon.awssdk.schema.serde.document.Document.of(members));

        elements.add(schemaOf("late"));
        members.put("later", schemaOf("x"));

        assertThat(view.asMap()).containsOnlyKeys("list", "later");
        assertThat(view.asMap().get("list").asList()).containsExactly(Document.fromString("late"));
    }

    @Test
    void wrapThenUnwrap_returnsTheOriginalLegacyDocument() {
        Document legacy = legacyTree();

        assertThat(SchemaSerdeSupport.unwrapLegacy(SchemaSerdeSupport.wrapLegacy(legacy))).isSameAs(legacy);
    }

    @Test
    void unwrapLegacy_view_isJavaSerializable() throws Exception {
        Document view = SchemaSerdeSupport.unwrapLegacy(schemaTree());

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(view);
        }
        Object copy;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            copy = in.readObject();
        }

        assertThat(copy).isEqualTo(legacyTree());
    }

    private static String unwrapNumber(software.amazon.awssdk.schema.serde.document.Document document) {
        Document legacy = SchemaSerdeSupport.unwrapLegacy(document);
        assertThat(legacy.isNumber()).isTrue();
        return legacy.asNumber().stringValue();
    }

    private static software.amazon.awssdk.schema.serde.document.Document schemaOf(String value) {
        return software.amazon.awssdk.schema.serde.document.Document.of(value);
    }

    /**
     * {"name": "n", "items": [1, {"flag": true, "big": 1.00000000000000000001}, null], "none": null}
     */
    private static Document legacyTree() {
        return Document.mapBuilder()
                       .putString("name", "n")
                       .putList("items", l -> l.addNumber(1)
                                               .addMap(m -> m.putBoolean("flag", true)
                                                             .putNumber("big", new BigDecimal("1.00000000000000000001")))
                                               .addNull())
                       .putNull("none")
                       .build();
    }

    private static software.amazon.awssdk.schema.serde.document.Document schemaTree() {
        Map<String, software.amazon.awssdk.schema.serde.document.Document> inner = new LinkedHashMap<>();
        inner.put("flag", software.amazon.awssdk.schema.serde.document.Document.of(true));
        inner.put("big", software.amazon.awssdk.schema.serde.document.Document.of(new BigDecimal("1.00000000000000000001")));
        Map<String, software.amazon.awssdk.schema.serde.document.Document> outer = new LinkedHashMap<>();
        outer.put("name", schemaOf("n"));
        outer.put("items", software.amazon.awssdk.schema.serde.document.Document.of(Arrays.asList(
            software.amazon.awssdk.schema.serde.document.Document.of(1),
            software.amazon.awssdk.schema.serde.document.Document.of(inner),
            null)));
        outer.put("none", null);
        return software.amazon.awssdk.schema.serde.document.Document.of(outer);
    }

    /**
     * A Legacy_Document number that counts reads of its value.
     */
    private static final class CountingNumberDocument implements Document {
        private static final long serialVersionUID = 1L;
        private final transient AtomicInteger reads;

        private CountingNumberDocument(AtomicInteger reads) {
            this.reads = reads;
        }

        @Override
        public Object unwrap() {
            return asNumber().stringValue();
        }

        @Override
        public boolean isNumber() {
            return true;
        }

        @Override
        public boolean asBoolean() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String asString() {
            throw new UnsupportedOperationException();
        }

        @Override
        public SdkNumber asNumber() {
            reads.incrementAndGet();
            return SdkNumber.fromInteger(42);
        }

        @Override
        public Map<String, Document> asMap() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Document> asList() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <R> R accept(DocumentVisitor<? extends R> visitor) {
            return visitor.visitNumber(asNumber());
        }

        @Override
        public void accept(VoidDocumentVisitor visitor) {
            visitor.visitNumber(asNumber());
        }
    }
}
