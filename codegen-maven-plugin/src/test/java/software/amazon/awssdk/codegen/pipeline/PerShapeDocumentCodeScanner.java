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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds per-shape document conversion code in a tree of generated Java sources: what Codegen must not generate,
 * because document conversion reuses the generated schemas through {@code Document.of} and {@code asShape} (design B.6,
 * Requirement 24.1). Used by {@code NoPerShapeDocumentCodeTest}.
 *
 * <p>Each {@link Rule} is matched against the source with its comments removed
 * ({@link LegacyRouteParity#withoutComments}), so that documentation that mentions a converter is not reported.
 *
 * <p>Requirement 24.1 exempts three things, the design's exempt registries:
 *
 * <ul>
 *     <li>the Primary_Type_Registry (Requirement 22) and the Error_Registry (Requirement 23): their declarations
 *     ({@link ExemptRegistry}) are removed from the source before the rules are matched, so that a registry entry may
 *     name whatever the registry needs. The rest of the declaring class is scanned;</li>
 *     <li>the Legacy_Document handling of members that target {@code document} shapes (Requirement 21): generated code
 *     converts those members through the shared runtime methods {@code SchemaSerdeSupport.wrapLegacy} and
 *     {@code unwrapLegacy}, which may delegate to the shared runtime {@code core.document.DocumentConverter}. No rule
 *     matches these names: {@link Rule#DOCUMENT_CONVERSION_TYPE} reports a reference to a converter type only when
 *     the type name has a prefix before {@code Document} (a per-shape type such as {@code FooDocumentConverter}),
 *     and reports a bare {@code DocumentConverter} only where generated code declares it.</li>
 * </ul>
 */
final class PerShapeDocumentCodeScanner {
    private PerShapeDocumentCodeScanner() {
    }

    /**
     * A kind of per-shape document conversion code.
     */
    enum Rule {
        /**
         * A method or field named {@code toDocument} or {@code fromDocument}, declared, called or referenced
         * ({@code Foo::toDocument}): a per-shape conversion to or from a Document.
         */
        DOCUMENT_METHOD(Pattern.compile("\\b(?:to|from)Document\\b")),

        /**
         * A per-shape document converter, serializer or deserializer type: a type named
         * {@code *DocumentConverter}, {@code *DocumentSerializer} or {@code *DocumentDeserializer} that generated code
         * declares (with any prefix, including none), or a reference to such a type whose name has a prefix before
         * {@code Document}. The shared runtime types {@code DocumentConverter} (Requirement 21) and
         * {@code DocumentDeserializer} (Requirement 19) may be referenced.
         */
        DOCUMENT_CONVERSION_TYPE(Pattern.compile(
            "\\b(?:(?:class|interface|enum)\\s+\\w*|[A-Z]\\w*)Document(?:Converter|Serializer|Deserializer)\\b"));

        private final Pattern pattern;

        Rule(Pattern pattern) {
            this.pattern = pattern;
        }
    }

    /**
     * A registry that Requirement 24.1 exempts, by the declaration that Codegen generates for it: a field of a class
     * named {@code <Service><classSuffix>}.
     *
     * <p>A declaration is matched up to its first {@code ;}. The registries' entries are expression lambdas
     * ({@code () -> FooSchemas.BAR}), so this is the end of the declaration; a statement lambda would end the match
     * early and the rest of the declaration would be scanned, which can only report more.
     */
    enum ExemptRegistry {
        /**
         * The Primary_Type_Registry, {@code <Service>TypeRegistry.INSTANCE} (Requirement 22, task 11.16).
         */
        PRIMARY_TYPE_REGISTRY("TypeRegistry", Pattern.compile("\\bTypeRegistry\\s+INSTANCE\\s*=[^;]*;")),

        /**
         * The Error_Registry, {@code <Service>Operations.ERROR_REGISTRY} (Requirement 23).
         */
        ERROR_REGISTRY("Operations", Pattern.compile("\\bErrorRegistry\\s+ERROR_REGISTRY\\s*=[^;]*;"));

        private final String classSuffix;
        private final Pattern declaration;

        ExemptRegistry(String classSuffix, Pattern declaration) {
            this.classSuffix = classSuffix;
            this.declaration = declaration;
        }

        /**
         * @param file the file's path relative to its source root, with {@code /} separators
         * @return whether the file is named {@code <Service><classSuffix>.java}, with a non-empty service name
         */
        boolean appliesTo(String file) {
            String simpleName = file.substring(file.lastIndexOf('/') + 1);
            String suffix = classSuffix + ".java";
            return simpleName.endsWith(suffix) && simpleName.length() > suffix.length();
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
     * An exempt registry declaration that was removed from a file before it was scanned.
     */
    static final class Exemption {
        private final String file;
        private final ExemptRegistry registry;

        Exemption(String file, ExemptRegistry registry) {
            this.file = file;
            this.registry = registry;
        }

        String file() {
            return file;
        }

        ExemptRegistry registry() {
            return registry;
        }

        @Override
        public String toString() {
            return file + ": " + registry;
        }
    }

    /**
     * The violations found in a scan, and the exempt registry declarations it skipped.
     */
    static final class Result {
        private final List<Violation> violations = new ArrayList<>();
        private final List<Exemption> exemptions = new ArrayList<>();

        List<Violation> violations() {
            return Collections.unmodifiableList(violations);
        }

        List<Exemption> exemptions() {
            return Collections.unmodifiableList(exemptions);
        }
    }

    /**
     * Scans every {@code .java} file under the given roots (a root that does not exist is skipped). Each rule is
     * reported at most once per file, with its first match.
     *
     * @param roots the generated source directories
     * @return the violations, ordered by root, file path and rule, and the exemptions applied
     */
    static Result scan(Path... roots) {
        Result result = new Result();
        for (Path root : roots) {
            for (Path file : PerOperationSerdeScanner.javaFiles(root)) {
                String relative = root.relativize(file).toString().replace('\\', '/');
                scan(relative, PerOperationSerdeScanner.read(file), result);
            }
        }
        return result;
    }

    /**
     * @param file the file's path relative to its source root, with {@code /} separators
     * @param source the file's content
     * @return the violations in the file, in rule order, and the exemptions applied
     */
    static Result scan(String file, String source) {
        Result result = new Result();
        scan(file, source, result);
        return result;
    }

    private static void scan(String file, String source, Result result) {
        String code = LegacyRouteParity.withoutComments(source);
        for (ExemptRegistry registry : ExemptRegistry.values()) {
            if (registry.appliesTo(file)) {
                Matcher m = registry.declaration.matcher(code);
                if (m.find()) {
                    result.exemptions.add(new Exemption(file, registry));
                    code = m.replaceAll(" ");
                }
            }
        }
        for (Rule rule : Rule.values()) {
            Matcher m = rule.pattern.matcher(code);
            if (m.find()) {
                result.violations.add(new Violation(file, rule, m.group().replaceAll("\\s+", " ")));
            }
        }
    }
}
