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

package software.amazon.awssdk.benchmark.harness;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * {@link PerformanceReferences#RACECAR_APPLICABILITY} equals the built Racecar reference client's own applicability
 * ({@code RacecarReferenceMain}), over the full matrix, with {@code RacecarReferenceMain} loaded from its Racecar-only
 * class path in an isolated class loader ({@link V2ReferenceApplicabilityTest#assertMainMatches}). Needs the built
 * {@code test/sdk-benchmarks-racecar-reference} (the reactor builds it first), like the forked smoke.
 */
class RacecarReferenceApplicabilityTest {

    @Test
    void harnessApplicabilityEqualsTheRacecarReferenceMain() throws Exception {
        PerformanceReferences.ClientLookup lookup =
            PerformanceReferences.racecarClient(Path.of(System.getProperty("user.dir")).toAbsolutePath().getParent());
        assertNull(lookup.problem(), "build test/sdk-benchmarks-racecar-reference first");
        V2ReferenceApplicabilityTest.assertMainMatches(lookup, PerformanceReferences.RACECAR_APPLICABILITY);
    }
}
