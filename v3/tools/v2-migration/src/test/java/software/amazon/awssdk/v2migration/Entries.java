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
import software.amazon.awssdk.v2migration.manifest.Classification;
import software.amazon.awssdk.v2migration.manifest.EntryKind;
import software.amazon.awssdk.v2migration.manifest.ManifestBaseline;
import software.amazon.awssdk.v2migration.manifest.ManifestEntry;
import software.amazon.awssdk.v2migration.manifest.MigrationKind;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;

/** Valid manifest entries for tests. */
final class Entries {

    static final ManifestBaseline BASELINE = new ManifestBaseline("2.54.0", "abc123", null, null, "parity-manifest/test",
                                                                  null, null);

    private Entries() {
    }

    static ManifestEntry preserved(String id) {
        return new ManifestEntry(id, kind(id), "surface " + id, "artifact", null, false, Classification.PRESERVED,
                                 List.of("source"), "Kept unchanged in V3.", List.of(), null, null, null, null, null,
                                 List.of());
    }

    static ManifestEntry adapted(String id, MigrationKind kind, String replacement, String migration) {
        return new ManifestEntry(id, kind(id), "surface " + id, "artifact", null, false, Classification.ADAPTED,
                                 List.of(), "V3 changes the contract.", List.of("The V3 contract differs."),
                                 "software.amazon.awssdk:sdk-core", replacement, kind, migration, null,
                                 List.of("test:evidence"));
    }

    static ManifestEntry excluded(String id, String replacement, String migration) {
        return new ManifestEntry(id, kind(id), "surface " + id, "artifact", null, true, Classification.EXCLUDED,
                                 List.of(), "Deprecated legacy facility.", List.of(), null, replacement,
                                 MigrationKind.DIAGNOSTIC, migration, null, List.of());
    }

    static MigrationManifest manifest(ManifestEntry... entries) {
        return MigrationManifest.of(BASELINE, List.of(entries));
    }

    private static EntryKind kind(String id) {
        return EntryKind.ofId(id).orElseThrow(() -> new IllegalArgumentException(id));
    }
}
