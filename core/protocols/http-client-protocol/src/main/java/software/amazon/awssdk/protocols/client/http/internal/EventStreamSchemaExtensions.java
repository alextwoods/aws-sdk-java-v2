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

package software.amazon.awssdk.protocols.client.http.internal;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaExtensionKey;
import software.amazon.awssdk.schema.SchemaExtensionProvider;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.serde.InterceptingSerializer;
import software.amazon.awssdk.schema.serde.ShapeSerializer;

// CHECKSTYLE:OFF - Keeps the smithy-java name, as HttpBindingSchemaExtensions does.
/**
 * Pre-computes the AWS event stream member partition of each structure and union, so the event stream encoder and
 * decoder do not walk the members and call {@code hasTrait} on every message. One value is computed per Schema instance
 * and cached by {@link Schema#getExtension}.
 *
 * <p>Each member of an event structure belongs to one disjoint group:
 * <ul>
 *     <li><b>event payload</b>: the {@code eventPayload} member, written as the whole message payload;</li>
 *     <li><b>header</b>: the {@code eventHeader} members, written as message headers;</li>
 *     <li><b>streaming</b>: in the initial request or response structure, the member whose target has
 *     {@code streaming} (the event stream itself);</li>
 *     <li><b>payload</b>: every other member, written to the message body through the codec.</li>
 * </ul>
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.aws.events.EventStreamSchemaExtensions}. SDK deviations:
 * it lives in this {@code internal} package rather than next to the encoder, because {@link java.util.ServiceLoader}
 * needs a public class; its records are final classes; and the filtering structures take the schema they report, so a
 * union member's view reports the union member, as {@code AwsEventShapeEncoder}'s previous filter did. Registered in
 * {@code META-INF/services/software.amazon.awssdk.schema.SchemaExtensionProvider}.
 */
@SdkInternalApi
public final class EventStreamSchemaExtensions
    implements SchemaExtensionProvider<EventStreamSchemaExtensions.EventStreamExt> {
    // CHECKSTYLE:ON

    /**
     * Extension key for the event stream member partition.
     */
    public static final SchemaExtensionKey<EventStreamExt> KEY = new SchemaExtensionKey<>();

    private static final Schema[] NO_SCHEMAS = new Schema[0];
    private static final Binding[] NO_BINDINGS = new Binding[0];

    /**
     * The event stream group of a member, resolved once from its traits.
     */
    public enum Binding {
        /**
         * Written as a message header ({@code eventHeader}).
         */
        HEADER,
        /**
         * Written to the message body through the codec (no event trait).
         */
        PAYLOAD,
        /**
         * Written as the whole message payload ({@code eventPayload}).
         */
        EVENT_PAYLOAD,
        /**
         * The event stream member of an initial request or response structure (its target has {@code streaming}).
         */
        STREAMING
    }

    /**
     * Returns the partition of a structure or union, or of a member's target.
     *
     * @param schema the schema
     * @return the partition
     * @throws IllegalStateException if the schema has none: it is not a structure, union or member of one, or the
     *     provider is not registered
     */
    public static EventStreamExt extensionOf(Schema schema) {
        EventStreamExt ext = schema.getExtension(KEY);
        if (ext == null) {
            throw new IllegalStateException("Schema " + schema.id() + " has no event-stream extension");
        }
        return ext;
    }

    @Override
    public SchemaExtensionKey<EventStreamExt> key() {
        return KEY;
    }

    @Override
    public EventStreamExt provide(Schema schema) {
        // The encoder looks the partition up through the union member, the decoder through its target. Delegating
        // members to their target computes and caches it once, on the target.
        if (schema.isMember()) {
            return schema.memberTarget().getExtension(KEY);
        }
        ShapeType type = schema.type();
        if (type != ShapeType.STRUCTURE && type != ShapeType.UNION) {
            return null;
        }
        Schema eventPayloadMember = null;
        Schema streamingMember = null;
        List<Schema> headerMembers = null;
        boolean hasPayloadMembers = false;
        List<Schema> members = schema.members();
        // Member indexes are dense declaration-order positions.
        Binding[] bindings = members.isEmpty() ? NO_BINDINGS : new Binding[members.size()];
        for (Schema member : members) {
            int index = member.memberIndex();
            // Precedence streaming > eventPayload > eventHeader > body, as upstream; a valid model puts at most one
            // of these on a member, and the encoder and decoder treat the groups as disjoint.
            if (member.memberTarget().hasTrait(TraitKey.STREAMING_TRAIT)) {
                streamingMember = member;
                bindings[index] = Binding.STREAMING;
            } else if (member.hasTrait(TraitKey.EVENT_PAYLOAD_TRAIT)) {
                eventPayloadMember = member;
                bindings[index] = Binding.EVENT_PAYLOAD;
            } else if (member.hasTrait(TraitKey.EVENT_HEADER_TRAIT)) {
                if (headerMembers == null) {
                    headerMembers = new ArrayList<>();
                }
                headerMembers.add(member);
                bindings[index] = Binding.HEADER;
            } else {
                hasPayloadMembers = true;
                bindings[index] = Binding.PAYLOAD;
            }
        }
        return new EventStreamExt(eventPayloadMember,
                                  headerMembers == null ? NO_SCHEMAS : headerMembers.toArray(NO_SCHEMAS),
                                  hasPayloadMembers, streamingMember, bindings);
    }

    /**
     * The event stream partition of one structure or union. Port of the smithy-java record of the same name.
     */
    public static final class EventStreamExt {
        private final Schema eventPayloadMember;
        private final Schema[] headerMembers;
        private final boolean hasPayloadMembers;
        private final Schema streamingMember;
        private final Binding[] bindings;

        EventStreamExt(Schema eventPayloadMember, Schema[] headerMembers, boolean hasPayloadMembers,
                       Schema streamingMember, Binding[] bindings) {
            this.eventPayloadMember = eventPayloadMember;
            this.headerMembers = headerMembers;
            this.hasPayloadMembers = hasPayloadMembers;
            this.streamingMember = streamingMember;
            this.bindings = bindings;
        }

        /**
         * Returns the {@code eventPayload} member.
         *
         * @return the member, or null if there is none
         */
        public Schema eventPayloadMember() {
            return eventPayloadMember;
        }

        /**
         * Returns the {@code eventHeader} members, in declaration order. The array is shared: callers must not modify
         * it.
         *
         * @return the members; empty if there are none
         */
        public Schema[] headerMembers() {
            return headerMembers;
        }

        /**
         * Returns whether any member is written to the body through the codec: one that is neither a header, the
         * event payload nor the event stream member.
         *
         * @return true if there is a body member
         */
        public boolean hasPayloadMembers() {
            return hasPayloadMembers;
        }

        /**
         * Returns the member whose target has {@code streaming}: the event stream member of an initial request or
         * response structure.
         *
         * @return the member, or null if there is none
         */
        public Schema streamingMember() {
            return streamingMember;
        }

        /**
         * Returns the group of each member, indexed by {@link Schema#memberIndex()}. The array is shared: callers must
         * not modify it.
         *
         * @return the bindings
         */
        public Binding[] bindings() {
            return bindings;
        }

        /**
         * Returns whether there is an {@code eventPayload} member.
         *
         * @return true if there is one
         */
        public boolean hasEventPayload() {
            return eventPayloadMember != null;
        }

        /**
         * Returns whether there is an {@code eventHeader} member.
         *
         * @return true if there is one
         */
        public boolean hasHeaderMembers() {
            return headerMembers.length > 0;
        }

        /**
         * Returns whether this is an initial request or response structure: one with an event stream member.
         *
         * @return true if there is a streaming member
         */
        public boolean isInitialEvent() {
            return streamingMember != null;
        }
    }

    /**
     * A view of a structure that writes only the members in one group, chosen with one array load per member. Port
     * of the smithy-java record of the same name.
     */
    public static final class BindingFilteredStruct implements SerializableStruct {
        private final Schema schema;
        private final SerializableStruct delegate;
        private final Binding[] bindings;
        private final Binding keep;

        /**
         * Creates the view.
         *
         * @param schema the schema the view reports
         * @param delegate the structure
         * @param bindings the bindings of the structure's members
         * @param keep the group to write
         */
        public BindingFilteredStruct(Schema schema, SerializableStruct delegate, Binding[] bindings, Binding keep) {
            this.schema = schema;
            this.delegate = delegate;
            this.bindings = bindings;
            this.keep = keep;
        }

        @Override
        public Schema sdkSchema() {
            return schema;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            delegate.serializeMembers(new InterceptingSerializer() {
                @Override
                protected ShapeSerializer before(Schema member) {
                    return bindings[member.memberIndex()] == keep ? serializer : ShapeSerializer.nullSerializer();
                }
            });
        }

        @Override
        public <T> T getMemberValue(Schema member) {
            return bindings[member.memberIndex()] == keep ? delegate.getMemberValue(member) : null;
        }
    }

    /**
     * A view of a structure that writes every member except those in one group: the initial request or response
     * without its event stream member. Port of the smithy-java record of the same name.
     */
    public static final class BindingExcludingStruct implements SerializableStruct {
        private final Schema schema;
        private final SerializableStruct delegate;
        private final Binding[] bindings;
        private final Binding exclude;

        /**
         * Creates the view.
         *
         * @param schema the schema the view reports
         * @param delegate the structure
         * @param bindings the bindings of the structure's members
         * @param exclude the group not to write
         */
        public BindingExcludingStruct(Schema schema, SerializableStruct delegate, Binding[] bindings,
                                      Binding exclude) {
            this.schema = schema;
            this.delegate = delegate;
            this.bindings = bindings;
            this.exclude = exclude;
        }

        @Override
        public Schema sdkSchema() {
            return schema;
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            delegate.serializeMembers(new InterceptingSerializer() {
                @Override
                protected ShapeSerializer before(Schema member) {
                    return bindings[member.memberIndex()] != exclude ? serializer : ShapeSerializer.nullSerializer();
                }
            });
        }

        @Override
        public <T> T getMemberValue(Schema member) {
            return bindings[member.memberIndex()] != exclude ? delegate.getMemberValue(member) : null;
        }
    }
}
