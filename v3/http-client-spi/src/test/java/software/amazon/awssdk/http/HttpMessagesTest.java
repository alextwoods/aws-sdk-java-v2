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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Raw targets, ordered header/query entries and immutability of the shared message values (Req 12.2-12.4). */
class HttpMessagesTest {
    private static SdkHttpRequest.Builder request() {
        return SdkHttpRequest.builder().method("get").host("example.com");
    }

    @Test
    void rawQueryEntriesAreKeptExactlyAndInOrder() {
        SdkHttpRequest request = request().encodedPath("/a%2Fb/c")
                                          .addRawQueryParameter("list-type", "2")
                                          .addRawQueryParameter("prefix", "a%2Fb+c")
                                          .addRawQueryParameter("tag", "x")
                                          .addRawQueryParameter("flag", null)
                                          .addRawQueryParameter("tag", "")
                                          .addRawQueryParameter("tag", "x")
                                          .build();
        assertEquals("list-type=2&prefix=a%2Fb+c&tag=x&flag&tag=&tag=x", request.encodedQuery());
        assertEquals("/a%2Fb/c?list-type=2&prefix=a%2Fb+c&tag=x&flag&tag=&tag=x", request.rawTarget());
        assertEquals(6, request.queryParameters().size());
        assertEquals(HttpQueryParameter.raw("flag", null), request.queryParameters().get(3));
        assertEquals("tag=", request.queryParameters().get(4).render());
    }

    @Test
    void decodedQueryParametersAreEncodedExactlyOnce() {
        HttpQueryParameter parameter = HttpQueryParameter.of("a b", "c/d%");
        assertEquals("a%20b", parameter.rawName());
        assertEquals("c%2Fd%25", parameter.rawValue());
        SdkHttpRequest request = request().addQueryParameter("a b", "c/d%").build();
        SdkHttpRequest copy = request.toBuilder().build();
        assertEquals("a%20b=c%2Fd%25", copy.encodedQuery());
        assertEquals(request, copy);
    }

    @Test
    void invalidRawComponentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> HttpQueryParameter.raw("", "v"));
        assertThrows(IllegalArgumentException.class, () -> HttpQueryParameter.raw("a=b", "v"));
        assertThrows(IllegalArgumentException.class, () -> HttpQueryParameter.raw("a", "b&c"));
        assertThrows(IllegalArgumentException.class, () -> HttpQueryParameter.raw("a", "b c"));
        assertThrows(IllegalArgumentException.class, () -> HttpQueryParameter.raw("a", "%2"));
        assertThrows(IllegalArgumentException.class, () -> HttpQueryParameter.raw("a", "%zz"));
        assertThrows(IllegalArgumentException.class, () -> HttpQueryParameter.raw("a#", "b"));
        assertThrows(NullPointerException.class, () -> HttpQueryParameter.of(null, "b"));
        assertThrows(IllegalArgumentException.class, () -> request().encodedPath("/a?b"));
        assertThrows(IllegalArgumentException.class, () -> request().encodedPath("/a#b"));
        assertThrows(IllegalArgumentException.class, () -> request().encodedPath("/a b"));
        assertThrows(IllegalArgumentException.class, () -> request().encodedPath("/a\r\nX: y"));
        assertThrows(IllegalArgumentException.class, () -> request().encodedPath("/caf\u00e9"));
        assertThrows(IllegalArgumentException.class, () -> request().encodedPath("/%4"));
        assertThrows(IllegalArgumentException.class, () -> request().encodedPath("a"));
        assertEquals("/", request().encodedPath("").build().encodedPath());
        assertEquals("/a;b=c/%E2%82%AC/~!$'()*+,@:", request().encodedPath("/a;b=c/%E2%82%AC/~!$'()*+,@:").build()
                                                                .encodedPath());
    }

    @Test
    void hostsAndMethodsThatWouldChangeTheRequestLineOrHostFieldAreRejected() {
        // Regression (Property 40): the host reaches Host/:authority and the method the request line unvalidated.
        for (String host : List.of("a\r\nX-Injected: 1", "a b", "a/b", "a?b", "a#b", "user@a", "a:8080", "::1",
                                   "[::1", "[]", "[[::1]]", "[::1]x", "caf\u00e9")) {
            assertThrows(IllegalArgumentException.class, () -> request().host(host), host);
        }
        for (String method : List.of("GET /x HTTP/1.1\r\n", "G ET", "GET\t", "G/T", "G:T", "(GET)")) {
            assertThrows(IllegalArgumentException.class, () -> request().method(method), method);
        }
        assertThrows(IllegalArgumentException.class, () -> request().method(""));
        SdkHttpRequest request = request().method("m-search").host("[2001:db8::1]").port(8443).build();
        assertEquals("M-SEARCH", request.method());
        assertEquals("[2001:db8::1]:8443", request.authority());
        assertEquals("my_bucket.s3.Example-1.com", request().host("my_bucket.s3.Example-1.com").build().host());
        assertEquals("[::]", request().host("[::]").build().authority());
    }

    @Test
    void headersKeepOrderCaseAndRepeatedValues() {
        HttpHeaders headers = HttpHeaders.builder().add("X-Amz-Meta-A", "1").add("x-amz-meta-a", "2")
                                         .add("Content-Type", "text/plain").add("X-AMZ-META-A", "3").build();
        assertEquals(4, headers.size());
        assertEquals("x-amz-meta-a", headers.name(1));
        assertEquals("X-AMZ-META-A", headers.name(3));
        assertEquals(List.of("1", "2", "3"), headers.allValues("x-amz-meta-a"));
        HttpHeaders removed = headers.toBuilder().remove("X-Amz-Meta-A").build();
        assertEquals(1, removed.size());
        assertEquals(4, headers.size());
        HttpHeaders set = headers.toBuilder().set("content-type", "a/b").build();
        assertEquals("content-type", set.name(3));
        assertNotEquals(headers, set);
    }

    @Test
    void headerNamesAndValuesThatWouldChangeTheWireAreRejected() {
        HttpHeaders.Builder builder = HttpHeaders.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.add("", "v"));
        assertThrows(IllegalArgumentException.class, () -> builder.add(":path", "/"));
        assertThrows(IllegalArgumentException.class, () -> builder.add("a:b", "v"));
        assertThrows(IllegalArgumentException.class, () -> builder.add("a b", "v"));
        assertThrows(IllegalArgumentException.class, () -> builder.add("a", "v\r\nX-Injected: 1"));
        assertThrows(IllegalArgumentException.class, () -> builder.add("a", "v\u0000"));
        assertTrue(builder.add("a", "tab\tand space ok").build().contains("A"));
    }

    @Test
    void rejectedReplacementHeaderLeavesExistingFieldsInPlace() {
        // Regression (Property 40): set/putHeader removed the existing fields before rejecting the new one.
        HttpHeaders.Builder headers = HttpHeaders.builder().add("A", "1").add("B", "2");
        assertThrows(IllegalArgumentException.class, () -> headers.set("a", "x\r\nX-Injected: 1"));
        assertEquals(List.of("1"), headers.build().allValues("A"));
        SdkHttpRequest.Builder builder = request().addHeader("A", "1");
        assertThrows(IllegalArgumentException.class, () -> builder.putHeader("A", "\n"));
        assertEquals(List.of("1"), builder.build().headers().allValues("a"));
    }

    @Test
    void pseudoHeadersAreDerivedFromTheTarget() {
        SdkHttpRequest request = request().port(8443).encodedPath("/k%20ey").addQueryParameter("versionId", "1")
                                          .build();
        assertEquals(List.of(Map.entry(":method", "GET"), Map.entry(":scheme", "https"),
                             Map.entry(":authority", "example.com:8443"), Map.entry(":path", "/k%20ey?versionId=1")),
                     request.pseudoHeaders());
    }

    @Test
    void messagesAreImmutableAndBuildersCopyOnWrite() {
        SdkHttpRequest.Builder builder = request().addHeader("A", "1").addRawQueryParameter("q", "1");
        SdkHttpRequest first = builder.build();
        builder.addHeader("A", "2").addRawQueryParameter("q", "2");
        SdkHttpRequest second = builder.build();
        assertEquals(1, first.headers().size());
        assertEquals(1, first.queryParameters().size());
        assertEquals(2, second.headers().size());
        assertThrows(UnsupportedOperationException.class,
                     () -> first.queryParameters().add(HttpQueryParameter.raw("x", null)));
        SdkHttpRequest derived = first.toBuilder().putHeader("A", "3").build();
        assertEquals("1", first.headers().firstValue("a").orElseThrow());
        assertEquals("3", derived.headers().firstValue("a").orElseThrow());
    }

    @Test
    void toStringNeverRendersHeaderOrQueryValues() {
        SdkHttpRequest request = request().addHeader("Authorization", "AWS4-HMAC-SHA256 Signature=secret-sig")
                                          .addHeader("X-Amz-Security-Token", "secret-token")
                                          .addRawQueryParameter("X-Amz-Signature", "secret-query").build();
        SdkHttpResponse response = SdkHttpResponse.builder().addHeader("Set-Cookie", "secret-cookie").build();
        String text = request + " " + response + " " + request.queryParameters();
        assertTrue(text.contains("Authorization"));
        assertFalse(text.contains("secret"), text);
    }

    @Test
    void responseWithoutBodySharesStatusAndHeaders() {
        SdkHttpResponse response = SdkHttpResponse.builder().statusCode(503).addHeader("x-amzn-RequestId", "r")
                                                  .body(HttpBody.fromUtf8("payload")).build();
        SdkHttpResponse withoutBody = response.withoutBody();
        assertEquals(503, withoutBody.statusCode());
        assertSame(response.headers(), withoutBody.headers());
        assertEquals(HttpBody.Kind.EMPTY, withoutBody.body().kind());
        assertEquals("payload", response.body().asUtf8String(), "the original keeps its body");
        assertSame(withoutBody, withoutBody.withoutBody());
    }
}
