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

package software.amazon.awssdk.messagemanager.sns.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.messagemanager.sns.internal.TypedMessageTestShapes.REGISTRY;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.codecs.json.JsonCodec;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.messagemanager.sns.SnsMessageManager;
import software.amazon.awssdk.messagemanager.sns.internal.TypedMessageTestShapes.OrderCancelled;
import software.amazon.awssdk.messagemanager.sns.internal.TypedMessageTestShapes.OrderPlaced;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessage;
import software.amazon.awssdk.messagemanager.sns.model.SnsNotification;
import software.amazon.awssdk.messaging.payload.MessagePayloadException;
import software.amazon.awssdk.messaging.payload.MessagePayloadException.Cause;
import software.amazon.awssdk.messaging.payload.MessagePayloads;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.schema.serde.document.DiscriminatorException;

/**
 * The {@link SnsNotification} typed-message methods, on notifications that {@link SnsMessageManager} parses and
 * validates. Each notification is signed with a generated key that the stubbed certificate retriever returns.
 */
class SnsNotificationTypedMessageTest {
    private static final JsonCodec JSON = JsonCodec.builder().build();
    private static final OrderPlaced PLACED = new OrderPlaced("o-1", 3);
    private static final OrderCancelled CANCELLED = new OrderCancelled("o-2", "out of stock");
    private static final String PLACED_WITHOUT_TYPE = "{\"orderId\":\"o-1\",\"quantity\":3}";

    private static KeyPair signingKeyPair;
    private static SnsMessageManager messageManager;

    @BeforeAll
    static void setup() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        signingKeyPair = keyPairGenerator.generateKeyPair();

        CertificateRetriever certificateRetriever = mock(CertificateRetriever.class);
        when(certificateRetriever.retrieveCertificate(any())).thenReturn(signingKeyPair.getPublic());
        // An HTTP client is configured so that the manager does not load the default one: the HTTP service loaded
        // first is cached for the JVM, which DefaultSnsMessageManagerTest relies on choosing itself.
        messageManager = new DefaultSnsMessageManager.BuilderImpl()
            .certificateRetriever(certificateRetriever)
            .httpClient(mock(SdkHttpClient.class))
            .region(Region.US_WEST_2)
            .build();
    }

    @AfterAll
    static void tearDown() {
        messageManager.close();
    }

    @Test
    void message_bodyWithType_messageStringUnchanged() throws Exception {
        String body = MessagePayloads.serialize(PLACED, JSON);
        assertThat(body).startsWith("{\"__type\":\"smithy.example#OrderPlaced\"");

        SnsNotification notification = parseSigned(body);

        assertThat(notification.message()).isEqualTo(body);
    }

    @Test
    void messageWithBuilder_bodyWithType_returnsValue() throws Exception {
        SnsNotification notification = parseSigned(MessagePayloads.serialize(PLACED, JSON));

        OrderPlaced order = notification.message(JSON, OrderPlaced.builder());

        assertThat(order).isEqualTo(PLACED);
    }

    @Test
    void messageWithBuilder_bodyWithoutType_returnsValue() throws Exception {
        SnsNotification notification = parseSigned(PLACED_WITHOUT_TYPE);

        assertThat(notification.message(JSON, OrderPlaced.builder())).isEqualTo(PLACED);
    }

    @Test
    void messageWithRegistry_bodiesWithType_returnRegisteredShapes() throws Exception {
        SnsNotification placed = parseSigned(MessagePayloads.serialize(PLACED, JSON));
        SnsNotification cancelled = parseSigned(MessagePayloads.serialize(CANCELLED, JSON));

        assertThat(placed.message(JSON, REGISTRY)).isEqualTo(PLACED);
        assertThat(cancelled.message(JSON, REGISTRY)).isEqualTo(CANCELLED);
    }

    @Test
    void messageWithRegistryAndShapeId_bodyWithoutType_returnsValue() throws Exception {
        SnsNotification notification = parseSigned(PLACED_WITHOUT_TYPE);

        assertThat(notification.message(JSON, REGISTRY, OrderPlaced.ID)).isEqualTo(PLACED);
    }

    @Test
    void messageWithRegistryAndShapeId_bodyWithOtherType_usesCallerShapeId() throws Exception {
        String body = "{\"__type\":\"smithy.example#OrderCancelled\",\"orderId\":\"o-1\",\"quantity\":3}";
        SnsNotification notification = parseSigned(body);

        assertThat(notification.message(JSON, REGISTRY, OrderPlaced.ID)).isEqualTo(PLACED);
    }

    static Stream<Arguments> failures() {
        Function<SnsNotification, Object> builder = n -> n.message(JSON, OrderPlaced.builder());
        Function<SnsNotification, Object> registry = n -> n.message(JSON, REGISTRY);
        Function<SnsNotification, Object> shapeId = n -> n.message(JSON, REGISTRY, OrderPlaced.ID);
        Function<SnsNotification, Object> unregisteredShapeId =
            n -> n.message(JSON, REGISTRY, ShapeId.fromParts("smithy.example", "Unknown"));
        String truncated = "{\"orderId\":\"o-1\",";
        String wrongType = "{\"orderId\":\"o-1\",\"quantity\":\"three\"}";
        String wrongTypeWithType = "{\"__type\":\"smithy.example#OrderPlaced\","
                                   + "\"orderId\":\"o-1\",\"quantity\":\"three\"}";
        String unknownType = "{\"__type\":\"smithy.example#Unknown\",\"orderId\":\"o-1\"}";
        return Stream.of(
            Arguments.of("malformed, builder", truncated, builder, Cause.MALFORMED, SerializationException.class),
            Arguments.of("malformed, registry", truncated, registry, Cause.MALFORMED, SerializationException.class),
            Arguments.of("malformed, ShapeId", truncated, shapeId, Cause.MALFORMED, SerializationException.class),
            Arguments.of("non-conforming, builder", wrongType, builder, Cause.NON_CONFORMING,
                         SerializationException.class),
            Arguments.of("non-conforming, registry", wrongTypeWithType, registry, Cause.NON_CONFORMING,
                         SerializationException.class),
            Arguments.of("non-conforming, ShapeId", wrongType, shapeId, Cause.NON_CONFORMING,
                         SerializationException.class),
            Arguments.of("unregistered __type", unknownType, registry, Cause.UNKNOWN_DISCRIMINATOR,
                         UnsupportedOperationException.class),
            Arguments.of("no __type", PLACED_WITHOUT_TYPE, registry, Cause.UNKNOWN_DISCRIMINATOR,
                         DiscriminatorException.class),
            Arguments.of("unregistered ShapeId", PLACED_WITHOUT_TYPE, unregisteredShapeId, Cause.UNKNOWN_DISCRIMINATOR,
                         null));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("failures")
    void message_bodyCannotBeRead_throwsMessagePayloadException(String name, String body,
                                                               Function<SnsNotification, Object> read,
                                                               Cause cause, Class<? extends Throwable> causeType)
        throws Exception {
        SnsNotification notification = parseSigned(body);

        Throwable thrown = catchThrowable(() -> read.apply(notification));

        assertThat(thrown).isInstanceOf(MessagePayloadException.class);
        assertThat(((MessagePayloadException) thrown).cause()).isEqualTo(cause);
        if (causeType == null) {
            assertThat(thrown.getCause()).isNull();
        } else {
            assertThat(thrown.getCause()).isInstanceOf(causeType);
        }
    }

    @Test
    void message_noMessage_returnsNull() {
        SnsNotification notification = SnsNotification.builder().messageId("message-id").build();

        assertThat(notification.message()).isNull();
        assertThat(notification.message(JSON, OrderPlaced.builder())).isNull();
        assertThat(notification.message(JSON, REGISTRY)).isNull();
        assertThat(notification.message(JSON, REGISTRY, OrderPlaced.ID)).isNull();
    }

    @Test
    void message_nullArguments_throwNullPointerException() {
        SnsNotification notification = SnsNotification.builder().build();

        assertThatThrownBy(() -> notification.message(null, OrderPlaced.builder()))
            .isInstanceOf(NullPointerException.class).hasMessageContaining("codec");
        assertThatThrownBy(() -> notification.message(JSON, (OrderPlaced.Builder) null))
            .isInstanceOf(NullPointerException.class).hasMessageContaining("builder");
        assertThatThrownBy(() -> notification.message(JSON, (TypeRegistry) null))
            .isInstanceOf(NullPointerException.class).hasMessageContaining("registry");
        assertThatThrownBy(() -> notification.message(null, REGISTRY))
            .isInstanceOf(NullPointerException.class).hasMessageContaining("codec");
        assertThatThrownBy(() -> notification.message(JSON, REGISTRY, null))
            .isInstanceOf(NullPointerException.class).hasMessageContaining("shapeId");
    }

    /**
     * Builds a notification envelope around {@code message}, signs it with signature version 2 (SHA256withRSA), and
     * parses and validates it with the message manager.
     */
    private static SnsNotification parseSigned(String message) throws Exception {
        String messageId = "11111111-2222-3333-4444-555555555555";
        String timestamp = "2024-06-15T12:30:45.123Z";
        String topicArn = "arn:aws:sns:us-west-2:123456789012:orders";
        String canonicalMessage = String.join("\n",
                                              "Message", message,
                                              "MessageId", messageId,
                                              "Timestamp", timestamp,
                                              "TopicArn", topicArn,
                                              "Type", "Notification") + "\n";
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(signingKeyPair.getPrivate());
        signer.update(canonicalMessage.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(signer.sign());

        String envelope = "{\n"
                          + "  \"Type\" : \"Notification\",\n"
                          + "  \"MessageId\" : \"" + messageId + "\",\n"
                          + "  \"TopicArn\" : \"" + topicArn + "\",\n"
                          + "  \"Message\" : " + jsonString(message) + ",\n"
                          + "  \"Timestamp\" : \"" + timestamp + "\",\n"
                          + "  \"SignatureVersion\" : \"2\",\n"
                          + "  \"Signature\" : \"" + signature + "\",\n"
                          + "  \"SigningCertURL\" : \"https://sns.us-west-2.amazonaws.com/"
                          + "SimpleNotificationService-0000000000000000000000.pem\",\n"
                          + "  \"UnsubscribeURL\" : \"https://sns.us-west-2.amazonaws.com/?Action=Unsubscribe\"\n"
                          + "}";

        SnsMessage parsed = messageManager.parseMessage(envelope);
        assertThat(parsed).isInstanceOf(SnsNotification.class);
        return (SnsNotification) parsed;
    }

    private static String jsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
