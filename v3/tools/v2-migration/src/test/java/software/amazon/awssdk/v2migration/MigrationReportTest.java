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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.v2migration.Fixtures.APP_PATH;
import static software.amazon.awssdk.v2migration.Fixtures.forEntry;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.v2migration.recipe.RecipeOutcome;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

class MigrationReportTest {

    @TempDir
    static Path stubDirectory;
    private static Path stubs;

    @TempDir
    Path project;

    @BeforeAll
    static void compileStubs() throws Exception {
        stubs = V2Stubs.compile(stubDirectory);
    }

    @Test
    void reportJson_migration_isCompleteAndConsistentWithTheResult() throws Exception {
        MigrationResult result = migrate();

        String json = result.reportJson(true);
        ObjectNode report = Node.parse(json).expectObjectNode();

        assertTrue(json.endsWith("}\n") && !json.endsWith("\n\n"));
        assertEquals(MigrationReport.TOP_MEMBERS, keys(report));
        assertEquals(1, report.expectNumberMember("schemaVersion").getValue().intValue());
        assertEquals("com.example:app", report.expectStringMember("project").getValue());
        assertTrue(report.expectBooleanMember("applied").getValue());
        ObjectNode summary = report.expectObjectMember("summary");
        assertEquals(result.items().size(), summary.expectNumberMember("items").getValue().intValue());
        ObjectNode dispositions = summary.expectObjectMember("dispositions");
        assertEquals(1, dispositions.expectNumberMember("TRANSFORMED").getValue().intValue());
        assertEquals(2, dispositions.expectNumberMember("UNRESOLVED").getValue().intValue());
        assertEquals(result.count(Disposition.PRESERVED),
                     dispositions.expectNumberMember("PRESERVED").getValue().longValue());
        assertEquals(names(MigrationCategory.values()), keys(summary.expectObjectMember("categories")));
        assertEquals(List.of("PRESERVED", "ADAPTED", "EXCLUDED", "UNCLASSIFIED"),
                     keys(summary.expectObjectMember("classifications")));

        List<ObjectNode> items = report.expectArrayMember("items").getElementsAs(ObjectNode.class);
        assertEquals(result.items().size(), items.size());
        items.forEach(i -> assertEquals(MigrationReport.ITEM_MEMBERS, keys(i)));
        List<ObjectNode> diagnostics = report.expectArrayMember("diagnostics").getElementsAs(ObjectNode.class);
        assertEquals(2, diagnostics.size());
        for (ObjectNode d : diagnostics) {
            assertEquals(MigrationReport.DIAGNOSTIC_MEMBERS, keys(d));
            assertEquals("BLOCKING", d.expectStringMember("severity").getValue());
            assertEquals(MigrationReport.LOCATION_MEMBERS, keys(d.expectObjectMember("location")));
            assertEquals(APP_PATH, d.expectObjectMember("location").expectStringMember("path").getValue());
            assertFalse(d.expectStringMember("rationale").getValue().isEmpty());
        }
        ObjectNode file = report.expectArrayMember("files").getElementsAs(ObjectNode.class).get(0);
        assertEquals(APP_PATH, file.expectStringMember("path").getValue());
        assertTrue(file.expectBooleanMember("changed").getValue());
        ObjectNode recipe = report.expectArrayMember("recipes").getElementsAs(ObjectNode.class).get(0);
        assertEquals("retry.rename", recipe.expectStringMember("id").getValue());
        assertEquals(1, recipe.expectNumberMember("transformed").getValue().intValue());
    }

    @Test
    void reportJson_sameMigration_isByteIdentical() throws Exception {
        assertEquals(migrate().reportJson(false), migrate().reportJson(false));
    }

    /** Every object the writer emits has exactly the members its schema requires, in the schema's order. */
    @Test
    void writerMembers_matchTheReportSchema() throws IOException {
        ObjectNode schema;
        try (InputStream in = MigrationReport.class.getResourceAsStream(MigrationReport.SCHEMA_RESOURCE)) {
            schema = Node.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8)).expectObjectNode();
        }
        ObjectNode properties = schema.expectObjectMember("properties");
        ObjectNode defs = schema.expectObjectMember("$defs");

        assertEquals(MigrationReport.TOP_MEMBERS, required(schema));
        assertEquals(MigrationReport.TOOL_MEMBERS, required(properties.expectObjectMember("tool")));
        assertEquals(MigrationReport.MANIFEST_MEMBERS, required(properties.expectObjectMember("manifest")));
        assertEquals(MigrationReport.SUMMARY_MEMBERS, required(properties.expectObjectMember("summary")));
        assertEquals(MigrationReport.RECIPE_MEMBERS,
                     required(properties.expectObjectMember("recipes").expectObjectMember("items")));
        assertEquals(MigrationReport.FILE_MEMBERS,
                     required(properties.expectObjectMember("files").expectObjectMember("items")));
        assertEquals(MigrationReport.LOCATION_MEMBERS, required(defs.expectObjectMember("location")));
        assertEquals(MigrationReport.ITEM_MEMBERS, required(defs.expectObjectMember("item")));
        assertEquals(MigrationReport.EDIT_MEMBERS, required(defs.expectObjectMember("item").expectObjectMember(
            "properties").expectObjectMember("edits").expectObjectMember("items")));
        assertEquals(MigrationReport.DIAGNOSTIC_MEMBERS, required(defs.expectObjectMember("diagnostic")));
        assertEquals(names(MigrationCategory.values()), strings(defs.expectObjectMember("category")
                                                                    .expectArrayMember("enum")));
        assertEquals(names(ItemKind.values()), strings(defs.expectObjectMember("item").expectObjectMember("properties")
                                                           .expectObjectMember("kind").expectArrayMember("enum")));
        assertEquals(names(Disposition.values()), strings(defs.expectObjectMember("item")
                                                              .expectObjectMember("properties")
                                                              .expectObjectMember("disposition")
                                                              .expectArrayMember("enum")));
    }

    private MigrationResult migrate() throws Exception {
        return MigrationEngine.run(Fixtures.app(project, stubs, List.of(
            forEntry("retry.rename", V2Stubs.DEFAULT_RETRY_POLICY, ItemKind.METHOD_INVOCATION,
                     c -> RecipeOutcome.transform("Rename.", List.of(
                         TextEdit.replace(c.item().location(), "StandardRetryStrategy.create()")))))).build());
    }

    private static List<String> keys(ObjectNode node) {
        return node.getMembers().keySet().stream().map(k -> k.getValue()).toList();
    }

    private static List<String> required(ObjectNode schema) {
        return strings(schema.expectArrayMember("required"));
    }

    private static List<String> strings(ArrayNode array) {
        return array.getElements().stream().filter(Node::isStringNode).map(n -> n.expectStringNode().getValue())
                    .toList();
    }

    private static List<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toList();
    }
}
