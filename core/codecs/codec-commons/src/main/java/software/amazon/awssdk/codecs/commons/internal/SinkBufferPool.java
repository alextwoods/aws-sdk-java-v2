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

package software.amazon.awssdk.codecs.commons.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;

/**
 * Lends the scratch buffer that a serializer writing to an {@code OutputStream} sink fills and drains into the sink.
 * The buffer is held from the serializer's creation to its {@code close()}, so a request serializes without allocating
 * one. SDK addition; smithy-java pools whole serializers in {@code Codec.serialize} instead.
 *
 * <p>Arrays are not cleared between holders: a holder writes from index 0 and reads only what it wrote. An array is
 * held by one serializer at a time ({@link StripedPool} hands each pooled item to one caller), must be released at most
 * once, and must not be used after release. An array that is never released is only garbage.
 */
@SdkInternalApi
@ThreadSafe
public final class SinkBufferPool {
    /**
     * The length of every array the pool lends.
     */
    public static final int BUFFER_SIZE = 1024;

    private static final StripedPool<byte[], Void> POOL = new BufferStripedPool();

    private SinkBufferPool() {
    }

    /**
     * Returns a pooled array of {@link #BUFFER_SIZE} bytes, or a new one when none is free. Its contents are undefined.
     *
     * @return an array the caller holds until {@link #release}
     */
    public static byte[] acquire() {
        return POOL.acquire(null);
    }

    /**
     * Returns an array to the pool. Arrays of any length other than {@link #BUFFER_SIZE}, for example one a serializer
     * grew, are dropped.
     *
     * @param buffer the array; the caller must not use it after this call
     */
    public static void release(byte[] buffer) {
        POOL.release(buffer);
    }

    private static final class BufferStripedPool extends StripedPool<byte[], Void> {
        @Override
        protected byte[] create(Void context) {
            return new byte[BUFFER_SIZE];
        }

        @Override
        protected boolean canPool(byte[] buffer) {
            return buffer.length == BUFFER_SIZE;
        }

        @Override
        protected boolean reset(byte[] buffer, Void context) {
            return true;
        }
    }
}
