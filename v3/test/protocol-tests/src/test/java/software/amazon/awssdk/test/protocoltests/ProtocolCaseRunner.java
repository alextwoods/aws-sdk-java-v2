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

package software.amazon.awssdk.test.protocoltests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.opentest4j.AssertionFailedError;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.operation.AwsOperationDescriptor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.client.config.ConfigurationSources;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.identity.providers.StaticCredentialsProvider;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.pipeline.client.AwsClientBuildState;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.BackoffStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.smithy.runtime.registry.SchemaTypeRegistry;
import software.amazon.awssdk.test.protocoltests.support.Bodies;
import software.amazon.awssdk.test.protocoltests.support.Canonical;
import software.amazon.awssdk.test.protocoltests.support.CapturingTransport;
import software.amazon.awssdk.test.protocoltests.support.GeneratedClients;
import software.amazon.awssdk.test.protocoltests.support.NodeValues;
import software.amazon.awssdk.test.protocoltests.support.ProtocolCase;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.schema.Unit;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.IdempotencyTokenTrait;
import software.amazon.smithy.protocoltests.traits.HttpRequestTestCase;
import software.amazon.smithy.protocoltests.traits.HttpResponseTestCase;

/**
 * Runs one protocol test case through the generated client of its service and the real V3 pipeline.
 *
 * <p>Each case builds a fresh client through the generated public builder: the case's region (default
 * {@code us-east-1}), fixed static credentials, retries disabled (one attempt), external configuration sources
 * disabled (so the environment and profile files cannot change the call), the {@link CapturingTransport}, and, except
 * for the AmazonS3 test service (whose canonical S3 rules derive the host), an endpoint override of
 * {@code https://<case host>} ({@code example.com} when the case gives none). The modeled input is built with the
 * generated builder from the case params, and the call goes through the generated operation method:
 * <ul>
 *   <li>request cases compare the one transmitted request with the case: method, path, the expected, forbidden and
 *       required query parameters (raw, as transmitted), the expected (exact value; multiple values joined with
 *       {@code ", "}), forbidden and required headers, the resolved host, and the body by media type
 *       ({@link Bodies});</li>
 *   <li>response cases reply with the case response and compare the generated output with the output the params
 *       describe, built with the generated builder ({@link Canonical}), plus the bytes of a streaming payload;</li>
 *   <li>error cases reply with the case response and require the generated modeled exception of the error shape,
 *       with the case status code, the members the params describe and, when the case gives one, the error code.</li>
 * </ul>
 *
 * <p>The only normalization is the idempotency token: when a case expects the Smithy placeholder token
 * {@code 00000000-0000-4000-8000-000000000000} and its params do not set the token member, the one UUID the client
 * generated in its place is replaced by the placeholder before comparison (Smithy protocol tests assume a test token
 * generator; V3 generates a random version-4 UUID, Requirement 13.9).
 */
final class ProtocolCaseRunner {
    private static final String PLACEHOLDER_TOKEN = "00000000-0000-4000-8000-000000000000";
    private static final Pattern UUID_V4 =
        Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
    private static final AwsCredentialsIdentity CREDENTIALS = AwsCredentialsIdentity.create("AKIDEXAMPLE",
                                                                                             "SECRETEXAMPLE");
    private static final String DEFAULT_HOST = "example.com";
    private static final String DEFAULT_REGION = "us-east-1";
    private static final ShapeId AMAZON_S3 = ShapeId.from("com.amazonaws.s3#AmazonS3");

    private final Model model;
    private final GeneratedClients clients;
    private final NodeValues values;

    ProtocolCaseRunner(Model model, GeneratedClients clients) {
        this.model = model;
        this.clients = clients;
        this.values = new NodeValues(model);
    }

    void run(ProtocolCase c) throws Exception {
        switch (c.kind()) {
            case REQUEST -> request(c, (HttpRequestTestCase) c.http());
            case RESPONSE -> response(c, (HttpResponseTestCase) c.http());
            case ERROR -> error(c, (HttpResponseTestCase) c.http());
            default -> throw new AssertionFailedError("The conformance runner has no " + c.kind().label()
                                                      + " runner, so the case cannot run");
        }
    }

    // -----------------------------------------------------------------------------------------------------------
    // Request cases

    private void request(ProtocolCase c, HttpRequestTestCase test) throws Exception {
        Call call = call(c, test.getParams());
        CapturingTransport transport = CapturingTransport.capturing();
        Throwable failure = call.invoke(client(c, test.getHost(), test.getVendorParams(), transport));
        List<CapturingTransport.Captured> requests = transport.requests();
        if (requests.isEmpty()) {
            throw new AssertionFailedError("The client transmitted no request: "
                                           + (failure == null ? "the call returned" : describe(failure)), failure);
        }
        assertEquals(1, requests.size(), "transmitted requests");
        CapturingTransport.Captured captured = requests.get(0);
        SdkHttpRequest request = captured.request();
        boolean normalizeToken = expectsPlaceholderToken(c, test);

        assertEquals(test.getMethod(), request.method(), "method");
        assertEquals(test.getUri(), request.encodedPath(), "uri");
        test.getResolvedHost().ifPresent(host -> assertEquals(host, request.host(), "resolved host"));

        String rawQuery = normalize(request.encodedQuery(), normalizeToken);
        List<String> query = rawQuery.isEmpty() ? List.of() : Arrays.asList(rawQuery.split("&"));
        List<String> remaining = new ArrayList<>(query);
        for (String expected : test.getQueryParams()) {
            assertTrue(remaining.remove(expected), () -> "query parameter " + expected + " in " + query);
        }
        List<String> names = query.stream().map(p -> p.contains("=") ? p.substring(0, p.indexOf('=')) : p).toList();
        for (String forbidden : test.getForbidQueryParams()) {
            assertFalse(names.contains(forbidden), () -> "forbidden query parameter " + forbidden + " in " + query);
        }
        for (String required : test.getRequireQueryParams()) {
            assertTrue(names.contains(required), () -> "required query parameter " + required + " in " + query);
        }
        for (Map.Entry<String, String> header : test.getHeaders().entrySet()) {
            List<String> actual = request.headers().allValues(header.getKey());
            assertEquals(header.getValue(), actual.isEmpty() ? null : normalize(String.join(", ", actual),
                                                                                normalizeToken),
                         "header " + header.getKey());
        }
        for (String forbidden : test.getForbidHeaders()) {
            assertFalse(request.headers().contains(forbidden), "forbidden header " + forbidden);
        }
        for (String required : test.getRequireHeaders()) {
            assertTrue(request.headers().contains(required), "required header " + required);
        }
        if (test.getBody().isPresent()) {
            String mediaType = test.getBodyMediaType().orElse("");
            byte[] body = captured.body();
            if (normalizeToken && !Bodies.binary(mediaType)) {
                body = normalize(new String(body, StandardCharsets.UTF_8), true).getBytes(StandardCharsets.UTF_8);
            }
            Bodies.assertBody(test.getBody().get(), mediaType, body);
        }
    }

    /** Whether the case expects the placeholder idempotency token for a token member its params leave unset. */
    private boolean expectsPlaceholderToken(ProtocolCase c, HttpRequestTestCase test) {
        StructureShape input = inputShape(c);
        boolean unsetToken = input.getAllMembers().values().stream()
                                  .anyMatch(m -> m.hasTrait(IdempotencyTokenTrait.class)
                                                 && !test.getParams().containsMember(m.getMemberName()));
        boolean expected = test.getHeaders().containsValue(PLACEHOLDER_TOKEN)
                           || test.getQueryParams().stream().anyMatch(q -> q.contains(PLACEHOLDER_TOKEN))
                           || test.getBody().filter(b -> b.contains(PLACEHOLDER_TOKEN)).isPresent();
        return unsetToken && expected;
    }

    private static String normalize(String text, boolean token) {
        return token ? UUID_V4.matcher(text).replaceAll(PLACEHOLDER_TOKEN) : text;
    }

    // -----------------------------------------------------------------------------------------------------------
    // Response and error cases

    private void response(ProtocolCase c, HttpResponseTestCase test) throws Exception {
        Call call = call(c, values.minimalParams(inputShape(c)));
        CapturingTransport transport = CapturingTransport.replying(response(test));
        Object result = call.invokeForResult(client(c, Optional.empty(), Node.objectNode(), transport));
        SerializableStruct output;
        byte[] stream = null;
        if (result instanceof ResponseBytes<?> bytes) {
            output = (SerializableStruct) bytes.response();
            stream = bytes.asByteArray();
        } else {
            output = (SerializableStruct) result;
        }
        StructureShape outputShape = model.expectShape(operation(c).getOutputShape(), StructureShape.class);
        SerializableStruct expected = build(call.registry, call.descriptor.outputId(),
                                            values.structure(outputShape, test.getParams()));
        assertEquals(Canonical.of(expected), Canonical.of(output), "output");
        Optional<MemberShape> streaming = streamingMember(outputShape);
        if (streaming.isPresent()) {
            String expectedStream = test.getParams().getStringMember(streaming.get().getMemberName())
                                        .map(s -> s.getValue()).orElse("");
            assertEquals(expectedStream, stream == null ? null : new String(stream, StandardCharsets.UTF_8),
                         "streaming payload " + streaming.get().getMemberName());
        }
    }

    private void error(ProtocolCase c, HttpResponseTestCase test) throws Exception {
        Call call = call(c, values.minimalParams(inputShape(c)));
        CapturingTransport transport = CapturingTransport.replying(response(test));
        Throwable failure = call.invoke(client(c, Optional.empty(), Node.objectNode(), transport));
        assertNotNull(failure, "the call succeeded; expected modeled error " + c.error());
        if (!(failure instanceof SerializableStruct modeled) || !modeled.schema().id().equals(c.error())) {
            throw new AssertionFailedError("expected the modeled error " + c.error() + " but the call failed with "
                                           + describe(failure), failure);
        }
        AwsServiceException service = (AwsServiceException) failure;
        assertEquals(test.getCode(), service.statusCode(), "status code");
        test.getVendorParams().getStringMember("code").ifPresent(
            code -> assertEquals(code.getValue(), service.awsErrorDetails().errorCode(), "error code"));
        StructureShape errorShape = model.expectShape(c.error(), StructureShape.class);
        SerializableStruct expected = build(call.registry, c.error(), values.structure(errorShape, test.getParams()));
        assertEquals(Canonical.of(expected), Canonical.of(modeled), "error members");
    }

    /** The case response, its body delivered as a one-shot streaming body, as a transport delivers it. */
    private static SdkHttpResponse response(HttpResponseTestCase test) {
        SdkHttpResponse.Builder response = SdkHttpResponse.builder().statusCode(test.getCode());
        test.getHeaders().forEach(response::addHeader);
        test.getBody().ifPresent(body -> {
            byte[] bytes = Bodies.bytes(body, test.getBodyMediaType().orElse(""));
            response.body(HttpBody.streaming(StreamingBodies.fromInputStream(new ByteArrayInputStream(bytes),
                                                                             bytes.length, null)));
        });
        return response.build();
    }

    // -----------------------------------------------------------------------------------------------------------
    // Clients and calls

    private OperationShape operation(ProtocolCase c) {
        return model.expectShape(c.operation(), OperationShape.class);
    }

    private StructureShape inputShape(ProtocolCase c) {
        return model.expectShape(operation(c).getInputShape(), StructureShape.class);
    }

    private Optional<MemberShape> streamingMember(StructureShape structure) {
        return structure.getAllMembers().values().stream().filter(values::isStreamingBlob).findFirst();
    }

    private static SerializableStruct build(SchemaTypeRegistry registry, ShapeId shape, Document document) {
        if (shape.equals(Unit.ID)) {
            // Operations without modeled input or output use smithy.api#Unit, which no service registry holds.
            return Unit.getInstance();
        }
        ShapeBuilder<?> builder = registry.createBuilder(shape);
        return (SerializableStruct) document.asShape(builder);
    }

    /** The generated call of the case's operation with the input {@code params} describe. */
    private Call call(ProtocolCase c, ObjectNode params) {
        GeneratedClients.Service service = clients.service(c.service());
        AwsOperationDescriptor descriptor = clients.operations(service).stream()
                                                   .filter(o -> o.id().equals(c.operation()))
                                                   .findFirst()
                                                   .orElseThrow(() -> new AssertionFailedError(
                                                       "No generated operation " + c.operation()));
        SchemaTypeRegistry registry = clients.typeRegistry(service);
        StructureShape input = inputShape(c);
        SerializableStruct request = build(registry, descriptor.inputId(), values.structure(input, params));
        RequestBody body = streamingMember(input)
            .map(m -> params.getStringMember(m.getMemberName())
                            .map(s -> RequestBody.fromString(s.getValue())).orElse(RequestBody.empty()))
            .orElse(null);
        boolean streamingOutput = streamingMember(model.expectShape(operation(c).getOutputShape(),
                                                                    StructureShape.class)).isPresent();
        return new Call(service, descriptor, registry, request, body, streamingOutput);
    }

    /** A built generated client of the case's service over {@code transport}. */
    private AutoCloseable client(ProtocolCase c, Optional<String> host, ObjectNode vendorParams,
                                 CapturingTransport transport) throws Exception {
        GeneratedClients.Service service = clients.service(c.service());
        Class<?> builderType = clients.type(service.builderClass());
        Object builder = clients.type(service.clientClass()).getMethod("builder").invoke(null);
        invoke(builderType, builder, "region", Region.of(region(vendorParams)));
        invoke(builderType, builder, "credentialsProvider", StaticCredentialsProvider.create(CREDENTIALS));
        invoke(builderType, builder, "retryStrategy",
               StandardRetryStrategy.builder().maxAttempts(1).backoffStrategy(BackoffStrategy.none())
                                    .throttlingBackoffStrategy(BackoffStrategy.none()).build());
        invoke(builderType, builder, "httpClient", transport);
        if (!c.service().equals(AMAZON_S3)) {
            invoke(builderType, builder, "endpointOverride", URI.create("https://" + host.orElse(DEFAULT_HOST)));
        }
        Field state = field(builder.getClass(), "state");
        ((AwsClientBuildState) state.get(builder)).configurationSources(ConfigurationSources.none());
        return (AutoCloseable) builderType.getMethod("build").invoke(builder);
    }

    /**
     * The client region of the case's {@code AwsConfig} vendor params. Configuration the V3 client has no setting
     * for fails the case instead of being ignored.
     */
    private static String region(ObjectNode vendorParams) {
        if (vendorParams.isEmpty() || vendorParams.containsMember("code")) {
            return DEFAULT_REGION;
        }
        ObjectNode scoped = vendorParams.expectObjectMember("scopedConfig");
        List<String> unsupported = new ArrayList<>();
        vendorParams.getStringMap().keySet().stream().filter(k -> !k.equals("scopedConfig"))
                    .forEach(k -> unsupported.add(k));
        scoped.getStringMap().keySet().stream().filter(k -> !k.equals("client"))
              .forEach(k -> unsupported.add("scopedConfig." + k + "=" + Node.printJson(scoped.expectMember(k))));
        ObjectNode client = scoped.getObjectMember("client").orElse(Node.objectNode());
        String region = DEFAULT_REGION;
        for (Map.Entry<String, Node> entry : client.getStringMap().entrySet()) {
            switch (entry.getKey()) {
                case "region" -> region = entry.getValue().expectStringNode().getValue();
                case "s3" -> entry.getValue().expectObjectNode().getStringMap().forEach((k, v) -> {
                    boolean virtual = k.equals("addressing_style")
                                      && (v.expectStringNode().getValue().equals("virtual")
                                          || v.expectStringNode().getValue().equals("auto"));
                    if (!virtual) {
                        unsupported.add("client.s3." + k + "=" + Node.printJson(v));
                    }
                });
                default -> unsupported.add("client." + entry.getKey());
            }
        }
        if (!unsupported.isEmpty()) {
            throw new AssertionFailedError("The V3 client has no configuration setting for the case's AwsConfig "
                                           + unsupported);
        }
        return region;
    }

    private static void invoke(Class<?> type, Object target, String name, Object argument) throws Exception {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 1
                && method.getParameterTypes()[0].isInstance(argument)) {
                method.invoke(target, argument);
                return;
            }
        }
        throw new NoSuchMethodException(type.getName() + "." + name);
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                Field field = c.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                // keep looking in the superclass
            }
        }
        throw new NoSuchFieldException(type.getName() + "." + name);
    }

    private static String describe(Throwable failure) {
        return software.amazon.awssdk.test.protocoltests.support.Conformance.describe(failure);
    }

    /** One generated operation call: the generated client method and its arguments. */
    private final class Call {
        private final GeneratedClients.Service service;
        private final AwsOperationDescriptor descriptor;
        private final SchemaTypeRegistry registry;
        private final SerializableStruct request;
        private final RequestBody body;
        private final boolean streamingOutput;

        Call(GeneratedClients.Service service, AwsOperationDescriptor descriptor, SchemaTypeRegistry registry,
             SerializableStruct request, RequestBody body, boolean streamingOutput) {
            this.service = service;
            this.descriptor = descriptor;
            this.registry = registry;
            this.request = request;
            this.body = body;
            this.streamingOutput = streamingOutput;
        }

        /** Calls the operation and closes the client; returns the failure, or {@code null} if the call returned. */
        Throwable invoke(AutoCloseable client) throws Exception {
            try {
                invokeForResult(client);
                return null;
            } catch (CallFailed e) {
                return e.getCause();
            }
        }

        /** Calls the operation and closes the client; returns the result. */
        Object invokeForResult(AutoCloseable client) throws Exception {
            try (client) {
                Method method = method();
                Object[] arguments = arguments(method);
                try {
                    return method.invoke(client, arguments);
                } catch (InvocationTargetException e) {
                    throw new CallFailed(e.getCause());
                }
            }
        }

        private Object[] arguments(Method method) {
            Class<?>[] types = method.getParameterTypes();
            Object[] arguments = new Object[types.length];
            for (int i = 0; i < types.length; i++) {
                if (types[i].isInstance(request)) {
                    arguments[i] = request;
                } else if (types[i] == RequestBody.class) {
                    arguments[i] = body;
                } else if (types[i] == ResponseTransformer.class) {
                    arguments[i] = ResponseTransformer.toBytes();
                } else if (types[i] == AwsRequestOverrideConfiguration.class) {
                    arguments[i] = null;
                }
            }
            return arguments;
        }

        /** The generated client method taking the generated request type and the operation's stream arguments. */
        private Method method() {
            Class<?> clientType = clients.type(service.clientClass());
            List<Class<?>> wanted = new ArrayList<>();
            // A Unit-input operation's generated method takes no request, only the optional override configuration.
            boolean unitInput = request instanceof Unit;
            if (!unitInput) {
                wanted.add(request.getClass());
            }
            if (body != null) {
                wanted.add(RequestBody.class);
            }
            if (streamingOutput) {
                wanted.add(ResponseTransformer.class);
            }
            if (unitInput) {
                wanted.add(AwsRequestOverrideConfiguration.class);
            }
            String name = descriptor.id().getName();
            for (Method method : clientType.getMethods()) {
                // By name too: operations can share an input type (TestGetNoPayload and TestPostNoPayload).
                if (Arrays.asList(method.getParameterTypes()).equals(wanted)
                    && method.getName().equalsIgnoreCase(name)) {
                    return method;
                }
            }
            throw new AssertionFailedError("The generated " + clientType.getSimpleName() + " has no method "
                                           + descriptor.id().getName() + wanted);
        }
    }

    /** A failure of the generated operation method itself. */
    private static final class CallFailed extends AssertionFailedError {
        private static final long serialVersionUID = 1L;

        CallFailed(Throwable cause) {
            super("the call failed: " + describe(cause), cause);
        }
    }
}
