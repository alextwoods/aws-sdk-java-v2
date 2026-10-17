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

package software.amazon.awssdk.httpbinding.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Ported from smithy-java {@code HttpBindingDeserializerTest#contentTypeTest}. The other smithy-java cases cover
 * {@code MemberSubsetCodec} direct-builder reads and {@code DataStream} discarding, which the SDK does not have; the
 * SDK's response cases are in {@code ResponseDeserializerTest}.
 */
class HttpBindingDeserializerTest {
    @ParameterizedTest
    @MethodSource("contentTypeMatchProvider")
    void contentTypeTest(String actual, String expected, int expectedResult) {
        assertThat(HttpBindingDeserializer.compareMediaType(actual, expected)).isEqualTo(expectedResult);
    }

    static List<Arguments> contentTypeMatchProvider() {
        return Arrays.asList(
            // Mismatches (return -1)
            Arguments.of("text/plain", "application/json", -1),
            Arguments.of("application/jsonp", "application/json", -1),
            Arguments.of("application/mson", "application/json", -1),
            // Exact matches (return 1)
            Arguments.of("application/json", "application/json", 1),
            Arguments.of("application/JSON", "application/json", 1),
            Arguments.of("application/Json", "application/json", 1),
            Arguments.of("APPLICATION/JSON", "application/json", 1),
            Arguments.of("APPLICATION/json", "application/json", 1),
            // Matches with parameters (return 1)
            Arguments.of("application/json; charset=utf-8", "application/json", 1),
            Arguments.of("application/json;charset=utf-8", "application/json", 1),
            Arguments.of("application/json ; charset=utf-8", "application/json", 1),
            Arguments.of("application/json ;", "application/json", 1),
            Arguments.of("application/json ; ", "application/json", 1),
            Arguments.of("application/json ", "application/json", 1),
            // Null cases
            Arguments.of(null, null, 1),
            Arguments.of(null, "application/json", 0),
            Arguments.of("application/json", null, 1));
    }
}
