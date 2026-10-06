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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import software.amazon.awssdk.v2migration.manifest.MigrationManifest;
import software.amazon.awssdk.v2migration.recipe.MigrationRecipe;

/**
 * The outcome of {@link MigrationEngine#run(MigrationRequest)}: one {@link ItemResult} per encountered item (in item
 * order), the new text of every changed file, and the machine-readable report. Nothing is written until
 * {@link #write()}.
 */
public final class MigrationResult {

    private final String project;
    private final Path root;
    private final MigrationManifest manifest;
    private final List<MigrationRecipe> recipes;
    private final List<ItemResult> items;
    private final Map<String, String> originals;
    private final Map<String, String> changed;
    private final Map<String, Charset> charsets;

    MigrationResult(String project, Path root, MigrationManifest manifest, List<MigrationRecipe> recipes,
                    List<ItemResult> items, Map<String, String> originals, Map<String, String> changed,
                    Map<String, Charset> charsets) {
        this.project = project;
        this.root = root;
        this.manifest = manifest;
        this.recipes = List.copyOf(recipes);
        this.items = List.copyOf(items);
        this.originals = Collections.unmodifiableMap(new TreeMap<>(originals));
        this.changed = Collections.unmodifiableMap(new TreeMap<>(changed));
        this.charsets = Collections.unmodifiableMap(new TreeMap<>(charsets));
    }

    /** The project name, or {@code null}. */
    public String project() {
        return project;
    }

    /** The manifest the items were joined against. */
    public MigrationManifest manifest() {
        return manifest;
    }

    /** The recipes consulted, in ID order. */
    public List<MigrationRecipe> recipes() {
        return recipes;
    }

    /** One result per encountered item, in item order. */
    public List<ItemResult> items() {
        return items;
    }

    /** The diagnostics of every unresolved item, in item order. */
    public List<MigrationDiagnostic> diagnostics() {
        return items.stream().map(ItemResult::diagnostic).filter(Objects::nonNull).toList();
    }

    /** The number of items with {@code disposition}. */
    public long count(Disposition disposition) {
        return items.stream().filter(i -> i.disposition() == disposition).count();
    }

    /** True if any item is unresolved. */
    public boolean hasUnresolved() {
        return count(Disposition.UNRESOLVED) > 0;
    }

    /** The original text of every editable file, by project-relative path. */
    public Map<String, String> originals() {
        return originals;
    }

    /** The new text of every changed file, by project-relative path. */
    public Map<String, String> changed() {
        return changed;
    }

    /** Writes every changed file in its original encoding. */
    public void write() throws IOException {
        for (Map.Entry<String, String> file : changed.entrySet()) {
            Files.writeString(root.resolve(file.getKey()), file.getValue(), charsets.get(file.getKey()));
        }
    }

    /** The machine-readable report; {@code applied} records whether the edits were written. */
    public String reportJson(boolean applied) {
        return MigrationReport.toJson(this, applied);
    }
}
