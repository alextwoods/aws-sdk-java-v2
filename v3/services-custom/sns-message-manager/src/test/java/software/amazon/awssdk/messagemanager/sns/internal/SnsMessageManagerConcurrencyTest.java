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

package software.amazon.awssdk.messagemanager.sns.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.messagemanager.sns.SnsMessageManager;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessage;
import software.amazon.awssdk.regions.Region;

/**
 * Concurrency suite of one shared SNS message manager (task 18.7; Requirements 19.9, 19.10): {@value #SIMULTANEOUS}
 * simultaneous callers, {@value #TOTAL} total messages in {@value #ROUNDS} rounds, a bounded
 * {@value #TIMEOUT_SECONDS}-second run. A start barrier releases each round's callers together, and an overlap barrier
 * of {@value #SIMULTANEOUS} parties in the first read of each message stream holds every caller inside
 * {@code parseMessage} until all are there. Every other message is tampered after signing: each caller must get its
 * own message back, or a signature failure for its own tampered one, so no verification state leaks between callers.
 * The signing certificate is fetched in the first (cold) round, by at most one caller each (the retriever fetches
 * outside its cache lock), and served from the cache afterwards.
 */
class SnsMessageManagerConcurrencyTest {
    static final int SIMULTANEOUS = 8;
    static final int ROUNDS = 8;
    static final int TOTAL = SIMULTANEOUS * ROUNDS;
    static final int TIMEOUT_SECONDS = 60;

    private final FakeSyncHttpClient http = new FakeSyncHttpClient();
    private final ExecutorService callers = Executors.newFixedThreadPool(SIMULTANEOUS);

    @AfterEach
    void stopCallers() throws InterruptedException {
        callers.shutdownNow();
        assertThat(callers.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @Timeout(TIMEOUT_SECONDS)
    void sharedManager_overlappingParses_verifyEachCallersOwnMessage() throws Exception {
        http.serve(SnsTestFixtures.CERT_URL, SnsTestFixtures.certificate("sns-global"));
        CyclicBarrier start = new CyclicBarrier(SIMULTANEOUS);
        CyclicBarrier overlap = new CyclicBarrier(SIMULTANEOUS);
        AtomicInteger insideTogether = new AtomicInteger();
        List<Integer> fetchesAfterRound = new ArrayList<>();

        try (SnsMessageManager manager = SnsMessageManager.builder().region(Region.US_WEST_2).httpClient(http)
                                                          .build()) {
            Object[] outcomes = new Object[TOTAL];
            List<Future<?>> lanes = new ArrayList<>();
            for (int lane = 0; lane < SIMULTANEOUS; lane++) {
                int laneIndex = lane;
                lanes.add(callers.submit(() -> {
                    for (int round = 0; round < ROUNDS; round++) {
                        int index = round * SIMULTANEOUS + laneIndex;
                        start.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                        if (laneIndex == 0 && round > 0) {
                            synchronized (fetchesAfterRound) {
                                fetchesAfterRound.add(http.requests().size());
                            }
                        }
                        InputStream message = new OverlapStream(message(index), () -> {
                            if (overlap.await(TIMEOUT_SECONDS, TimeUnit.SECONDS) == 0) {
                                insideTogether.incrementAndGet();
                            }
                        });
                        try {
                            outcomes[index] = manager.parseMessage(message);
                        } catch (SdkClientException e) {
                            outcomes[index] = e;
                        }
                    }
                    return null;
                }));
            }
            for (Future<?> lane : lanes) {
                lane.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }

            for (int i = 0; i < TOTAL; i++) {
                if (tampered(i)) {
                    assertThat(outcomes[i]).as("message %d", i).isInstanceOf(SdkClientException.class);
                } else {
                    assertThat(outcomes[i]).as("message %d", i).isInstanceOf(SnsMessage.class);
                    assertThat(((SnsMessage) outcomes[i]).message()).isEqualTo("message-" + i);
                }
            }
        }
        assertThat(insideTogether.get()).as("rounds with every caller inside parseMessage together").isEqualTo(ROUNDS);
        int coldFetches = http.requests().size();
        assertThat(coldFetches).isBetween(1, SIMULTANEOUS);
        assertThat(fetchesAfterRound).as("the certificate is cached after the cold round").containsOnly(coldFetches);
    }

    private static boolean tampered(int index) {
        return index % 2 == 1;
    }

    private static String message(int index) {
        Map<String, String> fields = SnsTestFixtures.signed(
            SnsTestFixtures.notification("message-" + index, "subject-" + index, "2024-01-01T00:00:00.000Z"));
        if (tampered(index)) {
            fields.put("Message", "tampered-" + index);
        }
        return SnsTestFixtures.json(fields);
    }

    /** Waits at the overlap barrier on the first read, inside the manager that reads the stream. */
    private static final class OverlapStream extends InputStream {
        private final ByteArrayInputStream delegate;
        private final BarrierAction arrive;
        private boolean arrived;

        OverlapStream(String text, BarrierAction arrive) {
            this.delegate = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
            this.arrive = arrive;
        }

        @Override
        public int read() {
            arrive();
            return delegate.read();
        }

        @Override
        public int read(byte[] b, int off, int len) {
            arrive();
            return delegate.read(b, off, len);
        }

        private void arrive() {
            if (!arrived) {
                arrived = true;
                try {
                    arrive.run();
                } catch (Exception e) {
                    throw new IllegalStateException("not every caller reached parseMessage together", e);
                }
            }
        }
    }

    @FunctionalInterface
    private interface BarrierAction {
        void run() throws Exception;
    }
}
