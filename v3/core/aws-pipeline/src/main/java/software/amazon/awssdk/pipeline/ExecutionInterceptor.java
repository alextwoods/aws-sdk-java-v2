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

import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.smithy.java.core.schema.SerializableStruct;

/**
 * A native V3 interceptor of the AWS pipeline's lifecycle (Requirements 11.1 to 11.10, 11.13, 11.14; design
 * "Interceptors and plugins"). Each hook is a default no-op, so a client's plans precompute, per hook, the array of
 * the interceptors that override it; a hook no interceptor overrides costs no dispatch and no context object.
 *
 * <p>Hooks run in lifecycle order at their {@link PipelineStage}, in registration order across interceptors. The
 * once-per-call hooks ({@link HookContext.Execution}) are {@link #beforeExecution}, {@link #modifyRequest},
 * {@link #modifyResponse}, {@link #modifyException} and {@link #afterExecution}; every other hook runs once per
 * attempt ({@link HookContext.Attempt}), inside the retry owner. Endpoint, auth scheme and identity are visible from
 * {@link #afterSerialization} on.
 *
 * <p>Modify hooks return a replacement (returning the context's value keeps it), which the next interceptor and then
 * the next stage receive. The pipeline validates every replacement: a modeled value must be of the same operation
 * shape and Java type, a transport message must be non-null, a body must stay replayable if the body it replaces was
 * replayable, and a signed request must keep the method and target that were signed. An incompatible replacement, or
 * an exception a hook throws, fails the attempt (for attempt hooks) or the call (for execution hooks) with an
 * {@link InterceptorException} naming the interceptor class and hook; a thrown exception is its cause. Such failures
 * take the same paths as any other: an attempt failure is classified for retry, and every terminal failure passes
 * through the call's one exception mapping, {@link #modifyException} and {@link #afterExecution}.
 *
 * <p>{@link #afterAttempt} and {@link #afterExecution} run for every overriding interceptor even if an earlier one
 * throws; a later failure is suppressed onto the first. A {@link java.lang.Error} is never passed to an interceptor.
 *
 * <p>Interceptors are client-constant: implementations must be thread safe and keep per-call state only in the typed
 * contexts. Hooks run on the call's driver (the caller's thread, or the async completion executor), never on a
 * transport event loop.
 */
public interface ExecutionInterceptor {
    /** Once per call, after validation, before the request may be modified. */
    default void beforeExecution(HookContext.BeforeExecution context) {
    }

    /**
     * Once per call, before the idempotency token and the first attempt: replaces the modeled request every attempt
     * serializes. The replacement must be of the operation's input shape and the request's Java type.
     */
    default SerializableStruct modifyRequest(HookContext.ModifyRequest context) {
        return context.request();
    }

    /** Once per attempt, before its endpoint is resolved. */
    default void beforeAttempt(HookContext.BeforeAttempt context) {
    }

    /** Once per attempt, after serialization. */
    default void afterSerialization(HookContext.AfterSerialization context) {
    }

    /**
     * Once per attempt, after the request features: replaces the unsigned transport request, including its body; the
     * replacement is what is signed (Requirement 11.4).
     */
    default SdkHttpRequest modifyBeforeSigning(HookContext.ModifyBeforeSigning context) {
        return context.httpRequest();
    }

    /**
     * Once per attempt, after signing: replaces the signed request. The replacement must keep the signed method,
     * scheme, host, port and path.
     */
    default SignedRequest modifyAfterSigning(HookContext.ModifyAfterSigning context) {
        return context.signedRequest();
    }

    /** Once per attempt, immediately before transmission. */
    default void beforeTransmission(HookContext.BeforeTransmission context) {
    }

    /** Once per transmitted attempt, with the received response or the transport's failure. */
    default void afterTransmission(HookContext.AfterTransmission context) {
    }

    /** Once per received response: replaces the transport response, including its body. */
    default SdkHttpResponse modifyHttpResponse(HookContext.ModifyHttpResponse context) {
        return context.httpResponse();
    }

    /**
     * Once per received response, after {@link #modifyHttpResponse}: replaces the response body; the complete
     * replacement is what checksum validation and deserialization read (Requirement 11.8).
     */
    default HttpBody modifyResponseBody(HookContext.ModifyResponseBody context) {
        return context.body();
    }

    /** Once per received response, after checksum validation, before deserialization. */
    default void beforeDeserialization(HookContext.BeforeDeserialization context) {
    }

    /**
     * Once per failed attempt, before retry classification: replaces the attempt's failure, which the classifier (and,
     * without a retry, the call's exception mapping) receives. The replacement must be an {@link Exception}.
     */
    default Throwable modifyAttemptException(HookContext.ModifyAttemptException context) {
        return context.failure();
    }

    /** Once per attempt, as it ends with its output or its failure. */
    default void afterAttempt(HookContext.AfterAttempt context) {
    }

    /**
     * Once on terminal success: replaces the modeled output returned to the caller. The replacement must be of the
     * operation's output shape and the output's Java type.
     */
    default SerializableStruct modifyResponse(HookContext.ModifyResponse context) {
        return context.response();
    }

    /**
     * Once on terminal failure, after the call's exception mapping: replaces the failure surfaced to the caller. The
     * replacement must be a {@link RuntimeException}.
     */
    default Throwable modifyException(HookContext.ModifyException context) {
        return context.failure();
    }

    /** Exactly once per call that reached its terminal stages, with its output or its surfaced failure. */
    default void afterExecution(HookContext.AfterExecution context) {
    }
}
