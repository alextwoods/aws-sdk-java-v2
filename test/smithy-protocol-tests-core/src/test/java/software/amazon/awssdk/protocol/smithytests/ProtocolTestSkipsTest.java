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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ProtocolTestSkipsTest {
    private static final String REST_JSON = "aws.protocols#restJson1";
    private static final String AWS_JSON_10 = "aws.protocols#awsJson1_0";

    private static final String SKIPS = "[\n"
                                        + "  {\"id\": \"CaseA\", \"reason\": \"not supported\"},\n"
                                        + "  {\"id\": \"CaseB\", \"reason\": \"json 1.0 only\", \"protocol\": \""
                                        + AWS_JSON_10 + "\"},\n"
                                        + "  {\"id\": \"CaseA\", \"reason\": \"rest-json specific\", \"protocol\": \""
                                        + REST_JSON + "\"}\n"
                                        + "]";

    @Test
    void parsesEntriesInOrder() {
        ProtocolTestSkips skips = ProtocolTestSkips.fromJson(SKIPS);
        assertThat(skips.entries()).containsExactly(ProtocolTestSkip.create("CaseA", "not supported", null),
                                                    ProtocolTestSkip.create("CaseB", "json 1.0 only", AWS_JSON_10),
                                                    ProtocolTestSkip.create("CaseA", "rest-json specific", REST_JSON));
    }

    @Test
    void findPrefersProtocolSpecificEntry() {
        ProtocolTestSkips skips = ProtocolTestSkips.fromJson(SKIPS);
        assertThat(skips.find("CaseA", REST_JSON).map(ProtocolTestSkip::reason)).hasValue("rest-json specific");
        assertThat(skips.find("CaseA", AWS_JSON_10).map(ProtocolTestSkip::reason)).hasValue("not supported");
        assertThat(skips.find("CaseB", AWS_JSON_10)).isPresent();
        assertThat(skips.find("CaseB", REST_JSON)).isEmpty();
        assertThat(skips.find("CaseC", REST_JSON)).isEmpty();
    }

    @Test
    void emptyArrayAndEmptyList() {
        assertThat(ProtocolTestSkips.fromJson("[]").entries()).isEmpty();
        assertThat(ProtocolTestSkips.empty().entries()).isEmpty();
    }

    @Test
    void loadsFromFileAndStream(@TempDir Path dir) throws IOException {
        Path file = dir.resolve(ProtocolTestSkips.fileName());
        Files.write(file, SKIPS.getBytes(StandardCharsets.UTF_8));
        assertThat(ProtocolTestSkips.load(file).entries()).hasSize(3);
        assertThat(ProtocolTestSkips.load(new ByteArrayInputStream(SKIPS.getBytes(StandardCharsets.UTF_8)), "s")
                                    .entries()).hasSize(3);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "{}                                                  | must contain a JSON array",
        "[1]                                                 | entry 0 must be a JSON object",
        "[{\"reason\": \"r\"}]                               | entry 0 must have a string \"id\"",
        "[{\"id\": \"A\"}]                                   | (id \"A\") must have a string \"reason\"",
        "[{\"id\": \"A\", \"reason\": \" \"}]                | has an empty \"reason\"",
        "[{\"id\": \"\", \"reason\": \"r\"}]                 | entry 0 has an empty \"id\"",
        "[{\"id\": \"A\", \"reason\": 1}]                    | must have a string \"reason\"",
        "[{\"id\": \"A\", \"reason\": \"r\", \"protocol\": \"\"}] | has an empty \"protocol\"",
        "[{\"id\": \"A\", \"reson\": \"r\"}]                 | unknown property \"reson\"",
        "[{\"id\": \"A\", \"reason\": \"r\"}, {\"id\": \"A\", \"reason\": \"s\"}] | entry 1 repeats the skip for id \"A\"",
        "[                                                   | not valid JSON"
    })
    void rejectsMalformedEntries(String json, String message) {
        assertThatThrownBy(() -> ProtocolTestSkips.fromJson(json)).isInstanceOf(IllegalArgumentException.class)
                                                                   .hasMessageContaining(message);
    }

    @Test
    void validateAgainstAcceptsKnownCases() {
        Map<String, List<String>> known = new HashMap<>();
        known.put("CaseA", Arrays.asList(REST_JSON, AWS_JSON_10));
        known.put("CaseB", Collections.singletonList(AWS_JSON_10));
        assertThatCode(() -> ProtocolTestSkips.fromJson(SKIPS).validateAgainst(known)).doesNotThrowAnyException();
    }

    @Test
    void validateAgainstNamesUnknownIdsAndProtocols() {
        Map<String, List<String>> known = new HashMap<>();
        known.put("CaseA", Collections.singletonList(AWS_JSON_10));
        assertThatThrownBy(() -> ProtocolTestSkips.fromJson(SKIPS).validateAgainst(known))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("id=CaseB")
            .hasMessageContaining("id=CaseA, protocol=" + REST_JSON)
            .satisfies(e -> assertThat(e.getMessage()).doesNotContain("reason=not supported"));
    }
}
