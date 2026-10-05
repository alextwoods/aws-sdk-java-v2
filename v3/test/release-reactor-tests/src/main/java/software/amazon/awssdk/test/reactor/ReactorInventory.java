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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.w3c.dom.Element;

/**
 * The set of modules a V3 reactor build includes, computed directly from the POM files rather than from Maven's own
 * reactor resolution so that it can serve as an independent oracle for what a build must have produced.
 *
 * <p>The V3 aggregator ({@code v3/pom.xml}) always builds its top-level {@code <modules>}; the remaining modules come
 * from exactly one aggregate profile. Without {@value #AGGREGATE_PROPERTY} the {@value #FULL_PROFILE} profile is active
 * and the full release reactor is built; {@code -Dv3.aggregate=<name>} selects the {@code v3-<name>} profile instead.
 */
public final class ReactorInventory {
    /** Profile that holds the full release reactor; active unless {@value #AGGREGATE_PROPERTY} is set. */
    public static final String FULL_PROFILE = "v3-all";

    /** User property that selects one aggregate build profile. */
    public static final String AGGREGATE_PROPERTY = "v3.aggregate";

    private static final String V3_GROUP_ID = "software.amazon.awssdk";

    private final Path v3Root;
    private final List<ReactorModule> modules;
    private final Map<String, ReactorModule> byArtifactId;

    private ReactorInventory(Path v3Root, List<ReactorModule> modules) {
        this.v3Root = v3Root;
        this.modules = List.copyOf(modules);
        Map<String, ReactorModule> index = new HashMap<>();
        for (ReactorModule module : modules) {
            ReactorModule previous = index.put(module.artifactId(), module);
            if (previous != null) {
                throw new IllegalStateException("artifactId " + module.artifactId() + " is declared by both "
                                                + previous.path() + " and " + module.path());
            }
        }
        this.byArtifactId = Collections.unmodifiableMap(index);
    }

    /** The full release reactor: what the single supported build command builds. */
    public static ReactorInventory load(Path v3Root) {
        return load(v3Root, FULL_PROFILE);
    }

    /** The reactor built when {@code profileId} is the active aggregate profile. */
    public static ReactorInventory load(Path v3Root, String profileId) {
        Path root = v3Root.toAbsolutePath().normalize();
        Element aggregator = Xml.read(root.resolve("pom.xml"));
        Element profile = profiles(aggregator).get(profileId);
        if (profile == null) {
            throw new IllegalArgumentException("v3/pom.xml has no profile '" + profileId + "'");
        }
        Set<String> selected = new LinkedHashSet<>(modules(aggregator));
        selected.addAll(modules(profile));

        Map<String, ReactorModule> collected = new TreeMap<>();
        Map<Path, Set<String>> referenceCache = new HashMap<>();
        collected.put(".", module(root, root, referenceCache));
        for (String selection : selected) {
            collect(root, root.resolve(selection).normalize(), collected, referenceCache);
        }
        return new ReactorInventory(root, new ArrayList<>(collected.values()));
    }

    /**
     * Aggregate profiles declared by {@code v3/pom.xml}, keyed by profile id, mapped to the {@value #AGGREGATE_PROPERTY}
     * value that activates them. The full-reactor profile is excluded.
     */
    public static Map<String, String> aggregateProfiles(Path v3Root) {
        Map<String, String> result = new TreeMap<>();
        Map<String, Element> profiles = profiles(Xml.read(v3Root.resolve("pom.xml")));
        profiles.forEach((id, profile) -> {
            Element property = Xml.path(profile, "activation", "property");
            if (!FULL_PROFILE.equals(id) && property != null
                && AGGREGATE_PROPERTY.equals(Xml.text(property, "name").orElse(null))) {
                Xml.text(property, "value").ifPresent(value -> result.put(id, value));
            }
        });
        return result;
    }

    public Path v3Root() {
        return v3Root;
    }

    /** Every module, sorted by path. */
    public List<ReactorModule> modules() {
        return modules;
    }

    public Optional<ReactorModule> module(String artifactId) {
        return Optional.ofNullable(byArtifactId.get(artifactId));
    }

    public Set<String> artifactIds() {
        return byArtifactId.keySet();
    }

    /** The module in directory {@code path} (relative to the V3 root, {@code /} separators), if this build has it. */
    public Optional<ReactorModule> moduleAt(String path) {
        return modules.stream().filter(m -> m.path().equals(path)).findFirst();
    }

    /**
     * The slice Maven builds for {@code -pl <selected> -am}: the selected modules plus, transitively, every module of
     * this inventory that they reference (dependencies and build plugins, including inherited ones) and their parent
     * POMs.
     */
    public ReactorInventory alsoMake(String... selectedArtifactIds) {
        Set<String> slice = new LinkedHashSet<>();
        List<String> pending = new ArrayList<>(List.of(selectedArtifactIds));
        while (!pending.isEmpty()) {
            String artifactId = pending.remove(pending.size() - 1);
            ReactorModule module = byArtifactId.get(artifactId);
            if (module == null) {
                throw new IllegalArgumentException(artifactId + " is not a module of " + v3Root);
            }
            if (slice.add(artifactId)) {
                module.v3References().stream().filter(byArtifactId::containsKey).forEach(pending::add);
                if (module.parentArtifactId() != null && byArtifactId.containsKey(module.parentArtifactId())) {
                    pending.add(module.parentArtifactId());
                }
            }
        }
        return new ReactorInventory(v3Root, modules.stream().filter(m -> slice.contains(m.artifactId())).toList());
    }

    private static void collect(Path root, Path directory, Map<String, ReactorModule> collected,
                                Map<Path, Set<String>> referenceCache) {
        ReactorModule module = module(root, directory, referenceCache);
        if (collected.putIfAbsent(module.path(), module) != null) {
            return;
        }
        for (String child : modules(Xml.read(directory.resolve("pom.xml")))) {
            collect(root, directory.resolve(child).normalize(), collected, referenceCache);
        }
    }

    private static ReactorModule module(Path root, Path directory, Map<Path, Set<String>> referenceCache) {
        Path pom = directory.resolve("pom.xml");
        if (!Files.isRegularFile(pom)) {
            throw new IllegalStateException("Reactor module " + directory + " has no pom.xml");
        }
        Element project = Xml.read(pom);
        String relative = root.relativize(directory).toString().replace(directory.getFileSystem().getSeparator(), "/");
        return new ReactorModule(relative.isEmpty() ? "." : relative,
                                 directory,
                                 Xml.text(project, "artifactId").orElseThrow(),
                                 Xml.text(project, "version").or(() -> Xml.text(project, "parent", "version"))
                                    .orElseThrow(),
                                 Xml.text(project, "packaging").orElse("jar"),
                                 Xml.text(project, "parent", "artifactId").orElse(null),
                                 v3References(pom, referenceCache));
    }

    /** V3 dependency and build-plugin artifactIds declared in {@code pom} and its filesystem parent chain. */
    private static Set<String> v3References(Path pom, Map<Path, Set<String>> cache) {
        Path key = pom.toAbsolutePath().normalize();
        Set<String> cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        Element project = Xml.read(key);
        Set<String> references = new LinkedHashSet<>();
        List<Element> declared = new ArrayList<>(Xml.children(Xml.child(project, "dependencies"), "dependency"));
        declared.addAll(Xml.children(Xml.path(project, "build", "plugins"), "plugin"));
        for (Element element : declared) {
            String groupId = Xml.text(element, "groupId").orElse(null);
            String version = Xml.text(element, "version").orElse(null);
            boolean v3Version = version == null || "${project.version}".equals(version) || version.startsWith("3.");
            if (V3_GROUP_ID.equals(groupId) && v3Version) {
                references.add(Xml.text(element, "artifactId").orElseThrow());
            }
        }

        Element parent = Xml.child(project, "parent");
        if (parent != null) {
            String relativePath = Xml.text(parent, "relativePath").orElse("../pom.xml");
            if (!relativePath.isEmpty()) {
                Path parentPom = key.getParent().resolve(relativePath).normalize();
                if (Files.isDirectory(parentPom)) {
                    parentPom = parentPom.resolve("pom.xml");
                }
                if (Files.isRegularFile(parentPom)
                    && Xml.text(Xml.read(parentPom), "artifactId").equals(Xml.text(parent, "artifactId"))) {
                    references.addAll(v3References(parentPom, cache));
                }
            }
        }
        Set<String> result = Set.copyOf(references);
        cache.put(key, result);
        return result;
    }

    private static Map<String, Element> profiles(Element project) {
        Map<String, Element> result = new TreeMap<>();
        for (Element profile : Xml.children(Xml.child(project, "profiles"), "profile")) {
            result.put(Xml.text(profile, "id").orElseThrow(), profile);
        }
        return result;
    }

    private static List<String> modules(Element projectOrProfile) {
        return Xml.children(Xml.child(projectOrProfile, "modules"), "module").stream()
                  .map(e -> e.getTextContent().trim())
                  .toList();
    }
}
