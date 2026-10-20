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

package software.amazon.awssdk.http.auth.aws.internal.signer;

import static software.amazon.awssdk.http.auth.aws.internal.signer.util.ChecksumUtil.isEventStreaming;
import static software.amazon.awssdk.http.auth.aws.internal.signer.util.ChecksumUtil.isPayloadSigning;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner.AUTH_LOCATION;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner.CHECKSUM_ALGORITHM;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner.CHUNK_ENCODING_ENABLED;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner.DOUBLE_URL_ENCODE;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner.EXPIRATION_DURATION;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner.NORMALIZE_PATH;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner.REGION_NAME;
import static software.amazon.awssdk.http.auth.aws.signer.SignerConstant.AUTHORIZATION;
import static software.amazon.awssdk.http.auth.aws.signer.SignerConstant.AWS4_SIGNING_ALGORITHM;
import static software.amazon.awssdk.http.auth.aws.signer.SignerConstant.AWS4_TERMINATOR;
import static software.amazon.awssdk.http.auth.aws.signer.SignerConstant.HOST;
import static software.amazon.awssdk.http.auth.aws.signer.SignerConstant.UNSIGNED_PAYLOAD;
import static software.amazon.awssdk.http.auth.aws.signer.SignerConstant.X_AMZ_CONTENT_SHA256;
import static software.amazon.awssdk.http.auth.aws.signer.SignerConstant.X_AMZ_DATE;
import static software.amazon.awssdk.http.auth.aws.signer.SignerConstant.X_AMZ_SECURITY_TOKEN;
import static software.amazon.awssdk.http.auth.spi.signer.HttpSigner.SIGNING_CLOCK;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.DigestException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.internal.signer.util.CredentialUtils;
import software.amazon.awssdk.http.auth.aws.internal.signer.util.V4SigningKeyCache;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner.AuthLocation;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * The SigV4 header-signing fast path used by {@link DefaultAwsV4HttpSigner#sign} for the common synchronous case: header auth,
 * non-anonymous credentials, no presigning, no chunk encoding, no flexible checksum and no event streaming. In that case the
 * legacy path composes {@code Checksummer.create()} (or the {@code UNSIGNED-PAYLOAD} constant), {@link V4RequestSigner#header}
 * and the pass-through {@link V4PayloadSigner}; this class computes the same request in one pass.
 * <p>
 * The output is identical to the legacy path: the same headers (including a caller-supplied {@code Host} or
 * {@code Authorization} header, which the legacy path signs), the same header-map key casing (the same {@code putHeader}
 * calls in the same order), and the same payload object. The URI and query canonicalization reuse the legacy
 * {@link V4CanonicalRequest} helpers. The differences are in cost:
 * <ul>
 *     <li>no {@code V4Properties}, {@code CredentialScope}, {@code Checksummer}, {@code V4CanonicalRequest},
 *     {@code Pair} list or intermediate {@code SdkHttpRequest} per call;</li>
 *     <li>the SHA-256 digest, text builders and byte buffers are reused per thread ({@link V4SigningResources}), and the
 *     canonical request and string to sign are hashed from those buffers without {@code String.getBytes};</li>
 *     <li>the signing key comes from {@link V4SigningKeyCache}, with no string key built per call;</li>
 *     <li>the signature HMAC starts from the signing key's inner and outer pad digests, prepared once per key and thread
 *     ({@link V4SigningResources#hmacSha256}), instead of a {@code Mac.init} per call (smithy-java {@code c62499ffd}).</li>
 * </ul>
 * <p>
 * When DEBUG logging is enabled for {@link DefaultV4RequestSigner}, the legacy path runs instead, so its canonical-request
 * and string-to-sign log lines are unchanged.
 */
@SdkInternalApi
final class FastV4HeaderSigner {
    private static final Logger REQUEST_SIGNER_LOG = Logger.loggerFor(DefaultV4RequestSigner.class);

    /**
     * SHA-256 of the empty payload, the legacy path's hash for a request with no payload.
     */
    private static final String EMPTY_PAYLOAD_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    /**
     * Two lowercase hex digits per byte value: entries {@code 2 * v} and {@code 2 * v + 1} are the digits of {@code v}.
     */
    private static final char[] HEX2 = new char[512];

    static {
        char[] digits = "0123456789abcdef".toCharArray();
        for (int v = 0; v < 256; v++) {
            HEX2[v << 1] = digits[v >>> 4];
            HEX2[(v << 1) + 1] = digits[v & 0x0F];
        }
    }

    /**
     * Canonical URI options, indexed by {@code (doubleUrlEncode ? 2 : 0) + (normalizePath ? 1 : 0)}.
     */
    private static final V4CanonicalRequest.Options[] URI_OPTIONS = {
        new V4CanonicalRequest.Options(false, false),
        new V4CanonicalRequest.Options(false, true),
        new V4CanonicalRequest.Options(true, false),
        new V4CanonicalRequest.Options(true, true)
    };

    private FastV4HeaderSigner() {
    }

    /**
     * Whether the fast path produces this request's signature. Anything else (query or presigned auth, anonymous credentials,
     * chunk encoding, a checksum algorithm, event streaming, or the header-auth/expiration error case) stays on the legacy
     * path.
     */
    static boolean canSign(SignRequest<? extends AwsCredentialsIdentity> request) {
        return !CredentialUtils.isAnonymous(request.identity())
               && request.requireProperty(AUTH_LOCATION, AuthLocation.HEADER) == AuthLocation.HEADER
               && request.property(EXPIRATION_DURATION) == null
               && !request.requireProperty(CHUNK_ENCODING_ENABLED, false)
               && !request.hasProperty(CHECKSUM_ALGORITHM)
               && !isEventStreaming(request.request())
               && !REQUEST_SIGNER_LOG.isLoggingLevelEnabled("debug");
    }

    /**
     * Sign a request for which {@link #canSign} returned {@code true}.
     */
    static SignedRequest sign(SignRequest<? extends AwsCredentialsIdentity> request) {
        boolean payloadSigning = isPayloadSigning(request);
        V4SigningResources resources = V4SigningResources.acquire();
        try {
            return sign(request, payloadSigning, resources);
        } finally {
            resources.release();
        }
    }

    private static SignedRequest sign(SignRequest<? extends AwsCredentialsIdentity> request,
                                      boolean payloadSigning,
                                      V4SigningResources r) {
        // The same inputs as DefaultAwsV4HttpSigner.v4Properties. Trimming the credential strings is what
        // CredentialUtils.sanitizeCredentials does, without allocating a new identity.
        Clock signingClock = request.requireProperty(SIGNING_CLOCK, Clock.systemUTC());
        Instant signingInstant = signingClock.instant();
        AwsCredentialsIdentity identity = request.identity();
        boolean sessionCredentials = identity instanceof AwsSessionCredentialsIdentity;
        String sessionToken = sessionCredentials
                              ? StringUtils.trim(((AwsSessionCredentialsIdentity) identity).sessionToken())
                              : null;
        SdkHttpRequest source = request.request();
        ContentStreamProvider payload = request.payload().orElse(null);
        String contentSha256 = payloadSigning ? hashPayload(payload, r) : UNSIGNED_PAYLOAD;
        String dateTime = r.dateTime(signingInstant);

        // Headers as the legacy canonical request sees them: the source headers, minus the ones the signer overwrites, plus
        // the signer's values.
        boolean hasHost = r.collectHeaders(source, sessionCredentials);
        r.addHeader("x-amz-content-sha256", contentSha256);
        r.addHeader("x-amz-date", dateTime);
        if (sessionCredentials) {
            r.addHeader("x-amz-security-token", sessionToken);
        }
        String hostHeader = hasHost ? null : hostHeader(source);
        if (!hasHost) {
            r.addHeader("host", hostHeader);
        }
        r.sortHeaders();

        buildCanonicalRequest(r, source, contentSha256,
                              uriOptions(request.requireProperty(DOUBLE_URL_ENCODE, true),
                                         request.requireProperty(NORMALIZE_PATH, true)));

        String authorization = authorization(r, identity, signingInstant, dateTime,
                                             request.requireProperty(REGION_NAME),
                                             request.requireProperty(SERVICE_SIGNING_NAME));

        // The same putHeader calls, in the same order, as the checksummer and V4RequestSigner.header on the legacy path.
        SdkHttpRequest.Builder builder = source.toBuilder();
        builder.putHeader(X_AMZ_CONTENT_SHA256, contentSha256);
        if (sessionCredentials) {
            builder.putHeader(X_AMZ_SECURITY_TOKEN, sessionToken);
        }
        if (!hasHost) {
            builder.putHeader(HOST, hostHeader);
        }
        builder.putHeader(X_AMZ_DATE, dateTime);
        builder.putHeader(AUTHORIZATION, authorization);

        return SignedRequest.builder()
                            .request(builder.build())
                            .payload(payload)
                            .build();
    }

    /**
     * Write the canonical request into {@link V4SigningResources#canonical}, recording where the signed-headers list is.
     */
    private static void buildCanonicalRequest(V4SigningResources r,
                                              SdkHttpRequest source,
                                              String contentSha256,
                                              V4CanonicalRequest.Options uriOptions) {
        StringBuilder canonical = r.canonical;
        canonical.setLength(0);
        canonical.append(source.method().toString()).append('\n');
        canonical.append(V4CanonicalRequest.getCanonicalUri(source, uriOptions)).append('\n');
        if (source.numRawQueryParameters() != 0) {
            SdkHttpUtils.flattenQueryParameters(canonical, V4CanonicalRequest.getCanonicalQueryParams(source));
        }
        canonical.append('\n');
        appendCanonicalHeaders(r, canonical);
        canonical.append('\n');
        r.signedHeadersStart = canonical.length();
        appendSignedHeaders(r, canonical);
        r.signedHeadersEnd = canonical.length();
        canonical.append('\n').append(contentSha256);
    }

    /**
     * Hash the canonical request, sign the string to sign and return the {@code Authorization} header value.
     */
    private static String authorization(V4SigningResources r,
                                        AwsCredentialsIdentity identity,
                                        Instant signingInstant,
                                        String dateTime,
                                        String region,
                                        String service) {
        String dateStamp = r.dateStamp(signingInstant);

        StringBuilder text = r.text;
        text.setLength(0);
        text.append(AWS4_SIGNING_ALGORITHM).append('\n')
            .append(dateTime).append('\n');
        appendScope(text, dateStamp, region, service);
        text.append('\n');
        appendHex(text, sha256(r, r.encode(r.canonical)));

        byte[] signingKey = V4SigningKeyCache.signingKey(StringUtils.trim(identity.secretAccessKey()), dateStamp, region,
                                                         service);
        byte[] signature = r.hmacSha256(r.encode(text), signingKey);

        text.setLength(0);
        text.append(AWS4_SIGNING_ALGORITHM)
            .append(" Credential=").append(StringUtils.trim(identity.accessKeyId())).append('/');
        appendScope(text, dateStamp, region, service);
        text.append(", SignedHeaders=").append(r.canonical, r.signedHeadersStart, r.signedHeadersEnd)
            .append(", Signature=");
        appendHex(text, signature);
        return text.toString();
    }

    private static V4CanonicalRequest.Options uriOptions(boolean doubleUrlEncode, boolean normalizePath) {
        return URI_OPTIONS[(doubleUrlEncode ? 2 : 0) + (normalizePath ? 1 : 0)];
    }

    /**
     * The legacy {@code SignerUtils.addHostHeader} value: the host, with the port when it is not the scheme's default.
     */
    private static String hostHeader(SdkHttpRequest request) {
        if (SdkHttpUtils.isUsingStandardPort(request.protocol(), request.port())) {
            return request.host();
        }
        return request.host() + ":" + request.port();
    }

    /**
     * The legacy {@code V4CanonicalRequest.getCanonicalHeadersString}: one line per entry, values trimmed and comma-joined.
     * An entry with an empty value list loses its colon there, and here.
     */
    private static void appendCanonicalHeaders(V4SigningResources r, StringBuilder out) {
        for (int i = 0; i < r.headerCount; i++) {
            out.append(r.headerNames[i]).append(':');
            Object value = r.headerValues[i];
            if (value instanceof String) {
                V4CanonicalRequest.addAndTrim(out, (String) value);
            } else {
                @SuppressWarnings("unchecked")
                List<String> values = (List<String>) value;
                for (int j = 0; j < values.size(); j++) {
                    V4CanonicalRequest.addAndTrim(out, values.get(j));
                    out.append(',');
                }
                out.setLength(out.length() - 1);
            }
            out.append('\n');
        }
    }

    private static void appendSignedHeaders(V4SigningResources r, StringBuilder out) {
        for (int i = 0; i < r.headerCount; i++) {
            if (i > 0) {
                out.append(';');
            }
            out.append(r.headerNames[i]);
        }
    }

    private static void appendScope(StringBuilder out, String dateStamp, String region, String service) {
        out.append(dateStamp).append('/').append(region).append('/').append(service).append('/').append(AWS4_TERMINATOR);
    }

    /**
     * Hash the payload the way the legacy {@code FlexibleChecksummer} does (read the whole stream, which is not closed), into
     * the lowercase-hex {@code x-amz-content-sha256} value.
     */
    private static String hashPayload(ContentStreamProvider payload, V4SigningResources r) {
        if (payload == null) {
            return EMPTY_PAYLOAD_SHA256;
        }
        InputStream stream;
        try {
            stream = payload.newStream();
        } catch (Exception e) {
            throw new RuntimeException("Unable to read request payload to sign request: ", e);
        }
        MessageDigest digest = r.sha256;
        digest.reset();
        byte[] buffer = r.readBuffer;
        try {
            int read;
            while ((read = stream.read(buffer)) > -1) {
                digest.update(buffer, 0, read);
            }
            digest.digest(r.digestBytes, 0, r.digestBytes.length);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (DigestException e) {
            throw new IllegalStateException("Unable to compute the SHA-256 hash of the request payload", e);
        }
        char[] hex = r.hex;
        for (int i = 0; i < r.digestBytes.length; i++) {
            int v = (r.digestBytes[i] & 0xFF) << 1;
            hex[2 * i] = HEX2[v];
            hex[2 * i + 1] = HEX2[v + 1];
        }
        return new String(hex);
    }

    private static byte[] sha256(V4SigningResources r, int length) {
        MessageDigest digest = r.sha256;
        digest.reset();
        digest.update(r.bytes, 0, length);
        try {
            digest.digest(r.digestBytes, 0, r.digestBytes.length);
        } catch (DigestException e) {
            throw new RuntimeException("Unable to compute hash while signing request: ", e);
        }
        return r.digestBytes;
    }

    private static void appendHex(StringBuilder out, byte[] bytes) {
        for (byte b : bytes) {
            int v = (b & 0xFF) << 1;
            out.append(HEX2[v]).append(HEX2[v + 1]);
        }
    }
}
