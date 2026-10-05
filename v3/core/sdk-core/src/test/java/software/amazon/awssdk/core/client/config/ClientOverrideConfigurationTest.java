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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.context.ContextKey;

class ClientOverrideConfigurationTest {
    private static final ContextKey<String> TENANT = ContextKey.execution("tenant", String.class);

    @Test
    void holdsOnlyWhatWasSet() {
        ClientOverrideConfiguration empty = ClientOverrideConfiguration.builder().build();
        assertTrue(empty.headers().isEmpty());
        assertEquals(Optional.empty(), empty.apiCallTimeout());
        assertEquals(Optional.empty(), empty.apiCallAttemptTimeout());
        assertEquals(TimeoutConfiguration.none(), empty.timeouts());
        assertTrue(empty.executionAttributes().isEmpty());
    }

    @Test
    void v2BuilderNamesRoundTrip() {
        ClientOverrideConfiguration config = ClientOverrideConfiguration.builder()
            .putHeader("X-Tenant", "a")
            .putHeader("x-multi", List.of("1", "2"))
            .apiCallTimeout(Duration.ofSeconds(30))
            .apiCallAttemptTimeout(Duration.ofSeconds(10))
            .putExecutionAttribute(TENANT, "t1")
            .build();

        assertEquals(List.of("a"), config.headers().get("x-tenant"), "header names are case-insensitive");
        assertEquals(List.of("1", "2"), config.headers().get("X-MULTI"));
        assertEquals(Optional.of(Duration.ofSeconds(30)), config.apiCallTimeout());
        assertEquals(Optional.of(Duration.ofSeconds(10)), config.apiCallAttemptTimeout());
        assertEquals(Optional.of("t1"), config.executionAttribute(TENANT));
        assertEquals(config, config.toBuilder().build());
        assertEquals(config.hashCode(), config.toBuilder().build().hashCode());

        ClientOverrideConfiguration changed = config.copy(b -> b.apiCallTimeout(null).putExecutionAttribute(TENANT, null));
        assertEquals(Optional.empty(), changed.apiCallTimeout());
        assertTrue(changed.executionAttributes().isEmpty());
        assertNotEquals(config, changed);
        assertEquals(Optional.of(Duration.ofSeconds(30)), config.apiCallTimeout(), "the original is unchanged");
    }

    @Test
    void builderGettersAndHeaderReplacement() {
        ClientOverrideConfiguration.Builder builder = ClientOverrideConfiguration.builder()
            .putHeader("a", "1")
            .apiCallAttemptTimeout(Duration.ofMillis(5));
        assertEquals(Duration.ofMillis(5), builder.apiCallAttemptTimeout());
        builder.headers(Map.of("b", List.of("2")));
        assertEquals(Map.of("b", List.of("2")), builder.build().headers());
    }

    @Test
    void rejectsInvalidValues() {
        ClientOverrideConfiguration.Builder builder = ClientOverrideConfiguration.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.apiCallTimeout(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> builder.apiCallAttemptTimeout(Duration.ofSeconds(-1)));
        assertThrows(IllegalArgumentException.class,
                     () -> builder.putExecutionAttribute(ContextKey.attempt("attempt", String.class), "x"));
        assertThrows(NullPointerException.class, () -> builder.putHeader(null, "x"));
    }

    @Test
    void toStringOmitsValues() {
        String rendered = ClientOverrideConfiguration.builder().putHeader("authorization", "secret")
                                                     .putExecutionAttribute(TENANT, "hidden").build().toString();
        assertTrue(rendered.contains("authorization"));
        assertTrue(!rendered.contains("secret") && !rendered.contains("hidden"), rendered);
    }
}
