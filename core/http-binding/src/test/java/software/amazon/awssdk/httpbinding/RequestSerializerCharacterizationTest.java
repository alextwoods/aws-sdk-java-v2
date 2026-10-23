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
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static software.amazon.awssdk.httpbinding.RequestSerializerTest.CODEC;
import static software.amazon.awssdk.httpbinding.RequestSerializerTest.WHEN;
import static software.amazon.awssdk.httpbinding.RequestSerializerTest.body;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.MapSerializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.traits.HttpHeaderTrait;
import software.amazon.awssdk.schema.traits.HttpPayloadTrait;
import software.amazon.awssdk.schema.traits.HttpPrefixHeadersTrait;
import software.amazon.awssdk.schema.traits.HttpQueryParamsTrait;
import software.amazon.awssdk.schema.traits.HttpQueryTrait;
import software.amazon.awssdk.schema.traits.HttpTrait;
import software.amazon.awssdk.schema.traits.MediaTypeTrait;

/**
 * Pins the exact request {@link RequestSerializer} builds: header entries in order with their key casing, raw query
 * entries in order (null values and duplicates included), path, protocol, host, port and body, for every way the
 * HTTP bindings write headers and query parameters and for each kind of endpoint.
 */
class RequestSerializerCharacterizationTest {
    private static final Schema JSON_TEXT = Schema.createString(TestModel.id("JsonText"),
                                                                new MediaTypeTrait("application/json"));
    private static final Schema HEADERS =
        Schema.structureBuilder(TestModel.id("CharHeaders"))
              .putMember("str", PreludeSchemas.STRING, new HttpHeaderTrait("X-Str"))
              .putMember("num", PreludeSchemas.INTEGER, new HttpHeaderTrait("X-Num"))
              .putMember("flag", PreludeSchemas.BOOLEAN, new HttpHeaderTrait("x-flag"))
              .putMember("when", PreludeSchemas.TIMESTAMP, new HttpHeaderTrait("X-When"))
              .putMember("json", JSON_TEXT, new HttpHeaderTrait("X-Json"))
              .putMember("list", TestModel.STRING_LIST, new HttpHeaderTrait("X-List"))
              .putMember("prefix", TestModel.STRING_MAP, new HttpPrefixHeadersTrait("X-"))
              .putMember("body", PreludeSchemas.STRING)
              .build();
    private static final ApiOperation<SerializableStruct, SerializableStruct> HEADERS_OPERATION =
        TestModel.operation("CharHeaders", new HttpTrait("POST", "/h"), HEADERS, HEADERS);

    private static final Schema ALL_PREFIX =
        Schema.structureBuilder(TestModel.id("CharAllPrefix"))
              .putMember("data", PreludeSchemas.BLOB, HttpPayloadTrait.INSTANCE)
              .putMember("all", TestModel.STRING_MAP, new HttpPrefixHeadersTrait(""))
              .build();
    private static final ApiOperation<SerializableStruct, SerializableStruct> ALL_PREFIX_OPERATION =
        TestModel.operation("CharAllPrefix", new HttpTrait("PUT", "/p"), ALL_PREFIX, ALL_PREFIX);

    private static final Schema LOWERCASE_CONTENT_TYPE =
        Schema.structureBuilder(TestModel.id("CharLowercaseContentType"))
              .putMember("data", PreludeSchemas.BLOB, HttpPayloadTrait.INSTANCE)
              .putMember("type", PreludeSchemas.STRING, new HttpHeaderTrait("content-type"))
              .build();
    private static final ApiOperation<SerializableStruct, SerializableStruct> LOWERCASE_CONTENT_TYPE_OPERATION =
        TestModel.operation("CharLowercaseContentType", new HttpTrait("PUT", "/c"), LOWERCASE_CONTENT_TYPE,
                            LOWERCASE_CONTENT_TYPE);

    private static final Schema QUERY =
        Schema.structureBuilder(TestModel.id("CharQuery"))
              .putMember("q", PreludeSchemas.STRING, new HttpQueryTrait("q"))
              .putMember("qs", TestModel.STRING_LIST, new HttpQueryTrait("qs"))
              .putMember("params", TestModel.STRING_LIST_MAP, HttpQueryParamsTrait.INSTANCE)
              .build();
    private static final ApiOperation<SerializableStruct, SerializableStruct> QUERY_OPERATION =
        TestModel.operation("CharQuery", new HttpTrait("GET", "/q?lit=1&bare"), QUERY, QUERY);
    private static final ApiOperation<SerializableStruct, SerializableStruct> QUERY_NO_LITERALS_OPERATION =
        TestModel.operation("CharQueryNoLiterals", new HttpTrait("GET", "/q"), QUERY, QUERY);
    private static final List<String> QUERY_INPUT_ENTRIES = Arrays.asList(
        "lit=['1']", "bare=[null]", "q=['v']", "qs=['1', '2', ' ']", "multi=['a', 'b']", "extra=['e']",
        "Q=['upper']");

    private static final Schema PREFIX_ONLY =
        Schema.structureBuilder(TestModel.id("CharPrefixOnly"))
              .putMember("meta", TestModel.STRING_MAP, new HttpPrefixHeadersTrait("X-Meta-"))
              .build();
    private static final ApiOperation<SerializableStruct, SerializableStruct> PREFIX_ONLY_OPERATION =
        TestModel.operation("CharPrefixOnly", new HttpTrait("POST", "/m"), PREFIX_ONLY, PREFIX_ONLY);
    // Non-capturing, so writing a map entry allocates nothing in the test code.
    private static final BiConsumer<String[], ShapeSerializer> WRITE_VALUE =
        (entry, s) -> s.writeString(TestModel.STRING_MAP.mapValueMember(), entry[1]);
    private static final BiConsumer<String[][], MapSerializer> WRITE_ENTRIES = (entries, ms) -> {
        for (String[] entry : entries) {
            ms.writeEntry(TestModel.STRING_MAP.mapKeyMember(), entry[0], entry, WRITE_VALUE);
        }
    };

    @Test
    void headerMembersOfEveryKind() throws IOException {
        Map<String, String> prefix = new LinkedHashMap<>();
        prefix.put("Abc", "1");
        prefix.put("ABC", "2");
        prefix.put("Str", "dropped: the X-Str member wins");
        prefix.put("FLAG", "dropped: the x-flag member wins");
        prefix.put("Other", "o");
        SdkHttpFullRequest request = serialize(HEADERS_OPERATION, "https://example.com",
                                               SchemaStruct.builder(HEADERS)
                                                           .set("str", "s")
                                                           .set("num", 5)
                                                           .set("flag", true)
                                                           .set("when", WHEN)
                                                           .set("json", "{}")
                                                           .set("list", Arrays.asList("a", "b,c", " ", "q\""))
                                                           .set("prefix", prefix)
                                                           .set("body", "n")
                                                           .build());

        // Case-insensitive name order; a later case variant replaces the value and keeps the first name's casing.
        assertThat(headerEntries(request)).containsExactly(
            "Content-Length=['12']",
            "Content-Type=['application/json']",
            "X-Abc=['2']",
            "x-flag=['true']",
            "X-Json=['e30=']",
            "X-List=['a, \"b,c\", \"q\\\"\"']",
            "X-Num=['5']",
            "X-Other=['o']",
            "X-Str=['s']",
            "X-When=['Tue, 29 Apr 2014 18:30:38 GMT']");
        assertThat(request.rawQueryParameters()).isEmpty();
        assertThat(request.encodedPath()).isEqualTo("/h");
        assertThat(body(request)).isEqualTo("{\"body\":\"n\"}");
    }

    @Test
    void emptyHeaderListAndUnsetMembers() throws IOException {
        SdkHttpFullRequest request = serialize(HEADERS_OPERATION, "https://example.com",
                                               SchemaStruct.builder(HEADERS)
                                                           .set("list", Collections.emptyList())
                                                           .set("prefix", Collections.emptyMap())
                                                           .build());
        assertThat(headerEntries(request)).containsExactly("Content-Length=['2']",
                                                           "Content-Type=['application/json']",
                                                           "X-List=['']");
        assertThat(body(request)).isEqualTo("{}");
    }

    @Test
    void emptyPrefixWritesContentTypeAndContentLength() throws IOException {
        Map<String, String> all = new LinkedHashMap<>();
        all.put("Content-Type", "text/custom");
        all.put("content-length", "999");
        all.put("x-a", "1");
        SdkHttpFullRequest request = serialize(ALL_PREFIX_OPERATION, "https://example.com",
                                               SchemaStruct.builder(ALL_PREFIX)
                                                           .set("data", ByteBuffer.wrap(utf8("abc")))
                                                           .set("all", all)
                                                           .build());
        // The prefix header wins over the blob payload's default Content-Type; the serializer's Content-Length
        // replaces the value but keeps the key casing the prefix header wrote.
        assertThat(headerEntries(request)).containsExactly("content-length=['3']", "Content-Type=['text/custom']",
                                                           "x-a=['1']");
        assertThat(body(request)).isEqualTo("abc");
    }

    @Test
    void lowercaseContentTypeMemberWithBlobPayload() {
        SdkHttpFullRequest request = serialize(LOWERCASE_CONTENT_TYPE_OPERATION, "https://example.com",
                                               SchemaStruct.builder(LOWERCASE_CONTENT_TYPE)
                                                           .set("data", ByteBuffer.wrap(utf8("abc")))
                                                           .set("type", "application/x-ct")
                                                           .build());
        assertThat(headerEntries(request)).containsExactly("Content-Length=['3']",
                                                           "content-type=['application/x-ct']");

        SdkHttpFullRequest withoutType = serialize(LOWERCASE_CONTENT_TYPE_OPERATION, "https://example.com",
                                                   SchemaStruct.builder(LOWERCASE_CONTENT_TYPE)
                                                               .set("data", ByteBuffer.wrap(utf8("abc")))
                                                               .build());
        assertThat(headerEntries(withoutType)).containsExactly("Content-Length=['3']",
                                                               "Content-Type=['application/octet-stream']");
    }

    @Test
    void queryLiteralsMembersListsAndQueryParams() {
        SdkHttpFullRequest request = serialize(QUERY_OPERATION, "https://example.com", queryInput());
        assertThat(queryEntries(request)).containsExactlyElementsOf(QUERY_INPUT_ENTRIES);
        assertThat(request.getUri().getRawQuery())
            .isEqualTo("lit=1&bare&q=v&qs=1&qs=2&qs=%20&multi=a&multi=b&extra=e&Q=upper");
        // No body members, but the codec's empty structure is still the body unless omitEmptyPayload is set.
        assertThat(headerEntries(request)).containsExactly("Content-Length=['2']", "Content-Type=['application/json']");
    }

    @Test
    void queryParamsAloneKeepTheirOrderAndDuplicates() {
        Map<String, List<String>> params = new LinkedHashMap<>();
        params.put("b", Arrays.asList("1", "2"));
        params.put("a", Collections.singletonList(""));
        SdkHttpFullRequest request = serialize(QUERY_NO_LITERALS_OPERATION, "https://example.com",
                                               SchemaStruct.builder(QUERY).set("params", params).build());
        assertThat(queryEntries(request)).containsExactly("b=['1', '2']", "a=['']");
    }

    @Test
    void endpointWithoutPath() {
        SdkHttpFullRequest request = serialize(QUERY_OPERATION, "http://h", queryInput());
        assertThat(request.protocol()).isEqualTo("http");
        assertThat(request.host()).isEqualTo("h");
        assertThat(request.port()).isEqualTo(80);
        assertThat(request.encodedPath()).isEqualTo("/q");
    }

    @Test
    void endpointWithSlashPath() {
        SdkHttpFullRequest request = serialize(QUERY_OPERATION, "http://h/", queryInput());
        assertThat(request.encodedPath()).isEqualTo("/q");
        assertThat(request.port()).isEqualTo(80);
    }

    @Test
    void endpointWithPortAndPathPrefix() {
        SdkHttpFullRequest request = serialize(QUERY_OPERATION, "https://h:8443/prefix", queryInput());
        assertThat(request.protocol()).isEqualTo("https");
        assertThat(request.host()).isEqualTo("h");
        assertThat(request.port()).isEqualTo(8443);
        assertThat(request.encodedPath()).isEqualTo("/prefix/q");
        assertThat(request.getUri().toString()).startsWith("https://h:8443/prefix/q?lit=1&bare&q=v");
    }

    @Test
    void endpointQuery_isKeptOnlyWhenTheInputWritesNoQuery() {
        SdkHttpFullRequest withQuery = serialize(QUERY_OPERATION, "http://h/p?a=1&b", queryInput());
        assertThat(withQuery.encodedPath()).isEqualTo("/p/q");
        assertThat(queryEntries(withQuery)).containsExactlyElementsOf(QUERY_INPUT_ENTRIES);

        SdkHttpFullRequest withoutQuery = serialize(QUERY_NO_LITERALS_OPERATION, "http://h/p?a=1&b",
                                                    SchemaStruct.builder(QUERY).build());
        assertThat(withoutQuery.encodedPath()).isEqualTo("/p/q");
        assertThat(queryEntries(withoutQuery)).containsExactly("a=['1']", "b=[null]");

        SdkHttpFullRequest headersOnly = serialize(HEADERS_OPERATION, "http://h/p?a=1&b",
                                                   SchemaStruct.builder(HEADERS).set("str", "s").build());
        assertThat(queryEntries(headersOnly)).containsExactly("a=['1']", "b=[null]");
        assertThat(headerEntries(headersOnly)).containsExactly("Content-Length=['2']",
                                                               "Content-Type=['application/json']",
                                                               "X-Str=['s']");
    }

    @Test
    void endpointWithoutRawPath_failsAsBuilderUriDid() {
        assertThatThrownBy(() -> serialize(HEADERS_OPERATION, "mailto:a@b", SchemaStruct.builder(HEADERS).build()))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("baseUri");
    }

    @Test
    void requestHeaders_areWrittenOnce() {
        com.sun.management.ThreadMXBean threads = allocationCounter();
        int entries = 32;
        int rounds = 100;
        String[][] meta = new String[entries][];
        for (int i = 0; i < entries; i++) {
            meta[i] = new String[] {"k" + i, "v" + i};
        }
        String[][] none = new String[0][];
        SerializableStruct full = prefixOnly(meta);
        SerializableStruct empty = prefixOnly(none);

        long perEntry = perEntryBytes(threads, entries, rounds, () -> serialize(full), () -> serialize(empty));
        // The same header writes straight into a request builder, with the name concatenation and the lowercase name
        // the serializer builds for its static-header check.
        long calibration = perEntryBytes(threads, entries, rounds, () -> putHeaders(meta), () -> putHeaders(none));

        // The serializer also allocates one map-entry value serializer (24 B) per entry. Building the headers in a
        // map and copying them into the request builder added a TreeMap entry and its value list per header.
        assertThat(perEntry).as("per-entry bytes %s, calibration %s", perEntry, calibration)
                            .isLessThanOrEqualTo(calibration + 40);
    }

    private static SdkHttpFullRequest serialize(SerializableStruct input) {
        return serialize(PREFIX_ONLY_OPERATION, "https://example.com", input);
    }

    private static void putHeaders(String[][] entries) {
        SdkHttpFullRequest.Builder builder = SdkHttpFullRequest.builder();
        for (String[] entry : entries) {
            String name = "X-Meta-" + entry[0];
            if (name.toLowerCase(Locale.ROOT).isEmpty()) {
                throw new AssertionError();
            }
            builder.putHeader(name, entry[1]);
        }
    }

    private static long perEntryBytes(com.sun.management.ThreadMXBean threads, int entries, int rounds, Runnable full,
                                      Runnable empty) {
        for (int i = 0; i < 200; i++) {
            full.run();
            empty.run();
        }
        long threadId = Thread.currentThread().getId();
        long before = threads.getThreadAllocatedBytes(threadId);
        for (int i = 0; i < rounds; i++) {
            full.run();
        }
        long fullBytes = threads.getThreadAllocatedBytes(threadId) - before;
        before = threads.getThreadAllocatedBytes(threadId);
        for (int i = 0; i < rounds; i++) {
            empty.run();
        }
        long emptyBytes = threads.getThreadAllocatedBytes(threadId) - before;
        return (fullBytes - emptyBytes) / ((long) rounds * entries);
    }

    /**
     * A {@code PREFIX_ONLY} input that writes its map without allocating per entry itself.
     */
    private static SerializableStruct prefixOnly(String[][] entries) {
        Schema member = PREFIX_ONLY.member("meta");
        return new SerializableStruct() {
            @Override
            public Schema sdkSchema() {
                return PREFIX_ONLY;
            }

            @Override
            public void serializeMembers(ShapeSerializer serializer) {
                serializer.writeMap(member, entries, entries.length, WRITE_ENTRIES);
            }

            @Override
            public <T> T getMemberValue(Schema schema) {
                return null;
            }
        };
    }

    private static SerializableStruct queryInput() {
        Map<String, List<String>> params = new LinkedHashMap<>();
        params.put("q", Collections.singletonList("dropped: the @httpQuery member wins"));
        params.put("lit", Collections.singletonList("dropped: the query literal wins"));
        params.put("bare", Collections.singletonList("dropped: the query literal wins"));
        params.put("multi", Arrays.asList("a", "b"));
        params.put("extra", Collections.singletonList("e"));
        params.put("Q", Collections.singletonList("upper"));
        return SchemaStruct.builder(QUERY)
                           .set("q", "v")
                           .set("qs", Arrays.asList("1", "2", " "))
                           .set("params", params)
                           .build();
    }

    private static SdkHttpFullRequest serialize(ApiOperation<?, ?> operation, String endpoint,
                                                SerializableStruct input) {
        return HttpBinding.create().requestSerializer()
                          .operation(operation)
                          .payloadCodec(CODEC)
                          .payloadMediaType("application/json")
                          .endpoint(URI.create(endpoint))
                          .shapeValue(input)
                          .serializeRequest();
    }

    private static List<String> headerEntries(SdkHttpFullRequest request) {
        return entries(request.headers());
    }

    private static List<String> queryEntries(SdkHttpFullRequest request) {
        return entries(request.rawQueryParameters());
    }

    // name=[values], each value quoted so an empty value differs from an empty list.
    private static List<String> entries(Map<String, List<String>> map) {
        List<String> entries = new ArrayList<>();
        map.forEach((name, values) -> {
            List<String> quoted = new ArrayList<>();
            for (String value : values) {
                quoted.add(value == null ? "null" : "'" + value + "'");
            }
            entries.add(name + "=" + quoted);
        });
        return entries;
    }

    private static com.sun.management.ThreadMXBean allocationCounter() {
        java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        assumeTrue(bean instanceof com.sun.management.ThreadMXBean);
        com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) bean;
        assumeTrue(threads.isThreadAllocatedMemorySupported() && threads.isThreadAllocatedMemoryEnabled());
        return threads;
    }

    private static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
