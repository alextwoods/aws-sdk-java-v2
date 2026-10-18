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

package software.amazon.awssdk.schema.serde.event;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.SerializableStruct;

/**
 * Decodes frames into the events of an event stream.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.serde.event.EventDecoder}. Two deviations:
 * {@link #decode(Frame)} returns {@code Object}, because an SDK event stream delivers an unknown event type as the
 * generated visitor's unknown-event value, which is not a {@link SerializableStruct}; and
 * {@link #decodeInitialEvent(Frame)} takes no {@code EventStream}, because SDK request and response classes do not
 * hold the event stream (the client method takes a {@code Publisher} or a response handler).
 *
 * @param <F> the type of the frame
 */
@SdkPublicApi
public interface EventDecoder<F extends Frame<?>> {
    /**
     * Decodes the frame into an event.
     *
     * @param frame the frame to decode
     * @return the event
     * @throws EventStreamingException if the frame is an error message
     * @throws RuntimeException the modeled exception, if the frame is an exception message for a modeled error
     */
    Object decode(F frame);

    /**
     * Decodes the frame into the initial request or response: the non-stream members of the operation's input or
     * output.
     *
     * @param frame the frame that contains the initial event
     * @return the initial event
     */
    SerializableStruct decodeInitialEvent(F frame);
}
