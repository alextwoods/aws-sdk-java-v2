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

package software.amazon.awssdk.messaging.payload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static software.amazon.awssdk.messaging.payload.PayloadTestShapes.REGISTRY;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.codecs.cbor.Rpcv2CborCodec;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.codecs.json.JsonCodec;
import software.amazon.awssdk.codecs.xml.XmlCodec;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.messaging.payload.MessagePayloadException.Cause;
import software.amazon.awssdk.messaging.payload.PayloadTestShapes.Notice;
import software.amazon.awssdk.messaging.payload.PayloadTestShapes.OrderCancelled;
import software.amazon.awssdk.messaging.payload.PayloadTestShapes.OrderPlaced;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.schema.serde.document.DiscriminatorException;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.utils.BinaryUtils;

class MessagePayloadsTest {
    private static final JsonCodec JSON = JsonCodec.builder().build();
    private static final Rpcv2CborCodec CBOR = Rpcv2CborCodec.builder().build();
    private static final OrderPlaced PLACED = new OrderPlaced("o-1", 3);
    private static final OrderCancelled CANCELLED = new OrderCancelled("o-2", "out of stock");

    static Stream<Codec> codecs() {
        return Stream.of(JSON, CBOR);
    }

    // serialize

    @Test
    void serialize_jsonStructure_writesTypeFirstAsUtf8Text() {
        assertThat(MessagePayloads.serialize(PLACED, JSON))
            .isEqualTo("{\"__type\":\"smithy.example#OrderPlaced\",\"orderId\":\"o-1\",\"quantity\":3}");
    }

    @Test
    void serialize_jsonWithoutSerializeTypeInDocuments_writesPlainStructure() {
        JsonCodec codec = JsonCodec.builder().serializeTypeInDocuments(false).build();

        assertThat(MessagePayloads.serialize(PLACED, codec)).isEqualTo("{\"orderId\":\"o-1\",\"quantity\":3}");
    }

    @Test
    void serialize_jsonUnion_writesNoType() {
        assertThat(MessagePayloads.serialize(new Notice("hi"), JSON)).isEqualTo("{\"text\":\"hi\"}");
    }

    @Test
    void serialize_cbor_writesBase64OfStructureDocument() {
        String body = MessagePayloads.serialize(PLACED, CBOR);

        assertThat(body).matches("[A-Za-z0-9+/]+=*");
        byte[] expected = BinaryUtils.copyBytesFrom(CBOR.serialize(Document.of(PLACED)));
        assertThat(Base64.getDecoder().decode(body)).isEqualTo(expected);
        Document document = CBOR.createDeserializer(Base64.getDecoder().decode(body)).readDocument();
        assertThat(document.getDiscriminator()).isEqualTo(OrderPlaced.ID);
    }

    @Test
    void serialize_xml_writesUtf8TextWithoutType() {
        String body = MessagePayloads.serialize(PLACED, XmlCodec.builder().build());

        assertThat(body).isEqualTo("<OrderPlaced><orderId>o-1</orderId><quantity>3</quantity></OrderPlaced>");
    }

    // deserialize with a builder

    @ParameterizedTest
    @MethodSource("codecs")
    void deserializeWithBuilder_roundTrip_returnsEqualValue(Codec codec) {
        String body = MessagePayloads.serialize(PLACED, codec);

        assertThat(MessagePayloads.deserialize(body, codec, OrderPlaced.builder())).isEqualTo(PLACED);
    }

    @ParameterizedTest
    @MethodSource("codecs")
    void deserializeWithBuilder_unionRoundTrip_returnsEqualValue(Codec codec) {
        Notice notice = new Notice("hi");
        String body = MessagePayloads.serialize(notice, codec);

        assertThat(MessagePayloads.deserialize(body, codec, new Notice.Builder())).isEqualTo(notice);
    }

    @Test
    void deserializeWithBuilder_xmlRoundTrip_returnsEqualValue() {
        XmlCodec xml = XmlCodec.builder().build();
        String body = MessagePayloads.serialize(PLACED, xml);

        assertThat(MessagePayloads.deserialize(body, xml, OrderPlaced.builder())).isEqualTo(PLACED);
    }

    @Test
    void deserializeWithBuilder_jsonWithoutType_readsMembers() {
        assertThat(MessagePayloads.deserialize("{\"orderId\":\"o-1\",\"quantity\":3}", JSON, OrderPlaced.builder()))
            .isEqualTo(PLACED);
    }

    @Test
    void deserializeWithBuilder_typeOfAnotherShape_isIgnored() {
        String body = "{\"__type\":\"smithy.example#OrderCancelled\",\"orderId\":\"o-1\",\"quantity\":3}";

        assertThat(MessagePayloads.deserialize(body, JSON, OrderPlaced.builder())).isEqualTo(PLACED);
    }

    // deserialize with a registry and __type

    @ParameterizedTest
    @MethodSource("codecs")
    void deserializeWithRegistry_roundTrip_returnsValueOfDiscriminatedType(Codec codec) {
        assertThat(MessagePayloads.deserialize(MessagePayloads.serialize(PLACED, codec), codec, REGISTRY))
            .isEqualTo(PLACED);
        assertThat(MessagePayloads.deserialize(MessagePayloads.serialize(CANCELLED, codec), codec, REGISTRY))
            .isEqualTo(CANCELLED);
    }

    @Test
    void deserializeWithRegistry_handWrittenJson_readsType() {
        String body = "{\"orderId\":\"o-2\",\"reason\":\"out of stock\",\"__type\":\"smithy.example#OrderCancelled\"}";

        assertThat(MessagePayloads.deserialize(body, JSON, REGISTRY)).isEqualTo(CANCELLED);
    }

    // deserialize with a registry and a caller-supplied ShapeId

    @ParameterizedTest
    @MethodSource("codecs")
    void deserializeWithShapeId_roundTrip_returnsValueOfSuppliedType(Codec codec) {
        String body = MessagePayloads.serialize(CANCELLED, codec);

        assertThat(MessagePayloads.deserialize(body, codec, REGISTRY, OrderCancelled.ID)).isEqualTo(CANCELLED);
    }

    @ParameterizedTest
    @MethodSource("codecs")
    void deserializeWithShapeId_union_returnsUnion(Codec codec) {
        Notice notice = new Notice("hi");
        String body = MessagePayloads.serialize(notice, codec);

        assertThat(MessagePayloads.deserialize(body, codec, REGISTRY, Notice.ID)).isEqualTo(notice);
    }

    @Test
    void deserializeWithShapeId_bodyWithoutType_usesSuppliedType() {
        assertThat(MessagePayloads.deserialize("{\"orderId\":\"o-1\",\"quantity\":3}", JSON, REGISTRY,
                                               OrderPlaced.ID))
            .isEqualTo(PLACED);
    }

    @Test
    void deserializeWithShapeId_bodyTypeDiffers_usesSuppliedType() {
        String body = MessagePayloads.serialize(CANCELLED, JSON);

        assertThat(MessagePayloads.deserialize(body, JSON, REGISTRY, OrderPlaced.ID))
            .isEqualTo(new OrderPlaced("o-2", null));
    }

    // MALFORMED

    @ParameterizedTest
    @ValueSource(strings = {"{\"orderId\":", "{\"orderId\" \"o-1\"}", "{} {}", "nul"})
    void deserialize_malformedJson_isMalformedForEveryVariant(String body) {
        assertPayloadFailure(() -> MessagePayloads.deserialize(body, JSON, OrderPlaced.builder()), Cause.MALFORMED,
                             SerializationException.class, "is not well-formed for JsonCodec");
        assertPayloadFailure(() -> MessagePayloads.deserialize(body, JSON, REGISTRY), Cause.MALFORMED,
                             SerializationException.class, "is not well-formed for JsonCodec");
        assertPayloadFailure(() -> MessagePayloads.deserialize(body, JSON, REGISTRY, OrderPlaced.ID),
                             Cause.MALFORMED, SerializationException.class, "is not well-formed for JsonCodec");
    }

    @Test
    void deserialize_emptyJsonBody_followsCodecForShapesAndIsMalformedForRegistry() {
        // The JSON codec reads an empty input as an empty structure (an empty HTTP body), and MessagePayloads keeps
        // the codec's behavior. A registry needs a document, and an empty input holds none.
        assertThat(MessagePayloads.deserialize("", JSON, OrderPlaced.builder())).isEqualTo(new OrderPlaced(null, null));
        assertPayloadFailure(() -> MessagePayloads.deserialize("", JSON, REGISTRY), Cause.MALFORMED,
                             SerializationException.class, "is not well-formed for JsonCodec");
    }

    @ParameterizedTest
    @ValueSource(strings = {"not base64!", "A", "oWFh\n", "b2s_"})
    void deserialize_cborInvalidBase64_isMalformedForEveryVariant(String body) {
        String message = "is not valid base64, which the binary codec Rpcv2CborCodec requires";
        assertPayloadFailure(() -> MessagePayloads.deserialize(body, CBOR, OrderPlaced.builder()), Cause.MALFORMED,
                             IllegalArgumentException.class, message);
        assertPayloadFailure(() -> MessagePayloads.deserialize(body, CBOR, REGISTRY), Cause.MALFORMED,
                             IllegalArgumentException.class, message);
        assertPayloadFailure(() -> MessagePayloads.deserialize(body, CBOR, REGISTRY, OrderPlaced.ID),
                             Cause.MALFORMED, IllegalArgumentException.class, message);
    }

    @Test
    void deserialize_cborTruncated_isMalformedForEveryVariant() {
        byte[] full = Base64.getDecoder().decode(MessagePayloads.serialize(PLACED, CBOR));
        String body = Base64.getEncoder().encodeToString(Arrays.copyOf(full, full.length - 2));

        assertPayloadFailure(() -> MessagePayloads.deserialize(body, CBOR, OrderPlaced.builder()), Cause.MALFORMED,
                             SerializationException.class, "is not well-formed for Rpcv2CborCodec");
        assertPayloadFailure(() -> MessagePayloads.deserialize(body, CBOR, REGISTRY), Cause.MALFORMED,
                             SerializationException.class, "is not well-formed for Rpcv2CborCodec");
        assertPayloadFailure(() -> MessagePayloads.deserialize(body, CBOR, REGISTRY, OrderPlaced.ID),
                             Cause.MALFORMED, SerializationException.class, "is not well-formed for Rpcv2CborCodec");
    }

    // NON_CONFORMING

    @ParameterizedTest
    @ValueSource(strings = {"{\"quantity\":\"three\"}", "{\"orderId\":{}}", "[1]", "\"text\""})
    void deserialize_wellFormedJsonOfWrongShape_isNonConforming(String body) {
        assertPayloadFailure(() -> MessagePayloads.deserialize(body, JSON, OrderPlaced.builder()),
                             Cause.NON_CONFORMING, SerializationException.class,
                             "does not conform to the target shape");
        assertPayloadFailure(() -> MessagePayloads.deserialize(body, JSON, REGISTRY, OrderPlaced.ID),
                             Cause.NON_CONFORMING, SerializationException.class,
                             "does not conform to the target shape");
    }

    @Test
    void deserializeWithRegistry_wrongMemberType_isNonConformingAndNamesShape() {
        String body = "{\"__type\":\"smithy.example#OrderPlaced\",\"quantity\":\"three\"}";

        assertPayloadFailure(() -> MessagePayloads.deserialize(body, JSON, REGISTRY), Cause.NON_CONFORMING,
                             SerializationException.class, "does not conform to smithy.example#OrderPlaced");
    }

    @Test
    void deserialize_cborWrongMemberType_isNonConforming() {
        Document wrong = Document.of(Collections.singletonMap("quantity", Document.of("three")));
        String body = Base64.getEncoder().encodeToString(BinaryUtils.copyBytesFrom(CBOR.serialize(wrong)));

        assertPayloadFailure(() -> MessagePayloads.deserialize(body, CBOR, OrderPlaced.builder()),
                             Cause.NON_CONFORMING, SerializationException.class,
                             "does not conform to the target shape");
        assertPayloadFailure(() -> MessagePayloads.deserialize(body, CBOR, REGISTRY, OrderPlaced.ID),
                             Cause.NON_CONFORMING, SerializationException.class,
                             "does not conform to the target shape");
    }

    @Test
    void deserialize_xmlSyntaxError_isNonConformingBecauseXmlHasNoDocuments() {
        XmlCodec xml = XmlCodec.builder().build();

        assertPayloadFailure(() -> MessagePayloads.deserialize("<OrderPlaced><orderId>", xml, OrderPlaced.builder()),
                             Cause.NON_CONFORMING, SerializationException.class,
                             "does not conform to the target shape");
    }

    // UNKNOWN_DISCRIMINATOR

    @ParameterizedTest
    @ValueSource(strings = {"{\"orderId\":\"o-1\"}", "{\"__type\":1,\"orderId\":\"o-1\"}", "[1]", "\"text\""})
    void deserializeWithRegistry_noDiscriminator_isUnknownDiscriminator(String body) {
        assertPayloadFailure(() -> MessagePayloads.deserialize(body, JSON, REGISTRY), Cause.UNKNOWN_DISCRIMINATOR,
                             DiscriminatorException.class, "has no discriminator");
    }

    @Test
    void deserializeWithRegistry_invalidDiscriminator_isUnknownDiscriminator() {
        assertPayloadFailure(() -> MessagePayloads.deserialize("{\"__type\":\"OrderPlaced\"}", JSON, REGISTRY),
                             Cause.UNKNOWN_DISCRIMINATOR, DiscriminatorException.class,
                             "The message body's type cannot be resolved from the type registry");
    }

    @ParameterizedTest
    @MethodSource("codecs")
    void deserializeWithRegistry_unregisteredDiscriminator_isUnknownDiscriminator(Codec codec) {
        String body = MessagePayloads.serialize(PLACED, codec);
        TypeRegistry other = TypeRegistry.builder()
                                         .putType(OrderCancelled.ID, OrderCancelled.class, OrderCancelled::builder)
                                         .build();

        assertPayloadFailure(() -> MessagePayloads.deserialize(body, codec, other), Cause.UNKNOWN_DISCRIMINATOR,
                             UnsupportedOperationException.class,
                             "No shape registered for document discriminator: smithy.example#OrderPlaced");
    }

    @ParameterizedTest
    @MethodSource("codecs")
    void deserializeWithRegistry_union_isUnknownDiscriminatorBecauseUnionsHaveNoType(Codec codec) {
        String body = MessagePayloads.serialize(new Notice("hi"), codec);

        assertPayloadFailure(() -> MessagePayloads.deserialize(body, codec, REGISTRY), Cause.UNKNOWN_DISCRIMINATOR,
                             DiscriminatorException.class, "has no discriminator");
    }

    @Test
    void deserializeWithRegistry_jsonNull_isUnknownDiscriminator() {
        assertPayloadFailure(() -> MessagePayloads.deserialize("null", JSON, REGISTRY), Cause.UNKNOWN_DISCRIMINATOR,
                             null, "it holds no document");
    }

    @Test
    void deserializeWithRegistry_xml_isUnknownDiscriminatorBecauseXmlHasNoDocuments() {
        XmlCodec xml = XmlCodec.builder().build();
        String body = MessagePayloads.serialize(PLACED, xml);

        assertPayloadFailure(() -> MessagePayloads.deserialize(body, xml, REGISTRY), Cause.UNKNOWN_DISCRIMINATOR,
                             null, "it holds no document");
    }

    @ParameterizedTest
    @MethodSource("codecs")
    void deserializeWithShapeId_unregistered_isUnknownDiscriminatorAndBodyIsNotRead(Codec codec) {
        ShapeId unknown = ShapeId.fromParts(PayloadTestShapes.NS, "Unknown");

        assertPayloadFailure(() -> MessagePayloads.deserialize("not even a body", codec, REGISTRY, unknown),
                             Cause.UNKNOWN_DISCRIMINATOR, null,
                             "No shape registered for ShapeId: smithy.example#Unknown");
    }

    // Null arguments

    static Stream<Arguments> nullArguments() {
        String body = "{}";
        OrderPlaced.Builder noBuilder = null;
        TypeRegistry noRegistry = null;
        ShapeId id = OrderPlaced.ID;
        return Stream.of(
            nullArgument("value", () -> MessagePayloads.serialize(null, JSON)),
            nullArgument("codec", () -> MessagePayloads.serialize(PLACED, null)),
            nullArgument("body", () -> MessagePayloads.deserialize(null, JSON, OrderPlaced.builder())),
            nullArgument("codec", () -> MessagePayloads.deserialize(body, null, OrderPlaced.builder())),
            nullArgument("builder", () -> MessagePayloads.deserialize(body, JSON, noBuilder)),
            nullArgument("body", () -> MessagePayloads.deserialize(null, JSON, REGISTRY)),
            nullArgument("codec", () -> MessagePayloads.deserialize(body, null, REGISTRY)),
            nullArgument("registry", () -> MessagePayloads.deserialize(body, JSON, noRegistry)),
            nullArgument("body", () -> MessagePayloads.deserialize(null, JSON, REGISTRY, id)),
            nullArgument("codec", () -> MessagePayloads.deserialize(body, null, REGISTRY, id)),
            nullArgument("registry", () -> MessagePayloads.deserialize(body, JSON, null, id)),
            nullArgument("shapeId", () -> MessagePayloads.deserialize(body, JSON, REGISTRY, null)));
    }

    private static Arguments nullArgument(String parameter, Executable call) {
        return Arguments.of(parameter, call);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nullArguments")
    void methods_nullArgument_throwNullPointerExceptionNamingIt(String parameter, Executable call) {
        assertThatThrownBy(call::execute).isExactlyInstanceOf(NullPointerException.class)
                                         .hasMessage(parameter + " must not be null.");
    }

    // MessagePayloadException

    @Test
    void exception_builder_keepsCauseMessageAndThrowable() {
        IllegalStateException throwable = new IllegalStateException("inner");
        MessagePayloadException e = MessagePayloadException.builder()
                                                           .cause(Cause.NON_CONFORMING)
                                                           .message("msg")
                                                           .cause(throwable)
                                                           .build();

        assertThat(e).isInstanceOf(SdkClientException.class).hasMessage("msg").hasCause(throwable);
        assertThat(e.cause()).isEqualTo(Cause.NON_CONFORMING);
        assertThat(e.retryable()).isFalse();
        MessagePayloadException copy = e.toBuilder().build();
        assertThat(copy.cause()).isEqualTo(Cause.NON_CONFORMING);
        assertThat(copy).hasMessage("msg").hasCause(throwable);
    }

    @Test
    void exception_builderWithoutCause_throwsNullPointerException() {
        assertThatThrownBy(() -> MessagePayloadException.builder().message("msg").build())
            .isExactlyInstanceOf(NullPointerException.class)
            .hasMessage("cause must not be null.");
    }

    private static void assertPayloadFailure(ThrowingCallable call, Cause cause, Class<? extends Throwable> causeType,
                                             String messagePart) {
        Throwable thrown = catchThrowable(call);
        assertThat(thrown).isExactlyInstanceOf(MessagePayloadException.class).hasMessageContaining(messagePart);
        assertThat(((MessagePayloadException) thrown).cause()).isEqualTo(cause);
        if (causeType == null) {
            assertThat(thrown.getCause()).isNull();
        } else {
            assertThat(thrown.getCause()).isInstanceOf(causeType);
        }
    }
}
