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

package software.amazon.awssdk.core.waiters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The JMESPath helpers that canonical AWS waiters need beyond field access and list projections: map value
 * projections (SES {@code VerificationAttributes.*.VerificationStatus}) and {@code contains} over a projected
 * multi-select list (Auto Scaling {@code GroupInService}).
 */
class WaiterMatchersTest {

    @Test
    void valuesProjectsAMapInIterationOrderAndIsNullForNonMaps() {
        Map<String, String> statuses = new LinkedHashMap<>();
        statuses.put("b", "Success");
        statuses.put("a", "Pending");
        assertEquals(List.of("Success", "Pending"), WaiterMatchers.values(statuses));
        assertNull(WaiterMatchers.values(List.of("x")));
        assertNull(WaiterMatchers.values(null));
        assertEquals(false, WaiterMatchers.allStringEquals(WaiterMatchers.values(statuses), "Success"));
    }

    @Test
    void containsSearchesListsByJmespathEqualityAndStringsBySubstring() {
        assertEquals(true, WaiterMatchers.contains(List.of(true, false), false));
        assertEquals(false, WaiterMatchers.contains(List.of(true, true), false));
        assertEquals(true, WaiterMatchers.contains(List.of(1, 2L), 2));
        assertEquals(true, WaiterMatchers.contains("InService", "Serv"));
        assertEquals(false, WaiterMatchers.contains("InService", 1));
        assertNull(WaiterMatchers.contains(null, false));
        assertNull(WaiterMatchers.contains(Map.of(), false));
    }

    @Test
    void multiSelectKeepsNullItemsAndIsNullForANullCurrentNode() {
        assertEquals(Arrays.asList("a", null), WaiterMatchers.multiSelect("x", v -> WaiterMatchers.list("a", null)));
        assertNull(WaiterMatchers.multiSelect(null, v -> WaiterMatchers.list("a")));
    }

    /** {@code contains(groups[].[length(instances[?state=='InService']) >= min][], `false`)}. */
    @Test
    void theAutoScalingGroupInServicePathFindsAnUnderProvisionedGroup() {
        record Group(List<String> instances, int min) {
        }
        List<Group> groups = List.of(new Group(List.of("InService", "InService"), 2),
                                     new Group(List.of("InService", "Pending"), 2));
        Object projected = WaiterMatchers.flatten(WaiterMatchers.project(groups, g -> WaiterMatchers.multiSelect(
            g, group -> WaiterMatchers.list(WaiterMatchers.compare(
                WaiterMatchers.length(WaiterMatchers.filter(((Group) group).instances(),
                                                            i -> WaiterMatchers.compare(i, "==", "InService"))),
                ">=", ((Group) group).min())))));
        assertEquals(List.of(true, false), projected);
        assertEquals(true, WaiterMatchers.contains(projected, false));
    }
}
