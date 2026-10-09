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

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** A {@link SystemProbe} replaying recorded host output, so parsing is tested for hosts other than the build host. */
final class RecordedProbe implements SystemProbe {
    private final String os;
    private final Map<String, String> commands = new HashMap<>();
    private final Map<Path, String> files = new HashMap<>();
    private final Map<String, Path> executables = new HashMap<>();

    RecordedProbe(String os) {
        this.os = os;
    }

    RecordedProbe command(String output, String... command) {
        commands.put(String.join(" ", command), output);
        return this;
    }

    RecordedProbe file(String file, String contents) {
        files.put(Path.of(file), contents);
        return this;
    }

    RecordedProbe executable(String name, String location) {
        executables.put(name, Path.of(location));
        return this;
    }

    @Override
    public String osName() {
        return os;
    }

    @Override
    public Optional<String> readFile(Path file) {
        return Optional.ofNullable(files.get(file));
    }

    @Override
    public Optional<String> run(List<String> command) {
        return Optional.ofNullable(commands.get(String.join(" ", command)));
    }

    @Override
    public Optional<Path> which(String executable) {
        return Optional.ofNullable(executables.get(executable));
    }
}
