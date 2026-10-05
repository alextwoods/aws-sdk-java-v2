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

package software.amazon.awssdk.awscodegen;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rejects a staged generated tree, before publication, if any generated or integration-written file resolves code
 * dynamically or embeds a Smithy model (Requirements 3.3, 3.4, 3.9, 10.12).
 *
 * <p>Generated Java sources must reach operation descriptors, and every other generated type, through direct static
 * references. The guard therefore rejects, anywhere in a generated {@code .java} file (comments included, so that no
 * exemption can hide in text the compiler ignores):
 * <ul>
 *   <li>class-name lookup: {@code Class.forName}, class loaders, {@code loadClass}/{@code findClass};</li>
 *   <li>service discovery: {@code ServiceLoader};</li>
 *   <li>reflection: {@code java.lang.reflect}, reflective member lookup ({@code getDeclaredMethod},
 *       {@code getMethod("..."}, ...), {@code newInstance} and {@code MethodHandles};</li>
 *   <li>runtime model loading: Smithy's model loader package and {@code ModelAssembler}.</li>
 * </ul>
 * Any other generated file is rejected if it is a Smithy IDL file or a Smithy JSON AST, because a serialized copy of
 * the service model would be a second semantic representation.
 */
final class GeneratedSourceGuard {

    private static final List<Rule> JAVA_RULES = List.of(
        new Rule("class-name lookup", "\\bClass\\s*\\.\\s*forName\\b"),
        new Rule("class loader access", "\\bClassLoader\\b|\\.\\s*(loadClass|findClass)\\s*\\("),
        new Rule("service discovery", "\\bServiceLoader\\b"),
        new Rule("reflection", "\\bjava\\s*\\.\\s*lang\\s*\\.\\s*reflect\\b"
                               + "|\\.\\s*getDeclared(Method|Field|Constructor)s?\\s*\\("
                               + "|\\.\\s*get(Methods|Fields|Constructors?)\\s*\\("
                               + "|\\.\\s*get(Method|Field)\\s*\\(\\s*\""
                               + "|\\.\\s*newInstance\\s*\\("
                               + "|\\bMethodHandles\\b"),
        new Rule("runtime model loading", "\\bsoftware\\s*\\.\\s*amazon\\s*\\.\\s*smithy\\s*\\.\\s*model\\s*\\.\\s*loader\\b"
                                          + "|\\bModelAssembler\\b|\\bModel\\s*\\.\\s*assembler\\s*\\("));

    private static final Pattern SMITHY_JSON_AST =
        Pattern.compile("\"smithy\"\\s*:\\s*\"[0-9][^\"]*\"[\\s\\S]*\"shapes\"\\s*:|\"shapes\"\\s*:[\\s\\S]*\"smithy\"\\s*:\\s*\"[0-9]");

    private GeneratedSourceGuard() {
    }

    /**
     * Checks every staged file.
     *
     * @throws AwsCodegenException listing every violation as {@code path:line: rule}, sorted by path
     */
    static void check(StagedFileManifest manifest) {
        List<String> violations = new ArrayList<>();
        for (Path relative : manifest.relativeFiles()) {
            violations.addAll(violations(StagedFileManifest.portable(relative),
                                         new String(manifest.content(relative), StandardCharsets.UTF_8)));
        }
        if (!violations.isEmpty()) {
            throw new AwsCodegenException("Generated sources must resolve operations and types through static "
                                          + "references and must not embed a Smithy model:\n  "
                                          + String.join("\n  ", violations));
        }
    }

    /** Violations of one generated file, in line order. */
    static List<String> violations(String path, String content) {
        List<String> result = new ArrayList<>();
        if (path.endsWith(".java")) {
            String[] lines = content.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                for (Rule rule : JAVA_RULES) {
                    Matcher matcher = rule.pattern().matcher(lines[i]);
                    if (matcher.find()) {
                        result.add(path + ":" + (i + 1) + ": " + rule.name() + " (" + matcher.group().trim() + ")");
                    }
                }
            }
        } else if (path.endsWith(".smithy")) {
            result.add(path + ": serialized Smithy model (IDL)");
        } else if (SMITHY_JSON_AST.matcher(content).find()) {
            result.add(path + ": serialized Smithy model (JSON AST)");
        }
        return result;
    }

    private record Rule(String name, Pattern pattern) {
        Rule(String name, String regex) {
            this(name, Pattern.compile(regex));
        }
    }
}
