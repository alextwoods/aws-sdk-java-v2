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

package software.amazon.awssdk.core;

import java.util.Objects;

/**
 * Base of every modeled service response, source compatible with the V2 {@code SdkResponse} for the type, its
 * {@link Builder} and {@link BuilderImpl}, and {@link #toBuilder()}.
 *
 * <p>Every response carries the {@link SdkResponseMetadata} of the call that produced it (Requirement 21.1): its
 * request IDs, HTTP status, attempt count and transport facts, each exactly as supplied and otherwise absent
 * (Requirement 21.13). A response that was built rather than received carries {@link SdkResponseMetadata#empty()}.
 * The metadata is not a modeled member: it takes no part in equality, hashing or {@code toString}.
 *
 * <p>The V2 {@code sdkHttpResponse()} view is declared by {@code software.amazon.awssdk.awscore.AwsResponse}, the
 * superclass of every generated response, because this module cannot depend on the HTTP SPI that defines
 * {@code SdkHttpResponse}.
 */
public abstract class SdkResponse {
    private final SdkResponseMetadata sdkResponseMetadata;

    protected SdkResponse(Builder builder) {
        SdkResponseMetadata metadata = builder.sdkResponseMetadata();
        this.sdkResponseMetadata = metadata == null ? SdkResponseMetadata.empty() : metadata;
    }

    /** The metadata of the response that produced this value; {@link SdkResponseMetadata#empty()} if none. Never null. */
    public SdkResponseMetadata sdkResponseMetadata() {
        return sdkResponseMetadata;
    }

    /** A builder initialized with this response's members and metadata. */
    public abstract Builder toBuilder();

    /** Builder of an {@link SdkResponse}. */
    public interface Builder {
        /** Sets the response metadata; {@code null} means none. */
        Builder sdkResponseMetadata(SdkResponseMetadata sdkResponseMetadata);

        /** The response metadata set so far, or {@code null}. */
        SdkResponseMetadata sdkResponseMetadata();

        SdkResponse build();
    }

    /** Base implementation of {@link Builder}, holding the response metadata. */
    protected abstract static class BuilderImpl implements Builder {
        private SdkResponseMetadata sdkResponseMetadata;

        protected BuilderImpl() {
        }

        protected BuilderImpl(SdkResponse response) {
            this.sdkResponseMetadata = Objects.requireNonNull(response, "response").sdkResponseMetadata;
        }

        @Override
        public Builder sdkResponseMetadata(SdkResponseMetadata sdkResponseMetadata) {
            this.sdkResponseMetadata = sdkResponseMetadata;
            return this;
        }

        @Override
        public SdkResponseMetadata sdkResponseMetadata() {
            return sdkResponseMetadata;
        }
    }
}
