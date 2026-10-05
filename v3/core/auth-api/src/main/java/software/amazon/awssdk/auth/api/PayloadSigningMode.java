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

/**
 * How a signer covers the payload of one request, which it reports through
 * {@link HttpSigner#payloadSigningMode(SignRequest)} before signing, so the pipeline can prepare the payload without
 * the signer ever reading a streaming body (V2 {@code DefaultAwsV4HttpSigner}'s payload signer selection; Smithy
 * {@code httpChecksum} "Resolving checksum location").
 */
public enum PayloadSigningMode {
    /**
     * The signature covers a SHA-256 hash of the whole payload ("header-based auth"). The signer hashes an in-memory
     * payload itself. For a streaming payload the pipeline computes the hash before signing by reading the body once,
     * which requires a replayable body, and passes it as {@link HttpSigner#PAYLOAD_SHA256}; a request checksum of
     * the payload goes in a header.
     */
    PAYLOAD_HASH,

    /**
     * The signature covers no payload bytes ("unsigned payload auth", for example SigV4 {@code UNSIGNED-PAYLOAD}
     * over HTTPS). The body is never read before transmission; a request checksum of a streaming payload goes in an
     * unsigned trailer.
     */
    UNSIGNED
}
