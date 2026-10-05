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

package software.amazon.awssdk.tools.paritymanifest;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/** Every problem found in a manifest, rules file or generation run, sorted by subject. */
public final class ManifestValidationException extends Exception {
    private static final long serialVersionUID = 1L;

    private final transient List<ManifestProblem> problems;

    public ManifestValidationException(Collection<ManifestProblem> problems) {
        super(render(problems));
        this.problems = problems.stream().sorted().toList();
    }

    /** The problems, sorted by subject, code and message. */
    public List<ManifestProblem> problems() {
        return problems;
    }

    /** Throws if {@code problems} is not empty. */
    static void throwIfAny(Collection<ManifestProblem> problems) throws ManifestValidationException {
        if (!problems.isEmpty()) {
            throw new ManifestValidationException(problems);
        }
    }

    private static String render(Collection<ManifestProblem> problems) {
        return problems.size() + " Parity Manifest problem(s):" + problems.stream().sorted()
                                                                        .map(p -> "\n  " + p)
                                                                        .collect(Collectors.joining());
    }
}
