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

package software.amazon.awssdk.http.crt.internal;

import java.util.function.Supplier;
import software.amazon.awssdk.crt.http.HttpStream;

/**
 * The native side of one exchange: its lease and stream. Thread safe; every native call on the stream or the lease
 * is made under this object's lock, so they never interleave.
 *
 * <p>Lifecycle, matching V2's CRT client:
 * <ul>
 *   <li>The stream is made and activated under the lock ({@link #start}), so an abandonment racing the send can
 *       neither cancel a stream that is not yet activated nor release the connection while {@code makeRequest} is
 *       using it. An abandonment that happened meanwhile cancels the stream right after activation.</li>
 *   <li>The stream is closed exactly once, only after {@code onResponseComplete} ({@link #completed}); an abandoned
 *       stream is only cancelled, and the CRT then completes it.</li>
 *   <li>The connection is returned exactly once: released for reuse only if the stream completed successfully, the
 *       exchange was not abandoned and the consumer reached the end of the body ({@link #consumed}); otherwise it is
 *       discarded (shut down, then returned so the pool forgets it). An early close or abort therefore never returns
 *       the connection to the pool, even if the CRT had already received the whole response.</li>
 * </ul>
 * Window increments are ignored once the stream is closed or the exchange abandoned.
 */
final class CrtStreamState {
    private CrtClientCore.Lease lease;
    private HttpStream stream;
    private boolean abandoned;
    private boolean streamClosed;
    private boolean succeeded;
    private boolean consumed;
    private boolean leaseReturned;

    /** Records the lease; false (and the unused lease is released) if the exchange was already abandoned. */
    synchronized boolean leased(CrtClientCore.Lease lease) {
        if (abandoned) {
            leaseReturned = true;
            lease.release();
            return false;
        }
        this.lease = lease;
        return true;
    }

    /**
     * Makes and activates the stream on the leased connection, then cancels it if the exchange was abandoned
     * meanwhile. If {@code makeStream} throws, the connection is discarded (once) and the exception rethrown.
     * No-op if the lease was already returned.
     */
    synchronized void start(Supplier<HttpStream> makeStream) {
        if (leaseReturned || lease == null) {
            return;
        }
        HttpStream made;
        try {
            made = makeStream.get();
        } catch (RuntimeException | Error e) {
            returnLease(false);
            throw e;
        }
        stream = made;
        try {
            // V2 ResponseHandlerHelper: activate before any other call on the stream, including cancel() and close().
            made.activate();
        } catch (RuntimeException | Error e) {
            // A stream that never activated never completes: close it and discard the connection here.
            completed(false);
            throw e;
        }
        if (abandoned) {
            made.cancel();
        }
    }

    /** Abandons the exchange: cancels a running stream, or discards a connection that has no running stream. */
    synchronized void abandon() {
        if (abandoned) {
            return;
        }
        abandoned = true;
        if (leaseReturned) {
            return;
        }
        if (stream != null && !streamClosed) {
            // The CRT completes the cancelled stream (onResponseComplete), which closes it and discards the lease.
            stream.cancel();
        } else if (lease != null) {
            // No stream yet (acquired but not sent), or the stream already completed but the body was not consumed.
            returnLease(false);
        }
    }

    /** {@code onResponseComplete}: closes the stream once; discards the connection unless it may still be reused. */
    synchronized void completed(boolean success) {
        if (streamClosed) {
            return;
        }
        streamClosed = true;
        succeeded = success;
        if (stream != null) {
            stream.close();
        }
        if (!success || abandoned) {
            returnLease(false);
        } else if (consumed) {
            returnLease(true);
        }
        // Otherwise the connection is held until the consumer reaches the end of the body or abandons it.
    }

    /** The consumer reached the end of the body (or the response has none): the connection may be reused. */
    synchronized void consumed() {
        consumed = true;
        if (streamClosed && succeeded && !abandoned) {
            returnLease(true);
        }
    }

    private void returnLease(boolean reusable) {
        if (leaseReturned || lease == null) {
            return;
        }
        leaseReturned = true;
        if (reusable) {
            lease.release();
        } else {
            lease.discard();
        }
    }

    /** Opens the read window by {@code bytes}, unless the stream is closed or the exchange abandoned. */
    synchronized void incrementWindow(int bytes) {
        if (!streamClosed && !abandoned && stream != null && bytes > 0) {
            stream.incrementWindow(bytes);
        }
    }

    synchronized boolean isAbandoned() {
        return abandoned;
    }

    /** Test view: whether the connection was returned to its pool (released or discarded). */
    synchronized boolean isLeaseReturned() {
        return leaseReturned;
    }
}
