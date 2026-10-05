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

package software.amazon.awssdk.identity.spi.internal;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.utils.ToString;

/** The {@link AwsSessionCredentialsIdentity} implementation. Not a public API. */
public final class DefaultAwsSessionCredentialsIdentity implements AwsSessionCredentialsIdentity {
    private final String accessKeyId;
    private final String secretAccessKey;
    private final String sessionToken;
    private final String accountId;
    private final String providerName;
    private final Instant expirationTime;

    private DefaultAwsSessionCredentialsIdentity(BuilderImpl builder) {
        this.accessKeyId = Objects.requireNonNull(builder.accessKeyId, "accessKeyId");
        this.secretAccessKey = Objects.requireNonNull(builder.secretAccessKey, "secretAccessKey");
        this.sessionToken = Objects.requireNonNull(builder.sessionToken, "sessionToken");
        this.accountId = builder.accountId;
        this.providerName = builder.providerName;
        this.expirationTime = builder.expirationTime;
    }

    public static AwsSessionCredentialsIdentity.Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public String accessKeyId() {
        return accessKeyId;
    }

    @Override
    public String secretAccessKey() {
        return secretAccessKey;
    }

    @Override
    public String sessionToken() {
        return sessionToken;
    }

    @Override
    public Optional<String> accountId() {
        return Optional.ofNullable(accountId);
    }

    @Override
    public Optional<String> providerName() {
        return Optional.ofNullable(providerName);
    }

    @Override
    public Optional<Instant> expirationTime() {
        return Optional.ofNullable(expirationTime);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DefaultAwsSessionCredentialsIdentity that
               && accessKeyId.equals(that.accessKeyId)
               && secretAccessKey.equals(that.secretAccessKey)
               && sessionToken.equals(that.sessionToken)
               && Objects.equals(accountId, that.accountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessKeyId, secretAccessKey, sessionToken, accountId);
    }

    @Override
    public String toString() {
        return ToString.builder("AwsSessionCredentialsIdentity")
                       .add("accessKeyId", accessKeyId)
                       .add("providerName", providerName)
                       .add("accountId", accountId)
                       .add("expirationTime", expirationTime)
                       .build();
    }

    private static final class BuilderImpl implements AwsSessionCredentialsIdentity.Builder {
        private String accessKeyId;
        private String secretAccessKey;
        private String sessionToken;
        private String accountId;
        private String providerName;
        private Instant expirationTime;

        @Override
        public Builder accessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
            return this;
        }

        @Override
        public Builder secretAccessKey(String secretAccessKey) {
            this.secretAccessKey = secretAccessKey;
            return this;
        }

        @Override
        public Builder sessionToken(String sessionToken) {
            this.sessionToken = sessionToken;
            return this;
        }

        @Override
        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        @Override
        public Builder providerName(String providerName) {
            this.providerName = providerName;
            return this;
        }

        @Override
        public Builder expirationTime(Instant expirationTime) {
            this.expirationTime = expirationTime;
            return this;
        }

        @Override
        public AwsSessionCredentialsIdentity build() {
            return new DefaultAwsSessionCredentialsIdentity(this);
        }
    }
}
