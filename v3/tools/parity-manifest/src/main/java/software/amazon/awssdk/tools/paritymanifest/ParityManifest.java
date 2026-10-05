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

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A Parity Manifest: the baseline identity, the optional candidate identity, and the entries sorted by ID. The
 * schema version of the JSON form is {@link #SCHEMA_VERSION}. Validity is checked by {@link ManifestValidator}.
 *
 * @param baseline  the Launch Parity Baseline identity
 * @param candidate the release candidate identity, or {@code null} for a baseline-only manifest
 * @param entries   the entries; stored sorted by {@link ManifestEntry#id()}
 */
public record ParityManifest(BaselineIdentity baseline, CandidateIdentity candidate, List<ManifestEntry> entries) {
    /** The schema version of the JSON form ({@code parity-manifest-1.schema.json}). */
    public static final int SCHEMA_VERSION = 1;

    public ParityManifest {
        Objects.requireNonNull(baseline, "baseline");
        entries = entries.stream().sorted(Comparator.comparing(ManifestEntry::id)).toList();
    }

    /** The candidate identity, if any. */
    public Optional<CandidateIdentity> candidateIdentity() {
        return Optional.ofNullable(candidate);
    }

    /** The entry with {@code id}, if any. */
    public Optional<ManifestEntry> entry(String id) {
        return entries.stream().filter(e -> e.id().equals(id)).findFirst();
    }

    /** Entry counts per classification, every classification present (the compatibility report summary, 17.12). */
    public Map<Classification, Integer> summary() {
        Map<Classification, Integer> counts = new EnumMap<>(Classification.class);
        for (Classification c : Classification.values()) {
            counts.put(c, 0);
        }
        entries.forEach(e -> counts.merge(e.classification(), 1, Integer::sum));
        return counts;
    }
}
