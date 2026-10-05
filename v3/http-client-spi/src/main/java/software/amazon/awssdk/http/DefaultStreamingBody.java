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

package software.amazon.awssdk.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/** The {@link StreamingBody} of {@link StreamingBodies}: two native views plus one-shot consumption guarding. */
final class DefaultStreamingBody implements StreamingBody {
    private final long contentLength;
    private final String contentType;
    private final boolean replayable;
    private final int maxChunkSize;
    private final InputStreamPublisher.Source streams;
    private final Publisher<ByteBuffer> publisher;
    private final AtomicBoolean consumed = new AtomicBoolean();

    DefaultStreamingBody(long contentLength, String contentType, boolean replayable, int maxChunkSize,
                         InputStreamPublisher.Source streams, Publisher<ByteBuffer> publisher) {
        this.contentLength = contentLength;
        this.contentType = contentType;
        this.replayable = replayable;
        this.maxChunkSize = maxChunkSize;
        this.streams = streams;
        this.publisher = publisher;
    }

    @Override
    public OptionalLong contentLength() {
        return contentLength < 0 ? OptionalLong.empty() : OptionalLong.of(contentLength);
    }

    @Override
    public Optional<String> contentType() {
        return Optional.ofNullable(contentType);
    }

    @Override
    public boolean isReplayable() {
        return replayable;
    }

    @Override
    public boolean isAvailable() {
        return replayable || !consumed.get();
    }

    @Override
    public int maxChunkSize() {
        return maxChunkSize;
    }

    @Override
    public InputStream openStream() {
        if (!claim()) {
            throw consumedError();
        }
        try {
            return LengthCheckedInputStream.of(Objects.requireNonNull(streams.open(), "source stream"),
                                               contentLength);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        Objects.requireNonNull(subscriber, "subscriber");
        if (!claim()) {
            subscriber.onSubscribe(Demand.NOOP);
            subscriber.onError(consumedError());
            return;
        }
        publisher.subscribe(subscriber);
    }

    private boolean claim() {
        return replayable || consumed.compareAndSet(false, true);
    }

    private static IllegalStateException consumedError() {
        return new IllegalStateException("The body is not replayable and was already consumed");
    }

    /** Renders metadata only; content may be sensitive. */
    @Override
    public String toString() {
        return "StreamingBody[" + (contentLength < 0 ? "unknown length" : contentLength + " bytes")
               + (replayable ? ", replayable" : ", one-shot") + "]";
    }
}
