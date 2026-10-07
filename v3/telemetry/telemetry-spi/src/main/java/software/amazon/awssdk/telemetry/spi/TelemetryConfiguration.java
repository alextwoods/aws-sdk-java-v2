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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.metrics.MetricPublisher;

/**
 * The telemetry of one client (design "{@code TelemetryConfiguration telemetry()}"): its publishers, in delivery
 * order, and its telemetry failure channel. Immutable and thread safe. Stored in the client configuration under
 * {@link TelemetryOptions#TELEMETRY}.
 *
 * <p>Telemetry is opt-in (Requirement 15.7): a client without a configuration, or with one that has no publisher,
 * runs no telemetry stage, makes no callback and keeps no timer (Requirement 21.5). Publishers are caller-owned: the
 * client never closes them.
 */
public final class TelemetryConfiguration {
    private final List<TelemetryPublisher> publishers;
    private final TelemetryFailureListener failureListener;

    private TelemetryConfiguration(Builder builder) {
        this.publishers = List.copyOf(builder.publishers);
        this.failureListener = builder.failureListener != null ? builder.failureListener
                                                               : TelemetryFailureListener.logging();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The publishers, in the order they receive each record. Immutable. */
    public List<TelemetryPublisher> publishers() {
        return publishers;
    }

    /** Whether the configuration has no publisher, so it enables nothing. */
    public boolean isEmpty() {
        return publishers.isEmpty();
    }

    /** The telemetry failure channel. */
    public TelemetryFailureListener failureListener() {
        return failureListener;
    }

    /** A builder with this configuration's publishers and failure listener. */
    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.publishers.addAll(publishers);
        builder.failureListener = failureListener;
        return builder;
    }

    @Override
    public String toString() {
        List<String> names = new ArrayList<>();
        publishers.forEach(p -> names.add(p.getClass().getName()));
        return "TelemetryConfiguration(publishers=" + names + ")";
    }

    /** Builder of a {@link TelemetryConfiguration}. Not thread safe. */
    public static final class Builder {
        private final List<TelemetryPublisher> publishers = new ArrayList<>();
        private TelemetryFailureListener failureListener;

        private Builder() {
        }

        /** Appends a publisher of terminal call and attempt records. */
        public Builder addPublisher(TelemetryPublisher publisher) {
            publishers.add(Objects.requireNonNull(publisher, "publisher"));
            return this;
        }

        /**
         * Appends a V2-compatible metric publisher, which receives one {@code ApiCall} collection per call
         * ({@link TelemetryPublisher#fromMetricPublisher}).
         */
        public Builder addMetricPublisher(MetricPublisher publisher) {
            return addPublisher(TelemetryPublisher.fromMetricPublisher(publisher));
        }

        /** The telemetry failure channel; {@link TelemetryFailureListener#logging()} by default. */
        public Builder failureListener(TelemetryFailureListener failureListener) {
            this.failureListener = Objects.requireNonNull(failureListener, "failureListener");
            return this;
        }

        public TelemetryConfiguration build() {
            return new TelemetryConfiguration(this);
        }
    }
}
