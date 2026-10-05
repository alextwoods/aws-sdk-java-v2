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

package software.amazon.awssdk.http;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * What a transport supports, declared by {@link SdkSyncHttpClient#capabilities()} and
 * {@link SdkAsyncHttpClient#capabilities()} and validated against the configured options before a client is returned
 * (Requirements 12.1, 12.6, 12.8, 12.13).
 *
 * <ul>
 *   <li>{@link #mode()}: sync or async;</li>
 *   <li>{@link #protocols()}: the negotiable {@link Protocol} versions;</li>
 *   <li>{@link #options()}: the supported timeout, TLS, proxy, connection and protocol {@link HttpClientOption}s;</li>
 *   <li>{@link #maxBufferedBytesPerExchange()}: the documented bound on body bytes the transport buffers per exchange
 *       in each direction beyond the caller's demand (Requirements 12.5, 12.6);</li>
 *   <li>{@link #cancellationTimeout()}: the documented time within which a successful cancellation terminates
 *       pending acquisition, transmission and response consumption (Requirement 12.8).</li>
 * </ul>
 *
 * <p>Instances are immutable.
 */
public final class HttpClientCapabilities
    implements ToCopyableBuilder<HttpClientCapabilities.Builder, HttpClientCapabilities> {
    private final HttpTransportMode mode;
    private final Set<Protocol> protocols;
    private final Set<HttpClientOption> options;
    private final long maxBufferedBytesPerExchange;
    private final Duration cancellationTimeout;

    private HttpClientCapabilities(Builder builder) {
        this.mode = Objects.requireNonNull(builder.mode, "mode");
        if (builder.protocols.isEmpty()) {
            throw new IllegalArgumentException("A transport supports at least one protocol version");
        }
        this.protocols = Collections.unmodifiableSet(EnumSet.copyOf(builder.protocols));
        this.options = builder.options.isEmpty() ? Set.of()
                                                 : Collections.unmodifiableSet(EnumSet.copyOf(builder.options));
        if (builder.maxBufferedBytesPerExchange <= 0) {
            throw new IllegalArgumentException("The buffer bound is not positive: "
                                               + builder.maxBufferedBytesPerExchange);
        }
        this.maxBufferedBytesPerExchange = builder.maxBufferedBytesPerExchange;
        this.cancellationTimeout = Objects.requireNonNull(builder.cancellationTimeout, "cancellationTimeout");
        if (cancellationTimeout.isNegative() || cancellationTimeout.isZero()) {
            throw new IllegalArgumentException("The cancellation timeout is not positive: " + cancellationTimeout);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Sync or async. */
    public HttpTransportMode mode() {
        return mode;
    }

    /** The negotiable protocol versions, in declaration order of {@link Protocol}; never empty. */
    public Set<Protocol> protocols() {
        return protocols;
    }

    /** The supported options, in declaration order of {@link HttpClientOption}. */
    public Set<HttpClientOption> options() {
        return options;
    }

    /** True if {@code protocol} can be negotiated. */
    public boolean supports(Protocol protocol) {
        return protocols.contains(protocol);
    }

    /** True if {@code option} is supported. */
    public boolean supports(HttpClientOption option) {
        return options.contains(option);
    }

    /** The options of {@code configured} that are not supported, in declaration order of {@link HttpClientOption}. */
    public List<HttpClientOption> unsupported(Collection<HttpClientOption> configured) {
        List<HttpClientOption> out = new ArrayList<>();
        for (HttpClientOption option : HttpClientOption.values()) {
            if (configured.contains(option) && !options.contains(option)) {
                out.add(option);
            }
        }
        return List.copyOf(out);
    }

    /** The bound, in bytes, on body data buffered per exchange in each direction beyond the caller's demand. */
    public long maxBufferedBytesPerExchange() {
        return maxBufferedBytesPerExchange;
    }

    /** The time within which a successful cancellation terminates the exchange. */
    public Duration cancellationTimeout() {
        return cancellationTimeout;
    }

    @Override
    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.mode = mode;
        builder.protocols.addAll(protocols);
        builder.options.addAll(options);
        builder.maxBufferedBytesPerExchange = maxBufferedBytesPerExchange;
        builder.cancellationTimeout = cancellationTimeout;
        return builder;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof HttpClientCapabilities other && mode == other.mode
                            && protocols.equals(other.protocols) && options.equals(other.options)
                            && maxBufferedBytesPerExchange == other.maxBufferedBytesPerExchange
                            && cancellationTimeout.equals(other.cancellationTimeout);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, protocols, options, maxBufferedBytesPerExchange, cancellationTimeout);
    }

    @Override
    public String toString() {
        return "HttpClientCapabilities[" + mode + ", " + protocols + ", " + options + ", buffer "
               + maxBufferedBytesPerExchange + " bytes, cancellation " + cancellationTimeout + "]";
    }

    /**
     * Mutable builder of {@link HttpClientCapabilities}; not thread safe. {@code mode}, at least one protocol, a
     * positive buffer bound and a positive cancellation timeout are required.
     */
    public static final class Builder implements CopyableBuilder<Builder, HttpClientCapabilities> {
        private HttpTransportMode mode;
        private final Set<Protocol> protocols = EnumSet.noneOf(Protocol.class);
        private final Set<HttpClientOption> options = EnumSet.noneOf(HttpClientOption.class);
        private long maxBufferedBytesPerExchange;
        private Duration cancellationTimeout;

        private Builder() {
        }

        public Builder mode(HttpTransportMode mode) {
            this.mode = mode;
            return this;
        }

        /** Adds negotiable protocol versions. */
        public Builder addProtocol(Protocol... protocols) {
            for (Protocol protocol : protocols) {
                this.protocols.add(Objects.requireNonNull(protocol, "protocol"));
            }
            return this;
        }

        /** Adds supported options. */
        public Builder addOption(HttpClientOption... options) {
            for (HttpClientOption option : options) {
                this.options.add(Objects.requireNonNull(option, "option"));
            }
            return this;
        }

        /** Replaces the supported options. */
        public Builder options(Collection<HttpClientOption> options) {
            this.options.clear();
            options.forEach(this::addOption);
            return this;
        }

        public Builder maxBufferedBytesPerExchange(long maxBufferedBytesPerExchange) {
            this.maxBufferedBytesPerExchange = maxBufferedBytesPerExchange;
            return this;
        }

        public Builder cancellationTimeout(Duration cancellationTimeout) {
            this.cancellationTimeout = cancellationTimeout;
            return this;
        }

        @Override
        public HttpClientCapabilities build() {
            return new HttpClientCapabilities(this);
        }
    }
}
