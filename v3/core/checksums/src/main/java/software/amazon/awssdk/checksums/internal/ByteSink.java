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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** A growable byte array that hands out its content as a fresh buffer; one per transform output. */
final class ByteSink {
    private static final ByteBuffer EMPTY = ByteBuffer.allocate(0).asReadOnlyBuffer();

    private byte[] bytes;
    private int size;

    ByteSink(int initialCapacity) {
        this.bytes = new byte[Math.max(16, initialCapacity)];
    }

    ByteSink write(byte[] source, int offset, int length) {
        ensure(length);
        System.arraycopy(source, offset, bytes, size, length);
        size += length;
        return this;
    }

    ByteSink write(ByteBuffer source) {
        int length = source.remaining();
        ensure(length);
        source.get(bytes, size, length);
        size += length;
        return this;
    }

    ByteSink writeAscii(String text) {
        byte[] ascii = text.getBytes(StandardCharsets.US_ASCII);
        return write(ascii, 0, ascii.length);
    }

    ByteSink writeByte(int value) {
        ensure(1);
        bytes[size++] = (byte) value;
        return this;
    }

    /** The content written so far as a buffer the sink never touches again; the sink is emptied. */
    ByteBuffer drain() {
        if (size == 0) {
            return EMPTY;
        }
        ByteBuffer result = ByteBuffer.wrap(bytes, 0, size);
        bytes = new byte[bytes.length];
        size = 0;
        return result;
    }

    private void ensure(int extra) {
        if (size + extra > bytes.length) {
            bytes = Arrays.copyOf(bytes, Math.max(bytes.length * 2, size + extra));
        }
    }
}
