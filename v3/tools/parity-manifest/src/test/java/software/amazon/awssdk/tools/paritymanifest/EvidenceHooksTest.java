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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.tools.paritymanifest.compat.CompatibilityFixture;
import software.amazon.awssdk.tools.paritymanifest.compat.CompatibilityFixtures;

/** The evidence hook registry and its text form, which CompatibilityFixtures.evidenceHooks emits. */
class EvidenceHooksTest {
    private static final String MEMBER = "api:p.Api#get(I)Ljava/lang/String;";

    @Test
    void parsesMemberIdsWhoseHashIsNotAComment() {
        EvidenceHooks hooks = EvidenceHooks.parse("""
            # a comment line
            LinkTest api:p.Api #trailing comment
            LinkTest\t%s
            BareTest
            """.formatted(MEMBER), "hooks.txt");

        assertEquals(Map.of("LinkTest", new TreeSet<>(List.of("api:p.Api", MEMBER)), "BareTest", new TreeSet<>()),
                     hooks.hooks());
        assertTrue(hooks.registered());
        assertTrue(hooks.known("BareTest"));
        assertFalse(hooks.known("Other"));
        ManifestEntry entry = Entries.preserved(MEMBER).build();
        assertEquals(Set.of("LinkTest"), hooks.of(entry));
    }

    @Test
    void rejectsMalformedEntryIdsWithTheirLine() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                                                  () -> EvidenceHooks.parse("A api:p.Api\nB not-an-id\n", "h.txt"));
        assertTrue(e.getMessage().startsWith("h.txt:2: 'not-an-id'"), e.getMessage());
    }

    @Test
    void ofCombinesManifestEvidenceIdsWithBindingsAndMergeUnionsRegistries() {
        Entries.B b = Entries.preserved("service:s3");
        b.evidenceIds.add("OwnTest");
        EvidenceHooks first = EvidenceHooks.parse("BoundTest service:s3", "a");
        EvidenceHooks merged = first.merge(EvidenceHooks.parse("BoundTest service:sqs\nOtherTest", "b"));

        assertEquals(Set.of("BoundTest", "OwnTest"), first.of(b.build()));
        assertEquals(Set.of("service:s3", "service:sqs"), merged.hooks().get("BoundTest"));
        assertTrue(merged.known("OtherTest"));
        assertFalse(EvidenceHooks.NONE.registered());
        assertEquals(Set.of("OwnTest"), EvidenceHooks.NONE.of(b.build()));
        assertTrue(EvidenceHooks.NONE.merge(first).registered());
        assertFalse(EvidenceHooks.NONE.merge(EvidenceHooks.NONE).registered());
    }

    @Test
    void readsTheCompatibilityFixtureHookOutput() {
        CompatibilityFixture link = new CompatibilityFixture(
            MEMBER + " LINK binary", MEMBER, Classification.PRESERVED, CompatibilityFixture.Kind.LINK,
            CompatibilityDimension.BINARY, "V2 get", null, "class CompatCaller {}", List.of(), null, List.of(),
            List.of());
        CompatibilityFixtures fixtures = new CompatibilityFixtures("2.54.0", null, List.of("api:p.*"), List.of(link),
                                                                   List.of());

        EvidenceHooks hooks = EvidenceHooks.parse(
            fixtures.evidenceHooks(Map.of(CompatibilityFixture.Kind.LINK, "V2BytecodeOnV3Test")), "fixtures");

        assertEquals(Map.of("V2BytecodeOnV3Test", new TreeSet<>(Set.of(MEMBER))), hooks.hooks());
    }
}
