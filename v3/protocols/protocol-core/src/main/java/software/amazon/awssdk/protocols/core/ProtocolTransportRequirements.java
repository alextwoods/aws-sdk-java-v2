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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * What a selected protocol requires of the transport, for the construction-time protocol/transport compatibility
 * check (design Property 34): the message kind it exchanges and the HTTP versions, as ALPN protocol IDs in preference
 * order, it can use for ordinary and for event-stream operations.
 *
 * <p>The HTTP SPI's client capabilities are supplied to {@link #incompatibilities} by the pipeline when it builds a
 * client; this type does not depend on the SPI.
 *
 * @param protocolId absolute shape ID of the protocol trait
 * @param message the message kind the protocol exchanges
 * @param httpVersions ALPN IDs usable for non-event-stream operations, in preference order; never empty
 * @param eventStreamHttpVersions ALPN IDs usable for event-stream operations, in preference order; never empty
 */
public record ProtocolTransportRequirements(ShapeId protocolId, Message message, List<String> httpVersions,
                                            List<String> eventStreamHttpVersions) {

    /** HTTP/1.1, the version a protocol uses when its trait lists none. */
    public static final String HTTP_1_1 = "http/1.1";
    /** HTTP/2. */
    public static final String HTTP_2 = "h2";

    /** The kind of message a protocol exchanges with the transport. */
    public enum Message {
        /** HTTP request/response messages. Every launch protocol is HTTP-bound. */
        HTTP
    }

    public ProtocolTransportRequirements {
        Objects.requireNonNull(protocolId, "protocolId");
        Objects.requireNonNull(message, "message");
        httpVersions = List.copyOf(httpVersions);
        eventStreamHttpVersions = List.copyOf(eventStreamHttpVersions);
        if (httpVersions.isEmpty() || eventStreamHttpVersions.isEmpty()) {
            throw new IllegalArgumentException("The HTTP versions of " + protocolId + " must not be empty");
        }
    }

    /**
     * The HTTP requirements of a declared protocol: its modeled {@code http} list (HTTP/1.1 if empty) and its
     * {@code eventStreamHttp} list (its {@code http} list if empty, as the AWS protocol traits specify).
     */
    public static ProtocolTransportRequirements http(ModeledProtocol protocol) {
        List<String> http = protocol.http().isEmpty() ? List.of(HTTP_1_1) : protocol.http();
        List<String> eventStream = protocol.eventStreamHttp().isEmpty() ? http : protocol.eventStreamHttp();
        return new ProtocolTransportRequirements(protocol.id(), Message.HTTP, http, eventStream);
    }

    /**
     * Every reason the transport {@code transportId} cannot carry this protocol, empty if it can. Each reason names
     * both the protocol and the transport.
     *
     * @param transportId identifier of the transport implementation, for diagnostics
     * @param transportMessage the message kind the transport exchanges
     * @param supportedHttpVersions ALPN IDs the transport supports
     * @param eventStreams whether the client has event-stream operations
     */
    public List<String> incompatibilities(String transportId, Message transportMessage,
                                          Set<String> supportedHttpVersions, boolean eventStreams) {
        Objects.requireNonNull(transportId, "transportId");
        List<String> reasons = new ArrayList<>();
        if (transportMessage != message) {
            reasons.add("Protocol " + protocolId + " exchanges " + message + " messages but transport " + transportId
                        + " exchanges " + transportMessage + " messages");
            return reasons;
        }
        if (httpVersions.stream().noneMatch(supportedHttpVersions::contains)) {
            reasons.add("Protocol " + protocolId + " requires one of the HTTP versions " + httpVersions
                        + " but transport " + transportId + " supports only " + sorted(supportedHttpVersions));
        }
        if (eventStreams && eventStreamHttpVersions.stream().noneMatch(supportedHttpVersions::contains)) {
            reasons.add("Event-stream operations of protocol " + protocolId + " require one of the HTTP versions "
                        + eventStreamHttpVersions + " but transport " + transportId + " supports only "
                        + sorted(supportedHttpVersions));
        }
        return reasons;
    }

    private static List<String> sorted(Set<String> values) {
        return values.stream().sorted().toList();
    }
}
