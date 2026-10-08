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

package software.amazon.awssdk.test.protocoltests.support;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.function.Executable;
import org.opentest4j.AssertionFailedError;

/** Shared harness utilities. */
public final class Conformance {
    /**
     * The bound of one case: a case that does not finish within it fails (Requirement 19.14). The slowest case is a
     * single client construction and call over an in-memory transport, which takes milliseconds.
     */
    public static final Duration CASE_TIMEOUT = Duration.ofSeconds(30);

    private Conformance() {
    }

    /** The value of a system property the build sets; fails, rather than skips, when it is missing. */
    public static String requiredProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("System property " + name + " is not set; run the suite through Maven.");
        }
        return value;
    }

    /** The outcome of running one case body. */
    public sealed interface Outcome {
        /** The case passed. */
        record Passed() implements Outcome {
        }

        /** The case failed with {@code failure}. */
        record Failed(Throwable failure) implements Outcome {
        }

        /** The case did not finish within {@link #CASE_TIMEOUT}. */
        record TimedOut() implements Outcome {
        }
    }

    /**
     * Runs {@code body} on a dedicated daemon thread, bounded by {@link #CASE_TIMEOUT}; a timed-out body is
     * interrupted and abandoned.
     */
    public static Outcome run(Executable body) {
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "protocol-conformance-case");
            thread.setDaemon(true);
            return thread;
        });
        try {
            Future<?> future = executor.submit(() -> {
                try {
                    body.execute();
                } catch (Exception | Error e) {
                    throw e;
                } catch (Throwable t) {
                    throw new IllegalStateException(t);
                }
                return null;
            });
            try {
                future.get(CASE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                return new Outcome.Passed();
            } catch (TimeoutException e) {
                future.cancel(true);
                return new Outcome.TimedOut();
            } catch (ExecutionException e) {
                return new Outcome.Failed(e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new Outcome.Failed(e);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    /** A JUnit failure for {@code outcome}, with {@code context} (the case key) in the message. */
    public static AssertionFailedError failure(String context, Outcome outcome) {
        if (outcome instanceof Outcome.TimedOut) {
            return new AssertionFailedError(context + ": did not finish within " + CASE_TIMEOUT);
        }
        Throwable cause = ((Outcome.Failed) outcome).failure();
        return new AssertionFailedError(context + ": " + describe(cause), cause);
    }

    /** One line naming a failure and its causes. */
    public static String describe(Throwable failure) {
        StringBuilder out = new StringBuilder();
        Throwable current = failure;
        for (int depth = 0; current != null && depth < 6; depth++) {
            if (depth > 0) {
                out.append(" <- ");
            }
            out.append(current instanceof AssertionError ? "" : current.getClass().getSimpleName() + ": ")
               .append(current.getMessage());
            current = current.getCause() == current ? null : current.getCause();
        }
        return out.toString();
    }
}
