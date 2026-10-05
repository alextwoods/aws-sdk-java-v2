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

package software.amazon.awssdk.core.sync;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.core.ResponseBytes;

class ResponseTransformerTest {
    private static final String RESPONSE = "response";

    private static InputStream stream(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void toBytesReadsThePayload() throws Exception {
        ResponseTransformer<String, ResponseBytes<String>> transformer = ResponseTransformer.toBytes();
        ResponseBytes<String> bytes = transformer.transform(RESPONSE, stream("payload"));
        assertSame(RESPONSE, bytes.response());
        assertEquals("payload", bytes.asUtf8String());
        assertFalse(transformer.needsConnectionLeftOpen());
        assertEquals("BYTES", transformer.name());
    }

    @Test
    void toOutputStreamCopiesWithoutClosing() throws Exception {
        boolean[] closed = {false};
        ByteArrayOutputStream out = new ByteArrayOutputStream() {
            @Override
            public void close() {
                closed[0] = true;
            }
        };
        assertSame(RESPONSE, ResponseTransformer.<String>toOutputStream(out).transform(RESPONSE, stream("abc")));
        assertEquals("abc", out.toString(StandardCharsets.UTF_8));
        assertFalse(closed[0]);
    }

    @Test
    void toFileWritesANewFile(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("out.txt");
        assertSame(RESPONSE, ResponseTransformer.<String>toFile(file.toFile()).transform(RESPONSE, stream("data")));
        assertEquals("data", Files.readString(file));

        assertThrows(FileAlreadyExistsException.class,
                     () -> ResponseTransformer.<String>toFile(file).transform(RESPONSE, stream("other")));
        assertEquals("data", Files.readString(file), "an existing file is left alone");
    }

    @Test
    void toFileDeletesAPartialFile(@TempDir Path dir) {
        Path file = dir.resolve("partial.bin");
        InputStream failing = new InputStream() {
            private int reads;

            @Override
            public int read() throws IOException {
                if (reads++ < 10) {
                    return 1;
                }
                throw new IOException("connection reset");
            }
        };
        IOException e = assertThrows(IOException.class,
                                     () -> ResponseTransformer.<String>toFile(file).transform(RESPONSE, failing));
        assertEquals("connection reset", e.getMessage());
        assertFalse(Files.exists(file));
    }

    @Test
    void responseBytesCopiesUnlessUnsafe() {
        byte[] data = {1, 2, 3};
        ResponseBytes<String> copied = ResponseBytes.fromByteArray(RESPONSE, data);
        ResponseBytes<String> unsafe = ResponseBytes.fromByteArrayUnsafe(RESPONSE, data);
        data[0] = 9;

        assertArrayEquals(new byte[] {1, 2, 3}, copied.asByteArray());
        assertSame(data, unsafe.asByteArrayUnsafe());
        assertTrue(copied.asByteBuffer().isReadOnly());
        assertEquals(ResponseBytes.fromByteArray(RESPONSE, new byte[] {1, 2, 3}), copied);
        assertEquals(copied.hashCode(), ResponseBytes.fromByteArray(RESPONSE, new byte[] {1, 2, 3}).hashCode());
        assertNotEquals(copied, unsafe);
        assertEquals("ResponseBytes(3 bytes, response=response)", copied.toString());
        assertEquals("hi", ResponseBytes.fromInputStream(RESPONSE, stream("hi")).asString(StandardCharsets.UTF_8));
        assertThrows(NullPointerException.class, () -> ResponseBytes.fromByteArray(null, data));
    }
}
