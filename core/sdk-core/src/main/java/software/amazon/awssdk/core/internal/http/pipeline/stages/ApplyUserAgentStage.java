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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.BUSINESS_METADATA;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.SLASH;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.SPACE;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.appendSpaceAndField;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.MutableRequestToRequestPipeline;
import software.amazon.awssdk.core.internal.useragent.UserAgentHeaderCache;
import software.amazon.awssdk.core.useragent.AdditionalMetadata;
import software.amazon.awssdk.core.useragent.BusinessMetricCollection;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Apply any custom user agent supplied, otherwise instrument the user agent with info about the SDK and environment.
 *
 * <p>The header is built from a per-client constant prefix, per-call metadata (body and transformer types), the business
 * metrics collected along the pipeline, any request-level {@link ApiName}s and a constant suffix. Everything but the
 * request-level api names is the same from one call to the next for a given client configuration and operation shape, so
 * the assembled value is served from the client's {@link UserAgentHeaderCache} when the metrics and metadata match the
 * previous call.
 */
@SdkInternalApi
public class ApplyUserAgentStage implements MutableRequestToRequestPipeline {
    public static final String HEADER_USER_AGENT = UserAgentHeaderCache.HEADER_USER_AGENT;
    public static final String SDK_METRICS = "sdk-metrics";

    private static final Pair<List<ApiName>, Collection<String>> NO_API_NAMES =
        Pair.of(Collections.emptyList(), Collections.emptyList());

    private final UserAgentHeaderCache.Snapshot userAgent;

    public ApplyUserAgentStage(HttpClientDependencies dependencies) {
        // Constructed per call: the dependencies carry this call's client configuration, from which the constants come.
        this.userAgent = dependencies.userAgentHeaderCache().forConfiguration(dependencies.clientConfiguration());
    }

    @Override
    public SdkHttpFullRequest.Builder execute(SdkHttpFullRequest.Builder request,
                                              RequestExecutionContext context) throws Exception {
        if (userAgent.userAgentInAdditionalHeaders() || hasUserAgentInRequestConfig(context)) {
            return request;
        }
        String headerValue = finalizeUserAgent(context);
        return request.putHeader(HEADER_USER_AGENT, headerValue);
    }

    private boolean hasUserAgentInRequestConfig(RequestExecutionContext context) {
        Map<String, List<String>> requestHeaders = context.requestConfig().headers();
        if (requestHeaders == null) {
            return false;
        }
        return requestHeaders.containsKey(HEADER_USER_AGENT);
    }

    private String finalizeUserAgent(RequestExecutionContext context) {
        ExecutionAttributes executionAttributes = context.executionAttributes();

        //separate apiNames into opaque customer added values and known values added internally as metrics
        Pair<List<ApiName>, Collection<String>> groupedApiNames = groupApiNames(context.requestConfig().apiNames());
        List<ApiName> customApiNames = groupedApiNames.left();

        List<AdditionalMetadata> userAgentMetadata =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.USER_AGENT_METADATA);
        BusinessMetricCollection businessMetrics = businessMetrics(executionAttributes, groupedApiNames.right());

        if (customApiNames.isEmpty()) {
            // Request-level api names are per request by definition; everything else is cacheable.
            return userAgent.headerValue(businessMetrics.recordedMetrics(), userAgentMetadata,
                                         () -> buildUserAgent(userAgentMetadata, businessMetrics, customApiNames));
        }
        return buildUserAgent(userAgentMetadata, businessMetrics, customApiNames);
    }

    private String buildUserAgent(List<AdditionalMetadata> userAgentMetadata,
                                  BusinessMetricCollection businessMetrics,
                                  List<ApiName> customApiNames) {
        String prefix = userAgent.constantPrefix();
        //sized to fit the constant prefix plus the usual per-request additions so it does not have to grow
        StringBuilder javaUserAgent = new StringBuilder(prefix.length() + 64);
        javaUserAgent.append(prefix);

        //add useragent metadata from execution context
        if (userAgentMetadata != null) {
            for (AdditionalMetadata metadata : userAgentMetadata) {
                javaUserAgent.append(SPACE).append(metadata);
            }
        }

        if (!businessMetrics.recordedMetrics().isEmpty()) {
            appendSpaceAndField(javaUserAgent, BUSINESS_METADATA, businessMetrics.asBoundedString());
        }

        //Any ApiName value that isn't known is added to the end of the user agent
        for (ApiName apiName : customApiNames) {
            javaUserAgent.append(SPACE).append(apiName.name()).append(SLASH).append(apiName.version());
        }

        String userSuffix = userAgent.constantSuffix();
        if (userSuffix != null) {
            javaUserAgent.append(SPACE).append(userSuffix);
        }
        return javaUserAgent.toString();
    }

    private static Pair<List<ApiName>, Collection<String>> groupApiNames(List<ApiName> input) {
        if (input.isEmpty()) {
            // The common case: no request-level api names, so don't allocate the two collections.
            return NO_API_NAMES;
        }
        List<ApiName> customApiNames = new ArrayList<>();
        Collection<String> metricsFromApiNames = new ArrayList<>();
        for (ApiName requestApiName : input) {
            if (requestApiName.name().equals(SDK_METRICS)) {
                metricsFromApiNames.add(requestApiName.version());
            } else {
                customApiNames.add(requestApiName);
            }
        }
        return Pair.of(customApiNames, metricsFromApiNames);
    }

    /**
     * The business metrics for this call: those collected along the pipeline, plus any supplied as {@code sdk-metrics}
     * api names, plus the credentials provider's name.
     */
    private static BusinessMetricCollection businessMetrics(ExecutionAttributes executionAttributes,
                                                            Collection<String> metricsFromApiNames) {
        BusinessMetricCollection businessMetrics =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.BUSINESS_METRICS);
        if (businessMetrics == null) {
            businessMetrics = new BusinessMetricCollection();
        }
        businessMetrics.merge(metricsFromApiNames);

        SelectedAuthScheme<?> selectedAuthScheme =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        if (selectedAuthScheme != null) {
            Identity identity = CompletableFutureUtils.joinLikeSync(selectedAuthScheme.identity());
            String providerName = identity == null ? null : identity.providerName().orElse(null);
            if (!StringUtils.isBlank(providerName)) {
                businessMetrics.addMetric(providerName);
            }
        }
        return businessMetrics;
    }
}
