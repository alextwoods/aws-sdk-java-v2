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

import java.nio.ByteBuffer;
import java.util.List;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Decodes frames from bytes. A decoder holds the bytes of an incomplete frame until the next call, so it is
 * single-use and not thread safe.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.serde.event.FrameDecoder}.
 *
 * @param <F> the type of the frame
 */
@SdkPublicApi
public interface FrameDecoder<F extends Frame<?>> {
    /**
     * Decodes zero or more frames from a buffer, keeping excess bytes for the next call.
     *
     * @param buffer the buffer to read frames from
     * @return the frames completed by this buffer, in order
     */
    List<F> decode(ByteBuffer buffer);
}
