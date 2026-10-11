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

package software.amazon.awssdk.protocol.smithytests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.utils.StringInputStream;

class ProtocolAssertsTest {

    private static final CapturedRequest REQUEST = CapturedRequest.create(
        SdkHttpFullRequest.builder()
                          .method(SdkHttpMethod.POST)
                          .protocol("https")
                          .host("foo.example.com")
                          .encodedPath("/path/a%2Fb")
                          .appendRawQueryParameter("Foo", "bar baz")
                          .appendRawQueryParameter("List", "1")
                          .appendRawQueryParameter("List", "2")
                          .appendRawQueryParameter("Empty", "")
                          .putHeader("Content-Type", "application/json")
                          .appendHeader("X-List", "a")
                          .appendHeader("X-List", "b")
                          .contentStreamProvider(() -> new StringInputStream("{\"a\":1}"))
                          .build());

    private static void passes(ThrowingCallable call) {
        assertThatCode(call).doesNotThrowAnyException();
    }

    private static void fails(ThrowingCallable call, String messagePart) {
        assertThatThrownBy(call).isInstanceOf(AssertionError.class).hasMessageContaining(messagePart);
    }

    @Test
    void capturedRequestExposesRequestParts() {
        assertThat(REQUEST.method()).isEqualTo("POST");
        assertThat(REQUEST.path()).isEqualTo("/path/a%2Fb");
        assertThat(REQUEST.host()).isEqualTo("foo.example.com");
        assertThat(REQUEST.queryParameters()).containsExactly(QueryParameter.of("Foo", "bar baz"),
                                                              QueryParameter.of("List", "1"),
                                                              QueryParameter.of("List", "2"),
                                                              QueryParameter.of("Empty", ""));
        assertThat(REQUEST.header("x-list")).hasValue("a, b");
        assertThat(new String(REQUEST.body(), StandardCharsets.UTF_8)).isEqualTo("{\"a\":1}");
    }

    @Test
    void method() {
        passes(() -> ProtocolAsserts.method(REQUEST, "POST"));
        passes(() -> ProtocolAsserts.method(REQUEST, "post"));
        fails(() -> ProtocolAsserts.method(REQUEST, "GET"), "Expected HTTP method GET but was POST");
    }

    @Test
    void uri() {
        passes(() -> ProtocolAsserts.uri(REQUEST, "/path/a%2Fb"));
        fails(() -> ProtocolAsserts.uri(REQUEST, "/path/a/b"), "Expected request path /path/a/b");
    }

    @Test
    void emptyPathIsSlash() {
        CapturedRequest r = CapturedRequest.create(SdkHttpFullRequest.builder().method(SdkHttpMethod.GET).protocol("https")
                                                                     .host("h").build());
        passes(() -> ProtocolAsserts.uri(r, "/"));
        assertThat(r.body()).isEmpty();
    }

    @Test
    void queryParams() {
        passes(() -> ProtocolAsserts.queryParams(REQUEST, Arrays.asList("Foo=bar%20baz", "List=2", "List=1", "Empty=")));
        passes(() -> ProtocolAsserts.queryParams(REQUEST, Collections.singletonList("F%6Fo=bar%20baz")));
        fails(() -> ProtocolAsserts.queryParams(REQUEST, Collections.singletonList("Foo=bar")), "[Foo=bar]");
        fails(() -> ProtocolAsserts.queryParams(REQUEST, Arrays.asList("List=1", "List=1")), "[List=1]");
        fails(() -> ProtocolAsserts.queryParams(REQUEST, Collections.singletonList("Empty")), "[Empty]");
    }

    @Test
    void forbidAndRequireQueryParams() {
        passes(() -> ProtocolAsserts.forbidQueryParams(REQUEST, Collections.singletonList("Missing")));
        fails(() -> ProtocolAsserts.forbidQueryParams(REQUEST, Arrays.asList("Missing", "List")), "[List] to be absent");
        passes(() -> ProtocolAsserts.requireQueryParams(REQUEST, Arrays.asList("Foo", "Empty")));
        fails(() -> ProtocolAsserts.requireQueryParams(REQUEST, Collections.singletonList("Missing")),
              "[Missing] to be present");
    }

    @Test
    void resolvedHost() {
        passes(() -> ProtocolAsserts.resolvedHost(REQUEST, "foo.example.com"));
        fails(() -> ProtocolAsserts.resolvedHost(REQUEST, "example.com"), "Expected resolved host example.com");
    }

    @Test
    void headersCompareNamesCaseInsensitively() {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("content-type", "application/json");
        expected.put("X-LIST", "a, b");
        passes(() -> ProtocolAsserts.headers(REQUEST, expected));
        fails(() -> ProtocolAsserts.headers(REQUEST, Collections.singletonMap("Content-Type", "text/plain")),
              "Content-Type: expected \"text/plain\" but was \"application/json\"");
        fails(() -> ProtocolAsserts.headers(REQUEST, Collections.singletonMap("X-Missing", "v")), "header is absent");
    }

    @Test
    void forbidAndRequireHeaders() {
        passes(() -> ProtocolAsserts.forbidHeaders(REQUEST, Collections.singletonList("X-Missing")));
        fails(() -> ProtocolAsserts.forbidHeaders(REQUEST, Collections.singletonList("content-TYPE")), "to be absent");
        passes(() -> ProtocolAsserts.requireHeaders(REQUEST, Collections.singletonList("x-list")));
        fails(() -> ProtocolAsserts.requireHeaders(REQUEST, Collections.singletonList("X-Missing")), "to be present");
    }

    @Test
    void body() {
        passes(() -> ProtocolAsserts.body(REQUEST, "{ \"a\": 1.0 }", "application/json"));
        fails(() -> ProtocolAsserts.body(REQUEST, "{\"a\":2}", "application/json"), "$.a");
        fails(() -> ProtocolAsserts.body(REQUEST, "", "application/json"), "Expected an empty body");
    }

    @Nested
    class OutputEquals {
        private TestShape full() {
            TestShape s = new TestShape();
            s.name = "n";
            s.data = SdkBytes.fromUtf8String("blob");
            s.number = Double.NaN;
            s.items = Arrays.asList("a", "b");
            Map<String, String> attrs = new HashMap<>();
            attrs.put("k", "v");
            s.attrs = attrs;
            s.nested = new TestShape();
            s.nested.name = "inner";
            s.doc = Document.mapBuilder().putNumber("n", 1).putString("s", "x").build();
            s.when = Instant.ofEpochMilli(1_500_000_000_123L);
            s.amount = new BigDecimal("1.5");
            return s;
        }

        @Test
        void equalShapesPass() {
            TestShape actual = full();
            actual.data = SdkBytes.fromByteArray("blob".getBytes(StandardCharsets.UTF_8));
            actual.amount = new BigDecimal("1.50");
            actual.doc = Document.mapBuilder().putString("s", "x").putNumber("n", SdkNumber.fromString("1.0")).build();
            passes(() -> ProtocolAsserts.outputEquals(full(), actual));
        }

        @Test
        void nanEqualsNan() {
            passes(() -> ProtocolAsserts.outputEquals(Double.NaN, Double.NaN));
            passes(() -> ProtocolAsserts.outputEquals(Float.NaN, Float.NaN));
        }

        @Test
        void blobsCompareByBytesAcrossRepresentations() {
            byte[] bytes = {1, 2, 3};
            passes(() -> ProtocolAsserts.outputEquals(SdkBytes.fromByteArray(bytes), ByteBuffer.wrap(bytes)));
            fails(() -> ProtocolAsserts.outputEquals(SdkBytes.fromByteArray(bytes), new byte[] {1, 2}), "$: expected blob");
        }

        @Test
        void reportsNestedMemberPath() {
            TestShape actual = full();
            actual.nested.name = "other";
            fails(() -> ProtocolAsserts.outputEquals(full(), actual), "$.Nested.Name: expected inner but was other");
        }

        @Test
        void reportsListElementAndMapEntry() {
            TestShape actual = full();
            actual.items = Arrays.asList("a", "c");
            actual.attrs = Collections.singletonMap("other", "v");
            fails(() -> ProtocolAsserts.outputEquals(full(), actual), "$.Items[1]: expected b but was c");
            fails(() -> ProtocolAsserts.outputEquals(full(), actual), "$.Attrs[k]: missing");
        }

        @Test
        void timestampsCompareByInstant() {
            TestShape actual = full();
            actual.when = Instant.ofEpochMilli(1_500_000_000_124L);
            fails(() -> ProtocolAsserts.outputEquals(full(), actual), "$.When");
        }

        @Test
        void documentNumbersCompareByValueButStructureMatters() {
            TestShape actual = full();
            actual.doc = Document.mapBuilder().putNumber("n", 2).putString("s", "x").build();
            fails(() -> ProtocolAsserts.outputEquals(full(), actual), "$.Doc[n]");
        }

        @Test
        void unsetMemberDiffersFromSetMember() {
            TestShape actual = full();
            actual.nested = null;
            fails(() -> ProtocolAsserts.outputEquals(full(), actual), "$.Nested: expected TestShape(Name=inner) but was null");
        }

        @Test
        void differentTypesFail() {
            fails(() -> ProtocolAsserts.outputEquals(1, 1L), "expected type java.lang.Integer but was java.lang.Long");
        }
    }
}
