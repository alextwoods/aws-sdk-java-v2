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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import software.amazon.awssdk.test.reactor.ReleaseGateEnforcement.Approvals;
import software.amazon.awssdk.test.reactor.ReleaseGateEnforcement.Finding;
import software.amazon.awssdk.test.reactor.ReleaseGateInventory.DeploymentModes;
import software.amazon.awssdk.test.reactor.ReleaseGateInventory.ExpectedSuite;
import software.amazon.awssdk.test.reactor.ReleaseGateInventory.Inventory;

/**
 * The case-level release gate (task 18.10; Requirements 16.14, 16.15, 19.14, 19.15): every case of the generated
 * release-gate inventory ran in this Maven session and passed within its timeout, and every skipped case is covered by
 * an approved omission of {@code release-gate-omissions.txt}. Enabled by the product-parity-gate profile
 * ({@code -Dv3.aggregate=product-parity}), which also runs the suite-level gates and orders every suite module before
 * this one; skipped in every other build. Writes the inventory and the findings to
 * {@code target/release-gate/} as release evidence.
 */
@EnabledIfSystemProperty(named = "v3.releaseGate", matches = "true")
class ReleaseGateReportsTest {
    static final String OMISSIONS = "release-gate-omissions.txt";

    @Test
    void everyReleaseGateCaseRanAndPassedWithinItsTimeoutWithoutAnUnapprovedSkip() throws IOException {
        Path v3Root = Path.of(System.getProperty("v3.root")).toAbsolutePath().normalize();
        Instant buildStart = CoreRuntimeSuiteReports.parseBuildStart(System.getProperty("v3.buildStartTime"));
        DeploymentModes modes = DeploymentModes.of(System.getProperty("v3.nativeImage.required"),
                                                   System.getProperty("v3.launchGate"));
        Inventory inventory = ReleaseGateInventory.generate(v3Root);
        List<Finding> findings = ReleaseGateEnforcement.check(v3Root, inventory, approvals(), modes, buildStart);
        writeEvidence(inventory, findings, modes);
        assertEquals(List.of(), findings, () -> ReleaseGateEnforcement.summarize(findings, 5));
    }

    /** The checked-in release-gate omission record. */
    static Approvals approvals() {
        URL resource = ReleaseGateReportsTest.class.getClassLoader().getResource(OMISSIONS);
        if (resource == null) {
            throw new IllegalStateException(OMISSIONS + " is not on the test classpath");
        }
        try {
            return ReleaseGateEnforcement.parseApprovals(Files.readAllLines(Path.of(resource.toURI())), OMISSIONS);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void writeEvidence(Inventory inventory, List<Finding> findings, DeploymentModes modes)
        throws IOException {
        String directory = System.getProperty("v3.releaseGate.evidenceDirectory");
        if (directory == null || directory.isBlank()) {
            return;
        }
        Path out = Path.of(directory);
        Files.createDirectories(out);
        List<String> lines = new ArrayList<>();
        lines.add("# Release-gate inventory: " + inventory.suites().size() + " suites, " + inventory.caseCount()
                  + " named cases; launch protocols " + inventory.launchProtocols() + "; "
                  + inventory.services().size() + " services; transports " + inventory.transports() + "; " + modes);
        for (ExpectedSuite suite : inventory.suites()) {
            lines.add(suite.suite() + " | " + suite.category() + " | " + suite.mode().id() + " | case<="
                      + suite.caseTimeout() + " suite<=" + suite.suiteTimeout() + " | " + suite.cases().size()
                      + " named cases | " + String.join(" ", suite.sources()));
        }
        Files.write(out.resolve("release-gate-inventory.txt"), lines);
        Files.write(out.resolve("release-gate-findings.txt"), findings.stream().map(Finding::toString).toList());
    }
}
