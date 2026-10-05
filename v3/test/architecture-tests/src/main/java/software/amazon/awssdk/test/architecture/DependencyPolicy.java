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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The machine-readable V3 dependency policy (Requirement 15.2), loaded from the generated resource
 * {@value #RESOURCE}. The source of truth is {@code v3/bom-internal/src/main/policy/dependency-policy.xml}; its
 * semantics are documented in {@code dependency-policy.xsd}.
 *
 * <p>Architecture tests use {@link #checkDirectDependency} for declared module dependencies and
 * {@link #forbiddenClasses} for any artifact reached transitively.
 */
public final class DependencyPolicy {

    public static final String RESOURCE = "META-INF/aws-sdk-java-v3/dependency-policy.xml";
    public static final String SCHEMA_RESOURCE = "META-INF/aws-sdk-java-v3/dependency-policy.xsd";
    public static final String NAMESPACE = "urn:software.amazon.awssdk:v3:dependency-policy:1";
    public static final String SERVICE_KIND = "service";

    private final String groupId;
    private final String releaseVersion;
    private final Map<String, ArtifactClass> artifactClasses;
    private final List<Concern> concerns;
    private final List<Waiver> waivers;
    private final EdgeRules serviceTemplate;
    private final String serviceTemplatePath;
    private final Map<String, ModuleRule> modules;
    private final Set<String> serviceModules;

    private DependencyPolicy(String groupId, String releaseVersion, Map<String, ArtifactClass> artifactClasses,
                             List<Concern> concerns, List<Waiver> waivers, EdgeRules serviceTemplate,
                             String serviceTemplatePath, Map<String, ModuleRule> modules, Set<String> serviceModules) {
        this.groupId = groupId;
        this.releaseVersion = releaseVersion;
        this.artifactClasses = artifactClasses;
        this.concerns = concerns;
        this.waivers = waivers;
        this.serviceTemplate = serviceTemplate;
        this.serviceTemplatePath = serviceTemplatePath;
        this.modules = modules;
        this.serviceModules = serviceModules;
    }

    /** A Maven dependency coordinate. {@code version} is empty when unknown. */
    public record Coordinate(String groupId, String artifactId, String version) {
        public Coordinate {
            Objects.requireNonNull(groupId, "groupId");
            Objects.requireNonNull(artifactId, "artifactId");
            Objects.requireNonNull(version, "version");
        }

        @Override
        public String toString() {
            return groupId + ":" + artifactId + (version.isEmpty() ? "" : ":" + version);
        }
    }

    /** A coordinate pattern; {@code *} matches any character sequence. */
    public record CoordinatePattern(String groupId, String artifactId, String version) {
        public boolean matches(Coordinate coordinate) {
            return glob(groupId, coordinate.groupId())
                   && glob(artifactId, coordinate.artifactId())
                   && glob(version, coordinate.version());
        }

        private static boolean glob(String pattern, String value) {
            if ("*".equals(pattern)) {
                return true;
            }
            StringBuilder regex = new StringBuilder();
            for (String part : pattern.split("\\*", -1)) {
                if (!regex.isEmpty()) {
                    regex.append(".*");
                }
                regex.append(Pattern.quote(part));
            }
            return value.matches(regex.toString());
        }
    }

    public record ArtifactClass(String id, String description, List<CoordinatePattern> coordinates,
                                Set<String> moduleKinds, Set<String> modules) {
    }

    /** Owners are module artifact IDs; ownerKinds are module kinds (for example every generated service). */
    public record Concern(String id, String description, Set<String> owners, Set<String> ownerKinds) {
    }

    public record Waiver(String id, String classId, String groupId, String artifactId, String via, String description,
                         String enforcement) {
        public boolean covers(Coordinate coordinate) {
            return groupId.equals(coordinate.groupId()) && artifactId.equals(coordinate.artifactId());
        }
    }

    /**
     * Dependency rules of a module or of the generated-service template. {@code requiredArtifacts} carry an empty
     * version. {@code requiredServices} are mandatory edges to generated service modules ({@code <requiresService>});
     * they are not part of {@link #requires()}, whose targets always have a {@code <modules>} entry.
     */
    public record EdgeRules(Set<String> requires, List<Set<String>> requiresAnyOf, Set<String> allows,
                            List<Coordinate> requiredArtifacts, Set<String> allowsClasses, Set<String> forbids,
                            Set<String> requiredServices) {

        /** Rules without {@code <requiresService>} edges. */
        public EdgeRules(Set<String> requires, List<Set<String>> requiresAnyOf, Set<String> allows,
                         List<Coordinate> requiredArtifacts, Set<String> allowsClasses, Set<String> forbids) {
            this(requires, requiresAnyOf, allows, requiredArtifacts, allowsClasses, forbids, Set.of());
        }

        /** Every V3 module this rule set may depend on directly. */
        public Set<String> allowedModules() {
            Set<String> result = new TreeSet<>(requires);
            requiresAnyOf.forEach(result::addAll);
            result.addAll(allows);
            result.addAll(requiredServices);
            return result;
        }
    }

    public record ModuleRule(String artifactId, String path, String kind, boolean release, String owns,
                             EdgeRules rules) {
    }

    /** Loads and schema-validates the generated policy resource from this class's class loader. */
    public static DependencyPolicy load() {
        ClassLoader loader = DependencyPolicy.class.getClassLoader();
        URL policy = loader.getResource(RESOURCE);
        URL schema = loader.getResource(SCHEMA_RESOURCE);
        if (policy == null || schema == null) {
            throw new IllegalStateException(RESOURCE + " or " + SCHEMA_RESOURCE + " is not on the class path");
        }
        return load(policy, schema);
    }

    /** Loads and schema-validates a policy document. */
    public static DependencyPolicy load(URL policy, URL schema) {
        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Schema compiled = schemaFactory.newSchema(schema);
            try (InputStream in = policy.openStream()) {
                compiled.newValidator().validate(new StreamSource(in, policy.toString()));
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document;
            try (InputStream in = policy.openStream()) {
                document = builder.parse(in, policy.toString());
            }
            return parse(document.getDocumentElement());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read dependency policy " + policy, e);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid dependency policy " + policy + ": " + e.getMessage(), e);
        }
    }

    /** Returns a copy of this policy that treats {@code artifactIds} as generated service modules. */
    public DependencyPolicy withServiceModules(Collection<String> artifactIds) {
        return new DependencyPolicy(groupId, releaseVersion, artifactClasses, concerns, waivers, serviceTemplate,
                                    serviceTemplatePath, modules, Set.copyOf(artifactIds));
    }

    public String groupId() {
        return groupId;
    }

    public String releaseVersion() {
        return releaseVersion;
    }

    public Map<String, ArtifactClass> artifactClasses() {
        return artifactClasses;
    }

    public List<Concern> concerns() {
        return concerns;
    }

    public List<Waiver> waivers() {
        return waivers;
    }

    public EdgeRules serviceTemplate() {
        return serviceTemplate;
    }

    /** Path pattern (relative to v3/) of generated service modules, for example {@code services/*}. */
    public String serviceTemplatePath() {
        return serviceTemplatePath;
    }

    /** Explicitly listed modules, in policy order. */
    public Map<String, ModuleRule> modules() {
        return modules;
    }

    /** Artifact IDs of the V3 release artifacts: listed modules with release="true" plus generated services. */
    public Set<String> releaseArtifacts() {
        Set<String> result = new TreeSet<>(serviceModules);
        modules.values().stream().filter(ModuleRule::release).map(ModuleRule::artifactId).forEach(result::add);
        return result;
    }

    /** Kind of a V3 module, or empty if it is neither listed nor a known generated service. */
    public Optional<String> kindOf(String artifactId) {
        ModuleRule rule = modules.get(artifactId);
        if (rule != null) {
            return Optional.of(rule.kind());
        }
        return serviceModules.contains(artifactId) ? Optional.of(SERVICE_KIND) : Optional.empty();
    }

    /** Rules of a listed module or of a generated service. */
    public Optional<EdgeRules> rulesFor(String artifactId) {
        ModuleRule rule = modules.get(artifactId);
        if (rule != null) {
            return Optional.of(rule.rules());
        }
        return serviceModules.contains(artifactId) ? Optional.of(serviceTemplate) : Optional.empty();
    }

    /** True for a coordinate of a V3 module at exactly the release version. */
    public boolean isV3Module(Coordinate coordinate) {
        return groupId.equals(coordinate.groupId())
               && releaseVersion.equals(coordinate.version())
               && kindOf(coordinate.artifactId()).isPresent();
    }

    /** True if the artifact class matches {@code coordinate}. */
    public boolean matches(String classId, Coordinate coordinate) {
        ArtifactClass artifactClass = artifactClasses.get(classId);
        if (artifactClass == null) {
            throw new IllegalArgumentException("Unknown artifact class " + classId);
        }
        if (artifactClass.coordinates().stream().anyMatch(p -> p.matches(coordinate))) {
            return true;
        }
        if (!isV3Module(coordinate)) {
            return false;
        }
        return artifactClass.modules().contains(coordinate.artifactId())
               || artifactClass.moduleKinds().contains(kindOf(coordinate.artifactId()).orElseThrow());
    }

    /**
     * Forbidden artifact classes of {@code rules} that match {@code dependency}. A waiver suppresses its class when its
     * "via" module is in {@code modulesOnPath}: the declaring module for a direct edge, or every module on the Maven
     * dependency path for a transitive artifact.
     */
    public List<String> forbiddenClasses(EdgeRules rules, Coordinate dependency, Collection<String> modulesOnPath) {
        List<String> result = new ArrayList<>();
        for (String classId : rules.forbids()) {
            if (matches(classId, dependency) && !waived(classId, dependency, modulesOnPath)) {
                result.add(classId);
            }
        }
        return result;
    }

    /**
     * Checks a dependency declared in compile, runtime or provided scope by {@code fromModule}.
     *
     * @return a violation description, or empty if the edge is allowed
     */
    public Optional<String> checkDirectDependency(String fromModule, Coordinate dependency) {
        EdgeRules rules = rulesFor(fromModule)
            .orElseThrow(() -> new IllegalArgumentException("No policy for module " + fromModule));
        String edge = fromModule + " -> " + dependency;
        List<String> forbidden = forbiddenClasses(rules, dependency, Set.of(fromModule));
        if (!forbidden.isEmpty()) {
            return Optional.of(edge + ": forbidden artifact class " + forbidden);
        }
        if (groupId.equals(dependency.groupId()) && kindOf(dependency.artifactId()).isPresent()) {
            if (!releaseVersion.equals(dependency.version())) {
                return Optional.of(edge + ": V3 module dependencies must be at exactly " + releaseVersion);
            }
            if (!rules.allowedModules().contains(dependency.artifactId())) {
                return Optional.of(edge + ": V3 module edge is not listed in <requires> or <allows>");
            }
            return Optional.empty();
        }
        boolean allowed = rules.allowsClasses().stream().anyMatch(c -> matches(c, dependency))
                          || waivers.stream().anyMatch(w -> w.via().equals(fromModule) && w.covers(dependency));
        return allowed ? Optional.empty()
                       : Optional.of(edge + ": no <allowsClass> artifact class matches this dependency");
    }

    /**
     * Transitive closure of required V3 module edges from {@code rules}; for each {@code requiresAnyOf} group the
     * module in {@code choices} is selected (it must belong to the group).
     */
    public Set<String> requiredClosure(EdgeRules rules, Set<String> choices) {
        Set<String> result = new TreeSet<>();
        Deque<String> pending = new ArrayDeque<>(rules.requires());
        for (Set<String> group : rules.requiresAnyOf()) {
            List<String> selected = group.stream().filter(choices::contains).toList();
            if (selected.isEmpty()) {
                throw new IllegalArgumentException("No module selected from requiresAnyOf group " + group);
            }
            pending.addAll(selected);
        }
        while (!pending.isEmpty()) {
            String next = pending.pop();
            if (result.add(next)) {
                ModuleRule rule = modules.get(next);
                if (rule == null) {
                    throw new IllegalStateException("Required module " + next + " has no policy entry");
                }
                pending.addAll(rule.rules().requires());
            }
        }
        return result;
    }

    private boolean waived(String classId, Coordinate dependency, Collection<String> modulesOnPath) {
        return waivers.stream().anyMatch(w -> w.classId().equals(classId)
                                              && w.covers(dependency)
                                              && modulesOnPath.contains(w.via()));
    }

    // ---- parsing --------------------------------------------------------------------------------------------------

    private static DependencyPolicy parse(Element root) {
        Map<String, ArtifactClass> classes = new LinkedHashMap<>();
        for (Element e : children(child(root, "artifactClasses"), "artifactClass")) {
            List<CoordinatePattern> coordinates = new ArrayList<>();
            for (Element c : children(e, "coordinates")) {
                coordinates.add(new CoordinatePattern(c.getAttribute("groupId"), attr(c, "artifactId", "*"),
                                                      attr(c, "version", "*")));
            }
            classes.put(e.getAttribute("id"),
                        new ArtifactClass(e.getAttribute("id"), text(child(e, "description")), List.copyOf(coordinates),
                                          attributes(children(e, "v3Modules"), "kind"),
                                          attributes(children(e, "v3Module"), "module")));
        }
        List<Concern> concerns = new ArrayList<>();
        for (Element e : children(child(root, "concerns"), "concern")) {
            concerns.add(new Concern(e.getAttribute("id"), text(child(e, "description")),
                                     attributes(children(e, "owner"), "module"),
                                     attributes(children(e, "ownerKind"), "kind")));
        }
        List<Waiver> waivers = new ArrayList<>();
        for (Element e : children(child(root, "waivers"), "waiver")) {
            waivers.add(new Waiver(e.getAttribute("id"), e.getAttribute("class"), e.getAttribute("groupId"),
                                   e.getAttribute("artifactId"), e.getAttribute("via"),
                                   text(child(e, "description")), text(child(e, "enforcement"))));
        }
        Element template = child(root, "serviceTemplate");
        Map<String, ModuleRule> modules = new LinkedHashMap<>();
        for (Element e : children(child(root, "modules"), "module")) {
            modules.put(e.getAttribute("artifactId"),
                        new ModuleRule(e.getAttribute("artifactId"), e.getAttribute("path"), e.getAttribute("kind"),
                                       Boolean.parseBoolean(e.getAttribute("release")), text(child(e, "owns")),
                                       edgeRules(e)));
        }
        return new DependencyPolicy(root.getAttribute("groupId"), root.getAttribute("releaseVersion"),
                                    Collections.unmodifiableMap(classes), List.copyOf(concerns),
                                    List.copyOf(waivers), edgeRules(template), template.getAttribute("path"),
                                    Collections.unmodifiableMap(modules), Set.of());
    }

    private static EdgeRules edgeRules(Element e) {
        List<Set<String>> anyOf = new ArrayList<>();
        for (Element group : children(e, "requiresAnyOf")) {
            anyOf.add(attributes(children(group, "module"), "module"));
        }
        List<Coordinate> artifacts = new ArrayList<>();
        for (Element a : children(e, "requiresArtifact")) {
            artifacts.add(new Coordinate(a.getAttribute("groupId"), a.getAttribute("artifactId"), ""));
        }
        return new EdgeRules(attributes(children(e, "requires"), "module"), List.copyOf(anyOf),
                             attributes(children(e, "allows"), "module"), List.copyOf(artifacts),
                             attributes(children(e, "allowsClass"), "class"),
                             attributes(children(e, "forbids"), "class"),
                             attributes(children(e, "requiresService"), "module"));
    }

    private static List<Element> children(Element parent, String localName) {
        List<Element> result = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element
                && NAMESPACE.equals(element.getNamespaceURI())
                && localName.equals(element.getLocalName())) {
                result.add(element);
            }
        }
        return result;
    }

    private static Element child(Element parent, String localName) {
        List<Element> result = children(parent, localName);
        if (result.size() != 1) {
            throw new IllegalStateException("Expected exactly one <" + localName + "> in <" + parent.getLocalName()
                                            + ">, found " + result.size());
        }
        return result.get(0);
    }

    private static Set<String> attributes(List<Element> elements, String name) {
        Set<String> result = new LinkedHashSet<>();
        for (Element element : elements) {
            result.add(element.getAttribute(name));
        }
        return Collections.unmodifiableSet(result);
    }

    private static String attr(Element element, String name, String defaultValue) {
        return element.hasAttribute(name) ? element.getAttribute(name) : defaultValue;
    }

    private static String text(Element element) {
        return element.getTextContent().strip();
    }
}
