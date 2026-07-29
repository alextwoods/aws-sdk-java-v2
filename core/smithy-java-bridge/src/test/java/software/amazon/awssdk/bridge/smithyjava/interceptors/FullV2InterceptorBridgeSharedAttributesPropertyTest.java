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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
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
 * Property-based test for shared {@link ExecutionAttributes} across hooks in {@link FullV2InterceptorBridge}.
 *
 * <p><b>Property 6: ExecutionAttributes Shared Across Hooks</b></p>
 * <p>For any attribute key/value pair set by a v2 {@code ExecutionInterceptor} in an early hook
 * (e.g., {@code beforeExecution}), all subsequent hooks invoked on the same {@code FullV2InterceptorBridge}
 * instance SHALL observe the same attribute value via the shared {@code ExecutionAttributes} instance.</p>
 *
 * <p><b>Validates: Requirements 5.14</b></p>
 */
class FullV2InterceptorBridgeSharedAttributesPropertyTest {

    /**
     * Shared ExecutionAttribute used across property tests. ExecutionAttributes enforce
     * name-uniqueness globally, so we use a single static constant rather than creating
     * new instances per property invocation.
     */
    private static final ExecutionAttribute<String> TEST_ATTR =
        new ExecutionAttribute<>("SharedAttrsPropertyTest.testAttribute");

    /**
     * Property: An attribute set in beforeExecution is visible on the bridge's shared ExecutionAttributes.
     *
     * <p>Generates random attribute values, sets them in beforeExecution, then verifies
     * bridge.executionAttributes() contains the set attribute.
     *
     * <p><b>Validates: Requirements 5.14</b>
     */
    @Property(tries = 100)
    void attributeSetInBeforeExecution_visibleOnBridgeExecutionAttributes(
            @ForAll("attributeScenarios") AttributeScenario scenario) {

        ExecutionInterceptor v2Interceptor = new ExecutionInterceptor() {
            @Override
            public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
                executionAttributes.putAttribute(TEST_ATTR, scenario.attributeValue);
            }
        };

        FullV2InterceptorBridge bridge = new FullV2InterceptorBridge(v2Interceptor);
        InputHook<?, ?> hook = mockInputHook(scenario.operationName);

        bridge.readBeforeExecution(hook);

        // The bridge's shared ExecutionAttributes should have the attribute set by beforeExecution
        assertThat(bridge.executionAttributes().getAttribute(TEST_ATTR)).isEqualTo(scenario.attributeValue);
    }

    /**
     * Property: An attribute set in beforeExecution is visible to subsequent hooks
     * (e.g., beforeMarshalling) on the same bridge instance.
     *
     * <p>This demonstrates that the same ExecutionAttributes object is shared across all
     * hook invocations within a single execution.
     *
     * <p><b>Validates: Requirements 5.14</b>
     */
    @Property(tries = 100)
    void attributeSetInBeforeExecution_visibleInSubsequentHook(
            @ForAll("attributeScenarios") AttributeScenario scenario) {

        AtomicReference<String> observedInLaterHook = new AtomicReference<>();

        ExecutionInterceptor v2Interceptor = new ExecutionInterceptor() {
            @Override
            public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
                executionAttributes.putAttribute(TEST_ATTR, scenario.attributeValue);
            }

            @Override
            public void beforeMarshalling(Context.BeforeMarshalling context, ExecutionAttributes executionAttributes) {
                observedInLaterHook.set(executionAttributes.getAttribute(TEST_ATTR));
            }
        };

        FullV2InterceptorBridge bridge = new FullV2InterceptorBridge(v2Interceptor);
        InputHook<?, ?> hook = mockInputHook(scenario.operationName);

        // Invoke beforeExecution (sets the attribute)
        bridge.readBeforeExecution(hook);
        // Invoke beforeMarshalling (reads the attribute)
        bridge.readBeforeSerialization(hook);

        assertThat(observedInLaterHook.get()).isEqualTo(scenario.attributeValue);
    }

    /**
     * Property: An attribute set in beforeExecution is visible to beforeTransmission on the same bridge.
     *
     * <p>This tests a later hook in the pipeline to ensure persistence across multiple stages.
     *
     * <p><b>Validates: Requirements 5.14</b>
     */
    @Property(tries = 100)
    void attributeSetInBeforeExecution_visibleInBeforeTransmission(
            @ForAll("attributeScenarios") AttributeScenario scenario) {

        AtomicReference<String> observedInTransmitHook = new AtomicReference<>();

        ExecutionInterceptor v2Interceptor = new ExecutionInterceptor() {
            @Override
            public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
                executionAttributes.putAttribute(TEST_ATTR, scenario.attributeValue);
            }

            @Override
            public void beforeTransmission(Context.BeforeTransmission context,
                                           ExecutionAttributes executionAttributes) {
                observedInTransmitHook.set(executionAttributes.getAttribute(TEST_ATTR));
            }
        };

        FullV2InterceptorBridge bridge = new FullV2InterceptorBridge(v2Interceptor);
        InputHook<?, ?> hook = mockInputHook(scenario.operationName);
        RequestHook<?, ?, ?> requestHook = mockRequestHookWithHttpRequest(scenario.operationName);

        // Invoke beforeExecution (sets the attribute)
        bridge.readBeforeExecution(hook);
        // Invoke beforeTransmission (reads the attribute)
        bridge.readBeforeTransmit(requestHook);

        assertThat(observedInTransmitHook.get()).isEqualTo(scenario.attributeValue);
    }

    /**
     * Property: The ExecutionAttributes instance is the same object reference across all hooks.
     *
     * <p>Captures the reference in two different hooks and asserts they are the same instance.
     *
     * <p><b>Validates: Requirements 5.14</b>
     */
    @Property(tries = 100)
    void sameExecutionAttributesInstance_acrossHooks(
            @ForAll("operationNames") String operationName) {

        AtomicReference<ExecutionAttributes> capturedInBeforeExecution = new AtomicReference<>();
        AtomicReference<ExecutionAttributes> capturedInBeforeMarshalling = new AtomicReference<>();

        ExecutionInterceptor v2Interceptor = new ExecutionInterceptor() {
            @Override
            public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
                capturedInBeforeExecution.set(executionAttributes);
            }

            @Override
            public void beforeMarshalling(Context.BeforeMarshalling context, ExecutionAttributes executionAttributes) {
                capturedInBeforeMarshalling.set(executionAttributes);
            }
        };

        FullV2InterceptorBridge bridge = new FullV2InterceptorBridge(v2Interceptor);
        InputHook<?, ?> hook = mockInputHook(operationName);

        bridge.readBeforeExecution(hook);
        bridge.readBeforeSerialization(hook);

        // Both hooks receive the exact same ExecutionAttributes instance
        assertThat(capturedInBeforeExecution.get()).isSameAs(capturedInBeforeMarshalling.get());
        // And it matches what the bridge exposes
        assertThat(capturedInBeforeExecution.get()).isSameAs(bridge.executionAttributes());
    }

    // -- Arbitraries --

    @Provide
    Arbitrary<AttributeScenario> attributeScenarios() {
        Arbitrary<String> values = Arbitraries.strings()
            .withCharRange('a', 'z')
            .withCharRange('A', 'Z')
            .withCharRange('0', '9')
            .withChars('-', '_', '.')
            .ofMinLength(1)
            .ofMaxLength(100);

        Arbitrary<String> opNames = operationNames();

        return Combinators.combine(values, opNames).as(AttributeScenario::new);
    }

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
     * Creates a mock InputHook that provides a mock SdkRequest (which also implements
     * SerializableStruct to satisfy the InputHook generic bound) and a real operation Schema.
     * Uses raw types and doReturn to avoid generic type capture issues with Mockito.
     * Uses a real Schema (via Schema.createOperation) to avoid Mockito issues with Schema.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private InputHook<?, ?> mockInputHook(String operationName) {
        InputHook hook = mock(InputHook.class);
        // SdkRequest must also implement SerializableStruct to satisfy InputHook<I extends SerializableStruct, ?>
        SdkRequest sdkRequest = mock(SdkRequest.class, withSettings().extraInterfaces(SerializableStruct.class));
        doReturn(sdkRequest).when(hook).input();

        ShapeId shapeId = ShapeId.from("com.amazonaws.test#" + operationName);
        Schema operationSchema = Schema.createOperation(shapeId);

        ApiOperation operation = mock(ApiOperation.class);
        doReturn(operationSchema).when(operation).schema();
        doReturn(operation).when(hook).operation();

        return hook;
    }

    /**
     * Creates a mock RequestHook whose request() returns a mock HttpRequest instance,
     * allowing the bridge's instanceof check to pass.
     * Uses raw types and doReturn to avoid generic type capture issues with Mockito.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private RequestHook<?, ?, ?> mockRequestHookWithHttpRequest(String operationName) {
        RequestHook hook = mock(RequestHook.class);
        // SdkRequest must also implement SerializableStruct to satisfy RequestHook generic bounds
        SdkRequest sdkRequest = mock(SdkRequest.class, withSettings().extraInterfaces(SerializableStruct.class));
        doReturn(sdkRequest).when(hook).input();

        // Create a mock HttpRequest that satisfies the bridge's conversion needs
        HttpRequest httpRequest = mock(HttpRequest.class);
        SmithyUri smithyUri = mock(SmithyUri.class);
        doReturn(URI.create("https://example.amazonaws.com/")).when(smithyUri).toURI();
        doReturn(smithyUri).when(httpRequest).uri();
        doReturn("GET").when(httpRequest).method();
        HttpHeaders headers = mock(HttpHeaders.class);
        doReturn(Collections.<String, List<String>>emptyMap()).when(headers).map();
        doReturn(headers).when(httpRequest).headers();

        doReturn(httpRequest).when(hook).request();

        ShapeId shapeId = ShapeId.from("com.amazonaws.test#" + operationName);
        Schema operationSchema = Schema.createOperation(shapeId);

        ApiOperation operation = mock(ApiOperation.class);
        doReturn(operationSchema).when(operation).schema();
        doReturn(operation).when(hook).operation();

        return hook;
    }

    /**
     * Data holder for a test scenario: attribute value and operation name.
     */
    static final class AttributeScenario {
        final String attributeValue;
        final String operationName;

        AttributeScenario(String attributeValue, String operationName) {
            this.attributeValue = attributeValue;
            this.operationName = operationName;
        }

        @Override
        public String toString() {
            return "AttributeScenario{value='" + attributeValue
                   + "', operation='" + operationName + "'}";
        }
    }
}
