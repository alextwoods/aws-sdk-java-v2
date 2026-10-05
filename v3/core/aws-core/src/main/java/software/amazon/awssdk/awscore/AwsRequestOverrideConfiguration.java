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

package software.amazon.awssdk.awscore;

import java.util.Optional;
import software.amazon.awssdk.awscore.override.AwsCallOption;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * The AWS {@link RequestOverrideConfiguration}: the protocol-agnostic per-call deltas plus a per-call credentials
 * provider (Requirement 9.12), with the V2 names {@code credentialsProvider}, {@code credentialsIdentityProvider} and
 * {@code from}.
 *
 * <p>Like its parent, an override is a sparse immutable delta: an absent credentials provider means "use the client's".
 * The provider is stored as the {@link AwsCallOption#CREDENTIALS_IDENTITY_PROVIDER} call-option entry, so
 * {@code CallOptions} layers it over the client's provider for this call only. Equal when every delta is equal.
 */
public final class AwsRequestOverrideConfiguration extends RequestOverrideConfiguration {
    private AwsRequestOverrideConfiguration(BuilderImpl builder) {
        super(builder);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    /**
     * {@code configuration} as an AWS override: the same instance if it already is one, otherwise a new override with
     * its protocol-agnostic deltas and no credentials provider. {@code null} returns {@code null}, as in V2.
     */
    public static AwsRequestOverrideConfiguration from(RequestOverrideConfiguration configuration) {
        if (configuration == null) {
            return null;
        }
        if (configuration instanceof AwsRequestOverrideConfiguration aws) {
            return aws;
        }
        return new BuilderImpl(configuration).build();
    }

    /** The provider of the credentials this call signs with, if overridden. */
    public Optional<IdentityProvider<? extends AwsCredentialsIdentity>> credentialsProvider() {
        return callOption(AwsCallOption.CREDENTIALS_IDENTITY_PROVIDER);
    }

    /** The V2 name of {@link #credentialsProvider()}; returns the same value. */
    public Optional<IdentityProvider<? extends AwsCredentialsIdentity>> credentialsIdentityProvider() {
        return credentialsProvider();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    @Override
    public String toString() {
        return ToString.builder("AwsRequestOverrideConfiguration")
                       .add("headers", headers().isEmpty() ? null : headers().keySet())
                       .add("rawQueryParameters", rawQueryParameters().isEmpty() ? null : rawQueryParameters().keySet())
                       .add("apiCallTimeout", apiCallTimeout().orElse(null))
                       .add("apiCallAttemptTimeout", apiCallAttemptTimeout().orElse(null))
                       .add("executionAttributes", executionAttributes().isEmpty() ? null
                                                                                   : executionAttributes().keySet())
                       .add("callOptions", callOptions().isEmpty() ? null : callOptionNames())
                       .build();
    }

    /** Builder of an {@link AwsRequestOverrideConfiguration}. */
    public interface Builder extends RequestOverrideConfiguration.Builder<Builder>,
                                     SdkBuilder<Builder, AwsRequestOverrideConfiguration> {
        /** The provider of the credentials this call signs with; {@code null} clears the override. */
        Builder credentialsProvider(IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider);

        IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider();

        @Override
        AwsRequestOverrideConfiguration build();
    }

    private static final class BuilderImpl extends RequestOverrideConfiguration.BuilderImpl<Builder>
        implements Builder {
        private BuilderImpl() {
        }

        private BuilderImpl(RequestOverrideConfiguration configuration) {
            super(configuration);
        }

        @Override
        public Builder credentialsProvider(IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider) {
            return putCallOption(AwsCallOption.CREDENTIALS_IDENTITY_PROVIDER, credentialsProvider);
        }

        @Override
        public IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider() {
            return callOption(AwsCallOption.CREDENTIALS_IDENTITY_PROVIDER);
        }

        @Override
        public AwsRequestOverrideConfiguration build() {
            return new AwsRequestOverrideConfiguration(this);
        }
    }
}
