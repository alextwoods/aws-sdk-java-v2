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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.handler.SdkPipeline;
import software.amazon.awssdk.core.client.handler.SdkPipelineProvider;

/**
 * {@link SdkPipelineProvider} implementation that creates a smithy-java-backed execution
 * pipeline. When this module ({@code smithy-java-bridge}) is on the classpath, service
 * clients whose POJOs implement {@code SerializableStruct} (i.e., have smithy-java serde
 * generated) will automatically use the smithy-java protocol pipeline for serialization,
 * transport, and deserialization.
 *
 * <p>This provider has priority 0 (highest), so it always wins over the default v2 pipeline
 * when present. It is always available — the act of having this jar on the classpath is the
 * opt-in signal.
 *
 * <p>Operations that the smithy-java pipeline cannot handle (e.g., those whose input does
 * not implement {@code SerializableStruct}, or streaming operations) will fall back to the
 * v2 pipeline via {@link SmithyJavaPipeline#supportsOperation}.
 */
@SdkPublicApi
public final class SmithyJavaPipelineProvider implements SdkPipelineProvider {

    /**
     * No-arg constructor required by {@link java.util.ServiceLoader}.
     */
    public SmithyJavaPipelineProvider() {
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public boolean isAvailable() {
        // The presence of this jar implies availability. We do a quick check that
        // smithy-java core classes are loadable (they're a transitive dependency of this module).
        try {
            Class.forName("software.amazon.smithy.java.core.schema.SerializableStruct");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public SdkPipeline createPipeline(SdkClientConfiguration clientConfiguration) {
        return new SmithyJavaPipeline(clientConfiguration);
    }
}
