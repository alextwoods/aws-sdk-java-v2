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

package software.amazon.awssdk.awscodegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;

class CanonicalJsonTest {

    @Test
    void sortsKeysKeepsArrayOrderAndUsesAFixedLayout() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("zeta", List.of("b", "a"));
        value.put("alpha", Map.of("y", true, "x", 1L));
        value.put("empty", Map.of());
        value.put("none", List.of());

        assertEquals(String.join("\n",
                                 "{",
                                 "  \"alpha\": {",
                                 "    \"x\": 1,",
                                 "    \"y\": true",
                                 "  },",
                                 "  \"empty\": {},",
                                 "  \"none\": [],",
                                 "  \"zeta\": [",
                                 "    \"b\",",
                                 "    \"a\"",
                                 "  ]",
                                 "}",
                                 ""),
                     CanonicalJson.write(value));
    }

    @Test
    void outputDoesNotDependOnMapIterationOrder() {
        Map<String, Object> forward = new LinkedHashMap<>();
        Map<String, Object> backward = new LinkedHashMap<>();
        Map<String, Object> hashed = new HashMap<>();
        List<String> keys = Arrays.asList("b", "a", "B", "aa", "_", "1");
        keys.forEach(k -> forward.put(k, k));
        keys.reversed().forEach(k -> backward.put(k, k));
        keys.forEach(k -> hashed.put(k, k));

        assertEquals(CanonicalJson.write(forward), CanonicalJson.write(backward));
        assertEquals(CanonicalJson.write(forward), CanonicalJson.write(hashed));
    }

    @Test
    void escapesStringsLosslessly() {
        String text = "quote\" backslash\\ newline\n tab\t nul\u0000 unit\u001f caf\u00e9 \u2028 #$";

        String json = CanonicalJson.write(Map.of("k", text));

        assertEquals("{\n  \"k\": \"quote\\\" backslash\\\\ newline\\n tab\\t nul\\u0000 unit\\u001f caf\u00e9 "
                     + "\u2028 #$\"\n}\n", json);
        assertEquals(text, Node.parse(json).expectObjectNode().expectStringMember("k").getValue());
    }

    @Test
    void rejectsValuesThatCannotBeWrittenDeterministically() {
        assertThrows(IllegalArgumentException.class, () -> CanonicalJson.write(null));
        assertThrows(IllegalArgumentException.class, () -> CanonicalJson.write(List.of(1.5d)));
        assertThrows(IllegalArgumentException.class, () -> CanonicalJson.write(Map.of(1, "non-string key")));
        assertThrows(IllegalArgumentException.class, () -> CanonicalJson.write(Arrays.asList("a", null)));
    }
}
