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

package software.amazon.awssdk.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_INPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_OUTPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_THING;
import static software.amazon.awssdk.pipeline.PipelineFixtures.SERVICE;
import static software.amazon.awssdk.pipeline.PipelineFixtures.retries;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.operation.AwsOperationDescriptor;
import software.amazon.awssdk.core.SdkResponseMetadata;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpFailureCategory;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.pipeline.PipelineFixtures.ScriptedHttp;
import software.amazon.awssdk.pipeline.PipelineFixtures.Struct;
import software.amazon.awssdk.retries.RetryStrategy;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Task 16.1 (Requirements 21.1, 21.13): the terminal pipeline exposes each response-metadata value the response and
 * the call supplied (request ID, extended request ID, status, service error code, attempt count, transport facts)
 * through the modeled response or the surfaced SDK exception, and the documented absent value otherwise, on both
 * drivers.
 */
class ResponseMetadataPropagationTest {
    private static final Map<String, String> TRANSPORT = Map.of(SdkResponseMetadata.HTTP_CLIENT, "ScriptedHttp");
    private static final AwsOperation<Struct, MetaResponse> GET_META = metaOperation();

    private final ScriptedHttp http = new ScriptedHttp();

    enum Driver {
        SYNC(HttpTransportMode.SYNC),
        ASYNC(HttpTransportMode.ASYNC);

        private final HttpTransportMode mode;

        Driver(HttpTransportMode mode) {
            this.mode = mode;
        }
    }

    @ParameterizedTest
    @EnumSource(Driver.class)
    void suppliedValuesReachTheResponseOnTheFinalAttempt(Driver driver) {
        http.respond(503, null)
            .respond(SdkHttpResponse.builder().statusCode(200)
                                    .addHeader("x-amzn-RequestId", "req-1")
                                    .addHeader("x-amz-id-2", "ext-1")
                                    .addHeader("X-Amz-Cf-Id", "cf-1")
                                    .body(HttpBody.fromUtf8("v"))
                                    .build());

        MetaResponse output = (MetaResponse) call(driver, retries(3));

        assertThat(output.value).isEqualTo("v");
        SdkResponseMetadata metadata = output.sdkResponseMetadata();
        assertThat(metadata.requestId()).contains("req-1");
        assertThat(metadata.extendedRequestId()).contains("ext-1");
        assertThat(metadata.statusCode()).isEqualTo(OptionalInt.of(200));
        assertThat(metadata.attemptCount()).isEqualTo(OptionalInt.of(2));
        assertThat(metadata.errorCode()).isEmpty();
        assertThat(metadata.transportMetadata()).isEqualTo(TRANSPORT);
        // The V2-compatible views: the HTTP response without its body, and the raw metadata map.
        assertThat(output.sdkHttpResponse().statusCode()).isEqualTo(200);
        assertThat(output.sdkHttpResponse().headers().firstValue("x-amz-cf-id")).contains("cf-1");
        assertThat(output.sdkHttpResponse().body().kind()).isEqualTo(HttpBody.Kind.EMPTY);
        assertThat(output.responseMetadata().requestId()).isEqualTo("req-1");
    }

    @ParameterizedTest
    @EnumSource(Driver.class)
    void unsuppliedValuesAreTheDocumentedAbsentValues(Driver driver) {
        http.respond(200, "v");

        MetaResponse output = (MetaResponse) call(driver, retries(1));

        SdkResponseMetadata metadata = output.sdkResponseMetadata();
        assertThat(metadata.requestId()).isEqualTo(Optional.empty());
        assertThat(metadata.extendedRequestId()).isEqualTo(Optional.empty());
        assertThat(metadata.errorCode()).isEqualTo(Optional.empty());
        assertThat(metadata.statusCode()).isEqualTo(OptionalInt.of(200));
        assertThat(metadata.attemptCount()).isEqualTo(OptionalInt.of(1));
        assertThat(output.responseMetadata().requestId()).isEqualTo("UNKNOWN");
    }

    @ParameterizedTest
    @EnumSource(Driver.class)
    void aServiceErrorCarriesItsCodeStatusAndTheCallsAttemptCount(Driver driver) {
        http.respond(503, null).respond(503, null).respond(503, null);

        Object failure = call(driver, retries(3));

        assertThat(failure).isInstanceOf(AwsServiceException.class);
        AwsServiceException error = (AwsServiceException) failure;
        assertThat(error.numAttempts()).isEqualTo(3);
        assertThat(error.getMessage()).contains("(SDK Attempt Count: 3)");
        SdkResponseMetadata metadata = error.sdkResponseMetadata();
        assertThat(metadata.statusCode()).isEqualTo(OptionalInt.of(503));
        assertThat(metadata.errorCode()).contains("Error503");
        assertThat(metadata.attemptCount()).isEqualTo(OptionalInt.of(3));
        assertThat(metadata.transportMetadata()).isEqualTo(TRANSPORT);
        assertThat(metadata.requestId()).isEmpty();
        assertThat(error.requestId()).isNull();
    }

    @ParameterizedTest
    @EnumSource(Driver.class)
    void aTransportFailureCarriesTheAttemptCountAndTransportFacts(Driver driver) {
        http.fail(HttpTransportException.create(HttpPhase.CONNECT, HttpFailureCategory.Kind.IO, "refused", null))
            .fail(HttpTransportException.create(HttpPhase.CONNECT, HttpFailureCategory.Kind.IO, "refused", null));

        Object failure = call(driver, retries(2));

        assertThat(failure).isInstanceOf(HttpTransportException.class);
        SdkException error = (SdkException) failure;
        assertThat(error.numAttempts()).isEqualTo(2);
        SdkResponseMetadata metadata = error.sdkResponseMetadata();
        assertThat(metadata.attemptCount()).isEqualTo(OptionalInt.of(2));
        assertThat(metadata.transportMetadata()).isEqualTo(TRANSPORT);
        assertThat(metadata.statusCode()).isEmpty();
        assertThat(metadata.requestId()).isEmpty();
    }

    @Test
    void onlyPlansWhoseOutputIsAnAwsResponseHaveTheMetadataFeature() {
        ClientRuntime runtime = runtime(Driver.SYNC, retries(1)).build();
        assertThat(runtime.plan(GET_META).stages().isActive(PipelineStage.RESPONSE_METADATA)).isTrue();
        assertThat(runtime.plan(GET_THING).stages().isActive(PipelineStage.RESPONSE_METADATA)).isFalse();
    }

    /** Runs one call of {@link #GET_META}; its output, or the failure it surfaced. */
    private Object call(Driver driver, RetryStrategy strategy) {
        ClientRuntime runtime = runtime(driver, strategy).build();
        AwsCallEngine.Builder builder = AwsCallEngine.builder().runtime(runtime);
        AwsCallEngine engine = (driver == Driver.SYNC ? builder.syncHttpClient(http)
                                                      : builder.asyncHttpClient(http)).build();
        PipelinePlan<Struct, MetaResponse> plan = runtime.plan(GET_META);
        Struct request = PipelineFixtures.request(GET_INPUT, "a");
        try {
            if (driver == Driver.SYNC) {
                return engine.executeSync(plan, request, null, null);
            }
            return engine.executeAsync(plan, request, null, null).get(5, TimeUnit.SECONDS);
        } catch (ExecutionException failure) {
            return failure.getCause();
        } catch (Exception failure) {
            return failure;
        }
    }

    private static ClientRuntime.Builder runtime(Driver driver, RetryStrategy strategy) {
        ClientRuntime.Builder runtime = PipelineFixtures.engineRuntime(strategy, driver.mode);
        if (driver == Driver.ASYNC) {
            runtime.completionExecutor(Runnable::run).retryScheduler(new PipelineFixtures.ManualScheduler());
        }
        return runtime;
    }

    private static AwsOperation<Struct, MetaResponse> metaOperation() {
        Schema operation = Schema.createOperation(ShapeId.from("com.example#GetMeta"));
        OperationSchemas schemas = OperationSchemas.builder(operation).input(GET_INPUT).output(GET_OUTPUT).build();
        AwsOperationDescriptor descriptor = AwsOperationDescriptor.builder()
                                                                  .id(operation.id())
                                                                  .service(SERVICE)
                                                                  .input(GET_INPUT.id())
                                                                  .output(GET_OUTPUT.id())
                                                                  .schemas(schemas)
                                                                  .build();
        return AwsOperation.builder(descriptor, InputBuilder::new, MetaResponse::builder).build();
    }

    /** A generated-style operation output: an {@link AwsResponse} with one {@code Value} member. */
    static final class MetaResponse extends AwsResponse implements SerializableStruct {
        private final String value;

        private MetaResponse(MetaBuilder builder) {
            super(builder);
            this.value = builder.value;
        }

        static MetaBuilder builder() {
            return new MetaBuilder();
        }

        @Override
        public MetaBuilder toBuilder() {
            return new MetaBuilder(this);
        }

        @Override
        public Schema schema() {
            return GET_OUTPUT;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            throw new UnsupportedOperationException();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getMemberValue(Schema member) {
            return (T) value;
        }

        static final class MetaBuilder extends AwsResponse.BuilderImpl implements ShapeBuilder<MetaResponse> {
            private String value;

            private MetaBuilder() {
            }

            private MetaBuilder(MetaResponse response) {
                super(response);
                this.value = response.value;
            }

            @Override
            public MetaResponse build() {
                return new MetaResponse(this);
            }

            @Override
            public ShapeBuilder<MetaResponse> deserialize(ShapeDeserializer decoder) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void setMemberValue(Schema member, Object memberValue) {
                this.value = (String) memberValue;
            }

            @Override
            public Schema schema() {
                return GET_OUTPUT;
            }
        }
    }

    /** The input builder of {@link #GET_META}. */
    private static final class InputBuilder implements ShapeBuilder<Struct> {
        private final Map<String, Object> values = new HashMap<>();

        @Override
        public Struct build() {
            return new Struct(GET_INPUT, values);
        }

        @Override
        public ShapeBuilder<Struct> deserialize(ShapeDeserializer decoder) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setMemberValue(Schema member, Object value) {
            values.put(member.memberName(), value);
        }

        @Override
        public Schema schema() {
            return GET_INPUT;
        }
    }
}
