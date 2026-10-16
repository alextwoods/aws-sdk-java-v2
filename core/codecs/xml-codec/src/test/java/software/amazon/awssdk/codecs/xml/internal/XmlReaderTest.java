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

package software.amazon.awssdk.codecs.xml.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import org.junit.jupiter.api.Test;

/**
 * Port of smithy-java {@code software.amazon.smithy.java.xml.XmlReaderTest}.
 */
public class XmlReaderTest {

    private static final String FOO_BAR_BAZ = "<foo>\n"
                                              + "    <bar>bar</bar>\n"
                                              + "    <baz hi=\"A\" bye=\"B\">1</baz>\n"
                                              + "</foo>\n";

    // smithy-java builds the factory inline with the same four settings; the SDK uses the codec's hardened factory.
    private static XMLInputFactory getFactory() {
        return XmlSerdeFactory.newHardenedInputFactory();
    }

    private static XmlReader.StreamReader streamReader(String xml) throws XMLStreamException {
        XMLInputFactory factory = getFactory();
        return new XmlReader.StreamReader(factory.createXMLStreamReader(new StringReader(xml)), factory);
    }

    @Test
    public void usesStreamReader() throws Exception {
        try (XmlReader.StreamReader reader = streamReader(FOO_BAR_BAZ)) {
            assertThat(reader.getLocation().getLineNumber()).isEqualTo(1);
            assertThat(reader.getText()).isEqualTo("");
            assertThat(reader.nextMemberElement()).isEqualTo("foo");
            assertThat(reader.nextMemberElement()).isEqualTo("bar");
            assertThat(reader.getAttributeValue(null, "bye")).isNull();
            assertThat(reader.getText()).isEqualTo("bar");
            assertThat(reader.getText()).isEqualTo("bar"); // call it again, it's cached now.
            reader.closeElement();
            assertThat(reader.getText()).isNotEqualTo("bar"); // ensure the text is not cached when node changes
            assertThat(reader.nextMemberElement()).isEqualTo("baz");
            assertThat(reader.getAttributeValue(null, "hi")).isEqualTo("A");
            assertThat(reader.getAttributeValue(null, "bye")).isEqualTo("B");
            assertThat(reader.getText()).isEqualTo("1");
            reader.closeElement();
            assertThat(reader.nextMemberElement()).isNull();
            assertThat(reader.getLocation().getLineNumber()).isEqualTo(4);
            assertThat(reader.getLocation().getColumnNumber()).isEqualTo(7);
            reader.closeElement();
        }
    }

    @Test
    public void selfClosedTag() throws Exception {
        try (XmlReader.StreamReader reader = streamReader("<foo/>")) {
            assertThat(reader.getText()).isEqualTo("");
            assertThat(reader.nextMemberElement()).isEqualTo("foo");
            assertThat(reader.getText()).isEqualTo("");
            assertThat(reader.nextMemberElement()).isNull();
            reader.closeElement();
        }
    }

    @Test
    public void getLocationNameFromBufferedReader() throws XMLStreamException {
        XmlReader.BufferedReader bufferedReader =
            new XmlReader.BufferedReader(streamReader("<foo>hi</foo>").bufferElement("foo"));
        assertThat(bufferedReader.getLocation().getLineNumber()).isEqualTo(1);
        assertThat(bufferedReader.getLocation().getColumnNumber()).isEqualTo(6);
    }

    @Test
    public void convertsStreamReaderToString() throws XMLStreamException {
        assertThat(streamReader("<foo>hi</foo>").toString()).isEqualTo("(event: 1, line: 1, column: 6)");
    }

    @Test
    public void convertsBufferedReaderToString() throws XMLStreamException {
        XmlReader.BufferedReader bufferedReader =
            new XmlReader.BufferedReader(streamReader("<foo>hi</foo>").bufferElement("foo"));
        assertThat(bufferedReader.toString()).isEqualTo("(event: 1, line: 1, column: 6)");
    }

    @Test
    public void buffersEvents() throws Exception {
        try (XmlReader.StreamReader r = streamReader(FOO_BAR_BAZ);
             XmlReader.BufferedReader reader = new XmlReader.BufferedReader(r.bufferElement("foo"))) {
            assertThat(reader.getLocation().getLineNumber()).isEqualTo(1);
            assertThat(reader.getText()).isEqualTo("");
            assertThat(reader.nextMemberElement()).isEqualTo("foo");
            assertThat(reader.nextMemberElement()).isEqualTo("bar");
            assertThat(reader.getAttributeValue(null, "bye")).isNull();
            assertThat(reader.getText()).isEqualTo("bar");
            assertThat(reader.getText()).isEqualTo("bar"); // call it again, it's cached now.
            reader.closeElement();
            assertThat(reader.getText()).isNotEqualTo("bar"); // ensure the text is not cached when node changes
            assertThat(reader.nextMemberElement()).isEqualTo("baz");
            assertThat(reader.getAttributeValue(null, "hi")).isEqualTo("A");
            assertThat(reader.getAttributeValue(null, "bye")).isEqualTo("B");
            assertThat(reader.getText()).isEqualTo("1");
            reader.closeElement();
            assertThat(reader.nextMemberElement()).isNull();
            assertThat(reader.getLocation().getLineNumber()).isEqualTo(4);
            assertThat(reader.getLocation().getColumnNumber()).isEqualTo(7);
            reader.closeElement();
        }
    }

    @Test
    public void buffersNestedElementsOfSameName() throws XMLStreamException {
        XmlReader.BufferedReader bufferedReader =
            new XmlReader.BufferedReader(streamReader("<foo><foo><foo>hi</foo></foo></foo>").bufferElement("foo"));

        assertThat(bufferedReader.nextMemberElement()).isEqualTo("foo");
        assertThat(bufferedReader.nextMemberElement()).isEqualTo("foo");
        assertThat(bufferedReader.nextMemberElement()).isEqualTo("foo");
        assertThat(bufferedReader.getText()).isEqualTo("hi");
        bufferedReader.closeElement();
        bufferedReader.closeElement();
        bufferedReader.closeElement();
    }

    @Test
    public void canBufferNestedBuffers() throws XMLStreamException {
        XmlReader.BufferedReader bufferedReader1 =
            new XmlReader.BufferedReader(streamReader("<foo><foo><foo>hi</foo></foo></foo>").bufferElement("foo"));
        XmlReader.BufferedReader bufferedReader2 = new XmlReader.BufferedReader(bufferedReader1.bufferElement("foo"));

        assertThat(bufferedReader2.nextMemberElement()).isEqualTo("foo");
        assertThat(bufferedReader2.nextMemberElement()).isEqualTo("foo");
        assertThat(bufferedReader2.nextMemberElement()).isEqualTo("foo");
        assertThat(bufferedReader2.getText()).isEqualTo("hi");
        bufferedReader2.closeElement();
        bufferedReader2.closeElement();
        bufferedReader2.closeElement();
    }

    @Test
    public void canBufferPartOfBuffer() throws XMLStreamException {
        XmlReader.BufferedReader bufferedReader1 =
            new XmlReader.BufferedReader(streamReader("<foo><bar>hi</bar><baz>bye</baz></foo>").bufferElement("foo"));

        assertThat(bufferedReader1.nextMemberElement()).isEqualTo("foo");
        assertThat(bufferedReader1.nextMemberElement()).isEqualTo("bar");
        // capture bar
        XmlReader.BufferedReader bufferedReader2 = new XmlReader.BufferedReader(bufferedReader1.bufferElement("bar"));
        assertThat(bufferedReader2.getText()).isEqualTo("hi");
        assertThat(bufferedReader2.nextMemberElement()).isNull();
        bufferedReader2.closeElement();
        assertThat(bufferedReader1.nextMemberElement()).isEqualTo("baz");
    }
}
