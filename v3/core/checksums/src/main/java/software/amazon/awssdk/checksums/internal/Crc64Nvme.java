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

import java.util.Objects;
import java.util.zip.Checksum;

/**
 * CRC-64/NVME in pure Java: reflected polynomial {@code 0x9A6C9329AC4BC9B5} (normal form {@code 0xAD93D23594C93659}),
 * initial value and final XOR all ones. The check value of {@code "123456789"} is {@code 0xAE8B14860A799888}.
 *
 * <p>Uses slicing-by-8 tables, so no native runtime (V2 required the CRT for this algorithm) is needed.
 */
public final class Crc64Nvme implements Checksum {
    private static final long POLYNOMIAL = 0x9A6C9329AC4BC9B5L;
    private static final long[][] TABLES = tables();

    private long crc = -1L;

    @Override
    public void update(int b) {
        crc = TABLES[0][(int) ((crc ^ b) & 0xFF)] ^ (crc >>> 8);
    }

    @Override
    public void update(byte[] bytes, int offset, int length) {
        Objects.checkFromIndexSize(offset, length, bytes.length);
        long value = crc;
        int index = offset;
        int end = offset + length;
        long[] t0 = TABLES[0];
        while (end - index >= 8) {
            value ^= (bytes[index] & 0xFFL)
                     | (bytes[index + 1] & 0xFFL) << 8
                     | (bytes[index + 2] & 0xFFL) << 16
                     | (bytes[index + 3] & 0xFFL) << 24
                     | (bytes[index + 4] & 0xFFL) << 32
                     | (bytes[index + 5] & 0xFFL) << 40
                     | (bytes[index + 6] & 0xFFL) << 48
                     | (bytes[index + 7] & 0xFFL) << 56;
            value = TABLES[7][(int) (value & 0xFF)]
                    ^ TABLES[6][(int) ((value >>> 8) & 0xFF)]
                    ^ TABLES[5][(int) ((value >>> 16) & 0xFF)]
                    ^ TABLES[4][(int) ((value >>> 24) & 0xFF)]
                    ^ TABLES[3][(int) ((value >>> 32) & 0xFF)]
                    ^ TABLES[2][(int) ((value >>> 40) & 0xFF)]
                    ^ TABLES[1][(int) ((value >>> 48) & 0xFF)]
                    ^ t0[(int) (value >>> 56)];
            index += 8;
        }
        while (index < end) {
            value = t0[(int) ((value ^ bytes[index++]) & 0xFF)] ^ (value >>> 8);
        }
        crc = value;
    }

    @Override
    public long getValue() {
        return ~crc;
    }

    @Override
    public void reset() {
        crc = -1L;
    }

    private static long[][] tables() {
        long[][] tables = new long[8][256];
        for (int n = 0; n < 256; n++) {
            long value = n;
            for (int bit = 0; bit < 8; bit++) {
                value = (value & 1) != 0 ? (value >>> 1) ^ POLYNOMIAL : value >>> 1;
            }
            tables[0][n] = value;
        }
        for (int n = 0; n < 256; n++) {
            long value = tables[0][n];
            for (int k = 1; k < 8; k++) {
                value = tables[0][(int) (value & 0xFF)] ^ (value >>> 8);
                tables[k][n] = value;
            }
        }
        return tables;
    }
}
