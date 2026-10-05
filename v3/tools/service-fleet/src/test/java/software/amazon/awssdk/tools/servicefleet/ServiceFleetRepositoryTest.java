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

package software.amazon.awssdk.tools.servicefleet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The fleet gate of this repository (task 13.2; Requirements 2.1, 2.10, 16.4, 16.5): every non-deprecated service of
 * the V2 Launch Parity Baseline ({@code services/} of this checkout) has a generated V3 service module pinned to the
 * canonical model revision, the modules, the services aggregator and the BOM are exactly what
 * {@code v3/services/service-fleet.json} renders, the vendored model files carry the pinned digests, and, when the
 * canonical aws-models checkout is present ({@code -Dv3.canonicalModels}), it is at the pinned revision with the
 * pinned model files. Missing AWS semantics and missing client types fail each module's {@code aws-codegen:generate}.
 */
class ServiceFleetRepositoryTest {

    @Test
    void theServiceFleetCoversTheBaselineAtThePinnedCanonicalModelRevision() {
        Path repository = Path.of(System.getProperty("v3.repositoryRoot")).toAbsolutePath().normalize();
        Path v3 = Path.of(System.getProperty("v3.root")).toAbsolutePath().normalize();
        String models = System.getProperty("v3.canonicalModels", "");
        ServiceFleetGate.Result result = ServiceFleetGate.check(
            repository, v3, models.isBlank() ? null : Path.of(models).toAbsolutePath().normalize());
        result.notes().forEach(note -> System.out.println("service fleet: " + note));
        List<String> problems = result.problems();
        assertEquals(List.of(), problems, problems.size() + " service fleet problem(s):\n"
                                          + String.join("\n", problems));
    }
}
