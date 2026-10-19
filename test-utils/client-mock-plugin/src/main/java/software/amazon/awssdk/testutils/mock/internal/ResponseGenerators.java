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

package software.amazon.awssdk.testutils.mock.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.testutils.mock.ResponseGenerator;
import software.amazon.awssdk.utils.Lazy;
import software.amazon.awssdk.utils.Validate;

/**
 * Loads the {@link ResponseGenerator} implementations once, with {@link ServiceLoader}, keyed by protocol ID. The first
 * implementation found for a protocol wins.
 */
@SdkInternalApi
public final class ResponseGenerators {
    private static final Lazy<Map<ShapeId, ResponseGenerator>> GENERATORS = new Lazy<>(ResponseGenerators::load);

    private ResponseGenerators() {
    }

    /**
     * Returns the generator registered for a protocol.
     *
     * @param protocolId the protocol trait ID
     * @return the generator
     * @throws NullPointerException if {@code protocolId} is null
     * @throws IllegalArgumentException if no generator is registered for the protocol
     */
    public static ResponseGenerator forProtocol(ShapeId protocolId) {
        Validate.paramNotNull(protocolId, "protocolId");
        Map<ShapeId, ResponseGenerator> generators = GENERATORS.getValue();
        ResponseGenerator generator = generators.get(protocolId);
        if (generator == null) {
            throw new IllegalArgumentException("No ResponseGenerator is registered for protocol " + protocolId
                                               + ". Registered protocols: " + generators.keySet());
        }
        return generator;
    }

    private static Map<ShapeId, ResponseGenerator> load() {
        Map<ShapeId, ResponseGenerator> generators = new LinkedHashMap<>();
        for (ResponseGenerator generator : ServiceLoader.load(ResponseGenerator.class,
                                                              ResponseGenerator.class.getClassLoader())) {
            generators.putIfAbsent(generator.protocolId(), generator);
        }
        return Collections.unmodifiableMap(generators);
    }
}
