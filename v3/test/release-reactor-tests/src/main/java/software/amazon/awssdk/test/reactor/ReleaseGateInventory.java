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
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.w3c.dom.Element;
import software.amazon.awssdk.test.reactor.CoreRuntimeSuiteReports.Suite;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;

/**
 * Generates the expected release-gate test inventory of Requirements 16.14, 16.15, 19.14 and 19.15 from the sources of
 * the V3 checkout, so that no release-gate case is listed by hand:
 *
 * <ul>
 *   <li><b>release-gate modules</b>: every concrete test class of the dedicated conformance, endpoint, schema-fidelity,
 *       differential, fault-injection, concurrency, bounded-memory, transport-TCK and module-path/native-image modules
 *       ({@link #RELEASE_GATE_MODULES}); a suite added to one of them is required without further registration;</li>
 *   <li><b>protocols</b>: the launch protocol IDs declared by {@code protocols/protocol-core}'s {@code ProtocolIds},
 *       and every {@code *ConformanceTest} of every protocol runtime module under {@code protocols/}; every launch
 *       protocol must have applicable, executed pinned conformance cases ({@link ProtocolEvidence});</li>
 *   <li><b>models and services</b>: every service pinned by {@code services/service-fleet.json} is one schema-fidelity
 *       case and one endpoint-conformance case, and each service's protocol modules must carry conformance suites;</li>
 *   <li><b>manifest</b>: every evidence ID of the Parity Manifest (the registered launch evidence hooks, the generated
 *       compatibility-fixture hooks and the classification rules' {@code evidenceIds}) is a required suite;</li>
 *   <li><b>transports</b>: every HTTP implementation under {@code http-clients/} must have an HTTP SPI TCK suite bound
 *       by an {@code http-mode:<module>:<mode>} evidence hook, and its own Reactive Streams TCK suites are required;</li>
 *   <li><b>deployment modes</b>: class path (every suite), module path and native image ({@link DeploymentMode}).</li>
 * </ul>
 *
 * Every expected suite carries a per-case and a whole-suite timeout: the suite bound is its surefire execution's
 * {@code forkedProcessTimeoutInSeconds} when the module declares one, otherwise {@link #DEFAULT_SUITE_TIMEOUT}.
 * {@link ReleaseGateEnforcement} checks the inventory against the reports of the current build.
 */
public final class ReleaseGateInventory {
    /** Bound on any one release-gate case unless its suite declares a tighter one (slowest observed case: ~2.5 min). */
    public static final Duration DEFAULT_CASE_TIMEOUT = Duration.ofMinutes(10);
    /** Bound on a whole release-gate suite when its module declares no surefire fork timeout. */
    public static final Duration DEFAULT_SUITE_TIMEOUT = Duration.ofMinutes(30);

    /** The dedicated release-gate modules and the Requirement 19.14 category of their suites. */
    public static final Map<String, String> RELEASE_GATE_MODULES = orderedMap(
        "test/protocol-tests", "conformance",
        "test/endpoint-tests", "endpoint",
        "test/fleet-differential-tests", "schema-fidelity-and-differential",
        "test/dynamic-differential-tests", "differential",
        "test/fault-injection-tests", "fault-injection",
        "test/concurrency-tests", "concurrency",
        "test/bounded-memory-tests", "bounded-memory",
        "test/http-client-tests", "transport-conformance",
        "test/module-path-tests", "module-path");

    static final String PROTOCOL_IDS = "protocols/protocol-core/src/main/java/software/amazon/awssdk/protocols/core/"
                                       + "ProtocolIds.java";
    static final String SERVICE_FLEET = "services/service-fleet.json";
    static final String PARITY_RESOURCES = "tools/parity-manifest/src/main/resources/software/amazon/awssdk/tools/"
                                           + "paritymanifest/";
    static final String LAUNCH_EVIDENCE_HOOKS = PARITY_RESOURCES + "launch-evidence-hooks.txt";
    static final String CLASSIFICATION_RULES = PARITY_RESOURCES + "classification-rules.json";
    static final String COMPATIBILITY_HOOKS = "test/codegen-generated-classes-test/src/test/resources/"
                                              + "compatibility-evidence-hooks.txt";
    static final String PROTOCOL_CONFORMANCE_REPORT = "test/protocol-tests/target/protocol-conformance/"
                                                      + "protocol-conformance-report.json";
    static final String PROTOCOL_CONFORMANCE_SUITE = "software.amazon.awssdk.test.protocoltests.ProtocolConformanceTest";
    static final String PROTOCOL_CASE_FACTORY = "everyApplicablePinnedProtocolCase()";
    static final String SCHEMA_FIDELITY_SUITE = "software.amazon.awssdk.test.fleet.FleetSchemaFidelityTest";
    static final String SCHEMA_FIDELITY_FACTORY = "everyGeneratedSchemaMatchesItsCanonicalShape()";
    static final String ENDPOINT_SUITE = "software.amazon.awssdk.test.endpointtests.EndpointConformanceTest";
    static final String ENDPOINT_FACTORY = "everyFleetEndpointTestCase()";
    static final String PROTOCOL_CASE_TIMEOUT_SOURCE = "test/protocol-tests/src/test/java/software/amazon/awssdk/test/"
                                                       + "protocoltests/support/Conformance.java";
    /** Cross-protocol suites: each runs every launch protocol. */
    static final List<String> CROSS_PROTOCOL_SUITES = List.of(
        PROTOCOL_CONFORMANCE_SUITE,
        "software.amazon.awssdk.test.fleet.WireDifferentialTest",
        "software.amazon.awssdk.test.differential.GeneratedDynamicDifferentialMatrixTest",
        "software.amazon.awssdk.test.fault.PipelineFaultMatrixTest");
    /** Suites that run every HTTP implementation. */
    static final List<String> CROSS_TRANSPORT_SUITES = List.of(
        "software.amazon.awssdk.http.tck.TransportConcurrencyTest",
        "software.amazon.awssdk.test.modulepath.ModulePathSmokeTest");

    private static final Pattern SUREFIRE_DEFAULT_INCLUDES = Pattern.compile("Test.*|.*Test|.*Tests|.*TestCase");
    private static final Pattern SHAPE_ID = Pattern.compile("ShapeId\\.from\\(\"([A-Za-z0-9_.]+#[A-Za-z0-9_]+)\"\\)");
    private static final Pattern DURATION_SECONDS = Pattern.compile(
        "CASE_TIMEOUT\\s*=\\s*Duration\\.ofSeconds\\((\\d+)\\)");
    private static final Pattern HTTP_MODE = Pattern.compile("http-mode:([A-Za-z0-9._-]+):([A-Za-z0-9._-]+)");

    /** The deployment modes of the release reactor (Requirements 16.10, 19.13). */
    public enum DeploymentMode {
        /** Ordinary class-path execution: every release-gate suite. */
        CLASS_PATH,
        /** A named JPMS application on the module path. */
        MODULE_PATH,
        /** A GraalVM native executable; required at the general-availability launch gate stage. */
        NATIVE_IMAGE;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
        }
    }

    /**
     * Which deployment modes the current release reactor must prove. Native image is required when
     * {@code -Dv3.nativeImage.required=true} or {@code -Dv3.launchGate=general-availability}.
     */
    public record DeploymentModes(boolean nativeImageRequired) {
        public static DeploymentModes of(String nativeImageRequired, String launchGate) {
            return new DeploymentModes(Boolean.parseBoolean(nativeImageRequired)
                                       || "general-availability".equals(launchGate));
        }

        public boolean required(DeploymentMode mode) {
            return mode != DeploymentMode.NATIVE_IMAGE || nativeImageRequired;
        }
    }

    /** One case that must appear in its suite's report, with the source that makes it expected. */
    public record ExpectedCase(String name, String source) {
    }

    /**
     * One expected release-gate suite.
     *
     * @param suite the surefire suite (module, class, report name suffix)
     * @param category the Requirement 19.14 category
     * @param sources why the suite is expected (module, protocol, service, manifest hook, transport, deployment mode)
     * @param cases the cases that must be present by name (dynamic cases are named {@code <factory>()[<n>]})
     * @param mode the deployment mode the suite proves
     * @param caseTimeout the bound on any one case
     * @param suiteTimeout the bound on the whole suite
     */
    public record ExpectedSuite(Suite suite, String category, SortedSet<String> sources, List<ExpectedCase> cases,
                                DeploymentMode mode, Duration caseTimeout, Duration suiteTimeout) {
        public String className() {
            return suite.className();
        }
    }

    /**
     * The pinned protocol conformance evidence: the suite's machine-readable report must show applicable, executed
     * cases for every launch protocol, no case left unrun or timed out, and every approved omission run as approved.
     */
    public record ProtocolEvidence(Path report, String suiteClass, String caseFactory, List<String> launchProtocols) {
    }

    /** The generated inventory, with every inconsistency found among its sources. */
    public record Inventory(List<ExpectedSuite> suites, ProtocolEvidence protocolEvidence,
                            List<String> launchProtocols, List<String> services, List<String> transports,
                            List<String> problems) {
        public Optional<ExpectedSuite> suite(String className) {
            return suites.stream().filter(s -> s.className().equals(className)).findFirst();
        }

        public int caseCount() {
            return suites.stream().mapToInt(s -> s.cases().size()).sum();
        }
    }

    private final Path v3Root;
    private final List<String> problems = new ArrayList<>();
    private final Map<String, List<TestClass>> testClasses;
    private final Map<String, Builder> suites = new TreeMap<>();

    private ReleaseGateInventory(Path v3Root) {
        this.v3Root = v3Root;
        this.testClasses = indexTestClasses(v3Root);
    }

    /** Generates the inventory from the checkout at {@code v3Root}. */
    public static Inventory generate(Path v3Root) {
        return new ReleaseGateInventory(v3Root.toAbsolutePath().normalize()).build();
    }

    private Inventory build() {
        releaseGateModules();
        List<String> protocols = protocols();
        List<String> services = services();
        manifest();
        List<String> transports = transports();
        deploymentModes();
        ProtocolEvidence evidence = new ProtocolEvidence(v3Root.resolve(PROTOCOL_CONFORMANCE_REPORT),
                                                         PROTOCOL_CONFORMANCE_SUITE, PROTOCOL_CASE_FACTORY,
                                                         protocols);
        List<ExpectedSuite> built = new ArrayList<>();
        for (Builder builder : suites.values()) {
            built.add(builder.build());
        }
        return new Inventory(List.copyOf(built), evidence, protocols, services, transports, List.copyOf(problems));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Sources
    // ---------------------------------------------------------------------------------------------------------------

    private void releaseGateModules() {
        RELEASE_GATE_MODULES.forEach((module, category) -> {
            List<TestClass> classes = testClasses.values().stream().flatMap(List::stream)
                                                 .filter(c -> c.module().equals(module)).toList();
            if (classes.isEmpty()) {
                problems.add("release-gate module " + module + " has no concrete test class");
            }
            for (TestClass c : classes) {
                expect(c, category, "release-gate-module:" + module);
            }
        });
    }

    private List<String> protocols() {
        List<String> launch = new ArrayList<>();
        Path ids = v3Root.resolve(PROTOCOL_IDS);
        if (!Files.isRegularFile(ids)) {
            problems.add("no launch protocol IDs: " + PROTOCOL_IDS + " is missing");
        } else {
            Matcher m = SHAPE_ID.matcher(read(ids));
            while (m.find()) {
                launch.add(m.group(1));
            }
            if (launch.isEmpty()) {
                problems.add(PROTOCOL_IDS + " declares no launch protocol ID");
            }
        }
        for (String suite : CROSS_PROTOCOL_SUITES) {
            Builder builder = suites.get(suite);
            if (builder == null) {
                problems.add("cross-protocol suite " + suite + " is not a concrete test class of a release-gate module");
                continue;
            }
            launch.forEach(id -> builder.sources.add("protocol:" + id));
        }
        Builder conformance = suites.get(PROTOCOL_CONFORMANCE_SUITE);
        if (conformance != null) {
            protocolCaseTimeout().ifPresent(t -> conformance.caseTimeout = t);
        }
        for (String module : protocolModules()) {
            List<TestClass> conformanceSuites = testClasses.values().stream().flatMap(List::stream)
                                                           .filter(c -> c.module().equals("protocols/" + module))
                                                           .filter(c -> c.simpleName().endsWith("ConformanceTest"))
                                                           .toList();
            if (conformanceSuites.isEmpty()) {
                problems.add("protocol runtime protocols/" + module + " has no *ConformanceTest suite");
            }
            conformanceSuites.forEach(c -> expect(c, "conformance", "protocol-module:" + module));
        }
        return List.copyOf(launch);
    }

    private Optional<Duration> protocolCaseTimeout() {
        Path source = v3Root.resolve(PROTOCOL_CASE_TIMEOUT_SOURCE);
        if (!Files.isRegularFile(source)) {
            return Optional.empty();
        }
        Matcher m = DURATION_SECONDS.matcher(read(source));
        return m.find() ? Optional.of(Duration.ofSeconds(Long.parseLong(m.group(1)))) : Optional.empty();
    }

    private List<String> protocolModules() {
        return subModules("protocols").stream().filter(m -> !m.equals("protocol-core")).toList();
    }

    private List<String> services() {
        Path fleet = v3Root.resolve(SERVICE_FLEET);
        if (!Files.isRegularFile(fleet)) {
            problems.add("no service fleet: " + SERVICE_FLEET + " is missing");
            return List.of();
        }
        List<String> names = new ArrayList<>();
        SortedSet<String> protocolModules = new TreeSet<>(protocolModules());
        ArrayNode services = Node.parse(read(fleet)).expectObjectNode().expectArrayMember("services");
        for (Node node : services.getElements()) {
            ObjectNode service = node.expectObjectNode();
            String name = service.expectStringMember("name").getValue();
            names.add(name);
            for (StringNode module : service.getArrayMember("protocolModules").map(a -> a.getElementsAs(
                StringNode.class)).orElse(List.of())) {
                if (!protocolModules.contains(module.getValue())) {
                    problems.add("service " + name + " uses protocol module " + module.getValue()
                                 + ", which is not a protocol runtime under protocols/");
                }
            }
        }
        if (names.isEmpty()) {
            problems.add(SERVICE_FLEET + " pins no service");
        }
        dynamicCases(SCHEMA_FIDELITY_SUITE, SCHEMA_FIDELITY_FACTORY, names, "schema fidelity");
        dynamicCases(ENDPOINT_SUITE, ENDPOINT_FACTORY, names, "endpoint conformance");
        return List.copyOf(names);
    }

    /** One dynamic case per fleet service: {@code <factory>[1..n]}, in fleet order. */
    private void dynamicCases(String suite, String factory, List<String> services, String what) {
        Builder builder = suites.get(suite);
        if (builder == null) {
            problems.add(what + " suite " + suite + " is not a concrete test class of a release-gate module");
            return;
        }
        builder.sources.add("models:" + SERVICE_FLEET + " (" + services.size() + " services)");
        for (int i = 0; i < services.size(); i++) {
            builder.cases.add(new ExpectedCase(factory + "[" + (i + 1) + "]",
                                               "service " + (i + 1) + " of " + services.size()));
        }
    }

    private void manifest() {
        Map<String, SortedSet<String>> hooks = new TreeMap<>();
        readHooks(LAUNCH_EVIDENCE_HOOKS, hooks);
        readHooks(COMPATIBILITY_HOOKS, hooks);
        Path rules = v3Root.resolve(CLASSIFICATION_RULES);
        if (!Files.isRegularFile(rules)) {
            problems.add("no Parity Manifest classification rules: " + CLASSIFICATION_RULES + " is missing");
        } else {
            collectEvidenceIds(Node.parse(read(rules)), hooks);
        }
        hooks.forEach((evidenceId, entries) -> {
            List<TestClass> matches = testClasses.getOrDefault(evidenceId, List.of());
            if (matches.isEmpty()) {
                problems.add("Parity Manifest evidence ID " + evidenceId + " names no concrete test class of the V3 "
                             + "tree");
            } else if (matches.size() > 1) {
                problems.add("Parity Manifest evidence ID " + evidenceId + " is ambiguous: " + matches);
            } else {
                Builder builder = expect(matches.get(0), "compatibility", "manifest-hook:" + evidenceId);
                entries.stream().filter(e -> e.startsWith("http-mode:") || e.startsWith("behavior:")
                                             || e.startsWith("service:"))
                       .forEach(e -> builder.sources.add("manifest-entry:" + e));
            }
        });
    }

    private void readHooks(String file, Map<String, SortedSet<String>> hooks) {
        Path path = v3Root.resolve(file);
        if (!Files.isRegularFile(path)) {
            problems.add("no evidence hooks: " + file + " is missing");
            return;
        }
        for (String line : lines(path)) {
            String content = line.strip();
            if (content.isEmpty() || content.startsWith("#")) {
                continue;
            }
            String[] fields = content.split("\\s+");
            SortedSet<String> entries = hooks.computeIfAbsent(fields[0], k -> new TreeSet<>());
            for (int i = 1; i < fields.length; i++) {
                entries.add(fields[i]);
            }
        }
    }

    private static void collectEvidenceIds(Node node, Map<String, SortedSet<String>> hooks) {
        if (node.isObjectNode()) {
            node.expectObjectNode().getMembers().forEach((key, value) -> {
                if (key.getValue().equals("evidenceIds") && value.isArrayNode()) {
                    value.expectArrayNode().getElements().forEach(
                        id -> hooks.computeIfAbsent(id.expectStringNode().getValue(), k -> new TreeSet<>()));
                } else {
                    collectEvidenceIds(value, hooks);
                }
            });
        } else if (node.isArrayNode()) {
            node.expectArrayNode().getElements().forEach(e -> collectEvidenceIds(e, hooks));
        }
    }

    private List<String> transports() {
        List<String> transports = subModules("http-clients");
        if (transports.isEmpty()) {
            problems.add("no HTTP implementation under http-clients/");
        }
        Map<String, SortedSet<String>> modes = new TreeMap<>();
        for (Builder builder : suites.values()) {
            for (String source : builder.sources) {
                Matcher m = HTTP_MODE.matcher(source);
                if (m.find()) {
                    modes.computeIfAbsent(m.group(1), k -> new TreeSet<>()).add(m.group(2));
                }
            }
        }
        for (String transport : transports) {
            if (!modes.containsKey(transport)) {
                problems.add("HTTP implementation http-clients/" + transport + " has no HTTP SPI TCK suite bound by an "
                             + "http-mode:" + transport + ":<mode> Parity Manifest evidence hook");
            }
            testClasses.values().stream().flatMap(List::stream)
                       .filter(c -> c.module().equals("http-clients/" + transport))
                       .filter(c -> c.simpleName().endsWith("TckTest"))
                       .forEach(c -> expect(c, "bounded-memory", "transport:" + transport));
        }
        for (String suite : CROSS_TRANSPORT_SUITES) {
            Builder builder = suites.get(suite);
            if (builder == null) {
                problems.add("cross-transport suite " + suite + " is not a concrete test class of a release-gate "
                             + "module");
                continue;
            }
            transports.forEach(t -> builder.sources.add("transport:" + t));
        }
        return List.copyOf(transports);
    }

    private void deploymentModes() {
        for (Builder builder : suites.values()) {
            if (builder.testClass.module().equals("test/module-path-tests")) {
                builder.mode = builder.testClass.simpleName().contains("NativeImage") ? DeploymentMode.NATIVE_IMAGE
                                                                                      : DeploymentMode.MODULE_PATH;
                if (builder.mode == DeploymentMode.NATIVE_IMAGE) {
                    builder.category = "native-image";
                }
            }
            builder.sources.add("deployment:" + builder.mode.id());
        }
        for (DeploymentMode mode : DeploymentMode.values()) {
            if (suites.values().stream().noneMatch(b -> b.mode == mode)) {
                problems.add("deployment mode " + mode.id() + " has no release-gate suite");
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Suites and their surefire executions
    // ---------------------------------------------------------------------------------------------------------------

    private Builder expect(TestClass c, String category, String source) {
        Builder builder = suites.computeIfAbsent(c.className(), k -> new Builder(c, category, execution(c)));
        builder.sources.add(source);
        return builder;
    }

    /** The surefire execution that runs {@code c}: its report name suffix and fork timeout. */
    private Execution execution(TestClass c) {
        Path pom = v3Root.resolve(c.module()).resolve("pom.xml");
        Element surefire = Files.isRegularFile(pom) ? surefirePlugin(Xml.read(pom)) : null;
        if (surefire == null) {
            return new Execution(null, DEFAULT_SUITE_TIMEOUT);
        }
        String sourcePath = c.className().replace('.', '/') + ".java";
        for (Element execution : Xml.children(Xml.child(surefire, "executions"), "execution")) {
            Element configuration = Xml.child(execution, "configuration");
            Optional<String> suffix = Xml.text(configuration, "reportNameSuffix");
            if (suffix.isPresent() && matchesAny(Xml.child(configuration, "includes"), "include", sourcePath)) {
                return new Execution(suffix.get(), timeout(configuration, Xml.child(surefire, "configuration")));
            }
        }
        return new Execution(null, timeout(Xml.child(surefire, "configuration"), null));
    }

    private static Element surefirePlugin(Element project) {
        return Xml.children(Xml.path(project, "build", "plugins"), "plugin").stream()
                  .filter(p -> Xml.text(p, "artifactId").equals(Optional.of("maven-surefire-plugin")))
                  .findFirst().orElse(null);
    }

    private static Duration timeout(Element configuration, Element fallback) {
        Optional<String> seconds = Xml.text(configuration, "forkedProcessTimeoutInSeconds")
                                      .or(() -> Xml.text(fallback, "forkedProcessTimeoutInSeconds"));
        return seconds.filter(s -> s.matches("\\d+")).map(s -> Duration.ofSeconds(Long.parseLong(s)))
                      .orElse(DEFAULT_SUITE_TIMEOUT);
    }

    private static boolean matchesAny(Element patterns, String element, String sourcePath) {
        return Xml.children(patterns, element).stream()
                  .anyMatch(p -> glob(p.getTextContent().strip()).matcher(sourcePath).matches());
    }

    /** Surefire include/exclude patterns: {@code **}/ spans directories, {@code *} stays within one. */
    static Pattern glob(String pattern) {
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            char ch = pattern.charAt(i);
            if (pattern.startsWith("**/", i)) {
                regex.append("(?:.*/)?");
                i += 2;
            } else if (ch == '*') {
                regex.append("[^/]*");
            } else {
                regex.append(Pattern.quote(String.valueOf(ch)));
            }
        }
        return Pattern.compile(regex.toString());
    }

    private record Execution(String reportNameSuffix, Duration timeout) {
    }

    private static final class Builder {
        private final TestClass testClass;
        private final Execution execution;
        private final SortedSet<String> sources = new TreeSet<>();
        private final List<ExpectedCase> cases = new ArrayList<>();
        private String category;
        private DeploymentMode mode = DeploymentMode.CLASS_PATH;
        private Duration caseTimeout = DEFAULT_CASE_TIMEOUT;

        private Builder(TestClass testClass, String category, Execution execution) {
            this.testClass = testClass;
            this.category = category;
            this.execution = execution;
        }

        private ExpectedSuite build() {
            Suite suite = new Suite(testClass.module(), testClass.className(), execution.reportNameSuffix());
            Duration perCase = caseTimeout.compareTo(execution.timeout()) > 0 ? execution.timeout() : caseTimeout;
            return new ExpectedSuite(suite, category, Collections.unmodifiableSortedSet(sources), List.copyOf(cases),
                                     mode, perCase, execution.timeout());
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // The V3 tree
    // ---------------------------------------------------------------------------------------------------------------

    /** A concrete test class that surefire's default includes run. */
    record TestClass(String module, String className) {
        String simpleName() {
            return className.substring(className.lastIndexOf('.') + 1);
        }

        @Override
        public String toString() {
            return module + " " + className;
        }
    }

    /** Every concrete, surefire-included top-level test class of every module, by simple name. */
    private static Map<String, List<TestClass>> indexTestClasses(Path v3Root) {
        Map<String, List<TestClass>> index = new TreeMap<>();
        try {
            Files.walkFileTree(v3Root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) {
                    String name = dir.getFileName() == null ? "" : dir.getFileName().toString();
                    String relative = v3Root.relativize(dir).toString().replace('\\', '/');
                    // Build output, VCS metadata, and fixture projects nested inside a module's src/ tree (whose own
                    // src/test/java holds fixture classes, not suites of the reactor) are not scanned.
                    boolean nestedSource = name.equals("src") && ("/" + relative).contains("/src/");
                    boolean mainSources = name.equals("main") && relative.endsWith("src/main");
                    boolean hidden = name.startsWith(".") && !dir.equals(v3Root);
                    if (name.equals("target") || name.equals("node_modules") || hidden || nestedSource
                        || mainSources) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    if (file.getFileName().toString().endsWith(".java")) {
                        testClass(v3Root, file).ifPresent(
                            c -> index.computeIfAbsent(c.simpleName(), k -> new ArrayList<>()).add(c));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to scan " + v3Root, e);
        }
        return index;
    }

    private static Optional<TestClass> testClass(Path v3Root, Path file) {
        String relative = v3Root.relativize(file).toString().replace('\\', '/');
        int marker = relative.indexOf("/src/test/java/");
        if (marker < 0 || relative.startsWith("target/")) {
            return Optional.empty();
        }
        String module = relative.substring(0, marker);
        String classPath = relative.substring(marker + "/src/test/java/".length());
        String className = classPath.substring(0, classPath.length() - ".java".length()).replace('/', '.');
        String simpleName = className.substring(className.lastIndexOf('.') + 1);
        if (!SUREFIRE_DEFAULT_INCLUDES.matcher(simpleName).matches()) {
            return Optional.empty();
        }
        String source = read(file);
        // Only classes hold tests surefire runs; a *Test interface, enum or record (a TCK fixture type) is not a suite.
        boolean declared = Pattern.compile("\\bclass\\s+" + simpleName + "\\b").matcher(source).find();
        boolean isAbstract = Pattern.compile("\\babstract\\s+class\\s+" + simpleName + "\\b").matcher(source).find();
        return declared && !isAbstract ? Optional.of(new TestClass(module, className)) : Optional.empty();
    }

    private List<String> subModules(String group) {
        Path dir = v3Root.resolve(group);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> children = Files.list(dir)) {
            return children.filter(p -> Files.isRegularFile(p.resolve("pom.xml")))
                           .map(p -> p.getFileName().toString()).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to list " + dir, e);
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read " + file, e);
        }
    }

    private static List<String> lines(Path file) {
        try {
            return Files.readAllLines(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read " + file, e);
        }
    }

    private static Map<String, String> orderedMap(String... keyValues) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return Collections.unmodifiableMap(map);
    }
}
