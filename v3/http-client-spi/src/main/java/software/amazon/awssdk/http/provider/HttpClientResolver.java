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
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.core.exception.SdkCloseException;
import software.amazon.awssdk.core.resource.ManagedResource;
import software.amazon.awssdk.core.resource.ResourceOwnership;
import software.amazon.awssdk.core.resource.ResourceScope;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpClientOption;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkSyncHttpClient;

/**
 * Resolves the HTTP client of one SDK client construction (Requirements 12.11-12.14), in this order:
 * <ol>
 *   <li>A {@link #client(AutoCloseable) client} configured explicitly wins. It is caller-owned: never closed by the
 *       SDK, including when resolution fails. Configuring both a client and an implementation fails.</li>
 *   <li>Otherwise an {@link #implementation(HttpImplementation) implementation} configured explicitly (the V2
 *       {@code httpClientBuilder} role) builds the client.</li>
 *   <li>Otherwise the {@link #installed(Supplier) installed} implementations are consulted, only now, and the one of
 *       the requested mode with the highest priority builds the client. A tie at the highest priority fails, naming
 *       every tied implementation; no implementation of the mode fails with the installed implementations of the
 *       other mode.</li>
 * </ol>
 * Every configured option the chosen implementation's declared capabilities do not support fails before
 * {@link HttpImplementation#create} runs, so nothing is acquired; the diagnostic names every unsupported option by its
 * V2 configuration name, the implementation and the mode. The built client's own capabilities are checked the same
 * way after {@code create}.
 *
 * <p>If {@code create} fails, or the built client fails that check, every SDK-owned resource registered with the
 * {@link HttpClientConstruction} (and the built client itself) is closed once, in reverse acquisition order,
 * caller-owned resources are left alone, and the original failure is thrown with any {@link SdkCloseException}
 * attached as suppressed.
 *
 * <p>Instances are single-use builders and are not thread-safe.
 *
 * @param <C> the client type
 * @param <S> the implementation type
 */
public final class HttpClientResolver<C extends AutoCloseable, S extends HttpImplementation<C>> {
    private final HttpTransportMode mode;
    private final Function<InstalledHttpImplementations, S> defaultSelection;
    private final Function<C, HttpClientCapabilities> capabilitiesOf;
    private final Function<C, String> nameOf;
    private C client;
    private S implementation;
    private HttpClientConfiguration configuration = HttpClientConfiguration.empty();
    private Supplier<InstalledHttpImplementations> installed =
        () -> HttpImplementationDiscovery.discover(HttpClientResolver.class.getClassLoader());

    private HttpClientResolver(HttpTransportMode mode, Function<InstalledHttpImplementations, S> defaultSelection,
                               Function<C, HttpClientCapabilities> capabilitiesOf, Function<C, String> nameOf) {
        this.mode = mode;
        this.defaultSelection = defaultSelection;
        this.capabilitiesOf = capabilitiesOf;
        this.nameOf = nameOf;
    }

    /** Resolves a sync client. */
    public static HttpClientResolver<SdkSyncHttpClient, SdkHttpService> sync() {
        return new HttpClientResolver<>(HttpTransportMode.SYNC, InstalledHttpImplementations::defaultSync,
                                        SdkSyncHttpClient::capabilities, SdkSyncHttpClient::clientName);
    }

    /** Resolves an async client. */
    public static HttpClientResolver<SdkAsyncHttpClient, SdkAsyncHttpService> async() {
        return new HttpClientResolver<>(HttpTransportMode.ASYNC, InstalledHttpImplementations::defaultAsync,
                                        SdkAsyncHttpClient::capabilities, SdkAsyncHttpClient::clientName);
    }

    /** A caller-owned client to use as is; {@code null} to clear. */
    public HttpClientResolver<C, S> client(C client) {
        this.client = client;
        return this;
    }

    /** The implementation to build the client with instead of the default; {@code null} to clear. */
    public HttpClientResolver<C, S> implementation(S implementation) {
        this.implementation = implementation;
        return this;
    }

    /** The configured HTTP options; empty by default. */
    public HttpClientResolver<C, S> configuration(HttpClientConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        return this;
    }

    /**
     * The source of the installed implementations, called at most once and only for default selection; by default
     * {@link HttpImplementationDiscovery#discover} with the SPI's class loader.
     */
    public HttpClientResolver<C, S> installed(Supplier<InstalledHttpImplementations> installed) {
        this.installed = Objects.requireNonNull(installed, "installed");
        return this;
    }

    /**
     * The client, per the class description.
     *
     * @throws HttpImplementationSelectionException if no client can be selected, or an option is unsupported
     * @throws RuntimeException the failure of {@link HttpImplementation#create}, after rollback
     */
    public ResolvedHttpClient<C> resolve() {
        if (client != null && implementation != null) {
            throw HttpImplementationSelectionException
                .builder()
                .reason(HttpImplementationSelectionException.Reason.EXPLICIT_CONFLICT)
                .mode(mode)
                .implementations(List.of(nameOf.apply(client), implementation.implementationName()))
                .option(clientOptionName(mode))
                .message("Both " + clientOptionName(mode) + " and " + builderOptionName(mode) + " are configured; "
                         + "configure at most one.")
                .build();
        }
        if (client != null) {
            return resolveExplicitClient(client);
        }
        S chosen = implementation != null ? implementation
                                          : defaultSelection.apply(Objects.requireNonNull(installed.get(),
                                                                                          "installed"));
        return construct(InstalledHttpImplementations.describe(chosen, mode));
    }

    private ResolvedHttpClient<C> resolveExplicitClient(C explicit) {
        String name = nameOf.apply(explicit);
        checkClient(explicit, name);
        return new ResolvedHttpClient<>(explicit, name, ResourceOwnership.CALLER_OWNED,
                                        ResourceScope.of(List.of(ManagedResource.callerOwned(explicit))));
    }

    private ResolvedHttpClient<C> construct(InstalledHttpImplementations.Installed<S> chosen) {
        checkOptions(chosen.name(), chosen.capabilities(), "implementation");
        HttpClientConstruction construction = new HttpClientConstruction(mode, chosen.name(), configuration);
        C built = null;
        try {
            built = chosen.implementation().create(construction);
            if (built == null) {
                throw InstalledHttpImplementations.invalid(mode, List.of(chosen.name()),
                                                           "HTTP implementation " + chosen.describe()
                                                           + " created no client.");
            }
            List<ManagedResource<?>> resources = new ArrayList<>(construction.seal());
            resources.add(ManagedResource.sdkOwned(built));
            checkClient(built, chosen.name());
            return new ResolvedHttpClient<>(built, chosen.name(), ResourceOwnership.SDK_OWNED,
                                            ResourceScope.of(resources));
        } catch (RuntimeException | Error failure) {
            rollBack(construction, built, failure);
            throw failure;
        }
    }

    /** Releases what a failed construction acquired: the built client first, then registrations in reverse. */
    private static void rollBack(HttpClientConstruction construction, AutoCloseable built, Throwable failure) {
        List<ManagedResource<?>> acquired = new ArrayList<>(construction.seal());
        if (built != null) {
            acquired.add(ManagedResource.sdkOwned(built));
        }
        try {
            ResourceScope.of(acquired).close();
        } catch (SdkCloseException e) {
            failure.addSuppressed(e);
        }
    }

    private void checkClient(C built, String name) {
        HttpClientCapabilities capabilities = capabilitiesOf.apply(built);
        if (capabilities == null || capabilities.mode() != mode) {
            throw InstalledHttpImplementations.invalid(
                mode, List.of(name), "HTTP client " + name + " (" + built.getClass().getName() + ") is used as "
                                     + InstalledHttpImplementations.label(mode) + " but declares "
                                     + (capabilities == null ? "no capabilities." : capabilities.mode() + " mode."));
        }
        checkOptions(name, capabilities, "client");
    }

    private void checkOptions(String name, HttpClientCapabilities capabilities, String what) {
        List<HttpClientOption> unsupported = capabilities.unsupported(configuration.configuredOptions());
        if (unsupported.isEmpty()) {
            return;
        }
        List<String> names = unsupported.stream().map(HttpClientOption::configurationName).toList();
        throw HttpImplementationSelectionException
            .builder()
            .reason(HttpImplementationSelectionException.Reason.UNSUPPORTED_OPTION)
            .mode(mode)
            .implementations(List.of(name))
            .unsupportedOptions(unsupported)
            .option(names.size() == 1 ? names.get(0) : null)
            .message("The " + InstalledHttpImplementations.label(mode) + " HTTP " + what + " " + name
                     + " does not support the configured option" + (names.size() == 1 ? " " : "s ")
                     + String.join(", ", names) + ". Remove " + (names.size() == 1 ? "it" : "them")
                     + " or configure an HTTP client that supports " + (names.size() == 1 ? "it." : "them."))
            .build();
    }

    /** The V2 builder property of an explicit client of {@code mode}. */
    static String clientOptionName(HttpTransportMode mode) {
        return mode == HttpTransportMode.SYNC ? "httpClient" : "asyncHttpClient";
    }

    /** The V2 builder property of an explicit implementation (builder) of {@code mode}. */
    static String builderOptionName(HttpTransportMode mode) {
        return mode == HttpTransportMode.SYNC ? "httpClientBuilder" : "asyncHttpClientBuilder";
    }
}
