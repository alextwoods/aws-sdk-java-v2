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

package software.amazon.awssdk.auth.api;

import java.util.Optional;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;

/**
 * A Modern Auth scheme a client can use, with the V2 name and members: the modeled scheme ID, the identity type it
 * signs with, where that identity comes from and the signer (Requirements 13.1, 13.2).
 *
 * <p>Implementations are immutable and thread-safe.
 *
 * @param <IdentityT> the identity type
 */
public interface AuthScheme<IdentityT extends Identity> {
    /** The absolute Smithy shape ID of the scheme's auth trait, for example {@code aws.auth#sigv4}. */
    String schemeId();

    /** The identity type the signer signs with. */
    Class<IdentityT> identityType();

    /**
     * The provider of this scheme's identities among {@code providers}; by default the one registered for
     * {@link #identityType()}. Empty means the client cannot use this scheme.
     */
    default Optional<IdentityProvider<? extends IdentityT>> identityProvider(IdentityProviders providers) {
        return providers.identityProvider(identityType());
    }

    /** The signer. */
    HttpSigner<IdentityT> signer();
}
