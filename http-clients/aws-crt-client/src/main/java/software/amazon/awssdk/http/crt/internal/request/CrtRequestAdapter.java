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

package software.amazon.awssdk.http.crt.internal.request;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.crt.http.HttpRequestBase;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.crt.internal.CrtAsyncRequestContext;
import software.amazon.awssdk.http.crt.internal.CrtRequestContext;

@SdkInternalApi
public final class CrtRequestAdapter {
    private CrtRequestAdapter() {
    }

    public static HttpRequestBase toAsyncCrtRequest(CrtAsyncRequestContext request) {
        AsyncExecuteRequest executeRequest = request.sdkRequest();
        SdkHttpRequest sdkRequest = executeRequest.request();
        String encodedPath = normalizedPath(sdkRequest);
        String query = sdkRequest.encodedQueryParameters().map(value -> "?" + value).orElse("");
        CrtRequestBodyAdapter body = new CrtRequestBodyAdapter(executeRequest.requestContentPublisher(),
                                                               request.readBufferSize());
        return new HttpRequest(sdkRequest.method().name(), encodedPath + query,
                               createAsyncHttpHeaders(executeRequest, request.protocol()), body);
    }

    public static HttpRequest toCrtRequest(CrtRequestContext request) {
        HttpExecuteRequest executeRequest = request.sdkRequest();
        SdkHttpRequest sdkRequest = executeRequest.httpRequest();
        String path = normalizedPath(sdkRequest)
                      + sdkRequest.encodedQueryParameters().map(value -> "?" + value).orElse("");
        HttpHeader[] headers = createHttpHeaders(executeRequest);
        return executeRequest.contentStreamProvider()
                             .map(provider -> new HttpRequest(sdkRequest.method().name(), path, headers,
                                                              new CrtRequestInputStreamAdapter(provider)))
                             .orElse(new HttpRequest(sdkRequest.method().name(), path, headers, null));
    }

    private static String normalizedPath(SdkHttpRequest request) {
        String path = request.encodedPath();
        return path == null || path.isEmpty() ? "/" : path;
    }

    private static HttpHeader[] createAsyncHttpHeaders(AsyncExecuteRequest executeRequest, Protocol protocol) {
        SdkHttpRequest request = executeRequest.request();
        CrtHeaderArrayBuilder headers = new CrtHeaderArrayBuilder(request.numHeaders() + 3);
        if (!request.firstMatchingHeader(Header.HOST).isPresent()) {
            headers.add(Header.HOST, request.host());
        }
        if (protocol != Protocol.HTTP2 && !request.firstMatchingHeader(Header.CONNECTION).isPresent()) {
            headers.add(Header.CONNECTION, Header.KEEP_ALIVE_VALUE);
        }
        Optional<Long> contentLength = executeRequest.requestContentPublisher().contentLength();
        if (!request.firstMatchingHeader(Header.CONTENT_LENGTH).isPresent()
            && !request.firstMatchingHeader(Header.TRANSFER_ENCODING).isPresent()
            && contentLength.isPresent()) {
            headers.add(Header.CONTENT_LENGTH, Long.toString(contentLength.get()));
        }
        request.forEachHeader(headers);
        return headers.build();
    }

    private static HttpHeader[] createHttpHeaders(HttpExecuteRequest executeRequest) {
        SdkHttpRequest request = executeRequest.httpRequest();
        CrtHeaderArrayBuilder headers = new CrtHeaderArrayBuilder(request.numHeaders() + 2);
        if (!request.firstMatchingHeader(Header.HOST).isPresent()) {
            headers.add(Header.HOST, request.host());
        }
        if (!request.firstMatchingHeader(Header.CONNECTION).isPresent()) {
            headers.add(Header.CONNECTION, Header.KEEP_ALIVE_VALUE);
        }
        request.forEachHeader(headers);
        return headers.build();
    }

    private static final class CrtHeaderArrayBuilder implements BiConsumer<String, List<String>> {
        private HttpHeader[] headers;
        private int size;

        private CrtHeaderArrayBuilder(int initialCapacity) {
            this.headers = new HttpHeader[Math.max(0, initialCapacity)];
        }

        @Override
        public void accept(String name, List<String> values) {
            for (int i = 0; i < values.size(); i++) {
                add(name, values.get(i));
            }
        }

        private void add(String name, String value) {
            if (size == headers.length) {
                grow();
            }
            headers[size++] = new HttpHeader(name, value);
        }

        private void grow() {
            int newLength = headers.length == 0 ? 4 : headers.length + (headers.length >> 1) + 1;
            if (newLength < 0) {
                newLength = Integer.MAX_VALUE;
            }
            headers = Arrays.copyOf(headers, newLength);
        }

        private HttpHeader[] build() {
            return size == headers.length ? headers : Arrays.copyOf(headers, size);
        }
    }
}
