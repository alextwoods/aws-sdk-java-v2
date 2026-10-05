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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Test builders for inventory items, entries and manifests. */
final class Entries {
    static final BaselineIdentity BASELINE =
        new BaselineIdentity("2.55.5", "e1ec10e99088686ca5f238aaca21a15b9a7b09d4", LocalDate.of(2026, 9, 1),
                             "Generally available, supported", "parity-manifest/test");

    private Entries() {
    }

    static InventoryItem item(String id, EntryKind kind, boolean deprecated) {
        return new InventoryItem(id, kind, "surface of " + id, new SourceLocation("software.amazon.awssdk:sdk-core",
                                                                                 "core/sdk-core/src/main/java"),
                                 deprecated);
    }

    static InventoryItem item(String id, EntryKind kind) {
        return item(id, kind, false);
    }

    /** A mutable entry builder whose defaults form a valid entry of the given classification. */
    static final class B {
        String id;
        EntryKind kind;
        String v2Surface;
        SourceLocation source = new SourceLocation("software.amazon.awssdk:sdk-core", null);
        boolean deprecated;
        Classification classification;
        Set<CompatibilityDimension> dimensions = EnumSet.noneOf(CompatibilityDimension.class);
        String rationale = "because";
        List<String> differences = new ArrayList<>();
        String v3Artifact = "software.amazon.awssdk:sdk-core";
        String replacement;
        MigrationKind migrationKind;
        String migration;
        String supportEvidence;
        Set<String> evidenceIds = new TreeSet<>();

        B(String id, EntryKind kind, Classification classification) {
            this.id = id;
            this.kind = kind;
            this.v2Surface = "V2 " + id;
            this.classification = classification;
            switch (classification) {
                case PRESERVED -> dimensions.add(CompatibilityDimension.SOURCE);
                case ADAPTED -> {
                    differences.add("returns a V3 type");
                    replacement = "api:software.amazon.awssdk.v3.Replacement";
                    migrationKind = MigrationKind.RECIPE;
                    migration = "apply recipe X";
                    evidenceIds.add("AdaptedTest");
                }
                case EXCLUDED -> {
                    supportEvidence = "deprecated since 2.20";
                    migrationKind = MigrationKind.DIAGNOSTIC;
                    migration = "remove it";
                }
                default -> throw new IllegalStateException();
            }
        }

        ManifestEntry build() {
            return new ManifestEntry(id, kind, v2Surface, source, deprecated, classification, new TreeSet<>(dimensions),
                                     rationale, differences, v3Artifact, replacement, migrationKind, migration,
                                     supportEvidence, new TreeSet<>(evidenceIds));
        }
    }

    static B preserved(String id) {
        return new B(id, EntryKind.ofId(id).orElseThrow(), Classification.PRESERVED);
    }

    static B adapted(String id) {
        return new B(id, EntryKind.ofId(id).orElseThrow(), Classification.ADAPTED);
    }

    static B excluded(String id) {
        return new B(id, EntryKind.ofId(id).orElseThrow(), Classification.EXCLUDED);
    }

    /** A valid manifest with one entry of each kind and classification. */
    static ParityManifest sample() {
        B binary = preserved("api:software.amazon.awssdk.services.dynamodb.DynamoDbClient#getItem"
                             + "(Ljava/util/function/Consumer;)Lsoftware/amazon/awssdk/services/dynamodb/model/"
                             + "GetItemResponse;");
        binary.dimensions.add(CompatibilityDimension.BINARY);
        binary.evidenceIds.addAll(List.of("V2BytecodeOnV3Test", "DynamoDbGeneratedApiTest"));
        B unicode = preserved("behavior:retry.throttling-backoff");
        unicode.dimensions = EnumSet.of(CompatibilityDimension.BEHAVIOR);
        unicode.rationale = "Délai exponentiel \u2014 unchanged";
        return new ParityManifest(BASELINE, new CandidateIdentity("3.0.0", null), List.of(
            excluded("api:software.amazon.awssdk.core.retry.RetryPolicy").build(),
            binary.build(),
            preserved("artifact:software.amazon.awssdk:dynamodb").build(),
            unicode.build(),
            preserved("config:env:AWS_REGION").build(),
            preserved("http-mode:apache-client:sync").build(),
            preserved("service:dynamodb").build(),
            adapted("api:software.amazon.awssdk.services.dynamodb.DynamoDbClient").build()));
    }
}
