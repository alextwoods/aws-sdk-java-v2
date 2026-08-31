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

package software.amazon.awssdk.core.internal.http;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.pipeline.stages.ApplyTransactionIdStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.ApplyUserAgentStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.AuthSchemeResolutionStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.CompressRequestStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.EndpointResolutionStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.HttpChecksumStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.MakeRequestImmutableStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.MakeRequestMutableStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.MergeCustomHeadersStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.MergeCustomQueryParamsStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.QueryParametersToBodyStage;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * The request-mutation sequence — the eleven stages, run once per request, that take the marshalled
 * request through transaction id, header/query merging, compression, auth scheme and endpoint
 * resolution, checksums and user agent, ending back at an immutable request.
 *
 * <p>This sequence is byte-for-byte the same for the sync and async pipelines (only the
 * {@link ClientType} passed to the checksum stage differs), which is why it lives in one shared
 * class called from both straight-line pipelines rather than being written out twice.
 */
@SdkInternalApi
final class RequestMutationStages implements RequestPipeline<SdkHttpFullRequest, SdkHttpFullRequest> {

    private final MakeRequestMutableStage makeMutable = new MakeRequestMutableStage();
    private final ApplyTransactionIdStage applyTransactionId = new ApplyTransactionIdStage();
    private final MergeCustomHeadersStage mergeCustomHeaders;
    private final MergeCustomQueryParamsStage mergeCustomQueryParams = new MergeCustomQueryParamsStage();
    private final QueryParametersToBodyStage queryParamsToBody = new QueryParametersToBodyStage();
    private final CompressRequestStage compressRequest;
    private final AuthSchemeResolutionStage authSchemeResolution;
    private final EndpointResolutionStage endpointResolution;
    private final HttpChecksumStage httpChecksum;
    private final ApplyUserAgentStage applyUserAgent;
    private final MakeRequestImmutableStage makeImmutable = new MakeRequestImmutableStage();

    RequestMutationStages(HttpClientDependencies dependencies, ClientType clientType) {
        this.mergeCustomHeaders = new MergeCustomHeadersStage(dependencies);
        this.compressRequest = new CompressRequestStage(dependencies);
        this.authSchemeResolution = new AuthSchemeResolutionStage(dependencies);
        this.endpointResolution = new EndpointResolutionStage(dependencies);
        this.httpChecksum = new HttpChecksumStage(clientType);
        this.applyUserAgent = new ApplyUserAgentStage(dependencies);
    }

    @Override
    public SdkHttpFullRequest execute(SdkHttpFullRequest request, RequestExecutionContext context)
            throws Exception {
        SdkHttpFullRequest.Builder mutable = makeMutable.execute(request, context);
        mutable = applyTransactionId.execute(mutable, context);
        mutable = mergeCustomHeaders.execute(mutable, context);
        mutable = mergeCustomQueryParams.execute(mutable, context);
        mutable = queryParamsToBody.execute(mutable, context);
        mutable = compressRequest.execute(mutable, context);
        mutable = authSchemeResolution.execute(mutable, context);
        mutable = endpointResolution.execute(mutable, context);
        mutable = httpChecksum.execute(mutable, context);
        mutable = applyUserAgent.execute(mutable, context);
        return makeImmutable.execute(mutable, context);
    }
}
