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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.http.provider.ResolvedHttpClient;

/**
 * Retrieves the certificate used to sign a message, validates it, and caches the public keys of the most recently
 * used certificates for future use. Thread safe.
 *
 * <p>A certificate is fetched only from a URL {@link CertificateUrlValidator} accepts (HTTPS, the regional SNS host),
 * must be a single PEM-encoded X.509 certificate, must be valid now, and must be issued to the regional SNS signing
 * name ({@link SigningCertificateNameVerifier}). Failures are not cached.
 */
public final class CertificateRetriever implements AutoCloseable {
    private static final Pattern X509_FORMAT = Pattern.compile(
        "^[\\s]*-----BEGIN [A-Z]+-----\\n[A-Za-z\\d+\\/\\n]+[=]{0,2}\\n-----END [A-Z]+-----[\\s]*$");
    private static final int CACHE_SIZE = 10;

    private final ResolvedHttpClient<SdkSyncHttpClient> httpClient;
    private final String certCommonName;
    private final CertificateUrlValidator certUrlValidator;
    private final Clock clock;
    private final Map<URI, PublicKey> certificateCache = new LinkedHashMap<>(CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<URI, PublicKey> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    /**
     * @param httpClient the client certificates are fetched with; {@link #close()} closes it as its ownership says
     */
    public CertificateRetriever(ResolvedHttpClient<SdkSyncHttpClient> httpClient, String certHost,
                                String certCommonName) {
        this(httpClient, certCommonName, new CertificateUrlValidator(certHost), Clock.systemUTC());
    }

    CertificateRetriever(ResolvedHttpClient<SdkSyncHttpClient> httpClient, String certCommonName,
                         CertificateUrlValidator certificateUrlValidator, Clock clock) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null.");
        this.certCommonName = Objects.requireNonNull(certCommonName, "certCommonName must not be null.");
        this.certUrlValidator = Objects.requireNonNull(certificateUrlValidator,
                                                       "certificateUrlValidator must not be null.");
        this.clock = Objects.requireNonNull(clock, "clock must not be null.");
    }

    public PublicKey retrieveCertificate(URI certificateUrl) {
        Objects.requireNonNull(certificateUrl, "certificateUrl must not be null.");
        certUrlValidator.validate(certificateUrl);
        synchronized (certificateCache) {
            PublicKey cached = certificateCache.get(certificateUrl);
            if (cached != null) {
                return cached;
            }
        }
        // Fetched outside the lock, so one slow certificate host does not block messages with cached certificates.
        PublicKey fetched = fetchCertificate(certificateUrl);
        synchronized (certificateCache) {
            PublicKey raced = certificateCache.putIfAbsent(certificateUrl, fetched);
            return raced != null ? raced : fetched;
        }
    }

    /** Closes the HTTP client if the SDK owns it; a caller-supplied client is left open. */
    @Override
    public void close() {
        httpClient.close();
    }

    private PublicKey fetchCertificate(URI certificateUrl) {
        byte[] cert = fetchUrl(certificateUrl);
        validateCertificateData(cert);
        return createPublicKey(cert);
    }

    private byte[] fetchUrl(URI certificateUrl) {
        SdkHttpResponse response;
        try (SyncHttpExchange exchange = httpClient.client().prepare(request(certificateUrl),
                                                                     HttpExecutionOptions.defaults())) {
            try {
                response = exchange.execute();
            } catch (RuntimeException e) {
                throw unexpected(certificateUrl, e);
            }
            if (!response.isSuccessful()) {
                throw SdkClientException.create("Request was unsuccessful. Status Code: " + response.statusCode());
            }
            return readResponse(certificateUrl, response.body());
        } catch (IllegalArgumentException | IllegalStateException e) {
            // The URL is not a valid HTTP request target, or the client is closed.
            throw unexpected(certificateUrl, e);
        }
    }

    private static SdkHttpRequest request(URI certificateUrl) {
        String path = certificateUrl.getRawPath();
        SdkHttpRequest.Builder request = SdkHttpRequest.builder()
                                                       .method("GET")
                                                       .scheme(certificateUrl.getScheme())
                                                       .host(certificateUrl.getHost())
                                                       .port(certificateUrl.getPort())
                                                       .encodedPath(path == null || path.isEmpty() ? "/" : path);
        String query = certificateUrl.getRawQuery();
        if (query != null) {
            for (String parameter : query.split("&")) {
                if (parameter.isEmpty()) {
                    continue;
                }
                int equals = parameter.indexOf('=');
                request.addRawQueryParameter(equals < 0 ? parameter : parameter.substring(0, equals),
                                             equals < 0 ? null : parameter.substring(equals + 1));
            }
        }
        return request.build();
    }

    private static byte[] readResponse(URI certificateUrl, HttpBody body) {
        switch (body.kind()) {
            case EMPTY:
                throw SdkClientException.create("Response body is empty");
            case BYTES:
                return body.asByteArray();
            case STREAMING:
                try (InputStream inputStream = body.streamingBody().openStream()) {
                    return inputStream.readAllBytes();
                } catch (IOException | RuntimeException e) {
                    throw unexpected(certificateUrl, e);
                }
            default:
                throw new IllegalStateException("Unknown body kind " + body.kind());
        }
    }

    private static SdkClientException unexpected(URI certificateUrl, Exception cause) {
        return SdkClientException.create("Unexpected error while retrieving URL: " + certificateUrl, cause);
    }

    private PublicKey createPublicKey(byte[] cert) {
        try {
            CertificateFactory fact = CertificateFactory.getInstance("X.509");
            InputStream stream = new ByteArrayInputStream(cert);
            X509Certificate cer = (X509Certificate) fact.generateCertificate(stream);
            validateCertificate(cer, certCommonName);
            return cer.getPublicKey();
        } catch (CertificateExpiredException e) {
            throw SdkClientException.create("The certificate is expired", e);
        } catch (CertificateNotYetValidException e) {
            throw SdkClientException.create("The certificate is not yet valid", e);
        } catch (CertificateException e) {
            throw SdkClientException.create("The certificate could not be parsed", e);
        }
    }

    /**
     * Check that the certificate is valid and that the principal is actually SNS.
     */
    private void validateCertificate(X509Certificate cer, String expectedCommonName)
        throws CertificateExpiredException, CertificateNotYetValidException {
        if (!SigningCertificateNameVerifier.matches(expectedCommonName, cer)) {
            throw SdkClientException.create("Certificate does not match expected common name: " + expectedCommonName);
        }
        cer.checkValidity(Date.from(clock.instant()));
    }

    private static void validateCertificateData(byte[] data) {
        if (!X509_FORMAT.matcher(new String(data, StandardCharsets.UTF_8)).matches()) {
            throw SdkClientException.create("Certificate does not match expected X509 PEM format.");
        }
    }
}
