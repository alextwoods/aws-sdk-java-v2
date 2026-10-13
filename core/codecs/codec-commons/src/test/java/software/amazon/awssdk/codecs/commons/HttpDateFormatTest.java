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

package software.amazon.awssdk.codecs.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Port of smithy-java {@code software.amazon.smithy.java.core.serde.HttpDateFormatTest}.
 *
 * <p>The fast path is verified against the reference {@link DateTimeFormatter} for all valid IMF-fixdate inputs.
 */
public class HttpDateFormatTest {

    private static final DateTimeFormatter REFERENCE = DateTimeFormatter
        .ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
        .withZone(ZoneOffset.UTC)
        .withLocale(Locale.US);

    /**
     * Each case is (Instant, expected formatted string). The expected strings were captured from the reference
     * {@link DateTimeFormatter}, so a regression in either implementation is caught.
     */
    static Stream<Arguments> formatCases() {
        return Stream.of(
            // Epoch boundary
            Arguments.of(Instant.EPOCH, "Thu, 01 Jan 1970 00:00:00 GMT"),
            // RFC 7231 section 7.1.1.1 example
            Arguments.of(Instant.ofEpochSecond(784111777L), "Sun, 06 Nov 1994 08:49:37 GMT"),
            // Each day-of-week, pinned against the reference formatter.
            referenceCase("2024-01-01T00:00:00Z"),
            referenceCase("2024-01-02T00:00:00Z"),
            referenceCase("2024-01-03T00:00:00Z"),
            referenceCase("2024-01-04T00:00:00Z"),
            referenceCase("2024-01-05T00:00:00Z"),
            referenceCase("2024-01-06T00:00:00Z"),
            referenceCase("2024-01-07T00:00:00Z"),
            // Each month
            referenceCase("2024-02-29T12:34:56Z"),
            referenceCase("2024-03-15T00:00:00Z"),
            referenceCase("2024-04-30T23:59:59Z"),
            referenceCase("2024-05-15T01:23:45Z"),
            referenceCase("2024-06-15T01:23:45Z"),
            referenceCase("2024-07-15T01:23:45Z"),
            referenceCase("2024-08-15T01:23:45Z"),
            referenceCase("2024-09-15T01:23:45Z"),
            referenceCase("2024-10-15T01:23:45Z"),
            referenceCase("2024-11-15T01:23:45Z"),
            referenceCase("2024-12-15T01:23:45Z"),
            // Year padding at zero-padded boundaries
            referenceCase("0001-01-01T00:00:00Z"),
            referenceCase("0099-12-31T23:59:59Z"),
            referenceCase("0999-12-31T23:59:59Z"),
            referenceCase("9999-12-31T23:59:59Z"),
            // Leap and non-leap year February ends
            referenceCase("2000-02-29T00:00:00Z"),
            referenceCase("2100-02-28T00:00:00Z"),
            // Sub-second component is dropped (HTTP-date is whole-second precision)
            Arguments.of(Instant.ofEpochSecond(784111777L, 999_999_999L), "Sun, 06 Nov 1994 08:49:37 GMT"));
    }

    private static Arguments referenceCase(String iso) {
        Instant value = Instant.parse(iso);
        return Arguments.of(value, REFERENCE.format(value));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("formatCases")
    void formatProducesExpectedString(Instant input, String expected) {
        assertEquals(expected, HttpDateFormat.format(input.getEpochSecond()));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("formatCases")
    void formatMatchesReferenceFormatter(Instant input, String expected) {
        assertEquals(REFERENCE.format(input.truncatedTo(ChronoUnit.SECONDS)),
                     HttpDateFormat.format(input.getEpochSecond()));
    }

    @ParameterizedTest(name = "{1} -> {0}")
    @MethodSource("formatCases")
    void parseRoundTrips(Instant expected, String input) {
        // Drop sub-second precision since HTTP-date doesn't carry it.
        Instant truncated = expected.truncatedTo(ChronoUnit.SECONDS);
        assertEquals(truncated, HttpDateFormat.parse(input));
    }

    @ParameterizedTest(name = "random epoch second {0}")
    @MethodSource("randomEpochSeconds")
    void roundTripWithReferenceFormatter(long epoch) {
        String fast = HttpDateFormat.format(epoch);
        String reference = REFERENCE.format(Instant.ofEpochSecond(epoch));
        assertEquals(reference, fast);
        assertEquals(Instant.ofEpochSecond(epoch), HttpDateFormat.parse(fast));
    }

    /**
     * A deterministic stream of random epoch seconds in years 0001 through 9999.
     */
    static Stream<Long> randomEpochSeconds() {
        Random rng = new Random(0xC0FFEEL);
        long min = LocalDateTime.of(1, 1, 1, 0, 0, 0).toEpochSecond(ZoneOffset.UTC);
        long max = LocalDateTime.of(9999, 12, 31, 23, 59, 59).toEpochSecond(ZoneOffset.UTC);
        return Stream.generate(() -> min + (long) (rng.nextDouble() * (max - min))).limit(500);
    }

    /**
     * Inputs that the strict fast path rejects; the JDK fallback rejects them too.
     */
    static Stream<Arguments> fallbackCases() {
        return Stream.of(
            // Wrong-cased month: the JDK pattern is case-sensitive too.
            Arguments.of("Sun, 06 nov 1994 08:49:37 GMT", true),
            // Wrong length
            Arguments.of("Sun, 06 Nov 1994 8:49:37 GMT", true),
            Arguments.of("not a date", true),
            Arguments.of("", true));
    }

    @ParameterizedTest(name = "fallback ({0})")
    @MethodSource("fallbackCases")
    void fallbackHandlesMalformedInputs(String input, boolean shouldThrow) {
        if (shouldThrow) {
            assertThrows(DateTimeParseException.class, () -> HttpDateFormat.parse(input));
        } else {
            HttpDateFormat.parse(input);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Mon, 32 Jan 2024 00:00:00 GMT", // day out of range
        "Mon, 00 Jan 2024 00:00:00 GMT", // day out of range
        "Mon, 31 Feb 2024 00:00:00 GMT", // Feb 31 doesn't exist
        "Mon, 30 Feb 2024 00:00:00 GMT", // Feb 30 doesn't exist (leap year still 29)
        "Mon, 01 Foo 2024 00:00:00 GMT", // bad month name
        "Mon, 01 Jan 2024 24:00:00 GMT", // hour out of range
        "Mon, 01 Jan 2024 00:60:00 GMT", // minute out of range
        "Mon, 01 Jan 2024 00:00:60 GMT", // second out of range
        "Mon, 1A Jan 2024 00:00:00 GMT", // non-digit in day
    })
    void rejectsOutOfRangeOrMalformedDates(String input) {
        assertThrows(RuntimeException.class, () -> HttpDateFormat.parse(input));
    }

    @Test
    void permissiveAboutDayOfWeekField() {
        // Real HTTP-date producers occasionally get the day-of-week wrong; the value is accepted.
        Instant parsed = HttpDateFormat.parse("Mon, 06 Nov 1994 08:49:37 GMT");
        assertEquals(Instant.ofEpochSecond(784111777L), parsed);
    }

    @Test
    void honoursOutputFromTimestampFormatter() {
        TimestampFormatter f = TimestampFormatter.Prelude.HTTP_DATE;
        Instant value = Instant.ofEpochSecond(784111777L);
        assertEquals("Sun, 06 Nov 1994 08:49:37 GMT", f.writeString(value));
        assertEquals(value, f.readFromString("Sun, 06 Nov 1994 08:49:37 GMT", false));
    }

    @Test
    void preludeHttpDateIsCachedSingleton() {
        assertSame(TimestampFormatter.Prelude.HTTP_DATE, TimestampFormatter.Prelude.HTTP_DATE);
    }
}
