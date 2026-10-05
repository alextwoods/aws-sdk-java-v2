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

package software.amazon.awssdk.endpoints.rules.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RulesFunctionsTest {

    @Test
    void substringCountsFromEitherEndAndRejectsInvalidRangesAndNonAscii() {
        assertEquals("abc", RulesFunctions.substring("abcdef", 0, 3, false));
        assertEquals("def", RulesFunctions.substring("abcdef", 0, 3, true));
        assertNull(RulesFunctions.substring("abc", 2, 2, false));
        assertNull(RulesFunctions.substring("abc", 0, 4, false));
        assertNull(RulesFunctions.substring("ab\u00e9def", 0, 2, false));
    }

    @Test
    void uriEncodeUsesRfc3986() {
        assertEquals("a%20b%2Fc~d%2A", RulesFunctions.uriEncode("a b/c~d*"));
    }

    @Test
    void hostLabels() {
        assertTrue(RulesFunctions.isValidHostLabel("us-east-1", false));
        assertFalse(RulesFunctions.isValidHostLabel("-bad", false));
        assertFalse(RulesFunctions.isValidHostLabel("a.b", false));
        assertTrue(RulesFunctions.isValidHostLabel("a.b", true));
        assertFalse(RulesFunctions.isValidHostLabel("a..b", true));
        assertFalse(RulesFunctions.isValidHostLabel("", true));
        assertFalse(RulesFunctions.isValidHostLabel("a".repeat(64), false));
    }

    @Test
    void parseUrlReturnsTheCanonicalRecord() {
        assertEquals(Map.of("scheme", "https", "authority", "example.com:8443", "path", "/a/b",
                            "normalizedPath", "/a/b/", "isIp", false),
                     RulesFunctions.parseUrl("https://example.com:8443/a/b"));
        assertEquals(true, RulesFunctions.parseUrl("http://192.168.0.1").get("isIp"));
        assertEquals(true, RulesFunctions.parseUrl("http://[::1]:80").get("isIp"));
        assertEquals("/", RulesFunctions.parseUrl("http://h").get("normalizedPath"));
        assertNull(RulesFunctions.parseUrl("https://example.com?query=1"));
        assertNull(RulesFunctions.parseUrl("not a url"));
    }

    @Test
    void ipAddressHeuristic() {
        assertTrue(RulesFunctions.isIpAddress("1.2.3.4"));
        assertTrue(RulesFunctions.isIpAddress("999.10.100.0"));
        assertFalse(RulesFunctions.isIpAddress("01.2.3.4"));
        assertFalse(RulesFunctions.isIpAddress("1.2.3"));
        assertFalse(RulesFunctions.isIpAddress("1.2.3.4.5"));
        assertFalse(RulesFunctions.isIpAddress("1.2.3.a"));
    }

    @Test
    void splitHonoursTheLimit() {
        assertEquals(List.of("a", "b", "c"), RulesFunctions.split("a--b--c", "--", 0));
        assertEquals(List.of("a", "b--c"), RulesFunctions.split("a--b--c", "--", 2));
        assertEquals(List.of("a--b"), RulesFunctions.split("a--b", "--", 1));
        assertEquals(List.of(""), RulesFunctions.split("", "-", 0));
        assertThrows(RuleEvaluationError.class, () -> RulesFunctions.split("a", "", 0));
    }

    @Test
    void parseArnSplitsTheResource() {
        assertEquals(Map.of("partition", "aws", "service", "s3", "region", "us-west-2", "accountId", "123456789012",
                            "resourceId", List.of("accesspoint", "my-ap", "object")),
                     RulesFunctions.parseArn("arn:aws:s3:us-west-2:123456789012:accesspoint/my-ap:object"));
        assertEquals(List.of("bucket"), RulesFunctions.parseArn("arn:aws:s3:::bucket").get("resourceId"));
        assertNull(RulesFunctions.parseArn("arn:aws:s3:us-west-2:123"));
        assertNull(RulesFunctions.parseArn("arn::s3:::x"));
        assertNull(RulesFunctions.parseArn("not-an-arn"));
    }

    @Test
    void virtualHostableBuckets() {
        assertTrue(RulesFunctions.isVirtualHostableS3Bucket("my-bucket", false));
        assertFalse(RulesFunctions.isVirtualHostableS3Bucket("my.bucket", false));
        assertTrue(RulesFunctions.isVirtualHostableS3Bucket("my.bucket", true));
        assertFalse(RulesFunctions.isVirtualHostableS3Bucket("my.-bucket", true));
        assertFalse(RulesFunctions.isVirtualHostableS3Bucket("192.168.5.4", true));
        assertFalse(RulesFunctions.isVirtualHostableS3Bucket("ab", false));
        assertFalse(RulesFunctions.isVirtualHostableS3Bucket("My-Bucket", false));
    }
}
