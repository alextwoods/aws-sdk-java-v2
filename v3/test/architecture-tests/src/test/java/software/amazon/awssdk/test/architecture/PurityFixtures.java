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
import java.util.stream.Stream;
import software.amazon.awssdk.test.architecture.DependencyPolicy.Coordinate;

/**
 * Synthetic modules resolved together with the real V3 reactor by {@link ResolvedDependencyTrees}: minimal generated
 * services (one per protocol the service template accepts), services that leak each forbidden artifact class
 * directly or transitively, a service missing required edges, and the stand-alone third-party and V2 artifacts they
 * depend on. Fixture artifacts that impersonate forbidden third-party coordinates use {@link #FAKE_VERSION}; they
 * exist only as modules of the forked fixture reactor and are never built or installed.
 */
final class PurityFixtures {

    static final String GROUP = "software.amazon.awssdk";
    static final String FIXTURE_GROUP = "com.example.architecture.fixture";
    static final String FIXTURE_VERSION = "1.0.0";
    static final String FAKE_VERSION = "0.0.0-architecture-fixture";
    static final String SERVICE_PREFIX = "architecture-fixture-";
    static final String MISSING_REQUIRED_EDGES = SERVICE_PREFIX + "missing-required-edges";
    /**
     * A minimal aws-json-protocol service that declares the dynamic client. Its diagnostics are checked by path
     * prefix rather than exactly: the dynamic client reaches every protocol, auth and identity module, so the exact
     * set of forbidden artifacts below it follows those modules' trees.
     */
    static final String DYNAMIC_CLIENT_LEAK = SERVICE_PREFIX + "leaks-dynamic-client";

    private PurityFixtures() {
    }

    /** A dependency of a fixture module. */
    record Dep(Coordinate coordinate, boolean optional) {
    }

    /** A stand-alone fixture module (no parent) with compile-scope dependencies. */
    record Module(Coordinate coordinate, List<Dep> dependencies) {
    }

    /** A leaking service fixture and the exact diagnostics the inspector must produce for it. */
    record LeakFixture(String service, List<Dep> extraDependencies, List<String> expectedViolations) {
    }

    static Dep v3(String artifactId, String release) {
        return new Dep(new Coordinate(GROUP, artifactId, release), false);
    }

    static Dep dep(String groupId, String artifactId, String version) {
        return new Dep(new Coordinate(groupId, artifactId, version), false);
    }

    /** Stand-alone third-party and V2 fixture artifacts. */
    static List<Module> libraries(String smithyVersion) {
        return List.of(
            module(GROUP, "architecture-fixture-v2-sdk-core", "2.99.0"),
            module(FIXTURE_GROUP, "legacy-adapter", FIXTURE_VERSION,
                   dep(GROUP, "architecture-fixture-v2-sdk-core", "2.99.0")),
            module(FIXTURE_GROUP, "model-tools", FIXTURE_VERSION,
                   dep("software.amazon.smithy", "smithy-model", smithyVersion)),
            module(GROUP, "architecture-fixture-json-marshaller", FAKE_VERSION),
            module(FIXTURE_GROUP, "protocol-bridge", FIXTURE_VERSION,
                   dep(GROUP, "architecture-fixture-json-marshaller", FAKE_VERSION)),
            module("io.netty", "netty-transport", FAKE_VERSION),
            module(FIXTURE_GROUP, "http-toolkit", FIXTURE_VERSION, dep("io.netty", "netty-transport", FAKE_VERSION)),
            module("io.opentelemetry", "opentelemetry-exporter-otlp", FAKE_VERSION),
            module(FIXTURE_GROUP, "metrics-toolkit", FIXTURE_VERSION,
                   dep("io.opentelemetry", "opentelemetry-exporter-otlp", FAKE_VERSION)),
            module("ch.qos.logback", "logback-classic", FAKE_VERSION),
            module(FIXTURE_GROUP, "logging-toolkit", FIXTURE_VERSION,
                   dep("ch.qos.logback", "logback-classic", FAKE_VERSION)));
    }

    /**
     * The required dependencies of a minimal service: the template's {@code <requires>} modules plus one module per
     * {@code <requiresAnyOf>} group ({@code protocol} for the group containing it, the first module otherwise).
     */
    static List<Dep> minimalDependencies(DependencyPolicy policy, String protocol) {
        List<Dep> result = new ArrayList<>();
        policy.serviceTemplate().requires().forEach(m -> result.add(v3(m, policy.releaseVersion())));
        for (Set<String> group : policy.serviceTemplate().requiresAnyOf()) {
            result.add(v3(group.contains(protocol) ? protocol : group.iterator().next(), policy.releaseVersion()));
        }
        return result;
    }

    /** Every module of the template's {@code <requiresAnyOf>} groups, in policy order. */
    static List<String> protocols(DependencyPolicy policy) {
        return policy.serviceTemplate().requiresAnyOf().stream().flatMap(Set::stream).distinct().toList();
    }

    static String minimalService(String protocol) {
        return SERVICE_PREFIX + "minimal-" + protocol;
    }

    /** Each leak fixture adds forbidden dependencies to a minimal aws-json-protocol service. */
    static List<LeakFixture> leaks(String release, String smithyVersion, String smithyJavaVersion) {
        return List.of(
            new LeakFixture(SERVICE_PREFIX + "leaks-v2-runtime",
                            List.of(dep(FIXTURE_GROUP, "legacy-adapter", FIXTURE_VERSION)),
                            List.of(path(release, "leaks-v2-runtime", FIXTURE_GROUP + ":legacy-adapter:1.0.0",
                                         GROUP + ":architecture-fixture-v2-sdk-core:2.99.0")
                                    + ": forbidden artifact class [v2-runtime]")),
            // aws-codegen and every Smithy build/codegen/model artifact it reaches are reported, each by its own path.
            new LeakFixture(SERVICE_PREFIX + "leaks-codegen",
                            List.of(v3("aws-codegen", release)),
                            codegenLeaks(release, smithyVersion)),
            new LeakFixture(SERVICE_PREFIX + "leaks-smithy-model-parser",
                            List.of(dep(FIXTURE_GROUP, "model-tools", FIXTURE_VERSION), v3("dynamic-model", release)),
                            modelParserLeaks(release, smithyVersion, smithyJavaVersion)),
            new LeakFixture(SERVICE_PREFIX + "leaks-generated-protocol-marshaller",
                            List.of(dep(FIXTURE_GROUP, "protocol-bridge", FIXTURE_VERSION)),
                            List.of(path(release, "leaks-generated-protocol-marshaller",
                                         FIXTURE_GROUP + ":protocol-bridge:1.0.0",
                                         GROUP + ":architecture-fixture-json-marshaller:" + FAKE_VERSION)
                                    + ": forbidden artifact class [generated-protocol-marshaller]")),
            new LeakFixture(SERVICE_PREFIX + "leaks-concrete-http",
                            // url-connection-client is the HTTP implementation with no engine (its policy forbids
                            // concrete-http), so this fixture's diagnostics do not change with the engine trees of
                            // the other transports.
                            List.of(new Dep(new Coordinate(GROUP, "url-connection-client", release), true),
                                    dep(FIXTURE_GROUP, "http-toolkit", FIXTURE_VERSION)),
                            // Declaring an HTTP implementation optional does not make the service tree clean.
                            List.of(path(release, "leaks-concrete-http", GROUP + ":url-connection-client:" + release)
                                    + ": forbidden artifact class [concrete-http]",
                                    path(release, "leaks-concrete-http", FIXTURE_GROUP + ":http-toolkit:1.0.0",
                                         "io.netty:netty-transport:" + FAKE_VERSION)
                                    + ": forbidden artifact class [concrete-http]")),
            new LeakFixture(SERVICE_PREFIX + "leaks-telemetry-exporter",
                            List.of(dep(FIXTURE_GROUP, "metrics-toolkit", FIXTURE_VERSION)),
                            List.of(path(release, "leaks-telemetry-exporter", FIXTURE_GROUP + ":metrics-toolkit:1.0.0",
                                         "io.opentelemetry:opentelemetry-exporter-otlp:" + FAKE_VERSION)
                                    + ": forbidden artifact class [telemetry-exporter]")),
            new LeakFixture(SERVICE_PREFIX + "leaks-logging-implementation",
                            List.of(dep(FIXTURE_GROUP, "logging-toolkit", FIXTURE_VERSION)),
                            List.of(path(release, "leaks-logging-implementation",
                                         FIXTURE_GROUP + ":logging-toolkit:1.0.0",
                                         "ch.qos.logback:logback-classic:" + FAKE_VERSION)
                                    + ": forbidden artifact class [logging-implementation]")));
    }

    /** Every fixture service module (the modules the policy must treat as generated services). */
    static Map<String, Module> services(DependencyPolicy policy, String smithyVersion,
                                        String smithyJavaVersion) {
        String release = policy.releaseVersion();
        Map<String, Module> result = new LinkedHashMap<>();
        for (String protocol : protocols(policy)) {
            String id = minimalService(protocol);
            result.put(id, new Module(new Coordinate(GROUP, id, release), minimalDependencies(policy, protocol)));
        }
        String defaultProtocol = protocols(policy).get(0);
        for (LeakFixture leak : leaks(release, smithyVersion, smithyJavaVersion)) {
            List<Dep> deps = new ArrayList<>(minimalDependencies(policy, defaultProtocol));
            deps.addAll(leak.extraDependencies());
            result.put(leak.service(), new Module(new Coordinate(GROUP, leak.service(), release), List.copyOf(deps)));
        }
        List<Dep> dynamicClientLeak = new ArrayList<>(minimalDependencies(policy, defaultProtocol));
        dynamicClientLeak.add(v3("dynamic-client", release));
        result.put(DYNAMIC_CLIENT_LEAK, new Module(new Coordinate(GROUP, DYNAMIC_CLIENT_LEAK, release),
                                                   List.copyOf(dynamicClientLeak)));
        result.put(MISSING_REQUIRED_EDGES,
                   new Module(new Coordinate(GROUP, MISSING_REQUIRED_EDGES, release),
                              List.of(v3("sdk-core", release), v3("smithy-runtime", release))));
        return result;
    }

    /** A node of an expected verbose dependency tree: a Smithy artifact (or aws-codegen) and its expanded children. */
    private record TreeNode(String artifactId, List<TreeNode> children) {
    }

    private static TreeNode node(String artifactId, TreeNode... children) {
        return new TreeNode(artifactId, List.of(children));
    }

    /**
     * Diagnostics for a service that depends on aws-codegen: one per path to every forbidden artifact in the verbose
     * tree Maven resolves below aws-codegen. Verbose trees expand an artifact's dependencies only at its winning
     * occurrence; every other occurrence is a leaf. smithy-utils and smithy-jmespath are not listed because no
     * service rule forbids them.
     */
    private static List<String> codegenLeaks(String release, String smithyVersion) {
        TreeNode codegen = node(
            "aws-codegen",
            node("smithy-codegen-core", node("smithy-build"), node("smithy-model")),
            node("smithy-model"),
            node("smithy-build", node("smithy-model")),
            node("smithy-aws-traits", node("smithy-diff", node("smithy-model")), node("smithy-model"),
                 node("smithy-protocol-traits")),
            node("smithy-protocol-traits", node("smithy-model")),
            node("smithy-rules-engine", node("smithy-build"), node("smithy-model-jmespath", node("smithy-model")),
                 node("smithy-model")),
            node("smithy-aws-endpoints", node("smithy-aws-traits"), node("smithy-diff"), node("smithy-model"),
                 node("smithy-rules-engine")),
            node("smithy-waiters", node("smithy-model-jmespath"), node("smithy-model")));
        List<String> result = new ArrayList<>();
        collectLeaks(release, smithyVersion, null, "leaks-codegen", codegen, List.of(), result);
        return result;
    }

    /**
     * Diagnostics for a service that depends on a third-party artifact built on the Smithy model (the fixture
     * model-tools) and on dynamic-model: the model-tools path to smithy-model, and one per path to dynamic-model and
     * every forbidden artifact Maven resolves below it, as for {@link #codegenLeaks}. smithy-build is not below
     * dynamic-model: its pom excludes it from smithy-rules-engine and smithy-aws-endpoints.
     */
    private static List<String> modelParserLeaks(String release, String smithyVersion, String smithyJavaVersion) {
        TreeNode dynamicModel = node(
            "dynamic-model",
            node("smithy-model"),
            node("smithy-aws-traits", node("smithy-diff", node("smithy-model")), node("smithy-model"),
                 node("smithy-protocol-traits")),
            node("smithy-protocol-traits", node("smithy-model")),
            node("smithy-rules-engine", node("smithy-model-jmespath", node("smithy-model")), node("smithy-model")),
            node("smithy-aws-endpoints", node("smithy-aws-traits"), node("smithy-diff"), node("smithy-model"),
                 node("smithy-rules-engine")),
            node("smithy-waiters", node("smithy-model-jmespath"), node("smithy-model")),
            node("dynamic-schemas"));
        List<String> result = new ArrayList<>();
        result.add(path(release, "leaks-smithy-model-parser", FIXTURE_GROUP + ":model-tools:1.0.0",
                        "software.amazon.smithy:smithy-model:" + smithyVersion)
                   + ": forbidden artifact class [smithy-model-parser]");
        collectLeaks(release, smithyVersion, smithyJavaVersion, "leaks-smithy-model-parser", dynamicModel, List.of(),
                     result);
        return result;
    }

    private static void collectLeaks(String release, String smithyVersion, String smithyJavaVersion, String service,
                                     TreeNode node, List<String> parents, List<String> result) {
        String artifactId = node.artifactId();
        List<String> nodes = new ArrayList<>(parents);
        if (Set.of("aws-codegen", "dynamic-model").contains(artifactId)) {
            nodes.add(GROUP + ":" + artifactId + ":" + release);
        } else if (artifactId.equals("dynamic-schemas")) {
            nodes.add("software.amazon.smithy.java:" + artifactId + ":" + smithyJavaVersion);
        } else {
            nodes.add("software.amazon.smithy:" + artifactId + ":" + smithyVersion);
        }
        String artifactClass = Set.of("aws-codegen", "smithy-build", "smithy-codegen-core").contains(artifactId)
                               ? "codegen" : "smithy-model-parser";
        result.add(path(release, service, nodes.toArray(String[]::new))
                   + ": forbidden artifact class [" + artifactClass + "]");
        node.children().forEach(child -> collectLeaks(release, smithyVersion, smithyJavaVersion, service, child,
                                                      nodes, result));
    }

    private static String path(String release, String service, String... nodes) {
        return String.join(" -> ", Stream.concat(Stream.of(GROUP + ":" + SERVICE_PREFIX + service + ":" + release),
                                                 Stream.of(nodes)).toList());
    }

    private static Module module(String groupId, String artifactId, String version, Dep... dependencies) {
        return new Module(new Coordinate(groupId, artifactId, version), List.of(dependencies));
    }
}
