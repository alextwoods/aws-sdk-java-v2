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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;
import software.amazon.awssdk.v2migration.pom.ProjectDependencies;
import software.amazon.awssdk.v2migration.recipe.MigrationRecipe;

/** What {@link MigrationEngine#run(MigrationRequest)} migrates: one project, its manifest and recipes. */
public final class MigrationRequest {

    private final Path projectRoot;
    private final String project;
    private final List<Path> sources;
    private final List<Path> contextSources;
    private final List<Path> classpath;
    private final Charset sourceEncoding;
    private final Path pomFile;
    private final ProjectDependencies dependencies;
    private final MigrationManifest manifest;
    private final List<MigrationRecipe> recipes;

    private MigrationRequest(Builder b) {
        this.projectRoot = Objects.requireNonNull(b.projectRoot, "projectRoot").toAbsolutePath().normalize();
        this.project = b.project;
        this.sources = List.copyOf(b.sources);
        this.contextSources = List.copyOf(b.contextSources);
        this.classpath = List.copyOf(b.classpath);
        this.sourceEncoding = Objects.requireNonNull(b.sourceEncoding, "sourceEncoding");
        this.pomFile = b.pomFile;
        this.dependencies = b.dependencies;
        this.manifest = Objects.requireNonNull(b.manifest, "manifest");
        this.recipes = List.copyOf(b.recipes);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The project directory; reported paths are relative to it. */
    public Path projectRoot() {
        return projectRoot;
    }

    /** The project name for the report (for example {@code groupId:artifactId}), or {@code null}. */
    public String project() {
        return project;
    }

    /** The Java sources to inventory and edit (absolute, or relative to the project root). */
    public List<Path> sources() {
        return sources;
    }

    /** Java sources compiled with {@link #sources()} for attribution only (for example generated sources). */
    public List<Path> contextSources() {
        return contextSources;
    }

    /** The class path the sources are attributed against: the project's resolved V2 dependencies. */
    public List<Path> classpath() {
        return classpath;
    }

    /** The encoding of the Java sources. */
    public Charset sourceEncoding() {
        return sourceEncoding;
    }

    /** The project's {@code pom.xml} to inventory and edit, or {@code null}. */
    public Path pomFile() {
        return pomFile;
    }

    /**
     * The project's effective dependency inventory, or {@code null} to derive it from {@link #pomFile()} alone
     * ({@link software.amazon.awssdk.v2migration.pom.PomDocument#standaloneDependencies()}).
     */
    public ProjectDependencies dependencies() {
        return dependencies;
    }

    public MigrationManifest manifest() {
        return manifest;
    }

    public List<MigrationRecipe> recipes() {
        return recipes;
    }

    public static final class Builder {
        private Path projectRoot;
        private String project;
        private final List<Path> sources = new ArrayList<>();
        private final List<Path> contextSources = new ArrayList<>();
        private final List<Path> classpath = new ArrayList<>();
        private Charset sourceEncoding = StandardCharsets.UTF_8;
        private Path pomFile;
        private ProjectDependencies dependencies;
        private MigrationManifest manifest;
        private final List<MigrationRecipe> recipes = new ArrayList<>();

        private Builder() {
        }

        public Builder projectRoot(Path projectRoot) {
            this.projectRoot = projectRoot;
            return this;
        }

        public Builder project(String project) {
            this.project = project;
            return this;
        }

        public Builder sources(List<Path> sources) {
            this.sources.addAll(sources);
            return this;
        }

        public Builder contextSources(List<Path> contextSources) {
            this.contextSources.addAll(contextSources);
            return this;
        }

        public Builder classpath(List<Path> classpath) {
            this.classpath.addAll(classpath);
            return this;
        }

        public Builder sourceEncoding(Charset sourceEncoding) {
            this.sourceEncoding = sourceEncoding;
            return this;
        }

        public Builder pomFile(Path pomFile) {
            this.pomFile = pomFile;
            return this;
        }

        public Builder dependencies(ProjectDependencies dependencies) {
            this.dependencies = dependencies;
            return this;
        }

        public Builder manifest(MigrationManifest manifest) {
            this.manifest = manifest;
            return this;
        }

        public Builder recipes(List<? extends MigrationRecipe> recipes) {
            this.recipes.addAll(recipes);
            return this;
        }

        public MigrationRequest build() {
            return new MigrationRequest(this);
        }
    }
}
