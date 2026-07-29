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

package software.amazon.awssdk.core.client.handler;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;

/**
 * Service Provider Interface for SDK execution pipeline implementations. Discovered via
 * {@link java.util.ServiceLoader} — implementations must be declared in
 * {@code META-INF/services/software.amazon.awssdk.core.client.handler.SdkPipelineProvider}.
 *
 * <p>When multiple providers are found on the classpath, the one with the <b>lowest</b>
 * {@link #priority()} value wins (priority 0 beats priority 100). The default v2 pipeline has
 * {@code priority = Integer.MAX_VALUE}, so any alternative implementation that registers itself
 * will take precedence.
 *
 * <h2>Contract</h2>
 * <ul>
 *   <li>{@link #createPipeline} is called once per client instance. The returned {@link SdkPipeline}
 *       is reused for the lifetime of that client.</li>
 *   <li>The provider must be stateless and cheap to instantiate (ServiceLoader creates it via
 *       the no-arg constructor).</li>
 *   <li>If a provider cannot operate in the current environment (e.g., missing JDK version or
 *       missing dependencies), it should return {@code false} from {@link #isAvailable()} rather
 *       than throwing.</li>
 * </ul>
 *
 * <h2>Example registration (smithy-java bridge)</h2>
 * <pre>
 * // META-INF/services/software.amazon.awssdk.core.client.handler.SdkPipelineProvider
 * software.amazon.awssdk.bridge.smithyjava.SmithyJavaPipelineProvider
 * </pre>
 */
@ThreadSafe
@SdkProtectedApi
public interface SdkPipelineProvider {

    /**
     * Priority of this provider. Lower values are higher priority. The default v2 pipeline
     * uses {@code Integer.MAX_VALUE}. A smithy-java-backed pipeline might use {@code 0} to
     * always win when present.
     */
    int priority();

    /**
     * Whether this provider is available in the current runtime environment. Called once during
     * discovery. If this returns {@code false}, the provider is skipped and the next-highest-priority
     * provider is tried.
     *
     * <p>Typical checks: required JDK version, required classes on classpath, feature flags.
     */
    default boolean isAvailable() {
        return true;
    }

    /**
     * Create a pipeline instance for the given client configuration. Called once during client
     * construction.
     *
     * @param clientConfiguration The fully-resolved client configuration, including HTTP client,
     *     credentials provider, region, endpoint, retry strategy, interceptors, etc.
     * @return A pipeline instance that will handle all operations for this client.
     */
    SdkPipeline createPipeline(SdkClientConfiguration clientConfiguration);
}
