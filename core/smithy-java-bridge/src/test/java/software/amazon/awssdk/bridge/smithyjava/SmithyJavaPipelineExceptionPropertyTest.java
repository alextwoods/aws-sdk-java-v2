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

package software.amazon.awssdk.bridge.smithyjava;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.concurrent.ConcurrentMap;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.smithy.java.core.error.CallException;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.SerializableStruct;

/**
 * Property-based tests for {@link SmithyJavaPipeline} exception mapping logic,
 * covering CallException unwrapping (Property 11) and non-SDK CallException mapping (Property 12).
 *
 * <p>Since {@code SmithyJavaPipeline} is package-private, these tests reside in the same package.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class SmithyJavaPipelineExceptionPropertyTest {

    // ========================================================================================
    // Property 11: CallException Unwrapping (Task 9.6)
    // ========================================================================================

    /**
     * <b>Property 11: CallException Unwrapping</b>
     *
     * <p>For any CallException whose getCause() is an instance of SdkServiceException (or a subtype
     * including AwsServiceException), the SmithyJavaPipeline SHALL throw the cause directly
     * without additional wrapping.</p>
     *
     * <p><b>Validates: Requirements 9.1, 9.2</b></p>
     */
    @Property(tries = 100)
    void callExceptionWithSdkServiceExceptionCause_throwsCauseDirectly(
            @ForAll("sdkServiceExceptions") SdkServiceException serviceCause) {

        CallException callException = new CallException(serviceCause.getMessage(), serviceCause);
        SmithyJavaPipeline pipeline = buildPipelineWithMockedClientThrowing(callException);

        ClientExecutionParams params = createExecutionParamsWithSerializableInput("TestOp");

        Throwable thrown = catchThrowable(() -> pipeline.execute(params, null));

        // The cause should be thrown directly — no wrapping
        assertThat(thrown).isSameAs(serviceCause);
    }

    /**
     * Variant: AwsServiceException subtypes (with requestId, statusCode) are also unwrapped directly.
     *
     * <p><b>Validates: Requirements 9.1, 9.2</b></p>
     */
    @Property(tries = 100)
    void callExceptionWithAwsServiceExceptionCause_throwsCauseDirectly(
            @ForAll("awsServiceExceptions") AwsServiceException awsCause) {

        CallException callException = new CallException(awsCause.getMessage(), awsCause);
        SmithyJavaPipeline pipeline = buildPipelineWithMockedClientThrowing(callException);

        ClientExecutionParams params = createExecutionParamsWithSerializableInput("TestOp");

        Throwable thrown = catchThrowable(() -> pipeline.execute(params, null));

        assertThat(thrown).isSameAs(awsCause);
        // Confirm it preserves the concrete type
        assertThat(thrown).isInstanceOf(AwsServiceException.class);
    }

    /**
     * Generates SdkServiceException instances (base type) with random messages and status codes.
     */
    @Provide
    Arbitrary<SdkServiceException> sdkServiceExceptions() {
        Arbitrary<String> messages = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(50);
        Arbitrary<Integer> statusCodes = Arbitraries.integers().between(400, 599);

        Arbitrary<SdkServiceException> baseSdkServiceExceptions = Combinators.combine(messages, statusCodes)
                .as((msg, status) -> (SdkServiceException) SdkServiceException.builder()
                        .message(msg)
                        .statusCode(status)
                        .build());

        Arbitrary<SdkServiceException> awsServiceExceptions = Combinators.combine(messages, statusCodes)
                .as((msg, status) -> (SdkServiceException) AwsServiceException.builder()
                        .message(msg)
                        .statusCode(status)
                        .build());

        return Arbitraries.oneOf(baseSdkServiceExceptions, awsServiceExceptions);
    }

    /**
     * Generates AwsServiceException instances with random messages, status codes, and request IDs.
     */
    @Provide
    Arbitrary<AwsServiceException> awsServiceExceptions() {
        Arbitrary<String> messages = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(50);
        Arbitrary<Integer> statusCodes = Arbitraries.integers().between(400, 599);
        Arbitrary<String> requestIds = Arbitraries.strings()
                .withCharRange('a', 'f')
                .withCharRange('0', '9')
                .ofMinLength(10)
                .ofMaxLength(36);

        return Combinators.combine(messages, statusCodes, requestIds)
                .as((msg, status, reqId) -> (AwsServiceException) AwsServiceException.builder()
                        .message(msg)
                        .statusCode(status)
                        .requestId(reqId)
                        .build());
    }

    // ========================================================================================
    // Property 12: Non-SDK CallException Mapping (Task 9.7)
    // ========================================================================================

    /**
     * <b>Property 12: Non-SDK CallException Mapping</b>
     *
     * <p>For any CallException whose getCause() is NOT an instance of SdkServiceException,
     * the SmithyJavaPipeline SHALL throw an SdkServiceException whose message() equals the
     * CallException message and whose cause() is the CallException itself.</p>
     *
     * <p><b>Validates: Requirements 9.3</b></p>
     */
    @Property(tries = 100)
    void callExceptionWithNonSdkCause_throwsSdkServiceExceptionWithCorrectMessageAndCause(
            @ForAll("nonSdkCallExceptions") CallExceptionWithNonSdkCause scenario) {

        SmithyJavaPipeline pipeline = buildPipelineWithMockedClientThrowing(scenario.callException);

        ClientExecutionParams params = createExecutionParamsWithSerializableInput("TestOp");

        Throwable thrown = catchThrowable(() -> pipeline.execute(params, null));

        // Must be an SdkServiceException (the generic wrapper)
        assertThat(thrown).isInstanceOf(SdkServiceException.class);
        // Message must match the CallException's message
        assertThat(thrown.getMessage()).isEqualTo(scenario.callException.getMessage());
        // Cause must be the CallException itself
        assertThat(thrown.getCause()).isSameAs(scenario.callException);
    }

    /**
     * Additional check: the non-SDK cause is preserved transitively
     * (thrown.getCause().getCause() == original non-SDK cause).
     *
     * <p><b>Validates: Requirements 9.3</b></p>
     */
    @Property(tries = 100)
    void callExceptionWithNonSdkCause_preservesOriginalCauseTransitively(
            @ForAll("nonSdkCallExceptions") CallExceptionWithNonSdkCause scenario) {

        SmithyJavaPipeline pipeline = buildPipelineWithMockedClientThrowing(scenario.callException);

        ClientExecutionParams params = createExecutionParamsWithSerializableInput("TestOp");

        Throwable thrown = catchThrowable(() -> pipeline.execute(params, null));

        // The wrapping chain: thrown (SdkServiceException) -> cause (CallException) -> cause (original)
        assertThat(thrown.getCause()).isSameAs(scenario.callException);
        assertThat(thrown.getCause().getCause()).isSameAs(scenario.originalCause);
    }

    /**
     * Generates CallException instances wrapping various non-SdkServiceException causes.
     */
    @Provide
    Arbitrary<CallExceptionWithNonSdkCause> nonSdkCallExceptions() {
        Arbitrary<String> messages = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(50);

        Arbitrary<Throwable> causes = Arbitraries.oneOf(
                messages.map(msg -> (Throwable) new RuntimeException(msg)),
                messages.map(msg -> (Throwable) new IllegalStateException(msg)),
                messages.map(msg -> (Throwable) new NullPointerException(msg)),
                messages.map(msg -> (Throwable) new IllegalArgumentException(msg)),
                messages.map(msg -> (Throwable) new java.io.IOException(msg)),
                messages.map(msg -> (Throwable) new UnsupportedOperationException(msg))
        );

        return Combinators.combine(messages, causes)
                .as((msg, cause) -> {
                    CallException callEx = new CallException(msg, cause);
                    return new CallExceptionWithNonSdkCause(callEx, cause);
                });
    }

    // ========================================================================================
    // Helper methods
    // ========================================================================================

    /**
     * Builds a SmithyJavaPipeline with a mocked BridgeClient that throws the given exception
     * when callOperation is invoked.
     */
    private SmithyJavaPipeline buildPipelineWithMockedClientThrowing(RuntimeException exception) {
        SmithyJavaPipeline pipeline = buildMinimalPipeline();

        try {
            SmithyJavaPipeline.BridgeClient mockClient = mock(SmithyJavaPipeline.BridgeClient.class);
            doThrow(exception).when(mockClient).callOperation(any(), any(), any());

            Field smithyClientField = SmithyJavaPipeline.class.getDeclaredField("smithyClient");
            smithyClientField.setAccessible(true);
            smithyClientField.set(pipeline, mockClient);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock client", e);
        }

        // Replace the protocolResolver field with a mock that returns a dummy protocol
        try {
            software.amazon.awssdk.bridge.smithyjava.protocol.ProtocolResolver mockResolver =
                mock(software.amazon.awssdk.bridge.smithyjava.protocol.ProtocolResolver.class);
            software.amazon.smithy.java.client.core.ClientProtocol mockProtocol =
                mock(software.amazon.smithy.java.client.core.ClientProtocol.class);
            org.mockito.Mockito.doReturn(mockProtocol).when(mockResolver).resolve(any());

            Field resolverField = SmithyJavaPipeline.class.getDeclaredField("protocolResolver");
            resolverField.setAccessible(true);
            resolverField.set(pipeline, mockResolver);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock protocol resolver", e);
        }

        // Pre-populate the operation cache so resolveOperation succeeds
        try {
            Field cacheField = SmithyJavaPipeline.class.getDeclaredField("operationCache");
            cacheField.setAccessible(true);
            ConcurrentMap<String, ApiOperation<SerializableStruct, SerializableStruct>> cache =
                    (ConcurrentMap<String, ApiOperation<SerializableStruct, SerializableStruct>>)
                            cacheField.get(pipeline);

            ApiOperation mockOp = mock(ApiOperation.class);
            cache.put(SdkRequest.class.getPackageName() + "#TestOp", mockOp);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject operation cache", e);
        }

        return pipeline;
    }

    /**
     * Builds a minimal SmithyJavaPipeline with the minimum required configuration.
     */
    private SmithyJavaPipeline buildMinimalPipeline() {
        SdkHttpClient httpClient = mock(SdkHttpClient.class);

        SdkClientConfiguration config = SdkClientConfiguration.builder()
                .option(SdkClientOption.SYNC_HTTP_CLIENT, httpClient)
                .option(AwsClientOption.CREDENTIALS_PROVIDER,
                        () -> AwsBasicCredentials.create("testKey", "testSecret"))
                .option(AwsClientOption.AWS_REGION, Region.US_EAST_1)
                .option(AwsClientOption.SERVICE_SIGNING_NAME, "test-service")
                .option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER,
                        ClientEndpointProvider.forEndpointOverride(URI.create("https://test.amazonaws.com")))
                .build();

        return new SmithyJavaPipeline(config);
    }

    /**
     * Creates a ClientExecutionParams with a mock input that implements both SdkRequest
     * and SerializableStruct (simulating a generated SDK request class).
     */
    private ClientExecutionParams createExecutionParamsWithSerializableInput(String operationName) {
        SdkRequest input = mock(SdkRequest.class,
                org.mockito.Mockito.withSettings().extraInterfaces(SerializableStruct.class));

        return new ClientExecutionParams()
                .withInput(input)
                .withOperationName(operationName);
    }

    // ========================================================================================
    // Data classes
    // ========================================================================================

    /**
     * Holds a CallException wrapping a non-SDK cause, along with the original cause for assertions.
     */
    static final class CallExceptionWithNonSdkCause {
        final CallException callException;
        final Throwable originalCause;

        CallExceptionWithNonSdkCause(CallException callException, Throwable originalCause) {
            this.callException = callException;
            this.originalCause = originalCause;
        }

        @Override
        public String toString() {
            return "CallExceptionWithNonSdkCause{message='" + callException.getMessage()
                   + "', cause=" + originalCause.getClass().getSimpleName() + "}";
        }
    }
}
