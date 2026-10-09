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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * The SDK-free wire half of the awsQuery fixture (SNS {@code Publish}, a form-encoded {@code Action=Publish} body,
 * routed by that body): the {@code Message} is the pad; the success response echoes it as the {@code MessageId}.
 * Error cells get the modeled {@code NotFoundException} (query code {@code NotFound}, 404); retry cells the modeled,
 * retryable {@code InternalErrorException} (query code {@code InternalError}, 500).
 */
public final class SnsPublishFixture {
    public static final String NAME = "sns-publish";
    public static final String TOPIC = "arn:aws:sns:us-east-1:123456789012:benchmark-topic";
    public static final String NS = "http://sns.amazonaws.com/doc/2010-03-31/";
    public static final String CONTENT_TYPE = "text/xml";

    private SnsPublishFixture() {
    }

    public static PaddedFixture create(PayloadClass payload) {
        return new PaddedFixture(
            payload, new PaddedFixture.Route("POST", "/", null, null, utf8("Action=Publish&")), 1,
            pad -> utf8("Action=Publish&Version=2010-03-31&TopicArn="
                        + URLEncoder.encode(TOPIC, StandardCharsets.UTF_8) + "&Message=" + pad),
            pad -> response(200, CONTENT_TYPE, "<PublishResponse xmlns=\"" + NS + "\"><PublishResult><MessageId>" + pad
                                               + "</MessageId></PublishResult><ResponseMetadata><RequestId>benchmark"
                                               + "</RequestId></ResponseMetadata></PublishResponse>"),
            response(404, CONTENT_TYPE, error("NotFound", "Sender", "Topic does not exist")),
            response(500, CONTENT_TYPE, error("InternalError", "Receiver", "injected retry")));
    }

    static String error(String code, String type, String message) {
        return "<ErrorResponse xmlns=\"" + NS + "\"><Error><Type>" + type + "</Type><Code>" + code + "</Code>"
               + "<Message>" + message + "</Message></Error><RequestId>benchmark</RequestId></ErrorResponse>";
    }
}
