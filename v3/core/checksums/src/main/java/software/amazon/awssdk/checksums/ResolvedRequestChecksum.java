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

/**
 * The request checksum one attempt sends: its algorithm, the header or trailer name that carries it, where it is
 * placed, and whether the algorithm is the SDK default rather than the caller's choice.
 *
 * <p>{@link #name()} is {@code Content-MD5} for the legacy MD5 checksum of {@code httpChecksumRequired} operations and
 * {@link ChecksumAlgorithm#headerName()} otherwise.
 *
 * @param algorithm the checksum algorithm
 * @param name the header or trailer name
 * @param location header or trailer
 * @param defaulted true if the SDK chose the algorithm because the caller did not
 */
public record ResolvedRequestChecksum(ChecksumAlgorithm algorithm, String name, Location location,
                                      boolean defaulted) {

    /** Where a request checksum travels. */
    public enum Location {
        /** In an HTTP request header, computed before transmission. */
        HEADER,
        /** In the trailer of an {@code aws-chunked} body, computed while the body is sent. */
        TRAILER
    }

    public ResolvedRequestChecksum {
        Objects.requireNonNull(algorithm, "algorithm");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(location, "location");
    }
}
