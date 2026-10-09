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

import static software.amazon.awssdk.benchmark.harness.FiniteWorkloads.response;
import static software.amazon.awssdk.benchmark.harness.FiniteWorkloads.utf8;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import software.amazon.awssdk.benchmark.harness.FiniteWorkloads.Invoker;
import software.amazon.awssdk.benchmark.harness.FiniteWorkloads.PaddedFixture;
import software.amazon.awssdk.benchmark.harness.FiniteWorkloads.Route;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.route53.Route53AsyncClient;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.NoSuchHostedZoneException;
import software.amazon.awssdk.services.route53.model.UpdateHostedZoneCommentRequest;
import software.amazon.awssdk.services.route53.model.UpdateHostedZoneCommentResponse;
import software.amazon.smithy.java.core.serde.document.Document;

/**
 * The restXml fixture: Route 53 {@code UpdateHostedZoneComment} ({@code POST} with an HTTP label and an XML body).
 * The {@code Comment} is the pad; the success response's hosted zone configuration echoes it. Error cells get the
 * modeled {@code NoSuchHostedZone} (404); retry cells a 500 {@code InternalFailure}, retryable by status.
 */
final class Route53Workloads extends FiniteWorkloads.Provider {
    static final String ZONE = "ZBENCHMARK";
    static final String NS = "https://route53.amazonaws.com/doc/2013-04-01/";
    static final String CONTENT_TYPE = "text/xml";

    Route53Workloads() {
        super("route53-updatehostedzonecomment", BenchProtocol.REST_XML, "route53",
              "com.amazonaws.route53#AWSDnsV20130401", "UpdateHostedZoneComment",
              "com.amazonaws.route53#NoSuchHostedZone");
    }

    @Override
    public PaddedFixture fixture(BenchmarkCell cell) {
        return new PaddedFixture(
            cell.payload(), Route.path("POST", "/2013-04-01/hostedzone/" + ZONE), 1,
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

    @Override
    Invoker generated(boolean async, PaddedFixture fixture, URI endpoint, AutoCloseable http) {
        UpdateHostedZoneCommentRequest request = UpdateHostedZoneCommentRequest.builder().id(ZONE)
                                                                               .comment(fixture.pad()).build();
        Predicate<Throwable> error = t -> t instanceof NoSuchHostedZoneException;
        Function<Object, String> echo = r -> ((UpdateHostedZoneCommentResponse) r).hostedZone().config().comment();
        if (async) {
            Route53AsyncClient client = Route53AsyncClient.builder().region(Region.US_EAST_1)
                .credentialsProvider(DynamoDbWorkloads.credentials()).endpointOverride(endpoint)
                .retryStrategy(DynamoDbWorkloads.retryStrategy()).httpClient((SdkAsyncHttpClient) http).build();
            return Invoker.async(client, () -> client.updateHostedZoneComment(request), error, echo);
        }
        Route53Client client = Route53Client.builder().region(Region.US_EAST_1)
            .credentialsProvider(DynamoDbWorkloads.credentials()).endpointOverride(endpoint)
            .retryStrategy(DynamoDbWorkloads.retryStrategy()).httpClient((SdkSyncHttpClient) http).build();
        return Invoker.sync(client, () -> client.updateHostedZoneComment(request), error, echo);
    }

    @Override
    Document dynamicInput(PaddedFixture fixture) {
        Map<String, Document> input = new LinkedHashMap<>();
        input.put("Id", Document.of(ZONE));
        input.put("Comment", Document.of(fixture.pad()));
        return Document.of(input);
    }

    @Override
    String dynamicEcho(Document output) {
        return output.getMember("HostedZone").getMember("Config").getMember("Comment").asString();
    }
}
