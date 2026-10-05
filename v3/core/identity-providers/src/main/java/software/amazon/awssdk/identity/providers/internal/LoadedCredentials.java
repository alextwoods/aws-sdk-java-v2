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

package software.amazon.awssdk.identity.providers.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;

/**
 * Builds credentials from the JSON credential documents of instance metadata, container endpoints and credential
 * processes, and parses their timestamps. Not a public API.
 */
public final class LoadedCredentials {
    private LoadedCredentials() {
    }

    /**
     * Credentials from a document with {@code AccessKeyId}, {@code SecretAccessKey}, an optional session token member
     * named {@code tokenMember}, optional {@code AccountId} (or {@code fallbackAccountId}) and optional ISO-8601
     * {@code Expiration}.
     *
     * @throws SdkClientException naming {@code source} if a required member is missing
     */
    public static AwsCredentialsIdentity credentials(Map<String, Object> document, String tokenMember,
                                                     String fallbackAccountId, String providerName, String source) {
        Instant expiration = instant(Json.string(document, "Expiration"), source);
        String accessKeyId = Json.string(document, "AccessKeyId");
        String secretAccessKey = Json.string(document, "SecretAccessKey");
        if (accessKeyId == null || secretAccessKey == null) {
            throw SdkClientException.create("The credentials from " + source + " are missing AccessKeyId or "
                                            + "SecretAccessKey.");
        }
        String token = Json.string(document, tokenMember);
        String accountId = Json.string(document, "AccountId");
        if (accountId == null) {
            accountId = fallbackAccountId;
        }
        if (token == null) {
            return AwsCredentialsIdentity.builder().accessKeyId(accessKeyId).secretAccessKey(secretAccessKey)
                                         .accountId(accountId).providerName(providerName).expirationTime(expiration)
                                         .build();
        }
        return session(accessKeyId, secretAccessKey, token, accountId, expiration, providerName);
    }

    /** Session credentials. */
    public static AwsSessionCredentialsIdentity session(String accessKeyId, String secretAccessKey, String token,
                                                        String accountId, Instant expiration, String providerName) {
        return AwsSessionCredentialsIdentity.builder().accessKeyId(accessKeyId).secretAccessKey(secretAccessKey)
                                            .sessionToken(token).accountId(accountId).providerName(providerName)
                                            .expirationTime(expiration).build();
    }

    /**
     * Parses an ISO-8601 instant, with or without an offset ({@code Z} assumed).
     *
     * @throws SdkClientException naming {@code source} if it is not a timestamp
     */
    public static Instant instant(String value, String source) {
        if (value == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException e) {
            try {
                return Instant.parse(value.endsWith("Z") ? value : value + "Z");
            } catch (DateTimeParseException e2) {
                throw SdkClientException.create("Unable to parse the credential expiration from " + source + ".", e2);
            }
        }
    }

    /** An instant from epoch milliseconds (SSO {@code expiration}). */
    public static Instant epochMillis(Object value) {
        if (value instanceof BigDecimal number) {
            return Instant.ofEpochMilli(number.longValue());
        }
        return value == null ? null : Instant.ofEpochMilli(Long.parseLong(value.toString()));
    }
}
