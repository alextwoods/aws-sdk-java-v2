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

import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.BUSINESS_METADATA;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.SLASH;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.SPACE;
import static software.amazon.awssdk.core.internal.useragent.UserAgentConstant.appendSpaceAndField;
import static software.amazon.awssdk.utils.StringUtils.trim;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.pipeline.stages.ApplyUserAgentStage;
import software.amazon.awssdk.core.useragent.AdditionalMetadata;
import software.amazon.awssdk.core.useragent.BusinessMetricCollection;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringUtils;

/**
 * The {@code User-Agent} header of the schema-based request pipeline (racecar H9, re-implemented). It produces the same
 * value as the legacy {@link ApplyUserAgentStage}, but the parts that depend only on the client configuration (the
 * user prefix and the client user agent, the user suffix, and whether the client's additional headers already set a
 * {@code User-Agent}) are computed once per client configuration, and a call that adds no request-level value gets the
 * precomputed header without building a string.
 *
 * <p>Instances are created once per client configuration by {@link RequestMutationStages} and shared by every call.
 */
@SdkInternalApi
@ThreadSafe
public final class UserAgentHeaderCache {
    private static final Logger log = Logger.loggerFor(ApplyUserAgentStage.class);

    private final boolean userAgentInAdditionalHeaders;
    private final boolean clientUserAgentMissing;
    /** The optional user prefix followed by the client user agent. */
    private final String head;
    /** A space and the user suffix, or the empty string. */
    private final String tail;
    /** {@link #head} followed by {@link #tail}: the header of a call that adds no request-level value. */
    private final String clientOnlyValue;
    private final ApplyUserAgentStage legacyStage;

    private UserAgentHeaderCache(SdkClientConfiguration clientConfiguration) {
        this.legacyStage = new ApplyUserAgentStage(HttpClientDependencies.builder()
                                                                         .clientConfiguration(clientConfiguration)
                                                                         .build());
        Map<String, List<String>> additionalHeaders = clientConfiguration.option(SdkClientOption.ADDITIONAL_HTTP_HEADERS);
        this.userAgentInAdditionalHeaders = additionalHeaders != null
                                            && additionalHeaders.containsKey(ApplyUserAgentStage.HEADER_USER_AGENT);

        String clientUserAgent = clientConfiguration.option(SdkClientOption.CLIENT_USER_AGENT);
        this.clientUserAgentMissing = clientUserAgent == null;

        StringBuilder headBuilder = new StringBuilder();
        String userPrefix = trim(clientConfiguration.option(SdkAdvancedClientOption.USER_AGENT_PREFIX));
        if (!StringUtils.isEmpty(userPrefix)) {
            headBuilder.append(userPrefix).append(SPACE);
        }
        headBuilder.append(clientUserAgent == null ? "" : clientUserAgent);
        this.head = headBuilder.toString();

        String userSuffix = trim(clientConfiguration.option(SdkAdvancedClientOption.USER_AGENT_SUFFIX));
        this.tail = StringUtils.isEmpty(userSuffix) ? "" : SPACE + userSuffix;
        this.clientOnlyValue = head + tail;
    }

    /**
     * Creates the cache for a client configuration.
     */
    public static UserAgentHeaderCache create(SdkClientConfiguration clientConfiguration) {
        return new UserAgentHeaderCache(clientConfiguration);
    }

    /**
     * Returns the {@code User-Agent} header value for a call, or null when the call must not get one because the client's
     * additional headers or the request's override configuration set the header, as in {@link ApplyUserAgentStage}.
     *
     * <p>Like the legacy stage, this merges the request's {@code sdk-metrics} API names and the credential provider name
     * into the call's {@link SdkInternalExecutionAttribute#BUSINESS_METRICS} collection. The provider name is read from
     * the selected auth scheme's identity when it is resolved; when it is not resolved yet (or failed), the legacy stage
     * computes the header, waiting for the identity as before.
     *
     * @param context the call's context
     * @return the header value, or null to leave the header unset
     */
    public String headerValue(RequestExecutionContext context) {
        SelectedAuthScheme<?> selectedAuthScheme =
            context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
        if (selectedAuthScheme != null
            && (!selectedAuthScheme.identity().isDone() || selectedAuthScheme.identity().isCompletedExceptionally())) {
            return legacyHeaderValue(context);
        }
        if (userAgentInAdditionalHeaders) {
            return null;
        }
        Map<String, List<String>> requestHeaders = context.requestConfig().headers();
        if (requestHeaders != null && requestHeaders.containsKey(ApplyUserAgentStage.HEADER_USER_AGENT)) {
            return null;
        }
        if (clientUserAgentMissing) {
            log.warn(() -> "Client user agent configuration is missing, so request user agent will be incomplete.");
        }

        ExecutionAttributes attributes = context.executionAttributes();
        List<ApiName> apiNames = context.requestConfig().apiNames();
        List<String> metricsFromApiNames = null;
        List<ApiName> customApiNames = null;
        for (ApiName apiName : apiNames) {
            if (ApplyUserAgentStage.SDK_METRICS.equals(apiName.name())) {
                metricsFromApiNames = add(metricsFromApiNames, apiName.version());
            } else {
                customApiNames = add(customApiNames, apiName);
            }
        }

        List<AdditionalMetadata> metadata = attributes.getAttribute(SdkInternalExecutionAttribute.USER_AGENT_METADATA);
        String businessMetrics = businessMetrics(attributes, selectedAuthScheme, metricsFromApiNames);

        if ((metadata == null || metadata.isEmpty()) && businessMetrics == null && customApiNames == null) {
            return clientOnlyValue;
        }

        StringBuilder value = new StringBuilder(clientOnlyValue.length() + 128).append(head);
        if (metadata != null) {
            for (AdditionalMetadata entry : metadata) {
                value.append(SPACE).append(entry);
            }
        }
        if (businessMetrics != null) {
            appendSpaceAndField(value, BUSINESS_METADATA, businessMetrics);
        }
        if (customApiNames != null) {
            for (ApiName apiName : customApiNames) {
                value.append(SPACE).append(apiName.name()).append(SLASH).append(apiName.version());
            }
        }
        return value.append(tail).toString();
    }

    /**
     * Same as the legacy stage's business metrics string: the call's collection (or a new one when the call has none)
     * with the API-name metrics and the credential provider name merged in, or null when it is empty.
     */
    private static String businessMetrics(ExecutionAttributes attributes,
                                          SelectedAuthScheme<?> selectedAuthScheme,
                                          List<String> metricsFromApiNames) {
        BusinessMetricCollection businessMetrics = attributes.getAttribute(SdkInternalExecutionAttribute.BUSINESS_METRICS);
        if (businessMetrics == null) {
            businessMetrics = new BusinessMetricCollection();
        }
        businessMetrics.merge(metricsFromApiNames);

        if (selectedAuthScheme != null) {
            Identity identity = selectedAuthScheme.identity().getNow(null);
            Optional<String> providerName = identity == null ? Optional.empty() : identity.providerName();
            if (providerName.isPresent() && !StringUtils.isBlank(providerName.get())) {
                businessMetrics.addMetric(providerName.get());
            }
        }

        if (businessMetrics.recordedMetrics().isEmpty()) {
            return null;
        }
        return businessMetrics.asBoundedString();
    }

    /**
     * The legacy stage's value, for a call whose identity is not resolved yet.
     */
    private String legacyHeaderValue(RequestExecutionContext context) {
        try {
            return legacyStage.execute(SdkHttpFullRequest.builder(), context)
                              .firstMatchingHeader(ApplyUserAgentStage.HEADER_USER_AGENT)
                              .orElse(null);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw SdkClientException.create("Unable to compute the User-Agent header", e);
        }
    }

    private static <T> List<T> add(List<T> list, T value) {
        List<T> result = list == null ? new ArrayList<>() : list;
        result.add(value);
        return result;
    }
}
