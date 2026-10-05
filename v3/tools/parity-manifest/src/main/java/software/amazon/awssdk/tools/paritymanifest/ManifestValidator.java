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

import static software.amazon.awssdk.tools.paritymanifest.ManifestProblem.Code.BASELINE;
import static software.amazon.awssdk.tools.paritymanifest.ManifestProblem.Code.DUPLICATE_ID;
import static software.amazon.awssdk.tools.paritymanifest.ManifestProblem.Code.FORBIDDEN_FIELD;
import static software.amazon.awssdk.tools.paritymanifest.ManifestProblem.Code.INVALID_ID;
import static software.amazon.awssdk.tools.paritymanifest.ManifestProblem.Code.INVALID_VALUE;
import static software.amazon.awssdk.tools.paritymanifest.ManifestProblem.Code.MISSING_FIELD;
import static software.amazon.awssdk.tools.paritymanifest.ManifestProblem.Code.UNKNOWN_REPLACEMENT;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Checks a {@link ParityManifest} against the requirements' required-field rules and reports every problem at once.
 *
 * <p>Per entry (Requirements 16.2, 16.3, 16.11, 16.13, 17.1-17.4):
 * <ul>
 *   <li>all: the ID follows the kind's scheme; {@code v2Surface}, {@code source.artifact} and {@code rationale} are
 *       present (glossary: every entry has a rationale); a {@code replacement} is a well-formed entry ID and, when a
 *       candidate inventory is supplied, resolves in it (Property 57 hook); evidence IDs are not blank;</li>
 *   <li>{@code PRESERVED}: at least one compatibility dimension; no replacement, differences or migration action
 *       (a preserved entry needs only JDK and dependency-version changes, 17.3);</li>
 *   <li>{@code ADAPTED} (16.11, 17.4): at least one difference, a replacement, a {@code RECIPE} or {@code DIAGNOSTIC}
 *       migration action with text, and at least one verification evidence ID;</li>
 *   <li>{@code EXCLUDED} (16.13): no compatibility dimension; deprecation or support evidence (the V2 deprecation
 *       marker or {@code supportEvidence}); a {@code COMPILE_TIME} or {@code DIAGNOSTIC} migration action with text;
 *       the replacement is optional ("when one exists").</li>
 * </ul>
 * Per manifest: IDs are unique; with {@link Options#requireFrozenBaseline()} the baseline is a V2 GA release with a
 * feature-freeze cutoff, support status and source revision (16.1).
 */
public final class ManifestValidator {
    /**
     * Validation options.
     *
     * @param requireFrozenBaseline require a complete Launch Parity Baseline identity (16.1)
     * @param candidateIds          the release candidate's artifact/API inventory IDs, or {@code null} to skip
     *                              replacement resolution
     */
    public record Options(boolean requireFrozenBaseline, Set<String> candidateIds) {
        /** No frozen-baseline requirement and no candidate inventory. */
        public static final Options DEFAULT = new Options(false, null);

        public Options {
            candidateIds = candidateIds == null ? null : Set.copyOf(candidateIds);
        }
    }

    private ManifestValidator() {
    }

    /** Every problem of {@code manifest}, sorted. */
    public static List<ManifestProblem> validate(ParityManifest manifest, Options options) {
        List<ManifestProblem> problems = new ArrayList<>();
        validateBaseline(manifest.baseline(), options, problems);
        Set<String> seen = new HashSet<>();
        Set<String> reported = new HashSet<>();
        for (ManifestEntry entry : manifest.entries()) {
            if (!seen.add(entry.id()) && reported.add(entry.id())) {
                problems.add(new ManifestProblem(DUPLICATE_ID, entry.id(), "the manifest contains this ID more than once"));
            }
            validateEntry(entry, options, problems);
        }
        return problems.stream().sorted().toList();
    }

    /** Throws with every problem of {@code manifest}, if any. */
    public static void check(ParityManifest manifest, Options options) throws ManifestValidationException {
        ManifestValidationException.throwIfAny(validate(manifest, options));
    }

    /** Appends the problems of one entry to {@code problems}. */
    static void validateEntry(ManifestEntry e, Options options, List<ManifestProblem> problems) {
        String id = e.id();
        if (!e.kind().accepts(id)) {
            problems.add(new ManifestProblem(INVALID_ID, id, "does not follow the " + e.kind() + " ID scheme"));
        }
        require(e.v2Surface(), "v2Surface", e, problems);
        require(e.source().artifact(), "source.artifact", e, problems);
        require(e.rationale(), "rationale", e, problems);
        if (present(e.replacement())) {
            if (EntryKind.ofId(e.replacement()).isEmpty()) {
                problems.add(new ManifestProblem(INVALID_VALUE, id, "replacement '" + e.replacement()
                                                                    + "' is not a well-formed entry ID"));
            } else if (options.candidateIds() != null && !options.candidateIds().contains(e.replacement())) {
                problems.add(new ManifestProblem(UNKNOWN_REPLACEMENT, id, "replacement '" + e.replacement()
                                                                          + "' is not in the candidate inventory"));
            }
        }
        if (e.evidenceIds().stream().anyMatch(String::isBlank)) {
            problems.add(new ManifestProblem(INVALID_VALUE, id, "evidenceIds contains a blank ID"));
        }
        if (e.differences().stream().anyMatch(d -> d == null || d.isBlank())) {
            problems.add(new ManifestProblem(INVALID_VALUE, id, "differences contains a blank difference"));
        }
        switch (e.classification()) {
            case PRESERVED -> {
                if (e.dimensions().isEmpty()) {
                    missing("dimensions", e, problems);
                }
                forbid(e.replacement() != null, "replacement", e, problems);
                forbid(!e.differences().isEmpty(), "differences", e, problems);
                forbid(e.migrationKind() != null, "migrationKind", e, problems);
                forbid(e.migration() != null, "migration", e, problems);
            }
            case ADAPTED -> {
                if (e.differences().isEmpty()) {
                    missing("differences", e, problems);
                }
                require(e.replacement(), "replacement", e, problems);
                requireMigration(e, MigrationKind.RECIPE, MigrationKind.DIAGNOSTIC, problems);
                if (e.evidenceIds().isEmpty()) {
                    missing("evidenceIds", e, problems);
                }
            }
            case EXCLUDED -> {
                forbid(!e.dimensions().isEmpty(), "dimensions", e, problems);
                if (!e.deprecated() && !present(e.supportEvidence())) {
                    problems.add(new ManifestProblem(MISSING_FIELD, id, "EXCLUDED requires supportEvidence "
                                                                        + "unless the V2 baseline deprecates it"));
                }
                requireMigration(e, MigrationKind.COMPILE_TIME, MigrationKind.DIAGNOSTIC, problems);
            }
            default -> throw new IllegalStateException(e.classification().name());
        }
    }

    private static void validateBaseline(BaselineIdentity b, Options options, List<ManifestProblem> problems) {
        if (b.v2Version().isBlank()) {
            problems.add(new ManifestProblem(BASELINE, "baseline", "v2Version is blank"));
        }
        if (b.scannerVersion().isBlank()) {
            problems.add(new ManifestProblem(BASELINE, "baseline", "scannerVersion is blank"));
        }
        if (!options.requireFrozenBaseline()) {
            return;
        }
        if (!b.isGeneralAvailabilityRelease()) {
            problems.add(new ManifestProblem(BASELINE, "baseline", "v2Version '" + b.v2Version()
                                                                   + "' is not a V2 general-availability release"));
        }
        if (b.featureFreezeCutoff() == null) {
            problems.add(new ManifestProblem(BASELINE, "baseline", "featureFreezeCutoff is required"));
        }
        if (!present(b.supportStatus())) {
            problems.add(new ManifestProblem(BASELINE, "baseline", "supportStatus is required"));
        }
        if (!present(b.gitRevision())) {
            problems.add(new ManifestProblem(BASELINE, "baseline", "gitRevision is required"));
        }
    }

    private static void requireMigration(ManifestEntry e, MigrationKind first, MigrationKind second,
                                         List<ManifestProblem> problems) {
        if (e.migrationKind() == null) {
            missing("migrationKind", e, problems);
        } else if (e.migrationKind() != first && e.migrationKind() != second) {
            problems.add(new ManifestProblem(INVALID_VALUE, e.id(), e.classification() + " migrationKind must be "
                                                                    + first + " or " + second + ", not "
                                                                    + e.migrationKind()));
        }
        require(e.migration(), "migration", e, problems);
    }

    private static void require(String value, String field, ManifestEntry e, List<ManifestProblem> problems) {
        if (!present(value)) {
            missing(field, e, problems);
        }
    }

    private static void missing(String field, ManifestEntry e, List<ManifestProblem> problems) {
        problems.add(new ManifestProblem(MISSING_FIELD, e.id(), e.classification() + " requires " + field));
    }

    private static void forbid(boolean present, String field, ManifestEntry e, List<ManifestProblem> problems) {
        if (present) {
            problems.add(new ManifestProblem(FORBIDDEN_FIELD, e.id(), e.classification() + " forbids " + field));
        }
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }
}
