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

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;

/**
 * Runs every verification in one JVM: all arms positive against the given (out-of-process) server with digests
 * compared across arms, then negative cases that must be rejected. Exits non-zero on any failure.
 *
 * <p>Usage: {@code VerifyAll [endpoint]}. Per-JVM digests (one JVM per arm) are compared by {@code verify.sh} from the
 * runner's {@code --verify-only} output.
 */
public final class VerifyAll {

    private VerifyAll() {
    }

    public static void main(String[] args) throws Exception {
        URI endpoint = URI.create(args.length > 0 ? args[0] : "http://127.0.0.1:" + MockDdbServer.DEFAULT_PORT);
        List<Scenario> all = List.of(Scenario.values());
        int failures = 0;

        Map<Scenario, String> reference = null;
        Arm referenceArm = null;
        for (Arm arm : Arm.values()) {
            try (V3Workload w = new V3Workload(arm, endpoint, 1)) {
                Map<Scenario, String> digests = TransportCheck.verify(w, all);
                if (reference == null) {
                    reference = digests;
                    referenceArm = arm;
                } else if (!reference.equals(digests)) {
                    System.out.printf("FAIL digest mismatch: %s=%s vs %s=%s%n", arm.cliName, digests,
                                      referenceArm.cliName, reference);
                    failures++;
                }
                System.out.printf("POSITIVE arm=%s ok digests=%s%n", arm.cliName, digests);
            } catch (Exception | Error e) {
                System.out.printf("FAIL arm=%s: %s%n", arm.cliName, e);
                failures++;
            }
        }
        System.out.printf("DIGESTS identical across %d arms: %s%n", Arm.values().length, failures == 0);

        failures += negatives(endpoint);
        System.out.println(failures == 0 ? "VERIFY PASSED" : "VERIFY FAILED failures=" + failures);
        System.exit(failures == 0 ? 0 : 1);
    }

    /** Each case must throw {@link TransportCheck.VerificationException}; returns the number that did not. */
    static int negatives(URI endpoint) throws Exception {
        int failures = 0;
        failures += expectRejected("wrong transport (apache5 arm, UrlConnection client)", () -> {
            try (V3Workload w = new V3Workload(Arm.V3_SYNC_APACHE5, endpoint, UrlConnectionHttpClient.create())) {
                TransportCheck.requireTransport(w);
            }
        });
        failures += expectRejected("wrong mode (sync-crt arm, async CRT client with the same clientName)", () -> {
            try (V3Workload w = new V3Workload(Arm.V3_SYNC_CRT, endpoint, AwsCrtAsyncHttpClient.create())) {
                TransportCheck.requireTransport(w);
            }
        });
        failures += expectRejected("wrong request body on the wire", () -> {
            try (V3Workload w = new V3Workload(Arm.V3_SYNC_APACHE5, endpoint, 1)) {
                w.call(Scenario.SMALL_PUT);
                TransportCheck.requireWire(w, Scenario.SMALL_PUT,
                                           DynamoDbItems.utf8(Scenario.SMALL_GET.expectedRequestJson()));
            }
        });

        Map<String, byte[]> tampered = MockDdbServer.cannedResponses();
        String get = MockDdbServer.TARGET_PREFIX + "GetItem";
        tampered.put(get, new String(tampered.get(get), StandardCharsets.UTF_8)
            .replace("test-value", "test-valuX").getBytes(StandardCharsets.UTF_8));
        String batch = MockDdbServer.TARGET_PREFIX + "BatchGetItem";
        tampered.put(batch, new String(tampered.get(batch), StandardCharsets.UTF_8)
            .replace("\"user#00024\"", "\"user#00099\"").getBytes(StandardCharsets.UTF_8));
        MockDdbServer server = MockDdbServer.start(0, 8, tampered);
        URI bad = URI.create("http://127.0.0.1:" + server.port());
        try {
            for (Arm arm : List.of(Arm.V3_SYNC_APACHE5, Arm.V3_ASYNC_NETTY)) {
                for (Scenario s : List.of(Scenario.SMALL_GET, Scenario.BATCH_GET)) {
                    failures += expectRejected("wrong response (" + arm.cliName + ", " + s.cliName + ")", () -> {
                        try (V3Workload w = new V3Workload(arm, bad, 1)) {
                            TransportCheck.verify(w, List.of(s));
                        }
                    });
                }
            }
            failures += expectRejected("server never saw the request", () -> {
                try (V3Workload w = new V3Workload(Arm.V3_SYNC_APACHE5, bad, 1)) {
                    TransportCheck.requireWire(w, Scenario.BATCH_PUT,
                                               DynamoDbItems.utf8(Scenario.BATCH_PUT.expectedRequestJson()));
                }
            });
            // Control: the same tampered server still passes the scenarios it did not tamper with.
            try (V3Workload w = new V3Workload(Arm.V3_SYNC_APACHE5, bad, 1)) {
                TransportCheck.verify(w, List.of(Scenario.SMALL_PUT, Scenario.BATCH_PUT));
                System.out.println("CONTROL untampered scenarios on the tampered server pass");
            } catch (Exception e) {
                System.out.println("FAIL control: " + e);
                failures++;
            }
        } finally {
            server.stop();
        }
        return failures;
    }

    interface Check {
        void run() throws Exception;
    }

    static int expectRejected(String name, Check check) {
        try {
            check.run();
            System.out.println("FAIL NEGATIVE accepted: " + name);
            return 1;
        } catch (TransportCheck.VerificationException e) {
            System.out.println("NEGATIVE rejected: " + name + " -> " + e.getMessage().lines().findFirst().orElse(""));
            return 0;
        } catch (Exception e) {
            System.out.println("FAIL NEGATIVE unexpected exception for " + name + ": " + e);
            return 1;
        }
    }

    static Map<Scenario, String> expectedDigests() {
        Map<Scenario, String> m = new EnumMap<>(Scenario.class);
        for (Scenario s : Scenario.values()) {
            m.put(s, ResponseDigest.sha256(ResponseDigest.expected(s)));
        }
        return m;
    }
}
