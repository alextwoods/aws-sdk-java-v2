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

import java.io.IOException;
import java.time.Duration;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.SdkStandardLogger;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.core.exception.NonRetryableException;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;

/**
 * The deserialization step of the schema-based request pipeline (design D.6, seam 3). The sync response stage and the
 * async response handler of the new pipeline path call it for each attempt's response; the legacy path keeps using
 * generated response handlers through {@code CombinedResponseHandler}.
 *
 * <p>It replaces the legacy {@code CombinedResponseHandler} wrapped around the handler that
 * {@code BaseClientHandler.decorateResponseHandlers} decorates, with the same observable behavior:
 * <ul>
 *     <li>The request ID is logged as the legacy handler logs it.</li>
 *     <li>{@link ClientProtocol#deserializeResponse} builds the output, or throws the error the response describes. A
 *     thrown {@link SdkServiceException} becomes a failed {@link Response}, so the retry decision sees a service error
 *     exactly as it sees an error unmarshalled by the legacy error handler. Other {@link SdkException}s (the
 *     protocol's {@code "Unable to unmarshall <op>: <cause>"} client failure) are thrown unchanged; any other runtime
 *     exception from a protocol becomes an {@link SdkClientException} with the legacy
 *     {@code "Unable to unmarshall response (...)"} message.</li>
 *     <li>{@code afterUnmarshalling} and {@code modifyResponse} run with the output, and the interceptor context is
 *     updated, as {@code BaseClientHandler.runAfterUnmarshallingInterceptors} does. A failure there is reported
 *     exactly as the legacy {@code CombinedResponseHandler} reports it.</li>
 *     <li>The final output carries the HTTP response as {@code sdkHttpResponse}. The SDK's protocols set it on the
 *     output builder before {@code build()} (task 3.21), so normally nothing is copied; the output is copied through
 *     {@code toBuilder()} only if it carries a different HTTP response or none, which happens for a protocol that does
 *     not set it or an interceptor that replaces the output in {@code modifyResponse}, as the legacy
 *     {@code attachHttpResponseToResult} step does for every response.</li>
 *     <li>For a streaming operation, the output and the unread response body go to the {@link ResponseTransformer}
 *     ({@link #handleStreaming}), as the legacy {@code HttpResponseHandlerAdapter} does.</li>
 *     <li>The response body is closed afterwards, except when a streaming transformer needs the connection left
 *     open.</li>
 * </ul>
 *
 * <p>The class references no generated unmarshaller or response handler type (Requirement 37.2).
 */
@SdkInternalApi
public final class ProtocolResponseHandling {
    private static final Logger log = Logger.loggerFor(ProtocolResponseHandling.class);

    private ProtocolResponseHandling() {
    }

    /**
     * Deserializes the response of a non-streaming operation.
     *
     * @param executionContext the call's execution context: its attributes are passed to the protocol, and its
     *     interceptor chain and context are used for {@code afterUnmarshalling} and {@code modifyResponse}
     * @param operation the operation being called, the {@link SdkExecutionAttribute#API_OPERATION} of the call
     * @param errorRegistry the registry the protocol creates modeled errors with
     * @param request the request that was sent
     * @param response the response to deserialize; its body is closed before this method returns
     * @param <I> the operation input type
     * @param <O> the operation output type
     * @return a successful response with the output returned by the {@code modifyResponse} interceptors, or a failed
     *     response with the service error the protocol threw
     * @throws SdkClientException if the response cannot be deserialized or an interceptor fails
     * @throws IllegalStateException if the call's protocol is not set or does not exchange HTTP messages
     */
    public static <I extends SerializableStruct, O extends SerializableStruct> Response<O> handle(
        ExecutionContext executionContext,
        ApiOperation<I, O> operation,
        TypeRegistry errorRegistry,
        SdkHttpFullRequest request,
        SdkHttpFullResponse response) {

        try {
            Response<O> result = deserialize(executionContext, operation, errorRegistry, request, response);
            if (!result.isSuccess()) {
                return result;
            }
            O output = runAfterUnmarshalling(executionContext, result.response(), response);
            return success(output, response);
        } finally {
            closeBody(response);
        }
    }

    /**
     * Deserializes the response of an operation with a streaming output and hands the output and the unread response
     * body to the transformer.
     *
     * <p>For a success response the protocol reads only the status, headers and other non-body members into the output
     * (design A.13); for an error response it throws the modeled error, so the transformer never sees an error body.
     * Exceptions from the transformer are handled as the legacy {@code HttpResponseHandlerAdapter} handles them:
     * {@link RetryableException}, {@link InterruptedException} and {@link AbortedException} are rethrown and any other
     * exception is wrapped in a {@link NonRetryableException}, both then reported as {@code CombinedResponseHandler}
     * reports them.
     *
     * <p>The asynchronous counterpart, which hands the body publisher to an {@code AsyncResponseTransformer} through
     * the transport's {@code AsyncResponseHandoff}, is part of task 7.19.
     *
     * @param executionContext the call's execution context
     * @param operation the operation being called
     * @param errorRegistry the registry the protocol creates modeled errors with
     * @param request the request that was sent
     * @param response the response; its body is passed to the transformer
     * @param transformer the transformer of the output and the body
     * @param <I> the operation input type
     * @param <O> the operation output type
     * @param <R> the type the transformer returns
     * @return a successful response with the transformer's result, or a failed response with the service error the
     *     protocol threw
     * @throws Exception as described above
     */
    public static <I extends SerializableStruct, O extends SerializableStruct, R> Response<R> handleStreaming(
        ExecutionContext executionContext,
        ApiOperation<I, O> operation,
        TypeRegistry errorRegistry,
        SdkHttpFullRequest request,
        SdkHttpFullResponse response,
        ResponseTransformer<? super O, R> transformer) throws Exception {

        boolean leaveOpen = false;
        try {
            Response<O> result = deserialize(executionContext, operation, errorRegistry, request, response);
            if (!result.isSuccess()) {
                return Response.<R>builder()
                               .httpResponse(response)
                               .exception(result.exception())
                               .isSuccess(false)
                               .build();
            }
            O output = runAfterUnmarshalling(executionContext, result.response(), response);
            R transformed;
            try {
                transformed = transform(transformer, output, response);
            } catch (IOException | InterruptedException e) {
                throw e;
            } catch (RuntimeException e) {
                throw legacyHandlerFailure(e, response);
            } catch (Exception e) {
                throw unmarshallFailure("Unable to unmarshall response", e, response);
            }
            leaveOpen = transformer.needsConnectionLeftOpen();
            return Response.<R>builder()
                           .httpResponse(response)
                           .response(transformed)
                           .isSuccess(true)
                           .build();
        } finally {
            if (!leaveOpen) {
                closeBody(response);
            }
        }
    }

    private static <I extends SerializableStruct, O extends SerializableStruct> Response<O> deserialize(
        ExecutionContext executionContext,
        ApiOperation<I, O> operation,
        TypeRegistry errorRegistry,
        SdkHttpFullRequest request,
        SdkHttpFullResponse response) {

        SdkStandardLogger.logRequestId(response);
        ExecutionAttributes attributes = executionContext.executionAttributes();
        ClientProtocol<SdkHttpFullRequest, SdkHttpFullResponse> protocol = ProtocolMarshalling.httpProtocol(attributes);

        O output;
        long start = System.nanoTime();
        try {
            output = protocol.deserializeResponse(operation, attributes, errorRegistry, request, response);
        } catch (SdkServiceException e) {
            return Response.<O>builder()
                           .httpResponse(response)
                           .exception(e)
                           .isSuccess(false)
                           .build();
        } catch (SdkException e) {
            throw e;
        } catch (RuntimeException e) {
            throw unmarshallFailure(response.isSuccessful() ? "Unable to unmarshall response"
                                                            : "Unable to unmarshall error response", e, response);
        }
        reportUnmarshallingDuration(attributes, start);

        if (!(output instanceof SdkResponse)) {
            throw new IllegalStateException("Protocol " + protocol.id() + " returned an output for "
                                            + operation.schema().id() + " that is not an SdkResponse: "
                                            + (output == null ? null : output.getClass().getName()));
        }
        return Response.<O>builder()
                       .httpResponse(response)
                       .response(output)
                       .isSuccess(true)
                       .build();
    }

    /**
     * Runs the interceptors and attaches the HTTP response, reporting failures as the legacy handler does.
     */
    private static <O> O runAfterUnmarshalling(ExecutionContext executionContext, O output, SdkHttpFullResponse response) {
        try {
            return withHttpResponse(afterUnmarshalling(executionContext, output), response);
        } catch (RuntimeException e) {
            throw legacyHandlerFailure(e, response);
        }
    }

    /**
     * Same as {@code BaseClientHandler.runAfterUnmarshallingInterceptors}.
     */
    @SuppressWarnings("unchecked")
    private static <O> O afterUnmarshalling(ExecutionContext executionContext, O output) {
        ExecutionAttributes attributes = executionContext.executionAttributes();
        InterceptorContext interceptorContext =
            executionContext.interceptorContext().copy(b -> b.response((SdkResponse) output));
        executionContext.interceptorChain().afterUnmarshalling(interceptorContext, attributes);
        interceptorContext = executionContext.interceptorChain().modifyResponse(interceptorContext, attributes);
        executionContext.interceptorContext(interceptorContext);
        return (O) interceptorContext.response();
    }

    /**
     * Same result as {@code BaseClientHandler.attachHttpResponseToResult}, without the copy when the output already
     * carries the response.
     */
    @SuppressWarnings("unchecked")
    private static <O> O withHttpResponse(O output, SdkHttpFullResponse response) {
        SdkResponse sdkResponse = (SdkResponse) output;
        if (sdkResponse.sdkHttpResponse() == response) {
            return output;
        }
        return (O) sdkResponse.toBuilder().sdkHttpResponse(response).build();
    }

    /**
     * Same as the legacy {@code HttpResponseHandlerAdapter.transformResponse}.
     */
    private static <O, R> R transform(ResponseTransformer<? super O, R> transformer,
                                      O output,
                                      SdkHttpFullResponse response) throws Exception {
        AbortableInputStream body = response.content().orElseGet(AbortableInputStream::createEmpty);
        try {
            InterruptMonitor.checkInterrupted();
            R result = transformer.transform(output, body);
            InterruptMonitor.checkInterrupted();
            return result;
        } catch (RetryableException | InterruptedException | AbortedException e) {
            throw e;
        } catch (Exception e) {
            InterruptMonitor.checkInterrupted();
            throw NonRetryableException.builder().cause(e).build();
        }
    }

    /**
     * Same classification as the legacy {@code CombinedResponseHandler.handleSuccessResponse} for a runtime failure: a
     * retryable SDK exception (including {@link RetryableException}) is returned unchanged so the retry decision sees
     * it, and anything else is wrapped. ({@code IOException} and {@code InterruptedException}, which the legacy handler
     * also rethrows, are checked and handled by the caller.)
     */
    private static RuntimeException legacyHandlerFailure(RuntimeException e, SdkHttpFullResponse response) {
        if (e instanceof SdkException && ((SdkException) e).retryable()) {
            return e;
        }
        return unmarshallFailure("Unable to unmarshall response", e, response);
    }

    private static SdkClientException unmarshallFailure(String prefix, Exception e, SdkHttpFullResponse response) {
        String message = prefix + " (" + e.getMessage() + "). Response Code: " + response.statusCode()
                         + ", Response Text: " + response.statusText().orElse(null);
        return SdkClientException.builder().message(message).cause(e).build();
    }

    private static <O> Response<O> success(O output, SdkHttpFullResponse response) {
        return Response.<O>builder()
                       .httpResponse(response)
                       .response(output)
                       .isSuccess(true)
                       .build();
    }

    private static void reportUnmarshallingDuration(ExecutionAttributes attributes, long startNanos) {
        MetricCollector collector = attributes.getAttribute(SdkExecutionAttribute.API_CALL_ATTEMPT_METRIC_COLLECTOR);
        if (collector != null) {
            collector.reportMetric(CoreMetric.UNMARSHALLING_DURATION, Duration.ofNanos(System.nanoTime() - startNanos));
        }
    }

    private static void closeBody(SdkHttpFullResponse response) {
        if (response != null) {
            response.content().ifPresent(content -> IoUtils.closeQuietlyV2(content, log));
        }
    }
}
