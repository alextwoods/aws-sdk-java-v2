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

package software.amazon.awssdk.awscore;

import software.amazon.awssdk.awscore.internal.ResponseMetadataMap;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.SdkResponseMetadata;
import software.amazon.awssdk.http.HttpHeaders;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * Base of every generated AWS service response (through the generated {@code <Service>Response}), source compatible
 * with the V2 {@code AwsResponse} for {@link #responseMetadata()}, its {@link Builder} and {@link BuilderImpl}, and
 * with the V2 {@code SdkResponse#sdkHttpResponse()} view, which V3 declares here.
 *
 * <p>The AWS pipeline sets the metadata of a received response on its builder before the protocol builds it, so a
 * response carries, without a copy:
 * <ul>
 *   <li>{@link #sdkResponseMetadata()}: the V3 metadata API, with empty {@code Optional}s for absent values;</li>
 *   <li>{@link #sdkHttpResponse()}: the status code and headers, without the body (never retained);</li>
 *   <li>{@link #responseMetadata()}: the V2-compatible view, computed on access, with V2's {@code "UNKNOWN"} for
 *       absent values.</li>
 * </ul>
 * None of these is a modeled member: they take no part in equality, hashing or {@code toString}.
 */
public abstract class AwsResponse extends SdkResponse {
    private final SdkHttpResponse sdkHttpResponse;
    private final AwsResponseMetadata responseMetadata;

    protected AwsResponse(Builder builder) {
        super(builder);
        SdkHttpResponse http = builder.sdkHttpResponse();
        this.sdkHttpResponse = http == null ? null : http.withoutBody();
        // The explicitly set view only: a service builder's responseMetadata() wraps it, and never returns null.
        this.responseMetadata = builder instanceof BuilderImpl impl ? impl.responseMetadata : builder.responseMetadata();
    }

    /**
     * The status code and headers of the HTTP response that produced this value, without its body, or {@code null}
     * for a response that was built rather than received (as in V2).
     */
    public SdkHttpResponse sdkHttpResponse() {
        return sdkHttpResponse;
    }

    /**
     * The V2-compatible metadata view: the metadata set on the builder, or else the view of this response's request
     * ID and headers, whose absent values read as {@code "UNKNOWN"} (Requirement 21.13). Never null.
     */
    public AwsResponseMetadata responseMetadata() {
        if (responseMetadata != null) {
            return responseMetadata;
        }
        HttpHeaders headers = sdkHttpResponse == null ? HttpHeaders.empty() : sdkHttpResponse.headers();
        return DefaultAwsResponseMetadata.create(
            new ResponseMetadataMap(sdkResponseMetadata().requestId().orElse(null), headers));
    }

    @Override
    public abstract Builder toBuilder();

    /** Builder of an {@link AwsResponse}. */
    public interface Builder extends SdkResponse.Builder {
        /** The V2-compatible metadata view set so far, or {@code null} (service builders wrap it). */
        AwsResponseMetadata responseMetadata();

        /** Sets the V2-compatible metadata view, which then takes precedence over the response's own. */
        Builder responseMetadata(AwsResponseMetadata metadata);

        /** Sets the HTTP response; only its status code and headers are retained. */
        Builder sdkHttpResponse(SdkHttpResponse sdkHttpResponse);

        /** The HTTP response set so far, or {@code null}. */
        SdkHttpResponse sdkHttpResponse();

        @Override
        Builder sdkResponseMetadata(SdkResponseMetadata sdkResponseMetadata);

        @Override
        AwsResponse build();
    }

    /** Base implementation of {@link Builder}. */
    protected abstract static class BuilderImpl extends SdkResponse.BuilderImpl implements Builder {
        private AwsResponseMetadata responseMetadata;
        private SdkHttpResponse sdkHttpResponse;

        protected BuilderImpl() {
        }

        protected BuilderImpl(AwsResponse response) {
            super(response);
            this.responseMetadata = response.responseMetadata;
            this.sdkHttpResponse = response.sdkHttpResponse;
        }

        @Override
        public AwsResponseMetadata responseMetadata() {
            return responseMetadata;
        }

        @Override
        public Builder responseMetadata(AwsResponseMetadata responseMetadata) {
            this.responseMetadata = responseMetadata;
            return this;
        }

        @Override
        public Builder sdkHttpResponse(SdkHttpResponse sdkHttpResponse) {
            this.sdkHttpResponse = sdkHttpResponse;
            return this;
        }

        @Override
        public SdkHttpResponse sdkHttpResponse() {
            return sdkHttpResponse;
        }

        @Override
        public Builder sdkResponseMetadata(SdkResponseMetadata sdkResponseMetadata) {
            super.sdkResponseMetadata(sdkResponseMetadata);
            return this;
        }
    }
}
