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
 * Credentials from the {@code aws.accessKeyId}, {@code aws.secretAccessKey}, {@code aws.sessionToken} and
 * {@code aws.accountId} JVM system properties, with the V2 name. The properties are read on every resolution.
 */
public final class SystemPropertyCredentialsProvider implements IdentityProvider<AwsCredentialsIdentity> {
    /** The provider name identities record. */
    public static final String PROVIDER_NAME = "SystemPropertyCredentialsProvider";

    private final SettingSource properties;

    private SystemPropertyCredentialsProvider(SettingSource properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    /** The provider of the JVM's system properties. */
    public static SystemPropertyCredentialsProvider create() {
        return new SystemPropertyCredentialsProvider(SettingSource.jvmProperties());
    }

    /** The provider of the properties {@code properties} supplies. */
    public static SystemPropertyCredentialsProvider create(SettingSource properties) {
        return new SystemPropertyCredentialsProvider(properties);
    }

    @Override
    public Class<AwsCredentialsIdentity> identityType() {
        return AwsCredentialsIdentity.class;
    }

    @Override
    public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
        try {
            return CompletableFuture.completedFuture(SystemSettingsCredentials.load(
                setting -> properties.setting(setting.property()), PROVIDER_NAME));
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public String toString() {
        return PROVIDER_NAME + "()";
    }
}
