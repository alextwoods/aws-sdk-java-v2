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

package software.amazon.awssdk.http.auth.aws.internal.signer.util;

import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.http.auth.aws.signer.SignerConstant;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.cache.bounded.BoundedCache;

/**
 * A bounded, thread-safe cache of derived SigV4 signing keys, used by the header-signing fast path.
 * <p>
 * Entries are keyed by (secret access key, UTC date stamp, region, service). Because the date is part of the key, an entry
 * never needs a validity check: a new day produces a new key, and the old entry ages out through the cache's eviction. A hit is
 * a lock-free {@code ConcurrentHashMap} read in {@link BoundedCache}, with no string concatenation for the key.
 * <p>
 * The derived key is the same four-step HMAC chain as {@link SignerUtils#deriveSigningKey}, which keeps its own cache for the
 * legacy signing path. Returned arrays are shared with the cache and must not be modified.
 */
@SdkInternalApi
@ThreadSafe
public final class V4SigningKeyCache {
    private static final Logger LOG = Logger.loggerFor(V4SigningKeyCache.class);

    /**
     * The same bound as the legacy signer's key cache in {@link SignerUtils}.
     */
    private static final int MAX_SIZE = 300;

    private static final BoundedCache<Key, byte[]> CACHE = BoundedCache.builder(V4SigningKeyCache::newSigningKey)
                                                                       .maxSize(MAX_SIZE)
                                                                       .build();

    private V4SigningKeyCache() {
    }

    /**
     * Return the signing key for the given scope, deriving and caching it on a miss.
     *
     * @param secretAccessKey the (already trimmed) secret access key
     * @param dateStamp the UTC date of the signing instant, in {@code yyyyMMdd} form
     * @param region the signing region
     * @param service the service signing name
     */
    public static byte[] signingKey(String secretAccessKey, String dateStamp, String region, String service) {
        return CACHE.get(new Key(secretAccessKey, dateStamp, region, service));
    }

    private static byte[] newSigningKey(Key key) {
        LOG.trace(() -> "Generating a new signing key as the signing key is not available in the cache for the date: "
                        + key.dateStamp);
        byte[] kSecret = ("AWS4" + key.secretAccessKey).getBytes(StandardCharsets.UTF_8);
        byte[] kDate = SignerUtils.sign(key.dateStamp, kSecret);
        byte[] kRegion = SignerUtils.sign(key.region, kDate);
        byte[] kService = SignerUtils.sign(key.service, kRegion);
        return SignerUtils.sign(SignerConstant.AWS4_TERMINATOR, kService);
    }

    /**
     * The cache key. It holds the secret key, so it has no {@code toString}.
     */
    private static final class Key {
        private final String secretAccessKey;
        private final String dateStamp;
        private final String region;
        private final String service;
        private final int hashCode;

        private Key(String secretAccessKey, String dateStamp, String region, String service) {
            this.secretAccessKey = secretAccessKey;
            this.dateStamp = dateStamp;
            this.region = region;
            this.service = service;
            int result = secretAccessKey.hashCode();
            result = 31 * result + dateStamp.hashCode();
            result = 31 * result + region.hashCode();
            result = 31 * result + service.hashCode();
            this.hashCode = result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key)) {
                return false;
            }
            Key other = (Key) o;
            return hashCode == other.hashCode
                   && secretAccessKey.equals(other.secretAccessKey)
                   && dateStamp.equals(other.dateStamp)
                   && region.equals(other.region)
                   && service.equals(other.service);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
