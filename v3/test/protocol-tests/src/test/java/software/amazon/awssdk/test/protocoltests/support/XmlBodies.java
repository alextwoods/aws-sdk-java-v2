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

package software.amazon.awssdk.test.protocoltests.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.opentest4j.AssertionFailedError;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Test support (the rest-xml-protocol module's conformance oracle; test classes are not shared across modules):
 * semantic comparison of XML bodies, as the Smithy protocol tests require. Both documents are parsed
 * namespace-aware and compared element by element:
 * <ul>
 *   <li>elements by their resolved namespace URI and local name (so a redundant or relocated {@code xmlns}
 *       declaration that resolves to the same namespace is insignificant, while a missing or different one is
 *       not);</li>
 *   <li>child elements grouped by resolved name: the order of same-named siblings (list items, flattened members) is
 *       significant, the order of differently named siblings (structure members) is not, as structure members are
 *       unordered in the Smithy data model;</li>
 *   <li>attributes as a set of (resolved namespace URI, local name, value); namespace declarations are not compared
 *       as attributes;</li>
 *   <li>the text of elements without child elements exactly; whitespace-only text between child elements is
 *       insignificant.</li>
 * </ul>
 */
final class XmlBodies {
    private XmlBodies() {
    }

    /** Asserts that {@code actual} is the same XML document as {@code expected}. */
    static void assertEquivalent(String expected, String actual) {
        Element e;
        Element a;
        try {
            e = parse(expected);
        } catch (Exception ex) {
            throw new IllegalStateException("The expected body is not XML: " + expected, ex);
        }
        try {
            a = parse(actual);
        } catch (Exception ex) {
            throw new AssertionFailedError("The body is not XML", expected, actual);
        }
        try {
            compare(e, a, "/" + e.getLocalName());
        } catch (AssertionFailedError failure) {
            throw new AssertionFailedError(failure.getMessage() + "\nexpected: " + expected + "\nactual:   " + actual,
                                           expected, actual);
        }
    }

    private static Element parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))).getDocumentElement();
    }

    private static void compare(Element expected, Element actual, String path) {
        assertEquals(nullToEmpty(expected.getNamespaceURI()), nullToEmpty(actual.getNamespaceURI()),
                     "namespace of " + path);
        assertEquals(expected.getLocalName(), actual.getLocalName(), "element name at " + path);
        assertEquals(attributes(expected), attributes(actual), "attributes of " + path);
        List<Element> expectedChildren = children(expected);
        List<Element> actualChildren = children(actual);
        if (expectedChildren.isEmpty() && actualChildren.isEmpty()) {
            assertEquals(expected.getTextContent(), actual.getTextContent(), "text of " + path);
            return;
        }
        Map<String, List<Element>> expectedGroups = groups(expectedChildren);
        Map<String, List<Element>> actualGroups = groups(actualChildren);
        assertEquals(counts(expectedGroups), counts(actualGroups), "child elements of " + path);
        expectedGroups.forEach((name, elements) -> {
            List<Element> actualElements = actualGroups.get(name);
            for (int i = 0; i < elements.size(); i++) {
                compare(elements.get(i), actualElements.get(i), path + "/" + elements.get(i).getLocalName() + "[" + i
                                                                 + "]");
            }
        });
    }

    /** Child elements by resolved name, each group in document order. */
    private static Map<String, List<Element>> groups(List<Element> elements) {
        Map<String, List<Element>> out = new TreeMap<>();
        for (Element element : elements) {
            out.computeIfAbsent("{" + nullToEmpty(element.getNamespaceURI()) + "}" + element.getLocalName(),
                                k -> new ArrayList<>()).add(element);
        }
        return out;
    }

    private static Map<String, Integer> counts(Map<String, List<Element>> groups) {
        Map<String, Integer> out = new TreeMap<>();
        groups.forEach((name, elements) -> out.put(name, elements.size()));
        return out;
    }

    /** The attributes of an element, without its namespace declarations, as a sorted map. */
    private static Map<String, String> attributes(Element element) {
        Map<String, String> out = new TreeMap<>();
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attribute = (Attr) attributes.item(i);
            if (!XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attribute.getNamespaceURI())) {
                out.put("{" + nullToEmpty(attribute.getNamespaceURI()) + "}" + attribute.getLocalName(),
                        attribute.getValue());
            }
        }
        return out;
    }

    private static List<Element> children(Element element) {
        List<Element> out = new ArrayList<>();
        boolean text = false;
        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                out.add((Element) node);
            } else if (node.getNodeType() == Node.TEXT_NODE && !node.getNodeValue().isBlank()) {
                text = true;
            }
        }
        if (text && !out.isEmpty()) {
            throw new AssertionFailedError("mixed content in " + element.getLocalName());
        }
        return out;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
