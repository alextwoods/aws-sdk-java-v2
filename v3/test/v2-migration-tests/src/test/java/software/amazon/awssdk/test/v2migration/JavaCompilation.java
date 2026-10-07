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

package software.amazon.awssdk.test.v2migration;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Compiles a corpus project's main sources with the running JDK's compiler, as its Maven build would. */
final class JavaCompilation {

    /** One compiler error: the project-relative file, its 1-based line and the UTF-16 offset into the file. */
    record Error(String path, long line, long column, long position, String code, String message) {
        @Override
        public String toString() {
            return path + ":" + line + ":" + column + ": " + message.lines().findFirst().orElse("");
        }
    }

    /** The errors (empty on success) and the compiled classes. */
    record Result(List<Error> errors, Path classes) {
        boolean succeeded() {
            return errors.isEmpty();
        }
    }

    private JavaCompilation() {
    }

    /** Every {@code .java} file under {@code projectRoot}/src/main/java, sorted. */
    static List<Path> sources(Path projectRoot) throws IOException {
        Path root = projectRoot.resolve("src/main/java");
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(f -> f.toString().endsWith(".java")).sorted().toList();
        }
    }

    /** Compiles the sources of {@code projectRoot} for {@code release} against {@code classpath} into {@code out}. */
    static Result compile(Path projectRoot, int release, List<Path> classpath, Path out) throws IOException {
        Files.createDirectories(out);
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        List<Error> errors = new ArrayList<>();
        try (StandardJavaFileManager files = javac.getStandardFileManager(diagnostics, Locale.ROOT,
                                                                          StandardCharsets.UTF_8)) {
            List<String> options = new ArrayList<>(List.of(
                "--release", Integer.toString(release), "-encoding", "UTF-8", "-proc:none", "-nowarn",
                "-Xlint:-options", "-d", out.toString(), "-classpath",
                classpath.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator))));
            StringWriter output = new StringWriter();
            boolean ok = javac.getTask(output, files, diagnostics, options, null,
                                       files.getJavaFileObjectsFromPaths(sources(projectRoot))).call();
            for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                if (d.getKind() != Diagnostic.Kind.ERROR) {
                    continue;
                }
                String path = d.getSource() == null ? "(none)"
                                                    : relative(projectRoot, Path.of(d.getSource().toUri()));
                // The preferred position: the offending name (for example the member of a call chain), not the start
                // of the enclosing expression.
                errors.add(new Error(path, d.getLineNumber(), d.getColumnNumber(), d.getPosition(), d.getCode(),
                                     d.getMessage(Locale.ROOT)));
            }
            if (!ok && errors.isEmpty()) {
                errors.add(new Error("(none)", 0, 0, -1, "compiler.failed", output.toString()));
            }
        }
        return new Result(List.copyOf(errors), out);
    }

    static String relative(Path root, Path file) {
        Path absolute = file.toAbsolutePath().normalize();
        Path base = root.toAbsolutePath().normalize();
        return (absolute.startsWith(base) ? base.relativize(absolute) : absolute).toString()
            .replace(File.separatorChar, '/');
    }
}
