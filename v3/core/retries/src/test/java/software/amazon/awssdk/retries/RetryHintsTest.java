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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RetryHintsTest {
    private static final Duration BACKOFF = Duration.ofMillis(400);

    @Test
    void validHintsParseAsMilliseconds() {
        assertEquals(Optional.of(Duration.ZERO), RetryHints.parseRetryAfter("0"));
        assertEquals(Optional.of(Duration.ofMillis(1500)), RetryHints.parseRetryAfter("1500"));
        assertEquals(Optional.of(Duration.ofMillis(7)), RetryHints.parseRetryAfter("007"));
        assertEquals(Optional.of(Duration.ofMillis(999_999_999_999_999_999L)),
                     RetryHints.parseRetryAfter("999999999999999999"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " 10", "10 ", "-1", "+1", "1.5", "1e3", "10ms", "abc", "１０",
                            "1000000000000000000"})
    void invalidHintsAreIgnored(String raw) {
        assertEquals(Optional.empty(), RetryHints.parseRetryAfter(raw));
        assertEquals(BACKOFF, RetryHints.boundedDelay(BACKOFF, raw));
    }

    @Test
    void absentHintUsesTheStrategyDelay() {
        assertEquals(Optional.empty(), RetryHints.parseRetryAfter(null));
        assertEquals(BACKOFF, RetryHints.boundedDelay(BACKOFF, null));
    }

    @Test
    void inBoundHintIsUsed() {
        assertEquals(BACKOFF, RetryHints.boundedDelay(BACKOFF, "400"));
        assertEquals(Duration.ofMillis(2_000), RetryHints.boundedDelay(BACKOFF, "2000"));
        assertEquals(Duration.ofMillis(5_400), RetryHints.boundedDelay(BACKOFF, "5400"));
    }

    @Test
    void aboveBoundHintIsCapped() {
        assertEquals(Duration.ofMillis(5_400), RetryHints.boundedDelay(BACKOFF, "5401"));
        assertEquals(Duration.ofMillis(5_400), RetryHints.boundedDelay(BACKOFF, "999999999999999999"));
        assertEquals(Duration.ofSeconds(1), RetryHints.boundedDelay(Duration.ZERO, Optional.of(Duration.ofHours(1)),
                                                                     Duration.ofSeconds(1)));
    }

    @Test
    void hintNeverShortensTheBackoff() {
        assertEquals(BACKOFF, RetryHints.boundedDelay(BACKOFF, "0"));
        assertEquals(BACKOFF, RetryHints.boundedDelay(BACKOFF, "399"));
    }

    @Test
    void negativeInputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> RetryHints.boundedDelay(Duration.ofMillis(-1), "1"));
        assertThrows(IllegalArgumentException.class,
                     () -> RetryHints.boundedDelay(BACKOFF, Optional.empty(), Duration.ofMillis(-1)));
    }
}
