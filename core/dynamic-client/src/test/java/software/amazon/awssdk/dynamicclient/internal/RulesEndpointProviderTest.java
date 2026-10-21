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

package software.amazon.awssdk.dynamicclient.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.dynamicclient.DynamicEndpointParams;
import software.amazon.awssdk.dynamicclient.schema.StructDocument;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.traits.EndpointBddTrait;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;

class RulesEndpointProviderTest {
    private static final Model PARAMS_MODEL = InternalTestModels.assemble("endpoint-params.smithy");
    private static final Path ACCOUNT_MODEL = Paths.get("..", "..", "services", "account", "model", "model.json");
    private static final Path SQS_RESOURCES = Paths.get("..", "..", "services", "sqs", "src", "main", "resources",
                                                         "codegen-resources");
    private static final ShapeId ACCOUNT = ShapeId.from("com.amazonaws.account#Account");

    @Test
    void accountRuleSet_matchesTheEndpointTestsOfTheModel() {
        Model model = accountModel();
        assertThat(InternalTestModels.onlyService(model).hasTrait(EndpointRuleSetTrait.class)).isTrue();

        int checked = runEndpointTests(model, ACCOUNT, endpointTests(InternalTestModels.onlyService(model)));

        assertThat(checked).isGreaterThan(10);
    }

    @Test
    void accountBdd_withoutTheRuleSet_matchesTheEndpointTestsOfTheModel() {
        Model full = accountModel();
        Model bddOnly = ModelTransformer.create().removeTraitsIf(full, (shape, trait) ->
            trait.toShapeId().equals(EndpointRuleSetTrait.ID));
        ServiceShape service = InternalTestModels.onlyService(bddOnly);
        assertThat(service.hasTrait(EndpointRuleSetTrait.class)).isFalse();
        assertThat(service.hasTrait(EndpointBddTrait.class)).isTrue();

        int checked = runEndpointTests(bddOnly, ACCOUNT, endpointTests(InternalTestModels.onlyService(full)));

        assertThat(checked).isGreaterThan(10);
    }

    @Test
    void sqsRuleSet_matchesItsEndpointTests() throws IOException {
        assumeTrue(Files.exists(SQS_RESOURCES.resolve("endpoint-rule-set.json")));
        Node ruleSet = Node.parse(read(SQS_RESOURCES.resolve("endpoint-rule-set.json")));
        Node tests = Node.parse(read(SQS_RESOURCES.resolve("endpoint-tests.json")));
        OperationShape operation = OperationShape.builder().id("com.amazonaws.sqs#GetQueueUrl").build();
        ServiceShape service = ServiceShape.builder()
                                           .id("com.amazonaws.sqs#AmazonSQS")
                                           .version("2012-11-05")
                                           .addOperation(operation)
                                           .addTrait(EndpointRuleSetTrait.builder().ruleSet(ruleSet).build())
                                           .build();
        Model model = Model.assembler()
                           .discoverModels(Model.class.getClassLoader())
                           .addShape(operation)
                           .addShape(service)
                           .assemble()
                           .unwrap();

        int checked = runEndpointTests(model, service.getId(), tests.expectObjectNode());

        assertThat(checked).isGreaterThan(20);
    }

    @Test
    void resolveEndpoint_regionsFipsAndDualStack_resolveTheAccountEndpoints() {
        Model model = accountModel();
        RulesEndpointProvider provider = RulesEndpointProvider.forService(model, InternalTestModels.onlyService(model));
        ApiOperation<StructDocument, StructDocument> operation =
            InternalTestModels.operation(model, ShapeId.from("com.amazonaws.account#GetContactInformation"));

        assertThat(url(provider, operation, attributes(Region.US_EAST_1, false, false)))
            .isEqualTo("https://account.us-east-1.amazonaws.com");
        assertThat(url(provider, operation, attributes(Region.US_EAST_1, true, false)))
            .isEqualTo("https://account-fips.us-east-1.amazonaws.com");
        // Account is global in each partition: the rules send every region of aws to us-east-1, and of aws-cn to
        // cn-northwest-1.
        assertThat(url(provider, operation, attributes(Region.EU_WEST_1, false, true)))
            .isEqualTo("https://account.us-east-1.api.aws");
        assertThat(url(provider, operation, attributes(Region.US_GOV_WEST_1, true, true)))
            .isEqualTo("https://account-fips.us-gov-west-1.api.aws");
        assertThat(url(provider, operation, attributes(Region.CN_NORTH_1, false, false)))
            .isEqualTo("https://account.cn-northwest-1.amazonaws.com.cn");
    }

    @Test
    void resolveEndpoint_endpointPropertiesAndHeaders_becomeEndpointAttributes() {
        RulesEndpointProvider provider = paramsProvider();
        Endpoint endpoint = provider.resolveEndpoint(params("Plain", Collections.emptyMap(),
                                                            attributes(Region.US_WEST_2, true, false))).join();

        assertThat(endpoint.url()).isEqualTo(URI.create("https://service-fips.us-west-2.example.com"));
        assertThat(endpoint.headers()).containsEntry("x-test", Arrays.asList("a", "b"));
        assertThat(endpoint.attribute(AwsEndpointAttribute.METRIC_VALUES)).containsExactly("1", "2");
        List<EndpointAuthScheme> authSchemes = endpoint.attribute(AwsEndpointAttribute.AUTH_SCHEMES);
        assertThat(authSchemes).hasSize(1);
        SigV4AuthScheme sigv4 = (SigV4AuthScheme) authSchemes.get(0);
        assertThat(sigv4.signingName()).isEqualTo("service");
        assertThat(sigv4.signingRegion()).isEqualTo("us-west-2");
        assertThat(sigv4.isDisableDoubleEncodingSet()).isFalse();
    }

    @Test
    void resolveEndpoint_errorRule_failsWithTheRuleMessage() {
        RulesEndpointProvider provider = paramsProvider();

        assertThat(failure(provider.resolveEndpoint(params("Plain", Collections.emptyMap(), new ExecutionAttributes()))))
            .hasMessage("A region is required");
    }

    @Test
    void resolveEndpoint_contextParam_bindsTheInputMember() {
        assertThat(paramsUrl("GetObject", members("bucket", Document.of("my-bucket"))))
            .isEqualTo("https://my-bucket.bucket.example.com");
        assertThat(paramsUrl("GetObject", Collections.emptyMap())).isEqualTo("https://service.us-west-2.example.com");
    }

    @Test
    void resolveEndpoint_staticContextParams_winOverContextParam() {
        assertThat(paramsUrl("PutObject", members("bucket", Document.of("my-bucket"))))
            .isEqualTo("https://static-bucket.bucket.example.com");
        assertThat(paramsUrl("Staged", Collections.emptyMap())).isEqualTo("https://beta.stage.example.com");
    }

    @Test
    void resolveEndpoint_operationContextParams_evaluateJmesPathOverTheInput() {
        Map<String, Document> input = new HashMap<>();
        input.put("Nested", Document.of(members("Name", Document.of("nested-name"))));
        input.put("Items", Document.of(Arrays.asList(Document.of(members("Key", Document.of("k1"))),
                                                     Document.of(members("Key", Document.of("k2"))))));
        RulesEndpointProvider provider = paramsProvider();

        Map<Identifier, Value> bound = provider.bindParameters(params("ListItems", input,
                                                                      attributes(Region.US_WEST_2, false, false)), null);

        assertThat(bound.get(Identifier.of("Name"))).isEqualTo(Value.stringValue("nested-name"));
        assertThat(bound.get(Identifier.of("Keys")))
            .isEqualTo(Value.arrayValue(Arrays.asList(Value.stringValue("k1"), Value.stringValue("k2"))));
        assertThat(bound.get(Identifier.of("Region"))).isEqualTo(Value.stringValue("us-west-2"));
        assertThat(bound.get(Identifier.of("UseFIPS"))).isEqualTo(Value.booleanValue(false));
        assertThat(paramsUrl("ListItems", input)).isEqualTo("https://nested-name.name.example.com");
        assertThat(paramsUrl("ListItems", Collections.emptyMap())).isEqualTo("https://service.us-west-2.example.com");
    }

    @Test
    void resolveEndpoint_contextParam_winsOverOperationContextParams() {
        Map<String, Document> input = new HashMap<>();
        input.put("name", Document.of("member-name"));
        input.put("Other", Document.of("path-name"));

        assertThat(paramsUrl("Describe", input)).isEqualTo("https://member-name.name.example.com");
        assertThat(paramsUrl("Describe", members("Other", Document.of("path-name"))))
            .isEqualTo("https://path-name.name.example.com");
    }

    @Test
    void resolveEndpoint_withEndpointOverride_returnsTheOverrideWithoutEvaluatingTheRules() {
        ExecutionAttributes attributes = attributes(Region.US_WEST_2, true, false);
        attributes.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER,
                                ClientEndpointProvider.forEndpointOverride(URI.create("https://localhost:8443/base")));

        Endpoint endpoint = paramsProvider().resolveEndpoint(params("Plain", Collections.emptyMap(), attributes)).join();

        assertThat(endpoint.url()).isEqualTo(URI.create("https://localhost:8443/base"));
        assertThat(endpoint.headers()).isEmpty();
    }

    @Test
    void resolveEndpoint_serviceWithoutRules_failsUnlessAnEndpointOverrideIsSet() {
        Model model = InternalTestModels.assembleIdl("$version: \"2.0\"\nnamespace test.norules\n"
                                                     + "service NoRules { operations: [Op] }\noperation Op {}\n");
        RulesEndpointProvider provider = RulesEndpointProvider.forService(model, InternalTestModels.onlyService(model));
        ApiOperation<StructDocument, StructDocument> operation =
            InternalTestModels.operation(model, ShapeId.from("test.norules#Op"));
        DynamicEndpointParams params = DynamicEndpointParams.builder()
                                                            .operation(operation)
                                                            .input(InternalTestModels.input(operation,
                                                                                            Collections.emptyMap()))
                                                            .executionAttributes(attributes(Region.US_WEST_2, false,
                                                                                            false))
                                                            .build();
        ExecutionAttributes overridden = attributes(Region.US_WEST_2, false, false);
        overridden.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER,
                                ClientEndpointProvider.forEndpointOverride(URI.create("https://localhost")));

        assertThat(provider.hasRules()).isFalse();
        assertThat(failure(provider.resolveEndpoint(params)))
            .hasMessage("No endpoint provider or endpointRuleSet available for service test.norules#NoRules: its model "
                        + "has neither a smithy.rules#endpointRuleSet nor a smithy.rules#endpointBdd trait. Configure "
                        + "an endpointOverride or an endpointProvider on the DynamicClient builder.");
        assertThat(provider.resolveEndpoint(params.toBuilder().executionAttributes(overridden).build()).join().url())
            .isEqualTo(URI.create("https://localhost"));
    }

    /**
     * The exception a future completed with, checked to be an {@link SdkClientException}.
     */
    private static Throwable failure(CompletableFuture<Endpoint> future) {
        Throwable thrown = catchThrowable(future::join);
        assertThat(thrown).isInstanceOf(CompletionException.class);
        assertThat(thrown.getCause()).isInstanceOf(SdkClientException.class);
        return thrown.getCause();
    }

    private static int runEndpointTests(Model model, ShapeId serviceId, ObjectNode tests) {
        ServiceShape service = model.expectShape(serviceId, ServiceShape.class);
        RulesEndpointProvider provider = RulesEndpointProvider.forService(model, service);
        OperationShape anyOperation = TopDownIndex.of(model).getContainedOperations(service).iterator().next();
        ApiOperation<StructDocument, StructDocument> operation = InternalTestModels.operation(model, anyOperation.getId());
        List<String> failures = new ArrayList<>();
        int checked = 0;
        for (Node testCase : tests.expectArrayMember("testCases").getElements()) {
            ObjectNode test = testCase.expectObjectNode();
            ObjectNode testParams = test.getObjectMember("params").orElse(Node.objectNode());
            ExecutionAttributes attributes = new ExecutionAttributes();
            if (!toAttributes(testParams, attributes)) {
                continue;
            }
            DynamicEndpointParams params = DynamicEndpointParams.builder()
                                                                .operation(operation)
                                                                .input(InternalTestModels.input(operation,
                                                                                                Collections.emptyMap()))
                                                                .executionAttributes(attributes)
                                                                .build();
            ObjectNode expect = test.expectObjectMember("expect");
            String description = test.getStringMemberOrDefault("documentation", testParams.toString());
            try {
                Endpoint endpoint = provider.resolveEndpoint(params).join();
                String expectedUrl = expect.getObjectMember("endpoint").map(e -> e.expectStringMember("url").getValue())
                                           .orElse(null);
                if (!endpoint.url().toString().equals(expectedUrl)) {
                    failures.add(description + ": expected " + expect + " but got " + endpoint.url());
                }
            } catch (CompletionException e) {
                String expectedError = expect.getStringMemberOrDefault("error", null);
                if (expectedError == null || !e.getCause().getMessage().equals(expectedError)) {
                    failures.add(description + ": expected " + expect + " but failed with " + e.getCause());
                }
            }
            checked++;
        }
        assertThat(failures).isEmpty();
        return checked;
    }

    /**
     * Sets the built-in parameters of an endpoint test case as the client would. Returns false for a case with a
     * parameter the client does not bind from its configuration: {@code Endpoint}, because an endpoint override skips
     * the rules (Requirement 27.6).
     */
    private static boolean toAttributes(ObjectNode testParams, ExecutionAttributes attributes) {
        for (Map.Entry<String, Node> param : testParams.getStringMap().entrySet()) {
            switch (param.getKey()) {
                case "Region":
                    attributes.putAttribute(AwsExecutionAttribute.AWS_REGION,
                                            Region.of(param.getValue().expectStringNode().getValue()));
                    break;
                case "UseFIPS":
                    attributes.putAttribute(AwsExecutionAttribute.FIPS_ENDPOINT_ENABLED,
                                            param.getValue().expectBooleanNode().getValue());
                    break;
                case "UseDualStack":
                    attributes.putAttribute(AwsExecutionAttribute.DUALSTACK_ENDPOINT_ENABLED,
                                            param.getValue().expectBooleanNode().getValue());
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    private static ObjectNode endpointTests(ServiceShape service) {
        return service.findTrait(ShapeId.from("smithy.rules#endpointTests")).get().toNode().expectObjectNode();
    }

    private static String url(RulesEndpointProvider provider, ApiOperation<StructDocument, StructDocument> operation,
                              ExecutionAttributes attributes) {
        return provider.resolveEndpoint(DynamicEndpointParams.builder()
                                                             .operation(operation)
                                                             .input(InternalTestModels.input(operation,
                                                                                             Collections.emptyMap()))
                                                             .executionAttributes(attributes)
                                                             .build())
                       .join()
                       .url()
                       .toString();
    }

    private static String paramsUrl(String operation, Map<String, Document> input) {
        return paramsProvider().resolveEndpoint(params(operation, input, attributes(Region.US_WEST_2, false, false)))
                               .join()
                               .url()
                               .toString();
    }

    private static RulesEndpointProvider paramsProvider() {
        return RulesEndpointProvider.forService(PARAMS_MODEL, InternalTestModels.onlyService(PARAMS_MODEL));
    }

    private static DynamicEndpointParams params(String operationName, Map<String, Document> input,
                                                ExecutionAttributes attributes) {
        ApiOperation<StructDocument, StructDocument> operation =
            InternalTestModels.operation(PARAMS_MODEL, ShapeId.from("test.endpoints#" + operationName));
        return DynamicEndpointParams.builder()
                                    .operation(operation)
                                    .input(InternalTestModels.input(operation, input))
                                    .executionAttributes(attributes)
                                    .build();
    }

    private static Map<String, Document> members(String name, Document value) {
        Map<String, Document> members = new HashMap<>();
        members.put(name, value);
        return members;
    }

    private static ExecutionAttributes attributes(Region region, boolean fips, boolean dualStack) {
        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(AwsExecutionAttribute.AWS_REGION, region);
        attributes.putAttribute(AwsExecutionAttribute.FIPS_ENDPOINT_ENABLED, fips);
        attributes.putAttribute(AwsExecutionAttribute.DUALSTACK_ENDPOINT_ENABLED, dualStack);
        return attributes;
    }

    private static Model accountModel() {
        assumeTrue(Files.exists(ACCOUNT_MODEL));
        return Model.assembler().discoverModels(Model.class.getClassLoader()).addImport(ACCOUNT_MODEL).assemble()
                    .unwrap();
    }

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
