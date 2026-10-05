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
import java.util.Optional;

/**
 * An identity a signer or auth scheme uses to authenticate a request (Requirement 9.1), with the V2 name and members.
 *
 * <p>Implementations are immutable and thread-safe. An identity that expires reports {@link #expirationTime()}; the
 * provider that resolved it, when known, reports {@link #providerName()}. Neither value is secret.
 */
public interface Identity {
    /** When this identity stops being valid, if it expires. */
    default Optional<Instant> expirationTime() {
        return Optional.empty();
    }

    /** Name of the provider that resolved this identity, if known. */
    default Optional<String> providerName() {
        return Optional.empty();
    }
}
