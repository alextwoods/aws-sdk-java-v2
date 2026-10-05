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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.w3c.dom.Element;
import software.amazon.awssdk.test.reactor.JdkInstallations.Jdk;

/**
 * Requirement 1 JDK and reactor failure modes, exercised by forking Maven on build fixtures in a clean checkout of
 * {@code v3/} with an explicit {@code JAVA_HOME} per fixture run.
 *
 * <p>Fixtures:
 * <ul>
 *   <li><b>Generated-service reactor slice</b>: the supported command narrowed to the codegen lifecycle fixture
 *       service ({@code mvn -f v3/pom.xml verify -pl test/codegen-lifecycle-tests -am}), which builds the parent,
 *       the code generator and its Maven plugin, the services parent, and a generated service in one reactor.</li>
 *   <li><b>Baseline project</b> ({@code src/test/fixtures/jdk-baseline-fixture}): a stand-alone jar module that
 *       inherits the real V3 parent and uses Java 21 language features and APIs.</li>
 *   <li><b>Release reactor</b>: the unmodified supported command {@code mvn -f v3/pom.xml verify}, used for the
 *       rejection cases (it stops in the first module, so it is fast).</li>
 * </ul>
 *
 * <p>Cases:
 * <ul>
 *   <li>JDK 21 (always runs; fails if no JDK 21 can be found): both build fixtures succeed; every class is release 21
 *       bytecode; generation, compilation, tests, and packaging run in order; no Gradle is involved.</li>
 *   <li>Each discovered GA JDK newer than 21 (Requirement 1.2): the same, without source changes. Skipped with the
 *       discovery report when none is found.</li>
 *   <li>Simulated older JDK (always runs): the JDK 21 build with {@code -Dv3.minimum.jdk.version=22}, which makes the
 *       parent's real {@code requireJavaVersion} rule reject the running JDK exactly as it rejects JDK 17 for the
 *       default minimum of 21. The release reactor and the baseline project must fail in {@code v3-build-policy}
 *       before any other mojo runs and leave no class files, generated sources, or archives (1.6, 1.7).</li>
 *   <li>Each discovered installed JDK from 8 to 20: the unmodified builds are rejected the same way, with a
 *       diagnostic naming JDK 21. Skipped with the discovery report when none is found.</li>
 * </ul>
 *
 * <p>JDKs are discovered by {@link JdkInstallations}; pass extra homes with {@code -Dv3.jdkHomes=<home>:<home>} or
 * {@code V3_JDK_HOMES}. The forked Maven is the one running the outer build ({@code maven.home}) with the outer local
 * repository. Tests are skipped inside a build forked by another reactor test.
 *
 * <p>Why fork Maven directly instead of maven-invoker-plugin: each fixture run needs its own JDK, chosen from JDKs
 * discovered at test time, with a JUnit skip reason when a JDK is unavailable (invoker selectors only filter on the
 * JDK running the outer build); and the fixtures must build the reactor's own code generator plugin inside the forked
 * reactor, as the supported {@code verify} command does, instead of requiring {@code invoker:install} of the V3
 * artifacts into a staging repository.
 */
@DisabledIfEnvironmentVariable(named = ReleaseReactorCommand.NESTED_BUILD_ENVIRONMENT_VARIABLE, matches = ".+")
class JdkFailureModeFixtureTest {
    private static final int MINIMUM_JDK = 21;
    /** Oldest JDK that Maven 3.9 itself runs on; older JDKs cannot start the build at all. */
    private static final int OLDEST_JDK_MAVEN_RUNS_ON = 8;
    private static final String SERVICE_FIXTURE = "codegen-lifecycle-tests";
    private static final String SERVICE_FIXTURE_PATH = "test/codegen-lifecycle-tests";
    private static final List<String> BASELINE_CLASSES = List.of(
        "software/amazon/awssdk/test/fixture/jdk/JdkBaselineFixture.class",
        "software/amazon/awssdk/test/fixture/jdk/JdkBaselineFixture$Shape.class",
        "software/amazon/awssdk/test/fixture/jdk/JdkBaselineFixture$Circle.class",
        "software/amazon/awssdk/test/fixture/jdk/JdkBaselineFixture$Square.class");

    private static final JdkInstallations JDKS = JdkInstallations.discover();
    private static final Path WORK = Path.of(System.getProperty("v3.jdkFailureModeTests.workDirectory"));
    private static final MavenFork MAVEN = MavenFork.fromSystemProperties(
        Duration.ofMinutes(Long.getLong("v3.jdkFailureModeTests.timeoutMinutes", 15)));

    @Test
    void jdk21BuildsTheFixturesInOrderWithRelease21Bytecode() throws Exception {
        buildsOnSupportedJdk(jdk21());
    }

    @TestFactory
    Stream<DynamicTest> newerJdksBuildTheFixturesWithoutSourceChanges() {
        return BuildFixtures.perJdk(JDKS, JDKS.newerThan(MINIMUM_JDK),
                                    "No GA JDK newer than " + MINIMUM_JDK + " was found, so the newer-JDK fixtures "
                                    + "(Requirement 1.2) are skipped: ",
                                    JdkFailureModeFixtureTest::buildsOnSupportedJdk);
    }

    @Test
    void simulatedOlderJdkIsRejectedBeforeCompilation() throws Exception {
        Jdk jdk = jdk21();
        int simulatedMinimum = jdk.feature() + 1;
        rejectedBeforeCompilation(jdk, "simulated-minimum-" + simulatedMinimum, simulatedMinimum,
                                  "-Dv3.minimum.jdk.version=" + simulatedMinimum);
    }

    @TestFactory
    Stream<DynamicTest> installedOlderJdksAreRejectedBeforeCompilation() {
        return BuildFixtures.perJdk(JDKS, JDKS.olderThan(MINIMUM_JDK, OLDEST_JDK_MAVEN_RUNS_ON),
                                    "No JDK " + OLDEST_JDK_MAVEN_RUNS_ON + "-" + (MINIMUM_JDK - 1) + " was found, so "
                                    + "only the simulated older-JDK fixture ran: ",
                                    jdk -> rejectedBeforeCompilation(jdk, "installed", MINIMUM_JDK));
    }

    /**
     * The simulation overrides only the minimum; the rule it exercises is the one that enforces JDK 21 by default:
     * {@code requireJavaVersion [${v3.minimum.jdk.version},)} in the validate-phase {@code v3-build-policy} execution,
     * with the minimum in its diagnostic.
     */
    @Test
    void simulationExercisesTheRuleThatEnforcesTheJdk21Minimum() {
        Element parent = Xml.read(BuildFixtures.V3_ROOT.resolve("parent/pom.xml"));
        assertEquals("21", Xml.text(parent, "properties", "v3.minimum.jdk.version").orElse(null));
        assertEquals("21", Xml.text(parent, "properties", "maven.compiler.release").orElse(null));
        Element enforcer = Xml.children(Xml.path(parent, "build", "plugins"), "plugin").stream()
                              .filter(p -> Xml.text(p, "artifactId").orElse("").equals("maven-enforcer-plugin"))
                              .findFirst().orElseThrow();
        Element policy = Xml.children(Xml.child(enforcer, "executions"), "execution").stream()
                            .filter(e -> Xml.text(e, "id").orElse("").equals("v3-build-policy"))
                            .findFirst().orElseThrow();
        assertEquals("validate", Xml.text(policy, "phase").orElse(null));
        assertEquals("[${v3.minimum.jdk.version},)",
                     Xml.text(policy, "configuration", "rules", "requireJavaVersion", "version").orElse(null));
        assertTrue(Xml.text(policy, "configuration", "rules", "requireJavaVersion", "message").orElse("")
                      .contains("requires JDK ${v3.minimum.jdk.version} or newer (minimum supported JDK: "
                                + "${v3.minimum.jdk.version})"));
    }

    private static void buildsOnSupportedJdk(Jdk jdk) throws Exception {
        Path work = WORK.resolve("supported-jdk-" + jdk.feature());
        Path checkout = BuildFixtures.checkout(work);

        MavenFork.Result slice = MAVEN.run(checkout, jdk.home(), ReleaseReactorCommand.argumentsWith(
            "-V", "-pl", SERVICE_FIXTURE_PATH, "-am"), work.resolve("service-slice.log"));
        assertEquals(0, slice.exitCode(), slice::describe);
        List<String> sliceLog = slice.readLog();
        assertRanOn(jdk, sliceLog, slice);
        ReactorInventory expected = ReactorInventory.load(checkout.resolve("v3")).alsoMake(SERVICE_FIXTURE);
        assertTrue(expected.module("aws-codegen-maven-plugin").isPresent(), "the slice builds the code generator");
        assertEquals(List.of(), ReactorBuildVerifier.verify(expected, sliceLog), slice::describe);
        assertEquals(List.of(), BuildAudit.lifecycleOrdering(expected, sliceLog), slice::describe);
        assertNoGradle(slice, sliceLog);

        MavenFork.Result baseline = MAVEN.run(checkout, jdk.home(), BuildFixtures.baselineFixtureArguments("-V"),
                                              work.resolve("baseline.log"));
        assertEquals(0, baseline.exitCode(), baseline::describe);
        List<String> baselineLog = baseline.readLog();
        assertRanOn(jdk, baselineLog, baseline);
        assertNoGradle(baseline, baselineLog);
        List<String> baselineOrder = MojoExecution.parse(baselineLog).stream().map(MojoExecution::key).toList();
        assertEquals(BuildAudit.BUILD_POLICY_EXECUTION, baselineOrder.get(0), baseline::describe);
        assertTrue(baselineOrder.indexOf("compiler:compile (default-compile)")
                   < baselineOrder.indexOf("jar:jar (default-jar)"), baseline::describe);
        Path classes = BuildFixtures.baselineFixture(checkout).resolve("target/classes");
        List<String> problems = new ArrayList<>();
        for (String classFile : BASELINE_CLASSES) {
            Path file = classes.resolve(classFile);
            if (!Files.isRegularFile(file)) {
                problems.add(classFile + " was not compiled");
            } else if (ReactorBuildVerifier.majorVersion(file) != ReactorBuildVerifier.RELEASE_21_MAJOR_VERSION) {
                problems.add(classFile + " has class-file major version " + ReactorBuildVerifier.majorVersion(file));
            }
        }
        assertEquals(List.of(), problems);
        assertTrue(Files.isRegularFile(BuildFixtures.baselineFixture(checkout)
                                                    .resolve("target/jdk-baseline-fixture-3.0.0-SNAPSHOT.jar")));
    }

    private static void rejectedBeforeCompilation(Jdk jdk, String label, int expectedMinimum, String... extraArguments)
        throws Exception {
        Path work = WORK.resolve("rejected-jdk-" + jdk.feature() + "-" + label);
        Path checkout = BuildFixtures.checkout(work);

        MavenFork.Result reactor = MAVEN.run(checkout, jdk.home(), ReleaseReactorCommand.argumentsWith(
            prepend("-V", extraArguments)), work.resolve("release-reactor.log"));
        List<String> reactorLog = reactor.readLog();
        assertNotEquals(0, reactor.exitCode(), reactor::describe);
        assertRanOn(jdk, reactorLog, reactor);
        assertEquals(List.of(), BuildAudit.rejectedBeforeCompilation(reactorLog, checkout, expectedMinimum),
                     reactor::describe);
        assertNoGradle(reactor, reactorLog);

        MavenFork.Result baseline = MAVEN.run(checkout, jdk.home(), BuildFixtures.baselineFixtureArguments(
            prepend("-V", extraArguments)), work.resolve("baseline.log"));
        List<String> baselineLog = baseline.readLog();
        assertNotEquals(0, baseline.exitCode(), baseline::describe);
        assertEquals(List.of(), BuildAudit.rejectedBeforeCompilation(baselineLog, checkout, expectedMinimum),
                     baseline::describe);
        assertTrue(baselineLog.stream().anyMatch(l -> l.contains("on project jdk-baseline-fixture")),
                   baseline::describe);
        assertNoGradle(baseline, baselineLog);
    }

    private static Jdk jdk21() {
        return JDKS.feature(MINIMUM_JDK).orElseThrow(() -> new AssertionError(
            "The JDK " + MINIMUM_JDK + " fixtures must always run, but no JDK " + MINIMUM_JDK + " was found: "
            + JDKS.describe()));
    }

    /** {@code -V} makes Maven print the JDK it runs on; this proves JAVA_HOME selected the intended JDK. */
    private static void assertRanOn(Jdk jdk, List<String> log, MavenFork.Result result) {
        assertTrue(log.stream().anyMatch(l -> l.startsWith("Java version: " + jdk.version() + ",")),
                   () -> "expected the build to run on " + jdk + "\n" + result.describe());
    }

    private static void assertNoGradle(MavenFork.Result result, List<String> log) {
        List<String> problems = new ArrayList<>(BuildAudit.gradleInBuildLog(log));
        problems.addAll(BuildAudit.gradleProcesses(result.descendantCommandLines()));
        assertEquals(List.of(), problems);
    }

    private static String[] prepend(String first, String... rest) {
        return Stream.concat(Stream.of(first), Stream.of(rest)).toArray(String[]::new);
    }
}
