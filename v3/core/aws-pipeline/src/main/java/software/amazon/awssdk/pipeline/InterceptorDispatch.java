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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;

/**
 * The interceptor dispatch plan of one {@link PipelinePlan} (Requirements 10.10, 11.3, 11.9, 11.10, 11.13, 11.14;
 * design "Interceptors and plugins"): per hook, the array of the plan's interceptors that override it, in registration
 * order, computed once when the plan is built. The plan activates exactly the hook stages whose array is non-empty.
 *
 * <p>Each dispatch method returns at once, without allocating, when its array is empty; otherwise it creates the
 * hook's {@link HookContext} once (and again only when a modify hook replaced the value), calls each interceptor,
 * validates each replacement, and turns a thrown exception or an incompatible replacement into an
 * {@link InterceptorException} naming the interceptor class and hook. Override detection uses reflection at plan
 * construction only; calls never reflect. Immutable and thread safe.
 */
final class InterceptorDispatch {
    // Declared before NONE, whose construction reads it.
    private static final ExecutionInterceptor[] NO_INTERCEPTORS = new ExecutionInterceptor[0];

    private static final String BEFORE_EXECUTION = "beforeExecution";
    private static final String MODIFY_REQUEST = "modifyRequest";
    private static final String BEFORE_ATTEMPT = "beforeAttempt";
    private static final String AFTER_SERIALIZATION = "afterSerialization";
    private static final String MODIFY_BEFORE_SIGNING = "modifyBeforeSigning";
    private static final String MODIFY_AFTER_SIGNING = "modifyAfterSigning";
    private static final String BEFORE_TRANSMISSION = "beforeTransmission";
    private static final String AFTER_TRANSMISSION = "afterTransmission";
    private static final String MODIFY_HTTP_RESPONSE = "modifyHttpResponse";
    private static final String MODIFY_RESPONSE_BODY = "modifyResponseBody";
    private static final String BEFORE_DESERIALIZATION = "beforeDeserialization";
    private static final String MODIFY_ATTEMPT_EXCEPTION = "modifyAttemptException";
    private static final String AFTER_ATTEMPT = "afterAttempt";
    private static final String MODIFY_RESPONSE = "modifyResponse";
    private static final String MODIFY_EXCEPTION = "modifyException";
    private static final String AFTER_EXECUTION = "afterExecution";

    static final InterceptorDispatch NONE = new InterceptorDispatch(List.of());

    private final List<ExecutionInterceptor> interceptors;
    private final ExecutionInterceptor[] beforeExecution;
    private final ExecutionInterceptor[] modifyRequest;
    private final ExecutionInterceptor[] beforeAttempt;
    private final ExecutionInterceptor[] afterSerialization;
    private final ExecutionInterceptor[] modifyBeforeSigning;
    private final ExecutionInterceptor[] modifyAfterSigning;
    private final ExecutionInterceptor[] beforeTransmission;
    private final ExecutionInterceptor[] afterTransmission;
    private final ExecutionInterceptor[] modifyHttpResponse;
    private final ExecutionInterceptor[] modifyResponseBody;
    private final ExecutionInterceptor[] beforeDeserialization;
    private final ExecutionInterceptor[] modifyAttemptException;
    private final ExecutionInterceptor[] afterAttempt;
    private final ExecutionInterceptor[] modifyResponse;
    private final ExecutionInterceptor[] modifyException;
    private final ExecutionInterceptor[] afterExecution;

    private InterceptorDispatch(List<ExecutionInterceptor> interceptors) {
        this.interceptors = List.copyOf(interceptors);
        this.beforeExecution = select(BEFORE_EXECUTION, HookContext.BeforeExecution.class);
        this.modifyRequest = select(MODIFY_REQUEST, HookContext.ModifyRequest.class);
        this.beforeAttempt = select(BEFORE_ATTEMPT, HookContext.BeforeAttempt.class);
        this.afterSerialization = select(AFTER_SERIALIZATION, HookContext.AfterSerialization.class);
        this.modifyBeforeSigning = select(MODIFY_BEFORE_SIGNING, HookContext.ModifyBeforeSigning.class);
        this.modifyAfterSigning = select(MODIFY_AFTER_SIGNING, HookContext.ModifyAfterSigning.class);
        this.beforeTransmission = select(BEFORE_TRANSMISSION, HookContext.BeforeTransmission.class);
        this.afterTransmission = select(AFTER_TRANSMISSION, HookContext.AfterTransmission.class);
        this.modifyHttpResponse = select(MODIFY_HTTP_RESPONSE, HookContext.ModifyHttpResponse.class);
        this.modifyResponseBody = select(MODIFY_RESPONSE_BODY, HookContext.ModifyResponseBody.class);
        this.beforeDeserialization = select(BEFORE_DESERIALIZATION, HookContext.BeforeDeserialization.class);
        this.modifyAttemptException = select(MODIFY_ATTEMPT_EXCEPTION, HookContext.ModifyAttemptException.class);
        this.afterAttempt = select(AFTER_ATTEMPT, HookContext.AfterAttempt.class);
        this.modifyResponse = select(MODIFY_RESPONSE, HookContext.ModifyResponse.class);
        this.modifyException = select(MODIFY_EXCEPTION, HookContext.ModifyException.class);
        this.afterExecution = select(AFTER_EXECUTION, HookContext.AfterExecution.class);
    }

    /** The dispatch plan of {@code interceptors}, in their order; {@link #NONE} when there are none. */
    static InterceptorDispatch of(List<ExecutionInterceptor> interceptors) {
        return interceptors.isEmpty() ? NONE : new InterceptorDispatch(interceptors);
    }

    /** The interceptors, in registration order. Immutable. */
    List<ExecutionInterceptor> interceptors() {
        return interceptors;
    }

    /** The hook stages at least one interceptor implements. */
    Set<PipelineStage> activeStages() {
        EnumSet<PipelineStage> active = EnumSet.noneOf(PipelineStage.class);
        addIf(active, beforeExecution, PipelineStage.BEFORE_EXECUTION);
        addIf(active, modifyRequest, PipelineStage.MODIFY_REQUEST);
        addIf(active, beforeAttempt, PipelineStage.BEFORE_ATTEMPT);
        addIf(active, afterSerialization, PipelineStage.AFTER_SERIALIZATION);
        addIf(active, modifyBeforeSigning, PipelineStage.MODIFY_BEFORE_SIGNING);
        addIf(active, modifyAfterSigning, PipelineStage.MODIFY_AFTER_SIGNING);
        addIf(active, beforeTransmission, PipelineStage.BEFORE_TRANSMISSION);
        addIf(active, afterTransmission, PipelineStage.AFTER_TRANSMISSION);
        addIf(active, modifyHttpResponse, PipelineStage.MODIFY_HTTP_RESPONSE);
        addIf(active, modifyResponseBody, PipelineStage.MODIFY_RESPONSE_BODY);
        addIf(active, beforeDeserialization, PipelineStage.BEFORE_DESERIALIZATION);
        addIf(active, modifyAttemptException, PipelineStage.MODIFY_ATTEMPT_EXCEPTION);
        addIf(active, afterAttempt, PipelineStage.AFTER_ATTEMPT);
        addIf(active, modifyResponse, PipelineStage.MODIFY_RESPONSE);
        addIf(active, modifyException, PipelineStage.MODIFY_EXCEPTION);
        addIf(active, afterExecution, PipelineStage.AFTER_EXECUTION);
        return active;
    }

    // Execution hooks before the first attempt.

    void beforeExecution(CallState<?, ?> call) {
        if (beforeExecution.length == 0) {
            return;
        }
        HookContext.BeforeExecution context =
            new HookContext.BeforeExecution(call.plan().operation(), call.options(), call.executionContext(),
                                            call.request());
        observe(call, beforeExecution, BEFORE_EXECUTION, context, ExecutionInterceptor::beforeExecution);
    }

    /** The modeled request after every {@code modifyRequest} replacement; the call's request if none replaced it. */
    @SuppressWarnings("unchecked")
    <I extends SerializableStruct> I modifyRequest(CallState<I, ?> call) {
        I request = call.request();
        if (modifyRequest.length == 0) {
            return request;
        }
        HookContext.ModifyRequest context =
            new HookContext.ModifyRequest(call.plan().operation(), call.options(), call.executionContext(), request);
        // Validated to be of the request's own class, so the cast is safe.
        return (I) InterceptorDispatch.<HookContext.ModifyRequest, SerializableStruct>modify(
                          call, modifyRequest, MODIFY_REQUEST, context, request, ExecutionInterceptor::modifyRequest,
                          InterceptorDispatch::checkInput,
                          (c, value) -> new HookContext.ModifyRequest(c.operation(), c.options(), c.execution(),
                                                                      value));
    }

    // Attempt hooks.

    void beforeAttempt(CallState<?, ?> call, AttemptState attempt) {
        if (beforeAttempt.length == 0) {
            return;
        }
        HookContext.BeforeAttempt context =
            new HookContext.BeforeAttempt(call.plan().operation(), call.options(), attempt.context(), call.request());
        observe(call, beforeAttempt, BEFORE_ATTEMPT, context, ExecutionInterceptor::beforeAttempt);
    }

    void afterSerialization(CallState<?, ?> call, AttemptState attempt, SdkHttpRequest request) {
        if (afterSerialization.length == 0) {
            return;
        }
        HookContext.AfterSerialization context =
            new HookContext.AfterSerialization(call.plan().operation(), call.options(), attempt.context(),
                                               call.request(), request);
        observe(call, afterSerialization, AFTER_SERIALIZATION, context, ExecutionInterceptor::afterSerialization);
    }

    /** The request to sign: {@code request} after every {@code modifyBeforeSigning} replacement. */
    SdkHttpRequest modifyBeforeSigning(CallState<?, ?> call, AttemptState attempt, SdkHttpRequest request) {
        if (modifyBeforeSigning.length == 0) {
            return request;
        }
        HookContext.ModifyBeforeSigning context =
            new HookContext.ModifyBeforeSigning(call.plan().operation(), call.options(), attempt.context(),
                                                call.request(), request);
        return modify(call, modifyBeforeSigning, MODIFY_BEFORE_SIGNING, context, request,
                      ExecutionInterceptor::modifyBeforeSigning, InterceptorDispatch::checkUnsignedRequest,
                      (c, value) -> new HookContext.ModifyBeforeSigning(c.operation(), c.options(), c.attempt(),
                                                                        c.request(), value));
    }

    /** The request to transmit: {@code signed} after every {@code modifyAfterSigning} replacement. */
    SignedRequest modifyAfterSigning(CallState<?, ?> call, AttemptState attempt, SignedRequest signed) {
        if (modifyAfterSigning.length == 0) {
            return signed;
        }
        HookContext.ModifyAfterSigning context =
            new HookContext.ModifyAfterSigning(call.plan().operation(), call.options(), attempt.context(),
                                               call.request(), signed);
        return modify(call, modifyAfterSigning, MODIFY_AFTER_SIGNING, context, signed,
                      ExecutionInterceptor::modifyAfterSigning, InterceptorDispatch::checkSignedRequest,
                      (c, value) -> new HookContext.ModifyAfterSigning(c.operation(), c.options(), c.attempt(),
                                                                       c.request(), value));
    }

    void beforeTransmission(CallState<?, ?> call, AttemptState attempt, SignedRequest signed) {
        if (beforeTransmission.length == 0) {
            return;
        }
        HookContext.BeforeTransmission context =
            new HookContext.BeforeTransmission(call.plan().operation(), call.options(), attempt.context(),
                                               call.request(), signed);
        observe(call, beforeTransmission, BEFORE_TRANSMISSION, context, ExecutionInterceptor::beforeTransmission);
    }

    /** {@code afterTransmission} with the received {@code response}; the first hook failure is thrown. */
    void afterTransmission(CallState<?, ?> call, AttemptState attempt, SdkHttpResponse response) {
        if (afterTransmission.length == 0) {
            return;
        }
        observe(call, afterTransmission, AFTER_TRANSMISSION,
                afterTransmissionContext(call, attempt, Optional.of(response), Optional.empty()),
                ExecutionInterceptor::afterTransmission);
    }

    /**
     * {@code afterTransmission} after the transport failed with {@code failure}; every interceptor runs.
     *
     * @return the attempt's failure: {@code failure}, or the first hook failure with {@code failure} suppressed
     */
    Throwable afterTransmissionFailed(CallState<?, ?> call, AttemptState attempt, Throwable failure) {
        if (afterTransmission.length == 0) {
            return failure;
        }
        HookContext.AfterTransmission context =
            afterTransmissionContext(call, attempt, Optional.empty(), Optional.of(failure));
        return replacingFailure(failure, observeAll(call, afterTransmission, AFTER_TRANSMISSION, context,
                                                    ExecutionInterceptor::afterTransmission));
    }

    private static HookContext.AfterTransmission afterTransmissionContext(CallState<?, ?> call, AttemptState attempt,
                                                                          Optional<SdkHttpResponse> response,
                                                                          Optional<Throwable> failure) {
        return new HookContext.AfterTransmission(call.plan().operation(), call.options(), attempt.context(),
                                                 call.request(), attempt.signedRequest().orElse(null), response,
                                                 failure);
    }

    /** The response to continue with: {@code response} after every {@code modifyHttpResponse} replacement. */
    SdkHttpResponse modifyHttpResponse(CallState<?, ?> call, AttemptState attempt, SdkHttpResponse response) {
        if (modifyHttpResponse.length == 0) {
            return response;
        }
        HookContext.ModifyHttpResponse context =
            new HookContext.ModifyHttpResponse(call.plan().operation(), call.options(), attempt.context(),
                                               call.request(), response);
        return modify(call, modifyHttpResponse, MODIFY_HTTP_RESPONSE, context, response,
                      ExecutionInterceptor::modifyHttpResponse, InterceptorDispatch::checkResponse,
                      (c, value) -> new HookContext.ModifyHttpResponse(c.operation(), c.options(), c.attempt(),
                                                                       c.request(), value));
    }

    /** {@code response} with its body after every {@code modifyResponseBody} replacement; itself if unchanged. */
    SdkHttpResponse modifyResponseBody(CallState<?, ?> call, AttemptState attempt, SdkHttpResponse response) {
        if (modifyResponseBody.length == 0) {
            return response;
        }
        HttpBody body = response.body();
        HookContext.ModifyResponseBody context =
            new HookContext.ModifyResponseBody(call.plan().operation(), call.options(), attempt.context(),
                                               call.request(), response, body);
        HttpBody replaced = modify(call, modifyResponseBody, MODIFY_RESPONSE_BODY, context, body,
                                   ExecutionInterceptor::modifyResponseBody, InterceptorDispatch::checkBody,
                                   (c, value) -> new HookContext.ModifyResponseBody(c.operation(), c.options(),
                                                                                    c.attempt(), c.request(),
                                                                                    c.httpResponse(), value));
        return replaced == body ? response : response.toBuilder().body(replaced).build();
    }

    void beforeDeserialization(CallState<?, ?> call, AttemptState attempt, SdkHttpResponse response) {
        if (beforeDeserialization.length == 0) {
            return;
        }
        HookContext.BeforeDeserialization context =
            new HookContext.BeforeDeserialization(call.plan().operation(), call.options(), attempt.context(),
                                                  call.request(), response);
        observe(call, beforeDeserialization, BEFORE_DESERIALIZATION, context,
                ExecutionInterceptor::beforeDeserialization);
    }

    /**
     * The attempt's failure after every {@code modifyAttemptException} replacement. A hook failure ends the chain and
     * becomes the failure, with the failure it replaced suppressed.
     */
    Throwable modifyAttemptException(CallState<?, ?> call, AttemptState attempt, Throwable failure) {
        if (modifyAttemptException.length == 0) {
            return failure;
        }
        HookContext.ModifyAttemptException context =
            new HookContext.ModifyAttemptException(call.plan().operation(), call.options(), attempt.context(),
                                                   call.request(), failure);
        try {
            return modify(call, modifyAttemptException, MODIFY_ATTEMPT_EXCEPTION, context, failure,
                          ExecutionInterceptor::modifyAttemptException, InterceptorDispatch::checkAttemptFailure,
                          (c, value) -> new HookContext.ModifyAttemptException(c.operation(), c.options(),
                                                                               c.attempt(), c.request(), value));
        } catch (InterceptorException hookFailure) {
            return replacingFailure(failure, hookFailure);
        }
    }

    /** {@code afterAttempt} of an attempt that produced {@code output}; the first hook failure is thrown. */
    void afterAttemptSucceeded(CallState<?, ?> call, AttemptState attempt, SerializableStruct output) {
        if (afterAttempt.length == 0) {
            return;
        }
        HookContext.AfterAttempt context =
            new HookContext.AfterAttempt(call.plan().operation(), call.options(), attempt.context(), call.request(),
                                         Optional.of(output), Optional.empty());
        InterceptorException hookFailure =
            observeAll(call, afterAttempt, AFTER_ATTEMPT, context, ExecutionInterceptor::afterAttempt);
        if (hookFailure != null) {
            throw hookFailure;
        }
    }

    /**
     * {@code afterAttempt} of an attempt that failed with {@code failure}.
     *
     * @return the attempt's failure: {@code failure}, or the first hook failure with {@code failure} suppressed
     */
    Throwable afterAttemptFailed(CallState<?, ?> call, AttemptState attempt, Throwable failure) {
        if (afterAttempt.length == 0) {
            return failure;
        }
        HookContext.AfterAttempt context =
            new HookContext.AfterAttempt(call.plan().operation(), call.options(), attempt.context(), call.request(),
                                         Optional.empty(), Optional.of(failure));
        return replacingFailure(failure, observeAll(call, afterAttempt, AFTER_ATTEMPT, context,
                                                    ExecutionInterceptor::afterAttempt));
    }

    // Terminal execution hooks.

    /** The output to return: {@code output} after every {@code modifyResponse} replacement. */
    @SuppressWarnings("unchecked")
    <O extends SerializableStruct> O modifyResponse(CallState<?, O> call, O output) {
        if (modifyResponse.length == 0) {
            return output;
        }
        HookContext.ModifyResponse context =
            new HookContext.ModifyResponse(call.plan().operation(), call.options(), call.executionContext(),
                                           call.request(), output);
        // Validated to be of the output's own class, so the cast is safe.
        return (O) InterceptorDispatch.<HookContext.ModifyResponse, SerializableStruct>modify(
                          call, modifyResponse, MODIFY_RESPONSE, context, output,
                          ExecutionInterceptor::modifyResponse, InterceptorDispatch::checkOutput,
                          (c, value) -> new HookContext.ModifyResponse(c.operation(), c.options(), c.execution(),
                                                                       c.request(), value));
    }

    /**
     * The failure to surface: the mapped {@code failure} after every {@code modifyException} replacement. A hook
     * failure ends the chain and becomes the failure, with the failure it replaced suppressed.
     */
    RuntimeException modifyException(CallState<?, ?> call, RuntimeException failure) {
        if (modifyException.length == 0) {
            return failure;
        }
        HookContext.ModifyException context =
            new HookContext.ModifyException(call.plan().operation(), call.options(), call.executionContext(),
                                            call.request(), failure);
        try {
            return (RuntimeException) modify(call, modifyException, MODIFY_EXCEPTION, context, (Throwable) failure,
                                             ExecutionInterceptor::modifyException,
                                             InterceptorDispatch::checkTerminalFailure,
                                             (c, value) -> new HookContext.ModifyException(c.operation(), c.options(),
                                                                                           c.execution(),
                                                                                           c.request(), value));
        } catch (InterceptorException hookFailure) {
            return (RuntimeException) replacingFailure(failure, hookFailure);
        }
    }

    /** {@code afterExecution} of a call that succeeds with {@code output}; the first hook failure is thrown. */
    void afterExecutionSucceeded(CallState<?, ?> call, SerializableStruct output) {
        if (afterExecution.length == 0) {
            return;
        }
        InterceptorException hookFailure =
            observeAll(call, afterExecution, AFTER_EXECUTION,
                       afterExecutionContext(call, Optional.of(output), Optional.empty()),
                       ExecutionInterceptor::afterExecution);
        if (hookFailure != null) {
            throw hookFailure;
        }
    }

    /** {@code afterExecution} of a call that fails with {@code failure}; hook failures are suppressed onto it. */
    void afterExecutionFailed(CallState<?, ?> call, Throwable failure) {
        if (afterExecution.length == 0) {
            return;
        }
        InterceptorException hookFailure =
            observeAll(call, afterExecution, AFTER_EXECUTION,
                       afterExecutionContext(call, Optional.empty(), Optional.of(failure)),
                       ExecutionInterceptor::afterExecution);
        if (hookFailure != null) {
            failure.addSuppressed(hookFailure);
        }
    }

    private static HookContext.AfterExecution afterExecutionContext(CallState<?, ?> call,
                                                                    Optional<SerializableStruct> output,
                                                                    Optional<Throwable> failure) {
        return new HookContext.AfterExecution(call.plan().operation(), call.options(), call.executionContext(),
                                              call.request(), output, failure);
    }

    // Dispatch mechanics.

    /** Calls {@code hook} of each interceptor in order; the first that throws fails the hook. */
    private static <C> void observe(CallState<?, ?> call, ExecutionInterceptor[] targets, String hook, C context,
                                    BiConsumer<ExecutionInterceptor, C> method) {
        for (ExecutionInterceptor interceptor : targets) {
            try {
                method.accept(interceptor, context);
            } catch (Exception thrown) {
                throw InterceptorException.thrown(call.plan().operation().id(), interceptor, hook, thrown);
            }
        }
    }

    /**
     * Calls {@code hook} of every interceptor, even after one throws.
     *
     * @return the first hook failure, with later ones suppressed onto it; {@code null} if none threw
     */
    private static <C> InterceptorException observeAll(CallState<?, ?> call, ExecutionInterceptor[] targets,
                                                       String hook, C context,
                                                       BiConsumer<ExecutionInterceptor, C> method) {
        InterceptorException first = null;
        for (ExecutionInterceptor interceptor : targets) {
            try {
                method.accept(interceptor, context);
            } catch (Exception thrown) {
                InterceptorException failure =
                    InterceptorException.thrown(call.plan().operation().id(), interceptor, hook, thrown);
                if (first == null) {
                    first = failure;
                } else {
                    first.addSuppressed(failure);
                }
            }
        }
        return first;
    }

    /**
     * Calls the modify {@code hook} of each interceptor in order, each with a context holding the previous
     * replacement, and validates each replacement against the value it replaces.
     */
    private static <C, T> T modify(CallState<?, ?> call, ExecutionInterceptor[] targets, String hook, C context,
                                   T value, Modify<C, T> method, Check<T> check, Rebuild<C, T> rebuild) {
        C currentContext = context;
        T current = value;
        for (ExecutionInterceptor interceptor : targets) {
            T replacement;
            try {
                replacement = method.apply(interceptor, currentContext);
            } catch (Exception thrown) {
                throw InterceptorException.thrown(call.plan().operation().id(), interceptor, hook, thrown);
            }
            if (replacement == current) {
                continue;
            }
            String incompatibility = replacement == null ? "null" : check.incompatibility(call, current, replacement);
            if (incompatibility != null) {
                throw InterceptorException.incompatible(call.plan().operation().id(), interceptor, hook,
                                                        incompatibility);
            }
            current = replacement;
            currentContext = rebuild.with(currentContext, replacement);
        }
        return current;
    }

    /** {@code hookFailure}, with the {@code failure} it replaces suppressed; {@code failure} if there is none. */
    private static Throwable replacingFailure(Throwable failure, InterceptorException hookFailure) {
        if (hookFailure == null) {
            return failure;
        }
        hookFailure.addSuppressed(failure);
        return hookFailure;
    }

    // Replacement validation: null if compatible, else what is incompatible.

    private static String checkInput(CallState<?, ?> call, SerializableStruct current, SerializableStruct value) {
        return checkModeled(call.plan().operation().schemas().input(), current, value);
    }

    private static String checkOutput(CallState<?, ?> call, SerializableStruct current, SerializableStruct value) {
        return checkModeled(call.plan().operation().schemas().output(), current, value);
    }

    private static String checkModeled(Schema expected, SerializableStruct current, SerializableStruct value) {
        if (value.getClass() != current.getClass()) {
            return "a " + value.getClass().getName() + ", not a " + current.getClass().getName();
        }
        Schema schema = value.schema();
        if (schema == null || !expected.id().equals(schema.id())) {
            return "a value of shape " + (schema == null ? null : schema.id()) + ", not " + expected.id();
        }
        return null;
    }

    private static String checkUnsignedRequest(CallState<?, ?> call, SdkHttpRequest current, SdkHttpRequest value) {
        return checkReplayable(current.body(), value.body());
    }

    private static String checkSignedRequest(CallState<?, ?> call, SignedRequest current, SignedRequest value) {
        SdkHttpRequest signed = current.request();
        SdkHttpRequest request = value.request();
        if (!signed.method().equals(request.method()) || !signed.scheme().equals(request.scheme())
            || !signed.host().equals(request.host()) || signed.port() != request.port()
            || !signed.encodedPath().equals(request.encodedPath())) {
            return "a request to " + request.method() + " " + target(request) + ", but " + signed.method() + " "
                   + target(signed) + " was signed";
        }
        return checkReplayable(signed.body(), request.body());
    }

    private static String target(SdkHttpRequest request) {
        return request.scheme() + "://" + request.authority() + request.encodedPath();
    }

    private static String checkResponse(CallState<?, ?> call, SdkHttpResponse current, SdkHttpResponse value) {
        return checkReplayable(current.body(), value.body());
    }

    private static String checkBody(CallState<?, ?> call, HttpBody current, HttpBody value) {
        return checkReplayable(current, value);
    }

    private static String checkAttemptFailure(CallState<?, ?> call, Throwable current, Throwable value) {
        return value instanceof Exception ? null : "a " + value.getClass().getName() + ", not an Exception";
    }

    private static String checkTerminalFailure(CallState<?, ?> call, Throwable current, Throwable value) {
        return value instanceof RuntimeException ? null
                                                 : "a " + value.getClass().getName() + ", not a RuntimeException";
    }

    /** A replayable body may only be replaced by a replayable body, so a retry can send it again. */
    private static String checkReplayable(HttpBody current, HttpBody value) {
        if (replayable(current) && !replayable(value)) {
            return "a one-shot streaming body in place of a replayable body";
        }
        return null;
    }

    private static boolean replayable(HttpBody body) {
        return body == null || body.kind() != HttpBody.Kind.STREAMING || body.streamingBody().isReplayable();
    }

    private ExecutionInterceptor[] select(String hook, Class<?> contextType) {
        List<ExecutionInterceptor> selected = new ArrayList<>();
        for (ExecutionInterceptor interceptor : interceptors) {
            if (overrides(interceptor, hook, contextType)) {
                selected.add(interceptor);
            }
        }
        return selected.isEmpty() ? NO_INTERCEPTORS : selected.toArray(NO_INTERCEPTORS);
    }

    /** Whether {@code interceptor}'s class implements {@code hook} rather than inheriting the no-op default. */
    private static boolean overrides(ExecutionInterceptor interceptor, String hook, Class<?> contextType) {
        try {
            Method method = interceptor.getClass().getMethod(hook, contextType);
            return method.getDeclaringClass() != ExecutionInterceptor.class;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("ExecutionInterceptor has no hook " + hook, e);
        }
    }

    private static void addIf(Set<PipelineStage> active, ExecutionInterceptor[] targets, PipelineStage stage) {
        if (targets.length != 0) {
            active.add(stage);
        }
    }

    @Override
    public String toString() {
        return "InterceptorDispatch" + activeStages();
    }

    /** A modify hook method, as an unbound method reference. */
    @FunctionalInterface
    private interface Modify<C, T> {
        T apply(ExecutionInterceptor interceptor, C context);
    }

    /** The context of a modify hook holding a replacement value. */
    @FunctionalInterface
    private interface Rebuild<C, T> {
        C with(C context, T value);
    }

    /** Validates a replacement: {@code null} if compatible, else a description of the incompatibility. */
    @FunctionalInterface
    private interface Check<T> {
        String incompatibility(CallState<?, ?> call, T current, T value);
    }
}
