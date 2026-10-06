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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.v2migration.Entries.excluded;
import static software.amazon.awssdk.v2migration.Entries.preserved;
import static software.amazon.awssdk.v2migration.Fixtures.forEntry;
import static software.amazon.awssdk.v2migration.Fixtures.result;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.v2migration.manifest.EntryKind;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;
import software.amazon.awssdk.v2migration.pom.ProjectDependencies;
import software.amazon.awssdk.v2migration.pom.XmlElement;
import software.amazon.awssdk.v2migration.recipe.MigrationRecipe;
import software.amazon.awssdk.v2migration.recipe.RecipeOutcome;

/** The dependency and BOM inventory and POM edits. */
class PomMigrationTest {

    private static final String POM = String.join("\n",
        "<project>",
        "  <groupId>com.example</groupId>",
        "  <artifactId>app</artifactId>",
        "  <version>1.0</version>",
        "  <dependencyManagement>",
        "    <dependencies>",
        "      <dependency>",
        "        <groupId>software.amazon.awssdk</groupId>",
        "        <artifactId>bom</artifactId>",
        "        <version>2.25.0</version>",
        "        <type>pom</type>",
        "        <scope>import</scope>",
        "      </dependency>",
        "    </dependencies>",
        "  </dependencyManagement>",
        "  <dependencies>",
        "    <dependency>",
        "      <groupId>software.amazon.awssdk</groupId>",
        "      <artifactId>s3</artifactId>",
        "    </dependency>",
        "    <dependency>",
        "      <groupId>software.amazon.awssdk</groupId>",
        "      <artifactId>apache-client</artifactId>",
        "      <version>3.0.0</version>",
        "    </dependency>",
        "  </dependencies>",
        "</project>",
        "");

    private static final String BOM = EntryKind.artifactId("software.amazon.awssdk", "bom");
    private static final String S3 = EntryKind.artifactId("software.amazon.awssdk", "s3");
    private static final String SDK_CORE = EntryKind.artifactId("software.amazon.awssdk", "sdk-core");
    private static final String LEGACY = EntryKind.artifactId("software.amazon.awssdk", "legacy-signer");

    @TempDir
    Path project;

    @Test
    void run_standalonePom_inventoriesV2DeclarationsAndSkipsV3Ones() throws Exception {
        MigrationResult result = run(null, List.of());

        assertEquals(List.of("BOM_IMPORT " + BOM, "DEPENDENCY " + S3),
                     result.items().stream().map(r -> r.item().kind() + " " + r.item().id()).toList());
        ItemResult bom = result(result, ItemKind.BOM_IMPORT, BOM);
        assertEquals("software.amazon.awssdk:bom:2.25.0 (dependencyManagement)", bom.item().construct());
        assertEquals("pom.xml:7:7", bom.item().location().toString());
        assertEquals(MigrationCategory.BOM, bom.item().category());
        ItemResult s3 = result(result, ItemKind.DEPENDENCY, S3);
        assertEquals("software.amazon.awssdk:s3:2.25.0", s3.item().construct());
        assertEquals(Disposition.PRESERVED, s3.disposition());
    }

    @Test
    void run_effectiveGraph_givesEveryUndeclaredV2ArtifactADisposition() throws Exception {
        ProjectDependencies effective = new ProjectDependencies(
            List.of(new ProjectDependencies.Declared("dependencies", 0, "software.amazon.awssdk", "s3", null, "2.25.0",
                                                     null, null, null),
                    new ProjectDependencies.Declared("dependencyManagement", 0, "software.amazon.awssdk", "bom",
                                                     "2.25.0", "2.25.0", "pom", null, "import")),
            List.of(new ProjectDependencies.Resolved("software.amazon.awssdk", "s3", "2.25.0", "compile",
                                                     List.of("software.amazon.awssdk:s3")),
                    new ProjectDependencies.Resolved("software.amazon.awssdk", "sdk-core", "2.25.0", "compile",
                                                     List.of("software.amazon.awssdk:s3",
                                                             "software.amazon.awssdk:sdk-core")),
                    new ProjectDependencies.Resolved("software.amazon.awssdk", "legacy-signer", "2.25.0", "compile",
                                                     List.of("software.amazon.awssdk:s3",
                                                             "software.amazon.awssdk:legacy-signer")),
                    new ProjectDependencies.Resolved("org.slf4j", "slf4j-api", "1.7.36", "compile", List.of())));

        MigrationResult result = run(effective, List.of());

        ItemResult sdkCore = result(result, ItemKind.EFFECTIVE_DEPENDENCY, SDK_CORE);
        assertEquals(Disposition.PRESERVED, sdkCore.disposition());
        assertEquals("software.amazon.awssdk:sdk-core:2.25.0 via software.amazon.awssdk:s3 -> "
                     + "software.amazon.awssdk:sdk-core", sdkCore.item().construct());
        // Located, as a position, at the declaration it arrives through.
        assertTrue(sdkCore.item().location().isPosition());
        assertEquals(result(result, ItemKind.DEPENDENCY, S3).item().location().startOffset(),
                     sdkCore.item().location().startOffset());
        ItemResult legacy = result(result, ItemKind.EFFECTIVE_DEPENDENCY, LEGACY);
        assertEquals(Disposition.UNRESOLVED, legacy.disposition());
        assertEquals("pom.xml:17:5", legacy.diagnostic().location().toString());
        assertEquals(4, result.items().size());
    }

    @Test
    void run_versionRecipe_editsOnlyTheVersionText() throws Exception {
        MigrationRecipe version = forEntry("dependencies.bom", BOM, ItemKind.BOM_IMPORT, c -> {
            XmlElement v = c.pom().orElseThrow().dependencyElement().orElseThrow().child("version").orElseThrow();
            return RecipeOutcome.transform("Use the V3 BOM.", List.of(
                new TextEdit("pom.xml", v.contentStart(), v.contentEnd(), "3.0.0")));
        });

        MigrationResult result = run(null, List.of(version));

        assertEquals(Disposition.TRANSFORMED, result(result, ItemKind.BOM_IMPORT, BOM).disposition());
        assertEquals(POM.replace("<version>2.25.0</version>", "<version>3.0.0</version>"),
                     result.changed().get("pom.xml"));
    }

    private MigrationResult run(ProjectDependencies dependencies, List<MigrationRecipe> recipes) throws Exception {
        Fixtures.write(project, "pom.xml", POM);
        MigrationManifest manifest = Entries.manifest(preserved(BOM), preserved(S3), preserved(SDK_CORE),
                                                      excluded(LEGACY, null, "Remove the dependency."));
        return MigrationEngine.run(MigrationRequest.builder().projectRoot(project).pomFile(Path.of("pom.xml"))
                                                   .dependencies(dependencies).manifest(manifest).recipes(recipes)
                                                   .build());
    }
}
