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

package software.amazon.awssdk.awscodegen;

import java.util.regex.Pattern;

/**
 * The immutable revision of the canonical model source recorded in a service's model provenance (Requirements 2.12,
 * 22.2).
 *
 * <p>A revision is recorded verbatim, never abbreviated, case-folded or otherwise normalized, together with its kind:
 * <ul>
 *   <li>{@value #GIT_COMMIT_KIND}: the full Git commit object ID of the canonical model source, exactly 40 (SHA-1)
 *       or 64 (SHA-256) lower-case hexadecimal digits. Abbreviated or upper-case IDs are rejected because they are
 *       either ambiguous or a normalization of the real ID.</li>
 *   <li>{@value #MODEL_DIGEST_KIND}: for unpinned generation only, the content digest of the model files
 *       ({@code sha256:<64 hex>}), which is immutable but does not identify a source-control revision.</li>
 * </ul>
 *
 * @param value the exact revision
 * @param kind {@value #GIT_COMMIT_KIND} or {@value #MODEL_DIGEST_KIND}
 */
record ModelRevision(String value, String kind) {

    static final String GIT_COMMIT_KIND = "git-commit";
    static final String MODEL_DIGEST_KIND = "model-digest";

    private static final Pattern GIT_COMMIT = Pattern.compile("[0-9a-f]{40}|[0-9a-f]{64}");
    private static final Pattern SHA256_DIGEST = Pattern.compile("sha256:[0-9a-f]{64}");

    ModelRevision {
        if (GIT_COMMIT_KIND.equals(kind)) {
            requireGitCommit(value);
        } else if (!MODEL_DIGEST_KIND.equals(kind) || value == null || !SHA256_DIGEST.matcher(value).matches()) {
            throw new AwsCodegenException("Invalid model revision " + value + " of kind " + kind + ".");
        }
    }

    /** The revision of a request: its Git commit when pinned, otherwise the model content digest. */
    static ModelRevision of(String requestedRevision, String modelDigest) {
        return requestedRevision == null ? new ModelRevision(modelDigest, MODEL_DIGEST_KIND)
                                         : new ModelRevision(requestedRevision, GIT_COMMIT_KIND);
    }

    /**
     * Rejects anything but a full lower-case Git commit object ID.
     *
     * @throws AwsCodegenException naming the rejected value and the accepted forms
     */
    static String requireGitCommit(String revision) {
        if (revision == null || !GIT_COMMIT.matcher(revision).matches()) {
            throw new AwsCodegenException("Model revision '" + revision + "' is not an untruncated immutable source "
                                          + "revision: expected the full Git commit object ID of the canonical model "
                                          + "source (40 or 64 lower-case hexadecimal digits).");
        }
        return revision;
    }
}
