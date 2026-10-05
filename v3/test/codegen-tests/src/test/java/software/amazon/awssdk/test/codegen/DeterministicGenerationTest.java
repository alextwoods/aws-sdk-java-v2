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

package software.amazon.awssdk.test.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.test.codegen.support.GeneratedTree.assertByteIdentical;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenContext;
import software.amazon.awssdk.awscodegen.AwsCodegenException;
import software.amazon.awssdk.awscodegen.AwsCodegenIntegration;
import software.amazon.awssdk.awscodegen.validation.Diagnostic;
import software.amazon.awssdk.awscodegen.validation.DiagnosticCode;
import software.amazon.awssdk.test.codegen.support.GeneratedTree;
import software.amazon.awssdk.test.codegen.support.Generation;
import software.amazon.awssdk.test.codegen.support.SmithyModelFixture;
import software.amazon.awssdk.test.codegen.support.TestIntegration;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * Requirement 3.6: identical model inputs, configuration, JDK and generator produce byte-identical generated source
 * and resource trees, independent of the output root, the checkout location, the order in which model files were
 * created, and the order in which integrations are enumerated.
 */
class DeterministicGenerationTest {

    private static final SmithyModelFixture FIXTURE = SmithyModelFixture.weather();

    @TempDir
    Path temp;

    @Test
    void generationIsByteIdenticalAcrossOutputRootsAndModelFileCreationOrder() {
        Generation a = Generation.run(temp.resolve("a"), FIXTURE, FIXTURE.files(), List.of());
        Generation b = Generation.run(temp.resolve("somewhere/much/deeper/b"), FIXTURE, FIXTURE.reversedFiles(),
                                      List.of());

        assertFalse(a.sources().isEmpty());
        assertFalse(a.resources().isEmpty());
        assertByteIdentical(a.sources(), b.sources());
        assertByteIdentical(a.resources(), b.resources());
        assertEquals(a.result().generatedFiles(), b.result().generatedFiles());
        assertEquals(a.result().generatedResources(), b.result().generatedResources());
        assertEquals(a.result().modelDigest(), b.result().modelDigest());
    }

    @Test
    void regeneratingIntoTheSameRootIsByteIdentical() {
        Generation first = Generation.run(temp.resolve("a"), FIXTURE, FIXTURE.files(), integrations());
        GeneratedTree sources = first.sources();
        GeneratedTree resources = first.resources();

        Generation second = Generation.run(temp.resolve("a"), FIXTURE, FIXTURE.files(), integrations());

        assertByteIdentical(sources, second.sources());
        assertByteIdentical(resources, second.resources());
    }

    @Test
    void integrationEnumerationOrderDoesNotChangeTheTree() {
        List<AwsCodegenIntegration> integrations = integrations();
        GeneratedTree expectedSources = null;
        GeneratedTree expectedResources = null;
        int run = 0;
        for (List<AwsCodegenIntegration> permutation : permutations(integrations)) {
            Generation generation = Generation.run(temp.resolve("run" + run++), FIXTURE, FIXTURE.files(),
                                                   permutation);
            if (expectedSources == null) {
                expectedSources = generation.sources();
                expectedResources = generation.resources();
                assertTrue(expectedSources.paths().contains(SmithyModelFixture.PACKAGE_PATH + "/Late.java"),
                           expectedSources.paths().toString());
            } else {
                assertByteIdentical(expectedSources, generation.sources());
                assertByteIdentical(expectedResources, generation.resources());
            }
        }
        assertEquals(6, run);
    }

    /**
     * Generated sources order shapes by absolute shape ID: declaring operations, errors and shapes in a different
     * order changes only the content-derived model identity, not the ordering of any generated file.
     */
    @Test
    void declarationOrderOfOperationsErrorsAndShapesDoesNotChangeShapeSortedSources() {
        Generation declared = Generation.run(temp.resolve("a"), FIXTURE, FIXTURE.files(), List.of());
        Generation reversed = Generation.run(temp.resolve("b"), SmithyModelFixture.weatherDeclaredInReverse(),
                                             FIXTURE.files(), List.of());

        String identity = SmithyModelFixture.PACKAGE_PATH + "/" + AwsCodegen.SERVICE_INFO_TYPE + ".java";
        assertByteIdentical(declared.sources().filter(path -> !path.equals(identity)),
                            reversed.sources().filter(path -> !path.equals(identity)));
        String table = declared.sources().text(SmithyModelFixture.PACKAGE_PATH
                                               + "/internal/operation/WeatherOperations.java");
        assertTrue(table.indexOf("GET_FORECAST =") < table.indexOf("LIST_CITIES ="), table);
        String descriptor = declared.sources().text(SmithyModelFixture.PACKAGE_PATH
                                                    + "/internal/operation/GetForecastOperation.java");
        List<String> errors = List.of("example.weather#AccessDenied", "example.weather#CityNotFound",
                                      "example.weather#ServiceFault", "example.weather#Throttled");
        List<Integer> positions = errors.stream().map(descriptor::indexOf).toList();
        assertFalse(positions.contains(-1), descriptor);
        assertEquals(positions.stream().sorted().toList(), positions, descriptor);
    }

    @Test
    void generatedFilesContainNoAbsolutePathsOrCarriageReturns() {
        Generation generation = Generation.run(temp.resolve("a"), FIXTURE, FIXTURE.files(), integrations());
        List<String> problems = new ArrayList<>();
        for (GeneratedTree tree : List.of(generation.sources(), generation.resources())) {
            for (String path : tree.paths()) {
                String text = tree.text(path);
                if (text.contains(temp.toString()) || text.contains(temp.toAbsolutePath().toString())) {
                    problems.add(path + " contains an absolute path");
                }
                if (text.indexOf('\r') >= 0) {
                    problems.add(path + " contains a carriage return");
                }
            }
        }
        assertEquals(List.of(), problems);
    }

    @Test
    void absoluteOutputPathInGeneratedFileFailsGenerationWithTheContributor() throws Exception {
        Path work = temp.resolve("a");
        Path existing = Files.createDirectories(Generation.sourceRoot(work));
        Files.writeString(existing.resolve("Existing.java"), "class Existing {}");
        TestIntegration leaky = TestIntegration.named("aws.leaky").withCustomize(context -> context.fileManifest()
            .writeFile("META-INF/leak.txt", "generated into " + context.fileManifest().getBaseDir() + "\n"));

        AwsCodegenException e = assertThrows(AwsCodegenException.class,
                                             () -> Generation.run(work, FIXTURE, FIXTURE.files(), List.of(leaky)));

        Diagnostic diagnostic = e.diagnostics().get(0);
        assertEquals(DiagnosticCode.ENVIRONMENT_DEPENDENT_OUTPUT, diagnostic.code());
        assertEquals(List.of("aws.leaky"), diagnostic.integrations());
        assertEquals(Optional.of(SmithyModelFixture.SERVICE), diagnostic.shape());
        assertEquals("output META-INF/leak.txt", diagnostic.element());
        assertEquals(Set.of("Existing.java"), GeneratedTree.read(existing).paths(), "nothing was published");
    }

    /**
     * Integrations whose output depends on the model and on each other: an order -10 integration documents an
     * operation, an order 0 integration writes a resource listing the documented operations, and an order 10
     * integration writes a Java file.
     */
    private static List<AwsCodegenIntegration> integrations() {
        ShapeId operation = ShapeId.from("example.weather#ListCities");
        TestIntegration documents = TestIntegration.named("aws.documents").withOrder(-10).withPreprocess(
            model -> documented(model, operation, "Lists cities."));
        TestIntegration lists = TestIntegration.named("aws.lists").withCustomize(context -> context.fileManifest()
            .writeFile("META-INF/aws-sdk-java-v3/documented-operations.txt", documentedOperations(context)));
        TestIntegration late = TestIntegration.named("aws.late").withOrder(10).withCustomize(
            context -> context.writerDelegator().useFileWriter(SmithyModelFixture.PACKAGE_PATH + "/Late.java",
                                                               SmithyModelFixture.PACKAGE,
                                                               writer -> writer.write("final class Late {\n}")));
        return List.of(documents, lists, late);
    }

    private static Model documented(Model model, ShapeId shape, String text) {
        return ModelTransformer.create().replaceShapes(
            model, List.of(model.expectShape(shape).asOperationShape().orElseThrow().toBuilder()
                                .addTrait(new DocumentationTrait(text)).build()));
    }

    private static String documentedOperations(AwsCodegenContext context) {
        StringBuilder result = new StringBuilder();
        context.model().getOperationShapes().stream()
               .filter(o -> o.hasTrait(DocumentationTrait.class))
               .map(o -> o.getId().toString())
               .sorted()
               .forEach(id -> result.append(id).append('\n'));
        return result.toString();
    }

    private static <T> List<List<T>> permutations(List<T> items) {
        if (items.isEmpty()) {
            return List.of(List.of());
        }
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            List<T> rest = new ArrayList<>(items);
            T head = rest.remove(i);
            for (List<T> tail : permutations(rest)) {
                List<T> permutation = new ArrayList<>();
                permutation.add(head);
                permutation.addAll(tail);
                result.add(permutation);
            }
        }
        return result;
    }
}
