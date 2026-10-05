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

package software.amazon.awssdk.transfer.s3.internal.util;

import java.util.function.Supplier;

/**
 * Lazy logging over the JDK {@link System.Logger} facade, so the library declares no logging dependency (Requirement
 * 15.8); messages are built only when the level is enabled. Not a public API.
 */
public final class Log {
    private final System.Logger logger;

    private Log(System.Logger logger) {
        this.logger = logger;
    }

    /** A logger named after {@code type}. */
    public static Log loggerFor(Class<?> type) {
        return new Log(System.getLogger(type.getName()));
    }

    public void debug(Supplier<String> message) {
        log(System.Logger.Level.DEBUG, message, null);
    }

    public void info(Supplier<String> message) {
        log(System.Logger.Level.INFO, message, null);
    }

    public void warn(Supplier<String> message, Throwable cause) {
        log(System.Logger.Level.WARNING, message, cause);
    }

    private void log(System.Logger.Level level, Supplier<String> message, Throwable cause) {
        if (logger.isLoggable(level)) {
            if (cause == null) {
                logger.log(level, message.get());
            } else {
                logger.log(level, message.get(), cause);
            }
        }
    }
}
