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

package software.amazon.awssdk.awscore.override;

import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.override.CallOption;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;

/** The AWS {@link CallOption}s: options an {@code AwsRequestOverrideConfiguration} may change for one call. */
public final class AwsCallOption {
    /**
     * The per-call credentials provider, replacing {@link AwsClientOption#CREDENTIALS_IDENTITY_PROVIDER} for one call
     * (V2 {@code AwsRequestOverrideConfiguration.credentialsProvider}).
     */
    public static final CallOption<IdentityProvider<? extends AwsCredentialsIdentity>> CREDENTIALS_IDENTITY_PROVIDER =
        CallOption.replacing(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER);

    private AwsCallOption() {
    }
}
