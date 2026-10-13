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

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Locale;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Formats and parses the HTTP-date (IMF-fixdate) format, with a fast path for well-formed values.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.serde.HttpDateFormat}.
 */
@SdkInternalApi
final class HttpDateFormat {

    /**
     * Length of a well-formed IMF-fixdate string.
     */
    static final int LENGTH = 29;

    /**
     * Concatenated three-letter day-of-week abbreviations, indexed by {@code (DayOfWeek.getValue() - 1) * 3}.
     * Order matches {@link java.time.DayOfWeek}: MONDAY to SUNDAY.
     */
    private static final char[] DAY_NAMES = "MonTueWedThuFriSatSun".toCharArray();

    /**
     * Concatenated three-letter month abbreviations, indexed by {@code (monthValue - 1) * 3}, January to December.
     */
    private static final char[] MONTH_NAMES = "JanFebMarAprMayJunJulAugSepOctNovDec".toCharArray();

    /**
     * Slow-path formatter and parser, used when the input is not a strict IMF-fixdate.
     */
    private static final DateTimeFormatter FALLBACK = DateTimeFormatter
        .ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
        .withZone(ZoneOffset.UTC)
        .withLocale(Locale.US);

    private static final long INSTANT_SECONDS_MIN = ChronoField.INSTANT_SECONDS.range().getMinimum();
    private static final long INSTANT_SECONDS_MAX = ChronoField.INSTANT_SECONDS.range().getMaximum();

    private HttpDateFormat() {
    }

    /**
     * Formats an epoch second as an HTTP-date. The year must be representable (see {@link #isYearRepresentable(long)}).
     *
     * @param epochSecond seconds since the epoch (UTC)
     * @return the IMF-fixdate string, always 29 characters long
     */
    static String format(long epochSecond) {
        LocalDateTime ldt = LocalDateTime.ofEpochSecond(epochSecond, 0, ZoneOffset.UTC);

        char[] buf = new char[LENGTH];
        int dowIdx = (ldt.getDayOfWeek().getValue() - 1) * 3;
        buf[0] = DAY_NAMES[dowIdx];
        buf[1] = DAY_NAMES[dowIdx + 1];
        buf[2] = DAY_NAMES[dowIdx + 2];
        buf[3] = ',';
        buf[4] = ' ';
        int day = ldt.getDayOfMonth();
        buf[5] = (char) ('0' + day / 10);
        buf[6] = (char) ('0' + day % 10);
        buf[7] = ' ';
        int monIdx = (ldt.getMonthValue() - 1) * 3;
        buf[8] = MONTH_NAMES[monIdx];
        buf[9] = MONTH_NAMES[monIdx + 1];
        buf[10] = MONTH_NAMES[monIdx + 2];
        buf[11] = ' ';
        int year = ldt.getYear();
        buf[12] = (char) ('0' + (year / 1000) % 10);
        buf[13] = (char) ('0' + (year / 100) % 10);
        buf[14] = (char) ('0' + (year / 10) % 10);
        buf[15] = (char) ('0' + year % 10);
        buf[16] = ' ';
        int hour = ldt.getHour();
        buf[17] = (char) ('0' + hour / 10);
        buf[18] = (char) ('0' + hour % 10);
        buf[19] = ':';
        int minute = ldt.getMinute();
        buf[20] = (char) ('0' + minute / 10);
        buf[21] = (char) ('0' + minute % 10);
        buf[22] = ':';
        int second = ldt.getSecond();
        buf[23] = (char) ('0' + second / 10);
        buf[24] = (char) ('0' + second % 10);
        buf[25] = ' ';
        buf[26] = 'G';
        buf[27] = 'M';
        buf[28] = 'T';
        return new String(buf);
    }

    /**
     * Formats an instant with the JDK fallback formatter.
     */
    static String formatFallback(Instant value) {
        return FALLBACK.format(value);
    }

    /**
     * Parses an HTTP-date.
     *
     * <p>Strict IMF-fixdate values take the fast path. Other inputs (different lengths, lowercase day or month names,
     * missing GMT, and so on) go to the JDK {@link DateTimeFormatter}, so behavior matches the previous implementation.
     * The day-of-week field is not checked against the date.
     *
     * @param value the input
     * @return the parsed instant
     * @throws DateTimeParseException if the input cannot be parsed by either path
     */
    static Instant parse(String value) {
        if (value.length() != LENGTH || !validateLayout(value)) {
            return FALLBACK.parse(value, Instant::from);
        }

        // The layout matched, so malformed fields throw instead of going to the fallback (which is too lenient about
        // values like hour 24). Throw the exception type the fallback would, for consistent handling by callers.
        try {
            int day = parseTwoDigit(value, 5);
            int month = parseMonthName(value, 8);
            int year = parseYear(value, 12);
            int hour = parseTwoDigit(value, 17);
            int minute = parseTwoDigit(value, 20);
            int second = parseTwoDigit(value, 23);
            LocalDate date = LocalDate.of(year, month, day);
            LocalTime time = LocalTime.of(hour, minute, second);
            return date.atTime(time).toInstant(ZoneOffset.UTC);
        } catch (IllegalArgumentException | DateTimeException e) {
            throw new DateTimeParseException("Invalid HTTP-date: " + value, value, 0, e);
        }
    }

    /**
     * Checks that the fixed-position separators of an IMF-fixdate are present.
     */
    private static boolean validateLayout(String s) {
        return s.charAt(3) == ','
               && s.charAt(4) == ' '
               && s.charAt(7) == ' '
               && s.charAt(11) == ' '
               && s.charAt(16) == ' '
               && s.charAt(19) == ':'
               && s.charAt(22) == ':'
               && s.charAt(25) == ' '
               && s.charAt(26) == 'G'
               && s.charAt(27) == 'M'
               && s.charAt(28) == 'T';
    }

    private static int parseTwoDigit(String s, int idx) {
        char a = s.charAt(idx);
        char b = s.charAt(idx + 1);
        if (a < '0' || a > '9' || b < '0' || b > '9') {
            throw new NumberFormatException();
        }
        return (a - '0') * 10 + (b - '0');
    }

    private static int parseYear(String s, int idx) {
        int year = 0;
        for (int i = 0; i < 4; i++) {
            char c = s.charAt(idx + i);
            if (c < '0' || c > '9') {
                throw new NumberFormatException();
            }
            year = year * 10 + (c - '0');
        }
        return year;
    }

    /**
     * Maps a three-letter month abbreviation to its month number, 1 to 12.
     */
    private static int parseMonthName(String s, int idx) {
        char a = s.charAt(idx);
        char b = s.charAt(idx + 1);
        char c = s.charAt(idx + 2);
        switch (a) {
            case 'J':
                if (b == 'a' && c == 'n') {
                    return 1;
                } else if (b == 'u' && c == 'n') {
                    return 6;
                } else if (b == 'u' && c == 'l') {
                    return 7;
                }
                break;
            case 'F':
                if (b == 'e' && c == 'b') {
                    return 2;
                }
                break;
            case 'M':
                if (b == 'a' && c == 'r') {
                    return 3;
                } else if (b == 'a' && c == 'y') {
                    return 5;
                }
                break;
            case 'A':
                if (b == 'p' && c == 'r') {
                    return 4;
                } else if (b == 'u' && c == 'g') {
                    return 8;
                }
                break;
            case 'S':
                if (b == 'e' && c == 'p') {
                    return 9;
                }
                break;
            case 'O':
                if (b == 'c' && c == 't') {
                    return 10;
                }
                break;
            case 'N':
                if (b == 'o' && c == 'v') {
                    return 11;
                }
                break;
            case 'D':
                if (b == 'e' && c == 'c') {
                    return 12;
                }
                break;
            default:
                break;
        }
        throw new IllegalArgumentException("Unknown month abbreviation: " + s.substring(idx, idx + 3));
    }

    /**
     * Returns whether an epoch second falls in years 0001 to 9999, which the fast formatter writes with four digits.
     * Values outside that range use the JDK formatter, which either succeeds or fails uniformly.
     */
    static boolean isYearRepresentable(long epochSecond) {
        // 0001-01-01T00:00:00Z = -62135596800
        // 9999-12-31T23:59:59Z =  253402300799
        return epochSecond >= -62135596800L
               && epochSecond <= 253402300799L
               && epochSecond >= INSTANT_SECONDS_MIN
               && epochSecond <= INSTANT_SECONDS_MAX;
    }
}
