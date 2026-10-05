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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class SdkChecksumTest {
    private static final byte[] CHECK = "123456789".getBytes(StandardCharsets.US_ASCII);

    @ParameterizedTest
    @CsvSource({
        "CRC32, cbf43926",
        "CRC32C, e3069283",
        "CRC64NVME, ae8b14860a799888",
        "MD5, 25f9e794323b453885f5181f1b624d0b",
        "SHA1, f7c3bc1d808e04732adf679965ccc34ca7ae3441",
        "SHA256, 15e2b0d3c33891ebb0f1ef609ec419420c20e320ce94c65fbc8c3312448eb225"
    })
    void digest_whenCheckInput_matchesPublishedCheckValue(ChecksumAlgorithm algorithm, String hex) {
        SdkChecksum checksum = SdkChecksum.create(algorithm);
        checksum.update(CHECK, 0, CHECK.length);
        assertThat(HexFormat.of().formatHex(checksum.digest())).isEqualTo(hex);
    }

    @Test
    void digest_sha512_matchesJdk() throws Exception {
        SdkChecksum checksum = SdkChecksum.create(ChecksumAlgorithm.SHA512);
        checksum.update(CHECK, 0, CHECK.length);
        assertThat(checksum.digest()).isEqualTo(MessageDigest.getInstance("SHA-512").digest(CHECK));
    }

    @Test
    void crc64Nvme_whenLongAndUnalignedInput_matchesBitwiseReference() {
        Random random = new Random(42);
        for (int length : new int[] {0, 1, 7, 8, 9, 63, 64, 1000, 65_537}) {
            byte[] data = new byte[length + 3];
            random.nextBytes(data);
            SdkChecksum checksum = SdkChecksum.create(ChecksumAlgorithm.CRC64NVME);
            checksum.update(data, 3, length);
            long expected = bitwiseCrc64Nvme(data, 3, length);
            assertThat(ByteBuffer.wrap(checksum.digest()).getLong()).as("length %d", length).isEqualTo(expected);
        }
    }

    @ParameterizedTest
    @EnumSource(value = ChecksumAlgorithm.class, names = {"CRC32", "CRC32C", "CRC64NVME", "SHA1", "SHA256", "SHA512"})
    void update_whenSplitAcrossArraysAndDirectBuffers_equalsOneShotDigest(ChecksumAlgorithm algorithm) {
        byte[] data = new byte[10_000];
        new Random(7).nextBytes(data);
        SdkChecksum whole = SdkChecksum.create(algorithm);
        whole.update(data, 0, data.length);

        SdkChecksum split = SdkChecksum.create(algorithm);
        split.update(data, 0, 1234);
        ByteBuffer direct = ByteBuffer.allocateDirect(data.length - 1234);
        direct.put(data, 1234, data.length - 1234).flip();
        split.update(direct);

        assertThat(direct.hasRemaining()).isFalse();
        assertThat(split.digest()).isEqualTo(whole.digest()).hasSize(algorithm.digestLength());
    }

    @Test
    void digest_resetsTheChecksum() {
        SdkChecksum checksum = SdkChecksum.create(ChecksumAlgorithm.CRC32);
        checksum.update(CHECK, 0, CHECK.length);
        checksum.digest();
        assertThat(checksum.digest()).isEqualTo(new byte[4]);
    }

    @ParameterizedTest
    @EnumSource(value = ChecksumAlgorithm.class, names = {"XXHASH64", "XXHASH3", "XXHASH128"})
    void create_whenAlgorithmNotImplemented_throws(ChecksumAlgorithm algorithm) {
        assertThat(algorithm.isImplemented()).isFalse();
        assertThatThrownBy(() -> SdkChecksum.create(algorithm))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining(algorithm.algorithmId());
    }

    @Test
    void algorithm_wireIdentity() {
        assertThat(ChecksumAlgorithm.CRC64NVME.headerName()).isEqualTo("x-amz-checksum-crc64nvme");
        assertThat(ChecksumAlgorithm.fromAlgorithmId("crc32c")).contains(ChecksumAlgorithm.CRC32C);
        assertThat(ChecksumAlgorithm.fromAlgorithmId("CRC16")).isEmpty();
        assertThat(ChecksumAlgorithm.CRC32.base64Length()).isEqualTo(8);
        assertThat(ChecksumAlgorithm.SHA256.base64Length()).isEqualTo(44);
        assertThat(ChecksumAlgorithm.MD5.supportsFlexibleChecksums()).isFalse();
        assertThat(ChecksumAlgorithm.MD5.isImplemented()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = ChecksumAlgorithm.class, names = {"CRC32", "CRC32C", "CRC64NVME", "SHA1", "SHA256", "SHA512",
                                                          "MD5"})
    void base64Length_matchesEncodedDigest(ChecksumAlgorithm algorithm) {
        assertThat(SdkChecksum.base64(algorithm, CHECK)).hasSize(algorithm.base64Length());
    }

    private static long bitwiseCrc64Nvme(byte[] data, int offset, int length) {
        long crc = -1L;
        for (int i = offset; i < offset + length; i++) {
            crc ^= data[i] & 0xFFL;
            for (int bit = 0; bit < 8; bit++) {
                crc = (crc & 1) != 0 ? (crc >>> 1) ^ 0x9A6C9329AC4BC9B5L : crc >>> 1;
            }
        }
        return ~crc;
    }
}
