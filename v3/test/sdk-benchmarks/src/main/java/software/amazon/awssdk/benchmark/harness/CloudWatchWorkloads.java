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

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
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
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.DashboardInvalidInputErrorException;
import software.amazon.awssdk.services.cloudwatch.model.PutDashboardRequest;
import software.amazon.awssdk.services.cloudwatch.model.PutDashboardResponse;
import software.amazon.smithy.java.core.serde.document.Document;

/**
 * The rpcv2Cbor fixture: CloudWatch {@code PutDashboard}. CloudWatch declares awsJson1.0, awsQuery and rpcv2Cbor;
 * with every runtime installed, model-default selection picks rpcv2Cbor (first in launch precedence), and the mock
 * service only answers the rpcv2Cbor route ({@code /service/<service>/operation/<op>} with
 * {@code smithy-protocol: rpc-v2-cbor}), so a run on any other protocol fails as unmatched. The
 * {@code DashboardBody} is the pad; the success response echoes it as a dashboard validation message. Error cells get
 * the modeled {@code DashboardInvalidInputError} (400); retry cells the modeled, retryable
 * {@code InternalServiceFault} (500).
 */
final class CloudWatchWorkloads extends FiniteWorkloads.Provider {
    static final String DASHBOARD = "benchmark-dashboard";
    static final String CONTENT_TYPE = "application/cbor";
    static final Map<String, String> HEADERS = Map.of("smithy-protocol", "rpc-v2-cbor");

    CloudWatchWorkloads() {
        super("cloudwatch-putdashboard", BenchProtocol.RPC_V2_CBOR, "cloudwatch",
              "com.amazonaws.cloudwatch#GraniteServiceVersion20100801", "PutDashboard",
              "com.amazonaws.cloudwatch#DashboardInvalidInputError");
    }

    @Override
    public PaddedFixture fixture(BenchmarkCell cell) {
        return new PaddedFixture(
            cell.payload(),
            new Route("POST", "/service/GraniteServiceVersion20100801/operation/PutDashboard", "smithy-protocol",
                      "rpc-v2-cbor", null), 1,
            // The client encodes the input as an indefinite-length map (0xBF ... 0xFF).
            pad -> new Cbor().indefiniteMap().text("DashboardName").text(DASHBOARD).text("DashboardBody").text(pad)
                             .breakCode().bytes(),
            pad -> response(200, CONTENT_TYPE, new Cbor().map(1).text("DashboardValidationMessages").array(1).map(2)
                                                         .text("DataPath").text("$").text("Message").text(pad)
                                                         .bytes(), HEADERS),
            response(400, CONTENT_TYPE, error("DashboardInvalidInputError", "Invalid dashboard body"), HEADERS),
            response(500, CONTENT_TYPE, error("InternalServiceFault", "injected retry"), HEADERS));
    }

    private static byte[] error(String name, String message) {
        return new Cbor().map(2).text("__type").text("com.amazonaws.cloudwatch#" + name).text("message").text(message)
                         .bytes();
    }

    @Override
    Invoker generated(boolean async, PaddedFixture fixture, URI endpoint, AutoCloseable http) {
        PutDashboardRequest request = PutDashboardRequest.builder().dashboardName(DASHBOARD)
                                                         .dashboardBody(fixture.pad()).build();
        Predicate<Throwable> error = t -> t instanceof DashboardInvalidInputErrorException;
        Function<Object, String> echo = r -> ((PutDashboardResponse) r).dashboardValidationMessages().get(0).message();
        if (async) {
            CloudWatchAsyncClient client = CloudWatchAsyncClient.builder().region(Region.US_EAST_1)
                .credentialsProvider(DynamoDbWorkloads.credentials()).endpointOverride(endpoint)
                .retryStrategy(DynamoDbWorkloads.retryStrategy()).httpClient((SdkAsyncHttpClient) http).build();
            return Invoker.async(client, () -> client.putDashboard(request), error, echo);
        }
        CloudWatchClient client = CloudWatchClient.builder().region(Region.US_EAST_1)
            .credentialsProvider(DynamoDbWorkloads.credentials()).endpointOverride(endpoint)
            .retryStrategy(DynamoDbWorkloads.retryStrategy()).httpClient((SdkSyncHttpClient) http).build();
        return Invoker.sync(client, () -> client.putDashboard(request), error, echo);
    }

    @Override
    Document dynamicInput(PaddedFixture fixture) {
        Map<String, Document> input = new LinkedHashMap<>();
        input.put("DashboardName", Document.of(DASHBOARD));
        input.put("DashboardBody", Document.of(fixture.pad()));
        return Document.of(input);
    }

    @Override
    String dynamicEcho(Document output) {
        return output.getMember("DashboardValidationMessages").asList().get(0).getMember("Message").asString();
    }

    /** A minimal CBOR writer (RFC 8949) for the fixture's maps, arrays and text strings. */
    static final class Cbor {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        Cbor map(int entries) {
            return head(5, entries);
        }

        Cbor indefiniteMap() {
            out.write(0xBF);
            return this;
        }

        Cbor breakCode() {
            out.write(0xFF);
            return this;
        }

        Cbor array(int elements) {
            return head(4, elements);
        }

        Cbor text(String s) {
            byte[] b = s.getBytes(StandardCharsets.UTF_8);
            head(3, b.length);
            out.writeBytes(b);
            return this;
        }

        private Cbor head(int major, long n) {
            int m = major << 5;
            if (n < 24) {
                out.write(m | (int) n);
            } else if (n < 0x100) {
                out.write(m | 24);
                out.write((int) n);
            } else if (n < 0x10000) {
                out.write(m | 25);
                out.write((int) (n >>> 8));
                out.write((int) n);
            } else {
                out.write(m | 26);
                for (int shift = 24; shift >= 0; shift -= 8) {
                    out.write((int) (n >>> shift));
                }
            }
            return this;
        }

        byte[] bytes() {
            return out.toByteArray();
        }
    }
}
