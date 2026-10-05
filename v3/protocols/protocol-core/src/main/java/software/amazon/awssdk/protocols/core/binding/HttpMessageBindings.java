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

package software.amazon.awssdk.protocols.core.binding;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;

/**
 * The immutable HTTP binding plan of one structure in one direction: where each member goes in the HTTP message,
 * derived once from the Smithy HTTP binding traits on the member schemas.
 *
 * <p>Smithy binding semantics are applied exactly:
 * <ul>
 *   <li>in a {@linkplain Direction#REQUEST request}, {@code httpLabel}, {@code httpQuery}, {@code httpQueryParams},
 *       {@code httpHeader}, {@code httpPrefixHeaders} and {@code httpPayload} bind; {@code httpResponseCode} has no
 *       meaning and the member stays in the body;</li>
 *   <li>in a {@linkplain Direction#RESPONSE response} (output or error), {@code httpHeader},
 *       {@code httpPrefixHeaders}, {@code httpPayload} and {@code httpResponseCode} bind; {@code httpLabel},
 *       {@code httpQuery} and {@code httpQueryParams} have no meaning and the member stays in the body;</li>
 *   <li>every other member is a {@link HttpBindingLocation#BODY} member.</li>
 * </ul>
 * The plan rejects structures no HTTP message can represent: more than one payload member, a payload member together
 * with body members, and more than one {@code httpQueryParams} or {@code httpResponseCode} member.
 *
 * <p>Protocols that ignore HTTP binding traits ({@link
 * software.amazon.awssdk.protocols.core.ProtocolProvider#usesHttpBindingTraits()}) do not use these plans.
 */
public final class HttpMessageBindings {
    private final Schema structure;
    private final Direction direction;
    private final List<HttpMemberBinding> bindings;
    private final Map<HttpBindingLocation, List<HttpMemberBinding>> byLocation;
    private final PayloadKind payloadKind;

    /** Which HTTP message a structure is bound to. */
    public enum Direction {
        /** An operation input, bound to the HTTP request. */
        REQUEST,
        /** An operation output or error, bound to the HTTP response. */
        RESPONSE
    }

    /** What the message body holds. */
    public enum PayloadKind {
        /** No body: no payload member and no body members. */
        NONE,
        /** A protocol-serialized document of the {@link HttpBindingLocation#BODY} members. */
        MEMBERS,
        /** A protocol-serialized structure, union or document payload member. */
        STRUCTURED,
        /** A non-streaming blob payload member, sent as raw bytes. */
        BLOB,
        /** A string or enum payload member, sent as text. */
        STRING,
        /** A streaming blob payload member. */
        STREAMING_BLOB,
        /** An event stream payload member. */
        EVENT_STREAM
    }

    private HttpMessageBindings(Schema structure, Direction direction, List<HttpMemberBinding> bindings) {
        this.structure = structure;
        this.direction = direction;
        this.bindings = List.copyOf(bindings);
        Map<HttpBindingLocation, List<HttpMemberBinding>> grouped = new EnumMap<>(HttpBindingLocation.class);
        for (HttpBindingLocation location : HttpBindingLocation.values()) {
            grouped.put(location, this.bindings.stream().filter(b -> b.location() == location).toList());
        }
        this.byLocation = grouped;
        requireAtMostOne(HttpBindingLocation.PAYLOAD);
        requireAtMostOne(HttpBindingLocation.QUERY_PARAMS);
        requireAtMostOne(HttpBindingLocation.STATUS_CODE);
        if (!members(HttpBindingLocation.PAYLOAD).isEmpty() && !members(HttpBindingLocation.BODY).isEmpty()) {
            throw new IllegalArgumentException(
                structure.id() + " binds " + names(members(HttpBindingLocation.PAYLOAD)) + " to the HTTP payload, so "
                + "every other member must be bound to another location, but " + names(members(HttpBindingLocation.BODY))
                + " are not");
        }
        this.payloadKind = computePayloadKind();
    }

    /**
     * The binding plan of {@code structure} (an operation input, output or error) bound in {@code direction}.
     *
     * @throws IllegalArgumentException if {@code structure} is not a structure schema or no HTTP message can
     *         represent its bindings
     */
    public static HttpMessageBindings of(Schema structure, Direction direction) {
        Objects.requireNonNull(structure, "structure");
        Objects.requireNonNull(direction, "direction");
        if (structure.isMember() || structure.type() != ShapeType.STRUCTURE) {
            throw new IllegalArgumentException("HTTP bindings apply to structures, not " + structure.type() + " "
                                               + structure.id());
        }
        List<HttpMemberBinding> bindings = new ArrayList<>(structure.members().size());
        for (Schema member : structure.members()) {
            bindings.add(bind(member, direction));
        }
        return new HttpMessageBindings(structure, direction, bindings);
    }

    private static HttpMemberBinding bind(Schema member, Direction direction) {
        HttpHeaderTrait header = member.getTrait(TraitKey.HTTP_HEADER_TRAIT);
        if (header != null) {
            return new HttpMemberBinding(member, HttpBindingLocation.HEADER, header.getValue());
        }
        HttpPrefixHeadersTrait prefix = member.getTrait(TraitKey.HTTP_PREFIX_HEADERS_TRAIT);
        if (prefix != null) {
            return new HttpMemberBinding(member, HttpBindingLocation.PREFIX_HEADERS, prefix.getValue());
        }
        if (member.hasTrait(TraitKey.HTTP_PAYLOAD_TRAIT)) {
            return new HttpMemberBinding(member, HttpBindingLocation.PAYLOAD, "");
        }
        if (direction == Direction.REQUEST) {
            if (member.hasTrait(TraitKey.HTTP_LABEL_TRAIT)) {
                return new HttpMemberBinding(member, HttpBindingLocation.LABEL, member.memberName());
            }
            HttpQueryTrait query = member.getTrait(TraitKey.HTTP_QUERY_TRAIT);
            if (query != null) {
                return new HttpMemberBinding(member, HttpBindingLocation.QUERY, query.getValue());
            }
            if (member.hasTrait(TraitKey.HTTP_QUERY_PARAMS_TRAIT)) {
                return new HttpMemberBinding(member, HttpBindingLocation.QUERY_PARAMS, "");
            }
        } else if (member.hasTrait(TraitKey.HTTP_RESPONSE_CODE_TRAIT)) {
            return new HttpMemberBinding(member, HttpBindingLocation.STATUS_CODE, "");
        }
        return new HttpMemberBinding(member, HttpBindingLocation.BODY, "");
    }

    /** The structure schema. */
    public Schema structure() {
        return structure;
    }

    /** The direction the structure is bound in. */
    public Direction direction() {
        return direction;
    }

    /** The binding of every member, in modeled member order. */
    public List<HttpMemberBinding> bindings() {
        return bindings;
    }

    /** The members bound to {@code location}, in modeled member order. */
    public List<HttpMemberBinding> members(HttpBindingLocation location) {
        return byLocation.get(location);
    }

    /** The {@code httpPayload} member, if any. */
    public Optional<HttpMemberBinding> payload() {
        List<HttpMemberBinding> payload = members(HttpBindingLocation.PAYLOAD);
        return payload.isEmpty() ? Optional.empty() : Optional.of(payload.get(0));
    }

    /** What the message body holds. */
    public PayloadKind payloadKind() {
        return payloadKind;
    }

    private PayloadKind computePayloadKind() {
        Optional<HttpMemberBinding> payload = payload();
        if (payload.isEmpty()) {
            return members(HttpBindingLocation.BODY).isEmpty() ? PayloadKind.NONE : PayloadKind.MEMBERS;
        }
        Schema member = payload.get().member();
        boolean streaming = member.hasTrait(TraitKey.STREAMING_TRAIT);
        return switch (member.type()) {
            case BLOB -> streaming ? PayloadKind.STREAMING_BLOB : PayloadKind.BLOB;
            case UNION -> streaming ? PayloadKind.EVENT_STREAM : PayloadKind.STRUCTURED;
            case STRING, ENUM -> PayloadKind.STRING;
            case STRUCTURE, DOCUMENT -> PayloadKind.STRUCTURED;
            default -> throw new IllegalArgumentException(
                "The httpPayload member " + member.id() + " targets a " + member.type() + ", which cannot be an "
                + "HTTP payload");
        };
    }

    @Override
    public String toString() {
        return "HttpMessageBindings[" + structure.id() + " " + direction + "]";
    }

    private void requireAtMostOne(HttpBindingLocation location) {
        List<HttpMemberBinding> members = members(location);
        if (members.size() > 1) {
            throw new IllegalArgumentException(structure.id() + " binds more than one member to " + location + ": "
                                               + names(members));
        }
    }

    private static String names(List<HttpMemberBinding> bindings) {
        return bindings.stream().map(HttpMemberBinding::memberName).collect(Collectors.joining(", ", "[", "]"));
    }
}
