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

package software.amazon.awssdk.http.crt.internal;

import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpHeaderBlock;
import software.amazon.awssdk.crt.http.HttpRequestBodyStream;
import software.amazon.awssdk.crt.http.HttpStream;
import software.amazon.awssdk.crt.http.HttpStreamResponseHandler;
import software.amazon.awssdk.http.AsyncExchangeCompletion;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.CancellationReason;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpHeaders;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * One non-blocking CRT exchange. Terminal uniqueness comes from {@link AsyncExchangeCompletion}: the handler or the
 * body subscriber is signalled only by the call that won. Handler signals ({@code onHeaders}, {@code onStream},
 * {@code onError}) are serialized by one lock, so a cancellation racing the event loop never signals concurrently or
 * after {@code onStream}; after {@code onStream} failures go to the body subscriber. Nothing here blocks.
 */
public final class CrtAsyncExchange implements HttpStreamResponseHandler, CrtResponsePublisher.Source {
    private final CrtClientCore core;
    private final SdkHttpRequest request;
    private final AsyncHttpResponseHandler handler;
    private final CrtStreamState nativeStream = new CrtStreamState();
    private final AsyncExchangeCompletion completion;
    private final CrtResponsePublisher body = new CrtResponsePublisher(this);
    private final HttpHeaders.Builder responseHeaders = HttpHeaders.builder();
    private final Object signalLock = new Object();
    private final Runnable cancelOnClientClose;
    private volatile CrtRequestBodies.FromPublisher requestBody;
    private boolean streamDelivered;
    private int status;

    public CrtAsyncExchange(CrtClientCore core, SdkHttpRequest request, AsyncHttpResponseHandler handler) {
        this.core = core;
        this.request = request;
        this.handler = handler;
        this.completion = new AsyncExchangeCompletion(core.clientName(), this::onCancelled);
        this.cancelOnClientClose = () -> completion.cancel(CancellationReason.CLIENT_CLOSED);
    }

    /** Starts the exchange; returns at once. */
    public AsyncHttpExchange start(HttpExecutionOptions options) {
        core.track(cancelOnClientClose);
        completion.completion().whenComplete((r, t) -> core.untrack(cancelOnClientClose));
        core.acquire(request, options.connectionAcquisitionTimeout()).whenComplete((lease, failure) -> {
            if (failure != null) {
                deliverFailure(failure);
                return;
            }
            if (!nativeStream.leased(lease)) {
                return;
            }
            send(lease);
        });
        return completion;
    }

    private void send(CrtClientCore.Lease lease) {
        completion.enter(HttpPhase.REQUEST_TRANSMISSION);
        try {
            // Makes and activates the stream under the stream state's lock; discards the connection if this throws.
            nativeStream.start(() -> {
                HttpRequestBodyStream bodyStream = null;
                HttpBody content = request.body();
                if (content.kind() == HttpBody.Kind.BYTES) {
                    bodyStream = CrtRequestBodies.bytes(content);
                } else if (content.kind() == HttpBody.Kind.STREAMING) {
                    requestBody = new CrtRequestBodies.FromPublisher(content.streamingBody(),
                                                                     content.contentLength().orElse(-1),
                                                                     this::requestBodyFailed);
                    bodyStream = requestBody;
                }
                return lease.connection().makeRequest(CrtClientCore.toCrtRequest(request, bodyStream), this);
            });
        } catch (RuntimeException e) {
            cancelRequestBody();
            deliverFailure(e);
        }
    }

    private void requestBodyFailed(Throwable t) {
        deliverFailure(CrtFailureMapping.toTransportException(t, HttpPhase.REQUEST_TRANSMISSION, core.clientName()));
        nativeStream.abandon();
    }

    @Override
    public void onResponseHeaders(HttpStream stream, int statusCode, int blockType, HttpHeader[] nextHeaders) {
        if (blockType == HttpHeaderBlock.MAIN.getValue()) {
            completion.enter(HttpPhase.RESPONSE_CONSUMPTION);
            status = statusCode;
            for (HttpHeader h : nextHeaders) {
                responseHeaders.add(h.getName(), h.getValue());
            }
        }
    }

    @Override
    public void onResponseHeadersDone(HttpStream stream, int blockType) {
        if (blockType != HttpHeaderBlock.MAIN.getValue()) {
            return;
        }
        SdkHttpResponse response = SdkHttpResponse.builder().statusCode(status).headers(responseHeaders.build())
                                                  .body(HttpBody.empty()).build();
        synchronized (signalLock) {
            if (completion.isDone()) {
                return;
            }
            handler.onHeaders(response);
            streamDelivered = true;
            handler.onStream(body);
        }
    }

    @Override
    public int onResponseBody(HttpStream stream, byte[] bodyBytesIn) {
        body.onData(bodyBytesIn);
        return 0;
    }

    @Override
    public void onResponseComplete(HttpStream stream, int errorCode) {
        cancelRequestBody();
        if (errorCode == 0) {
            boolean delivered;
            synchronized (signalLock) {
                delivered = streamDelivered;
            }
            // The connection is released for reuse only once the subscriber received onComplete (terminate).
            nativeStream.completed(delivered);
            if (delivered) {
                body.onComplete();
            } else {
                deliverFailure(HttpTransportException.builder().phase(HttpPhase.RESPONSE_CONSUMPTION)
                                                     .category(software.amazon.awssdk.http.HttpFailureCategory
                                                                   .PROTOCOL)
                                                     .clientName(core.clientName())
                                                     .message("The response ended without headers").build());
            }
        } else {
            nativeStream.completed(false);
            deliverFailure(CrtFailureMapping.fromErrorCode(errorCode, completion.phase(), core.clientName()));
        }
    }

    private void deliverFailure(Throwable failure) {
        HttpTransportException f =
            CrtFailureMapping.toTransportException(failure, completion.phase(), core.clientName());
        synchronized (signalLock) {
            if (streamDelivered) {
                body.onError(f);
            } else if (completion.fail(f)) {
                handler.onError(f);
            }
        }
    }

    private void onCancelled(CancellationReason reason, HttpTransportException failure) {
        nativeStream.abandon();
        cancelRequestBody();
        synchronized (signalLock) {
            if (streamDelivered) {
                body.onCancelled(failure);
            } else {
                handler.onError(failure);
            }
        }
    }

    private void cancelRequestBody() {
        CrtRequestBodies.FromPublisher b = requestBody;
        if (b != null) {
            b.cancel();
        }
    }

    // CrtResponsePublisher.Source

    @Override
    public void incrementWindow(int bytes) {
        nativeStream.incrementWindow(bytes);
    }

    @Override
    public Throwable terminate(Throwable failure) {
        if (failure == null) {
            // The whole body reached the subscriber: the connection may be reused. Released before the completion
            // is signalled so that a request issued on completion finds it idle; a no-op if the exchange was
            // already abandoned (a cancellation that won discarded it).
            nativeStream.consumed();
            return completion.complete() ? null : CrtResponsePublisher.SUPPRESS;
        }
        HttpTransportException f = completion.asTransportException(failure);
        return completion.fail(f) ? f : CrtResponsePublisher.SUPPRESS;
    }

    @Override
    public void cancelled(Throwable ruleViolation) {
        if (ruleViolation != null) {
            completion.fail(ruleViolation);
            nativeStream.abandon();
            cancelRequestBody();
        } else {
            completion.cancel(CancellationReason.RESPONSE_ABANDONED);
        }
    }

    /** Test view of the response publisher. */
    public CrtResponsePublisher body() {
        return body;
    }
}
