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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Compares label percent-encoding with the legacy SDK's {@code SdkHttpUtils.urlEncode} (non-greedy labels and query
 * values) and {@code urlEncodeIgnoreSlashes} (greedy labels).
 */
class UrlEncodingTest {
    @ParameterizedTest
    @ValueSource(strings = {"plain", "a b", "a/b", "/leading", "trailing/", "a+b", "a*b", "a~b", "-._~", "%2F", "%",
                            "a?b#c", "!$&'()*+,;=:@", "[]{}|\\^`\"<>", "ü", "日本語", "😀", "tab\tnew\nline", "  ", "é/ü"})
    void matchesTheLegacyEncoders(String value) {
        assertThat(UrlEncoding.encodeUnreserved(value, false)).isEqualTo(SdkHttpUtils.urlEncode(value));
        assertThat(UrlEncoding.encodeUnreserved(value, true)).isEqualTo(SdkHttpUtils.urlEncodeIgnoreSlashes(value));
    }

    @Test
    void encodesEveryByteOutsideTheUnreservedSet() {
        for (int c = 0; c < 128; c++) {
            String s = String.valueOf((char) c);
            boolean unreserved = Character.isLetterOrDigit(c) || "-._~".indexOf(c) >= 0;
            assertThat(UrlEncoding.encodeUnreserved(s, false))
                .as("char %d", c)
                .isEqualTo(unreserved ? s : String.format("%%%02X", c));
        }
        assertThat(UrlEncoding.encodeUnreserved("/", true)).isEqualTo("/");
    }

    @Test
    void loneSurrogatesDifferFromTheLegacyEncoder() {
        // The port writes U+FFFD; URLEncoder replaces a malformed character with '?'.
        assertThat(UrlEncoding.encodeUnreserved("a\uD83Db", false)).isEqualTo("a%EF%BF%BDb");
        assertThat(SdkHttpUtils.urlEncode("a\uD83Db")).isEqualTo("a%3Fb");
        assertThat(UrlEncoding.encodeUnreserved("\uDE00", false)).isEqualTo("%EF%BF%BD");
    }

    @Test
    void emptyStringEncodesToEmpty() {
        assertThat(UrlEncoding.encodeUnreserved("", false)).isEmpty();
        assertThat(SdkHttpUtils.urlEncode("")).isEmpty();
    }
}
