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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import software.amazon.awssdk.test.reactor.CoreRuntimeSuiteReports.Suite;
import software.amazon.awssdk.test.reactor.RequiredArtifacts.Category;

/**
 * Static checks of the checked-in V3 reactor definition: the full release reactor contains every artifact category of
 * Requirement 1.3, and every aggregate profile is a self-sufficient (dependency-closed) slice of it.
 */
class ReactorInventoryTest {
    private static final Path V3_ROOT = Path.of(System.getProperty("v3.root")).toAbsolutePath().normalize();
    private static final String CORE_RUNTIME_PROFILE = "v3-core-runtime";
    /** Simple class names matched by surefire's default includes (Test*, *Test, *Tests, *TestCase). */
    private static final Pattern SUREFIRE_DEFAULT_INCLUDES = Pattern.compile("Test.*|.*Test|.*Tests|.*TestCase");

    private static final Map<String, String> EXPECTED_AGGREGATE_PROFILES = Map.of(
        "v3-foundation", "foundation",
        "v3-core-runtime", "core-runtime",
        "v3-runtime", "runtime",
        "v3-http", "http",
        "v3-services", "services",
        "v3-high-level", "high-level",
        "v3-migration", "migration",
        "v3-tests", "tests",
        "v3-benchmarks", "benchmarks",
        "v3-release-evidence", "release-evidence");

    @Test
    void releaseReactorContainsEveryRequiredArtifactCategory() {
        assertEquals(List.of(), RequiredArtifacts.missing(ReactorInventory.load(V3_ROOT)));
    }

    @Test
    void aggregateProfilesAreTheDocumentedSet() {
        assertEquals(EXPECTED_AGGREGATE_PROFILES, ReactorInventory.aggregateProfiles(V3_ROOT));
    }

    /**
     * Every artifact-producing module can be built through some subset profile. Group POMs are exempt: a subset
     * profile may select individual modules of a group (for example {@code tools/v2-migration}), whose parent is then
     * resolved from the checkout via its relativePath. Published releases always use the full reactor.
     */
    @Test
    void everyReleaseReactorArtifactIsSelectableThroughAnAggregateProfile() {
        Set<String> selectable = new HashSet<>();
        for (String profile : EXPECTED_AGGREGATE_PROFILES.keySet()) {
            selectable.addAll(ReactorInventory.load(V3_ROOT, profile).artifactIds());
        }
        Set<String> unselectable = ReactorInventory.load(V3_ROOT).modules().stream()
                                                   .filter(ReactorModule::producesJar)
                                                   .map(ReactorModule::artifactId)
                                                   .collect(Collectors.toCollection(TreeSet::new));
        unselectable.removeAll(selectable);
        assertEquals(Set.of(), unselectable);
    }

    @Test
    void aggregateProfilesAreDependencyClosedSubsetsOfTheReleaseReactor() {
        ReactorInventory full = ReactorInventory.load(V3_ROOT);
        List<String> problems = new ArrayList<>();
        for (String profile : EXPECTED_AGGREGATE_PROFILES.keySet()) {
            ReactorInventory slice = ReactorInventory.load(V3_ROOT, profile);
            for (ReactorModule module : slice.modules()) {
                if (full.module(module.artifactId()).isEmpty()) {
                    problems.add(profile + " builds " + module.path() + ", which is not in the release reactor");
                }
                for (String reference : module.v3References()) {
                    if (full.module(reference).isPresent() && slice.module(reference).isEmpty()) {
                        problems.add(profile + ": " + module.path() + " needs " + reference
                                     + ", which the profile does not build");
                    }
                }
            }
        }
        assertEquals(List.of(), problems);
    }

    @Test
    void eachAggregateProfileBuildsItsArtifactCategory() {
        ReactorInventory full = ReactorInventory.load(V3_ROOT);
        assertBuilds("v3-runtime", RequiredArtifacts.named(Category.RUNTIME));
        assertBuilds("v3-http", RequiredArtifacts.named(Category.HTTP));
        assertBuilds("v3-services", RequiredArtifacts.named(Category.CODE_GENERATION));
        assertBuilds("v3-services", artifactIds(RequiredArtifacts.members(full, Category.SERVICE).stream()
                                                                 .filter(m -> m.path().startsWith("services/"))
                                                                 .toList()));
        assertBuilds("v3-high-level", RequiredArtifacts.named(Category.HIGH_LEVEL_LIBRARY));
        assertBuilds("v3-migration", RequiredArtifacts.named(Category.MIGRATION));
        assertBuilds("v3-tests", artifactIds(RequiredArtifacts.members(full, Category.TEST)));
        assertBuilds("v3-benchmarks", Set.of("sdk-benchmarks"));
        assertBuilds("v3-release-evidence", RequiredArtifacts.named(Category.RELEASE_TOOLING));
        assertBuilds("v3-release-evidence", Set.of("release-reactor-tests"));
    }

    /**
     * The foundation gate runs every verification module except the benchmarks, and architecture-tests inspects the
     * compiled classes of every release module. The profile therefore builds every production and generated-code
     * module first, so it passes from a clean checkout rather than only against previously compiled modules, and it
     * excludes the benchmark harness and the release-evidence tooling.
     */
    @Test
    void foundationProfileBuildsEveryProductionModuleAndEveryVerificationModuleButTheBenchmarks() {
        ReactorInventory full = ReactorInventory.load(V3_ROOT);
        for (Category category : List.of(Category.RUNTIME, Category.HTTP, Category.CODE_GENERATION,
                                          Category.HIGH_LEVEL_LIBRARY, Category.MIGRATION)) {
            assertBuilds("v3-foundation", RequiredArtifacts.named(category));
        }
        assertBuilds("v3-foundation", artifactIds(RequiredArtifacts.members(full, Category.SERVICE)));
        Set<String> verification = new TreeSet<>(artifactIds(RequiredArtifacts.members(full, Category.TEST)));
        verification.remove("sdk-benchmarks");
        assertBuilds("v3-foundation", verification);

        Set<String> excluded = new TreeSet<>(RequiredArtifacts.named(Category.RELEASE_TOOLING));
        excluded.add("sdk-benchmarks");
        excluded.retainAll(ReactorInventory.load(V3_ROOT, "v3-foundation").artifactIds());
        assertEquals(Set.of(), excluded, "v3-foundation must not build benchmarks or release-evidence tooling");
    }

    /** The core-runtime gate extends the foundation gate, so it builds (and tests) every v3-foundation module. */
    @Test
    void coreRuntimeProfileBuildsEveryFoundationModule() {
        assertBuilds(CORE_RUNTIME_PROFILE, ReactorInventory.load(V3_ROOT, "v3-foundation").artifactIds());
    }

    /**
     * Every required core-runtime suite names a concrete test class, matched by surefire's default includes, in a
     * module the core-runtime profile builds, so a renamed or moved suite fails here rather than only in the gate.
     */
    @Test
    void everyCoreRuntimeSuiteIsAConcreteTestClassOfAModuleTheProfileBuilds() throws IOException {
        ReactorInventory coreRuntime = ReactorInventory.load(V3_ROOT, CORE_RUNTIME_PROFILE);
        List<String> problems = new ArrayList<>();
        for (Suite suite : CoreRuntimeSuiteReportsTest.requiredSuites()) {
            Optional<ReactorModule> module = coreRuntime.moduleAt(suite.module());
            Path source = suite.source(V3_ROOT);
            if (module.isEmpty() || !module.get().producesJar()) {
                problems.add(suite + ": " + CORE_RUNTIME_PROFILE + " builds no jar module in " + suite.module());
            } else if (!Files.isRegularFile(source)) {
                problems.add(suite + ": no test source " + V3_ROOT.relativize(source));
            } else if (!SUREFIRE_DEFAULT_INCLUDES.matcher(suite.simpleName()).matches()) {
                problems.add(suite + ": not matched by surefire's default includes");
            } else if (Pattern.compile("\\babstract\\s+class\\s+" + suite.simpleName() + "\\b")
                              .matcher(Files.readString(source)).find()) {
                problems.add(suite + ": is abstract, so surefire writes no report for it");
            }
        }
        assertEquals(List.of(), problems);
    }

    /**
     * The gate runs after every module that holds a required suite: release-reactor-tests' core-runtime-gate profile,
     * activated by the core-runtime aggregate property, declares exactly those modules as classpath-free (type pom,
     * no transitive dependencies) test dependencies, which Maven uses to order (and, with -T, schedule) the reactor.
     */
    @Test
    void coreRuntimeGateProfileOrdersEverySuiteModuleBeforeTheGate() {
        ReactorInventory full = ReactorInventory.load(V3_ROOT);
        Set<String> suiteModules = new TreeSet<>();
        for (Suite suite : CoreRuntimeSuiteReportsTest.requiredSuites()) {
            full.moduleAt(suite.module()).ifPresent(m -> suiteModules.add(m.artifactId()));
        }

        Element project = Xml.read(V3_ROOT.resolve("test/release-reactor-tests/pom.xml"));
        Element gate = Xml.children(Xml.child(project, "profiles"), "profile").stream()
                          .filter(p -> Xml.text(p, "id").orElse("").equals("core-runtime-gate"))
                          .findFirst().orElseThrow(() -> new AssertionError("no core-runtime-gate profile"));
        assertEquals(Optional.of(ReactorInventory.AGGREGATE_PROPERTY), Xml.text(gate, "activation", "property", "name"));
        assertEquals(Optional.of("core-runtime"), Xml.text(gate, "activation", "property", "value"));
        assertEquals(Optional.of("true"), Xml.text(gate, "properties", "v3.coreRuntimeGate"));

        Set<String> ordered = new TreeSet<>();
        List<String> problems = new ArrayList<>();
        for (Element dependency : Xml.children(Xml.child(gate, "dependencies"), "dependency")) {
            String artifactId = Xml.text(dependency, "artifactId").orElseThrow();
            ordered.add(artifactId);
            boolean classpathFree = Xml.text(dependency, "groupId").equals(Optional.of("software.amazon.awssdk"))
                                    && Xml.text(dependency, "version").equals(Optional.of("${project.version}"))
                                    && Xml.text(dependency, "type").equals(Optional.of("pom"))
                                    && Xml.text(dependency, "scope").equals(Optional.of("test"))
                                    && Xml.text(dependency, "exclusions", "exclusion", "groupId")
                                          .equals(Optional.of("*"))
                                    && Xml.text(dependency, "exclusions", "exclusion", "artifactId")
                                          .equals(Optional.of("*"));
            if (!classpathFree) {
                problems.add(artifactId + " must be software.amazon.awssdk:" + artifactId + ":${project.version}, "
                             + "type pom, scope test, excluding *:*");
            }
        }
        assertEquals(List.of(), problems);
        assertEquals(suiteModules, ordered, "core-runtime-gate dependencies must be exactly the suite modules");
    }

    @Test
    void generatedServiceModulesInheritTheCodegenLifecycle() {
        List<ReactorModule> services = RequiredArtifacts.members(ReactorInventory.load(V3_ROOT), Category.SERVICE);
        assertTrue(!services.isEmpty(), "at least the codegen lifecycle fixture must be a generated service module");
        for (ReactorModule service : services) {
            assertTrue(service.v3References().contains("aws-codegen-maven-plugin"),
                       service.path() + " must inherit the aws-codegen-maven-plugin binding");
        }
    }

    private static void assertBuilds(String profile, Set<String> artifactIds) {
        Set<String> missing = new TreeSet<>(artifactIds);
        missing.removeAll(ReactorInventory.load(V3_ROOT, profile).artifactIds());
        assertEquals(Set.of(), missing, profile + " must build these artifacts");
    }

    private static Set<String> artifactIds(List<ReactorModule> modules) {
        return modules.stream().map(ReactorModule::artifactId).collect(Collectors.toSet());
    }
}
