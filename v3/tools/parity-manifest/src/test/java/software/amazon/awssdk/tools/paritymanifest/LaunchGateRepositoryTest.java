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

package software.amazon.awssdk.tools.paritymanifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;

/**
 * The in-build launch gate (task 13.3): the V3 tree's release candidate against the Parity Manifest of the V2
 * baseline in this repository. Enabled by {@code -Dv3.launchGate=packaging|general-availability} (the
 * {@code launch-gate} profile of this module's POM), and skipped otherwise; when enabled, any blocking problem fails
 * this module's build in the test phase.
 *
 * <p>It runs the two release CLIs exactly as a release pipeline would: {@link ParityManifestCli} generates the
 * manifest (every registered inventory source, the released V2 API jars of {@code v3.v2Baseline.version}, the
 * candidate identity), and {@link LaunchGateCli} gates the candidate tree ({@code --candidate-tree}: the compiled
 * output of every V3 BOM artifact) with the checked-in hook registry and the hooks of the generated compatibility
 * fixtures. Everything is written under {@code target/launch-gate}.
 */
class LaunchGateRepositoryTest {
    private static final Path V3 = Path.of("../..").toAbsolutePath().normalize();
    private static final Path REPOSITORY = V3.getParent();
    private static final Path OUTPUT = Path.of("target", "launch-gate").toAbsolutePath();

    @Test
    void releaseCandidatePassesTheLaunchGate() throws Exception {
        String stage = System.getProperty("v3.launchGate", "");
        assumeTrue(!stage.isBlank(), "launch gate disabled; enable with -Dv3.launchGate=packaging or "
                                     + "-Dv3.launchGate=general-availability");
        LaunchGate.Stage.fromCliName(stage);
        Path manifest = OUTPUT.resolve("parity-manifest.json");

        Output generation = new Output();
        List<String> manifestArgs = new ArrayList<>(List.of(
            "--repository", REPOSITORY.toString(), "--output", manifest.toString(),
            "--api-version", System.getProperty("v3.fixture.v2Version"),
            "--maven-repository", System.getProperty("v3.launchGate.mavenRepository"),
            "--candidate-version", System.getProperty("v3.launchGate.candidateVersion")));
        String revision = ParityManifestCli.gitHead(REPOSITORY);
        if (revision != null) {
            manifestArgs.addAll(List.of("--candidate-git-revision", revision));
        }
        int generated = ParityManifestCli.run(manifestArgs.toArray(String[]::new),
                                              ServiceLoader.load(InventorySource.class).stream()
                                                           .map(ServiceLoader.Provider::get).toList(),
                                              generation.out, generation.err);
        assertEquals(0, generated, "Parity Manifest generation failed:\n" + generation);

        List<String> gateArgs = new ArrayList<>(List.of(
            "--manifest", manifest.toString(), "--stage", stage, "--candidate-tree", V3.toString(),
            "--builtin-hooks", "--hooks", CompatibilityFixturesGoldenTest.HOOKS.toAbsolutePath().toString(),
            "--report", OUTPUT.resolve("launch-gate-report.json").toString(),
            "--report-text", OUTPUT.resolve("launch-gate-report.txt").toString()));
        String evidence = System.getProperty("v3.launchGate.evidence", "");
        if (!evidence.isBlank()) {
            gateArgs.addAll(List.of("--evidence", evidence));
        }
        Output gate = new Output();
        int status = LaunchGateCli.run(gateArgs.toArray(String[]::new), gate.out, gate.err);
        System.out.print(gate);

        // The high-level libraries' share of the problems (task 14.6): owned entries, and the known-gap entries of
        // other owners they depend on (high-level-library-matrix.json).
        Path report = OUTPUT.resolve("launch-gate-report.json");
        if (Files.isRegularFile(report)) {
            StringBuilder libraries = new StringBuilder("High-level library launch gate problems (" + stage + "):\n");
            HighLevelLibraryMatrix.loadDefault()
                                  .launchProblems(Node.parse(Files.readString(report, StandardCharsets.UTF_8))
                                                      .expectObjectNode())
                                  .forEach((library, codes) -> libraries.append("  ").append(library).append(": ")
                                                                        .append(codes.isEmpty() ? "none" : codes)
                                                                        .append('\n'));
            Files.writeString(OUTPUT.resolve("high-level-libraries.txt"), libraries, StandardCharsets.UTF_8);
            System.out.print(libraries);
        }
        assertEquals(0, status, "The launch gate blocks " + stage + " (all problems: "
                                + OUTPUT.resolve("launch-gate-report.txt") + "):\n" + gate);
    }

    /** Captured standard output and error of a CLI run. */
    private static final class Output {
        private final ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        private final ByteArrayOutputStream errBytes = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8);
        final PrintStream err = new PrintStream(errBytes, true, StandardCharsets.UTF_8);

        @Override
        public String toString() {
            return outBytes.toString(StandardCharsets.UTF_8) + errBytes.toString(StandardCharsets.UTF_8);
        }
    }
}
