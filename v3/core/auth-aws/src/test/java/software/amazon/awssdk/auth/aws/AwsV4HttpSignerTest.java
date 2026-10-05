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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.api.HttpSigner;
import software.amazon.awssdk.auth.api.PayloadSigningMode;
import software.amazon.awssdk.auth.api.SignRequest;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.auth.aws.AwsV4FamilyHttpSigner.AuthLocation;
import software.amazon.awssdk.auth.aws.s3express.S3ExpressHttpSigner;
import software.amazon.awssdk.auth.aws.s3express.S3ExpressSessionCredentials;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpQueryParameter;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.identity.spi.TokenIdentity;

/**
 * SigV4 end to end, differential against the signatures the V2 {@code V4RequestSigner} produces for the same request
 * ({@code V4RequestSignerTest}: {@code https://test.com/./foo}, access/secret, 1970-01-01, us-east-1, demo, no double
 * encoding or normalization).
 */
class AwsV4HttpSignerTest {
    private static final Clock EPOCH = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
    private static final AwsCredentialsIdentity CREDS = AwsCredentialsIdentity.create("access", "secret");
    private static final AwsSessionCredentialsIdentity SESSION =
        AwsSessionCredentialsIdentity.create("access", "secret", "token");

    @Test
    void headerSignatureMatchesV2() {
        SignedRequest signed = AwsV4HttpSigner.create().sign(v2Request(CREDS, AuthLocation.HEADER));
        assertEquals("AWS4-HMAC-SHA256 Credential=access/19700101/us-east-1/demo/aws4_request, "
                     + "SignedHeaders=host;x-amz-archive-description;x-amz-content-sha256;x-amz-date, "
                     + "Signature=0fafd04465eb6201e868a80f72d15d50731512298f554684ce6627c0619f429a",
                     signed.request().headers().firstValue("Authorization").orElseThrow());
        assertEquals("19700101T000000Z", signed.request().headers().firstValue("X-Amz-Date").orElseThrow());
        assertEquals("0fafd04465eb6201e868a80f72d15d50731512298f554684ce6627c0619f429a", signed.signature());
    }

    @Test
    void sessionHeaderSignatureMatchesV2() {
        SignedRequest signed = AwsV4HttpSigner.create().sign(v2Request(SESSION, AuthLocation.HEADER));
        assertTrue(signed.request().headers().firstValue("Authorization").orElseThrow()
                         .endsWith("Signature=cda79272f6d258c2cb2f04ac84a5f9515440e0158bf39e212c3dcf88b3a477a9"));
        assertEquals("token", signed.request().headers().firstValue("X-Amz-Security-Token").orElseThrow());
    }

    @Test
    void presignedSignatureMatchesV2() {
        SignRequest<AwsCredentialsIdentity> request =
            v2Request(CREDS, AuthLocation.QUERY_STRING).toBuilder()
                                                       .putProperty(AwsV4FamilyHttpSigner.EXPIRATION_DURATION,
                                                                    Duration.ZERO)
                                                       .build();
        SignedRequest signed = AwsV4HttpSigner.create().sign(request);
        Map<String, String> query = query(signed.request());
        assertEquals("0", query.get("X-Amz-Expires"));
        assertEquals("host%3Bx-amz-archive-description", query.get("X-Amz-SignedHeaders"));
        assertEquals("access%2F19700101%2Fus-east-1%2Fdemo%2Faws4_request", query.get("X-Amz-Credential"));
        assertEquals("691f39caa2064fe4fb897976dfb4b09df54749c825a5fcd1e2f0b3fcd1bcc600", query.get("X-Amz-Signature"));
        assertFalse(signed.request().headers().contains("x-amz-content-sha256"));
        assertFalse(signed.request().headers().contains("Authorization"));
    }

    /**
     * The published S3 query-string example ("Authenticating Requests: Using Query Parameters", SigV4): GET
     * {@code examplebucket.s3.amazonaws.com/test.txt} on 2013-05-24, valid 86400 seconds, signing only {@code host}
     * with an unsigned payload.
     */
    @Test
    void presignedSignatureMatchesThePublishedS3Example() {
        SdkHttpRequest get = SdkHttpRequest.builder().method("GET").host("examplebucket.s3.amazonaws.com")
                                           .encodedPath("/test.txt").build();
        SignRequest<AwsCredentialsIdentity> request = SignRequest.<AwsCredentialsIdentity>builder(
                AwsCredentialsIdentity.create("AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"))
            .request(get)
            .putProperty(AwsV4HttpSigner.REGION_NAME, "us-east-1")
            .putProperty(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME, "s3")
            .putProperty(AwsV4FamilyHttpSigner.DOUBLE_URL_ENCODE, false)
            .putProperty(AwsV4FamilyHttpSigner.NORMALIZE_PATH, false)
            .putProperty(AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED, false)
            .putProperty(AwsV4FamilyHttpSigner.AUTH_LOCATION, AuthLocation.QUERY_STRING)
            .putProperty(AwsV4FamilyHttpSigner.EXPIRATION_DURATION, Duration.ofSeconds(86400))
            .putProperty(HttpSigner.SIGNING_CLOCK, Clock.fixed(Instant.parse("2013-05-24T00:00:00Z"), ZoneOffset.UTC))
            .build();

        SignedRequest signed = AwsV4HttpSigner.create().sign(request);

        Map<String, String> query = query(signed.request());
        assertEquals("AWS4-HMAC-SHA256", query.get("X-Amz-Algorithm"));
        assertEquals("20130524T000000Z", query.get("X-Amz-Date"));
        assertEquals("86400", query.get("X-Amz-Expires"));
        assertEquals("host", query.get("X-Amz-SignedHeaders"));
        assertEquals("aeeed9bbccd4d02ee5c0109b86d86835f995330da4c265957d157751f604d404", query.get("X-Amz-Signature"));
        assertEquals(Instant.parse("2013-05-25T00:00:00Z"), AwsV4Presigning.expiration(signed.request()));
        assertThrows(SdkClientException.class, () -> AwsV4Presigning.expiration(get));
    }

    @Test
    void presignRejectsExpirationBeyondSevenDays() {
        SignRequest<AwsCredentialsIdentity> request =
            v2Request(CREDS, AuthLocation.QUERY_STRING).toBuilder()
                                                       .putProperty(AwsV4FamilyHttpSigner.EXPIRATION_DURATION,
                                                                    Duration.ofDays(7).plusSeconds(1))
                                                       .build();
        assertThrows(SdkClientException.class, () -> AwsV4HttpSigner.create().sign(request));
    }

    @Test
    void payloadHashCoversTheBodyOrIsUnsigned() {
        SdkHttpRequest post = SdkHttpRequest.builder().method("POST").host("test.com")
                                            .body(HttpBody.fromUtf8("hello")).build();
        SignedRequest signed = AwsV4HttpSigner.create().sign(base(CREDS, post).build());
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                     signed.request().headers().firstValue("x-amz-content-sha256").orElseThrow());

        SignedRequest unsigned = AwsV4HttpSigner.create().sign(
            base(CREDS, post).putProperty(AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED, false).build());
        assertEquals("UNSIGNED-PAYLOAD", unsigned.request().headers().firstValue("x-amz-content-sha256").orElseThrow());

        // Over plain HTTP a present payload is always signed (V2 behavior).
        SdkHttpRequest http = post.toBuilder().scheme("http").build();
        SignedRequest forced = AwsV4HttpSigner.create().sign(
            base(CREDS, http).putProperty(AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED, false).build());
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                     forced.request().headers().firstValue("x-amz-content-sha256").orElseThrow());
    }

    @Test
    void streamingPayloadRequiresAPrecomputedHashOrUnsignedPayload() {
        SdkHttpRequest streaming = SdkHttpRequest.builder().method("PUT").host("test.com")
                                                 .body(HttpBody.streaming(StreamingBodies.fromBytes(new byte[3], "application/octet-stream")))
                                                 .build();
        SdkClientException e = assertThrows(SdkClientException.class,
                                            () -> AwsV4HttpSigner.create().sign(base(CREDS, streaming).build()));
        assertTrue(e.getMessage().contains("x-amz-content-sha256"));
        SignedRequest unsigned = AwsV4HttpSigner.create().sign(
            base(CREDS, streaming).putProperty(AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED, false).build());
        assertEquals("UNSIGNED-PAYLOAD", unsigned.request().headers().firstValue("x-amz-content-sha256").orElseThrow());
    }

    @Test
    void sign_streamingPayloadWithSuppliedHash_signsExactlyAsTheInMemoryPayload() {
        SdkHttpRequest bytes = SdkHttpRequest.builder().method("PUT").host("test.com")
                                             .body(HttpBody.fromUtf8("hello")).build();
        SdkHttpRequest streaming = bytes.toBuilder()
                                        .body(HttpBody.streaming(StreamingBodies.fromBytes(
                                            "hello".getBytes(StandardCharsets.UTF_8), null)))
                                        .build();
        String sha256 = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";

        SignedRequest inMemory = AwsV4HttpSigner.create().sign(base(CREDS, bytes).build());
        SignedRequest supplied = AwsV4HttpSigner.create().sign(
            base(CREDS, streaming).putProperty(HttpSigner.PAYLOAD_SHA256, sha256).build());

        assertEquals(sha256, supplied.request().headers().firstValue("x-amz-content-sha256").orElseThrow());
        assertEquals(inMemory.request().headers().firstValue("Authorization").orElseThrow(),
                     supplied.request().headers().firstValue("Authorization").orElseThrow());
    }

    @Test
    void payloadSigningMode_followsPayloadSigningSchemeAndPrecomputedHashes() {
        SdkHttpRequest streaming = SdkHttpRequest.builder().method("PUT").host("test.com")
                                                 .body(HttpBody.streaming(StreamingBodies.fromBytes(new byte[3], null)))
                                                 .build();
        AwsV4HttpSigner signer = AwsV4HttpSigner.create();

        assertEquals(Optional.of(PayloadSigningMode.PAYLOAD_HASH),
                     signer.payloadSigningMode(base(CREDS, streaming).build()));
        assertEquals(Optional.of(PayloadSigningMode.UNSIGNED), signer.payloadSigningMode(
            base(CREDS, streaming).putProperty(AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED, false).build()));
        // Plain HTTP signs a present payload even when payload signing is disabled (V2 behavior).
        SdkHttpRequest http = streaming.toBuilder().scheme("http").build();
        assertEquals(Optional.of(PayloadSigningMode.PAYLOAD_HASH), signer.payloadSigningMode(
            base(CREDS, http).putProperty(AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED, false).build()));
        SdkHttpRequest emptyHttp = http.toBuilder().body(HttpBody.empty()).build();
        assertEquals(Optional.of(PayloadSigningMode.UNSIGNED), signer.payloadSigningMode(
            base(CREDS, emptyHttp).putProperty(AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED, false).build()));
        // A precomputed payload hash (an aws-chunked value) or an event stream needs nothing of the payload.
        SdkHttpRequest trailer = streaming.toBuilder()
                                          .putHeader("x-amz-content-sha256", "STREAMING-UNSIGNED-PAYLOAD-TRAILER")
                                          .build();
        assertEquals(Optional.empty(), signer.payloadSigningMode(base(CREDS, trailer).build()));
        SdkHttpRequest events = streaming.toBuilder().putHeader("Content-Type", "application/vnd.amazon.eventstream")
                                         .build();
        assertEquals(Optional.empty(), signer.payloadSigningMode(base(CREDS, events).build()));

        S3ExpressSessionCredentials session = S3ExpressSessionCredentials.create("access", "secret", "token", null);
        assertEquals(Optional.of(PayloadSigningMode.UNSIGNED), S3ExpressHttpSigner.create().payloadSigningMode(
            SignRequest.builder(session).request(streaming)
                       .putProperty(AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED, false).build()));
        assertEquals(Optional.empty(), BearerHttpSigner.create().payloadSigningMode(
            SignRequest.builder(TokenIdentity.create("tok")).request(streaming).build()));
    }

    @Test
    void s3ExpressUsesTheSessionTokenHeader() {
        S3ExpressSessionCredentials session = S3ExpressSessionCredentials.create("access", "secret", "token", null);
        SignRequest<S3ExpressSessionCredentials> request =
            SignRequest.builder(session)
                       .request(v2Request(CREDS, AuthLocation.HEADER).request())
                       .putProperties(v2Request(CREDS, AuthLocation.HEADER).properties())
                       .build();
        SignedRequest signed = S3ExpressHttpSigner.create().sign(request);
        assertEquals("token", signed.request().headers().firstValue("x-amz-s3session-token").orElseThrow());
        assertFalse(signed.request().headers().contains("X-Amz-Security-Token"));
        assertTrue(signed.request().headers().firstValue("Authorization").orElseThrow()
                         .contains("SignedHeaders=host;x-amz-archive-description;x-amz-content-sha256;x-amz-date;"
                                   + "x-amz-s3session-token,"));
    }

    @Test
    void bearerSignerSetsTheAuthorizationHeader() {
        SignedRequest signed = BearerHttpSigner.create().sign(
            SignRequest.builder(TokenIdentity.create("tok"))
                       .request(SdkHttpRequest.builder().method("GET").host("test.com").build())
                       .build());
        assertEquals("Bearer tok", signed.request().headers().firstValue("Authorization").orElseThrow());
    }

    private static SignRequest<AwsCredentialsIdentity> v2Request(AwsCredentialsIdentity creds, AuthLocation location) {
        SdkHttpRequest request = SdkHttpRequest.builder().method("GET").host("test.com").encodedPath("/./foo")
                                               .putHeader("x-amz-content-sha256", "checksum")
                                               .putHeader("x-amz-archive-description", "test  test")
                                               .build();
        return base(creds, request).putProperty(AwsV4FamilyHttpSigner.DOUBLE_URL_ENCODE, false)
                                   .putProperty(AwsV4FamilyHttpSigner.NORMALIZE_PATH, false)
                                   .putProperty(AwsV4FamilyHttpSigner.AUTH_LOCATION, location)
                                   .build();
    }

    private static SignRequest.Builder<AwsCredentialsIdentity> base(AwsCredentialsIdentity creds,
                                                                    SdkHttpRequest request) {
        return SignRequest.<AwsCredentialsIdentity>builder(creds)
                          .request(request)
                          .putProperty(AwsV4HttpSigner.REGION_NAME, "us-east-1")
                          .putProperty(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME, "demo")
                          .putProperty(HttpSigner.SIGNING_CLOCK, EPOCH);
    }

    private static Map<String, String> query(SdkHttpRequest request) {
        return request.queryParameters().stream()
                      .collect(Collectors.toMap(HttpQueryParameter::rawName, HttpQueryParameter::rawValue));
    }
}
