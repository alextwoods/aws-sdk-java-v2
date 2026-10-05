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

package software.amazon.awssdk.core.internal.async;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.core.async.AsyncRequestBody;

/** An {@link AsyncRequestBody} of known length whose publisher is a demand-exact {@link ChunkPublisher}. */
public final class ChunkedAsyncRequestBody implements AsyncRequestBody {
    private final ChunkPublisher publisher;
    private final long length;
    private final String contentType;
    private final String name;

    public ChunkedAsyncRequestBody(ChunkSource.Factory sources, long length, String contentType, String name) {
        if (length < 0) {
            throw new IllegalArgumentException("A content length is non-negative: " + length);
        }
        this.publisher = new ChunkPublisher(sources);
        this.length = length;
        this.contentType = Objects.requireNonNull(contentType, "contentType");
        this.name = Objects.requireNonNull(name, "name");
    }

    @Override
    public Optional<Long> contentLength() {
        return Optional.of(length);
    }

    @Override
    public String contentType() {
        return contentType;
    }

    /** Every subscription reads a fresh chunk source from the first byte. */
    @Override
    public boolean isReplayable() {
        return true;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        publisher.subscribe(subscriber);
    }

    @Override
    public String toString() {
        return "AsyncRequestBody(" + name + ", contentLength=" + length + ", contentType=" + contentType + ")";
    }
}
