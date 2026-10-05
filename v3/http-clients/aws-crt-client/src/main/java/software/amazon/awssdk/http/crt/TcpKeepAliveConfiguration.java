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

package software.amazon.awssdk.http.crt;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * TCP keep-alive of the CRT clients' sockets (V2 {@code TcpKeepAliveConfiguration}; the {@code tcpKeepAlive}
 * option): the idle time before the first probe ({@code keepAliveTimeout}), the interval between probes, and
 * optionally the number of failed probes before the connection is dropped. Immutable.
 */
public final class TcpKeepAliveConfiguration {
    private final Duration keepAliveInterval;
    private final Duration keepAliveTimeout;
    private final Integer keepAliveProbes;

    private TcpKeepAliveConfiguration(Builder builder) {
        this.keepAliveInterval = positive(builder.keepAliveInterval, "keepAliveInterval");
        this.keepAliveTimeout = positive(builder.keepAliveTimeout, "keepAliveTimeout");
        if (builder.keepAliveProbes != null && builder.keepAliveProbes < 1) {
            throw new IllegalArgumentException("keepAliveProbes is not positive: " + builder.keepAliveProbes);
        }
        this.keepAliveProbes = builder.keepAliveProbes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Duration keepAliveInterval() {
        return keepAliveInterval;
    }

    public Duration keepAliveTimeout() {
        return keepAliveTimeout;
    }

    public Optional<Integer> keepAliveProbes() {
        return Optional.ofNullable(keepAliveProbes);
    }

    @Override
    public String toString() {
        return "TcpKeepAliveConfiguration[interval=" + keepAliveInterval + ", timeout=" + keepAliveTimeout
               + ", probes=" + keepAliveProbes + "]";
    }

    private static Duration positive(Duration d, String name) {
        Objects.requireNonNull(d, name);
        if (d.isNegative() || d.isZero()) {
            throw new IllegalArgumentException(name + " is not positive: " + d);
        }
        return d;
    }

    /** Builder; {@code keepAliveInterval} and {@code keepAliveTimeout} are required. */
    public static final class Builder {
        private Duration keepAliveInterval;
        private Duration keepAliveTimeout;
        private Integer keepAliveProbes;

        private Builder() {
        }

        public Builder keepAliveInterval(Duration keepAliveInterval) {
            this.keepAliveInterval = keepAliveInterval;
            return this;
        }

        public Builder keepAliveTimeout(Duration keepAliveTimeout) {
            this.keepAliveTimeout = keepAliveTimeout;
            return this;
        }

        public Builder keepAliveProbes(Integer keepAliveProbes) {
            this.keepAliveProbes = keepAliveProbes;
            return this;
        }

        public TcpKeepAliveConfiguration build() {
            return new TcpKeepAliveConfiguration(this);
        }
    }
}
