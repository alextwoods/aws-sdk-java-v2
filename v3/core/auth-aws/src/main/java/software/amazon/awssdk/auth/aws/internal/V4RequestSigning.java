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

package software.amazon.awssdk.auth.aws.internal;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import software.amazon.awssdk.auth.api.HttpSigner;
import software.amazon.awssdk.auth.api.PayloadSigningMode;
import software.amazon.awssdk.auth.api.SignRequest;
import software.amazon.awssdk.auth.aws.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.auth.aws.AwsV4FamilyHttpSigner.AuthLocation;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpHeaders;
import software.amazon.awssdk.http.HttpQueryParameter;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.UriEncoding;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * The SigV4-family request signing algorithm shared by SigV4, SigV4a and S3 Express: canonical request, string to sign
 * and header or query-string placement. The algorithm-specific parts (credential scope, signature function, extra
 * signed values) come from {@link Spec}. Not a public API.
 */
public final class V4RequestSigning {
    public static final String X_AMZ_CONTENT_SHA256 = "x-amz-content-sha256";
    public static final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";
    /** The payload hash of a SigV4 signed event stream request (V2 {@code STREAMING_EVENTS_PAYLOAD}). */
    public static final String STREAMING_EVENTS_PAYLOAD = "STREAMING-AWS4-HMAC-SHA256-EVENTS";
    /** The content type of an event stream body. */
    public static final String EVENT_STREAM_CONTENT_TYPE = "application/vnd.amazon.eventstream";
    public static final Duration DEFAULT_EXPIRATION = Duration.ofMinutes(15);
    public static final Duration MAX_EXPIRATION = Duration.ofDays(7);

    private static final Set<String> IGNORED_HEADERS =
        Set.of("connection", "x-amzn-trace-id", "user-agent", "expect", "transfer-encoding", "x-forwarded-for");
    private static final DateTimeFormatter AMZ_DATE =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.ROOT).withZone(ZoneOffset.UTC);

    private V4RequestSigning() {
    }

    /**
     * The algorithm-specific inputs.
     *
     * @param algorithm e.g. {@code AWS4-HMAC-SHA256}
     * @param scopeRegion the credential-scope region, or null for a region-less scope (SigV4a)
     * @param securityTokenName the header/query name of the session token, or null for none
     * @param securityToken the session token, or null
     * @param extraSigned additional signed values (name → value), as headers or, when presigning, query parameters
     * @param signature maps (string to sign, date stamp, credential scope) to the hex signature
     */
    public record Spec(String algorithm, String scopeRegion, String securityTokenName, String securityToken,
                       Map<String, String> extraSigned, SignatureFunction signature) {
    }

    /** Computes the hex signature of a string to sign. */
    @FunctionalInterface
    public interface SignatureFunction {
        String sign(String stringToSign, String dateStamp, String serviceName, String scopeRegion);
    }

    /** The outcome of signing. */
    public record Result(SdkHttpRequest request, String signature, String canonicalRequest, String stringToSign) {
    }

    /** Signs {@code signRequest} per {@code spec}. */
    public static Result sign(SignRequest<? extends AwsCredentialsIdentity> signRequest, Spec spec) {
        AwsCredentialsIdentity credentials = signRequest.identity();
        String service = signRequest.requireProperty(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME);
        boolean doubleEncode = signRequest.requireProperty(AwsV4FamilyHttpSigner.DOUBLE_URL_ENCODE, true);
        boolean normalize = signRequest.requireProperty(AwsV4FamilyHttpSigner.NORMALIZE_PATH, true);
        AuthLocation location = signRequest.requireProperty(AwsV4FamilyHttpSigner.AUTH_LOCATION, AuthLocation.HEADER);
        Clock clock = signRequest.requireProperty(HttpSigner.SIGNING_CLOCK, Clock.systemUTC());
        Instant now = clock.instant();
        String amzDate = AMZ_DATE.format(now);
        String dateStamp = amzDate.substring(0, 8);
        String scope = dateStamp + "/" + (spec.scopeRegion() == null ? "" : spec.scopeRegion() + "/") + service
                       + "/aws4_request";

        SdkHttpRequest.Builder builder = signRequest.request().toBuilder();
        if (!builder.hasHeader("Host")) {
            builder.putHeader("Host", hostHeader(signRequest.request()));
        }
        String contentHash = contentHash(signRequest);

        if (location == AuthLocation.HEADER) {
            builder.putHeader("X-Amz-Date", amzDate);
            if (spec.securityToken() != null) {
                builder.putHeader(spec.securityTokenName(), spec.securityToken());
            }
            spec.extraSigned().forEach(builder::putHeader);
            builder.putHeader(X_AMZ_CONTENT_SHA256, contentHash);
            SdkHttpRequest unsigned = builder.build();
            Canonical canonical = canonicalRequest(unsigned, contentHash, doubleEncode, normalize);
            String stringToSign = stringToSign(spec.algorithm(), amzDate, scope, canonical.text());
            String signature = spec.signature().sign(stringToSign, dateStamp, service, spec.scopeRegion());
            builder.putHeader("Authorization", spec.algorithm() + " Credential=" + credentials.accessKeyId() + "/" + scope
                                               + ", SignedHeaders=" + canonical.signedHeaders() + ", Signature="
                                               + signature);
            return new Result(builder.build(), signature, canonical.text(), stringToSign);
        }

        Duration expiration = signRequest.requireProperty(AwsV4FamilyHttpSigner.EXPIRATION_DURATION, DEFAULT_EXPIRATION);
        if (expiration.isNegative() || expiration.compareTo(MAX_EXPIRATION) > 0) {
            throw SdkClientException.create("The presigned request expiration must not be negative or exceed 7 days, "
                                            + "but was " + expiration + ".");
        }
        builder.removeHeader(X_AMZ_CONTENT_SHA256);
        String signedHeaders = canonicalHeaders(builder.build().headers()).signedHeaders();
        if (spec.securityToken() != null) {
            builder.addQueryParameter(spec.securityTokenName(), spec.securityToken());
        }
        builder.addQueryParameter("X-Amz-Algorithm", spec.algorithm());
        builder.addQueryParameter("X-Amz-Credential", credentials.accessKeyId() + "/" + scope);
        builder.addQueryParameter("X-Amz-Date", amzDate);
        builder.addQueryParameter("X-Amz-Expires", Long.toString(expiration.getSeconds()));
        builder.addQueryParameter("X-Amz-SignedHeaders", signedHeaders);
        spec.extraSigned().forEach(builder::addQueryParameter);
        SdkHttpRequest unsigned = builder.build();
        Canonical canonical = canonicalRequest(unsigned, contentHash, doubleEncode, normalize);
        String stringToSign = stringToSign(spec.algorithm(), amzDate, scope, canonical.text());
        String signature = spec.signature().sign(stringToSign, dateStamp, service, spec.scopeRegion());
        builder.addQueryParameter("X-Amz-Signature", signature);
        return new Result(builder.build(), signature, canonical.text(), stringToSign);
    }

    /**
     * The payload hash: a precomputed {@code x-amz-content-sha256}, the event stream value, {@code UNSIGNED-PAYLOAD},
     * or the payload's SHA-256 (of an in-memory payload, or the pipeline's {@link HttpSigner#PAYLOAD_SHA256} of a
     * streaming one).
     */
    static String contentHash(SignRequest<?> signRequest) {
        SdkHttpRequest request = signRequest.request();
        var precomputed = request.headers().firstValue(X_AMZ_CONTENT_SHA256);
        if (precomputed.isPresent()) {
            return precomputed.get();
        }
        HttpBody body = request.body();
        if (isEventStream(request)) {
            // V2 parity: the frames are signed as they are sent, chained from this request's signature.
            return STREAMING_EVENTS_PAYLOAD;
        }
        if (!payloadSigned(signRequest)) {
            return UNSIGNED_PAYLOAD;
        }
        return switch (body.kind()) {
            case EMPTY -> SigningCrypto.EMPTY_SHA256;
            case BYTES -> SigningCrypto.hex(SigningCrypto.sha256(body.asByteBuffer()));
            case STREAMING -> {
                String supplied = signRequest.property(HttpSigner.PAYLOAD_SHA256);
                if (supplied == null) {
                    // The signer never reads a streaming body; the pipeline hashes a replayable one before signing.
                    throw SdkClientException.create(
                        "The streaming payload must be hashed into the signature, but no payload hash was supplied: "
                        + "the payload is not replayable, so it cannot be read before it is sent. Use a replayable "
                        + "request body, disable payload signing (HTTPS only), or supply the payload hash in "
                        + X_AMZ_CONTENT_SHA256 + ".");
                }
                yield supplied;
            }
        };
    }

    /**
     * How the signer covers the payload of {@code signRequest} ({@link AwsV4FamilyHttpSigner#payloadSigningMode}):
     * empty for a precomputed payload hash or an event stream, else whether {@link #contentHash} hashes the payload.
     */
    public static Optional<PayloadSigningMode> payloadSigningMode(SignRequest<?> signRequest) {
        SdkHttpRequest request = signRequest.request();
        if (request.headers().firstValue(X_AMZ_CONTENT_SHA256).isPresent() || isEventStream(request)) {
            return Optional.empty();
        }
        return Optional.of(payloadSigned(signRequest) ? PayloadSigningMode.PAYLOAD_HASH : PayloadSigningMode.UNSIGNED);
    }

    /**
     * Whether the payload is hashed into the signature: payload signing is enabled, or (V2 behavior) the request is
     * plain HTTP with a payload, which is always signed.
     */
    private static boolean payloadSigned(SignRequest<?> signRequest) {
        SdkHttpRequest request = signRequest.request();
        boolean enabled = signRequest.requireProperty(AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED, true);
        boolean https = "https".equals(request.scheme());
        return enabled || !(https || request.body().kind() == HttpBody.Kind.EMPTY);
    }

    /**
     * Whether {@code request} carries an input event stream: a streaming body of content type
     * {@value #EVENT_STREAM_CONTENT_TYPE} (by its {@code Content-Type} header, as V2, or the body's own type). Its
     * payload hash is {@value #STREAMING_EVENTS_PAYLOAD}, and its frames are signed when sent.
     */
    public static boolean isEventStream(SdkHttpRequest request) {
        HttpBody body = request.body();
        if (body.kind() != HttpBody.Kind.STREAMING) {
            return false;
        }
        String contentType = request.headers().firstValue("Content-Type")
                                    .or(() -> body.streamingBody().contentType())
                                    .orElse("");
        return EVENT_STREAM_CONTENT_TYPE.equalsIgnoreCase(contentType.strip());
    }

    /** The string to sign. */
    public static String stringToSign(String algorithm, String amzDate, String scope, String canonicalRequest) {
        return algorithm + "\n" + amzDate + "\n" + scope + "\n" + SigningCrypto.hex(SigningCrypto.sha256(canonicalRequest));
    }

    /** The canonical request and signed-header list. */
    public record Canonical(String text, String signedHeaders) {
    }

    /** The canonical request of {@code request} with payload hash {@code contentHash}. */
    public static Canonical canonicalRequest(SdkHttpRequest request, String contentHash, boolean doubleEncode,
                                             boolean normalize) {
        CanonicalHeaders headers = canonicalHeaders(request.headers());
        String text = request.method() + "\n"
                      + canonicalUri(request.encodedPath(), doubleEncode, normalize) + "\n"
                      + canonicalQuery(request.queryParameters()) + "\n"
                      + headers.text() + "\n"
                      + headers.signedHeaders() + "\n"
                      + contentHash;
        return new Canonical(text, headers.signedHeaders());
    }

    static String canonicalUri(String encodedPath, boolean doubleEncode, boolean normalize) {
        String path = encodedPath.isEmpty() ? "/" : encodedPath;
        if (normalize) {
            path = removeDotSegments(path);
        }
        return doubleEncode ? UriEncoding.encode(path, true) : path;
    }

    /** RFC 3986 section 5.2.4 dot-segment removal of an absolute path; empty segments are kept. */
    static String removeDotSegments(String path) {
        String[] segments = path.split("/", -1);
        List<String> out = new ArrayList<>();
        for (int i = 1; i < segments.length; i++) {
            String segment = segments[i];
            boolean last = i == segments.length - 1;
            if (segment.equals(".")) {
                if (last) {
                    out.add("");
                }
            } else if (segment.equals("..")) {
                if (!out.isEmpty()) {
                    out.remove(out.size() - 1);
                }
                if (last) {
                    out.add("");
                }
            } else {
                out.add(segment);
            }
        }
        return "/" + String.join("/", out);
    }

    static String canonicalQuery(List<HttpQueryParameter> parameters) {
        if (parameters.isEmpty()) {
            return "";
        }
        List<String[]> entries = new ArrayList<>(parameters.size());
        for (HttpQueryParameter parameter : parameters) {
            String name = UriEncoding.encode(decode(parameter.rawName()));
            String value = parameter.rawValue() == null ? "" : UriEncoding.encode(decode(parameter.rawValue()));
            entries.add(new String[] {name, value});
        }
        entries.sort(Comparator.<String[], String>comparing(e -> e[0]).thenComparing(e -> e[1]));
        StringBuilder out = new StringBuilder();
        for (String[] entry : entries) {
            if (!out.isEmpty()) {
                out.append('&');
            }
            out.append(entry[0]).append('=').append(entry[1]);
        }
        return out.toString();
    }

    private record CanonicalHeaders(String text, String signedHeaders) {
    }

    private static CanonicalHeaders canonicalHeaders(HttpHeaders headers) {
        Map<String, List<String>> byName = new TreeMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String name = headers.name(i).toLowerCase(Locale.ROOT);
            if (!IGNORED_HEADERS.contains(name)) {
                byName.computeIfAbsent(name, n -> new ArrayList<>()).add(headers.value(i).strip().replaceAll("\\s+", " "));
            }
        }
        StringBuilder text = new StringBuilder();
        StringBuilder signed = new StringBuilder();
        byName.forEach((name, values) -> {
            text.append(name).append(':').append(String.join(",", values)).append('\n');
            if (!signed.isEmpty()) {
                signed.append(';');
            }
            signed.append(name);
        });
        return new CanonicalHeaders(text.toString(), signed.toString());
    }

    private static String hostHeader(SdkHttpRequest request) {
        int port = request.port();
        boolean standard = port < 0 || (port == 443 && "https".equals(request.scheme()))
                           || (port == 80 && "http".equals(request.scheme()));
        return standard ? request.host() : request.host() + ":" + port;
    }

    /** Percent-decodes {@code text} (UTF-8); {@code +} is kept literally. */
    static String decode(String text) {
        if (text.indexOf('%') < 0) {
            return text;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '%' && i + 2 < text.length() && isHex(text.charAt(i + 1)) && isHex(text.charAt(i + 2))) {
                out.write(Character.digit(text.charAt(i + 1), 16) << 4 | Character.digit(text.charAt(i + 2), 16));
                i += 2;
            } else {
                byte[] bytes = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
                if (Character.isHighSurrogate(c) && i + 1 < text.length()) {
                    bytes = text.substring(i, i + 2).getBytes(StandardCharsets.UTF_8);
                    i++;
                }
                out.writeBytes(bytes);
            }
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    private static boolean isHex(char c) {
        return Character.digit(c, 16) >= 0;
    }
}
