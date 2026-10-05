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

package software.amazon.awssdk.retries;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Parsing and bounding of service retry-delay hints (Requirements 13.12-13.14).
 *
 * <p>The supported hint is the {@value #X_AMZ_RETRY_AFTER} response header: a whole number of milliseconds written as
 * 1 to {@value #MAX_HINT_DIGITS} ASCII digits with no sign, whitespace, fraction or unit. Any other value is invalid
 * and ignored.
 *
 * <p>The standard and adaptive modes bound a valid hint by the attempt's computed backoff {@code b} (the strategy
 * delay calculation) and the mode's {@link #HINT_EXTENSION hint extension} {@code e} of 5 seconds:
 * <table>
 *   <caption>Attempt delay by hint</caption>
 *   <tr><th>Hint {@code h}</th><th>Attempt delay</th></tr>
 *   <tr><td>absent or invalid</td><td>{@code b} (hint ignored)</td></tr>
 *   <tr><td>{@code b <= h <= b + e} (within the bound)</td><td>{@code h} (hint used)</td></tr>
 *   <tr><td>{@code h > b + e} (above the bound)</td><td>{@code b + e} (hint capped)</td></tr>
 *   <tr><td>{@code h < b}</td><td>{@code b} (a hint never shortens the SDK's own backoff)</td></tr>
 * </table>
 * That is, {@code delay = min(max(h, b), b + e)}.
 */
public final class RetryHints {
    /** Name of the supported retry-delay hint header. */
    public static final String X_AMZ_RETRY_AFTER = "x-amz-retry-after";

    /** How far above the computed backoff a valid hint may extend the attempt delay. */
    public static final Duration HINT_EXTENSION = Duration.ofSeconds(5);

    /** Maximum number of digits of a valid hint (so that it always fits a {@code long} of milliseconds). */
    static final int MAX_HINT_DIGITS = 18;

    private RetryHints() {
    }

    /** The delay of a syntactically valid {@value #X_AMZ_RETRY_AFTER} value, or empty if {@code value} is invalid. */
    public static Optional<Duration> parseRetryAfter(String value) {
        if (value == null || value.isEmpty() || value.length() > MAX_HINT_DIGITS) {
            return Optional.empty();
        }
        long millis = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < '0' || c > '9') {
                return Optional.empty();
            }
            millis = millis * 10 + (c - '0');
        }
        return Optional.of(Duration.ofMillis(millis));
    }

    /**
     * The attempt delay for the computed backoff {@code backoff} and the raw hint {@code rawHint} (may be null), per
     * the table above with the default {@link #HINT_EXTENSION}.
     */
    public static Duration boundedDelay(Duration backoff, String rawHint) {
        return boundedDelay(backoff, parseRetryAfter(rawHint), HINT_EXTENSION);
    }

    /** The attempt delay for {@code backoff}, a parsed {@code hint} and the mode's {@code extension}. */
    public static Duration boundedDelay(Duration backoff, Optional<Duration> hint, Duration extension) {
        Delays.requireNonNegative(backoff, "backoff");
        Delays.requireNonNegative(extension, "extension");
        Objects.requireNonNull(hint, "hint");
        if (hint.isEmpty()) {
            return backoff;
        }
        Duration h = hint.get();
        if (h.compareTo(backoff) < 0) {
            return backoff;
        }
        Duration upper = backoff.plus(extension);
        return h.compareTo(upper) > 0 ? upper : h;
    }
}
