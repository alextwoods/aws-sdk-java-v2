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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UriPatternTest {
    @Test
    void parsesSegmentsAndQueryLiterals() {
        UriPattern pattern = UriPattern.parse("/{Bucket}/lit/{Key+}?x-id=GetObject&tagging&empty=");
        List<String> segments = pattern.segments().stream()
                                       .map(s -> (s.isGreedyLabel() ? "greedy:" : s.isLabel() ? "label:" : "lit:")
                                                 + s.content())
                                       .collect(Collectors.toList());
        assertThat(segments).containsExactly("label:Bucket", "lit:lit", "greedy:Key");
        assertThat(pattern.queryLiterals()).containsExactly(entry("x-id", "GetObject"), entry("tagging", null),
                                                            entry("empty", ""));
    }

    @Test
    void rootHasNoSegments() {
        assertThat(UriPattern.parse("/").segments()).isEmpty();
        assertThat(UriPattern.parse("/?a=b").segments()).isEmpty();
        assertThat(UriPattern.parse("/?a=b").queryLiterals()).containsExactly(entry("a", "b"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "foo", "/a{b}", "/{a}{b}", "/{}", "/{+}", "/{a+}/{b+}", "/x?a=1&a=2", "/{a"})
    void rejectsMalformedPatterns(String pattern) {
        assertThatThrownBy(() -> UriPattern.parse(pattern)).isInstanceOf(IllegalArgumentException.class);
    }

    private static java.util.Map.Entry<String, String> entry(String key, String value) {
        return new java.util.AbstractMap.SimpleImmutableEntry<>(key, value);
    }
}
