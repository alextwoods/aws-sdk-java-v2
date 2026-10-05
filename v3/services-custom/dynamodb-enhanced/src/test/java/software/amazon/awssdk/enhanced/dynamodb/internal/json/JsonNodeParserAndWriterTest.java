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

package software.amazon.awssdk.enhanced.dynamodb.internal.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;

/**
 * The internal JSON parser and writer that replace V2's Jackson-based json-utils for {@link EnhancedDocument}: the
 * observable text and parse behaviors V2 had (exact number text, Jackson's escape set, raw non-ASCII output, last
 * duplicate wins, first value only, strict syntax).
 */
class JsonNodeParserAndWriterTest {
    private static final JsonNodeParser PARSER = JsonNodeParser.create();

    @Test
    void parsesEveryValueKindKeepingNumberTextAndMemberOrder() {
        JsonNode node = PARSER.parse(" {\"b\":1.50,\"a\":[true,false,null,\"x\",-0,1e10,2E-3],\"o\":{}} ");
        assertThat(node.asObject().keySet()).containsExactly("b", "a", "o");
        assertThat(node.asObject().get("b").asNumber()).isEqualTo("1.50");
        List<JsonNode> array = node.asObject().get("a").asArray();
        assertThat(array).extracting(JsonNode::text).containsExactly("true", "false", null, "x", "-0", "1e10", "2E-3");
        assertThat(node.asObject().get("o").asObject()).isEmpty();
        assertThat(node.toString()).isEqualTo("{\"b\":1.50,\"a\":[true,false,null,\"x\",-0,1e10,2E-3],\"o\":{}}");
    }

    @Test
    void decodesEscapesAndTheLastDuplicateMemberWins() {
        JsonNode node = PARSER.parse("{\"k\":\"old\",\"s\":\"\\\"\\\\\\/\\b\\f\\n\\r\\t\\u00e9\\ud83d\\ude00\",\"k\":\"new\"}");
        assertThat(node.asObject().get("k").asString()).isEqualTo("new");
        assertThat(node.asObject().keySet()).containsExactly("s", "k");
        assertThat(node.asObject().get("s").asString()).isEqualTo("\"\\/\b\f\n\r\t\u00e9\ud83d\ude00");
    }

    @Test
    void readsOnlyTheFirstValueAndNothingFromBlankInput() {
        assertThat(PARSER.parse("   ")).isNull();
        assertThat(PARSER.parse("[1] [2]").asArray()).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "{\"a\" 1}", "{'a':1}", "{a:1}", "[1,]", "01", "1.", "-", "NaN", "tru",
                            "\"unterminated", "\"bad \\x escape\"", "\"raw \u0001 control\"", "// comment\n{}",
                            "\"\\u12\""})
    void rejectsMalformedJsonAsAnUncheckedIoException(String json) {
        assertThatThrownBy(() -> PARSER.parse(json)).isInstanceOf(UncheckedIOException.class)
                                                    .hasMessageContaining("offset");
    }

    @Test
    void writesJacksonCompatibleEscapes() {
        String text = "q\" b\\ s/ \b\f\n\r\t \u0001\u001f \u007f \u00e9 \u2028 \ud83d\ude00";
        String written = new JsonWriter().writeValue(text).toString();
        assertThat(written).isEqualTo("\"q\\\" b\\\\ s/ \\b\\f\\n\\r\\t \\u0001\\u001F \u007f \u00e9 \u2028 "
                                      + "\ud83d\ude00\"");
        assertThat(PARSER.parse(written).asString()).isEqualTo(text);
    }

    @Test
    void writesNestedContainersWithSeparatorsAndBase64Binaries() {
        String json = new JsonWriter().writeStartObject()
                                      .writeFieldName("l").writeStartArray().writeNumber("1").writeNull()
                                      .writeStartObject().writeEndObject().writeEndArray()
                                      .writeFieldName("b").writeValue(ByteBuffer.wrap(new byte[] {1, 2, 3}))
                                      .writeFieldName("t").writeValue(true)
                                      .writeEndObject().toString();
        assertThat(json).isEqualTo("{\"l\":[1,null,{}],\"b\":\"AQID\",\"t\":true}");
    }

    @Test
    void enhancedDocumentJsonRoundTripsThroughTheseTypes() {
        String json = "{\"id\":\"1\",\"n\":[1,2.5,-3],\"m\":{\"nested\":[{\"deep\":null}]},\"flag\":false}";
        EnhancedDocument document = EnhancedDocument.fromJson(json);
        assertThat(document.toJson()).isEqualTo(json);
        assertThat(document.getJson("m")).isEqualTo("{\"nested\":[{\"deep\":null}]}");
        assertThat(PARSER.parse(document.toJson())).isEqualTo(PARSER.parse(json));
        assertThat(new ObjectJsonNode(Map.of("a", new NumberJsonNode("1")))).hasToString("{\"a\":1}");
    }
}
