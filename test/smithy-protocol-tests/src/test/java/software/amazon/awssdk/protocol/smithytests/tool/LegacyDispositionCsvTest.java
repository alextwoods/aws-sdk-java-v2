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

package software.amazon.awssdk.protocol.smithytests.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Checks the committed {@code legacy-disposition.csv} against a fresh {@link LegacyDispositionTool} run (Requirement
 * 32.10): it is up to date, and every legacy case has exactly one valid disposition.
 */
class LegacyDispositionCsvTest {

    private static LegacyDispositionTool tool;
    private static LegacyDispositionTool.Output output;

    @BeforeAll
    static void generate() throws IOException {
        // Surefire runs in the module directory; the tool reads the legacy suites relative to the repository root.
        tool = new LegacyDispositionTool(Paths.get("../..").toAbsolutePath().normalize());
        output = tool.generate(new PrintStream(new ByteArrayOutputStream(), true, "UTF-8"));
    }

    @Test
    void committedCsv_matchesToolOutput() throws IOException {
        String committed = new String(Files.readAllBytes(tool.csvFile()), StandardCharsets.UTF_8);
        assertThat(committed)
            .as("legacy-disposition.csv is out of date; regenerate it with src/tool/run.sh")
            .isEqualTo(output.csv);
    }

    @Test
    void reviewFile_hasNoStaleEntries() {
        assertThat(output.staleReviews).isEmpty();
    }

    @Test
    void everyLegacyCase_isEquivalentPortedOrRetired() {
        assertThat(output.unresolved()).isZero();
        assertThat(output.rows).hasSize(518);
        // One row per case per legacy suite (protocol) that runs it.
        assertThat(output.rows).extracting(r -> r.legacy.key() + "@" + r.legacy.suite.protocol)
                               .doesNotHaveDuplicates();
    }

    @Test
    void equivalentAndPortedRows_nameASmithyCaseOfTheSameProtocol() {
        Map<String, SmithyCase> cases = new HashMap<>();
        for (SmithyCase c : output.smithyCases) {
            cases.put(c.protocol + "|" + c.id, c);
        }
        for (Dispositions.Row row : output.rows) {
            String protocol = row.legacy.suite.protocol;
            switch (row.disposition) {
                case Dispositions.EQUIVALENT:
                    assertThat(cases.get(protocol + "|" + row.smithyId)).as(row.legacy.key()).isNotNull()
                                                                       .matches(c -> !c.sdkOwned, "is upstream");
                    break;
                case Dispositions.PORTED:
                    assertThat(cases.get(protocol + "|" + row.smithyId)).as(row.legacy.key()).isNotNull()
                                                                       .matches(c -> c.sdkOwned, "is SDK-owned");
                    break;
                default:
                    assertThat(row.disposition).as(row.legacy.key()).isEqualTo(Dispositions.RETIRED);
                    assertThat(row.reason).as(row.legacy.key()).isNotBlank();
            }
        }
    }
}
