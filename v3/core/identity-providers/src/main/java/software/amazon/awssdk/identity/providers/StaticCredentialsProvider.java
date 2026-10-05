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
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.utils.ToString;

/**
 * Always returns the same credentials, with the V2 name and factory. Fixed credentials cannot be refreshed, so a
 * rejected static identity is never retried with (Requirement 13.16).
 */
public final class StaticCredentialsProvider implements IdentityProvider<AwsCredentialsIdentity> {
    private final CompletableFuture<AwsCredentialsIdentity> credentials;
    private final AwsCredentialsIdentity value;

    private StaticCredentialsProvider(AwsCredentialsIdentity credentials) {
        this.value = Objects.requireNonNull(credentials, "credentials");
        this.credentials = CompletableFuture.completedFuture(credentials);
    }

    /** The provider of {@code credentials}. */
    public static StaticCredentialsProvider create(AwsCredentialsIdentity credentials) {
        return new StaticCredentialsProvider(credentials);
    }

    @Override
    public Class<AwsCredentialsIdentity> identityType() {
        return AwsCredentialsIdentity.class;
    }

    @Override
    public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
        return credentials.copy();
    }

    @Override
    public String toString() {
        return ToString.builder("StaticCredentialsProvider").add("credentials", value).build();
    }
}
