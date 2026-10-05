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
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPrivateKeySpec;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Derives the SigV4a ECDSA P-256 private key from AWS credentials, as the AWS Common Runtime and other AWS SDKs do:
 * NIST SP 800-108 HMAC-SHA256 counter-mode KDF with key {@code "AWS4A" + secret}, label
 * {@code AWS4-ECDSA-P256-SHA256} and context {@code accessKeyId || externalCounter}; the first candidate {@code c}
 * with {@code c < n - 2} gives the private key {@code c + 1}. Not a public API.
 */
public final class V4aKeyDerivation {
    public static final String ALGORITHM = "AWS4-ECDSA-P256-SHA256";
    /** The P-256 group order. */
    public static final BigInteger N =
        new BigInteger("FFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551", 16);
    private static final BigInteger N_MINUS_TWO = N.subtract(BigInteger.TWO);
    private static final ECParameterSpec P256 = p256();
    private static final int MAX_CACHED_KEYS = 100;
    private static final Map<Credentials, ECPrivateKey> KEYS = new ConcurrentHashMap<>();

    private V4aKeyDerivation() {
    }

    /** The P-256 curve parameters. */
    public static ECParameterSpec p256Parameters() {
        return P256;
    }

    /** The private key of {@code accessKeyId}/{@code secretAccessKey}; cached. */
    public static ECPrivateKey privateKey(String accessKeyId, String secretAccessKey) {
        Credentials id = new Credentials(accessKeyId, secretAccessKey);
        ECPrivateKey cached = KEYS.get(id);
        if (cached != null) {
            return cached;
        }
        ECPrivateKey key = toKey(derive(accessKeyId, secretAccessKey));
        if (KEYS.size() >= MAX_CACHED_KEYS) {
            KEYS.clear();
        }
        KEYS.put(id, key);
        return key;
    }

    /** The private scalar {@code d}. */
    public static BigInteger derive(String accessKeyId, String secretAccessKey) {
        byte[] inputKey = ("AWS4A" + secretAccessKey).getBytes(StandardCharsets.UTF_8);
        byte[] label = ALGORITHM.getBytes(StandardCharsets.UTF_8);
        byte[] accessKey = accessKeyId.getBytes(StandardCharsets.UTF_8);
        for (int counter = 1; counter <= 0xFF; counter++) {
            ByteArrayOutputStream fixedInput = new ByteArrayOutputStream();
            fixedInput.writeBytes(new byte[] {0, 0, 0, 1});
            fixedInput.writeBytes(label);
            fixedInput.write(0);
            fixedInput.writeBytes(accessKey);
            fixedInput.write(counter);
            fixedInput.writeBytes(new byte[] {0, 0, 1, 0});
            BigInteger candidate = new BigInteger(1, SigningCrypto.hmacSha256(inputKey, fixedInput.toByteArray()));
            if (candidate.compareTo(N_MINUS_TWO) < 0) {
                return candidate.add(BigInteger.ONE);
            }
        }
        throw new IllegalStateException("Unable to derive a SigV4a signing key from the credentials.");
    }

    private static ECPrivateKey toKey(BigInteger d) {
        try {
            return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(new ECPrivateKeySpec(d, P256));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to create the SigV4a signing key", e);
        }
    }

    private static ECParameterSpec p256() {
        try {
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec("secp256r1"));
            return parameters.getParameterSpec(ECParameterSpec.class);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("The JDK does not provide the secp256r1 curve", e);
        }
    }

    /** A key cache key. Never rendered: it holds the secret. */
    private record Credentials(String accessKeyId, String secretAccessKey) {
        @Override
        public String toString() {
            return "Credentials[" + accessKeyId + "]";
        }
    }
}
