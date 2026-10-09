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

package software.amazon.awssdk.test.modulepath;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The smoke suites' reading of dependency-policy.xml detects each forbidden class and honors its waivers. */
class ForbiddenDependenciesTest {
    private static ForbiddenDependencies policy;

    @BeforeAll
    static void load() throws IOException {
        policy = ForbiddenDependencies.load(SmokeFixture.policy());
    }

    @Test
    void detectsForbiddenCoordinatesAndV3ModuleKinds() {
        assertThat(policy.violations("software.amazon.awssdk:sdk-core:2.54.0")).containsExactly("v2-runtime");
        assertThat(policy.violations("ch.qos.logback:logback-classic:1.5.6")).containsExactly("logging-implementation");
        assertThat(policy.violations("software.amazon.smithy:smithy-build:1.73.0")).containsExactly("codegen");
        assertThat(policy.violations("software.amazon.awssdk:dynamic-client:3.0.0-SNAPSHOT"))
            .containsExactly("smithy-model-parser");
        assertThat(policy.violations("software.amazon.awssdk:aws-codegen:3.0.0-SNAPSHOT")).containsExactly("codegen");
        assertThat(policy.violations("software.amazon.awssdk:bounded-memory-tests:3.0.0-SNAPSHOT"))
            .containsExactly("non-production");
    }

    @Test
    void allowsRuntimeArtifactsAndWaivedCoordinates() {
        assertThat(policy.violations("software.amazon.awssdk:sdk-core:3.0.0-SNAPSHOT")).isEmpty();
        assertThat(policy.violations("software.amazon.awssdk:netty-nio-client:3.0.0-SNAPSHOT")).isEmpty();
        assertThat(policy.violations("io.netty:netty-handler:4.1.138.Final")).isEmpty();
        assertThat(policy.violations("software.amazon.smithy:smithy-model:1.73.0"))
            .as("waiver smithy-model-shape-identity").isEmpty();
    }
}
