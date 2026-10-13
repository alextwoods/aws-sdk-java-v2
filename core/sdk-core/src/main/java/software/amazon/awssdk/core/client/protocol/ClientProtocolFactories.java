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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.util.ClassLoaderHelper;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.utils.Validate;

/**
 * Finds {@link ClientProtocolFactory} implementations by protocol ID.
 *
 * <p>Factories are discovered with {@link ServiceLoader} from
 * {@code META-INF/services/software.amazon.awssdk.core.client.protocol.ClientProtocolFactory}, using the class loader
 * of the SDK (or, if it has none, the thread context class loader). Discovery runs once, on the first lookup; its
 * result is shared by all threads.
 *
 * <p>SDK addition: smithy-java has no protocol factory lookup.
 */
@SdkPublicApi
public final class ClientProtocolFactories {
    private static final Object LOCK = new Object();

    private static volatile Map<ShapeId, ClientProtocolFactory<?>> factories;

    private ClientProtocolFactories() {
    }

    /**
     * Returns the factory registered for a protocol.
     *
     * @param protocolId the shape ID of the protocol definition trait, for example {@code aws.protocols#restJson1}
     * @return the registered factory, whose {@link ClientProtocolFactory#id()} equals {@code protocolId}
     * @throws NullPointerException if {@code protocolId} is null
     * @throws SdkClientException if no factory is registered for {@code protocolId}
     * @throws IllegalStateException if the registered factories are invalid: a factory returns a null ID, or factories
     *     of two different classes are registered for the same ID
     */
    public static ClientProtocolFactory<?> forId(ShapeId protocolId) {
        Validate.paramNotNull(protocolId, "protocolId");
        return lookup(factories(), protocolId);
    }

    static ClientProtocolFactory<?> lookup(Map<ShapeId, ClientProtocolFactory<?>> index, ShapeId protocolId) {
        ClientProtocolFactory<?> factory = index.get(protocolId);
        if (factory == null) {
            throw SdkClientException.create("No ClientProtocolFactory registered for " + protocolId);
        }
        return factory;
    }

    /**
     * Indexes factories by protocol ID. A factory class listed more than once (for example by two copies of the same
     * jar) is kept once.
     */
    static Map<ShapeId, ClientProtocolFactory<?>> index(Iterable<? extends ClientProtocolFactory<?>> discovered) {
        Map<ShapeId, ClientProtocolFactory<?>> result = new HashMap<>();
        for (ClientProtocolFactory<?> factory : discovered) {
            ShapeId id = factory.id();
            if (id == null) {
                throw new IllegalStateException(factory.getClass().getName() + " returned a null protocol ID");
            }
            ClientProtocolFactory<?> existing = result.putIfAbsent(id, factory);
            if (existing != null && existing.getClass() != factory.getClass()) {
                throw new IllegalStateException("More than one ClientProtocolFactory is registered for " + id + ": "
                                                + existing.getClass().getName() + " and "
                                                + factory.getClass().getName());
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<ShapeId, ClientProtocolFactory<?>> factories() {
        Map<ShapeId, ClientProtocolFactory<?>> result = factories;
        if (result == null) {
            synchronized (LOCK) {
                result = factories;
                if (result == null) {
                    result = index(discover(ClassLoaderHelper.classLoader(ClientProtocolFactory.class)));
                    factories = result;
                }
            }
        }
        return result;
    }

    private static List<ClientProtocolFactory<?>> discover(ClassLoader classLoader) {
        List<ClientProtocolFactory<?>> discovered = new ArrayList<>();
        for (ClientProtocolFactory<?> factory : ServiceLoader.load(ClientProtocolFactory.class, classLoader)) {
            discovered.add(factory);
        }
        return discovered;
    }
}
