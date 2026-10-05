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
package software.amazon.awssdk.enhanced.dynamodb.internal.util;

import java.util.function.Supplier;

/**
 * A minimal lazily-formatting logger over {@link System.Logger}. V3 core ships no logging facade and the dependency
 * policy forbids logging implementations, so the JDK platform logger (which applications can route to any backend
 * through {@code System.LoggerFinder}) carries the V2 enhanced client's debug output under the same logger names.
 */
public final class Logger {
    private final System.Logger delegate;

    private Logger(System.Logger delegate) {
        this.delegate = delegate;
    }

    public static Logger loggerFor(Class<?> type) {
        return loggerFor(type.getName());
    }

    public static Logger loggerFor(String name) {
        return new Logger(System.getLogger(name));
    }

    public void debug(Supplier<String> message) {
        if (delegate.isLoggable(System.Logger.Level.DEBUG)) {
            delegate.log(System.Logger.Level.DEBUG, message.get());
        }
    }

    public void warn(Supplier<String> message) {
        if (delegate.isLoggable(System.Logger.Level.WARNING)) {
            delegate.log(System.Logger.Level.WARNING, message.get());
        }
    }

    public boolean isDebugEnabled() {
        return delegate.isLoggable(System.Logger.Level.DEBUG);
    }
}
