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
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;
import java.util.function.UnaryOperator;
import software.amazon.awssdk.auth.api.AuthSchemeOption;
import software.amazon.awssdk.auth.aws.AwsV4FamilyHttpSigner.AuthLocation;
import software.amazon.awssdk.auth.aws.internal.V4RequestSigning;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.HttpQueryParameter;
import software.amazon.awssdk.http.SdkHttpRequest;

/**
 * The Modern Auth replacement of V2's legacy presigner signing (Requirements 18.2, 18.3): the adjustment that makes a
 * selected SigV4-family auth scheme option (SigV4, SigV4a, S3 Express) sign into the query string with an expiration.
 * A generated service presigner applies it to the option its client's auth-scheme selection chose, so presigning
 * uses the same scheme, identity and signer properties as a call. There is no signer override.
 */
public final class AwsV4Presigning {
    private static final DateTimeFormatter AMZ_DATE =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.ROOT).withZone(ZoneOffset.UTC);

    private AwsV4Presigning() {
    }

    /**
     * When a SigV4-family presigned request expires, as its signature records it: {@code X-Amz-Date} plus
     * {@code X-Amz-Expires} seconds.
     *
     * @throws SdkClientException if the request is not a SigV4-family presigned request
     */
    public static Instant expiration(SdkHttpRequest presigned) {
        String date = null;
        String expires = null;
        for (HttpQueryParameter parameter : presigned.queryParameters()) {
            if ("X-Amz-Date".equals(parameter.rawName())) {
                date = parameter.rawValue();
            } else if ("X-Amz-Expires".equals(parameter.rawName())) {
                expires = parameter.rawValue();
            }
        }
        if (date == null || expires == null) {
            throw SdkClientException.create("The request is not SigV4 presigned: it has no X-Amz-Date and "
                                            + "X-Amz-Expires query parameters");
        }
        try {
            return Instant.from(AMZ_DATE.parse(date)).plusSeconds(Long.parseLong(expires));
        } catch (DateTimeParseException | NumberFormatException e) {
            throw SdkClientException.create("The request's SigV4 presign parameters are malformed", e);
        }
    }

    /**
     * The query-string presign adjustment.
     *
     * @param expiration how long the signature is valid: positive and at most 7 days (the SigV4 bound)
     * @param signPayload whether the payload hash is signed; false signs {@code UNSIGNED-PAYLOAD}, as S3 presigning
     *     does, so the sender may supply any payload
     * @throws SdkClientException if the expiration is out of bounds
     */
    public static UnaryOperator<AuthSchemeOption> queryString(Duration expiration, boolean signPayload) {
        Objects.requireNonNull(expiration, "expiration");
        if (expiration.isZero() || expiration.isNegative()
            || expiration.compareTo(V4RequestSigning.MAX_EXPIRATION) > 0) {
            throw SdkClientException.create("The presigned request expiration must be positive and at most 7 days, "
                                            + "but was " + expiration + ".");
        }
        return option -> option.toBuilder()
                               .putSignerProperty(AwsV4FamilyHttpSigner.AUTH_LOCATION, AuthLocation.QUERY_STRING)
                               .putSignerProperty(AwsV4FamilyHttpSigner.EXPIRATION_DURATION, expiration)
                               .putSignerProperty(AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED, signPayload)
                               .build();
    }

    /**
     * The S3 query-string presign adjustment, as V2's {@code S3Presigner} signs: {@link #queryString} with an
     * unsigned payload, and S3's canonical URI rules (the encoded key is neither encoded again nor normalized).
     *
     * @param expiration how long the signature is valid: positive and at most 7 days
     * @throws SdkClientException if the expiration is out of bounds
     */
    public static UnaryOperator<AuthSchemeOption> s3QueryString(Duration expiration) {
        UnaryOperator<AuthSchemeOption> query = queryString(expiration, false);
        return option -> query.apply(option).toBuilder()
                              .putSignerProperty(AwsV4FamilyHttpSigner.DOUBLE_URL_ENCODE, false)
                              .putSignerProperty(AwsV4FamilyHttpSigner.NORMALIZE_PATH, false)
                              .build();
    }
}
