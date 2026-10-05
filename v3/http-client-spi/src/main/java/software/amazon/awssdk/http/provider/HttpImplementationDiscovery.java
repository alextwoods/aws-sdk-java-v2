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

package software.amazon.awssdk.http.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Finds the HTTP implementations on the class path (Requirement 12.11).
 *
 * <p>{@link #discover(ClassLoader)} is the only service discovery of the HTTP SPI. It runs at most once per client
 * construction, only when neither a client nor an implementation is configured explicitly, and never while invoking an
 * operation. {@link HttpClientResolver#installed} replaces it, for example with a fixed
 * {@link InstalledHttpImplementations#of set}, so selection can be tested without a class path.
 */
public final class HttpImplementationDiscovery {
    private HttpImplementationDiscovery() {
    }

    /**
     * The implementations registered as {@code META-INF/services/software.amazon.awssdk.http.provider.SdkHttpService}
     * and {@code ...SdkAsyncHttpService} entries visible to {@code classLoader}. The result does not depend on the
     * order of those entries.
     *
     * @throws HttpImplementationSelectionException ({@link HttpImplementationSelectionException.Reason#DISCOVERY_FAILED})
     *         if a registered implementation cannot be loaded or instantiated, or
     *         ({@link HttpImplementationSelectionException.Reason#INVALID_IMPLEMENTATION}) as
     *         {@link InstalledHttpImplementations#of(java.util.Collection)} does
     */
    public static InstalledHttpImplementations discover(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader");
        List<HttpImplementation<?>> found = new ArrayList<>();
        try {
            for (SdkHttpService service : ServiceLoader.load(SdkHttpService.class, classLoader)) {
                found.add(service);
            }
            for (SdkAsyncHttpService service : ServiceLoader.load(SdkAsyncHttpService.class, classLoader)) {
                found.add(service);
            }
        } catch (ServiceConfigurationError e) {
            throw HttpImplementationSelectionException.builder()
                                                      .reason(HttpImplementationSelectionException.Reason
                                                                  .DISCOVERY_FAILED)
                                                      .message("Discovering the installed HTTP implementations "
                                                               + "failed: " + e.getMessage())
                                                      .cause(e)
                                                      .build();
        }
        return InstalledHttpImplementations.of(found);
    }
}
