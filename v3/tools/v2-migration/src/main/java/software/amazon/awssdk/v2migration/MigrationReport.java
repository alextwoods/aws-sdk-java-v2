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

package software.amazon.awssdk.v2migration;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import software.amazon.awssdk.v2migration.manifest.Classification;
import software.amazon.awssdk.v2migration.manifest.ManifestBaseline;
import software.amazon.awssdk.v2migration.recipe.MigrationRecipe;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * The machine-readable migration report ({@link #SCHEMA_RESOURCE}; Requirements 17.10-17.12): the manifest it was
 * joined against, disposition counts overall, per Compatibility Classification and per migration category, the
 * recipes consulted, every file, every item with its single disposition, and every unresolved diagnostic.
 *
 * <p>Output is byte-reproducible for identical input: fixed member order, every optional member written (as
 * {@code null} or {@code []}), items and diagnostics in item order, maps with every key present, four-space
 * indentation, UTF-8 and a trailing newline. No timestamps are written, and paths are relative to the project root
 * (a file outside the root, which only an explicit request can name, keeps its absolute path).
 */
public final class MigrationReport {

    /** The classpath location of the report JSON schema. */
    public static final String SCHEMA_RESOURCE = "/software/amazon/awssdk/v2migration/v2-migration-report-1.schema.json";
    /** The report schema version. */
    public static final int SCHEMA_VERSION = 1;
    /** The {@code classifications} key counting items without a manifest entry. */
    static final String UNCLASSIFIED = "UNCLASSIFIED";
    /** The severity of every diagnostic: an unresolved item blocks a complete migration. */
    static final String BLOCKING = "BLOCKING";

    static final List<String> TOP_MEMBERS = List.of("schemaVersion", "tool", "project", "applied", "manifest",
                                                    "summary", "recipes", "files", "items", "diagnostics");
    static final List<String> TOOL_MEMBERS = List.of("name", "version");
    static final List<String> MANIFEST_MEMBERS = List.of("v2Version", "gitRevision", "scannerVersion",
                                                         "candidateVersion", "entries", "summary");
    static final List<String> SUMMARY_MEMBERS = List.of("items", "dispositions", "classifications", "categories");
    static final List<String> RECIPE_MEMBERS = List.of("id", "description", "preserved", "transformed", "unresolved");
    static final List<String> FILE_MEMBERS = List.of("path", "items", "changed");
    static final List<String> LOCATION_MEMBERS = List.of("path", "line", "column", "endLine", "endColumn",
                                                         "startOffset", "endOffset");
    static final List<String> ITEM_MEMBERS = List.of("kind", "category", "id", "construct", "location", "disposition",
                                                     "classification", "recipe", "reason", "edits");
    static final List<String> EDIT_MEMBERS = List.of("path", "startOffset", "endOffset", "replacement");
    static final List<String> DIAGNOSTIC_MEMBERS = List.of("severity", "location", "construct", "entryId",
                                                           "classification", "rationale", "replacement",
                                                           "migrationKind", "migration", "differences", "reason",
                                                           "message");

    private static final String TOOL = "v2-migration";

    private MigrationReport() {
    }

    static String toJson(MigrationResult result, boolean applied) {
        ObjectNode.Builder top = Node.objectNodeBuilder()
                                     .withMember("schemaVersion", SCHEMA_VERSION)
                                     .withMember("tool", Node.objectNodeBuilder()
                                                             .withMember("name", TOOL)
                                                             .withMember("version", toolVersion())
                                                             .build())
                                     .withMember("project", nullable(result.project()))
                                     .withMember("applied", applied)
                                     .withMember("manifest", manifest(result))
                                     .withMember("summary", summary(result.items()))
                                     .withMember("recipes", recipes(result))
                                     .withMember("files", files(result))
                                     .withMember("items", result.items().stream().map(MigrationReport::item)
                                                                .collect(ArrayNode.collect()))
                                     .withMember("diagnostics", result.diagnostics().stream()
                                                                      .map(MigrationReport::diagnostic)
                                                                      .collect(ArrayNode.collect()));
        return Node.prettyPrintJson(top.build()) + "\n";
    }

    private static ObjectNode manifest(MigrationResult result) {
        ManifestBaseline b = result.manifest().baseline();
        ObjectNode.Builder summary = Node.objectNodeBuilder();
        result.manifest().summary().forEach((c, n) -> summary.withMember(c.name(), n));
        return Node.objectNodeBuilder()
                   .withMember("v2Version", b.v2Version())
                   .withMember("gitRevision", nullable(b.gitRevision()))
                   .withMember("scannerVersion", b.scannerVersion())
                   .withMember("candidateVersion", nullable(b.candidateVersion()))
                   .withMember("entries", result.manifest().size())
                   .withMember("summary", summary.build())
                   .build();
    }

    private static ObjectNode summary(List<ItemResult> items) {
        Map<Disposition, Integer> dispositions = counts(items);
        Map<String, Integer> classifications = new TreeMap<>();
        for (Classification c : Classification.values()) {
            classifications.put(c.name(), 0);
        }
        classifications.put(UNCLASSIFIED, 0);
        items.forEach(i -> classifications.merge(i.entry() == null ? UNCLASSIFIED : i.entry().classification().name(),
                                                 1, Integer::sum));
        ObjectNode.Builder byClassification = Node.objectNodeBuilder();
        for (Classification c : Classification.values()) {
            byClassification.withMember(c.name(), classifications.get(c.name()));
        }
        byClassification.withMember(UNCLASSIFIED, classifications.get(UNCLASSIFIED));
        ObjectNode.Builder categories = Node.objectNodeBuilder();
        for (MigrationCategory category : MigrationCategory.values()) {
            categories.withMember(category.name(),
                                  dispositions(counts(items.stream().filter(i -> i.item().category() == category)
                                                           .toList())));
        }
        return Node.objectNodeBuilder()
                   .withMember("items", items.size())
                   .withMember("dispositions", dispositions(dispositions))
                   .withMember("classifications", byClassification.build())
                   .withMember("categories", categories.build())
                   .build();
    }

    private static Map<Disposition, Integer> counts(List<ItemResult> items) {
        Map<Disposition, Integer> counts = new EnumMap<>(Disposition.class);
        for (Disposition d : Disposition.values()) {
            counts.put(d, 0);
        }
        items.forEach(i -> counts.merge(i.disposition(), 1, Integer::sum));
        return counts;
    }

    private static ObjectNode dispositions(Map<Disposition, Integer> counts) {
        ObjectNode.Builder out = Node.objectNodeBuilder();
        counts.forEach((d, n) -> out.withMember(d.name(), n));
        return out.build();
    }

    private static ArrayNode recipes(MigrationResult result) {
        return result.recipes().stream().map(r -> recipe(r, result.items())).collect(ArrayNode.collect());
    }

    private static ObjectNode recipe(MigrationRecipe recipe, List<ItemResult> items) {
        Map<Disposition, Integer> counts = counts(items.stream().filter(i -> recipe.id().equals(i.recipe())).toList());
        return Node.objectNodeBuilder()
                   .withMember("id", recipe.id())
                   .withMember("description", recipe.description())
                   .withMember("preserved", counts.get(Disposition.PRESERVED))
                   .withMember("transformed", counts.get(Disposition.TRANSFORMED))
                   .withMember("unresolved", counts.get(Disposition.UNRESOLVED))
                   .build();
    }

    private static ArrayNode files(MigrationResult result) {
        Map<String, Integer> itemCounts = new TreeMap<>();
        result.originals().keySet().forEach(p -> itemCounts.put(p, 0));
        result.items().forEach(i -> itemCounts.merge(i.item().location().path(), 1, Integer::sum));
        return itemCounts.entrySet().stream()
                         .map(e -> Node.objectNodeBuilder()
                                       .withMember("path", e.getKey())
                                       .withMember("items", e.getValue())
                                       .withMember("changed", result.changed().containsKey(e.getKey()))
                                       .build())
                         .collect(ArrayNode.collect());
    }

    private static ObjectNode item(ItemResult r) {
        MigrationItem item = r.item();
        return Node.objectNodeBuilder()
                   .withMember("kind", item.kind().name())
                   .withMember("category", item.category().name())
                   .withMember("id", nullable(item.id()))
                   .withMember("construct", item.construct())
                   .withMember("location", location(item.location()))
                   .withMember("disposition", r.disposition().name())
                   .withMember("classification", nullable(r.entry() == null ? null : r.entry().classification().name()))
                   .withMember("recipe", nullable(r.recipe()))
                   .withMember("reason", nullable(r.reason()))
                   .withMember("edits", r.edits().stream().map(MigrationReport::edit).collect(ArrayNode.collect()))
                   .build();
    }

    private static ObjectNode edit(TextEdit e) {
        return Node.objectNodeBuilder()
                   .withMember("path", e.path())
                   .withMember("startOffset", e.startOffset())
                   .withMember("endOffset", e.endOffset())
                   .withMember("replacement", e.replacement())
                   .build();
    }

    private static ObjectNode diagnostic(MigrationDiagnostic d) {
        return Node.objectNodeBuilder()
                   .withMember("severity", BLOCKING)
                   .withMember("location", location(d.location()))
                   .withMember("construct", d.construct())
                   .withMember("entryId", nullable(d.entryId()))
                   .withMember("classification", nullable(d.classification() == null ? null
                                                                                    : d.classification().name()))
                   .withMember("rationale", nullable(d.rationale()))
                   .withMember("replacement", nullable(d.replacement()))
                   .withMember("migrationKind", nullable(d.migrationKind() == null ? null : d.migrationKind().name()))
                   .withMember("migration", nullable(d.migration()))
                   .withMember("differences", Node.fromStrings(d.differences()))
                   .withMember("reason", d.reason())
                   .withMember("message", d.message())
                   .build();
    }

    private static ObjectNode location(SourceLocation l) {
        return Node.objectNodeBuilder()
                   .withMember("path", l.path())
                   .withMember("line", l.line())
                   .withMember("column", l.column())
                   .withMember("endLine", l.endLine())
                   .withMember("endColumn", l.endColumn())
                   .withMember("startOffset", l.startOffset())
                   .withMember("endOffset", l.endOffset())
                   .build();
    }

    private static Node nullable(String value) {
        return value == null ? Node.nullNode() : Node.from(value);
    }

    /** This artifact's version, from the module version resource every V3 artifact publishes. */
    static String toolVersion() {
        try (InputStream in = MigrationReport.class.getResourceAsStream(
            "/META-INF/aws-sdk-java-v3/modules/v2-migration.properties")) {
            if (in == null) {
                return "unknown";
            }
            Properties properties = new Properties();
            properties.load(in);
            return properties.getProperty("version", "unknown");
        } catch (IOException e) {
            return "unknown";
        }
    }
}
