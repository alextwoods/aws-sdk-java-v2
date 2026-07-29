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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.internal.util.ClassLoaderHelper;
import software.amazon.awssdk.utils.Logger;

/**
 * Discovers {@link SdkPipelineProvider} implementations via {@link ServiceLoader} and creates
 * the highest-priority available {@link SdkPipeline} for a given client configuration.
 *
 * <p>Discovery happens once per call to {@link #loadPipeline}. The loader finds all registered
 * providers, filters to those that are {@linkplain SdkPipelineProvider#isAvailable() available},
 * sorts by {@linkplain SdkPipelineProvider#priority() priority} (lowest wins), and asks the
 * winner to create a pipeline.
 *
 * <p>If no provider is found, returns {@link Optional#empty()} — the caller (generated client)
 * falls back to the default v2 {@link SyncClientHandler} path.
 */
@SdkInternalApi
public final class SdkPipelineLoader {

    private static final Logger log = Logger.loggerFor(SdkPipelineLoader.class);

    private static final SdkPipelineLoader INSTANCE = new SdkPipelineLoader();

    private SdkPipelineLoader() {
    }

    public static SdkPipelineLoader instance() {
        return INSTANCE;
    }

    /**
     * Discover and create the highest-priority pipeline for the given client configuration.
     *
     * @param clientConfiguration The fully-resolved client configuration.
     * @return An {@link SdkPipeline} if a provider was found and is available, otherwise empty.
     */
    public Optional<SdkPipeline> loadPipeline(SdkClientConfiguration clientConfiguration) {
        // If an explicit pipeline is set in the config, use it directly (bypasses SPI discovery)
        SdkPipeline explicitPipeline = clientConfiguration.option(SdkClientOption.SDK_PIPELINE);
        if (explicitPipeline != null) {
            log.debug(() -> "Using explicitly configured SdkPipeline: " + explicitPipeline.getClass().getName());
            return Optional.of(explicitPipeline);
        }

        List<SdkPipelineProvider> providers = discoverProviders();

        if (providers.isEmpty()) {
            log.debug(() -> "No SdkPipelineProvider implementations found on the classpath. "
                            + "Using default v2 pipeline.");
            return Optional.empty();
        }

        // Sort by priority (lowest value = highest priority)
        providers.sort(Comparator.comparingInt(SdkPipelineProvider::priority));

        for (SdkPipelineProvider provider : providers) {
            if (provider.isAvailable()) {
                log.debug(() -> "Using SdkPipelineProvider: " + provider.getClass().getName()
                                + " (priority=" + provider.priority() + ")");
                try {
                    SdkPipeline pipeline = provider.createPipeline(clientConfiguration);
                    return Optional.of(pipeline);
                } catch (Exception e) {
                    log.warn(() -> "SdkPipelineProvider " + provider.getClass().getName()
                                   + " failed to create pipeline, trying next provider.", e);
                }
            } else {
                log.debug(() -> "SdkPipelineProvider " + provider.getClass().getName()
                                + " is not available, skipping.");
            }
        }

        log.debug(() -> "No available SdkPipelineProvider could create a pipeline. "
                        + "Using default v2 pipeline.");
        return Optional.empty();
    }

    private List<SdkPipelineProvider> discoverProviders() {
        List<SdkPipelineProvider> providers = new ArrayList<>();
        try {
            ServiceLoader<SdkPipelineProvider> serviceLoader = ServiceLoader.load(
                SdkPipelineProvider.class,
                ClassLoaderHelper.classLoader(SdkPipelineLoader.class));
            for (SdkPipelineProvider provider : serviceLoader) {
                providers.add(provider);
            }
        } catch (Exception e) {
            log.debug(() -> "Error discovering SdkPipelineProvider implementations.", e);
        }
        return providers;
    }

    /**
     * Test-only: create a pipeline from an explicit provider (bypasses ServiceLoader discovery).
     */
    @SdkTestInternalApi
    public static Optional<SdkPipeline> loadPipeline(SdkPipelineProvider provider,
                                                     SdkClientConfiguration clientConfiguration) {
        if (provider.isAvailable()) {
            return Optional.of(provider.createPipeline(clientConfiguration));
        }
        return Optional.empty();
    }
}
