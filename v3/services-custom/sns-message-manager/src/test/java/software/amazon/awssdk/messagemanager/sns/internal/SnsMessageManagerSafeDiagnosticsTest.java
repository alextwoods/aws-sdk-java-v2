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
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.messagemanager.sns.SnsMessageManager;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessage;
import software.amazon.awssdk.regions.Region;

/**
 * Requirement 21.10 for the SNS Message Manager: a subscription token (which confirms a subscription by itself) and
 * message bodies never appear in exception messages, exception causes or stack-trace renderings of a rejected
 * message, and a token never appears in the rendering of a parsed message. Every case plants unique canaries in the
 * token, the subscribe URL's token and the message body, and searches the full rendered failure.
 */
class SnsMessageManagerSafeDiagnosticsTest {
    private static final String TOKEN = "canary-token-6f1d0c9e";
    private static final String BODY = "canary-body-0b7a52d4";

    static Stream<Arguments> rejectedMessages() {
        return Stream.of(
            rejected("tampered body", f -> put(f, "Message", BODY + " tampered")),
            rejected("tampered token", f -> put(f, "Token", TOKEN + "-tampered")),
            rejected("malformed subscribe URL", f -> put(f, "SubscribeURL", "https://h/?Token=" + TOKEN + " x")),
            rejected("malformed signing certificate URL", f -> put(f, "SigningCertURL", "https://h/" + TOKEN + " x")),
            rejected("certificate URL of another host",
                     f -> put(f, "SigningCertURL", "https://evil.example/" + TOKEN + ".pem")),
            rejected("unparsable timestamp", f -> put(f, "Timestamp", "not-a-time")),
            rejected("undecodable signature", f -> put(f, "Signature", "!" + TOKEN)),
            rejected("unknown type", f -> put(f, "Type", "Other")),
            rejected("non-string member", f -> f),
            rejected("missing signature", f -> {
                f.remove("Signature");
                return f;
            }));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rejectedMessages")
    void rejectedMessage_failureCarriesNoTokenOrBody(String name, UnaryOperator<Map<String, String>> change) {
        Map<String, String> fields = change.apply(new LinkedHashMap<>(signedSubscription()));
        String json = name.equals("non-string member")
                      ? SnsTestFixtures.json(fields).replace("\"Type\":\"SubscriptionConfirmation\"",
                                                             "\"Type\":[\"" + TOKEN + "\",\"" + BODY + "\"]")
                      : SnsTestFixtures.json(fields);

        Throwable failure = catchThrowable(() -> parse(json));

        assertThat(failure).isInstanceOf(SdkClientException.class);
        assertThat(render(failure)).doesNotContain(TOKEN).doesNotContain(BODY);
    }

    @Test
    void malformedJson_failureCarriesNoTokenOrBody() {
        String json = SnsTestFixtures.json(signedSubscription());
        String[] malformed = {
            json.replace("\"" + TOKEN + "\"", "\"" + TOKEN + "\" " + BODY),
            json.replace("\"" + TOKEN + "\"", TOKEN),
            json.substring(0, json.indexOf(TOKEN) + TOKEN.length()) + " " + BODY
        };

        for (String input : malformed) {
            Throwable failure = catchThrowable(() -> parse(input));

            assertThat(failure).isInstanceOf(SdkClientException.class);
            assertThat(render(failure)).doesNotContain(TOKEN).doesNotContain(BODY);
        }
    }

    @Test
    void parsedSubscriptionConfirmation_renderingCarriesNoToken() {
        SnsMessage message = parse(SnsTestFixtures.json(signedSubscription()));

        assertThat(message.toString()).doesNotContain(TOKEN).contains(SensitiveValues.REDACTED);
    }

    private static Map<String, String> signedSubscription() {
        Map<String, String> fields = SnsTestFixtures.subscription("SubscriptionConfirmation", TOKEN);
        fields.put("Message", BODY);
        return SnsTestFixtures.signed(fields);
    }

    private static SnsMessage parse(String json) {
        FakeSyncHttpClient http = new FakeSyncHttpClient().serve(SnsTestFixtures.CERT_URL,
                                                                 SnsTestFixtures.certificate("sns-global"));
        try (SnsMessageManager manager = SnsMessageManager.builder().region(Region.US_WEST_2).httpClient(http).build()) {
            return manager.parseMessage(json);
        }
    }

    private static Arguments rejected(String name, UnaryOperator<Map<String, String>> change) {
        return Arguments.of(name, change);
    }

    private static Map<String, String> put(Map<String, String> fields, String name, String value) {
        fields.put(name, value);
        return fields;
    }

    /** The exception with every cause and suppressed exception, as a stack trace prints it. */
    private static String render(Throwable failure) {
        StringWriter out = new StringWriter();
        failure.printStackTrace(new PrintWriter(out));
        return out.toString();
    }
}
