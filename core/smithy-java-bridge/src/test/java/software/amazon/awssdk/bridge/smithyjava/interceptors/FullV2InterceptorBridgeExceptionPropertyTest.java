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
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.smithy.java.client.core.interceptors.InputHook;
import software.amazon.smithy.java.client.core.interceptors.RequestHook;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.http.api.HttpHeaders;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.java.io.uri.SmithyUri;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Property-based test for exception transparency in {@link FullV2InterceptorBridge}.
 *
 * <p><b>Property 7: V2 Interceptor Exception Transparency</b></p>
 * <p>For any {@code RuntimeException} thrown by a v2 {@code ExecutionInterceptor} hook,
 * the {@code FullV2InterceptorBridge} SHALL propagate the exact same exception instance
 * without wrapping it in another exception type.</p>
 *
 * <p><b>Validates: Requirements 5.16</b></p>
 */
class FullV2InterceptorBridgeExceptionPropertyTest {

    /**
     * Provides arbitrary RuntimeException instances of various types that a v2 interceptor might throw.
     */
    @Provide
    Arbitrary<RuntimeException> arbitraryRuntimeExceptions() {
        Arbitrary<String> messages = Arbitraries.strings()
            .ofMinLength(1)
            .ofMaxLength(100)
            .alpha().numeric().withChars(' ', '-', '_');

        return Arbitraries.oneOf(
            messages.map(RuntimeException::new),
            messages.map(IllegalStateException::new),
            messages.map(IllegalArgumentException::new),
            messages.map(NullPointerException::new),
            messages.map(UnsupportedOperationException::new),
            messages.map(SecurityException::new),
            messages.map(msg -> new RuntimeException(msg, new RuntimeException("cause"))),
            messages.map(msg -> new IllegalStateException(msg, new NullPointerException("nested")))
        );
    }

    /**
     * <b>Property 7: V2 Interceptor Exception Transparency — readBeforeExecution</b>
     *
     * <p>When the v2 interceptor's {@code beforeExecution} hook throws a RuntimeException,
     * the exact same exception instance propagates through the bridge without wrapping.
     *
     * <p><b>Validates: Requirements 5.16</b>
     */
    @Property(tries = 100)
    void readBeforeExecution_propagatesExactException(
            @ForAll("arbitraryRuntimeExceptions") RuntimeException originalException) {

        ExecutionInterceptor throwingInterceptor = new ExecutionInterceptor() {
            @Override
            public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
                throw originalException;
            }
        };

        FullV2InterceptorBridge bridge = new FullV2InterceptorBridge(throwingInterceptor);
        InputHook<?, ?> hook = mockInputHook();

        Throwable thrown = catchThrowable(() -> bridge.readBeforeExecution(hook));

        assertThat(thrown).isSameAs(originalException);
    }

    /**
     * <b>Property 7: V2 Interceptor Exception Transparency — modifyBeforeSerialization</b>
     *
     * <p>When the v2 interceptor's {@code modifyRequest} hook throws a RuntimeException,
     * the exact same exception instance propagates through the bridge without wrapping.
     *
     * <p><b>Validates: Requirements 5.16</b>
     */
    @Property(tries = 100)
    void modifyBeforeSerialization_propagatesExactException(
            @ForAll("arbitraryRuntimeExceptions") RuntimeException originalException) {

        ExecutionInterceptor throwingInterceptor = new ExecutionInterceptor() {
            @Override
            public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
                throw originalException;
            }
        };

        FullV2InterceptorBridge bridge = new FullV2InterceptorBridge(throwingInterceptor);
        InputHook<?, ?> hook = mockInputHook();

        Throwable thrown = catchThrowable(() -> bridge.modifyBeforeSerialization(hook));

        assertThat(thrown).isSameAs(originalException);
    }

    /**
     * <b>Property 7: V2 Interceptor Exception Transparency — readBeforeTransmit</b>
     *
     * <p>When the v2 interceptor's {@code beforeTransmission} hook throws a RuntimeException,
     * the exact same exception instance propagates through the bridge without wrapping.
     *
     * <p><b>Validates: Requirements 5.16</b>
     */
    @Property(tries = 100)
    void readBeforeTransmit_propagatesExactException(
            @ForAll("arbitraryRuntimeExceptions") RuntimeException originalException) {

        ExecutionInterceptor throwingInterceptor = new ExecutionInterceptor() {
            @Override
            public void beforeTransmission(Context.BeforeTransmission context,
                                           ExecutionAttributes executionAttributes) {
                throw originalException;
            }
        };

        FullV2InterceptorBridge bridge = new FullV2InterceptorBridge(throwingInterceptor);
        RequestHook<?, ?, ?> hook = mockRequestHookWithHttpRequest();

        Throwable thrown = catchThrowable(() -> bridge.readBeforeTransmit(hook));

        assertThat(thrown).isSameAs(originalException);
    }

    // -- Test helpers --

    /**
     * Creates a mock InputHook that provides a mock input implementing both SdkRequest and
     * SerializableStruct (as real SDK requests do), and a mock operation with a real Schema
     * (needed for readBeforeExecution to extract operation name).
     */
    @SuppressWarnings("unchecked")
    private InputHook<?, ?> mockInputHook() {
        InputHook hook = mock(InputHook.class);

        // The input must implement both SdkRequest and SerializableStruct
        // (as real generated SDK request classes do)
        Object sdkRequest = mock(SdkRequest.class, withSettings().extraInterfaces(SerializableStruct.class));
        doReturn(sdkRequest).when(hook).input();

        // Use a real Schema (Schema is final and cannot be mocked)
        Schema schema = Schema.createOperation(ShapeId.from("com.example#TestOperation"));
        ApiOperation operation = mock(ApiOperation.class);
        doReturn(schema).when(operation).schema();
        doReturn(operation).when(hook).operation();

        return hook;
    }

    /**
     * Creates a mock RequestHook whose request() returns a mock HttpRequest instance,
     * allowing the bridge's instanceof check to pass and providing data needed
     * for conversion to SdkHttpRequest.
     */
    @SuppressWarnings("unchecked")
    private RequestHook<?, ?, ?> mockRequestHookWithHttpRequest() {
        RequestHook hook = mock(RequestHook.class);

        // The input must implement both SdkRequest and SerializableStruct
        Object sdkRequest = mock(SdkRequest.class, withSettings().extraInterfaces(SerializableStruct.class));
        doReturn(sdkRequest).when(hook).input();

        // Create a mock HttpRequest that satisfies the bridge's conversion needs
        HttpRequest httpRequest = mock(HttpRequest.class);
        SmithyUri smithyUri = mock(SmithyUri.class);
        doReturn(URI.create("https://example.com")).when(smithyUri).toURI();
        doReturn(smithyUri).when(httpRequest).uri();
        doReturn("GET").when(httpRequest).method();
        HttpHeaders headers = mock(HttpHeaders.class);
        doReturn(Collections.<String, List<String>>emptyMap()).when(headers).map();
        doReturn(headers).when(httpRequest).headers();

        doReturn(httpRequest).when(hook).request();

        return hook;
    }
}
