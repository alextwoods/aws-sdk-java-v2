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

package software.amazon.awssdk.v2migration;

import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.v2migration.manifest.Classification;
import software.amazon.awssdk.v2migration.manifest.MigrationKind;

/**
 * The blocking diagnostic of an {@link Disposition#UNRESOLVED} item (Requirement 17.9): where the unchanged construct
 * is, what it is, the Parity Manifest entry it joins by and that entry's classification, rationale, V3 replacement and
 * migration text, and why the tooling did not transform it.
 *
 * @param location       file, line and column of the unchanged construct
 * @param construct      the affected construct
 * @param entryId        the Parity Manifest entry ID; without an entry, the ID the construct was looked up by, or
 *                       {@code null} when it could not be identified
 * @param classification the entry's Compatibility Classification, or {@code null} without an entry
 * @param rationale      the entry's classification rationale, or {@code null} without an entry
 * @param replacement    the entry's V3 replacement entry ID, or {@code null} when none exists
 * @param migrationKind  how the entry's migration action is delivered, or {@code null}
 * @param migration      the entry's migration action or documentation text, or {@code null}
 * @param differences    the entry's behavioral and API differences from V2
 * @param reason         why the construct was left unchanged
 */
public record MigrationDiagnostic(SourceLocation location,
                                  String construct,
                                  String entryId,
                                  Classification classification,
                                  String rationale,
                                  String replacement,
                                  MigrationKind migrationKind,
                                  String migration,
                                  List<String> differences,
                                  String reason) {

    public MigrationDiagnostic {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(construct, "construct");
        Objects.requireNonNull(reason, "reason");
        differences = differences == null ? List.of() : List.copyOf(differences);
    }

    /**
     * The documentation identifier of the diagnostic (Requirement 18.9): for an {@code ADAPTED} or {@code EXCLUDED}
     * entry, its Parity Manifest entry ID, under which V3 publishes the entry's rationale, replacement, differences and
     * migration action (Requirements 16.6, 16.7, 22.6, 22.7); {@code null} for a construct without such an entry.
     */
    public String documentationId() {
        return entryId != null && (classification == Classification.ADAPTED
                                   || classification == Classification.EXCLUDED) ? entryId : null;
    }

    /**
     * A one-line human-readable form: {@code path:line:column: construct [entry, CLASSIFICATION]: reason}, followed by
     * the rationale, replacement, migration text and documentation identifier when present.
     */
    public String message() {
        StringBuilder out = new StringBuilder();
        out.append(location).append(": ").append(construct);
        if (entryId != null) {
            out.append(" [").append(entryId);
            if (classification != null) {
                out.append(", ").append(classification);
            }
            out.append(']');
        }
        out.append(": ").append(reason);
        if (rationale != null) {
            out.append(" Rationale: ").append(rationale);
        }
        if (replacement != null) {
            out.append(" Replacement: ").append(replacement).append('.');
        }
        if (migration != null) {
            out.append(" Migration: ").append(migration);
        }
        String documentation = documentationId();
        if (documentation != null) {
            out.append(" Documentation: ").append(documentation).append('.');
        }
        return out.toString();
    }
}
