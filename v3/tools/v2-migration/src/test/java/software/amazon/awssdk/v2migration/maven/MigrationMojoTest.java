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

package software.amazon.awssdk.v2migration.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.v2migration.MigrationRequest;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;
import software.amazon.awssdk.v2migration.pom.ProjectDependencies;
import software.amazon.awssdk.v2migration.recipe.Recipes;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/** The Maven adapter (effective model, graph, roots, class path) and the migrate and dry-run goals. */
class MigrationMojoTest {

    private static final String SAMPLE = "/software/amazon/awssdk/v2migration/manifest/sample-parity-manifest.json";
    private static final String APP = String.join("\n",
        "package com.example;",
        "import software.amazon.awssdk.regions.Region;",
        "class App { Region region; }",
        "");
    private static final String POM = String.join("\n",
        "<project>",
        "  <groupId>com.example</groupId>",
        "  <artifactId>app</artifactId>",
        "  <version>1.0</version>",
        "  <dependencyManagement><dependencies>",
        "    <dependency><groupId>software.amazon.awssdk</groupId><artifactId>bom</artifactId>"
        + "<version>${awssdk.version}</version><type>pom</type><scope>import</scope></dependency>",
        "  </dependencies></dependencyManagement>",
        "  <dependencies>",
        "    <dependency><groupId>software.amazon.awssdk</groupId><artifactId>s3</artifactId></dependency>",
        "  </dependencies>",
        "</project>",
        "");

    @TempDir
    Path root;
    private MavenProject project;
    private File manifest;

    @BeforeEach
    void createProject() throws Exception {
        write("pom.xml", POM);
        write("src/main/java/com/example/App.java", APP);
        write("target/generated-sources/gen/com/example/Generated.java", "package com.example; class Generated {}\n");
        write("src/test/java/com/example/AppTest.java", "package com.example; class AppTest {}\n");
        Files.createDirectories(root.resolve("lib"));
        try (InputStream in = MigrationMojoTest.class.getResourceAsStream(SAMPLE)) {
            manifest = write("manifest.json", new String(in.readAllBytes(), StandardCharsets.UTF_8)).toFile();
        }

        Model original = new Model();
        original.setGroupId("com.example");
        original.setArtifactId("app");
        original.setVersion("1.0");
        original.addDependency(dependency("s3", null, null, null));
        original.setDependencyManagement(new DependencyManagement());
        original.getDependencyManagement().addDependency(dependency("bom", "${awssdk.version}", "pom", "import"));

        Model effective = original.clone();
        effective.getProperties().setProperty("awssdk.version", "2.25.0");
        effective.getDependencies().get(0).setVersion("2.25.0");
        effective.setDependencyManagement(new DependencyManagement());
        effective.getDependencyManagement().addDependency(dependency("s3", "2.25.0", null, null));
        Build build = new Build();
        build.setDirectory(root.resolve("target").toString());
        build.setOutputDirectory(root.resolve("target/classes").toString());
        build.setTestOutputDirectory(root.resolve("target/test-classes").toString());
        effective.setBuild(build);

        project = new MavenProject(effective);
        project.setOriginalModel(original);
        project.setFile(root.resolve("pom.xml").toFile());
        project.addCompileSourceRoot(root.resolve("src/main/java").toString());
        project.addCompileSourceRoot(root.resolve("target/generated-sources/gen").toString());
        project.addTestCompileSourceRoot(root.resolve("src/test/java").toString());
        Set<Artifact> artifacts = new LinkedHashSet<>();
        artifacts.add(artifact("s3", List.of("com.example:app:jar:1.0", "software.amazon.awssdk:s3:jar:2.25.0")));
        artifacts.add(artifact("sdk-core", List.of("com.example:app:jar:1.0", "software.amazon.awssdk:s3:jar:2.25.0",
                                                   "software.amazon.awssdk:sdk-core:jar:2.25.0")));
        project.setArtifacts(artifacts);
    }

    @Test
    void dependencies_mavenProject_usesTheEffectiveModelAndGraph() {
        ProjectDependencies dependencies = MavenProjects.dependencies(project);

        assertEquals(List.of(
            new ProjectDependencies.Declared("dependencies", 0, "software.amazon.awssdk", "s3", null, "2.25.0", "jar",
                                             null, null),
            new ProjectDependencies.Declared("dependencyManagement", 0, "software.amazon.awssdk", "bom",
                                             "${awssdk.version}", "2.25.0", "pom", null, "import")),
                     dependencies.declared());
        assertEquals(List.of(
            new ProjectDependencies.Resolved("software.amazon.awssdk", "s3", "2.25.0", "compile",
                                             List.of("software.amazon.awssdk:s3")),
            new ProjectDependencies.Resolved("software.amazon.awssdk", "sdk-core", "2.25.0", "compile",
                                             List.of("software.amazon.awssdk:s3", "software.amazon.awssdk:sdk-core"))),
                     dependencies.resolved());
    }

    @Test
    void request_mavenProject_inventoriesProjectSourcesAndAttributesAgainstDependencies() throws Exception {
        MigrationRequest request = MavenProjects.request(project, MigrationManifest.read(manifest.toPath()), List.of(),
                                                         true, "UTF-8");

        assertEquals(List.of(root.resolve("src/main/java/com/example/App.java"),
                             root.resolve("src/test/java/com/example/AppTest.java")), request.sources());
        assertEquals(List.of(root.resolve("target/generated-sources/gen/com/example/Generated.java")),
                     request.contextSources());
        // The project's own output directories are excluded; dependency files remain.
        assertEquals(List.of(root.resolve("lib")), request.classpath());
        assertEquals(root.resolve("pom.xml"), request.pomFile());
        assertEquals("com.example:app", request.project());
    }

    @Test
    void migrate_project_writesTheReportAndLeavesUnresolvedSourceUnchanged() throws Exception {
        MigrateMojo mojo = configure(new MigrateMojo());

        mojo.execute();

        ObjectNode report = Node.parse(Files.readString(mojo.report.toPath(), StandardCharsets.UTF_8))
                                .expectObjectNode();
        assertTrue(report.expectBooleanMember("applied").getValue());
        assertEquals("com.example:app", report.expectStringMember("project").getValue());
        List<String> ids = report.expectArrayMember("items").getElementsAs(ObjectNode.class).stream()
                                 .map(i -> i.expectMember("id")).filter(Node::isStringNode)
                                 .map(n -> n.expectStringNode().getValue()).toList();
        assertTrue(ids.contains("api:software.amazon.awssdk.regions.Region"), ids.toString());
        assertTrue(ids.contains("artifact:software.amazon.awssdk:bom"), ids.toString());
        assertTrue(ids.contains("artifact:software.amazon.awssdk:sdk-core"), ids.toString());
        assertEquals(APP, Files.readString(root.resolve("src/main/java/com/example/App.java"), StandardCharsets.UTF_8));
        assertEquals(POM, Files.readString(root.resolve("pom.xml"), StandardCharsets.UTF_8));
    }

    @Test
    void dryRun_unresolvedItemsWithFailOnUnresolved_failsTheBuild() throws Exception {
        DryRunMojo mojo = configure(new DryRunMojo());
        mojo.failOnUnresolved = true;

        MojoFailureException e = assertThrows(MojoFailureException.class, mojo::execute);

        assertTrue(e.getMessage().contains("V2 constructs of com.example:app:jar:1.0 are unresolved"), e.getMessage());
        ObjectNode report = Node.parse(Files.readString(mojo.report.toPath(), StandardCharsets.UTF_8))
                                .expectObjectNode();
        assertFalse(report.expectBooleanMember("applied").getValue());
    }

    @Test
    void execute_withoutAManifest_explainsHowToProvideOne() {
        MigrateMojo mojo = configure(new MigrateMojo());
        mojo.manifest = null;

        MojoExecutionException e = assertThrows(MojoExecutionException.class, mojo::execute);

        assertTrue(e.getMessage().contains("set v2migration.manifest"), e.getMessage());
    }

    @Test
    void discover_pluginClassLoader_findsNoRecipesUntilRecipeFamiliesRegister() {
        assertEquals(List.of(), Recipes.discover(AbstractMigrationMojo.class.getClassLoader()));
    }

    private <T extends AbstractMigrationMojo> T configure(T mojo) {
        mojo.project = project;
        mojo.manifest = manifest;
        mojo.report = root.resolve("target/v2-migration/migration-report.json").toFile();
        mojo.includeTestSources = true;
        mojo.encoding = "UTF-8";
        return mojo;
    }

    private Artifact artifact(String artifactId, List<String> trail) {
        DefaultArtifactHandler handler = new DefaultArtifactHandler("jar");
        handler.setAddedToClasspath(true);
        DefaultArtifact artifact = new DefaultArtifact("software.amazon.awssdk", artifactId, "2.25.0", "compile", "jar",
                                                       null, handler);
        artifact.setFile(root.resolve("lib").toFile());
        artifact.setDependencyTrail(trail);
        return artifact;
    }

    private static Dependency dependency(String artifactId, String version, String type, String scope) {
        Dependency d = new Dependency();
        d.setGroupId("software.amazon.awssdk");
        d.setArtifactId(artifactId);
        d.setVersion(version);
        if (type != null) {
            d.setType(type);
        }
        d.setScope(scope);
        return d;
    }

    private Path write(String path, String content) throws Exception {
        Path file = root.resolve(path);
        Files.createDirectories(file.getParent());
        return Files.writeString(file, content, StandardCharsets.UTF_8);
    }
}
