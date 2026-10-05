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
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Per-exchange options the pipeline passes to {@link SdkSyncHttpClient#prepare} and {@link SdkAsyncHttpClient#execute}:
 * overrides of the transport's configured timeouts for one attempt (for example the remaining attempt budget). An
 * absent value means the transport's configured value. A transport that cannot honor a present value per exchange
 * does not declare the matching {@link HttpClientOption} in its {@link HttpClientCapabilities}, and the pipeline does
 * not set it. Instances are immutable.
 */
public final class HttpExecutionOptions implements ToCopyableBuilder<HttpExecutionOptions.Builder, HttpExecutionOptions> {
    private static final HttpExecutionOptions DEFAULTS = new Builder().build();

    private final Duration connectionAcquisitionTimeout;
    private final Duration readTimeout;
    private final Duration writeTimeout;

    private HttpExecutionOptions(Builder builder) {
        this.connectionAcquisitionTimeout = builder.connectionAcquisitionTimeout;
        this.readTimeout = builder.readTimeout;
        this.writeTimeout = builder.writeTimeout;
    }

    /** No overrides. */
    public static HttpExecutionOptions defaults() {
        return DEFAULTS;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Override of {@link HttpClientOption#CONNECTION_ACQUISITION_TIMEOUT}. */
    public Optional<Duration> connectionAcquisitionTimeout() {
        return Optional.ofNullable(connectionAcquisitionTimeout);
    }

    /** Override of {@link HttpClientOption#READ_TIMEOUT}. */
    public Optional<Duration> readTimeout() {
        return Optional.ofNullable(readTimeout);
    }

    /** Override of {@link HttpClientOption#WRITE_TIMEOUT}. */
    public Optional<Duration> writeTimeout() {
        return Optional.ofNullable(writeTimeout);
    }

    @Override
    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.connectionAcquisitionTimeout = connectionAcquisitionTimeout;
        builder.readTimeout = readTimeout;
        builder.writeTimeout = writeTimeout;
        return builder;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof HttpExecutionOptions other
                            && Objects.equals(connectionAcquisitionTimeout, other.connectionAcquisitionTimeout)
                            && Objects.equals(readTimeout, other.readTimeout)
                            && Objects.equals(writeTimeout, other.writeTimeout);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectionAcquisitionTimeout, readTimeout, writeTimeout);
    }

    @Override
    public String toString() {
        return "HttpExecutionOptions[connectionAcquisitionTimeout=" + connectionAcquisitionTimeout + ", readTimeout="
               + readTimeout + ", writeTimeout=" + writeTimeout + "]";
    }

    /** Mutable builder of {@link HttpExecutionOptions}; not thread safe. Durations must be positive or null. */
    public static final class Builder implements CopyableBuilder<Builder, HttpExecutionOptions> {
        private Duration connectionAcquisitionTimeout;
        private Duration readTimeout;
        private Duration writeTimeout;

        private Builder() {
        }

        public Builder connectionAcquisitionTimeout(Duration timeout) {
            this.connectionAcquisitionTimeout = positive(timeout, "connectionAcquisitionTimeout");
            return this;
        }

        public Builder readTimeout(Duration timeout) {
            this.readTimeout = positive(timeout, "readTimeout");
            return this;
        }

        public Builder writeTimeout(Duration timeout) {
            this.writeTimeout = positive(timeout, "writeTimeout");
            return this;
        }

        @Override
        public HttpExecutionOptions build() {
            return new HttpExecutionOptions(this);
        }

        private static Duration positive(Duration timeout, String name) {
            if (timeout != null && (timeout.isNegative() || timeout.isZero())) {
                throw new IllegalArgumentException(name + " is not positive: " + timeout);
            }
            return timeout;
        }
    }
}
