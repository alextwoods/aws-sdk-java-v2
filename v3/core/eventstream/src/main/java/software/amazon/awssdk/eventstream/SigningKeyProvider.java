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

import java.time.LocalDate;

/**
 * The identity seam of SigV4 event stream signing: the SigV4 signing key ({@code kSigning}) of the stream's
 * credentials for a credential-scope date. The SigV4 implementation in {@code auth-aws} provides it from the identity
 * that signed the initial request, so that event stream signing never sees the secret key and does not duplicate the
 * SigV4 key derivation:
 *
 * <pre>
 * kSigning = HMAC-SHA256(HMAC-SHA256(HMAC-SHA256(HMAC-SHA256("AWS4" + secretAccessKey, yyyyMMdd), region),
 *                                    service), "aws4_request")
 * </pre>
 *
 * with the region and service of the {@link SigV4FrameSignatureAlgorithm}. A stream may outlive a UTC day, so the
 * key is requested per scope date; the algorithm caches the key of the current date.
 *
 * <p>Invoked from one thread at a time. A failure (for example expired credentials) is reported as an
 * {@link EventStreamSigningException} and fails the stream.
 */
@FunctionalInterface
public interface SigningKeyProvider {
    /**
     * The signing key for the credential scope {@code scopeDate/region/service/aws4_request}; the caller keeps and
     * never modifies the returned array.
     */
    byte[] signingKey(LocalDate scopeDate);
}
