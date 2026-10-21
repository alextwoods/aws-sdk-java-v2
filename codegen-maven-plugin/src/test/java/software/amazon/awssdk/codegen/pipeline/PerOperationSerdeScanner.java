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

package software.amazon.awssdk.codegen.pipeline;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Finds the legacy per-operation serde components in a tree of generated Java sources: what a schema-based service no
 * longer generates (task 5.1, design A.15 "Generators removed", Requirements 17.12, 36.4, 37.2). Used by
 * {@code NoTransformPackageTest}.
 *
 * <p>Each {@link Rule} is matched against the source with its comments removed
 * ({@link LegacyRouteParity#withoutComments}), so that documentation that mentions a marshaller or a transform package
 * is not reported.
 */
final class PerOperationSerdeScanner {
    private static final Pattern PACKAGE_DECLARATION = Pattern.compile("\\bpackage\\s+([\\w.]+)\\s*;");

    private PerOperationSerdeScanner() {
    }

    /**
     * A kind of legacy per-operation serde component.
     */
    enum Rule {
        /**
         * A class in a {@code transform} package: the per-protocol marshallers ({@code MarshallerGeneratorTasks}).
         */
        TRANSFORM_PACKAGE(null),

        /**
         * A reference to a class in any {@code transform} package, by import or by qualified name: the service's own
         * marshallers, or the legacy {@code software.amazon.awssdk.core.runtime.transform.Marshaller} they implement.
         */
        TRANSFORM_REFERENCE(Pattern.compile("\\b(?:[a-z_][a-z0-9_]*\\.)+transform\\.(?:[A-Z]\\w*|\\*)")),

        /**
         * A type named {@code *Marshaller} or {@code *Unmarshaller}, declared or referenced: a generated marshaller, or a
         * legacy marshaller or unmarshaller type of the protocol modules.
         */
        MARSHALLER_TYPE(Pattern.compile("\\b(?:[A-Z]\\w*)?(?:Marshaller|Unmarshaller)\\b")),

        /**
         * The per-operation exception mapper of the legacy JSON-family client methods ({@code JsonProtocolSpec}): a
         * {@code Function<String, Optional<ExceptionMetadata>>} lambda, {@code exceptionMetadataMapper} or
         * {@code eventstreamExceptionMetadataMapper}, whose body (after an optional null check) switches on the error
         * code with one {@code case} per modeled error.
         */
        EXCEPTION_MAPPER_SWITCH(Pattern.compile(
            "\\b\\w*[eE]xceptionMetadataMapper\\s*=\\s*errorCode\\s*->\\s*\\{"
            + "(?:\\s*if\\s*\\(\\s*errorCode\\s*==\\s*null\\s*\\)\\s*\\{"
            + "\\s*return\\s+Optional\\s*\\.\\s*empty\\s*\\(\\s*\\)\\s*;\\s*\\})?"
            + "\\s*switch\\s*\\(\\s*errorCode\\s*\\)")),

        /**
         * A reference to the legacy {@code ExceptionMetadata} type, which every legacy protocol's exception mapping
         * uses: the JSON-family switch cases and the {@code registerModeledException} calls with which the query and
         * XML clients configure their protocol factory ({@code ProtocolSpec}). A schema-based client resolves errors
         * through the service's Error_Registry (Requirement 36.4).
         */
        EXCEPTION_METADATA(Pattern.compile("\\bExceptionMetadata\\b"));

        private final Pattern pattern;

        Rule(Pattern pattern) {
            this.pattern = pattern;
        }
    }

    /**
     * One match of a rule in a generated file.
     */
    static final class Violation {
        private final String file;
        private final Rule rule;
        private final String match;

        Violation(String file, Rule rule, String match) {
            this.file = file;
            this.rule = rule;
            this.match = match;
        }

        String file() {
            return file;
        }

        Rule rule() {
            return rule;
        }

        String match() {
            return match;
        }

        @Override
        public String toString() {
            return file + ": " + rule + ": " + match;
        }
    }

    /**
     * Scans every {@code .java} file under the given roots (a root that does not exist is skipped). Each rule is
     * reported at most once per file, with its first match.
     *
     * @param roots the generated source directories
     * @return the violations, ordered by root, file path and rule
     */
    static List<Violation> scan(Path... roots) {
        List<Violation> violations = new ArrayList<>();
        for (Path root : roots) {
            for (Path file : javaFiles(root)) {
                String relative = root.relativize(file).toString().replace('\\', '/');
                violations.addAll(scan(relative, read(file)));
            }
        }
        return violations;
    }

    /**
     * @param file the file's path relative to its source root, with {@code /} separators
     * @param source the file's content
     * @return the violations in the file, in rule order
     */
    static List<Violation> scan(String file, String source) {
        String code = LegacyRouteParity.withoutComments(source);
        List<Violation> violations = new ArrayList<>();
        String transformPackage = transformPackage(file, code);
        if (transformPackage != null) {
            violations.add(new Violation(file, Rule.TRANSFORM_PACKAGE, transformPackage));
        }
        for (Rule rule : Rule.values()) {
            if (rule.pattern == null) {
                continue;
            }
            Matcher m = rule.pattern.matcher(code);
            if (m.find()) {
                violations.add(new Violation(file, rule, m.group().replaceAll("\\s+", " ")));
            }
        }
        return violations;
    }

    /**
     * The file's {@code transform} package, from its declaration or else its directory, or null if it is in none.
     */
    private static String transformPackage(String file, String code) {
        Matcher declaration = PACKAGE_DECLARATION.matcher(code);
        if (declaration.find()) {
            String name = declaration.group(1);
            if (("." + name + ".").contains(".transform.")) {
                return "package " + name;
            }
        }
        int lastSlash = file.lastIndexOf('/');
        String directory = lastSlash < 0 ? "" : file.substring(0, lastSlash);
        for (String segment : directory.split("/")) {
            if (segment.equals("transform")) {
                return "directory " + directory;
            }
        }
        return null;
    }

    /**
     * The {@code .java} files under the root, sorted by path, or none if the root is not a directory. Also used by
     * {@link PerShapeDocumentCodeScanner}.
     */
    static List<Path> javaFiles(Path root) {
        if (!Files.isDirectory(root)) {
            return new ArrayList<>();
        }
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(f -> Files.isRegularFile(f) && f.toString().endsWith(".java"))
                       .sorted()
                       .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String read(Path file) {
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
