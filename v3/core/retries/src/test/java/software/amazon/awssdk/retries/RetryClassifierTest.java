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

package software.amazon.awssdk.retries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static software.amazon.awssdk.retries.RetryClassification.NON_RETRYABLE;
import static software.amazon.awssdk.retries.RetryClassification.THROTTLING;
import static software.amazon.awssdk.retries.RetryClassification.TRANSIENT;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.exception.SdkTimeoutException;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.SdkHttpResponse;

class RetryClassifierTest {
    private static final RetryClassifier CLASSIFIER = RetryClassifier.defaults();

    private static RetryClassification classify(AttemptFailure.Builder failure) {
        return CLASSIFIER.classify(failure.build());
    }

    private static SdkServiceException service(int status) {
        return SdkServiceException.builder().message("boom").statusCode(status).build();
    }

    @Test
    void modeledTraitsComeFromTheErrorSchema() {
        assertEquals(ModeledRetryTraits.NONE, ModeledRetryTraits.fromSchema(ModeledTestError.PLAIN));
        assertEquals(new ModeledRetryTraits(true, false), ModeledRetryTraits.fromSchema(ModeledTestError.RETRYABLE));
        assertEquals(new ModeledRetryTraits(true, true), ModeledRetryTraits.fromSchema(ModeledTestError.THROTTLING));
        assertEquals(new ModeledRetryTraits(true, true),
                     AttemptFailure.of(ModeledTestError.of(ModeledTestError.THROTTLING, 400)).modeledTraits());
        assertEquals(ModeledRetryTraits.NONE, AttemptFailure.of(new IllegalStateException()).modeledTraits());
        assertThrows(IllegalArgumentException.class, () -> new ModeledRetryTraits(false, true));
    }

    @Test
    void modeledTraitsAloneClassifyAModeledError() {
        assertEquals(NON_RETRYABLE, CLASSIFIER.classify(AttemptFailure.of(
            ModeledTestError.of(ModeledTestError.PLAIN, 400))));
        assertEquals(TRANSIENT, CLASSIFIER.classify(AttemptFailure.of(
            ModeledTestError.of(ModeledTestError.RETRYABLE, 400))));
        assertEquals(THROTTLING, CLASSIFIER.classify(AttemptFailure.of(
            ModeledTestError.of(ModeledTestError.THROTTLING, 400))));
    }

    @Test
    void responseMetadataCombinesWithModeledTraits() {
        // A @retryable (non-throttling) error answered with a throttling code or status is throttling.
        ModeledTestError retryable = ModeledTestError.of(ModeledTestError.RETRYABLE, 400);
        assertEquals(THROTTLING, classify(AttemptFailure.builder().failure(retryable).errorCode("SlowDown")));
        assertEquals(THROTTLING, classify(AttemptFailure.builder().failure(retryable).statusCode(429)));
        // An error without traits is retryable when the metadata says so.
        ModeledTestError plain = ModeledTestError.of(ModeledTestError.PLAIN, 503);
        assertEquals(TRANSIENT, CLASSIFIER.classify(AttemptFailure.of(plain)));
        assertEquals(TRANSIENT, classify(AttemptFailure.builder().failure(ModeledTestError.of(ModeledTestError.PLAIN,
                                                                                              400))
                                                        .errorCode("RequestTimeout")));
        // Explicit traits override derivation from the failure.
        assertEquals(NON_RETRYABLE, classify(AttemptFailure.builder().failure(ModeledTestError.of(
            ModeledTestError.RETRYABLE, 400)).modeledTraits(ModeledRetryTraits.NONE)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Throttling", "ThrottlingException", "ThrottledException", "RequestThrottledException",
                            "TooManyRequestsException", "ProvisionedThroughputExceededException",
                            "TransactionInProgressException", "RequestLimitExceeded", "BandwidthLimitExceeded",
                            "LimitExceededException", "RequestThrottled", "SlowDown", "EC2ThrottledException"})
    void throttlingErrorCodes(String code) {
        assertEquals(THROTTLING, classify(AttemptFailure.builder().failure(service(400)).errorCode(code)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"RequestTimeout", "RequestTimeoutException", "PriorRequestNotComplete", "InternalError",
                            "RequestTimeTooSkewed", "RequestExpired", "RequestInTheFuture"})
    void transientErrorCodes(String code) {
        assertEquals(TRANSIENT, classify(AttemptFailure.builder().failure(service(400)).errorCode(code)));
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 502, 503, 504})
    void transientStatusCodes(int status) {
        assertEquals(TRANSIENT, CLASSIFIER.classify(AttemptFailure.of(service(status))));
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 403, 404, 501})
    void otherStatusCodesAreNotRetryable(int status) {
        assertEquals(NON_RETRYABLE, CLASSIFIER.classify(AttemptFailure.of(service(status))));
    }

    @Test
    void statusCodeComesFromTheResponseOrTheServiceException() {
        assertEquals(THROTTLING, CLASSIFIER.classify(AttemptFailure.of(service(429))));
        SdkHttpResponse response = SdkHttpResponse.builder().statusCode(503).addHeader("x-amz-retry-after", "1500")
                                                  .build();
        AttemptFailure failure = AttemptFailure.builder().failure(new IllegalStateException()).response(response)
                                               .build();
        assertEquals(503, failure.statusCode().getAsInt());
        assertEquals("1500", failure.retryAfterHint().orElseThrow());
        assertEquals(TRANSIENT, CLASSIFIER.classify(failure));
    }

    @ParameterizedTest
    @EnumSource(HttpFailureCategory.class)
    void transportFailuresFollowTheirStableCategory(HttpFailureCategory category) {
        HttpTransportException transport = HttpTransportException.builder().category(category)
                                                                 .phase(HttpPhase.RESPONSE_CONSUMPTION).build();
        RetryClassification expected = category.transientByDefault() ? TRANSIENT : NON_RETRYABLE;
        assertEquals(expected, CLASSIFIER.classify(AttemptFailure.of(transport)));
        // Also when wrapped by a later stage.
        assertEquals(expected, CLASSIFIER.classify(AttemptFailure.of(new RuntimeException(transport))));
    }

    @Test
    void transientCategoriesMatchTheDocumentedTable() {
        for (HttpFailureCategory c : new HttpFailureCategory[] {HttpFailureCategory.CONNECTION_ACQUISITION,
                                                               HttpFailureCategory.CONNECT,
                                                               HttpFailureCategory.TIMEOUT,
                                                               HttpFailureCategory.REQUEST_TRANSMISSION,
                                                               HttpFailureCategory.RESPONSE_CONSUMPTION}) {
            assertEquals(true, c.transientByDefault(), c.name());
        }
        for (HttpFailureCategory c : new HttpFailureCategory[] {HttpFailureCategory.PROXY, HttpFailureCategory.TLS,
                                                               HttpFailureCategory.CANCELLED,
                                                               HttpFailureCategory.PROTOCOL}) {
            assertEquals(false, c.transientByDefault(), c.name());
        }
    }

    @Test
    void timeoutsIoAndCancellation() {
        SdkTimeoutException attempt = SdkTimeoutException.builder().scope(SdkTimeoutException.Scope.API_CALL_ATTEMPT)
                                                         .build();
        SdkTimeoutException call = SdkTimeoutException.builder().scope(SdkTimeoutException.Scope.API_CALL).build();
        assertEquals(TRANSIENT, CLASSIFIER.classify(AttemptFailure.of(attempt)));
        assertEquals(NON_RETRYABLE, CLASSIFIER.classify(AttemptFailure.of(call)));
        assertEquals(TRANSIENT, CLASSIFIER.classify(AttemptFailure.of(new UncheckedIOException(new IOException()))));
        assertEquals(TRANSIENT, CLASSIFIER.classify(AttemptFailure.of(SdkClientException.create("x",
                                                                                                new IOException()))));
        assertEquals(NON_RETRYABLE, CLASSIFIER.classify(AttemptFailure.of(new CancellationException())));
        assertEquals(NON_RETRYABLE, CLASSIFIER.classify(AttemptFailure.of(
            SdkClientException.create("x", new InterruptedException()))));
        assertEquals(NON_RETRYABLE, CLASSIFIER.classify(AttemptFailure.of(new IllegalArgumentException())));
    }

    @Test
    void cancellationAndNonReplayableBodiesWinOverRetryableMetadata() {
        assertEquals(NON_RETRYABLE, classify(AttemptFailure.builder().failure(new CancellationException())
                                                           .statusCode(503)));
        assertEquals(NON_RETRYABLE, classify(AttemptFailure.builder().failure(service(503))
                                                           .requestReplayable(false)));
        assertEquals(NON_RETRYABLE, classify(AttemptFailure.builder()
                                                           .failure(ModeledTestError.of(ModeledTestError.THROTTLING,
                                                                                        429))
                                                           .requestReplayable(false)));
    }

    @Test
    void cyclicCauseChainsTerminate() {
        RuntimeException a = new RuntimeException("a");
        RuntimeException b = new RuntimeException("b", a);
        a.initCause(b);
        assertEquals(NON_RETRYABLE, CLASSIFIER.classify(AttemptFailure.of(a)));
    }

    @Test
    void attemptFailureValidation() {
        assertThrows(NullPointerException.class, () -> AttemptFailure.builder().build());
        assertThrows(IllegalArgumentException.class, () -> AttemptFailure.builder().statusCode(42));
        assertEquals(false, AttemptFailure.of(new RuntimeException()).statusCode().isPresent());
    }
}
