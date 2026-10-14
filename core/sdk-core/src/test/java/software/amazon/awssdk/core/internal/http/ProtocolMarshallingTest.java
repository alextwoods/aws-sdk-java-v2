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

package software.amazon.awssdk.core.internal.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.OPERATION;
import static software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.SECRET;
import static software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.executionContext;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.client.protocol.MessageExchange;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.RecordingInterceptor;
import software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.StubProtocol;
import software.amazon.awssdk.core.internal.http.ProtocolStepsTestSupport.TestRequest;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.testutils.LogCaptor;
import software.amazon.awssdk.utils.IoUtils;

class ProtocolMarshallingTest {
    private static final String REQUEST_LOGGER = "software.amazon.awssdk.request";
    private static final String EXPECTED_LOG =
        "Using protocol com.example.test#stubProtocol for operation com.example#GetThing";

    @Test
    void marshall_withSchemaRequest_serializesThroughProtocolWithUnresolvedUri() {
        StubProtocol protocol = new StubProtocol();
        TestRequest input = new TestRequest(SECRET);
        ExecutionContext context = executionContext(protocol, input, null);

        SdkHttpFullRequest request = ProtocolMarshalling.marshall(context);

        assertThat(protocol.operation).isSameAs(OPERATION);
        assertThat(protocol.input).isSameAs(input);
        assertThat(protocol.context).isSameAs(context.executionAttributes());
        assertThat(protocol.endpoint).isSameAs(ProtocolMarshalling.UNRESOLVED_URI);
        assertThat(request.getUri().getHost()).isEqualTo("unresolved.invalid");
        assertThat(context.interceptorContext().httpRequest()).isSameAs(request);
    }

    @Test
    void marshall_afterModifyRequest_serializesTheInterceptorContextRequest() {
        StubProtocol protocol = new StubProtocol();
        TestRequest modified = new TestRequest("modified");
        ExecutionContext context = executionContext(protocol, new TestRequest("original"), null);
        context.interceptorContext(context.interceptorContext().copy(b -> b.request(modified)));

        ProtocolMarshalling.marshall(context);

        assertThat(protocol.input).isSameAs(modified);
    }

    @Test
    void marshall_withInterceptors_runsBeforeAndAfterMarshallingAroundSerialization() throws Exception {
        StubProtocol protocol = new StubProtocol();
        RecordingInterceptor interceptor = new RecordingInterceptor();
        protocol.serialize = (operation, input, attributes, endpoint) -> {
            interceptor.hooks.add("serializeRequest");
            return ProtocolStepsTestSupport.serializedRequest(endpoint, "body");
        };
        ExecutionContext context = executionContext(protocol, new TestRequest("a"), null, interceptor);

        SdkHttpFullRequest request = ProtocolMarshalling.marshall(context);

        assertThat(interceptor.hooks).containsExactly("beforeMarshalling", "serializeRequest", "afterMarshalling");
        assertThat(interceptor.attributes).allMatch(a -> a == context.executionAttributes());
        assertThat(interceptor.afterMarshalling).hasSize(1);
        assertThat(interceptor.afterMarshalling.get(0).httpRequest()).isSameAs(request);
        Optional<RequestBody> body = interceptor.afterMarshalling.get(0).requestBody();
        assertThat(body).isPresent();
        assertThat(body.get().optionalContentLength()).hasValue(4L);
        assertThat(body.get().contentType()).isEqualTo("application/cbor");
        try (InputStream stream = body.get().contentStreamProvider().newStream()) {
            assertThat(new String(IoUtils.toByteArray(stream), StandardCharsets.UTF_8)).isEqualTo("body");
        }
    }

    @Test
    void marshall_withoutBody_setsNoRequestBody() {
        StubProtocol protocol = new StubProtocol();
        protocol.serialize = (operation, input, attributes, endpoint) ->
            ProtocolStepsTestSupport.serializedRequest(endpoint, "").toBuilder().contentStreamProvider(null).build();
        RecordingInterceptor interceptor = new RecordingInterceptor();
        ExecutionContext context = executionContext(protocol, new TestRequest("a"), null, interceptor);

        ProtocolMarshalling.marshall(context);

        assertThat(interceptor.afterMarshalling.get(0).requestBody()).isEmpty();
    }

    @Test
    void marshall_withDebugEnabled_logsProtocolAndOperationOnceWithoutMemberValues() {
        StubProtocol protocol = new StubProtocol();
        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            ProtocolMarshalling.marshall(executionContext(protocol, new TestRequest(SECRET), null));

            List<LogEvent> events = logCaptor.loggedEvents();
            List<LogEvent> protocolEntries = events.stream()
                                                   .filter(e -> message(e).startsWith("Using protocol"))
                                                   .collect(Collectors.toList());
            assertThat(protocolEntries).hasSize(1);
            LogEvent entry = protocolEntries.get(0);
            assertThat(entry.getLoggerName()).isEqualTo(REQUEST_LOGGER);
            assertThat(entry.getLevel()).isEqualTo(Level.DEBUG);
            assertThat(message(entry)).isEqualTo(EXPECTED_LOG);
            assertThat(events).noneMatch(e -> message(e).contains(SECRET));
        }
    }

    @Test
    void marshall_whenCalledPerCall_logsOncePerCall() {
        StubProtocol protocol = new StubProtocol();
        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            ProtocolMarshalling.marshall(executionContext(protocol, new TestRequest("a"), null));
            ProtocolMarshalling.marshall(executionContext(protocol, new TestRequest("b"), null));

            assertThat(logCaptor.loggedEvents().stream().filter(e -> EXPECTED_LOG.equals(message(e)))).hasSize(2);
        }
    }

    @Test
    void marshall_withInfoLevel_doesNotLogProtocol() {
        try (LogCaptor logCaptor = LogCaptor.create(Level.INFO)) {
            ProtocolMarshalling.marshall(executionContext(new StubProtocol(), new TestRequest("a"), null));

            assertThat(logCaptor.loggedEvents()).noneMatch(e -> message(e).startsWith("Using protocol"));
        }
    }

    @Test
    void marshall_withMetricCollector_reportsMarshallingDuration() {
        MetricCollector collector = MetricCollector.create("ApiCall");

        ProtocolMarshalling.marshall(executionContext(new StubProtocol(), new TestRequest("a"), collector));

        assertThat(collector.collect().metricValues(CoreMetric.MARSHALLING_DURATION)).hasSize(1);
    }

    @Test
    void marshall_whenProtocolFails_propagatesExceptionAndSkipsAfterMarshalling() {
        StubProtocol protocol = new StubProtocol();
        SdkClientException failure = SdkClientException.create("Unable to marshall GetThing: missing label");
        protocol.serialize = (operation, input, attributes, endpoint) -> {
            throw failure;
        };
        RecordingInterceptor interceptor = new RecordingInterceptor();
        ExecutionContext context = executionContext(protocol, new TestRequest("a"), null, interceptor);

        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            assertThatThrownBy(() -> ProtocolMarshalling.marshall(context)).isSameAs(failure);
            assertThat(logCaptor.loggedEvents()).anyMatch(e -> EXPECTED_LOG.equals(message(e)));
        }
        assertThat(interceptor.hooks).containsExactly("beforeMarshalling");
        assertThat(context.interceptorContext().httpRequest()).isNull();
    }

    @Test
    void marshall_withNonSchemaRequest_throwsSdkClientException() {
        SdkRequest notSchema = new SdkRequest() {
            @Override
            public Optional<RequestOverrideConfiguration> overrideConfiguration() {
                return Optional.empty();
            }

            @Override
            public Builder toBuilder() {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<SdkField<?>> sdkFields() {
                return Collections.emptyList();
            }
        };

        assertThatThrownBy(() -> ProtocolMarshalling.marshall(executionContext(new StubProtocol(), notSchema, null)))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Unable to marshall GetThing")
            .hasMessageContaining("is not a schema-based request");
    }

    @Test
    void marshall_withoutOperation_throwsIllegalStateException() {
        ExecutionContext context = executionContext(new StubProtocol(), new TestRequest("a"), null);
        context.executionAttributes().putAttribute(SdkExecutionAttribute.API_OPERATION, null);

        assertThatThrownBy(() -> ProtocolMarshalling.marshall(context))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("API_OPERATION");
    }

    @Test
    void marshall_withoutProtocol_throwsIllegalStateException() {
        ExecutionContext context = executionContext(null, new TestRequest("a"), null);

        assertThatThrownBy(() -> ProtocolMarshalling.marshall(context))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("CLIENT_PROTOCOL");
    }

    @Test
    void marshall_withNonHttpProtocol_throwsIllegalStateException() {
        MessageExchange<SdkHttpFullRequest, SdkHttpFullResponse> other =
            new MessageExchange<SdkHttpFullRequest, SdkHttpFullResponse>() {
                @Override
                public String toString() {
                    return "OtherExchange";
                }
            };
        ExecutionContext context = executionContext(new StubProtocol(other), new TestRequest("a"), null);

        assertThatThrownBy(() -> ProtocolMarshalling.marshall(context))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("OtherExchange");
    }

    @Test
    void unresolvedUri_isInReservedInvalidDomain() {
        assertThat(ProtocolMarshalling.UNRESOLVED_URI).isEqualTo(URI.create("https://unresolved.invalid"));
    }

    private static String message(LogEvent event) {
        return event.getMessage().getFormattedMessage();
    }
}
