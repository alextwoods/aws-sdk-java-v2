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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RequestBodyTest {
    private static byte[] read(RequestBody body) throws IOException {
        try (InputStream in = body.contentStreamProvider().newStream()) {
            return in.readAllBytes();
        }
    }

    @Test
    void byteBodiesAreCopiedAndReplayable() throws IOException {
        byte[] bytes = {1, 2, 3};
        RequestBody body = RequestBody.fromBytes(bytes);
        bytes[0] = 9;
        assertTrue(body.replayable());
        assertEquals(Optional.of(3L), body.optionalContentLength());
        assertEquals("application/octet-stream", body.contentType());
        assertArrayEquals(new byte[] {1, 2, 3}, read(body));
        assertArrayEquals(new byte[] {1, 2, 3}, read(body));
        assertEquals(Optional.of(0L), RequestBody.empty().optionalContentLength());
    }

    @Test
    void stringAndBufferBodies() throws IOException {
        RequestBody text = RequestBody.fromString("h\u00e9");
        assertEquals("text/plain; charset=UTF-8", text.contentType());
        assertEquals(Optional.of(3L), text.optionalContentLength());
        assertEquals("h\u00e9", new String(read(text), StandardCharsets.UTF_8));

        ByteBuffer buffer = ByteBuffer.wrap(new byte[] {5, 6, 7});
        buffer.position(1);
        assertArrayEquals(new byte[] {6, 7}, read(RequestBody.fromByteBuffer(buffer)));
        assertEquals(1, buffer.position());
    }

    @Test
    void fileBodiesReopenTheFile(@TempDir Path dir) throws IOException {
        Path file = Files.write(dir.resolve("f"), new byte[] {4, 2});
        RequestBody body = RequestBody.fromFile(file);
        assertTrue(body.replayable());
        assertEquals(Optional.of(2L), body.optionalContentLength());
        assertArrayEquals(new byte[] {4, 2}, read(body));
        assertArrayEquals(new byte[] {4, 2}, read(body));
    }

    @Test
    void inputStreamBodiesAreOneShotAndLeaveTheCallerStreamOpen() throws IOException {
        boolean[] closed = {false};
        InputStream source = new ByteArrayInputStream(new byte[] {1, 2}) {
            @Override
            public void close() {
                closed[0] = true;
            }
        };
        RequestBody body = RequestBody.fromInputStream(source, 2);
        assertFalse(body.replayable());
        assertArrayEquals(new byte[] {1, 2}, read(body));
        assertFalse(closed[0], "the caller owns the stream");
        assertThrows(IllegalStateException.class, () -> body.contentStreamProvider().newStream());
    }

    @Test
    void contentProviderBodiesMayHaveUnknownLength() {
        RequestBody body = RequestBody.fromContentProvider(() -> new ByteArrayInputStream(new byte[0]), "text/csv");
        assertEquals(Optional.empty(), body.optionalContentLength());
        assertEquals("text/csv", body.contentType());
        assertThrows(IllegalArgumentException.class,
            () -> RequestBody.fromContentProvider(() -> InputStream.nullInputStream(), -1, "text/csv"));
    }
}
