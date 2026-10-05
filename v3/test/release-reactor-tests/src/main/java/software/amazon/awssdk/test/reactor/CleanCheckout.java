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

package software.amazon.awssdk.test.reactor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Materializes a clean checkout of a repository subtree: exactly the files git would check out (tracked files plus
 * untracked files that are not ignored), and none of the build output ({@code target/}), generated sources, or other
 * ignored state present in the working tree.
 */
public final class CleanCheckout {
    private CleanCheckout() {
    }

    /**
     * Copies the checkout-visible files of {@code subtree} (relative to {@code repositoryRoot}) into
     * {@code destination}, which is deleted first. Returns the number of files copied.
     */
    public static int create(Path repositoryRoot, String subtree, Path destination) throws IOException,
                                                                                          InterruptedException {
        List<String> files = checkoutFiles(repositoryRoot, subtree);
        if (files.isEmpty()) {
            throw new IllegalStateException("git reports no files under " + subtree + " in " + repositoryRoot);
        }
        deleteRecursively(destination);
        int copied = 0;
        for (String file : files) {
            Path source = repositoryRoot.resolve(file);
            if (!Files.isRegularFile(source)) {
                continue; // tracked but deleted in the working tree
            }
            Path target = destination.resolve(file);
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
            copied++;
        }
        return copied;
    }

    private static List<String> checkoutFiles(Path repositoryRoot, String subtree) throws IOException,
                                                                                          InterruptedException {
        Process git = new ProcessBuilder("git", "-C", repositoryRoot.toString(), "ls-files", "-z", "--cached",
                                         "--others", "--exclude-standard", "--", subtree)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start();
        byte[] output;
        try (InputStream in = git.getInputStream()) {
            output = in.readAllBytes();
        }
        if (!git.waitFor(5, TimeUnit.MINUTES) || git.exitValue() != 0) {
            throw new IllegalStateException("git ls-files failed in " + repositoryRoot
                                            + "; a git checkout is required to create a clean checkout");
        }
        List<String> files = new ArrayList<>();
        for (String entry : new String(output, StandardCharsets.UTF_8).split("\0")) {
            if (!entry.isEmpty()) {
                files.add(entry);
            }
        }
        return files;
    }

    static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
