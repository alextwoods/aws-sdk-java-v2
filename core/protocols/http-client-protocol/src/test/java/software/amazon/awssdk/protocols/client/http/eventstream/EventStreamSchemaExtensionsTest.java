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

package software.amazon.awssdk.protocols.client.http.eventstream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.protocols.client.http.eventstream.EventStreamTestShapes.BLOB_EVENT;
import static software.amazon.awssdk.protocols.client.http.eventstream.EventStreamTestShapes.BODY_AND_HEADER_EVENT;
import static software.amazon.awssdk.protocols.client.http.eventstream.EventStreamTestShapes.EVENT_STREAM;
import static software.amazon.awssdk.protocols.client.http.eventstream.EventStreamTestShapes.HEADERS_ONLY_EVENT;
import static software.amazon.awssdk.protocols.client.http.eventstream.EventStreamTestShapes.INPUT;
import static software.amazon.awssdk.protocols.client.http.eventstream.EventStreamTestShapes.STRING_EVENT;
import static software.amazon.awssdk.protocols.client.http.eventstream.EventStreamTestShapes.STRUCTURE;
import static software.amazon.awssdk.protocols.client.http.eventstream.EventStreamTestShapes.STRUCTURE_EVENT;
import static software.amazon.awssdk.protocols.client.http.eventstream.EventStreamTestShapes.struct;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.codecs.json.JsonCodec;
import software.amazon.awssdk.protocols.client.http.internal.EventStreamSchemaExtensions;
import software.amazon.awssdk.protocols.client.http.internal.EventStreamSchemaExtensions.Binding;
import software.amazon.awssdk.protocols.client.http.internal.EventStreamSchemaExtensions.BindingExcludingStruct;
import software.amazon.awssdk.protocols.client.http.internal.EventStreamSchemaExtensions.BindingFilteredStruct;
import software.amazon.awssdk.protocols.client.http.internal.EventStreamSchemaExtensions.EventStreamExt;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.ShapeSerializer;
import software.amazon.awssdk.schema.serde.SpecificShapeSerializer;
import software.amazon.awssdk.schema.traits.EventHeaderTrait;
import software.amazon.awssdk.schema.traits.EventPayloadTrait;
import software.amazon.eventstream.Message;

/**
 * Port of smithy-java {@code EventStreamSchemaExtensionsTest} (smithy-java {@code 8e892b96c}), on the shapes of
 * {@link EventStreamTestShapes}, plus the filtering views and the decoder's member order.
 */
class EventStreamSchemaExtensionsTest {
    private static final EventStreamSchemaExtensions PROVIDER = new EventStreamSchemaExtensions();
    private static final Codec CODEC = JsonCodec.builder().build();

    private static List<String> names(Schema[] members) {
        List<String> result = new ArrayList<>();
        for (Schema member : members) {
            result.add(member.memberName());
        }
        return result;
    }

    @Test
    void provide_nonAggregateSchema_returnsNull() {
        assertThat(PROVIDER.provide(PreludeSchemas.STRING)).isNull();
        assertThat(PROVIDER.provide(PreludeSchemas.INTEGER)).isNull();
    }

    @Test
    void extensionOf_schemaWithoutExtension_throws() {
        assertThatThrownBy(() -> EventStreamSchemaExtensions.extensionOf(PreludeSchemas.STRING))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Schema smithy.api#String has no event-stream extension");
    }

    @Test
    void provide_structureWithoutEventTraits_isAllPayload() {
        EventStreamExt ext = PROVIDER.provide(STRUCTURE);

        assertThat(ext.eventPayloadMember()).isNull();
        assertThat(ext.hasEventPayload()).isFalse();
        assertThat(ext.isInitialEvent()).isFalse();
        assertThat(ext.streamingMember()).isNull();
        assertThat(ext.hasPayloadMembers()).isTrue();
        assertThat(ext.hasHeaderMembers()).isFalse();
        assertThat(ext.headerMembers()).isEmpty();
        assertThat(ext.bindings()).containsExactly(Binding.PAYLOAD);
    }

    @Test
    void provide_headersOnlyEvent_hasOnlyHeaderMembers() {
        EventStreamExt ext = PROVIDER.provide(HEADERS_ONLY_EVENT);

        assertThat(ext.eventPayloadMember()).isNull();
        assertThat(ext.hasPayloadMembers()).isFalse();
        assertThat(ext.hasHeaderMembers()).isTrue();
        assertThat(names(ext.headerMembers())).containsExactly("sequence");
        assertThat(ext.bindings()).containsExactly(Binding.HEADER);
    }

    @Test
    void provide_bodyAndHeaderEvent_isPartitionedInDeclarationOrder() {
        EventStreamExt ext = PROVIDER.provide(BODY_AND_HEADER_EVENT);

        assertThat(ext.eventPayloadMember()).isNull();
        assertThat(ext.hasPayloadMembers()).isTrue();
        assertThat(names(ext.headerMembers()))
            .containsExactly("stringHeader", "intHeader", "longHeader", "booleanHeader", "timestampHeader",
                             "blobHeader");
        assertThat(ext.bindings()).containsExactly(Binding.HEADER, Binding.HEADER, Binding.HEADER, Binding.HEADER,
                                                   Binding.HEADER, Binding.HEADER, Binding.PAYLOAD, Binding.PAYLOAD);
    }

    @Test
    void provide_eventPayloadEvents_haveTheEventPayloadMember() {
        EventStreamExt blob = PROVIDER.provide(BLOB_EVENT);
        assertThat(blob.hasEventPayload()).isTrue();
        assertThat(blob.eventPayloadMember()).isSameAs(BLOB_EVENT.member("data"));
        assertThat(blob.hasPayloadMembers()).isFalse();
        assertThat(names(blob.headerMembers())).containsExactly("id");
        assertThat(blob.bindings()).containsExactly(Binding.HEADER, Binding.EVENT_PAYLOAD);

        EventStreamExt string = PROVIDER.provide(STRING_EVENT);
        assertThat(string.eventPayloadMember()).isSameAs(STRING_EVENT.member("text"));
        assertThat(string.hasPayloadMembers()).isFalse();
        assertThat(string.hasHeaderMembers()).isFalse();

        EventStreamExt structure = PROVIDER.provide(STRUCTURE_EVENT);
        assertThat(structure.eventPayloadMember()).isSameAs(STRUCTURE_EVENT.member("nested"));
        assertThat(structure.hasPayloadMembers()).isFalse();
    }

    @Test
    void provide_initialRequestStructure_hasTheStreamingMember() {
        EventStreamExt ext = PROVIDER.provide(INPUT);

        assertThat(ext.isInitialEvent()).isTrue();
        assertThat(ext.streamingMember()).isSameAs(INPUT.member("stream"));
        // The streaming member is in neither the header nor the payload group.
        assertThat(ext.headerMembers()).isEmpty();
        assertThat(ext.hasPayloadMembers()).isTrue();
        assertThat(ext.bindings()).containsExactly(Binding.PAYLOAD, Binding.STREAMING);
    }

    @Test
    void provide_union_isPartitionedLikeAStructure() {
        EventStreamExt ext = PROVIDER.provide(EVENT_STREAM);

        assertThat(ext.isInitialEvent()).isFalse();
        assertThat(ext.bindings()).hasSize(EVENT_STREAM.members().size()).containsOnly(Binding.PAYLOAD);
    }

    @Test
    void getExtension_memberSchema_returnsTheTargetsCachedInstance() {
        Schema unionMember = EVENT_STREAM.member("blob");

        EventStreamExt viaMember = unionMember.getExtension(EventStreamSchemaExtensions.KEY);

        assertThat(viaMember).isNotNull().isSameAs(BLOB_EVENT.getExtension(EventStreamSchemaExtensions.KEY));
        assertThat(BLOB_EVENT.getExtension(EventStreamSchemaExtensions.KEY)).isSameAs(viaMember);
    }

    @Test
    void bindingFilteredStruct_writesAndReturnsOnlyTheKeptGroup() {
        EventStreamTestShapes.Struct event = struct(BLOB_EVENT, "id", "i", "data", new byte[] {1});
        Binding[] bindings = EventStreamSchemaExtensions.extensionOf(BLOB_EVENT).bindings();
        Schema unionMember = EVENT_STREAM.member("blob");

        BindingFilteredStruct headers = new BindingFilteredStruct(unionMember, event, bindings, Binding.HEADER);

        assertThat(headers.sdkSchema()).isSameAs(unionMember);
        assertThat(written(headers)).containsExactly("id");
        assertThat(written(new BindingFilteredStruct(unionMember, event, bindings, Binding.EVENT_PAYLOAD)))
            .containsExactly("data");
        assertThat((Object) headers.getMemberValue(BLOB_EVENT.member("id"))).isEqualTo("i");
        assertThat((Object) headers.getMemberValue(BLOB_EVENT.member("data"))).isNull();
    }

    @Test
    void bindingExcludingStruct_writesAndReturnsEveryOtherGroup() {
        EventStreamTestShapes.Struct input = struct(INPUT, "inputHeader", "x", "stream", "not written");
        Binding[] bindings = EventStreamSchemaExtensions.extensionOf(INPUT).bindings();

        BindingExcludingStruct initial = new BindingExcludingStruct(INPUT, input, bindings, Binding.STREAMING);

        assertThat(initial.sdkSchema()).isSameAs(INPUT);
        assertThat(written(struct(INPUT, "inputHeader", "x"), bindings, Binding.STREAMING))
            .containsExactly("inputHeader");
        assertThat((Object) initial.getMemberValue(INPUT.member("inputHeader"))).isEqualTo("x");
        assertThat((Object) initial.getMemberValue(INPUT.member("stream"))).isNull();
    }

    /**
     * The decoder reads the event payload member at its declaration position among the header members, as the trait
     * walk it replaced did, skips absent headers, and does not read an empty payload.
     */
    @Test
    void eventStreamDeserializer_readsHeaderAndEventPayloadMembersInDeclarationOrder() {
        Schema ordered = Schema.structureBuilder(ShapeId.fromParts(EventStreamTestShapes.NS, "OrderedEvent"))
                               .putMember("first", PreludeSchemas.STRING, EventHeaderTrait.INSTANCE)
                               .putMember("data", PreludeSchemas.BLOB, EventPayloadTrait.INSTANCE)
                               .putMember("second", PreludeSchemas.STRING, EventHeaderTrait.INSTANCE)
                               .putMember("absent", PreludeSchemas.STRING, EventHeaderTrait.INSTANCE)
                               .build();
        Message message = new Message(HeadersBuilder.forEvent().eventType("ordered").put("first", "1")
                                                    .put("second", "2").build(), new byte[] {7});
        Message empty = new Message(HeadersBuilder.forEvent().eventType("ordered").put("first", "1")
                                                  .put("second", "2").build(), new byte[0]);

        assertThat(readOrder(ordered, message)).containsExactly("first", "data", "second");
        assertThat(readOrder(ordered, empty)).containsExactly("first", "second");
        assertThat(readOrder(BLOB_EVENT, message)).containsExactly("data");
    }

    private static List<String> readOrder(Schema schema, Message message) {
        List<String> order = new ArrayList<>();
        new AwsEventShapeDecoder.EventStreamDeserializer(CODEC, message)
            .readStruct(schema, order, (o, member, d) -> o.add(member.memberName()));
        return order;
    }

    private static List<String> written(EventStreamTestShapes.Struct struct, Binding[] bindings, Binding exclude) {
        List<String> names = new ArrayList<>();
        new BindingExcludingStruct(struct.sdkSchema(), struct, bindings, exclude).serializeMembers(recorder(names));
        return names;
    }

    private static List<String> written(BindingFilteredStruct view) {
        List<String> names = new ArrayList<>();
        view.serializeMembers(recorder(names));
        return names;
    }

    private static ShapeSerializer recorder(List<String> names) {
        return new SpecificShapeSerializer() {
            @Override
            public void writeString(Schema schema, String value) {
                names.add(schema.memberName());
            }

            @Override
            public void writeBlob(Schema schema, ByteBuffer value) {
                names.add(schema.memberName());
            }
        };
    }
}
