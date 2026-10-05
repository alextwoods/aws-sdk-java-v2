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

package software.amazon.awssdk.http.urlconnection;

import software.amazon.awssdk.core.exception.SdkClientConfigurationException;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.provider.HttpClientConstruction;
import software.amazon.awssdk.http.provider.SdkHttpService;

/**
 * The service-provider entry of the JDK URLConnection sync transport
 * ({@code META-INF/services/software.amazon.awssdk.http.provider.SdkHttpService}).
 *
 * <p>Priority {@value #PRIORITY}. V2 ({@code ClasspathSdkHttpServiceProvider.SYNC_HTTP_SERVICES_PRIORITY}) preferred,
 * in order, Apache 5, Apache 4, URLConnection, CRT. V3 keeps that order with "higher wins" sync priorities:
 * apache5-client 300, apache-client 200, url-connection-client 100, aws-crt-client-sync 20. URLConnection is
 * therefore the default only when neither Apache transport is installed, and it ties with no other V3 transport.
 *
 * <p>Metadata is constant and resource-free; {@link #create} acquires nothing but the client, which the resolver
 * registers and releases itself.
 */
public final class UrlConnectionSdkHttpService implements SdkHttpService {
    /** {@link #implementationName()}. */
    public static final String NAME = "url-connection-client";
    /** {@link #priority()}; see the class documentation. */
    public static final int PRIORITY = 100;

    @Override
    public String implementationName() {
        return NAME;
    }

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public HttpClientCapabilities capabilities() {
        return UrlConnectionHttpClient.CAPABILITIES;
    }

    @Override
    public SdkSyncHttpClient create(HttpClientConstruction construction) {
        try {
            return UrlConnectionHttpClient.fromConfiguration(construction.configuration());
        } catch (IllegalArgumentException e) {
            throw SdkClientConfigurationException.builder()
                                                 .message("Invalid " + NAME + " configuration: " + e.getMessage())
                                                 .cause(e)
                                                 .build();
        }
    }
}
