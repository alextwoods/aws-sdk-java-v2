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

import static software.amazon.awssdk.tools.paritymanifest.LaunchProblem.Code.CANDIDATE_INCOMPLETE;
import static software.amazon.awssdk.tools.paritymanifest.LaunchProblem.Code.CANDIDATE_MISMATCH;
import static software.amazon.awssdk.tools.paritymanifest.LaunchProblem.Code.EXCLUDED_NOT_DEPRECATED;
import static software.amazon.awssdk.tools.paritymanifest.LaunchProblem.Code.FAILED_CANDIDATE_EVIDENCE;
import static software.amazon.awssdk.tools.paritymanifest.LaunchProblem.Code.INCONSISTENT_CANDIDATE_EVIDENCE;
import static software.amazon.awssdk.tools.paritymanifest.LaunchProblem.Code.INVALID_MANIFEST;
import static software.amazon.awssdk.tools.paritymanifest.LaunchProblem.Code.MISSING_CANDIDATE_EVIDENCE;
import static software.amazon.awssdk.tools.paritymanifest.LaunchProblem.Code.MISSING_EVIDENCE_HOOK;
import static software.amazon.awssdk.tools.paritymanifest.LaunchProblem.Code.NOT_SHIPPED;
import static software.amazon.awssdk.tools.paritymanifest.LaunchProblem.Code.OWNER_NOT_SHIPPED;
import static software.amazon.awssdk.tools.paritymanifest.LaunchProblem.Code.REPLACEMENT_NOT_SHIPPED;
import static software.amazon.awssdk.tools.paritymanifest.LaunchProblem.Code.UNBOUND_CANDIDATE_EVIDENCE;
import static software.amazon.awssdk.tools.paritymanifest.LaunchProblem.Code.UNKNOWN_EVIDENCE_ENTRY;
import static software.amazon.awssdk.tools.paritymanifest.LaunchProblem.Code.UNKNOWN_EVIDENCE_HOOK;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import software.amazon.awssdk.tools.paritymanifest.CandidateEvidence.EvidenceRecord;
import software.amazon.awssdk.tools.paritymanifest.CandidateEvidence.Outcome;
import software.amazon.awssdk.tools.paritymanifest.CandidateInventory.CandidateArtifact;

/**
 * The launch-surface and behavior inventory gate (task 13.3; Requirements 16.4, 16.5, 16.9, 16.10, 16.12, 16.14,
 * 16.15): compares what a V3 release candidate ships ({@link CandidateInventory}) and the verification hooks it
 * registers ({@link EvidenceHooks}) with every Parity Manifest entry, one {@link LaunchCategory} at a time, and
 * reports every reason the candidate may not be packaged or released.
 *
 * <p><b>{@link Stage#PACKAGING}</b> blocks building a release candidate. For every entry:
 * <ul>
 *   <li>a non-deprecated {@code PRESERVED} entry's capability ships ({@link LaunchProblem.Code#NOT_SHIPPED}):
 *       {@code service:}, {@code http-mode:}, {@code artifact:} and {@code api:} type IDs exactly; {@code api:} member
 *       IDs exactly when the entry claims the binary dimension, otherwise any member of the same type with the same
 *       name and member kind ({@link CandidateInventory#shipsMemberNamed}; descriptor compatibility is the generated
 *       compile fixtures' job); {@code behavior:} and {@code config:} IDs when a candidate artifact declares them
 *       ({@link CandidateInventoryScanner#CAPABILITIES_RESOURCE}); {@link LaunchCategory#MODULE_PATH} behaviors
 *       structurally, when every candidate artifact has an explicit or automatic module name;</li>
 *   <li>a named replacement ships, exactly, whatever the classification and deprecation (16.12;
 *       {@link LaunchProblem.Code#REPLACEMENT_NOT_SHIPPED}), so no {@code ADAPTED} migration or {@code EXCLUDED}
 *       diagnostic points at a missing V3 API;</li>
 *   <li>the owning V3 artifact of a {@code PRESERVED} or {@code ADAPTED} entry is part of the candidate
 *       ({@link LaunchProblem.Code#OWNER_NOT_SHIPPED});</li>
 *   <li>a non-deprecated {@code PRESERVED} or {@code ADAPTED} entry has at least one evidence hook that resolves
 *       ({@link LaunchProblem.Code#MISSING_EVIDENCE_HOOK}); every evidence ID the manifest names is a registered hook
 *       that exists in the candidate, every registered hook exists in the candidate, and every hook binding names a
 *       manifest entry ({@link LaunchProblem.Code#UNKNOWN_EVIDENCE_HOOK},
 *       {@link LaunchProblem.Code#UNKNOWN_EVIDENCE_ENTRY});</li>
 *   <li>the manifest passes {@link ManifestValidator} ({@link LaunchProblem.Code#INVALID_MANIFEST}), and every
 *       expected release artifact is part of the candidate ({@link LaunchProblem.Code#CANDIDATE_INCOMPLETE}).</li>
 * </ul>
 *
 * <p><b>{@link Stage#GENERAL_AVAILABILITY}</b> additionally requires (16.14, 16.15) a frozen baseline, a candidate
 * identity shared by the manifest, the evidence and every versioned artifact, no non-deprecated {@code EXCLUDED}
 * entry, and for every non-deprecated {@code PRESERVED} or {@code ADAPTED} entry a {@code PASSED} record with equal
 * expected and actual outcomes for each of its hooks, and no failed, skipped or unbound record.
 *
 * <p>Deprecated entries are counted but not gated (16.15 gates non-deprecated entries), except for their
 * replacements. Evaluation is deterministic: the report lists problems sorted.
 */
public final class LaunchGate {

    /** The release step a gate run decides. */
    public enum Stage {
        /** Building (packaging) a release candidate. */
        PACKAGING("packaging"),
        /** Declaring a packaged candidate generally available; includes every packaging check. */
        GENERAL_AVAILABILITY("general-availability");

        private final String cliName;

        Stage(String cliName) {
            this.cliName = cliName;
        }

        /** The command-line and Maven property spelling of the stage. */
        public String cliName() {
            return cliName;
        }

        /** The stage spelled {@code name} ({@link #cliName()}, case-insensitive). */
        public static Stage fromCliName(String name) {
            for (Stage s : values()) {
                if (s.cliName.equals(name.strip().toLowerCase(Locale.ROOT))) {
                    return s;
                }
            }
            throw new IllegalArgumentException("Unknown launch gate stage '" + name + "'; expected packaging or "
                                               + "general-availability");
        }
    }

    private final ParityManifest manifest;
    private final CandidateInventory candidate;
    private final EvidenceHooks hooks;
    private final CandidateEvidence evidence;
    private final Set<String> expectedArtifacts;
    private final Set<String> availableEvidence;

    private LaunchGate(Builder b) {
        this.manifest = Objects.requireNonNull(b.manifest, "manifest");
        this.candidate = Objects.requireNonNull(b.candidate, "candidate");
        this.hooks = b.hooks == null ? EvidenceHooks.NONE : b.hooks;
        this.evidence = b.evidence;
        this.expectedArtifacts = b.expectedArtifacts == null ? null : new TreeSet<>(b.expectedArtifacts);
        this.availableEvidence = b.availableEvidence == null ? null : Set.copyOf(b.availableEvidence);
    }

    /** A gate builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** Evaluates {@code stage}: every problem whose stage is {@code stage} or earlier. */
    public LaunchGateReport evaluate(Stage stage) {
        Run run = new Run(stage);
        run.manifest();
        run.candidate();
        run.hooks();
        Map<String, List<EvidenceRecord>> records = run.evidence();
        Map<LaunchCategory, int[]> counts = new EnumMap<>(LaunchCategory.class);
        for (ManifestEntry entry : manifest.entries()) {
            run.entry(entry, records.getOrDefault(entry.id(), List.of()),
                      counts.computeIfAbsent(LaunchCategory.of(entry), k -> new int[Counter.values().length]));
        }
        List<LaunchGateReport.CategorySummary> summaries = new ArrayList<>();
        counts.forEach((category, c) -> summaries.add(new LaunchGateReport.CategorySummary(
            category, c[Counter.DEPRECATED.ordinal()], c[Counter.ENTRIES.ordinal()], c[Counter.PRESERVED.ordinal()],
            c[Counter.ADAPTED.ordinal()], c[Counter.EXCLUDED.ordinal()], c[Counter.SHIPPED.ordinal()],
            c[Counter.HOOKED.ordinal()], c[Counter.VERIFIED.ordinal()], c[Counter.BLOCKED.ordinal()])));
        return new LaunchGateReport(stage, manifest.candidate(), summaries, run.problems);
    }

    private enum Counter {
        DEPRECATED, ENTRIES, PRESERVED, ADAPTED, EXCLUDED, SHIPPED, HOOKED, VERIFIED, BLOCKED
    }

    /** The state of one evaluation. */
    private final class Run {
        private final Stage stage;
        private final List<LaunchProblem> problems = new ArrayList<>();
        private final Map<String, LaunchCategory> categories = new HashMap<>();
        private final List<String> unnamedModules;

        Run(Stage stage) {
            this.stage = stage;
            for (ManifestEntry e : manifest.entries()) {
                categories.put(e.id(), LaunchCategory.of(e));
            }
            unnamedModules = candidate.artifacts().values().stream().filter(a -> a.moduleName() == null)
                                      .map(CandidateArtifact::coordinates).toList();
        }

        /** Adds a problem if it blocks the evaluated stage; returns whether it did. */
        boolean add(LaunchProblem.Code code, Stage blocks, String subject, String message) {
            if (blocks.compareTo(stage) > 0) {
                return false;
            }
            problems.add(new LaunchProblem(code, blocks, subject, categories.get(subject), message));
            return true;
        }

        void manifest() {
            List<ManifestProblem> packaging = ManifestValidator.validate(manifest, ManifestValidator.Options.DEFAULT);
            packaging.forEach(p -> add(INVALID_MANIFEST, Stage.PACKAGING, subject(p), p.code() + ": " + p.message()));
            Set<ManifestProblem> reported = new HashSet<>(packaging);
            for (ManifestProblem p : ManifestValidator.validate(manifest,
                                                                new ManifestValidator.Options(true, null))) {
                if (!reported.contains(p)) {
                    add(INVALID_MANIFEST, Stage.GENERAL_AVAILABILITY, subject(p), p.code() + ": " + p.message());
                }
            }
        }

        private String subject(ManifestProblem p) {
            return categories.containsKey(p.subject()) ? p.subject() : "manifest";
        }

        void candidate() {
            if (expectedArtifacts != null) {
                for (String coordinates : expectedArtifacts) {
                    if (!candidate.hasArtifact(coordinates)) {
                        add(CANDIDATE_INCOMPLETE, Stage.PACKAGING, "candidate", "release artifact " + coordinates
                                                                                + " is not part of the candidate");
                    }
                }
            }
            CandidateIdentity identity = manifest.candidate();
            if (identity == null) {
                add(CANDIDATE_MISMATCH, Stage.GENERAL_AVAILABILITY, "candidate",
                    "the manifest records no release candidate identity");
                return;
            }
            for (CandidateArtifact a : candidate.artifacts().values()) {
                if (a.version() != null && !a.version().equals(identity.v3Version())) {
                    add(CANDIDATE_MISMATCH, Stage.GENERAL_AVAILABILITY, "candidate",
                        a.coordinates() + " is version " + a.version() + ", not the candidate version "
                        + identity.v3Version() + " (" + a.location() + ")");
                }
            }
        }

        void hooks() {
            if (!hooks.registered()) {
                return;
            }
            hooks.hooks().forEach((hook, entries) -> entries.stream().filter(e -> !categories.containsKey(e)).forEach(
                e -> add(UNKNOWN_EVIDENCE_ENTRY, Stage.PACKAGING, "evidence",
                         "evidence hook " + hook + " is bound to " + e + ", which the manifest does not contain")));
            if (availableEvidence != null) {
                for (String hook : hooks.hooks().keySet()) {
                    if (!availableEvidence.contains(hook)) {
                        add(UNKNOWN_EVIDENCE_HOOK, Stage.PACKAGING, "evidence", "evidence hook " + hook
                                                                                + " is registered but the candidate "
                                                                                + "has no such verification");
                    }
                }
            }
        }

        /** Candidate records by entry ID; reports candidate-level evidence problems. */
        Map<String, List<EvidenceRecord>> evidence() {
            Map<String, List<EvidenceRecord>> byEntry = new HashMap<>();
            if (stage != Stage.GENERAL_AVAILABILITY) {
                return byEntry;
            }
            if (evidence == null) {
                add(MISSING_CANDIDATE_EVIDENCE, Stage.GENERAL_AVAILABILITY, "evidence",
                    "no candidate evidence was supplied");
                return byEntry;
            }
            if (manifest.candidate() != null && !evidence.candidate().equals(manifest.candidate())) {
                add(CANDIDATE_MISMATCH, Stage.GENERAL_AVAILABILITY, "candidate",
                    "the evidence is for candidate " + describe(evidence.candidate()) + ", the manifest for "
                    + describe(manifest.candidate()));
            }
            for (EvidenceRecord r : evidence.records()) {
                if (!categories.containsKey(r.entryId())) {
                    add(UNKNOWN_EVIDENCE_ENTRY, Stage.GENERAL_AVAILABILITY, "evidence",
                        "evidence " + r.evidenceId() + " records " + r.entryId() + ", which the manifest does not "
                        + "contain");
                } else {
                    byEntry.computeIfAbsent(r.entryId(), k -> new ArrayList<>()).add(r);
                }
            }
            return byEntry;
        }

        void entry(ManifestEntry e, List<EvidenceRecord> records, int[] count) {
            String id = e.id();
            int before = problems.size();
            boolean ships = e.classification() != Classification.EXCLUDED;
            // Only hooks that resolve count: an unregistered or absent verification is no evidence hook.
            SortedSet<String> entryHooks = hooks.of(e).stream().filter(this::usable)
                                                .collect(Collectors.toCollection(TreeSet::new));
            for (String evidenceId : e.evidenceIds()) {
                if (hooks.registered() && !hooks.known(evidenceId)) {
                    add(UNKNOWN_EVIDENCE_HOOK, Stage.PACKAGING, id, "evidence ID " + evidenceId
                                                                    + " is not a registered evidence hook");
                } else if (availableEvidence != null && !availableEvidence.contains(evidenceId)) {
                    add(UNKNOWN_EVIDENCE_HOOK, Stage.PACKAGING, id, "evidence ID " + evidenceId
                                                                    + " names no verification in the candidate");
                }
            }
            if (present(e.replacement()) && !candidate.ships(e.replacement())) {
                add(REPLACEMENT_NOT_SHIPPED, Stage.PACKAGING, id, e.classification() + " replacement "
                                                                  + e.replacement() + " is not shipped by the "
                                                                  + "candidate");
            }
            if (ships && present(e.v3Artifact()) && !candidate.ships(EntryKind.ARTIFACT.prefix() + e.v3Artifact())) {
                add(OWNER_NOT_SHIPPED, Stage.PACKAGING, id, "owning V3 artifact " + e.v3Artifact()
                                                            + " is not part of the candidate");
            }
            if (e.deprecated()) {
                count[Counter.DEPRECATED.ordinal()]++;
                if (problems.size() > before) {
                    count[Counter.BLOCKED.ordinal()]++;
                }
                return;
            }
            count[Counter.ENTRIES.ordinal()]++;
            boolean shipped = false;
            switch (e.classification()) {
                case PRESERVED -> {
                    count[Counter.PRESERVED.ordinal()]++;
                    String missing = missingCapability(e);
                    shipped = missing == null;
                    if (!shipped) {
                        add(NOT_SHIPPED, Stage.PACKAGING, id, missing);
                    }
                }
                case ADAPTED -> {
                    count[Counter.ADAPTED.ordinal()]++;
                    shipped = present(e.replacement()) && candidate.ships(e.replacement());
                }
                case EXCLUDED -> {
                    count[Counter.EXCLUDED.ordinal()]++;
                    add(EXCLUDED_NOT_DEPRECATED, Stage.GENERAL_AVAILABILITY, id,
                        "the non-deprecated V2 surface is EXCLUDED, so V3 ships no capability for it");
                }
                default -> throw new IllegalStateException(e.classification().name());
            }
            if (shipped) {
                count[Counter.SHIPPED.ordinal()]++;
            }
            if (ships) {
                if (entryHooks.isEmpty()) {
                    add(MISSING_EVIDENCE_HOOK, Stage.PACKAGING, id, "no evidence hook verifies this "
                                                                    + e.classification() + " entry");
                } else {
                    count[Counter.HOOKED.ordinal()]++;
                }
                if (stage == Stage.GENERAL_AVAILABILITY && verify(e, entryHooks, records) && shipped) {
                    count[Counter.VERIFIED.ordinal()]++;
                }
            }
            if (problems.size() > before) {
                count[Counter.BLOCKED.ordinal()]++;
            }
        }

        /** True if {@code hook} is registered (when there is a registry) and exists (when that is known). */
        private boolean usable(String hook) {
            return (!hooks.registered() || hooks.known(hook))
                   && (availableEvidence == null || availableEvidence.contains(hook));
        }

        /** Checks the candidate records of a shipped entry; returns whether every hook passed. */
        private boolean verify(ManifestEntry e, SortedSet<String> entryHooks, List<EvidenceRecord> records) {
            if (evidence == null) {
                add(MISSING_CANDIDATE_EVIDENCE, Stage.GENERAL_AVAILABILITY, e.id(), "no candidate evidence");
                return false;
            }
            boolean passed = !entryHooks.isEmpty();
            Set<String> recorded = new HashSet<>();
            for (EvidenceRecord r : records) {
                recorded.add(r.evidenceId());
                if (!entryHooks.contains(r.evidenceId())) {
                    passed = false;
                    add(UNBOUND_CANDIDATE_EVIDENCE, Stage.GENERAL_AVAILABILITY, e.id(),
                        "evidence " + r.evidenceId() + " is not an evidence hook of this entry " + entryHooks);
                } else if (r.outcome() != Outcome.PASSED) {
                    passed = false;
                    add(FAILED_CANDIDATE_EVIDENCE, Stage.GENERAL_AVAILABILITY, e.id(),
                        "evidence " + r.evidenceId() + " " + r.outcome() + ": expected '" + r.expected()
                        + "', actual '" + r.actual() + "'");
                } else if (r.expected().isBlank() || !r.expected().equals(r.actual())) {
                    passed = false;
                    add(INCONSISTENT_CANDIDATE_EVIDENCE, Stage.GENERAL_AVAILABILITY, e.id(),
                        "evidence " + r.evidenceId() + " PASSED with expected '" + r.expected() + "' and actual '"
                        + r.actual() + "'");
                }
            }
            for (String hook : entryHooks) {
                if (!recorded.contains(hook)) {
                    passed = false;
                    add(MISSING_CANDIDATE_EVIDENCE, Stage.GENERAL_AVAILABILITY, e.id(),
                        "evidence hook " + hook + " has no candidate record");
                }
            }
            return passed;
        }

        /** Why the candidate lacks a PRESERVED entry's capability, or {@code null} if it ships it. */
        private String missingCapability(ManifestEntry e) {
            String id = e.id();
            return switch (e.kind()) {
                case PUBLIC_MEMBER -> {
                    if (candidate.ships(id) || (!e.binary() && candidate.shipsMemberNamed(id))) {
                        yield null;
                    }
                    yield e.binary() ? "the candidate ships no member with this exact JVM descriptor (binary claim)"
                                     : "the candidate ships no member of " + LaunchCategory.typeName(id)
                                       + " with this name";
                }
                case BEHAVIOR -> {
                    if (categories.get(id) == LaunchCategory.MODULE_PATH) {
                        if (candidate.artifacts().isEmpty()) {
                            yield "the candidate has no artifacts to put on the module path";
                        }
                        yield unnamedModules.isEmpty() ? null
                                                       : "candidate artifacts without a module name: " + unnamedModules;
                    }
                    yield candidate.ships(id) ? null : "no candidate artifact declares this behavior in "
                                                       + CandidateInventoryScanner.CAPABILITIES_RESOURCE;
                }
                case CONFIGURATION -> candidate.ships(id) ? null
                                                          : "no candidate artifact declares this configuration "
                                                            + "setting in "
                                                            + CandidateInventoryScanner.CAPABILITIES_RESOURCE;
                case SERVICE -> candidate.ships(id) ? null
                                                    : "no candidate artifact holds a generated service for this V2 "
                                                      + "service module";
                case HTTP_MODE -> candidate.ships(id) ? null
                                                      : "no candidate artifact registers a V3 HTTP provider for this "
                                                        + "implementation and mode";
                case ARTIFACT -> candidate.ships(id) ? null : "the candidate has no artifact with these coordinates";
                case PUBLIC_TYPE -> candidate.ships(id) ? null : "the candidate ships no public type with this name";
            };
        }
    }

    private static String describe(CandidateIdentity c) {
        return c.v3Version() + (c.gitRevision() == null ? "" : "@" + c.gitRevision());
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }

    /** Collects the gate inputs. */
    public static final class Builder {
        private ParityManifest manifest;
        private CandidateInventory candidate;
        private EvidenceHooks hooks;
        private CandidateEvidence evidence;
        private Set<String> expectedArtifacts;
        private Set<String> availableEvidence;

        private Builder() {
        }

        /** The Parity Manifest to gate against (required). */
        public Builder manifest(ParityManifest manifest) {
            this.manifest = manifest;
            return this;
        }

        /** What the candidate ships (required). */
        public Builder candidate(CandidateInventory candidate) {
            this.candidate = candidate;
            return this;
        }

        /** The registered evidence hooks; default {@link EvidenceHooks#NONE}. */
        public Builder hooks(EvidenceHooks hooks) {
            this.hooks = hooks;
            return this;
        }

        /** The candidate evidence {@link Stage#GENERAL_AVAILABILITY} requires; default none. */
        public Builder evidence(CandidateEvidence evidence) {
            this.evidence = evidence;
            return this;
        }

        /**
         * The {@code groupId:artifactId} of every release artifact the candidate must contain (for example the
         * entries of the V3 BOM); default unchecked.
         */
        public Builder expectedArtifacts(Set<String> expectedArtifacts) {
            this.expectedArtifacts = expectedArtifacts;
            return this;
        }

        /**
         * The evidence IDs that exist as verifications in the candidate (for example its test class names); when set,
         * every registered hook and every manifest evidence ID must be one of them. Default unchecked.
         */
        public Builder availableEvidence(Set<String> availableEvidence) {
            this.availableEvidence = availableEvidence;
            return this;
        }

        public LaunchGate build() {
            return new LaunchGate(this);
        }
    }
}
