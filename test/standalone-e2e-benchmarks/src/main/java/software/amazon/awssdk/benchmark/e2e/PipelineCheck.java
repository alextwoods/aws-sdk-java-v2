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
 * Establishes which request pipeline a V2 client actually runs, so every results row names it.
 *
 * <p>Every arm of a paired A/B run (Baseline_Build, racecar, feature, candidate) ships the same class names and
 * Maven coordinates, so a jar mix-up would otherwise produce a plausible table under the wrong label. The detected
 * pipeline is printed and written to the {@code pipeline} results column; {@code paired_ab_summary.py} refuses a
 * null experiment whose two arms ran different pipelines.
 *
 * <p>Adapted from racecar's check, which told its stock build from its smithy-java bridged build. Here the two
 * pipelines are the legacy one ({@link Pipeline#STOCK}) and the schema path ({@link Pipeline#SCHEMA}). The probe
 * uses a throwaway client and a credentials provider that records its own call stack; credentials are resolved
 * inside the pipeline on both paths, so the frames above it identify the pipeline. The measured client is built
 * separately with the plain provider, so nothing in this class is on the hot path.
 */
final class PipelineCheck {

    /** Frames that identify the schema path in a live call stack. */
    private static final String[] SCHEMA_FRAMES = {
        "software.amazon.awssdk.core.internal.http.SyncApiCallPipeline",
        "software.amazon.awssdk.core.internal.http.AsyncApiCallPipeline"
    };

    /**
     * Frames that identify the legacy V2 pipeline. The sync path resolves credentials inside the pipeline stages,
     * the async path earlier, in the client handler and execution-context builder.
     */
    private static final String[] STOCK_FRAMES = {
        "software.amazon.awssdk.core.internal.http.",
        "software.amazon.awssdk.core.internal.handler.",
        "software.amazon.awssdk.awscore.client.handler.",
        "software.amazon.awssdk.awscore.internal.AwsExecutionContextBuilder"
    };

    private static volatile Pipeline detected;

    private PipelineCheck() {
    }

    enum ClientKind {
        SYNC,
        ASYNC
    }

    /**
     * Detect the pipeline the {@code kind} client runs, print it, and remember it for the results file.
     *
     * @param endpoint the mock server, used for the probe call
     */
    static Pipeline report(URI endpoint, ClientKind kind) {
        Pipeline execution = detectExecution(endpoint, kind);
        detected = execution;
        System.out.printf("=== pipeline detected: %s %s%n", kind, execution);
        return execution;
    }

    /** The pipeline found by the last {@link #report} call, or {@code "unknown"} if none ran. */
    static String detectedName() {
        Pipeline p = detected;
        return p == null ? "unknown" : p.name().toLowerCase(java.util.Locale.ROOT);
    }

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
            // A failed probe is not fatal on its own: what matters is whether a pipeline stack was captured.
            if (probe.frames.isEmpty()) {
                throw new IllegalStateException("pipeline probe call failed before credentials were resolved, so the "
                                                + "pipeline could not be identified", e);
            }
        }
        if (probe.frames.isEmpty()) {
            throw new IllegalStateException("pipeline probe resolved no credentials; cannot identify the pipeline");
        }
        // Schema frames decide it: the legacy markers are general client machinery the schema path may also touch.
        if (anyFrameMatches(probe.frames, SCHEMA_FRAMES)) {
            return Pipeline.SCHEMA;
        }
        if (anyFrameMatches(probe.frames, STOCK_FRAMES)) {
            return Pipeline.STOCK;
        }
        throw new IllegalStateException("pipeline probe was inconclusive: no schema or stock frames on the stack. "
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

    /** Records the stack of whoever asked it for credentials. Used once, by the probe client only. */
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
