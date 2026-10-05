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

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.zip.CRC32;
import java.util.zip.CRC32C;
import software.amazon.awssdk.checksums.internal.Crc64Nvme;
import software.amazon.awssdk.checksums.internal.DigestChecksum;
import software.amazon.awssdk.checksums.internal.ZipChecksum;

/**
 * An incremental checksum of one {@link ChecksumAlgorithm}.
 *
 * <p>Instances are not thread-safe; use one per payload. {@link #digest()} returns the digest of every byte supplied
 * since creation or the last {@link #reset()}, and resets the checksum, as {@link java.security.MessageDigest} does.
 * CRC digests are the big-endian bytes of the CRC value.
 */
public interface SdkChecksum {

    /**
     * A new checksum of {@code algorithm}.
     *
     * @throws UnsupportedOperationException if this runtime does not implement {@code algorithm}
     */
    static SdkChecksum create(ChecksumAlgorithm algorithm) {
        return switch (algorithm) {
            case CRC32 -> new ZipChecksum(algorithm, new CRC32());
            case CRC32C -> new ZipChecksum(algorithm, new CRC32C());
            case CRC64NVME -> new ZipChecksum(algorithm, new Crc64Nvme());
            case SHA1 -> new DigestChecksum(algorithm, "SHA-1");
            case SHA256 -> new DigestChecksum(algorithm, "SHA-256");
            case SHA512 -> new DigestChecksum(algorithm, "SHA-512");
            case MD5 -> new DigestChecksum(algorithm, "MD5");
            case XXHASH64, XXHASH3, XXHASH128 ->
                throw new UnsupportedOperationException("The " + algorithm.algorithmId()
                                                        + " checksum algorithm is not implemented by this runtime");
        };
    }

    /** The Base64 digest of {@code bytes} under {@code algorithm}, the value of its checksum header. */
    static String base64(ChecksumAlgorithm algorithm, byte[] bytes) {
        SdkChecksum checksum = create(algorithm);
        checksum.update(bytes, 0, bytes.length);
        return Base64.getEncoder().encodeToString(checksum.digest());
    }

    /** The algorithm of this checksum. */
    ChecksumAlgorithm algorithm();

    /** Adds {@code length} bytes of {@code bytes} from {@code offset}. */
    void update(byte[] bytes, int offset, int length);

    /** Adds the remaining bytes of {@code buffer}, advancing its position to its limit. */
    void update(ByteBuffer buffer);

    /** The digest of the bytes added since creation or the last reset; the checksum is reset. */
    byte[] digest();

    /** The Base64 encoding of {@link #digest()}; the checksum is reset. */
    default String digestBase64() {
        return Base64.getEncoder().encodeToString(digest());
    }

    /** Discards every byte added so far. */
    void reset();
}
