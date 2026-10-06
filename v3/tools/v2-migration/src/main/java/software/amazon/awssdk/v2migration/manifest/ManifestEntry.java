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
import java.util.Objects;

/**
 * One entry of a published Parity Manifest, as read from the {@code entries} array of
 * {@code parity-manifest-1.schema.json}. Member names and meanings are the schema's; the derived {@code binary} and
 * {@code evidence} members are checked on read and not stored.
 *
 * @param id              stable entry ID (see {@link EntryKind})
 * @param kind            entry kind
 * @param v2Surface       human-readable V2 surface
 * @param sourceArtifact  the owning V2 artifact or non-artifact origin ({@code source.artifact})
 * @param sourcePath      the baseline file or jar entry, or {@code null} ({@code source.path})
 * @param deprecated      whether the V2 baseline deprecates the entry
 * @param classification  the single Compatibility Classification
 * @param dimensions      the claimed compatibility dimensions ({@code source}, {@code binary}, {@code behavior},
 *                        {@code configuration}), sorted
 * @param rationale       why the entry has its classification
 * @param differences     every behavioral and API difference from V2
 * @param v3Artifact      owning V3 artifact as {@code groupId:artifactId}, or {@code null}
 * @param replacement     the V3 replacement as an entry ID, or {@code null}
 * @param migrationKind   how the migration action is delivered, or {@code null}
 * @param migration       the migration action or diagnostic text, or {@code null}
 * @param supportEvidence deprecation or support evidence for an exclusion, or {@code null}
 * @param evidenceIds     verification test/evidence IDs, sorted
 */
public record ManifestEntry(String id,
                            EntryKind kind,
                            String v2Surface,
                            String sourceArtifact,
                            String sourcePath,
                            boolean deprecated,
                            Classification classification,
                            List<String> dimensions,
                            String rationale,
                            List<String> differences,
                            String v3Artifact,
                            String replacement,
                            MigrationKind migrationKind,
                            String migration,
                            String supportEvidence,
                            List<String> evidenceIds) {

    public ManifestEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(v2Surface, "v2Surface");
        Objects.requireNonNull(sourceArtifact, "sourceArtifact");
        Objects.requireNonNull(classification, "classification");
        dimensions = dimensions == null ? List.of() : dimensions.stream().sorted().toList();
        differences = differences == null ? List.of() : List.copyOf(differences);
        evidenceIds = evidenceIds == null ? List.of() : evidenceIds.stream().sorted().toList();
    }

    /** True if the entry claims binary compatibility (the schema's derived {@code binary} member). */
    public boolean binary() {
        return dimensions.contains("binary");
    }
}
