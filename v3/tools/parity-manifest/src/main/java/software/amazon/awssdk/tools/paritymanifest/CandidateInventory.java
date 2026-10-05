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

package software.amazon.awssdk.tools.paritymanifest;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * What a V3 release candidate ships, in Parity Manifest ID space: the candidate artifacts and every capability ID
 * ({@code artifact:}, {@code service:}, {@code api:}, {@code http-mode:}, and declared {@code behavior:} and
 * {@code config:} IDs) with the artifacts that ship it. Built by {@link CandidateInventoryScanner} or in code.
 *
 * @param artifacts    candidate artifacts by {@code groupId:artifactId}
 * @param capabilities shipped capability IDs, each with its owning artifact coordinates (or {@link #DECLARED})
 */
public record CandidateInventory(SortedMap<String, CandidateArtifact> artifacts,
                                 SortedMap<String, SortedSet<String>> capabilities) {
    /** The owner recorded for capabilities declared outside any candidate artifact. */
    public static final String DECLARED = "declared";

    /**
     * One candidate artifact.
     *
     * @param coordinates              {@code groupId:artifactId}
     * @param version                  the artifact version, or {@code null} when the artifact does not record it
     * @param location                 where the artifact was read from
     * @param moduleName               the explicit or automatic Java module name, or {@code null} when it has none
     * @param nativeImageConfiguration true if the artifact ships {@code META-INF/native-image/} resources
     */
    public record CandidateArtifact(String coordinates, String version, String location, String moduleName,
                                    boolean nativeImageConfiguration) {
        public CandidateArtifact {
            Objects.requireNonNull(coordinates, "coordinates");
            Objects.requireNonNull(location, "location");
        }
    }

    public CandidateInventory {
        artifacts = Collections.unmodifiableSortedMap(new TreeMap<>(artifacts));
        SortedMap<String, SortedSet<String>> copy = new TreeMap<>();
        capabilities.forEach((id, owners) -> copy.put(id, Collections.unmodifiableSortedSet(new TreeSet<>(owners))));
        capabilities = Collections.unmodifiableSortedMap(copy);
    }

    /** An empty inventory builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** Every shipped capability ID. */
    public Set<String> ids() {
        return capabilities.keySet();
    }

    /** True if the candidate ships capability {@code id}. */
    public boolean ships(String id) {
        return capabilities.containsKey(id);
    }

    /** True if the candidate contains the artifact {@code groupId:artifactId}. */
    public boolean hasArtifact(String coordinates) {
        return artifacts.containsKey(coordinates);
    }

    /** The artifacts (or {@link #DECLARED}) shipping {@code id}; empty if it is not shipped. */
    public SortedSet<String> owners(String id) {
        return capabilities.getOrDefault(id, Collections.emptySortedSet());
    }

    /**
     * True if the candidate ships a member of the same type with the same name and member kind as the
     * {@link EntryKind#PUBLIC_MEMBER} ID {@code memberId}, whatever its descriptor. This is the source-level shipped
     * check: an exact descriptor match is only required for binary claims, and source compatibility of differing
     * descriptors is verified by the generated compile fixtures.
     */
    public boolean shipsMemberNamed(String memberId) {
        String key = memberKey(memberId);
        if (key == null) {
            return false;
        }
        String next = capabilities.tailMap(key).keySet().stream().findFirst().orElse("");
        return next.startsWith(key);
    }

    /** {@code api:T#name(} for a method or constructor, {@code api:T#name:} for a field, else {@code null}. */
    static String memberKey(String memberId) {
        int hash = memberId.indexOf('#');
        if (!memberId.startsWith("api:") || hash < 0) {
            return null;
        }
        for (int i = hash + 1; i < memberId.length(); i++) {
            char c = memberId.charAt(i);
            if (c == '(' || c == ':') {
                return memberId.substring(0, i + 1);
            }
        }
        return null;
    }

    /** Accumulates artifacts and capabilities. */
    public static final class Builder {
        private final Map<String, CandidateArtifact> artifacts = new TreeMap<>();
        private final Map<String, SortedSet<String>> capabilities = new TreeMap<>();

        private Builder() {
        }

        /** Adds {@code artifact} and its {@code artifact:} capability; a second artifact with equal coordinates fails. */
        public Builder artifact(CandidateArtifact artifact) {
            CandidateArtifact previous = artifacts.putIfAbsent(artifact.coordinates(), artifact);
            if (previous != null) {
                throw new IllegalArgumentException("Candidate artifact " + artifact.coordinates() + " appears twice: "
                                                   + previous.location() + " and " + artifact.location());
            }
            return capability("artifact:" + artifact.coordinates(), artifact.coordinates());
        }

        /** Records that {@code owner} ships capability {@code id}; {@code id} must be a well-formed entry ID. */
        public Builder capability(String id, String owner) {
            if (EntryKind.ofId(id).isEmpty()) {
                throw new IllegalArgumentException("'" + id + "' is not a well-formed Parity Manifest entry ID");
            }
            capabilities.computeIfAbsent(id, k -> new TreeSet<>()).add(owner);
            return this;
        }

        public CandidateInventory build() {
            return new CandidateInventory(new TreeMap<>(artifacts), new TreeMap<>(capabilities));
        }
    }
}
