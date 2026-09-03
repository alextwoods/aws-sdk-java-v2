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

package software.amazon.awssdk.core.internal.io;

import java.io.ByteArrayInputStream;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Internal response stream over an SDK-owned immutable byte-array range.
 *
 * <p>The unsafe buffer accessor is for in-call protocol parsing only. Callers must not mutate or retain the array.
 */
@SdkInternalApi
public final class BufferedResponseInputStream extends ByteArrayInputStream {
    private final byte[] buffer;
    private final int offset;
    private final int length;

    public BufferedResponseInputStream(byte[] buffer, int offset, int length) {
        super(validate(buffer, offset, length), offset, length);
        this.buffer = buffer;
        this.offset = offset;
        this.length = length;
    }

    public byte[] bufferUnsafe() {
        return buffer;
    }

    public int offset() {
        return offset;
    }

    public int length() {
        return length;
    }

    public void consumeToEnd() {
        this.pos = this.count;
    }

    private static byte[] validate(byte[] buffer, int offset, int length) {
        Objects.requireNonNull(buffer, "buffer");
        if (offset < 0 || length < 0 || offset > buffer.length - length) {
            throw new IndexOutOfBoundsException("Invalid response buffer range.");
        }
        return buffer;
    }
}
