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

package software.amazon.awssdk.pipeline;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import software.amazon.awssdk.auth.api.AuthSchemeOption;
import software.amazon.awssdk.http.SdkHttpRequest;

/**
 * How {@link AwsCallEngine#presign} signs a call it does not transmit (design "Paginators, waiters, presigners, and
 * utilities"; Requirement 18.3): the signature's validity and the Modern Auth option adjustment that makes the
 * selected scheme sign for presigning, for example query-string placement with an expiration for SigV4. The pipeline
 * is auth-scheme neutral and reads no clock; the presigner of a service supplies the adjustment of the schemes it
 * presigns with, and how a signed request records when it expires.
 *
 * @param expiration how long the presigned request is valid; positive
 * @param authOption adjusts the selected auth scheme option's signer properties before signing
 * @param expirationOf when a request this spec signed expires, as its signature records it (for SigV4,
 *     {@code X-Amz-Date} plus {@code X-Amz-Expires})
 */
public record PresignSpec(Duration expiration, UnaryOperator<AuthSchemeOption> authOption,
                          Function<SdkHttpRequest, Instant> expirationOf) {
    public PresignSpec {
        Objects.requireNonNull(expiration, "expiration");
        Objects.requireNonNull(authOption, "authOption");
        Objects.requireNonNull(expirationOf, "expirationOf");
        if (expiration.isZero() || expiration.isNegative()) {
            throw new IllegalArgumentException("The presign expiration must be positive, but was " + expiration);
        }
    }
}
