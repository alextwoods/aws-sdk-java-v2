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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class QueryParameterTest {

    @Test
    void parsesNameAndValue() {
        assertThat(QueryParameter.parse("a%20b=c%3Dd")).isEqualTo(QueryParameter.of("a b", "c=d"));
        assertThat(QueryParameter.parse("a=")).isEqualTo(QueryParameter.of("a", ""));
        assertThat(QueryParameter.parse("a")).isEqualTo(QueryParameter.of("a", null));
        assertThat(QueryParameter.parse("a=b=c").value()).isEqualTo("b=c");
    }

    @Test
    void decodesUtf8AndKeepsPlus() {
        assertThat(QueryParameter.parse("k=%E2%9C%93+x").value()).isEqualTo("\u2713+x");
        assertThat(QueryParameter.parse("k=\uD83D\uDE00%20").value()).isEqualTo("\uD83D\uDE00 ");
    }

    @Test
    void parseAllSkipsEmptySegments() {
        assertThat(QueryParameter.parseAll("a=1&&b&")).containsExactly(QueryParameter.of("a", "1"),
                                                                       QueryParameter.of("b", null));
        assertThat(QueryParameter.parseAll(null)).isEmpty();
        assertThat(QueryParameter.parseAll("")).isEmpty();
    }

    @Test
    void rejectsBrokenPercentEncoding() {
        assertThatThrownBy(() -> QueryParameter.parse("a=%2")).isInstanceOf(IllegalArgumentException.class)
                                                              .hasMessageContaining("Truncated");
        assertThatThrownBy(() -> QueryParameter.parse("a=%zz")).isInstanceOf(IllegalArgumentException.class)
                                                               .hasMessageContaining("Invalid");
    }
}
