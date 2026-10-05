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

package software.amazon.awssdk.benchmark.dynamodb;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Pre-timing verification, the V3 counterpart of racecar's {@code PipelineCheck} and {@code ResponseDigest}. An arm is
 * never measured unless all of this holds:
 *
 * <ol>
 *   <li><b>Transport identity.</b> The constructed client's class, {@code clientName()} and
 *       {@code capabilities().mode()} are the ones the arm names.</li>
 *   <li><b>Server reached.</b> One call per scenario raises the server's request count by exactly one.</li>
 *   <li><b>Wire bytes.</b> The {@code X-Amz-Target}, {@code Content-Type} and body bytes the server received equal the
 *       independently written expected wire JSON, byte for byte.</li>
 *   <li><b>Response.</b> The deserialized response renders to {@link ResponseDigest#expected(Scenario)}; its SHA-256
 *       is printed so digests can also be compared across arms and JVMs ({@code verify.sh}).</li>
 * </ol>
 *
 * <p>There is no pipeline to identify yet (V3 has no client engine), so racecar's stack-frame probe has no V3
 * counterpart; the transport identity check is what prevents measuring the wrong arm.
 */
final class TransportCheck {

    private TransportCheck() {
    }

    /** Thrown when an arm fails verification; never caught by the runner. */
    static final class VerificationException extends IllegalStateException {
        private static final long serialVersionUID = 1L;

        VerificationException(String message) {
            super(message);
        }
    }

    static void requireTransport(V3Workload workload) {
        Arm arm = workload.arm;
        Object transport = workload.transport;
        if (transport.getClass() != arm.expectedClass) {
            throw new VerificationException("arm " + arm.cliName + " expects transport " + arm.expectedClass.getName()
                                            + " but the constructed client is " + transport.getClass().getName());
        }
        if (!arm.expectedClientName.equals(workload.clientName())) {
            throw new VerificationException("arm " + arm.cliName + " expects clientName() " + arm.expectedClientName
                                            + " but the transport reports " + workload.clientName());
        }
        if (workload.capabilityMode() != arm.mode) {
            throw new VerificationException("arm " + arm.cliName + " expects a " + arm.mode
                                            + " transport but capabilities().mode() is " + workload.capabilityMode());
        }
    }

    /**
     * Execution probe, the counterpart of racecar's stack-capturing credentials provider: one GetItem whose body is a
     * stream that records the call stack when the transport first reads it. The arm's V3 transport package must be on
     * that stack, which proves the call actually went through the named transport rather than merely that a client
     * of the right class was constructed. Not on the measured path: it runs once, before warmup.
     */
    static String requireExecution(V3Workload workload) {
        String expectedPackage = workload.arm.expectedClass.getPackageName() + ".";
        java.util.List<String> frames = workload.probeFrames(Scenario.SMALL_GET);
        if (frames.isEmpty()) {
            throw new VerificationException("arm " + workload.arm.cliName + ": the execution probe's request body was"
                                            + " never read, so the executing transport could not be identified");
        }
        String hit = frames.stream().filter(f -> f.startsWith(expectedPackage)).findFirst().orElse(null);
        if (hit == null) {
            throw new VerificationException("arm " + workload.arm.cliName + ": no " + expectedPackage + " frame on the"
                                            + " stack that read the request body; frames were " + frames);
        }
        return hit;
    }

    /** Runs every check for the given scenarios and returns each scenario's response digest (SHA-256). */
    static Map<Scenario, String> verify(V3Workload workload, List<Scenario> scenarios) throws Exception {
        requireTransport(workload);
        String executionFrame = requireExecution(workload);
        System.out.printf("=== execution verified: arm=%s body read from %s%n", workload.arm.cliName, executionFrame);
        Map<Scenario, String> digests = new EnumMap<>(Scenario.class);
        for (Scenario scenario : scenarios) {
            ServerStats before = ServerStats.fetch(workload.endpoint);
            if (before.unavailable) {
                throw new VerificationException("server /stats unavailable at " + workload.endpoint
                                                + "; verification needs the V3 MockDdbServer");
            }
            Object output = workload.call(scenario);
            ServerStats delta = ServerStats.fetch(workload.endpoint).minus(before);
            if (delta.requests != 1 || delta.errors != 0) {
                throw new VerificationException(scenario.cliName + ": expected the server to serve exactly 1 request,"
                                                + " saw requests=" + delta.requests + " errors=" + delta.errors);
            }
            requireWire(workload, scenario, DynamoDbItems.utf8(scenario.expectedRequestJson()));
            digests.put(scenario, requireResponse(scenario, output));
        }
        System.out.printf("=== transport verified: arm=%s class=%s clientName=%s mode=%s scope=%s scenarios=%s%n",
                          workload.arm.cliName, workload.transport.getClass().getSimpleName(), workload.clientName(),
                          workload.capabilityMode(), Arm.SCOPE, digests.keySet());
        return digests;
    }

    static void requireWire(V3Workload workload, Scenario scenario, byte[] expected) throws Exception {
        HttpResponse<byte[]> last = ServerStats.last(workload.endpoint, scenario.target());
        if (last == null) {
            throw new VerificationException(scenario.cliName + ": the server received no request with X-Amz-Target "
                                            + scenario.target());
        }
        String contentType = last.headers().firstValue("x-last-content-type").orElse("");
        if (!MockDdbServer.CONTENT_TYPE.equals(contentType)) {
            throw new VerificationException(scenario.cliName + ": request Content-Type was '" + contentType + "'");
        }
        if (!Arrays.equals(expected, last.body())) {
            throw new VerificationException(scenario.cliName + ": request body bytes differ from the expected wire"
                                            + " JSON (" + last.body().length + " B received, " + expected.length
                                            + " B expected)\n  received: " + abbreviate(last.body())
                                            + "\n  expected: " + abbreviate(expected));
        }
    }

    static String requireResponse(Scenario scenario, Object output) {
        String actual = ResponseDigest.render(scenario, output);
        String expected = ResponseDigest.expected(scenario);
        if (!expected.equals(actual)) {
            throw new VerificationException(scenario.cliName + ": deserialized response differs from the canned"
                                            + " response's model objects\n  actual:   " + abbreviate(actual)
                                            + "\n  expected: " + abbreviate(expected));
        }
        return ResponseDigest.sha256(actual);
    }

    private static String abbreviate(byte[] bytes) {
        return abbreviate(new String(bytes, StandardCharsets.UTF_8));
    }

    private static String abbreviate(String s) {
        return s.length() <= 300 ? s : s.substring(0, 300) + "...";
    }
}
