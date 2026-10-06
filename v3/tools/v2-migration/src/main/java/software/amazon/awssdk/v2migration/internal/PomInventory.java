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

package software.amazon.awssdk.v2migration.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import software.amazon.awssdk.v2migration.ItemKind;
import software.amazon.awssdk.v2migration.MigrationItem;
import software.amazon.awssdk.v2migration.SourceLocation;
import software.amazon.awssdk.v2migration.manifest.EntryKind;
import software.amazon.awssdk.v2migration.pom.PomDocument;
import software.amazon.awssdk.v2migration.pom.ProjectDependencies;
import software.amazon.awssdk.v2migration.pom.XmlElement;
import software.amazon.awssdk.v2migration.recipe.PomNode;

/**
 * The Maven side of the migration inventory: every V2 artifact the project declares (dependencies, managed
 * dependencies and BOM imports, located at their {@code <dependency>} element) and every V2 artifact of its effective
 * dependency graph that it does not declare (transitive or inherited, located at the declaration it arrives through).
 * An artifact is V2 when its group is {@code software.amazon.awssdk} and its effective version is 2.x, unknown, or a
 * version range with a 2.x bound.
 * When the POM is editable and declares or resolves any V2 artifact, the inventory also holds the build settings
 * that V3 constrains: the properties supplying V2 declaration versions ({@link ItemKind#VERSION_PROPERTY}) and the
 * Java language level settings ({@link ItemKind#BUILD_SETTING}). These have no Parity Manifest entry (their ID is
 * {@code null}); recipes decide them.
 */
public final class PomInventory {

    /** The V2 (and V3) Maven group. */
    public static final String GROUP_ID = "software.amazon.awssdk";

    /** The {@code maven-compiler-plugin} user properties that select the Java language level. */
    private static final Set<String> COMPILER_PROPERTIES = Set.of(
        "maven.compiler.source", "maven.compiler.target", "maven.compiler.release",
        "maven.compiler.testSource", "maven.compiler.testTarget", "maven.compiler.testRelease");

    /** The {@code maven-compiler-plugin} parameters that select the Java language level. */
    private static final Set<String> COMPILER_PARAMETERS = Set.of(
        "source", "target", "release", "testSource", "testTarget", "testRelease");

    /** One inventoried item and its Maven context. */
    public record Found(MigrationItem item, PomNode node) {
    }

    private PomInventory() {
    }

    /**
     * Inventories {@code dependencies}, locating declarations in {@code document} when the POM is editable.
     *
     * @param document     the project's POM file, or {@code null} when it is not editable
     * @param pomPath      the project-relative path of the POM, used for locations
     * @param dependencies the effective dependency inventory
     */
    public static List<Found> inventory(PomDocument document, String pomPath, ProjectDependencies dependencies) {
        SourceText text = new SourceText(pomPath, document == null ? "" : document.text());
        List<Found> found = new ArrayList<>();
        Set<String> declaredDirect = new HashSet<>();
        for (ProjectDependencies.Declared d : dependencies.declared()) {
            boolean management = ProjectDependencies.isManagement(d.section());
            if (!management) {
                declaredDirect.add(d.groupId() + ":" + d.artifactId());
            }
            if (!isV2(d.groupId(), d.effectiveVersion())) {
                continue;
            }
            ItemKind kind = d.isBomImport() ? ItemKind.BOM_IMPORT
                                            : management ? ItemKind.MANAGED_DEPENDENCY : ItemKind.DEPENDENCY;
            XmlElement element = locate(document, d).orElse(null);
            SourceLocation location = element == null ? text.location(0, 0)
                                                       : text.location(element.start(), element.end());
            String detail = null;
            if (element == null) {
                detail = "The declaration was not located in an editable POM";
            } else if (d.effectiveVersion() == null) {
                detail = "The effective version of the declaration is unknown";
            }
            String construct = d.coordinates() + (d.section().equals(ProjectDependencies.DEPENDENCIES)
                                                  ? "" : " (" + d.section() + ")");
            MigrationItem item = MigrationItem.of(kind, EntryKind.artifactId(d.groupId(), d.artifactId()), construct,
                                                  location, detail);
            found.add(new Found(item, new PomNode(document, dependencies, d, element, null)));
        }
        Set<String> seen = new HashSet<>();
        for (ProjectDependencies.Resolved r : dependencies.resolved()) {
            String key = r.groupId() + ":" + r.artifactId();
            if (!GROUP_ID.equals(r.groupId()) || !r.version().startsWith("2.") || declaredDirect.contains(key)
                || !seen.add(key)) {
                continue;
            }
            int position = position(document, dependencies, r);
            MigrationItem item = MigrationItem.of(ItemKind.EFFECTIVE_DEPENDENCY,
                                                  EntryKind.artifactId(r.groupId(), r.artifactId()),
                                                  key + ":" + r.version() + " via " + String.join(" -> ", r.trail()),
                                                  text.location(position, position), null);
            found.add(new Found(item, new PomNode(document, dependencies, null, null, r)));
        }
        if (document != null && !found.isEmpty()) {
            // Build settings and version properties matter only to a project that uses V2.
            versionProperties(document, text, dependencies, found);
            buildSettings(document, text, dependencies, found);
        }
        return found;
    }

    /**
     * One {@link ItemKind#VERSION_PROPERTY} item per property of this POM's {@code <properties>} that a V2 declaration
     * of this POM uses as its whole version ({@code ${name}}), in property order.
     */
    private static void versionProperties(PomDocument document, SourceText text, ProjectDependencies dependencies,
                                          List<Found> found) {
        Map<String, Set<String>> users = new TreeMap<>();
        for (Found f : found) {
            ProjectDependencies.Declared d = f.node().declared();
            if (d != null) {
                d.versionProperty().ifPresent(
                    p -> users.computeIfAbsent(p, k -> new TreeSet<>()).add(d.groupId() + ":" + d.artifactId()));
            }
        }
        Optional<XmlElement> properties = document.project().child("properties");
        if (users.isEmpty() || properties.isEmpty()) {
            return;
        }
        for (XmlElement property : properties.get().children()) {
            Set<String> artifacts = users.get(property.name());
            if (artifacts == null) {
                continue;
            }
            String detail = "This POM property supplies the version of " + String.join(", ", artifacts)
                            + "; no recipe migrated it";
            MigrationItem item = MigrationItem.of(ItemKind.VERSION_PROPERTY, null,
                                                  text.construct(property.start(), property.end()),
                                                  text.location(property.start(), property.end()), detail);
            found.add(new Found(item, new PomNode(document, dependencies, null, property, null)));
        }
    }

    /**
     * One {@link ItemKind#BUILD_SETTING} item per element of this POM that selects the Java language level: the
     * {@code maven.compiler.*} properties of {@code <properties>}, and the language-level parameters of every
     * {@code maven-compiler-plugin} configuration (plugin and execution level) of {@code <build>} and its
     * {@code <pluginManagement>}. Profiles are not inventoried.
     */
    private static void buildSettings(PomDocument document, SourceText text, ProjectDependencies dependencies,
                                      List<Found> found) {
        List<XmlElement> settings = new ArrayList<>();
        document.project().child("properties").ifPresent(
            p -> p.children().stream().filter(c -> COMPILER_PROPERTIES.contains(c.name())).forEach(settings::add));
        document.project().child("build").ifPresent(build -> {
            List<XmlElement> plugins = new ArrayList<>();
            build.child("plugins").ifPresent(p -> plugins.addAll(p.children("plugin")));
            build.child("pluginManagement").flatMap(m -> m.child("plugins"))
                 .ifPresent(p -> plugins.addAll(p.children("plugin")));
            for (XmlElement plugin : plugins) {
                if (!isCompilerPlugin(plugin)) {
                    continue;
                }
                List<XmlElement> configurations = new ArrayList<>(plugin.children("configuration"));
                plugin.child("executions").ifPresent(
                    e -> e.children("execution").forEach(x -> configurations.addAll(x.children("configuration"))));
                for (XmlElement configuration : configurations) {
                    configuration.children().stream().filter(c -> COMPILER_PARAMETERS.contains(c.name()))
                                 .forEach(settings::add);
                }
            }
        });
        for (XmlElement setting : settings) {
            MigrationItem item = MigrationItem.of(ItemKind.BUILD_SETTING, null,
                                                  text.construct(setting.start(), setting.end()),
                                                  text.location(setting.start(), setting.end()),
                                                  "The Java language level of a V2 project has no Parity Manifest "
                                                  + "entry; no recipe checked it against the V3 minimum");
            found.add(new Found(item, new PomNode(document, dependencies, null, setting, null)));
        }
    }

    private static boolean isCompilerPlugin(XmlElement plugin) {
        return plugin.childText("artifactId").filter("maven-compiler-plugin"::equals).isPresent()
               && plugin.childText("groupId").map("org.apache.maven.plugins"::equals).orElse(true);
    }

    /**
     * True for a {@code software.amazon.awssdk} artifact whose effective version is 2.x, unknown, or a Maven version
     * range (for example {@code [2.20.0,2.26.0)}) with a 2.x bound, which may select a V2 release.
     */
    static boolean isV2(String groupId, String version) {
        return GROUP_ID.equals(groupId) && (version == null || version.startsWith("2.") || isRangeWithV2Bound(version));
    }

    private static boolean isRangeWithV2Bound(String version) {
        String v = version.strip();
        if (!v.startsWith("[") && !v.startsWith("(")) {
            return false;
        }
        for (String bound : v.split("[\\[\\](),]")) {
            if (bound.strip().startsWith("2.")) {
                return true;
            }
        }
        return false;
    }

    private static Optional<XmlElement> locate(PomDocument document, ProjectDependencies.Declared d) {
        if (document == null) {
            return Optional.empty();
        }
        // The section and index address the element; the written artifact ID confirms it (it may be a property).
        return document.dependency(d.section(), d.index())
                       .filter(e -> e.childText("artifactId").map(a -> a.equals(d.artifactId()) || a.contains("${"))
                                     .orElse(false));
    }

    /** Where an effective-graph artifact arrives: its direct declaration, else the parent, else the project. */
    private static int position(PomDocument document, ProjectDependencies dependencies,
                                ProjectDependencies.Resolved r) {
        if (document == null) {
            return 0;
        }
        String direct = r.trail().get(0);
        for (ProjectDependencies.Declared d : dependencies.declared()) {
            if (!ProjectDependencies.isManagement(d.section()) && direct.equals(d.groupId() + ":" + d.artifactId())) {
                Optional<XmlElement> element = locate(document, d);
                if (element.isPresent()) {
                    return element.get().start();
                }
            }
        }
        return document.parent().orElse(document.project()).start();
    }
}
