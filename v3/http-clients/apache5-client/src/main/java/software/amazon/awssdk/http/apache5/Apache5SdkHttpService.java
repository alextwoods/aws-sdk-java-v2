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
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.provider.HttpClientConstruction;
import software.amazon.awssdk.http.provider.SdkHttpService;

/**
 * The sync service provider of {@code apache5-client}, registered in
 * {@code META-INF/services/software.amazon.awssdk.http.provider.SdkHttpService}.
 *
 * <p>Priority {@value #PRIORITY}: the design names Apache 5 the preferred sync candidate, so it is above the
 * proposed {@code apache-client} (200) and {@code url-connection-client} (100) sync priorities; the three must stay
 * distinct so a class path with all of them resolves without a tie.
 *
 * <p>{@link #create} reads the configuration (see {@link Apache5Settings#from}) before acquiring anything, then
 * registers the connection pool and the engine client as SDK owned, in that order.
 */
public final class Apache5SdkHttpService implements SdkHttpService {
    /** The implementation name used in diagnostics. */
    public static final String IMPLEMENTATION_NAME = "apache5-client";
    /** The default-selection priority. */
    public static final int PRIORITY = 300;

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
        return Apache5HttpClient.CAPABILITIES;
    }

    @Override
    public SdkSyncHttpClient create(HttpClientConstruction construction) {
        Apache5Settings settings = Apache5Settings.from(construction.configuration());
        return Apache5HttpClient.create(settings, construction::sdkOwned);
    }

    @Override
    public String toString() {
        return "Apache5SdkHttpService[" + IMPLEMENTATION_NAME + ", priority " + PRIORITY + "]";
    }
}
