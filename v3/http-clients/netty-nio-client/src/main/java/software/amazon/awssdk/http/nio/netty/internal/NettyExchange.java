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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpStatusClass;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import io.netty.handler.codec.http2.Http2StreamFrameToHttpObjectCodec;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.EventExecutor;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.AsyncExchangeCompletion;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.CancellationReason;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpHeaders;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportException;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * One exchange: acquisition, request transmission and response consumption.
 *
 * <p>Concurrency: until a connection is attached, the exchange may be touched by the caller, a timer and the pool on
 * different threads; attaching, acquisition failure and cancellation are serialized by {@code lock} together with
 * {@link AsyncExchangeCompletion}'s first-wins state, so exactly one of them proceeds. After attachment every step
 * runs on the channel's event loop, including cancellation (handed to the loop), so handler and subscriber signals
 * are serial. Terminal signals are sent only by the path that won the completion.
 *
 * <p>Flow control: the request body is requested one chunk at a time, only after the previous chunk has been
 * written and while the channel is writable; the response is read only on demand ({@link ResponseBodyPublisher}).
 *
 * <p>Event-loop safety: the only foreign code called on the loop is the {@link AsyncHttpResponseHandler} and the
 * request-body publisher's subscription (whose {@code request} is expected not to block, per
 * {@link software.amazon.awssdk.http.StreamingBody}) and the response-body subscriber.
 */
public final class NettyExchange implements EndpointPool.Waiter, ResponseBodyPublisher.Source {
    private final String clientName;
    private final SdkHttpRequest request;
    private final AsyncHttpResponseHandler handler;
    private final EndpointPool pool;
    private final Duration acquireTimeout;
    private final Duration readTimeout;
    private final Duration writeTimeout;
    private final AsyncExchangeCompletion completion;
    private final Object lock = new Object();

    private Connection connection;
    private volatile ScheduledFuture<?> acquireTimer;

    // Event-loop confined after attachment.
    private Channel channel;
    private boolean tornDown;
    private boolean headersReceived;
    private boolean skippingInformational;
    private boolean requestWritten;
    private boolean keepAlive = true;
    private ResponseBodyPublisher publisher;
    private Subscription bodySubscription;
    private boolean chunkInFlight;
    private boolean bodyEnded;
    private boolean awaitingData;
    private long lastActivityNanos;
    private ScheduledFuture<?> readTimer;

    public NettyExchange(String clientName, SdkHttpRequest request, AsyncHttpResponseHandler handler,
                         EndpointPool pool, Duration acquireTimeout, Duration readTimeout, Duration writeTimeout) {
        this.clientName = clientName;
        this.request = request;
        this.handler = handler;
        this.pool = pool;
        this.acquireTimeout = acquireTimeout;
        this.readTimeout = readTimeout;
        this.writeTimeout = writeTimeout;
        this.completion = new AsyncExchangeCompletion(clientName, this::cancelled);
    }

    /** The caller's handle. */
    public AsyncHttpExchange exchange() {
        return completion;
    }

    /** Starts acquisition; never blocks. */
    public void start(EventLoopGroup group) {
        acquireTimer = group.next().schedule(
            () -> failDetached(NettyFailures.timeout(HttpPhase.CONNECTION_ACQUISITION, "connectionAcquisitionTimeout",
                                                     clientName)),
            acquireTimeout.toNanos(), TimeUnit.NANOSECONDS);
        pool.acquire(this);
    }

    // ---- Acquisition (any thread) ----

    @Override
    public void phase(HttpPhase phase) {
        synchronized (lock) {
            if (connection == null && !completion.isDone()) {
                completion.enter(phase);
            }
        }
    }

    @Override
    public HttpPhase currentPhase() {
        return completion.phase();
    }

    @Override
    public boolean acquired(Connection leased) {
        synchronized (lock) {
            if (completion.isDone()) {
                return false;
            }
            connection = leased;
        }
        cancel(acquireTimer);
        Channel parent = leased.channel();
        if (!leased.isHttp2()) {
            onLoop(parent.eventLoop(), () -> begin(parent));
            return true;
        }
        onLoop(parent.eventLoop(), () -> new Http2StreamChannelBootstrap(parent)
            .handler(new ChannelInitializer<Http2StreamChannel>() {
                @Override
                protected void initChannel(Http2StreamChannel ch) {
                    ch.config().setAutoRead(false);
                    ch.pipeline().addLast(new Http2StreamFrameToHttpObjectCodec(false, false));
                }
            })
            .open()
            .addListener(f -> {
                if (f.isSuccess()) {
                    begin((Channel) f.getNow());
                } else {
                    failAttached(f.cause());
                }
            }));
        return true;
    }

    @Override
    public void failed(HttpTransportException failure) {
        failDetached(failure);
    }

    private void failDetached(HttpTransportException failure) {
        synchronized (lock) {
            if (connection != null || !completion.fail(failure)) {
                return;
            }
        }
        pool.abandon(this);
        cancel(acquireTimer);
        signalError(failure);
    }

    /** The winning {@link AsyncExchangeCompletion#cancel}; runs on the cancelling thread and never blocks. */
    private void cancelled(CancellationReason reason, HttpTransportException failure) {
        Connection attached;
        synchronized (lock) {
            attached = connection;
        }
        if (attached == null) {
            pool.abandon(this);
            cancel(acquireTimer);
            signalError(failure);
        } else {
            onLoop(attached.channel().eventLoop(), () -> teardown(failure));
        }
    }

    // ---- Transmission (event loop) ----

    private void begin(Channel ch) {
        if (tornDown) {
            if (connection.isHttp2()) {
                ch.close();
            }
            return;
        }
        channel = ch;
        if (completion.isDone()) {
            return; // A cancellation's teardown is queued behind this task.
        }
        completion.enter(HttpPhase.REQUEST_TRANSMISSION);
        ch.pipeline().addLast(ChannelSetup.EXCHANGE, new ResponseHandler());
        DefaultHttpRequest head = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.valueOf(request.method()),
                                                         request.rawTarget(), new DefaultHttpHeaders(false));
        HttpHeaders headers = request.headers();
        for (int i = 0; i < headers.size(); i++) {
            head.headers().add(headers.name(i), headers.value(i));
        }
        if (!headers.contains("Host")) {
            head.headers().add(HttpHeaderNames.HOST, request.authority());
        }
        HttpBody body = request.body();
        OptionalLong length = body.contentLength();
        boolean framed = headers.contains("Content-Length") || headers.contains("Transfer-Encoding");
        if (body.kind() != HttpBody.Kind.EMPTY && !framed) {
            if (length.isPresent()) {
                head.headers().add(HttpHeaderNames.CONTENT_LENGTH, length.getAsLong());
            } else if (!connection.isHttp2()) {
                head.headers().add(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
            }
        }
        switch (body.kind()) {
            case EMPTY -> {
                write(head, false, null);
                write(LastHttpContent.EMPTY_LAST_CONTENT, true, this::requestWritten);
            }
            case BYTES -> {
                write(head, false, null);
                write(new DefaultLastHttpContent(Unpooled.wrappedBuffer(body.asByteBuffer())), true,
                      this::requestWritten);
            }
            case STREAMING -> {
                write(head, true, null);
                body.streamingBody().subscribe(new BodyWriter());
            }
        }
        lastActivityNanos = System.nanoTime();
        ch.read(); // Early responses (for example an error before the body is sent) are read too.
    }

    private void write(Object message, boolean flush, Runnable onWritten) {
        ChannelFuture future = flush ? channel.writeAndFlush(message) : channel.write(message);
        ScheduledFuture<?> timer = writeTimeout == null || future.isDone() ? null : channel.eventLoop().schedule(() -> {
            if (!future.isDone()) {
                failAttached(NettyFailures.timeout(HttpPhase.REQUEST_TRANSMISSION, "writeTimeout", clientName));
            }
        }, writeTimeout.toNanos(), TimeUnit.NANOSECONDS);
        future.addListener(done -> {
            cancel(timer);
            if (!done.isSuccess()) {
                failAttached(NettyFailures.map(done.cause(), HttpPhase.REQUEST_TRANSMISSION, clientName));
            } else if (onWritten != null && !tornDown) {
                onWritten.run();
            }
        });
    }

    private void requestWritten() {
        requestWritten = true;
        if (!headersReceived) {
            completion.enter(HttpPhase.RESPONSE_CONSUMPTION);
            awaitingData = true;
            lastActivityNanos = System.nanoTime();
            armReadTimer();
        }
    }

    // ---- Response (event loop) ----

    private void onResponse(HttpResponse response) {
        headersReceived = true;
        completion.enter(HttpPhase.RESPONSE_CONSUMPTION);
        keepAlive = HttpUtil.isKeepAlive(response);
        SdkHttpResponse.Builder out = SdkHttpResponse.builder().statusCode(response.status().code());
        for (Map.Entry<String, String> header : response.headers()) {
            if (!header.getKey().startsWith("x-http2-")) {
                out.addHeader(header.getKey(), header.getValue());
            }
        }
        handler.onHeaders(out.build());
        if (completion.isDone()) {
            return;
        }
        publisher = new ResponseBodyPublisher(channel.eventLoop(), this);
        handler.onStream(publisher);
    }

    @Override
    public void read() {
        if (tornDown) {
            return;
        }
        awaitingData = true;
        lastActivityNanos = System.nanoTime();
        armReadTimer();
        channel.read();
    }

    @Override
    public void cancelled() {
        completion.cancel(CancellationReason.RESPONSE_ABANDONED);
    }

    @Override
    public boolean completing() {
        if (!completion.complete()) {
            return false;
        }
        tornDown = true;
        cancel(readTimer);
        boolean reusable = keepAlive && requestWritten;
        if (!requestWritten && bodySubscription != null) {
            bodySubscription.cancel();
        }
        if (connection.isHttp2()) {
            channel.close();
            pool.release(connection, true);
        } else {
            if (channel.pipeline().get(ChannelSetup.EXCHANGE) != null) {
                channel.pipeline().remove(ChannelSetup.EXCHANGE);
            }
            pool.release(connection, reusable);
        }
        return true;
    }

    // ---- Failure (event loop) ----

    private void failAttached(Throwable cause) {
        HttpTransportException failure = NettyFailures.map(cause, completion.phase(), clientName);
        if (completion.fail(failure)) {
            teardown(failure);
        }
    }

    /** Discards the connection (HTTP/1.1) or resets the stream (HTTP/2) and signals {@code failure} once. */
    private void teardown(HttpTransportException failure) {
        if (tornDown) {
            return;
        }
        tornDown = true;
        cancel(readTimer);
        if (bodySubscription != null) {
            bodySubscription.cancel();
        }
        if (channel == null) {
            pool.release(connection, true); // Nothing was written; the connection is still clean.
        } else if (connection.isHttp2()) {
            channel.close();
            pool.release(connection, true);
        } else {
            pool.release(connection, false);
        }
        if (publisher != null) {
            publisher.fail(failure);
        } else {
            signalError(failure);
        }
    }

    private void signalError(HttpTransportException failure) {
        try {
            handler.onError(failure);
        } catch (RuntimeException ignored) {
            // The handler's own failure cannot be reported anywhere else.
        }
    }

    // ---- Read timeout (event loop) ----

    private void armReadTimer() {
        if (readTimeout == null || readTimer != null || tornDown) {
            return;
        }
        readTimer = channel.eventLoop().schedule(this::checkRead, readTimeout.toNanos(), TimeUnit.NANOSECONDS);
    }

    private void checkRead() {
        readTimer = null;
        if (tornDown || !awaitingData) {
            return;
        }
        long idle = System.nanoTime() - lastActivityNanos;
        if (idle >= readTimeout.toNanos()) {
            failAttached(NettyFailures.timeout(HttpPhase.RESPONSE_CONSUMPTION, "readTimeout", clientName));
        } else {
            readTimer = channel.eventLoop().schedule(this::checkRead, readTimeout.toNanos() - idle,
                                                     TimeUnit.NANOSECONDS);
        }
    }

    private static void onLoop(EventExecutor loop, Runnable task) {
        if (loop.inEventLoop()) {
            task.run();
        } else {
            loop.execute(task);
        }
    }

    private static void cancel(ScheduledFuture<?> timer) {
        if (timer != null) {
            timer.cancel(false);
        }
    }

    /** Receives the decoded response of this exchange. */
    private final class ResponseHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            lastActivityNanos = System.nanoTime();
            try {
                if (tornDown) {
                    return;
                }
                if (msg instanceof HttpResponse response) {
                    if (response.status().codeClass() == HttpStatusClass.INFORMATIONAL
                        && response.status().code() != 101) {
                        skippingInformational = !(msg instanceof LastHttpContent);
                        return;
                    }
                    onResponse(response);
                }
                if (msg instanceof HttpContent content && !tornDown) {
                    if (skippingInformational) {
                        skippingInformational = !(msg instanceof LastHttpContent);
                        return;
                    }
                    if (publisher == null) {
                        return;
                    }
                    ByteBuf data = content.content();
                    awaitingData = false;
                    if (data.isReadable()) {
                        ByteBuffer copy = ByteBuffer.allocate(data.readableBytes());
                        data.getBytes(data.readerIndex(), copy);
                        copy.flip();
                        publisher.offer(copy.asReadOnlyBuffer());
                    }
                    if (content instanceof LastHttpContent && !tornDown) {
                        publisher.complete();
                    }
                }
            } catch (RuntimeException e) {
                failAttached(e);
            } finally {
                ReferenceCountUtil.release(msg);
            }
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            lastActivityNanos = System.nanoTime();
            if (tornDown) {
                return;
            }
            if (publisher != null) {
                publisher.readComplete();
            } else if (!headersReceived) {
                ctx.read();
            }
        }

        @Override
        public void channelWritabilityChanged(ChannelHandlerContext ctx) {
            requestMore();
            ctx.fireChannelWritabilityChanged();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            if (!tornDown) {
                failAttached(NettyFailures.prematureClose(completion.phase(), clientName));
            }
            ctx.fireChannelInactive();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            failAttached(cause);
        }
    }

    private void requestMore() {
        if (bodySubscription != null && !chunkInFlight && !bodyEnded && !tornDown && channel.isWritable()) {
            chunkInFlight = true;
            bodySubscription.request(1);
        }
    }

    /** Streams the request body: one chunk in flight, requested only while the channel is writable. */
    private final class BodyWriter implements Subscriber<ByteBuffer> {
        @Override
        public void onSubscribe(Subscription subscription) {
            onLoop(channel.eventLoop(), () -> {
                if (tornDown || completion.isDone() || bodySubscription != null) {
                    subscription.cancel();
                    return;
                }
                bodySubscription = subscription;
                requestMore();
            });
        }

        @Override
        public void onNext(ByteBuffer chunk) {
            onLoop(channel.eventLoop(), () -> {
                if (tornDown) {
                    return;
                }
                write(new DefaultHttpContent(Unpooled.wrappedBuffer(chunk)), true, () -> {
                    chunkInFlight = false;
                    requestMore();
                });
            });
        }

        @Override
        public void onError(Throwable error) {
            onLoop(channel.eventLoop(), () -> {
                bodyEnded = true;
                failAttached(NettyFailures.map(error, HttpPhase.REQUEST_TRANSMISSION, clientName));
            });
        }

        @Override
        public void onComplete() {
            onLoop(channel.eventLoop(), () -> {
                bodyEnded = true;
                if (!tornDown) {
                    write(LastHttpContent.EMPTY_LAST_CONTENT, true, NettyExchange.this::requestWritten);
                }
            });
        }
    }
}
