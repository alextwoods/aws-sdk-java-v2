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

    /**
     * A restJson1 service with a streaming blob input (with a header member) and a streaming blob output (task 7.19).
     */
    private static final String REST_JSON_STREAMING = String.join(
        "\n",
        "$version: \"2.0\"",
        "namespace streams",
        "use aws.api#service",
        "use aws.auth#sigv4",
        "use aws.protocols#restJson1",
        "@service(sdkId: \"Streams\")",
        "@sigv4(name: \"streams\")",
        "@restJson1",
        "service StreamsService {",
        "    version: \"2024-01-01\"",
        "    operations: [PutBlob, GetBlob]",
        "}",
        "@http(method: \"PUT\", uri: \"/blobs/{Name}\")",
        "operation PutBlob {",
        "    input: PutBlobRequest",
        "    output: PutBlobResponse",
        "}",
        "@readonly",
        "@http(method: \"GET\", uri: \"/blobs/{Name}\")",
        "operation GetBlob {",
        "    input: GetBlobRequest",
        "    output: GetBlobResponse",
        "}",
        "structure PutBlobRequest {",
        "    @required @httpLabel Name: String",
        "    @httpHeader(\"Content-Type\") ContentType: String",
        "    @httpPayload Body: BlobBody = \"\"",
        "}",
        "structure PutBlobResponse {}",
        "structure GetBlobRequest {",
        "    @required @httpLabel Name: String",
        "}",
        "structure GetBlobResponse {",
        "    @httpHeader(\"ETag\") ETag: String",
        "    @httpPayload Body: BlobBody = \"\"",
        "}",
        "@streaming",
        "blob BlobBody",
        "");

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
    void routing_withAStreamingBlobAndAnRpcDefaultProtocol_keepsTheOperationOnTheLegacyPath() {
        String src = resource().replace(", CountThings]", ", CountThings, UploadThing]")
                     + "\noperation UploadThing {\n    input: UploadThingRequest\n    output: UploadThingResponse\n}\n"
                     + "structure UploadThingRequest {\n    @httpPayload\n    Body: ThingBody = \"\"\n}\n"
                     + "structure UploadThingResponse {}\n"
                     + "@streaming\nblob ThingBody\n";
        IntermediateModel model = model(src);

        assertThat(routing(model).legacyReason(model.getOperation("UploadThing")))
            .isEqualTo("streaming blob members are bound only by HTTP binding protocols, not smithy.protocols#rpcv2Cbor");
    }

    @Test
    void clientClasses_withRestJson1StreamingOperations_routeThemWithTheBodyAndTheTransformer() {
        IntermediateModel model = model(REST_JSON_STREAMING, staged());
        SchemaOperationRouting routing = routing(model);
        assertThat(routing.routes(model.getOperation("PutBlob"))).isTrue();
        assertThat(routing.routes(model.getOperation("GetBlob"))).isTrue();

        String sync = PoetUtils.buildJavaFile(new SyncClientClass(params(model))).toString();
        String async = PoetUtils.buildJavaFile(new AsyncClientClass(params(model))).toString();

        for (String client : new String[] {sync, async}) {
            assertThat(client).contains(".withOperation(StreamsOperations.PUT_BLOB)", ".withOperation(StreamsOperations.GET_BLOB)")
                              .doesNotContain("StreamingRequestMarshaller", "Marshaller(", "protocolFactory");
        }
        assertThat(sync).contains(".withRequestBody(requestBody)", ".withResponseTransformer(responseTransformer), "
                                                                    + "responseTransformer);");
        assertThat(async).contains(".withAsyncRequestBody(requestBody)",
                                   ".withAsyncResponseTransformer(asyncResponseTransformer), asyncResponseTransformer)",
                                   "finalAsyncResponseTransformer.exceptionOccurred(e)",
                                   "endOfStreamFuture.whenComplete(");
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
        return model(src, CustomizationConfig.create());
    }

    private static IntermediateModel model(String src, CustomizationConfig customization) {
        Model smithy = Model.assembler()
                            .discoverModels(Model.class.getClassLoader())
                            .addUnparsedModel("schema-routing.smithy", src)
                            .assemble()
                            .unwrap();
        return new SmithyIntermediateModelBuilder(SmithyModels.builder()
                                                              .model(smithy)
                                                              .customizationConfig(customization)
                                                              .build())
            .build();
    }

    /**
     * restJson1 is a staged client protocol, available with {@code stagedClientProtocols} (the Protocol_Test_Services).
     */
    private static CustomizationConfig staged() {
        CustomizationConfig customization = CustomizationConfig.create();
        customization.setStagedClientProtocols(true);
        return customization;
    }

    private static String resource() {
        try (InputStream in = SchemaClientClassTest.class.getResourceAsStream("schema-routing.smithy")) {
            return IoUtils.toUtf8String(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
