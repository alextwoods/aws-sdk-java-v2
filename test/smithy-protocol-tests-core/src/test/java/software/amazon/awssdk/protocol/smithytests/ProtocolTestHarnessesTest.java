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

package software.amazon.awssdk.protocol.smithytests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.client.builder.SdkClientBuilder;

class ProtocolTestHarnessesTest {

    @Test
    void forClient_withoutRegisteredFactory_failsOnUseNotOnCreation() {
        ProtocolTestHarness<Object> harness =
            ProtocolTestHarnesses.<Object, FakeBuilder>forClient("aws.protocols#awsJson1_0", () -> null, b -> { });
        assertThatThrownBy(() -> harness.captureRequest(c -> { }, "https://example.com"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(ProtocolTestHarnessFactory.class.getName());
    }

    @Test
    void factory_rejectsMissingAndDuplicateRegistrations() {
        assertThatThrownBy(() -> ProtocolTestHarnesses.factory(Collections.emptyList()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No ");
        assertThatThrownBy(() -> ProtocolTestHarnesses.factory(Arrays.asList(new RecordingFactory(),
                                                                             new RecordingFactory())))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("More than one")
            .hasMessageContaining(RecordingFactory.class.getName());
    }

    @Test
    void factory_returnsTheSingleRegistration() {
        RecordingFactory factory = new RecordingFactory();
        assertThat(ProtocolTestHarnesses.factory(Collections.singletonList(factory))).isSameAs(factory);
    }

    @Test
    void mapOf_keepsOrderAndNullValues() {
        Map<String, Integer> map = ProtocolTestValues.mapOf("b", 1, "a", null);
        assertThat(map).containsExactly(entry("b", 1),
                                        entry("a", null));
        assertThatThrownBy(() -> map.put("c", 2)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void mapOf_rejectsMalformedArguments() {
        assertThatThrownBy(() -> ProtocolTestValues.mapOf("a")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ProtocolTestValues.mapOf(1, "a")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ProtocolTestValues.mapOf("a", 1, "a", 2)).isInstanceOf(IllegalArgumentException.class);
    }

    private interface FakeBuilder extends SdkClientBuilder<FakeBuilder, Object> {
    }

    private static final class RecordingFactory implements ProtocolTestHarnessFactory {
        @Override
        public <C, B extends SdkClientBuilder<B, C>> ProtocolTestHarness<C> create(String protocol,
                                                                                  Supplier<B> clientBuilder,
                                                                                  Consumer<? super B> configuration) {
            return new ProtocolTestHarness<C>() {
                @Override
                public CapturedRequest captureRequest(Consumer<? super C> call, String endpoint) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public <T> T respondWith(StubbedResponse response, Function<? super C, ? extends T> call) {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }
}
