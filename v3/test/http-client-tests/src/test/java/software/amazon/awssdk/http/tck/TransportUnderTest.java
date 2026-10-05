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

package software.amazon.awssdk.http.tck;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import software.amazon.awssdk.http.HttpTransportMode;

/**
 * Describes one TCK execution: a transport in one mode, how to build a client of it for given {@link ClientSettings},
 * and the {@link Deviation}s its implementation documents. What the client supports is not described here: the
 * suites read it from the built client's own {@code capabilities()}, so the TCK checks what the transport declares.
 *
 * @param <C> {@code SdkSyncHttpClient} or {@code SdkAsyncHttpClient}
 */
record TransportUnderTest<C extends AutoCloseable>(String name, HttpTransportMode mode,
                                                   Function<ClientSettings, C> factory, Set<Deviation> deviations) {

    TransportUnderTest {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(factory, "factory");
        deviations = deviations.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(deviations));
    }

    static <C extends AutoCloseable> TransportUnderTest<C> of(String name, HttpTransportMode mode,
                                                             Function<ClientSettings, C> factory,
                                                             Deviation... deviations) {
        return new TransportUnderTest<>(name, mode, factory, Set.of(deviations));
    }

    C create(ClientSettings settings) {
        return factory.apply(settings);
    }

    boolean documents(Deviation deviation) {
        return deviations.contains(deviation);
    }
}
