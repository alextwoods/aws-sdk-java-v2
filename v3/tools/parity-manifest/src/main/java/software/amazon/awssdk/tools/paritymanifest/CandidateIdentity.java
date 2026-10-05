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

import java.util.Objects;

/**
 * The V3 release candidate a manifest is evaluated for (Requirements 16.14, 17.12).
 *
 * @param v3Version   the candidate version
 * @param gitRevision the candidate source revision, or {@code null} when unknown
 */
public record CandidateIdentity(String v3Version, String gitRevision) {
    public CandidateIdentity {
        Objects.requireNonNull(v3Version, "v3Version");
    }
}
