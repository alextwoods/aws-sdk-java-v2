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

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.RandomAccess;
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
        AsyncExecuteRequest sdkExecuteRequest = request.sdkRequest();
        SdkHttpRequest sdkRequest = sdkExecuteRequest.request();

        String method = sdkRequest.method().name();
        String encodedPath = sdkRequest.encodedPath();
        if (encodedPath == null || encodedPath.isEmpty()) {
            encodedPath = "/";
        }

        String encodedQueryString = sdkRequest.encodedQueryParameters()
                                              .map(value -> "?" + value)
                                              .orElse("");
        String path = encodedPath + encodedQueryString;
        CrtRequestBodyAdapter crtRequestBodyAdapter = new CrtRequestBodyAdapter(sdkExecuteRequest.requestContentPublisher(),
                                                                                request.readBufferSize());
        HttpHeader[] crtHeaderArray = createAsyncHttpHeaders(sdkRequest.getUri(), sdkExecuteRequest, request.protocol());
        return new HttpRequest(method,
                               path,
                               crtHeaderArray,
                               crtRequestBodyAdapter);
    }

    public static HttpRequest toCrtRequest(CrtRequestContext request) {

        HttpExecuteRequest sdkExecuteRequest = request.sdkRequest();
        SdkHttpRequest sdkRequest = sdkExecuteRequest.httpRequest();

        String method = sdkRequest.method().name();
        String encodedPath = sdkRequest.encodedPath();
        if (encodedPath == null || encodedPath.isEmpty()) {
            encodedPath = "/";
        }

        String encodedQueryString = sdkRequest.encodedQueryParameters()
                                              .map(value -> "?" + value)
                                              .orElse("");

        HttpHeader[] crtHeaderArray = createHttpHeaders(sdkRequest.getUri(), sdkExecuteRequest);

        String finalEncodedPath = encodedPath + encodedQueryString;
        return sdkExecuteRequest.contentStreamProvider()
                                .map(provider -> new HttpRequest(method,
                                                                 finalEncodedPath,
                                                                 crtHeaderArray,
                                                                 new CrtRequestInputStreamAdapter(provider)))
                                .orElse(new HttpRequest(method,
                                                        finalEncodedPath,
                                                        crtHeaderArray, null));
    }

    private static HttpHeader[] createAsyncHttpHeaders(URI uri, AsyncExecuteRequest sdkExecuteRequest, Protocol protocol) {
        SdkHttpRequest sdkRequest = sdkExecuteRequest.request();

        // Set Host Header if needed
        HttpHeader host = null;
        if (!sdkRequest.firstMatchingHeader(Header.HOST).isPresent()) {
            host = new HttpHeader(Header.HOST, uri.getHost());
        }

        // Add Connection Keep Alive Header for HTTP/1.1 only (forbidden in HTTP/2 per RFC 7540)
        HttpHeader connection = null;
        if (protocol != Protocol.HTTP2 && !sdkRequest.firstMatchingHeader(Header.CONNECTION).isPresent()) {
            connection = new HttpHeader(Header.CONNECTION, Header.KEEP_ALIVE_VALUE);
        }

        // Set Content-Length if needed, but never alongside Transfer-Encoding. RFC 7230 forbids sending both
        HttpHeader contentLengthHeader = null;
        Optional<Long> contentLength = sdkExecuteRequest.requestContentPublisher().contentLength();
        if (!sdkRequest.firstMatchingHeader(Header.CONTENT_LENGTH).isPresent()
            && !sdkRequest.firstMatchingHeader(Header.TRANSFER_ENCODING).isPresent()
            && contentLength.isPresent()) {
            contentLengthHeader = new HttpHeader(Header.CONTENT_LENGTH, Long.toString(contentLength.get()));
        }

        CrtHeaderArrayBuilder headers = new CrtHeaderArrayBuilder(sdkRequest.numHeaders()
                                                                  + countPresent(host, connection, contentLengthHeader));
        headers.addIfPresent(host);
        headers.addIfPresent(connection);
        headers.addIfPresent(contentLengthHeader);

        // Add the rest of the Headers
        sdkRequest.forEachHeader(headers);

        return headers.build();
    }

    private static HttpHeader[] createHttpHeaders(URI uri, HttpExecuteRequest sdkExecuteRequest) {
        SdkHttpRequest sdkRequest = sdkExecuteRequest.httpRequest();

        // Set Host Header if needed
        HttpHeader host = null;
        if (!sdkRequest.firstMatchingHeader(Header.HOST).isPresent()) {
            host = new HttpHeader(Header.HOST, uri.getHost());
        }

        // Add Connection Keep Alive Header to reuse this Http Connection as long as possible
        HttpHeader connection = null;
        if (!sdkRequest.firstMatchingHeader(Header.CONNECTION).isPresent()) {
            connection = new HttpHeader(Header.CONNECTION, Header.KEEP_ALIVE_VALUE);
        }

        CrtHeaderArrayBuilder headers = new CrtHeaderArrayBuilder(sdkRequest.numHeaders() + countPresent(host, connection, null));
        headers.addIfPresent(host);
        headers.addIfPresent(connection);

        // Add the rest of the Headers
        sdkRequest.forEachHeader(headers);

        return headers.build();
    }

    private static int countPresent(HttpHeader first, HttpHeader second, HttpHeader third) {
        return (first == null ? 0 : 1) + (second == null ? 0 : 1) + (third == null ? 0 : 1);
    }

    /**
     * Collects CRT headers straight into an array. The initial capacity is one slot per added header and one per header
     * name, so the array is filled exactly, without a copy, when every header name has one value. A name with several values
     * grows the array, and {@link #build()} trims it to the number of headers added.
     */
    private static final class CrtHeaderArrayBuilder implements BiConsumer<String, List<String>> {
        private HttpHeader[] headers;
        private int size;

        private CrtHeaderArrayBuilder(int initialCapacity) {
            this.headers = new HttpHeader[initialCapacity];
        }

        @Override
        public void accept(String name, List<String> values) {
            int valueCount = values.size();
            ensureCapacity(size + valueCount);
            if (values instanceof RandomAccess) {
                for (int i = 0; i < valueCount; i++) {
                    headers[size++] = new HttpHeader(name, values.get(i));
                }
            } else {
                for (String value : values) {
                    headers[size++] = new HttpHeader(name, value);
                }
            }
        }

        private void addIfPresent(HttpHeader header) {
            if (header != null) {
                ensureCapacity(size + 1);
                headers[size++] = header;
            }
        }

        private void ensureCapacity(int capacity) {
            if (capacity > headers.length) {
                headers = Arrays.copyOf(headers, Math.max(capacity, headers.length + (headers.length >> 1)));
            }
        }

        private HttpHeader[] build() {
            return size == headers.length ? headers : Arrays.copyOf(headers, size);
        }
    }
}
