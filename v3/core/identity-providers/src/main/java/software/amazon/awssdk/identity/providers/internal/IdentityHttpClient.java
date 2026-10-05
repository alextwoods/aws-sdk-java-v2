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

package software.amazon.awssdk.identity.providers.internal;

import java.net.URI;
import java.time.Duration;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientOption;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpHeaders;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.http.provider.HttpClientConfiguration;
import software.amazon.awssdk.http.provider.HttpClientResolver;
import software.amazon.awssdk.http.provider.ResolvedHttpClient;

/**
 * The sync HTTP transport of one identity provider (instance metadata, container, STS, SSO): a caller-supplied
 * {@link SdkSyncHttpClient} (caller-owned, never closed), or the default installed sync implementation, resolved on
 * first use through the HTTP SPI and closed with the provider (SDK-owned). Identity providers never depend on a
 * concrete HTTP engine. Thread-safe. Not a public API.
 */
public final class IdentityHttpClient implements AutoCloseable {
    /** Response bodies larger than this are rejected. */
    public static final int MAX_BODY_BYTES = 1024 * 1024;

    private final SdkSyncHttpClient explicit;
    private final Duration connectionTimeout;
    private ResolvedHttpClient<SdkSyncHttpClient> resolved;
    private boolean closed;

    private IdentityHttpClient(SdkSyncHttpClient explicit, Duration connectionTimeout) {
        this.explicit = explicit;
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * A transport over {@code explicit}, or the default implementation (with {@code connectionTimeout}, if not null)
     * when {@code explicit} is null.
     */
    public static IdentityHttpClient create(SdkSyncHttpClient explicit, Duration connectionTimeout) {
        return new IdentityHttpClient(explicit, connectionTimeout);
    }

    /** A response with a buffered UTF-8 body. */
    public record Response(int statusCode, HttpHeaders headers, String body) {
        public boolean isSuccessful() {
            return statusCode >= 200 && statusCode < 300;
        }

        @Override
        public String toString() {
            return "Response[" + statusCode + "]";
        }
    }

    /**
     * Sends {@code request} and reads the whole response.
     *
     * @throws SdkClientException if the transport fails or no HTTP implementation is available
     */
    public Response send(SdkHttpRequest request, Duration readTimeout) {
        SdkSyncHttpClient client = client();
        HttpExecutionOptions.Builder options = HttpExecutionOptions.builder();
        if (readTimeout != null) {
            options.readTimeout(readTimeout);
        }
        try (SyncHttpExchange exchange = client.prepare(request, options.build())) {
            SdkHttpResponse response = exchange.execute();
            HttpBody body = response.body().buffered();
            if (body.contentLength().orElse(0) > MAX_BODY_BYTES) {
                throw SdkClientException.create("The response from " + request.host() + " exceeds " + MAX_BODY_BYTES
                                                + " bytes.");
            }
            return new Response(response.statusCode(), response.headers(),
                                body.kind() == HttpBody.Kind.EMPTY ? "" : body.asUtf8String());
        } catch (SdkClientException e) {
            throw e;
        } catch (RuntimeException e) {
            throw SdkClientException.create("Unable to reach " + request.scheme() + "://" + request.authority() + ": "
                                            + e.getMessage(), e);
        }
    }

    private synchronized SdkSyncHttpClient client() {
        if (closed) {
            throw SdkClientException.create("The identity provider was closed.");
        }
        if (explicit != null) {
            return explicit;
        }
        if (resolved == null) {
            HttpClientConfiguration.Builder configuration = HttpClientConfiguration.builder();
            if (connectionTimeout != null) {
                configuration.put(HttpClientOption.CONNECTION_TIMEOUT, connectionTimeout);
            }
            try {
                resolved = HttpClientResolver.sync().configuration(configuration.build()).resolve();
            } catch (RuntimeException e) {
                throw SdkClientException.create("An identity provider needs a sync HTTP client: configure one on the "
                                                + "provider builder or add a sync HTTP implementation (for example "
                                                + "url-connection-client) to the classpath. " + e.getMessage(), e);
            }
        }
        return resolved.client();
    }

    /** Closes the SDK-owned default client, if one was resolved; a caller-supplied client is left open. */
    @Override
    public synchronized void close() {
        closed = true;
        if (resolved != null) {
            resolved.close();
            resolved = null;
        }
    }

    /** An unsent request builder for {@code uri} with {@code method}. */
    public static SdkHttpRequest.Builder request(String method, URI uri) {
        SdkHttpRequest.Builder builder = SdkHttpRequest.builder()
                                                       .method(method)
                                                       .scheme(uri.getScheme())
                                                       .host(uri.getHost())
                                                       .port(uri.getPort())
                                                       .encodedPath(uri.getRawPath() == null ? "/" : uri.getRawPath());
        String query = uri.getRawQuery();
        if (query != null && !query.isEmpty()) {
            for (String entry : query.split("&")) {
                int equals = entry.indexOf('=');
                if (equals < 0) {
                    builder.addRawQueryParameter(entry, null);
                } else {
                    builder.addRawQueryParameter(entry.substring(0, equals), entry.substring(equals + 1));
                }
            }
        }
        return builder;
    }
}
