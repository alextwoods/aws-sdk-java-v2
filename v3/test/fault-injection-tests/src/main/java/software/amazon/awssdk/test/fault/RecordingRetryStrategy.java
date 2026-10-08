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

package software.amazon.awssdk.test.fault;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import software.amazon.awssdk.retries.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.AttemptFailure;
import software.amazon.awssdk.retries.RetryDecision;
import software.amazon.awssdk.retries.RetryStrategy;
import software.amazon.awssdk.retries.RetryToken;

/**
 * A real retry strategy that records its token ledger (Requirement 19.8): each entry names the operation and the
 * attempt the token authorized. A retry decision records the delay the pipeline must apply before the next attempt.
 * Thread-safe.
 */
public final class RecordingRetryStrategy implements RetryStrategy {
    private final RetryStrategy delegate;
    private final List<Entry> ledger = new CopyOnWriteArrayList<>();

    public RecordingRetryStrategy(RetryStrategy delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /** What happened to one token. */
    public enum Kind {
        /** The initial token of attempt 1 was granted. */
        ACQUIRE,
        /** Attempt {@code attempt} failed retryably; the token of the next attempt was granted after {@code delay}. */
        RETRY,
        /** Attempt {@code attempt} failed and the strategy stopped. */
        STOP,
        /** Attempt {@code attempt} succeeded. */
        SUCCESS,
        /** Attempt {@code attempt}'s token was released without a refresh (a cancellation or terminal failure). */
        RELEASE
    }

    /** One ledger entry. {@code delay} is the decision's delay for {@link Kind#RETRY} and {@link Kind#STOP}. */
    public record Entry(Kind kind, int attempt, Duration delay, String detail) {
        @Override
        public String toString() {
            return kind + "#" + attempt + (delay == null || delay.isZero() ? "" : "(" + delay.toMillis() + "ms)")
                   + (detail == null ? "" : "[" + detail + "]");
        }
    }

    @Override
    public int maxAttempts() {
        return delegate.maxAttempts();
    }

    @Override
    public AcquireInitialTokenResponse acquireInitialToken(AcquireInitialTokenRequest request) {
        AcquireInitialTokenResponse response = delegate.acquireInitialToken(request);
        ledger.add(new Entry(Kind.ACQUIRE, response.token().attempt(), response.delay(), null));
        return response;
    }

    @Override
    public RetryDecision refreshRetryToken(RetryToken token, AttemptFailure failure) {
        RetryDecision decision = delegate.refreshRetryToken(token, failure);
        if (decision instanceof RetryDecision.Retry) {
            ledger.add(new Entry(Kind.RETRY, token.attempt(), decision.delay(), decision.classification().name()));
        } else {
            ledger.add(new Entry(Kind.STOP, token.attempt(), decision.delay(),
                                 ((RetryDecision.Stop) decision).reason().name()));
        }
        return decision;
    }

    @Override
    public RetryToken recordSuccess(RetryToken token) {
        RetryToken consumed = delegate.recordSuccess(token);
        ledger.add(new Entry(Kind.SUCCESS, token.attempt(), null, null));
        return consumed;
    }

    @Override
    public RetryToken recordFailure(RetryToken token) {
        RetryToken consumed = delegate.recordFailure(token);
        ledger.add(new Entry(Kind.RELEASE, token.attempt(), null, null));
        return consumed;
    }

    /** The ledger so far. */
    public List<Entry> ledger() {
        return List.copyOf(ledger);
    }

    /**
     * Waits until the ledger holds an entry of {@code kind}.
     *
     * @throws IllegalStateException if none is recorded within {@code timeout}
     */
    public Entry awaitEntry(Kind kind, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (true) {
            for (Entry entry : ledger) {
                if (entry.kind() == kind) {
                    return entry;
                }
            }
            if (System.nanoTime() - deadline > 0) {
                throw new IllegalStateException("No " + kind + " entry within " + timeout + ": " + ledger);
            }
            if (Thread.currentThread().isInterrupted()) {
                throw new IllegalStateException("Interrupted while waiting for a " + kind + " entry");
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
        }
    }

    /** The delays of every retry decision, in order: the delays the pipeline must apply. */
    public List<Duration> retryDelays() {
        return ledger.stream().filter(e -> e.kind() == Kind.RETRY).map(Entry::delay).toList();
    }

    /**
     * Problems with the ledger's token accounting: a granted token (attempt 1's, or the next attempt's of a retry)
     * consumed other than exactly once. Empty when every token was consumed exactly once.
     */
    public List<String> tokenProblems() {
        Set<Integer> granted = new HashSet<>();
        Map<Integer, Integer> consumed = new HashMap<>();
        for (Entry entry : ledger) {
            if (entry.kind() == Kind.ACQUIRE) {
                granted.add(entry.attempt());
                continue;
            }
            consumed.merge(entry.attempt(), 1, Integer::sum);
            if (entry.kind() == Kind.RETRY) {
                granted.add(entry.attempt() + 1);
            }
        }
        List<String> problems = new ArrayList<>();
        for (int attempt : granted) {
            int count = consumed.getOrDefault(attempt, 0);
            if (count != 1) {
                problems.add("the token of attempt " + attempt + " was consumed " + count + " times: " + ledger);
            }
        }
        for (int attempt : consumed.keySet()) {
            if (!granted.contains(attempt)) {
                problems.add("a token of attempt " + attempt + " was consumed but never granted: " + ledger);
            }
        }
        return problems;
    }
}
