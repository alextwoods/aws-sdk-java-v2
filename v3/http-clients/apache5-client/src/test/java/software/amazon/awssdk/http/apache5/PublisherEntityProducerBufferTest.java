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

package software.amazon.awssdk.http.apache5;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.nio.DataStreamChannel;
import org.junit.jupiter.api.Test;

/**
 * {@link PublisherEntityProducer#ofBuffer}: an in-memory request body is written from the caller's buffer, in as many
 * writes as the connection needs, and ends the stream exactly once. Regression for the async Apache5 transport
 * allocating ~128 KiB per exchange (a copy plus two 64 KiB publisher chunk buffers) for a few-hundred-byte body.
 */
class PublisherEntityProducerBufferTest {

    /** A connection that accepts at most {@code perWrite} bytes per write and records everything. */
    private static final class RecordingChannel implements DataStreamChannel {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final int perWrite;
        int writes;
        int ends;

        RecordingChannel(int perWrite) {
            this.perWrite = perWrite;
        }

        @Override
        public void requestOutput() {
        }

        @Override
        public int write(ByteBuffer src) {
            writes++;
            int n = Math.min(perWrite, src.remaining());
            for (int i = 0; i < n; i++) {
                bytes.write(src.get());
            }
            return n;
        }

        @Override
        public void endStream() {
            ends++;
        }

        @Override
        public void endStream(List<? extends Header> trailers) {
            ends++;
        }
    }

    @Test
    void writesTheBufferAndEndsOnceAcrossPartialWrites() throws IOException {
        byte[] body = "{\"TableName\":\"benchmark-table\",\"Key\":{\"id\":{\"S\":\"key-00001\"}}}"
            .getBytes(StandardCharsets.UTF_8);
        AtomicInteger written = new AtomicInteger();
        PublisherEntityProducer producer =
            PublisherEntityProducer.ofBuffer(ByteBuffer.wrap(body).asReadOnlyBuffer(), body.length,
                                             written::incrementAndGet);
        RecordingChannel channel = new RecordingChannel(10);
        assertEquals(1, producer.available(), "an unstarted body asks for a write opportunity");
        int rounds = 0;
        while (channel.ends == 0 && rounds++ < 100) {
            producer.produce(channel);
        }
        assertArrayEquals(body, channel.bytes.toByteArray());
        assertEquals(1, channel.ends);
        assertEquals(1, written.get());
        assertEquals(body.length, producer.getContentLength());
        producer.produce(channel);
        assertEquals(1, channel.ends, "produce after the end does not end the stream again");
        assertEquals(1, written.get());
    }

    @Test
    void emptyBufferEndsTheStreamWithoutData() throws IOException {
        AtomicInteger written = new AtomicInteger();
        PublisherEntityProducer producer =
            PublisherEntityProducer.ofBuffer(ByteBuffer.allocate(0), 0, written::incrementAndGet);
        RecordingChannel channel = new RecordingChannel(Integer.MAX_VALUE);
        producer.produce(channel);
        assertEquals(0, channel.bytes.size());
        assertEquals(1, channel.ends);
        assertEquals(1, written.get());
    }

    @Test
    void writingASmallBodyAllocatesNoChunkBuffer() throws IOException {
        com.sun.management.ThreadMXBean threads =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        ByteBuffer body = ByteBuffer.wrap(new byte[470]).asReadOnlyBuffer();
        DataStreamChannel sink = new DataStreamChannel() {
            @Override
            public void requestOutput() {
            }

            @Override
            public int write(ByteBuffer src) {
                int n = src.remaining();
                src.position(src.limit());
                return n;
            }

            @Override
            public void endStream() {
            }

            @Override
            public void endStream(List<? extends Header> trailers) {
            }
        };
        Runnable none = () -> { };
        // Warm the path so class loading is not counted.
        for (int i = 0; i < 1000; i++) {
            PublisherEntityProducer.ofBuffer(body.duplicate(), 470, none).produce(sink);
        }
        int exchanges = 1000;
        long before = threads.getCurrentThreadAllocatedBytes();
        for (int i = 0; i < exchanges; i++) {
            PublisherEntityProducer.ofBuffer(body.duplicate(), 470, none).produce(sink);
        }
        long perExchange = (threads.getCurrentThreadAllocatedBytes() - before) / exchanges;
        // The old publisher path allocated a 470-byte copy plus 2 x 64 KiB per exchange; this one allocates a producer
        // and a buffer view (well under 1 KiB). 4 KiB leaves room for JIT/profiling noise, not for a chunk buffer.
        assertTrue(perExchange < 4096, "allocated " + perExchange + " bytes per exchange");
    }
}
