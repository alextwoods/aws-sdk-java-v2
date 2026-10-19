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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.protocol.AsyncClientTransport;
import software.amazon.awssdk.core.client.protocol.AsyncResponseHandoff;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.core.client.protocol.ClientTransport;
import software.amazon.awssdk.core.client.protocol.HttpMessageExchange;
import software.amazon.awssdk.core.client.protocol.MessageExchange;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.testutils.mock.internal.ClientConfigurationAccess;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Mocks a generated client by replacing its transport: calls return queued results instead of sending requests.
 *
 * <p>The plugin installs a mock transport as the client's {@code SdkClientOption.CLIENT_TRANSPORT} (synchronous
 * clients) and {@code ASYNC_CLIENT_TRANSPORT} (asynchronous clients). The rest of the request pipeline runs unchanged:
 * the client's protocol serializes and the pipeline signs each request, and the protocol deserializes each mocked
 * response. The client's HTTP client is never called, so no network request is sent.
 *
 * <p>For each attempt the transport asks the matchers, in the order they were added, for a {@link MockedResult}; a
 * {@link MockQueue} added with {@link Builder#addQueue} is a matcher that polls the queue. The first non-null result is
 * used:
 * <ul>
 *     <li>an output value, schema-backed Document or modeled exception ({@link MockedResult.Output}) is turned into a
 *     response with the {@link ResponseGenerator} of the client's protocol. Its schema must be the operation's output
 *     or one of its errors; otherwise the call fails with an {@link IllegalStateException} naming the expected and the
 *     queued shape IDs, and the result is still consumed;</li>
 *     <li>a raw response ({@link MockedResult.Response}) is returned unchanged;</li>
 *     <li>an {@link MockedResult.Error} is thrown by the transport.</li>
 * </ul>
 * When no matcher returns a result, for example because the queues are empty, the call fails with an
 * {@link IllegalStateException} whose message starts with {@code "Mock queue exhausted"}.
 *
 * <p>A retried attempt consumes another result, as it would send another request. Every attempt's request is recorded,
 * unless {@link Builder#trackRequests(boolean)} turns that off, and can be read with {@link #getRequests()}.
 *
 * <p>Usage:
 * <pre>{@code
 * MockQueue queue = new MockQueue().enqueue(DescribeAlarmsResponse.builder().build());
 * MockPlugin mock = MockPlugin.builder().addQueue(queue).build();
 * CloudWatchClient client = CloudWatchClient.builder().addPlugin(mock).build();
 * client.describeAlarms(); // returns the queued response
 * }</pre>
 *
 * <p>The plugin works only with clients generated with schema-based serialization: the transport reads the call's
 * {@link SdkExecutionAttribute#API_OPERATION} and {@link SdkExecutionAttribute#CLIENT_PROTOCOL}.
 *
 * <p>Port of smithy-java {@code MockPlugin}. It does not wrap the client's protocol; the call's input is read by an
 * execution interceptor that the plugin adds.
 */
@SdkPublicApi
@ThreadSafe
public final class MockPlugin implements SdkPlugin {
    private static final ExecutionAttribute<SerializableStruct> MOCKED_INPUT = new ExecutionAttribute<>("MockedInput");

    private final List<Function<MatcherRequest, MockedResult>> matchers;
    private final List<MockedRequest> requests = Collections.synchronizedList(new ArrayList<>());
    private final boolean trackRequests;

    private MockPlugin(Builder builder) {
        this.matchers = Collections.unmodifiableList(new ArrayList<>(builder.matchers));
        this.trackRequests = builder.trackRequests;
    }

    /**
     * Creates a builder.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Installs the mock transports, and an interceptor that records the call's input, on the client.
     *
     * @param config the configuration of a generated client
     * @throws IllegalStateException if {@code config} is not the configuration builder of a generated client
     */
    @Override
    public void configureClient(SdkServiceClientConfiguration.Builder config) {
        SdkClientConfiguration.Builder clientConfiguration = ClientConfigurationAccess.clientConfiguration(config);
        clientConfiguration.option(SdkClientOption.CLIENT_TRANSPORT, new MockClientTransport());
        clientConfiguration.option(SdkClientOption.ASYNC_CLIENT_TRANSPORT, new MockAsyncClientTransport());
        config.overrideConfiguration(o -> o.addExecutionInterceptor(new InputRecorder()));
    }

    /**
     * Returns the requests the plugin received, one per attempt, in the order they were sent.
     *
     * @return a copy of the received requests
     */
    public List<MockedRequest> getRequests() {
        synchronized (requests) {
            return Collections.unmodifiableList(new ArrayList<>(requests));
        }
    }

    /**
     * Clears the received requests.
     */
    public void clearRequests() {
        requests.clear();
    }

    private SdkHttpFullResponse respond(ExecutionAttributes context, SdkHttpFullRequest request) {
        ApiOperation<?, ?> operation = Validate.notNull(context.getAttribute(SdkExecutionAttribute.API_OPERATION),
                                                        "MockPlugin needs a client generated with schema-based "
                                                        + "serialization: the call has no API_OPERATION");
        SerializableStruct input = context.getAttribute(MOCKED_INPUT);
        if (trackRequests) {
            requests.add(new MockedRequest(input, request));
        }

        MatcherRequest matcherRequest = new MatcherRequest(context, operation, input, request);
        MockedResult result = null;
        for (Function<MatcherRequest, MockedResult> matcher : matchers) {
            result = matcher.apply(matcherRequest);
            if (result != null) {
                break;
            }
        }
        if (result == null) {
            throw new IllegalStateException("Mock queue exhausted: no queued result or matcher for the call to "
                                            + operation.schema().id());
        }

        if (result instanceof MockedResult.Response) {
            return ((MockedResult.Response) result).response();
        }
        if (result instanceof MockedResult.Error) {
            throw MockPlugin.<RuntimeException>sneakyThrow(((MockedResult.Error) result).error());
        }
        return generate(context, operation, (MockedResult.Output) result);
    }

    private static SdkHttpFullResponse generate(ExecutionAttributes context,
                                                ApiOperation<?, ?> operation,
                                                MockedResult.Output output) {
        SerializableStruct value = output.output();
        Schema schema = value.sdkSchema();
        checkSchema(operation, schema.id());
        ResponseGenerator generator = output.generator();
        if (generator == null) {
            ClientProtocol<?, ?> protocol = Validate.notNull(context.getAttribute(SdkExecutionAttribute.CLIENT_PROTOCOL),
                                                             "The call to %s has no CLIENT_PROTOCOL",
                                                             operation.schema().id());
            generator = ResponseGenerator.forProtocol(protocol.id());
        }
        return schema.hasTrait(TraitKey.ERROR_TRAIT) ? generator.generateError(operation, value)
                                                     : generator.generateOutput(operation, value);
    }

    private static void checkSchema(ApiOperation<?, ?> operation, ShapeId queued) {
        List<ShapeId> expected = new ArrayList<>();
        expected.add(operation.outputSchema().id());
        for (Schema error : operation.errorSchemas()) {
            expected.add(error.id());
        }
        if (!expected.contains(queued)) {
            throw new IllegalStateException("Expected one of " + expected + " but queued " + queued);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> E sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

    /**
     * Creates a {@link MockPlugin}.
     */
    public static final class Builder {
        private final List<Function<MatcherRequest, MockedResult>> matchers = new ArrayList<>();
        private boolean trackRequests = true;

        private Builder() {
        }

        /**
         * Sets whether the plugin records the requests it receives, for {@link MockPlugin#getRequests()}. Defaults to
         * {@code true}; turn it off to avoid keeping requests in memory, for example in benchmarks.
         *
         * @param trackRequests whether to record requests
         * @return this builder
         */
        public Builder trackRequests(boolean trackRequests) {
            this.trackRequests = trackRequests;
            return this;
        }

        /**
         * Adds a matcher, which mocks the response of a call when it returns a non-null result. When it returns null,
         * the next matcher is asked.
         *
         * @param matcher the matcher
         * @return this builder
         * @throws NullPointerException if {@code matcher} is null
         */
        public Builder addMatcher(Function<MatcherRequest, MockedResult> matcher) {
            matchers.add(Validate.paramNotNull(matcher, "matcher"));
            return this;
        }

        /**
         * Adds a queue of results, as a matcher that polls it. Matchers are asked in the order they were added, so a
         * queue can follow more specific matchers, or another queue that is drained first.
         *
         * @param queue the queue
         * @return this builder
         * @throws NullPointerException if {@code queue} is null
         */
        public Builder addQueue(MockQueue queue) {
            Validate.paramNotNull(queue, "queue");
            matchers.add(request -> queue.poll());
            return this;
        }

        /**
         * Creates the plugin.
         *
         * @return the plugin
         */
        public MockPlugin build() {
            return new MockPlugin(this);
        }
    }

    /**
     * Records the input of each call for the transport.
     */
    private static final class InputRecorder implements ExecutionInterceptor {
        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            SdkRequest request = context.request();
            if (request instanceof SerializableStruct) {
                executionAttributes.putAttribute(MOCKED_INPUT, (SerializableStruct) request);
            }
        }
    }

    /**
     * The synchronous mock transport.
     */
    private final class MockClientTransport implements ClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> {
        @Override
        public MessageExchange<SdkHttpFullRequest, SdkHttpFullResponse> messageExchange() {
            return HttpMessageExchange.INSTANCE;
        }

        @Override
        public SdkHttpFullResponse send(ExecutionAttributes context, SdkHttpFullRequest request) {
            return respond(context, request);
        }

        @Override
        public void close() {
        }
    }

    /**
     * The asynchronous mock transport. Responses are complete, with buffered bodies, so the streaming handoff is not
     * used.
     */
    private final class MockAsyncClientTransport
        implements AsyncClientTransport<SdkHttpFullRequest, SdkHttpFullResponse> {
        @Override
        public MessageExchange<SdkHttpFullRequest, SdkHttpFullResponse> messageExchange() {
            return HttpMessageExchange.INSTANCE;
        }

        @Override
        public CompletableFuture<SdkHttpFullResponse> send(ExecutionAttributes context,
                                                           SdkHttpFullRequest request,
                                                           AsyncResponseHandoff<SdkHttpFullResponse> streamingHandoff) {
            try {
                return CompletableFuture.completedFuture(respond(context, request));
            } catch (Throwable t) {
                return CompletableFutureUtils.failedFuture(t);
            }
        }

        @Override
        public void close() {
        }
    }
}
