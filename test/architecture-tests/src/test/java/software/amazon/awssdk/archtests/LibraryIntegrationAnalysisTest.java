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

package software.amazon.awssdk.archtests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Checks the Library_Integration_Analysis (design D.7, Requirements 38.1 and 38.2) through
 * {@code scripts/schema-serde/library_integration_analysis.py}, which generates
 * {@code docs/design/schema-serde/library-integration-analysis.md} from {@code library-integration.yaml}, the
 * Libraries rows of {@code size-report.md} and the JMH result {@code library-integration-jmh.json}.
 *
 * <p>The script's {@code --check} mode fails when the committed Markdown differs from the generated output, one of the
 * seven subjects is missing, repeated or out of order, a compatibility, direction or recommendation is outside the
 * Requirement 38 vocabulary, a required field is empty, a referenced Size_Report row or benchmark result is missing,
 * or a measured direction differs from the expected one without a note. {@code --self-test} runs the script's own unit
 * tests.
 *
 * <p>The script path and the Python interpreter come from the module's {@code pom.xml}
 * ({@code -Dlibrary-integration.python=...} selects another interpreter).
 */
class LibraryIntegrationAnalysisTest {
    private static final PythonScript SCRIPT = new PythonScript("library-integration.python",
                                                                "library-integration.script");

    @Test
    void libraryIntegrationAnalysis_check_markdownIsUpToDateAndInputsAreValid() {
        PythonScript.Result result = SCRIPT.run("--check");

        assertThat(result.exitCode()).as("library_integration_analysis.py --check:%n%s", result.output()).isZero();
    }

    @Test
    void libraryIntegrationAnalysis_selfTest_passes() {
        PythonScript.Result result = SCRIPT.run("--self-test");

        assertThat(result.exitCode()).as("library_integration_analysis.py --self-test:%n%s", result.output())
                                     .isZero();
    }
}
