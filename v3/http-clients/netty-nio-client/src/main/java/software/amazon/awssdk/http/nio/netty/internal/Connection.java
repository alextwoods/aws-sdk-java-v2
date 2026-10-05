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

import io.netty.channel.Channel;
import java.util.concurrent.ScheduledFuture;

/**
 * One established connection of an {@link EndpointPool}: an HTTP/1.1 channel with one exchange slot, or an HTTP/2
 * parent channel with {@code capacity} stream slots. Mutable fields are guarded by the owning pool's lock.
 */
public final class Connection {
    private final Channel channel;
    private final boolean http2;
    private final int capacity;
    private final long createdNanos = System.nanoTime();
    int leased;
    long idleSinceNanos = System.nanoTime();
    ScheduledFuture<?> idleTimer;

    Connection(Channel channel, boolean http2, int capacity) {
        this.channel = channel;
        this.http2 = http2;
        this.capacity = capacity;
    }

    public Channel channel() {
        return channel;
    }

    public boolean isHttp2() {
        return http2;
    }

    int capacity() {
        return capacity;
    }

    long createdNanos() {
        return createdNanos;
    }

    @Override
    public String toString() {
        return "Connection[" + (http2 ? "h2" : "http/1.1") + ", " + channel + "]";
    }
}
