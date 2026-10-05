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

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ClassificationRulesTest {
    private static final String RULES = """
        {
          "schemaVersion": 1,
          "rules": [
            {"name": "kind-member", "match": {"kind": "PUBLIC_MEMBER"}, "classification": "PRESERVED",
             "dimensions": ["source", "binary"], "rationale": "members"},
            {"name": "kind-member-deprecated", "match": {"kind": "PUBLIC_MEMBER", "deprecated": true},
             "classification": "EXCLUDED", "rationale": "gone", "migrationKind": "COMPILE_TIME", "migration": "m"},
            {"name": "glob-core", "match": {"id": "api:a.core.*"}, "classification": "PRESERVED",
             "dimensions": ["source"], "rationale": "core"},
            {"name": "glob-core-retry", "match": {"id": "api:a.core.retry.*"}, "classification": "EXCLUDED",
             "rationale": "legacy", "migrationKind": "DIAGNOSTIC", "migration": "use RetryStrategy",
             "supportEvidence": "deprecated"},
            {"name": "exact", "match": {"id": "api:a.core.retry.Kept"}, "classification": "PRESERVED",
             "dimensions": ["source"], "rationale": "kept"},
            {"name": "glob-tie-1", "match": {"id": "api:a.tie.*X"}, "classification": "PRESERVED",
             "dimensions": ["source"], "rationale": "t1"},
            {"name": "glob-tie-2", "match": {"id": "api:a.tie.X*"}, "classification": "PRESERVED",
             "dimensions": ["source"], "rationale": "t2"}
          ],
          "defaults": [
            {"name": "default-api", "match": {"deprecated": false}, "classification": "PRESERVED",
             "dimensions": ["source"], "rationale": "default"},
            {"name": "default-config", "match": {"kind": "CONFIGURATION", "deprecated": false},
             "classification": "PRESERVED", "dimensions": ["configuration"], "rationale": "config default"}
          ]
        }
        """;

    @Test
    void mostSpecificRuleWins() throws Exception {
        ClassificationRules rules = ClassificationRules.parse(RULES);
        ClassificationRules.Classified result = rules.classify(List.of(
            Entries.item("api:a.core.retry.Kept", EntryKind.PUBLIC_TYPE),            // exact beats globs
            Entries.item("api:a.core.retry.RetryPolicy", EntryKind.PUBLIC_TYPE),     // longer glob beats shorter
            Entries.item("api:a.core.Client", EntryKind.PUBLIC_TYPE),                // glob beats defaults
            Entries.item("api:a.other.C#m()V", EntryKind.PUBLIC_MEMBER),             // kind rule
            Entries.item("api:a.other.C#old()V", EntryKind.PUBLIC_MEMBER, true),     // kind + deprecated beats kind
            Entries.item("api:a.core.C#m()V", EntryKind.PUBLIC_MEMBER),              // id glob beats kind
            Entries.item("api:a.other.D", EntryKind.PUBLIC_TYPE),                    // match-all default
            Entries.item("config:env:AWS_REGION", EntryKind.CONFIGURATION)));        // kind default beats match-all
        assertEquals(List.of(), result.problems());
        assertEquals(Map.of("api:a.core.retry.Kept", "exact",
                            "api:a.core.retry.RetryPolicy", "glob-core-retry",
                            "api:a.core.Client", "glob-core",
                            "api:a.other.C#m()V", "kind-member",
                            "api:a.other.C#old()V", "kind-member-deprecated",
                            "api:a.core.C#m()V", "glob-core",
                            "api:a.other.D", "default-api",
                            "config:env:AWS_REGION", "default-config"), result.ruleByEntry());
        ManifestEntry member = result.entries().stream().filter(e -> e.id().equals("api:a.other.C#m()V"))
                                     .findFirst().orElseThrow();
        assertEquals(true, member.binary());
        assertEquals("PRESERVED members", member.disposition());
    }

    @Test
    void reportsConflictsAndUnmatchedItemsTogether() throws Exception {
        ClassificationRules.Classified result = ClassificationRules.parse(RULES).classify(List.of(
            Entries.item("api:a.tie.XyzX", EntryKind.PUBLIC_TYPE),
            Entries.item("api:a.other.Old", EntryKind.PUBLIC_TYPE, true),
            Entries.item("api:a.other.Fine", EntryKind.PUBLIC_TYPE)));
        assertEquals(List.of("UNMATCHED [api:a.other.Old]: no classification rule or default matches this "
                             + "deprecated PUBLIC_TYPE entry",
                             "CONFLICTING_RULES [api:a.tie.XyzX]: equally specific rules match: glob-tie-1, glob-tie-2"),
                     result.problems().stream().map(ManifestProblem::toString).toList());
        assertEquals(List.of("api:a.other.Fine"), result.entries().stream().map(ManifestEntry::id).toList());
    }

    @Test
    void parseReportsEveryMalformedRuleAtOnce() {
        String bad = """
            {"schemaVersion": 1,
             "rules": [
               {"name": "no-match", "classification": "PRESERVED"},
               {"name": "bad-kind", "match": {"kind": "NOPE"}, "classification": "PRESERVED"},
               {"name": "dup", "match": {"kind": "SERVICE"}, "classification": "MAYBE", "colour": "red"},
               {"name": "dup", "match": {"kind": "SERVICE"}, "classification": "PRESERVED", "dimensions": ["wire"]}
             ]}
            """;
        ManifestValidationException e = assertThrows(ManifestValidationException.class,
                                                     () -> ClassificationRules.parse(bad));
        assertEquals(List.of(
            "MALFORMED [rule:bad-kind]: 'kind' has unknown value 'NOPE'",
            "MALFORMED [rule:dup]: 'classification' has unknown value 'MAYBE'",
            "MALFORMED [rule:dup]: 'dimensions' has unknown value 'wire'",
            "MALFORMED [rule:dup]: unknown member 'colour'",
            "MALFORMED [rule:no-match]: a rule must constrain id, kind or deprecated; put match-all rules under "
            + "'defaults'"), e.problems().stream().map(ManifestProblem::toString).toList());
    }

    @Test
    void checkedInRulesParseAndClassifyRepresentativeItems() throws Exception {
        ClassificationRules rules = ClassificationRules.loadDefault();
        List<InventoryItem> items = List.of(
            Entries.item("api:software.amazon.awssdk.core.retry.RetryPolicy", EntryKind.PUBLIC_TYPE, true),
            Entries.item("api:software.amazon.awssdk.core.retry.RetryPolicy$Builder#build()"
                         + "Lsoftware/amazon/awssdk/core/retry/RetryPolicy;", EntryKind.PUBLIC_MEMBER, true),
            Entries.item("api:software.amazon.awssdk.core.RequestOverrideConfiguration$Builder#signer"
                         + "(Lsoftware/amazon/awssdk/core/signer/Signer;)"
                         + "Lsoftware/amazon/awssdk/core/RequestOverrideConfiguration$Builder;",
                         EntryKind.PUBLIC_MEMBER),
            Entries.item("artifact:software.amazon.awssdk:dynamodb", EntryKind.ARTIFACT),
            Entries.item("service:dynamodb", EntryKind.SERVICE),
            Entries.item("config:env:AWS_REGION", EntryKind.CONFIGURATION),
            Entries.item("http-mode:netty-nio-client:async", EntryKind.HTTP_MODE),
            Entries.item("behavior:retry.standard-mode", EntryKind.BEHAVIOR),
            Entries.item("api:software.amazon.awssdk.core.SdkBytes", EntryKind.PUBLIC_TYPE));
        ParityManifest manifest = new ManifestGenerator(List.of(new StaticInventorySource("test", items)), rules)
            .generate(null, Entries.BASELINE, null, ManifestValidator.Options.DEFAULT);
        assertEquals(Map.of(Classification.PRESERVED, 6, Classification.ADAPTED, 0, Classification.EXCLUDED, 3),
                     manifest.summary());
        ClassificationRules.Classified deprecatedWithoutRule = rules.classify(List.of(
            Entries.item("api:software.amazon.awssdk.profiles.Old", EntryKind.PUBLIC_TYPE, true)));
        assertEquals(List.of(ManifestProblem.Code.UNMATCHED),
                     deprecatedWithoutRule.problems().stream().map(ManifestProblem::code).toList());
    }
}
