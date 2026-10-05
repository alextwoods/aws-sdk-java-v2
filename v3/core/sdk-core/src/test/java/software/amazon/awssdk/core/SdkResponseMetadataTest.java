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

package software.amazon.awssdk.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class SdkResponseMetadataTest {
    @Test
    void absentFieldsAreEmptyNotNull() {
        SdkResponseMetadata empty = SdkResponseMetadata.builder().build();
        assertSame(SdkResponseMetadata.empty(), empty);
        assertEquals(Optional.empty(), empty.requestId());
        assertEquals(Optional.empty(), empty.extendedRequestId());
        assertEquals(OptionalInt.empty(), empty.statusCode());
        assertEquals(Optional.empty(), empty.errorCode());
        assertEquals(OptionalInt.empty(), empty.attemptCount());
        assertEquals(Map.of(), empty.transportMetadata());
    }

    @Test
    void suppliedFieldsRoundTripAndCompareByValue() {
        Map<String, String> transport = new LinkedHashMap<>();
        transport.put("protocol", "HTTP/2");
        transport.put("remote", "example.test");
        SdkResponseMetadata metadata = SdkResponseMetadata.builder()
                                                          .requestId("r")
                                                          .extendedRequestId("x")
                                                          .statusCode(503)
                                                          .errorCode("Throttling")
                                                          .attemptCount(3)
                                                          .transportMetadata(transport)
                                                          .build();
        transport.clear();
        assertEquals(Optional.of("r"), metadata.requestId());
        assertEquals(Optional.of("x"), metadata.extendedRequestId());
        assertEquals(OptionalInt.of(503), metadata.statusCode());
        assertEquals(Optional.of("Throttling"), metadata.errorCode());
        assertEquals(OptionalInt.of(3), metadata.attemptCount());
        assertEquals(List.of("protocol", "remote"), List.copyOf(metadata.transportMetadata().keySet()));
        assertThrows(UnsupportedOperationException.class, () -> metadata.transportMetadata().put("k", "v"));

        SdkResponseMetadata copy = metadata.toBuilder().build();
        assertEquals(metadata, copy);
        assertEquals(metadata.hashCode(), copy.hashCode());
        assertNotEquals(metadata, metadata.toBuilder().requestId(null).build());
        assertEquals(Optional.of("r"), metadata.requestId(), "built value unchanged by its builder");
    }

    @Test
    void attemptCountMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> SdkResponseMetadata.builder().attemptCount(0));
        assertThrows(NullPointerException.class, () -> SdkResponseMetadata.builder().putTransportMetadata("k", null));
    }
}
