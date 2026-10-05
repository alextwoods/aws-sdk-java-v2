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

package software.amazon.awssdk.test.reactor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The artifacts Requirement 1.3 obliges the release reactor to compile, test, and package, by category. Named
 * artifacts come from the V3 design's module table; the service and test categories are rule-based because their
 * membership grows as services and suites are added.
 */
public final class RequiredArtifacts {

    /** Artifact categories named by Requirement 1.3, plus the release tooling the evidence gates depend on. */
    public enum Category {
        CODE_GENERATION,
        RUNTIME,
        HTTP,
        SERVICE,
        HIGH_LEVEL_LIBRARY,
        MIGRATION,
        TEST,
        RELEASE_TOOLING
    }

    /** Parent artifactId of every generated service module. */
    public static final String SERVICES_PARENT = "v3-services";

    private static final Map<Category, Set<String>> NAMED = new EnumMap<>(Map.of(
        Category.CODE_GENERATION, Set.of("aws-codegen", "aws-codegen-maven-plugin"),
        Category.RUNTIME, Set.of("annotations", "sdk-core", "smithy-runtime", "aws-core", "aws-pipeline",
                                 "protocol-core", "aws-json-protocol", "rest-json-protocol", "rest-xml-protocol",
                                 "aws-query-protocol", "ec2-query-protocol", "rpcv2-cbor-protocol", "auth-api",
                                 "auth-aws", "identity-spi", "identity-providers", "regions", "endpoints", "retries",
                                 "checksums", "eventstream", "telemetry-spi", "dynamic-client", "dynamic-model"),
        Category.HTTP, Set.of("http-spi", "apache-client", "apache5-client", "url-connection-client",
                              "netty-nio-client", "aws-crt-client"),
        Category.HIGH_LEVEL_LIBRARY, Set.of("dynamodb-enhanced", "s3-transfer-manager", "iam-policy-builder",
                                            "s3-event-notifications", "sns-message-manager"),
        Category.MIGRATION, Set.of("v2-migration"),
        Category.RELEASE_TOOLING, Set.of("parity-manifest", "release-evidence")));

    private RequiredArtifacts() {
    }

    /** artifactIds that must exist in the release reactor for a named category (empty for rule-based categories). */
    public static Set<String> named(Category category) {
        return NAMED.getOrDefault(category, Set.of());
    }

    /** Jar-producing modules that belong to {@code category} in {@code inventory}. */
    public static List<ReactorModule> members(ReactorInventory inventory, Category category) {
        List<ReactorModule> result = new ArrayList<>();
        for (ReactorModule module : inventory.modules()) {
            if (module.producesJar() && belongs(module, category)) {
                result.add(module);
            }
        }
        return result;
    }

    /** Returns one diagnostic per required artifact or category missing from {@code inventory}. */
    public static List<String> missing(ReactorInventory inventory) {
        List<String> problems = new ArrayList<>();
        for (Category category : Category.values()) {
            for (String artifactId : named(category)) {
                if (inventory.module(artifactId).filter(ReactorModule::producesJar).isEmpty()) {
                    problems.add(category + ": required artifact '" + artifactId + "' is not a jar module of the "
                                 + "release reactor");
                }
            }
            if (members(inventory, category).isEmpty()) {
                problems.add(category + ": the release reactor contains no module of this category");
            }
        }
        return problems;
    }

    /** True for generated service modules, which must have aws-codegen sources compiled into their jar. */
    public static boolean isService(ReactorModule module) {
        return module.producesJar() && SERVICES_PARENT.equals(module.parentArtifactId());
    }

    private static boolean belongs(ReactorModule module, Category category) {
        return switch (category) {
            case SERVICE -> isService(module);
            case TEST -> module.path().startsWith("test/");
            default -> named(category).contains(module.artifactId());
        };
    }
}
