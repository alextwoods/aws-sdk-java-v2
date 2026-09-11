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

package software.amazon.awssdk.benchmark.e2e;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;

/**
 * Establishes which request pipeline a V2 client actually runs, and refuses to let an arm measure the
 * wrong one.
 *
 * <p>This exists because of how the bridged build has to be measured. The smithy-java bridge replaces the
 * generated client's execution path, but it keeps every class name and Maven coordinate that stock V2 uses,
 * so a bridged and a stock {@code DynamoDbClient} cannot coexist on one classpath and are told apart only
 * by which jar the JVM loaded. Nothing about a successful run would look wrong if the jars were swapped:
 * the arm would quietly report stock numbers under a bridged label, or the reverse. Both directions are
 * checked, so a mix-up fails the run instead of producing a plausible table.
 *
 * <p>Two independent checks, because they can fail in different ways:
 *
 * <ol>
 *   <li><b>Wiring</b> — the generated client holds a {@code SmithyBridgeClient} field when bridged. This is
 *       definitive about which build was loaded, and needs no request.</li>
 *   <li><b>Execution</b> — a probe call captures a stack from inside the pipeline and looks at which
 *       framework frames are on it. This is definitive about which path a call actually takes, which is the
 *       claim a benchmark makes. Codegen routes operations through the bridge unconditionally, so wiring
 *       implies execution today; the probe is what would catch that ceasing to be true.</li>
 * </ol>
 *
 * <p>The probe uses a throwaway client and a credentials provider that records its own call stack. Credential
 * resolution happens inside the pipeline on both paths, so the frames above it identify the pipeline. The
 * measured client is built separately with the plain provider, so nothing in this class is on the hot path.
 */
final class PipelineCheck {

    /** Marker for the bridged execution path: the field the bridged generated client holds. */
    private static final String BRIDGE_CLIENT_CLASS = "software.amazon.awssdk.bridge.smithyjava.client.SmithyBridgeClient";

    /** Frames that identify the bridged path in a live call stack. */
    private static final String[] BRIDGE_FRAMES = {
        "software.amazon.awssdk.bridge.smithyjava.",
        "software.amazon.smithy.java."
    };

    /**
     * Frames that identify the stock V2 pipeline in a live call stack.
     *
     * <p>More than the request-pipeline package, because where credentials get resolved differs by client: the
     * sync path resolves them inside the pipeline stages, while the async path resolves them earlier, in the
     * client handler and execution-context builder. Matching only {@code internal.http} left the async probe
     * unable to identify a pipeline it was in fact looking straight at.
     */
    private static final String[] STOCK_FRAMES = {
        "software.amazon.awssdk.core.internal.http.",
        "software.amazon.awssdk.core.internal.handler.",
        "software.amazon.awssdk.awscore.client.handler.",
        "software.amazon.awssdk.awscore.internal.AwsExecutionContextBuilder"
    };

    private PipelineCheck() {
    }

    /** Which generated client an arm uses; they are bridged independently, so they must be checked separately. */
    enum ClientKind {
        SYNC("software.amazon.awssdk.services.dynamodb.DefaultDynamoDbClient"),
        ASYNC("software.amazon.awssdk.services.dynamodb.DefaultDynamoDbAsyncClient");

        private final String implClass;

        ClientKind(String implClass) {
            this.implClass = implClass;
        }
    }

    /**
     * Verify that the {@code kind} client runs {@code expected}, and fail loudly if it does not.
     *
     * <p>The kind matters: the bridge prototype covers the synchronous client only, so on a bridged build the
     * sync client is bridged while the async client is still stock. Checking the sync client on behalf of an
     * async arm would report the wrong pipeline — and would have done, before this was parameterized.
     *
     * @param endpoint the mock server, used for the probe call
     */
    static void require(Pipeline expected, URI endpoint, ClientKind kind) {
        Pipeline wiring = detectWiring(kind);
        Pipeline execution = detectExecution(endpoint, kind);

        if (wiring != expected || execution != expected) {
            throw new IllegalStateException(String.format(
                "pipeline mismatch on the " + kind + " client: this arm expects the %s pipeline, but the SDK on the "
                + "classpath is wired for %s and executed through %s.%n"
                + "  The bridged and stock builds share every class name and Maven coordinate, so this almost "
                + "certainly means the wrong benchmark jar was used for this client arm.%n"
                + "  Run the %s arm against a jar built from the matching SDK: the bridged arm needs the bridge "
                + "build, v2-sync/v2-async need a stock build.",
                expected, wiring, execution, expected));
        }
        System.out.printf("=== pipeline verified: %s %s (wiring=%s execution=%s)%n",
                          kind, expected, wiring, execution);
    }

    /**
     * Which pipeline the loaded generated client is wired for, by looking for the bridge's client field.
     */
    private static Pipeline detectWiring(ClientKind kind) {
        Class<?> impl;
        try {
            impl = Class.forName(kind.implClass);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("cannot find the generated DynamoDB client implementation "
                                            + kind.implClass, e);
        }
        for (Field field : impl.getDeclaredFields()) {
            if (BRIDGE_CLIENT_CLASS.equals(field.getType().getName())) {
                return Pipeline.BRIDGED;
            }
        }
        return Pipeline.STOCK;
    }

    /**
     * Which pipeline a real call runs through, from a stack captured inside it.
     */
    private static Pipeline detectExecution(URI endpoint, ClientKind kind) {
        StackCapturingCredentialsProvider probe = new StackCapturingCredentialsProvider();
        GetItemRequest request = GetItemRequest.builder()
                                              .tableName(BenchmarkItems.TABLE_NAME)
                                              .key(Map.of("pk", AttributeValue.fromS(BenchmarkItems.SMALL_KEY)))
                                              .build();
        try {
            if (kind == ClientKind.ASYNC) {
                try (DynamoDbAsyncClient client = DynamoDbAsyncClient.builder()
                                                                     .endpointOverride(endpoint)
                                                                     .region(Region.US_EAST_1)
                                                                     .credentialsProvider(probe)
                                                                     .build()) {
                    client.getItem(request).join();
                }
            } else {
                try (DynamoDbClient client = DynamoDbClient.builder()
                                                           .endpointOverride(endpoint)
                                                           .region(Region.US_EAST_1)
                                                           .credentialsProvider(probe)
                                                           .build()) {
                    client.getItem(request);
                }
            }
        } catch (RuntimeException e) {
            // A failed probe call is not fatal on its own -- what matters is whether a pipeline stack was
            // captured before the failure. If none was, rethrow: an arm must not be measured unverified.
            if (probe.frames.isEmpty()) {
                throw new IllegalStateException("pipeline probe call failed before credentials were resolved, so the "
                                                + "pipeline could not be identified", e);
            }
        }
        if (probe.frames.isEmpty()) {
            throw new IllegalStateException("pipeline probe resolved no credentials; cannot identify the pipeline");
        }
        // Bridge frames decide it: those packages execute only when the bridge is running the call, whereas the
        // stock markers are general client machinery that a bridged path may also touch. Requiring the two to be
        // mutually exclusive would make the answer depend on how much of V2 the bridge happens to reuse.
        if (anyFrameMatches(probe.frames, BRIDGE_FRAMES)) {
            return Pipeline.BRIDGED;
        }
        if (anyFrameMatches(probe.frames, STOCK_FRAMES)) {
            return Pipeline.STOCK;
        }
        throw new IllegalStateException("pipeline probe was inconclusive: no bridge or stock frames on the stack. "
                                        + "Frames were: " + probe.frames);
    }

    private static boolean anyFrameMatches(List<String> frames, String[] prefixes) {
        for (String frame : frames) {
            for (String prefix : prefixes) {
                if (frame.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Records the stack of whoever asked it for credentials. Used once, by the probe client only.
     */
    private static final class StackCapturingCredentialsProvider implements AwsCredentialsProvider {
        private final List<String> frames = new ArrayList<>();

        @Override
        public AwsCredentials resolveCredentials() {
            if (frames.isEmpty()) {
                for (StackTraceElement element : new Throwable().getStackTrace()) {
                    frames.add(element.getClassName() + "." + element.getMethodName());
                }
            }
            return AwsBasicCredentials.create(Workloads.ACCESS_KEY, Workloads.SECRET_KEY);
        }
    }
}
