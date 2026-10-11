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

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.thirdparty.jackson.core.JsonParser;
import software.amazon.awssdk.thirdparty.jackson.core.JsonToken;
import software.amazon.awssdk.thirdparty.jackson.dataformat.cbor.CBORFactory;

/**
 * Compares CBOR bodies by decoded data-model value. The expected body is the base64 test case string. Both sides are
 * decoded with the shaded Jackson CBOR parser (independent of the SDK CBOR codec) into trees:
 *
 * <ul>
 *     <li>map key order is ignored, and definite- and indefinite-length containers compare equal</li>
 *     <li>integers compare by value whatever their encoded width</li>
 *     <li>half-, single- and double-precision floats compare by value, and NaN equals NaN</li>
 *     <li>an integer never equals a float, because RPCv2 CBOR distinguishes them</li>
 *     <li>tags (for example tag 1 on timestamps) are not compared; the tagged value is</li>
 * </ul>
 */
final class CborBodyComparator implements BodyComparator {
    private static final CBORFactory FACTORY = new CBORFactory();

    @Override
    public void assertMatches(String expected, byte[] actual) {
        byte[] expectedBytes;
        try {
            expectedBytes = Base64.getDecoder().decode(expected);
        } catch (IllegalArgumentException e) {
            throw new AssertionError("The expected CBOR body is not valid base64: " + expected, e);
        }
        Object expectedTree = decode("expected", expectedBytes);
        Object actualTree = decode("actual", actual);
        TreeComparison.assertTreesEqual("CBOR", expectedTree, actualTree, expected,
                                        Base64.getEncoder().encodeToString(actual));
    }

    static Object decode(String side, byte[] cbor) {
        try (JsonParser parser = FACTORY.createParser(cbor)) {
            JsonToken token = parser.nextToken();
            if (token == null) {
                throw new AssertionError("The " + side + " CBOR body contains no data item");
            }
            Object tree = read(parser, token);
            if (parser.nextToken() != null) {
                throw new AssertionError("The " + side + " CBOR body has data after its first data item");
            }
            return tree;
        } catch (IOException e) {
            throw new AssertionError("The " + side + " body is not valid CBOR: " + e.getMessage(), e);
        }
    }

    private static Object read(JsonParser parser, JsonToken token) throws IOException {
        switch (token) {
            case START_OBJECT:
                Map<String, Object> map = new LinkedHashMap<>();
                for (JsonToken t = parser.nextToken(); t != JsonToken.END_OBJECT; t = parser.nextToken()) {
                    String name = parser.currentName();
                    map.put(name, read(parser, parser.nextToken()));
                }
                return map;
            case START_ARRAY:
                List<Object> list = new ArrayList<>();
                for (JsonToken t = parser.nextToken(); t != JsonToken.END_ARRAY; t = parser.nextToken()) {
                    list.add(read(parser, t));
                }
                return list;
            case VALUE_STRING:
                return parser.getText();
            case VALUE_NUMBER_INT:
                return parser.getBigIntegerValue();
            case VALUE_NUMBER_FLOAT:
                return readFloat(parser);
            case VALUE_TRUE:
                return Boolean.TRUE;
            case VALUE_FALSE:
                return Boolean.FALSE;
            case VALUE_NULL:
                return null;
            case VALUE_EMBEDDED_OBJECT:
                return parser.getBinaryValue();
            default:
                throw new AssertionError("Unsupported CBOR token " + token);
        }
    }

    private static Object readFloat(JsonParser parser) throws IOException {
        Number number = parser.getNumberValue();
        if (number instanceof BigDecimal) {
            return number;
        }
        if (number instanceof BigInteger) {
            return new BigDecimal((BigInteger) number);
        }
        // Float widens to double exactly, so float32 and float64 encodings of one value compare equal.
        return number.doubleValue();
    }
}
