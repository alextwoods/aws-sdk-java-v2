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

package software.amazon.awssdk.endpoints.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonTest {

    @Test
    void parsesEveryValueType() {
        Map<String, Object> value = Json.parseObject(
            " {\"s\": \"a\\\"b\\u00e9\\n\", \"t\": true, \"f\": false, \"n\": null, \"i\": 20, \"big\": 5000000000,"
            + " \"d\": 1.5e2, \"l\": [1, [], {}]} ");
        assertEquals("a\"b\u00e9\n", value.get("s"));
        assertEquals(true, value.get("t"));
        assertEquals(false, value.get("f"));
        assertNull(value.get("n"));
        assertEquals(Integer.valueOf(20), value.get("i"));
        assertEquals(Long.valueOf(5_000_000_000L), value.get("big"));
        assertEquals(150.0, value.get("d"));
        assertEquals(Arrays.asList(1, List.of(), Map.of()), value.get("l"));
        assertEquals(List.of("s", "t", "f", "n", "i", "big", "d", "l"), List.copyOf(value.keySet()));
    }

    @Test
    void rejectsInvalidDocuments() {
        for (String invalid : List.of("", "{", "{\"a\": 1,}", "[1 2]", "{\"a\": 1} x", "{\"a\": 1, \"a\": 2}",
                                      "\"\\x\"", "tru", "-", "{a: 1}", "\"a\nb\"")) {
            assertThrows(IllegalArgumentException.class, () -> Json.parse(invalid), invalid);
        }
        assertThrows(IllegalArgumentException.class, () -> Json.parseObject("[]"));
    }
}
