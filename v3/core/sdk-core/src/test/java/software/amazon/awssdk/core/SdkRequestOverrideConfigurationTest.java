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

package software.amazon.awssdk.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.context.ContextKey;

class SdkRequestOverrideConfigurationTest {
    private static final ContextKey<String> TRACE = ContextKey.execution("trace", String.class);

    @Test
    void emptyOverrideSetsNothing() {
        SdkRequestOverrideConfiguration override = SdkRequestOverrideConfiguration.builder().build();
        assertEquals(Map.of(), override.headers());
        assertEquals(Map.of(), override.rawQueryParameters());
        assertEquals(Optional.empty(), override.apiCallTimeout());
        assertEquals(Optional.empty(), override.apiCallAttemptTimeout());
        assertEquals(Map.of(), override.executionAttributes());
        assertEquals(Optional.empty(), override.executionAttribute(TRACE));
    }

    @Test
    void headersAreCaseInsensitiveAndPutReplaces() {
        SdkRequestOverrideConfiguration override = SdkRequestOverrideConfiguration.builder()
                                                                                  .putHeader("X-Example", "a")
                                                                                  .putHeader("x-example", "b")
                                                                                  .putHeader("x-multi", List.of("1", "2"))
                                                                                  .build();
        assertEquals(2, override.headers().size());
        assertEquals(List.of("b"), override.headers().get("X-EXAMPLE"));
        assertEquals(List.of("1", "2"), override.headers().get("x-multi"));
    }

    @Test
    void queryParametersAreCaseSensitiveAndOrdered() {
        SdkRequestOverrideConfiguration override = SdkRequestOverrideConfiguration.builder()
                                                                                  .putRawQueryParameter("b", "1")
                                                                                  .putRawQueryParameter("B", "2")
                                                                                  .putRawQueryParameter("a", "3")
                                                                                  .build();
        assertEquals(List.of("b", "B", "a"), List.copyOf(override.rawQueryParameters().keySet()));
    }

    @Test
    void builtOverrideIsImmutableAndIsolatedFromItsBuilderAndInputs() {
        List<String> values = new ArrayList<>(List.of("v"));
        Map<String, List<String>> query = new LinkedHashMap<>(Map.of("q", values));
        SdkRequestOverrideConfiguration.Builder builder = SdkRequestOverrideConfiguration.builder()
                                                                                         .putHeader("h", values)
                                                                                         .rawQueryParameters(query);
        SdkRequestOverrideConfiguration override = builder.build();
        values.add("w");
        query.clear();
        builder.putHeader("other", "x").putRawQueryParameter("q", "changed");

        assertEquals(Map.of("h", List.of("v")), override.headers());
        assertEquals(Map.of("q", List.of("v")), override.rawQueryParameters());
        assertThrows(UnsupportedOperationException.class, () -> override.headers().put("n", List.of()));
        assertThrows(UnsupportedOperationException.class, () -> override.headers().get("h").add("x"));
        assertThrows(UnsupportedOperationException.class, () -> override.rawQueryParameters().clear());
        assertThrows(UnsupportedOperationException.class, () -> override.executionAttributes().clear());
    }

    @Test
    void toBuilderRoundTripsEveryField() {
        SdkRequestOverrideConfiguration override = SdkRequestOverrideConfiguration.builder()
                                                                                  .putHeader("h", "v")
                                                                                  .putRawQueryParameter("q", "v")
                                                                                  .apiCallTimeout(Duration.ofSeconds(5))
                                                                                  .apiCallAttemptTimeout(Duration.ofSeconds(2))
                                                                                  .putExecutionAttribute(TRACE, "t")
                                                                                  .build();
        SdkRequestOverrideConfiguration copy = override.toBuilder().build();
        assertEquals(override, copy);
        assertEquals(override.hashCode(), copy.hashCode());
        assertEquals(Optional.of(Duration.ofSeconds(5)), copy.apiCallTimeout());
        assertEquals(Optional.of(Duration.ofSeconds(2)), copy.apiCallAttemptTimeout());
        assertEquals(Optional.of("t"), copy.executionAttribute(TRACE));
        assertNotEquals(override, override.toBuilder().putExecutionAttribute(TRACE, null).build());
        assertNotEquals(override, override.toBuilder().apiCallTimeout(null).build());
    }

    @Test
    void executionAttributesAreTypedAndExecutionScoped() {
        ContextKey<String> attemptKey = ContextKey.attempt("attempt", String.class);
        assertThrows(IllegalArgumentException.class,
            () -> SdkRequestOverrideConfiguration.builder().putExecutionAttribute(attemptKey, "x"));
        @SuppressWarnings({"unchecked", "rawtypes"})
        ContextKey<Object> raw = (ContextKey) TRACE;
        assertThrows(ClassCastException.class,
            () -> SdkRequestOverrideConfiguration.builder().putExecutionAttribute(raw, 1));
        assertThrows(IllegalArgumentException.class,
            () -> SdkRequestOverrideConfiguration.builder().apiCallAttemptTimeout(Duration.ZERO));
    }

    @Test
    void toStringNamesHeadersWithoutValues() {
        String rendered = SdkRequestOverrideConfiguration.builder().putHeader("Authorization", "secret").build()
                                                         .toString();
        assertTrue(rendered.contains("Authorization"), rendered);
        assertFalse(rendered.contains("secret"), rendered);
    }
}
