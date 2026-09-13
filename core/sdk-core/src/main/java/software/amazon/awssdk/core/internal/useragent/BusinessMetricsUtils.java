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

package software.amazon.awssdk.core.internal.useragent;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.retries.AdaptiveRetryStrategy;
import software.amazon.awssdk.retries.LegacyRetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.RetryStrategy;

@SdkInternalApi
public final class BusinessMetricsUtils {
    private BusinessMetricsUtils() {
    }

    public static Optional<String> resolveRetryMode(RetryPolicy retryPolicy, RetryStrategy retryStrategy) {
        if (retryPolicy != null) {
            RetryMode retryMode = retryPolicy.retryMode();
            if (retryMode == RetryMode.STANDARD) {
                return Optional.of(BusinessMetricFeatureId.RETRY_MODE_STANDARD.value());
            }
            if (retryMode == RetryMode.LEGACY) {
                return Optional.of(BusinessMetricFeatureId.RETRY_MODE_LEGACY.value());
            } //TODO(business-metrics) Separate logic when feature id for ADAPTIVE is available
            if (retryMode == RetryMode.ADAPTIVE || retryMode == RetryMode.ADAPTIVE_V2) {
                return Optional.of(BusinessMetricFeatureId.RETRY_MODE_ADAPTIVE.value());
            }
        } else {
            if (retryStrategy instanceof StandardRetryStrategy) {
                return Optional.of(BusinessMetricFeatureId.RETRY_MODE_STANDARD.value());
            }
            if (retryStrategy instanceof LegacyRetryStrategy) {
                return Optional.of(BusinessMetricFeatureId.RETRY_MODE_LEGACY.value());
            }
            if (retryStrategy instanceof AdaptiveRetryStrategy) {
                return Optional.of(BusinessMetricFeatureId.RETRY_MODE_ADAPTIVE.value());
            }
        }
        return Optional.empty();
    }

    public static Optional<String> resolveRequestChecksumCalculationMetric(
        RequestChecksumCalculation requestChecksumCalculation) {
        if (requestChecksumCalculation == null) {
            return Optional.empty();
        }
        switch (requestChecksumCalculation) {
            case WHEN_SUPPORTED:
                return Optional.of(BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_WHEN_SUPPORTED.value());
            case WHEN_REQUIRED:
                return Optional.of(BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_WHEN_REQUIRED.value());
            default:
                return Optional.empty();
        }
    }

    public static Optional<String> resolveResponseChecksumValidationMetric(
        ResponseChecksumValidation responseChecksumValidation) {
        if (responseChecksumValidation == null) {
            return Optional.empty();
        }
        switch (responseChecksumValidation) {
            case WHEN_SUPPORTED:
                return Optional.of(BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_RES_WHEN_SUPPORTED.value());
            case WHEN_REQUIRED:
                return Optional.of(BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_RES_WHEN_REQUIRED.value());
            default:
                return Optional.empty();
        }
    }

    public static Set<String> resolveChecksumAlgorithmFeatureIds(ChecksumAlgorithm algorithm,
                                                                 SdkHttpFullRequest.Builder request) {
        Set<String> ids = new LinkedHashSet<>(8);
        addChecksumAlgorithmFeatureIds(algorithm, request, ids::add);
        return ids;
    }

    /**
     * Feed the checksum feature ids for this request — one per {@code x-amz-checksum-*} header the request carries, in
     * header order, then the one for the configured algorithm if it is not already among them — to {@code sink}.
     * Allocation-free apart from the scan's consumer; the ids themselves are shared constants.
     */
    public static void addChecksumAlgorithmFeatureIds(ChecksumAlgorithm algorithm,
                                                      SdkHttpFullRequest.Builder request,
                                                      Consumer<String> sink) {
        ChecksumHeaderScan scan = new ChecksumHeaderScan(sink);
        request.forEachHeader(scan);
        String algorithmId = resolveChecksumAlgorithmMetricOrNull(algorithm);
        if (algorithmId != null && !scan.seen(algorithmId)) {
            sink.accept(algorithmId);
        }
    }

    /**
     * Header scan that hands each recognised checksum header's feature id to the sink once. The recognised ids are the
     * closed set {@link #headerToChecksumFeatureId} maps to, so "seen" is a small array with room for all of them rather
     * than a set.
     */
    private static final class ChecksumHeaderScan implements BiConsumer<String, List<String>> {
        private static final int DISTINCT_CHECKSUM_FEATURE_IDS = 10;

        private final Consumer<String> sink;
        private final String[] seen = new String[DISTINCT_CHECKSUM_FEATURE_IDS];
        private int count;

        private ChecksumHeaderScan(Consumer<String> sink) {
            this.sink = sink;
        }

        @Override
        public void accept(String header, List<String> values) {
            String id = headerToChecksumFeatureId(header);
            if (id != null && !seen(id)) {
                seen[count++] = id;
                sink.accept(id);
            }
        }

        boolean seen(String id) {
            for (int i = 0; i < count; i++) {
                if (seen[i].equals(id)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static String resolveChecksumAlgorithmMetricOrNull(ChecksumAlgorithm algorithm) {
        if (algorithm == null) {
            return null;
        }

        String algorithmId = algorithm.algorithmId();
        if (algorithmId.equals(DefaultChecksumAlgorithm.CRC32.algorithmId())) {
            return BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_CRC32.value();
        }
        if (algorithmId.equals(DefaultChecksumAlgorithm.CRC32C.algorithmId())) {
            return BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_CRC32C.value();
        }
        if (algorithmId.equals(DefaultChecksumAlgorithm.CRC64NVME.algorithmId())) {
            return BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_CRC64.value();
        }
        if (algorithmId.equals(DefaultChecksumAlgorithm.SHA1.algorithmId())) {
            return BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_SHA1.value();
        }
        if (algorithmId.equals(DefaultChecksumAlgorithm.SHA256.algorithmId())) {
            return BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_SHA256.value();
        }

        if (algorithmId.equals(DefaultChecksumAlgorithm.SHA512.algorithmId())) {
            return BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_SHA512.value();
        }

        if (algorithmId.equals(DefaultChecksumAlgorithm.XXHASH3.algorithmId())) {
            return BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_XXHASH3.value();
        }

        if (algorithmId.equals(DefaultChecksumAlgorithm.XXHASH64.algorithmId())) {
            return BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_XXHASH64.value();
        }

        if (algorithmId.equals(DefaultChecksumAlgorithm.XXHASH128.algorithmId())) {
            return BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_XXHASH128.value();
        }

        if (algorithmId.equals(DefaultChecksumAlgorithm.MD5.algorithmId())) {
            return BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_MD5.value();
        }

        return null;
    }

    // pkg private for testing
    static String headerToChecksumFeatureId(String h) {
        switch (h) {
            case "x-amz-checksum-crc32":
                return BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_CRC32.value();
            case "x-amz-checksum-crc32c":
                return BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_CRC32C.value();
            case "x-amz-checksum-crc64nvme":
                return BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_CRC64.value();
            case "x-amz-checksum-sha256":
                return BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_SHA256.value();
            case "x-amz-checksum-sha512":
                return BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_SHA512.value();
            case "x-amz-checksum-sha1":
                return BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_SHA1.value();
            case "x-amz-checksum-md5":
                return BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_MD5.value();
            case "x-amz-checksum-xxhash64":
                return BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_XXHASH64.value();
            case "x-amz-checksum-xxhash3":
                return BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_XXHASH3.value();
            case "x-amz-checksum-xxhash128":
                return BusinessMetricFeatureId.FLEXIBLE_CHECKSUMS_REQ_XXHASH128.value();
            default:
                return null;
        }
    }

}
