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

package software.amazon.awssdk.auth.aws.s3express;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.utils.ToString;

/**
 * S3 Express One Zone session credentials returned by {@code CreateSession}, with the V2 name. Their session token is
 * sent in {@code x-amz-s3session-token} rather than {@code X-Amz-Security-Token}. Immutable; secrets are never
 * rendered.
 */
public final class S3ExpressSessionCredentials implements AwsSessionCredentialsIdentity {
    private final String accessKeyId;
    private final String secretAccessKey;
    private final String sessionToken;
    private final Instant expirationTime;

    private S3ExpressSessionCredentials(String accessKeyId, String secretAccessKey, String sessionToken,
                                        Instant expirationTime) {
        this.accessKeyId = Objects.requireNonNull(accessKeyId, "accessKeyId");
        this.secretAccessKey = Objects.requireNonNull(secretAccessKey, "secretAccessKey");
        this.sessionToken = Objects.requireNonNull(sessionToken, "sessionToken");
        this.expirationTime = expirationTime;
    }

    /** Session credentials expiring at {@code expirationTime} ({@code null} if unknown). */
    public static S3ExpressSessionCredentials create(String accessKeyId, String secretAccessKey, String sessionToken,
                                                     Instant expirationTime) {
        return new S3ExpressSessionCredentials(accessKeyId, secretAccessKey, sessionToken, expirationTime);
    }

    @Override
    public String accessKeyId() {
        return accessKeyId;
    }

    @Override
    public String secretAccessKey() {
        return secretAccessKey;
    }

    @Override
    public String sessionToken() {
        return sessionToken;
    }

    @Override
    public Optional<Instant> expirationTime() {
        return Optional.ofNullable(expirationTime);
    }

    @Override
    public Optional<String> providerName() {
        return Optional.of("S3ExpressIdentityProvider");
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof S3ExpressSessionCredentials that && accessKeyId.equals(that.accessKeyId)
               && secretAccessKey.equals(that.secretAccessKey) && sessionToken.equals(that.sessionToken)
               && Objects.equals(expirationTime, that.expirationTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessKeyId, secretAccessKey, sessionToken, expirationTime);
    }

    @Override
    public String toString() {
        return ToString.builder("S3ExpressSessionCredentials")
                       .add("accessKeyId", accessKeyId)
                       .add("expirationTime", expirationTime)
                       .build();
    }
}
