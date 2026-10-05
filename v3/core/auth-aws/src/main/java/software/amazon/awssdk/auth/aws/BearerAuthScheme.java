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

package software.amazon.awssdk.auth.aws;

import software.amazon.awssdk.auth.api.AuthScheme;
import software.amazon.awssdk.auth.api.AuthSchemeIds;
import software.amazon.awssdk.identity.spi.TokenIdentity;

/** The {@code smithy.api#httpBearerAuth} auth scheme, with the V2 name: a {@link TokenIdentity} bearer token. */
public final class BearerAuthScheme implements AuthScheme<TokenIdentity> {
    /** The scheme ID. */
    public static final String SCHEME_ID = AuthSchemeIds.HTTP_BEARER;
    private static final BearerAuthScheme INSTANCE = new BearerAuthScheme();

    private BearerAuthScheme() {
    }

    public static BearerAuthScheme create() {
        return INSTANCE;
    }

    @Override
    public String schemeId() {
        return SCHEME_ID;
    }

    @Override
    public Class<TokenIdentity> identityType() {
        return TokenIdentity.class;
    }

    @Override
    public BearerHttpSigner signer() {
        return BearerHttpSigner.create();
    }

    @Override
    public String toString() {
        return "BearerAuthScheme";
    }
}
