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

package software.amazon.awssdk.dynamicclient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import software.amazon.awssdk.utils.Lazy;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The Smithy protocol test models ({@code smithy-aws-protocol-tests} and {@code smithy-protocol-tests}), imported by
 * path from the JARs the build copies to {@code target/protocol-test-models}. They hold one service per
 * Supported_Protocol and the {@code smithy.test} response test cases.
 */
final class ProtocolTestModels {
    private static final Path DIRECTORY = Paths.get("target", "protocol-test-models");
    private static final String[] JARS = {
        "smithy-aws-protocol-tests.jar", "smithy-protocol-tests.jar", "smithy-protocol-test-traits.jar",
        "smithy-validation-model.jar"
    };
    private static final ShapeId HTTP_RESPONSE_TESTS = ShapeId.from("smithy.test#httpResponseTests");
    private static final Lazy<Model> MODEL = new Lazy<>(ProtocolTestModels::assemble);

    private ProtocolTestModels() {
    }

    static Model model() {
        return MODEL.getValue();
    }

    /**
     * The service of a protocol's tests.
     */
    static ShapeId service(SprocketsProtocol protocol) {
        switch (protocol) {
            case RPCV2_CBOR:
                return ShapeId.from("smithy.protocoltests.rpcv2Cbor#RpcV2Protocol");
            case AWS_JSON_1_0:
                return ShapeId.from("aws.protocoltests.json10#JsonRpc10");
            case AWS_JSON_1_1:
                return ShapeId.from("aws.protocoltests.json#JsonProtocol");
            case REST_JSON_1:
                return ShapeId.from("aws.protocoltests.restjson#RestJson");
            case REST_XML:
                return ShapeId.from("aws.protocoltests.restxml#RestXml");
            case AWS_QUERY:
                return ShapeId.from("aws.protocoltests.query#AwsQuery");
            case EC2_QUERY:
                return ShapeId.from("aws.protocoltests.ec2#AwsEc2");
            default:
                throw new IllegalArgumentException(protocol.toString());
        }
    }

    /**
     * The ID of the protocol's response test of its {@code InvalidGreeting} error (operation {@code GreetingWithErrors}).
     */
    static String invalidGreetingTest(SprocketsProtocol protocol) {
        switch (protocol) {
            case RPCV2_CBOR:
                return "RpcV2CborInvalidGreetingError";
            case AWS_JSON_1_0:
                return "AwsJson10InvalidGreetingError";
            case AWS_JSON_1_1:
                return "AwsJson11InvalidGreetingError";
            case REST_JSON_1:
                return "RestJsonInvalidGreetingError";
            case REST_XML:
                return "InvalidGreetingError";
            case AWS_QUERY:
                return "QueryInvalidGreetingError";
            case EC2_QUERY:
                return "Ec2InvalidGreetingError";
            default:
                throw new IllegalArgumentException(protocol.toString());
        }
    }

    /**
     * The response test case of a shape with this {@code id}, from its {@code smithy.test#httpResponseTests} trait.
     */
    static ObjectNode responseTest(ShapeId shape, String id) {
        ArrayNode cases = model().expectShape(shape).findTrait(HTTP_RESPONSE_TESTS)
                                 .orElseThrow(() -> new IllegalStateException(shape + " has no response tests"))
                                 .toNode().expectArrayNode();
        for (Node node : cases.getElements()) {
            ObjectNode testCase = node.expectObjectNode();
            if (testCase.expectStringMember("id").getValue().equals(id)) {
                return testCase;
            }
        }
        throw new IllegalStateException(shape + " has no response test " + id);
    }

    private static Model assemble() {
        ModelAssembler assembler = Model.assembler().discoverModels(Model.class.getClassLoader());
        for (String jar : JARS) {
            Path path = DIRECTORY.resolve(jar);
            if (!Files.isRegularFile(path)) {
                throw new IllegalStateException(path.toAbsolutePath() + " is missing: the build copies it in the "
                                                + "generate-test-resources phase (maven-dependency-plugin)");
            }
            assembler.addImport(path);
        }
        return assembler.assemble().unwrap();
    }
}
