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
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.utils.ToString;

/** The {@link TokenIdentity} implementation. Not a public API. */
public final class DefaultTokenIdentity implements TokenIdentity {
    private final String token;
    private final Instant expirationTime;
    private final String providerName;

    public DefaultTokenIdentity(String token, Instant expirationTime, String providerName) {
        this.token = Objects.requireNonNull(token, "token");
        this.expirationTime = expirationTime;
        this.providerName = providerName;
    }

    @Override
    public String token() {
        return token;
    }

    @Override
    public Optional<Instant> expirationTime() {
        return Optional.ofNullable(expirationTime);
    }

    @Override
    public Optional<String> providerName() {
        return Optional.ofNullable(providerName);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DefaultTokenIdentity that && token.equals(that.token)
               && Objects.equals(expirationTime, that.expirationTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token, expirationTime);
    }

    @Override
    public String toString() {
        return ToString.builder("TokenIdentity")
                       .add("expirationTime", expirationTime)
                       .add("providerName", providerName)
                       .build();
    }
}
