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

package software.amazon.awssdk.test.differential;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import software.amazon.awssdk.test.differential.Calls.Trace;
import software.amazon.awssdk.test.differential.Matrix.CallShape;
import software.amazon.awssdk.test.differential.Matrix.Cell;
import software.amazon.awssdk.test.differential.Matrix.Mode;
import software.amazon.awssdk.test.differential.Matrix.Scenario;

/**
 * The generated-versus-dynamic differential release matrix (Requirements 14.16, 19.6, 19.14).
 *
 * <p>For every launch protocol ({@code awsJson1_0}, {@code awsJson1_1}, {@code rpcv2Cbor}, {@code awsQuery},
 * {@code ec2Query}, {@code restJson1}, {@code restXml}), call shape ({@link CallShape}: finite success, modeled error,
 * unmodeled error, streaming blob input and output, input and output event streams, every page of a paginated
 * operation, a waiter) and execution mode (sync, async), the same deterministic call runs through a generated client
 * (the reference) and the Dynamic Client built from the same model AST, over the same scripted transport, with the
 * configuration and fixed nondeterministic metadata described in {@link Calls}. The scenarios come from synthetic
 * services ({@link SyntheticCells}, which cover every supported cell) and representative fleet services
 * ({@link FleetCells}, which cover every cell a deterministically selected real service has).
 *
 * <p>Oracle, byte equality against the generated client: every transmitted request's method, scheme, authority, raw
 * path and query, its complete ordered header list (including {@code Authorization} and {@code X-Amz-Date}), its
 * body kind and bytes, and for an event stream body every decoded frame with its headers, whether its signature
 * chains, and each wrapped event's headers and payload; then the modeled result (the output document; the response
 * and payload bytes of a streaming output; every page; the waiter's attempts and matched result; the event stream
 * handler's callbacks and every received event), or the failure with its status, error code, message, request ID,
 * attempts, retryability and modeled error document. Nothing is excluded from the comparison. Each reference run must
 * also send the scenario's exact number of requests and reach its expected outcome (and event count), so an
 * equal-but-broken pair (for example two identical deserialization failures) fails the cell instead of passing it.
 *
 * <p>A cell that no generated client can call is listed in {@link Matrix#UNSUPPORTED} with its justification;
 * {@link #everyCellIsRunOrExplicitlyUnsupported} fails if any cell is neither run nor listed, or both. The matrix,
 * each cell's scenarios and results, and the unsupported list are written to {@code target/differential-matrix.txt}.
 */
class GeneratedDynamicDifferentialMatrixTest {
    private static final Map<String, String> RESULTS = new LinkedHashMap<>();

    static List<Scenario> scenarios() {
        List<Scenario> out = new ArrayList<>(SyntheticCells.scenarios());
        out.addAll(FleetCells.scenarios());
        return out;
    }

    @TestFactory
    Stream<DynamicTest> generatedAndDynamicCallsAreWireEquivalent() {
        List<DynamicTest> tests = new ArrayList<>();
        for (Scenario scenario : scenarios()) {
            for (Mode mode : scenario.modes()) {
                Cell cell = new Cell(scenario.protocol(), scenario.shape(), mode);
                String name = cell + " | " + scenario;
                tests.add(DynamicTest.dynamicTest(name, () -> {
                    try {
                        runCell(scenario, mode);
                        record(name, "passed");
                    } catch (AssertionError | Exception e) {
                        record(name, "FAILED " + firstLine(e.getMessage()));
                        throw e;
                    }
                }));
            }
        }
        return tests.stream();
    }

    private static void runCell(Scenario scenario, Mode mode) throws Exception {
        Trace reference = Calls.run(scenario, mode, false);
        Trace dynamic = Calls.run(scenario, mode, true);
        String expected = reference.render();
        String context = scenario + " " + mode;
        String traces = System.getProperty("v3.differential.traces");
        if (traces != null) {
            // Opt-in evidence: the reference trace of every cell, one file per scenario and mode.
            Path file = Path.of(traces, (context).replaceAll("[^A-Za-z0-9._-]+", "_") + ".txt");
            Files.createDirectories(file.getParent());
            Files.writeString(file, expected + "\n\n== dynamic ==\n" + dynamic.render(), StandardCharsets.UTF_8);
        }
        assertEquals(scenario.requests(), reference.requests.size(),
                     "the generated client sent an unexpected number of requests for " + context + ":\n" + expected);
        assertOutcome(scenario, reference, expected);
        if (scenario.shape() == CallShape.EVENT_STREAM_OUTPUT) {
            assertEquals(scenario.events(), reference.handlerEvents(),
                         "the generated client received an unexpected number of events for " + context + ":\n"
                         + expected);
        }
        assertFalse(expected.contains("INVALID"), "the generated client's frames are not chain-signed for " + context
                                                  + ":\n" + expected);
        assertEquals(expected, dynamic.render(),
                     "the Dynamic Client's wire or result differs from the generated client's for " + context);
    }

    private static void assertOutcome(Scenario scenario, Trace reference, String rendered) {
        String outcome = reference.outcome;
        boolean matches = switch (scenario.expected()) {
            case SUCCESS -> outcome.startsWith("success ") || outcome.startsWith("pages=")
                            || outcome.equals("complete")
                            || (outcome.startsWith("waiter ") && outcome.contains(" response "));
            case MODELED_ERROR -> outcome.startsWith("failure ") && outcome.contains(", modeled=");
            case UNMODELED_ERROR -> outcome.startsWith("failure ") && outcome.contains(", unmodeled");
        };
        assertTrue(matches, "the generated client's outcome is not " + scenario.expected() + " for " + scenario
                            + ":\n" + rendered);
        if (scenario.shape() == CallShape.PAGINATED) {
            assertTrue(outcome.startsWith("pages=" + scenario.requests()), "expected every page:\n" + rendered);
        }
        if (scenario.shape() == CallShape.WAITER) {
            assertTrue(outcome.startsWith("waiter attempts=" + scenario.requests() + " "),
                       "expected one poll per scripted reply:\n" + rendered);
        }
    }

    /**
     * Every cell of the protocol x shape x mode product is run by at least one scenario or is explicitly unsupported
     * with a justification, never both, and every launch protocol is represented by a fleet service.
     */
    @Test
    void everyCellIsRunOrExplicitlyUnsupported() {
        Map<Cell, List<String>> covered = coverage();
        List<String> problems = new ArrayList<>();
        for (Cell cell : Matrix.allCells()) {
            boolean run = covered.containsKey(cell);
            boolean unsupported = Matrix.UNSUPPORTED.containsKey(cell);
            if (run == unsupported) {
                problems.add(cell + (run ? " is run but also listed as unsupported" : " is neither run nor listed "
                                                                                       + "as unsupported"));
            }
            if (run && covered.get(cell).stream().noneMatch(s -> s.startsWith("synthetic:"))) {
                problems.add(cell + " has no synthetic scenario");
            }
        }
        Set<Matrix.Protocol> fleetProtocols = new TreeSet<>();
        for (Scenario scenario : FleetCells.scenarios()) {
            fleetProtocols.add(scenario.protocol());
        }
        for (Matrix.Protocol protocol : Matrix.Protocol.values()) {
            if (!fleetProtocols.contains(protocol)) {
                problems.add(protocol + " has no fleet service scenario");
            }
        }
        assertEquals(List.of(), problems);
    }

    /**
     * The justification of the sync event stream cells: no generated sync client exposes an event stream operation
     * that its async counterpart has.
     */
    @Test
    void generatedSyncClientsExposeNoEventStreamOperation() throws Exception {
        List<String> exposed = new ArrayList<>();
        for (Scenario scenario : scenarios()) {
            if (!scenario.shape().eventStream()) {
                continue;
            }
            String name = Calls.lowerCamel(scenario.operation());
            boolean async = false;
            for (Method method : Calls.clientInterface(scenario, Mode.ASYNC).getMethods()) {
                async |= method.getName().equals(name);
            }
            assertTrue(async, scenario + ": the async client has no " + name);
            for (Method method : Calls.clientInterface(scenario, Mode.SYNC).getMethods()) {
                if (method.getName().equals(name)) {
                    exposed.add(scenario + ": " + method);
                }
            }
        }
        assertEquals(List.of(), exposed);
    }

    /** The scenarios that run each cell. */
    private static Map<Cell, List<String>> coverage() {
        Map<Cell, List<String>> covered = new LinkedHashMap<>();
        for (Scenario scenario : scenarios()) {
            for (Mode mode : scenario.modes()) {
                covered.computeIfAbsent(new Cell(scenario.protocol(), scenario.shape(), mode), c -> new ArrayList<>())
                       .add(scenario.toString());
            }
        }
        return covered;
    }

    private static synchronized void record(String name, String result) {
        RESULTS.put(name, result);
    }

    private static String firstLine(String message) {
        if (message == null) {
            return "";
        }
        int newline = message.indexOf('\n');
        return newline < 0 ? message : message.substring(0, newline);
    }

    /** Writes the matrix: every cell with its scenarios and results, then the unsupported cells and why. */
    @AfterAll
    static synchronized void writeReport() throws IOException {
        Path report = Path.of(System.getProperty("v3.differential.report", "target/differential-matrix.txt"));
        Map<Cell, List<String>> covered = coverage();
        StringBuilder out = new StringBuilder("# Generated-versus-dynamic differential matrix (task 18.4)\n");
        int run = 0;
        int passed = 0;
        for (Cell cell : Matrix.allCells()) {
            if (!covered.containsKey(cell)) {
                continue;
            }
            run++;
            boolean allPassed = true;
            StringBuilder lines = new StringBuilder();
            for (String scenario : covered.get(cell)) {
                String result = RESULTS.getOrDefault(cell + " | " + scenario, "not run");
                allPassed &= result.equals("passed");
                lines.append("    ").append(result).append("  ").append(scenario).append('\n');
            }
            passed += allPassed ? 1 : 0;
            out.append(allPassed ? "PASS " : "FAIL ").append(cell).append('\n').append(lines);
        }
        out.append("\n# Unsupported cells\n");
        Matrix.UNSUPPORTED.forEach((cell, reason) -> out.append("UNSUPPORTED ").append(cell).append(": ")
                                                        .append(reason).append('\n'));
        out.append("\ncells=").append(Matrix.allCells().size()).append(" run=").append(run).append(" passed=")
           .append(passed).append(" unsupported=").append(Matrix.UNSUPPORTED.size()).append(" scenarios=")
           .append(RESULTS.size()).append('\n');
        Files.createDirectories(report.toAbsolutePath().getParent());
        Files.writeString(report, out, StandardCharsets.UTF_8);
    }
}
