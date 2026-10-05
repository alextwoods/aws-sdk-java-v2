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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.messagemanager.sns.model.SignatureVersion;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessage;
import software.amazon.awssdk.messagemanager.sns.model.SnsNotification;

class SignatureValidatorTest {
    private static final SignatureValidator VALIDATOR = new SignatureValidator();
    private static final SnsMessageUnmarshaller UNMARSHALLER = new SnsMessageUnmarshaller();

    static Stream<Arguments> signedMessages() {
        return Stream.of("1", "2").flatMap(version -> Stream.of(
            Arguments.of("notification v" + version, version,
                         SnsTestFixtures.notification("hello world", "a subject", "2024-06-15T12:30:45.123Z")),
            Arguments.of("notification without subject v" + version, version,
                         SnsTestFixtures.notification("{\"k\":\"v\"}\n", null, "2024-06-15T12:30:45.123Z")),
            Arguments.of("subscription confirmation v" + version, version,
                         SnsTestFixtures.subscription("SubscriptionConfirmation", "token-1")),
            Arguments.of("unsubscribe confirmation v" + version, version,
                         SnsTestFixtures.subscription("UnsubscribeConfirmation", "token-2"))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("signedMessages")
    void validateSignature_signedBySns_doesNotThrow(String name, String version, Map<String, String> fields) {
        SnsMessage message = parse(SnsTestFixtures.signed(fields, version, SnsTestFixtures.CERT_URL));

        assertThatCode(() -> VALIDATOR.validateSignature(message, SnsTestFixtures.publicKey())).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("signedMessages")
    void canonicalMessage_matchesTheDocumentedFormat(String name, String version, Map<String, String> fields) {
        SnsMessage message = parse(SnsTestFixtures.signed(fields, version, SnsTestFixtures.CERT_URL));

        assertThat(SignatureValidator.buildCanonicalMessage(message)).isEqualTo(SnsTestFixtures.canonical(fields));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("signedMessages")
    void validateSignature_anySignedFieldChanged_throws(String name, String version, Map<String, String> fields) {
        for (String field : new String[] {"Message", "MessageId", "TopicArn", "Timestamp"}) {
            Map<String, String> tampered = new LinkedHashMap<>(SnsTestFixtures.signed(fields, version,
                                                                                      SnsTestFixtures.CERT_URL));
            tampered.put(field, field.equals("Timestamp") ? "2024-06-15T12:30:45.124Z" : fields.get(field) + "!");
            SnsMessage message = parse(tampered);

            assertThatThrownBy(() -> VALIDATOR.validateSignature(message, SnsTestFixtures.publicKey()))
                .as(field)
                .isInstanceOf(SdkClientException.class)
                .hasMessage("The computed signature did not match the expected signature");
        }
    }

    @ParameterizedTest(name = "timestamp={0}")
    @ValueSource(strings = {
        "2024-01-01T00:00:00.000Z", // whole second: Instant#toString() drops the ".000", changing the canonical string
        "2024-06-15T12:30:45.123Z"  // non-zero milliseconds: control that validates regardless
    })
    void validateSignature_signatureCoversRawMillisecondTimestamp_doesNotThrow(String timestamp) {
        SnsMessage message = parse(SnsTestFixtures.signed(SnsTestFixtures.notification("hello", null, timestamp)));

        assertThatCode(() -> VALIDATOR.validateSignature(message, SnsTestFixtures.publicKey())).doesNotThrowAnyException();
    }

    @Test
    void validateSignature_otherKey_throws() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        PublicKey otherKey = generator.generateKeyPair().getPublic();
        SnsMessage message = parse(SnsTestFixtures.signed(SnsTestFixtures.notification("hello", null,
                                                                                          "2024-01-01T00:00:00.000Z")));

        assertThatThrownBy(() -> VALIDATOR.validateSignature(message, otherKey))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("The computed signature did not match the expected signature");
    }

    @Test
    void validateSignature_signatureMissing_throws() {
        SnsNotification notification = notification().signature(null).build();

        assertThatThrownBy(() -> VALIDATOR.validateSignature(notification, SnsTestFixtures.publicKey()))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("Message signature cannot be null");
    }

    @Test
    void validateSignature_timestampMissing_throws() {
        SnsNotification notification = notification().timestamp(null).build();

        assertThatThrownBy(() -> VALIDATOR.validateSignature(notification, SnsTestFixtures.publicKey()))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("Message timestamp cannot be null");
    }

    @Test
    void validateSignature_signatureVersionMissing_throws() {
        SnsNotification notification = notification().signatureVersion(null).build();

        assertThatThrownBy(() -> VALIDATOR.validateSignature(notification, SnsTestFixtures.publicKey()))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("Message signature version cannot be null");
    }

    @Test
    void validateSignature_signatureVersionUnknown_throwsIllegalArgumentAsV2() {
        SnsNotification notification = notification().signatureVersion(SignatureVersion.UNKNOWN).build();

        assertThatThrownBy(() -> VALIDATOR.validateSignature(notification, SnsTestFixtures.publicKey()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported signature version: null");
    }

    @Test
    void validateSignature_signatureMalformed_throws() {
        SnsNotification notification = notification().signature(SdkBytes.fromByteArray(new byte[1])).build();

        assertThatThrownBy(() -> VALIDATOR.validateSignature(notification, SnsTestFixtures.publicKey()))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("The signature is invalid");
    }

    private static SnsNotification.Builder notification() {
        return SnsNotification.builder()
                              .subject("hello world")
                              .message("hello world")
                              .messageId("message-id")
                              .signature(SdkBytes.fromByteArray(new byte[256]))
                              .signatureVersion(SignatureVersion.VERSION_1)
                              .timestamp(Instant.parse("2024-01-01T00:00:00Z"))
                              .unsubscribeUrl(URI.create("https://my-test-service.amazonaws.com"))
                              .signingCertUrl(URI.create("https://my-test-service.amazonaws.com/cert.pem"));
    }

    private static SnsMessage parse(Map<String, String> fields) {
        byte[] json = SnsTestFixtures.json(fields).getBytes(StandardCharsets.UTF_8);
        return UNMARSHALLER.unmarshall(new ByteArrayInputStream(json));
    }
}
