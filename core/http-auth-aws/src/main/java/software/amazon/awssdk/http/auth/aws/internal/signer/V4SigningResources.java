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

import java.nio.charset.StandardCharsets;
import java.security.DigestException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import javax.crypto.Mac;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerUtils;
import software.amazon.awssdk.http.auth.aws.internal.signer.util.SigningAlgorithm;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Per-thread scratch state for {@link FastV4HeaderSigner}: a SHA-256 {@link MessageDigest}, two text builders, byte buffers,
 * the header table, and the HMAC pad digests of the last signing key ({@link #hmacSha256}). If the digest cannot be cloned,
 * the HMAC uses the existing per-thread {@code Mac} from {@link SigningAlgorithm}.
 * <p>
 * Use {@link #acquire()} and {@link #release()} around one signing call. If the thread's instance is already in use (a
 * re-entrant sign, for example from a payload's {@code newStream()}), {@link #acquire()} returns a fresh, unshared instance.
 * Buffers that grew past {@link #MAX_RETAINED_CHARS} are dropped on release so one large request does not pin memory.
 */
@SdkInternalApi
@NotThreadSafe
final class V4SigningResources implements BiConsumer<String, List<String>> {
    private static final int INITIAL_CHARS = 1024;
    private static final int MAX_RETAINED_CHARS = 16 * 1024;
    private static final int INITIAL_HEADERS = 16;
    private static final int READ_BUFFER_SIZE = 8192;
    private static final int HMAC_BLOCK_SIZE = 64;
    private static final String HMAC_SHA256 = SigningAlgorithm.HMAC_SHA256.getAlgorithmName();

    private static final ThreadLocal<V4SigningResources> RESOURCES = ThreadLocal.withInitial(V4SigningResources::new);

    final MessageDigest sha256;
    final byte[] digestBytes = new byte[32];
    final byte[] readBuffer = new byte[READ_BUFFER_SIZE];

    /**
     * The canonical request. It is kept until the {@code Authorization} value is built, which copies the signed-headers list
     * from it.
     */
    StringBuilder canonical = new StringBuilder(INITIAL_CHARS);

    /**
     * The string to sign, then the {@code Authorization} value.
     */
    StringBuilder text = new StringBuilder(INITIAL_CHARS);

    /**
     * ASCII bytes of {@link #canonical} or {@link #text}, for the digest and the HMAC.
     */
    byte[] bytes = new byte[INITIAL_CHARS];

    /**
     * Header table: lowercase names, and values that are either a {@code String} or a {@code List<String>}.
     */
    /**
     * Where the signed-headers list is in {@link #canonical}.
     */
    int signedHeadersStart;
    int signedHeadersEnd;

    String[] headerNames = new String[INITIAL_HEADERS];
    Object[] headerValues = new Object[INITIAL_HEADERS];
    int headerCount;

    /**
     * Date strings for the last signing instant on this thread, so a second within the same second (or day) formats nothing.
     */
    long cachedEpochSecond = Long.MIN_VALUE;
    String cachedDateTime;
    long cachedEpochDay = Long.MIN_VALUE;
    String cachedDateStamp;

    final char[] hex = new char[64];

    /**
     * HMAC-SHA256 state for the last signing key used on this thread: SHA-256 digests that have already absorbed the key's
     * inner and outer pads (RFC 2104), so an HMAC clones them instead of calling {@code Mac.init} and hashing both pads
     * again. {@link #padKey} is compared by identity: {@code V4SigningKeyCache} returns the same array for the same scope,
     * and its arrays are never modified.
     */
    private byte[] padKey;
    private MessageDigest innerPad;
    private MessageDigest outerPad;
    private final byte[] innerHash = new byte[32];
    private boolean digestCloneSupported = true;

    private boolean inUse;
    private boolean skipSecurityToken;
    private boolean sawHost;

    private V4SigningResources() {
        try {
            this.sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to fetch a MessageDigest instance for SHA-256", e);
        }
    }

    static V4SigningResources acquire() {
        V4SigningResources resources = RESOURCES.get();
        if (resources.inUse) {
            resources = new V4SigningResources();
        }
        resources.inUse = true;
        return resources;
    }

    void release() {
        Arrays.fill(headerNames, 0, headerCount, null);
        Arrays.fill(headerValues, 0, headerCount, null);
        headerCount = 0;
        if (canonical.capacity() > MAX_RETAINED_CHARS) {
            canonical = new StringBuilder(INITIAL_CHARS);
        }
        if (text.capacity() > MAX_RETAINED_CHARS) {
            text = new StringBuilder(INITIAL_CHARS);
        }
        if (bytes.length > MAX_RETAINED_CHARS) {
            bytes = new byte[INITIAL_CHARS];
        }
        inUse = false;
    }

    /**
     * Copy the request's headers into the header table, in the form the legacy path's canonical request sees them after the
     * checksummer and {@link V4RequestSigner#header} have run: ignored headers are dropped, and the headers the signer
     * overwrites ({@code x-amz-content-sha256}, {@code x-amz-date}, and {@code x-amz-security-token} for session credentials)
     * are skipped so the caller can add the signer's values. A caller-supplied {@code Host} or {@code Authorization} header is
     * kept, as on the legacy path.
     *
     * @return whether the request already has a {@code Host} header
     */
    boolean collectHeaders(SdkHttpRequest request, boolean sessionCredentials) {
        this.skipSecurityToken = sessionCredentials;
        this.sawHost = false;
        request.forEachHeader(this);
        return sawHost;
    }

    @Override
    public void accept(String rawName, List<String> values) {
        String name = lowerCaseIfNeeded(rawName);
        if (V4CanonicalRequest.HEADERS_TO_IGNORE_IN_LOWER_CASE.contains(name)) {
            return;
        }
        switch (name) {
            case "x-amz-content-sha256":
            case "x-amz-date":
                return;
            case "x-amz-security-token":
                if (skipSecurityToken) {
                    return;
                }
                break;
            case "host":
                sawHost = true;
                break;
            default:
                break;
        }
        addHeader(name, values);
    }

    /**
     * {@link StringUtils#lowerCase}, skipped when the name has no character it would change.
     */
    private static String lowerCaseIfNeeded(String name) {
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if ((c >= 'A' && c <= 'Z') || c >= 0x80) {
                return StringUtils.lowerCase(name);
            }
        }
        return name;
    }

    void addHeader(String lowercaseName, Object value) {
        if (headerCount == headerNames.length) {
            headerNames = Arrays.copyOf(headerNames, headerCount * 2);
            headerValues = Arrays.copyOf(headerValues, headerCount * 2);
        }
        headerNames[headerCount] = lowercaseName;
        headerValues[headerCount] = value;
        headerCount++;
    }

    /**
     * Stable insertion sort by name, the same order as the legacy path's {@code List.sort(Comparator.comparing(Pair::left))}.
     * Requests carry few headers and arrive almost sorted (the header map is case-insensitively ordered), so this is close to
     * linear.
     */
    void sortHeaders() {
        for (int i = 1; i < headerCount; i++) {
            String name = headerNames[i];
            Object value = headerValues[i];
            int j = i - 1;
            while (j >= 0 && headerNames[j].compareTo(name) > 0) {
                headerNames[j + 1] = headerNames[j];
                headerValues[j + 1] = headerValues[j];
                j--;
            }
            headerNames[j + 1] = name;
            headerValues[j + 1] = value;
        }
    }

    /**
     * The date-time string ({@code yyyyMMdd'T'HHmmss'Z'}) for the given epoch second, formatted by {@link SignerUtils}.
     */
    String dateTime(Instant instant) {
        long epochSecond = instant.getEpochSecond();
        if (epochSecond != cachedEpochSecond) {
            cachedDateTime = SignerUtils.formatDateTime(instant);
            cachedEpochSecond = epochSecond;
        }
        return cachedDateTime;
    }

    /**
     * The date string ({@code yyyyMMdd}) for the given instant, formatted by {@link SignerUtils}.
     */
    String dateStamp(Instant instant) {
        long epochDay = Math.floorDiv(instant.getEpochSecond(), 86_400L);
        if (epochDay != cachedEpochDay) {
            cachedDateStamp = SignerUtils.formatDate(instant);
            cachedEpochDay = epochDay;
        }
        return cachedDateStamp;
    }

    /**
     * Encode the builder's content into {@link #bytes} and return the byte count. Every character of a canonical request is
     * ASCII in practice (encoded path and query, lowercase names, hex digests), so this is a narrowing copy. If a header value,
     * region or service contains a non-ASCII character, it falls back to UTF-8, which is what the legacy path hashes.
     */
    int encode(StringBuilder source) {
        int length = source.length();
        if (bytes.length < length) {
            bytes = new byte[Math.max(length, bytes.length * 2)];
        }
        for (int i = 0; i < length; i++) {
            char c = source.charAt(i);
            if (c >= 0x80) {
                byte[] utf8 = source.toString().getBytes(StandardCharsets.UTF_8);
                if (bytes.length < utf8.length) {
                    bytes = new byte[utf8.length];
                }
                System.arraycopy(utf8, 0, bytes, 0, utf8.length);
                return utf8.length;
            }
            bytes[i] = (byte) c;
        }
        return length;
    }

    /**
     * HMAC-SHA256 of {@code bytes[0, length)} with the given key, into {@link #digestBytes}. The key's pad digests are prepared
     * once per key (see {@link #padKey}) and cloned per call. If the SHA-256 implementation cannot be cloned, every call uses
     * the per-thread {@code Mac} from {@link SigningAlgorithm} with {@code Mac.init}, as the legacy path does.
     */
    byte[] hmacSha256(int length, byte[] key) {
        if (digestCloneSupported) {
            try {
                if (padKey != key) {
                    preparePads(key);
                }
                MessageDigest inner = (MessageDigest) innerPad.clone();
                inner.update(bytes, 0, length);
                inner.digest(innerHash, 0, innerHash.length);
                MessageDigest outer = (MessageDigest) outerPad.clone();
                outer.update(innerHash, 0, innerHash.length);
                outer.digest(digestBytes, 0, digestBytes.length);
                return digestBytes;
            } catch (CloneNotSupportedException e) {
                digestCloneSupported = false;
                padKey = null;
                innerPad = null;
                outerPad = null;
            } catch (DigestException e) {
                throw new RuntimeException("Unable to calculate a request signature: ", e);
            }
        }
        try {
            Mac mac = SigningAlgorithm.HMAC_SHA256.getMac();
            mac.init(new SecretKeySpec(key, HMAC_SHA256));
            mac.update(bytes, 0, length);
            mac.doFinal(digestBytes, 0);
            return digestBytes;
        } catch (InvalidKeyException | ShortBufferException e) {
            throw new RuntimeException("Unable to calculate a request signature: ", e);
        }
    }

    /**
     * Absorb the key's inner and outer pads into two fresh SHA-256 digests, as {@code Mac.init} does for HmacSHA256: a key
     * longer than the 64-byte block is hashed first, and a shorter one is zero-padded.
     */
    private void preparePads(byte[] key) throws CloneNotSupportedException {
        // Clear the reused state first, so a failure part-way leaves no pads for a different key.
        padKey = null;
        sha256.reset();
        byte[] k = key.length > HMAC_BLOCK_SIZE ? sha256.digest(key) : key;
        byte[] ipad = new byte[HMAC_BLOCK_SIZE];
        byte[] opad = new byte[HMAC_BLOCK_SIZE];
        for (int i = 0; i < HMAC_BLOCK_SIZE; i++) {
            byte b = i < k.length ? k[i] : 0;
            ipad[i] = (byte) (b ^ 0x36);
            opad[i] = (byte) (b ^ 0x5c);
        }
        MessageDigest inner = (MessageDigest) sha256.clone();
        inner.update(ipad);
        MessageDigest outer = (MessageDigest) sha256.clone();
        outer.update(opad);
        Arrays.fill(ipad, (byte) 0);
        Arrays.fill(opad, (byte) 0);
        innerPad = inner;
        outerPad = outer;
        padKey = key;
    }
}
