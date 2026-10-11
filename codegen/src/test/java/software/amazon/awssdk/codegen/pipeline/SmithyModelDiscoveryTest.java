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

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

class SmithyModelDiscoveryTest {

    @Test
    void protocolTestModelJars_areRecognized() throws Exception {
        assertThat(SmithyModelDiscovery.isProtocolTestModels(new URL(
            "jar:file:/repo/software/amazon/smithy/smithy-aws-protocol-tests/1.71.0/smithy-aws-protocol-tests-1.71.0.jar"
            + "!/META-INF/smithy/manifest"))).isTrue();
        assertThat(SmithyModelDiscovery.isProtocolTestModels(new URL(
            "jar:file:/repo/smithy-protocol-tests-1.71.0.jar!/META-INF/smithy/manifest"))).isTrue();
    }

    @Test
    void otherModelJars_areNotRecognized() throws Exception {
        assertThat(SmithyModelDiscovery.isProtocolTestModels(new URL(
            "jar:file:/repo/smithy-protocol-test-traits-1.71.0.jar!/META-INF/smithy/manifest"))).isFalse();
        assertThat(SmithyModelDiscovery.isProtocolTestModels(new URL(
            "jar:file:/repo/smithy-aws-traits-1.71.0.jar!/META-INF/smithy/manifest"))).isFalse();
        assertThat(SmithyModelDiscovery.isProtocolTestModels(new URL(
            "file:/build/smithy-protocol-tests/target/classes/META-INF/smithy/manifest"))).isFalse();
    }

    @Test
    void discoverModels_keepsTraitDefinitionsFromTheClassPath() {
        ClassLoader classLoader = getClass().getClassLoader();
        Model model = SmithyModelDiscovery.discoverModels(Model.assembler(classLoader), classLoader)
                                          .assemble()
                                          .unwrap();

        assertThat(model.getShape(ShapeId.from("aws.api#service"))).isPresent();
        assertThat(model.getShape(ShapeId.from("com.amazonaws.c2j#locationName"))).isPresent();
    }
}
