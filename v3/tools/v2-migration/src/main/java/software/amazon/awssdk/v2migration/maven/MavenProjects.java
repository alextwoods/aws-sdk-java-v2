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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.project.MavenProject;
import software.amazon.awssdk.v2migration.MigrationRequest;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;
import software.amazon.awssdk.v2migration.pom.PomDocument;
import software.amazon.awssdk.v2migration.pom.ProjectDependencies;
import software.amazon.awssdk.v2migration.recipe.MigrationRecipe;

/**
 * Builds a {@link MigrationRequest} from Maven's own view of a project: its effective model (inheritance, property
 * interpolation, dependency management and imported BOMs already applied), its original model (what its
 * {@code pom.xml} declares, section by section), its resolved dependency graph and its source roots and class path.
 */
final class MavenProjects {

    private MavenProjects() {
    }

    /**
     * The request for {@code project}. Sources under the project directory (outside its build directory) are
     * inventoried and editable; other source roots, such as generated sources, only take part in attribution.
     */
    static MigrationRequest request(MavenProject project, MigrationManifest manifest, List<MigrationRecipe> recipes,
                                    boolean includeTestSources, String encoding)
        throws IOException, DependencyResolutionRequiredException {
        Path basedir = project.getBasedir().toPath().toAbsolutePath().normalize();
        Path buildDirectory = Paths.get(project.getBuild().getDirectory()).toAbsolutePath().normalize();
        Set<String> roots = new LinkedHashSet<>(project.getCompileSourceRoots());
        if (includeTestSources) {
            roots.addAll(project.getTestCompileSourceRoots());
        }
        List<Path> sources = new ArrayList<>();
        List<Path> context = new ArrayList<>();
        for (String root : roots) {
            for (Path file : javaFiles(Paths.get(root).toAbsolutePath().normalize())) {
                if (file.startsWith(basedir) && !file.startsWith(buildDirectory)) {
                    sources.add(file);
                } else {
                    context.add(file);
                }
            }
        }
        Set<Path> outputs = Set.of(Paths.get(project.getBuild().getOutputDirectory()).toAbsolutePath().normalize(),
                                   Paths.get(project.getBuild().getTestOutputDirectory()).toAbsolutePath().normalize());
        List<String> elements = includeTestSources ? project.getTestClasspathElements()
                                                   : project.getCompileClasspathElements();
        // The project's own (possibly stale) output is not consulted: its sources are attributed directly.
        List<Path> classpath = elements.stream().map(e -> Paths.get(e).toAbsolutePath().normalize())
                                       .filter(p -> !outputs.contains(p)).distinct().toList();
        Path pom = project.getFile() == null ? null : project.getFile().toPath().toAbsolutePath().normalize();
        return MigrationRequest.builder()
                               .projectRoot(basedir)
                               .project(project.getGroupId() + ":" + project.getArtifactId())
                               .sources(sources)
                               .contextSources(context)
                               .classpath(classpath)
                               .sourceEncoding(encoding == null || encoding.isBlank() ? StandardCharsets.UTF_8
                                                                                      : Charset.forName(encoding))
                               .pomFile(pom != null && pom.startsWith(basedir) ? pom : null)
                               .dependencies(dependencies(project))
                               .manifest(manifest)
                               .recipes(recipes)
                               .build();
    }

    /** The effective dependency and BOM inventory of {@code project}. */
    static ProjectDependencies dependencies(MavenProject project) {
        Model original = project.getOriginalModel();
        Model effective = project.getModel();
        Map<String, String> properties = properties(project);
        List<ProjectDependencies.Declared> declared = new ArrayList<>();
        section(declared, ProjectDependencies.DEPENDENCIES, original.getDependencies(), effective.getDependencies(),
                properties);
        section(declared, ProjectDependencies.DEPENDENCY_MANAGEMENT, managed(original.getDependencyManagement()),
                managed(effective.getDependencyManagement()), properties);
        for (Profile profile : original.getProfiles()) {
            // An active profile's declarations are merged into the effective model; an inactive one's are not.
            section(declared, ProjectDependencies.profileSection(profile.getId(), false), profile.getDependencies(),
                    effective.getDependencies(), properties);
            section(declared, ProjectDependencies.profileSection(profile.getId(), true),
                    managed(profile.getDependencyManagement()), managed(effective.getDependencyManagement()),
                    properties);
        }
        List<ProjectDependencies.Resolved> resolved = new ArrayList<>();
        Set<Artifact> artifacts = project.getArtifacts() == null ? Set.of() : project.getArtifacts();
        for (Artifact a : artifacts) {
            resolved.add(new ProjectDependencies.Resolved(a.getGroupId(), a.getArtifactId(), a.getVersion(),
                                                          a.getScope(), trail(a)));
        }
        resolved.sort(Comparator.comparing(ProjectDependencies.Resolved::groupId)
                                .thenComparing(ProjectDependencies.Resolved::artifactId));
        return new ProjectDependencies(declared, resolved);
    }

    private static void section(List<ProjectDependencies.Declared> out, String section, List<Dependency> declared,
                                List<Dependency> effective, Map<String, String> properties) {
        for (int i = 0; i < declared.size(); i++) {
            Dependency d = declared.get(i);
            String groupId = interpolate(d.getGroupId(), properties);
            String artifactId = interpolate(d.getArtifactId(), properties);
            String type = interpolate(d.getType(), properties);
            String classifier = interpolate(d.getClassifier(), properties);
            String scope = interpolate(d.getScope(), properties);
            String version = interpolate(d.getVersion(), properties);
            String effectiveVersion = null;
            if (!"import".equals(scope)) {
                // An import is consumed by Maven (its content replaces it), so only its written version applies.
                effectiveVersion = effective.stream()
                                            .filter(e -> Objects.equals(e.getGroupId(), groupId)
                                                         && Objects.equals(e.getArtifactId(), artifactId)
                                                         && Objects.equals(e.getType(), type)
                                                         && Objects.equals(e.getClassifier(), classifier))
                                            .map(Dependency::getVersion).filter(Objects::nonNull).findFirst()
                                            .orElse(null);
            }
            if (effectiveVersion == null && version != null && !version.contains("${")) {
                effectiveVersion = version;
            }
            out.add(new ProjectDependencies.Declared(section, i, groupId == null ? "" : groupId,
                                                     artifactId == null ? "" : artifactId, d.getVersion(),
                                                     effectiveVersion, type, classifier, scope));
        }
    }

    private static List<Dependency> managed(DependencyManagement management) {
        return management == null ? List.of() : management.getDependencies();
    }

    /** The {@code groupId:artifactId} path from the direct dependency to {@code artifact}. */
    private static List<String> trail(Artifact artifact) {
        List<String> trail = artifact.getDependencyTrail();
        List<String> out = new ArrayList<>();
        if (trail != null) {
            // The first element is the project itself.
            for (String step : trail.subList(Math.min(1, trail.size()), trail.size())) {
                String[] parts = step.split(":");
                if (parts.length >= 2) {
                    out.add(parts[0] + ":" + parts[1]);
                }
            }
        }
        return out.isEmpty() ? List.of(artifact.getGroupId() + ":" + artifact.getArtifactId()) : out;
    }

    private static Map<String, String> properties(MavenProject project) {
        Map<String, String> properties = new HashMap<>();
        project.getProperties().forEach((k, v) -> properties.put(String.valueOf(k), String.valueOf(v)));
        properties.put("project.groupId", project.getGroupId());
        properties.put("project.artifactId", project.getArtifactId());
        properties.put("project.version", project.getVersion());
        properties.put("pom.groupId", project.getGroupId());
        properties.put("pom.version", project.getVersion());
        if (project.getModel().getParent() != null) {
            properties.put("project.parent.groupId", project.getModel().getParent().getGroupId());
            properties.put("project.parent.version", project.getModel().getParent().getVersion());
        }
        return properties;
    }

    private static String interpolate(String value, Map<String, String> properties) {
        return value == null ? null : PomDocument.interpolate(value, properties);
    }

    private static List<Path> javaFiles(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(f -> f.toString().endsWith(".java") && Files.isRegularFile(f)).sorted().toList();
        }
    }
}
