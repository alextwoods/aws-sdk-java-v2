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

package software.amazon.awssdk.eventstream;

import java.nio.ByteBuffer;
import java.time.Instant;

/**
 * The auth-scheme seam of event stream signing: computes the chained signature of one outgoing frame. The
 * {@link EventStreamSigner} owns the chain (prior-signature state, frame order, the signed envelope and the closing
 * frame); an implementation owns only the scheme's signature function. {@link SigV4FrameSignatureAlgorithm} is the
 * SigV4 ({@code AWS4-HMAC-SHA256-PAYLOAD}) implementation.
 *
 * <p>Implementations must be thread safe or confined to one signer; a signer invokes its algorithm from one thread at
 * a time, in frame order.
 */
@FunctionalInterface
public interface FrameSignatureAlgorithm {
    /**
     * The signature of one frame.
     *
     * @param priorSignature the signature of the previous frame, or the seed (request) signature for the first frame;
     *        a copy the implementation may keep
     * @param signingTime the frame's signing time (millisecond precision, the value of its {@code :date} header)
     * @param headers the encoded non-signature headers of the envelope ({@link MessageEncoder#encodeHeaders}); a
     *        copy the implementation may keep
     * @param payload the envelope payload (the encoded inner message; empty for the closing frame), read-only; the
     *        implementation may consume it
     * @return the non-empty signature, at most {@link HeaderValue#MAX_VALUE_LENGTH} bytes
     * @throws RuntimeException if the signature cannot be computed; the signer reports it as an
     *         {@link EventStreamSigningException}
     */
    byte[] sign(byte[] priorSignature, Instant signingTime, byte[] headers, ByteBuffer payload);
}
