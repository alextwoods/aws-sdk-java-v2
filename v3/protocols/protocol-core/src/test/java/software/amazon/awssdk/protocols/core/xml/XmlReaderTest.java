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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.SerializationException;
import software.amazon.smithy.java.dynamicschemas.StructDocument;
import software.amazon.smithy.model.node.Node;

/** The shared XML reader: tree parsing, name matching, and the schema-guided deserializer. */
class XmlReaderTest {
    private static final String MODEL = """
        $version: "2"
        namespace example.xml

        structure Doc {
            @xmlAttribute
            @xmlName("xsi:type")
            kind: String
            @xmlAttribute
            plain: String
            @xmlName("Renamed")
            text: String
            @xmlFlattened
            @xmlName("Entry")
            pairs: Pairs
            nested: Nested
            @xmlFlattened
            flat: Nested2List
            sparse: SparseMap
        }

        map Pairs {
            @xmlName("K")
            key: String
            @xmlName("V")
            value: String
        }

        structure Nested {
            @xmlName("Item")
            value: String
        }

        list Nested2List {
            member: Nested
        }

        @sparse
        map SparseMap {
            key: String
            value: String
        }
        """;

    private static XmlElement parse(String xml) {
        return XmlTreeReader.parse(xml.getBytes(StandardCharsets.UTF_8));
    }

    private static Node read(String xml) {
        ShapeBuilder<StructDocument> builder = XmlTestValues.builder(XmlTestValues.model(MODEL), "example.xml#Doc");
        builder.deserialize(XmlShapeDeserializer.of(parse(xml)));
        return XmlTestValues.canonical(builder.build());
    }

    @Test
    void parsesNamesAttributesAndText() {
        XmlElement root = parse("""
            <?xml version="1.0" encoding="UTF-8"?>
            <p:Root xmlns:p="urn:p" xmlns:xsi="urn:xsi" xsi:type="T" a="1">
              <Child>one &amp; <![CDATA[<two>]]></Child><!-- comment -->
              <Child/>
            </p:Root>""");
        assertEquals("Root", root.localName());
        assertEquals("p:Root", root.qualifiedName());
        assertEquals("urn:p", root.namespaceUri());
        assertTrue(root.matches("Root"));
        assertTrue(root.matches("p:Root"));
        assertFalse(root.matches("q:Root"));
        assertEquals("T", root.attribute("xsi:type"));
        assertEquals("1", root.attribute("a"));
        assertNull(root.attribute("type"));
        assertEquals(2, root.children("Child").size());
        assertEquals("one & <two>", root.child("Child").text());
        assertEquals("", root.children("Child").get(1).text());
    }

    @Test
    void rejectsMalformedAndDoctypeInput() {
        assertThrows(SerializationException.class, () -> parse("<a><b></a>"));
        assertThrows(SerializationException.class, () -> parse("not xml"));
        assertThrows(SerializationException.class,
                     () -> parse("<!DOCTYPE a [<!ENTITY e \"boom\">]><a>&e;</a>"));
        assertTrue(XmlTreeReader.isBlank(" \n\t".getBytes(StandardCharsets.UTF_8)));
        assertFalse(XmlTreeReader.isBlank("<a/>".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void deserializesAttributesRenamedAndFlattenedMembers() {
        Node value = read("""
            <Doc xmlns:xsi="urn:xsi" xsi:type="T" plain="P">
              <Renamed>  keep  spaces  </Renamed>
              <Entry><K>a</K><V>A</V></Entry>
              <nested><Item>n</Item></nested>
              <Entry><K>b</K><V>B</V></Entry>
              <flat><Item>1</Item></flat>
              <flat><Item>2</Item></flat>
              <sparse><entry><key>x</key></entry><entry><key>y</key><value>Y</value></entry></sparse>
            </Doc>""");
        assertEquals(Node.parse("""
            {"kind": "T", "plain": "P", "text": "  keep  spaces  ", "pairs": {"a": "A", "b": "B"},
             "nested": {"value": "n"}, "flat": [{"value": "1"}, {"value": "2"}],
             "sparse": {"x": null, "y": "Y"}}"""), value);
    }

    @Test
    void readsEachErrorEnvelopeForm() {
        XmlErrorEnvelope query = XmlErrorEnvelope.read(parse("""
            <ErrorResponse><Error><Type>Sender</Type><Code> C </Code><Message>M</Message></Error>
            <RequestId>r</RequestId></ErrorResponse>""")).orElseThrow();
        assertEquals(List.of("C", "Sender", "M", "r"),
                     List.of(query.code(), query.type(), query.message(), query.requestId()));
        XmlErrorEnvelope ec2 = XmlErrorEnvelope.read(parse("""
            <Response><Errors><Error><Code>C</Code><message>m</message></Error></Errors>
            <RequestID>r2</RequestID></Response>""")).orElseThrow();
        assertEquals(List.of("C", "m", "r2"), List.of(ec2.code(), ec2.message(), ec2.requestId()));
        XmlErrorEnvelope bare = XmlErrorEnvelope.read(parse(
            "<Error><Code>C</Code><RequestId>r3</RequestId></Error>")).orElseThrow();
        assertEquals("r3", bare.requestId());
        assertNull(bare.message());
        assertTrue(XmlErrorEnvelope.read(parse("<Other/>")).isEmpty());
        assertTrue(XmlErrorEnvelope.read(parse("<ErrorResponse/>")).isEmpty());
    }
}
