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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Requirement 1.3: from a clean checkout on JDK 21+, the single supported command compiles, tests, and packages every
 * code-generation, runtime, service, HTTP, high-level-library, migration, and test artifact.
 *
 * <p>The test materializes a clean checkout of {@code v3/} (git-visible files only, so no {@code target/} or generated
 * sources), runs {@link ReleaseReactorCommand} in it, and verifies the outcome against an inventory computed
 * independently from the checked-out POMs. It forks a full reactor build, so it runs only when
 * {@code v3.cleanCheckoutReactorTest=true} (set by {@code -Dv3.aggregate=release-evidence}), and never inside the build
 * it forks.
 */
@EnabledIfSystemProperty(named = "v3.cleanCheckoutReactorTest", matches = "true")
@DisabledIfEnvironmentVariable(named = ReleaseReactorCommand.NESTED_BUILD_ENVIRONMENT_VARIABLE, matches = ".+")
class CleanCheckoutReactorTest {

    @Test
    void singleSupportedCommandCompilesTestsAndPackagesEveryRequiredArtifact() throws Exception {
        assertTrue(Runtime.version().feature() >= 21, "the clean-checkout reactor test must run on JDK 21+");
        Path v3Root = Path.of(System.getProperty("v3.root")).toRealPath();
        Path work = Path.of(System.getProperty("v3.cleanCheckoutReactorTest.workDirectory"));
        Path checkout = work.resolve("checkout");

        CleanCheckout.create(v3Root.getParent(), v3Root.getFileName().toString(), checkout);
        Path checkoutV3 = checkout.resolve(v3Root.getFileName().toString());
        assertEquals(List.of(), buildOutputDirectories(checkoutV3), "a clean checkout contains no build output");

        ReactorInventory expected = ReactorInventory.load(checkoutV3);
        assertEquals(List.of(), RequiredArtifacts.missing(expected));

        MavenFork.Result result = ReleaseReactorCommand.run(
            checkout,
            Path.of(System.getProperty("java.home")),
            optionalPath("maven.home"),
            optionalPath("v3.localRepository"),
            Boolean.parseBoolean(System.getProperty("v3.offline", "false")),
            work.resolve("build.log"),
            Duration.ofMinutes(Long.getLong("v3.cleanCheckoutReactorTest.timeoutMinutes", 60)));

        List<String> log = Files.readAllLines(result.log());
        assertEquals(0, result.exitCode(), () -> String.join(" ", result.command()) + " failed; see " + result.log()
                                                 + "\n" + String.join("\n", log.subList(Math.max(0, log.size() - 60),
                                                                                         log.size())));
        assertEquals(List.of(), ReactorBuildVerifier.verify(expected, log));
        // Requirements 1.4 and 1.5: lifecycle and reactor ordering, and no Gradle output or process in the build.
        List<String> audit = new ArrayList<>(BuildAudit.lifecycleOrdering(expected, log));
        audit.addAll(BuildAudit.gradleInBuildLog(log));
        audit.addAll(BuildAudit.gradleProcesses(result.descendantCommandLines()));
        assertEquals(List.of(), audit);
    }

    private static List<Path> buildOutputDirectories(Path root) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isDirectory).filter(p -> p.getFileName().toString().equals("target")).toList();
        }
    }

    private static Path optionalPath(String property) {
        String value = System.getProperty(property);
        return value == null || value.isBlank() || value.startsWith("${") ? null : Path.of(value);
    }
}
