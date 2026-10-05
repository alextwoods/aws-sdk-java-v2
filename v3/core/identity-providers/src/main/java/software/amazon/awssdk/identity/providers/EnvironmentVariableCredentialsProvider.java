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

package software.amazon.awssdk.identity.providers;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.core.client.config.SettingSource;
import software.amazon.awssdk.identity.providers.internal.SystemSettingsCredentials;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;

/**
 * Credentials from the {@code AWS_ACCESS_KEY_ID}, {@code AWS_SECRET_ACCESS_KEY}, {@code AWS_SESSION_TOKEN} and
 * {@code AWS_ACCOUNT_ID} environment variables, with the V2 name. The variables are read on every resolution.
 */
public final class EnvironmentVariableCredentialsProvider implements IdentityProvider<AwsCredentialsIdentity> {
    /** The provider name identities record. */
    public static final String PROVIDER_NAME = "EnvironmentVariableCredentialsProvider";

    private final SettingSource environment;

    private EnvironmentVariableCredentialsProvider(SettingSource environment) {
        this.environment = Objects.requireNonNull(environment, "environment");
    }

    /** The provider of the process environment. */
    public static EnvironmentVariableCredentialsProvider create() {
        return new EnvironmentVariableCredentialsProvider(SettingSource.environment());
    }

    /** The provider of the variables {@code environment} supplies. */
    public static EnvironmentVariableCredentialsProvider create(SettingSource environment) {
        return new EnvironmentVariableCredentialsProvider(environment);
    }

    @Override
    public Class<AwsCredentialsIdentity> identityType() {
        return AwsCredentialsIdentity.class;
    }

    @Override
    public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
        try {
            return CompletableFuture.completedFuture(SystemSettingsCredentials.load(
                setting -> environment.setting(setting.environmentVariable()), PROVIDER_NAME));
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public String toString() {
        return PROVIDER_NAME + "()";
    }
}
