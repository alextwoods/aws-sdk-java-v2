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

package software.amazon.awssdk.test.protocoltests.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;

/**
 * A synchronous transport that records every transmitted request, with its body bytes read completely, and either
 * replies with a scripted response or ends the attempt with {@link RequestCaptured}.
 *
 * <p>The recorded request is the request as it goes on the wire: like every V3 transport (see {@code ApacheExchange}),
 * this transport adds the message framing the pipeline deliberately leaves to the transport ({@code OperationProtocol}
 * sets no {@code Content-Length}) when the request has no framing header: {@code Content-Length} for a body of known
 * length, {@code Transfer-Encoding: chunked} for one of unknown length, and {@code Content-Length: 0} for an empty
 * POST, PUT or PATCH.
 */
public final class CapturingTransport implements SdkSyncHttpClient {
    private final SdkHttpResponse reply;
    private final List<Captured> requests = new ArrayList<>();

    private CapturingTransport(SdkHttpResponse reply) {
        this.reply = reply;
    }

    /** A transport that captures the request and fails the attempt; request cases compare only the request. */
    public static CapturingTransport capturing() {
        return new CapturingTransport(null);
    }

    /** A transport that captures the request and replies with {@code response}. */
    public static CapturingTransport replying(SdkHttpResponse response) {
        return new CapturingTransport(response);
    }

    /** A transmitted request and the bytes of its body. */
    public record Captured(SdkHttpRequest request, byte[] body) {
    }

    /** Every transmitted request, in order. */
    public synchronized List<Captured> requests() {
        return List.copyOf(requests);
    }

    /** Thrown by a capturing transport once the request is recorded. */
    public static final class RequestCaptured extends RuntimeException {
        private static final long serialVersionUID = 1L;

        RequestCaptured() {
            super("The protocol conformance transport captured the request and ended the attempt.", null, false,
                  false);
        }
    }

    @Override
    public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
        return new SyncHttpExchange() {
            @Override
            public SdkHttpResponse execute() {
                synchronized (CapturingTransport.this) {
                    requests.add(new Captured(framed(request), read(request.body())));
                }
                if (reply == null) {
                    throw new RequestCaptured();
                }
                return reply;
            }

            @Override
            public void abort() {
            }

            @Override
            public void close() {
            }
        };
    }

    /** The request with the framing header a transport adds when the request has none. */
    private static SdkHttpRequest framed(SdkHttpRequest request) {
        if (request.headers().contains("Content-Length") || request.headers().contains("Transfer-Encoding")) {
            return request;
        }
        HttpBody body = request.body();
        if (body.kind() != HttpBody.Kind.EMPTY) {
            return body.contentLength().isPresent()
                   ? request.toBuilder().addHeader("Content-Length", Long.toString(body.contentLength().getAsLong()))
                            .build()
                   : request.toBuilder().addHeader("Transfer-Encoding", "chunked").build();
        }
        String method = request.method();
        if (method.equals("POST") || method.equals("PUT") || method.equals("PATCH")) {
            return request.toBuilder().addHeader("Content-Length", "0").build();
        }
        return request;
    }

    private static byte[] read(HttpBody body) {
        return switch (body.kind()) {
            case EMPTY -> new byte[0];
            case BYTES -> body.asByteArray();
            case STREAMING -> {
                try (InputStream in = body.streamingBody().openStream()) {
                    yield in.readAllBytes();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        };
    }

    @Override
    public HttpClientCapabilities capabilities() {
        return HttpClientCapabilities.builder().mode(HttpTransportMode.SYNC).addProtocol(Protocol.HTTP1_1)
                                     .maxBufferedBytesPerExchange(1 << 20)
                                     .cancellationTimeout(Duration.ofSeconds(1)).build();
    }

    @Override
    public String clientName() {
        return "ProtocolConformanceTransport";
    }

    @Override
    public void close() {
    }
}
