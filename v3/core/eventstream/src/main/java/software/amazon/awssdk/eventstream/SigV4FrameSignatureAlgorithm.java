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

package software.amazon.awssdk.eventstream;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * The SigV4 event stream frame signature ({@code AWS4-HMAC-SHA256-PAYLOAD}):
 *
 * <pre>
 * stringToSign = "AWS4-HMAC-SHA256-PAYLOAD" + "\n"
 *              + signingTime as yyyyMMdd'T'HHmmss'Z' (UTC) + "\n"
 *              + yyyyMMdd + "/" + region + "/" + service + "/aws4_request" + "\n"
 *              + hex(priorSignature) + "\n"
 *              + hex(SHA-256(encoded non-signature headers)) + "\n"
 *              + hex(SHA-256(payload))
 * signature    = HMAC-SHA256(signingKey(yyyyMMdd), stringToSign)
 * </pre>
 *
 * where the scope date is the signing time's UTC date and the signing key comes from the {@link SigningKeyProvider}
 * (the seam to the SigV4 implementation in {@code auth-aws}). The key of the current scope date is cached; a stream
 * that crosses a UTC midnight requests the next day's key once.
 *
 * <p>Thread safe; intended to be created per stream (per identity).
 */
public final class SigV4FrameSignatureAlgorithm implements FrameSignatureAlgorithm {
    /** The SigV4 event stream string-to-sign algorithm. */
    public static final String ALGORITHM = "AWS4-HMAC-SHA256-PAYLOAD";

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SHA256 = "SHA-256";
    private static final String TERMINATOR = "aws4_request";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                                                                        .withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final HexFormat HEX = HexFormat.of();

    private final String region;
    private final String service;
    private final SigningKeyProvider keys;

    // Guarded by this.
    private LocalDate keyDate;
    private Mac mac;
    private MessageDigest digest;

    private SigV4FrameSignatureAlgorithm(String region, String service, SigningKeyProvider keys) {
        this.region = scopeComponent(region, "region");
        this.service = scopeComponent(service, "service");
        this.keys = Objects.requireNonNull(keys, "keys");
    }

    /**
     * The SigV4 frame signature for the credential scope {@code region}/{@code service} (the signing region and
     * signing name that signed the initial request) with keys from {@code keys}.
     *
     * @throws IllegalArgumentException if {@code region} or {@code service} is empty or contains {@code /} or a
     *         line break
     */
    public static SigV4FrameSignatureAlgorithm create(String region, String service, SigningKeyProvider keys) {
        return new SigV4FrameSignatureAlgorithm(region, service, keys);
    }

    private static String scopeComponent(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isEmpty() || value.indexOf('/') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("The SigV4 " + name + " must be non-empty without '/' or line breaks");
        }
        return value;
    }

    /** The credential-scope region. */
    public String region() {
        return region;
    }

    /** The credential-scope service (signing name). */
    public String service() {
        return service;
    }

    /** The credential scope {@code yyyyMMdd/region/service/aws4_request} of {@code signingTime}. */
    public String scope(Instant signingTime) {
        return scope(signingTime.atOffset(ZoneOffset.UTC).toLocalDate());
    }

    private String scope(LocalDate date) {
        return DATE.format(date) + "/" + region + "/" + service + "/" + TERMINATOR;
    }

    /** The string to sign of one frame (see the class documentation); exposed for verification and diagnostics. */
    public String stringToSign(byte[] priorSignature, Instant signingTime, byte[] headers, ByteBuffer payload) {
        Objects.requireNonNull(priorSignature, "priorSignature");
        Objects.requireNonNull(signingTime, "signingTime");
        Objects.requireNonNull(headers, "headers");
        Objects.requireNonNull(payload, "payload");
        MessageDigest sha256 = newDigest();
        return stringToSign(sha256, priorSignature, signingTime, headers, payload);
    }

    @Override
    public synchronized byte[] sign(byte[] priorSignature, Instant signingTime, byte[] headers, ByteBuffer payload) {
        Objects.requireNonNull(priorSignature, "priorSignature");
        Objects.requireNonNull(signingTime, "signingTime");
        Objects.requireNonNull(headers, "headers");
        Objects.requireNonNull(payload, "payload");
        if (digest == null) {
            digest = newDigest();
        }
        String stringToSign = stringToSign(digest, priorSignature, signingTime, headers, payload);
        Mac hmac = hmac(signingTime.atOffset(ZoneOffset.UTC).toLocalDate());
        return hmac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
    }

    private String stringToSign(MessageDigest sha256, byte[] priorSignature, Instant signingTime, byte[] headers,
                                ByteBuffer payload) {
        sha256.reset();
        String headersHash = HEX.formatHex(sha256.digest(headers));
        sha256.update(payload);
        String payloadHash = HEX.formatHex(sha256.digest());
        return ALGORITHM + '\n'
               + DATE_TIME.format(signingTime) + '\n'
               + scope(signingTime) + '\n'
               + HEX.formatHex(priorSignature) + '\n'
               + headersHash + '\n'
               + payloadHash;
    }

    /** The HMAC keyed with the signing key of {@code date}, requesting the key when the scope date changes. */
    private Mac hmac(LocalDate date) {
        try {
            if (!date.equals(keyDate)) {
                byte[] signingKey = keys.signingKey(date);
                if (signingKey == null || signingKey.length == 0) {
                    throw new IllegalStateException("The signing key provider returned no key");
                }
                if (mac == null) {
                    mac = Mac.getInstance(HMAC_SHA256);
                }
                keyDate = date;
                mac.init(new SecretKeySpec(signingKey, HMAC_SHA256));
            }
            return mac;
        } catch (GeneralSecurityException e) {
            keyDate = null;
            throw new IllegalStateException("HMAC-SHA256 is unavailable", e);
        } catch (RuntimeException e) {
            keyDate = null;
            throw e;
        }
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance(SHA256);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    @Override
    public String toString() {
        return "SigV4FrameSignatureAlgorithm[region=" + region + ", service=" + service + "]";
    }
}
