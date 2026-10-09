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

/**
 * The SDK-free wire half of the restXml fixture (Route 53 {@code UpdateHostedZoneComment}, a {@code POST} with an
 * HTTP label and an XML body): the {@code Comment} is the pad; the success response's hosted zone configuration
 * echoes it. Error cells get the modeled {@code NoSuchHostedZone} (404); retry cells a 500 {@code InternalFailure},
 * retryable by status.
 */
public final class Route53UpdateHostedZoneCommentFixture {
    public static final String NAME = "route53-updatehostedzonecomment";
    public static final String ZONE = "ZBENCHMARK";
    public static final String NS = "https://route53.amazonaws.com/doc/2013-04-01/";
    public static final String CONTENT_TYPE = "text/xml";

    private Route53UpdateHostedZoneCommentFixture() {
    }

    public static PaddedFixture create(PayloadClass payload) {
        return new PaddedFixture(
            payload, PaddedFixture.Route.path("POST", "/2013-04-01/hostedzone/" + ZONE), 1,
            pad -> utf8("<UpdateHostedZoneCommentRequest xmlns=\"" + NS + "\"><Comment>" + pad
                        + "</Comment></UpdateHostedZoneCommentRequest>"),
            pad -> response(200, CONTENT_TYPE, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                               + "<UpdateHostedZoneCommentResponse xmlns=\"" + NS + "\"><HostedZone>"
                                               + "<Id>/hostedzone/" + ZONE + "</Id><Name>example.com.</Name>"
                                               + "<CallerReference>benchmark</CallerReference><Config><Comment>" + pad
                                               + "</Comment></Config></HostedZone></UpdateHostedZoneCommentResponse>"),
            response(404, CONTENT_TYPE, error("NoSuchHostedZone", "Sender", "No hosted zone found")),
            response(500, CONTENT_TYPE, error("InternalFailure", "Receiver", "injected retry")));
    }

    private static String error(String code, String type, String message) {
        return "<?xml version=\"1.0\"?>\n<ErrorResponse xmlns=\"" + NS + "\"><Error><Type>" + type + "</Type><Code>"
               + code + "</Code><Message>" + message + "</Message></Error><RequestId>benchmark</RequestId>"
               + "</ErrorResponse>";
    }
}
