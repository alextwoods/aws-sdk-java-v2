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

package software.amazon.awssdk.test.generated;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.test.generated.support.CompatibilityFixtures;
import software.amazon.awssdk.test.generated.support.CompatibilityFixtures.Fixture;
import software.amazon.awssdk.test.generated.support.Javac;

/**
 * Compiles the V2 caller of every generated source fixture against the generated V3 API (Requirements 4.2, 4.3,
 * 17.1, 17.3, 19.12). The fixtures come from the Parity Manifest for this module's test scope
 * ({@code compatibility-fixtures.json}): a {@code COMPILE} caller of a source-preserved entry must compile, and a
 * {@code COMPILE_REJECTED} caller of an {@code ADAPTED} or {@code EXCLUDED} entry without a source claim must fail
 * only on symbols of the entry's own surface, reported with the migration diagnostic linked to the entry.
 */
class ManifestDiagnosticsTest {

    static Stream<Fixture> sourceFixtures() {
        return CompatibilityFixtures.ofKinds("COMPILE", "COMPILE_REJECTED");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sourceFixtures")
    void eachV2CallerCompilesOrIsRejectedAsItsManifestEntryDeclares(Fixture fixture, @TempDir Path temp) {
        List<String> errors = Javac.compile(CompatibilityFixtures.CALLER_CLASS, fixture.source(), Javac.testClassPath(),
                                            temp.resolve("classes"));
        if (fixture.kind().equals("COMPILE")) {
            assertEquals(List.of(), errors, fixture.entryId() + " is " + fixture.classification()
                                            + " in the source dimension, but its V2 caller does not compile:\n"
                                            + fixture.source());
            return;
        }
        assertFalse(errors.isEmpty(), fixture.entryId() + " is " + fixture.classification() + " without a source "
                                      + "claim, but its V2 caller compiles against V3; claim the source dimension "
                                      + "or remove the surface:\n" + fixture.source());
        List<String> unrelated = errors.stream().filter(e -> fixture.symbols().stream().noneMatch(e::contains))
                                       .toList();
        assertEquals(List.of(), unrelated, fixture.entryId() + " fails on symbols outside its surface "
                                           + fixture.symbols());

        String diagnostic = linkedDiagnostic(fixture, errors);
        assertTrue(diagnostic.startsWith("[" + fixture.entryId() + "] " + fixture.classification() + " "
                                         + fixture.v2Surface() + " -> "), diagnostic);
    }

    /** The scope verifies every classification, so 19.12's PRESERVED, ADAPTED and EXCLUDED cases all run. */
    @Test
    void theTestScopeCoversEveryClassificationInBothSourceOutcomes() {
        Set<String> cases = sourceFixtures().map(f -> f.kind() + " " + f.classification())
                                            .collect(Collectors.toSet());
        assertTrue(cases.containsAll(Set.of("COMPILE PRESERVED", "COMPILE_REJECTED ADAPTED",
                                            "COMPILE_REJECTED EXCLUDED")), cases.toString());
    }

    /** The migration diagnostic of a V2 caller that does not compile, linked to its manifest entry. */
    static String linkedDiagnostic(Fixture fixture, List<String> errors) {
        return fixture.diagnostic() + " (javac: " + errors.get(0) + ")";
    }
}
