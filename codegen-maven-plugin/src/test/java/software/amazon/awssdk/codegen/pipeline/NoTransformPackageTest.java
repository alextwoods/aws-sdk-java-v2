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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.pipeline.PerOperationSerdeScanner.Rule;
import software.amazon.awssdk.codegen.pipeline.PerOperationSerdeScanner.Violation;
import software.amazon.awssdk.codegen.poet.schema.SchemaCodegen;
import software.amazon.awssdk.codegen.poet.schema.SchemaOperationRouting;

/**
 * Checks that a schema-based Prototype_Service_Set service generates no legacy per-operation serde components (task
 * 5.2, design A.15 "Generators removed", Requirements 17.12, 36.4, 37.2): each module is generated through the
 * {@link CodegenPipeline} as {@code GenerationMojo} does ({@link LegacyRouteParity#generateWithPipeline}), and its
 * generated sources and tests are scanned by {@link PerOperationSerdeScanner} for a {@code transform} package, a
 * reference to a {@code transform} class, a {@code *Marshaller} or {@code *Unmarshaller} type, the per-operation
 * exception-mapper {@code switch} and the legacy {@code ExceptionMetadata} exception mapping.
 *
 * <p>Whether a service is schema-based is decided by the codegen itself:
 * {@link SchemaOperationRouting#routesEveryOperation}, the condition under which {@code MarshallerGeneratorTasks}
 * generates no marshallers (task 5.1). A module is schema-based when every service it generates is.
 * {@link #SCHEMA_BASED_MODULES} lists the modules that are expected to be schema-based, and the test fails, instead of
 * skipping, when the codegen and the list disagree:
 *
 * <ul>
 *     <li>a listed module that is not schema-based fails {@link #schemaBasedModule_generatesNoPerOperationSerde}, with
 *     the reason its first legacy operation stays on the legacy path;</li>
 *     <li>an unlisted module that has become schema-based fails
 *     {@link #legacyModule_isNotSchemaBased_andStillGeneratesPerOperationSerde}, so that it is added to the list and
 *     checked.</li>
 * </ul>
 *
 * <p>Tasks 9.2–9.4 switch the remaining modules to schema-based serde. Each of them adds the modules it switches to
 * {@link #SCHEMA_BASED_MODULES} (and updates the module's {@code legacy-route-parity} list, see
 * {@code LegacyRouteParityTest}). When the last module is added, {@link #legacyModules} is empty and
 * {@link #legacyModule_isNotSchemaBased_andStillGeneratesPerOperationSerde} is to be deleted. A module that generates
 * a schema-based and a legacy service (DynamoDB and DynamoDB Streams share one module) fails both tests until all of
 * its services are schema-based.
 */
class NoTransformPackageTest {
    /**
     * The Prototype_Service_Set modules ({@link LegacyRouteParity#PROTOTYPE_MODULES}) whose services are all
     * schema-based. Tasks 9.2–9.4 add to it.
     */
    private static final Set<String> SCHEMA_BASED_MODULES = Collections.unmodifiableSet(
        new LinkedHashSet<>(Arrays.asList("cloudwatch")));

    @TempDir
    Path tmp;

    static Stream<String> schemaBasedModules() {
        return SCHEMA_BASED_MODULES.stream();
    }

    static Stream<String> legacyModules() {
        return LegacyRouteParity.PROTOTYPE_MODULES.stream().filter(m -> !SCHEMA_BASED_MODULES.contains(m));
    }

    @Test
    void schemaBasedModules_arePrototypeModules() {
        assertThat(LegacyRouteParity.PROTOTYPE_MODULES).containsAll(SCHEMA_BASED_MODULES);
    }

    @ParameterizedTest
    @MethodSource("schemaBasedModules")
    void schemaBasedModule_generatesNoPerOperationSerde(String module) {
        assertThat(LegacyRouteParity.codegenResources(module)).as("C2J model of " + module).isDirectory();
        Path out = tmp.resolve("out");

        Map<String, String> notSchemaBased = notSchemaBasedServices(generate(module, out), out);

        assertThat(notSchemaBased)
            .as("%s is listed in SCHEMA_BASED_MODULES, but these of its services are not schema-based", module)
            .isEmpty();
        assertThat(scan(out))
            .as("%s generates legacy per-operation serde components", module)
            .isEmpty();
    }

    @ParameterizedTest
    @MethodSource("legacyModules")
    void legacyModule_isNotSchemaBased_andStillGeneratesPerOperationSerde(String module) {
        Assumptions.assumeTrue(Files.isDirectory(LegacyRouteParity.codegenResources(module)),
                               "C2J model of " + module + " not found");
        Path out = tmp.resolve("out");

        List<PreparedService> services = generate(module, out);

        assertThat(notSchemaBasedServices(services, out).keySet())
            .as("%s has become schema-based: add it to NoTransformPackageTest.SCHEMA_BASED_MODULES", module)
            .isNotEmpty();
        // The scanner finds the legacy components in a service that still generates them.
        Set<Rule> rules = scan(out).stream().map(Violation::rule).collect(Collectors.toCollection(
            () -> EnumSet.noneOf(Rule.class)));
        assertThat(rules).contains(Rule.TRANSFORM_PACKAGE, Rule.TRANSFORM_REFERENCE, Rule.MARSHALLER_TYPE,
                                   Rule.EXCEPTION_METADATA);
    }

    private List<PreparedService> generate(String module, Path out) {
        return LegacyRouteParity.generateWithPipeline(module, out, tmp.resolve("build"));
    }

    private static List<Violation> scan(Path out) {
        return PerOperationSerdeScanner.scan(out.resolve("src"), out.resolve("tests"));
    }

    /**
     * The services that are not schema-based, each with the reason.
     *
     * @param out the directory the services were generated into, which a model sharer's parameters check
     */
    private static Map<String, String> notSchemaBasedServices(List<PreparedService> services, Path out) {
        Map<String, String> reasons = new LinkedHashMap<>();
        for (PreparedService service : services) {
            IntermediateModel model = service.intermediateModel();
            SchemaCodegen schemaCodegen = GeneratorTaskParams.create(model, out.resolve("src").toString(),
                                                                     out.resolve("tests").toString(),
                                                                     out.resolve("resources").toString())
                                                             .getSchemaCodegen();
            if (!SchemaOperationRouting.routesEveryOperation(model, schemaCodegen)) {
                reasons.put(model.getMetadata().getServiceName(), legacyReason(model, schemaCodegen));
            }
        }
        return reasons;
    }

    private static String legacyReason(IntermediateModel model, SchemaCodegen schemaCodegen) {
        Optional<SchemaOperationRouting> routing = SchemaOperationRouting.forClient(model, schemaCodegen);
        if (!routing.isPresent()) {
            return "the client has no runtime protocol selection or no schemas";
        }
        for (OperationModel operation : model.getOperations().values()) {
            String reason = routing.get().legacyReason(operation);
            if (reason != null) {
                return operation.getOperationName() + ": " + reason;
            }
        }
        return "unknown";
    }
}
