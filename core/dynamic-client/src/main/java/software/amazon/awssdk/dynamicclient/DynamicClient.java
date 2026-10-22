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
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import software.amazon.awssdk.awscore.internal.AwsProtocolMetadata;
import software.amazon.awssdk.awscore.internal.AwsServiceProtocol;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.AsyncResponseTransformerUtils;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculationResolver;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidationResolver;
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
import software.amazon.awssdk.core.pagination.async.AsyncPageFetcher;
import software.amazon.awssdk.core.pagination.async.ResponsesSubscription;
import software.amazon.awssdk.core.pagination.sync.PaginatedResponsesIterator;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.core.pagination.sync.SyncPageFetcher;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.core.util.VersionInfo;
import software.amazon.awssdk.dynamicclient.internal.DynamicAuthSchemes;
import software.amazon.awssdk.dynamicclient.internal.DynamicCallResolvers;
import software.amazon.awssdk.dynamicclient.internal.DynamicPagination;
import software.amazon.awssdk.dynamicclient.internal.DynamicProtocolSelection;
import software.amazon.awssdk.dynamicclient.internal.DynamicServiceClientConfigurationBuilder;
import software.amazon.awssdk.dynamicclient.internal.DynamicServiceMetadata;
import software.amazon.awssdk.dynamicclient.internal.DynamicStreaming;
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
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.HttpChecksumTrait;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Pair;
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
 * <p>The features that generated clients configure per operation come from the operation's schemas and traits: the
 * host prefix of {@code smithy.api#endpoint}, {@code smithy.api#idempotencyToken} members filled with a generated
 * token when the input omits them, {@code smithy.api#requestCompression}, {@code aws.protocols#httpChecksum} and
 * {@code smithy.api#httpChecksumRequired} checksums, and the auth schemes. {@link #paginate} and
 * {@link #paginateAsync} page through an operation with its {@code smithy.api#paginated} trait.
 *
 * <p>Streaming blobs work as on a generated client. A streaming input member takes a data stream document over a
 * {@code RequestBody} ({@code call}) or an {@code AsyncRequestBody} ({@code callAsync}), created with
 * {@link DynamicStreams}, and the body is sent as the HTTP request body. An operation with a streaming output member
 * is called with a {@code ResponseTransformer} or {@code AsyncResponseTransformer} of output documents, which receives
 * the response body. Event streams are not supported yet: such calls fail before anything is sent.
 *
 * <p>An error response that resolves to an error shape of the called operation or of the service throws a
 * {@link DocumentException}, whose {@link DocumentException#getContents() contents} are the error's members; when both
 * list a matching error, the operation's is used. Any other error response throws a plain
 * {@link AwsServiceException}. Both carry the status code, request ID, error code and message, and an asynchronous
 * call completes its future exceptionally with them as the cause.
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
    private static final Pattern VERSION = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(.*)");
    private static final ApiName PAGINATOR_API_NAME =
        ApiName.builder().name("sdk-metrics").version(BusinessMetricFeatureId.PAGINATOR.value()).build();

    /**
     * The protocol metadata of each SDK protocol, by protocol trait ShapeId, as codegen maps a service's protocol to
     * {@code AwsServiceProtocol}.
     */
    private static final Map<String, AwsProtocolMetadata> PROTOCOL_METADATA;

    static {
        Map<String, AwsProtocolMetadata> metadata = new HashMap<>();
        metadata.put("smithy.protocols#rpcv2Cbor", protocolMetadata(AwsServiceProtocol.SMITHY_RPC_V2_CBOR));
        metadata.put("aws.protocols#awsJson1_0", protocolMetadata(AwsServiceProtocol.AWS_JSON));
        metadata.put("aws.protocols#awsJson1_1", protocolMetadata(AwsServiceProtocol.AWS_JSON));
        metadata.put("aws.protocols#restJson1", protocolMetadata(AwsServiceProtocol.REST_JSON));
        metadata.put("aws.protocols#restXml", protocolMetadata(AwsServiceProtocol.REST_XML));
        metadata.put("aws.protocols#awsQuery", protocolMetadata(AwsServiceProtocol.QUERY));
        metadata.put("aws.protocols#ec2Query", protocolMetadata(AwsServiceProtocol.EC2));
        PROTOCOL_METADATA = Collections.unmodifiableMap(metadata);
    }

    private final Model model;
    private final ServiceShape serviceShape;
    private final SchemaConverter schemaConverter;
    private final ApiService service;
    private final String serviceName;
    private final String sdkId;
    private final DynamicAuthSchemes authSchemes;
    private final SdkClientConfiguration syncClientConfiguration;
    private final SdkClientConfiguration asyncClientConfiguration;

    /**
     * The configurations calls run with: {@link #syncClientConfiguration} and {@link #asyncClientConfiguration} with
     * {@code SDK_CLIENT} (this client) and {@code API_METADATA} (the user agent's {@code api/} segment), as the
     * constructor of a generated client adds them. The handlers own and close the configurations without these two
     * options, so closing does not reach this client again.
     */
    private final SdkClientConfiguration syncCallConfiguration;
    private final SdkClientConfiguration asyncCallConfiguration;
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
        String apiMetadata = apiMetadata(sdkId);
        this.syncCallConfiguration = callConfiguration(syncClientConfiguration, apiMetadata);
        this.asyncCallConfiguration = callConfiguration(asyncClientConfiguration, apiMetadata);
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
        return new DynamicServiceClientConfigurationBuilder(syncCallConfiguration.toBuilder()).build();
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
        return call(operationName, Document.EMPTY_MAP, (AwsRequestOverrideConfiguration) null);
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
        return call(operationName, input, (AwsRequestOverrideConfiguration) null);
    }

    /**
     * Calls an operation, with configuration that applies to this call only.
     *
     * <p>The input is coerced to the operation's input schema before any interceptor runs (see
     * {@link #createStruct}); interceptors see the coerced document in the {@link DynamicRequest} of
     * {@code context.request()}. The output is a structure document keyed by Smithy member name, whose
     * discriminator is the operation's output ShapeId; modeled members that the response does not contain are absent.
     *
     * <p>A streaming blob input member (a member that targets a blob with {@code smithy.api#streaming}) takes
     * {@link DynamicStreams#dataStream(RequestBody)}; the body is sent as the HTTP request body without being
     * buffered. An operation with a streaming blob output member is called with
     * {@link #call(String, Document, AwsRequestOverrideConfiguration, ResponseTransformer)} instead.
     *
     * @param operationName the shape name of the operation, case-sensitive
     * @param input the input: a map or structure document keyed by Smithy member name
     * @param overrideConfiguration the configuration of this call (the same options as a generated client's request
     *     override configuration), or null
     * @return the output
     * @throws IllegalArgumentException if the service has no operation of this name, or a streaming member is misused:
     *     a required streaming input member is absent or does not hold a data stream document over a
     *     {@link RequestBody}, the operation has a streaming blob output, or it has an event stream (event stream
     *     operations can only be called with {@code callAsync}); no request is sent
     * @throws SerializationException if the input cannot be coerced to the operation's input schema; no request is sent
     * @throws software.amazon.awssdk.core.exception.SdkException if the call fails, as for a generated client
     */
    public Document call(String operationName, Document input, AwsRequestOverrideConfiguration overrideConfiguration) {
        DynamicOperation operation = getOperation(operationName);
        DynamicRequest request = request(operation, input, overrideConfiguration, false, false);
        return execute(operation, request, syncClientHandler::execute).document();
    }

    /**
     * Calls an operation that has a streaming blob output member, and hands the response body to a transformer: the
     * same as {@link #call(String, Document, AwsRequestOverrideConfiguration, ResponseTransformer)} without override
     * configuration.
     *
     * @param operationName the shape name of the operation, case-sensitive
     * @param input the input: a map or structure document keyed by Smithy member name
     * @param responseTransformer the transformer of the output document and the response body
     * @param <ReturnT> the transformer's result type
     * @return the transformer's result
     */
    public <ReturnT> ReturnT call(String operationName, Document input,
                                  ResponseTransformer<Document, ReturnT> responseTransformer) {
        return call(operationName, input, null, responseTransformer);
    }

    /**
     * Calls an operation that has a streaming blob output member, and hands the response body to a transformer, as a
     * generated client's streaming operation method does (Requirement 31.4).
     *
     * <p>The transformer receives the output document, which holds the members that are not the stream (headers and
     * status code), and the response body, unread. For example, {@code ResponseTransformer.toBytes()} returns the body
     * as {@code ResponseBytes<Document>} whose {@code response()} is the output document. An error response is thrown
     * as by {@link #call(String, Document, AwsRequestOverrideConfiguration)}, and its body never reaches the
     * transformer. The input is the same as for that method.
     *
     * @param operationName the shape name of the operation, case-sensitive
     * @param input the input: a map or structure document keyed by Smithy member name
     * @param overrideConfiguration the configuration of this call, or null
     * @param responseTransformer the transformer of the output document and the response body
     * @param <ReturnT> the transformer's result type
     * @return the transformer's result
     * @throws IllegalArgumentException if the service has no operation of this name, the operation has no streaming blob
     *     output member, or a streaming input member is misused as for
     *     {@link #call(String, Document, AwsRequestOverrideConfiguration)}; no request is sent
     * @throws SerializationException if the input cannot be coerced to the operation's input schema; no request is sent
     * @throws software.amazon.awssdk.core.exception.SdkException if the call fails, as for a generated client
     */
    public <ReturnT> ReturnT call(String operationName, Document input,
                                  AwsRequestOverrideConfiguration overrideConfiguration,
                                  ResponseTransformer<Document, ReturnT> responseTransformer) {
        Validate.paramNotNull(responseTransformer, "responseTransformer");
        DynamicOperation operation = getOperation(operationName);
        DynamicRequest request = request(operation, input, overrideConfiguration, false, true);
        ResponseTransformer<DynamicResponse, ReturnT> transformer =
            DynamicStreaming.responseTransformer(responseTransformer);
        return execute(operation, request,
                       params -> syncClientHandler.execute(params.withResponseTransformer(transformer), transformer));
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
        return callAsync(operationName, Document.EMPTY_MAP, (AwsRequestOverrideConfiguration) null);
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
        return callAsync(operationName, input, (AwsRequestOverrideConfiguration) null);
    }

    /**
     * Calls an operation asynchronously, with configuration that applies to this call only. The call is the same as
     * {@link #call(String, Document, AwsRequestOverrideConfiguration)}, through the client's asynchronous
     * configuration.
     *
     * <p>A streaming blob input member takes {@link DynamicStreams#dataStream(AsyncRequestBody)}; the body is sent as
     * the HTTP request body. An operation with a streaming blob output member is called with
     * {@link #callAsync(String, Document, AwsRequestOverrideConfiguration, AsyncResponseTransformer)} instead.
     *
     * <p>Event streams are not supported yet. A call of an operation with an event stream input or output member fails
     * with an {@link UnsupportedOperationException} before anything is sent, after its event stream input member, if
     * any, is checked: a required member must be set, to a document from {@link DynamicStreams#eventStream}.
     *
     * <p>This method does not throw: every failure, including an unknown operation, an input that cannot be coerced
     * and a misused streaming member (the {@code IllegalArgumentException}s of
     * {@link #call(String, Document, AwsRequestOverrideConfiguration)}, with an {@link AsyncRequestBody} in place of
     * the {@link RequestBody}), completes the returned future exceptionally (Requirement 28.12).
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
            DynamicRequest request = request(operation, input, overrideConfiguration, true, false);
            CompletableFuture<DynamicResponse> responseFuture = executeAsync(operation, request);
            CompletableFuture<Document> result = responseFuture.thenApply(DynamicResponse::document);
            return CompletableFutureUtils.forwardExceptionTo(result, responseFuture);
        } catch (Throwable t) {
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * Calls an operation that has a streaming blob output member asynchronously, and streams the response body to a
     * transformer: the same as
     * {@link #callAsync(String, Document, AwsRequestOverrideConfiguration, AsyncResponseTransformer)} without override
     * configuration.
     *
     * @param operationName the shape name of the operation, case-sensitive
     * @param input the input: a map or structure document keyed by Smithy member name
     * @param asyncResponseTransformer the transformer of the output document and the response body
     * @param <ReturnT> the transformer's result type
     * @return a future of the transformer's result
     */
    public <ReturnT> CompletableFuture<ReturnT> callAsync(
        String operationName, Document input, AsyncResponseTransformer<Document, ReturnT> asyncResponseTransformer) {
        return callAsync(operationName, input, null, asyncResponseTransformer);
    }

    /**
     * Calls an operation that has a streaming blob output member asynchronously, and streams the response body to a
     * transformer, as a generated asynchronous client's streaming operation method does (Requirement 31.4).
     *
     * <p>The transformer is prepared, receives the output document (the members that are not the stream) in
     * {@code onResponse}, and then the response body as it arrives in {@code onStream}. An error response fails the
     * returned future as {@link #callAsync(String, Document, AwsRequestOverrideConfiguration)} does, and its body
     * never reaches the transformer. When the call fails, the transformer's {@code exceptionOccurred} is called. The
     * input is the same as for that method.
     *
     * <p>This method does not throw: every failure, including an operation without a streaming blob output member,
     * completes the returned future exceptionally.
     *
     * @param operationName the shape name of the operation, case-sensitive
     * @param input the input: a map or structure document keyed by Smithy member name
     * @param overrideConfiguration the configuration of this call, or null
     * @param asyncResponseTransformer the transformer of the output document and the response body
     * @param <ReturnT> the transformer's result type
     * @return a future of the transformer's result
     */
    public <ReturnT> CompletableFuture<ReturnT> callAsync(
        String operationName, Document input, AwsRequestOverrideConfiguration overrideConfiguration,
        AsyncResponseTransformer<Document, ReturnT> asyncResponseTransformer) {
        if (asyncResponseTransformer == null) {
            return CompletableFutureUtils.failedFuture(new NullPointerException("asyncResponseTransformer must not "
                                                                                + "be null."));
        }
        AsyncResponseTransformer<DynamicResponse, ReturnT> transformer =
            DynamicStreaming.asyncResponseTransformer(asyncResponseTransformer);
        DynamicRequest request;
        DynamicOperation operation;
        try {
            operation = getOperation(operationName);
            request = request(operation, input, overrideConfiguration, true, true);
        } catch (Throwable t) {
            notifyExceptionOccurred(transformer, t);
            return CompletableFutureUtils.failedFuture(t);
        }
        return executeAsyncStreaming(operation, request, transformer);
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
     * Pages through a paginated operation: the same as {@link #paginate(String, Document,
     * AwsRequestOverrideConfiguration)} without override configuration.
     *
     * @param operationName the shape name of a paginated operation, case-sensitive
     * @param input the input of the first request
     * @return the pages
     */
    public SdkIterable<Document> paginate(String operationName, Document input) {
        return paginate(operationName, input, null);
    }

    /**
     * Pages through a paginated operation, as a generated client's paginator method does. The operation needs a
     * {@code smithy.api#paginated} trait with an {@code inputToken} and an {@code outputToken}, set on the operation or
     * inherited from the service's trait.
     *
     * <p>Each element of the returned iterable is the output document of one call. The first call has {@code input};
     * each later call has {@code input} with the previous page's output token (the {@code outputToken} member path of
     * the output) in the {@code inputToken} member. Paging stops after a page whose output token is absent or empty.
     * Calls are made lazily, as the iteration reaches them, and each new iterator starts again from the first page. The
     * items of a page are its member named by {@link DynamicOperation#paginated()}'s {@code items} path.
     *
     * <p>The calls go through {@link #call(String, Document, AwsRequestOverrideConfiguration)}, with the paginator's
     * user agent metadata added, so they throw what {@code call} throws when they are made.
     *
     * @param operationName the shape name of a paginated operation, case-sensitive
     * @param input the input of the first request
     * @param overrideConfiguration the configuration of every call, or null
     * @return the pages
     * @throws IllegalArgumentException if the service has no operation of this name, or the operation is not paginated;
     *     no request is sent
     * @throws SerializationException if the input cannot be coerced to the operation's input schema; no request is sent
     */
    public SdkIterable<Document> paginate(String operationName, Document input,
                                          AwsRequestOverrideConfiguration overrideConfiguration) {
        DynamicOperation operation = getOperation(operationName);
        DynamicPagination pagination = DynamicPagination.of(operation, operation.paginated().orElse(null));
        DynamicRequest first = request(operation, input, paginatorOverrideConfiguration(overrideConfiguration),
                                        false, false);
        SyncPageFetcher<Document> pages = new SyncPageFetcher<Document>() {
            @Override
            public boolean hasNextPage(Document previousPage) {
                return pagination.hasNextPage(previousPage);
            }

            @Override
            public Document nextPage(Document previousPage) {
                return execute(operation, nextRequest(pagination, first, previousPage), syncClientHandler::execute)
                    .document();
            }
        };
        return () -> PaginatedResponsesIterator.builder().nextPageFetcher(pages).build();
    }

    /**
     * Pages through a paginated operation asynchronously: the same as {@link #paginateAsync(String, Document,
     * AwsRequestOverrideConfiguration)} without override configuration.
     *
     * @param operationName the shape name of a paginated operation, case-sensitive
     * @param input the input of the first request
     * @return a publisher of the pages
     */
    public SdkPublisher<Document> paginateAsync(String operationName, Document input) {
        return paginateAsync(operationName, input, null);
    }

    /**
     * Pages through a paginated operation asynchronously, as a generated asynchronous client's paginator method does.
     * The pages and the end of paging are those of {@link #paginate(String, Document, AwsRequestOverrideConfiguration)};
     * each subscription starts from the first page, and makes a call when the subscriber requests a page. A failed call
     * ends the subscription with {@code onError}.
     *
     * @param operationName the shape name of a paginated operation, case-sensitive
     * @param input the input of the first request
     * @param overrideConfiguration the configuration of every call, or null
     * @return a publisher of the pages
     * @throws IllegalArgumentException if the service has no operation of this name, or the operation is not paginated;
     *     no request is sent
     * @throws SerializationException if the input cannot be coerced to the operation's input schema; no request is sent
     */
    public SdkPublisher<Document> paginateAsync(String operationName, Document input,
                                                AwsRequestOverrideConfiguration overrideConfiguration) {
        DynamicOperation operation = getOperation(operationName);
        DynamicPagination pagination = DynamicPagination.of(operation, operation.paginated().orElse(null));
        DynamicRequest first = request(operation, input, paginatorOverrideConfiguration(overrideConfiguration),
                                        true, false);
        AsyncPageFetcher<Document> pages = new AsyncPageFetcher<Document>() {
            @Override
            public boolean hasNextPage(Document previousPage) {
                return pagination.hasNextPage(previousPage);
            }

            @Override
            public CompletableFuture<Document> nextPage(Document previousPage) {
                CompletableFuture<DynamicResponse> responseFuture;
                try {
                    responseFuture = executeAsync(operation, nextRequest(pagination, first, previousPage));
                } catch (Throwable t) {
                    return CompletableFutureUtils.failedFuture(t);
                }
                CompletableFuture<Document> page = responseFuture.thenApply(DynamicResponse::document);
                return CompletableFutureUtils.forwardExceptionTo(page, responseFuture);
            }
        };
        return subscriber -> subscriber.onSubscribe(ResponsesSubscription.builder()
                                                                         .subscriber(subscriber)
                                                                         .nextPageFetcher(pages)
                                                                         .build());
    }

    /**
     * The request of the first page, or of the page after {@code previousPage}.
     */
    private static DynamicRequest nextRequest(DynamicPagination pagination, DynamicRequest first, Document previousPage) {
        if (previousPage == null) {
            return first;
        }
        return first.toBuilder().document(pagination.nextInput(first.document(), previousPage)).build();
    }

    /**
     * The override configuration with the paginator's {@code sdk-metrics} API name, as a generated paginator adds it
     * ({@code UserAgentUtils.applyPaginatorUserAgent}).
     */
    private static AwsRequestOverrideConfiguration paginatorOverrideConfiguration(
        AwsRequestOverrideConfiguration overrideConfiguration) {
        AwsRequestOverrideConfiguration.Builder builder = overrideConfiguration == null
                                                          ? AwsRequestOverrideConfiguration.builder()
                                                          : overrideConfiguration.toBuilder();
        return builder.addApiName(PAGINATOR_API_NAME).build();
    }

    /**
     * Checks the call's streaming members (Requirement 31.6), then coerces the input to the operation's input schema,
     * before any interceptor runs (Requirement 28.3).
     */
    private DynamicRequest request(DynamicOperation operation, Document input,
                                   AwsRequestOverrideConfiguration overrideConfiguration, boolean async,
                                   boolean responseTransformer) {
        Validate.paramNotNull(input, "input");
        DynamicStreaming.validate(operation, input, async, responseTransformer);
        StructDocument document = StructDocument.coerce(operation.inputSchema(), input, service.schema().id());
        return DynamicRequest.builder().document(document).overrideConfiguration(overrideConfiguration).build();
    }

    /**
     * Runs a call through the client handler, as a generated synchronous client does: {@code handler} executes the
     * parameters, which carry the request body of a streaming blob input.
     */
    private <ResultT> ResultT execute(DynamicOperation operation, DynamicRequest request,
                                      Function<ClientExecutionParams<DynamicRequest, DynamicResponse>, ResultT> handler) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(request, syncCallConfiguration);
        List<MetricPublisher> metricPublishers =
            resolveMetricPublishers(clientConfiguration, request.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = apiCallMetricCollector(metricPublishers, operation);
        try {
            ClientExecutionParams<DynamicRequest, DynamicResponse> params =
                executionParams(operation, request, clientConfiguration, apiCallMetricCollector);
            DynamicStreaming.requestBody(operation, request.document()).ifPresent(params::withRequestBody);
            return handler.apply(params);
        } finally {
            publish(metricPublishers, apiCallMetricCollector);
        }
    }

    /**
     * Runs a call through the asynchronous client handler, as a generated asynchronous client does.
     */
    private CompletableFuture<DynamicResponse> executeAsync(DynamicOperation operation, DynamicRequest request) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(request, asyncCallConfiguration);
        List<MetricPublisher> metricPublishers =
            resolveMetricPublishers(clientConfiguration, request.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = apiCallMetricCollector(metricPublishers, operation);
        try {
            CompletableFuture<DynamicResponse> executeFuture =
                asyncClientHandler.execute(asyncExecutionParams(operation, request, clientConfiguration,
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
     * Runs a call with a streaming blob output through the asynchronous client handler, as a generated asynchronous
     * client's streaming operation method does: the transformer is told of a failure with {@code exceptionOccurred},
     * and the metrics are published when the response stream ends.
     */
    private <ReturnT> CompletableFuture<ReturnT> executeAsyncStreaming(
        DynamicOperation operation, DynamicRequest request, AsyncResponseTransformer<DynamicResponse, ReturnT> transformer) {
        SdkClientConfiguration clientConfiguration = updateSdkClientConfiguration(request, asyncCallConfiguration);
        List<MetricPublisher> metricPublishers =
            resolveMetricPublishers(clientConfiguration, request.overrideConfiguration().orElse(null));
        MetricCollector apiCallMetricCollector = apiCallMetricCollector(metricPublishers, operation);
        Pair<AsyncResponseTransformer<DynamicResponse, ReturnT>, CompletableFuture<Void>> pair =
            AsyncResponseTransformerUtils.wrapWithEndOfStreamFuture(transformer);
        AsyncResponseTransformer<DynamicResponse, ReturnT> wrapped = pair.left();
        CompletableFuture<Void> endOfStreamFuture = pair.right();
        try {
            ClientExecutionParams<DynamicRequest, DynamicResponse> params =
                asyncExecutionParams(operation, request, clientConfiguration, apiCallMetricCollector)
                    .withAsyncResponseTransformer(wrapped);
            CompletableFuture<ReturnT> executeFuture = asyncClientHandler.execute(params, wrapped);
            CompletableFuture<ReturnT> whenCompleted = executeFuture.whenComplete((r, e) -> {
                if (e != null) {
                    notifyExceptionOccurred(wrapped, e);
                }
                endOfStreamFuture.whenComplete((r2, e2) -> publish(metricPublishers, apiCallMetricCollector));
            });
            return CompletableFutureUtils.forwardExceptionTo(whenCompleted, executeFuture);
        } catch (Throwable t) {
            notifyExceptionOccurred(wrapped, t);
            publish(metricPublishers, apiCallMetricCollector);
            return CompletableFutureUtils.failedFuture(t);
        }
    }

    /**
     * The parameters of an asynchronous call, with the request body of a streaming blob input.
     */
    private static ClientExecutionParams<DynamicRequest, DynamicResponse> asyncExecutionParams(
        DynamicOperation operation, DynamicRequest request, SdkClientConfiguration clientConfiguration,
        MetricCollector apiCallMetricCollector) {
        ClientExecutionParams<DynamicRequest, DynamicResponse> params =
            executionParams(operation, request, clientConfiguration, apiCallMetricCollector);
        DynamicStreaming.asyncRequestBody(operation, request.document()).ifPresent(params::withAsyncRequestBody);
        return params;
    }

    /**
     * Tells a transformer that its call failed, as the generated {@code runAndLogError} call does: a failure of the
     * transformer itself is logged and ignored, so that the call's own failure is reported.
     */
    private static void notifyExceptionOccurred(AsyncResponseTransformer<?, ?> transformer, Throwable error) {
        try {
            transformer.exceptionOccurred(error);
        } catch (Exception e) {
            log.error(() -> "Exception thrown in exceptionOccurred callback, ignoring", e);
        }
    }

    /**
     * The parameters of a call on the schema-based pipeline, with the settings a generated client's operation method
     * passes (codegen's {@code SchemaOperationsProtocolSpec}). {@code withOperation} routes the call to the pipeline
     * that generated clients use, with the operation as {@code SdkExecutionAttribute.API_OPERATION}. The protocol
     * metadata is that of the configuration's protocol, and the host prefix, credential type, checksum and compression
     * attributes come from the operation's schemas.
     */
    private static ClientExecutionParams<DynamicRequest, DynamicResponse> executionParams(
        DynamicOperation operation, DynamicRequest request, SdkClientConfiguration clientConfiguration,
        MetricCollector apiCallMetricCollector) {
        ClientExecutionParams<DynamicRequest, DynamicResponse> params =
            new ClientExecutionParams<DynamicRequest, DynamicResponse>()
                .withOperation(operation)
                .withProtocolMetadata(protocolMetadata(clientConfiguration))
                .withRequestConfiguration(clientConfiguration)
                .withInput(request)
                .withMetricCollector(apiCallMetricCollector)
                .withAuthSchemeOptionsResolver(DynamicCallResolvers.authSchemeOptionsResolver())
                .withEndpointResolver(DynamicCallResolvers.endpointResolver());
        operation.metadata().apply(params, request);
        return params;
    }

    /**
     * The protocol metadata a generated client of the configuration's protocol passes ({@code AwsServiceProtocol} of
     * the protocol), or null for a protocol the SDK does not define, such as a third-party protocol.
     */
    private static AwsProtocolMetadata protocolMetadata(SdkClientConfiguration clientConfiguration) {
        ClientProtocol<?, ?> protocol = clientConfiguration.option(SdkClientOption.CLIENT_PROTOCOL);
        return protocol == null ? null : PROTOCOL_METADATA.get(protocol.id().toString());
    }

    private static AwsProtocolMetadata protocolMetadata(AwsServiceProtocol protocol) {
        return AwsProtocolMetadata.builder().serviceProtocol(protocol).build();
    }

    /**
     * The {@code api/} user agent metadata of a generated client: the SDK ID with spaces replaced by underscores, and
     * the SDK version as {@code <major>.<minor>.x} (codegen's {@code ServiceVersionInfo.VERSION}).
     */
    private static String apiMetadata(String sdkId) {
        Matcher version = VERSION.matcher(VersionInfo.SDK_VERSION);
        String majorMinorX = version.matches() ? version.group(1) + "." + version.group(2) + ".x" + version.group(4)
                                               : VersionInfo.SDK_VERSION;
        return sdkId.replace(" ", "_") + "#" + majorMinorX;
    }

    /**
     * A copy of a resolved configuration with this client as {@code SDK_CLIENT} and the {@code api/} metadata, which
     * re-derives the client user agent.
     */
    private SdkClientConfiguration callConfiguration(SdkClientConfiguration configuration, String apiMetadata) {
        return configuration.toBuilder()
                            .option(SdkClientOption.SDK_CLIENT, this)
                            .option(SdkClientOption.API_METADATA, apiMetadata)
                            .build();
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
            BuildState buildState = new BuildState(resolvedModel, resolvedService, converter, serviceSchema);
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
            // As the generated builder of a service with flexible checksums: the profile file and system setting, else
            // WHEN_SUPPORTED, only for services that have them, so other services' calls record no checksum metrics.
            if (resolvedState.requestChecksums) {
                builder.lazyOptionIfAbsent(
                    SdkClientOption.REQUEST_CHECKSUM_CALCULATION,
                    c -> RequestChecksumCalculationResolver.create()
                                                           .profileFile(c.get(SdkClientOption.PROFILE_FILE_SUPPLIER))
                                                           .profileName(c.get(SdkClientOption.PROFILE_NAME))
                                                           .defaultChecksumCalculation(
                                                               RequestChecksumCalculation.WHEN_SUPPORTED)
                                                           .resolve());
            }
            if (resolvedState.responseChecksums) {
                builder.lazyOptionIfAbsent(
                    SdkClientOption.RESPONSE_CHECKSUM_VALIDATION,
                    c -> ResponseChecksumValidationResolver.create()
                                                           .profileFile(c.get(SdkClientOption.PROFILE_FILE_SUPPLIER))
                                                           .profileName(c.get(SdkClientOption.PROFILE_NAME))
                                                           .defaultChecksumValidation(
                                                               ResponseChecksumValidation.WHEN_SUPPORTED)
                                                           .resolve());
            }
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

        /**
         * Whether an operation's {@code httpChecksum} trait has a request algorithm member or requires a request
         * checksum, and whether one lists response algorithms: codegen's {@code hasRequestAlgorithmMember} and
         * {@code hasResponseAlgorithms}, read from the converted operation schemas.
         */
        private final boolean requestChecksums;
        private final boolean responseChecksums;

        private BuildState(Model model, DynamicServiceMetadata service, SchemaConverter converter, Schema serviceSchema) {
            ServiceShape serviceShape = service.service();
            boolean request = false;
            boolean response = false;
            for (OperationShape operation : TopDownIndex.of(model).getContainedOperations(serviceShape)) {
                HttpChecksumTrait checksum = converter.getSchema(operation).getTrait(TraitKey.HTTP_CHECKSUM_TRAIT);
                if (checksum != null) {
                    request |= checksum.getRequestAlgorithmMember().isPresent() || checksum.isRequestChecksumRequired();
                    response |= !checksum.getResponseAlgorithms().isEmpty();
                }
            }
            this.requestChecksums = request;
            this.responseChecksums = response;
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
