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

package software.amazon.awssdk.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SdkBytesTest {

    @Test
    void factoriesCopyExceptTheUnsafeOne() {
        byte[] source = {1, 2, 3};
        SdkBytes copied = SdkBytes.fromByteArray(source);
        SdkBytes unsafe = SdkBytes.fromByteArrayUnsafe(source);
        source[0] = 9;
        assertArrayEquals(new byte[] {1, 2, 3}, copied.asByteArray());
        assertSame(source, unsafe.asByteArrayUnsafe());

        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {7, 8, 9});
        buffer.get();
        SdkBytes fromBuffer = SdkBytes.fromByteBuffer(buffer);
        assertEquals(1, buffer.position(), "the buffer position is not changed");
        assertArrayEquals(new byte[] {8, 9}, fromBuffer.asByteArray());
        assertEquals("héllo", SdkBytes.fromInputStream(new ByteArrayInputStream("héllo".getBytes(StandardCharsets.UTF_8)))
                                      .asUtf8String());
    }

    @Test
    void viewsDoNotExposeTheBytes() {
        SdkBytes bytes = SdkBytes.fromUtf8String("ab");
        bytes.asByteArray()[0] = 0;
        assertEquals("ab", bytes.asUtf8String());
        ByteBuffer view = bytes.asByteBuffer();
        assertTrue(view.isReadOnly());
        assertThrows(ReadOnlyBufferException.class, () -> view.put((byte) 1));
    }

    @Test
    void equalityHashAndStringFollowV2() {
        SdkBytes a = SdkBytes.fromByteArray(new byte[] {0x0f, (byte) 0xa0});
        assertEquals(a, SdkBytes.fromByteArray(new byte[] {0x0f, (byte) 0xa0}));
        assertEquals(a.hashCode(), SdkBytes.fromByteArray(new byte[] {0x0f, (byte) 0xa0}).hashCode());
        assertNotEquals(a, SdkBytes.fromByteArray(new byte[] {0x0f}));
        assertEquals("SdkBytes(bytes=0x0fa0)", a.toString());
    }
}
