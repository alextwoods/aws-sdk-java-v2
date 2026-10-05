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

package software.amazon.awssdk.awscodegen;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.WriterDelegator;

/**
 * Emits a service's waiters from a {@link WaiterPlan} (design "Paginators, waiters, presigners, and utilities";
 * Requirements 7.9, 8.7).
 *
 * <ul>
 *   <li>{@code <Service>Waiter} / {@code <Service>AsyncWaiter}: V2-shaped interfaces with {@code builder()},
 *       {@code create()}, {@code close()} and, per modeled waiter, {@code waitUntil<Name>} with a request, a request
 *       and a {@code WaiterOverrideConfiguration}, and a request-builder consumer.</li>
 *   <li>Package-private {@code Default<Service>Waiter} / {@code Default<Service>AsyncWaiter}: each wait polls the
 *       exact client the waiter was built with through the runtime {@code Waiter}, with the modeled acceptors in
 *       model order and the modeled delays. A waiter borrows a client given to {@code client(...)} and owns, and
 *       closes, one it created. The async waiter never blocks: its delays are futures of the given scheduler (the
 *       client's retry scheduler for {@code client.waiter()}), or of a scheduler it creates, owns and closes.</li>
 * </ul>
 */
final class WaiterGenerator {
    private static final Symbol CONSUMER = jdk("java.util.function", "Consumer");
    private static final Symbol FUNCTION = jdk("java.util.function", "Function");
    private static final Symbol DURATION = jdk("java.time", "Duration");
    private static final Symbol OBJECTS = jdk("java.util", "Objects");
    private static final Symbol COMPLETABLE_FUTURE = jdk("java.util.concurrent", "CompletableFuture");
    private static final Symbol SCHEDULED_EXECUTOR = jdk("java.util.concurrent", "ScheduledExecutorService");
    private static final Symbol RUNTIME_WAITER = jdk("software.amazon.awssdk.core.waiters", "Waiter");
    private static final Symbol ACCEPTOR = jdk("software.amazon.awssdk.core.waiters", "WaiterAcceptor");
    private static final Symbol STATE = jdk("software.amazon.awssdk.core.waiters", "WaiterState");
    private static final Symbol MATCHERS = jdk("software.amazon.awssdk.core.waiters", "WaiterMatchers");
    private static final Symbol RESPONSE = jdk("software.amazon.awssdk.core.waiters", "WaiterResponse");
    private static final Symbol OVERRIDE = jdk("software.amazon.awssdk.core.waiters", "WaiterOverrideConfiguration");
    private static final Symbol RETRY_SCHEDULER = jdk("software.amazon.awssdk.retries", "RetryScheduler");
    private static final Symbol DEFAULT_RETRY_SCHEDULER = jdk("software.amazon.awssdk.retries",
                                                              "DefaultRetryScheduler");
    private static final Symbol AWS_SERVICE_EXCEPTION = jdk("software.amazon.awssdk.awscore.exception",
                                                            "AwsServiceException");

    private WaiterGenerator() {
    }

    static void generate(AwsCodegenContext context, ClientPlan client) {
        render(WaiterPlan.of(context, client), context.writerDelegator());
    }

    static void render(WaiterPlan plan, WriterDelegator<AwsJavaWriter> writers) {
        if (plan.waiters().isEmpty()) {
            return;
        }
        renderInterface(plan, writers, false);
        renderDefault(plan, writers, false);
        if (plan.asyncClient() != null) {
            renderInterface(plan, writers, true);
            renderDefault(plan, writers, true);
        }
    }

    private static void renderInterface(WaiterPlan plan, WriterDelegator<AwsJavaWriter> writers, boolean async) {
        Symbol type = async ? plan.asyncWaiter() : plan.syncWaiter();
        Symbol client = async ? plan.asyncClient() : plan.syncClient();
        Symbol implementation = async ? plan.defaultAsyncWaiter() : plan.defaultSyncWaiter();
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type);
            writer.write("/**");
            writer.write(" * Waiters of {@link $T}: each polls an operation until a modeled acceptor reaches a "
                         + "terminal state.", client);
            writer.write(" *");
            if (async) {
                writer.write(" * <p>Waiting never blocks the caller: each {@code waitUntil} method returns a future "
                             + "at once, and delays");
                writer.write(" * between polls are scheduled on the configured scheduler, never on the common pool.");
            } else {
                writer.write(" * <p>Each {@code waitUntil} method blocks the calling thread until the wait ends.");
            }
            writer.write(" * A waiter from {@code client.waiter()} or {@code client(...)} borrows that client and "
                         + "does not close it;");
            writer.write(" * a waiter built without a client creates, owns and closes its own. Thread safe.");
            writer.write(" */");
            writer.openBlock("public interface $L extends AutoCloseable {", "}", type.getName(), () -> {
                for (WaiterPlan.WaiterMethod waiter : plan.waiters()) {
                    String result = async ? writer.format("$T<$T<$T>>", COMPLETABLE_FUTURE, RESPONSE,
                                                          waiter.output())
                                          : writer.format("$T<$T>", RESPONSE, waiter.output());
                    writer.write("/**");
                    writer.write(" * Polls {@code $L} until the {@code $L} waiter succeeds$L, with the per-wait "
                                 + "overrides.", waiter.operationName(), waiter.name(),
                                 async ? "" : " or fails");
                    writer.write(" */");
                    writer.write("$L $L($T request, $T overrideConfiguration);", result, waiter.method(),
                                 waiter.input(), OVERRIDE);
                    writer.write("");
                    writer.write("/** Polls {@code $L} until the {@code $L} waiter ends. */", waiter.operationName(),
                                 waiter.name());
                    writer.openBlock("default $L $L($T request) {", "}", result, waiter.method(), waiter.input(),
                                     () -> writer.write("return $L(request, null);", waiter.method()));
                    writer.write("");
                    writer.write("/** Polls {@code $L}, with the request built by {@code request}, until the "
                                 + "{@code $L} waiter ends. */", waiter.operationName(), waiter.name());
                    writer.openBlock("default $L $L($T<$T.Builder> request) {", "}", result, waiter.method(),
                                     CONSUMER, waiter.input(), () -> {
                            writer.write("$T.Builder builder = $T.builder();", waiter.input(), waiter.input());
                            writer.write("request.accept(builder);");
                            writer.write("return $L(builder.build(), null);", waiter.method());
                        });
                    writer.write("");
                }
                writer.write("/** A builder of a waiter. */");
                writer.openBlock("static Builder builder() {", "}",
                                 () -> writer.write("return new $T.DefaultBuilder();", implementation));
                writer.write("");
                writer.write("/** A waiter with its own default client. */");
                writer.openBlock("static $L create() {", "}", type.getName(),
                                 () -> writer.write("return builder().build();"));
                writer.write("");
                writer.write("/** Closes what this waiter owns: never a client it borrows; idempotent. */");
                writer.write("@Override");
                writer.write("void close();");
                writer.write("");
                writer.write("/** Builder of a waiter; not thread safe. */");
                writer.openBlock("interface Builder {", "}", () -> {
                    writer.write("/** The client to poll: borrowed, never closed by the waiter. */");
                    writer.write("Builder client($T client);", client);
                    writer.write("");
                    writer.write("/** Overrides of every wait of the waiter. */");
                    writer.write("Builder overrideConfiguration($T overrideConfiguration);", OVERRIDE);
                    writer.write("");
                    if (async) {
                        writer.write("/** The executor delays are scheduled on: caller-owned, never closed by the "
                                     + "waiter. */");
                        writer.write("Builder scheduledExecutorService($T executor);", SCHEDULED_EXECUTOR);
                        writer.write("");
                        writer.write("/** The scheduler of delays, such as a client's: caller-owned. */");
                        writer.write("Builder retryScheduler($T scheduler);", RETRY_SCHEDULER);
                        writer.write("");
                    }
                    writer.write("/** The waiter. */");
                    writer.write("$L build();", type.getName());
                });
            });
        });
    }

    private static void renderDefault(WaiterPlan plan, WriterDelegator<AwsJavaWriter> writers, boolean async) {
        Symbol type = async ? plan.defaultAsyncWaiter() : plan.defaultSyncWaiter();
        Symbol api = async ? plan.asyncWaiter() : plan.syncWaiter();
        Symbol client = async ? plan.asyncClient() : plan.syncClient();
        writers.useFileWriter(type.getDefinitionFile(), type.getNamespace(), writer -> {
            writer.reserveTypeNames(type);
            writer.write("/** Default {@link $T}: the modeled acceptors and delays over the runtime waiter. */", api);
            writer.openBlock("final class $L implements $T {", "}", type.getName(), api, () -> {
                writer.write("private final $T client;", client);
                writer.write("private final boolean ownsClient;");
                writer.write("private final $T overrides;", OVERRIDE);
                if (async) {
                    writer.write("private final $T<$T, $T<Void>> delays;", FUNCTION, DURATION, COMPLETABLE_FUTURE);
                    writer.write("private final $T ownedScheduler;", DEFAULT_RETRY_SCHEDULER);
                }
                writer.write("");
                writer.openBlock("private $L(DefaultBuilder builder) {", "}", type.getName(), () -> {
                    writer.write("this.ownsClient = builder.client == null;");
                    writer.write("this.client = ownsClient ? $T.create() : builder.client;", client);
                    writer.write("this.overrides = builder.overrides;");
                    if (async) {
                        writer.openBlock("if (builder.scheduler != null) {", "}", () -> {
                            writer.write("this.delays = builder.scheduler::delay;");
                            writer.write("this.ownedScheduler = null;");
                        });
                        writer.openBlock("else if (builder.executor != null) {", "}", () -> {
                            writer.write("this.delays = $T.of(builder.executor)::delay;", RETRY_SCHEDULER);
                            writer.write("this.ownedScheduler = null;");
                        });
                        writer.openBlock("else {", "}", () -> {
                            writer.write("// An SDK-owned timer, closed with the waiter; polls are non-blocking "
                                         + "client calls.");
                            writer.write("this.ownedScheduler = $T.create($S, Runnable::run);",
                                         DEFAULT_RETRY_SCHEDULER, "sdk-" + api.getName());
                            writer.write("this.delays = ownedScheduler::delay;");
                        });
                    }
                });
                for (WaiterPlan.WaiterMethod waiter : plan.waiters()) {
                    writeWait(writer, waiter, async);
                }
                writer.write("");
                writer.write("@Override");
                writer.openBlock("public void close() {", "}", () -> {
                    if (async) {
                        writer.openBlock("if (ownedScheduler != null) {", "}",
                                         () -> writer.write("ownedScheduler.close();"));
                    }
                    writer.openBlock("if (ownsClient) {", "}", () -> writer.write("client.close();"));
                });
                writer.write("");
                writeBuilder(writer, api, client, async);
            });
        });
    }

    private static void writeWait(AwsJavaWriter writer, WaiterPlan.WaiterMethod waiter, boolean async) {
        String result = async ? writer.format("$T<$T<$T>>", COMPLETABLE_FUTURE, RESPONSE, waiter.output())
                              : writer.format("$T<$T>", RESPONSE, waiter.output());
        writer.write("");
        writer.write("@Override");
        writer.openBlock("public $L $L($T request, $T overrideConfiguration) {", "}", result, waiter.method(),
                         waiter.input(), OVERRIDE, () -> {
                writer.write("$T.requireNonNull(request, \"request\");", OBJECTS);
                writer.write("$T<$T> waiter = $T.of($S, $LAcceptors(request), $T.ofSeconds($L), $T.ofSeconds($L), "
                             + "overrides);", RUNTIME_WAITER, waiter.output(), RUNTIME_WAITER, waiter.name(),
                             lowerFirst(waiter.name()), DURATION, waiter.minDelaySeconds(), DURATION,
                             waiter.maxDelaySeconds());
                if (async) {
                    writer.write("return waiter.runAsync(() -> client.$L(request), delays, overrideConfiguration);",
                                 waiter.operationMethod());
                } else {
                    writer.write("return waiter.run(() -> client.$L(request), overrideConfiguration);",
                                 waiter.operationMethod());
                }
            });
        writer.write("");
        writer.write("/** The {@code $L} acceptors, in model order. */", waiter.name());
        writer.openBlock("private static $T<$T<$T>> $LAcceptors($T request) {", "}", RuntimeSymbols.LIST, ACCEPTOR,
                         waiter.output(), lowerFirst(waiter.name()), waiter.input(), () -> {
                List<String> acceptors = new ArrayList<>();
                for (WaiterPlan.AcceptorPlan acceptor : waiter.acceptors()) {
                    acceptors.add(acceptor(writer, waiter, acceptor));
                }
                writer.write("return $T.of(", RuntimeSymbols.LIST);
                writer.indent(2);
                for (int i = 0; i < acceptors.size(); i++) {
                    writer.write("$L$L", acceptors.get(i), i == acceptors.size() - 1 ? ");" : ",");
                }
                writer.dedent(2);
            });
    }

    private static String acceptor(AwsJavaWriter writer, WaiterPlan.WaiterMethod waiter,
                                   WaiterPlan.AcceptorPlan acceptor) {
        String factory = acceptor.onException() ? "onException" : "onResponse";
        String predicate;
        if (acceptor.success() != null) {
            predicate = acceptor.onException() ? "exception -> true" : "response -> true";
        } else if (acceptor.errorType() != null) {
            String code = writer.format("exception instanceof $T service && service.awsErrorDetails() != null"
                                        + "\n        && $S.equals(service.awsErrorDetails().errorCode())",
                                        AWS_SERVICE_EXCEPTION, acceptor.errorType());
            predicate = acceptor.errorClass() == null ? "exception -> " + code
                        : writer.format("exception -> exception instanceof $T\n        || $L", acceptor.errorClass(),
                                        code);
        } else {
            String value = expression(writer, acceptor.path(), 0);
            String expected = "booleanEquals".equals(acceptor.comparator())
                              ? Boolean.toString(Boolean.parseBoolean(acceptor.expected()))
                              : writer.format("$S", acceptor.expected());
            predicate = writer.format("response -> $T.$L(\n        $L,\n        $L)", MATCHERS, acceptor.comparator(),
                                      value, expected);
        }
        return writer.format("$T.<$T>$L($T.$L, $L)", ACCEPTOR, waiter.output(), factory, STATE, acceptor.state(),
                             predicate);
    }

    /** The Java expression of a compiled path; {@code depth} numbers nested projection variables. */
    private static String expression(AwsJavaWriter writer, WaiterPlan.Expr expr, int depth) {
        if (expr instanceof WaiterPlan.Root root) {
            return "element".equals(root.variable()) ? "element" + depth : root.variable();
        }
        if (expr instanceof WaiterPlan.Field field) {
            return writer.format("$T.field($L, $T.class, $T::$L)", MATCHERS, expression(writer, field.base(), depth),
                                 field.type(), field.type(), field.getter());
        }
        if (expr instanceof WaiterPlan.Project project) {
            return writer.format("$T.project($L, element$L -> $L)", MATCHERS,
                                 expression(writer, project.list(), depth), depth + 1,
                                 expression(writer, project.element(), depth + 1));
        }
        if (expr instanceof WaiterPlan.Flatten flatten) {
            return writer.format("$T.flatten($L)", MATCHERS, expression(writer, flatten.list(), depth));
        }
        if (expr instanceof WaiterPlan.Length length) {
            return writer.format("$T.length($L)", MATCHERS, expression(writer, length.value(), depth));
        }
        if (expr instanceof WaiterPlan.Compare compare) {
            return writer.format("$T.compare($L, $S, $L)", MATCHERS, expression(writer, compare.left(), depth),
                                 compare.comparator(), expression(writer, compare.right(), depth));
        }
        if (expr instanceof WaiterPlan.Logic logic) {
            return logic.right() == null
                   ? writer.format("$T.not($L)", MATCHERS, expression(writer, logic.left(), depth))
                   : writer.format("$T.$L($L, $L)", MATCHERS, logic.operator(),
                                   expression(writer, logic.left(), depth), expression(writer, logic.right(), depth));
        }
        if (expr instanceof WaiterPlan.Filter filter) {
            return writer.format("$T.project($T.filter($L, element$L -> $L), element$L -> $L)", MATCHERS, MATCHERS,
                                 expression(writer, filter.list(), depth), depth + 1,
                                 expression(writer, filter.condition(), depth + 1), depth + 1,
                                 expression(writer, filter.element(), depth + 1));
        }
        if (expr instanceof WaiterPlan.Values values) {
            return writer.format("$T.values($L)", MATCHERS, expression(writer, values.map(), depth));
        }
        if (expr instanceof WaiterPlan.Contains contains) {
            return writer.format("$T.contains($L, $L)", MATCHERS, expression(writer, contains.subject(), depth),
                                 expression(writer, contains.search(), depth));
        }
        if (expr instanceof WaiterPlan.MultiSelect select) {
            List<String> items = new ArrayList<>();
            for (WaiterPlan.Expr item : select.items()) {
                items.add(expression(writer, item, depth + 1));
            }
            return writer.format("$T.multiSelect($L, element$L -> $T.list($L))", MATCHERS,
                                 expression(writer, select.base(), depth), depth + 1, MATCHERS,
                                 String.join(", ", items));
        }
        Object value = ((WaiterPlan.Literal) expr).value();
        if (value == null) {
            return "(Object) null";
        }
        if (value instanceof String string) {
            return writer.format("$S", string);
        }
        if (value instanceof Long number) {
            return number + "L";
        }
        if (value instanceof Double number) {
            return number + "d";
        }
        return value.toString();
    }

    private static void writeBuilder(AwsJavaWriter writer, Symbol api, Symbol client, boolean async) {
        writer.write("/** Default {@link $T.Builder}. */", api);
        writer.openBlock("static final class DefaultBuilder implements $T.Builder {", "}", api, () -> {
            writer.write("private $T client;", client);
            writer.write("private $T overrides;", OVERRIDE);
            if (async) {
                writer.write("private $T executor;", SCHEDULED_EXECUTOR);
                writer.write("private $T scheduler;", RETRY_SCHEDULER);
            }
            writer.write("");
            builderSetter(writer, api, "client", writer.format("$T", client), "client");
            builderSetter(writer, api, "overrideConfiguration", writer.format("$T", OVERRIDE), "overrides");
            if (async) {
                builderSetter(writer, api, "scheduledExecutorService", writer.format("$T", SCHEDULED_EXECUTOR),
                              "executor");
                builderSetter(writer, api, "retryScheduler", writer.format("$T", RETRY_SCHEDULER), "scheduler");
            }
            writer.write("@Override");
            writer.openBlock("public $T build() {", "}", api,
                             () -> writer.write("return new $L(this);", "Default" + api.getName()));
        });
    }

    private static void builderSetter(AwsJavaWriter writer, Symbol api, String method, String type, String field) {
        writer.write("@Override");
        writer.openBlock("public $T.Builder $L($L value) {", "}", api, method, type, () -> {
            writer.write("this.$L = value;", field);
            writer.write("return this;");
        });
        writer.write("");
    }

    private static String lowerFirst(String name) {
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private static Symbol jdk(String packageName, String name) {
        return Symbol.builder().namespace(packageName, ".").name(name).build();
    }
}
