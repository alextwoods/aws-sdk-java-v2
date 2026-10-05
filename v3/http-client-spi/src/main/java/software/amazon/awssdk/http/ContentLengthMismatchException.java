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

package software.amazon.awssdk.http;

import java.io.IOException;

/**
 * A {@link StreamingBody} source delivered fewer or more bytes than its declared content length. The message carries
 * only the two lengths, never body data.
 */
public final class ContentLengthMismatchException extends IOException {
    private static final long serialVersionUID = 1L;

    private final long expected;
    private final long actual;

    /**
     * @param expected the declared content length
     * @param actual the bytes delivered so far (for a long source, the count that exceeded {@code expected})
     */
    public ContentLengthMismatchException(long expected, long actual) {
        super("The body declared a content length of " + expected + " bytes but its source "
              + (actual > expected ? "supplied more (at least " + actual + ")" : "ended after " + actual));
        this.expected = expected;
        this.actual = actual;
    }

    /** The declared content length. */
    public long expected() {
        return expected;
    }

    /** The bytes counted when the mismatch was detected. */
    public long actual() {
        return actual;
    }
}
