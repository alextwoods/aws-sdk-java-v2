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
import java.util.Optional;
import software.amazon.awssdk.identity.spi.internal.DefaultAwsCredentialsIdentity;

/**
 * AWS access-key credentials, with the V2 name and members.
 *
 * <p>The instances {@link #create} and {@link #builder()} return are immutable, equal when access key ID, secret access
 * key and account ID are equal, and never render the secret access key in {@code toString} (Requirement 21.10).
 */
public interface AwsCredentialsIdentity extends Identity {
    /** The AWS access key ID. */
    String accessKeyId();

    /** The AWS secret access key. Secret: never log it. */
    String secretAccessKey();

    /** The AWS account ID these credentials belong to, if known. */
    default Optional<String> accountId() {
        return Optional.empty();
    }

    static Builder builder() {
        return DefaultAwsCredentialsIdentity.builder();
    }

    /**
     * Credentials with {@code accessKeyId} and {@code secretAccessKey}.
     *
     * @throws NullPointerException if either value is null
     */
    static AwsCredentialsIdentity create(String accessKeyId, String secretAccessKey) {
        return builder().accessKeyId(accessKeyId)
                        .secretAccessKey(secretAccessKey)
                        .build();
    }

    /** Builder of an {@link AwsCredentialsIdentity}. */
    interface Builder {
        Builder accessKeyId(String accessKeyId);

        Builder secretAccessKey(String secretAccessKey);

        Builder accountId(String accountId);

        Builder providerName(String providerName);

        /** When the credentials expire; {@code null} (the default) if they do not or it is unknown. */
        Builder expirationTime(Instant expirationTime);

        /**
         * The credentials.
         *
         * @throws NullPointerException if the access key ID or secret access key is not set
         */
        AwsCredentialsIdentity build();
    }
}
