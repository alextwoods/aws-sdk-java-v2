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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * Candidate-specific verification evidence (Requirement 16.14): for one release candidate, the expected outcome, the
 * actual outcome and the result of each evidence hook run against each manifest entry. It joins the Parity Manifest
 * by stable entry ID (design "Parity Manifest schema") and is consumed by {@link LaunchGate} at
 * {@link LaunchGate.Stage#GENERAL_AVAILABILITY}.
 *
 * <p>JSON form (deterministic: fixed member order, records sorted, four-space indentation, trailing newline):
 * <pre>
 * {"schemaVersion": 1,
 *  "candidate": {"v3Version": "3.0.0", "gitRevision": "&lt;sha&gt;" | null},
 *  "records": [{"entryId": "...", "evidenceId": "...", "expected": "...", "actual": "...",
 *               "outcome": "PASSED" | "FAILED" | "SKIPPED"}]}
 * </pre>
 *
 * @param candidate the release candidate the evidence was produced for
 * @param records   the evidence records, stored sorted by entry ID, evidence ID, then outcome
 */
public record CandidateEvidence(CandidateIdentity candidate, List<EvidenceRecord> records) {
    /** The schema version of the JSON form. */
    public static final int SCHEMA_VERSION = 1;

    private static final Set<String> TOP = Set.of("schemaVersion", "candidate", "records");
    private static final Set<String> CANDIDATE = Set.of("v3Version", "gitRevision");
    private static final Set<String> RECORD = Set.of("entryId", "evidenceId", "expected", "actual", "outcome");
    private static final Comparator<EvidenceRecord> ORDER =
        Comparator.comparing(EvidenceRecord::entryId).thenComparing(EvidenceRecord::evidenceId)
                  .thenComparing(EvidenceRecord::outcome).thenComparing(EvidenceRecord::expected)
                  .thenComparing(EvidenceRecord::actual);

    /** The result of one evidence run. */
    public enum Outcome {
        PASSED, FAILED, SKIPPED
    }

    /**
     * One evidence run against one entry.
     *
     * @param entryId    the manifest entry verified
     * @param evidenceId the evidence hook that ran
     * @param expected   the candidate-specific expected outcome
     * @param actual     the observed outcome
     * @param outcome    whether the verification passed
     */
    public record EvidenceRecord(String entryId, String evidenceId, String expected, String actual,
                                 Outcome outcome) {
        public EvidenceRecord {
            Objects.requireNonNull(entryId, "entryId");
            Objects.requireNonNull(evidenceId, "evidenceId");
            Objects.requireNonNull(expected, "expected");
            Objects.requireNonNull(actual, "actual");
            Objects.requireNonNull(outcome, "outcome");
        }
    }

    public CandidateEvidence {
        Objects.requireNonNull(candidate, "candidate");
        records = records.stream().sorted(ORDER).toList();
    }

    /** The canonical JSON form, ending with a newline. */
    public String toJson() {
        ObjectNode c = Node.objectNodeBuilder()
                           .withMember("v3Version", candidate.v3Version())
                           .withMember("gitRevision", candidate.gitRevision() == null
                                                      ? Node.nullNode() : Node.from(candidate.gitRevision()))
                           .build();
        ArrayNode r = records.stream()
                             .map(rec -> (Node) Node.objectNodeBuilder()
                                                    .withMember("entryId", rec.entryId())
                                                    .withMember("evidenceId", rec.evidenceId())
                                                    .withMember("expected", rec.expected())
                                                    .withMember("actual", rec.actual())
                                                    .withMember("outcome", rec.outcome().name())
                                                    .build())
                             .collect(ArrayNode.collect());
        return Node.prettyPrintJson(Node.objectNodeBuilder()
                                        .withMember("schemaVersion", SCHEMA_VERSION)
                                        .withMember("candidate", c)
                                        .withMember("records", r)
                                        .build()) + "\n";
    }

    /** Parses the JSON form, reporting every malformed member at once. */
    public static CandidateEvidence parse(String json) throws ManifestValidationException {
        List<ManifestProblem> problems = new ArrayList<>();
        Node root;
        try {
            root = Node.parse(json);
        } catch (RuntimeException e) {
            throw new ManifestValidationException(List.of(new ManifestProblem(MALFORMED, "$", e.getMessage())));
        }
        ObjectNode top = JsonFields.object(root, "$", "the candidate evidence", problems);
        if (top == null) {
            throw new ManifestValidationException(problems);
        }
        JsonFields f = new JsonFields(top, "$", TOP, problems);
        Integer version = f.integer("schemaVersion");
        if (version == null || version != SCHEMA_VERSION) {
            f.bad("schemaVersion", "must be " + SCHEMA_VERSION);
        }
        CandidateIdentity candidate = null;
        ObjectNode c = JsonFields.object(f.member("candidate").orElse(Node.nullNode()), "candidate", "'candidate'",
                                         problems);
        if (c != null) {
            JsonFields cf = new JsonFields(c, "candidate", CANDIDATE, problems);
            String v3Version = cf.requiredString("v3Version");
            candidate = v3Version == null ? null : new CandidateIdentity(v3Version, cf.string("gitRevision"));
        }
        List<EvidenceRecord> records = new ArrayList<>();
        Node recordsNode = f.member("records").orElse(Node.nullNode());
        if (!recordsNode.isArrayNode()) {
            f.bad("records", "must be an array");
        } else {
            int index = 0;
            for (Node element : recordsNode.expectArrayNode().getElements()) {
                String subject = "records[" + index++ + "]";
                ObjectNode o = JsonFields.object(element, subject, "a record", problems);
                if (o == null) {
                    continue;
                }
                int before = problems.size();
                JsonFields rf = new JsonFields(o, subject, RECORD, problems);
                String entryId = rf.requiredString("entryId");
                String evidenceId = rf.requiredString("evidenceId");
                String expected = rf.requiredString("expected");
                String actual = rf.requiredString("actual");
                Outcome outcome = rf.enumValue("outcome", Outcome::valueOf);
                if (outcome == null && !rf.has("outcome")) {
                    rf.bad("outcome", "is required");
                }
                if (problems.size() == before && outcome != null) {
                    records.add(new EvidenceRecord(entryId, evidenceId, expected, actual, outcome));
                }
            }
        }
        ManifestValidationException.throwIfAny(problems);
        return new CandidateEvidence(candidate, records);
    }
}
