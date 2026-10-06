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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.v2migration.Fixtures.APP;
import static software.amazon.awssdk.v2migration.Fixtures.APP_PATH;
import static software.amazon.awssdk.v2migration.Fixtures.forEntry;
import static software.amazon.awssdk.v2migration.Fixtures.recipe;
import static software.amazon.awssdk.v2migration.Fixtures.result;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.v2migration.manifest.EntryKind;
import software.amazon.awssdk.v2migration.recipe.MigrationRecipe;
import software.amazon.awssdk.v2migration.recipe.RecipeOutcome;

/** Recipe dispatch, disposition rules, edit validation and deterministic application. */
class MigrationEngineTest {

    private static final String IMPORT_LINE = "import software.amazon.awssdk.retries.StandardRetryStrategy;\n";

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
    void run_transformingRecipe_appliesOnlyItsEditAndLeavesEverythingElseByteForByte() throws Exception {
        MigrationRecipe rename = forEntry("retry.rename", V2Stubs.DEFAULT_RETRY_POLICY, ItemKind.METHOD_INVOCATION,
                                          c -> RecipeOutcome.transform("Uses the standard strategy.", List.of(
                                              TextEdit.replace(c.item().location(), "StandardRetryStrategy.create()"))));

        MigrationResult result = run(rename);

        ItemResult call = result(result, ItemKind.METHOD_INVOCATION, V2Stubs.DEFAULT_RETRY_POLICY);
        assertEquals(Disposition.TRANSFORMED, call.disposition());
        assertEquals("retry.rename", call.recipe());
        assertEquals("Uses the standard strategy.", call.reason());
        assertNull(call.diagnostic());
        assertEquals(APP.replace("return defaultRetryPolicy();", "return StandardRetryStrategy.create();"),
                     result.changed().get(APP_PATH));
        // Nothing is written until write().
        assertEquals(APP, Files.readString(project.resolve(APP_PATH), StandardCharsets.UTF_8));
        result.write();
        assertEquals(result.changed().get(APP_PATH), Files.readString(project.resolve(APP_PATH), StandardCharsets.UTF_8));
    }

    @Test
    void run_sameInput_producesIdenticalEditsAndReport() throws Exception {
        MigrationRecipe rename = forEntry("retry.rename", V2Stubs.DEFAULT_RETRY_POLICY, ItemKind.METHOD_INVOCATION,
                                          c -> RecipeOutcome.transform("Rename.", List.of(
                                              TextEdit.replace(c.item().location(), "StandardRetryStrategy.create()"))));

        MigrationResult first = run(rename);
        MigrationResult second = run(rename);

        assertEquals(first.changed(), second.changed());
        assertEquals(first.reportJson(false), second.reportJson(false));
    }

    @Test
    void run_unsafeAnswer_leavesTheConstructUnchangedWithAFullDiagnostic() throws Exception {
        MigrationRecipe unsafe = forEntry("builder.region", V2Stubs.BUILDER_REGION, ItemKind.METHOD_INVOCATION,
                                          c -> RecipeOutcome.unsafe("the region expression is computed at run time."));

        MigrationResult result = run(unsafe);

        ItemResult region = result(result, ItemKind.METHOD_INVOCATION, V2Stubs.BUILDER_REGION);
        assertEquals(Disposition.UNRESOLVED, region.disposition());
        assertEquals("builder.region", region.recipe());
        MigrationDiagnostic d = region.diagnostic();
        assertEquals(V2Stubs.BUILDER_REGION, d.entryId());
        assertEquals("region(Region.of(region))", d.construct());
        assertEquals(APP_PATH + ":17:35", d.location().toString());
        assertEquals("Recipe builder.region judged the transformation unsafe: the region expression is computed at "
                     + "run time.", d.reason());
        assertTrue(result.changed().isEmpty());
    }

    @Test
    void run_editOfAnUnresolvedNestedConstruct_isRejectedAndBothStayUnchanged() throws Exception {
        // The outer call's edit replaces its argument, which holds an unresolved inner call.
        MigrationRecipe outer = forEntry("builder.region", V2Stubs.BUILDER_REGION, ItemKind.METHOD_INVOCATION,
                                         c -> RecipeOutcome.transform("Rewrite.", List.of(
                                             TextEdit.replace(c.item().location(), "region(Region.US_WEST_2)"))));
        MigrationRecipe inner = forEntry("region.of", V2Stubs.REGION_OF, ItemKind.METHOD_INVOCATION,
                                         c -> RecipeOutcome.unsafe("unknown region IDs are rejected in V3."));

        MigrationResult result = run(outer, inner);

        ItemResult outerResult = result(result, ItemKind.METHOD_INVOCATION, V2Stubs.BUILDER_REGION);
        assertEquals(Disposition.UNRESOLVED, outerResult.disposition());
        assertTrue(outerResult.reason().startsWith("Recipe builder.region proposed a transformation, but its edit "
                                                   + "would change the unresolved construct at " + APP_PATH + ":17:49"),
                   outerResult.reason());
        assertEquals(Disposition.UNRESOLVED, result(result, ItemKind.METHOD_INVOCATION, V2Stubs.REGION_OF)
            .disposition());
        assertTrue(result.changed().isEmpty());
    }

    @Test
    void run_overlappingEditsOfTwoItems_unresolveBoth() throws Exception {
        String hookId = V2Stubs.BEFORE_EXECUTION;
        String attributes = EntryKind.typeId("software.amazon.awssdk.core.interceptor.ExecutionAttributes");
        MigrationRecipe hook = forEntry("hook", hookId, ItemKind.METHOD_OVERRIDE,
                                        c -> RecipeOutcome.transform("Rewrite hook.", List.of(
                                            TextEdit.replace(c.item().location(), "// hook"))));
        MigrationRecipe type = forEntry("type", attributes, ItemKind.TYPE_REFERENCE,
                                        c -> RecipeOutcome.transform("Rename type.", List.of(
                                            TextEdit.replace(c.item().location(), "ExecutionContext"))));

        MigrationResult result = run(hook, type);

        assertEquals(Disposition.UNRESOLVED, result(result, ItemKind.METHOD_OVERRIDE, hookId).disposition());
        ItemResult typeResult = result(result, ItemKind.TYPE_REFERENCE, attributes);
        assertEquals(Disposition.UNRESOLVED, typeResult.disposition());
        assertTrue(typeResult.reason().contains("its edits overlap those of the construct at " + APP_PATH + ":11:5"),
                   typeResult.reason());
        assertTrue(result.changed().isEmpty());
    }

    @Test
    void run_editOutsideTheItemOrInANonEditableFile_isRejected() throws Exception {
        MigrationRecipe outside = forEntry("outside", V2Stubs.BUILDER_BUILD, ItemKind.METHOD_INVOCATION,
                                           c -> RecipeOutcome.transform("Too wide.", List.of(
                                               new TextEdit(APP_PATH, 0, 7, "module"))));
        MigrationRecipe elsewhere = forEntry("elsewhere", V2Stubs.S3_BUILDER, ItemKind.METHOD_INVOCATION,
                                             c -> RecipeOutcome.transform("Other file.", List.of(
                                                 TextEdit.insert("pom.xml", 0, "<!-- -->"))));

        MigrationResult result = run(outside, elsewhere);

        assertTrue(result(result, ItemKind.METHOD_INVOCATION, V2Stubs.BUILDER_BUILD).reason()
                       .endsWith("replaces text outside the item's own range."));
        assertTrue(result(result, ItemKind.METHOD_INVOCATION, V2Stubs.S3_BUILDER).reason()
                       .endsWith("its edit targets pom.xml, which the tool may not edit."));
        assertTrue(result.changed().isEmpty());
    }

    @Test
    void run_identicalInsertionsFromTwoItems_areAppliedOnce() throws Exception {
        int importOffset = APP.indexOf("import software.amazon.awssdk.core.interceptor.Context;");
        MigrationRecipe imports = recipe("retry.import", c -> V2Stubs.DEFAULT_RETRY_POLICY.equals(c.item().id())
            ? RecipeOutcome.transform("Import the strategy.", List.of(TextEdit.insert(APP_PATH, importOffset,
                                                                                      IMPORT_LINE)))
            : RecipeOutcome.notApplicable());

        MigrationResult result = run(imports);

        assertEquals(Disposition.TRANSFORMED, result(result, ItemKind.STATIC_IMPORT, V2Stubs.DEFAULT_RETRY_POLICY)
            .disposition());
        assertEquals(Disposition.TRANSFORMED, result(result, ItemKind.METHOD_INVOCATION, V2Stubs.DEFAULT_RETRY_POLICY)
            .disposition());
        String changed = result.changed().get(APP_PATH);
        assertEquals(APP.substring(0, importOffset) + IMPORT_LINE + APP.substring(importOffset), changed);
    }

    @Test
    void run_disagreeingOrFailingRecipes_leaveTheItemUnresolved() throws Exception {
        MigrationRecipe first = forEntry("a.first", V2Stubs.BUILDER_BUILD, ItemKind.METHOD_INVOCATION,
                                         c -> RecipeOutcome.transform("One.", List.of(
                                             TextEdit.replace(c.item().location(), "build1()"))));
        MigrationRecipe second = forEntry("b.second", V2Stubs.BUILDER_BUILD, ItemKind.METHOD_INVOCATION,
                                          c -> RecipeOutcome.preserve("Already V3."));
        MigrationRecipe failing = forEntry("c.failing", V2Stubs.S3_BUILDER, ItemKind.METHOD_INVOCATION, c -> {
            throw new IllegalStateException("boom");
        });

        MigrationResult result = run(second, failing, first);

        ItemResult build = result(result, ItemKind.METHOD_INVOCATION, V2Stubs.BUILDER_BUILD);
        assertEquals(Disposition.UNRESOLVED, build.disposition());
        assertEquals("Recipes disagree on this construct (a.first: transform, b.second: preserve), so it is left "
                     + "unchanged.", build.reason());
        ItemResult builder = result(result, ItemKind.METHOD_INVOCATION, V2Stubs.S3_BUILDER);
        assertEquals(Disposition.UNRESOLVED, builder.disposition());
        assertEquals("Recipe c.failing judged the transformation unsafe: the recipe failed (IllegalStateException: "
                     + "boom)", builder.reason());
        assertEquals(List.of("a.first", "b.second", "c.failing"),
                     result.recipes().stream().map(MigrationRecipe::id).toList());
    }

    @Test
    void run_preserveAnswerForAnAdaptedEntry_preservesIt() throws Exception {
        MigrationRecipe preserve = forEntry("hooks.execution", V2Stubs.BEFORE_EXECUTION, ItemKind.METHOD_OVERRIDE,
                                            c -> RecipeOutcome.preserve("beforeExecution keeps its frequency."));

        ItemResult hook = result(run(preserve), ItemKind.METHOD_OVERRIDE, V2Stubs.BEFORE_EXECUTION);

        assertEquals(Disposition.PRESERVED, hook.disposition());
        assertEquals("hooks.execution", hook.recipe());
        assertEquals("beforeExecution keeps its frequency.", hook.reason());
    }

    @Test
    void run_duplicateRecipeIds_areRejected() {
        MigrationRecipe a = recipe("same", c -> RecipeOutcome.notApplicable());
        MigrationRecipe b = recipe("same", c -> RecipeOutcome.notApplicable());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> run(a, b));

        assertEquals("Two recipes have the ID same", e.getMessage());
    }

    @Test
    void run_everyItem_hasExactlyOneDispositionAndDiagnosticsOnlyWhenUnresolved() throws Exception {
        MigrationResult result = run(forEntry("retry.rename", V2Stubs.DEFAULT_RETRY_POLICY, ItemKind.METHOD_INVOCATION,
                                              c -> RecipeOutcome.transform("Rename.", List.of(
                                                  TextEdit.replace(c.item().location(), "x()")))));

        assertEquals(result.items().size(), result.count(Disposition.PRESERVED) + result.count(Disposition.TRANSFORMED)
                                            + result.count(Disposition.UNRESOLVED));
        assertEquals(result.count(Disposition.UNRESOLVED), result.diagnostics().size());
        for (ItemResult r : result.items()) {
            assertEquals(r.disposition() == Disposition.UNRESOLVED, r.diagnostic() != null, r.toString());
            assertEquals(r.disposition() == Disposition.TRANSFORMED, !r.edits().isEmpty(), r.toString());
        }
    }

    private MigrationResult run(MigrationRecipe... recipes) throws Exception {
        return MigrationEngine.run(Fixtures.app(project, stubs, List.of(recipes)).build());
    }
}
