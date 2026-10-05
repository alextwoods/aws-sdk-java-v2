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

package software.amazon.awssdk.endpoints.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OperationContextPathTest {
    private static Object eval(String path, Object input) {
        return OperationContextPath.parse(path).evaluate(input);
    }

    @Test
    void theCanonicalPathsOfThePinnedModels() {
        Map<String, Object> requestItems = new LinkedHashMap<>();
        requestItems.put("TableA", List.of());
        requestItems.put("TableB", List.of());
        assertEquals(List.of("TableA", "TableB"), eval("keys(RequestItems)", Map.of("RequestItems", requestItems)));
        assertEquals("T", eval("TableCreationParameters.TableName",
                               Map.of("TableCreationParameters", Map.of("TableName", "T"))));

        List<Object> items = List.of(Map.of("Get", Map.of("TableName", "A")),
                                     Map.of("Put", Map.of("TableName", "B")),
                                     Map.of("Get", Map.of("TableName", "C"), "Update", Map.of("TableName", "D")));
        Map<String, Object> input = Map.of("TransactItems", items);
        assertEquals(List.of("A", "C"), eval("TransactItems[*].Get.TableName", input));
        assertEquals(List.of("B", "D"), eval(
            "TransactItems[*].[ConditionCheck.TableName, Put.TableName, Delete.TableName, Update.TableName][]",
            Map.of("TransactItems", items.subList(1, 3))));
    }

    @Test
    void structuresAreReadThroughTheirSchema() {
        TestStruct inner = TestStruct.of(Map.of("Name", "n"), "Name");
        TestStruct outer = TestStruct.of(Map.of("Inner", inner, "Items", List.of(inner, inner)), "Inner", "Items");
        assertEquals("n", eval("Inner.Name", outer));
        assertEquals(List.of("n", "n"), eval("Items[*].Name", outer));
        assertEquals(List.of("Inner", "Items"), eval("keys(@)", outer));
        assertNull(eval("Missing.Name", outer));
    }

    @Test
    void indexesFlattenAndMultiSelect() {
        Map<String, Object> input = Map.of("L", List.of(List.of("a", "b"), List.of("c")), "M", Map.of("x", "1"));
        assertEquals(List.of("a", "b", "c"), eval("L[]", input));
        assertEquals(List.of("c"), eval("L[-1]", input));
        assertEquals(List.of("1", List.of("c")), eval("[M.x, L[1]]", input));
        assertNull(eval("M[*]", input));
        assertNull(eval("L[5]", input));
    }

    @Test
    void unsupportedExpressionsAreRejected() {
        for (String invalid : List.of("", "a.", "a[?b]", "length(a)", "a | b", "a[1", "*.a", "a..b")) {
            assertThrows(IllegalArgumentException.class, () -> OperationContextPath.parse(invalid), invalid);
        }
    }
}
