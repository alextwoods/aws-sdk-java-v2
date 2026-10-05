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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/** Compiles fixture sources with the JDK compiler, for bytecode-inspection tests. */
final class JavaFixtures {

    private static final Pattern TYPE_DECLARATION = Pattern.compile("(?:class|interface|enum|record)\\s+(\\w+)");
    private static final Pattern PACKAGE_DECLARATION = Pattern.compile("\\bpackage\\s+([\\w.]+)\\s*;");
    private static final Pattern BLOCK_COMMENT = Pattern.compile("(?s)/\\*.*?\\*/");

    private JavaFixtures() {
    }

    /**
     * Compiles single-type sources (the type name is taken from the first "class X", "interface X", "enum X" or
     * "record X") to {@code work/name} and returns that directory.
     */
    static Path compile(Path work, String name, List<Path> classPath, String... sources) throws IOException {
        Path sourceRoot = work.resolve(name + "-src");
        Path output = Files.createDirectories(work.resolve(name));
        List<String> arguments = new ArrayList<>(List.of("-d", output.toString(), "--release", "21", "-proc:none"));
        if (!classPath.isEmpty()) {
            arguments.add("-classpath");
            arguments.add(String.join(File.pathSeparator, classPath.stream().map(Path::toString).toList()));
        }
        for (String source : sources) {
            // Block comments (license headers, Javadoc) may precede the package and mention "class" in prose.
            String code = BLOCK_COMMENT.matcher(source).replaceAll(" ");
            Matcher packageDeclaration = PACKAGE_DECLARATION.matcher(code);
            assertTrue(packageDeclaration.find(), source);
            String packageName = packageDeclaration.group(1);
            Matcher type = TYPE_DECLARATION.matcher(code);
            assertTrue(type.find(), source);
            Path file = sourceRoot.resolve(packageName.replace('.', '/')).resolve(type.group(1) + ".java");
            MavenFork.write(file, source);
            arguments.add(file.toString());
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        int exit = compiler.run(null, diagnostics, diagnostics, arguments.toArray(String[]::new));
        assertEquals(0, exit, "Fixture compilation failed: " + diagnostics.toString(StandardCharsets.UTF_8));
        return output;
    }
}
