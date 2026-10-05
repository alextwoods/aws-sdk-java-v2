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

package software.amazon.awssdk.test.generated.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Compiles one caller source in-test and reports its error diagnostics. */
public final class Javac {

    private static final JavaCompiler JAVAC = Objects.requireNonNull(ToolProvider.getSystemJavaCompiler(),
                                                                     "the tests must run on a JDK");

    private Javac() {
    }

    /** The test class path: the generated V3 classes and their runtime. */
    public static String testClassPath() {
        return System.getProperty("java.class.path");
    }

    /**
     * Compiles {@code source} (the public class {@code className}, in the default package) into {@code classes}
     * against {@code classPath}, and returns its error diagnostics; an empty list means it compiled.
     */
    public static List<String> compile(String className, String source, String classPath, Path classes) {
        try {
            Path sources = Files.createDirectories(classes.resolveSibling(classes.getFileName() + "-src"));
            Path file = sources.resolve(className + ".java");
            Files.writeString(file, source, StandardCharsets.UTF_8);
            Files.createDirectories(classes);
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            try (StandardJavaFileManager files = JAVAC.getStandardFileManager(diagnostics, Locale.ROOT,
                                                                              StandardCharsets.UTF_8)) {
                List<String> options = List.of("-proc:none", "-implicit:none", "--release", "21", "-classpath",
                                               classPath, "-d", classes.toString());
                JAVAC.getTask(null, files, diagnostics, options, null, files.getJavaFileObjects(file)).call();
            }
            return diagnostics.getDiagnostics().stream()
                              .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
                              .map(d -> d.getMessage(Locale.ROOT))
                              .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
