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

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.test.reactor.CoreRuntimeSuiteReports.Suite;
import software.amazon.awssdk.test.reactor.ReleaseGateEnforcement.Approval;
import software.amazon.awssdk.test.reactor.ReleaseGateInventory.DeploymentMode;
import software.amazon.awssdk.test.reactor.ReleaseGateInventory.ExpectedSuite;
import software.amazon.awssdk.test.reactor.ReleaseGateInventory.Inventory;

/**
 * Static checks (every build) of the release-gate inventory generated from the real checkout: its sources are
 * consistent, every expected suite is required by a suite gate (so the product-parity profile builds its module and
 * orders it before the gate), and the omission record is well formed and names expected suites only.
 */
class ReleaseGateInventoryTest {
    private static final Path V3_ROOT = Path.of(System.getProperty("v3.root")).toAbsolutePath().normalize();
    private static Inventory inventory;

    @BeforeAll
    static void generate() {
        inventory = ReleaseGateInventory.generate(V3_ROOT);
    }

    @Test
    void theInventorySourcesAreConsistent() {
        assertEquals(List.of(), inventory.problems());
    }

    @Test
    void theInventoryIsDerivedFromEverySource() {
        assertEquals(7, inventory.launchProtocols().size(), inventory.launchProtocols()::toString);
        assertTrue(inventory.services().size() > 400, "service-fleet.json services: " + inventory.services().size());
        assertTrue(inventory.transports().size() >= 5, inventory.transports()::toString);
        for (DeploymentMode mode : DeploymentMode.values()) {
            assertTrue(inventory.suites().stream().anyMatch(s -> s.mode() == mode), mode::id);
        }
        ExpectedSuite fidelity = inventory.suite(ReleaseGateInventory.SCHEMA_FIDELITY_SUITE).orElseThrow();
        assertEquals(inventory.services().size(), fidelity.cases().size());
        ExpectedSuite endpoints = inventory.suite(ReleaseGateInventory.ENDPOINT_SUITE).orElseThrow();
        assertEquals(inventory.services().size(), endpoints.cases().size());
        for (String protocol : inventory.launchProtocols()) {
            assertTrue(inventory.suite(ReleaseGateInventory.PROTOCOL_CONFORMANCE_SUITE).orElseThrow().sources()
                                .contains("protocol:" + protocol), protocol);
        }
    }

    /**
     * Every expected release-gate suite is listed in core-runtime-suites.txt or product-parity-suites.txt, so
     * ReactorInventoryTest's checks (the product-parity profile builds the module, and the gate profile orders it
     * before the gate) cover it as well.
     */
    @Test
    void everyExpectedSuiteIsRequiredByASuiteGate() {
        Set<Suite> listed = new HashSet<>(CoreRuntimeSuiteReportsTest.requiredSuites());
        listed.addAll(ProductParitySuiteReportsTest.requiredSuites());
        Set<String> listedModules = listed.stream().map(Suite::module).collect(Collectors.toSet());
        // The native-image suite is the one exception: it skips (approved) unless native image is required, so the
        // suite-level gates, which require executed tests, cannot list it; its module must still be ordered.
        List<String> unlisted = inventory.suites().stream()
                                         .filter(s -> s.mode() == DeploymentMode.NATIVE_IMAGE
                                                      ? !listedModules.contains(s.suite().module())
                                                      : !listed.contains(s.suite()))
                                         .map(s -> s.suite().toString()).sorted().toList();
        assertEquals(List.of(), unlisted, "add these suites to product-parity-suites.txt");
    }

    @Test
    void everyApprovedOmissionIsWellFormedAndNamesAnExpectedSuite() {
        ReleaseGateEnforcement.Approvals approvals = ReleaseGateReportsTest.approvals();
        assertEquals(List.of(), approvals.problems());
        Set<String> expected = inventory.suites().stream().map(ExpectedSuite::className).collect(Collectors.toSet());
        assertEquals(List.of(), approvals.approvals().stream().map(Approval::suite)
                                         .filter(s -> !expected.contains(s)).toList());
    }
}
