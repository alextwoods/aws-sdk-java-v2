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
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_INPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_OUTPUT;
import static software.amazon.awssdk.pipeline.PipelineFixtures.GET_THING;

import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.statistics.Statistics;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.StreamingBodies;
import software.amazon.awssdk.pipeline.PipelineFixtures.Driver;
import software.amazon.awssdk.pipeline.PipelineFixtures.ScriptedHttp;
import software.amazon.awssdk.pipeline.PipelineFixtures.Struct;
import software.amazon.smithy.java.core.schema.SerializableStruct;

/**
 * Property 39: Interceptor failures identify source and preserve cause (Requirements 11.13, 11.14).
 *
 * <p>For any of the 16 hooks, a {@link Failer} interceptor at any position among 1-3 interceptors either throws a
 * generated exception (unchecked, or checked through a sneaky throw) or, where the hook takes a replacement, returns an
 * incompatible one, every time the hook runs. The call's surfaced failure then holds exactly one
 * {@link InterceptorException} of the failer, naming its class, the hook and the operation, with the thrown exception
 * as its cause by identity or, for an incompatible return, no cause. The failure a hook failure replaced, and a later
 * {@link Accomplice}'s failure in the same after-hook, are suppressed onto it by identity; interceptors after the
 * failer in the same hook do not run unless the hook is {@code afterAttempt} or {@code afterExecution};
 * {@code afterExecution} observes the surfaced failure exactly once; and the transmissions are those {@link #predict}
 * derives. Two outcomes are not a surfaced {@code InterceptorException}: an {@code afterExecution} failure of a call
 * that already failed is suppressed onto that call's failure, and an attempt failure that is retried and followed by
 * a successful attempt (the failer's hook not running on it) leaves the call succeeding.
 *
 * <p>Each try scripts one of: success; a 503 then a success (two attempts allowed); a 400. Retries follow the default
 * classifier: an attempt's failure, an {@code InterceptorException} included, is retryable here only when the attempt
 * received a 503 response (the generated thrown types carry no I/O or transport failure in their cause chain), so a
 * failing hook before transmission is never retried and one after a 503 response is. The async driver runs inline.
 */
class InterceptorFailurePropertyTest {
    /** The attempt hooks that run before the request is transmitted. */
    private static final Set<PipelineStage> BEFORE_SEND = EnumSet.of(
        PipelineStage.BEFORE_ATTEMPT, PipelineStage.AFTER_SERIALIZATION, PipelineStage.MODIFY_BEFORE_SIGNING,
        PipelineStage.MODIFY_AFTER_SIGNING, PipelineStage.BEFORE_TRANSMISSION);
    /** The attempt hooks that run on every received response, before deserialization. */
    private static final Set<PipelineStage> ON_RESPONSE = EnumSet.of(
        PipelineStage.AFTER_TRANSMISSION, PipelineStage.MODIFY_HTTP_RESPONSE, PipelineStage.MODIFY_RESPONSE_BODY,
        PipelineStage.BEFORE_DESERIALIZATION);
    /** The hooks that run every interceptor even after one fails. */
    private static final Set<PipelineStage> RUN_ALL = EnumSet.of(PipelineStage.AFTER_ATTEMPT,
                                                                 PipelineStage.AFTER_EXECUTION);
    /** The hooks whose return value replaces something, so an incompatible return is possible. */
    private static final Set<PipelineStage> REPLACING = EnumSet.of(
        PipelineStage.MODIFY_REQUEST, PipelineStage.MODIFY_BEFORE_SIGNING, PipelineStage.MODIFY_AFTER_SIGNING,
        PipelineStage.MODIFY_HTTP_RESPONSE, PipelineStage.MODIFY_RESPONSE_BODY, PipelineStage.MODIFY_ATTEMPT_EXCEPTION,
        PipelineStage.MODIFY_RESPONSE, PipelineStage.MODIFY_EXCEPTION);

    /** The status of each scripted attempt; the strategy allows exactly as many attempts. */
    enum Script {
        OK(200), RETRY(503, 200), BAD(400);

        final List<Integer> statuses;

        Script(Integer... statuses) {
            this.statuses = List.of(statuses);
        }
    }

    /** A thrown exception type: unchecked, or checked (thrown sneakily). */
    enum ThrownType {
        ILLEGAL_STATE(IllegalStateException::new), ILLEGAL_ARGUMENT(IllegalArgumentException::new),
        UNSUPPORTED(UnsupportedOperationException::new), RUNTIME(RuntimeException::new),
        CHECKED(Exception::new), CHECKED_SECURITY(GeneralSecurityException::new);

        final Function<String, Exception> factory;

        ThrownType(Function<String, Exception> factory) {
            this.factory = factory;
        }
    }

    /** A thrown exception to create per call; {@code message} may be null. */
    record Thrown(ThrownType type, String message) {
        Exception create() {
            return type.factory.apply(message);
        }
    }

    /**
     * One generated call.
     *
     * @param thrown what the failer throws, or null for an incompatible return
     * @param accomplice the position of the accomplice (after the failer, in the same after-hook), or -1
     */
    record Scenario(PipelineStage hook, Thrown thrown, int interceptors, int failer, int accomplice,
                    Thrown accompliceThrown, Driver driver, Script script) {
    }

    /**
     * What the oracle predicts.
     *
     * @param runs how many times the failer's hook ran
     * @param onSurfaced whether the failer's exception is suppressed onto the surfaced failure (an afterExecution
     *     failure of a failing call) rather than being the surfaced failure
     * @param replaces whether the failer's last run saw a failure, which its exception must keep suppressed
     * @param sawFailure whether afterExecution observed a failure
     */
    record Prediction(boolean succeeded, int transmissions, int runs, boolean onSurfaced, boolean replaces,
                      boolean sawFailure) {
    }

    /** One hook invocation: which interceptor, which hook, and the failure it saw, if any. */
    record Entry(int interceptor, PipelineStage hook, Throwable seen) {
    }

    // Feature: aws-sdk-java-v3, Property 39: Interceptor failures identify source and preserve cause
    @Property(tries = 400, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 39: Interceptor failures identify source and preserve cause")
    void failuresNameTheInterceptorAndHookAndKeepTheCause(@ForAll("scenarios") Scenario scenario) {
        Prediction predicted = predict(scenario);
        Call call = new Call(scenario);
        Throwable surfaced = call.run();
        String description = scenario + "\n  predicted " + predicted + "\n  surfaced " + surfaced + "\n  log "
                             + call.log;

        assertThat(surfaced == null).as("succeeded: " + description).isEqualTo(predicted.succeeded());
        assertThat(call.http.transmissions).as("transmissions: " + description).isEqualTo(predicted.transmissions());
        List<Entry> failerRuns = call.entries(scenario.failer(), scenario.hook());
        assertThat(failerRuns).as("failer runs: " + description).hasSize(predicted.runs());
        for (int i = 0; i < scenario.interceptors(); i++) {
            boolean runs = i < scenario.failer() || RUN_ALL.contains(scenario.hook());
            assertThat(call.entries(i, scenario.hook())).as("runs of " + i + ": " + description)
                                                       .hasSize(i == scenario.failer() || runs ? predicted.runs() : 0);
            List<Entry> afterExecution = call.entries(i, PipelineStage.AFTER_EXECUTION);
            assertThat(afterExecution).as("afterExecution of " + i + ": " + description).hasSize(1);
            assertThat(afterExecution.get(0).seen()).as("afterExecution saw: " + description)
                                                    .isSameAs(predicted.sawFailure() ? surfaced : null);
        }

        List<InterceptorException> failerExceptions = interceptorExceptions(surfaced, Failer.class);
        boolean reported = !predicted.succeeded() && predicted.runs() > 0;
        assertThat(failerExceptions).as("failer exceptions: " + description).hasSize(reported ? 1 : 0);
        if (reported) {
            InterceptorException primary = failerExceptions.get(0);
            assertIdentifies(primary, Failer.class, scenario.hook(), call.thrown, description);
            if (predicted.onSurfaced()) {
                assertThat(surfaced.getSuppressed()).as("suppressed on surfaced: " + description).containsOnlyOnce(
                    primary);
            } else {
                assertThat(surfaced).as("surfaced: " + description).isSameAs(primary);
            }
            Throwable seen = failerRuns.get(failerRuns.size() - 1).seen();
            assertThat(seen != null).as("replaces: " + description).isEqualTo(predicted.replaces());
            if (seen != null && predicted.onSurfaced()) {
                assertThat(seen).as("observed failure: " + description).isSameAs(surfaced);
            } else if (seen != null) {
                assertThat(primary.getSuppressed()).as("replaced failure: " + description).containsOnlyOnce(seen);
            }
            if (scenario.accomplice() >= 0) {
                List<InterceptorException> later = interceptorExceptions(surfaced, Accomplice.class);
                assertThat(later).as("accomplice exceptions: " + description).hasSize(1);
                assertIdentifies(later.get(0), Accomplice.class, scenario.hook(), call.accompliceThrown, description);
                assertThat(primary.getSuppressed()).as("later failure: " + description).containsOnlyOnce(later.get(0));
            }
        }

        Statistics.label("hook").collect(scenario.hook());
        Statistics.label("hook").coverage(c -> PipelineArbitraries.HOOK_STAGES.forEach(
            h -> c.check(h).percentage(p -> p >= 2)));
        String failure = scenario.thrown() == null ? "incompatible"
                         : scenario.thrown().type().name().startsWith("CHECKED") ? "checked" : "unchecked";
        Statistics.label("failure").collect(failure);
        Statistics.label("failure").coverage(c -> {
            c.check("incompatible").percentage(p -> p >= 7);
            c.check("checked").percentage(p -> p >= 10);
            c.check("unchecked").percentage(p -> p >= 20);
        });
        String outcome = predicted.succeeded() ? "succeeded" : !reported ? "failed, hook not reached"
                         : predicted.onSurfaced() ? "suppressed on the call's failure" : "surfaced";
        Statistics.label("outcome").collect(outcome);
        Statistics.label("outcome").coverage(c -> {
            c.check("surfaced").percentage(p -> p >= 50);
            c.check("suppressed on the call's failure").percentage(p -> p > 0);
            c.check("succeeded").percentage(p -> p > 0);
        });
        Statistics.label("replaces a failure").collect(reported && predicted.replaces());
        Statistics.label("replaces a failure").coverage(c -> c.check(true).percentage(p -> p >= 5));
        Statistics.label("retried").collect(predicted.transmissions() > 1);
        Statistics.label("retried").coverage(c -> c.check(true).percentage(p -> p >= 10));
        Statistics.label("accomplice").collect(scenario.accomplice() >= 0);
        Statistics.label("accomplice").coverage(c -> c.check(true).percentage(p -> p >= 3));
        Statistics.label("driver").collect(scenario.driver());
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        List<Tuple.Tuple2<Integer, PipelineStage>> weighted = new ArrayList<>();
        PipelineArbitraries.HOOK_STAGES.forEach(h -> weighted.add(Tuple.of(RUN_ALL.contains(h) ? 3 : 1, h)));
        @SuppressWarnings("unchecked")
        Arbitrary<PipelineStage> hooks = Arbitraries.frequency(weighted.toArray(Tuple.Tuple2[]::new));
        Arbitrary<Thrown> thrown = Combinators.combine(
            Arbitraries.of(ThrownType.class),
            Arbitraries.strings().alpha().numeric().withChars(' ').ofMaxLength(16).injectNull(0.1)).as(Thrown::new);
        return hooks.flatMap(hook -> {
            // A null failure is an incompatible return.
            Arbitrary<Thrown> failure = REPLACING.contains(hook) ? thrown.injectNull(0.35) : thrown;
            Arbitrary<Script> scripts = switch (hook) {
                case MODIFY_EXCEPTION -> Arbitraries.just(Script.BAD);
                case MODIFY_ATTEMPT_EXCEPTION -> Arbitraries.of(Script.RETRY, Script.BAD);
                default -> Arbitraries.of(Script.class);
            };
            Arbitrary<int[]> layouts = Arbitraries.integers().between(1, 3).flatMap(
                size -> Arbitraries.integers().between(0, size - 1).flatMap(failer -> {
                    Arbitrary<Integer> none = Arbitraries.just(-1);
                    Arbitrary<Integer> accomplice = !RUN_ALL.contains(hook) || failer == size - 1 ? none
                        : Arbitraries.frequencyOf(Tuple.of(1, none),
                                                  Tuple.of(2, Arbitraries.integers().between(failer + 1, size - 1)));
                    return accomplice.map(a -> new int[] {size, failer, a});
                }));
            return Combinators.combine(failure, layouts, thrown, PipelineArbitraries.drivers(), scripts)
                              .as((f, l, a, driver, script) -> new Scenario(hook, f, l[0], l[1], l[2],
                                                                            l[2] < 0 ? null : a, driver, script));
        });
    }

    /** The hook table walked for one scenario, with the default classifier's verdict on each failed attempt. */
    static Prediction predict(Scenario s) {
        PipelineStage hook = s.hook();
        if (hook == PipelineStage.BEFORE_EXECUTION || hook == PipelineStage.MODIFY_REQUEST) {
            return new Prediction(false, 0, 1, false, false, true);
        }
        List<Integer> statuses = s.script().statuses;
        int runs = 0;
        for (int attempt = 1; attempt <= statuses.size(); attempt++) {
            int status = statuses.get(attempt - 1);
            // An attempt's failure is retried only if it received a 503 and another attempt is allowed.
            boolean retried = status == 503 && attempt < statuses.size();
            if (BEFORE_SEND.contains(hook)) {
                return new Prediction(false, attempt - 1, runs + 1, false, false, true);
            }
            if (ON_RESPONSE.contains(hook)) {
                runs++;
                if (retried) {
                    continue;
                }
                return new Prediction(false, attempt, runs, false, false, true);
            }
            if (status == 200) {
                return switch (hook) {
                    case AFTER_ATTEMPT, MODIFY_RESPONSE -> new Prediction(false, attempt, runs + 1, false, false, true);
                    case AFTER_EXECUTION -> new Prediction(false, attempt, runs + 1, false, false, false);
                    default -> new Prediction(true, attempt, runs, false, false, false);
                };
            }
            // The attempt failed with the decoded service error; these hooks replace it.
            if (hook == PipelineStage.MODIFY_ATTEMPT_EXCEPTION || hook == PipelineStage.AFTER_ATTEMPT) {
                runs++;
                if (retried) {
                    continue;
                }
                return new Prediction(false, attempt, runs, false, true, true);
            }
            if (retried) {
                continue;
            }
            return switch (hook) {
                case MODIFY_EXCEPTION -> new Prediction(false, attempt, runs + 1, false, true, true);
                case AFTER_EXECUTION -> new Prediction(false, attempt, runs + 1, true, true, true);
                default -> new Prediction(false, attempt, runs, false, false, true);
            };
        }
        throw new AssertionError("unreachable: " + s);
    }

    private static void assertIdentifies(InterceptorException e, Class<?> type, PipelineStage hook, Throwable thrown,
                                         String description) {
        String name = hookName(hook);
        assertThat(e.interceptorClass()).as("class: " + description).isSameAs(type);
        assertThat(e.hook()).as("hook: " + description).isEqualTo(name);
        assertThat(e.operation()).as("operation: " + description).isEqualTo(GET_THING.id());
        assertThat(e).as(description).hasMessageContaining(type.getName()).hasMessageContaining(name);
        if (thrown == null) {
            assertThat(e).as(description).hasNoCause().hasMessageContaining("incompatible");
        } else {
            assertThat(e.getCause()).as("cause: " + description).isSameAs(thrown);
            String message = thrown.getMessage();
            assertThat(e).as(description).hasMessageContaining(
                "threw " + thrown.getClass().getName() + (message == null ? "" : ": " + message));
        }
    }

    /** Every {@link InterceptorException} of {@code type} reachable from {@code root} by causes and suppressions. */
    private static List<InterceptorException> interceptorExceptions(Throwable root, Class<?> type) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Throwable> pending = new ArrayList<>();
        List<InterceptorException> found = new ArrayList<>();
        if (root != null) {
            pending.add(root);
        }
        while (!pending.isEmpty()) {
            Throwable t = pending.remove(pending.size() - 1);
            if (!seen.add(t)) {
                continue;
            }
            if (t instanceof InterceptorException e && e.interceptorClass() == type) {
                found.add(e);
            }
            if (t.getCause() != null) {
                pending.add(t.getCause());
            }
            pending.addAll(List.of(t.getSuppressed()));
        }
        return found;
    }

    /** The hook method's name: {@code MODIFY_HTTP_RESPONSE} is {@code modifyHttpResponse}. */
    private static String hookName(PipelineStage hook) {
        StringBuilder name = new StringBuilder();
        for (String word : hook.name().toLowerCase(Locale.ROOT).split("_")) {
            name.append(name.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1));
        }
        return name.toString();
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException sneakyThrow(Throwable t) throws E {
        throw (E) t;
    }

    /** One call of a scenario: its interceptors, its scripted transport, and the log its interceptors write. */
    private static final class Call {
        final List<Entry> log = new CopyOnWriteArrayList<>();
        final ScriptedHttp http = new ScriptedHttp();
        final Exception thrown;
        final Exception accompliceThrown;
        private final Scenario scenario;

        Call(Scenario scenario) {
            this.scenario = scenario;
            this.thrown = scenario.thrown() == null ? null : scenario.thrown().create();
            this.accompliceThrown = scenario.accompliceThrown() == null ? null : scenario.accompliceThrown().create();
        }

        List<Entry> entries(int interceptor, PipelineStage hook) {
            return log.stream().filter(e -> e.interceptor() == interceptor && e.hook() == hook).toList();
        }

        /** Runs the call; the failure it surfaced, or null if it succeeded. */
        Throwable run() {
            Driver driver = scenario.driver();
            List<Integer> statuses = scenario.script().statuses;
            ClientRuntime.Builder runtime =
                PipelineFixtures.engineRuntime(PipelineFixtures.retries(statuses.size()), driver.mode);
            if (driver == Driver.ASYNC) {
                runtime.completionExecutor(Runnable::run)
                       .retryScheduler(delay -> CompletableFuture.completedFuture(null));
            }
            for (int i = 0; i < scenario.interceptors(); i++) {
                runtime.addInterceptor(i == scenario.failer() ? new Failer(this, i)
                                       : i == scenario.accomplice() ? new Accomplice(this, i) : new Passer(this, i));
            }
            statuses.forEach(status -> http.respond(status, status == 200 ? "v" : null));
            AwsCallEngine.Builder builder = AwsCallEngine.builder().runtime(runtime.build());
            AwsCallEngine engine = (driver == Driver.SYNC ? builder.syncHttpClient(http)
                                                          : builder.asyncHttpClient(http)).build();
            PipelinePlan<Struct, Struct> plan = engine.runtime().plan(GET_THING);
            Struct request = PipelineFixtures.request(GET_INPUT, "a");
            if (driver == Driver.SYNC) {
                try {
                    engine.executeSync(plan, request, null, null);
                    return null;
                } catch (RuntimeException failure) {
                    return failure;
                }
            }
            CompletableFuture<Struct> future = engine.executeAsync(plan, request, null, null);
            assertThat(future).as("inline async call of " + scenario).isDone();
            try {
                future.get(5, TimeUnit.SECONDS);
                return null;
            } catch (ExecutionException failure) {
                return failure.getCause();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /** Overrides all 16 hooks; each records its invocation and returns {@link #on}'s result. */
    private abstract static class Member implements ExecutionInterceptor {
        final Call call;
        private final int index;

        Member(Call call, int index) {
            this.call = call;
            this.index = index;
        }

        /** Records the invocation of {@code hook}, which saw {@code seen}; the hook's result: {@code value}. */
        Object on(PipelineStage hook, Object value, Throwable seen) {
            call.log.add(new Entry(index, hook, seen));
            return value;
        }

        @Override
        public void beforeExecution(HookContext.BeforeExecution context) {
            on(PipelineStage.BEFORE_EXECUTION, null, null);
        }

        @Override
        public SerializableStruct modifyRequest(HookContext.ModifyRequest context) {
            return (SerializableStruct) on(PipelineStage.MODIFY_REQUEST, context.request(), null);
        }

        @Override
        public void beforeAttempt(HookContext.BeforeAttempt context) {
            on(PipelineStage.BEFORE_ATTEMPT, null, null);
        }

        @Override
        public void afterSerialization(HookContext.AfterSerialization context) {
            on(PipelineStage.AFTER_SERIALIZATION, null, null);
        }

        @Override
        public SdkHttpRequest modifyBeforeSigning(HookContext.ModifyBeforeSigning context) {
            return (SdkHttpRequest) on(PipelineStage.MODIFY_BEFORE_SIGNING, context.httpRequest(), null);
        }

        @Override
        public SignedRequest modifyAfterSigning(HookContext.ModifyAfterSigning context) {
            return (SignedRequest) on(PipelineStage.MODIFY_AFTER_SIGNING, context.signedRequest(), null);
        }

        @Override
        public void beforeTransmission(HookContext.BeforeTransmission context) {
            on(PipelineStage.BEFORE_TRANSMISSION, null, null);
        }

        @Override
        public void afterTransmission(HookContext.AfterTransmission context) {
            on(PipelineStage.AFTER_TRANSMISSION, null, context.failure().orElse(null));
        }

        @Override
        public SdkHttpResponse modifyHttpResponse(HookContext.ModifyHttpResponse context) {
            return (SdkHttpResponse) on(PipelineStage.MODIFY_HTTP_RESPONSE, context.httpResponse(), null);
        }

        @Override
        public HttpBody modifyResponseBody(HookContext.ModifyResponseBody context) {
            return (HttpBody) on(PipelineStage.MODIFY_RESPONSE_BODY, context.body(), null);
        }

        @Override
        public void beforeDeserialization(HookContext.BeforeDeserialization context) {
            on(PipelineStage.BEFORE_DESERIALIZATION, null, null);
        }

        @Override
        public Throwable modifyAttemptException(HookContext.ModifyAttemptException context) {
            return (Throwable) on(PipelineStage.MODIFY_ATTEMPT_EXCEPTION, context.failure(), context.failure());
        }

        @Override
        public void afterAttempt(HookContext.AfterAttempt context) {
            on(PipelineStage.AFTER_ATTEMPT, null, context.failure().orElse(null));
        }

        @Override
        public SerializableStruct modifyResponse(HookContext.ModifyResponse context) {
            return (SerializableStruct) on(PipelineStage.MODIFY_RESPONSE, context.response(), null);
        }

        @Override
        public Throwable modifyException(HookContext.ModifyException context) {
            return (Throwable) on(PipelineStage.MODIFY_EXCEPTION, context.failure(), context.failure());
        }

        @Override
        public void afterExecution(HookContext.AfterExecution context) {
            on(PipelineStage.AFTER_EXECUTION, null, context.failure().orElse(null));
        }
    }

    /** Returns every value unchanged. */
    private static final class Passer extends Member {
        Passer(Call call, int index) {
            super(call, index);
        }
    }

    /** Fails the scenario's hook every time it runs: throws, or returns an incompatible replacement. */
    private static final class Failer extends Member {
        Failer(Call call, int index) {
            super(call, index);
        }

        @Override
        Object on(PipelineStage hook, Object value, Throwable seen) {
            super.on(hook, value, seen);
            if (hook != call.scenario.hook()) {
                return value;
            }
            if (call.thrown != null) {
                throw sneakyThrow(call.thrown);
            }
            return switch (hook) {
                case MODIFY_REQUEST -> new Struct(GET_OUTPUT, Map.of());
                case MODIFY_BEFORE_SIGNING -> {
                    byte[] bytes = {1, 2, 3};
                    HttpBody oneShot = HttpBody.streaming(
                        StreamingBodies.fromInputStream(new ByteArrayInputStream(bytes), bytes.length, null));
                    yield ((SdkHttpRequest) value).toBuilder().body(oneShot).build();
                }
                case MODIFY_AFTER_SIGNING -> SignedRequest.unsigned(
                    ((SignedRequest) value).request().toBuilder().host("elsewhere.example.com").build());
                case MODIFY_HTTP_RESPONSE, MODIFY_RESPONSE_BODY -> null;
                case MODIFY_ATTEMPT_EXCEPTION -> new Throwable("not an Exception");
                case MODIFY_RESPONSE -> PipelineFixtures.request(GET_INPUT, "x");
                case MODIFY_EXCEPTION -> new Exception("not a RuntimeException");
                default -> throw new AssertionError("no replacement at " + hook);
            };
        }
    }

    /** Throws its own exception from the scenario's after-hook, after the failer. */
    private static final class Accomplice extends Member {
        Accomplice(Call call, int index) {
            super(call, index);
        }

        @Override
        Object on(PipelineStage hook, Object value, Throwable seen) {
            super.on(hook, value, seen);
            if (hook == call.scenario.hook()) {
                throw sneakyThrow(call.accompliceThrown);
            }
            return value;
        }
    }
}
