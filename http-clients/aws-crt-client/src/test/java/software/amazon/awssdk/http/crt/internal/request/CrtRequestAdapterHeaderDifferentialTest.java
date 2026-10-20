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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.http.HttpRequestBase;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.http.crt.internal.CrtAsyncRequestContext;
import software.amazon.awssdk.http.crt.internal.CrtRequestContext;

/**
 * Compares the headers that {@link CrtRequestAdapter} puts on the CRT request with {@link Reference}, a copy of the header
 * code before the list and the per-header streams were replaced with a direct array (racecar E12, task 9.25).
 *
 * <p>The seed and the number of requests can be changed with {@code -Dcrt.headers.seed} and {@code -Dcrt.headers.requests}.
 */
class CrtRequestAdapterHeaderDifferentialTest {
    private static final long SEED = Long.getLong("crt.headers.seed", 20261025L);
    private static final int REQUESTS = Integer.getInteger("crt.headers.requests", 5_000);

    /**
     * The headers that the adapter checks, in several spellings, and names that only look like them.
     */
    private static final List<String> SPECIAL_NAMES = Arrays.asList(
        "Host", "host", "HOST", "hOsT", "Hosts", "X-Host",
        "Connection", "connection", "CONNECTION", "cOnNeCtIoN", "Connections",
        "Content-Length", "content-length", "CONTENT-LENGTH", "cOnTeNt-LeNgTh", "Content-Lengths",
        "Transfer-Encoding", "transfer-encoding", "TRANSFER-ENCODING", "TrAnSfEr-EnCoDiNg", "Transfer-Encodings",
        "Content-Type", "User-Agent", "X-Amz-Date", "x-amz-date", "Authorization", "amz-sdk-invocation-id", "");

    /**
     * Hosts as {@code SdkHttpRequest} holds them. {@code my_host.example.com} is a valid request host, but {@link URI#getHost()}
     * returns {@code null} for it, so the previous code failed when it had to add a {@code Host} header. {@code bad host} makes
     * {@code getUri()} throw.
     */
    private static final List<String> HOSTS = Arrays.asList("localhost", "example.com", "Example.COM", "127.0.0.1", "[::1]",
                                                            "s3.us-west-2.amazonaws.com", "my_host.example.com",
                                                            "bad host");

    private static final List<Integer> PORTS = Arrays.asList(null, 80, 443, 8080);

    private static final List<String> PATHS = Arrays.asList("", "/", "/a/b", "/%2F");

    private static final List<SdkHttpMethod> METHODS = Arrays.asList(SdkHttpMethod.GET, SdkHttpMethod.HEAD,
                                                                      SdkHttpMethod.DELETE, SdkHttpMethod.POST,
                                                                      SdkHttpMethod.PUT, SdkHttpMethod.PATCH);

    @Test
    void randomRequests_asyncAndSync_sameHeadersAsReference() {
        Random random = new Random(SEED);
        for (int i = 0; i < REQUESTS; i++) {
            SdkHttpRequest request = randomRequest(random);
            if (random.nextInt(4) == 0) {
                request = new LinkedListHeaderRequest(request);
            }
            Protocol protocol = random.nextBoolean() ? Protocol.HTTP1_1 : Protocol.HTTP2;
            Long contentLength = randomContentLength(random);
            boolean syncBody = random.nextBoolean();
            SdkHttpRequest finalRequest = request;

            assertThat(outcome(() -> headersOf(toAsyncCrtRequest(finalRequest, contentLength, protocol).getHeadersAsArray())))
                .as("async request %s (seed %s, %s, content length %s): %s", i, SEED, protocol, contentLength,
                    request.headers())
                .isEqualTo(outcome(() -> headersOf(Reference.asyncHeaders(finalRequest, publisher(contentLength), protocol))));

            assertThat(outcome(() -> headersOf(toCrtRequest(finalRequest, syncBody).getHeadersAsArray())))
                .as("sync request %s (seed %s, body %s): %s", i, SEED, syncBody, request.headers())
                .isEqualTo(outcome(() -> headersOf(Reference.syncHeaders(finalRequest))));
        }
    }

    @Test
    void multiValuedAndEmptyHeaders_keepTheirOrderAfterTheAddedHeaders() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("z-Header", Arrays.asList("z1", "", "z1"));
        headers.put("Empty", Collections.emptyList());
        headers.put("a-Header", Collections.singletonList("a1"));
        SdkHttpRequest request = SdkHttpFullRequest.builder()
                                                   .uri(URI.create("http://localhost:8080"))
                                                   .method(SdkHttpMethod.PUT)
                                                   .headers(headers)
                                                   .build();

        List<String> expected = Arrays.asList("Host: localhost", "Connection: keep-alive", "Content-Length: 7",
                                              "a-Header: a1", "z-Header: z1", "z-Header: ", "z-Header: z1");
        assertThat(headersOf(toAsyncCrtRequest(request, 7L, Protocol.HTTP1_1).getHeadersAsArray()))
            .containsExactlyElementsOf(expected)
            .containsExactlyElementsOf(headersOf(Reference.asyncHeaders(request, publisher(7L), Protocol.HTTP1_1)));
        assertThat(headersOf(toAsyncCrtRequest(new LinkedListHeaderRequest(request), 7L, Protocol.HTTP1_1).getHeadersAsArray()))
            .containsExactlyElementsOf(expected);
        assertThat(headersOf(toCrtRequest(new LinkedListHeaderRequest(request), true).getHeadersAsArray()))
            .containsExactly("Host: localhost", "Connection: keep-alive", "a-Header: a1", "z-Header: z1", "z-Header: ",
                             "z-Header: z1");
    }

    @Test
    void hostThatUriCannotParse_failsAsBefore_whenNoHostHeader_andIsIgnored_whenHostHeaderIsSet() {
        SdkHttpRequest request = SdkHttpFullRequest.builder()
                                                   .protocol("https")
                                                   .host("my_host.example.com")
                                                   .method(SdkHttpMethod.GET)
                                                   .build();
        SdkHttpRequest withHostHeader = request.toBuilder().putHeader("host", "my_host.example.com").build();

        assertThat(outcome(() -> headersOf(toAsyncCrtRequest(request, null, Protocol.HTTP1_1).getHeadersAsArray())))
            .isEqualTo(Collections.singletonList(NullPointerException.class.getName()));
        assertThat(outcome(() -> headersOf(toCrtRequest(request, false).getHeadersAsArray())))
            .isEqualTo(Collections.singletonList(NullPointerException.class.getName()));
        assertThat(headersOf(toAsyncCrtRequest(withHostHeader, null, Protocol.HTTP2).getHeadersAsArray()))
            .containsExactly("host: my_host.example.com");
    }

    private static SdkHttpRequest randomRequest(Random random) {
        SdkHttpFullRequest.Builder builder = SdkHttpFullRequest.builder()
                                                               .protocol(random.nextBoolean() ? "http" : "https")
                                                               .host(HOSTS.get(random.nextInt(HOSTS.size())))
                                                               .port(PORTS.get(random.nextInt(PORTS.size())))
                                                               .encodedPath(PATHS.get(random.nextInt(PATHS.size())))
                                                               .method(METHODS.get(random.nextInt(METHODS.size())));
        if (random.nextInt(3) == 0) {
            builder.putRawQueryParameter("q", randomValue(random));
        }
        int headerCount = random.nextInt(12);
        for (int i = 0; i < headerCount; i++) {
            String name = random.nextInt(3) == 0 ? randomName(random) : SPECIAL_NAMES.get(random.nextInt(SPECIAL_NAMES.size()));
            List<String> values = new ArrayList<>();
            int valueCount = random.nextInt(4) == 0 ? random.nextInt(5) : 1;
            for (int v = 0; v < valueCount; v++) {
                values.add(randomValue(random));
            }
            if (random.nextBoolean()) {
                builder.putHeader(name, values);
            } else {
                for (String value : values) {
                    builder.appendHeader(name, value);
                }
            }
        }
        return builder.build();
    }

    private static Long randomContentLength(Random random) {
        switch (random.nextInt(3)) {
            case 0:
                return null;
            case 1:
                return 0L;
            default:
                return (long) random.nextInt(1_000_000);
        }
    }

    private static String randomName(Random random) {
        String alphabet = "abcHOSTlengthCONNECTIONtransferXYZ-_";
        int length = 1 + random.nextInt(18);
        StringBuilder name = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            name.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return name.toString();
    }

    private static String randomValue(Random random) {
        switch (random.nextInt(6)) {
            case 0:
                return "";
            case 1:
                return "chunked";
            case 2:
                return "close";
            case 3:
                return Integer.toString(random.nextInt(100_000));
            case 4:
                return "caf\u00e9 " + random.nextInt(10);
            default:
                return "v" + random.nextInt(10) + " " + random.nextInt(10);
        }
    }

    /**
     * The header list as {@code name: value} strings, or the class name of the exception that building it threw.
     */
    private static List<String> outcome(Supplier<List<String>> headers) {
        try {
            return headers.get();
        } catch (RuntimeException e) {
            return Collections.singletonList(e.getClass().getName());
        }
    }

    private static List<String> headersOf(HttpHeader[] headers) {
        return headersOf(Arrays.asList(headers));
    }

    private static List<String> headersOf(List<HttpHeader> headers) {
        List<String> result = new ArrayList<>(headers.size());
        for (HttpHeader header : headers) {
            result.add(header.getName() + ": " + header.getValue());
        }
        return result;
    }

    private static HttpRequestBase toAsyncCrtRequest(SdkHttpRequest request, Long contentLength, Protocol protocol) {
        AsyncExecuteRequest asyncRequest = AsyncExecuteRequest.builder()
                                                              .request(request)
                                                              .requestContentPublisher(publisher(contentLength))
                                                              .build();
        return CrtRequestAdapter.toAsyncCrtRequest(CrtAsyncRequestContext.builder()
                                                                         .request(asyncRequest)
                                                                         .readBufferSize(2000)
                                                                         .protocol(protocol)
                                                                         .build());
    }

    private static HttpRequestBase toCrtRequest(SdkHttpRequest request, boolean withBody) {
        HttpExecuteRequest.Builder executeRequest = HttpExecuteRequest.builder().request(request);
        if (withBody) {
            executeRequest.contentStreamProvider(() -> new ByteArrayInputStream(new byte[] {1, 2, 3}));
        }
        return CrtRequestAdapter.toCrtRequest(CrtRequestContext.builder()
                                                               .request(executeRequest.build())
                                                               .readBufferSize(2000)
                                                               .build());
    }

    private static SdkHttpContentPublisher publisher(Long contentLength) {
        return new SdkHttpContentPublisher() {
            @Override
            public Optional<Long> contentLength() {
                return Optional.ofNullable(contentLength);
            }

            @Override
            public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            }
        };
    }

    /**
     * A request whose {@code forEachHeader} hands out {@link LinkedList} values, to cover lists without random access.
     */
    private static final class LinkedListHeaderRequest implements SdkHttpRequest {
        private final SdkHttpRequest delegate;
        private final Map<String, List<String>> headers = new LinkedHashMap<>();

        private LinkedListHeaderRequest(SdkHttpRequest delegate) {
            this.delegate = delegate;
            delegate.forEachHeader((name, values) -> headers.put(name, new LinkedList<>(values)));
        }

        @Override
        public Map<String, List<String>> headers() {
            return headers;
        }

        @Override
        public String protocol() {
            return delegate.protocol();
        }

        @Override
        public String host() {
            return delegate.host();
        }

        @Override
        public int port() {
            return delegate.port();
        }

        @Override
        public String encodedPath() {
            return delegate.encodedPath();
        }

        @Override
        public Map<String, List<String>> rawQueryParameters() {
            return delegate.rawQueryParameters();
        }

        @Override
        public Optional<String> encodedQueryParameters() {
            return delegate.encodedQueryParameters();
        }

        @Override
        public SdkHttpMethod method() {
            return delegate.method();
        }

        @Override
        public Builder toBuilder() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * {@code CrtRequestAdapter.createAsyncHttpHeaderList} and {@code createHttpHeaderList} as they were before the change,
     * with the {@code getUri()} call that the request methods made before them.
     */
    private static final class Reference {
        static List<HttpHeader> asyncHeaders(SdkHttpRequest sdkRequest, SdkHttpContentPublisher publisher, Protocol protocol) {
            URI uri = sdkRequest.getUri();
            // worst case we may add 3 more headers here
            List<HttpHeader> crtHeaderList = new ArrayList<>(sdkRequest.numHeaders() + 3);

            // Set Host Header if needed
            if (!sdkRequest.firstMatchingHeader(Header.HOST).isPresent()) {
                crtHeaderList.add(new HttpHeader(Header.HOST, uri.getHost()));
            }

            // Add Connection Keep Alive Header for HTTP/1.1 only (forbidden in HTTP/2 per RFC 7540)
            if (protocol != Protocol.HTTP2 && !sdkRequest.firstMatchingHeader(Header.CONNECTION).isPresent()) {
                crtHeaderList.add(new HttpHeader(Header.CONNECTION, Header.KEEP_ALIVE_VALUE));
            }

            // Set Content-Length if needed, but never alongside Transfer-Encoding. RFC 7230 forbids sending both
            Optional<Long> contentLength = publisher.contentLength();
            if (!sdkRequest.firstMatchingHeader(Header.CONTENT_LENGTH).isPresent()
                && !sdkRequest.firstMatchingHeader(Header.TRANSFER_ENCODING).isPresent()
                && contentLength.isPresent()) {
                crtHeaderList.add(new HttpHeader(Header.CONTENT_LENGTH, Long.toString(contentLength.get())));
            }

            // Add the rest of the Headers
            sdkRequest.forEachHeader((key, value) -> value.stream()
                                                          .map(val -> new HttpHeader(key, val))
                                                          .forEach(crtHeaderList::add));

            return crtHeaderList;
        }

        static List<HttpHeader> syncHeaders(SdkHttpRequest sdkRequest) {
            URI uri = sdkRequest.getUri();
            // worst case we may add 3 more headers here
            List<HttpHeader> crtHeaderList = new ArrayList<>(sdkRequest.numHeaders() + 3);

            // Set Host Header if needed
            if (!sdkRequest.firstMatchingHeader(Header.HOST).isPresent()) {
                crtHeaderList.add(new HttpHeader(Header.HOST, uri.getHost()));
            }

            // Add Connection Keep Alive Header to reuse this Http Connection as long as possible
            if (!sdkRequest.firstMatchingHeader(Header.CONNECTION).isPresent()) {
                crtHeaderList.add(new HttpHeader(Header.CONNECTION, Header.KEEP_ALIVE_VALUE));
            }

            // Add the rest of the Headers
            sdkRequest.forEachHeader((key, value) -> value.stream()
                                                          .map(val -> new HttpHeader(key, val))
                                                          .forEach(crtHeaderList::add));

            return crtHeaderList;
        }
    }
}
