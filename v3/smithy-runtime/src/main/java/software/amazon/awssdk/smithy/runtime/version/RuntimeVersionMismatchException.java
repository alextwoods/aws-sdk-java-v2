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

package software.amazon.awssdk.smithy.runtime.version;

import java.io.Serial;
import java.util.List;
import java.util.Objects;

/**
 * Thrown during client construction when participating V3 runtime modules are not all at the exact expected release
 * version (Requirement 15.11). The message and {@link #conflicts()} identify every conflicting module and version.
 */
public final class RuntimeVersionMismatchException extends IllegalStateException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String expectedVersion;
    private final transient List<RuntimeVersionConflict> conflicts;

    RuntimeVersionMismatchException(String expectedVersion, List<RuntimeVersionConflict> conflicts) {
        super(RuntimeVersionHandshake.describe(expectedVersion, conflicts));
        this.expectedVersion = Objects.requireNonNull(expectedVersion, "expectedVersion");
        this.conflicts = List.copyOf(conflicts);
    }

    /**
     * The exact release version every participating module was required to have.
     */
    public String expectedVersion() {
        return expectedVersion;
    }

    /**
     * Every conflicting module, sorted by artifact ID.
     */
    public List<RuntimeVersionConflict> conflicts() {
        return conflicts == null ? List.of() : conflicts;
    }
}
