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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.test.architecture.ClassFile.Member;
import software.amazon.awssdk.test.architecture.ClassFile.MemberRef;

/**
 * Requirements 21.8 and 21.9 on the compiled classes of every runtime module (release runtime artifacts and generated
 * services):
 * <ul>
 *     <li>runtime logging goes through the SDK facade {@value #FACADE}: no class other than the facade references
 *     JDK platform logging ({@code System.Logger}), {@code java.util.logging} or a third-party logging API, and no
 *     class writes to {@code System.out}/{@code System.err} or prints stack traces;</li>
 *     <li>no runtime artifact emits INFO events: the facade has no INFO method and references the INFO level only to
 *     answer {@code isLoggingLevelEnabled("info")}.</li>
 * </ul>
 * The classes are read from each module's {@code target/classes}, so the production modules must be compiled earlier
 * in the reactor.
 */
class RuntimeLoggingArchitectureTest {
    static final String FACADE = "software.amazon.awssdk.utils.Logger";
    private static final String FACADE_MODULE = "sdk-core";
    private static final String INFO_LEVEL_QUERY = "isLoggingLevelEnabled";
    private static final List<String> LOGGING_PREFIXES = List.of(
        "java.lang.System$Logger", "java.lang.System$LoggerFinder", "java.util.logging.", "org.slf4j.",
        "org.apache.commons.logging.", "org.apache.logging.log4j.", "org.apache.log4j.", "ch.qos.logback.",
        "org.jboss.logging.", "io.netty.util.internal.logging.");

    private static V3Reactor reactor;
    private static DependencyPolicy policy;

    @BeforeAll
    static void load() {
        ResolvedDependencyTrees trees = ResolvedDependencyTrees.load();
        reactor = trees.reactor();
        policy = trees.policy();
    }

    @Test
    void runtimeClassesLogOnlyThroughTheFacade() {
        List<String> violations = new ArrayList<>();
        Map<String, Set<String>> facadeUsers = new TreeMap<>();
        for (String module : runtimeModules()) {
            for (ClassFile type : reactor.classesOf(module).values()) {
                boolean facade = type.name().equals(FACADE) || type.name().startsWith(FACADE + "$");
                for (String reference : type.referencedClasses()) {
                    if (!facade && isLoggingApi(reference)) {
                        violations.add(module + ": " + type.name() + " references " + reference
                                       + "; log through " + FACADE);
                    }
                    if (!facade && reference.equals(FACADE)) {
                        facadeUsers.computeIfAbsent(module, m -> new TreeSet<>()).add(type.name());
                    }
                }
                for (MemberRef ref : type.memberReferences()) {
                    if (ref.owner().equals("java.lang.System") && (ref.name().equals("out") || ref.name().equals("err"))
                        || ref.name().equals("printStackTrace") && ref.descriptor().equals("()V")) {
                        violations.add(module + ": " + type.name() + " uses " + ref + "; log through " + FACADE);
                    }
                }
            }
        }
        assertEquals(List.of(), violations);
        // Not vacuous: the pipeline's request log and the high-level libraries use the facade.
        assertTrue(facadeUsers.containsKey("aws-pipeline"), facadeUsers.toString());
        assertTrue(facadeUsers.containsKey("dynamodb-enhanced"), facadeUsers.toString());
        assertTrue(facadeUsers.containsKey("s3-transfer-manager"), facadeUsers.toString());
    }

    @Test
    void theFacadeHasNoInfoLevelAndReadsItOnlyToAnswerLevelQueries() {
        ClassFile facade = reactor.classesOf(FACADE_MODULE).get(FACADE);
        assertTrue(facade != null, FACADE + " is not compiled into " + FACADE_MODULE);
        for (Member method : facade.methods()) {
            assertFalse(method.isApi() && method.name().equals("info"), "The facade declares " + method);
            boolean readsInfo = method.memberReferences().stream()
                                      .anyMatch(r -> r.owner().equals("java.lang.System$Logger$Level")
                                                     && r.name().equals("INFO"));
            assertFalse(readsInfo && !method.name().equals(INFO_LEVEL_QUERY),
                        FACADE + "." + method.name() + method.descriptor() + " reads the INFO level");
        }
        // The facade itself delegates to platform logging.
        assertTrue(facade.referencedClasses().contains("java.lang.System$Logger"));
    }

    private static boolean isLoggingApi(String reference) {
        for (String prefix : LOGGING_PREFIXES) {
            if (reference.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /** Runtime release artifacts and generated services. */
    private static Set<String> runtimeModules() {
        Set<String> result = new TreeSet<>();
        for (String module : reactor.productionModules()) {
            if (reactor.generatedCodeModules().contains(module)
                || V3Reactor.RUNTIME_KINDS.contains(policy.kindOf(module).orElseThrow())) {
                result.add(module);
            }
        }
        assertTrue(result.contains(FACADE_MODULE), result.toString());
        return result;
    }
}
