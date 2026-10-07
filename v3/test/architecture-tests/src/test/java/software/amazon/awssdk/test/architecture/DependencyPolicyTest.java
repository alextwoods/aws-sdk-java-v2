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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.test.architecture.DependencyPolicy.Coordinate;
import software.amazon.awssdk.test.architecture.DependencyPolicy.EdgeRules;
import software.amazon.awssdk.test.architecture.DependencyPolicy.ModuleRule;

/** Internal consistency of the dependency policy and the semantics of its evaluation API. */
class DependencyPolicyTest {

    /** Requirement 15.2 concerns that must each map to owning artifacts. */
    private static final Set<String> REQUIRED_CONCERNS = Set.of(
        "public-api", "schema-serde", "protocol-runtime", "aws-pipeline", "auth", "identity", "endpoint", "retry",
        "checksum", "telemetry", "http-spi", "http-implementation", "dynamic-model", "codegen", "service",
        "high-level-library");

    private static final Set<String> NON_RELEASE_KINDS = Set.of("release-tooling", "test");

    private static final String DYNAMIC_KIND = "dynamic";
    private static final String MODEL_PARSER = "smithy-model-parser";

    private static final Set<String> PROTOCOLS = Set.of("aws-json-protocol", "rest-json-protocol", "rest-xml-protocol",
                                                        "aws-query-protocol", "ec2-query-protocol",
                                                        "rpcv2-cbor-protocol");

    private final DependencyPolicy policy = DependencyPolicy.load();

    @Test
    void generatedPolicyCarriesTheReleaseVersion() {
        assertEquals(System.getProperty("v3.releaseVersion"), policy.releaseVersion());
        assertEquals("software.amazon.awssdk", policy.groupId());
    }

    @Test
    void everyRequiredConcernHasAnOwningArtifact() {
        Map<String, DependencyPolicy.Concern> concerns = new HashMap<>();
        policy.concerns().forEach(c -> concerns.put(c.id(), c));
        for (String id : REQUIRED_CONCERNS) {
            DependencyPolicy.Concern concern = concerns.get(id);
            assertTrue(concern != null, "Missing concern " + id);
            assertFalse(concern.owners().isEmpty() && concern.ownerKinds().isEmpty(), "Concern without owner: " + id);
        }
    }

    @Test
    void releaseFlagMatchesModuleKind() {
        for (ModuleRule module : policy.modules().values()) {
            assertEquals(!NON_RELEASE_KINDS.contains(module.kind()), module.release(),
                         module.artifactId() + " (" + module.kind() + ")");
        }
    }

    @Test
    void requiredModuleEdgesAreAcyclic() {
        for (ModuleRule module : policy.modules().values()) {
            assertFalse(policy.requiredClosure(module.rules(), Set.of()).contains(module.artifactId()),
                        module.artifactId() + " transitively requires itself");
        }
    }

    @Test
    void noModuleReachesAModuleItForbids() {
        List<String> violations = new ArrayList<>();
        for (ModuleRule module : policy.modules().values()) {
            violations.addAll(forbiddenReach(module.artifactId(), module.rules(), Set.of()));
        }
        for (String protocol : PROTOCOLS) {
            violations.addAll(forbiddenReach("service[" + protocol + "]", policy.serviceTemplate(), Set.of(protocol)));
        }
        assertEquals(List.of(), violations);
    }

    @Test
    void requiredArtifactsAreAllowedOrWaived() {
        List<String> violations = new ArrayList<>();
        for (ModuleRule module : policy.modules().values()) {
            for (Coordinate artifact : module.rules().requiredArtifacts()) {
                policy.checkDirectDependency(module.artifactId(), artifact).ifPresent(violations::add);
            }
        }
        assertEquals(List.of(), violations);
    }

    @Test
    void theDynamicLayerIsAnOptionalModelParserArtifactForbiddenOutsideIt() {
        // Requirements 14.18 and 15.5: dynamic-model parses runtime models and dynamic-client requires it, so both
        // belong to the smithy-model-parser class, which the service template and every runtime module outside the
        // dynamic layer forbid; only the dynamic layer itself and test modules may depend on them.
        String v = policy.releaseVersion();
        Set<String> dynamic = new TreeSet<>();
        policy.modules().values().stream().filter(m -> m.kind().equals(DYNAMIC_KIND))
              .forEach(m -> dynamic.add(m.artifactId()));
        assertEquals(Set.of("dynamic-client", "dynamic-model"), dynamic);
        List<String> violations = new ArrayList<>();
        dynamic.stream().filter(m -> !policy.matches(MODEL_PARSER, v3(m, v)))
               .forEach(m -> violations.add(m + " is not in artifact class " + MODEL_PARSER));
        if (!policy.serviceTemplate().forbids().contains(MODEL_PARSER)) {
            violations.add("the service template does not forbid " + MODEL_PARSER);
        }
        Set<String> templateEdges = new TreeSet<>(policy.serviceTemplate().allowedModules());
        templateEdges.retainAll(dynamic);
        if (!templateEdges.isEmpty()) {
            violations.add("the service template lists dynamic modules " + templateEdges);
        }
        for (ModuleRule module : policy.modules().values()) {
            boolean runtime = V3Reactor.RUNTIME_KINDS.contains(module.kind()) && !module.kind().equals(DYNAMIC_KIND);
            if (runtime && !module.rules().forbids().contains(MODEL_PARSER)) {
                violations.add(module.artifactId() + " (" + module.kind() + ") does not forbid " + MODEL_PARSER);
            }
            Set<String> edges = new TreeSet<>(module.rules().allowedModules());
            edges.retainAll(dynamic);
            if (!edges.isEmpty() && !Set.of(DYNAMIC_KIND, "test").contains(module.kind())) {
                violations.add(module.artifactId() + " (" + module.kind() + ") lists dynamic modules " + edges);
            }
        }
        assertEquals(List.of(), violations);

        // Declared edges: a generated service, the pipeline or a high-level library cannot declare either dynamic
        // module; the dynamic client declares the dynamic model.
        DependencyPolicy withS3 = policy.withServiceModules(Set.of("s3"));
        for (String module : dynamic) {
            assertTrue(withS3.checkDirectDependency("s3", v3(module, v)).orElseThrow().contains(MODEL_PARSER), module);
            for (String runtime : List.of("aws-pipeline", "aws-json-protocol", "url-connection-client",
                                          "dynamodb-enhanced")) {
                assertTrue(policy.checkDirectDependency(runtime, v3(module, v)).orElseThrow().contains(MODEL_PARSER),
                           runtime + " -> " + module);
            }
        }
        assertTrue(policy.checkDirectDependency("dynamic-client", v3("dynamic-model", v)).isEmpty());
    }

    @Test
    void waiversAreScopedToTheirViaModule() {
        assertFalse(policy.waivers().isEmpty());
        for (DependencyPolicy.Waiver waiver : policy.waivers()) {
            Coordinate artifact = new Coordinate(waiver.groupId(), waiver.artifactId(), "");
            assertTrue(policy.matches(waiver.classId(), artifact), waiver.id() + " does not match its class");
            assertFalse(waiver.enforcement().isBlank(), waiver.id() + " names no replacement check");
            assertTrue(policy.modules().get(waiver.via()).rules().requiredArtifacts().contains(artifact),
                       waiver.id() + " is not declared by its via module");
            // Any other module that forbids the class may not declare the artifact itself.
            for (ModuleRule module : policy.modules().values()) {
                if (!module.artifactId().equals(waiver.via()) && module.rules().forbids().contains(waiver.classId())) {
                    assertTrue(policy.checkDirectDependency(module.artifactId(), artifact).isPresent(),
                               module.artifactId() + " may declare waived " + artifact);
                }
            }
        }
    }

    @Test
    void minimalServiceTreeIsTheDocumentedRequiredRuntimeSet() {
        // Requirement 15.9: the documented service-and-required-runtime set for a restXml service such as S3. It
        // includes the Telemetry SPI contract (task 16.2: the pipeline's telemetry stage and the generated builders'
        // telemetry settings), never an exporter (Requirement 15.7).
        Set<String> minimal = policy.requiredClosure(policy.serviceTemplate(), Set.of("rest-xml-protocol"));
        assertEquals(new TreeSet<>(Set.of("aws-core", "aws-pipeline", "auth-api", "checksums", "endpoints",
                                          "http-spi", "identity-spi", "protocol-core", "regions",
                                          "rest-xml-protocol", "retries", "sdk-core", "smithy-runtime",
                                          "telemetry-spi")),
                     minimal);
        assertThrows(IllegalArgumentException.class,
                     () -> policy.requiredClosure(policy.serviceTemplate(), Set.of()));
    }

    @Test
    void evaluatesModuleEdges() {
        String v = policy.releaseVersion();
        assertTrue(policy.checkDirectDependency("aws-pipeline", v3("http-spi", v)).isEmpty());
        assertTrue(policy.checkDirectDependency("aws-core", v3("aws-pipeline", v)).orElseThrow()
                         .contains("not listed in <requires> or <allows>"));
        assertTrue(policy.checkDirectDependency("aws-core", v3("sdk-core", "3.999.0")).orElseThrow()
                         .contains("must be at exactly " + v));
        assertTrue(policy.checkDirectDependency("sdk-core", v3("netty-nio-client", v)).orElseThrow()
                         .contains("concrete-http"));
    }

    @Test
    void evaluatesThirdPartyEdges() {
        assertTrue(policy.checkDirectDependency("sdk-core", new Coordinate("software.amazon.awssdk", "annotations",
                                                                           "2.25.50"))
                         .orElseThrow().contains("v2-runtime"));
        assertTrue(policy.checkDirectDependency("netty-nio-client", new Coordinate("io.netty", "netty-codec-http",
                                                                                   "4.1.118.Final"))
                         .isEmpty());
        assertTrue(policy.checkDirectDependency("apache-client", new Coordinate("io.netty", "netty-codec-http",
                                                                                "4.1.118.Final"))
                         .orElseThrow().contains("async-engine"));
        assertTrue(policy.checkDirectDependency("sdk-core", new Coordinate("com.example", "unknown", "1.0"))
                         .orElseThrow().contains("no <allowsClass>"));
        assertTrue(policy.checkDirectDependency("smithy-runtime", new Coordinate("ch.qos.logback", "logback-classic",
                                                                                 "1.5.0"))
                         .orElseThrow().contains("logging-implementation"));
    }

    @Test
    void waiverAppliesOnlyThroughItsViaModule() {
        Coordinate smithyModel = new Coordinate("software.amazon.smithy", "smithy-model", "1.73.0");
        assertTrue(policy.checkDirectDependency("smithy-runtime", smithyModel).isEmpty());
        assertTrue(policy.checkDirectDependency("aws-core", smithyModel).orElseThrow().contains("smithy-model-parser"));
        EdgeRules service = policy.serviceTemplate();
        assertEquals(List.of(), policy.forbiddenClasses(service, smithyModel, Set.of("aws-core", "smithy-runtime")));
        assertEquals(List.of("smithy-model-parser"), policy.forbiddenClasses(service, smithyModel, Set.of("aws-core")));
        Coordinate dynamicSchemas = new Coordinate("software.amazon.smithy.java", "dynamic-schemas", "1.6.1");
        assertEquals(List.of("smithy-model-parser"),
                     policy.forbiddenClasses(service, dynamicSchemas, Set.of("smithy-runtime")));
    }

    @Test
    void serviceModulesUseTheServiceTemplate() {
        DependencyPolicy withS3 = policy.withServiceModules(Set.of("s3"));
        String v = policy.releaseVersion();
        assertTrue(withS3.releaseArtifacts().contains("s3"));
        assertTrue(withS3.checkDirectDependency("s3", v3("rest-xml-protocol", v)).isEmpty());
        assertTrue(withS3.checkDirectDependency("s3", v3("apache5-client", v)).orElseThrow().contains("concrete-http"));
        assertTrue(withS3.checkDirectDependency("aws-pipeline", v3("s3", v)).orElseThrow()
                         .contains("generated-service"));
    }

    private List<String> forbiddenReach(String from, EdgeRules rules, Set<String> choices) {
        List<String> violations = new ArrayList<>();
        Set<String> reachable = new HashSet<>(rules.allowedModules());
        reachable.addAll(policy.requiredClosure(rules, choices));
        for (String target : reachable) {
            Coordinate coordinate = v3(target, policy.releaseVersion());
            List<String> forbidden = policy.forbiddenClasses(rules, coordinate, Set.of());
            if (!forbidden.isEmpty()) {
                violations.add(from + " reaches " + target + " forbidden by " + forbidden);
            }
        }
        return violations;
    }

    private static Coordinate v3(String artifactId, String version) {
        return new Coordinate("software.amazon.awssdk", artifactId, version);
    }
}
