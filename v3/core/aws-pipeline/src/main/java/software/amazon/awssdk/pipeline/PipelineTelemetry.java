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

package software.amazon.awssdk.pipeline;

import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.retries.AdaptiveRetryStrategy;
import software.amazon.awssdk.retries.RetryMode;
import software.amazon.awssdk.retries.RetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.telemetry.spi.TelemetryConfiguration;
import software.amazon.awssdk.telemetry.spi.TelemetryFailureListener;
import software.amazon.awssdk.telemetry.spi.TelemetryPublisher;

/**
 * The telemetry plan of a client, and of each of its operations (design "{@code TelemetryPlan}"; Requirements
 * 21.2-21.5): the configured publishers and failure channel, and the client-constant record dimensions (service,
 * HTTP implementation, retry mode, and per operation its name), resolved once.
 *
 * <p>It exists only when the client configures at least one publisher: then, and only then, a plan activates
 * {@link PipelineStage#TELEMETRY} and each call creates one {@link CallTelemetry}. A client without telemetry has no
 * instance, so its calls create no telemetry object, read no clock and make no callback (Requirement 21.5). Together
 * with {@link CallTelemetry}, this is the only pipeline code that references the Telemetry SPI or reads a clock.
 *
 * <p>Immutable and thread safe; holds no customer data.
 */
final class PipelineTelemetry {
    /** The HTTP implementation dimension of a client without a transport (a presigner). */
    static final String NO_HTTP_IMPLEMENTATION = "none";

    private final TelemetryPublisher[] publishers;
    private final TelemetryFailureListener failureListener;
    private final String service;
    private final String operation;
    private final String httpImplementation;
    private final String retryMode;

    private PipelineTelemetry(TelemetryPublisher[] publishers, TelemetryFailureListener failureListener,
                              String service, String operation, String httpImplementation, String retryMode) {
        this.publishers = publishers;
        this.failureListener = failureListener;
        this.service = service;
        this.operation = operation;
        this.httpImplementation = httpImplementation;
        this.retryMode = retryMode;
    }

    /**
     * The client-level plan of {@code configuration}, or {@code null} when it is absent or has no publisher, so that
     * telemetry stays inactive.
     */
    static PipelineTelemetry forClient(TelemetryConfiguration configuration, String service, String httpImplementation,
                                       RetryStrategy retryStrategy) {
        if (configuration == null || configuration.isEmpty()) {
            return null;
        }
        List<TelemetryPublisher> publishers = configuration.publishers();
        return new PipelineTelemetry(publishers.toArray(new TelemetryPublisher[0]), configuration.failureListener(),
                                     Objects.requireNonNull(service, "service"), null,
                                     Objects.requireNonNull(httpImplementation, "httpImplementation"),
                                     retryMode(retryStrategy));
    }

    /** The retry mode dimension of {@code strategy}: its {@link RetryMode} value, else its class name. */
    static String retryMode(RetryStrategy strategy) {
        if (strategy instanceof AdaptiveRetryStrategy) {
            return RetryMode.ADAPTIVE.value();
        }
        if (strategy instanceof StandardRetryStrategy) {
            return RetryMode.STANDARD.value();
        }
        return strategy.getClass().getName();
    }

    /** This client plan for the operation named {@code operation}; built once per {@link PipelinePlan}. */
    PipelineTelemetry forOperation(String operation) {
        return new PipelineTelemetry(publishers, failureListener, service, Objects.requireNonNull(operation),
                                     httpImplementation, retryMode);
    }

    /** The telemetry state of one new call of this operation, started now. */
    CallTelemetry newCall() {
        return new CallTelemetry(this, System.nanoTime());
    }

    TelemetryPublisher[] publishers() {
        return publishers;
    }

    TelemetryFailureListener failureListener() {
        return failureListener;
    }

    String service() {
        return service;
    }

    String operation() {
        return operation;
    }

    String httpImplementation() {
        return httpImplementation;
    }

    String retryMode() {
        return retryMode;
    }

    @Override
    public String toString() {
        return "PipelineTelemetry(" + service + (operation == null ? "" : "." + operation) + ", publishers="
               + publishers.length + ")";
    }
}
