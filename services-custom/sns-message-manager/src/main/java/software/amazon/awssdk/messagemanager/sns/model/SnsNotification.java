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

package software.amazon.awssdk.messagemanager.sns.model;

import java.net.URI;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.messaging.payload.MessagePayloadException;
import software.amazon.awssdk.messaging.payload.MessagePayloads;
import software.amazon.awssdk.schema.SerializableShape;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.utils.Validate;

/**
 * A 'Notification' message from SNS.
 * <p>
 * See the <a href="https://docs.aws.amazon.com/sns/latest/dg/http-notification-json.html">API reference</a> for more
 * information.
 *
 * <h2>Typed messages</h2>
 * When the publisher wrote the message body with a schema-based {@link Codec}, for example with
 * {@link MessagePayloads#serialize}, the {@code message(Codec, ...)} methods read the body back into a typed value:
 * <pre>{@code
 * SnsMessage parsed = messageManager.parseMessage(requestBody);
 * if (parsed instanceof SnsNotification) {
 *     OrderPlaced order = ((SnsNotification) parsed).message(JsonCodec.builder().build(), OrderPlaced.builder());
 * }
 * }</pre>
 * They decode {@link #message()} with {@link MessagePayloads} and change nothing else: the envelope is parsed and the
 * signature is validated by {@code SnsMessageManager} exactly as before, over the message string. Each call decodes the
 * message again; the value is not cached.
 * <p>
 * These methods need the {@code software.amazon.awssdk:message-payloads} artifact on the classpath. The SNS Message
 * Manager declares it as an optional dependency, so add it to your build to use them. Without it, the methods throw
 * {@link NoClassDefFoundError}; everything else in this class works as before.
 */
@SdkPublicApi
public final class SnsNotification extends SnsMessage {
    private final String subject;
    private final URI unsubscribeUrl;

    SnsNotification(BuilderImpl builder) {
        super(builder);
        this.subject = builder.subject;
        this.unsubscribeUrl = builder.unsubscribeUrl;
    }

    @Override
    public SnsMessageType type() {
        return SnsMessageType.NOTIFICATION;
    }

    /**
     * The subject of the message. This may be {@code null}.
     */
    public String subject() {
        return subject;
    }

    /**
     * The URL that can be visited to unsubscribe from the topic.
     */
    public URI unsubscribeUrl() {
        return unsubscribeUrl;
    }

    /**
     * Reads the message body into the shape of a builder, with
     * {@link MessagePayloads#deserialize(String, Codec, ShapeBuilder)}. A {@code __type} member in the body is ignored.
     *
     * @param codec the codec the message body was written with
     * @param builder a new builder of the target shape
     * @param <T> the shape type
     * @return the value, or {@code null} if this notification has no message
     * @throws NullPointerException if {@code codec} or {@code builder} is null
     * @throws MessagePayloadException with {@link MessagePayloadException.Cause#MALFORMED} or
     *     {@link MessagePayloadException.Cause#NON_CONFORMING} if the message cannot be read into the shape
     */
    public <T extends SerializableShape> T message(Codec codec, ShapeBuilder<T> builder) {
        Validate.paramNotNull(codec, "codec");
        Validate.paramNotNull(builder, "builder");
        String body = message();
        return body == null ? null : MessagePayloads.deserialize(body, codec, builder);
    }

    /**
     * Reads the message body into the registered shape named by the body's {@code __type} discriminator, with
     * {@link MessagePayloads#deserialize(String, Codec, TypeRegistry)}.
     *
     * @param codec the codec the message body was written with
     * @param registry the shapes the message may hold
     * @return the value, or {@code null} if this notification has no message
     * @throws NullPointerException if {@code codec} or {@code registry} is null
     * @throws MessagePayloadException with {@link MessagePayloadException.Cause#MALFORMED} if the message is not
     *     well-formed, {@link MessagePayloadException.Cause#UNKNOWN_DISCRIMINATOR} if it has no valid {@code __type} or
     *     names an unregistered shape, and {@link MessagePayloadException.Cause#NON_CONFORMING} if it does not match the
     *     named shape
     */
    public SerializableStruct message(Codec codec, TypeRegistry registry) {
        Validate.paramNotNull(codec, "codec");
        Validate.paramNotNull(registry, "registry");
        String body = message();
        return body == null ? null : MessagePayloads.deserialize(body, codec, registry);
    }

    /**
     * Reads the message body into the registered shape with a ShapeId the caller supplies, with
     * {@link MessagePayloads#deserialize(String, Codec, TypeRegistry, ShapeId)}. A {@code __type} member in the body is
     * ignored.
     *
     * @param codec the codec the message body was written with
     * @param registry the shapes the message may hold
     * @param shapeId the ShapeId of the shape the message holds
     * @return the value, or {@code null} if this notification has no message
     * @throws NullPointerException if {@code codec}, {@code registry} or {@code shapeId} is null
     * @throws MessagePayloadException with {@link MessagePayloadException.Cause#UNKNOWN_DISCRIMINATOR} if
     *     {@code shapeId} is not registered, or with {@link MessagePayloadException.Cause#MALFORMED} or
     *     {@link MessagePayloadException.Cause#NON_CONFORMING} if the message cannot be read into the shape
     */
    public SerializableStruct message(Codec codec, TypeRegistry registry, ShapeId shapeId) {
        Validate.paramNotNull(codec, "codec");
        Validate.paramNotNull(registry, "registry");
        Validate.paramNotNull(shapeId, "shapeId");
        String body = message();
        return body == null ? null : MessagePayloads.deserialize(body, codec, registry, shapeId);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        SnsNotification that = (SnsNotification) o;
        return Objects.equals(subject, that.subject)
               && Objects.equals(unsubscribeUrl, that.unsubscribeUrl);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(subject);
        result = 31 * result + Objects.hashCode(unsubscribeUrl);
        return result;
    }

    @Override
    public String toString() {
        return toStringBuilder("SnsNotification")
            .add("Subject", subject())
            .add("UnsubscribeUrl", unsubscribeUrl())
            .build();
    }

    public interface Builder extends SnsMessage.Builder<Builder> {

        /**
         * The subject of the message. This may be {@code null}.
         */
        Builder subject(String subject);

        /**
         * The URL that can be visited to unsubscribe from the topic.
         */
        Builder unsubscribeUrl(URI unsubscribeUrl);

        SnsNotification build();
    }

    private static class BuilderImpl extends SnsMessage.BuilderImpl<Builder> implements Builder {
        private String subject;
        private URI unsubscribeUrl;

        @Override
        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        @Override
        public Builder unsubscribeUrl(URI unsubscribeUrl) {
            this.unsubscribeUrl = unsubscribeUrl;
            return this;
        }

        @Override
        public SnsNotification build() {
            return new SnsNotification(this);
        }
    }
}
