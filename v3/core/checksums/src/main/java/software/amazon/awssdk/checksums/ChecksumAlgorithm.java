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

import java.util.Locale;
import java.util.Optional;

/**
 * Every checksum algorithm the Smithy {@code aws.protocols#httpChecksum} trait can model, with its wire identity.
 *
 * <p>{@link #algorithmId()} is the exact modeled enum value (for example {@code CRC32C}), and {@link #headerName()} is
 * the checksum name used as both HTTP header and aws-chunked trailer name, {@code x-amz-checksum-} followed by the
 * lower-case algorithm ID. The value of a checksum header is the Base64 encoding of the big-endian digest bytes.
 *
 * <p>Not every modeled algorithm is implemented by this runtime. {@link #supportsFlexibleChecksums()} is true for the
 * algorithms this runtime computes for {@code httpChecksum} requests and validates in responses. {@link #MD5} is
 * computed only for the legacy {@code Content-MD5} header of {@code httpChecksumRequired} operations, as in V2: a
 * caller who selects MD5 as a flexible checksum algorithm is rejected before transmission. The XXHash algorithms are
 * modeled but not implemented: selecting one fails the call, and response validation skips them.
 */
public enum ChecksumAlgorithm {
    CRC32("CRC32", 4, true),
    CRC32C("CRC32C", 4, true),
    CRC64NVME("CRC64NVME", 8, true),
    SHA1("SHA1", 20, true),
    SHA256("SHA256", 32, true),
    SHA512("SHA512", 64, true),
    MD5("MD5", 16, false),
    XXHASH64("XXHASH64", 8, false),
    XXHASH3("XXHASH3", 8, false),
    XXHASH128("XXHASH128", 16, false);

    /** Prefix of every checksum header and trailer name. */
    public static final String HEADER_PREFIX = "x-amz-checksum-";

    private final String algorithmId;
    private final String headerName;
    private final int digestLength;
    private final boolean flexible;

    ChecksumAlgorithm(String algorithmId, int digestLength, boolean flexible) {
        this.algorithmId = algorithmId;
        this.headerName = HEADER_PREFIX + algorithmId.toLowerCase(Locale.ROOT);
        this.digestLength = digestLength;
        this.flexible = flexible;
    }

    /**
     * The algorithm whose modeled ID equals {@code algorithmId}, ignoring case, as enum member values are matched on
     * the wire.
     */
    public static Optional<ChecksumAlgorithm> fromAlgorithmId(String algorithmId) {
        if (algorithmId == null) {
            return Optional.empty();
        }
        for (ChecksumAlgorithm algorithm : values()) {
            if (algorithm.algorithmId.equalsIgnoreCase(algorithmId)) {
                return Optional.of(algorithm);
            }
        }
        return Optional.empty();
    }

    /** The exact modeled enum value, for example {@code CRC64NVME}. */
    public String algorithmId() {
        return algorithmId;
    }

    /** The checksum header and trailer name, for example {@code x-amz-checksum-crc32c}. */
    public String headerName() {
        return headerName;
    }

    /** Length of the digest in bytes. */
    public int digestLength() {
        return digestLength;
    }

    /** Length of the Base64 encoding of the digest, the value length of a checksum header or trailer. */
    public int base64Length() {
        return 4 * ((digestLength + 2) / 3);
    }

    /** True if this runtime computes and validates the algorithm for {@code httpChecksum} operations. */
    public boolean supportsFlexibleChecksums() {
        return flexible;
    }

    /** True if this runtime can compute the algorithm: every flexible algorithm plus {@link #MD5}. */
    public boolean isImplemented() {
        return flexible || this == MD5;
    }
}
