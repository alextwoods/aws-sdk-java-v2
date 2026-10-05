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

package software.amazon.awssdk.policybuilder.iam.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.policybuilder.iam.IamPolicy;
import software.amazon.awssdk.policybuilder.iam.IamPolicyReader;
import software.amazon.awssdk.policybuilder.iam.internal.PolicyJsonReader.ArrayNode;
import software.amazon.awssdk.policybuilder.iam.internal.PolicyJsonReader.BooleanNode;
import software.amazon.awssdk.policybuilder.iam.internal.PolicyJsonReader.Node;
import software.amazon.awssdk.policybuilder.iam.internal.PolicyJsonReader.NullNode;
import software.amazon.awssdk.policybuilder.iam.internal.PolicyJsonReader.NumberNode;
import software.amazon.awssdk.policybuilder.iam.internal.PolicyJsonReader.ObjectNode;
import software.amazon.awssdk.policybuilder.iam.internal.PolicyJsonReader.StringNode;

/**
 * The document semantics of the V2 policy reader (Jackson with Java comments, first root value only) that
 * {@link PolicyJsonReader} keeps.
 */
class PolicyJsonReaderTest {
    private static final String MINIMAL = "{\"Statement\":{\"Effect\":\"Allow\"}}";

    @Test
    void parse_withEveryValueKind_keepsKindsOrderAndNumberText() throws IOException {
        Node node = parse("{\"s\":\"x\",\"n\":-12.0E+05,\"t\":true,\"f\":false,\"z\":null,\"a\":[1.50,{}],\"o\":{}}");

        assertThat(node).isEqualTo(new ObjectNode(Map.of(
            "s", new StringNode("x"), "n", new NumberNode("-12.0E+05"), "t", new BooleanNode(true),
            "f", new BooleanNode(false), "z", NullNode.INSTANCE,
            "a", new ArrayNode(List.of(new NumberNode("1.50"), new ObjectNode(Map.of()))),
            "o", new ObjectNode(Map.of()))));
        assertThat(((ObjectNode) node).members().keySet()).containsExactly("s", "n", "t", "f", "z", "a", "o");
    }

    @Test
    void parse_withDuplicateMembers_keepsFirstPositionAndLastValue() throws IOException {
        ObjectNode node = (ObjectNode) parse("{\"a\":\"1\",\"b\":\"2\",\"a\":\"3\"}");

        assertThat(node.members().keySet()).containsExactly("a", "b");
        assertThat(node.members().get("a")).isEqualTo(new StringNode("3"));
    }

    @Test
    void parse_withJavaComments_ignoresThem() throws IOException {
        Node node = parse("// leading\n{ /* a */ \"a\" /* b */ : // c\n \"x\" /**/ }");

        assertThat(node).isEqualTo(new ObjectNode(Map.of("a", new StringNode("x"))));
    }

    @Test
    void parse_withContentAfterTheRootValue_ignoresIt() throws IOException {
        assertThat(parse("{\"a\":\"x\"} trailing ] garbage")).isEqualTo(new ObjectNode(Map.of("a",
                                                                                                new StringNode("x"))));
    }

    @Test
    void parse_withRootNumberFollowedByNonWhitespace_fails() {
        assertThatThrownBy(() -> parse("123x")).isInstanceOf(PolicyJsonParseException.class)
                                               .hasMessageContaining("root-level");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   \n\t", "// only a comment", "/* only a comment */"})
    void parse_withoutAValue_returnsNull(String document) throws IOException {
        assertThat(parse(document)).isNull();
    }

    @Test
    void parse_withEscapes_decodesThem() throws IOException {
        Node node = parse("\"\\\" \\\\ \\/ \\b \\f \\n \\r \\t \\u00e9 \\uD83D\\uDE00 \\uDC00\"");

        assertThat(node).isEqualTo(new StringNode("\" \\ / \b \f \n \r \t \u00e9 \uD83D\uDE00 \uDC00"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"a\":01}", "{\"a\":-}", "{\"a\":1.}", "{\"a\":1e}", "{\"a\":+1}", "{\"a\":.5}", "{\"a\":NaN}",
        "{\"a\":\"x\",}", "[1,]", "{'a':\"x\"}", "{a:\"x\"}", "{\"a\" \"x\"}", "{\"a\":\"x\" \"b\":\"y\"}",
        "{\"a\":\"\\x\"}", "{\"a\":\"\\u12G4\"}", "{\"a\":\"\t\"}", "{\"a\":truex}", "{\"a\":nul}",
        "{\"a\":\"x\"", "[", "{\"a\":\"x", "/* unterminated", "/x", "{\"a\":\"x\"]", "\u0001"})
    void parse_withMalformedJson_fails(String document) {
        assertThatThrownBy(() -> parse(document)).isInstanceOf(PolicyJsonParseException.class)
                                                 .hasMessageContaining("[line: ");
    }

    @Test
    void parse_withLeadingZero_failsWithTheV2Message() {
        assertThatThrownBy(() -> parse("[001234567890]"))
            .hasMessageContaining("Invalid numeric value: Leading zeroes not allowed");
    }

    @Test
    void parse_atTheNestingLimit_succeedsAndBeyondFails() throws IOException {
        int max = PolicyJsonReader.MAX_DEPTH;
        assertThat(parse("[".repeat(max) + "]".repeat(max))).isInstanceOf(ArrayNode.class);
        assertThatThrownBy(() -> parse("[".repeat(max + 1) + "]".repeat(max + 1)))
            .isInstanceOf(PolicyJsonParseException.class).hasMessageContaining("nesting depth");
    }

    @Test
    void parse_withATooLongNumber_fails() {
        assertThatThrownBy(() -> parse("[" + "1".repeat(PolicyJsonReader.MAX_NUMBER_LENGTH + 1) + "]"))
            .isInstanceOf(PolicyJsonParseException.class).hasMessageContaining("Number length");
    }

    @ParameterizedTest
    @ValueSource(strings = {"UTF-8", "UTF-16BE", "UTF-16LE", "UTF-32BE", "UTF-32LE"})
    void parse_withAnyJsonEncodingWithAndWithoutBom_detectsIt(String charsetName) throws IOException {
        Charset charset = Charset.forName(charsetName);
        String document = "{\"a\":\"\u00e9\u4e2d\uD83D\uDE00\"}";
        Node expected = new ObjectNode(Map.of("a", new StringNode("\u00e9\u4e2d\uD83D\uDE00")));

        assertThat(PolicyJsonReader.parse(new ByteArrayInputStream(document.getBytes(charset)))).isEqualTo(expected);
        assertThat(PolicyJsonReader.parse(new ByteArrayInputStream(("\uFEFF" + document).getBytes(charset))))
            .isEqualTo(expected);
    }

    @Test
    void parse_withMalformedUtf8_fails() {
        byte[] document = {'"', (byte) 0xC3, '"'};

        assertThatThrownBy(() -> PolicyJsonReader.parse(new ByteArrayInputStream(document)))
            .isInstanceOf(IOException.class);
    }

    @Test
    void parse_withAStream_doesNotCloseIt() throws IOException {
        TrackingStream stream = new TrackingStream(MINIMAL.getBytes(StandardCharsets.UTF_8));

        assertThat(PolicyJsonReader.parse(stream)).isInstanceOf(ObjectNode.class);
        assertThat(stream.closed).isFalse();
    }

    @Test
    void read_withMalformedJson_throwsUncheckedIoException() {
        assertThatThrownBy(() -> IamPolicyReader.create().read("{\"Statement\":"))
            .isInstanceOf(UncheckedIOException.class)
            .hasCauseInstanceOf(PolicyJsonParseException.class);
    }

    @Test
    void read_withAnEmptyDocument_throwsNullPointerExceptionAsV2() {
        assertThatThrownBy(() -> IamPolicyReader.create().read("  ")).isInstanceOf(NullPointerException.class);
    }

    @Test
    void read_withAStreamThatFails_throwsUncheckedIoException() {
        InputStream failing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("boom");
            }
        };

        assertThatThrownBy(() -> IamPolicyReader.create().read(failing)).isInstanceOf(UncheckedIOException.class)
                                                                         .hasRootCauseMessage("boom");
    }

    @Test
    void read_withAllThreeInputForms_readsTheSamePolicy() {
        IamPolicyReader reader = IamPolicyReader.create();
        IamPolicy expected = reader.read(MINIMAL);

        assertThat(reader.read(MINIMAL.getBytes(StandardCharsets.UTF_8))).isEqualTo(expected);
        assertThat(reader.read(new ByteArrayInputStream(MINIMAL.getBytes(StandardCharsets.UTF_8))))
            .isEqualTo(expected);
    }

    private static Node parse(String document) throws IOException {
        return PolicyJsonReader.parse(new ByteArrayInputStream(document.getBytes(StandardCharsets.UTF_8)));
    }

    private static final class TrackingStream extends ByteArrayInputStream {
        private boolean closed;

        private TrackingStream(byte[] bytes) {
            super(bytes);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
