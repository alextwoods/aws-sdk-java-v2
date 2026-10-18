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
 * Creates encoders that encode events into frames and frames into bytes.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.serde.event.EventEncoderFactory}, without
 * {@code withFrameProcessor}: the SDK signs the encoded bytes with its existing event stream signer.
 *
 * @param <F> the type of the frame
 */
@SdkPublicApi
public interface EventEncoderFactory<F extends Frame<?>> {
    /**
     * Returns a new event encoder.
     *
     * @return a new event encoder
     */
    EventEncoder<F> newEventEncoder();

    /**
     * Returns a new frame encoder.
     *
     * @return a new frame encoder
     */
    FrameEncoder<F> newFrameEncoder();

    /**
     * Returns the content type of the encoded stream.
     *
     * @return the content type
     */
    String contentType();
}
