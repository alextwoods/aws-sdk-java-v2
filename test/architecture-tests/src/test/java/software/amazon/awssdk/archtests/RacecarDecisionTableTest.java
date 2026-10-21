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

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Checks the racecar decision table (design D.6, Requirement 37.1) through
 * {@code scripts/schema-serde/racecar_decisions.py}, which generates {@code docs/design/schema-serde/racecar-decisions.md}
 * from {@code racecar-decisions.yaml}.
 *
 * <p>The script's {@code --check} mode fails when the committed Markdown differs from the generated output, a racecar
 * phase id is missing or repeated, a decision is outside the D.6 vocabulary, a required field is empty, D2, H1 or H5 is
 * not Deferred, or a row applied in phase 5 has no link to its paired A/B record (task 9.19). This test also loads every
 * class that an Applied or Superseded row names, which needs this module's classpath.
 *
 * <p>The script path and the Python interpreter come from the module's {@code pom.xml}
 * ({@code -Dracecar-decisions.python=...} selects another interpreter).
 */
class RacecarDecisionTableTest {
    private static final PythonScript SCRIPT = new PythonScript("racecar-decisions.python", "racecar-decisions.script");

    @Test
    void racecarDecisions_check_markdownIsUpToDateAndYamlIsValid() {
        PythonScript.Result result = SCRIPT.run("--check");

        assertThat(result.exitCode()).as("racecar_decisions.py --check:%n%s", result.output()).isZero();
    }

    @Test
    void racecarDecisions_appliedAndSupersededRows_nameClassesOnTheClasspath() {
        PythonScript.Result result = SCRIPT.run("--list-classes");
        assertThat(result.exitCode()).as("racecar_decisions.py --list-classes:%n%s", result.output()).isZero();

        List<String> classes = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String line : result.output().split("\n")) {
            if (line.isEmpty()) {
                continue;
            }
            String[] idAndClass = line.split("\t", 2);
            assertThat(idAndClass).as("output line %s", line).hasSize(2);
            classes.add(idAndClass[1]);
            if (!isOnClasspath(idAndClass[1])) {
                missing.add(idAndClass[0] + ": " + idAndClass[1]);
            }
        }

        assertThat(classes).as("classes named by Applied and Superseded rows").isNotEmpty();
        assertThat(missing).as("classes named in racecar-decisions.yaml that are not on the classpath").isEmpty();
    }

    private static boolean isOnClasspath(String className) {
        try {
            Class.forName(className, false, RacecarDecisionTableTest.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
