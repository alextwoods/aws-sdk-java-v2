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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import software.amazon.awssdk.v2migration.Disposition;
import software.amazon.awssdk.v2migration.MigrationDiagnostic;
import software.amazon.awssdk.v2migration.MigrationEngine;
import software.amazon.awssdk.v2migration.MigrationResult;
import software.amazon.awssdk.v2migration.manifest.ManifestFormatException;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;
import software.amazon.awssdk.v2migration.recipe.MigrationRecipe;
import software.amazon.awssdk.v2migration.recipe.Recipes;

/** Shared configuration and execution of the migration goals. */
abstract class AbstractMigrationMojo extends AbstractMojo {

    /** Where a Parity Manifest bundled with the plugin would live; used when {@link #manifest} is not set. */
    static final String BUNDLED_MANIFEST = "/META-INF/aws-sdk-java-v3/v2-migration/parity-manifest.json";

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    MavenProject project;

    /** The Parity Manifest JSON ({@code parity-manifest-1} schema); defaults to the manifest bundled with the plugin. */
    @Parameter(property = "v2migration.manifest")
    File manifest;

    /** Where the machine-readable migration report is written. */
    @Parameter(property = "v2migration.report",
               defaultValue = "${project.build.directory}/v2-migration/migration-report.json")
    File report;

    /** Fail the build when any encountered item is unresolved. */
    @Parameter(property = "v2migration.failOnUnresolved", defaultValue = "false")
    boolean failOnUnresolved;

    /** Also inventory and migrate test sources. */
    @Parameter(property = "v2migration.includeTestSources", defaultValue = "true")
    boolean includeTestSources;

    /** Skip the goal. */
    @Parameter(property = "v2migration.skip", defaultValue = "false")
    boolean skip;

    /** The encoding of the Java sources. */
    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    String encoding;

    /** True if the goal writes the transformed files. */
    abstract boolean applies();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping the V2 migration (v2migration.skip)");
            return;
        }
        MigrationResult result;
        try {
            List<MigrationRecipe> recipes = Recipes.discover(AbstractMigrationMojo.class.getClassLoader());
            result = MigrationEngine.run(MavenProjects.request(project, loadManifest(), recipes, includeTestSources,
                                                               encoding));
            if (applies()) {
                result.write();
            }
            Path reportFile = report.toPath();
            Files.createDirectories(reportFile.toAbsolutePath().getParent());
            Files.writeString(reportFile, result.reportJson(applies()), StandardCharsets.UTF_8);
        } catch (IOException | DependencyResolutionRequiredException | RuntimeException e) {
            throw new MojoExecutionException("V2 migration of " + project.getId() + " failed: " + e.getMessage(), e);
        }
        for (MigrationDiagnostic diagnostic : result.diagnostics()) {
            getLog().warn(diagnostic.message());
        }
        getLog().info(String.format("V2 migration of %s: %d items, %d preserved, %d transformed, %d unresolved; "
                                    + "%d files %s; report %s",
                                    project.getId(), result.items().size(), result.count(Disposition.PRESERVED),
                                    result.count(Disposition.TRANSFORMED), result.count(Disposition.UNRESOLVED),
                                    result.changed().size(), applies() ? "changed" : "would change", report));
        if (failOnUnresolved && result.hasUnresolved()) {
            throw new MojoFailureException(result.count(Disposition.UNRESOLVED) + " V2 constructs of "
                                           + project.getId() + " are unresolved; see " + report);
        }
    }

    private MigrationManifest loadManifest() throws IOException, MojoExecutionException {
        try {
            if (manifest != null) {
                return MigrationManifest.read(manifest.toPath());
            }
            try (InputStream in = AbstractMigrationMojo.class.getResourceAsStream(BUNDLED_MANIFEST)) {
                if (in == null) {
                    throw new MojoExecutionException("No Parity Manifest is bundled with this plugin; set "
                                                     + "v2migration.manifest to a parity-manifest-1 JSON file");
                }
                return MigrationManifest.read(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        } catch (ManifestFormatException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
