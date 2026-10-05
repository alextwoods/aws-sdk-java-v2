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
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/** The {@code aws.auth#sigv4} auth scheme, with the V2 name: AWS credentials signed by {@link AwsV4HttpSigner}. */
public final class AwsV4AuthScheme implements AuthScheme<AwsCredentialsIdentity> {
    /** The scheme ID. */
    public static final String SCHEME_ID = AuthSchemeIds.SIGV4;
    private static final AwsV4AuthScheme INSTANCE = new AwsV4AuthScheme();

    private AwsV4AuthScheme() {
    }

    public static AwsV4AuthScheme create() {
        return INSTANCE;
    }

    @Override
    public String schemeId() {
        return SCHEME_ID;
    }

    @Override
    public Class<AwsCredentialsIdentity> identityType() {
        return AwsCredentialsIdentity.class;
    }

    @Override
    public AwsV4HttpSigner signer() {
        return AwsV4HttpSigner.create();
    }

    @Override
    public String toString() {
        return "AwsV4AuthScheme";
    }
}
