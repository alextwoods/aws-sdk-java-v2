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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The host facts the harness reads: OS system properties, a few files, and short-lived commands. Host metadata and
 * pinning detection go through this interface, so their parsing is tested against recorded macOS and Linux output
 * (and a live probe of the build host) without depending on which host runs the build.
 */
public interface SystemProbe {

    /** {@code os.name}, lower-cased. */
    String osName();

    /** Contents of {@code file}, or empty if it does not exist or cannot be read. */
    Optional<String> readFile(Path file);

    /** Standard output and error of {@code command}, merged, if it exits 0 within a few seconds, else empty. */
    Optional<String> run(List<String> command);

    /** The absolute path of {@code executable} on {@code PATH}, or empty. */
    Optional<Path> which(String executable);

    default boolean isLinux() {
        return osName().startsWith("linux");
    }

    default boolean isMac() {
        return osName().startsWith("mac");
    }

    /** The probe of the running host. */
    static SystemProbe live() {
        return Live.INSTANCE;
    }

    /** The live implementation. */
    final class Live implements SystemProbe {
        static final Live INSTANCE = new Live();
        private static final long TIMEOUT_SECONDS = 5;

        private Live() {
        }

        @Override
        public String osName() {
            return System.getProperty("os.name", "unknown").toLowerCase(java.util.Locale.ROOT);
        }

        @Override
        public Optional<String> readFile(Path file) {
            try {
                return Files.isReadable(file) ? Optional.of(Files.readString(file, StandardCharsets.UTF_8))
                                              : Optional.empty();
            } catch (IOException | RuntimeException e) {
                return Optional.empty();
            }
        }

        @Override
        public Optional<String> run(List<String> command) {
            Process p = null;
            try {
                // Merged: java -XshowSettings writes its report to stderr.
                p = new ProcessBuilder(command).redirectErrorStream(true).start();
                p.getOutputStream().close();
                // Drain concurrently so neither a full pipe nor a hung command can outlive the timeout.
                Process started = p;
                CompletableFuture<byte[]> out = CompletableFuture.supplyAsync(() -> {
                    try {
                        return started.getInputStream().readAllBytes();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
                if (!p.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS) || p.exitValue() != 0) {
                    return Optional.empty();
                }
                return Optional.of(new String(out.get(TIMEOUT_SECONDS, TimeUnit.SECONDS), StandardCharsets.UTF_8));
            } catch (IOException | ExecutionException | TimeoutException e) {
                return Optional.empty();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            } finally {
                if (p != null && p.isAlive()) {
                    p.destroyForcibly();
                }
            }
        }

        @Override
        public Optional<Path> which(String executable) {
            String pathVariable = System.getenv("PATH");
            if (pathVariable == null) {
                return Optional.empty();
            }
            for (String dir : pathVariable.split(File.pathSeparator)) {
                if (dir.isEmpty()) {
                    continue;
                }
                Path candidate = Path.of(dir, executable);
                if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
                    return Optional.of(candidate.toAbsolutePath());
                }
            }
            return Optional.empty();
        }
    }
}
