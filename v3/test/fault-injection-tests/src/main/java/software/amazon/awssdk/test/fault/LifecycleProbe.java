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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import software.amazon.awssdk.pipeline.ExecutionInterceptor;
import software.amazon.awssdk.pipeline.HookContext;

/**
 * An interceptor that observes one call's lifecycle through the public hooks: how many attempts began and ended,
 * the monotonic time between one attempt's end and the next one's start (the applied retry delay of the sync driver,
 * which sleeps in real time), and the call's one terminal hook. A second {@code afterExecution} or
 * {@code modifyException}, or an attempt that ends without having begun, is recorded as a violation.
 *
 * <p>Use one probe per call. Thread-safe.
 */
public final class LifecycleProbe implements ExecutionInterceptor {
    private final Violations violations;
    private final AtomicInteger beforeExecution = new AtomicInteger();
    private final AtomicInteger beforeAttempt = new AtomicInteger();
    private final AtomicInteger afterAttempt = new AtomicInteger();
    private final AtomicInteger modifyException = new AtomicInteger();
    private final AtomicInteger afterExecution = new AtomicInteger();
    private final List<Long> attemptStarts = new CopyOnWriteArrayList<>();
    private final List<Long> attemptEnds = new CopyOnWriteArrayList<>();
    private final List<Integer> attemptNumbers = new CopyOnWriteArrayList<>();
    private final AtomicReference<Throwable> finalFailure = new AtomicReference<>();
    private final AtomicReference<Object> finalResponse = new AtomicReference<>();
    private final Set<String> hookThreads = ConcurrentHashMap.newKeySet();

    public LifecycleProbe(Violations violations) {
        this.violations = Objects.requireNonNull(violations, "violations");
    }

    @Override
    public void beforeExecution(HookContext.BeforeExecution context) {
        hookThreads.add(Thread.currentThread().getName());
        if (beforeExecution.incrementAndGet() > 1) {
            violations.record("lifecycle: beforeExecution ran " + beforeExecution.get() + " times");
        }
    }

    @Override
    public void beforeAttempt(HookContext.BeforeAttempt context) {
        hookThreads.add(Thread.currentThread().getName());
        beforeAttempt.incrementAndGet();
        attemptStarts.add(System.nanoTime());
        attemptNumbers.add(context.attempt().attemptNumber());
    }

    @Override
    public void afterAttempt(HookContext.AfterAttempt context) {
        hookThreads.add(Thread.currentThread().getName());
        attemptEnds.add(System.nanoTime());
        if (afterAttempt.incrementAndGet() > beforeAttempt.get()) {
            violations.record("lifecycle: afterAttempt ran more often than beforeAttempt");
        }
    }

    @Override
    public Throwable modifyException(HookContext.ModifyException context) {
        hookThreads.add(Thread.currentThread().getName());
        if (modifyException.incrementAndGet() > 1) {
            violations.record("lifecycle: modifyException ran " + modifyException.get() + " times");
        }
        return context.failure();
    }

    @Override
    public void afterExecution(HookContext.AfterExecution context) {
        hookThreads.add(Thread.currentThread().getName());
        if (afterExecution.incrementAndGet() > 1) {
            violations.record("lifecycle: afterExecution ran " + afterExecution.get() + " times");
        }
        context.failure().ifPresent(finalFailure::set);
        context.response().ifPresent(finalResponse::set);
        if (context.failure().isPresent() == context.response().isPresent()) {
            violations.record("lifecycle: afterExecution carried " + (context.failure().isPresent() ? "both" : "neither")
                              + " a response and a failure");
        }
    }

    /**
     * The names of the threads the observed hooks ran on: the async driver runs every stage on its completion
     * executor, never on a transport event loop.
     */
    public Set<String> hookThreads() {
        return Set.copyOf(hookThreads);
    }

    /** How many attempts began ({@code beforeAttempt}). */
    public int attempts() {
        return beforeAttempt.get();
    }

    /** How many attempts ended through {@code afterAttempt}. */
    public int endedAttempts() {
        return afterAttempt.get();
    }

    /** The attempt numbers the attempt contexts reported, in order: 1, 2, ... */
    public List<Integer> attemptNumbers() {
        return List.copyOf(attemptNumbers);
    }

    public int beforeExecutions() {
        return beforeExecution.get();
    }

    public int modifyExceptions() {
        return modifyException.get();
    }

    public int afterExecutions() {
        return afterExecution.get();
    }

    /** The failure {@code afterExecution} saw, if any. */
    public Optional<Throwable> finalFailure() {
        return Optional.ofNullable(finalFailure.get());
    }

    /** The response {@code afterExecution} saw, if any. */
    public Optional<Object> finalResponse() {
        return Optional.ofNullable(finalResponse.get());
    }

    /**
     * The real time between the end of each attempt that was followed by another and the start of that next attempt:
     * a lower bound check of the delay the driver applied.
     */
    public List<Duration> gapsBetweenAttempts() {
        List<Duration> gaps = new ArrayList<>();
        for (int i = 1; i < attemptStarts.size() && i - 1 < attemptEnds.size(); i++) {
            gaps.add(Duration.ofNanos(attemptStarts.get(i) - attemptEnds.get(i - 1)));
        }
        return gaps;
    }
}
