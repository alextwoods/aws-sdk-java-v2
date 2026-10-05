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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2FrameStream;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.util.ReferenceCountUtil;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.StreamingBodies;

/** HTTP/2 over TLS (ALPN, JDK provider) against a Netty h2 test server. */
class Http2Test {
    private NioEventLoopGroup serverGroup;
    private Channel serverChannel;
    private NettyNioAsyncHttpClient client;

    @BeforeEach
    void start() throws Exception {
        SslContext serverSsl = SslContextBuilder.forServer(TestSupport.keyManagers())
            .sslProvider(io.netty.handler.ssl.SslProvider.JDK)
            .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
            .applicationProtocolConfig(new ApplicationProtocolConfig(
                ApplicationProtocolConfig.Protocol.ALPN, ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT, ApplicationProtocolNames.HTTP_2))
            .build();
        serverGroup = new NioEventLoopGroup(1);
        serverChannel = new ServerBootstrap().group(serverGroup).channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast(serverSsl.newHandler(ch.alloc()));
                    ch.pipeline().addLast(Http2FrameCodecBuilder.forServer().build());
                    ch.pipeline().addLast(new EchoHandler());
                }
            }).bind(new InetSocketAddress("127.0.0.1", 0)).sync().channel();
        javax.net.ssl.TrustManager[] trust = TestSupport.trustManagers().getTrustManagers();
        client = NettyNioAsyncHttpClient.builder().protocol(Protocol.HTTP2).tlsTrustManagersProvider(() -> trust)
                                        .build();
    }

    @AfterEach
    void stop() {
        client.close();
        serverChannel.close();
        serverGroup.shutdownGracefully(0, 1, TimeUnit.SECONDS);
    }

    private int port() {
        return ((InetSocketAddress) serverChannel.localAddress()).getPort();
    }

    @Test
    void concurrentExchangesShareOneConnectionAndKeepTheRawTarget() throws Exception {
        List<TestSupport.Recorder> recorders = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            SdkHttpRequest request = SdkHttpRequest.builder().method("GET").scheme("https").host("localhost")
                                                   .port(port()).encodedPath("/p%2F" + i)
                                                   .addRawQueryParameter("q", "a%20b")
                                                   .addHeader("X-Rep", "1").addHeader("X-Rep", "2").build();
            TestSupport.Recorder recorder = new TestSupport.Recorder();
            client.execute(request, recorder, HttpExecutionOptions.defaults());
            recorders.add(recorder);
        }
        for (int i = 0; i < recorders.size(); i++) {
            TestSupport.Recorder recorder = recorders.get(i);
            SdkHttpResponse response = recorder.headers.get(10, TimeUnit.SECONDS);
            assertEquals(200, response.statusCode());
            assertEquals("/p%2F" + i + "?q=a%20b", response.headers().firstValue("x-path").orElseThrow());
            assertEquals("1,2", response.headers().firstValue("x-rep").orElseThrow());
            assertEquals("h2 0", new String(recorder.body.get(10, TimeUnit.SECONDS), StandardCharsets.UTF_8));
            assertTrue(recorder.threads.stream().allMatch(t -> t.startsWith("aws-java-sdk-NettyEventLoop")));
        }
        assertEquals(1, client.openConnections(true, "localhost", port()));
    }

    @Test
    void streamingRequestBodyOverHttp2() throws Exception {
        byte[] payload = new byte[300 * 1024];
        SdkHttpRequest request = SdkHttpRequest.builder().method("POST").scheme("https").host("localhost")
                                               .port(port()).encodedPath("/upload")
                                               .body(HttpBody.streaming(StreamingBodies.fromBytes(payload, null)))
                                               .build();
        TestSupport.Recorder recorder = new TestSupport.Recorder();
        client.execute(request, recorder, HttpExecutionOptions.defaults());
        assertEquals("h2 " + payload.length, new String(recorder.body.get(10, TimeUnit.SECONDS),
                                                         StandardCharsets.UTF_8));
    }

    @Test
    void cancellingAStreamResetsOnlyThatStreamWithinTheDeadline() throws Exception {
        SdkHttpRequest stall = SdkHttpRequest.builder().method("GET").scheme("https").host("localhost").port(port())
                                             .encodedPath("/stall").build();
        TestSupport.Recorder stalled = new TestSupport.Recorder();
        software.amazon.awssdk.http.AsyncHttpExchange exchange =
            client.execute(stall, stalled, HttpExecutionOptions.defaults());
        stalled.stream.get(10, TimeUnit.SECONDS);
        long start = System.nanoTime();
        assertTrue(exchange.cancel(software.amazon.awssdk.http.CancellationReason.CALLER_CANCELLED));
        Throwable error = stalled.error.get(NettyNioAsyncHttpClient.CANCELLATION_TIMEOUT.toMillis(),
                                            TimeUnit.MILLISECONDS);
        assertTrue(System.nanoTime() - start < NettyNioAsyncHttpClient.CANCELLATION_TIMEOUT.toNanos());
        assertEquals(software.amazon.awssdk.http.HttpFailureCategory.CANCELLED,
                     ((software.amazon.awssdk.http.HttpTransportException) error).category());

        TestSupport.Recorder next = new TestSupport.Recorder();
        client.execute(SdkHttpRequest.builder().method("GET").scheme("https").host("localhost").port(port())
                                     .encodedPath("/after").build(), next, HttpExecutionOptions.defaults());
        assertEquals("h2 0", new String(next.body.get(10, TimeUnit.SECONDS), StandardCharsets.UTF_8));
        assertEquals(1, client.openConnections(true, "localhost", port()), "the connection survives a stream reset");
    }

    /** Answers each stream with its :path, its x-rep values and the request body length. */
    private static final class EchoHandler extends ChannelInboundHandlerAdapter {
        private final Map<Integer, Http2Headers> heads = new HashMap<>();
        private final Map<Integer, Integer> lengths = new HashMap<>();

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            try {
                if (msg instanceof Http2HeadersFrame headers) {
                    heads.put(headers.stream().id(), headers.headers());
                    lengths.put(headers.stream().id(), 0);
                    if (headers.isEndStream()) {
                        respond(ctx, headers.stream());
                    }
                } else if (msg instanceof Http2DataFrame data) {
                    lengths.merge(data.stream().id(), data.content().readableBytes(), Integer::sum);
                    // Return the flow-control credit (the frame codec does not do it automatically).
                    ctx.write(new io.netty.handler.codec.http2.DefaultHttp2WindowUpdateFrame(
                        data.initialFlowControlledBytes()).stream(data.stream()));
                    if (data.isEndStream()) {
                        respond(ctx, data.stream());
                    } else {
                        ctx.flush();
                    }
                }
            } finally {
                ReferenceCountUtil.release(msg);
            }
        }

        private void respond(ChannelHandlerContext ctx, Http2FrameStream stream) {
            Http2Headers request = heads.remove(stream.id());
            if (request.path().toString().startsWith("/stall")) {
                ctx.writeAndFlush(new DefaultHttp2HeadersFrame(new DefaultHttp2Headers().status("200")).stream(stream));
                return;
            }
            List<CharSequence> reps = request.getAll("x-rep");
            Http2Headers response = new DefaultHttp2Headers().status("200").add("x-path", request.path())
                                                             .add("x-rep", String.join(",", reps));
            ctx.write(new DefaultHttp2HeadersFrame(response).stream(stream));
            byte[] body = ("h2 " + lengths.remove(stream.id())).getBytes(StandardCharsets.UTF_8);
            ctx.writeAndFlush(new DefaultHttp2DataFrame(Unpooled.wrappedBuffer(body), true).stream(stream));
        }
    }
}
