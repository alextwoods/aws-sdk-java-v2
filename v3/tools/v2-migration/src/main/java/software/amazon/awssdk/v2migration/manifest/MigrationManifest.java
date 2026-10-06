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

package software.amazon.awssdk.v2migration.manifest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

/**
 * A published Parity Manifest ({@code parity-manifest-1.schema.json}, written by the {@code parity-manifest} release
 * tooling), indexed for the lookups the Migration Tooling performs: exact entry IDs, binary-name resolution of source
 * type names, and member entries by owner and name.
 *
 * <p>{@link #read(String)} accepts exactly the documents the manifest's publisher accepts: the schema's structure,
 * member types and classification-dependent rules, plus the publisher's validation of ID schemes, required text,
 * derived members ({@code binary}, {@code evidence}, {@code summary}) and unique IDs. Like the publisher's reader, a
 * nullable member may be absent, unknown members are rejected, and every problem is reported at once.
 */
public final class MigrationManifest {

    /** The only supported schema version ({@code schemaVersion}). */
    public static final int SCHEMA_VERSION = 1;

    private final ManifestBaseline baseline;
    private final NavigableMap<String, ManifestEntry> entries;
    private final Map<Classification, Integer> summary;

    private MigrationManifest(ManifestBaseline baseline, NavigableMap<String, ManifestEntry> entries) {
        this.baseline = baseline;
        this.entries = Collections.unmodifiableNavigableMap(entries);
        Map<Classification, Integer> counts = new EnumMap<>(Classification.class);
        for (Classification c : Classification.values()) {
            counts.put(c, 0);
        }
        entries.values().forEach(e -> counts.merge(e.classification(), 1, Integer::sum));
        this.summary = Collections.unmodifiableMap(counts);
    }

    /** Reads and validates the manifest at {@code path} (UTF-8). */
    public static MigrationManifest read(Path path) throws IOException, ManifestFormatException {
        return read(Files.readString(path, StandardCharsets.UTF_8));
    }

    /** Reads and validates a manifest document, reporting every problem at once. */
    public static MigrationManifest read(String json) throws ManifestFormatException {
        ManifestReader reader = new ManifestReader();
        ManifestReader.Result result = reader.read(json);
        if (!result.problems().isEmpty()) {
            throw new ManifestFormatException(result.problems());
        }
        TreeMap<String, ManifestEntry> byId = new TreeMap<>();
        result.entries().forEach(e -> byId.put(e.id(), e));
        return new MigrationManifest(result.baseline(), byId);
    }

    /** A manifest of {@code entries} (for callers that build manifests in memory); IDs must be unique. */
    public static MigrationManifest of(ManifestBaseline baseline, List<ManifestEntry> entries) {
        TreeMap<String, ManifestEntry> byId = new TreeMap<>();
        for (ManifestEntry e : entries) {
            if (byId.put(e.id(), e) != null) {
                throw new IllegalArgumentException("Duplicate manifest entry ID: " + e.id());
            }
        }
        return new MigrationManifest(baseline, byId);
    }

    /** The baseline (and candidate) identity. */
    public ManifestBaseline baseline() {
        return baseline;
    }

    /** Every entry, sorted by ID. */
    public List<ManifestEntry> entries() {
        return List.copyOf(entries.values());
    }

    /** The number of entries. */
    public int size() {
        return entries.size();
    }

    /** Entry counts per classification, every classification present. */
    public Map<Classification, Integer> summary() {
        return summary;
    }

    /** The entry with exactly {@code id}, if any. */
    public Optional<ManifestEntry> entry(String id) {
        return Optional.ofNullable(entries.get(id));
    }

    /**
     * The {@link EntryKind#PUBLIC_TYPE} entry of a type named in source, if any. Source names nested types with dots
     * ({@code a.b.Outer.Inner}); the manifest uses binary names ({@code a.b.Outer$Inner}), so the trailing dots are
     * tried as {@code $} separators from the right until an entry matches.
     */
    public Optional<ManifestEntry> typeEntry(String sourceOrBinaryName) {
        String name = sourceOrBinaryName;
        while (true) {
            ManifestEntry entry = entries.get(EntryKind.typeId(name));
            if (entry != null) {
                return Optional.of(entry);
            }
            int dot = name.lastIndexOf('.');
            if (dot < 0) {
                return Optional.empty();
            }
            name = name.substring(0, dot) + "$" + name.substring(dot + 1);
        }
    }

    /**
     * Every {@link EntryKind#PUBLIC_MEMBER} entry named {@code memberName} that {@code ownerBinaryName} declares
     * (all overloads of a method, or the field), sorted by ID.
     */
    public List<ManifestEntry> members(String ownerBinaryName, String memberName) {
        String base = "api:" + ownerBinaryName + "#" + memberName;
        List<ManifestEntry> found = new ArrayList<>();
        found.addAll(prefixed(base + "(").values());
        found.addAll(prefixed(base + ":").values());
        found.sort((a, b) -> a.id().compareTo(b.id()));
        return found;
    }

    private NavigableMap<String, ManifestEntry> prefixed(String prefix) {
        return entries.subMap(prefix, true, prefix + Character.MAX_VALUE, false);
    }
}
