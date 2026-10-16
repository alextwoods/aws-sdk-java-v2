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

package software.amazon.awssdk.codecs.json.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codecs.json.JsonCodec;
import software.amazon.awssdk.codecs.json.JsonSettings;
import software.amazon.awssdk.schema.serde.document.Document;

/**
 * SDK tests of {@link JsonPrettyPrinter}, the indenting pass for {@code prettyPrint} (smithy-java delegates it to
 * Jackson).
 */
class JsonPrettyPrinterTest {

    private static final JsonCodec COMPACT = JsonCodec.builder().settings(JsonSettings.REST_JSON_1).build();

    @Test
    void scalarsPassThroughUnchanged() throws IOException {
        assertThat(pretty("1")).isEqualTo("1");
        assertThat(pretty("\"a:b,{c}[d]\\\"\"")).isEqualTo("\"a:b,{c}[d]\\\"\"");
        assertThat(pretty("null")).isEqualTo("null");
    }

    @Test
    void containers_followTheJacksonLayout() throws IOException {
        assertThat(pretty("{}")).isEqualTo("{ }");
        assertThat(pretty("[]")).isEqualTo("[ ]");
        assertThat(pretty("[1,[2,[]],{}]")).isEqualTo("[ 1, [ 2, [ ] ], { } ]");
        assertThat(pretty("{\"a\":{\"b\":[{\"c\":1}]},\"d\":2}"))
            .isEqualTo("{\n  \"a\" : {\n    \"b\" : [ {\n      \"c\" : 1\n    } ]\n  },\n  \"d\" : 2\n}");
    }

    @Test
    void escapedBackslashBeforeQuote_endsTheString() throws IOException {
        assertThat(pretty("{\"a\\\\\":1}")).isEqualTo("{\n  \"a\\\\\" : 1\n}");
    }

    /**
     * Pretty output reads back to the same document as compact output, and does not depend on how the compact bytes
     * are split into writes.
     */
    @Property(tries = 300)
    void prettyOutput_readsBackToTheSameDocumentForAnyChunking(@ForAll("documents") Document document,
                                                                @ForAll @IntRange(min = 1, max = 17) int chunk)
        throws IOException {
        byte[] compact = COMPACT.serializeToString(document).getBytes(StandardCharsets.UTF_8);
        String whole = pretty(compact, compact.length);
        String chunked = pretty(compact, chunk);
        assertThat(chunked).isEqualTo(whole);
        Document reread = COMPACT.createDeserializer(whole.getBytes(StandardCharsets.UTF_8)).readDocument();
        assertThat(Document.equals(reread, document)).isTrue();
    }

    @Provide
    Arbitrary<Document> documents() {
        return Arbitraries.lazyOf(
            () -> Arbitraries.strings().ofMaxLength(12).map(Document::of),
            () -> Arbitraries.integers().map(Document::of),
            () -> Arbitraries.of(true, false).map(Document::of),
            () -> documents().list().ofMaxSize(4).map(JsonPrettyPrinterTest::listDocument),
            () -> Arbitraries.maps(Arbitraries.strings().ofMaxLength(6), documents())
                             .ofMaxSize(4)
                             .map(JsonPrettyPrinterTest::mapDocument));
    }

    private static Document listDocument(List<Document> elements) {
        return Document.of(new ArrayList<>(elements));
    }

    private static Document mapDocument(Map<String, Document> members) {
        return Document.of(new LinkedHashMap<>(members));
    }

    private static String pretty(String compact) throws IOException {
        byte[] bytes = compact.getBytes(StandardCharsets.UTF_8);
        return pretty(bytes, bytes.length);
    }

    private static String pretty(byte[] compact, int chunk) throws IOException {
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        JsonPrettyPrinter printer = new JsonPrettyPrinter(sink);
        for (int i = 0; i < compact.length; i += chunk) {
            printer.write(compact, i, Math.min(chunk, compact.length - i));
        }
        printer.close();
        return new String(sink.toByteArray(), StandardCharsets.UTF_8);
    }
}
