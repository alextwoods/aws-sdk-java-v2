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

package software.amazon.awssdk.dynamicclient;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;

/**
 * Fixed test credentials, so that builds do not read the default credentials chain.
 */
final class TestCredentials implements IdentityProvider<AwsCredentialsIdentity> {
    static final TestCredentials PROVIDER = new TestCredentials();

    private TestCredentials() {
    }

    @Override
    public Class<AwsCredentialsIdentity> identityType() {
        return AwsCredentialsIdentity.class;
    }

    @Override
    public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
        return CompletableFuture.completedFuture(AwsCredentialsIdentity.create("akid", "skid"));
    }
}
