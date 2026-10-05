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

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import software.amazon.awssdk.auth.api.AuthSchemeIds;
import software.amazon.awssdk.auth.api.AuthSchemeOption;
import software.amazon.awssdk.auth.api.AuthSchemeSelector;
import software.amazon.awssdk.auth.api.ModeledAuth;
import software.amazon.awssdk.auth.api.PayloadSigningMode;
import software.amazon.awssdk.auth.api.SelectedAuthScheme;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.checksums.ChecksumOptions;
import software.amazon.awssdk.checksums.PayloadSigning;
import software.amazon.awssdk.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.checksums.RequestChecksumFeature;
import software.amazon.awssdk.checksums.ResolvedRequestChecksum;
import software.amazon.awssdk.checksums.ResponseChecksumFeature;
import software.amazon.awssdk.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.checksums.compression.CompressionSettings;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.override.CallOptions;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.endpoints.EndpointPlan;
import software.amazon.awssdk.endpoints.EndpointResolutionException;
import software.amazon.awssdk.http.CancellationReason;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.pipeline.feature.IdempotencyTokenFeature;
import software.amazon.awssdk.protocols.core.OperationProtocol;
import software.amazon.awssdk.protocols.core.eventstream.EventStreamRequestBody;
import software.amazon.awssdk.protocols.core.eventstream.OutputEventStream;
import software.amazon.awssdk.retries.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.AttemptFailure;
import software.amazon.awssdk.retries.RetryDecision;
import software.amazon.awssdk.retries.RetryToken;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The shared semantic stages of the one AWS pipeline (design "Plan and drivers"; Requirements 10.1, 10.4): the
 * transport-independent, non-blocking work of each {@link PipelineStage}, called in lifecycle order by both the sync
 * and the async driver of {@link AwsCallEngine}. The drivers own only the mechanics that differ between them: waiting
 * for identity, transmitting through the sync or async transport, and honoring retry delays.
 *
 * <p>Each method enters its stages in {@link PipelineStage} declaration order, including the stage points of the
 * hooks around it, so both drivers follow one lifecycle by calling the methods in the order: {@link #begin},
 * {@link #beforeAttempts}; per attempt {@link #startAttempt}, {@link #resolveEndpoint}, {@link #selectAuthScheme},
 * {@link #resolveIdentity} and {@link #identityResolved} (signed calls only), {@link #serialize},
 * {@link #requestFeatures}, {@link #sign}, {@link #transmit} and the transport exchange, {@link #responseReceived},
 * {@link #deserialize}; then {@link #succeed}, or {@link #classify} of the failed attempt and, without a retry token,
 * {@link #fail}. A failing stage throws, so its dependent stages never run (Requirement 10.14).
 *
 * <p>Every method runs on the call's driver and does no I/O and no blocking wait, so the async driver may call it
 * from any continuation; the one exception is {@link #sign(CallState, AttemptState)}, which reads a streaming
 * payload's pending digests through its blocking view for the sync driver and presigning. The async driver signs
 * through {@link #prepareSigning} and {@link #sign(CallState, AttemptState, PayloadDigests.Result)} instead, with
 * a non-blocking {@link PayloadDigests} pass between them when the payload needs one. Stages that are inactive in the plan's {@link StageMetadata} cost one bit test; without a
 * {@link StageObserver} a stage point costs one null check, and a hook no interceptor implements costs one array
 * length check in the plan's {@link InterceptorDispatch}, with no context object. One instance per engine; immutable
 * and thread safe, holding only client-constant components.
 */
final class CallStages {
    private final ClientRuntime runtime;
    private final IdentityProviders identityProviders;
    private final AuthOptionsResolver authOptions;
    private final StageObserver observer;

    CallStages(ClientRuntime runtime, IdentityProviders identityProviders, AuthOptionsResolver authOptions,
               StageObserver observer) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.identityProviders = Objects.requireNonNull(identityProviders, "identityProviders");
        this.authOptions = Objects.requireNonNull(authOptions, "authOptions");
        this.observer = observer;
    }

    /**
     * The point where {@code stage} starts; with an observer, an active stage is reported to it. A hook stage's
     * interceptors are dispatched right after its point through the plan's {@link InterceptorDispatch}, which does
     * nothing for a hook no interceptor implements.
     */
    private void enter(CallState<?, ?> call, AttemptState attempt, PipelineStage stage) {
        StageObserver stageObserver = observer;
        if (stageObserver != null && call.plan().stages().isActive(stage)) {
            stageObserver.entered(call, attempt, stage);
        }
    }

    /**
     * {@link PipelineStage#VALIDATE}: checks the invocation and merges the call's override deltas into a new call.
     *
     * @throws IllegalArgumentException if the plan belongs to another client or the input is not the operation's
     */
    <I extends SerializableStruct, O extends SerializableStruct> CallState<I, O> begin(
        PipelinePlan<I, O> plan, I input, AwsRequestOverrideConfiguration override) {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(input, "input");
        if (plan.runtime() != runtime) {
            throw new IllegalArgumentException(plan + " belongs to another client");
        }
        if (!plan.operation().schemas().input().id().equals(input.schema().id())) {
            throw new IllegalArgumentException("The input of " + plan.operation().id() + " is "
                                               + plan.operation().schemas().input().id() + ", not "
                                               + input.schema().id());
        }
        // The request-embedded override joins here once generated request types carry one.
        CallOptions options = runtime.callOptions(null, override);
        CallState<I, O> call = plan.newCall(input, options);
        // Validation precedes the call, so it is reported on the call it produced.
        enter(call, null, PipelineStage.VALIDATE);
        return call;
    }

    /**
     * The execution-scoped stages before the first attempt: the {@link PipelineStage#BEFORE_EXECUTION} and
     * {@link PipelineStage#MODIFY_REQUEST} hook points, {@link PipelineStage#IDEMPOTENCY_TOKEN}, once per call, so
     * every attempt serializes the same token, then {@link PipelineStage#ACQUIRE_RETRY_TOKEN}. The driver records the
     * token on the call and waits for its delay before the first attempt.
     */
    AcquireInitialTokenResponse beforeAttempts(CallState<?, ?> call) {
        enter(call, null, PipelineStage.BEFORE_EXECUTION);
        call.plan().dispatch().beforeExecution(call);
        enter(call, null, PipelineStage.MODIFY_REQUEST);
        modifyRequest(call);
        enter(call, null, PipelineStage.IDEMPOTENCY_TOKEN);
        injectIdempotencyToken(call);
        enter(call, null, PipelineStage.ACQUIRE_RETRY_TOKEN);
        AcquireInitialTokenResponse initial =
            runtime.retryStrategy().acquireInitialToken(call.plan().initialTokenRequest());
        recordToken(call, initial.token());
        return initial;
    }

    /**
     * Records the new {@code token} on the call. If the call ended meanwhile (a caller cancelled it on another thread
     * while the token was being obtained), its end found no token to release, so the token is released here, and the
     * call's terminal failure is thrown: no further stage runs. Claims make the release exactly-once either way.
     */
    private void recordToken(CallState<?, ?> call, RetryToken token) {
        call.retryToken(token);
        if (call.terminal().isTerminal()) {
            releaseToken(call);
            throw terminalFailure(call);
        }
    }

    private static <I extends SerializableStruct> void modifyRequest(CallState<I, ?> call) {
        I replacement = call.plan().dispatch().modifyRequest(call);
        if (replacement != call.request()) {
            call.replaceRequest(replacement);
        }
    }

    private <I extends SerializableStruct> void injectIdempotencyToken(CallState<I, ?> call) {
        PipelinePlan<I, ?> plan = call.plan();
        if (!plan.stages().isActive(PipelineStage.IDEMPOTENCY_TOKEN)) {
            return;
        }
        IdempotencyTokenFeature.Result<I> result = plan.idempotencyToken().orElseThrow().apply(call.request());
        call.replaceRequest(result.request());
        result.generatedToken().ifPresent(
            token -> call.executionContext().put(plan.idempotencyTokenSlot().orElseThrow(), token));
    }

    /**
     * Starts the one attempt {@code token} authorizes; its first stage is {@link #resolveEndpoint}.
     *
     * @throws IllegalStateException if the token was already used, or the call ended or was cancelled
     */
    AttemptState startAttempt(CallState<?, ?> call, RetryToken token) {
        return call.startAttempt(token);
    }

    /**
     * The {@link PipelineStage#BEFORE_ATTEMPT} hook point, the first work of the attempt (so a hook failure is the
     * attempt's failure), then {@link PipelineStage#RESOLVE_ENDPOINT}: from the operation's endpoint rules, else the
     * client endpoint.
     */
    void resolveEndpoint(CallState<?, ?> call, AttemptState attempt) {
        enter(call, attempt, PipelineStage.BEFORE_ATTEMPT);
        call.plan().dispatch().beforeAttempt(call, attempt);
        enter(call, attempt, PipelineStage.RESOLVE_ENDPOINT);
        Optional<EndpointPlan> rules = call.plan().endpoint();
        Endpoint endpoint;
        if (rules.isPresent()) {
            try {
                endpoint = rules.get().resolve(call.request(), runtime.endpointClientValues());
            } catch (EndpointResolutionException e) {
                // Rule errors, unmatched rules and host-label failures do not know the operation (Requirement 13.18).
                throw EndpointResolutionException.forOperation(call.plan().operation().id().toString(), e);
            }
        } else {
            URI url = call.options().get(SdkClientOption.CLIENT_ENDPOINT).orElseThrow(
                () -> SdkClientException.create("No endpoint is configured for " + call.plan().operation().id()
                                                + ": the client has no endpoint rule set and no endpoint"));
            endpoint = Endpoint.builder().url(url).build();
        }
        attempt.endpoint(endpoint);
    }

    /**
     * {@link PipelineStage#SELECT_AUTH_SCHEME}: the attempt's auth scheme, or {@code null} when the call is not
     * signed (the operation records no modeled auth, or only {@code smithy.api#noAuth} and the client has no
     * selector). The driver then resolves the scheme's identity and passes it to {@link #identityResolved}.
     *
     * @throws SdkClientException if the operation requires auth the client cannot provide
     */
    SelectedAuthScheme<?> selectAuthScheme(CallState<?, ?> call, AttemptState attempt) {
        return selectAuthScheme(call, attempt, null);
    }

    /**
     * {@link #selectAuthScheme(CallState, AttemptState)} of a presign call: the selected option is adjusted by
     * {@code presign}'s auth option (for SigV4, query-string placement and the expiration) before it is recorded, so
     * {@link #sign} presigns. A presign call must be signed.
     *
     * @throws SdkClientException if the operation is not signed, so it cannot be presigned
     */
    SelectedAuthScheme<?> selectPresignAuthScheme(CallState<?, ?> call, AttemptState attempt, PresignSpec presign) {
        SelectedAuthScheme<?> selected = selectAuthScheme(call, attempt, presign);
        if (selected == null || AuthSchemeIds.NO_AUTH.equals(selected.option().schemeId())) {
            throw SdkClientException.create("Operation " + call.plan().operation().id() + " is not signed, so it "
                                            + "cannot be presigned");
        }
        return selected;
    }

    private SelectedAuthScheme<?> selectAuthScheme(CallState<?, ?> call, AttemptState attempt, PresignSpec presign) {
        enter(call, attempt, PipelineStage.SELECT_AUTH_SCHEME);
        Optional<ModeledAuth> auth = call.plan().auth();
        if (auth.isEmpty()) {
            return null;
        }
        Optional<AuthSchemeSelector> selector = runtime.authSchemeSelector();
        if (selector.isEmpty()) {
            List<String> schemes = auth.get().effectiveSchemeIds();
            if (schemes.equals(List.of(AuthSchemeIds.NO_AUTH))) {
                return null;
            }
            throw SdkClientException.create("Failed to select an auth scheme for operation "
                                            + call.plan().operation().id() + ": the client has no auth scheme "
                                            + "selector, and the operation uses " + schemes);
        }
        SelectedAuthScheme<?> selected = selector.get().select(call.plan().operation().id().toString(),
                                                               authOptions.options(auth.get(), call.options()),
                                                               identityProviders);
        if (presign != null) {
            selected = adjusted(selected, presign);
        }
        attempt.authScheme(selected);
        return selected;
    }

    private static <T extends Identity> SelectedAuthScheme<T> adjusted(SelectedAuthScheme<T> selected,
                                                                       PresignSpec presign) {
        AuthSchemeOption option = Objects.requireNonNull(presign.authOption().apply(selected.option()),
                                                         "The presign auth option returned null");
        return new SelectedAuthScheme<>(selected.scheme(), option, selected.identityProvider());
    }

    /**
     * {@link PipelineStage#RESOLVE_IDENTITY}: starts resolving (or refreshing) the identity of the attempt's
     * {@code scheme}; the driver waits for it without blocking a stage, then calls {@link #identityResolved}. An
     * unsigned call has no identity stage.
     */
    CompletableFuture<? extends Identity> resolveIdentity(CallState<?, ?> call, AttemptState attempt,
                                                          SelectedAuthScheme<?> scheme) {
        enter(call, attempt, PipelineStage.RESOLVE_IDENTITY);
        return scheme.resolveIdentity();
    }

    /** Records the identity the driver resolved for the selected scheme. */
    void identityResolved(AttemptState attempt, Identity identity) {
        attempt.identity(identity);
    }

    /**
     * The attempt failure of an identity resolution that failed with {@code cause} (already unwrapped from its
     * future): the cause itself if unchecked, else wrapped with it.
     *
     * @throws Error {@code cause}, if it is one
     */
    static RuntimeException identityFailure(CallState<?, ?> call, Throwable cause) {
        if (cause instanceof RuntimeException runtimeFailure) {
            return runtimeFailure;
        }
        if (cause instanceof Error error) {
            throw error;
        }
        return SdkClientException.create("Failed to resolve the identity of " + call.plan().operation().id(), cause);
    }

    /**
     * {@link PipelineStage#SERIALIZE}: Schema-serializes the call's request against the attempt's endpoint, then
     * applies the endpoint headers, the call's override headers and query parameters, and the explicit streaming
     * body, if any.
     *
     * @param body the operation's explicit input body, or {@code null}
     */
    void serialize(CallState<?, ?> call, AttemptState attempt, HttpBody body) {
        enter(call, attempt, PipelineStage.SERIALIZE);
        Endpoint endpoint = attempt.endpoint().orElseThrow();
        SdkHttpRequest request = call.plan().protocol().serializeRequest(serializableRequest(call), endpoint.url());
        Map<String, List<String>> endpointHeaders = endpoint.headers();
        CallOptions options = call.options();
        Map<String, List<String>> headers = options.headers();
        Map<String, List<String>> query = options.rawQueryParameters();
        if (body != null || !endpointHeaders.isEmpty() || !headers.isEmpty() || !query.isEmpty()) {
            SdkHttpRequest.Builder builder = request.toBuilder();
            putHeaders(builder, endpointHeaders);
            putHeaders(builder, headers);
            query.forEach((name, values) -> values.forEach(value -> builder.addRawQueryParameter(name, value)));
            if (body != null) {
                builder.body(body);
            }
            request = builder.build();
        }
        attempt.unsignedRequest(request);
        enter(call, attempt, PipelineStage.AFTER_SERIALIZATION);
        call.plan().dispatch().afterSerialization(call, attempt, request);
    }

    /** The view of the call's request its operation's protocol serializes. */
    private static <I extends SerializableStruct> SerializableStruct serializableRequest(CallState<I, ?> call) {
        return call.plan().operation().serializableRequest(call.request());
    }

    private static void putHeaders(SdkHttpRequest.Builder builder, Map<String, List<String>> headers) {
        headers.forEach((name, values) -> {
            builder.removeHeader(name);
            values.forEach(value -> builder.addHeader(name, value));
        });
    }

    /**
     * The request feature stages, in lifecycle order: {@link PipelineStage#REQUEST_COMPRESSION}, then
     * {@link PipelineStage#REQUEST_CHECKSUM} over the compressed payload, and the validation-mode header of
     * {@link PipelineStage#RESPONSE_CHECKSUM} (the request half of that feature; the stage itself starts when the
     * response arrives).
     */
    void requestFeatures(CallState<?, ?> call, AttemptState attempt) {
        PipelinePlan<?, ?> plan = call.plan();
        StageMetadata stages = plan.stages();
        SdkHttpRequest request = attempt.unsignedRequest().orElseThrow();
        SdkHttpRequest original = request;
        if (stages.isActive(PipelineStage.REQUEST_COMPRESSION)) {
            enter(call, attempt, PipelineStage.REQUEST_COMPRESSION);
            request = plan.requestCompression().orElseThrow()
                          .apply(request, CompressionSettings.from(call.options().clientConfiguration()));
        }
        if (stages.isActive(PipelineStage.REQUEST_CHECKSUM)) {
            enter(call, attempt, PipelineStage.REQUEST_CHECKSUM);
            RequestChecksumCalculation calculation =
                call.options().get(ChecksumOptions.REQUEST_CHECKSUM_CALCULATION)
                    .orElse(RequestChecksumCalculation.WHEN_SUPPORTED);
            // A header checksum of a streaming payload is computed with the signer's payload hash, in one pass over
            // the body after the pre-signing hooks (prepareSigning), so this stage never reads a body.
            RequestChecksumFeature.Prepared prepared =
                plan.requestChecksum().orElseThrow()
                    .prepare(call.request(), request, calculation, payloadSigning(plan, attempt, request));
            request = prepared.request();
            attempt.pendingChecksum(prepared.pendingHeaderChecksum().orElse(null));
        }
        if (stages.isActive(PipelineStage.RESPONSE_CHECKSUM)) {
            request = plan.responseChecksum().orElseThrow()
                          .prepareRequest(call.request(), request, responseValidation(call));
        }
        if (request != original) {
            attempt.unsignedRequest(request);
        }
    }

    /**
     * How the attempt's auth covers the payload of {@code request}, which places a streaming payload's request
     * checksum: unsigned without a scheme, with {@code noAuth} or for an {@code unsignedPayload} operation; else as
     * the selected signer reports it ({@link PayloadSigningMode#UNSIGNED} for S3's {@code UNSIGNED-PAYLOAD} over
     * HTTPS, for example), and signed (a header checksum) if it does not say. Only a streaming payload's checksum
     * location depends on it, so the signer is asked only for a streaming body.
     */
    private static PayloadSigning payloadSigning(PipelinePlan<?, ?> plan, AttemptState attempt,
                                                 SdkHttpRequest request) {
        Optional<SelectedAuthScheme<?>> scheme = attempt.authScheme();
        boolean unsigned = scheme.isEmpty()
                           || AuthSchemeIds.NO_AUTH.equals(scheme.get().option().schemeId())
                           || plan.auth().map(ModeledAuth::unsignedPayload).orElse(false);
        if (unsigned) {
            return PayloadSigning.UNSIGNED;
        }
        if (request.body().kind() != HttpBody.Kind.STREAMING) {
            return PayloadSigning.SIGNED;
        }
        return payloadSigningMode(scheme.get(), request, attempt.identity().orElseThrow())
            .filter(PayloadSigningMode.UNSIGNED::equals)
            .map(mode -> PayloadSigning.UNSIGNED)
            .orElse(PayloadSigning.SIGNED);
    }

    private static <T extends Identity> Optional<PayloadSigningMode> payloadSigningMode(
        SelectedAuthScheme<T> scheme, SdkHttpRequest request, Identity identity) {
        return scheme.payloadSigningMode(request, scheme.scheme().identityType().cast(identity));
    }

    private static ResponseChecksumValidation responseValidation(CallState<?, ?> call) {
        return call.options().get(ChecksumOptions.RESPONSE_CHECKSUM_VALIDATION)
                   .orElse(ResponseChecksumValidation.WHEN_SUPPORTED);
    }

    /**
     * {@link PipelineStage#SIGN}, between the {@link PipelineStage#MODIFY_BEFORE_SIGNING} and
     * {@link PipelineStage#MODIFY_AFTER_SIGNING} hook points: signs the attempt's request with the selected scheme and
     * resolved identity, or leaves it unsigned when no scheme was selected. A streaming payload's pending digests are
     * read through the body's blocking view, so this is for the sync driver and presigning only; the async driver
     * calls {@link #prepareSigning} and {@link #sign(CallState, AttemptState, PayloadDigests.Result)} around its
     * non-blocking pass.
     *
     * @return the request to transmit
     */
    SdkHttpRequest sign(CallState<?, ?> call, AttemptState attempt) {
        PayloadDigests digests = prepareSigning(call, attempt);
        return sign(call, attempt, digests == null ? null : digests.read());
    }

    /**
     * The {@link PipelineStage#MODIFY_BEFORE_SIGNING} hook point, then the start of {@link PipelineStage#SIGN}: what
     * the attempt's payload needs before it can be signed. For a streaming payload, that is the header checksum the
     * request checksum stage left pending and the SHA-256 payload hash of a
     * {@link PayloadSigningMode#PAYLOAD_HASH} signer, both over the body as the hooks left it; the driver computes
     * them in one pass ({@link PayloadDigests#read()} or, without blocking, {@link PayloadDigests#subscribe()}) and
     * passes them to {@link #sign(CallState, AttemptState, PayloadDigests.Result)}. An in-memory payload needs no
     * pass: its pending checksum is computed here.
     *
     * @return the digests to compute, or {@code null} if none is needed
     * @throws SdkClientException if a digest of a streaming payload is needed but the body is not replayable, so it
     *     cannot be read before it is sent; nothing has been read or transmitted
     */
    PayloadDigests prepareSigning(CallState<?, ?> call, AttemptState attempt) {
        InterceptorDispatch dispatch = call.plan().dispatch();
        enter(call, attempt, PipelineStage.MODIFY_BEFORE_SIGNING);
        SdkHttpRequest request = attempt.unsignedRequest().orElseThrow();
        SdkHttpRequest modified = dispatch.modifyBeforeSigning(call, attempt, request);
        if (modified != request) {
            // The replacement is what is signed (Requirement 11.4).
            attempt.unsignedRequest(modified);
            request = modified;
        }
        enter(call, attempt, PipelineStage.SIGN);
        ResolvedRequestChecksum checksum = attempt.pendingChecksum();
        HttpBody body = request.body();
        if (body.kind() != HttpBody.Kind.STREAMING) {
            if (checksum != null) {
                // A hook replaced the streaming payload with an in-memory one, whose checksum needs no pass.
                SdkChecksum digest = SdkChecksum.create(checksum.algorithm());
                digest.update(body.asByteBuffer());
                attempt.unsignedRequest(RequestChecksumFeature.withHeaderChecksum(request, checksum, digest.digest()));
                attempt.pendingChecksum(null);
            }
            return null;
        }
        Optional<SelectedAuthScheme<?>> scheme = attempt.authScheme();
        boolean sha256 = scheme.isPresent()
                         && payloadSigningMode(scheme.get(), request, attempt.identity().orElseThrow())
                                .filter(PayloadSigningMode.PAYLOAD_HASH::equals).isPresent();
        if (checksum == null && !sha256) {
            return null;
        }
        if (!body.streamingBody().isReplayable()) {
            ShapeId operation = call.plan().operation().id();
            throw SdkClientException.create(
                checksum != null
                ? "Operation " + operation + " needs the " + checksum.name() + " header checksum of its streaming "
                  + "payload before transmission, but the payload is not replayable and would be consumed; use a "
                  + "replayable body or supply the checksum header"
                : "Operation " + operation + " signs a SHA-256 hash of its streaming payload, which is computed "
                  + "before transmission, but the payload is not replayable and would be consumed; use a "
                  + "replayable body, or HTTPS where the service accepts an unsigned payload");
        }
        return new PayloadDigests(body.streamingBody(), checksum, sha256);
    }

    /**
     * The rest of {@link PipelineStage#SIGN}, then the {@link PipelineStage#MODIFY_AFTER_SIGNING} hook point: adds the
     * pending header checksum from {@code digests}, signs the attempt's request with the selected scheme, resolved
     * identity and payload hash, or leaves it unsigned when no scheme was selected.
     *
     * @param digests the result of the {@link PayloadDigests} {@link #prepareSigning} returned, or {@code null} if
     *     it returned none
     * @return the request to transmit
     */
    SdkHttpRequest sign(CallState<?, ?> call, AttemptState attempt, PayloadDigests.Result digests) {
        InterceptorDispatch dispatch = call.plan().dispatch();
        SdkHttpRequest request = attempt.unsignedRequest().orElseThrow();
        String payloadSha256 = null;
        if (digests != null) {
            if (digests.checksum() != null) {
                request = RequestChecksumFeature.withHeaderChecksum(request, attempt.pendingChecksum(),
                                                                    digests.checksum());
                attempt.unsignedRequest(request);
            }
            payloadSha256 = digests.sha256();
        }
        Optional<SelectedAuthScheme<?>> scheme = attempt.authScheme();
        SignedRequest signed = scheme.isPresent()
                               ? sign(scheme.get(), request, attempt.identity().orElseThrow(), payloadSha256)
                               : SignedRequest.unsigned(request);
        HttpBody signedBody = signed.request().body();
        if (signedBody.kind() == HttpBody.Kind.STREAMING
            && signedBody.streamingBody() instanceof EventStreamRequestBody events) {
            // Input event stream frames are chain-signed from the request signature as they are sent; the
            // interceptors after signing see the request with the signed frame body.
            signed = EventStreamFrameSigning.apply(signed, events);
        }
        attempt.signedRequest(signed);
        enter(call, attempt, PipelineStage.MODIFY_AFTER_SIGNING);
        SignedRequest transmitted = dispatch.modifyAfterSigning(call, attempt, signed);
        if (transmitted != signed) {
            attempt.signedRequest(transmitted);
        }
        return transmitted.request();
    }

    /**
     * The {@link PipelineStage#BEFORE_TRANSMISSION} hook point, then {@link PipelineStage#TRANSMIT}: the driver then
     * acquires a connection, transmits the signed request and consumes the response through its transport.
     */
    void transmit(CallState<?, ?> call, AttemptState attempt) {
        enter(call, attempt, PipelineStage.BEFORE_TRANSMISSION);
        call.plan().dispatch().beforeTransmission(call, attempt, attempt.signedRequest().orElseThrow());
        enter(call, attempt, PipelineStage.TRANSMIT);
    }

    /**
     * Ends a presign call after {@link #sign}, without {@link PipelineStage#TRANSMIT}: the attempt's initial retry
     * token is recorded as a success (no attempt failed), the call makes its {@link TerminalState.Status#PRESIGNED}
     * transition, then {@link PipelineStage#TELEMETRY}. No transport is involved and no response hook runs.
     *
     * @return the presigned request
     * @throws RuntimeException the call's terminal failure, if it already ended otherwise (for example cancelled)
     */
    PresignedHttpRequest presigned(CallState<?, ?> call, AttemptState attempt, PresignSpec presign) {
        SdkHttpRequest request = attempt.signedRequest().orElseThrow().request();
        Instant expiration = Objects.requireNonNull(presign.expirationOf().apply(request),
                                                    "The presign expiration of the signed request is null");
        if (call.terminal().isTerminal() || !call.claimToken(attempt.token())) {
            throw terminalFailure(call);
        }
        call.retryToken(runtime.retryStrategy().recordSuccess(attempt.token()));
        if (!call.presigned()) {
            throw terminalFailure(call);
        }
        enter(call, null, PipelineStage.TELEMETRY);
        return new PresignedHttpRequest(request, expiration);
    }

    private static <T extends Identity> SignedRequest sign(SelectedAuthScheme<T> scheme, SdkHttpRequest request,
                                                           Identity identity, String payloadSha256) {
        return scheme.sign(request, scheme.scheme().identityType().cast(identity), payloadSha256);
    }

    /**
     * Records the transport's response: the {@link PipelineStage#AFTER_TRANSMISSION},
     * {@link PipelineStage#MODIFY_HTTP_RESPONSE} and {@link PipelineStage#MODIFY_RESPONSE_BODY} hook points,
     * {@link PipelineStage#RESPONSE_CHECKSUM} validation, then the {@link PipelineStage#BEFORE_DESERIALIZATION} hook
     * point.
     *
     * @return the response to deserialize
     */
    SdkHttpResponse responseReceived(CallState<?, ?> call, AttemptState attempt, SdkHttpResponse response) {
        InterceptorDispatch dispatch = call.plan().dispatch();
        attempt.response(response);
        enter(call, attempt, PipelineStage.AFTER_TRANSMISSION);
        dispatch.afterTransmission(call, attempt, response);
        enter(call, attempt, PipelineStage.MODIFY_HTTP_RESPONSE);
        SdkHttpResponse current = dispatch.modifyHttpResponse(call, attempt, response);
        enter(call, attempt, PipelineStage.MODIFY_RESPONSE_BODY);
        current = dispatch.modifyResponseBody(call, attempt, current);
        if (current != response) {
            // The complete replacement is the input of validation and deserialization (Requirement 11.8).
            attempt.response(current);
        }
        SdkHttpResponse validated = validateChecksum(call, attempt, current);
        enter(call, attempt, PipelineStage.BEFORE_DESERIALIZATION);
        dispatch.beforeDeserialization(call, attempt, validated);
        return validated;
    }

    private SdkHttpResponse validateChecksum(CallState<?, ?> call, AttemptState attempt, SdkHttpResponse response) {
        PipelinePlan<?, ?> plan = call.plan();
        if (!plan.stages().isActive(PipelineStage.RESPONSE_CHECKSUM)) {
            return response;
        }
        enter(call, attempt, PipelineStage.RESPONSE_CHECKSUM);
        ResponseChecksumFeature feature = plan.responseChecksum().orElseThrow();
        boolean enabled = feature.validationEnabled(call.request(), responseValidation(call));
        ResponseChecksumFeature.ValidatedResponse validated = feature.validate(response, enabled);
        attempt.context().put(plan.checksumValidationSlot().orElseThrow(), validated.result());
        if (validated.response() != response) {
            attempt.response(validated.response());
        }
        return validated.response();
    }

    /**
     * {@link PipelineStage#DESERIALIZE}: the modeled output of a successful response, then the
     * {@link PipelineStage#RESPONSE_METADATA} point (no metadata feature attaches anything yet; the protocol's error
     * decoder records the service's request IDs and status on a decoded error).
     *
     * @throws AwsServiceException the decoded error of an error response
     */
    <O extends SerializableStruct> O deserialize(CallState<?, O> call, AttemptState attempt,
                                                 SdkHttpResponse response) {
        enter(call, attempt, PipelineStage.DESERIALIZE);
        OperationProtocol protocol = call.plan().protocol();
        if (protocol.isError(response)) {
            AwsServiceException error = protocol.deserializeError(response);
            enter(call, attempt, PipelineStage.RESPONSE_METADATA);
            throw error;
        }
        O output = protocol.deserializeResponse(response, call.plan().operation().outputBuilder().get());
        enter(call, attempt, PipelineStage.RESPONSE_METADATA);
        return output;
    }

    /**
     * {@link PipelineStage#DESERIALIZE} of a streaming-output call whose payload a response transformer consumes:
     * {@link #deserialize}, except that a successful response is read through
     * {@link OperationProtocol#deserializeStreamingOutput}, which binds the status code and headers and leaves the
     * payload unread. An error response is read in full, as always.
     *
     * @throws AwsServiceException the decoded error of an error response
     */
    <O extends SerializableStruct> O deserializeStreamingOutput(CallState<?, O> call, AttemptState attempt,
                                                                SdkHttpResponse response) {
        enter(call, attempt, PipelineStage.DESERIALIZE);
        OperationProtocol protocol = call.plan().protocol();
        if (protocol.isError(response)) {
            AwsServiceException error = protocol.deserializeError(response);
            enter(call, attempt, PipelineStage.RESPONSE_METADATA);
            throw error;
        }
        O output = protocol.deserializeStreamingOutput(response, call.plan().operation().outputBuilder().get());
        enter(call, attempt, PipelineStage.RESPONSE_METADATA);
        return output;
    }

    /**
     * {@link PipelineStage#DESERIALIZE} of an async event-stream-output call, before its body is read: an error
     * response is read in full and thrown, as always. For a successful response whose other output members are bound
     * to the headers (HTTP binding protocols) it returns the output without its event stream member, through
     * {@link OperationProtocol#deserializeStreamingOutput}, then the {@link PipelineStage#RESPONSE_METADATA} point;
     * for an RPC output it returns {@code null}, and {@link #readInitialResponse} completes the stage once the
     * {@code initial-response} event arrives.
     *
     * @throws AwsServiceException the decoded error of an error response
     */
    <O extends SerializableStruct> O deserializeEventStreamHeaders(CallState<?, O> call, AttemptState attempt,
                                                                   SdkHttpResponse response,
                                                                   OutputEventStream events) {
        enter(call, attempt, PipelineStage.DESERIALIZE);
        OperationProtocol protocol = call.plan().protocol();
        if (protocol.isError(response)) {
            AwsServiceException error = protocol.deserializeError(response);
            enter(call, attempt, PipelineStage.RESPONSE_METADATA);
            throw error;
        }
        if (events.initialResponse()) {
            return null;
        }
        O output = protocol.deserializeStreamingOutput(response, call.plan().operation().outputBuilder().get());
        enter(call, attempt, PipelineStage.RESPONSE_METADATA);
        return output;
    }

    /**
     * The rest of {@link PipelineStage#DESERIALIZE} of an async RPC event-stream-output call: the output read from
     * its {@code initial-response} {@code message}, without its event stream member, then the
     * {@link PipelineStage#RESPONSE_METADATA} point.
     *
     * @throws AwsServiceException if the message is a modeled exception or {@code error} event
     */
    <O extends SerializableStruct> O readInitialResponse(CallState<?, O> call, AttemptState attempt,
                                                         OutputEventStream.InitialResponse message) {
        O output = message.read(call.plan().operation().outputBuilder().get());
        enter(call, attempt, PipelineStage.RESPONSE_METADATA);
        return output;
    }

    /**
     * The failed attempt's hooks, then {@link PipelineStage#CLASSIFY_RETRY}: classifies the attempt's failure,
     * consumes its token and ends the attempt. A {@link RetryDecision.Retry} authorizes exactly one further attempt.
     *
     * <p>The hooks, unless the attempt already reached {@link PipelineStage#AFTER_ATTEMPT} (its after-attempt hook
     * failed): {@link PipelineStage#AFTER_TRANSMISSION} with the failure if the transport failed after transmission
     * began, {@link PipelineStage#MODIFY_ATTEMPT_EXCEPTION} and {@link PipelineStage#AFTER_ATTEMPT}. The failure
     * they leave is recorded as the attempt's {@link AttemptState#failure()}; the classifier receives it, and so must
     * the call's exception mapping if no retry follows.
     *
     * @param transmitted whether the request body may have been (partially) consumed by the transport
     */
    RetryDecision classify(CallState<?, ?> call, AttemptState attempt, Throwable failure, boolean transmitted) {
        // Records the failure once; a completed attempt (already classified or ended) rejects it.
        attempt.failure(failure);
        Throwable current = failure;
        if (attempt.reachAfterAttempt()) {
            InterceptorDispatch dispatch = call.plan().dispatch();
            if (transmitted && attempt.response().isEmpty()) {
                enter(call, attempt, PipelineStage.AFTER_TRANSMISSION);
                current = dispatch.afterTransmissionFailed(call, attempt, current);
            }
            enter(call, attempt, PipelineStage.MODIFY_ATTEMPT_EXCEPTION);
            current = dispatch.modifyAttemptException(call, attempt, current);
            enter(call, attempt, PipelineStage.AFTER_ATTEMPT);
            current = dispatch.afterAttemptFailed(call, attempt, current);
            if (current != failure) {
                attempt.failure(current);
            }
        }
        enter(call, attempt, PipelineStage.CLASSIFY_RETRY);
        AttemptFailure.Builder details = AttemptFailure.builder().failure(current)
                                                       .requestReplayable(!transmitted || replayable(attempt));
        attempt.response().ifPresent(details::response);
        if (current instanceof AwsServiceException service && service.awsErrorDetails() != null) {
            details.errorCode(service.awsErrorDetails().errorCode());
        }
        if (!call.claimToken(attempt.token())) {
            // The caller cancelled the call meanwhile, and its cancellation released the token.
            attempt.complete();
            throw terminalFailure(call);
        }
        RetryDecision decision = runtime.retryStrategy().refreshRetryToken(attempt.token(), details.build());
        attempt.complete();
        recordToken(call, decision instanceof RetryDecision.Retry retry ? retry.token()
                                                                         : ((RetryDecision.Stop) decision).token());
        return decision;
    }

    private static boolean replayable(AttemptState attempt) {
        HttpBody body = attempt.signedRequest().map(SignedRequest::request)
                               .or(attempt::unsignedRequest)
                               .map(SdkHttpRequest::body)
                               .orElse(null);
        return body == null || body.kind() != HttpBody.Kind.STREAMING || body.streamingBody().isReplayable();
    }

    /**
     * Terminal success: the attempt's {@link PipelineStage#AFTER_ATTEMPT} hook point, the attempt's success recorded
     * with the retry strategy (consuming its token), the end of the attempt (its context closes, so the execution
     * hooks after it cannot read it), the {@link PipelineStage#MODIFY_RESPONSE} and
     * {@link PipelineStage#AFTER_EXECUTION} hook points, the call's terminal transition, then
     * {@link PipelineStage#TELEMETRY}.
     *
     * <p>A failure of an {@code afterAttempt} hook is the attempt's failure (thrown to the driver, which classifies
     * it). A failure of a {@code modifyResponse} or {@code afterExecution} hook, after the token was consumed, is the
     * call's: it takes the one mapping path of {@link #fail}, which runs {@code afterExecution} only if it had not
     * started, and is thrown as the call's terminal failure.
     *
     * <p>A caller's cancellation wins only before the terminal transition: one that lands while the
     * {@code modifyResponse} or {@code afterExecution} hooks run still cancels the call (those hooks observed the
     * success, and do not run again), and one that lands after it fails and changes nothing.
     *
     * @return the output after every {@code modifyResponse} replacement
     * @throws RuntimeException the call's terminal failure, if it already ended otherwise (for example cancelled);
     *     its token was then released when it ended
     */
    <O extends SerializableStruct> O succeed(CallState<?, O> call, AttemptState attempt, O output) {
        InterceptorDispatch dispatch = call.plan().dispatch();
        enter(call, attempt, PipelineStage.AFTER_ATTEMPT);
        attempt.reachAfterAttempt();
        dispatch.afterAttemptSucceeded(call, attempt, output);
        if (call.terminal().isTerminal() || !call.claimToken(attempt.token())) {
            // The caller cancelled the call, and its cancellation releases the token this stage did not claim.
            throw terminalFailure(call);
        }
        call.retryToken(runtime.retryStrategy().recordSuccess(attempt.token()));
        // The attempt ends with its after-attempt hooks, as a failed one ends in classify: the execution hooks that
        // follow are not hooks of the attempt, so its context is closed to them (Requirement 11.7).
        attempt.complete();
        O result;
        boolean afterExecution = false;
        try {
            enter(call, null, PipelineStage.MODIFY_RESPONSE);
            result = dispatch.modifyResponse(call, output);
            enter(call, null, PipelineStage.AFTER_EXECUTION);
            afterExecution = true;
            dispatch.afterExecutionSucceeded(call, result);
        } catch (InterceptorException hookFailure) {
            throw fail(call, hookFailure, !afterExecution);
        }
        if (!call.succeed(result)) {
            throw terminalFailure(call);
        }
        enter(call, null, PipelineStage.TELEMETRY);
        return result;
    }

    /**
     * Terminal failure through {@link PipelineStage#MAP_EXCEPTION}, the call's one exception-mapping path
     * (Requirement 10.15; design "Failure rules" 1 and 2): every terminal failure of either driver, before, during or
     * after the attempts, ends here exactly once. It maps {@code failure}, records an unconsumed retry token as
     * failed, runs the {@link PipelineStage#MODIFY_EXCEPTION} and {@link PipelineStage#AFTER_EXECUTION} hook points,
     * makes the call's terminal transition, ends its current attempt, then runs {@link PipelineStage#TELEMETRY}.
     *
     * <p>The mapping, which preserves the original failure and never wraps twice:
     * <ul>
     *   <li>{@link CompletionException} and {@link ExecutionException} wrappers are unwrapped to their cause first, so
     *       framework wrappers never hide the failure.</li>
     *   <li>An {@link SdkException} surfaces unchanged, as the same instance: modeled and unmodeled service errors, an
     *       {@link software.amazon.awssdk.http.HttpTransportException} with its stable transport category and phase,
     *       timeouts, and every typed client, protocol, endpoint, identity, signing and integrity failure.</li>
     *   <li>An {@link InterruptedException} (a sync caller interrupted while the call waited) restores the thread's
     *       interrupt status, cancels the call ({@link CancellationReason#CALLER_CANCELLED}) and surfaces as an
     *       {@link SdkClientException} with it as the cause.</li>
     *   <li>An {@link Error} is not mapped: it becomes the terminal failure and is rethrown as is.</li>
     *   <li>Anything else (an unchecked failure of a component, a checked one, a stray
     *       {@link java.util.concurrent.CancellationException} of a call that was not cancelled) surfaces as one
     *       {@link SdkClientException} naming the operation, with the failure as its cause.</li>
     * </ul>
     * A failure of releasing the token is suppressed onto the surfaced failure. A call that already ended (cancelled
     * by its caller, or completed) keeps its outcome: nothing is mapped or run again, and that outcome is returned.
     *
     * <p>The {@code modifyException} hooks replace the mapped failure; a hook failure replaces it too, with the mapped
     * failure suppressed. The {@code afterExecution} hooks then observe the failure to surface, and their own failures
     * are suppressed onto it. An {@link Error} is never passed to a hook.
     *
     * @return the exception to surface to the caller
     * @throws Error {@code failure} or its unwrapped cause, if it is one
     */
    RuntimeException fail(CallState<?, ?> call, Throwable failure) {
        return fail(call, failure, true);
    }

    /** {@link #fail(CallState, Throwable)}; {@code afterExecution} false when those hooks already started. */
    private RuntimeException fail(CallState<?, ?> call, Throwable failure, boolean afterExecution) {
        Throwable cause = unwrap(failure);
        boolean interrupted = cause instanceof InterruptedException;
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        if (call.terminal().isTerminal()) {
            endAttempt(call);
            return terminalFailure(call);
        }
        enter(call, null, PipelineStage.MAP_EXCEPTION);
        Throwable surfaced = cause instanceof Error ? cause : mapException(call, cause);
        try {
            releaseToken(call);
        } catch (RuntimeException release) {
            if (release != surfaced) {
                surfaced.addSuppressed(release);
            }
        }
        InterceptorDispatch dispatch = call.plan().dispatch();
        enter(call, null, PipelineStage.MODIFY_EXCEPTION);
        if (!interrupted && surfaced instanceof RuntimeException mapped) {
            surfaced = dispatch.modifyException(call, mapped);
        }
        if (afterExecution) {
            enter(call, null, PipelineStage.AFTER_EXECUTION);
            if (!(surfaced instanceof Error)) {
                dispatch.afterExecutionFailed(call, surfaced);
            }
        }
        boolean ended = interrupted ? call.cancel(CancellationReason.CALLER_CANCELLED) : call.fail(surfaced);
        endAttempt(call);
        if (!ended) {
            return terminalFailure(call);
        }
        enter(call, null, PipelineStage.TELEMETRY);
        if (surfaced instanceof Error error) {
            throw error;
        }
        return (RuntimeException) surfaced;
    }

    /**
     * Terminal cancellation, typically on a caller's thread while the driver runs elsewhere: the call's terminal
     * transition, then cancellation of its attempt and in-flight work, then release of its unconsumed retry token.
     * A racing driver stage that already claimed the token consumes it instead, and a driver that records a new token
     * afterwards releases that one itself, so each token is consumed exactly once. Never throws for a failing
     * cancellation handle or token release: their failures are suppressed onto the call's terminal
     * {@link java.util.concurrent.CancellationException}.
     *
     * @return true if this cancelled the call; false if it had already ended, which then changes nothing
     */
    boolean cancel(CallState<?, ?> call, CancellationReason reason) {
        if (!call.cancel(reason)) {
            return false;
        }
        try {
            releaseToken(call);
        } catch (RuntimeException release) {
            call.terminal().failure().orElseThrow().addSuppressed(release);
        }
        return true;
    }

    /** Records the call's unconsumed token, if no stage claimed it, as failed; each token is consumed exactly once. */
    private void releaseToken(CallState<?, ?> call) {
        RetryToken token = call.claimUnconsumedToken();
        if (token != null) {
            call.retryToken(runtime.retryStrategy().recordFailure(token));
        }
    }

    /** Ends the attempt a terminal failure or cancellation left running; idempotent. */
    private static void endAttempt(CallState<?, ?> call) {
        call.currentAttempt().ifPresent(AttemptState::complete);
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable cause = failure;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException)
               && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    /** The mapping rules of {@link #fail} for a failure that is not an {@link Error}. */
    private static RuntimeException mapException(CallState<?, ?> call, Throwable cause) {
        if (cause instanceof SdkException sdk) {
            return sdk;
        }
        if (cause instanceof InterruptedException) {
            return SdkClientException.create("The call of " + call.plan().operation().id() + " was interrupted",
                                             cause);
        }
        String message = cause.getMessage();
        return SdkClientException.create("Unable to execute " + call.plan().operation().id()
                                         + (message == null ? "" : ": " + message), cause);
    }

    /**
     * The surfaced failure of a call that already ended: its mapped failure or its
     * {@link java.util.concurrent.CancellationException}.
     *
     * @throws Error the call's terminal failure, if it is one
     */
    private static RuntimeException terminalFailure(CallState<?, ?> call) {
        Throwable failure = call.terminal().failure().orElseThrow(
            () -> new IllegalStateException("The call of " + call.plan().operation().id() + " already succeeded"));
        if (failure instanceof Error error) {
            throw error;
        }
        return (RuntimeException) failure;
    }
}
