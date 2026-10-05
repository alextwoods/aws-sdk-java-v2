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

package software.amazon.awssdk.protocols.core.flow;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.LongSupplier;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;
import net.jqwik.api.statistics.Statistics;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.eventstream.HeaderValue;
import software.amazon.awssdk.eventstream.Message;
import software.amazon.awssdk.eventstream.MessageEncoder;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.http.StreamingBody;
import software.amazon.awssdk.protocols.core.eventstream.EventPayloadCodec;
import software.amazon.awssdk.protocols.core.eventstream.EventStreamCodec;
import software.amazon.awssdk.protocols.core.eventstream.EventStreams;
import software.amazon.awssdk.protocols.core.serde.WireContext;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.dynamicschemas.SchemaConverter;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Property 14 (Requirements 5.11, 8.4, 8.5, 8.6, 12.5): for any stream content, any trace of downstream demand
 * changes, upstream deliveries and a cancellation point, an async body or Event Stream publisher delivers no more
 * elements than the cumulative positive demand, requests no later upstream element while demand is zero, preserves
 * element order, and keeps buffered data within its documented bound.
 *
 * <p>Publishers under test (every async streaming path that exists today):
 * <ul>
 *   <li>{@link StreamingBodies#fromInputStream(InputStream, long, String, int)}: the demand-driven publisher over a
 *       blocking source, used for streaming blob request bodies (Requirement 8.4) and, through
 *       {@code EventStreamRequestBody}, for encoded input event frames. Documented bound: it reads at most one chunk
 *       of at most {@code maxChunkSize} bytes per unit of demand and buffers nothing beyond the chunk being
 *       delivered.</li>
 *   <li>{@link StreamingBodies#boundedChunks}: the re-chunking adapter behind {@code StreamingBodies.of} and
 *       {@code StreamingPayloads.toBody} (streaming blob payloads over a Smithy Java {@code DataStream}, Requirements
 *       5.11, 8.5 and 12.5). Documented bound: one upstream buffer requested at a time, only under positive demand,
 *       and at most one upstream buffer held.</li>
 *   <li>{@link EventStreams#publisher}: the typed output Event Stream publisher (Requirements 5.11 and 8.5).
 *       Documented bound: at most one body chunk plus one partial message buffered and no decoded event queued; the
 *       property checks the tighter consequence that when a new body chunk arrives, only a partial message (fewer
 *       bytes than the largest frame) is held.</li>
 * </ul>
 *
 * <p>Each try generates the content (bytes split into upstream buffers, some empty, or events with payloads split
 * into body chunks at arbitrary byte positions), the chunk bound, whether the upstream delivers synchronously inside
 * {@code request} or only on explicit pump steps, whether it ends with completion or an error, an action taken inside
 * {@code onSubscribe}, and a trace of steps: {@code request(n)} (including {@code Long.MAX_VALUE}), upstream pumps,
 * cancellation and non-positive (invalid, rule 3.9) requests. Unless the trace cancels or terminates the stream, the
 * try finishes by requesting unbounded demand and pumping the upstream to its end.
 *
 * <p>Oracles:
 * <ul>
 *   <li>A reference demand ledger: cumulative positive {@code request(n)} minus deliveries. Every {@code onNext} must
 *       find positive outstanding demand; no signal may follow cancellation or a terminal signal (the implementations
 *       are synchronous and document that cancellation stops delivery immediately, so the Reactive Streams
 *       allowance for a bounded number of signals after {@code cancel} is not used); at most one terminal signal; an
 *       invalid request yields
 *       exactly one {@link IllegalArgumentException}.</li>
 *   <li>Upstream demand: every upstream request (or blocking source read) happens while the downstream ledger has
 *       positive outstanding demand and before cancellation, with at most one upstream element outstanding.</li>
 *   <li>A peak-buffer counter: bytes received from the upstream minus bytes the downstream received (or, for an
 *       Event Stream, minus the frame bytes of delivered events), sampled at each upstream delivery, never exceeds
 *       the documented bound.</li>
 *   <li>Order and content: the delivered bytes (events) are a prefix of the generated content, every chunk has
 *       1..{@code maxChunkSize} bytes, and a completed stream delivered all of it; a cancelled or failed stream
 *       cancels (closes) its upstream.</li>
 * </ul>
 *
 * <p>Deferred coverage: paginator page traces (Requirement 8.6's "request no page after demand reaches zero" for
 * paginated operations) need the async paginator publishers, which do not exist yet; they are generated by task 10.2
 * ("Generate sync/async paginators and waiters"), whose property test 10.6 (Property 19) generates page/token graphs
 * with demand traces. This test covers the demand half of Requirement 8.6 for the stream publishers above only.
 *
 * <p>Every try runs on a worker thread with a bounded wait, so a publisher that loops or stalls fails the try
 * instead of hanging the build.
 */
class DemandAndBufferingBoundsPropertyTest {
    private static final long TRY_TIMEOUT_SECONDS = 10;
    /**
     * Pumps allowed, after the trace, for an asynchronous upstream to deliver its remaining buffers and end. Each pump
     * delivers one buffer or the terminal signal, so a try needs at most (buffers + 1) pumps. The largest event
     * stream is 8 frames of at most 151 bytes (111 bytes of framing and headers plus a 40-byte payload), and split
     * sizes of 1 byte with up to 7 empty buffers per byte cut it into at most 8 * 1,208 buffers; the byte-body
     * properties cut at most 300 bytes the same way. A fixed budget of 1,000 was exceeded by an 8-event stream of
     * 1,000 bytes split into 1-byte buffers.
     */
    private static final int MAX_FINAL_PUMPS = 100_000;
    private static final ExecutorService WORKERS = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "p14-try");
        t.setDaemon(true);
        return t;
    });

    private static final String EVENTS_IDL = String.join("\n",
        "$version: \"2\"",
        "namespace test.flow",
        "@streaming",
        "union Events { data: DataEvent }",
        "structure DataEvent {",
        "    @eventHeader",
        "    seq: Long",
        "    @eventPayload",
        "    payload: Blob",
        "}");
    private static final EventStreamCodec CODEC = eventCodec();

    // ---------------------------------------------------------------------------------------------------------------
    // Properties
    // ---------------------------------------------------------------------------------------------------------------

    @Property(tries = 1_000)
    void inputStreamBodiesReadOnlyUnderDemandAndBufferAtMostTheDeliveredChunk(
        @ForAll("contents") byte[] content,
        @ForAll @Size(min = 1, max = 8) List<@IntRange(min = 1, max = 80) Integer> readSizes,
        @ForAll @IntRange(min = 1, max = 64) int maxChunkSize,
        @ForAll boolean knownLength,
        @ForAll("onSubscribe") Step onSubscribe,
        @ForAll("traces") List<Step> trace) {
        collect(bounded(() -> {
            Ledger ledger = new Ledger();
            TrackedStream source = new TrackedStream(content, readSizes, ledger);
            ByteProbe probe = new ByteProbe(ledger, onSubscribe);
            source.delivered = () -> probe.deliveredBytes;
            StreamingBodies.fromInputStream(source, knownLength ? content.length : -1, null, maxChunkSize)
                           .subscribe(probe);
            // A blocking source has no asynchronous deliveries: pumps are no-ops.
            run(probe, trace, () -> { });
            ledger.checkClean();
            assertEquals(1, probe.subscribes, "exactly one onSubscribe");
            assertTrue(source.peakHeld <= 0, "buffered source bytes beyond the delivered chunk: " + source.peakHeld);
            probe.checkChunks(content, maxChunkSize);
            if (ledger.completes == 1) {
                assertArrayEquals(content, probe.bytes(), "a completed body delivers the whole content in order");
            }
            ledger.checkInvalidRequest();
            if (source.opened && (ledger.cancelled || ledger.terminated())) {
                assertTrue(source.closed, "a cancelled or terminated body closes its source");
            }
            return ledger;
        }));
    }

    @Property(tries = 1_000)
    void boundedChunksRequestOneBufferUnderDemandAndHoldAtMostOne(
        @ForAll("contents") byte[] content,
        @ForAll @Size(min = 1, max = 8) List<@IntRange(min = 0, max = 80) Integer> splits,
        @ForAll @IntRange(min = 1, max = 64) int maxChunkSize,
        @ForAll boolean knownLength,
        @ForAll boolean synchronous,
        @ForAll boolean failUpstream,
        @ForAll("onSubscribe") Step onSubscribe,
        @ForAll("traces") List<Step> trace) {
        collect(bounded(() -> {
            List<byte[]> buffers = split(content, splits);
            Ledger ledger = new Ledger();
            ByteProbe probe = new ByteProbe(ledger, onSubscribe);
            IOException failure = failUpstream ? new IOException("upstream failure") : null;
            // Documented bound: the adapter holds at most one upstream buffer, so a new buffer is requested (and
            // delivered) only once the previous one has been fully delivered downstream.
            ScriptedUpstream upstream = new ScriptedUpstream(buffers, synchronous, failure, ledger,
                                                             () -> probe.deliveredBytes, 0);
            StreamingBodies.boundedChunks(upstream, maxChunkSize, knownLength ? content.length : -1)
                           .subscribe(probe);
            run(probe, trace, upstream::pump);
            ledger.checkClean();
            assertEquals(1, probe.subscribes, "exactly one onSubscribe");
            assertEquals(1, upstream.subscriptions, "exactly one upstream subscription");
            assertTrue(upstream.maxOutstanding <= 1, "upstream requests outstanding: " + upstream.maxOutstanding);
            int largest = buffers.stream().mapToInt(b -> b.length).max().orElse(0);
            assertTrue(upstream.peakHeld <= largest,
                       "held " + upstream.peakHeld + " bytes, more than one upstream buffer (" + largest + ")");
            probe.checkChunks(content, maxChunkSize);
            if (ledger.completes == 1) {
                assertArrayEquals(content, probe.bytes(), "a completed body delivers the whole content in order");
            }
            if (ledger.errors == 1 && ledger.expectedInvalid == null && failure != null) {
                assertSame(failure, ledger.error, "the upstream failure is propagated");
                assertArrayEquals(content, probe.bytes(), "buffered data is delivered before the upstream failure");
            }
            ledger.checkInvalidRequest();
            upstream.checkCancelledIfStopped();
            return ledger;
        }));
    }

    @Property(tries = 1_000)
    void eventStreamsDecodeOnlyUnderDemandAndHoldOneChunkAndOnePartialMessage(
        @ForAll @Size(max = 8) List<@Size(max = 40) byte[]> payloads,
        @ForAll @Size(min = 1, max = 8) List<@IntRange(min = 0, max = 90) Integer> splits,
        @ForAll @IntRange(min = 0, max = 64) int messageSizeSlack,
        @ForAll boolean synchronous,
        @ForAll boolean failUpstream,
        @ForAll("onSubscribe") Step onSubscribe,
        @ForAll("traces") List<Step> trace) {
        collect(bounded(() -> {
            List<byte[]> frames = new ArrayList<>();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            for (int i = 0; i < payloads.size(); i++) {
                byte[] frame = frame(i, payloads.get(i));
                frames.add(frame);
                stream.writeBytes(frame);
            }
            List<byte[]> chunks = split(stream.toByteArray(), splits);
            int largestFrame = frames.stream().mapToInt(f -> f.length).max().orElse(0);
            int largestChunk = chunks.stream().mapToInt(c -> c.length).max().orElse(0);
            int maxMessageSize = Math.max(largestFrame, MessageEncoder.MIN_MESSAGE_LENGTH) + messageSizeSlack;

            Ledger ledger = new Ledger();
            EventProbe probe = new EventProbe(ledger, onSubscribe, frames);
            IOException failure = failUpstream ? new IOException("body failure") : null;
            // Documented bound: one body chunk and one partial message; a new chunk is requested only once the
            // current one is consumed, so on arrival at most a partial message (< the largest frame) is held.
            ScriptedUpstream body = new ScriptedUpstream(chunks, synchronous, failure, ledger,
                                                         () -> probe.consumedFrameBytes,
                                                         Math.max(largestFrame - 1, 0));
            EventStreams.publisher(new ScriptedBody(body), CODEC, null, maxMessageSize).subscribe(probe);
            run(probe, trace, body::pump);
            ledger.checkClean();
            assertEquals(1, probe.subscribes, "exactly one onSubscribe");
            assertTrue(body.subscriptions <= 1, "at most one body subscription");
            assertTrue(body.maxOutstanding <= 1, "body requests outstanding: " + body.maxOutstanding);
            long documented = (long) largestChunk + maxMessageSize;
            assertTrue(body.peakHeld <= documented,
                       "held " + body.peakHeld + " bytes, more than one chunk and one message (" + documented + ")");
            probe.checkOrder(payloads);
            if (ledger.completes == 1) {
                assertEquals(payloads.size(), probe.events.size(), "a completed stream delivers every event");
            }
            if (ledger.errors == 1 && ledger.expectedInvalid == null && failure != null) {
                assertSame(failure, ledger.error, "the body failure is propagated");
            }
            ledger.checkInvalidRequest();
            if (body.subscriptions == 1) {
                body.checkCancelledIfStopped();
            }
            return ledger;
        }));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Generators
    // ---------------------------------------------------------------------------------------------------------------

    @Provide
    Arbitrary<byte[]> contents() {
        return Arbitraries.bytes().array(byte[].class).ofMaxSize(300);
    }

    /** The action taken inside {@code onSubscribe}: nothing (a pump step), a request or a cancellation. */
    @Provide
    Arbitrary<Step> onSubscribe() {
        return Arbitraries.frequencyOf(
            Tuple.of(6, Arbitraries.just(new Step(Kind.PUMP, 0))),
            Tuple.of(2, Arbitraries.longs().between(1, 4).map(n -> new Step(Kind.REQUEST, n))),
            Tuple.of(1, Arbitraries.just(new Step(Kind.CANCEL, 0))));
    }

    @Provide
    Arbitrary<List<Step>> traces() {
        Arbitrary<Step> step = Arbitraries.frequencyOf(
            Tuple.of(6, Arbitraries.longs().between(1, 3).map(n -> new Step(Kind.REQUEST, n))),
            Tuple.of(2, Arbitraries.longs().between(4, 16).map(n -> new Step(Kind.REQUEST, n))),
            Tuple.of(1, Arbitraries.just(new Step(Kind.REQUEST, Long.MAX_VALUE))),
            Tuple.of(7, Arbitraries.just(new Step(Kind.PUMP, 0))),
            Tuple.of(1, Arbitraries.just(new Step(Kind.CANCEL, 0))),
            Tuple.of(1, Arbitraries.of(0L, -1L, Long.MIN_VALUE).map(n -> new Step(Kind.REQUEST, n))));
        return step.list().ofMaxSize(30);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Trace execution
    // ---------------------------------------------------------------------------------------------------------------

    enum Kind { REQUEST, PUMP, CANCEL }

    /** One trace step; a {@code REQUEST} with non-positive {@code n} is an invalid request. */
    record Step(Kind kind, long n) {
    }

    private static void run(Probe<?> probe, List<Step> trace, Runnable pump) {
        for (Step step : trace) {
            if (step.kind() == Kind.PUMP) {
                pump.run();
            } else {
                probe.apply(step);
            }
            Ledger ledger = probe.ledger;
            ledger.checkQuiescent();
            if (!ledger.cancelled && !ledger.terminated() && ledger.delivered > 0 && ledger.outstanding() == 0) {
                ledger.stalls++;
            }
        }
        Ledger ledger = probe.ledger;
        if (!ledger.cancelled && !ledger.terminated()) {
            probe.apply(new Step(Kind.REQUEST, Long.MAX_VALUE));
            for (int i = 0; i < MAX_FINAL_PUMPS && !ledger.terminated(); i++) {
                pump.run();
            }
            assertTrue(ledger.terminated(), "unbounded demand and an exhausted upstream end the stream");
        }
    }

    /**
     * Records the try's outcome and checks that the generators reach every interesting region (per 1,000 tries the
     * observed rates are well above these minimums), so the oracles are not satisfied vacuously.
     */
    private static void collect(Ledger ledger) {
        String outcome = ledger.expectedInvalid != null ? "invalid request"
                         : ledger.cancelled ? "cancelled"
                         : ledger.completes == 1 ? "completed"
                         : ledger.errors == 1 ? "failed" : "open";
        Statistics.label("outcome").collect(outcome);
        Statistics.label("outcome").coverage(c -> {
            c.check("completed").percentage(p -> p >= 20);
            c.check("cancelled").percentage(p -> p >= 5);
            c.check("invalid request").percentage(p -> p >= 3);
            c.check("open").count(n -> n == 0);
        });
        Statistics.label("demand exhausted after a delivery, stream still open").collect(ledger.stalls > 0);
        Statistics.label("demand exhausted after a delivery, stream still open")
                  .coverage(c -> c.check(true).percentage(p -> p >= 5));
    }

    /** Runs one try on a worker thread with a bounded wait and returns its ledger. */
    private static Ledger bounded(Callable<Ledger> body) {
        Future<Ledger> result = WORKERS.submit(body);
        try {
            return result.get(TRY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            result.cancel(true);
            throw new AssertionError("the try did not finish within " + TRY_TIMEOUT_SECONDS
                                     + " seconds (a stalled or looping publisher)");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Error error) {
                throw error;
            }
            throw new AssertionError("the try threw", cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Oracles
    // ---------------------------------------------------------------------------------------------------------------

    /** The reference demand ledger of one downstream subscriber. */
    static final class Ledger {
        private final List<String> violations = new ArrayList<>();
        long requested;
        long delivered;
        /** Trace steps after which demand was exhausted with the stream still open (for coverage statistics). */
        int stalls;
        boolean cancelled;
        int completes;
        int errors;
        Throwable error;
        /** Set when an invalid request is made on a live subscription: the error it must produce. */
        String expectedInvalid;

        long outstanding() {
            return requested - delivered;
        }

        boolean terminated() {
            return completes + errors > 0;
        }

        void violation(String what) {
            violations.add(what + " (requested " + requested + ", delivered " + delivered + ", cancelled "
                           + cancelled + ", terminals " + (completes + errors) + ")");
        }

        void request(long n) {
            if (n <= 0) {
                if (!cancelled && !terminated() && expectedInvalid == null) {
                    expectedInvalid = "request(" + n + ")";
                }
                return;
            }
            long sum = requested + n;
            requested = sum < 0 ? Long.MAX_VALUE : sum;
        }

        void onNext() {
            if (cancelled) {
                violation("onNext after cancel");
            }
            if (terminated()) {
                violation("onNext after a terminal signal");
            }
            if (delivered >= requested) {
                violation("onNext beyond cumulative demand");
            }
            delivered++;
        }

        void onTerminal(Throwable t) {
            if (cancelled) {
                violation("terminal signal after cancel: " + t);
            }
            if (terminated()) {
                violation("second terminal signal: " + t);
            }
            if (t == null) {
                completes++;
            } else {
                errors++;
                error = t;
            }
        }

        /** After every step: a pending invalid request has already failed the (synchronous) stream. */
        void checkQuiescent() {
            if (expectedInvalid != null && !terminated()) {
                violation(expectedInvalid + " did not fail the subscription");
            }
        }

        void checkClean() {
            if (!violations.isEmpty()) {
                fail("demand ledger violations: " + violations);
            }
            assertTrue(completes + errors <= 1, "at most one terminal signal");
        }

        void checkInvalidRequest() {
            if (expectedInvalid != null) {
                assertEquals(1, errors, expectedInvalid + " fails the subscription once");
                assertInstanceOf(IllegalArgumentException.class, error, expectedInvalid + " (rule 3.9)");
            }
        }
    }

    /** A downstream subscriber that applies trace steps and records signals in its ledger. */
    abstract static class Probe<T> implements Subscriber<T> {
        final Ledger ledger;
        private final Step onSubscribeStep;
        Subscription subscription;
        int subscribes;

        Probe(Ledger ledger, Step onSubscribeStep) {
            this.ledger = ledger;
            this.onSubscribeStep = onSubscribeStep;
        }

        void apply(Step step) {
            if (subscription == null) {
                return;
            }
            switch (step.kind()) {
                case REQUEST -> {
                    // Record demand before signalling: a synchronous publisher delivers inside request().
                    ledger.request(step.n());
                    subscription.request(step.n());
                }
                case CANCEL -> {
                    if (!ledger.terminated()) {
                        ledger.cancelled = true;
                    }
                    subscription.cancel();
                }
                case PUMP -> {
                }
                default -> throw new IllegalStateException();
            }
        }

        @Override
        public void onSubscribe(Subscription s) {
            subscribes++;
            subscription = s;
            apply(onSubscribeStep);
        }

        @Override
        public void onNext(T item) {
            ledger.onNext();
            record(item);
        }

        abstract void record(T item);

        @Override
        public void onError(Throwable throwable) {
            ledger.onTerminal(throwable);
        }

        @Override
        public void onComplete() {
            ledger.onTerminal(null);
        }
    }

    /** Records delivered byte chunks. */
    static final class ByteProbe extends Probe<ByteBuffer> {
        private final List<byte[]> chunks = new ArrayList<>();
        long deliveredBytes;

        ByteProbe(Ledger ledger, Step onSubscribe) {
            super(ledger, onSubscribe);
        }

        @Override
        void record(ByteBuffer item) {
            byte[] bytes = new byte[item.remaining()];
            item.get(bytes);
            chunks.add(bytes);
            deliveredBytes += bytes.length;
        }

        byte[] bytes() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            chunks.forEach(out::writeBytes);
            return out.toByteArray();
        }

        void checkChunks(byte[] content, int maxChunkSize) {
            for (byte[] chunk : chunks) {
                assertTrue(chunk.length >= 1 && chunk.length <= maxChunkSize,
                           "chunk of " + chunk.length + " bytes, bound " + maxChunkSize);
            }
            byte[] delivered = bytes();
            assertTrue(delivered.length <= content.length, "delivered more bytes than the content");
            assertArrayEquals(Arrays.copyOf(content, delivered.length), delivered, "delivered bytes are in order");
        }
    }

    /** Records delivered events and the frame bytes they consumed. */
    static final class EventProbe extends Probe<SerializableStruct> {
        private final List<byte[]> frames;
        final List<SerializableStruct> events = new ArrayList<>();
        long consumedFrameBytes;

        EventProbe(Ledger ledger, Step onSubscribe, List<byte[]> frames) {
            super(ledger, onSubscribe);
            this.frames = frames;
        }

        @Override
        void record(SerializableStruct item) {
            if (events.size() < frames.size()) {
                consumedFrameBytes += frames.get(events.size()).length;
            }
            events.add(item);
        }

        void checkOrder(List<byte[]> payloads) {
            assertTrue(events.size() <= payloads.size(), "delivered more events than were sent");
            for (int i = 0; i < events.size(); i++) {
                Document data = ((Document) events.get(i)).getMember("data");
                assertEquals(i, data.getMember("seq").asLong(), "event order");
                Document payload = data.getMember("payload");
                byte[] actual = new byte[0];
                if (payload != null) {
                    ByteBuffer blob = payload.asBlob().duplicate();
                    actual = new byte[blob.remaining()];
                    blob.get(actual);
                }
                assertArrayEquals(payloads.get(i), actual, "event " + i + " payload");
            }
        }
    }

    /**
     * An upstream buffer publisher driven by the trace: it records requests against the downstream ledger and
     * delivers either synchronously inside {@code request} or one buffer per pump step. It ends (with completion or
     * {@code failure}) once every buffer is delivered, on the same synchronous call or on the next pump.
     */
    static final class ScriptedUpstream implements Publisher<ByteBuffer> {
        private final List<byte[]> buffers;
        private final boolean synchronous;
        private final Throwable failure;
        private final Ledger downstream;
        private final LongSupplier consumed;
        private final long heldBeforeDelivery;
        private Subscriber<? super ByteBuffer> subscriber;
        private long outstanding;
        private int next;
        private boolean pumping;
        private boolean ended;
        boolean cancelled;
        int subscriptions;
        long maxOutstanding;
        long emittedBytes;
        long peakHeld;

        ScriptedUpstream(List<byte[]> buffers, boolean synchronous, Throwable failure, Ledger downstream,
                         LongSupplier consumed, long heldBeforeDelivery) {
            this.buffers = buffers;
            this.synchronous = synchronous;
            this.failure = failure;
            this.downstream = downstream;
            this.consumed = consumed;
            this.heldBeforeDelivery = heldBeforeDelivery;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            subscriptions++;
            subscriber = s;
            s.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    if (n <= 0) {
                        downstream.violation("non-positive upstream request(" + n + ")");
                        return;
                    }
                    if (cancelled || ended) {
                        return; // rule 3.6: a no-op
                    }
                    if (downstream.cancelled) {
                        downstream.violation("upstream request after downstream cancel");
                    } else if (downstream.outstanding() <= 0) {
                        downstream.violation("upstream request while downstream demand is zero");
                    }
                    long sum = outstanding + n;
                    outstanding = sum < 0 ? Long.MAX_VALUE : sum;
                    maxOutstanding = Math.max(maxOutstanding, outstanding);
                    if (synchronous) {
                        deliver(Integer.MAX_VALUE);
                    }
                }

                @Override
                public void cancel() {
                    cancelled = true;
                }
            });
        }

        /** One asynchronous delivery step. */
        void pump() {
            if (!synchronous) {
                deliver(1);
            }
        }

        private void deliver(int max) {
            if (pumping || subscriber == null) {
                return;
            }
            pumping = true;
            try {
                int signals = 0;
                while (!cancelled && !ended && signals < max) {
                    if (next == buffers.size()) {
                        ended = true;
                        if (failure != null) {
                            subscriber.onError(failure);
                        } else {
                            subscriber.onComplete();
                        }
                        return;
                    }
                    if (outstanding == 0) {
                        return;
                    }
                    long held = emittedBytes - consumed.getAsLong();
                    if (held > heldBeforeDelivery) {
                        downstream.violation("a new upstream buffer while " + held + " bytes are held (bound "
                                             + heldBeforeDelivery + ")");
                    }
                    byte[] buffer = buffers.get(next++);
                    outstanding--;
                    signals++;
                    emittedBytes += buffer.length;
                    peakHeld = Math.max(peakHeld, emittedBytes - consumed.getAsLong());
                    subscriber.onNext(ByteBuffer.wrap(buffer));
                }
            } finally {
                pumping = false;
            }
        }

        /** A downstream cancellation, or a failure raised by the adapter, cancels the upstream. */
        void checkCancelledIfStopped() {
            if (ended) {
                return;
            }
            if (downstream.cancelled || downstream.errors == 1) {
                assertTrue(cancelled, "the upstream subscription is cancelled (cancelled " + downstream.cancelled
                                      + ", error " + downstream.error + ")");
            }
        }
    }

    /** A one-shot {@link StreamingBody} over a scripted publisher. */
    static final class ScriptedBody implements StreamingBody {
        private final ScriptedUpstream publisher;

        ScriptedBody(ScriptedUpstream publisher) {
            this.publisher = publisher;
        }

        @Override
        public OptionalLong contentLength() {
            return OptionalLong.empty();
        }

        @Override
        public Optional<String> contentType() {
            return Optional.of(EventStreams.CONTENT_TYPE);
        }

        @Override
        public boolean isReplayable() {
            return false;
        }

        @Override
        public boolean isAvailable() {
            return publisher.subscriptions == 0;
        }

        @Override
        public int maxChunkSize() {
            return StreamingBodies.DEFAULT_MAX_CHUNK_SIZE;
        }

        @Override
        public InputStream openStream() {
            throw new UnsupportedOperationException("async only");
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            publisher.subscribe(subscriber);
        }
    }

    /**
     * A blocking source that returns at most the next generated read size per read, checks that every read happens
     * under positive downstream demand, and counts bytes read but not yet delivered.
     */
    static final class TrackedStream extends InputStream {
        private final byte[] content;
        private final List<Integer> readSizes;
        private final Ledger downstream;
        LongSupplier delivered = () -> 0;
        private int position;
        private int reads;
        boolean opened;
        boolean closed;
        long peakHeld;

        TrackedStream(byte[] content, List<Integer> readSizes, Ledger downstream) {
            this.content = content;
            this.readSizes = readSizes;
            this.downstream = downstream;
        }

        @Override
        public int read() {
            byte[] one = new byte[1];
            return read(one, 0, 1) < 0 ? -1 : one[0] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            opened = true;
            if (closed) {
                downstream.violation("read after close");
            }
            if (downstream.cancelled) {
                downstream.violation("source read after cancel");
            } else if (downstream.outstanding() <= 0) {
                downstream.violation("source read while downstream demand is zero");
            }
            // Bytes read earlier but not yet delivered are buffered by the publisher.
            peakHeld = Math.max(peakHeld, position - delivered.getAsLong());
            if (position == content.length) {
                return -1;
            }
            int n = Math.min(Math.min(len, readSizes.get(reads++ % readSizes.size())), content.length - position);
            System.arraycopy(content, position, b, off, n);
            position += n;
            return n;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Content helpers
    // ---------------------------------------------------------------------------------------------------------------

    /** {@code content} cut into consecutive buffers of the (cyclic) split sizes; zero sizes yield empty buffers. */
    private static List<byte[]> split(byte[] content, List<Integer> sizes) {
        List<byte[]> out = new ArrayList<>();
        int position = 0;
        int i = 0;
        int emptyRun = 0;
        while (position < content.length) {
            int size = sizes.get(i++ % sizes.size());
            if (size == 0) {
                if (++emptyRun > sizes.size()) {
                    size = content.length - position; // every size is zero: one buffer of the rest
                } else {
                    out.add(new byte[0]);
                    continue;
                }
            }
            emptyRun = 0;
            int n = Math.min(size, content.length - position);
            out.add(Arrays.copyOfRange(content, position, position + n));
            position += n;
        }
        return out;
    }

    /** The encoded {@code data} event frame with sequence number {@code seq} and blob {@code payload}. */
    private static byte[] frame(long seq, byte[] payload) {
        Map<String, HeaderValue> headers = new LinkedHashMap<>();
        headers.put(EventStreamCodec.MESSAGE_TYPE, HeaderValue.ofString("event"));
        headers.put(EventStreamCodec.EVENT_TYPE, HeaderValue.ofString("data"));
        headers.put(EventStreamCodec.CONTENT_TYPE, HeaderValue.ofString("application/octet-stream"));
        headers.put("seq", HeaderValue.ofLong(seq));
        ByteBuffer encoded = MessageEncoder.encode(new Message(headers, payload));
        byte[] out = new byte[encoded.remaining()];
        encoded.get(out);
        return out;
    }

    private static EventStreamCodec eventCodec() {
        Model model = Model.assembler().addUnparsedModel("events.smithy", EVENTS_IDL).assemble().unwrap();
        SchemaConverter converter = new SchemaConverter(model);
        return EventStreamCodec.of(new WireContext(ShapeId.from("aws.protocols#restJson1"),
                                                   ShapeId.from("test.flow#SubscribeToEvents")),
                                   converter.getSchema(model.expectShape(ShapeId.from("test.flow#Events"))),
                                   EventPayloadCodec.of(JsonCodec.builder().build(), "application/json"), null);
    }
}
