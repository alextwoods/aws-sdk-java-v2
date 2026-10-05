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

package software.amazon.awssdk.eventnotifications.s3.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The JSON parser's limits and value model. Its agreement with V2's parser on valid and malformed messages is
 * covered by S3EventNotificationV2DifferentialTest.
 */
class JsonTreeTest {
    @Test
    void parse_whenValueIsValid_returnsOrderedTreeWithNumberText() throws IOException {
        Object tree = parse("{\"b\":[1.50,-0,true,null,\"\\u00e9\\n\"],\"a\":{},\"b2\":1e3}");

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("b", Arrays.asList(new JsonTree.JsonNumber("1.50"), new JsonTree.JsonNumber("-0"), Boolean.TRUE,
                                        JsonTree.NULL, "\u00e9\n"));
        expected.put("a", Map.of());
        expected.put("b2", new JsonTree.JsonNumber("1e3"));
        assertThat(tree).isEqualTo(expected);
        assertThat(List.copyOf(((Map<?, ?>) tree).keySet())).isEqualTo(List.of("b", "a", "b2"));
    }

    @Test
    void parse_whenNameIsDuplicated_keepsTheLastValueAtTheFirstPosition() throws IOException {
        Map<?, ?> tree = (Map<?, ?>) parse("{\"x\":1,\"y\":2,\"x\":3}");
        assertThat(List.copyOf(tree.keySet())).isEqualTo(List.of("x", "y"));
        assertThat(tree.get("x")).isEqualTo(new JsonTree.JsonNumber("3"));
    }

    @Test
    void parse_whenInputHasNoValue_returnsNull() throws IOException {
        assertThat(parse("")).isNull();
        assertThat(parse(" // only a comment\n /* and another */ ")).isNull();
        assertThat(parse("\uFEFF")).isNull();
    }

    @Test
    void parse_whenNestingExceedsTheLimit_failsInsteadOfOverflowingTheStack() throws IOException {
        assertThat(parse("[".repeat(JsonTree.MAX_DEPTH) + "]".repeat(JsonTree.MAX_DEPTH))).isInstanceOf(List.class);
        assertThatThrownBy(() -> parse("[".repeat(JsonTree.MAX_DEPTH + 1)))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("nesting depth");
        assertThatThrownBy(() -> parse("[".repeat(100_000))).isInstanceOf(IOException.class);
    }

    @Test
    void parse_whenNumberIsTooLong_fails() {
        assertThatThrownBy(() -> parse("1".repeat(JsonTree.MAX_NUMBER_LENGTH + 1)))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Number length");
    }

    @Test
    void parse_whenInputIsMalformed_failsWithThePosition() {
        assertThatThrownBy(() -> parse("{\n  \"a\" 1}")).isInstanceOf(IOException.class)
                                                     .hasMessageContaining("line 2, column 7");
        assertThatThrownBy(() -> parse("/* unterminated")).isInstanceOf(IOException.class);
        assertThatThrownBy(() -> parse("\"\\u12g4\"")).isInstanceOf(IOException.class);
        assertThatThrownBy(() -> JsonTree.parse(new byte[] {'"', (byte) 0xC3, '"'}))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("UTF-8");
    }

    @Test
    void describe_rendersValuesAsV2JsonNodeToString() throws IOException {
        assertThat(JsonTree.describe(parse("{\"a\":[1,\"x\\\"\"],\"b\":{}}"))).isEqualTo("{\"a\": [1, \"x\\\"\"],\"b\": {}}");
        assertThat(JsonTree.describe(JsonTree.NULL)).isEqualTo("null");
        assertThat(JsonTree.describe(Boolean.TRUE)).isEqualTo("true");
    }

    private static Object parse(String json) throws IOException {
        return JsonTree.parse(json.getBytes(StandardCharsets.UTF_8));
    }
}
