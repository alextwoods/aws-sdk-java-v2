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

import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.identity.spi.Identity;

/**
 * Signs HTTP requests with an identity for one Modern Auth scheme, with the V2 name (Requirement 13.2). There is no
 * legacy {@code Signer} contract in V3.
 *
 * <p>Signing is CPU-only work over the in-memory request: signers never read a streaming body (a streaming payload is
 * either unsigned or covered by a precomputed/chunked hash that the pipeline supplies), so {@link #signAsync} may run
 * on any thread. A signer that signs a hash of the payload says so through {@link #payloadSigningMode}, and the
 * pipeline then supplies the hash of a streaming payload as {@link #PAYLOAD_SHA256}. Implementations are immutable
 * and thread-safe.
 *
 * @param <IdentityT> the identity type this signer signs with
 */
public interface HttpSigner<IdentityT extends Identity> {
    /** The clock signing timestamps are taken from; the system UTC clock when absent. */
    SignerProperty<Clock> SIGNING_CLOCK = SignerProperty.create(HttpSigner.class, "SigningClock");

    /**
     * The lower-case hex SHA-256 of every byte of the request's streaming payload, as the pipeline computed it before
     * signing for a signer whose {@link #payloadSigningMode} is {@link PayloadSigningMode#PAYLOAD_HASH}. Set by the
     * pipeline only; absent for an in-memory payload, which the signer hashes itself.
     */
    SignerProperty<String> PAYLOAD_SHA256 = SignerProperty.create(HttpSigner.class, "PayloadSha256");

    /**
     * How {@link #sign} will cover the payload of {@code request}, asked by the pipeline before its request checksum
     * stage and again before signing, with the request as it is then. Empty (the default) if the signer needs
     * nothing of the payload: the pipeline then reads no streaming body for the signer, and keeps a request checksum
     * in a header.
     */
    default Optional<PayloadSigningMode> payloadSigningMode(SignRequest<? extends IdentityT> request) {
        return Optional.empty();
    }

    /**
     * Signs {@code request}.
     *
     * @throws software.amazon.awssdk.core.exception.SdkClientException if the request cannot be signed
     */
    SignedRequest sign(SignRequest<? extends IdentityT> request);

    /** Signs {@code request}; the returned future completes exceptionally with the failure of {@link #sign}. */
    default CompletableFuture<SignedRequest> signAsync(SignRequest<? extends IdentityT> request) {
        try {
            return CompletableFuture.completedFuture(sign(request));
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
