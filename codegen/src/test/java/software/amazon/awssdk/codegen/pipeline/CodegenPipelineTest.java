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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.codegen.c2j2smithy.C2jConversionException;
import software.amazon.awssdk.codegen.c2j2smithy.ConversionGapReport;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.config.customization.ShapeModifier;
import software.amazon.awssdk.codegen.model.config.customization.SmithyValidationSuppression;
import software.amazon.awssdk.codegen.model.config.customization.UnderscoresInNameBehavior;
import software.amazon.awssdk.codegen.pipeline.CodegenPipeline.Stage;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

class CodegenPipelineTest {

    private static final String SERVICE = "demo";
    private static final ShapeId REQUEST = ShapeId.from("com.amazonaws.demo#GetThingRequest");

    private static final String C2J_MODEL =
        "{\n"
        + "  \"version\": \"2.0\",\n"
        + "  \"metadata\": {\n"
        + "    \"apiVersion\": \"2024-01-01\", \"endpointPrefix\": \"demo\", \"jsonVersion\": \"1.1\",\n"
        + "    \"protocol\": \"json\", \"protocols\": [\"json\"], \"serviceFullName\": \"Demo Service\",\n"
        + "    \"serviceId\": \"Demo\", \"signatureVersion\": \"v4\", \"targetPrefix\": \"DemoService\",\n"
        + "    \"uid\": \"demo-2024-01-01\"\n"
        + "  },\n"
        + "  \"operations\": {\n"
        + "    \"GetThing\": {\n"
        + "      \"name\": \"GetThing\", \"http\": { \"method\": \"POST\", \"requestUri\": \"/\" },\n"
        + "      \"input\": { \"shape\": \"GetThingRequest\" }, \"output\": { \"shape\": \"GetThingResponse\" },\n"
        + "      \"errors\": [ { \"shape\": \"NotFound\" } ]\n"
        + "    }\n"
        + "  },\n"
        + "  \"shapes\": {\n"
        + "    \"GetThingRequest\": { \"type\": \"structure\", \"members\": {\n"
        + "      \"Name\": { \"shape\": \"String\" }, \"Extra\": { \"shape\": \"String\" } } },\n"
        + "    \"GetThingResponse\": { \"type\": \"structure\", \"members\": { \"Value\": { \"shape\": \"String\" } } },\n"
        + "    \"NotFound\": { \"type\": \"structure\", \"members\": { \"message\": { \"shape\": \"String\" } },\n"
        + "      \"exception\": true },\n"
        + "    \"String\": { \"type\": \"%s\" }\n"
        + "  }\n"
        + "}\n";

    private static final String SMITHY_MODEL =
        "$version: \"2.0\"\nnamespace demo\n\n"
        + "use aws.api#service\n"
        + "use aws.auth#sigv4\n"
        + "use aws.protocols#restJson1\n"
        + "@service(sdkId: \"Demo\", arnNamespace: \"demo\")\n"
        + "@sigv4(name: \"demo\")\n"
        + "@restJson1\n"
        + "service DemoService { version: \"2024-01-01\", operations: [Op] }\n\n"
        + "@http(method: \"POST\", uri: \"/op\")\n"
        + "operation Op { input: OpRequest, output: OpResponse }\n"
        + "structure OpRequest { name: %s }\n"
        + "structure OpResponse { name: String }\n";

    @TempDir
    Path tmp;

    private final RecordingListener listener = new RecordingListener();
    private final CodegenPipeline pipeline = CodegenPipeline.builder().listener(listener).build();

    @Test
    void c2jModel_runsTheSixStagesInOrder_andMovesTheOutputIntoPlace() throws IOException {
        Path existing = write(tmp.resolve("out/src/keep/Existing.java"), "// not generated");

        PreparedService prepared = pipeline.run(c2jInputs(c2jModel("string"), CustomizationConfig.create()));

        assertThat(listener.stages).containsExactly(Stage.C2J_LOAD, Stage.C2J_CUSTOMIZATIONS, Stage.C2J_CONVERSION,
                                                    Stage.ASSEMBLY_AND_VALIDATION, Stage.INTERMEDIATE_MODEL,
                                                    Stage.GENERATION);
        assertThat(files(tmp.resolve("out/src"))).anyMatch(f -> f.endsWith("DemoClient.java"));
        assertThat(existing).exists();
        assertThat(tmp.resolve("target").resolve(CodegenPipeline.STAGING_DIRECTORY)).doesNotExist();

        Path gapReport = tmp.resolve("target/c2j-conversion-gaps/demo.json");
        assertThat(gapReport).exists();
        assertThat(ConversionGapReport.fromJson(new String(Files.readAllBytes(gapReport), StandardCharsets.UTF_8)))
            .isEqualTo(prepared.gapReport());
        assertThat(listener.warnings).isEmpty();
    }

    @Test
    void c2jModel_customizationsAreAppliedBeforeConversion() throws IOException {
        CustomizationConfig config = CustomizationConfig.create();
        config.setShapeModifiers(Collections.singletonMap("GetThingRequest", excludeExtra()));

        PreparedService prepared = pipeline.prepare(c2jInputs(c2jModel("string"), config));

        assertThat(memberNames(prepared.smithyModel())).containsExactly("Name");
    }

    @Test
    void disabledCustomization_isSkippedWithAWarning_andTheOthersStillRun() throws IOException {
        CustomizationConfig config = CustomizationConfig.create();
        config.setShapeModifiers(Collections.singletonMap("GetThingRequest", excludeExtra()));
        config.setRenameShapes(Collections.singletonMap("GetThingResponse", "GetThingResult"));
        config.setDisabledCustomizations(Collections.singletonList("shapeModifiers"));

        PreparedService prepared = pipeline.prepare(c2jInputs(c2jModel("string"), config));

        assertThat(listener.warnings)
            .containsExactly("customization shapeModifiers skipped for demo: disabled by disabledCustomizations");
        assertThat(memberNames(prepared.smithyModel())).containsExactly("Name", "Extra");
        assertThat(prepared.smithyModel().getShape(ShapeId.from("com.amazonaws.demo#GetThingResult"))).isPresent();
    }

    @Test
    void failingCustomization_isUndoneAndSkippedWithAWarning() throws IOException {
        // The first modifier applies, the second fails: the whole shapeModifiers customization is undone.
        Map<String, ShapeModifier> modifiers = new LinkedHashMap<>();
        modifiers.put("GetThingRequest", excludeExtra());
        modifiers.put("Missing", excludeExtra());
        CustomizationConfig config = CustomizationConfig.create();
        config.setShapeModifiers(modifiers);

        PreparedService prepared = pipeline.prepare(c2jInputs(c2jModel("string"), config));

        assertThat(listener.warnings).hasSize(1);
        assertThat(listener.warnings.get(0)).startsWith("customization shapeModifiers skipped for demo: ")
                                            .contains("Missing");
        assertThat(memberNames(prepared.smithyModel())).containsExactly("Name", "Extra");
    }

    @Test
    void unknownDisabledCustomization_isReported() throws IOException {
        CustomizationConfig config = CustomizationConfig.create();
        config.setDisabledCustomizations(Collections.singletonList("noSuchCustomization"));

        pipeline.prepare(c2jInputs(c2jModel("string"), config));

        assertThat(listener.warnings).hasSize(1);
        assertThat(listener.warnings.get(0)).contains("noSuchCustomization").contains("shapeModifiers");
    }

    @Test
    void conversionFailure_stopsInStage3_andWritesNothing() throws IOException {
        ServiceInputs inputs = c2jInputs(c2jModel("widget"), CustomizationConfig.create());

        assertThatThrownBy(() -> pipeline.run(inputs))
            .isInstanceOfSatisfying(CodegenPipelineException.class, e -> {
                assertThat(e.stage()).isEqualTo(Stage.C2J_CONVERSION);
                assertThat(e.service()).isEqualTo(SERVICE);
            })
            .hasCauseInstanceOf(C2jConversionException.class)
            .hasMessageContaining("String")
            .hasMessageContaining("widget");
        assertThat(listener.stages).doesNotContain(Stage.ASSEMBLY_AND_VALIDATION);
        assertNothingWritten();
    }

    @Test
    void smithyFilesWithValidationErrors_stopInStage4_reportShapeIdAndMessage_andWriteNothing() throws IOException {
        Path model = write(tmp.resolve("model/main.smithy"), String.format(SMITHY_MODEL, "NoSuchShape"));
        ServiceInputs inputs = outputs(ServiceInputs.builder().serviceName(SERVICE)
                                                    .smithyModelSources(Collections.singletonList(model.getParent())))
            .build();

        assertThatThrownBy(() -> pipeline.run(inputs))
            .isInstanceOfSatisfying(CodegenPipelineException.class,
                                    e -> assertThat(e.stage()).isEqualTo(Stage.ASSEMBLY_AND_VALIDATION))
            .hasMessageContaining("ERROR")
            .hasMessageContaining("[demo#OpRequest$name]")
            .hasMessageContaining("NoSuchShape");
        assertThat(listener.stages).containsExactly(Stage.ASSEMBLY_AND_VALIDATION);
        assertNothingWritten();
    }

    @Test
    void assembledModelWithADangerEvent_stopsInStage4() {
        Model model = Model.assembler().discoverModels(getClass().getClassLoader())
                           .addUnparsedModel("main.smithy", String.format(SMITHY_MODEL, "String"))
                           .assemble().unwrap();
        ValidationEvent danger = ValidationEvent.builder()
                                                .id("Example")
                                                .severity(Severity.DANGER)
                                                .shapeId(ShapeId.from("demo#OpRequest"))
                                                .message("something dangerous")
                                                .build();
        ServiceInputs inputs = outputs(ServiceInputs.builder().serviceName(SERVICE)
                                                    .smithyModel(model, Collections.singletonList(danger)))
            .build();

        assertThatThrownBy(() -> pipeline.run(inputs))
            .isInstanceOfSatisfying(CodegenPipelineException.class,
                                    e -> assertThat(e.stage()).isEqualTo(Stage.ASSEMBLY_AND_VALIDATION))
            .hasMessageContaining("DANGER Example [demo#OpRequest]: something dangerous");
        assertNothingWritten();
    }

    @Test
    void nativeSmithyFiles_skipTheC2jStages() throws IOException {
        Path model = write(tmp.resolve("model/main.smithy"), String.format(SMITHY_MODEL, "String"));
        ServiceInputs inputs = outputs(ServiceInputs.builder().serviceName(SERVICE)
                                                    .smithyModelSources(Collections.singletonList(model)))
            .build();

        PreparedService prepared = pipeline.run(inputs);

        assertThat(listener.stages).containsExactly(Stage.ASSEMBLY_AND_VALIDATION, Stage.INTERMEDIATE_MODEL,
                                                    Stage.GENERATION);
        assertThat(prepared.gapReport()).isNull();
        assertThat(files(tmp.resolve("out/src"))).anyMatch(f -> f.endsWith("DemoClient.java"));
        assertThat(tmp.resolve("target/c2j-conversion-gaps")).doesNotExist();
    }

    @Test
    void generationFailure_writesNoSources_butKeepsTheRequestedValidationReport() throws IOException {
        CustomizationConfig config = CustomizationConfig.create();
        config.setUnderscoresInNameBehavior(UnderscoresInNameBehavior.ALLOW);
        config.setAllowedUnderscoreNames(Collections.singletonList("Name"));
        ServiceInputs inputs = outputs(ServiceInputs.builder().serviceName(SERVICE)
                                                    .c2jModelDirectory(c2jDirectory(c2jModel("string")))
                                                    .customizationConfig(config)
                                                    .emitValidationReport(true))
            .build();

        assertThatThrownBy(() -> pipeline.run(inputs))
            .isInstanceOfSatisfying(CodegenPipelineException.class,
                                    e -> assertThat(e.stage()).isEqualTo(Stage.GENERATION));
        assertThat(files(tmp.resolve("out"))).containsExactly("src/models/validation-report.json");
        assertThat(tmp.resolve("target").resolve(CodegenPipeline.STAGING_DIRECTORY)).doesNotExist();
    }

    @Test
    void servicesGeneratedTogether_areMovedIntoPlaceOnlyIfAllSucceed() throws IOException {
        PreparedService good = pipeline.prepare(c2jInputs(c2jModel("string"), CustomizationConfig.create()));
        CustomizationConfig invalid = CustomizationConfig.create();
        invalid.setUnderscoresInNameBehavior(UnderscoresInNameBehavior.ALLOW);
        invalid.setAllowedUnderscoreNames(Collections.singletonList("Name"));
        PreparedService bad = pipeline.prepare(outputs(ServiceInputs.builder()
                                                                    .serviceName("other")
                                                                    .c2jModelDirectory(good.inputs().c2jModelDirectory())
                                                                    .customizationConfig(invalid))
                                                   .build());

        assertThatThrownBy(() -> pipeline.generate(Arrays.asList(good, bad), s -> null))
            .isInstanceOfSatisfying(CodegenPipelineException.class, e -> {
                assertThat(e.service()).isEqualTo("other");
                assertThat(e.stage()).isEqualTo(Stage.GENERATION);
            });
        assertNothingWritten();
    }

    @Test
    void serviceInputs_requireExactlyOneModelSource() {
        assertThatThrownBy(() -> outputs(ServiceInputs.builder().serviceName(SERVICE)).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Exactly one");
        assertThatThrownBy(() -> outputs(ServiceInputs.builder().serviceName(SERVICE)
                                                      .c2jModelDirectory(tmp)
                                                      .smithyModelSources(Collections.singletonList(tmp))).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("2 are set");
    }

    // ----- smithyValidationSuppressions -----

    @Test
    void suppressedEvent_doesNotStopTheService_andIsReportedWithTheReason() throws IOException {
        CustomizationConfig config = suppressing(suppression("SdkServiceId", "A test service ID."));

        PreparedService prepared = pipeline.prepare(allowingSuppressions(c2jModel("string").replace(
            "\"serviceId\": \"Demo\"", "\"serviceId\": \"Demo Service\""), config));

        assertThat(prepared.intermediateModel()).isNotNull();
        assertThat(listener.warnings)
            .containsExactly("Suppressed 1 Smithy validation event(s) SdkServiceId for demo: A test service ID.");
    }

    @Test
    void suppressedCriticalError_letsTheOtherValidatorsRun() throws IOException {
        // The member-level sensitive flag is a TraitTarget ERROR, which normally stops Smithy before the validator
        // that reports the service ID (DANGER).
        CustomizationConfig config = suppressing(
            suppression("TraitTarget", "Member-level sensitive.", "com.amazonaws.demoservice#GetThingRequest$Extra"));
        ServiceInputs inputs = allowingSuppressions(
            c2jModel("string").replace("\"serviceId\": \"Demo\"", "\"serviceId\": \"Demo Service\"")
                              .replace("\"Extra\": { \"shape\": \"String\" }",
                                       "\"Extra\": { \"shape\": \"String\", \"sensitive\": true }"),
            config);

        assertThatThrownBy(() -> pipeline.prepare(inputs))
            .isInstanceOfSatisfying(CodegenPipelineException.class,
                                    e -> assertThat(e.stage()).isEqualTo(Stage.ASSEMBLY_AND_VALIDATION))
            .hasMessageContaining("1 validation event(s)")
            .hasMessageContaining("DANGER SdkServiceId")
            .hasMessageNotContaining("TraitTarget");
        assertThat(listener.warnings).containsExactly(
            "Suppressed 1 Smithy validation event(s) TraitTarget [com.amazonaws.demoservice#GetThingRequest$Extra] for "
            + "demo: Member-level sensitive.");
    }

    @Test
    void suppressionsInAModuleThatDoesNotAllowThem_failTheService() throws IOException {
        CustomizationConfig config = suppressing(suppression("SdkServiceId", "A test service ID."));

        assertThatThrownBy(() -> pipeline.prepare(c2jInputs(c2jModel("string"), config)))
            .isInstanceOfSatisfying(CodegenPipelineException.class,
                                    e -> assertThat(e.stage()).isEqualTo(Stage.ASSEMBLY_AND_VALIDATION))
            .hasMessageContaining("does not allow them")
            .hasMessageContaining("allowSmithyValidationSuppressions");
    }

    @Test
    void staleSuppression_failsTheService_withTheRemainingEvents() throws IOException {
        CustomizationConfig config = suppressing(
            suppression("SdkServiceId", "Only one shape.", "com.amazonaws.demoservice#NoSuchShape"),
            suppression("HttpMethodSemantics", "Nothing matches."));

        assertThatThrownBy(() -> pipeline.prepare(allowingSuppressions(c2jModel("string").replace(
            "\"serviceId\": \"Demo\"", "\"serviceId\": \"Demo Service\""), config)))
            .isInstanceOf(CodegenPipelineException.class)
            .hasMessageContaining("DANGER SdkServiceId")
            .hasMessageContaining("match no ERROR or DANGER event")
            .hasMessageContaining("SdkServiceId [com.amazonaws.demoservice#NoSuchShape]")
            .hasMessageContaining("HttpMethodSemantics");
    }

    @Test
    void invalidSuppressions_failTheService() throws IOException {
        CustomizationConfig config = suppressing(suppression("SdkServiceId", " "),
                                                 suppression("Model", "Hide a parse failure."),
                                                 suppression("Target.Something", "Hide a missing shape."),
                                                 suppression("TraitTarget", "Relative shape.", "GetThingRequest"));

        assertThatThrownBy(() -> pipeline.prepare(allowingSuppressions(c2jModel("string"), config)))
            .isInstanceOf(CodegenPipelineException.class)
            .hasMessageContaining("entry 0 (SdkServiceId): a reason is required")
            .hasMessageContaining("entry 1 (Model): Model events")
            .hasMessageContaining("entry 2 (Target.Something): Target events")
            .hasMessageContaining("entry 3 (TraitTarget): GetThingRequest is not an absolute shape ID");
    }

    @Test
    void suppressionsForANativeSmithyModel_failTheService() throws IOException {
        Path model = write(tmp.resolve("model/main.smithy"), String.format(SMITHY_MODEL, "String"));
        ServiceInputs inputs = outputs(ServiceInputs.builder().serviceName(SERVICE)
                                                    .smithyModelSources(Collections.singletonList(model))
                                                    .customizationConfig(suppressing(suppression("X", "Reason.")))
                                                    .allowSmithyValidationSuppressions(true))
            .build();

        assertThatThrownBy(() -> pipeline.prepare(inputs))
            .isInstanceOf(CodegenPipelineException.class)
            .hasMessageContaining("apply only to C2J models");
    }

    private static SmithyValidationSuppression suppression(String id, String reason, String... shapes) {
        SmithyValidationSuppression suppression = new SmithyValidationSuppression();
        suppression.setId(id);
        suppression.setReason(reason);
        suppression.setShapes(Arrays.asList(shapes));
        return suppression;
    }

    private static CustomizationConfig suppressing(SmithyValidationSuppression... suppressions) {
        CustomizationConfig config = CustomizationConfig.create();
        config.setSmithyValidationSuppressions(Arrays.asList(suppressions));
        return config;
    }

    private ServiceInputs allowingSuppressions(String c2jModel, CustomizationConfig config) throws IOException {
        return outputs(ServiceInputs.builder()
                                    .serviceName(SERVICE)
                                    .c2jModelDirectory(c2jDirectory(c2jModel))
                                    .customizationConfig(config)
                                    .allowSmithyValidationSuppressions(true))
            .build();
    }

    private static ShapeModifier excludeExtra() {
        ShapeModifier modifier = new ShapeModifier();
        modifier.setExclude(Collections.singletonList("Extra"));
        return modifier;
    }

    private static List<String> memberNames(Model model) {
        return new ArrayList<>(model.expectShape(REQUEST, StructureShape.class).getAllMembers().keySet());
    }

    /** The C2J fixture with the given type for its {@code String} shape. */
    private static String c2jModel(String stringShapeType) {
        return String.format(C2J_MODEL, stringShapeType);
    }

    private ServiceInputs c2jInputs(String c2jModel, CustomizationConfig config) throws IOException {
        return outputs(ServiceInputs.builder()
                                    .serviceName(SERVICE)
                                    .c2jModelDirectory(c2jDirectory(c2jModel))
                                    .customizationConfig(config))
            .build();
    }

    private Path c2jDirectory(String c2jModel) throws IOException {
        Path dir = tmp.resolve("codegen-resources");
        write(dir.resolve("service-2.json"), c2jModel);
        return dir;
    }

    private ServiceInputs.Builder outputs(ServiceInputs.Builder builder) {
        return builder.sourcesDirectory(tmp.resolve("out/src"))
                      .resourcesDirectory(tmp.resolve("out/resources"))
                      .testsDirectory(tmp.resolve("out/tests"))
                      .buildDirectory(tmp.resolve("target"));
    }

    private void assertNothingWritten() {
        assertThat(tmp.resolve("out")).doesNotExist();
        assertThat(tmp.resolve("target").resolve(CodegenPipeline.STAGING_DIRECTORY)).doesNotExist();
    }

    private static Path write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        return Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    }

    private static List<String> files(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return Collections.emptyList();
        }
        try (Stream<Path> walk = Files.walk(directory)) {
            return walk.filter(Files::isRegularFile)
                       .map(p -> directory.relativize(p).toString().replace('\\', '/'))
                       .sorted()
                       .collect(Collectors.toList());
        }
    }

    private static final class RecordingListener implements CodegenPipelineListener {
        private final List<Stage> stages = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        @Override
        public void stageStarted(String service, Stage stage) {
            stages.add(stage);
        }

        @Override
        public void warning(String service, String message) {
            warnings.add(message);
        }
    }
}
