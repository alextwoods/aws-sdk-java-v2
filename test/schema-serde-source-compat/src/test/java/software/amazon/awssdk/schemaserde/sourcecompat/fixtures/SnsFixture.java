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

package software.amazon.awssdk.schemaserde.sourcecompat.fixtures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesResponse;
import software.amazon.awssdk.services.sns.model.ListSmsSandboxPhoneNumbersResponse;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.NotFoundException;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SMSSandboxPhoneNumber;
import software.amazon.awssdk.services.sns.model.SMSSandboxPhoneNumberVerificationStatus;
import software.amazon.awssdk.services.sns.model.SnsException;
import software.amazon.awssdk.services.sns.model.Topic;

/**
 * SNS, written against the Baseline_Build public API (Requirement 16.8).
 */
public final class SnsFixture {
    public static final String TOPIC_ARN = "arn:aws:sns:us-west-2:123456789012:fixture";

    private SnsFixture() {
    }

    public static SnsClient syncClient(SdkHttpClient httpClient, SdkPlugin plugin, ClientSettings.CallCounter counter) {
        return SnsClient.builder()
                        .region(ClientSettings.REGION)
                        .credentialsProvider(ClientSettings.credentials())
                        .overrideConfiguration(ClientSettings.overrideConfiguration(counter))
                        .endpointOverride(ClientSettings.ENDPOINT)
                        .httpClient(httpClient)
                        .addPlugin(plugin)
                        .build();
    }

    public static SnsAsyncClient asyncClient(SdkAsyncHttpClient httpClient, SdkPlugin plugin) {
        return SnsAsyncClient.builder()
                             .region(ClientSettings.REGION)
                             .credentialsProvider(ClientSettings.credentials())
                             .endpointOverride(ClientSettings.ENDPOINT)
                             .httpClient(httpClient)
                             .addPlugin(plugin)
                             .build();
    }

    public static PublishRequest publishRequest() {
        Map<String, MessageAttributeValue> attributes = new TreeMap<>();
        attributes.put("color", MessageAttributeValue.builder().dataType("String").stringValue("blue").build());
        return PublishRequest.builder()
                             .topicArn(TOPIC_ARN)
                             .subject("fixture")
                             .message("hello")
                             .messageAttributes(attributes)
                             .build();
    }

    public static PublishResponse publishResponse() {
        return PublishResponse.builder().messageId("msg-1").sequenceNumber("10").build();
    }

    public static String publish(SnsClient client) {
        PublishResponse response = client.publish(publishRequest().toBuilder().subject("copy").build());
        return response.messageId() + " " + response.sequenceNumber();
    }

    public static GetTopicAttributesResponse getTopicAttributesResponse() {
        Map<String, String> attributes = new TreeMap<>();
        attributes.put("DisplayName", "Fixture");
        attributes.put("SubscriptionsConfirmed", "2");
        return GetTopicAttributesResponse.builder().attributes(attributes).build();
    }

    /**
     * Reads a response through its getters: a string-to-string map.
     */
    public static CompletableFuture<String> getTopicAttributesAsync(SnsAsyncClient client) {
        return client.getTopicAttributes(r -> r.topicArn(TOPIC_ARN))
                     .thenApply(r -> r.hasAttributes() + " " + new TreeMap<>(r.attributes()));
    }

    public static ListSmsSandboxPhoneNumbersResponse sandboxPhoneNumbersResponse() {
        return ListSmsSandboxPhoneNumbersResponse.builder()
                                                 .phoneNumbers(SMSSandboxPhoneNumber.builder()
                                                                                    .phoneNumber("+12065550100")
                                                                                    .status(SMSSandboxPhoneNumberVerificationStatus.VERIFIED)
                                                                                    .build())
                                                 .build();
    }

    /**
     * Paginates a list of structures with an enum.
     */
    public static List<String> sandboxPhoneNumbers(SnsClient client) {
        List<String> numbers = new ArrayList<>();
        client.listSMSSandboxPhoneNumbersPaginator(r -> r.maxResults(10))
              .phoneNumbers()
              .forEach(n -> numbers.add(n.phoneNumber() + " " + n.status() + "/" + n.statusAsString()));
        return numbers;
    }

    /**
     * Paginates with the async publisher.
     */
    public static CompletableFuture<List<String>> topicArnsAsync(SnsAsyncClient client) {
        List<String> arns = Collections.synchronizedList(new ArrayList<>());
        return client.listTopicsPaginator().topics().subscribe((Topic t) -> arns.add(t.topicArn())).thenApply(v -> arns);
    }

    public static NotFoundException notFound() {
        return NotFoundException.builder().message("Topic does not exist").statusCode(404).build();
    }

    public static String getMissingTopic(SnsClient client) {
        try {
            client.getTopicAttributes(r -> r.topicArn(TOPIC_ARN + "-missing"));
            return "no exception";
        } catch (NotFoundException e) {
            return "NotFoundException " + e.statusCode() + " " + e.awsErrorDetails().errorCode();
        } catch (SnsException e) {
            return "SnsException " + e.awsErrorDetails().errorCode();
        }
    }
}
