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

package software.amazon.awssdk.enhanced.dynamodb.internal;

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.ApiName;

/**
 * The user-agent contribution of the DynamoDB Enhanced Client (Requirements 21.6, 21.7): every DynamoDB request the
 * enhanced clients send passes {@link #OVERRIDE} as its explicit override, so its user agent lists the V2
 * {@code BusinessMetricFeatureId.DDB_MAPPER} feature identifier {@value #FEATURE_ID} in its {@code m/} feature list,
 * as the V2 {@code ApplyUserAgentInterceptor} ({@code sdk-metrics/d}) did. Requests the application sends through
 * the same DynamoDB client do not carry it (V2 tagged every request of every DynamoDB client of a JVM with the
 * enhanced client on its class path).
 */
public final class EnhancedClientUserAgent {
    /** The V2 {@code BusinessMetricFeatureId.DDB_MAPPER} value. */
    public static final String FEATURE_ID = "d";

    /** The API name that contributes {@link #FEATURE_ID}. */
    public static final ApiName API_NAME = ApiName.builder().name(ApiName.SDK_METRICS).version(FEATURE_ID).build();

    /** The explicit override of every request the enhanced clients send. Immutable. */
    public static final AwsRequestOverrideConfiguration OVERRIDE =
        AwsRequestOverrideConfiguration.builder().addApiName(API_NAME).build();

    private EnhancedClientUserAgent() {
    }
}
