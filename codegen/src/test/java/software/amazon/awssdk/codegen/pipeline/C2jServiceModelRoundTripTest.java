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

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.codegen.c2j2smithy.C2jInput;
import software.amazon.awssdk.codegen.c2j2smithy.C2jToSmithyConverter;
import software.amazon.awssdk.codegen.c2j2smithy.ConversionResult;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ModelSerializer;

/**
 * Stage 2 of the Codegen_Pipeline applies C2J customizations to codegen's {@link ServiceModel} POJO and hands the
 * result to the C2J_Converter as a JSON tree. This test checks that the POJO round trip loses nothing the converter
 * reads: for each Prototype_Service_Set service, converting the raw {@code service-2.json} and converting the POJO
 * written back by {@link C2jServiceModelSerializer} give the same Smithy model and the same gap report.
 */
class C2jServiceModelRoundTripTest {

    @ParameterizedTest
    @ValueSource(strings = {"sqs", "secretsmanager", "account", "route53", "sns", "cloudwatch", "dynamodb"})
    void roundTrippedPojo_convertsToTheSameSmithyModelAsTheRawFile(String service) {
        Path dir = prototypeServiceDirectory(service);
        Assumptions.assumeTrue(Files.isRegularFile(dir.resolve(C2jInput.SERVICE_MODEL_FILE)),
                               "C2J model of " + service + " not found");

        C2jInput raw = C2jInput.fromDirectory(service, dir);
        ServiceModel pojo = ModelLoaderUtils.loadModel(ServiceModel.class,
                                                       dir.resolve(C2jInput.SERVICE_MODEL_FILE).toFile());
        C2jInput roundTripped = withServiceModel(raw, C2jServiceModelSerializer.toTree(pojo));

        ConversionResult expected = C2jToSmithyConverter.convert(raw);
        ConversionResult actual = C2jToSmithyConverter.convert(roundTripped);

        assertThat(differences(serialize(expected), serialize(actual))).isEmpty();
        assertThat(actual.gapReport()).isEqualTo(expected.gapReport());
    }

    @ParameterizedTest
    @ValueSource(strings = {"sqs", "secretsmanager", "account", "route53", "sns", "cloudwatch", "dynamodb"})
    void writtenTree_readsBackToAnEqualTree(String service) {
        Path dir = prototypeServiceDirectory(service);
        Assumptions.assumeTrue(Files.isRegularFile(dir.resolve(C2jInput.SERVICE_MODEL_FILE)),
                               "C2J model of " + service + " not found");

        ServiceModel pojo = ModelLoaderUtils.loadModel(ServiceModel.class,
                                                       dir.resolve(C2jInput.SERVICE_MODEL_FILE).toFile());
        JsonNode tree = C2jServiceModelSerializer.toTree(pojo);

        assertThat(C2jServiceModelSerializer.toTree(C2jServiceModelSerializer.fromTree(tree))).isEqualTo(tree);
    }

    static Path prototypeServiceDirectory(String service) {
        Path dir = Paths.get("..", "services", service, "src", "main", "resources", "codegen-resources");
        if (!Files.isRegularFile(dir.resolve(C2jInput.SERVICE_MODEL_FILE))) {
            dir = dir.resolve(service);
        }
        return dir;
    }

    static C2jInput withServiceModel(C2jInput input, JsonNode serviceModel) {
        return C2jInput.builder()
                       .serviceName(input.serviceName())
                       .serviceModel(serviceModel)
                       .paginatorsModel(input.paginatorsModel())
                       .waitersModel(input.waitersModel())
                       .endpointRuleSetModel(input.endpointRuleSetModel())
                       .endpointTestsModel(input.endpointTestsModel())
                       .build();
    }

    private static ObjectNode serialize(ConversionResult result) {
        return ModelSerializer.builder().build().serialize(result.model());
    }

    /**
     * One entry per shape whose serialized form differs, including member order, with both forms.
     */
    private static List<String> differences(ObjectNode expected, ObjectNode actual) {
        Map<StringNode, Node> expectedShapes = shapes(expected);
        Map<StringNode, Node> actualShapes = shapes(actual);
        TreeSet<String> ids = new TreeSet<>();
        expectedShapes.keySet().forEach(k -> ids.add(k.getValue()));
        actualShapes.keySet().forEach(k -> ids.add(k.getValue()));

        List<String> differences = new ArrayList<>();
        for (String id : ids) {
            Optional<Node> e = Optional.ofNullable(expectedShapes.get(StringNode.from(id)));
            Optional<Node> a = Optional.ofNullable(actualShapes.get(StringNode.from(id)));
            diff(id, e.orElse(null), a.orElse(null), differences);
        }
        return differences;
    }

    /**
     * Records the path of each difference, with both values. The order of shape members counts, because it decides
     * the generated member order; the order of keys in trait values does not.
     */
    private static void diff(String path, Node expected, Node actual, List<String> differences) {
        if (expected != null && actual != null && expected.isObjectNode() && actual.isObjectNode()) {
            Map<StringNode, Node> e = expected.expectObjectNode().getMembers();
            Map<StringNode, Node> a = actual.expectObjectNode().getMembers();
            boolean orderMatters = path.endsWith("/members");
            if (orderMatters && !new ArrayList<>(e.keySet()).equals(new ArrayList<>(a.keySet()))
                && e.keySet().equals(a.keySet())) {
                differences.add(path + ": member order " + e.keySet() + " != " + a.keySet());
            }
            TreeSet<String> keys = new TreeSet<>();
            e.keySet().forEach(k -> keys.add(k.getValue()));
            a.keySet().forEach(k -> keys.add(k.getValue()));
            for (String key : keys) {
                diff(path + "/" + key, e.get(StringNode.from(key)), a.get(StringNode.from(key)), differences);
            }
            return;
        }
        String expectedJson = expected == null ? "<absent>" : Node.printJson(expected);
        String actualJson = actual == null ? "<absent>" : Node.printJson(actual);
        if (!expectedJson.equals(actualJson)) {
            differences.add(path + ": raw " + abbreviate(expectedJson) + " != round trip " + abbreviate(actualJson));
        }
    }

    private static String abbreviate(String json) {
        return json.length() <= 300 ? json : json.substring(0, 300) + "...";
    }

    private static Map<StringNode, Node> shapes(ObjectNode model) {
        return model.expectObjectMember("shapes").getMembers();
    }
}
