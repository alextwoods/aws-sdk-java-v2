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

package software.amazon.awssdk.awscore.client.config;

import software.amazon.awssdk.core.client.config.ClientOption;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.regions.Region;

/**
 * The AWS {@link ClientOption} keys of a resolved client configuration, with the V2 constant names.
 */
public final class AwsClientOption {
    /** The region the client sends requests to. */
    public static final ClientOption<Region> AWS_REGION = ClientOption.create("AwsRegion", Region.class);

    /**
     * The provider of the AWS credentials the client signs with. The option's declared type is the raw
     * {@link IdentityProvider} class, so a resolved view also checks {@link IdentityProvider#identityType()}.
     */
    public static final ClientOption<IdentityProvider<? extends AwsCredentialsIdentity>> CREDENTIALS_IDENTITY_PROVIDER =
        ClientOption.create("CredentialsIdentityProvider", credentialsProviderType());

    private AwsClientOption() {
    }

    @SuppressWarnings("unchecked")
    private static Class<IdentityProvider<? extends AwsCredentialsIdentity>> credentialsProviderType() {
        return (Class<IdentityProvider<? extends AwsCredentialsIdentity>>) (Class<?>) IdentityProvider.class;
    }
}
