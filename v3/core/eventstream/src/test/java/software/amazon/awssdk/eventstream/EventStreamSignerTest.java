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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/**
 * Event stream signature chaining ({@link EventStreamSigner}, {@link SigV4FrameSignatureAlgorithm}): the V2 SigV4
 * event stream vector, envelope structure, chain state against an independent verifier, per-date keys, fail-stop
 * errors and closing.
 */
class EventStreamSignerTest {
    private static final HexFormat HEX = HexFormat.of();
    private static final String SECRET = "secret";
    private static final String REGION = "us-east-1";
    private static final String SERVICE = "demo";

    /** The instants of the V2 SigV4DataFramePublisherTest vector; the first one signed the request. */
    private static final List<Instant> V2_INSTANTS = List.of(
        OffsetDateTime.of(1981, 1, 16, 6, 30, 1, 0, ZoneOffset.UTC).toInstant(),
        OffsetDateTime.of(1981, 1, 16, 6, 30, 2, 0, ZoneOffset.UTC).toInstant(),
        OffsetDateTime.of(1981, 1, 16, 6, 30, 3, 0, ZoneOffset.UTC).toInstant(),
        OffsetDateTime.of(1981, 1, 16, 6, 30, 4, 0, ZoneOffset.UTC).toInstant());
    private static final String V2_SEED = "e1d8e8c8815e60969f2a34765c9a15945ffc0badbaa4b7e3b163ea19131e949b";
    private static final List<String> V2_SIGNATURES = List.of(
        "7aabf85b765e6a4d0d500b6e968657b14726fa3e1eb7e839302728ffd77629a5",
        "f72aa9642f571d24a6e1ae42f10f073ad9448d8a028b6bcd82da081335adda02",
        "632af120435b57ec241d8bfbb12e496dfd5e2730a1a02ac0ab6eaa230ae02e9a",
        "c6f679ddb3af68f5e82f0cf6761244cb2338cf11e7d01a24130aea1b7c17e53e");

    @Test
    void sigV4ChainMatchesTheV2EventStreamVector() {
        EventStreamSigner signer = EventStreamSigner.sigV4(V2_SEED, REGION, SERVICE, EventStreamSignerTest::signingKey,
                                                           clock(V2_INSTANTS));
        List<Message> frames = new ArrayList<>();
        for (String payload : List.of("A", "B", "C")) {
            frames.add(signer.signPayload(payload.getBytes(StandardCharsets.UTF_8)));
        }
        frames.add(signer.closingFrame());

        for (int i = 0; i < frames.size(); i++) {
            Message frame = frames.get(i);
            assertEquals(List.of(EventStreamSigner.DATE_HEADER, EventStreamSigner.CHUNK_SIGNATURE_HEADER),
                         List.copyOf(frame.headers().keySet()), "the :date header, then the signature");
            assertEquals(V2_INSTANTS.get(i), frame.header(EventStreamSigner.DATE_HEADER).asTimestamp());
            assertEquals(V2_SIGNATURES.get(i),
                         HEX.formatHex(frame.header(EventStreamSigner.CHUNK_SIGNATURE_HEADER).asByteArray()));
        }
        assertEquals(0, frames.get(3).payloadLength(), "the closing frame is empty");
        assertEquals(V2_SIGNATURES.get(3), HEX.formatHex(signer.priorSignature()));
        assertEquals(4, signer.signedFrames());
        assertTrue(signer.isClosed());
    }

    @Test
    void envelopesCarryTheEncodedMessageAndVerifyAgainstThePriorSignature() {
        byte[] seed = HEX.parseHex(V2_SEED);
        Instant start = Instant.parse("2024-05-01T12:00:00.123456Z");
        List<Instant> times = List.of(start, start.plusMillis(1500), start.plusSeconds(90), start.plusSeconds(91));
        EventStreamSigner signer = EventStreamSigner.sigV4(V2_SEED, REGION, SERVICE, EventStreamSignerTest::signingKey,
                                                           clock(times));
        List<Message> messages = List.of(
            message("one", "{\"a\":1}"),
            message("two", ""),
            new Message(Map.of(":message-type", HeaderValue.ofString("event"),
                               "flag", HeaderValue.ofBoolean(true)), new byte[] {0, 1, 2, (byte) 255}));

        byte[] prior = seed;
        MessageDecoder inner = new MessageDecoder();
        for (int i = 0; i <= messages.size(); i++) {
            boolean closing = i == messages.size();
            Message envelope = closing ? signer.closingFrame() : signer.transform(messages.get(i));
            if (closing) {
                assertEquals(0, envelope.payloadLength());
            } else {
                assertEquals(messages.get(i), inner.decodeNext(envelope.payload()), "the payload is the message");
            }
            Instant date = envelope.header(EventStreamSigner.DATE_HEADER).asTimestamp();
            assertEquals(times.get(i).toEpochMilli(), date.toEpochMilli(), "millisecond signing time");
            byte[] signature = envelope.header(EventStreamSigner.CHUNK_SIGNATURE_HEADER).asByteArray();
            assertArrayEquals(expectedSignature(prior, date, envelope.payloadBytes()), signature, "frame " + i);
            assertArrayEquals(signature, signer.priorSignature(), "the signature is the next prior signature");
            prior = signature;
            // The envelope survives framing unchanged.
            assertEquals(envelope, new MessageDecoder().decodeNext(MessageEncoder.encode(envelope)));
        }
        assertEquals(4, signer.signedFrames());
    }

    @Test
    void closedSignerRejectsFurtherFrames() {
        EventStreamSigner signer = EventStreamSigner.sigV4(V2_SEED, REGION, SERVICE, EventStreamSignerTest::signingKey,
                                                           Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        signer.transform(message("one", "x"));
        signer.closingFrame();
        assertThrows(IllegalStateException.class, () -> signer.transform(message("two", "y")));
        assertThrows(IllegalStateException.class, signer::closingFrame);
        assertEquals(2, signer.signedFrames());
    }

    @Test
    void signingKeyIsRequestedOncePerScopeDateIncludingAcrossMidnight() {
        Instant beforeMidnight = Instant.parse("2024-12-31T23:59:59Z");
        List<LocalDate> requested = new ArrayList<>();
        SigningKeyProvider keys = date -> {
            requested.add(date);
            return signingKey(date);
        };
        List<Instant> times = List.of(beforeMidnight, beforeMidnight, beforeMidnight.plusSeconds(1),
                                      beforeMidnight.plusSeconds(2));
        EventStreamSigner signer = EventStreamSigner.sigV4(V2_SEED, REGION, SERVICE, keys, clock(times));
        byte[] prior = signer.priorSignature();
        for (int i = 0; i < 4; i++) {
            Message frame = signer.transform(message("e" + i, "p"));
            byte[] signature = frame.header(EventStreamSigner.CHUNK_SIGNATURE_HEADER).asByteArray();
            assertArrayEquals(expectedSignature(prior, times.get(i), frame.payloadBytes()), signature);
            prior = signature;
        }
        assertEquals(List.of(LocalDate.of(2024, 12, 31), LocalDate.of(2025, 1, 1)), requested);
        assertEquals("20250101/us-east-1/demo/aws4_request",
                     ((SigV4FrameSignatureAlgorithm) signer.algorithm()).scope(times.get(3)));
    }

    @Test
    void aSigningFailureFailsTheChainPermanentlyWithoutLeakingSecrets() {
        RuntimeException cause = new IllegalStateException("The credentials expired");
        int[] calls = {0};
        SigningKeyProvider keys = date -> {
            if (calls[0]++ > 0) {
                throw cause;
            }
            return signingKey(date);
        };
        List<Instant> times = List.of(Instant.parse("2024-01-01T00:00:00Z"), Instant.parse("2024-01-02T00:00:00Z"),
                                      Instant.parse("2024-01-02T00:00:01Z"));
        EventStreamSigner signer = EventStreamSigner.sigV4(V2_SEED, REGION, SERVICE, keys, clock(times));
        signer.transform(message("one", SECRET));
        byte[] prior = signer.priorSignature();

        EventStreamSigningException e = assertThrows(EventStreamSigningException.class,
                                                     () -> signer.transform(message("two", SECRET)));
        assertSame(cause, e.getCause());
        assertFalse(e.getMessage().contains(SECRET));
        assertFalse(e.getMessage().contains(HEX.formatHex(prior)));
        assertArrayEquals(prior, signer.priorSignature(), "the chain did not advance");
        assertEquals(1, signer.signedFrames());
        assertThrows(IllegalStateException.class, () -> signer.transform(message("three", "z")));
        assertThrows(IllegalStateException.class, signer::closingFrame);
        assertFalse(signer.isClosed());
    }

    @Test
    void invalidSignaturesAndOversizedFramesFailTheChain() {
        EventStreamSigner empty = EventStreamSigner.create(new byte[] {1}, (prior, time, headers, payload) -> new byte[0],
                                                           Clock.systemUTC());
        assertThrows(EventStreamSigningException.class, () -> empty.transform(message("one", "x")));
        assertThrows(IllegalStateException.class, () -> empty.transform(message("one", "x")));

        EventStreamSigner oversized = EventStreamSigner.create(new byte[] {1}, (prior, time, headers, payload) -> prior,
                                                               Clock.systemUTC());
        Message large = new Message(Map.of(), new byte[MessageEncoder.MAX_PAYLOAD_LENGTH]);
        EventStreamSigningException e = assertThrows(EventStreamSigningException.class,
                                                     () -> oversized.transform(large));
        assertTrue(e.getMessage().contains("exceeds the signed-frame payload limit"), e.getMessage());
        assertEquals(0, oversized.signedFrames());
    }

    @Test
    void algorithmReceivesACopyOfTheChainStateAndTheEncodedDateHeader() {
        List<byte[]> seenPriors = new ArrayList<>();
        Instant time = Instant.parse("2024-01-01T00:00:00.5Z");
        LinkedHashMap<String, HeaderValue> date = new LinkedHashMap<>();
        date.put(EventStreamSigner.DATE_HEADER, HeaderValue.ofTimestamp(time));
        byte[] expectedHeaders = MessageEncoder.encodeHeaders(date);
        FrameSignatureAlgorithm recording = (prior, signingTime, headers, payload) -> {
            seenPriors.add(prior.clone());
            assertEquals(time, signingTime);
            assertArrayEquals(expectedHeaders, headers);
            assertTrue(payload.isReadOnly());
            byte[] signature = {(byte) seenPriors.size()};
            prior[0] = 42; // Mutating the argument must not corrupt the chain.
            return signature;
        };
        EventStreamSigner signer = EventStreamSigner.create(new byte[] {7, 7}, recording,
                                                            Clock.fixed(time, ZoneOffset.UTC));
        signer.transform(message("one", "x"));
        assertArrayEquals(new byte[] {1}, signer.priorSignature());
        signer.transform(message("two", "y"));
        assertArrayEquals(new byte[] {7, 7}, seenPriors.get(0), "the seed starts the chain");
        assertArrayEquals(new byte[] {1}, seenPriors.get(1), "the first signature, unaffected by the mutation");
        assertArrayEquals(new byte[] {2}, signer.priorSignature());
    }

    @Test
    void sigV4WithInstantSupplierReadsOncePerFrameAndMatchesTheClockOverload() {
        Instant start = Instant.parse("2024-05-01T12:00:00.123456Z");
        List<Instant> times = List.of(start, start.plusNanos(1_999_999), start.plusSeconds(90), start.plusSeconds(91));
        Iterator<Instant> next = times.iterator();
        int[] reads = new int[1];
        Supplier<Instant> time = () -> {
            reads[0]++;
            return next.next();
        };
        EventStreamSigner fromSupplier = EventStreamSigner.sigV4(V2_SEED, REGION, SERVICE,
                                                                 EventStreamSignerTest::signingKey, time);
        EventStreamSigner fromClock = EventStreamSigner.sigV4(V2_SEED, REGION, SERVICE,
                                                              EventStreamSignerTest::signingKey, clock(times));
        for (int i = 0; i < times.size(); i++) {
            boolean closing = i == times.size() - 1;
            Message viaSupplier = closing ? fromSupplier.closingFrame() : fromSupplier.transform(message("e", "p" + i));
            Message viaClock = closing ? fromClock.closingFrame() : fromClock.transform(message("e", "p" + i));
            assertEquals(i + 1, reads[0], "the time is read once per frame");
            assertEquals(times.get(i).truncatedTo(ChronoUnit.MILLIS),
                         viaSupplier.header(EventStreamSigner.DATE_HEADER).asTimestamp(), "millisecond signing time");
            assertEquals(viaClock, viaSupplier, "frame " + i + " is identical to the clock overload's");
        }
        assertTrue(fromSupplier.isClosed());

        Iterator<Instant> v2 = V2_INSTANTS.iterator();
        EventStreamSigner vector = EventStreamSigner.sigV4(V2_SEED, REGION, SERVICE, EventStreamSignerTest::signingKey,
                                                           (Supplier<Instant>) v2::next);
        for (String payload : List.of("A", "B", "C")) {
            vector.signPayload(payload.getBytes(StandardCharsets.UTF_8));
        }
        vector.closingFrame();
        assertEquals(V2_SIGNATURES.get(3), HEX.formatHex(vector.priorSignature()), "the V2 vector");

        EventStreamSigner nullTime = EventStreamSigner.sigV4(V2_SEED, REGION, SERVICE,
                                                             EventStreamSignerTest::signingKey,
                                                             (Supplier<Instant>) () -> null);
        assertThrows(EventStreamSigningException.class, nullTime::closingFrame);
        assertThrows(NullPointerException.class, () -> EventStreamSigner.sigV4(V2_SEED, REGION, SERVICE,
                                                                               EventStreamSignerTest::signingKey,
                                                                               (Supplier<Instant>) null));
    }

    @Test
    void rejectsInvalidSeedsAndScopes() {
        SigningKeyProvider keys = EventStreamSignerTest::signingKey;
        Clock clock = Clock.systemUTC();
        assertThrows(IllegalArgumentException.class, () -> EventStreamSigner.sigV4("", REGION, SERVICE, keys, clock));
        assertThrows(IllegalArgumentException.class, () -> EventStreamSigner.sigV4("xyz", REGION, SERVICE, keys, clock));
        assertThrows(IllegalArgumentException.class, () -> EventStreamSigner.sigV4(V2_SEED, "us/east", SERVICE, keys,
                                                                                   clock));
        assertThrows(IllegalArgumentException.class, () -> EventStreamSigner.sigV4(V2_SEED, REGION, "de\nmo", keys,
                                                                                   clock));
        assertThrows(IllegalArgumentException.class, () -> EventStreamSigner.sigV4(V2_SEED, REGION, "", keys, clock));
    }

    @Test
    void encodeHeadersIsTheHeaderSectionOfTheEncodedMessage() {
        LinkedHashMap<String, HeaderValue> headers = new LinkedHashMap<>();
        headers.put(":date", HeaderValue.ofTimestamp(Instant.ofEpochMilli(1234)));
        headers.put("s", HeaderValue.ofString("value"));
        headers.put("b", HeaderValue.ofByteArray(new byte[] {1, 2, 3}));
        byte[] section = MessageEncoder.encodeHeaders(headers);
        ByteBuffer encoded = MessageEncoder.encode(new Message(headers, new byte[] {9}));
        assertEquals(section.length, encoded.getInt(4));
        byte[] fromMessage = new byte[section.length];
        encoded.get(12, fromMessage);
        assertArrayEquals(fromMessage, section);
        assertThrows(IllegalArgumentException.class, () -> MessageEncoder.encodeHeaders(Map.of("", HeaderValue.ofBoolean(true))));
    }

    private static Message message(String eventType, String payload) {
        LinkedHashMap<String, HeaderValue> headers = new LinkedHashMap<>();
        headers.put(":message-type", HeaderValue.ofString("event"));
        headers.put(":event-type", HeaderValue.ofString(eventType));
        return new Message(headers, payload.getBytes(StandardCharsets.UTF_8));
    }

    // Independent SigV4 oracle: key derivation and the event string-to-sign written out from the SigV4 specification.

    static byte[] signingKey(LocalDate date) {
        byte[] key = hmac(("AWS4" + SECRET).getBytes(StandardCharsets.UTF_8),
                          String.format("%04d%02d%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth()));
        key = hmac(key, REGION);
        key = hmac(key, SERVICE);
        return hmac(key, "aws4_request");
    }

    private static byte[] expectedSignature(byte[] prior, Instant time, byte[] payload) {
        OffsetDateTime utc = time.atOffset(ZoneOffset.UTC);
        String date = String.format("%04d%02d%02d", utc.getYear(), utc.getMonthValue(), utc.getDayOfMonth());
        String dateTime = date + String.format("T%02d%02d%02dZ", utc.getHour(), utc.getMinute(), utc.getSecond());
        // The encoded :date header: name length, name, type 8 (timestamp), big-endian epoch milliseconds.
        ByteBuffer dateHeader = ByteBuffer.allocate(1 + 5 + 1 + 8);
        dateHeader.put((byte) 5).put(":date".getBytes(StandardCharsets.US_ASCII)).put((byte) 8)
                  .putLong(time.toEpochMilli());
        String stringToSign = "AWS4-HMAC-SHA256-PAYLOAD\n" + dateTime + "\n" + date + "/" + REGION + "/" + SERVICE
                              + "/aws4_request\n" + HEX.formatHex(prior) + "\n"
                              + HEX.formatHex(sha256(dateHeader.array())) + "\n" + HEX.formatHex(sha256(payload));
        return hmac(signingKey(utc.toLocalDate()), stringToSign);
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

    /** A clock that returns {@code instants} in order. */
    private static Clock clock(List<Instant> instants) {
        Iterator<Instant> next = instants.iterator();
        return new Clock() {
            @Override
            public Instant instant() {
                return next.next();
            }

            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
