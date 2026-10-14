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

package software.amazon.awssdk.codegen.poet.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.pipeline.CodegenPipeline;
import software.amazon.awssdk.codegen.pipeline.ServiceInputs;
import software.amazon.awssdk.codegen.poet.model.ModelPackageCompiler;
import software.amazon.awssdk.codegen.schema.RuntimeCompiler;
import software.amazon.awssdk.codegen.schema.TraitInitializers;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;
import software.amazon.awssdk.schema.serde.ErrorRegistry;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Checks the generated Error_Registry of the C2J-route prototype services against the error resolution of the legacy
 * clients: the same exception classes, reached from the same error codes.
 */
class PrototypeErrorRegistryTest {
    private static final String CLOUDWATCH_NS = "com.amazonaws.cloudwatch";

    @TempDir
    Path tmp;

    /**
     * The registry holds exactly the error shapes the intermediate model generates an exception class for, and every
     * error code a legacy client maps (the intermediate model's {@code errorCode}, which the legacy exception switch and
     * {@code registerModeledException} use) resolves to the shape of the same class.
     */
    @ParameterizedTest
    @ValueSource(strings = {"sqs", "route53", "sns", "cloudwatch", "dynamodb"})
    void errorRegistry_matchesTheLegacyExceptionClasses(String service) throws IOException {
        IntermediateModel model = prepare(service);
        OperationsClassSpec operations = SchemaCodegen.create(model, initializers()).operations();
        Map<ShapeId, String> classes = exceptionClasses(model);
        String namespace = model.serviceId().getNamespace();

        assertThat(operations.registeredErrors()).containsExactlyInAnyOrderElementsOf(classes.keySet());
        for (ShapeModel exception : exceptions(model)) {
            ShapeId resolved = resolve(operations, namespace, exception.getErrorCode());
            assertThat(classes.get(resolved)).as("%s %s", service, exception.getErrorCode())
                                             .isEqualTo(exception.getShapeName());
        }
    }

    /**
     * Every C2J error code a legacy {@code awsQuery} client would register ({@code error.code}, or the shape name)
     * resolves to the class the legacy route generates for it. The legacy route keeps the last declared of several C2J
     * shapes with the same class, and registers only that shape's code.
     */
    @ParameterizedTest
    @ValueSource(strings = {"sqs", "sns", "cloudwatch"})
    void errorRegistry_resolvesC2jQueryCodesToTheLegacyClass(String service) throws IOException {
        IntermediateModel model = prepare(service);
        OperationsClassSpec operations = SchemaCodegen.create(model, initializers()).operations();
        Map<ShapeId, String> classes = exceptionClasses(model);
        Function<ShapeId, ShapeId> owners = SchemaCodegen.errorShapeOwners(model);
        String namespace = model.serviceId().getNamespace();

        Map<String, String> codeByClass = new LinkedHashMap<>();
        JsonNode shapes = new ObjectMapper().readTree(directory(service).resolve("service-2.json").toFile())
                                            .get("shapes");
        for (Iterator<Map.Entry<String, JsonNode>> it = shapes.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> shape = it.next();
            if (!shape.getValue().path("exception").asBoolean(false)) {
                continue;
            }
            String className = classes.get(owners.apply(ShapeId.fromParts(namespace, shape.getKey())));
            String code = shape.getValue().path("error").path("code").asText(shape.getKey());
            // A later declaration of the same class replaces the earlier one, as on the legacy route.
            codeByClass.put(className, code);
        }
        // No two legacy-registered classes share a code, so the legacy registration order cannot matter.
        assertThat(codeByClass.values()).doesNotHaveDuplicates();
        for (Map.Entry<String, String> legacy : codeByClass.entrySet()) {
            assertThat(classes.get(resolve(operations, namespace, legacy.getValue())))
                .as("%s %s", service, legacy.getValue())
                .isEqualTo(legacy.getKey());
        }
    }

    /**
     * CloudWatch's {@code DashboardNotFoundError} and {@code ResourceNotFound} both have the code
     * {@code ResourceNotFound}. {@code ResourceNotFound} shares its class with {@code ResourceNotFoundException},
     * which the legacy route keeps, so the legacy clients resolve the code to {@code DashboardNotFoundErrorException};
     * the generated registry does too.
     */
    @Test
    void cloudWatch_sharedResourceNotFoundCode_resolvesToDashboardNotFoundError() throws Exception {
        IntermediateModel model = prepare("cloudwatch");
        SchemaCodegen codegen = SchemaCodegen.create(model, initializers());
        OperationsClassSpec operations = codegen.operations();

        assertThat(operations.registeredErrors())
            .contains(id("DashboardNotFoundError"), id("ResourceNotFoundException"), id("LimitExceededFault"))
            .doesNotContain(id("ResourceNotFound"), id("LimitExceededException"));
        assertThat(operations.errorCodes()).containsEntry("ResourceNotFound", id("DashboardNotFoundError"))
                                           .containsEntry("LimitExceeded", id("LimitExceededFault"));

        // The schemas reference the model classes (task 3.7), so the whole model package is compiled.
        Path classes = ModelPackageCompiler.compile(model, codegen, tmp.resolve("compile"));
        try (URLClassLoader loader = RuntimeCompiler.classLoader(classes)) {
            ErrorRegistry registry = (ErrorRegistry) Class.forName(operations.className().reflectionName(), true, loader)
                                                          .getField("ERROR_REGISTRY").get(null);
            assertThat(registry.resolveCode("ResourceNotFound")).hasToString(CLOUDWATCH_NS + "#DashboardNotFoundError");
            assertThat(registry.resolveCode("LimitExceeded")).hasToString(CLOUDWATCH_NS + "#LimitExceededFault");
            assertThat(registry.contains(runtimeId("ResourceNotFound"))).isFalse();
            assertThat(registry.contains(runtimeId("LimitExceededException"))).isFalse();
            // The schemas carry the generated exception classes and their builders (task 3.7).
            software.amazon.awssdk.schema.Schema schema = registry.getSchema(runtimeId("DashboardNotFoundError"));
            assertThat(schema.shapeClass().getSimpleName()).isEqualTo("DashboardNotFoundErrorException");
            assertThat(registry.getShapeClass(runtimeId("DashboardNotFoundError"))).isSameAs(schema.shapeClass());
            assertThat(registry.createBuilder(runtimeId("DashboardNotFoundError")).build())
                .isInstanceOf(schema.shapeClass());
        }
    }

    /**
     * Resolves an error code as the client protocols do for query and {@code awsQueryCompatible} services: the
     * {@code awsQueryError} code index first, then the code as a shape name in the service namespace.
     */
    private static ShapeId resolve(OperationsClassSpec operations, String namespace, String code) {
        ShapeId byCode = operations.errorCodes().get(code);
        if (byCode != null) {
            return byCode;
        }
        ShapeId byName;
        try {
            byName = ShapeId.fromParts(namespace, code);
        } catch (RuntimeException e) {
            return null;
        }
        return operations.registeredErrors().contains(byName) ? byName : null;
    }

    private static List<ShapeModel> exceptions(IntermediateModel model) {
        List<ShapeModel> result = new ArrayList<>();
        for (ShapeModel shape : model.getShapes().values()) {
            if (shape.getShapeType() == ShapeType.Exception) {
                result.add(shape);
            }
        }
        return result;
    }

    private static Map<ShapeId, String> exceptionClasses(IntermediateModel model) {
        Map<ShapeId, String> classes = new HashMap<>();
        for (ShapeModel exception : exceptions(model)) {
            classes.put(exception.shapeId(), exception.getShapeName());
        }
        return classes;
    }

    private static ShapeId id(String name) {
        return ShapeId.fromParts(CLOUDWATCH_NS, name);
    }

    private static software.amazon.awssdk.schema.ShapeId runtimeId(String name) {
        return software.amazon.awssdk.schema.ShapeId.fromParts(CLOUDWATCH_NS, name);
    }

    private static TraitInitializers initializers() {
        return TraitInitializers.load(PrototypeErrorRegistryTest.class.getClassLoader());
    }

    private IntermediateModel prepare(String service) {
        Path dir = directory(service);
        Assumptions.assumeTrue(Files.isRegularFile(dir.resolve("service-2.json")), "C2J model of " + service
                                                                                   + " not found");
        CustomizationConfig config = ModelLoaderUtils.loadOptionalModel(CustomizationConfig.class,
                                                                        dir.resolve("customization.config").toFile(),
                                                                        true)
                                                     .orElse(CustomizationConfig.create());
        ServiceInputs inputs = ServiceInputs.builder()
                                            .serviceName(service)
                                            .customizationConfig(config)
                                            .c2jModelDirectory(dir)
                                            .sourcesDirectory(tmp.resolve(service + "/src"))
                                            .resourcesDirectory(tmp.resolve(service + "/resources"))
                                            .testsDirectory(tmp.resolve(service + "/tests"))
                                            .buildDirectory(tmp.resolve(service + "/build"))
                                            .classLoader(CodegenPipeline.class.getClassLoader())
                                            .build();
        return CodegenPipeline.create().prepare(inputs).intermediateModel();
    }

    private static Path directory(String service) {
        Path dir = Paths.get("..", "services", service, "src", "main", "resources", "codegen-resources");
        if (!Files.isRegularFile(dir.resolve("service-2.json"))) {
            dir = dir.resolve(service);
        }
        return dir;
    }
}
