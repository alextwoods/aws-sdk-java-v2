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

package software.amazon.awssdk.protocols.core.eventstream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.CRC32;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.statistics.Statistics;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.eventstream.EventStreamSigner;
import software.amazon.awssdk.eventstream.HeaderValue;
import software.amazon.awssdk.eventstream.Message;
import software.amazon.awssdk.eventstream.MessageEncoder;
import software.amazon.awssdk.eventstream.SigningKeyProvider;
import software.amazon.awssdk.protocols.core.serde.WireContext;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.event.EventStream;
import software.amazon.smithy.java.dynamicschemas.SchemaConverter;
import software.amazon.smithy.java.json.JsonCodec;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Property 48 (Requirement 13.10): for any modeled event stream frame sequence, every frame requiring signing is
 * signed in order under the selected auth scheme (SigV4, {@code AWS4-HMAC-SHA256-PAYLOAD}) and verifies against the
 * prior signature state.
 *
 * <p>Each try generates an identity (secret access key, signing region and signing name), a seed (request)
 * signature in lower- or upper-case hex, a frame sequence (0 to 12 messages with random typed headers and payloads of
 * up to 70,000 bytes, or modeled events for the request-body views) and the signer's clock readings: sub-millisecond,
 * millisecond, second, hour and small backward steps, from a random start or just before a UTC midnight so that
 * streams cross (and occasionally re-cross) scope dates.
 *
 * <p>The chain verifier is independent of the production signing code: it parses the wire bytes with its own
 * event stream frame parser (checking both CRCs), derives SigV4 signing keys from the generated secret itself,
 * builds the frame string-to-sign and the encoded {@code :date} header from the SigV4 event stream specification, and
 * encodes the expected inner messages with its own encoder. It checks, frame by frame, that
 * <ul>
 *   <li>every message becomes exactly one envelope, in order, followed by one empty closing frame;</li>
 *   <li>each envelope carries exactly {@code :date} (the clock reading truncated to milliseconds) then a 32-byte
 *       {@code :chunk-signature}, and its payload is the encoded message;</li>
 *   <li>each signature equals HMAC-SHA256 over the frame's string-to-sign keyed with the signing key of the frame's
 *       UTC scope date and chained on the previous frame's signature (the seed for the first frame);</li>
 *   <li>the signer's state ({@code priorSignature()}, {@code signedFrames()}, {@code isClosed()}) tracks the chain
 *       after every frame and at the end, and signing keys are requested once per change of scope date.</li>
 * </ul>
 * The request-body property sends the same chain through the sync ({@code openStream}) and async
 * ({@code subscribe}) views of {@link EventStreamRequestBody}, optionally with an RPC initial event, and requires both
 * to transmit identical, verified bytes.
 */
class EventStreamSignatureChainPropertyTest {
    private static final HexFormat HEX = HexFormat.of();
    private static final String DATE_HEADER = ":date";
    private static final String SIGNATURE_HEADER = ":chunk-signature";
    private static final int TYPE_BYTE_ARRAY = 6;
    private static final int TYPE_TIMESTAMP = 8;
    private static final int SIGNATURE_LENGTH = 32;
    private static final long YEAR_2000_MILLIS = 946_684_800_000L;
    private static final long YEAR_2100_MILLIS = 4_102_444_800_000L;
    private static final long MILLIS_PER_DAY = 86_400_000L;
    /** The writer's frame queue holds 16 frames; the body views write every event before reading. */
    private static final int MAX_BODY_EVENTS = 12;
    private static final EventStreamCodec CODEC = codec();

    // Feature: aws-sdk-java-v3, Property 48: Event Stream signatures form the modeled chain
    @Property(tries = 1_000)
    @Label("Property 48: Event Stream signatures form the modeled chain")
    void signerProducesTheModeledChain(@ForAll("signerStreams") SignerStream stream) {
        KeyRequests keys = new KeyRequests(stream.identity());
        ReadingClock clock = new ReadingClock(stream.clockReadings());
        EventStreamSigner signer = EventStreamSigner.sigV4(stream.seedHex(), stream.identity().region(),
                                                           stream.identity().service(), keys, clock);
        assertArrayEquals(stream.seed(), signer.priorSignature(), "the seed starts the chain");
        assertEquals(0, signer.signedFrames());

        ByteArrayOutputStream wire = new ByteArrayOutputStream();
        List<byte[]> observedPriors = new ArrayList<>();
        List<byte[]> expectedInner = new ArrayList<>();
        for (InnerMessage message : stream.messages()) {
            Message envelope = signer.transform(message.toMessage());
            wire.writeBytes(bytes(MessageEncoder.encode(envelope)));
            observedPriors.add(signer.priorSignature());
            assertEquals(observedPriors.size(), signer.signedFrames(), "one signed frame per message");
            assertFalse(signer.isClosed());
            expectedInner.add(message.encode());
        }
        wire.writeBytes(bytes(MessageEncoder.encode(signer.closingFrame())));
        observedPriors.add(signer.priorSignature());

        List<byte[]> chain = verifyChain(stream.identity(), stream.seed(), stream.clockReadings(),
                                         wire.toByteArray(), expectedInner);
        for (int i = 0; i < chain.size(); i++) {
            assertArrayEquals(chain.get(i), observedPriors.get(i), "priorSignature() after frame " + i);
        }
        assertArrayEquals(chain.get(chain.size() - 1), signer.priorSignature(), "the final chain state");
        assertEquals(stream.messages().size() + 1, signer.signedFrames(), "messages plus the closing frame");
        assertTrue(signer.isClosed());
        assertEquals(stream.clockReadings().size(), clock.reads(), "one clock reading per frame");
        assertEquals(expectedKeyRequests(stream.clockReadings()), keys.requested(),
                     "one signing key per change of scope date");

        collectStatistics(stream.messages().size(), stream.clockReadings());
    }

    // Feature: aws-sdk-java-v3, Property 48: Event Stream signatures form the modeled chain
    // 1,000 tries, like the property above: the coverage checks in collectStatistics need them. At 200 tries the
    // "1-3 messages" bucket (expected ~16%, required >= 10%) missed its threshold in about 1% of runs.
    @Property(tries = 1_000)
    @Label("Property 48: Event Stream signatures form the modeled chain (sync and async request bodies)")
    void requestBodyViewsSendTheModeledChain(@ForAll("bodyStreams") BodyStream stream) throws Exception {
        SerializableStruct initial = stream.initialPayload() == null ? null : event(stream.initialPayload());
        List<SerializableStruct> events = stream.eventPayloads().stream().map(p -> event(p)).toList();
        List<byte[]> expectedInner = new ArrayList<>();
        if (initial != null) {
            expectedInner.add(bytes(MessageEncoder.encode(CODEC.encodeEvent(initial))));
        }
        for (SerializableStruct e : events) {
            expectedInner.add(bytes(MessageEncoder.encode(CODEC.encodeEvent(e))));
        }

        KeyRequests syncKeys = new KeyRequests(stream.identity());
        EventStreamSigner syncSigner = signer(stream, syncKeys);
        EventStream<SerializableStruct> syncWriter = EventStream.newWriter();
        byte[] syncBytes;
        try (InputStream in = EventStreams.requestBody(syncWriter, CODEC, initial)
                                          .withFrameTransformer(syncSigner)
                                          .openStream()) {
            writeAll(syncWriter, events);
            syncBytes = in.readAllBytes();
        }

        KeyRequests asyncKeys = new KeyRequests(stream.identity());
        EventStreamSigner asyncSigner = signer(stream, asyncKeys);
        EventStream<SerializableStruct> asyncWriter = EventStream.newWriter();
        Collector collector = new Collector();
        EventStreams.requestBody(asyncWriter, CODEC, initial).withFrameTransformer(asyncSigner).subscribe(collector);
        writeAll(asyncWriter, events);
        collector.subscription.request(Long.MAX_VALUE);
        assertTrue(collector.completed.await(10, TimeUnit.SECONDS), "the async view completes");
        assertNull(collector.error.get());

        List<byte[]> chain = verifyChain(stream.identity(), stream.seed(), stream.clockReadings(), syncBytes,
                                         expectedInner);
        assertArrayEquals(syncBytes, collector.allBytes(), "the sync and async views send identical frames");
        for (int i = 0; i < chain.size() - 1; i++) {
            // The inner payload of each event is the generated blob (checked by the independent parser).
            RawFrame inner = RawFrame.parseAll(expectedInner.get(i)).get(0);
            byte[] blob = i == 0 && initial != null ? stream.initialPayload()
                                                    : stream.eventPayloads().get(initial == null ? i : i - 1);
            assertArrayEquals(blob, inner.payload(), "event payload " + i);
        }
        byte[] last = chain.get(chain.size() - 1);
        List<LocalDate> expectedKeys = expectedKeyRequests(stream.clockReadings());
        for (EventStreamSigner signer : List.of(syncSigner, asyncSigner)) {
            assertArrayEquals(last, signer.priorSignature(), "the final chain state");
            assertEquals(expectedInner.size() + 1, signer.signedFrames(), "frames plus the closing frame");
            assertTrue(signer.isClosed());
        }
        assertEquals(expectedKeys, syncKeys.requested());
        assertEquals(expectedKeys, asyncKeys.requested());

        Statistics.label("body initial event").collect(initial != null);
        Statistics.label("body initial event").coverage(c -> c.check(true).percentage(p -> p >= 20));
        collectStatistics(expectedInner.size(), stream.clockReadings());
    }

    // The independent chain verifier.

    /**
     * Parses {@code wire} with the independent frame parser and verifies every envelope against the chain that
     * starts at {@code seed}; returns the verified signatures in frame order (the closing frame last).
     */
    private static List<byte[]> verifyChain(Identity identity, byte[] seed, List<Instant> clockReadings, byte[] wire,
                                            List<byte[]> expectedInner) {
        List<RawFrame> frames = RawFrame.parseAll(wire);
        assertEquals(expectedInner.size() + 1, frames.size(), "one envelope per message plus the closing frame");
        assertEquals(clockReadings.size(), frames.size(), "one clock reading per frame");
        List<byte[]> signatures = new ArrayList<>();
        byte[] prior = seed;
        for (int i = 0; i < frames.size(); i++) {
            RawFrame frame = frames.get(i);
            Instant signingTime = clockReadings.get(i).truncatedTo(ChronoUnit.MILLIS);
            byte[] dateHeader = Sigv4Oracle.encodedDateHeader(signingTime.toEpochMilli());
            assertEquals(2, frame.headers().size(), "frame " + i + " has exactly :date and :chunk-signature");
            RawHeader date = frame.headers().get(0);
            assertEquals(DATE_HEADER, date.name(), "frame " + i + " header 0");
            assertEquals(TYPE_TIMESTAMP, date.type(), "frame " + i + " :date type");
            assertEquals(signingTime.toEpochMilli(), ByteBuffer.wrap(date.value()).getLong(),
                         "frame " + i + " :date is the millisecond signing time");
            RawHeader signature = frame.headers().get(1);
            assertEquals(SIGNATURE_HEADER, signature.name(), "frame " + i + " header 1");
            assertEquals(TYPE_BYTE_ARRAY, signature.type(), "frame " + i + " :chunk-signature type");
            assertEquals(SIGNATURE_LENGTH, signature.value().length, "frame " + i + " signature length");

            byte[] expectedPayload = i < expectedInner.size() ? expectedInner.get(i) : new byte[0];
            assertArrayEquals(expectedPayload, frame.payload(), i < expectedInner.size()
                                                                ? "frame " + i + " payload is the encoded message"
                                                                : "the closing frame is empty");
            byte[] expected = Sigv4Oracle.frameSignature(identity, prior, signingTime, dateHeader, frame.payload());
            if (!MessageDigest.isEqual(expected, signature.value())) {
                fail("frame " + i + " of " + frames.size() + " does not verify against the prior signature: expected "
                     + HEX.formatHex(expected) + ", was " + HEX.formatHex(signature.value()));
            }
            signatures.add(signature.value());
            prior = signature.value();
        }
        return signatures;
    }

    /** The scope dates in the order a per-date key cache requests them: one request per change of UTC date. */
    private static List<LocalDate> expectedKeyRequests(List<Instant> clockReadings) {
        List<LocalDate> dates = new ArrayList<>();
        for (Instant reading : clockReadings) {
            LocalDate date = reading.truncatedTo(ChronoUnit.MILLIS).atOffset(ZoneOffset.UTC).toLocalDate();
            if (dates.isEmpty() || !dates.get(dates.size() - 1).equals(date)) {
                dates.add(date);
            }
        }
        return dates;
    }

    /** SigV4 key derivation and the event stream frame string-to-sign, written out from the SigV4 specification. */
    private static final class Sigv4Oracle {
        static byte[] signingKey(Identity identity, LocalDate date) {
            byte[] key = hmac(("AWS4" + identity.secretKey()).getBytes(StandardCharsets.UTF_8), yyyyMMdd(date));
            key = hmac(key, identity.region());
            key = hmac(key, identity.service());
            return hmac(key, "aws4_request");
        }

        static byte[] frameSignature(Identity identity, byte[] prior, Instant signingTime, byte[] dateHeader,
                                     byte[] payload) {
            OffsetDateTime utc = signingTime.atOffset(ZoneOffset.UTC);
            String date = yyyyMMdd(utc.toLocalDate());
            String dateTime = date + String.format("T%02d%02d%02dZ", utc.getHour(), utc.getMinute(), utc.getSecond());
            String stringToSign = "AWS4-HMAC-SHA256-PAYLOAD\n"
                                  + dateTime + "\n"
                                  + date + "/" + identity.region() + "/" + identity.service() + "/aws4_request\n"
                                  + HEX.formatHex(prior) + "\n"
                                  + HEX.formatHex(sha256(dateHeader)) + "\n"
                                  + HEX.formatHex(sha256(payload));
            return hmac(signingKey(identity, utc.toLocalDate()), stringToSign);
        }

        /** The encoded {@code :date} header: name length, name, type 8 (timestamp), big-endian epoch millis. */
        static byte[] encodedDateHeader(long epochMillis) {
            byte[] name = DATE_HEADER.getBytes(StandardCharsets.US_ASCII);
            return ByteBuffer.allocate(1 + name.length + 1 + 8)
                             .put((byte) name.length).put(name).put((byte) TYPE_TIMESTAMP).putLong(epochMillis)
                             .array();
        }

        private static String yyyyMMdd(LocalDate date) {
            return String.format("%04d%02d%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
        }

        private static byte[] hmac(byte[] key, String data) {
            try {
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(key, "HmacSHA256"));
                return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }

        private static byte[] sha256(byte[] data) {
            try {
                return MessageDigest.getInstance("SHA-256").digest(data);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }
    }

    /** One header as parsed from the wire; {@code value} excludes the length prefix of byte array and string values. */
    private record RawHeader(String name, int type, byte[] value) {
    }

    /** One frame as parsed from the wire by the independent parser. */
    private record RawFrame(List<RawHeader> headers, byte[] payload) {
        /** Parses consecutive frames covering all of {@code wire}, checking lengths and both CRCs. */
        static List<RawFrame> parseAll(byte[] wire) {
            List<RawFrame> frames = new ArrayList<>();
            int start = 0;
            while (start < wire.length) {
                assertTrue(wire.length - start >= 16, "a truncated prelude at byte " + start);
                ByteBuffer in = ByteBuffer.wrap(wire);
                int total = in.getInt(start);
                int headersLength = in.getInt(start + 4);
                assertEquals(crc(wire, start, 8), Integer.toUnsignedLong(in.getInt(start + 8)), "prelude CRC");
                assertTrue(total >= 16 + headersLength && total <= wire.length - start, "frame length " + total);
                assertEquals(crc(wire, start, total - 4), Integer.toUnsignedLong(in.getInt(start + total - 4)),
                             "message CRC");
                List<RawHeader> headers = new ArrayList<>();
                int position = start + 12;
                int headersEnd = position + headersLength;
                while (position < headersEnd) {
                    int nameLength = wire[position++] & 0xFF;
                    String name = new String(wire, position, nameLength, StandardCharsets.UTF_8);
                    position += nameLength;
                    int type = wire[position++];
                    int valueLength = switch (type) {
                        case 0, 1 -> 0;
                        case 2 -> 1;
                        case 3 -> 2;
                        case 4 -> 4;
                        case 5, 8 -> 8;
                        case 6, 7 -> {
                            int length = in.getShort(position) & 0xFFFF;
                            position += 2;
                            yield length;
                        }
                        case 9 -> 16;
                        default -> throw new AssertionError("unknown header type " + type);
                    };
                    byte[] value = new byte[valueLength];
                    System.arraycopy(wire, position, value, 0, valueLength);
                    position += valueLength;
                    headers.add(new RawHeader(name, type, value));
                }
                assertEquals(headersEnd, position, "the header section ends at its declared length");
                byte[] payload = new byte[total - 16 - headersLength];
                System.arraycopy(wire, headersEnd, payload, 0, payload.length);
                frames.add(new RawFrame(headers, payload));
                start += total;
            }
            return frames;
        }

        private static long crc(byte[] bytes, int offset, int length) {
            CRC32 crc = new CRC32();
            crc.update(bytes, offset, length);
            return crc.getValue();
        }
    }

    // Generated inputs.

    /** The signing identity: a secret access key and the credential scope's region and signing name. */
    record Identity(String secretKey, String region, String service) {
        @Override
        public String toString() {
            return "Identity[region=" + region + ", service=" + service + ", secretLength=" + secretKey.length() + "]";
        }
    }

    /** A typed header value with its independently encoded value bytes (without a length prefix). */
    record TypedValue(HeaderValue value, int type, byte[] raw) {
        @Override
        public String toString() {
            return "type " + type + " (" + raw.length + " bytes)";
        }
    }

    record HeaderSpec(String name, TypedValue value) {
        @Override
        public String toString() {
            return name + ": " + value;
        }
    }

    /** A message to sign: unique headers in order and a payload filled from a seeded generator. */
    record InnerMessage(List<HeaderSpec> headers, byte[] payload) {
        Message toMessage() {
            Map<String, HeaderValue> map = new LinkedHashMap<>();
            headers.forEach(h -> map.put(h.name(), h.value().value()));
            return new Message(map, payload);
        }

        /** The independent encoding: prelude, prelude CRC, headers, payload, message CRC. */
        byte[] encode() {
            ByteArrayOutputStream section = new ByteArrayOutputStream();
            for (HeaderSpec header : headers) {
                byte[] name = header.name().getBytes(StandardCharsets.UTF_8);
                section.write(name.length);
                section.writeBytes(name);
                section.write(header.value().type());
                if (header.value().type() == 6 || header.value().type() == 7) {
                    int length = header.value().raw().length;
                    section.write(length >>> 8);
                    section.write(length & 0xFF);
                }
                section.writeBytes(header.value().raw());
            }
            byte[] headerBytes = section.toByteArray();
            int total = 16 + headerBytes.length + payload.length;
            ByteBuffer out = ByteBuffer.allocate(total);
            out.putInt(total).putInt(headerBytes.length);
            out.putInt((int) RawFrame.crc(out.array(), 0, 8));
            out.put(headerBytes).put(payload);
            out.putInt((int) RawFrame.crc(out.array(), 0, total - 4));
            return out.array();
        }

        @Override
        public String toString() {
            return "Message[headers=" + headers + ", payloadLength=" + payload.length + "]";
        }
    }

    record SignerStream(Identity identity, byte[] seed, boolean upperCaseSeed, List<InnerMessage> messages,
                        List<Instant> clockReadings) {
        String seedHex() {
            String hex = HEX.formatHex(seed);
            return upperCaseSeed ? hex.toUpperCase(Locale.ROOT) : hex;
        }

        @Override
        public String toString() {
            return "SignerStream[" + identity + ", seed=" + seedHex() + ", messages=" + messages + ", clock="
                   + clockReadings + "]";
        }
    }

    record BodyStream(Identity identity, byte[] seed, byte[] initialPayload, List<byte[]> eventPayloads,
                      List<Instant> clockReadings) {
        @Override
        public String toString() {
            return "BodyStream[" + identity + ", seed=" + HEX.formatHex(seed) + ", initial="
                   + (initialPayload == null ? "none" : initialPayload.length + " bytes") + ", events="
                   + eventPayloads.stream().map(p -> p.length).toList() + ", clock=" + clockReadings + "]";
        }
    }

    @Provide
    Arbitrary<SignerStream> signerStreams() {
        return Combinators.combine(identities(), seeds(), Arbitraries.of(false, true),
                                   innerMessages().list().ofMinSize(0).ofMaxSize(12))
                          .flatAs((identity, seed, upper, messages) ->
                                      clockReadings(messages.size() + 1)
                                          .map(clock -> new SignerStream(identity, seed, upper, messages, clock)));
    }

    @Provide
    Arbitrary<BodyStream> bodyStreams() {
        // null: no initial event (non-RPC protocols).
        Arbitrary<byte[]> initial = payloads(4096).injectNull(0.6);
        return Combinators.combine(identities(), seeds(), initial,
                                   payloads(4096).list().ofMinSize(0).ofMaxSize(MAX_BODY_EVENTS))
                          .flatAs((identity, seed, init, events) ->
                                      clockReadings(events.size() + 1 + (init == null ? 0 : 1))
                                          .map(clock -> new BodyStream(identity, seed, init, events, clock)));
    }

    private static Arbitrary<Identity> identities() {
        Arbitrary<String> secrets = Arbitraries.strings().withCharRange('!', '~').ofMinLength(1).ofMaxLength(64);
        Arbitrary<String> regions = Arbitraries.oneOf(
            Arbitraries.of("us-east-1", "us-west-2", "eu-central-1", "ap-southeast-2", "cn-north-1", "us-gov-west-1"),
            Arbitraries.strings().withChars("abcdefghijklmnopqrstuvwxyz0123456789-").ofMinLength(1).ofMaxLength(20));
        Arbitrary<String> services = Arbitraries.oneOf(
            Arbitraries.of("transcribe", "kinesis", "lex", "bedrock", "s3", "rekognition"),
            Arbitraries.strings().withChars("abcdefghijklmnopqrstuvwxyz0123456789-.").ofMinLength(1).ofMaxLength(20));
        return Combinators.combine(secrets, regions, services).as(Identity::new);
    }

    private static Arbitrary<byte[]> seeds() {
        return Arbitraries.frequencyOf(
            Tuple.of(3, Arbitraries.bytes().array(byte[].class).ofSize(SIGNATURE_LENGTH)),
            Tuple.of(1, Arbitraries.bytes().array(byte[].class).ofMinSize(1).ofMaxSize(64)));
    }

    /** Payloads by (length, seed) so that large payloads are cheap to generate and shrink by length. */
    private static Arbitrary<byte[]> payloads(int max) {
        Arbitrary<Integer> lengths = Arbitraries.frequencyOf(
            Tuple.of(1, Arbitraries.just(0)),
            Tuple.of(6, Arbitraries.integers().between(1, Math.min(256, max))),
            Tuple.of(3, Arbitraries.integers().between(1, max)));
        return Combinators.combine(lengths, Arbitraries.longs()).as((length, seed) -> {
            byte[] bytes = new byte[length];
            new Random(seed).nextBytes(bytes);
            return bytes;
        });
    }

    private static Arbitrary<InnerMessage> innerMessages() {
        Arbitrary<List<HeaderSpec>> headers = Combinators.combine(headerNames(), typedValues())
                                                         .as(HeaderSpec::new)
                                                         .list().ofMaxSize(6).uniqueElements(HeaderSpec::name);
        Arbitrary<byte[]> payload = Arbitraries.frequencyOf(Tuple.of(9, payloads(4096)),
                                                            Tuple.of(1, payloads(70_000)));
        return Combinators.combine(headers, payload).as(InnerMessage::new);
    }

    private static Arbitrary<String> headerNames() {
        return Arbitraries.oneOf(
            Arbitraries.of(":message-type", ":event-type", ":content-type", "x-amz-id", "flag"),
            Arbitraries.strings().withChars("abcdefghijklmnopqrstuvwxyz:-_").withCharRange('\u00e0', '\u00ff')
                       .ofMinLength(1).ofMaxLength(16));
    }

    private static Arbitrary<TypedValue> typedValues() {
        Arbitrary<String> strings = Arbitraries.strings().withCharRange(' ', '~').withCharRange('\u00a0', '\u07ff')
                                               .ofMaxLength(24);
        return Arbitraries.oneOf(
            Arbitraries.of(true, false).map(b -> new TypedValue(HeaderValue.ofBoolean(b), b ? 0 : 1, new byte[0])),
            Arbitraries.bytes().map(b -> new TypedValue(HeaderValue.ofByte(b), 2, new byte[] {b})),
            Arbitraries.shorts().map(s -> new TypedValue(HeaderValue.ofShort(s), 3,
                                                         ByteBuffer.allocate(2).putShort(s).array())),
            Arbitraries.integers().map(i -> new TypedValue(HeaderValue.ofInteger(i), 4,
                                                           ByteBuffer.allocate(4).putInt(i).array())),
            Arbitraries.longs().map(l -> new TypedValue(HeaderValue.ofLong(l), 5,
                                                        ByteBuffer.allocate(8).putLong(l).array())),
            Arbitraries.bytes().array(byte[].class).ofMaxSize(48)
                       .map(b -> new TypedValue(HeaderValue.ofByteArray(b), 6, b.clone())),
            strings.map(s -> new TypedValue(HeaderValue.ofString(s), 7, s.getBytes(StandardCharsets.UTF_8))),
            Arbitraries.longs().between(0, YEAR_2100_MILLIS)
                       .map(ms -> new TypedValue(HeaderValue.ofTimestamp(Instant.ofEpochMilli(ms)), 8,
                                                 ByteBuffer.allocate(8).putLong(ms).array())),
            Combinators.combine(Arbitraries.longs(), Arbitraries.longs())
                       .as((msb, lsb) -> new TypedValue(HeaderValue.ofUuid(new UUID(msb, lsb)), 9,
                                                        ByteBuffer.allocate(16).putLong(msb).putLong(lsb).array())));
    }

    /**
     * {@code count} clock readings: a start (random, or up to a few seconds before a UTC midnight, including exactly
     * at it) followed by steps of zero, sub-millisecond, millisecond-to-second, minute-to-hour and small backward
     * (clock skew) durations.
     */
    private static Arbitrary<List<Instant>> clockReadings(int count) {
        Arbitrary<Long> nanos = Arbitraries.longs().between(0, 999_999);
        Arbitrary<Instant> anywhere = Combinators.combine(
            Arbitraries.longs().between(YEAR_2000_MILLIS, YEAR_2100_MILLIS - MILLIS_PER_DAY), nanos)
                                                 .as((ms, n) -> Instant.ofEpochMilli(ms).plusNanos(n));
        Arbitrary<Instant> beforeMidnight = Combinators.combine(
            Arbitraries.longs().between(YEAR_2000_MILLIS / MILLIS_PER_DAY + 1, YEAR_2100_MILLIS / MILLIS_PER_DAY - 1),
            Arbitraries.frequencyOf(Tuple.of(1, Arbitraries.just(0L)),
                                    Tuple.of(4, Arbitraries.longs().between(1, 6_000))),
            nanos).as((day, offsetMillis, n) -> Instant.ofEpochMilli(day * MILLIS_PER_DAY - offsetMillis).plusNanos(n));
        Arbitrary<Instant> start = Arbitraries.frequencyOf(Tuple.of(3, anywhere), Tuple.of(2, beforeMidnight));
        Arbitrary<Long> stepNanos = Arbitraries.frequencyOf(
            Tuple.of(2, Arbitraries.just(0L)),
            Tuple.of(2, Arbitraries.longs().between(1, 999_999)),
            Tuple.of(6, Arbitraries.longs().between(1, 2_000).map(ms -> ms * 1_000_000)),
            Tuple.of(1, Arbitraries.longs().between(60, 3 * 3_600).map(s -> s * 1_000_000_000)),
            Tuple.of(1, Arbitraries.longs().between(1, 1_500).map(ms -> -ms * 1_000_000)));
        return Combinators.combine(start, stepNanos.list().ofSize(count - 1)).as((first, steps) -> {
            List<Instant> readings = new ArrayList<>(count);
            Instant time = first;
            readings.add(time);
            for (long step : steps) {
                time = time.plusNanos(step);
                readings.add(time);
            }
            return Collections.unmodifiableList(readings);
        });
    }

    private static void collectStatistics(int messages, List<Instant> clockReadings) {
        String frames = messages == 0 ? "closing frame only" : messages <= 3 ? "1-3 messages" : "4+ messages";
        Statistics.label("frames").collect(frames);
        Statistics.label("frames").coverage(c -> {
            c.check("closing frame only").percentage(p -> p >= 3);
            c.check("1-3 messages").percentage(p -> p >= 10);
            c.check("4+ messages").percentage(p -> p >= 30);
        });
        boolean crossesMidnight = expectedKeyRequests(clockReadings).size() > 1;
        Statistics.label("crosses a UTC midnight").collect(crossesMidnight);
        Statistics.label("crosses a UTC midnight").coverage(c -> c.check(true).percentage(p -> p >= 10));
        boolean subMillisecond = clockReadings.stream().anyMatch(t -> t.getNano() % 1_000_000 != 0);
        Statistics.label("sub-millisecond clock reading").collect(subMillisecond);
        Statistics.label("sub-millisecond clock reading").coverage(c -> c.check(true).percentage(p -> p >= 50));
    }

    // Test doubles and helpers.

    private static EventStreamSigner signer(BodyStream stream, KeyRequests keys) {
        return EventStreamSigner.sigV4(HEX.formatHex(stream.seed()), stream.identity().region(),
                                       stream.identity().service(), keys, new ReadingClock(stream.clockReadings()));
    }

    private static void writeAll(EventStream<SerializableStruct> writer, List<SerializableStruct> events) {
        for (SerializableStruct e : events) {
            writer.asWriter().write(e);
        }
        writer.close();
    }

    private static byte[] bytes(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return bytes;
    }

    private static SerializableStruct event(byte[] payload) {
        Map<String, HeaderValue> headers = new LinkedHashMap<>();
        headers.put(EventStreamCodec.MESSAGE_TYPE, HeaderValue.ofString("event"));
        headers.put(EventStreamCodec.EVENT_TYPE, HeaderValue.ofString("data"));
        headers.put(EventStreamCodec.CONTENT_TYPE, HeaderValue.ofString("application/octet-stream"));
        return CODEC.decodeEvent(new Message(headers, payload), null);
    }

    private static EventStreamCodec codec() {
        Model model = Model.assembler().addUnparsedModel("events.smithy", String.join("\n",
            "$version: \"2\"",
            "namespace test.chain",
            "@streaming",
            "union Events { data: DataEvent }",
            "structure DataEvent {",
            "    @eventPayload",
            "    payload: Blob",
            "}")).assemble().unwrap();
        return EventStreamCodec.of(new WireContext(ShapeId.from("aws.protocols#restJson1"),
                                                   ShapeId.from("test.chain#PublishEvents")),
                                   new SchemaConverter(model).getSchema(
                                       model.expectShape(ShapeId.from("test.chain#Events"))),
                                   EventPayloadCodec.of(JsonCodec.builder().build(), "application/json"), null);
    }

    /**
     * The identity seam ({@code auth-aws}'s role): derives each date's signing key from the generated secret and
     * records the requested scope dates.
     */
    private static final class KeyRequests implements SigningKeyProvider {
        private final Identity identity;
        private final List<LocalDate> requested = Collections.synchronizedList(new ArrayList<>());

        KeyRequests(Identity identity) {
            this.identity = identity;
        }

        @Override
        public byte[] signingKey(LocalDate scopeDate) {
            requested.add(scopeDate);
            return Sigv4Oracle.signingKey(identity, scopeDate);
        }

        List<LocalDate> requested() {
            return List.copyOf(requested);
        }
    }

    /** A clock that returns the generated readings in order and fails if read more often than there are frames. */
    private static final class ReadingClock extends Clock {
        private final List<Instant> readings;
        private final AtomicInteger next = new AtomicInteger();

        ReadingClock(List<Instant> readings) {
            this.readings = readings;
        }

        int reads() {
            return next.get();
        }

        @Override
        public Instant instant() {
            int index = next.getAndIncrement();
            if (index >= readings.size()) {
                throw new AssertionError("the signer read the clock more than once per frame");
            }
            return readings.get(index);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }
    }

    /** Records chunks and the terminal signal; requests nothing by itself. */
    private static final class Collector implements Subscriber<ByteBuffer> {
        private final ByteArrayOutputStream received = new ByteArrayOutputStream();
        private final AtomicReference<Throwable> error = new AtomicReference<>();
        private final CountDownLatch completed = new CountDownLatch(1);
        private volatile Subscription subscription;

        @Override
        public void onSubscribe(Subscription s) {
            subscription = s;
        }

        @Override
        public synchronized void onNext(ByteBuffer item) {
            received.writeBytes(bytes(item));
        }

        @Override
        public void onError(Throwable throwable) {
            error.set(throwable);
            completed.countDown();
        }

        @Override
        public void onComplete() {
            completed.countDown();
        }

        synchronized byte[] allBytes() {
            return received.toByteArray();
        }
    }
}
