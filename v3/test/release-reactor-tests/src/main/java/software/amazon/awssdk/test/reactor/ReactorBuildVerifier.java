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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.w3c.dom.Element;

/**
 * Checks, module by module, that a completed reactor build compiled, tested, and packaged every module of a
 * {@link ReactorInventory}, and that generated service sources were produced before compilation.
 *
 * <p>Evidence comes from two independent sources: the build log (which mojo executions ran for which module, and in
 * which order) and the build outputs on disk (class files, surefire reports, jars, generated sources).
 */
public final class ReactorBuildVerifier {
    /** Class-file major version of Java release 21. */
    public static final int RELEASE_21_MAJOR_VERSION = 65;

    private static final String GENERATED_SOURCE_ROOT = "target/generated-sources/aws-codegen";
    private static final String GENERATED_RESOURCE_ROOT = "target/generated-resources/aws-codegen";
    /** Generated per-service metadata: META-INF/aws-sdk-java-v3/services/<package>/model-provenance.json etc. */
    private static final String SERVICE_METADATA = "META-INF/aws-sdk-java-v3/services/";

    private ReactorBuildVerifier() {
    }

    /** Returns every violation found; an empty list means every module was compiled, tested, and packaged. */
    public static List<String> verify(ReactorInventory inventory, List<String> buildLog) {
        List<String> problems = new ArrayList<>();
        if (buildLog.stream().noneMatch(line -> line.equals("[INFO] BUILD SUCCESS"))) {
            problems.add("build log does not report BUILD SUCCESS");
        }
        Map<String, List<String>> executions = executionsByArtifactId(buildLog);
        for (ReactorModule module : inventory.modules()) {
            verifyModule(module, executions.getOrDefault(module.artifactId(), List.of()), problems);
        }
        return problems;
    }

    /** Mojo executions per artifactId, in log order, as {@code prefix:goal (executionId)}. */
    static Map<String, List<String>> executionsByArtifactId(List<String> buildLog) {
        Map<String, List<String>> result = new HashMap<>();
        for (MojoExecution execution : MojoExecution.parse(buildLog)) {
            result.computeIfAbsent(execution.artifactId(), k -> new ArrayList<>()).add(execution.key());
        }
        return result;
    }

    private static void verifyModule(ReactorModule module, List<String> executions, List<String> problems) {
        String id = module.path() + " (" + module.artifactId() + ")";
        requireExecution(id, executions, "enforcer:enforce (v3-build-policy)", problems);
        if (!module.producesJar()) {
            return;
        }

        // Compiled: the compiler ran and every main class file is Java release 21 bytecode.
        requireExecution(id, executions, "compiler:compile (default-compile)", problems);
        Path classes = module.directory().resolve("target/classes");
        List<String> classFiles = relativeFiles(classes, ".class");
        if (classFiles.isEmpty()) {
            problems.add(id + ": no class files under target/classes");
        }
        for (String classFile : classFiles) {
            int major = majorVersion(classes.resolve(classFile));
            if (major != RELEASE_21_MAJOR_VERSION) {
                problems.add(id + ": " + classFile + " has class-file major version " + major + ", expected "
                             + RELEASE_21_MAJOR_VERSION + " (release 21)");
            }
        }

        // Tested: surefire ran; modules with test sources produced passing, non-empty reports.
        requireExecution(id, executions, "surefire:test (default-test)", problems);
        if (!relativeFiles(module.directory().resolve("src/test/java"), ".java").isEmpty()) {
            verifySurefireReports(id, module.directory().resolve("target/surefire-reports"), problems);
        }

        // Packaged: the jar exists and contains every compiled main class.
        requireExecution(id, executions, "jar:jar (default-jar)", problems);
        Set<String> entries = jarEntries(module.jar());
        if (entries == null) {
            problems.add(id + ": packaged archive " + module.jar().getFileName() + " is missing");
            return;
        }
        for (String classFile : classFiles) {
            if (!entries.contains(classFile)) {
                problems.add(id + ": " + classFile + " is missing from " + module.jar().getFileName());
            }
        }
        if ("maven-plugin".equals(module.packaging()) && !entries.contains("META-INF/maven/plugin.xml")) {
            problems.add(id + ": maven-plugin archive has no META-INF/maven/plugin.xml descriptor");
        }

        if (RequiredArtifacts.isService(module)) {
            verifyGeneratedService(id, module, executions, entries, problems);
        }
    }

    private static void verifyGeneratedService(String id, ReactorModule module, List<String> executions,
                                               Set<String> jarEntries, List<String> problems) {
        int generate = indexOf(executions, "aws-codegen:generate (aws-codegen-generate)");
        int compile = indexOf(executions, "compiler:compile (default-compile)");
        if (generate < 0) {
            problems.add(id + ": aws-codegen:generate did not run");
        } else if (compile >= 0 && generate > compile) {
            problems.add(id + ": aws-codegen:generate ran after compilation");
        }
        List<String> generated = relativeFiles(module.directory().resolve(GENERATED_SOURCE_ROOT), ".java");
        if (generated.isEmpty()) {
            problems.add(id + ": no generated sources under " + GENERATED_SOURCE_ROOT);
        }
        for (String source : generated) {
            String expectedClass = source.substring(0, source.length() - ".java".length()) + ".class";
            if (!jarEntries.contains(expectedClass)) {
                problems.add(id + ": generated source " + source + " was not compiled into the jar");
            }
        }
        // Requirements 2.12, 22.2: the generated model provenance and generation manifest ship in the service jar.
        List<String> resources = relativeFiles(module.directory().resolve(GENERATED_RESOURCE_ROOT), "");
        for (String required : List.of("model-provenance.json", "generation-manifest.json")) {
            if (resources.stream().noneMatch(r -> r.startsWith(SERVICE_METADATA) && r.endsWith("/" + required))) {
                problems.add(id + ": no generated " + required + " under " + GENERATED_RESOURCE_ROOT);
            }
        }
        for (String resource : resources) {
            if (!jarEntries.contains(resource)) {
                problems.add(id + ": generated resource " + resource + " is missing from the jar");
            }
        }
    }

    private static void verifySurefireReports(String id, Path reports, List<String> problems) {
        List<String> files = relativeFiles(reports, ".xml").stream()
                                                             .filter(f -> f.startsWith("TEST-"))
                                                             .toList();
        long tests = 0;
        for (String file : files) {
            Element suite = Xml.read(reports.resolve(file));
            tests += Long.parseLong(suite.getAttribute("tests"));
            long failed = Long.parseLong(suite.getAttribute("failures")) + Long.parseLong(suite.getAttribute("errors"));
            if (failed > 0) {
                problems.add(id + ": " + file + " reports " + failed + " failing test(s)");
            }
        }
        if (tests == 0) {
            problems.add(id + ": has test sources but surefire reported no executed tests");
        }
    }

    private static void requireExecution(String id, List<String> executions, String execution, List<String> problems) {
        if (indexOf(executions, execution) < 0) {
            problems.add(id + ": " + execution + " did not run");
        }
    }

    private static int indexOf(List<String> executions, String execution) {
        return executions.indexOf(execution);
    }

    /** Files under {@code root} with {@code suffix}, relative to root with {@code /} separators; empty if absent. */
    static List<String> relativeFiles(Path root, String suffix) {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile)
                       .filter(p -> p.getFileName().toString().endsWith(suffix))
                       .map(p -> root.relativize(p).toString().replace(root.getFileSystem().getSeparator(), "/"))
                       .sorted()
                       .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Class-file major version of {@code classFile}, or -1 if it is not a class file. */
    static int majorVersion(Path classFile) {
        try (InputStream in = Files.newInputStream(classFile); DataInputStream data = new DataInputStream(in)) {
            if (data.readInt() != 0xCAFEBABE) {
                return -1;
            }
            data.readUnsignedShort(); // minor version
            return data.readUnsignedShort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Set<String> jarEntries(Path jar) {
        if (!Files.isRegularFile(jar)) {
            return null;
        }
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            Set<String> entries = new HashSet<>();
            zip.stream().map(ZipEntry::getName).forEach(entries::add);
            return entries;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
