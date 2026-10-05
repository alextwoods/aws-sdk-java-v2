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

package software.amazon.awssdk.auth.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.api.HttpSigner;
import software.amazon.awssdk.auth.api.SignRequest;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.auth.aws.internal.V4RequestSigning;
import software.amazon.awssdk.auth.aws.internal.V4aKeyDerivation;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * SigV4a: the key derivation against the public key of the AWS SDK SigV4a derivation test vector, and signatures
 * verified with an independently computed public key.
 */
class AwsV4aHttpSignerTest {
    private static final Pattern AUTHORIZATION = Pattern.compile(
        "AWS4-ECDSA-P256-SHA256 Credential=([^,]+), SignedHeaders=([^,]+), Signature=([0-9a-f]+)");

    @Test
    void derivedKeyMatchesTheAwsTestVector() {
        BigInteger d = V4aKeyDerivation.derive("AKISORANDOMAASORANDOM", "q+jcrXGc+0zWN6uzclKVhvMmUsIfRPa4rlRandom");
        ECPoint q = multiply(V4aKeyDerivation.p256Parameters(), d);
        assertEquals(new BigInteger("15D242CEEBF8D8169FD6A8B5A746C41140414C3B07579038DA06AF89190FFFCB", 16),
                     q.getAffineX());
        assertEquals(new BigInteger("0515242CEDD82E94799482E4C0514B505AFCCF2C0C98D6A553BF539F424C5EC0", 16),
                     q.getAffineY());
    }

    @Test
    void signatureVerifiesWithTheDerivedPublicKey() throws Exception {
        AwsCredentialsIdentity creds = AwsCredentialsIdentity.create("AKIDEXAMPLE",
                                                                     "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
        SdkHttpRequest request = SdkHttpRequest.builder().method("GET").host("example.amazonaws.com").build();
        SignRequest<AwsCredentialsIdentity> signRequest =
            SignRequest.<AwsCredentialsIdentity>builder(creds)
                       .request(request)
                       .putProperty(AwsV4aHttpSigner.REGION_SET, RegionSet.create("us-east-1,us-west-*"))
                       .putProperty(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME, "service")
                       .putProperty(HttpSigner.SIGNING_CLOCK,
                                    Clock.fixed(Instant.parse("2015-08-30T12:36:00Z"), ZoneOffset.UTC))
                       .build();
        SignedRequest signed = AwsV4aHttpSigner.create().sign(signRequest);

        assertEquals("us-east-1,us-west-*", signed.request().headers().firstValue("X-Amz-Region-Set").orElseThrow());
        Matcher authorization = AUTHORIZATION.matcher(signed.request().headers().firstValue("Authorization")
                                                            .orElseThrow());
        assertTrue(authorization.matches());
        assertEquals("AKIDEXAMPLE/20150830/service/aws4_request", authorization.group(1));
        assertEquals("host;x-amz-content-sha256;x-amz-date;x-amz-region-set", authorization.group(2));

        V4RequestSigning.Canonical canonical = V4RequestSigning.canonicalRequest(
            signed.request().toBuilder().removeHeader("Authorization").build(),
            signed.request().headers().firstValue("x-amz-content-sha256").orElseThrow(), true, true);
        String stringToSign = V4RequestSigning.stringToSign("AWS4-ECDSA-P256-SHA256", "20150830T123600Z",
                                                            "20150830/service/aws4_request", canonical.text());
        Signature verifier = Signature.getInstance("SHA256withECDSA");
        verifier.initVerify(publicKey(V4aKeyDerivation.derive(creds.accessKeyId(), creds.secretAccessKey())));
        verifier.update(stringToSign.getBytes(StandardCharsets.UTF_8));
        assertTrue(verifier.verify(HexFormat.of().parseHex(authorization.group(3))));
    }

    private static PublicKey publicKey(BigInteger d) throws Exception {
        ECParameterSpec params = V4aKeyDerivation.p256Parameters();
        return KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(multiply(params, d), params));
    }

    /** Affine double-and-add scalar multiplication on a short Weierstrass prime curve: an independent oracle. */
    private static ECPoint multiply(ECParameterSpec params, BigInteger k) {
        BigInteger p = ((ECFieldFp) params.getCurve().getField()).getP();
        BigInteger a = params.getCurve().getA();
        ECPoint result = ECPoint.POINT_INFINITY;
        ECPoint addend = params.getGenerator();
        for (int i = 0; i < k.bitLength(); i++) {
            if (k.testBit(i)) {
                result = add(result, addend, p, a);
            }
            addend = add(addend, addend, p, a);
        }
        return result;
    }

    private static ECPoint add(ECPoint u, ECPoint v, BigInteger p, BigInteger a) {
        if (u.equals(ECPoint.POINT_INFINITY)) {
            return v;
        }
        if (v.equals(ECPoint.POINT_INFINITY)) {
            return u;
        }
        BigInteger x1 = u.getAffineX();
        BigInteger y1 = u.getAffineY();
        BigInteger x2 = v.getAffineX();
        BigInteger y2 = v.getAffineY();
        BigInteger lambda;
        if (x1.equals(x2)) {
            if (y1.add(y2).mod(p).signum() == 0) {
                return ECPoint.POINT_INFINITY;
            }
            lambda = x1.pow(2).multiply(BigInteger.valueOf(3)).add(a).multiply(y1.shiftLeft(1).modInverse(p)).mod(p);
        } else {
            lambda = y2.subtract(y1).multiply(x2.subtract(x1).modInverse(p)).mod(p);
        }
        BigInteger x3 = lambda.pow(2).subtract(x1).subtract(x2).mod(p);
        BigInteger y3 = lambda.multiply(x1.subtract(x3)).subtract(y1).mod(p);
        return new ECPoint(x3, y3);
    }
}
