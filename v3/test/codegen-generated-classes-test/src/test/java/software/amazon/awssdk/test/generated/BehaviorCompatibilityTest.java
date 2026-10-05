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

package software.amazon.awssdk.test.generated;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import software.amazon.awssdk.test.generated.support.CompatibilityFixtures;
import software.amazon.awssdk.test.generated.support.CompatibilityFixtures.Fixture;

/**
 * Runs the evidence tests of every generated behavior fixture against the generated V3 classes (Requirement 19.12;
 * {@code compatibility-fixtures.json}): for a behavior- or configuration-preserved entry, and for the verified
 * differences of an {@code ADAPTED} entry, each named evidence test must execute at least one test and none may fail
 * or be aborted, so a claim cannot rest on a test that is missing, renamed, empty, skipped or failing.
 *
 * <p>Evidence IDs are {@code TestClass} or {@code TestClass#method}, resolved in this package unless fully
 * qualified. Each evidence test runs once per suite run, through a nested JUnit Platform launcher.
 */
class BehaviorCompatibilityTest {
    private static final Map<String, TestExecutionSummary> RESULTS = new HashMap<>();

    static Stream<Fixture> behaviorFixtures() {
        return CompatibilityFixtures.ofKinds("BEHAVIOR");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("behaviorFixtures")
    void everyEvidenceTestOfTheEntryPassesAgainstV3(Fixture fixture) {
        assertFalse(fixture.evidenceIds().isEmpty(), fixture.entryId() + " has a behavior fixture without evidence");
        for (String evidence : fixture.evidenceIds()) {
            TestExecutionSummary summary = RESULTS.computeIfAbsent(evidence, BehaviorCompatibilityTest::run);
            String subject = fixture.entryId() + " evidence " + evidence;
            assertNotEquals(0L, summary.getTestsSucceededCount(), subject + " ran no passing test");
            assertEquals(List.of(), summary.getFailures().stream()
                                           .map(f -> f.getTestIdentifier().getDisplayName() + ": " + f.getException())
                                           .toList(), subject + " failed");
            assertEquals(0L, summary.getTestsAbortedCount(), subject + " was aborted");
        }
    }

    private static TestExecutionSummary run(String evidence) {
        int hash = evidence.indexOf('#');
        String type = hash < 0 ? evidence : evidence.substring(0, hash);
        String className = type.contains(".") ? type : BehaviorCompatibilityTest.class.getPackageName() + "." + type;
        if (className.equals(BehaviorCompatibilityTest.class.getName())) {
            throw new AssertionError("evidence " + evidence + " names the behavior suite itself");
        }
        Class<?> testClass;
        try {
            testClass = Class.forName(className, false, BehaviorCompatibilityTest.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new AssertionError("evidence " + evidence + " is not a test class of this module", e);
        }
        DiscoverySelector selector = hash < 0 ? selectClass(testClass)
                                              : selectMethod(testClass, evidence.substring(hash + 1));
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.execute(LauncherDiscoveryRequestBuilder.request().selectors(selector).build(), listener);
        return listener.getSummary();
    }
}
