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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.AwsClient;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.auth.AuthSchemePreferenceResolver;
import software.amazon.awssdk.awscore.client.builder.AwsAsyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsDefaultClientBuilder;
import software.amazon.awssdk.awscore.client.builder.AwsSyncClientBuilder;
import software.amazon.awssdk.awscore.client.builder.ClientProtocolSupport;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.awscore.client.handler.AwsAsyncClientHandler;
import software.amazon.awssdk.awscore.client.handler.AwsSyncClientHandler;
import software.amazon.awssdk.awscore.endpoint.AwsClientEndpointProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.AsyncClientHandler;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.client.handler.SyncClientHandler;
import software.amazon.awssdk.core.client.protocol.AsyncClientTransport;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.core.client.protocol.ClientTransport;
import software.amazon.awssdk.core.client.protocol.HttpMessageExchange;
import software.amazon.awssdk.core.client.protocol.MessageExchange;
import software.amazon.awssdk.core.client.protocol.ProtocolSettings;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.dynamicclient.internal.DynamicAuthSchemes;
import software.amazon.awssdk.dynamicclient.internal.DynamicCallResolvers;
import software.amazon.awssdk.dynamicclient.internal.DynamicProtocolSelection;
import software.amazon.awssdk.dynamicclient.internal.DynamicServiceClientConfigurationBuilder;
import software.amazon.awssdk.dynamicclient.internal.DynamicServiceMetadata;
import software.amazon.awssdk.dynamicclient.internal.ModelAuthSchemeResolver;
import software.amazon.awssdk.dynamicclient.internal.RulesEndpointProvider;
import software.amazon.awssdk.dynamicclient.schema.SchemaConverter;
import software.amazon.awssdk.dynamicclient.schema.StructDocument;
import software.amazon.awssdk.dynamicclient.schema.internal.ModelConversions;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4aAuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeProvider;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.regions.ServiceMetadataAdvancedOption;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.schema.ApiService;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
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
 * <p>Call an operation by name with {@link #call(String, Document)} or {@link #callAsync(String, Document)}. Input and
 * output are documents in the Smithy data model of the service: structure and member names are the ones the model
 * declares, not protocol-specific names such as {@code jsonName}. The input is coerced to the operation's input schema
 * with the rules of {@link #createStruct}, and the output is a structure document whose discriminator is the
 * operation's output ShapeId. Calls run through the same client handler and request pipeline as a generated client's,
 * with a {@link DynamicOperation} as the operation and {@link DynamicRequest} and {@link DynamicResponse} as the
 * request and response that interceptors see.
 *
 * <p>A dynamic client is both synchronous and
 * asynchronous, so it resolves the configuration of a generated synchronous client and of a generated asynchronous
 * client from the same builder: it uses the configured {@code SdkHttpClient} and {@code SdkAsyncHttpClient}, or creates
 * the default ones, and {@link #close()} closes what it created.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.dynamicclient.DynamicClient}.
 */
@SdkPublicApi
@ThreadSafe
public final class DynamicClient implements AwsClient {
    private static final Logger log = Logger.loggerFor(DynamicClient.class);

    private final Model model;
    private final ServiceShape serviceShape;
    private final SchemaConverter schemaConverter;
    private final ApiService service;
    private final String serviceName;
    private final String sdkId;
    private final DynamicAuthSchemes authSchemes;
    private final SdkClientConfiguration syncClientConfiguration;
    private final SdkClientConfiguration asyncClientConfiguration;
    private final SyncClientHandler syncClientHandler;
    private final AsyncClientHandler asyncClientHandler;

    /**
     * The operations in the service's closure, by shape name (with the service's renames applied).
     */
    private final Map<String, OperationShape> operationShapes;

    /**
     * The operations created so far, by name: each is created on first use and then reused (Requirement 28.9).
     */
    private final ConcurrentMap<String, DynamicOperation> operations = new ConcurrentHashMap<>();

    private DynamicClient(Model model,
                          DynamicServiceMetadata metadata,
                          SchemaConverter schemaConverter,
                          Schema serviceSchema,
                          DynamicAuthSchemes authSchemes,
                          SdkClientConfiguration syncClientConfiguration,
                          SdkClientConfiguration asyncClientConfiguration) {
        this.model = model;
        this.serviceShape = metadata.service();
        this.schemaConverter = schemaConverter;
        this.service = () -> serviceSchema;
        this.serviceName = metadata.signingName().isEmpty() ? metadata.endpointPrefix() : metadata.signingName();
        this.sdkId = metadata.sdkId();
        this.authSchemes = authSchemes;
        this.syncClientConfiguration = syncClientConfiguration;
        this.asyncClientConfiguration = asyncClientConfiguration;
        this.syncClientHandler = new AwsSyncClientHandler(syncClientConfiguration);
        this.asyncClientHandler = new AwsAsyncClientHandler(asyncClientConfiguration);
        Map<String, OperationShape> shapes = new HashMap<>();
        for (OperationShape operation : TopDownIndex.of(model).getContainedOperations(serviceShape)) {
            shapes.put(operation.getId().getName(serviceShape), operation);
        }
        this.operationShapes = Collections.unmodifiableMap(shapes);
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
        syncClientHandler.close();
        asyncClientHandler.close();
    }

    /**
     * Calls an operation with no input members: the same as {@link #call(String, Document)} with an empty map.
     *
     * @param operationName the shape name of the operation, case-sensitive
     * @return the output, a structure document whose discriminator is the operation's output ShapeId
     * @see #call(String, Document, AwsRequestOverrideConfiguration)
     */
    public Document call(String operationName) {
        return call(operationName, Document.EMPTY_MAP, null);
    }

    /**
     * Calls an operation.
     *
     * @param operationName the shape name of the operation, case-sensitive
     * @param input the input: a map or structure document keyed by Smithy member name
     * @return the output, a structure document whose discriminator is the operation's output ShapeId
     * @see #call(String, Document, AwsRequestOverrideConfiguration)
     */
    public Document call(String operationName, Document input) {
        return call(operationName, input, null);
    }

    /**
     * Calls an operation, with configuration that applies to this call only.
     *
     * <p>The input is coerced to the operation's input schema before any interceptor runs (see
     * {@link #createStruct}); interceptors see the coerced document in the {@link DynamicRequest} of
     * {@code context.request()}. The output is a structure document keyed by Smithy member name, whose
     * discriminator is the operation's output ShapeId; modeled members that the response does not contain are absent.
     *
     * @param operationName the shape name of the operation, case-sensitive
     * @param input the input: a map or structure document keyed by Smithy member name
     * @param overrideConfiguration the configuration of this call (the same options as a generated client's request
     *     override configuration), or null
     * @return the output
     * @throws IllegalArgumentException if the service has no operation of this name; no request is sent
     * @throws SerializationException if the input cannot be coerced to the operation's input schema; no request is sent
     * @throws software.amazon.awssdk.core.exception.SdkException if the call fails, as for a generated client
     */
    public Document call(String operationName, Document input, AwsRequestOverrideConfiguration overrideConfiguration) {
        DynamicOperation operation = getOperation(operationName);
        DynamicRequest request = request(operation, input, overrideConfiguration);
        return execute(operation, request).document();
    }

    /**
     * Calls an operation with no input members, asynchronously: the same as {@link #callAsync(String, Document)} with
     * an empty map.
     *
     * @param operationName the shape name of the operation, case-sensitive
     * @return a future of the output
     * @see #callAsync(String, Document, AwsRequestOverrideConfiguration)
     */
    public CompletableFuture<Document> callAsync(String operationName) {
        return callAsync(operationName, Document.EMPTY_MAP, null);
    }

    /**
     * Calls an operation asynchronously.
     *
     * @param operationName the shape name of the operation, case-sensitive
     * @param input the input: a map or structure document keyed by Smithy member name
     * @return a future of the output
     * @see #callAsync(String, Document, AwsRequestOverrideConfiguration)
     */
    public CompletableFuture<Document> callAsync(String operationName, Document input) {
        return callAsync(operationName, input, null);
    }

    /**
     * Calls an operation asynchronously, with configuration that applies to this call only. The call is the same as
     * {@link #call(String, Document, AwsRequestOverrideConfiguration)}, through the client's asynchronous
     * configuration.
     *
     * <p>This method does not throw: every failure, including an unknown operation and an input that cannot be
     * coerced, completes the returned future exceptionally (Requirement 28.12).
     *
     * @param operationName the shape name of the operation, case-sensitive
     * @param input the input: a map or structure document keyed by Smithy member name
     * @param overrideConfiguration the configuration of this call, or null
     * @return a future of the output
     */
    public CompletableFuture<Document> callAsync(String operationName, Document input,
                                                 AwsRequestOverrideConfiguration overrideConfiguration) {
        try {
            DynamicOperation operation = getOperation(operationName);
            DynamicRequest request = request(operation, input, overrideConfiguration);
            CompletableFuture<DynamicResponse> responseFuture = executeAsync(operation, request);
            CompletableFuture<Document> result = responseFuture.thenApply(DynamicResponse::document);
            return CompletableFutureUtils.forwardExceptionTo(result, responseFuture);
        } catch (Throwable t) {
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * Returns an operation of the service. The operation is created on the first call that names it, and the same
     * instance is returned afterwards; it is also the instance the request pipeline exposes to interceptors as
     * {@code SdkExecutionAttribute.API_OPERATION}.
     *
     * @param operationName the shape name of an operation bound to the service or to one of its resources,
     *     case-sensitive
     * @return the operation
     * @throws IllegalArgumentException if the service has no operation of this name
     */
    public DynamicOperation getOperation(String operationName) {
        Validate.paramNotNull(operationName, "operationName");
        DynamicOperation operation = operations.get(operationName);
        if (operation != null) {
            return operation;
        }
        OperationShape shape = operationShapes.get(operationName);
        if (shape == null) {
            throw new IllegalArgumentException("Operation " + operationName + " not found in service "
                                               + serviceShape.getId());
        }
        DynamicOperation created = DynamicOperation.create(shape, serviceShape, schemaConverter,
                                                           effectiveAuthSchemes(shape), service);
        DynamicOperation raced = operations.putIfAbsent(operationName, created);
        return raced != null ? raced : created;
    }

    /**
     * Creates a structure document of a structure in the model from a map or structure document, with the coercion
     * rules of a call's input:
     * <ul>
     *     <li>members are matched by Smithy member name, and a name that is not a member fails;</li>
     *     <li>each value must fit its member: a map or structure for a structure, a list for a list, a number for a
     *     number, and so on;</li>
     *     <li>byte, short, integer, intEnum, long and bigInteger members need an integral number in their range;</li>
     *     <li>a timestamp member takes a timestamp, a number of epoch seconds (with the fraction kept down to
     *     milliseconds) or an RFC 3339 {@code date-time} string;</li>
     *     <li>a blob member takes only a blob document ({@code Document.of(byte[])}); a string is rejected;</li>
     *     <li>a union may have only one member set.</li>
     * </ul>
     *
     * @param shapeId the ShapeId of a structure shape in the model
     * @param value a map or structure document keyed by Smithy member name
     * @return the structure document, whose discriminator is {@code shapeId}
     * @throws IllegalArgumentException if the model has no structure shape with this ShapeId
     * @throws SerializationException if the value cannot be coerced; the message names the member
     */
    public Document createStruct(ShapeId shapeId, Document value) {
        Validate.paramNotNull(shapeId, "shapeId");
        Validate.paramNotNull(value, "value");
        Shape shape = model.getShape(shapeId).orElse(null);
        if (shape == null) {
            throw new IllegalArgumentException("Shape " + shapeId + " is not in the model");
        }
        if (!shape.isStructureShape()) {
            throw new IllegalArgumentException("Shape " + shapeId + " is a " + shape.getType() + ", not a structure");
        }
        return StructDocument.coerce(schemaConverter.getSchema(shape), value, service.schema().id());
    }

    /**
     * Coerces the input to the operation's input schema, before any interceptor runs (Requirement 28.3).
     */
    private DynamicRequest request(DynamicOperation operation, Document input,
                                   AwsRequestOverrideConfiguration overrideConfiguration) {
        Validate.paramNotNull(input, "input");
        StructDocument document = StructDocument.coerce(operation.inputSchema(), input, service.schema().id());
        return DynamicRequest.builder().document(document).overrideConfiguration(overrideConfiguration).build();
    }

    /**
     * Runs a call through the client handler, as a generated synchronous client does.
     */
    private DynamicResponse execute(DynamicOperation operation, DynamicRequest request) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(request, syncClientConfiguration);
        List<MetricPublisher> metricPublishers =
            resolveMetricPublishers(clientConfiguration, request.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = apiCallMetricCollector(metricPublishers, operation);
        try {
            return syncClientHandler.execute(executionParams(operation, request, clientConfiguration,
                                                             apiCallMetricCollector));
        } finally {
            publish(metricPublishers, apiCallMetricCollector);
        }
    }

    /**
     * Runs a call through the asynchronous client handler, as a generated asynchronous client does.
     */
    private CompletableFuture<DynamicResponse> executeAsync(DynamicOperation operation, DynamicRequest request) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(request, asyncClientConfiguration);
        List<MetricPublisher> metricPublishers =
            resolveMetricPublishers(clientConfiguration, request.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = apiCallMetricCollector(metricPublishers, operation);
        try {
            CompletableFuture<DynamicResponse> executeFuture =
                asyncClientHandler.execute(executionParams(operation, request, clientConfiguration,
                                                           apiCallMetricCollector));
            CompletableFuture<DynamicResponse> whenCompleted =
                executeFuture.whenComplete((r, e) -> publish(metricPublishers, apiCallMetricCollector));
            return CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
        } catch (Throwable t) {
            publish(metricPublishers, apiCallMetricCollector);
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * The parameters of a call on the schema-based pipeline. {@code withOperation} routes the call to the pipeline that
     * generated clients use, with the operation as {@code SdkExecutionAttribute.API_OPERATION}.
     */
    private static ClientExecutionParams<DynamicRequest, DynamicResponse> executionParams(
        DynamicOperation operation, DynamicRequest request, SdkClientConfiguration clientConfiguration,
        MetricCollector apiCallMetricCollector) {
        return new ClientExecutionParams<DynamicRequest, DynamicResponse>()
            .withOperation(operation)
            .withRequestConfiguration(clientConfiguration)
            .withInput(request)
            .withMetricCollector(apiCallMetricCollector)
            .withAuthSchemeOptionsResolver(DynamicCallResolvers.authSchemeOptionsResolver())
            .withEndpointResolver(DynamicCallResolvers.endpointResolver());
    }

    private MetricCollector apiCallMetricCollector(List<MetricPublisher> metricPublishers, DynamicOperation operation) {
        MetricCollector collector = metricPublishers.isEmpty() ? NoOpMetricCollector.create()
                                                               : MetricCollector.create("ApiCall");
        collector.reportMetric(CoreMetric.SERVICE_ID, sdkId);
        collector.reportMetric(CoreMetric.OPERATION_NAME, operation.name());
        return collector;
    }

    private static void publish(List<MetricPublisher> metricPublishers, MetricCollector apiCallMetricCollector) {
        metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
    }

    /**
     * The metric publishers of the call's override configuration, else of the client, as a generated client resolves
     * them.
     */
    private static List<MetricPublisher> resolveMetricPublishers(SdkClientConfiguration clientConfiguration,
                                                                 RequestOverrideConfiguration overrideConfiguration) {
        List<MetricPublisher> publishers = null;
        if (overrideConfiguration != null) {
            publishers = overrideConfiguration.metricPublishers();
        }
        if (publishers == null || publishers.isEmpty()) {
            publishers = clientConfiguration.option(SdkClientOption.METRIC_PUBLISHERS);
        }
        return publishers == null ? Collections.emptyList() : publishers;
    }

    /**
     * Applies the plugins of the call's override configuration to a copy of the client configuration, as a generated
     * client does.
     */
    private static SdkClientConfiguration updateSdkClientConfiguration(DynamicRequest request,
                                                                       SdkClientConfiguration clientConfiguration) {
        List<SdkPlugin> plugins = request.overrideConfiguration().map(c -> c.plugins()).orElse(Collections.emptyList());
        return applyPlugins(clientConfiguration, plugins);
    }

    /**
     * Runs plugins on a {@link DynamicServiceClientConfiguration.Builder} that writes through to a copy of the
     * configuration, then applies their retry settings, as the generated builder and client do.
     */
    private static SdkClientConfiguration applyPlugins(SdkClientConfiguration config, List<SdkPlugin> plugins) {
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

    private List<software.amazon.awssdk.schema.ShapeId> effectiveAuthSchemes(OperationShape operation) {
        DynamicAuthSchemes.OperationAuth auth = authSchemes.operation(ModelConversions.shapeId(operation.getId()));
        List<software.amazon.awssdk.schema.ShapeId> ids = new ArrayList<>(auth.schemes().size());
        for (DynamicAuthSchemes.EffectiveAuthScheme scheme : auth.schemes()) {
            ids.add(software.amazon.awssdk.schema.ShapeId.from(scheme.schemeId()));
        }
        return ids;
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
         * <p>When no protocol is set, the client uses the first protocol of the SDK's priority list
         * ({@code rpcv2Cbor}, {@code awsJson1_0}, {@code awsJson1_1}, {@code restJson1}, {@code restXml},
         * {@code awsQuery}, {@code ec2Query}) whose trait the service carries and whose messages the client's transport
         * exchanges.
         *
         * @param protocol the protocol, or null to select one from the model
         * @return this builder
         */
        Builder protocol(ClientProtocol<?, ?> protocol);

        /**
         * Sets the auth scheme resolver of every call, in place of the auth schemes of the model.
         *
         * <p>When no resolver is set, each call uses the effective auth schemes of its operation in the model that
         * have an implementation: {@code aws.auth#sigv4}, {@code aws.auth#sigv4a} (with the
         * {@code software.amazon.awssdk:http-auth-aws-crt} module), {@code smithy.api#httpBearerAuth},
         * {@code smithy.api#noAuth}, and the schemes added with {@link #putAuthScheme}. {@link #build()} logs a warning
         * for each scheme of the model without one, and a call to an operation none of whose schemes has one fails
         * before a request is sent.
         *
         * @param authSchemeResolver the resolver, or null to use the auth schemes of the model
         * @return this builder
         */
        Builder authSchemeResolver(DynamicAuthSchemeResolver authSchemeResolver);

        /**
         * Sets the endpoint provider of every call, in place of the endpoint rules of the model.
         *
         * <p>When no provider is set, each call evaluates the service's {@code smithy.rules#endpointRuleSet} (or, if it
         * has none, its {@code smithy.rules#endpointBdd}) with the region, FIPS and dual-stack settings of the client
         * and the context parameters of the call. With an {@code endpointOverride}, every call goes to that endpoint
         * and the rules are not evaluated.
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
         * @throws IllegalStateException if no protocol is configured and the service has none of the SDK's protocol
         *     traits (the message lists the service's protocol traits, or says none were found), or if the protocol and
         *     the client's transport exchange different message types (the message names both)
         */
        @Override
        DynamicClient build();
    }

    private static final class DefaultBuilder extends AwsDefaultClientBuilder<Builder, DynamicClient> implements Builder {
        private final Map<String, AuthScheme<?>> additionalAuthSchemes = new HashMap<>();
        private Model model;
        private ShapeId serviceId;

        /**
         * The service of the client being built and what the configuration derives from its model, read by the
         * configuration hooks of {@link AwsDefaultClientBuilder}. Set only while {@link #buildClient()} runs.
         */
        private BuildState state;

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
            BuildState buildState = new BuildState(resolvedModel, resolvedService, serviceSchema);
            state = buildState;
            try {
                SdkClientConfiguration sync = syncClientConfiguration();
                SdkClientConfiguration async = null;
                try {
                    validateProtocol(sync, buildState);
                    async = asyncClientConfiguration();
                    validateProtocol(async, buildState);
                } catch (RuntimeException e) {
                    IoUtils.closeQuietlyV2(sync, null);
                    IoUtils.closeQuietlyV2(async, null);
                    throw e;
                }
                warnAboutMissingAuthSchemes(sync, resolvedService);
                return new DynamicClient(resolvedModel, resolvedService, converter, serviceSchema, buildState.authSchemes,
                                         sync, async);
            } finally {
                state = null;
            }
        }

        @Override
        protected String serviceEndpointPrefix() {
            return resolvedState().service.endpointPrefix();
        }

        @Override
        protected String signingName() {
            return resolvedState().service.signingName();
        }

        @Override
        protected String serviceName() {
            return resolvedState().service.serviceName();
        }

        /**
         * Adds the built-in auth schemes (the configured ones replace them) and the endpoint provider that evaluates the
         * endpoint rules of the model, as defaults: {@code merge} keeps a configured endpoint provider.
         */
        @Override
        protected SdkClientConfiguration mergeServiceDefaults(SdkClientConfiguration config) {
            BuildState resolved = resolvedState();
            Map<String, AuthScheme<?>> authSchemes =
                DynamicAuthSchemes.builtInAuthSchemes(DynamicAuthSchemes.sigv4aSignerAvailable());
            authSchemes.putAll(additionalAuthSchemes);
            return config.merge(c -> c.option(SdkClientOption.AUTH_SCHEMES, authSchemes)
                                      .option(SdkClientOption.ENDPOINT_PROVIDER, resolved.endpointProvider));
        }

        /**
         * Adds the service-specific endpoint override settings, as the generated builder does: the
         * {@code AWS_ENDPOINT_URL_<SDK ID>} environment variable, the {@code aws.endpointUrl<Service>} system property
         * and the service's section of the profile file. Then adds, unless configured, the auth scheme resolver of the
         * model and the protocol selected from the service's protocol traits. Both are lazy: a plugin that changes the
         * auth schemes or the transport changes what they resolve to.
         */
        @Override
        protected SdkClientConfiguration finalizeServiceConfiguration(SdkClientConfiguration config) {
            BuildState resolvedState = resolvedState();
            DynamicServiceMetadata resolved = resolvedState.service;
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
            DynamicAuthSchemes modelAuthSchemes = resolvedState.authSchemes;
            builder.lazyOptionIfAbsent(
                SdkClientOption.AUTH_SCHEME_PROVIDER,
                c -> new ModelAuthSchemeResolver(modelAuthSchemes, c.get(SdkClientOption.AUTH_SCHEMES).keySet(),
                                                 authSchemePreferences(c)));
            DynamicProtocolSelection protocolSelection = resolvedState.protocolSelection;
            ProtocolSettings protocolSettings = resolvedState.protocolSettings;
            builder.lazyOptionIfAbsent(SdkClientOption.CLIENT_PROTOCOL,
                                       c -> protocolSelection.select(protocolSettings, transportExchange(c)));
            return builder.build();
        }

        /**
         * Runs the plugins on a {@link DynamicServiceClientConfiguration.Builder} that writes through to the
         * configuration, as the generated builder does.
         */
        @Override
        protected SdkClientConfiguration invokePlugins(SdkClientConfiguration config) {
            return applyPlugins(config, plugins());
        }

        private BuildState resolvedState() {
            if (state == null) {
                throw new IllegalStateException("The service is resolved only while a client is being built");
            }
            return state;
        }

        /**
         * Fails the build when no protocol is configured and the service has none of the SDK's protocol traits
         * (Requirement 27.2), or when the protocol and the transport exchange different messages (Requirement 27.8).
         */
        private static void validateProtocol(SdkClientConfiguration config, BuildState buildState) {
            if (config.option(SdkClientOption.CLIENT_PROTOCOL) == null) {
                throw buildState.protocolSelection.noSupportedProtocol();
            }
            ClientProtocolSupport.validateMessageExchange(config);
        }

        /**
         * Logs one warning per effective auth scheme of the model that has no implementation, when the client uses the
         * auth scheme resolver of the model (Requirement 27.4). A configured resolver replaces the model's schemes, so
         * nothing is logged for it.
         */
        private static void warnAboutMissingAuthSchemes(SdkClientConfiguration config, DynamicServiceMetadata service) {
            AuthSchemeProvider provider = config.option(SdkClientOption.AUTH_SCHEME_PROVIDER);
            if (!(provider instanceof ModelAuthSchemeResolver)) {
                return;
            }
            ModelAuthSchemeResolver resolver = (ModelAuthSchemeResolver) provider;
            resolver.authSchemes().missingSchemes(resolver.implementedSchemes()).forEach((schemeId, operations) -> {
                log.warn(() -> missingAuthSchemeMessage(service, schemeId, operations));
            });
        }

        private static String missingAuthSchemeMessage(DynamicServiceMetadata service, String schemeId,
                                                       Set<String> operations) {
            StringBuilder message = new StringBuilder();
            message.append("The auth scheme ").append(schemeId).append(" of service ").append(service.service().getId())
                   .append(" has no implementation, so this dynamic client does not use it. ");
            if (operations.isEmpty()) {
                message.append("Every operation that lists it has another auth scheme. ");
            } else {
                message.append("Calls to these operations fail because they have no other auth scheme: ")
                       .append(operations).append(". ");
            }
            message.append("To use it, ");
            if (AwsV4aAuthScheme.SCHEME_ID.equals(schemeId)) {
                message.append("add the software.amazon.awssdk:http-auth-aws-crt dependency, or ");
            }
            message.append("register an implementation with DynamicClient.Builder.putAuthScheme.");
            return message.toString();
        }

        /**
         * The {@code auth_scheme_preference} of the profile file, as the generated builder's default auth scheme
         * provider reads it.
         */
        private static List<String> authSchemePreferences(AttributeMap.LazyValueSource config) {
            return AuthSchemePreferenceResolver.builder()
                                               .profileFile(config.get(SdkClientOption.PROFILE_FILE_SUPPLIER))
                                               .profileName(config.get(SdkClientOption.PROFILE_NAME))
                                               .build()
                                               .resolveAuthSchemePreference();
        }

        /**
         * The message exchange of the client's transport, as {@link ClientProtocolSupport#validateMessageExchange}
         * reads it: the configured transport of the client type, or the SDK HTTP client transport.
         */
        private static MessageExchange<?, ?> transportExchange(AttributeMap.LazyValueSource config) {
            if (config.get(SdkClientOption.CLIENT_TYPE) == ClientType.ASYNC) {
                AsyncClientTransport<?, ?> transport = config.get(SdkClientOption.ASYNC_CLIENT_TRANSPORT);
                return transport == null ? HttpMessageExchange.INSTANCE : transport.messageExchange();
            }
            ClientTransport<?, ?> transport = config.get(SdkClientOption.CLIENT_TRANSPORT);
            return transport == null ? HttpMessageExchange.INSTANCE : transport.messageExchange();
        }

    }

    /**
     * What one build derives from the model, shared by the synchronous and the asynchronous configuration.
     */
    private static final class BuildState {
        private final DynamicServiceMetadata service;
        private final DynamicProtocolSelection protocolSelection;
        private final ProtocolSettings protocolSettings;
        private final DynamicAuthSchemes authSchemes;
        private final RulesEndpointProvider endpointProvider;

        private BuildState(Model model, DynamicServiceMetadata service, Schema serviceSchema) {
            ServiceShape serviceShape = service.service();
            this.service = service;
            this.protocolSelection = DynamicProtocolSelection.forService(model, serviceShape);
            this.protocolSettings = ProtocolSettings.builder()
                                                    .serviceSchema(serviceSchema)
                                                    .serviceVersion(serviceShape.getVersion())
                                                    .baseExceptionBuilder(AwsServiceException::builder)
                                                    .build();
            this.authSchemes = DynamicAuthSchemes.forService(model, serviceShape);
            this.endpointProvider = RulesEndpointProvider.forService(model, serviceShape);
        }
    }
}
