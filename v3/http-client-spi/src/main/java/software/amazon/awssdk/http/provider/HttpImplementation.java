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

import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpTransportMode;

/**
 * An installable HTTP implementation: service-provider metadata plus a factory of clients (Requirements 12.11-12.14).
 * Implement {@link SdkHttpService} (sync) or {@link SdkAsyncHttpService} (async), never this directly.
 *
 * <p>The metadata ({@link #implementationName()}, {@link #priority()}, {@link #capabilities()}) is read once, when
 * the implementation is installed, and must be constant and resource-free: instantiating an implementation or reading
 * its metadata acquires nothing. Resources are acquired only in {@link #create}, and registered with its
 * {@link HttpClientConstruction} so a failed construction releases them.
 *
 * <p>Default selection picks, among the installed implementations of the requested mode, the one with the highest
 * {@link #priority()}; a tie at the highest priority fails construction and names every tied implementation. The
 * result never depends on class-path order.
 *
 * @param <C> the client type
 */
public interface HttpImplementation<C extends AutoCloseable> {
    /**
     * A stable, unique name, for example {@code apache-client}; used in diagnostics and to order the installed
     * implementations.
     */
    String implementationName();

    /** Default-selection priority; higher wins. Documented per implementation module. */
    int priority();

    /** What every client this implementation creates supports; its mode must be {@link #mode()}. */
    HttpClientCapabilities capabilities();

    /** The mode of the SPI this implementation provides; fixed by {@link SdkHttpService}/{@link SdkAsyncHttpService}. */
    HttpTransportMode mode();

    /**
     * Creates a client configured by {@code construction.configuration()}, which holds only options the
     * {@link #capabilities()} support. Every acquired resource is registered with {@code construction} before the next
     * one is acquired. Throws {@link HttpImplementationSelectionException} or another
     * {@link software.amazon.awssdk.core.exception.SdkClientConfigurationException} for a configuration it cannot
     * honour.
     */
    C create(HttpClientConstruction construction);
}
