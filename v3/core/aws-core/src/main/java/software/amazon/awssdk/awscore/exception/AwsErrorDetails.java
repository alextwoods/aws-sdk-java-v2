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

package software.amazon.awssdk.awscore.exception;

import java.io.Serializable;
import java.util.Objects;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.ToString;

/**
 * Details of an AWS service error response, source compatible with the V2 {@code AwsErrorDetails} for the error
 * message, error code, service name, raw response and {@link #sdkHttpResponse()} (the status code and headers of the
 * error response, without its body).
 *
 * <p>The HTTP response is transport metadata, not error content: it is not serialized (it is {@code null} after
 * deserialization) and takes no part in equality, hashing or {@code toString}.
 */
public final class AwsErrorDetails implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String errorMessage;
    private final String errorCode;
    private final String serviceName;
    private final SdkBytes rawResponse;
    private final transient SdkHttpResponse sdkHttpResponse;

    private AwsErrorDetails(Builder builder) {
        this.errorMessage = builder.errorMessage;
        this.errorCode = builder.errorCode;
        this.serviceName = builder.serviceName;
        this.rawResponse = builder.rawResponse;
        this.sdkHttpResponse = builder.sdkHttpResponse == null ? null : builder.sdkHttpResponse.withoutBody();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    /** The error message the service returned. */
    public String errorMessage() {
        return errorMessage;
    }

    /** The error code the service returned. */
    public String errorCode() {
        return errorCode;
    }

    /** The name of the service that returned the error. */
    public String serviceName() {
        return serviceName;
    }

    /** The raw response body, if it was retained. */
    public SdkBytes rawResponse() {
        return rawResponse;
    }

    /**
     * The status code and headers of the error response, without its body, or {@code null} if unknown (built
     * details, or details after deserialization).
     */
    public SdkHttpResponse sdkHttpResponse() {
        return sdkHttpResponse;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof AwsErrorDetails other
                            && Objects.equals(errorMessage, other.errorMessage)
                            && Objects.equals(errorCode, other.errorCode)
                            && Objects.equals(serviceName, other.serviceName)
                            && Objects.equals(rawResponse, other.rawResponse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(errorMessage, errorCode, serviceName, rawResponse);
    }

    /** Names and codes only: the raw response is not rendered. */
    @Override
    public String toString() {
        return ToString.builder("AwsErrorDetails").add("errorMessage", errorMessage).add("errorCode", errorCode)
                       .add("serviceName", serviceName).build();
    }

    /** Builder of {@link AwsErrorDetails}. */
    public static final class Builder {

        private String errorMessage;
        private String errorCode;
        private String serviceName;
        private SdkBytes rawResponse;
        private SdkHttpResponse sdkHttpResponse;

        private Builder() {
        }

        private Builder(AwsErrorDetails details) {
            this.errorMessage = details.errorMessage;
            this.errorCode = details.errorCode;
            this.serviceName = details.serviceName;
            this.rawResponse = details.rawResponse;
            this.sdkHttpResponse = details.sdkHttpResponse;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public String errorMessage() {
            return errorMessage;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public String errorCode() {
            return errorCode;
        }

        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public String serviceName() {
            return serviceName;
        }

        public Builder rawResponse(SdkBytes rawResponse) {
            this.rawResponse = rawResponse;
            return this;
        }

        public SdkBytes rawResponse() {
            return rawResponse;
        }

        /** Sets the error's HTTP response; only its status code and headers are retained. */
        public Builder sdkHttpResponse(SdkHttpResponse sdkHttpResponse) {
            this.sdkHttpResponse = sdkHttpResponse;
            return this;
        }

        public SdkHttpResponse sdkHttpResponse() {
            return sdkHttpResponse;
        }

        public AwsErrorDetails build() {
            return new AwsErrorDetails(this);
        }
    }
}
