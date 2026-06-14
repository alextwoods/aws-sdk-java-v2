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

package software.amazon.awssdk.bridge.smithyjava.transport;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.smithy.java.client.core.ClientTransport;
import software.amazon.smithy.java.context.Context;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.java.http.api.HttpResponse;
import software.amazon.smithy.java.io.datastream.DataStream;

/**
 * The reverse of {@link V2TransportBridge}: adapts a smithy-java
 * {@link ClientTransport ClientTransport&lt;HttpRequest, HttpResponse&gt;} (e.g.
 * {@code SmithyHttpClientTransport}) as an AWS SDK for Java <b>v2</b> {@link SdkHttpClient}.
 *
 * <p>This lets a real v2 service client (e.g. {@code DynamoDbClient}) send its requests over
 * the smithy-java HTTP client — the transport half of running the v2 public API on the
 * smithy-java stack. A v2 {@code DynamoDbClient.builder().httpClient(...)} configured with
 * this adapter puts every request on the wire via the supplied smithy transport.
 *
 * <p>Synchronous only. Bodies are buffered into the smithy {@link DataStream} (v2 hands the
 * request body as a {@code ContentStreamProvider} input stream).
 */
@SdkPublicApi
public final class SmithyTransportSdkHttpClient implements SdkHttpClient {

    private final ClientTransport<HttpRequest, HttpResponse> transport;

    public SmithyTransportSdkHttpClient(ClientTransport<HttpRequest, HttpResponse> transport) {
        this.transport = transport;
    }

    @Override
    public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
        HttpRequest smithyRequest = toSmithyRequest(request);
        return new ExecutableHttpRequest() {
            @Override
            public HttpExecuteResponse call() {
                HttpResponse smithyResponse = transport.send(Context.create(), smithyRequest);
                return toV2Response(smithyResponse);
            }

            @Override
            public void abort() {
                // No-op: the synchronous smithy transport call isn't externally abortable here.
            }
        };
    }

    @Override
    public String clientName() {
        return "SmithyTransportBridge";
    }

    @Override
    public void close() {
        try {
            transport.close();
        } catch (Exception e) {
            // best-effort
        }
    }

    // ---- v2 request -> smithy HttpRequest ------------------------------------

    private static HttpRequest toSmithyRequest(HttpExecuteRequest request) {
        SdkHttpRequest v2 = request.httpRequest();
        var modifiable = HttpRequest.create()
                .setMethod(v2.method().name())
                .setUri(v2.getUri());

        for (Map.Entry<String, List<String>> e : v2.headers().entrySet()) {
            for (String value : e.getValue()) {
                modifiable.headers().addHeader(e.getKey(), value);
            }
        }

        // Stream the v2 request body into the smithy DataStream, if any.
        var bodyProvider = request.contentStreamProvider().orElse(null);
        if (bodyProvider != null) {
            modifiable.setBody(DataStream.ofInputStream(bodyProvider.newStream()));
        }
        return modifiable;
    }

    // ---- smithy HttpResponse -> v2 HttpExecuteResponse -----------------------

    private static HttpExecuteResponse toV2Response(HttpResponse smithy) {
        SdkHttpFullResponse.Builder responseBuilder = SdkHttpFullResponse.builder()
                .statusCode(smithy.statusCode())
                .headers(smithy.headers().map());

        HttpExecuteResponse.Builder builder = HttpExecuteResponse.builder()
                .response(responseBuilder.build());

        DataStream body = smithy.body();
        if (body != null) {
            InputStream in = body.asInputStream();
            if (in != null) {
                builder.responseBody(AbortableInputStream.create(in));
            }
        }
        return builder.build();
    }
}
