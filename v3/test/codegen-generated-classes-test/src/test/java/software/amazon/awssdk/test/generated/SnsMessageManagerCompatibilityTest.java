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

package software.amazon.awssdk.test.generated;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.messagemanager.sns.SnsMessageManager;
import software.amazon.awssdk.messagemanager.sns.model.SignatureVersion;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessage;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessageType;
import software.amazon.awssdk.messagemanager.sns.model.SnsNotification;
import software.amazon.awssdk.messagemanager.sns.model.SnsSubscriptionConfirmation;
import software.amazon.awssdk.messagemanager.sns.model.SnsUnsubscribeConfirmation;
import software.amazon.awssdk.regions.Region;

/**
 * Launch-parity evidence for the V3 SNS Message Manager (task 14.5; Requirements 16.8, 17.1, 19.12, 21.10), over its
 * public API only: the documented V2 behaviors of {@code parseMessage} (typed, signature-verified SignatureVersion 1
 * and 2 messages of every type; tampered, foreign-host and wrongly issued certificates rejected with the V2
 * diagnostics), the {@code ADAPTED} {@code httpClient(SdkSyncHttpClient)} replacement (a caller-owned client is used
 * and never closed), and the {@code ADAPTED} token redaction of {@code toString} and of failure diagnostics.
 *
 * <p>Deterministic and offline: messages are signed here with the checked-in, locally generated test key of the
 * {@code sns-message-manager} module ({@code v3.fixture.snsSigning}; certificates valid 2020-2040), and certificates
 * are served by an in-memory {@link SdkSyncHttpClient}. Nothing is fetched from the network.
 */
class SnsMessageManagerCompatibilityTest {
    private static final String CERT_HOST = "sns.us-west-2.amazonaws.com";
    private static final String CERT_URL = "https://" + CERT_HOST + "/SimpleNotificationService-0f.pem";
    private static final String TOPIC_ARN = "arn:aws:sns:us-west-2:123456789012:MyTopic";
    private static final String TOKEN = "2336412f37fb687f5d51e6e2425f004aed7b8d7c";
    private static final char[] STORE_PASSWORD = "test-only".toCharArray();

    private static Path fixtures;
    private static PrivateKey signingKey;

    @BeforeAll
    static void loadFixtures() throws Exception {
        fixtures = Path.of(System.getProperty("v3.fixture.snsSigning"));
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(fixtures.resolve("signing-key.p12"))) {
            store.load(in, STORE_PASSWORD);
        }
        signingKey = (PrivateKey) store.getKey("signing", STORE_PASSWORD);
    }

    @Test
    void parseMessage_signedNotificationOfEitherSignatureVersion_isReturnedTyped() throws Exception {
        for (String version : List.of("1", "2")) {
            Map<String, String> fields = notification();
            InMemoryHttpClient http = new InMemoryHttpClient().serve(CERT_URL, certificate("sns-global"));
            try (SnsMessageManager manager = manager(http)) {
                SnsMessage message = manager.parseMessage(json(signed(fields, version)));

                SnsNotification notification = assertInstanceOf(SnsNotification.class, message);
                assertEquals(SnsMessageType.NOTIFICATION, notification.type());
                assertEquals("hello world", notification.message());
                assertEquals("the subject", notification.subject());
                assertEquals(TOPIC_ARN, notification.topicArn());
                assertEquals(Instant.parse("2024-01-01T00:00:00Z"), notification.timestamp());
                assertEquals("1".equals(version) ? SignatureVersion.VERSION_1 : SignatureVersion.VERSION_2,
                             notification.signatureVersion());
                assertEquals(URI.create(CERT_URL), notification.signingCertUrl());
                assertEquals(URI.create(fields.get("UnsubscribeURL")), notification.unsubscribeUrl());
            }
        }
    }

    @Test
    void parseMessage_subscriptionMessagesFromStreamOrString_areEqualAndTyped() throws Exception {
        InMemoryHttpClient http = new InMemoryHttpClient().serve(CERT_URL, certificate("sns-global"));
        try (SnsMessageManager manager = manager(http)) {
            String subscribe = json(signed(subscription("SubscriptionConfirmation"), "2"));
            String unsubscribe = json(signed(subscription("UnsubscribeConfirmation"), "1"));

            SnsMessage fromString = manager.parseMessage(subscribe);
            SnsMessage fromStream = manager.parseMessage(new ByteArrayInputStream(
                subscribe.getBytes(StandardCharsets.UTF_8)));

            assertEquals(fromString, fromStream);
            assertEquals(TOKEN, assertInstanceOf(SnsSubscriptionConfirmation.class, fromString).token());
            assertEquals(TOKEN, assertInstanceOf(SnsUnsubscribeConfirmation.class, manager.parseMessage(unsubscribe))
                .token());
            // The certificate is fetched once and cached.
            assertEquals(1, http.requests.size());
        }
    }

    @Test
    void parseMessage_tamperedMessage_isRejected() throws Exception {
        Map<String, String> fields = signed(notification(), "2");
        fields.put("Message", "hello world!");
        InMemoryHttpClient http = new InMemoryHttpClient().serve(CERT_URL, certificate("sns-global"));
        try (SnsMessageManager manager = manager(http)) {
            SdkClientException e = assertThrows(SdkClientException.class, () -> manager.parseMessage(json(fields)));

            assertEquals("The computed signature did not match the expected signature", e.getMessage());
        }
    }

    @Test
    void parseMessage_certificateOfANonSnsHost_isRejectedWithoutBeingFetched() throws Exception {
        InMemoryHttpClient http = new InMemoryHttpClient();
        try (SnsMessageManager manager = manager(http)) {
            for (String url : List.of("https://evil.example/cert.pem", "http://" + CERT_HOST + "/cert.pem",
                                      "https://" + CERT_HOST + "@evil.example/cert.pem",
                                      "https://sns.us-east-1.amazonaws.com/cert.pem")) {
                Map<String, String> fields = signed(notification(), "2");
                fields.put("SigningCertURL", url);

                SdkClientException e = assertThrows(SdkClientException.class, () -> manager.parseMessage(json(fields)));
                assertTrue(e.getMessage().startsWith("Certificate URL "), e.getMessage());
            }
        }
        assertEquals(List.of(), http.requests);
    }

    @Test
    void parseMessage_certificateNotIssuedToSnsOrExpired_isRejected() throws Exception {
        Map<String, String> expected = Map.of("wrong-name", "Certificate does not match expected common name: "
                                                            + "sns.amazonaws.com",
                                              "expired", "The certificate is expired",
                                              "not-yet-valid", "The certificate is not yet valid");
        for (Map.Entry<String, String> certificate : expected.entrySet()) {
            InMemoryHttpClient http = new InMemoryHttpClient().serve(CERT_URL, certificate(certificate.getKey()));
            try (SnsMessageManager manager = manager(http)) {
                SdkClientException e = assertThrows(SdkClientException.class,
                                                    () -> manager.parseMessage(json(signed(notification(), "2"))));

                assertEquals(certificate.getValue(), e.getMessage(), certificate.getKey());
            }
        }
    }

    /** The ADAPTED httpClient(SdkSyncHttpClient) replacement: the caller's client is used and stays open. */
    @Test
    void httpClient_callerOwnedSyncClient_isUsedAndNotClosed() throws Exception {
        InMemoryHttpClient http = new InMemoryHttpClient().serve(CERT_URL, certificate("sns-global"));
        SnsMessageManager.Builder builder = SnsMessageManager.builder().region(Region.US_WEST_2);

        SnsMessageManager manager = builder.httpClient(http).build();
        manager.parseMessage(json(signed(notification(), "2")));
        manager.close();

        assertEquals(List.of("GET https://" + CERT_HOST + "/SimpleNotificationService-0f.pem"),
                     http.requests.stream().map(r -> r.method() + " " + r.scheme() + "://" + r.host() + r.encodedPath())
                                  .collect(Collectors.toList()));
        assertFalse(http.closed);
    }

    /** The ADAPTED toString of subscription messages redacts the token (Requirement 21.10). */
    @Test
    void toString_ofSubscriptionMessages_redactsTheToken() {
        URI subscribeUrl = URI.create("https://" + CERT_HOST + "/?Action=ConfirmSubscription&TopicArn=" + TOPIC_ARN
                                      + "&Token=" + TOKEN);
        String subscribe = SnsSubscriptionConfirmation.builder().messageId("id").subscribeUrl(subscribeUrl).token(TOKEN)
                                                      .build().toString();
        String unsubscribe = SnsUnsubscribeConfirmation.builder().subscribeUrl(subscribeUrl).token(TOKEN).build()
                                                       .toString();

        assertEquals("SnsSubscriptionConfirmation(MessageId=id, SubscribeUrl=https://" + CERT_HOST
                     + "/?Action=ConfirmSubscription&TopicArn=" + TOPIC_ARN + "&Token=*** Sensitive Data Redacted ***, "
                     + "Token=*** Sensitive Data Redacted ***)", subscribe);
        assertFalse(unsubscribe.contains(TOKEN), unsubscribe);
        // Notifications render every field as in V2.
        assertEquals("SnsNotification(MessageId=id, Signature=SdkBytes(bytes=0x01), Subject=s)",
                     SnsNotification.builder().messageId("id").signature(SdkBytes.fromByteArray(new byte[] {1}))
                                    .subject("s").build().toString());
    }

    /** The ADAPTED verification diagnostics: rejected messages never echo the token or message content. */
    @Test
    void rejectedMessages_failureDiagnosticsCarryNoMessageContent() throws Exception {
        String valid = json(signed(subscription("SubscriptionConfirmation"), "2"));
        List<String> rejected = List.of(valid.substring(0, valid.indexOf(TOKEN) + TOKEN.length()),
                                        valid.replace("&Token=" + TOKEN, "&Token=" + TOKEN + " invalid"),
                                        valid.replace("\"Token\":\"" + TOKEN, "\"Token\":\"" + TOKEN + "x"));
        InMemoryHttpClient http = new InMemoryHttpClient().serve(CERT_URL, certificate("sns-global"));
        try (SnsMessageManager manager = manager(http)) {
            for (String message : rejected) {
                SdkClientException e = assertThrows(SdkClientException.class, () -> manager.parseMessage(message));
                for (Throwable t = e; t != null; t = t.getCause()) {
                    assertFalse(String.valueOf(t.getMessage()).contains(TOKEN), t.toString());
                }
            }
        }
    }

    private static SnsMessageManager manager(SdkSyncHttpClient http) {
        return SnsMessageManager.builder().region(Region.US_WEST_2).httpClient(http).build();
    }

    private static Map<String, String> notification() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Type", "Notification");
        fields.put("MessageId", "22b80b92-fdea-4c2c-8f9d-bdfb0c7bf324");
        fields.put("TopicArn", TOPIC_ARN);
        fields.put("Subject", "the subject");
        fields.put("Message", "hello world");
        fields.put("Timestamp", "2024-01-01T00:00:00.000Z");
        fields.put("UnsubscribeURL", "https://" + CERT_HOST + "/?Action=Unsubscribe&SubscriptionArn=" + TOPIC_ARN
                                     + ":c9135db0-26c4-47ec-8998-413945fb5a96");
        return fields;
    }

    private static Map<String, String> subscription(String type) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("Type", type);
        fields.put("MessageId", "165545c9-2a5c-472c-8df2-7ff2be2b3b1b");
        fields.put("Token", TOKEN);
        fields.put("TopicArn", TOPIC_ARN);
        fields.put("Message", "You have chosen to subscribe to the topic " + TOPIC_ARN + ".");
        fields.put("SubscribeURL", "https://" + CERT_HOST + "/?Action=ConfirmSubscription&TopicArn=" + TOPIC_ARN
                                   + "&Token=" + TOKEN);
        fields.put("Timestamp", "2024-06-15T12:30:45.123Z");
        return fields;
    }

    /**
     * {@code fields} with SignatureVersion, SigningCertURL and the Signature SNS computes: SHA1withRSA (version 1) or
     * SHA256withRSA (version 2) over "name\nvalue\n" of the documented signed fields, in order.
     */
    private static Map<String, String> signed(Map<String, String> fields, String version)
        throws GeneralSecurityException {
        List<String> names = "Notification".equals(fields.get("Type"))
                             ? List.of("Message", "MessageId", "Subject", "Timestamp", "TopicArn", "Type")
                             : List.of("Message", "MessageId", "SubscribeURL", "Timestamp", "Token", "TopicArn", "Type");
        StringBuilder canonical = new StringBuilder();
        for (String name : names) {
            if (fields.containsKey(name)) {
                canonical.append(name).append('\n').append(fields.get(name)).append('\n');
            }
        }
        Signature signature = Signature.getInstance("1".equals(version) ? "SHA1withRSA" : "SHA256withRSA");
        signature.initSign(signingKey);
        signature.update(canonical.toString().getBytes(StandardCharsets.UTF_8));
        Map<String, String> result = new LinkedHashMap<>(fields);
        result.put("SignatureVersion", version);
        result.put("Signature", Base64.getEncoder().encodeToString(signature.sign()));
        result.put("SigningCertURL", CERT_URL);
        return result;
    }

    private static String json(Map<String, String> fields) {
        return fields.entrySet().stream()
                     .map(e -> quote(e.getKey()) + ":" + quote(e.getValue()))
                     .collect(Collectors.joining(",", "{", "}"));
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    private static byte[] certificate(String name) throws IOException {
        return Files.readAllBytes(fixtures.resolve(name + ".pem"));
    }

    /** Serves configured bodies by request URL; 404 otherwise. Records requests and closing. */
    private static final class InMemoryHttpClient implements SdkSyncHttpClient {
        private final Map<String, byte[]> bodies = new LinkedHashMap<>();
        private final List<SdkHttpRequest> requests = new ArrayList<>();
        private boolean closed;

        InMemoryHttpClient serve(String url, byte[] body) {
            bodies.put(url, body);
            return this;
        }

        @Override
        public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
            return new SyncHttpExchange() {
                @Override
                public SdkHttpResponse execute() {
                    requests.add(request);
                    byte[] body = bodies.get(request.scheme() + "://" + request.host() + request.encodedPath());
                    return body == null ? SdkHttpResponse.builder().statusCode(404).build()
                                        : SdkHttpResponse.builder().statusCode(200).body(HttpBody.fromBytes(body))
                                                         .build();
                }

                @Override
                public void abort() {
                }

                @Override
                public void close() {
                }
            };
        }

        @Override
        public HttpClientCapabilities capabilities() {
            return HttpClientCapabilities.builder().mode(HttpTransportMode.SYNC).addProtocol(Protocol.HTTP1_1)
                                         .maxBufferedBytesPerExchange(64 * 1024)
                                         .cancellationTimeout(Duration.ofSeconds(1)).build();
        }

        @Override
        public String clientName() {
            return "InMemoryHttpClient";
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
