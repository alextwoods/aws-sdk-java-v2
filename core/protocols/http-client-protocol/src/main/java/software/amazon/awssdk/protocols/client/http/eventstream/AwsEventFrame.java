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

package software.amazon.awssdk.protocols.client.http.eventstream;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.serde.event.Frame;
import software.amazon.awssdk.utils.Validate;
import software.amazon.eventstream.Message;

/**
 * A frame of the AWS event stream encoding ({@code application/vnd.amazon.eventstream}): one {@link Message}.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.aws.events.AwsEventFrame}.
 */
@SdkPublicApi
public final class AwsEventFrame implements Frame<Message> {
    private final Message message;

    /**
     * @param message the message
     * @throws NullPointerException if {@code message} is null
     */
    public AwsEventFrame(Message message) {
        this.message = Validate.paramNotNull(message, "message");
    }

    @Override
    public Message unwrap() {
        return message;
    }
}
