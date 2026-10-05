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

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.auth.api.EventStreamSigningContext;
import software.amazon.awssdk.auth.api.SignRequest;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.auth.aws.AwsV4HttpSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;

/** The {@link AwsV4HttpSigner} implementation. Not a public API. */
public final class DefaultAwsV4HttpSigner implements AwsV4HttpSigner {
    public static final DefaultAwsV4HttpSigner INSTANCE = new DefaultAwsV4HttpSigner();
    public static final String ALGORITHM = "AWS4-HMAC-SHA256";

    private static final int MAX_CACHED_KEYS = 300;
    private static final Map<KeyId, byte[]> SIGNING_KEYS = new ConcurrentHashMap<>();

    private DefaultAwsV4HttpSigner() {
    }

    @Override
    public SignedRequest sign(SignRequest<? extends AwsCredentialsIdentity> request) {
        AwsCredentialsIdentity credentials = request.identity();
        String token = credentials instanceof AwsSessionCredentialsIdentity session ? session.sessionToken() : null;
        return sign(request, "X-Amz-Security-Token", token);
    }

    /** Signs with the session token (if any) in {@code tokenName}; S3 Express uses its own token header. */
    public static SignedRequest sign(SignRequest<? extends AwsCredentialsIdentity> request, String tokenName,
                                     String token) {
        String region = request.requireProperty(REGION_NAME);
        String secret = request.identity().secretAccessKey();
        V4RequestSigning.Spec spec = new V4RequestSigning.Spec(
            ALGORITHM, region, tokenName, token, Map.of(),
            (stringToSign, date, service, scopeRegion) ->
                SigningCrypto.hex(SigningCrypto.hmacSha256(signingKey(secret, date, scopeRegion, service), stringToSign)));
        V4RequestSigning.Result result = V4RequestSigning.sign(request, spec);
        EventStreamSigningContext eventStream = null;
        AuthLocation location = request.requireProperty(AUTH_LOCATION, AuthLocation.HEADER);
        if (location == AuthLocation.HEADER && V4RequestSigning.isEventStream(request.request())) {
            // The frames are chain-signed when sent, with this request's signing key, scope and clock (V2 parity);
            // the pipeline gets only the clock's instants, so it never depends on a clock type.
            String service = request.requireProperty(SERVICE_SIGNING_NAME);
            Clock clock = request.requireProperty(SIGNING_CLOCK, Clock.systemUTC());
            eventStream = new EventStreamSigningContext(
                EventStreamSigningContext.SIGV4_PAYLOAD, region, service,
                date -> signingKey(secret, DateTimeFormatter.BASIC_ISO_DATE.format(date), region, service),
                clock::instant);
        }
        return new SignedRequest(result.request(), result.signature(), eventStream);
    }

    /** The SigV4 signing key of {@code secret} for one date, region and service; cached. */
    public static byte[] signingKey(String secret, String date, String region, String service) {
        KeyId id = new KeyId(secret, date, region, service);
        byte[] cached = SIGNING_KEYS.get(id);
        if (cached != null) {
            return cached;
        }
        byte[] kDate = SigningCrypto.hmacSha256(("AWS4" + secret).getBytes(StandardCharsets.UTF_8), date);
        byte[] kRegion = SigningCrypto.hmacSha256(kDate, region);
        byte[] kService = SigningCrypto.hmacSha256(kRegion, service);
        byte[] key = SigningCrypto.hmacSha256(kService, "aws4_request");
        if (SIGNING_KEYS.size() >= MAX_CACHED_KEYS) {
            SIGNING_KEYS.clear();
        }
        SIGNING_KEYS.put(id, key);
        return key;
    }

    @Override
    public String toString() {
        return "AwsV4HttpSigner";
    }

    /** A signing-key cache key. Never rendered: it holds the secret. */
    private record KeyId(String secret, String date, String region, String service) {
        @Override
        public String toString() {
            return "KeyId[" + date + "/" + region + "/" + service + "]";
        }
    }
}
