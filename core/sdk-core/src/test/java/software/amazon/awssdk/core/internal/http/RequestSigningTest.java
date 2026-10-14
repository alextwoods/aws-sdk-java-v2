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

package software.amazon.awssdk.core.internal.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncSigningStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.SigningStage;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignerProperty;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.NoOpMetricCollector;

/**
 * Compares {@link RequestSigning} with the legacy {@link SigningStage} and {@link AsyncSigningStage}.
 */
class RequestSigningTest {
    private static final SignerProperty<String> PROPERTY = SignerProperty.create(RequestSigningTest.class, "property");
    private static final AwsCredentialsIdentity IDENTITY = AwsCredentialsIdentity.create("akid", "secret");

    private HttpClientDependencies dependencies;
    private final TestSigner signer = new TestSigner();

    @BeforeEach
    void setup() {
        dependencies = HttpClientDependencies.builder().clientConfiguration(SdkClientConfiguration.builder().build()).build();
        dependencies.updateTimeOffset(7);
    }

    @AfterEach
    void resetTimeOffset() {
        dependencies.updateTimeOffset(0);
    }

    @Test
    void sign_producesTheLegacySigningStageRequest() throws Exception {
        SdkHttpFullRequest request = request();
        RequestExecutionContext legacy = context(CompletableFuture.completedFuture(IDENTITY), request);
        SdkHttpFullRequest expected = new SigningStage(dependencies).execute(request, legacy);

        RequestExecutionContext context = context(CompletableFuture.completedFuture(IDENTITY), request);
        SdkHttpFullRequest actual = RequestSigning.create(dependencies).sign(request, context);

        assertSigned(actual, expected);
        assertThat(context.executionContext().interceptorContext().httpRequest()).isSameAs(actual);
        assertThat(context.executionAttributes().getAttribute(SdkExecutionAttribute.TIME_OFFSET)).isEqualTo(7);
        assertThat(signer.lastSignRequest.property(PROPERTY)).isEqualTo("value");
        assertThat(signer.lastSignRequest.property(HttpSigner.SIGNING_CLOCK).instant())
            .isBetween(Instant.now().minusSeconds(9), Instant.now().minusSeconds(5));
    }

    @Test
    void signAsync_resolvedIdentity_signsInlineAsTheLegacyAsyncStage() throws Exception {
        SdkHttpFullRequest request = request();
        RequestExecutionContext legacy = context(CompletableFuture.completedFuture(IDENTITY), request);
        SdkHttpFullRequest expected = new AsyncSigningStage(dependencies).execute(request, legacy).join();

        RequestExecutionContext context = context(CompletableFuture.completedFuture(IDENTITY), request);
        CompletableFuture<SdkHttpFullRequest> actual = RequestSigning.create(dependencies).signAsync(request, context);

        assertThat(actual).isDone();
        assertSigned(actual.join(), expected);
        assertThat(context.executionContext().interceptorContext().httpRequest()).isSameAs(actual.join());
        assertThat(context.executionAttributes().getAttribute(SdkExecutionAttribute.TIME_OFFSET)).isEqualTo(7);
    }

    @Test
    void signAsync_pendingIdentity_signsWhenTheIdentityIsResolved() throws Exception {
        CompletableFuture<AwsCredentialsIdentity> identity = new CompletableFuture<>();
        SdkHttpFullRequest request = request();
        RequestExecutionContext context = context(identity, request);

        CompletableFuture<SdkHttpFullRequest> signed = RequestSigning.create(dependencies).signAsync(request, context);
        assertThat(signed).isNotDone();
        identity.complete(IDENTITY);

        assertThat(signed.join().firstMatchingHeader("Authorization")).hasValue("signed-by-akid");
    }

    @Test
    void sign_pendingIdentity_waitsForItAsTheLegacyStage() throws Exception {
        CompletableFuture<AwsCredentialsIdentity> identity = new CompletableFuture<>();
        SdkHttpFullRequest request = request();
        RequestExecutionContext context = context(identity, request);
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            identity.complete(IDENTITY);
        });

        SdkHttpFullRequest signed = RequestSigning.create(dependencies).sign(request, context);

        assertThat(signed.firstMatchingHeader("Authorization")).hasValue("signed-by-akid");
        assertThat(context.executionContext().interceptorContext().httpRequest()).isSameAs(signed);
    }

    @Test
    void sign_failedIdentity_failsAsTheLegacyStage() {
        CompletableFuture<AwsCredentialsIdentity> identity = new CompletableFuture<>();
        identity.completeExceptionally(new IllegalStateException("no credentials"));
        RequestExecutionContext context = context(identity, request());

        assertThatThrownBy(() -> RequestSigning.create(dependencies).sign(request(), context))
            .isInstanceOf(IllegalStateException.class).hasMessage("no credentials");
    }

    @Test
    void signAsync_signerFailure_failsTheFuture() throws Exception {
        signer.failure = new IllegalStateException("cannot sign");
        RequestExecutionContext context = context(CompletableFuture.completedFuture(IDENTITY), request());

        CompletableFuture<SdkHttpFullRequest> signed = RequestSigning.create(dependencies).signAsync(request(), context);

        assertThatThrownBy(signed::join).isInstanceOf(CompletionException.class).hasRootCauseMessage("cannot sign");
    }

    @Test
    void sign_noAuthSchemes_returnsTheRequestAsTheLegacyStage() throws Exception {
        SdkHttpFullRequest request = request();
        RequestExecutionContext context = context(CompletableFuture.completedFuture(IDENTITY), request);
        context.executionAttributes().putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES, null);

        assertThat(RequestSigning.create(dependencies).sign(request, context)).isSameAs(request);
        assertThat(RequestSigning.create(dependencies).signAsync(request, context).join()).isSameAs(request);
    }

    @Test
    void sign_collectingMetrics_reportsTheSigningDuration() throws Exception {
        MetricCollector collector = mock(MetricCollector.class);
        RequestExecutionContext context = context(CompletableFuture.completedFuture(IDENTITY), request());
        context.attemptMetricCollector(collector);

        RequestSigning.create(dependencies).sign(request(), context);

        verify(collector).reportMetric(eq(CoreMetric.SIGNING_DURATION), any());
    }

    @Test
    void sign_notCollectingMetrics_signsWithoutMeasuring() throws Exception {
        RequestExecutionContext context = context(CompletableFuture.completedFuture(IDENTITY), request());
        context.attemptMetricCollector(NoOpMetricCollector.create());

        assertThat(RequestSigning.create(dependencies).sign(request(), context).firstMatchingHeader("Authorization"))
            .hasValue("signed-by-akid");
    }

    private static void assertSigned(SdkHttpFullRequest actual, SdkHttpFullRequest expected) {
        assertThat(actual.getUri()).isEqualTo(expected.getUri());
        assertThat(actual.headers()).isEqualTo(expected.headers());
        assertThat(actual.firstMatchingHeader("Authorization")).hasValue("signed-by-akid");
        assertThat(ApiCallStagesTestSupport.body(actual)).isEqualTo(ApiCallStagesTestSupport.body(expected));
    }

    private static SdkHttpFullRequest request() {
        return ApiCallStagesTestSupport.marshalledRequest(ApiCallStagesTestSupport.CLIENT_ENDPOINT, "{}");
    }

    private RequestExecutionContext context(CompletableFuture<AwsCredentialsIdentity> identity, SdkHttpFullRequest request) {
        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES, Collections.emptyMap());
        attributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME,
                                new SelectedAuthScheme<>(identity, signer,
                                                         AuthSchemeOption.builder().schemeId("aws.auth#sigv4")
                                                                         .putSignerProperty(PROPERTY, "value").build()));
        RequestExecutionContext context =
            ApiCallStagesTestSupport.context(NoopTestRequest.builder().build(), attributes, request);
        context.attemptMetricCollector(mock(MetricCollector.class));
        return context;
    }

    private static final class TestSigner implements HttpSigner<AwsCredentialsIdentity> {
        private SignRequest<? extends AwsCredentialsIdentity> lastSignRequest;
        private RuntimeException failure;

        @Override
        public SignedRequest sign(SignRequest<? extends AwsCredentialsIdentity> request) {
            lastSignRequest = request;
            if (failure != null) {
                throw failure;
            }
            Clock clock = request.property(HttpSigner.SIGNING_CLOCK);
            return SignedRequest.builder()
                                .request(request.request().toBuilder()
                                                .putHeader("Authorization", "signed-by-" + request.identity().accessKeyId())
                                                .putHeader("X-Amz-Date-Present", String.valueOf(clock != null))
                                                .build())
                                .payload(request.payload().orElse(null))
                                .build();
        }

        @Override
        public CompletableFuture<AsyncSignedRequest> signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request) {
            throw new UnsupportedOperationException();
        }
    }
}
