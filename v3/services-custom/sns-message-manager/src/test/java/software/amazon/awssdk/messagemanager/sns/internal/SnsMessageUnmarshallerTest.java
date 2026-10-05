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

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.messagemanager.sns.model.SignatureVersion;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessage;
import software.amazon.awssdk.messagemanager.sns.model.SnsNotification;
import software.amazon.awssdk.messagemanager.sns.model.SnsSubscriptionConfirmation;
import software.amazon.awssdk.messagemanager.sns.model.SnsUnsubscribeConfirmation;

class SnsMessageUnmarshallerTest {
    private static final SnsMessageUnmarshaller UNMARSHALLER = new SnsMessageUnmarshaller();
    private static final String SIGNATURE = Base64.getEncoder().encodeToString(new byte[] {1, 2, 3});

    @Test
    void unmarshall_notification_maximal() {
        SnsMessage msg = unmarshall("{\"Type\":\"Notification\",\"MessageId\":\"message-id\",\"TopicArn\":\"topic-arn\","
                                    + "\"Subject\":\"subject\",\"Message\":\"hello\\nworld\","
                                    + "\"Timestamp\":\"2024-01-01T00:00:00.000Z\",\"SignatureVersion\":\"1\","
                                    + "\"Signature\":\"" + SIGNATURE + "\",\"SigningCertURL\":\"https://host/cert.pem\","
                                    + "\"UnsubscribeURL\":\"https://host/?Action=Unsubscribe\","
                                    + "\"MessageAttributes\":{\"ignored\":{\"Type\":\"String\",\"Value\":\"x\"}}}");

        assertThat(msg).isInstanceOf(SnsNotification.class);
        SnsNotification notification = (SnsNotification) msg;
        assertThat(notification.messageId()).isEqualTo("message-id");
        assertThat(notification.topicArn()).isEqualTo("topic-arn");
        assertThat(notification.subject()).isEqualTo("subject");
        assertThat(notification.message()).isEqualTo("hello\nworld");
        assertThat(notification.timestamp()).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"));
        assertThat(notification.signatureVersion()).isEqualTo(SignatureVersion.VERSION_1);
        assertThat(notification.signature()).isEqualTo(SdkBytes.fromByteArray(new byte[] {1, 2, 3}));
        assertThat(notification.signingCertUrl()).isEqualTo(URI.create("https://host/cert.pem"));
        assertThat(notification.unsubscribeUrl()).isEqualTo(URI.create("https://host/?Action=Unsubscribe"));
    }

    @Test
    void unmarshall_subscriptionConfirmation_maximal() {
        SnsMessage msg = unmarshall(SnsTestFixtures.json(SnsTestFixtures.signed(
            SnsTestFixtures.subscription("SubscriptionConfirmation", "the-token"))));

        assertThat(msg).isInstanceOf(SnsSubscriptionConfirmation.class);
        SnsSubscriptionConfirmation confirmation = (SnsSubscriptionConfirmation) msg;
        assertThat(confirmation.token()).isEqualTo("the-token");
        assertThat(confirmation.subscribeUrl().getQuery()).endsWith("&Token=the-token");
        assertThat(confirmation.signatureVersion()).isEqualTo(SignatureVersion.VERSION_2);
        assertThat(confirmation.timestamp()).isEqualTo(Instant.parse("2024-06-15T12:30:45.123Z"));
    }

    @Test
    void unmarshall_unsubscribeConfirmation_maximal() {
        SnsMessage msg = unmarshall(SnsTestFixtures.json(SnsTestFixtures.signed(
            SnsTestFixtures.subscription("UnsubscribeConfirmation", "the-token"))));

        assertThat(msg).isInstanceOf(SnsUnsubscribeConfirmation.class);
        assertThat(((SnsUnsubscribeConfirmation) msg).token()).isEqualTo("the-token");
    }

    @Test
    void unmarshall_typeIsCaseInsensitiveAndUnknownSignatureVersionIsUnknown() {
        SnsMessage msg = unmarshall("{\"Type\":\"notification\",\"SignatureVersion\":\"3\"}");

        assertThat(msg).isInstanceOf(SnsNotification.class);
        assertThat(msg.signatureVersion()).isEqualTo(SignatureVersion.UNKNOWN);
    }

    @Test
    void unmarshall_unknownType_throws() {
        assertThatThrownBy(() -> unmarshall("{\"Type\":\"MyCustomType\"}"))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Unsupported sns message type");
    }

    @Test
    void unmarshall_typeMissing_throws() {
        assertThatThrownBy(() -> unmarshall("{\"Message\":\"hello\"}"))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("'Type' field must be present");
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "\"Notification\"", "42", "null"})
    void unmarshall_jsonNotObject_throws(String json) {
        assertThatThrownBy(() -> unmarshall(json))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Expected an JSON object");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"Type\":\"Notification\",\"Message\":\"secret-canary", "{secret-canary}", "",
                            "{\"Token\":secret-canary}"})
    void unmarshall_malformedJson_throwsWithoutEchoingInput(String json) {
        assertThatThrownBy(() -> unmarshall(json))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("Unable to parse the SNS message: it is not well-formed JSON")
            .hasNoCause();
    }

    @Test
    void unmarshall_signatureNotDecodable_throws() {
        assertThatThrownBy(() -> unmarshall("{\"Type\":\"Notification\",\"Signature\":\"this is not base64!\"}"))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Unable to decode signature");
    }

    @Test
    void unmarshall_timestampNotParsable_throws() {
        assertThatThrownBy(() -> unmarshall("{\"Type\":\"Notification\",\"Timestamp\":\"yesterday\"}"))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Unable to parse timestamp");
    }

    @Test
    void unmarshall_uriNotParsable_throwsWithoutTheUri() {
        String token = "secret-canary-token";
        assertThatThrownBy(() -> unmarshall("{\"Type\":\"SubscriptionConfirmation\",\"SubscribeURL\":"
                                            + "\"https://host/?Action=ConfirmSubscription&Token=" + token + " x\"}"))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("Unable to parse URI")
            .hasNoCause();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"Type\":1}", "{\"Type\":null}", "{\"Type\":{}}", "{\"Type\":[\"Notification\"]}"})
    void unmarshall_typeNotString_throws(String json) {
        assertThatThrownBy(() -> unmarshall(json))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("Expected field 'Type' to be a string");
    }

    @Test
    void unmarshall_memberNotString_throws() {
        assertThatThrownBy(() -> unmarshall("{\"Type\":\"Notification\",\"Message\":true}"))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("Expected field 'Message' to be a string");
    }

    @Test
    void unmarshall_fixtureNotification_roundTripsEveryField() {
        Map<String, String> fields = SnsTestFixtures.signed(
            SnsTestFixtures.notification("{\"default\":\"payload\"}", null, "2024-01-01T00:00:00.000Z"));

        SnsNotification notification = (SnsNotification) unmarshall(SnsTestFixtures.json(fields));

        assertThat(notification.message()).isEqualTo("{\"default\":\"payload\"}");
        assertThat(notification.subject()).isNull();
        assertThat(notification.signingCertUrl()).isEqualTo(URI.create(SnsTestFixtures.CERT_URL));
        assertThat(notification.signature().asByteArray())
            .isEqualTo(Base64.getDecoder().decode(fields.get("Signature")));
    }

    private static SnsMessage unmarshall(String json) {
        return UNMARSHALLER.unmarshall(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    }
}
