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

package software.amazon.awssdk.protocols.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.TreeMap;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The protocol runtimes available to a client, at most one provider per protocol trait shape ID.
 *
 * <p>{@link #discover(ClassLoader)} finds the providers of the protocol modules on the class path. It is the only
 * service discovery in the protocol runtime and runs once per client construction, never while invoking an
 * operation (Requirements 10.12, 20.22). Discovery is deterministic: the result does not depend on class-path order,
 * and two providers of the same protocol fail construction and name both classes instead of one silently winning.
 *
 * <p>Instances are immutable.
 */
public final class InstalledProtocols {
    private static final InstalledProtocols NONE = new InstalledProtocols(new TreeMap<>());

    private final Map<ShapeId, ProtocolProvider> providers;
    private final List<ShapeId> ids;

    private InstalledProtocols(TreeMap<ShapeId, ProtocolProvider> providers) {
        this.providers = Collections.unmodifiableMap(providers);
        this.ids = List.copyOf(providers.keySet());
    }

    /** No installed protocol. */
    public static InstalledProtocols none() {
        return NONE;
    }

    /**
     * Exactly {@code providers}.
     *
     * @throws ProtocolSelectionException ({@link ProtocolSelectionException.Reason#DUPLICATE_PROVIDER}) if two
     *         providers implement the same protocol
     */
    public static InstalledProtocols of(ProtocolProvider... providers) {
        return of(List.of(providers));
    }

    /**
     * Exactly {@code providers}.
     *
     * @throws ProtocolSelectionException ({@link ProtocolSelectionException.Reason#DUPLICATE_PROVIDER}) if two
     *         providers implement the same protocol
     */
    public static InstalledProtocols of(Collection<? extends ProtocolProvider> providers) {
        Map<ShapeId, List<ProtocolProvider>> byId = new TreeMap<>();
        for (ProtocolProvider provider : providers) {
            Objects.requireNonNull(provider, "provider");
            ShapeId id = Objects.requireNonNull(provider.protocolId(), () -> provider.getClass().getName()
                                                                          + ".protocolId()");
            byId.computeIfAbsent(id, k -> new ArrayList<>()).add(provider);
        }
        TreeMap<ShapeId, ProtocolProvider> unique = new TreeMap<>();
        for (Map.Entry<ShapeId, List<ProtocolProvider>> entry : byId.entrySet()) {
            List<ProtocolProvider> candidates = entry.getValue();
            if (candidates.size() > 1) {
                List<String> classes = candidates.stream().map(p -> p.getClass().getName()).sorted().toList();
                throw ProtocolSelectionException.builder()
                                                .reason(ProtocolSelectionException.Reason.DUPLICATE_PROVIDER)
                                                .installed(List.copyOf(byId.keySet()))
                                                .providers(classes)
                                                .message("More than one installed runtime implements protocol "
                                                         + entry.getKey() + ": " + String.join(", ", classes)
                                                         + ". Remove all but one protocol module from the class "
                                                         + "path, or pass the protocol explicitly.")
                                                .build();
            }
            unique.put(entry.getKey(), candidates.get(0));
        }
        return new InstalledProtocols(unique);
    }

    /**
     * The providers registered as {@code META-INF/services/software.amazon.awssdk.protocols.core.ProtocolProvider}
     * entries visible to {@code classLoader}. Called at client construction only.
     *
     * @throws ProtocolSelectionException ({@link ProtocolSelectionException.Reason#DISCOVERY_FAILED}) if a
     *         registered provider cannot be loaded or instantiated, or
     *         ({@link ProtocolSelectionException.Reason#DUPLICATE_PROVIDER}) if two implement the same protocol
     */
    public static InstalledProtocols discover(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader");
        List<ProtocolProvider> found = new ArrayList<>();
        try {
            for (ProtocolProvider provider : ServiceLoader.load(ProtocolProvider.class, classLoader)) {
                found.add(provider);
            }
        } catch (ServiceConfigurationError e) {
            throw ProtocolSelectionException.builder()
                                            .reason(ProtocolSelectionException.Reason.DISCOVERY_FAILED)
                                            .message("Discovering the installed protocol runtimes failed: "
                                                     + e.getMessage())
                                            .cause(e)
                                            .build();
        }
        return of(found);
    }

    /** The provider of the protocol with trait shape ID {@code protocolId}, if installed. */
    public Optional<ProtocolProvider> provider(ShapeId protocolId) {
        return Optional.ofNullable(providers.get(protocolId));
    }

    /** True if a runtime for {@code protocolId} is installed. */
    public boolean contains(ShapeId protocolId) {
        return providers.containsKey(protocolId);
    }

    /** The installed protocol trait shape IDs, sorted. */
    public List<ShapeId> protocolIds() {
        return ids;
    }

    @Override
    public String toString() {
        return "InstalledProtocols" + ids;
    }
}
