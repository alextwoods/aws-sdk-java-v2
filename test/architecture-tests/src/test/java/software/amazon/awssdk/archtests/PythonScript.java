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
import java.util.concurrent.TimeUnit;

/**
 * Runs one of the {@code scripts/schema-serde} Python scripts that generate a design document, for the tests that
 * check the document. The interpreter and the script path come from system properties that the module's
 * {@code pom.xml} sets.
 */
final class PythonScript {
    private static final long TIMEOUT_SECONDS = 60;

    private final String pythonProperty;
    private final String scriptProperty;

    /**
     * @param pythonProperty the system property naming the Python interpreter
     * @param scriptProperty the system property naming the script
     */
    PythonScript(String pythonProperty, String scriptProperty) {
        this.pythonProperty = pythonProperty;
        this.scriptProperty = scriptProperty;
    }

    /**
     * Run the script with one argument, with standard error merged into the output.
     */
    Result run(String argument) {
        String python = property(pythonProperty);
        Path script = Paths.get(property(scriptProperty)).toAbsolutePath().normalize();
        ProcessBuilder builder = new ProcessBuilder(python, script.toString(), argument).redirectErrorStream(true);
        try {
            Process process = builder.start();
            process.getOutputStream().close();
            String output = readFully(process.getInputStream());
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                fail("%s %s %s did not finish within %d seconds", python, script, argument, TIMEOUT_SECONDS);
            }
            return new Result(process.exitValue(), output);
        } catch (IOException e) {
            throw new AssertionError("Could not run " + python + " " + script + " " + argument
                                     + ". Python 3 is required; select an interpreter with -D" + pythonProperty, e);
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

    /**
     * The exit code and the merged output of one run.
     */
    static final class Result {
        private final int exitCode;
        private final String output;

        private Result(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }

        int exitCode() {
            return exitCode;
        }

        String output() {
            return output;
        }
    }
}
