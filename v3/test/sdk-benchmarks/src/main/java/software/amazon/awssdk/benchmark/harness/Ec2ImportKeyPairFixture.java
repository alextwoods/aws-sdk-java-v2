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

package software.amazon.awssdk.benchmark.harness;

import static software.amazon.awssdk.benchmark.harness.PaddedFixture.response;
import static software.amazon.awssdk.benchmark.harness.PaddedFixture.utf8;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * The SDK-free wire half of the ec2Query fixture (EC2 {@code ImportKeyPair}, a form-encoded
 * {@code Action=ImportKeyPair} body, routed by that body): the {@code PublicKeyMaterial} blob (base64 on the wire) is
 * the pad; the success response echoes it as the {@code keyFingerprint}. Retry cells get a 500 {@code InternalError},
 * retryable by status. No modeled error: the EC2 model declares no error shapes.
 */
public final class Ec2ImportKeyPairFixture {
    public static final String NAME = "ec2-importkeypair";
    public static final String KEY_NAME = "benchmark-key";
    public static final String NS = "http://ec2.amazonaws.com/doc/2016-11-15/";
    public static final String CONTENT_TYPE = "text/xml;charset=UTF-8";

    private Ec2ImportKeyPairFixture() {
    }

    public static PaddedFixture create(PayloadClass payload) {
        return new PaddedFixture(
            payload, new PaddedFixture.Route("POST", "/", null, null, utf8("Action=ImportKeyPair&")), 3,
            pad -> utf8("Action=ImportKeyPair&Version=2016-11-15&KeyName=" + KEY_NAME + "&PublicKeyMaterial="
                        + Base64.getEncoder().encodeToString(pad.getBytes(StandardCharsets.UTF_8))),
            pad -> response(200, CONTENT_TYPE, "<ImportKeyPairResponse xmlns=\"" + NS + "\"><requestId>benchmark"
                                               + "</requestId><keyFingerprint>" + pad + "</keyFingerprint><keyName>"
                                               + KEY_NAME + "</keyName><keyPairId>key-0123456789abcdef0</keyPairId>"
                                               + "</ImportKeyPairResponse>"),
            null,
            response(500, CONTENT_TYPE, "<Response><Errors><Error><Code>InternalError</Code><Message>injected retry"
                                        + "</Message></Error></Errors><RequestID>benchmark</RequestID></Response>"));
    }
}
