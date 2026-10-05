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

package software.amazon.awssdk.test.codegen.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * An immutable snapshot of a generated file tree: every regular file under a root, keyed by its {@code /}-separated
 * path relative to that root, with its exact bytes. Directories carry no content and are not part of the snapshot.
 *
 * <p>Two trees are byte-identical (Requirement 3.6) when they have the same relative path set and the same bytes for
 * every path; {@link #compare} reports every difference, so a failing determinism check shows all of them at once.
 */
public final class GeneratedTree {

    private final SortedMap<String, byte[]> files;

    private GeneratedTree(SortedMap<String, byte[]> files) {
        this.files = files;
    }

    /** Reads every regular file under {@code root}; an absent root is an empty tree. */
    public static GeneratedTree read(Path root) {
        SortedMap<String, byte[]> files = new TreeMap<>();
        if (!Files.exists(root)) {
            return new GeneratedTree(files);
        }
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path file : walk.filter(Files::isRegularFile).toList()) {
                files.put(portable(root.relativize(file)), Files.readAllBytes(file));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read generated tree " + root, e);
        }
        return new GeneratedTree(files);
    }

    /** A tree with exactly {@code files} (path to content). */
    public static GeneratedTree of(Map<String, byte[]> files) {
        SortedMap<String, byte[]> copy = new TreeMap<>();
        files.forEach((path, content) -> copy.put(path, content.clone()));
        return new GeneratedTree(copy);
    }

    /** Sorted relative paths of every file. */
    public SortedSet<String> paths() {
        return Collections.unmodifiableSortedSet(new TreeSet<>(files.keySet()));
    }

    /** A copy of the content at {@code path}. */
    public byte[] content(String path) {
        byte[] content = files.get(path);
        if (content == null) {
            throw new IllegalArgumentException("No generated file " + path + "; files: " + files.keySet());
        }
        return content.clone();
    }

    /** The content at {@code path} decoded as UTF-8. */
    public String text(String path) {
        return new String(content(path), StandardCharsets.UTF_8);
    }

    public boolean isEmpty() {
        return files.isEmpty();
    }

    /** The subtree of files whose path matches {@code filter}. */
    public GeneratedTree filter(Predicate<String> filter) {
        SortedMap<String, byte[]> kept = new TreeMap<>();
        files.forEach((path, content) -> {
            if (filter.test(path)) {
                kept.put(path, content);
            }
        });
        return new GeneratedTree(kept);
    }

    /** Every difference between this tree ({@code left}) and {@code right}. */
    public Difference compare(GeneratedTree right) {
        SortedSet<String> onlyLeft = new TreeSet<>(files.keySet());
        onlyLeft.removeAll(right.files.keySet());
        SortedSet<String> onlyRight = new TreeSet<>(right.files.keySet());
        onlyRight.removeAll(files.keySet());
        List<FileDifference> differing = new ArrayList<>();
        files.forEach((path, left) -> {
            byte[] other = right.files.get(path);
            if (other != null && !Arrays.equals(left, other)) {
                differing.add(FileDifference.of(path, left, other));
            }
        });
        return new Difference(onlyLeft, onlyRight, differing);
    }

    /** Throws an {@link AssertionError} describing every difference unless the trees are byte-identical. */
    public static void assertByteIdentical(GeneratedTree expected, GeneratedTree actual) {
        Difference difference = expected.compare(actual);
        if (!difference.isEmpty()) {
            throw new AssertionError("Generated trees differ:\n" + difference.describe());
        }
    }

    private static String portable(Path relative) {
        return relative.toString().replace(relative.getFileSystem().getSeparator(), "/");
    }

    /**
     * All differences between two trees.
     *
     * @param onlyLeft paths present only in the left tree
     * @param onlyRight paths present only in the right tree
     * @param differing paths present in both with different bytes, sorted by path
     */
    public record Difference(SortedSet<String> onlyLeft, SortedSet<String> onlyRight, List<FileDifference> differing) {
        public Difference {
            onlyLeft = Collections.unmodifiableSortedSet(new TreeSet<>(onlyLeft));
            onlyRight = Collections.unmodifiableSortedSet(new TreeSet<>(onlyRight));
            differing = List.copyOf(differing);
        }

        public boolean isEmpty() {
            return onlyLeft.isEmpty() && onlyRight.isEmpty() && differing.isEmpty();
        }

        /** One line per difference. */
        public String describe() {
            StringBuilder result = new StringBuilder();
            onlyLeft.forEach(p -> result.append("  only in left:  ").append(p).append('\n'));
            onlyRight.forEach(p -> result.append("  only in right: ").append(p).append('\n'));
            differing.forEach(d -> result.append("  differs:       ").append(d).append('\n'));
            return result.toString();
        }
    }

    /**
     * A file whose bytes differ.
     *
     * @param path relative path
     * @param offset first differing byte offset
     * @param line 1-based line of {@code offset} in the left file
     * @param leftLine that line of the left file
     * @param rightLine that line of the right file (empty if the right file is shorter)
     */
    public record FileDifference(String path, int offset, int line, String leftLine, String rightLine) {
        static FileDifference of(String path, byte[] left, byte[] right) {
            int offset = Arrays.mismatch(left, right);
            String leftText = new String(left, StandardCharsets.UTF_8);
            String rightText = new String(right, StandardCharsets.UTF_8);
            int line = 1;
            for (int i = 0; i < Math.min(offset, left.length); i++) {
                if (left[i] == '\n') {
                    line++;
                }
            }
            return new FileDifference(path, offset, line, lineAt(leftText, line), lineAt(rightText, line));
        }

        private static String lineAt(String text, int line) {
            String[] lines = text.split("\n", -1);
            return line <= lines.length ? lines[line - 1] : "";
        }

        @Override
        public String toString() {
            return path + " at byte " + offset + " (line " + line + "): '" + leftLine + "' vs '" + rightLine + "'";
        }
    }
}
