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
import software.amazon.awssdk.identity.spi.internal.DefaultAwsSessionCredentialsIdentity;

/**
 * Temporary AWS credentials with a session token, with the V2 name and members.
 *
 * <p>The instances {@link #create} and {@link #builder()} return are immutable, equal when access key ID, secret access
 * key, session token and account ID are equal, and never render the secret access key or session token in
 * {@code toString} (Requirement 21.10).
 */
public interface AwsSessionCredentialsIdentity extends AwsCredentialsIdentity {
    /** The session token. Secret: never log it. */
    String sessionToken();

    static Builder builder() {
        return DefaultAwsSessionCredentialsIdentity.builder();
    }

    /**
     * Session credentials with {@code accessKeyId}, {@code secretAccessKey} and {@code sessionToken}.
     *
     * @throws NullPointerException if any value is null
     */
    static AwsSessionCredentialsIdentity create(String accessKeyId, String secretAccessKey, String sessionToken) {
        return builder().accessKeyId(accessKeyId)
                        .secretAccessKey(secretAccessKey)
                        .sessionToken(sessionToken)
                        .build();
    }

    /** Builder of an {@link AwsSessionCredentialsIdentity}. */
    interface Builder extends AwsCredentialsIdentity.Builder {
        @Override
        Builder accessKeyId(String accessKeyId);

        @Override
        Builder secretAccessKey(String secretAccessKey);

        @Override
        Builder accountId(String accountId);

        Builder sessionToken(String sessionToken);

        @Override
        Builder providerName(String providerName);

        @Override
        Builder expirationTime(Instant expirationTime);

        /**
         * The session credentials.
         *
         * @throws NullPointerException if the access key ID, secret access key or session token is not set
         */
        @Override
        AwsSessionCredentialsIdentity build();
    }
}
