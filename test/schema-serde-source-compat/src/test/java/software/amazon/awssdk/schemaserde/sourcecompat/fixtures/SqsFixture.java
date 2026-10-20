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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

/**
 * SQS, written against the Baseline_Build public API (Requirement 16.8).
 */
public final class SqsFixture {
    public static final String QUEUE_URL = "https://sqs.us-west-2.amazonaws.com/123456789012/fixture";

    private SqsFixture() {
    }

    public static SqsClient syncClient(SdkHttpClient httpClient, SdkPlugin plugin, ClientSettings.CallCounter counter) {
        return SqsClient.builder()
                        .region(ClientSettings.REGION)
                        .credentialsProvider(ClientSettings.credentials())
                        .overrideConfiguration(ClientSettings.overrideConfiguration(counter))
                        .endpointOverride(ClientSettings.ENDPOINT)
                        .httpClient(httpClient)
                        .checksumValidationEnabled(false)
                        .addPlugin(plugin)
                        .build();
    }

    public static SqsAsyncClient asyncClient(SdkAsyncHttpClient httpClient, SdkPlugin plugin) {
        return SqsAsyncClient.builder()
                             .region(ClientSettings.REGION)
                             .credentialsProvider(ClientSettings.credentials())
                             .endpointOverride(ClientSettings.ENDPOINT)
                             .httpClient(httpClient)
                             .addPlugin(plugin)
                             .build();
    }

    public static ReceiveMessageRequest receiveMessageRequest() {
        return ReceiveMessageRequest.builder()
                                    .queueUrl(QUEUE_URL)
                                    .maxNumberOfMessages(10)
                                    .messageSystemAttributeNames(MessageSystemAttributeName.SENT_TIMESTAMP)
                                    .messageAttributeNames("All")
                                    .waitTimeSeconds(0)
                                    .build();
    }

    public static ReceiveMessageResponse receiveMessageResponse() {
        Map<String, MessageAttributeValue> attributes = new TreeMap<>();
        attributes.put("color", MessageAttributeValue.builder().dataType("String").stringValue("blue").build());
        attributes.put("raw", MessageAttributeValue.builder()
                                                   .dataType("Binary")
                                                   .binaryValue(SdkBytes.fromUtf8String("bytes"))
                                                   .build());
        Message message = Message.builder()
                                 .messageId("m-1")
                                 .receiptHandle("rh-1")
                                 .body("hello")
                                 .attributesWithStrings(singleton(MessageSystemAttributeName.SENT_TIMESTAMP.toString(),
                                                                  "1700000000000"))
                                 .messageAttributes(attributes)
                                 .build();
        return ReceiveMessageResponse.builder().messages(message).build();
    }

    /**
     * Reads a response through its getters: a list of structures, an enum-keyed map with {@code xxxAsStrings}, and a
     * map of nested structures holding a blob.
     */
    public static String summarize(ReceiveMessageResponse response) {
        StringBuilder result = new StringBuilder();
        for (Message message : response.messages()) {
            result.append(message.messageId()).append(' ').append(message.body())
                  .append(' ').append(message.attributes().get(MessageSystemAttributeName.SENT_TIMESTAMP))
                  .append(' ').append(message.attributesAsStrings().keySet());
            for (Map.Entry<String, MessageAttributeValue> e : new TreeMap<>(message.messageAttributes()).entrySet()) {
                MessageAttributeValue value = e.getValue();
                result.append(' ').append(e.getKey()).append('=').append(value.dataType()).append(':')
                      .append(value.stringValue() != null ? value.stringValue() : value.binaryValue().asUtf8String());
            }
        }
        return result.toString();
    }

    public static String receiveMessage(SqsClient client) {
        return summarize(client.receiveMessage(receiveMessageRequest().toBuilder().visibilityTimeout(30).build()));
    }

    public static GetQueueAttributesResponse getQueueAttributesResponse() {
        Map<QueueAttributeName, String> attributes = new TreeMap<>();
        attributes.put(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES, "3");
        attributes.put(QueueAttributeName.VISIBILITY_TIMEOUT, "30");
        return GetQueueAttributesResponse.builder().attributes(attributes).build();
    }

    public static CompletableFuture<String> getQueueAttributesAsync(SqsAsyncClient client) {
        return client.getQueueAttributes(r -> r.queueUrl(QUEUE_URL).attributeNames(QueueAttributeName.ALL))
                     .thenApply(r -> r.attributes().get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)
                                     + " " + new TreeMap<>(r.attributesAsStrings()));
    }

    public static List<String> queueUrls(SqsClient client) {
        List<String> urls = new ArrayList<>();
        client.listQueuesPaginator(r -> r.queueNamePrefix("fix")).queueUrls().forEach(urls::add);
        return urls;
    }

    public static QueueDoesNotExistException queueDoesNotExist() {
        return QueueDoesNotExistException.builder().message("The queue does not exist").statusCode(400).build();
    }

    public static String getMissingQueueUrl(SqsClient client) {
        try {
            client.getQueueUrl(r -> r.queueName("missing"));
            return "no exception";
        } catch (QueueDoesNotExistException e) {
            return "QueueDoesNotExistException " + e.statusCode() + " " + e.awsErrorDetails().errorCode();
        } catch (SqsException e) {
            return "SqsException " + e.awsErrorDetails().errorCode();
        }
    }

    private static Map<String, String> singleton(String key, String value) {
        Map<String, String> map = new TreeMap<>();
        map.put(key, value);
        return map;
    }
}
