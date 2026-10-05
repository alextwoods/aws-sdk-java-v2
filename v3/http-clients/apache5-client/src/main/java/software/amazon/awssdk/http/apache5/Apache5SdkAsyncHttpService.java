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

package software.amazon.awssdk.http.apache5;

import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.provider.HttpClientConstruction;
import software.amazon.awssdk.http.provider.SdkAsyncHttpService;

/**
 * The isolated async provider of {@code apache5-client}. Deliberately <strong>not</strong> registered in
 * {@code META-INF/services}: the design ships Apache 5 async only "if parity baseline requires and benchmarks pass",
 * V2 has no Apache 5 async client, and the generated Parity Manifest that would enable it does not exist yet, so the
 * gate is closed by default. It is constructible explicitly (for example
 * {@code HttpClientResolver.async().implementation(new Apache5SdkAsyncHttpService())}); registering it is a one-line
 * service file once the manifest enables it.
 *
 * <p>Priority {@value #PRIORITY}, below the proposed {@code netty-nio-client} async default (200) so that, even if
 * registered, it never ties with or displaces V2's default async transport. {@link #create} registers the connection
 * pool and the started engine client (its I/O reactor threads) as SDK owned, in that order.
 */
public final class Apache5SdkAsyncHttpService implements SdkAsyncHttpService {
    /** The implementation name used in diagnostics. */
    public static final String IMPLEMENTATION_NAME = "apache5-async-client";
    /** The default-selection priority. */
    public static final int PRIORITY = 50;

    @Override
    public String implementationName() {
        return IMPLEMENTATION_NAME;
    }

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public HttpClientCapabilities capabilities() {
        return Apache5AsyncHttpClient.CAPABILITIES;
    }

    @Override
    public SdkAsyncHttpClient create(HttpClientConstruction construction) {
        Apache5Settings settings = Apache5Settings.from(construction.configuration());
        return Apache5AsyncHttpClient.create(settings, construction::sdkOwned);
    }

    @Override
    public String toString() {
        return "Apache5SdkAsyncHttpService[" + IMPLEMENTATION_NAME + ", priority " + PRIORITY + "]";
    }
}
