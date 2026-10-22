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

import java.lang.reflect.Field;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.dynamicclient.internal.DynamicServiceClientConfigurationBuilder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.smithy.model.Model;

/**
 * Builds dynamic clients that send their calls to recording HTTP clients, and input documents.
 */
final class CallTestClients {
    static final URI ENDPOINT = URI.create("https://example.com");

    private CallTestClients() {
    }

    static DynamicClient client(Model model, RecordingHttpClients.Sync sync, RecordingHttpClients.Async async,
                                ExecutionInterceptor... interceptors) {
        return builder(model, sync, async, interceptors).endpointOverride(ENDPOINT).build();
    }

    /**
     * The builder {@link #client} builds, without the endpoint override.
     */
    static DynamicClient.Builder builder(Model model, RecordingHttpClients.Sync sync, RecordingHttpClients.Async async,
                                         ExecutionInterceptor... interceptors) {
        return DynamicClient.builder()
                            .model(model)
                            .region(Region.US_WEST_2)
                            .credentialsProvider(TestCredentials.PROVIDER)
                            .httpClient(sync)
                            .httpClient(async)
                            .overrideConfiguration(o -> {
                                for (ExecutionInterceptor interceptor : interceptors) {
                                    o.addExecutionInterceptor(interceptor);
                                }
                            });
    }

    /**
     * The client options behind the configuration builder a plugin receives, for plugins that set options the
     * configuration builder does not expose, such as the transport.
     */
    static SdkClientConfiguration.Builder clientOptions(SdkServiceClientConfiguration.Builder config) {
        try {
            Field field = DynamicServiceClientConfigurationBuilder.class.getDeclaredField("config");
            field.setAccessible(true);
            return (SdkClientConfiguration.Builder) field.get(config);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * A map document from alternating member names and values; a value that is not a document is converted with the
     * matching {@code Document.of}.
     */
    static Document map(Object... namesAndValues) {
        Map<String, Document> members = new LinkedHashMap<>();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            members.put((String) namesAndValues[i], document(namesAndValues[i + 1]));
        }
        return Document.of(members);
    }

    private static Document document(Object value) {
        if (value instanceof Document) {
            return (Document) value;
        }
        if (value instanceof String) {
            return Document.of((String) value);
        }
        if (value instanceof Integer) {
            return Document.of((Integer) value);
        }
        if (value instanceof Long) {
            return Document.of((Long) value);
        }
        if (value instanceof Double) {
            return Document.of((Double) value);
        }
        if (value instanceof Boolean) {
            return Document.of((Boolean) value);
        }
        if (value instanceof byte[]) {
            return Document.of((byte[]) value);
        }
        throw new IllegalArgumentException("Unsupported test value " + value);
    }
}
