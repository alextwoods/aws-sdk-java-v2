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
package software.amazon.awssdk.test.boundedmemory;
import java.util.concurrent.atomic.AtomicLong;
/**
 * The bytes in flight between the producer and the consumer of a transfer: every byte produced but not yet consumed
 * is held somewhere in between (SDK buffers, transport buffers, socket buffers). The peak is an upper bound on what
 * the SDK and the transport buffer, and is sampled at every production and consumption. Demand is recorded the same
 * way for Reactive Streams legs.
 */
final class InFlightGauge {
    private final AtomicLong produced = new AtomicLong();
    private final AtomicLong consumed = new AtomicLong();
    private final AtomicLong peak = new AtomicLong();
    private final AtomicLong outstandingDemand = new AtomicLong();
    private final AtomicLong peakDemand = new AtomicLong();
    private int passes;
    /**
     * A new read pass of a replayable body begins (over plain HTTP the signer reads the body once for its SHA-256
     * before transmitting it, as in V2): production restarts from the bytes the consumer has seen, and the in-flight
     * peak is measured for the new pass. Returns the number of passes so far.
     */
    synchronized int newPass() {
        produced.set(consumed.get());
        peak.set(0);
        return ++passes;
    }
    synchronized int passes() {
        return passes;
    }
    void produced(long n) {
        long now = produced.addAndGet(n) - consumed.get();
        peak.accumulateAndGet(now, Math::max);
    }
    void consumed(long n) {
        consumed.addAndGet(n);
    }
    /** {@code n} more elements were requested by the consumer. */
    void requested(long n) {
        long now = outstandingDemand.addAndGet(n);
        peakDemand.accumulateAndGet(now, Math::max);
    }
    /** One requested element was delivered. */
    void delivered() {
        outstandingDemand.decrementAndGet();
    }
    long produced() {
        return produced.get();
    }
    long consumed() {
        return consumed.get();
    }
    long peakInFlight() {
        return peak.get();
    }
    long peakDemand() {
        return peakDemand.get();
    }
    @Override
    public String toString() {
        return String.format("passes=%d produced=%d consumed=%d peakInFlight=%.2f MiB peakDemand=%d", passes(),
                             produced(), consumed(), peakInFlight() / (double) Payload.MIB, peakDemand());
    }
}
