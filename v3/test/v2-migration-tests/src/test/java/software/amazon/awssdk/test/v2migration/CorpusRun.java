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

package software.amazon.awssdk.test.v2migration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import software.amazon.awssdk.tools.releaseevidence.CompatibilityReport.StepResult;
import software.amazon.awssdk.v2migration.Disposition;
import software.amazon.awssdk.v2migration.ItemResult;
import software.amazon.awssdk.v2migration.MigrationEngine;
import software.amazon.awssdk.v2migration.MigrationRequest;
import software.amazon.awssdk.v2migration.MigrationResult;
import software.amazon.awssdk.v2migration.SourceLocation;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;
import software.amazon.awssdk.v2migration.pom.PomDocument;
import software.amazon.awssdk.v2migration.pom.ProjectDependencies;
import software.amazon.awssdk.v2migration.recipe.MigrationRecipe;

/**
 * One corpus project taken through the whole migration: resolved, compiled and run on the V2 baseline, migrated with
 * the real manifest, and compiled and run on the candidate. Work files are kept under
 * {@code <output>/work/<project>}: {@code original/} (the V2 project as resolved and compiled), {@code migrated/}
 * (the transformed project) and the logs and classes of each step.
 */
record CorpusRun(CorpusProject project, Path migrated, StepResult baseline, MigrationResult migration,
                 PomFacts migratedPom, StepResult compile, StepResult behavior) {

    /** The behavioral run of one side: its transcript (or failure) and the requests the fake service received. */
    record Behavior(IsolatedRun.Outcome outcome, List<FakeDynamoDb.Request> requests) {
    }

    static CorpusRun run(CorpusProject project, Corpus corpus) throws Exception {
        Path work = corpus.output().resolve("work").resolve(project.name());
        deleteRecursively(work);
        Path original = copy(project.directory(), work.resolve("original"));
        Path migrated = copy(project.directory(), work.resolve("migrated"));

        // 1. The V2 baseline: the project's own POM resolved by Maven, compiled and run.
        Path tree = work.resolve("v2-dependency-tree.txt");
        MavenFork.run(original, work.resolve("v2-resolve.log"),
                      "org.apache.maven.plugins:maven-dependency-plugin:" + System.getProperty("v3.dependencyPluginVersion")
                      + ":tree", "-DoutputFile=" + tree);
        DependencyGraph v2Graph = DependencyGraph.parse(Files.readString(tree, StandardCharsets.UTF_8), tree.toString());
        List<Path> v2Classpath = v2Classpath(v2Graph, Path.of(System.getProperty("v3.localRepository")));
        int v2Release = PomFacts.read(original.resolve("pom.xml")).javaRelease();
        JavaCompilation.Result v2Classes = JavaCompilation.compile(original, v2Release, v2Classpath,
                                                                   work.resolve("v2-classes"));
        Behavior v2Behavior = null;
        StepResult baseline;
        if (!v2Classes.succeeded()) {
            baseline = StepResult.failed("The V2 project does not compile for Java " + v2Release + " against V2 "
                                         + Corpus.V2_VERSION, strings(v2Classes.errors()));
        } else {
            v2Behavior = behave(project, v2Classes.classes(), v2Classpath);
            baseline = baselineResult(project, v2Behavior, v2Release);
        }

        // 2. The migration, exactly as the Maven plugin runs it: the effective dependency inventory and class path
        //    Maven resolved, every source, every discovered recipe.
        MigrationResult migration = MigrationEngine.run(request(project, migrated, v2Graph, v2Classpath,
                                                                corpus.manifest(), corpus.recipes()));
        migration.write();
        Files.createDirectories(corpus.output().resolve("reports"));
        Files.writeString(corpus.output().resolve(reportPath(project)), migration.reportJson(true),
                          StandardCharsets.UTF_8);
        PomFacts migratedPom = PomFacts.read(migrated.resolve("pom.xml"));

        // 3. The transformed project on the candidate (Requirements 17.7, 17.8).
        StepResult compile;
        StepResult behavior;
        if (migration.hasUnresolved()) {
            compile = StepResult.notRun("The migration left " + migration.count(Disposition.UNRESOLVED)
                                        + " unresolved diagnostics, so the transformed project is incomplete");
            behavior = StepResult.notRun("The transformed project was not compiled");
        } else {
            CandidateCompile candidate = CandidateCompile.compile(migrated, migratedPom, migration, corpus,
                                                                  work.resolve("v3-classes"));
            compile = candidate.result();
            if (candidate.classes() == null) {
                behavior = StepResult.notRun("The transformed project did not compile against the candidate");
            } else {
                behavior = candidateBehaviorResult(project, v2Behavior,
                                                   behave(project, candidate.classes(), candidate.classpath()));
            }
        }
        return new CorpusRun(project, migrated, baseline, migration, migratedPom, compile, behavior);
    }

    static String reportPath(CorpusProject project) {
        return "reports/" + project.name() + ".json";
    }

    private static StepResult baselineResult(CorpusProject project, Behavior v2, int release) {
        if (!v2.outcome().succeeded()) {
            return StepResult.failed("The V2 behavioral suite failed", List.of(v2.outcome().failure()));
        }
        if (!v2.outcome().transcript().strip().equals(project.expectedBehavior())) {
            return StepResult.failed("The V2 transcript differs from " + CorpusProject.EXPECTED_BEHAVIOR,
                                     List.of("expected:\n" + project.expectedBehavior() + "\nactual:\n"
                                             + v2.outcome().transcript().strip()));
        }
        return StepResult.passed("Compiled for Java " + release + " against V2 " + Corpus.V2_VERSION + "; the "
                                 + "behavioral suite printed the expected transcript"
                                 + (v2.requests().isEmpty() ? "" : " over " + v2.requests().size() + " requests"));
    }

    private static StepResult candidateBehaviorResult(CorpusProject project, Behavior v2, Behavior v3) {
        if (!v3.outcome().succeeded()) {
            return StepResult.failed("The transformed behavioral suite failed on the candidate",
                                     List.of(v3.outcome().failure()));
        }
        List<String> failures = new ArrayList<>();
        String transcript = v3.outcome().transcript().strip();
        if (!transcript.equals(project.expectedBehavior())) {
            failures.add("transcript differs from the V2 baseline:\nexpected:\n" + project.expectedBehavior()
                         + "\nactual:\n" + transcript);
        }
        if (v2 != null && !v2.requests().equals(v3.requests())) {
            failures.add("the service received different requests:\nV2:\n" + join(v2.requests()) + "\nV3:\n"
                         + join(v3.requests()));
        }
        if (!failures.isEmpty()) {
            return StepResult.failed("The transformed project behaves differently from the V2 baseline", failures);
        }
        return StepResult.passed("The transformed behavioral suite printed the V2 transcript"
                                 + (v3.requests().isEmpty() ? ""
                                                            : " and sent the V2 requests (" + v3.requests().size()
                                                              + ")"));
    }

    private static Behavior behave(CorpusProject project, Path classes, List<Path> classpath) throws IOException {
        if (project.service().equals("none")) {
            return new Behavior(IsolatedRun.run(classes, classpath, project.entryPoint(), null), List.of());
        }
        try (FakeDynamoDb service = new FakeDynamoDb()) {
            IsolatedRun.Outcome outcome = IsolatedRun.run(classes, classpath, project.entryPoint(), service.endpoint());
            return new Behavior(outcome, service.requests());
        }
    }

    private static MigrationRequest request(CorpusProject project, Path root, DependencyGraph graph,
                                            List<Path> classpath, MigrationManifest manifest,
                                            List<MigrationRecipe> recipes) throws IOException {
        PomDocument pom = PomDocument.parse("pom.xml", Files.readString(root.resolve("pom.xml"),
                                                                        StandardCharsets.UTF_8));
        // The POM's declarations, with the effective version Maven resolved for those a BOM manages.
        List<ProjectDependencies.Declared> declared = new ArrayList<>();
        for (ProjectDependencies.Declared d : pom.standaloneDependencies().declared()) {
            DependencyGraph.Node node = graph.node(d.groupId() + ":" + d.artifactId());
            if (d.effectiveVersion() == null && !d.isBomImport() && !ProjectDependencies.isManagement(d.section())
                && node != null) {
                d = new ProjectDependencies.Declared(d.section(), d.index(), d.groupId(), d.artifactId(),
                                                     d.declaredVersion(), node.version(), d.type(), d.classifier(),
                                                     d.scope());
            }
            declared.add(d);
        }
        List<ProjectDependencies.Resolved> resolved = new ArrayList<>();
        for (DependencyGraph.Node n : graph.nodes()) {
            resolved.add(new ProjectDependencies.Resolved(n.groupId(), n.artifactId(), n.version(), n.scope(),
                                                          n.trail()));
        }
        resolved.sort(Comparator.comparing(ProjectDependencies.Resolved::groupId)
                                .thenComparing(ProjectDependencies.Resolved::artifactId));
        return MigrationRequest.builder()
                               .projectRoot(root)
                               .project("com.example.corpus:" + project.name())
                               .sources(JavaCompilation.sources(root))
                               .classpath(classpath)
                               .sourceEncoding(StandardCharsets.UTF_8)
                               .pomFile(root.resolve("pom.xml"))
                               .dependencies(new ProjectDependencies(declared, resolved))
                               .manifest(manifest)
                               .recipes(recipes)
                               .build();
    }

    /** The jar of every compile- or runtime-scope artifact of {@code graph}, from the local repository. */
    private static List<Path> v2Classpath(DependencyGraph graph, Path repository) {
        List<Path> out = new ArrayList<>();
        for (DependencyGraph.Node n : graph.nodes()) {
            if (!Set.of("compile", "runtime", "provided").contains(n.scope()) || !n.type().equals("jar")) {
                continue;
            }
            Path jar = repository.resolve(n.groupId().replace('.', '/')).resolve(n.artifactId()).resolve(n.version())
                                 .resolve(n.artifactId() + "-" + n.version()
                                          + (n.classifier() == null ? "" : "-" + n.classifier()) + ".jar");
            if (!Files.isRegularFile(jar)) {
                throw new AssertionError("Maven resolved " + n.key() + ":" + n.version() + " but " + jar
                                         + " does not exist");
            }
            out.add(jar);
        }
        return out;
    }

    /** The items of {@code migration} whose range contains {@code position} of {@code path}, innermost first. */
    static List<ItemResult> itemsAt(MigrationResult migration, String path, long position) {
        List<ItemResult> out = new ArrayList<>();
        for (ItemResult r : migration.items()) {
            SourceLocation l = r.item().location();
            if (l.path().equals(path) && l.startOffset() <= position
                && (position < l.endOffset() || (l.isPosition() && position == l.startOffset()))) {
                out.add(r);
            }
        }
        out.sort(Comparator.comparingInt(r -> r.item().location().endOffset() - r.item().location().startOffset()));
        return out;
    }

    /** Every Java file whose text the migration changed. */
    Set<String> changedJavaFiles() {
        Set<String> out = new TreeSet<>();
        migration.changed().keySet().stream().filter(p -> p.endsWith(".java")).forEach(out::add);
        return out;
    }

    static List<String> strings(List<?> values) {
        return values.stream().map(String::valueOf).toList();
    }

    private static String join(List<FakeDynamoDb.Request> requests) {
        StringBuilder out = new StringBuilder();
        requests.forEach(r -> out.append("  ").append(r).append('\n'));
        return out.toString();
    }

    private static Path copy(Path from, Path to) throws IOException {
        try (Stream<Path> files = Files.walk(from)) {
            for (Path source : files.filter(f -> !from.relativize(f).startsWith("target")).toList()) {
                Path target = to.resolve(from.relativize(source).toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(source, target);
                }
            }
        }
        return to;
    }

    static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(path)) {
            for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }

}
