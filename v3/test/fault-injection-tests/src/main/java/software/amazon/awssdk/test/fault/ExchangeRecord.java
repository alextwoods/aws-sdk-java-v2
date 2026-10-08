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

package software.amazon.awssdk.test.fault;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import software.amazon.awssdk.http.CancellationReason;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.SdkHttpRequest;

/**
 * What happened to one exchange of {@link FaultyHttpClient}: the request it was handed, the fault it ran, how much of
 * the request body it read, and how the pipeline released it. Thread-safe; the transport updates it on whatever thread
 * runs the exchange.
 */
public final class ExchangeRecord {
    private final int index;
    private final HttpTransportMode mode;
    private final SdkHttpRequest request;
    private final AtomicLong requestBytesRead = new AtomicLong();
    private final AtomicInteger closes = new AtomicInteger();
    private final AtomicInteger aborts = new AtomicInteger();
    private final AtomicInteger terminalSignals = new AtomicInteger();
    private final AtomicInteger bodyCancels = new AtomicInteger();
    private final List<CancellationReason> cancellations = new CopyOnWriteArrayList<>();
    private volatile TransportFault fault;
    private volatile boolean parked;
    private volatile boolean requestBodyOpen;
    private volatile boolean responseBodyOpen;
    private volatile BooleanSupplier asyncDone = () -> false;

    ExchangeRecord(int index, HttpTransportMode mode, SdkHttpRequest request) {
        this.index = index;
        this.mode = mode;
        this.request = request;
    }

    /** The exchange's position among the client's exchanges, from 0. */
    public int index() {
        return index;
    }

    public HttpTransportMode mode() {
        return mode;
    }

    /** The (signed) request the pipeline handed the transport. */
    public SdkHttpRequest request() {
        return request;
    }

    /** The fault the exchange ran, or {@code null} if it has not started (sync: not executed) or had no script. */
    public TransportFault fault() {
        return fault;
    }

    /** Request body bytes the transport consumed. */
    public long requestBytesRead() {
        return requestBytesRead.get();
    }

    /** Sync: how many times the pipeline closed the exchange; it must be exactly once. */
    public int closes() {
        return closes.get();
    }

    /** Sync: how many times the pipeline aborted the exchange. */
    public int aborts() {
        return aborts.get();
    }

    /**
     * Async: how many terminal signals the transport sent for this exchange (the handler's {@code onError}, or the
     * response body's {@code onComplete} or {@code onError}); at most one.
     */
    public int terminalSignals() {
        return terminalSignals.get();
    }

    /** Async: how many times the response body subscription was cancelled by its subscriber. */
    public int bodyCancels() {
        return bodyCancels.get();
    }

    /** Async: the reasons of every winning {@code cancel} of the exchange; at most one. */
    public List<CancellationReason> cancellations() {
        return List.copyOf(cancellations);
    }

    /** True once the exchange parked on a {@link TransportFault.Hang} or {@link TransportFault.HangBody}. */
    public boolean parked() {
        return parked;
    }

    /**
     * True if the pipeline released the exchange: sync, closed (aborting alone does not return the connection); async,
     * terminated (completed, failed or cancelled). A released exchange holds no open request or response body.
     */
    public boolean released() {
        boolean terminated = mode == HttpTransportMode.SYNC ? closes.get() > 0 : asyncDone.getAsBoolean();
        return terminated && !requestBodyOpen && !responseBodyOpen;
    }

    /** Why the exchange is not cleanly released, or {@code null} if it is. */
    public String leak() {
        StringBuilder problems = new StringBuilder();
        if (mode == HttpTransportMode.SYNC) {
            if (closes.get() == 0) {
                problems.append(" never closed;");
            } else if (closes.get() > 1) {
                problems.append(" closed ").append(closes.get()).append(" times;");
            }
        } else {
            if (!asyncDone.getAsBoolean()) {
                problems.append(" never terminated;");
            }
            if (terminalSignals.get() > 1) {
                problems.append(" signalled ").append(terminalSignals.get()).append(" terminal events;");
            }
            if (cancellations.size() > 1) {
                problems.append(" cancelled ").append(cancellations.size()).append(" times;");
            }
        }
        if (requestBodyOpen) {
            problems.append(" request body still open;");
        }
        if (responseBodyOpen) {
            problems.append(" response body still open;");
        }
        return problems.isEmpty() ? null : "exchange " + index + " (" + (fault == null ? "-" : fault.label()) + "):"
                                           + problems;
    }

    @Override
    public String toString() {
        return "ExchangeRecord[" + index + ", " + mode + ", " + (fault == null ? "-" : fault.label()) + ", read "
               + requestBytesRead + ", closes " + closes + ", aborts " + aborts + ", signals " + terminalSignals
               + ", cancels " + cancellations + ", bodyCancels " + bodyCancels + "]";
    }

    void fault(TransportFault value) {
        this.fault = value;
    }

    void addRequestBytes(long n) {
        requestBytesRead.addAndGet(n);
    }

    void closed() {
        closes.incrementAndGet();
    }

    void aborted() {
        aborts.incrementAndGet();
    }

    void terminalSignal() {
        terminalSignals.incrementAndGet();
    }

    void bodyCancelled() {
        bodyCancels.incrementAndGet();
    }

    void cancelled(CancellationReason reason) {
        cancellations.add(reason);
    }

    void park() {
        parked = true;
    }

    void requestBodyOpen(boolean open) {
        requestBodyOpen = open;
    }

    void responseBodyOpen(boolean open) {
        responseBodyOpen = open;
    }

    void asyncDone(BooleanSupplier done) {
        this.asyncDone = done;
    }
}
