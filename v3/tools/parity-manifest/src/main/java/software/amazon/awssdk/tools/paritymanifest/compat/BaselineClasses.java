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

package software.amazon.awssdk.tools.paritymanifest.compat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import software.amazon.awssdk.tools.paritymanifest.ApiClassFile;

/**
 * The class files of the V2 baseline API (the released jars the Parity Manifest was scanned from), indexed by binary
 * name. They supply the facts the manifest ID does not carry: owner and member access flags, type kind, finality and
 * the supertype graph that hierarchy fixtures require.
 */
public final class BaselineClasses {
    private final Map<String, ApiClassFile> classes;

    private BaselineClasses(Map<String, ApiClassFile> classes) {
        this.classes = classes;
    }

    /** An index of {@code classes}; the first class of each binary name wins. */
    public static BaselineClasses of(Collection<ApiClassFile> classes) {
        Map<String, ApiClassFile> index = new TreeMap<>();
        classes.forEach(c -> index.putIfAbsent(c.name(), c));
        return new BaselineClasses(index);
    }

    /** Reads every class file of {@code roots}: jars or class directories, in order (the first class wins). */
    public static BaselineClasses read(List<Path> roots) throws IOException {
        Map<String, ApiClassFile> index = new TreeMap<>();
        for (Path root : roots) {
            if (Files.isDirectory(root)) {
                try (Stream<Path> files = Files.walk(root)) {
                    for (Path file : files.filter(Files::isRegularFile)
                                          .filter(f -> isClassFile(root.relativize(f).toString().replace('\\', '/')))
                                          .sorted().toList()) {
                        add(index, Files.readAllBytes(file));
                    }
                }
            } else if (Files.isRegularFile(root)) {
                try (ZipFile zip = new ZipFile(root.toFile())) {
                    for (ZipEntry entry : zip.stream().sorted(Comparator.comparing(ZipEntry::getName)).toList()) {
                        if (isClassFile(entry.getName())) {
                            try (InputStream in = zip.getInputStream(entry)) {
                                add(index, in.readAllBytes());
                            }
                        }
                    }
                }
            } else {
                throw new IOException("Baseline class path element does not exist: " + root);
            }
        }
        return new BaselineClasses(index);
    }

    /** The class with binary name {@code name}, if the baseline contains it. */
    public Optional<ApiClassFile> find(String name) {
        return Optional.ofNullable(classes.get(name));
    }

    /** The number of indexed classes. */
    public int size() {
        return classes.size();
    }

    private static void add(Map<String, ApiClassFile> index, byte[] bytes) {
        ApiClassFile c = ApiClassFile.parse(bytes);
        index.putIfAbsent(c.name(), c);
    }

    private static boolean isClassFile(String name) {
        return name.endsWith(".class") && !name.startsWith("META-INF/") && !name.endsWith("module-info.class")
               && !name.endsWith("package-info.class");
    }
}
