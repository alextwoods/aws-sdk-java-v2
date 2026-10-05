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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpClientOption;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.provider.HttpClientConfiguration;
import software.amazon.awssdk.http.provider.HttpClientConstruction;
import software.amazon.awssdk.http.provider.HttpImplementationSelectionException;
import software.amazon.awssdk.http.provider.InstalledHttpImplementations;
import software.amazon.awssdk.http.provider.SdkHttpService;
import software.amazon.awssdk.messagemanager.sns.SnsMessageManager;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessage;
import software.amazon.awssdk.messagemanager.sns.model.SnsNotification;
import software.amazon.awssdk.messagemanager.sns.model.SnsSubscriptionConfirmation;
import software.amazon.awssdk.regions.Region;

class DefaultSnsMessageManagerTest {
    private final FakeSyncHttpClient http = new FakeSyncHttpClient();

    @Test
    void parseMessage_signedNotification_returnsIt() {
        http.serve(SnsTestFixtures.CERT_URL, SnsTestFixtures.certificate("sns-global"));
        Map<String, String> fields = SnsTestFixtures.signed(
            SnsTestFixtures.notification("hello", "subject", "2024-01-01T00:00:00.000Z"));

        try (SnsMessageManager manager = manager()) {
            SnsMessage message = manager.parseMessage(SnsTestFixtures.json(fields));

            assertThat(message).isInstanceOf(SnsNotification.class);
            assertThat(message.message()).isEqualTo("hello");
            assertThat(((SnsNotification) message).subject()).isEqualTo("subject");
        }
    }

    @Test
    void parseMessage_streamAndStringForms_areEqual() {
        http.serve(SnsTestFixtures.CERT_URL, SnsTestFixtures.certificate("sns-global"));
        String json = SnsTestFixtures.json(SnsTestFixtures.signed(
            SnsTestFixtures.subscription("SubscriptionConfirmation", "the-token"), "1", SnsTestFixtures.CERT_URL));

        try (SnsMessageManager manager = manager()) {
            SnsMessage fromString = manager.parseMessage(json);
            SnsMessage fromStream = manager.parseMessage(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

            assertThat(fromString).isInstanceOf(SnsSubscriptionConfirmation.class).isEqualTo(fromStream);
            assertThat(http.requests()).hasSize(1);
        }
    }

    @Test
    void parseMessage_tamperedMessage_throws() {
        http.serve(SnsTestFixtures.CERT_URL, SnsTestFixtures.certificate("sns-global"));
        Map<String, String> fields = SnsTestFixtures.signed(
            SnsTestFixtures.notification("hello", null, "2024-01-01T00:00:00.000Z"));
        fields.put("Message", "goodbye");

        try (SnsMessageManager manager = manager()) {
            assertThatThrownBy(() -> manager.parseMessage(SnsTestFixtures.json(fields)))
                .isInstanceOf(SdkClientException.class)
                .hasMessage("The computed signature did not match the expected signature");
        }
    }

    @Test
    void parseMessage_certificateUrlOfAnotherHost_isRejectedWithoutFetching() {
        String evil = "https://evil.example/SimpleNotificationService-1.pem";
        http.serve(evil, SnsTestFixtures.certificate("sns-global"));
        Map<String, String> fields = SnsTestFixtures.signed(
            SnsTestFixtures.notification("hello", null, "2024-01-01T00:00:00.000Z"), "2", evil);

        try (SnsMessageManager manager = manager()) {
            assertThatThrownBy(() -> manager.parseMessage(SnsTestFixtures.json(fields)))
                .isInstanceOf(SdkClientException.class)
                .hasMessage("Certificate URL does not match expected host: " + SnsTestFixtures.REGION_CERT_HOST);
        }
        assertThat(http.requests()).isEmpty();
    }

    @Test
    void parseMessage_certificateOfAnotherRegionalName_isRejected() {
        // ap-east-1 certificates must be issued to sns-signing.ap-east-1.amazonaws.com.
        String url = "https://sns.ap-east-1.amazonaws.com/SimpleNotificationService-1.pem";
        http.serve(url, SnsTestFixtures.certificate("sns-global"));
        Map<String, String> fields = SnsTestFixtures.signed(
            SnsTestFixtures.notification("hello", null, "2024-01-01T00:00:00.000Z"), "2", url);

        try (SnsMessageManager manager = SnsMessageManager.builder().region(Region.AP_EAST_1).httpClient(http)
                                                          .build()) {
            assertThatThrownBy(() -> manager.parseMessage(SnsTestFixtures.json(fields)))
                .isInstanceOf(SdkClientException.class)
                .hasMessage("Certificate does not match expected common name: sns-signing.ap-east-1.amazonaws.com");
        }
    }

    @Test
    void close_callerOwnedHttpClient_isLeftOpen() {
        SnsMessageManager.builder().region(Region.US_WEST_2).httpClient(http).build().close();

        assertThat(http.isClosed()).isFalse();
    }

    @Test
    void build_withoutHttpClient_createsAnSdkOwnedClientWithTheV2Defaults() {
        AtomicReference<HttpClientConfiguration> configuration = new AtomicReference<>();
        DefaultSnsMessageManager.BuilderImpl builder = new DefaultSnsMessageManager.BuilderImpl();
        builder.region(Region.US_WEST_2);
        builder.installedHttpImplementations(() -> InstalledHttpImplementations.of(new FakeService(configuration)));

        SnsMessageManager manager = builder.build();

        assertThat(configuration.get().value(HttpClientOption.CONNECTION_TIMEOUT)).contains(Duration.ofSeconds(10));
        assertThat(configuration.get().value(HttpClientOption.READ_TIMEOUT)).contains(Duration.ofSeconds(30));
        assertThat(configuration.get().configuredOptions())
            .containsExactlyInAnyOrder(HttpClientOption.CONNECTION_TIMEOUT, HttpClientOption.READ_TIMEOUT);
        assertThat(http.isClosed()).isFalse();
        manager.close();
        assertThat(http.isClosed()).isTrue();
    }

    @Test
    void build_withoutHttpClientOrInstalledImplementation_throws() {
        DefaultSnsMessageManager.BuilderImpl builder = new DefaultSnsMessageManager.BuilderImpl();
        builder.region(Region.US_WEST_2);
        builder.installedHttpImplementations(InstalledHttpImplementations::none);

        assertThatThrownBy(builder::build).isInstanceOf(HttpImplementationSelectionException.class);
    }

    @Test
    void build_regionMissing_throws() {
        assertThatThrownBy(() -> SnsMessageManager.builder().httpClient(http).build())
            .isInstanceOf(NullPointerException.class)
            .hasMessage("region must not be null");
    }

    @Test
    void parseMessage_null_throws() {
        try (SnsMessageManager manager = manager()) {
            assertThatThrownBy(() -> manager.parseMessage((String) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("message cannot be null");
            assertThatThrownBy(() -> manager.parseMessage((java.io.InputStream) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("message cannot be null");
        }
    }

    private SnsMessageManager manager() {
        return SnsMessageManager.builder().region(Region.US_WEST_2).httpClient(http).build();
    }

    /** An installed HTTP implementation that builds {@link #http} and records its construction configuration. */
    private final class FakeService implements SdkHttpService {
        private final AtomicReference<HttpClientConfiguration> configuration;

        FakeService(AtomicReference<HttpClientConfiguration> configuration) {
            this.configuration = configuration;
        }

        @Override
        public String implementationName() {
            return "fake";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Override
        public HttpClientCapabilities capabilities() {
            return FakeSyncHttpClient.CAPABILITIES;
        }

        @Override
        public SdkSyncHttpClient create(HttpClientConstruction construction) {
            configuration.set(construction.configuration());
            return http;
        }
    }
}
