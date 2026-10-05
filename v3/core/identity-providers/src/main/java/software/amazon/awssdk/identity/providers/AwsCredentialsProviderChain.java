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

package software.amazon.awssdk.identity.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkCloseException;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.RefreshableIdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;

/**
 * Asks each credentials provider in order and returns the first credentials resolved, with the V2 name and behavior
 * (Requirement 13.3):
 *
 * <ul>
 *   <li>Providers are consulted strictly in the configured order; a provider is consulted only after every earlier one
 *       failed.</li>
 *   <li>With {@code reuseLastProviderEnabled} (the default) the provider that last succeeded is asked first on later
 *       resolutions, so a refresh stays with the same source.</li>
 *   <li>If every provider fails, the error lists each provider and its failure message, in order.</li>
 * </ul>
 *
 * <p>{@link #invalidate} forwards a rejected identity to the provider that returned it; {@link #close} closes every
 * closeable provider once and aggregates failures.
 */
public final class AwsCredentialsProviderChain implements RefreshableIdentityProvider<AwsCredentialsIdentity>,
                                                          AutoCloseable {
    private final List<IdentityProvider<? extends AwsCredentialsIdentity>> providers;
    private final boolean reuseLastProvider;
    private volatile IdentityProvider<? extends AwsCredentialsIdentity> lastUsed;

    private AwsCredentialsProviderChain(Builder builder) {
        this.providers = List.copyOf(builder.providers);
        this.reuseLastProvider = builder.reuseLastProviderEnabled;
        if (providers.isEmpty()) {
            throw new IllegalArgumentException("No credential providers were specified.");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /** A chain of {@code providers}, in order, reusing the last successful provider. */
    @SafeVarargs
    public static AwsCredentialsProviderChain of(IdentityProvider<? extends AwsCredentialsIdentity>... providers) {
        Builder builder = builder();
        for (IdentityProvider<? extends AwsCredentialsIdentity> provider : providers) {
            builder.addCredentialsProvider(provider);
        }
        return builder.build();
    }

    /** The providers, in the order they are consulted. */
    public List<IdentityProvider<? extends AwsCredentialsIdentity>> providers() {
        return providers;
    }

    @Override
    public Class<AwsCredentialsIdentity> identityType() {
        return AwsCredentialsIdentity.class;
    }

    @Override
    public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
        IdentityProvider<? extends AwsCredentialsIdentity> last = lastUsed;
        if (reuseLastProvider && last != null) {
            return last.resolveIdentity(request).thenApply(c -> (AwsCredentialsIdentity) c);
        }
        return attempt(request, 0, new ArrayList<>());
    }

    private CompletableFuture<AwsCredentialsIdentity> attempt(ResolveIdentityRequest request, int index,
                                                              List<String> failures) {
        if (index == providers.size()) {
            return CompletableFuture.failedFuture(SdkClientException.create(
                "Unable to load credentials from any of the providers in the chain " + this + ": " + failures));
        }
        IdentityProvider<? extends AwsCredentialsIdentity> provider = providers.get(index);
        CompletableFuture<? extends AwsCredentialsIdentity> future;
        try {
            future = provider.resolveIdentity(request);
        } catch (RuntimeException e) {
            future = CompletableFuture.failedFuture(e);
        }
        return future.handle((credentials, failure) -> {
            if (failure == null) {
                lastUsed = provider;
                return CompletableFuture.completedFuture((AwsCredentialsIdentity) credentials);
            }
            Throwable cause = failure instanceof CompletionException && failure.getCause() != null ? failure.getCause()
                                                                                                   : failure;
            failures.add(provider + ": " + cause.getMessage());
            return attempt(request, index + 1, failures);
        }).thenCompose(f -> f);
    }

    @Override
    public void invalidate(Identity identity) {
        for (IdentityProvider<? extends AwsCredentialsIdentity> provider : providers) {
            if (provider instanceof RefreshableIdentityProvider<?> refreshable) {
                refreshable.invalidate(identity);
            }
        }
    }

    @Override
    public void close() {
        List<Throwable> failures = new ArrayList<>();
        for (IdentityProvider<?> provider : providers) {
            if (provider instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception e) {
                    failures.add(e);
                }
            }
        }
        if (!failures.isEmpty()) {
            throw SdkCloseException.create(failures);
        }
    }

    @Override
    public String toString() {
        List<String> names = new ArrayList<>();
        providers.forEach(p -> names.add(p.toString()));
        return "AwsCredentialsProviderChain(credentialsProviders=" + names + ")";
    }

    /** Builder of an {@link AwsCredentialsProviderChain}. */
    public static final class Builder {
        private final List<IdentityProvider<? extends AwsCredentialsIdentity>> providers = new ArrayList<>();
        private boolean reuseLastProviderEnabled = true;

        private Builder() {
        }

        /** Whether the provider that last succeeded is asked first on later resolutions; default true. */
        public Builder reuseLastProviderEnabled(boolean enabled) {
            this.reuseLastProviderEnabled = enabled;
            return this;
        }

        /** Appends {@code provider}. */
        public Builder addCredentialsProvider(IdentityProvider<? extends AwsCredentialsIdentity> provider) {
            providers.add(Objects.requireNonNull(provider, "provider"));
            return this;
        }

        public AwsCredentialsProviderChain build() {
            return new AwsCredentialsProviderChain(this);
        }
    }
}
