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

package software.amazon.awssdk.eventstream;

import java.util.Objects;

/**
 * The per-frame transform of an outgoing event stream, applied to every message after event serialization and before
 * framing, in stream order. This is the signing hook: {@link EventStreamSigner} (for example SigV4 signature
 * chaining) wraps each message in a signed envelope carrying {@code :date} and {@code :chunk-signature} headers, with
 * the encoded inner message as its payload, and supplies the empty signed trailing frame as {@link #closingFrame()}.
 *
 * <p>An instance is stateful per stream (a signature chain carries the prior signature) and is invoked from one
 * thread at a time, in order: {@link #transform} for each message, then {@link #closingFrame()} once when the writer
 * closes normally. It is not invoked after the stream fails or is cancelled.
 */
@FunctionalInterface
public interface FrameTransformer {
    /** The message to send in place of {@code message}; never {@code null}. */
    Message transform(Message message);

    /** The frame to send after the last message, or {@code null} for none (the default). */
    default Message closingFrame() {
        return null;
    }

    /** The transform that sends every message unchanged and no closing frame. */
    static FrameTransformer identity() {
        return message -> message;
    }

    /**
     * This transform followed by {@code next}: messages pass through this then {@code next}; the closing frame is this
     * transform's closing frame (if any) passed through {@code next}, else {@code next}'s own.
     */
    default FrameTransformer andThen(FrameTransformer next) {
        Objects.requireNonNull(next, "next");
        FrameTransformer first = this;
        return new FrameTransformer() {
            @Override
            public Message transform(Message message) {
                return next.transform(first.transform(message));
            }

            @Override
            public Message closingFrame() {
                Message closing = first.closingFrame();
                return closing != null ? next.transform(closing) : next.closingFrame();
            }
        };
    }
}
