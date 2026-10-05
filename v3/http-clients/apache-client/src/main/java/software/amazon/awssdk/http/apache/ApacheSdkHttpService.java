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

package software.amazon.awssdk.http.apache;

import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.provider.HttpClientConstruction;
import software.amazon.awssdk.http.provider.SdkHttpService;

/**
 * The sync service provider of {@code apache-client}, registered in
 * {@code META-INF/services/software.amazon.awssdk.http.provider.SdkHttpService}.
 *
 * <p>Priority {@value #PRIORITY}: below {@code apache5-client} (300), the preferred sync candidate, and above
 * {@code url-connection-client} (100) and {@code aws-crt-client} sync (20). The sync priorities stay distinct so a
 * class path with several of them resolves without a tie.
 *
 * <p>{@link #create} reads and validates the configuration before acquiring anything, then registers the connection
 * pool and (if enabled) the idle reaper as SDK owned, in that order, so a failed construction releases them.
 */
public final class ApacheSdkHttpService implements SdkHttpService {
    /** The implementation name used in diagnostics. */
    public static final String IMPLEMENTATION_NAME = "apache-client";
    /** The default-selection priority. */
    public static final int PRIORITY = 200;

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
        return ApacheHttpClient.CAPABILITIES;
    }

    @Override
    public SdkSyncHttpClient create(HttpClientConstruction construction) {
        ApacheSettings settings = ApacheSettings.from(construction.configuration());
        return ApacheHttpClient.create(settings, construction::sdkOwned);
    }

    @Override
    public String toString() {
        return "ApacheSdkHttpService[" + IMPLEMENTATION_NAME + ", priority " + PRIORITY + "]";
    }
}
