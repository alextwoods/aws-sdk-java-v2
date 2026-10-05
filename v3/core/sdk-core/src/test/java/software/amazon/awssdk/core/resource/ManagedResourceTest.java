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

package software.amazon.awssdk.core.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class ManagedResourceTest {
    @Test
    void equalityIsByResourceIdentityAndOwnership() {
        String a = new String("r");
        String b = new String("r");
        assertEquals(ManagedResource.sdkOwned(a), ManagedResource.of(a, ResourceOwnership.SDK_OWNED));
        assertEquals(ManagedResource.sdkOwned(a).hashCode(), ManagedResource.sdkOwned(a).hashCode());
        assertNotEquals(ManagedResource.sdkOwned(a), ManagedResource.sdkOwned(b), "equal but distinct instances");
        assertNotEquals(ManagedResource.sdkOwned(a), ManagedResource.callerOwned(a));
    }

    @Test
    void onlySdkOwnedCloseablesAreClosedBySdk() {
        AutoCloseable closeable = () -> { };
        assertSame(closeable, ManagedResource.sdkOwned(closeable).closeableBySdk().orElseThrow());
        assertEquals(Optional.empty(), ManagedResource.callerOwned(closeable).closeableBySdk());
        assertEquals(Optional.empty(), ManagedResource.sdkOwned("not closeable").closeableBySdk());
        assertTrue(ResourceOwnership.SDK_OWNED.closedBySdk());
        assertFalse(ResourceOwnership.CALLER_OWNED.closedBySdk());
    }
}
