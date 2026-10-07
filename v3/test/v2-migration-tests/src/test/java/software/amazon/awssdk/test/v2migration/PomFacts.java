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

package software.amazon.awssdk.test.v2migration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * What a corpus project's own (standalone, parentless) POM says about the build Maven would run: its properties, its
 * Java level, its SDK BOM import and its SDK dependencies with their interpolated versions.
 */
final class PomFacts {
    static final String SDK_GROUP = "software.amazon.awssdk";
    private static final Pattern PROPERTY = Pattern.compile("\\$\\{([^}]+)}");

    /** One SDK dependency of the {@code <dependencies>} section; {@code version} is {@code null} when managed. */
    record Dependency(String artifactId, String version, String scope) {
        String key() {
            return SDK_GROUP + ":" + artifactId;
        }
    }

    private final Map<String, String> properties;
    private final List<Dependency> dependencies;
    private final String bomVersion;

    private PomFacts(Map<String, String> properties, List<Dependency> dependencies, String bomVersion) {
        this.properties = properties;
        this.dependencies = dependencies;
        this.bomVersion = bomVersion;
    }

    static PomFacts read(Path pom) throws IOException {
        Element project;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setNamespaceAware(false);
            project = factory.newDocumentBuilder().parse(pom.toFile()).getDocumentElement();
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException(pom + ": " + e.getMessage(), e);
        }
        Map<String, String> properties = new LinkedHashMap<>();
        child(project, "properties").ifPresent(p -> children(p).forEach(e -> properties.put(e.getTagName(),
                                                                                             e.getTextContent()
                                                                                              .strip())));
        List<Dependency> dependencies = new ArrayList<>();
        child(project, "dependencies").ifPresent(ds -> children(ds).forEach(d -> {
            if (SDK_GROUP.equals(text(d, "groupId", properties))) {
                dependencies.add(new Dependency(text(d, "artifactId", properties), text(d, "version", properties),
                                                text(d, "scope", properties)));
            }
        }));
        String bom = null;
        Optional<Element> managed = child(project, "dependencyManagement").flatMap(m -> child(m, "dependencies"));
        if (managed.isPresent()) {
            for (Element d : children(managed.get())) {
                if (SDK_GROUP.equals(text(d, "groupId", properties)) && "bom".equals(text(d, "artifactId", properties))
                    && "import".equals(text(d, "scope", properties))) {
                    bom = text(d, "version", properties);
                }
            }
        }
        return new PomFacts(properties, List.copyOf(dependencies), bom);
    }

    /** The SDK dependencies of the project's {@code <dependencies>}. */
    List<Dependency> dependencies() {
        return dependencies;
    }

    /** The version of the imported {@code software.amazon.awssdk:bom}, if one is imported. */
    Optional<String> bomVersion() {
        return Optional.ofNullable(bomVersion);
    }

    /**
     * The Java release the Maven compiler plugin compiles for: {@code maven.compiler.release}, else
     * {@code maven.compiler.target} ({@code 1.8} is release 8).
     */
    int javaRelease() {
        String level = properties.getOrDefault("maven.compiler.release", properties.get("maven.compiler.target"));
        if (level == null) {
            throw new IllegalStateException("The POM sets neither maven.compiler.release nor maven.compiler.target");
        }
        level = interpolate(level, properties);
        return Integer.parseInt(level.startsWith("1.") ? level.substring(2) : level);
    }

    private static String text(Element parent, String name, Map<String, String> properties) {
        return child(parent, name).map(e -> interpolate(e.getTextContent().strip(), properties)).orElse(null);
    }

    private static String interpolate(String value, Map<String, String> properties) {
        Matcher m = PROPERTY.matcher(value);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(out, Matcher.quoteReplacement(properties.getOrDefault(m.group(1), m.group())));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static Optional<Element> child(Element parent, String name) {
        return children(parent).stream().filter(e -> e.getTagName().equals(name)).findFirst();
    }

    private static List<Element> children(Element parent) {
        List<Element> out = new ArrayList<>();
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e) {
                out.add(e);
            }
        }
        return out;
    }
}
