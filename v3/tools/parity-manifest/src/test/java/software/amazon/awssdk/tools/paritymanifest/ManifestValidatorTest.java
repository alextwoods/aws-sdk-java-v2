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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ManifestValidatorTest {
    private static final String ID = "api:software.amazon.awssdk.core.Thing";

    @Test
    void validEntriesOfEveryClassificationPass() {
        assertEquals(List.of(), ManifestValidator.validate(Entries.sample(), ManifestValidator.Options.DEFAULT));
        Entries.B deprecatedExclusion = Entries.excluded(ID);
        deprecatedExclusion.supportEvidence = null;
        deprecatedExclusion.deprecated = true;
        deprecatedExclusion.migrationKind = MigrationKind.COMPILE_TIME;
        assertEquals(List.of(), problems(deprecatedExclusion));
    }

    /** The required/forbidden-field matrix: one mutation of a valid entry, and the single problem it causes. */
    static Stream<Arguments> matrix() {
        return Stream.of(
            row("all: rationale", Entries.preserved(ID), b -> b.rationale = " ", "MISSING_FIELD", "requires rationale"),
            row("all: v2Surface", Entries.adapted(ID), b -> b.v2Surface = "", "MISSING_FIELD", "requires v2Surface"),
            row("all: ID scheme", Entries.preserved(ID), b -> b.kind = EntryKind.PUBLIC_MEMBER, "INVALID_ID",
                "PUBLIC_MEMBER ID scheme"),
            row("all: replacement is an ID", Entries.adapted(ID), b -> b.replacement = "RetryStrategy",
                "INVALID_VALUE", "not a well-formed entry ID"),
            row("PRESERVED: dimensions", Entries.preserved(ID), b -> b.dimensions.clear(), "MISSING_FIELD",
                "PRESERVED requires dimensions"),
            row("PRESERVED: replacement", Entries.preserved(ID), b -> b.replacement = "api:a.B", "FORBIDDEN_FIELD",
                "PRESERVED forbids replacement"),
            row("PRESERVED: differences", Entries.preserved(ID), b -> b.differences.add("x"), "FORBIDDEN_FIELD",
                "PRESERVED forbids differences"),
            row("PRESERVED: migration", Entries.preserved(ID), b -> b.migration = "x", "FORBIDDEN_FIELD",
                "PRESERVED forbids migration"),
            row("PRESERVED: migrationKind", Entries.preserved(ID), b -> b.migrationKind = MigrationKind.RECIPE,
                "FORBIDDEN_FIELD", "PRESERVED forbids migrationKind"),
            row("ADAPTED: differences", Entries.adapted(ID), b -> b.differences.clear(), "MISSING_FIELD",
                "ADAPTED requires differences"),
            row("ADAPTED: replacement", Entries.adapted(ID), b -> b.replacement = null, "MISSING_FIELD",
                "ADAPTED requires replacement"),
            row("ADAPTED: migration", Entries.adapted(ID), b -> b.migration = null, "MISSING_FIELD",
                "ADAPTED requires migration"),
            row("ADAPTED: migrationKind", Entries.adapted(ID), b -> b.migrationKind = null, "MISSING_FIELD",
                "ADAPTED requires migrationKind"),
            row("ADAPTED: recipe or diagnostic", Entries.adapted(ID), b -> b.migrationKind = MigrationKind.COMPILE_TIME,
                "INVALID_VALUE", "must be RECIPE or DIAGNOSTIC"),
            row("ADAPTED: verification evidence", Entries.adapted(ID), b -> b.evidenceIds.clear(), "MISSING_FIELD",
                "ADAPTED requires evidenceIds"),
            row("EXCLUDED: support evidence", Entries.excluded(ID), b -> b.supportEvidence = null, "MISSING_FIELD",
                "requires supportEvidence"),
            row("EXCLUDED: diagnostic", Entries.excluded(ID), b -> b.migration = null, "MISSING_FIELD",
                "EXCLUDED requires migration"),
            row("EXCLUDED: compile-time or diagnostic", Entries.excluded(ID),
                b -> b.migrationKind = MigrationKind.RECIPE, "INVALID_VALUE", "must be COMPILE_TIME or DIAGNOSTIC"),
            row("EXCLUDED: dimensions", Entries.excluded(ID), b -> b.dimensions.add(CompatibilityDimension.SOURCE),
                "FORBIDDEN_FIELD", "EXCLUDED forbids dimensions"));
    }

    private static Arguments row(String name, Entries.B builder, Consumer<Entries.B> mutation, String code,
                                 String detail) {
        mutation.accept(builder);
        return Arguments.of(name, builder, code, detail);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("matrix")
    void requiredFieldMatrix(String name, Entries.B builder, String code, String detail) {
        List<ManifestProblem> problems = problems(builder);
        assertEquals(1, problems.size(), problems.toString());
        assertEquals(code, problems.get(0).code().name());
        assertEquals(ID, problems.get(0).subject());
        assertEquals(true, problems.get(0).message().contains(detail), problems.get(0).message());
    }

    @Test
    void excludedReplacementIsOptional() {
        Entries.B b = Entries.excluded(ID);
        b.replacement = "api:software.amazon.awssdk.retries.api.RetryStrategy";
        assertEquals(List.of(), problems(b));
    }

    @Test
    void reportsDuplicatesUnknownReplacementsAndEveryEntryAtOnce() {
        Entries.B noDifferences = Entries.adapted("api:a.Adapted");
        noDifferences.differences.clear();
        Entries.B unknown = Entries.adapted("api:a.Unknown");
        unknown.replacement = "api:v3.Missing";
        Entries.B noDimensions = Entries.preserved("config:env:AWS_REGION");
        noDimensions.dimensions = EnumSet.noneOf(CompatibilityDimension.class);
        ParityManifest manifest = new ParityManifest(Entries.BASELINE, null, List.of(
            Entries.preserved("api:a.Dup").build(), Entries.preserved("api:a.Dup").build(),
            noDifferences.build(), unknown.build(), noDimensions.build()));
        List<String> problems = ManifestValidator.validate(
            manifest, new ManifestValidator.Options(false, Set.of("api:software.amazon.awssdk.v3.Replacement")))
                                                 .stream().map(ManifestProblem::toString).toList();
        assertEquals(List.of(
            "MISSING_FIELD [api:a.Adapted]: ADAPTED requires differences",
            "DUPLICATE_ID [api:a.Dup]: the manifest contains this ID more than once",
            "UNKNOWN_REPLACEMENT [api:a.Unknown]: replacement 'api:v3.Missing' is not in the candidate inventory",
            "MISSING_FIELD [config:env:AWS_REGION]: PRESERVED requires dimensions"), problems);
    }

    @Test
    void frozenBaselineRequiresGaReleaseCutoffStatusAndRevision() {
        BaselineIdentity snapshot = new BaselineIdentity("2.55.6-SNAPSHOT", null, null, null, "parity-manifest/test");
        ParityManifest manifest = new ParityManifest(snapshot, null, List.of());
        assertEquals(List.of(), ManifestValidator.validate(manifest, ManifestValidator.Options.DEFAULT));
        List<String> problems = ManifestValidator.validate(manifest, new ManifestValidator.Options(true, null))
                                                 .stream().map(ManifestProblem::message).toList();
        assertEquals(List.of("featureFreezeCutoff is required", "gitRevision is required",
                             "supportStatus is required",
                             "v2Version '2.55.6-SNAPSHOT' is not a V2 general-availability release"), problems);
        assertEquals(List.of(), ManifestValidator.validate(new ParityManifest(Entries.BASELINE, null, List.of()),
                                                           new ManifestValidator.Options(true, null)));
    }

    private static List<ManifestProblem> problems(Entries.B builder) {
        return ManifestValidator.validate(new ParityManifest(Entries.BASELINE, null, List.of(builder.build())),
                                          ManifestValidator.Options.DEFAULT);
    }
}
