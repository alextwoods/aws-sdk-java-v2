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

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import software.amazon.awssdk.benchmark.harness.FiniteWorkloads.Invoker;
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
    static final String DASHBOARD = CloudWatchPutDashboardFixture.DASHBOARD;
    static final String CONTENT_TYPE = CloudWatchPutDashboardFixture.CONTENT_TYPE;
    static final Map<String, String> HEADERS = CloudWatchPutDashboardFixture.HEADERS;

    CloudWatchWorkloads() {
        super("cloudwatch-putdashboard", BenchProtocol.RPC_V2_CBOR, "cloudwatch",
              "com.amazonaws.cloudwatch#GraniteServiceVersion20100801", "PutDashboard",
              "com.amazonaws.cloudwatch#DashboardInvalidInputError");
    }

    @Override
    public PaddedFixture fixture(BenchmarkCell cell) {
        return CloudWatchPutDashboardFixture.create(cell.payload());
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

}
