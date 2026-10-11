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

package software.amazon.awssdk.protocol.smithytests;

import java.nio.charset.StandardCharsets;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.examples.RecursiveElementNameAndTextQualifier;

/**
 * Compares XML bodies with XMLUnit {@code similar()}, ignoring whitespace between elements. Elements are matched with
 * {@link RecursiveElementNameAndTextQualifier}, as the legacy protocol tests' {@code XmlAsserts} does, so a difference in
 * namespace prefixes or attribute order is not a failure.
 *
 * <p>XMLUnit's whitespace setting is global, so it is applied and restored under a lock around each comparison.
 */
final class XmlBodyComparator implements BodyComparator {
    private static final String DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl";

    static {
        // Test fixtures are trusted, but there is no reason to resolve DTDs or external entities while comparing them.
        try {
            XMLUnit.getControlDocumentBuilderFactory().setFeature(DISALLOW_DOCTYPE, true);
            XMLUnit.getTestDocumentBuilderFactory().setFeature(DISALLOW_DOCTYPE, true);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to harden the XMLUnit document builder factories", e);
        }
    }

    @Override
    public void assertMatches(String expected, byte[] actual) {
        String actualText = new String(actual, StandardCharsets.UTF_8);
        Diff diff;
        boolean similar;
        synchronized (XMLUnit.class) {
            boolean previous = XMLUnit.getIgnoreWhitespace();
            XMLUnit.setIgnoreWhitespace(true);
            try {
                diff = new Diff(expected, actualText);
                diff.overrideElementQualifier(new RecursiveElementNameAndTextQualifier());
                similar = diff.similar();
            } catch (Exception e) {
                throw new AssertionError("Unable to compare XML bodies (is one of them malformed?): " + e.getMessage()
                                         + "\nExpected:\n" + expected + "\nActual:\n" + actualText, e);
            } finally {
                XMLUnit.setIgnoreWhitespace(previous);
            }
        }
        if (!similar) {
            throw new AssertionError("XML bodies are not similar: " + new DetailedDiff(diff).getAllDifferences()
                                     + "\nExpected:\n" + expected + "\nActual:\n" + actualText);
        }
    }
}
