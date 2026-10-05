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

import software.amazon.awssdk.auth.api.AuthScheme;
import software.amazon.awssdk.auth.api.AuthSchemeIds;

/**
 * The {@code aws.auth#sigv4-s3express} auth scheme, with the V2 name. Its identities come from an
 * {@link S3ExpressIdentityProvider}, which the S3 service artifact registers with the {@code CreateSession} call.
 */
public final class S3ExpressAuthScheme implements AuthScheme<S3ExpressSessionCredentials> {
    /** The scheme ID. */
    public static final String SCHEME_ID = AuthSchemeIds.SIGV4_S3EXPRESS;
    private static final S3ExpressAuthScheme INSTANCE = new S3ExpressAuthScheme();

    private S3ExpressAuthScheme() {
    }

    public static S3ExpressAuthScheme create() {
        return INSTANCE;
    }

    @Override
    public String schemeId() {
        return SCHEME_ID;
    }

    @Override
    public Class<S3ExpressSessionCredentials> identityType() {
        return S3ExpressSessionCredentials.class;
    }

    @Override
    public S3ExpressHttpSigner signer() {
        return S3ExpressHttpSigner.create();
    }

    @Override
    public String toString() {
        return "S3ExpressAuthScheme";
    }
}
