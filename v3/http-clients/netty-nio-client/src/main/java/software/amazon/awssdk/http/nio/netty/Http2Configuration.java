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

package software.amazon.awssdk.http.nio.netty;

import java.util.Objects;

/**
 * HTTP/2 settings (V2 {@code Http2Configuration}, configured by {@code http2Configuration}). Immutable.
 *
 * <ul>
 *   <li>{@link #maxStreams()}: the most concurrent streams the client opens on one connection (default
 *       {@link #DEFAULT_MAX_STREAMS}); the server's {@code SETTINGS_MAX_CONCURRENT_STREAMS} lowers it.</li>
 *   <li>{@link #initialWindowSize()}: the stream flow-control window the client advertises (default
 *       {@link #DEFAULT_INITIAL_WINDOW_SIZE} bytes). It is also the bound on response bytes buffered per HTTP/2
 *       exchange, because the window is only replenished as the body subscriber consumes data.</li>
 * </ul>
 */
public final class Http2Configuration {
    /** Default concurrent streams per connection (V2 default: effectively unbounded; V3 bounds it). */
    public static final int DEFAULT_MAX_STREAMS = 100;
    /** Default stream window, in bytes (V2 default 1 MiB). */
    public static final int DEFAULT_INITIAL_WINDOW_SIZE = 1024 * 1024;

    private final int maxStreams;
    private final int initialWindowSize;

    private Http2Configuration(Builder builder) {
        this.maxStreams = builder.maxStreams;
        this.initialWindowSize = builder.initialWindowSize;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The defaults. */
    public static Http2Configuration defaults() {
        return builder().build();
    }

    public int maxStreams() {
        return maxStreams;
    }

    public int initialWindowSize() {
        return initialWindowSize;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Http2Configuration other && maxStreams == other.maxStreams
                            && initialWindowSize == other.initialWindowSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxStreams, initialWindowSize);
    }

    @Override
    public String toString() {
        return "Http2Configuration[maxStreams=" + maxStreams + ", initialWindowSize=" + initialWindowSize + "]";
    }

    /** Mutable builder; not thread safe. */
    public static final class Builder {
        private int maxStreams = DEFAULT_MAX_STREAMS;
        private int initialWindowSize = DEFAULT_INITIAL_WINDOW_SIZE;

        private Builder() {
        }

        /** Concurrent streams per connection; positive. */
        public Builder maxStreams(int maxStreams) {
            if (maxStreams <= 0) {
                throw new IllegalArgumentException("maxStreams is not positive: " + maxStreams);
            }
            this.maxStreams = maxStreams;
            return this;
        }

        /** Stream window in bytes, 65535 to 2^31-1 (RFC 9113). */
        public Builder initialWindowSize(int initialWindowSize) {
            if (initialWindowSize < 65_535) {
                throw new IllegalArgumentException("initialWindowSize is below 65535: " + initialWindowSize);
            }
            this.initialWindowSize = initialWindowSize;
            return this;
        }

        public Http2Configuration build() {
            return new Http2Configuration(this);
        }
    }
}
