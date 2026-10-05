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

import java.time.Instant;
import software.amazon.awssdk.identity.spi.internal.DefaultTokenIdentity;

/**
 * A bearer token, with the V2 name and members, used by the {@code smithy.api#httpBearerAuth} scheme.
 *
 * <p>The instances {@link #create} returns are immutable, equal when token and expiration are equal, and never render
 * the token in {@code toString}.
 */
public interface TokenIdentity extends Identity {
    /** The token. Secret: never log it. */
    String token();

    /** A token that does not expire. */
    static TokenIdentity create(String token) {
        return new DefaultTokenIdentity(token, null, null);
    }

    /** A token that expires at {@code expirationTime}; {@code null} means it does not expire. */
    static TokenIdentity create(String token, Instant expirationTime) {
        return new DefaultTokenIdentity(token, expirationTime, null);
    }

    /**
     * A token that expires at {@code expirationTime}, resolved by {@code providerName}; either may be {@code null}.
     *
     * @throws NullPointerException if {@code token} is null
     */
    static TokenIdentity create(String token, Instant expirationTime, String providerName) {
        return new DefaultTokenIdentity(token, expirationTime, providerName);
    }
}
