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

package software.amazon.awssdk.auth.aws.s3express;

import software.amazon.awssdk.auth.api.SignRequest;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.auth.aws.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.auth.aws.AwsV4HttpSigner;
import software.amazon.awssdk.auth.aws.internal.DefaultAwsV4HttpSigner;

/**
 * The {@code aws.auth#sigv4-s3express} signer, with the V2 behavior: SigV4 (properties of {@link AwsV4HttpSigner},
 * including {@link AwsV4HttpSigner#REGION_NAME}) with the session token in {@code x-amz-s3session-token}, or
 * {@code X-Amz-S3session-Token} when presigning.
 */
public final class S3ExpressHttpSigner implements AwsV4FamilyHttpSigner<S3ExpressSessionCredentials> {
    /** The session token header. */
    public static final String SESSION_HEADER = "x-amz-s3session-token";
    /** The session token query parameter of presigned requests. */
    public static final String SESSION_QUERY_PARAMETER = "X-Amz-S3session-Token";

    private static final S3ExpressHttpSigner INSTANCE = new S3ExpressHttpSigner();

    private S3ExpressHttpSigner() {
    }

    public static S3ExpressHttpSigner create() {
        return INSTANCE;
    }

    @Override
    public SignedRequest sign(SignRequest<? extends S3ExpressSessionCredentials> request) {
        AuthLocation location = request.requireProperty(AUTH_LOCATION, AuthLocation.HEADER);
        String tokenName = location == AuthLocation.HEADER ? SESSION_HEADER : SESSION_QUERY_PARAMETER;
        return DefaultAwsV4HttpSigner.sign(request, tokenName, request.identity().sessionToken());
    }

    @Override
    public String toString() {
        return "S3ExpressHttpSigner";
    }
}
