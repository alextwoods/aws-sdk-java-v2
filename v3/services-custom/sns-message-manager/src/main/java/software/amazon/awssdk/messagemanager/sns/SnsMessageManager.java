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

package software.amazon.awssdk.messagemanager.sns;

import java.io.InputStream;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.messagemanager.sns.internal.DefaultSnsMessageManager;
import software.amazon.awssdk.messagemanager.sns.model.SnsMessage;
import software.amazon.awssdk.regions.Region;

/**
 * Message manager for validating SNS message signatures. Create an instance using {@link #builder()}.
 *
 * <p>This manager provides automatic validation of SNS message signatures received via HTTP/HTTPS endpoints,
 * ensuring that messages originate from Amazon SNS and have not been modified during transmission.
 * It supports both SignatureVersion1 (SHA1) and SignatureVersion2 (SHA256) as per AWS SNS standards.
 *
 * <p>The manager handles certificate retrieval, caching, and validation automatically, supporting different
 * AWS regions and partitions (aws, aws-gov, aws-cn). Signing certificates are only fetched over HTTPS from the SNS
 * endpoint of the configured region, and must be valid and issued to the regional SNS signing name.
 *
 * <p>Failures to parse or verify a message are reported as {@code SdkClientException}s whose messages never contain
 * the message body, subscription tokens or other message content.
 *
 * <p>Basic usage with default configuration (an HTTP implementation such as {@code url-connection-client} must be on
 * the class path):
 * <pre>
 * {@code
 * SnsMessageManager messageManager = SnsMessageManager.builder().region(Region.US_WEST_2).build();
 *
 * try {
 *     SnsMessage validatedMessage = messageManager.parseMessage(messageBody);
 *     String messageContent = validatedMessage.message();
 *     String topicArn = validatedMessage.topicArn();
 *     // Process the validated message
 * } catch (SdkClientException e) {
 *     // Handle validation failure
 * }
 * }
 * </pre>
 *
 * <p>Advanced usage with custom HTTP client:
 * <pre>
 * {@code
 * SnsMessageManager messageManager = SnsMessageManager.builder()
 *     .region(Region.US_WEST_2)
 *     .httpClient(ApacheHttpClient.create())
 *     .build();
 * }
 * </pre>
 *
 * <p>Implementations are thread safe.
 *
 * @see SnsMessage
 * @see Builder
 */
public interface SnsMessageManager extends AutoCloseable {

    /**
     * Creates a builder for configuring and creating an {@link SnsMessageManager}.
     *
     * @return A new builder.
     */
    static Builder builder() {
        return DefaultSnsMessageManager.builder();
    }

    /**
     * Parses and validates an SNS message from a stream.
     * <p>
     * This method reads the JSON message payload, validates the signature, returns a parsed SNS message object with all
     * message attributes if validation succeeds. The stream is read to its end and is not closed.
     *
     * @param messageStream The binary stream representation of the SNS message.
     * @return The parsed SNS message.
     */
    SnsMessage parseMessage(InputStream messageStream);

    /**
     * Parses and validates an SNS message from a string.
     * <p>
     * This method reads the JSON message payload, validates the signature, returns a parsed SNS message object with all
     * message attributes if validation succeeds.
     *
     * @param messageContent The string representation of the SNS message.
     * @return the parsed SNS message.
     */
    SnsMessage parseMessage(String messageContent);

    /**
     * Close this {@code SnsMessageManager}, releasing any resources it owned.
     * <p>
     * <b>Note:</b> if you provided your own {@link SdkSyncHttpClient}, you must close it separately.
     */
    @Override
    void close();

    interface Builder {

        /**
         * Sets the HTTP client to use for certificate retrieval. The caller is responsible for closing this HTTP client after
         * the {@code SnsMessageManager} is closed.
         *
         * <p>When no client is set, {@link #build()} creates one from the installed HTTP implementation with a 10 second
         * connection timeout and a 30 second read timeout, and {@link SnsMessageManager#close()} closes it.
         *
         * @param httpClient The HTTP client to use for fetching signing certificates.
         * @return This builder for method chaining.
         */
        Builder httpClient(SdkSyncHttpClient httpClient);

        /**
         * Sets the AWS region for certificate validation. This region must match the SNS region where the messages originate.
         * This is a required parameter.
         *
         * @param region The AWS region where the SNS messages originate.
         * @return This builder for method chaining.
         */
        Builder region(Region region);

        /**
         * Builds an instance of {@link SnsMessageManager} based on the supplied configurations.
         *
         * @return An initialized SnsMessageManager ready to validate SNS messages.
         */
        SnsMessageManager build();
    }
}
