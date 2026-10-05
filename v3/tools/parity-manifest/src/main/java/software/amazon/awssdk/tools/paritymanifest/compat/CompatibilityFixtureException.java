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

package software.amazon.awssdk.tools.paritymanifest.compat;

import java.util.List;

/**
 * Fixture generation failed: every in-scope entry problem, sorted, each formatted {@code [<entryId>] <message>}
 * (for example a scope entry with no applicable fixture, a claim without evidence, or an incomplete baseline class
 * path).
 */
public final class CompatibilityFixtureException extends Exception {
    private static final long serialVersionUID = 1L;

    private final transient List<String> problems;

    public CompatibilityFixtureException(List<String> problems) {
        super(problems.size() + " compatibility fixture problem(s):\n  " + String.join("\n  ", problems));
        this.problems = List.copyOf(problems);
    }

    /** The problems, sorted. */
    public List<String> problems() {
        return problems;
    }
}
