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

package software.amazon.awssdk.protocol.smithytests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import software.amazon.awssdk.protocol.smithytests.ProtocolTestSummaryExtension.ProtocolCounts;

/**
 * Runs small fixture classes through a JUnit Platform launcher with the listener attached. The fixtures are static nested
 * classes, which Surefire does not run on its own; their failing test also only fails while this test launches it.
 */
class ProtocolTestSummaryExtensionTest {
    private static final String REST_JSON = "aws.protocols#restJson1";
    private static final String RPCV2 = "smithy.protocols#rpcv2Cbor";

    private static volatile boolean launchedByTest;

    @Test
    void countsEachTestOncePerProtocol() {
        List<String> output = new ArrayList<>();
        ProtocolTestSummaryExtension listener = new ProtocolTestSummaryExtension(output::add);

        run(listener, RestJsonFixture.class, RpcV2Fixture.class, DisabledFixture.class, UntaggedFixture.class);

        Map<String, ProtocolCounts> summary = listener.summary();
        assertThat(summary).containsOnlyKeys(REST_JSON, RPCV2);

        ProtocolCounts restJson = summary.get(REST_JSON);
        assertThat(restJson.passed()).isEqualTo(1);
        assertThat(restJson.failed()).isEqualTo(1);
        assertThat(restJson.skipped()).isEqualTo(2);

        // One passing test, plus two tests in a disabled class, which JUnit does not report individually.
        ProtocolCounts rpcv2 = summary.get(RPCV2);
        assertThat(rpcv2.passed()).isEqualTo(1);
        assertThat(rpcv2.failed()).isZero();
        assertThat(rpcv2.skipped()).isEqualTo(2);
        assertThat(rpcv2.total()).isEqualTo(3);

        assertThat(output).containsExactly("Protocol test summary:",
                                           "  " + REST_JSON + ": 1 passed, 1 failed, 2 skipped (4 total)",
                                           "  " + RPCV2 + ": 1 passed, 0 failed, 2 skipped (3 total)");
    }

    @Test
    void printsNothingWithoutProtocolTests() {
        List<String> output = new ArrayList<>();
        ProtocolTestSummaryExtension listener = new ProtocolTestSummaryExtension(output::add);

        run(listener, UntaggedFixture.class);

        assertThat(listener.summary()).isEmpty();
        assertThat(output).isEmpty();
    }

    @Test
    void countsResetForEachTestPlan() {
        ProtocolTestSummaryExtension listener = new ProtocolTestSummaryExtension(line -> { });
        run(listener, RpcV2Fixture.class);
        run(listener, RpcV2Fixture.class);
        assertThat(listener.summary().get(RPCV2).passed()).isEqualTo(1);
    }

    @Test
    void defaultConstructorIsUsableByServiceLoader() {
        new ProtocolTestSummaryExtension().testPlanExecutionFinished(null);
    }

    private static void run(ProtocolTestSummaryExtension listener, Class<?>... fixtures) {
        LauncherDiscoveryRequestBuilder builder = LauncherDiscoveryRequestBuilder.request();
        for (Class<?> fixture : fixtures) {
            builder.selectors(selectClass(fixture));
        }
        LauncherDiscoveryRequest request = builder.build();
        Launcher launcher = LauncherFactory.create();
        launchedByTest = true;
        try {
            launcher.execute(request, listener);
        } finally {
            launchedByTest = false;
        }
    }

    @Tag("protocol:" + REST_JSON)
    static class RestJsonFixture {
        @Test
        void passes() {
        }

        @Test
        void fails() {
            if (launchedByTest) {
                fail("expected failure");
            }
        }

        @Test
        @Disabled("skip list entry")
        void disabled() {
        }

        @Test
        void aborted() {
            Assumptions.assumeTrue(!launchedByTest);
        }
    }

    @Tag("protocol:" + RPCV2)
    static class RpcV2Fixture {
        @Test
        void passes() {
        }
    }

    @Tag("protocol:" + RPCV2)
    @Disabled("whole class skipped")
    static class DisabledFixture {
        @Test
        void first() {
        }

        @Test
        void second() {
        }
    }

    @Tag("other")
    static class UntaggedFixture {
        @Test
        void passes() {
        }
    }
}
