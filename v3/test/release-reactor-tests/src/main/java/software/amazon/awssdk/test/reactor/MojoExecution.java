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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One mojo execution reported by a (non-parallel, batch-mode) Maven build log, for example
 * {@code [INFO] --- compiler:3.16.0:compile (default-compile) @ aws-codegen ---}.
 *
 * @param line zero-based index of the reporting line in the log; orders executions across the whole reactor
 * @param prefix plugin goal prefix, for example {@code compiler}
 * @param version plugin version
 * @param goal goal name, for example {@code compile}
 * @param executionId execution id, for example {@code default-compile}
 * @param artifactId artifactId of the module the execution ran in
 */
public record MojoExecution(int line, String prefix, String version, String goal, String executionId,
                            String artifactId) {

    private static final Pattern MOJO_LINE = Pattern.compile(
        "^\\[INFO] --- (?<prefix>[^:\\s]+):(?<version>[^:\\s]+):(?<goal>\\S+) \\((?<execution>[^)]+)\\) @ "
        + "(?<artifactId>\\S+) ---$");

    /** Every mojo execution in {@code buildLog}, in log order. */
    public static List<MojoExecution> parse(List<String> buildLog) {
        List<MojoExecution> result = new ArrayList<>();
        for (int i = 0; i < buildLog.size(); i++) {
            Matcher m = MOJO_LINE.matcher(buildLog.get(i));
            if (m.matches()) {
                result.add(new MojoExecution(i, m.group("prefix"), m.group("version"), m.group("goal"),
                                             m.group("execution"), m.group("artifactId")));
            }
        }
        return result;
    }

    /** {@code prefix:goal (executionId)}, the version-independent form used in expectations and diagnostics. */
    public String key() {
        return prefix + ":" + goal + " (" + executionId + ")";
    }
}
