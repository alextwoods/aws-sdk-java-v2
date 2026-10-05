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

package software.amazon.awssdk.auth.aws;

import java.time.Duration;
import java.util.Optional;
import software.amazon.awssdk.auth.api.HttpSigner;
import software.amazon.awssdk.auth.api.PayloadSigningMode;
import software.amazon.awssdk.auth.api.SignRequest;
import software.amazon.awssdk.auth.api.SignerProperty;
import software.amazon.awssdk.auth.aws.internal.V4RequestSigning;
import software.amazon.awssdk.identity.spi.Identity;

/**
 * The signer properties shared by the SigV4 family (SigV4, SigV4a, S3 Express), with the V2 names and defaults.
 *
 * <p>Payload signing (V2 {@code DefaultAwsV4HttpSigner}): the payload hash in {@code x-amz-content-sha256} is a
 * precomputed value the request already carries (an aws-chunked streaming value, for example), the event stream value
 * for an event stream, {@code UNSIGNED-PAYLOAD} when {@link #PAYLOAD_SIGNING_ENABLED} is false over HTTPS (or the
 * payload is empty), and otherwise the SHA-256 of the payload: computed here for an in-memory payload, and for a
 * streaming payload the {@link HttpSigner#PAYLOAD_SHA256} the pipeline supplies, as
 * {@link #payloadSigningMode} reports {@link PayloadSigningMode#PAYLOAD_HASH}.
 *
 * @param <IdentityT> the identity type
 */
public interface AwsV4FamilyHttpSigner<IdentityT extends Identity> extends HttpSigner<IdentityT> {
    /** The service signing name (the {@code name} of the modeled {@code aws.auth#sigv4} trait). Required. */
    SignerProperty<String> SERVICE_SIGNING_NAME = SignerProperty.create(AwsV4FamilyHttpSigner.class,
                                                                        "ServiceSigningName");

    /** Whether the canonical URI encodes the (already encoded) path again. Default {@code true}; S3 uses false. */
    SignerProperty<Boolean> DOUBLE_URL_ENCODE = SignerProperty.create(AwsV4FamilyHttpSigner.class, "DoubleUrlEncode");

    /** Whether the canonical URI removes dot segments from the path. Default {@code true}; S3 uses false. */
    SignerProperty<Boolean> NORMALIZE_PATH = SignerProperty.create(AwsV4FamilyHttpSigner.class, "NormalizePath");

    /** Where the signature goes. Default {@link AuthLocation#HEADER}; presigners use the query string. */
    SignerProperty<AuthLocation> AUTH_LOCATION = SignerProperty.create(AwsV4FamilyHttpSigner.class, "AuthLocation");

    /** How long a query-string (presigned) signature is valid; default 15 minutes, at most 7 days (V2 bound). */
    SignerProperty<Duration> EXPIRATION_DURATION = SignerProperty.create(AwsV4FamilyHttpSigner.class,
                                                                         "ExpirationDuration");

    /**
     * Whether the payload is hashed into the signature. Default {@code true}. When false over HTTPS the payload hash is
     * {@code UNSIGNED-PAYLOAD}; over plain HTTP a present payload is always signed (V2 behavior).
     */
    SignerProperty<Boolean> PAYLOAD_SIGNING_ENABLED = SignerProperty.create(AwsV4FamilyHttpSigner.class,
                                                                            "PayloadSigningEnabled");

    /**
     * Whether the payload is sent with aws-chunked encoding. Chunk framing and signing are applied by the pipeline's
     * payload stages, which set the streaming payload hash in {@code x-amz-content-sha256} before signing; the signer
     * then signs that value.
     */
    SignerProperty<Boolean> CHUNK_ENCODING_ENABLED = SignerProperty.create(AwsV4FamilyHttpSigner.class,
                                                                           "ChunkEncodingEnabled");

    /**
     * Empty if the request already carries its payload hash in {@code x-amz-content-sha256} or is an event stream;
     * otherwise {@link PayloadSigningMode#UNSIGNED} when {@link #PAYLOAD_SIGNING_ENABLED} is false and the request is
     * HTTPS or has no payload, else {@link PayloadSigningMode#PAYLOAD_HASH}.
     */
    @Override
    default Optional<PayloadSigningMode> payloadSigningMode(SignRequest<? extends IdentityT> request) {
        return V4RequestSigning.payloadSigningMode(request);
    }

    /** Where a SigV4-family signature is placed. */
    enum AuthLocation {
        /** The {@code Authorization} header. */
        HEADER,
        /** Query parameters ({@code X-Amz-Signature} and friends): presigned requests. */
        QUERY_STRING
    }
}
