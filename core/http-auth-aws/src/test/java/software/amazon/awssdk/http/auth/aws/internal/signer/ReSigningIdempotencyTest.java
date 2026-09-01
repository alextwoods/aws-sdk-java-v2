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

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner.REGION_NAME;
import static software.amazon.awssdk.http.auth.spi.signer.HttpSigner.SIGNING_CLOCK;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;

/**
 * Re-signing an already-signed request must produce the same signature as signing the original
 * request (with a fixed clock): the signer-managed headers from the first pass — most importantly
 * {@code Authorization}, which is the output of signing — must not leak into the second pass's
 * canonical request. This is the prerequisite for reusing a mutable request across retry attempts
 * (phase B part 2).
 */
public class ReSigningIdempotencyTest {

    private static final Clock FIXED_CLOCK =
        Clock.fixed(Instant.parse("2024-03-11T17:00:00Z"), ZoneOffset.UTC);

    private final DefaultAwsV4HttpSigner signer = new DefaultAwsV4HttpSigner();

    @Test
    @DisplayName("fast path: sign(sign(request)) == sign(request)")
    public void fastPathReSignIsIdempotent() {
        assertReSignIdempotent(signer::sign, awsCreds());
    }

    @Test
    @DisplayName("fast path, session credentials: security token replaced, not folded")
    public void fastPathSessionCredentialsReSignIsIdempotent() {
        assertReSignIdempotent(signer::sign,
                               AwsSessionCredentialsIdentity.create("access", "secret", "session-token"));
    }

    @Test
    @DisplayName("legacy path: sign(sign(request)) == sign(request)")
    public void legacyPathReSignIsIdempotent() {
        assertReSignIdempotent(signer::signLegacyPath, awsCreds());
    }

    @Test
    @DisplayName("legacy path, session credentials: security token replaced, not folded")
    public void legacyPathSessionCredentialsReSignIsIdempotent() {
        assertReSignIdempotent(signer::signLegacyPath,
                               AwsSessionCredentialsIdentity.create("access", "secret", "session-token"));
    }

    private void assertReSignIdempotent(
            Function<SignRequest<? extends AwsCredentialsIdentity>, SignedRequest> sign,
            AwsCredentialsIdentity credentials) {
        SignedRequest first = sign.apply(signRequest(credentials, request()));

        // Feed the fully signed request (carrying Authorization, X-Amz-Date, X-Amz-Content-Sha256,
        // Host, and possibly X-Amz-Security-Token) back into the signer, as a mutable-request retry
        // loop would.
        SignedRequest second = sign.apply(signRequest(credentials, first.request()));

        assertThat(headerMap(second.request())).isEqualTo(headerMap(first.request()));
    }

    private static Map<String, java.util.List<String>> headerMap(SdkHttpRequest request) {
        Map<String, java.util.List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        request.forEachHeader((name, values) -> headers.put(name, values));
        return headers;
    }

    private static SignRequest<AwsCredentialsIdentity> signRequest(AwsCredentialsIdentity credentials,
                                                                   SdkHttpRequest request) {
        return SignRequest.builder(credentials)
                          .request(request)
                          .payload(() -> new ByteArrayInputStream("{\"TableName\":\"foo\"}".getBytes()))
                          .putProperty(REGION_NAME, "us-east-1")
                          .putProperty(SERVICE_SIGNING_NAME, "demo")
                          .putProperty(SIGNING_CLOCK, FIXED_CLOCK)
                          .build();
    }

    private static SdkHttpRequest request() {
        return SdkHttpRequest.builder()
                             .protocol("https")
                             .method(SdkHttpMethod.POST)
                             .putHeader("Host", "demo.us-east-1.amazonaws.com")
                             .putHeader("x-amz-archive-description", "test  test")
                             .encodedPath("/")
                             .uri(URI.create("https://demo.us-east-1.amazonaws.com"))
                             .build();
    }

    private static AwsCredentialsIdentity awsCreds() {
        return AwsCredentialsIdentity.create("access", "secret");
    }
}
