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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * One classified Parity Manifest entry (design "Parity Manifest schema"). Construction only enforces structure;
 * the classification-dependent required-field rules are checked by {@link ManifestValidator} so that every problem
 * of a manifest can be reported at once.
 *
 * @param id              stable entry ID (see {@link EntryKind})
 * @param kind            entry kind
 * @param v2Surface       human-readable V2 surface
 * @param source          baseline source location
 * @param deprecated      whether the V2 baseline marks the entry deprecated
 * @param classification  the single Compatibility Classification (Requirement 16.3)
 * @param dimensions      the compatibility dimensions claimed for the entry
 * @param rationale       why the entry has its classification (and freeze evidence)
 * @param differences     every behavioral and API difference from V2 (Requirement 16.11)
 * @param v3Artifact      owning V3 artifact as {@code groupId:artifactId}, or {@code null}
 * @param replacement     the V3 replacement as an entry ID of the candidate inventory, or {@code null}
 * @param migrationKind   how the migration action is delivered, or {@code null}
 * @param migration       the migration action or diagnostic text, or {@code null}
 * @param supportEvidence deprecation or support evidence for an exclusion (Requirement 16.13), or {@code null}
 * @param evidenceIds     verification test/evidence IDs
 */
public record ManifestEntry(String id,
                            EntryKind kind,
                            String v2Surface,
                            SourceLocation source,
                            boolean deprecated,
                            Classification classification,
                            SortedSet<CompatibilityDimension> dimensions,
                            String rationale,
                            List<String> differences,
                            String v3Artifact,
                            String replacement,
                            MigrationKind migrationKind,
                            String migration,
                            String supportEvidence,
                            SortedSet<String> evidenceIds) {
    public ManifestEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(v2Surface, "v2Surface");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(classification, "classification");
        dimensions = sorted(dimensions);
        differences = differences == null ? List.of() : List.copyOf(differences);
        evidenceIds = sorted(evidenceIds);
    }

    /** True if the entry claims {@link CompatibilityDimension#BINARY} (the JSON form's {@code binary}). */
    public boolean binary() {
        return dimensions.contains(CompatibilityDimension.BINARY);
    }

    /**
     * The disposition string the V3 runtime's unsupported-option diagnostics carry: the classification name, followed
     * by the rationale when there is one.
     */
    public String disposition() {
        return rationale == null || rationale.isBlank() ? classification.name() : classification + " " + rationale;
    }

    private static <T extends Comparable<T>> SortedSet<T> sorted(Collection<T> values) {
        return Collections.unmodifiableSortedSet(values == null ? new TreeSet<>() : new TreeSet<>(values));
    }
}
