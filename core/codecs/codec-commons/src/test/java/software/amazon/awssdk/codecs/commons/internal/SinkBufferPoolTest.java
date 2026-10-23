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

package software.amazon.awssdk.codecs.commons.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class SinkBufferPoolTest {
    // More than the slots one thread probes, so holding them leaves this thread's probed slots empty.
    private static final int DRAIN = 8;

    @Test
    void acquire_returnsAnArrayOfTheBufferSize() {
        assertThat(SinkBufferPool.acquire()).hasSize(SinkBufferPool.BUFFER_SIZE);
    }

    @Test
    void release_thenAcquireOnTheSameThread_returnsTheSameArray() {
        List<byte[]> held = drain();
        try {
            byte[] buffer = new byte[SinkBufferPool.BUFFER_SIZE];
            SinkBufferPool.release(buffer);
            assertThat(SinkBufferPool.acquire()).isSameAs(buffer);
            assertThat(SinkBufferPool.acquire()).isNotSameAs(buffer).hasSize(SinkBufferPool.BUFFER_SIZE);
        } finally {
            held.forEach(SinkBufferPool::release);
        }
    }

    @Test
    void release_ofAnotherLength_isDropped() {
        List<byte[]> held = drain();
        try {
            byte[] grown = new byte[SinkBufferPool.BUFFER_SIZE * 2];
            SinkBufferPool.release(grown);
            SinkBufferPool.release(new byte[SinkBufferPool.BUFFER_SIZE - 1]);
            byte[] next = SinkBufferPool.acquire();
            assertThat(next).isNotSameAs(grown).hasSize(SinkBufferPool.BUFFER_SIZE);
        } finally {
            held.forEach(SinkBufferPool::release);
        }
    }

    @Test
    void concurrentHolders_neverShareAnArray() throws Exception {
        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicBoolean shared = new AtomicBoolean();
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                byte pattern = (byte) (t + 1);
                futures.add(executor.submit(() -> {
                    start.await();
                    for (int i = 0; i < 10_000; i++) {
                        byte[] buffer = SinkBufferPool.acquire();
                        for (int j = 0; j < buffer.length; j++) {
                            buffer[j] = pattern;
                        }
                        Thread.yield();
                        for (byte b : buffer) {
                            if (b != pattern) {
                                shared.set(true);
                                break;
                            }
                        }
                        SinkBufferPool.release(buffer);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
        assertThat(shared).isFalse();
    }

    private static List<byte[]> drain() {
        List<byte[]> held = new ArrayList<>();
        for (int i = 0; i < DRAIN; i++) {
            held.add(SinkBufferPool.acquire());
        }
        return held;
    }
}
