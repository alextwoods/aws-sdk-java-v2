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

import java.util.Objects;
import software.amazon.awssdk.core.resource.ResourceOwnership;
import software.amazon.awssdk.core.resource.ResourceScope;

/**
 * The HTTP client an SDK client uses, with who owns it and the resources closing it releases.
 *
 * <p>{@link #resources()} holds the client last, so {@link #close()} closes an SDK-owned client first and then the
 * SDK-owned resources its construction registered, in reverse acquisition order. A caller-supplied client is
 * caller-owned and never closed. Closing is idempotent; failures are aggregated as
 * {@link ResourceScope#close()} describes.
 *
 * @param <C> the client type
 */
public final class ResolvedHttpClient<C extends AutoCloseable> implements AutoCloseable {
    private final C client;
    private final String implementationName;
    private final ResourceOwnership ownership;
    private final ResourceScope resources;

    ResolvedHttpClient(C client, String implementationName, ResourceOwnership ownership, ResourceScope resources) {
        this.client = Objects.requireNonNull(client, "client");
        this.implementationName = Objects.requireNonNull(implementationName, "implementationName");
        this.ownership = Objects.requireNonNull(ownership, "ownership");
        this.resources = Objects.requireNonNull(resources, "resources");
    }

    /** The client. */
    public C client() {
        return client;
    }

    /** The implementation that built the client, or the client's own name if the caller supplied it. */
    public String implementationName() {
        return implementationName;
    }

    /** {@link ResourceOwnership#CALLER_OWNED} for a caller-supplied client, else {@link ResourceOwnership#SDK_OWNED}. */
    public ResourceOwnership ownership() {
        return ownership;
    }

    /** The client and the resources of its construction, with their ownership. */
    public ResourceScope resources() {
        return resources;
    }

    /** Closes {@link #resources()}. */
    @Override
    public void close() {
        resources.close();
    }

    @Override
    public String toString() {
        return "ResolvedHttpClient[" + implementationName + ", " + ownership + "]";
    }
}
