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

package software.amazon.awssdk.httpbinding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.httpbinding.TestModel.ALL_INPUT;
import static software.amazon.awssdk.httpbinding.TestModel.ALL_OPERATION;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.codecs.commons.ByteBufferOutputStream;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.codecs.json.JsonCodec;
import software.amazon.awssdk.codecs.json.JsonSettings;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.traits.HttpTrait;
import software.amazon.awssdk.utils.DateUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

class RequestSerializerTest {
    static final Codec CODEC = JsonCodec.builder().settings(JsonSettings.REST_JSON_1).build();
    static final URI ENDPOINT = URI.create("https://example.com/base");
    static final Instant WHEN = Instant.parse("2014-04-29T18:30:38.123Z");

    @Test
    void writesEveryBindingLocation() throws IOException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("count", "dropped: @httpQuery wins");
        params.put("x-id", "dropped: the query literal wins");
        params.put("extra", "e");
        SerializableStruct input = SchemaStruct.builder(ALL_INPUT)
                                               .set("bucket", "my bucket")
                                               .set("key", "a/b c/d")
                                               .set("count", 5)
                                               .set("tags", Arrays.asList("x", "y z"))
                                               .set("since", WHEN)
                                               .set("params", params)
                                               .set("token", "tok")
                                               .set("when", WHEN)
                                               .set("items", Arrays.asList("a", "b,c", "\"q\""))
                                               .set("meta", Collections.singletonMap("One", "1"))
                                               .set("name", "n")
                                               .set("size", 3L)
                                               .build();

        SdkHttpFullRequest request = serialize(ALL_OPERATION, input);

        assertThat(request.method()).isEqualTo(SdkHttpMethod.PUT);
        assertThat(request.protocol()).isEqualTo("https");
        assertThat(request.host()).isEqualTo("example.com");
        assertThat(request.encodedPath()).isEqualTo("/base/my%20bucket/a/b%20c/d");
        assertThat(request.rawQueryParameters()).containsExactly(
            entry("x-id", Collections.singletonList("Put")),
            entry("flag", Collections.singletonList(null)),
            entry("count", Collections.singletonList("5")),
            entry("tag", Arrays.asList("x", "y z")),
            entry("since", Collections.singletonList("2014-04-29T18:30:38.123Z")),
            entry("extra", Collections.singletonList("e")));
        assertThat(request.getUri().getRawQuery())
            .isEqualTo("x-id=Put&flag&count=5&tag=x&tag=y%20z&since=2014-04-29T18%3A30%3A38.123Z&extra=e");
        assertThat(request.headers()).containsOnlyKeys("X-Token", "X-When", "X-Items", "X-Meta-One", "Content-Type",
                                                       "Content-Length");
        assertThat(request.firstMatchingHeader("x-token")).hasValue("tok");
        assertThat(request.firstMatchingHeader("X-When")).hasValue("Tue, 29 Apr 2014 18:30:38 GMT");
        assertThat(request.matchingHeaders("X-Items")).containsExactly("a, \"b,c\", \"\\\"q\\\"\"");
        assertThat(request.firstMatchingHeader("X-Meta-One")).hasValue("1");
        assertThat(request.firstMatchingHeader("Content-Type")).hasValue("application/json");
        String body = body(request);
        assertThat(body).isEqualTo("{\"name\":\"n\",\"size\":3}");
        assertThat(request.firstMatchingHeader("Content-Length")).hasValue(Integer.toString(body.length()));
    }

    @Test
    void nullMembersAreLeftOutButBodyMembersStillWriteAnEmptyObject() throws IOException {
        // RestJsonDoesntSerializeNullStructureValues: an input with body members always has a body.
        SdkHttpFullRequest request = serialize(ALL_OPERATION, SchemaStruct.builder(ALL_INPUT)
                                                                          .set("bucket", "b")
                                                                          .set("key", "k")
                                                                          .build());
        assertThat(request.encodedPath()).isEqualTo("/base/b/k");
        assertThat(request.rawQueryParameters()).containsOnlyKeys("x-id", "flag");
        assertThat(request.headers()).containsOnlyKeys("Content-Type", "Content-Length");
        assertThat(body(request)).isEqualTo("{}");
    }

    @Test
    void emptyInputWritesTheCodecEmptyStructureUnlessOmitted() throws IOException {
        SerializableStruct input = SchemaStruct.builder(TestModel.EMPTY_INPUT).build();
        SdkHttpFullRequest request = serialize(TestModel.EMPTY_OPERATION, input);
        assertThat(body(request)).isEqualTo("{}");
        assertThat(request.firstMatchingHeader("Content-Type")).hasValue("application/json");

        // RestJsonEmptyInputAndEmptyOutput: restJson1 omits the body.
        SdkHttpFullRequest omitted = HttpBinding.create().requestSerializer()
                                                .operation(TestModel.EMPTY_OPERATION)
                                                .payloadCodec(CODEC)
                                                .payloadMediaType("application/json")
                                                .endpoint(URI.create("https://example.com"))
                                                .shapeValue(input)
                                                .omitEmptyPayload(true)
                                                .serializeRequest();
        assertThat(omitted.contentStreamProvider()).isEmpty();
        assertThat(omitted.headers()).isEmpty();
        assertThat(omitted.encodedPath()).isEqualTo("/");
    }

    @Test
    void contentTypeHeaderMemberReplacesTheMediaType() throws IOException {
        SdkHttpFullRequest request = serialize(TestModel.HEADER_ONLY_OPERATION,
                                               SchemaStruct.builder(TestModel.HEADER_ONLY)
                                                           .set("contentType", "application/x-custom")
                                                           .build());
        assertThat(request.matchingHeaders("Content-Type")).containsExactly("application/x-custom");
        assertThat(body(request)).isEqualTo("{}");
    }

    @Test
    void httpQueryWinsOverQueryParamsRegardlessOfMemberOrder() {
        Map<String, List<String>> params = new LinkedHashMap<>();
        params.put("q", Collections.singletonList("from-map"));
        params.put("lit", Collections.singletonList("from-map"));
        params.put("multi", Arrays.asList("a", "b"));
        SdkHttpFullRequest request = serialize(TestModel.QUERY_OPERATION,
                                               SchemaStruct.builder(TestModel.QUERY_INPUT)
                                                           .set("params", params)
                                                           .set("value", "v")
                                                           .build());
        assertThat(request.rawQueryParameters()).containsExactly(
            entry("lit", Collections.singletonList("1")),
            entry("q", Collections.singletonList("v")),
            entry("multi", Arrays.asList("a", "b")));

        // A null @httpQuery member does not block the map entry.
        SdkHttpFullRequest withoutQuery = serialize(TestModel.QUERY_OPERATION,
                                                    SchemaStruct.builder(TestModel.QUERY_INPUT)
                                                                .set("params", params)
                                                                .build());
        assertThat(withoutQuery.rawQueryParameters().get("q")).containsExactly("from-map");
    }

    @Test
    void prefixHeadersDoNotOverrideHttpHeaderMembers() {
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("A", "1");
        meta.put("B", "2");
        SchemaStruct input = SchemaStruct.builder(ALL_INPUT)
                                         .set("bucket", "b")
                                         .set("key", "k")
                                         .set("meta", meta)
                                         .build();
        SdkHttpFullRequest request = serialize(ALL_OPERATION, input);
        assertThat(request.firstMatchingHeader("X-Meta-A")).hasValue("1");
        assertThat(request.firstMatchingHeader("x-meta-b")).hasValue("2");
    }

    @ParameterizedTest
    @ValueSource(strings = {"a b", "a/b", "/leading", "a+b", "a*b", "a~b", "ü", "日本語", "😀", "%2F", "!$&'()*+,;=:@"})
    void labelsMatchTheLegacyEncoders(String value) {
        SdkHttpFullRequest request = serialize(TestModel.LABEL_OPERATION,
                                               SchemaStruct.builder(TestModel.LABEL_INPUT)
                                                           .set("plain", value)
                                                           .set("greedy", value)
                                                           .build());
        // Legacy restXml (PathMarshaller.GREEDY_WITH_SLASHES) keeps a leading '/' of a greedy label; legacy restJson1
        // (PathMarshaller.GREEDY) trims it. By default the port keeps it, as smithy-java does; restJson1 sets
        // trimGreedyLabelLeadingSlash.
        assertThat(request.encodedPath()).isEqualTo("/base/a/" + SdkHttpUtils.urlEncode(value) + "/b/"
                                                     + SdkHttpUtils.urlEncodeIgnoreSlashes(value));
    }

    @Test
    void greedyLabelKeepsALeadingSlash() {
        SdkHttpFullRequest request = serialize(TestModel.LABEL_OPERATION,
                                               SchemaStruct.builder(TestModel.LABEL_INPUT)
                                                           .set("plain", "p")
                                                           .set("greedy", "/x/y")
                                                           .build());
        assertThat(request.encodedPath()).isEqualTo("/base/a/p/b//x/y");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/x/y", "x/y", "//x", "/", "/a b"})
    void trimGreedyLabelLeadingSlash_whenEnabled_matchesTheLegacyRestJsonGreedyMarshaller(String value) {
        SdkHttpFullRequest request = HttpBinding.create().requestSerializer()
                                                .operation(TestModel.LABEL_OPERATION)
                                                .payloadCodec(CODEC)
                                                .payloadMediaType("application/json")
                                                .endpoint(ENDPOINT)
                                                .trimGreedyLabelLeadingSlash(true)
                                                .shapeValue(SchemaStruct.builder(TestModel.LABEL_INPUT)
                                                                        .set("plain", "/p")
                                                                        .set("greedy", value)
                                                                        .build())
                                                .serializeRequest();
        // Legacy restJson1 PathMarshaller.GREEDY: urlEncodeIgnoreSlashes(trimLeadingSlash(value)). Non-greedy labels are
        // unchanged.
        String trimmed = value.startsWith("/") ? value.substring(1) : value;
        assertThat(request.encodedPath()).isEqualTo("/base/a/%2Fp/b/" + SdkHttpUtils.urlEncodeIgnoreSlashes(trimmed));
    }

    @Test
    void nullOrEmptyLabelFailsNamingTheMember() {
        assertThatThrownBy(() -> serialize(TestModel.LABEL_OPERATION,
                                           SchemaStruct.builder(TestModel.LABEL_INPUT).set("plain", "p").build()))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("smithy.example#LabelInput$greedy");
        assertThatThrownBy(() -> serialize(TestModel.LABEL_OPERATION,
                                           SchemaStruct.builder(TestModel.LABEL_INPUT)
                                                       .set("plain", "")
                                                       .set("greedy", "g")
                                                       .build()))
            .isInstanceOf(SerializationException.class)
            .hasMessageContaining("smithy.example#LabelInput$plain")
            .hasMessageContaining("empty");
    }

    @Test
    void queryAndHeaderValuesMatchTheLegacyEncodingAndDates() {
        String tricky = "a b+c*d~e/f&g=h ü 😀";
        SdkHttpFullRequest request = serialize(ALL_OPERATION, SchemaStruct.builder(ALL_INPUT)
                                                                          .set("bucket", "b")
                                                                          .set("key", "k")
                                                                          .set("tags", Collections.singletonList(tricky))
                                                                          .set("since", WHEN)
                                                                          .set("when", WHEN)
                                                                          .build());
        assertThat(request.getUri().getRawQuery())
            .contains("tag=" + SdkHttpUtils.urlEncode(tricky))
            .contains("since=" + SdkHttpUtils.urlEncode(DateUtils.formatIso8601Date(WHEN)));
        assertThat(request.firstMatchingHeader("X-When")).hasValue(DateUtils.formatRfc822Date(WHEN));
    }

    @Test
    void emptyStringsAreWritten() {
        SdkHttpFullRequest request = serialize(ALL_OPERATION, SchemaStruct.builder(ALL_INPUT)
                                                                          .set("bucket", "b")
                                                                          .set("key", "k")
                                                                          .set("token", "")
                                                                          .set("tags", Arrays.asList("", "x"))
                                                                          .set("items", Arrays.asList("", "x"))
                                                                          .build());
        // firstMatchingHeader reports an empty value as absent.
        assertThat(request.matchingHeaders("X-Token")).containsExactly("");
        assertThat(request.rawQueryParameters().get("tag")).containsExactly("", "x");
        assertThat(request.getUri().getRawQuery()).contains("tag=&tag=x");
        // Blank list header elements are left out, as the legacy HeaderMarshaller.LIST does.
        assertThat(request.firstMatchingHeader("X-Items")).hasValue("x");
    }

    @Test
    void headerListLeavesOutNullAndBlankElementsLikeTheLegacyMarshaller() {
        SdkHttpFullRequest request = serialize(ALL_OPERATION, SchemaStruct.builder(ALL_INPUT)
                                                                          .set("bucket", "b")
                                                                          .set("key", "k")
                                                                          .set("items", Arrays.asList("a", null, "",
                                                                                                      "  ", "b"))
                                                                          .build());
        // SdkRestJsonHeaderListSkipsNullAndEmptyElements
        assertThat(request.matchingHeaders("X-Items")).containsExactly("a, b");

        SdkHttpFullRequest allLeftOut = serialize(ALL_OPERATION, SchemaStruct.builder(ALL_INPUT)
                                                                             .set("bucket", "b")
                                                                             .set("key", "k")
                                                                             .set("items", Arrays.asList(null, ""))
                                                                             .build());
        // SdkRestJsonHeaderListWithOnlyNullIsOmitted
        assertThat(allLeftOut.headers()).doesNotContainKey("X-Items");
    }

    @Test
    void emptyListsWriteAnEmptyHeaderAndNoParameter() {
        SdkHttpFullRequest request = serialize(ALL_OPERATION, SchemaStruct.builder(ALL_INPUT)
                                                                          .set("bucket", "b")
                                                                          .set("key", "k")
                                                                          .set("tags", Collections.emptyList())
                                                                          .set("items", Collections.emptyList())
                                                                          .build());
        assertThat(request.rawQueryParameters()).doesNotContainKey("tag");
        // RestJsonNullAndEmptyHeaders: an empty list is sent as an empty header value.
        assertThat(request.matchingHeaders("X-Items")).containsExactly("");
    }

    @Test
    void bodyIsWrittenIntoTheGivenBuffer() throws IOException {
        ByteBufferOutputStream buffer = new ByteBufferOutputStream(8);
        SdkHttpFullRequest request = HttpBinding.create().requestSerializer()
                                                .operation(ALL_OPERATION)
                                                .payloadCodec(CODEC)
                                                .payloadMediaType("application/json")
                                                .endpoint(ENDPOINT)
                                                .bodyBuffer(buffer)
                                                .shapeValue(SchemaStruct.builder(ALL_INPUT)
                                                                        .set("bucket", "b")
                                                                        .set("key", "k")
                                                                        .set("name", "n")
                                                                        .build())
                                                .serializeRequest();
        assertThat(buffer.size()).isEqualTo(12);
        assertThat(body(request)).isEqualTo("{\"name\":\"n\"}");
        assertThat(request.contentStreamProvider().get().contentAsByteBufferOrNull().array())
            .isSameAs(buffer.array());
    }

    @Test
    void unsupportedHttpMethod_failsWhenTheRequestIsSerialized() {
        Schema input = Schema.structureBuilder(TestModel.id("FooInput")).build();
        ApiOperation<SerializableStruct, SerializableStruct> foo =
            TestModel.operation("Foo", new HttpTrait("FOO", "/foo"), input, input);
        assertThatThrownBy(() -> serialize(foo, SchemaStruct.builder(input).build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported HTTP method name FOO");
    }

    @Test
    void requiresEverySetting() {
        assertThatThrownBy(() -> HttpBinding.create().requestSerializer().serializeRequest())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("shapeValue");
    }

    static SdkHttpFullRequest serialize(ApiOperation<?, ?> operation, SerializableStruct input) {
        return HttpBinding.create().requestSerializer()
                          .operation(operation)
                          .payloadCodec(CODEC)
                          .payloadMediaType("application/json")
                          .endpoint(ENDPOINT)
                          .shapeValue(input)
                          .serializeRequest();
    }

    static String body(SdkHttpFullRequest request) throws IOException {
        return IoUtils.toUtf8String(request.contentStreamProvider().get().newStream());
    }

    static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return new java.util.AbstractMap.SimpleImmutableEntry<>(key, value);
    }

    static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
