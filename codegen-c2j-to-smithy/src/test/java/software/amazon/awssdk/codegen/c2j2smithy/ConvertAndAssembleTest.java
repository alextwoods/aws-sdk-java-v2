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

package software.amazon.awssdk.codegen.c2j2smithy;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Port of the source branch's {@code ConvertAndAssembleTest} main: convert a C2J model, serialize it to the Smithy JSON
 * AST, and re-assemble it through the validating {@code ModelAssembler}, with the marker trait definitions discovered
 * from the class path. A converted model must assemble with no ERROR or DANGER event (Requirement 17.13).
 */
class ConvertAndAssembleTest {

    static final String[] FIXTURES = {"json10", "json11", "restjson", "restxml", "query", "ec2", "cbor"};

    @ParameterizedTest
    @ValueSource(strings = {"json10", "json11", "restjson", "restxml", "query", "ec2", "cbor"})
    void convert_fixture_reassemblesWithoutErrors(String fixture) {
        ConversionResult result = TestModels.convertFixture(fixture);

        assertThat(errorEvents(result)).isEmpty();
    }

    /**
     * Converts the C2J models (service, paginators, waiters, endpoint rule set and tests) of the Prototype_Service_Set
     * from the {@code services} modules of this repository, when they are present.
     */
    @ParameterizedTest
    @ValueSource(strings = {"sqs", "secretsmanager", "account", "route53", "sns", "cloudwatch", "dynamodb"})
    void convert_prototypeService_reassemblesWithoutErrors(String service) {
        Path dir = prototypeServiceDirectory(service);
        Assumptions.assumeTrue(Files.isRegularFile(dir.resolve(C2jInput.SERVICE_MODEL_FILE)),
                               "C2J model of " + service + " not found");

        ConversionResult result = C2jToSmithyConverter.convert(C2jInput.fromDirectory(service, dir));

        assertThat(errorEvents(result)).isEmpty();
        assertThat(result.model().getServiceShapes()).hasSize(1);
        assertThat(result.gapReport().service()).isEqualTo(service);
    }

    static Path prototypeServiceDirectory(String service) {
        Path dir = Paths.get("..", "services", service, "src", "main", "resources", "codegen-resources");
        if (!Files.isRegularFile(dir.resolve(C2jInput.SERVICE_MODEL_FILE))) {
            dir = dir.resolve(service);
        }
        return dir;
    }

    static List<String> errorEvents(ConversionResult result) {
        Model converted = result.model();
        String json = Node.printJson(ModelSerializer.builder().build().serialize(converted));

        ClassLoader classLoader = ConvertAndAssembleTest.class.getClassLoader();
        ValidatedResult<Model> reassembled = Model.assembler(classLoader)
                                                  .discoverModels(classLoader)
                                                  .addUnparsedModel("converted.json", json)
                                                  .assemble();
        List<String> errors = reassembled.getValidationEvents().stream()
                                         .filter(e -> e.getSeverity() == Severity.ERROR
                                                      || e.getSeverity() == Severity.DANGER)
                                         .map(ValidationEvent::toString)
                                         .collect(Collectors.toList());
        if (errors.isEmpty()) {
            assertThat(reassembled.unwrap().getShapeIds()).containsAll(converted.getShapeIds());
        }
        return errors;
    }
}
