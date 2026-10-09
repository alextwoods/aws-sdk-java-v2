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
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.NotFoundException;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.smithy.java.core.serde.document.Document;

/**
 * The awsQuery fixture: SNS {@code Publish} (a form-encoded {@code Action=Publish} body, routed by that body). The
 * {@code Message} is the pad; the success response echoes it as the {@code MessageId}. Error cells get the modeled
 * {@code NotFoundException} (query code {@code NotFound}, 404); retry cells the modeled, retryable
 * {@code InternalErrorException} (query code {@code InternalError}, 500).
 */
final class SnsWorkloads extends FiniteWorkloads.Provider {
    static final String TOPIC = SnsPublishFixture.TOPIC;
    static final String NS = SnsPublishFixture.NS;
    static final String CONTENT_TYPE = SnsPublishFixture.CONTENT_TYPE;

    SnsWorkloads() {
        super("sns-publish", BenchProtocol.AWS_QUERY, "sns", "com.amazonaws.sns#AmazonSimpleNotificationService",
              "Publish", "com.amazonaws.sns#NotFoundException");
    }

    @Override
    public PaddedFixture fixture(BenchmarkCell cell) {
        return SnsPublishFixture.create(cell.payload());
    }

    @Override
    Invoker generated(boolean async, PaddedFixture fixture, URI endpoint, AutoCloseable http) {
        PublishRequest request = PublishRequest.builder().topicArn(TOPIC).message(fixture.pad()).build();
        Predicate<Throwable> error = t -> t instanceof NotFoundException;
        Function<Object, String> echo = r -> ((PublishResponse) r).messageId();
        if (async) {
            SnsAsyncClient client = SnsAsyncClient.builder().region(Region.US_EAST_1)
                .credentialsProvider(DynamoDbWorkloads.credentials()).endpointOverride(endpoint)
                .retryStrategy(DynamoDbWorkloads.retryStrategy()).httpClient((SdkAsyncHttpClient) http).build();
            return Invoker.async(client, () -> client.publish(request), error, echo);
        }
        SnsClient client = SnsClient.builder().region(Region.US_EAST_1)
            .credentialsProvider(DynamoDbWorkloads.credentials()).endpointOverride(endpoint)
            .retryStrategy(DynamoDbWorkloads.retryStrategy()).httpClient((SdkSyncHttpClient) http).build();
        return Invoker.sync(client, () -> client.publish(request), error, echo);
    }

    @Override
    Document dynamicInput(PaddedFixture fixture) {
        Map<String, Document> input = new LinkedHashMap<>();
        input.put("TopicArn", Document.of(TOPIC));
        input.put("Message", Document.of(fixture.pad()));
        return Document.of(input);
    }

    @Override
    String dynamicEcho(Document output) {
        return output.getMember("MessageId").asString();
    }
}
