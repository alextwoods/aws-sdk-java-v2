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

package software.amazon.awssdk.test.modulepath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Native-image smoke (task 18.9; Requirements 16.10, 19.13, 19.14): the fixture module is built into a GraalVM
 * native executable from the same module path ({@code --no-fallback}, so a missing reachability configuration fails the
 * build rather than falling back to a JVM) and run, completing one modeled call per supported native-image HTTP mode.
 *
 * <p>The supported native-image modes are those of V2's native-image test (test/sdk-native-image-test): the Apache 4,
 * Apache 5 and URLConnection sync clients and the Netty async client. The CRT client is not a supported native-image
 * mode.
 *
 * <p>native-image is found in {@code GRAALVM_HOME}, {@code JAVA_HOME}, this JVM's home or {@code PATH}. Without it the
 * test fails when {@code v3.nativeImage.required} is {@code true} (the general-availability launch gate) and is
 * otherwise aborted, which surefire reports as skipped; it never passes without running the executable.
 */
class NativeImageSmokeTest {
    static final List<String> NATIVE_MODES = List.of("apache-client:sync", "apache5-client:sync",
                                                     "url-connection-client:sync", "netty-nio-client:async");

    @Test
    void nativeExecutableCompletesAModeledCallOverEverySupportedHttpMode() throws Exception {
        Optional<Path> nativeImage = locateNativeImage();
        if (nativeImage.isEmpty()) {
            String message = "GraalVM native-image was not found in GRAALVM_HOME, JAVA_HOME, java.home or PATH";
            if (Boolean.getBoolean("v3.nativeImage.required")) {
                fail(message + ", and v3.nativeImage.required is true (Requirement 19.14)");
            }
            Assumptions.abort(message + "; the native-image smoke run did not execute");
        }

        Path app = SmokeFixture.compiledModule();
        Path output = SmokeFixture.workDirectory().resolve("native");
        Files.createDirectories(output);
        Path executable = output.resolve("module-path-smoke");
        SmokeFixture.Run build = SmokeFixture.run(
            List.of(nativeImage.get().toString(), "--no-fallback", "-H:+ReportExceptionStackTraces",
                    "--module-path", SmokeFixture.modulePath(List.of(app)),
                    "--module", SmokeFixture.MODULE + "/" + SmokeFixture.MAIN_CLASS,
                    "-o", executable.toString()),
            Duration.ofMinutes(25));
        assertThat(build.exit()).as("native-image build:%n%s", build.output()).isZero();

        List<String> command = new ArrayList<>();
        command.add(executable.toString());
        command.addAll(NATIVE_MODES);
        SmokeFixture.Run run = SmokeFixture.run(command, Duration.ofMinutes(2));
        assertThat(run.exit()).as("native executable:%n%s", run.output()).isZero();
        assertThat(run.lines("DONE")).as(run.output()).hasSize(1);
        List<String> modes = new ArrayList<>();
        for (String line : run.lines("MODE ")) {
            String[] fields = line.split(" ", 5);
            assertThat(fields[2]).as(line).isEqualTo("OK");
            assertThat(fields[4]).as(line).isEqualTo("[module-path-smoke]");
            modes.add(fields[1]);
        }
        assertThat(modes).as("every supported native-image HTTP mode completed a call")
                         .containsExactlyElementsOf(NATIVE_MODES);
    }

    private static Optional<Path> locateNativeImage() {
        String executable = System.getProperty("os.name").toLowerCase().contains("win") ? "native-image.cmd"
                                                                                        : "native-image";
        List<Path> candidates = new ArrayList<>();
        Stream.of(System.getenv("GRAALVM_HOME"), System.getenv("JAVA_HOME"), System.getProperty("java.home"))
              .filter(home -> home != null && !home.isBlank())
              .forEach(home -> candidates.add(Path.of(home, "bin", executable)));
        String searchPath = System.getenv("PATH");
        if (searchPath != null) {
            for (String dir : searchPath.split(File.pathSeparator)) {
                if (!dir.isBlank()) {
                    candidates.add(Path.of(dir, executable));
                }
            }
        }
        return candidates.stream().filter(Files::isExecutable).findFirst();
    }
}
