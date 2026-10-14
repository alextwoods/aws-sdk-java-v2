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

package software.amazon.awssdk.protocol.smithytests;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.builder.SdkAsyncClientBuilder;
import software.amazon.awssdk.core.client.builder.SdkDefaultClientBuilder;
import software.amazon.awssdk.core.client.builder.SdkSyncClientBuilder;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.client.handler.SdkAsyncClientHandler;
import software.amazon.awssdk.core.client.handler.SdkSyncClientHandler;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.util.IdempotentUtils;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * A minimal hand-written sync and async client on the SDK's legacy request pipeline, standing in for a generated client
 * in the harness tests. {@code echo(body)} sends {@code POST /echo} with the body as text and an {@code X-Token} header
 * from the idempotency token generator, and returns the response body. A response with status 300 or above becomes an
 * {@link SdkServiceException} with that status. A null body fails in the marshaller, before anything is sent.
 */
final class EchoClients {
    static final String TOKEN_HEADER = "X-Token";

    private EchoClients() {
    }

    static final class EchoClient implements SdkAutoCloseable {
        private final SdkClientConfiguration configuration;
        private final SdkSyncClientHandler handler;

        private EchoClient(SdkClientConfiguration configuration) {
            this.configuration = configuration;
            // The constructor is protected, as generated clients use AwsSyncClientHandler.
            this.handler = new SdkSyncClientHandler(configuration) {
            };
        }

        static EchoClientBuilder builder() {
            return new EchoClientBuilder();
        }

        SdkClientConfiguration configuration() {
            return configuration;
        }

        EchoResponse echo(String body) {
            return handler.execute(params(configuration, body));
        }

        @Override
        public void close() {
            handler.close();
        }
    }

    static final class EchoAsyncClient implements SdkAutoCloseable {
        private final SdkClientConfiguration configuration;
        private final SdkAsyncClientHandler handler;

        private EchoAsyncClient(SdkClientConfiguration configuration) {
            this.configuration = configuration;
            this.handler = new SdkAsyncClientHandler(configuration);
        }

        static EchoAsyncClientBuilder builder() {
            return new EchoAsyncClientBuilder();
        }

        SdkClientConfiguration configuration() {
            return configuration;
        }

        CompletableFuture<EchoResponse> echo(String body) {
            return handler.execute(params(configuration, body));
        }

        @Override
        public void close() {
            handler.close();
        }
    }

    static final class EchoClientBuilder extends SdkDefaultClientBuilder<EchoClientBuilder, EchoClient>
        implements SdkSyncClientBuilder<EchoClientBuilder, EchoClient> {
        @Override
        protected EchoClient buildClient() {
            return new EchoClient(syncClientConfiguration());
        }
    }

    static final class EchoAsyncClientBuilder extends SdkDefaultClientBuilder<EchoAsyncClientBuilder, EchoAsyncClient>
        implements SdkAsyncClientBuilder<EchoAsyncClientBuilder, EchoAsyncClient> {
        @Override
        protected EchoAsyncClient buildClient() {
            return new EchoAsyncClient(asyncClientConfiguration());
        }
    }

    private static ClientExecutionParams<EchoRequest, EchoResponse> params(SdkClientConfiguration configuration,
                                                                           String body) {
        HttpResponseHandler<EchoResponse> responseHandler =
            (response, attributes) -> EchoResponse.builder().body(read(response.content())).build();
        HttpResponseHandler<SdkServiceException> errorHandler =
            (response, attributes) -> SdkServiceException.builder()
                                                         .statusCode(response.statusCode())
                                                         .message("Echo failed")
                                                         .build();
        return new ClientExecutionParams<EchoRequest, EchoResponse>()
            .withOperationName("Echo")
            .withInput(new EchoRequest(body))
            .withMarshaller(request -> marshall(configuration, request))
            .withResponseHandler(responseHandler)
            .withErrorResponseHandler(errorHandler);
    }

    private static SdkHttpFullRequest marshall(SdkClientConfiguration configuration, EchoRequest request) {
        if (request.body == null) {
            throw new IllegalArgumentException("body must not be null");
        }
        URI endpoint = configuration.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER).clientEndpoint();
        byte[] bytes = request.body.getBytes(StandardCharsets.UTF_8);
        return SdkHttpFullRequest.builder()
                                 .method(SdkHttpMethod.POST)
                                 .uri(endpoint)
                                 .encodedPath("/echo")
                                 .putRawQueryParameter("mode", "loud")
                                 .putHeader("Content-Type", "text/plain")
                                 .putHeader("Content-Length", Integer.toString(bytes.length))
                                 .putHeader(TOKEN_HEADER, IdempotentUtils.getGenerator().get())
                                 .contentStreamProvider(ContentStreamProvider.fromByteArray(bytes))
                                 .build();
    }

    private static byte[] read(Optional<? extends InputStream> content) {
        if (!content.isPresent()) {
            return new byte[0];
        }
        try (InputStream in = content.get()) {
            return IoUtils.toByteArray(in);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static final class EchoRequest extends SdkRequest {
        private final String body;

        private EchoRequest(String body) {
            this.body = body;
        }

        @Override
        public Optional<? extends RequestOverrideConfiguration> overrideConfiguration() {
            return Optional.empty();
        }

        @Override
        public Builder toBuilder() {
            return new Builder() {
                @Override
                public RequestOverrideConfiguration overrideConfiguration() {
                    return null;
                }

                @Override
                public SdkRequest build() {
                    return EchoRequest.this;
                }
            };
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return Collections.emptyList();
        }
    }

    static final class EchoResponse extends SdkResponse {
        private final byte[] body;

        private EchoResponse(BuilderImpl builder) {
            super(builder);
            this.body = builder.body;
        }

        static BuilderImpl builder() {
            return new BuilderImpl();
        }

        byte[] body() {
            return body.clone();
        }

        @Override
        public BuilderImpl toBuilder() {
            return new BuilderImpl(this);
        }

        @Override
        public List<SdkField<?>> sdkFields() {
            return Collections.emptyList();
        }

        static final class BuilderImpl extends SdkResponse.BuilderImpl {
            private byte[] body = new byte[0];

            private BuilderImpl() {
            }

            private BuilderImpl(EchoResponse response) {
                super(response);
                this.body = response.body;
            }

            BuilderImpl body(byte[] body) {
                this.body = body;
                return this;
            }

            @Override
            public EchoResponse build() {
                return new EchoResponse(this);
            }
        }
    }
}
