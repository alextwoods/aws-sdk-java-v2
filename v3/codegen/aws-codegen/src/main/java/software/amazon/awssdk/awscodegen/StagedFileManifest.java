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

package software.amazon.awssdk.awscodegen;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;
import software.amazon.smithy.build.FileManifest;

/**
 * A {@link FileManifest} that stages every generated file in memory and publishes the complete tree in one step.
 *
 * <p>{@link #getBaseDir()} is the final output directory, but nothing is written to disk until {@link #publish()}.
 * If generation fails before then, the previously published tree is untouched and no partial output exists.
 * Publishing writes the staged files to a sibling {@code <output>.staging} directory, moves any previous tree aside
 * to {@code <output>.previous}, renames the staged directory into place (atomically where the file system supports
 * it), and only then deletes the previous tree; if the final rename fails, the previous tree is restored.
 *
 * <p>Paths are kept relative to the base directory and must stay inside it. Each file may be written once per
 * generation: a second write to the same path is a generator or integration defect and fails generation instead of
 * silently replacing content. {@link #addFile(Path)} cannot register files written outside the manifest.
 *
 * <p>A manifest created with a {@link ContributionLedger} attributes every write (and every {@linkplain #claim claim}
 * made by a writer the delegator checks out) to the contributor in scope. A second write then keeps the first content
 * and is reported with every other conflict of the generation, naming the contributors and the shape; publication is
 * refused while the ledger holds any conflict (Requirement 3.7). Without a ledger a second write fails immediately.
 */
final class StagedFileManifest implements FileManifest {

    private static final String STAGING_SUFFIX = ".staging";
    private static final String PREVIOUS_SUFFIX = ".previous";

    private final Path baseDir;
    private final ContributionLedger ledger;
    private final Map<Path, byte[]> files = new TreeMap<>(Comparator.comparing(StagedFileManifest::portable));

    StagedFileManifest(Path outputDirectory) {
        this(outputDirectory, null);
    }

    /** A manifest whose writes are attributed in {@code ledger} ({@code null} for none). */
    StagedFileManifest(Path outputDirectory, ContributionLedger ledger) {
        this.baseDir = outputDirectory.toAbsolutePath().normalize();
        this.ledger = ledger;
    }

    @Override
    public Path getBaseDir() {
        return baseDir;
    }

    /** Absolute paths of the staged files, sorted by relative path. */
    @Override
    public synchronized Set<Path> getFiles() {
        Set<Path> result = new LinkedHashSet<>();
        files.keySet().forEach(relative -> result.add(baseDir.resolve(relative)));
        return result;
    }

    /** Staged file paths relative to {@link #getBaseDir()}, sorted. */
    synchronized List<Path> relativeFiles() {
        return List.copyOf(files.keySet());
    }

    /** Content of a staged file, or {@code null} if nothing was staged at {@code path}. */
    synchronized byte[] content(Path path) {
        byte[] content = files.get(relative(path));
        return content == null ? null : content.clone();
    }

    @Override
    public Path resolvePath(Path path) {
        return baseDir.resolve(relative(path));
    }

    /** Only files already staged through this manifest can be added; nothing written elsewhere is published. */
    @Override
    public synchronized Path addFile(Path path) {
        Path relative = relative(path);
        if (!files.containsKey(relative)) {
            throw new AwsCodegenException("Generated file " + portable(relative) + " was not written through the "
                                          + "generation FileManifest; files written elsewhere are never published.");
        }
        return baseDir.resolve(relative);
    }

    @Override
    public Path writeFile(Path path, Reader fileContentsReader) {
        StringWriter content = new StringWriter();
        try {
            fileContentsReader.transferTo(content);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read generated content for " + path, e);
        }
        return stage(path, content.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Path writeFile(Path path, InputStream fileContentsInputStream) {
        try {
            return stage(path, fileContentsInputStream.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read generated content for " + path, e);
        }
    }

    /** Same contract as the default (content followed by a newline), but staged in memory. */
    @Override
    public Path writeUsing(Path path, Consumer<Writer> consumer) {
        StringWriter content = new StringWriter();
        consumer.accept(content);
        content.write('\n');
        return writeFile(path, content.toString());
    }

    private synchronized Path stage(Path path, byte[] content) {
        Path relative = relative(path);
        Path absolute = baseDir.resolve(relative);
        if (ledger != null) {
            ledger.claimOutput(absolute, portable(relative));
        }
        if (files.putIfAbsent(relative, content) != null) {
            if (ledger == null) {
                throw new AwsCodegenException("Generated file " + portable(relative) + " was written more than once "
                                              + "in one generation.");
            }
            ledger.duplicateWrite(absolute, portable(relative));
        }
        return absolute;
    }

    /**
     * Attributes the file at {@code path} to the contributor in scope without writing it, for content that reaches
     * this manifest later (writer-delegator writers are claimed when checked out and written when flushed).
     */
    void claim(Path path) {
        if (ledger != null) {
            Path relative = relative(path);
            ledger.claimOutput(baseDir.resolve(relative), portable(relative));
        }
    }

    /** The ledger attributing this manifest's writes, or {@code null}. */
    ContributionLedger ledger() {
        return ledger;
    }

    /** Normalizes {@code path} to a non-empty path relative to the base directory, rejecting paths that escape it. */
    private Path relative(Path path) {
        Path candidate = path.isAbsolute() ? path.normalize() : baseDir.resolve(path).normalize();
        if (!candidate.startsWith(baseDir) || candidate.equals(baseDir)) {
            throw new AwsCodegenException("Generated file path '" + path + "' is not inside the generated source root.");
        }
        return baseDir.relativize(candidate);
    }

    /** Replaces the output directory with exactly the staged files. */
    void publish() {
        publishTogether(List.of(this));
    }

    /**
     * Publishes several staged trees (for example the generated source root and the generated resource root of one
     * service) as one unit: every tree is completely written to its staging directory before any published tree is
     * replaced, and if replacing any tree fails, every tree already replaced is restored to its previous content.
     *
     * @throws AwsCodegenException if the base directories overlap, a manifest's ledger holds a conflict (nothing is
     *         written then), or a tree cannot be published
     */
    static void publishTogether(List<StagedFileManifest> manifests) {
        for (int i = 0; i < manifests.size(); i++) {
            for (int j = i + 1; j < manifests.size(); j++) {
                Path a = manifests.get(i).baseDir;
                Path b = manifests.get(j).baseDir;
                if (a.startsWith(b) || b.startsWith(a)) {
                    throw new AwsCodegenException("Generated output roots " + a + " and " + b + " overlap.");
                }
            }
        }
        for (StagedFileManifest manifest : manifests) {
            if (manifest.ledger != null) {
                manifest.ledger.requireNoConflicts();
            }
        }
        List<StagedFileManifest> replaced = new ArrayList<>();
        List<Boolean> hadPrevious = new ArrayList<>();
        try {
            for (StagedFileManifest manifest : manifests) {
                manifest.writeStaging();
            }
            for (StagedFileManifest manifest : manifests) {
                boolean existed = Files.exists(manifest.baseDir);
                if (existed) {
                    move(manifest.baseDir, manifest.previous());
                }
                try {
                    move(manifest.staging(), manifest.baseDir);
                } catch (IOException e) {
                    if (existed) {
                        move(manifest.previous(), manifest.baseDir);
                    }
                    throw e;
                }
                replaced.add(manifest);
                hadPrevious.add(existed);
            }
            for (StagedFileManifest manifest : manifests) {
                deleteRecursively(manifest.previous());
            }
        } catch (IOException e) {
            AwsCodegenException failure = new AwsCodegenException(
                "Unable to publish generated output to "
                + manifests.stream().map(m -> m.baseDir.toString()).toList(), e);
            for (int i = replaced.size() - 1; i >= 0; i--) {
                StagedFileManifest manifest = replaced.get(i);
                try {
                    deleteRecursively(manifest.baseDir);
                    if (hadPrevious.get(i)) {
                        move(manifest.previous(), manifest.baseDir);
                    }
                } catch (IOException restore) {
                    failure.addSuppressed(restore);
                }
            }
            for (StagedFileManifest manifest : manifests) {
                try {
                    deleteRecursively(manifest.staging());
                } catch (IOException cleanup) {
                    failure.addSuppressed(cleanup);
                }
            }
            throw failure;
        }
    }

    /** Writes exactly the staged files to a fresh staging directory next to the base directory. */
    private synchronized void writeStaging() throws IOException {
        Path staging = staging();
        deleteRecursively(staging);
        deleteRecursively(previous());
        Files.createDirectories(staging);
        for (Map.Entry<Path, byte[]> file : files.entrySet()) {
            Path target = staging.resolve(file.getKey());
            Files.createDirectories(target.getParent());
            Files.write(target, file.getValue());
        }
    }

    private Path staging() {
        return baseDir.resolveSibling(baseDir.getFileName() + STAGING_SUFFIX);
    }

    private Path previous() {
        return baseDir.resolveSibling(baseDir.getFileName() + PREVIOUS_SUFFIX);
    }

    private static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target);
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    static String portable(Path relative) {
        return relative.toString().replace(relative.getFileSystem().getSeparator(), "/");
    }
}
