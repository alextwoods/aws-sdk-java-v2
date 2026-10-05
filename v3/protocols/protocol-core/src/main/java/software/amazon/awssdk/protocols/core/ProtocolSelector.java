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

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Selects the single protocol of a client at construction (Requirements 5.6, 5.12; design Property 11).
 *
 * <p>Rules, in precedence order:
 * <ol>
 *   <li><b>Explicit provider.</b> A protocol runtime supplied by the caller is used if the service declares its
 *       protocol; installed runtimes are not consulted.</li>
 *   <li><b>Explicit protocol ID.</b> A protocol named by the caller must be declared by the service and installed.</li>
 *   <li><b>Model default.</b> Otherwise the candidates are the declared protocols that are installed; the one ranked
 *       first by {@link ProtocolIds#LAUNCH_PRECEDENCE} (then, for protocols outside it, by shape ID) is selected.</li>
 * </ol>
 * Any other outcome throws a {@link ProtocolSelectionException} naming the service, the requested protocol, the
 * declared and the installed protocols. The selected runtime's {@link ProtocolProvider#validate} runs last. The result
 * depends only on the three inputs, never on class-path order.
 */
public final class ProtocolSelector {
    private static final Comparator<ShapeId> PRECEDENCE = Comparator
        .comparingInt(ProtocolSelector::rank)
        .thenComparing(Comparator.naturalOrder());

    private ProtocolSelector() {
    }

    /**
     * The model default: the highest-precedence protocol that {@code service} declares and {@code installed}
     * contains.
     *
     * @throws ProtocolSelectionException if no declared protocol is installed, or the runtime rejects the service
     */
    public static SelectedProtocol select(ServiceProtocols service, InstalledProtocols installed) {
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(installed, "installed");
        Optional<ShapeId> best = service.protocolIds().stream().filter(installed::contains).min(PRECEDENCE);
        if (best.isEmpty()) {
            throw failure(ProtocolSelectionException.Reason.NO_INSTALLED_DECLARED_PROTOCOL, service, null, installed,
                          "No protocol declared by " + service.serviceId() + " " + service.protocolIds()
                          + " has an installed runtime; installed protocols are " + installed.protocolIds()
                          + ". Add the protocol module of one of the declared protocols to the class path.");
        }
        ShapeId id = best.get();
        return validated(new SelectedProtocol(service, service.protocol(id).orElseThrow(),
                                              installed.provider(id).orElseThrow(),
                                              SelectedProtocol.Source.MODEL_DEFAULT));
    }

    /**
     * The protocol {@code requested} by the caller, taken from {@code installed}.
     *
     * @throws ProtocolSelectionException if {@code service} does not declare it, it is not installed, or the runtime
     *         rejects the service
     */
    public static SelectedProtocol select(ServiceProtocols service, InstalledProtocols installed, ShapeId requested) {
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(installed, "installed");
        Objects.requireNonNull(requested, "requested");
        ModeledProtocol declaration = requireDeclared(service, requested, installed);
        ProtocolProvider provider = installed.provider(requested).orElseThrow(() -> failure(
            ProtocolSelectionException.Reason.NOT_INSTALLED, service, requested, installed,
            "The requested protocol " + requested + " is declared by " + service.serviceId()
            + " but has no installed runtime; installed protocols are " + installed.protocolIds()
            + ". Add its protocol module to the class path."));
        return validated(new SelectedProtocol(service, declaration, provider, SelectedProtocol.Source.EXPLICIT_ID));
    }

    /**
     * The protocol runtime {@code provider} supplied by the caller.
     *
     * @throws ProtocolSelectionException if {@code service} does not declare its protocol or it rejects the service
     */
    public static SelectedProtocol select(ServiceProtocols service, ProtocolProvider provider) {
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(provider, "provider");
        ShapeId requested = Objects.requireNonNull(provider.protocolId(), "provider.protocolId()");
        InstalledProtocols supplied = InstalledProtocols.of(provider);
        ModeledProtocol declaration = requireDeclared(service, requested, supplied);
        return validated(new SelectedProtocol(service, declaration, provider,
                                              SelectedProtocol.Source.EXPLICIT_PROVIDER));
    }

    private static ModeledProtocol requireDeclared(ServiceProtocols service, ShapeId requested,
                                                   InstalledProtocols installed) {
        return service.protocol(requested).orElseThrow(() -> failure(
            ProtocolSelectionException.Reason.NOT_DECLARED, service, requested, installed,
            "The requested protocol " + requested + " is not declared by the model of " + service.serviceId()
            + "; declared protocols are " + service.protocolIds() + "."));
    }

    private static SelectedProtocol validated(SelectedProtocol selected) {
        selected.provider().validate(selected.service(), selected.declaration());
        return selected;
    }

    private static ProtocolSelectionException failure(ProtocolSelectionException.Reason reason,
                                                       ServiceProtocols service, ShapeId requested,
                                                       InstalledProtocols installed, String message) {
        return ProtocolSelectionException.builder()
                                         .reason(reason)
                                         .serviceId(service.serviceId())
                                         .requested(requested)
                                         .declared(service.protocolIds())
                                         .installed(installed.protocolIds())
                                         .message(message)
                                         .build();
    }

    private static int rank(ShapeId id) {
        int index = ProtocolIds.LAUNCH_PRECEDENCE.indexOf(id);
        return index < 0 ? ProtocolIds.LAUNCH_PRECEDENCE.size() : index;
    }
}
