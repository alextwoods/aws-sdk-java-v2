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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static software.amazon.awssdk.dynamicclient.CallTestClients.map;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.protocol.AsyncClientTransport;
import software.amazon.awssdk.core.client.protocol.AsyncResponseHandoff;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.core.client.protocol.ClientTransport;
import software.amazon.awssdk.core.client.protocol.HttpMessageExchange;
import software.amazon.awssdk.core.client.protocol.MessageExchange;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.dynamicclient.schema.internal.ModelConversions;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.signer.BearerHttpSigner;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.utils.Validate;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * The behavioral cases DC-1 through DC-30 of the Dynamic Client SEP's Testing section, one or more test methods per
 * case, each named after its case. Every case uses the SEP's reference model ({@code sprockets.smithy}) with the
 * case's "Given" traits applied ({@link SprocketsModels}); a case that depends on the protocol runs for each of the
 * seven Supported_Protocols, with the protocol trait swapped and hand-written wire fixtures ({@link SprocketsProtocol}).
 * DC-13 also runs the {@code InvalidGreeting} response test case of each protocol's Smithy protocol test service
 * ({@link ProtocolTestModels}), and DC-2 and DC-5 also use the Account prototype model.
 *
 * <p>Event streams are deferred (task 7.20 stage (c), user decision): DC-22, DC-28 and DC-29 assert that the call
 * fails before anything is sent. DC-21 and DC-27 stream blobs on restJson1 and restXml; the five RPC protocols have
 * no streaming blob binding in the SDK, so their cases assert the SDK's behavior there.
 */
class DynamicClientBehaviorTest {
    private static final String NS = "smithy.example#";
    private static final byte[] CONTENT = "sprocket bytes, streamed".getBytes(StandardCharsets.UTF_8);
    private static final byte[] BLOB = {1, 2, 3};

    /**
     * DC-17 and DC-18 Given: {@code GetSprocket} has a non-streaming blob member. The reference model's only one,
     * {@code UploadSprocket$checksum}, is next to a required stream, which the RPC protocols reject, and cannot be
     * bound in restJson1 or restXml (see {@link SprocketsModels}).
     */
    private static final Map<SprocketsProtocol, Model> BLOB_MODELS = new ConcurrentHashMap<>();

    private final RecordingHttpClients.Sync sync = new RecordingHttpClients.Sync();
    private final RecordingHttpClients.Async async = new RecordingHttpClients.Async();
    private final List<DynamicClient> clients = new ArrayList<>();

    @AfterEach
    void closeClients() {
        clients.forEach(DynamicClient::close);
    }

    @Test
    void dc01_buildWithoutAModel_failsBecauseTheModelIsRequired() {
        DynamicClient.Builder builder = DynamicClient.builder()
                                                     .region(Region.US_WEST_2)
                                                     .credentialsProvider(TestCredentials.PROVIDER)
                                                     .httpClient(new StubHttpClients.Sync())
                                                     .httpClient(new StubHttpClients.Async());

        assertThatThrownBy(builder::build)
            .isInstanceOf(NullPointerException.class)
            .hasMessage("A Smithy model is required");
    }

    @ParameterizedTest
    @EnumSource(SprocketsProtocol.class)
    void dc02_modelWithOneServiceAndNoServiceId_resolvesThatService(SprocketsProtocol protocol) {
        Model model = SprocketsModels.forProtocol(protocol);

        DynamicClient client = client(model);

        assertThat(client.serviceShape()).isSameAs(model.expectShape(SprocketsModels.SERVICE));
        assertThat(client.service().schema().id()).isEqualTo(sdkId(NS + "Sprockets"));
        assertThat(client.service().schema().type()).isEqualTo(ShapeType.SERVICE);
    }

    @Test
    void dc02_accountPrototypeModel_resolvesItsOnlyService() {
        DynamicClient client = client(DynamicTestModels.accountPrototype());

        assertThat(client.serviceShape().getId()).isEqualTo(DynamicTestModels.ACCOUNT);
    }

    @Test
    void dc03_modelWithTwoServicesAndNoServiceId_failsNamingBoth() {
        Model model = SprocketsModels.builder().given("service OtherSprockets { operations: [GetSprocket] }").build();

        assertThatThrownBy(() -> client(model))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("No serviceId is set, and the model contains multiple services: [smithy.example#OtherSprockets, "
                        + "smithy.example#Sprockets]. Set serviceId to one of them.");
        assertThat(clientBuilder(model).serviceId(SprocketsModels.SERVICE).build().serviceShape().getId())
            .isEqualTo(SprocketsModels.SERVICE);
    }

    @Test
    void dc04_modelWithoutServices_fails() {
        Model reference = SprocketsModels.reference();
        Model model = ModelTransformer.create().removeShapes(
            reference, Collections.singleton(reference.expectShape(SprocketsModels.SERVICE)));

        assertThatThrownBy(() -> client(model))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("No serviceId is set, and the model contains no services");
    }

    @ParameterizedTest
    @EnumSource(SprocketsProtocol.class)
    void dc05_noExplicitProtocol_resolvesTheImplementationOfTheServiceProtocol(SprocketsProtocol protocol) {
        DynamicClient client = client(SprocketsModels.forProtocol(protocol));

        assertThat(client.syncClientConfiguration().option(SdkClientOption.CLIENT_PROTOCOL))
            .isInstanceOf(protocol.protocolClass());
        assertThat(client.asyncClientConfiguration().option(SdkClientOption.CLIENT_PROTOCOL))
            .isInstanceOf(protocol.protocolClass());
    }

    @Test
    void dc05_accountPrototypeModel_resolvesRestJson1() {
        DynamicClient client = client(DynamicTestModels.accountPrototype());

        assertThat(client.syncClientConfiguration().option(SdkClientOption.CLIENT_PROTOCOL))
            .isInstanceOf(SprocketsProtocol.REST_JSON_1.protocolClass());
    }

    @Test
    void dc06_protocolTraitWithoutAnImplementation_failsNamingTheProtocolTraits() {
        Model model = SprocketsModels.builder()
                                     .protocolTraits("@sprocketsProtocol")
                                     .given("@trait(selector: \"service\")", "@protocolDefinition",
                                            "structure sprocketsProtocol {}")
                                     .build();

        assertThatThrownBy(() -> client(model))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageStartingWith("Service smithy.example#Sprockets declares no protocol that the dynamic client "
                                    + "supports. Protocol traits on the service: [smithy.example#sprocketsProtocol]. "
                                    + "Supported protocols: [smithy.protocols#rpcv2Cbor, aws.protocols#awsJson1_0,");
    }

    @ParameterizedTest
    @EnumSource(SprocketsProtocol.class)
    void dc07_explicitTransportOfTheProtocolsMessageExchange_selectsTheMatchingProtocol(SprocketsProtocol protocol) {
        RecordingTransport transport = new RecordingTransport(() -> protocol.output("GetSprocket", "id", "1"));
        SdkPlugin explicitTransport = config -> {
            SdkClientConfiguration.Builder options = CallTestClients.clientOptions(config);
            options.option(SdkClientOption.CLIENT_TRANSPORT, transport.sync);
            options.option(SdkClientOption.ASYNC_CLIENT_TRANSPORT, transport.async);
        };
        DynamicClient client = track(clientBuilder(SprocketsModels.forProtocol(protocol))
                                         .addPlugin(explicitTransport)
                                         .build());

        ClientProtocol<?, ?> selected = client.syncClientConfiguration().option(SdkClientOption.CLIENT_PROTOCOL);
        Document output = client.call("GetSprocket", map("id", "1"));
        Document asyncOutput = client.callAsync("GetSprocket", map("id", "1")).join();

        assertThat(selected).isInstanceOf(protocol.protocolClass());
        assertThat(selected.messageExchange()).isEqualTo(transport.sync.messageExchange());
        assertThat(output.getMember("id").asString()).isEqualTo("1");
        assertThat(asyncOutput.getMember("id").asString()).isEqualTo("1");
        assertThat(transport.requests).hasSize(2);
        assertThat(sync.requests()).isEmpty();
        assertThat(async.requests()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(SprocketsProtocol.class)
    void dc08_noEndpointProviderAndAStaticEndpointRuleSet_resolvesTheRuleUrlAtCallTime(SprocketsProtocol protocol) {
        DynamicClient client = track(clientBuilder(SprocketsModels.forProtocol(protocol)).build());
        respond(protocol.output("GetSprocket", "id", "1"));

        client.call("GetSprocket", map("id", "1"));

        SdkHttpRequest request = sync.lastRequest().request;
        assertThat(request.protocol()).isEqualTo("https");
        assertThat(request.host()).isEqualTo("example.com");
        assertThat(request.encodedPath()).isEqualTo(protocol.httpBindings() ? "/get-sprocket" : rpcPath(protocol));
    }

    @ParameterizedTest
    @EnumSource(SprocketsProtocol.class)
    void dc09_callWithoutInput_returnsAStructureDocument(SprocketsProtocol protocol) {
        DynamicClient client = client(SprocketsModels.forProtocol(protocol));
        respond(protocol.output("CreateSprocket", "id", "1"));

        Document output = client.call("CreateSprocket");

        assertThat(output.type()).isEqualTo(ShapeType.STRUCTURE);
        assertThat(output.getDiscriminator()).isEqualTo(sdkId(NS + "CreateSprocketOutput"));
        assertThat(output.getMember("id").asString()).isEqualTo("1");
    }

    /**
     * The SEP's native-map overload is not offered: design C.2 removed {@code call(String, Map)}, so the input is
     * {@code Document.of(Map<String, Document>)}, the SDK's form of {@code Document.ofObject(Map.of("id", "1"))}.
     */
    @ParameterizedTest
    @EnumSource(SprocketsProtocol.class)
    void dc10_callWithAMapDocument_serializesTheModeledMemberName(SprocketsProtocol protocol) {
        DynamicClient client = client(SprocketsModels.forProtocol(protocol));
        respond(protocol.output("GetSprocket", "id", "1"));

        Document output = client.call("GetSprocket", Document.of(Collections.singletonMap("id", Document.of("1"))));

        assertThat(protocol.requestMembers(sync.lastRequest())).containsExactly(entry(protocol.wireName("id"), "1"));
        assertThat(output.getMember("id").asString()).isEqualTo("1");
    }

    @ParameterizedTest
    @EnumSource(SprocketsProtocol.class)
    void dc11_interceptorInspectingTheInput_seesTheInputDocument(SprocketsProtocol protocol) {
        List<Document> seen = new CopyOnWriteArrayList<>();
        ExecutionInterceptor inspector = new ExecutionInterceptor() {
            @Override
            public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
                seen.add(((DynamicRequest) context.request()).document());
            }
        };
        DynamicClient client = client(SprocketsModels.forProtocol(protocol), inspector);
        respond(protocol.output("GetSprocket", "id", "1"));

        client.call("GetSprocket", map("id", "1"));
        client.callAsync("GetSprocket", map("id", "1")).join();

        assertThat(seen).hasSize(2).allSatisfy(input -> {
            assertThat(input.getMember("id").asString()).isEqualTo("1");
            assertThat(input.getDiscriminator()).isEqualTo(sdkId(NS + "GetSprocketInput"));
        });
    }

    @Test
    void dc12_callOfAnOperationNotInTheService_failsNamingTheOperationAndTheService() {
        DynamicClient client = client(SprocketsModels.reference());
        String message = "Operation DeleteSprocket not found in service smithy.example#Sprockets";

        assertThatThrownBy(() -> client.call("DeleteSprocket", map("id", "1")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(message);
        assertThat(client.callAsync("DeleteSprocket", map("id", "1")))
            .failsWithin(Duration.ofSeconds(5))
            .withThrowableOfType(ExecutionException.class)
            .havingCause()
            .isInstanceOf(IllegalArgumentException.class)
            .withMessage(message);
        assertNothingSent();
    }

    @ParameterizedTest
    @EnumSource(SprocketsProtocol.class)
    void dc13_errorWithAnAbsoluteErrorType_throwsAModeledDocumentException(SprocketsProtocol protocol) {
        DynamicClient client = client(SprocketsModels.forProtocol(protocol));
        respond(protocol.error("smithy.example#InvalidSprocketId", "id", "1"));

        assertModeledError(() -> client.call("GetSprocket", map("id", "1")), "InvalidSprocketId", "id", "1");
        assertModeledError(() -> joinCause(client.callAsync("GetSprocket", map("id", "1"))), "InvalidSprocketId",
                           "id", "1");
    }

    /**
     * DC-13 with each protocol's Smithy protocol test of an operation error, {@code InvalidGreeting}: its status,
     * headers and body, and its {@code params} as the expected contents. The error type is absolute in the awsJson
     * and rpcv2Cbor cases and relative in the others.
     */
    @ParameterizedTest
    @EnumSource(SprocketsProtocol.class)
    void dc13_protocolTestInvalidGreetingResponse_throwsADocumentExceptionWithTheTestParams(SprocketsProtocol protocol) {
        ShapeId service = ProtocolTestModels.service(protocol);
        ShapeId error = ShapeId.fromParts(service.getNamespace(), "InvalidGreeting");
        ObjectNode test = ProtocolTestModels.responseTest(error, ProtocolTestModels.invalidGreetingTest(protocol));
        DynamicClient client = track(CallTestClients.builder(ProtocolTestModels.model(), sync, async)
                                                    .serviceId(service)
                                                    .endpointOverride(CallTestClients.ENDPOINT)
                                                    .build());
        respond(responseOf(test));
        Document expected = client.createStruct(error, ModelConversions.document(test.expectObjectMember("params")));

        assertThatThrownBy(() -> client.call("GreetingWithErrors"))
            .isInstanceOfSatisfying(DocumentException.class, e -> {
                assertThat(e.sdkSchema().id()).isEqualTo(ModelConversions.shapeId(error));
                assertThat(e.getContents()).isEqualTo(expected);
                assertThat(e.statusCode()).isEqualTo(test.expectNumberMember("code").getValue().intValue());
            });
    }

    @ParameterizedTest
    @EnumSource(SprocketsProtocol.class)
    void dc14_errorWithARelativeErrorType_resolvesItAgainstTheServiceNamespace(SprocketsProtocol protocol) {
        DynamicClient client = client(SprocketsModels.forProtocol(protocol));
        respond(protocol.error("InvalidSprocketId", "id", "1"));

        assertModeledError(() -> client.call("GetSprocket", map("id", "1")), "InvalidSprocketId", "id", "1");
    }

    @ParameterizedTest
    @EnumSource(SprocketsProtocol.class)
    void dc15_serviceLevelErrorWithARelativeErrorType_throwsADocumentException(SprocketsProtocol protocol) {
        DynamicClient client = client(SprocketsModels.forProtocol(protocol));
        respond(protocol.error("ServiceFooError", "why", "IDK"));

        assertModeledError(() -> client.call("GetSprocket", map("id", "1")), "ServiceFooError", "why", "IDK");
    }

    @ParameterizedTest
    @EnumSource(SprocketsProtocol.class)
    void dc16_serviceWithoutModeledAuth_resolvesToNoAuth(SprocketsProtocol protocol) {
        AuthRecorder auth = new AuthRecorder();
        DynamicClient client = client(SprocketsModels.forProtocol(protocol), auth);
        respond(protocol.output("GetSprocket", "id", "1"));

        client.call("GetSprocket", map("id", "1"));

        for (String operation : Arrays.asList("CreateSprocket", "GetSprocket", "TagSprocket", "UploadSprocket",
                                              "DownloadSprocket", "WatchSprockets", "SendSprocketCommands",
                                              "SyncSprockets")) {
            assertThat(client.getOperation(operation).effectiveAuthSchemes())
                .containsExactly(sdkId("smithy.api#noAuth"));
        }
        assertThat(auth.selected).containsExactly("smithy.api#noAuth");
        assertThat(sync.lastRequest().request.firstMatchingHeader("Authorization")).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(SprocketsProtocol.class)
    void dc17_binaryValueForANonStreamingBlobMember_isWrittenInTheProtocolsBlobEncoding(SprocketsProtocol protocol) {
        List<ShapeType> seen = new CopyOnWriteArrayList<>();
        ExecutionInterceptor inspector = new ExecutionInterceptor() {
            @Override
            public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
                seen.add(((DynamicRequest) context.request()).document().getMember("checksum").type());
            }
        };
        DynamicClient client = client(blobModel(protocol), inspector);
        respond(protocol.output("GetSprocket", "id", "1"));

        client.call("GetSprocket", map("id", "1", "checksum", BLOB));

        assertThat(seen).containsExactly(ShapeType.BLOB);
        assertThat(protocol.requestMembers(sync.lastRequest()))
            .containsEntry(protocol.wireName("checksum"), protocol.wireBlob(BLOB))
            .containsEntry(protocol.wireName("id"), "1");
    }

    @ParameterizedTest
    @EnumSource(SprocketsProtocol.class)
    void dc18_stringForABlobMember_failsInputCoercion(SprocketsProtocol protocol) {
        HookCounter hooks = new HookCounter();
        DynamicClient client = client(blobModel(protocol), hooks);
        Document input = map("id", "1", "checksum", Base64.getEncoder().encodeToString(BLOB));
        String message = "Cannot coerce member smithy.example#GetSprocketInput$checksum: a blob member needs a blob "
                         + "document, but found a string document. Pass the bytes with Document.of(byte[]) or "
                         + "Document.of(ByteBuffer).";

        assertThatThrownBy(() -> client.call("GetSprocket", input))
            .isInstanceOf(SerializationException.class)
            .hasMessage(message);
        assertThat(client.callAsync("GetSprocket", input))
            .failsWithin(Duration.ofSeconds(5))
            .withThrowableOfType(ExecutionException.class)
            .havingCause()
            .isInstanceOf(SerializationException.class)
            .withMessage(message);
        assertThat(hooks.beforeExecution).hasValue(0);
        assertNothingSent();
    }

    @ParameterizedTest
    @EnumSource(SprocketsProtocol.class)
    void dc19_operationAuthDifferentFromTheServiceDefault_isExposedAndUsedInOrder(SprocketsProtocol protocol) {
        Model model = SprocketsModels.builder()
                                     .protocols(protocol)
                                     .given("apply Sprockets @aws.auth#sigv4(name: \"sprockets\")",
                                            "apply Sprockets @httpBearerAuth",
                                            "apply Sprockets @auth([aws.auth#sigv4, httpBearerAuth])",
                                            "apply GetSprocket @auth([httpBearerAuth, aws.auth#sigv4])")
                                     .build();
        AuthRecorder auth = new AuthRecorder();
        DynamicClient client = track(CallTestClients.builder(model, sync, async, auth)
                                                    .endpointOverride(CallTestClients.ENDPOINT)
                                                    .putAuthScheme(new StaticBearerAuthScheme())
                                                    .build());
        respond(protocol.output("GetSprocket", "id", "1"), protocol.output("CreateSprocket", "id", "2"));

        client.call("GetSprocket", map("id", "1"));
        client.call("CreateSprocket");

        assertThat(client.getOperation("GetSprocket").effectiveAuthSchemes())
            .containsExactly(sdkId("smithy.api#httpBearerAuth"), sdkId("aws.auth#sigv4"));
        assertThat(client.getOperation("CreateSprocket").effectiveAuthSchemes())
            .containsExactly(sdkId("aws.auth#sigv4"), sdkId("smithy.api#httpBearerAuth"));
        assertThat(auth.selected).containsExactly("smithy.api#httpBearerAuth", "aws.auth#sigv4");
        assertThat(sync.requests().get(0).request.firstMatchingHeader("Authorization"))
            .hasValue("Bearer " + StaticBearerAuthScheme.TOKEN);
        assertThat(sync.requests().get(1).request.firstMatchingHeader("Authorization").get())
            .startsWith("AWS4-HMAC-SHA256 Credential=akid/")
            .contains("/us-west-2/sprockets/aws4_request");
    }

    /**
     * Smithy's rule: {@code @optionalAuth} adds no-auth after the operation's schemes. With a sigv4 scheme whose
     * identity is unavailable, the call falls back to that no-auth option; an operation without {@code optionalAuth}
     * has no option left.
     */
    @ParameterizedTest
    @EnumSource(SprocketsProtocol.class)
    void dc20_optionalAuthOperation_hasNoAuthLastInItsEffectiveAuthSchemes(SprocketsProtocol protocol) {
        Model model = SprocketsModels.builder()
                                     .protocols(protocol)
                                     .given("apply Sprockets @aws.auth#sigv4(name: \"sprockets\")",
                                            "apply GetSprocket @optionalAuth")
                                     .build();
        AuthRecorder auth = new AuthRecorder();
        DynamicClient signing = client(model, auth);
        DynamicClient withoutIdentity = track(CallTestClients.builder(model, sync, async, auth)
                                                             .endpointOverride(CallTestClients.ENDPOINT)
                                                             .putAuthScheme(new SigV4WithoutIdentity())
                                                             .build());
        respond(protocol.output("GetSprocket", "id", "1"));

        signing.call("GetSprocket", map("id", "1"));
        withoutIdentity.call("GetSprocket", map("id", "1"));

        assertThat(signing.getOperation("GetSprocket").effectiveAuthSchemes())
            .containsExactly(sdkId("aws.auth#sigv4"), sdkId("smithy.api#noAuth"));
        assertThat(signing.getOperation("CreateSprocket").effectiveAuthSchemes())
            .containsExactly(sdkId("aws.auth#sigv4"));
        assertThat(auth.selected).containsExactly("aws.auth#sigv4", "smithy.api#noAuth");
        assertThat(sync.requests().get(0).request.firstMatchingHeader("Authorization")).isPresent();
        assertThat(sync.requests().get(1).request.firstMatchingHeader("Authorization")).isEmpty();
        assertThatThrownBy(() -> withoutIdentity.call("CreateSprocket"))
            .isInstanceOf(SdkException.class)
            .hasMessageContaining("'aws.auth#sigv4' does not have an identity provider configured");
        assertThat(sync.requests()).hasSize(2);
    }

    /**
     * restJson1 and restXml send the stream as the HTTP payload: nothing opens the body before the transport. The SDK
     * has no streaming blob binding for the RPC protocols (task 7.19), so they fail while marshalling, before sending.
     */
    @ParameterizedTest
    @EnumSource(SprocketsProtocol.class)
    void dc21_streamingBlobInput_isSentWithoutBufferingIntoADocument(SprocketsProtocol protocol) {
        CountingContentProvider provider = new CountingContentProvider();
        CountingAsyncBody asyncBody = new CountingAsyncBody();
        StreamRecorder recorder = new StreamRecorder(provider, asyncBody);
        DynamicClient client = client(SprocketsModels.forProtocol(protocol), recorder);
        respond(protocol.output("UploadSprocket"));
        Document syncInput = map("data", DynamicStreams.dataStream(
            RequestBody.fromContentProvider(provider, CONTENT.length, "application/octet-stream")));
        Document asyncInput = map("data", DynamicStreams.dataStream(asyncBody));

        if (!protocol.httpBindings()) {
            assertThatThrownBy(() -> client.call("UploadSprocket", syncInput))
                .isInstanceOf(SdkClientException.class)
                .hasMessageStartingWith("Unable to marshall UploadSprocket: Streaming ")
                .hasMessageContaining("input is not supported by " + protocol.traitId());
            assertNothingSent();
            return;
        }
        client.call("UploadSprocket", syncInput);
        client.callAsync("UploadSprocket", asyncInput).join();

        assertThat(recorder.dataTypes).containsExactly(ShapeType.BLOB, ShapeType.BLOB);
        assertThat(recorder.openedBeforeTransmission).containsExactly(0, 0);
        assertThat(sync.lastRequest().bytes).isEqualTo(CONTENT);
        assertThat(sync.lastRequest().request.encodedPath()).isEqualTo("/upload-sprocket");
        assertThat(sync.lastRequest().request.firstMatchingHeader("Content-Length"))
            .hasValue(String.valueOf(CONTENT.length));
        assertThat(async.lastRequest().bytes).isEqualTo(CONTENT);
    }

    @ParameterizedTest
    @EnumSource(SprocketsProtocol.class)
    void dc22_outputEventStream_failsBeforeSendingWhileEventStreamsAreDeferred(SprocketsProtocol protocol) {
        HookCounter hooks = new HookCounter();
        DynamicClient client = client(SprocketsModels.forProtocol(protocol), hooks);

        assertThatThrownBy(() -> client.call("WatchSprockets"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Operation smithy.example#WatchSprockets has the event stream member "
                        + "smithy.example#WatchSprocketsOutput$events, so it can be called only with callAsync. No "
                        + "request was sent.");
        assertEventStreamsUnsupported(client.callAsync("WatchSprockets"), "WatchSprockets",
                                      "WatchSprocketsOutput$events");
        assertThat(hooks.beforeExecution).hasValue(0);
        assertNothingSent();
    }

    /**
     * Each case lists the service's protocol traits in priority order and in reverse; the first compatible protocol in
     * the SDK's priority list is selected either way.
     */
    @ParameterizedTest(name = "{index}: traits {0}")
    @MethodSource("protocolTraitOrders")
    void dc23_serviceWithSeveralAvailableProtocols_selectsTheFirstInPriorityOrder(List<SprocketsProtocol> traits,
                                                                                SprocketsProtocol expected) {
        Model model = SprocketsModels.builder().protocols(traits.toArray(new SprocketsProtocol[0])).build();

        DynamicClient client = client(model);

        assertThat(client.syncClientConfiguration().option(SdkClientOption.CLIENT_PROTOCOL))
            .isInstanceOf(expected.protocolClass());
        assertThat(client.asyncClientConfiguration().option(SdkClientOption.CLIENT_PROTOCOL))
            .isInstanceOf(expected.protocolClass());
    }

    @Test
    void dc24_walkFromTheOperationToTheIdMemberTarget_reachesStringAndTerminates() {
        DynamicOperation operation = client(SprocketsModels.reference()).getOperation("GetSprocket");

        Schema input = operation.inputSchema();
        Schema id = input.member("id");
        Set<software.amazon.awssdk.schema.ShapeId> reached = new HashSet<>();
        walk(input, reached);

        assertThat(operation.schema().type()).isEqualTo(ShapeType.OPERATION);
        assertThat(input.id()).isEqualTo(sdkId(NS + "GetSprocketInput"));
        assertThat(id.type()).isEqualTo(ShapeType.STRING);
        assertThat(id.memberTarget().id()).isEqualTo(sdkId("smithy.api#String"));
        assertThat(id.memberTarget().members()).isEmpty();
        assertThat(reached).containsExactlyInAnyOrder(sdkId(NS + "GetSprocketInput"), sdkId("smithy.api#String"));
    }

    @Test
    void dc25_jsonNameMemberTargetingAPatternString_reportsTheTargetTypeAndItsDirectJsonName() {
        Schema id = client(SprocketsModels.reference()).getOperation("TagSprocket").inputSchema().member("id");

        assertThat(id.type()).isEqualTo(ShapeType.STRING);
        assertThat(id.memberTarget().id()).isEqualTo(sdkId(NS + "SprocketId"));
        assertThat(id.getTrait(TraitKey.JSON_NAME_TRAIT).getValue()).isEqualTo("Id");
        assertThat(id.getDirectTrait(TraitKey.JSON_NAME_TRAIT).getValue()).isEqualTo("Id");
    }

    @Disabled("@pattern is a constraint trait, which the Trait_Inclusion_Policy excludes from runtime schemas "
              + "(Requirement 25.3, task 13.1), while DC-25 expects it in the member's effective traits. Needs a design "
              + "decision; see porting-deviations.md, task 13.16")
    @Test
    void dc25_jsonNameMemberTargetingAPatternString_exposesTheTargetPatternInItsEffectiveTraits() {
        Schema id = client(SprocketsModels.reference()).getOperation("TagSprocket").inputSchema().member("id");

        assertThat(id.getGenericTrait(sdkId("smithy.api#pattern")).value()).isEqualTo(Document.of("^[a-z]+$"));
    }

    @Test
    void dc26_twoUntypedCustomTraits_convertAndAreEachRetrievableByShapeId() {
        String customTraits = "$version: \"2\"\nnamespace com.example\n"
                              + "@trait\nstructure tagging { team: String }\n"
                              + "@trait\nstring owner\n";
        String[] applied = {"apply GetSprocket @com.example#tagging(team: \"sprockets\")",
                            "apply GetSprocket @com.example#owner(\"platform\")"};
        Model notIncluded = SprocketsModels.builder().model(customTraits).given(applied).build();
        Model included = SprocketsModels.builder()
                                        .model(customTraits)
                                        .given(applied)
                                        .given("@trait(selector: \"service\")",
                                               "@protocolDefinition(traits: [com.example#tagging, com.example#owner])",
                                               "structure sprocketsTraits {}",
                                               "apply Sprockets @sprocketsTraits")
                                        .build();

        Schema operation = client(included).getOperation("GetSprocket").schema();
        Schema withDefaults = client(notIncluded).getOperation("GetSprocket").schema();

        assertThat(operation.getGenericTrait(sdkId("com.example#tagging")).value())
            .isEqualTo(Document.of(Collections.singletonMap("team", Document.of("sprockets"))));
        assertThat(operation.getGenericTrait(sdkId("com.example#owner")).value()).isEqualTo(Document.of("platform"));
        assertThat(withDefaults.getGenericTrait(sdkId("com.example#tagging"))).isNull();
        assertThat(withDefaults.getGenericTrait(sdkId("com.example#owner"))).isNull();
    }

    /**
     * restJson1 and restXml hand the HTTP payload to the transformer, and the output document has no {@code data}
     * member. The SDK has no streaming blob binding for the RPC protocols (task 7.19): they reject the response while
     * unmarshalling, after the request was sent, and the transformer gets no body.
     */
    @ParameterizedTest
    @EnumSource(SprocketsProtocol.class)
    void dc27_streamingBlobOutput_isDeliveredWithoutBufferingIntoADocument(SprocketsProtocol protocol) {
        DynamicClient client = client(SprocketsModels.forProtocol(protocol));
        respond(RecordingHttpClients.Stub.of(200, CONTENT, "Content-Type", "application/octet-stream"));

        if (!protocol.httpBindings()) {
            String rejection = "output is not supported by " + protocol.traitId();
            assertThatThrownBy(() -> client.call("DownloadSprocket", map("id", "1"), ResponseTransformer.toBytes()))
                .isInstanceOf(SdkClientException.class)
                .hasMessageStartingWith("Unable to unmarshall DownloadSprocket: Streaming ")
                .hasMessageContaining(rejection);
            assertThat(client.callAsync("DownloadSprocket", map("id", "1"), AsyncResponseTransformer.toBytes()))
                .failsWithin(Duration.ofSeconds(5))
                .withThrowableOfType(ExecutionException.class)
                .havingCause()
                .isInstanceOf(SdkClientException.class)
                .withMessageContaining(rejection);
            assertThat(sync.requests()).hasSize(1);
            assertThat(async.requests()).hasSize(1);
            return;
        }
        ResponseBytes<Document> syncResult = client.call("DownloadSprocket", map("id", "1"),
                                                         ResponseTransformer.toBytes());
        ResponseBytes<Document> asyncResult = client.callAsync("DownloadSprocket", map("id", "1"),
                                                               AsyncResponseTransformer.toBytes()).join();

        assertThat(syncResult.asByteArray()).isEqualTo(CONTENT);
        assertThat(syncResult.response().getMember("data")).isNull();
        assertThat(asyncResult.asByteArray()).isEqualTo(CONTENT);
        assertThat(asyncResult.response().getMember("data")).isNull();
    }

    @ParameterizedTest
    @EnumSource(SprocketsProtocol.class)
    void dc28_inputEventStream_failsBeforeSendingWhileEventStreamsAreDeferred(SprocketsProtocol protocol) {
        HookCounter hooks = new HookCounter();
        CountingPublisher commands = new CountingPublisher();
        DynamicClient client = client(SprocketsModels.forProtocol(protocol), hooks);
        Document input = map("commands", DynamicStreams.eventStream(commands));

        assertThatThrownBy(() -> client.call("SendSprocketCommands", input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("so it can be called only with callAsync");
        assertEventStreamsUnsupported(client.callAsync("SendSprocketCommands", input), "SendSprocketCommands",
                                      "SendSprocketCommandsInput$commands");
        assertThat(commands.subscriptions).hasValue(0);
        assertThat(hooks.beforeExecution).hasValue(0);
        assertNothingSent();
    }

    @ParameterizedTest
    @EnumSource(SprocketsProtocol.class)
    void dc29_bidirectionalEventStream_failsBeforeSendingWhileEventStreamsAreDeferred(SprocketsProtocol protocol) {
        HookCounter hooks = new HookCounter();
        CountingPublisher commands = new CountingPublisher();
        DynamicClient client = client(SprocketsModels.forProtocol(protocol), hooks);
        Document input = map("commands", DynamicStreams.eventStream(commands));

        assertThatThrownBy(() -> client.call("SyncSprockets", input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("so it can be called only with callAsync");
        assertEventStreamsUnsupported(client.callAsync("SyncSprockets", input), "SyncSprockets",
                                      "SyncSprocketsInput$commands");
        assertThat(commands.subscriptions).hasValue(0);
        assertThat(hooks.beforeExecution).hasValue(0);
        assertNothingSent();
    }

    @ParameterizedTest
    @EnumSource(SprocketsProtocol.class)
    void dc30_pluginWithAnAuxiliaryOperationCallback_callsItOverTheDynamicClient(SprocketsProtocol protocol) {
        AtomicReference<DynamicClient> dynamicClient = new AtomicReference<>();
        SprocketProvisioningPlugin plugin =
            new SprocketProvisioningPlugin(input -> dynamicClient.get().call("CreateSprocket", input));
        DynamicClient client = track(CallTestClients.builder(SprocketsModels.forProtocol(protocol), sync, async)
                                                    .endpointOverride(CallTestClients.ENDPOINT)
                                                    .addPlugin(plugin)
                                                    .build());
        dynamicClient.set(client);
        respond(protocol.output("CreateSprocket", "id", "7"), protocol.output("GetSprocket", "id", "7"));

        Document output = client.call("GetSprocket", map("id", "7"));

        assertThat(output.getMember("id").asString()).isEqualTo("7");
        assertThat(plugin.operationsSeen).containsExactly("GetSprocket", "CreateSprocket");
        assertThat(sync.requests()).hasSize(2);
        assertThat(sync.requests().get(0).request.firstMatchingHeader(SprocketProvisioningPlugin.HEADER)).isEmpty();
        assertThat(sync.requests().get(1).request.firstMatchingHeader(SprocketProvisioningPlugin.HEADER)).hasValue("7");
        assertThatThrownBy(() -> new SprocketProvisioningPlugin(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("createSprocket");
    }

    static Stream<Arguments> protocolTraitOrders() {
        List<SprocketsProtocol> priority = Arrays.asList(SprocketsProtocol.values());
        List<Arguments> cases = new ArrayList<>();
        for (int i = 0; i < priority.size(); i++) {
            List<SprocketsProtocol> inPriorityOrder = priority.subList(i, priority.size());
            List<SprocketsProtocol> reversed = new ArrayList<>(inPriorityOrder);
            Collections.reverse(reversed);
            cases.add(Arguments.of(inPriorityOrder, priority.get(i)));
            if (reversed.size() > 1) {
                cases.add(Arguments.of(reversed, priority.get(i)));
            }
        }
        return cases.stream();
    }

    private DynamicClient client(Model model, ExecutionInterceptor... interceptors) {
        return track(CallTestClients.client(model, sync, async, interceptors));
    }

    private DynamicClient.Builder clientBuilder(Model model) {
        return CallTestClients.builder(model, sync, async);
    }

    private DynamicClient track(DynamicClient client) {
        clients.add(client);
        return client;
    }

    private void respond(RecordingHttpClients.Stub... stubs) {
        sync.stub(stubs);
        async.stub(stubs);
    }

    private void assertNothingSent() {
        assertThat(sync.requests()).isEmpty();
        assertThat(async.requests()).isEmpty();
    }

    private static Model blobModel(SprocketsProtocol protocol) {
        return BLOB_MODELS.computeIfAbsent(protocol, p -> SprocketsModels.builder()
                                                                         .protocols(p)
                                                                         .edit("operation GetSprocket {\n"
                                                                               + "    input := { id: String }",
                                                                               "operation GetSprocket {\n"
                                                                               + "    input := { id: String, "
                                                                               + "checksum: Blob }")
                                                                         .build());
    }

    private static String rpcPath(SprocketsProtocol protocol) {
        return protocol == SprocketsProtocol.RPCV2_CBOR ? "/service/Sprockets/operation/GetSprocket" : "/";
    }

    private static software.amazon.awssdk.schema.ShapeId sdkId(String id) {
        return software.amazon.awssdk.schema.ShapeId.from(id);
    }

    private static void walk(Schema schema, Set<software.amazon.awssdk.schema.ShapeId> reached) {
        if (!reached.add(schema.id())) {
            return;
        }
        for (Schema member : schema.members()) {
            walk(member.memberTarget(), reached);
        }
    }

    private static void assertModeledError(Runnable call, String errorName, String member, String value) {
        assertThatThrownBy(call::run)
            .isInstanceOfSatisfying(DocumentException.class, e -> {
                assertThat(e.sdkSchema().id()).isEqualTo(sdkId(NS + errorName));
                assertThat(e.sdkSchema().id().getName()).isEqualTo(errorName);
                assertThat(e.getContents().getDiscriminator()).isEqualTo(sdkId(NS + errorName));
                assertThat(e.getContents().getMember(member).asString()).isEqualTo(value);
                assertThat(e.statusCode()).isEqualTo(400);
            });
    }

    private static void joinCause(CompletableFuture<?> future) {
        try {
            future.get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new IllegalStateException(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static void assertEventStreamsUnsupported(CompletableFuture<?> call, String operation, String member) {
        assertThat(call)
            .failsWithin(Duration.ofSeconds(5))
            .withThrowableOfType(ExecutionException.class)
            .havingCause()
            .isInstanceOf(UnsupportedOperationException.class)
            .withMessage("The dynamic client does not support event streams yet: operation " + NS + operation
                         + " has the event stream member " + NS + member + ". No request was sent.");
    }

    /**
     * A stub response from a protocol test case: its status code, headers and body (base64 for CBOR).
     */
    private static RecordingHttpClients.Stub responseOf(ObjectNode test) {
        List<String> headers = new ArrayList<>();
        test.getObjectMember("headers").ifPresent(h -> h.getStringMap().forEach((name, value) -> {
            headers.add(name);
            headers.add(value.expectStringNode().getValue());
        }));
        String body = test.getStringMemberOrDefault("body", "");
        boolean cbor = "application/cbor".equals(test.getStringMemberOrDefault("bodyMediaType", ""));
        byte[] bytes = cbor ? Base64.getDecoder().decode(body) : body.getBytes(StandardCharsets.UTF_8);
        return RecordingHttpClients.Stub.of(test.expectNumberMember("code").getValue().intValue(), bytes,
                                            headers.toArray(new String[0]));
    }

    private static final class HookCounter implements ExecutionInterceptor {
        private final AtomicInteger beforeExecution = new AtomicInteger();

        @Override
        public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
            beforeExecution.incrementAndGet();
        }
    }

    /**
     * Records the scheme ID of each call's selected auth scheme.
     */
    private static final class AuthRecorder implements ExecutionInterceptor {
        private final List<String> selected = new CopyOnWriteArrayList<>();

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            selected.add(executionAttributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME)
                                            .authSchemeOption().schemeId());
        }
    }

    /**
     * Records the type of the {@code data} member interceptors see, and how often the request body had been opened
     * when the request reached the transport.
     */
    private static final class StreamRecorder implements ExecutionInterceptor {
        private final List<ShapeType> dataTypes = new CopyOnWriteArrayList<>();
        private final List<Integer> openedBeforeTransmission = new CopyOnWriteArrayList<>();
        private final CountingContentProvider provider;
        private final CountingAsyncBody asyncBody;

        private StreamRecorder(CountingContentProvider provider, CountingAsyncBody asyncBody) {
            this.provider = provider;
            this.asyncBody = asyncBody;
        }

        @Override
        public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
            dataTypes.add(((DynamicRequest) context.request()).document().getMember("data").type());
        }

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            openedBeforeTransmission.add(context.requestBody().isPresent() ? provider.opened.get()
                                                                           : asyncBody.subscriptions.get());
        }
    }

    private static final class CountingContentProvider implements ContentStreamProvider {
        private final AtomicInteger opened = new AtomicInteger();

        @Override
        public InputStream newStream() {
            opened.incrementAndGet();
            return new ByteArrayInputStream(CONTENT);
        }
    }

    private static final class CountingAsyncBody implements AsyncRequestBody {
        private final AsyncRequestBody delegate = AsyncRequestBody.fromBytes(CONTENT);
        private final AtomicInteger subscriptions = new AtomicInteger();

        @Override
        public Optional<Long> contentLength() {
            return delegate.contentLength();
        }

        @Override
        public String contentType() {
            return "application/octet-stream";
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            subscriptions.incrementAndGet();
            delegate.subscribe(subscriber);
        }
    }

    /**
     * A publisher of events that counts subscriptions: event stream calls fail before subscribing.
     */
    private static final class CountingPublisher implements Publisher<Document> {
        private final AtomicInteger subscriptions = new AtomicInteger();

        @Override
        public void subscribe(Subscriber<? super Document> subscriber) {
            subscriptions.incrementAndGet();
            throw new UnsupportedOperationException("Event streams are deferred");
        }
    }

    /**
     * A transport of SDK HTTP messages, set by a plugin, that records requests and answers with a stub.
     */
    private static final class RecordingTransport {
        private final List<SdkHttpFullRequest> requests = new CopyOnWriteArrayList<>();
        private final ClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> sync;
        private final AsyncClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> async;

        private RecordingTransport(java.util.function.Supplier<RecordingHttpClients.Stub> response) {
            this.sync = new ClientTransport<SdkHttpFullRequest, SdkHttpFullResponse>() {
                @Override
                public MessageExchange<SdkHttpFullRequest, SdkHttpFullResponse> messageExchange() {
                    return HttpMessageExchange.INSTANCE;
                }

                @Override
                public SdkHttpFullResponse send(ExecutionAttributes context, SdkHttpFullRequest request) {
                    requests.add(request);
                    return response.get().fullResponse();
                }

                @Override
                public void close() {
                }
            };
            this.async = new AsyncClientTransport<SdkHttpFullRequest, SdkHttpFullResponse>() {
                @Override
                public MessageExchange<SdkHttpFullRequest, SdkHttpFullResponse> messageExchange() {
                    return HttpMessageExchange.INSTANCE;
                }

                @Override
                public CompletableFuture<SdkHttpFullResponse> send(
                    ExecutionAttributes context, SdkHttpFullRequest request,
                    AsyncResponseHandoff<SdkHttpFullResponse> streamingHandoff) {
                    requests.add(request);
                    return CompletableFuture.completedFuture(response.get().fullResponse());
                }

                @Override
                public void close() {
                }
            };
        }
    }

    /**
     * {@code smithy.api#httpBearerAuth} with a fixed token: the dynamic client builder has no token provider option.
     */
    private static final class StaticBearerAuthScheme implements AuthScheme<TokenIdentity> {
        private static final String TOKEN = "sprocket-token";

        @Override
        public String schemeId() {
            return "smithy.api#httpBearerAuth";
        }

        @Override
        public IdentityProvider<TokenIdentity> identityProvider(IdentityProviders providers) {
            return new IdentityProvider<TokenIdentity>() {
                @Override
                public Class<TokenIdentity> identityType() {
                    return TokenIdentity.class;
                }

                @Override
                public CompletableFuture<TokenIdentity> resolveIdentity(ResolveIdentityRequest request) {
                    return CompletableFuture.completedFuture(TokenIdentity.create(TOKEN));
                }
            };
        }

        @Override
        public HttpSigner<TokenIdentity> signer() {
            return BearerHttpSigner.create();
        }
    }

    /**
     * {@code aws.auth#sigv4} without an identity provider, so the SDK discards its option.
     */
    private static final class SigV4WithoutIdentity implements AuthScheme<AwsCredentialsIdentity> {
        @Override
        public String schemeId() {
            return "aws.auth#sigv4";
        }

        @Override
        public IdentityProvider<AwsCredentialsIdentity> identityProvider(IdentityProviders providers) {
            return null;
        }

        @Override
        public HttpSigner<AwsCredentialsIdentity> signer() {
            return AwsV4HttpSigner.create();
        }
    }

    /**
     * The SEP's auxiliary-operation pattern: a plugin that needs {@code CreateSprocket}, which the application never
     * calls, takes it as a required callback and checks it when the plugin is constructed. Before each
     * {@code GetSprocket} call, it creates a sprocket through the callback and sends its ID in a header.
     */
    private static final class SprocketProvisioningPlugin implements SdkPlugin {
        private static final String HEADER = "x-amz-provisioned-sprocket";
        private final Function<Document, Document> createSprocket;
        private final List<String> operationsSeen = new CopyOnWriteArrayList<>();

        private SprocketProvisioningPlugin(Function<Document, Document> createSprocket) {
            this.createSprocket = Validate.paramNotNull(createSprocket, "createSprocket");
        }

        @Override
        public void configureClient(SdkServiceClientConfiguration.Builder config) {
            ClientOverrideConfiguration override = config.overrideConfiguration();
            config.overrideConfiguration(override.toBuilder().addExecutionInterceptor(new Provisioner()).build());
        }

        private final class Provisioner implements ExecutionInterceptor {
            @Override
            public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context,
                                                    ExecutionAttributes executionAttributes) {
                String operation = executionAttributes.getAttribute(SdkExecutionAttribute.OPERATION_NAME);
                operationsSeen.add(operation);
                if (!"GetSprocket".equals(operation)) {
                    return context.httpRequest();
                }
                Document created = createSprocket.apply(Document.EMPTY_MAP);
                return context.httpRequest().toBuilder().putHeader(HEADER, created.getMember("id").asString()).build();
            }
        }
    }
}
