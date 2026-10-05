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

package software.amazon.awssdk.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ToStringTest {

    @Test
    void rendersV2Format() {
        assertEquals("Empty()", ToString.create("Empty"));
        assertEquals("Empty()", ToString.builder("Empty").add("skipped", null).build());
        assertEquals("Thing(Name=a, Items=[1, 2], Bytes=0x01ff, Array=[x, y], Ints=[3])",
                     ToString.builder("Thing").add("Name", "a").add("Items", List.of(1, 2))
                             .add("Bytes", new byte[] {1, (byte) 0xff}).add("Array", new String[] {"x", "y"})
                             .add("Ints", new int[] {3}).build());
    }
}
