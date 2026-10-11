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

package software.amazon.awssdk.codegen.poet.protocoltest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.core.client.builder.SdkClientBuilder;
import software.amazon.awssdk.core.client.builder.SdkSyncClientBuilder;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.protocol.smithytests.CapturedRequest;
import software.amazon.awssdk.protocol.smithytests.ProtocolTestHarness;
import software.amazon.awssdk.protocol.smithytests.ProtocolTestHarnessFactory;
import software.amazon.awssdk.protocol.smithytests.StubbedResponse;
import software.amazon.awssdk.utils.IoUtils;

/**
 * A test-only {@link ProtocolTestHarnessFactory} over a fake {@link SdkHttpClient}, so the generator test can run
 * generated protocol tests against a generated client before the {@code ClientTransport}-backed harness exists.
 * Registered through {@code META-INF/services} in the codegen test resources.
 */
public final class HttpClientHarnessFactory implements ProtocolTestHarnessFactory {

    @Override
    public <C, B extends SdkClientBuilder<B, C>> ProtocolTestHarness<C> create(String protocol,
                                                                              Supplier<B> clientBuilder,
                                                                              Consumer<? super B> configuration) {
        return new ProtocolTestHarness<C>() {
            @Override
            public CapturedRequest captureRequest(Consumer<? super C> call, String endpoint) {
                FakeHttpClient http = new FakeHttpClient(null);
                C client = build(clientBuilder, configuration, endpoint, http);
                try {
                    call.accept(client);
                } catch (RuntimeException e) {
                    if (http.captured == null) {
                        throw e;
                    }
                }
                if (http.captured == null) {
                    throw new AssertionError("The client sent no request");
                }
                return http.captured;
            }

            @Override
            public <T> T respondWith(StubbedResponse response, Function<? super C, ? extends T> call) {
                return call.apply(build(clientBuilder, configuration, "https://example.com", new FakeHttpClient(response)));
            }
        };
    }

    private static <C, B extends SdkClientBuilder<B, C>> C build(Supplier<B> clientBuilder,
                                                                Consumer<? super B> configuration,
                                                                String endpoint,
                                                                SdkHttpClient http) {
        B builder = clientBuilder.get();
        configuration.accept(builder);
        builder.endpointOverride(URI.create(endpoint));
        ((SdkSyncClientBuilder<?, ?>) builder).httpClient(http);
        return builder.build();
    }

    private static final class FakeHttpClient implements SdkHttpClient {
        private final StubbedResponse response;
        private CapturedRequest captured;

        private FakeHttpClient(StubbedResponse response) {
            this.response = response;
        }

        @Override
        public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
            byte[] body = request.contentStreamProvider().map(p -> read(p.newStream())).orElse(new byte[0]);
            captured = CapturedRequest.create(request.httpRequest(), body);
            return new ExecutableHttpRequest() {
                @Override
                public HttpExecuteResponse call() throws IOException {
                    if (response == null) {
                        throw new IOException("Request captured");
                    }
                    Map<String, List<String>> headers = new LinkedHashMap<>();
                    response.headers().forEach((k, v) -> headers.put(k, Collections.singletonList(v)));
                    return HttpExecuteResponse.builder()
                                              .response(SdkHttpResponse.builder()
                                                                       .statusCode(response.statusCode())
                                                                       .headers(headers)
                                                                       .build())
                                              .responseBody(AbortableInputStream.create(
                                                  new ByteArrayInputStream(response.body())))
                                              .build();
                }

                @Override
                public void abort() {
                }
            };
        }

        private static byte[] read(InputStream stream) {
            try (InputStream in = stream) {
                return IoUtils.toByteArray(in);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void close() {
        }
    }
}
