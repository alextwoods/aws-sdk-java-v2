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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class BackoffStrategyTest {
    private static final double ALMOST_ONE = Math.nextDown(1.0);

    @Test
    void exponentialCeilingDoublesUpToTheMaximum() {
        BackoffStrategy full = BackoffStrategy.exponentialDelay(Duration.ofMillis(50), Duration.ofSeconds(20),
                                                                () -> 0.5);
        assertEquals(Duration.ofMillis(25), full.computeDelay(1));
        assertEquals(Duration.ofMillis(50), full.computeDelay(2));
        assertEquals(Duration.ofMillis(100), full.computeDelay(3));
        assertEquals(Duration.ofSeconds(10), full.computeDelay(20));
        assertEquals(Duration.ofSeconds(10), full.computeDelay(Integer.MAX_VALUE));
    }

    @Test
    void jitterSpansZeroToTheCeiling() {
        Duration max = Duration.ofSeconds(20);
        BackoffStrategy low = BackoffStrategy.exponentialDelay(Duration.ofSeconds(1), max, () -> 0.0);
        BackoffStrategy high = BackoffStrategy.exponentialDelay(Duration.ofSeconds(1), max, () -> ALMOST_ONE);
        assertEquals(Duration.ZERO, low.computeDelay(3));
        assertTrue(high.computeDelay(3).compareTo(Duration.ofSeconds(4)) < 0);
        assertTrue(high.computeDelay(3).compareTo(Duration.ofMillis(3_999)) > 0);
        BackoffStrategy random = BackoffStrategy.exponentialDelay(Duration.ofMillis(50), max);
        for (int attempt = 1; attempt < 70; attempt++) {
            Duration d = random.computeDelay(attempt);
            assertTrue(!d.isNegative() && d.compareTo(max) < 0, d.toString());
        }
    }

    @Test
    void invalidInputsAreRejected() {
        BackoffStrategy strategy = BackoffStrategy.exponentialDelay(Duration.ofMillis(1), Duration.ofSeconds(1));
        assertThrows(IllegalArgumentException.class, () -> strategy.computeDelay(0));
        assertThrows(IllegalStateException.class,
                     () -> BackoffStrategy.exponentialDelay(Duration.ofMillis(1), Duration.ofSeconds(1), () -> 1.0)
                                          .computeDelay(1));
        assertThrows(IllegalArgumentException.class, () -> BackoffStrategy.fixedDelay(Duration.ofMillis(-1)));
        assertEquals(Duration.ZERO, BackoffStrategy.none().computeDelay(5));
        assertEquals(Duration.ofMillis(3), BackoffStrategy.fixedDelay(Duration.ofMillis(3)).computeDelay(5));
    }
}
