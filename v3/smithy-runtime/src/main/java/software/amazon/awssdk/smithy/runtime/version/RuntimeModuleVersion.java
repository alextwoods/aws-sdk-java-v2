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

import java.util.Objects;

/**
 * One copy of a V3 module version resource observed on the class path.
 *
 * @param artifactId the artifact ID declared by the resource, or an empty string when the resource does not declare one
 * @param version the version declared by the resource, or an empty string when the resource does not declare one
 * @param source where the resource was loaded from (normally the resource URL), used in diagnostics
 */
public record RuntimeModuleVersion(String artifactId, String version, String source) {

    public RuntimeModuleVersion {
        Objects.requireNonNull(artifactId, "artifactId");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(source, "source");
    }

    /**
     * Returns true if this copy declares exactly {@code expectedArtifactId} at exactly {@code expectedVersion}.
     */
    public boolean matches(String expectedArtifactId, String expectedVersion) {
        return artifactId.equals(expectedArtifactId) && version.equals(expectedVersion);
    }

    String describe(String expectedArtifactId) {
        String shownVersion = version.isEmpty() ? "<no version>" : version;
        String declared = artifactId.equals(expectedArtifactId)
                          ? ""
                          : " declaring artifactId=" + (artifactId.isEmpty() ? "<none>" : artifactId);
        return shownVersion + declared + " (" + source + ")";
    }
}
