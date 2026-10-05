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

package software.amazon.awssdk.protocols.core.eventstream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import software.amazon.awssdk.eventstream.EventStreamFormatException;
import software.amazon.awssdk.eventstream.FrameTransformer;
import software.amazon.awssdk.eventstream.Message;
import software.amazon.awssdk.eventstream.MessageDecoder;
import software.amazon.awssdk.eventstream.MessageEncoder;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.event.EventDecoder;
import software.amazon.smithy.java.core.serde.event.EventDecoderFactory;
import software.amazon.smithy.java.core.serde.event.EventEncoder;
import software.amazon.smithy.java.core.serde.event.EventEncoderFactory;
import software.amazon.smithy.java.core.serde.event.EventStream;
import software.amazon.smithy.java.core.serde.event.Frame;
import software.amazon.smithy.java.core.serde.event.FrameDecoder;
import software.amazon.smithy.java.core.serde.event.FrameEncoder;

/**
 * Adapters between the V3 framing ({@code software.amazon.awssdk.eventstream}) and the Smithy Java event stream
 * reader/writer, which generated types hold as their event stream member values ({@code EventStream} is sealed to
 * the Smithy Java implementations).
 */
final class MessageFrames {
    private MessageFrames() {
    }

    /** A V3 message as a Smithy Java frame. */
    record MessageFrame(Message message) implements Frame<Message> {
        @Override
        public Message unwrap() {
            return message;
        }
    }

    /**
     * Encodes the writer's events (and its initial event, if any) through {@code codec} and {@code transformer}.
     * Nothing is encoded or transformed once {@code stream} is cancelled or failed; a transform failure fails
     * {@code stream} (see {@link OutgoingEventStream}).
     */
    static EventEncoderFactory<MessageFrame> encoders(EventStreamCodec codec, FrameTransformer transformer,
                                                      OutgoingEventStream stream) {
        return new EventEncoderFactory<>() {
            @Override
            public EventEncoder<MessageFrame> newEventEncoder() {
                return new EventEncoder<>() {
                    @Override
                    public MessageFrame encode(SerializableStruct item) {
                        stream.checkActive();
                        Message message = item.schema().id().equals(codec.union().id())
                                          ? codec.encodeEvent(item)
                                          : codec.encodeInitialMessage(item, EventStreamCodec.INITIAL_REQUEST);
                        return new MessageFrame(transform(message));
                    }

                    @Override
                    public MessageFrame encodeFailure(Throwable exception) {
                        stream.checkActive();
                        // The error code names the failure type only; its message may carry caller data.
                        return new MessageFrame(transform(
                            codec.encodeError(exception.getClass().getSimpleName(), "The event stream failed")));
                    }

                    @Override
                    public MessageFrame closingFrame() {
                        if (!stream.isActive()) {
                            return null;
                        }
                        Message closing;
                        try {
                            closing = transformer.closingFrame();
                        } catch (RuntimeException e) {
                            // The writer is already closing: returning no frame lets it end the pipe, and the stream
                            // then fails the transport with this failure instead of ending normally.
                            stream.failed(e);
                            return null;
                        }
                        return closing == null ? null : new MessageFrame(closing);
                    }

                    private Message transform(Message message) {
                        try {
                            return transformer.transform(message);
                        } catch (RuntimeException e) {
                            stream.failed(e);
                            throw e;
                        }
                    }
                };
            }

            @Override
            public FrameEncoder<MessageFrame> newFrameEncoder() {
                return frame -> MessageEncoder.encode(frame.unwrap());
            }

            @Override
            public String contentType() {
                return EventStreams.CONTENT_TYPE;
            }
        };
    }

    /**
     * Decodes frames with a bounded {@link MessageDecoder} and events through {@code codec}. A terminal failure (a
     * malformed frame, a modeled exception or error message) terminates {@code input}, releasing the response body, so
     * the reader signals it exactly once and then reports the end of the stream.
     *
     * @param initial decodes the initial-response message, or {@code null} if the protocol sends none
     */
    static EventDecoderFactory<MessageFrame> decoders(EventStreamCodec codec, SdkHttpResponse response,
                                                      int maxMessageSize, MessageBoundaryInputStream input,
                                                      Function<InitialEvent, SerializableStruct> initial) {
        MessageDecoder frames = new MessageDecoder(maxMessageSize);
        return new EventDecoderFactory<>() {
            @Override
            public EventDecoder<MessageFrame> newEventDecoder() {
                return new EventDecoder<>() {
                    @Override
                    public SerializableStruct decode(MessageFrame frame) {
                        try {
                            return codec.decodeEvent(frame.unwrap(), response);
                        } catch (RuntimeException e) {
                            input.terminate();
                            throw e;
                        }
                    }

                    @Override
                    public SerializableStruct decodeInitialEvent(MessageFrame frame, EventStream<?> stream) {
                        try {
                            return initial.apply(new InitialEvent(frame.unwrap(), stream));
                        } catch (RuntimeException e) {
                            input.terminate();
                            throw e;
                        }
                    }
                };
            }

            @Override
            public FrameDecoder<MessageFrame> newFrameDecoder() {
                return buffer -> {
                    List<MessageFrame> out = new ArrayList<>(1);
                    try {
                        for (Message message : frames.decode(buffer)) {
                            out.add(new MessageFrame(message));
                        }
                    } catch (EventStreamFormatException e) {
                        input.terminate();
                        throw e;
                    }
                    return out;
                };
            }
        };
    }

    /** The first message of an RPC response stream and the stream to set on the output. */
    record InitialEvent(Message message, EventStream<?> stream) {
    }

    /**
     * An input stream over the response body that returns at most the rest of the current message per read, so the
     * Smithy Java reader decodes at most one frame (and queues at most one event) per read and a terminal event is
     * never reordered before earlier events. A stream that ends inside a message fails with
     * {@link EventStreamFormatException} ({@code TRUNCATED_MESSAGE}); {@link #terminate()} closes the body and ends
     * the stream.
     */
    static final class MessageBoundaryInputStream extends InputStream {
        private final InputStream source;
        private final int maxMessageSize;
        private final byte[] lengthBytes = new byte[4];
        private int pending;
        private int pendingPos;
        private long remaining;
        private boolean inMessage;
        private volatile boolean terminated;

        MessageBoundaryInputStream(InputStream source, int maxMessageSize) {
            this.source = source;
            this.maxMessageSize = maxMessageSize;
        }

        void terminate() {
            if (!terminated) {
                terminated = true;
                try {
                    source.close();
                } catch (IOException | RuntimeException e) {
                    // Releasing the body is best effort; the stream is already failed.
                }
            }
        }

        @Override
        public int read() throws IOException {
            byte[] one = new byte[1];
            int n = read(one, 0, 1);
            return n <= 0 ? -1 : one[0] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (terminated) {
                return -1;
            }
            if (len == 0) {
                return 0;
            }
            if (!inMessage) {
                int got = 0;
                while (got < 4) {
                    int n = source.read(lengthBytes, got, 4 - got);
                    if (n < 0) {
                        break;
                    }
                    got += n;
                }
                if (got == 0) {
                    return -1;
                }
                if (got < 4) {
                    throw truncated();
                }
                long total = Integer.toUnsignedLong(ByteBuffer.wrap(lengthBytes).getInt());
                // An invalid length is delivered with its prelude so that the decoder reports it precisely.
                long limit = total < 16 || total > maxMessageSize ? 12 : total;
                pending = 4;
                pendingPos = 0;
                remaining = limit - 4;
                inMessage = true;
            }
            int copied = 0;
            if (pendingPos < pending) {
                copied = Math.min(len, pending - pendingPos);
                System.arraycopy(lengthBytes, pendingPos, b, off, copied);
                pendingPos += copied;
            }
            if (copied < len && remaining > 0) {
                int n = source.read(b, off + copied, (int) Math.min(len - copied, remaining));
                if (n < 0) {
                    throw truncated();
                }
                remaining -= n;
                copied += n;
            }
            if (pendingPos == pending && remaining == 0) {
                inMessage = false;
            }
            return copied;
        }

        private IOException truncated() {
            terminated = true;
            return new IOException(EventStreamFormatException.create(
                EventStreamFormatException.Reason.TRUNCATED_MESSAGE, "the stream ended inside a message"));
        }

        @Override
        public void close() throws IOException {
            terminated = true;
            source.close();
        }
    }
}
