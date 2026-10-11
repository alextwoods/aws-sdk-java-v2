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

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestTag;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * A JUnit Platform {@link TestExecutionListener} that counts passed, failed and skipped protocol tests per protocol and
 * prints the counts when the test plan finishes.
 *
 * <p>A test is attributed to the protocol in its {@code @Tag("protocol:<protocol ShapeId>")} tag, which the generator puts
 * on every generated test class (method tags inherit class tags). Tests without such a tag are not counted. Each test is
 * counted exactly once: a successful test as passed, a failed test as failed, and a disabled or aborted test (for example
 * a skip list entry rendered as {@code @Disabled}) as skipped. When a whole container is skipped, each test in it is
 * counted as skipped.
 *
 * <p>Register it through {@code META-INF/services/org.junit.platform.launcher.TestExecutionListener} in the module that
 * runs the generated tests. Output goes to the {@code software.amazon.awssdk.protocol.smithytests} logger at INFO level.
 */
public final class ProtocolTestSummaryExtension implements TestExecutionListener {
    /** Prefix of the tag that names a test's protocol. */
    public static final String PROTOCOL_TAG_PREFIX = "protocol:";

    private static final Logger log = Logger.loggerFor(ProtocolTestSummaryExtension.class);

    private final ConcurrentMap<String, Counts> counts = new ConcurrentHashMap<>();
    private final Consumer<String> output;
    private volatile TestPlan testPlan;

    /**
     * The constructor used by {@link java.util.ServiceLoader}; the summary goes to the logger.
     */
    public ProtocolTestSummaryExtension() {
        this(line -> log.info(() -> line));
    }

    ProtocolTestSummaryExtension(Consumer<String> output) {
        this.output = Validate.paramNotNull(output, "output");
    }

    @Override
    public void testPlanExecutionStarted(TestPlan plan) {
        this.testPlan = plan;
        counts.clear();
    }

    @Override
    public void executionSkipped(TestIdentifier identifier, String reason) {
        if (identifier.isTest()) {
            record(identifier, Outcome.SKIPPED);
            return;
        }
        // Children of a skipped container are not reported individually.
        TestPlan plan = testPlan;
        if (plan != null) {
            for (TestIdentifier descendant : plan.getDescendants(identifier)) {
                if (descendant.isTest()) {
                    record(descendant, Outcome.SKIPPED);
                }
            }
        }
    }

    @Override
    public void executionFinished(TestIdentifier identifier, TestExecutionResult result) {
        if (!identifier.isTest()) {
            return;
        }
        switch (result.getStatus()) {
            case SUCCESSFUL:
                record(identifier, Outcome.PASSED);
                break;
            case ABORTED:
                record(identifier, Outcome.SKIPPED);
                break;
            case FAILED:
            default:
                record(identifier, Outcome.FAILED);
                break;
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan plan) {
        Map<String, ProtocolCounts> snapshot = summary();
        if (snapshot.isEmpty()) {
            return;
        }
        output.accept("Protocol test summary:");
        snapshot.forEach((protocol, c) -> output.accept("  " + protocol + ": " + c));
    }

    /**
     * A snapshot of the counts recorded in the current test plan, keyed by protocol ShapeId in sorted order.
     */
    public Map<String, ProtocolCounts> summary() {
        Map<String, ProtocolCounts> result = new TreeMap<>();
        counts.forEach((protocol, c) -> result.put(protocol, c.snapshot()));
        return Collections.unmodifiableMap(result);
    }

    /**
     * Immutable pass, fail and skip counts for one protocol.
     */
    public static final class ProtocolCounts {
        private final int passed;
        private final int failed;
        private final int skipped;

        private ProtocolCounts(int passed, int failed, int skipped) {
            this.passed = passed;
            this.failed = failed;
            this.skipped = skipped;
        }

        public int passed() {
            return passed;
        }

        public int failed() {
            return failed;
        }

        public int skipped() {
            return skipped;
        }

        public int total() {
            return passed + failed + skipped;
        }

        @Override
        public String toString() {
            return passed + " passed, " + failed + " failed, " + skipped + " skipped (" + total() + " total)";
        }
    }

    private void record(TestIdentifier identifier, Outcome outcome) {
        protocolOf(identifier).ifPresent(protocol -> counts.computeIfAbsent(protocol, p -> new Counts()).add(outcome));
    }

    /**
     * The protocol named by the identifier's {@code protocol:} tag. With more than one such tag, the first in sorted order
     * is used so that a test is still counted once.
     */
    static Optional<String> protocolOf(TestIdentifier identifier) {
        return identifier.getTags().stream()
                         .map(TestTag::getName)
                         .filter(name -> name.startsWith(PROTOCOL_TAG_PREFIX)
                                         && name.length() > PROTOCOL_TAG_PREFIX.length())
                         .map(name -> name.substring(PROTOCOL_TAG_PREFIX.length()))
                         .sorted()
                         .findFirst();
    }

    private enum Outcome {
        PASSED, FAILED, SKIPPED
    }

    private static final class Counts {
        private final AtomicInteger passed = new AtomicInteger();
        private final AtomicInteger failed = new AtomicInteger();
        private final AtomicInteger skipped = new AtomicInteger();

        void add(Outcome outcome) {
            switch (outcome) {
                case PASSED:
                    passed.incrementAndGet();
                    break;
                case FAILED:
                    failed.incrementAndGet();
                    break;
                case SKIPPED:
                default:
                    skipped.incrementAndGet();
                    break;
            }
        }

        ProtocolCounts snapshot() {
            return new ProtocolCounts(passed.get(), failed.get(), skipped.get());
        }
    }
}
