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

package software.amazon.awssdk.core.client.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.context.ContextKey;

class ConfigurationKeysAndTimeoutsTest {
    @Test
    void clientOptionsAreIdentityKeyedAndTypeChecked() {
        ClientOption<Integer> a = ClientOption.create("maxAttempts", Integer.class);
        ClientOption<Integer> b = ClientOption.create("maxAttempts", Integer.class);
        assertNotEquals(a, b);
        assertEquals(a, a);
        assertEquals("maxAttempts", a.name());
        assertSame(Integer.class, a.valueType());
        assertEquals(3, a.cast(3));
        assertNull(a.cast(null));
        assertThrows(ClassCastException.class, () -> a.cast("3"));
        assertThrows(IllegalArgumentException.class, () -> ClientOption.create("", String.class));
        assertThrows(NullPointerException.class, () -> ClientOption.create("x", null));
    }

    @Test
    void contextKeysCarryTheirScope() {
        ContextKey<String> execution = ContextKey.execution("trace", String.class);
        ContextKey<String> attempt = ContextKey.attempt("trace", String.class);
        assertEquals(ContextKey.Scope.EXECUTION, execution.scope());
        assertEquals(ContextKey.Scope.ATTEMPT, attempt.scope());
        assertNotEquals(execution, ContextKey.execution("trace", String.class));
        assertThrows(ClassCastException.class, () -> execution.cast(1));
        assertThrows(IllegalArgumentException.class, () -> ContextKey.attempt("", String.class));
    }

    @Test
    void timeoutConfigurationRoundTripsAndComparesByValue() {
        TimeoutConfiguration timeouts = TimeoutConfiguration.builder()
                                                            .apiCallTimeout(Duration.ofSeconds(30))
                                                            .apiCallAttemptTimeout(Duration.ofSeconds(10))
                                                            .build();
        assertEquals(Optional.of(Duration.ofSeconds(30)), timeouts.apiCallTimeout());
        assertEquals(Optional.of(Duration.ofSeconds(10)), timeouts.apiCallAttemptTimeout());
        TimeoutConfiguration copy = timeouts.toBuilder().build();
        assertEquals(timeouts, copy);
        assertEquals(timeouts.hashCode(), copy.hashCode());
        assertNotEquals(timeouts, timeouts.toBuilder().apiCallAttemptTimeout(null).build());
        assertEquals(Optional.of(Duration.ofSeconds(10)), timeouts.apiCallAttemptTimeout(), "built value unchanged");
    }

    @Test
    void timeoutConfigurationRejectsNonPositiveTimeoutsAndDefaultsToNone() {
        assertThrows(IllegalArgumentException.class,
            () -> TimeoutConfiguration.builder().apiCallTimeout(Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
            () -> TimeoutConfiguration.builder().apiCallAttemptTimeout(Duration.ofMillis(-1)));
        assertSame(TimeoutConfiguration.none(), TimeoutConfiguration.builder().build());
        assertEquals(Optional.empty(), TimeoutConfiguration.none().apiCallTimeout());
        assertEquals(Optional.empty(), TimeoutConfiguration.none().apiCallAttemptTimeout());
    }
}
