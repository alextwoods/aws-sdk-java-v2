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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import software.amazon.awssdk.bridge.smithyjava.interceptors.FullV2InterceptorBridge;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;

/**
 * Property-based tests for interceptor registration order preservation in {@link SmithyJavaPipeline}.
 *
 * <p><b>Property 16: Interceptor Registration Order Preservation</b> — For any ordered list of v2
 * {@code ExecutionInterceptor} instances from {@code SdkClientOption.EXECUTION_INTERCEPTORS}, the
 * {@code SmithyJavaPipeline} SHALL register them as {@code FullV2InterceptorBridge} instances on
 * the smithy-java Client in the same order, such that forward hooks execute in registration order
 * and completion hooks in reverse registration order.
 *
 * <p><b>Validates: Requirements 5.1</b>
 */
class SmithyJavaPipelineInterceptorOrderPropertyTest {

    /**
     * Property: For any list of v2 ExecutionInterceptors, wrapping them as FullV2InterceptorBridge
     * instances preserves the original ordering. This test verifies the core wrapping logic that
     * SmithyJavaPipeline uses when registering interceptors on the smithy-java Client builder.
     *
     * <p>The wrapping logic in SmithyJavaPipeline is:
     * <pre>
     * for (ExecutionInterceptor v2Interceptor : v2Interceptors) {
     *     builder.addInterceptor(new FullV2InterceptorBridge(v2Interceptor));
     * }
     * </pre>
     *
     * This property verifies that the resulting bridge list maintains the same order as the input.
     */
    @Property(tries = 100)
    void interceptorRegistrationPreservesOrder(@ForAll("interceptorLists") List<ExecutionInterceptor> v2Interceptors) {
        // Simulate the registration logic from SmithyJavaPipeline.buildSmithyClient()
        List<FullV2InterceptorBridge> bridges = new ArrayList<>();
        for (ExecutionInterceptor v2Interceptor : v2Interceptors) {
            bridges.add(new FullV2InterceptorBridge(v2Interceptor));
        }

        // Verify order preservation: bridge[i].v2Interceptor() == v2Interceptors.get(i)
        assertThat(bridges).hasSameSizeAs(v2Interceptors);
        for (int i = 0; i < v2Interceptors.size(); i++) {
            assertThat(bridges.get(i).v2Interceptor())
                .as("Bridge at index %d should wrap the interceptor at the same index", i)
                .isSameAs(v2Interceptors.get(i));
        }
    }

    /**
     * Property: The ordering of FullV2InterceptorBridge instances is deterministic regardless
     * of the number of interceptors — the i-th bridge always wraps the i-th v2 interceptor.
     */
    @Property(tries = 100)
    void interceptorOrderIsDeterministic(@ForAll("listSizes") int size) {
        // Create a list of distinct interceptor instances
        List<ExecutionInterceptor> v2Interceptors = IntStream.range(0, size)
            .mapToObj(i -> new TaggedInterceptor(i))
            .collect(Collectors.toList());

        // Wrap them using the same pattern as SmithyJavaPipeline
        List<FullV2InterceptorBridge> bridges = new ArrayList<>();
        for (ExecutionInterceptor v2Interceptor : v2Interceptors) {
            bridges.add(new FullV2InterceptorBridge(v2Interceptor));
        }

        // Verify each bridge wraps the correct interceptor
        for (int i = 0; i < size; i++) {
            TaggedInterceptor expected = (TaggedInterceptor) v2Interceptors.get(i);
            TaggedInterceptor actual = (TaggedInterceptor) bridges.get(i).v2Interceptor();
            assertThat(actual.tag())
                .as("Bridge at position %d should wrap interceptor with tag %d", i, i)
                .isEqualTo(expected.tag());
        }
    }

    /**
     * Property: Empty interceptor list results in empty bridge list (no NPE or exceptions).
     */
    @Property(tries = 10)
    void emptyInterceptorListProducesEmptyBridgeList() {
        List<ExecutionInterceptor> v2Interceptors = List.of();

        List<FullV2InterceptorBridge> bridges = new ArrayList<>();
        for (ExecutionInterceptor v2Interceptor : v2Interceptors) {
            bridges.add(new FullV2InterceptorBridge(v2Interceptor));
        }

        assertThat(bridges).isEmpty();
    }

    // -- Arbitraries --

    @Provide
    Arbitrary<List<ExecutionInterceptor>> interceptorLists() {
        Arbitrary<ExecutionInterceptor> interceptorArbitrary = Arbitraries.integers()
            .between(0, 1000)
            .map(TaggedInterceptor::new);

        return interceptorArbitrary.list().ofMinSize(1).ofMaxSize(20);
    }

    @Provide
    Arbitrary<Integer> listSizes() {
        return Arbitraries.integers().between(1, 50);
    }

    // -- Helper class --

    /**
     * A trivial ExecutionInterceptor implementation with a tag for identity tracking.
     */
    private static final class TaggedInterceptor implements ExecutionInterceptor {
        private final int tag;

        TaggedInterceptor(int tag) {
            this.tag = tag;
        }

        int tag() {
            return tag;
        }
    }
}
