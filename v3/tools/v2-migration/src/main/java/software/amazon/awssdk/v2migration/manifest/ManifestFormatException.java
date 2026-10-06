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

package software.amazon.awssdk.v2migration.manifest;

import java.util.List;

/**
 * Thrown when a document is not a valid Parity Manifest. Lists every problem found, each as {@code <subject>: <text>},
 * where the subject is the entry ID or a JSON location.
 */
public final class ManifestFormatException extends Exception {

    private static final long serialVersionUID = 1L;

    private final transient List<String> problems;

    ManifestFormatException(List<String> problems) {
        super("Invalid Parity Manifest (" + problems.size() + " problem" + (problems.size() == 1 ? "" : "s") + "):\n  "
              + String.join("\n  ", problems));
        this.problems = List.copyOf(problems);
    }

    /** Every problem, sorted. */
    public List<String> problems() {
        return problems;
    }
}
