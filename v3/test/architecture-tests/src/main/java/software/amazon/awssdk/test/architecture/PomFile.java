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

package software.amazon.awssdk.test.architecture;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Minimal, dependency-free reader for the raw (non-effective) content of a V3 POM file. Only the project's own
 * elements are read; inherited values are not resolved except for the groupId/version fallback to {@code <parent>}.
 */
public final class PomFile {

    private final Path path;
    private final Element project;

    private PomFile(Path path, Element project) {
        this.path = path;
        this.project = project;
    }

    /** A declared dependency. {@code version} is the raw value with project-level expressions resolved. */
    public record Dependency(String groupId, String artifactId, String version, String scope, String type) {
    }

    public static PomFile read(Path path) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return new PomFile(path, factory.newDocumentBuilder().parse(path.toFile()).getDocumentElement());
        } catch (Exception e) {
            throw new IllegalStateException("Cannot parse " + path + ": " + e.getMessage(), e);
        }
    }

    public Path path() {
        return path;
    }

    public String artifactId() {
        return childText(project, "artifactId").orElseThrow();
    }

    public String groupId() {
        return childText(project, "groupId").or(() -> parent().flatMap(p -> childText(p, "groupId"))).orElseThrow();
    }

    public String version() {
        return childText(project, "version").or(() -> parent().flatMap(p -> childText(p, "version"))).orElseThrow();
    }

    public String packaging() {
        return childText(project, "packaging").orElse("jar");
    }

    public Optional<String> parentArtifactId() {
        return parent().flatMap(p -> childText(p, "artifactId"));
    }

    public boolean declaresDependencyManagement() {
        return child(project, "dependencyManagement").isPresent();
    }

    /** Dependencies declared in {@code <dependencies>} of the project and of every profile. */
    public List<Dependency> dependencies() {
        List<Dependency> result = new ArrayList<>(dependenciesOf(child(project, "dependencies")));
        child(project, "profiles").ifPresent(profiles -> {
            for (Element profile : children(profiles, "profile")) {
                result.addAll(dependenciesOf(child(profile, "dependencies")));
            }
        });
        return result;
    }

    /** Dependencies declared in {@code <dependencyManagement>}. */
    public List<Dependency> managedDependencies() {
        return dependenciesOf(child(project, "dependencyManagement").flatMap(dm -> child(dm, "dependencies")));
    }

    private List<Dependency> dependenciesOf(Optional<Element> dependencies) {
        List<Dependency> result = new ArrayList<>();
        dependencies.ifPresent(deps -> {
            for (Element d : children(deps, "dependency")) {
                result.add(new Dependency(resolve(childText(d, "groupId").orElse("")),
                                          childText(d, "artifactId").orElse(""),
                                          resolve(childText(d, "version").orElse("")),
                                          childText(d, "scope").orElse("compile"),
                                          childText(d, "type").orElse("jar")));
            }
        });
        return result;
    }

    private String resolve(String value) {
        return switch (value) {
            case "${project.version}" -> version();
            case "${project.groupId}" -> groupId();
            default -> value;
        };
    }

    private Optional<Element> parent() {
        return child(project, "parent");
    }

    private static Optional<String> childText(Element parent, String name) {
        return child(parent, name).map(e -> e.getTextContent().strip());
    }

    private static Optional<Element> child(Element parent, String name) {
        List<Element> result = children(parent, name);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    private static List<Element> children(Element parent, String name) {
        List<Element> result = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element && name.equals(element.getTagName())) {
                result.add(element);
            }
        }
        return result;
    }
}
