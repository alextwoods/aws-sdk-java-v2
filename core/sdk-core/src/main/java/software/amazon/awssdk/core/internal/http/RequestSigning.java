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

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.pipeline.stages.AsyncSigningStage;
import software.amazon.awssdk.core.internal.http.pipeline.stages.SigningStage;
import software.amazon.awssdk.core.internal.util.MetricUtils;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.PayloadChecksumStore;
import software.amazon.awssdk.http.auth.spi.signer.SdkInternalHttpSignerProperty;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Logger;

/**
 * The signing step of the schema-based request pipeline, shared by the sync and async pipelines. Each attempt is
 * signed from the unsigned request of that attempt (the prepared request with that attempt's retry-info header), so a
 * signature is never computed over an earlier attempt's signature (racecar B2.a is not needed).
 *
 * <p>The observable behavior is that of the legacy {@link SigningStage} and {@link AsyncSigningStage}: the interceptor
 * context holds the request before and after signing, {@link SdkExecutionAttribute#TIME_OFFSET} is the client's clock
 * skew, the signing clock is offset by it, and a pre-SRA {@code Signer} or a client without auth schemes is handled as
 * before. Differences in what is computed:
 * <ul>
 *     <li><b>H8.</b> {@link CoreMetric#SIGNING_DURATION} is measured only when the attempt's metric collector collects
 *     metrics.</li>
 *     <li><b>Racecar C (de-futuring, re-implemented).</b> {@link #signAsync} signs inline and returns a completed future
 *     when the request has no asynchronous body and the identity is already resolved, instead of chaining futures.</li>
 * </ul>
 * A pre-SRA {@code Signer}, an asynchronous request body, or an identity that is not resolved yet are delegated to the
 * legacy stages, which are used unchanged.
 */
@SdkInternalApi
@ThreadSafe
public final class RequestSigning {
    private static final Logger log = Logger.loggerFor(SigningStage.class);
    private static final Logger ASYNC_LOG = Logger.loggerFor(AsyncSigningStage.class);

    private final HttpClientDependencies dependencies;
    private final SigningStage syncSigningStage;
    private final AsyncSigningStage asyncSigningStage;

    private RequestSigning(HttpClientDependencies dependencies) {
        this.dependencies = dependencies;
        this.syncSigningStage = new SigningStage(dependencies);
        this.asyncSigningStage = new AsyncSigningStage(dependencies);
    }

    /**
     * Creates the signing step for a client.
     */
    public static RequestSigning create(HttpClientDependencies dependencies) {
        return new RequestSigning(dependencies);
    }

    /**
     * Signs an attempt's request (sync pipeline).
     *
     * @param request the attempt's unsigned request
     * @param context the call's context; its attempt metric collector is set
     * @return the signed request, also set on the interceptor context
     * @throws Exception as the legacy {@link SigningStage} throws (including {@code InterruptedException} when the thread
     *     is interrupted)
     */
    public SdkHttpFullRequest sign(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {
        if (context.signer() != null || !hasAuthSchemes(context)) {
            return syncSigningStage.execute(request, context);
        }
        SelectedAuthScheme<?> selectedAuthScheme = selectedAuthScheme(context);
        Identity identity = resolvedIdentity(selectedAuthScheme);
        if (identity == null) {
            // Not resolved yet (or failed): the legacy stage waits for it, or throws its failure.
            return syncSigningStage.execute(request, context);
        }
        InterruptMonitor.checkInterrupted();
        updateHttpRequestInInterceptorContext(request, context.executionContext());
        logSelectedAuthScheme(log, selectedAuthScheme);
        context.executionAttributes().putAttribute(SdkExecutionAttribute.TIME_OFFSET, dependencies.timeOffset());
        return signAndRecord(request, context, selectedAuthScheme, identity);
    }

    /**
     * Signs an attempt's request (async pipeline). The returned future is already complete when the request was signed
     * inline.
     *
     * @param request the attempt's unsigned request
     * @param context the call's context; its attempt metric collector is set
     * @return the signed request, also set on the interceptor context. As with {@link AsyncSigningStage}, a failure of
     *     the signer completes the future exceptionally, and a failure before signing starts is thrown
     * @throws Exception as {@link AsyncSigningStage#execute} throws
     */
    public CompletableFuture<SdkHttpFullRequest> signAsync(SdkHttpFullRequest request, RequestExecutionContext context)
            throws Exception {
        if (context.signer() != null || !hasAuthSchemes(context) || context.requestProvider() != null) {
            return asyncSigningStage.execute(request, context);
        }
        SelectedAuthScheme<?> selectedAuthScheme = selectedAuthScheme(context);
        Identity identity = resolvedIdentity(selectedAuthScheme);
        if (identity == null) {
            return asyncSigningStage.execute(request, context);
        }
        updateHttpRequestInInterceptorContext(request, context.executionContext());
        logSelectedAuthScheme(ASYNC_LOG, selectedAuthScheme);
        context.executionAttributes().putAttribute(SdkExecutionAttribute.TIME_OFFSET, dependencies.timeOffset());
        try {
            return CompletableFuture.completedFuture(signAndRecord(request, context, selectedAuthScheme, identity));
        } catch (Throwable t) {
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    private static boolean hasAuthSchemes(RequestExecutionContext context) {
        return context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEMES) != null;
    }

    private static SelectedAuthScheme<?> selectedAuthScheme(RequestExecutionContext context) {
        return context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
    }

    /**
     * The selected auth scheme's identity when it is already resolved, without waiting; null when there is no auth
     * scheme or its identity is not resolved yet or failed, in which case the legacy stage handles the attempt exactly
     * as before (it waits for the identity, or reports its failure).
     */
    private static Identity resolvedIdentity(SelectedAuthScheme<?> selectedAuthScheme) {
        if (selectedAuthScheme == null) {
            return null;
        }
        CompletableFuture<? extends Identity> identity = selectedAuthScheme.identity();
        if (!identity.isDone() || identity.isCompletedExceptionally()) {
            return null;
        }
        return identity.getNow(null);
    }

    private static void logSelectedAuthScheme(Logger logger, SelectedAuthScheme<?> selectedAuthScheme) {
        logger.debug(() -> String.format("Using SelectedAuthScheme: %s", selectedAuthScheme.authSchemeOption().schemeId()));
    }

    /**
     * Signs and, when the attempt collects metrics, reports {@link CoreMetric#SIGNING_DURATION} after a successful
     * signature, as both legacy stages do (the legacy async stage signs a request without an asynchronous body
     * synchronously too, so a failure there reports nothing either).
     */
    @SuppressWarnings("unchecked")
    private <T extends Identity> SdkHttpFullRequest signAndRecord(SdkHttpFullRequest request,
                                                                  RequestExecutionContext context,
                                                                  SelectedAuthScheme<T> selectedAuthScheme,
                                                                  Identity identity) {
        PayloadChecksumStore payloadChecksumStore =
            context.executionAttributes().getAttribute(SdkInternalExecutionAttribute.CHECKSUM_STORE);
        MetricCollector attemptCollector = context.attemptMetricCollector();
        boolean measure = MetricUtils.collectsMetrics(attemptCollector);
        long start = measure ? System.nanoTime() : 0L;
        SdkHttpFullRequest signed = doSign(request, selectedAuthScheme, (T) identity, payloadChecksumStore);
        if (measure) {
            attemptCollector.reportMetric(CoreMetric.SIGNING_DURATION, Duration.ofNanos(System.nanoTime() - start));
        }
        updateHttpRequestInInterceptorContext(signed, context.executionContext());
        return signed;
    }

    private <T extends Identity> SdkHttpFullRequest doSign(SdkHttpFullRequest request,
                                                           SelectedAuthScheme<T> selectedAuthScheme,
                                                           T identity,
                                                           PayloadChecksumStore payloadChecksumStore) {
        SignRequest.Builder<T> signRequest = SignRequest.builder(identity)
                                                        .putProperty(HttpSigner.SIGNING_CLOCK, signingClock())
                                                        .putProperty(SdkInternalHttpSignerProperty.CHECKSUM_STORE,
                                                                     payloadChecksumStore)
                                                        .request(request)
                                                        .payload(request.contentStreamProvider().orElse(null));
        selectedAuthScheme.authSchemeOption().forEachSignerProperty(signRequest::putProperty);
        return toSdkHttpFullRequest(selectedAuthScheme.signer().sign(signRequest.build()));
    }

    /**
     * Same as {@link SigningStage}: the signed request is returned as it is when it is already an
     * {@link SdkHttpFullRequest} with the same payload.
     */
    private static SdkHttpFullRequest toSdkHttpFullRequest(SignedRequest signedRequest) {
        SdkHttpRequest request = signedRequest.request();
        if (request instanceof SdkHttpFullRequest) {
            SdkHttpFullRequest fullRequest = (SdkHttpFullRequest) request;
            if (signedRequest.payload().orElse(null) == fullRequest.contentStreamProvider().orElse(null)) {
                return fullRequest;
            }
            return fullRequest.toBuilder().contentStreamProvider(signedRequest.payload().orElse(null)).build();
        }
        return SdkHttpFullRequest.builder()
                                 .contentStreamProvider(signedRequest.payload().orElse(null))
                                 .protocol(request.protocol())
                                 .method(request.method())
                                 .host(request.host())
                                 .port(request.port())
                                 .encodedPath(request.encodedPath())
                                 .applyMutation(r -> request.forEachHeader(r::putHeader))
                                 .applyMutation(r -> request.forEachRawQueryParameter(r::putRawQueryParameter))
                                 .build();
    }

    private Clock signingClock() {
        return Clock.offset(Clock.systemUTC(), Duration.ofSeconds(-dependencies.timeOffset()));
    }

    private static void updateHttpRequestInInterceptorContext(SdkHttpFullRequest request,
                                                              ExecutionContext executionContext) {
        executionContext.interceptorContext(executionContext.interceptorContext().copy(b -> b.httpRequest(request)));
    }
}
