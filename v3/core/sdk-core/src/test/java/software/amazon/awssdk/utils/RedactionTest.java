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

package software.amazon.awssdk.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Safe renderings of possibly sensitive values (Requirement 21.10). */
class RedactionTest {
    private static final String R = Redaction.REDACTED;

    @Test
    void credentialAuthorizationTokenAndCookieNamesAreSensitive() {
        for (String name : List.of("Authorization", "proxy-authorization", "Cookie", "Set-Cookie",
                                   "X-Amz-Security-Token", "x-amz-server-side-encryption-customer-key",
                                   "x-amz-copy-source-server-side-encryption-customer-key-MD5",
                                   "x-aws-ec2-metadata-token", "x-amz-sso_bearer_token", "X-Amz-Signature",
                                   "X-Amz-Credential", "secretAccessKey", "sessionToken", "password", "x-api-key",
                                   "privateKey")) {
            assertTrue(Redaction.isSensitiveName(name), name);
        }
        for (String name : List.of("Content-Type", "Content-Length", "x-amz-date", "x-amzn-RequestId", "Host",
                                   "x-amz-target", "TableName")) {
            assertFalse(Redaction.isSensitiveName(name), name);
        }
        assertFalse(Redaction.isSensitiveName(null));
    }

    @Test
    void valuesOfSensitiveNamesAndSecretsAreRedacted() {
        assertEquals(R, Redaction.value("Authorization", "AWS4-HMAC-SHA256 Credential=canary"));
        assertEquals("application/json", Redaction.value("Content-Type", "application/json"));
        assertNull(Redaction.value("Authorization", null));
        assertEquals(R, Redaction.secret("canary-secret"));
        assertNull(Redaction.secret(null));
    }

    @Test
    void headerAndQueryMapsKeepNamesAndRedactSensitiveValues() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("Content-Type", List.of("text/plain"));
        headers.put("Authorization", List.of("canary-signature"));
        headers.put("Cookie", List.of("a=canary", "b=canary"));
        assertEquals("{Content-Type=[text/plain], Authorization=" + R + ", Cookie=" + R + "}",
                     Redaction.headers(headers));
        Map<String, List<String>> query = new LinkedHashMap<>();
        query.put("prefix", List.of("p"));
        query.put("X-Amz-Security-Token", List.of("canary"));
        assertEquals("{prefix=[p], X-Amz-Security-Token=" + R + "}", Redaction.queryParameters(query));
        assertNull(Redaction.headers(null));
    }

    @Test
    void presignedUrlQueryValuesAndUserInfoAreRedacted() {
        URI presigned = URI.create("https://user:canary@bucket.s3.amazonaws.com:8443/key%20a?X-Amz-Algorithm=AWS4"
                                   + "&X-Amz-Credential=AKID%2F20240101&X-Amz-Signature=canary"
                                   + "&X-Amz-Security-Token=canary&flag&partNumber=1");
        assertEquals("https://" + R + "@bucket.s3.amazonaws.com:8443/key%20a?X-Amz-Algorithm=AWS4"
                     + "&X-Amz-Credential=" + R + "&X-Amz-Signature=" + R + "&X-Amz-Security-Token=" + R
                     + "&flag&partNumber=1",
                     Redaction.uri(presigned));
        assertEquals("http://localhost/", Redaction.uri(URI.create("http://localhost/")));
        assertEquals("a=1&Token=" + R + "&", Redaction.query("a=1&Token=canary&"));
        assertEquals("", Redaction.query(""));
        assertNull(Redaction.uri(null));
    }
}
