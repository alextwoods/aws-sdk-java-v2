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

package software.amazon.awssdk.core.internal.util;

import static software.amazon.awssdk.core.internal.util.HttpChecksumUtils.getAlgorithmChecksumValuePair;

import java.io.InputStream;
import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;
import software.amazon.awssdk.core.checksums.ChecksumSpecs;
import software.amazon.awssdk.core.checksums.ChecksumValidation;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.internal.async.ChecksumValidatingPublisher;
import software.amazon.awssdk.core.internal.io.ChecksumValidatingInputStream;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.Pair;

/**
 * Flexible-checksum validation of a response body: when the operation asked for response validation and the response
 * carries a checksum header for a supported algorithm, the body is wrapped in a stream (or publisher) that verifies the
 * checksum as it is consumed, and the outcome is recorded in the execution attributes.
 *
 * <p>This used to be an {@link software.amazon.awssdk.core.interceptor.ExecutionInterceptor} registered on every client.
 * It is now called directly by the pipeline, at the same point in the response path (after every configured interceptor's
 * {@code modifyHttpResponse}, which is where it sat in the chain), so that a client with no interceptors of its own has an
 * empty interceptor chain.
 */
@SdkInternalApi
public final class ResponseChecksumValidation {
    private ResponseChecksumValidation() {
    }

    /**
     * The body to hand to the unmarshaller: {@code responseBody} wrapped for validation if this response should be validated,
     * otherwise {@code responseBody} itself.
     */
    public static InputStream validating(SdkHttpResponse httpResponse, InputStream responseBody,
                                         ExecutionAttributes executionAttributes) {
        Pair<ChecksumAlgorithm, String> algorithmChecksumPair = resolve(httpResponse, executionAttributes);
        if (algorithmChecksumPair != null && responseBody != null) {
            return new ChecksumValidatingInputStream(responseBody,
                                                     SdkChecksum.forAlgorithm(algorithmChecksumPair.left()),
                                                     algorithmChecksumPair.right());
        }
        return responseBody;
    }

    /**
     * The publisher to hand to the unmarshaller: {@code responsePublisher} wrapped for validation if this response should be
     * validated, otherwise {@code responsePublisher} itself.
     */
    public static Publisher<ByteBuffer> validating(SdkHttpResponse httpResponse, Publisher<ByteBuffer> responsePublisher,
                                                   ExecutionAttributes executionAttributes) {
        Pair<ChecksumAlgorithm, String> algorithmChecksumPair = resolve(httpResponse, executionAttributes);
        if (algorithmChecksumPair != null && responsePublisher != null) {
            return new ChecksumValidatingPublisher(responsePublisher,
                                                   SdkChecksum.forAlgorithm(algorithmChecksumPair.left()),
                                                   algorithmChecksumPair.right());
        }
        return responsePublisher;
    }

    /**
     * The algorithm and expected value to validate against, or {@code null} if this response is not to be validated. Records
     * the validation status in the execution attributes when validation was requested.
     */
    private static Pair<ChecksumAlgorithm, String> resolve(SdkHttpResponse httpResponse,
                                                           ExecutionAttributes executionAttributes) {
        ChecksumSpecs resolvedChecksumSpecs = HttpChecksumResolver.getResolvedChecksumSpecs(executionAttributes);
        if (resolvedChecksumSpecs == null || !isFlexibleChecksumValidationForResponse(executionAttributes,
                                                                                       resolvedChecksumSpecs)) {
            return null;
        }

        Pair<ChecksumAlgorithm, String> algorithmChecksumPair =
            getAlgorithmChecksumValuePair(httpResponse, resolvedChecksumSpecs);
        updateContextWithChecksumValidationStatus(executionAttributes, algorithmChecksumPair);
        return algorithmChecksumPair;
    }

    private static void updateContextWithChecksumValidationStatus(ExecutionAttributes executionAttributes,
                                                                  Pair<ChecksumAlgorithm, String> algorithmChecksumPair) {
        if (algorithmChecksumPair == null || algorithmChecksumPair.left() == null) {
            executionAttributes.putAttribute(SdkExecutionAttribute.HTTP_RESPONSE_CHECKSUM_VALIDATION,
                                             ChecksumValidation.CHECKSUM_ALGORITHM_NOT_FOUND);
        } else {
            executionAttributes.putAttribute(SdkExecutionAttribute.HTTP_RESPONSE_CHECKSUM_VALIDATION,
                                             ChecksumValidation.VALIDATED);
            executionAttributes.putAttribute(SdkExecutionAttribute.HTTP_CHECKSUM_VALIDATION_ALGORITHM_V2,
                                             algorithmChecksumPair.left());
        }
    }

    private static boolean isFlexibleChecksumValidationForResponse(ExecutionAttributes executionAttributes,
                                                                   ChecksumSpecs checksumSpecs) {
        return HttpChecksumUtils.isHttpChecksumValidationEnabled(checksumSpecs) &&
               !ChecksumValidation.FORCE_SKIP.equals(
                   executionAttributes.getAttribute(SdkExecutionAttribute.HTTP_RESPONSE_CHECKSUM_VALIDATION));
    }
}
