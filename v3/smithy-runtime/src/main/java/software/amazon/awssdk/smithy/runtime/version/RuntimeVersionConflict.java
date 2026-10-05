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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A participating V3 module whose observed version does not exactly match the expected release version.
 *
 * @param artifactId the participating module
 * @param expectedVersion the exact release version every participating module must have
 * @param observed every copy of the module's version resource found on the class path, in class-path order; empty when
 *                 the module is missing
 */
public record RuntimeVersionConflict(String artifactId, String expectedVersion, List<RuntimeModuleVersion> observed) {

    public RuntimeVersionConflict {
        Objects.requireNonNull(artifactId, "artifactId");
        Objects.requireNonNull(expectedVersion, "expectedVersion");
        observed = List.copyOf(observed);
    }

    /**
     * Returns true if no version resource for the module was found.
     */
    public boolean missing() {
        return observed.isEmpty();
    }

    String describe() {
        if (missing()) {
            return artifactId + ": missing (no " + RuntimeVersionHandshake.resourceName(artifactId)
                   + " on the class path)";
        }
        return artifactId + ": found " + observed.stream()
                                                 .map(v -> v.describe(artifactId))
                                                 .collect(Collectors.joining(", "));
    }
}
