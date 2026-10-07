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

import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.auth.api.SelectedAuthScheme;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.checksums.ResolvedRequestChecksum;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.retries.RetryToken;

/**
 * The mutable state of one attempt of one call (design "{@code AttemptState}"; Requirements 10.8, 10.9, 11.7): the
 * retry token that authorized it, its fresh {@link AttemptContext}, its child {@link CancellationScope}, and the
 * values its stages produce: endpoint, auth scheme, identity, unsigned and signed request, response and failure.
 *
 * <p>Endpoint, auth scheme and identity are stored in the attempt context's fixed {@link PipelineKeys} slots, so
 * stages and interceptors observe one value, and each is written once by its producing stage: before that it is
 * empty, afterwards it is visible to every later stage and hook of this attempt only. {@link #complete()} ends the
 * attempt: its context closes, its cancellation scope is released, and every further write throws, so nothing of the
 * attempt survives into the next one. The number is the one-based attempt number of the authorizing token.
 *
 * <p>Created only by {@link CallState#startAttempt(RetryToken)}; confined to the call's driver.
 */
public final class AttemptState {
    private final int number;
    private final RetryToken token;
    private final AttemptContext context;
    private final CancellationScope cancellation;
    private SdkHttpRequest unsignedRequest;
    /** The header checksum of the streaming payload the request checksum stage left to compute before signing. */
    private ResolvedRequestChecksum pendingChecksum;
    private SignedRequest signedRequest;
    private SdkHttpResponse response;
    private Throwable failure;
    private boolean afterAttemptReached;
    private volatile boolean completed;
    /** The call's telemetry state, which records this attempt's end; null when the plan has no telemetry. */
    private final CallTelemetry telemetry;
    private final TerminalState<?> callTerminal;

    AttemptState(RetryToken token, ExecutionContext execution, CancellationScope cancellation,
                 CallTelemetry telemetry, TerminalState<?> callTerminal) {
        this.number = token.attempt();
        this.token = token;
        this.context = new AttemptContext(execution, number);
        this.cancellation = cancellation;
        this.telemetry = telemetry;
        this.callTerminal = callTerminal;
        if (telemetry != null) {
            telemetry.attemptStarted();
        }
    }

    /** The one-based attempt number. */
    public int number() {
        return number;
    }

    /** The retry token that authorized this attempt. */
    public RetryToken token() {
        return token;
    }

    /** The attempt-scoped context. */
    public AttemptContext context() {
        return context;
    }

    /** The attempt's cancellation scope, a child of the call's. */
    public CancellationScope cancellation() {
        return cancellation;
    }

    /** Whether the attempt has completed. */
    public boolean isCompleted() {
        return completed;
    }

    /** The resolved endpoint, once the endpoint stage has run. */
    public Optional<Endpoint> endpoint() {
        return Optional.ofNullable((Endpoint) context.getBuiltIn(PipelineKeys.ENDPOINT_SLOT));
    }

    /** The selected auth scheme, once the auth-scheme stage has run. */
    public Optional<SelectedAuthScheme<?>> authScheme() {
        return Optional.ofNullable((SelectedAuthScheme<?>) context.getBuiltIn(PipelineKeys.AUTH_SCHEME_SLOT));
    }

    /** The resolved identity, once the identity stage has run. */
    public Optional<Identity> identity() {
        return Optional.ofNullable((Identity) context.getBuiltIn(PipelineKeys.IDENTITY_SLOT));
    }

    /** The serialized, unsigned request, once serialization has run; replaced by later request stages. */
    public Optional<SdkHttpRequest> unsignedRequest() {
        return Optional.ofNullable(unsignedRequest);
    }

    /** The signed request, once signing has run. */
    public Optional<SignedRequest> signedRequest() {
        return Optional.ofNullable(signedRequest);
    }

    /** The received response, once transmission produced one. */
    public Optional<SdkHttpResponse> response() {
        return Optional.ofNullable(response);
    }

    /** The attempt's failure, once a stage failed. */
    public Optional<Throwable> failure() {
        return Optional.ofNullable(failure);
    }

    void endpoint(Endpoint endpoint) {
        writeOnce(PipelineKeys.ENDPOINT_SLOT, Objects.requireNonNull(endpoint, "endpoint"), "endpoint");
    }

    void authScheme(SelectedAuthScheme<?> authScheme) {
        writeOnce(PipelineKeys.AUTH_SCHEME_SLOT, Objects.requireNonNull(authScheme, "authScheme"), "auth scheme");
    }

    void identity(Identity identity) {
        writeOnce(PipelineKeys.IDENTITY_SLOT, Objects.requireNonNull(identity, "identity"), "identity");
    }

    void unsignedRequest(SdkHttpRequest request) {
        requireRunning();
        this.unsignedRequest = Objects.requireNonNull(request, "request");
    }

    /** The pending header checksum of the streaming payload, or {@code null}. */
    ResolvedRequestChecksum pendingChecksum() {
        return pendingChecksum;
    }

    void pendingChecksum(ResolvedRequestChecksum checksum) {
        requireRunning();
        this.pendingChecksum = checksum;
    }

    void signedRequest(SignedRequest request) {
        requireRunning();
        this.signedRequest = Objects.requireNonNull(request, "request");
    }

    void response(SdkHttpResponse response) {
        requireRunning();
        this.response = Objects.requireNonNull(response, "response");
    }

    void failure(Throwable failure) {
        requireRunning();
        this.failure = Objects.requireNonNull(failure, "failure");
    }

    /**
     * Marks that the attempt reached its {@link PipelineStage#AFTER_ATTEMPT} point, so its after-attempt hooks run
     * at most once.
     *
     * @return true if this call marked it
     */
    boolean reachAfterAttempt() {
        if (afterAttemptReached) {
            return false;
        }
        afterAttemptReached = true;
        return true;
    }

    /**
     * Ends the attempt: closes its context, releases its cancellation scope and, with telemetry, emits its terminal
     * record exactly once (Requirement 21.3). Idempotent.
     *
     * @return true if this call ended the attempt
     */
    boolean complete() {
        if (completed) {
            return false;
        }
        completed = true;
        context.close();
        cancellation.close();
        if (telemetry != null) {
            telemetry.attemptEnded(this, callTerminal);
        }
        return true;
    }

    private void writeOnce(int slot, Object value, String name) {
        requireRunning();
        if (context.getBuiltIn(slot) != null) {
            throw new IllegalStateException("The " + name + " of attempt " + number + " is already resolved");
        }
        context.putBuiltIn(slot, value);
    }

    private void requireRunning() {
        if (completed) {
            throw new IllegalStateException("Attempt " + number + " has completed");
        }
    }

    @Override
    public String toString() {
        return "AttemptState(" + number + (completed ? ", completed" : "") + ")";
    }
}
