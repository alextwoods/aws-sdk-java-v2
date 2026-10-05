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

package software.amazon.awssdk.core.override;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequestOverrideConfiguration;
import software.amazon.awssdk.core.client.config.ClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.client.config.TimeoutConfiguration;
import software.amazon.awssdk.core.context.ContextKey;

/**
 * Merging and scope of per-call overrides. Retry, interceptor and telemetry values use test stand-ins declared the way
 * their owning modules (tasks 8.3, 11.x, 16.2) will declare them: a {@link ClientOption} plus a {@link CallOption}.
 */
class CallOptionsTest {
    /** Stand-in retry strategy; toString would leak the marker if a view rendered values. */
    record Retry(int maxAttempts) {
        @Override
        public String toString() {
            return "Retry(SECRET-" + maxAttempts + ")";
        }
    }

    record Interceptor(String name) {
    }

    record Telemetry(String publisher) {
    }

    static final ClientOption<Retry> RETRY_STRATEGY = ClientOption.create("RetryStrategy", Retry.class);
    static final ClientOption<List<Interceptor>> INTERCEPTORS = ClientOption.create("ExecutionInterceptors", listType());
    static final ClientOption<Telemetry> TELEMETRY = ClientOption.create("Telemetry", Telemetry.class);

    static final CallOption<Retry> RETRY_CALL = CallOption.replacing(RETRY_STRATEGY);
    static final CallOption<List<Interceptor>> INTERCEPTORS_CALL = CallOption.appending(INTERCEPTORS, Interceptor.class);
    static final CallOption<Telemetry> TELEMETRY_CALL = CallOption.replacing(TELEMETRY);

    static final ContextKey<String> TRACE = ContextKey.execution("Trace", String.class);
    static final URI ENDPOINT = URI.create("https://example.test");
    static final Interceptor CLIENT_INTERCEPTOR = new Interceptor("client");

    @SuppressWarnings("unchecked")
    static <E> Class<List<E>> listType() {
        return (Class<List<E>>) (Class<?>) List.class;
    }

    static SdkClientConfiguration base() {
        return SdkClientConfiguration.builder()
                                     .option(SdkClientOption.CLIENT_ENDPOINT, ENDPOINT)
                                     .option(SdkClientOption.TIMEOUT_CONFIGURATION,
                                             TimeoutConfiguration.builder()
                                                                 .apiCallTimeout(Duration.ofSeconds(30))
                                                                 .apiCallAttemptTimeout(Duration.ofSeconds(10))
                                                                 .build())
                                     .option(RETRY_STRATEGY, new Retry(5))
                                     .option(INTERCEPTORS, List.of(CLIENT_INTERCEPTOR))
                                     .option(TELEMETRY, new Telemetry("client"))
                                     .build();
    }

    @Test
    void noOverrideReturnsTheClientInstanceAndReadsThrough() {
        SdkClientConfiguration base = base();
        CallOptions client = CallOptions.of(base);
        assertSame(client, client.withOverrides(null, null));
        assertSame(client, client.withOverrides(SdkRequestOverrideConfiguration.builder().build(), null));
        assertFalse(client.hasOverrides());
        assertSame(base, client.clientConfiguration());
        for (ClientOption<?> option : base.options()) {
            assertEquals(base.get(option), client.get(option), option.name());
            assertFalse(client.isOverridden(option));
        }
        assertEquals(Map.of(), client.overriddenOptions());
    }

    @Test
    void timeoutsMergeFieldByFieldOverTheClient() {
        CallOptions call = CallOptions.of(base(), SdkRequestOverrideConfiguration.builder()
                                                                                 .apiCallAttemptTimeout(
                                                                                     Duration.ofSeconds(2))
                                                                                 .build());
        assertEquals(Optional.of(Duration.ofSeconds(30)), call.timeouts().apiCallTimeout());
        assertEquals(Optional.of(Duration.ofSeconds(2)), call.timeouts().apiCallAttemptTimeout());

        CallOptions noClientTimeouts = CallOptions.of(SdkClientConfiguration.empty(),
                                                      SdkRequestOverrideConfiguration.builder()
                                                                                     .apiCallTimeout(
                                                                                         Duration.ofSeconds(3))
                                                                                     .build());
        assertEquals(Optional.of(Duration.ofSeconds(3)), noClientTimeouts.timeouts().apiCallTimeout());
        assertEquals(Optional.empty(), noClientTimeouts.timeouts().apiCallAttemptTimeout());
        assertEquals(TimeoutConfiguration.none(), CallOptions.of(SdkClientConfiguration.empty()).timeouts());
    }

    @Test
    void retryReplacesInterceptorsAppendAndTelemetryReplaces() {
        SdkClientConfiguration base = base();
        Interceptor callInterceptor = new Interceptor("call");
        CallOptions call = CallOptions.of(base, SdkRequestOverrideConfiguration.builder()
                                                                               .putCallOption(RETRY_CALL, new Retry(2))
                                                                               .addCallOptionElement(INTERCEPTORS_CALL,
                                                                                                     callInterceptor)
                                                                               .putCallOption(TELEMETRY_CALL,
                                                                                              new Telemetry("call"))
                                                                               .build());
        assertEquals(Optional.of(new Retry(2)), call.get(RETRY_STRATEGY));
        assertEquals(Optional.of(List.of(CLIENT_INTERCEPTOR, callInterceptor)), call.get(INTERCEPTORS));
        assertEquals(Optional.of(new Telemetry("call")), call.get(TELEMETRY));
        assertEquals(Optional.of(ENDPOINT), call.get(SdkClientOption.CLIENT_ENDPOINT));
        assertEquals(3, call.overriddenOptions().size());
        assertEquals(base(), base);
    }

    @Test
    void explicitOverloadOverrideWinsOverTheRequestEmbeddedOne() {
        Interceptor embeddedInterceptor = new Interceptor("embedded");
        Interceptor explicitInterceptor = new Interceptor("explicit");
        RequestOverrideConfiguration embedded = SdkRequestOverrideConfiguration.builder()
                                                                               .putHeader("x-example", "embedded")
                                                                               .putHeader("x-embedded-only", "e")
                                                                               .putRawQueryParameter("q", "embedded")
                                                                               .putExecutionAttribute(TRACE, "embedded")
                                                                               .apiCallTimeout(Duration.ofSeconds(9))
                                                                               .putCallOption(RETRY_CALL, new Retry(3))
                                                                               .addCallOptionElement(INTERCEPTORS_CALL,
                                                                                                     embeddedInterceptor)
                                                                               .build();
        RequestOverrideConfiguration explicit = SdkRequestOverrideConfiguration.builder()
                                                                               .putHeader("X-EXAMPLE", "explicit")
                                                                               .putRawQueryParameter("q", "explicit")
                                                                               .putExecutionAttribute(TRACE, "explicit")
                                                                               .apiCallAttemptTimeout(
                                                                                   Duration.ofSeconds(1))
                                                                               .putCallOption(RETRY_CALL, new Retry(2))
                                                                               .addCallOptionElement(INTERCEPTORS_CALL,
                                                                                                     explicitInterceptor)
                                                                               .build();
        CallOptions call = CallOptions.of(base()).withOverrides(embedded, explicit);

        assertEquals(List.of("explicit"), call.headers().get("x-example"));
        assertEquals("X-EXAMPLE", call.headers().keySet().stream().filter("x-example"::equalsIgnoreCase)
                                      .findFirst().orElseThrow());
        assertEquals(List.of("e"), call.headers().get("x-embedded-only"));
        assertEquals(Map.of("q", List.of("explicit")), call.rawQueryParameters());
        assertEquals(Optional.of("explicit"), call.executionAttribute(TRACE));
        assertEquals(TimeoutConfiguration.builder()
                                         .apiCallTimeout(Duration.ofSeconds(9))
                                         .apiCallAttemptTimeout(Duration.ofSeconds(1))
                                         .build(), call.timeouts());
        assertEquals(Optional.of(new Retry(2)), call.get(RETRY_STRATEGY));
        assertEquals(Optional.of(List.of(CLIENT_INTERCEPTOR, embeddedInterceptor, explicitInterceptor)),
                     call.get(INTERCEPTORS));
        assertEquals(Optional.of(new Telemetry("client")), call.get(TELEMETRY));
    }

    @Test
    void clientScopedOptionsCannotBeOverriddenPerCall() {
        CallOptions call = CallOptions.of(base(), SdkRequestOverrideConfiguration.builder()
                                                                                 .putHeader("x", "y")
                                                                                 .apiCallTimeout(Duration.ofSeconds(1))
                                                                                 .build());
        assertFalse(call.isOverridden(SdkClientOption.CLIENT_ENDPOINT));
        assertEquals(Optional.of(ENDPOINT), call.get(SdkClientOption.CLIENT_ENDPOINT));
        assertTrue(call.isOverridden(SdkClientOption.TIMEOUT_CONFIGURATION));

        // Timeouts have exactly one call option; a second key over the same client option is rejected.
        CallOption<TimeoutConfiguration> rogue = CallOption.replacing(SdkClientOption.TIMEOUT_CONFIGURATION);
        assertThrows(IllegalArgumentException.class,
                     () -> SdkRequestOverrideConfiguration.builder().putCallOption(rogue, TimeoutConfiguration.none()));
    }

    @Test
    void deltasAreValidatedAndIsolatedFromCallerHeldInputs() {
        SdkRequestOverrideConfiguration.Builder builder = SdkRequestOverrideConfiguration.builder();
        @SuppressWarnings({"unchecked", "rawtypes"})
        CallOption<Object> untyped = (CallOption) RETRY_CALL;
        assertThrows(ClassCastException.class, () -> builder.putCallOption(untyped, "not a retry strategy"));
        @SuppressWarnings({"unchecked", "rawtypes"})
        List<Interceptor> wrongElements = (List) List.of("not an interceptor");
        assertThrows(ClassCastException.class, () -> builder.putCallOption(INTERCEPTORS_CALL, wrongElements));
        assertThrows(IllegalArgumentException.class, () -> builder.addCallOptionElement(
            CallOption.replacing(INTERCEPTORS), new Interceptor("x")));

        List<Interceptor> callerList = new ArrayList<>(List.of(new Interceptor("a")));
        builder.putCallOption(INTERCEPTORS_CALL, callerList);
        RequestOverrideConfiguration built = builder.build();
        callerList.add(new Interceptor("b"));
        builder.addCallOptionElement(INTERCEPTORS_CALL, new Interceptor("c"));
        assertEquals(Optional.of(List.of(new Interceptor("a"))), built.callOption(INTERCEPTORS_CALL));
        assertThrows(UnsupportedOperationException.class,
                     () -> built.callOption(INTERCEPTORS_CALL).orElseThrow().add(new Interceptor("d")));

        // A second call option over the same client option replaces the first delta.
        CallOption<Retry> other = CallOption.replacing(RETRY_STRATEGY);
        RequestOverrideConfiguration replaced = SdkRequestOverrideConfiguration.builder()
                                                                               .putCallOption(RETRY_CALL, new Retry(1))
                                                                               .putCallOption(other, new Retry(2))
                                                                               .build();
        assertEquals(1, replaced.callOptions().size());
        assertEquals(Optional.of(new Retry(2)), CallOptions.of(base(), replaced).get(RETRY_STRATEGY));
    }

    @Test
    void callOptionsParticipateInEqualityAndRoundTripThroughBuilders() {
        RequestOverrideConfiguration override = SdkRequestOverrideConfiguration.builder()
                                                                               .apiCallTimeout(Duration.ofSeconds(4))
                                                                               .putCallOption(RETRY_CALL, new Retry(2))
                                                                               .build();
        assertEquals(override, override.toBuilder().build());
        assertEquals(override.hashCode(), override.toBuilder().build().hashCode());
        assertNotEquals(override, override.toBuilder().putCallOption(RETRY_CALL, null).build());
        assertEquals(List.of(SdkCallOption.TIMEOUT_CONFIGURATION, RETRY_CALL), List.copyOf(override.callOptions()
                                                                                                    .keySet()));
        assertEquals(Optional.of(TimeoutConfiguration.builder().apiCallTimeout(Duration.ofSeconds(4)).build()),
                     override.callOption(SdkCallOption.TIMEOUT_CONFIGURATION));

        RequestOverrideConfiguration viaKey = SdkRequestOverrideConfiguration.builder()
                                                                             .putCallOption(
                                                                                 SdkCallOption.TIMEOUT_CONFIGURATION,
                                                                                 TimeoutConfiguration.builder()
                                                                                                     .apiCallTimeout(
                                                                                                         Duration.ofSeconds(4))
                                                                                                     .build())
                                                                             .putCallOption(RETRY_CALL, new Retry(2))
                                                                             .build();
        assertEquals(override, viaKey);
        assertEquals(Optional.of(Duration.ofSeconds(4)), viaKey.apiCallTimeout());
        assertTrue(SdkRequestOverrideConfiguration.builder().build().isEmpty());
        assertFalse(override.isEmpty());
    }

    @Test
    void overrideStateIsNotSerializableAndRendersNoValues() {
        assertFalse(Serializable.class.isAssignableFrom(CallOptions.class));
        assertFalse(Serializable.class.isAssignableFrom(CallOption.class));
        assertFalse(Serializable.class.isAssignableFrom(RequestOverrideConfiguration.class));

        RequestOverrideConfiguration override = SdkRequestOverrideConfiguration.builder()
                                                                               .putHeader("Authorization", "SECRET-h")
                                                                               .putExecutionAttribute(TRACE, "SECRET-a")
                                                                               .putCallOption(RETRY_CALL, new Retry(2))
                                                                               .build();
        CallOptions call = CallOptions.of(base(), override);
        for (String rendered : List.of(override.toString(), call.toString())) {
            assertFalse(rendered.contains("SECRET"), rendered);
            assertTrue(rendered.contains("RetryStrategy"), rendered);
        }
    }
}
