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

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * Reads the {@code parity-manifest-1.schema.json} document form. Member lists, types and rules follow the schema and
 * the publisher's reader and validator (default options); see {@link MigrationManifest#read(String)}.
 */
final class ManifestReader {

    static final List<String> TOP_MEMBERS = List.of("schemaVersion", "baseline", "candidate", "summary", "entries");
    static final List<String> BASELINE_MEMBERS = List.of("v2Version", "gitRevision", "featureFreezeCutoff",
                                                         "supportStatus", "scannerVersion");
    static final List<String> CANDIDATE_MEMBERS = List.of("v3Version", "gitRevision");
    static final List<String> SOURCE_MEMBERS = List.of("artifact", "path");
    static final List<String> ENTRY_MEMBERS = List.of("id", "kind", "v2Surface", "source", "deprecated",
                                                      "classification", "dimensions", "binary", "rationale",
                                                      "differences", "v3Artifact", "replacement", "migrationKind",
                                                      "migration", "supportEvidence", "evidenceIds", "evidence");
    static final List<String> DIMENSIONS = List.of("source", "binary", "behavior", "configuration");

    /** The read entries and baseline (when readable), and every problem, sorted. */
    record Result(ManifestBaseline baseline, List<ManifestEntry> entries, List<String> problems) {
    }

    private final List<String> problems = new ArrayList<>();

    Result read(String json) {
        Node root;
        try {
            root = Node.parse(json);
        } catch (RuntimeException e) {
            return new Result(null, List.of(), List.of("$: is not JSON: " + e.getMessage()));
        }
        if (!root.isObjectNode()) {
            return new Result(null, List.of(), List.of("$: the manifest must be an object"));
        }
        Fields top = new Fields(root.expectObjectNode(), "$", TOP_MEMBERS);
        Optional<Node> version = top.member("schemaVersion");
        if (version.isEmpty() || !version.get().isNumberNode() || !version.get().expectNumberNode().isNaturalNumber()
            || version.get().expectNumberNode().getValue().longValue() != MigrationManifest.SCHEMA_VERSION) {
            top.bad("schemaVersion", "must be " + MigrationManifest.SCHEMA_VERSION);
        }
        ManifestBaseline baseline = baseline(top);
        List<ManifestEntry> entries = new ArrayList<>();
        // Every read entry, duplicates included: the publisher's summary counts each one.
        List<ManifestEntry> counted = new ArrayList<>();
        boolean complete = true;
        Optional<Node> entriesNode = top.member("entries");
        if (entriesNode.isEmpty() || !entriesNode.get().isArrayNode()) {
            top.bad("entries", "must be an array");
            complete = false;
        } else {
            int index = 0;
            Set<String> seen = new HashSet<>();
            for (Node element : entriesNode.get().expectArrayNode().getElements()) {
                ManifestEntry entry = entry(element, "entries[" + index++ + "]");
                if (entry == null) {
                    complete = false;
                    continue;
                }
                counted.add(entry);
                if (!seen.add(entry.id())) {
                    problems.add(entry.id() + ": the manifest contains this ID more than once");
                } else {
                    entries.add(entry);
                }
            }
        }
        if (complete) {
            summary(top, counted);
        }
        return new Result(baseline, entries, problems.stream().sorted().toList());
    }

    private ManifestBaseline baseline(Fields top) {
        Optional<ObjectNode> node = top.object("baseline", false);
        if (node.isEmpty()) {
            return null;
        }
        Fields f = new Fields(node.get(), "baseline", BASELINE_MEMBERS);
        String version = f.text("v2Version", true);
        String scanner = f.text("scannerVersion", true);
        String cutoff = f.string("featureFreezeCutoff");
        if (cutoff != null) {
            try {
                LocalDate.parse(cutoff);
            } catch (DateTimeParseException e) {
                f.bad("featureFreezeCutoff", "must be an ISO-8601 date");
            }
        }
        String gitRevision = f.string("gitRevision");
        String supportStatus = f.string("supportStatus");
        String candidateVersion = null;
        String candidateRevision = null;
        Optional<ObjectNode> candidate = top.object("candidate", true);
        if (candidate.isPresent()) {
            Fields c = new Fields(candidate.get(), "candidate", CANDIDATE_MEMBERS);
            candidateVersion = c.text("v3Version", true);
            candidateRevision = c.string("gitRevision");
        }
        if (version == null || scanner == null) {
            return null;
        }
        return new ManifestBaseline(version, gitRevision, cutoff, supportStatus, scanner, candidateVersion,
                                    candidateRevision);
    }

    private ManifestEntry entry(Node node, String fallback) {
        if (!node.isObjectNode()) {
            problems.add(fallback + ": an entry must be an object");
            return null;
        }
        ObjectNode o = node.expectObjectNode();
        String subject = o.getStringMember("id").map(s -> s.getValue()).orElse(fallback);
        int before = problems.size();
        Fields f = new Fields(o, subject, ENTRY_MEMBERS);
        String id = f.text("id", true);
        EntryKind kind = f.enumValue("kind", EntryKind::valueOf, true);
        String surface = f.text("v2Surface", true);
        String sourceArtifact = null;
        String sourcePath = null;
        Optional<ObjectNode> source = f.object("source", false);
        if (source.isPresent()) {
            Fields s = new Fields(source.get(), subject + " source", SOURCE_MEMBERS);
            sourceArtifact = s.text("artifact", true);
            sourcePath = s.string("path");
        }
        Boolean deprecated = f.bool("deprecated");
        Classification classification = f.enumValue("classification", Classification::valueOf, true);
        List<String> dimensions = f.uniqueStrings("dimensions");
        dimensions.stream().filter(d -> !DIMENSIONS.contains(d))
                  .forEach(d -> f.bad("dimensions", "has unknown value '" + d + "'"));
        Boolean binary = f.bool("binary");
        String rationale = f.text("rationale", true);
        List<String> differences = f.strings("differences");
        if (differences.stream().anyMatch(String::isBlank)) {
            f.bad("differences", "contains a blank difference");
        }
        String v3Artifact = f.string("v3Artifact");
        String replacement = f.string("replacement");
        MigrationKind migrationKind = f.enumValue("migrationKind", MigrationKind::valueOf, false);
        String migration = f.string("migration");
        String supportEvidence = f.string("supportEvidence");
        List<String> evidenceIds = f.uniqueStrings("evidenceIds");
        if (evidenceIds.stream().anyMatch(String::isBlank)) {
            f.bad("evidenceIds", "contains a blank ID");
        }
        String evidence = f.string("evidence");
        if (problems.size() != before || id == null || kind == null || surface == null || sourceArtifact == null
            || deprecated == null || classification == null || binary == null || rationale == null) {
            return null;
        }
        ManifestEntry entry = new ManifestEntry(id, kind, surface, sourceArtifact, sourcePath, deprecated,
                                                classification, dimensions, rationale, differences, v3Artifact,
                                                replacement, migrationKind, migration, supportEvidence, evidenceIds);
        validate(entry, binary, evidence, f);
        return problems.size() == before ? entry : null;
    }

    /** The publisher's validation rules, mirrored by the schema's {@code allOf} where JSON Schema can express them. */
    private void validate(ManifestEntry e, boolean binary, String evidence, Fields f) {
        if (!e.kind().accepts(e.id())) {
            f.bad("id", "does not follow the " + e.kind() + " ID scheme");
        }
        if (binary != e.binary()) {
            f.bad("binary", "disagrees with dimensions");
        }
        String expectedEvidence = e.evidenceIds().isEmpty() ? null : String.join(", ", e.evidenceIds());
        if (!Objects.equals(evidence, expectedEvidence)) {
            f.bad("evidence", "disagrees with evidenceIds");
        }
        if (present(e.replacement()) && EntryKind.ofId(e.replacement()).isEmpty()) {
            f.bad("replacement", "'" + e.replacement() + "' is not a well-formed entry ID");
        }
        switch (e.classification()) {
            case PRESERVED -> {
                f.require(!e.dimensions().isEmpty(), "dimensions", "PRESERVED requires at least one dimension");
                f.require(e.replacement() == null, "replacement", "PRESERVED forbids a replacement");
                f.require(e.differences().isEmpty(), "differences", "PRESERVED forbids differences");
                f.require(e.migrationKind() == null, "migrationKind", "PRESERVED forbids a migration kind");
                f.require(e.migration() == null, "migration", "PRESERVED forbids a migration action");
            }
            case ADAPTED -> {
                f.require(!e.differences().isEmpty(), "differences", "ADAPTED requires at least one difference");
                f.require(present(e.replacement()), "replacement", "ADAPTED requires a replacement");
                f.require(e.migrationKind() == MigrationKind.RECIPE || e.migrationKind() == MigrationKind.DIAGNOSTIC,
                          "migrationKind", "ADAPTED requires RECIPE or DIAGNOSTIC");
                f.require(present(e.migration()), "migration", "ADAPTED requires a migration action");
                f.require(!e.evidenceIds().isEmpty(), "evidenceIds", "ADAPTED requires verification evidence");
            }
            case EXCLUDED -> {
                f.require(e.dimensions().isEmpty(), "dimensions", "EXCLUDED forbids dimensions");
                f.require(e.deprecated() || present(e.supportEvidence()), "supportEvidence",
                          "EXCLUDED requires supportEvidence unless the V2 baseline deprecates it");
                f.require(e.migrationKind() == MigrationKind.COMPILE_TIME
                          || e.migrationKind() == MigrationKind.DIAGNOSTIC,
                          "migrationKind", "EXCLUDED requires COMPILE_TIME or DIAGNOSTIC");
                f.require(present(e.migration()), "migration", "EXCLUDED requires a migration action");
            }
            default -> throw new IllegalStateException(e.classification().name());
        }
    }

    private void summary(Fields top, List<ManifestEntry> entries) {
        Optional<ObjectNode> node = top.object("summary", false);
        if (node.isEmpty()) {
            return;
        }
        Map<Classification, Integer> expected = new EnumMap<>(Classification.class);
        for (Classification c : Classification.values()) {
            expected.put(c, 0);
        }
        entries.forEach(e -> expected.merge(e.classification(), 1, Integer::sum));
        Fields f = new Fields(node.get(), "summary", expected.keySet().stream().map(Enum::name).toList());
        expected.forEach((c, n) -> {
            Optional<Node> actual = f.member(c.name());
            if (actual.isEmpty() || !actual.get().isNumberNode()
                || actual.get().expectNumberNode().getValue().longValue() != n
                || !actual.get().expectNumberNode().isNaturalNumber()) {
                f.bad(c.name(), "must equal the " + c + " entry count " + n);
            }
        });
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }

    /** Typed member access on one JSON object; problems are recorded against {@code subject}. */
    private final class Fields {
        private final ObjectNode node;
        private final String subject;

        Fields(ObjectNode node, String subject, List<String> allowed) {
            this.node = node;
            this.subject = subject;
            node.getMembers().keySet().stream().map(k -> k.getValue()).filter(k -> !allowed.contains(k)).sorted()
                .forEach(k -> bad(k, "is not a member of the schema"));
        }

        Optional<Node> member(String name) {
            return node.getMember(name);
        }

        void bad(String member, String text) {
            problems.add(subject + ": '" + member + "' " + text);
        }

        void require(boolean condition, String member, String text) {
            if (!condition) {
                problems.add(subject + ": " + text + " ('" + member + "')");
            }
        }

        /** A string or {@code null}; absent and JSON {@code null} both read as {@code null}. */
        String string(String name) {
            Optional<Node> value = member(name);
            if (value.isEmpty() || value.get().isNullNode()) {
                return null;
            }
            if (!value.get().isStringNode()) {
                bad(name, "must be a string or null");
                return null;
            }
            return value.get().expectStringNode().getValue();
        }

        /** A non-blank string; with {@code required}, absence is a problem. */
        String text(String name, boolean required) {
            String value = string(name);
            if (value == null || value.isBlank()) {
                if (required && member(name).filter(n -> !n.isNullNode() && !n.isStringNode()).isEmpty()) {
                    bad(name, "is required");
                }
                return null;
            }
            return value;
        }

        Boolean bool(String name) {
            Optional<Node> value = member(name);
            if (value.isEmpty() || !value.get().isBooleanNode()) {
                bad(name, "is required and must be a boolean");
                return null;
            }
            return value.get().expectBooleanNode().getValue();
        }

        <E extends Enum<E>> E enumValue(String name, Function<String, E> parse, boolean required) {
            String value = string(name);
            if (value == null) {
                if (required) {
                    bad(name, "is required");
                }
                return null;
            }
            try {
                return parse.apply(value);
            } catch (IllegalArgumentException e) {
                bad(name, "has unknown value '" + value + "'");
                return null;
            }
        }

        Optional<ObjectNode> object(String name, boolean nullable) {
            Optional<Node> value = member(name);
            if (nullable && (value.isEmpty() || value.get().isNullNode())) {
                return Optional.empty();
            }
            if (value.isEmpty() || !value.get().isObjectNode()) {
                bad(name, nullable ? "must be an object or null" : "is required and must be an object");
                return Optional.empty();
            }
            return Optional.of(value.get().expectObjectNode());
        }

        /** An array of strings; absent reads as empty. */
        List<String> strings(String name) {
            Optional<Node> value = member(name);
            if (value.isEmpty()) {
                return List.of();
            }
            if (!value.get().isArrayNode()) {
                bad(name, "must be an array of strings");
                return List.of();
            }
            List<String> result = new ArrayList<>();
            for (Node element : value.get().expectArrayNode().getElements()) {
                if (!element.isStringNode()) {
                    bad(name, "must be an array of strings");
                    return List.of();
                }
                result.add(element.expectStringNode().getValue());
            }
            return result;
        }

        List<String> uniqueStrings(String name) {
            List<String> values = strings(name);
            if (new HashSet<>(values).size() != values.size()) {
                bad(name, "must not contain duplicates");
            }
            return values;
        }
    }
}
