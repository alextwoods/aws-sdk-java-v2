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

package software.amazon.awssdk.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.CancellationReason;

class CancellationScopeTest {

    @Test
    void cancel_runsEveryHandleOnceMostRecentFirst() {
        CancellationScope scope = CancellationScope.root();
        List<String> events = new ArrayList<>();
        scope.register(reason -> events.add("exchange:" + reason));
        scope.register(reason -> events.add("body:" + reason));

        assertThat(scope.cancel(CancellationReason.API_CALL_TIMEOUT)).isTrue();
        assertThat(scope.cancel(CancellationReason.CALLER_CANCELLED)).isFalse();
        assertThat(scope.close()).isFalse();

        assertThat(events).containsExactly("body:API_CALL_TIMEOUT", "exchange:API_CALL_TIMEOUT");
        assertThat(scope.state()).isEqualTo(CancellationScope.State.CANCELLED);
        assertThat(scope.reason()).contains(CancellationReason.API_CALL_TIMEOUT);
    }

    @Test
    void releasedHandles_areNotRun() {
        CancellationScope scope = CancellationScope.root();
        AtomicInteger runs = new AtomicInteger();
        CancellationScope.Registration registration = scope.register(reason -> runs.incrementAndGet());

        assertThat(registration.release()).isTrue();
        assertThat(registration.release()).isFalse();
        scope.cancel(CancellationReason.CALLER_CANCELLED);

        assertThat(runs).hasValue(0);
    }

    @Test
    void lateRegistration_isCancelledAtOnce() {
        CancellationScope cancelled = CancellationScope.root();
        cancelled.cancel(CancellationReason.CALLER_CANCELLED);
        CancellationScope closed = CancellationScope.root();
        closed.close();
        List<CancellationReason> reasons = new ArrayList<>();

        assertThat(cancelled.register(reasons::add).release()).isFalse();
        closed.register(reasons::add);

        assertThat(reasons).containsExactly(CancellationReason.CALLER_CANCELLED, CancellationReason.RESPONSE_ABANDONED);
    }

    @Test
    void close_releasesHandlesWithoutRunningThem() {
        CancellationScope scope = CancellationScope.root();
        AtomicInteger runs = new AtomicInteger();
        scope.register(reason -> runs.incrementAndGet());

        assertThat(scope.close()).isTrue();
        assertThat(scope.cancel(CancellationReason.CALLER_CANCELLED)).isFalse();

        assertThat(runs).hasValue(0);
        assertThat(scope.state()).isEqualTo(CancellationScope.State.CLOSED);
        assertThat(scope.reason()).isEmpty();
    }

    @Test
    void children_areCancelledWithTheParentAndDetachWhenTheyEnd() {
        CancellationScope call = CancellationScope.root();
        CancellationScope finished = call.child();
        CancellationScope timedOut = call.child();
        CancellationScope running = call.child();
        AtomicInteger finishedRuns = new AtomicInteger();
        List<CancellationReason> runningReasons = new ArrayList<>();
        finished.register(reason -> finishedRuns.incrementAndGet());
        running.register(runningReasons::add);
        assertThat(call.registeredHandles()).isEqualTo(3);

        finished.close();
        timedOut.cancel(CancellationReason.API_CALL_ATTEMPT_TIMEOUT);
        assertThat(call.isOpen()).isTrue();
        assertThat(call.registeredHandles()).isEqualTo(1);

        call.cancel(CancellationReason.CALLER_CANCELLED);
        assertThat(running.isCancelled()).isTrue();
        assertThat(runningReasons).containsExactly(CancellationReason.CALLER_CANCELLED);
        assertThat(finishedRuns).hasValue(0);
        assertThat(timedOut.reason()).contains(CancellationReason.API_CALL_ATTEMPT_TIMEOUT);
        assertThat(call.child().isCancelled()).isTrue();
    }

    @Test
    void failingHandle_doesNotStopTheOthers() {
        CancellationScope scope = CancellationScope.root();
        AtomicInteger runs = new AtomicInteger();
        IllegalStateException first = new IllegalStateException("first");
        IllegalStateException second = new IllegalStateException("second");
        scope.register(reason -> runs.incrementAndGet());
        scope.register(reason -> {
            throw second;
        });
        scope.register(reason -> {
            throw first;
        });

        assertThatThrownBy(() -> scope.cancel(CancellationReason.CALLER_CANCELLED))
            .isSameAs(first)
            .satisfies(e -> assertThat(e.getSuppressed()).containsExactly(second));
        assertThat(runs).hasValue(1);
        assertThat(scope.isCancelled()).isTrue();
    }

    @Test
    void concurrentCancellation_runsEachHandleExactlyOnce() throws Exception {
        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            for (int round = 0; round < 200; round++) {
                CancellationScope scope = CancellationScope.root();
                CancellationScope attempt = scope.child();
                AtomicInteger runs = new AtomicInteger();
                scope.register(reason -> runs.incrementAndGet());
                attempt.register(reason -> runs.incrementAndGet());
                CyclicBarrier start = new CyclicBarrier(threads);
                List<Future<Boolean>> results = new ArrayList<>();
                for (int t = 0; t < threads; t++) {
                    int thread = t;
                    results.add(executor.submit(() -> {
                        start.await();
                        return thread % 2 == 0 ? scope.cancel(CancellationReason.CALLER_CANCELLED) : scope.close();
                    }));
                }
                int winners = 0;
                for (Future<Boolean> result : results) {
                    winners += result.get() ? 1 : 0;
                }
                assertThat(winners).isEqualTo(1);
                assertThat(runs.get()).isEqualTo(scope.isCancelled() ? 2 : 0);
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
