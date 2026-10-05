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

package software.amazon.awssdk.http;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.statistics.Statistics;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.sync.RequestBody;

/**
 * Property 40: HTTP representation preserves protocol-significant data (Requirements 12.2, 12.3, 12.4).
 *
 * <p>Each try generates the raw wire components of an HTTP/1.1 or HTTP/2 request (method, scheme, DNS/IPv4/IPv6
 * host, default or explicit port, a raw path of pchar and percent escapes, ordered query entries given raw or decoded
 * with repeated names, empty values and name-only entries, mixed-case repeated headers interleaved with other names
 * whose values carry spaces and tabs, and an empty, buffered or streaming body) and builds the request through the
 * SPI builders. The oracle is the generated components themselves plus an encoder, target joiner and pseudo-header
 * table written here independently of the SPI: the built request, its {@code toBuilder()} copy and a request rebuilt
 * from its accessors (what a transport or signer adapter does) must all expose exactly those components, with no
 * re-encoding, reordering, case folding or collapsing. Responses round-trip the same way.
 *
 * <p>A second property generates inputs that would change the meaning of the request line or header block and
 * asserts that the builders reject each one when it is supplied, leaving the builder's earlier valid state intact.
 */
class HttpRepresentationFidelityPropertyTest {
    private static final String ALPHA = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGIT = "0123456789";
    private static final String HEX = "0123456789ABCDEFabcdef";
    private static final String UNRESERVED = ALPHA + DIGIT + "-._~";
    private static final String SUB_DELIMS = "!$&'()*+,;=";
    /** RFC 3986 pchar without the percent escapes, which are generated separately. */
    private static final String PCHAR = UNRESERVED + SUB_DELIMS + ":@";
    /** Query text: pchar, {@code /} and {@code ?}, minus the entry and name/value delimiters. */
    private static final String QUERY_VALUE_CHARS = (PCHAR + "/?").replace("&", "");
    private static final String QUERY_NAME_CHARS = QUERY_VALUE_CHARS.replace("=", "");
    /** RFC 9110 tchar. */
    private static final String TCHAR = ALPHA + DIGIT + "!#$%&'*+-.^_`|~";
    private static final String NON_HEX = "gGxXzZ-_~.";
    private static final List<String> METHODS = List.of("GET", "PUT", "POST", "DELETE", "HEAD", "PATCH", "OPTIONS");
    private static final List<String> COMMON_HEADERS =
        List.of("X-Amz-Meta-A", "Content-Type", "x-amz-date", "Accept", "Cache-Control");
    /** Code points of decoded query text: ASCII (with delimiters and %), and multi-byte UTF-8 characters. */
    private static final List<Integer> DECODED_EXTRA = List.of(0xE9, 0x20AC, 0x65E5, 0x1F600, 0x0, 0x7F, 0xA0);

    enum Protocol { HTTP1_1, HTTP2 }

    enum BodyKind { EMPTY, BYTES, SPI_STREAM, SYNC_ADAPTER, ASYNC_ADAPTER }

    /** How a query entry reaches the builder. */
    enum QueryRoute { RAW, RAW_RECORD, DECODED, DECODED_RECORD }

    record QuerySpec(QueryRoute route, String name, String value) {
        boolean decoded() {
            return route == QueryRoute.DECODED || route == QueryRoute.DECODED_RECORD;
        }
    }

    record HeaderSpec(String name, String value) {
    }

    record BodySpec(BodyKind kind, byte[] bytes) {
        @Override
        public String toString() {
            return "BodySpec[" + kind + ", " + bytes.length + " bytes]";
        }
    }

    record TargetSpec(String method, String scheme, String host, int port, String path, List<QuerySpec> query) {
    }

    record RequestSpec(Protocol protocol, TargetSpec target, List<HeaderSpec> headers, boolean headersAsBlock,
                       BodySpec body) {
    }

    record ResponseSpec(int status, List<HeaderSpec> headers, BodySpec body) {
    }

    // Feature: aws-sdk-java-v3, Property 40: HTTP representation preserves protocol-significant data
    @Property(tries = 1_000)
    @Label("Property 40: HTTP representation preserves protocol-significant data")
    void httpRepresentationPreservesProtocolSignificantData(@ForAll("requests") RequestSpec spec,
                                                           @ForAll("responses") ResponseSpec responseSpec) {
        Statistics.label("protocol").collect(spec.protocol());
        Statistics.label("request body").collect(spec.body().kind());
        Statistics.label("port").collect(spec.target().port() < 0 ? "default" : "explicit");
        Statistics.label("repeated header name").collect(hasRepeatedName(spec.headers()));

        HttpBody body = body(spec.body());
        SdkHttpRequest built = build(spec, body);
        SdkHttpRequest copied = built.toBuilder().build();
        SdkHttpRequest adapted = adapt(built);

        for (SdkHttpRequest request : List.of(built, copied, adapted)) {
            assertMatchesOracle(spec, request);
            assertSame(body, request.body());
        }
        assertEquals(built, copied);
        assertEquals(built, adapted);
        assertEquals(built.hashCode(), adapted.hashCode());

        // Copy-on-write: deriving a modified request leaves the original untouched and appends after every field.
        SdkHttpRequest derived = built.toBuilder().addHeader("X-Derived", "1").addRawQueryParameter("derived", null)
                                      .build();
        assertMatchesOracle(spec, built);
        assertEquals(built.headers().size() + 1, derived.headers().size());
        assertEquals("X-Derived", derived.headers().name(built.headers().size()));
        assertEquals(expectedTarget(spec.target()) + (spec.target().query().isEmpty() ? "?" : "&") + "derived",
                     derived.rawTarget());

        assertBodyMatches(spec.body(), body);

        HttpBody responseBody = body(responseSpec.body());
        SdkHttpResponse response = buildResponse(responseSpec, responseBody);
        SdkHttpResponse responseCopy = response.toBuilder().build();
        SdkHttpResponse responseAdapted = SdkHttpResponse.builder().statusCode(response.statusCode())
                                                         .headers(response.headers()).body(response.body()).build();
        for (SdkHttpResponse r : List.of(response, responseCopy, responseAdapted)) {
            assertEquals(responseSpec.status(), r.statusCode());
            assertEquals(responseSpec.status() / 100 == 2, r.isSuccessful());
            assertHeadersMatch(responseSpec.headers(), r.headers());
            assertSame(responseBody, r.body());
        }
        assertEquals(response, responseCopy);
        assertEquals(response, responseAdapted);
        assertBodyMatches(responseSpec.body(), responseBody);
    }

    // Feature: aws-sdk-java-v3, Property 40: HTTP representation preserves protocol-significant data
    @Property(tries = 1_000)
    @Label("Property 40: HTTP representation rejects components that would change the wire meaning")
    void invalidComponentsAreRejectedAtBuildTime(@ForAll("requests") RequestSpec baseline,
                                                 @ForAll("invalidInputs") Invalid invalid) {
        Statistics.label("invalid kind").collect(invalid.kind());
        SdkHttpRequest.Builder builder = builder(baseline, HttpBody.empty());
        SdkHttpRequest before = builder.build();

        assertThrows(IllegalArgumentException.class, () -> invalid.attempt().accept(builder),
                     () -> invalid.kind() + " was accepted: " + escape(invalid.text()));

        // A rejected component changes nothing already in the builder.
        SdkHttpRequest after = builder.build();
        assertEquals(before, after);
        assertEquals(before.rawTarget(), after.rawTarget());
        assertEquals(before.pseudoHeaders(), after.pseudoHeaders());
    }

    // ---- oracle ----

    private static void assertMatchesOracle(RequestSpec spec, SdkHttpRequest request) {
        TargetSpec target = spec.target();
        String method = target.method().toUpperCase(Locale.ROOT);
        String scheme = target.scheme().toLowerCase(Locale.ROOT);
        String path = target.path().isEmpty() ? "/" : target.path();
        String query = expectedQuery(target.query());
        String rawTarget = expectedTarget(target);
        String authority = target.port() < 0 ? target.host() : target.host() + ":" + target.port();

        // Request line (HTTP/1.1) and target components, byte for byte.
        assertEquals(method, request.method());
        assertEquals(scheme, request.scheme());
        assertEquals(target.host(), request.host());
        assertEquals(target.port(), request.port());
        assertEquals(authority, request.authority());
        assertEquals(path, request.encodedPath());
        assertEquals(query, request.encodedQuery());
        assertEquals(rawTarget, request.rawTarget());
        assertEquals(target.query().size(), request.queryParameters().size());
        for (int i = 0; i < target.query().size(); i++) {
            QuerySpec entry = target.query().get(i);
            HttpQueryParameter actual = request.queryParameters().get(i);
            assertEquals(expectedRawName(entry), actual.rawName());
            assertEquals(expectedRawValue(entry), actual.rawValue());
        }
        if (spec.protocol() == Protocol.HTTP1_1) {
            assertEquals(method + " " + rawTarget + " HTTP/1.1", request.method() + " " + request.rawTarget()
                                                                 + " HTTP/1.1");
        }

        // HTTP/2 pseudo-header fields, in transmission order; the port only if explicit.
        List<Map.Entry<String, String>> pseudo = List.of(Map.entry(":method", method), Map.entry(":scheme", scheme),
                                                         Map.entry(":authority", authority),
                                                         Map.entry(":path", rawTarget));
        assertEquals(pseudo, request.pseudoHeaders());

        assertHeadersMatch(spec.headers(), request.headers());
    }

    private static void assertHeadersMatch(List<HeaderSpec> expected, HttpHeaders actual) {
        assertEquals(expected.size(), actual.size());
        assertEquals(expected.isEmpty(), actual.isEmpty());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).name(), actual.name(i), "name of field " + i);
            assertEquals(expected.get(i).value(), actual.value(i), "value of field " + i);
        }
        for (HeaderSpec field : expected) {
            List<String> values = expected.stream().filter(f -> f.name().equalsIgnoreCase(field.name()))
                                          .map(HeaderSpec::value).collect(Collectors.toList());
            String probe = flipCase(field.name());
            assertEquals(values, actual.allValues(probe));
            assertEquals(Optional.of(values.get(0)), actual.firstValue(probe));
            assertTrue(actual.contains(probe));
        }
        assertEquals(actual, actual.toBuilder().build());
    }

    private static void assertBodyMatches(BodySpec spec, HttpBody body) {
        byte[] bytes = spec.bytes();
        switch (spec.kind()) {
            case EMPTY -> {
                assertEquals(HttpBody.Kind.EMPTY, body.kind());
                assertEquals(OptionalLong.of(0), body.contentLength());
                assertArrayEquals(new byte[0], body.asByteArray());
            }
            case BYTES -> {
                assertEquals(bytes.length == 0 ? HttpBody.Kind.EMPTY : HttpBody.Kind.BYTES, body.kind());
                assertEquals(OptionalLong.of(bytes.length), body.contentLength());
                assertArrayEquals(bytes, body.asByteArray());
                ByteBuffer buffer = body.asByteBuffer();
                assertTrue(buffer.isReadOnly());
                assertEquals(bytes.length, buffer.remaining());
            }
            default -> {
                assertEquals(HttpBody.Kind.STREAMING, body.kind());
                assertEquals(OptionalLong.of(bytes.length), body.contentLength());
                StreamingBody stream = body.streamingBody();
                assertEquals(OptionalLong.of(bytes.length), stream.contentLength());
                assertArrayEquals(bytes, readPublisher(stream));
                // Every generated source is replayable, including AsyncRequestBody.fromBytes through the adapter.
                assertTrue(stream.isReplayable());
                assertArrayEquals(bytes, readStream(stream));
            }
        }
    }

    /** The raw query string: the entries in order joined by {@code &}, each raw form exactly as generated. */
    private static String expectedQuery(List<QuerySpec> query) {
        StringBuilder out = new StringBuilder();
        for (QuerySpec entry : query) {
            if (!out.isEmpty()) {
                out.append('&');
            }
            out.append(expectedRawName(entry));
            String value = expectedRawValue(entry);
            if (value != null) {
                out.append('=').append(value);
            }
        }
        return out.toString();
    }

    private static String expectedTarget(TargetSpec target) {
        String path = target.path().isEmpty() ? "/" : target.path();
        String query = expectedQuery(target.query());
        return query.isEmpty() ? path : path + "?" + query;
    }

    private static String expectedRawName(QuerySpec entry) {
        return entry.decoded() ? oracleEncode(entry.name()) : entry.name();
    }

    private static String expectedRawValue(QuerySpec entry) {
        if (entry.value() == null) {
            return null;
        }
        return entry.decoded() ? oracleEncode(entry.value()) : entry.value();
    }

    /** RFC 3986 encoding as AWS applies it: every UTF-8 byte other than unreserved as upper-case {@code %XX}. */
    private static String oracleEncode(String text) {
        StringBuilder out = new StringBuilder();
        for (byte b : text.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xFF;
            if (c < 0x80 && UNRESERVED.indexOf(c) >= 0) {
                out.append((char) c);
            } else {
                out.append(String.format(Locale.ROOT, "%%%02X", c));
            }
        }
        return out.toString();
    }

    // ---- SPI under test ----

    private static SdkHttpRequest.Builder builder(RequestSpec spec, HttpBody body) {
        TargetSpec target = spec.target();
        SdkHttpRequest.Builder builder = SdkHttpRequest.builder().method(target.method()).scheme(target.scheme())
                                                       .host(target.host()).port(target.port())
                                                       .encodedPath(target.path()).body(body);
        for (QuerySpec entry : target.query()) {
            switch (entry.route()) {
                case RAW -> builder.addRawQueryParameter(entry.name(), entry.value());
                case RAW_RECORD -> builder.addQueryParameter(HttpQueryParameter.raw(entry.name(), entry.value()));
                case DECODED -> builder.addQueryParameter(entry.name(), entry.value());
                case DECODED_RECORD -> builder.addQueryParameter(HttpQueryParameter.of(entry.name(), entry.value()));
                default -> throw new AssertionError(entry.route());
            }
        }
        if (spec.headersAsBlock()) {
            HttpHeaders.Builder headers = HttpHeaders.builder();
            spec.headers().forEach(h -> headers.add(h.name(), h.value()));
            builder.headers(headers.build());
        } else {
            spec.headers().forEach(h -> builder.addHeader(h.name(), h.value()));
        }
        return builder;
    }

    private static SdkHttpRequest build(RequestSpec spec, HttpBody body) {
        return builder(spec, body).build();
    }

    /** A request rebuilt from another's accessors, as a transport or signer adapter copies one. */
    private static SdkHttpRequest adapt(SdkHttpRequest request) {
        SdkHttpRequest.Builder builder = SdkHttpRequest.builder().method(request.method()).scheme(request.scheme())
                                                       .host(request.host()).port(request.port())
                                                       .encodedPath(request.encodedPath()).body(request.body());
        request.queryParameters().forEach(p -> builder.addRawQueryParameter(p.rawName(), p.rawValue()));
        HttpHeaders headers = request.headers();
        for (int i = 0; i < headers.size(); i++) {
            builder.addHeader(headers.name(i), headers.value(i));
        }
        return builder.build();
    }

    private static SdkHttpResponse buildResponse(ResponseSpec spec, HttpBody body) {
        SdkHttpResponse.Builder builder = SdkHttpResponse.builder().statusCode(spec.status()).body(body);
        spec.headers().forEach(h -> builder.addHeader(h.name(), h.value()));
        return builder.build();
    }

    private static HttpBody body(BodySpec spec) {
        byte[] bytes = spec.bytes();
        return switch (spec.kind()) {
            case EMPTY -> HttpBody.empty();
            case BYTES -> bytes.length % 2 == 0 ? HttpBody.fromBytes(bytes) : HttpBody.fromBytes(ByteBuffer.wrap(bytes));
            case SPI_STREAM -> HttpBody.streaming(StreamingBodies.fromBytes(bytes, "application/octet-stream"));
            case SYNC_ADAPTER -> RequestBodyAdapter.toHttpBody(RequestBody.fromBytes(bytes));
            case ASYNC_ADAPTER -> RequestBodyAdapter.toHttpBody(AsyncRequestBody.fromBytes(bytes));
        };
    }

    private static byte[] readStream(StreamingBody body) {
        try (InputStream in = body.openStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Reads the demand-driven view one chunk per request, checking each chunk against the stream's chunk bound. */
    private static byte[] readPublisher(StreamingBody body) {
        CompletableFuture<byte[]> done = new CompletableFuture<>();
        body.subscribe(new Subscriber<ByteBuffer>() {
            private final ByteArrayOutputStream out = new ByteArrayOutputStream();
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription s) {
                subscription = s;
                s.request(1);
            }

            @Override
            public void onNext(ByteBuffer chunk) {
                if (chunk.remaining() > body.maxChunkSize()) {
                    done.completeExceptionally(new AssertionError("chunk of " + chunk.remaining() + " bytes"));
                    subscription.cancel();
                    return;
                }
                byte[] data = new byte[chunk.remaining()];
                chunk.get(data);
                out.write(data, 0, data.length);
                subscription.request(1);
            }

            @Override
            public void onError(Throwable t) {
                done.completeExceptionally(t);
            }

            @Override
            public void onComplete() {
                done.complete(out.toByteArray());
            }
        });
        try {
            return done.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new AssertionError("reading the body failed", e);
        }
    }

    // ---- generators of valid components ----

    @Provide
    Arbitrary<RequestSpec> requests() {
        Arbitrary<TargetSpec> targets = Combinators.combine(methods(), schemes(), hosts(), ports(), paths(),
                                                            queryEntries().list().ofMaxSize(8))
                                                   .as(TargetSpec::new);
        return Combinators.combine(Arbitraries.of(Protocol.class), targets, headerFields(), Arbitraries.of(true, false),
                                   bodies())
                          .as(RequestSpec::new);
    }

    @Provide
    Arbitrary<ResponseSpec> responses() {
        return Combinators.combine(Arbitraries.integers().between(100, 599), headerFields(), bodies())
                          .as(ResponseSpec::new);
    }

    private static Arbitrary<String> methods() {
        return Arbitraries.of(METHODS).flatMap(m -> Arbitraries.of(m, m.toLowerCase(Locale.ROOT), capitalize(m)));
    }

    private static Arbitrary<String> schemes() {
        return Arbitraries.of("http", "https", "HTTPS", "Http");
    }

    private static Arbitrary<String> hosts() {
        Arbitrary<String> label = Combinators.combine(chars(ALPHA + DIGIT, 1, 6), Arbitraries.of("", "-", "_", "--"),
                                                      chars(ALPHA + DIGIT, 0, 6))
                                             .as((a, sep, b) -> b.isEmpty() ? a : a + sep + b);
        Arbitrary<String> dns = label.list().ofMinSize(1).ofMaxSize(4).map(l -> String.join(".", l));
        Arbitrary<String> ipv4 = Arbitraries.integers().between(0, 255).list().ofSize(4)
                                            .map(l -> l.stream().map(String::valueOf).collect(Collectors.joining(".")));
        Arbitrary<String> group = chars(HEX, 1, 4);
        Arbitrary<String> fullIpv6 = group.list().ofSize(8).map(l -> "[" + String.join(":", l) + "]");
        Arbitrary<String> compressedIpv6 = Combinators.combine(group.list().ofMaxSize(3), group.list().ofMaxSize(3))
                                                      .as((a, b) -> "[" + String.join(":", a) + "::"
                                                                    + String.join(":", b) + "]");
        return Arbitraries.frequencyOf(weighted(6, dns), weighted(2, ipv4), weighted(1, fullIpv6),
                                       weighted(1, compressedIpv6),
                                       weighted(1, Arbitraries.of("localhost", "[::1]", "127.0.0.1")));
    }

    private static Arbitrary<Integer> ports() {
        return Arbitraries.frequencyOf(weighted(4, Arbitraries.just(-1)),
                                       weighted(2, Arbitraries.of(80, 443, 0, 65535)),
                                       weighted(3, Arbitraries.integers().between(1, 65535)));
    }

    /** A raw path: {@code /}-separated segments (empty ones included) of pchar and percent escapes of any case. */
    private static Arbitrary<String> paths() {
        Arbitrary<String> segment = pctText(PCHAR, 0, 6);
        Arbitrary<String> nonEmpty = segment.list().ofMinSize(1).ofMaxSize(6)
                                            .map(l -> "/" + String.join("/", l));
        return Arbitraries.frequencyOf(weighted(1, Arbitraries.of("", "/", "//", "/a/", "/%2F%2f")),
                                       weighted(8, nonEmpty));
    }

    /** Query entries from a small name pool, so repeated names are common. */
    private static Arbitrary<QuerySpec> queryEntries() {
        Arbitrary<String> rawName = Arbitraries.frequencyOf(weighted(3, Arbitraries.of("tag", "list-type", "a")),
                                                            weighted(2, pctText(QUERY_NAME_CHARS, 1, 6)));
        Arbitrary<String> rawValue = Arbitraries.<String>frequencyOf(weighted(1, Arbitraries.just("")),
                                                                     weighted(4, pctText(QUERY_VALUE_CHARS, 1, 8)))
                                                .injectNull(0.2);
        Arbitrary<QuerySpec> raw = Combinators.combine(Arbitraries.of(QueryRoute.RAW, QueryRoute.RAW_RECORD), rawName,
                                                       rawValue).as(QuerySpec::new);
        Arbitrary<String> decodedName = Arbitraries.frequencyOf(weighted(2, Arbitraries.of("tag", "a b", "k&=")),
                                                                weighted(3, decodedText(1, 6)));
        Arbitrary<String> decodedValue = Arbitraries.<String>frequencyOf(weighted(1, Arbitraries.just("")),
                                                                         weighted(4, decodedText(1, 8)))
                                                    .injectNull(0.2);
        Arbitrary<QuerySpec> decoded = Combinators.combine(Arbitraries.of(QueryRoute.DECODED,
                                                                          QueryRoute.DECODED_RECORD),
                                                           decodedName, decodedValue).as(QuerySpec::new);
        return Arbitraries.oneOf(raw, decoded);
    }

    /**
     * Header fields whose names come mostly from a small pool in random case, so repeats of one name in different
     * case are interleaved with other names; values are visible ASCII with spaces and tabs anywhere.
     */
    private static Arbitrary<List<HeaderSpec>> headerFields() {
        Arbitrary<String> name = Arbitraries.frequencyOf(
            weighted(3, Combinators.combine(Arbitraries.of(COMMON_HEADERS), Arbitraries.longs())
                                        .as((n, bits) -> randomCase(n, bits))),
            weighted(1, chars(TCHAR, 1, 10)));
        StringBuilder visible = new StringBuilder();
        for (char c = 0x21; c < 0x7F; c++) {
            visible.append(c);
        }
        Arbitrary<String> value = Arbitraries.frequencyOf(
            weighted(1, Arbitraries.of("", " ", "\t", " a\tb ")),
            weighted(5, chars(visible + " \t", 1, 20)));
        return Combinators.combine(name, value).as(HeaderSpec::new).list().ofMaxSize(12);
    }

    private static Arbitrary<BodySpec> bodies() {
        Arbitrary<byte[]> small = Arbitraries.bytes().array(byte[].class).ofMaxSize(2048);
        Arbitrary<byte[]> large = Arbitraries.integers().between(StreamingBodies.DEFAULT_MAX_CHUNK_SIZE - 1,
                                                                 2 * StreamingBodies.DEFAULT_MAX_CHUNK_SIZE + 1)
                                             .map(n -> {
                                                 byte[] b = new byte[n];
                                                 for (int i = 0; i < n; i++) {
                                                     b[i] = (byte) (i * 31 + n);
                                                 }
                                                 return b;
                                             });
        Arbitrary<byte[]> bytes = Arbitraries.frequencyOf(weighted(19, small), weighted(1, large));
        return Combinators.combine(Arbitraries.of(BodyKind.class), bytes)
                          .as((kind, b) -> new BodySpec(kind, kind == BodyKind.EMPTY ? new byte[0] : b));
    }

    // ---- generators of invalid components ----

    enum InvalidKind {
        HEADER_VALUE_LINE_BREAK, HEADER_NAME, PATH_CHARACTER, PATH_ESCAPE, QUERY_NAME, QUERY_VALUE, QUERY_ESCAPE,
        HOST, METHOD
    }

    record Invalid(InvalidKind kind, String text, Consumer<SdkHttpRequest.Builder> attempt) {
        @Override
        public String toString() {
            return "Invalid[" + kind + ", " + escape(text) + "]";
        }
    }

    @Provide
    Arbitrary<Invalid> invalidInputs() {
        Arbitrary<String> token = chars(TCHAR, 1, 8);
        Arbitrary<String> fieldValue = chars(ALPHA + " \t", 0, 8);

        Arbitrary<Invalid> headerValue = Combinators.combine(token, insert(fieldValue, Arbitraries.of("\r", "\n", "\0",
                                                                                                    "\r\nX-Injected: 1")),
                                                             Arbitraries.integers().between(0, 3))
                                                    .as((name, value, route) -> new Invalid(
                                                        InvalidKind.HEADER_VALUE_LINE_BREAK, name + ": " + value,
                                                        b -> addHeader(b, route, name, value)));
        Arbitrary<String> badName = Arbitraries.oneOf(token.map(t -> ":" + t),
                                                      insert(token, Arbitraries.of(":", " ", "\t", "\r\n", "\u00e9",
                                                                                   "\u007f")),
                                                      Arbitraries.just(""));
        Arbitrary<Invalid> headerName = Combinators.combine(badName, fieldValue, Arbitraries.integers().between(0, 3))
                                                   .as((name, value, route) -> new Invalid(
                                                       InvalidKind.HEADER_NAME, name, b -> addHeader(b, route, name,
                                                                                                     value)));
        Arbitrary<Invalid> pathChar = insertAfterSlash(paths(), Arbitraries.of("?", "#", " ", "\t", "\r\n", "\u00e9",
                                                                              "\u007f", "\0"))
            .map(p -> new Invalid(InvalidKind.PATH_CHARACTER, p, b -> b.encodedPath(p)));
        Arbitrary<Invalid> pathEscape = Combinators.combine(paths(), malformedEscape())
                                                   .as((p, e) -> (p.isEmpty() ? "/" : p) + e)
                                                   .map(p -> new Invalid(InvalidKind.PATH_ESCAPE, p,
                                                                         b -> b.encodedPath(p)));
        Arbitrary<Invalid> queryName = Arbitraries.oneOf(insert(pctText(QUERY_NAME_CHARS, 0, 6),
                                                                Arbitraries.of("=", "&", "#", " ", "\r\n", "\u00e9")),
                                                         Arbitraries.just(""))
                                                  .map(n -> new Invalid(InvalidKind.QUERY_NAME, n,
                                                                        b -> b.addRawQueryParameter(n, "v")));
        Arbitrary<Invalid> queryValue = insert(pctText(QUERY_VALUE_CHARS, 0, 6),
                                               Arbitraries.of("&", "#", " ", "\r\n", "\u00e9", "\0"))
            .map(v -> new Invalid(InvalidKind.QUERY_VALUE, v,
                                  b -> b.addQueryParameter(HttpQueryParameter.raw("n", v))));
        Arbitrary<Invalid> queryEscape = Combinators.combine(pctText(QUERY_VALUE_CHARS, 0, 6), malformedEscape(),
                                                             Arbitraries.of(true, false))
                                                    .as((v, e, inName) -> new Invalid(
                                                        InvalidKind.QUERY_ESCAPE, v + e,
                                                        inName ? b -> b.addRawQueryParameter("n" + v.replace("=", "")
                                                                                             + e, null)
                                                               : b -> b.addRawQueryParameter("n", v + e)));
        Arbitrary<Invalid> host = Arbitraries.oneOf(insert(hosts().filter(h -> !h.startsWith("[")),
                                                           Arbitraries.of("\r\n", "\n", " ", "\t", "/", "?", "#", "@",
                                                                          ":", "\0", "\u00e9")),
                                                    Arbitraries.of("::1", "[::1", "::1]", "[[::1]]", "[::1]x",
                                                                   "example.com:8080", "user@example.com", "[]"))
                                             .map(h -> new Invalid(InvalidKind.HOST, h, b -> b.host(h)));
        Arbitrary<Invalid> method = insert(Arbitraries.of(METHODS), Arbitraries.of(" ", "\r\n", "\t", "/", "(", ":",
                                                                                   "\0", "\u00e9"))
            .map(m -> new Invalid(InvalidKind.METHOD, m, b -> b.method(m)));
        return Arbitraries.oneOf(headerValue, headerName, pathChar, pathEscape, queryName, queryValue, queryEscape,
                                 host, method);
    }

    /** Adds a header through one of the four header entry points. */
    private static void addHeader(SdkHttpRequest.Builder builder, int route, String name, String value) {
        switch (route) {
            case 0 -> builder.addHeader(name, value);
            case 1 -> builder.putHeader(name, value);
            case 2 -> builder.headers(HttpHeaders.builder().add(name, value).build());
            default -> SdkHttpResponse.builder().addHeader(name, value);
        }
    }

    /** A percent sign not followed by two hex digits, whatever follows it. */
    private static Arbitrary<String> malformedEscape() {
        Arbitrary<String> hex = chars(HEX, 1, 1);
        Arbitrary<String> nonHex = chars(NON_HEX, 1, 1);
        return Arbitraries.oneOf(Combinators.combine(nonHex, hex).as((a, b) -> "%" + a + b),
                                 Combinators.combine(hex, nonHex).as((a, b) -> "%" + a + b),
                                 hex.map(h -> "%" + h), Arbitraries.just("%"));
    }

    // ---- helpers ----

    private static Arbitrary<String> chars(String alphabet, int min, int max) {
        return Arbitraries.strings().withChars(alphabet.toCharArray()).ofMinLength(min).ofMaxLength(max);
    }

    /** Text of {@code alphabet} characters and well-formed percent escapes with hex digits of either case. */
    private static Arbitrary<String> pctText(String alphabet, int min, int max) {
        Arbitrary<String> unit = Arbitraries.frequencyOf(weighted(4, chars(alphabet, 1, 1)),
                                                         weighted(1, chars(HEX, 2, 2).map(h -> "%" + h)));
        return unit.list().ofMinSize(min).ofMaxSize(max).map(l -> String.join("", l));
    }

    /** Decoded text: printable ASCII (delimiters and {@code %} included) and multi-byte characters. */
    private static Arbitrary<String> decodedText(int min, int max) {
        Arbitrary<Integer> codePoint = Arbitraries.frequencyOf(weighted(8, Arbitraries.integers().between(0x20,
                                                                                                                0x7E)),
                                                               weighted(2, Arbitraries.of(DECODED_EXTRA)));
        return codePoint.list().ofMinSize(min).ofMaxSize(max).map(l -> {
            StringBuilder out = new StringBuilder();
            l.forEach(out::appendCodePoint);
            return out.toString();
        });
    }

    /** {@code base} with one of {@code inserted} at a random position. */
    private static Arbitrary<String> insert(Arbitrary<String> base, Arbitrary<String> inserted) {
        return Combinators.combine(base, inserted, Arbitraries.integers().between(0, 100))
                          .as((b, s, at) -> b.substring(0, at % (b.length() + 1)) + s
                                            + b.substring(at % (b.length() + 1)));
    }

    /** A non-empty path with one of {@code inserted} after its leading {@code /}. */
    private static Arbitrary<String> insertAfterSlash(Arbitrary<String> paths, Arbitrary<String> inserted) {
        return Combinators.combine(paths.map(p -> p.isEmpty() ? "/" : p), inserted,
                                   Arbitraries.integers().between(0, 100))
                          .as((p, s, at) -> {
                              int index = 1 + at % p.length();
                              return p.substring(0, index) + s + p.substring(index);
                          });
    }

    private static String randomCase(String text, long bits) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            out.append((bits >>> (i % 64) & 1) == 0 ? Character.toLowerCase(c) : Character.toUpperCase(c));
        }
        return out.toString();
    }

    private static String flipCase(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            out.append(Character.isUpperCase(c) ? Character.toLowerCase(c) : Character.toUpperCase(c));
        }
        return out.toString();
    }

    private static String capitalize(String method) {
        return method.charAt(0) + method.substring(1).toLowerCase(Locale.ROOT);
    }

    private static boolean hasRepeatedName(List<HeaderSpec> headers) {
        return headers.stream().map(h -> h.name().toLowerCase(Locale.ROOT)).distinct().count() < headers.size();
    }

    private static String escape(String text) {
        StringBuilder out = new StringBuilder();
        for (char c : text.toCharArray()) {
            out.append(c < 0x20 || c > 0x7E ? String.format(Locale.ROOT, "\\u%04x", (int) c) : String.valueOf(c));
        }
        return out.toString();
    }

    /** One weighted alternative of {@link Arbitraries#frequencyOf}. */
    private static <T> Tuple.Tuple2<Integer, Arbitrary<? extends T>> weighted(int weight,
                                                                             Arbitrary<? extends T> arbitrary) {
        return Tuple.<Integer, Arbitrary<? extends T>>of(weight, arbitrary);
    }
}
