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

import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * One AWS partition of the canonical partition table: its ID, the region-name pattern and known regions it covers,
 * and the outputs the {@code aws.partition} endpoint rules function returns for its regions.
 *
 * @param id the partition ID, for example {@code aws} or {@code aws-cn}
 * @param regionRegex the pattern that region names in the partition match
 * @param regions the known region names of the partition, including its global pseudo-region; immutable
 * @param name the {@code name} output (the partition ID)
 * @param dnsSuffix the DNS suffix of the partition's endpoints, for example {@code amazonaws.com}
 * @param dualStackDnsSuffix the DNS suffix of the partition's dual-stack endpoints, for example {@code api.aws}
 * @param supportsFips whether the partition has FIPS endpoints
 * @param supportsDualStack whether the partition has dual-stack endpoints
 * @param implicitGlobalRegion the region that the partition's global endpoints are signed for
 */
public record PartitionMetadata(String id, String regionRegex, Set<String> regions, String name, String dnsSuffix,
                                String dualStackDnsSuffix, boolean supportsFips, boolean supportsDualStack,
                                String implicitGlobalRegion) {
    public PartitionMetadata {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(regionRegex, "regionRegex");
        regions = Set.copyOf(regions);
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(dnsSuffix, "dnsSuffix");
        Objects.requireNonNull(dualStackDnsSuffix, "dualStackDnsSuffix");
        Objects.requireNonNull(implicitGlobalRegion, "implicitGlobalRegion");
    }

    /** Whether {@code region} is a known region of this partition or matches its region pattern. */
    public boolean covers(String region) {
        return regions.contains(region) || Pattern.compile(regionRegex).matcher(region).matches();
    }
}
