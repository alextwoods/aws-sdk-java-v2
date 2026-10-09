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
 * Receives every run record as it is produced, and the manifest once the run ends. Extension point for 19.2
 * (repetition/cell validity), 19.3 (metrics, statistics, reports) and 19.4 (gates); the orchestrator itself only
 * appends records to {@code results.jsonl} and writes {@code manifest.json}.
 */
public interface ResultSink {

    void accept(ObjectNode runRecord) throws IOException;

    default void finish(ObjectNode manifest) throws IOException {
    }

    /** Appends one compact JSON record per line to {@code file}. */
    static ResultSink jsonLines(Path file) {
        return record -> Files.writeString(file, Node.printJson(record) + "\n", StandardCharsets.UTF_8,
                                           StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
