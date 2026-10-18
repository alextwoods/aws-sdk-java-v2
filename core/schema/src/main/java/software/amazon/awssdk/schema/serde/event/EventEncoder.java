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
 * Encodes events of an event stream into frames.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.serde.event.EventEncoder}.
 *
 * @param <F> the type of the frame
 */
@SdkPublicApi
public interface EventEncoder<F extends Frame<?>> {
    /**
     * Returns the frame that contains the encoded item: an event stream union value, an event, or the initial
     * request or response (a structure with the event stream member).
     *
     * @param item the item to encode
     * @return the frame that contains the encoded item
     */
    F encode(SerializableStruct item);

    /**
     * Returns the frame that contains the encoded exception.
     *
     * @param exception the exception to encode
     * @return the frame that contains the encoded exception
     */
    F encodeFailure(Throwable exception);

    /**
     * Returns the frame to send when the stream ends, as the protocol or the auth scheme that signs the frames
     * defines it, or null if no closing frame is sent.
     *
     * @return the closing frame, or null
     */
    default F closingFrame() {
        return null;
    }
}
