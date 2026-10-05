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

import static software.amazon.awssdk.tools.paritymanifest.ManifestProblem.Code.MALFORMED;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * The deterministic JSON form of a {@link ParityManifest} (schema {@link #SCHEMA_RESOURCE}).
 *
 * <p>Output is byte-reproducible: fixed member order, entries sorted by ID, sets sorted, every optional member
 * written (as {@code null} or {@code []} when empty), four-space indentation, UTF-8, and a trailing newline. No
 * timestamps are written.
 *
 * <p>The entry form keeps the members of the former hand-written fixture manifest
 * ({@code id}, {@code v2Surface}, {@code v3Artifact}, {@code classification}, {@code binary}, {@code replacement},
 * {@code migration}, {@code evidence}); {@code binary} and {@code evidence} are derived from {@code dimensions} and
 * {@code evidenceIds}, and the reader rejects documents where they disagree. Compile and linkage callers are not
 * manifest data; they are generated from it ({@code compat.CompatibilityFixtureGenerator}).
 */
public final class ManifestJson {
    /** The classpath location of the manifest JSON schema. */
    public static final String SCHEMA_RESOURCE = "/software/amazon/awssdk/tools/paritymanifest/parity-manifest-1.schema.json";

    static final List<String> TOP_MEMBERS = List.of("schemaVersion", "baseline", "candidate", "summary", "entries");
    static final List<String> BASELINE_MEMBERS = List.of("v2Version", "gitRevision", "featureFreezeCutoff",
                                                         "supportStatus", "scannerVersion");
    static final List<String> CANDIDATE_MEMBERS = List.of("v3Version", "gitRevision");
    static final List<String> SOURCE_MEMBERS = List.of("artifact", "path");
    static final List<String> ENTRY_MEMBERS = List.of("id", "kind", "v2Surface", "source", "deprecated",
                                                      "classification", "dimensions", "binary", "rationale",
                                                      "differences", "v3Artifact", "replacement", "migrationKind",
                                                      "migration", "supportEvidence", "evidenceIds", "evidence");

    private ManifestJson() {
    }

    /** The UTF-8 bytes of {@code manifest}'s canonical JSON form. */
    public static byte[] write(ParityManifest manifest) {
        return toJson(manifest).getBytes(StandardCharsets.UTF_8);
    }

    /** Writes {@code manifest} to {@code path}, creating parent directories. */
    public static void write(ParityManifest manifest, Path path) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(path, write(manifest));
    }

    /** The canonical JSON form of {@code manifest}, ending with a newline. */
    public static String toJson(ParityManifest manifest) {
        ObjectNode.Builder top = Node.objectNodeBuilder()
                                     .withMember("schemaVersion", ParityManifest.SCHEMA_VERSION)
                                     .withMember("baseline", baseline(manifest.baseline()))
                                     .withMember("candidate", candidate(manifest.candidate()));
        ObjectNode.Builder summary = Node.objectNodeBuilder();
        manifest.summary().forEach((c, n) -> summary.withMember(c.name(), n));
        top.withMember("summary", summary.build());
        top.withMember("entries", manifest.entries().stream().map(ManifestJson::entry).collect(ArrayNode.collect()));
        return Node.prettyPrintJson(top.build()) + "\n";
    }

    /** Reads and validates a manifest, reporting every malformed member and validation problem at once. */
    public static ParityManifest read(Path path) throws IOException, ManifestValidationException {
        return read(Files.readString(path, StandardCharsets.UTF_8));
    }

    /** Reads and validates a manifest, reporting every malformed member and validation problem at once. */
    public static ParityManifest read(String json) throws ManifestValidationException {
        List<ManifestProblem> problems = new ArrayList<>();
        Node root;
        try {
            root = Node.parse(json);
        } catch (RuntimeException e) {
            throw new ManifestValidationException(List.of(new ManifestProblem(MALFORMED, "$", e.getMessage())));
        }
        ObjectNode top = JsonFields.object(root, "$", "the manifest", problems);
        if (top == null) {
            throw new ManifestValidationException(problems);
        }
        JsonFields f = new JsonFields(top, "$", Set.copyOf(TOP_MEMBERS), problems);
        Integer version = f.integer("schemaVersion");
        if (version == null || version != ParityManifest.SCHEMA_VERSION) {
            f.bad("schemaVersion", "must be " + ParityManifest.SCHEMA_VERSION);
        }
        BaselineIdentity baseline = readBaseline(f.member("baseline").orElse(Node.nullNode()), problems);
        CandidateIdentity candidate = readCandidate(f.member("candidate").orElse(Node.nullNode()), problems);
        List<ManifestEntry> entries = new ArrayList<>();
        boolean complete = true;
        Node entriesNode = f.member("entries").orElse(Node.nullNode());
        if (!entriesNode.isArrayNode()) {
            f.bad("entries", "must be an array");
        } else {
            int index = 0;
            for (Node element : entriesNode.expectArrayNode().getElements()) {
                ManifestEntry entry = readEntry(element, "entries[" + index++ + "]", problems);
                if (entry != null) {
                    entries.add(entry);
                } else {
                    complete = false;
                }
            }
        }
        if (baseline == null) {
            throw new ManifestValidationException(problems);
        }
        ParityManifest manifest = new ParityManifest(baseline, candidate, entries);
        if (complete) {
            // The summary is derived from the entries; skip it when an entry could not be read.
            checkSummary(f.member("summary").orElse(Node.nullNode()), manifest, problems);
        }
        problems.addAll(ManifestValidator.validate(manifest, ManifestValidator.Options.DEFAULT));
        ManifestValidationException.throwIfAny(problems);
        return manifest;
    }

    private static ObjectNode baseline(BaselineIdentity b) {
        return Node.objectNodeBuilder()
                   .withMember("v2Version", b.v2Version())
                   .withMember("gitRevision", nullable(b.gitRevision()))
                   .withMember("featureFreezeCutoff", nullable(b.featureFreezeCutoff() == null
                                                                ? null : b.featureFreezeCutoff().toString()))
                   .withMember("supportStatus", nullable(b.supportStatus()))
                   .withMember("scannerVersion", b.scannerVersion())
                   .build();
    }

    private static Node candidate(CandidateIdentity c) {
        if (c == null) {
            return Node.nullNode();
        }
        return Node.objectNodeBuilder()
                   .withMember("v3Version", c.v3Version())
                   .withMember("gitRevision", nullable(c.gitRevision()))
                   .build();
    }

    private static ObjectNode entry(ManifestEntry e) {
        return Node.objectNodeBuilder()
                   .withMember("id", e.id())
                   .withMember("kind", e.kind().name())
                   .withMember("v2Surface", e.v2Surface())
                   .withMember("source", Node.objectNodeBuilder()
                                             .withMember("artifact", e.source().artifact())
                                             .withMember("path", nullable(e.source().path()))
                                             .build())
                   .withMember("deprecated", e.deprecated())
                   .withMember("classification", e.classification().name())
                   .withMember("dimensions", e.dimensions().stream().map(d -> Node.from(d.jsonName()))
                                              .collect(ArrayNode.collect()))
                   .withMember("binary", e.binary())
                   .withMember("rationale", nullable(e.rationale()))
                   .withMember("differences", Node.fromStrings(e.differences()))
                   .withMember("v3Artifact", nullable(e.v3Artifact()))
                   .withMember("replacement", nullable(e.replacement()))
                   .withMember("migrationKind", nullable(e.migrationKind() == null ? null : e.migrationKind().name()))
                   .withMember("migration", nullable(e.migration()))
                   .withMember("supportEvidence", nullable(e.supportEvidence()))
                   .withMember("evidenceIds", Node.fromStrings(List.copyOf(e.evidenceIds())))
                   .withMember("evidence", nullable(evidence(e)))
                   .build();
    }

    /** The fixture-compatible {@code evidence} string: evidence IDs joined by {@code ", "}, or {@code null}. */
    static String evidence(ManifestEntry e) {
        return e.evidenceIds().isEmpty() ? null : String.join(", ", e.evidenceIds());
    }

    private static Node nullable(String value) {
        return value == null ? Node.nullNode() : Node.from(value);
    }

    private static BaselineIdentity readBaseline(Node node, List<ManifestProblem> problems) {
        ObjectNode o = JsonFields.object(node, "baseline", "'baseline'", problems);
        if (o == null) {
            return null;
        }
        JsonFields f = new JsonFields(o, "baseline", Set.copyOf(BASELINE_MEMBERS), problems);
        String version = f.requiredString("v2Version");
        String scanner = f.requiredString("scannerVersion");
        String cutoff = f.string("featureFreezeCutoff");
        LocalDate date = null;
        if (cutoff != null) {
            try {
                date = LocalDate.parse(cutoff);
            } catch (DateTimeParseException e) {
                f.bad("featureFreezeCutoff", "must be an ISO-8601 date");
            }
        }
        if (version == null || scanner == null) {
            return null;
        }
        return new BaselineIdentity(version, f.string("gitRevision"), date, f.string("supportStatus"), scanner);
    }

    private static CandidateIdentity readCandidate(Node node, List<ManifestProblem> problems) {
        if (node.isNullNode()) {
            return null;
        }
        ObjectNode o = JsonFields.object(node, "candidate", "'candidate'", problems);
        if (o == null) {
            return null;
        }
        JsonFields f = new JsonFields(o, "candidate", Set.copyOf(CANDIDATE_MEMBERS), problems);
        String version = f.requiredString("v3Version");
        return version == null ? null : new CandidateIdentity(version, f.string("gitRevision"));
    }

    private static ManifestEntry readEntry(Node node, String fallback, List<ManifestProblem> problems) {
        ObjectNode o = JsonFields.object(node, fallback, "an entry", problems);
        if (o == null) {
            return null;
        }
        String subject = o.getStringMember("id").map(s -> s.getValue()).orElse(fallback);
        int before = problems.size();
        JsonFields f = new JsonFields(o, subject, Set.copyOf(ENTRY_MEMBERS), problems);
        String id = f.requiredString("id");
        EntryKind kind = f.enumValue("kind", EntryKind::valueOf);
        Classification classification = f.enumValue("classification", Classification::valueOf);
        for (String required : List.of("kind", "classification", "deprecated", "binary")) {
            if (!f.has(required) || f.member(required).get().isNullNode()) {
                f.bad(required, "is required");
            }
        }
        String surface = f.requiredString("v2Surface");
        SourceLocation source = null;
        ObjectNode s = JsonFields.object(f.member("source").orElse(Node.nullNode()), subject, "'source'", problems);
        if (s != null) {
            JsonFields sf = new JsonFields(s, subject, Set.copyOf(SOURCE_MEMBERS), problems);
            String artifact = sf.requiredString("artifact");
            source = artifact == null ? null : new SourceLocation(artifact, sf.string("path"));
        }
        TreeSet<CompatibilityDimension> dimensions = new TreeSet<>();
        for (String d : f.strings("dimensions")) {
            try {
                dimensions.add(CompatibilityDimension.fromJsonName(d));
            } catch (IllegalArgumentException e) {
                f.bad("dimensions", "has unknown value '" + d + "'");
            }
        }
        Boolean deprecated = f.bool("deprecated");
        Boolean binary = f.bool("binary");
        String rationale = f.string("rationale");
        List<String> differences = f.strings("differences");
        String v3Artifact = f.string("v3Artifact");
        String replacement = f.string("replacement");
        MigrationKind migrationKind = f.enumValue("migrationKind", MigrationKind::valueOf);
        String migration = f.string("migration");
        String supportEvidence = f.string("supportEvidence");
        List<String> evidenceIds = f.strings("evidenceIds");
        String evidence = f.string("evidence");
        if (problems.size() != before || id == null || kind == null || classification == null || surface == null
            || source == null || deprecated == null || binary == null) {
            return null;
        }
        ManifestEntry entry = new ManifestEntry(id, kind, surface, source, deprecated, classification, dimensions,
                                                rationale, differences, v3Artifact, replacement, migrationKind,
                                                migration, supportEvidence, new TreeSet<>(evidenceIds));
        if (binary != entry.binary()) {
            f.bad("binary", "disagrees with dimensions");
        }
        if (!Objects.equals(evidence, evidence(entry))) {
            f.bad("evidence", "disagrees with evidenceIds");
        }
        return entry;
    }

    private static void checkSummary(Node node, ParityManifest manifest, List<ManifestProblem> problems) {
        ObjectNode o = JsonFields.object(node, "summary", "'summary'", problems);
        if (o == null) {
            return;
        }
        Map<Classification, Integer> expected = manifest.summary();
        List<String> names = expected.keySet().stream().map(Classification::name).toList();
        JsonFields f = new JsonFields(o, "summary", Set.copyOf(names), problems);
        expected.forEach((c, n) -> {
            Integer actual = f.integer(c.name());
            if (actual == null || actual.intValue() != n) {
                f.bad(c.name(), "must equal the " + c + " entry count " + n);
            }
        });
    }
}
