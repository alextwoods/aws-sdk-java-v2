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

package software.amazon.awssdk.awscodegen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Event stream operations (Requirements 8.4, 8.5, 8.9): as in V2, they have only async methods. An output event
 * stream is handed to the operation's {@code <Operation>ResponseHandler} (in the model package) and the call's future
 * is {@code CompletableFuture<Void>}; an input event stream is a {@code Publisher} of the input event union, set on
 * the input as its event stream member. There is no request-only method, the sync client has no method, and the
 * model types hold the event stream member instead of rejecting it. {@code codegen-tests} compiles and calls them.
 */
class EventStreamClientGenerationTest {
    private static final String ROOT = "software/amazon/awssdk/services/streams/";

    private static final String REST_JSON = """
        $version: "2"
        namespace example.streams

        @aws.protocols#restJson1
        @aws.auth#sigv4(name: "streams")
        service Streams {
            version: "2024-01-01"
            operations: [Subscribe, Publish, Chat]
        }

        @http(method: "POST", uri: "/subscribe")
        operation Subscribe {
            input := {
                @httpHeader("x-topic")
                topic: String
            }
            output := {
                @httpHeader("x-session")
                sessionId: String
                @httpPayload
                events: OutEvents
            }
        }

        @http(method: "POST", uri: "/publish")
        operation Publish {
            input := {
                @httpHeader("x-topic")
                topic: String
                @required
                @httpPayload
                events: InEvents
            }
            output := {
                @httpHeader("x-count")
                count: Integer
            }
        }

        @http(method: "POST", uri: "/chat")
        operation Chat {
            input := {
                @httpPayload
                events: InEvents
            }
            output := {
                @httpPayload
                events: OutEvents
            }
        }

        @streaming
        union OutEvents {
            message: MessageEvent
            failure: FailureEvent
        }

        @streaming
        union InEvents {
            message: MessageEvent
        }

        structure MessageEvent {
            text: String
        }

        @error("client")
        structure FailureEvent {
            reason: String
        }
        """;

    private static final String AWS_JSON = """
        $version: "2"
        namespace example.streams

        @aws.protocols#awsJson1_1
        @aws.auth#sigv4(name: "streams")
        service Streams {
            version: "2024-01-01"
            operations: [Watch]
        }

        operation Watch {
            input := { name: String }
            output := {
                status: String
                events: WatchEvents
            }
        }

        @streaming
        union WatchEvents {
            change: ChangeEvent
        }

        structure ChangeEvent {
            path: String
        }
        """;

    @TempDir
    Path temp;

    @Test
    void anOutputEventStreamOperationTakesItsResponseHandler() throws IOException {
        AwsCodegenResult result = generate(REST_JSON);
        String client = read(result, "StreamsAsyncClient.java");
        assertContains(client, "    CompletableFuture<Void> subscribe(SubscribeRequest request, "
                               + "SubscribeResponseHandler asyncResponseHandler);\n");
        assertContains(client, "    default CompletableFuture<Void> subscribe(Consumer<SubscribeRequest.Builder> "
                               + "request, SubscribeResponseHandler asyncResponseHandler) {\n");
        assertContains(client, "        return subscribe(builder.build(), asyncResponseHandler);\n");
        assertNoPlainOverload(client, "subscribe", "SubscribeRequest");

        String handler = read(result, "model/SubscribeResponseHandler.java");
        assertContains(handler, "package software.amazon.awssdk.services.streams.model;\n");
        assertContains(handler, "import software.amazon.awssdk.awscore.eventstream.EventStreamResponseHandler;\n");
        assertContains(handler, "public interface SubscribeResponseHandler extends "
                                + "EventStreamResponseHandler<SubscribeResponse, OutEvents> {\n");

        String implementation = read(result, "DefaultStreamsAsyncClient.java");
        assertContains(implementation, "    public CompletableFuture<Void> subscribe(SubscribeRequest request, "
                                       + "SubscribeResponseHandler asyncResponseHandler) {\n"
                                       + "        return engine.executeAsync(subscribePlan, request, null, null, "
                                       + "asyncResponseHandler);\n");
    }

    @Test
    void anInputEventStreamOperationTakesAPublisherOfItsEvents() throws IOException {
        AwsCodegenResult result = generate(REST_JSON);
        String client = read(result, "StreamsAsyncClient.java");
        assertContains(client, "    CompletableFuture<PublishResponse> publish(PublishRequest request, "
                               + "Publisher<InEvents> requestStream);\n");
        assertContains(client, "    default CompletableFuture<PublishResponse> publish(Consumer<PublishRequest.Builder> "
                               + "request, Publisher<InEvents> requestStream) {\n");
        assertContains(client, "import org.reactivestreams.Publisher;\n");
        assertNoPlainOverload(client, "publish", "PublishRequest");
        assertFalse(result.generatedFiles().contains(Path.of(ROOT + "model/PublishResponseHandler.java")));

        String implementation = read(result, "DefaultStreamsAsyncClient.java");
        assertContains(implementation, "    public CompletableFuture<PublishResponse> publish(PublishRequest request, "
                                       + "Publisher<InEvents> requestStream) {\n"
                                       + "        PublishRequest.Builder builder = request.toBuilder();\n"
                                       + "        builder.setMemberValue(request.schema().member(\"events\"), "
                                       + "PublisherEventStream.of(requestStream));\n"
                                       + "        return engine.executeAsync(publishPlan, builder.build(), null, "
                                       + "null);\n");
        assertContains(implementation,
                       "import software.amazon.awssdk.protocols.core.eventstream.PublisherEventStream;\n");
    }

    @Test
    void aBidirectionalOperationTakesThePublisherAndTheHandler() throws IOException {
        AwsCodegenResult result = generate(REST_JSON);
        String client = read(result, "StreamsAsyncClient.java");
        assertContains(client, "    CompletableFuture<Void> chat(ChatRequest request, Publisher<InEvents> "
                               + "requestStream, ChatResponseHandler asyncResponseHandler);\n");
        assertContains(client, "        return chat(builder.build(), requestStream, asyncResponseHandler);\n");
        assertNoPlainOverload(client, "chat", "ChatRequest");
        assertContains(read(result, "model/ChatResponseHandler.java"),
                       "public interface ChatResponseHandler extends EventStreamResponseHandler<ChatResponse, "
                       + "OutEvents> {\n");
        String implementation = read(result, "DefaultStreamsAsyncClient.java");
        assertContains(implementation, "        return engine.executeAsync(chatPlan, builder.build(), null, null, "
                                       + "asyncResponseHandler);\n");
    }

    @Test
    void theSyncClientHasNoEventStreamOperation() throws IOException {
        AwsCodegenResult result = generate(REST_JSON);
        String client = read(result, "StreamsClient.java");
        for (String method : List.of("subscribe(", "publish(", "chat(")) {
            assertFalse(client.contains(method), () -> method + " in\n" + client);
        }
        assertContains(client, "operations with an event stream are available only on the async client");
        String implementation = read(result, "DefaultStreamsClient.java");
        for (String plan : List.of("subscribePlan", "publishPlan", "chatPlan")) {
            assertFalse(implementation.contains(plan), () -> plan + " in\n" + implementation);
        }
    }

    @Test
    void theModelTypesHoldTheEventStreamMemberInsteadOfRejectingIt() throws IOException {
        AwsCodegenResult result = generate(REST_JSON);
        for (String type : List.of("SubscribeRequest", "SubscribeResponse", "PublishRequest", "PublishResponse",
                                   "ChatRequest", "ChatResponse")) {
            String source = read(result, "model/" + type + ".java");
            assertFalse(source.contains("is bound to the call"), () -> type + "\n" + source);
        }
        String output = read(result, "model/SubscribeResponse.java");
        assertContains(output, "    private final EventStream<? extends SerializableStruct> events;\n");
        assertContains(output, " -> builder.events = de.readEventStream(member);\n");
        assertContains(output, " -> this.events = (EventStream<? extends SerializableStruct>) "
                               + "SchemaUtils.validateSameMember(SCHEMA_EVENTS, member, value);\n");
        assertContains(output, " -> (T) SchemaUtils.validateSameMember(SCHEMA_EVENTS, member, this.events);\n");
        // No V2 accessor or setter, and not part of the value.
        assertFalse(output.contains("events()"), output);
        assertFalse(output.contains("Objects.hashCode(this.events)"), output);
        // A @required input event stream is not required by build(): it is set from the call's publisher.
        String input = read(result, "model/PublishRequest.java");
        assertFalse(input.contains("MissingRequiredMembersException"), input);
        // The event unions are generated union model types.
        assertContains(read(result, "model/OutEvents.java"), "public final class OutEvents implements "
                                                              + "SerializableStruct");
        assertContains(read(result, "model/InEvents.java"), "public final class InEvents implements "
                                                             + "SerializableStruct");
    }

    @Test
    void anAwsJsonOutputEventStreamOperationTakesItsResponseHandler() throws IOException {
        AwsCodegenResult result = generate(AWS_JSON);
        String client = read(result, "StreamsAsyncClient.java");
        assertContains(client, "    CompletableFuture<Void> watch(WatchRequest request, WatchResponseHandler "
                               + "asyncResponseHandler);\n");
        assertNoPlainOverload(client, "watch", "WatchRequest");
        assertFalse(read(result, "StreamsClient.java").contains("watch("));
        assertContains(read(result, "model/WatchResponseHandler.java"),
                       "public interface WatchResponseHandler extends EventStreamResponseHandler<WatchResponse, "
                       + "WatchEvents> {\n");
        assertFalse(read(result, "model/WatchResponse.java").contains("is bound to the call"));
    }

    private static void assertNoPlainOverload(String client, String method, String input) {
        assertFalse(client.contains(method + "(" + input + " request)"), client);
        assertFalse(client.contains(method + "(" + input + " request, AwsRequestOverrideConfiguration"), client);
        assertFalse(client.contains(method + "()"), client);
    }

    private static void assertContains(String source, String expected) {
        assertTrue(source.contains(expected), () -> "Expected\n" + expected + "\nin\n" + source);
    }

    private AwsCodegenResult generate(String model) throws IOException {
        Path models = Files.createDirectories(temp.resolve("model-" + System.nanoTime()));
        Files.writeString(models.resolve("streams.smithy"), model, StandardCharsets.UTF_8);
        return AwsCodegen.generate(new AwsCodegenRequest("example.streams#Streams",
                                                         "software.amazon.awssdk.services.streams", models,
                                                         temp.resolve("generated-" + System.nanoTime())), List.of());
    }

    private static String read(AwsCodegenResult result, String name) throws IOException {
        return Files.readString(result.sourceRoot().resolve(ROOT + name));
    }
}
