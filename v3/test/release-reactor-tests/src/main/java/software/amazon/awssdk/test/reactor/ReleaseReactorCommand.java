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

package software.amazon.awssdk.test.reactor;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * The single supported V3 build command, run from the repository root:
 *
 * <pre>{@code mvn -f v3/pom.xml verify}</pre>
 *
 * <p>It compiles, tests, and packages the full release reactor (Requirement 1.3). {@link #run} executes exactly these
 * arguments through {@link MavenFork}, adding only output-formatting flags ({@code -B -ntp}) and the caller's
 * local-repository/offline settings, none of which change which modules or phases are built.
 */
public final class ReleaseReactorCommand {
    /** Arguments of the supported command after the {@code mvn} executable. */
    public static final List<String> ARGUMENTS = List.of("-f", "v3/pom.xml", "verify");

    /** Environment variable set on every forked build so that reactor-forking tests never recurse into themselves. */
    public static final String NESTED_BUILD_ENVIRONMENT_VARIABLE = "V3_CLEAN_CHECKOUT_REACTOR_TEST_NESTED";

    private ReleaseReactorCommand() {
    }

    /**
     * Runs the supported command in {@code checkoutRoot} with the JDK at {@code javaHome}, writing combined output to
     * {@code log}.
     *
     * @param mavenHome Maven installation to use, or {@code null} to use {@code mvn} from {@code PATH}
     * @param localRepository local repository to use, or {@code null} for Maven's default
     */
    public static MavenFork.Result run(Path checkoutRoot, Path javaHome, Path mavenHome, Path localRepository,
                                       boolean offline, Path log, Duration timeout)
        throws IOException, InterruptedException {
        return new MavenFork(mavenHome, localRepository, offline, timeout).run(checkoutRoot, javaHome, ARGUMENTS, log);
    }

    /** The supported command's arguments followed by {@code extraArguments} (selection or property overrides). */
    public static List<String> argumentsWith(String... extraArguments) {
        List<String> arguments = new ArrayList<>(ARGUMENTS);
        arguments.addAll(List.of(extraArguments));
        return List.copyOf(arguments);
    }
}
