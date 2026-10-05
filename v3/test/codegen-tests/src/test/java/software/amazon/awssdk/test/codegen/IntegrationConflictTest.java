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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
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
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * Requirement 3.7: when two code-generation integrations produce conflicting output or settings, generation fails
 * before anything is published, with a diagnostic naming both integration identifiers and the conflicting Smithy
 * shape. Covers every conflict kind aws-codegen detects: output files (file manifest and writer delegator, between
 * integrations and between an integration and the core generator), symbol-provider decorations, and model
 * preprocessing.
 */
class IntegrationConflictTest {

    private static final SmithyModelFixture FIXTURE = SmithyModelFixture.weather();
    private static final String SERVICE = SmithyModelFixture.SERVICE;
    private static final ShapeId FORECAST = ShapeId.from("example.weather#Forecast");
    private static final ShapeId GET_FORECAST = ShapeId.from("example.weather#GetForecast");
    private static final ShapeId LIST_CITIES = ShapeId.from("example.weather#ListCities");
    private static final ShapeId CITY_LIST = ShapeId.from("example.weather#CityList");
    private static final ShapeId CITY_NOT_FOUND = ShapeId.from("example.weather#CityNotFound");
    private static final String GENERATOR = AwsCodegen.GENERATOR_IDENTIFIER;

    @TempDir
    Path temp;

    @Test
    void twoIntegrationsWritingOneResourceThroughTheFileManifestConflict() throws IOException {
        Consumer<AwsCodegenContext> write = context -> context.fileManifest()
                                                             .writeFile("META-INF/shared.properties", "a=b\n");
        Diagnostic conflict = singleConflict(TestIntegration.named("aws.alpha").withCustomize(write),
                                             TestIntegration.named("aws.beta").withCustomize(write));

        assertConflict(conflict, DiagnosticCode.INTEGRATION_OUTPUT_CONFLICT, SERVICE, "aws.alpha", "aws.beta");
        assertEquals("output META-INF/shared.properties", conflict.element());
    }

    @Test
    void twoIntegrationsWritingOneJavaFileThroughTheWriterDelegatorConflict() throws IOException {
        // Without attribution the delegator would silently append both classes to one file. The file is the
        // definition file a third integration gives the CityList list, which no directive renders, so the conflict is
        // reported for the shape defined in the file.
        String file = definitionFile(CITY_LIST);
        TestIntegration definesCityList = TestIntegration.named("aws.defines").withSymbolDecorator(
            TestIntegration.mapSymbol(CITY_LIST, (shape, symbol) -> Symbol.builder()
                .namespace(SmithyModelFixture.PACKAGE + ".model", ".").name("CityList").definitionFile(file).build()));
        Consumer<AwsCodegenContext> write = context -> context.writerDelegator().useFileWriter(
            file, SmithyModelFixture.PACKAGE + ".model", writer -> writer.write("final class CityList {\n}"));
        Diagnostic conflict = singleConflict(definesCityList,
                                             TestIntegration.named("aws.alpha").withCustomize(write),
                                             TestIntegration.named("aws.beta").withOrder(5).withCustomize(write));

        assertConflict(conflict, DiagnosticCode.INTEGRATION_OUTPUT_CONFLICT, CITY_LIST.toString(), "aws.alpha",
                       "aws.beta");
        assertEquals("output " + file, conflict.element());
    }

    @Test
    void integrationWritingAGeneratedModelTypeConflictsWithTheGenerator() throws IOException {
        // The structure directive renders model/Forecast.java, so the core generator owns it.
        String file = definitionFile(FORECAST);
        Diagnostic conflict = singleConflict(TestIntegration.named("aws.alpha").withCustomize(
            context -> context.writerDelegator().useFileWriter(file, SmithyModelFixture.PACKAGE + ".model",
                                                               writer -> writer.write("final class Forecast {\n}"))));

        assertConflict(conflict, DiagnosticCode.INTEGRATION_OUTPUT_CONFLICT, FORECAST.toString(), GENERATOR,
                       "aws.alpha");
        assertEquals("output " + file, conflict.element());
    }

    @Test
    void integrationWritingAGeneratedErrorTypeConflictsWithTheGenerator() throws IOException {
        // Modeled errors are V2 <Name>Exception types, rendered by the error directive.
        String file = SmithyModelFixture.PACKAGE_PATH + "/model/CityNotFoundException.java";
        Diagnostic conflict = singleConflict(TestIntegration.named("aws.alpha").withCustomize(
            context -> context.fileManifest().writeFile(file, "// replaced\n")));

        assertConflict(conflict, DiagnosticCode.INTEGRATION_OUTPUT_CONFLICT, CITY_NOT_FOUND.toString(), GENERATOR,
                       "aws.alpha");
    }

    @Test
    void integrationWritingAFileTheGeneratorOwnsConflictsWithTheGenerator() throws IOException {
        TestIntegration shadow = TestIntegration.named("aws.shadow").withCustomize(context -> context.writerDelegator()
            .useShapeWriter(context.model().expectShape(LIST_CITIES), writer -> writer.write("// extra")));

        Diagnostic conflict = singleConflict(shadow);

        assertConflict(conflict, DiagnosticCode.INTEGRATION_OUTPUT_CONFLICT, LIST_CITIES.toString(), GENERATOR,
                       "aws.shadow");
    }

    @Test
    void everyOutputConflictIsReportedTogether() throws IOException {
        Consumer<AwsCodegenContext> writeBoth = context -> {
            context.fileManifest().writeFile("META-INF/one.txt", "1\n");
            context.fileManifest().writeFile("META-INF/two.txt", "2\n");
        };
        List<Diagnostic> conflicts = conflicts(TestIntegration.named("aws.alpha").withCustomize(writeBoth),
                                               TestIntegration.named("aws.beta").withCustomize(writeBoth),
                                               TestIntegration.named("aws.gamma").withCustomize(
                                                   c -> c.fileManifest().writeFile("META-INF/two.txt", "3\n")));

        assertEquals(List.of("output META-INF/one.txt", "output META-INF/two.txt"),
                     conflicts.stream().map(Diagnostic::element).toList());
        assertEquals(List.of("aws.alpha", "aws.beta"), conflicts.get(0).integrations());
        assertEquals(List.of("aws.alpha", "aws.beta", "aws.gamma"), conflicts.get(1).integrations());
    }

    @Test
    void twoSymbolDecoratorsRenamingOneShapeConflict() throws IOException {
        TestIntegration first = TestIntegration.named("aws.first").withSymbolDecorator(
            TestIntegration.mapSymbol(FORECAST, (shape, symbol) -> renamed(symbol, "WeatherForecast")));
        TestIntegration second = TestIntegration.named("aws.second").withOrder(1).withSymbolDecorator(
            TestIntegration.mapSymbol(FORECAST, (shape, symbol) -> renamed(symbol, "DailyForecast")));

        Diagnostic conflict = singleConflict(first, second);

        assertConflict(conflict, DiagnosticCode.INTEGRATION_SYMBOL_CONFLICT, FORECAST.toString(), "aws.first",
                       "aws.second");
        assertTrue(conflict.message().contains("WeatherForecast") && conflict.message().contains("DailyForecast"),
                   conflict.message());
    }

    @Test
    void compatibleSymbolDecorationsDoNotConflict() throws IOException {
        TestIntegration renamesForecast = TestIntegration.named("aws.first").withSymbolDecorator(
            TestIntegration.mapSymbol(FORECAST, (shape, symbol) -> renamed(symbol, "WeatherForecast")));
        TestIntegration renamesOther = TestIntegration.named("aws.second").withSymbolDecorator(
            TestIntegration.mapSymbol(ShapeId.from("example.weather#CityList"), (shape, symbol) -> symbol));
        TestIntegration addsProperty = TestIntegration.named("aws.third").withSymbolDecorator(
            TestIntegration.mapSymbol(FORECAST, (shape, symbol) -> symbol.toBuilder().putProperty("x", "y").build()));

        Generation.run(temp.resolve("a"), FIXTURE, FIXTURE.files(), List.of(renamesForecast, renamesOther,
                                                                           addsProperty));
    }

    @Test
    void twoPreprocessorsSettingTheSameTraitConflict() throws IOException {
        TestIntegration first = TestIntegration.named("aws.first").withPreprocess(
            model -> withTrait(model, GET_FORECAST, new DocumentationTrait("First.")));
        TestIntegration second = TestIntegration.named("aws.second").withPreprocess(
            model -> withTrait(model, GET_FORECAST, new DocumentationTrait("Second.")));

        Diagnostic conflict = singleConflict(first, second);

        assertConflict(conflict, DiagnosticCode.INTEGRATION_MODEL_CONFLICT, GET_FORECAST.toString(), "aws.first",
                       "aws.second");
        assertEquals("trait smithy.api#documentation", conflict.element());
    }

    @Test
    void preprocessorChangingAShapeDefinitionConflictsWithEveryOtherChangeOfTheShape() throws IOException {
        TestIntegration documents = TestIntegration.named("aws.documents").withPreprocess(
            model -> withTrait(model, FORECAST, new DocumentationTrait("Changed.")));
        TestIntegration strips = TestIntegration.named("aws.strips").withOrder(3).withPreprocess(
            model -> ModelTransformer.create().removeTraitsIf(model, (shape, trait) -> shape.getId().equals(FORECAST)
                                                                                     && trait instanceof SensitiveTrait));
        TestIntegration retargets = TestIntegration.named("aws.retargets").withOrder(4).withPreprocess(
            model -> ModelTransformer.create().replaceShapes(model, List.of(
                model.expectShape(FORECAST).asStructureShape().orElseThrow().toBuilder()
                     .addMember("low", ShapeId.from("smithy.api#Integer")).build())));

        List<Diagnostic> conflicts = conflicts(documents, strips, retargets);

        // documentation and sensitive are different traits, but aws.retargets changes Forecast's definition.
        assertEquals(List.of("trait smithy.api#documentation", "trait smithy.api#sensitive"),
                     conflicts.stream().map(Diagnostic::element).toList(), conflicts.toString());
        assertConflict(conflicts.get(0), DiagnosticCode.INTEGRATION_MODEL_CONFLICT, FORECAST.toString(),
                       "aws.documents", "aws.retargets");
        assertConflict(conflicts.get(1), DiagnosticCode.INTEGRATION_MODEL_CONFLICT, FORECAST.toString(),
                       "aws.retargets", "aws.strips");
    }

    @Test
    void preprocessorsChangingDifferentTraitsOrRepeatingAValueCompose() throws IOException {
        TestIntegration documents = TestIntegration.named("aws.documents").withPreprocess(
            model -> withTrait(model, LIST_CITIES, new DocumentationTrait("Lists.")));
        TestIntegration repeats = TestIntegration.named("aws.repeats").withOrder(1).withPreprocess(
            model -> withTrait(model, LIST_CITIES, new DocumentationTrait("Lists.")));
        TestIntegration otherShape = TestIntegration.named("aws.other").withPreprocess(
            model -> withTrait(model, GET_FORECAST, new DocumentationTrait("Other.")));

        Generation.run(temp.resolve("a"), FIXTURE, FIXTURE.files(), List.of(documents, repeats, otherShape));
    }

    @Test
    void diagnosticsDoNotDependOnIntegrationEnumerationOrder() throws IOException {
        Consumer<AwsCodegenContext> write = context -> context.fileManifest().writeFile("META-INF/x.txt", "x\n");
        List<AwsCodegenIntegration> integrations = List.of(
            TestIntegration.named("aws.c").withCustomize(write),
            TestIntegration.named("aws.a").withOrder(2).withCustomize(write),
            TestIntegration.named("aws.b").withOrder(-1).withCustomize(write));

        List<Diagnostic> forward = conflicts(integrations.toArray(AwsCodegenIntegration[]::new));
        List<Diagnostic> backward = conflicts(integrations.reversed().toArray(AwsCodegenIntegration[]::new));

        assertEquals(forward, backward);
        assertEquals(List.of("aws.a", "aws.b", "aws.c"), forward.get(0).integrations());
    }

    @Test
    void theCoreGeneratorIdentifierIsReserved() {
        AwsCodegenException e = assertThrows(AwsCodegenException.class, () -> Generation.run(
            temp.resolve("a"), FIXTURE, FIXTURE.files(), List.of(TestIntegration.named(GENERATOR))));
        assertTrue(e.getMessage().contains("reserved"), e.getMessage());
    }

    /** Runs generation over an existing published tree, which must survive the conflict untouched. */
    private List<Diagnostic> conflicts(AwsCodegenIntegration... integrations) throws IOException {
        Path work = Files.createDirectories(temp.resolve("work" + System.nanoTime()));
        Path sources = Files.createDirectories(Generation.sourceRoot(work));
        Path resources = Files.createDirectories(Generation.resourceRoot(work));
        Files.writeString(sources.resolve("Existing.java"), "class Existing {}");
        Files.writeString(resources.resolve("existing.txt"), "existing");

        AwsCodegenException e = assertThrows(AwsCodegenException.class, () -> Generation.run(
            work, FIXTURE, FIXTURE.files(), List.of(integrations)));

        assertTrue(!e.diagnostics().isEmpty(), e.getMessage());
        assertEquals(List.of("Existing.java"), List.copyOf(GeneratedTree.read(sources).paths()));
        assertEquals(List.of("existing.txt"), List.copyOf(GeneratedTree.read(resources).paths()));
        try (Stream<Path> siblings = Files.list(work.resolve("out"))) {
            assertEquals(List.of("resources", "src"),
                         siblings.map(p -> p.getFileName().toString()).sorted().toList(),
                         "no staging or previous-tree directories remain");
        }
        return e.diagnostics();
    }

    private Diagnostic singleConflict(AwsCodegenIntegration... integrations) throws IOException {
        List<Diagnostic> conflicts = conflicts(integrations);
        assertEquals(1, conflicts.size(), conflicts.toString());
        return conflicts.get(0);
    }

    private static void assertConflict(Diagnostic diagnostic, DiagnosticCode code, String shape,
                                       String... integrations) {
        assertEquals(code, diagnostic.code(), diagnostic.render());
        assertEquals(SERVICE, diagnostic.service());
        assertEquals(Optional.of(shape), diagnostic.shape(), diagnostic.render());
        assertEquals(List.of(integrations), diagnostic.integrations(), diagnostic.render());
        for (String integration : integrations) {
            assertTrue(diagnostic.render().contains(integration), diagnostic.render());
        }
        assertTrue(diagnostic.render().contains(shape), diagnostic.render());
    }

    /** Definition file of {@code shape}'s generated type, as the AWS symbol provider assigns it. */
    private static String definitionFile(ShapeId shape) {
        return SmithyModelFixture.PACKAGE_PATH + "/model/" + shape.getName() + ".java";
    }

    private static Symbol renamed(Symbol symbol, String name) {
        return symbol.toBuilder().name(name).definitionFile(symbol.getDefinitionFile().replace(
            symbol.getName() + ".java", name + ".java")).build();
    }

    private static Model withTrait(Model model, ShapeId id, Trait trait) {
        Shape shape = model.expectShape(id);
        return ModelTransformer.create().replaceShapes(model, List.of(
            Shape.shapeToBuilder(shape).addTrait(trait).build()));
    }
}
