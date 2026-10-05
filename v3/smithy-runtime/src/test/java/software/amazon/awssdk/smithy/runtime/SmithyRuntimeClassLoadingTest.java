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

package software.amazon.awssdk.smithy.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.smithy.runtime.fixture.WidgetClientScenario;
import software.amazon.awssdk.smithy.runtime.internal.SmithyRuntimeBoundary;

/**
 * Proves that a generated-style service client running on the {@code smithy-runtime} runtime classpath never resolves
 * a Smithy model parser, assembler, or code-generation class (Requirements 14.18, 15.4, 15.5), and that it behaves
 * identically when those classes are physically removed from the classpath.
 */
class SmithyRuntimeClassLoadingTest {

    private static final String SCENARIO = WidgetClientScenario.class.getName();
    private static final String SMITHY_MODEL_PACKAGE = "software.amazon.smithy.model.";

    private static String expectedTranscript;

    @BeforeAll
    static void runScenarioOnTestClasspath() {
        expectedTranscript = new WidgetClientScenario().get();
    }

    @Test
    void generatedClient_onRuntimeClasspath_resolvesNoParserAssemblerOrCodegenClass() {
        try (IsolatedRuntimeClassLoader loader = new IsolatedRuntimeClassLoader(isolatedClasspath(RuntimeClasspath.jars()))) {
            String transcript = loader.run(SCENARIO);
            SortedSet<String> requested = loader.requestedClasses();

            assertThat(transcript).isEqualTo(expectedTranscript);
            // The recording is meaningful only if the boundary types were resolved through the isolated loader.
            assertThat(requested).contains(
                SCENARIO,
                "software.amazon.smithy.java.core.schema.Schema",
                "software.amazon.smithy.java.core.schema.SerializableStruct",
                "software.amazon.smithy.java.core.schema.ShapeBuilder",
                "software.amazon.smithy.java.core.serde.document.Document",
                "software.amazon.smithy.java.core.serde.TypeRegistry",
                "software.amazon.smithy.java.json.JsonCodec",
                "software.amazon.smithy.java.cbor.Rpcv2CborCodec",
                "software.amazon.smithy.java.xml.XmlCodec",
                "software.amazon.smithy.model.shapes.ShapeId",
                "software.amazon.smithy.model.traits.RequiredTrait");
            assertThat(requested)
                .as("classes outside the Smithy runtime boundary resolved by a generated client")
                .filteredOn(SmithyRuntimeBoundary::isForbiddenRuntimeClass)
                .isEmpty();

            long smithyModelClasses = requested.stream().filter(name -> name.startsWith(SMITHY_MODEL_PACKAGE)).count();
            System.out.println("[smithy-runtime] generated-client scenario resolved " + requested.size()
                               + " non-JDK classes, " + smithyModelClasses + " from smithy-model, 0 forbidden");
        }
    }

    @Test
    void generatedClient_withParserAndAssemblerRemovedFromClasspath_behavesIdentically(@TempDir Path tempDir) {
        Path smithyModel = RuntimeClasspath.jar("smithy-model", RuntimeClasspath.smithyVersion());
        Path parserFree = tempDir.resolve("smithy-model-parser-free.jar");
        List<String> removed = ParserFreeJar.write(smithyModel, parserFree);
        assertThat(removed).contains(
            "software.amazon.smithy.model.Model",
            "software.amazon.smithy.model.loader.ModelAssembler",
            "software.amazon.smithy.model.validation.Validator");

        List<Path> jars = new ArrayList<>(RuntimeClasspath.jars());
        jars.replaceAll(jar -> jar.equals(smithyModel) ? parserFree : jar);
        assertThat(jars).contains(parserFree).doesNotContain(smithyModel);

        try (IsolatedRuntimeClassLoader loader = new IsolatedRuntimeClassLoader(isolatedClasspath(jars))) {
            String transcript = loader.run(SCENARIO);

            assertThat(transcript).isEqualTo(expectedTranscript);
            assertThat(loader.requestedClasses()).filteredOn(SmithyRuntimeBoundary::isForbiddenRuntimeClass).isEmpty();
            assertThatThrownBy(() -> loader.loadClass("software.amazon.smithy.model.loader.ModelAssembler"))
                .isInstanceOf(ClassNotFoundException.class);
        }
    }

    private static List<Path> isolatedClasspath(List<Path> runtimeJars) {
        List<Path> classpath = new ArrayList<>();
        classpath.add(RuntimeClasspath.mainClasses());
        // Hosts the generated-style fixture; the isolated loader cannot see the test framework on the test classpath.
        classpath.add(RuntimeClasspath.testClasses());
        classpath.addAll(runtimeJars);
        return classpath;
    }
}
