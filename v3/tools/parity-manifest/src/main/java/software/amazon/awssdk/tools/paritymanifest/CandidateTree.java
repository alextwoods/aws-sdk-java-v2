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

package software.amazon.awssdk.tools.paritymanifest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Locates a V3 release candidate in a built V3 source tree (the {@code v3/} directory after the reactor compiled it):
 * the release artifacts the public BOM manages, each artifact module's compiled output directory, and the test
 * classes that can act as evidence hooks. {@link LaunchGateCli} and the in-build gate use it so the gate runs on the
 * module outputs before they are packaged.
 */
public final class CandidateTree {
    /** Directory names never descended into when looking for modules. */
    private static final Set<String> SKIPPED = Set.of("target", "src", ".git", "node_modules", ".mvn");
    /** A {@code void} method declaration (JUnit test methods return {@code void}); group 1 is its name. */
    private static final Pattern VOID_METHOD = Pattern.compile("\\bvoid\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\(");

    private CandidateTree() {
    }

    /**
     * The {@code groupId:artifactId} of every artifact the BOM POM at {@code bomPom} manages (its
     * {@code dependencyManagement} dependencies): the V3 release artifacts.
     */
    public static SortedSet<String> bomArtifacts(Path bomPom) throws IOException {
        Element project = V2PomTree.parse(bomPom);
        Element management = V2PomTree.child(project, "dependencyManagement");
        Element dependencies = management == null ? null : V2PomTree.child(management, "dependencies");
        if (dependencies == null) {
            throw new IOException(bomPom + " manages no dependencies");
        }
        String projectGroup = groupId(project);
        SortedSet<String> artifacts = new TreeSet<>();
        for (Node n = dependencies.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element d && d.getTagName().equals("dependency")) {
                String group = V2PomTree.childText(d, "groupId");
                if (group == null || group.equals("${project.groupId}")) {
                    group = projectGroup;
                }
                artifacts.add(group + ":" + V2PomTree.childText(d, "artifactId"));
            }
        }
        return artifacts;
    }

    /**
     * The built output of every module below {@code root} whose {@code groupId:artifactId} is in {@code artifacts}
     * and whose compiled output ({@code target/classes}) holds the V3 module resource
     * ({@code META-INF/aws-sdk-java-v3/modules/<artifactId>.properties}), by coordinates: its packaged jar
     * ({@code target/<artifactId>-<version>.jar}, which also carries the jar manifest) when one exists that is not
     * older than the compiled output, else the compiled output itself. Modules that have not been built are absent.
     */
    public static SortedMap<String, Path> moduleOutputs(Path root, Set<String> artifacts) throws IOException {
        SortedMap<String, Path> outputs = new TreeMap<>();
        for (Path pom : poms(root)) {
            Element project = V2PomTree.parse(pom);
            String artifactId = V2PomTree.childText(project, "artifactId");
            String coordinates = groupId(project) + ":" + artifactId;
            Path classes = pom.getParent().resolve("target/classes");
            Path properties = classes.resolve(CandidateInventoryScanner.MODULE_PROPERTIES + artifactId + ".properties");
            if (artifacts.contains(coordinates) && Files.isRegularFile(properties)) {
                Path output = packaged(classes.getParent(), artifactId, properties).orElse(classes);
                Path previous = outputs.put(coordinates, output);
                if (previous != null) {
                    throw new IOException(coordinates + " is built by two modules: " + previous + " and " + output);
                }
            }
        }
        return outputs;
    }

    /**
     * The coordinates of the POM-only release artifacts of the tree that {@link CandidateInventoryScanner} cannot
     * scan: the public BOM ({@code bom/pom.xml}) and the internal BOM ({@code bom-internal/pom.xml}), when present.
     */
    public static SortedSet<String> pomArtifacts(Path root) throws IOException {
        SortedSet<String> artifacts = new TreeSet<>();
        for (String module : new String[] {"bom", "bom-internal"}) {
            Path pom = root.resolve(module).resolve("pom.xml");
            if (Files.isRegularFile(pom)) {
                Element project = V2PomTree.parse(pom);
                artifacts.add(groupId(project) + ":" + V2PomTree.childText(project, "artifactId"));
            }
        }
        return artifacts;
    }

    private static Optional<Path> packaged(Path target, String artifactId, Path properties)
        throws IOException {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(properties)) {
            p.load(in);
        }
        String version = p.getProperty("version");
        Path jar = version == null ? null : target.resolve(artifactId + "-" + version + ".jar");
        if (jar == null || !Files.isRegularFile(jar)
            || Files.getLastModifiedTime(jar).compareTo(Files.getLastModifiedTime(properties)) < 0) {
            return Optional.empty();
        }
        return Optional.of(jar);
    }

    /**
     * The simple names of every test source ({@code src/test/java/**}{@code /*.java}) of every module below
     * {@code root}: the verifications the tree can run as evidence hooks.
     */
    public static SortedSet<String> testClasses(Path root) throws IOException {
        return new TreeSet<>(testSources(root).keySet());
    }

    /**
     * The names of the methods (declared {@code void name(}, as JUnit test methods are) of every test source of every
     * module below {@code root}, by the simple name of the source file; sources with the same simple name in several
     * modules share one entry.
     */
    public static SortedMap<String, SortedSet<String>> testMethods(Path root) throws IOException {
        SortedMap<String, SortedSet<String>> methods = new TreeMap<>();
        for (Map.Entry<String, List<Path>> source : testSources(root).entrySet()) {
            SortedSet<String> names = methods.computeIfAbsent(source.getKey(), k -> new TreeSet<>());
            for (Path file : source.getValue()) {
                Matcher m = VOID_METHOD.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (m.find()) {
                    names.add(m.group(1));
                }
            }
        }
        return methods;
    }

    /** Every test source ({@code src/test/java/**}{@code /*.java}) below {@code root}, by simple name. */
    private static SortedMap<String, List<Path>> testSources(Path root) throws IOException {
        SortedMap<String, List<Path>> sources = new TreeMap<>();
        for (Path pom : poms(root)) {
            Path tests = pom.getParent().resolve("src/test/java");
            if (!Files.isDirectory(tests)) {
                continue;
            }
            try (var files = Files.walk(tests)) {
                files.filter(f -> f.getFileName().toString().endsWith(".java")).sorted().forEach(f -> {
                    String name = f.getFileName().toString();
                    sources.computeIfAbsent(name.substring(0, name.length() - ".java".length()),
                                            k -> new ArrayList<>()).add(f);
                });
            }
        }
        return sources;
    }

    private static String groupId(Element project) {
        String group = V2PomTree.childText(project, "groupId");
        if (group == null) {
            Element parent = V2PomTree.child(project, "parent");
            group = parent == null ? null : V2PomTree.childText(parent, "groupId");
        }
        return group;
    }

    /** Every {@code pom.xml} below {@code root}, outside build output and source directories, sorted. */
    private static SortedSet<Path> poms(Path root) throws IOException {
        SortedSet<Path> poms = new TreeSet<>();
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                return !dir.equals(root) && SKIPPED.contains(dir.getFileName().toString())
                       ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.getFileName().toString().equals("pom.xml")) {
                    poms.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return poms;
    }
}
