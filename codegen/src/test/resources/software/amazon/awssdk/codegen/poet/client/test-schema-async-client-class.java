package software.amazon.awssdk.services.demo;

import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.client.handler.AwsAsyncClientHandler;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointProviderUtils;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.awscore.eventstream.EventStreamAsyncResponseTransformer;
import software.amazon.awssdk.awscore.eventstream.EventStreamTaggedUnionPojoSupplier;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.internal.AwsProtocolMetadata;
import software.amazon.awssdk.awscore.internal.AwsServiceProtocol;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkPojoBuilder;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.AsyncClientHandler;
import software.amazon.awssdk.core.client.handler.AttachHttpMetadataResponseHandler;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksumRequired;
import software.amazon.awssdk.core.internal.interceptor.trait.RequestCompression;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.protocol.VoidSdkResponse;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.protocols.core.ExceptionMetadata;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.BaseAwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.json.JsonOperationMetadata;
import software.amazon.awssdk.protocols.rpcv2.SmithyRpcV2CborProtocolFactory;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.services.demo.auth.scheme.DemoAuthSchemeParams;
import software.amazon.awssdk.services.demo.auth.scheme.DemoAuthSchemeProvider;
import software.amazon.awssdk.services.demo.endpoints.DemoEndpointParams;
import software.amazon.awssdk.services.demo.endpoints.DemoEndpointProvider;
import software.amazon.awssdk.services.demo.endpoints.internal.DemoEndpointResolverUtils;
import software.amazon.awssdk.services.demo.internal.DemoServiceClientConfigurationBuilder;
import software.amazon.awssdk.services.demo.internal.ServiceVersionInfo;
import software.amazon.awssdk.services.demo.model.CountThingsRequest;
import software.amazon.awssdk.services.demo.model.CountThingsResponse;
import software.amazon.awssdk.services.demo.model.DemoException;
import software.amazon.awssdk.services.demo.model.DemoOperations;
import software.amazon.awssdk.services.demo.model.GetThingRequest;
import software.amazon.awssdk.services.demo.model.GetThingResponse;
import software.amazon.awssdk.services.demo.model.InternalException;
import software.amazon.awssdk.services.demo.model.ListThingsRequest;
import software.amazon.awssdk.services.demo.model.ListThingsResponse;
import software.amazon.awssdk.services.demo.model.PutThingRequest;
import software.amazon.awssdk.services.demo.model.PutThingResponse;
import software.amazon.awssdk.services.demo.model.ThingEvents;
import software.amazon.awssdk.services.demo.model.ThingNotFoundException;
import software.amazon.awssdk.services.demo.model.WatchThingsRequest;
import software.amazon.awssdk.services.demo.model.WatchThingsResponse;
import software.amazon.awssdk.services.demo.model.WatchThingsResponseHandler;
import software.amazon.awssdk.services.demo.transform.CountThingsRequestMarshaller;
import software.amazon.awssdk.services.demo.transform.WatchThingsRequestMarshaller;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Internal implementation of {@link DemoAsyncClient}.
 *
 * @see DemoAsyncClient#builder()
 */
@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
final class DefaultDemoAsyncClient implements DemoAsyncClient {
    private static final Logger log = LoggerFactory.getLogger(DefaultDemoAsyncClient.class);

    private static final AwsProtocolMetadata protocolMetadata = AwsProtocolMetadata.builder()
            .serviceProtocol(AwsServiceProtocol.SMITHY_RPC_V2_CBOR).build();

    private final AsyncClientHandler clientHandler;

    private final SmithyRpcV2CborProtocolFactory protocolFactory;

    private final SdkClientConfiguration clientConfiguration;

    private final Executor executor;

    protected DefaultDemoAsyncClient(SdkClientConfiguration clientConfiguration) {
        this.clientHandler = new AwsAsyncClientHandler(clientConfiguration);
        this.clientConfiguration = clientConfiguration.toBuilder().option(SdkClientOption.SDK_CLIENT, this)
                .option(SdkClientOption.API_METADATA, "Demo" + "#" + ServiceVersionInfo.VERSION).build();
        this.protocolFactory = init(SmithyRpcV2CborProtocolFactory.builder()).build();
        this.executor = clientConfiguration.option(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR);
    }

    /**
     * An operation whose input class has no schema serde members (a BigInteger member), which stays on the legacy path.
     *
     * @param countThingsRequest
     * @return A Java Future containing the result of the CountThings operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>InternalException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>DemoException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample DemoAsyncClient.CountThings
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/demo-2024-01-01/CountThings" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public CompletableFuture<CountThingsResponse> countThings(CountThingsRequest countThingsRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(countThingsRequest, this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, countThingsRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Demo");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "CountThings");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<CountThingsResponse> responseHandler = protocolFactory.createResponseHandler(operationMetadata,
                    CountThingsResponse::builder);
            Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
                if (errorCode == null) {
                    return Optional.empty();
                }
                switch (errorCode) {
                case "ThingNotFound":
                    return Optional.of(ExceptionMetadata.builder().errorCode("ThingNotFound").httpStatusCode(404)
                            .exceptionBuilderSupplier(ThingNotFoundException::builder).build());
                case "InternalFault":
                    return Optional.of(ExceptionMetadata.builder().errorCode("InternalFault")
                            .exceptionBuilderSupplier(InternalException::builder).build());
                default:
                    return Optional.empty();
                }
            };
            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata, exceptionMetadataMapper);

            CompletableFuture<CountThingsResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<CountThingsRequest, CountThingsResponse>()
                            .withOperationName("CountThings").withProtocolMetadata(protocolMetadata)
                            .withMarshaller(new CountThingsRequestMarshaller(protocolFactory))
                            .withResponseHandler(responseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                            .withAuthSchemeOptionsResolver(this::resolveAuthSchemeOptions)
                            .withEndpointResolver(this::resolveEndpoint).withInput(countThingsRequest));
            CompletableFuture<CountThingsResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * A plain operation with a modeled error and a host prefix.
     *
     * @param getThingRequest
     * @return A Java Future containing the result of the GetThing operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>InternalException</li>
     *         <li>ThingNotFoundException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>DemoException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample DemoAsyncClient.GetThing
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/demo-2024-01-01/GetThing" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public CompletableFuture<GetThingResponse> getThing(GetThingRequest getThingRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(getThingRequest, this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, getThingRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Demo");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "GetThing");
            String hostPrefix = "data.";
            String resolvedHostExpression = "data.";

            CompletableFuture<GetThingResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<GetThingRequest, GetThingResponse>()
                            .withOperation(DemoOperations.GET_THING).withProtocolMetadata(protocolMetadata)
                            .hostPrefixExpression(resolvedHostExpression).withRequestConfiguration(clientConfiguration)
                            .withInput(getThingRequest).withMetricCollector(apiCallMetricCollector)
                            .withAuthSchemeOptionsResolver(this::resolveAuthSchemeOptions)
                            .withEndpointResolver(this::resolveEndpoint));
            CompletableFuture<GetThingResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * Invokes the ListThings operation asynchronously.
     *
     * @param listThingsRequest
     * @return A Java Future containing the result of the ListThings operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>InternalException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>DemoException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample DemoAsyncClient.ListThings
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/demo-2024-01-01/ListThings" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public CompletableFuture<ListThingsResponse> listThings(ListThingsRequest listThingsRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(listThingsRequest, this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, listThingsRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Demo");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "ListThings");

            CompletableFuture<ListThingsResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<ListThingsRequest, ListThingsResponse>()
                            .withOperation(DemoOperations.LIST_THINGS).withProtocolMetadata(protocolMetadata)
                            .withRequestConfiguration(clientConfiguration).withInput(listThingsRequest)
                            .withMetricCollector(apiCallMetricCollector)
                            .withAuthSchemeOptionsResolver(this::resolveAuthSchemeOptions)
                            .withEndpointResolver(this::resolveEndpoint));
            CompletableFuture<ListThingsResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * An operation with request compression and a required checksum.
     *
     * @param putThingRequest
     * @return A Java Future containing the result of the PutThing operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>InternalException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>DemoException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample DemoAsyncClient.PutThing
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/demo-2024-01-01/PutThing" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public CompletableFuture<PutThingResponse> putThing(PutThingRequest putThingRequest) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(putThingRequest, this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, putThingRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Demo");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "PutThing");

            CompletableFuture<PutThingResponse> executeFuture = clientHandler
                    .execute(new ClientExecutionParams<PutThingRequest, PutThingResponse>()
                            .withOperation(DemoOperations.PUT_THING)
                            .withProtocolMetadata(protocolMetadata)
                            .withRequestConfiguration(clientConfiguration)
                            .withInput(putThingRequest)
                            .withMetricCollector(apiCallMetricCollector)
                            .withAuthSchemeOptionsResolver(this::resolveAuthSchemeOptions)
                            .withEndpointResolver(this::resolveEndpoint)
                            .putExecutionAttribute(SdkInternalExecutionAttribute.HTTP_CHECKSUM_REQUIRED,
                                    HttpChecksumRequired.create())
                            .putExecutionAttribute(SdkInternalExecutionAttribute.REQUEST_COMPRESSION,
                                    RequestCompression.builder().encodings("gzip").isStreaming(false).build()));
            CompletableFuture<PutThingResponse> whenCompleted = executeFuture.whenComplete((r, e) -> {
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return executeFuture;
        } catch (Throwable t) {
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * An event-stream operation, which stays on the legacy path.
     *
     * @param watchThingsRequest
     * @return A Java Future containing the result of the WatchThings operation returned by the service.<br/>
     *         The CompletableFuture returned by this method can be completed exceptionally with the following
     *         exceptions. The exception returned is wrapped with CompletionException, so you need to invoke
     *         {@link Throwable#getCause} to retrieve the underlying exception.
     *         <ul>
     *         <li>InternalException</li>
     *         <li>SdkException Base class for all exceptions that can be thrown by the SDK (both service and client).
     *         Can be used for catch all scenarios.</li>
     *         <li>SdkClientException If any client side error occurs such as an IO related failure, failure to get
     *         credentials, etc.</li>
     *         <li>DemoException Base class for all service exceptions. Unknown exceptions will be thrown as an instance
     *         of this type.</li>
     *         </ul>
     * @sample DemoAsyncClient.WatchThings
     * @see <a href="https://docs.aws.amazon.com/goto/WebAPI/demo-2024-01-01/WatchThings" target="_top">AWS API
     *      Documentation</a>
     */
    @Override
    public CompletableFuture<Void> watchThings(WatchThingsRequest watchThingsRequest,
            WatchThingsResponseHandler asyncResponseHandler) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(watchThingsRequest, this.clientConfiguration);
        List<MetricPublisher> metricPublishers = resolveMetricPublishers(clientConfiguration, watchThingsRequest
                .overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = metricPublishers.isEmpty() ? NoOpMetricCollector.create() : MetricCollector
                .create("ApiCall");
        try {
            apiCallMetricCollector.reportMetric(CoreMetric.SERVICE_ID, "Demo");
            apiCallMetricCollector.reportMetric(CoreMetric.OPERATION_NAME, "WatchThings");
            JsonOperationMetadata operationMetadata = JsonOperationMetadata.builder().hasStreamingSuccessResponse(false)
                    .isPayloadJson(true).build();

            HttpResponseHandler<WatchThingsResponse> responseHandler = new AttachHttpMetadataResponseHandler(
                    protocolFactory.createResponseHandler(operationMetadata, WatchThingsResponse::builder));

            HttpResponseHandler<SdkResponse> voidResponseHandler = protocolFactory.createResponseHandler(JsonOperationMetadata
                    .builder().isPayloadJson(false).hasStreamingSuccessResponse(true).build(), VoidSdkResponse::builder);

            HttpResponseHandler<? extends ThingEvents> eventResponseHandler = protocolFactory.createResponseHandler(
                    JsonOperationMetadata.builder().isPayloadJson(true).hasStreamingSuccessResponse(false).build(),
                    EventStreamTaggedUnionPojoSupplier.builder().putSdkPojoSupplier("Changed", ThingEvents::changedBuilder)
                            .defaultSdkPojoSupplier(() -> new SdkPojoBuilder(ThingEvents.UNKNOWN)).build());

            Function<String, Optional<ExceptionMetadata>> eventstreamExceptionMetadataMapper = errorCode -> {
                switch (errorCode) {
                default:
                    return Optional.empty();
                }
            };

            HttpResponseHandler<AwsServiceException> errorEventResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata, eventstreamExceptionMetadataMapper);

            Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper = errorCode -> {
                if (errorCode == null) {
                    return Optional.empty();
                }
                switch (errorCode) {
                case "ThingNotFound":
                    return Optional.of(ExceptionMetadata.builder().errorCode("ThingNotFound").httpStatusCode(404)
                            .exceptionBuilderSupplier(ThingNotFoundException::builder).build());
                case "InternalFault":
                    return Optional.of(ExceptionMetadata.builder().errorCode("InternalFault")
                            .exceptionBuilderSupplier(InternalException::builder).build());
                default:
                    return Optional.empty();
                }
            };
            HttpResponseHandler<AwsServiceException> errorResponseHandler = createErrorResponseHandler(protocolFactory,
                    operationMetadata, exceptionMetadataMapper);
            CompletableFuture<Void> future = new CompletableFuture<>();
            EventStreamAsyncResponseTransformer<WatchThingsResponse, ThingEvents> asyncResponseTransformer = EventStreamAsyncResponseTransformer
                    .<WatchThingsResponse, ThingEvents> builder().eventStreamResponseHandler(asyncResponseHandler)
                    .eventResponseHandler(eventResponseHandler).initialResponseHandler(responseHandler)
                    .exceptionResponseHandler(errorEventResponseHandler).future(future).executor(executor)
                    .serviceName(serviceName()).build();

            CompletableFuture<Void> executeFuture = clientHandler.execute(
                    new ClientExecutionParams<WatchThingsRequest, SdkResponse>().withOperationName("WatchThings")
                            .withProtocolMetadata(protocolMetadata)
                            .withMarshaller(new WatchThingsRequestMarshaller(protocolFactory))
                            .withResponseHandler(voidResponseHandler).withErrorResponseHandler(errorResponseHandler)
                            .withRequestConfiguration(clientConfiguration).withMetricCollector(apiCallMetricCollector)
                            .withAuthSchemeOptionsResolver(this::resolveAuthSchemeOptions)
                            .withEndpointResolver(this::resolveEndpoint).withInput(watchThingsRequest), asyncResponseTransformer);
            CompletableFuture<Void> whenCompleted = executeFuture.whenComplete((r, e) -> {
                if (e != null) {
                    try {
                        asyncResponseHandler.exceptionOccurred(e);
                    } finally {
                        future.completeExceptionally(e);
                    }
                }
                metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            });
            executeFuture = CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
            return CompletableFutureUtils.forwardExceptionTo(future, executeFuture);
        } catch (Throwable t) {
            runAndLogError(log, "Exception thrown in exceptionOccurred callback, ignoring",
                    () -> asyncResponseHandler.exceptionOccurred(t));
            metricPublishers.forEach(p -> p.publish(apiCallMetricCollector.collect()));
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    @Override
    public final DemoServiceClientConfiguration serviceClientConfiguration() {
        return new DemoServiceClientConfigurationBuilder(this.clientConfiguration.toBuilder()).build();
    }

    @Override
    public final String serviceName() {
        return SERVICE_NAME;
    }

    private <T extends BaseAwsJsonProtocolFactory.Builder<T>> T init(T builder) {
        return builder.clientConfiguration(clientConfiguration).defaultServiceExceptionSupplier(DemoException::builder)
                .protocol(AwsJsonProtocol.SMITHY_RPC_V2_CBOR).protocolVersion("1.0");
    }

    private static List<MetricPublisher> resolveMetricPublishers(SdkClientConfiguration clientConfiguration,
            RequestOverrideConfiguration requestOverrideConfiguration) {
        List<MetricPublisher> publishers = null;
        if (requestOverrideConfiguration != null) {
            publishers = requestOverrideConfiguration.metricPublishers();
        }
        if (publishers == null || publishers.isEmpty()) {
            publishers = clientConfiguration.option(SdkClientOption.METRIC_PUBLISHERS);
        }
        if (publishers == null) {
            publishers = Collections.emptyList();
        }
        return publishers;
    }

    private List<AuthSchemeOption> resolveAuthSchemeOptions(SdkRequest request, ExecutionAttributes executionAttributes) {
        String operationName = executionAttributes.getAttribute(SdkExecutionAttribute.OPERATION_NAME);
        DemoAuthSchemeProvider requestAuthSchemeProvider = request
                .overrideConfiguration()
                .flatMap(c -> c.authSchemeProvider())
                .map(p -> Validate
                        .isInstanceOf(DemoAuthSchemeProvider.class, p, "Expected an instance of DemoAuthSchemeProvider"))
                .orElse(null);
        DemoAuthSchemeProvider authSchemeProvider = requestAuthSchemeProvider != null ? requestAuthSchemeProvider : Validate
                .isInstanceOf(DemoAuthSchemeProvider.class,
                        executionAttributes.getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_RESOLVER),
                        "Expected an instance of DemoAuthSchemeProvider");
        DemoAuthSchemeParams.Builder paramsBuilder = DemoAuthSchemeParams.builder().operation(operationName);
        paramsBuilder.region(executionAttributes.getAttribute(AwsExecutionAttribute.AWS_REGION));
        List<AuthSchemeOption> options = authSchemeProvider.resolveAuthScheme(paramsBuilder.build());
        return options;
    }

    private Endpoint resolveEndpoint(SdkRequest request, ExecutionAttributes executionAttributes) {
        String operationName = executionAttributes.getAttribute(SdkExecutionAttribute.OPERATION_NAME);
        DemoEndpointProvider provider = (DemoEndpointProvider) executionAttributes
                .getAttribute(SdkInternalExecutionAttribute.ENDPOINT_PROVIDER);
        try {
            DemoEndpointParams endpointParams = DemoEndpointResolverUtils.ruleParams(request, executionAttributes);
            Endpoint endpoint = provider.resolveEndpoint(endpointParams).join();
            if (!AwsEndpointProviderUtils.disableHostPrefixInjection(executionAttributes)) {
                Optional<String> hostPrefix = DemoEndpointResolverUtils.hostPrefix(operationName, request);
                if (hostPrefix.isPresent()) {
                    endpoint = AwsEndpointProviderUtils.addHostPrefix(endpoint, hostPrefix.get());
                }
            }
            List<EndpointAuthScheme> endpointAuthSchemes = endpoint.attribute(AwsEndpointAttribute.AUTH_SCHEMES);
            SelectedAuthScheme<?> selectedAuthScheme = executionAttributes
                    .getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
            if (endpointAuthSchemes != null && selectedAuthScheme != null) {
                selectedAuthScheme = DemoEndpointResolverUtils.authSchemeWithEndpointSignerProperties(endpointAuthSchemes,
                        selectedAuthScheme);
                executionAttributes.putAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME, selectedAuthScheme);
            }
            DemoEndpointResolverUtils.setMetricValues(endpoint, executionAttributes);
            return endpoint;
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SdkClientException) {
                throw (SdkClientException) cause;
            }
            throw SdkClientException.create("Endpoint resolution failed: " + cause.getMessage(), cause);
        }
    }

    private void updateRetryStrategyClientConfiguration(SdkClientConfiguration.Builder configuration) {
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

    private SdkClientConfiguration updateSdkClientConfiguration(SdkRequest request, SdkClientConfiguration clientConfiguration) {
        List<SdkPlugin> plugins = request.overrideConfiguration().map(c -> c.plugins()).orElse(Collections.emptyList());
        if (plugins.isEmpty()) {
            return clientConfiguration;
        }
        SdkClientConfiguration.Builder configuration = clientConfiguration.toBuilder();
        DemoServiceClientConfigurationBuilder serviceConfigBuilder = new DemoServiceClientConfigurationBuilder(configuration);
        for (SdkPlugin plugin : plugins) {
            plugin.configureClient(serviceConfigBuilder);
        }
        updateRetryStrategyClientConfiguration(configuration);
        return configuration.build();
    }

    private HttpResponseHandler<AwsServiceException> createErrorResponseHandler(BaseAwsJsonProtocolFactory protocolFactory,
            JsonOperationMetadata operationMetadata, Function<String, Optional<ExceptionMetadata>> exceptionMetadataMapper) {
        return protocolFactory.createErrorResponseHandler(operationMetadata, exceptionMetadataMapper);
    }

    @Override
    public void close() {
        clientHandler.close();
    }
}
