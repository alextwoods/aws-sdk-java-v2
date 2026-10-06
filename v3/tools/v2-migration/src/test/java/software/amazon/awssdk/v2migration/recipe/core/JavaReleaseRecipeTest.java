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

package software.amazon.awssdk.v2migration.recipe.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.CANDIDATE;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.all;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.artifact;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.assertDecided;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.manifest;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.migratePom;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.migrated;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.preserved;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.v2migration.Disposition;
import software.amazon.awssdk.v2migration.ItemKind;
import software.amazon.awssdk.v2migration.ItemResult;
import software.amazon.awssdk.v2migration.MigrationCategory;
import software.amazon.awssdk.v2migration.MigrationResult;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;

/** {@link JavaReleaseRecipe} and the build-setting inventory it decides. */
class JavaReleaseRecipeTest {

    private static final String RECIPE = "build.java-release";
    private static final MigrationManifest MANIFEST = manifest(CANDIDATE, preserved(artifact("s3")));
    private static final String S3 = "  <dependencies>\n    <dependency>\n      <groupId>software.amazon.awssdk</groupId>\n"
                                     + "      <artifactId>s3</artifactId>\n      <version>3.0.0</version>\n"
                                     + "    </dependency>\n  </dependencies>\n";

    @TempDir
    Path project;

    @Test
    void visit_levelsBelowTheMinimum_areRaisedTo21AndOthersPreserved() throws Exception {
        String pom = pom("    <maven.compiler.source>1.8</maven.compiler.source>\n"
                         + "    <maven.compiler.target>11</maven.compiler.target>\n"
                         + "    <maven.compiler.testRelease>25</maven.compiler.testRelease>\n",
                         "<release>17</release>", v2Dependency());

        MigrationResult result = migratePom(project, pom, MANIFEST);

        List<ItemResult> settings = all(result, ItemKind.BUILD_SETTING, null);
        assertEquals(4, settings.size(), settings::toString);
        settings.forEach(s -> assertEquals(MigrationCategory.BUILD, s.item().category()));
        assertEquals(List.of(Disposition.TRANSFORMED, Disposition.TRANSFORMED, Disposition.PRESERVED,
                             Disposition.TRANSFORMED),
                     settings.stream().map(ItemResult::disposition).toList());
        settings.forEach(s -> assertEquals(RECIPE, s.recipe()));
        assertEquals(withV3Dependency(pom).replace(">1.8<", ">21<").replace(">11<", ">21<").replace(">17<", ">21<"),
                     migrated(result, "pom.xml"));
    }

    @Test
    void visit_expressionOrUnknownLevel_isUnresolvedAndUnchanged() throws Exception {
        String pom = pom("    <maven.compiler.release>${java.version}</maven.compiler.release>\n",
                         "<source>eight</source>", v2Dependency());

        MigrationResult result = migratePom(project, pom, MANIFEST);

        List<ItemResult> settings = all(result, ItemKind.BUILD_SETTING, null);
        assertEquals(2, settings.size());
        settings.forEach(s -> assertDecided(s, Disposition.UNRESOLVED, RECIPE));
        assertTrue(settings.get(0).reason().contains("set the property it references to 21"), settings.get(0).reason());
        assertTrue(settings.get(1).diagnostic().message().contains("not a recognized Java version"),
                   settings.get(1).diagnostic().message());
        // Only the dependency (decided by its own recipe) changes; the build settings are left byte-for-byte.
        assertEquals(withV3Dependency(pom), migrated(result, "pom.xml"));
    }

    @Test
    void inventory_projectWithoutV2Artifacts_hasNoBuildSettings() throws Exception {
        String pom = pom("    <maven.compiler.source>1.8</maven.compiler.source>\n", "<release>8</release>", S3);

        MigrationResult result = migratePom(project, pom, MANIFEST);

        assertEquals(List.of(), result.items());
    }

    private static String v2Dependency() {
        return S3.replace("3.0.0", "2.25.0");
    }

    /** {@code pom} with its V2 dependency moved to V3, as recipe dependencies.v3-coordinates does. */
    private static String withV3Dependency(String pom) {
        return pom.replace("<version>2.25.0</version>", "<version>3.0.0</version>");
    }

    private static String pom(String properties, String compilerConfiguration, String dependencies) {
        return "<project>\n"
               + "  <groupId>com.example</groupId>\n"
               + "  <artifactId>app</artifactId>\n"
               + "  <version>1.0</version>\n"
               + "  <properties>\n" + properties + "  </properties>\n"
               + dependencies
               + "  <build>\n    <plugins>\n      <plugin>\n"
               + "        <artifactId>maven-compiler-plugin</artifactId>\n"
               + "        <configuration>" + compilerConfiguration + "</configuration>\n"
               + "      </plugin>\n      <plugin>\n"
               + "        <artifactId>maven-surefire-plugin</artifactId>\n"
               + "        <configuration><release>8</release></configuration>\n"
               + "      </plugin>\n    </plugins>\n  </build>\n"
               + "</project>\n";
    }
}
