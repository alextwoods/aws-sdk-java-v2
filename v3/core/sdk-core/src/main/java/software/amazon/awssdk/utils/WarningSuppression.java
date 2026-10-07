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

package software.amazon.awssdk.utils;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * A suppression scope of benign warnings (Requirement 21.12): a successful fallback, an ignored callback failure or an
 * ignored background cleanup failure is logged at {@code WARN} at most once per category and operation within one
 * scope. Use {@link #jvm()} for conditions that repeat for the whole process (for example a deprecated setting),
 * and {@link #create()} for one owned by a client, transfer or other component.
 *
 * <p>Only warnings that are emitted are recorded: while {@code WARN} is disabled nothing is recorded and no message is
 * built. Once a scope holds {@value #MAX_KEYS} distinct keys it suppresses further new keys rather than grow without
 * bound. Thread safe: concurrent equivalent warnings emit exactly one event.
 */
public final class WarningSuppression {
    /** The maximum number of distinct category and operation keys one scope records. */
    public static final int MAX_KEYS = 1024;

    private static final WarningSuppression JVM = new WarningSuppression("jvm");

    private final String name;
    private final Set<String> emitted = ConcurrentHashMap.newKeySet();

    private WarningSuppression(String name) {
        this.name = name;
    }

    /** The process-wide scope. */
    public static WarningSuppression jvm() {
        return JVM;
    }

    /** A new, empty scope; its owner keeps it for as long as the warnings should stay suppressed. */
    public static WarningSuppression create() {
        return new WarningSuppression("scope@" + Integer.toHexString(System.identityHashCode(new Object())));
    }

    /**
     * Logs {@code message} at {@code WARN} through {@code logger} unless an equivalent warning (same {@code category}
     * and {@code operation}) was already emitted in this scope.
     *
     * @param operation the operation the warning is about, or {@code null} when it is not operation specific
     * @return whether the warning was emitted
     */
    public boolean warn(Logger logger, String category, String operation, Supplier<String> message) {
        return warn(logger, category, operation, message, null);
    }

    /** As {@link #warn(Logger, String, String, Supplier)}, with the cause of the benign condition. */
    public boolean warn(Logger logger, String category, String operation, Supplier<String> message,
                        Throwable cause) {
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(message, "message");
        if (!logger.isWarnEnabled() || !claim(category, operation)) {
            return false;
        }
        if (cause == null) {
            logger.warn(message);
        } else {
            logger.warn(message, cause);
        }
        return true;
    }

    /** Whether an equivalent warning was emitted in this scope. */
    public boolean emitted(String category, String operation) {
        return emitted.contains(key(category, operation));
    }

    private boolean claim(String category, String operation) {
        if (emitted.size() >= MAX_KEYS) {
            return false;
        }
        return emitted.add(key(category, operation));
    }

    private static String key(String category, String operation) {
        return category + '\u0000' + (operation == null ? "" : operation);
    }

    @Override
    public String toString() {
        return "WarningSuppression(" + name + ", keys=" + emitted.size() + ")";
    }
}
