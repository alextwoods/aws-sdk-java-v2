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

package software.amazon.awssdk.endpoints;

import java.util.Optional;

/**
 * The endpoint rules built-ins this SDK sources from client configuration and resolved identity. A rule-set parameter
 * with {@code builtIn} set is bound to the configured value of that built-in unless a more specific binding (static,
 * context, operation-context or client-context parameter) sets it.
 */
public enum EndpointBuiltIn {
    /** {@code AWS::Region}: the configured region ID. */
    REGION("AWS::Region", Type.STRING),
    /** {@code AWS::UseFIPS}: whether FIPS endpoints are required. */
    USE_FIPS("AWS::UseFIPS", Type.BOOLEAN),
    /** {@code AWS::UseDualStack}: whether dual-stack endpoints are required. */
    USE_DUAL_STACK("AWS::UseDualStack", Type.BOOLEAN),
    /** {@code SDK::Endpoint}: the configured endpoint override URI. */
    ENDPOINT("SDK::Endpoint", Type.STRING),
    /** {@code AWS::Auth::AccountId}: the account ID of the resolved credentials, for account-based endpoints. */
    ACCOUNT_ID("AWS::Auth::AccountId", Type.STRING),
    /** {@code AWS::Auth::AccountIdEndpointMode}: {@code preferred}, {@code required} or {@code disabled}. */
    ACCOUNT_ID_ENDPOINT_MODE("AWS::Auth::AccountIdEndpointMode", Type.STRING),
    /** {@code AWS::S3::Accelerate}. */
    S3_ACCELERATE("AWS::S3::Accelerate", Type.BOOLEAN),
    /** {@code AWS::S3::DisableMultiRegionAccessPoints}. */
    S3_DISABLE_MULTI_REGION_ACCESS_POINTS("AWS::S3::DisableMultiRegionAccessPoints", Type.BOOLEAN),
    /** {@code AWS::S3::ForcePathStyle}. */
    S3_FORCE_PATH_STYLE("AWS::S3::ForcePathStyle", Type.BOOLEAN),
    /** {@code AWS::S3::UseArnRegion}. */
    S3_USE_ARN_REGION("AWS::S3::UseArnRegion", Type.BOOLEAN),
    /** {@code AWS::S3::UseGlobalEndpoint}: whether {@code us-east-1} uses the legacy global S3 endpoint. */
    S3_USE_GLOBAL_ENDPOINT("AWS::S3::UseGlobalEndpoint", Type.BOOLEAN),
    /** {@code AWS::S3Control::UseArnRegion}. */
    S3_CONTROL_USE_ARN_REGION("AWS::S3Control::UseArnRegion", Type.BOOLEAN),
    /** {@code AWS::STS::UseGlobalEndpoint}: whether STS uses the legacy global endpoint. */
    STS_USE_GLOBAL_ENDPOINT("AWS::STS::UseGlobalEndpoint", Type.BOOLEAN);

    /** The value type of a built-in. */
    public enum Type {
        STRING,
        BOOLEAN
    }

    private final String id;
    private final Type type;

    EndpointBuiltIn(String id, Type type) {
        this.id = id;
        this.type = type;
    }

    /** The built-in identifier used in rule sets, for example {@code AWS::Region}. */
    public String id() {
        return id;
    }

    /** The value type of the built-in. */
    public Type type() {
        return type;
    }

    /** The built-in with identifier {@code id}, if this SDK version knows it. */
    public static Optional<EndpointBuiltIn> fromId(String id) {
        for (EndpointBuiltIn builtIn : values()) {
            if (builtIn.id.equals(id)) {
                return Optional.of(builtIn);
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return id;
    }
}
