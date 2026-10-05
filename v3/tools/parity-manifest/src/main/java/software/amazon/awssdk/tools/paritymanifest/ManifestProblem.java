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

import java.util.Comparator;
import java.util.Objects;

/**
 * One manifest, rules or inventory problem.
 *
 * @param code    the problem kind
 * @param subject the entry ID, or {@code baseline}, {@code rule:<name>}, {@code source:<name>} or {@code $} (the
 *                document) for problems not about an entry
 * @param message the human-readable detail
 */
public record ManifestProblem(Code code, String subject, String message) implements Comparable<ManifestProblem> {
    private static final Comparator<ManifestProblem> ORDER =
        Comparator.comparing(ManifestProblem::subject).thenComparing(ManifestProblem::code)
                  .thenComparing(ManifestProblem::message);

    /** Problem kinds. */
    public enum Code {
        /** Two inventory items or entries share an ID. */
        DUPLICATE_ID,
        /** An ID does not follow its kind's scheme. */
        INVALID_ID,
        /** No rule matched an entry and no default applies. */
        UNMATCHED,
        /** Several equally specific rules matched an entry. */
        CONFLICTING_RULES,
        /** A field required by the entry's classification is absent or blank. */
        MISSING_FIELD,
        /** A field forbidden by the entry's classification is present. */
        FORBIDDEN_FIELD,
        /** A field value is not allowed, e.g. a migration kind an ADAPTED entry cannot use. */
        INVALID_VALUE,
        /** A replacement does not resolve in the supplied candidate inventory (Property 57 hook). */
        UNKNOWN_REPLACEMENT,
        /** The baseline identity is incomplete for a frozen baseline (Requirement 16.1). */
        BASELINE,
        /** An inventory source failed or none was supplied. */
        INVENTORY,
        /** A JSON document (manifest or rules) is malformed. */
        MALFORMED
    }

    public ManifestProblem {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(message, "message");
    }

    @Override
    public int compareTo(ManifestProblem o) {
        return ORDER.compare(this, o);
    }

    @Override
    public String toString() {
        return code + " [" + subject + "]: " + message;
    }
}
