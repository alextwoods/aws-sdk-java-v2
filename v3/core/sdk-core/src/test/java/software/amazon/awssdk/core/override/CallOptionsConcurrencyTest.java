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

package software.amazon.awssdk.core.override;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static software.amazon.awssdk.core.override.CallOptionsTest.CLIENT_INTERCEPTOR;
import static software.amazon.awssdk.core.override.CallOptionsTest.INTERCEPTORS;
import static software.amazon.awssdk.core.override.CallOptionsTest.INTERCEPTORS_CALL;
import static software.amazon.awssdk.core.override.CallOptionsTest.RETRY_CALL;
import static software.amazon.awssdk.core.override.CallOptionsTest.RETRY_STRATEGY;
import static software.amazon.awssdk.core.override.CallOptionsTest.TRACE;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequestOverrideConfiguration;
import software.amazon.awssdk.core.client.config.ClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.config.TimeoutConfiguration;
import software.amazon.awssdk.core.override.CallOptionsTest.Interceptor;
import software.amazon.awssdk.core.override.CallOptionsTest.Retry;

/**
 * Simultaneous calls on one client each observe exactly the client configuration plus their own deltas (Requirement
 * 9.4), and neither the client configuration nor other calls change (design "Async and concurrency testing": at least
 * two simultaneous calls, more operations than simultaneous calls, a barrier proving overlap, a bounded timeout).
 */
class CallOptionsConcurrencyTest {
    private static final int SIMULTANEOUS_CALLS = 4;
    private static final int ROUNDS = 16;
    private static final long TIMEOUT_SECONDS = 30;

    @Test
    void simultaneousCallsSeeOnlyTheirOwnDeltas() throws Exception {
        SdkClientConfiguration base = CallOptionsTest.base();
        CallOptions client = CallOptions.of(base);
        Map<ClientOption<?>, Object> baseSnapshot = snapshot(base);
        CyclicBarrier viewsBuilt = new CyclicBarrier(SIMULTANEOUS_CALLS);
        CyclicBarrier viewsChecked = new CyclicBarrier(SIMULTANEOUS_CALLS);
        AtomicInteger live = new AtomicInteger();
        AtomicInteger operations = new AtomicInteger();

        ExecutorService executor = Executors.newFixedThreadPool(SIMULTANEOUS_CALLS);
        try {
            List<Callable<Void>> workers = new ArrayList<>();
            for (int worker = 0; worker < SIMULTANEOUS_CALLS; worker++) {
                int workerId = worker;
                workers.add(() -> {
                    for (int round = 0; round < ROUNDS; round++) {
                        int callId = workerId * ROUNDS + round;
                        RequestOverrideConfiguration embedded = embedded(callId);
                        RequestOverrideConfiguration explicit = explicit(callId);
                        CallOptions call = client.withOverrides(embedded, explicit);

                        live.incrementAndGet();
                        viewsBuilt.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                        // Every call of this round holds its view now: the calls overlap.
                        assertEquals(SIMULTANEOUS_CALLS, live.get());
                        assertMatchesOracle(base, callId, embedded, explicit, call);
                        viewsChecked.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                        live.decrementAndGet();
                        operations.incrementAndGet();
                    }
                    return null;
                });
            }
            List<Future<Void>> results = executor.invokeAll(workers, TIMEOUT_SECONDS, TimeUnit.SECONDS);
            for (Future<Void> result : results) {
                result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
            assertEquals(true, executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        }

        assertEquals(SIMULTANEOUS_CALLS * ROUNDS, operations.get());
        assertEquals(baseSnapshot, snapshot(base));
        assertEquals(CallOptionsTest.base(), base);
        assertFalse(client.hasOverrides());
        assertSame(base, client.clientConfiguration());
    }

    /** Calls whose id is a multiple of 5 carry no override and must see the unchanged client configuration. */
    private static boolean overridden(int callId) {
        return callId % 5 != 0;
    }

    private static RequestOverrideConfiguration embedded(int callId) {
        if (!overridden(callId)) {
            return null;
        }
        return SdkRequestOverrideConfiguration.builder()
                                              .putHeader("x-call", Integer.toString(callId))
                                              .apiCallAttemptTimeout(Duration.ofMillis(callId + 1))
                                              .build();
    }

    private static RequestOverrideConfiguration explicit(int callId) {
        if (!overridden(callId)) {
            return null;
        }
        SdkRequestOverrideConfiguration.Builder builder = SdkRequestOverrideConfiguration.builder()
                                                                                         .putExecutionAttribute(
                                                                                             TRACE, "call-" + callId)
                                                                                         .putRawQueryParameter(
                                                                                             "call", "" + callId);
        if (callId % 2 == 0) {
            builder.putCallOption(RETRY_CALL, new Retry(callId));
        } else {
            builder.addCallOptionElement(INTERCEPTORS_CALL, new Interceptor("call-" + callId));
        }
        return builder.build();
    }

    /** Compares the view with an oracle computed independently of {@link CallOptions}: base values plus this call's delta. */
    private static void assertMatchesOracle(SdkClientConfiguration base, int callId, RequestOverrideConfiguration embedded,
                                            RequestOverrideConfiguration explicit, CallOptions call) {
        Map<ClientOption<?>, Object> expected = snapshot(base);
        Set<ClientOption<?>> expectedOverridden = new HashSet<>();
        if (overridden(callId)) {
            expected.put(SdkClientOption.TIMEOUT_CONFIGURATION,
                         TimeoutConfiguration.builder()
                                             .apiCallTimeout(Duration.ofSeconds(30))
                                             .apiCallAttemptTimeout(Duration.ofMillis(callId + 1))
                                             .build());
            expectedOverridden.add(SdkClientOption.TIMEOUT_CONFIGURATION);
            if (callId % 2 == 0) {
                expected.put(RETRY_STRATEGY, new Retry(callId));
                expectedOverridden.add(RETRY_STRATEGY);
            } else {
                expected.put(INTERCEPTORS, List.of(CLIENT_INTERCEPTOR, new Interceptor("call-" + callId)));
                expectedOverridden.add(INTERCEPTORS);
            }
        }
        for (Map.Entry<ClientOption<?>, Object> entry : expected.entrySet()) {
            assertEquals(Optional.of(entry.getValue()), call.get(entry.getKey()), entry.getKey().name() + " " + callId);
        }
        assertEquals(expectedOverridden, call.overriddenOptions().keySet());
        assertEquals(overridden(callId) ? Map.of("x-call", List.of(Integer.toString(callId))) : Map.of(),
                     call.headers());
        assertEquals(overridden(callId) ? Map.of("call", List.of("" + callId)) : Map.of(), call.rawQueryParameters());
        assertEquals(overridden(callId) ? Optional.of("call-" + callId) : Optional.empty(),
                     call.executionAttribute(TRACE));
        assertSame(base, call.clientConfiguration());
        if (overridden(callId)) {
            // The overrides themselves are unchanged by merging.
            assertEquals(embedded, embedded(callId));
            assertEquals(explicit, explicit(callId));
        }
    }

    private static Map<ClientOption<?>, Object> snapshot(SdkClientConfiguration configuration) {
        Map<ClientOption<?>, Object> values = new LinkedHashMap<>();
        for (ClientOption<?> option : configuration.options()) {
            values.put(option, configuration.get(option).orElseThrow());
        }
        return values;
    }
}
