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

package software.amazon.awssdk.tools.paritymanifest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** The launch-surface category of every kind of entry ID (task 13.3). */
class LaunchCategoryTest {
    private static final String S3 = "api:software.amazon.awssdk.services.s3.";

    static Stream<Arguments> ids() {
        return Stream.of(
            Arguments.of("service:s3", LaunchCategory.SERVICE),
            Arguments.of("http-mode:netty-nio-client:async", LaunchCategory.HTTP_MODE),
            Arguments.of("artifact:software.amazon.awssdk:s3", LaunchCategory.ARTIFACT),
            Arguments.of(S3 + "S3Client", LaunchCategory.CLIENT),
            Arguments.of(S3 + "S3AsyncClientBuilder#region(Lsoftware/amazon/awssdk/regions/Region;)V",
                         LaunchCategory.CLIENT),
            Arguments.of(S3 + "S3Utilities", LaunchCategory.UTILITY),
            Arguments.of(S3 + "model.GetObjectRequest", LaunchCategory.MODEL),
            Arguments.of(S3 + "model.NoSuchKeyException", LaunchCategory.EXCEPTION),
            Arguments.of(S3 + "model.SelectObjectContentEventStream", LaunchCategory.EVENT_STREAM),
            Arguments.of(S3 + "model.SelectObjectContentResponseHandler$Visitor", LaunchCategory.EVENT_STREAM),
            Arguments.of(S3 + "paginators.ListObjectsV2Iterable", LaunchCategory.PAGINATOR),
            Arguments.of(S3 + "waiters.S3Waiter", LaunchCategory.WAITER),
            Arguments.of(S3 + "presigner.S3Presigner", LaunchCategory.PRESIGNER),
            Arguments.of(S3 + "endpoints.S3EndpointProvider", LaunchCategory.ENDPOINT_PROVIDER),
            Arguments.of(S3 + "S3Configuration", LaunchCategory.CUSTOMIZATION),
            Arguments.of(S3 + "multipart.MultipartConfiguration", LaunchCategory.CUSTOMIZATION),
            Arguments.of("api:software.amazon.awssdk.core.sync.RequestBody", LaunchCategory.STREAMING),
            Arguments.of("api:software.amazon.awssdk.core.checksums.Algorithm", LaunchCategory.CHECKSUM),
            Arguments.of("api:software.amazon.awssdk.core.CompressionConfiguration", LaunchCategory.COMPRESSION),
            Arguments.of("api:software.amazon.awssdk.regions.Region", LaunchCategory.REGION),
            Arguments.of("api:software.amazon.awssdk.profiles.ProfileFile", LaunchCategory.PROFILE),
            Arguments.of("api:software.amazon.awssdk.auth.credentials.AwsCredentials", LaunchCategory.CREDENTIALS),
            Arguments.of("api:software.amazon.awssdk.metrics.MetricPublisher", LaunchCategory.TELEMETRY),
            Arguments.of("api:software.amazon.awssdk.utils.Logger", LaunchCategory.LOGGING),
            Arguments.of("api:software.amazon.awssdk.http.apache.ApacheHttpClient", LaunchCategory.HTTP_MODE),
            Arguments.of("api:software.amazon.awssdk.core.retry.RetryPolicy", LaunchCategory.OTHER),
            Arguments.of("behavior:module-path.support", LaunchCategory.MODULE_PATH),
            Arguments.of("behavior:native-image.support", LaunchCategory.NATIVE_IMAGE),
            Arguments.of("behavior:telemetry.user-agent", LaunchCategory.TELEMETRY),
            Arguments.of("behavior:retry.standard-strategy", LaunchCategory.OTHER),
            Arguments.of("behavior:service-defaults.timeouts", LaunchCategory.CONFIGURATION),
            Arguments.of("config:env:AWS_REQUEST_CHECKSUM_CALCULATION", LaunchCategory.CHECKSUM),
            Arguments.of("config:profile:disable_request_compression", LaunchCategory.COMPRESSION),
            Arguments.of("config:env:AWS_ACCESS_KEY_ID", LaunchCategory.CREDENTIALS),
            Arguments.of("config:sysprop:aws.region", LaunchCategory.REGION),
            Arguments.of("config:env:AWS_CONFIG_FILE", LaunchCategory.PROFILE),
            Arguments.of("config:env:AWS_USE_FIPS_ENDPOINT", LaunchCategory.ENDPOINT_PROVIDER),
            Arguments.of("config:env:AWS_RETRY_MODE", LaunchCategory.CONFIGURATION));
    }

    @ParameterizedTest
    @MethodSource("ids")
    void categorizesByIdAlone(String id, LaunchCategory category) {
        assertEquals(category, LaunchCategory.of(EntryKind.ofId(id).orElseThrow(), id));
    }
}
