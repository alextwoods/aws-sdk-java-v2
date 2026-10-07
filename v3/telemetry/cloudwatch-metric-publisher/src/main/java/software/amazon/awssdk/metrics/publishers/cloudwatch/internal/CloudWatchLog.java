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

package software.amazon.awssdk.metrics.publishers.cloudwatch.internal;

import java.util.function.Supplier;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.WarningSuppression;

/**
 * The logging of one publisher, through the SDK logging facade under V2's logger name
 * {@value #LOGGER_NAME}: a background failure is logged at {@code WARN} once per category within the publisher's own
 * {@link WarningSuppression} scope, then at {@code DEBUG} (Requirement 21.12). Not a public API.
 */
public final class CloudWatchLog {
    /** V2's {@code CloudWatchMetricLogger.METRIC_LOGGER} name. */
    public static final String LOGGER_NAME = "software.amazon.awssdk.metrics.publishers.cloudwatch";
    private static final Logger LOGGER = Logger.loggerFor(LOGGER_NAME);

    private final WarningSuppression warnings = WarningSuppression.create();

    /** Logs {@code message} at {@code WARN} the first time {@code category} occurs in this scope, else at DEBUG. */
    public void warnOnce(String category, Supplier<String> message, Throwable cause) {
        if (warnings.warn(LOGGER, category, null, message, cause)) {
            return;
        }
        if (cause == null) {
            LOGGER.debug(message);
        } else {
            LOGGER.debug(message, cause);
        }
    }

    /** Logs {@code message} at {@code DEBUG}. */
    public void debug(Supplier<String> message) {
        LOGGER.debug(message);
    }
}
