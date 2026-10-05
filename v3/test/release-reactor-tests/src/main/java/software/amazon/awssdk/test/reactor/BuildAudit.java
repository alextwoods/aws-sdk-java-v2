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
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Build audits for Requirement 1: the V3 build never invokes a Gradle executable (1.5), rejects unsupported JDKs in the
 * validate phase before any source generation or compilation and leaves no class files (1.6, 1.7), and runs source
 * generation, compilation, tests, and packaging in order, with every upstream module packaged before it is used (1.4).
 *
 * <p>Every method returns the list of violations found; an empty list means the audit passed.
 *
 * <p>The Gradle audit looks for <em>invocations</em>, not for the word: POM comments, descriptions and diagnostic
 * messages may mention Gradle freely. It flags
 * <ul>
 *   <li>Gradle build or wrapper files outside {@code src/} (where they can only be test data),</li>
 *   <li>POM plugins, dependencies and extensions that launch Gradle ({@code org.gradle} coordinates, plugins named
 *       {@code *gradle*}) and Gradle executables or launcher classes in plugin configuration or POM properties,</li>
 *   <li>Gradle executables in non-comment lines of build scripts,</li>
 *   <li>Gradle output in a build log and Gradle processes started by a build.</li>
 * </ul>
 */
public final class BuildAudit {
    /** artifactId of the parent POM that carries the V3 build policy. */
    public static final String V3_PARENT = "aws-sdk-java-v3-parent";
    /** The enforcer execution that rejects unsupported JDKs. */
    public static final String BUILD_POLICY_EXECUTION = "enforcer:enforce (v3-build-policy)";

    /**
     * A Gradle executable used as a command or path: {@code gradle}, {@code ./gradlew}, {@code gradlew.bat},
     * {@code /opt/gradle/bin/gradle}. Case-sensitive, and not part of a longer name ({@code build.gradle},
     * {@code gradle-wrapper}, {@code org.gradle}, {@code gradle/}) or prose ({@code Gradle}).
     */
    static final Pattern GRADLE_EXECUTABLE = Pattern.compile(
        "(?<![\\w.-])gradlew?(?:\\.bat|\\.cmd|\\.exe)?(?![\\w./\\\\-])");
    /** Classes that start a Gradle build from a JVM (wrapper, launcher, daemon, Tooling API). */
    static final Pattern GRADLE_LAUNCHER = Pattern.compile(
        "GradleWrapperMain|GradleDaemon|org\\.gradle\\.launcher\\.|org\\.gradle\\.tooling\\.GradleConnector");

    private static final Set<String> GRADLE_BUILD_FILES = Set.of(
        "build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts", "gradle.properties", "gradlew",
        "gradlew.bat", "gradle-wrapper.properties", "gradle-wrapper.jar");
    /** Configuration elements and attributes that hold human-readable text, never a command. */
    private static final Set<String> PROSE = Set.of("message", "regexMessage", "description", "name", "echo", "url",
                                                    "comment", "comments");
    private static final Set<String> HASH_COMMENT_SCRIPTS = Set.of("sh", "bash", "zsh", "ksh", "py", "rb", "pl",
                                                                   "ps1", "mk", "Makefile", "GNUmakefile",
                                                                   "Dockerfile");
    private static final Set<String> SLASH_COMMENT_SCRIPTS = Set.of("groovy", "gvy", "kts", "Jenkinsfile");
    private static final Set<String> BATCH_SCRIPTS = Set.of("bat", "cmd");
    private static final List<Pattern> GRADLE_LOG_MARKERS = List.of(
        Pattern.compile("Welcome to Gradle"),
        Pattern.compile("Starting a Gradle Daemon"),
        Pattern.compile("services\\.gradle\\.org/distributions"),
        Pattern.compile("^> Task :"),
        Pattern.compile("^BUILD (SUCCESSFUL|FAILED) in \\d"),
        GRADLE_LAUNCHER);

    private BuildAudit() {
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Requirement 1.5: no Gradle executable
    // ---------------------------------------------------------------------------------------------------------------

    /** Gradle build files, POM configuration, and build scripts under {@code v3Root} (build output excluded). */
    public static List<String> gradleInSourceTree(Path v3Root) {
        List<String> problems = new ArrayList<>();
        for (Path file : sourceFiles(v3Root)) {
            String relative = v3Root.relativize(file).toString().replace(file.getFileSystem().getSeparator(), "/");
            String name = file.getFileName().toString();
            boolean underSrc = ("/" + relative).contains("/src/");
            if (GRADLE_BUILD_FILES.contains(name) && !underSrc) {
                problems.add(relative + ": Gradle build file; the V3 build is Maven-only");
            }
            if (name.equals("pom.xml") || (name.equals("extensions.xml") && relative.contains(".mvn/"))) {
                gradleInPom(relative, Xml.read(file), problems);
            } else if (relative.contains(".mvn/") && (name.equals("maven.config") || name.equals("jvm.config"))) {
                scanLines(relative, file, null, problems);
            } else if (!("/" + relative).contains("/src/test/resources/")) {
                String kind = scriptKind(name);
                if (kind != null) {
                    scanLines(relative, file, kind, problems);
                }
            }
        }
        return problems.stream().distinct().toList();
    }

    /** Evidence in a Maven build log that Gradle ran. */
    public static List<String> gradleInBuildLog(List<String> buildLog) {
        List<String> problems = new ArrayList<>();
        for (int i = 0; i < buildLog.size(); i++) {
            String line = buildLog.get(i);
            for (Pattern marker : GRADLE_LOG_MARKERS) {
                if (marker.matcher(line).find()) {
                    problems.add("build log line " + (i + 1) + " shows Gradle output: " + line.strip());
                    break;
                }
            }
        }
        for (MojoExecution execution : MojoExecution.parse(buildLog)) {
            if (execution.prefix().toLowerCase(Locale.ROOT).contains("gradle")) {
                problems.add("build log line " + (execution.line() + 1) + ": " + execution.key() + " @ "
                             + execution.artifactId() + " runs a Gradle build");
            }
        }
        return problems;
    }

    /** Gradle processes among the command lines of processes a build started (see {@link MavenFork.Result}). */
    public static List<String> gradleProcesses(Collection<String> commandLines) {
        List<String> problems = new ArrayList<>();
        for (String commandLine : commandLines) {
            boolean executable = Stream.of(commandLine.trim().split("\\s+"))
                                       .map(token -> token.substring(Math.max(token.lastIndexOf('/'),
                                                                              token.lastIndexOf('\\')) + 1))
                                       .anyMatch(token -> GRADLE_EXECUTABLE.matcher(token).matches());
            if (executable || GRADLE_LAUNCHER.matcher(commandLine).find()) {
                problems.add("the build started a Gradle process: " + commandLine);
            }
        }
        return problems;
    }

    private static void gradleInPom(String file, Element project, List<String> problems) {
        for (String container : List.of("plugin", "dependency", "extension")) {
            for (Element element : descendants(project, container)) {
                String groupId = Xml.text(element, "groupId").orElse("");
                String artifactId = Xml.text(element, "artifactId").orElse("");
                if (groupId.equals("org.gradle") || groupId.startsWith("org.gradle.")
                    || container.equals("plugin") && artifactId.toLowerCase(Locale.ROOT).contains("gradle")) {
                    problems.add(file + ": " + container + " " + groupId + ":" + artifactId + " launches Gradle");
                }
            }
        }
        List<Element> scanned = new ArrayList<>(descendants(project, "plugin"));
        scanned.addAll(descendants(project, "properties"));
        for (Element root : scanned) {
            scanValues(file, root, problems);
        }
    }

    /** Checks text and attribute values below {@code element}, skipping prose elements and attributes. */
    private static void scanValues(String file, Element element, List<String> problems) {
        if (PROSE.contains(element.getTagName())) {
            return;
        }
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attribute = (Attr) attributes.item(i);
            if (!PROSE.contains(attribute.getName())) {
                checkValue(file, "<" + element.getTagName() + " " + attribute.getName() + ">", attribute.getValue(),
                           problems);
            }
        }
        for (Node n = element.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element child) {
                scanValues(file, child, problems);
            } else if (n.getNodeType() == Node.TEXT_NODE || n.getNodeType() == Node.CDATA_SECTION_NODE) {
                checkValue(file, "<" + element.getTagName() + ">", n.getNodeValue(), problems);
            }
        }
    }

    private static void checkValue(String file, String where, String value, List<String> problems) {
        if (GRADLE_EXECUTABLE.matcher(value).find() || GRADLE_LAUNCHER.matcher(value).find()) {
            problems.add(file + ": " + where + " invokes Gradle: " + value.strip());
        }
    }

    private static void scanLines(String file, Path path, String kind, List<String> problems) {
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        for (int i = 0; i < lines.size(); i++) {
            String code = stripComment(lines.get(i), kind);
            if (GRADLE_EXECUTABLE.matcher(code).find() || GRADLE_LAUNCHER.matcher(code).find()) {
                problems.add(file + ":" + (i + 1) + " invokes Gradle: " + lines.get(i).strip());
            }
        }
    }

    /** The code part of {@code line}; comment syntax depends on the script kind ({@code null}: no comments). */
    static String stripComment(String line, String kind) {
        if (kind == null) {
            return line;
        }
        String trimmed = line.strip();
        if (BATCH_SCRIPTS.contains(kind)) {
            String upper = trimmed.toUpperCase(Locale.ROOT);
            return upper.startsWith("REM ") || upper.equals("REM") || upper.startsWith("::") ? "" : line;
        }
        if (SLASH_COMMENT_SCRIPTS.contains(kind)) {
            if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
                return "";
            }
            int comment = line.indexOf(" //");
            return comment < 0 ? line : line.substring(0, comment);
        }
        if (trimmed.startsWith("#!")) {
            return line; // a shebang line is an interpreter invocation, not a comment
        }
        if (trimmed.startsWith("#")) {
            return "";
        }
        int comment = line.indexOf(" #");
        return comment < 0 ? line : line.substring(0, comment);
    }

    private static String scriptKind(String name) {
        if (HASH_COMMENT_SCRIPTS.contains(name) || SLASH_COMMENT_SCRIPTS.contains(name)) {
            return name;
        }
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? "" : name.substring(dot + 1);
        return HASH_COMMENT_SCRIPTS.contains(extension) || SLASH_COMMENT_SCRIPTS.contains(extension)
               || BATCH_SCRIPTS.contains(extension) ? extension : null;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Requirements 1.6 and 1.7: every module enforces the JDK minimum before generation and compilation
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * Every module of {@code inventory} inherits {@value #V3_PARENT} through reactor parents, and no POM skips the
     * enforcer or moves the {@code v3-build-policy} execution out of the validate phase.
     */
    public static List<String> buildPolicyCoverage(ReactorInventory inventory) {
        List<String> problems = new ArrayList<>();
        for (ReactorModule module : inventory.modules()) {
            String id = module.path() + " (" + module.artifactId() + ")";
            ReactorModule current = module;
            Set<String> visited = new HashSet<>();
            while (!V3_PARENT.equals(current.artifactId())) {
                String parent = current.parentArtifactId();
                if (parent == null || !visited.add(parent)) {
                    problems.add(id + ": does not inherit " + V3_PARENT + " (chain ends at " + current.artifactId()
                                 + ")");
                    break;
                }
                if (V3_PARENT.equals(parent)) {
                    break;
                }
                ReactorModule next = inventory.module(parent).orElse(null);
                if (next == null) {
                    problems.add(id + ": parent " + parent + " is not a V3 reactor module, so " + V3_PARENT
                                 + " is not inherited");
                    break;
                }
                current = next;
            }
            enforcerOverrides(id, Xml.read(module.directory().resolve("pom.xml")), problems);
        }
        return problems;
    }

    private static void enforcerOverrides(String id, Element project, List<String> problems) {
        for (Element properties : descendants(project, "properties")) {
            Xml.text(properties, "enforcer.skip").filter(v -> !v.equals("false"))
               .ifPresent(v -> problems.add(id + ": sets enforcer.skip=" + v));
        }
        for (Element plugin : descendants(project, "plugin")) {
            if (!"maven-enforcer-plugin".equals(Xml.text(plugin, "artifactId").orElse(null))) {
                continue;
            }
            Xml.text(plugin, "configuration", "skip").filter(v -> !v.equals("false"))
               .ifPresent(v -> problems.add(id + ": configures maven-enforcer-plugin skip=" + v));
            for (Element execution : Xml.children(Xml.child(plugin, "executions"), "execution")) {
                if (!"v3-build-policy".equals(Xml.text(execution, "id").orElse(null))) {
                    continue;
                }
                Xml.text(execution, "phase").filter(v -> !v.equals("validate"))
                   .ifPresent(v -> problems.add(id + ": binds v3-build-policy to phase " + v + " instead of validate"));
                Xml.text(execution, "configuration", "skip").filter(v -> !v.equals("false"))
                   .ifPresent(v -> problems.add(id + ": configures v3-build-policy skip=" + v));
            }
        }
    }

    /**
     * A build rejected for its JDK: it failed in {@code v3-build-policy} with the diagnostic naming
     * {@code minimumJdk}, no mojo other than that check ran, nothing was compiled, and {@code checkoutRoot} contains no
     * class files, generated sources or resources, or archives.
     */
    public static List<String> rejectedBeforeCompilation(List<String> buildLog, Path checkoutRoot, int minimumJdk) {
        List<String> problems = new ArrayList<>();
        if (buildLog.stream().noneMatch("[INFO] BUILD FAILURE"::equals)) {
            problems.add("build log does not report BUILD FAILURE");
        }
        Pattern failedGoal = Pattern.compile("^\\[ERROR] Failed to execute goal org\\.apache\\.maven\\.plugins:"
                                             + "maven-enforcer-plugin:\\S+:enforce \\(v3-build-policy\\) on project ");
        if (buildLog.stream().noneMatch(line -> failedGoal.matcher(line).find())) {
            problems.add("the build did not fail in the " + BUILD_POLICY_EXECUTION + " execution");
        }
        String diagnostic = "requires JDK " + minimumJdk + " or newer (minimum supported JDK: " + minimumJdk + ")";
        if (buildLog.stream().noneMatch(line -> line.startsWith("[ERROR]") && line.contains(diagnostic))) {
            problems.add("no [ERROR] diagnostic identifies JDK " + minimumJdk + " as the minimum ('" + diagnostic
                         + "')");
        }
        for (MojoExecution execution : MojoExecution.parse(buildLog)) {
            if (!BUILD_POLICY_EXECUTION.equals(execution.key())) {
                problems.add(execution.key() + " ran in " + execution.artifactId() + " although the JDK was rejected");
            }
        }
        for (int i = 0; i < buildLog.size(); i++) {
            if (buildLog.get(i).matches("^\\[INFO] Compiling \\d+ source files?.*")) {
                problems.add("build log line " + (i + 1) + " shows compilation: " + buildLog.get(i));
            }
        }
        for (Path file : allFiles(checkoutRoot)) {
            String relative = checkoutRoot.relativize(file).toString().replace(file.getFileSystem().getSeparator(),
                                                                                 "/");
            if (relative.endsWith(".class")) {
                problems.add(relative + ": class file produced by a rejected build");
            } else if (("/" + relative).contains("/target/") && (relative.endsWith(".jar")
                                                                  || relative.contains("/generated-sources/")
                                                                  || relative.contains("/generated-resources/")
                                                                  || relative.contains("/classes/"))) {
                problems.add(relative + ": build output produced by a rejected build");
            }
        }
        return problems;
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Requirement 1.4: generated sources before compilation; compile, test, package in order; upstream first
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * Checks a successful build of exactly the modules of {@code expected}: each module ran the JDK/build-policy check
     * before anything but {@code clean}; generated service modules ran {@code aws-codegen:generate} before
     * {@code compiler:compile}; jar modules compiled before testing and tested before packaging; and every upstream
     * V3 module (dependency, build plugin, or parent) finished before a module that uses it started, so that for
     * example {@code aws-codegen-maven-plugin} is packaged before any module runs {@code aws-codegen:generate}.
     */
    public static List<String> lifecycleOrdering(ReactorInventory expected, List<String> buildLog) {
        List<String> problems = new ArrayList<>();
        Map<String, List<MojoExecution>> byModule = new LinkedHashMap<>();
        for (MojoExecution execution : MojoExecution.parse(buildLog)) {
            byModule.computeIfAbsent(execution.artifactId(), k -> new ArrayList<>()).add(execution);
        }
        for (String artifactId : byModule.keySet()) {
            if (expected.module(artifactId).isEmpty()) {
                problems.add(artifactId + " was built but is not part of the expected reactor");
            }
        }
        for (ReactorModule module : expected.modules()) {
            String id = module.path() + " (" + module.artifactId() + ")";
            List<MojoExecution> executions = byModule.get(module.artifactId());
            if (executions == null) {
                problems.add(id + ": was not built");
                continue;
            }
            int policy = index(executions, BUILD_POLICY_EXECUTION);
            if (policy < 0) {
                problems.add(id + ": " + BUILD_POLICY_EXECUTION + " did not run");
            } else {
                executions.subList(0, policy).stream()
                          .filter(e -> !e.prefix().equals("clean"))
                          .forEach(e -> problems.add(id + ": " + e.key() + " ran before " + BUILD_POLICY_EXECUTION));
            }
            if (module.producesJar()) {
                int compile = index(executions, "compiler:compile (default-compile)");
                int test = index(executions, "surefire:test (default-test)");
                int jar = index(executions, "jar:jar (default-jar)");
                requireBefore(id, executions, compile, jar, problems);
                requireBefore(id, executions, compile, test, problems);
                requireBefore(id, executions, test, jar, problems);
                if (RequiredArtifacts.isService(module)) {
                    int generate = index(executions, "aws-codegen:generate (aws-codegen-generate)");
                    if (generate < 0) {
                        problems.add(id + ": aws-codegen:generate did not run");
                    }
                    requireBefore(id, executions, generate, compile, problems);
                }
            }
            int start = executions.get(0).line();
            Set<String> upstream = new HashSet<>(module.v3References());
            if (module.parentArtifactId() != null) {
                upstream.add(module.parentArtifactId());
            }
            upstream.remove(module.artifactId());
            for (String reference : upstream.stream().sorted().toList()) {
                List<MojoExecution> referenced = byModule.get(reference);
                if (referenced != null && referenced.get(referenced.size() - 1).line() > start) {
                    problems.add(id + ": started before upstream module " + reference + " finished");
                }
            }
        }
        return problems;
    }

    private static void requireBefore(String id, List<MojoExecution> executions, int first, int second,
                                      List<String> problems) {
        if (first >= 0 && second >= 0 && first > second) {
            problems.add(id + ": " + executions.get(first).key() + " ran after " + executions.get(second).key());
        }
    }

    private static int index(List<MojoExecution> executions, String key) {
        for (int i = 0; i < executions.size(); i++) {
            if (executions.get(i).key().equals(key)) {
                return i;
            }
        }
        return -1;
    }

    // ---------------------------------------------------------------------------------------------------------------

    private static List<Element> descendants(Element root, String name) {
        List<Element> result = new ArrayList<>();
        for (Node n = root.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e) {
                if (name.equals(e.getTagName())) {
                    result.add(e);
                }
                result.addAll(descendants(e, name));
            }
        }
        return result;
    }

    /** Regular files under {@code root}, excluding {@code target/} build output and VCS/IDE metadata. */
    private static List<Path> sourceFiles(Path root) {
        List<Path> files = new ArrayList<>();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String name = dir.getFileName() == null ? "" : dir.getFileName().toString();
                    boolean skip = !dir.equals(root) && Set.of("target", ".git", ".idea", "node_modules")
                                                           .contains(name);
                    return skip ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isRegularFile()) {
                        files.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        files.sort(null);
        return files;
    }

    private static List<Path> allFiles(Path root) {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
