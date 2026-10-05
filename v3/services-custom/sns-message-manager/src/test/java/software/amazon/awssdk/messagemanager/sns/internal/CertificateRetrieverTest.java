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

package software.amazon.awssdk.messagemanager.sns.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.SdkHttpResponse;

class CertificateRetrieverTest {
    private static final String CERT_HOST = SnsTestFixtures.REGION_CERT_HOST;
    private static final String CERT_URL = SnsTestFixtures.CERT_URL;
    private static final String COMMON_NAME = "sns.amazonaws.com";

    private FakeSyncHttpClient http;
    private CertificateRetriever retriever;

    @BeforeEach
    void setUp() {
        http = new FakeSyncHttpClient();
        retriever = retriever(SnsTestFixtures.FIXED_CLOCK);
    }

    @Test
    void retrieveCertificate_validPemCertificate_returnsItsKey() {
        http.serve(CERT_URL, SnsTestFixtures.certificate("sns-global"));

        assertThat(retriever.retrieveCertificate(URI.create(CERT_URL))).isEqualTo(SnsTestFixtures.publicKey());
        assertThat(http.requests()).singleElement().satisfies(request -> {
            assertThat(request.method()).isEqualTo("GET");
            assertThat(request.scheme()).isEqualTo("https");
            assertThat(request.host()).isEqualTo(CERT_HOST);
            assertThat(request.encodedPath()).isEqualTo(URI.create(CERT_URL).getRawPath());
        });
    }

    @Test
    void retrieveCertificate_streamingResponseBody_isRead() {
        http.serveStreaming(CERT_URL, SnsTestFixtures.certificate("sns-global"));

        assertThat(retriever.retrieveCertificate(URI.create(CERT_URL))).isEqualTo(SnsTestFixtures.publicKey());
    }

    @Test
    void retrieveCertificate_nonSnsHost_isRejectedBeforeAnyRequest() {
        for (String url : List.of("https://evil.example/cert.pem", "http://" + CERT_HOST + "/cert.pem",
                                  "https://" + CERT_HOST + "@evil.example/cert.pem")) {
            assertThatThrownBy(() -> retriever.retrieveCertificate(URI.create(url)))
                .isInstanceOf(SdkClientException.class);
        }
        assertThat(http.requests()).isEmpty();
    }

    @Test
    void retrieveCertificate_nullUrl_throws() {
        assertThatThrownBy(() -> retriever.retrieveCertificate(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("certificateUrl must not be null.");
    }

    @Test
    void retrieveCertificate_httpError_throws() {
        http.respond(CERT_URL, () -> SdkHttpResponse.builder().statusCode(400).build());

        assertThatThrownBy(() -> retriever.retrieveCertificate(URI.create(CERT_URL)))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("Request was unsuccessful. Status Code: 400");
    }

    @Test
    void retrieveCertificate_transportFails_throwsWithCause() {
        HttpTransportException failure = HttpTransportException.create(HttpPhase.CONNECT,
                                                                        HttpFailureCategory.Kind.IO,
                                                                        "connection refused", null);
        http.respond(CERT_URL, () -> {
            throw failure;
        });

        assertThatThrownBy(() -> retriever.retrieveCertificate(URI.create(CERT_URL)))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("Unexpected error while retrieving URL: " + CERT_URL)
            .hasCause(failure);
    }

    @Test
    void retrieveCertificate_noResponseBody_throws() {
        http.respond(CERT_URL, () -> SdkHttpResponse.builder().statusCode(200).build());

        assertThatThrownBy(() -> retriever.retrieveCertificate(URI.create(CERT_URL)))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("Response body is empty");
    }

    @Test
    void retrieveCertificate_notPem_throws() {
        // Streaming, as transports return bodies: a zero-length stream is a body, so it fails the format check.
        for (String body : List.of("", "not a certificate", "-----BEGIN CERTIFICATE-----\n!!!\n-----END CERTIFICATE-----",
                                   new String(SnsTestFixtures.certificate("sns-global"), StandardCharsets.UTF_8)
                                       .replace("\n", "\r\n"))) {
            http.serveStreaming(CERT_URL, body.getBytes(StandardCharsets.UTF_8));

            assertThatThrownBy(() -> retriever.retrieveCertificate(URI.create(CERT_URL)))
                .isInstanceOf(SdkClientException.class)
                .hasMessage("Certificate does not match expected X509 PEM format.");
        }
    }

    @Test
    void retrieveCertificate_pemButNotACertificate_throws() {
        http.serve(CERT_URL, "-----BEGIN CERTIFICATE-----\nAAAA\n-----END CERTIFICATE-----\n"
            .getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> retriever.retrieveCertificate(URI.create(CERT_URL)))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("The certificate could not be parsed");
    }

    @Test
    void retrieveCertificate_expired_throws() {
        http.serve(CERT_URL, SnsTestFixtures.certificate("expired"));

        assertThatThrownBy(() -> retriever.retrieveCertificate(URI.create(CERT_URL)))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("The certificate is expired");
    }

    @Test
    void retrieveCertificate_notYetValid_throws() {
        http.serve(CERT_URL, SnsTestFixtures.certificate("not-yet-valid"));

        assertThatThrownBy(() -> retriever.retrieveCertificate(URI.create(CERT_URL)))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("The certificate is not yet valid");
    }

    @Test
    void retrieveCertificate_validityIsCheckedAtTheClock() {
        http.serve(CERT_URL, SnsTestFixtures.certificate("sns-global"));

        assertThatThrownBy(() -> retriever(Clock.offset(SnsTestFixtures.FIXED_CLOCK, java.time.Duration.ofDays(20 * 366)))
                                     .retrieveCertificate(URI.create(CERT_URL)))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("The certificate is expired");
    }

    @Test
    void retrieveCertificate_issuedToAnotherName_throws() {
        http.serve(CERT_URL, SnsTestFixtures.certificate("wrong-name"));

        assertThatThrownBy(() -> retriever.retrieveCertificate(URI.create(CERT_URL)))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("Certificate does not match expected common name: " + COMMON_NAME);
    }

    @Test
    void retrieveCertificate_cacheHit_fetchesOnce() {
        http.serve(CERT_URL, SnsTestFixtures.certificate("sns-global"));

        PublicKey first = retriever.retrieveCertificate(URI.create(CERT_URL));
        PublicKey second = retriever.retrieveCertificate(URI.create(CERT_URL));

        assertThat(second).isSameAs(first);
        assertThat(http.requests()).hasSize(1);
    }

    @Test
    void retrieveCertificate_failureIsNotCached() {
        http.serve(CERT_URL, SnsTestFixtures.certificate("wrong-name"));
        assertThatThrownBy(() -> retriever.retrieveCertificate(URI.create(CERT_URL)))
            .isInstanceOf(SdkClientException.class);

        http.serve(CERT_URL, SnsTestFixtures.certificate("sns-global"));

        assertThat(retriever.retrieveCertificate(URI.create(CERT_URL))).isEqualTo(SnsTestFixtures.publicKey());
        assertThat(http.requests()).hasSize(2);
    }

    @Test
    void retrieveCertificate_cacheKeepsTheTenMostRecentlyUsedUrls() {
        for (int i = 0; i < 11; i++) {
            http.serve(url(i), SnsTestFixtures.certificate("sns-global"));
        }
        for (int i = 0; i < 10; i++) {
            retriever.retrieveCertificate(URI.create(url(i)));
        }
        retriever.retrieveCertificate(URI.create(url(0))); // url(1) is now the least recently used
        retriever.retrieveCertificate(URI.create(url(10))); // evicts url(1)
        assertThat(http.requests()).hasSize(11);

        retriever.retrieveCertificate(URI.create(url(0)));
        assertThat(http.requests()).hasSize(11);
        retriever.retrieveCertificate(URI.create(url(1)));
        assertThat(http.requests()).hasSize(12);
    }

    /**
     * Two simultaneous callers (released together by a barrier), 40 retrievals in total over 4 URLs, completing within
     * 10 seconds: every caller gets the fixture key, and each URL is fetched at most once per simultaneous miss.
     */
    @Test
    void retrieveCertificate_concurrentCallers_threadSafe() throws Exception {
        int threads = 2;
        int perThread = 20;
        for (int i = 0; i < 4; i++) {
            http.serve(url(i), SnsTestFixtures.certificate("sns-global"));
        }
        CyclicBarrier barrier = new CyclicBarrier(threads);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            CompletableFuture<?>[] futures = new CompletableFuture<?>[threads];
            for (int t = 0; t < threads; t++) {
                futures[t] = CompletableFuture.runAsync(() -> {
                    try {
                        barrier.await(10, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                    for (int i = 0; i < perThread; i++) {
                        assertThat(retriever.retrieveCertificate(URI.create(url(i % 4))))
                            .isEqualTo(SnsTestFixtures.publicKey());
                    }
                }, executor);
            }
            CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
        assertThat(http.requests()).hasSizeBetween(4, 4 * threads);
    }

    @Test
    void close_callerOwnedClient_isNotClosed() {
        retriever.close();

        assertThat(http.isClosed()).isFalse();
    }

    @Test
    void constructor_nullArguments_throw() {
        assertThatThrownBy(() -> new CertificateRetriever(null, CERT_HOST, COMMON_NAME))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("httpClient must not be null.");
        assertThatThrownBy(() -> new CertificateRetriever(http.callerOwned(), CERT_HOST, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("certCommonName must not be null.");
        assertThatThrownBy(() -> new CertificateRetriever(http.callerOwned(), null, COMMON_NAME))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Expected certificate host cannot be null or empty");
    }

    private CertificateRetriever retriever(Clock clock) {
        return new CertificateRetriever(http.callerOwned(), COMMON_NAME, new CertificateUrlValidator(CERT_HOST), clock);
    }

    private static String url(int i) {
        return "https://" + CERT_HOST + "/SimpleNotificationService-" + i + ".pem";
    }
}
