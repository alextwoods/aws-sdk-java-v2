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

package software.amazon.awssdk.http.nio.netty;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An event loop group and the channel type it uses (V2 {@code SdkEventLoopGroup}).
 *
 * <p>Ownership: a group from {@link #create(EventLoopGroup)} is the caller's; the client never shuts it down. A group
 * from {@link #builder()} passed with {@code eventLoopGroupBuilder} (or the default) is created and shut down by the
 * client. {@link #close()} starts a graceful shutdown without waiting, so it may be called from any thread,
 * including an event loop; {@link #eventLoopGroup()}'s {@code terminationFuture()} reports termination.
 *
 * <p>Only NIO channels are used: V2 also picks native epoll when the native library is present, which V3 does not
 * ship (only {@code netty-transport-classes-epoll}, as in V2).
 */
public final class SdkEventLoopGroup implements AutoCloseable {
    private final EventLoopGroup eventLoopGroup;
    private final AtomicBoolean closed = new AtomicBoolean();

    private SdkEventLoopGroup(EventLoopGroup eventLoopGroup) {
        this.eventLoopGroup = Objects.requireNonNull(eventLoopGroup, "eventLoopGroup");
    }

    /**
     * Wraps a caller-owned NIO group. The client uses {@link NioSocketChannel}; a group that cannot register NIO
     * channels fails the first exchange.
     */
    public static SdkEventLoopGroup create(EventLoopGroup eventLoopGroup) {
        return new SdkEventLoopGroup(eventLoopGroup);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The group. */
    public EventLoopGroup eventLoopGroup() {
        return eventLoopGroup;
    }

    /** The channel type, always {@link NioSocketChannel}. */
    public Class<NioSocketChannel> channelType() {
        return NioSocketChannel.class;
    }

    /** Starts a graceful shutdown (no quiet period, 5 s bound) without waiting; idempotent. */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            eventLoopGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }
    }

    @Override
    public String toString() {
        return "SdkEventLoopGroup[" + eventLoopGroup.getClass().getSimpleName() + "]";
    }

    /** Builder of an SDK-created group; not thread safe. */
    public static final class Builder {
        private Integer numberOfThreads;
        private ThreadFactory threadFactory;

        private Builder() {
        }

        /** Event loop threads; default is Netty's (2 x available processors). */
        public Builder numberOfThreads(Integer numberOfThreads) {
            if (numberOfThreads != null && numberOfThreads <= 0) {
                throw new IllegalArgumentException("numberOfThreads is not positive: " + numberOfThreads);
            }
            this.numberOfThreads = numberOfThreads;
            return this;
        }

        /** The thread factory; default names daemon threads {@code aws-java-sdk-NettyEventLoop-*}. */
        public Builder threadFactory(ThreadFactory threadFactory) {
            this.threadFactory = threadFactory;
            return this;
        }

        public SdkEventLoopGroup build() {
            ThreadFactory factory = threadFactory != null ? threadFactory
                                                          : new DefaultThreadFactory("aws-java-sdk-NettyEventLoop", true);
            return new SdkEventLoopGroup(new NioEventLoopGroup(numberOfThreads == null ? 0 : numberOfThreads, factory));
        }
    }
}
