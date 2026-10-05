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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpTransportMode;

/**
 * The HTTP implementations available to a client (Requirements 12.11, 12.12), and default selection among them.
 *
 * <p>Each implementation's metadata is read once, here, and validated: a non-blank name unique within its mode,
 * capabilities of the implementation's own mode. The implementations of each mode are held sorted by name, so
 * neither the installed set nor {@link #defaultSync()}/{@link #defaultAsync()} depends on the order implementations
 * were found in.
 *
 * <p>Default selection picks the implementation with the highest {@link HttpImplementation#priority() priority} for
 * the mode. Compatibility is by mode only: configured options are then checked against the selected implementation,
 * and an unsupported one fails construction rather than silently selecting a lower-priority implementation.
 *
 * <p>Instances are immutable.
 */
public final class InstalledHttpImplementations {
    private static final InstalledHttpImplementations NONE = new InstalledHttpImplementations(List.of(), List.of());

    private final List<Installed<SdkHttpService>> sync;
    private final List<Installed<SdkAsyncHttpService>> async;

    private InstalledHttpImplementations(List<Installed<SdkHttpService>> sync,
                                         List<Installed<SdkAsyncHttpService>> async) {
        this.sync = sync;
        this.async = async;
    }

    /** No installed implementation. */
    public static InstalledHttpImplementations none() {
        return NONE;
    }

    /** Exactly {@code implementations}; see {@link #of(Collection)}. */
    public static InstalledHttpImplementations of(HttpImplementation<?>... implementations) {
        return of(List.of(implementations));
    }

    /**
     * Exactly {@code implementations}, in any order.
     *
     * @throws HttpImplementationSelectionException ({@link HttpImplementationSelectionException.Reason#INVALID_IMPLEMENTATION})
     *         if an implementation's metadata is invalid, it is neither an {@link SdkHttpService} nor an
     *         {@link SdkAsyncHttpService}, or two implementations of one mode share a name
     */
    public static InstalledHttpImplementations of(Collection<? extends HttpImplementation<?>> implementations) {
        Map<String, List<Installed<SdkHttpService>>> sync = new TreeMap<>();
        Map<String, List<Installed<SdkAsyncHttpService>>> async = new TreeMap<>();
        for (HttpImplementation<?> implementation : Objects.requireNonNull(implementations, "implementations")) {
            Objects.requireNonNull(implementation, "implementation");
            boolean added = false;
            if (implementation instanceof SdkHttpService service) {
                Installed<SdkHttpService> installed = describe(service, HttpTransportMode.SYNC);
                sync.computeIfAbsent(installed.name(), k -> new ArrayList<>()).add(installed);
                added = true;
            }
            if (implementation instanceof SdkAsyncHttpService service) {
                Installed<SdkAsyncHttpService> installed = describe(service, HttpTransportMode.ASYNC);
                async.computeIfAbsent(installed.name(), k -> new ArrayList<>()).add(installed);
                added = true;
            }
            if (!added) {
                throw invalid(null, List.of(implementation.getClass().getName()),
                              implementation.getClass().getName() + " implements neither SdkHttpService nor "
                              + "SdkAsyncHttpService.");
            }
        }
        return new InstalledHttpImplementations(unique(sync, HttpTransportMode.SYNC),
                                                unique(async, HttpTransportMode.ASYNC));
    }

    /** The installed sync implementations, sorted by name. */
    public List<SdkHttpService> sync() {
        return sync.stream().map(Installed::implementation).toList();
    }

    /** The installed async implementations, sorted by name. */
    public List<SdkAsyncHttpService> async() {
        return async.stream().map(Installed::implementation).toList();
    }

    /** The names of the installed implementations of {@code mode}, sorted. */
    public List<String> names(HttpTransportMode mode) {
        return (mode == HttpTransportMode.SYNC ? sync : async).stream().map(Installed::name).toList();
    }

    /**
     * The installed sync implementation with the highest priority.
     *
     * @throws HttpImplementationSelectionException ({@link HttpImplementationSelectionException.Reason#NO_IMPLEMENTATION})
     *         if none is installed, or ({@link HttpImplementationSelectionException.Reason#TIED_PRIORITY}) if two or
     *         more share the highest priority, naming every one of them
     */
    public SdkHttpService defaultSync() {
        return select(sync, HttpTransportMode.SYNC);
    }

    /** The installed async implementation with the highest priority; fails as {@link #defaultSync()} does. */
    public SdkAsyncHttpService defaultAsync() {
        return select(async, HttpTransportMode.ASYNC);
    }

    @Override
    public String toString() {
        return "InstalledHttpImplementations[sync=" + names(HttpTransportMode.SYNC) + ", async="
               + names(HttpTransportMode.ASYNC) + "]";
    }

    private <S extends HttpImplementation<?>> S select(List<Installed<S>> installed, HttpTransportMode mode) {
        if (installed.isEmpty()) {
            HttpTransportMode other = mode == HttpTransportMode.SYNC ? HttpTransportMode.ASYNC : HttpTransportMode.SYNC;
            throw HttpImplementationSelectionException
                .builder()
                .reason(HttpImplementationSelectionException.Reason.NO_IMPLEMENTATION)
                .mode(mode)
                .implementations(names(other))
                .option(HttpClientResolver.clientOptionName(mode))
                .message("No " + label(mode) + " HTTP implementation is installed (installed " + label(other)
                         + " implementations: " + names(other) + "). Add a " + label(mode) + " HTTP client module to "
                         + "the class path, or configure " + HttpClientResolver.clientOptionName(mode) + " or "
                         + HttpClientResolver.builderOptionName(mode) + " explicitly.")
                .build();
        }
        int highest = Integer.MIN_VALUE;
        for (Installed<S> candidate : installed) {
            highest = Math.max(highest, candidate.priority());
        }
        List<Installed<S>> top = new ArrayList<>();
        for (Installed<S> candidate : installed) {
            if (candidate.priority() == highest) {
                top.add(candidate);
            }
        }
        if (top.size() > 1) {
            List<String> tied = top.stream().map(Installed::name).toList();
            List<String> described = top.stream().map(Installed::describe).toList();
            throw HttpImplementationSelectionException
                .builder()
                .reason(HttpImplementationSelectionException.Reason.TIED_PRIORITY)
                .mode(mode)
                .implementations(tied)
                .option(HttpClientResolver.clientOptionName(mode))
                .message(top.size() + " installed " + label(mode) + " HTTP implementations share the highest "
                         + "priority " + highest + ": " + String.join(", ", described) + ". Configure "
                         + HttpClientResolver.clientOptionName(mode) + " or "
                         + HttpClientResolver.builderOptionName(mode) + " explicitly, or remove all but one of them "
                         + "from the class path.")
                .build();
        }
        return top.get(0).implementation();
    }

    /** Reads and validates the metadata of {@code implementation}, which provides {@code mode}. */
    static <S extends HttpImplementation<?>> Installed<S> describe(S implementation, HttpTransportMode mode) {
        String type = implementation.getClass().getName();
        String name = implementation.implementationName();
        if (name == null || name.isBlank()) {
            throw invalid(mode, List.of(type), type + " has no implementationName.");
        }
        HttpClientCapabilities capabilities = implementation.capabilities();
        if (capabilities == null) {
            throw invalid(mode, List.of(name), "HTTP implementation " + name + " (" + type + ") declares no "
                                               + "capabilities.");
        }
        if (implementation.mode() != mode || capabilities.mode() != mode) {
            throw invalid(mode, List.of(name), "HTTP implementation " + name + " (" + type + ") is installed as "
                                               + label(mode) + " but declares mode " + implementation.mode()
                                               + " with " + capabilities.mode() + " capabilities.");
        }
        return new Installed<>(implementation, name, implementation.priority(), capabilities);
    }

    private static <S extends HttpImplementation<?>> List<Installed<S>> unique(Map<String, List<Installed<S>>> byName,
                                                                              HttpTransportMode mode) {
        List<Installed<S>> out = new ArrayList<>(byName.size());
        for (Map.Entry<String, List<Installed<S>>> entry : byName.entrySet()) {
            if (entry.getValue().size() > 1) {
                List<String> classes = entry.getValue().stream()
                                            .map(i -> i.implementation().getClass().getName()).sorted().toList();
                throw invalid(mode, List.of(entry.getKey()),
                              entry.getValue().size() + " installed " + label(mode) + " HTTP implementations are "
                              + "named " + entry.getKey() + ": " + String.join(", ", classes)
                              + ". Remove the duplicate module from the class path.");
            }
            out.add(entry.getValue().get(0));
        }
        return List.copyOf(out);
    }

    static String label(HttpTransportMode mode) {
        return mode == HttpTransportMode.SYNC ? "sync" : "async";
    }

    static HttpImplementationSelectionException invalid(HttpTransportMode mode, List<String> names, String message) {
        return HttpImplementationSelectionException.builder()
                                                   .reason(HttpImplementationSelectionException.Reason
                                                               .INVALID_IMPLEMENTATION)
                                                   .mode(mode)
                                                   .implementations(names)
                                                   .message(message)
                                                   .build();
    }

    /** An implementation with its metadata, read once. */
    record Installed<S extends HttpImplementation<?>>(S implementation, String name, int priority,
                                                       HttpClientCapabilities capabilities) {
        String describe() {
            return name + " (" + implementation.getClass().getName() + ")";
        }
    }
}
