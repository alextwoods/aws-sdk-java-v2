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

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import software.amazon.awssdk.core.SdkBytes;

class SnsMessageModelTest {
    private static final String TOKEN = "2336412f37fb687f5d51e6e2425f004aed7b8d7c";
    private static final URI SUBSCRIBE_URL = URI.create(
        "https://sns.us-west-2.amazonaws.com/?Action=ConfirmSubscription&TopicArn=arn:aws:sns:us-west-2:1:T&Token="
        + TOKEN);

    @Test
    void notification_equalsHashCodeAndV2ToString() {
        SnsNotification a = notification().build();
        SnsNotification b = notification().build();

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b).isNotEqualTo(notification().subject("other").build());
        assertThat(a.type()).isEqualTo(SnsMessageType.NOTIFICATION);
        assertThat(a).hasToString("SnsNotification(MessageId=id, Message=hello, TopicArn=arn, "
                                  + "Timestamp=2024-01-01T00:00:00Z, Signature=SdkBytes(bytes=0x0102), "
                                  + "SignatureVersion=2, SigningCertUrl=https://host/cert.pem, Subject=subject, "
                                  + "UnsubscribeUrl=https://host/?Action=Unsubscribe)");
    }

    @Test
    void notification_differentTypesWithEqualFields_areNotEqual() {
        SnsSubscriptionConfirmation subscribe = SnsSubscriptionConfirmation.builder().messageId("id").build();
        SnsUnsubscribeConfirmation unsubscribe = SnsUnsubscribeConfirmation.builder().messageId("id").build();

        assertThat((Object) subscribe).isNotEqualTo(unsubscribe);
        assertThat(subscribe.type()).isEqualTo(SnsMessageType.SUBSCRIPTION_CONFIRMATION);
        assertThat(unsubscribe.type()).isEqualTo(SnsMessageType.UNSUBSCRIBE_CONFIRMATION);
    }

    @Test
    void subscriptionConfirmation_toStringRedactsTheToken() {
        SnsSubscriptionConfirmation confirmation = SnsSubscriptionConfirmation.builder()
                                                                              .messageId("id")
                                                                              .token(TOKEN)
                                                                              .subscribeUrl(SUBSCRIBE_URL)
                                                                              .build();

        assertThat(confirmation.token()).isEqualTo(TOKEN);
        assertThat(confirmation.subscribeUrl()).isEqualTo(SUBSCRIBE_URL);
        assertThat(confirmation.toString())
            .doesNotContain(TOKEN)
            .isEqualTo("SnsSubscriptionConfirmation(MessageId=id, SubscribeUrl=https://sns.us-west-2.amazonaws.com/"
                       + "?Action=ConfirmSubscription&TopicArn=arn:aws:sns:us-west-2:1:T"
                       + "&Token=*** Sensitive Data Redacted ***, Token=*** Sensitive Data Redacted ***)");
    }

    @Test
    void unsubscribeConfirmation_toStringRedactsTheToken() {
        SnsUnsubscribeConfirmation confirmation = SnsUnsubscribeConfirmation.builder()
                                                                            .token(TOKEN)
                                                                            .subscribeUrl(SUBSCRIBE_URL)
                                                                            .build();

        assertThat(confirmation.toString()).doesNotContain(TOKEN).contains("Token=*** Sensitive Data Redacted ***");
        assertThat(SnsUnsubscribeConfirmation.builder().build()).hasToString("SnsUnsubscribeConfirmation()");
    }

    @ParameterizedTest
    @CsvSource({"Notification, NOTIFICATION", "NOTIFICATION, NOTIFICATION", "SubscriptionConfirmation, "
                + "SUBSCRIPTION_CONFIRMATION", "unsubscribeconfirmation, UNSUBSCRIBE_CONFIRMATION", "Other, UNKNOWN"})
    void messageType_fromValue_ignoresCase(String value, SnsMessageType expected) {
        assertThat(SnsMessageType.fromValue(value)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"1, VERSION_1", "2, VERSION_2", "3, UNKNOWN", "v1, UNKNOWN"})
    void signatureVersion_fromValue_isExact(String value, SignatureVersion expected) {
        assertThat(SignatureVersion.fromValue(value)).isEqualTo(expected);
        assertThat(SignatureVersion.fromValue(null)).isEqualTo(SignatureVersion.UNKNOWN);
    }

    private static SnsNotification.Builder notification() {
        return SnsNotification.builder()
                              .messageId("id")
                              .message("hello")
                              .topicArn("arn")
                              .timestamp(Instant.parse("2024-01-01T00:00:00Z"))
                              .signature(SdkBytes.fromByteArray(new byte[] {1, 2}))
                              .signatureVersion(SignatureVersion.VERSION_2)
                              .signingCertUrl(URI.create("https://host/cert.pem"))
                              .subject("subject")
                              .unsubscribeUrl(URI.create("https://host/?Action=Unsubscribe"));
    }
}
