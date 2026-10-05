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

package software.amazon.awssdk.test.architecture;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * An index of the classes defined by an ordered set of class-path roots (class directories or jars), each labelled
 * with the artifact that owns it. The JDK's own classes are resolved through the platform class loader.
 */
public final class ClassPath {

    private static final Pattern VERSIONED_ENTRY = Pattern.compile("^META-INF/versions/\\d+/");

    private final Map<String, String> owners;

    private ClassPath(Map<String, String> owners) {
        this.owners = owners;
    }

    /**
     * Indexes {@code roots} (owner label to class directory or jar). The first root that defines a class owns it, as
     * on a class path. Roots that do not exist are an error: an unindexed artifact would make every reference into it
     * look unresolvable.
     */
    public static ClassPath of(Map<String, Path> roots) {
        Map<String, String> owners = new HashMap<>();
        roots.forEach((owner, root) -> {
            for (String name : classNames(root)) {
                owners.putIfAbsent(name, owner);
            }
        });
        return new ClassPath(Collections.unmodifiableMap(owners));
    }

    /** The owner label of the root that defines {@code binaryName}, or {@code "JDK"} for a platform class. */
    public Optional<String> ownerOf(String binaryName) {
        String owner = owners.get(binaryName);
        if (owner != null) {
            return Optional.of(owner);
        }
        return isJdkClass(binaryName) ? Optional.of("JDK") : Optional.empty();
    }

    /** True if the running JDK (boot or platform class loader) defines {@code binaryName}. */
    public static boolean isJdkClass(String binaryName) {
        return ClassLoader.getPlatformClassLoader().getResource(binaryName.replace('.', '/') + ".class") != null;
    }

    /**
     * Reads every class file under a class directory or in a jar, keyed by binary name. {@code module-info} is
     * skipped. For a multi-release jar the base entry is kept and versioned entries are read only for classes the
     * base does not define.
     */
    public static Map<String, ClassFile> readClasses(Path root) {
        Map<String, ClassFile> result = new TreeMap<>();
        forEachClass(root, (name, bytes) -> result.putIfAbsent(name, ClassFile.parse(bytes.read())));
        return Collections.unmodifiableMap(result);
    }

    private static Set<String> classNames(Path root) {
        Set<String> names = new LinkedHashSet<>();
        forEachClass(root, (name, bytes) -> names.add(name));
        return names;
    }

    @FunctionalInterface
    private interface ClassBytes {
        byte[] read();
    }

    @FunctionalInterface
    private interface ClassVisitor {
        void visit(String binaryName, ClassBytes bytes);
    }

    private static void forEachClass(Path root, ClassVisitor visitor) {
        if (Files.isDirectory(root)) {
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(f -> f.toString().endsWith(".class")).sorted().toList()) {
                    String entry = root.relativize(file).toString().replace('\\', '/');
                    visitEntry(entry, () -> readAllBytes(file), visitor);
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot read classes under " + root, e);
            }
        } else if (Files.isRegularFile(root)) {
            try (ZipFile zip = new ZipFile(root.toFile())) {
                // Base entries first so that they win over versioned entries.
                List<? extends ZipEntry> entries = zip.stream()
                                                      .filter(e -> e.getName().endsWith(".class"))
                                                      .sorted((a, b) -> Boolean.compare(
                                                          VERSIONED_ENTRY.matcher(a.getName()).find(),
                                                          VERSIONED_ENTRY.matcher(b.getName()).find()))
                                                      .toList();
                for (ZipEntry entry : entries) {
                    visitEntry(entry.getName(), () -> readEntry(zip, entry), visitor);
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot read jar " + root, e);
            }
        } else {
            throw new IllegalStateException("Class-path root does not exist: " + root);
        }
    }

    private static void visitEntry(String entry, ClassBytes bytes, ClassVisitor visitor) {
        String name = VERSIONED_ENTRY.matcher(entry).replaceFirst("");
        name = name.substring(0, name.length() - ".class".length());
        if (!name.equals("module-info") && !name.endsWith("/module-info")) {
            visitor.visit(name.replace('/', '.'), bytes);
        }
    }

    private static byte[] readAllBytes(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] readEntry(ZipFile zip, ZipEntry entry) {
        try (InputStream in = zip.getInputStream(entry)) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
