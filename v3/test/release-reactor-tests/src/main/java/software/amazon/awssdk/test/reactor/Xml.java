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

package software.amazon.awssdk.test.reactor;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/** Minimal, hardened DOM helpers for reading POMs and surefire reports. */
final class Xml {
    private Xml() {
    }

    static Element read(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setExpandEntityReferences(false);
            factory.setNamespaceAware(false);
            return factory.newDocumentBuilder().parse(in).getDocumentElement();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read " + file, e);
        } catch (ParserConfigurationException | SAXException e) {
            throw new IllegalStateException("Unable to parse " + file, e);
        }
    }

    static List<Element> children(Element parent, String name) {
        List<Element> result = new ArrayList<>();
        if (parent == null) {
            return result;
        }
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && name.equals(e.getTagName())) {
                result.add(e);
            }
        }
        return result;
    }

    static Element child(Element parent, String name) {
        List<Element> all = children(parent, name);
        return all.isEmpty() ? null : all.get(0);
    }

    /** Follows a path of child element names, returning null if any step is absent. */
    static Element path(Element parent, String... names) {
        Element current = parent;
        for (String name : names) {
            current = child(current, name);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    static Optional<String> text(Element parent, String... names) {
        Element e = path(parent, names);
        return e == null ? Optional.empty() : Optional.of(e.getTextContent().trim());
    }
}
