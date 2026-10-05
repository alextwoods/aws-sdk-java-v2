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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.endpoints.internal.Json;
import software.amazon.awssdk.endpoints.rules.EndpointTestSuite;
import software.amazon.awssdk.test.codegen.support.CompiledSources;
import software.amazon.awssdk.test.codegen.support.GeneratedEndpointVectors;

/**
 * Requirement 19.2 for parameter binding: every canonical endpoint test case that has {@code operationInputs} (the
 * pinned models' cases that exercise static, context and operation-context parameters, client context parameters and
 * built-ins) runs through the generated binding of the real service, generated from its canonical model. These are
 * exactly the services of the pinned checkout whose endpoint tests have operation inputs.
 */
class CanonicalEndpointBindingTest {

    static Stream<Arguments> services() {
        return Stream.of(
            // Generated types are named after the V2 service name (the SDK ID in V2 Pascal case).
            Arguments.of("dynamodb", "com.amazonaws.dynamodb#DynamoDB_20120810", "DynamoDb", 267, true),
            Arguments.of("eventbridge", "com.amazonaws.eventbridge#AWSEvents", "EventBridge", 1, true),
            Arguments.of("kinesis", "com.amazonaws.kinesis#Kinesis_20131202", "Kinesis", 39, true),
            // S3's Object and Error structures are generated as V2's S3Object and S3Error, so the whole S3 tree
            // compiles and every input is a typed request.
            Arguments.of("s3", "com.amazonaws.s3#AmazonS3", "S3", 215, true),
            Arguments.of("s3control", "com.amazonaws.s3control#AWSS3ControlServiceV20180820", "S3Control", 187,
                         true),
            Arguments.of("sts", "com.amazonaws.sts#AWSSecurityTokenServiceV20110615", "Sts", 18, true));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("services")
    void everyOperationInputResolvesThroughTheGeneratedBinding(String directory, String service, String typeName,
                                                               int expectedInputs, boolean typedInputs,
                                                               @TempDir Path temp)
        throws Exception {
        Path models = Path.of(System.getProperty("v3.canonicalModels")).resolve(directory).resolve("smithy");
        assertTrue(Files.isDirectory(models), "The canonical model " + models + " does not exist");
        String packageName = "software.amazon.awssdk.services." + directory;
        AwsCodegenResult result = AwsCodegen.generate(new AwsCodegenRequest(service, packageName, models,
                                                                            temp.resolve("out")), List.of());
        EndpointTestSuite suite = suite(models.resolve("model.json"), service);
        assertEquals(expectedInputs, GeneratedEndpointVectors.operationInputCount(suite));
        String[] compiled = typedInputs ? new String[0]
                                        : new String[] {packageName.replace('.', '/') + "/endpoints/"};
        try (URLClassLoader loader = CompiledSources.compile(result.sourceRoot(), temp.resolve("classes"), compiled)
                                                    .loader()) {
            List<String> failures = new GeneratedEndpointVectors(loader, packageName, typeName, typedInputs)
                .run(suite);
            assertEquals(List.of(), failures, failures.size() + " operation inputs failed");
        }
    }

    @SuppressWarnings("unchecked")
    private static EndpointTestSuite suite(Path model, String service) throws Exception {
        Map<String, Object> root = Json.parseObject(Files.readString(model));
        Map<String, Object> shape = (Map<String, Object>)
            ((Map<String, Object>) root.get("shapes")).get(service);
        Map<String, Object> traits = (Map<String, Object>) shape.get("traits");
        return EndpointTestSuite.fromDocument((Map<String, Object>) traits.get("smithy.rules#endpointTests"));
    }
}
