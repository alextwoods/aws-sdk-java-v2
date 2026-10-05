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
 * One reason {@link LaunchGate} blocks a release candidate.
 *
 * @param code     the problem kind
 * @param stage    the earliest stage this problem blocks; a problem blocking {@link LaunchGate.Stage#PACKAGING} also
 *                 blocks {@link LaunchGate.Stage#GENERAL_AVAILABILITY}
 * @param subject  the manifest entry ID, or {@code candidate}, {@code evidence} or {@code manifest} for problems not
 *                 about one entry
 * @param category the launch category of the entry, or {@code null} for problems not about one entry
 * @param message  the human-readable detail
 */
public record LaunchProblem(Code code, LaunchGate.Stage stage, String subject, LaunchCategory category,
                            String message) implements Comparable<LaunchProblem> {
    private static final Comparator<LaunchProblem> ORDER =
        Comparator.comparing(LaunchProblem::subject).thenComparing(LaunchProblem::code)
                  .thenComparing(LaunchProblem::message);

    /** Problem kinds. */
    public enum Code {
        /**
         * The manifest itself fails {@link ManifestValidator} (blocks packaging; an incomplete frozen baseline, 16.1,
         * blocks general availability).
         */
        INVALID_MANIFEST,
        /** An expected release artifact (for example a V3 BOM entry) is not part of the candidate. */
        CANDIDATE_INCOMPLETE,
        /** A non-deprecated {@code PRESERVED} entry's capability is not shipped by the candidate (16.4-16.10). */
        NOT_SHIPPED,
        /** An {@code ADAPTED} entry's replacement is not shipped by the candidate (16.12). */
        REPLACEMENT_NOT_SHIPPED,
        /** The entry's owning V3 artifact is not part of the candidate. */
        OWNER_NOT_SHIPPED,
        /** A non-deprecated entry has no verification evidence hook (16.14). */
        MISSING_EVIDENCE_HOOK,
        /**
         * An evidence ID named by the manifest is not a registered evidence hook, or a registered hook names no
         * verification that exists in the candidate.
         */
        UNKNOWN_EVIDENCE_HOOK,
        /** A non-deprecated entry is {@code EXCLUDED}, so it lacks a shipped capability (16.15). */
        EXCLUDED_NOT_DEPRECATED,
        /** The candidate identity is missing, or the manifest, artifacts and evidence disagree on it (16.14). */
        CANDIDATE_MISMATCH,
        /** A non-deprecated entry has no candidate-specific evidence record (16.14). */
        MISSING_CANDIDATE_EVIDENCE,
        /** A candidate evidence record did not pass (16.15). */
        FAILED_CANDIDATE_EVIDENCE,
        /** A candidate evidence record's expected and actual outcomes are blank or disagree although it passed. */
        INCONSISTENT_CANDIDATE_EVIDENCE,
        /** A candidate evidence record names an evidence ID that is not a hook of its entry. */
        UNBOUND_CANDIDATE_EVIDENCE,
        /**
         * An evidence hook binding (blocks packaging) or a candidate evidence record (blocks general availability)
         * names an entry the manifest does not contain.
         */
        UNKNOWN_EVIDENCE_ENTRY
    }

    public LaunchProblem {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(message, "message");
    }

    @Override
    public int compareTo(LaunchProblem o) {
        return ORDER.compare(this, o);
    }

    @Override
    public String toString() {
        return code + " [" + subject + (category == null ? "" : ", " + category) + "]: " + message;
    }
}
