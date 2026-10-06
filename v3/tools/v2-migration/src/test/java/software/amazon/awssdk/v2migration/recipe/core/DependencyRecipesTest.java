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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.BASELINE_ONLY;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.CANDIDATE;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.artifact;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.assertDecided;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.excluded;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.manifest;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.migratePom;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.migrated;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.only;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.preserved;
import static software.amazon.awssdk.v2migration.recipe.core.CoreFixtures.renamed;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.v2migration.Disposition;
import software.amazon.awssdk.v2migration.ItemKind;
import software.amazon.awssdk.v2migration.ItemResult;
import software.amazon.awssdk.v2migration.MigrationCategory;
import software.amazon.awssdk.v2migration.MigrationEngine;
import software.amazon.awssdk.v2migration.MigrationRequest;
import software.amazon.awssdk.v2migration.MigrationResult;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;
import software.amazon.awssdk.v2migration.pom.ProjectDependencies;

/** {@link DependencyCoordinatesRecipe} and {@link EffectiveDependencyRecipe}. */
class DependencyRecipesTest {

    private static final String COORDINATES = "dependencies.v3-coordinates";
    private static final String EFFECTIVE = "dependencies.effective-graph";

    private static final MigrationManifest MANIFEST = manifest(
        CANDIDATE, preserved(artifact("bom")), preserved(artifact("s3")), preserved(artifact("dynamodb")),
        preserved(artifact("sdk-core")), renamed(artifact("old-client"), artifact("new-client")),
        excluded(artifact("legacy-signer"), null, "Remove the dependency."));

    @TempDir
    Path project;

    @Test
    void visit_literalVersions_setsTheCandidateVersionAndKeepsFormatting() throws Exception {
        String pom = pom("",
                         managed(dependency("bom", "2.25.0", "<type>pom</type><scope>import</scope>")),
                         dependency("s3", null, "") + dependency("dynamodb", " 2.20.1 ", ""));

        MigrationResult result = migratePom(project, pom, MANIFEST);

        assertDecided(only(result, ItemKind.BOM_IMPORT, artifact("bom")), Disposition.TRANSFORMED, COORDINATES);
        assertDecided(only(result, ItemKind.DEPENDENCY, artifact("dynamodb")), Disposition.TRANSFORMED, COORDINATES);
        ItemResult s3 = only(result, ItemKind.DEPENDENCY, artifact("s3"));
        assertDecided(s3, Disposition.PRESERVED, COORDINATES);
        assertTrue(s3.reason().contains("managed by the software.amazon.awssdk:bom import"), s3.reason());
        assertEquals(pom.replace("<version>2.25.0</version>", "<version>3.0.0</version>")
                        .replace("<version> 2.20.1 </version>", "<version> 3.0.0 </version>"),
                     migrated(result, "pom.xml"));
        assertFalse(result.hasUnresolved());
    }

    @Test
    void visit_versionProperty_rewritesThePropertyOnceAndPreservesItsUsers() throws Exception {
        String pom = pom("<properties>\n    <aws.version>2.25.0</aws.version>\n  </properties>\n",
                         "",
                         dependency("s3", "${aws.version}", "") + dependency("dynamodb", "${aws.version}", ""));

        MigrationResult result = migratePom(project, pom, MANIFEST);

        ItemResult property = only(result, ItemKind.VERSION_PROPERTY, null);
        assertDecided(property, Disposition.TRANSFORMED, COORDINATES);
        assertEquals(MigrationCategory.DEPENDENCY, property.item().category());
        assertDecided(only(result, ItemKind.DEPENDENCY, artifact("s3")), Disposition.PRESERVED, COORDINATES);
        assertDecided(only(result, ItemKind.DEPENDENCY, artifact("dynamodb")), Disposition.PRESERVED, COORDINATES);
        assertEquals(pom.replace("<aws.version>2.25.0</aws.version>", "<aws.version>3.0.0</aws.version>"),
                     migrated(result, "pom.xml"));
    }

    @Test
    void visit_propertySharedWithNonSdkArtifact_leavesEverythingUnchanged() throws Exception {
        String other = "    <dependency>\n      <groupId>com.example</groupId>\n      <artifactId>lib</artifactId>\n"
                       + "      <version>${aws.version}</version>\n    </dependency>\n";
        String pom = pom("<properties>\n    <aws.version>2.25.0</aws.version>\n  </properties>\n",
                         "", dependency("s3", "${aws.version}", "") + other);

        MigrationResult result = migratePom(project, pom, MANIFEST);

        ItemResult property = only(result, ItemKind.VERSION_PROPERTY, null);
        assertDecided(property, Disposition.UNRESOLVED, COORDINATES);
        assertTrue(property.reason().contains("com.example:lib, which is not an SDK artifact"), property.reason());
        ItemResult s3 = only(result, ItemKind.DEPENDENCY, artifact("s3"));
        assertDecided(s3, Disposition.UNRESOLVED, COORDINATES);
        assertTrue(s3.reason().contains("${aws.version}"), s3.reason());
        assertNull(result.changed().get("pom.xml"));
    }

    @Test
    void visit_versionFromUndefinedProperty_isUnresolved() throws Exception {
        String pom = pom("", "", dependency("s3", "${parent.aws.version}", ""));

        MigrationResult result = migratePom(project, pom, MANIFEST);

        ItemResult s3 = only(result, ItemKind.DEPENDENCY, artifact("s3"));
        assertDecided(s3, Disposition.UNRESOLVED, COORDINATES);
        assertTrue(s3.reason().contains("which this POM does not define"), s3.reason());
        assertEquals("pom.xml:6:5", s3.diagnostic().location().toString());
    }

    @Test
    void visit_versionManagedOutsideThePom_isUnresolved() throws Exception {
        String pom = pom("", "", dependency("s3", null, ""));
        ProjectDependencies effective = new ProjectDependencies(
            List.of(new ProjectDependencies.Declared("dependencies", 0, "software.amazon.awssdk", "s3", null, "2.25.0",
                                                     null, null, null)),
            List.of());

        MigrationResult result = run(pom, effective);

        ItemResult s3 = only(result, ItemKind.DEPENDENCY, artifact("s3"));
        assertDecided(s3, Disposition.UNRESOLVED, COORDINATES);
        assertTrue(s3.reason().contains("managed outside this POM"), s3.reason());
    }

    @Test
    void visit_managedDeclaration_isTransformedAndItsUserPreserved() throws Exception {
        String pom = pom("", managed(dependency("s3", "2.25.0", "")), dependency("s3", null, ""));

        MigrationResult result = migratePom(project, pom, MANIFEST);

        assertDecided(only(result, ItemKind.MANAGED_DEPENDENCY, artifact("s3")), Disposition.TRANSFORMED, COORDINATES);
        ItemResult user = only(result, ItemKind.DEPENDENCY, artifact("s3"));
        assertDecided(user, Disposition.PRESERVED, COORDINATES);
        assertTrue(user.reason().contains("dependencyManagement declaration"), user.reason());
    }

    @Test
    void visit_artifactReplacedByTheManifest_rewritesTheArtifactIdAndVersion() throws Exception {
        String pom = pom("", "", dependency("old-client", "2.25.0", ""));

        MigrationResult result = migratePom(project, pom, MANIFEST);

        assertDecided(only(result, ItemKind.DEPENDENCY, artifact("old-client")), Disposition.TRANSFORMED,
                      COORDINATES);
        assertEquals(pom.replace("<artifactId>old-client</artifactId>", "<artifactId>new-client</artifactId>")
                        .replace("<version>2.25.0</version>", "<version>3.0.0</version>"),
                     migrated(result, "pom.xml"));
    }

    @Test
    void visit_artifactWithoutAutomatedMigration_isLeftToTheManifestDiagnostic() throws Exception {
        String pom = pom("", "", dependency("legacy-signer", "2.25.0", "") + dependency("unknown", "2.25.0", ""));

        MigrationResult result = migratePom(project, pom, MANIFEST);

        ItemResult legacy = only(result, ItemKind.DEPENDENCY, artifact("legacy-signer"));
        assertDecided(legacy, Disposition.UNRESOLVED, null);
        assertEquals("Remove the dependency.", legacy.diagnostic().migration());
        assertDecided(only(result, ItemKind.DEPENDENCY, artifact("unknown")), Disposition.UNRESOLVED, null);
        assertNull(result.changed().get("pom.xml"));
    }

    @Test
    void visit_versionRange_isUnresolvedAndUnchanged() throws Exception {
        String pom = pom("", "", dependency("s3", "[2.20.0,2.26.0)", ""));

        MigrationResult result = migratePom(project, pom, MANIFEST);

        ItemResult s3 = only(result, ItemKind.DEPENDENCY, artifact("s3"));
        assertDecided(s3, Disposition.UNRESOLVED, COORDINATES);
        assertTrue(s3.reason().contains("not a literal 2.x version"), s3.reason());
        assertNull(result.changed().get("pom.xml"));
    }

    @Test
    void visit_baselineOnlyManifest_isUnresolvedBecauseTheV3VersionIsUnknown() throws Exception {
        String pom = pom("", "", dependency("s3", "2.25.0", ""));

        MigrationResult result = migratePom(project, pom, manifest(BASELINE_ONLY, preserved(artifact("s3"))));

        ItemResult s3 = only(result, ItemKind.DEPENDENCY, artifact("s3"));
        assertDecided(s3, Disposition.UNRESOLVED, COORDINATES);
        assertTrue(s3.reason().contains("no V3 release candidate version"), s3.reason());
    }

    @Test
    void visit_effectiveGraph_preservesSdkTrailsAndReportsThirdPartyAndInheritedOnes() throws Exception {
        String thirdParty = "    <dependency>\n      <groupId>com.example</groupId>\n      <artifactId>lib</artifactId>\n"
                            + "      <version>1.0</version>\n    </dependency>\n";
        String pom = pom("", "", dependency("s3", "2.25.0", "") + thirdParty);
        ProjectDependencies effective = new ProjectDependencies(
            List.of(new ProjectDependencies.Declared("dependencies", 0, "software.amazon.awssdk", "s3", "2.25.0",
                                                     "2.25.0", null, null, null),
                    new ProjectDependencies.Declared("dependencies", 1, "com.example", "lib", "1.0", "1.0", null,
                                                     null, null)),
            List.of(resolved("sdk-core", "software.amazon.awssdk:s3"),
                    resolved("dynamodb", "com.example:lib"),
                    resolved("bom", "com.example:parent-managed")));

        MigrationResult result = run(pom, effective);

        ItemResult viaSdk = only(result, ItemKind.EFFECTIVE_DEPENDENCY, artifact("sdk-core"));
        assertDecided(viaSdk, Disposition.PRESERVED, EFFECTIVE);
        ItemResult viaLibrary = only(result, ItemKind.EFFECTIVE_DEPENDENCY, artifact("dynamodb"));
        assertDecided(viaLibrary, Disposition.UNRESOLVED, EFFECTIVE);
        assertTrue(viaLibrary.reason().contains("third-party dependency com.example:lib"), viaLibrary.reason());
        ItemResult inherited = only(result, ItemKind.EFFECTIVE_DEPENDENCY, artifact("bom"));
        assertDecided(inherited, Disposition.UNRESOLVED, EFFECTIVE);
        assertTrue(inherited.reason().contains("which this POM does not declare"), inherited.reason());
    }

    private MigrationResult run(String pom, ProjectDependencies dependencies) throws Exception {
        CoreFixtures.write(project, "pom.xml", pom);
        return MigrationEngine.run(MigrationRequest.builder().projectRoot(project).pomFile(Path.of("pom.xml"))
                                                   .dependencies(dependencies).manifest(MANIFEST)
                                                   .recipes(CoreFixtures.coreRecipes()).build());
    }

    private static ProjectDependencies.Resolved resolved(String artifactId, String through) {
        return new ProjectDependencies.Resolved("software.amazon.awssdk", artifactId, "2.25.0", "compile",
                                                List.of(through, "software.amazon.awssdk:" + artifactId));
    }

    /** A POM with optional {@code <properties>}, dependency management and dependencies. */
    static String pom(String properties, String management, String dependencies) {
        return "<project>\n"
               + "  <groupId>com.example</groupId>\n"
               + "  <artifactId>app</artifactId>\n"
               + "  <version>1.0</version>\n"
               + (properties.isEmpty() ? "" : "  " + properties)
               + management
               + "  <dependencies>\n"
               + dependencies
               + "  </dependencies>\n"
               + "</project>\n";
    }

    static String managed(String dependencies) {
        return "  <dependencyManagement>\n  <dependencies>\n" + dependencies + "  </dependencies>\n"
               + "  </dependencyManagement>\n";
    }

    /** One SDK {@code <dependency>}; {@code version} {@code null} omits the version element. */
    static String dependency(String artifactId, String version, String extra) {
        return "    <dependency>\n"
               + "      <groupId>software.amazon.awssdk</groupId>\n"
               + "      <artifactId>" + artifactId + "</artifactId>\n"
               + (version == null ? "" : "      <version>" + version + "</version>\n")
               + (extra.isEmpty() ? "" : "      " + extra + "\n")
               + "    </dependency>\n";
    }
}
