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

package software.amazon.awssdk.benchmark.harness;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * Receives the plan before anything is launched, every run record as it is produced, and the manifest once the run
 * ends. {@link ValiditySink} (repetition, cell and collection validity) is the first consumer and forwards annotated
 * records to 19.3 (metrics, statistics, reports) and 19.4 (gates); the orchestrator itself writes {@code manifest.json}.
 */
public interface ResultSink {

    /**
     * The preflight plan: {@code mode}, {@code release_evidence}, {@code host} (preflight metadata), {@code config}
     * (including {@code measure_ns}, {@code jvm_flags} and {@code warmup.sampling}), {@code pinning}, and
     * {@code schedule} (arms, runs, structural {@code problems}).
     */
    default void begin(ObjectNode plan) throws IOException {
    }

    void accept(ObjectNode runRecord) throws IOException;

    default void finish(ObjectNode manifest) throws IOException {
    }

    /** Appends one compact JSON record per line to {@code file}. */
    static ResultSink jsonLines(Path file) {
        return record -> Files.writeString(file, Node.printJson(record) + "\n", StandardCharsets.UTF_8,
                                           StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
