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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;
import software.amazon.awssdk.http.HttpClientOption;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.provider.HttpClientConfiguration;
import software.amazon.awssdk.http.provider.HttpClientResolver;
import software.amazon.awssdk.http.provider.InstalledHttpImplementations;
import software.amazon.awssdk.http.provider.ResolvedHttpClient;
import software.amazon.awssdk.http.provider.SdkHttpService;
import software.amazon.awssdk.messagemanager.sns.SnsMessageManager;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessage;
import software.amazon.awssdk.regions.Region;

/**
 * The {@link SnsMessageManager}: parses a message, retrieves (and caches) the regional signing certificate its
 * {@code SigningCertURL} names, and verifies its signature. Thread safe.
 */
public final class DefaultSnsMessageManager implements SnsMessageManager {
    /** The options of an SDK-created HTTP client, as in V2. */
    static final HttpClientConfiguration HTTP_CLIENT_DEFAULTS =
        HttpClientConfiguration.builder()
                               .put(HttpClientOption.CONNECTION_TIMEOUT, Duration.ofSeconds(10))
                               .put(HttpClientOption.READ_TIMEOUT, Duration.ofSeconds(30))
                               .build();

    private final SnsMessageUnmarshaller unmarshaller;
    private final CertificateRetriever certRetriever;
    private final SignatureValidator signatureValidator;

    private DefaultSnsMessageManager(BuilderImpl builder) {
        this.unmarshaller = new SnsMessageUnmarshaller();

        SnsHostProvider hostProvider = new SnsHostProvider(builder.region);
        URI signingCertEndpoint = hostProvider.regionalEndpoint();
        String signingCertCommonName = hostProvider.signingCertCommonName();

        certRetriever = builder.certRetriever != null
                        ? builder.certRetriever
                        : new CertificateRetriever(resolveHttpClient(builder), signingCertEndpoint.getHost(),
                                                   signingCertCommonName);

        signatureValidator = new SignatureValidator();
    }

    @Override
    public SnsMessage parseMessage(InputStream message) {
        Objects.requireNonNull(message, "message cannot be null");

        SnsMessage snsMessage = unmarshaller.unmarshall(message);
        PublicKey certificate = certRetriever.retrieveCertificate(snsMessage.signingCertUrl());

        signatureValidator.validateSignature(snsMessage, certificate);

        return snsMessage;
    }

    @Override
    public SnsMessage parseMessage(String message) {
        Objects.requireNonNull(message, "message cannot be null");
        return parseMessage(new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public void close() {
        certRetriever.close();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    /**
     * A caller-supplied client is used as is and never closed (V2 wrapped it in an unmanaged client). Otherwise the
     * installed HTTP implementation builds an SDK-owned client with {@link #HTTP_CLIENT_DEFAULTS}.
     */
    private static ResolvedHttpClient<SdkSyncHttpClient> resolveHttpClient(BuilderImpl builder) {
        HttpClientResolver<SdkSyncHttpClient, SdkHttpService> resolver = HttpClientResolver.sync();
        if (builder.httpClient != null) {
            return resolver.client(builder.httpClient).resolve();
        }
        if (builder.installed != null) {
            resolver.installed(builder.installed);
        }
        return resolver.configuration(HTTP_CLIENT_DEFAULTS).resolve();
    }

    static final class BuilderImpl implements SnsMessageManager.Builder {
        private Region region;
        private SdkSyncHttpClient httpClient;

        // Testing only
        private CertificateRetriever certRetriever;
        private Supplier<InstalledHttpImplementations> installed;

        @Override
        public Builder httpClient(SdkSyncHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        @Override
        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        /** Testing only: the certificate retriever to use instead of one built from the region. */
        Builder certificateRetriever(CertificateRetriever certificateRetriever) {
            this.certRetriever = certificateRetriever;
            return this;
        }

        /** Testing only: the installed HTTP implementations the default client is selected from. */
        Builder installedHttpImplementations(Supplier<InstalledHttpImplementations> installed) {
            this.installed = installed;
            return this;
        }

        @Override
        public SnsMessageManager build() {
            return new DefaultSnsMessageManager(this);
        }
    }
}
