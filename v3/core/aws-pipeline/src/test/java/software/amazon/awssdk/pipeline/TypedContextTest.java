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

package software.amazon.awssdk.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_INPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_THING;

import java.net.URI;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkRequestOverrideConfiguration;
import software.amazon.awssdk.core.context.ContextKey;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.pipeline.PipelineFixtures.CountingProtocol;
import software.amazon.awssdk.pipeline.PipelineFixtures.Struct;
import software.amazon.awssdk.retries.RetryToken;
import software.amazon.smithy.model.shapes.ShapeId;

class TypedContextTest {
    private static final ContextKey<String> EXEC = ContextKey.execution("Exec", String.class);
    private static final ContextKey<String> ATTEMPT = ContextKey.attempt("Attempt", String.class);
    private static final ContextKey<Integer> DECLARED = ContextKey.attempt("Declared", Integer.class);

    private final PipelinePlan<Struct, Struct> plan = PipelineFixtures.runtime().plan(GET_THING);

    private CallState<Struct, Struct> newCall() {
        return plan.newCall(PipelineFixtures.request(GET_INPUT, "a"), plan.runtime().callOptions());
    }

    @Test
    void executionValuesSurviveAttemptsAndAttemptValuesDoNot() {
        CallState<Struct, Struct> call = newCall();
        call.executionContext().put(EXEC, "before");
        RetryToken first = PipelineFixtures.firstToken(plan);

        AttemptState one = call.startAttempt(first);
        AttemptContext oneContext = one.context();
        assertThat(oneContext.get(EXEC)).isEqualTo("before");
        oneContext.put(ATTEMPT, "one");
        oneContext.execution().put(EXEC, "from-attempt-1");
        assertThat(oneContext.get(ATTEMPT)).isEqualTo("one");
        one.complete();

        AttemptState two = call.startAttempt(PipelineFixtures.retryToken(plan.runtime().retryStrategy(), first));
        assertThat(two.number()).isEqualTo(2);
        assertThat(two.context().get(ATTEMPT)).isNull();
        assertThat(two.context().contains(ATTEMPT)).isFalse();
        assertThat(two.context().get(EXEC)).isEqualTo("from-attempt-1");
        assertThat(two.context()).isNotSameAs(oneContext);

        // The first attempt's context is closed: nothing of it is reachable any more.
        assertThat(oneContext.isOpen()).isFalse();
        assertThatThrownBy(() -> oneContext.get(ATTEMPT)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> oneContext.put(ATTEMPT, "late")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(oneContext::attemptNumber).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void attemptNumberIsOneBasedAndPipelineOwned() {
        CallState<Struct, Struct> call = newCall();
        AttemptState attempt = call.startAttempt(PipelineFixtures.firstToken(plan));

        assertThat(attempt.context().attemptNumber()).isEqualTo(1);
        assertThat(attempt.context().get(PipelineKeys.ATTEMPT_NUMBER)).isEqualTo(1);
        assertThatThrownBy(() -> attempt.context().put(PipelineKeys.ATTEMPT_NUMBER, 7))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> attempt.context().put(PipelineKeys.ENDPOINT, endpoint("https://evil.example.com")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resolvedValuesAreForwardOnlyAndWrittenOnce() {
        CallState<Struct, Struct> call = newCall();
        AttemptState attempt = call.startAttempt(PipelineFixtures.firstToken(plan));
        Endpoint endpoint = endpoint("https://things.us-east-1.amazonaws.com");
        AwsCredentialsIdentity identity = AwsCredentialsIdentity.create("akid", "secret");

        assertThat(attempt.endpoint()).isEmpty();
        assertThat(attempt.context().get(PipelineKeys.ENDPOINT)).isNull();
        attempt.endpoint(endpoint);
        attempt.identity(identity);

        assertThat(attempt.endpoint()).containsSame(endpoint);
        assertThat(attempt.context().get(PipelineKeys.ENDPOINT)).isSameAs(endpoint);
        assertThat(attempt.context().get(PipelineKeys.IDENTITY)).isSameAs(identity);
        assertThatThrownBy(() -> attempt.endpoint(endpoint("https://other.example.com")))
            .isInstanceOf(IllegalStateException.class);

        attempt.complete();
        assertThatThrownBy(attempt::endpoint).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> attempt.response(null)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void scopesAreEnforced() {
        CallState<Struct, Struct> call = newCall();
        AttemptState attempt = call.startAttempt(PipelineFixtures.firstToken(plan));

        assertThatThrownBy(() -> call.executionContext().put(ATTEMPT, "x"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> call.executionContext().get(ATTEMPT))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> attempt.context().put(EXEC, "x"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("execution()");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void readsAndWritesEnforceTheDeclaredType() {
        CallState<Struct, Struct> call = newCall();
        ContextKey raw = EXEC;

        assertThatThrownBy(() -> call.executionContext().put(raw, 42)).isInstanceOf(ClassCastException.class);
        assertThat(call.executionContext().contains(EXEC)).isFalse();
        call.executionContext().put(EXEC, "v");
        call.executionContext().put(EXEC, null);
        assertThat(call.executionContext().contains(EXEC)).isFalse();
    }

    @Test
    void declaredKeysUseSlotsAndSlotsAreBoundToTheirPlan() {
        ClientRuntime runtime = PipelineFixtures.runtimeBuilder(new CountingProtocol())
                                                .addPlugin(new DeclaringPlugin())
                                                .build();
        PipelinePlan<Struct, Struct> declaring = runtime.plan(GET_THING);
        ContextSlot<Integer> slot = declaring.contextLayout().slot(DECLARED).orElseThrow();
        CallState<Struct, Struct> call = declaring.newCall(PipelineFixtures.request(GET_INPUT, "a"),
                                                           runtime.callOptions());
        AttemptContext context = call.startAttempt(PipelineFixtures.firstToken(declaring)).context();

        context.put(slot, 5);
        assertThat(context.get(DECLARED)).isEqualTo(5);
        context.put(DECLARED, 6);
        assertThat(context.get(slot)).isEqualTo(6);

        // The same key has no slot in another plan's layout; its slot is rejected there, while the key itself
        // still works through the overflow storage.
        assertThat(plan.contextLayout().slot(DECLARED)).isEmpty();
        AttemptContext other = newCall().startAttempt(PipelineFixtures.firstToken(plan)).context();
        assertThatThrownBy(() -> other.get(slot)).isInstanceOf(IllegalArgumentException.class);
        other.put(DECLARED, 9);
        assertThat(other.get(DECLARED)).isEqualTo(9);
    }

    @Test
    void executionAttributesOfTheOverrideSeedTheExecutionContext() {
        SdkRequestOverrideConfiguration override = SdkRequestOverrideConfiguration.builder()
                                                                                  .putExecutionAttribute(EXEC, "o")
                                                                                  .build();
        CallState<Struct, Struct> call = plan.newCall(PipelineFixtures.request(GET_INPUT, "a"),
                                                      plan.runtime().callOptions(override, null));

        assertThat(call.executionContext().get(EXEC)).isEqualTo("o");
        assertThat(newCall().executionContext().get(EXEC)).isNull();
    }

    private static Endpoint endpoint(String url) {
        return Endpoint.builder().url(URI.create(url)).build();
    }

    private static final class DeclaringPlugin implements AwsFeaturePlugin {
        @Override
        public ShapeId id() {
            return ShapeId.from("test#Declaring");
        }

        @Override
        public boolean appliesTo(AwsOperation<?, ?> operation) {
            return true;
        }

        @Override
        public void configure(PipelinePlan.Builder<?, ?> plan) {
            plan.declare(DECLARED);
        }
    }
}
