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

package software.amazon.awssdk.codegen.poet.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.ClientTestModels;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.poet.schema.SchemaOperationRouting;
import software.amazon.awssdk.codegen.smithy.SmithyIntermediateModelBuilder;
import software.amazon.awssdk.codegen.smithy.SmithyModels;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.smithy.model.Model;

/**
 * Generated client methods of schema-enabled services call {@code withOperation(<Svc>Operations.OP)} with no
 * marshaller, response handler or exception metadata mapping (task 3.32, Requirements 23.6, 30.3, 37.2). The fixture
 * service, {@code schema-routing.smithy}, defaults to rpcv2Cbor like CloudWatch and has routed operations (with a host
 * prefix, request compression, a required checksum, pagination) and two that stay on the legacy path (an event
 * stream, and an input class without schema serde members). The golden files of services generated from C2J (the
 * other client class tests) do not change.
 */
class SchemaClientClassTest {
    private static final String CLOUDWATCH_LIKE_PROTOCOLS = "@rpcv2Cbor\n@awsJson1_0\n@awsQuery";

    @Test
    void syncClientClass_withRpcv2CborDefault_routesSupportedOperationsThroughTheOperationsClass() {
        assertThat(new SyncClientClass(params(model())), generatesTo("test-schema-sync-client-class.java"));
    }

    @Test
    void asyncClientClass_withRpcv2CborDefault_routesSupportedOperationsThroughTheOperationsClass() {
        assertThat(new AsyncClientClass(params(model())), generatesTo("test-schema-async-client-class.java"));
    }

    @Test
    void syncClientClass_forASchemaBasedService_hasNoPerOperationSerdeComponents() {
        assertThat(new SyncClientClass(params(schemaBasedModel())),
                   generatesTo("test-schema-service-sync-client-class.java"));
    }

    @Test
    void asyncClientClass_forASchemaBasedService_hasNoPerOperationSerdeComponents() {
        assertThat(new AsyncClientClass(params(schemaBasedModel())),
                   generatesTo("test-schema-service-async-client-class.java"));
    }

    @Test
    void routing_withUnsupportedOperations_keepsThemOnTheLegacyPath() {
        IntermediateModel model = model();
        SchemaOperationRouting routing = routing(model);

        assertThat(routing.operationsClass().reflectionName())
            .isEqualTo("software.amazon.awssdk.services.demo.model.DemoOperations");
        assertThat(routing.routes(model.getOperation("GetThing"))).isTrue();
        assertThat(routing.operationReference(model.getOperation("ListThings")).toString())
            .isEqualTo("software.amazon.awssdk.services.demo.model.DemoOperations.LIST_THINGS");
        assertThat(routing.renamesOperation(model.getOperation("GetThing"))).isFalse();

        assertThat(routing.legacyReason(model.getOperation("WatchThings"))).contains("event streams");
        assertThat(routing.legacyReason(model.getOperation("CountThings"))).contains("no schema serde members");
        assertThat(routing.hasLegacyOperations(model.getOperations().values())).isTrue();
        assertThatThrownBy(() -> routing.operationReference(model.getOperation("WatchThings")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("WatchThings is not routed through DemoOperations");
    }

    @Test
    void routing_whenAnErrorClassHasNoSchemaSerdeMembers_keepsEveryOperationOnTheLegacyPath() {
        // The service-wide Error_Registry could resolve InternalFault for any operation, and could not build it.
        IntermediateModel model = model(resource().replace("structure InternalFault {",
                                                           "structure InternalFault {\n    Code: BigInteger"));
        SchemaOperationRouting routing = routing(model);

        assertThat(routing.legacyReason(model.getOperation("GetThing")))
            .isEqualTo("the error classes of [demo#InternalFault] have no schema serde members");
        String sync = PoetUtils.buildJavaFile(new SyncClientClass(params(model))).toString();
        assertThat(sync).doesNotContain("withOperation(")
                        .contains(".withMarshaller(new GetThingRequestMarshaller(protocolFactory))");
    }

    @Test
    void clientClasses_whenEveryOperationIsRouted_haveNoProtocolFactoryOrErrorResponseHandler() {
        IntermediateModel model = model();
        model.getOperations().remove("WatchThings");
        model.getOperations().remove("CountThings");

        String sync = PoetUtils.buildJavaFile(new SyncClientClass(params(model))).toString();
        String async = PoetUtils.buildJavaFile(new AsyncClientClass(params(model))).toString();

        for (String client : new String[] {sync, async}) {
            assertThat(client).contains(".withOperation(DemoOperations.GET_THING)")
                              .doesNotContain("protocolFactory", "createErrorResponseHandler", "Marshaller",
                                              "HttpResponseHandler", "ExceptionMetadata", "JsonOperationMetadata");
        }
    }

    @Test
    void syncClientClass_withoutEventStreamMethods_doesNotCountTheEventStreamOperationAsLegacy() {
        IntermediateModel model = model();
        model.getOperations().remove("CountThings");

        String sync = PoetUtils.buildJavaFile(new SyncClientClass(params(model))).toString();
        String async = PoetUtils.buildJavaFile(new AsyncClientClass(params(model))).toString();

        assertThat(sync).doesNotContain("protocolFactory");
        assertThat(async).contains("new WatchThingsRequestMarshaller(protocolFactory)");
    }

    @Test
    void routing_withADefaultProtocolWithoutAFactory_isEmptyAndKeepsTheLegacyCalls() {
        // awsJson1_0 has no ClientProtocolFactory in phase 2, so the client keeps its marshallers.
        IntermediateModel model = model(resource().replace(CLOUDWATCH_LIKE_PROTOCOLS, "@awsJson1_0\n@awsQuery"));

        assertThat(SchemaOperationRouting.forClient(model, params(model).getSchemaCodegen())).isEmpty();
        String sync = PoetUtils.buildJavaFile(new SyncClientClass(params(model))).toString();
        assertThat(sync).doesNotContain("withOperation(", "DemoOperations")
                        .contains(".withMarshaller(new GetThingRequestMarshaller(protocolFactory))");
    }

    @Test
    void routing_withAC2jModel_isEmpty() {
        IntermediateModel model = ClientTestModels.rpcv2ServiceModels();

        assertThat(SchemaOperationRouting.forClient(model, params(model).getSchemaCodegen())).isEmpty();
    }

    private static SchemaOperationRouting routing(IntermediateModel model) {
        return SchemaOperationRouting.forClient(model, params(model).getSchemaCodegen())
                                     .orElseThrow(AssertionError::new);
    }

    private static GeneratorTaskParams params(IntermediateModel model) {
        return GeneratorTaskParams.create(model, "sources/", "tests/", "resources/");
    }

    private static IntermediateModel model() {
        return model(resource());
    }

    /**
     * The fixture without the two operations that stay on the legacy path: a schema-based service (task 5.1).
     */
    private static IntermediateModel schemaBasedModel() {
        IntermediateModel model = model(resource().replace(", WatchThings, CountThings]", "]"));
        assertThat(SchemaOperationRouting.routesEveryOperation(model, params(model).getSchemaCodegen())).isTrue();
        return model;
    }

    private static IntermediateModel model(String src) {
        Model smithy = Model.assembler()
                            .discoverModels(Model.class.getClassLoader())
                            .addUnparsedModel("schema-routing.smithy", src)
                            .assemble()
                            .unwrap();
        return new SmithyIntermediateModelBuilder(SmithyModels.builder()
                                                              .model(smithy)
                                                              .customizationConfig(CustomizationConfig.create())
                                                              .build())
            .build();
    }

    private static String resource() {
        try (InputStream in = SchemaClientClassTest.class.getResourceAsStream("schema-routing.smithy")) {
            return IoUtils.toUtf8String(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
