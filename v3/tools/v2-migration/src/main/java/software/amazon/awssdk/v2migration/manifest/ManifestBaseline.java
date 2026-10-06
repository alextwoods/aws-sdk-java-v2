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

import java.util.Objects;

/**
 * The Launch Parity Baseline identity of a manifest (the schema's {@code baseline} member) and the optional release
 * candidate it was evaluated for (the schema's {@code candidate} member).
 *
 * @param v2Version           the V2 baseline release version
 * @param gitRevision         the scanned V2 source revision, or {@code null}
 * @param featureFreezeCutoff the ISO-8601 feature-freeze cutoff date, or {@code null}
 * @param supportStatus       the support or deprecation status of the baseline, or {@code null}
 * @param scannerVersion      the generator identity that produced the manifest
 * @param candidateVersion    the V3 release candidate version, or {@code null} for a baseline-only manifest
 * @param candidateRevision   the V3 release candidate source revision, or {@code null}
 */
public record ManifestBaseline(String v2Version,
                               String gitRevision,
                               String featureFreezeCutoff,
                               String supportStatus,
                               String scannerVersion,
                               String candidateVersion,
                               String candidateRevision) {

    public ManifestBaseline {
        Objects.requireNonNull(v2Version, "v2Version");
        Objects.requireNonNull(scannerVersion, "scannerVersion");
    }
}
