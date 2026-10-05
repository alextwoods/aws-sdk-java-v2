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

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * What a signer hands the pipeline so it can chain-sign the frames of an input event stream after the initial request
 * (Requirement 13.10): the frame signature algorithm, the credential scope and signing keys of the request signature,
 * and the time source of the frames. The chain itself (seed, order, envelopes, closing frame) is owned by the event stream signer
 * the pipeline builds from this context; the seed is {@link SignedRequest#signature()}.
 *
 * <p>A signer returns a context only for a request it signed as an event stream (for SigV4, with
 * {@code x-amz-content-sha256: STREAMING-AWS4-HMAC-SHA256-EVENTS}).
 *
 * @param algorithm the frame signature algorithm, for SigV4 {@value #SIGV4_PAYLOAD}
 * @param region the credential-scope region of the request signature
 * @param service the credential-scope service of the request signature
 * @param signingKeys the signing key of a scope date; the caller keeps and never modifies the returned array. Never
 *        rendered: it derives keys from the secret.
 * @param frameTime the signing time of each frame, read once per frame (the event stream signer truncates it to
 *        milliseconds). A signer derives it from the request's signing clock ({@link HttpSigner#SIGNING_CLOCK} when
 *        set, else the system UTC clock), so a fixed signing clock also fixes every frame's {@code :date} and
 *        signature. A supplier rather than a clock keeps the time source owned by the signer: the pipeline that
 *        builds the event stream signer never depends on a clock type.
 */
public record EventStreamSigningContext(String algorithm, String region, String service,
                                        Function<LocalDate, byte[]> signingKeys,
                                        Supplier<Instant> frameTime) {
    /** The SigV4 frame signature algorithm. */
    public static final String SIGV4_PAYLOAD = "AWS4-HMAC-SHA256-PAYLOAD";

    public EventStreamSigningContext {
        Objects.requireNonNull(algorithm, "algorithm");
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(signingKeys, "signingKeys");
        Objects.requireNonNull(frameTime, "frameTime");
    }

    @Override
    public String toString() {
        return "EventStreamSigningContext[" + algorithm + ", " + region + "/" + service + "]";
    }
}
