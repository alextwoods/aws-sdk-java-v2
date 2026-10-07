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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.override.CallOptions;
import software.amazon.awssdk.core.override.SdkCallOption;

/**
 * The V2 {@code ApiName} and {@code RequestOverrideConfiguration} API-name API (Requirement 21.6): the per-call
 * contributions a call's user agent carries.
 */
class ApiNameTest {
    private static final ApiName HLL = ApiName.builder().name("hll").version("ddb-enh").build();
    private static final ApiName METRIC = ApiName.builder().name(ApiName.SDK_METRICS).version("d").build();

    @Test
    void apiNameHasV2ValueSemanticsAndRequiresBothParts() {
        ApiName same = ApiName.builder().name("hll").version("ddb-enh").build();
        assertEquals("hll", HLL.name());
        assertEquals("ddb-enh", HLL.version());
        assertEquals(HLL, same);
        assertEquals(HLL.hashCode(), same.hashCode());
        assertNotEquals(HLL, METRIC);
        assertNotEquals(HLL, ApiName.builder().name("hll").version("other").build());
        NullPointerException noName =
            assertThrows(NullPointerException.class, () -> ApiName.builder().version("1").build());
        assertEquals("name must not be null", noName.getMessage());
        NullPointerException noVersion =
            assertThrows(NullPointerException.class, () -> ApiName.builder().name("n").build());
        assertEquals("version must not be null", noVersion.getMessage());
    }

    @Test
    void overrideKeepsAddedApiNamesInOrder() {
        SdkRequestOverrideConfiguration override = SdkRequestOverrideConfiguration.builder()
                                                                                  .addApiName(HLL)
                                                                                  .addApiName(b -> b.name("sdk-metrics")
                                                                                                    .version("d"))
                                                                                  .build();
        assertEquals(List.of(HLL, METRIC), override.apiNames());
        assertEquals(List.of(HLL, METRIC), override.toBuilder().apiNames());
        assertEquals(override, override.toBuilder().build());
        assertFalse(override.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> override.apiNames().add(HLL));
        assertEquals(List.of(), SdkRequestOverrideConfiguration.builder().build().apiNames());
        assertTrue(SdkRequestOverrideConfiguration.builder().build().isEmpty());
        assertEquals(List.of(HLL, METRIC), override.callOption(SdkCallOption.API_NAMES).orElseThrow());
    }

    @Test
    void callOptionsAppendExplicitApiNamesAfterEmbeddedOnes() {
        CallOptions client = CallOptions.of(SdkClientConfiguration.empty());
        SdkRequestOverrideConfiguration embedded = SdkRequestOverrideConfiguration.builder().addApiName(HLL).build();
        SdkRequestOverrideConfiguration explicit =
            SdkRequestOverrideConfiguration.builder().addApiName(METRIC).build();
        CallOptions call = client.withOverrides(embedded, explicit);
        assertEquals(List.of(HLL, METRIC), call.get(SdkClientOption.API_NAMES).orElseThrow());
        assertTrue(call.isOverridden(SdkClientOption.API_NAMES));
        assertFalse(client.isOverridden(SdkClientOption.API_NAMES));
        assertEquals(List.of(METRIC), client.withOverrides(null, explicit).get(SdkClientOption.API_NAMES)
                                            .orElseThrow());
    }
}
