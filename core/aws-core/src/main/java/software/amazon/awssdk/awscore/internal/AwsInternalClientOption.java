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

package software.amazon.awssdk.awscore.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.client.config.ClientOption;
import software.amazon.awssdk.core.interceptor.ExecutionAttributesTemplate;

/**
 * Client options that are an implementation detail of the AWS request pipeline: computed once when the client is
 * built, read on every call, and not part of any public or protected configuration surface.
 */
@SdkInternalApi
public final class AwsInternalClientOption<T> extends ClientOption<T> {

    /**
     * The {@link SelectedAuthScheme} placeholder that the legacy {@code SERVICE_SIGNING_NAME} and
     * {@code SIGNING_REGION} execution-attribute writes would produce for this client.
     *
     * <p>Those two attributes are mapped: writing them constructs an {@code "unset"} auth scheme carrying the signing
     * name and region as signer properties, for the benefit of interceptors and old-style signers that read them back.
     * Both values are client constants, so the object graph they produce is the same on every call — but it was being
     * rebuilt per call: an option build, then a copy of that option to add the second property, plus a completed
     * future and two sentinel objects. This option holds that graph, built once, for the common case where no
     * request-level override has already put an auth scheme in place.
     */
    public static final AwsInternalClientOption<SelectedAuthScheme<?>> PLACEHOLDER_AUTH_SCHEME =
        new AwsInternalClientOption<>(new UnsafeValueType(SelectedAuthScheme.class));

    /**
     * The business-metric feature id for the client's retry mode (see
     * {@code BusinessMetricsUtils.resolveRetryMode}), or the empty string if the retry configuration maps to none. It is
     * a function of two client options and was being re-derived — option reads, {@code instanceof} chain, an
     * {@code Optional} — at the start of every call.
     */
    public static final AwsInternalClientOption<String> RETRY_MODE_BUSINESS_METRIC =
        new AwsInternalClientOption<>(new UnsafeValueType(String.class));

    /**
     * The execution attributes that {@code AwsExecutionContextBuilder} seeds on every call and that are functions of the
     * client configuration alone — region, service name, endpoint and auth-scheme settings, checksum preferences and the
     * like — captured once as an {@link ExecutionAttributesTemplate}. They were being read from the configuration and
     * written through attribute storage one at a time, some two dozen of them, at the start of every call. Being a lazy
     * option, the template is recomputed if a plugin changes any option it was derived from.
     */
    public static final AwsInternalClientOption<ExecutionAttributesTemplate> CLIENT_EXECUTION_ATTRIBUTES =
        new AwsInternalClientOption<>(new UnsafeValueType(ExecutionAttributesTemplate.class));

    private AwsInternalClientOption(UnsafeValueType unsafeValueType) {
        super(unsafeValueType);
    }
}
