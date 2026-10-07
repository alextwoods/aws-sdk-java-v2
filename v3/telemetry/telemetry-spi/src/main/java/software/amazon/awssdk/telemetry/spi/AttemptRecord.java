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

package software.amazon.awssdk.telemetry.spi;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * The terminal record of one attempt of a call (Requirements 21.3, 21.4): emitted exactly once, when the attempt
 * ends, before the next attempt starts and before its call's {@link CallRecord}.
 *
 * <p>It carries only metadata: no request, response, header, identity or body value, so it holds no sensitive data
 * (Requirement 21.10). The error type is the failure category, never the failure message.
 *
 * @param service the service ID of the client (for example {@code DynamoDB})
 * @param operation the operation name (for example {@code GetItem})
 * @param attemptNumber the one-based attempt number
 * @param outcome how the attempt ended
 * @param httpImplementation the client name of the HTTP implementation (for example {@code UrlConnection}), or
 *     {@code none} for a client without a transport (a presigner)
 * @param retryMode the retry mode of the client: {@code standard}, {@code adaptive}, or the strategy's class name
 *     for a custom strategy
 * @param duration the attempt's duration, from its start to its end
 * @param httpStatusCode the HTTP status code of the attempt's response; empty when no response was received
 * @param errorType the failure category of a failed attempt ({@code Throttling}, {@code ServerError},
 *     {@code ConfiguredTimeout}, {@code IO} or {@code Other}); empty otherwise
 */
public record AttemptRecord(String service, String operation, int attemptNumber, TelemetryOutcome outcome,
                            String httpImplementation, String retryMode, Duration duration, OptionalInt httpStatusCode,
                            Optional<String> errorType) {
    public AttemptRecord {
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(httpImplementation, "httpImplementation");
        Objects.requireNonNull(retryMode, "retryMode");
        Objects.requireNonNull(duration, "duration");
        Objects.requireNonNull(httpStatusCode, "httpStatusCode");
        Objects.requireNonNull(errorType, "errorType");
        if (attemptNumber < 1) {
            throw new IllegalArgumentException("attemptNumber must be at least 1, was " + attemptNumber);
        }
    }
}
