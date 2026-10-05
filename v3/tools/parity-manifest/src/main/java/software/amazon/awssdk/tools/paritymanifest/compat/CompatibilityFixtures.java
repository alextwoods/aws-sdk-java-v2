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

package software.amazon.awssdk.tools.paritymanifest.compat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import software.amazon.awssdk.tools.paritymanifest.compat.CompatibilityFixture.Kind;
import software.amazon.awssdk.tools.paritymanifest.compat.CompatibilityFixture.Target;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * The compatibility fixtures generated for one test scope of a Parity Manifest, in a deterministic JSON form
 * ({@link #toJson()}, schema version {@link #SCHEMA_VERSION}): fixed member order, fixtures in
 * {@link CompatibilityFixture#ORDER}, every optional member written ({@code null} or {@code []}), four-space
 * indentation, UTF-8, trailing newline, no timestamps.
 *
 * @param v2Version   the manifest baseline's V2 version
 * @param gitRevision the manifest baseline's V2 source revision, or {@code null}
 * @param scope       the entry ID globs that selected the entries
 * @param fixtures    the fixtures, stored in {@link CompatibilityFixture#ORDER}
 * @param notes       in-scope fixture forms that could not be generated, and why; each noted entry still has at
 *                    least one fixture
 */
public record CompatibilityFixtures(String v2Version, String gitRevision, List<String> scope,
                                    List<CompatibilityFixture> fixtures, List<Note> notes) {
    /** The schema version of the JSON form. */
    public static final int SCHEMA_VERSION = 1;

    /**
     * A fixture form the generator could not produce for an in-scope entry.
     *
     * @param entryId the entry
     * @param note    what was not generated and why
     */
    public record Note(String entryId, String note) {
        public Note {
            Objects.requireNonNull(entryId, "entryId");
            Objects.requireNonNull(note, "note");
        }
    }

    public CompatibilityFixtures {
        Objects.requireNonNull(v2Version, "v2Version");
        scope = List.copyOf(scope);
        fixtures = fixtures.stream().sorted(CompatibilityFixture.ORDER).toList();
        notes = notes.stream().sorted(Comparator.comparing(Note::entryId).thenComparing(Note::note)).toList();
    }

    /** Fixture counts per kind, every kind present. */
    public Map<Kind, Integer> summary() {
        Map<Kind, Integer> counts = new EnumMap<>(Kind.class);
        for (Kind k : Kind.values()) {
            counts.put(k, 0);
        }
        fixtures.forEach(f -> counts.merge(f.kind(), 1, Integer::sum));
        return counts;
    }

    /** The fixtures of {@code kind}. */
    public List<CompatibilityFixture> ofKind(Kind kind) {
        return fixtures.stream().filter(f -> f.kind() == kind).toList();
    }

    /**
     * The evidence-hook registrations these fixtures provide, in the text form of the launch gate's evidence hooks
     * ({@code <evidenceId> <entryId>...} per line): each kind's executing suite, from {@code suiteByKind}, is bound to
     * every entry it has a fixture for. Kinds without a suite are omitted.
     */
    public String evidenceHooks(Map<Kind, String> suiteByKind) {
        SortedMap<String, SortedSet<String>> hooks = new TreeMap<>();
        for (CompatibilityFixture f : fixtures) {
            String suite = suiteByKind.get(f.kind());
            if (suite != null) {
                hooks.computeIfAbsent(suite, k -> new TreeSet<>()).add(f.entryId());
            }
        }
        StringBuilder text = new StringBuilder();
        hooks.forEach((suite, entries) -> entries.forEach(e -> text.append(suite).append(' ').append(e).append('\n')));
        return text.toString();
    }

    /** The canonical JSON form, ending with a newline. */
    public String toJson() {
        ObjectNode.Builder summary = Node.objectNodeBuilder();
        summary().forEach((k, n) -> summary.withMember(k.name(), n));
        ObjectNode top = Node.objectNodeBuilder()
                             .withMember("schemaVersion", SCHEMA_VERSION)
                             .withMember("baseline", Node.objectNodeBuilder()
                                                         .withMember("v2Version", v2Version)
                                                         .withMember("gitRevision", nullable(gitRevision))
                                                         .build())
                             .withMember("scope", Node.fromStrings(scope))
                             .withMember("summary", summary.build())
                             .withMember("fixtures", fixtures.stream().map(CompatibilityFixtures::fixture)
                                                             .collect(ArrayNode.collect()))
                             .withMember("notes", notes.stream().map(n -> Node.objectNodeBuilder()
                                                                              .withMember("entryId", n.entryId())
                                                                              .withMember("note", n.note())
                                                                              .build())
                                                       .collect(ArrayNode.collect()))
                             .build();
        return Node.prettyPrintJson(top) + "\n";
    }

    /** Writes {@link #toJson()} as UTF-8 to {@code path}, creating parent directories. */
    public void write(Path path) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, toJson(), StandardCharsets.UTF_8);
    }

    private static ObjectNode fixture(CompatibilityFixture f) {
        return Node.objectNodeBuilder()
                   .withMember("id", f.id())
                   .withMember("entryId", f.entryId())
                   .withMember("classification", f.classification().name())
                   .withMember("kind", f.kind().name())
                   .withMember("dimension", nullable(f.dimension() == null ? null : f.dimension().jsonName()))
                   .withMember("v2Surface", f.v2Surface())
                   .withMember("target", f.target() == null ? Node.nullNode() : target(f.target()))
                   .withMember("source", nullable(f.source()))
                   .withMember("symbols", Node.fromStrings(f.symbols()))
                   .withMember("diagnostic", nullable(f.diagnostic()))
                   .withMember("supertypes", Node.fromStrings(f.supertypes()))
                   .withMember("evidenceIds", Node.fromStrings(f.evidenceIds()))
                   .build();
    }

    private static ObjectNode target(Target t) {
        return Node.objectNodeBuilder()
                   .withMember("kind", t.kind().name())
                   .withMember("owner", t.owner())
                   .withMember("ownerKind", t.ownerKind())
                   .withMember("ownerFinal", t.ownerFinal())
                   .withMember("name", nullable(t.name()))
                   .withMember("descriptor", nullable(t.descriptor()))
                   .withMember("static", t.isStatic())
                   .withMember("protected", t.isProtected())
                   .build();
    }

    private static Node nullable(String value) {
        return value == null ? Node.nullNode() : Node.from(value);
    }
}
