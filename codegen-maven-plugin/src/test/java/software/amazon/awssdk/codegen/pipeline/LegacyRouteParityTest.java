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

package software.amazon.awssdk.codegen.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Regression check for the Codegen_Pipeline (C2J_Converter plus {@code SmithyIntermediateModelBuilder}, tasks 1.22
 * and 1.23): for each Prototype_Service_Set module, the code it generates equals the code of the legacy C2J route,
 * except for the differences listed in {@code legacy-route-parity/<module>.txt} (none when the file is absent). For
 * Secrets Manager and Account, the pipeline generates from the module's native Smithy model (task 1.31). See
 * {@link LegacyRouteParity} for what the comparison ignores, and the {@code codegen} section of
 * {@code docs/design/schema-serde/porting-deviations.md} for why each listed difference is accepted.
 *
 * <p>To update a list after an intended change, run {@code LegacyRouteParity.main(<module>)} and copy its output
 * below the {@code ===} line.
 */
class LegacyRouteParityTest {
    @TempDir
    Path tmp;

    @ParameterizedTest
    @ValueSource(strings = {"sqs", "secretsmanager", "account", "route53", "sns", "cloudwatch", "dynamodb"})
    void pipelineGeneratesTheSameCodeAsTheLegacyRoute(String module) throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(LegacyRouteParity.codegenResources(module)),
                               "C2J model of " + module + " not found");

        String actual = LegacyRouteParity.format(LegacyRouteParity.compare(module, tmp));

        assertThat(actual).isEqualTo(expectedDifferences(module));
    }

    private static String expectedDifferences(String module) throws IOException {
        try (InputStream in = LegacyRouteParityTest.class.getResourceAsStream("/legacy-route-parity/" + module + ".txt")) {
            return in == null ? "" : new String(IoUtils.toByteArray(in), StandardCharsets.UTF_8);
        }
    }
}
