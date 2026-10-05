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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import software.amazon.awssdk.smithy.runtime.internal.SmithyRuntimeBoundary;

/**
 * Class-name rules for the dependency policy's forbidden artifact classes, used by bytecode inspection. The policy
 * identifies artifacts by Maven coordinates; these rules identify the classes of those artifacts, so that a
 * reference is caught even when the artifact reaches the class path unexpectedly (for example shaded into an allowed
 * jar, or as a compile-only dependency).
 *
 * <p>Artifact classes in {@link #TREE_ONLY_CLASSES} have no class-name rule because their classes share packages
 * with allowed V3 modules ({@code software.amazon.awssdk}) or have no stable package. Bytecode references to them are
 * caught by resolution instead: every class a runtime module references must be defined by the JDK, the module or an
 * artifact in its runtime dependency tree, and the tree itself is checked against the policy.
 */
public final class ForbiddenClassNames {

    /** Artifact classes enforced for bytecode through dependency-tree resolution only. */
    public static final Set<String> TREE_ONLY_CLASSES = Set.of(
        "v2-runtime", "generated-protocol-marshaller", "generated-service", "non-production");

    private static final List<String> APACHE_HTTP = List.of("org.apache.http.", "org.apache.hc.");
    private static final List<String> NETTY = List.of("io.netty.");

    private static final Map<String, Predicate<String>> RULES = new LinkedHashMap<>();

    static {
        RULES.put("codegen", prefixes(List.of("software.amazon.smithy.build.", "software.amazon.smithy.codegen.",
                                              "software.amazon.smithy.traitcodegen.",
                                              "software.amazon.smithy.java.codegen.")));
        // The class layer of the smithy-model-shape-identity waiver: smithy-model's shape-identity, trait and node
        // classes are allowed; its parser, assembler, semantic model and tooling classes are not, nor are the classes
        // of the other artifacts of the class (waiters, rules engine, AWS/protocol traits, Smithy Java dynamic
        // schemas) and of the V3 dynamic layer (software.amazon.awssdk.dynamic).
        RULES.put("smithy-model-parser", SmithyRuntimeBoundary::isForbiddenRuntimeClass);
        RULES.put("concrete-http", prefixes(concat(APACHE_HTTP, NETTY, List.of("software.amazon.awssdk.crt."))));
        RULES.put("async-engine", prefixes(NETTY));
        RULES.put("sync-engine", prefixes(APACHE_HTTP));
        // Micrometer registries and Smithy Java's OpenTelemetry metrics bridge have no common package; the
        // dependency-tree check covers them by coordinates.
        RULES.put("telemetry-exporter", prefixes(List.of("io.opentelemetry.sdk.", "io.opentelemetry.exporter.")));
        RULES.put("logging-implementation", prefixes(List.of(
            "ch.qos.logback.",                      // Logback
            "org.apache.logging.log4j.core.",       // Log4j 2 implementation (log4j-api is a facade)
            "org.apache.logging.slf4j.",            // log4j-slf4j-impl / log4j-slf4j2-impl bindings
            "org.apache.log4j.",                    // Log4j 1.x and reload4j
            "org.slf4j.impl.",                      // SLF4J 1.x bindings (StaticLoggerBinder)
            "org.slf4j.simple.",                    // slf4j-simple
            "org.slf4j.nop.",                       // slf4j-nop
            "org.slf4j.jul.",                       // slf4j-jdk14
            "org.slf4j.reload4j.",                  // slf4j-reload4j
            "org.apache.commons.logging.impl.")));  // commons-logging implementations
    }

    private ForbiddenClassNames() {
    }

    /** Artifact class IDs that have a class-name rule. */
    public static Set<String> ruleClasses() {
        return RULES.keySet();
    }

    /** IDs of the artifact classes in {@code candidates} whose class-name rule matches {@code binaryName}. */
    public static List<String> classify(String binaryName, Set<String> candidates) {
        List<String> result = new ArrayList<>();
        RULES.forEach((id, rule) -> {
            if (candidates.contains(id) && rule.test(binaryName)) {
                result.add(id);
            }
        });
        return result;
    }

    /** True if {@code binaryName} is a third-party logging implementation class (Requirement 15.8). */
    public static boolean isLoggingImplementation(String binaryName) {
        return RULES.get("logging-implementation").test(binaryName);
    }

    @SafeVarargs
    private static List<String> concat(List<String>... lists) {
        List<String> result = new ArrayList<>();
        for (List<String> list : lists) {
            result.addAll(list);
        }
        return result;
    }

    private static Predicate<String> prefixes(List<String> prefixes) {
        List<String> copy = List.copyOf(prefixes);
        return name -> copy.stream().anyMatch(name::startsWith);
    }
}
