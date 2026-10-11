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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BodyComparatorsTest {

    private static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static void assertMatches(String expected, String mediaType, byte[] actual) {
        assertThatCode(() -> BodyComparators.assertBodyEquals(expected, mediaType, actual)).doesNotThrowAnyException();
    }

    private static void assertMismatch(String expected, String mediaType, byte[] actual, String messagePart) {
        assertThatThrownBy(() -> BodyComparators.assertBodyEquals(expected, mediaType, actual))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining(messagePart);
    }

    @ParameterizedTest
    @CsvSource({
        "application/json, JsonBodyComparator",
        "application/x-amz-json-1.0, JsonBodyComparator",
        "application/vnd.foo+json, JsonBodyComparator",
        "APPLICATION/JSON; charset=utf-8, JsonBodyComparator",
        "application/xml, XmlBodyComparator",
        "text/xml, XmlBodyComparator",
        "application/cbor, CborBodyComparator",
        "application/x-www-form-urlencoded, FormUrlEncodedBodyComparator",
        "application/octet-stream, BytesBodyComparator",
        "text/plain, BytesBodyComparator"
    })
    void selectsComparatorByMediaType(String mediaType, String comparator) {
        assertThat(BodyComparators.forMediaType(mediaType).getClass().getSimpleName()).isEqualTo(comparator);
    }

    @Test
    void nullMediaTypeComparesBytes() {
        assertThat(BodyComparators.forMediaType(null)).isInstanceOf(BytesBodyComparator.class);
    }

    @Nested
    class EmptyBodies {
        @ParameterizedTest
        @CsvSource({"application/json", "application/xml", "application/cbor", "application/x-www-form-urlencoded",
                    "application/octet-stream"})
        void emptyExpectedMatchesOnlyEmptyActual(String mediaType) {
            assertMatches("", mediaType, new byte[0]);
            assertMatches("", mediaType, null);
            assertMismatch("", mediaType, utf8("{}"), "Expected an empty body");
        }

        @Test
        void nonEmptyExpectedDoesNotMatchEmptyActual() {
            assertMismatch("{}", "application/json", new byte[0], "the actual body was empty");
        }
    }

    @Nested
    class Json {
        @Test
        void ignoresMemberOrderAndWhitespace() {
            assertMatches("{\"a\": 1, \"b\": [true, null, \"x\"]}", "application/json",
                          utf8("{\"b\":[true,null,\"x\"],\"a\":1}"));
        }

        @Test
        void comparesNumbersByValue() {
            assertMatches("{\"n\": 1.0, \"big\": 1e2}", "application/json", utf8("{\"n\":1,\"big\":100}"));
        }

        @Test
        void reportsPathOfDifference() {
            assertMismatch("{\"a\": {\"b\": [1, 2]}}", "application/json", utf8("{\"a\":{\"b\":[1,3]}}"), "$.a.b[1]");
        }

        @Test
        void listOrderMatters() {
            assertMismatch("[1, 2]", "application/json", utf8("[2,1]"), "$[0]");
        }

        @Test
        void reportsMissingAndUnexpectedMembers() {
            assertMismatch("{\"a\": 1}", "application/json", utf8("{\"b\":1}"), "$.a: missing");
            assertMismatch("{\"a\": 1}", "application/json", utf8("{\"a\":1,\"b\":1}"), "$.b: unexpected member");
        }

        @Test
        void stringIsNotNumber() {
            assertMismatch("{\"a\": \"1\"}", "application/json", utf8("{\"a\":1}"), "$.a");
        }

        @Test
        void invalidActualJsonFails() {
            assertMismatch("{}", "application/json", utf8("{"), "actual body is not valid JSON");
        }
    }

    @Nested
    class Xml {
        @Test
        void ignoresWhitespaceBetweenElements() {
            assertMatches("<Foo>\n    <Bar>baz</Bar>\n</Foo>\n", "application/xml", utf8("<Foo><Bar>baz</Bar></Foo>"));
        }

        @Test
        void ignoresAttributeOrder() {
            assertMatches("<Foo a=\"1\" b=\"2\"/>", "application/xml", utf8("<Foo b=\"2\" a=\"1\"></Foo>"));
        }

        @Test
        void detectsDifferentText() {
            assertMismatch("<Foo><Bar>baz</Bar></Foo>", "application/xml", utf8("<Foo><Bar>qux</Bar></Foo>"),
                           "not similar");
        }

        @Test
        void detectsMissingElement() {
            assertMismatch("<Foo><Bar>baz</Bar><Qux/></Foo>", "application/xml", utf8("<Foo><Bar>baz</Bar></Foo>"),
                           "not similar");
        }

        @Test
        void malformedXmlFails() {
            assertMismatch("<Foo/>", "application/xml", utf8("<Foo>"), "Unable to compare XML bodies");
        }

        @Test
        void doctypeIsRejected() {
            assertMismatch("<Foo/>", "application/xml", utf8("<!DOCTYPE Foo [<!ENTITY x \"y\">]><Foo>&x;</Foo>"),
                           "Unable to compare XML bodies");
        }
    }

    @Nested
    class Cbor {
        @Test
        void ignoresMapKeyOrderAndFloatWidth() {
            // {"a": 1.5 (float32), "b": [1, "x"]}
            byte[] expected = cbor(0xa2, 0x61, 'a', 0xfa, 0x3f, 0xc0, 0x00, 0x00,
                                   0x61, 'b', 0x82, 0x01, 0x61, 'x');
            // {"b": [1, "x"], "a": 1.5 (float64)} with an indefinite-length array
            byte[] actual = cbor(0xa2, 0x61, 'b', 0x9f, 0x01, 0x61, 'x', 0xff,
                                 0x61, 'a', 0xfb, 0x3f, 0xf8, 0, 0, 0, 0, 0, 0);
            assertMatches(base64(expected), "application/cbor", actual);
        }

        @Test
        void halfPrecisionFloatMatchesDouble() {
            byte[] half = cbor(0xf9, 0x3e, 0x00); // 1.5
            byte[] dbl = cbor(0xfb, 0x3f, 0xf8, 0, 0, 0, 0, 0, 0);
            assertMatches(base64(half), "application/cbor", dbl);
        }

        @Test
        void nanEqualsNan() {
            byte[] nanHalf = cbor(0xf9, 0x7e, 0x00);
            byte[] nanDouble = cbor(0xfb, 0x7f, 0xf8, 0, 0, 0, 0, 0, 0);
            assertMatches(base64(nanHalf), "application/cbor", nanDouble);
        }

        @Test
        void integerWidthDoesNotMatter() {
            byte[] small = cbor(0x18, 0x64);       // 100 as uint8
            byte[] wide = cbor(0x1a, 0, 0, 0, 0x64); // 100 as uint32
            assertMatches(base64(small), "application/cbor", wide);
        }

        @Test
        void integerDoesNotEqualFloat() {
            byte[] one = cbor(0x01);
            byte[] oneFloat = cbor(0xfb, 0x3f, 0xf0, 0, 0, 0, 0, 0, 0);
            assertMismatch(base64(one), "application/cbor", oneFloat, "$");
        }

        @Test
        void comparesBlobsByContent() {
            byte[] blob = cbor(0x43, 1, 2, 3);
            assertMatches(base64(blob), "application/cbor", cbor(0x43, 1, 2, 3));
            assertMismatch(base64(blob), "application/cbor", cbor(0x43, 1, 2, 4), "blob");
        }

        @Test
        void taggedTimestampComparesItsValue() {
            byte[] tagged = cbor(0xc1, 0x1a, 0x5f, 0x5e, 0x10, 0x00);
            assertMatches(base64(tagged), "application/cbor", tagged);
            assertMismatch(base64(tagged), "application/cbor", cbor(0xc1, 0x1a, 0x5f, 0x5e, 0x10, 0x01), "$");
        }

        @Test
        void invalidBase64Fails() {
            assertMismatch("not base64!", "application/cbor", cbor(0x01), "not valid base64");
        }

        @Test
        void trailingDataFails() {
            assertMismatch(base64(cbor(0x01)), "application/cbor", cbor(0x01, 0x02), "data after its first data item");
        }
    }

    @Nested
    class FormUrlEncoded {
        @Test
        void ignoresPairOrderAndLineBreaks() {
            assertMatches("Action=Foo\n&Version=2020-01-08\n&Bar=a%20b", "application/x-www-form-urlencoded",
                          utf8("Bar=a%20b&Version=2020-01-08&Action=Foo"));
        }

        @Test
        void comparesDecodedValues() {
            assertMatches("Key=a%2Ab", "application/x-www-form-urlencoded", utf8("Key=a%2ab"));
        }

        @Test
        void plusIsNotASpace() {
            assertMismatch("Key=a%20b", "application/x-www-form-urlencoded", utf8("Key=a+b"), "missing pairs: [Key=a b]");
        }

        @Test
        void countsRepeatedPairs() {
            assertMismatch("A=1&A=1", "application/x-www-form-urlencoded", utf8("A=1"), "missing pairs: [A=1]");
            assertMismatch("A=1", "application/x-www-form-urlencoded", utf8("A=1&A=1"), "unexpected pairs: [A=1]");
        }

        @Test
        void distinguishesEmptyValueFromNoValue() {
            assertMismatch("A=", "application/x-www-form-urlencoded", utf8("A"), "missing pairs");
        }
    }

    @Nested
    class Bytes {
        @Test
        void comparesUtf8Bytes() {
            assertMatches("blobby blob blob", "application/octet-stream", utf8("blobby blob blob"));
            assertMatches("caf\u00e9", null, utf8("caf\u00e9"));
        }

        @Test
        void detectsAnyDifference() {
            assertMismatch("abc", "text/plain", utf8("abd"), "differ byte for byte");
            assertMismatch("{\"a\":1}", "application/octet-stream", utf8("{ \"a\":1}"), "differ byte for byte");
        }
    }

    private static byte[] cbor(int... bytes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int b : bytes) {
            out.write(b);
        }
        return out.toByteArray();
    }

    private static String base64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
