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

package software.amazon.awssdk.regions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RegionTest {
    @Test
    void ofInternsAndReturnsConstants() {
        assertSame(Region.US_WEST_2, Region.of("us-west-2"));
        assertSame(Region.of("xx-test-1"), Region.of("xx-test-1"));
        assertEquals("us-east-1", Region.US_EAST_1.id());
        assertEquals("us-east-1", Region.US_EAST_1.toString());
    }

    @Test
    void equalityIsById() {
        assertEquals(Region.of("xx-test-2"), Region.of("xx-test-2"));
        assertEquals(Region.of("xx-test-2").hashCode(), "xx-test-2".hashCode());
        assertNotEquals(Region.US_EAST_1, Region.US_EAST_2);
        assertFalse(Region.US_EAST_1.equals("us-east-1"));
    }

    @Test
    void onlyPartitionGlobalConstantsAreGlobal() {
        assertTrue(Region.AWS_GLOBAL.isGlobalRegion());
        assertTrue(Region.of("aws-cn-global").isGlobalRegion());
        assertFalse(Region.US_WEST_2.isGlobalRegion());
        assertFalse(Region.of("xx-global").isGlobalRegion());
    }

    @Test
    void regionsListsEveryConstantOnceAndIsImmutable() {
        assertEquals(53, Region.regions().size());
        Set<String> ids = new HashSet<>();
        Region.regions().forEach(r -> assertTrue(ids.add(r.id()), r.id()));
        assertTrue(Region.regions().contains(Region.US_WEST_2));
        assertThrows(UnsupportedOperationException.class, () -> Region.regions().add(Region.of("xx-test-3")));
    }

    @Test
    void invalidIdsAreRejectedAndUnsafeCharactersEncoded() {
        assertThrows(NullPointerException.class, () -> Region.of(null));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Region.of(" "));
        assertEquals("A region ID must not be blank.", e.getMessage());
        assertEquals("us%20west%2F", Region.of("us west/").id());
        assertEquals("a~b%2Ac", Region.of("a~b*c").id());
    }
}
