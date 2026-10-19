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
import static org.assertj.core.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
    private static final long TIMEOUT_SECONDS = 60;

    @Test
    void racecarDecisions_check_markdownIsUpToDateAndYamlIsValid() {
        ScriptResult result = runScript("--check");

        assertThat(result.exitCode).as("racecar_decisions.py --check:%n%s", result.output).isZero();
    }

    @Test
    void racecarDecisions_appliedAndSupersededRows_nameClassesOnTheClasspath() {
        ScriptResult result = runScript("--list-classes");
        assertThat(result.exitCode).as("racecar_decisions.py --list-classes:%n%s", result.output).isZero();

        List<String> classes = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String line : result.output.split("\n")) {
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

    private static ScriptResult runScript(String mode) {
        String python = property("racecar-decisions.python");
        Path script = Paths.get(property("racecar-decisions.script")).toAbsolutePath().normalize();
        ProcessBuilder builder = new ProcessBuilder(python, script.toString(), mode).redirectErrorStream(true);
        try {
            Process process = builder.start();
            process.getOutputStream().close();
            String output = readFully(process.getInputStream());
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                fail("%s %s %s did not finish within %d seconds", python, script, mode, TIMEOUT_SECONDS);
            }
            return new ScriptResult(process.exitValue(), output);
        } catch (IOException e) {
            throw new AssertionError("Could not run " + python + " " + script + " " + mode
                                     + ". Python 3 is required; select an interpreter with -Dracecar-decisions.python",
                                     e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while running " + script, e);
        }
    }

    private static String readFully(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String property(String name) {
        String value = System.getProperty(name);
        assertThat(value).as("system property %s (set by the architecture-tests pom)", name).isNotBlank();
        return value;
    }

    private static final class ScriptResult {
        private final int exitCode;
        private final String output;

        private ScriptResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }
}
