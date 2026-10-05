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

package software.amazon.awssdk.http.nio.netty.internal;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslHandler;
import io.netty.resolver.NoopAddressResolverGroup;
import io.netty.util.NetUtil;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.Protocol;

/**
 * Opens connections: DNS resolution off the event loops, TCP connect, TLS handshake with ALPN, then the HTTP/1.1
 * codec or the HTTP/2 frame codec and multiplexer.
 *
 * <p>Event-loop safety: host names are resolved on a small SDK-owned resolver pool (at most
 * {@link #RESOLVER_THREADS} daemon threads), never on an event loop, because Netty's default resolver calls the
 * blocking {@link InetAddress} lookup on the loop. IP literals are parsed without a lookup.
 *
 * <p>Flow control: channels start with {@code autoRead} off, one message per read and at most
 * {@link #MAX_READ_BYTES} per read; HTTP/2 parents read automatically (streams are bounded by their window).
 */
public final class ChannelSetup implements AutoCloseable {
    /** The largest single socket read, in bytes. */
    public static final int MAX_READ_BYTES = 64 * 1024;
    /** Resolver threads. */
    public static final int RESOLVER_THREADS = 4;
    static final String SSL = "ssl";
    static final String HTTP1_CODEC = "http1-codec";
    static final String EXCHANGE = "exchange";

    private final ClientConfig config;
    private final ExecutorService resolver;

    /** The phase the connection being opened is in. */
    @FunctionalInterface
    public interface PhaseListener {
        void phase(HttpPhase phase);
    }

    public ChannelSetup(ClientConfig config) {
        this.config = config;
        ThreadPoolExecutor pool = new ThreadPoolExecutor(RESOLVER_THREADS, RESOLVER_THREADS, 30, TimeUnit.SECONDS,
                                                         new LinkedBlockingQueue<>(),
                                                         new DefaultThreadFactory("aws-java-sdk-NettyResolver", true));
        pool.allowCoreThreadTimeOut(true);
        this.resolver = pool;
    }

    /** Opens a connection to {@code endpoint}; the future completes on an arbitrary thread. */
    public Future<Connection> connect(Endpoint endpoint, PhaseListener listener) {
        Promise<Connection> ready = GlobalEventExecutor.INSTANCE.newPromise();
        listener.phase(HttpPhase.CONNECT);
        byte[] literal = NetUtil.createByteArrayFromIpAddressString(endpoint.host());
        if (literal != null) {
            try {
                connect(endpoint, new InetSocketAddress(InetAddress.getByAddress(endpoint.host(), literal),
                                                        endpoint.port()), listener, ready);
            } catch (Exception e) {
                ready.tryFailure(e);
            }
            return ready;
        }
        try {
            resolver.execute(() -> {
                try {
                    InetAddress address = InetAddress.getByName(endpoint.host());
                    connect(endpoint, new InetSocketAddress(address, endpoint.port()), listener, ready);
                } catch (Throwable t) {
                    ready.tryFailure(t);
                }
            });
        } catch (RejectedExecutionException e) {
            ready.tryFailure(e);
        }
        return ready;
    }

    private void connect(Endpoint endpoint, InetSocketAddress address, PhaseListener listener,
                         Promise<Connection> ready) {
        RecvByteBufAllocator allocator = new AdaptiveRecvByteBufAllocator(512, 8 * 1024, MAX_READ_BYTES)
            .maxMessagesPerRead(1);
        Bootstrap bootstrap = new Bootstrap()
            .group(config.group())
            .channel(NioSocketChannel.class)
            .resolver(NoopAddressResolverGroup.INSTANCE)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) Math.min(Integer.MAX_VALUE,
                                                                         config.connectionTimeout().toMillis()))
            .option(ChannelOption.SO_KEEPALIVE, config.tcpKeepAlive())
            .option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.AUTO_READ, false)
            .option(ChannelOption.RCVBUF_ALLOCATOR, allocator)
            .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(32 * 1024, 64 * 1024))
            .handler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) {
                    if (endpoint.secure()) {
                        SslHandler ssl = config.sslContext().newHandler(ch.alloc(), endpoint.host(), endpoint.port());
                        ssl.setHandshakeTimeoutMillis(config.tlsNegotiationTimeout().toMillis());
                        ch.pipeline().addLast(SSL, ssl);
                    }
                }
            });
        ChannelFuture connected = bootstrap.connect(address);
        connected.addListener(f -> {
            if (!f.isSuccess()) {
                ready.tryFailure(f.cause());
                return;
            }
            Channel ch = connected.channel();
            if (!endpoint.secure()) {
                finish(ch, false, ready);
                return;
            }
            listener.phase(HttpPhase.TLS_HANDSHAKE);
            SslHandler ssl = (SslHandler) ch.pipeline().get(SSL);
            ssl.handshakeFuture().addListener(h -> {
                if (!h.isSuccess()) {
                    ch.close();
                    ready.tryFailure(h.cause());
                    return;
                }
                boolean h2 = config.protocol() == Protocol.HTTP2
                             && ApplicationProtocolNames.HTTP_2.equals(ssl.applicationProtocol());
                finish(ch, h2, ready);
            });
            ch.read();
        });
    }

    private void finish(Channel ch, boolean h2, Promise<Connection> ready) {
        if (h2) {
            Http2Settings settings = Http2Settings.defaultSettings().pushEnabled(false)
                                                  .initialWindowSize(config.http2InitialWindowSize());
            ch.pipeline().addLast(Http2FrameCodecBuilder.forClient().initialSettings(settings).build());
            ch.pipeline().addLast(new Http2MultiplexHandler(new ChannelInboundHandlerAdapter()));
            ch.config().setAutoRead(true);
        } else {
            ch.pipeline().addLast(HTTP1_CODEC, new HttpClientCodec(4096, 16 * 1024, 16 * 1024, false, false));
        }
        Connection connection = new Connection(ch, h2, h2 ? config.http2MaxStreams() : 1);
        if (!ready.trySuccess(connection)) {
            ch.close();
        }
    }

    /** Stops the resolver threads; idempotent. */
    @Override
    public void close() {
        resolver.shutdownNow();
    }
}
