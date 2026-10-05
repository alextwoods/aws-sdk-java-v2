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

package software.amazon.awssdk.eventstream;

import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Event stream signature chaining: the {@link FrameTransformer} that signs every outgoing frame, in order, with the
 * signature of the previous frame (initially the seed signature of the signed HTTP request) as chain state, and
 * appends the signed empty closing frame when the stream ends.
 *
 * <p>Every message becomes a signed envelope whose payload is the encoded message and whose headers are, in order,
 * <ul>
 *   <li>{@value #DATE_HEADER}: the signing time (timestamp, millisecond precision), and</li>
 *   <li>{@value #CHUNK_SIGNATURE_HEADER}: the frame signature (byte array) computed by the
 *       {@link FrameSignatureAlgorithm} over the prior signature, the signing time, the encoded
 *       {@value #DATE_HEADER} header and the payload.</li>
 * </ul>
 * The frame signature becomes the prior signature of the next frame. The closing frame has an empty payload.
 *
 * <p>Use one signer per stream, attached after the initial request is signed, for example
 * {@code requestBody.withFrameTransformer(EventStreamSigner.sigV4(seed, region, service, keys, clock))}. A signer is
 * thread safe (serialized); a stream writer invokes it in frame order.
 *
 * <p>Failure: a signature that cannot be computed throws {@link EventStreamSigningException} and fails the signer
 * permanently (the chain cannot skip a frame), so every later call throws {@link IllegalStateException}. After the
 * closing frame, every later call throws {@link IllegalStateException}. No failure message contains key material,
 * signatures, header values or payload bytes.
 */
public final class EventStreamSigner implements FrameTransformer {
    /** The envelope header carrying the signing time. */
    public static final String DATE_HEADER = ":date";
    /** The envelope header carrying the frame signature. */
    public static final String CHUNK_SIGNATURE_HEADER = ":chunk-signature";
    /**
     * The {@code x-amz-content-sha256} value with which SigV4 signs the initial HTTP request of a signed event
     * stream; the request signature is the seed of the chain.
     */
    public static final String STREAMING_EVENTS_PAYLOAD = "STREAMING-AWS4-HMAC-SHA256-EVENTS";

    private static final byte[] EMPTY = new byte[0];

    private enum State { OPEN, CLOSED, FAILED }

    private final FrameSignatureAlgorithm algorithm;
    private final Supplier<Instant> time;

    // Guarded by this.
    private byte[] priorSignature;
    private long signedFrames;
    private State state = State.OPEN;

    private EventStreamSigner(byte[] seedSignature, FrameSignatureAlgorithm algorithm, Supplier<Instant> time) {
        Objects.requireNonNull(seedSignature, "seedSignature");
        if (seedSignature.length == 0) {
            throw new IllegalArgumentException("The seed signature must not be empty");
        }
        this.priorSignature = seedSignature.clone();
        this.algorithm = Objects.requireNonNull(algorithm, "algorithm");
        this.time = Objects.requireNonNull(time, "time");
    }

    /**
     * A signer whose chain starts at {@code seedSignature} and whose frames are signed by {@code algorithm} at
     * {@code clock}'s time.
     *
     * @throws IllegalArgumentException if {@code seedSignature} is empty
     */
    public static EventStreamSigner create(byte[] seedSignature, FrameSignatureAlgorithm algorithm, Clock clock) {
        return new EventStreamSigner(seedSignature, algorithm, instantsOf(clock));
    }

    /**
     * A SigV4 signer: the chain starts at {@code seedSignatureHex}, the hex signature of the initial request (the
     * {@code Signature=} value of its {@code Authorization} header), and frames are signed with
     * {@link SigV4FrameSignatureAlgorithm} for {@code region}/{@code service} with keys from {@code keys}.
     *
     * @throws IllegalArgumentException if {@code seedSignatureHex} is empty or not hexadecimal, or {@code region} or
     *         {@code service} is invalid
     */
    public static EventStreamSigner sigV4(String seedSignatureHex, String region, String service,
                                          SigningKeyProvider keys, Clock clock) {
        return sigV4(seedSignatureHex, region, service, keys, instantsOf(clock));
    }

    /**
     * A SigV4 signer like {@link #sigV4(String, String, String, SigningKeyProvider, Clock)} whose frames are signed
     * at the instant {@code time} supplies: it is read once per frame and truncated to milliseconds, exactly like a
     * clock's instant. Lets a caller that must not depend on clock types (the request pipeline) pass the time source
     * of the request signer.
     *
     * @throws IllegalArgumentException if {@code seedSignatureHex} is empty or not hexadecimal, or {@code region} or
     *         {@code service} is invalid
     */
    public static EventStreamSigner sigV4(String seedSignatureHex, String region, String service,
                                          SigningKeyProvider keys, Supplier<Instant> time) {
        Objects.requireNonNull(seedSignatureHex, "seedSignatureHex");
        Objects.requireNonNull(time, "time");
        byte[] seed;
        try {
            seed = HexFormat.of().parseHex(seedSignatureHex);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("The seed signature must be hexadecimal", e);
        }
        return new EventStreamSigner(seed, SigV4FrameSignatureAlgorithm.create(region, service, keys), time);
    }

    private static Supplier<Instant> instantsOf(Clock clock) {
        Objects.requireNonNull(clock, "clock");
        return clock::instant;
    }

    /** The frame signature algorithm. */
    public FrameSignatureAlgorithm algorithm() {
        return algorithm;
    }

    /** A copy of the current chain state: the last frame signature, or the seed before the first frame. */
    public synchronized byte[] priorSignature() {
        return priorSignature.clone();
    }

    /** The number of frames signed so far, including the closing frame. */
    public synchronized long signedFrames() {
        return signedFrames;
    }

    /** True once the closing frame was signed. */
    public synchronized boolean isClosed() {
        return state == State.CLOSED;
    }

    /**
     * The signed envelope of {@code message}.
     *
     * @throws EventStreamSigningException if the frame cannot be signed (the signer fails permanently)
     * @throws IllegalStateException if the signer failed or signed its closing frame
     */
    @Override
    public synchronized Message transform(Message message) {
        Objects.requireNonNull(message, "message");
        checkOpen();
        int length = MessageEncoder.encodedLength(message);
        if (length > MessageEncoder.MAX_PAYLOAD_LENGTH) {
            state = State.FAILED;
            throw EventStreamSigningException.create(
                "An encoded event stream message of " + length + " bytes exceeds the signed-frame payload limit of "
                + MessageEncoder.MAX_PAYLOAD_LENGTH + " bytes", null);
        }
        ByteBuffer encoded;
        try {
            encoded = MessageEncoder.encode(message);
        } catch (RuntimeException e) {
            state = State.FAILED;
            throw EventStreamSigningException.create("The event stream message could not be encoded for signing", e);
        }
        // encode returns an exactly sized heap buffer: its array is the envelope payload without a copy.
        return sign(encoded.array());
    }

    /**
     * The signed empty closing frame; the signer is closed afterwards.
     *
     * @throws EventStreamSigningException if the frame cannot be signed (the signer fails permanently)
     * @throws IllegalStateException if the signer failed or already signed its closing frame
     */
    @Override
    public synchronized Message closingFrame() {
        checkOpen();
        Message closing = sign(EMPTY);
        state = State.CLOSED;
        return closing;
    }

    /** Package-private: the signed envelope of an already encoded frame payload (a copy is signed). */
    synchronized Message signPayload(byte[] payload) {
        checkOpen();
        return sign(payload.clone());
    }

    private void checkOpen() {
        switch (state) {
            case OPEN -> {
            }
            case CLOSED -> throw new IllegalStateException("The event stream signer already signed its closing frame");
            case FAILED -> throw new IllegalStateException("The event stream signer failed and cannot sign further "
                                                           + "frames");
            default -> throw new IllegalStateException("Unknown state " + state);
        }
    }

    private Message sign(byte[] payload) {
        Instant signingTime;
        byte[] signature;
        LinkedHashMap<String, HeaderValue> headers = new LinkedHashMap<>(4);
        try {
            signingTime = Objects.requireNonNull(time.get(), "signing time").truncatedTo(ChronoUnit.MILLIS);
            headers.put(DATE_HEADER, HeaderValue.ofTimestamp(signingTime));
            byte[] signedHeaders = MessageEncoder.encodeHeaders(headers);
            signature = algorithm.sign(priorSignature.clone(), signingTime, signedHeaders,
                                       ByteBuffer.wrap(payload).asReadOnlyBuffer());
        } catch (RuntimeException e) {
            state = State.FAILED;
            throw EventStreamSigningException.create("Event stream frame " + signedFrames + " could not be signed", e);
        }
        if (signature == null || signature.length == 0 || signature.length > HeaderValue.MAX_VALUE_LENGTH) {
            state = State.FAILED;
            throw EventStreamSigningException.create("The frame signature algorithm returned an invalid signature for "
                                                     + "event stream frame " + signedFrames, null);
        }
        byte[] chained = signature.clone();
        headers.put(CHUNK_SIGNATURE_HEADER, HeaderValue.wrapByteArray(chained.clone()));
        priorSignature = chained;
        signedFrames++;
        return Message.wrap(headers, payload);
    }

    @Override
    public synchronized String toString() {
        return "EventStreamSigner[algorithm=" + algorithm + ", signedFrames=" + signedFrames + ", state=" + state + "]";
    }
}
