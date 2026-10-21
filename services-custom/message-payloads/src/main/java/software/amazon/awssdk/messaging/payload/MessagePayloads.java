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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codecs.cbor.Rpcv2CborCodec;
import software.amazon.awssdk.codecs.commons.Codec;
import software.amazon.awssdk.codecs.json.JsonCodec;
import software.amazon.awssdk.messaging.payload.MessagePayloadException.Cause;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableShape;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.serde.SerializationException;
import software.amazon.awssdk.schema.serde.ShapeDeserializer;
import software.amazon.awssdk.schema.serde.TypeRegistry;
import software.amazon.awssdk.schema.serde.document.DiscriminatorException;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.utils.Validate;

/**
 * Serializes typed values into SQS and SNS message bodies, and reads them back, with any schema-based {@link Codec}.
 *
 * <pre>{@code
 * Codec codec = JsonCodec.builder().build();
 * String body = MessagePayloads.serialize(orderPlaced, codec);
 * sqs.sendMessage(r -> r.queueUrl(queueUrl).messageBody(body));
 *
 * // One known type
 * OrderPlaced order = MessagePayloads.deserialize(message.body(), codec, OrderPlaced.builder());
 * // Several types, told apart by the body's __type member
 * SerializableStruct event = MessagePayloads.deserialize(message.body(), codec, registry);
 * }</pre>
 *
 * <h2>Text and binary codecs</h2>
 * Message bodies are text. The SDK CBOR codec ({@link Rpcv2CborCodec}) is binary, so its bytes are written as standard
 * base64 ({@link Base64#getEncoder()}: RFC 4648 alphabet, with padding, no line breaks) and read back the same way.
 * Every other codec, such as the JSON and XML codecs, is treated as a text codec: its output is decoded as UTF-8, and
 * the body is encoded as UTF-8 before it is read.
 *
 * <h2>The {@code __type} discriminator</h2>
 * {@link #serialize} writes a structure as a structure document, so a codec that writes discriminators in documents
 * adds the structure's absolute ShapeId as a {@code __type} member. The JSON codec does this when
 * {@code serializeTypeInDocuments} is on (the default and the JSON presets), and the CBOR codec always does. Other
 * codecs, and unions, are written as the plain shape with no {@code __type}. Reading a body into a shape ignores a
 * {@code __type} member, so every body can be read with a {@link ShapeBuilder}, and
 * {@link #deserialize(String, Codec, TypeRegistry)} can read the bodies that have one.
 *
 * <h2>Errors</h2>
 * The deserialize methods throw {@link MessagePayloadException}, with the exception of the codec or registry as its
 * cause, and return no value:
 * <ul>
 *     <li>{@link Cause#MALFORMED}: the body is not well-formed for the codec, or is not valid base64 for a binary
 *     codec.</li>
 *     <li>{@link Cause#NON_CONFORMING}: the body is well-formed but does not match the target schema. The codec
 *     reports both kinds of failure as a {@link SerializationException}; after a failure, the body is parsed again as
 *     a schema-less document, and if that succeeds the body is well-formed. A codec that reads no documents (the XML
 *     codec) cannot be checked this way, so its failures are reported as {@code NON_CONFORMING}.</li>
 *     <li>{@link Cause#UNKNOWN_DISCRIMINATOR}: with a registry, the body has no {@code __type}, its {@code __type} is
 *     not a valid ShapeId, or the ShapeId (read from the body or supplied by the caller) is not registered.</li>
 * </ul>
 * Other exceptions, for example from a builder, are not wrapped.
 *
 * <p>Union bodies have no {@code __type}, so they cannot be read through a registry by discriminator; read them with
 * their builder, or with {@link #deserialize(String, Codec, TypeRegistry, ShapeId)}.
 *
 * <p>Reading into a shape follows the codec's rules. For example, the JSON codec reads an empty body as an empty
 * structure, while {@link #deserialize(String, Codec, TypeRegistry)} reports it as {@link Cause#MALFORMED}.
 *
 * <p>The methods are thread-safe when the codec, builders and registry are.
 */
@SdkPublicApi
public final class MessagePayloads {

    private MessagePayloads() {
    }

    /**
     * Serializes a value into a message body. A text codec's output is returned as UTF-8 text, and a binary codec's
     * output as standard base64. A structure is written with a {@code __type} member when the codec writes
     * discriminators in documents (see the class documentation).
     *
     * @param value the value to serialize, such as a generated model class instance
     * @param codec the codec that writes the value
     * @return the message body
     * @throws NullPointerException if {@code value} or {@code codec} is null
     * @throws SerializationException if the codec cannot serialize the value
     */
    public static String serialize(SerializableShape value, Codec codec) {
        Validate.paramNotNull(value, "value");
        Validate.paramNotNull(codec, "codec");
        SerializableShape shape = writesTypeInDocuments(codec) && isStructure(value) ? Document.of(value) : value;
        if (!isBinary(codec)) {
            return codec.serializeToString(shape);
        }
        ByteBuffer encoded = Base64.getEncoder().encode(codec.serialize(shape));
        return new String(encoded.array(), encoded.arrayOffset() + encoded.position(), encoded.remaining(),
                          StandardCharsets.ISO_8859_1);
    }

    /**
     * Deserializes a message body into the shape of a builder. A {@code __type} member in the body is ignored.
     *
     * @param body the message body
     * @param codec the codec that reads the body
     * @param builder a new builder of the target shape
     * @param <T> the shape type
     * @return the value
     * @throws NullPointerException if {@code body}, {@code codec} or {@code builder} is null
     * @throws MessagePayloadException with {@link Cause#MALFORMED} or {@link Cause#NON_CONFORMING} if the body cannot be
     *     read into the shape
     */
    public static <T extends SerializableShape> T deserialize(String body, Codec codec, ShapeBuilder<T> builder) {
        Validate.paramNotNull(body, "body");
        Validate.paramNotNull(codec, "codec");
        Validate.paramNotNull(builder, "builder");
        return deserializeShape(decode(body, codec), codec, builder);
    }

    /**
     * Deserializes a message body into the registered shape named by the body's {@code __type} discriminator. The body
     * is parsed once into a document, and {@link TypeRegistry#deserialize(Document)} builds the value from it.
     *
     * @param body the message body
     * @param codec the codec that reads the body
     * @param registry the shapes the body may hold
     * @return the value
     * @throws NullPointerException if {@code body}, {@code codec} or {@code registry} is null
     * @throws MessagePayloadException with {@link Cause#MALFORMED} if the body is not well-formed,
     *     {@link Cause#UNKNOWN_DISCRIMINATOR} if the body has no valid {@code __type} or names an unregistered shape,
     *     and {@link Cause#NON_CONFORMING} if the body does not match the named shape
     */
    public static SerializableStruct deserialize(String body, Codec codec, TypeRegistry registry) {
        Validate.paramNotNull(body, "body");
        Validate.paramNotNull(codec, "codec");
        Validate.paramNotNull(registry, "registry");
        Document document;
        try {
            document = readDocument(codec, decode(body, codec));
        } catch (SerializationException e) {
            throw malformed(codec, e);
        }
        if (document == null) {
            throw failure(Cause.UNKNOWN_DISCRIMINATOR, "The message body has no discriminator: it holds no document",
                          null);
        }
        try {
            return registry.deserialize(document);
        } catch (DiscriminatorException | UnsupportedOperationException e) {
            throw failure(Cause.UNKNOWN_DISCRIMINATOR,
                          "The message body's type cannot be resolved from the type registry: " + e.getMessage(), e);
        } catch (SerializationException e) {
            throw nonConforming(document.getDiscriminator().toString(), e);
        }
    }

    /**
     * Deserializes a message body into the registered shape with a ShapeId the caller supplies, for example from an
     * SQS message attribute. A {@code __type} member in the body is ignored.
     *
     * @param body the message body
     * @param codec the codec that reads the body
     * @param registry the shapes the body may hold
     * @param shapeId the ShapeId of the shape the body holds
     * @return the value
     * @throws NullPointerException if {@code body}, {@code codec}, {@code registry} or {@code shapeId} is null
     * @throws MessagePayloadException with {@link Cause#UNKNOWN_DISCRIMINATOR} if {@code shapeId} is not registered,
     *     or with {@link Cause#MALFORMED} or {@link Cause#NON_CONFORMING} if the body cannot be read into the shape
     */
    public static SerializableStruct deserialize(String body, Codec codec, TypeRegistry registry, ShapeId shapeId) {
        Validate.paramNotNull(body, "body");
        Validate.paramNotNull(codec, "codec");
        Validate.paramNotNull(registry, "registry");
        Validate.paramNotNull(shapeId, "shapeId");
        ShapeBuilder<SerializableStruct> builder = registry.createBuilder(shapeId, SerializableStruct.class);
        if (builder == null) {
            throw failure(Cause.UNKNOWN_DISCRIMINATOR, "No shape registered for ShapeId: " + shapeId, null);
        }
        return deserializeShape(decode(body, codec), codec, builder);
    }

    private static <T extends SerializableShape> T deserializeShape(byte[] bytes, Codec codec,
                                                                    ShapeBuilder<T> builder) {
        try {
            return codec.deserializeShape(bytes, builder);
        } catch (SerializationException e) {
            if (!isWellFormed(codec, bytes)) {
                throw malformed(codec, e);
            }
            throw nonConforming("the target shape", e);
        }
    }

    private static byte[] decode(String body, Codec codec) {
        if (!isBinary(codec)) {
            return body.getBytes(StandardCharsets.UTF_8);
        }
        try {
            return Base64.getDecoder().decode(body);
        } catch (IllegalArgumentException e) {
            throw failure(Cause.MALFORMED, "The message body is not valid base64, which the binary codec "
                                           + codecName(codec) + " requires: " + e.getMessage(), e);
        }
    }

    /**
     * Reads the bytes as one schema-less document and closes the deserializer, which rejects content after it.
     */
    private static Document readDocument(Codec codec, byte[] bytes) {
        ShapeDeserializer deserializer = codec.createDeserializer(bytes);
        Document document;
        try {
            document = deserializer.readDocument();
        } catch (RuntimeException e) {
            try {
                deserializer.close();
            } catch (RuntimeException closeFailure) {
                e.addSuppressed(closeFailure);
            }
            throw e;
        }
        deserializer.close();
        return document;
    }

    /**
     * Returns whether the bytes parse as a schema-less document. A failure other than a {@link SerializationException}
     * means the codec cannot be checked this way, and the bytes are treated as well-formed.
     */
    private static boolean isWellFormed(Codec codec, byte[] bytes) {
        try {
            readDocument(codec, bytes);
            return true;
        } catch (SerializationException e) {
            return false;
        } catch (RuntimeException e) {
            return true;
        }
    }

    private static boolean isBinary(Codec codec) {
        return codec instanceof Rpcv2CborCodec;
    }

    private static boolean writesTypeInDocuments(Codec codec) {
        if (codec instanceof JsonCodec) {
            return ((JsonCodec) codec).settings().serializeTypeInDocuments();
        }
        return codec instanceof Rpcv2CborCodec;
    }

    private static boolean isStructure(SerializableShape value) {
        if (!(value instanceof SerializableStruct)) {
            return false;
        }
        Schema schema = ((SerializableStruct) value).sdkSchema();
        return schema != null && schema.type() == ShapeType.STRUCTURE;
    }

    private static String codecName(Codec codec) {
        return codec.getClass().getSimpleName();
    }

    private static MessagePayloadException malformed(Codec codec, SerializationException e) {
        return failure(Cause.MALFORMED,
                       "The message body is not well-formed for " + codecName(codec) + ": " + e.getMessage(), e);
    }

    private static MessagePayloadException nonConforming(String target, SerializationException e) {
        return failure(Cause.NON_CONFORMING, "The message body does not conform to " + target + ": " + e.getMessage(),
                       e);
    }

    private static MessagePayloadException failure(Cause cause, String message, Throwable throwable) {
        return MessagePayloadException.builder().cause(cause).message(message).cause(throwable).build();
    }
}
