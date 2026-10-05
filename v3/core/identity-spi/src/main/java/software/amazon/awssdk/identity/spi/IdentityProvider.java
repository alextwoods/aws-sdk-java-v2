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

package software.amazon.awssdk.identity.spi;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Resolves identities of one type (Requirement 9.1), with the V2 name and members.
 *
 * <p>Resolution is asynchronous so async clients never block an event loop on credential loading (Requirement 8.8):
 * {@link #resolveIdentity(ResolveIdentityRequest)} returns a future that completes with an identity or exceptionally
 * with the cause. A provider may refresh; a client resolves its provider once, not the identity it returns
 * (design, "Client construction and resolved configuration"). Implementations must be thread-safe.
 *
 * @param <IdentityT> the identity type this provider resolves
 */
public interface IdentityProvider<IdentityT extends Identity> {
    /**
     * The type of identity this provider resolves. Clients use it to match a provider to the auth schemes that can use
     * its identities, so it must be the type every resolved identity is an instance of.
     */
    Class<IdentityT> identityType();

    /** Resolves an identity for {@code request}. */
    CompletableFuture<? extends IdentityT> resolveIdentity(ResolveIdentityRequest request);

    /** Resolves an identity for the request {@code consumer} configures on a new builder. */
    default CompletableFuture<? extends IdentityT> resolveIdentity(Consumer<ResolveIdentityRequest.Builder> consumer) {
        return resolveIdentity(ResolveIdentityRequest.builder().applyMutation(consumer).build());
    }

    /** Resolves an identity for a request with no properties. */
    default CompletableFuture<? extends IdentityT> resolveIdentity() {
        return resolveIdentity(ResolveIdentityRequest.builder().build());
    }
}
