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

package software.amazon.awssdk.checksums.internal;

import java.nio.ByteBuffer;

/**
 * An incremental transformation of one body consumption: each source chunk in order, then the end of the source.
 *
 * <p>The same transform drives both views of a transformed body, the blocking {@link TransformingInputStream} and the
 * demand-driven {@link TransformingPublisher}, so both views produce identical bytes. One instance serves exactly one
 * consumption; its methods are called serially, never concurrently.
 *
 * <p>A chunk passed to {@link #onChunk} is valid only during the call; a transform that keeps bytes must copy them. A
 * returned buffer is handed to the consumer, so it must either be the given chunk (pass-through) or a buffer the
 * transform never modifies afterwards. An empty result delivers nothing.
 */
public interface ChunkTransform {
    /** The bytes to deliver for {@code chunk}, possibly none. */
    ByteBuffer onChunk(ByteBuffer chunk) throws Exception;

    /** The final bytes to deliver after the last chunk, possibly none; throwing fails the consumption. */
    ByteBuffer onComplete() throws Exception;

    /** Releases held resources; called exactly once when the consumption completes, fails or is cancelled. */
    default void release() {
    }
}
