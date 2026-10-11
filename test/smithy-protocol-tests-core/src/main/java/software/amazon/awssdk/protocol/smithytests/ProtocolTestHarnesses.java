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

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.core.client.builder.SdkClientBuilder;
import software.amazon.awssdk.utils.Validate;

/**
 * How generated protocol test classes obtain their {@link ProtocolTestHarness}. Each generated class holds one harness
 * in a static field:
 *
 * <pre>{@code
 * private static final ProtocolTestHarness<JsonRpc10Client> HARNESS =
 *     ProtocolTestHarnesses.<JsonRpc10Client, JsonRpc10ClientBuilder>forClient(
 *         "aws.protocols#awsJson1_0", JsonRpc10Client::builder, b -> b.region(Region.US_EAST_1) ...);
 * }</pre>
 *
 * <p>The returned harness is lazy: the {@link ProtocolTestHarnessFactory} is looked up through
 * {@link ServiceLoader} on the first {@code captureRequest} or {@code respondWith} call, not when the test class is
 * loaded. When no factory is registered, every test fails with an {@link IllegalStateException} that says so, instead
 * of the test class failing to initialize.
 */
public final class ProtocolTestHarnesses {

    private ProtocolTestHarnesses() {
    }

    /**
     * A harness for clients of type {@code C}, created by the registered {@link ProtocolTestHarnessFactory} on first use.
     *
     * @param protocol      the protocol ShapeId of the test cases
     * @param clientBuilder creates a new client builder, for example {@code JsonRpc10Client::builder}
     * @param configuration client settings applied to every new builder
     */
    public static <C, B extends SdkClientBuilder<B, C>> ProtocolTestHarness<C> forClient(String protocol,
                                                                                        Supplier<B> clientBuilder,
                                                                                        Consumer<? super B> configuration) {
        Validate.paramNotBlank(protocol, "protocol");
        Validate.paramNotNull(clientBuilder, "clientBuilder");
        Validate.paramNotNull(configuration, "configuration");
        return new LazyHarness<>(() -> factory().create(protocol, clientBuilder, configuration));
    }

    static ProtocolTestHarnessFactory factory() {
        return factory(ServiceLoader.load(ProtocolTestHarnessFactory.class,
                                          ProtocolTestHarnessFactory.class.getClassLoader()));
    }

    static ProtocolTestHarnessFactory factory(Iterable<ProtocolTestHarnessFactory> discovered) {
        List<ProtocolTestHarnessFactory> factories = new ArrayList<>();
        discovered.forEach(factories::add);
        if (factories.isEmpty()) {
            throw new IllegalStateException(
                "No " + ProtocolTestHarnessFactory.class.getName() + " is registered through "
                + "META-INF/services, so generated protocol tests cannot build a client. The transport-backed harness is "
                + "provided once the ClientTransport SPI exists.");
        }
        if (factories.size() > 1) {
            List<String> names = new ArrayList<>();
            factories.forEach(f -> names.add(f.getClass().getName()));
            throw new IllegalStateException("More than one " + ProtocolTestHarnessFactory.class.getName()
                                            + " is registered: " + names);
        }
        return factories.get(0);
    }

    /**
     * Creates its delegate on first use. A failed creation is not cached, so every test reports it.
     */
    private static final class LazyHarness<C> implements ProtocolTestHarness<C> {
        private final Supplier<ProtocolTestHarness<C>> creator;
        private volatile ProtocolTestHarness<C> delegate;

        private LazyHarness(Supplier<ProtocolTestHarness<C>> creator) {
            this.creator = creator;
        }

        private ProtocolTestHarness<C> delegate() {
            ProtocolTestHarness<C> result = delegate;
            if (result == null) {
                synchronized (this) {
                    result = delegate;
                    if (result == null) {
                        result = Validate.notNull(creator.get(), "The protocol test harness factory returned null");
                        delegate = result;
                    }
                }
            }
            return result;
        }

        @Override
        public CapturedRequest captureRequest(Consumer<? super C> call, String endpoint) {
            return delegate().captureRequest(call, endpoint);
        }

        @Override
        public <T> T respondWith(StubbedResponse response, Function<? super C, ? extends T> call) {
            return delegate().respondWith(response, call);
        }
    }
}
