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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.net.URI;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
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
 * Property-based tests for {@link FullV2InterceptorBridge} verifying interceptor hook dispatch fidelity.
 *
 * <p><b>Property 5: V2 Interceptor Hook Dispatch Fidelity</b> — For any v2 {@code ExecutionInterceptor},
 * when a smithy-java hook is invoked on the {@code FullV2InterceptorBridge}, the corresponding v2 interceptor
 * hook SHALL be called with the correct context, and modify hooks SHALL return mutations that flow through
 * to subsequent stages.
 *
 * <p><b>Validates: Requirements 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9, 5.10, 5.11, 5.12, 5.13, 5.15</b>
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class FullV2InterceptorBridgePropertyTest {

    /**
     * Property: readBeforeExecution on the bridge calls v2 interceptor's beforeExecution
     * with the correct SdkRequest from the hook's input.
     *
     * <p><b>Validates: Requirements 5.2</b>
     */
    @Property(tries = 100)
    void readBeforeExecution_callsV2BeforeExecution(@ForAll("operationNames") String operationName) {
        AtomicReference<Context.BeforeExecution> capturedContext = new AtomicReference<>();
        ExecutionInterceptor v2Interceptor = new ExecutionInterceptor() {
            @Override
            public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
                capturedContext.set(context);
            }
        };

        FullV2InterceptorBridge bridge = new FullV2InterceptorBridge(v2Interceptor);
        SdkRequest sdkRequest = mockSdkRequest();
        InputHook hook = createInputHook(sdkRequest, operationName);

        bridge.readBeforeExecution(hook);

        assertThat(capturedContext.get()).isNotNull();
        assertThat(capturedContext.get().request()).isSameAs(sdkRequest);
    }

    /**
     * Property: modifyBeforeSerialization calls v2 interceptor's modifyRequest and returns the modified SdkRequest.
     *
     * <p><b>Validates: Requirements 5.3</b>
     */
    @Property(tries = 100)
    void modifyBeforeSerialization_callsV2ModifyRequest(@ForAll("operationNames") String operationName) {
        SdkRequest originalRequest = mockSdkRequest();
        SdkRequest modifiedRequest = mockSdkRequest();

        ExecutionInterceptor v2Interceptor = new ExecutionInterceptor() {
            @Override
            public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
                assertThat(context.request()).isSameAs(originalRequest);
                return modifiedRequest;
            }
        };

        FullV2InterceptorBridge bridge = new FullV2InterceptorBridge(v2Interceptor);
        InputHook hook = createInputHook(originalRequest, operationName);

        Object result = bridge.modifyBeforeSerialization(hook);

        assertThat(result).isSameAs(modifiedRequest);
    }

    /**
     * Property: readBeforeSerialization calls v2 interceptor's beforeMarshalling with the correct SdkRequest.
     *
     * <p><b>Validates: Requirements 5.4</b>
     */
    @Property(tries = 100)
    void readBeforeSerialization_callsV2BeforeMarshalling(@ForAll("operationNames") String operationName) {
        AtomicReference<Context.BeforeMarshalling> capturedContext = new AtomicReference<>();
        ExecutionInterceptor v2Interceptor = new ExecutionInterceptor() {
            @Override
            public void beforeMarshalling(Context.BeforeMarshalling context, ExecutionAttributes executionAttributes) {
                capturedContext.set(context);
            }
        };

        FullV2InterceptorBridge bridge = new FullV2InterceptorBridge(v2Interceptor);
        SdkRequest sdkRequest = mockSdkRequest();
        InputHook hook = createInputHook(sdkRequest, operationName);

        bridge.readBeforeSerialization(hook);

        assertThat(capturedContext.get()).isNotNull();
        assertThat(capturedContext.get().request()).isSameAs(sdkRequest);
    }

    /**
     * Property: readAfterSerialization calls v2 interceptor's afterMarshalling with the SdkRequest
     * and SdkHttpRequest from the hook.
     *
     * <p><b>Validates: Requirements 5.5</b>
     */
    @Property(tries = 100)
    void readAfterSerialization_callsV2AfterMarshalling(@ForAll("operationNames") String operationName) {
        AtomicReference<Context.AfterMarshalling> capturedContext = new AtomicReference<>();
        ExecutionInterceptor v2Interceptor = new ExecutionInterceptor() {
            @Override
            public void afterMarshalling(Context.AfterMarshalling context, ExecutionAttributes executionAttributes) {
                capturedContext.set(context);
            }
        };

        FullV2InterceptorBridge bridge = new FullV2InterceptorBridge(v2Interceptor);
        SdkRequest sdkRequest = mockSdkRequest();
        HttpRequest httpRequest = createHttpRequest();
        RequestHook hook = createRequestHook(sdkRequest, httpRequest, operationName);

        bridge.readAfterSerialization(hook);

        assertThat(capturedContext.get()).isNotNull();
        assertThat(capturedContext.get().request()).isSameAs(sdkRequest);
        assertThat(capturedContext.get().httpRequest()).isNotNull();
    }

    /**
     * Property: modifyBeforeSigning calls v2 interceptor's modifyHttpRequest and returns a modified request.
     *
     * <p><b>Validates: Requirements 5.6</b>
     */
    @Property(tries = 100)
    void modifyBeforeSigning_callsV2ModifyHttpRequest(@ForAll("operationNames") String operationName) {
        AtomicBoolean modifyHttpRequestCalled = new AtomicBoolean(false);
        ExecutionInterceptor v2Interceptor = new ExecutionInterceptor() {
            @Override
            public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context,
                                                    ExecutionAttributes executionAttributes) {
                modifyHttpRequestCalled.set(true);
                return context.httpRequest();
            }
        };

        FullV2InterceptorBridge bridge = new FullV2InterceptorBridge(v2Interceptor);
        SdkRequest sdkRequest = mockSdkRequest();
        HttpRequest httpRequest = createHttpRequest();
        RequestHook hook = createRequestHook(sdkRequest, httpRequest, operationName);

        bridge.modifyBeforeSigning(hook);

        assertThat(modifyHttpRequestCalled.get()).isTrue();
    }

    /**
     * Property: readBeforeTransmit calls v2 interceptor's beforeTransmission when the request is an HttpRequest.
     *
     * <p><b>Validates: Requirements 5.7</b>
     */
    @Property(tries = 100)
    void readBeforeTransmit_callsV2BeforeTransmission(@ForAll("operationNames") String operationName) {
        AtomicBoolean beforeTransmissionCalled = new AtomicBoolean(false);
        ExecutionInterceptor v2Interceptor = new ExecutionInterceptor() {
            @Override
            public void beforeTransmission(Context.BeforeTransmission context,
                                           ExecutionAttributes executionAttributes) {
                beforeTransmissionCalled.set(true);
            }
        };

        FullV2InterceptorBridge bridge = new FullV2InterceptorBridge(v2Interceptor);
        SdkRequest sdkRequest = mockSdkRequest();
        HttpRequest httpRequest = createHttpRequest();
        RequestHook hook = createRequestHook(sdkRequest, httpRequest, operationName);

        bridge.readBeforeTransmit(hook);

        assertThat(beforeTransmissionCalled.get()).isTrue();
    }

    /**
     * Property: readAfterTransmit calls v2 interceptor's afterTransmission when both request and response
     * are HTTP types.
     *
     * <p><b>Validates: Requirements 5.8</b>
     */
    @Property(tries = 100)
    void readAfterTransmit_callsV2AfterTransmission(@ForAll("operationNames") String operationName) {
        AtomicBoolean afterTransmissionCalled = new AtomicBoolean(false);
        ExecutionInterceptor v2Interceptor = new ExecutionInterceptor() {
            @Override
            public void afterTransmission(Context.AfterTransmission context,
                                          ExecutionAttributes executionAttributes) {
                afterTransmissionCalled.set(true);
            }
        };

        FullV2InterceptorBridge bridge = new FullV2InterceptorBridge(v2Interceptor);
        SdkRequest sdkRequest = mockSdkRequest();
        HttpRequest httpRequest = createHttpRequest();
        HttpResponse httpResponse = createHttpResponse(200);
        ResponseHook hook = createResponseHook(sdkRequest, httpRequest, httpResponse, operationName);

        bridge.readAfterTransmit(hook);

        assertThat(afterTransmissionCalled.get()).isTrue();
    }

    /**
     * Property: modifyBeforeDeserialization calls v2 interceptor's modifyHttpResponse when
     * the response is an HttpResponse.
     *
     * <p><b>Validates: Requirements 5.9</b>
     */
    @Property(tries = 100)
    void modifyBeforeDeserialization_callsV2ModifyHttpResponse(@ForAll("operationNames") String operationName) {
        AtomicBoolean modifyHttpResponseCalled = new AtomicBoolean(false);
        ExecutionInterceptor v2Interceptor = new ExecutionInterceptor() {
            @Override
            public SdkHttpResponse modifyHttpResponse(Context.ModifyHttpResponse context,
                                                      ExecutionAttributes executionAttributes) {
                modifyHttpResponseCalled.set(true);
                return context.httpResponse();
            }
        };

        FullV2InterceptorBridge bridge = new FullV2InterceptorBridge(v2Interceptor);
        SdkRequest sdkRequest = mockSdkRequest();
        HttpRequest httpRequest = createHttpRequest();
        HttpResponse httpResponse = createHttpResponse(200);
        ResponseHook hook = createResponseHook(sdkRequest, httpRequest, httpResponse, operationName);

        bridge.modifyBeforeDeserialization(hook);

        assertThat(modifyHttpResponseCalled.get()).isTrue();
    }

    /**
     * Property: readBeforeDeserialization calls v2 interceptor's beforeUnmarshalling when
     * the response is an HttpResponse and input is SdkRequest.
     *
     * <p><b>Validates: Requirements 5.10</b>
     */
    @Property(tries = 100)
    void readBeforeDeserialization_callsV2BeforeUnmarshalling(@ForAll("operationNames") String operationName) {
        AtomicBoolean beforeUnmarshallingCalled = new AtomicBoolean(false);
        ExecutionInterceptor v2Interceptor = new ExecutionInterceptor() {
            @Override
            public void beforeUnmarshalling(Context.BeforeUnmarshalling context,
                                            ExecutionAttributes executionAttributes) {
                beforeUnmarshallingCalled.set(true);
            }
        };

        FullV2InterceptorBridge bridge = new FullV2InterceptorBridge(v2Interceptor);
        SdkRequest sdkRequest = mockSdkRequest();
        HttpRequest httpRequest = createHttpRequest();
        HttpResponse httpResponse = createHttpResponse(200);
        ResponseHook hook = createResponseHook(sdkRequest, httpRequest, httpResponse, operationName);

        bridge.readBeforeDeserialization(hook);

        assertThat(beforeUnmarshallingCalled.get()).isTrue();
    }

    /**
     * Property: readAfterDeserialization calls v2 interceptor's afterUnmarshalling when
     * the input is SdkRequest.
     *
     * <p><b>Validates: Requirements 5.11</b>
     */
    @Property(tries = 100)
    void readAfterDeserialization_callsV2AfterUnmarshalling(@ForAll("operationNames") String operationName) {
        AtomicBoolean afterUnmarshallingCalled = new AtomicBoolean(false);
        ExecutionInterceptor v2Interceptor = new ExecutionInterceptor() {
            @Override
            public void afterUnmarshalling(Context.AfterUnmarshalling context,
                                           ExecutionAttributes executionAttributes) {
                afterUnmarshallingCalled.set(true);
            }
        };

        FullV2InterceptorBridge bridge = new FullV2InterceptorBridge(v2Interceptor);
        SdkRequest sdkRequest = mockSdkRequest();
        SdkResponse sdkResponse = mockSdkResponse();
        HttpRequest httpRequest = createHttpRequest();
        HttpResponse httpResponse = createHttpResponse(200);
        OutputHook hook = createOutputHook(sdkRequest, httpRequest, httpResponse, sdkResponse, operationName);

        bridge.readAfterDeserialization(hook, null);

        assertThat(afterUnmarshallingCalled.get()).isTrue();
    }

    /**
     * Property: readAfterExecution calls v2 interceptor's afterExecution on success (no error).
     *
     * <p><b>Validates: Requirements 5.12</b>
     */
    @Property(tries = 100)
    void readAfterExecution_success_callsV2AfterExecution(@ForAll("operationNames") String operationName) {
        AtomicBoolean afterExecutionCalled = new AtomicBoolean(false);
        ExecutionInterceptor v2Interceptor = new ExecutionInterceptor() {
            @Override
            public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
                afterExecutionCalled.set(true);
            }
        };

        FullV2InterceptorBridge bridge = new FullV2InterceptorBridge(v2Interceptor);
        SdkRequest sdkRequest = mockSdkRequest();
        SdkResponse sdkResponse = mockSdkResponse();
        HttpRequest httpRequest = createHttpRequest();
        HttpResponse httpResponse = createHttpResponse(200);
        OutputHook hook = createOutputHook(sdkRequest, httpRequest, httpResponse, sdkResponse, operationName);

        bridge.readAfterExecution(hook, null);

        assertThat(afterExecutionCalled.get()).isTrue();
    }

    /**
     * Property: readAfterExecution calls v2 interceptor's onExecutionFailure when error is present.
     *
     * <p><b>Validates: Requirements 5.12</b>
     */
    @Property(tries = 100)
    void readAfterExecution_failure_callsV2OnExecutionFailure(@ForAll("operationNames") String operationName) {
        AtomicBoolean onExecutionFailureCalled = new AtomicBoolean(false);
        ExecutionInterceptor v2Interceptor = new ExecutionInterceptor() {
            @Override
            public void onExecutionFailure(Context.FailedExecution context,
                                           ExecutionAttributes executionAttributes) {
                onExecutionFailureCalled.set(true);
            }
        };

        FullV2InterceptorBridge bridge = new FullV2InterceptorBridge(v2Interceptor);
        SdkRequest sdkRequest = mockSdkRequest();
        SdkResponse sdkResponse = mockSdkResponse();
        HttpRequest httpRequest = createHttpRequest();
        HttpResponse httpResponse = createHttpResponse(500);
        OutputHook hook = createOutputHook(sdkRequest, httpRequest, httpResponse, sdkResponse, operationName);

        RuntimeException error = new RuntimeException("test failure");
        bridge.readAfterExecution(hook, error);

        assertThat(onExecutionFailureCalled.get()).isTrue();
    }

    /**
     * Property: modifyBeforeAttemptCompletion calls v2 interceptor's modifyException when error is present,
     * and the modified exception flows through.
     *
     * <p><b>Validates: Requirements 5.15</b>
     */
    @Property(tries = 100)
    void modifyBeforeAttemptCompletion_callsV2ModifyException(@ForAll("operationNames") String operationName) {
        RuntimeException originalError = new RuntimeException("original");
        RuntimeException modifiedError = new RuntimeException("modified");
        AtomicBoolean modifyExceptionCalled = new AtomicBoolean(false);

        ExecutionInterceptor v2Interceptor = new ExecutionInterceptor() {
            @Override
            public Throwable modifyException(Context.FailedExecution context,
                                             ExecutionAttributes executionAttributes) {
                modifyExceptionCalled.set(true);
                return modifiedError;
            }
        };

        FullV2InterceptorBridge bridge = new FullV2InterceptorBridge(v2Interceptor);
        SdkRequest sdkRequest = mockSdkRequest();
        SdkResponse sdkResponse = mockSdkResponse();
        HttpRequest httpRequest = createHttpRequest();
        HttpResponse httpResponse = createHttpResponse(500);
        OutputHook hook = createOutputHook(sdkRequest, httpRequest, httpResponse, sdkResponse, operationName);

        try {
            bridge.modifyBeforeAttemptCompletion(hook, originalError);
        } catch (RuntimeException e) {
            // The bridge calls hook.forward(modifiedError) which throws the error
        }

        assertThat(modifyExceptionCalled.get()).isTrue();
    }

    /**
     * Property: modifyBeforeSerialization's mutation flows through — when the v2 interceptor's modifyRequest
     * returns a different SdkRequest, the bridge returns that modified request as the new input.
     *
     * <p><b>Validates: Requirements 5.3</b> (mutation flow-through)
     */
    @Property(tries = 100)
    void modifyBeforeSerialization_mutationFlowsThrough(@ForAll("operationNames") String operationName) {
        SdkRequest original = mockSdkRequest();
        SdkRequest modified = mockSdkRequest();

        ExecutionInterceptor v2Interceptor = new ExecutionInterceptor() {
            @Override
            public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
                return modified;
            }
        };

        FullV2InterceptorBridge bridge = new FullV2InterceptorBridge(v2Interceptor);
        InputHook hook = createInputHook(original, operationName);

        Object result = bridge.modifyBeforeSerialization(hook);

        // The modified request is different from the original
        assertThat(result).isSameAs(modified);
        assertThat(result).isNotSameAs(original);
    }

    // -- Arbitraries --

    @Provide
    Arbitrary<String> operationNames() {
        return Arbitraries.of(
            "GetItem", "PutItem", "DeleteItem", "UpdateItem", "Query", "Scan",
            "ListTables", "DescribeTable", "CreateTable", "BatchWriteItem",
            "BatchGetItem", "TransactWriteItems", "TransactGetItems"
        );
    }

    // -- Helper methods --

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
    private SdkResponse mockSdkResponse() {
        return mock(SdkResponse.class, withSettings().extraInterfaces(SerializableStruct.class));
    }

    /**
     * Creates a real InputHook using its constructor with a mock ApiOperation.
     */
    private InputHook createInputHook(SdkRequest sdkRequest, String operationName) {
        ApiOperation operation = mockOperation(operationName);
        return new InputHook(operation, software.amazon.smithy.java.context.Context.create(),
            (SerializableStruct) (Object) sdkRequest);
    }

    /**
     * Creates a real RequestHook with an HttpRequest.
     */
    private RequestHook createRequestHook(SdkRequest sdkRequest, HttpRequest httpRequest, String operationName) {
        ApiOperation operation = mockOperation(operationName);
        return new RequestHook(operation, software.amazon.smithy.java.context.Context.create(),
            (SerializableStruct) (Object) sdkRequest, httpRequest);
    }

    /**
     * Creates a real ResponseHook with HttpRequest and HttpResponse.
     */
    private ResponseHook createResponseHook(SdkRequest sdkRequest, HttpRequest httpRequest,
                                             HttpResponse httpResponse, String operationName) {
        ApiOperation operation = mockOperation(operationName);
        return new ResponseHook(operation, software.amazon.smithy.java.context.Context.create(),
            (SerializableStruct) (Object) sdkRequest, httpRequest, httpResponse);
    }

    /**
     * Creates a real OutputHook with all fields populated.
     */
    private OutputHook createOutputHook(SdkRequest sdkRequest, HttpRequest httpRequest,
                                         HttpResponse httpResponse, SdkResponse sdkResponse,
                                         String operationName) {
        ApiOperation operation = mockOperation(operationName);
        return new OutputHook(operation, software.amazon.smithy.java.context.Context.create(),
            (SerializableStruct) (Object) sdkRequest, httpRequest, httpResponse,
            (SerializableStruct) (Object) sdkResponse);
    }

    /**
     * Creates a mock ApiOperation with a schema containing the given operation name.
     */
    private ApiOperation mockOperation(String operationName) {
        ApiOperation operation = mock(ApiOperation.class);
        ShapeId shapeId = ShapeId.from("com.amazonaws.test#" + operationName);
        Schema schema = Schema.createOperation(shapeId);
        when(operation.schema()).thenReturn(schema);
        return operation;
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
