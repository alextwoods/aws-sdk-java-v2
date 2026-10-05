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

package software.amazon.awssdk.identity.providers.internal;

import java.util.Optional;
import java.util.function.Function;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;

/**
 * Loads credentials from one kind of system setting (JVM properties or environment variables), with the V2
 * {@code SystemSettingsCredentialsProvider} rules and messages. Not a public API.
 */
public final class SystemSettingsCredentials {
    private SystemSettingsCredentials() {
    }

    /**
     * The credentials the settings {@code lookup} (a setting → value function) defines.
     *
     * @throws SdkClientException if the access key or secret key is missing
     */
    public static AwsCredentialsIdentity load(Function<IdentitySettings, Optional<String>> lookup, String providerName) {
        String accessKey = trimmed(lookup, IdentitySettings.AWS_ACCESS_KEY_ID);
        String secretKey = trimmed(lookup, IdentitySettings.AWS_SECRET_ACCESS_KEY);
        String sessionToken = trimmed(lookup, IdentitySettings.AWS_SESSION_TOKEN);
        String accountId = trimmed(lookup, IdentitySettings.AWS_ACCOUNT_ID);
        if (accessKey == null) {
            throw SdkClientException.create("Unable to load credentials from system settings. Access key must be "
                                            + "specified either via " + IdentitySettings.AWS_ACCESS_KEY_ID.describe()
                                            + ".");
        }
        if (secretKey == null) {
            throw SdkClientException.create("Unable to load credentials from system settings. Secret key must be "
                                            + "specified either via " + IdentitySettings.AWS_SECRET_ACCESS_KEY.describe()
                                            + ".");
        }
        if (sessionToken == null) {
            return AwsCredentialsIdentity.builder().accessKeyId(accessKey).secretAccessKey(secretKey)
                                         .accountId(accountId).providerName(providerName).build();
        }
        return AwsSessionCredentialsIdentity.builder().accessKeyId(accessKey).secretAccessKey(secretKey)
                                            .sessionToken(sessionToken).accountId(accountId)
                                            .providerName(providerName).build();
    }

    private static String trimmed(Function<IdentitySettings, Optional<String>> lookup, IdentitySettings setting) {
        return lookup.apply(setting).map(String::strip).filter(v -> !v.isEmpty()).orElse(null);
    }
}
