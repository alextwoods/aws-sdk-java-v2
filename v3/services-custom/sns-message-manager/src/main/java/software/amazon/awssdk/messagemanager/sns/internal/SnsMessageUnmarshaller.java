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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.messagemanager.sns.model.SignatureVersion;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessage;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessageType;
import software.amazon.awssdk.messagemanager.sns.model.SnsNotification;
import software.amazon.awssdk.messagemanager.sns.model.SnsSubscriptionConfirmation;
import software.amazon.awssdk.messagemanager.sns.model.SnsUnsubscribeConfirmation;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * Parses the JSON of an SNS HTTP/S message (the Smithy Java JSON codec, read as a {@link Document}) into its message
 * type. Thread safe.
 *
 * <p>Diagnostics never carry message content (Requirement 21.10): parse failures name the field, not its value, and
 * do not chain causes whose messages would echo the input (a malformed subscribe URL contains the subscription token).
 */
public final class SnsMessageUnmarshaller {
    private static final String MESSAGE_FIELD = "Message";
    private static final String MESSAGE_ID_FIELD = "MessageId";
    private static final String SIGNATURE_FIELD = "Signature";
    private static final String SIGNATURE_VERSION_FIELD = "SignatureVersion";
    private static final String SIGNING_CERT_URL = "SigningCertURL";
    private static final String SUBJECT_FIELD = "Subject";
    private static final String SUBSCRIBE_URL = "SubscribeURL";
    private static final String TOKEN_FIELD = "Token";
    private static final String TOPIC_ARN_FIELD = "TopicArn";
    private static final String TIMESTAMP_FIELD = "Timestamp";
    private static final String TYPE_FIELD = "Type";
    private static final String UNSUBSCRIBE_URL_FIELD = "UnsubscribeURL";

    private static final JsonCodec CODEC = JsonCodec.builder().build();

    public SnsMessage unmarshall(InputStream stream) {
        Map<String, Document> node = parseObject(stream);

        Optional<String> type = stringMember(node, TYPE_FIELD);

        if (!type.isPresent()) {
            throw SdkClientException.create("'Type' field must be present");
        }

        SnsMessageType snsMessageType = SnsMessageType.fromValue(type.get());
        switch (snsMessageType) {
            case NOTIFICATION: {
                SnsNotification.Builder builder = SnsNotification.builder();
                unmarshallNotification(node, builder);
                return builder.build();
            }
            case SUBSCRIPTION_CONFIRMATION: {
                SnsSubscriptionConfirmation.Builder builder = SnsSubscriptionConfirmation.builder();
                unmarshallSubscriptionConfirmation(node, builder);
                return builder.build();
            }
            case UNSUBSCRIBE_CONFIRMATION: {
                SnsUnsubscribeConfirmation.Builder builder = SnsUnsubscribeConfirmation.builder();
                unmarshallUnsubscribeNotification(node, builder);
                return builder.build();
            }
            default:
                throw SdkClientException.create("Unsupported sns message type: " + snsMessageType);
        }
    }

    private static Map<String, Document> parseObject(InputStream stream) {
        byte[] bytes;
        try {
            bytes = stream.readAllBytes();
        } catch (IOException e) {
            throw SdkClientException.create("Unable to read the SNS message", e);
        }
        Document document;
        try {
            document = CODEC.createDeserializer(bytes).readDocument();
        } catch (RuntimeException e) {
            // The parser's message can quote the input, so it is not chained.
            throw SdkClientException.create("Unable to parse the SNS message: it is not well-formed JSON");
        }
        if (document == null || !document.isType(ShapeType.MAP)) {
            throw SdkClientException.create("Expected an JSON object");
        }
        return document.asStringMap();
    }

    private static Optional<String> stringMember(Map<String, Document> obj, String fieldName) {
        if (!obj.containsKey(fieldName)) {
            return Optional.empty();
        }

        Document node = obj.get(fieldName);
        if (node == null || !node.isType(ShapeType.STRING)) {
            String msg = String.format("Expected field '%s' to be a string", fieldName);
            throw SdkClientException.create(msg);
        }

        return Optional.of(node.asString());
    }

    private static void unmarshallNotification(Map<String, Document> node, SnsNotification.Builder builder) {
        stringMember(node, SUBJECT_FIELD).ifPresent(builder::subject);
        stringMember(node, UNSUBSCRIBE_URL_FIELD)
            .map(SnsMessageUnmarshaller::toUri)
            .ifPresent(builder::unsubscribeUrl);

        unmarshallCommon(node, builder);
    }

    // https://docs.aws.amazon.com/sns/latest/dg/http-subscription-confirmation-json.html
    private static void unmarshallSubscriptionConfirmation(Map<String, Document> node,
                                                           SnsSubscriptionConfirmation.Builder builder) {
        stringMember(node, TOKEN_FIELD).ifPresent(builder::token);
        stringMember(node, SUBSCRIBE_URL).map(SnsMessageUnmarshaller::toUri).ifPresent(builder::subscribeUrl);
        unmarshallCommon(node, builder);
    }

    // https://docs.aws.amazon.com/sns/latest/dg/http-unsubscribe-confirmation-json.html
    private static void unmarshallUnsubscribeNotification(Map<String, Document> node,
                                                          SnsUnsubscribeConfirmation.Builder builder) {
        stringMember(node, TOKEN_FIELD).ifPresent(builder::token);
        stringMember(node, SUBSCRIBE_URL).map(SnsMessageUnmarshaller::toUri).ifPresent(builder::subscribeUrl);
        unmarshallCommon(node, builder);
    }

    private static void unmarshallCommon(Map<String, Document> node, SnsMessage.Builder<?> builder) {
        stringMember(node, MESSAGE_ID_FIELD).ifPresent(builder::messageId);
        stringMember(node, MESSAGE_FIELD).ifPresent(builder::message);
        stringMember(node, TOPIC_ARN_FIELD).ifPresent(builder::topicArn);

        Optional<String> timestamp = stringMember(node, TIMESTAMP_FIELD);
        if (timestamp.isPresent()) {
            try {
                builder.timestamp(Instant.parse(timestamp.get()));
            } catch (DateTimeParseException e) {
                // Chained as in V2: the cause quotes only the timestamp.
                throw SdkClientException.create("Unable to parse timestamp", e);
            }
        }

        Optional<String> signatureField = stringMember(node, SIGNATURE_FIELD);
        if (signatureField.isPresent()) {
            try {
                byte[] decoded = Base64.getDecoder().decode(signatureField.get());
                builder.signature(SdkBytes.fromByteArray(decoded));
            } catch (IllegalArgumentException e) {
                // Chained as in V2: the cause names at most one character of the (public) signature.
                throw SdkClientException.create("Unable to decode signature", e);
            }
        }

        stringMember(node, SIGNATURE_VERSION_FIELD)
            .map(SignatureVersion::fromValue)
            .ifPresent(builder::signatureVersion);

        stringMember(node, SIGNING_CERT_URL)
            .map(SnsMessageUnmarshaller::toUri)
            .ifPresent(builder::signingCertUrl);
    }

    private static URI toUri(String s) {
        try {
            return new URI(s);
        } catch (URISyntaxException e) {
            // Not chained: the cause quotes the whole URI, and a subscribe URL carries the subscription token.
            throw SdkClientException.create("Unable to parse URI");
        }
    }
}
