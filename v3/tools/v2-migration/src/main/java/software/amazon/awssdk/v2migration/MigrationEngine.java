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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import software.amazon.awssdk.v2migration.internal.DispositionPlanner;
import software.amazon.awssdk.v2migration.internal.JavaInventory;
import software.amazon.awssdk.v2migration.internal.PomInventory;
import software.amazon.awssdk.v2migration.internal.SourceText;
import software.amazon.awssdk.v2migration.manifest.ManifestEntry;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;
import software.amazon.awssdk.v2migration.pom.PomDocument;
import software.amazon.awssdk.v2migration.pom.ProjectDependencies;
import software.amazon.awssdk.v2migration.recipe.JavaNode;
import software.amazon.awssdk.v2migration.recipe.MigrationRecipe;
import software.amazon.awssdk.v2migration.recipe.PomNode;
import software.amazon.awssdk.v2migration.recipe.RecipeContext;
import software.amazon.awssdk.v2migration.recipe.RecipeOutcome;
import software.amazon.awssdk.v2migration.recipe.Recipes;

/**
 * The V2-to-V3 migration engine (design "Migration tooling flow"):
 * <ol>
 *   <li>inventory the project's effective V2 dependency and BOM graph and its Java sources (attributed against the
 *       project's class path) into {@link MigrationItem}s;</li>
 *   <li>join every item to its Parity Manifest entry and offer it to every recipe in ID order;</li>
 *   <li>assign exactly one {@link Disposition} per item, accepting only deterministic, non-overlapping edits that
 *       leave every unresolved construct byte-for-byte unchanged;</li>
 *   <li>return the edited file texts and the machine-readable report, without writing anything; the caller decides
 *       whether to {@linkplain MigrationResult#write() write} the edits.</li>
 * </ol>
 */
public final class MigrationEngine {

    private MigrationEngine() {
    }

    /** Migrates {@code request}'s project in memory. */
    public static MigrationResult run(MigrationRequest request) throws IOException {
        Path root = request.projectRoot();
        List<MigrationRecipe> recipes = Recipes.ordered(request.recipes());
        MigrationManifest manifest = request.manifest();
        Map<String, SourceText> editable = new TreeMap<>();
        Map<String, Charset> charsets = new TreeMap<>();
        List<SourceText> sources = read(root, request.sources(), request.sourceEncoding());
        sources.forEach(s -> {
            editable.put(s.path(), s);
            charsets.put(s.path(), request.sourceEncoding());
        });
        Set<String> inventoriedPaths = new HashSet<>(editable.keySet());
        List<SourceText> context = new ArrayList<>();
        for (SourceText s : read(root, request.contextSources(), request.sourceEncoding())) {
            if (!inventoriedPaths.contains(s.path())) {
                context.add(s);
            }
        }

        PomDocument pom = null;
        String pomPath = "pom.xml";
        ProjectDependencies dependencies = request.dependencies();
        if (request.pomFile() != null) {
            Path pomFile = root.resolve(request.pomFile()).normalize();
            pomPath = relative(root, pomFile);
            pom = PomDocument.parse(pomPath, Files.readString(pomFile, StandardCharsets.UTF_8));
            editable.put(pomPath, new SourceText(pomPath, pom.text()));
            charsets.put(pomPath, StandardCharsets.UTF_8);
            if (dependencies == null) {
                dependencies = pom.standaloneDependencies();
            }
        }

        Map<String, String> texts = new TreeMap<>();
        editable.forEach((path, text) -> texts.put(path, text.content()));
        List<Located> located = new ArrayList<>();
        if (dependencies != null) {
            for (PomInventory.Found f : PomInventory.inventory(pom, pomPath, dependencies)) {
                located.add(new Located(f.item(), null, f.node()));
            }
        }
        DispositionPlanner.Plan plan;
        if (sources.isEmpty()) {
            plan = plan(located, manifest, recipes, texts);
        } else {
            List<Path> classpath = request.classpath().stream().filter(Files::exists).toList();
            try (JavaInventory java = JavaInventory.analyze(sources, context, classpath, manifest)) {
                for (JavaInventory.Found f : java.found()) {
                    located.add(new Located(f.item(), f.node(), null));
                }
                // Recipes run while the compiler's attribution is alive.
                plan = plan(located, manifest, recipes, texts);
            }
        }
        return new MigrationResult(request.project(), root, manifest, recipes, plan.results(), texts, plan.changed(),
                                   charsets);
    }

    /** An item and its syntax. */
    private record Located(MigrationItem item, JavaNode java, PomNode pom) {
    }

    private static DispositionPlanner.Plan plan(List<Located> located, MigrationManifest manifest,
                                                List<MigrationRecipe> recipes, Map<String, String> texts) {
        // One item per kind, ID and range, in the deterministic item order.
        Map<MigrationItem, Located> unique = new TreeMap<>();
        for (Located l : located) {
            unique.putIfAbsent(l.item(), l);
        }
        Set<String> editablePaths = Collections.unmodifiableSet(new TreeSet<>(texts.keySet()));
        List<DispositionPlanner.Proposal> proposals = new ArrayList<>();
        for (Located l : unique.values()) {
            ManifestEntry entry = l.item().id() == null ? null : manifest.entry(l.item().id()).orElse(null);
            Context context = new Context(l.item(), entry, manifest, editablePaths, texts, l.java(), l.pom());
            List<DispositionPlanner.Answer> answers = new ArrayList<>();
            for (MigrationRecipe recipe : recipes) {
                RecipeOutcome outcome;
                try {
                    outcome = recipe.visit(context);
                } catch (RuntimeException e) {
                    outcome = RecipeOutcome.unsafe("the recipe failed (" + e.getClass().getSimpleName()
                                                   + (e.getMessage() == null ? "" : ": " + e.getMessage()) + ")");
                }
                if (outcome == null) {
                    outcome = RecipeOutcome.unsafe("the recipe returned no answer");
                }
                if (!(outcome instanceof RecipeOutcome.NotApplicable)) {
                    answers.add(new DispositionPlanner.Answer(recipe.id(), outcome));
                }
            }
            proposals.add(new DispositionPlanner.Proposal(l.item(), entry, answers));
        }
        return DispositionPlanner.plan(proposals, texts);
    }

    private static List<SourceText> read(Path root, List<Path> files, Charset charset) throws IOException {
        Map<String, SourceText> byPath = new TreeMap<>();
        for (Path file : files) {
            Path absolute = root.resolve(file).toAbsolutePath().normalize();
            String path = relative(root, absolute);
            if (!byPath.containsKey(path)) {
                byPath.put(path, new SourceText(path, Files.readString(absolute, charset)));
            }
        }
        return new ArrayList<>(byPath.values());
    }

    static String relative(Path root, Path file) {
        Path relative = file.startsWith(root) ? root.relativize(file) : file;
        return relative.toString().replace(file.getFileSystem().getSeparator(), "/");
    }

    /** The recipe view of one item. */
    private record Context(MigrationItem item, ManifestEntry entryValue, MigrationManifest manifest,
                           Set<String> editablePaths, Map<String, String> texts, JavaNode javaNode, PomNode pomNode)
        implements RecipeContext {

        @Override
        public Optional<ManifestEntry> entry() {
            return Optional.ofNullable(entryValue);
        }

        @Override
        public String text(String path) {
            String text = texts.get(path);
            if (text == null) {
                throw new IllegalArgumentException(path + " is not an editable project file");
            }
            return text;
        }

        @Override
        public Optional<JavaNode> java() {
            return Optional.ofNullable(javaNode);
        }

        @Override
        public Optional<PomNode> pom() {
            return Optional.ofNullable(pomNode);
        }
    }
}
