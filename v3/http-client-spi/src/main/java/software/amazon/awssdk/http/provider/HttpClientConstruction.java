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
import software.amazon.awssdk.core.resource.ManagedResource;
import software.amazon.awssdk.http.HttpTransportMode;

/**
 * The context of one {@link HttpImplementation#create} call (Requirement 12.14).
 *
 * <p>An implementation registers every resource it acquires while constructing a client (thread pools, event loops,
 * connection managers, native handles) here, in acquisition order, as soon as it acquires it: {@link #sdkOwned} for
 * resources the SDK created and must release, {@link #callerOwned} for resources the caller supplied through the
 * configuration. If construction then fails, for an implementation's own failure or because the built client does not
 * support a configured option, the resolver releases every SDK-owned registration once, in reverse acquisition order,
 * and never touches caller-owned ones. If construction succeeds, the registrations are released the same way after
 * the client when the resolved client is closed, so the client itself need not release them.
 *
 * <p>Registration is thread-safe and allowed only until {@code create} returns.
 */
public final class HttpClientConstruction {
    private final HttpTransportMode mode;
    private final String implementationName;
    private final HttpClientConfiguration configuration;
    private final List<ManagedResource<?>> acquired = new ArrayList<>();
    private boolean sealed;

    HttpClientConstruction(HttpTransportMode mode, String implementationName, HttpClientConfiguration configuration) {
        this.mode = mode;
        this.implementationName = implementationName;
        this.configuration = configuration;
    }

    /** The mode of the client being constructed. */
    public HttpTransportMode mode() {
        return mode;
    }

    /** The name of the implementation constructing the client. */
    public String implementationName() {
        return implementationName;
    }

    /** The configured options; every one is supported by the implementation's declared capabilities. */
    public HttpClientConfiguration configuration() {
        return configuration;
    }

    /** Registers a resource the SDK acquired and must release; returns {@code resource}. */
    public <T extends AutoCloseable> T sdkOwned(T resource) {
        register(ManagedResource.sdkOwned(Objects.requireNonNull(resource, "resource")));
        return resource;
    }

    /** Registers a resource the caller supplied, which the SDK never releases; returns {@code resource}. */
    public <T> T callerOwned(T resource) {
        register(ManagedResource.callerOwned(Objects.requireNonNull(resource, "resource")));
        return resource;
    }

    /**
     * Registers {@code resource} with its ownership.
     *
     * @throws IllegalStateException if {@link HttpImplementation#create} has returned or failed
     */
    public synchronized void register(ManagedResource<?> resource) {
        Objects.requireNonNull(resource, "resource");
        if (sealed) {
            throw new IllegalStateException("Resources of " + implementationName + " must be registered before "
                                            + "create returns");
        }
        acquired.add(resource);
    }

    /** Ends registration and returns the registrations, in acquisition order. */
    synchronized List<ManagedResource<?>> seal() {
        sealed = true;
        return List.copyOf(acquired);
    }

    @Override
    public String toString() {
        return "HttpClientConstruction[" + implementationName + ", " + mode + ", " + configuration + "]";
    }
}
