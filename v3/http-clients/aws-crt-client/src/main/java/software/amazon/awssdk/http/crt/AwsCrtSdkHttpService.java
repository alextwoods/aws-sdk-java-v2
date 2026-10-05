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

package software.amazon.awssdk.http.crt;

import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.crt.internal.CrtNativeAvailability;
import software.amazon.awssdk.http.crt.internal.CrtSettings;
import software.amazon.awssdk.http.provider.HttpClientConstruction;
import software.amazon.awssdk.http.provider.SdkHttpService;

/**
 * The sync CRT implementation's service provider ({@code aws-crt-client-sync}).
 *
 * <p>Priority {@value #PRIORITY}. As in V2, where the CRT client was not the default when Apache or Netty was on the
 * class path, CRT ranks below every other default HTTP implementation of its mode; the value is chosen so it does not
 * tie with any other V3 transport (a tie would fail construction naming both). The numbers stay evidence dependent
 * (design, open decision 2).
 *
 * <p>Metadata is constant and resource free: it never touches a CRT class, so a missing native library cannot break
 * discovery. {@link #create} checks the native library, then builds the client, registering each native object with
 * the construction as SDK owned as soon as it exists.
 */
public final class AwsCrtSdkHttpService implements SdkHttpService {
    /** The implementation name. */
    public static final String NAME = "aws-crt-client-sync";
    /** The default-selection priority. */
    public static final int PRIORITY = 20;

    private static final HttpClientCapabilities CAPABILITIES =
        CrtSettings.capabilities(HttpTransportMode.SYNC, CrtSettings.DEFAULT_READ_BUFFER_SIZE);

    private final CrtNativeAvailability.Probe probe;

    public AwsCrtSdkHttpService() {
        this(CrtNativeAvailability.DEFAULT_PROBE);
    }

    AwsCrtSdkHttpService(CrtNativeAvailability.Probe probe) {
        this.probe = probe;
    }

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
        return CAPABILITIES;
    }

    @Override
    public SdkSyncHttpClient create(HttpClientConstruction construction) {
        return AwsCrtHttpClient.builder().nativeProbe(probe).applyConfiguration(construction.configuration())
                               .build(construction::sdkOwned);
    }
}
