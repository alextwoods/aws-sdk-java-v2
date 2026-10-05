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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import software.amazon.awssdk.endpoints.internal.Json;
import software.amazon.awssdk.regions.Region;

/**
 * The canonical AWS partition table, as used by the {@code aws.partition} endpoint rules function, and the
 * region-to-partition mapping it defines.
 *
 * <p>{@link #defaults()} is the table of the pinned Smithy AWS endpoints release this SDK version was built with
 * (copied into this artifact at build time), so partition outputs are exactly those the canonical endpoint rule sets
 * and their test cases are evaluated against. A region maps to the partition that lists it; otherwise to the first
 * partition whose region pattern it matches (an <em>inferred</em> partition); otherwise to the {@code aws} partition.
 * Instances are immutable and thread-safe.
 */
public final class Partitions {
    /** The ID of the partition unknown regions fall back to. */
    public static final String DEFAULT_PARTITION = "aws";

    private static final String RESOURCE = "partitions.json";

    private final String version;
    private final List<PartitionMetadata> partitions;
    private final List<Pattern> patterns;
    private final Map<String, PartitionMetadata> byRegion;
    private final Map<String, PartitionMetadata> byId;
    private final PartitionMetadata fallback;

    private Partitions(String version, List<PartitionMetadata> partitions) {
        this.version = version;
        this.partitions = List.copyOf(partitions);
        List<Pattern> compiled = new ArrayList<>(partitions.size());
        Map<String, PartitionMetadata> regions = new HashMap<>();
        Map<String, PartitionMetadata> ids = new LinkedHashMap<>();
        for (PartitionMetadata partition : partitions) {
            compiled.add(Pattern.compile(partition.regionRegex()));
            // A later partition listing the same region wins, as in the canonical evaluator.
            partition.regions().forEach(region -> regions.put(region, partition));
            ids.put(partition.id(), partition);
        }
        this.patterns = List.copyOf(compiled);
        this.byRegion = Map.copyOf(regions);
        this.byId = Collections.unmodifiableMap(ids);
        this.fallback = ids.get(DEFAULT_PARTITION);
    }

    /** The canonical partition table bundled with this SDK version. */
    public static Partitions defaults() {
        return Defaults.INSTANCE;
    }

    /**
     * A partition table from its canonical JSON document ({@code {"version": ..., "partitions": [...]}}).
     *
     * @throws IllegalArgumentException if the document is not a valid partition table
     */
    public static Partitions fromJson(String json) {
        Map<String, Object> root = Json.parseObject(json);
        List<PartitionMetadata> partitions = new ArrayList<>();
        for (Object entry : list(root.get("partitions"), "partitions")) {
            Map<String, Object> partition = object(entry, "partition");
            Map<String, Object> outputs = object(partition.get("outputs"), "partition outputs");
            Map<String, Object> regions = object(partition.get("regions"), "partition regions");
            partitions.add(new PartitionMetadata(
                string(partition.get("id"), "partition id"),
                string(partition.get("regionRegex"), "regionRegex"),
                regions.keySet(),
                string(outputs.get("name"), "name"),
                string(outputs.get("dnsSuffix"), "dnsSuffix"),
                string(outputs.get("dualStackDnsSuffix"), "dualStackDnsSuffix"),
                bool(outputs.get("supportsFIPS"), "supportsFIPS"),
                bool(outputs.get("supportsDualStack"), "supportsDualStack"),
                string(outputs.get("implicitGlobalRegion"), "implicitGlobalRegion")));
        }
        return new Partitions(root.get("version") instanceof String v ? v : "", partitions);
    }

    /** The version of the partition table document. */
    public String version() {
        return version;
    }

    /** Every partition, in table order. Immutable. */
    public List<PartitionMetadata> partitions() {
        return partitions;
    }

    /** The partition with ID {@code id}, if the table has it. */
    public Optional<PartitionMetadata> partition(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /** The partition of {@code region}; see {@link #forRegion(String)}. */
    public PartitionMetadata of(Region region) {
        return forRegion(Objects.requireNonNull(region, "region").id())
            .orElseThrow(() -> new IllegalStateException("No partition matches region " + region + " and the table "
                                                         + "has no " + DEFAULT_PARTITION + " partition."));
    }

    /**
     * The partition of region name {@code region}: the partition that lists it, else the first whose region pattern
     * matches it, else the {@value #DEFAULT_PARTITION} partition. Empty only if none applies and the table has no
     * {@value #DEFAULT_PARTITION} partition.
     */
    public Optional<PartitionMetadata> forRegion(String region) {
        Objects.requireNonNull(region, "region");
        PartitionMetadata known = byRegion.get(region);
        if (known != null) {
            return Optional.of(known);
        }
        for (int i = 0; i < partitions.size(); i++) {
            if (patterns.get(i).matcher(region).matches()) {
                return Optional.of(partitions.get(i));
            }
        }
        return Optional.ofNullable(fallback);
    }

    /** Whether {@code region} is listed by a partition (rather than matched by a pattern or the fallback). */
    public boolean isKnownRegion(String region) {
        return byRegion.containsKey(region);
    }

    private static List<?> list(Object value, String what) {
        if (value instanceof List<?> list) {
            return list;
        }
        throw new IllegalArgumentException("Invalid partition table: " + what + " must be an array.");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value, String what) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new IllegalArgumentException("Invalid partition table: " + what + " must be an object.");
    }

    private static String string(Object value, String what) {
        if (value instanceof String s) {
            return s;
        }
        throw new IllegalArgumentException("Invalid partition table: " + what + " must be a string.");
    }

    private static boolean bool(Object value, String what) {
        if (value instanceof Boolean b) {
            return b;
        }
        throw new IllegalArgumentException("Invalid partition table: " + what + " must be a boolean.");
    }

    /** Loads the bundled table on first use. */
    private static final class Defaults {
        static final Partitions INSTANCE = load();

        private Defaults() {
        }

        private static Partitions load() {
            try (InputStream in = Partitions.class.getResourceAsStream(RESOURCE)) {
                if (in == null) {
                    throw new IllegalStateException("The canonical partition table " + RESOURCE + " is missing from "
                                                    + "the endpoints artifact.");
                }
                return fromJson(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException("Unable to read the canonical partition table " + RESOURCE, e);
            }
        }
    }
}
