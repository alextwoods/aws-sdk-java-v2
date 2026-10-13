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

package software.amazon.awssdk.core.client.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.traits.Trait;

class ClientProtocolFactoriesTest {
    private static final ShapeId OTHER_ID = ShapeId.from("com.example.test#otherProtocol");

    @Test
    void forId_registeredProtocol_returnsServiceLoaderFactory() {
        ClientProtocolFactory<?> factory = ClientProtocolFactories.forId(TestClientProtocolFactory.ID);

        assertThat(factory).isInstanceOf(TestClientProtocolFactory.class);
        assertThat(factory.id()).isEqualTo(TestClientProtocolFactory.ID);
    }

    @Test
    void forId_registeredProtocol_createsProtocolWithRequestedId() {
        ProtocolSettings settings = ProtocolSettings.builder().service(ShapeId.from("com.example#Service")).build();

        ClientProtocol<?, ?> protocol = createProtocol(ClientProtocolFactories.forId(TestClientProtocolFactory.ID),
                                                       settings);

        assertThat(protocol.id()).isEqualTo(TestClientProtocolFactory.ID);
        assertThat(protocol.messageExchange()).isSameAs(HttpMessageExchange.INSTANCE);
        assertThat(((TestClientProtocolFactory.TestClientProtocol) protocol).settings()).isSameAs(settings);
    }

    @Test
    void forId_calledRepeatedly_returnsSameInstance() {
        assertThat(ClientProtocolFactories.forId(TestClientProtocolFactory.ID))
            .isSameAs(ClientProtocolFactories.forId(TestClientProtocolFactory.ID));
    }

    @Test
    void forId_concurrentFirstCalls_allReturnSameInstance() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            Callable<ClientProtocolFactory<?>> lookup = () -> ClientProtocolFactories.forId(TestClientProtocolFactory.ID);
            Future<?>[] results = new Future<?>[16];
            for (int i = 0; i < results.length; i++) {
                results[i] = executor.submit(lookup);
            }
            Object first = results[0].get();
            for (Future<?> result : results) {
                assertThat(result.get()).isSameAs(first);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void forId_unregisteredProtocol_throwsSdkClientExceptionNamingId() {
        assertThatThrownBy(() -> ClientProtocolFactories.forId(OTHER_ID))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("No ClientProtocolFactory registered for com.example.test#otherProtocol");
    }

    @Test
    void forId_nullId_throwsNullPointerException() {
        assertThatThrownBy(() -> ClientProtocolFactories.forId(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("protocolId");
    }

    @Test
    void index_sameFactoryClassListedTwice_keepsFirst() {
        TestClientProtocolFactory first = new TestClientProtocolFactory();
        TestClientProtocolFactory second = new TestClientProtocolFactory();

        Map<ShapeId, ClientProtocolFactory<?>> index = ClientProtocolFactories.index(Arrays.asList(first, second));

        assertThat(index).hasSize(1);
        assertThat(ClientProtocolFactories.lookup(index, TestClientProtocolFactory.ID)).isSameAs(first);
    }

    @Test
    void index_twoFactoryClassesForSameId_throwsIllegalStateException() {
        ClientProtocolFactory<Trait> conflicting = new FixedIdFactory(TestClientProtocolFactory.ID);

        assertThatThrownBy(() -> ClientProtocolFactories.index(Arrays.asList(new TestClientProtocolFactory(), conflicting)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("More than one ClientProtocolFactory is registered for com.example.test#testProtocol")
            .hasMessageContaining(TestClientProtocolFactory.class.getName())
            .hasMessageContaining(FixedIdFactory.class.getName());
    }

    @Test
    void index_factoryWithNullId_throwsIllegalStateException() {
        assertThatThrownBy(() -> ClientProtocolFactories.index(Collections.singletonList(new FixedIdFactory(null))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(FixedIdFactory.class.getName() + " returned a null protocol ID");
    }

    @Test
    void index_distinctIds_indexesEachFactory() {
        TestClientProtocolFactory test = new TestClientProtocolFactory();
        FixedIdFactory other = new FixedIdFactory(OTHER_ID);

        Map<ShapeId, ClientProtocolFactory<?>> index = ClientProtocolFactories.index(Arrays.asList(test, other));

        assertThat(ClientProtocolFactories.lookup(index, TestClientProtocolFactory.ID)).isSameAs(test);
        assertThat(ClientProtocolFactories.lookup(index, OTHER_ID)).isSameAs(other);
        assertThatThrownBy(() -> index.put(OTHER_ID, test)).isInstanceOf(UnsupportedOperationException.class);
    }

    @SuppressWarnings("unchecked")
    private static ClientProtocol<?, ?> createProtocol(ClientProtocolFactory<?> factory, ProtocolSettings settings) {
        return ((ClientProtocolFactory<Trait>) factory).createProtocol(settings, null);
    }

    private static final class FixedIdFactory implements ClientProtocolFactory<Trait> {
        private final ShapeId id;

        private FixedIdFactory(ShapeId id) {
            this.id = id;
        }

        @Override
        public ShapeId id() {
            return id;
        }

        @Override
        public ClientProtocol<?, ?> createProtocol(ProtocolSettings settings, Trait protocolTrait) {
            throw new UnsupportedOperationException();
        }
    }
}
