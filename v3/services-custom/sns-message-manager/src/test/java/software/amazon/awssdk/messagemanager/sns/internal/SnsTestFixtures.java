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
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic SNS signing fixtures: the checked-in test-only key and certificates of
 * {@code src/test/fixtures/generate-signing-fixtures.sh}, a fixed clock inside their validity windows, and an
 * independent oracle of the SNS canonical string (written from the SNS developer guide, not from
 * {@link SignatureValidator}) that signs JSON messages the way SNS does. Nothing touches the network.
 */
final class SnsTestFixtures {
    /** Inside the validity of every fixture certificate except {@code expired} and {@code not-yet-valid}. */
    static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);

    static final String REGION_CERT_HOST = "sns.us-west-2.amazonaws.com";
    static final String CERT_URL = "https://" + REGION_CERT_HOST
                                   + "/SimpleNotificationService-0000000000000000000000000000000f.pem";
    static final String TOPIC_ARN = "arn:aws:sns:us-west-2:123456789012:MyTopic";

    private static final String ROOT = "/software/amazon/awssdk/messagemanager/sns/fixtures/";
    private static final char[] STORE_PASSWORD = "test-only".toCharArray();

    private static final List<String> NOTIFICATION_FIELDS =
        List.of("Message", "MessageId", "Subject", "Timestamp", "TopicArn", "Type");
    private static final List<String> SUBSCRIPTION_FIELDS =
        List.of("Message", "MessageId", "SubscribeURL", "Timestamp", "Token", "TopicArn", "Type");

    private static final KeyStore.PrivateKeyEntry SIGNING_ENTRY = loadSigningEntry();

    private SnsTestFixtures() {
    }

    /** The PEM bytes of fixture certificate {@code name} (without {@code .pem}). */
    static byte[] certificate(String name) {
        try (InputStream in = SnsTestFixtures.class.getResourceAsStream(ROOT + name + ".pem")) {
            if (in == null) {
                throw new IllegalArgumentException("No fixture certificate " + name);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static PublicKey publicKey() {
        return SIGNING_ENTRY.getCertificate().getPublicKey();
    }

    /** A Notification's fields; {@code subject} may be {@code null} (then absent). */
    static Map<String, String> notification(String message, String subject, String timestamp) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Type", "Notification");
        fields.put("MessageId", "22b80b92-fdea-4c2c-8f9d-bdfb0c7bf324");
        fields.put("TopicArn", TOPIC_ARN);
        if (subject != null) {
            fields.put("Subject", subject);
        }
        fields.put("Message", message);
        fields.put("Timestamp", timestamp);
        fields.put("UnsubscribeURL", "https://" + REGION_CERT_HOST + "/?Action=Unsubscribe&SubscriptionArn="
                                     + TOPIC_ARN + ":c9135db0-26c4-47ec-8998-413945fb5a96");
        return fields;
    }

    /** A SubscriptionConfirmation ({@code type} may also be {@code UnsubscribeConfirmation}) carrying {@code token}. */
    static Map<String, String> subscription(String type, String token) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Type", type);
        fields.put("MessageId", "165545c9-2a5c-472c-8df2-7ff2be2b3b1b");
        fields.put("Token", token);
        fields.put("TopicArn", TOPIC_ARN);
        fields.put("Message", "You have chosen to subscribe to the topic " + TOPIC_ARN
                              + ".\nTo confirm the subscription, visit the SubscribeURL included in this message.");
        fields.put("SubscribeURL", "https://" + REGION_CERT_HOST + "/?Action=ConfirmSubscription&TopicArn="
                                   + TOPIC_ARN + "&Token=" + token);
        fields.put("Timestamp", "2024-06-15T12:30:45.123Z");
        return fields;
    }

    /**
     * {@code fields} signed with the fixture key: adds SignatureVersion, Signature (over the canonical string of the
     * fields as given) and SigningCertURL.
     */
    static Map<String, String> signed(Map<String, String> fields, String signatureVersion, String certUrl) {
        Map<String, String> result = new LinkedHashMap<>(fields);
        result.put("SignatureVersion", signatureVersion);
        result.put("Signature", sign(canonical(fields), signatureVersion));
        result.put("SigningCertURL", certUrl);
        return result;
    }

    static Map<String, String> signed(Map<String, String> fields) {
        return signed(fields, "2", CERT_URL);
    }

    /**
     * The SNS canonical string: "name\nvalue\n" for each signed field present, in the documented order (a
     * Notification signs Message, MessageId, Subject if present, Timestamp, TopicArn, Type; a Subscription- or
     * UnsubscribeConfirmation signs Message, MessageId, SubscribeURL, Timestamp, Token, TopicArn, Type).
     */
    static String canonical(Map<String, String> fields) {
        List<String> names = "Notification".equals(fields.get("Type")) ? NOTIFICATION_FIELDS : SUBSCRIPTION_FIELDS;
        StringBuilder result = new StringBuilder();
        for (String name : names) {
            if (fields.containsKey(name)) {
                result.append(name).append('\n').append(fields.get(name)).append('\n');
            }
        }
        return result.toString();
    }

    static String sign(String canonical, String signatureVersion) {
        try {
            Signature signature = Signature.getInstance("1".equals(signatureVersion) ? "SHA1withRSA"
                                                                                       : "SHA256withRSA");
            signature.initSign(signingKey());
            signature.update(canonical.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A JSON object of string fields, in order. */
    static String json(Map<String, String> fields) {
        StringBuilder result = new StringBuilder("{");
        fields.forEach((name, value) -> {
            if (result.length() > 1) {
                result.append(',');
            }
            result.append(quote(name)).append(':').append(quote(value));
        });
        return result.append('}').toString();
    }

    private static String quote(String value) {
        StringBuilder result = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (c < 0x20) {
                        result.append(String.format("\\u%04x", (int) c));
                    } else {
                        result.append(c);
                    }
                }
            }
        }
        return result.append('"').toString();
    }

    private static PrivateKey signingKey() {
        return SIGNING_ENTRY.getPrivateKey();
    }

    private static KeyStore.PrivateKeyEntry loadSigningEntry() {
        try (InputStream in = SnsTestFixtures.class.getResourceAsStream(ROOT + "signing-key.p12")) {
            KeyStore store = KeyStore.getInstance("PKCS12");
            store.load(in, STORE_PASSWORD);
            return (KeyStore.PrivateKeyEntry) store.getEntry("signing", new KeyStore.PasswordProtection(STORE_PASSWORD));
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("Cannot load the test signing key", e);
        }
    }
}
