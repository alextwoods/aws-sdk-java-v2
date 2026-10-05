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

package software.amazon.awssdk.endpoints.partition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;

class PartitionsTest {
    private static final Partitions DEFAULTS = Partitions.defaults();

    @Test
    void theBundledTableIsTheCanonicalOne() {
        assertEquals("1.1", DEFAULTS.version());
        assertTrue(DEFAULTS.partitions().size() >= 5, DEFAULTS.partitions()::toString);
        assertEquals("aws", DEFAULTS.partitions().get(0).id());
        PartitionMetadata aws = DEFAULTS.partition("aws").orElseThrow();
        assertEquals("amazonaws.com", aws.dnsSuffix());
        assertEquals("api.aws", aws.dualStackDnsSuffix());
        assertTrue(aws.supportsFips());
        assertTrue(aws.supportsDualStack());
        assertEquals("us-east-1", aws.implicitGlobalRegion());
        assertTrue(aws.regions().contains("aws-global"));
    }

    @Test
    void knownRegionsMapToTheirListingPartition() {
        assertEquals("aws-cn", DEFAULTS.of(Region.CN_NORTH_1).id());
        assertEquals("amazonaws.com.cn", DEFAULTS.of(Region.CN_NORTH_1).dnsSuffix());
        assertEquals("aws-us-gov", DEFAULTS.of(Region.US_GOV_WEST_1).id());
        assertEquals("aws", DEFAULTS.of(Region.AWS_GLOBAL).id());
        assertTrue(DEFAULTS.isKnownRegion("us-west-2"));
    }

    @Test
    void unknownRegionsAreInferredFromThePatternOrFallBackToAws() {
        assertEquals("aws-cn", DEFAULTS.of(Region.of("cn-future-9")).id());
        assertFalse(DEFAULTS.isKnownRegion("cn-future-9"));
        assertEquals("aws", DEFAULTS.of(Region.of("mars-east-1")).id());
        assertTrue(DEFAULTS.partition("aws").orElseThrow().covers("eu-future-3"));
    }

    @Test
    void customTablesAreValidated() {
        Partitions custom = Partitions.fromJson("""
            {"version": "1.1", "partitions": [{"id": "p", "regionRegex": "^p-\\\\d$", "regions": {"p-1": {}},
              "outputs": {"name": "p", "dnsSuffix": "p.test", "dualStackDnsSuffix": "ds.p.test",
                          "supportsFIPS": false, "supportsDualStack": true, "implicitGlobalRegion": "p-1"}}]}
            """);
        assertEquals("p", custom.forRegion("p-2").orElseThrow().id());
        assertEquals(Optional.empty(), custom.forRegion("other"));
        assertThrows(IllegalStateException.class, () -> custom.of(Region.of("other")));
        assertThrows(IllegalArgumentException.class, () -> Partitions.fromJson("{\"partitions\": [{}]}"));
    }
}
