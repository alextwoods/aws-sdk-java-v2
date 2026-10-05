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

package software.amazon.awssdk.dynamic.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.dynamic.client.TestSupport.WEATHER;
import static software.amazon.awssdk.dynamic.client.TestSupport.doc;
import static software.amazon.awssdk.dynamic.client.TestSupport.json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.api.ModeledAuth;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.dynamic.client.TestSupport.RecordingInterceptor;
import software.amazon.awssdk.dynamic.client.TestSupport.Recorded;
import software.amazon.awssdk.dynamic.client.TestSupport.TestTransport;
import software.amazon.awssdk.dynamic.model.DynamicModel;
import software.amazon.awssdk.dynamic.model.DynamicModelException;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.pipeline.AwsFeaturePlugin;
import software.amazon.awssdk.pipeline.AwsOperation;
import software.amazon.awssdk.pipeline.ExecutionInterceptor;
import software.amazon.awssdk.pipeline.HookContext;
import software.amazon.awssdk.pipeline.PipelinePlan;
import software.amazon.awssdk.regions.Region;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Synchronous dynamic calls on the shared pipeline (Requirements 14.2-14.8, 14.11, 14.14, 14.15, 14.19): service and
 * operation selection with lexical diagnostics, finite Document input and output, configuration, overrides,
 * interceptors, plugins, Modern Auth, endpoint rules and modeled errors.
 */
class DynamicClientTest {

    private static final String FORECAST =
        "{\"city\":\"seattle\",\"high\":21,\"readings\":[{\"hour\":9,\"value\":17.5}]}";

    private static Document forecastInput() {
        return doc(Map.of("city", Document.of("seattle"), "days", Document.of(3)));
    }

    @Test
    void finiteCallReturnsTheModeledOutputThroughTheSharedPipeline() {
        TestTransport transport = TestTransport.sync(r -> json(200, FORECAST));
        try (DynamicClient client = TestSupport.syncClient(transport).build()) {
            Document output = client.call("GetForecast", forecastInput());

            assertEquals(ShapeId.from("example.weather#GetForecastOutput"), output.discriminator());
            assertEquals("seattle", output.getMember("city").asString());
            assertEquals(21, output.getMember("high").asInteger());
            List<Document> readings = output.getMember("readings").asList();
            assertEquals(1, readings.size());
            assertEquals(17.5, readings.get(0).getMember("value").asDouble());

            Recorded sent = transport.requests().get(0);
            SdkHttpRequest request = sent.request();
            assertEquals(1, transport.transmissions());
            assertEquals("POST", request.method());
            // The endpoint rule set, bound through the input's contextParam member.
            assertEquals("seattle.weather.us-west-2.example.com", request.host());
            assertEquals("Weather.GetForecast", sent.header("X-Amz-Target"));
            assertEquals("application/x-amz-json-1.0", sent.header("Content-Type"));
            // Modern Auth: SigV4 with the modeled signing name and the configured region.
            String authorization = sent.header("Authorization");
            assertNotNull(authorization);
            assertTrue(authorization.startsWith("AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/"), authorization);
            assertTrue(authorization.contains("/us-west-2/weather/aws4_request"), authorization);
            assertTrue(sent.body().contains("\"city\":\"seattle\""), sent.body());
            assertTrue(sent.body().contains("\"days\":3"), sent.body());
            // The idempotency token feature of the plan filled the token once.
            assertTrue(sent.body().contains("\"token\":\""), sent.body());
        }
    }

    @Test
    void unitOperationsTakeAndReturnEmptyDocuments() {
        TestTransport transport = TestTransport.sync(r -> json(200, "{}"));
        try (DynamicClient client = TestSupport.syncClient(transport).build()) {
            Document output = client.call("Ping", Document.of(Map.of()));
            assertEquals(Map.of(), output.asStringMap());
            assertEquals("Weather.Ping", transport.requests().get(0).header("X-Amz-Target"));
            // Without the contextParam, the region rule.
            assertEquals("weather.us-west-2.example.com", transport.requests().get(0).request().host());
        }
    }

    @Test
    void unknownOperationIsRejectedWithLexicalDiagnosticsBeforeTransport() {
        TestTransport transport = TestTransport.sync(r -> json(200, "{}"));
        RecordingInterceptor interceptor = new RecordingInterceptor();
        try (DynamicClient client = TestSupport.syncClient(transport).addExecutionInterceptor(interceptor).build()) {
            // Names are exact: case matters.
            DynamicModelException e = assertThrows(DynamicModelException.class,
                                                   () -> client.call("getForecast", forecastInput()));
            assertEquals(DynamicModelException.Reason.UNKNOWN_OPERATION, e.reason());
            assertEquals(WEATHER, e.service().orElseThrow());
            assertEquals("getForecast", e.operationName().orElseThrow());
            assertEquals(List.of("GetForecast", "Ping", "Upload"), e.available());
            assertTrue(e.getMessage().contains("GetForecast, Ping, Upload"), e.getMessage());
        }
        assertEquals(0, transport.transmissions());
        assertEquals(List.of(), interceptor.hooks);
    }

    @Test
    void streamingOperationsNeedAStreamingCall() {
        TestTransport transport = TestTransport.sync(r -> json(200, "{}"));
        try (DynamicClient client = TestSupport.syncClient(transport).build()) {
            DynamicInvocationException e = assertThrows(DynamicInvocationException.class,
                                                        () -> client.call("Upload", Document.of(Map.of())));
            assertEquals(DynamicInvocationException.Reason.REQUIRES_STREAMING_CALL, e.reason());
            assertEquals(ShapeId.from("example.weather#Upload"), e.operation());
            assertTrue(e.getMessage().contains("'body'"), e.getMessage());
        }
        assertEquals(0, transport.transmissions());
    }

    @Test
    void theOnlyServiceOfAModelIsSelectedWithoutAnId() {
        try (DynamicClient client = TestSupport.syncClient(TestTransport.sync(r -> json(200, "{}"))).build()) {
            assertEquals(WEATHER, client.service().id());
            assertSame(TestSupport.weather().service(), client.service());
        }
    }

    @Test
    void serviceSelectionIsExactAndListsServicesInLexicalOrder() {
        DynamicModel model = TestSupport.load(TestSupport.TWO_SERVICES_MODEL);
        TestTransport transport = TestTransport.sync(r -> json(200, "{}"));
        DynamicModelException ambiguous = assertThrows(
            DynamicModelException.class, () -> TestSupport.syncClient(transport).model(model).build());
        assertEquals(DynamicModelException.Reason.AMBIGUOUS_SERVICE, ambiguous.reason());
        assertEquals(List.of("example.multi#Alpha", "example.multi#Zeta"), ambiguous.available());

        DynamicModelException unknown = assertThrows(
            DynamicModelException.class,
            () -> TestSupport.syncClient(transport).model(model).serviceId(ShapeId.from("example.multi#alpha"))
                             .build());
        assertEquals(DynamicModelException.Reason.UNKNOWN_SERVICE, unknown.reason());
        assertEquals(List.of("example.multi#Alpha", "example.multi#Zeta"), unknown.available());

        try (DynamicClient client = TestSupport.syncClient(transport).model(model)
                                               .serviceId(ShapeId.from("example.multi#Zeta"))
                                               .endpointOverride(java.net.URI.create("https://zeta.example.com"))
                                               .build()) {
            assertEquals(ShapeId.from("example.multi#Zeta"), client.service().id());
            assertEquals(List.of("ZetaPing"), client.service().operationNames());
            client.call("ZetaPing", Document.of(Map.of()));
            assertEquals("Zeta.ZetaPing", transport.requests().get(0).header("X-Amz-Target"));
            assertEquals("zeta.example.com", transport.requests().get(0).request().host());
        }

        DynamicModelException none = assertThrows(
            DynamicModelException.class,
            () -> TestSupport.syncClient(transport).model(TestSupport.load(TestSupport.NO_SERVICE_MODEL)).build());
        assertEquals(DynamicModelException.Reason.NO_SERVICE, none.reason());
        assertEquals(1, transport.transmissions());
    }

    @Test
    void aServiceIdMustMatchAGivenService() {
        TestTransport transport = TestTransport.sync(r -> json(200, "{}"));
        assertThrows(IllegalStateException.class,
                     () -> TestSupport.syncClient(transport).service(TestSupport.weather().service())
                                      .serviceId(ShapeId.from("example.weather#Other")).build());
        assertThrows(IllegalStateException.class, () -> DynamicClient.builder().httpClient(transport).build());
    }

    @Test
    void clientConfigurationOverridesAndInterceptorsAreTheGeneratedClientOnes() {
        TestTransport transport = TestTransport.sync(r -> json(200, FORECAST));
        RecordingInterceptor interceptor = new RecordingInterceptor();
        try (DynamicClient client = TestSupport.syncClient(transport).addExecutionInterceptor(interceptor).build()) {
            assertEquals(Region.US_WEST_2, client.serviceClientConfiguration().get(AwsClientOption.AWS_REGION)
                                                 .orElseThrow());

            AwsRequestOverrideConfiguration override = AwsRequestOverrideConfiguration.builder()
                                                                                      .putHeader("x-override", "1")
                                                                                      .build();
            client.call("GetForecast", forecastInput(), override);
            client.call("GetForecast", forecastInput());

            List<Recorded> sent = transport.requests();
            assertEquals("1", sent.get(0).header("x-override"));
            assertEquals(null, sent.get(1).header("x-override"));
            assertEquals(List.of("beforeExecution", "beforeTransmission", "afterExecution",
                                 "beforeExecution", "beforeTransmission", "afterExecution"), interceptor.hooks);
            // The interceptor sees the dynamic operation's pipeline binding: the dynamic-model descriptor, its
            // modeled auth and its schema-guided input.
            HookContext first = interceptor.contexts.get(0);
            AwsOperation<?, ?> operation = first.operation();
            assertSame(client.service().operation("GetForecast").descriptor(), operation.descriptor());
            ModeledAuth auth = operation.auth().orElseThrow();
            assertEquals("example.weather#GetForecast", auth.operationId());
            assertEquals(List.of("aws.auth#sigv4"), auth.effectiveSchemeIds());
            assertEquals(Map.of("name", "weather"), auth.schemeTraitValues("aws.auth#sigv4").orElseThrow());
            assertEquals("GetForecast", operation.endpointBinding().operationName());
            assertEquals(Map.of("City", "city"), operation.endpointBinding().contextParams());
            assertEquals("seattle", first.request().getMemberValue(
                client.service().operation("GetForecast").schemas().input().member("city")));
            // Every call of one client and operation shares one binding and plan.
            assertSame(operation, interceptor.contexts.get(3).operation());
        }
    }

    @Test
    void pluginsConfigureEachPlanOnceAndContributeToItsCalls() {
        TestTransport transport = TestTransport.sync(r -> json(200, FORECAST));
        CountingPlugin plugin = new CountingPlugin();
        try (DynamicClient client = TestSupport.syncClient(transport).addPlugin(plugin).build()) {
            client.call("GetForecast", forecastInput());
            client.call("GetForecast", forecastInput());
            client.call("Ping", Document.of(Map.of()));
        }
        // Plans are built once per client and operation, and the plugin applied only to GetForecast.
        assertEquals(1, plugin.configured.get());
        assertEquals(List.of("example.weather#GetForecast", "example.weather#Ping"), plugin.asked);
        List<Recorded> sent = transport.requests();
        assertEquals("on", sent.get(0).header("x-plugin"));
        assertEquals("on", sent.get(1).header("x-plugin"));
        assertEquals(null, sent.get(2).header("x-plugin"));
        // The header was added before signing, so it is signed like any other.
        assertTrue(sent.get(0).header("Authorization").contains("x-plugin"), sent.get(0).header("Authorization"));
    }

    @Test
    void modeledErrorsSurfaceAsDynamicServiceExceptions() {
        TestTransport transport = TestTransport.sync(r -> json(
            400, "{\"__type\":\"example.weather#NotFound\",\"message\":\"no forecast\",\"city\":\"atlantis\"}"));
        try (DynamicClient client = TestSupport.syncClient(transport).build()) {
            DynamicServiceException e = assertThrows(DynamicServiceException.class,
                                                     () -> client.call("GetForecast", forecastInput()));
            assertEquals(ShapeId.from("example.weather#NotFound"), e.errorShapeId());
            assertEquals("atlantis", e.error().getMember("city").asString());
            assertEquals(400, e.statusCode());
            assertTrue(e.getMessage().startsWith("no forecast"), e.getMessage());
            assertEquals("no forecast", e.getMemberValue(e.schema().member("message")));
            assertFalse(e.retryable());
            assertEquals(1, transport.transmissions());
            // A copy keeps the modeled members.
            DynamicServiceException copy = (DynamicServiceException) e.toBuilder().numAttempts(7).build();
            assertEquals("atlantis", copy.error().getMember("city").asString());
            assertEquals(7, copy.numAttempts());
        }
    }

    @Test
    void unmodeledErrorsSurfaceAsAwsServiceExceptions() {
        TestTransport transport = TestTransport.sync(r -> json(400, "{\"__type\":\"Mystery\",\"message\":\"?\"}"));
        try (DynamicClient client = TestSupport.syncClient(transport).build()) {
            AwsServiceException e = assertThrows(AwsServiceException.class,
                                                 () -> client.call("GetForecast", forecastInput()));
            assertFalse(e instanceof DynamicServiceException, e.toString());
            assertEquals(400, e.statusCode());
        }
    }

    @Test
    void typedDocumentsOfTheInputSchemaAreAcceptedAsIs() {
        TestTransport transport = TestTransport.sync(r -> json(200, FORECAST));
        try (DynamicClient client = TestSupport.syncClient(transport).build()) {
            var operation = client.service().operation("GetForecast");
            var builder = operation.inputBuilder().get();
            builder.setMemberValue(operation.schemas().input().member("city"), "seattle");
            Document typed = builder.build();
            assertInstanceOf(Document.class, client.call("GetForecast", typed));
            assertTrue(transport.requests().get(0).body().contains("\"city\":\"seattle\""));
        }
    }

    /** Adds a header before signing to the calls of {@code GetForecast}; counts its plan contributions. */
    private static final class CountingPlugin implements AwsFeaturePlugin {
        final AtomicInteger configured = new AtomicInteger();
        final List<String> asked = new ArrayList<>();

        @Override
        public ShapeId id() {
            return ShapeId.from("example.test#CountingPlugin");
        }

        @Override
        public boolean appliesTo(AwsOperation<?, ?> operation) {
            asked.add(operation.id().toString());
            return operation.id().getName().equals("GetForecast");
        }

        @Override
        public void configure(PipelinePlan.Builder<?, ?> plan) {
            configured.incrementAndGet();
            plan.addInterceptor(new ExecutionInterceptor() {
                @Override
                public SdkHttpRequest modifyBeforeSigning(HookContext.ModifyBeforeSigning context) {
                    return context.httpRequest().toBuilder().putHeader("x-plugin", "on").build();
                }
            });
        }
    }
}
