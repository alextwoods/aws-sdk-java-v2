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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An AWS region, with the V2 name and members {@code of(String)}, {@code id()}, {@code isGlobalRegion()},
 * {@code regions()} and the V2 region constants (for example {@link #US_EAST_1} and {@link #US_WEST_2}).
 *
 * <p>A region is an identifier only: it does not imply that a service is available there, and {@link #of(String)}
 * accepts any non-blank identifier so new regions work without an SDK upgrade. Instances are interned and immutable,
 * equal when their {@link #id()}s are equal, and render as their ID.
 *
 * <p>The constants mirror the V2 region set. Partition and endpoint metadata (V2's {@code metadata()} and
 * {@code RegionMetadata}) are derived from the canonical partition table by the endpoints runtime
 * ({@code software.amazon.awssdk.endpoints.partition.Partitions#of(Region)}) and the service's endpoint rules, not by
 * this type.
 */
public final class Region {
    public static final Region AF_SOUTH_1 = Region.of("af-south-1");

    public static final Region AP_EAST_1 = Region.of("ap-east-1");

    public static final Region AP_EAST_2 = Region.of("ap-east-2");

    public static final Region AP_NORTHEAST_1 = Region.of("ap-northeast-1");

    public static final Region AP_NORTHEAST_2 = Region.of("ap-northeast-2");

    public static final Region AP_NORTHEAST_3 = Region.of("ap-northeast-3");

    public static final Region AP_SOUTHEAST_1 = Region.of("ap-southeast-1");

    public static final Region AP_SOUTHEAST_2 = Region.of("ap-southeast-2");

    public static final Region AP_SOUTHEAST_3 = Region.of("ap-southeast-3");

    public static final Region AP_SOUTHEAST_4 = Region.of("ap-southeast-4");

    public static final Region AP_SOUTHEAST_5 = Region.of("ap-southeast-5");

    public static final Region AP_SOUTHEAST_6 = Region.of("ap-southeast-6");

    public static final Region AP_SOUTHEAST_7 = Region.of("ap-southeast-7");

    public static final Region AP_SOUTH_1 = Region.of("ap-south-1");

    public static final Region AP_SOUTH_2 = Region.of("ap-south-2");

    public static final Region AWS_CN_GLOBAL = Region.of("aws-cn-global", true);

    public static final Region AWS_GLOBAL = Region.of("aws-global", true);

    public static final Region AWS_ISO_B_GLOBAL = Region.of("aws-iso-b-global", true);

    public static final Region AWS_ISO_E_GLOBAL = Region.of("aws-iso-e-global", true);

    public static final Region AWS_ISO_F_GLOBAL = Region.of("aws-iso-f-global", true);

    public static final Region AWS_ISO_GLOBAL = Region.of("aws-iso-global", true);

    public static final Region AWS_US_GOV_GLOBAL = Region.of("aws-us-gov-global", true);

    public static final Region CA_CENTRAL_1 = Region.of("ca-central-1");

    public static final Region CA_WEST_1 = Region.of("ca-west-1");

    public static final Region CN_NORTHWEST_1 = Region.of("cn-northwest-1");

    public static final Region CN_NORTH_1 = Region.of("cn-north-1");

    public static final Region EUSC_DE_EAST_1 = Region.of("eusc-de-east-1");

    public static final Region EU_CENTRAL_1 = Region.of("eu-central-1");

    public static final Region EU_CENTRAL_2 = Region.of("eu-central-2");

    public static final Region EU_ISOE_WEST_1 = Region.of("eu-isoe-west-1");

    public static final Region EU_NORTH_1 = Region.of("eu-north-1");

    public static final Region EU_SOUTH_1 = Region.of("eu-south-1");

    public static final Region EU_SOUTH_2 = Region.of("eu-south-2");

    public static final Region EU_WEST_1 = Region.of("eu-west-1");

    public static final Region EU_WEST_2 = Region.of("eu-west-2");

    public static final Region EU_WEST_3 = Region.of("eu-west-3");

    public static final Region IL_CENTRAL_1 = Region.of("il-central-1");

    public static final Region ME_CENTRAL_1 = Region.of("me-central-1");

    public static final Region ME_SOUTH_1 = Region.of("me-south-1");

    public static final Region MX_CENTRAL_1 = Region.of("mx-central-1");

    public static final Region SA_EAST_1 = Region.of("sa-east-1");

    public static final Region US_EAST_1 = Region.of("us-east-1");

    public static final Region US_EAST_2 = Region.of("us-east-2");

    public static final Region US_GOV_EAST_1 = Region.of("us-gov-east-1");

    public static final Region US_GOV_WEST_1 = Region.of("us-gov-west-1");

    public static final Region US_ISOB_EAST_1 = Region.of("us-isob-east-1");

    public static final Region US_ISOB_WEST_1 = Region.of("us-isob-west-1");

    public static final Region US_ISOF_EAST_1 = Region.of("us-isof-east-1");

    public static final Region US_ISOF_SOUTH_1 = Region.of("us-isof-south-1");

    public static final Region US_ISO_EAST_1 = Region.of("us-iso-east-1");

    public static final Region US_ISO_WEST_1 = Region.of("us-iso-west-1");

    public static final Region US_WEST_1 = Region.of("us-west-1");

    public static final Region US_WEST_2 = Region.of("us-west-2");

    private static final List<Region> REGIONS = List.of(
        AF_SOUTH_1, AP_EAST_1, AP_EAST_2, AP_NORTHEAST_1, AP_NORTHEAST_2, AP_NORTHEAST_3, AP_SOUTHEAST_1,
        AP_SOUTHEAST_2, AP_SOUTHEAST_3, AP_SOUTHEAST_4, AP_SOUTHEAST_5, AP_SOUTHEAST_6, AP_SOUTHEAST_7, AP_SOUTH_1,
        AP_SOUTH_2, AWS_CN_GLOBAL, AWS_GLOBAL, AWS_ISO_B_GLOBAL, AWS_ISO_E_GLOBAL, AWS_ISO_F_GLOBAL, AWS_ISO_GLOBAL,
        AWS_US_GOV_GLOBAL, CA_CENTRAL_1, CA_WEST_1, CN_NORTHWEST_1, CN_NORTH_1, EUSC_DE_EAST_1, EU_CENTRAL_1,
        EU_CENTRAL_2, EU_ISOE_WEST_1, EU_NORTH_1, EU_SOUTH_1, EU_SOUTH_2, EU_WEST_1, EU_WEST_2, EU_WEST_3,
        IL_CENTRAL_1, ME_CENTRAL_1, ME_SOUTH_1, MX_CENTRAL_1, SA_EAST_1, US_EAST_1, US_EAST_2, US_GOV_EAST_1,
        US_GOV_WEST_1, US_ISOB_EAST_1, US_ISOB_WEST_1, US_ISOF_EAST_1, US_ISOF_SOUTH_1, US_ISO_EAST_1, US_ISO_WEST_1,
        US_WEST_1, US_WEST_2);

    private final String id;
    private final boolean isGlobalRegion;

    private Region(String id, boolean isGlobalRegion) {
        this.id = id;
        this.isGlobalRegion = isGlobalRegion;
    }

    /**
     * The region with identifier {@code value}. Characters outside the URL-unreserved set are percent-encoded, as in
     * V2, so the ID is always safe to place in a host name or path.
     *
     * @throws NullPointerException if {@code value} is null
     * @throws IllegalArgumentException if {@code value} is blank
     */
    public static Region of(String value) {
        return of(value, false);
    }

    private static Region of(String value, boolean isGlobalRegion) {
        Objects.requireNonNull(value, "region");
        if (value.isBlank()) {
            throw new IllegalArgumentException("A region ID must not be blank.");
        }
        return Cache.VALUES.computeIfAbsent(encode(value), id -> new Region(id, isGlobalRegion));
    }

    /** Every region constant this SDK version declares, in declaration order. Immutable. */
    public static List<Region> regions() {
        return REGIONS;
    }

    /** The region identifier, for example {@code us-west-2}. */
    public String id() {
        return id;
    }

    /**
     * Whether this is a partition-global pseudo region (for example {@code aws-global}) rather than a geographic
     * region. Identifiers created through {@link #of(String)} that are not constants are never global.
     */
    public boolean isGlobalRegion() {
        return isGlobalRegion;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Region that && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }

    /** RFC 3986 percent-encoding of every character outside the unreserved set, matching V2's region encoding. */
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                         .replace("+", "%20")
                         .replace("*", "%2A")
                         .replace("%7E", "~");
    }

    /** Interned instances; a holder so the cache exists before the constants above are initialized. */
    private static final class Cache {
        private static final ConcurrentHashMap<String, Region> VALUES = new ConcurrentHashMap<>();

        private Cache() {
        }
    }
}
