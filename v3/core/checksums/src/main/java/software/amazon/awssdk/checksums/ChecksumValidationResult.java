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

package software.amazon.awssdk.checksums;

import java.util.Objects;
import java.util.Optional;

/**
 * Whether and how a response checksum was validated: the mechanism by which callers identify whether validation was
 * performed and which algorithm was validated (Smithy {@code httpChecksum} client behavior).
 *
 * @param status what happened
 * @param algorithm the validated algorithm, for {@link Status#VALIDATED} and {@link Status#VALIDATING_STREAM}
 */
public record ChecksumValidationResult(Status status, Optional<ChecksumAlgorithm> algorithm) {

    /** The outcome of response checksum validation. */
    public enum Status {
        /** The buffered payload matched the checksum before the response was deserialized. */
        VALIDATED,
        /**
         * The streaming payload is validated while it is read: the read reaching its end fails with
         * {@link ChecksumMismatchException} instead of signalling end-of-stream if the checksum differs.
         */
        VALIDATING_STREAM,
        /** Validation was not enabled for this call. */
        NOT_ENABLED,
        /** The response had no checksum header of a supported modeled algorithm. */
        CHECKSUM_NOT_FOUND,
        /**
         * The only checksums found are composite (multipart) checksums, {@code <base64>-<parts>}, which do not cover
         * the payload as one digest and so cannot be validated against it.
         */
        COMPOSITE_CHECKSUM_SKIPPED,
        /** The response is not a success response, whose payload is an error document and is not validated. */
        NOT_SUCCESS_RESPONSE
    }

    public ChecksumValidationResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(algorithm, "algorithm");
        boolean hasAlgorithm = status == Status.VALIDATED || status == Status.VALIDATING_STREAM;
        if (hasAlgorithm != algorithm.isPresent()) {
            throw new IllegalArgumentException("Status " + status + (hasAlgorithm ? " requires" : " has no")
                                               + " algorithm");
        }
    }

    /** A result without an algorithm. */
    public static ChecksumValidationResult of(Status status) {
        return new ChecksumValidationResult(status, Optional.empty());
    }

    /** A result of validating {@code algorithm}. */
    public static ChecksumValidationResult of(Status status, ChecksumAlgorithm algorithm) {
        return new ChecksumValidationResult(status, Optional.of(algorithm));
    }
}
