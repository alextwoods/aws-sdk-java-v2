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

import java.util.function.Consumer;
import java.util.function.Supplier;
import software.amazon.awssdk.core.client.builder.SdkClientBuilder;

/**
 * Creates the {@link ProtocolTestHarness} that generated protocol tests run on. Implementations are discovered through
 * {@link java.util.ServiceLoader} by {@link ProtocolTestHarnesses}; exactly one must be registered in
 * {@code META-INF/services/software.amazon.awssdk.protocol.smithytests.ProtocolTestHarnessFactory} on the test class path.
 *
 * <p>An implementation builds a fresh client per call from {@code clientBuilder}, applies {@code configuration} (region,
 * credentials and retry settings chosen by the generator), then sets the endpoint and the capturing or fixed-response
 * transport itself.
 */
public interface ProtocolTestHarnessFactory {

    /**
     * Create a harness for clients of type {@code C}.
     *
     * @param protocol      the protocol ShapeId the test cases are written for, for example {@code aws.protocols#awsJson1_0}
     * @param clientBuilder creates a new client builder for each call
     * @param configuration the generator's client settings, applied to every new builder
     * @param <C>           the client type
     * @param <B>           the client builder type
     */
    <C, B extends SdkClientBuilder<B, C>> ProtocolTestHarness<C> create(String protocol,
                                                                       Supplier<B> clientBuilder,
                                                                       Consumer<? super B> configuration);
}
