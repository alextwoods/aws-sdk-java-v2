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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.function.Supplier;
import software.amazon.awssdk.checksums.ChecksumAlgorithm;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.StreamingBody;

/** Builds transformed HTTP bodies whose blocking and demand-driven views share one {@link ChunkTransform}. */
public final class TransformedBodies {
    private TransformedBodies() {
    }

    /**
     * A streaming body of {@code source} transformed by a fresh transform per consumption, with the given length
     * ({@code -1} if unknown). Replayability is the source's: every consumption consumes the source once. Both views
     * enforce {@code contentLength} and deliver at most {@link StreamingBody#maxChunkSize()} bytes per chunk.
     */
    public static StreamingBody transform(StreamingBody source, long contentLength,
                                          Supplier<? extends ChunkTransform> transforms) {
        int readSize = source.maxChunkSize();
        return StreamingBodies.of(contentLength, source.contentType().orElse(null), source.isReplayable(),
                                  () -> new TransformingInputStream(source.openStream(), transforms.get(), readSize),
                                  new TransformingPublisher(source, transforms), readSize);
    }

    /**
     * The body {@code body} transformed by {@code transforms}: a buffered body is transformed now into a buffered
     * body; a streaming body lazily, with {@code streamingLength} ({@code -1} if unknown).
     */
    public static HttpBody transform(HttpBody body, long streamingLength, Supplier<? extends ChunkTransform> transforms) {
        if (body.kind() == HttpBody.Kind.STREAMING) {
            return HttpBody.streaming(transform(body.streamingBody(), streamingLength, transforms));
        }
        return HttpBody.fromBytes(transformBytes(body.asByteArray(), transforms.get()));
    }

    /** {@code bytes} through {@code transform}, which is released. */
    public static byte[] transformBytes(byte[] bytes, ChunkTransform transform) {
        try (InputStream in = new TransformingInputStream(new ByteArrayInputStream(bytes), transform,
                                                          Math.max(1, bytes.length))) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The {@code algorithm} digest of every byte of {@code body}. A streaming body is read through its blocking view,
     * so it is consumed; callers only do this for replayable bodies, off any event loop.
     *
     * @throws UncheckedIOException if reading the body fails
     */
    public static byte[] digest(HttpBody body, ChecksumAlgorithm algorithm) {
        SdkChecksum checksum = SdkChecksum.create(algorithm);
        if (body.kind() != HttpBody.Kind.STREAMING) {
            checksum.update(body.asByteBuffer());
            return checksum.digest();
        }
        byte[] buffer = new byte[16 * 1024];
        try (InputStream in = body.streamingBody().openStream()) {
            int n;
            while ((n = in.read(buffer)) >= 0) {
                checksum.update(buffer, 0, n);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return checksum.digest();
    }
}
