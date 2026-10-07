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

import software.amazon.awssdk.core.context.ContextKey;

/**
 * The semantic stages of the one AWS pipeline (design "Stage order and retry ownership"), declared in the documented
 * lifecycle order.
 *
 * <p>Each stage runs either once per call ({@link ContextKey.Scope#EXECUTION}) or once per attempt
 * ({@link ContextKey.Scope#ATTEMPT}, inside the retry owner). {@link Kind#CORE} stages are active in every plan;
 * {@link Kind#FEATURE} stages are active only when the operation's plan has the feature, and {@link Kind#HOOK}
 * stages only when a configured interceptor implements the hook. A plan records its active stages once, in
 * {@link StageMetadata}, so an inactive stage costs the drivers one bit test and no dispatch, context object, timer
 * or counter (Requirement 10.10).
 *
 * <p>Every trace of a call follows the declaration order within each scope (Requirement 10.2; Property 29): the
 * execution stages up to {@link #ACQUIRE_RETRY_TOKEN}, then per attempt {@link #BEFORE_ATTEMPT} through
 * {@link #AFTER_ATTEMPT}, then the terminal stages. A failed attempt continues from the failed stage directly to
 * {@link #MODIFY_ATTEMPT_EXCEPTION}, {@link #AFTER_ATTEMPT} and {@link #CLASSIFY_RETRY}, skipping every dependent
 * stage in between (Requirement 10.14; an {@link Error} is not classified and continues directly to
 * {@link #MAP_EXCEPTION}); a granted retry token then starts exactly one next attempt at
 * {@link #BEFORE_ATTEMPT} (Requirement 10.6). Terminal success runs {@link #MODIFY_RESPONSE}; terminal failure runs
 * {@link #MAP_EXCEPTION}, the call's one exception-mapping path, then {@link #MODIFY_EXCEPTION} (Requirement 10.15);
 * both end with {@link #AFTER_EXECUTION} and {@link #TELEMETRY}.
 */
public enum PipelineStage {
    /** Validate the invocation and merge the call's override deltas. */
    VALIDATE(ContextKey.Scope.EXECUTION, Kind.CORE),
    /** {@code beforeExecution} hooks. */
    BEFORE_EXECUTION(ContextKey.Scope.EXECUTION, Kind.HOOK),
    /** {@code modifyRequest} hooks; the replacement is reused by every attempt. */
    MODIFY_REQUEST(ContextKey.Scope.EXECUTION, Kind.HOOK),
    /** One-per-call idempotency token injection. */
    IDEMPOTENCY_TOKEN(ContextKey.Scope.EXECUTION, Kind.FEATURE),
    /** Acquire the retry token of attempt 1. */
    ACQUIRE_RETRY_TOKEN(ContextKey.Scope.EXECUTION, Kind.CORE),
    /** {@code beforeAttempt} hooks. */
    BEFORE_ATTEMPT(ContextKey.Scope.ATTEMPT, Kind.HOOK),
    /** Resolve the attempt's endpoint. */
    RESOLVE_ENDPOINT(ContextKey.Scope.ATTEMPT, Kind.CORE),
    /** Select the attempt's auth scheme. */
    SELECT_AUTH_SCHEME(ContextKey.Scope.ATTEMPT, Kind.CORE),
    /** Resolve or refresh the attempt's identity. */
    RESOLVE_IDENTITY(ContextKey.Scope.ATTEMPT, Kind.CORE),
    /** Schema-serialize and HTTP-bind the request. */
    SERIALIZE(ContextKey.Scope.ATTEMPT, Kind.CORE),
    /** {@code afterSerialization} hooks. */
    AFTER_SERIALIZATION(ContextKey.Scope.ATTEMPT, Kind.HOOK),
    /** Request compression and content encoding; before the checksum, which must cover the compressed payload. */
    REQUEST_COMPRESSION(ContextKey.Scope.ATTEMPT, Kind.FEATURE),
    /** Request checksum calculation. */
    REQUEST_CHECKSUM(ContextKey.Scope.ATTEMPT, Kind.FEATURE),
    /** {@code modifyBeforeSigning} hooks; the output is signed. */
    MODIFY_BEFORE_SIGNING(ContextKey.Scope.ATTEMPT, Kind.HOOK),
    /** Sign the request (or event stream). */
    SIGN(ContextKey.Scope.ATTEMPT, Kind.CORE),
    /** {@code modifyAfterSigning} hooks. */
    MODIFY_AFTER_SIGNING(ContextKey.Scope.ATTEMPT, Kind.HOOK),
    /** {@code beforeTransmission} hooks. */
    BEFORE_TRANSMISSION(ContextKey.Scope.ATTEMPT, Kind.HOOK),
    /** HTTP acquisition, transmission and response consumption. */
    TRANSMIT(ContextKey.Scope.ATTEMPT, Kind.CORE),
    /** {@code afterTransmission} hooks. */
    AFTER_TRANSMISSION(ContextKey.Scope.ATTEMPT, Kind.HOOK),
    /** {@code modifyHttpResponse} hooks. */
    MODIFY_HTTP_RESPONSE(ContextKey.Scope.ATTEMPT, Kind.HOOK),
    /** {@code modifyResponseBody} hooks. */
    MODIFY_RESPONSE_BODY(ContextKey.Scope.ATTEMPT, Kind.HOOK),
    /** Response checksum validation. */
    RESPONSE_CHECKSUM(ContextKey.Scope.ATTEMPT, Kind.FEATURE),
    /** {@code beforeDeserialization} hooks. */
    BEFORE_DESERIALIZATION(ContextKey.Scope.ATTEMPT, Kind.HOOK),
    /** Schema-deserialize the output or decode the error. */
    DESERIALIZE(ContextKey.Scope.ATTEMPT, Kind.CORE),
    /**
     * Attach the attempt's response metadata (request IDs, status, attempt count, transport facts) to the deserialized
     * output, so every later hook observes it; active in the plan of every operation whose output is a generated AWS
     * response ({@code AwsResponse}). The protocol's error decoder records a decoded error's IDs, status and code, and
     * the exception-mapping path records the call's attempt count and transport facts on the surfaced exception.
     */
    RESPONSE_METADATA(ContextKey.Scope.ATTEMPT, Kind.FEATURE),
    /** {@code modifyAttemptException} hooks, before classification. */
    MODIFY_ATTEMPT_EXCEPTION(ContextKey.Scope.ATTEMPT, Kind.HOOK),
    /** {@code afterAttempt} hooks. */
    AFTER_ATTEMPT(ContextKey.Scope.ATTEMPT, Kind.HOOK),
    /** Retry classification and token refresh of a failed attempt. */
    CLASSIFY_RETRY(ContextKey.Scope.ATTEMPT, Kind.CORE),
    /** {@code modifyResponse} hooks, once on terminal success. */
    MODIFY_RESPONSE(ContextKey.Scope.EXECUTION, Kind.HOOK),
    /** The one exception-mapping path of a terminal failure. */
    MAP_EXCEPTION(ContextKey.Scope.EXECUTION, Kind.CORE),
    /** {@code modifyException} hooks, once on terminal failure. */
    MODIFY_EXCEPTION(ContextKey.Scope.EXECUTION, Kind.HOOK),
    /** {@code afterExecution} hooks, once per call. */
    AFTER_EXECUTION(ContextKey.Scope.EXECUTION, Kind.HOOK),
    /** The call's terminal telemetry record; active only when a telemetry publisher is configured. */
    TELEMETRY(ContextKey.Scope.EXECUTION, Kind.FEATURE);

    /** How a stage becomes active in a plan. */
    public enum Kind {
        /** Active in every plan. */
        CORE,
        /** Active when the operation's plan has the feature. */
        FEATURE,
        /** Active when an interceptor implements the hook. */
        HOOK
    }

    private final ContextKey.Scope scope;
    private final Kind kind;

    PipelineStage(ContextKey.Scope scope, Kind kind) {
        this.scope = scope;
        this.kind = kind;
    }

    /** Whether the stage runs once per call or once per attempt. */
    public ContextKey.Scope scope() {
        return scope;
    }

    /** How the stage becomes active. */
    public Kind kind() {
        return kind;
    }
}
