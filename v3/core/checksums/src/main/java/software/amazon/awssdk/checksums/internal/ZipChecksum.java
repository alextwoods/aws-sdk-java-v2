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

package software.amazon.awssdk.checksums.internal;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.zip.Checksum;
import software.amazon.awssdk.checksums.ChecksumAlgorithm;
import software.amazon.awssdk.checksums.SdkChecksum;

/** An {@link SdkChecksum} over a {@link Checksum} CRC whose digest is the big-endian value truncated to its width. */
public final class ZipChecksum implements SdkChecksum {
    private final ChecksumAlgorithm algorithm;
    private final Checksum crc;

    public ZipChecksum(ChecksumAlgorithm algorithm, Checksum crc) {
        this.algorithm = Objects.requireNonNull(algorithm, "algorithm");
        this.crc = Objects.requireNonNull(crc, "crc");
    }

    @Override
    public ChecksumAlgorithm algorithm() {
        return algorithm;
    }

    @Override
    public void update(byte[] bytes, int offset, int length) {
        crc.update(bytes, offset, length);
    }

    @Override
    public void update(ByteBuffer buffer) {
        crc.update(buffer);
    }

    @Override
    public byte[] digest() {
        long value = crc.getValue();
        crc.reset();
        int width = algorithm.digestLength();
        byte[] digest = new byte[width];
        for (int i = width - 1; i >= 0; i--) {
            digest[i] = (byte) value;
            value >>>= 8;
        }
        return digest;
    }

    @Override
    public void reset() {
        crc.reset();
    }
}
