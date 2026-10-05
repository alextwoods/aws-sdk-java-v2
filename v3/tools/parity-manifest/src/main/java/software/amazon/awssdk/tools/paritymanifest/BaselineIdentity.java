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

package software.amazon.awssdk.tools.paritymanifest;

import java.time.LocalDate;
import java.util.Objects;

/**
 * The Launch Parity Baseline identity (Requirement 16.1): the single V2 release, its feature-freeze cutoff date and
 * support or deprecation status, plus the scanned source revision and the scanner that produced the inventory.
 *
 * @param v2Version           the V2 release version (the root {@code pom.xml} version of this repository)
 * @param gitRevision         the scanned V2 source revision, or {@code null} when unknown
 * @param featureFreezeCutoff the feature-freeze cutoff date, or {@code null} before freeze
 * @param supportStatus       the support or deprecation status of the baseline release, or {@code null} before freeze
 * @param scannerVersion      the generator identity, e.g. {@code parity-manifest/3.0.0}
 */
public record BaselineIdentity(String v2Version,
                               String gitRevision,
                               LocalDate featureFreezeCutoff,
                               String supportStatus,
                               String scannerVersion) {
    public BaselineIdentity {
        Objects.requireNonNull(v2Version, "v2Version");
        Objects.requireNonNull(scannerVersion, "scannerVersion");
    }

    /** True for a V2 general-availability release version (not a {@code -SNAPSHOT} or pre-release). */
    public boolean isGeneralAvailabilityRelease() {
        return v2Version.matches("2\\.\\d+\\.\\d+");
    }
}
