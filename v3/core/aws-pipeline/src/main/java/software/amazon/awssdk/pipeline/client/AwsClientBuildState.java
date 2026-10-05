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

package software.amazon.awssdk.pipeline.client;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.auth.api.AuthSchemeSelector;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.awscore.client.config.AwsClientSettings;
import software.amazon.awssdk.core.client.config.ClientConfigurationResolver;
import software.amazon.awssdk.core.client.config.ConfigurationSource;
import software.amazon.awssdk.core.client.config.ConfigurationSources;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.resource.ManagedResource;
import software.amazon.awssdk.endpoints.EndpointBuiltIn;
import software.amazon.awssdk.endpoints.binding.EndpointClientValues;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.provider.HttpClientResolver;
import software.amazon.awssdk.http.provider.ResolvedHttpClient;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.pipeline.AuthOptionsResolver;
import software.amazon.awssdk.pipeline.AwsCallEngine;
import software.amazon.awssdk.pipeline.AwsFeaturePlugin;
import software.amazon.awssdk.pipeline.ClientRuntime;
import software.amazon.awssdk.pipeline.ExecutionInterceptor;
import software.amazon.awssdk.protocols.core.InstalledProtocols;
import software.amazon.awssdk.protocols.core.ProtocolProvider;
import software.amazon.awssdk.protocols.core.ProtocolSelector;
import software.amazon.awssdk.protocols.core.SelectedProtocol;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.DefaultRetryScheduler;
import software.amazon.awssdk.retries.RetryScheduler;
import software.amazon.awssdk.retries.RetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;

/**
 * The mutable option state behind every generated {@code <Service>ClientBuilder} and
 * {@code <Service>AsyncClientBuilder}, and behind the dynamic client's builders (design "Client construction and
 * resolved configuration"; Requirements 7.2, 7.3, 7.11, 8.1, 14.14). Builders forward each V2-named setter here and
 * call {@link #buildSync} or {@link #buildAsync} exactly once per client, which:
 * <ol>
 *     <li>resolves the client configuration once through {@link ClientConfigurationResolver}
 *     (explicit builder values, then external sources, then SDK defaults), recording resource ownership;</li>
 *     <li>selects the service's protocol from its declared protocols and the installed (or explicit) protocol
 *     provider;</li>
 *     <li>resolves the HTTP client (caller-owned when passed built, SDK-owned when discovered);</li>
 *     <li>without an explicit credentials provider, creates the service's
 *     {@linkplain AwsServiceClientDefinition#defaultCredentials default} one (SDK-owned);</li>
 *     <li>builds the client's {@link ClientRuntime} and its {@link AwsCallEngine}.</li>
 * </ol>
 * If any step fails, every SDK-owned resource acquired so far is closed and caller-owned ones stay open.
 *
 * <p>Not thread safe; one instance per builder. The built {@link AwsClientParts} are immutable and thread safe.
 */
public final class AwsClientBuildState {
    private Region region;
    private IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider;
    private URI endpointOverride;
    private RetryStrategy retryStrategy;
    private SdkSyncHttpClient httpClient;
    private SdkAsyncHttpClient asyncHttpClient;
    private Executor completionExecutor;
    private RetryScheduler retryScheduler;
    private ProtocolProvider protocolProvider;
    private AuthSchemeSelector authSchemeSelector;
    private AuthOptionsResolver authOptions = AuthOptionsResolver.modeled();
    private ConfigurationSources sources = ConfigurationSources.system();
    private final List<ExecutionInterceptor> interceptors = new ArrayList<>();
    private final List<AwsFeaturePlugin> plugins = new ArrayList<>();

    public void region(Region region) {
        this.region = region;
    }

    public void credentialsProvider(IdentityProvider<? extends AwsCredentialsIdentity> credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    public void endpointOverride(URI endpointOverride) {
        this.endpointOverride = endpointOverride;
    }

    public void retryStrategy(RetryStrategy retryStrategy) {
        this.retryStrategy = retryStrategy;
    }

    /** A built sync HTTP client: caller-owned, never closed by the SDK. */
    public void httpClient(SdkSyncHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /** A built async HTTP client: caller-owned, never closed by the SDK. */
    public void asyncHttpClient(SdkAsyncHttpClient asyncHttpClient) {
        this.asyncHttpClient = asyncHttpClient;
    }

    /** The executor async calls complete on: caller-owned. Without one, the SDK creates and owns one. */
    public void completionExecutor(Executor completionExecutor) {
        this.completionExecutor = completionExecutor;
    }

    /**
     * The scheduler of async retry delays (for example {@link RetryScheduler#of}): caller-owned. Without one, the SDK
     * creates and owns a {@link DefaultRetryScheduler}.
     */
    public void retryScheduler(RetryScheduler retryScheduler) {
        this.retryScheduler = retryScheduler;
    }

    /** An explicit protocol implementation; without one, the installed protocol providers are discovered. */
    public void protocolProvider(ProtocolProvider protocolProvider) {
        this.protocolProvider = protocolProvider;
    }

    public void authSchemeSelector(AuthSchemeSelector authSchemeSelector) {
        this.authSchemeSelector = authSchemeSelector;
    }

    /**
     * The auth scheme candidates of each call, with their signer properties; generated AWS clients supply one over
     * {@code AwsAuthSchemeOptions}. Without one, the modeled schemes without signer properties.
     */
    public void authOptions(AuthOptionsResolver authOptions) {
        this.authOptions = Objects.requireNonNull(authOptions, "authOptions");
    }

    /** The external configuration sources (JVM properties, environment, profile); system sources by default. */
    public void configurationSources(ConfigurationSources sources) {
        this.sources = Objects.requireNonNull(sources, "sources");
    }

    public void addInterceptor(ExecutionInterceptor interceptor) {
        interceptors.add(Objects.requireNonNull(interceptor, "interceptor"));
    }

    /**
     * Appends a feature plugin of every plan of the client ({@link ClientRuntime.Builder#addPlugin}); plugins configure
     * plans in registration order, and a second plugin with the same ID fails the build.
     */
    public void addPlugin(AwsFeaturePlugin plugin) {
        plugins.add(Objects.requireNonNull(plugin, "plugin"));
    }

    /** Builds the runtime and engine of a sync client of {@code service}. */
    public AwsClientParts buildSync(AwsServiceClientDefinition service) {
        return build(service, HttpTransportMode.SYNC, false);
    }

    /** Builds the runtime and engine of an async client of {@code service}. */
    public AwsClientParts buildAsync(AwsServiceClientDefinition service) {
        return build(service, HttpTransportMode.ASYNC, false);
    }

    /**
     * Builds the runtime and {@linkplain AwsCallEngine.Builder#signingOnly() signing-only} engine of a presigner of
     * {@code service} (Requirement 18.3): the same configuration, protocol, endpoint, auth-scheme and identity
     * resolution as a sync client, without a transport, so no HTTP client is resolved or created. A configured
     * HTTP client is ignored.
     */
    public AwsClientParts buildPresigner(AwsServiceClientDefinition service) {
        return build(service, HttpTransportMode.SYNC, true);
    }

    private AwsClientParts build(AwsServiceClientDefinition service, HttpTransportMode mode, boolean signingOnly) {
        Objects.requireNonNull(service, "service");
        List<AutoCloseable> acquired = new ArrayList<>();
        try {
            SelectedProtocol protocol = protocolProvider != null
                                        ? ProtocolSelector.select(service.protocols(), protocolProvider)
                                        : ProtocolSelector.select(service.protocols(), InstalledProtocols.discover(
                                            AwsClientBuildState.class.getClassLoader()));
            SdkClientConfiguration.Builder values = SdkClientConfiguration.builder();
            values.option(SdkClientOption.PROTOCOL_ID, protocol.protocolId());
            if (region != null) {
                values.option(AwsClientOption.AWS_REGION, region);
            }
            if (credentialsProvider != null) {
                values.option(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER, credentialsProvider);
            }
            if (endpointOverride != null) {
                values.option(SdkClientOption.CLIENT_ENDPOINT, endpointOverride);
            }

            SdkSyncHttpClient sync = null;
            SdkAsyncHttpClient async = null;
            if (signingOnly) {
                // A presigner transmits nothing, so it has no transport.
            } else if (mode == HttpTransportMode.SYNC) {
                ResolvedHttpClient<SdkSyncHttpClient> resolved = HttpClientResolver.sync().client(httpClient).resolve();
                acquired.add(resolved);
                values.resources(resolved.resources().resources());
                sync = resolved.client();
            } else {
                ResolvedHttpClient<SdkAsyncHttpClient> resolved =
                    HttpClientResolver.async().client(asyncHttpClient).resolve();
                acquired.add(resolved);
                values.resources(resolved.resources().resources());
                async = resolved.client();
            }

            // Without an explicit provider, the service's default chain (if it has one) over the client's sources.
            // Added after the HTTP resources, which replace the resource list.
            if (credentialsProvider == null) {
                service.defaultCredentialsFactory().ifPresent(factory -> {
                    IdentityProvider<? extends AwsCredentialsIdentity> defaults =
                        Objects.requireNonNull(factory.apply(sources), "default credentials provider");
                    if (defaults instanceof AutoCloseable closeable) {
                        acquired.add(closeable);
                        values.addResource(ManagedResource.sdkOwned(defaults));
                    }
                    // An SDK default, so SDK-owned: a builder value would make the option caller-owned.
                    values.option(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER, defaults, ConfigurationSource.DEFAULT);
                });
            }

            ClientRuntime.Builder runtime = ClientRuntime.builder();
            if (mode == HttpTransportMode.ASYNC) {
                Executor executor = completionExecutor;
                if (executor == null) {
                    ExecutorService owned = Executors.newFixedThreadPool(
                        Math.max(2, Runtime.getRuntime().availableProcessors()),
                        daemonThreads(service.serviceName() + "-completion"));
                    acquired.add(owned::shutdownNow);
                    values.addResource(ManagedResource.sdkOwned(owned));
                    executor = owned;
                } else {
                    values.addResource(ManagedResource.callerOwned(executor));
                }
                RetryScheduler scheduler = retryScheduler;
                if (scheduler == null) {
                    DefaultRetryScheduler owned = DefaultRetryScheduler.create(service.serviceName() + "-retry",
                                                                               executor);
                    acquired.add(owned);
                    values.addResource(ManagedResource.sdkOwned(owned));
                    scheduler = owned;
                } else {
                    values.addResource(ManagedResource.callerOwned(scheduler));
                }
                runtime.completionExecutor(executor).retryScheduler(scheduler);
            }

            // Client-constant configuration is resolved exactly once, here.
            SdkClientConfiguration configuration = ClientConfigurationResolver.builder()
                                                                              .sources(sources)
                                                                              .addSetting(AwsClientSettings.REGION)
                                                                              .build()
                                                                              .resolve(values.build());

            runtime.configuration(configuration)
                   .protocol(protocol.createClientProtocol(service.typeRegistry(), true))
                   .transportMode(mode)
                   .transportRequirements(protocol.transportRequirements(), false)
                   .retryStrategy(retryStrategy != null ? retryStrategy : StandardRetryStrategy.create(),
                                  service.serviceName())
                   .authSchemeSelector(authSchemeSelector);
            service.rules().ifPresent(runtime::endpointRules);
            runtime.endpointClientValues(endpointValues(configuration));
            interceptors.forEach(runtime::addInterceptor);
            plugins.forEach(runtime::addPlugin);
            ClientRuntime built = runtime.build();
            acquired.clear();
            acquired.add(built);

            IdentityProviders.Builder identities = IdentityProviders.builder();
            configuration.get(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER)
                         .ifPresent(identities::putIdentityProvider);
            AwsCallEngine.Builder engine = AwsCallEngine.builder()
                                                        .runtime(built)
                                                        .syncHttpClient(sync)
                                                        .asyncHttpClient(async)
                                                        .identityProviders(identities.build())
                                                        .authOptions(authOptions);
            if (signingOnly) {
                engine.signingOnly();
            }
            return new AwsClientParts(service, built, engine.build());
        } catch (RuntimeException | Error e) {
            for (AutoCloseable resource : acquired) {
                try {
                    resource.close();
                } catch (Exception closeFailure) {
                    e.addSuppressed(closeFailure);
                }
            }
            throw e;
        }
    }

    private static EndpointClientValues endpointValues(SdkClientConfiguration configuration) {
        EndpointClientValues.Builder values = EndpointClientValues.builder();
        configuration.get(AwsClientOption.AWS_REGION).ifPresent(r -> values.builtIn(EndpointBuiltIn.REGION, r.id()));
        configuration.get(SdkClientOption.CLIENT_ENDPOINT)
                     .ifPresent(e -> values.builtIn(EndpointBuiltIn.ENDPOINT, e.toString()));
        return values.build();
    }

    private static ThreadFactory daemonThreads(String prefix) {
        AtomicInteger count = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + "-" + count.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
