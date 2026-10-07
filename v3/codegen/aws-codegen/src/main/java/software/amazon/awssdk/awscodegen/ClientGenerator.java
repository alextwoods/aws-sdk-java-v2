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

package software.amazon.awssdk.awscodegen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.WriterDelegator;

/**
 * Emits a service's generated clients from a {@link ClientPlan} (design "Generated sync client", "Generated async
 * client", "Client construction and resolved configuration"; Requirements 7.1-7.7, 7.11, 7.12, 8.1-8.3, 8.13, 17.1).
 *
 * <ul>
 *   <li>{@code <Service>Client} / {@code <Service>AsyncClient}: V2-shaped interfaces with {@code SERVICE_NAME},
 *       static {@code create()} and {@code builder()}, one typed method per operation (blocking or returning
 *       {@code CompletableFuture}), the request-builder consumer and no-argument overloads the classifier preserves,
 *       streaming request-body and response-transformer overloads, an explicit
 *       {@code AwsRequestOverrideConfiguration} overload, {@code serviceName()},
 *       {@code serviceClientConfiguration()} and {@code close()}. As in V2, an operation with an event stream has
 *       only async methods: {@code CompletableFuture<Void> op(request[, Publisher requestStream],
 *       <Operation>ResponseHandler)} for an output event stream, {@code CompletableFuture<Output> op(request,
 *       Publisher requestStream)} for an input-only one, and no request-only method.</li>
 *   <li>{@code <Operation>ResponseHandler} in the model package (as in V2): the {@code EventStreamResponseHandler}
 *       of the operation's output and output event union.</li>
 *   <li>{@code <Service>ClientBuilder} / {@code <Service>AsyncClientBuilder}: V2-named setters forwarded to the
 *       runtime's {@code AwsClientBuildState}, which resolves the configuration once and builds the client's
 *       {@code ClientRuntime} and {@code AwsCallEngine}.</li>
 *   <li>Package-private {@code Default<Service>Client} / {@code Default<Service>AsyncClient}: one {@code final}
 *       {@code PipelinePlan} field per operation, created once in the constructor from the operation's static binding,
 *       and operation methods that are one direct engine call each; no operation lookup by name and no reflection.
 *       </li>
 *   <li>Package-private {@code <Service>OperationBindings}: one {@code static final AwsOperation} per operation,
 *       from the operation table's descriptor, the generated input and output builders, the endpoint binding, the
 *       modeled auth and, for operations with {@code aws.protocols#httpChecksum}, a static
 *       {@code HttpChecksumMetadata} of the trait value.</li>
 * </ul>
 */
final class ClientGenerator {
    private static final Symbol CONSUMER = jdk("java.util.function", "Consumer");
    private static final Symbol COMPLETABLE_FUTURE = jdk("java.util.concurrent", "CompletableFuture");
    private static final Symbol EXECUTOR = jdk("java.util.concurrent", "Executor");
    private static final Symbol URI = jdk("java.net", "URI");
    /** Through its symbol: a modeled UnsupportedOperationException error (SSM, KMS, ...) would shadow it. */
    private static final Symbol UNSUPPORTED_OPERATION = jdk("java.lang", "UnsupportedOperationException");
    private static final Symbol MAP = jdk("java.util", "Map");
    private static final Symbol LIST = RuntimeSymbols.LIST;
    private static final Symbol OVERRIDE = jdk("software.amazon.awssdk.awscore", "AwsRequestOverrideConfiguration");
    private static final Symbol AWS_SERVICE_EXCEPTION = jdk("software.amazon.awssdk.awscore.exception",
                                                            "AwsServiceException");
    private static final Symbol SDK_CLIENT_EXCEPTION = jdk("software.amazon.awssdk.core.exception",
                                                           "SdkClientException");
    private static final Symbol CONFIGURATION = jdk("software.amazon.awssdk.core.client.config",
                                                    "SdkClientConfiguration");
    private static final Symbol REQUEST_BODY = jdk("software.amazon.awssdk.core.sync", "RequestBody");
    private static final Symbol RESPONSE_TRANSFORMER = jdk("software.amazon.awssdk.core.sync", "ResponseTransformer");
    private static final Symbol ASYNC_REQUEST_BODY = jdk("software.amazon.awssdk.core.async", "AsyncRequestBody");
    private static final Symbol ASYNC_RESPONSE_TRANSFORMER = jdk("software.amazon.awssdk.core.async",
                                                                 "AsyncResponseTransformer");
    private static final Symbol REGION = jdk("software.amazon.awssdk.regions", "Region");
    private static final Symbol IDENTITY_PROVIDER = jdk("software.amazon.awssdk.identity.spi", "IdentityProvider");
    private static final Symbol CREDENTIALS = jdk("software.amazon.awssdk.identity.spi", "AwsCredentialsIdentity");
    private static final Symbol RETRY_STRATEGY = jdk("software.amazon.awssdk.retries", "RetryStrategy");
    private static final Symbol SYNC_HTTP = jdk("software.amazon.awssdk.http", "SdkSyncHttpClient");
    private static final Symbol ASYNC_HTTP = jdk("software.amazon.awssdk.http", "SdkAsyncHttpClient");
    private static final Symbol INTERCEPTOR = jdk("software.amazon.awssdk.pipeline", "ExecutionInterceptor");
    private static final Symbol TELEMETRY = jdk("software.amazon.awssdk.telemetry.spi", "TelemetryConfiguration");
    private static final Symbol METRIC_PUBLISHER = jdk("software.amazon.awssdk.metrics", "MetricPublisher");
    private static final Symbol ENGINE = jdk("software.amazon.awssdk.pipeline", "AwsCallEngine");
    private static final Symbol RUNTIME = jdk("software.amazon.awssdk.pipeline", "ClientRuntime");
    private static final Symbol PLAN = jdk("software.amazon.awssdk.pipeline", "PipelinePlan");
    private static final Symbol AWS_OPERATION = jdk("software.amazon.awssdk.pipeline", "AwsOperation");
    private static final Symbol BUILD_STATE = jdk("software.amazon.awssdk.pipeline.client", "AwsClientBuildState");
    private static final Symbol PARTS = jdk("software.amazon.awssdk.pipeline.client", "AwsClientParts");
    private static final Symbol DEFINITION = jdk("software.amazon.awssdk.pipeline.client",
                                                 "AwsServiceClientDefinition");
    private static final Symbol MODELED_AUTH = jdk("software.amazon.awssdk.auth.api", "ModeledAuth");
    private static final Symbol HTTP_CHECKSUM_METADATA = jdk("software.amazon.awssdk.checksums",
                                                             "HttpChecksumMetadata");
    private static final Symbol AUTH_SCHEME_SELECTOR = jdk("software.amazon.awssdk.auth.api", "AuthSchemeSelector");
    private static final Symbol AUTH_SCHEME_OPTION = jdk("software.amazon.awssdk.auth.api", "AuthSchemeOption");
    private static final Symbol AWS_AUTH_SCHEME_OPTIONS = jdk("software.amazon.awssdk.auth.aws",
                                                              "AwsAuthSchemeOptions");
    private static final Symbol AUTH_OPTIONS_RESOLVER = jdk("software.amazon.awssdk.pipeline",
                                                            "AuthOptionsResolver");
    private static final Symbol CALL_OPTIONS = jdk("software.amazon.awssdk.core.override", "CallOptions");
    private static final Symbol AWS_CLIENT_OPTION = jdk("software.amazon.awssdk.awscore.client.config",
                                                        "AwsClientOption");
    private static final Symbol DEFAULT_CREDENTIALS = jdk("software.amazon.awssdk.identity.providers",
                                                          "DefaultCredentialsProvider");
    private static final Symbol SYSTEM_SETTINGS = jdk("software.amazon.awssdk.profiles", "SystemSettings");
    private static final Symbol PUBLISHER = jdk("org.reactivestreams", "Publisher");
    private static final Symbol EVENT_STREAM_RESPONSE_HANDLER = jdk("software.amazon.awssdk.awscore.eventstream",
                                                                    "EventStreamResponseHandler");
    private static final Symbol PUBLISHER_EVENT_STREAM = jdk("software.amazon.awssdk.protocols.core.eventstream",
                                                             "PublisherEventStream");

    private ClientGenerator() {
    }

    static void generate(AwsCodegenContext context) {
        ClientPlan plan = ClientPlan.of(context, ClientSurfaceClassifier.V2_BASELINE);
        render(plan, context.writerDelegator());
        PaginatorGenerator.generate(context, plan);
        WaiterGenerator.generate(context, plan);
        PresignerGenerator.generate(context, plan, ClientSurfaceClassifier.V2_BASELINE);
        UtilitiesGenerator.generate(context, plan, ClientSurfaceClassifier.V2_BASELINE);
    }

    static void render(ClientPlan plan, WriterDelegator<AwsJavaWriter> writers) {
        renderBindings(plan, writers);
        renderInterface(plan, writers, false);
        renderBuilder(plan, writers, false);
        renderDefaultBuilder(plan, writers, false);
        renderDefaultClient(plan, writers, false);
        if (plan.asyncClient() != null) {
            renderInterface(plan, writers, true);
            renderResponseHandlers(plan, writers);
            renderBuilder(plan, writers, true);
            renderDefaultBuilder(plan, writers, true);
            renderDefaultClient(plan, writers, true);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // <Service>OperationBindings

    private static void renderBindings(ClientPlan plan, WriterDelegator<AwsJavaWriter> writers) {
        Symbol type = plan.bindings();
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type);
            writer.write("/**");
            writer.write(" * The typed pipeline binding of every operation of {@code $L}, each a direct static "
                         + "reference to the", plan.service());
            writer.write(" * operation's descriptor in the operation table and to its generated input and output "
                         + "builders.");
            writer.write(" */");
            writer.openBlock("final class $L {", "}", type.getName(), () -> {
                for (ClientPlan.Operation operation : plan.operations()) {
                    writer.write("/** Binding of {@code $L}. */", operation.id());
                    writer.write("static final $T<$T, $T> $L = $T.<$T, $T>builder(", AWS_OPERATION,
                                 operation.input(), operation.output(), operation.constant(), AWS_OPERATION,
                                 operation.input(), operation.output());
                    writer.indent(2);
                    writer.write("$T.$L, $T::builder, $T::builder)", plan.operationTable(), operation.constant(),
                                 operation.input(), operation.output());
                    if (plan.endpointRules() != null) {
                        writer.write(".endpointBinding($T.$L)", plan.endpointRules(), operation.constant());
                    }
                    writer.write(".auth($L)", authExpression(writer, operation));
                    if (operation.httpChecksum() != null) {
                        writer.write(".httpChecksum($L)", httpChecksumExpression(writer, operation.httpChecksum()));
                    }
                    writer.write(".build();");
                    writer.dedent(2);
                    writer.write("");
                }
                writer.openBlock("private $L() {", "}", type.getName(), () -> {
                });
            });
        });
    }

    static String authExpression(AwsJavaWriter writer, ClientPlan.Operation operation) {
        ClientPlan.Auth auth = operation.auth();
        StringBuilder expression = new StringBuilder(writer.format("$T.builder().operationId($S)", MODELED_AUTH,
                                                                   operation.id().toString()));
        for (Map.Entry<String, Map<String, String>> scheme : auth.serviceSchemes().entrySet()) {
            if (scheme.getValue().isEmpty()) {
                expression.append(writer.format("\n    .addServiceScheme($S)", scheme.getKey()));
            } else {
                List<String> pairs = new ArrayList<>();
                scheme.getValue().forEach((k, v) -> pairs.add(writer.format("$S, $S", k, v)));
                expression.append(writer.format("\n    .addServiceScheme($S, $T.of($L))", scheme.getKey(), MAP,
                                                String.join(", ", pairs)));
            }
        }
        if (auth.serviceAuthTrait() != null) {
            expression.append(writer.format("\n    .serviceAuthTrait($L)",
                                            listOf(writer, auth.serviceAuthTrait())));
        }
        if (auth.operationAuthTrait() != null) {
            expression.append(writer.format("\n    .operationAuthTrait($L)",
                                            listOf(writer, auth.operationAuthTrait())));
        }
        if (auth.optionalAuth()) {
            expression.append("\n    .optionalAuth(true)");
        }
        if (auth.unsignedPayload()) {
            expression.append("\n    .unsignedPayload(true)");
        }
        return expression.append("\n    .build()").toString();
    }

    /** A static {@code HttpChecksumMetadata} construction equal to the runtime's reading of the trait value. */
    static String httpChecksumExpression(AwsJavaWriter writer, ClientPlan.HttpChecksum checksum) {
        StringBuilder expression = new StringBuilder(writer.format("$T.builder()", HTTP_CHECKSUM_METADATA));
        if (checksum.requestChecksumRequired()) {
            expression.append("\n    .requestChecksumRequired(true)");
        }
        if (checksum.requestAlgorithmMember() != null) {
            expression.append(writer.format("\n    .requestAlgorithmMember($S)", checksum.requestAlgorithmMember()));
        }
        if (checksum.requestValidationModeMember() != null) {
            expression.append(writer.format("\n    .requestValidationModeMember($S)",
                                            checksum.requestValidationModeMember()));
        }
        for (String algorithm : checksum.responseAlgorithms()) {
            expression.append(writer.format("\n    .addResponseAlgorithm($S)", algorithm));
        }
        return expression.append("\n    .build()").toString();
    }

    private static String listOf(AwsJavaWriter writer, List<String> values) {
        List<String> quoted = values.stream().map(v -> writer.format("$S", v)).toList();
        return writer.format("$T.of($L)", LIST, String.join(", ", quoted));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // <Service>Client / <Service>AsyncClient

    private static void renderInterface(ClientPlan plan, WriterDelegator<AwsJavaWriter> writers, boolean async) {
        Symbol type = async ? plan.asyncClient() : plan.syncClient();
        Symbol builder = async ? plan.asyncBuilder() : plan.syncBuilder();
        Symbol defaultBuilder = async ? plan.defaultAsyncBuilder() : plan.defaultSyncBuilder();
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type);
            writer.write("/**");
            writer.write(" * Service client for accessing {@code $L}$L.", plan.service(),
                         async ? " asynchronously" : "");
            writer.write(" *");
            writer.write(" * <p>Create one with {@link #create()} or {@link #builder()}. Clients are thread safe and "
                         + "should be shared;");
            writer.write(" * {@link #close()} releases the resources the SDK created for the client.");
            if (async) {
                writer.write(" *");
                writer.write(" * <p>Every operation returns its {@code CompletableFuture} before the call completes; "
                             + "SDK-owned continuations");
                writer.write(" * run on the client's completion executor, never on the common fork-join pool.");
            } else if (plan.operations().stream().anyMatch(ClientPlan.Operation::eventStream)) {
                writer.write(" *");
                writer.write(" * <p>As in V2, operations with an event stream are available only on the async "
                             + "client.");
            }
            writer.write(" */");
            writer.openBlock("public interface $L extends AutoCloseable {", "}", type.getName(), () -> {
                writer.write("/** The service's V2-compatible name: its endpoint prefix. */");
                writer.write("String SERVICE_NAME = $S;", plan.serviceName());
                writer.write("");
                writer.write("/** A client configured from the default configuration sources. */");
                writer.openBlock("static $L create() {", "}", type.getName(),
                                 () -> writer.write("return builder().build();"));
                writer.write("");
                writer.write("/** A builder of a client. */");
                writer.openBlock("static $T builder() {", "}", builder,
                                 () -> writer.write("return new $T();", defaultBuilder));
                for (ClientPlan.Operation operation : plan.operations()) {
                    if (operation.eventStream()) {
                        // As in V2, event stream operations are async only.
                        if (async) {
                            writeEventStreamOperation(writer, operation);
                        }
                        continue;
                    }
                    writeOperation(writer, plan, operation, async);
                    writePaginator(writer, operation, async);
                }
                Symbol waiter = async ? plan.asyncWaiter() : plan.syncWaiter();
                if (waiter != null) {
                    writer.write("");
                    writer.write("/** A waiter that borrows this client: closing it leaves this client open. */");
                    writer.openBlock("default $T waiter() {", "}", waiter,
                                     () -> writer.write("return $T.builder().client(this).build();", waiter));
                }
                if (!async && plan.utilities() != null) {
                    writer.write("");
                    writer.write("/** The service's utilities, sharing this client's configuration. */");
                    writer.write("$T utilities();", plan.utilities());
                }
                writer.write("");
                writer.write("/** The service's V2-compatible name, {@link #SERVICE_NAME}. */");
                writer.openBlock("default String serviceName() {", "}", () -> writer.write("return SERVICE_NAME;"));
                writer.write("");
                writer.write("/** The exact, immutable configuration resolved for this client and read by its "
                             + "calls. */");
                writer.write("$T serviceClientConfiguration();", CONFIGURATION);
                writer.write("");
                writer.write("/** Closes the resources the SDK created for this client; idempotent. */");
                writer.write("@Override");
                writer.write("void close();");
            });
        });
    }

    private static void writeOperation(AwsJavaWriter writer, ClientPlan plan, ClientPlan.Operation operation,
                                       boolean async) {
        String method = operation.method();
        String result = async ? writer.format("$T<$T>", COMPLETABLE_FUTURE, operation.output())
                              : writer.format("$T", operation.output());
        String throwsClause = async ? "" : throwsClause(writer, plan, operation);
        // With a response-transformer overload of the same arity, a bare null override would be ambiguous.
        String noOverride = operation.streamingOutput() ? writer.format("($T) null", OVERRIDE) : "null";
        if (operation.streamingInput()) {
            Symbol body = async ? ASYNC_REQUEST_BODY : REQUEST_BODY;
            writer.write("");
            doc(writer, operation, async, "with its streaming request body");
            writer.write("$L $L($T request, $T requestBody, $T overrideConfiguration)$L;", result, method,
                         operation.input(), body, OVERRIDE, throwsClause);
            writer.write("");
            doc(writer, operation, async, "with its streaming request body");
            writer.openBlock("default $L $L($T request, $T requestBody)$L {", "}", result, method, operation.input(),
                             body, throwsClause,
                             () -> writer.write("return $L(request, requestBody, null);", method));
            if (operation.consumerOverload()) {
                writer.write("");
                doc(writer, operation, async, "built by {@code request}, with its streaming request body");
                writer.openBlock(writer.format("default $L $L($T<$T.Builder> request, $T requestBody)$L {", result,
                                               method, CONSUMER, operation.input(), body, throwsClause), "}", () -> {
                        writer.write("$T.Builder builder = $T.builder();", operation.input(), operation.input());
                        writer.write("request.accept(builder);");
                        writer.write("return $L(builder.build(), requestBody, null);", method);
                    });
            }
        } else if (!operation.unitInput()) {
            writer.write("");
            doc(writer, operation, async, "with an explicit per-call override");
            writer.write("$L $L($T request, $T overrideConfiguration)$L;", result, method, operation.input(),
                         OVERRIDE, throwsClause);
            writer.write("");
            doc(writer, operation, async, "");
            writer.openBlock("default $L $L($T request)$L {", "}", result, method, operation.input(), throwsClause,
                             () -> writer.write("return $L(request, $L);", method, noOverride));
            if (operation.consumerOverload()) {
                writer.write("");
                doc(writer, operation, async, "with the request built by {@code request}");
                writer.openBlock("default $L $L($T<$T.Builder> request)$L {", "}", result, method, CONSUMER,
                                 operation.input(), throwsClause, () -> {
                        writer.write("$T.Builder builder = $T.builder();", operation.input(), operation.input());
                        writer.write("request.accept(builder);");
                        writer.write("return $L(builder.build(), $L);", method, noOverride);
                    });
            }
            if (operation.noArgumentOverload()) {
                writer.write("");
                doc(writer, operation, async, "with an empty request");
                writer.openBlock("default $L $L()$L {", "}", result, method, throwsClause,
                                 () -> writer.write("return $L($T.builder().build(), $L);", method,
                                                    operation.input(), noOverride));
            }
        } else {
            // An operation without modeled input: the no-argument form, and an explicit override.
            writer.write("");
            doc(writer, operation, async, "with an explicit per-call override");
            writer.write("$L $L($T overrideConfiguration)$L;", result, method, OVERRIDE, throwsClause);
            writer.write("");
            doc(writer, operation, async, "");
            writer.openBlock("default $L $L()$L {", "}", result, method, throwsClause,
                             () -> writer.write("return $L(($T) null);", method, OVERRIDE));
        }
        if (operation.streamingOutput() && !operation.unitInput()) {
            writeResponseTransformer(writer, operation, async, throwsClause);
        }
    }

    private static void writePaginator(AwsJavaWriter writer, ClientPlan.Operation operation, boolean async) {
        Symbol paginator = async ? operation.asyncPaginator() : operation.syncPaginator();
        if (paginator == null) {
            return;
        }
        String method = operation.method() + "Paginator";
        writer.write("");
        writer.write("/**");
        writer.write(" * The pages of the {@code $L} operation, $L from this client by the modeled tokens.",
                     operation.name(), async ? "requested on downstream demand" : "fetched lazily");
        writer.write(" */");
        writer.openBlock("default $T $L($T request) {", "}", paginator, method, operation.input(),
                         () -> writer.write("return new $T(this, request);", paginator));
        writer.write("");
        writer.write("/** The pages of the {@code $L} operation, with the request built by {@code request}. */",
                     operation.name());
        writer.openBlock("default $T $L($T<$T.Builder> request) {", "}", paginator, method, CONSUMER,
                         operation.input(), () -> {
                writer.write("$T.Builder builder = $T.builder();", operation.input(), operation.input());
                writer.write("request.accept(builder);");
                writer.write("return $L(builder.build());", method);
            });
    }

    private static void writeResponseTransformer(AwsJavaWriter writer, ClientPlan.Operation operation, boolean async,
                                                 String throwsClause) {
        Symbol transformer = async ? ASYNC_RESPONSE_TRANSFORMER : RESPONSE_TRANSFORMER;
        String result = async ? writer.format("$T<ReturnT>", COMPLETABLE_FUTURE) : "ReturnT";
        writer.write("");
        writer.write("/**");
        writer.write(" * Invokes {@code $L}, handing its streaming response body to {@code responseTransformer}.",
                     operation.name());
        writer.write(" *");
        writer.write(" * <p>As in V2, an implementation that does not override this overload fails with");
        writer.write(" * {@link UnsupportedOperationException}; the generated client streams the live payload to the");
        writer.write(" * transformer.");
        writer.write(" */");
        writer.openBlock(writer.format("default <ReturnT> $L $L($T request, $T<$T, ReturnT> responseTransformer)$L {",
                                       result, operation.method(), operation.input(), transformer, operation.output(),
                                       throwsClause), "}", () -> {
                if (async) {
                    writer.write("return $T.failedFuture(new $T());", COMPLETABLE_FUTURE, UNSUPPORTED_OPERATION);
                } else {
                    writer.write("throw new $T();", UNSUPPORTED_OPERATION);
                }
            });
        writer.write("");
        writer.openBlock(writer.format("default <ReturnT> $L $L($T<$T.Builder> request, $T<$T, ReturnT> "
                                       + "responseTransformer)$L {", result, operation.method(), CONSUMER,
                                       operation.input(), transformer, operation.output(), throwsClause), "}", () -> {
                writer.write("$T.Builder builder = $T.builder();", operation.input(), operation.input());
                writer.write("request.accept(builder);");
                writer.write("return $L(builder.build(), responseTransformer);", operation.method());
            });
    }

    private static void doc(AwsJavaWriter writer, ClientPlan.Operation operation, boolean async, String detail) {
        writer.write("/**");
        writer.write(" * Invokes the {@code $L} operation$L$L.", operation.name(), detail.isEmpty() ? "" : " ",
                     detail);
        if (async) {
            writer.write(" *");
            writer.write(" * <p>Returns before the call completes; the future completes exceptionally with the "
                         + "call's SDK exception.");
        }
        writer.write(" */");
    }

    private static String throwsClause(AwsJavaWriter writer, ClientPlan plan, ClientPlan.Operation operation) {
        List<String> types = new ArrayList<>();
        operation.errors().forEach(error -> types.add(writer.format("$T", error)));
        types.add(writer.format("$T", AWS_SERVICE_EXCEPTION));
        types.add(writer.format("$T", SDK_CLIENT_EXCEPTION));
        types.add(writer.format("$T", plan.serviceException()));
        return "\n        throws " + String.join(", ", types);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // <Service>ClientBuilder / <Service>AsyncClientBuilder

    private static void renderBuilder(ClientPlan plan, WriterDelegator<AwsJavaWriter> writers, boolean async) {
        Symbol type = async ? plan.asyncBuilder() : plan.syncBuilder();
        Symbol client = async ? plan.asyncClient() : plan.syncClient();
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type);
            writer.write("/** Builder of a {@link $T}; not thread safe. */", client);
            writer.openBlock("public interface $L {", "}", type.getName(), () -> {
                writer.write("/** The region the client sends requests to. */");
                writer.write("$L region($T region);", type.getName(), REGION);
                writer.write("");
                writer.write("/** The provider of the client's AWS credentials. */");
                writer.write("$L credentialsProvider($T<? extends $T> credentialsProvider);", type.getName(),
                             IDENTITY_PROVIDER, CREDENTIALS);
                writer.write("");
                writer.write("/** The endpoint every request is sent to, instead of the resolved one. */");
                writer.write("$L endpointOverride($T endpointOverride);", type.getName(), URI);
                writer.write("");
                writer.write("/** The client's retry strategy. */");
                writer.write("$L retryStrategy($T retryStrategy);", type.getName(), RETRY_STRATEGY);
                writer.write("");
                if (async) {
                    writer.write("/** A built async HTTP client: caller-owned, never closed by the SDK client. */");
                    writer.write("$L httpClient($T httpClient);", type.getName(), ASYNC_HTTP);
                    writer.write("");
                    writer.write("/** The executor SDK-owned continuations run on: caller-owned. */");
                    writer.write("$L asyncCompletionExecutor($T executor);", type.getName(), EXECUTOR);
                } else {
                    writer.write("/** A built HTTP client: caller-owned, never closed by the SDK client. */");
                    writer.write("$L httpClient($T httpClient);", type.getName(), SYNC_HTTP);
                }
                writer.write("");
                writer.write("/** Appends an interceptor of every call of the client. */");
                writer.write("$L addExecutionInterceptor($T interceptor);", type.getName(), INTERCEPTOR);
                writer.write("");
                writer.write("/**");
                writer.write(" * The client's telemetry: its publishers of terminal call and attempt records and its");
                writer.write(" * telemetry failure channel. Opt-in: without a publisher the client does no");
                writer.write(" * telemetry work.");
                writer.write(" */");
                writer.write("$L telemetry($T telemetry);", type.getName(), TELEMETRY);
                writer.write("");
                writer.write("/** Appends a V2-style metric publisher: one {@code ApiCall} collection per call. */");
                writer.write("$L addMetricPublisher($T metricPublisher);", type.getName(),
                             METRIC_PUBLISHER);
                writer.write("");
                writer.write("/** Resolves the configuration once and builds the client. */");
                writer.write("$T build();", client);
            });
        });
    }

    private static void renderDefaultBuilder(ClientPlan plan, WriterDelegator<AwsJavaWriter> writers, boolean async) {
        Symbol type = async ? plan.defaultAsyncBuilder() : plan.defaultSyncBuilder();
        Symbol builder = async ? plan.asyncBuilder() : plan.syncBuilder();
        Symbol client = async ? plan.asyncClient() : plan.syncClient();
        Symbol defaultClient = async ? plan.defaultAsyncClient() : plan.defaultSyncClient();
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type);
            writer.write("/** Default {@link $T}: forwards every option to the runtime's build state. */", builder);
            writer.openBlock("final class $L implements $T {", "}", type.getName(), builder, () -> {
                writer.write("private final $T state = new $T();", BUILD_STATE, BUILD_STATE);
                writer.write("");
                writeModernAuth(writer, plan, type.getName());
                setter(writer, builder, "region", writer.format("$T", REGION), "region");
                setter(writer, builder, "credentialsProvider",
                       writer.format("$T<? extends $T>", IDENTITY_PROVIDER, CREDENTIALS), "credentialsProvider");
                setter(writer, builder, "endpointOverride", writer.format("$T", URI), "endpointOverride");
                setter(writer, builder, "retryStrategy", writer.format("$T", RETRY_STRATEGY), "retryStrategy");
                if (async) {
                    setter(writer, builder, "httpClient", writer.format("$T", ASYNC_HTTP), "asyncHttpClient");
                    setter(writer, builder, "asyncCompletionExecutor", writer.format("$T", EXECUTOR),
                           "completionExecutor");
                } else {
                    setter(writer, builder, "httpClient", writer.format("$T", SYNC_HTTP), "httpClient");
                }
                setter(writer, builder, "addExecutionInterceptor", writer.format("$T", INTERCEPTOR),
                       "addInterceptor");
                setter(writer, builder, "telemetry", writer.format("$T", TELEMETRY), "telemetry");
                setter(writer, builder, "addMetricPublisher", writer.format("$T", METRIC_PUBLISHER),
                       "addMetricPublisher");
                writer.write("@Override");
                writer.openBlock("public $T build() {", "}", client,
                                 () -> writer.write("return new $T(state.$L(definition()));", defaultClient,
                                                    async ? "buildAsync" : "buildSync"));
                writer.write("");
                writeDefinition(writer, plan);
            });
        });
    }

    /**
     * The Modern Auth setup of a builder over {@code state} (Requirements 13.1, 13.2): a constructor installing the
     * service's AWS schemes, and the modeled options with the signing name from the model and the region from the
     * resolved configuration. Nothing for a service without AWS auth schemes. Shared by client and presigner
     * builders, so a presigner selects and configures auth exactly as its client does.
     */
    static void writeModernAuth(AwsJavaWriter writer, ClientPlan plan, String typeName) {
        String schemes = awsAuthSchemes(plan);
        if (schemes == null) {
            return;
        }
        writer.openBlock("$L() {", "}", typeName, () -> {
            writer.write("state.authSchemeSelector($T.create($T.$L()));", AUTH_SCHEME_SELECTOR,
                         AWS_AUTH_SCHEME_OPTIONS, schemes);
            writer.write("state.authOptions($L::authOptions);", typeName);
        });
        writer.write("");
        String signerDefaults = ClientSurfaceClassifier.V2_SIGNER_DEFAULTS.get(plan.service());
        writer.openBlock("private static $T<$T> authOptions($T auth, $T options) {", "}", LIST,
                         AUTH_SCHEME_OPTION, MODELED_AUTH, CALL_OPTIONS, () -> {
                writer.write("return options.get($T.AWS_REGION)", AWS_CLIENT_OPTION);
                if (signerDefaults == null) {
                    writer.write("              .map(region -> $T.modeled(auth, region.id()))",
                                 AWS_AUTH_SCHEME_OPTIONS);
                } else {
                    // The service's V2 auth-type signer defaults (for S3: no payload signing over HTTPS).
                    writer.write("              .map(region -> $T.modeled(auth, region.id(), null, $T.$L()))",
                                 AWS_AUTH_SCHEME_OPTIONS, AWS_AUTH_SCHEME_OPTIONS, signerDefaults);
                }
                writer.write("              .orElseGet(() -> $T.modeled().options(auth, options));",
                             AUTH_OPTIONS_RESOLVER);
            });
        writer.write("");
    }

    /** The {@code static definition()} of the service: its identity, protocols, registry, rules and credentials. */
    static void writeDefinition(AwsJavaWriter writer, ClientPlan plan) {
        writer.openBlock("static $T definition() {", "}", DEFINITION, () -> {
            writer.write("return new $T($T.serviceId(), $T.SERVICE_NAME,", DEFINITION,
                         plan.serviceMetadata(), plan.syncClient());
            boolean credentials = needsAwsCredentials(plan);
            writer.write("        $T.serviceProtocols(), $T.typeRegistry(), $L)$L", plan.serviceMetadata(),
                         plan.serviceMetadata(), plan.endpointRules() == null ? "null"
                             : writer.format("$T.ruleSet()", plan.endpointRules()), credentials ? "" : ";");
            if (credentials) {
                // Without a credentials provider, the default chain over the client's JVM-property and
                // environment sources. The profile source is not bridged: the chain reads its own profile
                // file, as SystemSettings carries only JVM properties and environment variables.
                writer.write("    .withDefaultCredentials(sources -> $T.builder()", DEFAULT_CREDENTIALS);
                writer.write("        .settings(new $T(sources.jvmProperties(), sources.environment()))",
                             SYSTEM_SETTINGS);
                writer.write("        .build());");
            }
        });
    }

    /**
     * The {@code AwsAuthSchemeOptions} factory of the service's auth schemes, or {@code null} when the service models
     * no AWS auth scheme (then the generated client does not depend on {@code auth-aws}).
     */
    private static String awsAuthSchemes(ClientPlan plan) {
        Set<String> modeled = modeledSchemes(plan);
        if (modeled.contains("aws.auth#sigv4-s3express")) {
            return "s3Schemes";
        }
        boolean aws = modeled.contains("aws.auth#sigv4") || modeled.contains("aws.auth#sigv4a")
                      || modeled.contains("smithy.api#httpBearerAuth");
        return aws ? "defaultSchemes" : null;
    }

    /**
     * Whether the service models a scheme that signs with AWS credentials (SigV4, SigV4a, S3 Express); only then do
     * its generated builders default to the {@code DefaultCredentialsProvider} chain (and depend on
     * {@code identity-providers}).
     */
    private static boolean needsAwsCredentials(ClientPlan plan) {
        Set<String> modeled = modeledSchemes(plan);
        return modeled.contains("aws.auth#sigv4") || modeled.contains("aws.auth#sigv4a")
               || modeled.contains("aws.auth#sigv4-s3express");
    }

    private static Set<String> modeledSchemes(ClientPlan plan) {
        return plan.operations().isEmpty() ? Set.of() : plan.operations().get(0).auth().serviceSchemes().keySet();
    }

    private static void setter(AwsJavaWriter writer, Symbol builder, String method, String parameterType,
                               String stateMethod) {
        writer.write("@Override");
        writer.openBlock("public $T $L($L value) {", "}", builder, method, parameterType, () -> {
            writer.write("state.$L(value);", stateMethod);
            writer.write("return this;");
        });
        writer.write("");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Default<Service>Client / Default<Service>AsyncClient

    private static void renderDefaultClient(ClientPlan plan, WriterDelegator<AwsJavaWriter> writers, boolean async) {
        Symbol type = async ? plan.defaultAsyncClient() : plan.defaultSyncClient();
        Symbol client = async ? plan.asyncClient() : plan.syncClient();
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type);
            writer.write("/**");
            writer.write(" * Default {@link $T}. Each operation's {@link $T} is a final field built once from the "
                         + "operation's", client, PLAN);
            writer.write(" * static binding and the client's resolved runtime; each operation method is one direct "
                         + "engine call.");
            writer.write(" * Thread safe: the client holds no per-call state.");
            writer.write(" */");
            writer.openBlock("final class $L implements $T {", "}", type.getName(), client, () -> {
                writer.write("private final $T parts;", PARTS);
                writer.write("private final $T engine;", ENGINE);
                List<ClientPlan.Operation> operations = plan.operations().stream()
                                                            .filter(o -> async || !o.eventStream()).toList();
                for (ClientPlan.Operation operation : operations) {
                    writer.write("private final $T<$T, $T> $LPlan;", PLAN, operation.input(), operation.output(),
                                 operation.method());
                }
                writer.write("");
                writer.openBlock("$L($T parts) {", "}", type.getName(), PARTS, () -> {
                    writer.write("this.parts = parts;");
                    writer.write("this.engine = parts.engine();");
                    writer.write("try {");
                    writer.indent();
                    writer.write("$T runtime = parts.runtime();", RUNTIME);
                    for (ClientPlan.Operation operation : operations) {
                        writer.write("this.$LPlan = runtime.plan($T.$L);", operation.method(), plan.bindings(),
                                     operation.constant());
                    }
                    writer.dedent();
                    writer.write("} catch (RuntimeException e) {");
                    writer.indent();
                    writer.write("// Plans are resolved once per client; a client that cannot plan releases what "
                                 + "it acquired.");
                    writer.write("parts.close();");
                    writer.write("throw e;");
                    writer.dedent();
                    writer.write("}");
                });
                for (ClientPlan.Operation operation : operations) {
                    if (operation.eventStream()) {
                        writeEventStreamImplementation(writer, operation);
                    } else {
                        writeImplementation(writer, operation, async);
                    }
                }
                if (async && plan.asyncWaiter() != null) {
                    // The waiter borrows this client and schedules its delays on this client's retry scheduler.
                    writer.write("");
                    writer.write("@Override");
                    writer.openBlock("public $T waiter() {", "}", plan.asyncWaiter(), () -> {
                        writer.write("$T.Builder builder = $T.builder().client(this);", plan.asyncWaiter(),
                                     plan.asyncWaiter());
                        writer.write("parts.runtime().retryScheduler().ifPresent(builder::retryScheduler);");
                        writer.write("return builder.build();");
                    });
                }
                if (!async && plan.utilities() != null) {
                    writer.write("");
                    writer.write("@Override");
                    writer.openBlock("public $T utilities() {", "}", plan.utilities(),
                                     () -> writer.write("return Default$L.create(parts.configuration());",
                                                        plan.utilities().getName()));
                }
                writer.write("");
                writer.write("@Override");
                writer.openBlock("public $T serviceClientConfiguration() {", "}", CONFIGURATION,
                                 () -> writer.write("return parts.configuration();"));
                writer.write("");
                writer.write("@Override");
                writer.openBlock("public void close() {", "}", () -> writer.write("parts.close();"));
            });
        });
    }

    private static void writeImplementation(AwsJavaWriter writer, ClientPlan.Operation operation, boolean async) {
        String result = async ? writer.format("$T<$T>", COMPLETABLE_FUTURE, operation.output())
                              : writer.format("$T", operation.output());
        String execute = async ? "executeAsync" : "executeSync";
        writer.write("");
        writer.write("@Override");
        if (operation.streamingInput()) {
            writer.openBlock("public $L $L($T request, $T requestBody, $T overrideConfiguration) {", "}", result,
                             operation.method(), operation.input(), async ? ASYNC_REQUEST_BODY : REQUEST_BODY,
                             OVERRIDE, () -> writer.write("return engine.$L($LPlan, request, overrideConfiguration, "
                                                          + "requestBody);", execute, operation.method()));
        } else if (!operation.unitInput()) {
            writer.openBlock("public $L $L($T request, $T overrideConfiguration) {", "}", result, operation.method(),
                             operation.input(), OVERRIDE,
                             () -> writer.write("return engine.$L($LPlan, request, overrideConfiguration, null);",
                                                execute, operation.method()));
        } else {
            writer.openBlock("public $L $L($T overrideConfiguration) {", "}", result, operation.method(), OVERRIDE,
                             () -> writer.write("return engine.$L($LPlan, $T.getInstance(), overrideConfiguration, "
                                                + "null);", execute, operation.method(), operation.input()));
        }
        if (operation.streamingOutput() && !operation.unitInput()) {
            // The V2 transformer overload, over the engine's streaming-output entry point.
            Symbol transformer = async ? ASYNC_RESPONSE_TRANSFORMER : RESPONSE_TRANSFORMER;
            String transformed = async ? writer.format("$T<ReturnT>", COMPLETABLE_FUTURE) : "ReturnT";
            writer.write("");
            writer.write("@Override");
            writer.openBlock(writer.format("public <ReturnT> $L $L($T request, $T<$T, ReturnT> responseTransformer) {",
                                           transformed, operation.method(), operation.input(), transformer,
                                           operation.output()), "}",
                             () -> writer.write("return engine.$L($LPlan, request, null, null, responseTransformer);",
                                                execute, operation.method()));
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Event stream operations (async only, as in V2)

    /** {@code <Operation>ResponseHandler}: V2's handler interface of each operation whose output is an event stream. */
    private static void renderResponseHandlers(ClientPlan plan, WriterDelegator<AwsJavaWriter> writers) {
        for (ClientPlan.Operation operation : plan.operations()) {
            Symbol type = operation.responseHandler();
            if (type == null) {
                continue;
            }
            writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
                writer.reserveTypeNames(type);
                writer.write("/**");
                writer.write(" * Receives the response and then the event stream of the {@code $L} operation: "
                             + "{@code responseReceived}", operation.name());
                writer.write(" * with the output without its event stream, then {@code onEventStream} with the "
                             + "publisher of the events,");
                writer.write(" * then exactly one of {@code complete} or {@code exceptionOccurred}.");
                writer.write(" */");
                writer.openBlock("public interface $L extends $T<$T, $T> {", "}", type.getName(),
                                 EVENT_STREAM_RESPONSE_HANDLER, operation.output(), operation.outputEvent(), () -> {
                    });
            });
        }
    }

    /**
     * The async methods of an event stream operation: with an output event stream, V2's
     * {@code CompletableFuture<Void> op(request[, requestStream], asyncResponseHandler)}; with only an input event
     * stream, {@code CompletableFuture<Output> op(request, requestStream)}; each with its request-builder consumer
     * overload. There is no request-only method.
     */
    private static void writeEventStreamOperation(AwsJavaWriter writer, ClientPlan.Operation operation) {
        String result = operation.responseHandler() != null ? writer.format("$T<Void>", COMPLETABLE_FUTURE)
                                                            : writer.format("$T<$T>", COMPLETABLE_FUTURE,
                                                                            operation.output());
        String rest = eventStreamParameters(writer, operation);
        String arguments = eventStreamArguments(operation);
        writer.write("");
        eventStreamDoc(writer, operation, "");
        if (operation.unitInput()) {
            writer.write("$L $L($L);", result, operation.method(), rest);
            return;
        }
        writer.write("$L $L($T request, $L);", result, operation.method(), operation.input(), rest);
        if (operation.consumerOverload()) {
            writer.write("");
            eventStreamDoc(writer, operation, " with the request built by {@code request}");
            writer.openBlock(writer.format("default $L $L($T<$T.Builder> request, $L) {", result, operation.method(),
                                           CONSUMER, operation.input(), rest), "}", () -> {
                    writer.write("$T.Builder builder = $T.builder();", operation.input(), operation.input());
                    writer.write("request.accept(builder);");
                    writer.write("return $L(builder.build(), $L);", operation.method(), arguments);
                });
        }
    }

    private static void writeEventStreamImplementation(AwsJavaWriter writer, ClientPlan.Operation operation) {
        String result = operation.responseHandler() != null ? writer.format("$T<Void>", COMPLETABLE_FUTURE)
                                                            : writer.format("$T<$T>", COMPLETABLE_FUTURE,
                                                                            operation.output());
        String rest = eventStreamParameters(writer, operation);
        String handler = operation.responseHandler() != null ? ", asyncResponseHandler" : "";
        writer.write("");
        writer.write("@Override");
        if (operation.unitInput()) {
            writer.openBlock("public $L $L($L) {", "}", result, operation.method(), rest,
                             () -> writer.write("return engine.executeAsync($LPlan, $T.getInstance(), null, null$L);",
                                                operation.method(), operation.input(), handler));
            return;
        }
        writer.openBlock("public $L $L($T request, $L) {", "}", result, operation.method(), operation.input(), rest,
                         () -> {
                String input = "request";
                if (operation.inputEvent() != null) {
                    // The input events are the input's event stream member, as the protocol reads it.
                    writer.write("$T.Builder builder = request.toBuilder();", operation.input());
                    writer.write("builder.setMemberValue(request.schema().member($S), $T.of(requestStream));",
                                 operation.inputEventMember(), PUBLISHER_EVENT_STREAM);
                    input = "builder.build()";
                }
                writer.write("return engine.executeAsync($LPlan, $L, null, null$L);", operation.method(), input,
                             handler);
            });
    }

    private static String eventStreamParameters(AwsJavaWriter writer, ClientPlan.Operation operation) {
        List<String> parameters = new ArrayList<>();
        if (operation.inputEvent() != null) {
            parameters.add(writer.format("$T<$T> requestStream", PUBLISHER, operation.inputEvent()));
        }
        if (operation.responseHandler() != null) {
            parameters.add(writer.format("$T asyncResponseHandler", operation.responseHandler()));
        }
        return String.join(", ", parameters);
    }

    private static String eventStreamArguments(ClientPlan.Operation operation) {
        List<String> arguments = new ArrayList<>();
        if (operation.inputEvent() != null) {
            arguments.add("requestStream");
        }
        if (operation.responseHandler() != null) {
            arguments.add("asyncResponseHandler");
        }
        return String.join(", ", arguments);
    }

    private static void eventStreamDoc(AwsJavaWriter writer, ClientPlan.Operation operation, String detail) {
        writer.write("/**");
        writer.write(" * Invokes the {@code $L} operation$L.", operation.name(), detail);
        writer.write(" *");
        if (operation.inputEvent() != null) {
            writer.write(" * <p>{@code requestStream} is subscribed when the call starts; its events, requested "
                         + "one at a time as the");
            writer.write(" * transport sends them, are the input event stream, which ends when it completes.");
        }
        if (operation.responseHandler() != null) {
            writer.write(" * <p>{@code asyncResponseHandler} receives the output and then its event stream; the "
                         + "returned future");
            writer.write(" * completes after the event stream does, or exceptionally with the call's SDK "
                         + "exception.");
        } else {
            writer.write(" * <p>Returns before the call completes; the future completes exceptionally with the "
                         + "call's SDK exception.");
        }
        writer.write(" */");
    }

    private static Symbol jdk(String packageName, String name) {
        return Symbol.builder().namespace(packageName, ".").name(name).build();
    }
}
