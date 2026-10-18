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

/**
 * Creates decoders that decode bytes into frames and frames into events.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.serde.event.EventDecoderFactory}.
 *
 * @param <F> the type of the frame
 */
@SdkPublicApi
public interface EventDecoderFactory<F extends Frame<?>> {
    /**
     * Returns a new event decoder.
     *
     * @return a new event decoder
     */
    EventDecoder<F> newEventDecoder();

    /**
     * Returns a new frame decoder.
     *
     * @return a new frame decoder
     */
    FrameDecoder<F> newFrameDecoder();
}
