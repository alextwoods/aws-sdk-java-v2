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

/**
 * Whether a run may stand as release-gate evidence.
 *
 * <ul>
 *     <li>{@code exploratory}: development runs. Missing host capabilities (CPU pinning on macOS, an undetectable power
 *     source) and cells without a workload fixture are recorded with the reason and the run continues; its records
 *     are marked {@code releaseEvidence=false}.</li>
 *     <li>{@code release}: a release-gate collection (Req 20.5). It fails closed before launching anything if the
 *     client and mock service cannot be pinned to disjoint dedicated CPUs, or if any selected cell has no workload
 *     fixture, since either would make the collection invalid evidence.</li>
 * </ul>
 */
public enum HarnessMode {
    EXPLORATORY("exploratory"),
    RELEASE("release");

    private final String id;

    HarnessMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static HarnessMode fromId(String id) {
        for (HarnessMode m : values()) {
            if (m.id.equals(id)) {
                return m;
            }
        }
        throw new IllegalArgumentException("unknown harness mode: " + id);
    }
}
