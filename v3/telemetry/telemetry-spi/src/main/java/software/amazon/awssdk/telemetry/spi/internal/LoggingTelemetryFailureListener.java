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

package software.amazon.awssdk.telemetry.spi.internal;

import java.util.Objects;
import software.amazon.awssdk.telemetry.spi.TelemetryFailure;
import software.amazon.awssdk.telemetry.spi.TelemetryFailureListener;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.WarningSuppression;

/**
 * {@link TelemetryFailureListener#logging()}: through the SDK logging facade, one {@code WARN} per publisher class and
 * callback within the listener's own {@link WarningSuppression} scope, {@code DEBUG} afterwards (Requirement 21.12).
 * The message names the publisher, callback and operation; the cause is the publisher's own exception, and the
 * records it failed on carry no sensitive data. Not a public API.
 */
public final class LoggingTelemetryFailureListener implements TelemetryFailureListener {
    /** The name of the default channel's logger. */
    public static final String LOGGER_NAME = "software.amazon.awssdk.telemetry";
    private static final String CATEGORY = "telemetry-callback-failure";

    private final Logger logger;
    private final WarningSuppression warnings = WarningSuppression.create();

    /** The default channel, logging through {@value #LOGGER_NAME}. */
    public LoggingTelemetryFailureListener() {
        this(Logger.loggerFor(LOGGER_NAME));
    }

    /** A channel logging through {@code logger}; for tests. */
    public LoggingTelemetryFailureListener(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public void telemetryFailed(TelemetryFailure failure) {
        String publisher = failure.publisher().getClass().getName();
        String category = CATEGORY + ":" + publisher + "#" + failure.callback();
        boolean warned = warnings.warn(logger, category, null, () -> message(failure, publisher, true),
                                       failure.cause());
        if (!warned) {
            logger.debug(() -> message(failure, publisher, false), failure.cause());
        }
    }

    private static String message(TelemetryFailure failure, String publisher, boolean first) {
        return "Telemetry publisher " + publisher + " failed in " + failure.callback() + " for " + failure.service()
               + "." + failure.operation() + "; the call's result is unchanged."
               + (first ? " Further failures of this publisher and callback are logged at DEBUG." : "");
    }
}
