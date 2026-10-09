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

import java.util.ArrayList;
import java.util.List;

/**
 * The exact argv of one forked JVM: {@code [pinning prefix] java <jvm flags> -cp <classpath> <main> <args>}. The JVM
 * flags are the run's single flag list, shared verbatim by client and mock service (Req 20.5); only the pinning
 * prefix, main class and program arguments differ by role.
 *
 * @param role {@code client} or {@code mock-service}
 */
public record JvmLaunchSpec(String role, List<String> pinningPrefix, String javaExecutable, List<String> jvmFlags,
                            String classpath, String mainClass, List<String> programArgs) {

    public JvmLaunchSpec {
        pinningPrefix = List.copyOf(pinningPrefix);
        jvmFlags = List.copyOf(jvmFlags);
        programArgs = List.copyOf(programArgs);
        for (String flag : jvmFlags) {
            if (flag.equals("-cp") || flag.equals("-classpath") || flag.startsWith("--class-path")) {
                throw new IllegalArgumentException("the classpath is set by the harness, not by JVM flags: " + flag);
            }
        }
    }

    public List<String> command() {
        List<String> cmd = new ArrayList<>(pinningPrefix.size() + jvmFlags.size() + programArgs.size() + 4);
        cmd.addAll(pinningPrefix);
        cmd.add(javaExecutable);
        cmd.addAll(jvmFlags);
        cmd.add("-cp");
        cmd.add(classpath);
        cmd.add(mainClass);
        cmd.addAll(programArgs);
        return cmd;
    }
}
