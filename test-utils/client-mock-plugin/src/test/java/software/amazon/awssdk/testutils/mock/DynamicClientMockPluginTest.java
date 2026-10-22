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

package software.amazon.awssdk.testutils.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.dynamicclient.DocumentException;
import software.amazon.awssdk.dynamicclient.DynamicClient;
import software.amazon.awssdk.dynamicclient.DynamicOperation;
import software.amazon.awssdk.dynamicclient.DynamicRequest;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.AwsJson1_0Trait;
import software.amazon.awssdk.schema.traits.Rpcv2CborTrait;
import software.amazon.awssdk.services.account.AccountAsyncClient;
import software.amazon.awssdk.services.account.AccountClient;
import software.amazon.awssdk.services.account.model.AlternateContact;
import software.amazon.awssdk.services.account.model.AlternateContactType;
import software.amazon.awssdk.services.account.model.GetAlternateContactResponse;
import software.amazon.awssdk.services.account.model.ResourceNotFoundException;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.StringInputStream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The Mock_Plugin on the Dynamic_Client (Requirement 35.9): queued schema-backed documents, modeled errors and raw
 * responses, through awsJson1_0, restJson1, rpcv2Cbor and restXml, and the same queued items on the generated and the
 * dynamic Account clients.
 */
class DynamicClientMockPluginTest {
    private static final StaticCredentialsProvider CREDENTIALS =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));
    private static final URI ENDPOINT = URI.create("https://things.example.com");
    private static final String NAMESPACE = "test.mock.things#";
    private static final ShapeId GET_THING_OUTPUT = ShapeId.from(NAMESPACE + "GetThingOutput");
    private static final ShapeId PUT_THING_OUTPUT = ShapeId.from(NAMESPACE + "PutThingOutput");
    private static final ShapeId THING_NOT_FOUND = ShapeId.from(NAMESPACE + "ThingNotFound");
    private static final Model THINGS = assemble("dynamic-things.smithy");

    private final FailingHttpClient httpClient = new FailingHttpClient();
    private final FailingAsyncHttpClient asyncHttpClient = new FailingAsyncHttpClient();

    @ParameterizedTest
    @ValueSource(strings = {"JsonThings", "RestJsonThings", "CborThings", "XmlThings"})
    void call_whenDocumentsQueued_returnsThemFirstInFirstOut(String service) {
        MockQueue queue = new MockQueue();
        try (DynamicClient client = things(service, MockPlugin.builder().addQueue(queue).build())) {
            Document first = thing(client, "first", 1);
            Document second = thing(client, "second", 2);
            queue.enqueueDocument(first).enqueueDocument(second);

            Document firstOutput = client.call("GetThing", map("id", "a"));
            Document secondOutput = client.call("GetThing", map("id", "b"));

            assertThat(firstOutput).isEqualTo(first);
            assertThat(firstOutput.getDiscriminator()).hasToString(GET_THING_OUTPUT.toString());
            assertThat(firstOutput.getMember("version").asString()).isEqualTo("v-first");
            assertThat(secondOutput).isEqualTo(second);
        }
        assertThat(queue.remaining()).isZero();
        assertThat(httpClient.calls()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"JsonThings", "RestJsonThings", "CborThings", "XmlThings"})
    void callAsync_whenDocumentQueued_completesWithIt(String service) throws Exception {
        MockQueue queue = new MockQueue();
        try (DynamicClient client = things(service, MockPlugin.builder().addQueue(queue).build())) {
            Document queued = thing(client, "async", 3);
            queue.enqueueDocument(queued);

            Document output = client.callAsync("GetThing", map("id", "a")).get(10, TimeUnit.SECONDS);

            assertThat(output).isEqualTo(queued);
        }
        assertThat(asyncHttpClient.calls()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"JsonThings", "RestJsonThings", "CborThings", "XmlThings"})
    void call_whenDocumentExceptionQueued_throwsADocumentExceptionWithEqualContents(String service) {
        MockQueue queue = new MockQueue();
        try (DynamicClient client = things(service, MockPlugin.builder().addQueue(queue).build())) {
            DocumentException queued = thingNotFound(client, "r-1");
            queue.enqueueError(queued).enqueueError(thingNotFound(client, "r-2"));

            Throwable thrown = catchThrowable(() -> client.call("GetThing", map("id", "a")));
            ExecutionException asyncFailure = thrownAs(
                () -> client.callAsync("GetThing", map("id", "b")).get(10, TimeUnit.SECONDS), ExecutionException.class);

            assertThat(thrown).isExactlyInstanceOf(DocumentException.class);
            DocumentException error = (DocumentException) thrown;
            assertThat(error.getContents()).isEqualTo(queued.getContents());
            assertThat(error.getContents().getDiscriminator()).hasToString(THING_NOT_FOUND.toString());
            assertThat(error.sdkSchema()).isSameAs(queued.sdkSchema());
            assertThat(error.statusCode()).isEqualTo(404);
            assertThat(error.requestId()).isEqualTo("r-1");
            assertThat(error.awsErrorDetails().errorMessage()).isEqualTo("no such thing");
            assertThat(asyncFailure.getCause()).isExactlyInstanceOf(DocumentException.class);
            assertThat(((DocumentException) asyncFailure.getCause()).getContents()).isEqualTo(queued.getContents());
        }
        assertThat(queue.remaining()).isZero();
        assertThat(httpClient.calls() + asyncHttpClient.calls()).isZero();
    }

    @Test
    void call_whenSyncAndAsyncCallsShareAQueue_consumeItInCallOrder() throws Exception {
        MockQueue queue = new MockQueue();
        try (DynamicClient client = things("RestJsonThings", MockPlugin.builder().addQueue(queue).build())) {
            Document first = thing(client, "first", 1);
            Document second = thing(client, "second", 2);
            Document third = thing(client, "third", 3);
            queue.enqueueDocument(first).enqueueDocument(second).enqueueDocument(third);

            assertThat(client.callAsync("GetThing", map("id", "a")).get(10, TimeUnit.SECONDS)).isEqualTo(first);
            assertThat(client.call("GetThing", map("id", "b"))).isEqualTo(second);
            assertThat(client.callAsync("GetThing", map("id", "c")).get(10, TimeUnit.SECONDS)).isEqualTo(third);
            assertThat(queue.remaining()).isZero();
        }
    }

    @Test
    void call_whenRawResponseQueued_deserializesItUnchanged() {
        String body = "{\"name\":\"raw\",\"count\":7}";
        SdkHttpFullResponse raw = SdkHttpFullResponse.builder()
                                                     .statusCode(200)
                                                     .putHeader("Content-Type", "application/json")
                                                     .putHeader("X-Thing-Version", "raw-version")
                                                     .content(AbortableInputStream.create(new StringInputStream(body)))
                                                     .build();
        MockQueue queue = new MockQueue().enqueue(raw);

        try (DynamicClient client = things("RestJsonThings", MockPlugin.builder().addQueue(queue).build())) {
            Document output = client.call("GetThing", map("id", "a"));

            assertThat(output).isEqualTo(client.createStruct(GET_THING_OUTPUT, map("name", "raw", "count", 7,
                                                                                   "version", "raw-version")));
        }
        assertThat(httpClient.calls()).isZero();
    }

    @Test
    void call_whenAnotherOperationsOutputQueued_throwsNamingBothIdsAndConsumesIt() {
        MockQueue queue = new MockQueue();
        try (DynamicClient client = things("JsonThings", MockPlugin.builder().addQueue(queue).build())) {
            queue.enqueueDocument(client.createStruct(PUT_THING_OUTPUT, map("id", "a")));

            assertThatThrownBy(() -> client.call("GetThing", map("id", "a")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Expected one of [test.mock.things#GetThingOutput, test.mock.things#ThingNotFound, "
                            + "test.mock.things#AccessDenied] but queued test.mock.things#PutThingOutput");
        }
        assertThat(queue.remaining()).isZero();
    }

    @Test
    void call_whenQueueExhausted_failsSyncAndAsyncWithoutCallingTheHttpClients() {
        MockPlugin mock = MockPlugin.builder().addQueue(new MockQueue()).build();

        try (DynamicClient client = things("JsonThings", mock)) {
            assertThatThrownBy(() -> client.call("GetThing", map("id", "a")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Mock queue exhausted: no queued result or matcher for the call to "
                            + "test.mock.things#GetThing");
            CompletableFuture<Document> future = client.callAsync("GetThing", map("id", "b"));

            assertThat(catchThrowable(() -> future.get(10, TimeUnit.SECONDS)))
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Mock queue exhausted: no queued result or matcher for the call to "
                                     + "test.mock.things#GetThing");
        }
        assertThat(mock.getRequests()).hasSize(2);
        assertThat(httpClient.calls() + asyncHttpClient.calls()).isZero();
    }

    @Test
    void generateOutput_whenStructDocumentGiven_writesTheStructureWithoutADiscriminator() throws Exception {
        try (DynamicClient client = things("JsonThings", MockPlugin.builder().build())) {
            SerializableStruct output = (SerializableStruct) client.createStruct(PUT_THING_OUTPUT, map("id", "a"));
            DynamicOperation operation = client.getOperation("PutThing");

            SdkHttpFullResponse json = ResponseGenerator.forProtocol(AwsJson1_0Trait.ID).generateOutput(operation, output);
            SdkHttpFullResponse cbor = ResponseGenerator.forProtocol(Rpcv2CborTrait.ID).generateOutput(operation, output);

            // StructDocument.serialize writes a document, which the codecs write with a __type member.
            assertThat(IoUtils.toUtf8String(json.content().get())).isEqualTo("{\"id\":\"a\"}");
            assertThat(new String(IoUtils.toByteArray(cbor.content().get()), StandardCharsets.ISO_8859_1))
                .contains("id").doesNotContain("__type");
        }
    }

    @Test
    void enqueueDocument_whenDocumentHasNoSchema_throws() {
        assertThatThrownBy(() -> new MockQueue().enqueueDocument(map("name", "x")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageStartingWith("Only a schema-backed document can be enqueued, but the map document has no schema");
    }

    @Test
    void build_withMockPlugin_runsItOncePerConfigurationAndRecordsEachCallOnce() throws Exception {
        MockQueue queue = new MockQueue();
        MockPlugin mock = MockPlugin.builder().addQueue(queue).build();
        AtomicInteger runs = new AtomicInteger();
        SdkPlugin counting = config -> {
            runs.incrementAndGet();
            mock.configureClient(config);
        };

        try (DynamicClient client = things("JsonThings", counting)) {
            assertThat(runs).hasValue(2);
            queue.enqueueDocument(thing(client, "sync", 1)).enqueueDocument(thing(client, "async", 2));

            client.call("GetThing", map("id", "a"));
            client.callAsync("GetThing", map("id", "b")).get(10, TimeUnit.SECONDS);
        }

        List<MockedRequest> requests = mock.getRequests();
        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).input()).isInstanceOf(DynamicRequest.class);
        assertThat(((DynamicRequest) requests.get(0).input()).document().getMember("id").asString()).isEqualTo("a");
        assertThat(((DynamicRequest) requests.get(1).input()).document().getMember("id").asString()).isEqualTo("b");
        assertThat(requests.get(0).request().firstMatchingHeader("X-Amz-Target")).hasValue("JsonThings.GetThing");
        assertThat(requests.get(0).request().firstMatchingHeader("Authorization")).isPresent();
    }

    @Test
    void call_withAnInterceptor_seesTheSignedRequestAndTheMockedResponse() {
        RecordingInterceptor interceptor = new RecordingInterceptor();
        MockQueue queue = new MockQueue();
        try (DynamicClient client = DynamicClient.builder()
                                                 .model(THINGS)
                                                 .serviceId(ShapeId.from(NAMESPACE + "RestJsonThings"))
                                                 .region(Region.US_WEST_2)
                                                 .credentialsProvider(CREDENTIALS)
                                                 .endpointOverride(ENDPOINT)
                                                 .httpClient(httpClient)
                                                 .httpClient(asyncHttpClient)
                                                 .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
                                                 .addPlugin(MockPlugin.builder().addQueue(queue).build())
                                                 .build()) {
            queue.enqueueDocument(thing(client, "seen", 5));

            client.call("GetThing", map("id", "t 1"));
        }

        assertThat(interceptor.requests).singleElement().satisfies(r -> {
            assertThat(r.method()).isEqualTo(SdkHttpMethod.GET);
            assertThat(r.encodedPath()).isEqualTo("/things/t%201");
            assertThat(r.firstMatchingHeader("Authorization")).hasValueSatisfying(a -> assertThat(a)
                .contains("/us-west-2/restjsonthings/aws4_request"));
        });
        assertThat(interceptor.responseStatuses).containsExactly(200);
        assertThat(interceptor.versionHeaders).containsExactly("v-seen");
    }

    @Test
    void accountClients_whenTheSameOutputsQueued_returnEqualOutputs() {
        GetAlternateContactResponse generated = GetAlternateContactResponse.builder().alternateContact(
            AlternateContact.builder().name("Jane").title("CFO").alternateContactType(AlternateContactType.BILLING)
                            .build()).build();
        Model model = accountModel();
        Document document;
        try (DynamicClient dynamic = account(model, null)) {
            document = dynamic.createStruct(ShapeId.from("com.amazonaws.account#GetAlternateContactResponse"),
                                            Document.of(generated));
        }

        List<Document> outputs = new ArrayList<>();
        for (Object item : Arrays.asList(generated, document)) {
            try (AccountClient sync = accountSync(item);
                 AccountAsyncClient async = accountAsync(item);
                 DynamicClient dynamic = account(model, mockOf(item))) {
                outputs.add(Document.of(sync.getAlternateContact(r -> r.alternateContactType("BILLING"))));
                outputs.add(Document.of(CompletableFutureUtils.joinLikeSync(
                    async.getAlternateContact(r -> r.alternateContactType("BILLING")))));
                outputs.add(dynamic.call("GetAlternateContact", map("AlternateContactType", "BILLING")));
            }
        }

        assertThat(outputs).hasSize(6).allSatisfy(o -> assertThat(o).isEqualTo(Document.of(generated)));
        assertThat(httpClient.calls() + asyncHttpClient.calls()).isZero();
    }

    @Test
    void accountClients_whenTheSameErrorsQueued_throwTheModeledTypeWithEqualMembers() {
        ResourceNotFoundException generated = ResourceNotFoundException.builder().message("missing").build();
        Model model = accountModel();
        DocumentException document;
        try (DynamicClient dynamic = account(model, null)) {
            Schema errorSchema = errorSchema(dynamic, "GetAlternateContact", "ResourceNotFoundException");
            document = dynamic.createStruct(ShapeId.from("com.amazonaws.account#ResourceNotFoundException"),
                                            map("message", "missing"))
                              .asShape(DocumentException.builder(errorSchema));
        }

        for (Object item : Arrays.asList(generated, document)) {
            try (AccountClient sync = accountSync(item);
                 AccountAsyncClient async = accountAsync(item);
                 DynamicClient dynamic = account(model, mockOf(item))) {
                ResourceNotFoundException fromSync = thrownAs(
                    () -> sync.getAlternateContact(r -> r.alternateContactType("BILLING")),
                    ResourceNotFoundException.class);
                ResourceNotFoundException fromAsync = thrownAs(
                    () -> CompletableFutureUtils.joinLikeSync(
                        async.getAlternateContact(r -> r.alternateContactType("BILLING"))),
                    ResourceNotFoundException.class);
                DocumentException fromDynamic = thrownAs(
                    () -> dynamic.call("GetAlternateContact", map("AlternateContactType", "BILLING")),
                    DocumentException.class);

                assertThat(fromSync.statusCode()).isEqualTo(404);
                assertThat(fromSync.getMessage()).startsWith("missing");
                assertThat(Document.of(fromAsync)).isEqualTo(Document.of(fromSync));
                assertThat(fromDynamic.statusCode()).isEqualTo(404);
                assertThat(fromDynamic.getContents()).isEqualTo(Document.of(fromSync));
                assertThat(fromDynamic.getContents().getMember("message").asString()).isEqualTo("missing");
            }
        }
        assertThat(httpClient.calls() + asyncHttpClient.calls()).isZero();
    }

    private DynamicClient things(String service, SdkPlugin plugin) {
        return DynamicClient.builder()
                            .model(THINGS)
                            .serviceId(ShapeId.from(NAMESPACE + service))
                            .region(Region.US_WEST_2)
                            .credentialsProvider(CREDENTIALS)
                            .endpointOverride(ENDPOINT)
                            .httpClient(httpClient)
                            .httpClient(asyncHttpClient)
                            .addPlugin(plugin)
                            .build();
    }

    private DynamicClient account(Model model, MockPlugin plugin) {
        DynamicClient.Builder builder = DynamicClient.builder()
                                                     .model(model)
                                                     .region(Region.US_EAST_1)
                                                     .credentialsProvider(CREDENTIALS)
                                                     .httpClient(httpClient)
                                                     .httpClient(asyncHttpClient);
        if (plugin != null) {
            builder.addPlugin(plugin);
        }
        return builder.build();
    }

    private AccountClient accountSync(Object item) {
        return AccountClient.builder().region(Region.US_EAST_1).credentialsProvider(CREDENTIALS).httpClient(httpClient)
                            .addPlugin(mockOf(item)).build();
    }

    private AccountAsyncClient accountAsync(Object item) {
        return AccountAsyncClient.builder().region(Region.US_EAST_1).credentialsProvider(CREDENTIALS)
                                 .httpClient(asyncHttpClient).addPlugin(mockOf(item)).build();
    }

    /**
     * A plugin whose queue holds one item, enqueued with the method a test would use for it: documents with
     * {@code enqueueDocument}, modeled errors with {@code enqueueError}, generated outputs with {@code enqueue}.
     */
    private static MockPlugin mockOf(Object item) {
        MockQueue queue = new MockQueue();
        if (item instanceof Document) {
            queue.enqueueDocument((Document) item);
        } else if (item instanceof Throwable) {
            queue.enqueueError((SerializableStruct) item);
        } else {
            queue.enqueue((SerializableStruct) item);
        }
        return MockPlugin.builder().addQueue(queue).build();
    }

    private static Document thing(DynamicClient client, String name, int count) {
        Map<String, Document> owner = new LinkedHashMap<>();
        owner.put("name", Document.of("owner-" + name));
        owner.put("level", Document.of(count * 10));
        return client.createStruct(GET_THING_OUTPUT,
                                   map("name", name,
                                       "count", count,
                                       "version", "v-" + name,
                                       "created", Document.of(Instant.ofEpochSecond(1_700_000_000L + count)),
                                       "tags", Document.of(Arrays.asList(Document.of(name), Document.of("tag"))),
                                       "owner", Document.of(owner)));
    }

    private static DocumentException thingNotFound(DynamicClient client, String requestId) {
        return client.createStruct(THING_NOT_FOUND, map("message", "no such thing", "thingId", "a", "retries", 2))
                     .asShape(DocumentException.builder(errorSchema(client, "GetThing", "ThingNotFound"))
                                               .requestId(requestId));
    }

    private static Schema errorSchema(DynamicClient client, String operation, String errorName) {
        return client.getOperation(operation).errorSchemas().stream()
                     .filter(s -> s.id().getName().equals(errorName))
                     .findFirst()
                     .orElseThrow(() -> new AssertionError(operation + " has no error " + errorName));
    }

    /**
     * A map document from alternating member names and values; strings and integers are converted with
     * {@code Document.of}.
     */
    private static Document map(Object... namesAndValues) {
        Map<String, Document> members = new LinkedHashMap<>();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            Object value = namesAndValues[i + 1];
            members.put((String) namesAndValues[i], value instanceof Document ? (Document) value
                                                    : value instanceof Integer ? Document.of((Integer) value)
                                                    : Document.of((String) value));
        }
        return Document.of(members);
    }

    private static <T extends Throwable> T thrownAs(ThrowingCallable call, Class<T> type) {
        Throwable thrown = catchThrowable(call);
        assertThat(thrown).isInstanceOf(type);
        return type.cast(thrown);
    }

    private static Model assemble(String file) {
        URL url = DynamicClientMockPluginTest.class.getResource(file);
        if (url == null) {
            throw new IllegalStateException("Test model " + file + " not found");
        }
        return Model.assembler().discoverModels(Model.class.getClassLoader()).addImport(url).assemble().unwrap();
    }

    /**
     * The Smithy JSON AST that the generated Account client is generated from.
     */
    private static Model accountModel() {
        Path path = Paths.get("..", "..", "services", "account", "model", "model.json");
        assertThat(path).as("The Account model, read from the source tree").isRegularFile();
        return Model.assembler().discoverModels(Model.class.getClassLoader()).addImport(path).assemble().unwrap();
    }

    /**
     * Records what the interceptor hooks of a mocked call see.
     */
    private static final class RecordingInterceptor implements ExecutionInterceptor {
        private final List<SdkHttpRequest> requests = Collections.synchronizedList(new ArrayList<>());
        private final List<Integer> responseStatuses = Collections.synchronizedList(new ArrayList<>());
        private final List<String> versionHeaders = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            requests.add(context.httpRequest());
        }

        @Override
        public void afterTransmission(Context.AfterTransmission context, ExecutionAttributes executionAttributes) {
            responseStatuses.add(context.httpResponse().statusCode());
            context.httpResponse().firstMatchingHeader("X-Thing-Version").ifPresent(versionHeaders::add);
        }
    }

    /**
     * An HTTP client that counts and fails every call: a mocked client must never reach it.
     */
    private static final class FailingHttpClient implements SdkHttpClient {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
            calls.incrementAndGet();
            throw new AssertionError("The mocked client called its HTTP client");
        }

        int calls() {
            return calls.get();
        }

        @Override
        public void close() {
        }
    }

    private static final class FailingAsyncHttpClient implements SdkAsyncHttpClient {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
            calls.incrementAndGet();
            throw new AssertionError("The mocked client called its async HTTP client");
        }

        int calls() {
            return calls.get();
        }

        @Override
        public void close() {
        }
    }
}
