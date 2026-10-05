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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.HttpQueryParameter;
import software.amazon.awssdk.http.SdkHttpRequest;

/**
 * The SigV4 canonicalization and signing-key algorithm against the published AWS Signature Version 4 examples
 * ({@code get-vanilla} of the SigV4 test suite and the IAM {@code ListUsers} example of the SigV4 documentation).
 */
class V4RequestSigningTest {
    private static final String SECRET = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";

    @Test
    void getVanillaMatchesTheTestSuite() {
        SdkHttpRequest request = SdkHttpRequest.builder().method("GET").host("example.amazonaws.com").encodedPath("/")
                                               .putHeader("Host", "example.amazonaws.com")
                                               .putHeader("X-Amz-Date", "20150830T123600Z")
                                               .build();
        V4RequestSigning.Canonical canonical =
            V4RequestSigning.canonicalRequest(request, SigningCrypto.EMPTY_SHA256, true, true);
        assertEquals("GET\n/\n\nhost:example.amazonaws.com\nx-amz-date:20150830T123600Z\n\nhost;x-amz-date\n"
                     + SigningCrypto.EMPTY_SHA256, canonical.text());
        assertEquals("5fa00fa31553b73ebf1942676e86291e8372ff2a2260956d9b8aae1d763fbf31",
                     signature(canonical.text(), "us-east-1", "service"));
    }

    @Test
    void iamListUsersMatchesTheDocumentedExample() {
        SdkHttpRequest request = SdkHttpRequest.builder().method("GET").host("iam.amazonaws.com").encodedPath("/")
                                               .addQueryParameter("Version", "2010-05-08")
                                               .addQueryParameter("Action", "ListUsers")
                                               .putHeader("Content-Type",
                                                          "application/x-www-form-urlencoded; charset=utf-8")
                                               .putHeader("Host", "iam.amazonaws.com")
                                               .putHeader("X-Amz-Date", "20150830T123600Z")
                                               .build();
        V4RequestSigning.Canonical canonical =
            V4RequestSigning.canonicalRequest(request, SigningCrypto.EMPTY_SHA256, true, true);
        assertEquals("f536975d06c0309214f805bb90ccff089219ecd68b2577efef23edd43b7e1a59",
                     SigningCrypto.hex(SigningCrypto.sha256(canonical.text())));
        assertEquals("5d672d79c15b13162d9279b0855cfba6789a8edb4c82c400e06b5924a6f2b5d7",
                     signature(canonical.text(), "us-east-1", "iam"));
    }

    @Test
    void canonicalUriRemovesDotSegmentsAndEncodesTwice() {
        assertEquals("/foo/", V4RequestSigning.canonicalUri("/foo/bar/..", false, true));
        assertEquals("/a/c", V4RequestSigning.canonicalUri("/a/./b/../c", false, true));
        assertEquals("/a//b", V4RequestSigning.canonicalUri("/a//b", false, true));
        assertEquals("/a%2520b", V4RequestSigning.canonicalUri("/a%20b", true, true));
        assertEquals("/./foo", V4RequestSigning.canonicalUri("/./foo", false, false));
    }

    @Test
    void canonicalQuerySortsAndReencodes() {
        List<HttpQueryParameter> query = List.of(HttpQueryParameter.raw("b", "2"), HttpQueryParameter.raw("a", "x%2Fy"),
                                                 HttpQueryParameter.raw("a", "*"), HttpQueryParameter.raw("flag", null),
                                                 HttpQueryParameter.of("sp ace", "v+1"));
        assertEquals("a=%2A&a=x%2Fy&b=2&flag=&sp%20ace=v%2B1", V4RequestSigning.canonicalQuery(query));
    }

    private static String signature(String canonicalRequest, String region, String service) {
        String scope = "20150830/" + region + "/" + service + "/aws4_request";
        String stringToSign = V4RequestSigning.stringToSign("AWS4-HMAC-SHA256", "20150830T123600Z", scope,
                                                            canonicalRequest);
        byte[] key = DefaultAwsV4HttpSigner.signingKey(SECRET, "20150830", region, service);
        return SigningCrypto.hex(SigningCrypto.hmacSha256(key, stringToSign));
    }
}
