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

package software.amazon.awssdk.bridge.smithyjava.interceptors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.net.URI;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.smithy.java.client.core.interceptors.InputHook;
import software.amazon.smithy.java.client.core.interceptors.OutputHook;
import software.amazon.smithy.java.client.core.interceptors.RequestHook;
import software.amazon.smithy.java.client.core.interceptors.ResponseHook;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.http.api.HttpHeaders;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.java.http.api.HttpResponse;
import software.amazon.smithy.java.http.api.HttpVersion;
import software.amazon.smithy.java.http.api.ModifiableHttpRequest;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Unit tests for {@link FullV2InterceptorBridge} verifying all 13 hook mappings,
 * shared ExecutionAttributes, exception propagation, and modify hook mutations.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class FullV2InterceptorBridgeTest {

    private static final String OPERATION_NAME = "GetItem";
    private static final ShapeId OPERATION_SHAPE_ID = ShapeId.from("com.amazonaws.dynamodb#" + OPERATION_NAME);
    private static final Schema OPERATION_SCHEMA = Schema.createOperation(OPERATION_SHAPE_ID);

    private ExecutionInterceptor v2Interceptor;
    private FullV2InterceptorBridge bridge;
    private ApiOperation mockOperation;

    @BeforeEach
    void setUp() {
        v2Interceptor = mock(ExecutionInterceptor.class);
        bridge = new FullV2InterceptorBridge(v2Interceptor);
        mockOperation = mock(ApiOperation.class);
        when(mockOperation.schema()).thenReturn(OPERATION_SCHEMA);
    }

    // ========== 1. readBeforeExecution calls v2 beforeExecution with correct context ==========

    @Test
    void readBeforeExecution_whenCalled_callsV2BeforeExecutionWithCorrectRequest() {
        SdkRequest sdkRequest = mockSdkRequest();
        InputHook hook = createInputHook(sdkRequest);

        bridge.readBeforeExecution(hook);

        ArgumentCaptor<Context.BeforeExecution> contextCaptor = ArgumentCaptor.forClass(Context.BeforeExecution.class);
        verify(v2Interceptor).beforeExecution(contextCaptor.capture(), any(ExecutionAttributes.class));
        assertThat(contextCaptor.getValue().request()).isSameAs(sdkRequest);
    }

    // ========== 2. modifyBeforeSerialization calls v2 modifyRequest and returns modified request ==========

    @Test
    void modifyBeforeSerialization_whenV2ReturnsModifiedRequest_returnsThatModifiedRequest() {
        SdkRequest originalRequest = mockSdkRequest();
        SdkRequest modifiedRequest = mockSdkRequest();

        when(v2Interceptor.modifyRequest(any(Context.ModifyRequest.class), any(ExecutionAttributes.class)))
            .thenReturn(modifiedRequest);

        InputHook hook = createInputHook(originalRequest);

        Object result = bridge.modifyBeforeSerialization(hook);

        assertThat(result).isSameAs(modifiedRequest);
    }

    @Test
    void modifyBeforeSerialization_whenCalled_passesOriginalRequestInContext() {
        SdkRequest originalRequest = mockSdkRequest();
        when(v2Interceptor.modifyRequest(any(Context.ModifyRequest.class), any(ExecutionAttributes.class)))
            .thenReturn(originalRequest);

        InputHook hook = createInputHook(originalRequest);

        bridge.modifyBeforeSerialization(hook);

        ArgumentCaptor<Context.ModifyRequest> contextCaptor = ArgumentCaptor.forClass(Context.ModifyRequest.class);
        verify(v2Interceptor).modifyRequest(contextCaptor.capture(), any(ExecutionAttributes.class));
        assertThat(contextCaptor.getValue().request()).isSameAs(originalRequest);
    }

    // ========== 3. readBeforeSerialization calls v2 beforeMarshalling ==========

    @Test
    void readBeforeSerialization_whenCalled_callsV2BeforeMarshallingWithCorrectRequest() {
        SdkRequest sdkRequest = mockSdkRequest();
        InputHook hook = createInputHook(sdkRequest);

        bridge.readBeforeSerialization(hook);

        ArgumentCaptor<Context.BeforeMarshalling> contextCaptor =
            ArgumentCaptor.forClass(Context.BeforeMarshalling.class);
        verify(v2Interceptor).beforeMarshalling(contextCaptor.capture(), any(ExecutionAttributes.class));
        assertThat(contextCaptor.getValue().request()).isSameAs(sdkRequest);
    }

    // ========== 4. readAfterSerialization calls v2 afterMarshalling with SdkHttpRequest ==========

    @Test
    void readAfterSerialization_whenCalled_callsV2AfterMarshallingWithSdkHttpRequest() {
        SdkRequest sdkRequest = mockSdkRequest();
        HttpRequest httpRequest = createHttpRequest();
        RequestHook hook = createRequestHook(sdkRequest, httpRequest);

        bridge.readAfterSerialization(hook);

        ArgumentCaptor<Context.AfterMarshalling> contextCaptor =
            ArgumentCaptor.forClass(Context.AfterMarshalling.class);
        verify(v2Interceptor).afterMarshalling(contextCaptor.capture(), any(ExecutionAttributes.class));
        assertThat(contextCaptor.getValue().request()).isSameAs(sdkRequest);
        assertThat(contextCaptor.getValue().httpRequest()).isNotNull();
        assertThat(contextCaptor.getValue().httpRequest().getUri())
            .isEqualTo(URI.create("https://example.amazonaws.com/"));
    }

    // ========== 5. modifyBeforeSigning calls v2 modifyHttpRequest and returns modified request ==========

    @Test
    void modifyBeforeSigning_whenCalled_callsV2ModifyHttpRequest() {
        SdkRequest sdkRequest = mockSdkRequest();
        HttpRequest httpRequest = createHttpRequest();
        RequestHook hook = createRequestHook(sdkRequest, httpRequest);

        when(v2Interceptor.modifyHttpRequest(any(Context.ModifyHttpRequest.class), any(ExecutionAttributes.class)))
            .thenAnswer(invocation -> {
                Context.ModifyHttpRequest ctx = invocation.getArgument(0);
                return ctx.httpRequest();
            });

        bridge.modifyBeforeSigning(hook);

        verify(v2Interceptor).modifyHttpRequest(any(Context.ModifyHttpRequest.class), any(ExecutionAttributes.class));
    }

    @Test
    void modifyBeforeSigning_whenV2ModifiesRequest_returnsModifiedHttpRequest() {
        SdkRequest sdkRequest = mockSdkRequest();
        HttpRequest httpRequest = createHttpRequest();
        RequestHook hook = createRequestHook(sdkRequest, httpRequest);

        when(v2Interceptor.modifyHttpRequest(any(Context.ModifyHttpRequest.class), any(ExecutionAttributes.class)))
            .thenAnswer(invocation -> {
                Context.ModifyHttpRequest ctx = invocation.getArgument(0);
                return ctx.httpRequest().toBuilder()
                    .putHeader("X-Custom-Header", "custom-value")
                    .build();
            });

        Object result = bridge.modifyBeforeSigning(hook);

        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(HttpRequest.class);
    }

    // ========== 6. readBeforeTransmit calls v2 beforeTransmission ==========

    @Test
    void readBeforeTransmit_withHttpRequest_callsV2BeforeTransmission() {
        SdkRequest sdkRequest = mockSdkRequest();
        HttpRequest httpRequest = createHttpRequest();
        RequestHook hook = createRequestHook(sdkRequest, httpRequest);

        bridge.readBeforeTransmit(hook);

        ArgumentCaptor<Context.BeforeTransmission> contextCaptor =
            ArgumentCaptor.forClass(Context.BeforeTransmission.class);
        verify(v2Interceptor).beforeTransmission(contextCaptor.capture(), any(ExecutionAttributes.class));
        assertThat(contextCaptor.getValue().request()).isSameAs(sdkRequest);
        assertThat(contextCaptor.getValue().httpRequest()).isNotNull();
    }

    @Test
    void readBeforeTransmit_withNonHttpRequest_doesNotCallV2BeforeTransmission() {
        SdkRequest sdkRequest = mockSdkRequest();
        // Use a non-HttpRequest object as the request type
        Object nonHttpRequest = "not-an-http-request";
        RequestHook hook = new RequestHook(mockOperation,
            software.amazon.smithy.java.context.Context.create(), (SerializableStruct) (Object) sdkRequest, nonHttpRequest);

        bridge.readBeforeTransmit(hook);

        verify(v2Interceptor, never()).beforeTransmission(any(), any());
    }

    // ========== 7. readAfterTransmit calls v2 afterTransmission with response ==========

    @Test
    void readAfterTransmit_withHttpRequestAndResponse_callsV2AfterTransmission() {
        SdkRequest sdkRequest = mockSdkRequest();
        HttpRequest httpRequest = createHttpRequest();
        HttpResponse httpResponse = createHttpResponse(200);
        ResponseHook hook = createResponseHook(sdkRequest, httpRequest, httpResponse);

        bridge.readAfterTransmit(hook);

        ArgumentCaptor<Context.AfterTransmission> contextCaptor =
            ArgumentCaptor.forClass(Context.AfterTransmission.class);
        verify(v2Interceptor).afterTransmission(contextCaptor.capture(), any(ExecutionAttributes.class));
        assertThat(contextCaptor.getValue().request()).isSameAs(sdkRequest);
        assertThat(contextCaptor.getValue().httpRequest()).isNotNull();
        assertThat(contextCaptor.getValue().httpResponse()).isNotNull();
        assertThat(contextCaptor.getValue().httpResponse().statusCode()).isEqualTo(200);
    }

    // ========== 8. modifyBeforeDeserialization calls v2 modifyHttpResponse ==========

    @Test
    void modifyBeforeDeserialization_whenCalled_callsV2ModifyHttpResponse() {
        SdkRequest sdkRequest = mockSdkRequest();
        HttpRequest httpRequest = createHttpRequest();
        HttpResponse httpResponse = createHttpResponse(200);
        ResponseHook hook = createResponseHook(sdkRequest, httpRequest, httpResponse);

        when(v2Interceptor.modifyHttpResponse(any(Context.ModifyHttpResponse.class), any(ExecutionAttributes.class)))
            .thenAnswer(invocation -> {
                Context.ModifyHttpResponse ctx = invocation.getArgument(0);
                return ctx.httpResponse();
            });

        bridge.modifyBeforeDeserialization(hook);

        verify(v2Interceptor).modifyHttpResponse(any(Context.ModifyHttpResponse.class), any(ExecutionAttributes.class));
    }

    // ========== 9. readBeforeDeserialization calls v2 beforeUnmarshalling ==========

    @Test
    void readBeforeDeserialization_whenCalled_callsV2BeforeUnmarshalling() {
        SdkRequest sdkRequest = mockSdkRequest();
        HttpRequest httpRequest = createHttpRequest();
        HttpResponse httpResponse = createHttpResponse(200);
        ResponseHook hook = createResponseHook(sdkRequest, httpRequest, httpResponse);

        bridge.readBeforeDeserialization(hook);

        verify(v2Interceptor).beforeUnmarshalling(any(Context.BeforeUnmarshalling.class), any(ExecutionAttributes.class));
    }

    // ========== 10. readAfterDeserialization calls v2 afterUnmarshalling ==========

    @Test
    void readAfterDeserialization_whenCalled_callsV2AfterUnmarshalling() {
        SdkRequest sdkRequest = mockSdkRequest();
        SdkResponse sdkResponse = mockSdkResponseAsSerializableStruct();
        HttpRequest httpRequest = createHttpRequest();
        HttpResponse httpResponse = createHttpResponse(200);
        OutputHook hook = createOutputHook(sdkRequest, httpRequest, httpResponse, sdkResponse);

        bridge.readAfterDeserialization(hook, null);

        verify(v2Interceptor).afterUnmarshalling(any(Context.AfterUnmarshalling.class), any(ExecutionAttributes.class));
    }

    // ========== 11. modifyBeforeCompletion calls v2 modifyResponse on success ==========

    @Test
    void modifyBeforeCompletion_onSuccess_callsV2ModifyResponse() {
        SdkRequest sdkRequest = mockSdkRequest();
        SdkResponse sdkResponse = mockSdkResponseAsSerializableStruct();
        HttpRequest httpRequest = createHttpRequest();
        HttpResponse httpResponse = createHttpResponse(200);
        OutputHook hook = createOutputHook(sdkRequest, httpRequest, httpResponse, sdkResponse);

        when(v2Interceptor.modifyResponse(any(Context.ModifyResponse.class), any(ExecutionAttributes.class)))
            .thenReturn(sdkResponse);

        bridge.modifyBeforeCompletion(hook, null);

        verify(v2Interceptor).modifyResponse(any(Context.ModifyResponse.class), any(ExecutionAttributes.class));
    }

    @Test
    void modifyBeforeCompletion_onError_doesNotCallV2ModifyResponse() {
        SdkRequest sdkRequest = mockSdkRequest();
        SdkResponse sdkResponse = mockSdkResponseAsSerializableStruct();
        HttpRequest httpRequest = createHttpRequest();
        HttpResponse httpResponse = createHttpResponse(500);
        OutputHook hook = createOutputHook(sdkRequest, httpRequest, httpResponse, sdkResponse);

        RuntimeException error = new RuntimeException("service error");

        // When an error is present, the bridge calls hook.forward(error) which throws the error
        assertThatThrownBy(() -> bridge.modifyBeforeCompletion(hook, error))
            .isSameAs(error);

        verify(v2Interceptor, never()).modifyResponse(any(), any());
    }

    @Test
    void modifyBeforeCompletion_whenV2ReturnsModifiedResponse_returnsThatResponse() {
        SdkRequest sdkRequest = mockSdkRequest();
        SdkResponse originalResponse = mockSdkResponseAsSerializableStruct();
        SdkResponse modifiedResponse = mockSdkResponseAsSerializableStruct();
        HttpRequest httpRequest = createHttpRequest();
        HttpResponse httpResponse = createHttpResponse(200);
        OutputHook hook = createOutputHook(sdkRequest, httpRequest, httpResponse, originalResponse);

        when(v2Interceptor.modifyResponse(any(Context.ModifyResponse.class), any(ExecutionAttributes.class)))
            .thenReturn(modifiedResponse);

        Object result = bridge.modifyBeforeCompletion(hook, null);

        // The modified response should be returned (via asOutputType)
        assertThat(result).isSameAs(modifiedResponse);
    }

    // ========== 12. readAfterExecution calls v2 afterExecution on success, onExecutionFailure on error ==========

    @Test
    void readAfterExecution_onSuccess_callsV2AfterExecution() {
        SdkRequest sdkRequest = mockSdkRequest();
        SdkResponse sdkResponse = mockSdkResponseAsSerializableStruct();
        HttpRequest httpRequest = createHttpRequest();
        HttpResponse httpResponse = createHttpResponse(200);
        OutputHook hook = createOutputHook(sdkRequest, httpRequest, httpResponse, sdkResponse);

        bridge.readAfterExecution(hook, null);

        verify(v2Interceptor).afterExecution(any(Context.AfterExecution.class), any(ExecutionAttributes.class));
        verify(v2Interceptor, never()).onExecutionFailure(any(), any());
    }

    @Test
    void readAfterExecution_onError_callsV2OnExecutionFailure() {
        SdkRequest sdkRequest = mockSdkRequest();
        SdkResponse sdkResponse = mockSdkResponseAsSerializableStruct();
        HttpRequest httpRequest = createHttpRequest();
        HttpResponse httpResponse = createHttpResponse(500);
        OutputHook hook = createOutputHook(sdkRequest, httpRequest, httpResponse, sdkResponse);

        RuntimeException error = new RuntimeException("request failed");
        bridge.readAfterExecution(hook, error);

        verify(v2Interceptor).onExecutionFailure(any(Context.FailedExecution.class), any(ExecutionAttributes.class));
        verify(v2Interceptor, never()).afterExecution(any(), any());
    }

    @Test
    void readAfterExecution_onError_passesExceptionInFailedContext() {
        SdkRequest sdkRequest = mockSdkRequest();
        SdkResponse sdkResponse = mockSdkResponseAsSerializableStruct();
        HttpRequest httpRequest = createHttpRequest();
        HttpResponse httpResponse = createHttpResponse(500);
        OutputHook hook = createOutputHook(sdkRequest, httpRequest, httpResponse, sdkResponse);

        RuntimeException error = new RuntimeException("request failed");
        bridge.readAfterExecution(hook, error);

        ArgumentCaptor<Context.FailedExecution> captor = ArgumentCaptor.forClass(Context.FailedExecution.class);
        verify(v2Interceptor).onExecutionFailure(captor.capture(), any(ExecutionAttributes.class));
        assertThat(captor.getValue().exception()).isSameAs(error);
    }

    // ========== 13. modifyBeforeAttemptCompletion calls v2 modifyException on error ==========

    @Test
    void modifyBeforeAttemptCompletion_onError_callsV2ModifyException() {
        SdkRequest sdkRequest = mockSdkRequest();
        SdkResponse sdkResponse = mockSdkResponseAsSerializableStruct();
        HttpRequest httpRequest = createHttpRequest();
        HttpResponse httpResponse = createHttpResponse(500);
        OutputHook hook = createOutputHook(sdkRequest, httpRequest, httpResponse, sdkResponse);

        RuntimeException originalError = new RuntimeException("original");
        RuntimeException modifiedError = new RuntimeException("modified");

        when(v2Interceptor.modifyException(any(Context.FailedExecution.class), any(ExecutionAttributes.class)))
            .thenReturn(modifiedError);

        // modifyBeforeAttemptCompletion calls hook.forward(modifiedError) which throws the error
        assertThatThrownBy(() -> bridge.modifyBeforeAttemptCompletion(hook, originalError))
            .isSameAs(modifiedError);

        verify(v2Interceptor).modifyException(any(Context.FailedExecution.class), any(ExecutionAttributes.class));
    }

    @Test
    void modifyBeforeAttemptCompletion_withNullError_doesNotCallV2ModifyException() {
        SdkRequest sdkRequest = mockSdkRequest();
        SdkResponse sdkResponse = mockSdkResponseAsSerializableStruct();
        HttpRequest httpRequest = createHttpRequest();
        HttpResponse httpResponse = createHttpResponse(200);
        OutputHook hook = createOutputHook(sdkRequest, httpRequest, httpResponse, sdkResponse);

        // With null error, the bridge should just forward (no modifyException call)
        bridge.modifyBeforeAttemptCompletion(hook, null);

        verify(v2Interceptor, never()).modifyException(any(), any());
    }

    // ========== 14. ExecutionAttributes populated with operation name ==========

    @Test
    void readBeforeExecution_whenCalled_populatesOperationNameInExecutionAttributes() {
        SdkRequest sdkRequest = mockSdkRequest();
        InputHook hook = createInputHook(sdkRequest);

        bridge.readBeforeExecution(hook);

        ExecutionAttributes attrs = bridge.executionAttributes();
        assertThat(attrs.getAttribute(SdkExecutionAttribute.OPERATION_NAME)).isEqualTo(OPERATION_NAME);
    }

    @Test
    void executionAttributes_sharedAcrossHooks_subsequentHooksSeeSameAttributes() {
        // Call readBeforeExecution to populate the operation name
        SdkRequest sdkRequest = mockSdkRequest();
        InputHook inputHook = createInputHook(sdkRequest);
        bridge.readBeforeExecution(inputHook);

        // Then call readBeforeSerialization - it should receive the same ExecutionAttributes
        bridge.readBeforeSerialization(inputHook);

        ArgumentCaptor<ExecutionAttributes> attrCaptor = ArgumentCaptor.forClass(ExecutionAttributes.class);
        verify(v2Interceptor).beforeMarshalling(any(), attrCaptor.capture());
        assertThat(attrCaptor.getValue().getAttribute(SdkExecutionAttribute.OPERATION_NAME))
            .isEqualTo(OPERATION_NAME);
    }

    // ========== 15. Exception from v2 interceptor propagates unwrapped ==========

    @Test
    void readBeforeExecution_whenV2InterceptorThrows_propagatesExactException() {
        RuntimeException originalException = new IllegalStateException("interceptor failed");
        doThrow(originalException).when(v2Interceptor).beforeExecution(any(), any());

        SdkRequest sdkRequest = mockSdkRequest();
        InputHook hook = createInputHook(sdkRequest);

        assertThatThrownBy(() -> bridge.readBeforeExecution(hook))
            .isSameAs(originalException);
    }

    @Test
    void modifyBeforeSerialization_whenV2InterceptorThrows_propagatesExactException() {
        RuntimeException originalException = new UnsupportedOperationException("cannot modify");
        when(v2Interceptor.modifyRequest(any(), any())).thenThrow(originalException);

        InputHook hook = createInputHook(mockSdkRequest());

        assertThatThrownBy(() -> bridge.modifyBeforeSerialization(hook))
            .isSameAs(originalException);
    }

    @Test
    void readBeforeTransmit_whenV2InterceptorThrows_propagatesExactException() {
        RuntimeException originalException = new SecurityException("forbidden");
        doThrow(originalException).when(v2Interceptor).beforeTransmission(any(), any());

        SdkRequest sdkRequest = mockSdkRequest();
        HttpRequest httpRequest = createHttpRequest();
        RequestHook hook = createRequestHook(sdkRequest, httpRequest);

        assertThatThrownBy(() -> bridge.readBeforeTransmit(hook))
            .isSameAs(originalException);
    }

    @Test
    void readAfterExecution_whenV2InterceptorThrowsOnSuccess_propagatesExactException() {
        RuntimeException originalException = new RuntimeException("afterExecution blew up");
        doThrow(originalException).when(v2Interceptor).afterExecution(any(), any());

        SdkRequest sdkRequest = mockSdkRequest();
        SdkResponse sdkResponse = mockSdkResponseAsSerializableStruct();
        HttpRequest httpRequest = createHttpRequest();
        HttpResponse httpResponse = createHttpResponse(200);
        OutputHook hook = createOutputHook(sdkRequest, httpRequest, httpResponse, sdkResponse);

        assertThatThrownBy(() -> bridge.readAfterExecution(hook, null))
            .isSameAs(originalException);
    }

    // ========== Helper methods ==========

    /**
     * Creates a mock SdkRequest that also implements SerializableStruct,
     * which is needed because InputHook.input() requires SerializableStruct.
     */
    private SdkRequest mockSdkRequest() {
        return mock(SdkRequest.class, withSettings().extraInterfaces(SerializableStruct.class));
    }

    /**
     * Creates a mock SdkResponse that also implements SerializableStruct.
     */
    private SdkResponse mockSdkResponseAsSerializableStruct() {
        return mock(SdkResponse.class, withSettings().extraInterfaces(SerializableStruct.class));
    }

    /**
     * Creates a real InputHook using its constructor with the mock ApiOperation.
     */
    private InputHook createInputHook(SdkRequest sdkRequest) {
        return new InputHook(mockOperation, software.amazon.smithy.java.context.Context.create(),
            (SerializableStruct) (Object) sdkRequest);
    }

    /**
     * Creates a real RequestHook with an HttpRequest.
     */
    private RequestHook createRequestHook(SdkRequest sdkRequest, HttpRequest httpRequest) {
        return new RequestHook(mockOperation, software.amazon.smithy.java.context.Context.create(),
            (SerializableStruct) (Object) sdkRequest, httpRequest);
    }

    /**
     * Creates a real ResponseHook with HttpRequest and HttpResponse.
     */
    private ResponseHook createResponseHook(SdkRequest sdkRequest, HttpRequest httpRequest,
                                             HttpResponse httpResponse) {
        return new ResponseHook(mockOperation, software.amazon.smithy.java.context.Context.create(),
            (SerializableStruct) (Object) sdkRequest, httpRequest, httpResponse);
    }

    /**
     * Creates a real OutputHook with all fields populated.
     */
    private OutputHook createOutputHook(SdkRequest sdkRequest, HttpRequest httpRequest,
                                         HttpResponse httpResponse, SdkResponse sdkResponse) {
        return new OutputHook(mockOperation, software.amazon.smithy.java.context.Context.create(),
            (SerializableStruct) (Object) sdkRequest, httpRequest, httpResponse, (SerializableStruct) (Object) sdkResponse);
    }

    /**
     * Creates a real HttpRequest using the create() factory method.
     */
    private HttpRequest createHttpRequest() {
        ModifiableHttpRequest req = HttpRequest.create();
        req.setMethod("GET");
        req.setUri(URI.create("https://example.amazonaws.com/"));
        return req;
    }

    /**
     * Creates a real HttpResponse with the given status code.
     */
    private HttpResponse createHttpResponse(int statusCode) {
        return HttpResponse.of(
            HttpVersion.HTTP_1_1,
            statusCode,
            HttpHeaders.of(Collections.emptyMap()),
            null
        );
    }
}
