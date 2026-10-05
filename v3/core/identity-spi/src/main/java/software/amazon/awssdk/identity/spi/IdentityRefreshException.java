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

import java.util.Objects;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * A required identity refresh failed (Requirement 13.19): the cached identity was missing, expired, stale or rejected,
 * and loading a new one failed. The {@linkplain #getCause() cause} is the load failure. No request may be signed or
 * transmitted with the previous identity.
 */
public final class IdentityRefreshException extends SdkClientException {
    private static final long serialVersionUID = 1L;

    private final String providerName;

    private IdentityRefreshException(Builder builder, String providerName) {
        super(builder);
        this.providerName = providerName;
    }

    /** The refresh of {@code providerName}'s identity failed with {@code cause}. */
    public static IdentityRefreshException create(String providerName, Throwable cause) {
        Objects.requireNonNull(providerName, "providerName");
        Objects.requireNonNull(cause, "cause");
        String detail = cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
        Builder builder = SdkClientException.builder()
                                            .message("Unable to refresh the identity of " + providerName + ": " + detail)
                                            .cause(cause);
        return new IdentityRefreshException(builder, providerName);
    }

    /** Name of the provider whose refresh failed. */
    public String providerName() {
        return providerName;
    }
}
