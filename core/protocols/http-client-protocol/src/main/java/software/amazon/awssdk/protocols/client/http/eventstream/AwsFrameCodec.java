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

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.schema.serde.event.FrameDecoder;
import software.amazon.awssdk.schema.serde.event.FrameEncoder;
import software.amazon.eventstream.Message;
import software.amazon.eventstream.MessageDecoder;

/**
 * The frame encoder and decoder of the AWS event stream encoding, over the SDK's {@code eventstream} library: the
 * same {@link Message#toByteBuffer()} and {@link MessageDecoder} the legacy SDK uses, so frames are byte-identical.
 *
 * <p>Ports smithy-java {@code AwsFrameEncoder} and {@code AwsFrameDecoder}, which wrap the same library, without the
 * decoder's {@code FrameProcessor} (the SDK does not transform inbound frames).
 */
@SdkInternalApi
final class AwsFrameCodec {
    static final FrameEncoder<AwsEventFrame> ENCODER = frame -> frame.unwrap().toByteBuffer();

    private AwsFrameCodec() {
    }

    static FrameDecoder<AwsEventFrame> newDecoder() {
        MessageDecoder decoder = new MessageDecoder();
        return buffer -> {
            decoder.feed(buffer);
            List<Message> messages = decoder.getDecodedMessages();
            List<AwsEventFrame> frames = new ArrayList<>(messages.size());
            for (Message message : messages) {
                frames.add(new AwsEventFrame(message));
            }
            return frames;
        };
    }
}
