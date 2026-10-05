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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.tools.paritymanifest.CandidateEvidence.EvidenceRecord;
import software.amazon.awssdk.tools.paritymanifest.CandidateEvidence.Outcome;

/** The candidate evidence JSON form the general-availability gate reads (Requirement 16.14). */
class CandidateEvidenceTest {

    @Test
    void roundTripsSortedAndDeterministic() throws Exception {
        CandidateEvidence evidence = new CandidateEvidence(new CandidateIdentity("3.0.0", null), List.of(
            new EvidenceRecord("service:s3", "S3GeneratedApiTest", "ok", "ok", Outcome.PASSED),
            new EvidenceRecord("service:dynamodb", "DynamoDbGeneratedApiTest", "ok", "timeout", Outcome.FAILED)));

        String json = evidence.toJson();
        CandidateEvidence parsed = CandidateEvidence.parse(json);

        assertEquals(evidence, parsed);
        assertEquals("service:dynamodb", parsed.records().get(0).entryId());
        assertEquals(json, parsed.toJson());
        assertTrue(json.contains("\"gitRevision\": null"));
    }

    @Test
    void reportsEveryMalformedMemberAtOnce() {
        ManifestValidationException e = assertThrows(ManifestValidationException.class, () -> CandidateEvidence.parse("""
            {"schemaVersion": 2, "candidate": {"v3Version": "3.0.0", "extra": 1},
             "records": [{"entryId": "service:s3", "evidenceId": "T", "expected": "ok", "actual": "ok",
                          "outcome": "MAYBE"},
                         {"entryId": "service:s3", "evidenceId": "T", "expected": "ok", "actual": "ok"}]}
            """));
        List<String> problems = e.problems().stream().map(ManifestProblem::toString).toList();
        assertEquals(4, problems.size(), problems.toString());
        assertTrue(problems.stream().anyMatch(p -> p.contains("schemaVersion")));
        assertTrue(problems.stream().anyMatch(p -> p.contains("'extra'")));
        assertTrue(problems.stream().anyMatch(p -> p.contains("records[0]") && p.contains("MAYBE")));
        assertTrue(problems.stream().anyMatch(p -> p.contains("records[1]") && p.contains("outcome")));
        assertThrows(ManifestValidationException.class, () -> CandidateEvidence.parse("not json"));
    }
}
