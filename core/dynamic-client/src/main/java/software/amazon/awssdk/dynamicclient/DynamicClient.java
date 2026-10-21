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

package software.amazon.awssdk.dynamicclient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.AwsClient;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsDefaultClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.ClientProtocolSupport;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.awscore.endpoint.AwsClientEndpointProvider;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.dynamicclient.internal.DynamicServiceClientConfigurationBuilder;
import software.amazon.awssdk.dynamicclient.internal.DynamicServiceMetadata;
import software.amazon.awssdk.dynamicclient.schema.SchemaConverter;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.regions.ServiceMetadataAdvancedOption;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.schema.ApiService;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Validate;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * A client that calls a service described by a Smithy model loaded at runtime, without a code generation step.
 *
 * <p>Create one with {@link #builder()}. The builder takes the Smithy {@link Model} (for example one assembled from a
 * Smithy JSON AST file with {@code Model.assembler().addImport(path).assemble().unwrap()}) and, when the model contains
 * more than one service, the ShapeId of the service to call. Every other option is the same as on a generated client
 * builder (region, credentials provider, HTTP clients, override configuration, plugins, ...), and an option that is
 * not set resolves to the same default as on a generated client builder.
 *
 * <p>Input and output are documents in the Smithy data model of the service: structure and member names are the ones
 * the model declares, not protocol-specific names such as {@code jsonName}. A dynamic client is both synchronous and
 * asynchronous, so it resolves the configuration of a generated synchronous client and of a generated asynchronous
 * client from the same builder: it uses the configured {@code SdkHttpClient} and {@code SdkAsyncHttpClient}, or creates
 * the default ones, and {@link #close()} closes what it created.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.dynamicclient.DynamicClient}.
 */
@SdkPublicApi
@ThreadSafe
public final class DynamicClient implements AwsClient {
    private final Model model;
    private final ServiceShape serviceShape;
    private final SchemaConverter schemaConverter;
    private final ApiService service;
    private final String serviceName;
    private final SdkClientConfiguration syncClientConfiguration;
    private final SdkClientConfiguration asyncClientConfiguration;

    private DynamicClient(Model model,
                          DynamicServiceMetadata metadata,
                          SchemaConverter schemaConverter,
                          Schema serviceSchema,
                          SdkClientConfiguration syncClientConfiguration,
                          SdkClientConfiguration asyncClientConfiguration) {
        this.model = model;
        this.serviceShape = metadata.service();
        this.schemaConverter = schemaConverter;
        this.service = () -> serviceSchema;
        this.serviceName = metadata.signingName().isEmpty() ? metadata.endpointPrefix() : metadata.signingName();
        this.syncClientConfiguration = syncClientConfiguration;
        this.asyncClientConfiguration = asyncClientConfiguration;
    }

    /**
     * Creates a builder of dynamic clients.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * Returns the service this client calls. Its schema is the service's schema converted from the model, with the
     * service's ShapeId and shape type {@code service}; the same instance is returned on every call.
     *
     * @return the service
     */
    public ApiService service() {
        return service;
    }

    /**
     * Returns the name of the service: the signing name of the model's {@code aws.auth#sigv4} trait, as a generated
     * client's {@code SERVICE_NAME}, or the endpoint prefix when the model has no signing name.
     *
     * @return the service name, for example {@code sqs}
     */
    @Override
    public String serviceName() {
        return serviceName;
    }

    @Override
    public DynamicServiceClientConfiguration serviceClientConfiguration() {
        return new DynamicServiceClientConfigurationBuilder(syncClientConfiguration.toBuilder()).build();
    }

    /**
     * Closes the resources this client created: the HTTP clients, credentials providers and executors it resolved as
     * defaults. Resources that were configured on the builder are not closed.
     */
    @Override
    public void close() {
        syncClientConfiguration.close();
        asyncClientConfiguration.close();
    }

    Model model() {
        return model;
    }

    ServiceShape serviceShape() {
        return serviceShape;
    }

    SchemaConverter schemaConverter() {
        return schemaConverter;
    }

    SdkClientConfiguration syncClientConfiguration() {
        return syncClientConfiguration;
    }

    SdkClientConfiguration asyncClientConfiguration() {
        return asyncClientConfiguration;
    }

    /**
     * The builder of {@link DynamicClient}s. It has every option of a generated client builder, synchronous and
     * asynchronous, and the options below.
     *
     * <p>{@link #build()} resolves the service, then the client configuration. It throws if no model is set, or the
     * service cannot be resolved; a later change to the builder does not change a client it built.
     */
    @NotThreadSafe
    public interface Builder extends AwsClientBuilder<Builder, DynamicClient>,
                                     AwsSyncClientBuilder<Builder, DynamicClient>,
                                     AwsAsyncClientBuilder<Builder, DynamicClient> {
        /**
         * Sets the Smithy model of the service. Required.
         *
         * @param model the model, or null to unset it
         * @return this builder
         */
        Builder model(Model model);

        /**
         * Returns the model set on this builder.
         *
         * @return the model, or null when none is set
         */
        Model model();

        /**
         * Sets the ShapeId of the service to call. Required when the model contains more than one service; when it is
         * not set, the client calls the model's only service.
         *
         * @param serviceId the service ShapeId, or null to unset it
         * @return this builder
         */
        Builder serviceId(ShapeId serviceId);

        /**
         * Returns the service ShapeId set on this builder.
         *
         * @return the service ShapeId, or null when none is set
         */
        ShapeId serviceId();

        /**
         * Sets the protocol of every call, in place of the one selected from the protocol traits of the service.
         *
         * @param protocol the protocol, or null to select one from the model
         * @return this builder
         */
        Builder protocol(ClientProtocol<?, ?> protocol);

        /**
         * Sets the auth scheme resolver of every call, in place of the auth schemes of the model.
         *
         * @param authSchemeResolver the resolver, or null to use the auth schemes of the model
         * @return this builder
         */
        Builder authSchemeResolver(DynamicAuthSchemeResolver authSchemeResolver);

        /**
         * Sets the endpoint provider of every call, in place of the endpoint rules of the model.
         *
         * @param endpointProvider the provider, or null to use the endpoint rules of the model
         * @return this builder
         */
        Builder endpointProvider(DynamicEndpointProvider endpointProvider);

        /**
         * Builds a client.
         *
         * @return a new client
         * @throws NullPointerException if no model is set ({@code "A Smithy model is required"}), or no service ID is
         *     set and the model contains no service or several services; the message lists the services of the model
         * @throws IllegalArgumentException if the service ID does not name a service shape in the model; the message
         *     names the ID and lists the services of the model
         * @throws IllegalStateException if the configured protocol and the client's transport exchange different
         *     message types; the message names both
         */
        @Override
        DynamicClient build();
    }

    private static final class DefaultBuilder extends AwsDefaultClientBuilder<Builder, DynamicClient> implements Builder {
        private final Map<String, AuthScheme<?>> additionalAuthSchemes = new HashMap<>();
        private Model model;
        private ShapeId serviceId;

        /**
         * The service of the client being built, read by the configuration hooks of {@link AwsDefaultClientBuilder}.
         * Set only while {@link #buildClient()} runs.
         */
        private DynamicServiceMetadata service;

        @Override
        public Builder model(Model model) {
            this.model = model;
            return this;
        }

        @Override
        public Model model() {
            return model;
        }

        @Override
        public Builder serviceId(ShapeId serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        @Override
        public ShapeId serviceId() {
            return serviceId;
        }

        @Override
        public Builder protocol(ClientProtocol<?, ?> protocol) {
            clientConfiguration.option(SdkClientOption.CLIENT_PROTOCOL, protocol);
            return this;
        }

        @Override
        public Builder authSchemeResolver(DynamicAuthSchemeResolver authSchemeResolver) {
            clientConfiguration.option(SdkClientOption.AUTH_SCHEME_PROVIDER, authSchemeResolver);
            return this;
        }

        @Override
        public Builder endpointProvider(DynamicEndpointProvider endpointProvider) {
            clientConfiguration.option(SdkClientOption.ENDPOINT_PROVIDER, endpointProvider);
            return this;
        }

        @Override
        public Builder putAuthScheme(AuthScheme<?> authScheme) {
            additionalAuthSchemes.put(authScheme.schemeId(), authScheme);
            return this;
        }

        @Override
        protected DynamicClient buildClient() {
            Model resolvedModel = Validate.notNull(model, "A Smithy model is required");
            DynamicServiceMetadata resolvedService = DynamicServiceMetadata.resolve(resolvedModel, serviceId);
            SchemaConverter converter = new SchemaConverter(resolvedModel, resolvedService.service());
            Schema serviceSchema = converter.getSchema(resolvedService.service());
            service = resolvedService;
            try {
                SdkClientConfiguration sync = syncClientConfiguration();
                SdkClientConfiguration async = null;
                try {
                    ClientProtocolSupport.validateMessageExchange(sync);
                    async = asyncClientConfiguration();
                    ClientProtocolSupport.validateMessageExchange(async);
                } catch (RuntimeException e) {
                    IoUtils.closeQuietlyV2(sync, null);
                    IoUtils.closeQuietlyV2(async, null);
                    throw e;
                }
                return new DynamicClient(resolvedModel, resolvedService, converter, serviceSchema, sync, async);
            } finally {
                service = null;
            }
        }

        @Override
        protected String serviceEndpointPrefix() {
            return resolvedService().endpointPrefix();
        }

        @Override
        protected String signingName() {
            return resolvedService().signingName();
        }

        @Override
        protected String serviceName() {
            return resolvedService().serviceName();
        }

        @Override
        protected SdkClientConfiguration mergeServiceDefaults(SdkClientConfiguration config) {
            return config.merge(c -> c.option(SdkClientOption.AUTH_SCHEMES, new HashMap<>(additionalAuthSchemes)));
        }

        /**
         * Adds the service-specific endpoint override settings, as the generated builder does: the
         * {@code AWS_ENDPOINT_URL_<SDK ID>} environment variable, the {@code aws.endpointUrl<Service>} system property
         * and the service's section of the profile file.
         */
        @Override
        protected SdkClientConfiguration finalizeServiceConfiguration(SdkClientConfiguration config) {
            DynamicServiceMetadata resolved = resolvedService();
            SdkClientConfiguration.Builder builder = config.toBuilder();
            builder.lazyOptionIfAbsent(
                SdkClientOption.CLIENT_ENDPOINT_PROVIDER,
                c -> AwsClientEndpointProvider
                    .builder()
                    .serviceEndpointOverrideEnvironmentVariable("AWS_ENDPOINT_URL_"
                                                                + resolved.serviceNameForEnvironmentVariables())
                    .serviceEndpointOverrideSystemProperty("aws.endpointUrl" + resolved.serviceName())
                    .serviceProfileProperty(resolved.serviceNameForProfileFile())
                    .serviceEndpointPrefix(resolved.endpointPrefix())
                    .defaultProtocol("https")
                    .region(c.get(AwsClientOption.AWS_REGION))
                    .profileFile(c.get(SdkClientOption.PROFILE_FILE_SUPPLIER))
                    .profileName(c.get(SdkClientOption.PROFILE_NAME))
                    .putAdvancedOption(ServiceMetadataAdvancedOption.DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT,
                                       c.get(ServiceMetadataAdvancedOption.DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT))
                    .dualstackEnabled(c.get(AwsClientOption.DUALSTACK_ENDPOINT_ENABLED))
                    .fipsEnabled(c.get(AwsClientOption.FIPS_ENDPOINT_ENABLED))
                    .build());
            return builder.build();
        }

        /**
         * Runs the plugins on a {@link DynamicServiceClientConfiguration.Builder} that writes through to the
         * configuration, as the generated builder does.
         */
        @Override
        protected SdkClientConfiguration invokePlugins(SdkClientConfiguration config) {
            List<SdkPlugin> plugins = plugins();
            if (plugins.isEmpty()) {
                return config;
            }
            SdkClientConfiguration.Builder configuration = config.toBuilder();
            DynamicServiceClientConfigurationBuilder serviceConfigBuilder =
                new DynamicServiceClientConfigurationBuilder(configuration);
            for (SdkPlugin plugin : plugins) {
                plugin.configureClient(serviceConfigBuilder);
            }
            updateRetryStrategyClientConfiguration(configuration);
            return configuration.build();
        }

        private DynamicServiceMetadata resolvedService() {
            if (service == null) {
                throw new IllegalStateException("The service is resolved only while a client is being built");
            }
            return service;
        }

        /**
         * Applies the retry settings a plugin made through the override configuration, as the generated builder does.
         */
        private static void updateRetryStrategyClientConfiguration(SdkClientConfiguration.Builder configuration) {
            ClientOverrideConfiguration.Builder builder = configuration.asOverrideConfigurationBuilder();
            RetryMode retryMode = builder.retryMode();
            if (retryMode != null) {
                configuration.option(SdkClientOption.RETRY_STRATEGY, AwsRetryStrategy.forRetryMode(retryMode));
            } else {
                Consumer<RetryStrategy.Builder<?, ?>> configurator = builder.retryStrategyConfigurator();
                if (configurator != null) {
                    RetryStrategy.Builder<?, ?> defaultBuilder = AwsRetryStrategy.defaultRetryStrategy().toBuilder();
                    configurator.accept(defaultBuilder);
                    configuration.option(SdkClientOption.RETRY_STRATEGY, defaultBuilder.build());
                } else {
                    RetryStrategy retryStrategy = builder.retryStrategy();
                    if (retryStrategy != null) {
                        configuration.option(SdkClientOption.RETRY_STRATEGY, retryStrategy);
                    }
                }
            }
            configuration.option(SdkClientOption.CONFIGURED_RETRY_MODE, null);
            configuration.option(SdkClientOption.CONFIGURED_RETRY_STRATEGY, null);
            configuration.option(SdkClientOption.CONFIGURED_RETRY_CONFIGURATOR, null);
        }
    }
}
