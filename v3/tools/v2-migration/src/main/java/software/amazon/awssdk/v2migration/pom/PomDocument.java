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

package software.amazon.awssdk.v2migration.pom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A losslessly scanned {@code pom.xml}: the element tree with exact offsets, and the {@code <dependency>} declarations
 * of every dependency section in document order, addressed the same way as {@link ProjectDependencies.Declared}
 * (section and index), so effective-model declarations can be located in the file text.
 */
public final class PomDocument {

    private static final Pattern PROPERTY = Pattern.compile("\\$\\{([^}]+)}");
    private static final int MAX_INTERPOLATION_DEPTH = 16;

    private final String path;
    private final String text;
    private final XmlElement project;
    private final Map<String, List<XmlElement>> sections;

    private PomDocument(String path, String text, XmlElement project) {
        this.path = path;
        this.text = text;
        this.project = project;
        Map<String, List<XmlElement>> found = new LinkedHashMap<>();
        found.put(ProjectDependencies.DEPENDENCIES, dependencies(project));
        found.put(ProjectDependencies.DEPENDENCY_MANAGEMENT,
                  project.child("dependencyManagement").map(PomDocument::dependencies).orElse(List.of()));
        project.child("profiles").ifPresent(profiles -> {
            for (XmlElement profile : profiles.children("profile")) {
                String id = profile.childText("id").orElse("default");
                found.put(ProjectDependencies.profileSection(id, false), dependencies(profile));
                found.put(ProjectDependencies.profileSection(id, true),
                          profile.child("dependencyManagement").map(PomDocument::dependencies).orElse(List.of()));
            }
        });
        this.sections = found;
    }

    /** Scans {@code text}, the content of the POM at project-relative {@code path}. */
    public static PomDocument parse(String path, String text) {
        XmlElement root = XmlScanner.scan(path, text);
        if (!root.name().equals("project")) {
            throw new XmlFormatException(path + ":1: the root element is <" + root.name() + ">, not <project>");
        }
        return new PomDocument(path, text, root);
    }

    /** The project-relative path. */
    public String path() {
        return path;
    }

    /** The file text. */
    public String text() {
        return text;
    }

    /** The {@code <project>} element. */
    public XmlElement project() {
        return project;
    }

    /** The {@code <parent>} element, if any. */
    public Optional<XmlElement> parent() {
        return project.child("parent");
    }

    /** The {@code <dependency>} elements of every section, keyed by section name in document order. */
    public Map<String, List<XmlElement>> sections() {
        return sections;
    }

    /** The {@code index}-th {@code <dependency>} element of {@code section}, if present. */
    public Optional<XmlElement> dependency(String section, int index) {
        List<XmlElement> elements = sections.getOrDefault(section, List.of());
        return index >= 0 && index < elements.size() ? Optional.of(elements.get(index)) : Optional.empty();
    }

    /**
     * The dependency inventory of this POM alone: declarations with versions interpolated from this POM's
     * {@code <properties>} and project coordinates, an absent version taken from this POM's dependency management or,
     * for {@code software.amazon.awssdk} artifacts, from an imported {@code software.amazon.awssdk:bom}. No parent POM,
     * imported BOM content or dependency graph is consulted, so {@link ProjectDependencies#resolved()} is empty; the
     * Maven plugin supplies Maven's effective model instead.
     */
    public ProjectDependencies standaloneDependencies() {
        Map<String, String> properties = properties();
        List<ProjectDependencies.Declared> declared = new ArrayList<>();
        sections.forEach((section, elements) -> {
            for (int i = 0; i < elements.size(); i++) {
                XmlElement d = elements.get(i);
                String version = d.childText("version").orElse(null);
                declared.add(new ProjectDependencies.Declared(
                    section, i,
                    interpolate(d.childText("groupId").orElse(""), properties),
                    interpolate(d.childText("artifactId").orElse(""), properties),
                    version,
                    version == null ? null : resolvedOrNull(interpolate(version, properties)),
                    d.childText("type").map(t -> interpolate(t, properties)).orElse(null),
                    d.childText("classifier").map(c -> interpolate(c, properties)).orElse(null),
                    d.childText("scope").map(s -> interpolate(s, properties)).orElse(null)));
            }
        });
        List<ProjectDependencies.Declared> managed = new ArrayList<>();
        for (ProjectDependencies.Declared d : declared) {
            if (d.effectiveVersion() == null && !ProjectDependencies.isManagement(d.section())) {
                managed.add(withVersion(d, managedVersion(d, declared)));
            } else {
                managed.add(d);
            }
        }
        return new ProjectDependencies(managed, List.of());
    }

    /** This POM's properties plus its project coordinates ({@code project.version} and so on). */
    Map<String, String> properties() {
        Map<String, String> properties = new HashMap<>();
        project.child("properties").ifPresent(p -> p.children().forEach(
            c -> properties.put(c.name(), c.text().orElse(""))));
        Optional<XmlElement> parent = parent();
        String groupId = project.childText("groupId")
                                .orElseGet(() -> parent.flatMap(e -> e.childText("groupId")).orElse(null));
        String version = project.childText("version")
                                .orElseGet(() -> parent.flatMap(e -> e.childText("version")).orElse(null));
        putIfPresent(properties, "project.groupId", groupId);
        putIfPresent(properties, "project.version", version);
        putIfPresent(properties, "project.artifactId", project.childText("artifactId").orElse(null));
        parent.flatMap(e -> e.childText("version")).ifPresent(v -> properties.put("project.parent.version", v));
        parent.flatMap(e -> e.childText("groupId")).ifPresent(g -> properties.put("project.parent.groupId", g));
        return properties;
    }

    /** Replaces {@code ${name}} references with {@code properties} values, leaving unknown references unchanged. */
    public static String interpolate(String value, Map<String, String> properties) {
        String current = value;
        for (int depth = 0; depth < MAX_INTERPOLATION_DEPTH && current.contains("${"); depth++) {
            Matcher m = PROPERTY.matcher(current);
            StringBuilder out = new StringBuilder();
            boolean changed = false;
            while (m.find()) {
                String replacement = properties.get(m.group(1));
                if (replacement != null) {
                    changed = true;
                }
                m.appendReplacement(out, Matcher.quoteReplacement(replacement == null ? m.group() : replacement));
            }
            m.appendTail(out);
            current = out.toString();
            if (!changed) {
                break;
            }
        }
        return current;
    }

    private static String resolvedOrNull(String version) {
        return version.contains("${") ? null : version;
    }

    private static String managedVersion(ProjectDependencies.Declared d, List<ProjectDependencies.Declared> all) {
        for (ProjectDependencies.Declared m : all) {
            if (ProjectDependencies.isManagement(m.section()) && !m.isBomImport() && m.groupId().equals(d.groupId())
                && m.artifactId().equals(d.artifactId()) && m.effectiveVersion() != null) {
                return m.effectiveVersion();
            }
        }
        if ("software.amazon.awssdk".equals(d.groupId())) {
            for (ProjectDependencies.Declared m : all) {
                if (m.isBomImport() && "software.amazon.awssdk".equals(m.groupId()) && "bom".equals(m.artifactId())
                    && m.effectiveVersion() != null) {
                    return m.effectiveVersion();
                }
            }
        }
        return null;
    }

    private static ProjectDependencies.Declared withVersion(ProjectDependencies.Declared d, String version) {
        return new ProjectDependencies.Declared(d.section(), d.index(), d.groupId(), d.artifactId(), d.declaredVersion(),
                                                version, d.type(), d.classifier(), d.scope());
    }

    private static List<XmlElement> dependencies(XmlElement owner) {
        return owner.child("dependencies").map(d -> d.children("dependency")).orElse(List.of());
    }

    private static void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
