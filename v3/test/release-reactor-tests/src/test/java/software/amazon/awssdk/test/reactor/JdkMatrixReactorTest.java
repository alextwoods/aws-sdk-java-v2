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

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import software.amazon.awssdk.test.reactor.JdkInstallations.Jdk;

/**
 * Requirement 1.2 release evidence: the unmodified single supported command builds, tests, and packages the full
 * release reactor from a clean checkout on every discovered GA JDK newer than 21, with generation, compilation, tests
 * and packaging in order and no Gradle involvement. (JDK 21 itself is covered by {@link CleanCheckoutReactorTest}.)
 *
 * <p>It forks one full reactor build per JDK, so it runs only when {@code v3.jdkMatrixReactorTest=true} (set by
 * {@code -Dv3.aggregate=release-evidence}), and never inside a build forked by another reactor test:
 * <pre>{@code mvn -f v3/pom.xml verify -Dv3.aggregate=release-evidence -Dv3.jdkHomes=<jdk25-home>:<jdk26-home>}</pre>
 */
@EnabledIfSystemProperty(named = "v3.jdkMatrixReactorTest", matches = "true")
@DisabledIfEnvironmentVariable(named = ReleaseReactorCommand.NESTED_BUILD_ENVIRONMENT_VARIABLE, matches = ".+")
class JdkMatrixReactorTest {
    private static final int MINIMUM_JDK = 21;

    @TestFactory
    Stream<DynamicTest> releaseReactorBuildsOnEveryNewerJdkWithoutSourceChanges() {
        JdkInstallations jdks = JdkInstallations.discover();
        return BuildFixtures.perJdk(jdks, jdks.newerThan(MINIMUM_JDK),
                                    "No GA JDK newer than " + MINIMUM_JDK + " was found, so the release reactor JDK "
                                    + "matrix is skipped: ",
                                    JdkMatrixReactorTest::buildsReleaseReactor);
    }

    private static void buildsReleaseReactor(Jdk jdk) throws Exception {
        Path work = Path.of(System.getProperty("v3.jdkFailureModeTests.workDirectory"))
                        .resolve("release-reactor-jdk-" + jdk.feature());
        Path checkout = BuildFixtures.checkout(work);
        ReactorInventory expected = ReactorInventory.load(checkout.resolve("v3"));
        MavenFork maven = MavenFork.fromSystemProperties(
            Duration.ofMinutes(Long.getLong("v3.cleanCheckoutReactorTest.timeoutMinutes", 60)));
        MavenFork.Result result = maven.run(checkout, jdk.home(), ReleaseReactorCommand.ARGUMENTS,
                                            work.resolve("build.log"));
        assertEquals(0, result.exitCode(), result::describe);
        List<String> log = result.readLog();
        List<String> problems = new ArrayList<>(ReactorBuildVerifier.verify(expected, log));
        problems.addAll(BuildAudit.lifecycleOrdering(expected, log));
        problems.addAll(BuildAudit.gradleInBuildLog(log));
        problems.addAll(BuildAudit.gradleProcesses(result.descendantCommandLines()));
        assertEquals(List.of(), problems);
    }
}
